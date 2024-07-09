/**
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
 * @history created 15/11/2017
 * 
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:1303:15/11/2017: Problem for high order/degree for denormalization
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 * 
 */
package fr.cnes.sirius.patrius.forces.gravity.potential;

import java.io.IOException;
import java.text.ParseException;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class EGMFormatReaderTest {

    @Test
    public void testRead() throws IOException, ParseException, PatriusException {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new EGMFormatReader("egm96_to5.ascii", false));
        final PotentialCoefficientsProvider provider = GravityFieldFactory.getPotentialProvider();
        final double[][] C = provider.getC(5, 5, true);
        final double[][] S = provider.getS(5, 5, true);
        Assert.assertEquals(0.957254173792E-06, C[3][0], 0);
        Assert.assertEquals(0.174971983203E-06, C[5][5], 0);
        Assert.assertEquals(0, S[4][0], 0);
        Assert.assertEquals(0.308853169333E-06, S[4][4], 0);

        final double[][] UC = provider.getC(5, 5, false);
        double a = (-0.295301647654E-06);
        double b = MathLib.sqrt((2. * 11.) / 30.);
        b = b * MathLib.sqrt(1. / 28.);
        b = b * MathLib.sqrt(1. / 24.);
        b = b * MathLib.sqrt(1. / 18.);
        double result = a * b;

        Assert.assertEquals(result, UC[5][4], 0);

        a = -0.188560802735E-06;
        b = MathLib.sqrt((2. * 9.) / 20.);
        b = b * MathLib.sqrt(1. / 18.);
        b = b * MathLib.sqrt(1. / 14.);
        b = b * MathLib.sqrt(1. / 8.);
        result = a * b;
        Assert.assertEquals(result, UC[4][4], 0);

        Assert.assertEquals(1.0826266835531513e-3, provider.getJ(false, 2)[2], 0);

    }

    @Test(expected = PatriusException.class)
    public void testCorruptedFile1() throws IOException, ParseException, PatriusException {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new EGMFormatReader("egm96_to5.corrupted-1", false));
        GravityFieldFactory.getPotentialProvider();
    }

    @Test(expected = PatriusException.class)
    public void testCorruptedFile2() throws IOException, ParseException, PatriusException {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new EGMFormatReader("egm96_to5.corrupted-2", false));
        GravityFieldFactory.getPotentialProvider();
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testCorruptedFile3() throws IOException, ParseException, PatriusException {

        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new EGMFormatReader("egm96_to5.corrupted-3", true));
        GravityFieldFactory.getPotentialProvider();
    }

}
