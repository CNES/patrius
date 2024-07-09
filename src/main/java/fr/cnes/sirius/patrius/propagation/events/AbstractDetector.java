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
 * VERSION:4.5:DM:DM-2460:27/05/2020:Prise en compte des temps de propagation dans les calculs evenements
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 *                             (added forward parameter to eventOccurred signature)
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:454:24/11/2015:Add method shouldBeRemoved() to manage detector suppression
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.signalpropagation.SignalPropagation;
import fr.cnes.sirius.patrius.signalpropagation.SignalPropagationModel;
import fr.cnes.sirius.patrius.signalpropagation.SignalPropagationModel.FixedDate;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;

/**
 * Common parts shared by several events finders.
 * A default implementation of most of the methods of EventDetector Interface.
 * Make it easier to create a new detector.
 * 
 * @see fr.cnes.sirius.patrius.propagation.Propagator#addEventDetector(EventDetector)
 * @author Luc Maisonobe
 */
public abstract class AbstractDetector implements EventDetector {

    /** Default maximum checking interval (s). */
    public static final double DEFAULT_MAXCHECK = 600;

    /** Default convergence threshold (s). */
    public static final double DEFAULT_THRESHOLD = 1.e-6;

    /** Default max number of iterations. */
    private static final int MAX_ITERATIONS = 100;

    /** Serializable UID. */
    private static final long serialVersionUID = -8212002898109868489L;

    /** Max check interval. */
    private final double maxCheck;

    /** Convergence threshold. */
    private final double threshold;

    /** Select all events, increasing g related events or decreasing g related events only. */
    private final int slopeSelection;

    /** Propagation delay type. Default is INSTANTANEOUS. */
    private PropagationDelayType propagationDelayType;
    
    /**
     * Propagation delay type.
     */
    public enum PropagationDelayType {

        /** Instantaneous. */
        INSTANTANEOUS,

        /** Computed considering light speed. */
        LIGHT_SPEED;
    }

    /**
     * Build a new instance. The detector will detect both ascending and descending g-function
     * related events.
     * 
     * @param maxCheckIn
     *        maximum checking interval (s)
     * @param thresholdIn
     *        convergence threshold (s)
     */
    protected AbstractDetector(final double maxCheckIn, final double thresholdIn) {
        this.maxCheck = maxCheckIn;
        this.threshold = thresholdIn;
        this.slopeSelection = INCREASING_DECREASING;
        this.propagationDelayType = PropagationDelayType.INSTANTANEOUS;
    }

