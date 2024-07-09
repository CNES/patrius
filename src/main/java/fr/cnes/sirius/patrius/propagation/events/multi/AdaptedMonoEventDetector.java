/**
 * 
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
 * 
 * @history created 18/03/2015
 * 
 * HISTORY
 * VERSION:4.5:DM:DM-2460:27/05/2020:Prise en compte des temps de propagation dans les calculs evenements
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:300:18/03/2015:Creation multi propagator
 * VERSION::DM:454:24/11/2015:Overload method shouldBeRemoved()
 * VERSION::FA:1653:23/10/2018: correct handling of detectors in several propagations
 * VERSION::DM:1872:10/10/2016:add Multi-attitude provider
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events.multi;

import fr.cnes.sirius.patrius.attitudes.multi.MultiAttitudeProvider;
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
 * Adapt an {@link EventDetector} to commons-math {@link fr.cnes.sirius.patrius.math.ode.events.EventHandler} interface.
 * A {@link MultiStateVectorInfo} is needed, so that the state vector can be translated to/from a map of
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
 * @since 3.0
 * 
 */
public class AdaptedMonoEventDetector implements EventHandler {

    /** Propagation orbit type. */
    private OrbitType orbitT;

    /** Position angle type. */
    private PositionAngle angleT;

    /** Attitude provider for forces computation. */
    private MultiAttitudeProvider attitudeProviderForces;

    /** Attitude provider for events computation. */
    private MultiAttitudeProvider attitudeProviderEvents;

    /** Underlying event detector. */
    private final EventDetector singleDetector;

    /** Reference date from which t is counted. */
    private AbsoluteDate refDate;

    /** ID of the concerned spacecraft. */
    private String satID;

    /** Informations about the global state vector containing all spacecraft data */
    private MultiStateVectorInfo stateInfos;

    /**
     * Build a wrapped event detector.
     * 
     * @param detector
     *        event detector to wrap
     * @param orbitType
     *        orbit type
     * @param angleType
     *        position angle type
     * @param attProviderForces
     *        attitude provider for forces computation
     * @param attProviderEvents
     *        attitude provider for events computation
     * @param referenceDate
     *        reference date from which t is counted
     * @param stateVectorInfo
     *        informations about the global state vector
     * @param satId
     *        spacecraft Id
     */
    public AdaptedMonoEventDetector(final EventDetector detector,
        final OrbitType orbitType, final PositionAngle angleType,
        final MultiAttitudeProvider attProviderForces,
        final MultiAttitudeProvider attProviderEvents,
        final AbsoluteDate referenceDate,
        final MultiStateVectorInfo stateVectorInfo,
        final String satId) {
        this.singleDetector = detector;
        this.orbitT = orbitType;
        this.angleT = angleType;
        this.attitudeProviderForces = attProviderForces;
        this.attitudeProviderEvents = attProviderEvents;
        this.refDate = referenceDate;
        this.stateInfos = stateVectorInfo;
        this.satID = satId;
    }

    /**
     * Reinitialize data.
     * 
     * @param orbitType
     *        orbit type
     * @param angleType
     *        position angle type
     * @param attProviderForces
     *        attitude provider for forces computation
     * @param attProviderEvents
     *        attitude provider for events computation
     * @param referenceDate
     *        reference date from which t is counted
     * @param stateVectorInfo
     *        informations about the global state vector
     * @param satId
     *        spacecraft Id
     */
    public void reinitialize(
                             final OrbitType orbitType, final PositionAngle angleType,
                             final MultiAttitudeProvider attProviderForces,
                             final MultiAttitudeProvider attProviderEvents,
                             final AbsoluteDate referenceDate,
                             final MultiStateVectorInfo stateVectorInfo,
                             final String satId) {
        this.orbitT = orbitType;
        this.angleT = angleType;
        this.attitudeProviderForces = attProviderForces;
        this.attitudeProviderEvents = attProviderEvents;
        this.refDate = referenceDate;
        this.stateInfos = stateVectorInfo;
        this.satID = satId;
    }

    /** {@inheritDoc} */
    @Override
    public void init(final double t0, final double[] y0, final double t) {
        try {
            final AbsoluteDate currentDate = this.refDate.shiftedBy(t0);
            this.singleDetector.init(this.stateInfos.mapArrayToState(y0, currentDate, this.orbitT, this.angleT,
                this.attitudeProviderForces, this.attitudeProviderEvents, this.satID), currentDate);
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
            return this.singleDetector.g(this.stateInfos.mapArrayToState(y, currentDate, this.orbitT, this.angleT,
                this.attitudeProviderForces, this.attitudeProviderEvents, getSatId()));
        } catch (final PatriusException oe) {
            throw new PatriusExceptionWrapper(oe);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Action eventOccurred(final double t, final double[] y, final boolean increasing, final boolean forward) {
        try {
            // Get current state
            final AbsoluteDate currentDate = this.refDate.shiftedBy(t);
            final SpacecraftState state = this.stateInfos.mapArrayToState(y, currentDate, this.orbitT, this.angleT,
                this.attitudeProviderForces, this.attitudeProviderEvents, this.satID);
            // Get Action from Orekit
            final EventDetector.Action whatNext = this.singleDetector.eventOccurred(state, increasing, forward);
            // Convert Orekit action into Commons-Math action
            return convertOrekitIntoCMAction(whatNext);
        } catch (final PatriusException oe) {
            throw new PatriusExceptionWrapper(oe);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean shouldBeRemoved() {
        return this.singleDetector.shouldBeRemoved();
    }

    /** {@inheritDoc} */
    @Override
    public void resetState(final double t, final double[] y) {
        try {
            final AbsoluteDate currentDate = this.refDate.shiftedBy(t);
            final SpacecraftState oldState = this.stateInfos.mapArrayToState(y, currentDate, this.orbitT, this.angleT,
                this.attitudeProviderForces, this.attitudeProviderEvents, this.satID);
            final SpacecraftState newState = this.singleDetector.resetState(oldState);
            final int satVectorSize = newState.getStateVectorSize();
            final double[] localY = new double[satVectorSize];
            newState.mapStateToArray(this.orbitT, this.angleT, localY);
            System.arraycopy(localY, 0, y, this.stateInfos.getSatRank(this.satID), satVectorSize);
        } catch (final PatriusException oe) {
            throw new PatriusExceptionWrapper(oe);
        }
    }

    /** {@inheritDoc} */
    @Override
    public int getSlopeSelection() {
        return this.singleDetector.getSlopeSelection();
    }

    /**
     * Convert Orekit action into Commons-Math action
     * 
     * @param orekitAction
     *        orekit action
     * @return commons math action
     */
    public static final Action convertOrekitIntoCMAction(final EventDetector.Action orekitAction) {
        // Initialize result
        final Action result;
        // set result depending on action
        switch (orekitAction) {
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
        // return result
        return result;
    }

    /**
     * Returns satellite ID.
     * 
     * @return satellite ID
     */
    public String getSatId() {
        return this.satID;
    }

}
