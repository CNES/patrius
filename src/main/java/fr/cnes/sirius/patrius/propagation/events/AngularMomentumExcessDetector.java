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
 * HISTORY
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11:DM:DM-3197:22/05/2023:[PATRIUS] Deplacement dans PATRIUS de classes façade ALGO DV SIRUS
 * VERSION:4.11:FA:FA-3277:22/05/2023:[PATRIUS] Ellipsoïde ajuste sur un FacetBodyShape
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Detector triggered when the angular momentum reaches a maximal threshold
 *
 *
 * @since 4.11
 */
public final class AngularMomentumExcessDetector extends AbstractDetector {

    /** Serial UID. */
    private static final long serialVersionUID = 5108618094929296017L;

    /** Threshold triggering the event. */
    private final double maxMomentum;

    /** Reference frame to compute angular momentum. */
    private final Frame refFrame;

    /** Represented frame of attitude law. */
    private Frame repFrame = null;

    /** Reference frame of inertia matrix. */
    private Frame inertiaFrame = null;

    /** the inertia matrix to use */
    private RealMatrix inertiaMatrix = null;

    /** Action performed at excess detection when ascending. */
    private final Action actionAtAscending;

    /** Action performed at excess detection when descending. */
    private final Action actionAtDescending;

    /**
     * Constructor for an AngularMomentumExcessDetector instance.
     *
     * @param momentum
     *            angular momentum value triggering the event.
     * @param inertial
     *            frame with respect to which the w is to be computed
     * @param slopeSelectionIn
     *            g-function slope selection (0, 1, or 2)
     * @see PositionAngle
     */
    public AngularMomentumExcessDetector(final double momentum, final Frame inertial, final int slopeSelectionIn) {
        this(momentum, inertial, slopeSelectionIn, DEFAULT_MAXCHECK, DEFAULT_THRESHOLD);
    }

    /**
     * Constructor for an AngularMomentumExcessDetector instance with complementary parameters.
     *
     * @param momentum
     *            AOL value triggering the event.
     * @param inertial
     *            equator with respect to which the AOL is to be computed
     * @param slopeSelectionIn
     *            g-function slope selection (0, 1, or 2)
     * @param maxCheck
     *            maximum check (see {@link AbstractDetector})
     * @param threshold
     *            threshold (see {@link AbstractDetector})
     */
    public AngularMomentumExcessDetector(final double momentum, final Frame inertial, final int slopeSelectionIn,
            final double maxCheck, final double threshold) {
        this(momentum, inertial, slopeSelectionIn, maxCheck, threshold, Action.CONTINUE, Action.STOP);

    }

    /**
     * Constructor for an AngularMomentumExcessDetector instance with complementary parameters.
     *
     * @param momentum
     *            momentum excess value triggering the event.
     * @param inertial
     *            inertial frame wrt the angular velocity is computed
     * @param slopeSelectionIn
     *            g-function slope selection (0, 1, or 2)
     * @param maxCheck
     *            maximum check (see {@link AbstractDetector})
     * @param threshold
     *            threshold (see {@link AbstractDetector})
     * @param ascending
     *            action performed when ascending
     * @param descending
     *            action performed when descending
     */
    public AngularMomentumExcessDetector(final double momentum, final Frame inertial, final int slopeSelectionIn,
            final double maxCheck, final double threshold, final Action ascending, final Action descending) {
        super(slopeSelectionIn, maxCheck, threshold);
        this.maxMomentum = momentum;
        this.refFrame = inertial;

        this.actionAtAscending = ascending;
        this.actionAtDescending = descending;
    }

    /**
     * Define the inertia matrix (from CoM and in vehicle axis)
     *
     * @param inertia
     *            the inertia 3x3 matrix expressed in CoM
     * @param frame
     *            Frame
     */
    public final void setInertia(final RealMatrix inertia, final Frame frame) {
        this.inertiaMatrix = inertia;
        this.inertiaFrame = frame;
    }

    /**
     * Define the frame represented by the attitude law
     *
     * @param frame
     *            the attitude represented law
     */
    public final void setAttitudeRepresentedFrame(final Frame frame) {
        this.repFrame = frame;
    }

    /**
     * Handle an angular momentum excess event and choose what to do next.
     *
     * @param s
     *            the current state information : date, kinematics, attitude
     * @param increasing
     *            if true, the value of the switching function increases when times increases around event
     * @param forward
     *            if true, the integration variable (time) increases during integration.
     * @return the action performed when angular momentum excess is reached.
     * @exception PatriusException
     *                if some specific error occurs
     */
    @Override
    public final Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
        throws PatriusException {
        Action outputAction = this.actionAtDescending;
        if (increasing) {
            outputAction = this.actionAtAscending;
        }
        return outputAction;
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public final double g(final SpacecraftState s) throws PatriusException {
        // transform attitudeState reference frame to an inertial frame, if necessary
        final Attitude att = s.getAttitude().withReferenceFrame(this.refFrame);

        Vector3D angularVel = att.getOrientation().getRotationRate();
        // transform represented frame to Inertia frame, if necessary
        if (!this.repFrame.equals(this.inertiaFrame)) {
            // compute transform from repFrame to inertiaFrame
            final Transform transform = this.repFrame.getTransformTo(this.inertiaFrame, s.getDate());
            final AngularCoordinates coord = transform.getAngular();
            // compose angular velocity to express it in inertiaFrame (most likely coord.W == 0 so just rotation)
            angularVel = coord.getRotationRate().add(coord.getRotation().applyTo(angularVel));
        }

        final double[] angularVelVector = {angularVel.getX(), angularVel.getY(), angularVel.getZ()};

        // inertia matrix must have been set somewhere else (not a part of SpacecraftState)
        // Compute the angular momentum -> L = I · w
        final double[] angularMomentum = this.inertiaMatrix.operate(angularVelVector);
        final double momentum = new Vector3D(angularMomentum[0], angularMomentum[1], angularMomentum[2]).getNorm();

        // computes the difference between the actual spacecraft angular momentum and the threshold value:
        return momentum - this.maxMomentum;

    }

    /** {@inheritDoc} */
    @Override
    public EventDetector copy() {
        return new AngularMomentumExcessDetector(maxMomentum, inertiaFrame, getSlopeSelection(), getMaxCheckInterval(),
                getThreshold(), actionAtAscending, actionAtDescending);
    }
}
