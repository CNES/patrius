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
* VERSION:4.8:FA:FA-2941:15/11/2021:[PATRIUS] Correction anomalies suite a DM 2767 
* VERSION:4.8:FA:FA-2956:15/11/2021:[PATRIUS] Temps de propagation non implemente pour certains evenements 
* VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
* VERSION:4.3:DM:DM-2089:15/05/2019:[PATRIUS] passage a Java 8
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::FA:381:15/12/2014:Propagator tolerances and default mass and attitude issues
 * VERSION::DM:393:12/03/2015: Constant Attitude Laws
 * VERSION::DM:426:30/10/2015:Possibility to set up orbits in non inertial frames and manage the conversion frame
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation;

import java.util.Collection;

import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusFixedStepHandler;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusStepHandler;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * This interface provides a way to propagate an orbit at any time.
 * 
 * <p>
 * This interface is the top-level abstraction for orbit propagation. It only allows propagation to a predefined date.
 * It is implemented by analytical models which have no time limit, by orbit readers based on external data files, by
 * numerical integrators using rich force models and by continuous models built after numerical integration has been
 * completed and dense output data as been gathered.
 * </p>
 * 
 * @author Luc Maisonobe
 * @author V&eacute;ronique Pommier-Maurussane
 * 
 */

public interface Propagator extends SpacecraftStateProvider {

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
     * @see #setMasterMode(double, PatriusFixedStepHandler)
     * @see #setMasterMode(PatriusStepHandler)
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
     * @see #setMasterMode(double, PatriusFixedStepHandler)
     * @see #setMasterMode(PatriusStepHandler)
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
     * @see #setMasterMode(PatriusStepHandler)
     * @see #setEphemerisMode()
     * @see #getMode()
     * @see #MASTER_MODE
     */
    void setMasterMode(double h, PatriusFixedStepHandler handler);

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
     * @see #setMasterMode(double, PatriusFixedStepHandler)
     * @see #setEphemerisMode()
     * @see #getMode()
     * @see #MASTER_MODE
     */
    void setMasterMode(PatriusStepHandler handler);

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
     * @see #getGeneratedEphemeris()
     * @see #setSlaveMode()
     * @see #setMasterMode(double, PatriusFixedStepHandler)
     * @see #setMasterMode(PatriusStepHandler)
     * @see #getMode()
     * @see #EPHEMERIS_GENERATION_MODE
     */
    void setEphemerisMode();

    /**
     * Set propagation frame.
     * 
     * @param frame
     *        the frame to use.
     *        This frame must be inertial or pseudo-inertial, otherwise an exception is raised.
     * @throws PatriusException
     *         if frame is not inertial or pseudo-inertial
     */
    void setOrbitFrame(final Frame frame) throws PatriusException;

    /**
     * Get the ephemeris generated during propagation.
     * 
     * @return generated ephemeris
     * @exception IllegalStateException
     *            if the propagator was not set in ephemeris
     *            generation mode before propagation
     * @see #setEphemerisMode()
     */
    BoundedPropagator getGeneratedEphemeris();

    /**
     * Get the propagator initial state.
     * 
     * @return initial state
     * @exception PatriusException
     *            if state cannot be retrieved
     */
    SpacecraftState getInitialState() throws PatriusException;

    /**
     * Reset the propagator initial state.
     * 
     * @param state
     *        new initial state to consider
     * @exception PropagationException
     *            if initial state cannot be reset
     */
    void resetInitialState(final SpacecraftState state) throws PropagationException;

    /**
     * Add an event detector.
     * 
     * @param detector
     *        event detector to add
     * @see #clearEventsDetectors()
     * @see #getEventsDetectors()
     */
    void addEventDetector(final EventDetector detector);

    /**
     * Get all the events detectors that have been added.
     * 
     * @return an unmodifiable collection of the added detectors
     * @see #addEventDetector(EventDetector)
     * @see #clearEventsDetectors()
     */
    Collection<EventDetector> getEventsDetectors();

    /**
     * Remove all events detectors.
     * 
     * @see #addEventDetector(EventDetector)
     * @see #getEventsDetectors()
     */
    void clearEventsDetectors();

    /**
     * Get attitude provider.
     * 
     * @return attitude provider for forces computation (by default)
     */
    AttitudeProvider getAttitudeProvider();

    /**
     * Get attitude provider for forces computation.
     * 
     * @return attitude provider for forces computation
     */
    AttitudeProvider getAttitudeProviderForces();

    /**
     * Get attitude provider for events computation.
     * 
     * @return attitude provider for events computation
     */
    AttitudeProvider getAttitudeProviderEvents();

    /**
     * Set attitude provider for forces and events computation.
     * A default attitude provider is available in {@link fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw}.
     * 
     * @param attitudeProvider
     *        attitude provider
     */
    void setAttitudeProvider(final AttitudeProvider attitudeProvider);

    /**
     * Set attitude provider for forces computation.
     * A default attitude provider is available in {@link fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw}.
     * 
     * @param attitudeProviderForces
     *        attitude provider for forces computation
     */
    void setAttitudeProviderForces(final AttitudeProvider attitudeProviderForces);

    /**
     * Set attitude provider for events computation.
     * A default attitude provider is available in {@link fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw}.
     * 
     * @param attitudeProviderEvents
     *        attitude provider for events computation
     */
    void setAttitudeProviderEvents(final AttitudeProvider attitudeProviderEvents);

    /**
     * Get the frame in which the orbit is propagated.
     * <p>
     * 4 cases are possible:
     * <ul>
     * <li>The propagation frame has been defined (using {@link #setOrbitFrame(Frame)}): it is returned.</li>
     * <li>The propagation frame has not been defined and the initial state has been provided and is expressed in a
     * pseudo-inertial frame: the initial state frame is returned.</li>
     * <li>The propagation frame has not been defined and the initial state has been provided and is not expressed in a
     * pseudo-inertial frame: null is returned.</li>
     * <li>The propagation frame has not been defined and the initial state has not been provided:
     *  null is returned.</li>
     * </ul>
     * </p>
     * 
     * @return frame in which the orbit is propagated
     */
    Frame getFrame();

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
    SpacecraftState propagate(AbsoluteDate target) throws PropagationException;

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
    SpacecraftState propagate(AbsoluteDate start, AbsoluteDate target) throws PropagationException;

    /** {@inheritDoc} */
    @Override
    default Frame getNativeFrame(final AbsoluteDate date,
            final Frame frame) throws PatriusException {
        return getFrame();
    }

}
