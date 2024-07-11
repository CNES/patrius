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
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.stat.regression;

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;

/**
 * Test cases for the TestStatistic class.
 * 
 * @version $Id: SimpleRegressionTest.java 18108 2017-10-04 06:45:27Z bignon $
 */

public final class SimpleRegressionTest {

    /*
     * NIST "Norris" refernce data set from
     * http://www.itl.nist.gov/div898/strd/lls/data/LINKS/DATA/Norris.dat
     * Strangely, order is {y,x}
     */
    private final double[][] data = { { 0.1, 0.2 }, { 338.8, 337.4 }, { 118.1, 118.2 },
        { 888.0, 884.6 }, { 9.2, 10.1 }, { 228.1, 226.5 }, { 668.5, 666.3 }, { 998.5, 996.3 },
        { 449.1, 448.6 }, { 778.9, 777.0 }, { 559.2, 558.2 }, { 0.3, 0.4 }, { 0.1, 0.6 }, { 778.1, 775.5 },
        { 668.8, 666.9 }, { 339.3, 338.0 }, { 448.9, 447.5 }, { 10.8, 11.6 }, { 557.7, 556.0 },
        { 228.3, 228.1 }, { 998.0, 995.8 }, { 888.8, 887.6 }, { 119.6, 120.2 }, { 0.3, 0.3 },
        { 0.6, 0.3 }, { 557.6, 556.8 }, { 339.3, 339.1 }, { 888.0, 887.2 }, { 998.5, 999.0 },
        { 778.9, 779.0 }, { 10.2, 11.1 }, { 117.6, 118.3 }, { 228.9, 229.2 }, { 668.4, 669.1 },
        { 449.2, 448.9 }, { 0.2, 0.5 }
    };

    /*
     * Correlation example from
     * http://www.xycoon.com/correlation.htm
     */
    private final double[][] corrData = { { 101.0, 99.2 }, { 100.1, 99.0 }, { 100.0, 100.0 },
        { 90.6, 111.6 }, { 86.5, 122.2 }, { 89.7, 117.6 }, { 90.6, 121.1 }, { 82.8, 136.0 },
        { 70.1, 154.2 }, { 65.4, 153.6 }, { 61.3, 158.5 }, { 62.5, 140.6 }, { 63.6, 136.2 },
        { 52.6, 168.0 }, { 59.7, 154.3 }, { 59.5, 149.0 }, { 61.3, 165.5 }
    };

    /*
     * From Moore and Mcabe, "Introduction to the Practice of Statistics"
     * Example 10.3
     */
    private final double[][] infData = { { 15.6, 5.2 }, { 26.8, 6.1 }, { 37.8, 8.7 }, { 36.4, 8.5 },
        { 35.5, 8.8 }, { 18.6, 4.9 }, { 15.3, 4.5 }, { 7.9, 2.5 }, { 0.0, 1.1 }
    };

    /*
     * Points to remove in the remove tests
     */
    private final double[][] removeSingle = { this.infData[1] };
    private final double[][] removeMultiple = { this.infData[1], this.infData[2] };
    private final double removeX = this.infData[0][0];
    private final double removeY = this.infData[0][1];

    /*
     * Data with bad linear fit
     */
    private final double[][] infData2 = { { 1, 1 }, { 2, 0 }, { 3, 5 }, { 4, 2 },
        { 5, -1 }, { 6, 12 }
    };

    /*
     * Data from NIST NOINT1
     */
    private final double[][] noint1 = {
        { 130.0, 60.0 },
        { 131.0, 61.0 },
        { 132.0, 62.0 },
        { 133.0, 63.0 },
        { 134.0, 64.0 },
        { 135.0, 65.0 },
        { 136.0, 66.0 },
        { 137.0, 67.0 },
        { 138.0, 68.0 },
        { 139.0, 69.0 },
        { 140.0, 70.0 }
    };

