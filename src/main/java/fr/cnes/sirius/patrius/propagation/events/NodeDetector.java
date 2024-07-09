/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */
/*
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 *                             (added forward parameter to eventOccurred signature)
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:454:24/11/2015:Add constructors, overload method shouldBeRemoved() and adapt eventOccured()
 * VERSION::DM:1173:24/08/2017:add propulsive and engine properties
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Finder for node crossing events.
 * <p>
 * This class finds equator crossing events (i.e. ascending and/or descending node crossing).
 * </p>
 * <p>
 * The default implementation behavior is to {@link EventDetector.Action#STOP stop} propagation at node crossing. This
 * can be changed by overriding one of the following constructors :
 * </p>
 * <ul>
 * <li>
 * {@link #NodeDetector(Frame, int, double, double, fr.cnes.sirius.patrius.propagation.events.EventDetector.Action)
 * NodeDetector} : the defined action is performed at ascending OR/AND descending node depending on slope selection
 * defined.
 * <li>
 * {@link #NodeDetector(Frame, double, double, fr.cnes.sirius.patrius.propagation.events.EventDetector.Action,
 * fr.cnes.sirius.patrius.propagation.events.EventDetector.Action)
 * NodeDetector} : the defined actions are performed at ascending AND descending node.
 * </ul>
 * <p>
 * Beware that node detection will fail for almost equatorial orbits.
 * </p>
 * 
 * @see fr.cnes.sirius.patrius.propagation.Propagator#addEventDetector(EventDetector)
 * @author Luc Maisonobe
 */
@SuppressWarnings("PMD.NullAssignment")
public class NodeDetector extends AbstractDetector {

    /** Flag for ascending node detection (slopeSelection = 0). */
    public static final int ASCENDING = 0;

    /** Flag for descending node detection (slopeSelection = 1). */
    public static final int DESCENDING = 1;

    /** Flag for both ascending and descending node detection (slopeSelection = 2). */
    public static final int ASCENDING_DESCENDING = 2;

    /** Default convergence threshold (in % of Keplerian period). */
    private static final double DEFAULT_THRESHOLD = 1.0e-13;

    /** Serializable UID. */
    private static final long serialVersionUID = 601812664015866572L;

    /** Frame in which the equator is defined. */
    private final Frame frame;

    /** Action performed at ascending node detection. */
    private Action actionAscendingNote;

    /** Action performed at descending node detection. */
    private Action actionDescendingNode;

    /** True if detector should be removed at ascending node detection. */
    private boolean removeAtAscendingNode;

    /** True if detector should be removed at descending node detection. */
    private boolean removeAtDescendingNode;

    /** True if detector should be removed (updated by eventOccurred). */
    private boolean shouldBeRemovedFlag = false;

    /**
     * Build a new instance.
     * <p>
     * The orbit is used only to set an upper bound for the max check interval to period/3 and to set the convergence
     * threshold according to orbit size.
     * </p>
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#STOP stop} propagation at node crossing.
     * </p>
     * 
     * @param orbit initial orbit
     * @param frameIn frame in which the equator is defined (typical values are
     *        {@link fr.cnes.sirius.patrius.frames.FramesFactory#getEME2000() J<sub>2000</sub>} or
     *        {@link fr.cnes.sirius.patrius.frames.FramesFactory#getITRF() ITRF 2005})
     * @param slopeSelection <br>
     *        {@link NodeDetector#ASCENDING} for ascending node detection,<br>
     *        {@link NodeDetector#DESCENDING} for descending node detection,<br>
     *        {@link NodeDetector#ASCENDING_DESCENDING} for both ascending and descending node
     *        detection.
     */
    public NodeDetector(final Orbit orbit, final Frame frameIn, final int slopeSelection) {
        this(frameIn, slopeSelection, orbit.getKeplerianPeriod() / 3, DEFAULT_THRESHOLD
            * orbit.getKeplerianPeriod(), Action.STOP);
    }

    /**
     * Build a new instance.
     * <p>
     * The orbit is used only to set an upper bound for the max check interval to period/3.
     * </p>
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#STOP stop} propagation at node crossing.
     * </p>
     * 
     * @param orbit initial orbit
     * @param frameIn frame in which the equator is defined (typical values are
     *        {@link fr.cnes.sirius.patrius.frames.FramesFactory#getEME2000() J<sub>2000</sub>} or
     *        {@link fr.cnes.sirius.patrius.frames.FramesFactory#getITRF() ITRF 2005})
     * @param slopeSelection <br>
     *        {@link NodeDetector#ASCENDING} for ascending node detection,<br>
     *        {@link NodeDetector#DESCENDING} for descending node detection,<br>
     *        {@link NodeDetector#ASCENDING_DESCENDING} for both ascending and descending node
     *        detection.
     * @param threshold convergence threshold (s)
     */
    public NodeDetector(final Orbit orbit, final Frame frameIn, final int slopeSelection,
        final double threshold) {
        this(frameIn, slopeSelection, orbit.getKeplerianPeriod() / 3, threshold, Action.STOP);
    }

