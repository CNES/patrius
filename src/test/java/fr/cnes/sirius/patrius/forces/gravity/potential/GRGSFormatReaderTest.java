/**
 * Copyright 2011-2017 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim5-c1.txt", true));
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

}
