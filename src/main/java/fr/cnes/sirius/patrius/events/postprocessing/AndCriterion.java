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
 * @history creation 15/03/2012
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events.postprocessing;

import java.util.Set;

import fr.cnes.sirius.patrius.events.CodedEvent;
import fr.cnes.sirius.patrius.events.Phenomenon;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;

/**
 * @description <p>
 *              This criterion adds to the phenomena list of a TimeLine object the phenomena corresponding to each time
 *              intervals when phenomena of particular types A and B occur at the same time. The events defining the
 *              beginning and the end of those new phenomena are directly the ones of the original A and B phenomena.
 *              </p>
 * 
 * @useSample </p> PostProcessing filter = new AndCriterion("codeA", "codeB"); filter.applyTo(aTimeline); </p>
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
public final class AndCriterion implements PostProcessing {

    /** code of the new phenomena added to the list */
    private final String codeIn;

    /** comment of the new phenomena added to the list */
    private final String commentIn;

    /** code of the A phenomenon */
    private final String codePhenoA;

    /** code of the B phenomenon */
    private final String codePhenoB;

    /**
     * Constructor
     * 
     * @param codeA
     *        code of the A phenomenon
     * @param codeB
     *        code of the B phenomenon
     * @param newCode
     *        code of the new phenomena added to the list
     * @param newComment
     *        comment of the new phenomena added to the list
     */
    public AndCriterion(final String codeA, final String codeB, final String newCode, final String newComment) {
        this.codePhenoA = codeA;
        this.codePhenoB = codeB;
        this.codeIn = newCode;
        this.commentIn = newComment;
    }

    /**
     * Adds to the phenomena list of a TimeLine object the phenomena corresponding to each time intervals when phenomena
     * of particular types A and B occur at the same time. The events defining the beginning and the end of those new
     * phenomena are directly the ones of the original A and B phenomena.
     * 
     * @param list
     *        the list of events and phenomenon to be modified
     */
    @Override
    public void applyTo(final Timeline list) {

        // phenomena of the list of the A and B type
        final Set<Phenomenon> phenoSetA = list.getPhenomena().getPhenomena(this.codePhenoA, null, null);
        final Set<Phenomenon> phenoSetB = list.getPhenomena().getPhenomena(this.codePhenoB, null, null);

        // loop on the two phenomena lists
        for (final Phenomenon pA : phenoSetA) {
            // time interval of the A phenomenon
            final AbsoluteDateInterval intervalA = pA.getTimespan();

            for (final Phenomenon pB : phenoSetB) {
                // time interval of the B phenomenon
                final AbsoluteDateInterval intervalB = pB.getTimespan();

                // if the two interval overlap
                if (intervalA.overlaps(intervalB)) {

                    final CodedEvent boundaryOne;
                    final boolean boundaryOneDefined;
                    final CodedEvent boundaryTwo;
                    final boolean boundaryTwoDefined;

                    // comparison of the lower points
                    final int lowerEndComparison = intervalA.compareLowerEndTo(intervalB);
                    if (lowerEndComparison < 0) {
                        // new lower point is B lower end point
                        boundaryOne = pB.getStartingEvent();
                        boundaryOneDefined = pB.getStartingIsDefined();
                    } else {
                        // new lower point is A lower end point
                        boundaryOne = pA.getStartingEvent();
                        boundaryOneDefined = pA.getStartingIsDefined();
                    }

                    // comparison of the upper points
                    final int upperEndComparison = intervalA.compareUpperEndTo(intervalB);
                    if (upperEndComparison < 0) {
                        // new upper point is A upper end point
                        boundaryTwo = pA.getEndingEvent();
                        boundaryTwoDefined = pA.getEndingIsDefined();
                    } else {
                        // new upper point is B upper end point
                        boundaryTwo = pB.getEndingEvent();
                        boundaryTwoDefined = pB.getEndingIsDefined();
                    }

                    // creation of the phenomenon
                    final Phenomenon newPhenomenon = new Phenomenon(boundaryOne, boundaryOneDefined, boundaryTwo,
                        boundaryTwoDefined, this.codeIn, this.commentIn);

                    list.addPhenomenon(newPhenomenon);
                }
            }
        }
    }
}
