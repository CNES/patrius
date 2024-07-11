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
 * @history created 10/09/2012
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:699:26/01/2017:Remove overlapping check for maneuvers defined by generic events
 * VERSION::DM:570:05/04/2017:add PropulsiveProperty and TankProperty
 * VERSION::DM:1173:26/06/2017:add propulsive and tank properties
 * VERSION::DM:976:16/11/2017:Merge continuous maneuvers classes
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.maneuvers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.math.interval.IntervalEndpointType;
import fr.cnes.sirius.patrius.propagation.events.DateDetector;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;

/**
 * This class handles a sequence of different maneuvers.<br>
 * The maneuvers can be continue maneuvers (with constant or variable thrust), or impulse maneuvers.
 * <p>
 * Maneuvers defined with dates cannot overlap. Maneuvers defined by generic events can overlap.
 * </p>
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment The instances are mutable.
 * 
 * @see ContinuousThrustManeuver
 * @see ImpulseManeuver
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public class ManeuversSequence {

    /**
     * List containing all the continue maneuvers (with constant and variable thrust). A HashSet has
     * been used because the order of its elements is not important.
     */
    private final Set<ContinuousThrustManeuver> continueManeuvers = new HashSet<>();

    /**
     * List containing all the impulse maneuvers. A HashSet has been used because the order of its
     * elements is not important.
     */
    private final Set<ImpulseManeuver> impulseManeuvers = new HashSet<>();

    /**
     * Lists containing time intervals. These intervals correspond to the time interval of each
     * maneuver plus the minimum allowed time between two consecutive maneuvers; they are used when
     * adding a new maneuver in the sequence to check if it is valid.
     */
    private final Set<AbsoluteDateInterval> maneuverIntervals = new HashSet<>();

    /**
     * The minimum allowed time between a continue maneuver and the next maneuver.
     */
    private final double continueDt;

    /**
     * The minimum allowed time between an impulse maneuver and the next maneuver.
     */
    private final double impulseDt;

    /**
     * Constructor of the maneuvers sequence.
     * 
     * @param dTcontinue the minimum allowed time between a continue maneuver and the next maneuver.
     * @param dTimpulse the minimum allowed time between an impulse maneuver and the next maneuver.
     */
    public ManeuversSequence(final double dTcontinue, final double dTimpulse) {
        this.continueDt = dTcontinue;
        this.impulseDt = dTimpulse;
    }

    /**
     * Adds a continuous maneuver to the list.<br>
     * Two scenarios are possible :
     * <ul>
     * <li>the maneuver to add is triggered by a detector other than {@link DateDetector}, always add the maneuver; it
     * means some overlapping may occur.</li>
     * <li>the maneuver to add is triggered by a {@link DateDetector}, always check first if the time interval of the
     * maneuver respects the two following conditions:
     * <ul>
     * <li>no superposition with any maneuver in the list;</li>
     * <li>the time between the end of the previous maneuver and the start of the maneuver to add, as well as the time
     * between the end of the maneuver to add and the start of the next maneuver, must be bigger than the threshold
     * value.</li>
     * </ul>
     * </ul>
     * </ul>
     * 
     * @param maneuver the continue maneuver to add to the list
     * @return true if the maneuver has been added, false otherwise (the conditions have not been
     *         respected)
     */
    public final boolean add(final ContinuousThrustManeuver maneuver) {
        return this.add(maneuver, maneuver.getStartDate(), maneuver.getEndDate());
    }

    /**
     * Remove the selected continue maneuver from the list.<br>
     * If the maneuver is not in the list, it returns false.
     * 
     * @param maneuver the maneuver to remove from the list
     * @return true if the maneuver has been removed, false otherwise
     */
    public final boolean remove(final ContinuousThrustManeuver maneuver) {
        return this.remove(maneuver, maneuver.getStartDate(), maneuver.getEndDate());
    }

    /**
     * Adds an impulse maneuver to the list.<br>
     * Two scenarios are possible:
     * <ul>
     * <li>the maneuver to add is triggered by a detector other than {@link DateDetector}, always add the maneuver; it
     * means some overlapping may occur.</li>
     * <li>the maneuver to add is triggered by a {@link DateDetector}, check if the date of the maneuver respects the
     * two following conditions:
     * <ul>
     * <li>no superposition with any maneuver in the list;</li>
     * <li>the time between the end of the previous maneuver and the date of the maneuver to add, as well as the time
     * between the end of the maneuver to add and the date of the next maneuver, must be bigger than the threshold
     * value.</li>
     * </ul>
     * </ul>
     * 
     * @param maneuver the impulse maneuver to add to the list
     * @return true if the maneuver has been added, false otherwise (the conditions have not been
     *         respected)
     */
    public final boolean add(final ImpulseManeuver maneuver) {
        if (maneuver.getTrigger().getClass().equals(DateDetector.class)) {
            // the triggering event detector is a date detector: check the conditions
            final AbsoluteDate maneuverDate = ((DateDetector) maneuver.getTrigger()).getDate();
            // creates a time interval containing just one date in order to call the checking
            // method:
            final AbsoluteDateInterval interval = new AbsoluteDateInterval(
                IntervalEndpointType.CLOSED, maneuverDate, maneuverDate.shiftedBy(this.impulseDt),
                IntervalEndpointType.CLOSED);
            if (!this.checkManeuverInterval(interval)) {
                return false;
            }
            this.maneuverIntervals.add(interval);
        }
        this.impulseManeuvers.add(maneuver);
        return true;
    }

    /**
     * Remove the selected impulse maneuver from the list.<br>
     * If the maneuver is not in the list, it returns false.
     * 
     * @param maneuver the maneuver to remove from the list
     * @return true if the maneuver has been removed, false otherwise
     */
    public final boolean remove(final ImpulseManeuver maneuver) {
        if (this.impulseManeuvers.contains(maneuver)) {
            this.impulseManeuvers.remove(maneuver);
            if (maneuver.getTrigger().getClass().equals(DateDetector.class)) {
                // remove from the list the time interval associated to the maneuver:
                final AbsoluteDate maneuverDate = ((DateDetector) maneuver.getTrigger()).getDate();
                final AbsoluteDate intervalStart = maneuverDate;
                final AbsoluteDate intervalEnd = maneuverDate.shiftedBy(this.impulseDt);
                final AbsoluteDateInterval interval = new AbsoluteDateInterval(
                    IntervalEndpointType.CLOSED, intervalStart, intervalEnd,
                    IntervalEndpointType.CLOSED);
                this.maneuverIntervals.remove(interval);
            }
            return true;
        }
        return false;
    }

    /**
     * @return the number of maneuvers in the list.
     */
    public final int getSize() {
        return this.continueManeuvers.size() + this.impulseManeuvers.size();
    }

    /**
     * Adds all the maneuvers (continue and impulse) to the propagator.
     * 
     * @param propagator the propagator to which all the maneuvers must be applied
     */
    public final void applyTo(final NumericalPropagator propagator) {
        // adds all the continue maneuvers:
        final Iterator<ContinuousThrustManeuver> forces = this.continueManeuvers.iterator();
        while (forces.hasNext()) {
            propagator.addForceModel(forces.next());
        }
        // adds all the impulse maneuvers:
        final Iterator<ImpulseManeuver> detectors = this.impulseManeuvers.iterator();
        while (detectors.hasNext()) {
            propagator.addEventDetector(detectors.next());
        }
    }

    /**
     * Private method that adds a continue maneuver (an instance of the ForceModel class) to the
     * sequence.
     * 
     * @param maneuver the continue maneuver to add
     * @param start the starting date of the maneuver
     * @param end the ending date of the maneuver
     * @return true if the maneuver has been added, false otherwise (the conditions have not been
     *         respected)
     */
    private boolean add(final ContinuousThrustManeuver maneuver, final AbsoluteDate start,
                        final AbsoluteDate end) {

        // Case of maneuvers defined by generic events (start and end dates are unknown)
        // Maneuver is added, interval is unknown, hence user may assume some maneuvers overlapping
        if (start == null || end == null) {
            this.continueManeuvers.add(maneuver);
            return true;
        }

        // gets the maneuver starting date:
        final AbsoluteDate intervalStart = start;
        // gets the maneuver ending date + the least time between two maneuvers:
        final AbsoluteDate intervalEnd = end.shiftedBy(this.continueDt);
        // creates the validity time interval of the maneuver:
        final AbsoluteDateInterval interval = new AbsoluteDateInterval(IntervalEndpointType.CLOSED,
            intervalStart, intervalEnd, IntervalEndpointType.CLOSED);
        boolean res = false;
        if (this.checkManeuverInterval(interval)) {
            this.maneuverIntervals.add(interval);
            this.continueManeuvers.add(maneuver);
            res = true;
        }
        return res;
    }

    /**
     * Private method that removes a continue maneuver (an instance of the ForceModel class) to the
     * sequence.
     * 
     * @param maneuver the continue maneuver to remove
     * @param start the starting date of the maneuver
     * @param end the ending date of the maneuver
     * @return true if the maneuver has been removed, false otherwise (the conditions have not been
     *         respected)
     */
    private boolean remove(final ForceModel maneuver, final AbsoluteDate start,
                           final AbsoluteDate end) {
        if (this.continueManeuvers.contains(maneuver)) {
            this.continueManeuvers.remove(maneuver);
            // remove from the list the time interval associated to the maneuver:
            final AbsoluteDate intervalStart = start;
            final AbsoluteDate intervalEnd = end.shiftedBy(this.continueDt);
            final AbsoluteDateInterval interval = new AbsoluteDateInterval(
                IntervalEndpointType.CLOSED, intervalStart, intervalEnd,
                IntervalEndpointType.CLOSED);
            this.maneuverIntervals.remove(interval);
            return true;
        }
        return false;
    }

    /**
     * Check that the time interval does not overlap any time interval in the list.
     * 
     * @param interval the time interval of the new maneuver
     * @return true if the interval does not overlap any maneuver in the list, false otherwise
     */
    private boolean checkManeuverInterval(final AbsoluteDateInterval interval) {
        final Iterator<AbsoluteDateInterval> iterator = this.maneuverIntervals.iterator();
        while (iterator.hasNext()) {
            final boolean isIntervalOverlapping = iterator.next().overlaps(interval);
            if (isIntervalOverlapping) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get the continue maneuvers list.
     * 
     * @return the custom maneuvers list
     */
    public Set<ContinuousThrustManeuver> getContinueManeuversList() {
        return this.continueManeuvers;
    }

    /**
     * Get the continue maneuvers list.
     * 
     * @return the custom maneuvers list
     */
    public Set<ImpulseManeuver> getImpulseManeuversList() {
        return this.impulseManeuvers;
    }

    /**
     * Get the custom maneuvers list.
     * 
     * @return the custom maneuvers list
     */
    public List<Maneuver> getManeuversList() {
        final List<Maneuver> res = new ArrayList<>();
        res.addAll(this.getImpulseManeuversList());
        res.addAll(this.getContinueManeuversList());
        return res;
    }

    /**
     * Get the time constraint for impulsive maneuvers.
     * 
     * @return the time constraint
     */
    public double getConstraintImpulsive() {
        return this.impulseDt;
    }

    /**
     * Get the time constraint for continuous maneuvers.
     * 
     * @return the time constraint
     */
    public double getConstraintContinuous() {
        return this.continueDt;
    }

}
