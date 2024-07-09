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
* VERSION:4.7:DM:DM-2653:18/05/2021:generalisation des sequences et correction/refonte des sequences de segments 
 * VERSION:4.5:FA:FA-2341:27/05/2020:Bug lors de la recuperation de l'attitude issue d'une sequence d'attitude 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014:Modified maneuvers in retro-propagation case
 *                              (added forward parameter to eventOccurred signature)
 * VERSION::DM:226:12/09/2014: problem with event detections.
 * VERSION::FA:411:10/02/2015:javadoc
 * VERSION::DM:489:06/10/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::DM:403:20/10/2015:Improving ergonomic
 * VERSION::DM:454:24/11/2015:Overload method shouldBeRemoved()
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This classes manages a sequence of different attitude providers that are activated in turn
 * according to switching events.
 * <p>
 * Only one attitude provider in the sequence is in an active state. When one of the switch event associated with the
 * active provider occurs, the active provider becomes the one specified with the event. A simple example is a provider
 * for the sun lighted part of the orbit and another provider for the eclipse time. When the sun lighted provider is
 * active, the eclipse entry event is checked and when it occurs the eclipse provider is activated. When the eclipse
 * provider is active, the eclipse exit event is checked and when it occurs the sun lighted provider is activated again.
 * This sequence is a simple loop.
 * </p>
 * <p>
 * An active attitude provider may have several switch events and next provider settings, leading to different
 * activation patterns depending on which events are triggered first. An example of this feature is handling switches to
 * safe mode if some contingency condition is met, in addition to the nominal switches that correspond to proper
 * operations. Another example is handling of maneuver mode.
 * <p>
 * 
 * @author Luc Maisonobe
 * @since 5.1
 */
@SuppressWarnings("PMD.NullAssignment")
public class AttitudesSequence implements AttitudeLaw {

    /** Serializable UID. */
    private static final long serialVersionUID = 5140034224175180354L;

    /** Active provider. */
    private AttitudeLaw active;

    /** Switching events map. */
    private final Map<AttitudeLaw, List<Switch>> switchingMap;

    /** Validity intervals map <Law, date up to when the law is valid>. */
    private final Map<AttitudeLaw, AbsoluteDate> validityMap;

    /**
     * Constructor for an initially empty sequence.
     */
    public AttitudesSequence() {
        this.active = null;
        this.switchingMap = new LinkedHashMap<AttitudeLaw, List<Switch>>();
        this.validityMap = new LinkedHashMap<AttitudeLaw, AbsoluteDate>();
    }

    /**
     * Reset the active provider.
     * 
     * @param provider the provider to activate
     */
    public void resetActiveProvider(final AttitudeLaw provider) {

        // add the provider if not already known
        if (!this.switchingMap.containsKey(provider)) {
            this.switchingMap.put(provider, new ArrayList<Switch>());
        }

        this.active = provider;

    }

    /**
     * Register all wrapped switch events to the propagator.
     * <p>
     * This method must be called once before propagation, after the switching conditions have been set up by calls to
     * {@link #addSwitchingCondition(AttitudeLaw, EventDetector, boolean, boolean, AttitudeLaw)}.
     * </p>
     * 
     * @param propagator propagator that will handle the events
     */
    public void registerSwitchEvents(final Propagator propagator) {
        for (final Collection<Switch> collection : this.switchingMap.values()) {
            for (final Switch s : collection) {
                propagator.addEventDetector(s);
            }
        }
    }

    /**
     * Add a switching condition between two attitude providers.
     * <p>
     * An attitude provider may have several different switch events associated to it. Depending on which event is
     * triggered, the appropriate provider is switched to.
     * </p>
     * <p>
     * The switch events specified here must <em>not</em> be registered to the propagator directly. The proper way to
     * register these events is to call {@link #registerSwitchEvents(Propagator)} once after all switching conditions
     * have been set up. The reason for this is that the events will be wrapped before being registered.
     * </p>
     * 
     * @param before attitude provider before the switch event occurrence
     * @param switchEvent event triggering the attitude providers switch ; the event should generate
     *        ACTION.RESET_STATE when event occured. (may be null for a provider without any ending
     *        condition, in this case the after provider is not referenced and may be null too)
     * @param switchOnIncrease if true, switch is triggered on increasing event
     * @param switchOnDecrease if true, switch is triggered on decreasing event
     * @param after attitude provider to activate after the switch event occurrence (used only if
     *        switchEvent is non null)
     */
    public void addSwitchingCondition(final AttitudeLaw before, final EventDetector switchEvent,
                                      final boolean switchOnIncrease, final boolean switchOnDecrease,
                                      final AttitudeLaw after) {

        // add the before provider if not already known
        if (!this.switchingMap.containsKey(before)) {
            this.switchingMap.put(before, new ArrayList<Switch>());
            if (this.active == null) {
                this.active = before;
            }
        }

        if (switchEvent != null) {

            // add the after provider if not already known
            if (!this.switchingMap.containsKey(after)) {
                this.switchingMap.put(after, new ArrayList<Switch>());
            }

            // add the switching condition
            this.switchingMap.get(before).add(
                new Switch(switchEvent, switchOnIncrease, switchOnDecrease, after));

        }

    }

