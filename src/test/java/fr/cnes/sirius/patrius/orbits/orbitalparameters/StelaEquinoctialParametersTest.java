/**
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
 * VERSION:4.5:FA:FA-2326:27/05/2020:Orbits - Correction equals & HashCode 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:396:16/03/2015:new architecture for orbital parameters
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
/**
 */
package fr.cnes.sirius.patrius.orbits.orbitalparameters;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for the class {@link StelaEquinoctialParameters}.
 * 
 * @author Emmanuel Bignon
 * 
 * @version $Id: StelaEquinoctialParametersTest.java 18088 2017-10-02 17:01:51Z bignon $
 * 
 * @since 3.0
 * 
 */
public class StelaEquinoctialParametersTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Stela equinoctial parameters constructors
         * 
         * @featureDescription test constructors
         * 
         * @coveredRequirements
         */
        STELA_EQUINOCTIAL_PARAMETERS_CONSTRUCTORS,
        /**
         * @featureTitle Stela equinoctial parameters conversion
         * 
         * @featureDescription test the conversion from/to equinoctial parameters
         * 
         * @coveredRequirements
         */
        STELA_EQUINOCTIAL_PARAMETERS_CONVERSION,
    }

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(StelaEquinoctialParametersTest.class.getSimpleName(), "STELA equinoctial parameters");
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_EQUINOCTIAL_PARAMETERS_CONSTRUCTORS}
     * 
     * @description test constructors of Stela equinoctial parameters.
     * 
     * @input an equinoctial orbit
     * 
     * @output exception
     * 
     * @testPassCriteria the output values are the expected ones
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public final void testConstructors() {

        try {
            new StelaEquinoctialParameters(10000E3, 0.5, 0.9, 0.3, 0.4, 0.5, Constants.CNES_STELA_MU, false);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        try {
            new StelaEquinoctialParameters(1000E3, 0.1, 0.2, 0.3, 0.4, 0.5, Constants.CNES_STELA_MU, false);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_EQUINOCTIAL_PARAMETERS_CONVERSION}
     * 
     * @testedMethod {@link StelaEquinoctialParameters#getCartesianParameters()}
     * @testedMethod {@link StelaEquinoctialParameters#getApsisRadiusParameters()}
     * @testedMethod {@link StelaEquinoctialParameters#getCircularParameters()}
     * @testedMethod {@link StelaEquinoctialParameters#getEquatorialParameters()}
     * @testedMethod {@link StelaEquinoctialParameters#getEquinoctialParameters()}
     * 
     * @description test conversions from Stela equinoctial parameters.
     * 
     * @input an equinoctial orbit
     * 
     * @output an equinoctial orbit
     * 
     * @testPassCriteria the output values are the expected ones
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public final void testConversions() {

        Report.printMethodHeader("testConversions", "Stela equinoctial parameters to cartesian", "Other conversions",
            1E-14, ComparisonType.RELATIVE);

        // Initialization
        final StelaEquinoctialParameters params = new StelaEquinoctialParameters(10000E3, 0.01, 0.02, 0.3, 0.4, 0.5,
            Constants.CNES_STELA_MU, false);

        // Check Stela equinoctial parameters
        final StelaEquinoctialParameters stelaParams = params.getStelaEquinoctialParameters();
        final StelaEquinoctialParameters stelaExp = params.getKeplerianParameters().getStelaEquinoctialParameters();
        Assert.assertEquals(stelaExp.getA(), stelaParams.getA(), 0);
        Assert.assertEquals(stelaExp.getEquinoctialEx(), stelaParams.getEquinoctialEx(), 1E-15);
        Assert.assertEquals(stelaExp.getEquinoctialEy(), stelaParams.getEquinoctialEy(), 0);
        Assert.assertEquals(stelaExp.getIx(), stelaParams.getIx(), 0);
        Assert.assertEquals(stelaExp.getIy(), stelaParams.getIy(), 0);
        Assert.assertEquals(stelaExp.getLM(), stelaParams.getLM(), 1E-15);

        // Check cartesian parameters
        final CartesianParameters cartParams = params.getCartesianParameters();
        final CartesianParameters cartExp = params.getKeplerianParameters().getCartesianParameters();
        Assert.assertEquals(cartParams.getPosition().getX(), cartExp.getPosition().getX(), 1E-8);
        Assert.assertEquals(cartParams.getPosition().getY(), cartExp.getPosition().getY(), 1E-8);
        Assert.assertEquals(cartParams.getPosition().getZ(), cartExp.getPosition().getZ(), 1E-8);
        Assert.assertEquals(cartParams.getVelocity().getX(), cartExp.getVelocity().getX(), 1E-11);
        Assert.assertEquals(cartParams.getVelocity().getY(), cartExp.getVelocity().getY(), 1E-11);
        Assert.assertEquals(cartParams.getVelocity().getZ(), cartExp.getVelocity().getZ(), 1E-11);

        Report.printToReport("X", cartExp.getPosition().getX(), cartParams.getPosition().getX());
        Report.printToReport("Y", cartExp.getPosition().getY(), cartParams.getPosition().getY());
        Report.printToReport("Z", cartExp.getPosition().getZ(), cartParams.getPosition().getZ());
        Report.printToReport("VX", cartExp.getVelocity().getX(), cartParams.getVelocity().getX());
        Report.printToReport("VY", cartExp.getVelocity().getY(), cartParams.getVelocity().getY());
        Report.printToReport("VZ", cartExp.getVelocity().getZ(), cartParams.getVelocity().getZ());

        // Check a second time for cache
        final CartesianParameters cartParams2 = params.getCartesianParameters();
        Assert.assertEquals(cartParams2.getPosition().getX(), cartExp.getPosition().getX(), 1E-8);
        Assert.assertEquals(cartParams2.getPosition().getY(), cartExp.getPosition().getY(), 1E-8);
        Assert.assertEquals(cartParams2.getPosition().getZ(), cartExp.getPosition().getZ(), 1E-8);
        Assert.assertEquals(cartParams2.getVelocity().getX(), cartExp.getVelocity().getX(), 1E-11);
        Assert.assertEquals(cartParams2.getVelocity().getY(), cartExp.getVelocity().getY(), 1E-11);
        Assert.assertEquals(cartParams2.getVelocity().getZ(), cartExp.getVelocity().getZ(), 1E-11);

        // Check circular parameters
        final CircularParameters circParams = params.getCircularParameters();
        final CircularParameters circExp = params.getKeplerianParameters().getCircularParameters();
        Assert.assertEquals(circParams.getA(), circExp.getA(), 0);
        Assert.assertEquals(circParams.getCircularEx(), circExp.getCircularEx(), 1E-15);
        Assert.assertEquals(circParams.getCircularEy(), circExp.getCircularEy(), 1E-15);
        Assert.assertEquals(circParams.getI(), circExp.getI(), 1E-15);
        Assert.assertEquals(circParams.getRightAscensionOfAscendingNode(), circExp.getRightAscensionOfAscendingNode(),
            0);
        Assert.assertEquals(circParams.getAlphaV(), circExp.getAlphaV(), 1E-15);

        // Check equatorial parameters
        final EquatorialParameters equatParams = params.getEquatorialParameters();
        final EquatorialParameters equatExp = params.getKeplerianParameters().getEquatorialParameters();
        Assert.assertEquals(equatParams.getA(), equatExp.getA(), 0);
        Assert.assertEquals(equatParams.getE(), equatExp.getE(), 0);
        Assert.assertEquals(equatParams.getIx(), equatExp.getIx(), 0);
        Assert.assertEquals(equatParams.getIy(), equatExp.getIy(), 0);
        Assert.assertEquals(equatParams.getPomega(), equatExp.getPomega(), 0);
        Assert.assertEquals(equatParams.getTrueAnomaly(), equatExp.getTrueAnomaly(), 0);

        // Check equinoctial parameters
        final EquinoctialParameters equiParams = params.getEquinoctialParameters();
        final EquinoctialParameters equiExp = params.getKeplerianParameters().getEquinoctialParameters();
        Assert.assertEquals(equiParams.getA(), equiExp.getA(), 0);
        Assert.assertEquals(equiParams.getEquinoctialEx(), equiExp.getEquinoctialEx(), 1E-15);
        Assert.assertEquals(equiParams.getEquinoctialEy(), equiExp.getEquinoctialEy(), 0);
        Assert.assertEquals(equiParams.getHx(), equiExp.getHx(), 0);
        Assert.assertEquals(equiParams.getHy(), equiExp.getHy(), 0);
        Assert.assertEquals(equiParams.getLv(), equiExp.getLv(), 1E-15);

        // Check circular parameters
        final ApsisRadiusParameters apsisParams = params.getApsisRadiusParameters();
        final ApsisRadiusParameters apsisExp = params.getKeplerianParameters().getApsisRadiusParameters();
        Assert.assertEquals(apsisParams.getPeriapsis(), apsisExp.getPeriapsis(), 0);
        Assert.assertEquals(apsisParams.getApoapsis(), apsisExp.getApoapsis(), 1E-15);
        Assert.assertEquals(apsisParams.getI(), apsisExp.getI(), 1E-15);
        Assert.assertEquals(apsisParams.getPerigeeArgument(), apsisExp.getPerigeeArgument(), 1E-15);
        Assert.assertEquals(apsisParams.getRightAscensionOfAscendingNode(),
            apsisExp.getRightAscensionOfAscendingNode(), 0);
        Assert.assertEquals(apsisParams.getAnomaly(PositionAngle.TRUE), apsisExp.getAnomaly(PositionAngle.TRUE), 1E-15);

        // Also check toString method() (for coverage purpose)
        Assert.assertNotNull(stelaParams.toString());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_EQUINOCTIAL_PARAMETERS_CONVERSION}
     * 
     * @testedMethod {@link StelaEquinoctialParameters#correctInclination(double, double)}
     * 
     * @description test the inclination correction around 180°.
     *              This test is adapted from StelaEquinoctialOrbitTest.testInclinationCorrection from Patrius package.
     * 
     * @input equinoctial parameters
     * 
     * @testPassCriteria the output values are the expected ones (the reference values have been generated with STELA)
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public final void testInclinationCorrection() {
        // Check 1
        final double mu = 3.9860044144982E14;
        final IOrbitalParameters kep1 = new KeplerianParameters(30000000, 0.1, MathLib.toRadians(179.9),
            MathLib.toRadians(20),
            MathLib.toRadians(30), MathLib.toRadians(60), PositionAngle.MEAN, mu);
        final StelaEquinoctialParameters eqPar1 = kep1.getStelaEquinoctialParameters();

        Assert.assertEquals(MathLib.toRadians(179.5), eqPar1.getKeplerianParameters().getI(), 1e-14);
    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link StelaEquinoctialParameters#equals(Object)}
     * @testedMethod {@link StelaEquinoctialParameters#hashCode()}
     * 
     * @description test equals() and hashcode() method for different case:
     * - params1 vs params1
     * - params1 vs params2 with same attributes
     * - params 1 vs params3 with different attributes
     * 
     * @input ReentryParameters
     * 
     * @output boolean and int
     * 
     * @testPassCriteria output is as expected
     * 
     * @referenceVersion 4.5
     * 
     * @nonRegressionVersion 4.5
     */
    @Test
    public void testEquals() throws PatriusException {

        // Initialization
        final StelaEquinoctialParameters params1 = new StelaEquinoctialParameters(60000000, 0.0001, 0.0002, 0.003, 0.004, 2, Constants.CNES_STELA_MU, false);
        final StelaEquinoctialParameters params2 = new StelaEquinoctialParameters(60000000, 0.0001, 0.0002, 0.003, 0.004, 2, Constants.CNES_STELA_MU, false);
        final StelaEquinoctialParameters params3 = new StelaEquinoctialParameters(60000000, 0.0001, 0.0002, 0.003, 0.004, 2.1, Constants.CNES_STELA_MU, false);

        // Check
        Assert.assertTrue(params1.equals(params1));
        Assert.assertTrue(params1.equals(params2));
        Assert.assertFalse(params1.equals(params3));

        Assert.assertTrue(params1.hashCode() == params2.hashCode());
        Assert.assertFalse(params1.hashCode() == params3.hashCode());
    }
}