    /**
     * Build a new instance.
     * 
     * @param slopeSelectionIn
     *        g-function slope selection (0, 1, or 2)
     * @param maxCheckIn
     *        maximum checking interval (s)
     * @param thresholdIn
     *        convergence threshold (s)
     */
    public AbstractDetector(final int slopeSelectionIn, final double maxCheckIn, final double thresholdIn) {
        this.maxCheck = maxCheckIn;
        this.threshold = thresholdIn;
        // Validate input
        if (slopeSelectionIn != 0 && slopeSelectionIn != 1 && slopeSelectionIn != 2) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.UNSUPPORTED_SLOPE_SELECTION_TYPE);
        }
        this.slopeSelection = slopeSelectionIn;
        this.propagationDelayType = PropagationDelayType.INSTANTANEOUS;
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        // do nothing by default
    }

    /** {@inheritDoc} */
    @Override
    public abstract Action eventOccurred(SpacecraftState s, boolean increasing,
                                         boolean forward) throws PatriusException;

    /** {@inheritDoc} */
    @Override
    public abstract boolean shouldBeRemoved();

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public abstract double g(SpacecraftState s) throws PatriusException;

    /** {@inheritDoc} */
    @Override
    public double getMaxCheckInterval() {
        return this.maxCheck;
    }

    /** {@inheritDoc} */
    @Override
    public int getMaxIterationCount() {
        return MAX_ITERATIONS;
    }

    /** {@inheritDoc} */
    @Override
    public double getThreshold() {
        return this.threshold;
    }

    /** {@inheritDoc} */
    @Override
    public SpacecraftState resetState(final SpacecraftState oldState) throws PatriusException {
        return oldState;
    }

    /** {@inheritDoc} */
    @Override
    public int getSlopeSelection() {
        return this.slopeSelection;
    }

    /**
     * Compute signal emission date which is date at which currently seen signal by spacecraft has been emitted by
     * emitter depending on {@link PropagationDelayType}.
     * @param emitter emitter
     * @param s spacecraft state (receiver)
     * @param epsilon absolute duration threshold used for convergence of signal propagation computation. The epsilon is
     *        a time absolute error (ex: 1E-14s, in this case, the signal distance accuracy is 1E-14s x 3E8m/s = 3E-6m).
     * @param delayType propagation delay type
     * @return signal emission date
     * @throws PatriusException thrown if computation failed
     */
    protected AbsoluteDate getSignalEmissionDate(final PVCoordinatesProvider emitter,
            final SpacecraftState s,
            final double epsilon,
            final PropagationDelayType delayType) throws PatriusException {
        final AbsoluteDate res;
        switch (delayType) {
            case INSTANTANEOUS:
                // Instantaneous signal
                res = s.getDate();
                break;
            case LIGHT_SPEED:
                // Take light speed into account
                final SignalPropagationModel model = new SignalPropagationModel(s.getFrame(), epsilon);
                final SignalPropagation signal = model.computeSignalPropagation(emitter, s.getOrbit(), s.getDate(),
                        FixedDate.RECEPTION);
                res = signal.getStartDate();
                break;
            default:
                // Cannot happen
                throw new PatriusRuntimeException(PatriusMessages.INTERNAL_ERROR, null);
        }
        return res;
    }

    /**
     * Compute signal reception date which is date at which currently seen signal by receiver has been emitted by
     * spacecraft (emitter) depending on {@link PropagationDelayType}.
     * @param receiver receiver
     * @param s spacecraft state (emitter)
     * @param epsilon absolute duration threshold used for convergence of signal propagation computation. The epsilon is
     *        a time absolute error (ex: 1E-14s, in this case, the signal distance accuracy is 1E-14s x 3E8m/s = 3E-6m).
     * @param delayType propagation delay type
     * @return signal emission date
     * @throws PatriusException thrown if computation failed
     */
    protected AbsoluteDate getSignalReceptionDate(final PVCoordinatesProvider receiver,
            final SpacecraftState s,
            final double epsilon,
            final PropagationDelayType delayType) throws PatriusException {
        final AbsoluteDate res;
        switch (delayType) {
            case INSTANTANEOUS:
                // Instantaneous signal
                res = s.getDate();
                break;
            case LIGHT_SPEED:
                // Take light speed into account
                final SignalPropagationModel model = new SignalPropagationModel(s.getFrame(), epsilon);
                final SignalPropagation signal = model.computeSignalPropagation(receiver, s.getOrbit(), s.getDate(),
                        FixedDate.EMISSION);
                res = signal.getEndDate();
                break;
            default:
                // Cannot happen
                throw new PatriusRuntimeException(PatriusMessages.INTERNAL_ERROR, null);
        }
        return res;
    }

    /**
     * Returns the propagation delay type.
     * @return the propagation delay type
     */
    public PropagationDelayType getPropagationDelayType() {
        return propagationDelayType;
    }
    
    /**
     * Setter for propagation delay computation type. Warning: check Javadoc of detector to see if detector takes into
     * account propagation time delay. if not, signals are always considered instantaneous.
     * @param propagationDelayType propagation delay type used in events computation
     */
    public void setPropagationDelayType(final PropagationDelayType propagationDelayType) {
        this.propagationDelayType = propagationDelayType;
    }
}
