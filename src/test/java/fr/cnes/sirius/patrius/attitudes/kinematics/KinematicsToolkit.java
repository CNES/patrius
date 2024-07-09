/**
 * Copyright 2011-2017 CNES
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
 * @history 28/06/12
 * HISTORY
* VERSION:4.4:DM:DM-2231:04/10/2019:[PATRIUS] Creation d'un cache dans les profils de vitesse angulaire
* VERSION:4.3:DM:DM-2099:15/05/2019:[PATRIUS] Possibilite de by-passer le critere du pas min dans l'integrateur numerique DOP853
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
* VERSION:4.3:DM:DM-2106:15/05/2019:[Patrius] Modification de KinematicsToolkit
 * VERSION::DM:319:05/03/2015:Corrected Rotation class (Step1)
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes.kinematics;

import fr.cnes.sirius.patrius.math.complex.Quaternion;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.RotationOrder;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3DFunction;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;

/**
 * This class contains static methods performing kinematics operations.
 * 
 * @concurrency immutable
 * 
 * @author Tiziana Sabatini, Rami Houdroge
 * 
 * @version $Id: KinematicsToolkit.java 18065 2017-10-02 16:42:02Z bignon $
 * 
 * @since 1.2
 * 
 */
public final class KinematicsToolkit {

    /** Default 4 Hz integration step. */
    public static final double DEFAULT_4HZ = 1 / 4.;

    /** Default 8 Hz integration step. */
    public static final double DEFAULT_8HZ = 1 / 8.;

    /** 12. */
    private static final double TWELVE = 12.;

    /**
     * Class only contains static methods.
     */
    private KinematicsToolkit() {

    }

    /** Integration types. */
    public enum IntegrationType {
        /** Wilcox, 1st order. */
        WILCOX_1,
        /** Wilcox, 2nd order. */
        WILCOX_2,
        /** Wilcox, 3rd order. */
        WILCOX_3,
        /** Wilcox, 4th order. */
        WILCOX_4,
        /** Edwards, Wilcox 3rd order and commutation correction. */
        EDWARDS;
    }

    /**
     * Estimate spin between two orientations.<br>
     * Estimation is based on a simple fixed rate rotation during the time interval between the two
     * attitude. This method has been copied from the Attitude class; when the class
     * KinematicsToolkit will be merged to Orekit, the Attitude estimateSpin method should be
     * deleted.
     * 
     * @param start start orientation
     * @param end end orientation
     * @param dt time elapsed between the dates of the two orientations
     * @return the spin allowing to go from start to end orientation
     */
    public static Vector3D estimateSpin(final Rotation start, final Rotation end, final double dt) {

        final Rotation evolution = end.applyTo(start.revert());
        return new Vector3D(evolution.getAngle() / dt, start.applyInverseTo(evolution.getAxis()));
    }

    /**
     * Compute spin knowing the instantaneous quaternion and its derivative.
     * 
     * @param q
     *        the quaternion (rotation transforming frame R to frame S) at a given date
     * @param qd
     *        the derivative of the quaternion at a given date
     * @return the spin of the frame S with regard to frame R (expressed in frame S) at the given date
     */
    public static Vector3D computeSpin(final Quaternion q, final Quaternion qd) {

        final Quaternion spin = Quaternion.multiply(q.getConjugate(), qd).multiply(2);
        return new Vector3D(spin.getVectorPart());
    }

    /**
     * Compute spin knowing the instantaneous quaternion and its derivative.
     * 
     * @param ang
     *        the angles describing a rotation from frame R to frame S at a given date
     * @param angd
     *        the derivative of the angles
     * @param order
     *        the rotation order: ZXZ for Euler angles, ZYX for Cardan angles
     * @return the spin of the frame S with regard to frame R (expressed in frame S) at the given date
     * @throws PatriusException
     *         the rotation order is not supported
     */
    public static Vector3D computeSpin(final double[] ang, final double[] angd,
                                       final RotationOrder order) throws PatriusException {

        // Initialization
        final double x;
        final double y;
        final double z;
        if (order == RotationOrder.ZXZ) {
            // Euler angles:
            x = angd[0] * MathLib.sin(ang[1]) * MathLib.sin(ang[2]) + angd[1] * MathLib.cos(ang[2]);
            y = angd[0] * MathLib.sin(ang[1]) * MathLib.cos(ang[2]) - angd[1] * MathLib.sin(ang[2]);
            z = angd[0] * MathLib.cos(ang[1]) + angd[2];

        } else if (order == RotationOrder.ZYX) {
            // Cardan angles:
            x = -angd[0] * MathLib.sin(ang[1]) + angd[2];
            y = angd[0] * MathLib.cos(ang[1]) * MathLib.sin(ang[2]) + angd[1] * MathLib.cos(ang[2]);
            z = angd[0] * MathLib.cos(ang[1]) * MathLib.cos(ang[2]) - angd[1] * MathLib.sin(ang[2]);

        } else {
            // Unsupported rotation order
            throw new PatriusException(PatriusMessages.UNSUPPORTED_ROTATION_ORDER);
        }
        // Return result
        return new Vector3D(x, y, z);
    }

