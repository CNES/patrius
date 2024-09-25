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
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:206:10/03/2014:Created a no EOP1980 history loader
 * VERSION::FA:1301:06/09/2017:Generalized EOP history
 * END-HISTORY
 */
/*
 */
package fr.cnes.sirius.patrius.frames.configuration.eop;

import java.util.ArrayList;

import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationBuilder;
import fr.cnes.sirius.patrius.frames.configuration.eop.EOPEntry.DtType;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This test class makes sure no EOP files are used in case the user configured empty loaders.
 */
public class NoEOP1980Test {

    /**
     * This test makes sure that the frame transformations don't use any external files
     * in case the No1980HistoryLodaer is given
     * 
     * @throws PatriusException
     */
    @Test
    public void testNoEOPForFrame() throws PatriusException {
        Utils.setDataRoot("no-data");

        // EOP 1980
        EOPHistoryFactory.addEOP1980HistoryLoader(new NoEOP1980HistoryLoader());

        // EOP 2000
        final EOP2000History history2000 = new EOP2000History(EOPInterpolators.LINEAR);

        // fill it with UT1-TAI data
        final ArrayList<EOP2000Entry> entries = new ArrayList<>();
        for (int i = -30; i < 30; i++) {
            final AbsoluteDate date = new AbsoluteDate().shiftedBy(i * Constants.JULIAN_DAY);
            entries.add(new EOP2000Entry(date, 0., 0., 0, 0, 0, 0, DtType.UT1_TAI));
        }
        EOP2000History.fillHistory(entries, history2000);

        // Make a frames configuration out of it and give it to the frames factory
        final FramesConfigurationBuilder builder =
            new FramesConfigurationBuilder(Utils.getIERS2003ConfigurationWOEOP(true));
        builder.setEOPHistory(history2000);
        FramesFactory.setConfiguration(builder.getConfiguration());

        // no files for all transformations
        FramesFactory.getVeis1950().getTransformTo(FramesFactory.getITRF(), new AbsoluteDate());
        FramesFactory.getGTOD(true).getTransformTo(FramesFactory.getITRF(), new AbsoluteDate());
    }
}
