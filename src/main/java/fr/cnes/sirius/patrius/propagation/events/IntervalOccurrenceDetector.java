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
 * @history 21/05/2018
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:FA:FA-2338:27/05/2020:Correction dans IntervalOccurenceDetector 
 * VERSION:4.4:DM:DM-2210:04/10/2019:[PATRIUS] Ameliorations de IntervalOccurenceDetector
 * VERSION:4.3:DM:DM-2089:15/05/2019:[PATRIUS] passage a Java 8
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * This event detector detects the occurrences "i" of an underlying event detector matching the 2 following condition:
 * <ul>
 * <li>i belong to [n, m]</li>
 * <li>i - n % p = 0</li>
 * </ul>
 * n being the first occurrence to detect, m the last occurrence to detect and p the occurrence step of detection. For
 * example, this detector will detect all the occurrence of an event between the 4th and 10th occurrence with a step of
 * 2 which means 4th, 6th, 8th and 10th occurrences will be detected.
 * </p>
 * However the {@link #eventOccurred(SpacecraftState, boolean, boolean)} method is triggered at
 * every event of the underlying detector. As a result, the behaviour of this detector is the
 * following:
 * <ul>
 * <li>At any occurrence not matching above conditions, the {@link #eventOccurred(SpacecraftState, boolean, boolean)}
 * returns Action.CONTINUE.</li>
 * <li>At the ith occurrence matching the above conditions, the
 * {@link #eventOccurred(SpacecraftState, boolean, boolean)} returns the user-provided action.</li>
 * </ul>
 * </p>
 * 
 * <p>
 * <b>Warning: </b> the {@link #eventOccurred(SpacecraftState, boolean, boolean)} method is triggered at every
 * occurrence of the underlying detector, not only at matching occurrences. Hence, overloading this detector should be
 * performed carefully: in the overloaded eventOccurred() method, the method {@link #getCurrentOccurrence()} should be
 * used to ensure we are at the right occurrence before calling super.eventOccurred(). This detector also provides the
 * method {@link #processEventOccurrence(SpacecraftState, boolean, boolean)} which is triggered at every occurrence.
 * Hence, when overloading this detector, you can overload the
 * {@link #processEventOccurrence(SpacecraftState, boolean, boolean)} method to get a listener on every occurrence.
 * </p>
 * 
 * @concurrency not thread-safe
 * @concurrency.comment use of internal mutable attributes
 * 
 * @author Emmanuel Bignon
 * 
 * @version $Id: NthOccurrenceDetector.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 4.1
 * 
 */
public class IntervalOccurrenceDetector implements EventDetector {

    /** Serializable UID. */
    private static final long serialVersionUID = -5081786749694651265L;

    /** Event */
    private final EventDetector event;
    /** 1st occurrence to detect. */
    private final int firstOccurrence;
    /** Last occurrence to detect. */
    private final int lastOccurrence;
    /** Step of occurrence to detect. */
    private final int step;
    /** Behavior upon detection. */
    private final Action action;

    /** Number of detected occurrences. */
    private int n;
    /** True if detector should be removed after detection. */
    private final boolean removeAtOcc;
    /**
     * Internal variable to indicate if event has been detected and detector should be removed after
     * detection.
     */
    private boolean shouldBeRemovedFlag = false;

    /**
     * Constructor.
     * 
     * @param eventToDetect event to detect
     * @param firstOccurrenceIn first occurrence of the event to detect
     * @param lastOccurrenceIn last occurrence of the event to detect
     * @param stepIn step between occurrences of the event to detect
     * @param actionAtOccurrence action at event nth occurrence
     */
    public IntervalOccurrenceDetector(final EventDetector eventToDetect,
        final int firstOccurrenceIn, final int lastOccurrenceIn, final int stepIn,
        final Action actionAtOccurrence) {
        this(eventToDetect, firstOccurrenceIn, lastOccurrenceIn, stepIn, actionAtOccurrence, false);
    }

    /**
     * Constructor.
     * 
     * @param eventToDetect event to detect
     * @param firstOccurrenceIn first occurrence of the event to detect
     * @param lastOccurrenceIn last occurrence of the event to detect
     * @param stepIn step between occurrences of the event to detect
     * @param actionAtOccurrence action at event nth occurrence
     * @param remove true if detector should be removed after detection of nth occurrence
     */
    public IntervalOccurrenceDetector(final EventDetector eventToDetect,
        final int firstOccurrenceIn, final int lastOccurrenceIn, final int stepIn,
        final Action actionAtOccurrence, final boolean remove) {
        this.event = eventToDetect;
        this.firstOccurrence = firstOccurrenceIn;
        this.lastOccurrence = lastOccurrenceIn;
        this.step = stepIn;
        this.action = actionAtOccurrence;
        this.removeAtOcc = remove;
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        this.n = 0;
        this.shouldBeRemovedFlag = false;
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public double g(final SpacecraftState s) throws PatriusException {
        return this.event.g(s);
    }

    /** {@inheritDoc} */
    @Override
    public boolean shouldBeRemoved() {
        return this.shouldBeRemovedFlag;
    }

    /** {@inheritDoc} */
    @Override
    public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                final boolean forward) throws PatriusException {

        final Action result;
        if (isEventConsidered(increasing)) {
            // Process the event occurrence
            processEventOccurrence(s, increasing, forward);

            // Check if the last occurrence to be considered has been reached
            if (n == getLastOccurrence()) {
                // Update flag of whether detector should be removed after detection
                shouldBeRemovedFlag = isRemoveAtOccurrence();
            }
            // Get action to be performed when event occurrence is found
            result = getActionAtOccurrence();
        } else {
            result = Action.CONTINUE;
        }

        return result;
    }

    /**
     * Checks if the event occurrence is to be considered:
     * <ol>
     * <li>The event occurs in the direction of the desired slope of the g-function</li>
     * <li>The event occurs between the specified first and last occurrence</li>
     * <li>The event occurs in the correct step between occurrences</li>
     * </ol>
     *
     * @param increasing
     *        if <code>true</code>, the value of the switching function increases when times increases around event
     *        (note that increase
     *        is measured with respect to physical time, not with respect to propagation which may go backward in time)
     * @return <code>true</code> if the event is to be considered
     */
    private boolean isEventConsidered(final boolean increasing) {

        // Check slope is in the desired direction
        final boolean rightSlope = (increasing && (getEvent().getSlopeSelection() != EventDetector.DECREASING))
                || (!increasing && (getEvent().getSlopeSelection() != EventDetector.INCREASING));

        boolean rightOccurrence = false;
        if (rightSlope) {
            // Increment occurrence counter
            n++;

            // Check occurrence is between the first and last occurrences to be considered and it's in the correct step
            rightOccurrence = (n >= getFirstOccurrence()) && (n <= getLastOccurrence())
                    && (((n - getFirstOccurrence()) % getStep()) == 0);
        }

        return rightOccurrence;
    }

    /**
     * Process the event occurrence. This method should be overriden by children classes.
     * @param s state at event
     * @param increasing
     *        if <code>true</code>, the value of the switching function increases when times increases around event
     *        (note that increase is measured with respect to physical time, not with respect to propagation which
     *        may go backward in time)
     * @param forward if <code>true</code>, the integration variable (time) increases during integration
     */
    protected void processEventOccurrence(final SpacecraftState s, final boolean increasing, final boolean forward) {
        // Do nothing for IntervalOccurrenceDetector. Should be overriden by sub-classes
    }

    /** {@inheritDoc} */
    @Override
    public SpacecraftState resetState(final SpacecraftState oldState) throws PatriusException {
        return oldState;
    }

    /** {@inheritDoc} */
    @Override
    public double getThreshold() {
        return this.event.getThreshold();
    }

    /** {@inheritDoc} */
    @Override
    public double getMaxCheckInterval() {
        return this.event.getMaxCheckInterval();
    }

    /** {@inheritDoc} */
    @Override
    public int getMaxIterationCount() {
        return this.event.getMaxIterationCount();
    }

    /** {@inheritDoc} */
    @Override
    public int getSlopeSelection() {
        return this.event.getSlopeSelection();
    }

    /**
     * Get event to detect.
     * 
     * @return event to detect.
     */
    public EventDetector getEvent() {
        return this.event;
    }

    /**
     * Get the action at occurrence.
     * 
     * @return action at occurrence
     */
    public Action getActionAtOccurrence() {
        return this.action;
    }

    /**
     * Returns true if detection is remove at occurrence.
     * 
     * @return remove at occurrence
     */
    public boolean isRemoveAtOccurrence() {
        return this.removeAtOcc;
    }

    /**
     * Get the first occurrence to detect.
     * 
     * @return first occurrence to detect
     */
    public int getFirstOccurrence() {
        return this.firstOccurrence;
    }

    /**
     * Get the last occurrence to detect.
     * 
     * @return last occurrence to detect
     */
    public int getLastOccurrence() {
        return this.lastOccurrence;
    }

    /**
     * Get the detection step.
     * 
     * @return detection step
     */
    public int getStep() {
        return this.step;
    }

    /**
     * Get the current occurrence (during propagation).
     * 
     * @return current occurrence
     */
    public int getCurrentOccurrence() {
        return this.n;
    }

    /** {@inheritDoc} */
    @Override
    public EventDetector copy() {
        return new IntervalOccurrenceDetector(this.event, this.firstOccurrence, this.lastOccurrence, this.step,
            this.action,
            this.removeAtOcc);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return String.format(
            "%s: event=%s, firstOccurrence=%s, step=%s, lastOccurrence=%s, action=%s",
            this.getClass().getSimpleName(), this.event.toString(), this.firstOccurrence, this.step,
            this.lastOccurrence, this.action);
    }
}