    /*
     * Data from NIST NOINT2
     */
    private final double[][] noint2 = {
        { 3.0, 4 },
        { 4, 5 },
        { 4, 6 }
    };

    @Test
    public void testRegressIfaceMethod() {
        final SimpleRegression regression = new SimpleRegression(true);
        final UpdatingMultipleLinearRegression iface = regression;
        final SimpleRegression regressionNoint = new SimpleRegression(false);
        final SimpleRegression regressionIntOnly = new SimpleRegression(false);
        for (final double[] element : this.data) {
            iface.addObservation(new double[] { element[1] }, element[0]);
            regressionNoint.addData(element[1], element[0]);
            regressionIntOnly.addData(1.0, element[0]);
        }

        // should not be null
        final RegressionResults fullReg = iface.regress();
        Assert.assertNotNull(fullReg);
        Assert.assertEquals("intercept", regression.getIntercept(), fullReg.getParameterEstimate(0), 1.0e-16);
        Assert.assertEquals("intercept std err", regression.getInterceptStdErr(), fullReg.getStdErrorOfEstimate(0),
            1.0E-16);
        Assert.assertEquals("slope", regression.getSlope(), fullReg.getParameterEstimate(1), 1.0e-16);
        Assert.assertEquals("slope std err", regression.getSlopeStdErr(), fullReg.getStdErrorOfEstimate(1), 1.0E-16);
        Assert.assertEquals("number of observations", regression.getN(), fullReg.getN());
        Assert.assertEquals("r-square", regression.getRSquare(), fullReg.getRSquared(), 1.0E-16);
        Assert.assertEquals("SSR", regression.getRegressionSumSquares(), fullReg.getRegressionSumSquares(), 1.0E-16);
        Assert.assertEquals("MSE", regression.getMeanSquareError(), fullReg.getMeanSquareError(), 1.0E-16);
        Assert.assertEquals("SSE", regression.getSumSquaredErrors(), fullReg.getErrorSumSquares(), 1.0E-16);

        final RegressionResults noInt = iface.regress(new int[] { 1 });
        Assert.assertNotNull(noInt);
        Assert.assertEquals("slope", regressionNoint.getSlope(), noInt.getParameterEstimate(0), 1.0e-12);
        Assert.assertEquals("slope std err", regressionNoint.getSlopeStdErr(), noInt.getStdErrorOfEstimate(0), 1.0E-16);
        Assert.assertEquals("number of observations", regressionNoint.getN(), noInt.getN());
        Assert.assertEquals("r-square", regressionNoint.getRSquare(), noInt.getRSquared(), 1.0E-16);
        Assert.assertEquals("SSR", regressionNoint.getRegressionSumSquares(), noInt.getRegressionSumSquares(), 1.0E-8);
        Assert.assertEquals("MSE", regressionNoint.getMeanSquareError(), noInt.getMeanSquareError(), 1.0E-16);
        Assert.assertEquals("SSE", regressionNoint.getSumSquaredErrors(), noInt.getErrorSumSquares(), 1.0E-16);

        final RegressionResults onlyInt = iface.regress(new int[] { 0 });
        Assert.assertNotNull(onlyInt);
        Assert.assertEquals("slope", regressionIntOnly.getSlope(), onlyInt.getParameterEstimate(0), 1.0e-12);
        Assert.assertEquals("slope std err", regressionIntOnly.getSlopeStdErr(), onlyInt.getStdErrorOfEstimate(0),
            1.0E-12);
        Assert.assertEquals("number of observations", regressionIntOnly.getN(), onlyInt.getN());
        Assert.assertEquals("r-square", regressionIntOnly.getRSquare(), onlyInt.getRSquared(), 1.0E-14);
        Assert.assertEquals("SSE", regressionIntOnly.getSumSquaredErrors(), onlyInt.getErrorSumSquares(), 1.0E-8);
        Assert.assertEquals("SSR", regressionIntOnly.getRegressionSumSquares(), onlyInt.getRegressionSumSquares(),
            1.0E-8);
        Assert.assertEquals("MSE", regressionIntOnly.getMeanSquareError(), onlyInt.getMeanSquareError(), 1.0E-8);

    }

