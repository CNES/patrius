/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
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
 * HISTORY
 * VERSION:4.13:FA:FA-45:08/12/2023:[PATRIUS]Probleme de detection d'evenement
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.6:DM:DM-2571:27/01/2021:[PATRIUS] Integrateur Stormer-Cowell 
 * VERSION:4.3:FA:FA-2079:15/05/2019:Non suppression de detecteurs d'evenements en fin de propagation
 * VERSION:4.3:FA:FA-2116:15/05/2019:[PATRIUS] plantage lors du calcul des evenements
 * VERSION:4.3:DM:DM-2089:15/05/2019:[PATRIUS] passage a Java 8
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:189:18/12/2013:Cancelled changes made by previous commit
 * VERSION::FA:105:21/11/2013: class modified to solve a bug in the events detection
 * mechanism when a reset state is called.
 * VERSION::DM:208:05/08/2014: one shot event detector
 * VERSION::DM:226:12/09/2014: problem with event detections.
 * VERSION::DM:454:01/12/2015: RESET_STATE and REMOVE_DETECTOR
 * VERSION::FA:593:06/04/2016: corrected event detection (derivatives reinitialisation)
 * VERSION::FA:612:21/07/2016:Bug in same date events with Action.STOP
 * VERSION::FA:676:02/09/2016:Avoid recomputation of function g()
 * VERSION::FA:764:06/01/2017:Anomaly on events detection
 * VERSION::FA:1868:31/10/2018: handle proper end of integration
 * VERSION::FA:2079:29/01/2019: extend the Observable class and describe notification action when detector is removed
 * VERSION::FA:2116:15/04/2019: properly clear events at reset state
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.math.ode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.SortedSet;
import java.util.TreeSet;

import fr.cnes.sirius.patrius.math.analysis.solver.BracketingNthOrderBrentSolver;
import fr.cnes.sirius.patrius.math.analysis.solver.UnivariateSolver;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;
import fr.cnes.sirius.patrius.math.exception.NoBracketingException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.ode.events.EventHandler;
import fr.cnes.sirius.patrius.math.ode.events.EventState;
import fr.cnes.sirius.patrius.math.ode.sampling.AbstractStepInterpolator;
import fr.cnes.sirius.patrius.math.ode.sampling.StepHandler;
import fr.cnes.sirius.patrius.math.ode.sampling.StepInterpolator;
import fr.cnes.sirius.patrius.math.util.Incrementor;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Base class managing common boilerplate for all integrators.
 * 
 * <p>
 * The class extends {@link Observable} to be able to notify the possible observers of a change.
 * </p>
 * 
 * @version $Id: AbstractIntegrator.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.0
 */
@SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
public abstract class AbstractIntegrator extends Observable implements FirstOrderIntegrator {

    /** Step handler. */
    protected Collection<StepHandler> stepHandlers;

    /** Current step start time. */
    protected double stepStart;

    /** Current stepsize. */
    protected double stepSize;

    /** Indicator for last step. */
    protected boolean isLastStep;

    /** Indicator for last step handle: true is last step should be handled as such. */
    protected boolean lastStepHandle;

    /** Indicator that a state or derivative reset was triggered by some event. */
    protected boolean resetOccurred;

    /** Events states. */
    private final List<EventState> eventsStates;

    /** Initialization indicator of events states. */
    private boolean statesInitialized;

    /** Name of the method. */
    private final String name;

    /** Counter for number of evaluations. */
    private final Incrementor evaluations;

    /** Differential equations to integrate. */
    private transient ExpandableStatefulODE expandable;

    /**
     * Build an instance.
     * 
     * @param nameIn
     *        name of the method
     */
    public AbstractIntegrator(final String nameIn) {
        super();
        this.name = nameIn;
        this.stepHandlers = new ArrayList<>();
        this.stepStart = Double.NaN;
        this.stepSize = Double.NaN;
        this.eventsStates = new ArrayList<>();
        this.statesInitialized = false;
        this.evaluations = new Incrementor();
        this.setMaxEvaluations(-1);
        this.evaluations.resetCount();
        this.lastStepHandle = true;
    }

