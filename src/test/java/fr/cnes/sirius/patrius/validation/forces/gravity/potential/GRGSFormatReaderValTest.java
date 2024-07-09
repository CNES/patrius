/**
 * 
 * Copyright 2011-2017 CNES
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 */
package fr.cnes.sirius.patrius.validation.forces.gravity.potential;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;

import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.forces.gravity.potential.GRGSFormatReader;
import fr.cnes.sirius.patrius.forces.gravity.potential.GravityFieldFactory;
import fr.cnes.sirius.patrius.forces.gravity.potential.PotentialCoefficientsProvider;
import fr.cnes.sirius.patrius.tools.validationTool.Validate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Validation tests for <code>GRGSFormatReader</code>.
 * IMPORTANT : this is a validation test, NOT MEANT to be merged back to Orekit.
 * This applies to the file GRGS_EIGEN_GL04S.txt, also.
 * 
 * @see GRGSFormatReader
 * 
 * @author cardosop
 * 
 * @version $Id: GRGSFormatReaderValTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.0
 * 
 */
public class GRGSFormatReaderValTest {

    /** String constant. */
    private static final String S2 = "S[";
    /** String constant. */
    private static final String ENDACCOL = "]";
    /** String constant. */
    private static final String ACCOLS = "][";
    /** String constant. */
    private static final String C2 = "C[";

    /** Features description. */
    public enum features {
        /**
         * @featureTitle GRGS file format compatible
         * 
         * @featureDescription Orekit can read the gravity potential GRGS file format
         * 
         * @coveredRequirements DV-MOD_180
         */
        GRAVITY_POT_GRGS_FILE_COMPAT
    }

    /**
     * OREKIT resource directory.
     */
    private static final String POTDIR = "potentialCNES";
    /**
     * Max degree in the file.
     */
    private static final int FDEG = 150;
    /**
     * Max order in the file.
     */
    private static final int FORDER = 150;

    /** Perfect epsilon. */
    private static final double PERFEPS = 0.;

    /**
     * @testType TVT
     * 
     * @testedFeature {@link features#GRAVITY_POT_GRGS_FILE_COMPAT}
     * 
     * @testedMethod {@link GRGSFormatReader#loadData(java.io.InputStream, String)}
     * 
     * @description validation test : reading a full CNES GRGS file
     * 
     * @input a full-featured CNES GRGS file
     * 
     * @output a properly loaded GRGSFormatReader instance
     * 
     * @testPassCriteria loading successful and data compliant
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     * 
     * @throws PatriusException
     *         should not happen
     * @throws IOException
     *         should not happen
     * @throws ParseException
     *         should not happen
     * @throws URISyntaxException
     *         should not happen
     */
    @Test
    public final void testLoadData() throws IOException, ParseException, PatriusException, URISyntaxException {
        Utils.setDataRoot(POTDIR);
        // Validate instance
        final Validate val = new Validate(this.getClass());
        // Missing coefficients are allowed
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("GRGS_EIGEN_GL04S.txt", true));
        final PotentialCoefficientsProvider provider = GravityFieldFactory.getPotentialProvider();
        // Here, we get the data as extracted from the file.
        final double[][] C = provider.getC(FDEG, FORDER, true);
        final double[][] S = provider.getS(FDEG, FORDER, true);

        // The file coefficients are normalized so the data should be the same as in the file.
        // We do not test all the data, only a small subsample from several points in the file
        // chosen randomly
        final double expectedMu = 0.39860044150000E+15;
        val.assertEquals(provider.getMu(), expectedMu, PERFEPS, expectedMu, PERFEPS, "Mu");
        final double expectedAe = 0.63781364600000E+07;
        val.assertEquals(provider.getAe(), expectedAe, PERFEPS, expectedAe, PERFEPS, "Ae");

        final double[] expectedData = { -0.48416526482172E-03, 0. };
        int to = 0;
        int td = 2;
        val.assertEquals(C[td][to], expectedData[0], PERFEPS,
            expectedData[0], PERFEPS, C2 + td + ACCOLS + to + ENDACCOL);
        val.assertEquals(S[td][to], expectedData[1], PERFEPS,
            expectedData[1], PERFEPS, S2 + td + ACCOLS + to + ENDACCOL);
        final double[] expectedData2 = { -0.27865446367523E-09, 0.48385196009924E-09 };

        to = FORDER;
        td = FDEG;
        val.assertEquals(C[td][to], expectedData2[0], PERFEPS,
            expectedData2[0], PERFEPS, C2 + td + ACCOLS + to + ENDACCOL);
        val.assertEquals(S[td][to], expectedData2[1], PERFEPS,
            expectedData2[1], PERFEPS, S2 + td + ACCOLS + to + ENDACCOL);

        final double[] expectedData3 = { 0.25087010824253E-09, 0.14000253956621E-08 };
        td = 85;
        to = 38;
        val.assertEquals(C[td][to], expectedData3[0], PERFEPS,
            expectedData3[0], PERFEPS, C2 + td + ACCOLS + to + ENDACCOL);
        val.assertEquals(S[td][to], expectedData3[1], PERFEPS,
            expectedData3[1], PERFEPS, S2 + td + ACCOLS + to + ENDACCOL);

        final double[] expectedData4 = { -0.28246605488554E-09, 0.20832288079097E-08 };
        td = 110;
        to = 20;
        val.assertEquals(C[td][to], expectedData4[0], PERFEPS,
            expectedData4[0], PERFEPS, C2 + td + ACCOLS + to + ENDACCOL);
        val.assertEquals(S[td][to], expectedData4[1], PERFEPS,
            expectedData4[1], PERFEPS, S2 + td + ACCOLS + to + ENDACCOL);

        final double[] expectedData5 = { -0.10077239155959E-08, -0.44301185266804E-09 };
        td = 116;
        to = 84;
        val.assertEquals(C[td][to], expectedData5[0], PERFEPS,
            expectedData5[0], PERFEPS, C2 + td + ACCOLS + to + ENDACCOL);
        val.assertEquals(S[td][to], expectedData5[1], PERFEPS,
            expectedData5[1], PERFEPS, S2 + td + ACCOLS + to + ENDACCOL);

        final double[] expectedData6 = { 0.51243252257696E-09, -0.49583011923570E-09 };
        td = 142;
        to = 15;
        val.assertEquals(C[td][to], expectedData6[0], PERFEPS,
            expectedData6[0], PERFEPS, C2 + td + ACCOLS + to + ENDACCOL);
        val.assertEquals(S[td][to], expectedData6[1], PERFEPS,
            expectedData6[1], PERFEPS, S2 + td + ACCOLS + to + ENDACCOL);

        final double[] expectedData7 = { 0.25095834522485E-08, 0.99011004336867E-09 };
        td = 83;
        to = 9;
        val.assertEquals(C[td][to], expectedData7[0], PERFEPS,
            expectedData7[0], PERFEPS, C2 + td + ACCOLS + to + ENDACCOL);
        val.assertEquals(S[td][to], expectedData7[1], PERFEPS,
            expectedData7[1], PERFEPS, S2 + td + ACCOLS + to + ENDACCOL);

        val.produceLog();
    }

}
