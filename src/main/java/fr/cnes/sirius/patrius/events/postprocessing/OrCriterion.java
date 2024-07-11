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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3131:10/05/2022:[PATRIUS] Anomalie dans la methode applyTo de la classe OrCriterion 
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
 *              intervals when phenomena of particular types A OR B occur. The events defining the beginning and the end
 *              of those new phenomena are directly the ones of the original A and B phenomena.
 *              </p>
 * 
 * @useSample </p> PostProcessing filter = new OrCriterion("codeA", "codeB"); filter.applyTo(aTimeline); </p>
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
public final class OrCriterion implements PostProcessing {

    /** code of the A phenomenon */
    private final String codePhenoA;

    /** code of the B phenomenon */
    private final String codePhenoB;

    /** code of the new phenomena added to the list */
    private final String codeIn;

    /** comment of the new phenomena added to the list */
    private final String commentIn;

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
    public OrCriterion(final String codeA, final String codeB, final String newCode, final String newComment) {
        this.codePhenoA = codeA;
        this.codePhenoB = codeB;
        this.codeIn = newCode;
        this.commentIn = newComment;
    }

    /**
     * Adds to the phenomena list of a TimeLine object the phenomena corresponding to each time intervals when phenomena
     * of particular types A OR B occur. The events defining the beginning and the end of those new phenomena are
     * directly the ones of the original A and B phenomena.
     * 
     * @param list
     *        the list of events and phenomenon to be modified
     */
    @Override
    public void applyTo(final Timeline list) {

        // phenomena of the list of the A and B type
        final Set<Phenomenon> phenoTotalSet = list.getPhenomena().getPhenomena(this.codePhenoA, null, null);
        phenoTotalSet.addAll(list.getPhenomena().getPhenomena(this.codePhenoB, null, null));

        // creation of an ORDERED array containing all the phenomena "A OR B"
        // The order is the one of the beginning dates
        final int phenomenaTotalNumber = phenoTotalSet.size();
        final Phenomenon[] phenomenaTotalArray = phenoTotalSet.toArray(new Phenomenon[phenomenaTotalNumber]);

        // initialisations
        int treatedPhenomena = 0;

        // loop to treat all phenomena of the list
        while (treatedPhenomena < phenomenaTotalNumber) {

            // current basis phenomenon
            final Phenomenon currentBasisP = phenomenaTotalArray[treatedPhenomena];
            final AbsoluteDateInterval currentBasisInterval = currentBasisP.getTimespan();

            AbsoluteDateInterval treatedPhenomenaInterval = currentBasisInterval;

            // default : second boundary is the one of the first interval
            CodedEvent boundaryTwo = phenomenaTotalArray[treatedPhenomena].getEndingEvent();
            boolean boundaryTwoDefined = phenomenaTotalArray[treatedPhenomena].getEndingIsDefined();

            boolean overlapsTheNextPhenomena = true;

            while (overlapsTheNextPhenomena) {

                // next phenomenon : existence test
                final boolean isLastPhenomenon = (treatedPhenomena == phenomenaTotalNumber - 1);

                if (isLastPhenomenon) {
                    // if the treated phenomenon is the last of the list, end of the loop
                    overlapsTheNextPhenomena = false;
                } else {
                    // next phenomenon
                    final Phenomenon nextP = phenomenaTotalArray[treatedPhenomena + 1];
                    final AbsoluteDateInterval nextInterval = nextP.getTimespan();

                    // overlap with the next interval test
                    overlapsTheNextPhenomena = treatedPhenomenaInterval.overlaps(nextInterval);

                    if (overlapsTheNextPhenomena) {
                        // treated phenomena account
                        treatedPhenomena++;

                        // if the end of the newt phenomenon occurs after the and of the current one,
                        // the second boundary is set to it.
                        if (nextInterval.getUpperData().compareTo(treatedPhenomenaInterval.getUpperData()) > 0) {
                            boundaryTwo = phenomenaTotalArray[treatedPhenomena].getEndingEvent();
                            boundaryTwoDefined = phenomenaTotalArray[treatedPhenomena].getEndingIsDefined();

                            // initialisation to test the overlap with the next interval
                            treatedPhenomenaInterval = nextInterval;
                        }
                    }
                }
            }

            // new phenomenon creation
            // boundary one is
            final CodedEvent boundaryOne = currentBasisP.getStartingEvent();
            final boolean boundaryOneDefined = currentBasisP.getStartingIsDefined();

            final Phenomenon newPhenomenon = new Phenomenon(boundaryOne, boundaryOneDefined, boundaryTwo,
                boundaryTwoDefined, this.codeIn, this.commentIn);

            // adding of the new phenomenon to the list
            list.addPhenomenon(newPhenomenon);

            // initialisation for the next step of the loop
            treatedPhenomena++;
        }
    }

}