    /**
     * Build an instance with a null name.
     */
    protected AbstractIntegrator() {
        this(null);
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return this.name;
    }

    /** {@inheritDoc} */
    @Override
    public void addStepHandler(final StepHandler handler) {
        this.stepHandlers.add(handler);
    }

    /** {@inheritDoc} */
    @Override
    public Collection<StepHandler> getStepHandlers() {
        return Collections.unmodifiableCollection(this.stepHandlers);
    }

    /** {@inheritDoc} */
    @Override
    public void clearStepHandlers() {
        this.stepHandlers.clear();
    }

    /** {@inheritDoc} */
    @Override
    public void addEventHandler(final EventHandler handler,
                                final double maxCheckInterval,
                                final double convergence,
                                final int maxIterationCount) {
        this.addEventHandler(handler, maxCheckInterval, convergence,
            maxIterationCount,
            new BracketingNthOrderBrentSolver(convergence, 5));
    }

    /** {@inheritDoc} */
    @Override
    public void addEventHandler(final EventHandler handler,
                                final double maxCheckInterval,
                                final double convergence,
                                final int maxIterationCount,
                                final UnivariateSolver solver) {
        this.eventsStates.add(new EventState(handler, maxCheckInterval, convergence,
            maxIterationCount, solver));
    }

    /** {@inheritDoc} */
    @Override
    public Collection<EventHandler> getEventHandlers() {
        final List<EventHandler> list = new ArrayList<>();
        for (final EventState state : this.eventsStates) {
            list.add(state.getEventHandler());
        }
        return Collections.unmodifiableCollection(list);
    }

    /** {@inheritDoc} */
    @Override
    public void clearEventHandlers() {
        this.eventsStates.clear();
    }

    /** {@inheritDoc} */
    @Override
    public double getCurrentStepStart() {
        return this.stepStart;
    }

    /** {@inheritDoc} */
    @Override
    public double getCurrentSignedStepsize() {
        return this.stepSize;
    }

    /** {@inheritDoc} */
    @Override
    public void setMaxEvaluations(final int maxEvaluations) {
        this.evaluations.setMaximalCount((maxEvaluations < 0) ? Integer.MAX_VALUE : maxEvaluations);
    }

    /** {@inheritDoc} */
    @Override
    public int getMaxEvaluations() {
        return this.evaluations.getMaximalCount();
    }

    /** {@inheritDoc} */
    @Override
    public int getEvaluations() {
        return this.evaluations.getCount();
    }

    /**
     * Prepare the start of an integration.
     * 
     * @param t0
     *        start value of the independent <i>time</i> variable
     * @param y0
     *        array containing the start value of the state vector
     * @param t
     *        target time for the integration
     */
    protected void initIntegration(final double t0, final double[] y0, final double t) {

        this.evaluations.resetCount();

        for (final EventState state : this.eventsStates) {
            state.getEventHandler().init(t0, y0, t);
        }

        for (final StepHandler handler : this.stepHandlers) {
            handler.init(t0, y0, t);
        }

        this.setStateInitialized(false);

    }

    /**
     * Set the equations.
     * 
     * @param equations
     *        equations to set
     */
    protected void setEquations(final ExpandableStatefulODE equations) {
        this.expandable = equations;
    }

    /** {@inheritDoc} */
    @Override
    public double integrate(final FirstOrderDifferentialEquations equations,
                            final double t0, final double[] y0, final double t, final double[] y) {

        if (y0.length != equations.getDimension()) {
            throw new DimensionMismatchException(y0.length, equations.getDimension());
        }
        if (y.length != equations.getDimension()) {
            throw new DimensionMismatchException(y.length, equations.getDimension());
        }

        // prepare expandable stateful equations
        final ExpandableStatefulODE expandableODE = new ExpandableStatefulODE(equations);
        expandableODE.setTime(t0);
        expandableODE.setPrimaryState(y0);

        // perform integration
        this.integrate(expandableODE, t);

        // extract results back from the stateful equations
        System.arraycopy(expandableODE.getPrimaryState(), 0, y, 0, y.length);
        return expandableODE.getTime();

    }

