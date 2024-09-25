/**
 * Copyright 2011-2022 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * HISTORY
 * VERSION:4.13:DM:DM-103:08/12/2023:[PATRIUS] Optimisation du CIRFProvider
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.precessionnutation;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

import fr.cnes.sirius.patrius.frames.configuration.FrameConvention;
import fr.cnes.sirius.patrius.math.analysis.interpolation.HermiteInterpolator;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.time.interpolation.TimeStampedInterpolableEphemeris;
import fr.cnes.sirius.patrius.time.interpolation.TimeStampedInterpolationFunctionBuilder;
import fr.cnes.sirius.patrius.utils.TimeStampedPVCoordinates;

/**
 * Precession nutation correction computation through an interpolation process.
 *
 * <p>
 * This implementation includes a caching/interpolation feature to tremendously improve efficiency. The IAU-2000 model
 * involves lots of terms (1600 components for x, 1275 components for y and 66 components for s). Recomputing all these
 * components for each point is really slow. The shortest period for these components is about 5.5 days (one fifth of
 * the moon revolution period), hence the pole motion is smooth at the day or week scale. This implies that these
 * motions can be computed accurately using a few reference points per day or week and interpolated between these
 * points. This implementation uses 4 points (CIP and CIP velocities) separated by 1/2 day (43200 seconds) each, the
 * resulting maximal interpolation error on the frame is about 1.3&times;10<sup>-10</sup> arcseconds.
 * </p>
 * 
 * <p>
 * Some information about performance: The CIP coordinates are interpolated thanks to reference CIPCoordinates that we
 * will call "references".<br>
 * These references are computed in function of the needs for the interpolation. For implementation simplicity, it is
 * imposed that there is no holes between references. Thus, if dates are too apart from each other (more than
 * {@link #allowedExtensionBeforeEphemerisReset} holes to be filled), the references are reinitialized. As a
 * consequence, if the dates are erratically spread, it is advised to pre-initialize the reference values with
 * {@link #initializeCipEphemeris} in order to avoid too many re-initializations of the references.<br>
 * 
 * Another aspect is that if the required dates are separated of more than the {@link #getInterpolationStep()}, the
 * interpolation management will not be efficient and it is advised to use a direct {@link PrecessionNutationModel}
 * instead.
 * </p>
 *
 * @author veuillh
 * 
 * @since 4.13
 */
public class PrecessionNutationInterpolation implements PrecessionNutationModel {

    /** Default number of interpolation points. */
    public static final int DEFAULT_INTERP_ORDER = 4;

    /** Default time span between generated reference points. */
    public static final int DEFAULT_INTERP_STEP = 43_200;

    /** Default ephemeris max size before resetting for memory usage purpose. */
    public static final int DEFAULT_EPHEM_MAX_SIZE = 5_000;

    /** Default allowed extra CIPCoordinates to compute in order to keep the same ephemeris. */
    public static final int DEFAULT_ALLOWED_EXTENSION_BEFORE_EPHEM_RESET = 60;

    /** Serializable UID. */
    private static final long serialVersionUID = -5228653233948458389L;

    /** Optimal cache size for the interpolation functions. */
    private static final int CACHE_SIZE = 6;

    /** Read write lock for the cipEphemeris. */
    private final ReadWriteLock cipEphemerisLock = new ReentrantReadWriteLock(false);

    /** Half interpolation order. */
    private final int halfInterpolationOrder;

    /** Time span between 2 samples of the interpolation (default value: {@link #DEFAULT_INTERP_STEP}). */
    private final int interpolationStep;

    /** Ephemeris max size for memory usage control (default value: {@link #DEFAULT_EPHEM_MAX_SIZE}). */
    private final int ephemerisMaxSize;

    /**
     * Corresponds to the allowed extra CIPCoordinates to compute in order to keep the same ephemeris (default value:
     * {@link #DEFAULT_ALLOWED_EXTENSION_BEFORE_EPHEM_RESET}).
     */
    private final int allowedExtensionBeforeEphemerisReset;

    /** Precession nutation model. */
    private final PrecessionNutationModel pnModel;

