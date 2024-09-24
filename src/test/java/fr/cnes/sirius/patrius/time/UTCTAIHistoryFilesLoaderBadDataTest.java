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
 * @history Creation 02/08/11
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.time;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @description <p>
 *              - test class for the UTC-TAI History Files Loader : cases with bad input files.
 *              </p>
 * 
 * @see Class UTCTAIHistoryFilesLoader
 * 
 * @author Thomas TRAPIER
 * 
 * @version $Id: UTCTAIHistoryFilesLoaderBadDataTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.0
 * 
 */
public class UTCTAIHistoryFilesLoaderBadDataTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle UTC-TAI history files with bad data
         * 
         * @featureDescription tests of UTC time scale creation with different
         *                     types of bad data in UTC-TAI history files
         * 
         * @coveredRequirements DV-DATES_70
         */
        UTC_TAI_HISTORY_BAD_DATA
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#UTC_TAI_HISTORY_BAD_DATA}
     * 
     * @testedMethod {@link UTCTAIHistoryFilesLoader#loadData}
     * 
     * @description UTC-TAI History Files Loading with an empty file
     * 
     * @input the test/ressources/empty-data/UTC-TAI.history
     * 
     * @output none
     * 
     * @testPassCriteria an OrekitException "NO_ENTRIES_IN_IERS_UTC_TAI_HISTORY_FILE"
     *                   must be thrown
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void emptyDataTest() {
        try {
            Utils.setDataRoot("empty-data");
            TimeScalesFactory.getUTC().offsetFromTAI(AbsoluteDate.J2000_EPOCH);
            Assert.fail();
        } catch (final PatriusException e) {
            // expected !
        }

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#UTC_TAI_HISTORY_BAD_DATA}
     * 
     * @testedMethod {@link UTCTAIHistoryFilesLoader#loadData}
     * 
     * @description UTC-TAI History Files Loading with a file containing a bad line
     * 
     * @input the test/ressources/unreadable-line-data/UTC-TAI.history
     * 
     * @output none
     * 
     * @testPassCriteria an OrekitException "UNABLE_TO_PARSE_LINE_IN_FILE"
     *                   must be thrown
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void badLineDataTest() {
        try {
            Utils.setDataRoot("unreadable-line-data");
            TimeScalesFactory.getUTC().offsetFromTAI(AbsoluteDate.J2000_EPOCH);
            Assert.fail();
        } catch (final PatriusException e) {
            // expected !
        }

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#UTC_TAI_HISTORY_BAD_DATA}
     * 
     * @testedMethod {@link UTCTAIHistoryFilesLoader#loadData}
     * 
     * @description UTC-TAI History Files Loading with a file with an end
     *              line in the middle of the file
     * 
     * @input the test/ressources/bad-end-line-data/UTC-TAI.history
     * 
     * @output none
     * 
     * @testPassCriteria an OrekitException "UNEXPECTED_DATA_AFTER_LINE_IN_FILE"
     *                   must be thrown
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void badEndDataTest() {
        try {
            Utils.setDataRoot("bad-end-data");
            TimeScalesFactory.getUTC().offsetFromTAI(AbsoluteDate.J2000_EPOCH);
            Assert.fail();
        } catch (final PatriusException e) {
            // expected !
        }

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#UTC_TAI_HISTORY_BAD_DATA}
     * 
     * @testedMethod {@link UTCTAIHistoryFilesLoader#loadData}
     * 
     * @description UTC-TAI History Files Loading with a file with data in a
     *              non chronological order
     * 
     * @input the test/ressources/non-chronological-data/UTC-TAI.history
     * 
     * @output none
     * 
     * @testPassCriteria an OrekitException "NON_CHRONOLOGICAL_DATES_IN_FILE"
     *                   must be thrown
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void nonChronologicalDataTest() {
        try {
            Utils.setDataRoot("non-chronological-data");
            TimeScalesFactory.getUTC().offsetFromTAI(AbsoluteDate.J2000_EPOCH);
            Assert.fail();
        } catch (final PatriusException e) {
            // expected !
        }
    }
}
