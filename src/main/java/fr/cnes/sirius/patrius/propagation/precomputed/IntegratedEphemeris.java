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
* VERSION:4.8:FA:FA-2941:15/11/2021:[PATRIUS] Correction anomalies suite a DM 2767 
* VERSION:4.7:DM:DM-2767:18/05/2021:Evolutions et corrections diverses 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::FA:381:15/12/2014:Propagator tolerances and default mass issues
 * VERSION::DM:300:18/03/2015:Creation multi propagator (Replaced AdditionalStateData by AdditionalStateInfo)
 * VERSION::FA:476:06/10/2015:Propagation until final date in ephemeris mode
 * VERSION::DM:426:06/11/2015:set propagation frame
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.precomputed;

import java.util.List;
import java.util.Map;

import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.ode.ContinuousOutputModel;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.AbstractPropagator;
import fr.cnes.sirius.patrius.propagation.BoundedPropagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.AdditionalStateInfo;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * This class stores sequentially generated orbital parameters for
 * later retrieval.
 * 
 * <p>
 * Instances of this class are built and then must be fed with the results provided by
 * {@link fr.cnes.sirius.patrius.propagation.Propagator Propagator} objects configured in
 * {@link fr.cnes.sirius.patrius.propagation.Propagator#setEphemerisMode()
 * ephemeris generation mode}. Once propagation is o, random access to any intermediate state of the orbit throughout
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
 * @see fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator
 * @author Mathieu Rom&eacute;ro
 * @author Luc Maisonobe
 * @author V&eacute;ronique Pommier-Maurussane
 */
public class IntegratedEphemeris
    extends AbstractPropagator implements BoundedPropagator {

    /** Serializable UID. */
    private static final long serialVersionUID = -2135002726640830424L;

    /** Propagation orbit type. */
    private final OrbitType orbitType;

    /** Position angle type. */
    private final PositionAngle angleType;

    /** Reference frame. */
    private final Frame referenceFrame;

    /** Additional states informations. */
    private final Map<String, AdditionalStateInfo> addStateInfos;

    /** Central body gravitational constant. */
    private final double mu;

    /** List of start dates of the integration (can be min or max). */
    private final List<AbsoluteDate> startDates;

    /** List of first date of the ranges. */
    private final List<AbsoluteDate> minDates;

    /** List of last date of the ranges. */
    private final List<AbsoluteDate> maxDates;

    /** List of underlying raw mathematical models. */
    private final List<ContinuousOutputModel> models;

    /**
     * Creates a new instance of IntegratedEphemeris.
     * 
     * @param startDatesIn
     *        list of start dates of the integration (can be minDate or maxDate)
     * @param minDatesIn
     *        list of first dates of the ranges
     * @param maxDatesIn
     *        list of last dates of the ranges
     * @param orbitTypeIn
     *        orbit type
     * @param angleTypeIn
     *        position angle type
     * @param attitudeForcesProvider
     *        attitude provider for forces computation
     * @param attitudeEventsProvider
     *        attitude provider for events computation
     * @param additionalStateInfos
     *        additional states informations
     * @param modelsIn
     *        list of underlying raw mathematical models
     * @param referenceFrameIn
     *        reference referenceFrame
     * @param muIn
     *        central body attraction coefficient
     * @exception PatriusException
     *            if several providers have the same name
     */
    public IntegratedEphemeris(final List<AbsoluteDate> startDatesIn,
        final List<AbsoluteDate> minDatesIn, final List<AbsoluteDate> maxDatesIn,
        final OrbitType orbitTypeIn, final PositionAngle angleTypeIn,
        final AttitudeProvider attitudeForcesProvider,
        final AttitudeProvider attitudeEventsProvider,
        final Map<String, AdditionalStateInfo> additionalStateInfos,
        final List<ContinuousOutputModel> modelsIn, final Frame referenceFrameIn,
        final double muIn) throws PatriusException {

        super(attitudeForcesProvider, attitudeEventsProvider);

        this.startDates = startDatesIn;
        this.minDates = minDatesIn;
        this.maxDates = maxDatesIn;
        this.orbitType = orbitTypeIn;
        this.angleType = angleTypeIn;
        this.addStateInfos = additionalStateInfos;
        this.models = modelsIn;
        this.referenceFrame = referenceFrameIn;
        this.mu = muIn;
        this.setOrbitFrame(referenceFrameIn);
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
            throw new PropagationException(PatriusMessages.OUT_OF_RANGE_EPHEMERIDES_DATE,
                date, this.getMinDate(), this.getMaxDate());
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
     *        index of leg valid for required date
     */
    private void setInterpolationDate(final AbsoluteDate date, final int index) {
        // Reset interpolation model to the desired date
        this.models.get(index).setInterpolatedTime(date.durationFrom(this.startDates.get(index)));
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings({"PMD.AvoidRethrowingException", "PMD.PreserveStackTrace"})
    protected SpacecraftState basicPropagate(final AbsoluteDate date) throws PropagationException {
        try {
            // Get leg
            final int index = this.getLeg(date);
            // Interpolate
            this.setInterpolationDate(date, index);
            final double[] y = this.models.get(index).getInterpolatedState();
            return new SpacecraftState(y, this.orbitType, this.angleType, date, this.mu, this.referenceFrame,
                this.addStateInfos,
                this.getAttitudeProviderForces(), this.getAttitudeProviderEvents());
        } catch (final PatriusExceptionWrapper oew) {
            // Exception
            if (oew.getException() instanceof PropagationException) {
                throw (PropagationException) oew.getException();
            } else {
                throw new PropagationException(oew.getException());
            }
        } catch (final PropagationException oe) {
            throw oe;
        }
    }

    /**
     * In this class, nothing as to be done in the frame managing before propagation
     * because propagation will be performed in Frame referenceFrame
     * It just throws an OrekitException if this frame is non inertial or pseudo-inertial.
     * 
     * @throws PatriusException
     *         if the frame is non inertial or pseudo-inertial
     */
    @Override
    public void manageStateFrame() throws PatriusException {
        if (!this.referenceFrame.isPseudoInertial()) {
            throw new PatriusException(PatriusMessages.NOT_INERTIAL_FRAME);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected Orbit propagateOrbit(final AbsoluteDate date) throws PropagationException {
        return this.basicPropagate(date).getOrbit();
    }

    /** {@inheritDoc} */
    @Override
    public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
        return this.basicPropagate(date).getPVCoordinates(frame);
    }

    /**
     * Get the first date of the range.
     * 
     * @return the first date of the range
     */
    @Override
    public AbsoluteDate getMinDate() {
        return this.minDates.get(0);
    }

    /**
     * Get the last date of the range.
     * 
     * @return the last date of the range
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

    /** {@inheritDoc} */
    @Override
    public SpacecraftState getSpacecraftState(final AbsoluteDate date) throws PropagationException {
        return this.basicPropagate(date);
    }
}
