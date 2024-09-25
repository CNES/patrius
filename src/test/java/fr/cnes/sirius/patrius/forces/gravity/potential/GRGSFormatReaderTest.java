/**
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
 * HISTORY
 * VERSION:4.13:DM:DM-34:08/12/2023:[PATRIUS] Lecture des sigmas du modele de gravite
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity.potential;

import java.io.IOException;
import java.text.ParseException;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class GRGSFormatReaderTest {

    @Test
    public void testAdditionalColumn() throws IOException, ParseException, PatriusException {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim5-c1.txt", true,false));
        final PotentialCoefficientsProvider provider = GravityFieldFactory.getPotentialProvider();
        final double[][] C = provider.getC(5, 5, true);
        final double[][] S = provider.getS(5, 5, true);

        Assert.assertEquals(0.95857491635129E-06, C[3][0], 0);
        Assert.assertEquals(0.17481512311600E-06, C[5][5], 0);
        Assert.assertEquals(0, S[4][0], 0);
        Assert.assertEquals(0.30882755318300E-06, S[4][4], 0);
        Assert.assertEquals(0.3986004415E+15, provider.getMu(), 0);
        Assert.assertEquals(0.6378136460E+07, provider.getAe(), 0);

    }

    @Test
    public void testRegular05c() throws IOException, ParseException, PatriusException {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim5_C1.dat", true));
        final PotentialCoefficientsProvider provider = GravityFieldFactory.getPotentialProvider();
        final double[][] C = provider.getC(5, 5, true);
        final double[][] S = provider.getS(5, 5, true);

        Assert.assertEquals(0.95857491635129E-06, C[3][0], 0);
        Assert.assertEquals(0.17481512311600E-06, C[5][5], 0);
        Assert.assertEquals(0, S[4][0], 0);
        Assert.assertEquals(0.30882755318300E-06, S[4][4], 0);
        Assert.assertEquals(0.3986004415E+15, provider.getMu(), 0);
        Assert.assertEquals(0.6378136460E+07, provider.getAe(), 0);

    }

    @Test(expected = PatriusException.class)
    public void testCorruptedFile1() throws IOException, ParseException, PatriusException {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim5_corrupted1.dat", false));
        GravityFieldFactory.getPotentialProvider();
    }

    @Test(expected = PatriusException.class)
    public void testCorruptedFile2() throws IOException, ParseException, PatriusException {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim5_corrupted2.dat", false));
        GravityFieldFactory.getPotentialProvider();
    }

    @Test(expected = PatriusException.class)
    public void testCorruptedFile3() throws IOException, ParseException, PatriusException {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim5_corrupted3.dat", false));
        GravityFieldFactory.getPotentialProvider();
    }

    @Test
    public void testReadingSigmas() throws IOException, ParseException, PatriusException {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("jgmro_120d_sha_4x4.tab", true, true));
        final PotentialCoefficientsProvider provider = GravityFieldFactory.getPotentialProvider();

        final double[][] C = provider.getC(4, 4, true);
        final double[][] S = provider.getS(4, 4, true);

        final double[][] SigmaC = provider.getSigmaC(4, 4, true);
        final double[][] SigmaS = provider.getSigmaS(4, 4, true);
        
        final double[][] SigmaCUn = provider.getSigmaC(4, 4, false);
        final double[][] SigmaSUn = provider.getSigmaS(4, 4, false);

        Assert.assertEquals(-0.1189701503730000E-04, C[3][0], 0);
        Assert.assertEquals(0.5129095830134000E-05, C[4][0], 0);
        Assert.assertEquals(0, S[4][0], 0);
        Assert.assertEquals(-0.1287305697738000E-04, S[4][4], 0);
        Assert.assertEquals(0.6765599578359000E-10, SigmaC[4][1], 0);
        Assert.assertEquals(0.1010005343691000E-09, SigmaC[4][0], 0);
        Assert.assertEquals(0, SigmaS[4][0], 0);
        Assert.assertEquals(0.2249475825705000E-10, SigmaS[4][4], 0);
        Assert.assertEquals(0.4282837581575610E+14, provider.getMu(), 0);
        Assert.assertEquals(0.33960000000000E+07, provider.getAe(), 0);
        // Test unnormalized sigmas
        Assert.assertEquals(6.674221667314548E-13, SigmaCUn[4][4], 0);
        Assert.assertEquals(4.752885162508809E-13, SigmaSUn[4][4], 0);
    }
    
    @Test(expected = PatriusException.class)
    public void testReadingSigmasCorrupted() throws IOException, ParseException, PatriusException {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("jgmro_120d_sha_4x4_corrupted.tab", true, true));
        final PotentialCoefficientsProvider provider = GravityFieldFactory.getPotentialProvider();
    }
}
