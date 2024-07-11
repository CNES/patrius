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
 * @history created 14/03/12
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:---:11/04/2014:Quality assurance
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events.postprocessing;

import java.util.Set;

import fr.cnes.sirius.patrius.events.CodedEvent;
import fr.cnes.sirius.patrius.events.CodedEventsList;
import fr.cnes.sirius.patrius.events.PhenomenaList;
import fr.cnes.sirius.patrius.events.Phenomenon;

/**
 * @description <p>
 *              Filter that removes or keeps only the N &times; k occurrences of a specific element type (code) in a
 *              given timeline.
 *              </p>
 * 
 * @concurrency immutable
 * 
 * @author Julie Anton
 * 
 * @version $Id$
 * 
 * @since 1.1
 * 
 */
public final class OccurrenceFilter implements PostProcessing {

    /** Filter on the nxk. */
    private final int n;
    /** Code of the element that has to be filtered. */
    private final String code;
    /** True if all of those elements have to be removed. */
    private final boolean remove;

    /**
     * Constructor
     * 
     * @description build an instance of OccurrenceFilter from a code
     * 
     * @param elementCode
     *        : code of the elements that has to be removed / kept
     * @param k
     *        : occurrence sampling
     * @param removeAll
     *        : true if the filter consists in removing the specified elements, false if the filter consisits in
     *        keeping only the specified elements.
     */
    public OccurrenceFilter(final String elementCode, final int k, final boolean removeAll) {
        this.code = elementCode;
        this.n = k;
        this.remove = removeAll;
    }

    /** {@inheritDoc} */
    @Override
    public void applyTo(final Timeline list) {
        // lists
        final CodedEventsList events = list.getCodedEvents();
        final PhenomenaList phenomena = list.getPhenomena();

        // containers
        final Set<CodedEvent> eventsSet = events.getEvents(this.code, null, null);
        final CodedEvent[] eventsList = eventsSet.toArray(new CodedEvent[eventsSet.size()]);
        final Set<Phenomenon> phenomenaSet = phenomena.getPhenomena(this.code, null, null);
        final Phenomenon[] phenomenaList = phenomenaSet.toArray(new Phenomenon[phenomenaSet.size()]);

        // remove events or phenomena
        if (this.remove) {
            for (int i = this.n - 1; i < eventsList.length; i += this.n) {
                // remove events
                list.removeCodedEvent(eventsList[i]);
            }
            for (int i = this.n - 1; i < phenomenaList.length; i += this.n) {
                // remove phenomenon
                list.removePhenomenon(phenomenaList[i]);
            }
        } else {
            // remove events or phenomena
            for (int i = 0; i < eventsList.length; i++) {
                if (i != (i + 1) / this.n * this.n - 1) {
                    // remove coded event
                    list.removeCodedEvent(eventsList[i]);
                }
            }
            for (int i = 0; i < phenomenaList.length; i++) {
                if (i != (i + 1) / this.n * this.n - 1) {
                    // remove coded phenomenon
                    list.removePhenomenon(phenomenaList[i]);
                }
            }
        }
    }
}
