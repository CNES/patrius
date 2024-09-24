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
 * @history created 23/04/12
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:90:03/10/2013:Created TidesToolbox
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity.tides;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.forces.gravity.tides.TidesStandards.TidesStandard;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

// Règle checkstyle désactivée pour lisibilité du code de test
// CHECKSTYLE: stop MagicNumberCheck

/**
 * This class tests the tidal corrections tool box.
 * 
 * @author Gérald Mercadier, Julie Anton
 * 
 * @version $Id: TidesToolboxTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.2
 */
public class TidesToolboxTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Doodson number
         * 
         * @featureDescription decomposition of the Doodson nunmber into a sextuplet of integers.
         * 
         * @coveredRequirements DV-MOD_200
         */
        DOODSON_SEXTUPLET,
        /**
         * @featureTitle Doodson fundamental arguments
         * 
         * @featureDescription computation of the Doodson fundamental arguments
         * 
         * @coveredRequirements DV-MOD_200
         */
        DOODSON_FUNDAMENTAL_ARGUMENTS
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#DOODSON_SEXTUPLET}
     * 
     * @testedMethod {@link TidesToolbox#nDoodson(double)}
     * 
     * @description test the decomposition of the Doodson number into a sextuplet of integers
     * 
     * @input Doodson number 274.554
     * 
     * @output {2, 2, -1, 0, 0, -1}
     * 
     * @testPassCriteria the sextuplet is the expected one
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testDoodson() {

        final double doodson = 274.554;
        final int[] sextuplet = TidesToolbox.nDoodson(doodson);

        Assert.assertEquals(2, sextuplet[0]);
        Assert.assertEquals(2, sextuplet[1]);
        Assert.assertEquals(-1, sextuplet[2]);
        Assert.assertEquals(0, sextuplet[3]);
        Assert.assertEquals(0, sextuplet[4]);
        Assert.assertEquals(-1, sextuplet[5]);

    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#DOODSON_FUNDAMENTAL_ARGUMENTS}
     * 
     * @testedMethod {@link TidesToolbox#computeFundamentalArguments(AbsoluteDate, TidesStandard)}
     * 
     * @description test the computation of the Doodson fundamental arguments
     * 
     * @input a date (AbsoluteDate)
     * 
     * @output the Doodson fundamental arguments
     * 
     * @testPassCriteria the fundamental arguments are the expected one (OBELIX reference)
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testComputeFundamentalArguments() throws PatriusException {

        // Orekit data initialization
        fr.cnes.sirius.patrius.Utils.setDataRoot("regular-dataCNES-2003");
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));

        final double delta = 1.e-9;

        final AbsoluteDate date1 = new AbsoluteDate(2005, 03, 05, 00, 24, 0.0, TimeScalesFactory.getTAI());

        double[][] fundamentalArguments;
        final double[][] expectedfundamentalArg = new double[6][2];

        try {

            // test 1

            // expected results for date1 (OBELIX reference with GINS2004 standard)
            // 1.2807128746638616E+00 4.8056296567483630E+00 -2.9908080071020304E-01
            // -1.1544991332721279E+00 -4.3611455743087402E-01 -1.3434445472681302E+00
            expectedfundamentalArg[0][0] = 1.2807128746638616E+00;
            expectedfundamentalArg[1][0] = 4.8056296567483630E+00;
            expectedfundamentalArg[2][0] = -2.9908080071020304E-01;
            expectedfundamentalArg[3][0] = -1.1544991332721279E+00;
            expectedfundamentalArg[4][0] = -4.3611455743087402E-01;
            expectedfundamentalArg[5][0] = -1.3434445472681302E+00;

            expectedfundamentalArg[0][1] = 0;
            expectedfundamentalArg[1][1] = 0;
            expectedfundamentalArg[2][1] = 0;
            expectedfundamentalArg[3][1] = 0;
            expectedfundamentalArg[4][1] = 0;
            expectedfundamentalArg[5][1] = 0;

            fundamentalArguments = TidesToolbox.computeFundamentalArguments(date1, TidesStandard.GINS2004);

            this.assertArrayEquals(expectedfundamentalArg, fundamentalArguments, delta);
            FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));
            // test 2: test for negative sideral time, computed with the date "new AbsoluteDate().shiftedBy(-10000000)"
            // The aim of this test is to complete the coverage of computeSideralTime method

            // expected results for date2 (OREKIT results with GINS2004 standard)
            // 11.626955393876042 -3.9571717968630393 -3.379186164200738 1.2297470639660313 -2.289408816382161
            // -7.62827753816736
            expectedfundamentalArg[0][0] = 11.626955393876042;
            expectedfundamentalArg[1][0] = -3.9571717968630393;
            expectedfundamentalArg[2][0] = -3.379186164200738;
            expectedfundamentalArg[3][0] = 1.2297470639660313;
            expectedfundamentalArg[4][0] = -2.289408816382161;
            expectedfundamentalArg[5][0] = -7.62827753816736;

            expectedfundamentalArg[0][1] = 0;
            expectedfundamentalArg[1][1] = 0;
            expectedfundamentalArg[2][1] = 0;
            expectedfundamentalArg[3][1] = 0;
            expectedfundamentalArg[4][1] = 0;
            expectedfundamentalArg[5][1] = 0;

            fundamentalArguments = TidesToolbox.computeFundamentalArguments(new AbsoluteDate().shiftedBy(-10000000),
                TidesStandard.GINS2004);

            this.assertArrayEquals(expectedfundamentalArg, fundamentalArguments, Precision.DOUBLE_COMPARISON_EPSILON);

        } catch (final PatriusException e) {
            Assert.fail();
        }
    }

    /**
     * Check that two double[][] arrays are equal
     * 
     * @param exp
     *        expected
     * @param act
     *        actual
     * @param eps
     *        threshold
     */
    public void assertArrayEquals(final double[][] exp, final double[][] act, final double eps) {
        Assert.assertEquals(exp.length, act.length);
        for (int k = 0; k < exp.length; k++) {
            Assert.assertEquals(exp[k].length, act[k].length);
            for (int j = 0; j < 1; j++) {
                Assert.assertEquals(exp[k][j], act[k][j], eps);
            }
        }
    }

}
