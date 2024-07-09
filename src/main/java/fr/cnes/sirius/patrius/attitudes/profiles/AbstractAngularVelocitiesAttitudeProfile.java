/**
 * Copyright 2011-2017 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * HISTORY
* VERSION:4.7:DM:DM-2653:18/05/2021:generalisation des sequences et correction/refonte des sequences de segments 
 * VERSION:4.5:FA:FA-2337:27/05/2020:Methode truncate() de AbstractLegsSequence 
* VERSION:4.4:DM:DM-2231:04/10/2019:[PATRIUS] Creation d'un cache dans les profils de vitesse angulaire
 * VERSION:4.4:DM:DM-2097:04/10/2019:[PATRIUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 */

package fr.cnes.sirius.patrius.attitudes.profiles;

import java.io.Serializable;
import java.util.Map;

import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.complex.Quaternion;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3DFunction;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * <p>
 * An attitude profile which is defined by its angular velocity whose x-y-z components are represented with an
 * underlying {@link Vector3DFunction}. The attitude orientation is computed integrating that angular velocity.
 *
 * @author Pierre Brechard
 *
 * @since 4.4
 */
@SuppressWarnings("PMD.NullAssignment")
public abstract class AbstractAngularVelocitiesAttitudeProfile extends AbstractAttitudeProfile {

    /**
     * Attitude Angular Velocity Integration types.
     */
    public enum AngularVelocityIntegrationType {

        /** Wilcox 1st order. */
        WILCOX_1,

        /** Wilcox 2nd order. */
        WILCOX_2,

        /** Wilcox 3rd order. */
        WILCOX_3,

        /** Wilcox 4th order. */
        WILCOX_4,

        /** Edwards: Wilcox 3rd order and commutation correction. */
        EDWARDS;
    }

    /** Serialization UID. */
    private static final long serialVersionUID = -2071073528335086278L;

    /** Default value for the number of integration steps performed between 2 values stored in the underlying cache. */
    private static final int DEFAULT_CACHE_FREQ = 100;
    
    /** Constant for computation during Wilcox integration step*/
    private static final double CONST1 = 8.;
    
    /** Constant for computation during Wilcox integration step*/
    private static final double CONST2 = 24.;

    /** Epsilon for dates comparison. */
    private static final double DATE_EPSILON = 1E-9;
    
    /** Angular velocity wrapper {@link Vector3DFunction}. */
    protected Vector3DFunction spinVectorFunction;

    /** Frame of initial rotation and Fourier series. */
    protected final Frame refFrame;

    /** Initial rotation. */
    protected Rotation rotationInitial;

    /** Reference date. */
    protected AbsoluteDate dateRef;

    /** Integration type. */
    protected final AngularVelocityIntegrationType type;

    /** Integration step. */
    protected final double integStep;

    /** Cache frequency. */
    protected final int cacheFreq;
    
    /** Boolean for spin derivative computation. */
    protected boolean spinDerivativesComputation;

    /** Underlying cache for the computed attitude rotations. */
    private final RotationCache cache;

    /**
     * Constructor.
     * @param spinVectorFunction
     *        Angular velocity wrapper {@link Vector3DFunction}
     * @param frame
     *        frame where initial rotation and the x,y,z components are expressed
     * @param interval
     *        interval of profile
     * @param rotationRef
     *        rotation at reference date
     * @param dateRef
     *        Reference date
     * @param integType
     *        integration type (see {@link AngularVelocityIntegrationType})
     * @param integStep
     *        integration step
     * @param cacheFreq
     *        Number of integration steps performed between two values stored in the underlying cache
     * @param nature
     *        Nature
     * @throws PatriusException
     *         thrown if the reference date is after the lower bound of the interval to be covered by the profile
     */
    public AbstractAngularVelocitiesAttitudeProfile(final Vector3DFunction spinVectorFunction, final Frame frame,
            final AbsoluteDateInterval interval, final Rotation rotationRef, final AbsoluteDate dateRef,
            final AngularVelocityIntegrationType integType, final double integStep, final int cacheFreq,
            final String nature) throws PatriusException {
        super(interval, nature);

        // Verify that the reference date is equal or before the lower bound of the covered interval
        if (dateRef.compareTo(interval.getLowerData()) > 0) {
            throw new PatriusException(PatriusMessages.INVALID_ANG_VEL_ATT_PROFILE_REF_DATE, dateRef, interval);
        }

        this.spinVectorFunction = spinVectorFunction;
        this.refFrame = frame;
        this.rotationInitial = rotationRef;
        this.dateRef = dateRef;
        this.type = integType;
        this.integStep = integStep;
        this.spinDerivativesComputation = false;
        this.cacheFreq = cacheFreq;

        // Initialize cache and add the reference value
        this.cache = new RotationCache(dateRef, cacheFreq * integStep, interval);
        this.cache.put(dateRef, rotationRef);
    }

