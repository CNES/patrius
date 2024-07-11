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
 * @history created 04/03/2015
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:317:04/03/2015: STELA integration in CIRF with referential choice (ICRF, CIRF or MOD)
 * VERSION::DM:605:30/09/2016:gathered Meeus models
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.propagation;

import fr.cnes.sirius.patrius.bodies.MeeusSun;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusStepHandler;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusStepInterpolator;
import fr.cnes.sirius.patrius.stela.bodies.MeeusMoonStela;
import fr.cnes.sirius.patrius.stela.forces.noninertial.NonInertialContribution;
import fr.cnes.sirius.patrius.stela.orbits.OrbitNatureConverter;
import fr.cnes.sirius.patrius.stela.orbits.StelaEquinoctialOrbit;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * Step handler handling forces requiring to be updated every step and not every substep.
 * 
 * @author Emmanuel Bignon
 * @concurrency not thread-safe
 * @version $Id$
 * @since 3.0
 * 
 */
public class ForcesStepHandler implements PatriusStepHandler {

    /** UID. */
    private static final long serialVersionUID = 6884459947160793318L;

    /** Orbit nature converter. */
    private final OrbitNatureConverter converter;

    /** Non-inertial contribution. */
    private final NonInertialContribution nonInertialContribution;

    /** Non-inertial contribution (value). */
    private double[] dnonInertial = new double[6];

    /**
     * Constructor.
     * 
     * @param converterIn
     *        orbit nature converter
     * @param nonInertialContributionIn
     *        nonInertialContribution force (null if not used)
     */
    public ForcesStepHandler(final OrbitNatureConverter converterIn,
        final NonInertialContribution nonInertialContributionIn) {
        this.nonInertialContribution = nonInertialContributionIn;
        this.converter = converterIn;
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        try {
            this.compute(s0);
        } catch (final PatriusException e) {
            // Not possible to throw any other exception with additional information
            throw new PatriusRuntimeException(PatriusMessages.INTERNAL_ERROR, e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void handleStep(final PatriusStepInterpolator interpolator,
                           final boolean isLast) throws PropagationException {
        if (!isLast) {
            try {
                this.compute(interpolator.getInterpolatedState());
            } catch (final PatriusException e) {
                throw new PropagationException(e);
            }
        }
    }

    /**
     * Compute forces that need to be computed.
     * 
     * @param state
     *        a state
     * @throws PatriusException
     *         thrown if non-inertial contribution computation failed
     */
    private void compute(final SpacecraftState state) throws PatriusException {

        // Initialization
        final StelaEquinoctialOrbit orbit = (StelaEquinoctialOrbit) state.getOrbit();

        // Sun/Moon transform update
        MeeusMoonStela.updateTransform(orbit.getDate(), FramesFactory.getCIRF());
        MeeusSun.updateTransform(orbit.getDate(), FramesFactory.getCIRF());

        // Non-inertial contribution
        if (this.nonInertialContribution != null) {
            this.dnonInertial = this.nonInertialContribution.computePerturbation(orbit, this.converter);
        }
    }

    /**
     * Getter for non-inertial contribution.
     * 
     * @return non-inertial contribution
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public double[] getDnonInertial() {
        return this.dnonInertial;
    }
}
