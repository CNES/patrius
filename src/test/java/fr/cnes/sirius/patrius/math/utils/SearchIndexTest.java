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
 * @history Created 19/12/2014
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:386:05/12/2014: (creation) index mutualisation for ephemeris interpolation
 * VERSION::FA:417:12/02/2015:AbstractLinearIntervalsFunction modifications
 * VERSION::FA:664:29/07/2016:corrected method value() leadind to NaN value in some cases
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.utils;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.analysis.interpolation.BiLinearIntervalsFunction;
import fr.cnes.sirius.patrius.math.analysis.interpolation.TriLinearIntervalsFunction;
import fr.cnes.sirius.patrius.math.analysis.interpolation.TriLinearIntervalsInterpolator;
import fr.cnes.sirius.patrius.math.analysis.interpolation.UniLinearIntervalsFunction;
import fr.cnes.sirius.patrius.math.analysis.interpolation.UniLinearIntervalsInterpolator;
import fr.cnes.sirius.patrius.math.utils.ISearchIndex.SearchIndexIntervalConvention;

/**
 * Tests the search index classes.
 * 
 * @version $Id: SearchIndexTest.java 17909 2017-09-11 11:57:36Z bignon $
 * @since 2.3.1
 * 
 */
public class SearchIndexTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Tests the search indexes functionnalities
         * @featureDescription
         */
        SEARCH_INDEX
    }

    /**
     * @testType UT
     * @testedFeature {@link features#SEARCH_INDEX}
     * @testedMethod {@link UniLinearIntervalsFunction#value(double)}
     *               {@link BiLinearIntervalsFunction#value(double, double)}
     *               {@link TriLinearIntervalsFunction#value(double, double, double)}
     * 
     * @description Test the corrections made to solve the problems noticed in FA 664 :
     *              the linear interpolation with double[] containing duplicates led to NaN value
     *              because of a division by zero in the interpolation process. This is not the case
     *              anymore since conditions have been added in the method value() to avoid the
     *              interpolation in such case and make direct return when possible.
     * 
     * @input Well sorted double[] with duplicates.
     * 
     * @testPassCriteria The values in output of value() must be the one expected.
     * @since 3.3
     */
    @Test
    public void indexInterpolationTest() {

        /** Test 1D. */
        final double[] x = { 1, 1, 1, 2 };
        final double[] x1dCO = { 0, 1, 1, 1 };
        final double[] y = { 0, 1, 2, 6 };
        final UniLinearIntervalsInterpolator interp = new UniLinearIntervalsInterpolator();
        final BinarySearchIndexOpenClosed algo = new BinarySearchIndexOpenClosed(x);
        final UniLinearIntervalsFunction function = new UniLinearIntervalsFunction(algo, y);
        final double valAlgoOC = function.value(1.);
        final double valxCO = interp.interpolate(x1dCO, y).value(1.);

        // Value must be the one expected and different from NaN
        Assert.assertEquals(valAlgoOC, y[0], 0.);
        Assert.assertEquals(valxCO, y[3], 0.);

        // Coverage case : Value in 1 / 2 ? must be y[0] as xindex returned will be 0 in x
        // We are in the case where xtab[xindex] = xtab[xindex + 1] so a quick return is made to avoid interpolation
        final double valX0 = function.value(0.5);
        Assert.assertEquals(valX0, y[0], 0.);

        /** Test 2D. */

        // Input double[] : duplicates could be either for xtab and ytab at the lower/upper bound of the tab
        final double[] x2Norm = { 1, 2, 3 };
        final double[] y2dNorm = { 1, 2, 3 };
        final double[] x2dCO = { 0, 1, 1 };
        final double[] x2dOC = { 1, 1, 2 };
        final double[] y2dCO = { 0, 1, 1 };
        final double[] y2dOC = { 1, 1, 2 };

        final double[][] fval = { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };
        final double pointX = 1;
        final double pointY = 1;

        // Test with xCo, yNorm : x has duplicates at upper bound, y is normal (ie. has no duplicates)
        final BinarySearchIndexClosedOpen algoXCOCO = new BinarySearchIndexClosedOpen(x2dCO);
        final BinarySearchIndexOpenClosed algoXCOOC = new BinarySearchIndexOpenClosed(x2dCO);
        final BinarySearchIndexClosedOpen algoYNormCO = new BinarySearchIndexClosedOpen(y2dNorm);
        final BinarySearchIndexOpenClosed algoYNormOC = new BinarySearchIndexOpenClosed(y2dNorm);

        final double val1 = this.getInterpValueFunction2d(algoXCOCO, algoYNormCO, fval, pointX, pointY);
        final double val2 = this.getInterpValueFunction2d(algoXCOOC, algoYNormOC, fval, pointX, pointY);
        final double val3 = this.getInterpValueFunction2d(algoXCOCO, algoYNormOC, fval, pointX, pointY);
        final double val4 = this.getInterpValueFunction2d(algoXCOOC, algoYNormCO, fval, pointX, pointY);
        Assert.assertEquals(val1, fval[2][0], 0.);
        Assert.assertEquals(val2, fval[1][0], 0.);
        Assert.assertEquals(val3, fval[2][0], 0.);
        Assert.assertEquals(val4, fval[1][0], 0.);

        // Test xOC, yNOrm
        final BinarySearchIndexClosedOpen algoXOCCO = new BinarySearchIndexClosedOpen(x2dOC);
        final BinarySearchIndexOpenClosed algoXOCOC = new BinarySearchIndexOpenClosed(x2dOC);

        final double val5 = this.getInterpValueFunction2d(algoXOCCO, algoYNormCO, fval, pointX, pointY);
        final double val6 = this.getInterpValueFunction2d(algoXOCOC, algoYNormOC, fval, pointX, pointY);
        final double val7 = this.getInterpValueFunction2d(algoXOCCO, algoYNormOC, fval, pointX, pointY);
        final double val8 = this.getInterpValueFunction2d(algoXOCOC, algoYNormCO, fval, pointX, pointY);
        Assert.assertEquals(val5, fval[1][0], 0.);
        Assert.assertEquals(val6, fval[0][0], 0.);
        Assert.assertEquals(val7, fval[1][0], 0.);
        Assert.assertEquals(val8, fval[0][0], 0.);

        // Test xNorm, yOC
        final BinarySearchIndexClosedOpen algoXNormCO = new BinarySearchIndexClosedOpen(x2Norm);
        final BinarySearchIndexOpenClosed algoXNormOC = new BinarySearchIndexOpenClosed(x2Norm);
        final BinarySearchIndexClosedOpen algoYOCCO = new BinarySearchIndexClosedOpen(y2dOC);
        final BinarySearchIndexOpenClosed algoYOCOC = new BinarySearchIndexOpenClosed(y2dOC);

        final double val9 = this.getInterpValueFunction2d(algoXNormCO, algoYOCCO, fval, pointX, pointY);
        final double val10 = this.getInterpValueFunction2d(algoXNormOC, algoYOCOC, fval, pointX, pointY);
        final double val11 = this.getInterpValueFunction2d(algoXNormCO, algoYOCOC, fval, pointX, pointY);
        final double val12 = this.getInterpValueFunction2d(algoXNormOC, algoYOCCO, fval, pointX, pointY);
        Assert.assertEquals(val9, fval[0][1], 0.);
        Assert.assertEquals(val10, fval[0][0], 0.);
        Assert.assertEquals(val11, fval[0][0], 0.);
        Assert.assertEquals(val12, fval[0][1], 0.);

        // Test xNorm, yCO
        final BinarySearchIndexClosedOpen algoYCOCO = new BinarySearchIndexClosedOpen(y2dCO);
        final BinarySearchIndexOpenClosed algoYCOOC = new BinarySearchIndexOpenClosed(y2dCO);

        final double val13 = this.getInterpValueFunction2d(algoXNormCO, algoYCOCO, fval, pointX, pointY);
        final double val14 = this.getInterpValueFunction2d(algoXNormOC, algoYCOOC, fval, pointX, pointY);
        final double val15 = this.getInterpValueFunction2d(algoXNormCO, algoYCOOC, fval, pointX, pointY);
        final double val16 = this.getInterpValueFunction2d(algoXNormOC, algoYCOCO, fval, pointX, pointY);
        Assert.assertEquals(val13, fval[0][2], 0.);
        Assert.assertEquals(val14, fval[0][1], 0.);
        Assert.assertEquals(val15, fval[0][1], 0.);
        Assert.assertEquals(val16, fval[0][2], 0.);

        // Test xOC, yOC
        final double val17 = this.getInterpValueFunction2d(algoXOCCO, algoYOCCO, fval, pointX, pointY);
        final double val18 = this.getInterpValueFunction2d(algoXOCOC, algoYOCOC, fval, pointX, pointY);
        final double val19 = this.getInterpValueFunction2d(algoXOCCO, algoYOCOC, fval, pointX, pointY);
        final double val20 = this.getInterpValueFunction2d(algoXOCOC, algoYOCCO, fval, pointX, pointY);
        Assert.assertEquals(val17, fval[1][1], 0.);
        Assert.assertEquals(val18, fval[0][0], 0.);
        Assert.assertEquals(val19, fval[1][0], 0.);
        Assert.assertEquals(val20, fval[0][1], 0.);

        // Test xOC, yCO
        final double val21 = this.getInterpValueFunction2d(algoXOCCO, algoYCOCO, fval, pointX, pointY);
        final double val22 = this.getInterpValueFunction2d(algoXOCOC, algoYCOOC, fval, pointX, pointY);
        final double val23 = this.getInterpValueFunction2d(algoXOCCO, algoYCOOC, fval, pointX, pointY);
        final double val24 = this.getInterpValueFunction2d(algoXOCOC, algoYCOCO, fval, pointX, pointY);
        Assert.assertEquals(val21, fval[1][2], 0.);
        Assert.assertEquals(val22, fval[0][1], 0.);
        Assert.assertEquals(val23, fval[1][1], 0.);
        Assert.assertEquals(val24, fval[0][2], 0.);

        // Test xCO, yOC
        final double val25 = this.getInterpValueFunction2d(algoXCOCO, algoYOCCO, fval, pointX, pointY);
        final double val26 = this.getInterpValueFunction2d(algoXCOOC, algoYOCOC, fval, pointX, pointY);
        final double val27 = this.getInterpValueFunction2d(algoXCOCO, algoYOCOC, fval, pointX, pointY);
        final double val28 = this.getInterpValueFunction2d(algoXCOOC, algoYOCCO, fval, pointX, pointY);
        Assert.assertEquals(val25, fval[2][1], 0.);
        Assert.assertEquals(val26, fval[1][0], 0.);
        Assert.assertEquals(val27, fval[2][0], 0.);
        Assert.assertEquals(val28, fval[1][1], 0.);

        // Test xCO, yCO
        final double val29 = this.getInterpValueFunction2d(algoXCOCO, algoYCOCO, fval, pointX, pointY);
        final double val30 = this.getInterpValueFunction2d(algoXCOOC, algoYCOOC, fval, pointX, pointY);
        final double val31 = this.getInterpValueFunction2d(algoXCOCO, algoYCOOC, fval, pointX, pointY);
        final double val32 = this.getInterpValueFunction2d(algoXCOOC, algoYCOCO, fval, pointX, pointY);
        Assert.assertEquals(val29, fval[2][2], 0.);
        Assert.assertEquals(val30, fval[1][1], 0.);
        Assert.assertEquals(val31, fval[1][1], 0.);
        Assert.assertEquals(val32, fval[1][1], 0.);

        /** Test 3D. */

        // Data : abscissae, ordonnae, width
        final double[] x3d = { 0, 1, 1 };
        final double[] y3d = { 1, 2, 2 };
        final double[] z3d = { 3, 4, 4 };

        // Moving data
        final double[] x3dMov = { 1, 1, 2 };
        final double[] y3dMov = { 2, 2, 3 };
        final double[] z3dMov = { 1, 2, 4 };

        // 3D Function
        final double[][][] fval3d = new double[3][3][3];
        final double pointX3d = 1;
        final double pointY3d = 2;
        final double pointZ3d = 4;

        fval3d[0][0][0] = 1;
        fval3d[0][0][1] = 2;
        fval3d[0][0][2] = 3;
        fval3d[0][1][0] = 4;
        fval3d[0][1][1] = 5;
        fval3d[0][1][2] = 6;
        fval3d[0][2][0] = 7;
        fval3d[0][2][1] = 8;
        fval3d[0][2][2] = 9;
        fval3d[1][0][0] = 10;
        fval3d[1][0][1] = 11;
        fval3d[1][0][2] = 12;
        fval3d[1][1][0] = 13;
        fval3d[1][1][1] = 14;
        fval3d[1][1][2] = 15;
        fval3d[1][2][0] = 16;
        fval3d[1][2][1] = 17;
        fval3d[1][2][2] = 18;
        fval3d[2][0][0] = 19;
        fval3d[2][0][1] = 20;
        fval3d[2][0][2] = 21;
        fval3d[2][1][0] = 22;
        fval3d[2][1][1] = 23;
        fval3d[2][1][2] = 24;
        fval3d[2][2][0] = 25;
        fval3d[2][2][1] = 26;
        fval3d[2][2][2] = 27;

        // Simple assert : the function is wanted at (x, y, z) with x, y, z being in the input tables
        final TriLinearIntervalsInterpolator interp3d = new TriLinearIntervalsInterpolator();
        final double val3d = interp3d.interpolate(x3d, y3d, z3d, fval3d).value(pointX3d, pointY3d, pointZ3d);
        Assert.assertEquals(val3d, fval3d[2][2][2], 0.);

        // Coverage cases for the correction in TriLinearIntervalsFunction :
        // use convention OpenClosed != ClosedOpen used in default constructor
        final BinarySearchIndexOpenClosed algo3dX = new BinarySearchIndexOpenClosed(x3dMov);
        final BinarySearchIndexOpenClosed algo3dY = new BinarySearchIndexOpenClosed(y3dMov);
        final BinarySearchIndexOpenClosed algo3dZ = new BinarySearchIndexOpenClosed(z3dMov);
        final double valCo1 =
            this.getInterpValueFunction3d(algo3dX, algo3dY, algo3dZ, fval3d, pointX3d, pointY3d, pointZ3d);
        Assert.assertEquals(valCo1, fval3d[0][0][2], 0.);

        // Change y = {2, 3, 3}
        y3dMov[1] = 3;
        final double valCo2 =
            this.getInterpValueFunction3d(algo3dX, algo3dY, algo3dZ, fval3d, pointX3d, pointY3d, pointZ3d);
        Assert.assertEquals(valCo2, fval3d[0][0][2], 0.);

        // Changes on y = {1, 3, 3} and z = {1, 1, 4}
        y3dMov[0] = 1;
        z3dMov[1] = 1;
        final double valCo3 =
            this.getInterpValueFunction3d(algo3dX, algo3dY, algo3dZ, fval3d, pointX3d, pointX3d, pointX3d);
        Assert.assertEquals(valCo3, fval3d[0][0][0], 0.);

        // Changes on x = {0, 1, 2}
        x3dMov[0] = 0;
        final double valCo4 =
            this.getInterpValueFunction3d(algo3dX, algo3dY, algo3dZ, fval3d, pointX3d, pointX3d, pointX3d);
        Assert.assertEquals(valCo4, fval3d[1][0][0], 0.);

        // Changes on y = {2, 2, 3}
        y3dMov[0] = 2;
        y3dMov[1] = 2;
        final double valCo5 =
            this.getInterpValueFunction3d(algo3dX, algo3dY, algo3dZ, fval3d, pointX3d, pointY3d, pointZ3d);
        Assert.assertEquals(valCo5, fval3d[1][0][2], 0.);

        final double valCo6 =
            this.getInterpValueFunction3d(algo3dX, algo3dY, algo3dZ, fval3d, pointX3d, pointY3d, pointX3d);
        Assert.assertEquals(valCo6, fval3d[1][0][0], 0.);

        // Coverage case : x3dMov = {1, 1, 2}, y3dMov = {2, 3, 3} and z3dMov = {1, 1, 4}
        // search f value at (1, 2, 1 / 2) point. It should cover the case where x[xindex] = x[xindex + 1]
        // and z[xindex] = z[xindex + 1] so the interpolation is only performed on y
        final double[] x3dTest = { 1, 1, 2 };
        final double[] y3dTest = { 2, 3, 3 };
        final double[] z3dTest = { 1, 1, 4 };

        final BinarySearchIndexOpenClosed algoXTest = new BinarySearchIndexOpenClosed(x3dTest);
        final BinarySearchIndexOpenClosed algoYTest = new BinarySearchIndexOpenClosed(y3dTest);
        final BinarySearchIndexOpenClosed algoZTest = new BinarySearchIndexOpenClosed(z3dTest);
        final double valTest =
            this.getInterpValueFunction3d(algoXTest, algoYTest, algoZTest, fval3d, pointX3d, pointY3d,
                0.5);

        // Expected value computed by hand
        final double expectedValue = 1.0;
        Assert.assertEquals(valTest, expectedValue, 0.);
    }

    /**
     * 
     * Utilitary method to return the value of the linear 2D function;
     * 
     * @param algoX
     *        the convention for the search algorithm on xtab
     * @param algoY
     *        the convention for the search algorithm on ytab
     * @param fval
     *        the function
     * @param evaluatedPointX
     *        the abscissa point where the value of the function
     *        is needed
     * @param evaluatedPointY
     *        the ordonnae point where the value of the function
     *        is needed
     * 
     * @return The value of the function at the point
     * @since 3.3
     */
    private double getInterpValueFunction2d(final ISearchIndex algoX, final ISearchIndex algoY,
                                            final double[][] fval, final double evaluatedPointX,
                                            final double evaluatedPointY) {

        final BiLinearIntervalsFunction function2d = new BiLinearIntervalsFunction(algoX, algoY, fval);
        final double val = function2d.value(evaluatedPointX, evaluatedPointY);

        return val;
    }

    /**
     * 
     * Utilitary method to return the value of the linear 3D function;
     * 
     * @param algoX
     *        the convention for the search algorithm on xtab
     * @param algoY
     *        the convention for the search algorithm on ytab
     * @param fval
     *        the function
     * @param evaluatedPointX
     *        the abscissa point where the value of the function
     *        is needed
     * @param evaluatedPointY
     *        the ordonnae point where the value of the function
     *        is needed
     * 
     * @return The value of the function at the point
     * @since 3.3
     */
    private double getInterpValueFunction3d(final ISearchIndex algoX, final ISearchIndex algoY,
                                            final ISearchIndex algoZ, final double[][][] fval,
                                            final double evaluatedPointX,
                                            final double evaluatedPointY,
                                            final double evaluatedPointZ) {

        final TriLinearIntervalsFunction function3d = new TriLinearIntervalsFunction(algoX, algoY, algoZ, fval);
        final double val = function3d.value(evaluatedPointX, evaluatedPointY, evaluatedPointZ);

        return val;
    }

    /**
     * @testType UT
     * @testedFeature {@link features#SEARCH_INDEX}
     * @testedMethod {@link BinarySearchIndexClosedOpen#BinarySearchIndexClosedOpen(double[])}
     * @testedMethod {@link BinarySearchIndexOpenClosed#BinarySearchIndexOpenClosed(double[])}
     * @testedMethod {@link RecordSegmentSearchIndex#RecordSegmentSearchIndex(ISearchIndex)}
     * @testedMethod {@link BinarySearchIndexClosedOpen#getIndex(double)}
     * @testedMethod {@link BinarySearchIndexOpenClosed#getIndex(double)}
     * @testedMethod {@link RecordSegmentSearchIndex#getIndex(double)}
     * @testedMethod {@link BinarySearchIndexClosedOpen#getConvention()}
     * @testedMethod {@link BinarySearchIndexOpenClosed#getConvention()}
     * @testedMethod {@link RecordSegmentSearchIndex#getConvention()}
     * @testedMethod {@link BinarySearchIndexClosedOpen#getTab()}
     * @testedMethod {@link BinarySearchIndexOpenClosed#getTab()}
     * @testedMethod {@link RecordSegmentSearchIndex#getTab()}
     * @description Tests the getIndex method.
     * @input A well sorted double[] with duplicates.
     * @testPassCriteria finding the correct indices
     * @since 2.3.1
     */
    @Test
    public void testGetIndex() {

        final double[] tab = { 1, 2, 4, 4, 6, 7, 8 };
        // x = [tab, middle values, outside boundaries]
        final double[] x = { 1, 2, 4, 4, 6, 7, 8, 1.5, 2.5, 5, 6.5, 7.5, 0, 9 };
        final int[] expectedCO = { 0, 1, 3, 3, 4, 5, 6, 0, 1, 3, 4, 5, -1, 6 };
        final int[] expectedOC = { -1, 0, 1, 1, 3, 4, 5, 0, 1, 3, 4, 5, -1, 6 };

        final BinarySearchIndexClosedOpen algoCO = new BinarySearchIndexClosedOpen(tab);
        final BinarySearchIndexOpenClosed algoOC = new BinarySearchIndexOpenClosed(tab);
        final RecordSegmentSearchIndex algoRCO = new RecordSegmentSearchIndex(algoCO);
        final RecordSegmentSearchIndex algoROC = new RecordSegmentSearchIndex(algoOC);
        final RecordSegmentSearchIndex algoRCO2 = new RecordSegmentSearchIndex(algoRCO);

        for (int i = 0; i < x.length; i++) {
            final int indexCO = algoCO.getIndex(x[i]);
            final int indexOC = algoOC.getIndex(x[i]);
            final int indexRCO = algoRCO.getIndex(x[i]);
            final int indexROC = algoROC.getIndex(x[i]);
            final int indexRCO2 = algoRCO2.getIndex(x[i]);
            Assert.assertEquals(indexCO, expectedCO[i]);
            Assert.assertEquals(indexCO, indexRCO);
            Assert.assertEquals(indexOC, expectedOC[i]);
            Assert.assertEquals(indexOC, indexROC);
            Assert.assertEquals(indexCO, indexRCO2);
        }

        for (int i = x.length - 1; i >= 0; i--) {
            final int indexCO = algoCO.getIndex(x[i]);
            final int indexOC = algoOC.getIndex(x[i]);
            final int indexRCO = algoRCO.getIndex(x[i]);
            final int indexROC = algoROC.getIndex(x[i]);
            final int indexRCO2 = algoRCO2.getIndex(x[i]);
            Assert.assertEquals(indexCO, expectedCO[i]);
            Assert.assertEquals(indexCO, indexRCO);
            Assert.assertEquals(indexOC, expectedOC[i]);
            Assert.assertEquals(indexOC, indexROC);
            Assert.assertEquals(indexCO, indexRCO2);
        }

        // Test getConvention
        Assert.assertEquals(SearchIndexIntervalConvention.CLOSED_OPEN, algoCO.getConvention());
        Assert.assertEquals(SearchIndexIntervalConvention.OPEN_CLOSED, algoOC.getConvention());
        Assert.assertEquals(SearchIndexIntervalConvention.CLOSED_OPEN, algoRCO.getConvention());
        Assert.assertEquals(SearchIndexIntervalConvention.OPEN_CLOSED, algoROC.getConvention());
    }

    /**
     * @testType UT
     * @testedFeature {@link features#SEARCH_INDEX}
     * @testedMethod {@link RecordSegmentSearchIndex#getIndex(double)}
     * @description Tests the getIndex method with tab containing less that 2 elements.
     * @input A double[] with 1 element.
     * @testPassCriteria finding the correct indices
     * @since 2.3.1
     */
    @Test
    public void testGetIndex1Point() {

        final double[] tab = { 1 };
        // x = [tab, middle values, outside boundaries]
        final double[] x = { 0, 0.5, 1., 1.5 };
        final int[] expectedCO = { -1, -1, 0, 0 };
        final int[] expectedOC = { -1, -1, -1, 0 };

        final BinarySearchIndexClosedOpen algoCO = new BinarySearchIndexClosedOpen(tab);
        final BinarySearchIndexOpenClosed algoOC = new BinarySearchIndexOpenClosed(tab);
        final RecordSegmentSearchIndex algoRCO = new RecordSegmentSearchIndex(algoCO);
        final RecordSegmentSearchIndex algoROC = new RecordSegmentSearchIndex(algoOC);

        for (int i = 0; i < x.length; i++) {
            final int indexCO = algoCO.getIndex(x[i]);
            final int indexOC = algoOC.getIndex(x[i]);
            final int indexRCO = algoRCO.getIndex(x[i]);
            final int indexROC = algoROC.getIndex(x[i]);
            Assert.assertEquals(indexCO, expectedCO[i]);
            Assert.assertEquals(indexCO, indexRCO);
            Assert.assertEquals(indexOC, expectedOC[i]);
            Assert.assertEquals(indexOC, indexROC);
        }
    }

}
