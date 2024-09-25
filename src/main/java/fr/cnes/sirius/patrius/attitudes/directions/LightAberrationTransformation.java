/**
 *
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
 * HISTORY
 * VERSION:4.13:DM:DM-120:08/12/2023:[PATRIUS] Merge de la branche patrius-for-lotus dans Patrius
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes.directions;

import fr.cnes.sirius.patrius.attitudes.directions.ITargetDirection.SignalDirection;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.signalpropagation.VacuumSignalPropagation;
import fr.cnes.sirius.patrius.signalpropagation.VacuumSignalPropagationModel;
import fr.cnes.sirius.patrius.utils.Constants;

/**
 * This class implements the light aberration transformation. It allows to transform a light path direction (of
 * observation or targeting) from a frame to another.
 *
 * <p>
 * Note that this class only treats the transformation coming from the translational velocity between two frames since
 * this is the only aspect that matters regarding the aberration.<br>
 * For example, if a user wants to transform a light path direction from GCRF to a topocentric frame TOPO, they can
 * proceed in two equivalent ways (depending on handling the rotation before of after the light aberration
 * transformation):
 * <ul>
 * <li>First use this class to transform the light path direction into an intermediate frame attached to TOPO but with
 * axes having the same orientation as the GCRF and then transform the resulting direction in TOPO by applying the axes
 * rotation.</li>
 * <li>First apply the rotation of the light path direction in TOPO and then apply the light aberration transformation
 * with the resulting vector and the velocity of TOPO in the GCRF (but projected on the axes of TOPO).</li>
 * </ul>
 * </p>
 *
 * <p>
 * The light aberration simply comes from the difference of photons velocity direction, arriving (resp. leaving) an
 * object, depending on the frame of observation. Thus, in the same manner as the velocity, the light path direction is
 * necessarily attached to a frame.
 * </p>
 *
 * <p>
 * <b>Examples</b>:
 * <ul>
 * <li>The signal propagation from an object A to an object B is computed thanks to {@link VacuumSignalPropagationModel}
 * in an inertial frame F (for example GCRF).</li>
 * <ul>
 * <li>The user wants to know in which direction the object B will see the signal (emitted by the object A) coming in
 * its own frame (attached to B). Then this class should be used with: {@code toTargetDirection=vectorBToA} (light path
 * direction from B to A), {@code newFrameVelocity=velocityBInFrameF}, {@code signalDirection = }
 * {@link SignalDirection#FROM_TARGET}</li>
 * <li>The user wants to know in which direction, in object B's frame, the object B should target a signal to reach
 * object A. Then this class should be used with: {@code toTargetDirection=vectorBToA} (light path direction from B to
 * A), {@code newFrameVelocity=velocityBInFrameF}, {@code signalDirection = }{@link SignalDirection#TOWARD_TARGET}</li>
 * </ul>
 * <li>A reception light path direction {@code toTargetDirection} expressed in an instrument frame F is provided as
 * input. The user wants to know the equivalent reception light path direction seen from another frame F'. Then this
 * class should be used with: {@code newFrameVelocity=velocityF'InF}, {@code signalDirection = }
 * {@link SignalDirection#FROM_TARGET}</li>
 * </ul>
 * </p>
 *
 * @author Alice Latourte, Hugo Veuillez
 *
 * @since 4.13
 */
public final class LightAberrationTransformation {

    /** Aberration angle [rad]. */
    private final double aberrationAngle;

    /** To target direction defined in the new frame F'. */
    private final Vector3D transformedToTargetDirection;

    /** Indicates whether the signal is coming from or going towards the target. */
    private final SignalDirection signalDirection;

    /**
     * Constructor for the light aberration transformation to convert the propagation vector from frame F to F'.
     * 
     * <p>
     * The {@link VacuumSignalPropagation#getVector light propagation vector} is defined with respect to the
     * {@link VacuumSignalPropagation#getFrame frame F}. The new frame F' is attached to the receiver or emitter
     * according to the {@code signalDirection}, but keeps the same orientation as the frame F.
     * </p>
     * 
     * <p>
     * See {@link #computeAberrationAngle} for more details on the aberration formula.
     * </p>
     *
     * @param signalPropagation
     *        The signal propagation from emitter to receiver
     * @param signalDirection
     *        Indicate whether the frame F' is attached to the receiver {@link SignalDirection#FROM_TARGET} or the
     *        emitter {@link SignalDirection#TOWARD_TARGET}
     */
    public LightAberrationTransformation(final VacuumSignalPropagation signalPropagation,
                                         final SignalDirection signalDirection) {
        this(getToTargetDirection(signalPropagation, signalDirection), getNewReferentialVelocity(signalPropagation,
            signalDirection), signalDirection);
    }

