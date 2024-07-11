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
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for the coverage of the class MeeusSunStela.
 * 
 * @author Cedric Dental
 * 
 * @version
 * 
 * @since 1.3
 * 
 */
public class JavaMathAdapterTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle JavaMathAdapter test class
         * 
         * @featureDescription perform simple tests
         * 
         * @coveredRequirements NA
         */
        MATH_STELA
    }

    /**
     * Test matrix transposition
     * 
     * @throws PatriusException
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#MATH_STELA}
     * 
     * @testedMethod {@link JavaMathAdapter#matrixTranspose(double[][])}
     * 
     * @description coverage test
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testMatrixTransposition() throws PatriusException {

        final double[][] M1 = { { 76, -63, -6 },
            { 34, -38, -49 },
            { -30, -42, 71 },
            { -76, -76, 83 },
            { -67, 79, 85 },
            { -18, 48, -7 } };

        final double[][] expRes = JavaMathAdapter.matrixTranspose(M1);

        for (int i = 0; i < expRes.length; i++) {
            for (int j = 0; j < expRes[0].length; j++) {
                Assert.assertEquals(expRes[i][j], M1[j][i]);
            }

        }
    }

    /**
     * Test multiplication between a vector and a 3D matrix
     * 
     * @throws PatriusException
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#MATH_STELA}
     * 
     * @testedMethod {@link JavaMathAdapter#threeDMatrixVectorMultiply(double[][][], double[])}
     * 
     * @description coverage test
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testThreeDMatrixVectorMultiply() throws PatriusException {

        final double[][][] M1 = { { { 100, -83, 92, }, {
            -70, 31, -19 },
        { 76, -52, -23 },
        { 25, -9, -64 },
        { 51, 93, 17 },
        { -72, 58, -78 },
        }, {

        { 63, -94, 86 },
        { -96, 1, -4 },
        { 34, -69, 36 },
        { -15, -70, -23 },
        { -80, -30, -7 },
        { -9, -53, -66 },
        }, {

        { -30, -92, -3 },
        { 91, 92, -41 },
        { 57, -95, -30 },
        { -34, -22, 46 },
        { 80, -19, -92 },
        { 71, -54, 65 },
        }, {

        { 17, -51, -78 },
        { -82, -13, 86 },
        { -33, -78, -10 },
        { -23, 60, -79 },
        { 18, -37, 82 },
        { 28, 95, -84 },
        }, {

        { 14, 45, -74 },
        { 75, -12, -19 },
        { 11, 91, 54 },
        { 19, 65, 7 },
        { -32, 60, -73 },
        { 76, 54, -97 },
        }, {

        { 76, -63, -6 },
        { 34, -38, -49 },
        { -30, -42, 71 },
        { -76, -76, 83 },
        { -67, 79, 85 },
        { -18, 48, -7 } } };

        final double[] M2 = { 100, 60, -45, 0, -8, 41 };

        final double[][] expectedProduct = {

            { -980, -719, 2166, -731, 8777, 10788 },
            { -2466, -8168, -1467, 1821, 1419, -5354 },
            { 5761, 4090, 1991, -6290, -14363, -7702 } };

        final double[][] res = JavaMathAdapter.threeDMatrixVectorMultiply(M1, M2);

        for (int i = 0; i < res.length; i++) {
            for (int j = 0; j < res[0].length; j++) {
                Assert.assertEquals(expectedProduct[i][j], res[i][j]);
            }

        }
        // mismatch dimension:
        final double[][][] M3 = new double[6][4][7];
        try {
            JavaMathAdapter.threeDMatrixVectorMultiply(M3, M2);
            Assert.fail();

        } catch (final Exception e) {
            // Nothing to do ...

        }

    }

    /**
     * Test matrix to vector
     * 
     * @throws PatriusException
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#MATH_STELA}
     * 
     * @testedMethod {@link JavaMathAdapter#matrixToVector(double[][], double[], int)}
     * @testedMethod {@link JavaMathAdapter#vectorToMatrix(double[], double[][])}
     * 
     * @description coverage test
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testMatrix2Vector() throws PatriusException {
        final double[][] M1 = { { 76, -63, -6 },
            { 34, -38, -49 },
            { -30, -42, 71 },
            { -76, -76, 83 },
            { -67, 79, 85 },
            { -18, 48, -7 } };
        final double[] V = { 76, 34, -30, -76, -67, -18, -63, -38, -42, -76, 79, 48, -6, -49, 71, 83, 85, -7 };

        final double[] resV = new double[V.length];
        JavaMathAdapter.matrixToVector(M1, resV, 0);
        final double[][] resM = new double[M1.length][M1[0].length];
        JavaMathAdapter.vectorToMatrix(V, resM);

        for (int i = 0; i < resM.length; i++) {
            for (int j = 0; j < resM[0].length; j++) {
                Assert.assertEquals(M1[i][j], resM[i][j]);

            }

        }
        for (int i = 0; i < resV.length; i++) {
            Assert.assertEquals(V[i], resV[i]);
        }

        // mismatch dimension:
        final double[][] M3 = new double[6][4];
        try {
            JavaMathAdapter.vectorToMatrix(new double[256], M3);
            Assert.fail();

        } catch (final Exception e) {
            // Nothing to do ...
        }
        // mismatch dimension:
        try {
            JavaMathAdapter.matrixToVector(new double[3][5], new double[5], 2);
            Assert.fail();

        } catch (final Exception e) {
            // Nothing to do ...

        }

    }

    /**
     * Test matrix addition
     * 
     * @throws PatriusException
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#MATH_STELA}
     * 
     * @testedMethod {@link JavaMathAdapter#matrixAdd(double[][], double[][])}
     * 
     * @description coverage test
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testMatrixAdd() throws PatriusException {

        final double[][] M1 = { { 76, -63, -6 },
            { 34, -38, -49 },
            { -30, -42, 71 },
            { -76, -76, 83 },
            { -67, 79, 85 },
            { -18, 48, -7 } };
        final double[][] M2 = { { 14, 45, -74 },
            { 75, -12, -19 },
            { 11, 91, 54 },
            { 19, 65, 7 },
            { -32, 60, -73 },
            { 76, 54, -97 } };

        final double[][] res = { { 90, -18, -80 },
            { 109, -50, -68 },
            { -19, 49, 125 },
            { -57, -11, 90 },
            { -99, 139, 12 },
            { 58, 102, -104 } };

        final double[][] expRes = JavaMathAdapter.matrixAdd(M1, M2);

        for (int i = 0; i < res.length; i++) {
            for (int j = 0; j < res[0].length; j++) {
                Assert.assertEquals(expRes[i][j], res[i][j]);
            }

        }
        // mismatch dimension:
        try {
            JavaMathAdapter.matrixAdd(new double[3][5], new double[5][2]);
            Assert.fail();

        } catch (final Exception e) {
            // Nothing to do ...

        }
        // mismatch dimension 2:
        try {
            JavaMathAdapter.matrixAdd(new double[3][5], new double[5][5]);
            Assert.fail();

        } catch (final Exception e) {
            // Nothing to do ...

        }
    }

    /**
     * Test matrix multiplication
     * 
     * @throws PatriusException
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#MATH_STELA}
     * 
     * @testedMethod {@link JavaMathAdapter#matrixMultiply(double[][], double[][])}
     * 
     * @description coverage test
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testMatrixMultiply() throws PatriusException {

        final double[][] M1 = { { 76, -63, -6 },
            { 34, -38, -49 },
            { -30, -42, 71 },
            { -76, -76, 83 },
            { -67, 79, 85 },
            { -18, 48, -7 } };
        final double[][] M2 = { { 14, 45, -74 },
            { 75, -12, -19 },
            { 11, 91, 54 },
            { 19, 65, 7 },
            { -32, 60, -73 },
            { 76, 54, -97 } };

        final double[][] res = { { -1327, 6570, -5221, -2693, -5774, 2956 },
            { 2392, 3937, -5730, -2167, 209, 5285 },
            { -7564, -3095, -318, -2803, -6743, -11435 },
            { -10626, -6365, -3270, -5803, -8187, -17931 },
            { -3673, -7588, 11042, 4457, 679, -9071 },
            { 2426, -1793, 3792, 2729, 3967, 1903 } };

        final double[][] expRes = JavaMathAdapter.matrixMultiply(M1, JavaMathAdapter.matrixTranspose(M2));

        for (int i = 0; i < res.length; i++) {
            for (int j = 0; j < res[0].length; j++) {
                Assert.assertEquals(expRes[i][j], res[i][j]);
            }

        }
        // mismatch dimension:
        try {
            JavaMathAdapter.matrixMultiply(new double[3][5], new double[6][2]);
            Assert.fail();

        } catch (final Exception e) {
            // Nothing to do ...

        }
    }

    /**
     * Test matrix vector multiplication
     * 
     * @throws PatriusException
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#MATH_STELA}
     * 
     * @testedMethod {@link JavaMathAdapter#matrixVectorMultiply(double[][], double[])}
     * 
     * @description coverage test
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testMatrixVectorMultiply() throws PatriusException {

        final double[] M1 = { 76, -63, -6 };
        final double[][] M2 = { { 14, 45, -74 },
            { 75, -12, -19 },
            { 11, 91, 54 },
            { 19, 65, 7 },
            { -32, 60, -73 },
            { 76, 54, -97 } };

        final double[] res = { -1327,
            6570,
            -5221,
            -2693,
            -5774,
            2956 };

        final double[] expRes = JavaMathAdapter.matrixVectorMultiply(M2, M1);
        for (int i = 0; i < res.length; i++) {

            Assert.assertEquals(expRes[i], res[i]);

        }

        // mismatch dimension:
        try {
            JavaMathAdapter.matrixVectorMultiply(new double[3][5], new double[2]);
            Assert.fail();

        } catch (final Exception e) {
            // Nothing to do ...

        }
    }

    /**
     * Test matrix vector multiplication
     * 
     * @throws PatriusException
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#MATH_STELA}
     * 
     * @testedMethod {@link JavaMathAdapter#scalarMultiply(double, double[][])}
     * 
     * @description coverage test
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testScalarMultiply() {

        final double[][] M2 = { { 14, 45, -74 },
            { 75, -12, -19 },
            { 11, 91, 54 },
            { 19, 65, 7 },
            { -32, 60, -73 },
            { 76, 54, -97 } };
        final double[][] expRes = { { -14, -45, 74 },
            { -75, 12, 19 },
            { -11, -91, -54 },
            { -19, -65, -7 },
            { 32, -60, 73 },
            { -76, -54, 97 } };
        final double[][] res = JavaMathAdapter.scalarMultiply(-1, M2);

        for (int i = 0; i < res.length; i++) {
            for (int j = 0; j < res[0].length; j++) {
                Assert.assertEquals(expRes[i][j], res[i][j]);
            }

        }
    }

    /**
     * Test mod
     * 
     * @throws PatriusException
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#MATH_STELA}
     * 
     * @testedMethod {@link JavaMathAdapter#mod(double, double)}
     * 
     * @description coverage test
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testMod() {

        final double x = 0;
        final int mod = 1;

        Assert.assertEquals(0, (int) JavaMathAdapter.mod(x, mod));

    }
}
