/**
 * 
 * Copyright 2011-2022 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2465:27/05/2020:Refactoring calculs de ralliements
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes.slew;

import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.TabulatedSlew;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.linear.BlockRealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.SingularValueDecomposition;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 *
 **/
/**
 * Class for ISIS spin bias slew computation: slew with trapezoidal angular velocity profile calculated in GCRF.
 * Computation of slew returns a {@link TabulatedSlew}.
 * 
 * @author Emmanuel Bignon
 * 
 * @since 4.5
 */
public class IsisSpinBiasSlewComputer {

    /** Default maximum number of iterations allowed for slew duration computation's convergence. */
    private static final int DEFAUT_MAX_ITERATIONS = 25;

    /** Default nature. */
    private static final String DEFAULT_NATURE = "ATTITUDE_ISIS_SPIN_BIAS_SLEW";

    /** Reference frame. */
    private final Frame refFrame;

    /** Time step. */
    private final double dtSCAO;

    /** Maximum slew angular amplitude allowed. */
    private final double thetaMaxAllowed;

    /** Maximum duration expected for the slew, including the tranquilisation phase. */
    private final double durationMax;

    /** Convergence threshold for the iterative computation of the slew duration. */
    private final double dtConvergenceThreshold;

    /** Satellite inertia matrix in satellite reference frame. */
    private final double[][] inertiaMatrix;

    /** Torque allocation for each RW during the acceleration phase. */
    private final double rwTorqueAllocAccel;

    /** Torque allocation for each RW during the deceleration phase. */
    private final double rwTorqueAllocDecel;

    /** Angular momentum allocation for each RW during the manoeuvre. */
    private final double rwDeltaMomentumAlloc;

    /**
     * cosine directors matrix of the reaction wheels in the satellite reference frame. Matrix
     * 3xN_RW with N_RW wheels (Reaction wheels spin axes written in column in the satellite reference frame).
     */
    private final double[][] rwMatrix;

    /** Tranquilisation time after the end of the slew. */
    private final double tranquillisationTime;

    /** Maximum number of iterations allowed for slew duration computation's convergence. */
    private final double maxIterationsNumber;

    /** Nature. */
    private final String nature;

    // Class variables (computed after slew computation)

    /** Angle of the rotation associated to the spin bias slew. */
    private double slewAngle;

    /** Axis of the rotation associated to the spin bias slew. */
    private Vector3D slewAxis;

    /** Duration of the spin bias slew, excluding the tranquillisation phase. */
    private double durationWoTranq;

    /** Value of the acceleration step during the first phase. */
    private double accelMax;

    /** Value of the deceleration step during the last phase (assumed > 0). */
    private double decelMax;

    /** Duration of the acceleration phase. */
    private double accelDuration;

    /** Duration of the deceleration phase. */
    private double decelDuration;

    /**
     * Constructor with default maximum number of iterations allowed for slew duration computation's convergence.
     * 
     * @param dtSCAOIn
     *        time step (s)
     * @param thetaMaxAllowedIn
     *        maximum slew angular amplitude allowed (rad)
     * @param durationMaxIn
     *        maximum duration expected for the slew, including the tranquilisation phase (s)
     * @param dtConvergenceThresholdIn
     *        convergence threshold for the iterative computation of the slew duration (s)
     * @param inertiaMatrixIn
     *        satellite inertia matrix in satellite reference frame (kg.m^2)
     * @param rwTorqueAllocAccelIn
     *        torque allocation for each RW during the acceleration phase (N.m)
     * @param rwTorqueAllocDecelIn
     *        torque allocation for each RW during the deceleration phase (assumed > 0) (N.m)
     * @param rwDeltaMomentumAllocIn
     *        angular momentum allocation for each RW during the manoeuvre (N.m.s)
     * @param rwMatrixIn
     *        cosine directors matrix of the reaction wheels in the satellite reference frame. Matrix
     *        3xN_RW with N_RW wheels (Reaction wheels spin axes written in column in the satellite reference
     *        frame).
     * @param tranquillisationTimeIn
     *        tranquilisation time after the end of the slew (s)
     */
    public IsisSpinBiasSlewComputer(final double dtSCAOIn,
                                    final double thetaMaxAllowedIn, final double durationMaxIn,
                                    final double dtConvergenceThresholdIn,
                                    final double[][] inertiaMatrixIn, final double rwTorqueAllocAccelIn,
                                    final double rwTorqueAllocDecelIn,
                                    final double rwDeltaMomentumAllocIn, final double[][] rwMatrixIn,
                                    final double tranquillisationTimeIn) {
        this(dtSCAOIn, thetaMaxAllowedIn, durationMaxIn,
                dtConvergenceThresholdIn, inertiaMatrixIn, rwTorqueAllocAccelIn, rwTorqueAllocDecelIn,
                rwDeltaMomentumAllocIn, rwMatrixIn,
                tranquillisationTimeIn, DEFAUT_MAX_ITERATIONS, DEFAULT_NATURE);
    }

