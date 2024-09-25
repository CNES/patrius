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
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:300:18/03/2015:Creation multi propagator
 * VERSION::DM:426:06/11/2015:set propagation frame
 * VERSION::FA:476:06/10/2015:Propagation until final date in ephemeris mode
 * VERSION::DM:1872:10/12/2018:Substitution of AttitudeProvider with MultiAttitudeProvider
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.precomputed.multi;

import java.util.List;

import fr.cnes.sirius.patrius.attitudes.multi.MultiAttitudeProvider;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.ode.ContinuousOutputModel;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.AbstractPropagator;
import fr.cnes.sirius.patrius.propagation.BoundedPropagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.multi.MultiStateVectorInfo;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * <p>
 * This interface is copied from IntegratedEphemeris and adapted to multi propagation.
 * </p>
 * <p>
 * This class stores sequentially generated orbital parameters for later retrieval.
 * </p>
 * <p>
 * Instances of this class are built and then must be fed with the results provided by
 * {@link fr.cnes.sirius.patrius.propagation.MultiPropagator MultiPropagator} objects configured in
 * {@link fr.cnes.sirius.patrius.propagation.MultiPropagator#setEphemerisMode()
 * ephemeris generation mode}. Once propagation is over, random access to any intermediate state of the orbit throughout
 * the propagation range is possible.
 * </p>
 * <p>
 * A typical use case is for numerically integrated orbits, which can be used by algorithms that need to wander around
 * according to their own algorithm without cumbersome tight links with the integrator.
 * </p>
 * <p>
 * Another use case is for persistence, as this class is serializable.
 * </p>
 * <p>
 * As this class implements the {@link fr.cnes.sirius.patrius.propagation.Propagator Propagator} interface, it can
 * itself be used in batch mode to build another instance of the same type. This is however not recommended since it
 * would be a waste of resources.
 * </p>
 * <p>
 * Note that this class stores all intermediate states along with interpolation models, so it may be memory intensive.
 * </p>
 *
 * @concurrency immutable
 *
 * @author maggioranic
 *
 * @version $Id$
 *
 * @since 3.0
 *
 */
public class MultiIntegratedEphemeris extends AbstractPropagator implements BoundedPropagator {

    /** Serializable UID. */
    private static final long serialVersionUID = 2114446502509891643L;

    /** Propagation orbit type. */
    private final OrbitType orbitType;

    /** Position angle type. */
    private final PositionAngle angleType;

    /** State vector informations */
    private final MultiStateVectorInfo stateVectorInfo;

    /** List of start dates of the integration (can be min or max). */
    private final List<AbsoluteDate> startDates;

    /** List of first date of the ranges. */
    private final List<AbsoluteDate> minDates;

    /** List of last date of the ranges. */
    private final List<AbsoluteDate> maxDates;

    /** List of underlying raw mathematical models. */
    private final List<ContinuousOutputModel> models;

    /** Sat id. */
    private final String satId;

    /** MultiAttitudeProvide for forces */
    private final MultiAttitudeProvider multiAttitudeProviderForces;

    /** MultiAttitudeProvide for events */
    private final MultiAttitudeProvider multiAttitudeProviderEvents;

    /**
     * Creates a new instance of IntegratedEphemeris.
     *
     * @param startDatesIn
     *        Start dates of the integration (can be minDate or maxDate)
     * @param minDatesIn
     *        first dates of the range
     * @param maxDatesIn
     *        last dates of the range
     * @param orbitTypeIn
     *        orbit type
     * @param angleTypeIn
     *        position angle type
     * @param multiAttitudeProviderForcesIn
     *        attitude provider for forces computation
     * @param multiAttitudeProviderEventsIn
     *        attitude provider for events computation
     * @param stateInfos
     *        state vector infos
     * @param modelsIn
     *        underlying raw mathematical models
     * @param referenceFrameIn
     *        reference referenceFrame
     * @param satIdIn
     *        spacecraft id
     * @exception PatriusException
     *            if several providers have the same name
     */
    public MultiIntegratedEphemeris(final List<AbsoluteDate> startDatesIn,
        final List<AbsoluteDate> minDatesIn, final List<AbsoluteDate> maxDatesIn,
        final OrbitType orbitTypeIn, final PositionAngle angleTypeIn,
        final MultiAttitudeProvider multiAttitudeProviderForcesIn,
        final MultiAttitudeProvider multiAttitudeProviderEventsIn,
        final MultiStateVectorInfo stateInfos, final List<ContinuousOutputModel> modelsIn,
        final Frame referenceFrameIn, final String satIdIn) throws PatriusException {
        super(null, null);

        this.startDates = startDatesIn;
        this.minDates = minDatesIn;
        this.maxDates = maxDatesIn;
        this.orbitType = orbitTypeIn;
        this.angleType = angleTypeIn;
        this.stateVectorInfo = stateInfos;
        this.models = modelsIn;
        this.satId = satIdIn;
        this.setOrbitFrame(referenceFrameIn);
        this.multiAttitudeProviderEvents = multiAttitudeProviderEventsIn;
        this.multiAttitudeProviderForces = multiAttitudeProviderForcesIn;
    }

