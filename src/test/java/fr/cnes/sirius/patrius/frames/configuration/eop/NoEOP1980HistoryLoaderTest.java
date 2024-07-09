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
 */
/**
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:206:10/03/2014:Created a no EOP1980 history loader
 * VERSION::FA:306:12/11/2014:coverage
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.eop;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class NoEOP1980HistoryLoaderTest {

    final double eps = 1e-14;

    /**
     * This test makes sure that the recovered EOP UT1-TAI data is zero
     * 
     * @throws PatriusException
     * @throws ParseException
     * @throws IOException
     */
    @Test
    public void testNoEOPEqualZero() throws PatriusException, IOException, ParseException {
        // no files
        Utils.setDataRoot("no-data");

        // no eops
        final NoEOP1980HistoryLoader loader = new NoEOP1980HistoryLoader();
        EOPHistoryFactory.addEOP1980HistoryLoader(loader);
        Assert.assertTrue(loader.stillAcceptsData());
        // The following lines have been added just for coverage of the loadData method:
        loader.loadData(new InputStream(){

            @Override
            public int read() throws IOException {
                return 0;
            }
        }, "Mock");

        // history
        final EOP1980History history = EOPHistoryFactory.getEOP1980History();

        // dates
        final AbsoluteDate start = history.getStartDate();
        final AbsoluteDate end = history.getEndDate();

        // control variable
        AbsoluteDate date = start;

        // control loop
        while (date.compareTo(end) < 0) {
            Assert.assertEquals(0, history.getUT1MinusTAI(date), this.eps);
            date = date.shiftedBy(86400 * 10);
        }

    }

}
