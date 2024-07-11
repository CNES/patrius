/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */
/*
 *
 * HISTORY
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.7:DM:DM-2590:18/05/2021:Configuration des TransformProvider 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:287:21/10/2014:Bug in frame transformation when changing order of two following transformation
 * VERSION::DM:489:12/01/2016:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:524:25/05/2016:serialization java doc
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.transformations;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfiguration;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TAIScale;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.time.TimeStampedCache;
import fr.cnes.sirius.patrius.time.TimeStampedGenerator;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;

/**
 * Transform provider using thread-safe interpolation on transforms sample.
 * <p>
 * The interpolation is a polynomial Hermite interpolation, which can either use or ignore the derivatives provided by
 * the raw provider. This means that simple raw providers that do not compute derivatives can be used, the derivatives
 * will be added appropriately by the interpolation process.
 * 
 * <p>
 * Spin derivative is available and computed if required.
 * </p>
 * <p>Frames configuration is unused.</p>
 *
 * @serial given the attribut {@link TransformProvider} an instance of InterpolatingTransformProvider
 *         is not guaranteed to be serializable
 *         </p>
 * @see TimeStampedCache
 * @author Luc Maisonobe
 */
public class InterpolatingTransformProvider implements TransformProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = -1750070230136582364L;

    /** Provider for raw (non-interpolated) transforms. */
    private final TransformProvider rawProvider;

    /** Flag for use of sample transforms velocities. */
    private final boolean useVelocities;

    /** Flag for use sample points rotation rates. */
    private final boolean useRotationRates;

    /** Earliest supported date. */
    private final AbsoluteDate earliest;

    /** Latest supported date. */
    private final AbsoluteDate latest;

    /** Grid points time step. */
    private final double step;

    /** Cache for sample points. */
    private final transient TimeStampedCache<Transform> cache;

    /**
     * Simple constructor.
     * 
     * @param rawProviderIn
     *        provider for raw (non-interpolated) transforms
     * @param useVelocitiesIn
     *        if true, use sample transforms velocities,
     *        otherwise ignore them and use only positions
     * @param useRotationRatesIn
     *        if true, use sample points rotation rates,
     *        otherwise ignore them and use only rotations
     * @param earliestIn
     *        earliest supported date
     * @param latestIn
     *        latest supported date
     * @param gridPoints
     *        number of interpolation grid points
     * @param stepIn
     *        grid points time step
     * @param maxSlots
     *        maximum number of independent cached time slots
     *        in the {@link TimeStampedCache time-stamped cache}
     * @param maxSpan
     *        maximum duration span in seconds of one slot
     *        in the {@link TimeStampedCache time-stamped cache}
     * @param newSlotInterval
     *        time interval above which a new slot is created
     *        in the {@link TimeStampedCache time-stamped cache}
     */
    public InterpolatingTransformProvider(final TransformProvider rawProviderIn,
        final boolean useVelocitiesIn, final boolean useRotationRatesIn,
        final AbsoluteDate earliestIn, final AbsoluteDate latestIn,
        final int gridPoints, final double stepIn,
        final int maxSlots, final double maxSpan, final double newSlotInterval) {
        this(rawProviderIn, useVelocitiesIn, useRotationRatesIn, earliestIn, latestIn,
            gridPoints, stepIn, maxSlots, maxSpan, newSlotInterval, false);
    }

    /**
     * Simple constructor.
     * 
     * @param rawProviderIn
     *        provider for raw (non-interpolated) transforms
     * @param useVelocitiesIn
     *        if true, use sample transforms velocities,
     *        otherwise ignore them and use only positions
     * @param useRotationRatesIn
     *        if true, use sample points rotation rates,
     *        otherwise ignore them and use only rotations
     * @param earliestIn
     *        earliest supported date
     * @param latestIn
     *        latest supported date
     * @param gridPoints
     *        number of interpolation grid points
     * @param stepIn
     *        grid points time step
     * @param maxSlots
     *        maximum number of independent cached time slots
     *        in the {@link TimeStampedCache time-stamped cache}
     * @param maxSpan
     *        maximum duration span in seconds of one slot
     *        in the {@link TimeStampedCache time-stamped cache}
     * @param newSlotInterval
     *        time interval above which a new slot is created
     * @param computeSpinDerivatives
     *        spin derivatives are computed : true, or not : false
     *        in the {@link TimeStampedCache time-stamped cache}
     */
    public InterpolatingTransformProvider(final TransformProvider rawProviderIn,
        final boolean useVelocitiesIn, final boolean useRotationRatesIn,
        final AbsoluteDate earliestIn, final AbsoluteDate latestIn,
        final int gridPoints, final double stepIn,
        final int maxSlots, final double maxSpan, final double newSlotInterval,
        final boolean computeSpinDerivatives) {
        this.rawProvider = rawProviderIn;
        this.useVelocities = useVelocitiesIn;
        this.useRotationRates = useRotationRatesIn;
        this.earliest = earliestIn;
        this.latest = latestIn;
        this.step = stepIn;
        this.cache = new TimeStampedCache<Transform>(gridPoints, maxSlots, maxSpan, newSlotInterval,
            new Generator(computeSpinDerivatives), Transform.class);
    }

    /**
     * Get the underlying provider for raw (non-interpolated) transforms.
     * 
     * @return provider for raw (non-interpolated) transforms
     */
    public TransformProvider getRawProvider() {
        return this.rawProvider;
    }

    /** {@inheritDoc} */
    @Override
    public Transform getTransform(final AbsoluteDate date) throws PatriusException {
        return this.getTransform(date, FramesFactory.getConfiguration());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Frames configuration is unused.
     * </p>
     */
    @Override
    public Transform getTransform(final AbsoluteDate date, final FramesConfiguration config) throws PatriusException {
        return this.getTransform(date, config, false);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Spin derivative is available and computed if required.
     * </p>
     */
    @Override
    public Transform getTransform(final AbsoluteDate date,
                                  final boolean computeSpinDerivatives) throws PatriusException {
        return this.getTransform(date, FramesFactory.getConfiguration(), computeSpinDerivatives);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Spin derivative is available and computed if required.
     * </p>
     * <p>
     * Frames configuration is unused.
     * </p>
     */
    @Override
    public Transform getTransform(final AbsoluteDate date, final FramesConfiguration config,
                                  final boolean computeSpinDerivatives) throws PatriusException {
        try {
            // retrieve a sample from the thread-safe cache
            final Transform[] sample = this.cache.getNeighbors(date);

            // interpolate to specified date
            return Transform.interpolate(date, this.useVelocities, this.useRotationRates, Arrays.asList(sample),
                computeSpinDerivatives);

        } catch (final PatriusExceptionWrapper oew) {
            // something went wrong while generating the sample,
            // we just forward the exception up
            throw oew.getException();
        }
    }

    /**
     * Replace the instance with a data transfer object for serialization.
     * <p>
     * This intermediate class serializes only the data needed for generation, but does <em>not</em> serializes the
     * cache itself (in fact the cache is not serializable).
     * </p>
     * 
     * @return data transfer object that will be serialized
     */
    private Object writeReplace() {
        return new DataTransferObject(this.rawProvider, this.useVelocities, this.useRotationRates,
            this.earliest, this.latest, this.cache.getNeighborsSize(), this.step,
            this.cache.getMaxSlots(), this.cache.getMaxSpan(), this.cache.getNewSlotQuantumGap());
    }

    /** Internal class used only for serialization. */
    private static final class DataTransferObject implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 8127819004703645170L;

        /** Provider for raw (non-interpolated) transforms. */
        private final TransformProvider rawProvider;

        /** Flag for use of sample transforms velocities. */
        private final boolean useVelocities;

        /** Flag for use sample points rotation rates. */
        private final boolean useRotationRates;

        /** Earliest supported date. */
        private final AbsoluteDate earliest;

        /** Latest supported date. */
        private final AbsoluteDate latest;

        /** Number of grid points. */
        private final int gridPoints;

        /** Grid points time step. */
        private final double step;

        /** Maximum number of independent cached time slots. */
        private final int maxSlots;

        /** Maximum duration span in seconds of one slot. */
        private final double maxSpan;

        /** Time interval above which a new slot is created. */
        private final double newSlotInterval;

        /**
         * Simple constructor.
         * 
         * @param rawProviderIn
         *        provider for raw (non-interpolated) transforms
         * @param useVelocitiesIn
         *        if true, use sample transforms velocities,
         *        otherwise ignore them and use only positions
         * @param useRotationRatesIn
         *        if true, use sample points rotation rates,
         *        otherwise ignore them and use only rotations
         * @param earliestIn
         *        earliest supported date
         * @param latestIn
         *        latest supported date
         * @param gridPointsIn
         *        number of interpolation grid points
         * @param stepIn
         *        grid points time step
         * @param maxSlotsIn
         *        maximum number of independent cached time slots
         *        in the {@link TimeStampedCache time-stamped cache}
         * @param maxSpanIn
         *        maximum duration span in seconds of one slot
         *        in the {@link TimeStampedCache time-stamped cache}
         * @param newSlotIntervalIn
         *        time interval above which a new slot is created
         *        in the {@link TimeStampedCache time-stamped cache}
         */
        private DataTransferObject(final TransformProvider rawProviderIn,
            final boolean useVelocitiesIn, final boolean useRotationRatesIn,
            final AbsoluteDate earliestIn, final AbsoluteDate latestIn,
            final int gridPointsIn, final double stepIn,
            final int maxSlotsIn, final double maxSpanIn, final double newSlotIntervalIn) {
            this.rawProvider = rawProviderIn;
            this.useVelocities = useVelocitiesIn;
            this.useRotationRates = useRotationRatesIn;
            this.earliest = earliestIn;
            this.latest = latestIn;
            this.gridPoints = gridPointsIn;
            this.step = stepIn;
            this.maxSlots = maxSlotsIn;
            this.maxSpan = maxSpanIn;
            this.newSlotInterval = newSlotIntervalIn;
        }

        /**
         * Replace the deserialized data transfer object with a {@link InterpolatingTransformProvider}.
         * 
         * @return replacement {@link InterpolatingTransformProvider}
         */
        private Object readResolve() {
            // build a new provider, with an empty cache
            return new InterpolatingTransformProvider(this.rawProvider, this.useVelocities, this.useRotationRates,
                this.earliest, this.latest, this.gridPoints, this.step,
                this.maxSlots, this.maxSpan, this.newSlotInterval);
        }

    }

    /** Local generator for thread-safe cache. */
    private class Generator implements TimeStampedGenerator<Transform> {

        /** Spin derivatives are computed : true, or not : false. **/
        private final boolean computeSpinDerivatives;

        /**
         * Simple constructor.
         * 
         * @param computeSpinDeriv
         *        spin derivatives are computed : true, or not : false.
         **/
        public Generator(final boolean computeSpinDeriv) {
            this.computeSpinDerivatives = computeSpinDeriv;
        }

        /** {@inheritDoc} */
        @Override
        public List<Transform> generate(final Transform existing, final AbsoluteDate date) {

            try {
                final List<Transform> generated = new ArrayList<Transform>();

                if (existing == null) {

                    final TAIScale tai = TimeScalesFactory.getTAI();
                    final double tCenter =
                        InterpolatingTransformProvider.this.step
                            * MathLib.floor(date.getComponents(tai).getTime().getSecondsInDay()
                                / InterpolatingTransformProvider.this.step);

                    // compute a new date depending on the step but not on the current date:
                    final AbsoluteDate newDate = new AbsoluteDate(new AbsoluteDate(date.getComponents(tai).getDate(),
                        new TimeComponents(0, 0, 0), tai), tCenter);

                    // no prior existing transforms, just generate a first set
                    for (int i = 0; i < InterpolatingTransformProvider.this.cache.getNeighborsSize(); ++i) {
                        generated.add(InterpolatingTransformProvider.this.rawProvider.getTransform(
                            newDate.shiftedBy(i * InterpolatingTransformProvider.this.step),
                            this.computeSpinDerivatives));
                    }
                } else {
                    // some transforms have already been generated
                    // add the missing ones up to specified date

                    AbsoluteDate t = existing.getDate();
                    if (date.compareTo(t) > 0) {
                        // forward generation
                        do {
                            t = t.shiftedBy(InterpolatingTransformProvider.this.step);
                            generated.add(generated.size(), InterpolatingTransformProvider.this.rawProvider
                                .getTransform(t, this.computeSpinDerivatives));
                        } while (t.compareTo(date) <= 0);
                    } else {
                        // backward generation
                        do {
                            t = t.shiftedBy(-InterpolatingTransformProvider.this.step);
                            generated.add(0, InterpolatingTransformProvider.this.rawProvider.getTransform(t,
                                this.computeSpinDerivatives));
                        } while (t.compareTo(date) >= 0);
                    }
                }

                // return the generated transforms
                return generated;
            } catch (final PatriusException oe) {
                throw new PatriusExceptionWrapper(oe);
            }
        }
    }
}