    /**
     * Build a new instance.
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#STOP stop} propagation at node crossing.
     * </p>
     * 
     * @param frameIn frame in which the equator is defined (typical values are
     *        {@link fr.cnes.sirius.patrius.frames.FramesFactory#getEME2000() J<sub>2000</sub>} or
     *        {@link fr.cnes.sirius.patrius.frames.FramesFactory#getITRF() ITRF 2005})
     * @param slopeSelection <br>
     *        {@link NodeDetector#ASCENDING} for ascending node detection,<br>
     *        {@link NodeDetector#DESCENDING} for descending node detection,<br>
     *        {@link NodeDetector#ASCENDING_DESCENDING} for both ascending and descending node
     *        detection.
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     */
    public NodeDetector(final Frame frameIn, final int slopeSelection, final double maxCheck,
        final double threshold) {
        this(frameIn, slopeSelection, maxCheck, threshold, Action.STOP);
    }

    /**
     * Build a new instance for both ascending and descending node detection.
     * 
     * @param frameIn frame in which the equator is defined (typical values are
     *        {@link fr.cnes.sirius.patrius.frames.FramesFactory#getEME2000() J<sub>2000</sub>} or
     *        {@link fr.cnes.sirius.patrius.frames.FramesFactory#getITRF() ITRF 2005})
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param ascendingNode action performed at ascending node crossing
     * @param descendingNode action performed at descending node crossing
     */
    public NodeDetector(final Frame frameIn, final double maxCheck, final double threshold,
        final Action ascendingNode, final Action descendingNode) {
        this(frameIn, maxCheck, threshold, ascendingNode, descendingNode, false, false);
    }

    /**
     * Build a new instance for both ascending and descending node detection.
     * 
     * @param frameIn frame in which the equator is defined (typical values are
     *        {@link fr.cnes.sirius.patrius.frames.FramesFactory#getEME2000() J<sub>2000</sub>} or
     *        {@link fr.cnes.sirius.patrius.frames.FramesFactory#getITRF() ITRF 2005})
     * @param maxCheckIn maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param ascendingNode action performed at ascending node crossing
     * @param descendingNode action performed at descending node crossing
     * @param removeAscendingNode true if detector should be removed at ascending node crossing
     * @param removeDescendingNode true if detector should be removed at descending node crossing
     * @since 3.1
     */
    public NodeDetector(final Frame frameIn, final double maxCheckIn, final double threshold,
        final Action ascendingNode, final Action descendingNode,
        final boolean removeAscendingNode, final boolean removeDescendingNode) {
        super(ASCENDING_DESCENDING, maxCheckIn, threshold);
        this.frame = frameIn;
        this.actionAscendingNote = ascendingNode;
        this.actionDescendingNode = descendingNode;
        this.removeAtAscendingNode = removeAscendingNode;
        this.removeAtDescendingNode = removeDescendingNode;
    }

    /**
     * Build a new instance.
     * 
     * @param frameIn frame in which the equator is defined (typical values are
     *        {@link fr.cnes.sirius.patrius.frames.FramesFactory#getEME2000() J<sub>2000</sub>} or
     *        {@link fr.cnes.sirius.patrius.frames.FramesFactory#getITRF() ITRF 2005})
     * @param slopeSelection <br>
     *        {@link NodeDetector#ASCENDING} for ascending node detection,<br>
     *        {@link NodeDetector#DESCENDING} for descending node detection,<br>
     *        {@link NodeDetector#ASCENDING_DESCENDING} for both ascending and descending node
     *        detection.
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param action action to be performed at node crossing
     */
    public NodeDetector(final Frame frameIn, final int slopeSelection, final double maxCheck,
        final double threshold, final Action action) {
        this(frameIn, slopeSelection, maxCheck, threshold, action, false);
    }

