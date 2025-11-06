/**
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
 * VERSION:4.14:OPENFD-292:22/08/2024: Implementation de multi-propagateurs mixtes
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.13:FA:FA-79:08/12/2023:[PATRIUS] Probleme dans la fonction g de LocalTimeAngleDetector
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3184:10/05/2022:[PATRIUS] Non detection d'evenement pour une propagation de duree tres courte
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:FA:FA-2441:27/05/2020:Calcul evenement, date relatives
 * VERSION:4.4:FA:FA-2121:04/10/2019:[PATRIUS] precision de la methode shiftedBy de AbsoluteDate
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:189:18/12/2013:Cancelled changes made by previous commit
 * VERSION::FA:105:21/11/2013: class modified to solve a bug in the events detection
 * mechanism when a reset state is called.
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:208:05/08/2014: one shot event detector
 * VERSION::DM:226:12/09/2014: problem with event detections.
 * VERSION::FA:374:08/01/2015: wrong eclipse detection.
 * VERSION::DM:454:24/11/2015:Add method shouldBeRemoved() to manage detector suppression
 * VERSION::FA:558:25/02/2016:Correction of algorithm for simultaneous events detection
 * VERSION::FA:764:06/01/2017:Anomaly on events detection
 * VERSION::FA:1976:04/12/2018:Anomaly on events detection
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events.utils;

import java.io.Serializable;

import fr.cnes.sirius.patrius.events.EventDetector;
import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.solver.AllowedSolution;
import fr.cnes.sirius.patrius.math.analysis.solver.BracketedUnivariateSolver;
import fr.cnes.sirius.patrius.math.analysis.solver.BracketingNthOrderBrentSolver;
import fr.cnes.sirius.patrius.math.analysis.solver.BrentSolver;
import fr.cnes.sirius.patrius.math.analysis.solver.PegasusSolver;
import fr.cnes.sirius.patrius.math.analysis.solver.UnivariateSolver;
import fr.cnes.sirius.patrius.math.analysis.solver.UnivariateSolverUtils;
import fr.cnes.sirius.patrius.math.exception.NoBracketingException;
import fr.cnes.sirius.patrius.math.exception.TooManyEvaluationsException;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusStepInterpolator;
import fr.cnes.sirius.patrius.propagation.sampling.multi.MultiPatriusStepInterpolator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class handles the state for one {@link EventDetector event detector} during integration
 * steps.
 * 
 * <p>
 * This class is heavily based on the class with the same name from the Apache commons-math library. The changes
 * performed consist in replacing raw types (double and double arrays) with space dynamics types ({@link AbsoluteDate},
 * {@link SpacecraftState}).
 * </p>
 * <p>
 * Each time the propagator proposes a step, the event detector should be checked. This class handles the state of one
 * detector during one propagation step, with references to the state at the end of the preceding step. This information
 * is used to determine if the detector should trigger an event or not during the proposed step (and hence the step
 * should be reduced to ensure the event occurs at a bound rather than inside the step).
 * </p>
 * <p>
 * See <a href="https://www.orekit.org/forge/issues/110">Orekit issue 110</a> for more information. Default constructor
 * changed in order to instanciate a bracketing solver, to solve the bracketing exception.
 * </p>
 * 
 * @author Luc Maisonobe
 */
@SuppressWarnings("PMD.NullAssignment")
public class EventState implements Serializable {

     /** Serializable UID. */
    private static final long serialVersionUID = 4489391420715269318L;
    
    /** Spacecraft related to the detector and event. */
    private final String spacecraftId;

    /** Event detector. */
    private final EventDetector detector;

    /** Time at the beginning of the propagation. */
    private AbsoluteDate initialDate;

    /** Time at the beginning of the step. */
    private AbsoluteDate t0;

    /** Time at the beginning of the step. */
    private AbsoluteDate t00;
    
    /** Time at the beginning of the step. */
    private AbsoluteDate ta;

    /** Value of the event detector at the beginning of the step. */
    private double g0;

    /** Value of the event detector at the beginning of the step. */
    private double g0Old;
    
    /** Value of the event detector at the beginning of the step. */
    private double ga;

    /** Indicator of event expected during the step. */
    private boolean pendingEvent;

    /** Occurrence time of the pending event. */
    private AbsoluteDate pendingEventTime;

    /** Occurrence time of the previous event. */
    private AbsoluteDate previousEventTime;

