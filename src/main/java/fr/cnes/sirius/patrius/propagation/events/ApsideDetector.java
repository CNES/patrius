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
* VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
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

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Finder for apside crossing events.
 * <p>
 * This class finds apside crossing events (i.e. apogee and/or perigee crossing).
 * </p>
 * <p>
 * The default implementation behavior is to {@link EventDetector.Action#STOP stop} propagation at apogee or/and perigee
 * crossing depending on slope selection defined {@link ApsideDetector#PERIGEE}, {@link ApsideDetector#APOGEE} and
 * {@link ApsideDetector#PERIGEE_APOGEE}. This can be changed by overriding one of the following constructors :
 * </p>
 * <ul>
 * <li>
 * {@link #ApsideDetector(double, double, fr.cnes.sirius.patrius.propagation.events.EventDetector.Action, 
 * fr.cnes.sirius.patrius.propagation.events.EventDetector.Action)
 * ApsideDetector} : the defined action is performed at apogee OR/AND perigee depending on slope selection defined.
 * <li>
 * {@link #ApsideDetector(int, double, double, fr.cnes.sirius.patrius.propagation.events.EventDetector.Action)
 * ApsideDetector} : the defined actions are performed at apogee AND perigee.
 * </ul>
 * 
 * <p>
 * Beware that apside detection will fail for almost circular orbits.
 * </p>
 * 
 * @see fr.cnes.sirius.patrius.propagation.Propagator#addEventDetector(EventDetector)
 * @author Luc Maisonobe
 */
@SuppressWarnings("PMD.NullAssignment")
public class ApsideDetector extends AbstractDetector {

    /** Flag for perigee detection (slopeSelection = 0). */
    public static final int PERIGEE = 0;

    /** Flag for apogee detection (slopeSelection = 1). */
    public static final int APOGEE = 1;

    /** Flag for both perigee and apogee detection (slopeSelection = 2). */
    public static final int PERIGEE_APOGEE = 2;

    /** Default convergence threshold (in % of Keplerian period). */
    private static final double DEFAULT_THRESHOLD = 1.0e-13;

    /** Serializable UID. */
    private static final long serialVersionUID = -7542434866922384844L;

    /**
     * Build a new instance.
     * <p>
     * The orbit is used only to set an upper bound for the max check interval to period/3 and to set the convergence
     * threshold according to orbit size
     * </p>
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#STOP stop} propagation when the expected
     * apside is reached.
     * </p>
     * 
     * @param orbit initial orbit
     * @param slopeSelection <br>
     *        {@link ApsideDetector#PERIGEE} for perigee detection,<br>
     *        {@link ApsideDetector#APOGEE} for apogee detection,<br>
     *        {@link ApsideDetector#PERIGEE_APOGEE} for both perigee and apogee detection.
     */
    public ApsideDetector(final Orbit orbit, final int slopeSelection) {
        this(slopeSelection, orbit.getKeplerianPeriod() / 3, DEFAULT_THRESHOLD
            * orbit.getKeplerianPeriod());
    }

    /**
     * Build a new instance.
     * <p>
     * The orbit is used only to set an upper bound for the max check interval to period/3
     * </p>
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#STOP stop} propagation when the expected
     * apside is reached.
     * </p>
     * 
     * @param orbit initial orbit
     * @param slopeSelection <br>
     *        {@link ApsideDetector#PERIGEE} for perigee detection,<br>
     *        {@link ApsideDetector#APOGEE} for apogee detection,<br>
     *        {@link ApsideDetector#PERIGEE_APOGEE} for both perigee and apogee detection.
     * @param threshold convergence threshold (s)
     */
    public ApsideDetector(final Orbit orbit, final int slopeSelection, final double threshold) {
        this(slopeSelection, orbit.getKeplerianPeriod() / 3, threshold);
    }

    /**
     * Build a new instance.
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#STOP stop} propagation when the expected
     * apside is reached.
     * </p>
     * 
     * @param slopeSelection <br>
     *        {@link ApsideDetector#PERIGEE} for perigee detection,<br>
     *        {@link ApsideDetector#APOGEE} for apogee detection,<br>
     *        {@link ApsideDetector#PERIGEE_APOGEE} for both perigee and apogee detection.
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     */
    public ApsideDetector(final int slopeSelection, final double maxCheck, final double threshold) {
        this(slopeSelection, maxCheck, threshold, Action.STOP);
    }

    /**
     * Build a new instance with both apogee and perigee detection.
     * 
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param actionApogee action to be perform when the apogee is reached
     * @param actionPerigee action to be perform when the perigee is reached
     */
    public ApsideDetector(final double maxCheck, final double threshold, final Action actionApogee,
        final Action actionPerigee) {
        this(maxCheck, threshold, actionApogee, actionPerigee, false, false);
    }

    /**
     * Build a new instance with both apogee and perigee detection.
     * 
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param actionApogee action to be perform when the apogee is reached
     * @param actionPerigee action to be perform when the perigee is reached
     * @param removeApogee true if detector should be removed at apogee
     * @param removePerigee true if detector should be removed at perigee
     * @since 3.1
     */
    public ApsideDetector(final double maxCheck, final double threshold, final Action actionApogee,
        final Action actionPerigee, final boolean removeApogee, final boolean removePerigee) {
        super(PERIGEE_APOGEE, maxCheck, threshold, actionApogee, actionPerigee, removeApogee, removePerigee);
    }

    /**
     * Build a new instance with apogee OR/AND perigee detection depending on slope selection.
     * 
     * @param slopeSelection <br>
     *        {@link ApsideDetector#PERIGEE} for perigee detection,<br>
     *        {@link ApsideDetector#APOGEE} for apogee detection,<br>
     *        {@link ApsideDetector#PERIGEE_APOGEE} for both perigee and apogee detection.
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param action action to be performed when the expected apside is reached
     */
    public ApsideDetector(final int slopeSelection, final double maxCheck, final double threshold,
        final Action action) {
        this(slopeSelection, maxCheck, threshold, action, false);
    }

    /**
     * Build a new instance with apogee OR/AND perigee detection depending on slope selection.
     * 
     * @param slopeSelection <br>
     *        {@link ApsideDetector#PERIGEE} for perigee detection,<br>
     *        {@link ApsideDetector#APOGEE} for apogee detection,<br>
     *        {@link ApsideDetector#PERIGEE_APOGEE} for both perigee and apogee detection.
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param action action to be performed when the expected apside is reached
     * @param remove true if detector should be removed
     */
    public ApsideDetector(final int slopeSelection, final double maxCheck, final double threshold,
        final Action action, final boolean remove) {
        super(slopeSelection, maxCheck, threshold, action, remove);
        this.shouldBeRemovedFlag = remove;
    }

    /**
     * <p>
     * Handle an apside crossing event and choose what to do next.
     * </p>
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#STOP stop} propagation when the expected
     * apside is reached.
     * </p>
     * 
     * @param s the current state information : date, kinematics, attitude
     * @param increasing if true, the value of the switching function increases when times increases
     *        around event
     * @param forward if true, the integration variable (time) increases during integration.
     * @return the action performed when the expected apside is reached.
     * @exception PatriusException if some specific error occurs
     */
    @Override
    public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                final boolean forward) throws PatriusException {
        final Action result;
        if (this.getSlopeSelection() == 0) {
            result = this.getActionAtExit();
            this.shouldBeRemovedFlag = this.isRemoveAtExit();
        } else if (this.getSlopeSelection() == 1) {
            result = this.getActionAtEntry();
            this.shouldBeRemovedFlag = this.isRemoveAtEntry();
        } else {
            if (forward ^ !increasing) {
                // perigee case
                result = this.getActionAtExit();
                this.shouldBeRemovedFlag = this.isRemoveAtExit();
            } else {
                // apogee case
                result = this.getActionAtEntry();
                this.shouldBeRemovedFlag = this.isRemoveAtEntry();
            }
        }
        // Return result
        return result;
    }

    /**
     * Compute the value of the switching function. This function computes the dot product of the 2
     * vectors : position.velocity.
     * 
     * @param state state
     * @return value of the switching function
     * @exception PatriusException if some specific error occurs
     */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public double g(final SpacecraftState state) throws PatriusException {
        final PVCoordinates pv = state.getPVCoordinates();
        return Vector3D.dotProduct(pv.getPosition(), pv.getVelocity());
    }

    /** {@inheritDoc} */
    @Override
    public EventDetector copy() {
        final EventDetector result;
        if (this.getSlopeSelection() == PERIGEE) {
            result = new ApsideDetector(this.getSlopeSelection(), this.getMaxCheckInterval(), this.getThreshold(),
                this.getActionAtExit(), this.isRemoveAtExit());
        } else if (this.getSlopeSelection() == APOGEE) {
            result = new ApsideDetector(this.getSlopeSelection(), this.getMaxCheckInterval(), this.getThreshold(),
                this.getActionAtEntry(), this.isRemoveAtEntry());
        } else {
            result = new ApsideDetector(this.getMaxCheckInterval(), this.getThreshold(), this.getActionAtEntry(),
                this.getActionAtExit(), this.isRemoveAtEntry(), this.isRemoveAtExit());
        }
        return result;
    }
}
