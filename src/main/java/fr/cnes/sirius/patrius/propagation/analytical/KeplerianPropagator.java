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
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::FA:381:15/12/2014:Propagator tolerances and default mass issues
 * VERSION::DM:403:20/10/2015:Improving ergonomics
 * VERSION::DM:426:06/11/2015:Overloading method manageStateFrame()
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.analytical;

import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.propagation.AbstractPropagator;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * Simple keplerian orbit propagator.
 * 
 * @see Orbit
 * @author Guylaine Prat
 */
public class KeplerianPropagator extends AbstractPropagator {

    /** Serializable UID. */
    private static final long serialVersionUID = 2094439036855266946L;

    /** Initial state. */
    private SpacecraftState initialState;

    /**
     * Build a propagator from orbit only.
     * <p>
     * The central attraction coefficient &mu; is set to the same value used for the initial orbit definition. Mass and
     * attitude provider are set to null values.
     * </p>
     * 
     * @param initialOrbit
     *        initial orbit
     * @exception PropagationException
     *            if initial attitude cannot be computed
     */
    public KeplerianPropagator(final Orbit initialOrbit) throws PropagationException {
        this(initialOrbit, null, initialOrbit.getMu(), null);
    }

    /**
     * Build a propagator from orbit and central attraction coefficient &mu;.
     * <p>
     * Mass and attitude provider are set to null values.
     * </p>
     * 
     * @param initialOrbit
     *        initial orbit
     * @param mu
     *        central attraction coefficient (m^3/s^2)
     * @exception PropagationException
     *            if initial attitude cannot be computed
     */
    public KeplerianPropagator(final Orbit initialOrbit, final double mu) throws PropagationException {
        this(initialOrbit, null, mu, null);
    }

    /**
     * Build a propagator from orbit and a single attitude provider.
     * <p>
     * The central attraction coefficient &mu; is set to the same value used for the initial orbit definition. Mass is
     * set to null value.
     * </p>
     * 
     * @param initialOrbit
     *        initial orbit
     * @param attitudeProv
     *        attitude provider
     * @exception PropagationException
     *            if initial attitude cannot be computed
     */
    public KeplerianPropagator(final Orbit initialOrbit,
        final AttitudeProvider attitudeProv) throws PropagationException {
        this(initialOrbit, attitudeProv, initialOrbit.getMu(), null);
    }

    /**
     * Build a propagator from orbit and attitude provider for forces and event computation.
     * <p>
     * The central attraction coefficient &mu; is set to the same value used for the initial orbit definition. Mass is
     * set to null value.
     * </p>
     * 
     * @param initialOrbit
     *        initial orbit
     * @param attitudeProvForces
     *        attitude provider for forces computation
     * @param attitudeProvEvents
     *        attitude provider for events computation
     * @exception PropagationException
     *            if initial attitude cannot be computed
     */
    public KeplerianPropagator(final Orbit initialOrbit, final AttitudeProvider attitudeProvForces,
        final AttitudeProvider attitudeProvEvents) throws PropagationException {
        this(initialOrbit, attitudeProvForces, attitudeProvEvents, initialOrbit.getMu(), null);
    }

    /**
     * Build a propagator from orbit, a single attitude provider and central attraction
     * coefficient &mu;.
     * <p>
     * Mass is set to null value.
     * </p>
     * 
     * @param initialOrbit
     *        initial orbit
     * @param attitudeProv
     *        attitude provider
     * @param mu
     *        central attraction coefficient (m^3/s^2)
     * @exception PropagationException
     *            if initial attitude cannot be computed
     */
    public KeplerianPropagator(final Orbit initialOrbit, final AttitudeProvider attitudeProv,
        final double mu) throws PropagationException {
        this(initialOrbit, attitudeProv, mu, null);
    }