    /** Interpolable ephemeris of CIPCoordinates. */
    @SuppressWarnings("PMD.AvoidUsingVolatile")
    // Reason: volatile so that all threads have the most recent value of this attribute
    private volatile TimeStampedInterpolableEphemeris<CIPCoordinates, CIPCoordinates> cipEphemeris;

    /**
     * Simple constructor with default values.
     *
     * @param pnModel
     *        Precession nutation model to use (must be a {@link PrecessionNutationModel#isDirect() direct)}.
     * @throws IllegalArgumentException
     *         if the precession nutation model is not direct
     */
    public PrecessionNutationInterpolation(final PrecessionNutationModel pnModel) {
        this(pnModel, DEFAULT_INTERP_STEP, DEFAULT_INTERP_ORDER, DEFAULT_EPHEM_MAX_SIZE,
                DEFAULT_ALLOWED_EXTENSION_BEFORE_EPHEM_RESET);
    }

    /**
     * Main constructor.
     *
     * @param pnModel
     *        Precession nutation model to use (must be a {@link PrecessionNutationModel#isDirect() direct)}.
     * @param interpolationStep
     *        time span between interpolation points
     * @param interpolationOrder
     *        number of interpolation points to use
     * @param ephemerisMaxSize
     *        Max size of the internal ephemeris before reset (too avoid memory leaks)
     * @param allowedExtensionBeforeEphemerisReset
     *        When a new CIP coordinates needs to be interpolated while not present in the ephemeris, the latter is
     *        extended. This parameter corresponds to the maximum number of CIPCoordinates allowed for the extension
     *        before resetting the ephemeris around the required date.
     * @throws IllegalArgumentException
     *         if the number of interpolation points is {@code < 2}<br>
     *         if the number of interpolation points is not even<br>
     *         if the precession nutation model is not direct
     */
    @SuppressWarnings("PMD.NullAssignment")
    // Reason: null on purpose, lazy initialization
    public PrecessionNutationInterpolation(final PrecessionNutationModel pnModel, final int interpolationStep,
                                           final int interpolationOrder, final int ephemerisMaxSize,
                                           final int allowedExtensionBeforeEphemerisReset) {

        // Check the inputs
        if (interpolationOrder < 2 || interpolationOrder % 2 != 0) {
            throw new IllegalArgumentException("The order must be an even number greater or equal than 2");
        }
        if (!pnModel.isDirect()) {
            throw new IllegalArgumentException(
                "The precession nutation model must be direct so that an interpolation is not "
                        + "performed on an already interpolated model.");
        }

        // Store the parameters
        this.pnModel = pnModel;
        this.interpolationStep = interpolationStep;
        this.halfInterpolationOrder = interpolationOrder / 2;
        this.ephemerisMaxSize = ephemerisMaxSize;
        this.allowedExtensionBeforeEphemerisReset = allowedExtensionBeforeEphemerisReset;
        this.cipEphemeris = null;
    }

