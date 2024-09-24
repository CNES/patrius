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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3184:10/05/2022:[PATRIUS] Non detection d'evenement pour une propagation de duree tres courte
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:189:18/12/2013:Cancelled changes made by previous commit
 * VERSION::DM:190:29/07/2014:Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:208:05/08/2014: one shot event detector
 * VERSION::DM:226:12/09/2014: problem with event detections.
 * VERSION::FA:374:08/01/2015: wrong eclipse detection.
 * VERSION::FA:676:02/09/2016:Avoid recomputation of function g()
 * VERSION::FA:764:06/01/2017:Anomaly on events detection
 * VERSION::FA:1306:06/09/2017:Correct numerical quality issue in method evaluateStep()
 * VERSION::FA:1868:31/10/2018: handle proper end of integration
 * VERSION::FA:1976:04/12/2018:Anomaly on events detection
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.math.ode.events;

import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.solver.AllowedSolution;
import fr.cnes.sirius.patrius.math.analysis.solver.BracketedUnivariateSolver;
import fr.cnes.sirius.patrius.math.analysis.solver.PegasusSolver;
import fr.cnes.sirius.patrius.math.analysis.solver.UnivariateSolver;
import fr.cnes.sirius.patrius.math.analysis.solver.UnivariateSolverUtils;
import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;
import fr.cnes.sirius.patrius.math.exception.NoBracketingException;
import fr.cnes.sirius.patrius.math.ode.sampling.StepInterpolator;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * This class handles the state for one {@link EventHandler
 * event handler} during integration steps.
 * 
 * <p>
 * Each time the integrator proposes a step, the event handler switching function should be checked.
 * This class handles the state of one handler during one integration step, with references to the
 * state at the end of the preceding step. This information is used to decide if the handler should
 * trigger an event or not during the proposed step.
 * </p>
 * 
 * @version $Id: EventState.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 1.2
 */
public class EventState {

    /** Event handler. */
    private final EventHandler handler;

    /** Maximal time interval between events handler checks. */
    private final double maxCheckInterval;

    /** Convergence threshold for event localization. */
    private final double convergence;

    /** Upper limit in the iteration count for event localization. */
    private final int maxIterationCount;

    /**
     * Convergence threshold for event localization for the current step.
     * Is equal to convergence, unless the integration step is too small.
     * */
    private double stepConvergence;

    /** Time at the beginning of the propagation. */
    private double initialDate;

    /** Time at the beginning of the step. */
    private double t0;

    /** Value of the events handler at the beginning of the step. */
    private double g0;

    /** Value of the events handler at the beginning of the step. */
    private double g0Old;

    /** Indicator of event expected during the step. */
    private boolean pendingEvent;

    /** Occurrence time of the pending event. */
    private double pendingEventTime;

    /** Occurrence time of the previous event. */
    private double previousEventTime;

    /** Integration direction. */
    private boolean forward;

    /**
     * Variation direction around pending event.
     * True: g function is going up, false: it is going down.
     * This boolean is only kept to record slope of g function in case of event at t = 0.
     */
    private boolean increasing;

    /** Next action indicator. */
    private EventHandler.Action nextAction;

    /** True if detector should be removed. */
    private boolean remove = false;

    /** Root-finding algorithm to use to detect state events. */
    private final UnivariateSolver solver;

    /**
     * Simple constructor.
     * 
     * @param handlerIn
     *        event handler
     * @param maxCheckIntervalIn
     *        maximal time interval between switching
     *        function checks (this interval prevents missing sign changes in
     *        case the integration steps becomes very large)
     * @param convergenceIn
     *        convergence threshold in the event time search
     * @param maxIterationCountIn
     *        upper limit of the iteration count in
     *        the event time search
     * @param solverIn
     *        Root-finding algorithm to use to detect state events
     */
    public EventState(final EventHandler handlerIn, final double maxCheckIntervalIn,
            final double convergenceIn, final int maxIterationCountIn,
            final UnivariateSolver solverIn) {
        super();
        this.handler = handlerIn;
        this.maxCheckInterval = maxCheckIntervalIn;
        this.convergence = MathLib.abs(convergenceIn);
        this.maxIterationCount = maxIterationCountIn;
        this.solver = solverIn;

        // Step convergence initialization
        this.stepConvergence = convergenceIn;
        // some dummy values ...
        this.t0 = Double.NaN;
        this.g0 = Double.NaN;
        this.pendingEvent = false;
        this.pendingEventTime = Double.NaN;
        this.previousEventTime = Double.NaN;
        this.nextAction = EventHandler.Action.CONTINUE;
    }

