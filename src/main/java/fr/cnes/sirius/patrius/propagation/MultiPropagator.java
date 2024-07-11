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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2089:15/05/2019:[PATRIUS] passage a Java 8
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:300:18/03/2015:Creation multi propagator
 * VERSION::DM:1872:10/12/2018:Substitution of AttitudeProvider with MultiAttitudeProvider
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation;

import java.util.Collection;
import java.util.Map;

import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.multi.MultiAttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.multi.MultiAttitudeProviderWrapper;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.events.multi.MultiEventDetector;
import fr.cnes.sirius.patrius.propagation.sampling.multi.MultiPatriusFixedStepHandler;
import fr.cnes.sirius.patrius.propagation.sampling.multi.MultiPatriusStepHandler;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * 
 * <p>
 * This interface is copied from {@link fr.cnes.sirius.patrius.propagation.Propagator} and adapted to multi propagation.
 * </p>
 * <p>
 * This interface provides a way to propagate several {@link SpacecraftState} (including orbit, attitudes and additional
 * states) at any time.
 * </p>
 * <p>
 * This interface is the top-level abstraction for multi states propagation. An initial state is identified by its ID.
 * It could be added to the propagator through {@link #addInitialState(SpacecraftState, String)}. All initial states
 * added should have the same initial state. Each initial state is defined with a proper frame with can be retrieved
 * using {@link #getFrame(String)}. This interface only allows propagation to a predefined date by calling
 * {@link #propagate(AbsoluteDate)} or {@link #propagate(AbsoluteDate, AbsoluteDate)}.
 * </p>
 * <p>
 * This interface is implemented by numerical integrators using rich force models and by continuous models built after
 * numerical integration has been completed and dense output data as been gathered.
 * </p>
 * 
 * @author maggioranic
 * 
 * @version $Id$
 * 
 * @since 3.0
 * 
 */
public interface MultiPropagator {

    /** Indicator for slave mode. */
    int SLAVE_MODE = 0;

    /** Indicator for master mode. */
    int MASTER_MODE = 1;

    /** Indicator for ephemeris generation mode. */
    int EPHEMERIS_GENERATION_MODE = 2;

    /**
     * Get the current operating mode of the propagator.
     * 
     * @return one of {@link #SLAVE_MODE}, {@link #MASTER_MODE}, {@link #EPHEMERIS_GENERATION_MODE}
     * @see #setSlaveMode()
     * @see #setMasterMode(double, MultiPatriusFixedStepHandler)
     * @see #setMasterMode(MultiPatriusStepHandler)
     * @see #setEphemerisMode()
     */
    int getMode();

    /**
     * Set the propagator to slave mode.
     * <p>
     * This mode is used when the user needs only the final orbit at the target time. The (slave) propagator computes
     * this result and return it to the calling (master) application, without any intermediate feedback.
     * </p>
     * <p>
     * This is the default mode.
     * </p>
     * 
     * @see #setMasterMode(double, MultiPatriusFixedStepHandler)
     * @see #setMasterMode(MultiPatriusStepHandler)
     * @see #setEphemerisMode()
     * @see #getMode()
     * @see #SLAVE_MODE
     */
    void setSlaveMode();

    /**
     * Set the propagator to master mode with fixed steps.
     * <p>
     * This mode is used when the user needs to have some custom function called at the end of each finalized step
     * during integration. The (master) propagator integration loop calls the (slave) application callback methods at
     * each finalized step.
     * </p>
     * 
     * @param h
     *        fixed stepsize (s)
     * @param handler
     *        handler called at the end of each finalized step
     * @see #setSlaveMode()
     * @see #setMasterMode(MultiPatriusStepHandler)
     * @see #setEphemerisMode()
     * @see #getMode()
     * @see #MASTER_MODE
     */
    void setMasterMode(double h, MultiPatriusFixedStepHandler handler);

    /**
     * Set the propagator to master mode with variable steps.
     * <p>
     * This mode is used when the user needs to have some custom function called at the end of each finalized step
     * during integration. The (master) propagator integration loop calls the (slave) application callback methods at
     * each finalized step.
     * </p>
     * 
     * @param handler
     *        handler called at the end of each finalized step
     * @see #setSlaveMode()
     * @see #setMasterMode(double, MultiPatriusFixedStepHandler)
     * @see #setEphemerisMode()
     * @see #getMode()
     * @see #MASTER_MODE
     */
    void setMasterMode(MultiPatriusStepHandler handler);

    /**
     * Set the propagator to ephemeris generation mode.
     * <p>
     * This mode is used when the user needs random access to the orbit state at any time between the initial and target
     * times, and in no sequential order. A typical example is the implementation of search and iterative algorithms
     * that may navigate forward and backward inside the propagation range before finding their result.
     * </p>
     * <p>
     * Beware that since this mode stores <strong>all</strong> intermediate results, it may be memory intensive for long
     * integration ranges and high precision/short time steps.
     * </p>
     * 
     * @see #getGeneratedEphemeris(String)
     * @see #setSlaveMode()
     * @see #setMasterMode(double, MultiPatriusFixedStepHandler)
     * @see #setMasterMode(MultiPatriusStepHandler)
     * @see #getMode()
     * @see #EPHEMERIS_GENERATION_MODE
     */
    void setEphemerisMode();

    /**
     * Get the ephemeris generated during propagation for a defined spacecraft.
     * 
     * @param satId
     *        the spacecraft ID
     * @return generated ephemeris
     * @exception IllegalStateException
     *            if the propagator was not set in ephemeris generation mode before propagation
     * @see #setEphemerisMode()
     */
    BoundedPropagator getGeneratedEphemeris(final String satId);

    /**
     * Get the propagator initial states.
     * 
     * @return initial states
     * @exception PatriusException
     *            if state cannot be retrieved
     */
    Map<String, SpacecraftState> getInitialStates() throws PatriusException;

    /**
     * Add a multi spacecraft event detector.
     * 
     * @param detector
     *        event detector to add
     * @see #clearEventsDetectors()
     * @see #getEventsDetectors()
     */
    void addEventDetector(final MultiEventDetector detector);

    /**
     * Add an event detector to a specific spacecraft.
     * The spacecraft defined by the input ID should already be added using
     * {@link #addInitialState(SpacecraftState, String)}.
     * 
     * @param detector
     *        event detector to add
     * @param satId
     *        the spacecraft ID
     * @see #clearEventsDetectors()
     * @see #getEventsDetectors()
     */
    void addEventDetector(final EventDetector detector, final String satId);

    /**
     * Get all the events {@link MultiEventDetector detectors} that have been added.
     * 
     * @return an unmodifiable collection of the added detectors
     * @see #addEventDetector(MultiEventDetector)
     * @see #addEventDetector(EventDetector, String)
     * @see #clearEventsDetectors()
     */
    Collection<MultiEventDetector> getEventsDetectors();

    /**
     * Remove all events detectors.
     * 
     * @see #addEventDetector(MultiEventDetector)
     * @see #addEventDetector(EventDetector, String)
     * @see #getEventsDetectors()
     */
    void clearEventsDetectors();

    /**
     * <p>
     * Get the default attitude provider.
     * </p>
     * <p>
     * The unique attitude provider given by default is returned. If null, the attitude provider for forces computation,
     * and then the attitude provider for events computation is returned.
     * </p>
     * 
     * <p>
     * <b> Warning: if you provided an {@link AttitudeProvider} then to get back your {@link AttitudeProvider}, the
     * returned {@link MultiAttitudeProvider} should be cast to {@link MultiAttitudeProviderWrapper} and method
     * {@link MultiAttitudeProviderWrapper#getAttitudeProvider()} should be used. </b>
     * </p>
     * 
     * @param satId
     *        the spacecraft ID
     * @return attitude provider for forces computation (by default)
     */
    MultiAttitudeProvider getAttitudeProvider(final String satId);

    /**
     * <p>
     * Get the attitude provider for forces computation.
     * </p>
     *
     * <p>
     * <b> Warning: if you provided an {@link AttitudeProvider} then to get back your {@link AttitudeProvider}, the
     * returned {@link MultiAttitudeProvider} should be cast to {@link MultiAttitudeProviderWrapper} and method
     * {@link MultiAttitudeProviderWrapper#getAttitudeProvider()} should be used. </b>
     * </p>
     *
     * @param satId
     *        the spacecraft ID
     * @return attitude provider for forces computation, return null if not defined.
     */
    MultiAttitudeProvider getAttitudeProviderForces(final String satId);

    /**
     * <p>
     * Get the attitude provider for events computation.
     * </p>
     *
     * <p>
     * <b> Warning: if you provided an {@link AttitudeProvider} then to get back your {@link AttitudeProvider}, the
     * returned {@link MultiAttitudeProvider} should be cast to {@link MultiAttitudeProviderWrapper} and method
     * {@link MultiAttitudeProviderWrapper#getAttitudeProvider()} should be used. </b>
     * </p>
     *
     * @param satId
     *        the spacecraft ID
     * @return attitude provider for events computation, return null if not defined.
     */
    MultiAttitudeProvider getAttitudeProviderEvents(final String satId);

    /**
     * <p>
     * Set attitude provider for defined spacecraft.
     * </p>
     * <p>
     * A default attitude provider is available in {@link fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw}.
     * </p>
     * <p>
     * The spacecraft defined by the input ID should already be added using
     * {@link #addInitialState(SpacecraftState, String)}.
     * </p>
     * 
     * @param satId
     *        the spacecraft ID
     * @param attitudeProvider
     *        attitude provider
     */
    void setAttitudeProvider(final AttitudeProvider attitudeProvider, final String satId);

    /**
     * <p>
     * Set attitude provider for forces computation.
     * </p>
     * <p>
     * A default attitude provider is available in {@link fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw}.
     * </p>
     * <p>
     * The spacecraft defined by the input ID should already be added using
     * {@link #addInitialState(SpacecraftState, String)}.
     * </p>
     * 
     * @param satId
     *        the spacecraft ID
     * @param attitudeProviderForces
     *        attitude provider for forces computation
     */
    void setAttitudeProviderForces(final AttitudeProvider attitudeProviderForces, final String satId);

    /**
     * <p>
     * Set attitude provider for events computation.
     * </p>
     * <p>
     * A default attitude provider is available in {@link fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw}.
     * </p>
     * <p>
     * The spacecraft defined by the input ID should already be added using
     * {@link #addInitialState(SpacecraftState, String)}.
     * </p>
     * 
     * @param satId
     *        the spacecraft ID
     * @param attitudeProviderEvents
     *        attitude provider for events computation
     */
    void setAttitudeProviderEvents(final AttitudeProvider attitudeProviderEvents, final String satId);

    /**
     * Get the frame in which the orbit is propagated.
     * <p>
     * The propagation frame is the definition frame of the initial state, so this method should be called after this
     * state has been set.
     * </p>
     * <p>
     * The spacecraft defined by the input ID should already be added using
     * {@link #addInitialState(SpacecraftState, String)}.
     * </p>
     * 
     * @param satId
     *        the spacecraft ID
     * @return frame in which the orbit is propagated
     * @see #addInitialState(SpacecraftState, String)
     */
    Frame getFrame(final String satId);

    /**
     * Add a new spacecraft state to be propagated.
     * 
     * @param satId
     *        the spacecraft ID
     * @param initialState
     *        the new spacecraft state
     * @throws PatriusException
     *         if an initial state is already defined with this ID.
     *         if the input ID is null.
     *         if the date of the initial state is different from the initial states already defined in the
     *         propagator.
     */
    void addInitialState(final SpacecraftState initialState, final String satId) throws PatriusException;

    /**
     * Propagate towards a target date.
     * <p>
     * Simple propagators use only the target date as the specification for computing the propagated state. More feature
     * rich propagators can consider other information and provide different operating modes or G-stop facilities to
     * stop at pinpointed events occurrences. In these cases, the target date is only a hint, not a mandatory objective.
     * </p>
     * 
     * @param target
     *        target date towards which orbit state should be propagated
     * @return propagated state
     * @exception PropagationException
     *            if state cannot be propagated
     */
    Map<String, SpacecraftState> propagate(AbsoluteDate target) throws PropagationException;

    /**
     * Propagate from a start date towards a target date.
     * <p>
     * Those propagators use a start date and a target date to compute the propagated state. For propagators using event
     * detection mechanism, if the provided start date is different from the initial state date, a first, simple
     * propagation is performed, without processing any event computation. Then complete propagation is performed from
     * start date to target date.
     * </p>
     * 
     * @param start
     *        start date from which orbit state should be propagated
     * @param target
     *        target date to which orbit state should be propagated
     * @return propagated state
     * @exception PropagationException
     *            if state cannot be propagated
     */
    Map<String, SpacecraftState> propagate(AbsoluteDate start, AbsoluteDate target) throws PropagationException;
}