    /**
     * Pre-initialize (optional) the CIP coordinates for a given interval for performance purpose.
     *
     * <p>
     * Can be useful when CIP coordinates are required at very different dates that would lead to multiple ephemeris
     * re-initializations (with regards to the {@link #allowedExtensionBeforeEphemerisReset}.<br>
     * Calling this method does not prevent the class to extend the ephemeris, it is just a way to reduce the
     * re-initialization occurrences.
     * </p>
     *
     * <p>
     * This method is thread-safe.
     * </p>
     *
     * @param firstUsableDate
     *        First usable date of the ephemeris
     * @param lastUsableDate
     *        Last usable date of the ephemeris
     * @throws IllegalArgumentException
     *         if the ephemeris exceeds the maximum allowed size
     */
    public void initializeCIPEphemeris(final AbsoluteDate firstUsableDate, final AbsoluteDate lastUsableDate) {
        this.cipEphemerisLock.writeLock().lock();
        try {
            final int nbSteps = (int) MathLib.ceil(lastUsableDate.durationFrom(firstUsableDate)
                    / this.interpolationStep) - 1;
            if (nbSteps + 2 * this.halfInterpolationOrder > this.ephemerisMaxSize) {
                throw new IllegalArgumentException("The initialization requires an ephemeris that exceeds the maximum "
                        + "allowed size. Please configure the maximum size.");
            }
            final CIPCoordinates[] cipCoordinatesArray = generateCIPCoordinates(generateMeshDate(firstUsableDate),
                this.halfInterpolationOrder - 1, nbSteps + this.halfInterpolationOrder + 1);
            this.cipEphemeris = generateEphemeris(cipCoordinatesArray);
        } finally {
            this.cipEphemerisLock.writeLock().unlock();
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * This method is thread-safe.
     * </p>
     */
    @Override
    public CIPCoordinates getCIPCoordinates(final AbsoluteDate date) {
        // Implementation note: the thread safety management is based on the "CachedData" example of the
        // ReentrantReadWriteLock javadoc
        this.cipEphemerisLock.readLock().lock();
        if (needToUpdateCipEphemeris(date)) {
            // The cipEphemeris must be updated before being used
            // Must release read lock before acquiring write lock
            this.cipEphemerisLock.readLock().unlock();
            this.cipEphemerisLock.writeLock().lock();
            try {
                // Try block for the write lock

                // Check how the ephemeris should be updated
                if (this.cipEphemeris == null || this.cipEphemeris.getSampleSize() > this.ephemerisMaxSize) {
                    // The ephemeris does not exist yet or is too big:
                    // Generate a new ephemeris around the required date
                    this.cipEphemeris = initializeEphemeris(date);
                } else {
                    // A CIP ephemeris already exists. Check if it needs to be extended to cover the required date.

                    // Check with respect to the start of the ephemeris
                    final double durationWrtFirstUsableDate = date.durationFrom(this.cipEphemeris.getFirstUsableDate());

                    if (durationWrtFirstUsableDate < 0) {
                        // The required date is before the already available ephemeris dates
                        final int stepsBefore =
                            (int) MathLib.ceil(-durationWrtFirstUsableDate / this.interpolationStep);
                        if (stepsBefore > 2 * this.halfInterpolationOrder + this.allowedExtensionBeforeEphemerisReset) {
                            // The date is too far from the existent ephemeris:
                            // Restart with a new ephemeris around this date
                            this.cipEphemeris = initializeEphemeris(date);
                        } else {
                            // Extend the cipEphemeris with past CIP coordinates
                            final CIPCoordinates[] extraSamples =
                                generateCIPCoordinates(this.cipEphemeris.getFirstDate(), stepsBefore, -1);
                            this.cipEphemeris =
                                this.cipEphemeris.extendInterpolableEphemeris(extraSamples, false, false);
                        }
                    } else {
                        // Check with respect to the end of the ephemeris
                        final double durationWrtLastUsableDate =
                            date.durationFrom(this.cipEphemeris.getLastUsableDate());
                        if (durationWrtLastUsableDate > 0) {
                            // The required date is after the already available ephemeris dates
                            final int stepsAfter = (int) MathLib.ceil(durationWrtLastUsableDate
                                    / this.interpolationStep);
                            if (stepsAfter > 2 * this.halfInterpolationOrder
                                    + this.allowedExtensionBeforeEphemerisReset) {
                                // The date is too far from the existent ephemeris:
                                // Restart with a new ephemeris around this date
                                this.cipEphemeris = initializeEphemeris(date);
                            } else {
                                // Extend the cipEphemeris with future CIP coordinates
                                final CIPCoordinates[] extraSamples =
                                    generateCIPCoordinates(this.cipEphemeris.getLastDate(), -1, stepsAfter);
                                this.cipEphemeris =
                                    this.cipEphemeris.extendInterpolableEphemeris(extraSamples, true, false);
                            }
                        }
                    }
                }
                // Downgrade by acquiring read lock before releasing write lock
                // The downgrade grants that no other thread will modify the cipEphmeris
                this.cipEphemerisLock.readLock().lock();
            } finally {
                // Unlock the write lock but still hold the read lock
                this.cipEphemerisLock.writeLock().unlock();
            }
        }

        // At this step, the CIP ephemeris is valid for the interpolation
        try {
            // Try block for the read lock
            return this.cipEphemeris.interpolate(date);
        } finally {
            this.cipEphemerisLock.readLock().unlock();
        }
    }

    /**
     * Getter for the interpolation order.
     * 
     * @return the interpolation order
     */
    public int getInterpolationOrder() {
        return this.halfInterpolationOrder * 2;
    }

    /**
     * Getter for the interpolation step.
     * 
     * @return the interpolation step
     */
    public int getInterpolationStep() {
        return this.interpolationStep;
    }

    /**
     * Getter for the maximal internal reference values size before reinitialization.
     * 
     * @return the maximal internal reference values size before reinitialization
     */
    public int getEphemerisMaxSize() {
        return this.ephemerisMaxSize;
    }

    /**
     * Getter for the maximum allowed reference extensions before reinitialization.
     * 
     * @return the maximum allowed reference extensions before reinitialization
     */
    public int getAllowedExtensionBeforeEphemerisReset() {
        return this.allowedExtensionBeforeEphemerisReset;
    }

    /**
     * Getter for the internal precession nutation model.
     * 
     * @return the internal precession nutation model
     */
    public PrecessionNutationModel getModel() {
        return this.pnModel;
    }

    /**
     * Check if the cipEphemeris should be updated before being used.
     *
     * @param date
     *        Working date
     * @return true if the cipEphemeris needs to be updated
     */
    private boolean needToUpdateCipEphemeris(final AbsoluteDate date) {
        return this.cipEphemeris == null
                || date.compareTo(this.cipEphemeris.getFirstUsableDate()) < 0
                || date.compareTo(this.cipEphemeris.getLastUsableDate()) > 0;
    }

    /**
     * Initialize an ephemeris of the correct interpolation order.
     *
     * <p>
     * Note that the ephemeris is generated using dates belonging to a specific mesh in order to avoid having too
     * erratic reference dates.
     * </p>
     *
     * @param date
     *        Date for which the ephemeris should be initialized
     * @return the generated ephemeris
     */
    private TimeStampedInterpolableEphemeris<CIPCoordinates, CIPCoordinates>
        initializeEphemeris(final AbsoluteDate date) {
        // The CIP coordinates to initialize the ephemeris
        final CIPCoordinates[] cipCoordinatesArray = generateCIPCoordinates(generateMeshDate(date),
            this.halfInterpolationOrder - 1, this.halfInterpolationOrder + 1);
        return generateEphemeris(cipCoordinatesArray);
    }

    /**
     * Generate a date belonging to a specific mesh (multiples of {@link #interpolationStep}) so that the set of
     * CIPCooridnates reference dates does not depend of the specific dates used to initialize the ephemeris.
     * 
     * @param date
     *        Date used to generate the mesh date
     * @return the mesh date (multiple of the interpolation step)
     */
    private AbsoluteDate generateMeshDate(final AbsoluteDate date) {
        final long epoch = date.getEpoch();
        final long floorDivision;
        if (epoch >= 0) {
            // Integer division is equivalent to do a division as doubles and then call floor function
            floorDivision = epoch / this.interpolationStep;
        } else {
            // If epoch is negative, we need to remove 1 to the integer division to have the the same result as the
            // positive case
            floorDivision = epoch / this.interpolationStep - 1;
        }
        final long roundEpoch = floorDivision * this.interpolationStep;
        return new AbsoluteDate(roundEpoch, 0.);
    }

    /**
     * Initialize an ephemeris of the correct interpolation order.
     *
     * @param cipCoordinatesArray
     *        CIP coordinates
     * @return the generated ephemeris
     */
    private TimeStampedInterpolableEphemeris<CIPCoordinates, CIPCoordinates>
        generateEphemeris(final CIPCoordinates[] cipCoordinatesArray) {

        // The interpolation function builder
        final TimeStampedInterpolationFunctionBuilder<CIPCoordinates, CIPCoordinates> interpolationFunctionBuilder =
            (samples, indexInf, indexSup) -> {
                return new HermiteTimeStampedCIPCoordinatesInterpolationFunction(samples, indexInf, indexSup);
            };
        // The ephemeris
        return new TimeStampedInterpolableEphemeris<>(cipCoordinatesArray, this.halfInterpolationOrder * 2,
            interpolationFunctionBuilder, false, false, false, CACHE_SIZE);
    }

    /**
     * Generate CIPCoordinates array.
     *
     * @param refDate
     *        Reference date to start with
     * @param stepsBefore
     *        Number of steps before the reference date. Can be negative to not include the refDate.
     * @param stepsAfter
     *        Number of steps after the reference date. Can be negative to not include the ref date.
     * @return the array of CIPCoordinates
     */
    private CIPCoordinates[] generateCIPCoordinates(final AbsoluteDate refDate, final int stepsBefore,
                                                    final int stepsAfter) {

        final int size = stepsBefore + stepsAfter + 1;
        final CIPCoordinates[] data = new CIPCoordinates[size];
        final AbsoluteDate firstDate = refDate.shiftedBy(-stepsBefore * this.interpolationStep);

        for (int i = 0; i < size; i++) {
            final AbsoluteDate date = firstDate.shiftedBy(i * this.interpolationStep);
            data[i] = this.pnModel.getCIPCoordinates(date);
        }
        return data;
    }

    /**
     * Getter for the current usable interval of the ephemeris.
     *
     * @return the current usable interval of the ephemeris. Can be {@code null} if the ephemeris has not been
     *         initialized yet.
     */
    public AbsoluteDateInterval getCurrentUsableInterval() {
        if (this.cipEphemeris == null) {
            return null;
        }
        return new AbsoluteDateInterval(this.cipEphemeris.getFirstUsableDate(), this.cipEphemeris.getLastUsableDate());
    }

    /**
     * Getter for the cache reusability ratio.<br>
     * See {@link TimeStampedInterpolableEphemeris#getCacheReusabilityRatio()} for more information.
     * 
     * @return the reusability ratio
     */
    public double getEphemerisCacheReusabilityRatio() {
        if (this.cipEphemeris == null) {
            return 0.;
        }
        return this.cipEphemeris.getCacheReusabilityRatio();
    }

    /**
     * Getter for the ephemeris size.
     *
     * @return the ephemeris size
     */
    public int getEphemerisSize() {
        if (this.cipEphemeris == null) {
            return 0;
        }
        return this.cipEphemeris.getSampleSize();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDirect() {
        return false; // PrecessionNutationInterpolation isn't direct by definition (interpolated)
    }

    /** {@inheritDoc} */
    @Override
    public FrameConvention getOrigin() {
        return this.pnModel.getOrigin();
    }

    /** Hermite interpolation function for the {@link CIPCoordinates}. */
    private static class HermiteTimeStampedCIPCoordinatesInterpolationFunction
        implements Function<AbsoluteDate, CIPCoordinates> {

        /** Hermite interpolator used to interpolate the {@link TimeStampedPVCoordinates}. */
        private final HermiteInterpolator interpolator;

        /** Reference date. */
        private AbsoluteDate refDate;

        /**
         * Standard constructor.
         *
         * @param samples
         *        Samples of time stamped PV data. It should be sorted.
         * @param indexInf
         *        Inferior index
         * @param indexSup
         *        Superior index
         */
        public HermiteTimeStampedCIPCoordinatesInterpolationFunction(final CIPCoordinates[] samples,
                                                                     final int indexInf, final int indexSup) {

            this.interpolator = new HermiteInterpolator();

            // Build HermiteInterpolator
            for (int i = indexInf; i < indexSup; i++) {
                final CIPCoordinates sample = samples[i];
                final AbsoluteDate date = sample.getDate();
                if (this.refDate == null) {
                    this.refDate = date;
                }

                final double[] value = new double[] { sample.getX(), sample.getY(), sample.getS() };
                final double[] derivativeValue = new double[] { sample.getxP(), sample.getyP(), sample.getsP() };
                this.interpolator.addSamplePoint(date.durationFrom(this.refDate), value, derivativeValue);
            }
        }

        /** {@inheritDoc} */
        @Override
        public CIPCoordinates apply(final AbsoluteDate date) {
            // The interpolator uses the duration from the reference date to interpolate
            final double duration = date.durationFrom(this.refDate);
            final double[][] values = this.interpolator.valueAndDerivative(duration, false);
            return new CIPCoordinates(date, values[0], values[1]);
        }
    }
}
