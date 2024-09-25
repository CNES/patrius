/**
 * HISTORY
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.interval.IntervalEndpointType;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.time.AbsoluteDateIntervalsList;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit tests for the class : {@link TimeLine}
 * 
 * @author Hugo Barrere.
 */
public class TimelineTest {

    /** Arbitrary date for testing purpose. */
    private static final AbsoluteDate DATE = AbsoluteDate.J2000_EPOCH;
    
    /** Arbitrary comment for testing purpose. */
    private static final String COMMENT = "comment";
    
    /**
     * Tests the constructors : <br>
     * {@link Timeline#Timeline(AbsoluteDateInterval)}
     * {@link Timeline#Timeline(Timeline, AbsoluteDateInterval)} {@link Timeline#Timeline(Timeline)}
     * @throws PatriusException
     */
    @Test
    public void testConstructors() throws PatriusException {

        final AbsoluteDate t0 = DATE;
        final AbsoluteDate t1 = DATE.shiftedBy(15);
        final AbsoluteDate t2 = DATE.shiftedBy(40);
        final AbsoluteDate t3 = DATE.shiftedBy(50);
        final AbsoluteDate t4 = DATE.shiftedBy(65);
        final AbsoluteDate t5 = DATE.shiftedBy(90);

        final AbsoluteDateInterval validityInterval = new AbsoluteDateInterval(
                IntervalEndpointType.OPEN, t0, t5, IntervalEndpointType.OPEN);

        // test with an empty events logger
        final CodedEventsLogger logger = new CodedEventsLogger();

        // builds a timeline of reference
        final Timeline timeline = new Timeline(logger, validityInterval, null);
        final List<String> phenCodes = new ArrayList<>();

        // dummy events
        final CodedEvent event0 = new CodedEvent("event0", COMMENT, t0, false);
        final CodedEvent event1 = new CodedEvent("event1", COMMENT, t1, true);
        final CodedEvent event2 = new CodedEvent("event2", COMMENT, t2, false);
        final CodedEvent event3 = new CodedEvent("event3", COMMENT, t3, true);
        final CodedEvent event4 = new CodedEvent("event4", COMMENT, t4, true);
        final CodedEvent event5 = new CodedEvent("event5", COMMENT, t5, false);

        // phenomena
        final String commentPheno1 = "Phenomenon 1";
        final String commentPheno2 = "Phenomenon 2";
        final Phenomenon pheno1 = new Phenomenon(event1, true, event2, true, commentPheno1, COMMENT);
        final Phenomenon pheno2 = new Phenomenon(event3, true, event5, true, commentPheno2, COMMENT);
        final Phenomenon pheno1Bis = new Phenomenon(event4, true, event5, true, commentPheno1,
                COMMENT);

        // add the dummy events to the timeline
        timeline.addCodedEvent(event0);
        timeline.addCodedEvent(event1);
        timeline.addCodedEvent(event2);
        timeline.addCodedEvent(event3);
        timeline.addCodedEvent(event4);
        timeline.addCodedEvent(event5);

        // add the dummy phenomena to the timeline
        timeline.addPhenomenon(pheno1);
        timeline.addPhenomenon(pheno2);
        timeline.addPhenomenon(pheno1Bis);

        // add the phenomena codes
        phenCodes.add(commentPheno1);
        phenCodes.add(commentPheno2);
        Assert.assertTrue(phenCodes.equals(timeline.getPhenomenaCodesList()));
        // the third phenomena has the same code as the first one
        phenCodes.add(pheno1Bis.getComment());
        Assert.assertFalse(phenCodes.equals(timeline.getPhenomenaCodesList()));

        Timeline newTimeline;

        // builds an empty time line from a validity time interval
        newTimeline = new Timeline(validityInterval);
        Assert.assertTrue(newTimeline.getCodedEventsList().isEmpty());
        Assert.assertTrue(newTimeline.getPhenomenaList().isEmpty());
        Assert.assertTrue(newTimeline.getPhenomenaCodesList().isEmpty());

        // buils a new timeline copying the specified timeline with a wider validity interval
        final AbsoluteDateIntervalsList newIntervals = new AbsoluteDateIntervalsList();
        // new validity time intervals that include the one stored in the reference timeline
        newIntervals.add(validityInterval.shift(-1, +1));
        newIntervals.add(validityInterval.shift(0, 0));
        newIntervals.add(validityInterval.shift(0, +1));
        newIntervals.add(validityInterval.shift(-1, 0));

        for (final AbsoluteDateInterval newInterval : newIntervals) {
            newTimeline = new Timeline(timeline, newInterval);
            // check the the equivalence between the lists
            Assert.assertTrue(newTimeline.getCodedEventsList()
                    .equals(timeline.getCodedEventsList()));
            Assert.assertTrue(newTimeline.getPhenomenaList().equals(timeline.getPhenomenaList()));
            Assert.assertTrue(newTimeline.getPhenomenaCodesList().equals(
                    timeline.getPhenomenaCodesList()));
            Assert.assertEquals(newInterval, newTimeline.getIntervalOfValidity());
        }

        // an exception is thrown if the specified interval does not include the former interval
        boolean thrown = false;
        try {
            newTimeline = new Timeline(timeline, validityInterval.shift(+1, -1));

        } catch (PatriusException e) {
            thrown = true;
        }
        Assert.assertTrue(thrown);
    }