    /**
     * Verify that regress generates exceptions as advertised for bad model specifications.
     */
    @Test
    public void testRegressExceptions() {
        // No intercept
        final SimpleRegression noIntRegression = new SimpleRegression(false);
        noIntRegression.addData(this.noint2[0][1], this.noint2[0][0]);
        noIntRegression.addData(this.noint2[1][1], this.noint2[1][0]);
        noIntRegression.addData(this.noint2[2][1], this.noint2[2][0]);
        try { // null array
            noIntRegression.regress(null);
            Assert.fail("Expecting MathIllegalArgumentException for null array");
        } catch (final MathIllegalArgumentException ex) {
            // Expected
        }
        try { // empty array
            noIntRegression.regress(new int[] {});
            Assert.fail("Expecting MathIllegalArgumentException for empty array");
        } catch (final MathIllegalArgumentException ex) {
            // Expected
        }
        try { // more than 1 regressor
            noIntRegression.regress(new int[] { 0, 1 });
            Assert.fail("Expecting ModelSpecificationException - too many regressors");
        } catch (final ModelSpecificationException ex) {
            // Expected
        }
        try { // invalid regressor
            noIntRegression.regress(new int[] { 1 });
            Assert.fail("Expecting OutOfRangeException - invalid regression");
        } catch (final OutOfRangeException ex) {
            // Expected
        }

        // With intercept
        final SimpleRegression regression = new SimpleRegression(true);
        regression.addData(this.noint2[0][1], this.noint2[0][0]);
        regression.addData(this.noint2[1][1], this.noint2[1][0]);
        regression.addData(this.noint2[2][1], this.noint2[2][0]);
        try { // null array
            regression.regress(null);
            Assert.fail("Expecting MathIllegalArgumentException for null array");
        } catch (final MathIllegalArgumentException ex) {
            // Expected
        }
        try { // empty array
            regression.regress(new int[] {});
            Assert.fail("Expecting MathIllegalArgumentException for empty array");
        } catch (final MathIllegalArgumentException ex) {
            // Expected
        }
        try { // more than 2 regressors
            regression.regress(new int[] { 0, 1, 2 });
            Assert.fail("Expecting ModelSpecificationException - too many regressors");
        } catch (final ModelSpecificationException ex) {
            // Expected
        }
        try { // wrong order
            regression.regress(new int[] { 1, 0 });
            Assert.fail("Expecting ModelSpecificationException - invalid regression");
        } catch (final ModelSpecificationException ex) {
            // Expected
        }
        try { // out of range
            regression.regress(new int[] { 3, 4 });
            Assert.fail("Expecting OutOfRangeException");
        } catch (final OutOfRangeException ex) {
            // Expected
        }
        try { // out of range
            regression.regress(new int[] { 0, 2 });
            Assert.fail("Expecting OutOfRangeException");
        } catch (final OutOfRangeException ex) {
            // Expected
        }
        try { // out of range
            regression.regress(new int[] { 2 });
            Assert.fail("Expecting OutOfRangeException");
        } catch (final OutOfRangeException ex) {
            // Expected
        }
    }

