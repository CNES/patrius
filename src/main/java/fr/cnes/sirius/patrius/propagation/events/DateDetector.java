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
 * VERSION::DM:394:30/03/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:454:24/11/2015:Add constructors, overload method shouldBeRemoved()
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeStamped;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Finder for date events.
 * <p>
 * This class finds date events (i.e. occurrence of some predefined dates).
 * </p>
 * <p>
 * As of version 5.1, it is an enhanced date detector:
 * </p>
 * <ul>
 * <li>it can be defined without prior date ({@link #DateDetector(double,double)})</li>
 * <li>several dates can be added ({@link #addEventDate(AbsoluteDate)})</li>
 * </ul>
 * <p>
 * The gap between the added dates must be more than the maxCheck.
 * </p>
 * <p>
 * The default implementation behavior is to {@link EventDetector.Action#STOP stop} propagation at the first event date
 * occurrence. This can be changed by using provided constructors.
 * </p>
 * 
 * @see fr.cnes.sirius.patrius.propagation.Propagator#addEventDetector(EventDetector)
 * @author Luc Maisonobe
 * @author Pascal Parraud
 */
@SuppressWarnings({"PMD.NullAssignment", "PMD.ConstructorCallsOverridableMethod"})
public class DateDetector extends AbstractDetector implements TimeStamped {

    /** Default convergence threshold (s). */
    public static final double DEFAULT_THRESHOLD = 10.e-10;

    /** Default convergence threshold (s). */
    public static final double DEFAULT_MAXCHECK = 10.e9;

    /** Serializable UID. */
    private static final long serialVersionUID = -334171965326514174L;

    /** Last date for g computation. */
    private AbsoluteDate gDate;

    /** List of event dates. */
    private final List<EventDate> eventDateList;

    /** Current event date. */
    private int currentIndex;

    /** Action performed. */
    private Action actionDate;

    /**
     * Build a new instance.
     * <p>
     * This constructor is dedicated to date detection when the event date is not known before propagating. It can be
     * triggered later by adding some event date, it then acts like a timer.
     * </p>
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#STOP stop} propagation at date occurrence.
     * </p>
     * 
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @see #addEventDate(AbsoluteDate)
     */
    public DateDetector(final double maxCheck, final double threshold) {
        this(maxCheck, threshold, Action.STOP);
    }

    /**
     * Build a new instance.
     * <p>
     * This constructor is dedicated to date detection when the event date is not known before propagating. It can be
     * triggered later by adding some event date, it then acts like a timer.
     * </p>
     * 
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @param action action performed at date detection
     * @see #addEventDate(AbsoluteDate)
     */
    public DateDetector(final double maxCheck, final double threshold, final Action action) {
        this(maxCheck, threshold, action, false);
    }

    /**
     * Build a new instance.
     * <p>
     * This constructor is dedicated to date detection when the event date is not known before propagating. It can be
     * triggered later by adding some event date, it then acts like a timer.
     * </p>
     * 
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @param action action performed at date detection
     * @param remove true if detector should be removed after first detection
     * @see #addEventDate(AbsoluteDate)
     */
    public DateDetector(final double maxCheck, final double threshold, final Action action,
        final boolean remove) {
        super(maxCheck, threshold);
        this.eventDateList = new ArrayList<>();
        this.currentIndex = -1;
        this.gDate = null;
        // action
        this.actionDate = action;
        this.shouldBeRemovedFlag = remove;
    }

    /**
     * Build a new instance.
     * <p>
     * First event date is set here, but others can be added later with {@link #addEventDate(AbsoluteDate)}.
     * </p>
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#STOP stop} propagation at date occurrence.
     * </p>
     * 
     * @param target target date
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @see #addEventDate(AbsoluteDate)
     */
    public DateDetector(final AbsoluteDate target, final double maxCheck, final double threshold) {
        this(maxCheck, threshold, Action.STOP);
        this.addEventDate(target);
    }

    /**
     * Build a new instance.
     * <p>
     * First event date is set here, but others can be added later with {@link #addEventDate(AbsoluteDate)}.
     * </p>
     * 
     * @param target target date
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @param action action performed at date detection
     * @see #addEventDate(AbsoluteDate)
     */
    public DateDetector(final AbsoluteDate target, final double maxCheck, final double threshold,
        final Action action) {
        this(target, maxCheck, threshold, action, false);
    }

    /**
     * Build a new instance.
     * <p>
     * First event date is set here, but others can be added later with {@link #addEventDate(AbsoluteDate)}.
     * </p>
     * 
     * @param target target date
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @param action action performed at date detection
     * @param remove true if detector should be removed after first detection
     * @see #addEventDate(AbsoluteDate)
     * @since 3.1
     */
    public DateDetector(final AbsoluteDate target, final double maxCheck, final double threshold,
        final Action action, final boolean remove) {
        this(maxCheck, threshold);
        this.addEventDate(target);
        // action
        this.actionDate = action;
        // remove (or not) detector
        this.shouldBeRemovedFlag = remove;
    }

    /**
     * Build a new instance.
     * <p>
     * This constructor is dedicated to single date detection. MaxCheck is set to 10.e9, so almost no other date can be
     * added. Tolerance is set to 10.e-10.
     * </p>
     * 
     * @param target target date
     * @see #addEventDate(AbsoluteDate)
     */
    public DateDetector(final AbsoluteDate target) {
        this(target, DEFAULT_MAXCHECK, DEFAULT_THRESHOLD);
    }

    /**
     * Handle a date event and choose what to do next.
     * 
     * @param s the current state information : date, kinematics, attitude
     * @param increasing the way of the switching function is not guaranted as it can change
     *        according to the added event dates
     * @param forward if true, the integration variable (time) increases during integration.
     * @return the action performed at the first event date occurrence.
     * @exception PatriusException if some specific error occurs
     */
    @Override
    public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                final boolean forward) throws PatriusException {
        return this.actionDate;
    }

    /**
     * Compute the value of the switching function. This function measures the difference between
     * the current and the target date.
     * 
     * @param state state
     * @return value of the switching function
     * @exception PatriusException if some specific error occurs
     */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public double g(final SpacecraftState state) throws PatriusException {
        this.gDate = state.getDate();
        if (this.currentIndex < 0) {
            return -1.0;
        }
        final EventDate event = this.getClosest(this.gDate);
        return event.isgIncrease() ? this.gDate.durationFrom(event.getDate()) : event.getDate()
            .durationFrom(this.gDate);
    }

    /**
     * Get the current event date according to the propagator.
     * 
     * @return event date
     */
    @Override
    public AbsoluteDate getDate() {
        return this.currentIndex < 0 ? null : this.eventDateList.get(this.currentIndex).getDate();
    }

    /**
     * Return the action at detection.
     * 
     * @return action at detection
     */
    public Action getAction() {
        return this.actionDate;
    }

    /**
     * Add an event date.
     * <p>
     * The date to add must be:
     * </p>
     * <ul>
     * <li>less than the smallest already registered event date minus the maxCheck</li>
     * <li>or more than the largest already registered event date plus the maxCheck</li>
     * </ul>
     * 
     * @param target target date
     * @throws IllegalArgumentException if the date is too close from already defined interval
     * @see #DateDetector(double, double)
     */
    public void addEventDate(final AbsoluteDate target) {
        // Initialization
        final boolean increasing;

        if (this.currentIndex < 0) {
            // Simply add date
            increasing = (this.gDate == null) ? true : target.durationFrom(this.gDate) > 0.0;
            this.currentIndex = 0;
            this.eventDateList.add(new EventDate(target, increasing));
        } else {
            final int lastIndex = this.eventDateList.size() - 1;
            if (this.eventDateList.get(0).getDate().durationFrom(target) > this.getMaxCheckInterval()) {
                // Add date to the front of the list
                increasing = !this.eventDateList.get(0).isgIncrease();
                this.eventDateList.add(0, new EventDate(target, increasing));
                this.currentIndex++;
            } else if (target.durationFrom(this.eventDateList.get(lastIndex).getDate()) > this.getMaxCheckInterval()) {
                // Add date to the back of the list
                increasing = !this.eventDateList.get(lastIndex).isgIncrease();
                this.eventDateList.add(new EventDate(target, increasing));
            } else {
                // Exception
                throw PatriusException.createIllegalArgumentException(
                    PatriusMessages.EVENT_DATE_TOO_CLOSE, target, this.eventDateList.get(0)
                        .getDate(), this.eventDateList.get(lastIndex).getDate(),
                    this.getMaxCheckInterval());
            }
        }
    }

    /**
     * Get the closest EventDate to the target date.
     * 
     * @param target target date
     * @return current EventDate
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Orekit code kept as such
    private EventDate getClosest(final AbsoluteDate target) {
        // CHECKSTYLE: resume CyclomaticComplexity check

        // dt from current event date
        final double dt = target.durationFrom(this.eventDateList.get(this.currentIndex).getDate());

        if (dt < 0.0 && this.currentIndex > 0) {
            // current event date before current index date
            boolean found = false;
            while (this.currentIndex > 0 && !found) {
                if (target.durationFrom(this.eventDateList.get(this.currentIndex - 1).getDate()) < this.eventDateList
                    .get(this.currentIndex).getDate().durationFrom(target)) {
                    this.currentIndex--;
                } else {
                    found = true;
                }
            }
        } else if (dt > 0.0 && this.currentIndex < this.eventDateList.size() - 1) {
            // current event date after current index date
            final int maxIndex = this.eventDateList.size() - 1;
            boolean found = false;
            while (this.currentIndex < maxIndex && !found) {
                if (target.durationFrom(this.eventDateList.get(this.currentIndex + 1).getDate()) > this.eventDateList
                    .get(this.currentIndex).getDate().durationFrom(target)) {
                    this.currentIndex++;
                } else {
                    found = true;
                }
            }
        }
        // Return result
        return this.eventDateList.get(this.currentIndex);
    }

    /** {@inheritDoc} */
    @Override
    public EventDetector copy() {
        final DateDetector result;
        if (this.gDate == null) {
            result = new DateDetector(this.getMaxCheckInterval(), this.getThreshold(), this.actionDate,
                this.shouldBeRemovedFlag);
        } else {
            result = new DateDetector(new AbsoluteDate(this.gDate, 0.), this.getMaxCheckInterval(),
                this.getThreshold(), this.actionDate, this.shouldBeRemovedFlag);
        }
        for (int i = 0; i < this.eventDateList.size(); i++) {
            result.addEventDate(new AbsoluteDate(this.eventDateList.get(i).getDate(), 0.));
        }
        return result;
    }

    /** Event date specification. */
    private static class EventDate implements Serializable, TimeStamped {

        /** Serializable UID. */
        private static final long serialVersionUID = -7641032576122527149L;

        /** Event date. */
        private final AbsoluteDate date;

        /** Flag for g function way around event date. */
        private final boolean gIncrease;

        /**
         * Simple constructor.
         * 
         * @param date date
         * @param increase if true, g function increases around event date
         */
        public EventDate(final AbsoluteDate date, final boolean increase) {
            this.date = date;
            this.gIncrease = increase;
        }

        /**
         * Getter for event date.
         * 
         * @return event date
         */
        @Override
        public AbsoluteDate getDate() {
            return this.date;
        }

        /**
         * Getter for g function way at event date.
         * 
         * @return g function increasing flag
         */
        public boolean isgIncrease() {
            return this.gIncrease;
        }
    }
}