    /**
     * Integrate a set of differential equations up to the given time.
     * <p>
     * This method solves an Initial Value Problem (IVP).
     * </p>
     * <p>
     * The set of differential equations is composed of a main set, which can be extended by some sets of secondary
     * equations. The set of equations must be already set up with initial time and partial states. At integration
     * completion, the final time and partial states will be available in the same object.
     * </p>
     * <p>
     * Since this method stores some internal state variables made available in its public interface during integration
     * ({@link #getCurrentSignedStepsize()}), it is <em>not</em> thread-safe.
     * </p>
     * 
     * @param equations
     *        complete set of differential equations to integrate
     * @param t
     *        target time for the integration
     *        (can be set to a value smaller than <code>t0</code> for backward integration)
     * @exception NumberIsTooSmallException
     *            if integration step is too small
     * @throws DimensionMismatchException
     *         if the dimension of the complete state does not
     *         match the complete equations sets dimension
     * @exception MaxCountExceededException
     *            if the number of functions evaluations is exceeded
     * @exception NoBracketingException
     *            if the location of an event cannot be bracketed
     */
    public abstract void integrate(ExpandableStatefulODE equations, double t);

    /**
     * Compute the derivatives and check the number of evaluations.
     * 
     * @param t
     *        current value of the independent <I>time</I> variable
     * @param y
     *        array containing the current value of the state vector
     * @param yDot
     *        placeholder array where to put the time derivative of the state vector
     * @exception MaxCountExceededException
     *            if the number of functions evaluations is exceeded
     * @exception DimensionMismatchException
     *            if arrays dimensions do not match equations settings
     */
    public void computeDerivatives(final double t, final double[] y, final double[] yDot) {
        this.evaluations.incrementCount();
        this.expandable.computeDerivatives(t, y, yDot);
    }

    /**
     * Set the stateInitialized flag.
     * <p>
     * This method must be called by integrators with the value {@code false} before they start integration, so a proper
     * lazy initialization is done automatically on the first step.
     * </p>
     * 
     * @param stateInitialized
     *        new value for the flag
     * @since 2.2
     */
    protected void setStateInitialized(final boolean stateInitialized) {
        this.statesInitialized = stateInitialized;
    }

