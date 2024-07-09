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
* VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:317:02/03/2015: STELA integration in CIRF with referential choice (ICRF, CIRF or MOD)
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.forces;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.stela.JavaMathAdapter;
import fr.cnes.sirius.patrius.stela.orbits.StelaEquinoctialOrbit;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * 
 * Test class computing Squaring
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.3
 * 
 */
public class SquaringTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Simpson's quadrature computation
         * 
         * @featureDescription tests the Simpson's quadrature computation
         * 
         * @coveredRequirements
         */
        SQUARING_POINTS_COMPUTATION
    }

    /**
     * setUp.
     */
    @BeforeClass
    public static void setUp() {
    }

    /**
     * test SimpsonMean. Test method for {@link Squaring#simpsonMean(double[])}
     */
    @Test
    public void testSimpsonMean() {
        // Report.printUtHeader("SimpsonMean", "test Simpson's mean computation", "NOMINAL", "None");
        // Report.printString("test with the x^2 function in I=[0;5]");
        final double[] y = { 0., 0.25, 1., 2.25, 4., 6.25, 9., 12.25, 16., 20.25, 25. };
        double simpsMean = 0.;
        try {
            simpsMean = Squaring.simpsonMean(y);
        } catch (final PatriusException e) {
            e.printStackTrace();
        }
        Assert.assertEquals(8.3333333333333333, simpsMean, 1E-15);
    }

    /**
     * test SimpsonMean. Test method for
     * {@link fr.cnes.los.stela.processing.mathematics.diffEquation.squaring#simpsonMean(double[], double)}
     */
    @Test
    public void testSimpsonMean2() {
        // Report.printUtHeader("SimpsonMean", "test Simpson's mean computation", "NOMINAL", "None");
        // Report.printString("test with the x^2 function in I=[0;5]");
        final double[] y = { 0., 0.25, 1., 2.25, 4., 6.25, 9., 12.25, 16., 20.25, 25. };
        double simpsMean = 0.;
        try {
            simpsMean = Squaring.simpsonMean(y, FastMath.PI * 2 / (y.length - 1));
        } catch (final PatriusException e) {
            e.printStackTrace();
        }
        Assert.assertEquals(8.3333333333333333, simpsMean, 1E-15);
    }

    /**
     * @throws PatriusException
     *         thrown if squaring points computation fails
     * @testType UT
     * @testedFeature {@link features#SQUARING_POINTS_COMPUTATION}
     * @testedMethod {@link Squaring#computeSquaringPoints(int, StelaEquinoctialOrbit, double, double)}
     * @description Test for true anomaly squaring points computation
     * @input StelaEquinoctialOrbit, number of squaring points
     * @output the squaring points
     * @testPassCriteria references from Satlight V2 (28/11/2011) (same epsilon value)
     * @referenceVersion 1.3
     * @nonRegressionVersion 1.3
     */
    @Test
    public void computeSquaringPointsType8() throws PatriusException {

        final int squaringPoints = 33;
        final double a = 24500000;
        final double LambdaEq = 0;
        final double eX = 0.732;
        final double eY = 0;
        final double iX = 0;
        final double iY = 0;
        final AbsoluteDate date = new AbsoluteDate();
        final StelaEquinoctialOrbit pv8 = new StelaEquinoctialOrbit(a, eX, eY, iX, iY, LambdaEq,
            FramesFactory.getMOD(false), date, Constants.EGM96_EARTH_MU);
        // final StelaDate date = new StelaDate(21759.0);

        // Test
        double[][] result = new double[squaringPoints][6];
        result = Squaring.computeSquaringPoints(squaringPoints, pv8, -0.7383322391285152,
            0.7383322391285152);

        // Comparison with expected results
        final double[][] expected = new double[squaringPoints][6];
        final double[][] exp = { { 2.4500000000000E+07, 6.1988833567298E+00, 0.732, 0, 0, 0 },
            { 2.4500000000000E+07, 6.2049379383327E+00, 0.732, 0, 0, 0 },
            { 2.4500000000000E+07, 6.2108278039379E+00, 0.732, 0, 0, 0 },
            { 2.4500000000000E+07, 6.2165683782152E+00, 0.732, 0, 0, 0 },
            { 2.4500000000000E+07, 6.2221740516884E+00, 0.732, 0, 0, 0 },
            { 2.4500000000000E+07, 6.2276582983122E+00, 0.732, 0, 0, 0 },
            { 2.4500000000000E+07, 6.2330337818728E+00, 0.732, 0, 0, 0 },
            { 2.4500000000000E+07, 6.2383124526720E+00, 0.732, 0, 0, 0 },
            { 2.4500000000000E+07, 6.2435056357919E+00, 0.732, 0, 0, 0 },
            { 2.4500000000000E+07, 6.2486241120902E+00, 0.732, 0, 0, 0 },
            { 2.4500000000000E+07, 6.2536781929552E+00, 0.732, 0, 0, 0 },
            { 2.4500000000000E+07, 6.2586777897482E+00, 0.732, 0, 0, 0 },
            { 2.4500000000000E+07, 6.2636324787677E+00, 0.732, 0, 0, 0 },
            { 2.4500000000000E+07, 6.2685515625045E+00, 0.732, 0, 0, 0 },
            { 2.4500000000000E+07, 6.2734441278889E+00, 0.732, 0, 0, 0 },
            { 2.4500000000000E+07, 6.2783191021891E+00, 0.732, 0, 0, 0 },
            { 2.4500000000000E+07, 0.0000000000000E+00, 0.732, 0, 0, 0 },
            { 2.4500000000000E+07, 4.8662049904579E-03, 0.732, 0, 0, 0 },
            { 2.4500000000000E+07, 9.7411792906882E-03, 0.732, 0, 0, 0 },
            { 2.4500000000000E+07, 1.4633744675039E-02, 0.732, 0, 0, 0 },
            { 2.4500000000000E+07, 1.9552828411846E-02, 0.732, 0, 0, 0 },
            { 2.4500000000000E+07, 2.4507517431416E-02, 0.732, 0, 0, 0 },
            { 2.4500000000000E+07, 2.9507114224357E-02, 0.732, 0, 0, 0 },
            { 2.4500000000000E+07, 3.4561195089430E-02, 0.732, 0, 0, 0 },
            { 2.4500000000000E+07, 3.9679671387656E-02, 0.732, 0, 0, 0 },
            { 2.4500000000000E+07, 4.4872854507594E-02, 0.732, 0, 0, 0 },
            { 2.4500000000000E+07, 5.0151525306784E-02, 0.732, 0, 0, 0 },
            { 2.4500000000000E+07, 5.5527008867341E-02, 0.732, 0, 0, 0 },
            { 2.4500000000000E+07, 6.1011255491209E-02, 0.732, 0, 0, 0 },
            { 2.4500000000000E+07, 6.6616928964384E-02, 0.732, 0, 0, 0 },
            { 2.4500000000000E+07, 7.2357503241689E-02, 0.732, 0, 0, 0 },
            { 2.4500000000000E+07, 7.8247368846857E-02, 0.732, 0, 0, 0 },
            { 2.4500000000000E+07, 8.4301950449832E-02, 0.732, 0, 0, 0 } };

        for (int k = 0; k < squaringPoints; k++) {
            expected[k] = exp[k];
        }
        for (int j = 0; j < squaringPoints; j++) {
            for (int i = 0; i < 6; i++) {
                Assert.assertEquals(expected[j][i], result[j][i], 1E-13);
            }
        }
    }

    /**
     * @throws PatriusException
     *         thrown if squaring points computation fails
     * @testType UT
     * @testedFeature {@link features#SQUARING_POINTS_COMPUTATION}
     * @testedMethod {@link Squaring#computeSquaringPointsEccentric(int, StelaEquinoctialOrbit)}
     * @description Test for eccentric anomaly squaring points computation
     * @input StelaEquinoctialOrbit, number of squaring points
     * @output the squaring points
     * @testPassCriteria references from Scilab CNES (same tolerance as STELA v2.6: 1E-13)
     * @referenceVersion 3.0
     * @nonRegressionVersion 3.0
     */
    @Test
    public void computeSquaringEccentric() throws PatriusException {

        // Initialization
        final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(36525 * 86400 + 35);
        final StelaEquinoctialOrbit orbit = new StelaEquinoctialOrbit(7078136.29999999981, 0.01412801276516814, 0.,
            0.75470958022277190, 0., 0.,
            FramesFactory.getMOD(false), date, Constants.CNES_STELA_MU);

        // Test
        final StelaEquinoctialOrbit[] actual = Squaring.computeSquaringPointsEccentric(7, orbit);
        final double[][] expectedInv = { // Points are in column
            { 6.9781363000000E+06, 3.4390681500000E+06, -3.6390681500000E+06, -7.1781363000000E+06,
                -3.6390681500000E+06, 3.4390681500000E+06, 6.9781363000000E+06 },
                { 0.0000000000000E+00, -8.5302451005861E+05, -8.5302451005861E+05, -1.2062622311915E-10,
                    8.5302451005861E+05, 8.5302451005861E+05, 0.0000000000000E+00 },
                { 0.0000000000000E+00, 6.0695847713227E+06, 6.0695847713227E+06, 8.5830017570755E-10,
                    -6.0695847713227E+06, -6.0695847713227E+06, 0.0000000000000E+00 },
                { 0.0000000000000E+00, -6.5451379527539E+03, -6.4533167855190E+03, -9.0620718144955E-13,
                    6.4533167855190E+03, 6.5451379527539E+03, 0.0000000000000E+00 },
                { -1.0592558160403E+03, -5.2585999654683E+02, 5.1848275270054E+02, 1.0297424222632E+03,
                    5.1848275270054E+02, -5.2585999654683E+02, -1.0592558160403E+03 },
                { 7.5369967617129E+03, 3.7416882976424E+03, -3.6891964801438E+03, -7.3269980532260E+03,
                    -3.6891964801438E+03, 3.7416882976424E+03, 7.5369967617129E+03 },
        };
        // Transpose expected data
        final double[][] expected = JavaMathAdapter.matrixTranspose(expectedInv);

        // Check
        for (int j = 0; j < actual.length; j++) {
            final PVCoordinates actualPV = new CartesianOrbit(actual[j]).getPVCoordinates();
            final Vector3D actualPos = actualPV.getPosition();
            final Vector3D actualVel = actualPV.getVelocity();
            final double[] actualT1 = { actualPos.getX(), actualPos.getY(), actualPos.getZ(), actualVel.getX(),
                actualVel.getY(), actualVel.getZ() };

            for (int i = 0; i < actualT1.length; i++) {
                if (expected[j][i] != 0) {
                    final double relDiff = MathLib.abs((actualT1[i] - expected[j][i]) / expected[j][i]);
                    Assert.assertEquals(0, relDiff, 1E-10);
                } else {
                    final double absDiff = MathLib.abs(actualT1[i] - expected[j][i]);
                    Assert.assertEquals(0, absDiff, 1E-10);
                }
            }
        }
    }

    /**
     * Test for uneaven use of simpson squaring and squaring points computation
     * 
     * @throws PatriusException
     * @testType UT
     * @referenceVersion 1.3
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testUneavenMethods() throws PatriusException {

        // Simpson mean
        final double[] y = new double[6];
        try {
            Squaring.simpsonMean(y);
            Assert.assertFalse(true);
        } catch (final Exception e) {

            Assert.assertTrue(true);
        }
        try {
            Squaring.simpsonMean(y, 2);
            Assert.assertFalse(true);
        } catch (final Exception e) {

            Assert.assertTrue(true);
        }

        // Squaring points
        try {
            final StelaEquinoctialOrbit kep1 = new StelaEquinoctialOrbit(30000000, 0.1, MathLib.toRadians(178),
                MathLib.toRadians(20),
                MathLib.toRadians(30), MathLib.toRadians(60), FramesFactory.getEME2000(),
                new AbsoluteDate(), Constants.CNES_STELA_MU);
            Squaring.computeSquaringPoints(12, kep1, 1, 6);
            Assert.assertFalse(true);
        } catch (final Exception e) {
            Assert.assertTrue(true);
        }

        try {
            final StelaEquinoctialOrbit kep1 = new StelaEquinoctialOrbit(30000000, 0.1, MathLib.toRadians(178),
                MathLib.toRadians(20),
                MathLib.toRadians(30), MathLib.toRadians(60), FramesFactory.getEME2000(),
                new AbsoluteDate(), Constants.CNES_STELA_MU);
            Squaring.computeSquaringPointsEccentric(12, kep1);
            Assert.assertFalse(true);
        } catch (final Exception e) {
            Assert.assertTrue(true);
        }
    }

}
