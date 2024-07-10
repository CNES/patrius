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
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 *                             (added forward parameter to eventOccurred signature)
 * VERSION::DM:208:05/08/2014: one shot event detector
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:454:24/11/2015:Add method shouldBeRemoved() to manage detector suppression
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import java.io.Serializable;

import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This interface represents space-dynamics aware events detectors with support for additional
 * states.
 * 
 * <p>
 * It mirrors the {@link fr.cnes.sirius.patrius.math.ode.events.EventHandler EventHandler} interface from <a
 * href="http://commons.apache.org/math/"> commons-math</a> but provides a space-dynamics interface to the methods.
 * </p>
 * 
 * <p>
 * Events detectors are a useful solution to meet the requirements of propagators concerning discrete conditions. The
 * state of each event detector is queried by the integrator at each step. When the sign of the underlying g switching
 * function changes, the step is rejected and reduced, in order to make sure the sign changes occur only at steps
 * boundaries.
 * </p>
 * 
 * <p>
 * When step ends exactly at a switching function sign change, the corresponding event is triggered, by calling the
 * {@link #eventOccurred(SpacecraftState, boolean, boolean)} method. The method can do whatever it needs with the event
 * (logging it, performing some processing, ignore it ...). The return value of the method will be used by the
 * propagator to stop or resume propagation, possibly changing the state vector or the future event detection.
 * <p>
 * 
 * @author Luc Maisonobe
 * @author V&eacute;ronique Pommier-Maurussane
 */
public interface EventDetector extends Serializable {

    /** Enumerate for actions to be performed when an event occurs. */
    public enum Action {

        /**
         * Stop indicator.
         * <p>
         * This value should be used as the return value of the {@link #eventOccurred eventOccurred} method when the
         * propagation should be stopped after the event ending the current step.
         * </p>
         */
        STOP,

        /**
         * Reset state indicator.
         * <p>
         * This value should be used as the return value of the {@link #eventOccurred eventOccurred} method when the
         * propagation should go on after the event ending the current step, with a new state (which will be retrieved
         * thanks to the {@link #resetState resetState} method).
         * </p>
         */
        RESET_STATE,

        /**
         * Reset derivatives indicator.
         * <p>
         * This value should be used as the return value of the {@link #eventOccurred eventOccurred} method when the
         * propagation should go on after the event ending the current step, with recomputed derivatives vector.
         * </p>
         */
        RESET_DERIVATIVES,

        /**
         * Continue indicator.
         * <p>
         * This value should be used as the return value of the {@link #eventOccurred eventOccurred} method when the
         * propagation should go on after the event ending the current step.
         * </p>
         */
        CONTINUE;
    }

    /** Increasing g-function related events parameter. */
    int INCREASING = 0;

    /** Decreasing g-function related events parameter. */
    int DECREASING = 1;

    /** Both increasing and decreasing g-function related events parameter. */
    int INCREASING_DECREASING = 2;

    /**
     * Initialize event handler at the start of a propagation.
     * <p>
     * This method is called once at the start of the propagation. It may be used by the event handler to initialize
     * some internal data if needed.
     * </p>
     * 
     * @param s0 initial state
     * @param t target time for the integration
     */
    void init(SpacecraftState s0, AbsoluteDate t);

    /**
     * Compute the value of the switching function. This function must be continuous (at least in
     * its roots neighborhood), as the integrator will need to find its roots to locate the events.
     * 
     * @param s the current state information: date, kinematics, attitude for forces and events
     *        computation, mass provider, and additional states
     * @return value of the switching function
     * @exception PatriusException if some specific error occurs
     */
    @SuppressWarnings("PMD.ShortMethodName")
    double g(SpacecraftState s) throws PatriusException;

    /**
     * Handle an event and choose what to do next.
     * 
     * <p>
     * The scheduling between this method and the PatriusStepHandler method handleStep() is to call this method first
     * and <code>handleStep</code> afterwards. This scheduling allows the propagator to pass <code>true</code> as the
     * <code>isLast</code> parameter to the step handler to make it aware the step will be the last one if this method
     * returns {@link EventDetector.Action#STOP}. As the interpolator may be used to navigate back throughout the last
     * step (as {@link fr.cnes.sirius.patrius.propagation.sampling.PatriusStepNormalizer
     * OrekitStepNormalizer} does for example), user code called by this method and user code called by step handlers
     * may experience apparently out of order values of the independent time variable. As an example, if the same user
     * object implements both this {@link EventDetector
     * EventDetector} interface and the {@link fr.cnes.sirius.patrius.propagation.sampling.PatriusFixedStepHandler
     * OrekitFixedStepHandler} interface, a <em>forward</em> integration may call its <code>eventOccurred</code> method
     * with a state at 2000-01-01T00:00:10 first and call its <code>handleStep</code> method with a state at
     * 2000-01-01T00:00:09 afterwards. Such out of order calls are limited to the size of the integration step for
     * {@link fr.cnes.sirius.patrius.propagation.sampling.PatriusStepHandler variable step handlers} and to the size of
     * the fixed step for {@link fr.cnes.sirius.patrius.propagation.sampling.PatriusFixedStepHandler fixed step
     * handlers}.
     * </p>
     * 
     * @param s the current state information: date, kinematics, attitude for forces and events
     *        computation, mass provider, and additional states
     * @param increasing if true, the value of the switching function increases when times increases
     *        around event (note that increase is measured with respect to physical time, not with
     *        respect to propagation which may go backward in time)
     * @param forward if true, the integration variable (time) increases during integration.
     * @return one of {@link EventDetector.Action#STOP}, {@link EventDetector.Action#RESET_STATE},
     *         {@link EventDetector.Action#RESET_DERIVATIVES}, {@link EventDetector.Action#CONTINUE}
     * @exception PatriusException if some specific error occurs
     */
    Action eventOccurred(SpacecraftState s, boolean increasing, boolean forward)
                                                                                throws PatriusException;

    /**
     * This method is called after {@link #eventOccurred} has been triggered. It returns true if the
     * current detector should be removed after first event detection. <b>WARNING:</b> this method
     * can be called only once a event has been triggered. Before, the value is not available.
     * 
     * @return true if the current detector should be removed after first event detection
     */
    boolean shouldBeRemoved();

    /**
     * Reset the state (including additional states) prior to continue propagation.
     * <p>
     * This method is called after the step handler has returned and before the next step is started, but only when
     * {@link #eventOccurred} has itself returned the {@link EventDetector.Action#RESET_STATE} indicator. It allows the
     * user to reset the state for the next step, without perturbing the step handler of the finishing step. If the
     * {@link #eventOccurred} never returns the {@link EventDetector.Action#RESET_STATE} indicator, this function will
     * never be called, and it is safe to simply return null.
     * </p>
     * 
     * @param oldState old state
     * @return new state
     * @exception PatriusException if the state cannot be reseted
     */
    SpacecraftState resetState(SpacecraftState oldState) throws PatriusException;

    /**
     * Get the convergence threshold in the event time search.
     * 
     * @return convergence threshold (s)
     */
    double getThreshold();

    /**
     * Get maximal time interval between switching function checks.
     * 
     * @return maximal time interval (s) between switching function checks
     */
    double getMaxCheckInterval();

    /**
     * Get maximal number of iterations in the event time search.
     * 
     * @return maximal number of iterations in the event time search
     */
    int getMaxIterationCount();

    /**
     * Get the parameter in charge of the selection of detected events by the slope of the
     * g-function.
     * 
     * @return EventDetector.INCREASING (0): events related to the increasing g-function;<br>
     *         EventDetector.DECREASING (1): events related to the decreasing g-function;<br>
     *         EventDetector.INCREASING_DECREASING (2): events related to both increasing and
     *         decreasing g-function.
     */
    int getSlopeSelection();

    /**
     * A copy of the detector. By default copy is deep. If not, detector javadoc will specify which
     * attribute is not fully copied. In that case, the attribute reference is passed.
     * 
     * @return a copy of the detector.
     */
    EventDetector copy();
}