    @Test
    public void testNoInterceot_noint2() {
        final SimpleRegression regression = new SimpleRegression(false);
        regression.addData(this.noint2[0][1], this.noint2[0][0]);
        regression.addData(this.noint2[1][1], this.noint2[1][0]);
        regression.addData(this.noint2[2][1], this.noint2[2][0]);
        Assert.assertEquals("intercept", 0, regression.getIntercept(), 0);
        Assert.assertEquals("slope", 0.727272727272727,
            regression.getSlope(), 10E-12);
        Assert.assertEquals("slope std err", 0.420827318078432E-01,
            regression.getSlopeStdErr(), 10E-12);
        Assert.assertEquals("number of observations", 3, regression.getN());
        Assert.assertEquals("r-square", 0.993348115299335,
            regression.getRSquare(), 10E-12);
        Assert.assertEquals("SSR", 40.7272727272727,
            regression.getRegressionSumSquares(), 10E-9);
        Assert.assertEquals("MSE", 0.136363636363636,
            regression.getMeanSquareError(), 10E-10);
        Assert.assertEquals("SSE", 0.272727272727273,
            regression.getSumSquaredErrors(), 10E-9);
    }

    @Test
    public void testNoIntercept_noint1() {
        final SimpleRegression regression = new SimpleRegression(false);
        for (final double[] element : this.noint1) {
            regression.addData(element[1], element[0]);
        }
        Assert.assertEquals("intercept", 0, regression.getIntercept(), 0);
        Assert.assertEquals("slope", 2.07438016528926, regression.getSlope(), 10E-12);
        Assert.assertEquals("slope std err", 0.165289256198347E-01,
            regression.getSlopeStdErr(), 10E-12);
        Assert.assertEquals("number of observations", 11, regression.getN());
        Assert.assertEquals("r-square", 0.999365492298663,
            regression.getRSquare(), 10E-12);
        Assert.assertEquals("SSR", 200457.727272727,
            regression.getRegressionSumSquares(), 10E-9);
        Assert.assertEquals("MSE", 12.7272727272727,
            regression.getMeanSquareError(), 10E-10);
        Assert.assertEquals("SSE", 127.272727272727,
            regression.getSumSquaredErrors(), 10E-9);

    }

    @Test
    public void testNorris() {
        final SimpleRegression regression = new SimpleRegression();
        for (final double[] element : this.data) {
            regression.addData(element[1], element[0]);
        }
        // Tests against certified values from
        // http://www.itl.nist.gov/div898/strd/lls/data/LINKS/DATA/Norris.dat
        Assert.assertEquals("slope", 1.00211681802045, regression.getSlope(), 10E-12);
        Assert.assertEquals("slope std err", 0.429796848199937E-03,
            regression.getSlopeStdErr(), 10E-12);
        Assert.assertEquals("number of observations", 36, regression.getN());
        Assert.assertEquals("intercept", -0.262323073774029,
            regression.getIntercept(), 10E-12);
        Assert.assertEquals("std err intercept", 0.232818234301152,
            regression.getInterceptStdErr(), 10E-12);
        Assert.assertEquals("r-square", 0.999993745883712,
            regression.getRSquare(), 10E-12);
        Assert.assertEquals("SSR", 4255954.13232369,
            regression.getRegressionSumSquares(), 10E-9);
        Assert.assertEquals("MSE", 0.782864662630069,
            regression.getMeanSquareError(), 10E-10);
        Assert.assertEquals("SSE", 26.6173985294224,
            regression.getSumSquaredErrors(), 10E-9);
        // ------------ End certified data tests

        Assert.assertEquals("predict(0)", -0.262323073774029,
            regression.predict(0), 10E-12);
        Assert.assertEquals("predict(1)", 1.00211681802045 - 0.262323073774029,
            regression.predict(1), 10E-12);
    }

    @Test
    public void testCorr() {
        final SimpleRegression regression = new SimpleRegression();
        regression.addData(this.corrData);
        Assert.assertEquals("number of observations", 17, regression.getN());
        Assert.assertEquals("r-square", .896123, regression.getRSquare(), 10E-6);
        Assert.assertEquals("r", -0.94663767742, regression.getR(), 1E-10);
    }

