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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:FA:FA-2079:15/05/2019:Non suppression de detecteurs d'evenements en fin de propagation
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:300:18/03/2015:Creation multi propagator
 * VERSION::DM:454:24/11/2015:Overload method shouldBeRemoved()
 * VERSION::FA:1653:23/10/2018: correct handling of detectors in several propagations
 * VERSION::DM:1872:10/10/2016:add Multi-attitude provider
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events.multi;

import java.util.Map;

import fr.cnes.sirius.patrius.attitudes.multi.MultiAttitudeProvider;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.ode.events.EventHandler;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.numerical.multi.MultiStateVectorInfo;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;

/**
 * <p>
 * This interface is copied from {@link fr.cnes.sirius.patrius.propagation.events.AdaptedEventDetector} and adapted to
 * multi propagation.
 * </p>
 * <p>
 * Adapt a {@link MultiEventDetector} to commons-math {@link fr.cnes.sirius.patrius.math.ode.events.EventHandler}
 * interface. A {@link MultiStateVectorInfo} is needed, so that the state vector can be translated to/from a map of
 * {@link SpacecraftState}.
 * </p>
 * 
 * @concurrency conditionally thread-safe
 * @concurrency.comment Conditionally thread-safe if all attributes are thread-safe
 * 
 * @author maggioranic
 * 
 * @version $Id$
 * 
 * @since 2.3
 * 
 */
public class AdaptedMultiEventDetector implements EventHandler {

    /** Propagation orbit type. */
    private OrbitType orbitT;

    /** Position angle type. */
    private PositionAngle angleT;

    /** Map of attitude provider for forces computation defined for each spacecraft. */
    private Map<String, MultiAttitudeProvider> attitudeProvidersForces;

    /** Map of attitude provider for events computation defined for each spacecraft. */
    private Map<String, MultiAttitudeProvider> attitudeProvidersEvents;

    /** Underlying multi-sat event detector. */
    private final MultiEventDetector multiDetector;

    /** Reference date from which t is counted. */
    private AbsoluteDate refDate;

    /** Central body attraction coefficient (m<sup>3</sup>/s<sup>2</sup>). */
    private Map<String, Double> mus;

    /** integrationFrame frame in which integration is performed. */
    private Map<String, Frame> integrationFrames;

    /** Informations about the global state vector containing all spacecraft data */
    private MultiStateVectorInfo stateInfo;

    /**
     * Build a wrapped multi-sat event detector.
     * 
     * @param detector
     *        multi-sat event detector to wrap
     * @param orbitType
     *        orbit type
     * @param angleType
     *        position angle type
     * @param attProvidersForces
     *        map of attitude provider for forces computation
     * @param attProvidersEvents
     *        map of attitude provider for events computation
     * @param referenceDate
     *        reference date from which t is counted
     * @param muMap
     *        map of central body attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @param framesMap
     *        map of frame in which integration is performed
     * @param stateVectorInfo
     *        informations about the global state vector
     */
    public AdaptedMultiEventDetector(final MultiEventDetector detector, final OrbitType orbitType,
        final PositionAngle angleType,
        final Map<String, MultiAttitudeProvider> attProvidersForces,
        final Map<String, MultiAttitudeProvider> attProvidersEvents,
        final AbsoluteDate referenceDate, final Map<String, Double> muMap,
        final Map<String, Frame> framesMap, final MultiStateVectorInfo stateVectorInfo) {
        this.multiDetector = detector;
        this.orbitT = orbitType;
        this.angleT = angleType;
        this.attitudeProvidersForces = attProvidersForces;
        this.attitudeProvidersEvents = attProvidersEvents;
        this.refDate = referenceDate;
        this.mus = muMap;
        this.integrationFrames = framesMap;
        this.stateInfo = stateVectorInfo;
    }

    /**
     * Reinitialize data.
     * 
     * @param orbitType
     *        orbit type
     * @param angleType
     *        position angle type
     * @param attProvidersForces
     *        map of attitude provider for forces computation
     * @param attProvidersEvents
     *        map of attitude provider for events computation
     * @param referenceDate
     *        reference date from which t is counted
     * @param muMap
     *        map of central body attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @param framesMap
     *        map of frame in which integration is performed
     * @param stateVectorInfo
     *        informations about the global state vector
     */
    public void reinitialize(final OrbitType orbitType, final PositionAngle angleType,
                             final Map<String, MultiAttitudeProvider> attProvidersForces,
                             final Map<String, MultiAttitudeProvider> attProvidersEvents,
                             final AbsoluteDate referenceDate, final Map<String, Double> muMap,
                             final Map<String, Frame> framesMap, final MultiStateVectorInfo stateVectorInfo) {
        this.orbitT = orbitType;
        this.angleT = angleType;
        this.attitudeProvidersForces = attProvidersForces;
        this.attitudeProvidersEvents = attProvidersEvents;
        this.refDate = referenceDate;
        this.mus = muMap;
        this.integrationFrames = framesMap;
        this.stateInfo = stateVectorInfo;
    }

