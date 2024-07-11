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

import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.math.utils.BinarySearchIndexClosedOpen;

/**
 * 
 * Tests the class AbstractLinearIntervalsFunction.
 * 
 * @author Sophie LAURENS
 * @version $Id: AbstractLinearIntervalsFunctionTest.java 17909 2017-09-11 11:57:36Z bignon $
 * @since 2.3
 */

public class AbstractLinearIntervalsFunctionTest {

    /** Numerical precision. */
    protected static final double EPSILON = Precision.DOUBLE_COMPARISON_EPSILON;

    /** Features description. */
    public enum features {
        /**
         * @featureTitle AbstractLinearIntervalsFunctionTest
         * 
         * @featureDescription tests linear interpolation
         * 
         */
        ABSTRACT_LINEAR_INTERVALS_FUNCTION_TEST,
    }

    /**
     * Creates instances from Uni/Bi/TriLinearIntervalsFunction.
     * No trick.
     */
    @Test
    public void simpleTestGetIndex() {

        final double[] xtab = { 0, 1, 2, 3, 4, 5 };
        final double[] ytab = { 0, 1, 2, 3, 4 };
        final double[] ztab = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        final double[] ftab1 = new double[6];
        final double[][] ftab2 = new double[6][5];
        final double[][][] ftab3 = new double[6][5][10];
        for (int i = 0; i < 6; i++) {
            ftab1[i] = 2 * xtab[i];

            for (int j = 0; j < 5; j++) {
                ftab2[i][j] = xtab[i] * ytab[j];

                for (int k = 0; k < 10; k++) {
                    ftab3[i][j][k] = xtab[i] - ytab[j] * ztab[k];
                }
            }
        }
        final UniLinearIntervalsFunction unilinf = new UniLinearIntervalsFunction(xtab, ftab1);
        final BiLinearIntervalsFunction bilinf = new BiLinearIntervalsFunction(xtab, ytab, ftab2);
        final TriLinearIntervalsFunction trilinf = new TriLinearIntervalsFunction(xtab, ytab, ztab, ftab3);

        final double x = 4.5;
        final double y = 3.5;
        final double z = 0.5;

        final double fxref = 2 * x;
        final double fxyref = x * y;
        final double fxyzref = x - y * z;

        final double fx = unilinf.value(x);
        final double fxy = bilinf.value(x, y);
        final double fxyz = trilinf.value(x, y, z);

        Assert.assertEquals(fxref, fx, EPSILON);
        Assert.assertEquals(fxyref, fxy, EPSILON);
        Assert.assertEquals(fxyzref, fxyz, EPSILON);
    }

    /**
     * @testType UT
     * @testedFeature {@link features#ABSTRACT_LINEAR_INTERVALS_FUNCTION_TEST}
     * @testedMethod {@link AbstractLinearIntervalsFunction#getxtab()}
     * @testedMethod {@link AbstractLinearIntervalsFunction#getytab()}
     * @testedMethod {@link AbstractLinearIntervalsFunction#getztab()}
     * @description test array getters
     * @testPassCriteria
     * @since 2.3.1
     */
    @Test
    public void testGetTab() {

        final double[] xtab = { 0, 1, 2, 3, 4, 5 };
        final double[] ytab = { 0, 1, 2, 3, 4 };
        final double[] ztab = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        final double[] ftab1 = new double[6];
        final double[][] ftab2 = new double[6][5];
        final double[][][] ftab3 = new double[6][5][10];
        for (int i = 0; i < 6; i++) {
            ftab1[i] = xtab[i];
            for (int j = 0; j < 5; j++) {
                ftab2[i][j] = xtab[i] * ytab[j];
                for (int k = 0; k < 10; k++) {
                    ftab3[i][j][k] = xtab[i] * ytab[j] + ztab[k];
                }
            }
        }

        final UniLinearIntervalsFunction interp1 = new UniLinearIntervalsFunction(
            new BinarySearchIndexClosedOpen(xtab),
            ftab1);
        final BiLinearIntervalsFunction interp2 = new BiLinearIntervalsFunction(new BinarySearchIndexClosedOpen(xtab),
            new BinarySearchIndexClosedOpen(ytab), ftab2);
        final TriLinearIntervalsFunction interp3 = new TriLinearIntervalsFunction(
            new BinarySearchIndexClosedOpen(xtab),
            new BinarySearchIndexClosedOpen(ytab), new BinarySearchIndexClosedOpen(ztab), ftab3);

        // tests method getValues (a copy should be returned)
        final double[] xRes1 = interp1.getxtab();
        final double[] xRes2 = interp2.getxtab();
        final double[] yRes2 = interp2.getytab();
        final double[] xRes3 = interp3.getxtab();
        final double[] yRes3 = interp3.getytab();
        final double[] zRes3 = interp3.getztab();
        for (int i = 0; i < xtab.length; i++) {
            Assert.assertEquals(xtab[i], xRes1[i], 0);
            Assert.assertEquals(xtab[i], xRes2[i], 0);
            Assert.assertEquals(xtab[i], xRes3[i], 0);
        }
        for (int i = 0; i < ytab.length; i++) {
            Assert.assertEquals(ytab[i], yRes2[i], 0);
            Assert.assertEquals(ytab[i], yRes3[i], 0);
        }
        for (int i = 0; i < ztab.length; i++) {
            Assert.assertEquals(ztab[i], zRes3[i], 0);
        }
    }

}
