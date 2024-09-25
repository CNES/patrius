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
* VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
* VERSION:4.13:DM:DM-37:08/12/2023:[PATRIUS] Date d'evenement et propagation du signal
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
 * VERSION::FA:485:12/11/2015:Add orthogonal test for vectors (center,axis1),(center,axis2) at construction
 * VERSION::DM:454:24/11/2015:Add constructors, overload method shouldBeRemoved() and adapt eventOccured()
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events.detectors;

import fr.cnes.sirius.patrius.events.EventDetector;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Finder for body entering/exiting dihedral fov events.
 * <p>
 * This class finds dihedral field of view events (i.e. body entry and exit in fov).
 * </p>
 * <p>
 * The default implementation behavior is to {@link EventDetector.Action#CONTINUE continue} propagation at entry and to
 * {@link EventDetector.Action#STOP stop} propagation at exit. This can be changed by using provided constructors.
 * </p>
 * <p>
 * This detector can takes into account signal propagation duration through
 * {@link #setPropagationDelayType(PropagationDelayType, Frame)} (default is signal being instantaneous).
 * </p>
 * 
 * @see fr.cnes.sirius.patrius.propagation.Propagator#addEventDetector(EventDetector)
 * @see CircularFieldOfViewDetector
 * @author V&eacute;ronique Pommier-Maurussane
 */
public class DihedralFieldOfViewDetector extends AbstractSignalPropagationDetector {

    /** Default convergence threshold (s). */
    private static final double DEFAULT_THRESHOLD = 1.e-3;

    /** Serializable UID. */
    private static final long serialVersionUID = 4571340030201230951L;

    /** Position/velocity provider of the considered target. */
    private final PVCoordinatesProvider targetPVProvider;

    /** Direction of the fov center. */
    private final Vector3D center;

    /** Fov dihedral axis 1. */
    private final Vector3D normalCenterPlane1;

    /** Fov dihedral half aperture angle 1. */
    private final double halfAperture1;

    /** Fov dihedral axis 2. */
    private final Vector3D normalCenterPlane2;

    /** Fov dihedral half aperture angle 2. */
    private final double halfAperture2;

    /**
     * Build a new instance.
     * <p>
     * The maximal interval between distance to fov boundary checks should be smaller than the half duration of the
     * minimal pass to handle, otherwise some short passes could be missed.
     * </p>
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#CONTINUE continue} propagation at entry and
     * to {@link EventDetector.Action#STOP stop} propagation at exit.
     * </p>
     * 
     * @param pvTarget Position/velocity provider of the considered target
     * @param centerIn Direction of the fov center
     * @param axis1 Fov dihedral axis 1
     * @param halfAperture1In Fov dihedral half aperture angle 1
     * @param axis2 Fov dihedral axis 2
     * @param halfAperture2In Fov dihedral half aperture angle 2
     * @param maxCheck maximal interval in seconds
     * @throws PatriusException if vectors center and axis1 or center and axis2 are not strictly
     *         orthogonal
     */
    public DihedralFieldOfViewDetector(final PVCoordinatesProvider pvTarget,
        final Vector3D centerIn, final Vector3D axis1, final double halfAperture1In,
        final Vector3D axis2, final double halfAperture2In, final double maxCheck)
        throws PatriusException {
        this(pvTarget, centerIn, axis1, halfAperture1In, axis2, halfAperture2In, maxCheck,
            Action.CONTINUE, Action.STOP);
    }

    /**
     * Build a new instance.
     * <p>
     * The maximal interval between distance to fov boundary checks should be smaller than the half duration of the
     * minimal pass to handle, otherwise some short passes could be missed.
     * </p>
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#CONTINUE continue} propagation at entry and
     * to {@link EventDetector.Action#STOP stop} propagation at exit.
     * </p>
     * 
     * @param pvTarget Position/velocity provider of the considered target
     * @param centerIn Direction of the fov center
     * @param axis1 Fov dihedral axis 1
     * @param halfAperture1In Fov dihedral half aperture angle 1
     * @param axis2 Fov dihedral axis 2
     * @param halfAperture2In Fov dihedral half aperture angle 2
     * @param maxCheck maximal interval in seconds
     * @param epsilon threshold determining if vectors are orthogonal or not
     * @throws PatriusException if vectors center and axis1 or center and axis2 are not orthogonal
     *         regarding epsilon
     */
    public DihedralFieldOfViewDetector(final PVCoordinatesProvider pvTarget,
        final Vector3D centerIn, final Vector3D axis1, final double halfAperture1In,
        final Vector3D axis2, final double halfAperture2In, final double maxCheck,
        final double epsilon) throws PatriusException {

        this(pvTarget, centerIn, axis1, halfAperture1In, axis2, halfAperture2In, maxCheck,
            Action.CONTINUE, Action.STOP, epsilon);
    }

    /**
     * Build a new instance.
     * <p>
     * The maximal interval between distance to fov boundary checks should be smaller than the half duration of the
     * minimal pass to handle, otherwise some short passes could be missed.
     * </p>
     * 
     * @param pvTarget Position/velocity provider of the considered target
     * @param centerIn Direction of the fov center
     * @param axis1 Fov dihedral axis 1
     * @param halfAperture1In Fov dihedral half aperture angle 1
     * @param axis2 Fov dihedral axis 2
     * @param halfAperture2In Fov dihedral half aperture angle 2
     * @param maxCheck maximal interval in seconds
     * @param entry action performed at fov entry
     * @param exit action performed at fov exit
     * @throws PatriusException if vectors center and axis1 or center and axis2 are not strictly
     *         orthogonal
     */
    public DihedralFieldOfViewDetector(final PVCoordinatesProvider pvTarget,
        final Vector3D centerIn, final Vector3D axis1, final double halfAperture1In,
        final Vector3D axis2, final double halfAperture2In, final double maxCheck,
        final Action entry, final Action exit) throws PatriusException {

        this(pvTarget, centerIn, axis1, halfAperture1In, axis2, halfAperture2In, maxCheck, entry,
            exit, false, false);
    }

    /**
     * Build a new instance.
     * <p>
     * The maximal interval between distance to fov boundary checks should be smaller than the half duration of the
     * minimal pass to handle, otherwise some short passes could be missed.
     * </p>
     * 
     * @param pvTarget Position/velocity provider of the considered target
     * @param centerIn Direction of the fov center
     * @param axis1 Fov dihedral axis 1
     * @param halfAperture1In Fov dihedral half aperture angle 1
     * @param axis2 Fov dihedral axis 2
     * @param halfAperture2In Fov dihedral half aperture angle 2
     * @param maxCheck maximal interval in seconds
     * @param entry action performed at fov entry
     * @param exit action performed at fov exit
     * @param removeEntry true if detector should be removed at fov entry
     * @param removeExit true if detector should be removed at fov exit
     * @since 3.1
     * @throws PatriusException if vectors center and axis1 or center and axis2 are not strictly
     *         orthogonal
     */
    public DihedralFieldOfViewDetector(final PVCoordinatesProvider pvTarget,
        final Vector3D centerIn, final Vector3D axis1, final double halfAperture1In,
        final Vector3D axis2, final double halfAperture2In, final double maxCheck,
        final Action entry, final Action exit, final boolean removeEntry,
        final boolean removeExit) throws PatriusException {

        super(maxCheck, DEFAULT_THRESHOLD, entry, exit, removeEntry, removeExit);
        this.targetPVProvider = pvTarget;
        this.center = centerIn;

        // Computation of the center plane normal for dihedra 1
        this.normalCenterPlane1 = Vector3D.crossProduct(axis1, centerIn);

        // Computation of the center plane normal for dihedra 2
        this.normalCenterPlane2 = Vector3D.crossProduct(axis2, centerIn);

        this.halfAperture1 = halfAperture1In;
        this.halfAperture2 = halfAperture2In;

        if (centerIn.dotProduct(axis1) != 0.0) {
            throw new PatriusException(PatriusMessages.DIHEDRAL_FOV_NOT_ORTHOGONAL_AXIS, axis1);
        } else if (centerIn.dotProduct(axis2) != 0.0) {
            throw new PatriusException(PatriusMessages.DIHEDRAL_FOV_NOT_ORTHOGONAL_AXIS, axis2);
        }
    }

    /**
     * Build a new instance.
     * <p>
     * The maximal interval between distance to fov boundary checks should be smaller than the half duration of the
     * minimal pass to handle, otherwise some short passes could be missed.
     * </p>
     * 
     * @param pvTarget Position/velocity provider of the considered target
     * @param centerIn Direction of the fov center
     * @param axis1 Fov dihedral axis 1
     * @param halfAperture1In Fov dihedral half aperture angle 1
     * @param axis2 Fov dihedral axis 2
     * @param halfAperture2In Fov dihedral half aperture angle 2
     * @param maxCheck maximal interval in seconds
     * @param entry action performed at fov entry
     * @param exit action performed at fov exit
     * @param epsilon threshold determining if vectors are orthogonal or not
     * @throws PatriusException if vectors center and axis1 or center and axis2 are not orthogonal
     *         regarding epsilon
     */
    public DihedralFieldOfViewDetector(final PVCoordinatesProvider pvTarget,
        final Vector3D centerIn, final Vector3D axis1, final double halfAperture1In,
        final Vector3D axis2, final double halfAperture2In, final double maxCheck,
        final Action entry, final Action exit, final double epsilon) throws PatriusException {

        this(pvTarget, centerIn, axis1, halfAperture1In, axis2, halfAperture2In, maxCheck, entry,
            exit, false, false, epsilon);
    }

    /**
     * Build a new instance.
     * <p>
     * The maximal interval between distance to fov boundary checks should be smaller than the half duration of the
     * minimal pass to handle, otherwise some short passes could be missed.
     * </p>
     * 
     * @param pvTarget Position/velocity provider of the considered target
     * @param centerIn Direction of the fov center
     * @param axis1 Fov dihedral axis 1
     * @param halfAperture1In Fov dihedral half aperture angle 1
     * @param axis2 Fov dihedral axis 2
     * @param halfAperture2In Fov dihedral half aperture angle 2
     * @param maxCheck maximal interval in seconds
     * @param entry action performed at fov entry
     * @param exit action performed at fov exit
     * @param removeEntry true if detector should be removed at fov entry
     * @param removeExit true if detector should be removed at fov exit
     * @param epsilon threshold determining if vectors are orthogonal or not
     * @throws PatriusException if vectors center and axis1 or center and axis2 are not orthogonal
     *         regarding epsilon
     */
    public DihedralFieldOfViewDetector(final PVCoordinatesProvider pvTarget,
        final Vector3D centerIn, final Vector3D axis1, final double halfAperture1In,
        final Vector3D axis2, final double halfAperture2In, final double maxCheck,
        final Action entry, final Action exit, final boolean removeEntry,
        final boolean removeExit, final double epsilon) throws PatriusException {

        super(maxCheck, DEFAULT_THRESHOLD, entry, exit, removeEntry, removeExit);
        this.targetPVProvider = pvTarget;
        this.center = centerIn;

        // Computation of the center plane normal for dihedra 1
        this.normalCenterPlane1 = Vector3D.crossProduct(axis1, centerIn);

        // Computation of the center plane normal for dihedra 2
        this.normalCenterPlane2 = Vector3D.crossProduct(axis2, centerIn);

        this.halfAperture1 = halfAperture1In;
        this.halfAperture2 = halfAperture2In;

        if (centerIn.dotProduct(axis1) > epsilon) {
            throw new PatriusException(PatriusMessages.DIHEDRAL_FOV_NOT_ORTHOGONAL_AXIS, axis1);
        } else if (centerIn.dotProduct(axis2) > epsilon) {
            throw new PatriusException(PatriusMessages.DIHEDRAL_FOV_NOT_ORTHOGONAL_AXIS, axis2);
        }
    }

    /**
     * Private construor for copy method.
     * 
     * @param maxCheck maximal interval in seconds
     * @param pvTarget Position/velocity provider of the considered target
     * @param centerIn Direction of the fov center
     * @param normalCenterPlane1In Fov dihedral axis 1
     * @param halfAperture1In Fov dihedral half aperture angle 1
     * @param normalCenterPlane2In Fov dihedral axis 2
     * @param halfAperture2In Fov dihedral half aperture angle 2
     * @param entry action performed at fov entry
     * @param exit action performed at fov exit
     * @param removeEntry true if detector should be removed at fov entry
     * @param removeExit true if detector should be removed at fov exit
     */
    private DihedralFieldOfViewDetector(final double maxCheck, final PVCoordinatesProvider pvTarget,
                                        final Vector3D centerIn, final Vector3D normalCenterPlane1In,
                                        final double halfAperture1In, final Vector3D normalCenterPlane2In,
                                        final double halfAperture2In, final Action entry, final Action exit,
                                        final boolean removeEntry, final boolean removeExit) {

        super(maxCheck, DEFAULT_THRESHOLD, entry, exit, removeEntry, removeExit);

        this.targetPVProvider = pvTarget;
        this.center = centerIn;
        this.normalCenterPlane1 = normalCenterPlane1In;
        this.halfAperture1 = halfAperture1In;
        this.normalCenterPlane2 = normalCenterPlane2In;
        this.halfAperture2 = halfAperture2In;
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
     * Get the direction of fov 1st dihedral axis.
     * 
     * @return the direction of fov 1st dihedral axis
     */
    public Vector3D getAxis1() {
        return Vector3D.crossProduct(this.center, this.normalCenterPlane1);
    }

    /**
     * Get the half aperture angle of fov 1st dihedra.
     * 
     * @return the half aperture angle of fov 1st dihedras
     */
    public double getHalfAperture1() {
        return this.halfAperture1;
    }

    /**
     * Get the half aperture angle of fov 2nd dihedra.
     * 
     * @return the half aperture angle of fov 2nd dihedras
     */
    public double getHalfAperture2() {
        return this.halfAperture2;
    }

    /**
     * Get the direction of fov 2nd dihedral axis.
     * 
     * @return the direction of fov 2nd dihedral axis
     */
    public Vector3D getAxis2() {
        return Vector3D.crossProduct(this.center, this.normalCenterPlane2);
    }

    /**
     * Handle an fov event and choose what to do next.
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#CONTINUE continue} propagation at entry and
     * to {@link EventDetector.Action#STOP stop} propagation at exit. This can be changed by overriding the
     * {@link #eventOccurred(SpacecraftState, boolean, boolean)
     * eventOccurred} method in a derived class.
     * </p>
     * 
     * @param s the current state information : date, kinematics, attitude
     * @param increasing if true, the value of the switching function increases when times increases
     *        around event, i.e. target enters the fov (note that increase is measured with respect
     *        to physical time, not with respect to propagation which may go backward in time)
     * @param forward if true, the integration variable (time) increases during integration.
     * @return the action performed at fov entry or exit.
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
     * {@inheritDoc} g function value is the target signed distance to the closest fov boundary. It
     * is positive inside the fov, and negative outside.
     */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public double g(final SpacecraftState s) throws PatriusException {

        // Get position of target at current date in spacecraft frame.
        final AbsoluteDate targetDate = getSignalEmissionDate(s);
        final Vector3D targetPosInert = new Vector3D(1, this.targetPVProvider.getPVCoordinates(
                targetDate, s.getFrame()).getPosition(), -1, s.getPVCoordinates().getPosition());
        final Vector3D targetPosSat = s.getAttitude().getRotation().applyInverseTo(targetPosInert);

        // Compute the four angles from the four fov boundaries.
        final double angle1 = MathLib.atan2(Vector3D.dotProduct(targetPosSat, this.normalCenterPlane1),
            Vector3D.dotProduct(targetPosSat, this.center));
        final double angle2 = MathLib.atan2(Vector3D.dotProduct(targetPosSat, this.normalCenterPlane2),
            Vector3D.dotProduct(targetPosSat, this.center));

        // g function value is distance to the fov boundary, computed as a dihedral angle.
        // It is positive inside the fov, and negative outside.
        return MathLib.min(this.halfAperture1 - MathLib.abs(angle1),
            this.halfAperture2 - MathLib.abs(angle2));
    }
    
    /** @inheritDoc */
    @Override
    public void setPropagationDelayType(final PropagationDelayType propagationDelayType, final Frame frame) {
        super.setPropagationDelayType(propagationDelayType, frame);
    }

    /** {@inheritDoc} */
    @Override
    public PVCoordinatesProvider getEmitter(final SpacecraftState s) {
        return this.targetPVProvider;
    }

    /** {@inheritDoc} */
    @Override
    public PVCoordinatesProvider getReceiver(final SpacecraftState s) {
        return s.getOrbit();
    }

    /** {@inheritDoc} */
    @Override
    public DatationChoice getDatationChoice() {
        return DatationChoice.RECEIVER;
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
        final DihedralFieldOfViewDetector res = new DihedralFieldOfViewDetector(getMaxCheckInterval(),
            this.targetPVProvider, this.center, this.normalCenterPlane1, this.halfAperture1,
            this.normalCenterPlane2, this.halfAperture2, this.actionAtEntry, this.actionAtExit, this.removeAtEntry,
            this.removeAtExit);
        res.setPropagationDelayType(getPropagationDelayType(), getInertialFrame());
        return res;
    }
}