    /**
     * Compute the derivative of a quaternion knowing the instantaneous spin.
     * 
     * @param q
     *        the quaternion (rotation transforming frame R to frame S) at a given date
     * @param spin
     *        the spin (from frame S to frame R, expressed in frame S) at the given date
     * @return the derivative of the quaternion q at the given date.
     */
    public static Quaternion differentiateQuaternion(final Quaternion q, final Vector3D spin) {

        final Quaternion qSpin = new Quaternion(spin.toArray());
        return Quaternion.multiply(q, qSpin).multiply(1. / 2.);
    }

    /**
     * Integrate a spin function. Use the Edwards method for integration of mobile rotation axes.
     * 
     * @param type type of the integration (either Wilcox or Edwards)
     * @param initOrientation rotation at initial date
     * @param initDate initial date
     * @param finalDate final date (is after or equals the initial date)
     * @param spin spin function
     * @param step time step
     * @return the computed rotation at final date
     */
    public static Rotation integrate(final IntegrationType type, final Rotation initOrientation,
                                     final AbsoluteDate initDate, final AbsoluteDate finalDate,
                                     final Vector3DFunction spin,
                                     final double step) {

        // Initialisation
        AngularCoordinates currentAngCoor = new AngularCoordinates(initOrientation, Vector3D.ZERO);

        // If the initial date is strictly before the final date
        if (initDate.compareTo(finalDate) < 0) {

            double dt = step;
            double t0;

            // Integration depending on method
            AbsoluteDate currentDate = initDate;

            // While the integration can be done on a whole integration step (numerical error are
            // neglected)
            while (currentDate.compareTo(finalDate.shiftedBy(-step)) <= 0) {
                t0 = currentDate.durationFrom(initDate);
                currentAngCoor = computeStep(type, currentAngCoor, t0, dt, spin);
                currentDate = currentDate.shiftedBy(step);
            }

            // Last time step (which is smaller than the whole integration step)
            t0 = currentDate.durationFrom(initDate);
            dt = finalDate.durationFrom(currentDate);

            // Check that the final date is different from the current date
            if (MathLib.abs(dt) > Precision.EPSILON) {
                currentAngCoor = computeStep(type, currentAngCoor, t0, dt, spin);
            }
        }

        // If the initial date equals or is after the final date, the initial rotation is returned
        return currentAngCoor.getRotation();
    }

    /**
     * Compute the integrated rotation depending on the integration type
     * 
     * @param type the integration type
     * @param previousAngCoor the previous computed orientation for Edwards integration
     * @param t0 start time of the integration
     * @param dt time step
     * @param spin spin function
     * @return the integrated angular coordinates
     */
    private static AngularCoordinates computeStep(final IntegrationType type,
                                                  final AngularCoordinates previousAngCoor, final double t0,
                                                  final double dt,
                                                  final Vector3DFunction spin) {

        final Rotation currentRot;
        final AngularCoordinates currentAngCoor;

        switch (type) {
            case WILCOX_1:
                // Wilcox 1
                currentRot = wilcoxStep(previousAngCoor.getRotation(), t0, dt, spin, 1);
                currentAngCoor = new AngularCoordinates(currentRot, Vector3D.ZERO);
                break;
            case WILCOX_2:
                // Wilcox 2
                currentRot = wilcoxStep(previousAngCoor.getRotation(), t0, dt, spin, 2);
                currentAngCoor = new AngularCoordinates(currentRot, Vector3D.ZERO);
                break;
            case WILCOX_3:
                // Wilcox 3
                currentRot = wilcoxStep(previousAngCoor.getRotation(), t0, dt, spin, 3);
                currentAngCoor = new AngularCoordinates(currentRot, Vector3D.ZERO);
                break;
            case WILCOX_4:
                // Wilcox 4
                currentRot = wilcoxStep(previousAngCoor.getRotation(), t0, dt, spin, 4);
                currentAngCoor = new AngularCoordinates(currentRot, Vector3D.ZERO);
                break;
            case EDWARDS:
                // Edwards
                currentAngCoor = edwardsStep(previousAngCoor, t0, dt, spin);
                break;
            default:
                // Cannot happen
                throw new PatriusRuntimeException(PatriusMessages.UNKNOWN_PARAMETER, null);
        }

        return currentAngCoor;
    }

