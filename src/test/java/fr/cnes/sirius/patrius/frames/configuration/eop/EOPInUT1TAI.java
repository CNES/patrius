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
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:144:04/11/2013:Changed UT1-UTC to UT1-TAI in EOPEntry, EOP1980Entry and EOP2000Entry
 * VERSION::FA:1301:06/09/2017:Generalized EOP history
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.eop;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationBuilder;
import fr.cnes.sirius.patrius.frames.configuration.eop.EOPEntry.DtType;
import fr.cnes.sirius.patrius.frames.transformations.GTODProvider;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * 
 * Test class to make sure we can calculate GMST without UTC-TAI data.
 * 
 * The UT1-TAI data used here is
 * 
 * @author Rami Houdroge
 * @since 2.1
 * @version $$
 */
public class EOPInUT1TAI {

    @Test
    public void test() throws PatriusException {

        Utils.setDataRoot("no-data");

        /*
         * EOP 1980 for VEIS
         */
        // create a user specific EOP loader
        final EOP1980HistoryLoader loader = new EOP1980HistoryLoader(){

            @Override
            public boolean stillAcceptsData() {
                return false;
            }

            @Override
            public void loadData(final InputStream input, final String name)
                                                                            throws IOException, ParseException,
                                                                            PatriusException {
                // this method wont be called by the DataProvidersManager because we donc need to feed it with data
            }

            @Override
            public void fillHistory(final EOP1980History history) throws PatriusException {
                // it feeds the history itself directly

                final ArrayList<EOP1980Entry> entries = new ArrayList<>();

                // fill it with UT1-TAI data
                for (int i = -30; i < 30; i++) {
                    final AbsoluteDate date = new AbsoluteDate().shiftedBy(i * Constants.JULIAN_DAY);
                    entries.add(new EOP1980Entry(date, i / 1000. + 1, 0., 0, 0, 0, 0, DtType.UT1_TAI));
                }

                EOP1980History.fillHistory(entries, history);
            }
        };

        // give it to the EOP factory
        EOPHistoryFactory.addEOP1980HistoryLoader(loader);

        // use without UTC / TAI file - we dont care about the result
        GTODProvider.getGMST(new AbsoluteDate().shiftedBy(86400));

        /*
         * EOP 2000 for IERS
         */

        // create EOP2000 History
        final EOP2000History history = new EOP2000History(EOPInterpolators.LINEAR);

        // fill it with UT1-TAI data
        final ArrayList<EOP2000Entry> entries = new ArrayList<>();
        for (int i = -30; i < 30; i++) {
            final AbsoluteDate date = new AbsoluteDate().shiftedBy(i * Constants.JULIAN_DAY);
            entries.add(new EOP2000Entry(date, i / 1000. + 1, 0., 0, 0, 0, 0, DtType.UT1_TAI));
        }
        EOP2000History.fillHistory(entries, history);

        // Make a frames configuration out of it and give it to the frames factory
        final FramesConfigurationBuilder builder =
            new FramesConfigurationBuilder(Utils.getIERS2003ConfigurationWOEOP(true));
        builder.setEOPHistory(history);
        FramesFactory.setConfiguration(builder.getConfiguration());

        /*
         * TESTS
         */
        // try for one value
        for (int i = -30; i < 30; i++) {
            final double daysShift = i * 86400;
            final double exp = 1 + i / 1000.;
            final double act = EOPHistoryFactory.getEOP1980History().getUT1MinusTAI(
                new AbsoluteDate().shiftedBy(daysShift));
            Assert.assertEquals(0, (act - exp) / act, Precision.EPSILON);
        }

        // but we make sure that we can't create a UTC timescale!
        try {
            TimeScalesFactory.getUTC();
            Assert.fail();
        } catch (final PatriusException e) {
            // expected!
        }

        // no files for all transformations
        FramesFactory.getVeis1950().getTransformTo(FramesFactory.getITRF(), new AbsoluteDate());
        FramesFactory.getGTOD(true).getTransformTo(FramesFactory.getITRF(), new AbsoluteDate());

        /*
         * GCRF
         * │
         * ┌──────────────────┴──────┬────────────────────┐
         * │ │ Frame bias │
         * │ │ EME2000
         * │ │ │
         * │ │ Precession effects │
         * Bias, Precession and Nutation effects │ │ │
         * with or w/o EOP nutation correction │ MOD MOD (Mean Equator Of Date)
         * │ │ w/o EOP corrections
         * │ ┌─────────┤ Nutation effects ├──────────────────────────────────┐
         * (Celestial Intermediate Reference Frame) CIRF │ │ │ │
         * │ │ TOD TOD (True Equator Of Date) │
         * Earth natural rotation │ │ │ w/o EOP corrections │
         * │ │ │ Sidereal Time │ │
         * │ │ │ │ │
         * (Terrestrial Intermediate Reference Frame) TIRF EOD GTOD GTOD (Greenwich True Of Date) EOD (Mean ecliptic and
         * │ w/o EOP corrections equinox of the epoch)
         * Pole motion │ │
         * │ ├────────────┐
         * │ │ │
         * (International Terrestrial Reference Frame) ITRF ITRF VEIS1950
         * equinox-based
         */
    }
}
