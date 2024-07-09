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
 * @history created 02/04/12
 */
package fr.cnes.sirius.patrius.events.postprocessing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import fr.cnes.sirius.patrius.events.Phenomenon;

/**
 * This class is a post processing creation that creates a new polarization single selection
 * phenomenon from two sets of visibility phenomena.
 * While no changes are made to the events list of the timeline, the phenomena list will contain
 * a new element.
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
public final class PolarizationSingleSelection implements PostProcessing {

    /**
     * Default polarization single selection code.
     */
    private static final String DEFAULT_NAME = " Selection";

    /**
     * Minimal duration of phenomena.
     */
    private final double min;

    /**
     * Maximal gap between two successive phenomena.
     */
    private final double max;

    /**
     * The code of the left visibility phenomenon
     */
    private final String codeLogL;

    /**
     * The code of the right visibility phenomenon
     */
    private final String codeLogR;

    /**
     * Builds an instance of PolarizationSingleSelection.
     * 
     * @param minD
     *        the minimal duration for the phenomena duration filter
     * @param maxD
     *        the maximal gap between phenomena for the phenomena merging filter
     * @param codeL
     *        the code of the left visibility phenomenon
     * @param codeR
     *        the code of the right visibility phenomenon
     */
    public PolarizationSingleSelection(final String codeL, final String codeR,
        final double minD, final double maxD) {
        this.min = minD;
        this.max = maxD;
        this.codeLogL = codeL;
        this.codeLogR = codeR;
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
        // Type filter:
        final ElementTypeFilter typeFilter = new ElementTypeFilter(codes, false);
        // Duration filter:
        final PhenomenonDurationFilter durationFilter = new PhenomenonDurationFilter(codes, this.min, true);
        // Merging phenomena filter:
        final Map<String, String> codesMap = new ConcurrentHashMap<String, String>();
        codesMap.put(this.codeLogL, this.codeLogL);
        codesMap.put(this.codeLogR, this.codeLogR);
        final MergePhenomenaCriterion fusionFilter = new MergePhenomenaCriterion(codesMap, this.max);
        // applies the filters:
        typeFilter.applyTo(listCopy);
        durationFilter.applyTo(listCopy);
        fusionFilter.applyTo(listCopy);

        // chooses the longest phenomenon among the filtered phenomena:
        Phenomenon longest = null;
        double duration = 0;
        for (final Phenomenon phen : listCopy.getPhenomenaList()) {
            if (phen.getTimespan().getDuration() > duration) {
                longest = phen;
                duration = longest.getTimespan().getDuration();
            }
        }

        if (longest != null) {
            // creates the new phenomenon:
            final Phenomenon newPhenomenon = new Phenomenon(longest.getStartingEvent(), longest.getStartingIsDefined(),
                longest.getEndingEvent(), longest.getEndingIsDefined(), (longest.getCode() + DEFAULT_NAME), "");
            // adds the new phenomenon to the timeline:
            list.addPhenomenon(newPhenomenon);
        }
    }
}
