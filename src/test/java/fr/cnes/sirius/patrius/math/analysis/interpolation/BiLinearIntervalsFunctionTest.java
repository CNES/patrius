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
 * VERSION::FA:417:12/02/2015:AbstractLinearIntervalsFunction modifications
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.interpolation;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.utils.BinarySearchIndexClosedOpen;

/**
 * 
 * Tests the class BiLinearIntervalsInterpolator.
 * 
 * @author Sophie LAURENS
 * @version $Id: BiLinearIntervalsFunctionTest.java 17909 2017-09-11 11:57:36Z bignon $
 * @since 2.3
 */

public class BiLinearIntervalsFunctionTest {

    /** Numerical precision. */
    protected static final double EPSILON = 10e-14;

    /** Features description. */
    public enum features {

        /**
         * @featureTitle tests method value(double)
         * @featureDescription tests linear interpolation in dimension 2
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
     * @testedMethod {@link BiLinearIntervalsFunction#value(double)}
     * @description Tests the value method for the test case given in the MU of MSPro
     * @input xval = {0,5,10,15,20}, yval = {0,10,20,30,40,50,60,70,80,90,100}
     * @testPassCriteria obtaining fxref = 185.30 for x=6 and y=55
     * @since 2.3
     */
    @Test
    public void nominalCaseMSPRO() {
        final int dimx = 5;
        final int dimy = 11;
        final double[] xval = { 0, 5, 10, 15, 20 };
        final double[] yval = { 0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };
        final double x = 6;
        final double y = 55;

        final double fxyref = 185.30;

        final double[][] fval = new double[dimx][dimy];
        for (int i = 0; i < dimx; i++) {
            for (int j = 0; j < dimy; j++) {
                fval[i][j] = 5 + 2 * xval[i] + 3 * yval[j] + 0.01 * xval[i] * yval[j];
            }
        }

        final BiLinearIntervalsFunction interp = new BiLinearIntervalsFunction(xval, yval, fval);
        final double fxy = interp.value(x, y);
        Assert.assertEquals(fxyref, fxy, EPSILON);
    }

    /**
     * @testType UT
     * @testedFeature {@link features#INTERPOLATION}
     * @testedMethod {@link BiLinearIntervalsFunction#value(double)}
     * @description Computes the Cx and Cz coefficients with incidence laws.
     * @input given by CNES in DM 269
     * @testPassCriteria given by CNES in DM 269
     * @since 2.3
     */
    @Test
    public void nominalCase() {

        // incidence laws, depending on the mach number
        final double[] aoaTab = { 10., 20., 30. };
        final double[] machTab = { 0., 1., 10., 50. };

        // Cx and Cz depending only on the incidence
        final double[][] cxTab = { { 0.10, 0.20, 0.30, 0.40 },
            { 0.20, 0.30, 0.40, 0.50 },
            { 0.30, 0.40, 0.50, 0.60 }, };

        final double[][] czTab = { { 0.05, 0.10, 0.10, 0.05 },
            { 0.10, 0.15, 0.15, 0.10 },
            { 0.15, 0.20, 0.20, 0.25 }, };

        final BiLinearIntervalsFunction cxInterpolator = new BiLinearIntervalsFunction(aoaTab, machTab, cxTab);
        final BiLinearIntervalsFunction czInterpolator = new BiLinearIntervalsFunction(aoaTab, machTab, czTab);

        final double alpha = 12.5;

        double mach = 0.;
        final double dmach = 1.;

        final ArrayList<Double> xres = new ArrayList<Double>();
        final ArrayList<Double> yres = new ArrayList<Double>();

        do {
            final double cx = cxInterpolator.value(alpha, mach);
            final double cz = czInterpolator.value(alpha, mach);
            // System.out.println("alpha : " + alpha + " mach : " + mach + " cx : " + cx + " cz : " + cz + " cz/cx  " +
            // cz / cx);
            xres.add(mach);
            yres.add(cz / cx);
            mach = mach + dmach;
        } while (mach <= machTab[machTab.length - 1]);
    }

    /**
     * @testType UT
     * @testedFeature {@link features#INTERPOLATION}
     * @testedMethod {@link BiLinearIntervalsFunction#value(double)}
     * @description Creates an instance from BiLinearIntervalsFunction. No trick.
     *              Goes through ALL the boundary cases, meaning at 4 cases are required !
     * @since 2.3
     */
    @Test
    public void testBoundaryConditions() {

        final double[] xtab = { 0, 1, 2, 3, 4, 5 };
        final double[] ytab = { 0, 1, 2, 3, 4 };
        final double[][] ftab = new double[6][5];
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 5; j++) {
                ftab[i][j] = xtab[i] * ytab[j];
            }
        }

        final BiLinearIntervalsInterpolator interp1 = new BiLinearIntervalsInterpolator();
        final BiLinearIntervalsFunction interp = interp1.interpolate(xtab, ytab, ftab);

        double x = 4.5;
        double y = 3.5;
        double fxyref = x * y;
        double fxy = interp.value(x, y);
        Assert.assertEquals(fxyref, fxy, EPSILON);

        x = 5;
        fxyref = x * y;
        fxy = interp.value(x, y);
        Assert.assertEquals(fxyref, fxy, EPSILON);

        y = 4;
        fxyref = x * y;
        fxy = interp.value(x, y);
        Assert.assertEquals(fxyref, fxy, EPSILON);

