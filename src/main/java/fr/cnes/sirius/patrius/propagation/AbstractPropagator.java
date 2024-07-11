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
* VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
* VERSION:4.9:DM:DM-3165:10/05/2022:[PATRIUS] Amelioration de la propagation du signal 
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.7:FA:FA-2886:18/05/2021:Pas de pas de propagation par defaut avec une orbite hyperbolique 
 * VERSION:4.5:FA:FA-2338:27/05/2020:Correction dans IntervalOccurenceDetector 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:208:05/08/2014: one shot event detector
 * VERSION::DM:226:12/09/2014: problem with event detections.
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::FA:381:15/12/2014:Propagator tolerances and default mass issues
 * VERSION::FA:415:09/03/2015: Attitude discontinuity on event issue
 * VERSION::FA:473:24/09/2015: Problem with propagate
 * VERSION::FA:468:22/10/2015:Proper handling of ephemeris mode for analytical propagators
 * VERSION::DM:426:30/10/2015:Possibility to set up orbits in non inertial frames
 * VERSION::DM:480:15/02/2016: new analytical propagators and mean/osculating conversion
 * VERSION::DM:484:16/11/2015:Increase performance in simple use case
 * VERSION::FA:578:23/03/2016:Bug in management of deleted events
 * VERSION::FA:612:21/07/2016:Bug in same date events with Action.STOP
 * VERSION::FA:648:27/07/2016:Corrected minor points staying from V3.2
 * VERSION::FA:764:06/01/2017:Anomaly on events detection
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.exception.NoBracketingException;
import fr.cnes.sirius.patrius.math.exception.TooManyEvaluationsException;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.analytical.AnalyticalEphemerisModeHandler;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.events.EventState;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusFixedStepHandler;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusStepHandler;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusStepInterpolator;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusStepNormalizer;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * Common handling of {@link Propagator} methods for analytical propagators.
 * <p>
 * This abstract class allows to provide easily the full set of {@link Propagator} methods, including all propagation
 * modes support and discrete events support for any simple propagation method. Only one method must be implemented by
 * derived classes: {@link #propagateOrbit(AbsoluteDate)}. The first method should perform straightforward propagation
 * starting from some internally stored initial state up to the specified target date.
 * </p>
 * 
 * @author Luc Maisonobe
 */
@SuppressWarnings({"PMD.NullAssignment", "PMD.ConstructorCallsOverridableMethod"})
public abstract class AbstractPropagator implements Propagator{

    /** Default prefix for additional state provider from MassProvider. */
    public static final String MASS = "MASS_";

    /** Serializable UID. */
    private static final long serialVersionUID = 2434402795728927604L;

    /** One hundred. */
    private static final double ONE_HUNDRED = 100;

    /** Propagation mode. */
    private int mode;

    /** Fixed step size. */
    private double fixedStepSize;

    /** Step handler. */
    private PatriusStepHandler stepHandler;

    /** Event steps. */
    private final List<EventState> eventsStates;

    /** Initialization indicator of events states. */
    private boolean statesInitialized;

    /** Additional state providers. */
    private final List<AdditionalStateProvider> additionalStateProviders;

    /** Internal steps interpolator. */
    private final BasicStepInterpolator interpolator;

    /** Start date. */
    private AbsoluteDate startDate;

    /** Provider for attitude computation. */
    private final PVCoordinatesProvider pvProvider;

    /** Attitude provider for forces computation. */
    private AttitudeProvider attitudeProviderForces;

    /** Attitude provider for events computation. */
    private AttitudeProvider attitudeProviderEvents;

    /** Attitude provider given by default for one attitude. */
    private AttitudeProvider attitudeProviderByDefault;

    /** Indicator for last step. */
    private boolean isLastStep;

    /** Indicator for last detection of the current detector. */
    private boolean isLastDetection;

    /** Initial state. */
    private SpacecraftState initialState;

    /** Frame used for propagation. */
    private Frame propagationFrame;

    /**
     * Build a new instance.
     * 
     * @param attitudeProvider
     *        provider for attitude computation in case of both forces and events computation
     */
    protected AbstractPropagator(final AttitudeProvider attitudeProvider) {
        this.eventsStates = new ArrayList<>();
        this.statesInitialized = false;
        this.additionalStateProviders = new ArrayList<>();
        this.interpolator = new BasicStepInterpolator();
        this.pvProvider = new LocalPVProvider();
        this.attitudeProviderForces = null;
        this.attitudeProviderEvents = null;
        this.attitudeProviderByDefault = attitudeProvider;
        this.propagationFrame = null;
        this.setSlaveMode();
    }

    /**
     * Build a new instance.
     * 
     * @param attitudeProviderForcesIn
     *        provider for attitude computation in case of forces computation
     * @param attitudeProviderEventsIn
     *        provider for attitude computation in case of events computation
     */
    protected AbstractPropagator(final AttitudeProvider attitudeProviderForcesIn,
        final AttitudeProvider attitudeProviderEventsIn) {
        this.eventsStates = new ArrayList<>();
        this.statesInitialized = false;
        this.additionalStateProviders = new ArrayList<>();
        this.interpolator = new BasicStepInterpolator();
        this.pvProvider = new LocalPVProvider();
        this.attitudeProviderForces = attitudeProviderForcesIn;
        this.attitudeProviderEvents = attitudeProviderEventsIn;
        this.attitudeProviderByDefault = null;
        this.propagationFrame = null;
        this.setSlaveMode();
    }

    /**
     * Set a start date.
     * 
     * @param startDateIn
     *        start date
     */
    protected void setStartDate(final AbsoluteDate startDateIn) {
        this.startDate = startDateIn;
    }

    /** {@inheritDoc} */
    @Override
    public AttitudeProvider getAttitudeProvider() {
        final AttitudeProvider res;
        if (this.attitudeProviderByDefault == null) {
            if (this.attitudeProviderForces == null) {
                res = this.attitudeProviderEvents;
            } else {
                res = this.attitudeProviderForces;
            }
        } else {
            res = this.attitudeProviderByDefault;
        }
        return res;
    }

    /** {@inheritDoc} */
    @Override
    public AttitudeProvider getAttitudeProviderForces() {
        return this.attitudeProviderForces;
    }

    /** {@inheritDoc} */
    @Override
    public AttitudeProvider getAttitudeProviderEvents() {
        return this.attitudeProviderEvents;
    }

    /** {@inheritDoc} */
    @Override
    public void setAttitudeProvider(final AttitudeProvider attitudeProvider) {
        if ((this.attitudeProviderForces != null) || (this.attitudeProviderEvents != null)) {
            throw PatriusException.createIllegalStateException(PatriusMessages.TWO_ATTITUDES_TREATMENT_EXPECTED);
        }
        this.attitudeProviderByDefault = attitudeProvider;
        if (this.mode == EPHEMERIS_GENERATION_MODE) {
            ((AnalyticalEphemerisModeHandler) this.stepHandler).setAttitudeProviderForces(attitudeProvider);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setAttitudeProviderForces(final AttitudeProvider attProviderForces) {
        // If an attitude provider or an additional equation is already defined by default for a single attitude
        if (this.attitudeProviderByDefault != null) {
            throw PatriusException.createIllegalStateException(PatriusMessages.SINGLE_ATTITUDE_TREATMENT_EXPECTED);
        }
        this.attitudeProviderForces = attProviderForces;
        if (this.mode == EPHEMERIS_GENERATION_MODE) {
            ((AnalyticalEphemerisModeHandler) this.stepHandler).setAttitudeProviderForces(this.attitudeProviderForces);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setAttitudeProviderEvents(final AttitudeProvider attProviderEvents) {
        // If an attitude provider or an additional equation is already defined by default for a single attitude
        if (this.attitudeProviderByDefault != null) {
            throw PatriusException.createIllegalStateException(PatriusMessages.SINGLE_ATTITUDE_TREATMENT_EXPECTED);
        }
        this.attitudeProviderEvents = attProviderEvents;
        if (this.mode == EPHEMERIS_GENERATION_MODE) {
            ((AnalyticalEphemerisModeHandler) this.stepHandler).setAttitudeProviderEvents(this.attitudeProviderEvents);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setOrbitFrame(final Frame frame) throws PatriusException {
        if (frame.isPseudoInertial()) {
            this.propagationFrame = frame;
        } else {
            throw new PatriusException(PatriusMessages.NOT_INERTIAL_FRAME);
        }
    }

    /**
     * Get PV coordinates provider.
     * 
     * @return PV coordinates provider
     */
    public PVCoordinatesProvider getPvProvider() {
        return this.pvProvider;
    }

    /** {@inheritDoc} */
    @Override
    public SpacecraftState getInitialState() throws PatriusException {
        return this.initialState;
    }

    /** {@inheritDoc} */
    @Override
    public int getMode() {
        return this.mode;
    }

    /** {@inheritDoc} */
    @Override
    public Frame getFrame() {
        return this.propagationFrame == null ?
            (this.initialState != null && this.initialState.getFrame().isPseudoInertial() ? this.initialState
                .getFrame() : null)
            : this.propagationFrame;
    }

    /** {@inheritDoc} */
    @Override
    public void setSlaveMode() {
        this.mode = SLAVE_MODE;
        this.stepHandler = null;
        this.fixedStepSize = Double.NaN;
    }

    /** {@inheritDoc} */
    @Override
    public void setMasterMode(final double h,
                              final PatriusFixedStepHandler handler) {
        this.mode = MASTER_MODE;
        this.stepHandler = new PatriusStepNormalizer(h, handler);
        this.fixedStepSize = h;
    }

    /** {@inheritDoc} */
    @Override
    public void setMasterMode(final PatriusStepHandler handler) {
        this.mode = MASTER_MODE;
        this.stepHandler = handler;
        this.fixedStepSize = Double.NaN;
    }

    /** {@inheritDoc} */
    @Override
    public void setEphemerisMode() {
        this.mode = EPHEMERIS_GENERATION_MODE;
        if (this.attitudeProviderByDefault == null) {
            this.stepHandler =
                new AnalyticalEphemerisModeHandler(this, this.attitudeProviderForces, this.attitudeProviderEvents);
        } else {
            this.stepHandler = new AnalyticalEphemerisModeHandler(this, this.attitudeProviderByDefault, null);
        }
        this.fixedStepSize = Double.NaN;
    }

    /** {@inheritDoc} */
    @Override
    public BoundedPropagator getGeneratedEphemeris() {
        if (this.mode != EPHEMERIS_GENERATION_MODE) {
            throw PatriusException
                .createIllegalStateException(PatriusMessages.PROPAGATOR_NOT_IN_EPHEMERIS_GENERATION_MODE);
        }
        return ((AnalyticalEphemerisModeHandler) this.stepHandler).getEphemeris();
    }

    /** {@inheritDoc} */
    @Override
    public void addEventDetector(final EventDetector detector) {
        this.eventsStates.add(new EventState(detector));
    }

    /** {@inheritDoc} */
    @Override
    public Collection<EventDetector> getEventsDetectors() {
        final List<EventDetector> list = new ArrayList<>();
        for (final EventState state : this.eventsStates) {
            list.add(state.getEventDetector());
        }
        return Collections.unmodifiableCollection(list);
    }

    /** {@inheritDoc} */
    @Override
    public void clearEventsDetectors() {
        this.eventsStates.clear();
    }

    /**
     * Add a set of user-specified state parameters to be computed along with the orbit propagation.
     * 
     * @param additionalStateProvider
     *        provider for additional state
     */
    public void addAdditionalStateProvider(final AdditionalStateProvider additionalStateProvider) {
        // this is really a new name, add it
        this.additionalStateProviders.add(additionalStateProvider);
    }

    /**
     * Add a set of state parameters from MassProvider to be computed along with the orbit propagation.
     * 
     * @param massProvider
     *        mass provider for additional state
     */
    public void addAdditionalStateProvider(final MassProvider massProvider) {
        if (massProvider != null) {
            final List<String> partsNames = massProvider.getAllPartsNames();
            final int size = partsNames.size();
            for (int i = 0; i < size; i++) {
                final String partName = partsNames.get(i);
                final String addStateName = MASS + partName;
                this.additionalStateProviders.add(new AdditionalStateProvider(){

                    /**
                     * 
                     */
                    private static final long serialVersionUID = 8971118960598212581L;

                    /** {@inheritDoc} */
                    @Override
                    public String getName() {
                        return addStateName;
                    }

                    /** {@inheritDoc} */
                    @Override
                    public double[] getAdditionalState(final AbsoluteDate date) throws PropagationException {
                        return new double[] { massProvider.getMass(partName) };
                    }
                });
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public SpacecraftState propagate(final AbsoluteDate target) throws PropagationException {
        try {
            if (this.startDate == null) {
                this.startDate = this.getInitialState().getDate();
            }
            return this.propagate(this.startDate, target);
        } catch (final PatriusException oe) {

            // recover a possible embedded PropagationException
            for (Throwable t = oe; t != null; t = t.getCause()) {
                if (t instanceof PropagationException) {
                    // throw the caught propagation exception
                    throw (PropagationException) t;
                }
            }
            // propagation exception created from Patrius exception
            throw new PropagationException(oe);

        }
    }

    /** {@inheritDoc} */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Orekit code kept as such
    @Override
    @SuppressWarnings("PMD.AvoidRethrowingException")
    public SpacecraftState propagate(final AbsoluteDate start, final AbsoluteDate target) throws PropagationException {
        // CHECKSTYLE: resume CyclomaticComplexity check
        try {
            this.manageStateFrame();
            SpacecraftState state = null;
            this.interpolator.storeForwardFlag(start, target);

            if (!this.eventsStates.isEmpty() || (this.stepHandler != null)) {
                this.interpolator.storeDate(start);
                final double dt = target.durationFrom(start);
                final double epsilon = MathLib.ulp(dt);
                state = this.interpolator.getInterpolatedState();
                // evaluate step size
                final double stepSize;
                if (this.mode == MASTER_MODE) {
                    if (Double.isNaN(this.fixedStepSize)) {
                        // Step size arbitrarily at 1/100th of an orbit
                        // Use mean motion instead of keplerian period for robustness to hyperbolic orbits
                        stepSize = MathLib.copySign(2.0 * MathLib.PI / ONE_HUNDRED / state.getKeplerianMeanMotion(),
                                dt);
                    } else {
                        stepSize = MathLib.copySign(this.fixedStepSize, dt);
                    }
                } else {
                    stepSize = dt;
                }

                // initialize event detectors
                for (final EventState es : this.eventsStates) {
                    es.getEventDetector().init(state, target);
                }

                // initialize step handler
                if (this.stepHandler != null) {
                    this.stepHandler.init(state, target);
                }

                // iterate over the propagation range
                this.statesInitialized = false;
                this.isLastStep = false;
                this.isLastDetection = false;
                do {

                    // go ahead one step size
                    this.interpolator.shift();
                    final AbsoluteDate t = this.interpolator.getCurrentDate().shiftedBy(stepSize);
                    if ((dt == 0) || ((dt > 0) ^ (t.compareTo(target) <= 0))) {
                        // current step exceeds target
                        this.interpolator.storeDate(target);
                    } else {
                        // current step is within range
                        this.interpolator.storeDate(t);
                    }

                    // accept the step, trigger events and step handlers
                    state = this.acceptStep(target, epsilon);

                } while (!this.isLastStep);
            } else {
                this.interpolator.storeDate(target);
                state = this.interpolator.getInterpolatedState();
            }

            // return the last computed state
            this.startDate = state.getDate();
            return state;

        } catch (final PropagationException pe) {
            // In that specific case we want to keep the exception
            throw pe;
        } catch (final PatriusException oe) {
            throw PropagationException.unwrap(oe);
        } catch (final TooManyEvaluationsException tmee) {
            throw PropagationException.unwrap(tmee);
        } catch (final NoBracketingException nbe) {
            throw PropagationException.unwrap(nbe);
        }
    }

    /**
     * Accept a step, triggering events and step handlers.
     * 
     * @param target
     *        final propagation time
     * @param epsilon
     *        threshold for end date detection
     * @return state at the end of the step
     * @exception PatriusException
     *            if the switching function cannot be evaluated
     * @exception TooManyEvaluationsException
     *            if an event cannot be located
     * @exception NoBracketingException
     *            if bracketing cannot be performed
     */
    // CHECKSTYLE: stop MethodLength check
    // CHECKSTYLE: stop CyclomaticComplexity check
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Orekit code kept as such
    protected SpacecraftState acceptStep(final AbsoluteDate target, final double epsilon) throws PatriusException {
        // CHECKSTYLE: resume MethodLength check
        // CHECKSTYLE: resume CyclomaticComplexity check
        // CHECKSTYLE: resume ReturnCount check
        AbsoluteDate previousT = this.interpolator.getGlobalPreviousDate();
        AbsoluteDate currentT = this.interpolator.getGlobalCurrentDate();

        // initialize the events states if needed
        if (!this.statesInitialized) {

            // initialize the events states
            final AbsoluteDate t0 = this.interpolator.getPreviousDate();
            this.interpolator.setInterpolatedDate(t0);
            final SpacecraftState y = this.interpolator.getInterpolatedState();
            for (final EventState state : this.eventsStates) {
                state.reinitializeBegin(y);
            }

            this.statesInitialized = true;

        }

        // search for next events that may occur during the step
        final int orderingSign = this.interpolator.isForward() ? +1 : -1;
        final SortedSet<EventState> occuringEvents = new TreeSet<>(new Comparator<EventState>(){

                /** {@inheritDoc} */
                @Override
                public int compare(final EventState es0, final EventState es1) {
                    int rez = orderingSign * es0.getEventTime().compareTo(es1.getEventTime());
                    if (rez == 0) {
                        // First event is considered in case of identical date
                        rez = 1;
                    }
                    return rez;
                }

            });

        for (final EventState state : this.eventsStates) {
            if (state.evaluateStep(this.interpolator)) {
                // the event occurs during the current step
                occuringEvents.add(state);
            }
        }

        while (!occuringEvents.isEmpty()) {

            // handle the chronologically first event
            final Iterator<EventState> iterator = occuringEvents.iterator();
            final EventState currentEvent = iterator.next();
            iterator.remove();

            // restrict the interpolator to the first part of the step, up to the event
            final AbsoluteDate eventT = currentEvent.getEventTime();
            this.interpolator.setSoftPreviousDate(previousT);
            this.interpolator.setSoftCurrentDate(eventT);

            // trigger the event
            this.interpolator.setInterpolatedDate(eventT);
            final SpacecraftState eventY = this.interpolator.getInterpolatedState();

            // Specific case: event not treated if end of propagation
            this.isLastStep = Precision.equals(target.durationFrom(eventT), 0, epsilon);
            if (!this.isLastStep) {
                currentEvent.stepAccepted(eventY);
                this.isLastStep = currentEvent.stop();
            }
            this.isLastDetection = currentEvent.removeDetector();

            if (this.stepHandler != null) {
                this.stepHandler.handleStep(this.interpolator, this.isLastStep);
            }

            if (this.isLastStep) {
                // the event asked to stop integration
                return eventY;
            }

            if (currentEvent.isPendingReset()) {
                // If a reset is about to occur: check we didn't miss any event beforehand on interval [t0,
                // t(reset_state)]
                // Event could have been be missed because of maxCheck threshold
                boolean found = false;
                for (final EventState state : this.eventsStates) {
                    if ((this.interpolator.isForward() && state.getT0().compareTo(eventT) > 0) ||
                        (!this.interpolator.isForward() && state.getT0().compareTo(eventT) < 0)) {
                        // Particular case of event with slope selection != 2
                        // In that case t0 > eventT means we skipped an event because of slope selection
                        // In that case going backward in time requires to go back to beginning of step
                        this.interpolator.setInterpolatedDate(previousT);
                        state.storeState(this.interpolator.getInterpolatedState(), true);
                        this.interpolator.setInterpolatedDate(eventT);
                    }
                    if (!state.equals(currentEvent) && state.evaluateStep(this.interpolator)) {
                        // A missed event has been found during the reduced step
                        // If event occurs exactly at reset_state event time: it should not be considered since this
                        // event
                        // will be treated after reset state
                        if (MathLib.abs(state.getEventTime().durationFrom(eventT)) > state.getEventDetector()
                            .getThreshold()) {
                            occuringEvents.add(state);
                            found = true;
                        }
                    }
                }
                if (found) {
                    // If a missed event has been found, skip current reset state for now
                    // but add it in last to treat it once all new anterior events have been treated
                    // Warning: cancelled reset state will be detected twice (but reset performed once)
                    currentEvent.cancelStepAccepted();
                    occuringEvents.add(currentEvent);
                    continue;
                }
            }

            SpacecraftState resetState = currentEvent.reset(eventY);
            if (resetState != null) {
                // Full reset state (including attitude)
                resetState = this.resetState(resetState);

                // Check events just after event date and update
                // We cannot use the interpolator which is no longer up-to-date
                for (int i = 0; i < this.eventsStates.size(); i++) {
                    final EventState state = this.eventsStates.get(i);
                    if ((!state.equals(currentEvent)) && (state.evaluateStep(resetState))) {
                        // Pending event: treat it
                        state.stepAccepted(resetState);

                        // Treat RESET_STATE
                        if (state.isPendingReset()) {
                            resetState = state.reset(resetState);
                            resetState = this.resetState(resetState);
                        }

                        // Treat last detection
                        this.isLastDetection = state.removeDetector();
                        if (this.isLastDetection) {
                            this.eventsStates.remove(state);
                        }

                        // Treat Action.STOP
                        this.isLastStep = state.stop();
                        if (this.isLastStep) {
                            return resetState;
                        }
                    }
                }

                // Store all event states
                for (final EventState state : this.eventsStates) {
                    state.storeState(resetState, false);
                }

                if (this.isLastDetection) {
                    // the current event is removed from the event list
                    this.eventsStates.remove(currentEvent);
                }

                return resetState;
            }

            // prepare handling of the remaining part of the step
            previousT = eventT;
            this.interpolator.setSoftPreviousDate(eventT);
            this.interpolator.setSoftCurrentDate(currentT);

            if (this.isLastDetection) {
                // the current event is removed from the event list
                this.eventsStates.remove(currentEvent);
            } else {
                // check if the same event occurs again in the remaining part of the step
                if (currentEvent.evaluateStep(this.interpolator) && !this.isLastDetection) {
                    // the event occurs during the current step
                    occuringEvents.add(currentEvent);
                }
            }
        }

        final double remaining = target.durationFrom(currentT);
        if (this.interpolator.isForward()) {
            this.isLastStep = remaining < epsilon;
        } else {
            this.isLastStep = remaining > -epsilon;
        }
        if (this.isLastStep) {
            currentT = target;
        }

        this.interpolator.setInterpolatedDate(currentT);
        final SpacecraftState currentY = this.interpolator.getInterpolatedState();
        for (final EventState state : this.eventsStates) {
            state.stepAccepted(currentY);
            this.isLastStep = this.isLastStep || state.stop();
        }

        // handle the remaining part of the step, after all events if any
        if (this.stepHandler != null) {
            this.stepHandler.handleStep(this.interpolator, this.isLastStep);
        }

        return currentY;

    }

    /**
     * Reset state including attitude.
     * 
     * @param resetState
     *        current reseted state
     * @return reseted state including attitude
     * @throws PatriusException
     *         thrown if attitude could not be computed or failed to reset initial state
     */
    private SpacecraftState resetState(final SpacecraftState resetState) throws PatriusException {

        // Current PV provider cannot be used at this moment
        // Build a new one only for use at date of event after RESET_STATE action
        final SpacecraftState referenceState = resetState;
        final PVCoordinatesProvider coordProvider = new PVCoordinatesProvider(){
            /** Serializable UID. */
            private static final long serialVersionUID = -4370867554063148676L;

            /** {@inheritDoc} */
            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
                return referenceState.getPVCoordinates(frame);
            }

            /** {@inheritDoc} */
            @Override
            public Frame getNativeFrame(final AbsoluteDate date,
                    final Frame frame) throws PatriusException {
                return referenceState.getFrame();
            }
        };

        // Initialize attitude force and event
        Attitude attitudeForce = null;
        Attitude attitudeEvent = null;
        // If single attitude treatment
        if (this.attitudeProviderByDefault == null) {
            // If two attitudes treatment
            if (this.attitudeProviderForces != null) {
                attitudeForce = this.attitudeProviderForces.getAttitude(coordProvider, resetState.getDate(),
                    resetState.getFrame());
            }
            if (this.attitudeProviderEvents != null) {
                attitudeEvent = this.attitudeProviderEvents.getAttitude(coordProvider, resetState.getDate(),
                    resetState.getFrame());
            }
        } else {
            // single attitude treatment
            attitudeForce = this.attitudeProviderByDefault.getAttitude(coordProvider, resetState.getDate(),
                resetState.getFrame());
            attitudeEvent = null;
        }
        // Update attitude (orbit is unchanged since date is the same)
        final SpacecraftState res = new SpacecraftState(resetState.getOrbit(), attitudeForce, attitudeEvent,
            resetState.getAdditionalStates());

        this.resetInitialState(res);

        return res;
    }

    /**
     * Manage the state frame : the orbit to propagate is converted in the propagation frame.
     * 
     * @throws PatriusException
     *         if the frame of the initial state is not inertial or pseudo-inertial
     */
    protected void manageStateFrame() throws PatriusException {

        if (this.propagationFrame == null) {
            // Propagation frame has not been provided: frame used is orbit frame is inertial or pseudo-inertial
            if (this.getInitialState().getFrame().isPseudoInertial()) {
                this.propagationFrame = this.getInitialState().getFrame();
            } else {
                // Exception
                throw new PatriusException(PatriusMessages.NOT_INERTIAL_FRAME);
            }
        } else {
            // Propagation frame has been provided: convert initial state in propagation frame
            if (this.getInitialState().getFrame() != this.propagationFrame) {
                final Orbit initOrbit = this.getInitialState().getOrbit();
                final OrbitType type = initOrbit.getType();
                final Orbit propagationOrbit = type.convertOrbit(initOrbit, this.propagationFrame);
                this.initialState = this.getInitialState().updateOrbit(propagationOrbit);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
        return this.propagate(date).getPVCoordinates(frame);
    }

    /** {@inheritDoc} */
    @Override
    public SpacecraftState getSpacecraftState(final AbsoluteDate date) throws PropagationException {
        return this.propagate(date);
    }

    /**
     * Propagate an orbit without any fancy features.
     * <p>
     * This method is similar in spirit to the {@link #propagate} method, except that it does <strong>not</strong> call
     * any handler during propagation, nor any discrete events. It always stop exactly at the specified date.
     * </p>
     * 
     * @param date
     *        target date for propagation
     * @return state at specified date
     * @exception PropagationException
     *            if propagation cannot reach specified date
     */
    protected SpacecraftState basicPropagate(final AbsoluteDate date) throws PropagationException {
        try {

            // evaluate orbit
            final Orbit orbit = this.propagateOrbit(date);

            // evaluate attitudes
            // If single attitude treatment
            Attitude attitudeForces = null;
            Attitude attitudeEvents = null;
            if (this.attitudeProviderByDefault == null) {
                if (this.attitudeProviderForces != null) {
                    attitudeForces = this.attitudeProviderForces.getAttitude(this.pvProvider, date, orbit.getFrame());
                }
                if (this.attitudeProviderEvents != null) {
                    attitudeEvents = this.attitudeProviderEvents.getAttitude(this.pvProvider, date, orbit.getFrame());
                }
            } else {
                attitudeForces = this.attitudeProviderByDefault.getAttitude(this.pvProvider, date, orbit.getFrame());
                attitudeEvents = null;
                // If two attitudes treatment
            }
            // compute additional states
            if (this.additionalStateProviders != null && !this.additionalStateProviders.isEmpty()) {
                final SpacecraftState temp = new SpacecraftState(orbit, attitudeForces, attitudeEvents);
                final Map<String, double[]> additionalStates = new ConcurrentHashMap<>();
                for (final AdditionalStateProvider provider : this.additionalStateProviders) {
                    additionalStates.put(provider.getName(), provider.getAdditionalState(temp.getDate()));
                }
                return new SpacecraftState(orbit, attitudeForces, attitudeEvents, additionalStates);
            }

            return new SpacecraftState(orbit, attitudeForces, attitudeEvents);

        } catch (final PatriusException oe) {
            throw new PropagationException(oe);
        }
    }

    /**
     * Extrapolate an orbit up to a specific target date.
     * 
     * @param date
     *        target date for the orbit
     * @return extrapolated parameters
     * @exception PropagationException
     *            if some parameters are out of bounds
     */
    protected abstract Orbit propagateOrbit(final AbsoluteDate date) throws PropagationException;

    /** {@inheritDoc} */
    @Override
    public void resetInitialState(final SpacecraftState state) throws PropagationException {
        this.initialState = state;
    }

    /** Internal PVCoordinatesProvider for attitude computation. */
    private class LocalPVProvider implements PVCoordinatesProvider {

        /** Serializable UID. */
        private static final long serialVersionUID = -5121444553818793467L;

        /** {@inheritDoc} */
        @Override
        public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
            return AbstractPropagator.this.propagateOrbit(date).getPVCoordinates(frame);
        }

        /** {@inheritDoc} */
        @Override
        public Frame getNativeFrame(final AbsoluteDate date,
                final Frame frame) throws PatriusException {
            return AbstractPropagator.this.getFrame();
        }
    }

    /** Internal class for local propagation. */
    private class BasicStepInterpolator implements PatriusStepInterpolator {

        /** Serializable UID. */
        private static final long serialVersionUID = 26269718303505539L;

        /** Global previous date. */
        private AbsoluteDate globalPreviousDate;

        /** Global current date. */
        private AbsoluteDate globalCurrentDate;

        /** Global forward propagation indicator. */
        private boolean globalForward;

        /** Soft previous date. */
        private AbsoluteDate softPreviousDate;

        /** Soft current date. */
        private AbsoluteDate softCurrentDate;

        /** Interpolated state. */
        private SpacecraftState interpolatedState;

        /** Forward propagation indicator. */
        private boolean forward;

        /**
         * Build a new instance from a basic propagator.
         */
        public BasicStepInterpolator() {
            this.globalPreviousDate = AbsoluteDate.PAST_INFINITY;
            this.globalCurrentDate = AbsoluteDate.PAST_INFINITY;
            this.softPreviousDate = AbsoluteDate.PAST_INFINITY;
            this.softCurrentDate = AbsoluteDate.PAST_INFINITY;
        }

        /**
         * Restrict step range to a limited part of the global step.
         * <p>
         * This method can be used to restrict a step and make it appear as if the original step was smaller. Calling
         * this method <em>only</em> changes the value returned by {@link #getPreviousDate()}, it does not change any
         * other property
         * </p>
         * 
         * @param softPreviousDateIn
         *        start of the restricted step
         */
        public void setSoftPreviousDate(final AbsoluteDate softPreviousDateIn) {
            this.softPreviousDate = softPreviousDateIn;
        }

        /**
         * Restrict step range to a limited part of the global step.
         * <p>
         * This method can be used to restrict a step and make it appear as if the original step was smaller. Calling
         * this method <em>only</em> changes the value returned by {@link #getCurrentDate()}, it does not change any
         * other property
         * </p>
         * 
         * @param softCurrentDateIn
         *        end of the restricted step
         */
        public void setSoftCurrentDate(final AbsoluteDate softCurrentDateIn) {
            this.softCurrentDate = softCurrentDateIn;
        }

        /**
         * Get the previous global grid point time.
         * 
         * @return previous global grid point time
         */
        public AbsoluteDate getGlobalPreviousDate() {
            return this.globalPreviousDate;
        }

        /**
         * Get the current global grid point time.
         * 
         * @return current global grid point time
         */
        public AbsoluteDate getGlobalCurrentDate() {
            return this.globalCurrentDate;
        }

        /** {@inheritDoc} */
        @Override
        public AbsoluteDate getCurrentDate() {
            return this.softCurrentDate;
        }

        /** {@inheritDoc} */
        @Override
        public AbsoluteDate getInterpolatedDate() {
            return this.interpolatedState.getDate();
        }

        /** {@inheritDoc} */
        @Override
        public SpacecraftState getInterpolatedState() throws PatriusException {
            return this.interpolatedState;
        }

        /** {@inheritDoc} */
        @Override
        public AbsoluteDate getPreviousDate() {
            return this.softPreviousDate;
        }

        /** {@inheritDoc} */
        @Override
        public boolean isForward() {
            return this.forward;
        }

        /** {@inheritDoc} */
        @Override
        public void setInterpolatedDate(final AbsoluteDate date) throws PropagationException {

            // compute the raw spacecraft state
            this.interpolatedState = AbstractPropagator.this.basicPropagate(date);

        }

        /**
         * Shift one step forward.
         * Copy the current date into the previous date, hence preparing the
         * interpolator for future calls to {@link #storeDate storeDate}
         */
        public void shift() {
            this.globalPreviousDate = this.globalCurrentDate;
            this.softPreviousDate = this.globalPreviousDate;
            this.softCurrentDate = this.globalCurrentDate;
        }

        /**
         * Store the current step date.
         * 
         * @param date
         *        current date
         * @exception PropagationException
         *            if the state cannot be propagated at specified date
         */
        public void storeDate(final AbsoluteDate date) throws PropagationException {
            this.globalCurrentDate = date;
            this.softCurrentDate = this.globalCurrentDate;

            if (this.globalCurrentDate.compareTo(this.globalPreviousDate) == 0) {
                // Current date = previous date: the only way to known propagation direction is to compare global
                // propagation direction
                this.forward = this.globalForward;
            } else {
                this.forward = this.globalCurrentDate.compareTo(this.globalPreviousDate) >= 0;
            }

            this.setInterpolatedDate(this.globalCurrentDate);
        }

        /**
         * Store global forward direction flag.
         * 
         * @param start
         *        start date
         * @param target
         *        target propagation date
         */
        public void storeForwardFlag(final AbsoluteDate start, final AbsoluteDate target) {
            this.globalForward = target.compareTo(start) >= 0;
        }
    }

}
