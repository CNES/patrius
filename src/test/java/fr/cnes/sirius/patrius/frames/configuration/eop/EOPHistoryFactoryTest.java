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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
 * VERSION:4.5:FA:FA-2244:27/05/2020:Evolution de la prise en compte des fichiers EOP IERS
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:144:04/11/2013:CHanged dt to UT1-TAI in EOPEntry
 * VERSION::FA:306:12/11/2014:coverage
 * END-HISTORY
 */
/*
 */
package fr.cnes.sirius.patrius.frames.configuration.eop;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class EOPHistoryFactoryTest {

    @Test
    public void testGetEOP2000HistoryEOPInterpolatorsEOP2000HistoryLoader()
                                                                           throws PatriusException,
                                                                           IllegalArgumentException {
        Utils.setDataRoot("regular-data");
        final EOPC04FilesLoader loader = new EOPC04FilesLoader("eopc04_IAU2000.05");
        EOP2000History history = EOPHistoryFactory.getEOP2000History(EOPInterpolators.LINEAR, loader);

        // changed threshold to 1e-14, because as of FT 144, stored value is UT1-TAI instead of UT1-UTC
        assertEquals(-0.5067208, history.getUT1MinusUTC(new AbsoluteDate(2005, 1, 5, TimeScalesFactory.getUTC())),
            Precision.DOUBLE_COMPARISON_EPSILON);

        history = EOPHistoryFactory.getEOP2000History(EOPInterpolators.LAGRANGE4, loader);
        history.getUT1MinusTAI(new AbsoluteDate(2005, 1, 5, TimeScalesFactory.getUTC()));
    }

}
