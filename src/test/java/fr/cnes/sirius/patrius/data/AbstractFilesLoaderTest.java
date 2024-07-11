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
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.data;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.configuration.eop.EOPHistory;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.time.TimeStamped;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public abstract class AbstractFilesLoaderTest {

    protected void setRoot(final String directoryName) throws PatriusException {
        Utils.setDataRoot(directoryName);
    }

    protected int getMaxGap(final EOPHistory history) {
        double maxGap = 0;
        TimeStamped previous = null;
        for (final TimeStamped current : history) {
            if (previous != null) {
                maxGap = MathLib.max(maxGap, current.getDate().durationFrom(previous.getDate()));
            }
            previous = current;
        }
        return (int) MathLib.round(maxGap / Constants.JULIAN_DAY);
    }

}