    /**
     * Get the underlying event handler.
     * 
     * @return underlying event handler
     */
    public EventHandler getEventHandler() {
        return this.handler;
    }

    /**
     * Get the maximal time interval between events handler checks.
     * 
     * @return maximal time interval between events handler checks
     */
    public double getMaxCheckInterval() {
        return this.maxCheckInterval;
    }

    /**
     * Get the convergence threshold for event localization.
     * 
     * @return convergence threshold for event localization
     */
    public double getConvergence() {
        return this.stepConvergence;
    }

    /**
     * Get the upper limit in the iteration count for event localization.
     * 
     * @return upper limit in the iteration count for event localization
     */
    public int getMaxIterationCount() {
        return this.maxIterationCount;
    }

    /**
     * Reinitialize the beginning of the step.
     * 
     * @param interpolator
     *        valid for the current step
     * @exception MaxCountExceededException
     *            if the interpolator throws one because
     *            the number of functions evaluations is exceeded
     */
    public void reinitializeBegin(final StepInterpolator interpolator) {

        this.t0 = interpolator.getPreviousTime();
        interpolator.setInterpolatedTime(this.t0);
        this.g0 = this.handler.g(this.t0, interpolator.getInterpolatedState());
        // If g is equal to 0 at the beginning of propagation, an event will be detected
        // and g0 will be set to +inf or -inf depending on increasing value of g
        if (this.g0 > 0) {
            this.g0 = Double.POSITIVE_INFINITY;
        } else if (this.g0 < 0) {
            this.g0 = Double.NEGATIVE_INFINITY;
        }
        this.initialDate = this.t0;
    }

    /**
     * Check if some reset is being triggered.
     * 
     * @return true is some reset is being triggered
     */
    public boolean isPendingReset() {
        return (this.nextAction == EventHandler.Action.RESET_STATE)
                || (this.nextAction == EventHandler.Action.RESET_DERIVATIVES);
    }

    /**
     * Store event state with provided time and state.
     * 
     * @param t
     *        current time
     * @param y
     *        current state
     * @param forceUpdate
     *        force update of event parameters
     */
    public void storeState(final double t, final double[] y, final boolean forceUpdate) {
        this.t0 = t;
        if (forceUpdate) {
            // In that case, g0 may not be up-to-date: recompute it
            this.g0Old = this.g0;
            this.g0 = this.handler.g(t, y);
            if (this.g0 > 0) {
                this.g0 = Double.POSITIVE_INFINITY;
            } else if (this.g0 < 0) {
                this.g0 = Double.NEGATIVE_INFINITY;
            }
            this.previousEventTime = Double.NaN;
        }
    }

    /**
     * Evaluate the impact of the proposed step on the event handler.
     * In that case, the step is of null size (the provided time should be equal to t0)
     * 
     * @param t
     *        current time
     * @param y
     *        current state
     * @return true if the event handler triggers an event before
     *         the end of the proposed step
     */
    public boolean evaluateStep(final double t, final double[] y) {

        // Update bounds
        final double ga = this.g0;
        final double gb = this.handler.g(t, y);

        // Discard sign change if correspond to an already treated event at exactly the same date
        final boolean pastEvent = (this.previousEventTime == t);

        // Detect sign change
        boolean signChange = false;
        if (!pastEvent) {

            final boolean wasPendingEvent = this.pendingEvent
                    && (MathLib.abs(this.pendingEventTime - t) <= this.stepConvergence);

            if (wasPendingEvent) {
                // An event was pending:
                // To avoid to detect cancelled event, we must check sign of detector at the
                // beginning of the main step
                // rather than at t-
                signChange = (this.g0Old >= 0) ^ (gb >= 0);
            } else {
                // No event pending: classic way of checking events
                signChange = (ga >= 0) ^ (gb >= 0);
            }

            if (signChange) {
                // Sign change
                this.increasing = wasPendingEvent ? gb >= this.g0Old : gb >= ga;

                // Take into account slope selection and propagation direction
                final int slope = this.handler.getSlopeSelection();
                if (slope == 2 || ((this.forward ^ !this.increasing) ^ slope == 1)) {
                    this.pendingEventTime = t;
                    this.pendingEvent = true;
                }
            } else {
                // No sign change
                this.pendingEvent = false;
                this.pendingEventTime = Double.NaN;
            }
        }

        return this.pendingEvent;
    }