    /**
     * Constructor.
     * 
     * @param dtSCAOIn
     *        time step (s)
     * @param thetaMaxAllowedIn
     *        maximum slew angular amplitude allowed (rad)
     * @param durationMaxIn
     *        maximum duration expected for the slew, including the tranquilisation phase (s)
     * @param dtConvergenceThresholdIn
     *        convergence threshold for the iterative computation of the slew duration (s)
     * @param inertiaMatrixIn
     *        satellite inertia matrix in satellite reference frame (kg.m^2)
     * @param rwTorqueAllocAccelIn
     *        torque allocation for each RW during the acceleration phase (N.m)
     * @param rwTorqueAllocDecelIn
     *        torque allocation for each RW during the deceleration phase (assumed > 0) (N.m)
     * @param rwDeltaMomentumAllocIn
     *        angular momentum allocation for each RW during the manoeuvre (N.m.s)
     * @param rwMatrixIn
     *        cosine directors matrix of the reaction wheels in the satellite reference frame. Matrix
     *        3xN_RW with N_RW wheels (Reaction wheels spin axes written in column in the satellite reference
     *        frame).
     * @param tranquillisationTimeIn
     *        tranquilisation time after the end of the slew (s)
     * @param maxIterationsNumberIn
     *        maximum number of iterations allowed for slew duration computation's convergence
     */
    public IsisSpinBiasSlewComputer(final double dtSCAOIn,
                                    final double thetaMaxAllowedIn, final double durationMaxIn,
                                    final double dtConvergenceThresholdIn,
                                    final double[][] inertiaMatrixIn, final double rwTorqueAllocAccelIn,
                                    final double rwTorqueAllocDecelIn,
                                    final double rwDeltaMomentumAllocIn, final double[][] rwMatrixIn,
                                    final double tranquillisationTimeIn,
                                    final int maxIterationsNumberIn) {
        this(dtSCAOIn, thetaMaxAllowedIn, durationMaxIn, dtConvergenceThresholdIn, inertiaMatrixIn,
                rwTorqueAllocAccelIn, rwTorqueAllocDecelIn, rwDeltaMomentumAllocIn, rwMatrixIn, tranquillisationTimeIn,
                maxIterationsNumberIn, DEFAULT_NATURE);
    }

    /**
     * Constructor with default maximum number of iterations allowed for slew duration computation's
     * convergence.
     * 
     * @param dtSCAOIn time step (s)
     * @param thetaMaxAllowedIn maximum slew angular amplitude allowed (rad)
     * @param durationMaxIn maximum duration expected for the slew, including the tranquilisation
     *        phase (s)
     * @param dtConvergenceThresholdIn convergence threshold for the iterative computation of the
     *        slew duration (s)
     * @param inertiaMatrixIn satellite inertia matrix in satellite reference frame (kg.m^2)
     * @param rwTorqueAllocAccelIn torque allocation for each RW during the acceleration phase (N.m)
     * @param rwTorqueAllocDecelIn torque allocation for each RW during the deceleration phase
     *        (assumed > 0) (N.m)
     * @param rwDeltaMomentumAllocIn angular momentum allocation for each RW during the manoeuvre
     *        (N.m.s)
     * @param rwMatrixIn cosine directors matrix of the reaction wheels in the satellite reference
     *        frame. Matrix 3xN_RW with N_RW wheels (Reaction wheels spin axes written in column in
     *        the satellite reference frame).
     * @param tranquillisationTimeIn tranquilisation time after the end of the slew (s)
     * @param natureIn leg nature
     */
    public IsisSpinBiasSlewComputer(final double dtSCAOIn,
                                    final double thetaMaxAllowedIn,
                                    final double durationMaxIn, final double dtConvergenceThresholdIn,
                                    final double[][] inertiaMatrixIn, final double rwTorqueAllocAccelIn,
                                    final double rwTorqueAllocDecelIn, final double rwDeltaMomentumAllocIn,
                                    final double[][] rwMatrixIn, final double tranquillisationTimeIn,
                                    final String natureIn) {
        this(dtSCAOIn, thetaMaxAllowedIn, durationMaxIn, dtConvergenceThresholdIn, inertiaMatrixIn,
                rwTorqueAllocAccelIn, rwTorqueAllocDecelIn, rwDeltaMomentumAllocIn, rwMatrixIn, tranquillisationTimeIn,
                DEFAUT_MAX_ITERATIONS, natureIn);
    }

