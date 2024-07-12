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
 * @history created 20/11/12
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Unit tests for R12Loader.
 * 
 * @author cardosop
 * 
 * @version $Id$
 * 
 * @since 1.3
 * 
 */
public class R12LoaderTest {

    /** CCIR12 file name. */
    private static final String CCIR12_FILENAME = "CCIR12";

    /** Features description. */
    public enum features {
        /**
         * @featureTitle CCIR12 file loader
         * 
         * @featureDescription CCIR12 file loader
         * 
         * @coveredRequirements DV-MES_FILT_450
         */
        CCIR12_FILE_LOADER
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
     * @testedFeature {@link features#CCIR12_FILE_LOADER}
     * 
     * @testedMethod {@link R12Loader#getR12(AbsoluteDate)}
     * 
     * @description this unit test checks that the getR12() method works as expected.
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
    public void testGetR12() throws PatriusException, IOException, ParseException {
        // R12
        final R12Provider r12Prov = new R12Loader(CCIR12_FILENAME);
        final List<Double> rez = new ArrayList<>();

        // Full range of the reference file
        boolean goOn = true;
        int curYear = 1986;
        int curMonth = 6;
        final int lastYear = 2011;
        final int lastMonth = 11;
        while (goOn) {
            if (curMonth == lastMonth && curYear == lastYear) {
                goOn = false;
            }
            final DateComponents dc = new DateComponents(curYear, curMonth, 1);
            final AbsoluteDate date = new AbsoluteDate(dc, TimeScalesFactory.getTT());
            final double r12 = r12Prov.getR12(date);
            rez.add(r12);
            curYear = curYear + (curMonth / 12);
            curMonth = (curMonth % 12) + 1;
        }

        // reference data at a few sample datapoints in the CCIR12 file.
        final String[] dates = { "1986-06-01", "2011-10-30",
            "1998-01-15", "2005-12-15",
            "2007-06-30", "2008-04-30" };
        final double[] values = { 14., 54.,
            43.7, 23.,
            7.35, 3.4 };
        for (int i = 0; i < dates.length; i++) {
            final AbsoluteDate dat = new AbsoluteDate(dates[i],
                TimeScalesFactory.getTT());
            final double val = r12Prov.getR12(dat);
            Assert.assertEquals(values[i], val, Precision.EPSILON);
        }

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#CCIR12_FILE_LOADER}
     * 
     * @testedMethod {@link R12Loader#getR12(AbsoluteDate)}
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
    public void testGetR12Errors() throws PatriusException, IOException, ParseException {
        // R12
        final R12Provider r12Prov = new R12Loader(CCIR12_FILENAME);

        // Date outside of the date range (before)
        boolean asExpected = false;
        try {
            final DateComponents dc = new DateComponents(1986, 05, 31);
            final AbsoluteDate date = new AbsoluteDate(dc, TimeScalesFactory.getTT());
            final double r12 = r12Prov.getR12(date);
            // We should not reach here
            Assert.fail("" + r12);
        } catch (final IllegalArgumentException e) {
            if (e.getLocalizedMessage().equals(
                PatriusMessages.PDB_IONO_DATE_OUT_OF_FILE
                    .getLocalizedString(Locale.getDefault()))) {
                asExpected = true;
            }
        }
        Assert.assertTrue(asExpected);

        // Date outside of the date range (after)
        asExpected = false;
        try {
            final DateComponents dc = new DateComponents(2011, 11, 02);
            final AbsoluteDate date = new AbsoluteDate(dc, TimeScalesFactory.getTT());
            final double r12 = r12Prov.getR12(date);
            // We should not reach here
            Assert.fail("" + r12);
        } catch (final IllegalArgumentException e) {
            if (e.getLocalizedMessage().equals(
                PatriusMessages.PDB_IONO_DATE_OUT_OF_FILE
                    .getLocalizedString(Locale.getDefault()))) {
                asExpected = true;
            }
        }
        Assert.assertTrue(asExpected);
    }

    // Serialization tested through the BentModelTest#testSerialization() test
}
