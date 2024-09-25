/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 * @history 22/03/2013
 *
 * HISTORY
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.13:DM:DM-132:08/12/2023:[PATRIUS] Suppression de la possibilite
 * de convertir les sorties de VacuumSignalPropagation
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11:DM:DM-3235:22/05/2023:[PATRIUS][TEMPS_CALCUL] Attitude spacecraft state lazy
 * VERSION:4.11:DM:DM-3256:22/05/2023:[PATRIUS] Suite 3246
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3165:10/05/2022:[PATRIUS] Amelioration de la propagation du signal 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:71:25/05/2013:Access modifiers updates.
 * VERSION::FA:180:18/03/2014:Added FA 71 history
 * VERSION::DM:200:28/08/2014: dealing with a negative mass in the propagator
 * VERSION::DM:208:05/08/2014: one shot event detector
 * VERSION::DM:226:12/09/2014: problem with event detections.
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::FA:406:20/02/2015:Checkstyle corrections (nb cyclomatic)
 * VERSION::DM:317:04/03/2015: STELA integration in CIRF with referential choice (ICRF, CIRF or MOD)
 * VERSION::FA:391:13/04/2015: system to retrieve STELA dE/dt
 * VERSION::DM:426:06/11/2015:set propagation frame
 * VERSION::FA:764:06/01/2017:Anomaly on events detection
 * VERSION::FA:1286:05/09/2017:correct osculating orbit propagation
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.propagation;

/**
 * <p>
 * Stela Abstract Propagator is an abstract propagator espacially usefull for semi-analytical propagation.
 * It derives directly from the Orekit Abstract propagator.
 *</p>
 *<p>
 *Notable changes from Abstract Propagator:
 * - Interpolator is not included and can be changed
 * - Integration is performed in the propagate method (through propagationManagement,goAhead and basicPropagate)
 * - Interpolator does not normally integrate
 *</p>
 *
 * @author Cedric Dental
 * @author Luc Maisonobe
 *
 * @version $Id$
 *
 * @since 1.3
 *
 * Licensed to CS Systèmes d'Information (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.events.EventDetector;
import fr.cnes.sirius.patrius.events.utils.EventState;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.exception.NoBracketingException;
import fr.cnes.sirius.patrius.math.exception.TooManyEvaluationsException;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.AdditionalStateProvider;
import fr.cnes.sirius.patrius.propagation.BoundedPropagator;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusFixedStepHandler;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusStepHandler;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusStepNormalizer;
import fr.cnes.sirius.patrius.stela.propagation.data.TimeDerivativeData;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * Common handling of {@link Propagator} methods for analytical propagators.
 * <p>
 * This abstract class allows to provide easily the full set of {@link Propagator} methods, including all propagation
 * modes support and discrete events support for any simple propagation method. Only two methods must be implemented by
 * derived classes: {@link #propagateSpacecraftState(AbsoluteDate)} and {@link #getMass(AbsoluteDate)}. The first method
 * should perform straightforward propagation starting from some internally stored initial state up to the specified
 * target date.
 * </p>
 */
// CHECKSTYLE: stop AbstractClassName check
@SuppressWarnings({ "PMD.AbstractNaming", "PMD.NullAssignment", "PMD.ConstructorCallsOverridableMethod" })
public abstract class StelaAbstractPropagator implements Propagator {
    // CHECKSTYLE: resume AbstractClassName check

    /** Serializable UID. */
    private static final long serialVersionUID = 2434402795728927604L;

    /** Entry Step handler. */
    protected PatriusFixedStepHandler oldStepHandler;

    /** Event steps. */
    protected final Collection<EventState> eventsStates;

    /** Integrator for the orbital extrapolation process. */
    protected FirstOrderIntegrator integrator;

    /** Attitude provider for forces computation. */
    protected AttitudeProvider attitudeProviderForces;

    /** Attitude provider for events computation. */
    protected AttitudeProvider attitudeProviderEvents;

    /** Attitude provider given by default for one attitude. */
    protected AttitudeProvider attitudeProviderByDefault;

    /** Propagation mode. */
    private int mode;

    /** Fixed step size. */
    private double fixedStepSize;

    /** Step handler. */
    private PatriusStepHandler stepHandler;

    /** Step handlers. */
    private List<PatriusStepHandler> stepHandlers;

