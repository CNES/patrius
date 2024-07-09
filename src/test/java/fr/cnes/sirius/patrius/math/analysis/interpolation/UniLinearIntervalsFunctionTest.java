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
 * @history 01/10/2014:creation
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:269:01/10/2014:piecewise linear interpolations
 * VERSION::FA:386:19/12/2014:index mutualisation for ephemeris interpolation
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.interpolation;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.math.utils.BinarySearchIndexOpenClosed;

/**
 * 
 * Tests the class UniLinearIntervalsInterpolator based on for SearchIndexInDoubleTab
 * created from BinarySearch algorithm.
 * 
 * @author Sophie LAURENS
 * @version $Id: UniLinearIntervalsFunctionTest.java 17909 2017-09-11 11:57:36Z bignon $
 * @since 2.3
 */
public class UniLinearIntervalsFunctionTest {

    /** Numerical precision. */
    protected static final double EPSILON = Precision.DOUBLE_COMPARISON_EPSILON;

    /** Features description. */
    public enum features {
        /**
         * @featureTitle tests method value(double)
         * @featureDescription tests linear interpolation in dimension 1
         */
        INTERPOLATION,
        /**
         * @featureTitle tests method to ensure proper coverage
         * @featureDescription
         */
        COVERAGE,
    }

    /**
     * @testType UT
     * @testedFeature {@link features#INTERPOLATION}
     * @testedMethod {@link UniLinearIntervalsFunction#value(double)}
     * @description Tests the value method for the test case given in the MU of MSPro
     * @input xval = {-0.34, -15, -9.01, -9.0, 0.5, 0.6, 25, 54, 98, 105}
     * @testPassCriteria obtaining fxref = 25 for x=10
     * @since 2.3
     */
    @Test
    public void nominalCaseMSPRO() {
        final int dimx = 10;
        final double[] xval = { -0.34, -15, -9.01, -9.0, 0.5, 0.6, 25, 54, 98, 105 };
        final double x = 10;

        final double fxref = 25;

        final double[] fval = new double[dimx];
        for (int i = 0; i < dimx; i++) {
            fval[i] = 5 + 2 * xval[i];
        }

        final UniLinearIntervalsFunction interp = new UniLinearIntervalsFunction(xval, fval);
        final double fx = interp.value(x);
        Assert.assertEquals(fxref, fx, EPSILON);
    }

    /**
     * @testType UT
     * @testedFeature {@link features#INTERPOLATION}
     * @testedMethod {@link UniLinearIntervalsFunction#value(double)}
     * @description Computes the Cx and Cz coefficients with incidence laws.
     * @input given by CNES in DM 269
     * @testPassCriteria given by CNES in DM 269
     * @since 2.3
     */
    @Test
    public void nominalCase() {

        // incidence laws, depending on the mach number
        final double[] machTab = { 0., 2., 10., 20., 25., 50. };
        final double[] aoaTab = { 10., 10., 33., 33., 40., 40. };

        // Cx and Cz depending only on the incidence
        final double[] aoaTab2 = { 10., 20., 30., 40., 50. };
        final double[] cxTab = { 0.10, 0.20, 0.30, 0.40, 0.50 };
        final double[] czTab = { 0.05, 0.15, 0.30, 0.30, 0.25 };

        // linear interpolators in dimension 1
        final UniLinearIntervalsFunction aoaInterpolator = new UniLinearIntervalsFunction(machTab, aoaTab);
        final UniLinearIntervalsFunction cxInterpolator = new UniLinearIntervalsFunction(aoaTab2, cxTab);
        final UniLinearIntervalsFunction czInterpolator = new UniLinearIntervalsFunction(aoaTab2, czTab);

        double mach = 1.;
        final double dmach = 1.;

        final ArrayList<Double> xres = new ArrayList<Double>();
        final ArrayList<Double> fres = new ArrayList<Double>();

        double alpha;

        do {
            alpha = aoaInterpolator.value(mach);
            final double cx = cxInterpolator.value(alpha);
            final double cz = czInterpolator.value(alpha);
            xres.add(alpha);
            fres.add(cz / cx);
            mach = mach + dmach;
        } while (mach <= machTab[machTab.length - 1] && alpha <= aoaTab2[aoaTab2.length - 1]);
    }

    /**
     * @testType UT
     * @testedFeature {@link features#INTERPOLATION}
     * @testedMethod {@link UniLinearIntervalsFunction#UniLinearIntervalsFunction(double[], double[])}
     * @testedMethod {@link UniLinearIntervalsFunction#value(double)}
     * @description Tests the 1D linear interpolation with f(x) = 10 x with no duplicate
     * @input sorted entries
     * @testPassCriteria
     * @since 2.3.1
     */
    @Test
    public void testInterpolationNoDuplicate() {

        final double[] xtab = { 1, 2, 3, 4 };
        // f(x) = 10 x
        final double[] ftab = { 10, 20, 30, 40 };
        // values to interpolate with some of them outside xtab
        final double[] xtabInterp = { 0.5, 1.5, 2.5, 3.5, 4.5 };

        final UniLinearIntervalsFunction interp = new UniLinearIntervalsInterpolator().interpolate(xtab, ftab);

        for (final double element : xtabInterp) {
            Assert.assertEquals(10 * element, interp.value(element), EPSILON);
        }

    }