        y = 1.5;
        fxyref = x * y;
        fxy = interp.value(x, y);
        Assert.assertEquals(fxyref, fxy, EPSILON);

        x = 0.5;
        y = 4;
        fxyref = x * y;
        fxy = interp.value(x, y);
        Assert.assertEquals(fxyref, fxy, EPSILON);

        y = 1.4;
        fxyref = x * y;
        fxy = interp.value(x, y);
        Assert.assertEquals(fxyref, fxy, EPSILON);

        x = -1;
        fxyref = x * y;
        fxy = interp.value(x, y);
        Assert.assertEquals(fxyref, fxy, EPSILON);

        y = -1;
        fxyref = x * y;
        fxy = interp.value(x, y);
        Assert.assertEquals(fxyref, fxy, EPSILON);
    }

    /**
     * @testType UT
     * @testedFeature {@link features#COVERAGE}
     * @testedMethod {@link BiiLinearIntervalsFunction#BiiLinearIntervalsFunction(double[], double[])}
     * @description Error test where not enough in x, y for the constructor
     * @testPassCriteria NumberIsTooSmallException
     * @since 2.3
     */
    @Test(expected = NumberIsTooSmallException.class)
    public void errorNotEnoughX() {

        final double[] xtab = { 0 };
        final double[] ytab = { 0, 1, 2, 3, 4 };

        // case with unsorted entries
        final double[][] ftab = new double[1][5];
        for (int i = 0; i < 1; i++) {
            for (int j = 0; j < 5; j++) {
                ftab[i][j] = xtab[i] + ytab[j];
            }

        }
        // expected behavior : throws exception
        new BiLinearIntervalsFunction(xtab, ytab, ftab);
    }

    /**
     * @testType UT
     * @testedFeature {@link features#COVERAGE}
     * @testedMethod {@link BiiLinearIntervalsFunction#BiiLinearIntervalsFunction(double[], double[])}
     * @description Error test where not enough in x, y for the constructor
     * @testPassCriteria NumberIsTooSmallException
     * @since 2.3
     */
    @Test(expected = NumberIsTooSmallException.class)
    public void errorNotEnoughY() {

        final double[] ytab = { 0 };
        final double[] xtab = { 0, 1, 2, 3, 4 };

        // case with unsorted entries
        final double[][] ftab = new double[5][1];
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 1; j++) {
                ftab[i][j] = xtab[i] + ytab[j];

            }
        }

        // expected behavior : throws exception
        new BiLinearIntervalsFunction(xtab, ytab, ftab);
    }

    /**
     * @testType UT
     * @testedFeature {@link features#COVERAGE}
     * @testedMethod {@link BiiLinearIntervalsFunction#BiiLinearIntervalsFunction(double[], double[])}
     * @description Error test where x, y do not have the same length as ftab for the constructor
     * @testPassCriteria DimensionMismatchException
     * @since 2.3
     */
    @Test(expected = DimensionMismatchException.class)
    public void errorDimensionTabandX() {

        final double[] xtab = { 0, 1, 2, 3, 4, 5 };
        final double[] ytab = { 0, 1, 2, 3, 4 };

        // case with unsorted entries
        final double[][] ftab = new double[5][5];
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                ftab[i][j] = xtab[i] + ytab[j];

            }
        }
        // expected behavior : throws exception
        new BiLinearIntervalsFunction(xtab, ytab, ftab);
    }

    /**
     * @testType UT
     * @testedFeature {@link features#COVERAGE}
     * @testedMethod {@link BiiLinearIntervalsFunction#BiiLinearIntervalsFunction(double[], double[])}
     * @description Error test where x, y do not have the same length as ftab for the constructor
     * @testPassCriteria DimensionMismatchException
     * @since 2.3
     */
    @Test(expected = DimensionMismatchException.class)
    public void errorDimensionTabandY() {

        final double[] xtab = { 0, 1, 2, 3, 4, 5 };
        final double[] ytab = { 0, 1, 2, 3, 4 };

        // case with unsorted entries
        final double[][] ftab = new double[6][4];
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 4; j++) {
                ftab[i][j] = xtab[i] + ytab[j];
            }

        }
        // expected behavior : throws exception
        new BiLinearIntervalsFunction(xtab, ytab, ftab);
    }

    /**
     * @testType UT
     * @testedFeature {@link features#COVERAGE}
     * @testedMethod {@link BiLinearIntervalsFunction#getValues()}
     * @description
     * @testPassCriteria
     * @since 2.3.1
     */
    @Test
    public void testGetValues() {

        final double[] xtab = { 0, 1, 2, 3, 4, 5 };
        final double[] ytab = { 0, 1, 2, 3, 4 };
        final double[][] ftab = new double[6][5];
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 5; j++) {
                ftab[i][j] = xtab[i] * ytab[j];
            }
        }

        final BiLinearIntervalsFunction interp = new BiLinearIntervalsFunction(new BinarySearchIndexClosedOpen(xtab),
            new BinarySearchIndexClosedOpen(ytab), ftab);

        // tests method getValues (a copy should be returned)
        final double[][] fobtained = interp.getValues();
        for (int i = 0; i < fobtained.length; i++) {
            for (int j = 0; j < fobtained[i].length; j++) {
                Assert.assertEquals(ftab[i][j], fobtained[i][j], 0);
            }
        }
    }

}