    /**
     * Build a propagator from orbit, attitude provider for forces and events computation and central attraction
     * coefficient &mu;.
     * <p>
     * Mass is set to null value.
     * </p>
     * 
     * @param initialOrbit
     *        initial orbit
     * @param attitudeProvForces
     *        attitude provider for forces computation
     * @param attitudeProvEvents
     *        attitude provider for events computation
     * @param mu
     *        central attraction coefficient (m^3/s^2)
     * @exception PropagationException
     *            if initial attitude cannot be computed
     */
    public KeplerianPropagator(final Orbit initialOrbit, final AttitudeProvider attitudeProvForces,
        final AttitudeProvider attitudeProvEvents,
        final double mu) throws PropagationException {
        this(initialOrbit, attitudeProvForces, attitudeProvEvents, mu, null);
    }

    /**
     * Build propagator from orbit, attitude provider, central attraction
     * coefficient &mu; and mass.
     * 
     * @param initialOrbit
     *        initial orbit
     * @param attitudeProv
     *        attitude provider
     * @param mu
     *        central attraction coefficient (m^3/s^2)
     * @param massProvider
     *        spacecraft mass (kg)
     * @exception PropagationException
     *            if initial attitude cannot be computed
     */
    public KeplerianPropagator(final Orbit initialOrbit, final AttitudeProvider attitudeProv,
        final double mu, final MassProvider massProvider) throws PropagationException {
        super(attitudeProv);

        try {
            // Treatment in case attitude provider is null
            Attitude attitude = null;
            if (this.getAttitudeProvider() != null) {
                attitude = this.getAttitudeProvider().getAttitude(initialOrbit);
            }
            this.resetInitialState(new SpacecraftState(initialOrbit, attitude, massProvider));
            this.addAdditionalStateProvider(massProvider);

        } catch (final PatriusException oe) {
            throw new PropagationException(oe);
        }
    }

    /**
     * Build propagator from orbit, attitude provider, central attraction
     * coefficient &mu; and mass.
     * 
     * @param initialOrbit
     *        initial orbit
     * @param attitudeProvForces
     *        attitude provider for forces computation
     * @param attitudeProvEvents
     *        attitude provider for events computation
     * @param mu
     *        central attraction coefficient (m^3/s^2)
     * @param massProvider
     *        spacecraft mass (kg)
     * @exception PropagationException
     *            if initial attitude cannot be computed
     */
    public KeplerianPropagator(final Orbit initialOrbit, final AttitudeProvider attitudeProvForces,
        final AttitudeProvider attitudeProvEvents, final double mu,
        final MassProvider massProvider) throws PropagationException {

        super(attitudeProvForces, attitudeProvEvents);

        try {
            // Treatment in case attitude providers are null
            Attitude attitudeForces = null;
            Attitude attitudeEvents = null;
            if (this.getAttitudeProviderForces() != null) {
                attitudeForces = attitudeProvForces.getAttitude(initialOrbit);
            }
            if (this.getAttitudeProviderEvents() != null) {
                attitudeEvents = attitudeProvEvents.getAttitude(initialOrbit);
            }
            this.resetInitialState(new SpacecraftState(initialOrbit, attitudeForces, attitudeEvents, massProvider));
            this.addAdditionalStateProvider(massProvider);
        } catch (final PatriusException oe) {
            throw new PropagationException(oe);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void resetInitialState(final SpacecraftState state) throws PropagationException {
        super.resetInitialState(state);
        this.initialState = state;
    }

    /** {@inheritDoc} */
    @Override
    protected void manageStateFrame() throws PatriusException {
        super.manageStateFrame();
        this.initialState = this.getInitialState();
    }

    /** {@inheritDoc} */
    @Override
    protected Orbit propagateOrbit(final AbsoluteDate date) throws PropagationException {

        Orbit orbit = this.initialState.getOrbit();

        // no change if the dates are equal
        if (date.equals(orbit.getDate())) {
            return orbit;
        }

        // propagate orbit
        do {
            // we use a loop here to compensate for very small date shifts error
            // that occur with long propagation time
            orbit = orbit.shiftedBy(date.durationFrom(orbit.getDate()));

        } while (!date.equals(orbit.getDate()));

        return orbit;
    }
}
