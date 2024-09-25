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
 * VERSION:4.13.5:DM:DM-319:03/07/2024:[PATRIUS] Assurer la compatibilite ascendante de la v4.13
 * VERSION:4.13.2:DM:DM-222:08/03/2024:[PATRIUS] Assurer la compatibilité ascendante
 * VERSION:4.13:DM:DM-103:08/12/2023:[PATRIUS] Optimisation du CIRFProvider
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
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
import fr.cnes.sirius.patrius.utils.PatriusConfiguration;
import fr.cnes.sirius.patrius.utils.PatriusConfiguration.PatriusVersionCompatibility;

/**
 * Test class for {@link IERS20032010PrecessionNutation}.
 * 
 * @author Rami Houdroge
 * @since 1.3
 * @version $Id: IERS2010PrecessionNutationTest.java 18088 2017-10-02 17:01:51Z bignon $
 * 
 */
public class IERS20032010PrecessionNutationTest {

    @Test
    public void testSynchronizedToken() {

        // IERS 2010
        final IERS20032010PrecessionNutation pnD = new IERS20032010PrecessionNutation(
            PrecessionNutationConvention.IERS2010);
        // IERS 2003
        final IERS20032010PrecessionNutation pnD3 = new IERS20032010PrecessionNutation(
            PrecessionNutationConvention.IERS2003);

        // Date
        final AbsoluteDate date = new AbsoluteDate(2005, 3, 5, 0, 0, 0.0, TimeScalesFactory.getTAI());

        // no asserts are necessary, values tested below
        pnD.getCIPCoordinates(date).getCIPMotion();
        pnD.getCIPCoordinates(date).getCIPMotionTimeDerivatives();
        pnD3.getCIPCoordinates(date).getCIPMotionTimeDerivatives();
        pnD3.getCIPCoordinates(date).getCIPMotion();
    }

    @Test
    public void testIsDirect() {
        final PrecessionNutationModel pnD = new IERS20032010PrecessionNutation(PrecessionNutationConvention.IERS2010);

        Assert.assertTrue(pnD.isDirect());
    }

    @Test
    public void testRef() throws IllegalArgumentException {
        
        PatriusConfiguration.setPatriusCompatibilityMode(PatriusVersionCompatibility.NEW_MODELS);

        // IERS 2010
        final PrecessionNutationModel pnD = new IERS20032010PrecessionNutation(PrecessionNutationConvention.IERS2010);

        // IERS 2003
        final PrecessionNutationModel pnD3 = new IERS20032010PrecessionNutation(PrecessionNutationConvention.IERS2003);

        // Date
        final AbsoluteDate date = new AbsoluteDate(2005, 3, 5, 0, 0, 0.0, TimeScalesFactory.getTAI());

        // test against original implementation
        final double[] cip10ref = { 4.903997475506334E-4, 4.166125167417491E-5, -1.4432494003292058E-8 };
        final double[] cip03ref = { 4.903997402091521E-4, 4.1661387967743685E-5, -1.4432569047616784E-8 };
        assertArrayEquals(cip10ref, pnD.getCIPCoordinates(date).getCIPMotion(), Precision.EPSILON);
        assertArrayEquals(cip03ref, pnD3.getCIPCoordinates(date).getCIPMotion(), Precision.EPSILON);

        final double[] cip03dvRef = { 5.937686116830366E-12, 6.514079689041986E-13, -3.7719714412691603E-17 };
        final double[] cip10dvRef = { 5.937685565984846E-12, 6.514070432335979E-13, -3.7719099455199287E-17 };
        assertArrayEquals(cip10dvRef, pnD.getCIPCoordinates(date).getCIPMotionTimeDerivatives(), Precision.EPSILON);
        assertArrayEquals(cip03dvRef, pnD3.getCIPCoordinates(date).getCIPMotionTimeDerivatives(), Precision.EPSILON);
    }