    /**
     * Get leg valid for required date.
     *
     * @param date
     *        desired interpolation date
     * @return index of leg valid for required date
     * @exception PropagationException
     *            if specified date is outside of supported range
     */
    private int getLeg(final AbsoluteDate date) throws PropagationException {

        if ((date.compareTo(this.getMinDate()) < 0) || (date.compareTo(this.getMaxDate()) > 0)) {
            // Date is outside of supported range
            throw new PropagationException(PatriusMessages.OUT_OF_RANGE_EPHEMERIDES_DATE, date,
                this.getMinDate(), this.getMaxDate());
        }

        // Get leg valid for required date
        int index = this.minDates.size() - 1;
        while (!(date.compareTo(this.minDates.get(index)) >= 0 && date.compareTo(this.maxDates.get(index)) <= 0)) {
            index--;
        }

        return index;
    }

    /**
     * Set up the model at some interpolation date.
     *
     * @param date
     *        desired interpolation date
     * @param index
     *        date and model index
     */
    private void setInterpolationDate(final AbsoluteDate date, final int index) {
        // reset interpolation model to the desired date
        this.models.get(index).setInterpolatedTime(date.durationFrom(this.startDates.get(index)));
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings({"PMD.AvoidRethrowingException", "PMD.PreserveStackTrace"})
    protected SpacecraftState basicPropagate(final AbsoluteDate date) throws PropagationException {
        try {
            // Get interpolated state in array form
            final int index = this.getLeg(date);
            this.setInterpolationDate(date, index);
            final double[] y = this.models.get(index).getInterpolatedState();
            // Get interpolated SpacecraftState
            return this.stateVectorInfo.mapArrayToState(y, date, this.orbitType, this.angleType,
                this.multiAttitudeProviderForces, this.multiAttitudeProviderEvents, this.satId);
        } catch (final PatriusExceptionWrapper oew) {
            // Exception management
            if (oew.getException() instanceof PropagationException) {
                throw (PropagationException) oew.getException();
            } else {
                throw new PropagationException(oew.getException());
            }
        } catch (final PropagationException oe) {
            throw oe;
        } catch (final PatriusException oe) {
            throw new PropagationException(oe);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected Orbit propagateOrbit(final AbsoluteDate date) throws PropagationException {
        return this.basicPropagate(date).getOrbit();
    }

    /** {@inheritDoc} */
    @Override
    public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame)
                                                                                     throws PatriusException {
        return this.propagate(date).getPVCoordinates(frame);
    }

    /**
     * {@inheritDoc}
     *
     * @see fr.cnes.sirius.patrius.propagation.BoundedPropagator#getMinDate()
     */
    @Override
    public AbsoluteDate getMinDate() {
        return this.minDates.get(0);
    }

    /**
     * {@inheritDoc}
     *
     * @see fr.cnes.sirius.patrius.propagation.BoundedPropagator#getMaxDate()
     */
    @Override
    public AbsoluteDate getMaxDate() {
        return this.maxDates.get(this.maxDates.size() - 1);
    }

    /** {@inheritDoc} */
    @Override
    public void resetInitialState(final SpacecraftState state) throws PropagationException {
        throw new PropagationException(PatriusMessages.NON_RESETABLE_STATE);
    }

    /** {@inheritDoc} */
    @Override
    public SpacecraftState getInitialState() throws PatriusException {
        return this.basicPropagate(this.getMinDate());
    }
}
