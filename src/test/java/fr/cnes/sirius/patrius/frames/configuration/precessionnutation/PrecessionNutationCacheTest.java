/**
 *
 * Copyright 2011-2022 CNES
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
 * @history creation 18/10/2012
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.precessionnutation;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for {@link PrecessionNutationCache}.
 * 
 * @author Rami Houdroge
 * @since 1.3
 * @version $Id: PrecessionNutationCacheTest.java 18088 2017-10-02 17:01:51Z bignon $
 * 
 */
public class PrecessionNutationCacheTest {

    private final double epsN = 5e-12;

    private final double epsNd = 1e-9;

    @Test
    public void testSynchronizedToken() throws PatriusException {

        // IERS 2010
        final IERS20032010PrecessionNutation pnD = new IERS20032010PrecessionNutation(
            PrecessionNutationConvention.IERS2010, true);
        // IERS 2003
        final IERS20032010PrecessionNutation pnD3 = new IERS20032010PrecessionNutation(
            PrecessionNutationConvention.IERS2003, true);

        // Date
        final AbsoluteDate date = new AbsoluteDate(2005, 3, 5, 0, 0, 0.0, TimeScalesFactory.getTAI());

        // no asserts are necessary, values tested below
        pnD.getCIPMotion(date);
        pnD.getCIPMotionTimeDerivative(date);
        pnD3.getCIPMotionTimeDerivative(date);
        pnD3.getCIPMotion(date);

    }

    @Test
    public void testIsDirect() throws PatriusException {
        final PrecessionNutationModel pnD = new IERS20032010PrecessionNutation(PrecessionNutationConvention.IERS2010,
            true);
        final PrecessionNutationModel pnN3 = new PrecessionNutationCache(new IERS20032010PrecessionNutation(
            PrecessionNutationConvention.IERS2003, false));

        Assert.assertTrue(pnD.isDirect());
        Assert.assertFalse(pnN3.isDirect());

        Assert.assertArrayEquals(pnN3.getCIPMotionTimeDerivative(AbsoluteDate.J2000_EPOCH), new double[3],
            Precision.EPSILON);
    }

    @Test
    public void testCIPInterpolations() throws PatriusException {

        // IERS 2010
        final PrecessionNutationModel pnD = new IERS20032010PrecessionNutation(PrecessionNutationConvention.IERS2010,
            true);
        final PrecessionNutationCache pnN = new PrecessionNutationCache(new IERS20032010PrecessionNutation(
            PrecessionNutationConvention.IERS2010, true));

        // IERS 2003
        final PrecessionNutationModel pnD3 = new IERS20032010PrecessionNutation(PrecessionNutationConvention.IERS2003,
            true);
        final PrecessionNutationCache pnN3 = new PrecessionNutationCache(new IERS20032010PrecessionNutation(
            PrecessionNutationConvention.IERS2003, true));

        // Date
        final AbsoluteDate date = new AbsoluteDate(2005, 3, 5, 0, 0, 0.0, TimeScalesFactory.getTAI());

        for (int i = 0; i < 43200; i += 600) {

            final double[] cipD = pnD.getCIPMotion(date.shiftedBy(i));
            final double[] cipN = pnN.getCIPMotion(date.shiftedBy(i));

            this.assertArrayEquals(cipN, cipD, this.epsN);

            final double[] cipD3 = pnD3.getCIPMotion(date.shiftedBy(i));
            final double[] cipN3 = pnN3.getCIPMotion(date.shiftedBy(i));

            this.assertArrayEquals(cipN3, cipD3, this.epsN);
        }

    }

    @Test
    public void testCIPDvInperpolations() throws PatriusException {

        // IERS 2010
        final PrecessionNutationModel pnD = new IERS20032010PrecessionNutation(PrecessionNutationConvention.IERS2010,
            true);
        final PrecessionNutationCache pnN = new PrecessionNutationCache(new IERS20032010PrecessionNutation(
            PrecessionNutationConvention.IERS2010, true));

        // IERS 2003
        final PrecessionNutationModel pnD3 = new IERS20032010PrecessionNutation(PrecessionNutationConvention.IERS2003,
            true);
        final PrecessionNutationCache pnN3 = new PrecessionNutationCache(new IERS20032010PrecessionNutation(
            PrecessionNutationConvention.IERS2003, true));

        // Date
        final AbsoluteDate date = new AbsoluteDate(2010, 3, 5, 0, 0, 0.0, TimeScalesFactory.getTAI());

        for (int i = 0; i < 43200; i += 600) {

            final double[] cipD3 = pnD3.getCIPMotionTimeDerivative(date.shiftedBy(i));
            final double[] cipN3 = pnN3.getCIPMotionTimeDerivative(date.shiftedBy(i));

            final double[] cipD = pnD.getCIPMotionTimeDerivative(date.shiftedBy(i));
            final double[] cipN = pnN.getCIPMotionTimeDerivative(date.shiftedBy(i));

            this.assertArrayEquals(cipN, cipD, this.epsNd);
            this.assertArrayEquals(cipN3, cipD3, this.epsNd);
        }

    }

    /**
     * array equals relative
     * 
     * @param exp
     *        expected
     * @param act
     *        actual
     * @param eps
     *        threshold
     */
    private void assertArrayEquals(final double[] exp, final double[] act, final double eps) {

        Assert.assertEquals(exp.length, act.length);
        for (int i = 0; i < exp.length; i++) {
            Assert.assertEquals(0, (exp[i] - act[i]) / act[i], eps);
        }

    }

}
