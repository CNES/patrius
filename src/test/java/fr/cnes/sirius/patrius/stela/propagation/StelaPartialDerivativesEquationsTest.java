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
 * HISTORY
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:64:30/05/2013:update with renamed classes
 * VERSION::DM:91:26/07/2013:test modification
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:317:04/03/2015: STELA integration in CIRF with referential choice (ICRF, CIRF or MOD)
 * VERSION::FA:391:13/04/2015: system to retrieve STELA dE/dt
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.propagation;

import java.io.IOException;
import java.text.ParseException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationFactory;
import fr.cnes.sirius.patrius.math.ode.nonstiff.RungeKutta6Integrator;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusFixedStepHandler;
import fr.cnes.sirius.patrius.stela.JavaMathAdapter;
import fr.cnes.sirius.patrius.stela.PotentialCoefficientsProviderTest;
import fr.cnes.sirius.patrius.stela.forces.gravity.StelaZonalAttraction;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * <p>
 * Tests Partial derivatives equations
 * </p>
 * 
 * @author dentalc
 * 
 * @version $Id$
 * 
 * @since 1.3
 * 
 */

public class StelaPartialDerivativesEquationsTest {

    /** The potential coefficients provider used for test purposes. */
    PotentialCoefficientsProviderTest provider = new PotentialCoefficientsProviderTest();

    /** Stela GTO propagator */
    private static StelaGTOPropagator propagator;

    /** Start date */
    private static AbsoluteDate start;

    /** Orbit */
    static Orbit orbit;

    /** dt */
    private static double dt;

    /** Features description. */
    public enum features {
        /**
         * @featureTitle STELA Partial derivatives equations test
         * 
         * @featureDescription tests the partial derivatives equations
         * 
         * @coveredRequirements
         */
        STELA_GTO_PARTIAL_DERIVATIVES_COMPUTATION
    }

    /**
     * General set up method.
     * 
     * @throws PatriusException
     *         if an Orekit error occurs
     * @throws ParseException
     * @throws IOException
     */
    @Before
    public void setUp() throws PatriusException, IOException, ParseException {

        FramesFactory.setConfiguration(FramesConfigurationFactory.getStelaConfiguration());

        /*****************************************************
         * Parameters of the test: this part can be modified *
         *****************************************************/
        start = new AbsoluteDate(new DateComponents(2011, 04, 01),
            new TimeComponents(0, 0, 35.), TimeScalesFactory.getTAI());
        dt = Constants.JULIAN_DAY;
        orbit = new KeplerianOrbit(24530000.0, 0.73, MathLib.toRadians(10.),
            MathLib.toRadians(30.), MathLib.toRadians(20.), MathLib.toRadians(45.),
            PositionAngle.MEAN, FramesFactory.getCIRF(), start, 398600441449820.0);
        // start = new AbsoluteDate(new DateComponents(2013, 04, 01),
        // new TimeComponents(0, 0, 35.), TimeScalesFactory.getTAI());
        // end = new AbsoluteDate(new DateComponents(2013, 04, 02),
        // new TimeComponents(0, 0, 35.), TimeScalesFactory.getTAI());
        // dt = Constants.JULIAN_DAY;
        // orbit = new KeplerianOrbit(24530000.0, 0.7310223143394789513, 0.1717486024772727493,
        // 4.405059704922067887, 1.437587618812528723, 3.354999227116208793,
        // PositionAngle.MEAN, FramesFactory.getMOD(false), start, 398600441449820.0);

        final RungeKutta6Integrator rk6 = new RungeKutta6Integrator(dt);

        propagator = new StelaGTOPropagator(rk6);
        // propagator.setMass(1000);
        propagator.setInitialState(new SpacecraftState(orbit), 1000, false);
    }