    /**
     * Evaluate the impact of the proposed step on the event handler.
     * 
     * @param interpolator
     *        step interpolator for the proposed step
     * @return true if the event handler triggers an event before
     *         the end of the proposed step
     * @exception MaxCountExceededException
     *            if the interpolator throws one because
     *            the number of functions evaluations is exceeded
     * @exception NoBracketingException
     *            if the event cannot be bracketed
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // CHECKSTYLE: stop ReturnCount check
    // CHECKSTYLE: stop MethodLength check
    // Reason: Commons-Math code kept as such
    @SuppressWarnings("PMD.ExceptionAsFlowControl")
    public boolean evaluateStep(final StepInterpolator interpolator) {
        // CHECKSTYLE: resume MethodLength check
        // CHECKSTYLE: resume ReturnCount check
        // CHECKSTYLE: resume CyclomaticComplexity check

        try {
            this.forward = interpolator.isForward();
            final double t1 = interpolator.getCurrentTime();
            final double dt = t1 - this.t0;
            final double absdt = MathLib.abs(dt);
            // The step convergence cannot be smaller
            // than the current step interval
            this.stepConvergence = MathLib.min(absdt, this.convergence);

            // Step on which events are searched is too small: root-finding algorithm cannot work
            if (absdt < Precision.DOUBLE_COMPARISON_EPSILON) {
                // Check if there is an event, if yes, start of step is returned
                // Per PATRIUS convention, event cannot be detected at end of step
                interpolator.setInterpolatedTime(t1);
                final double gb = this.handler.g(t1, interpolator.getInterpolatedState());
                final boolean signChange = ((g0 >= 0) && (gb < 0)) || ((g0 <= 0) && (gb > 0));
                if (absdt > 0 && signChange) {
                    // Event
                    this.pendingEventTime = this.t0;
                    this.pendingEvent = true;
                    return true;
                }
                // No event or interval is 0s (then event will be detected on start of next step)
                this.pendingEvent = false;
                this.pendingEventTime = Double.NaN;
                return false;
            }

            final UnivariateFunction f = new UnivariateFunction() {
                /** Serializable UID. */
                private static final long serialVersionUID = 3652585541755862494L;