    /**
     * Constructor.
     * 
     * @param dtSCAOIn time step (s)
     * @param thetaMaxAllowedIn maximum slew angular amplitude allowed (rad)
     * @param durationMaxIn maximum duration expected for the slew, including the tranquilisation
     *        phase (s)
     * @param dtConvergenceThresholdIn convergence threshold for the iterative computation of the
     *        slew duration (s)
     * @param inertiaMatrixIn satellite inertia matrix in satellite reference frame (kg.m^2)
     * @param rwTorqueAllocAccelIn torque allocation for each RW during the acceleration phase (N.m)
     * @param rwTorqueAllocDecelIn torque allocation for each RW during the deceleration phase
     *        (assumed > 0) (N.m)
     * @param rwDeltaMomentumAllocIn angular momentum allocation for each RW during the manoeuvre
     *        (N.m.s)
     * @param rwMatrixIn cosine directors matrix of the reaction wheels in the satellite reference
     *        frame. Matrix 3xN_RW with N_RW wheels (Reaction wheels spin axes written in column in
     *        the satellite reference frame).
     * @param tranquillisationTimeIn tranquilisation time after the end of the slew (s)
     * @param maxIterationsNumberIn maximum number of iterations allowed for slew duration
     *        computation's convergence
     * @param natureIn leg nature
     */
    public IsisSpinBiasSlewComputer(final double dtSCAOIn,
                                    final double thetaMaxAllowedIn,
                                    final double durationMaxIn, final double dtConvergenceThresholdIn,
                                    final double[][] inertiaMatrixIn, final double rwTorqueAllocAccelIn,
                                    final double rwTorqueAllocDecelIn, final double rwDeltaMomentumAllocIn,
                                    final double[][] rwMatrixIn, final double tranquillisationTimeIn,
                                    final int maxIterationsNumberIn, final String natureIn) {

        // Attributes
        this.refFrame = FramesFactory.getGCRF();
        this.dtSCAO = dtSCAOIn;
        this.thetaMaxAllowed = thetaMaxAllowedIn;
        this.durationMax = durationMaxIn;
        this.dtConvergenceThreshold = dtConvergenceThresholdIn;
        this.inertiaMatrix = inertiaMatrixIn;
        this.rwTorqueAllocAccel = rwTorqueAllocAccelIn;
        this.rwTorqueAllocDecel = rwTorqueAllocDecelIn;
        this.rwDeltaMomentumAlloc = rwDeltaMomentumAllocIn;
        this.rwMatrix = rwMatrixIn;
        this.tranquillisationTime = tranquillisationTimeIn;
        this.maxIterationsNumber = maxIterationsNumberIn;
        this.nature = natureIn;

        // Class variables (computed after slew computation)
        this.slewAngle = Double.NaN;
        this.slewAxis = Vector3D.NaN;
        this.durationWoTranq = Double.NaN;
        this.accelMax = Double.NaN;
        this.decelMax = Double.NaN;
        this.accelDuration = Double.NaN;
        this.decelDuration = Double.NaN;
    }

