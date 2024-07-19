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
 * 
 * @history created 21/03/12
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:207:27/03/2014:Added type of AOL to detect as well as reference equator
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:226:12/09/2014: problem with event detections.
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:454:24/11/2015:Add constructors, overload method shouldBeRemoved()
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Detects when the Argument of Latitude of the spacecraft reaches a predetermined value, &theta;. <br>
 * The Argument of Latitude is not defined for all kinds of orbits: this detector will detect an
 * event only if the corresponding orbit is not an equatorial orbit, otherwise it may trigger events
 * randomly.
 * <p>
 * The default implementation behaviour is to {@link EventDetector.Action#STOP stop} propagation when the angle &theta;
 * is reached. This can be changed by using provided constructors.
 * 
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment attributes are mutable and related to propagation.
 * 
 * @see EventDetector
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id: AOLDetector.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.1
 */
public class AOLDetector extends AbstractDetector {

     /** Serializable UID. */
    private static final long serialVersionUID = 1950385578566948721L;

    /** Angle triggering the event. */
    private final double aol;

    /** Type of angle triggering the event. */
    private final PositionAngle typeToDetect;

    /** Reference frame to compute AOL. */
    private final Frame refFrame;

    /** Action performed */
    private final Action actionAOL;

    /**
     * Constructor for an AOLDetector instance.
     * 
     * @param angle AOL value triggering the event.
     * @param type type of AOL to detect, one of TRUE, MEAN and ECCENTRIC.
     * @param equator equator with respect to which the AOL is to be computed
     * @see PositionAngle
     */
    public AOLDetector(final double angle, final PositionAngle type, final Frame equator) {
        this(angle, type, equator, DEFAULT_MAXCHECK, DEFAULT_THRESHOLD);
    }

    /**
     * Constructor for an AOLDetector instance with complementary parameters.
     * <p>
     * The default implementation behaviour is to {@link EventDetector.Action#STOP stop} propagation when the AOL angle
     * is reached.
     * </p>
     * 
     * @param angle AOL value triggering the event.
     * @param type type of AOL to detect, one of TRUE, MEAN and ECCENTRIC.
     * @param equator equator with respect to which the AOL is to be computed
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold threshold (see {@link AbstractDetector})
     */
    public AOLDetector(final double angle, final PositionAngle type, final Frame equator,
        final double maxCheck, final double threshold) {
        this(angle, type, equator, maxCheck, threshold, Action.STOP);
    }

    /**
     * Constructor for an AOLDetector instance with complementary parameters.
     * 
     * @param angle AOL value triggering the event.
     * @param type type of AOL to detect, one of TRUE, MEAN and ECCENTRIC.
     * @param equator equator with respect to which the AOL is to be computed
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold threshold (see {@link AbstractDetector})
     * @param action action performed at AOL detection
     */
    public AOLDetector(final double angle, final PositionAngle type, final Frame equator,
        final double maxCheck, final double threshold, final Action action) {
        this(angle, type, equator, maxCheck, threshold, action, false);
    }

    /**
     * Constructor for an AOLDetector instance with complementary parameters.
     * 
     * @param angle AOL value triggering the event.
     * @param type type of AOL to detect, one of TRUE, MEAN and ECCENTRIC.
     * @param equator equator with respect to which the AOL is to be computed
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold threshold (see {@link AbstractDetector})
     * @param action action performed at AOL detection
     * @param remove true if detector should be removed
     * @since 3.1
     */
    public AOLDetector(final double angle, final PositionAngle type, final Frame equator,
        final double maxCheck, final double threshold, final Action action, final boolean remove) {
        // the AOL event is triggered when the g-function slope is positive at its zero:
        super(EventDetector.INCREASING, maxCheck, threshold);
        this.aol = angle;
        this.typeToDetect = type;
        this.refFrame = equator;

        // action
        this.actionAOL = action;
        // remove (or not) detector
        this.shouldBeRemovedFlag = remove;
    }

    /**
     * Handle an AOL event and choose what to do next.
     * 
     * @param s the current state information : date, kinematics, attitude
     * @param increasing if true, the value of the switching function increases when times increases
     *        around event
     * @param forward if true, the integration variable (time) increases during integration.
     * @return the action performed when when the AOL angle is reached.
     * @exception PatriusException if some specific error occurs
     */
    @Override
    public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                final boolean forward) throws PatriusException {
        return this.actionAOL;
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public double g(final SpacecraftState state) throws PatriusException {

        final CircularOrbit orbit = this.getOrbitInUserFrame(state.getOrbit());

        if (orbit.getI() < Precision.DOUBLE_COMPARISON_EPSILON) {
            // equatorial orbit: AOL is undefined
            return 0;
        } else {
            // non-equatorial orbit: get aol wrt user specified type
            double current = 0;
            switch (this.typeToDetect) {
                case TRUE:
                    current = orbit.getAlphaV();
                    break;
                case MEAN:
                    current = orbit.getAlphaM();
                    break;
                case ECCENTRIC:
                    current = orbit.getAlphaE();
                    break;
                default:
                    throw PatriusException.createIllegalArgumentException(
                        PatriusMessages.UNKNOWN_PARAMETER, this.typeToDetect);
            }

            // computes the sinus of the difference between the actual spacecraft AOL and the
            // threshold value:
            return MathLib.sin(current - this.aol);
        }
    }

    /**
     * Convert the input orbit to a CircularOrbit with respect to the user specified equator
     * 
     * @param orbit orbit in spacecraft state frame
     * @return orbit in user specified equator
     * @throws PatriusException if frame conversion fails
     */
    private CircularOrbit getOrbitInUserFrame(final Orbit orbit) throws PatriusException {

        // container
        CircularOrbit result = null;

        if (orbit.getFrame().equals(this.refFrame)) {
            result = (CircularOrbit) OrbitType.CIRCULAR.convertType(orbit);
        } else {
            if (this.refFrame.isPseudoInertial()) {
                // if the user frame is inertial, create a new orbit with that frame
                final PVCoordinates pvInRef = orbit.getPVCoordinates(this.refFrame);
                result = new CircularOrbit(pvInRef, this.refFrame, orbit.getDate(), orbit.getMu());
            } else {
                // otherwise, compute the pvcoordinates manually (without
                // taking into account dynamics effects linked to the reference
                // frame being non inertial)

                // the full transformation
                final Transform transformation = orbit.getFrame().getTransformTo(this.refFrame,
                    orbit.getDate());

                // rotation and rotation rate
                final Rotation rotation = transformation.getAngular().getRotation();

                // translation
                final Vector3D translation = transformation.getCartesian().getPosition();

                // pvs to transform
                final Vector3D p = orbit.getPVCoordinates().getPosition();
                final Vector3D v = orbit.getPVCoordinates().getVelocity();

                // PVs
                final Vector3D transformedP = rotation.applyInverseTo(p.subtract(translation));
                final Vector3D transformedV = rotation.applyInverseTo(v);

                final PVCoordinates pvInRef = new PVCoordinates(transformedP, transformedV);

                // new orbit- the frame doesnt matter because the g method uses only the conversion
                // methods
                result = new CircularOrbit(pvInRef, FramesFactory.getGCRF(), orbit.getDate(),
                    orbit.getMu());
            }
        }

        return result;
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        // Does nothing
    }

    /**
     * Get the AOL to detect.
     * 
     * @return the angle triggering the event.
     */
    public double getAOL() {
        return this.aol;
    }

    /**
     * Get the type of AOL to detect.
     * 
     * @return the type of angle triggering the event
     */
    public PositionAngle getAOLType() {
        return this.typeToDetect;
    }

    /**
     * Get the reference frame.
     * 
     * @return the reference frame to compute AOL
     */
    public Frame getAOLFrame() {
        return this.refFrame;
    }

    /**
     * Return the action at detection.
     * 
     * @return action at detection
     */
    public Action getAction() {
        return this.actionAOL;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The following attributes are not deeply copied:
     * <ul>
     * <li>refFrame: {@link Frame}</li>
     * </ul>
     * </p>
     */
    @Override
    public EventDetector copy() {
        return new AOLDetector(this.aol, this.typeToDetect, this.refFrame, this.getMaxCheckInterval(),
            this.getThreshold(),
            this.actionAOL, this.shouldBeRemovedFlag);
    }
}
