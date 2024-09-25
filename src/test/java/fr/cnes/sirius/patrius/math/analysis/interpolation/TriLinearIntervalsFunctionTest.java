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
 *
 * @history 01/10/2014:creation
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:269:01/10/2014:piecewise linear interpolations
 * VERSION::FA:386:19/12/2014:index mutualisation for ephemeris interpolation
 * VERSION::FA:417:12/02/2015:AbstractLinearIntervalsFunction modifications
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.interpolation;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.utils.BinarySearchIndexClosedOpen;

/**
 * 
 * Tests the class UniLinearIntervalsInterpolator.
 * 
 * @author Sophie LAURENS
 * @version $Id: TriLinearIntervalsFunctionTest.java 17909 2017-09-11 11:57:36Z bignon $
 * @since 2.3
 */
public class TriLinearIntervalsFunctionTest {

    /** Numerical precision. */
    protected static final double EPSILON = 10e-14;

    /** Features description. */
    public enum features {

        /**
         * @featureTitle tests method value(double)
         * @featureDescription tests linear interpolation in dimension 3
         */
        INTERPOLATION,
        /**
         * @featureTitle tests method to ensure proper coverage
         * @featureDescription
         */
        COVERAGE,
    }

    /**
     * Comparison with 3D test case from MU_MSPRO.
     */
    @Test
    public void nominalCaseMSPRO() {
        final int dimx = 5;
        final int dimy = 6;
        final int dimz = 5;
        final double[] xval = { 0, 5, 10, 15, 20 };
        final double[] yval = { 0, 20, 40, 60, 80, 100 };
        final double[] zval = { -20, -15, -10, -5, 0 };
        final double x = 6;
        final double y = 55;
        final double z = -14;

        final double fxyzref = 120.76;

        final double[][][] fval = new double[dimx][dimy][dimz];
        for (int i = 0; i < dimx; i++) {
            for (int j = 0; j < dimy; j++) {
                for (int k = 0; k < dimz; k++) {
                    fval[i][j][k] = 5 + 2 * xval[i] + 3 * yval[j] + 4 * zval[k] + 0.01 * xval[i]
                            * yval[j] + 0.01 * xval[i] * zval[k] + 0.01 * yval[j] * zval[k];
                }
            }
        }

        final TriLinearIntervalsFunction interp = new TriLinearIntervalsFunction(xval, yval, zval,
                fval);
        final double fxyz = interp.value(x, y, z);
        Assert.assertEquals(fxyzref, fxyz, EPSILON);
    }