    @Test
    public void testNaNs() {
        SimpleRegression regression = new SimpleRegression();
        Assert.assertTrue("intercept not NaN", Double.isNaN(regression.getIntercept()));
        Assert.assertTrue("slope not NaN", Double.isNaN(regression.getSlope()));
        Assert.assertTrue("slope std err not NaN", Double.isNaN(regression.getSlopeStdErr()));
        Assert.assertTrue("intercept std err not NaN", Double.isNaN(regression.getInterceptStdErr()));
        Assert.assertTrue("MSE not NaN", Double.isNaN(regression.getMeanSquareError()));
        Assert.assertTrue("e not NaN", Double.isNaN(regression.getR()));
        Assert.assertTrue("r-square not NaN", Double.isNaN(regression.getRSquare()));
        Assert.assertTrue("RSS not NaN", Double.isNaN(regression.getRegressionSumSquares()));
        Assert.assertTrue("SSE not NaN", Double.isNaN(regression.getSumSquaredErrors()));
        Assert.assertTrue("SSTO not NaN", Double.isNaN(regression.getTotalSumSquares()));
        Assert.assertTrue("predict not NaN", Double.isNaN(regression.predict(0)));

        regression.addData(1, 2);
        regression.addData(1, 3);

        // No x variation, so these should still blow...
        Assert.assertTrue("intercept not NaN", Double.isNaN(regression.getIntercept()));
        Assert.assertTrue("slope not NaN", Double.isNaN(regression.getSlope()));
        Assert.assertTrue("slope std err not NaN", Double.isNaN(regression.getSlopeStdErr()));
        Assert.assertTrue("intercept std err not NaN", Double.isNaN(regression.getInterceptStdErr()));
        Assert.assertTrue("MSE not NaN", Double.isNaN(regression.getMeanSquareError()));
        Assert.assertTrue("e not NaN", Double.isNaN(regression.getR()));
        Assert.assertTrue("r-square not NaN", Double.isNaN(regression.getRSquare()));
        Assert.assertTrue("RSS not NaN", Double.isNaN(regression.getRegressionSumSquares()));
        Assert.assertTrue("SSE not NaN", Double.isNaN(regression.getSumSquaredErrors()));
        Assert.assertTrue("predict not NaN", Double.isNaN(regression.predict(0)));

        // but SSTO should be OK
        Assert.assertTrue("SSTO NaN", !Double.isNaN(regression.getTotalSumSquares()));

        regression = new SimpleRegression();

        regression.addData(1, 2);
        regression.addData(3, 3);

        // All should be OK except MSE, s(b0), s(b1) which need one more df
        Assert.assertTrue("interceptNaN", !Double.isNaN(regression.getIntercept()));
        Assert.assertTrue("slope NaN", !Double.isNaN(regression.getSlope()));
        Assert.assertTrue("slope std err not NaN", Double.isNaN(regression.getSlopeStdErr()));
        Assert.assertTrue("intercept std err not NaN", Double.isNaN(regression.getInterceptStdErr()));
        Assert.assertTrue("MSE not NaN", Double.isNaN(regression.getMeanSquareError()));
        Assert.assertTrue("r NaN", !Double.isNaN(regression.getR()));
        Assert.assertTrue("r-square NaN", !Double.isNaN(regression.getRSquare()));
        Assert.assertTrue("RSS NaN", !Double.isNaN(regression.getRegressionSumSquares()));
        Assert.assertTrue("SSE NaN", !Double.isNaN(regression.getSumSquaredErrors()));
        Assert.assertTrue("SSTO NaN", !Double.isNaN(regression.getTotalSumSquares()));
        Assert.assertTrue("predict NaN", !Double.isNaN(regression.predict(0)));

        regression.addData(1, 4);

        // MSE, MSE, s(b0), s(b1) should all be OK now
        Assert.assertTrue("MSE NaN", !Double.isNaN(regression.getMeanSquareError()));
        Assert.assertTrue("slope std err NaN", !Double.isNaN(regression.getSlopeStdErr()));
        Assert.assertTrue("intercept std err NaN", !Double.isNaN(regression.getInterceptStdErr()));
    }

