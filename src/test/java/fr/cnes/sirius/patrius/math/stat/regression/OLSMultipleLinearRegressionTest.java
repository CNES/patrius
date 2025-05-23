/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.stat.regression;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.linear.Array2DRowRealMatrix;
import fr.cnes.sirius.patrius.math.linear.DefaultRealMatrixChangingVisitor;
import fr.cnes.sirius.patrius.math.linear.MatrixUtils;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealVector;
import fr.cnes.sirius.patrius.math.stat.StatUtils;

public class OLSMultipleLinearRegressionTest extends MultipleLinearRegressionAbstractTest {

    private double[] y;
    private double[][] x;

    @Before
    @Override
    public void setUp() {
        this.y = new double[] { 11.0, 12.0, 13.0, 14.0, 15.0, 16.0 };
        this.x = new double[6][];
        this.x[0] = new double[] { 0, 0, 0, 0, 0 };
        this.x[1] = new double[] { 2.0, 0, 0, 0, 0 };
        this.x[2] = new double[] { 0, 3.0, 0, 0, 0 };
        this.x[3] = new double[] { 0, 0, 4.0, 0, 0 };
        this.x[4] = new double[] { 0, 0, 0, 5.0, 0 };
        this.x[5] = new double[] { 0, 0, 0, 0, 6.0 };
        super.setUp();
    }

    @Override
    protected OLSMultipleLinearRegression createRegression() {
        final OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();
        regression.newSampleData(this.y, this.x);
        return regression;
    }

    @Override
    protected int getNumberOfRegressors() {
        return this.x[0].length + 1;
    }