                /** {@inheritDoc} */
                @Override
                public double value(final double t) {
                    try {
                        interpolator.setInterpolatedTime(t);
                        return EventState.this.handler.g(t, interpolator.getInterpolatedState());
                    } catch (final MaxCountExceededException mcee) {
                        throw new LocalMaxCountExceededException(mcee);
                    }
                }
            };

            final int n = MathLib.max(1, (int) MathLib.ceil(absdt / this.maxCheckInterval));
            final double h = dt / n;

            // t00 variable used in case of untreated event because of slope selection: t0 is then
            // changed for later
            // whereas it should stay idle for the remaining part of the algorithm
            final double t00 = this.t0;

            double ta = this.t0;
            double ga = this.g0;
            for (int i = 0; i < n; ++i) {

                // evaluate handler value at the end of the substep
                double tb = t00 + (i + 1) * h;
                if (i == n - 1) {
                    // Due to numerical quality issues, at last step tb should exactly match t1
                    // (FT-1306)
                    tb = t1;
                }

                interpolator.setInterpolatedTime(tb);
                final double gb = this.handler.g(tb, interpolator.getInterpolatedState());

                // Specific case: event at first date
                final boolean eventAtFirstStep = (this.g0 == 0) && (t00 == this.initialDate)
                        && (ta == t00);

                // check events occurrence
                if ((ga >= 0) ^ (gb >= 0) || eventAtFirstStep) {
                    // there is a sign change: an event is expected during this step
                    // variation direction, with respect to the integration direction

                    this.increasing = gb >= ga;
                    final int slope = this.handler.getSlopeSelection();
                    if (slope == 2 || ((this.forward ^ !this.increasing) ^ slope == 1)) {

                        // find the event time making sure we select a solution just at or past the
                        // exact root
                        double root = ta;
                        final double ga2 = f.value(ta);

                        if ((ga2 >= 0) ^ (gb >= 0)) {
                            if (this.solver instanceof BracketedUnivariateSolver<?>) {
                                @SuppressWarnings("unchecked")
                                final BracketedUnivariateSolver<UnivariateFunction> bracketing =
                                    (BracketedUnivariateSolver<UnivariateFunction>) this.solver;
                                root = this.forward ? bracketing.solve(this.maxIterationCount, f,
                                        ta, tb, AllowedSolution.RIGHT_SIDE) : bracketing.solve(
                                        this.maxIterationCount, f, tb, ta,
                                        AllowedSolution.LEFT_SIDE);
                            } else {
                                final double baseRoot = this.forward ? this.solver.solve(
                                        this.maxIterationCount, f, ta, tb) : this.solver.solve(
                                        this.maxIterationCount, f, tb, ta);
                                final int remainingEval = this.maxIterationCount
                                        - this.solver.getEvaluations();
                                final BracketedUnivariateSolver<UnivariateFunction> bracketing = new PegasusSolver(
                                        this.solver.getRelativeAccuracy(),
                                        this.solver.getAbsoluteAccuracy());
                                root = this.forward ? UnivariateSolverUtils.forceSide(
                                        remainingEval, f, bracketing, baseRoot, ta, tb,
                                        AllowedSolution.RIGHT_SIDE) : UnivariateSolverUtils
                                        .forceSide(remainingEval, f, bracketing, baseRoot, tb, ta,
                                                AllowedSolution.LEFT_SIDE);
                            }
                        }
                        if ((!Double.isNaN(this.previousEventTime))
                                && (MathLib.abs(root - ta) <= this.stepConvergence)
                                && (MathLib.abs(root - this.previousEventTime) <= this.stepConvergence)) {
                            // we have either found nothing or found (again ?) a past event,
                            // retry the substep excluding this value
                            ta = this.forward ? ta + this.stepConvergence : ta
                                    - this.stepConvergence;
                            ga = f.value(ta);

                            // Update reference event state (useful only for current algorithm
                            // consistency since it is
                            // updated later anyway)
                            // g0 = ga;

                            // CHECKSTYLE: stop ModifiedControlVariable check
                            // Reason: Commons-Math code kept as such
                            --i;
                            // CHECKSTYLE: resume ModifiedControlVariable check

                        } else if (Double.isNaN(this.previousEventTime)
                                || (MathLib.abs(this.previousEventTime - root) > this.stepConvergence)) {
                            this.pendingEventTime = root;
                            this.pendingEvent = true;
                            return true;

                        } else {
                            // no sign change: there is no event for now
                            ta = tb;
                            ga = gb;
                        }
                    } else {
                        // There is a sign change but must not be taken into account because of
                        // slope selection
                        ta = tb;
                        ga = gb;
                        // Update g0 however since sign of g has changed (= stepAccepted())
                        this.t0 = ta;
                        this.previousEventTime = this.t0;
                        this.increasing = !this.increasing;
                        this.g0Old = this.g0;
                        this.g0 = this.increasing ? Double.NEGATIVE_INFINITY
                                : Double.POSITIVE_INFINITY;
                    }
                } else {
                    // no sign change: there is no event for now
                    ta = tb;
                    ga = gb;
                }
            }

            // no event during the whole step
            this.pendingEvent = false;
            this.pendingEventTime = Double.NaN;
            return false;

        } catch (final LocalMaxCountExceededException lmcee) {
            throw lmcee.getException();
        }

    }

    /**
     * Get the occurrence time of the event triggered in the current step.
     * 
     * @return occurrence time of the event triggered in the current
     *         step or infinity if no events are triggered
     */
    public double getEventTime() {
        return this.pendingEvent ? this.pendingEventTime : (this.forward ? Double.POSITIVE_INFINITY
                : Double.NEGATIVE_INFINITY);
    }

    /**
     * Get previous event time.
     * 
     * @return previous event time
     */
    public double getPreviousEventTime() {
        return this.previousEventTime;
    }

    /**
     * Acknowledge the fact the step has been accepted by the integrator.
     * 
     * @param t
     *        value of the independent <i>time</i> variable at the
     *        end of the step
     * @param y
     *        array containing the current value of the state vector
     *        at the end of the step
     */
    public void stepAccepted(final double t, final double[] y) {

        // If there is an event, then g is increasing only if g0 (before the event) is negative
        // If g0 = 0, value of "increasing" has already been stored
        if (this.g0 != 0) {
            this.increasing = this.g0 < 0;
        }

        this.t0 = t;
        this.g0Old = this.g0;

        if (this.pendingEvent && (MathLib.abs(this.pendingEventTime - t) <= this.stepConvergence)) {
            // force the sign to its value "just after the event"
            this.previousEventTime = t;
            this.nextAction = this.handler.eventOccurred(t, y, this.increasing, this.forward);
            this.remove = this.handler.shouldBeRemoved();
            this.g0Old = this.g0;
            this.g0 = this.increasing ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
        } else {
            this.nextAction = EventHandler.Action.CONTINUE;
        }
    }

    /**
     * Cancel stepAccepted call (does not cancel event).
     * This method is used only when some missed event have occurred: event search algorithm goes
     * backward in time,
     * rewriting the future: stepAccepted() call leading to this jump in the past needs to be
     * canceled.
     */
    public void cancelStepAccepted() {
        this.increasing = !this.increasing;
        this.g0Old = this.g0;
        this.g0 = this.increasing ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
        this.previousEventTime = Double.NaN;
    }

    /**
     * Getter for t0.
     * 
     * @return t0
     */
    public double getT0() {
        return this.t0;
    }

    /**
     * Check if the integration should be stopped at the end of the
     * current step.
     * 
     * @return true if the integration should be stopped
     */
    public boolean stop() {
        return this.nextAction == EventHandler.Action.STOP;
    }

    /**
     * Check the current detector should be removed at the end of the current step
     * current step.
     * 
     * @return true if the detector should be removed
     */
    public boolean removeDetector() {
        return this.remove;
    }

    /**
     * Let the event handler reset the state if it wants.
     * 
     * @param t
     *        value of the independent <i>time</i> variable at the
     *        beginning of the next step
     * @param y
     *        array were to put the desired state vector at the beginning
     *        of the next step
     * @return true if the integrator should reset the derivatives too
     */
    public boolean reset(final double t, final double[] y) {

        if (!(this.pendingEvent && (MathLib.abs(this.pendingEventTime - t) <= this.stepConvergence))) {
            return false;
        }

        if (this.nextAction == EventHandler.Action.RESET_STATE) {
            this.handler.resetState(t, y);
        }
        this.pendingEvent = false;
        this.pendingEventTime = Double.NaN;

        return (this.nextAction == EventHandler.Action.RESET_STATE)
                || (this.nextAction == EventHandler.Action.RESET_DERIVATIVES);

    }

    /** Local wrapper to propagate exceptions. */
    private static class LocalMaxCountExceededException extends RuntimeException {

        /** Serializable UID. */
        private static final long serialVersionUID = 20120901L;

        /** Wrapped exception. */
        private final MaxCountExceededException wrapped;

        /**
         * Simple constructor.
         * 
         * @param exception
         *        exception to wrap
         */
        public LocalMaxCountExceededException(final MaxCountExceededException exception) {
            super();
            this.wrapped = exception;
        }

        /**
         * Get the wrapped exception.
         * 
         * @return wrapped exception
         */
        public MaxCountExceededException getException() {
            return this.wrapped;
        }

    }

}