    /**
     * {@inheritDoc}
     * <p>
     * Spin derivatives computation does not apply to provided law. Call {@link #setSpinDerivativesComputation(boolean)}
     * on each law to activate/deactivate underlying law spin derivative computation.
     * </p>
     */
    @Override
    public void setSpinDerivativesComputation(final boolean computeSpinDerivatives) {
        // Nothing to do : the method of each attitude law must be called
    }

    /** {@inheritDoc} */
    @Override
    public Attitude getAttitude(final PVCoordinatesProvider pvProv, final AbsoluteDate date,
                                final Frame frame) throws PatriusException {
        // Default attitude law to use is active law
        AttitudeLaw attitudeLaw = this.active;

        // Check if date is part of a past attitude law
        // In this case use this law
        final Iterator<Entry<AttitudeLaw, AbsoluteDate>> iterator = validityMap.entrySet().iterator();
        if (iterator.hasNext()) {
            Entry<AttitudeLaw, AbsoluteDate> currentEntry = iterator.next();
            AbsoluteDate currentDate = currentEntry.getValue();
            while (currentDate.compareTo(date) <= 0) {
                if (iterator.hasNext()) {
                    currentEntry = iterator.next();
                    currentDate = currentEntry.getValue();
                    attitudeLaw = currentEntry.getKey();
                } else {
                    // No past law found: stay on active law
                    attitudeLaw = this.active;
                    break;
                }
            }
        }

        // Get attitude
        return attitudeLaw.getAttitude(pvProv, date, frame);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        // Initialisation
        final String colonSpace = ": ";
        final StringBuffer strBuffer = new StringBuffer();
        strBuffer.append(this.getClass().getSimpleName());
        strBuffer.append(":");
        int i = 0;
        // Loop on all attitude laws
        for (final Entry<AttitudeLaw, List<Switch>> entry : this.switchingMap.entrySet()) {
            final AttitudeLaw law = entry.getKey();
            // Law
            strBuffer.append("\n Law ");
            strBuffer.append(i);
            strBuffer.append(colonSpace);
            strBuffer.append(law.toString());
            // Switches
            if (!this.switchingMap.get(law).isEmpty()) {
                strBuffer.append("\n Switch ");
                strBuffer.append(colonSpace);
                for (final Switch sw : this.switchingMap.get(law)) {
                    strBuffer.append(sw.event.toString());
                }
            }
            i++;
        }

        // Return result
        return strBuffer.toString();
    }

    /** Switch specification. */
    private class Switch implements EventDetector {

        /** Serializable UID. */
        private static final long serialVersionUID = -668295773303559063L;

        /** Event. */
        private final EventDetector event;

        /** Event direction triggering the switch. */
        private final boolean switchOnIncrease;

        /** Event direction triggering the switch. */
        private final boolean switchOnDecrease;

        /** Direction of g function (stored between eventOccurred and resetState. */
        private boolean increasing;

        /** Next attitude provider. */
        private final AttitudeLaw next;

        /**
         * Simple constructor.
         * 
         * @param eventIn event
         * @param switchOnIncreaseIn if true, switch is triggered on increasing event
         * @param switchOnDecreaseIn if true, switch is triggered on decreasing event otherwise
         *        switch is triggered on decreasing event
         * @param nextIn next attitude provider
         */
        public Switch(final EventDetector eventIn, final boolean switchOnIncreaseIn,
            final boolean switchOnDecreaseIn, final AttitudeLaw nextIn) {
            this.event = eventIn;
            this.switchOnIncrease = switchOnIncreaseIn;
            this.switchOnDecrease = switchOnDecreaseIn;
            this.next = nextIn;
        }

        /** {@inheritDoc} */
        @Override
        public void init(final SpacecraftState s0, final AbsoluteDate t) {
            this.event.init(s0, t);
        }

        /** {@inheritDoc} */
        @Override
        public Action eventOccurred(final SpacecraftState s, final boolean increasingValue,
                                    final boolean forward) throws PatriusException {
            this.increasing = increasingValue;
            // No code implying computation modification should be included in eventOccurred method!
            return this.event.eventOccurred(s, this.increasing, forward);
        }

        /** {@inheritDoc} */
        @Override
        public boolean shouldBeRemoved() {
            return this.event.shouldBeRemoved();
        }

        /** {@inheritDoc} */
        @Override
        @SuppressWarnings("PMD.ShortMethodName")
        public double g(final SpacecraftState s) throws PatriusException {
            return this.event.g(s);
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
        public double getThreshold() {
            return this.event.getThreshold();
        }

        /** {@inheritDoc} */
        @Override
        public SpacecraftState resetState(final SpacecraftState oldState) throws PatriusException {

            if ((this.increasing && this.switchOnIncrease) || (!this.increasing && this.switchOnDecrease)) {
                // Store past attitude law and date up to when it was valid
                AttitudesSequence.this.validityMap.put(AttitudesSequence.this.active, oldState.getDate());

                // switch to next attitude provider
                AttitudesSequence.this.active = this.next;
            }
            return this.event.resetState(oldState);
        }

        /** {@inheritDoc} */
        @Override
        public int getSlopeSelection() {
            return 2;
        }

        /**
         * {@inheritDoc}
         * <p>
         * The following attributes are not deeply copied:
         * <ul>
         * <li>next: {@link AttitudeLaw}</li>
         * </ul>
         * </p>
         */
        @Override
        public EventDetector copy() {
            return new Switch(this.event.copy(), this.switchOnIncrease, this.switchOnDecrease, this.next);
        }
    }
}