    /** Initialization indicator of events states. */
    private boolean statesInitialized;

    /** Additional state providers. */
    private final List<AdditionalStateProvider> additionalStateProviders;

    /** Steps interpolator. */
    private final StelaBasicInterpolator interpolator;

    /** Start date of last propagation. */
    private AbsoluteDate lastPropagationStart;

    /** End date of last propagation. */
    private AbsoluteDate lastPropagationEnd;

    /** Start date. */
    private AbsoluteDate startDate;

    /** Provider for attitude computation. */
    private final PVCoordinatesProvider pvProvider;

    /** Initial state. */
    private SpacecraftState initialState;

    /** Indicator for last step. */
    private boolean isLastStep;

    /** Indicator for last detection of the current detector. */
    private boolean isLastDetection;

    /** Initialisation indicator for interpolation */
    private boolean interInitialised;

    /** Flag to indicate if time derivatives dE'/dt must be stored. */
    private boolean storeTimeDerivatives;

    /** Flag to indicate if time derivatives dE'/dt must be stored during next step (internal use only). */
    private boolean registerTimeDerivatives;

    /** Time derivatives list. */
    private List<TimeDerivativeData> timeDerivativesList;

    /**
     * Build a new instance.
     *
     * @param attitudeProviderIn
     *        provider for attitude computation
     * @param inInterpolator
     *        the interpolator used during the propagation
     */
    protected StelaAbstractPropagator(final AttitudeProvider attitudeProviderIn,
                                      final StelaBasicInterpolator inInterpolator) {
        this.eventsStates = new ArrayList<>();
        this.statesInitialized = false;
        this.additionalStateProviders = new ArrayList<>();
        this.interpolator = inInterpolator;
        this.lastPropagationStart = AbsoluteDate.PAST_INFINITY;
        this.lastPropagationEnd = AbsoluteDate.FUTURE_INFINITY;
        this.pvProvider = new LocalPVProvider();
        this.attitudeProviderForces = null;
        this.attitudeProviderEvents = null;
        this.attitudeProviderByDefault = attitudeProviderIn;
        this.interInitialised = false;
        this.setSlaveMode();
        this.storeTimeDerivatives = false;
        this.registerTimeDerivatives = false;
    }