    /** Integration direction. */
    private boolean forward;

    /**
     * Variation direction around pending event.
     * True: g function is going up, false: it is going down.
     * This boolean is only kept to record slope of g function in case of event at t = 0.
     */
    private boolean increasing;

    /** Next action indicator. */
    private EventDetector.Action nextAction;

    /** True if detector should be removed. */
    private boolean remove;

    /** Root-finding algorithm to use to detect state events. */
    private final UnivariateSolver solver;

    /**
     * Constructor allowing the user to provide the solver used in switch detection and an identifier for the spacecraft
     * whose state is used by the detector.
     * 
     * @param detectorIn
     *        monitored event detector
     * @param solverIn
     *        the UnivariateSolver used in switch detection
     * @param spacecraftId
     *        identifier of the spacecraft (can be null)
     */
    public EventState(final EventDetector detectorIn, final UnivariateSolver solverIn, final String spacecraftId) {
        super();
        this.detector = detectorIn;
        this.spacecraftId = spacecraftId;

        // some dummy values ...
        this.t0 = null;
        this.g0 = Double.NaN;
        this.pendingEvent = false;
        this.pendingEventTime = null;
        this.previousEventTime = null;
        this.nextAction = EventDetector.Action.CONTINUE;
        this.remove = false;
        // initialize the 5th order solver with convergence threshold asked by
        // the event detector
        this.solver = solverIn;
    }

    /**
     * Simple constructor. The default solver used in switch detection is the Brent solver and no identifier is provided
     * for the spacecraft.
     * 
     * @param detectorIn
     *        monitored event detector
     */
    public EventState(final EventDetector detectorIn) {
        this(detectorIn, null);
    }
    
    /**
     * Constructor allowing the user to provide an identifier for the spacecraft whose state is used by the detector.
     * The default solver used in switch detection is the Brent solver.
     * 
     * @param detectorIn
     *        monitored event detector
     * @param spacecraftId
     *        identifier of the spacecraft (can be null)
     */
    public EventState(final EventDetector detectorIn, final String spacecraftId) {
        this(detectorIn, new BracketingNthOrderBrentSolver(detectorIn.getThreshold(), 5), spacecraftId);
    }
    
    /**
     * Get the identifier of the spacecraft.
     * 
     * @return the identifier of the spacecraft
     */
    public String getSpacecraftId() {
        return this.spacecraftId;
    }

    /**
     * Get the underlying event detector.
     * 
     * @return underlying event detector
     */
    public EventDetector getEventDetector() {
        return this.detector;
    }

    /**
     * Reinitialize the beginning of the step.
     * 
     * @param state0
     *        state value at the beginning of the step
     * @exception PatriusException
     *            if the event detector value cannot be evaluated at the beginning of the step
     */
    public void reinitializeBegin(final SpacecraftState state0) throws PatriusException {
        this.t0 = state0.getDate();
        this.g0 = this.detector.g(state0);
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
        return this.nextAction == EventDetector.Action.RESET_STATE;
    }

    /**
     * Reinitialize event state with provided time and state.
     * 
     * @param state
     *        SpacecraftState current state
     * @param forceUpdate
     *        force update of event parameters
     * @throws PatriusException
     *         SpacecraftState
     */
    public void storeState(final SpacecraftState state,
                           final boolean forceUpdate) throws PatriusException {
        this.t0 = state.getDate();
        if (forceUpdate) {
            // In that case, g0 may not be up-to-date: recompute it
            this.g0Old = this.g0;
            this.g0 = this.detector.g(state);
            if (this.g0 > 0) {
                this.g0 = Double.POSITIVE_INFINITY;
            } else if (this.g0 < 0) {
                this.g0 = Double.NEGATIVE_INFINITY;
            }
            this.previousEventTime = null;
        }
    }