    /**
     * Perform a Wilcox integration step.
     * 
     * @param initOrientation
     *        the rotation at the initial date
     * @param t0
     *        the initial date t0
     * @param dt
     *        the integration duration
     * @param spin
     *        the spin time function : the t = 0. abscissa must correspond to the initial date.
     * @param order
     *        the order of the computation (available : 1 to 4)
     * @return the final rotation at initDate + dt
     */
    private static Rotation wilcoxStep(final Rotation initOrientation, final double t0,
                                       final double dt, final Vector3DFunction spin, final int order) {

        // initial quaternion
        final Quaternion initQuat = initOrientation.getQuaternion();

        // initialisations
        final Vector3D multVvector;
        final double multScalar;

        // spin integration
        final Vector3D integratedSpin = spin.integral(t0, t0 + dt);
        final double integratedSpinNorm = integratedSpin.getNorm();

        // switch on the order
        switch (order) {
            case 1:
                multScalar = 1.;
                multVvector = integratedSpin.scalarMultiply(1. / 2.);
                break;
            case 2:
                multScalar = 1. - (integratedSpinNorm * integratedSpinNorm) / 8.;
                multVvector = integratedSpin.scalarMultiply(1. / 2.);
                break;
            case 3:
                multScalar = 1. - (integratedSpinNorm * integratedSpinNorm) / 8.;
                multVvector = integratedSpin
                    .scalarMultiply((1. - (integratedSpinNorm * integratedSpinNorm) / 24.) / 2.);
                break;
            case 4:
                multScalar = 1. - (integratedSpinNorm * integratedSpinNorm) / 8.
                    + MathLib.pow(integratedSpinNorm / 2., 4) / 24.;
                multVvector = integratedSpin
                    .scalarMultiply((1. - (integratedSpinNorm * integratedSpinNorm) / 24.) / 2.);
                break;
            default:
                throw PatriusException
                    .createIllegalArgumentException(PatriusMessages.OUT_OF_RANGE_ORDER);
        }

        // final quaternion and rotation creation
        final Quaternion multQuat = new Quaternion(multScalar, multVvector.toArray());
        if (multQuat.getNorm() < Precision.EPSILON) {
            return new Rotation(false, initQuat);
        } else {
            return new Rotation(false, Quaternion.multiply(initQuat, multQuat).normalize());
        }
    }

    /**
     * Perform an Edwards integration step.
     * 
     * @param initCoord the rotation at the initial date
     * @param t0 the initial date t0
     * @param dt the integration duration
     * @param spin the vector3D function representing the spin; the value of the function at x=0
     *        must coincide with the spin value at the initial date t0.
     * @return the computed rotation and spin at t0+dt
     */
    private static AngularCoordinates edwardsStep(final AngularCoordinates initCoord,
                                                  final double t0, final double dt, final Vector3DFunction spin) {

        // spin integration
        final Vector3D integratedSpin = spin.integral(t0, t0 + dt);
        final double integratedSpinNorm = integratedSpin.getNorm();

        // initial quaternion
        final Quaternion initQuat = initCoord.getRotation().getQuaternion();

        // commutation error
        final Vector3D commutation = Vector3D.crossProduct(
            initCoord.getRotationRate().scalarMultiply(1. / 2.),
            integratedSpin.scalarMultiply(1. / 2.)).scalarMultiply(1. / TWELVE);

        final double multScalar = 1. - (integratedSpinNorm * integratedSpinNorm) / 8.;
        final Vector3D multVvector = integratedSpin.scalarMultiply(
            (1. - (integratedSpinNorm * integratedSpinNorm) / (2. * TWELVE)) / 2.).add(
            commutation);

        // final quaternion and rotation creation
        final Quaternion multQuat = new Quaternion(multScalar, multVvector.toArray());
        final Rotation rot = new Rotation(false, Quaternion.multiply(initQuat, multQuat).normalize());

        // the computed rotation and spin at t0+dt
        return new AngularCoordinates(rot, integratedSpin);
    }
}