    /**
     * Build a new instance.
     *
     * @param attitudeProviderInForces
     *        attitude for forces computation
     * @param attitudeProviderInEvents
     *        attitude for events computation
     * @param inInterpolator
     *        the interpolator used during the propagation
     */
    protected StelaAbstractPropagator(final AttitudeProvider attitudeProviderInForces,
                                      final AttitudeProvider attitudeProviderInEvents,
                                      final StelaBasicInterpolator inInterpolator) {
        this.eventsStates = new ArrayList<>();
        this.statesInitialized = false;
        this.additionalStateProviders = new ArrayList<>();
        this.interpolator = inInterpolator;
        this.lastPropagationStart = AbsoluteDate.PAST_INFINITY;
        this.lastPropagationEnd = AbsoluteDate.FUTURE_INFINITY;
        this.pvProvider = new LocalPVProvider();
        this.attitudeProviderForces = attitudeProviderInForces;
        this.attitudeProviderEvents = attitudeProviderInEvents;
        this.attitudeProviderByDefault = null;
        this.interInitialised = false;
        this.setSlaveMode();
        this.storeTimeDerivatives = false;
        this.registerTimeDerivatives = false;
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
        AttitudeProvider result = null;
        if (this.attitudeProviderByDefault == null) {
            if (this.attitudeProviderForces == null) {
                result = this.attitudeProviderEvents;
            } else {
                result = this.attitudeProviderForces;
            }
        } else {
            result = this.attitudeProviderByDefault;
        }
        return result;
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

    /**
     * This method is only used in local in order to deals with single attitude treatment and two attitudes treatment.
     *
     * @return the attitude provider for forces computation or attitude provider defined for a single attitude.
     */
    private AttitudeProvider getAttitudeProviderForcesLocal() {
        // If single attitude treatment
        if (this.attitudeProviderByDefault == null) {
            return this.attitudeProviderForces;
        }

        // If two attitudes treatment
        return this.attitudeProviderByDefault;
    }

    /**
     * This method is only used in local in order to deals with single attitude treatment and two attitudes treatment.
     *
     * @return the attitude provider for events computation or attitude provider defined for a single attitude.
     */
    private AttitudeProvider getAttitudeProviderEventsLocal() {
        // If single attitude treatment
        if (this.attitudeProviderByDefault == null) {
            // If two attitudes treatment
            return this.attitudeProviderEvents;
        }
        return this.attitudeProviderByDefault;
    }

    /** {@inheritDoc} */
    @Override
    public void setAttitudeProvider(final AttitudeProvider attitudeProviderIn) {
        if ((this.attitudeProviderForces != null) || (this.attitudeProviderEvents != null)) {
            throw PatriusException.createIllegalStateException(PatriusMessages.TWO_ATTITUDES_TREATMENT_EXPECTED);
        }
        this.attitudeProviderByDefault = attitudeProviderIn;
    }

    /** {@inheritDoc} */
    @Override
    public void setAttitudeProviderForces(final AttitudeProvider attitudeProviderIn) {
        // If an attitude provider or an additional equation is already defined by default for a single attitude
        if (this.attitudeProviderByDefault != null) {
            throw PatriusException.createIllegalStateException(PatriusMessages.SINGLE_ATTITUDE_TREATMENT_EXPECTED);
        }
        this.attitudeProviderForces = attitudeProviderIn;
    }

    /** {@inheritDoc} */
    @Override
    public void setAttitudeProviderEvents(final AttitudeProvider attitudeProviderIn) {
        // If an attitude provider or an additional equation is already defined by default for a single attitude
        if (this.attitudeProviderByDefault != null) {
            throw PatriusException.createIllegalStateException(PatriusMessages.SINGLE_ATTITUDE_TREATMENT_EXPECTED);
        }
        this.attitudeProviderEvents = attitudeProviderIn;
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
        return this.initialState.getFrame();
    }

    /** {@inheritDoc} */
    @Override
    public void setSlaveMode() {
        this.mode = SLAVE_MODE;
        this.stepHandler = null;
        this.fixedStepSize = Double.NaN;
    }

    /**
     * {@inheritDoc} Note that mean elements will be provided by the step handler.
     */
    @Override
    public void setMasterMode(final double h,
                              final PatriusFixedStepHandler handler) {
        this.mode = MASTER_MODE;
        this.stepHandler = new PatriusStepNormalizer(h, handler);
        this.fixedStepSize = h;
        this.oldStepHandler = handler;
    }

    /**
     * {@inheritDoc} Note that mean elements will be provided by the step handler.
     */
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
        this.stepHandler = null;
        this.fixedStepSize = Double.NaN;
    }

    /**
     * Set propagation frame.
     * 
     * @param frame
     *        the frame to use.
     *        This frame must be inertial or pseudo-inertial, otherwise an exception is raised.
     * @throws PatriusException
     *         if frame is not inertial or pseudo-inertial
     */
    public void setOrbitFrame(final Frame frame) throws PatriusException {
        throw new PatriusException(PatriusMessages.STELA_INTEGRATION_FRAME_NOT_SUPPORTED);
    }

    /** {@inheritDoc} */
    @Override
    public BoundedPropagator getGeneratedEphemeris() {
        return new BoundedPropagatorView(this.lastPropagationStart, this.lastPropagationEnd);
    }

    /**
     * {@inheritDoc} Note that mean elements will be provided to event detectors in <i>g</i> function.
     */
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
     * @exception PatriusException
     *            if an additional state with the same name is already present
     */
    public void addAdditionalStateProvider(
                                           final AdditionalStateProvider additionalStateProvider)
        throws PatriusException {
        // this is really a new name, add it
        this.additionalStateProviders.add(additionalStateProvider);
    }