    /**
     * Evaluate the impact of the proposed step on the event handler.
     * In that case, the step is of null size (the provided date should be equal to t0)
     * 
     * @param state
     *        current state
     * @return true if the event handler triggers an event before
     *         the end of the proposed step
     * @throws PatriusException
     *         SpacecraftState
     */
    public boolean evaluateStep(final SpacecraftState state) throws PatriusException {

        // Update bounds
        final double gInitial = this.g0;
        final double gState = this.detector.g(state);

        // Discard sign change if correspond to an already treated event at exactly the same date
        final boolean pastEvent = (this.previousEventTime != null)
            && (this.previousEventTime.durationFrom(state.getDate()) == 0);

        // Detect sign change
        boolean signChange = false;
        if (!pastEvent) {

            final boolean wasPendingEvent = this.pendingEvent
                && (MathLib.abs(this.pendingEventTime.durationFrom(this.t0)) < this.detector.getThreshold());

            if (wasPendingEvent) {
                // An event was pending:
                // To avoid to detect cancelled event, we must check sign of detector at the beginning of the main step
                // rather than at t-
                signChange = (this.g0Old > 0) ^ (gState >= 0);
            } else {
                // No event pending:
                // Classic way of checking events
                signChange = (gInitial >= 0) ^ (gState >= 0);
            }

            if (signChange) {
                // Sign change
                if (wasPendingEvent) {
                    this.increasing = gState >= this.g0Old;
                } else {
                    this.increasing = gState >= gInitial;
                }
                
                // Take into account slope selection and propagation direction
                final int slope = this.detector.getSlopeSelection();
                if (slope == 2 || ((this.forward ^ !this.increasing) ^ slope == 1)) {
                    this.pendingEventTime = state.getDate();
                    this.pendingEvent = true;
                }
            } else {
                // No sign change
                this.pendingEvent = false;
                this.pendingEventTime = null;
            }
        }

        return this.pendingEvent;
    }

    /**
     * Evaluate the impact of the proposed step on the event detector.<br>
     * 
     * See <a href="https://www.orekit.org/forge/issues/110">Orekit issue 110</a> for more
     * information. Default
     * constructor changed in order to instanciate a bracketing solver, to solve the bracketing
     * exception.
     * 
     * @param interpolator
     *        step interpolator for the proposed step
     * @return true if the event detector triggers an event before the end of the proposed step
     *         (this implies the step
     *         should be rejected)
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
    @SuppressWarnings("PMD.ExceptionAsFlowControl")
    public boolean evaluateStep(final PatriusStepInterpolator interpolator) throws PatriusException {
        // CHECKSTYLE: resume MethodLength check
        // CHECKSTYLE: resume CyclomaticComplexity check
        // CHECKSTYLE: resume ReturnCount check

        try {

            final double convergence = this.detector.getThreshold();
            if (this.forward ^ interpolator.isForward()) {
                this.forward = !this.forward;
                this.pendingEvent = false;
                this.pendingEventTime = null;
                this.previousEventTime = null;
            }
            final AbsoluteDate t1 = interpolator.getCurrentDate();
            final double dt = t1.durationFrom(this.t0);
            if (MathLib.abs(dt) < convergence) {
                // we cannot do anything on such a small step, don't trigger any events
                // Check if there is an event, if yes, start of step is returned
                // Per PATRIUS convention, event cannot be detected at end of step
                interpolator.setInterpolatedDate(t1);
                final double gb = this.detector.g(interpolator.getInterpolatedState());
                final boolean signChange = ((g0 >= 0) && (gb < 0)) || ((g0 <= 0) && (gb > 0));
                if (dt > 0 && signChange) {
                    // Event
                    this.pendingEventTime = this.t0;
                    this.pendingEvent = true;
                    return true;
                } else {
                    // No event or interval is 0s (then event will be detected on start of next step)
                    this.pendingEventTime = null;
                    this.pendingEvent = false;
                    return false;
                }
            }
            final int n = MathLib.max(1,
                (int) MathLib.ceil(MathLib.abs(dt) / this.detector.getMaxCheckInterval()));
            final double h = dt / n;

            final UnivariateFunction f = new UnivariateFunction(){
                /** Serializable UID. */
                private static final long serialVersionUID = 197517831095184758L;