    /**
     * Compute the slew (analytical version).
     * @param pvProv satellite PV coordinates through time
     * @param initialLaw initial attitude law (before the slew)
     * @param initialDateIn slew start date (null if slew defined with its end date)
     * @param finalLaw final attitude law (after the slew)
     * @param finalDate slew end date (null if slew defined with its start date)
     * @return built slew (analytical version)
     * @throws PatriusException thrown if computation failed or if dates are both null or not null
     */
    public TabulatedSlew computeAnalytical(final PVCoordinatesProvider pvProv, final AttitudeProvider initialLaw,
            final AbsoluteDate initialDateIn,
            final AttitudeProvider finalLaw,
            final AbsoluteDate finalDate) throws PatriusException {

        // Check type of slew
        checkInputs(initialDateIn, finalDate);

        // Compute slew duration
        final double duration = this.computeDuration(pvProv, initialLaw, initialDateIn, finalLaw, finalDate);

        // Get initial slew date
        AbsoluteDate initialDate = null;
        if (initialDateIn != null) {
            initialDate = initialDateIn;
        } else {
            initialDate = finalDate.shiftedBy(-duration);
        }

        // Initialization
        final Attitude initialAttitude = initialLaw.getAttitude(pvProv, initialDate, this.refFrame);
        final List<Attitude> slewEphem = new ArrayList<Attitude>();
        slewEphem.add(initialAttitude);
        double t = 0;
        final Vector3D slewAxisNormed = this.slewAxis.normalize();

        // Loop on slew duration
        while (t < duration) {

            t += this.dtSCAO;

            // Computation of the attitude
            final Attitude nextAtt = this.nextAtt(pvProv, duration, t, initialAttitude, slewAxisNormed, initialDate,
                    finalLaw);

            if (nextAtt == null) {
                // Exit the while loop
                break;
            }

            slewEphem.add(nextAtt);
        }

        // Build slew ephemeris
        return new TabulatedSlew(slewEphem, nature);
    }

    /**
     * Inner loop of compute() method.
     * 
     * @param pvProv
     *        PV provider
     * @param duration
     *        slew duration
     * @param t
     *        current time
     * @param initialAttitude
     *        initial slew attitude
     * @param slewAxisNormed
     *        normalized slew axis
     * @param initialDate
     *        slew initial date
     * @param finalLaw
     *        attitude law after the slew
     * @return slew next attitude
     * @throws PatriusException
     *         thrown if computation failed
     */
    private Attitude nextAtt(final PVCoordinatesProvider pvProv,
            final double duration,
            final double t,
            final Attitude initialAttitude,
            final Vector3D slewAxisNormed,
            final AbsoluteDate initialDate,
            final AttitudeProvider finalLaw) throws PatriusException {

        // Computation of the attitude
        final Attitude nextAtt;
        if (Precision.compareTo(t, this.accelDuration, Precision.DOUBLE_COMPARISON_EPSILON) < 0) {
            // Acceleration phase
            final double theta = 0.5 * this.accelMax * t * t;
            final double omega = this.accelMax * t;

            // Build attitude
            final AbsoluteDate nextDate = initialDate.shiftedBy(t);
            final Rotation deltaRot = new Rotation(this.slewAxis, theta);
            final Rotation newRot = initialAttitude.getRotation().applyTo(deltaRot);
            final Vector3D newOmega = new Vector3D(omega, slewAxisNormed);
            nextAtt = new Attitude(nextDate, this.refFrame, new AngularCoordinates(newRot, newOmega));
        } else if (Precision.compareTo(t, this.durationWoTranq - this.decelDuration,
            Precision.DOUBLE_COMPARISON_EPSILON) < 0) {
            // Constant velocity phase
            final double omega0 = this.accelMax * this.accelDuration;
            final double theta0 = 0.5 * this.accelMax * this.accelDuration * this.accelDuration;
            final double tShifted = t - this.accelDuration;
            final double theta = theta0 + omega0 * tShifted;
            final double omega = omega0;

            // Build attitude
            final AbsoluteDate nextDate = initialDate.shiftedBy(t);
            final Rotation deltaRot = new Rotation(this.slewAxis, theta);
            final Rotation newRot = initialAttitude.getRotation().applyTo(deltaRot);
            final Vector3D newOmega = new Vector3D(omega, slewAxisNormed);
            nextAtt = new Attitude(nextDate, this.refFrame, new AngularCoordinates(newRot, newOmega));
        } else if (Precision.compareTo(t, this.durationWoTranq, Precision.DOUBLE_COMPARISON_EPSILON) < 0) {
            // Deceleration phase
            final double half = 0.5;
            // Equality is allowed since in this case durationWoTranq has been set to accelDuration + decelDuration
            if (this.accelDuration + this.decelDuration == this.durationWoTranq) {
                // There was no constant velocity phase
                final double omega0 = this.accelMax * this.accelDuration;
                final double theta0 = half * this.accelMax * this.accelDuration * this.accelDuration;
                final double tShifted = t - this.accelDuration;
                final double theta = theta0 + omega0 * tShifted - half * this.decelMax * tShifted * tShifted;
                final double omega = omega0 - this.decelMax * tShifted;

                // Build attitude
                final AbsoluteDate nextDate = initialDate.shiftedBy(t);
                final Rotation deltaRot = new Rotation(this.slewAxis, theta);
                final Rotation newRot = initialAttitude.getRotation().applyTo(deltaRot);
                final Vector3D newOmega = new Vector3D(omega, slewAxisNormed);
                nextAtt = new Attitude(nextDate, this.refFrame, new AngularCoordinates(newRot, newOmega));
            } else {
                // There was a constant velocity phase
                final double omega0 = this.accelMax * this.accelDuration;
                final double theta0 = half * this.accelMax * this.accelDuration * this.accelDuration
                    + (this.durationWoTranq - this.decelDuration - this.accelDuration) * omega0;
                final double tShifted = t - (this.durationWoTranq - this.decelDuration);
                final double theta = theta0 + omega0 * tShifted - half * this.decelMax * tShifted * tShifted;
                final double omega = omega0 - this.decelMax * tShifted;

                // Build attitude
                final AbsoluteDate nextDate = initialDate.shiftedBy(t);
                final Rotation deltaRot = new Rotation(this.slewAxis, theta);
                final Rotation newRot = initialAttitude.getRotation().applyTo(deltaRot);
                final Vector3D newOmega = new Vector3D(omega, slewAxisNormed);
                nextAtt = new Attitude(nextDate, this.refFrame, new AngularCoordinates(newRot, newOmega));
            }
        } else {
            if (t < duration) {
                final AbsoluteDate finalDate = initialDate.shiftedBy(t);
                nextAtt = finalLaw.getAttitude(pvProv, finalDate, this.refFrame);
            } else {
                // Exit the while loop
                return null;
            }
        }

        return nextAtt;
    }