    @Test
    public void testClear() {
        final SimpleRegression regression = new SimpleRegression();
        regression.addData(this.corrData);
        Assert.assertEquals("number of observations", 17, regression.getN());
        regression.clear();
        Assert.assertEquals("number of observations", 0, regression.getN());
        regression.addData(this.corrData);
        Assert.assertEquals("r-square", .896123, regression.getRSquare(), 10E-6);
        regression.addData(this.data);
        Assert.assertEquals("number of observations", 53, regression.getN());
    }

    @Test
    public void testInference() {
        // ---------- verified against R, version 1.8.1 -----
        // infData
        SimpleRegression regression = new SimpleRegression();
        regression.addData(this.infData);
        Assert.assertEquals("slope std err", 0.011448491,
            regression.getSlopeStdErr(), 1E-10);
        Assert.assertEquals("std err intercept", 0.286036932,
            regression.getInterceptStdErr(), 1E-8);
        Assert.assertEquals("significance", 4.596e-07,
            regression.getSignificance(), 1E-8);
        Assert.assertEquals("slope conf interval half-width", 0.0270713794287,
            regression.getSlopeConfidenceInterval(), 1E-8);
        // infData2
        regression = new SimpleRegression();
        regression.addData(this.infData2);
        Assert.assertEquals("slope std err", 1.07260253,
            regression.getSlopeStdErr(), 1E-8);
        Assert.assertEquals("std err intercept", 4.17718672,
            regression.getInterceptStdErr(), 1E-8);
        Assert.assertEquals("significance", 0.261829133982,
            regression.getSignificance(), 1E-11);
        Assert.assertEquals("slope conf interval half-width", 2.97802204827,
            regression.getSlopeConfidenceInterval(), 1E-8);
        // ------------- End R-verified tests -------------------------------

        Assert.assertTrue("tighter means wider",
            regression.getSlopeConfidenceInterval() < regression.getSlopeConfidenceInterval(0.01));

        try {
            regression.getSlopeConfidenceInterval(1);
            Assert.fail("expecting MathIllegalArgumentException for alpha = 1");
        } catch (final MathIllegalArgumentException ex) {
            // ignored
        }

    }

    @Test
    public void testPerfect() {
        final SimpleRegression regression = new SimpleRegression();
        final int n = 100;
        for (int i = 0; i < n; i++) {
            regression.addData(((double) i) / (n - 1), i);
        }
        Assert.assertEquals(0.0, regression.getSignificance(), 1.0e-5);
        Assert.assertTrue(regression.getSlope() > 0.0);
        Assert.assertTrue(regression.getSumSquaredErrors() >= 0.0);
    }

    @Test
    public void testPerfectNegative() {
        final SimpleRegression regression = new SimpleRegression();
        final int n = 100;
        for (int i = 0; i < n; i++) {
            regression.addData(-((double) i) / (n - 1), i);
        }

        Assert.assertEquals(0.0, regression.getSignificance(), 1.0e-5);
        Assert.assertTrue(regression.getSlope() < 0.0);
    }

    @Test
    public void testRandom() {
        final SimpleRegression regression = new SimpleRegression();
        final Random random = new Random(1);
        final int n = 100;
        for (int i = 0; i < n; i++) {
            regression.addData(((double) i) / (n - 1), random.nextDouble());
        }

        Assert.assertTrue(0.0 < regression.getSignificance()
            && regression.getSignificance() < 1.0);
    }

    // Jira MATH-85 = Bugzilla 39432
    @Test
    public void testSSENonNegative() {
        final double[] y = { 8915.102, 8919.302, 8923.502 };
        final double[] x = { 1.107178495E2, 1.107264895E2, 1.107351295E2 };
        final SimpleRegression reg = new SimpleRegression();
        for (int i = 0; i < x.length; i++) {
            reg.addData(x[i], y[i]);
        }
        Assert.assertTrue(reg.getSumSquaredErrors() >= 0.0);
    }