    /**
     * @throws PatriusException
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_GTO_PARTIAL_DERIVATIVES_COMPUTATION}
     * 
     * @testedMethod {@link StelaGTOPropagator#propagate(AbsoluteDate, AbsoluteDate)}
     * 
     * @description test computation mechanism of the the partial derivatives of the state in the
     *              Stela GTO propagator. This test uses a mock force model and a step handler to check the partial
     *              derivatives
     *              value every 100 seconds.
     * 
     * @input a Stela GTO propagator, a mock force model, and a step handler that checks the value of the
     *        computed partial derivatives.
     * 
     * @output the computed partial derivatives
     * 
     * @testPassCriteria the partial derivatives are properly computed during the propagation and their
     *                   value is the expected one
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 2.3
     */
    @Test
    public void testPartialDerivativesComputation() throws PatriusException {

        final double EPSILON = 1e-12;

        // Expected results:

        final double[][] expectedResults = {
            { 1.000000000000000000E+00, 0.000000000000000000E+00, 0.000000000000000000E+00,
                0.000000000000000000E+00, 0.000000000000000000E+00, 0.000000000000000000E+00 },
            { -8.698490295553953694E-07, 1.000000000000000000E+00, 4.123276181115036881E-02,
                4.912338417813327807E-02, -1.404478194020236312E-02, -5.020218701534026953E-03 },
            { 5.410808102287231390E-10, 0.000000000000000000E+00, 9.847142812577899251E-01,
                -2.491658646657546328E-02, 5.229974657043310465E-03, 1.863780190779770231E-03 },
            { -4.479923356815579273E-10, 0.000000000000000000E+00, 1.936923487459370555E-02,
                1.015031384420792104E+00, -4.321314335090708364E-03, -1.535580706630449615E-03 },
            { -2.909222298215361738E-11, 0.000000000000000000E+00, 8.146455773063358916E-04,
                9.753085155533755242E-04, 9.999046302838107136E-01, 7.032401525004741694E-03 },
            { 8.320035256067769250E-11, 0.000000000000000000E+00, -2.347737309836986710E-03,
                -2.802769488856324438E-03, -6.858910631793103257E-03, 1.000048599932992666E+00 } };
        final double[] exVect = new double[36];

        JavaMathAdapter.matrixToVector(expectedResults, exVect, 0);

        // Extrapolation:

        final StelaZonalAttraction zonaux = new StelaZonalAttraction(this.provider, 7, true, 2, 5, true);
        propagator.addForceModel(zonaux);
        final StelaPartialDerivativesEquations pd = new StelaPartialDerivativesEquations(
            propagator.getGaussForceModels(), propagator.getLagrangeForceModels(), 1, propagator);
        propagator.addAdditionalEquations(pd);

        SpacecraftState initialState = new SpacecraftState(orbit);
        initialState = pd.addInitialAdditionalState(initialState);
        propagator.setInitialState(initialState, 1000, false);

        final PatriusFixedStepHandler handler = new PatriusFixedStepHandler(){
            @Override
            public void init(final SpacecraftState s0, final AbsoluteDate t) {
            }

            @Override
            public void handleStep(final SpacecraftState currentState, final boolean isLast)
                                                                                            throws PropagationException {
                double[] addState;
                try {

                    addState = currentState.getAdditionalState("PARTIAL_DERIVATIVES");
                    // final double t = currentState.getDate().durationFrom(start);
                    if (currentState.getOrbit().getDate().durationFrom(start) > 0) {
                        for (int i = 0; i < exVect.length; i++) {

                            if (exVect[i] == 0) {

                                Assert.assertEquals(exVect[i], addState[i], EPSILON);
                            } else {

                                Assert.assertEquals(0, (exVect[i] - addState[i]) / exVect[i], EPSILON);
                            }
                        }
                    }
                } catch (final PatriusException e) {
                    e.printStackTrace();
                }
            }
        };
        propagator.setMasterMode(dt, handler);
        propagator.propagate(start, start.shiftedBy(dt));
    }
}
