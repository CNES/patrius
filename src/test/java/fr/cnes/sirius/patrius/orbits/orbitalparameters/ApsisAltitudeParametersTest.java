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
 * HISTORY
 * VERSION:4.5:FA:FA-2326:27/05/2020:Orbits - Correction equals & HashCode 
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:396:16/03/2015:new architecture for orbital parameters
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.orbits.orbitalparameters;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Tests for {@link ApsisAltitudeParameters}.
 * 
 * @author Emmanuel Bignon
 * 
 * @version $Id: ApsisAltitudeParametersTest.java 18088 2017-10-02 17:01:51Z bignon $
 * 
 * @since 3.0
 * 
 */
public class ApsisAltitudeParametersTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Apsis parameters conversion to/from any other parameters
         * 
         * @featureDescription Apsis parameters conversion to/from any other parameters.
         * 
         * @coveredRequirements
         */
        CONVERSION
    }

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(ApsisAltitudeParametersTest.class.getSimpleName(), "Apsis altitude parameters");
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#CONVERSION}
     * 
     * @testedMethod {@link ApsisAltitudeParameters#getKeplerianParameters()}
     * 
     * @description test apsis parameters conversion to keplerian parameters.
     * 
     * @input Apsis parameters
     * 
     * @output keplerian parameters
     * 
     * @testPassCriteria results are the same as expected at 1E-14 (relative difference)
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public final void testApsisParametersToKeplerianParameters() {

        Report.printMethodHeader("testApsisParametersToKeplerianParameters", "Apsis parameters to keplerian", "Math",
            1E-14, ComparisonType.RELATIVE);

        // Initialization
        final double ae = Constants.GRS80_EARTH_EQUATORIAL_RADIUS;
        final double mu = Constants.GRS80_EARTH_MU;
        final double eps = 1E-14;
        final ApsisAltitudeParameters parameters = new ApsisAltitudeParameters(100E3, 400E3, 0.1, 0.2, 0.3, 0.4,
            PositionAngle.TRUE, mu, ae);

        // Check
        final KeplerianParameters keplerianParameters = parameters.getKeplerianParameters();
        this.checkKeplerianParameters(
            new double[] { 250E3 + ae, 1. - (100E3 + ae) / (250E3 + ae), 0.1, 0.2, 0.3, 0.4 },
            keplerianParameters, eps);

        // Other minor checks
        Assert.assertEquals(parameters.getAe(), ae);
        Assert.assertEquals(parameters.getMu(), mu);
        Assert.assertNotNull(parameters.toString());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#CONVERSION}
     * 
     * @testedMethod {@link ApsisAltitudeParameters#getCartesianParameters()}
     * @testedMethod {@link ApsisAltitudeParameters#getKeplerianParameters()}
     * @testedMethod {@link ApsisAltitudeParameters#getApsisAltitudeParameters(double)}
     * @testedMethod {@link ApsisAltitudeParameters#getApsisRadiusParameters()}
     * @testedMethod {@link ApsisAltitudeParameters#getCircularParameters()}
     * @testedMethod {@link ApsisAltitudeParameters#getEquatorialParameters()}
     * @testedMethod {@link ApsisAltitudeParameters#getEquinoctialParameters()}
     * @testedMethod {@link ApsisAltitudeParameters#getStelaEquinoctialParameters()}
     * 
     * @description test apsis parameters conversion to/from any type by go and return conversions.
     * 
     * @input ApsisAltitudeParameters
     * 
     * @output ApsisAltitudeParameters
     * 
     * @testPassCriteria results are the same as expected at 1E-13 (relative difference)
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public final void testAllConversionTypes() {

        // Initialization
        final double ae = Constants.GRS80_EARTH_EQUATORIAL_RADIUS;
        final double mu = Constants.GRS80_EARTH_MU;
        final double eps = 1E-13;
        final ApsisAltitudeParameters parameters = new ApsisAltitudeParameters(100E3, 400E3, 0.1, 0.2, 0.3, 0.4,
            PositionAngle.TRUE, mu, ae);

        // Check conversions
        this.checkApsisAltitudeParameters(new double[] { 100E3, 400E3, 0.1, 0.2, 0.3, 0.4 }, parameters
            .getCartesianParameters().getApsisAltitudeParameters(ae), eps);
        this.checkApsisAltitudeParameters(new double[] { 100E3, 400E3, 0.1, 0.2, 0.3, 0.4 }, parameters
            .getKeplerianParameters().getApsisAltitudeParameters(ae), eps);
        this.checkApsisAltitudeParameters(new double[] { 100E3, 400E3, 0.1, 0.2, 0.3, 0.4 }, parameters
            .getApsisAltitudeParameters(ae).getApsisAltitudeParameters(ae), eps);
        this.checkApsisAltitudeParameters(new double[] { 100E3, 400E3, 0.1, 0.2, 0.3, 0.4 }, parameters
            .getApsisRadiusParameters().getApsisAltitudeParameters(ae), eps);
        this.checkApsisAltitudeParameters(new double[] { 100E3, 400E3, 0.1, 0.2, 0.3, 0.4 }, parameters
            .getCircularParameters().getApsisAltitudeParameters(ae), eps);
        this.checkApsisAltitudeParameters(new double[] { 100E3, 400E3, 0.1, 0.2, 0.3, 0.4 }, parameters
            .getEquatorialParameters().getApsisAltitudeParameters(ae), eps);
        this.checkApsisAltitudeParameters(new double[] { 100E3, 400E3, 0.1, 0.2, 0.3, 0.4 }, parameters
            .getEquinoctialParameters().getApsisAltitudeParameters(ae), eps);
        this.checkApsisAltitudeParameters(new double[] { 100E3, 400E3, 0.1, 0.2, 0.3, 0.4 }, parameters
            .getStelaEquinoctialParameters().getApsisAltitudeParameters(ae), eps);
        this.checkApsisAltitudeParameters(new double[] { 100E3, 400E3, 0.1, 0.2, 0.3, 0.4 },
            parameters.getApsisAltitudeParameters(ae), eps);
    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link ApsisAltitudeParameters#equals(Object)}
     * @testedMethod {@link ApsisAltitudeParameters#hashCode()}
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
        final ApsisAltitudeParameters params1 = new ApsisAltitudeParameters(300000, 600000, 10, 20, 30, 2, PositionAngle.TRUE, Constants.CNES_STELA_MU, 6378000);
        final ApsisAltitudeParameters params2 = new ApsisAltitudeParameters(300000, 600000, 10, 20, 30, 2, PositionAngle.TRUE, Constants.CNES_STELA_MU, 6378000);
        final ApsisAltitudeParameters params3 = new ApsisAltitudeParameters(300000, 600000, 10, 20, 30, 2, PositionAngle.TRUE, Constants.CNES_STELA_MU, 6378001);

        // Check
        Assert.assertTrue(params1.equals(params1));
        Assert.assertTrue(params1.equals(params2));
        Assert.assertFalse(params1.equals(params3));

        Assert.assertTrue(params1.hashCode() == params2.hashCode());
        Assert.assertFalse(params1.hashCode() == params3.hashCode());
    }

    /**
     * Check apsis parameters.
     * 
     * @param expected
     *        expected values
     * @param params
     *        apsis parameters
     * @param eps
     *        epsilon
     */
    private void checkApsisAltitudeParameters(final double[] expected, final ApsisAltitudeParameters params,
                                              final double eps) {
        this.checkRelDiff(expected[0], params.getPeriapsisAltitude(), eps);
        this.checkRelDiff(expected[1], params.getApoapsisAltitude(), eps);
        this.checkRelDiff(expected[2], params.getI(), eps);
        this.checkRelDiff(expected[3], params.getPerigeeArgument(), eps);
        this.checkRelDiff(expected[4], params.getRightAscensionOfAscendingNode(), eps);
        this.checkRelDiff(expected[5], params.getAnomaly(PositionAngle.TRUE), eps);
    }

    /**
     * Check keplerian parameters.
     * 
     * @param expected
     *        expected values
     * @param params
     *        apsis parameters
     * @param eps
     *        epsilon
     */
    private void checkKeplerianParameters(final double[] expected, final KeplerianParameters params, final double eps) {
        this.checkRelDiff(expected[0], params.getA(), eps);
        this.checkRelDiff(expected[1], params.getE(), eps);
        this.checkRelDiff(expected[2], params.getI(), eps);
        this.checkRelDiff(expected[3], params.getPerigeeArgument(), eps);
        this.checkRelDiff(expected[4], params.getRightAscensionOfAscendingNode(), eps);
        this.checkRelDiff(expected[5], params.getAnomaly(PositionAngle.TRUE), eps);
        Report.printToReport("a", expected[0], params.getA());
        Report.printToReport("e", expected[1], params.getE());
        Report.printToReport("i", expected[2], params.getI());
        Report.printToReport("Pa", expected[3], params.getPerigeeArgument());
        Report.printToReport("RAAN", expected[4], params.getRightAscensionOfAscendingNode());
        Report.printToReport("M", expected[5], params.getAnomaly(PositionAngle.TRUE));
    }

    /**
     * Check relative difference.
     * 
     * @param expected
     *        expected value
     * @param actual
     *        actual value
     * @param eps
     *        epsilon
     */
    private void checkRelDiff(final double expected, final double actual, final double eps) {
        if (expected == 0) {
            Assert.assertEquals(expected, actual, eps);
        } else {
            Assert.assertEquals((expected - actual) / expected, 0, eps);
        }
    }

}
