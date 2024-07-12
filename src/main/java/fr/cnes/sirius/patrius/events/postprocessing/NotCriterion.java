/**
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
 * VERSION:4.9:DM:DM-3130:10/05/2022:[PATRIUS] Robustifier le calcul des phenomenes des CodedEventsLogger, ...
 * END-HISTORY
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
package fr.cnes.sirius.patrius.events.postprocessing;

import java.util.Set;
import java.util.TreeSet;

import fr.cnes.sirius.patrius.events.CodedEvent;
import fr.cnes.sirius.patrius.events.Phenomenon;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;

/**
 * This criterion adds to the phenomena list of a Timeline the complementary phenomena of a given
 * phenomenon. The complementary phenomena are defined for every time interval
 * for which the phenomenon of interest does not occur. Depending the replication of the original
 * phenomenon over the timeline, the events defining the beginning and the
 * end of those new phenomena can be : <br>
 * - the ones of the original phenomena <br>
 * - the (dummy) events corresponding to the beginning of the timeline validity interval <br>
 * - the (dummy) events corresponding to the end of the timeline validity interval
 * 
 * 
 * @author Hugo Barrere (Thales).
 */
public class NotCriterion implements PostProcessing {

    /** code used to describe the event corresponding to the beginning of the validity time interval */
    private static final String START = "Start of validity interval";

    /** code used to describe the event corresponding to the end of the validity time interval */
    private static final String END = "End of validity interval";

    /** code of the phenomenon for which the complementary phenomena must be found */
    private final String codePheno;

    /** code of the new phenomena added to the list */
    private final String codeIn;

    /** comment of the new phenomena added to the list */
    private final String commentIn;

    /**
     * Constructor
     * 
     * @param code
     *        code of the phenomenon for which the complementary phenomena must be added to the
     *        list.
     * @param newCode
     *        code of the new phenomena added to the list
     * @param newComment
     *        comment of the new phenomena added to the list
     */
    public NotCriterion(final String code, final String newCode, final String newComment) {

        this.codePheno = code;
        this.codeIn = newCode;
        this.commentIn = newComment;
    }

    /**
     * Adds to the phenomena list of a TimeLine object the phenomena corresponding to each time
     * intervals when a phenomenon does not occur. The events defining the beginning and the end of
     * those new phenomena are directly the ones of the original phenomenon or dummy events
     * corresponding to the bounds of the validity interval.
     * 
     * @param list
     *        the list of events and phenomenon to be modified
     */
    @Override
    public void applyTo(final Timeline list) {

        final AbsoluteDateInterval validityInterval = list.getIntervalOfValidity();
        // set of phenomenon of interest
        final Set<Phenomenon> phenoSet = list.getPhenomena().getPhenomena(this.codePheno, null,
            null);
        
        // events corresponding to the bounds of the validity interval
        final CodedEvent startInterval = new CodedEvent(START, this.commentIn,
            validityInterval.getLowerData(), true);
        final CodedEvent endInterval = new CodedEvent(END, this.commentIn,
            validityInterval.getUpperData(), false);
        
        // test if the list is empty
        if (!phenoSet.isEmpty()) {
            final int size = phenoSet.size();
            final Phenomenon[] phenoArray = phenoSet.toArray(new Phenomenon[size]);

            // initialisation of the complementary phenomena list
            Set<Phenomenon> complementaries = getComplementaryPhenomena(phenoArray[0],
                startInterval, endInterval, this.codeIn, this.commentIn);

            for (int index = 1; index < size; index++) {
                final Set<Phenomenon> current = getComplementaryPhenomena(phenoArray[index],
                    startInterval, endInterval, this.codeIn, this.commentIn);
                complementaries = getPhenomenaIntersections(complementaries, current, this.codeIn,
                    this.commentIn);
            }

            // add the complementary phenomena to the initial list
            for (final Phenomenon complementary : complementaries) {
                list.addPhenomenon(complementary);
            }  
        } else{
            list.addPhenomenon(new Phenomenon(startInterval, true, endInterval, true, this.codeIn, this.commentIn));
        }
    }