    /**
     * Check input dates
     * @param initialDate slew initial date
     * @param finalDate slew final date
     * @throws PatriusException thrown if dates are incoherent
     */
    private void checkInputs(final AbsoluteDate initialDate, final AbsoluteDate finalDate) throws PatriusException {
        // Check type of slew
        if ((initialDate == null && finalDate == null) || (initialDate != null && finalDate != null)) {
            // Incoherent case
            throw new PatriusException(PatriusMessages.INTERNAL_ERROR);
        }
    }

    /**
     * Compute the slew (numerical version).
     * @param pvProv satellite PV coordinates through time
     * @param initialLaw initial attitude law (before the slew)
     * @param initialDateIn slew start date (null if slew defined with its end date)
     * @param finalLaw final attitude law (after the slew)
     * @param finalDate slew end date (null if slew defined with its start date)
     * @return built slew (numerical version)
     * @throws PatriusException thrown if computation failed or if dates are both null or not null
     */
    public TabulatedSlew computeNumerical(final PVCoordinatesProvider pvProv, final AttitudeProvider initialLaw,
            final AbsoluteDate initialDateIn,
            final AttitudeProvider finalLaw,
            final AbsoluteDate finalDate) throws PatriusException {

        // Check type of slew
        checkInputs(initialDateIn, finalDate);

        // Compute slew duration
        final double duration = this.computeDuration(pvProv, initialLaw, initialDateIn, finalLaw, finalDate);

        // Get initial slew date
        AbsoluteDate initialDate = null;
        if (initialDateIn != null) {
            initialDate = initialDateIn;
        } else {
            initialDate = finalDate.shiftedBy(-duration);
        }

        // Initialization
        final Attitude initialAttitude = initialLaw.getAttitude(pvProv, initialDate, this.refFrame);
        Attitude prevAtt = initialAttitude;
        double prevOmega = 0;
        final List<Attitude> slewEphem = new ArrayList<Attitude>();
        slewEphem.add(initialAttitude);
        double t = 0;
        final Vector3D slewAxisNormed = this.slewAxis.normalize();

        // Loop on slew duration
        while (t < duration) {
            // Acceleration computation
            final double accel;
            if (Precision.compareTo(t, this.accelDuration, Precision.DOUBLE_COMPARISON_EPSILON) < 0) {
                // Acceleration phase
                accel = this.accelMax;
            } else if (Precision.compareTo(t, this.durationWoTranq - this.decelDuration,
                Precision.DOUBLE_COMPARISON_EPSILON) < 0) {
                // Coasting phase
                accel = 0.;
            } else if (Precision.compareTo(t, this.durationWoTranq, Precision.DOUBLE_COMPARISON_EPSILON) < 0) {
                // Deceleration phase
                accel = -this.decelMax;
            } else {
                // Tranquilisation period
                accel = 0.;
            }

            // Computation of the attitude
            final double nextOmega;
            final Attitude nextAtt;
            if (t < this.durationWoTranq) {
                // Computation of the angular rate and the attitude
                nextOmega = prevOmega + accel * this.dtSCAO;
                // Computation of the increment angle vector
                final double deltaTheta = nextOmega * this.dtSCAO;
                // Computation of the rotation quaternion
                final Rotation deltaRot = new Rotation(this.slewAxis, deltaTheta);
                // Computation of the attitude
                final Rotation newRot = prevAtt.getRotation().applyTo(deltaRot);
                final Vector3D newOmega = new Vector3D(nextOmega, slewAxisNormed);
                final AbsoluteDate nextDate = initialDate.shiftedBy(t + this.dtSCAO);
                nextAtt = new Attitude(nextDate, this.refFrame, new AngularCoordinates(newRot, newOmega));
            } else {
                if (t + this.dtSCAO < duration) {
                    final AbsoluteDate finalCurrentDate = initialDate.shiftedBy(t + this.dtSCAO);
                    nextAtt = finalLaw.getAttitude(pvProv, finalCurrentDate, this.refFrame);
                    nextOmega = 0;
                } else {
                    // Exit the while loop
                    break;
                }
            }

            prevOmega = nextOmega;
            prevAtt = nextAtt;
            t += this.dtSCAO;
            slewEphem.add(nextAtt);
        }

        // Build slew ephemeris
        return new TabulatedSlew(slewEphem, nature);
    }

