/**
 * 
 * Copyright 2011-2017 CNES
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 * @history created 13/03/2012
 */
package fr.cnes.sirius.patrius.events.postprocessing;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import fr.cnes.sirius.patrius.events.CodedEvent;
import fr.cnes.sirius.patrius.events.CodedEventsList;
import fr.cnes.sirius.patrius.events.PhenomenaList;
import fr.cnes.sirius.patrius.events.Phenomenon;

/**
 * @description <p>
 *              Filter that removes or keeps only the items of specific types (code) in a given timeline.
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
public final class ElementTypeFilter implements PostProcessing {

    /**
     * List of codes of the elements that have to be filtered.
     */
    private final List<String> code;

    /**
     * True if all of those elements have to be removed.
     */
    private final boolean remove;

    /**
     * Constructor that uses only one code to identify the elements that have to be removed/kept.
     * 
     * @param elementCode
     *        : code of the elements that have to be removed / kept
     * @param removeAll
     *        : true if the filter consists in removing the specified elements, false if the filter consists in
     *        keeping only the specified elements.
     */
    public ElementTypeFilter(final String elementCode, final boolean removeAll) {
        this.code = new ArrayList<String>();
        this.code.add(elementCode);
        this.remove = removeAll;
    }

    /**
     * Constructor that uses a list of codes to identify the elements that have to be removed/kept.
     * 
     * @param listCode
     *        : list of codes of the elements that have to be removed / kept
     * @param removeAll
     *        : true if the filter consists in removing the specified elements, false if the filter consists in
     *        keeping only the specified elements.
     */
    public ElementTypeFilter(final List<String> listCode, final boolean removeAll) {
        this.code = new ArrayList<String>();
        this.code.addAll(listCode);
        this.remove = removeAll;
    }

    /** {@inheritDoc} */
    @Override
    public void applyTo(final Timeline list) {
        final CodedEventsList eventsList = list.getCodedEvents();
        final PhenomenaList phenomenaList = list.getPhenomena();
        final Set<CodedEvent> eventsSet = new TreeSet<CodedEvent>();
        final Set<Phenomenon> phenomenaSet = new TreeSet<Phenomenon>();
        for (final String currentCode : this.code) {
            // creates two lists containing all the events and phenomena of the selected type:
            eventsSet.addAll(eventsList.getEvents(currentCode, null, null));
            phenomenaSet.addAll(phenomenaList.getPhenomena(currentCode, null, null));
        }
        if (this.remove) {
            // removes all the elements of the selected type:
            for (final CodedEvent event : eventsSet) {
                list.removeCodedEvent(event);
            }
            for (final Phenomenon phenomenon : phenomenaSet) {
                list.removePhenomenon(phenomenon);
            }
        } else {
            // keeps only the elements of the selected type:
            this.applyToRemoveFalse(list, eventsList, phenomenaList, eventsSet, phenomenaSet);
        }
    }

    /**
     * Method extracted from applyTo() to decrease cyclomatic complexity.
     * 
     * @param list
     *        see {@link ElementTypeFilter#applyTo(Timeline)}
     * @param eventsList
     *        see {@link ElementTypeFilter#applyTo(Timeline)}
     * @param phenomenaList
     *        see {@link ElementTypeFilter#applyTo(Timeline)}
     * @param eventsSet
     *        see {@link ElementTypeFilter#applyTo(Timeline)}
     * @param phenomenaSet
     *        see {@link ElementTypeFilter#applyTo(Timeline)}
     */
    private void applyToRemoveFalse(final Timeline list, final CodedEventsList eventsList,
                                    final PhenomenaList phenomenaList, final Set<CodedEvent> eventsSet,
                                    final Set<Phenomenon> phenomenaSet) {

        final List<CodedEvent> events = eventsList.getList();
        final List<Phenomenon> phen = phenomenaList.getList();

        if (eventsSet.isEmpty()) {
            // The list of selected events is empty, remove ALL events from the timeline:
            for (final CodedEvent event : events) {
                list.removeOnlyCodedEvent(event);
            }
        } else {
            // The list of selected events is not empty, remove all the other events from the timeline:
            for (final CodedEvent event : events) {
                if (!eventsSet.contains(event)) {
                    list.removeCodedEvent(event);
                }
            }
        }

        if (phenomenaSet.isEmpty()) {
            // The list of selected phenomena is empty, remove ALL phenomena from the timeline:
            for (final Phenomenon phenomenon : phen) {
                list.removePhenomenon(phenomenon);
            }
        } else {
            // The list of selected phenomena is not empty, remove all the other phenomena from the timeline:
            for (final Phenomenon phenomenon : phen) {
                if (!phenomenaSet.contains(phenomenon)) {
                    list.removePhenomenon(phenomenon);
                }
            }
            for (final Phenomenon phenomenon : phenomenaSet) {
                list.addCodedEvent(phenomenon.getEndingEvent());
                list.addCodedEvent(phenomenon.getStartingEvent());
            }
        }
    }
}