    /**
     * {@inheritDoc} Note that mean elements are returned.
     */
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
                    // throw embedded PropagationException
                    throw (PropagationException) t;
                }
            }
            // Throw a new PropagationException created from caught PatriusException
            throw new PropagationException(oe);

        }
    }

    /**
     * {@inheritDoc} Note that mean elements are returned.
     */
    @Override
    @SuppressWarnings("PMD.AvoidRethrowingException")
    public SpacecraftState propagate(final AbsoluteDate start, final AbsoluteDate target) throws PropagationException {
        try {
            // Retrieve propagator-specific step handlers
            this.stepHandlers = this.getStepHandlers();

            // Time derivative storage initialization
            this.initTimeDerivatives();

            this.lastPropagationStart = start;

            final double dt = target.durationFrom(start);
            final double epsilon = MathLib.ulp(dt);
            SpacecraftState state = this.basicPropagate(start);
            // interpolator.storeSC(state, state);
            if (!this.interInitialised) {
                this.interpolator.storeSC(state, state);
                this.interInitialised = true;
            }

            // evaluate step size
            final double stepSize = this.evaluateStep(state, dt);

            // initialize event detectors
            for (final EventState es : this.eventsStates) {
                es.getEventDetector().init(state, target);
            }

            // initialize step handler
            if (this.stepHandler != null) {
                this.stepHandler.init(state, target);
            }
            for (int i = 0; i < this.stepHandlers.size(); i++) {
                this.stepHandlers.get(i).init(state, target);
            }

            // iterate over the propagation range
            this.statesInitialized = false;
            this.isLastStep = false;
            this.isLastDetection = false;
            do {
                this.setRegisterTimeDerivatives(true);

                // compute the final state
                final SpacecraftState finalState = this.propagationManagement(state, stepSize, dt, target);

                // store values in the interpolator
                this.interpolator.storeSC(state, finalState);

                // update additional state
                this.interpolator.setAdditionalStateProviders(this.additionalStateProviders);

                // accept the step, trigger events and step handlers
                state = this.acceptStep(target, epsilon);
                this.initialState = null;
                this.initialState = state;

            } while (!this.isLastStep);

            // return the last computed state
            this.lastPropagationEnd = state.getDate();
            this.startDate = state.getDate();
            return state;

        } catch (final PropagationException pe) {
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
     * Initialize time derivatives storage.
     */
    private void initTimeDerivatives() {
        if (this.isStoreTimeDerivatives()) {
            this.timeDerivativesList = new ArrayList<>();
            this.registerTimeDerivatives = true;
        }
    }

    /**
     * Setter for flag indicating if time derivatives dE'/dt must be stored during next step.
     *
     * @param flag
     *        flag indicating if time derivatives dE'/dt must be stored during next step
     */
    private void setRegisterTimeDerivatives(final boolean flag) {
        if (this.isStoreTimeDerivatives()) {
            this.registerTimeDerivatives = flag;
        }
    }

    /**
     * Provides a list of step handlers to be performed during propagation.
     *
     * @return propagation list of step handlers.
     */
    protected abstract List<PatriusStepHandler> getStepHandlers();

    /**
     *
     * go one step ahead
     *
     * @param stepSize
     *        the current stepsize
     * @param dt
     *        the dt time
     * @param target
     *        target date to propagate
     * @return finalState
     *         the final spacecraft state
     * @throws PropagationException
     *         propagation exception
     * @since 1.3
     */

    protected SpacecraftState goAhead(final double stepSize, final double dt,
                                      final AbsoluteDate target) throws PropagationException {
        // goes ahead one step size

        final AbsoluteDate t = this.interpolator.getCurrentDate().shiftedBy(stepSize);
        final SpacecraftState finalState;
        if ((dt == 0) || ((dt > 0) ^ (t.compareTo(target) <= 0))) {
            // current step exceeds target
            finalState = this.basicPropagate(target);
        } else {
            // current step is within range
            finalState = this.basicPropagate(t);
        }

        return finalState;
    }

    /**
     * Manages the current step, method to override when user wants to deal with exceptions during the propagation.
     *
     * @param state
     *        the current SpacecraftState
     * @param stepSize
     *        the current stepsize
     * @param dt
     *        the dt time
     * @param target
     *        target date to propagate
     * @return finalState
     *         the final spacecraft state
     * @throws PatriusException
     *         Orekit exception
     * @since 1.3
     */
    protected SpacecraftState propagationManagement(final SpacecraftState state, final double stepSize,
                                                    final double dt,
                                                    final AbsoluteDate target) throws PatriusException {
        // Goes ahead one step size
        return this.goAhead(stepSize, dt, target);
    }

    /**
     * Evaluate the stepsize depending on the chosen mode of propagation.
     *
     * @param state
     *        the spacecraft state
     * @param dt
     *        the integration duration
     * @return stepSize
     *         the step size
     *
     * @since 1.3
     */
    private double evaluateStep(final SpacecraftState state, final double dt) {

        // evaluate step size
        final double stepSize;
        if (this.mode == MASTER_MODE) {
            if (Double.isNaN(this.fixedStepSize)) {
                final int constant = 100;
                stepSize = MathLib.copySign(state.getKeplerianPeriod() / constant, dt);
            } else {
                stepSize = MathLib.copySign(this.fixedStepSize, dt);
            }
        } else {
            stepSize = dt;
        }

        return stepSize;
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
     */
    @SuppressWarnings("PMD.PrematureDeclaration")
    protected SpacecraftState acceptStep(final AbsoluteDate target, final double epsilon) throws PatriusException {

        final AbsoluteDate previousT = this.interpolator.getPreviousDate();
        AbsoluteDate currentT = this.interpolator.getCurrentDate();

        // Initialize the events states if needed
        this.initializeStates(previousT);

        // Search for next events that may occur during the step
        final SortedSet<EventState> occuringEvents = this.getEvents();

        while (!occuringEvents.isEmpty()) {

            // handle the chronologically first event
            final Iterator<EventState> iterator = occuringEvents.iterator();
            final EventState currentEvent = iterator.next();
            iterator.remove();

            // restrict the interpolator to the first part of the step, up to the event
            final AbsoluteDate eventT = currentEvent.getEventTime();

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

            // handle the first part of the step, up to the event
            this.handleStep();

            if (this.isLastStep) {
                // the event asked to stop integration
                return eventY;
            }

            // Store states
            this.storeStates(currentEvent, eventY);

            // Reset state
            final SpacecraftState resetState = currentEvent.reset(eventY);
            if (resetState != null) {
                return this.resetState(resetState, currentEvent);
            }
            // check if the same event occurs again in the remaining part of the step
            if (currentEvent.evaluateStep(this.interpolator) && !this.isLastDetection) {
                // the event occurs during the current step
                occuringEvents.add(currentEvent);
            }

            if (this.isLastDetection) {
                // the current event is removed from the event list
                this.eventsStates.remove(currentEvent);
            }
        }

        // Evaluate remaining
        currentT = this.evaluateRemaining(target, currentT, epsilon);

        this.interpolator.setInterpolatedDate(currentT);
        final SpacecraftState currentY = this.interpolator.getInterpolatedState();
        for (final EventState state : this.eventsStates) {
            state.stepAccepted(currentY);
            this.isLastStep = this.isLastStep || state.stop();
        }

        // handle the remaining part of the step, after all events if any
        this.handleStep();

        return currentY;
    }

    /**
     * Initialize states.
     *
     * @param previousT
     *        previous date
     * @throws PatriusException
     *         thrown if an even state initialization failed or interpolation failed
     */
    private void initializeStates(final AbsoluteDate previousT) throws PatriusException {
        if (!this.statesInitialized) {
            // initialize the events states
            final AbsoluteDate t0 = previousT;
            this.interpolator.setInterpolatedDate(t0);
            final SpacecraftState y = this.interpolator.getInterpolatedState();
            for (final EventState state : this.eventsStates) {
                state.reinitializeBegin(y);
            }

            this.statesInitialized = true;
        }
    }

    /**
     * Get events.
     *
     * @return events
     * @throws TooManyEvaluationsException
     * @throws NoBracketingException
     * @throws PatriusException
     *         thrown if some event state could not be evaluated
     */
    private SortedSet<EventState> getEvents() throws PatriusException {

        // Get interpolation direction
        final int orderingSign = this.interpolator.isForward() ? +1 : -1;
        final SortedSet<EventState> occuringEvents =
            new TreeSet<>(new Comparator<EventState>(){

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

        // Loop on all event states
        for (final EventState state : this.eventsStates) {
            if (state.evaluateStep(this.interpolator)) {
                // the event occurs during the current step
                occuringEvents.add(state);
            }
        }

        return occuringEvents;
    }

    /**
     * Evaluate remaining
     *
     * @param target
     *        final propagation time
     * @param currentT
     *        current time
     * @param epsilon
     *        threshold for end date detection
     * @return the updated current time
     *
     * @since 3.0
     */
    private AbsoluteDate evaluateRemaining(final AbsoluteDate target, final AbsoluteDate currentT,
                                           final double epsilon) {
        final double remaining = target.durationFrom(currentT);
        if (this.interpolator.isForward()) {
            this.isLastStep = remaining < epsilon;
        } else {
            this.isLastStep = remaining > -epsilon;
        }
        if (this.isLastStep) {
            return target;
        }
        return currentT;
    }

    /**
     * Reset state.
     *
     * @param oldState
     *        the state
     * @param currentEvent
     *        current event
     * @return the reset state with new attitudes.
     * @throws PatriusException
     *         if no attitudes defined
     *
     * @since 3.0
     */
    private SpacecraftState resetState(final SpacecraftState oldState,
                                       final EventState currentEvent) throws PatriusException {

        // Update attitude (orbit is unchanged since date is the same)
        final SpacecraftState resetState;
        if (this.attitudeProviderByDefault == null) {
            // If two attitudes treatment
            resetState = new SpacecraftState(attitudeProviderForces, attitudeProviderEvents,
                    oldState.getOrbit(), oldState.getAdditionalStates());
        } else {
            // If single attitude treatment
            resetState = new SpacecraftState(attitudeProviderByDefault, attitudeProviderByDefault, oldState.getOrbit(),
                    oldState.getAdditionalStates());
        }
        this.resetInitialState(resetState);

        // Check events just after event date and update
        // We cannot use the interpolator which is no longer up-to-date
        for (final EventState state : this.eventsStates) {
            if ((!state.equals(currentEvent)) && (state.evaluateStep(resetState))) {
                state.stepAccepted(resetState);
                state.reset(resetState);
            }
        }

        // Reinitialize all event states
        for (final EventState state : this.eventsStates) {
            state.storeState(resetState, false);
        }

        return resetState;
    }

    /**
     * Store states.
     *
     * @param currentEvent
     *        current event
     * @param eventY
     *        state at event
     * @throws PatriusException
     *         thrown if store state failed
     */
    private void storeStates(final EventState currentEvent, final SpacecraftState eventY) throws PatriusException {
        if (currentEvent.isPendingReset()) {
            // Store event state before reset
            for (final EventState state : this.eventsStates) {
                if (!state.equals(currentEvent)) {
                    state.storeState(eventY, false);
                }
            }
        }
    }

    /**
     * Handle step.
     *
     * @throws PropagationException
     *         thrown if step handler call failed
     */
    private void handleStep() throws PropagationException {
        if (this.stepHandler != null) {
            this.stepHandler.handleStep(this.interpolator, this.isLastStep);
        }
        for (int i = 0; i < this.stepHandlers.size(); i++) {
            this.stepHandlers.get(i).handleStep(this.interpolator, this.isLastStep);
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
     * Propagate a SpacecraftState without any fancy features.
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
            // evaluate SpacecraftState
            return this.propagateSpacecraftState(date);
        } catch (final PatriusException oe) {
            throw new PropagationException(oe);
        }
    }

    /**
     * Extrapolate a spacecraftState up to a specific target date.
     *
     * @param date
     *        target date for the orbit
     * @return extrapolated parameters
     * @throws PatriusException
     *         * if some parameters are out of bounds
     *
     */
    protected abstract SpacecraftState propagateSpacecraftState(final AbsoluteDate date) throws PatriusException;

    /**
     * Get the mass.
     *
     * @param date
     *        target date for the orbit
     * @return mass mass
     * @exception PropagationException
     *            if some parameters are out of bounds
     */
    protected abstract double getMass(final AbsoluteDate date) throws PropagationException;

    /** {@inheritDoc} */
    @Override
    public void resetInitialState(final SpacecraftState state) throws PropagationException {
        this.initialState = state;
    }

    /**
     * Set the integrator.
     *
     * @param integrator
     *        integrator to use for propagation.
     */
    public void setIntegrator(final FirstOrderIntegrator integrator) {
        this.integrator = integrator;
    }

    // =========================== Time derivatives =========================== //

    /**
     * Returns flag indicating if time derivatives dE'/dt must be stored.
     *
     * @return flag indicating if time derivatives dE'/dt must be stored
     */
    public boolean isStoreTimeDerivatives() {
        return this.storeTimeDerivatives;
    }

    /**
     * Returns flag indicating if time derivatives dE'/dt have to be stored during next step.
     * Method for internal use only.
     *
     * @return flag indicating if time derivatives dE'/dt have to be stored during next step
     */
    public boolean isRegisterTimeDerivatives() {
        return this.registerTimeDerivatives;
    }

    /**
     * Setter for flag indicating if time derivatives dE'/dt must be stored.
     *
     * @param isStoreTimeDerivatives
     *        flag indicating if time derivatives dE'/dt must be stored
     */
    public void setStoreTimeDerivatives(final boolean isStoreTimeDerivatives) {
        this.storeTimeDerivatives = isStoreTimeDerivatives;
    }

    /**
     * Returns time derivatives list.
     *
     * @return time derivatives list
     */
    public List<TimeDerivativeData> getTimeDerivativesList() {
        return this.timeDerivativesList;
    }

    /**
     * Add time derivatives data to list.
     * Method for internal use only.
     *
     * @param data
     *        time derivative data
     */
    public void addTimeDerivativeData(final TimeDerivativeData data) {
        this.timeDerivativesList.add(data);

        // Once time derivatives have been stored, wait for next step to store them again
        this.registerTimeDerivatives = false;
    }

    /** Internal PVCoordinatesProvider for attitude computation. */
    private class LocalPVProvider implements PVCoordinatesProvider {

        /** Serializable UID. */
        private static final long serialVersionUID = -5121444553818793467L;

        /** {@inheritDoc} */
        @Override
        public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
            return StelaAbstractPropagator.this.propagateSpacecraftState(date).getPVCoordinates(frame);
        }

        /** {@inheritDoc} */
        @Override
        public Frame getNativeFrame(final AbsoluteDate date) throws PatriusException {
            return StelaAbstractPropagator.this.getFrame();
        }
    }

    /** {@link BoundedPropagator} view of the instance. */
    private class BoundedPropagatorView extends StelaAbstractPropagator implements BoundedPropagator {

        /** Serializable UID. */
        private static final long serialVersionUID = -3340036098040553110L;

        /** Min date. */
        private final AbsoluteDate minDate;

        /** Max date. */
        private final AbsoluteDate maxDate;

        /**
         * Simple constructor.
         *
         * @param startDateIn
         *        start date of the propagation
         * @param endDate
         *        end date of the propagation
         */
        public BoundedPropagatorView(final AbsoluteDate startDateIn, final AbsoluteDate endDate) {
            super(StelaAbstractPropagator.this.getAttitudeProviderForcesLocal(), StelaAbstractPropagator.this
                .getAttitudeProviderEventsLocal(), StelaAbstractPropagator.this.interpolator);
            if (startDateIn.compareTo(endDate) <= 0) {
                this.minDate = startDateIn;
                this.maxDate = endDate;
            } else {
                this.minDate = endDate;
                this.maxDate = startDateIn;
            }
        }

        /** {@inheritDoc} */
        @Override
        public AbsoluteDate getMinDate() {
            return this.minDate;
        }

        /** {@inheritDoc} */
        @Override
        public AbsoluteDate getMaxDate() {
            return this.maxDate;
        }

        /**
         * {@inheritDoc}
         *
         * @throws PatriusException
         */
        @Override
        protected SpacecraftState propagateSpacecraftState(final AbsoluteDate target) throws PatriusException {
            return StelaAbstractPropagator.this.propagateSpacecraftState(target);
        }

        /** {@inheritDoc} */
        @Override
        public double getMass(final AbsoluteDate date) throws PropagationException {
            return StelaAbstractPropagator.this.getMass(date);
        }

        /** {@inheritDoc} */
        @Override
        public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
            return this.propagate(date).getPVCoordinates(frame);
        }

        /** {@inheritDoc} */
        @Override
        public void resetInitialState(final SpacecraftState state) throws PropagationException {
            StelaAbstractPropagator.this.resetInitialState(state);
        }

        /** {@inheritDoc} */
        @Override
        public SpacecraftState getInitialState() throws PatriusException {
            return StelaAbstractPropagator.this.getInitialState();
        }

        /** {@inheritDoc} */
        @Override
        protected List<PatriusStepHandler> getStepHandlers() {
            // No step-handlers to add
            return new ArrayList<>();
        }

        /** {@inheritDoc} */
        @Override
        public Frame getNativeFrame(final AbsoluteDate date) throws PatriusException {
            return StelaAbstractPropagator.this.getFrame();
        }
    }
}