    /**
     * Build a new instance.
     * 
     * @param frameIn frame in which the equator is defined (typical values are
     *        {@link fr.cnes.sirius.patrius.frames.FramesFactory#getEME2000() J<sub>2000</sub>} or
     *        {@link fr.cnes.sirius.patrius.frames.FramesFactory#getITRF() ITRF 2005})
     * @param slopeSelection <br>
     *        {@link NodeDetector#ASCENDING} for ascending node detection,<br>
     *        {@link NodeDetector#DESCENDING} for descending node detection,<br>
     *        {@link NodeDetector#ASCENDING_DESCENDING} for both ascending and descending node
     *        detection.
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param action action to be performed at node crossing
     * @param removeCrossingNode true if detector should be removed at node crossing
     * 
     * @since 3.1
     */
    public NodeDetector(final Frame frameIn, final int slopeSelection, final double maxCheck,
        final double threshold, final Action action, final boolean removeCrossingNode) {
        super(slopeSelection, maxCheck, threshold);
        this.frame = frameIn;
        this.shouldBeRemovedFlag = removeCrossingNode;
        // If slopeSelection is different from 0, 1 or 2, an error has already been raised is
        // superclass.
        if (slopeSelection == ASCENDING) {
            this.actionAscendingNote = action;
            this.actionDescendingNode = null;
            this.removeAtAscendingNode = removeCrossingNode;
            this.removeAtDescendingNode = false;
        } else if (slopeSelection == DESCENDING) {
            this.actionAscendingNote = null;
            this.actionDescendingNode = action;
            this.removeAtAscendingNode = false;
            this.removeAtDescendingNode = removeCrossingNode;
        } else {
            // detection at ascending and descending node
            this.actionAscendingNote = action;
            this.actionDescendingNode = action;
            this.removeAtAscendingNode = removeCrossingNode;
            this.removeAtDescendingNode = removeCrossingNode;
        }
    }

    /**
     * Get the frame in which the equator is defined.
     * 
     * @return the frame in which the equator is defined
     */
    public Frame getFrame() {
        return this.frame;
    }

    /**
     * Returns action at ascending node detection.
     * 
     * @return action at ascending node detection
     */
    public Action getActionAtAscending() {
        return this.actionAscendingNote;
    }

    /**
     * Returns action at descending node detection.
     * 
     * @return action at descending node detection
     */
    public Action getActionAtDescending() {
        return this.actionDescendingNode;
    }

    /**
     * Returns true if detection is remove after ascending node detection.
     * 
     * @return true if detection is remove after ascending node detection
     */
    public boolean removeAtAscending() {
        return this.removeAtAscendingNode;
    }

    /**
     * Returns true if detection is remove after descending node detection.
     * 
     * @return true if detection is remove after descending node detection
     */
    public boolean removeAtDescending() {
        return this.removeAtDescendingNode;
    }

    /**
     * Handle a node crossing event and choose what to do next.
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#STOP stop} propagation at node crossing.
     * </p>
     * 
     * @param s the current state information : date, kinematics, attitude
     * @param increasing if true, the value of the switching function increases when times increases
     *        around event
     * @param forward if true, the integration variable (time) increases during integration.
     * @return the action performed when ascending or/and descending node is reached.
     * @exception PatriusException if some specific error occurs
     */
    @Override
    public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                final boolean forward) throws PatriusException {
        final Action result;
        if (this.getSlopeSelection() == ASCENDING) {
            result = this.actionAscendingNote;
            this.shouldBeRemovedFlag = this.removeAtAscendingNode;
        } else if (this.getSlopeSelection() == DESCENDING) {
            result = this.actionDescendingNode;
            this.shouldBeRemovedFlag = this.removeAtDescendingNode;
        } else {
            if (forward ^ !increasing) {
                // ascending node case
                result = this.actionAscendingNote;
                this.shouldBeRemovedFlag = this.removeAtAscendingNode;
            } else {
                // descending node case
                result = this.actionDescendingNode;
                this.shouldBeRemovedFlag = this.removeAtDescendingNode;
            }
        }
        // Return result
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public boolean shouldBeRemoved() {
        return this.shouldBeRemovedFlag;
    }

    /**
     * Compute the value of the switching function. This function computes the Z position in the
     * defined frame.
     * 
     * @param s the current state information: date, kinematics, attitude
     * @return value of the switching function
     * @exception PatriusException if some specific error occurs
     */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public double g(final SpacecraftState s) throws PatriusException {
        return s.getPVCoordinates(this.frame).getPosition().getZ();
    }

    /**
     * {@inheritDoc}
     * <p>
     * The following attributes are not deeply copied:
     * <ul>
     * <li>frame: {@link Frame}</li>
     * </ul>
     * </p>
     */
    @Override
    public EventDetector copy() {
        final EventDetector detector;
        if (this.getSlopeSelection() == ASCENDING) {
            detector = new NodeDetector(this.frame, this.getSlopeSelection(), this.getMaxCheckInterval(),
                this.getThreshold(), this.actionAscendingNote, this.removeAtAscendingNode);
        } else if (this.getSlopeSelection() == DESCENDING) {
            detector = new NodeDetector(this.frame, this.getSlopeSelection(), this.getMaxCheckInterval(),
                this.getThreshold(), this.actionDescendingNode, this.removeAtDescendingNode);
        } else {
            detector = new NodeDetector(this.frame, this.getMaxCheckInterval(), this.getThreshold(),
                this.actionAscendingNote, this.actionDescendingNode, this.removeAtAscendingNode,
                this.removeAtDescendingNode);
        }
        return detector;
    }
}
