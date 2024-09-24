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
* VERSION:4.9:DM:DM-3172:10/05/2022:[PATRIUS] Ajout d'un throws PatriusException a la methode init de l'interface EDet
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.7:DM:DM-2859:18/05/2021:Optimisation du code ; SpacecraftState 
 * VERSION:4.3:FA:FA-2079:15/05/2019:Non suppression de detecteurs d'evenements en fin de propagation
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 *                             (added forward parameter to eventOccurred signature)
 * VERSION::DM:208:05/08/2014: one shot event detector
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::FA:377:08/12/2014:StepHandler initializing anomaly in propagator
 * VERSION::FA:410:16/04/2015: Anomalies in the Patrius Javadoc
 * VERSION::DM:454:24/11/2015:Overload method shouldBeRemoved()
 * VERSION::FA:1653:23/10/2018: correct handling of detectors in several propagations
 * VERSION::FA:2079:29/01/2019: add the required detector parameter getter
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import java.io.Serializable;
import java.util.Map;

import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.ode.events.EventHandler;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.AdditionalStateInfo;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;

/**
 * Adapt an {@link fr.cnes.sirius.patrius.propagation.events.EventDetector} to commons-math
 * {@link fr.cnes.sirius.patrius.math.ode.events.EventHandler} interface. <br>
 * The implemented classes should
 * A list of {@link AdditionalStateInfo} is needed, so that the state vector can be translated
 * to/from additional states in a simple and generic manner by {@link SpacecraftState}.
 * Conditionally thread-safe if all attributes are thread-safe.<br>
 * 
 * @author Fabien Maussion
 */