    /**
     * Constructor.
     * @param spinVectorFunction
     *        Angular velocity wrapper {@link Vector3DFunction}
     * @param frame
     *        frame where initial rotation and the x,y,z components are expressed
     * @param interval
     *        interval of profile
     * @param rotationRef
     *        rotation at reference date
     * @param dateRef
     *        Reference date
     * @param integType
     *        integration type (see {@link AngularVelocityIntegrationType})
     * @param integStep
     *        integration step
     * @param nature
     *        Nature
     * @throws PatriusException
     *         thrown if the reference date is after the lower bound of the interval to be covered by the profile
     */
    public AbstractAngularVelocitiesAttitudeProfile(final Vector3DFunction spinVectorFunction, final Frame frame,
            final AbsoluteDateInterval interval, final Rotation rotationRef, final AbsoluteDate dateRef,
            final AngularVelocityIntegrationType integType, final double integStep, final String nature)
            throws PatriusException {
        this(spinVectorFunction, frame, interval, rotationRef, dateRef, integType, integStep, DEFAULT_CACHE_FREQ,
                nature);
    }

    /** {@inheritDoc} */
    @Override
    public Attitude getAttitude(final PVCoordinatesProvider pvProv, final AbsoluteDate date, final Frame frame)
            throws PatriusException {

        // Check if date is contained
        checkDate(date);

        // Tranform from user specified frame to reference frame
        final Transform frameToRefFrameTransform = frame.getTransformTo(this.refFrame, date);

        // Integration of spin - orientation in reference frame
        final Rotation orientation = getOrientation(date);

        // Spin
        final Vector3D spin = this.spinVectorFunction.getVector3D(date);

        // Rotation acceleration
        Vector3D rotAcc = null;
        if (this.spinDerivativesComputation) {
            rotAcc = this.spinVectorFunction.nthDerivative(1).getVector3D(date);
        }

        // Transforms
        final Transform refFrameToSatFrameTransform = new Transform(date, orientation, spin, rotAcc);
        final Transform frameToSatFrameTransform = new Transform(date, frameToRefFrameTransform,
                refFrameToSatFrameTransform, this.spinDerivativesComputation);

        // Extract Orientation, Spin and Rotation acceleration
        final Rotation orientationInFrame = frameToSatFrameTransform.getRotation();
        final Vector3D spinWrtFrameInSatFrame = frameToSatFrameTransform.getRotationRate();
        final Vector3D rotAccWrtFrameInSatFrame = frameToSatFrameTransform.getRotationAcceleration();

        return new Attitude(date, frame, orientationInFrame, spinWrtFrameInSatFrame, rotAccWrtFrameInSatFrame);
    }

    /** {@inheritDoc} */
    @Override
    public void setSpinDerivativesComputation(final boolean computeSpinDerivatives) {
        this.spinDerivativesComputation = computeSpinDerivatives;
    }

    /**
     * Gets the orientation from the angular velocity function at a given date, integrating the function.
     *
     * @param date
     *        the date
     * @return the orientation at a given date
     *
     * @throws PatriusException
     *         thrown if the date is outside the covered interval
     */
    protected Rotation getOrientation(final AbsoluteDate date) throws PatriusException {
        // Check if date is contained
        checkDate(date);

        // Time stamped rotation
        final TimeStampedRotation dateRotation = this.cache.get(date);

        final Rotation rotation;

        if (dateRotation == null) {
            // If there is no time stamped rotation at date, use initial rotation and ref date to integrate spin
            rotation = integrateSpin(this.rotationInitial, this.dateRef, date);
        } else {
            if (date.equals(dateRotation.getDate())) {
                rotation = dateRotation;
            } else {
                // Get rotation from the initial rotation dateRotation
                rotation = integrateSpin(dateRotation, dateRotation.getDate(), date);
            }
        }

        return rotation;
    }

