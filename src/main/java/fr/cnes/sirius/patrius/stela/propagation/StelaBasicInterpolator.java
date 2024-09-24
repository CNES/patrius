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
 * @history created on 18/03/2013
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2082:15/05/2019:Modifications mineures d'api
 * VERSION:4.3:DM:DM-2102:15/05/2019:[Patrius] Refactoring du paquet fr.cnes.sirius.patrius.bodies
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:484:25/09/2015:Get additional state from an AbsoluteDate
 * VERSION::FA:449:21/12/2015:Changes in attitude handling
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.propagation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.propagation.AdditionalStateProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusStepInterpolator;
import fr.cnes.sirius.patrius.stela.orbits.StelaEquinoctialOrbit;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * <p>
 * Basic Linear Step Interpolator for StelaAbstractPropagator. Does not interpolate the attitude of the spacecraft at
 * the moment.
 * </p>
 * 
 * @concurrency not thread-safe
 * 
 * @author Cedric Dental
 * 
 * @version 1.3
 * 
 * @since 1.3
 * 
 */
public class StelaBasicInterpolator implements PatriusStepInterpolator {

    /** Serializable UID. */
    private static final long serialVersionUID = 26269718303505539L;

    /** Previous date. */
    private AbsoluteDate previousDate;

    /** Current date. */
    private AbsoluteDate currentDate;

    /** Interpolated state. */
    private SpacecraftState interpolatedState;
    /** Initial state. */
    private SpacecraftState initialState;
    /** Interpolated state. */
    private SpacecraftState currentState;

    /** Forward propagation indicator. */
    private boolean forward;
    /** Additional state providers. */
    private List<AdditionalStateProvider> additionalStateProviders;

    /**
     * Build a new instance from a basic propagator.
     */
    public StelaBasicInterpolator() {
        this.previousDate = AbsoluteDate.PAST_INFINITY;
        this.currentDate = AbsoluteDate.PAST_INFINITY;
        this.additionalStateProviders = new ArrayList<>();
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getCurrentDate() {
        return this.currentDate;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getInterpolatedDate() {
        return this.interpolatedState.getDate();
    }

    /** {@inheritDoc} */
    @Override
    public SpacecraftState getInterpolatedState() throws PatriusException {
        return this.interpolatedState;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getPreviousDate() {
        return this.previousDate;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isForward() {
        return this.forward;
    }

    /**
     * sets the additionalStateProviders
     * 
     * @param additionalStateProvidersIn
     *        the list of additional State providers
     */
    public void setAdditionalStateProviders(final List<AdditionalStateProvider> additionalStateProvidersIn) {
        final List<AdditionalStateProvider> additionalStateProviders2 = new ArrayList<>();
        additionalStateProviders2.addAll(additionalStateProvidersIn);
        this.additionalStateProviders.clear();
        this.additionalStateProviders = additionalStateProviders2;
    }

    /** {@inheritDoc} */
    @Override
    public void setInterpolatedDate(final AbsoluteDate interpolatedDate)
                                                                        throws PropagationException, PatriusException {

        if (interpolatedDate.compareTo(this.previousDate) == 0) {
            // does not interpolate for known value
            this.interpolatedState = this.initialState;
        } else if (interpolatedDate.compareTo(this.currentDate) == 0) {
            // does not interpolate for known value
            this.interpolatedState = this.currentState;
        } else {
            // compute the interpolated orbit and attitude
            final double linearCoeff;
            if (this.currentDate.compareTo(this.previousDate) == 0) {

                linearCoeff = 0;
            } else {
                linearCoeff =
                    (interpolatedDate.durationFrom(this.previousDate))
                        / (this.currentDate.durationFrom(this.previousDate));
            }

            // Orbit interpolation
            final StelaEquinoctialOrbit before = new StelaEquinoctialOrbit(this.initialState.getOrbit());
            final StelaEquinoctialOrbit after = new StelaEquinoctialOrbit(this.currentState.getOrbit());
            final StelaEquinoctialOrbit interpolatedOrbit = new StelaEquinoctialOrbit(
                this.linearInterpolation(linearCoeff, before.getA(), after.getA()),
                this.linearInterpolation(linearCoeff, before.getEquinoctialEx(), after.getEquinoctialEx()),
                this.linearInterpolation(linearCoeff, before.getEquinoctialEy(), after.getEquinoctialEy()),
                this.linearInterpolation(linearCoeff, before.getIx(), after.getIx()),
                this.linearInterpolation(linearCoeff, before.getIy(), after.getIy()),
                this.linearInterpolation(linearCoeff, before.getLM(), after.getLM()),
                before.getFrame(), interpolatedDate, this.linearInterpolation(linearCoeff, before.getMu(),
                    after.getMu()));
            // Attitude interpolation if necessary
            final Attitude interpolatedAttitudeForces;
            final Attitude interpolatedAttitudeEvents;
            if (this.initialState.getAttitudeForces() == null) {
                interpolatedAttitudeForces = null;
            } else {
                interpolatedAttitudeForces = new Attitude(interpolatedDate, this.initialState.getAttitudeForces()
                    .getReferenceFrame(), this.initialState.getAttitudeForces().getOrientation());
            }
            if (this.initialState.getAttitudeEvents() == null) {
                interpolatedAttitudeEvents = null;
            } else {
                interpolatedAttitudeEvents = new Attitude(interpolatedDate, this.initialState.getAttitudeEvents()
                    .getReferenceFrame(), this.initialState.getAttitudeEvents().getOrientation());
            }
            // compute additional states
            final SpacecraftState temp = new SpacecraftState(interpolatedOrbit, interpolatedAttitudeForces,
                interpolatedAttitudeEvents);
            final Map<String, double[]> additionalStates = new ConcurrentHashMap<>();
            for (final AdditionalStateProvider provider : this.additionalStateProviders) {
                additionalStates.put(provider.getName(), provider.getAdditionalState(temp.getDate()));
            }

            this.interpolatedState = new SpacecraftState(interpolatedOrbit, interpolatedAttitudeForces,
                interpolatedAttitudeEvents, additionalStates);
        }
    }

    /**
     * 
     * Interpolates lineary
     * 
     * 
     * @param linearCoeff
     *        the linear coefficient
     * @param before
     *        the previous state
     * @param after
     *        the current state
     * @return inter
     *         the interpolated value
     */
    public double linearInterpolation(final double linearCoeff, final double before, final double after) {
        return linearCoeff * (after - before) + before;
    }

    /**
     * Store the current dates and spacecraft states.
     * 
     * @param initialStateIn
     *        the initial state
     * @param targetState
     *        the target state
     * @exception PropagationException
     *            if the state cannot be propagated at specified date
     */
    public void storeSC(final SpacecraftState initialStateIn,
                        final SpacecraftState targetState) throws PropagationException {
        this.previousDate = initialStateIn.getDate();
        this.currentDate = targetState.getDate();
        this.forward = this.currentDate.compareTo(this.previousDate) >= 0;

        this.initialState = initialStateIn;
        this.currentState = targetState;

    }

    /**
     * @return the initialState
     */
    public SpacecraftState getInitialState() {
        return this.initialState;
    }

}
