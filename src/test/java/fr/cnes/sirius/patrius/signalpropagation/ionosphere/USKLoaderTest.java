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
 * @history created 04/12/12
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2929:15/11/2021:[PATRIUS] Harmonisation des modeles de troposphere 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.signalpropagation.ionosphere;

import java.io.IOException;
import java.text.ParseException;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit tests for USKLoader.
 * 
 * @author cardosop
 * 
 * @version $Id$
 * 
 * @since 1.3
 * 
 */
public class USKLoaderTest {

    /** USK file name. */
    private static final String USK_FILENAME = "NEWUSK";

    /** Features description. */
    public enum features {
        /**
         * @featureTitle USK file loader
         * 
         * @featureDescription USK file loader
         * 
         * @coveredRequirements DV-MES_FILT_450
         */
        USK_FILE_LOADER
    }

    /**
     * Setup.
     */
    @BeforeClass
    public static void setUp() {
        Utils.setDataRoot("bent");
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#USK_FILE_LOADER}
     * 
     * @testedMethod {@link USKLoader#getData(AbsoluteDate, double)}
     * 
     * @description this unit test checks that the getData() method works as expected.
     * 
     * @input misc
     * 
     * @output misc
     * 
     * @testPassCriteria values produced match the reference file
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     * 
     * @throws PatriusException
     *         should not happen
     * @throws ParseException
     *         should not happen
     * @throws IOException
     *         should not happen
     */
    @Test
    public void testGetData() throws PatriusException, IOException, ParseException {
        // USK
        final USKProvider uskProv = new USKLoader(USK_FILENAME);

        // reference data at a few sample datapoints in the USK file.
        final String[] dates = { "1986-06-01", "2011-10-30",
            "1998-01-15", "2005-12-15",
            "2007-06-30", "2008-04-30" };
        // Data samples for the arrays.
        // Arbitrary picked at [2] or [2][2] indexes.
        final int[] valuesIuf = { 53, 53,
            53, 53,
            53, 53 };
        final double[] valuesUf = { -1.5769731769958213, -3.019451200000006,
            0.4736195900000002, 0.35436552972809565,
            -1.5769731769958213, -2.9717490999999967 };
        final int[] valuesIum = { 34, 34,
            34, 34,
            34, 34 };
        final double[] valuesUm = { -0.010671041939834924, -0.17962436000000004,
            -0.10835456999999993, -0.10094847577079014,
            -0.010671041939834924, -0.04470273499999999 };
        final double bogusR12 = 1.e-16;
        for (int i = 0; i < dates.length; i++) {
            final AbsoluteDate dat = new AbsoluteDate(dates[i],
                TimeScalesFactory.getTT());
            final USKData val = uskProv.getData(dat, bogusR12);
            Assert.assertEquals(valuesIuf[i], val.getIuf()[2], Precision.EPSILON);
            Assert.assertEquals(valuesUf[i], val.getUf()[2][2], Precision.EPSILON);
            Assert.assertEquals(valuesIum[i], val.getIum()[2], Precision.EPSILON);
            Assert.assertEquals(valuesUm[i], val.getUm()[2][2], Precision.EPSILON);
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#USK_FILE_LOADER}
     * 
     * @testedMethod {@link USKLoader#getData(AbsoluteDate, double)}
     * 
     * @description this unit test checks that the getR12() method works as expected with out-of-range parameters.
     * 
     * @input misc out-of-range parameters
     * 
     * @output misc
     * 
     * @testPassCriteria IllegalArgumentException instances raised
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     * 
     * @throws PatriusException
     *         should not happen
     * @throws ParseException
     *         should not happen
     * @throws IOException
     *         should not happen
     */
    @Test
    public void testGetUSKErrors() throws PatriusException, IOException, ParseException {
        boolean expected = false;
        try {
            // USK
            final USKProvider uskProv = new USKLoader("wrongname");
            final AbsoluteDate dat = new AbsoluteDate("2005-11-15",
                TimeScalesFactory.getTT());
            final USKData data = uskProv.getData(dat, 1e16);
            Assert.fail("Should be no object " + data + "here at all...");
        } catch (final PatriusException e) {
            expected = true;
        }
        Assert.assertTrue(expected);
        expected = false;
        try {
            // USK
            final USKProvider uskProv = new USKLoader("emptyfile");
            final AbsoluteDate dat = new AbsoluteDate("2005-11-15",
                TimeScalesFactory.getTT());
            final USKData data = uskProv.getData(dat, 1e16);
            Assert.fail("Should be no object " + data + "here at all...");
        } catch (final PatriusException e) {
            expected = true;
        }
        Assert.assertTrue(expected);
    }
    
    // Serialization tested through the BentModelTest#testSerialization() test
}