    /**
     * Integrates the angular velocity spin function to obtain the attitude rotation.
     *
     * @param initOrientation
     *        rotation at initial date
     * @param initDate
     *        initial date
     * @param finalDate
     *        final date (can be after or before the initial date)
     * @return the computed rotation at final date
     */
    private Rotation integrateSpin(final Rotation initOrientation, final AbsoluteDate initDate,
            final AbsoluteDate finalDate) {

        // Initialization (variable required to store the integrated spin in Edwards-Wilcox algorithm)
        AngularCoordinates currentAngCoor = new AngularCoordinates(initOrientation, Vector3D.ZERO);

        // Time-span over which integration is to be performed
        final double span = finalDate.preciseDurationFrom(initDate);

        // Perform integration if initial and final dates are not equal
        if (MathLib.abs(span) > DATE_EPSILON) {
            // Compute the number of steps required to perform the integration
            final int numSteps = computeNumOfIntegrationSteps(span);

            AbsoluteDate currentDate = initDate;
            final double offset = initDate.preciseDurationFrom(this.dateRef);
            double t0 = 0.;
            for (int step = 1; step < numSteps; ++step) {
                // Compute rotation
                currentAngCoor = integrationStep(currentAngCoor, t0 + offset, this.integStep);

                // Advance (multiplication to avoid addition of numerical errors in long loops)
                t0 = step * this.integStep;
                currentDate = initDate.shiftedBy(t0);

                // Put value in the cache
                this.cache.put(currentDate, currentAngCoor.getRotation());
            }

            // Perform last step of the integration
            final double dtLast = span - t0;
            currentAngCoor = integrationStep(currentAngCoor, t0 + offset, dtLast);
            currentDate = currentDate.shiftedBy(dtLast);

            // Put value in the cache
            this.cache.put(currentDate, currentAngCoor.getRotation());
        }

        return currentAngCoor.getRotation();
    }

    /**
     * Computes the number of steps required to perform the integration including the final date step and taking into
     * account numerical
     * rounding errors.
     *
     * @param span
     *        Time-span to be covered
     * @return the number of integration steps
     */
    private int computeNumOfIntegrationSteps(final double span) {
        int numSteps = (int) MathLib.ceil(span / this.integStep);
        if (MathLib.abs(((numSteps - 1) * this.integStep) - span) < DATE_EPSILON) {
            --numSteps;
        }
        return numSteps;
    }

    /**
     * Computes a step of the spin integration to obtain the rotation depending on the integration type. All the
     * "magic numbers" here are
     * justified for they are used in mathematical formulas.
     *
     * @param angCoordInit
     *        the orientation rotation at t<sub>init</sub> (and the integrated spin value required for Edwards
     *        integration)
     * @param t0
     *        Start time of the integration
     * @param dt
     *        Time step (can be positive or negative, but never zero)
     * @return The integrated angular coordinates
     */
    private AngularCoordinates integrationStep(final AngularCoordinates angCoordInit, final double t0,
            final double dt) {

        final Rotation currentRot;
        final AngularCoordinates currentAngCoor;
        switch (this.type) {
            case WILCOX_1:
                // Wilcox 1
                currentRot = wilcoxStep(angCoordInit.getRotation(), t0, dt, 1);
                currentAngCoor = new AngularCoordinates(currentRot, Vector3D.ZERO);
                break;

            case WILCOX_2:
                // Wilcox 2
                currentRot = wilcoxStep(angCoordInit.getRotation(), t0, dt, 2);
                currentAngCoor = new AngularCoordinates(currentRot, Vector3D.ZERO);
                break;

            case WILCOX_3:
                // Wilcox 3
                currentRot = wilcoxStep(angCoordInit.getRotation(), t0, dt, 3);
                currentAngCoor = new AngularCoordinates(currentRot, Vector3D.ZERO);
                break;

            case WILCOX_4:
                // Wilcox 4
                currentRot = wilcoxStep(angCoordInit.getRotation(), t0, dt, 4);
                currentAngCoor = new AngularCoordinates(currentRot, Vector3D.ZERO);
                break;

            case EDWARDS:
                // Edwards
                currentAngCoor = edwardsStep(angCoordInit, t0, dt);
                break;

            default:
                // Cannot happen
                throw new EnumConstantNotPresentException(AngularVelocityIntegrationType.class, this.type.name());
        }

        return currentAngCoor;
    }

