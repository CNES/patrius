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
 * @history created 15/03/2012
 */
package fr.cnes.sirius.patrius.events.postprocessing;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import fr.cnes.sirius.patrius.events.CodedEvent;
import fr.cnes.sirius.patrius.events.PhenomenaList;
import fr.cnes.sirius.patrius.events.Phenomenon;

/**
 * @description <p>
 *              This class is a post processing criterion that merges two successive phenomena if the time lapse is
 *              below a given value.
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
public final class MergePhenomenaCriterion implements PostProcessing {

    /**
     * Default merged phenomena code.
     */
    private static final String DEFAULT_NAME = "Merged ";

    /**
     * Code of the element that has to be filtered.
     */
    private final Map<String, String> codes;

    /**
     * Maximal gap between two successive phenomena.
     */
    private final double gap;

    /**
     * Constructor
     * 
     * @description build an instance of PhenomenaFusionCriterion from a code
     * 
     * @param phenomenonCode
     *        : code of the phenomena that have to be merged when they are too close
     * @param maximalGap
     *        : maximal lapse time between two successive phenomena (in sec)
     */
    public MergePhenomenaCriterion(final String phenomenonCode, final double maximalGap) {
        this(phenomenonCode, maximalGap, DEFAULT_NAME + phenomenonCode);
    }

    /**
     * Constructor
     * 
     * @description build an instance of PhenomenaFusionCriterion from a code and a code for the created phenomena.
     * 
     * @param phenomenonCode
     *        : code of the phenomena that have to be merged when they are too close
     * @param maximalGap
     *        : maximal lapse time between two successive phenomena (in sec)
     * @param mergedPhenomenonCode
     *        : merged phenomenon code
     */
    public MergePhenomenaCriterion(final String phenomenonCode, final double maximalGap,
        final String mergedPhenomenonCode) {
        this.codes = new HashMap<String, String>();
        this.codes.put(phenomenonCode, mergedPhenomenonCode);
        this.gap = maximalGap;
    }

    /**
     * Constructor
     * 
     * @description build an instance of PhenomenaFusionCriterion from a list of phenomena codes and codes for the
     *              created phenomena.
     * 
     * @param phenCodes
     *        : codes of the phenomena that have to be merged when they are too close and codes of the created
     *        phenomena
     * @param maximalGap
     *        : maximal lapse time between two successive phenomena (in sec)
     */
    public MergePhenomenaCriterion(final Map<String, String> phenCodes, final double maximalGap) {
        this.codes = new HashMap<String, String>();
        this.codes.putAll(phenCodes);
        this.gap = maximalGap;
    }

    /**
     * Constructor
     * 
     * @description build an instance of PhenomenaFusionCriterion from a list of phenomena codes.
     * 
     * @param phenomenonCode
     *        : codes of the phenomena that have to be merged when they are too close
     * @param maximalGap
     *        : maximal lapse time between two successive phenomena (in sec)
     */
    public MergePhenomenaCriterion(final List<String> phenomenonCode, final double maximalGap) {
        this.gap = maximalGap;
        this.codes = new HashMap<String, String>();
        String code;
        for (int i = 0; i < phenomenonCode.size(); i++) {
            code = phenomenonCode.get(i);
            this.codes.put(code, DEFAULT_NAME + code);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void applyTo(final Timeline list) {
        final PhenomenaList phenomenaList = list.getPhenomena();

        for (final Entry<String, String> entry : this.codes.entrySet()) {
            final String currentCode = entry.getKey();
            // creates the phenomena list of the selected type:
            final Set<Phenomenon> phenomenaSet = new TreeSet<Phenomenon>();
            phenomenaSet.addAll(phenomenaList.getPhenomena(currentCode, null, null));

            if (!phenomenaSet.isEmpty()) {
                final Iterator<Phenomenon> iterator = phenomenaSet.iterator();
                Phenomenon previousPhen = iterator.next();
                Phenomenon currentPhen;

                Phenomenon newPhenomenon;
                CodedEvent newStartEvent;
                CodedEvent newEndEvent;

                double duration;

                while (iterator.hasNext()) {
                    // iterates over the phenomena in the restricted list:
                    currentPhen = iterator.next();
                    duration = currentPhen.getTimespan().getLowerData()
                        .durationFrom(previousPhen.getTimespan().getUpperData());

                    if (duration < this.gap) {
                        // merged phenomenon creation
                        newStartEvent = previousPhen.getStartingEvent();
                        newEndEvent = currentPhen.getEndingEvent();
                        newPhenomenon = new Phenomenon(newStartEvent, previousPhen.getStartingIsDefined(), newEndEvent,
                            currentPhen.getEndingIsDefined(), this.codes.get(currentCode), "");

                        list.addPhenomenon(newPhenomenon);

                        // remove the merged phenomena
                        list.removePhenomenon(previousPhen);
                        list.removePhenomenon(currentPhen);

                        previousPhen = newPhenomenon;
                    } else {
                        previousPhen = currentPhen;
                    }
                }
            }
            phenomenaSet.clear();
            phenomenaSet.addAll(phenomenaList.getPhenomena(currentCode, null, null));
            for (int i = 0; i < phenomenaSet.size(); i++) {
                // the phenomenon should be renamed: removes it from the list and add another one to the list
                // with a different code:
                final Phenomenon phenomenonToRemove = (Phenomenon) phenomenaSet.toArray()[i];
                final Phenomenon newPhenomenonToAdd = new Phenomenon(phenomenonToRemove.getStartingEvent(),
                    phenomenonToRemove.getStartingIsDefined(), phenomenonToRemove.getEndingEvent(),
                    phenomenonToRemove.getEndingIsDefined(), this.codes.get(currentCode), "");
                list.removePhenomenon(phenomenonToRemove);
                // add the new phenomenon:
                list.addPhenomenon(newPhenomenonToAdd);
            }
        }
    }
}