    /**
     * Test methods getOrigin() and isConstant().
     */
    @Test
    public void testGetters() {

        final PrecessionNutationModel pnModel1 = PrecessionNutationModelFactory.NO_PN;
        Assert.assertEquals(FrameConvention.NONE, pnModel1.getOrigin());
        Assert.assertTrue(pnModel1.isDirect());

        final PrecessionNutationModel pnModel2 = PrecessionNutationModelFactory.PN_IERS2010_INTERPOLATED;
        Assert.assertEquals(FrameConvention.IERS2010, pnModel2.getOrigin());
        Assert.assertFalse(pnModel2.isDirect());

        final PrecessionNutationModel pnModel3 = PrecessionNutationModelFactory.PN_IERS2003_INTERPOLATED;
        Assert.assertEquals(FrameConvention.IERS2003, pnModel3.getOrigin());
        Assert.assertFalse(pnModel3.isDirect());

        final PrecessionNutationModel pnModel4 = PrecessionNutationModelFactory.PN_IERS2010_INTERPOLATED_BY_THREAD;
        Assert.assertEquals(FrameConvention.IERS2010, pnModel4.getOrigin());
        Assert.assertFalse(pnModel4.isDirect());

        Assert.assertEquals(pnModel4.getCIPCoordinates(AbsoluteDate.J2000_EPOCH),
            pnModel2.getCIPCoordinates(AbsoluteDate.J2000_EPOCH));

        final PrecessionNutationModel pnModel5 = PrecessionNutationModelFactory.PN_IERS2003_INTERPOLATED_BY_THREAD;
        Assert.assertEquals(FrameConvention.IERS2003, pnModel5.getOrigin());
        Assert.assertFalse(pnModel5.isDirect());
        Assert.assertEquals(pnModel5.getCIPCoordinates(AbsoluteDate.J2000_EPOCH),
            pnModel3.getCIPCoordinates(AbsoluteDate.J2000_EPOCH));

        final PrecessionNutationModel pnModel6 = PrecessionNutationModelFactory.PN_IERS2010_DIRECT;
        Assert.assertEquals(FrameConvention.IERS2010, pnModel6.getOrigin());
        Assert.assertTrue(pnModel6.isDirect());

        final PrecessionNutationModel pnModel7 = PrecessionNutationModelFactory.PN_IERS2003_DIRECT;
        Assert.assertEquals(FrameConvention.IERS2003, pnModel7.getOrigin());
        Assert.assertTrue(pnModel7.isDirect());

        final PrecessionNutationModel pnModel8 = PrecessionNutationModelFactory.PN_STELA;
        Assert.assertEquals(FrameConvention.STELA, pnModel8.getOrigin());
        Assert.assertTrue(pnModel8.isDirect());

        final PrecessionNutationModel pnModel9 = PrecessionNutationModelFactory.PN_IERS2003_DIRECT_CONSTANT_OLD;
        Assert.assertEquals(FrameConvention.IERS2003, pnModel9.getOrigin());
        Assert.assertTrue(pnModel9.isDirect());

        final PrecessionNutationModel pnModel10 = PrecessionNutationModelFactory.PN_IERS2003_DIRECT_NON_CONSTANT_OLD;
        Assert.assertEquals(FrameConvention.IERS2003, pnModel10.getOrigin());
        Assert.assertTrue(pnModel10.isDirect());

        final PrecessionNutationModel pnModel11 = PrecessionNutationModelFactory.PN_IERS2003_INTERPOLATED_CONSTANT_OLD;
        Assert.assertEquals(FrameConvention.IERS2003, pnModel11.getOrigin());
        Assert.assertTrue(!pnModel11.isDirect());

        final PrecessionNutationModel pnModel12 =
            PrecessionNutationModelFactory.PN_IERS2003_INTERPOLATED_NON_CONSTANT_OLD;
        Assert.assertEquals(FrameConvention.IERS2003, pnModel12.getOrigin());
        Assert.assertTrue(!pnModel12.isDirect());

        final PrecessionNutationModel pnModel13 = PrecessionNutationModelFactory.PN_IERS2010_DIRECT_CONSTANT_OLD;
        Assert.assertEquals(FrameConvention.IERS2010, pnModel13.getOrigin());
        Assert.assertTrue(pnModel13.isDirect());

        final PrecessionNutationModel pnModel14 = PrecessionNutationModelFactory.PN_IERS2010_DIRECT_NON_CONSTANT_OLD;
        Assert.assertEquals(FrameConvention.IERS2010, pnModel14.getOrigin());
        Assert.assertTrue(pnModel14.isDirect());

        final PrecessionNutationModel pnModel15 = PrecessionNutationModelFactory.PN_IERS2010_INTERPOLATED_CONSTANT_OLD;
        Assert.assertEquals(FrameConvention.IERS2010, pnModel15.getOrigin());
        Assert.assertTrue(!pnModel15.isDirect());

        final PrecessionNutationModel pnModel16 =
            PrecessionNutationModelFactory.PN_IERS2010_INTERPOLATED_NON_CONSTANT_OLD;
        Assert.assertEquals(FrameConvention.IERS2010, pnModel16.getOrigin());
        Assert.assertTrue(!pnModel16.isDirect());

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
    private static void assertArrayEquals(final double[] exp, final double[] act, final double eps) {

        Assert.assertEquals(exp.length, act.length);
        for (int i = 0; i < exp.length; i++) {
            Assert.assertEquals(0, (exp[i] - act[i]) / act[i], eps);
        }
    }
}