    /**
     * Perform a Wilcox integration step. All the "magic numbers" here are justified for they are used in mathematical
     * formulas.
     *
     * @param rotationInit
     *        the rotation at the initial date
     * @param t0
     *        the initial date t<sub>0</sub>
     * @param dt
     *        the integration duration
     * @param order
     *        the order of the computation (available : 1 to 4)
     * @return the final rotation at t<sub>0</sub> + dt
     */
    private Rotation wilcoxStep(final Rotation rotationInit, final double t0, final double dt, final int order) {

        // initialisations
        final Vector3D multVvector;
        final double multScalar;

        // spin integration
        final Vector3D integratedSpin = this.spinVectorFunction.integral(t0, t0 + dt);
        final double integratedSpinNorm = integratedSpin.getNorm();

        // switch on the order
        switch (order) {
            case 1:
                multScalar = 1.;
                multVvector = integratedSpin.scalarMultiply(1. / 2.);
                break;

            case 2:
                multScalar = 1. - ((integratedSpinNorm * integratedSpinNorm) / CONST1);
                multVvector = integratedSpin.scalarMultiply(1. / 2.);
                break;

            case 3:
                multScalar = 1. - ((integratedSpinNorm * integratedSpinNorm) / CONST1);
                multVvector = integratedSpin
                        .scalarMultiply((1. - ((integratedSpinNorm * integratedSpinNorm) / CONST2)) / 2.);
                break;

            case 4:
                multScalar = (1. - ((integratedSpinNorm * integratedSpinNorm) / CONST1))
                        + (MathLib.pow(integratedSpinNorm / 2., 4) / CONST2);
                multVvector = integratedSpin
                        .scalarMultiply((1. - ((integratedSpinNorm * integratedSpinNorm) / CONST2)) / 2.);
                break;

            default:
                // Cannot happen
                throw PatriusException.createIllegalArgumentException(PatriusMessages.OUT_OF_RANGE_ORDER);
        }

        // Initial quaternion
        final Quaternion initQuat = rotationInit.getQuaternion();

        // Final quaternion and rotation creation
        final Quaternion multQuat = new Quaternion(multScalar, multVvector.toArray());
        final Rotation result;
        if (multQuat.getNorm() < Precision.EPSILON) {
            result = new Rotation(false, initQuat);
        } else {
            result = new Rotation(false, Quaternion.multiply(initQuat, multQuat).normalize());
        }

        return result;
    }

    /**
     * Perform an Edwards integration step. All the "magic numbers" here are justified for they are used in mathematical
     * formulas.
     *
     * @param angCoordInit
     *        the rotation at the initial date
     * @param t0
     *        the initial date t<sub>0</sub>
     * @param dt
     *        the integration duration
     * @return the computed rotation and spin at t<sub>0</sub> + dt
     */
    private AngularCoordinates edwardsStep(final AngularCoordinates angCoordInit, final double t0, final double dt) {

        // spin integration
        final Vector3D integratedSpin = this.spinVectorFunction.integral(t0, t0 + dt);
        final double integratedSpinNorm = integratedSpin.getNorm();

        // initial quaternion
        final Quaternion initQuat = angCoordInit.getRotation().getQuaternion();

        // commutation error
        final Vector3D commutation = Vector3D.crossProduct(angCoordInit.getRotationRate().scalarMultiply(1. / 2.),
                integratedSpin.scalarMultiply(1. / 2.)).scalarMultiply(1. / 12.);

        final double multScalar = 1. - ((integratedSpinNorm * integratedSpinNorm) / 8.);
        final Vector3D multVvector = integratedSpin.scalarMultiply(
                (1. - ((integratedSpinNorm * integratedSpinNorm) / CONST2)) / 2.).add(commutation);

        // final quaternion and rotation creation
        final Quaternion multQuat = new Quaternion(multScalar, multVvector.toArray());
        final Rotation rot = new Rotation(false, Quaternion.multiply(initQuat, multQuat).normalize());

        // the computed rotation and spin at t0 + dt
        return new AngularCoordinates(rot, integratedSpin);
    }

    /**
     * Removes all of the elements from the orientation rotation cache
     */
    public void clearCache() {
        this.cache.clear();
    }

    /**
     * <p>
     * Class used to cache the computed attitude rotation values for optimization purposes.
     * <p>
     * For performance purposes this cache relies on an underlying array which contains all the stored values separated
     * at a fixed time-step. The cache size is defined such that it can contain all the points in the covered interval
     * at a fixed step, including one extra point before the lower and after the upper bounds of that interval. The
     * first index of the cache corresponds to the date t<sub>0</sub> which is lower or equal to the covering interval.
     * Moreover, the indices in the array correspond to the number of the caching time-step's from the first date. When
     * a value for a certain date has not already been stored, a {@code null} entry is contained in the list.
     * <p>
     * <b>Note</b>: Since this is an inner class {@link Map} is not implemented in order to avoid defining all the
     * methods of the interface which will never be used.
     *
     * @author Pierre Brechard
     *
     * @since 4.4
     *
     */
    private static final class RotationCache implements Serializable {

        /** Serialization UID. */
        private static final long serialVersionUID = -8299725230444257289L;

        /** Underlying elements stored in the cache. */
        private final TimeStampedRotation[] elements;

        /** Date of the first element of the cache. */
        private final AbsoluteDate date0;

        /** Time-step between two elements stored in the cache. */
        private final double step;

