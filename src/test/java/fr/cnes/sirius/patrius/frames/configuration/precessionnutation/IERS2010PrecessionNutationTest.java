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
 * @history creation 18/10/2012
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:306:12/11/2014:coverage
 * VERSION::DM:660:24/09/2016:add getters to frames configuration
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.precessionnutation;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.frames.configuration.FrameConvention;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for {@link PrecessionNutationCache}.
 * 
 * @author Rami Houdroge
 * @since 1.3
 * @version $Id: IERS2010PrecessionNutationTest.java 18088 2017-10-02 17:01:51Z bignon $
 * 
 */
public class IERS2010PrecessionNutationTest {

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
    public void testIsDirect() {
        final PrecessionNutationModel pnD = new IERS20032010PrecessionNutation(PrecessionNutationConvention.IERS2010,
            true);

        Assert.assertTrue(pnD.isDirect());

    }

    @Test
    public void testRef() throws IllegalArgumentException {

        // IERS 2010
        final PrecessionNutationModel pnD = new IERS20032010PrecessionNutation(PrecessionNutationConvention.IERS2010,
            true);

        // IERS 2003
        final PrecessionNutationModel pnD3 = new IERS20032010PrecessionNutation(PrecessionNutationConvention.IERS2003,
            true);

        // Date
        final AbsoluteDate date = new AbsoluteDate(2005, 3, 5, 0, 0, 0.0, TimeScalesFactory.getTAI());

        // test against original implementation
        final double[] cip10ref = { 4.903997475506334E-4, 4.166125167417491E-5, -1.4432494003292058E-8 };
        final double[] cip03ref = { 4.903997402091521E-4, 4.1661387967743685E-5, -1.4432569047616784E-8 };
        this.assertArrayEquals(cip10ref, pnD.getCIPMotion(date), Precision.EPSILON);
        this.assertArrayEquals(cip03ref, pnD3.getCIPMotion(date), Precision.EPSILON);

        final double[] cip03dvRef = { 5.937686116830366E-12, 6.514079689041986E-13, -3.7719714412691603E-17 };
        final double[] cip10dvRef = { 5.937685565984846E-12, 6.514070432335979E-13, -3.7719099455199287E-17 };
        this.assertArrayEquals(cip10dvRef, pnD.getCIPMotionTimeDerivative(date), Precision.EPSILON);
        this.assertArrayEquals(cip03dvRef, pnD3.getCIPMotionTimeDerivative(date), Precision.EPSILON);
    }

    /**
     * Test methods getOrigin() and isConstant().
     */
    @Test
    public void testGetters() {
        final PrecessionNutationModel pnModel1 = PrecessionNutationModelFactory.PN_IERS2003_DIRECT_CONSTANT;
        Assert.assertEquals(FrameConvention.IERS2003, pnModel1.getOrigin());
        Assert.assertEquals(true, pnModel1.isConstant());

        final PrecessionNutationModel pnModel2 = PrecessionNutationModelFactory.PN_IERS2003_DIRECT_NON_CONSTANT;
        Assert.assertEquals(FrameConvention.IERS2003, pnModel2.getOrigin());
        Assert.assertEquals(false, pnModel2.isConstant());

        final PrecessionNutationModel pnModel3 = PrecessionNutationModelFactory.PN_IERS2003_INTERPOLATED_CONSTANT;
        Assert.assertEquals(FrameConvention.IERS2003, pnModel3.getOrigin());
        Assert.assertEquals(true, pnModel3.isConstant());

        final PrecessionNutationModel pnModel4 = PrecessionNutationModelFactory.PN_IERS2003_INTERPOLATED_NON_CONSTANT;
        Assert.assertEquals(FrameConvention.IERS2003, pnModel4.getOrigin());
        Assert.assertEquals(false, pnModel4.isConstant());

        final PrecessionNutationModel pnModel5 = PrecessionNutationModelFactory.PN_IERS2010_DIRECT_CONSTANT;
        Assert.assertEquals(FrameConvention.IERS2010, pnModel5.getOrigin());
        Assert.assertEquals(true, pnModel5.isConstant());

        final PrecessionNutationModel pnModel6 = PrecessionNutationModelFactory.PN_IERS2010_DIRECT_NON_CONSTANT;
        Assert.assertEquals(FrameConvention.IERS2010, pnModel6.getOrigin());
        Assert.assertEquals(false, pnModel6.isConstant());

        final PrecessionNutationModel pnModel7 = PrecessionNutationModelFactory.PN_IERS2010_INTERPOLATED_CONSTANT;
        Assert.assertEquals(FrameConvention.IERS2010, pnModel7.getOrigin());
        Assert.assertEquals(true, pnModel7.isConstant());

        final PrecessionNutationModel pnModel8 = PrecessionNutationModelFactory.PN_IERS2010_INTERPOLATED_NON_CONSTANT;
        Assert.assertEquals(FrameConvention.IERS2010, pnModel8.getOrigin());
        Assert.assertEquals(false, pnModel8.isConstant());
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