    /**
     * Creates an instance from TriLinearIntervalsFunction.
     * No trick.
     * Goes through ALL the loops for the boundary treatments, meaning at least 2^3 cases are
     * required !
     */
    @Test
    public void testBoundaryConditions() {

        final double[] xtab = { 0, 1, 2, 3, 4, 5 };
        final double[] ytab = { 0, 1, 2, 3, 4 };
        final double[] ztab = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        final double[][][] ftab = new double[6][5][10];
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 5; j++) {
                for (int k = 0; k < 10; k++) {
                    ftab[i][j][k] = xtab[i] * ytab[j] + ztab[k];
                }
            }
        }
        final TriLinearIntervalsInterpolator interp1 = new TriLinearIntervalsInterpolator();
        final TriLinearIntervalsFunction interp = interp1.interpolate(xtab, ytab, ztab, ftab);

        double x = 4.5;
        double y = 3.5;
        double z = 0.5;
        double fxyzref = x * y + z;
        double fxyz = interp.value(x, y, z);
        Assert.assertEquals(fxyzref, fxyz, EPSILON);

        x = 5;
        fxyzref = x * y + z;
        fxyz = interp.value(x, y, z);
        Assert.assertEquals(fxyzref, fxyz, EPSILON);

        y = 4;
        fxyzref = x * y + z;
        fxyz = interp.value(x, y, z);
        Assert.assertEquals(fxyzref, fxyz, EPSILON);

        z = 9;
        fxyzref = x * y + z;
        fxyz = interp.value(x, y, z);
        Assert.assertEquals(fxyzref, fxyz, EPSILON);

        z = 2.9;
        fxyzref = x * y + z;
        fxyz = interp.value(x, y, z);
        Assert.assertEquals(fxyzref, fxyz, EPSILON);

        z = 9;
        y = 1.5;
        fxyzref = x * y + z;
        fxyz = interp.value(x, y, z);
        Assert.assertEquals(fxyzref, fxyz, EPSILON);

        x = 0.5;
        y = 4;
        fxyzref = x * y + z;
        fxyz = interp.value(x, y, z);
        Assert.assertEquals(fxyzref, fxyz, EPSILON);

        z = 7.9;
        fxyzref = x * y + z;
        fxyz = interp.value(x, y, z);
        Assert.assertEquals(fxyzref, fxyz, EPSILON);

        y = 1.4;
        z = 9;
        fxyzref = x * y + z;
        fxyz = interp.value(x, y, z);
        Assert.assertEquals(fxyzref, fxyz, EPSILON);

        x = -1;
        fxyzref = x * y + z;
        fxyz = interp.value(x, y, z);
        Assert.assertEquals(fxyzref, fxyz, EPSILON);

        y = -1;
        fxyzref = x * y + z;
        fxyz = interp.value(x, y, z);
        Assert.assertEquals(fxyzref, fxyz, EPSILON);

        z = -1;
        fxyzref = x * y + z;
        fxyz = interp.value(x, y, z);
        Assert.assertEquals(fxyzref, fxyz, EPSILON);
    }

    /**
     * Error test where not enough in x, y or z for the constructor
     */
    @Test(expected = NumberIsTooSmallException.class)
    public void errorNotEnoughX() {

        final double[] xtab = { 0 };
        final double[] ytab = { 0, 1, 2, 3, 4 };
        final double[] ztab = { 0, 1, 2, 3, 4, 5, 6, 7, 7, 8, 9 };

        // case with unsorted entries
        final double[][][] ftab = new double[1][5][11];
        for (int i = 0; i < 1; i++) {
            for (int j = 0; j < 5; j++) {
                for (int k = 0; k < 11; k++) {
                    ftab[i][j][k] = xtab[i] * ytab[j];
                }
            }
        }
        // expected behavior : throws exception
        new TriLinearIntervalsFunction(xtab, ytab, ztab, ftab);

    }

    /**
     * Error test where not enough in x, y or z for the constructor
     */
    @Test(expected = NumberIsTooSmallException.class)
    public void errorNotEnoughY() {

        final double[] ytab = { 0 };
        final double[] xtab = { 0, 1, 2, 3, 4 };
        final double[] ztab = { 0, 1, 2, 3, 4, 5, 6, 7, 7, 8, 9 };

        // case with unsorted entries
        final double[][][] ftab = new double[5][1][11];
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 1; j++) {
                for (int k = 0; k < 11; k++) {
                    ftab[i][j][k] = xtab[i] * ytab[j];
                }
            }
        }
        // expected behavior : throws exception
        new TriLinearIntervalsFunction(xtab, ytab, ztab, ftab);

    }

    /**
     * Error test where not enough in x, y or z for the constructor
     */
    @Test(expected = NumberIsTooSmallException.class)
    public void errorNotEnoughZ() {

        final double[] ztab = { 0 };
        final double[] ytab = { 0, 1, 2, 3, 4 };
        final double[] xtab = { 0, 1, 2, 3, 4, 5, 6, 7, 7, 8, 9 };

        // case with unsorted entries
        final double[][][] ftab = new double[11][5][1];
        for (int i = 0; i < 11; i++) {
            for (int j = 0; j < 5; j++) {
                for (int k = 0; k < 1; k++) {
                    ftab[i][j][k] = xtab[i] * ytab[j];
                }
            }
        }
        // expected behavior : throws exception
        new TriLinearIntervalsFunction(xtab, ytab, ztab, ftab);

    }

    /**
     * Error test where not enough in x, y or z do not have the same length as ftab for the
     * constructor
     */
    @Test(expected = DimensionMismatchException.class)
    public void errorDimensionTabandX() {

        final double[] xtab = { 0, 1, 2, 3, 4, 5 };
        final double[] ytab = { 0, 1, 2, 3, 4 };
        final double[] ztab = { 0, 1, 2, 3, 4, 5, 6, 7, 7, 8, 9 };

        // case with unsorted entries
        final double[][][] ftab = new double[5][5][11];
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                for (int k = 0; k < 11; k++) {
                    ftab[i][j][k] = xtab[i] * ytab[j];
                }
            }
        }
        // expected behavior : throws exception
        new TriLinearIntervalsFunction(xtab, ytab, ztab, ftab);
    }

    /**
     * Error test where not enough in x, y or z do not have the same length as ftab for the
     * constructor
     */
    @Test(expected = DimensionMismatchException.class)
    public void errorDimensionTabandY() {

        final double[] xtab = { 0, 1, 2, 3, 4, 5 };
        final double[] ytab = { 0, 1, 2, 3, 4 };
        final double[] ztab = { 0, 1, 2, 3, 4, 5, 6, 7, 7, 8, 9 };

        // case with unsorted entries
        final double[][][] ftab = new double[6][4][11];
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 11; k++) {
                    ftab[i][j][k] = xtab[i] * ytab[j];
                }
            }
        }
        // expected behavior : throws exception
        new TriLinearIntervalsFunction(xtab, ytab, ztab, ftab);
    }

    /**
     * Error test where not enough in x, y or z do not have the same length as ftab for the
     * constructor
     */
    @Test(expected = DimensionMismatchException.class)
    public void errorDimensionTabandZ() {

        final double[] xtab = { 0, 1, 2, 3, 4, 5 };
        final double[] ytab = { 0, 1, 2, 3, 4 };
        final double[] ztab = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };

        // case with unsorted entries
        final double[][][] ftab = new double[6][5][8];
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 5; j++) {
                for (int k = 0; k < 8; k++) {
                    ftab[i][j][k] = xtab[i] * ytab[j];
                }
            }
        }
        new TriLinearIntervalsFunction(xtab, ytab, ztab, ftab);
    }

    /**
     * @testType UT
     * @testedFeature {@link features#COVERAGE}
     * @testedMethod {@link TriLinearIntervalsFunction#getValues()}
     * @description
     * @testPassCriteria
     * @since 2.3.1
     */
    @Test
    public void testGetValues() {

        final double[] xtab = { 0, 1, 2, 3, 4, 5 };
        final double[] ytab = { 0, 1, 2, 3, 4 };
        final double[] ztab = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        final double[][][] ftab = new double[6][5][10];
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 5; j++) {
                for (int k = 0; k < 10; k++) {
                    ftab[i][j][k] = xtab[i] * ytab[j] + ztab[k];
                }
            }
        }

        final TriLinearIntervalsFunction interp = new TriLinearIntervalsFunction(
                new BinarySearchIndexClosedOpen(xtab), new BinarySearchIndexClosedOpen(ytab),
                new BinarySearchIndexClosedOpen(ztab), ftab);

        // tests method getValues (a copy should be returned)
        final double[][][] fobtained = interp.getValues();
        for (int i = 0; i < fobtained.length; i++) {
            for (int j = 0; j < fobtained[i].length; j++) {
                for (int k = 0; k < fobtained[i][j].length; k++) {
                    Assert.assertEquals(ftab[i][j][k], fobtained[i][j][k], 0);
                }
            }
        }
    }

    /**
     * @description Evaluate the linear function serialization / deserialization process.
     *
     * @testPassCriteria The linear function can be serialized and deserialized.
     */
    @Test
    public void testSerialization() {

        final double[] xtab = { 0, 1, 2, 3, 4, 5 };
        final double[] ytab = { 0, 1, 2, 3, 4 };
        final double[] ztab = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        final double[][][] ftab = new double[6][5][10];
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 5; j++) {
                for (int k = 0; k < 10; k++) {
                    ftab[i][j][k] = xtab[i] * ytab[j] + ztab[k];
                }
            }
        }
        final TriLinearIntervalsFunction fct = new TriLinearIntervalsFunction(xtab, ytab, ztab,
                ftab);
        final TriLinearIntervalsFunction deserializedFct = TestUtils.serializeAndRecover(fct);

        for (double x = 0.; x <= 5.; x += 0.5) {
            for (double y = 0.; y <= 5.; y += 0.5) {
                for (double z = 0.; z <= 10.; z += 0.5) {
                    Assert.assertEquals(fct.value(x, y, z), deserializedFct.value(x, y, z), EPSILON);
                }
            }
        }
    }
}