public class AdaptedEventDetector implements EventHandler, Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = -5983739314228874403L;

    /** Propagation orbit type. */
    private OrbitType orbitType;

    /** Position angle type. */
    private PositionAngle angleType;

    /** Attitude provider for forces computation. */
    private AttitudeProvider attitudeProviderForces;

    /** Attitude provider for events computation. */
    private AttitudeProvider attitudeProviderEvents;

    /** Underlying event detector. */
    private final EventDetector detector;

    /** Reference date from which t is counted. */
    private AbsoluteDate referenceDate;

    /** Central body attraction coefficient (m<sup>3</sup>/s<sup>2</sup>). */
    private double mu;

    /** integrationFrame frame in which integration is performed. */
    private Frame integrationFrame;

    /** Information needed to map the state array to additional states data. */
    private Map<String, AdditionalStateInfo> asinfos;

    /**
     * Build a wrapped event detector.
     * 
     * @param detectorIn
     *        event detector to wrap
     * @param info
     *        information on additional states (position in the state vector is deduced from it)
     * @param orbitTypeIn
     *        orbit type
     * @param angleTypeIn
     *        position angle type
     * @param attProviderForces
     *        attitude provider for forces computation
     * @param attProviderEvents
     *        attitude provider for events computation
     * @param referenceDateIn
     *        reference date from which t is counted
     * @param muIn
     *        central body attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @param integrationFrameIn
     *        frame in which integration is performed
     */
    public AdaptedEventDetector(final EventDetector detectorIn,
        final Map<String, AdditionalStateInfo> info,
        final OrbitType orbitTypeIn, final PositionAngle angleTypeIn,
        final AttitudeProvider attProviderForces,
        final AttitudeProvider attProviderEvents,
        final AbsoluteDate referenceDateIn,
        final double muIn, final Frame integrationFrameIn) {
        this.detector = detectorIn;
        this.asinfos = info;
        this.orbitType = orbitTypeIn;
        this.angleType = angleTypeIn;
        this.attitudeProviderForces = attProviderForces;
        this.attitudeProviderEvents = attProviderEvents;
        this.referenceDate = referenceDateIn;
        this.mu = muIn;
        this.integrationFrame = integrationFrameIn;
    }

    /**
     * Reinitialize reference data.
     * 
     * @param info info on additional states (position in the state vector is deduced from it)
     * @param orbitTypeIn orbit type
     * @param angleTypeIn position angle type
     * @param attProviderForces attitude provider for forces computation
     * @param attProviderEvents attitude provider for events computation
     * @param referenceDateIn reference date from which t is counted
     * @param muIn central body attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @param integrationFrameIn frame in which integration is performed
     */
    public void reinitialize(final Map<String, AdditionalStateInfo> info,
                             final OrbitType orbitTypeIn, final PositionAngle angleTypeIn,
                             final AttitudeProvider attProviderForces,
                             final AttitudeProvider attProviderEvents,
                             final AbsoluteDate referenceDateIn,
                             final double muIn, final Frame integrationFrameIn) {
        this.asinfos = info;
        this.orbitType = orbitTypeIn;
        this.angleType = angleTypeIn;
        this.attitudeProviderForces = attProviderForces;
        this.attitudeProviderEvents = attProviderEvents;
        this.referenceDate = referenceDateIn;
        this.mu = muIn;
        this.integrationFrame = integrationFrameIn;
    }

    /** {@inheritDoc} */
    @Override
    public void init(final double t0, final double[] y0, final double t) {
        final AbsoluteDate currentDate = this.referenceDate.shiftedBy(t0);
        final SpacecraftState s0 = new SpacecraftState(y0, this.orbitType, this.angleType, currentDate, this.mu,
                this.integrationFrame, this.asinfos, this.attitudeProviderForces, this.attitudeProviderEvents);
        try {
            this.detector.init(s0, this.referenceDate.shiftedBy(t));
        } catch (final PatriusException e) {
            throw new PatriusExceptionWrapper(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public double g(final double t, final double[] y) {
        try {
            final AbsoluteDate currentDate = this.referenceDate.shiftedBy(t);
            final SpacecraftState s =
                new SpacecraftState(y, this.orbitType, this.angleType, currentDate, this.mu, this.integrationFrame,
                    this.asinfos, this.attitudeProviderForces, this.attitudeProviderEvents);
            return this.detector.g(s);
        } catch (final PatriusException oe) {
            throw new PatriusExceptionWrapper(oe);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Action eventOccurred(final double t, final double[] y, final boolean increasing, final boolean forward) {
        try {
            // Compute current date
            final AbsoluteDate currentDate = this.referenceDate.shiftedBy(t);
            // Create new spacecraft state for current date
            final SpacecraftState state = new SpacecraftState(y, this.orbitType, this.angleType, currentDate, this.mu,
                this.integrationFrame,
                this.asinfos, this.attitudeProviderForces, this.attitudeProviderEvents);
            final EventDetector.Action whatNext = this.detector.eventOccurred(state, increasing, forward);
            // Action is event action
            final Action result;
            switch (whatNext) {
                case STOP:
                    result = Action.STOP;
                    break;
                case RESET_STATE:
                    result = Action.RESET_STATE;
                    break;
                case RESET_DERIVATIVES:
                    result = Action.RESET_DERIVATIVES;
                    break;
                default:
                    result = Action.CONTINUE;
                    break;
            }
            // Return result
            return result;
        } catch (final PatriusException oe) {
            // Exception
            throw new PatriusExceptionWrapper(oe);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean shouldBeRemoved() {
        return this.detector.shouldBeRemoved();
    }

    /** {@inheritDoc} */
    @Override
    public void resetState(final double t, final double[] y) {
        try {
            final AbsoluteDate currentDate = this.referenceDate.shiftedBy(t);
            final SpacecraftState oldState =
                new SpacecraftState(y, this.orbitType, this.angleType, currentDate, this.mu,
                    this.integrationFrame, this.asinfos, this.attitudeProviderForces, this.attitudeProviderEvents);
            final SpacecraftState newState = this.detector.resetState(oldState);
            newState.mapStateToArray(this.orbitType, this.angleType, y);
        } catch (final PatriusException oe) {
            throw new PatriusExceptionWrapper(oe);
        }
    }

    /** {@inheritDoc} */
    @Override
    public int getSlopeSelection() {
        return this.detector.getSlopeSelection();
    }

    /**
     * Get the detector object.
     * 
     * @return detector
     **/
    public EventDetector getDetector() {
        return this.detector;
    }
}