    /**
     * Builds a set of complementary phenomena of a provided phenomenon. The complementary phenomena
     * are defined on time intervals for which the phenomenon of interest does not occur.
     * If its time range does not cover the whole validity time interval (scrict
     * comparison) of the timeline, the set contains at least one complementary phenomenon and two
     * at most.
     * 
     * @param phenomenon
     *        the phenomenon of interest
     * @param startInterval
     *        the dummy events corresponding to the beginning of the timeline validity interval
     * @param endInterval
     *        the dummy events corresponding to the end of the timeline validity interval
     * @param codeIn
     *        the code to apply on the complementary phenomena
     * @param commentIn
     *        the comment to describe the complementary phenomena
     * 
     * @return the set of complementary phenomena
     */
    private static Set<Phenomenon> getComplementaryPhenomena(final Phenomenon phenomenon,
                                                             final CodedEvent startInterval,
                                                             final CodedEvent endInterval, final String codeIn,
                                                             final String commentIn) {

        final Set<Phenomenon> complementaries = new TreeSet<>();

        // events defining the phenomenon
        final CodedEvent startEvent = phenomenon.getStartingEvent();
        final CodedEvent endEvent = phenomenon.getEndingEvent();

        // check if the starting event occurred at the begin of the validity interval
        if (startEvent.getDate().durationFrom(startInterval.getDate()) > 0) {
            complementaries.add(new Phenomenon(startInterval, true, startEvent, phenomenon
                .getStartingIsDefined(), codeIn, commentIn));
        }

        // check if the ending event occurred at the end of the validity interval
        if (endEvent.getDate().durationFrom(endInterval.getDate()) < 0) {
            complementaries.add(new Phenomenon(endEvent, phenomenon.getEndingIsDefined(),
                endInterval, true, codeIn, commentIn));
        }

        return complementaries;
    }

    /**
     * Builds a set of phenomena from the intersection of two sets of phenomena. If the timespan of
     * two phenomena overlaps, a new phenomenon is built and its beginning and ending events are
     * directly the ones of the original overlapping phenomena.
     * 
     * @param phenoSet1
     *        the first set of phenomenon
     * @param phenoSet2
     *        the second set of phenomenon
     * @param codeIn
     *        the new code to apply on new phenomena
     * @param commentIn
     *        the comment to describe the new phenomena
     * 
     * @return the set of phenomena
     */
    private static Set<Phenomenon> getPhenomenaIntersections(final Set<Phenomenon> phenoSet1,
                                                             final Set<Phenomenon> phenoSet2, final String codeIn,
                                                             final String commentIn) {

        final Set<Phenomenon> intersection = new TreeSet<>();

        // loop on the two phenomena lists
        for (final Phenomenon p1 : phenoSet1) {
            // time interval of the A phenomenon
            final AbsoluteDateInterval interval1 = p1.getTimespan();

            for (final Phenomenon p2 : phenoSet2) {
                // time interval of the B phenomenon
                final AbsoluteDateInterval interval2 = p2.getTimespan();

                // if the two interval overlap
                if (interval1.overlaps(interval2)) {

                    final CodedEvent boundaryOne;
                    final boolean boundaryOneDefined;
                    final CodedEvent boundaryTwo;
                    final boolean boundaryTwoDefined;

                    // comparison of the lower points
                    final int lowerEndComparison = interval1.compareLowerEndTo(interval2);
                    if (lowerEndComparison < 0) {
                        // new lower point is B lower end point
                        boundaryOne = p2.getStartingEvent();
                        boundaryOneDefined = p2.getStartingIsDefined();
                    } else {
                        // new lower point is A lower end point
                        boundaryOne = p1.getStartingEvent();
                        boundaryOneDefined = p1.getStartingIsDefined();
                    }

                    // comparison of the upper points
                    final int upperEndComparison = interval1.compareUpperEndTo(interval2);
                    if (upperEndComparison < 0) {
                        // new upper point is A upper end point
                        boundaryTwo = p1.getEndingEvent();
                        boundaryTwoDefined = p1.getEndingIsDefined();
                    } else {
                        // new upper point is B upper end point
                        boundaryTwo = p2.getEndingEvent();
                        boundaryTwoDefined = p2.getEndingIsDefined();
                    }

                    // creation of the phenomenon
                    final Phenomenon newPhenomenon = new Phenomenon(boundaryOne,
                        boundaryOneDefined, boundaryTwo, boundaryTwoDefined, codeIn, commentIn);

                    intersection.add(newPhenomenon);
                }
            }
        }

        return intersection;
    }
}