        /**
         * Index of the last element in the cache which contains a non-null value. It can be interpreted as a measure
         * of the cache size.
         */
        private int indexLast;

        /**
         * Constructor.
         * @param dateRef
         *        Date used as reference. Entries of the cache will be an integer number of caching steps from this
         *        date.
         * @param cachingStep
         *        Time-step between the elements to be stored in the cache
         * @param interval
         *        Time interval to be covered by the cache
         */
        public RotationCache(final AbsoluteDate dateRef, final double cachingStep,
                final AbsoluteDateInterval interval) {

            // Compute the first date to be contained in the cache such that it is separated an integer number of
            // caching steps from the
            // input reference date. The closest date to the beginning of the interval to be covered is chosen.
            final double offSet = dateRef.preciseDurationFrom(interval.getLowerData()) % cachingStep;
            if (MathLib.abs(offSet) < (cachingStep / 2.)) {
                this.date0 = interval.getLowerData().shiftedBy(offSet);
            } else {
                this.date0 = interval.getLowerData().shiftedBy(offSet - MathLib.copySign(cachingStep, offSet));
            }

            // Compute the cache size to cover the whole interval with the last point being the closest to step to the
            // interval upper bound
            final int cacheSize = (int) MathLib.round(interval.getUpperData().preciseDurationFrom(this.date0)
                    / cachingStep) + 1;

            // Initialize the cache to the computed size with all the elements to NULL
            this.elements = new TimeStampedRotation[cacheSize];
            this.step = cachingStep;
            this.indexLast = -1;
        }

        /**
         * Gets the {@link Rotation} value stored in the cache which is at the closest date from the given date. Since
         * by construction the
         * cache is filled sequentially from the beginning (reference date) onwards, {@code null} can be returned in two
         * cases:
         * <ul>
         * <li>The cache is empty</li>
         * <li>Input date is outside the bounds of the cache</li>
         * </ul>
         *
         * @param date
         *        Date - key
         * @return the {@link Rotation} stored in the cache at the closest date from the given date or {@code null}
         */
        public TimeStampedRotation get(final AbsoluteDate date) {

            // Initialize the value associated with the date key to NULL
            TimeStampedRotation value = null;

            // Get the closest value from the cache (only if index is within its bounds)
            int index = getIndex(date);
            if ((this.indexLast >= 0) && isValidIndex(index)) {
                if (index > this.indexLast) {
                    index = this.indexLast;
                }

                // By construction at this point a valid index is used
                value = this.elements[index];
            }

            return value;
        }

        /**
         * Associates the specified rotation value with the specified date in this cache. If the cache previously
         * contained a mapping for
         * the key, the old value is replaced by the specified value.
         * <p>
         * <b>NOTE</b>: By construction, the parent class invokes this method sequentially from the reference date,
         * hence the cache is filled from the beginning onwards
         *
         * @param date
         *        Key {@link AbsoluteDate} with which the specified value is to be associated
         * @param value
         *        rotation to be associated with the key
         */
        public void put(final AbsoluteDate date, final Rotation value) {

            // Get index
            final int index = getIndex(date);
            final double doubleIndex = date.preciseDurationFrom(this.date0) / this.step;

            // Process the date-rotation pair only if the date is contained in the caching bounds and at a valid step
            // point
            if (isValidIndex(index) && (MathLib.abs(index - doubleIndex) < Precision.DOUBLE_COMPARISON_EPSILON)) {

                // Put the value in the cache if there is not an entry for this date
                if (this.elements[index] == null) {
                    this.elements[index] = new TimeStampedRotation(value, date);

                    // Update last index. By construction entries are put in the cache sequentially from the reference
                    // date onwards.
                    this.indexLast = index;
                }
            }
        }

        /**
         * Removes all of the elements from this cache setting them to {@code null}
         */
        public void clear() {
            for (int i = 0; i < this.elements.length; i++) {
                this.elements[i] = null;
            }
        }

        /**
         * Gets the index of the given date in the underlying array of the cache. The index corresponds to the closest
         * entry whose date is
         * equal or lower than the input date.
         *
         * @param date
         *        Date to be checked
         * @return the closest index to the given date
         */
        private int getIndex(final AbsoluteDate date) {
            return (int) MathLib.floor(date.durationFrom(this.date0) / this.step);
        }

        /**
         * Checks if the give index is within the bounds of validity of the underlying array of the cache
         *
         * @param index
         *        Index to be checked
         * @return <code>true</code> if the index is valid
         */
        private boolean isValidIndex(final int index) {
            return (index >= 0) && (index <= (this.elements.length - 1));
        }
    }
}