                /** {@inheritDoc} */
                @Override
                public double value(final double t) {
                    try {
                        final double nextUlp = new AbsoluteDate(t00, MathLib.nextAfter(t, forward
                                ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY)).durationFrom(t1);
                        final AbsoluteDate interpolatedDate;
                        if ((forward && nextUlp > 0) || (!forward && nextUlp < 0)) {
                            // In case the date is the last possible date before the upper interval bound: set it to
                            // last date since that comes from a numerical quality issue from AbsoluteDate.durationFrom
                            interpolatedDate = t1;
                        } else {
                            // Standard case
                            interpolatedDate = EventState.this.t00.shiftedBy(t, t1, forward);
                        }
                        interpolator.setInterpolatedDate(interpolatedDate);
                        return EventState.this.detector.g(interpolator.getInterpolatedState());
                    } catch (final PatriusException oe) {
                        // NOTEST: unreachable exception since g, setInterpolatedDate and getInterpolatedStates are all
                        // called in the method before f.value()
                        throw new LocalWrapperException(oe);
                    }
                }
            };

            final BrentSolver nonBracketing = new BrentSolver(convergence);
            final PegasusSolver bracketing = new PegasusSolver(convergence);
            final int maxIterationcount = this.detector.getMaxIterationCount();

            // t00 variable used in case of untreated event because of slope selection: t0 is then
            // changed for later
            // whereas it should stay idle for the remaining part of the algorithm
            this.t00 = this.t0;

            this.ta = this.t0;
            this.ga = this.g0;
            for (int i = 0; i < n; ++i) {

                // evaluate detector value at the end of the substep
                final AbsoluteDate tb;
                if (i < (n - 1)) {
                    tb = this.t00.shiftedBy((i + 1) * h, t1, forward);
                } else {
                    // CNES BUG A-1036
                    // Last step should be exactly t1.
                    // We force it because, even if shiftedBy is very accurate, it does not
                    // always fall exactly on t1 at the last step - and this is sometimes
                    // critical (for bounded propagators for instance).
                    tb = t1;
                }
                interpolator.setInterpolatedDate(tb);
                final double gb = this.detector.g(interpolator.getInterpolatedState());

                final int result =
                    this.evaluateStepInternal(gb, tb, t1, f, nonBracketing, bracketing, maxIterationcount, convergence);
                if (result == -1) {
                    // CHECKSTYLE: stop ModifiedControlVariable check
                    // Reason: Orekit code kept as such
                    --i;
                    // CHECKSTYLE: resume ModifiedControlVariable check
                } else if (result == 1) {
                    return true;
                }
                
            }

            // no event during the whole step
            this.pendingEvent = false;
            this.pendingEventTime = null;
            return false;

        } catch (final LocalWrapperException lwe) {
            // NOTEST: unreachable exception since LocalWrapperException is a private EventState RuntimeException
            // Can only be thrown through f function, whose exception is unreachable too
            throw lwe.getWrappedException();
        }

    }
    
    /**
     * Evaluate the impact of the proposed step on the event detector.<br>
     * 
     * See <a href="https://www.orekit.org/forge/issues/110">Orekit issue 110</a> for more
     * information. Default
     * constructor changed in order to instanciate a bracketing solver, to solve the bracketing
     * exception.
     * 
     * @param interpolator
     *        step interpolator for the proposed step
     * @param satId
     *        satellite ID whose step is assessed
     * @return true if the event detector triggers an event before the end of the proposed step
     *         (this implies the step should be rejected)
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
    @SuppressWarnings("PMD.ExceptionAsFlowControl")
    public boolean evaluateStep(final MultiPatriusStepInterpolator interpolator, final String satId) throws PatriusException {
        // CHECKSTYLE: resume MethodLength check
        // CHECKSTYLE: resume CyclomaticComplexity check
        // CHECKSTYLE: resume ReturnCount check
        try {
            final double convergence = this.detector.getThreshold();
            if (this.forward ^ interpolator.isForward()) {
                this.forward = !this.forward;
                this.pendingEvent = false;
                this.pendingEventTime = null;
                this.previousEventTime = null;
            }
            final AbsoluteDate t1 = interpolator.getCurrentDate();
            final double dt = t1.durationFrom(this.t0);
            if (MathLib.abs(dt) < convergence) {
                // we cannot do anything on such a small step, don't trigger any events
                // Check if there is an event, if yes, start of step is returned
                // Per PATRIUS convention, event cannot be detected at end of step
                interpolator.setInterpolatedDate(t1);
                final double gb = this.detector.g(interpolator.getInterpolatedStates().get(satId));
                final boolean signChange = ((this.g0 >= 0) && (gb < 0)) || ((this.g0 <= 0) && (gb > 0));
                if (dt > 0 && signChange) {
                    // Event
                    this.pendingEventTime = this.t0;
                    this.pendingEvent = true;
                    return true;
                } else {
                    // No event or interval is 0s (then event will be detected on start of next step)
                    this.pendingEventTime = null;
                    this.pendingEvent = false;
                    return false;
                }
            }

            final UnivariateFunction f = new UnivariateFunction(){
                /** Serializable UID. */
                private static final long serialVersionUID = 197517831095184758L;

                /** {@inheritDoc} */
                @Override
                public double value(final double t) {
                    try {
                        double direction = Double.POSITIVE_INFINITY;
                        if (!EventState.this.forward) {
                            direction = Double.NEGATIVE_INFINITY;
                        }
                        final double nextUlp =
                            new AbsoluteDate(EventState.this.t00, MathLib.nextAfter(t, direction)).durationFrom(t1);
                        final AbsoluteDate interpolatedDate;
                        if ((EventState.this.forward && nextUlp > 0) || (!EventState.this.forward && nextUlp < 0)) {
                            // In case the date is the last possible date before the upper interval bound: set it to
                            // last date since that comes from a numerical quality issue from AbsoluteDate.durationFrom
                            interpolatedDate = t1;
                        } else {
                            // Standard case
                            interpolatedDate = EventState.this.t00.shiftedBy(t, t1, EventState.this.forward);
                        }
                        interpolator.setInterpolatedDate(interpolatedDate);
                        return EventState.this.detector.g(interpolator.getInterpolatedStates().get(satId));
                    } catch (final PatriusException oe) {
                        // NOTEST: unreachable exception since g, setInterpolatedDate and getInterpolatedStates are all
                        // called in the method before f.value()
                        throw new LocalWrapperException(oe);
                    }
                }
            };

            final BrentSolver nonBracketing = new BrentSolver(convergence);
            final PegasusSolver bracketing = new PegasusSolver(convergence);
            final int maxIterationcount = this.detector.getMaxIterationCount();

            // t00 variable used in case of untreated event because of slope selection: t0 is then changed for later
            // whereas it should stay idle for the remaining part of the algorithm
            this.t00 = this.t0;

            final int n = MathLib.max(1, (int) MathLib.ceil(MathLib.abs(dt) / this.detector.getMaxCheckInterval()));
            final double h = dt / n;
            this.ta = this.t0;
            this.ga = this.g0;
            for (int i = 0; i < n; ++i) {

                // evaluate detector value at the end of the substep
                final AbsoluteDate tb;
                if (i < (n - 1)) {
                    tb = this.t00.shiftedBy((i + 1) * h, t1, this.forward);
                } else {
                    // CNES BUG A-1036
                    // Last step should be exactly t1.
                    // We force it because, even if shiftedBy is very accurate, it does not
                    // always fall exactly on t1 at the last step - and this is sometimes
                    // critical (for bounded propagators for instance).
                    tb = t1;
                }
                interpolator.setInterpolatedDate(tb);
                final double gb = this.detector.g(interpolator.getInterpolatedStates().get(satId));

                final int result =
                    this.evaluateStepInternal(gb, tb, t1, f, nonBracketing, bracketing, maxIterationcount, convergence);
                if (result == -1) {
                    // CHECKSTYLE: stop ModifiedControlVariable check
                    // Reason: Orekit code kept as such
                    --i;
                    // CHECKSTYLE: resume ModifiedControlVariable check
                } else if (result == 1) {
                    return true;
                }
                
            }

            // No event during the whole step
            this.pendingEvent = false;
            this.pendingEventTime = null;
            return false;
        } catch (final LocalWrapperException lwe) {
            // NOTEST: unreachable exception since LocalWrapperException is a private EventState RuntimeException
            // Can only be thrown through f function, whose exception is unreachable too
            throw lwe.getWrappedException();
        }
    }
    
    /**
     * Common part between mono and multi evaluateStep methods.
     * 
     * @param gaIn
     * 
     * @param gb
     *        value of g at tb
     * @param tb
     *        date at the end of the substep
     * @param t1
     *        step date limit that tb cannot exceed
     * @param f
     *        function evaluating g
     * @param nonBracketing
     *        non-bracketed solver to find a zero of f
     * @param bracketing
     *        bracketed solver to find a zero of f
     * @param maxIterationcount
     *        iterations limit
     * @param convergence
     *        convergence threshold
     * 
     * @return -1 if a false positive detection occurs, 1 if the event detector triggers an event before the end of the
     *         proposed step (this implies the step should be rejected), 0 otherwise
     */
    private int evaluateStepInternal(final double gb, final AbsoluteDate tb, final AbsoluteDate t1,
                                     final UnivariateFunction f, final BrentSolver nonBracketing,
                                     final PegasusSolver bracketing, final int maxIterationcount,
                                     final double convergence) {

        // Specific case: event at first date
        final boolean eventAtFirstStep = (MathLib.abs(this.g0) < Precision.DOUBLE_COMPARISON_EPSILON)
                && (this.t00.equals(this.initialDate)) && (this.ta.equals(this.t00));

        // Check events occurrence
        int resultFlag = 0;
        if ((this.ga >= 0) ^ (gb >= 0) || eventAtFirstStep) {
            // There is a sign change: an event is expected during this step

            // Variation direction, with respect to the integration direction
            this.increasing = gb >= this.ga;
            final int slope = this.detector.getSlopeSelection();
            if (slope == 2 || ((this.forward ^ !this.increasing) ^ slope == 1)) {

                // Find the event time making sure we select a solution just at or past the exact root
                final double dtA = this.ta.durationFrom(this.t00);
                final double dtB = tb.durationFrom(this.t00);

                // Compute the root date
                final AbsoluteDate root =
                    computeRoot(gb, dtA, dtB, t1, f, nonBracketing, bracketing, maxIterationcount);

                if ((this.previousEventTime != null)
                        && (MathLib.abs(root.durationFrom(this.ta)) <= convergence)
                        && (MathLib.abs(root.durationFrom(this.previousEventTime)) <= convergence)) {
                    // We have either found nothing or found (again ?) a past event, retry the substep excluding this
                    // value
                    if (this.forward) {
                        this.ta = this.ta.shiftedBy(convergence, t1, this.forward);
                    } else {
                        this.ta = this.ta.shiftedBy(-convergence, t1, this.forward);
                    }
                    this.ga = f.value(this.ta.durationFrom(this.t00));
                    resultFlag = -1;
                } else if ((this.previousEventTime == null)
                        || (MathLib.abs(this.previousEventTime.durationFrom(root)) > convergence)) {
                    // If the monotony hasn't change since last accepted step, this event must be overlapped to prevent
                    // a non consistent list of events occurred
                    this.pendingEventTime = root;
                    this.pendingEvent = true;
                    resultFlag = 1;
                } else {
                    // No sign change: there is no event for now
                    this.ta = tb;
                    this.ga = gb;
                }
                
            } else {
                // There is a sign change but must not be taken into account because of slope selection
                this.ta = tb;
                this.ga = gb;
                // Update g0 however since sign of g has changed (= stepAccepted())
                this.t0 = this.ta;
                this.previousEventTime = this.t0;
                this.increasing = !this.increasing;
                this.g0Old = this.g0;
                if (this.increasing) {
                    this.g0 = Double.NEGATIVE_INFINITY;
                } else {
                    this.g0 = Double.POSITIVE_INFINITY;
                }
            }
            
        } else {
            // No sign change: there is no event for now
            this.ta = tb;
            this.ga = gb;
        }

        // No specific treatment in parent method
        return resultFlag;
    }
    
    /**
     * Compute the root date of the event.
     * 
     * @param gb
     *        value of g at tb
     * @param dtA
     *        event time before (or equal to) the root time
     * @param dtB
     *        event time after the root time
     * @param t1
     *        step date limit that tb cannot exceed
     * @param f
     *        function evaluating g
     * @param nonBracketing
     *        non-bracketed solver to find a zero of f
     * @param bracketing
     *        bracketed solver to find a zero of f
     * @param maxIterationcount
     *        iterations limit
     * 
     * @return the date corresponding to the event
     */
    private AbsoluteDate computeRoot(final double gb, final double dtA, final double dtB, final AbsoluteDate t1,
                                     final UnivariateFunction f, final BrentSolver nonBracketing,
                                     final PegasusSolver bracketing, final int maxIterationcount) {

        // Initialize value of the root
        double dtRoot = dtA;

        if ((f.value(dtA) >= 0) ^ (gb >= 0)) {

            // Parameterize based on the forward flag
            final double minDt;
            final double maxDt;
            final AllowedSolution allowedSolution;
            if (this.forward) {
                minDt = dtA;
                maxDt = dtB;
                allowedSolution = AllowedSolution.RIGHT_SIDE;
            } else {
                minDt = dtB;
                maxDt = dtA;
                allowedSolution = AllowedSolution.LEFT_SIDE;
            }

            // Solve: find zero of the univariate function
            if (this.solver instanceof BracketedUnivariateSolver<?>) {
                @SuppressWarnings("unchecked")
                final BracketedUnivariateSolver<UnivariateFunction> bracketSolver =
                    (BracketedUnivariateSolver<UnivariateFunction>) this.solver;
                dtRoot = bracketSolver.solve(maxIterationcount, f, minDt, maxDt, allowedSolution);
            } else {
                final double dtBaseRoot = nonBracketing.solve(maxIterationcount, f, minDt, maxDt);
                final int remainingEval = maxIterationcount - nonBracketing.getEvaluations();
                dtRoot = UnivariateSolverUtils.forceSide(remainingEval, f, bracketing, dtBaseRoot, minDt, maxDt,
                    allowedSolution);
            }

        }

        // Compute the associated root date
        return this.t00.shiftedBy(dtRoot, t1, this.forward);

    }
    
    /**
     * Get the occurrence time of the event triggered in the current step.
     * 
     * @return occurrence time of the event triggered in the current step.
     */
    public AbsoluteDate getEventTime() {
        return this.pendingEventTime;
    }

    /**
     * Acknowledge the fact the step has been accepted by the propagator.
     * 
     * @param state
     *        value of the state vector at the end of the step
     * @exception PatriusException
     *            if the value of the switching function cannot be evaluated
     */
    public void stepAccepted(final SpacecraftState state) throws PatriusException {

        // If there is an event, then g is increasing only if g0 (before the event) is negative
        // If g0 = 0, value of "increasing" has already been stored
        if (this.g0 != 0) {
            this.increasing = this.g0 < 0;
        }

        this.t0 = state.getDate();
        this.g0Old = this.g0;

        if (this.pendingEvent && (this.pendingEventTime.durationFrom(this.t0) <= this.detector.getThreshold())) {
            // force the sign to its value "just after the event"
            this.previousEventTime = state.getDate();
            // Filter event, other variables are updated (g0 in order to account for modulo and unexpected 
            // g sign change, etc.)
            if (!this.detector.filterEvent(state, increasing, forward)) {
                // eventOccurred only if event not filtered
                this.nextAction = this.detector.eventOccurred(state, this.increasing, this.forward);
                this.remove = this.detector.shouldBeRemoved();
            }
            this.g0Old = this.g0;
            this.g0 = this.increasing ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
        } else {
            this.nextAction = EventDetector.Action.CONTINUE;
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
        this.previousEventTime = null;
    }

    /**
     * Getter for t0.
     * 
     * @return t0
     */
    public AbsoluteDate getT0() {
        return this.t0;
    }

    /**
     * Check if the propagation should be stopped at the end of the current step.
     * 
     * @return true if the propagation should be stopped
     */
    public boolean stop() {
        return this.nextAction == EventDetector.Action.STOP;
    }

    /**
     * Check if the current detector should be removed at the end of the current step.
     * 
     * @return true if the detector should be removed
     */
    public boolean removeDetector() {
        return this.remove;
    }

    /**
     * Let the event detector reset the state if it wants.
     * 
     * @param oldState
     *        value of the state vector at the beginning of the next step
     * @return new state (null if no reset is needed)
     * @exception PatriusException
     *            if the state cannot be reset by the event detector
     */
    public SpacecraftState reset(final SpacecraftState oldState) throws PatriusException {

        if (!this.pendingEvent) {
            return null;
        }

        final SpacecraftState newState = (this.nextAction == EventDetector.Action.RESET_STATE)
            ? this.detector.resetState(oldState) : null;
        this.pendingEvent = false;
        this.pendingEventTime = null;

        return newState;

    }

    /** Local runtime exception wrapping OrekitException. */
    private static class LocalWrapperException extends RuntimeException {

        /** Serializable UID. */
        private static final long serialVersionUID = 2734331164409224983L;

        /** Wrapped exception. */
        private final PatriusException wrappedException;

        /**
         * Simple constructor.
         * 
         * @param wrapped
         *        wrapped exception
         */
        public LocalWrapperException(final PatriusException wrapped) {
            super();
            this.wrappedException = wrapped;
        }

        /**
         * Get the wrapped exception.
         * 
         * @return wrapped exception
         */
        public PatriusException getWrappedException() {
            return this.wrappedException;
        }

    }

}