    // Test remove X,Y (single observation)
    @Test
    public void testRemoveXY() {
        // Create regression with inference data then remove to test
        final SimpleRegression regression = new SimpleRegression();
        regression.addData(this.infData);
        regression.removeData(this.removeX, this.removeY);
        regression.addData(this.removeX, this.removeY);
        // Use the inference assertions to make sure that everything worked
        Assert.assertEquals("slope std err", 0.011448491,
            regression.getSlopeStdErr(), 1E-10);
        Assert.assertEquals("std err intercept", 0.286036932,
            regression.getInterceptStdErr(), 1E-8);
        Assert.assertEquals("significance", 4.596e-07,
            regression.getSignificance(), 1E-8);
        Assert.assertEquals("slope conf interval half-width", 0.0270713794287,
            regression.getSlopeConfidenceInterval(), 1E-8);
    }

    // Test remove single observation in array
    @Test
    public void testRemoveSingle() {
        // Create regression with inference data then remove to test
        final SimpleRegression regression = new SimpleRegression();
        regression.addData(this.infData);
        regression.removeData(this.removeSingle);
        regression.addData(this.removeSingle);
        // Use the inference assertions to make sure that everything worked
        Assert.assertEquals("slope std err", 0.011448491,
            regression.getSlopeStdErr(), 1E-10);
        Assert.assertEquals("std err intercept", 0.286036932,
            regression.getInterceptStdErr(), 1E-8);
        Assert.assertEquals("significance", 4.596e-07,
            regression.getSignificance(), 1E-8);
        Assert.assertEquals("slope conf interval half-width", 0.0270713794287,
            regression.getSlopeConfidenceInterval(), 1E-8);
    }

    // Test remove multiple observations
    @Test
    public void testRemoveMultiple() {
        // Create regression with inference data then remove to test
        final SimpleRegression regression = new SimpleRegression();
        regression.addData(this.infData);
        regression.removeData(this.removeMultiple);
        regression.addData(this.removeMultiple);
        // Use the inference assertions to make sure that everything worked
        Assert.assertEquals("slope std err", 0.011448491,
            regression.getSlopeStdErr(), 1E-10);
        Assert.assertEquals("std err intercept", 0.286036932,
            regression.getInterceptStdErr(), 1E-8);
        Assert.assertEquals("significance", 4.596e-07,
            regression.getSignificance(), 1E-8);
        Assert.assertEquals("slope conf interval half-width", 0.0270713794287,
            regression.getSlopeConfidenceInterval(), 1E-8);
    }

    // Remove observation when empty
    @Test
    public void testRemoveObsFromEmpty() {
        final SimpleRegression regression = new SimpleRegression();
        regression.removeData(this.removeX, this.removeY);
        Assert.assertEquals(regression.getN(), 0);
    }

    // Remove single observation to empty
    @Test
    public void testRemoveObsFromSingle() {
        final SimpleRegression regression = new SimpleRegression();
        regression.addData(this.removeX, this.removeY);
        regression.removeData(this.removeX, this.removeY);
        Assert.assertEquals(regression.getN(), 0);
    }

    // Remove multiple observations to empty
    @Test
    public void testRemoveMultipleToEmpty() {
        final SimpleRegression regression = new SimpleRegression();
        regression.addData(this.removeMultiple);
        regression.removeData(this.removeMultiple);
        Assert.assertEquals(regression.getN(), 0);
    }

    // Remove multiple observations past empty (i.e. size of array > n)
    @Test
    public void testRemoveMultiplePastEmpty() {
        final SimpleRegression regression = new SimpleRegression();
        regression.addData(this.removeX, this.removeY);
        regression.removeData(this.removeMultiple);
        Assert.assertEquals(regression.getN(), 0);
    }
}
