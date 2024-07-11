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
 * @history Created 25/02/2013
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:106:16/07/2013:Account of changes to DirectRadiativeModel, no exception thrown.
 * VERSION::DM:200:28/08/2014: dealing with a negative mass in the propagator
 * VERSION::FA:358:09/03/2015:proper handling of vehicle negative surface
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.forces.radiation;

import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.models.DirectRadiativeModel;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.stela.StelaSpacecraftFactory;
import fr.cnes.sirius.patrius.stela.forces.AbstractStelaGaussContribution;
import fr.cnes.sirius.patrius.stela.orbits.OrbitNatureConverter;
import fr.cnes.sirius.patrius.stela.orbits.StelaEquinoctialOrbit;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class represents the Stela SRP model, which computes perturbations using the squaring method and the partial
 * derivatives using the potential approximation.
 * 
 * @see SRPSquaring
 * @see SRPPotential
 * 
 * @concurrency conditionally thread-safe
 * @concurrency.comment thread-safe if the SRPSquaring and SRPPotential models are thread-safe.
 * 
 * @author Rami Houdroge
 * 
 * @version $Id$
 * 
 * @since 1.3
 * 
 */
public class StelaSRPSquaring extends AbstractStelaGaussContribution {

     /** Serializable UID. */
    private static final long serialVersionUID = 4172315976813551789L;

    /** Squaring model */
    private final SRPSquaring squaring;
    /** Potential model */
    private final SRPPotential potential;

    /**
     * Create an instance of the SRP force Stela model.
     * 
     * @param mass
     *        mass of spacecraft
     * @param surface
     *        radiative surface of spacecraft
     * @param reflectionCoef
     *        reflection coefficient of spacecraft
     * @param quadraturePoints
     *        number of quadrature points
     * @param sunBody
     *        sun as a celestialbody
     * @throws PatriusException
     *         if the mass is negative (PatriusMessages.MASS_ARGUMENT_IS_NEGATIVE)
     */
    public StelaSRPSquaring(final double mass, final double surface, final double reflectionCoef,
        final int quadraturePoints, final CelestialBody sunBody) throws PatriusException {
        super();
        final Assembly radiativeSpacecraft = getSpacecraft(mass, surface, reflectionCoef);
        this.squaring = new SRPSquaring(new DirectRadiativeModel(radiativeSpacecraft), quadraturePoints, sunBody,
            Constants.CNES_STELA_AE);
        this.potential = new SRPPotential(sunBody, mass, surface, reflectionCoef);
    }

    /**
     * Create an instance of the SRP force Stela model.
     * 
     * @param mass
     *        mass of spacecraft
     * @param surface
     *        radiative surface of spacecraft
     * @param reflectionCoef
     *        reflection coefficient of spacecraft
     * @param sunBody
     *        sun as a celestialbody
     * @param quadraturePoints
     *        number of quadrature points
     * @param earthRadius
     *        earth radius
     * @param dRef
     *        reference distant
     * @param pRef
     *        reference solar pressure at dRef
     * @throws PatriusException
     *         if the mass is negative (PatriusMessages.MASS_ARGUMENT_IS_NEGATIVE)
     */
    public StelaSRPSquaring(final double mass, final double surface, final double reflectionCoef,
        final int quadraturePoints, final CelestialBody sunBody, final double earthRadius, final double dRef,
        final double pRef) throws PatriusException {
        super();
        final Assembly radiativeSpacecraft = getSpacecraft(mass, surface, reflectionCoef);
        this.squaring = new SRPSquaring(new DirectRadiativeModel(radiativeSpacecraft),
            quadraturePoints, sunBody, earthRadius, dRef, pRef);
        this.potential = new SRPPotential(sunBody, mass, surface, reflectionCoef);
    }

    /**
     * Get a Stela spherical spacecraft
     * 
     * @param mass
     *        mass
     * @param surface
     *        surface
     * @param reflectionCoef
     *        reflection coefficient
     * @return a spherical assembly
     * @throws PatriusException
     *         if the mass is negative (PatriusMessages.MASS_ARGUMENT_IS_NEGATIVE)
     */
    private static Assembly getSpacecraft(final double mass, final double surface,
                                   final double reflectionCoef) throws PatriusException {
        return StelaSpacecraftFactory.createStelaRadiativeSpacecraft("main", mass,
            surface, reflectionCoef);
    }

    /** {@inheritDoc} */
    @Override
    public double[] computeShortPeriods(final StelaEquinoctialOrbit orbit) throws PatriusException {
        return new double[6];
    }

    /** {@inheritDoc} */
    @Override
    public double[][] computePartialDerivatives(final StelaEquinoctialOrbit orbit) throws PatriusException {
        return this.potential.computePartialDerivatives(orbit);
    }

    /** {@inheritDoc} */
    @Override
    public double[] computePerturbation(final StelaEquinoctialOrbit orbit,
                                        final OrbitNatureConverter converter) throws PatriusException {
        this.dPert = this.squaring.computePerturbation(orbit, converter);
        return this.dPert;
    }

    /**
     * @param orbit
     *        the stela equinoctial orbit
     * @return the potential perturbation
     * @throws PatriusException
     *         the Orekit exception
     */
    public double[] computePotentialPerturbation(final StelaEquinoctialOrbit orbit) throws PatriusException {
        return this.potential.computePerturbation(orbit);
    }

}
