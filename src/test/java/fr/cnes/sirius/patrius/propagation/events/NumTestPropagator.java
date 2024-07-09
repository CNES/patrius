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
 * @history creation 24/03/2015
 *
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:414:24/03/2015: proper handling of mass evolution
 * VERSION::FA:673:12/09/2016: add getTotalMass(state)
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import java.io.IOException;
import java.text.ParseException;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.forces.gravity.DrozinerAttractionModel;
import fr.cnes.sirius.patrius.forces.gravity.potential.GRGSFormatReader;
import fr.cnes.sirius.patrius.forces.gravity.potential.GravityFieldFactory;
import fr.cnes.sirius.patrius.forces.gravity.potential.PotentialCoefficientsProvider;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * propagator set-up for mass test.
 */
public class NumTestPropagator extends NumericalPropagator {

    private static final long serialVersionUID = -2902349006522333152L;

    /**
     * Build a numerical propagator
     */
    public NumTestPropagator(final Orbit initialOrbit, final Assembly spacecraft, final MassProvider massProvider,
        final AttitudeProvider attProv) throws PatriusException, IOException, ParseException {

        /* 0) Creation initiale */
        super(createIntegrator());

        /* 1) Init de l'acces aux donnees d'environnement */
        Utils.setDataRoot("regular-dataPBASE");

        /* 3) Gestion de la masse : fourniture du mass model au propagateur */
        this.setMassProviderEquation(massProvider);
        // Donner les valeurs de tolérance pour l'intégration des états additionnels
        // Ici trois parts ont la propriété de masse
        this.setAdditionalStateTolerance("MASS_BODY", new double[] { 1e-06 }, new double[] { 1e-09 });
        this.setAdditionalStateTolerance("MASS_Reservoir1", new double[] { 1e-06 }, new double[] { 1e-09 });
        this.setAdditionalStateTolerance("MASS_Reservoir2", new double[] { 1e-06 }, new double[] { 1e-09 });

        // f) Add these force models to the propagator
        this.addForceModel(createEarthPotential());

        /* 5) Ajout de l'attitude, calcul de l'attitude initiale */
        this.setAttitudeProvider(attProv);
        final Attitude initAtt = attProv.getAttitude(initialOrbit, initialOrbit.getDate(), initialOrbit.getFrame());

        /* 6) Initialisation de l'état : fournir un état complet Orbite, Attitude, Masse via massProvider */
        this.setInitialState(new SpacecraftState(initialOrbit, initAtt, massProvider));

    }

    /**
     * Creation d'un integrateur numerique avec tolerances absolues reglees a 1e-5 m / 1e-8 m.s-1 et
     * tolerances relatives reglees a 1e-12 Le pas peut varier entre 0.1 et 500 s
     * 
     * @return un integrateur premier ordre
     */
    private static final FirstOrderIntegrator createIntegrator() {
        final double[] absTOL = { 1e-5, 1e-5, 1e-5, 1e-8, 1e-8, 1e-8 };
        final double[] relTOL = { 1e-12, 1e-12, 1e-12, 1e-12, 1e-12, 1e-12 };
        return new DormandPrince853Integrator(1e-5, 500, absTOL, relTOL);
    }

    /**
     * Create an Earth potential force model.
     * 
     * @return a force model
     * @throws IOException
     * @throws ParseException
     * @throws PatriusException
     */
    private static final ForceModel createEarthPotential() throws IOException, ParseException,
                                                          PatriusException {
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));
        final PotentialCoefficientsProvider provider = GravityFieldFactory.getPotentialProvider();
        final double[][] C = provider.getC(3, 3, false);
        final double[][] S = provider.getS(3, 3, false);
        return new DrozinerAttractionModel(FramesFactory.getITRF(), provider.getAe(), provider.getMu(), C, S);
    }
}