    @Override
    protected int getSampleSize() {
        return this.y.length;
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotAddSampleDataWithSizeMismatch() {
        final double[] y = new double[] { 1.0, 2.0 };
        final double[][] x = new double[1][];
        x[0] = new double[] { 1.0, 0 };
        this.createRegression().newSampleData(y, x);
    }

    @Test
    public void testPerfectFit() {
        final double[] betaHat = this.regression.estimateRegressionParameters();
        TestUtils.assertEquals(betaHat,
            new double[] { 11.0, 1.0 / 2.0, 2.0 / 3.0, 3.0 / 4.0, 4.0 / 5.0, 5.0 / 6.0 },
            1e-14);
        final double[] residuals = this.regression.estimateResiduals();
        TestUtils.assertEquals(residuals, new double[] { 0d, 0d, 0d, 0d, 0d, 0d },
            1e-14);
        final RealMatrix errors =
            new Array2DRowRealMatrix(this.regression.estimateRegressionParametersVariance(), false);
        final double[] s = { 1.0, -1.0 / 2.0, -1.0 / 3.0, -1.0 / 4.0, -1.0 / 5.0, -1.0 / 6.0 };
        final RealMatrix referenceVariance = new Array2DRowRealMatrix(s.length, s.length);
        referenceVariance.walkInOptimizedOrder(new DefaultRealMatrixChangingVisitor(){
            @Override
            public double visit(final int row, final int column, final double value) {
                if (row == 0) {
                    return s[column];
                }
                final double x = s[row] * s[column];
                return (row == column) ? 2 * x : x;
            }
        });
        Assert.assertEquals(0.0,
            errors.subtract(referenceVariance).getNorm(),
            5.0e-16 * referenceVariance.getNorm());
        Assert.assertEquals(1, ((OLSMultipleLinearRegression) this.regression).calculateRSquared(), 1E-12);
    }

    /**
     * Test Longley dataset against certified values provided by NIST.
     * Data Source: J. Longley (1967) "An Appraisal of Least Squares
     * Programs for the Electronic Computer from the Point of View of the User"
     * Journal of the American Statistical Association, vol. 62. September,
     * pp. 819-841.
     * 
     * Certified values (and data) are from NIST:
     * http://www.itl.nist.gov/div898/strd/lls/data/LINKS/DATA/Longley.dat
     */
    @Test
    public void testLongly() {
        // Y values are first, then independent vars
        // Each row is one observation
        final double[] design = new double[] {
            60323, 83.0, 234289, 2356, 1590, 107608, 1947,
            61122, 88.5, 259426, 2325, 1456, 108632, 1948,
            60171, 88.2, 258054, 3682, 1616, 109773, 1949,
            61187, 89.5, 284599, 3351, 1650, 110929, 1950,
            63221, 96.2, 328975, 2099, 3099, 112075, 1951,
            63639, 98.1, 346999, 1932, 3594, 113270, 1952,
            64989, 99.0, 365385, 1870, 3547, 115094, 1953,
            63761, 100.0, 363112, 3578, 3350, 116219, 1954,
            66019, 101.2, 397469, 2904, 3048, 117388, 1955,
            67857, 104.6, 419180, 2822, 2857, 118734, 1956,
            68169, 108.4, 442769, 2936, 2798, 120445, 1957,
            66513, 110.8, 444546, 4681, 2637, 121950, 1958,
            68655, 112.6, 482704, 3813, 2552, 123366, 1959,
            69564, 114.2, 502601, 3931, 2514, 125368, 1960,
            69331, 115.7, 518173, 4806, 2572, 127852, 1961,
            70551, 116.9, 554894, 4007, 2827, 130081, 1962
        };

        final int nobs = 16;
        final int nvars = 6;

        // Estimate the model
        final OLSMultipleLinearRegression model = new OLSMultipleLinearRegression();
        model.newSampleData(design, nobs, nvars);

        // Check expected beta values from NIST
        double[] betaHat = model.estimateRegressionParameters();
        TestUtils.assertEquals(betaHat,
            new double[] { -3482258.63459582, 15.0618722713733,
                -0.358191792925910E-01, -2.02022980381683,
                -1.03322686717359, -0.511041056535807E-01,
                1829.15146461355 }, 2E-8); //

        // Check expected residuals from R
        double[] residuals = model.estimateResiduals();
        TestUtils.assertEquals(residuals, new double[] {
            267.340029759711, -94.0139423988359, 46.28716775752924,
            -410.114621930906, 309.7145907602313, -249.3112153297231,
            -164.0489563956039, -13.18035686637081, 14.30477260005235,
            455.394094551857, -17.26892711483297, -39.0550425226967,
            -155.5499735953195, -85.6713080421283, 341.9315139607727,
            -206.7578251937366 },
            1E-8);

        // Check standard errors from NIST
        double[] errors = model.estimateRegressionParametersStandardErrors();
        TestUtils.assertEquals(new double[] { 890420.383607373,
            84.9149257747669,
            0.334910077722432E-01,
            0.488399681651699,
            0.214274163161675,
            0.226073200069370,
            455.478499142212 }, errors, 1E-6);

        // Check regression standard error against R
        Assert.assertEquals(304.8540735619638, model.estimateRegressionStandardError(), 1E-10);

        // Check R-Square statistics against R
        Assert.assertEquals(0.995479004577296, model.calculateRSquared(), 1E-12);
        Assert.assertEquals(0.992465007628826, model.calculateAdjustedRSquared(), 1E-12);

        this.checkVarianceConsistency(model);

        // Estimate model without intercept
        model.setNoIntercept(true);
        model.newSampleData(design, nobs, nvars);

        // Check expected beta values from R
        betaHat = model.estimateRegressionParameters();
        TestUtils.assertEquals(betaHat,
            new double[] { -52.99357013868291, 0.07107319907358,
                -0.42346585566399, -0.57256866841929,
                -0.41420358884978, 48.41786562001326 }, 1E-11);

        // Check standard errors from R
        errors = model.estimateRegressionParametersStandardErrors();
        TestUtils.assertEquals(new double[] { 129.54486693117232, 0.03016640003786,
            0.41773654056612, 0.27899087467676, 0.32128496193363,
            17.68948737819961 }, errors, 1E-11);

        // Check expected residuals from R
        residuals = model.estimateResiduals();
        TestUtils.assertEquals(residuals, new double[] {
            279.90274927293092, -130.32465380836874, 90.73228661967445, -401.31252201634948,
            -440.46768772620027, -543.54512853774793, 201.32111639536299, 215.90889365977932,
            73.09368242049943, 913.21694494481869, 424.82484953610174, -8.56475876776709,
            -361.32974610842876, 27.34560497213464, 151.28955976355002, -492.49937355336846 },
            1E-10);

        // Check regression standard error against R
        Assert.assertEquals(475.1655079819517, model.estimateRegressionStandardError(), 1E-10);

        // Check R-Square statistics against R
        Assert.assertEquals(0.9999670130706, model.calculateRSquared(), 1E-12);
        Assert.assertEquals(0.999947220913, model.calculateAdjustedRSquared(), 1E-12);

    }

    /**
     * Test R Swiss fertility dataset against R.
     * Data Source: R datasets package
     */
    @Test
    public void testSwissFertility() {
        final double[] design = new double[] {
            80.2, 17.0, 15, 12, 9.96,
            83.1, 45.1, 6, 9, 84.84,
            92.5, 39.7, 5, 5, 93.40,
            85.8, 36.5, 12, 7, 33.77,
            76.9, 43.5, 17, 15, 5.16,
            76.1, 35.3, 9, 7, 90.57,
            83.8, 70.2, 16, 7, 92.85,
            92.4, 67.8, 14, 8, 97.16,
            82.4, 53.3, 12, 7, 97.67,
            82.9, 45.2, 16, 13, 91.38,
            87.1, 64.5, 14, 6, 98.61,
            64.1, 62.0, 21, 12, 8.52,
            66.9, 67.5, 14, 7, 2.27,
            68.9, 60.7, 19, 12, 4.43,
            61.7, 69.3, 22, 5, 2.82,
            68.3, 72.6, 18, 2, 24.20,
            71.7, 34.0, 17, 8, 3.30,
            55.7, 19.4, 26, 28, 12.11,
            54.3, 15.2, 31, 20, 2.15,
            65.1, 73.0, 19, 9, 2.84,
            65.5, 59.8, 22, 10, 5.23,
            65.0, 55.1, 14, 3, 4.52,
            56.6, 50.9, 22, 12, 15.14,
            57.4, 54.1, 20, 6, 4.20,
            72.5, 71.2, 12, 1, 2.40,
            74.2, 58.1, 14, 8, 5.23,
            72.0, 63.5, 6, 3, 2.56,
            60.5, 60.8, 16, 10, 7.72,
            58.3, 26.8, 25, 19, 18.46,
            65.4, 49.5, 15, 8, 6.10,
            75.5, 85.9, 3, 2, 99.71,
            69.3, 84.9, 7, 6, 99.68,
            77.3, 89.7, 5, 2, 100.00,
            70.5, 78.2, 12, 6, 98.96,
            79.4, 64.9, 7, 3, 98.22,
            65.0, 75.9, 9, 9, 99.06,
            92.2, 84.6, 3, 3, 99.46,
            79.3, 63.1, 13, 13, 96.83,
            70.4, 38.4, 26, 12, 5.62,
            65.7, 7.7, 29, 11, 13.79,
            72.7, 16.7, 22, 13, 11.22,
            64.4, 17.6, 35, 32, 16.92,
            77.6, 37.6, 15, 7, 4.97,
            67.6, 18.7, 25, 7, 8.65,
            35.0, 1.2, 37, 53, 42.34,
            44.7, 46.6, 16, 29, 50.43,
            42.8, 27.7, 22, 29, 58.33
        };

        final int nobs = 47;
        final int nvars = 4;

        // Estimate the model
        OLSMultipleLinearRegression model = new OLSMultipleLinearRegression();
        model.newSampleData(design, nobs, nvars);

        // Check expected beta values from R
        double[] betaHat = model.estimateRegressionParameters();
        TestUtils.assertEquals(betaHat,
            new double[] { 91.05542390271397,
                -0.22064551045715,
                -0.26058239824328,
                -0.96161238456030,
                0.12441843147162 }, 1E-12);

        // Check expected residuals from R
        double[] residuals = model.estimateResiduals();
        TestUtils.assertEquals(residuals, new double[] {
            7.1044267859730512, 1.6580347433531366,
            4.6944952770029644, 8.4548022690166160, 13.6547432343186212,
            -9.3586864458500774, 7.5822446330520386, 15.5568995563859289,
            0.8113090736598980, 7.1186762732484308, 7.4251378771228724,
            2.6761316873234109, 0.8351584810309354, 7.1769991119615177,
            -3.8746753206299553, -3.1337779476387251, -0.1412575244091504,
            1.1186809170469780, -6.3588097346816594, 3.4039270429434074,
            2.3374058329820175, -7.9272368576900503, -7.8361010968497959,
            -11.2597369269357070, 0.9445333697827101, 6.6544245101380328,
            -0.9146136301118665, -4.3152449403848570, -4.3536932047009183,
            -3.8907885169304661, -6.3027643926302188, -7.8308982189289091,
            -3.1792280015332750, -6.7167298771158226, -4.8469946718041754,
            -10.6335664353633685, 11.1031134362036958, 6.0084032641811733,
            5.4326230830188482, -7.2375578629692230, 2.1671550814448222,
            15.0147574652763112, 4.8625103516321015, -7.1597256413907706,
            -0.4515205619767598, -10.2916870903837587, -15.7812984571900063 },
            1E-12);

        // Check standard errors from R
        double[] errors = model.estimateRegressionParametersStandardErrors();
        TestUtils.assertEquals(new double[] { 6.94881329475087,
            0.07360008972340,
            0.27410957467466,
            0.19454551679325,
            0.03726654773803 }, errors, 1E-10);

        // Check regression standard error against R
        Assert.assertEquals(7.73642194433223, model.estimateRegressionStandardError(), 1E-12);

        // Check R-Square statistics against R
        Assert.assertEquals(0.649789742860228, model.calculateRSquared(), 1E-12);
        Assert.assertEquals(0.6164363850373927, model.calculateAdjustedRSquared(), 1E-12);

        this.checkVarianceConsistency(model);

        // Estimate the model with no intercept
        model = new OLSMultipleLinearRegression();
        model.setNoIntercept(true);
        model.newSampleData(design, nobs, nvars);

        // Check expected beta values from R
        betaHat = model.estimateRegressionParameters();
        TestUtils.assertEquals(betaHat,
            new double[] { 0.52191832900513,
                2.36588087917963,
                -0.94770353802795,
                0.30851985863609 }, 1E-12);

        // Check expected residuals from R
        residuals = model.estimateResiduals();
        TestUtils.assertEquals(residuals, new double[] {
            44.138759883538249, 27.720705122356215, 35.873200836126799,
            34.574619581211977, 26.600168342080213, 15.074636243026923, -12.704904871199814,
            1.497443824078134, 2.691972687079431, 5.582798774291231, -4.422986561283165,
            -9.198581600334345, 4.481765170730647, 2.273520207553216, -22.649827853221336,
            -17.747900013943308, 20.298314638496436, 6.861405135329779, -8.684712790954924,
            -10.298639278062371, -9.896618896845819, 4.568568616351242, -15.313570491727944,
            -13.762961360873966, 7.156100301980509, 16.722282219843990, 26.716200609071898,
            -1.991466398777079, -2.523342564719335, 9.776486693095093, -5.297535127628603,
            -16.639070567471094, -10.302057295211819, -23.549487860816846, 1.506624392156384,
            -17.939174438345930, 13.105792202765040, -1.943329906928462, -1.516005841666695,
            -0.759066561832886, 20.793137744128977, -2.485236153005426, 27.588238710486976,
            2.658333257106881, -15.998337823623046, -5.550742066720694, -14.219077806826615 },
            1E-12);

        // Check standard errors from R
        errors = model.estimateRegressionParametersStandardErrors();
        TestUtils.assertEquals(new double[] { 0.10470063765677, 0.41684100584290,
            0.43370143099691, 0.07694953606522 }, errors, 1E-10);

        // Check regression standard error against R
        Assert.assertEquals(17.24710630547, model.estimateRegressionStandardError(), 1E-10);

        // Check R-Square statistics against R
        Assert.assertEquals(0.946350722085, model.calculateRSquared(), 1E-12);
        Assert.assertEquals(0.9413600915813, model.calculateAdjustedRSquared(), 1E-12);
    }

    /**
     * Test hat matrix computation
     * 
     */
    @Test
    public void testHat() {

        /*
         * This example is from "The Hat Matrix in Regression and ANOVA",
         * David C. Hoaglin and Roy E. Welsch,
         * The American Statistician, Vol. 32, No. 1 (Feb., 1978), pp. 17-22.
         */
        final double[] design = new double[] {
            11.14, .499, 11.1,
            12.74, .558, 8.9,
            13.13, .604, 8.8,
            11.51, .441, 8.9,
            12.38, .550, 8.8,
            12.60, .528, 9.9,
            11.13, .418, 10.7,
            11.7, .480, 10.5,
            11.02, .406, 10.5,
            11.41, .467, 10.7
        };

        final int nobs = 10;
        final int nvars = 2;

        // Estimate the model
        final OLSMultipleLinearRegression model = new OLSMultipleLinearRegression();
        model.newSampleData(design, nobs, nvars);

        final RealMatrix hat = model.calculateHat();

        // Reference data is upper half of symmetric hat matrix
        final double[] referenceData = new double[] {
            .418, -.002, .079, -.274, -.046, .181, .128, .222, .050, .242,
            .242, .292, .136, .243, .128, -.041, .033, -.035, .004,
            .417, -.019, .273, .187, -.126, .044, -.153, .004,
            .604, .197, -.038, .168, -.022, .275, -.028,
            .252, .111, -.030, .019, -.010, -.010,
            .148, .042, .117, .012, .111,
            .262, .145, .277, .174,
            .154, .120, .168,
            .315, .148,
            .187
        };

        // Check against reference data and verify symmetry
        int k = 0;
        for (int i = 0; i < 10; i++) {
            for (int j = i; j < 10; j++) {
                Assert.assertEquals(referenceData[k], hat.getEntry(i, j), 10e-3);
                Assert.assertEquals(hat.getEntry(i, j), hat.getEntry(j, i), 10e-12);
                k++;
            }
        }

        /*
         * Verify that residuals computed using the hat matrix are close to
         * what we get from direct computation, i.e. r = (I - H) y
         */
        final double[] residuals = model.estimateResiduals();
        final RealMatrix I = MatrixUtils.createRealIdentityMatrix(10);
        final double[] hatResiduals = I.subtract(hat).operate(model.getY()).toArray();
        TestUtils.assertEquals(residuals, hatResiduals, 10e-12);
    }

    /**
     * test calculateYVariance
     */
    @Test
    public void testYVariance() {

        // assumes: y = new double[]{11.0, 12.0, 13.0, 14.0, 15.0, 16.0};

        final OLSMultipleLinearRegression model = new OLSMultipleLinearRegression();
        model.newSampleData(this.y, this.x);
        TestUtils.assertEquals(model.calculateYVariance(), 3.5, 0);
    }

    /**
     * Verifies that calculateYVariance and calculateResidualVariance return consistent
     * values with direct variance computation from Y, residuals, respectively.
     */
    protected void checkVarianceConsistency(final OLSMultipleLinearRegression model) {
        // Check Y variance consistency
        TestUtils.assertEquals(StatUtils.variance(model.getY().toArray()), model.calculateYVariance(), 0);

        // Check residual variance consistency
        final double[] residuals = model.calculateResiduals().toArray();
        final RealMatrix X = model.getX();
        TestUtils.assertEquals(
            StatUtils.variance(model.calculateResiduals().toArray()) * (residuals.length - 1),
            model.calculateErrorVariance() * (X.getRowDimension() - X.getColumnDimension()), 1E-20);

    }

    /**
     * Verifies that setting X and Y separately has the same effect as newSample(X,Y).
     */
    @Test
    public void testNewSample2() {
        final double[] y = new double[] { 1, 2, 3, 4 };
        final double[][] x = new double[][] {
            { 19, 22, 33 },
            { 20, 30, 40 },
            { 25, 35, 45 },
            { 27, 37, 47 }
        };
        final OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();
        regression.newSampleData(y, x);
        RealMatrix combinedX = regression.getX().copy();
        RealVector combinedY = regression.getY().copy();
        regression.newXSampleData(x);
        regression.newYSampleData(y);
        Assert.assertEquals(combinedX, regression.getX());
        Assert.assertEquals(combinedY, regression.getY());

        // No intercept
        regression.setNoIntercept(true);
        regression.newSampleData(y, x);
        combinedX = regression.getX().copy();
        combinedY = regression.getY().copy();
        regression.newXSampleData(x);
        regression.newYSampleData(y);
        Assert.assertEquals(combinedX, regression.getX());
        Assert.assertEquals(combinedY, regression.getY());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewSampleDataYNull() {
        this.createRegression().newSampleData(null, new double[][] {});
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewSampleDataXNull() {
        this.createRegression().newSampleData(new double[] {}, null);
    }

    /*
     * This is a test based on the Wampler1 data set
     * http://www.itl.nist.gov/div898/strd/lls/data/Wampler1.shtml
     */
    @Test
    public void testWampler1() {
        final double[] data = new double[] {
            1, 0,
            6, 1,
            63, 2,
            364, 3,
            1365, 4,
            3906, 5,
            9331, 6,
            19608, 7,
            37449, 8,
            66430, 9,
            111111, 10,
            177156, 11,
            271453, 12,
            402234, 13,
            579195, 14,
            813616, 15,
            1118481, 16,
            1508598, 17,
            2000719, 18,
            2613660, 19,
            3368421, 20 };
        final OLSMultipleLinearRegression model = new OLSMultipleLinearRegression();

        final int nvars = 5;
        final int nobs = 21;
        final double[] tmp = new double[(nvars + 1) * nobs];
        int off = 0;
        int off2 = 0;
        for (int i = 0; i < nobs; i++) {
            tmp[off2] = data[off];
            tmp[off2 + 1] = data[off + 1];
            tmp[off2 + 2] = tmp[off2 + 1] * tmp[off2 + 1];
            tmp[off2 + 3] = tmp[off2 + 1] * tmp[off2 + 2];
            tmp[off2 + 4] = tmp[off2 + 1] * tmp[off2 + 3];
            tmp[off2 + 5] = tmp[off2 + 1] * tmp[off2 + 4];
            off2 += (nvars + 1);
            off += 2;
        }
        model.newSampleData(tmp, nobs, nvars);
        final double[] betaHat = model.estimateRegressionParameters();
        TestUtils.assertEquals(betaHat,
            new double[] { 1.0,
                1.0, 1.0,
                1.0, 1.0,
                1.0 }, 1E-8);

        final double[] se = model.estimateRegressionParametersStandardErrors();
        TestUtils.assertEquals(se,
            new double[] { 0.0,
                0.0, 0.0,
                0.0, 0.0,
                0.0 }, 1E-8);

        TestUtils.assertEquals(1.0, model.calculateRSquared(), 1.0e-10);
        TestUtils.assertEquals(0, model.estimateErrorVariance(), 1.0e-7);
        TestUtils.assertEquals(0.00, model.calculateResidualSumOfSquares(), 1.0e-6);

        return;
    }

    /*
     * This is a test based on the Wampler2 data set
     * http://www.itl.nist.gov/div898/strd/lls/data/Wampler2.shtml
     */
    @Test
    public void testWampler2() {
        final double[] data = new double[] {
            1.00000, 0,
            1.11111, 1,
            1.24992, 2,
            1.42753, 3,
            1.65984, 4,
            1.96875, 5,
            2.38336, 6,
            2.94117, 7,
            3.68928, 8,
            4.68559, 9,
            6.00000, 10,
            7.71561, 11,
            9.92992, 12,
            12.75603, 13,
            16.32384, 14,
            20.78125, 15,
            26.29536, 16,
            33.05367, 17,
            41.26528, 18,
            51.16209, 19,
            63.00000, 20 };
        final OLSMultipleLinearRegression model = new OLSMultipleLinearRegression();

        final int nvars = 5;
        final int nobs = 21;
        final double[] tmp = new double[(nvars + 1) * nobs];
        int off = 0;
        int off2 = 0;
        for (int i = 0; i < nobs; i++) {
            tmp[off2] = data[off];
            tmp[off2 + 1] = data[off + 1];
            tmp[off2 + 2] = tmp[off2 + 1] * tmp[off2 + 1];
            tmp[off2 + 3] = tmp[off2 + 1] * tmp[off2 + 2];
            tmp[off2 + 4] = tmp[off2 + 1] * tmp[off2 + 3];
            tmp[off2 + 5] = tmp[off2 + 1] * tmp[off2 + 4];
            off2 += (nvars + 1);
            off += 2;
        }
        model.newSampleData(tmp, nobs, nvars);
        final double[] betaHat = model.estimateRegressionParameters();
        TestUtils.assertEquals(betaHat,
            new double[] {
                1.0,
                1.0e-1,
                1.0e-2,
                1.0e-3, 1.0e-4,
                1.0e-5 }, 1E-8);

        final double[] se = model.estimateRegressionParametersStandardErrors();
        TestUtils.assertEquals(se,
            new double[] { 0.0,
                0.0, 0.0,
                0.0, 0.0,
                0.0 }, 1E-8);
        TestUtils.assertEquals(1.0, model.calculateRSquared(), 1.0e-10);
        TestUtils.assertEquals(0, model.estimateErrorVariance(), 1.0e-7);
        TestUtils.assertEquals(0.00, model.calculateResidualSumOfSquares(), 1.0e-6);
        return;
    }

    /*
     * This is a test based on the Wampler3 data set
     * http://www.itl.nist.gov/div898/strd/lls/data/Wampler3.shtml
     */
    @Test
    public void testWampler3() {
        final double[] data = new double[] {
            760, 0,
            -2042, 1,
            2111, 2,
            -1684, 3,
            3888, 4,
            1858, 5,
            11379, 6,
            17560, 7,
            39287, 8,
            64382, 9,
            113159, 10,
            175108, 11,
            273291, 12,
            400186, 13,
            581243, 14,
            811568, 15,
            1121004, 16,
            1506550, 17,
            2002767, 18,
            2611612, 19,
            3369180, 20 };

        final OLSMultipleLinearRegression model = new OLSMultipleLinearRegression();
        final int nvars = 5;
        final int nobs = 21;
        final double[] tmp = new double[(nvars + 1) * nobs];
        int off = 0;
        int off2 = 0;
        for (int i = 0; i < nobs; i++) {
            tmp[off2] = data[off];
            tmp[off2 + 1] = data[off + 1];
            tmp[off2 + 2] = tmp[off2 + 1] * tmp[off2 + 1];
            tmp[off2 + 3] = tmp[off2 + 1] * tmp[off2 + 2];
            tmp[off2 + 4] = tmp[off2 + 1] * tmp[off2 + 3];
            tmp[off2 + 5] = tmp[off2 + 1] * tmp[off2 + 4];
            off2 += (nvars + 1);
            off += 2;
        }
        model.newSampleData(tmp, nobs, nvars);
        final double[] betaHat = model.estimateRegressionParameters();
        TestUtils.assertEquals(betaHat,
            new double[] {
                1.0,
                1.0,
                1.0,
                1.0,
                1.0,
                1.0 }, 1E-8);

        final double[] se = model.estimateRegressionParametersStandardErrors();
        TestUtils.assertEquals(se,
            new double[] { 2152.32624678170,
                2363.55173469681, 779.343524331583,
                101.475507550350, 5.64566512170752,
                0.112324854679312 }, 1E-8); //

        TestUtils.assertEquals(.999995559025820, model.calculateRSquared(), 1.0e-10);
        TestUtils.assertEquals(5570284.53333333, model.estimateErrorVariance(), 1.0e-6);
        TestUtils.assertEquals(83554268.0000000, model.calculateResidualSumOfSquares(), 1.0e-5);
        return;
    }

    /*
     * This is a test based on the Wampler4 data set
     * http://www.itl.nist.gov/div898/strd/lls/data/Wampler4.shtml
     */
    @Test
    public void testWampler4() {
        final double[] data = new double[] {
            75901, 0,
            -204794, 1,
            204863, 2,
            -204436, 3,
            253665, 4,
            -200894, 5,
            214131, 6,
            -185192, 7,
            221249, 8,
            -138370, 9,
            315911, 10,
            -27644, 11,
            455253, 12,
            197434, 13,
            783995, 14,
            608816, 15,
            1370781, 16,
            1303798, 17,
            2205519, 18,
            2408860, 19,
            3444321, 20 };

        final OLSMultipleLinearRegression model = new OLSMultipleLinearRegression();
        final int nvars = 5;
        final int nobs = 21;
        final double[] tmp = new double[(nvars + 1) * nobs];
        int off = 0;
        int off2 = 0;
        for (int i = 0; i < nobs; i++) {
            tmp[off2] = data[off];
            tmp[off2 + 1] = data[off + 1];
            tmp[off2 + 2] = tmp[off2 + 1] * tmp[off2 + 1];
            tmp[off2 + 3] = tmp[off2 + 1] * tmp[off2 + 2];
            tmp[off2 + 4] = tmp[off2 + 1] * tmp[off2 + 3];
            tmp[off2 + 5] = tmp[off2 + 1] * tmp[off2 + 4];
            off2 += (nvars + 1);
            off += 2;
        }
        model.newSampleData(tmp, nobs, nvars);
        final double[] betaHat = model.estimateRegressionParameters();
        TestUtils.assertEquals(betaHat,
            new double[] {
                1.0,
                1.0,
                1.0,
                1.0,
                1.0,
                1.0 }, 1E-6);

        final double[] se = model.estimateRegressionParametersStandardErrors();
        TestUtils.assertEquals(se,
            new double[] { 215232.624678170,
                236355.173469681, 77934.3524331583,
                10147.5507550350, 564.566512170752,
                11.2324854679312 }, 1E-8);

        TestUtils.assertEquals(.957478440825662, model.calculateRSquared(), 1.0e-10);
        TestUtils.assertEquals(55702845333.3333, model.estimateErrorVariance(), 1.0e-4);
        TestUtils.assertEquals(835542680000.000, model.calculateResidualSumOfSquares(), 1.0e-3);
        return;
    }
}
