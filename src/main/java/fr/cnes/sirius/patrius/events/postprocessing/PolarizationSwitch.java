/**
 * 
 * Copyright 2011-2022 CNES
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
 * @history created 02/04/12
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events.postprocessing;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import fr.cnes.sirius.patrius.events.CodedEvent;
import fr.cnes.sirius.patrius.events.Phenomenon;
import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * This class is a post processing creation that creates new elements corresponding to polarization
 * switch events.
 * While no changes are made to the phenomena list of the timeline, the events list will contain
 * new elements.
 * 
 * @concurrency immutable
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public final class PolarizationSwitch implements PostProcessing {

    /**
     * Default polarization switch code.
     */
    private static final String DEFAULT_NAME = " Selection";

    /**
     * The code of the left visibility phenomenon
     */
    private final String codeLogL;

    /**
     * The code of the right visibility phenomenon
     */
    private final String codeLogR;

    /**
     * Minimal duration of phenomena.
     */
    private final double min;

    /**
     * Maximal gap between two successive phenomena.
     */
    private final double max;

    /**
     * Builds an instance of PolarizationSwitch.
     * 
     * @param codeL
     *        the code of the left visibility phenomenon
     * @param codeR
     *        the code of the right visibility phenomenon
     * @param minD
     *        the minimal duration for the phenomena duration filter
     * @param maxD
     *        the maximal gap between phenomena for the phenomena merging filter
     */
    public PolarizationSwitch(final String codeL, final String codeR,
        final double minD, final double maxD) {
        this.codeLogL = codeL;
        this.codeLogR = codeR;
        this.min = minD;
        this.max = maxD;
    }

    /** {@inheritDoc} */
    @Override
    public void applyTo(final Timeline list) {
        // creates a working copy of the timeline:
        final Timeline listCopy = new Timeline(list);
        // initialises the filters:
        // list of phenomena codes:
        final List<String> codes = new ArrayList<String>();
        codes.add(this.codeLogL);
        codes.add(this.codeLogR);
        // Duration filter:
        final PhenomenonDurationFilter durationFilter = new PhenomenonDurationFilter(codes, this.min, true);
        // Type filter:
        final ElementTypeFilter typeFilter = new ElementTypeFilter(codes, false);
        // Merging phenomena filter:
        final Map<String, String> codesMap = new ConcurrentHashMap<String, String>();
        codesMap.put(this.codeLogL, this.codeLogL);
        codesMap.put(this.codeLogR, this.codeLogR);
        final MergePhenomenaCriterion fusionFilter = new MergePhenomenaCriterion(codesMap, this.max);
        // applies the filters:
        typeFilter.applyTo(listCopy);
        durationFilter.applyTo(listCopy);
        fusionFilter.applyTo(listCopy);

        Phenomenon current = null;
        final List<Phenomenon> all = listCopy.getPhenomenaList();
        final Iterator<Phenomenon> iter = all.iterator();
        if (!all.isEmpty()) {
            current = this.getFirstSwitchEvent(list, listCopy, iter);
            // adds the first new event to the list:
            list.addCodedEvent(new CodedEvent(current.getCode() + DEFAULT_NAME, "",
                list.getIntervalOfValidity().getLowerData(), true));
        }
        while (iter.hasNext()) {
            // loop on the L and R phenomena:
            final AbsoluteDate endDate = current.getEndingEvent().getDate();
            final Phenomenon next = iter.next();
            if (current.getCode().equals(next.getCode())) {
                // same phenomenon code: does not create any event
                current = next;
            } else {
                // different phenomenon code: possibly creates an event
                if (next.getTimespan().contains(endDate) &&
                    next.getEndingEvent().getDate().durationFrom(endDate) > this.min) {
                    // the current and next phenomena overlap and the remaining duration of the next phenomenon
                    // is > min: adds a new phenomenon
                    current = next;
                    list.addCodedEvent(new CodedEvent(current.getCode() + DEFAULT_NAME, "", endDate, true));

                } else if (next.getStartingEvent().getDate().compareTo(endDate) > 0) {
                    // the next phenomenon start date is after the current phenomenon end date: adds a new phenomenon:
                    current = next;
                    list.addCodedEvent(new CodedEvent(current.getCode() + DEFAULT_NAME, "", endDate, true));
                }
                // out of these conditions: the current and next phenomena overlap and the remaining duration of the
                // next phenomenon is too short. Ignore the next, switch to the next next keeping the same "current"
                // for possible switch at current end date.
            }
        }
    }

    /**
     * Selects the phenomenon used to create the first switch event.
     * 
     * @param list
     *        the original timeline
     * @param listCopy
     *        a copy of the original timeline containing only L and R events and phenomena
     * @param iterator
     *        the iterator over the L and R timeline phenomena
     * @return a {@Phenomenon}
     * 
     */
    private Phenomenon getFirstSwitchEvent(final Timeline list, final Timeline listCopy,
                                           final Iterator<Phenomenon> iterator) {
        final Phenomenon current;
        final Set<Phenomenon> l = listCopy.getPhenomena().getPhenomena(this.codeLogL, null, null);
        final Set<Phenomenon> r = listCopy.getPhenomena().getPhenomena(this.codeLogR, null, null);
        // handles the first event creation:
        if (l.isEmpty()) {
            current = r.iterator().next();
        } else if (r.isEmpty()) {
            current = l.iterator().next();
        } else {
            final Phenomenon firstL = l.iterator().next();
            final Phenomenon firstR = r.iterator().next();
            if (firstL.getStartingEvent().getDate().equals(list.getIntervalOfValidity().getLowerData())
                && firstR.getStartingEvent().getDate().equals(list.getIntervalOfValidity().getLowerData())) {
                // the first L and R phenomena start at the beginning of the validity interval:
                if (firstL.getEndingEvent().compareTo(firstR.getEndingEvent()) < 0) {
                    // takes the R phenomenon:
                    current = firstR;
                } else {
                    // takes the L phenomenon:
                    current = firstL;
                }
                iterator.next();
                iterator.next();
            } else {
                current = iterator.next();
            }
        }
        return current;
    }
}
