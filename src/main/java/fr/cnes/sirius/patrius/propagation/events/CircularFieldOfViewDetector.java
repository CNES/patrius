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
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
* VERSION:4.9:DM:DM-3181:10/05/2022:[PATRIUS] Passage a protected de la methode setPropagationDelayType
* VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2460:27/05/2020:Prise en compte des temps de propagation dans les calculs evenements
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 *                             (added forward parameter to eventOccurred signature)
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:454:24/11/2015:Add constructors, overload method shouldBeRemoved() and adapt eventOccured()
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Finder for target entry/exit events with respect to a satellite sensor field of view.
 * <p>
 * This class handle fields of view with a circular boundary.
 * </p>
 * <p>
 * The default implementation behavior is to {@link EventDetector.Action#CONTINUE continue} propagation at fov entry and
 * to {@link EventDetector.Action#STOP stop} propagation at fov exit. This can be changed by using the constructor
 * {@link #CircularFieldOfViewDetector(PVCoordinatesProvider, Vector3D, double, double, double, 
 * fr.cnes.sirius.patrius.propagation.events.EventDetector.Action, 
 * fr.cnes.sirius.patrius.propagation.events.EventDetector.Action)
 * CircularFieldOfViewDetector}.
 * </p>
 * <p>
 * This detector can takes into account signal propagation duration through
 * {@link #setPropagationDelayType(PropagationDelayType, fr.cnes.sirius.patrius.frames.Frame)} 
 * (default is signal being instantaneous).
 * </p>
 * 
 * @see fr.cnes.sirius.patrius.propagation.Propagator#addEventDetector(EventDetector)
 * @see DihedralFieldOfViewDetector
 * @author V&eacute;ronique Pommier-Maurussane
 */
public class CircularFieldOfViewDetector extends AbstractDetector {

    /** Default convergence threshold (s). */
    private static final double DEFAULT_THRESHOLD = 1.e-3;

    /** Serializable UID. */
    private static final long serialVersionUID = 4571340030201230951L;

    /** Position/velocity provider of the considered target. */
    private final PVCoordinatesProvider targetPVProvider;

    /** Direction of the fov center. */
    private final Vector3D center;

    /** Fov half aperture angle. */
    private final double halfAperture;

    /**
     * Build a new instance.
     * <p>
     * The maximal interval between distance to fov boundary checks should be smaller than the half duration of the
     * minimal pass to handle, otherwise some short passes could be missed.
     * </p>
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#CONTINUE continue} propagation at fov entry
     * and to {@link EventDetector.Action#STOP stop} propagation at fov exit.
     * </p>
     * 
     * @param pvTarget Position/velocity provider of the considered target
     * @param centerIn Direction of the fov center, in spacecraft frame
     * @param halfApertureIn Fov half aperture angle
     * @param maxCheckIn maximal interval in seconds
     */
    public CircularFieldOfViewDetector(final PVCoordinatesProvider pvTarget,
        final Vector3D centerIn, final double halfApertureIn, final double maxCheckIn) {
        this(pvTarget, centerIn, halfApertureIn, maxCheckIn, DEFAULT_THRESHOLD);
    }

    /**
     * Build a new instance.
     * <p>
     * The maximal interval between distance to fov boundary checks should be smaller than the half duration of the
     * minimal pass to handle, otherwise some short passes could be missed.
     * </p>
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#CONTINUE continue} propagation at fov entry
     * and to {@link EventDetector.Action#STOP stop} propagation at fov exit.
     * </p>
     * 
     * @param pvTarget Position/velocity provider of the considered target
     * @param centerIn Direction of the fov center, in spacecraft frame
     * @param halfApertureIn Fov half aperture angle
     * @param maxCheck maximal interval in seconds
     * @param threshold threshold in seconds for events date computation
     */
    public CircularFieldOfViewDetector(final PVCoordinatesProvider pvTarget,
        final Vector3D centerIn, final double halfApertureIn, final double maxCheck,
        final double threshold) {
        this(pvTarget, centerIn, halfApertureIn, maxCheck, threshold, Action.CONTINUE, Action.STOP);
    }

    /**
     * Build a new instance with defined actions at fov entry and exit.
     * <p>
     * The maximal interval between distance to fov boundary checks should be smaller than the half duration of the
     * minimal pass to handle, otherwise some short passes could be missed.
     * </p>
     * 
     * @param pvTarget Position/velocity provider of the considered target
     * @param centerIn Direction of the fov center, in spacecraft frame
     * @param halfApertureIn Fov half aperture angle
     * @param maxCheck maximal interval in seconds
     * @param threshold threshold in seconds for events date computation
     * @param entry action performed at fov entry
     * @param exit action performed at fov exit
     */
    public CircularFieldOfViewDetector(final PVCoordinatesProvider pvTarget,
        final Vector3D centerIn, final double halfApertureIn, final double maxCheck,
        final double threshold, final Action entry, final Action exit) {

        this(pvTarget, centerIn, halfApertureIn, maxCheck, threshold, entry, exit, false, false);
    }

    /**
     * Build a new instance with defined actions at fov entry and exit.
     * <p>
     * The maximal interval between distance to fov boundary checks should be smaller than the half duration of the
     * minimal pass to handle, otherwise some short passes could be missed.
     * </p>
     * 
     * @param pvTarget Position/velocity provider of the considered target
     * @param centerIn Direction of the fov center, in spacecraft frame
     * @param halfApertureIn Fov half aperture angle
     * @param maxCheck maximal interval in seconds
     * @param threshold threshold in seconds for events date computation
     * @param entry action performed at fov entry
     * @param exit action performed at fov exit
     * @param removeEntry true if detector should be removed at fov entry
     * @param removeExit true if detector should be removed at fov exit
     * @since 3.1
     */
    public CircularFieldOfViewDetector(final PVCoordinatesProvider pvTarget,
        final Vector3D centerIn, final double halfApertureIn, final double maxCheck,
        final double threshold, final Action entry, final Action exit,
        final boolean removeEntry, final boolean removeExit) {
        super(maxCheck, threshold, entry, exit, removeEntry, removeExit);
        this.targetPVProvider = pvTarget;
        this.center = centerIn;
        this.halfAperture = halfApertureIn;
    }

    /**
     * Get the position/velocity provider of the target .
     * 
     * @return the position/velocity provider of the target
     */
    public PVCoordinatesProvider getPVTarget() {
        return this.targetPVProvider;
    }

    /**
     * Get the direction of fov center.
     * 
     * @return the direction of fov center
     */
    public Vector3D getCenter() {
        return this.center;
    }

    /**
     * Get fov half aperture angle.
     * 
     * @return the fov half aperture angle
     */
    public double getHalfAperture() {
        return this.halfAperture;
    }

    /**
     * Handle an fov event and choose what to do next.
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#CONTINUE continue} propagation at entry and
     * to {@link EventDetector.Action#STOP stop} propagation at exit. This can be changed by using the constructor
     * {@link #CircularFieldOfViewDetector(PVCoordinatesProvider, Vector3D, double, double, double, 
     * fr.cnes.sirius.patrius.propagation.events.EventDetector.Action, 
     * fr.cnes.sirius.patrius.propagation.events.EventDetector.Action)
     * CircularFieldOfViewDetector}
     * </p>
     * 
     * @param s the current state information : date, kinematics, attitude
     * @param increasing if true, the value of the switching function increases when times increases
     *        around event, i.e. target enters the fov (note that increase is measured with respect
     *        to physical time, not with respect to propagation which may go backward in time)
     * @param forward if true, the integration variable (time) increases during integration.
     * @return the action performed at expected fov entry or exit.
     * @exception PatriusException if some specific error occurs
     */
    @Override
    public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                final boolean forward) throws PatriusException {
        if (increasing) {
            this.shouldBeRemovedFlag = this.isRemoveAtEntry();
        } else {
            this.shouldBeRemovedFlag = this.isRemoveAtExit();
        }
        return increasing ? this.getActionAtEntry() : this.getActionAtExit();
    }

    /**
     * {@inheritDoc} g function value is the difference between fov half aperture and the absolute
     * value of the angle between target direction and field of view center. It is positive inside
     * the fov and negative outside.
     */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public double g(final SpacecraftState s) throws PatriusException {

        // Compute target position/velocity at date in spacecraft frame */
        final AbsoluteDate targetDate = getSignalEmissionDate(this.targetPVProvider, s.getOrbit(), s.getDate());
        final Vector3D targetPosInert = new Vector3D(1, this.targetPVProvider.getPVCoordinates(
                targetDate, s.getFrame()).getPosition(), -1, s.getPVCoordinates().getPosition());
        final Vector3D targetPosSat = s.getAttitude().getRotation().applyInverseTo(targetPosInert);

        // Target is in the field of view if the absolute value that angle is smaller than fov half
        // aperture.
        // g function value is the difference between fov half aperture and the absolute value of
        // the angle between
        // target direction and field of view center. It is positive inside the fov and negative
        // outside.
        return this.halfAperture - Vector3D.angle(targetPosSat, this.center);
    }
    
    /** @inheritDoc */
    @Override
    public void setPropagationDelayType(final PropagationDelayType propagationDelayType, final Frame frame) {
        super.setPropagationDelayType(propagationDelayType, frame);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The following attributes are not deeply copied:
     * <ul>
     * <li>targetPVProvider: {@link PVCoordinatesProvider}</li>
     * </ul>
     * </p>
     */
    @Override
    public EventDetector copy() {
        final CircularFieldOfViewDetector res = new CircularFieldOfViewDetector(this.targetPVProvider, new Vector3D(1.,
                this.center), this.halfAperture, this.getMaxCheckInterval(), this.getThreshold(),
            this.getActionAtEntry(), this.getActionAtExit(), this.isRemoveAtEntry(), this.isRemoveAtExit());
        res.setPropagationDelayType(getPropagationDelayType(), getInertialFrame());
        return res;
    }
}
