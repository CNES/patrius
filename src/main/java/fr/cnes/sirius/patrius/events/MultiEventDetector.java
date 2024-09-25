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
 * @history created 18/03/2015
 *
 * HISTORY
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.13:FA:FA-79:08/12/2023:[PATRIUS] Probleme dans la fonction g de LocalTimeAngleDetector
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3172:10/05/2022:[PATRIUS] Ajout d'un throws PatriusException a la methode init de l'interface EDet
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:300:18/03/2015:Creation multi propagator
 * VERSION::DM:454:24/11/2015:Add method shouldBeRemoved() to manage detector suppression
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events;

import java.util.Map;

import fr.cnes.sirius.patrius.events.EventDetector.Action;
import fr.cnes.sirius.patrius.events.detectors.LocalTimeAngleDetector;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * This interface is copied from {@link fr.cnes.sirius.patrius.events.EventDetector} and adapted to multi
 * propagation.
 * </p>
 * <p>
 * This interface represents space-dynamics aware events detectors.
 * <p>
 * It mirrors the {@link fr.cnes.sirius.patrius.math.ode.events.EventHandler
 * EventHandler} interface from <a href="http://commons.apache.org/math/"> commons-math</a> but provides a
 * space-dynamics interface to the methods.
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
 * {@link #eventOccurred(Map, boolean, boolean)} method. The method can do whatever it needs with the event (logging it,
 * performing some processing, ignore it ...). The return value of the method will be used by the propagator to stop or
 * resume propagation, possibly changing the state vector or the future event detection.
 * </p>
 * 
 * @author maggioranic
 * 
 * @version $Id: MultiEventDetector.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 3.0
 * 
 */
public interface MultiEventDetector {

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
     * @param s0
     *        map of initial states
     * @param t
     *        target time for the integration
     * @throws PatriusException thrown if initialization failed
     */
    void init(final Map<String, SpacecraftState> s0, final AbsoluteDate t) throws PatriusException;

    /**
     * Compute the value of the switching function.
     * <p>
     * This function must be continuous (at least in its roots neighborhood), as the integrator will need to find its
     * roots to locate the events.
     * </p>
     * 
     * @param s
     *        the current states information: date, kinematics, attitudes for forces
     *        and events computation, and additional states for each states
     * @return value of the switching function
     * @exception PatriusException
     *            if some specific error occurs
     */
    @SuppressWarnings("PMD.ShortMethodName")
    double g(final Map<String, SpacecraftState> s) throws PatriusException;

    /**
     * Handle an event and choose what to do next.
     * 
     * <p>
     * The scheduling between this method and the MultiOrekitStepHandler method handleStep is to call this method first
     * and <code>handleStep</code> afterwards. This scheduling allows the propagator to pass <code>true</code> as the
     * <code>isLast</code> parameter to the step handler to make it aware the step will be the last one if this method
     * returns {@link fr.cnes.sirius.patrius.events.EventDetector.Action#STOP}. As the interpolator may be
     * used to navigate back throughout the last step MultiOrekitStepNormalizer does for example), user code called by
     * this method and user code called by step handlers may experience apparently out of order values of the
     * independent time variable. As an example, if the same user object implements both this {@link MultiEventDetector
     * MultiEventDetector} interface and the MultiOrekitFixedStepHandler interface, a <em>forward</em> integration may
     * call its <code>eventOccurred</code> method with a state at 2000-01-01T00:00:10 first and call its
     * <code>handleStep</code> method with a state at 2000-01-01T00:00:09 afterwards. Such out of order calls are
     * limited to the size of the integration step for MultiOrekitStepHandler and to the size of the fixed step for
     * MultiOrekitFixedStepHandler.
     * </p>
     * 
     * @param s
     *        the current states information: date, kinematics, attitude for forces
     *        and events computation, and additional states for each states
     * @param increasing
     *        if true, the value of the switching function increases
     *        when times increases around event (note that increase is measured with respect
     *        to physical time, not with respect to propagation which may go backward in time)
     * @param forward
     *        if true, the integration variable (time) increases during integration.
     * @return one of {@link fr.cnes.sirius.patrius.events.EventDetector.Action#STOP},
     *         {@link fr.cnes.sirius.patrius.events.EventDetector.Action#RESET_STATE},
     *         {@link fr.cnes.sirius.patrius.events.EventDetector.Action#RESET_DERIVATIVES},
     *         {@link fr.cnes.sirius.patrius.events.EventDetector.Action#CONTINUE}
     * @exception PatriusException
     *            if some specific error occurs
     */
    Action eventOccurred(final Map<String, SpacecraftState> s, final boolean increasing,
                         final boolean forward) throws PatriusException;

    /**
     * <p>
     * This method is called after the step handler has returned and before the next step is started, but only when
     * {@link #eventOccurred} has been called.
     * </p>
     * 
     * @return true if the current detector should be removed
     */
    boolean shouldBeRemoved();

    /**
     * Reset the states (including additional states) prior to continue propagation.
     * <p>
     * This method is called after the step handler has returned and before the next step is started, but only when
     * {@link #eventOccurred} has itself returned the
     * {@link fr.cnes.sirius.patrius.events.EventDetector.Action#RESET_STATE} indicator. It allows the user
     * to reset the state for the next step, without perturbing the step handler of the finishing step. If the
     * {@link #eventOccurred} never returns the
     * {@link fr.cnes.sirius.patrius.events.EventDetector.Action#RESET_STATE} indicator, this function will
     * never be called, and it is safe to simply return null.
     * </p>
     * 
     * @param oldStates
     *        old states
     * @return new states
     * @exception PatriusException
     *            if the states cannot be reseted
     */
    Map<String, SpacecraftState> resetStates(final Map<String, SpacecraftState> oldStates) throws PatriusException;

    /**
     * Filter last event: returns true if the last event is a false detection, false otherwise.
     * <p>This method is called right before {@link #eventOccurred(SpacecraftState, boolean, boolean)} method.</p>
     * <p>This may be useful in order to filter some events in particular when angles are at stake (see for example 
     * {@link LocalTimeAngleDetector}).</p>
     * @param states states at last event occurrence
     * @param increasing if true, the value of the switching function increases when times increases
     *        around event (note that increase is measured with respect to physical time, not with
     *        respect to propagation which may go backward in time)
     * @param forward if true, the integration variable (time) increases during integration.
     * @return true if the last event is a false detection, false otherwise
     * @throws PatriusException thrown if computation failed for some reasons
     */
    boolean filterEvent(final Map<String, SpacecraftState> states,
            final boolean increasing,
            final boolean forward) throws PatriusException;

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
     * Get the parameter in charge of the selection of detected events by the slope of the g-function.
     * 
     * @return
     *         EventDetector.INCREASING (0): events related to the increasing g-function;<br>
     *         EventDetector.DECREASING (1): events related to the decreasing g-function;<br>
     *         EventDetector.INCREASING_DECREASING (2): events related to both increasing and decreasing g-function.
     */
    int getSlopeSelection();
}