    /**
     * Computes the slew duration.
     * 
     * @param pvProv
     *        PV coordinates provider
     * @param initialLaw initial attitude law (before the slew)
     * @param initialDateIn slew start date (null if slew defined with its end date)
     * @param finalLaw final attitude law (after the slew)
     * @param finalDate slew end date (null if slew defined with its start date)
     * @return slew duration
     * @throws PatriusException thrown if computation failed or if dates are both null or not null
     */
    public double computeDuration(final PVCoordinatesProvider pvProv, final AttitudeProvider initialLaw,
            final AbsoluteDate initialDateIn,
            final AttitudeProvider finalLaw,
            final AbsoluteDate finalDate) throws PatriusException {

        // Initialization
        double durationPrevious = Double.POSITIVE_INFINITY;
        double duration = this.durationMax;

        // Get reference attitude depending on type of date
        Attitude initialAttitude = null;
        Attitude finalAttitude = null;
        if (initialDateIn != null) {
            initialAttitude = initialLaw.getAttitude(pvProv, initialDateIn, this.refFrame);
        } else {
            finalAttitude = finalLaw
                    .getAttitude(pvProv, finalDate.shiftedBy(-this.tranquillisationTime), this.refFrame);
        }
        int iLoop = 0;

        // Loop until convergence is reached
        while (MathLib.abs(durationPrevious - duration) > this.dtConvergenceThreshold) {
            iLoop++;
            if (iLoop > this.maxIterationsNumber) {
                throw new PatriusException(PatriusMessages.CONVERGENCE_FAILED_AFTER_N_ITERATIONS,
                    this.maxIterationsNumber);
            }
            durationPrevious = duration;

            // Get target attitude depending on type of date
            if (initialDateIn != null) {
                final AbsoluteDate date = initialDateIn.shiftedBy(durationPrevious - this.tranquillisationTime);
                finalAttitude = finalLaw.getAttitude(pvProv, date, this.refFrame);
            } else {
                final AbsoluteDate date = finalDate.shiftedBy(-durationPrevious);
                initialAttitude = initialLaw.getAttitude(pvProv, date, this.refFrame);
            }

            duration = this.analyze(initialAttitude, finalAttitude);
        }

        // Return computed duration
        return duration;
    }