    /**
     * Constructor for the light aberration transformation to transform the provided light path direction from frame F
     * to F'.
     *
     * <p>
     * See {@link #computeAberrationAngle} for more details on the aberration formula.
     * </p>
     *
     * @param toTargetDirection
     *        Direction towards the target, defined in a frame F
     * @param newFrameVelocity
     *        Velocity of the new frame F' with respect to F. Note that F' keeps the same orientation as F.
     * @param signalDirection
     *        Indicates whether the signal is coming from or towards the target
     */
    public LightAberrationTransformation(final Vector3D toTargetDirection, final Vector3D newFrameVelocity,
                                         final SignalDirection signalDirection) {
        this.aberrationAngle = computeAberrationAngle(toTargetDirection, newFrameVelocity);
        this.signalDirection = signalDirection;
        this.transformedToTargetDirection =
            applyAberrationTransformation(toTargetDirection, newFrameVelocity, this.aberrationAngle, signalDirection);
    }

    /**
     * Getter for the aberration angle [rad].
     *
     * @return the aberration angle
     */
    public double getAberrationAngle() {
        return this.aberrationAngle;
    }

    /**
     * Getter for the transformed light path direction towards the target in the new frame F'.
     *
     * <p>
     * Note: The returned vector can be projected in any other frame thanks to {@link Transform#transformVector}.<br>
     * But beware that it is only a projection and the light path direction remains defined with respect to F' (in terms
     * of aberration).
     * </p>
     *
     * @return the direction towards the target defined with respect to the frame F'
     */
    public Vector3D getTransformedToTargetDirection() {
        return this.transformedToTargetDirection;
    }

    /**
     * Getter for the signal direction.
     *
     * @return the signal direction
     */
    public SignalDirection getSignalDirection() {
        return this.signalDirection;
    }

    /**
     * Compute the light aberration angle between a light path direction seen from a frame F and a frame F'.
     *
     * <p>
     * Let's assume that {@code toTargetDirection} is a light path direction defined in the frame F.<br>
     * To convert this direction in a new frame F', the velocity of F' with respect to F ({@code newFrameVelocity})
     * needs to be taken into consideration. Note that F' keeps the same orientation as F.
     * </p>
     *
     * <pre>
     *          target (in F)    target (in F')
     *                 T          T'
     *                ^          ^
     *               /         .
     *              /        .  dir (in F')
     *             /__ Δθ  .
     * dir (in F) /   \  .
     *           /     .
     *          /    .
     *         /   .__
     *        /__.    \ θ'
     *       / .  \   |
     *      /.  θ |   |
     *      -----------&gt; _
     *    O              V
     *                velocity of F' in F
     * 
     *              sin(θ)
     *   tan(θ') = ------------- sqrt(1 - (V/c)²)
     *             cos(θ) + V/c
     * </pre>
     *
     * @param toTargetDirection
     *        Direction towards the target, defined in the frame F
     * @param newFrameVelocity
     *        Velocity of the new frame F' w.r.t the frame F
     * @return the aberration angle so that {@code angleInNewFrame = angleInPreviousFrame + aberrationAngle}
     */
    public static double computeAberrationAngle(final Vector3D toTargetDirection, final Vector3D newFrameVelocity) {

        // Compute the velocity norm
        final double newFrameVelocityNorm = newFrameVelocity.getNorm();

        final double deltaTheta;
        if (newFrameVelocityNorm < Precision.DOUBLE_COMPARISON_EPSILON) {
            // Quick escape if the velocity norm is null
            deltaTheta = 0.;
        } else {
            // Compute the aberration angle
            final double theta = Vector3D.angle(toTargetDirection, newFrameVelocity);
            final double[] sinAndCos = MathLib.sinAndCos(theta);
            final double velocitiesRatio = newFrameVelocityNorm / Constants.SPEED_OF_LIGHT;
            final double specialRelativityCorrection = MathLib.sqrt(1 - (velocitiesRatio * velocitiesRatio));
            final double thetaWithAberration = MathLib.atan2(specialRelativityCorrection * sinAndCos[0],
                sinAndCos[1] + velocitiesRatio);
            deltaTheta = thetaWithAberration - theta;
        }
        return deltaTheta;
    }

