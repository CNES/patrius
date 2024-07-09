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
 * @history creation 15/03/1012
 */
package fr.cnes.sirius.patrius.events.postprocessing;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import fr.cnes.sirius.patrius.events.PhenomenaList;
import fr.cnes.sirius.patrius.events.Phenomenon;
import fr.cnes.sirius.patrius.math.Comparators;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * @description <p>
 *              This class is a phenomena filter to be applied on a TimeLine object : after treatment, only the
 *              specified phenomena with a duration greater / lower (depending on the isMinDuration boolean) will remain
 *              in the list.
 *              </p>
 * 
 * @useSample </p> PostProcessing filter = new PhenomenonDurationFilter(eclipse, 5.0, true); filter.applyTo(aTimeline);
 *            </p>
 * 
 * @concurrency immutable
 * 
 * @see PostProcessing
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.1
 * 
 */
public final class PhenomenonDurationFilter implements PostProcessing {

    /**
     * List of the phenomena that have to be filtered
     */
    private final List<String> phenCodes;

    /**
     * Duration to for comparison
     */
    private final double timeDuration;

    /**
     * Boolean : true if the time duration is the minimal one : only specified phenomena with greater durations will
     * remain after treatment
     */
    private final boolean minDuration;

    /**
     * Constructor
     * 
     * @param phenCode
     *        : code of the phenomenon that has to be filtered
     * @param duration
     *        max or min duration of the phenomena
     * @param isMinDuration
     *        true if the time duration is the minimal one : only phenomena with greater durations will remain after
     *        treatment
     * 
     */
    public PhenomenonDurationFilter(final String phenCode, final double duration, final boolean isMinDuration) {
        this.phenCodes = new ArrayList<String>();
        this.phenCodes.add(phenCode);
        this.timeDuration = duration;
        this.minDuration = isMinDuration;
    }

    /**
     * Constructor
     * 
     * @param phenCode
     *        : codes of the phenomena that have to be filtered
     * @param duration
     *        max or min duration of the phenomena
     * @param isMinDuration
     *        true if the time duration is the minimal one : only phenomena with greater durations will remain after
     *        treatment
     * 
     */
    public PhenomenonDurationFilter(final List<String> phenCode, final double duration, final boolean isMinDuration) {
        this.phenCodes = new ArrayList<String>();
        this.phenCodes.addAll(phenCode);
        this.timeDuration = duration;
        this.minDuration = isMinDuration;
    }

    /**
     * Removes from the list the specified phenomena that are longer / shorter than the duration criterion (depending on
     * the isMinDuration boolean value). The durations comparison is made with a e-14 relative "double comparison"
     * epsilon.
     * 
     * @param list
     *        the TimeLine to be modified
     */
    @Override
    public void applyTo(final Timeline list) {

        // loop on the phenomena of the list
        final PhenomenaList phenoList = list.getPhenomena();
        final Set<Phenomenon> phenomenaSet = new TreeSet<Phenomenon>();

        for (final String currentCode : this.phenCodes) {
            // creates the phenomena list of the selected type:
            phenomenaSet.addAll(phenoList.getPhenomena(currentCode, null, null));
        }
        for (final Phenomenon p : phenomenaSet) {

            // duration test
            final double duration = p.getTimespan().getDuration();
            final boolean isGreaterThanCriterion = (Comparators.greaterOrEqual(duration, this.timeDuration,
                Precision.DOUBLE_COMPARISON_EPSILON));

            // cases to remove the phenomenon :
            // the criterion is a max duration and the effective duration is greater
            final boolean removeCauseGreater = isGreaterThanCriterion && !this.minDuration;
            // the criterion is a min duration and the effective duration is lower
            final boolean removeCauseLower = !isGreaterThanCriterion && this.minDuration;

            // in one of those two cases, the phenomenon is removed
            if (removeCauseGreater || removeCauseLower) {
                list.removePhenomenon(p);
            }
        }
    }
}