    /**
     * Tests the method : <br>
     * {@link Timeline#merge(Timeline)}
     * @throws PatriusException
     */
    @Test
    public void testMerge() throws PatriusException {
        // test with an empty events logger
        final CodedEventsLogger logger = new CodedEventsLogger();

        final AbsoluteDate t0 = DATE;
        final AbsoluteDate t1 = DATE.shiftedBy(15);
        final AbsoluteDate t2 = DATE.shiftedBy(40);
        final AbsoluteDate t3 = DATE.shiftedBy(50);
        final AbsoluteDate t4 = DATE.shiftedBy(65);
        final AbsoluteDate t5 = DATE.shiftedBy(90);

        final AbsoluteDateInterval validityInterval = new AbsoluteDateInterval(t0, t5);

        // dummy events
        final CodedEvent event0 = new CodedEvent("event0", COMMENT, t0, true);
        final CodedEvent event1 = new CodedEvent("event1", COMMENT, t1, true);
        final CodedEvent event2 = new CodedEvent("event2", COMMENT, t2, false);
        final CodedEvent event3 = new CodedEvent("event3", COMMENT, t3, false);
        final CodedEvent event4 = new CodedEvent("event4", COMMENT, t4, true);
        final CodedEvent event5 = new CodedEvent("event5", COMMENT, t5, false);

        // phenomena
        final String commentPheno1 = "Phenomenon 1";
        final String commentPheno2 = "Phenomenon 2";
        final String commentPheno3 = "Phenomenon 3";
        final Phenomenon pheno1 = new Phenomenon(event0, true, event2, true, commentPheno1, COMMENT);
        final Phenomenon pheno2 = new Phenomenon(event1, true, event3, true, commentPheno2, COMMENT);
        final Phenomenon pheno3 = new Phenomenon(event4, true, event5, true, commentPheno3, COMMENT);

        final Timeline timeline1 = new Timeline(logger, validityInterval, null);
        timeline1.addCodedEvent(event0);
        timeline1.addCodedEvent(event2);
        timeline1.addCodedEvent(event1);
        timeline1.addCodedEvent(event3);
        timeline1.addPhenomenon(pheno1);
        timeline1.addPhenomenon(pheno2);

        final Timeline timeline2 = new Timeline(logger, validityInterval, null);
        timeline2.addCodedEvent(event4);
        timeline2.addCodedEvent(event5);
        timeline2.addCodedEvent(event1);
        timeline2.addPhenomenon(pheno3);

        // merge both timelines
        timeline1.merge(timeline2);
        Assert.assertTrue(timeline1.getCodedEventsList().equals(
                Arrays.asList(event0, event1, event2, event3, event4, event5)));
        Assert.assertTrue(timeline1.getPhenomenaList()
                .equals(Arrays.asList(pheno1, pheno2, pheno3)));
        Assert.assertTrue(timeline1.getPhenomenaCodesList().equals(
                Arrays.asList(commentPheno1, commentPheno2, commentPheno3)));

        // merge the previous timeline with an empty timeline : nothing to add
        timeline1.merge(new Timeline(validityInterval));
        Assert.assertEquals(6, timeline1.getCodedEventsList().size());
        Assert.assertEquals(3, timeline1.getPhenomenaList().size());
        Assert.assertEquals(3, timeline1.getPhenomenaCodesList().size());

        // exception must me thrown
        boolean thrown = false;
        try {
            timeline1.merge(new Timeline(validityInterval.shift(+1)));
        } catch (PatriusException e) {
            thrown = true;
        }
        Assert.assertTrue(thrown);
    }
    