    /**
     * @testType UT
     * @testedFeature {@link features#INTERPOLATION}
     * @testedMethod {@link UniLinearIntervalsFunction#UniLinearIntervalsFunction(double[], double[])}
     * @testedMethod {@link UniLinearIntervalsFunction#value(double)}
     * @description Tests interpolation and extrapolation with f(x) = 2 x + 14.7
     * @input sorted entries with duplicates
     * @testPassCriteria f(xinterp) = finterp
     * @since 2.3.1
     */
    @Test
    public void testInterpolationDuplicates() {

        final double[] xtab = { 0, 1, 1, 2, 3 };
        final double[] xinterp = { -1, 0.5, 1, 1.1, 4 };
        final int dimx = xtab.length;
        final double[] ftab = new double[dimx];
        final double[] finterp = new double[dimx];
        for (int i = 0; i < dimx; i++) {
            ftab[i] = 2 * xtab[i] + 14.7;
            finterp[i] = 2 * xinterp[i] + 14.7;
        }

        final UniLinearIntervalsFunction interp = new UniLinearIntervalsFunction(xtab, ftab);

        for (int i = 0; i < dimx; i++) {
            final double fx = interp.value(xinterp[i]);
            Assert.assertEquals(fx, finterp[i], EPSILON);
        }
    }

    /**
     * @testType UT
     * @testedFeature {@link features#CONSTRUCTOR}
     * @testedFeature {@link features#MSPRO}
     * @testedMethod {@link UniLinearIntervalsFunction#UniLinearIntervalsFunction(double[], double[])}
     * @testedMethod {@link UniLinearIntervalsFunction#UniLinearIntervalsFunction(double[], double[], boolean)}
     * @testedMethod {@link UniLinearIntervalsFunction#value(double)}
     * @description Tests the 1D linear interpolation
     * @input sorted entries with duplicates
     * @testPassCriteria a correct interpolation
     * @since 2.3.1
     */
    @Test
    public void testDuplicateLinearInterpolationWithoutDiscontinuities() {

        final double[] xval = { -0.1, 0, 0, 1, 2, 3, 3.3, 3.3, 3.3, 4, 5 };
        final int dimx = xval.length;

        final double[] xInterp = { -0.05, 0.5, 1.5, 2.5, 3.1, 3.3, 3.7, 4, 4.895555 };
        final int dimxInterp = xInterp.length;

        final double[] fval = new double[dimx];
        final double[] fvalInterp = new double[dimxInterp];
        // fill fval and fvalInterp (result) with linear function f(x) = 1.47 + 23.2 * x
        for (int i = 0; i < dimx; i++) {
            fval[i] = 1.47 + 23.2 * xval[i];
        }
        for (int i = 0; i < dimxInterp; i++) {
            fvalInterp[i] = 1.47 + 23.2 * xInterp[i];
        }

        final UniLinearIntervalsFunction interp = new UniLinearIntervalsFunction(xval, fval);

        final double[] fx = new double[dimxInterp];

        for (int i = 0; i < dimxInterp; i++) {
            fx[i] = interp.value(xInterp[i]);

            Assert.assertEquals(fx[i], fvalInterp[i], 1e-13);
        }
    }

    /**
     * @testType UT
     * @testedFeature {@link features#CONSTRUCTOR}
     * @testedMethod {@link UniLinearIntervalsFunction#UniLinearIntervalsFunction(double[], double[])}
     * @testedMethod {@link UniLinearIntervalsFunction#value(double)}
     * @description Tests the exception that occurs if xtab and ftab have different lengths
     * @testPassCriteria DimensionMismatchException
     * @since 2.3
     */
    @Test(expected = DimensionMismatchException.class)
    public void wrongXYLength() {

        final double[] ftab = { 2, 2, 4, 7, 1 };
        final double[] xtab = { 1, 2, 7 };
        new UniLinearIntervalsFunction(xtab, ftab);
    }

    /**
     * @testType UT
     * @testedFeature {@link features#CONSTRUCTOR}
     * @testedMethod {@link UniLinearIntervalsFunction#UniLinearIntervalsFunction(double[], double[])}
     * @testedMethod {@link UniLinearIntervalsFunction#value(double)}
     * @description Tests the exception that occurs if xtab and ftab have same length equals to 1
     * @testPassCriteria NumberIsTooSmallException
     * @since 2.3
     */
    @Test(expected = NumberIsTooSmallException.class)
    public void exceptionLengthInfTo2() {

        final double[] ftab = { 2 };
        final double[] xtab = { 1 };
        new UniLinearIntervalsFunction(xtab, ftab);
    }

    /**
     * @testType UT
     * @testedFeature {@link features#COVERAGE}
     * @testedMethod {@link UniLinearIntervalsFunction#getValues(double)}
     * @description
     * @testPassCriteria
     * @since 2.3.1
     */
    @Test
    public void testGetValues() {

        final double[] xtab = { 1, 2, 3, 4 };
        // f(x) = x
        // values to interpolate with some of them outside xtab
        final double[] xtabInterp = { 0.5, 1.5, 2.5, 3.5, 4.5 };

        final UniLinearIntervalsFunction interp = new UniLinearIntervalsFunction(xtab, xtab);

        // tests method getValues
        final double[] fobtained = interp.getValues();
        for (int i = 0; i < fobtained.length; i++) {
            fobtained[i] = xtab[i];
        }

        // tests interpolation
        for (final double element : xtabInterp) {
            Assert.assertEquals(element, interp.value(element), EPSILON);
        }

        // tests interpolation with another search index algorithm with a different convention
        final BinarySearchIndexOpenClosed algo = new BinarySearchIndexOpenClosed(xtab);
        final UniLinearIntervalsFunction interpOpenClosed = new UniLinearIntervalsFunction(algo, xtab);
        for (final double element : xtabInterp) {
            Assert.assertEquals(element, interpOpenClosed.value(element), EPSILON);
        }

    }

}
