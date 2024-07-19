/**
 * Copyright 2011-2022 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * @history creation 10/03/2014
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:206:10/03/2014:Created a no EOP1980 history loader
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.eop;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;

import fr.cnes.sirius.patrius.frames.configuration.eop.EOPEntry.DtType;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScale;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * NoEOP2000History.
 * 
 * In order to use this class, the user must use the loader in the EOPHistoryFactory :
 * 
 * <pre>
 * final EOP1980HistoryLoader loader = new NoEOP1980HistoryLoader();<br>
 * EOPHistoryFactory.addEOP1980HistoryLoader(loader);
 * </pre>
 * 
 * @concurrency not thread-safe
 * @concurrency.comment uses internal mutable attributes
 * 
 * @author Rami Houdroge
 * @since 2.2
 * @version $Id: NoEOP1980HistoryLoader.java 18073 2017-10-02 16:48:07Z bignon $
 * 
 */
public class NoEOP1980HistoryLoader implements EOP1980HistoryLoader {

    /** 86400 */
    private static final double C_86400 = 86400;

    /** {@inheritDoc} */
    @Override
    public boolean stillAcceptsData() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public void loadData(final InputStream input, final String name) throws IOException,
        ParseException, PatriusException {
        // Nothing to do
    }

    /** History with zero orientation. {@inheritDoc} */
    @Override
    public void fillHistory(final EOP1980History history) throws PatriusException {

        // TAI scale
        final TimeScale tai = TimeScalesFactory.getTAI();

        // history valid between 1950 and 2200
        final AbsoluteDate dateMin = new AbsoluteDate(1950, 1, 1, tai);
        final AbsoluteDate dateMax = new AbsoluteDate(2200, 1, 1, tai);

        // control variable
        AbsoluteDate date = dateMin;

        // one data point per day
        while (date.compareTo(dateMax) < 0) {
            history.addEntry(new EOP1980Entry(date, 0., 0., 0., 0., 0., 0., DtType.UT1_TAI));
            date = date.shiftedBy(C_86400);
        }

    }

}