    /**
     * Transform the provided light path direction from frame F to F'.
     *
     * <p>
     * Let's assume that {@code toTargetDirection} is a light path direction defined in the frame F.<br>
     * To convert this direction in a new frame F', the {@code newFrameVelocity} should be the velocity of F' with
     * respect to F.<br>
     * Note that F' keeps the same orientation as F.
     * </p>
     *
     * <p>
     * See {@link #computeAberrationAngle} for more details on the aberration formula.
     * </p>
     *
     * @param toTargetDirection
     *        Direction towards the target defined in the frame F
     * @param newFrameVelocity
     *        Velocity of the frame F' w.r.t the frame F
     * @param signalDirection
     *        Indicates whether the signal is coming from or towards the target
     * @return the transformed light path direction, expressed in the frame F'
     */
    public static Vector3D applyTo(final Vector3D toTargetDirection, final Vector3D newFrameVelocity,
                                   final SignalDirection signalDirection) {
        final double aberrationAngleCorrection = computeAberrationAngle(toTargetDirection, newFrameVelocity);
        return applyAberrationTransformation(toTargetDirection, newFrameVelocity, aberrationAngleCorrection,
            signalDirection);
    }

    /**
     * Apply the aberration transformation angle to the toTargetDirection light path direction.
     *
     * @param toTargetDirection
     *        Vector to transform
     * @param newFrameVelocity
     *        The new frame velocity
     * @param aberrationAngle
     *        The aberration angle [rad]
     * @param signalDirection
     *        The signal direction
     * @return the transformed vector light path direction
     */
    private static Vector3D applyAberrationTransformation(final Vector3D toTargetDirection,
                                                          final Vector3D newFrameVelocity,
                                                          final double aberrationAngle,
                                                          final SignalDirection signalDirection) {
        // Define the sign according to the signal direction
        final int sign;
        switch (signalDirection) {
            case FROM_TARGET:
                sign = -1;
                break;
            case TOWARD_TARGET:
                sign = 1;
                break;
            default:
                // Shouldn't happened (internal error)
                throw new EnumConstantNotPresentException(SignalDirection.class, signalDirection.toString());
        }

        // Compute the rotation to apply
        final Vector3D perp = Vector3D.crossProduct(toTargetDirection, newFrameVelocity);
        final Rotation rot = new Rotation(perp, sign * aberrationAngle);

        // Compute the transformed vector light path direction
        return rot.applyTo(toTargetDirection);
    }

    /**
     * Getter for the toTarget direction according to the signal direction.
     *
     * @param sigPropagation
     *        The signal propagation object
     * @param signalDirection
     *        The signal light path direction
     * @return the toTarget light path direction
     */
    private static Vector3D getToTargetDirection(final VacuumSignalPropagation sigPropagation,
                                                 final SignalDirection signalDirection) {
        // Extract the emitter to receiver vector
        final Vector3D emitterToReceiver = sigPropagation.getPVPropagation().getPosition();

        // Define the toTarget vector according to the signal direction
        final Vector3D toTargetDirection;
        switch (signalDirection) {
            case FROM_TARGET:
                toTargetDirection = emitterToReceiver.negate();
                break;
            case TOWARD_TARGET:
                toTargetDirection = emitterToReceiver;
                break;
            default:
                // Shouldn't happened (internal error)
                throw new EnumConstantNotPresentException(SignalDirection.class, signalDirection.toString());
        }
        return toTargetDirection;
    }

    /**
     * Getter for the new frame velocity according to the signal direction.
     *
     * @param sigPropagation
     *        The signal propagation object
     * @param signalDirection
     *        The signal direction
     * @return the new frame velocity
     */
    private static Vector3D getNewReferentialVelocity(final VacuumSignalPropagation sigPropagation,
                                                      final SignalDirection signalDirection) {
        // Define the new frame velocity according to the signal direction
        final Vector3D newFrameVelocity;
        switch (signalDirection) {
            case FROM_TARGET:
                newFrameVelocity = sigPropagation.getReceiverPV().getVelocity();
                break;
            case TOWARD_TARGET:
                newFrameVelocity = sigPropagation.getEmitterPV().getVelocity();
                break;
            default:
                // Shouldn't happened (internal error)
                throw new EnumConstantNotPresentException(SignalDirection.class, signalDirection.toString());
        }
        return newFrameVelocity;
    }
}