    /**
     * Accept a step, triggering events and step handlers.
     * 
     * @param interpolator
     *        step interpolator
     * @param y
     *        state vector at step end time, must be reset if an event
     *        asks for resetting or if an events stops integration during the step
     * @param yDot
     *        placeholder array where to put the time derivative of the state vector
     * @param tEnd
     *        final integration time
     * @return time at end of step
     * @exception MaxCountExceededException
     *            if the interpolator throws one because
     *            the number of functions evaluations is exceeded
     * @exception NoBracketingException
     *            if the location of an event cannot be bracketed
     * @exception DimensionMismatchException
     *            if arrays dimensions do not match equations settings
     * @since 2.2
     */
    // CHECKSTYLE: stop IllegalType check
    // CHECKSTYLE: stop MethodLength check
    // CHECKSTYLE: stop CyclomaticComplexity check
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    protected double acceptStep(final AbstractStepInterpolator interpolator,
                                // CHECKSTYLE: resume ReturnCount check
                                // CHECKSTYLE: resume IllegalType check
                                // CHECKSTYLE: resume MethodLength check
                                // CHECKSTYLE: resume CyclomaticComplexity check
                                final double[] y, final double[] yDot, final double tEnd) {

        // Initialization
        double previousT = interpolator.getGlobalPreviousTime();
        final double currentT = interpolator.getGlobalCurrentTime();

        // initialize the events states if needed
        if (!this.statesInitialized) {
            for (final EventState state : this.eventsStates) {
                state.reinitializeBegin(interpolator);
            }
            this.statesInitialized = true;
        }

        // search for next events that may occur during the step
        final int orderingSign = interpolator.isForward() ? +1 : -1;
        final SortedSet<EventState> occuringEvents = new TreeSet<>(new Comparator<EventState>(){

            /** {@inheritDoc} */
            @Override
            public int compare(final EventState es0, final EventState es1) {
                int rez = orderingSign * Double.compare(es0.getEventTime(), es1.getEventTime());
                if (rez == 0) {
                    // First event is considered in case of identical date
                    rez = 1;
                }
                return rez;
            }
        });

        for (final EventState state : this.eventsStates) {
            if (state.evaluateStep(interpolator)) {
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
            final double eventT = currentEvent.getEventTime();
            interpolator.setSoftPreviousTime(previousT);
            interpolator.setSoftCurrentTime(eventT);

            // trigger the event
            interpolator.setInterpolatedTime(eventT);
            final double[] eventY = interpolator.getInterpolatedState().clone();

            // Specific case: event not treated if end of propagation
            this.isLastStep = (eventT == tEnd);
            if (!this.isLastStep) {
                // g value cache must be recomputed before calling stepAccepted() in that case
                currentEvent.stepAccepted(eventT, eventY);
                this.isLastStep = currentEvent.stop();
            }
            final boolean isLastDetection = currentEvent.removeDetector();

            // handle the first part of the step, up to the event
            for (final StepHandler handler : this.stepHandlers) {
                handler.handleStep(interpolator, this.isLastStep);
            }

            if (this.isLastStep) {
                // the event asked to stop integration
                if (isLastDetection) {
                    // the current event is removed from the event list
                    this.removeEventState(currentEvent);
                }
                System.arraycopy(eventY, 0, y, 0, y.length);
                return eventT;
            }

            if (currentEvent.isPendingReset()) {
                // If a reset is about to occur: check we didn't miss any event beforehand on interval [t0,
                // t(reset_state)]
                // Event could have been be missed because of maxCheck threshold
                boolean found = false;
                occuringEvents.clear();
                for (final EventState state : this.eventsStates) {
                    if ((interpolator.isForward() && state.getT0() > eventT) ||
                        (!interpolator.isForward() && state.getT0() < eventT)) {
                        // Particular case of event with slope selection != 2
                        // In that case t0 > eventT means we skipped an event because of slope selection
                        // In that case going backward in time requires to go back to beginning of step
                        final StepInterpolator interpolator2 = interpolator.copy();
                        interpolator2.setInterpolatedTime(previousT);
                        state.storeState(previousT, interpolator2.getInterpolatedState(), true);
                    }
                    // Event too close to be detected
                    final boolean closeEvent = !Double.isNaN(state.getPreviousEventTime())
                            && MathLib.abs(state.getPreviousEventTime() - eventT) <= state.getConvergence();

                    if (!state.equals(currentEvent) && !closeEvent && state.evaluateStep(interpolator)) {
                        // A missed event has been found during the reduced step
                        // If event occurs exactly at reset_state event time: it should not be considered since this
                        // event
                        // will be treated after reset state
                        if (MathLib.abs(state.getEventTime() - eventT) > state.getConvergence()) {
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

            if (currentEvent.reset(eventT, eventY)) {
                this.resetOccurred = true;

                // Check events just after event date and update
                // We cannot use the interpolator which is no longer up-to-date
                for (int i = 0; i < this.eventsStates.size(); i++) {
                    final EventState state = this.eventsStates.get(i);
                    if ((!state.equals(currentEvent)) && (state.evaluateStep(eventT, eventY))) {

                        // Trigger the event and reset state if necessary
                        state.stepAccepted(eventT, eventY);

                        // Treat ACTION.RESET_STATE
                        state.reset(eventT, eventY);

                        // Treat last detection
                        if (state.removeDetector()) {
                            this.removeEventState(state);
                        }

                        // Treat Action.STOP
                        this.isLastStep = state.stop();
                        if (this.isLastStep) {
                            if (isLastDetection) {
                                // the current event is removed from the event list
                                this.removeEventState(currentEvent);
                            }
                            System.arraycopy(eventY, 0, y, 0, y.length);
                            return eventT;
                        }
                    }
                }

                // Store all event states
                for (final EventState state : this.eventsStates) {
                    state.storeState(eventT, eventY, false);
                }

                // some event handler has triggered changes that
                // invalidate the derivatives, we need to recompute them
                System.arraycopy(eventY, 0, y, 0, y.length);
                this.computeDerivatives(eventT, y, yDot);

                // Reinitialize interpolator
                interpolator.reinitialize(y, interpolator.isForward(), this.expandable.getPrimaryMapper(),
                    this.expandable.getSecondaryMappers());

                if (isLastDetection) {
                    // the current event is removed from the event list
                    this.removeEventState(currentEvent);
                }

                return eventT;
            }

            // prepare handling of the remaining part of the step
            previousT = eventT;
            interpolator.setSoftPreviousTime(eventT);
            interpolator.setSoftCurrentTime(currentT);

            // check if the same event occurs again in the remaining part of the step
            if (!isLastDetection && currentEvent.evaluateStep(interpolator)) {
                // the event occurs during the current step
                occuringEvents.add(currentEvent);
            }

            if (isLastDetection) {
                // the current event is removed from the event list
                this.removeEventState(currentEvent);
            }
        }

        interpolator.setInterpolatedTime(currentT);

        final double[] currentY = interpolator.getInterpolatedState();
        for (final EventState state : this.eventsStates) {
            state.stepAccepted(currentT, currentY);
            this.isLastStep = this.isLastStep || state.stop();
        }
        this.isLastStep = this.isLastStep || (currentT == tEnd);

        // handle the remaining part of the step, after all events if any
        for (final StepHandler handler : this.stepHandlers) {
            handler.handleStep(interpolator, this.isLastStep && this.lastStepHandle);
        }

        return currentT;

    }

    /**
     * Check the integration span.
     * 
     * @param equations
     *        set of differential equations
     * @param t
     *        target time for the integration
     * @exception NumberIsTooSmallException
     *            if integration span is too small
     * @exception DimensionMismatchException
     *            if adaptive step size integrators
     *            tolerance arrays dimensions are not compatible with equations settings
     */
    protected void sanityChecks(final ExpandableStatefulODE equations, final double t) {

        final double threshold = 1000 * MathLib.ulp(MathLib.max(MathLib.abs(equations.getTime()),
            MathLib.abs(t)));
        final double dt = MathLib.abs(equations.getTime() - t);
        if (dt <= threshold) {
            throw new NumberIsTooSmallException(PatriusMessages.TOO_SMALL_INTEGRATION_INTERVAL,
                dt, threshold, false);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void handleLastStep(final boolean handleLastStep) {
        this.lastStepHandle = handleLastStep;
    }

    /**
     * Removed the specified event from the event list and notify the observers (if they exist) of
     * this change.
     * 
     * @param eventState event state to remove
     **/
    public void removeEventState(final EventState eventState) {
        this.eventsStates.remove(eventState);
        this.setChanged();
        final EventHandler eventHandler = eventState.getEventHandler();
        this.notifyObservers(eventHandler);
    }

    /**
     * Update step size to avoid overshoot.
     * @param start step start
     * @param end step end
     * @param size step size
     * @param forward integration direction
     * @return step size such as start + size <= end
     */
    protected static double avoidOvershoot(final double start, final double end, final double size,
                                           final boolean forward) {
        double updatedSize = size;
        if (start + size != end) {
            // These double condition is necessary for non-symmetry of "<=" condition between forward and backward cases
            if ((start + size <= end) ^ forward) {
                // Overshoot
                final double direction = forward ? size - 1 : size + 1;
                updatedSize = MathLib.nextAfter(size, direction);
            }
        }
        return updatedSize;
    }
}