    /** {@inheritDoc} */
    @Override
    public void init(final double t0, final double[] y0, final double t) {
        try {
            final AbsoluteDate currentDate = this.refDate.shiftedBy(t0);
            final Map<String, SpacecraftState> s0 = this.stateInfo.mapArrayToStates(y0, currentDate,
                this.orbitT, this.angleT, this.attitudeProvidersForces, this.attitudeProvidersEvents, this.mus,
                this.integrationFrames);
            this.multiDetector.init(s0, currentDate);
        } catch (final PatriusException oe) {
            throw new PatriusExceptionWrapper(oe);
        }
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public double g(final double t, final double[] y) {
        try {
            final AbsoluteDate currentDate = this.refDate.shiftedBy(t);

            if (this.multiDetector.getClass().equals(OneSatEventDetectorWrapper.class)) {
                // Mono-sat case
                final OneSatEventDetectorWrapper wrapper = ((OneSatEventDetectorWrapper) this.multiDetector);
                final String id = wrapper.getID();
                final SpacecraftState s = this.stateInfo.mapArrayToState(y, currentDate, this.orbitT, this.angleT,
                    this.attitudeProvidersForces.get(id), this.attitudeProvidersEvents.get(id),
                    wrapper.getID());

                // final SpacecraftState s = stateInfo.mapArrayToState(y, currentDate, orbitT,
                // angleT, attitudeProvidersForces.get(id), attitudeProvidersEvents.get(id),
                // mus.get(id),
                // integrationFrames.get(id), wrapper.getID());

                return wrapper.g(s);
            } else {
                // Multi-sat case
                final Map<String, SpacecraftState> s = this.stateInfo.mapArrayToStates(y, currentDate,
                    this.orbitT, this.angleT, this.attitudeProvidersForces, this.attitudeProvidersEvents, this.mus,
                    this.integrationFrames);
                return this.multiDetector.g(s);
            }

        } catch (final PatriusException oe) {
            throw new PatriusExceptionWrapper(oe);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Action eventOccurred(final double t, final double[] y, final boolean increasing,
                                final boolean forward) {
        try {
            // Get current state
            final AbsoluteDate currentDate = this.refDate.shiftedBy(t);
            final Map<String, SpacecraftState> states = this.stateInfo.mapArrayToStates(y, currentDate,
                this.orbitT, this.angleT, this.attitudeProvidersForces, this.attitudeProvidersEvents, this.mus,
                this.integrationFrames);
            // Get Action from Orekit
            final EventDetector.Action whatNext = this.multiDetector.eventOccurred(states, increasing,
                forward);
            // Convert Orekit action into Commons-Math action
            return AdaptedMonoEventDetector.convertOrekitIntoCMAction(whatNext);
        } catch (final PatriusException oe) {
            throw new PatriusExceptionWrapper(oe);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean shouldBeRemoved() {
        return this.multiDetector.shouldBeRemoved();
    }

    /** {@inheritDoc} */
    @Override
    public void resetState(final double t, final double[] y) {
        try {
            // Reset all states
            final AbsoluteDate currentDate = this.refDate.shiftedBy(t);
            final Map<String, SpacecraftState> states = this.stateInfo.mapArrayToStates(y, currentDate,
                this.orbitT, this.angleT, this.attitudeProvidersForces, this.attitudeProvidersEvents, this.mus,
                this.integrationFrames);
            final Map<String, SpacecraftState> newStates = this.multiDetector.resetStates(states);
            this.stateInfo.mapStatesToArray(newStates, this.orbitT, this.angleT, y);
        } catch (final PatriusException oe) {
            throw new PatriusExceptionWrapper(oe);
        }
    }

    /** {@inheritDoc} */
    @Override
    public int getSlopeSelection() {
        return this.multiDetector.getSlopeSelection();
    }

    /**
     * Get the multiDetector object.
     * 
     * @return multiDetector
     **/
    public MultiEventDetector getMultiDetector() {
        return this.multiDetector;
    }
}