    /**
     * Tests the method : <br>
     * {@link Timeline#join(Timeline)}
     * @throws PatriusException
     */
    @Test
    public void join() throws PatriusException {
        // test with an empty events logger
        final CodedEventsLogger logger = new CodedEventsLogger();

        // Tests with 3 timelines (A<B<C)

        // dates of events for timeline A
        final AbsoluteDate t0A = DATE;
        final AbsoluteDate t1A = DATE.shiftedBy(25);
        final AbsoluteDate t2A = DATE.shiftedBy(60);

        // dates of events for timeline B
        final AbsoluteDate t0B = t2A.shiftedBy(60);
        final AbsoluteDate t1B = t2A.shiftedBy(100);
        final AbsoluteDate t2B = t2A.shiftedBy(110);

        // dates of events for timeline B
        final AbsoluteDate t0C = t2B.shiftedBy(30);
        final AbsoluteDate t1C = t2B.shiftedBy(60);
        final AbsoluteDate t2C = t2B.shiftedBy(75);

        // dates of events for timeline C
        final AbsoluteDateInterval validityA = new AbsoluteDateInterval(t0A, t2A);
        final AbsoluteDateInterval validityB = new AbsoluteDateInterval(t0B, t2B);
        final AbsoluteDateInterval validityC = new AbsoluteDateInterval(t0C, t2C);

        // dummy events for A timeline
        final String comment = "comment";
        final CodedEvent event0A = new CodedEvent("event0", comment, t0A, true);
        final CodedEvent event1A = new CodedEvent("event1", comment, t1A, true);
        final CodedEvent event2A = new CodedEvent("event2", comment, t2A, false);

        // dummy events for B timeline
        final CodedEvent event0B = new CodedEvent("event0", comment, t0B, true);
        final CodedEvent event1B = new CodedEvent("event1", comment, t1B, false);
        final CodedEvent event2B = new CodedEvent("event2", comment, t2B, false);

        // dummy events for C timeline
        final CodedEvent event0C = new CodedEvent("event0", comment, t0C, true);
        final CodedEvent event1C = new CodedEvent("event1", comment, t1C, false);
        final CodedEvent event2C = new CodedEvent("event2", comment, t2C, true);

        // phenomena A timeline
        final String comPhenoA = "phenomenon A";
        final Phenomenon pheno1A = new Phenomenon(event0A, true, event2A, true, comPhenoA, comment);
        final Phenomenon pheno2A = new Phenomenon(event1A, true, event2A, true, comPhenoA, comment);

        // phenomena B timeline
        final String comPhenoB = "phenomenon B";
        final Phenomenon pheno1B = new Phenomenon(event0B, true, event1B, true, comPhenoB, comment);
        final Phenomenon pheno2B = new Phenomenon(event0B, true, event2B, true, comPhenoB, comment);

        // phenomena C timeline
        final String comPhenoC = "phenomenon C";
        final Phenomenon pheno1C = new Phenomenon(event0C, true, event1C, true, comPhenoC, comment);

        // builds the 3 timelines and the events/phenomena
        final Timeline timelineA = new Timeline(logger, validityA, null);
        final Timeline timelineB = new Timeline(logger, validityB, null);
        final Timeline timelineC = new Timeline(logger, validityC, null);

        timelineA.addCodedEvent(event0A);
        timelineA.addCodedEvent(event1A);
        timelineA.addCodedEvent(event2A);
        timelineA.addPhenomenon(pheno1A);
        timelineA.addPhenomenon(pheno2A);

        timelineB.addCodedEvent(event0B);
        timelineB.addCodedEvent(event1B);
        timelineB.addCodedEvent(event2B);
        timelineB.addPhenomenon(pheno1B);
        timelineB.addPhenomenon(pheno2B);

        timelineC.addCodedEvent(event0C);
        timelineC.addCodedEvent(event1C);
        timelineC.addCodedEvent(event2C);
        timelineC.addPhenomenon(pheno1C);

        Timeline joined;

        // concatenate A and B timelines
        joined = timelineB.join(timelineA);
        Assert.assertEquals(joined.getPhenomenaList(),
                Arrays.asList(pheno1A, pheno2A, pheno1B, pheno2B));
        Assert.assertEquals(joined.getCodedEventsList(),
                Arrays.asList(event0A, event1A, event2A, event0B, event1B, event2B));
        Assert.assertEquals(joined.getPhenomenaCodesList(), Arrays.asList(comPhenoB, comPhenoA));
        Assert.assertEquals(joined.getIntervalOfValidity(), new AbsoluteDateInterval(t0A, t2B));

        // reverse concatenation should return the same result
        joined = timelineA.join(timelineB);
        Assert.assertEquals(joined.getPhenomenaList(),
                Arrays.asList(pheno1A, pheno2A, pheno1B, pheno2B));
        Assert.assertEquals(joined.getCodedEventsList(),
                Arrays.asList(event0A, event1A, event2A, event0B, event1B, event2B));
        Assert.assertEquals(joined.getPhenomenaCodesList(), Arrays.asList(comPhenoA, comPhenoB));
        Assert.assertEquals(joined.getIntervalOfValidity(), new AbsoluteDateInterval(t0A, t2B));

        // concatenate B and C timelines
        joined = timelineC.join(timelineB);
        Assert.assertEquals(joined.getPhenomenaList(), Arrays.asList(pheno1B, pheno2B, pheno1C));
        Assert.assertEquals(joined.getCodedEventsList(),
                Arrays.asList(event0B, event1B, event2B, event0C, event1C, event2C));
        Assert.assertEquals(joined.getPhenomenaCodesList(), Arrays.asList(comPhenoC, comPhenoB));
        Assert.assertEquals(joined.getIntervalOfValidity(), new AbsoluteDateInterval(t0B, t2C));

        // concatenate A and C timelines
        joined = timelineA.join(timelineC);
        Assert.assertEquals(joined.getPhenomenaList(), Arrays.asList(pheno1A, pheno2A, pheno1C));
        Assert.assertEquals(joined.getCodedEventsList(),
                Arrays.asList(event0A, event1A, event2A, event0C, event1C, event2C));
        Assert.assertEquals(joined.getPhenomenaCodesList(), Arrays.asList(comPhenoA, comPhenoC));
        Assert.assertEquals(joined.getIntervalOfValidity(), new AbsoluteDateInterval(t0A, t2C));

        // concatenate A with an empty timeline
        joined = timelineA.join(new Timeline(validityB));
        Assert.assertEquals(joined.getPhenomenaList(), timelineA.getPhenomenaList());
        Assert.assertEquals(joined.getCodedEventsList(), timelineA.getCodedEventsList());
        Assert.assertEquals(joined.getPhenomenaCodesList(), timelineA.getPhenomenaCodesList());
        Assert.assertEquals(joined.getIntervalOfValidity(), new AbsoluteDateInterval(t0A, t2B));

        // concatenate 2 empty timelines
        final Timeline empty = new Timeline(validityA);
        joined = empty.join(new Timeline(validityB));
        Assert.assertTrue(joined.getPhenomenaList().isEmpty());
        Assert.assertTrue(joined.getCodedEventsList().isEmpty());
        Assert.assertTrue(joined.getPhenomenaCodesList().isEmpty());
        Assert.assertEquals(joined.getIntervalOfValidity(), new AbsoluteDateInterval(t0A, t2B));

        // exception is supposed to be thrown : validity time intervals do overlap
        boolean thrown = false;
        try {
            joined = timelineA.join(new Timeline(timelineB, new AbsoluteDateInterval(t1A, t2B)));
        } catch (final PatriusException e) {
            thrown = true;
        }
        Assert.assertTrue(thrown);
    }
}
