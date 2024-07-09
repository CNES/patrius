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
 * VERSION::DM:489:17/12/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.orbits.orbitalparameters;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Tests for {@link ReentryParameters}.
 * 
 * @author Emmanuel Bignon
 * 
 * @version $Id: ReentryParametersTest.java 18088 2017-10-02 17:01:51Z bignon $
 * 
 * @since 3.0
 * 
 */
public class ReentryParametersTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Reentry parameters conversion to/from any other parameters
         * 
         * @featureDescription Reentry parameters conversion to/from any other parameters.
         * 
         * @coveredRequirements
         */
        CONVERSION
    }

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(ReentryParametersTest.class.getSimpleName(), "Reentry parameters");
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#CONVERSION}
     * 
     * @testedMethod {@link ReentryParameters#getCartesianParameters()}
     * 
     * @description test reentry parameters conversion to cartesian parameters.
     * 
     * @input {@link ReentryParameters}
     * 
     * @output {@link CartesianParameters}
     * 
     * @testPassCriteria results are the same as expected (reference BibMS), at 1E-15 (relative values).
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public final void testReentryParametersCartesianParameters() {

        // Initialization
        final double ae = Constants.GRS80_EARTH_EQUATORIAL_RADIUS;
        final double f = 0.5;
        final double mu = Constants.GRS80_EARTH_MU;
        final double eps = 1E-15;
        final double eps2 = 1E-14;

        // Case 1 from BibMS
        Report.printMethodHeader("testReentryParametersCartesianParameters", "Reentry parameters to cartesian (1/2)",
            "BibMS", eps, ComparisonType.RELATIVE);
        final ReentryParameters reentryParameters1 = new ReentryParameters(8599616.15015757, 0.839975092194095,
            0.789962057936808, 9906.73508276062, -0.390035484104706, 4.34000921645338, ae, f, mu);
        final CartesianParameters cartesianParameters1 = reentryParameters1.getCartesianParameters();
        this.checkCartesianParameters(new double[] { 7961E3, 8034E3, 7957E3, 6.04E3, -6.03E3, -5.03E3 },
            cartesianParameters1, eps, true);
        Report.printMethodHeader("testReentryParametersCartesianParameters", "Cartesian to reentry parameters (1/2)",
            "BibMS", eps2, ComparisonType.RELATIVE);
        final ReentryParameters reverseReentryParams1 = cartesianParameters1.getReentryParameters(ae, f);
        this.checkRentryParameters(new double[] { 8599616.15015757, 0.839975092194095, 0.789962057936808,
            9906.73508276062,
            -0.390035484104706, 4.34000921645338 }, reverseReentryParams1, eps2, true);

        // Case 2 from BibMS
        Report.printMethodHeader("testReentryParametersCartesianParameters", "Reentry parameters to cartesian (2/2)",
            "BibMS", eps, ComparisonType.RELATIVE);
        final CartesianParameters cartesianParameters2 = new CartesianParameters(new Vector3D(7961E3, 8034E3, 7957E3),
            new Vector3D(6.04E3, -6.03E3, -5.03E3), Vector3D.ZERO, mu);
        final ReentryParameters reentryParameters2 = cartesianParameters2.getReentryParameters(ae, f);
        this.checkRentryParameters(new double[] { 8599616.15015757, 0.839975092194095, 0.789962057936808,
            9906.73508276062,
            -0.390035484104706, 4.34000921645338 }, reentryParameters2, eps, true);
        Report.printMethodHeader("testReentryParametersCartesianParameters", "Cartesian to reentry parameters (2/2)",
            "BibMS", eps2, ComparisonType.RELATIVE);
        final CartesianParameters reverseCartesianParams2 = reentryParameters2.getCartesianParameters();
        this.checkCartesianParameters(new double[] { 7961E3, 8034E3, 7957E3, 6.04E3, -6.03E3, -5.03E3 },
            reverseCartesianParams2, eps2, true);

        // Other minor checks
        Assert.assertEquals(reentryParameters1.getAe(), ae);
        Assert.assertEquals(reentryParameters1.getF(), f);
        Assert.assertEquals(reentryParameters1.getMu(), mu);
        Assert.assertNotNull(reentryParameters1.toString());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#CONVERSION}
     * 
     * @testedMethod {@link ReentryParameters#getCartesianParameters()}
     * @testedMethod {@link CartesianParameters#getReentryParameters(double, double)}
     * 
     * @description test reentry parameters conversion to cartesian parameters and vice-versa for various specific
     *              configurations.
     * 
     * @input ReentryParameters or Cartesian parameters
     * 
     * @output Cartesian parameters or ReentryParameters
     * 
     * @testPassCriteria results are the same as expected at 1E-14 (relative difference)
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public final void testConversionCases() {

        // Initialization
        final double ae = Constants.GRS80_EARTH_EQUATORIAL_RADIUS;
        final double f = Constants.GRS80_EARTH_FLATTENING;
        final double mu = Constants.GRS80_EARTH_MU;
        final double eps = 1E-14;

        // Launch phase on the equator along ITRF X-axis
        final CartesianParameters cartParams1 = new CartesianParameters(new Vector3D(ae + 1E3, 0, 0), new Vector3D(10,
            0, 0), new Vector3D(10, 0, 0), mu);
        final ReentryParameters reentryParams1 = cartParams1.getReentryParameters(ae, f);
        this.checkRentryParameters(new double[] { 1E3, 0, 0, 10, FastMath.PI / 2., 0 }, reentryParams1, eps);
        final CartesianParameters reverseCartParams1 = reentryParams1.getCartesianParameters();
        this.checkCartesianParameters(new double[] { ae + 1E3, 0, 0, 10, 0, 0 }, reverseCartParams1, eps);

        // Vertical descent phase on the equator along ITRF X-axis
        final CartesianParameters cartParams2 = new CartesianParameters(new Vector3D(ae + 1E3, 0, 0), new Vector3D(-10,
            0, 0), new Vector3D(10, 0, 0), mu);
        final ReentryParameters reentryParams2 = cartParams2.getReentryParameters(ae, f);
        this.checkRentryParameters(new double[] { 1E3, 0, 0, 10, -FastMath.PI / 2., 0 }, reentryParams2, eps);
        final CartesianParameters reverseCartParams2 = reentryParams2.getCartesianParameters();
        this.checkCartesianParameters(new double[] { ae + 1E3, 0, 0, -10, 0, 0 }, reverseCartParams2, eps);

        // Launch phase on the pole along ITRF Z-axis
        final CartesianParameters cartParams3 = new CartesianParameters(new Vector3D(0, 0, ae * (1. - f) + 1E3),
            new Vector3D(0, 0, 10), new Vector3D(10, 0, 0), mu);
        final ReentryParameters reentryParams3 = cartParams3.getReentryParameters(ae, f);
        this.checkRentryParameters(new double[] { 1E3, FastMath.PI / 2., 0, 10, FastMath.PI / 2., 0 }, reentryParams3,
            eps);
        final CartesianParameters reverseCartParams3 = reentryParams3.getCartesianParameters();
        this.checkCartesianParameters(new double[] { 0, 0, ae * (1. - f) + 1E3, 0, 0, 10 }, reverseCartParams3, 1E-9);

        // Vertical descent phase on the pole along ITRF Z-axis
        final CartesianParameters cartParams4 = new CartesianParameters(new Vector3D(0, 0, ae * (1. - f) + 1E3),
            new Vector3D(0, 0, -10), new Vector3D(10, 0, 0), mu);
        final ReentryParameters reentryParams4 = cartParams4.getReentryParameters(ae, f);
        this.checkRentryParameters(new double[] { 1E3, FastMath.PI / 2., 0, 10, -FastMath.PI / 2., 0 }, reentryParams4,
            eps);
        final CartesianParameters reverseCartParams4 = reentryParams4.getCartesianParameters();
        this.checkCartesianParameters(new double[] { 0, 0, ae * (1. - f) + 1E3, 0, 0, -10 }, reverseCartParams4, 1E-9);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#CONVERSION}
     * 
     * @testedMethod {@link ReentryParameters#getCartesianParameters()}
     * @testedMethod {@link ReentryParameters#getKeplerianParameters()}
     * @testedMethod {@link ReentryParameters#getApsisAltitudeParameters(double)}
     * @testedMethod {@link ReentryParameters#getApsisRadiusParameters()}
     * @testedMethod {@link ReentryParameters#getCircularParameters()}
     * @testedMethod {@link ReentryParameters#getEquatorialParameters()}
     * @testedMethod {@link ReentryParameters#getEquinoctialParameters()}
     * @testedMethod {@link ReentryParameters#getStelaEquinoctialParameters()}
     * 
     * @description test reentry parameters conversion to/from any type by go and return conversions.
     * 
     * @input ReentryParameters
     * 
     * @output ReentryParameters
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
        final double f = Constants.GRS80_EARTH_FLATTENING;
        final double mu = Constants.GRS80_EARTH_MU;
        final double eps = 1E-13;
        final ReentryParameters reentryParameters = new ReentryParameters(1000000., 0.84, 0.79, 7100, 0.01, 4.34, ae,
            f, mu);

        // Check conversions
        this.checkRentryParameters(new double[] { 1000000., 0.84, 0.79, 7100, 0.01, 4.34 }, reentryParameters
            .getCartesianParameters().getReentryParameters(ae, f), eps);
        this.checkRentryParameters(new double[] { 1000000., 0.84, 0.79, 7100, 0.01, 4.34 }, reentryParameters
            .getKeplerianParameters().getReentryParameters(ae, f), eps);
        this.checkRentryParameters(new double[] { 1000000., 0.84, 0.79, 7100, 0.01, 4.34 }, reentryParameters
            .getApsisAltitudeParameters(ae).getReentryParameters(ae, f), eps);
        this.checkRentryParameters(new double[] { 1000000., 0.84, 0.79, 7100, 0.01, 4.34 }, reentryParameters
            .getApsisRadiusParameters().getReentryParameters(ae, f), eps);
        this.checkRentryParameters(new double[] { 1000000., 0.84, 0.79, 7100, 0.01, 4.34 }, reentryParameters
            .getCircularParameters().getReentryParameters(ae, f), eps);
        this.checkRentryParameters(new double[] { 1000000., 0.84, 0.79, 7100, 0.01, 4.34 }, reentryParameters
            .getEquatorialParameters().getReentryParameters(ae, f), eps);
        this.checkRentryParameters(new double[] { 1000000., 0.84, 0.79, 7100, 0.01, 4.34 }, reentryParameters
            .getEquinoctialParameters().getReentryParameters(ae, f), eps);
        this.checkRentryParameters(new double[] { 1000000., 0.84, 0.79, 7100, 0.01, 4.34 }, reentryParameters
            .getStelaEquinoctialParameters().getReentryParameters(ae, f), eps);
        this.checkRentryParameters(new double[] { 1000000., 0.84, 0.79, 7100, 0.01, 4.34 },
            reentryParameters.getReentryParameters(ae, f), eps);
    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link ReentryParameters#toString()}
     * 
     * @description test string conversion of reentry parameters
     * 
     * @input ReentryParameters
     * 
     * @output ReentryParameters
     * 
     * @testPassCriteria string is as expected
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testToString() {
        // Params
        final double ae = Constants.GRS80_EARTH_EQUATORIAL_RADIUS;
        final double f = Constants.GRS80_EARTH_FLATTENING;
        final double mu = Constants.GRS80_EARTH_MU;
        final ReentryParameters reentryParameters = new ReentryParameters(1000000., 0.84, 0.79, 7100, 0.01, 4.34, ae,
            f, mu);
        System.out.println(reentryParameters.toString());
        Assert
            .assertEquals(
                reentryParameters.toString(),
                "reentry parameters: {altitude(m): 1000000.0; latitude(deg): 48.128454790989146; longitude(deg): 45.26366581533504; velocity(m/s): 7100.0; slope(deg): 0.5729577951308232; azimuth(deg): 248.66368308677727; ae(m): 6378137.0; f: 0.003352810681182319;}");
    }

    /**
     * Check reentry parameters with report writing.
     * 
     * @param expected
     *        expected values
     * @param reentryParams
     *        reentry parameters
     * @param eps
     *        epsilon
     * @param writeToReport
     *        true if write to report
     */
    private void checkRentryParameters(final double[] expected, final ReentryParameters reentryParams,
                                       final double eps, final boolean writeToReport) {
        this.checkRelDiff(expected[0], reentryParams.getAltitude(), eps, writeToReport, "Altitude");
        this.checkRelDiff(expected[1], reentryParams.getLatitude(), eps, writeToReport, "Latitude");
        this.checkRelDiff(expected[2], reentryParams.getLongitude(), eps, writeToReport, "Longitude");
        this.checkRelDiff(expected[3], reentryParams.getVelocity(), eps, writeToReport, "Velocity");
        this.checkRelDiff(expected[4], reentryParams.getSlope(), eps, writeToReport, "Slope");
        this.checkRelDiff(expected[5], reentryParams.getAzimuth(), eps, writeToReport, "Azimuth");
    }

    /**
     * Check cartesian parameters with report writing.
     * 
     * @param expected
     *        expected values
     * @param cartParams
     *        cartesian parameters
     * @param eps
     *        epsilon
     * @param writeToReport
     *        true if write to report
     */
    private void checkCartesianParameters(final double[] expected, final CartesianParameters cartParams,
                                          final double eps, final boolean writeToReport) {
        this.checkRelDiff(expected[0], cartParams.getPosition().getX(), eps, writeToReport, "X");
        this.checkRelDiff(expected[1], cartParams.getPosition().getY(), eps, writeToReport, "Y");
        this.checkRelDiff(expected[2], cartParams.getPosition().getZ(), eps, writeToReport, "Z");
        this.checkRelDiff(expected[3], cartParams.getVelocity().getX(), eps, writeToReport, "VX");
        this.checkRelDiff(expected[4], cartParams.getVelocity().getY(), eps, writeToReport, "VY");
        this.checkRelDiff(expected[5], cartParams.getVelocity().getZ(), eps, writeToReport, "VZ");
    }

    /**
     * Check reentry parameters.
     * 
     * @param expected
     *        expected values
     * @param reentryParams
     *        reentry parameters
     * @param eps
     *        epsilon
     */
    private void
            checkRentryParameters(final double[] expected, final ReentryParameters reentryParams, final double eps) {
        this.checkRentryParameters(expected, reentryParams, eps, false);
    }

    /**
     * Check cartesian parameters.
     * 
     * @param expected
     *        expected values
     * @param cartParams
     *        cartesian parameters
     * @param eps
     *        epsilon
     */
    private void checkCartesianParameters(final double[] expected, final CartesianParameters cartParams,
                                          final double eps) {
        this.checkCartesianParameters(expected, cartParams, eps, false);
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
     * @param writeToReport
     *        true if write to report
     * @param tag
     *        report tag
     */
    private void checkRelDiff(final double expected, final double actual, final double eps,
                              final boolean writeToReport, final String tag) {
        if (expected == 0) {
            Assert.assertEquals(expected, actual, eps);
        } else {
            Assert.assertEquals((expected - actual) / expected, 0, eps);
        }
        if (writeToReport) {
            Report.printToReport(tag, expected, actual);
        }
    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link ReentryParameters#equals(Object)}
     * @testedMethod {@link ReentryParameters#hashCode()}
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
        final ReentryParameters params1 = new ReentryParameters(300000, 10, 20, 7000, 2, 4, 6378000, 0.001, Constants.CNES_STELA_MU);
        final ReentryParameters params2 = new ReentryParameters(300000, 10, 20, 7000, 2, 4, 6378000, 0.001, Constants.CNES_STELA_MU);
        final ReentryParameters params3 = new ReentryParameters(300000, 10, 20, 7000, 2, 4, 6379000, 0.001, Constants.CNES_STELA_MU);

        // Check
        Assert.assertTrue(params1.equals(params1));
        Assert.assertTrue(params1.equals(params2));
        Assert.assertFalse(params1.equals(params3));

        Assert.assertTrue(params1.hashCode() == params2.hashCode());
        Assert.assertFalse(params1.hashCode() == params3.hashCode());
    }
}
