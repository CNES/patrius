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
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.time;

import java.util.SortedMap;

import fr.cnes.sirius.patrius.data.DataLoader;

/**
 * Interface for loading UTC-TAI offsets data files.
 * 
 * @author Pascal Parraud
 */
public interface UTCTAILoader extends DataLoader {

    /**
     * Load UTC-TAI offsets entries.
     * <p>
     * Only the integer offsets used since 1972-01-01 are loaded here, the linear offsets used between 1961-01-01 and
     * 1971-12-31 are hard-coded in the {@link UTCScale UTCScale} class itself.
     * </p>
     * 
     * @return sorted UTC-TAI offsets entries (may be empty)
     */
    SortedMap<DateComponents, Integer> loadTimeSteps();

    /**
     * Get the regular expression for supported UTC-TAI offsets files names.
     * 
     * @return regular expression for supported UTC-TAI offsets files names
     */
    String getSupportedNames();

}
