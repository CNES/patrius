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
$ * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
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
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.analytical;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.propagation.AbstractPropagator;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * Orbit propagator that adapts an underlying propagator, adding {@link DifferentialEffect differential effects}.
 * <p>
 * This propagator is used when a reference propagator does not handle some effects that we need. A typical example
 * would be an ephemeris that was computed for a reference orbit, and we want to compute a station-keeping maneuver on
 * top of this ephemeris, changing its final state. The principal is to add one or more
 * {@link fr.cnes.sirius.patrius.forces.maneuvers.SmallManeuverAnalyticalModel small maneuvers
 * analytical models} to it and use it as a new propagator, which takes the maneuvers into account.
 * </p>
 * <p>
 * From a space flight dynamics point of view, this is a differential correction approach. From a computer science point
 * of view, this is a use of the decorator design pattern.
 * </p>
 * 
 * @see Propagator
 * @see fr.cnes.sirius.patrius.forces.maneuvers.SmallManeuverAnalyticalModel
 * @author Luc Maisonobe
 */
public class AdapterPropagator extends AbstractPropagator {

    /** Serializable UID. */
    private static final long serialVersionUID = -5953975769121996528L;

    /** Underlying reference propagator. */
    private final Propagator reference;

    /** Effects to add. */
    private final List<DifferentialEffect> effects;

    /**
     * Build a propagator from an underlying reference propagator.
     * <p>
     * The reference propagator can be almost anything, numerical, analytical, and even an ephemeris. It may already
     * take some maneuvers into account.
     * </p>
     * 
     * @param referenceIn
     *        reference propagator
     */
    public AdapterPropagator(final Propagator referenceIn) {
        super(referenceIn.getAttitudeProvider());
        this.reference = referenceIn;
        this.effects = new ArrayList<>();
    }

    /**
     * Add a differential effect.
     * 
     * @param effect
     *        differential effect
     */
    public void addEffect(final DifferentialEffect effect) {
        this.effects.add(effect);
    }

    /**
     * Get the reference propagator.
     * 
     * @return reference propagator
     */
    public Propagator getPropagator() {
        return this.reference;
    }

    /**
     * Get the differential effects.
     * 
     * @return differential effects models, as an unmodifiable list
     */
    public List<DifferentialEffect> getEffects() {
        return Collections.unmodifiableList(this.effects);
    }

    /** {@inheritDoc} */
    @Override
    public SpacecraftState getInitialState() throws PatriusException {
        return this.reference.getInitialState();
    }

    /** {@inheritDoc} */
    @Override
    public void resetInitialState(final SpacecraftState state) throws PropagationException {
        this.reference.resetInitialState(state);
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings({"PMD.AvoidRethrowingException", "PMD.PreserveStackTrace"})
    protected SpacecraftState basicPropagate(final AbsoluteDate date) throws PropagationException {

        try {
            // compute reference state
            SpacecraftState state = this.reference.propagate(date);

            // add all the effects
            for (final DifferentialEffect effect : this.effects) {
                state = effect.apply(state);
            }

            return state;
        } catch (final PatriusExceptionWrapper oew) {
            if (oew.getException() instanceof PropagationException) {
                // Throw the wrapped exception
                throw (PropagationException) oew.getException();
            }
            // Create new PropagationException from wrapped exception
            throw new PropagationException(oew.getException());
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

    /** Interface for orbit differential effects. */
    public interface DifferentialEffect {

        /**
         * Apply the effect to a {@link SpacecraftState spacecraft state}.
         * <p>
         * Applying the effect may be a no-op in some cases. A typical example is maneuvers, for which the state is
         * changed only for time <em>after</em> the maneuver occurrence.
         * </p>
         * 
         * @param original
         *        original state <em>without</em> the effect
         * @return updated state at the same date, taking the effect
         *         into account if meaningful
         * @exception PatriusException
         *            if effect cannot be computed
         */
        SpacecraftState apply(SpacecraftState original) throws PatriusException;
    }
}