    /**
     * Computes the slew duration.
     * 
     * @param initialAttitude
     *        initial attitude
     * @param finalAttitude
     *        final attitude
     * @return slew duration
     * @throws PatriusException
     *         thrown if an error occurs during attitudes computation
     */
    private double analyze(final Attitude initialAttitude, final Attitude finalAttitude) throws PatriusException {

        // 1. Computation of the rotation characteristics

        // Rotation from initial attitude to final attitude
        final Rotation slewRot = initialAttitude.getRotation().applyInverseTo(finalAttitude.getRotation());
        // Rotation associated angle (always positive)
        this.slewAngle = slewRot.getAngle();

        // Check slew angle does not exceed maximum allowed value
        if (this.slewAngle > this.thetaMaxAllowed) {
            throw new PatriusException(PatriusMessages.MANEUVER_AMPLITUDE_EXCEEDS_FIXED_MAXIMUM_VALUE);
        }

        // Rotation axis computation (normalized)
        this.slewAxis = slewRot.getAxis();

        // 2. Computation of the maximum equivalent satellite inertia in function of the rotation axis :

        // Computation of the pseudo-inv. matrix representing the rotation from satellite frame to reaction wheel frame
        final RealMatrix matrix = new BlockRealMatrix(this.rwMatrix);
        final SingularValueDecomposition decomposition = new SingularValueDecomposition(matrix);
        final RealMatrix pseudoInvMatrix = decomposition.getSolver().getInverse();

        // Computation of the equivalent satellite inertia in reaction wheel frame
        final double[] inertiaEquivRwFrameRaw = pseudoInvMatrix.multiply(new BlockRealMatrix(this.inertiaMatrix))
            .operate(this.slewAxis.toArray());
        final double[] inertiaEquivRwFrame = new double[inertiaEquivRwFrameRaw.length];
        for (int i = 0; i < inertiaEquivRwFrame.length; i++) {
            inertiaEquivRwFrame[i] = MathLib.abs(inertiaEquivRwFrameRaw[i]);
        }

        // Computation of the max equivalent satellite inertia
        double inertiaEquivMax = Double.NEGATIVE_INFINITY;
        for (final double element : inertiaEquivRwFrame) {
            inertiaEquivMax = MathLib.max(inertiaEquivMax, element);
        }

        // 3. Computation of the maximum angular rate and angular acceleration

        // Computation of the maximum angular rate
        final double omegaMax = MathLib.divide(this.rwDeltaMomentumAlloc, inertiaEquivMax);

        // Computation of the angular acceleration and deceleration
        this.accelMax = MathLib.divide(this.rwTorqueAllocAccel, inertiaEquivMax);
        this.decelMax = MathLib.divide(this.rwTorqueAllocDecel, inertiaEquivMax);

        // 4. Computation of the manoeuvre duration

        // Computation the maximum acceleration and deceleration durations
        final double accelMaxDuration = MathLib.divide(this.rwDeltaMomentumAlloc, this.rwTorqueAllocAccel);
        final double decelMaxDuration = MathLib.divide(this.rwDeltaMomentumAlloc, this.rwTorqueAllocDecel);

        // Computation of the acceleration and deceleration durations assuming there is no constant velocity phase
        final double coef = MathLib.divide(2 * this.slewAngle, (this.accelMax + this.decelMax));
        this.accelDuration = MathLib.sqrt(coef * MathLib.divide(this.decelMax, this.accelMax));
        this.decelDuration = MathLib.sqrt(coef * MathLib.divide(this.accelMax, this.decelMax));

        // Saturation of the acceleration or deceleration duration
        // (in case of saturation there will be a constant velocity phase)
        this.accelDuration = (this.accelDuration > accelMaxDuration) ? accelMaxDuration : this.accelDuration;
        this.decelDuration = (this.decelDuration > decelMaxDuration) ? decelMaxDuration : this.decelDuration;

        // Computation of the manoeuvre duration without tranquillisation phase
        if (this.accelDuration == accelMaxDuration || this.decelDuration == decelMaxDuration) {
            // There is a constant velocity phase
            this.durationWoTranq =
                MathLib.divide(this.slewAngle, omegaMax) + (this.accelDuration + this.decelDuration) / 2.;
        } else {
            // There is no constant velocity phase
            this.durationWoTranq = this.accelDuration + this.decelDuration;
        }

        // Add the tranquillisation time
        // Return result
        return this.durationWoTranq + this.tranquillisationTime;
    }
}
