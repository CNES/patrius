/**
 * Copyright 2021-2021 CNES
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
 * HISTORY
 * VERSION:4.9:DM:DM-3081:10/05/2022:[PATRIUS] Surcharge de la methode toString() dans StrictLegsSequence 
 * VERSION:4.9:DM:DM-3154:10/05/2022:[PATRIUS] Amelioration des methodes permettant l'extraction d'une sous-sequence 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.7:DM:DM-2859:18/05/2021:Optimisation du code ; SpacecraftState 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.utils.legs;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.math.interval.IntervalEndpointType;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for {@link StrictLegsSequence} class.
 *
 * @author Emmanuel Bignon
 *
 * @since 4.7
 */
public class StrictLegsSequenceTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle strict legs sequence
         *
         * @featureDescription object describing a strict legs sequence
         *
         * @coveredRequirements DM-2653
         */
        LEGS_SEQUENCE
    }

    /**
     * @testType UT
     *
     * @testedFeature {@link features#LEGS_SEQUENCE}
     *
     * @testedMethod all legs sequence methods
     *
     * @description test all functional methods of legs sequence for null input values
     *
     * @input an empty leg sequence, null input values
     *
     * @output exceptions
     *
     * @testPassCriteria result is as expected (functional test)
     *
     * @referenceVersion 4.7
     *
     * @nonRegressionVersion 4.7
     */
    @Test
    public final void testNullValues() throws PatriusException {

        // Build sequence
        final StrictLegsSequence<Leg> sequence = new StrictLegsSequence<Leg>();

        // ================ Test null values ================

        try {
            sequence.first(null);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        try {
            sequence.last(null);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        try {
            sequence.simultaneous(null);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        try {
            sequence.previous(null);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        try {
            sequence.next(null);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        try {
            final Leg leg = null;
            sequence.head(leg);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        try {
            final AbsoluteDate date = null;
            sequence.head(date);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        try {
            final Leg leg = null;
            sequence.tail(leg);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        try {
            final AbsoluteDate date = null;
            sequence.tail(date);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        try {
            final Leg leg = null;
            sequence.sub(leg, leg);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        try {
            final AbsoluteDate date = null;
            sequence.sub(date, date);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        try {
            sequence.isEmpty(null, null);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        try {
            sequence.add(null);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        try {
            sequence.remove(null);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        try {
            sequence.removeAll(null);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
    }

    /**
     * @testType UT
     *
     * @testedFeature {@link features#LEGS_SEQUENCE}
     *
     * @testedMethod all legs sequence methods
     *
     * @description test all functional methods of legs sequence for empty sequence
     *
     * @input an empty leg sequence, standard input values
     *
     * @output output of all methods
     *
     * @testPassCriteria result is as expected (functional test)
     *
     * @referenceVersion 4.9
     *
     * @nonRegressionVersion 4.9
     */
    @Test
    public final void testSequenceEmpty() throws PatriusException {

        // Build expected empty strict legs sequence
        final String expectedEmptyStrictLegsSequenceString = "Empty legs sequence";

        // Build sequence
        final StrictLegsSequence<Leg> sequence = new StrictLegsSequence<Leg>();
        final AbsoluteDate date1 = AbsoluteDate.J2000_EPOCH.shiftedBy(0.);
        final AbsoluteDate date2 = AbsoluteDate.J2000_EPOCH.shiftedBy(10.);
        final AbsoluteDate date3 = AbsoluteDate.J2000_EPOCH.shiftedBy(20.);
        final Leg leg1 = new LinearLeg(new AbsoluteDateInterval(date1, date2), 1, 2);
        final Leg leg2 = new LinearLeg(new AbsoluteDateInterval(date2, date3), 2, 3);

        // ================ Tests on an empty sequence ================

        Assert.assertNull(sequence.first());
        Assert.assertNull(sequence.first(AbsoluteDate.J2000_EPOCH));
        Assert.assertNull(sequence.first(leg1));

        Assert.assertNull(sequence.last());
        Assert.assertNull(sequence.last(AbsoluteDate.J2000_EPOCH));
        Assert.assertNull(sequence.last(leg1));

        Assert.assertTrue(sequence.simultaneous(AbsoluteDate.J2000_EPOCH).isEmpty());
        Assert.assertTrue(sequence.simultaneous(leg1).isEmpty());

        Assert.assertNull(sequence.previous(leg1));

        Assert.assertNull(sequence.next(leg1));

        Assert.assertTrue(sequence.head(leg1).isEmpty());
        Assert.assertTrue(sequence.head(AbsoluteDate.J2000_EPOCH).isEmpty());
        Assert.assertTrue(sequence.head(AbsoluteDate.J2000_EPOCH, true).isEmpty());

        Assert.assertTrue(sequence.tail(leg1).isEmpty());
        Assert.assertTrue(sequence.tail(AbsoluteDate.J2000_EPOCH).isEmpty());
        Assert.assertTrue(sequence.tail(AbsoluteDate.J2000_EPOCH, true).isEmpty());

        Assert.assertTrue(sequence.sub(leg1, leg2).isEmpty());
        Assert.assertTrue(sequence.sub(AbsoluteDate.J2000_EPOCH, AbsoluteDate.J2000_EPOCH)
                .isEmpty());
        Assert.assertTrue(sequence.sub(AbsoluteDate.J2000_EPOCH, AbsoluteDate.J2000_EPOCH, true)
                .isEmpty());
        Assert.assertTrue(sequence.sub(
                new AbsoluteDateInterval(AbsoluteDate.J2000_EPOCH, AbsoluteDate.J2000_EPOCH))
                .isEmpty());
        Assert.assertTrue(sequence.sub(
                new AbsoluteDateInterval(AbsoluteDate.J2000_EPOCH, AbsoluteDate.J2000_EPOCH), true)
                .isEmpty());

        Assert.assertTrue(sequence.isEmpty());

        Assert.assertFalse(sequence.remove(leg1));

        Assert.assertTrue(sequence.removeAll(new ArrayList<LinearLeg>()));

        Assert.assertEquals(0, sequence.size());

        Assert.assertEquals(0, sequence.toArray().length);

        Assert.assertEquals(0, sequence.toArray(new LinearLeg[0]).length);

        Assert.assertTrue(sequence.toPrettyString().equals(expectedEmptyStrictLegsSequenceString));

        Assert.assertTrue(sequence.toString().equals(expectedEmptyStrictLegsSequenceString));

        Assert.assertNull(sequence.getTimeInterval());
    }

    /**
     * @testType UT
     *
     * @testedFeature {@link features#LEGS_SEQUENCE}
     *
     * @testedMethod all legs sequence methods
     *
     * @description test all functional methods of legs sequence
     *
     * @input a standard continuous leg sequence with 3 segments
     *
     * @output output of all methods
     *
     * @testPassCriteria result is as expected (functional test)
     *
     * @referenceVersion 4.9
     *
     * @nonRegressionVersion 4.9
     */
    @Test
    public final void testSequenceStandard() throws PatriusException {

        // Build sequence [date1; date2] U [date2; date3] U [date3; date4]
        final StrictLegsSequence<Leg> sequence = new StrictLegsSequence<Leg>();
        final AbsoluteDate date1 = AbsoluteDate.J2000_EPOCH.shiftedBy(0.);
        final AbsoluteDate datehalf12 = AbsoluteDate.J2000_EPOCH.shiftedBy(5.);
        final AbsoluteDate date2 = AbsoluteDate.J2000_EPOCH.shiftedBy(10.);
        final AbsoluteDate datehalf23 = AbsoluteDate.J2000_EPOCH.shiftedBy(15.);
        final AbsoluteDate date3 = AbsoluteDate.J2000_EPOCH.shiftedBy(20.);
        final AbsoluteDate datehalf34 = AbsoluteDate.J2000_EPOCH.shiftedBy(25.);
        final AbsoluteDate date4 = AbsoluteDate.J2000_EPOCH.shiftedBy(30.);
        final Leg leg1 = new LinearLeg(new AbsoluteDateInterval(date1, date2), 1, 2);
        final Leg leg2 = new LinearLeg(new AbsoluteDateInterval(date2, date3), 2, 3);
        final Leg leg3 = new LinearLeg(new AbsoluteDateInterval(date3, date4), 3, 4);

        // ================ Test Add method ================
        // Add in random order
        sequence.add(leg3);
        sequence.add(leg1);
        sequence.add(leg2);

        // Checks
        Assert.assertTrue(sequence.contains(leg1));
        Assert.assertTrue(sequence.contains(leg2));
        Assert.assertTrue(sequence.contains(leg3));

        // Particular cases
        try {
            // Overlapping
            sequence.add(new LinearLeg(new AbsoluteDateInterval(date2, date4), 1, 2));
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        try {
            // Already added leg
            sequence.add(leg1);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        // ================ Test First method ================

        // Standard
        Assert.assertEquals(leg1, sequence.first());
        Assert.assertEquals(leg2, sequence.first(date2));
        // Within
        Assert.assertEquals(leg3, sequence.first(date2.shiftedBy(1.)));
        // Before sequence
        Assert.assertEquals(leg1, sequence.first(date1.shiftedBy(-1.)));
        // After sequence
        Assert.assertEquals(null, sequence.first(date3.shiftedBy(1.)));

        // ================ Test Last method ================

        // Standard
        Assert.assertEquals(leg3, sequence.last());
        Assert.assertEquals(leg1, sequence.last(date2));
        // Within
        Assert.assertEquals(leg1, sequence.last(date2.shiftedBy(1.)));
        // Before sequence
        Assert.assertEquals(null, sequence.last(date1.shiftedBy(-1.)));
        // After sequence
        Assert.assertEquals(leg2, sequence.last(date3.shiftedBy(1.)));
        Assert.assertEquals(leg3, sequence.last(date4.shiftedBy(1.)));

        // ================ Test Simultaneous method ================

        // Standard
        Assert.assertEquals(leg1, sequence.simultaneous(leg1).iterator().next());
        Assert.assertEquals(1, sequence.simultaneous(leg1).size());
        Assert.assertEquals(leg2, sequence.simultaneous(leg2).iterator().next());
        Assert.assertEquals(leg3, sequence.simultaneous(leg3).iterator().next());
        Assert.assertEquals(leg1, sequence.simultaneous(date1).iterator().next());
        Assert.assertEquals(leg2, sequence.simultaneous(date2).iterator().next());
        Assert.assertEquals(1, sequence.simultaneous(date2).size());
        // Exception
        try {
            // Non-existent leg
            sequence.simultaneous(new LinearLeg(new AbsoluteDateInterval(date2, date4), 1, 2));
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        // ================ Test Previous method ================

        // Standard
        Assert.assertEquals(null, sequence.previous(leg1));
        Assert.assertEquals(leg1, sequence.previous(leg2));
        // Exception
        try {
            // Non-existent leg
            sequence.previous(new LinearLeg(new AbsoluteDateInterval(date2, date4), 1, 2));
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        // ================ Test Next method ================

        // Standard
        Assert.assertEquals(null, sequence.next(leg3));
        Assert.assertEquals(leg3, sequence.next(leg2));
        // Exception
        try {
            // Non-existent leg
            sequence.next(new LinearLeg(new AbsoluteDateInterval(date2, date4), 1, 2));
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        // ================ Test Head method ================

        // Standard
        Assert.assertEquals(2, sequence.head(leg2).size());
        Assert.assertEquals(1, sequence.head(leg1).size());
        Assert.assertEquals(2, sequence.head(leg2.getDate()).size());
        Assert.assertEquals(1, sequence.head(leg2.getDate(), true).size());
        // Exception
        try {
            // Non-existent leg
            sequence.head(new LinearLeg(new AbsoluteDateInterval(date2, date4), 1, 2));
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        // ================ Test Tail method ================

        // Standard
        Assert.assertEquals(2, sequence.tail(leg2).size());
        Assert.assertEquals(1, sequence.tail(leg3).size());
        Assert.assertEquals(2, sequence.tail(leg2.getDate()).size());
        Assert.assertEquals(1, sequence.tail(leg2.getDate(), true).size());
        // Exception
        try {
            // Non-existent leg
            sequence.tail(new LinearLeg(new AbsoluteDateInterval(date2, date4), 1, 2));
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        // ================ Test Sub method ================

        // Standard
        Assert.assertEquals(2, sequence.sub(leg1, leg2).size());
        Assert.assertEquals(1, sequence.sub(leg2, leg2).size());
        Assert.assertEquals(2, sequence.sub(leg1.getDate(), leg2.getDate()).size());
        Assert.assertEquals(0, sequence.sub(leg1.getDate(), leg2.getDate(), true).size());
        Assert.assertEquals(2,
                sequence.sub(new AbsoluteDateInterval(leg1.getDate(), leg2.getDate())).size());
        Assert.assertEquals(0,
                sequence.sub(new AbsoluteDateInterval(leg1.getDate(), leg2.getDate()), true).size());

        // ================ Test Copy method ================

        // Standard
        StrictLegsSequence<Leg> truncatedSeq = sequence.copy(new AbsoluteDateInterval(datehalf12,
                datehalf34));
        Assert.assertEquals(3, truncatedSeq.size());
        Assert.assertTrue(truncatedSeq.getTimeInterval().contains(datehalf12));
        Assert.assertTrue(truncatedSeq.getTimeInterval().contains(datehalf34));
        // For StrictLegsSequence Legs are considered to have closed boundaries, so is must not
        // change whether we ask for strict copy or not
        truncatedSeq = sequence.copy(new AbsoluteDateInterval(datehalf12, datehalf23), true);
        Assert.assertEquals(2, truncatedSeq.size());
        Assert.assertTrue(truncatedSeq.getTimeInterval().contains(datehalf12));
        Assert.assertTrue(truncatedSeq.getTimeInterval().contains(datehalf23));
        Assert.assertFalse(truncatedSeq.getTimeInterval().contains(datehalf34));
        // Exception
        try {
            // Input interval not included
            sequence.copy(new AbsoluteDateInterval(date1.shiftedBy(-1), datehalf34), true);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        // Exception
        try {
            // Keys in reverse order
            sequence.sub(leg2, leg1);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        try {
            // Non-existent leg
            sequence.sub(leg1, new LinearLeg(new AbsoluteDateInterval(date2, date4), 1, 2));
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        // ================ Test IsEmpty method ================

        // Standard
        Assert.assertFalse(sequence.isEmpty());
        Assert.assertFalse(sequence.isEmpty(date1, date2));
        Assert.assertFalse(sequence.isEmpty(date1, date3));
        Assert.assertFalse(sequence.isEmpty(date1.shiftedBy(-1.), date1.shiftedBy(1.)));
        Assert.assertTrue(sequence.isEmpty(date1.shiftedBy(-2.), date1.shiftedBy(-1.)));

        // ================ Test Clear method ================

        sequence.clear();

        // Checks
        Assert.assertEquals(0, sequence.size());

        // ================ Test AddAll method ================

        final List<Leg> collection = new ArrayList<Leg>();
        collection.add(leg1);
        collection.add(leg2);
        collection.add(leg3);

        try {
            sequence.addAll(collection);
            Assert.fail();
        } catch (final UnsupportedOperationException e) {
            Assert.assertTrue(true);
        }

        // ================ Test remove, removeAll method ================

        sequence.add(leg1);
        sequence.add(leg2);
        sequence.add(leg3);

        sequence.remove(leg1);

        // Checks
        Assert.assertFalse(sequence.contains(leg1));
        Assert.assertTrue(sequence.contains(leg2));
        Assert.assertTrue(sequence.contains(leg3));

        sequence.removeAll(collection);
        Assert.assertEquals(0, sequence.size());

        // ================ Test retainAll method ================

        try {
            sequence.retainAll(collection);
            Assert.fail();
        } catch (final UnsupportedOperationException e) {
            Assert.assertTrue(true);
        }

        // ================ Test toArray method ================

        sequence.add(leg1);
        sequence.add(leg2);
        sequence.add(leg3);

        final Object[] array1 = sequence.toArray();
        final Object[] array2 = sequence.toArray(new Leg[0]);
        Assert.assertEquals(3, array1.length);
        Assert.assertEquals(3, array2.length);
        Assert.assertEquals(leg1, array1[0]);
        Assert.assertEquals(leg2, array1[1]);
        Assert.assertEquals(leg3, array1[2]);
        Assert.assertEquals(leg1, array2[0]);
        Assert.assertEquals(leg2, array2[1]);
        Assert.assertEquals(leg3, array2[2]);

        // ================ Test toPrettyString method ================

        final String prettyString1 = sequence.toPrettyString();
        final String[] linesPrettyString1 = prettyString1.split("\n");
        Assert.assertEquals(201, prettyString1.length());
        Assert.assertEquals("1 : " + leg1.toPrettyString(), linesPrettyString1[1]);
        Assert.assertEquals("2 : " + leg2.toPrettyString(), linesPrettyString1[2]);
        Assert.assertEquals("3 : " + leg3.toPrettyString(), linesPrettyString1[3]);

        // ================ Test toString method ================

        final String string1 = sequence.toPrettyString();
        final String[] linesString1 = string1.split("\n");
        Assert.assertEquals(201, string1.length());
        Assert.assertEquals("1 : " + leg1.toPrettyString(), linesString1[1]);
        Assert.assertEquals("2 : " + leg2.toPrettyString(), linesString1[2]);
        Assert.assertEquals("3 : " + leg3.toPrettyString(), linesString1[3]);

        // ================ Test getTimeInterval method ================

        Assert.assertEquals(date1, sequence.getTimeInterval().getLowerData());
        Assert.assertEquals(date4, sequence.getTimeInterval().getUpperData());
    }

    /**
     * @testType UT
     *
     * @testedFeature {@link features#LEGS_SEQUENCE}
     *
     * @testedMethod all legs sequence methods
     *
     * @description test all functional methods of legs sequence
     *
     * @input a continuous leg sequence with 3 segments, all with open boundaries
     *
     * @output output of all methods
     *
     * @testPassCriteria result is as expected (functional test)
     *
     * @referenceVersion 4.9
     *
     * @nonRegressionVersion 4.9
     */
    @Test
    public final void testSequenceWithOpenBoundaries() throws PatriusException {

        // Build sequence ]date1; date2[ U ]date2; date3[ U ]date3; date4[
        final StrictLegsSequence<Leg> sequence = new StrictLegsSequence<Leg>();
        final AbsoluteDate date1 = AbsoluteDate.J2000_EPOCH.shiftedBy(0.);
        final AbsoluteDate date2 = AbsoluteDate.J2000_EPOCH.shiftedBy(10.);
        final AbsoluteDate date3 = AbsoluteDate.J2000_EPOCH.shiftedBy(20.);
        final AbsoluteDate date4 = AbsoluteDate.J2000_EPOCH.shiftedBy(30.);
        final AbsoluteDate datehalf12 = AbsoluteDate.J2000_EPOCH.shiftedBy(5.);
        final AbsoluteDate datehalf34 = AbsoluteDate.J2000_EPOCH.shiftedBy(25.);
        final Leg leg1 = new LinearLeg(new AbsoluteDateInterval(IntervalEndpointType.OPEN, date1,
                date2, IntervalEndpointType.OPEN), 1, 2);
        final Leg leg2 = new LinearLeg(new AbsoluteDateInterval(IntervalEndpointType.OPEN, date2,
                date3, IntervalEndpointType.OPEN), 2, 3);
        final Leg leg3 = new LinearLeg(new AbsoluteDateInterval(IntervalEndpointType.OPEN, date3,
                date4, IntervalEndpointType.OPEN), 3, 4);

        // ================ Test Add method ================
        // Add in random order
        sequence.add(leg3);
        sequence.add(leg1);
        sequence.add(leg2);

        // Checks
        Assert.assertTrue(sequence.contains(leg1));
        Assert.assertTrue(sequence.contains(leg2));
        Assert.assertTrue(sequence.contains(leg3));

        // ================ Test Copy method ================

        // Standard
        StrictLegsSequence<Leg> truncatedSeq = sequence.copy(new AbsoluteDateInterval(datehalf12,
                datehalf34));
        Assert.assertEquals(3, truncatedSeq.size());
        Assert.assertTrue(truncatedSeq.getTimeInterval().contains(datehalf12));
        Assert.assertTrue(truncatedSeq.getTimeInterval().contains(datehalf34));
        // For StrictLegsSequence Legs are considered to have closed boundaries, so is must not
        // change whether we ask for strict copy or not
        truncatedSeq = sequence.copy(new AbsoluteDateInterval(datehalf12, datehalf34), true);
        Assert.assertEquals(3, truncatedSeq.size());
        Assert.assertTrue(truncatedSeq.getTimeInterval().contains(datehalf12));
        Assert.assertTrue(truncatedSeq.getTimeInterval().contains(datehalf34));

        // Particular cases
        try {
            // Overlapping
            sequence.add(new LinearLeg(new AbsoluteDateInterval(date2, date4), 1, 2));
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        try {
            // Already added leg
            sequence.add(leg1);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        // ================ Test First method ================

        // Standard
        Assert.assertEquals(leg1, sequence.first());
        Assert.assertEquals(leg2, sequence.first(date2));
        // Within
        Assert.assertEquals(leg3, sequence.first(date2.shiftedBy(1.)));
        // Before sequence
        Assert.assertEquals(leg1, sequence.first(date1.shiftedBy(-1.)));
        // After sequence
        Assert.assertEquals(null, sequence.first(date3.shiftedBy(1.)));

        // ================ Test Last method ================

        // Standard
        Assert.assertEquals(leg3, sequence.last());
        Assert.assertEquals(leg1, sequence.last(date2));
        // Within
        Assert.assertEquals(leg1, sequence.last(date2.shiftedBy(1.)));
        // Before sequence
        Assert.assertEquals(null, sequence.last(date1.shiftedBy(-1.)));
        // After sequence
        Assert.assertEquals(leg2, sequence.last(date3.shiftedBy(1.)));
        Assert.assertEquals(leg3, sequence.last(date4.shiftedBy(1.)));

        // ================ Test Simultaneous method ================

        // Standard
        Assert.assertEquals(leg1, sequence.simultaneous(leg1).iterator().next());
        Assert.assertEquals(1, sequence.simultaneous(leg1).size());
        Assert.assertEquals(leg2, sequence.simultaneous(leg2).iterator().next());
        Assert.assertEquals(leg3, sequence.simultaneous(leg3).iterator().next());
        Assert.assertEquals(leg1, sequence.simultaneous(date1).iterator().next());
        Assert.assertEquals(leg2, sequence.simultaneous(date2).iterator().next());
        Assert.assertEquals(1, sequence.simultaneous(date2).size());
        // Exception
        try {
            // Non-existent leg
            sequence.simultaneous(new LinearLeg(new AbsoluteDateInterval(date2, date4), 1, 2));
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        // ================ Test Previous method ================

        // Standard
        Assert.assertEquals(null, sequence.previous(leg1));
        Assert.assertEquals(leg1, sequence.previous(leg2));
        // Exception
        try {
            // Non-existent leg
            sequence.previous(new LinearLeg(new AbsoluteDateInterval(date2, date4), 1, 2));
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        // ================ Test Next method ================

        // Standard
        Assert.assertEquals(null, sequence.next(leg3));
        Assert.assertEquals(leg3, sequence.next(leg2));
        // Exception
        try {
            // Non-existent leg
            sequence.next(new LinearLeg(new AbsoluteDateInterval(date2, date4), 1, 2));
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        // ================ Test Head method ================

        // Standard
        Assert.assertEquals(2, sequence.head(leg2).size());
        Assert.assertEquals(1, sequence.head(leg1).size());
        Assert.assertEquals(2, sequence.head(leg2.getDate()).size());
        Assert.assertEquals(1, sequence.head(leg2.getDate(), true).size());
        // Exception
        try {
            // Non-existent leg
            sequence.head(new LinearLeg(new AbsoluteDateInterval(date2, date4), 1, 2));
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        // ================ Test Tail method ================

        // Standard
        Assert.assertEquals(2, sequence.tail(leg2).size());
        Assert.assertEquals(1, sequence.tail(leg3).size());
        Assert.assertEquals(2, sequence.tail(leg2.getDate()).size());
        Assert.assertEquals(1, sequence.tail(leg2.getDate(), true).size());
        // Exception
        try {
            // Non-existent leg
            sequence.tail(new LinearLeg(new AbsoluteDateInterval(date2, date4), 1, 2));
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        // ================ Test Sub method ================

        // Standard
        Assert.assertEquals(2, sequence.sub(leg1, leg2).size());
        Assert.assertEquals(1, sequence.sub(leg2, leg2).size());
        Assert.assertEquals(2, sequence.sub(leg1.getDate(), leg2.getDate()).size());
        Assert.assertEquals(0, sequence.sub(leg1.getDate(), leg2.getDate(), true).size());
        Assert.assertEquals(2,
                sequence.sub(new AbsoluteDateInterval(leg1.getDate(), leg2.getDate())).size());
        Assert.assertEquals(0,
                sequence.sub(new AbsoluteDateInterval(leg1.getDate(), leg2.getDate()), true).size());

        // Exception
        try {
            // Keys in reverse order
            sequence.sub(leg2, leg1);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        try {
            // Non-existent leg
            sequence.sub(leg1, new LinearLeg(new AbsoluteDateInterval(date2, date4), 1, 2));
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        // ================ Test IsEmpty method ================

        // Standard
        Assert.assertFalse(sequence.isEmpty());
        Assert.assertFalse(sequence.isEmpty(date1, date2));
        Assert.assertFalse(sequence.isEmpty(date1, date3));
        Assert.assertFalse(sequence.isEmpty(date1.shiftedBy(-1.), date1.shiftedBy(1.)));
        Assert.assertTrue(sequence.isEmpty(date1.shiftedBy(-2.), date1.shiftedBy(-1.)));

        // ================ Test Clear method ================

        sequence.clear();

        // Checks
        Assert.assertEquals(0, sequence.size());

        // ================ Test remove, removeAll method ================

        sequence.add(leg1);
        sequence.add(leg2);
        sequence.add(leg3);
        sequence.remove(leg1);

        // Checks
        Assert.assertFalse(sequence.contains(leg1));
        Assert.assertTrue(sequence.contains(leg2));
        Assert.assertTrue(sequence.contains(leg3));

        final List<Leg> collection = new ArrayList<Leg>();
        collection.add(leg1);
        collection.add(leg2);
        collection.add(leg3);
        sequence.removeAll(collection);

        // Checks
        Assert.assertFalse(sequence.contains(leg1));
        Assert.assertFalse(sequence.contains(leg2));
        Assert.assertFalse(sequence.contains(leg3));

        // ================ Test toArray method ================

        sequence.add(leg1);
        sequence.add(leg2);
        sequence.add(leg3);

        final Object[] array1 = sequence.toArray();
        final Object[] array2 = sequence.toArray(new Leg[0]);
        Assert.assertEquals(3, array1.length);
        Assert.assertEquals(3, array2.length);
        Assert.assertEquals(leg1, array1[0]);
        Assert.assertEquals(leg2, array1[1]);
        Assert.assertEquals(leg3, array1[2]);
        Assert.assertEquals(leg1, array2[0]);
        Assert.assertEquals(leg2, array2[1]);
        Assert.assertEquals(leg3, array2[2]);

        // ================ Test toPrettyString method ================

        final String prettyString1 = sequence.toPrettyString();
        final String[] linesPrettyString1 = prettyString1.split("\n");
        Assert.assertEquals(201, prettyString1.length());
        Assert.assertEquals("1 : " + leg1.toPrettyString(), linesPrettyString1[1]);
        Assert.assertEquals("2 : " + leg2.toPrettyString(), linesPrettyString1[2]);
        Assert.assertEquals("3 : " + leg3.toPrettyString(), linesPrettyString1[3]);

        // ================ Test toString method ================

        final String string1 = sequence.toPrettyString();
        final String[] linesString1 = string1.split("\n");
        Assert.assertEquals(201, string1.length());
        Assert.assertEquals("1 : " + leg1.toPrettyString(), linesString1[1]);
        Assert.assertEquals("2 : " + leg2.toPrettyString(), linesString1[2]);
        Assert.assertEquals("3 : " + leg3.toPrettyString(), linesString1[3]);

        // ================ Test getTimeInterval method ================

        Assert.assertEquals(date1, sequence.getTimeInterval().getLowerData());
        Assert.assertEquals(date4, sequence.getTimeInterval().getUpperData());
    }

    /**
     * @testType UT
     *
     * @testedFeature {@link features#LEGS_SEQUENCE}
     *
     * @testedMethod all legs sequence methods
     *
     * @description test all functional methods of legs sequence
     *
     * @input a leg sequence with 2 segments separated by a hole
     *
     * @output output of all methods
     *
     * @testPassCriteria result is as expected (functional test)
     *
     * @referenceVersion 4.9
     *
     * @nonRegressionVersion 4.9
     */
    @Test
    public final void testSequenceWithHoles() throws PatriusException {

        // Build sequence [date1; date2] U [date2; date3] U [date3; date4]
        final StrictLegsSequence<Leg> sequence = new StrictLegsSequence<Leg>();
        final AbsoluteDate date1 = AbsoluteDate.J2000_EPOCH.shiftedBy(0.);
        final AbsoluteDate date2 = AbsoluteDate.J2000_EPOCH.shiftedBy(10.);
        final AbsoluteDate date3 = AbsoluteDate.J2000_EPOCH.shiftedBy(20.);
        final AbsoluteDate date4 = AbsoluteDate.J2000_EPOCH.shiftedBy(30.);
        final AbsoluteDate datehalf12 = AbsoluteDate.J2000_EPOCH.shiftedBy(5.);
        final AbsoluteDate datehalf34 = AbsoluteDate.J2000_EPOCH.shiftedBy(25.);
        final Leg leg1 = new LinearLeg(new AbsoluteDateInterval(date1, date2), 1, 2);
        final Leg leg3 = new LinearLeg(new AbsoluteDateInterval(date3, date4), 3, 4);

        // ================ Test Add method ================
        // Add in random order
        sequence.add(leg3);
        sequence.add(leg1);

        // Checks
        Assert.assertTrue(sequence.contains(leg1));
        Assert.assertTrue(sequence.contains(leg3));

        // Particular cases
        try {
            // Overlapping
            sequence.add(new LinearLeg(new AbsoluteDateInterval(date2, date4), 1, 2));
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        try {
            // Already added leg
            sequence.add(leg1);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        // ================ Test Copy method ================

        // Standard
        StrictLegsSequence<Leg> truncatedSeq = sequence.copy(new AbsoluteDateInterval(datehalf12,
                datehalf34));
        Assert.assertEquals(2, truncatedSeq.size());
        Assert.assertTrue(truncatedSeq.getTimeInterval().contains(datehalf12));
        Assert.assertTrue(truncatedSeq.getTimeInterval().contains(datehalf34));
        // For StrictLegsSequence Legs are considered to have closed boundaries, so is must not
        // change whether we ask for strict copy or not
        truncatedSeq = sequence.copy(new AbsoluteDateInterval(datehalf12, datehalf34), true);
        Assert.assertEquals(2, truncatedSeq.size());
        Assert.assertTrue(truncatedSeq.getTimeInterval().contains(datehalf12));
        Assert.assertTrue(truncatedSeq.getTimeInterval().contains(datehalf34));
        // Exception
        try {
            // Input interval not included
            final StrictLegsSequence<Leg> seq = sequence.copy(
                    new AbsoluteDateInterval(date2.shiftedBy(1), date3.shiftedBy(-1)), true);
            Assert.assertTrue(seq.isEmpty());
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        // ================ Test First method ================

        // Standard
        Assert.assertEquals(leg1, sequence.first());
        Assert.assertEquals(leg3, sequence.first(date2));
        // Within
        Assert.assertEquals(leg3, sequence.first(date2.shiftedBy(1.)));
        // Before sequence
        Assert.assertEquals(leg1, sequence.first(date1.shiftedBy(-1.)));
        // After sequence
        Assert.assertEquals(null, sequence.first(date3.shiftedBy(1.)));

        // ================ Test Last method ================

        // Standard
        Assert.assertEquals(leg3, sequence.last());
        Assert.assertEquals(leg1, sequence.last(date2));
        // Within
        Assert.assertEquals(leg1, sequence.last(date2.shiftedBy(1.)));
        // Before sequence
        Assert.assertEquals(null, sequence.last(date1.shiftedBy(-1.)));
        // After sequence
        Assert.assertEquals(leg1, sequence.last(date3.shiftedBy(1.)));
        Assert.assertEquals(leg3, sequence.last(date4.shiftedBy(1.)));

        // ================ Test Simultaneous method ================

        // Standard
        Assert.assertEquals(leg1, sequence.simultaneous(leg1).iterator().next());
        Assert.assertEquals(1, sequence.simultaneous(leg1).size());
        Assert.assertEquals(leg3, sequence.simultaneous(leg3).iterator().next());
        Assert.assertEquals(leg1, sequence.simultaneous(date1).iterator().next());
        Assert.assertEquals(0, sequence.simultaneous(date2).size());
        // Exception
        try {
            // Non-existent leg
            sequence.simultaneous(new LinearLeg(new AbsoluteDateInterval(date2, date4), 1, 2));
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        // ================ Test Previous method ================

        // Standard
        Assert.assertEquals(null, sequence.previous(leg1));
        Assert.assertEquals(leg1, sequence.previous(leg3));
        // Exception
        try {
            // Non-existent leg
            sequence.previous(new LinearLeg(new AbsoluteDateInterval(date2, date4), 1, 2));
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        // ================ Test Next method ================

        // Standard
        Assert.assertEquals(null, sequence.next(leg3));
        Assert.assertEquals(leg3, sequence.next(leg1));
        // Exception
        try {
            // Non-existent leg
            sequence.next(new LinearLeg(new AbsoluteDateInterval(date2, date4), 1, 2));
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        // ================ Test Head method ================

        // Standard
        Assert.assertEquals(2, sequence.head(leg3).size());
        Assert.assertEquals(1, sequence.head(leg1).size());
        Assert.assertEquals(1, sequence.head(leg1.getDate()).size());
        Assert.assertEquals(0, sequence.head(leg1.getDate(), true).size());
        // Exception
        try {
            // Non-existent leg
            sequence.head(new LinearLeg(new AbsoluteDateInterval(date2, date4), 1, 2));
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        // ================ Test Tail method ================

        // Standard
        Assert.assertEquals(2, sequence.tail(leg1).size());
        Assert.assertEquals(1, sequence.tail(leg3).size());
        Assert.assertEquals(1, sequence.tail(leg3.getDate()).size());
        Assert.assertEquals(0, sequence.tail(leg3.getDate(), true).size());
        // Exception
        try {
            // Non-existent leg
            sequence.tail(new LinearLeg(new AbsoluteDateInterval(date2, date4), 1, 2));
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        // ================ Test Sub method ================

        // Standard
        Assert.assertEquals(2, sequence.sub(leg1, leg3).size());
        Assert.assertEquals(1, sequence.sub(leg3, leg3).size());
        Assert.assertEquals(1, sequence.sub(leg3.getDate(), leg3.getDate()).size());
        Assert.assertEquals(0, sequence.sub(leg3.getDate(), leg3.getDate(), true).size());
        Assert.assertEquals(1,
                sequence.sub(new AbsoluteDateInterval(leg3.getDate(), leg3.getDate())).size());
        Assert.assertEquals(0,
                sequence.sub(new AbsoluteDateInterval(leg3.getDate(), leg3.getDate()), true).size());

        // Exception
        try {
            // Keys in reverse order
            sequence.sub(leg3, leg1);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        try {
            // Non-existent leg
            sequence.sub(leg1, new LinearLeg(new AbsoluteDateInterval(date2, date4), 1, 2));
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        // ================ Test IsEmpty method ================

        // Standard
        Assert.assertFalse(sequence.isEmpty());
        Assert.assertFalse(sequence.isEmpty(date1, date2));
        Assert.assertFalse(sequence.isEmpty(date1, date3));
        Assert.assertFalse(sequence.isEmpty(date1.shiftedBy(-1.), date1.shiftedBy(1.)));
        Assert.assertTrue(sequence.isEmpty(date1.shiftedBy(-2.), date1.shiftedBy(-1.)));

        // ================ Test Clear method ================

        sequence.clear();

        // Checks
        Assert.assertEquals(0, sequence.size());

        // ================ Test remove, removeAll method ================

        sequence.add(leg1);
        sequence.add(leg3);
        sequence.remove(leg1);

        // Checks
        Assert.assertFalse(sequence.contains(leg1));
        Assert.assertTrue(sequence.contains(leg3));

        final List<Leg> collection = new ArrayList<Leg>();
        collection.add(leg1);
        collection.add(leg3);
        sequence.removeAll(collection);

        // Checks
        Assert.assertFalse(sequence.contains(leg1));
        Assert.assertFalse(sequence.contains(leg3));

        // ================ Test toArray method ================

        sequence.add(leg1);
        sequence.add(leg3);

        final Object[] array1 = sequence.toArray();
        final Object[] array2 = sequence.toArray(new Leg[0]);
        Assert.assertEquals(2, array1.length);
        Assert.assertEquals(2, array2.length);
        Assert.assertEquals(leg1, array1[0]);
        Assert.assertEquals(leg3, array1[1]);
        Assert.assertEquals(leg1, array2[0]);
        Assert.assertEquals(leg3, array2[1]);

        // ================ Test toPrettyString method ================

        final String prettyString1 = sequence.toPrettyString();
        final String[] linesPrettyString1 = prettyString1.split("\n");
        Assert.assertEquals(137, prettyString1.length());
        Assert.assertEquals("1 : " + leg1.toPrettyString(), linesPrettyString1[1]);
        Assert.assertEquals("2 : " + leg3.toPrettyString(), linesPrettyString1[2]);

        // ================ Test toString method ================

        final String string1 = sequence.toPrettyString();
        final String[] linesString1 = string1.split("\n");
        Assert.assertEquals(137, string1.length());
        Assert.assertEquals("1 : " + leg1.toPrettyString(), linesString1[1]);
        Assert.assertEquals("2 : " + leg3.toPrettyString(), linesString1[2]);

        // ================ Test getTimeInterval method ================

        Assert.assertEquals(date1, sequence.getTimeInterval().getLowerData());
        Assert.assertEquals(date4, sequence.getTimeInterval().getUpperData());
    }

    /**
     * @testType UT
     *
     * @testedFeature {@link features#LEGS_SEQUENCE}
     *
     * @testedMethod all legs sequence methods
     *
     * @description test all functional methods of legs sequence
     *
     * @input a standard continuous leg sequence with 3 segments, from past to future infinity
     *
     * @output output of all methods
     *
     * @testPassCriteria result is as expected (functional test)
     *
     * @referenceVersion 4.9
     *
     * @nonRegressionVersion 4.9
     */
    @Test
    public final void testSequenceWithInfiniteBoundaries() throws PatriusException {

        // Build sequence [date1; date2] U [date2; date3] U [date3; date4]
        final StrictLegsSequence<Leg> sequence = new StrictLegsSequence<Leg>();
        final AbsoluteDate date1 = AbsoluteDate.PAST_INFINITY;
        final AbsoluteDate date2 = AbsoluteDate.J2000_EPOCH.shiftedBy(10.);
        final AbsoluteDate date3 = AbsoluteDate.J2000_EPOCH.shiftedBy(20.);
        final AbsoluteDate date4 = AbsoluteDate.FUTURE_INFINITY;
        final Leg leg1 = new LinearLeg(new AbsoluteDateInterval(date1, date2), 1, 2);
        final Leg leg2 = new LinearLeg(new AbsoluteDateInterval(date2, date3), 2, 3);
        final Leg leg3 = new LinearLeg(new AbsoluteDateInterval(date3, date4), 3, 4);

        // ================ Test Add method ================
        // Add in random order
        sequence.add(leg3);
        sequence.add(leg1);
        sequence.add(leg2);

        // Checks
        Assert.assertTrue(sequence.contains(leg1));
        Assert.assertTrue(sequence.contains(leg2));
        Assert.assertTrue(sequence.contains(leg3));

        // Particular cases
        try {
            // Overlapping
            sequence.add(new LinearLeg(new AbsoluteDateInterval(date2, date4), 1, 2));
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        try {
            // Already added leg
            sequence.add(leg1);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        // ================ Test Copy method ================

        // Standard
        final AbsoluteDate datePast = date2.shiftedBy(-1000);
        final AbsoluteDate dateFuture = date3.shiftedBy(1000);
        final StrictLegsSequence<Leg> truncatedSeq = sequence.copy(new AbsoluteDateInterval(
                datePast, dateFuture));
        Assert.assertEquals(3, truncatedSeq.size());
        Assert.assertTrue(truncatedSeq.getTimeInterval().contains(datePast));
        Assert.assertTrue(truncatedSeq.getTimeInterval().contains(dateFuture));
        Assert.assertFalse(truncatedSeq.getTimeInterval().contains(date4));

        // ================ Test First method ================

        // Standard
        Assert.assertEquals(leg1, sequence.first());
        Assert.assertEquals(leg2, sequence.first(date2));
        // Within
        Assert.assertEquals(leg3, sequence.first(date2.shiftedBy(1.)));
        // Before sequence
        Assert.assertEquals(leg1, sequence.first(date1.shiftedBy(-1.)));
        // After sequence
        Assert.assertEquals(null, sequence.first(date3.shiftedBy(1.)));

        // ================ Test Last method ================

        // Standard
        Assert.assertEquals(leg3, sequence.last());
        Assert.assertEquals(leg1, sequence.last(date2));
        // Within
        Assert.assertEquals(leg1, sequence.last(date2.shiftedBy(1.)));
        // Before sequence
        Assert.assertEquals(null, sequence.last(date1.shiftedBy(-1.)));
        // After sequence
        Assert.assertEquals(leg2, sequence.last(date3.shiftedBy(1.)));
        Assert.assertEquals(leg3, sequence.last(date4.shiftedBy(1.)));

        // ================ Test Simultaneous method ================

        // Standard
        Assert.assertEquals(leg1, sequence.simultaneous(leg1).iterator().next());
        Assert.assertEquals(1, sequence.simultaneous(leg1).size());
        Assert.assertEquals(leg2, sequence.simultaneous(leg2).iterator().next());
        Assert.assertEquals(leg3, sequence.simultaneous(leg3).iterator().next());
        Assert.assertEquals(leg1, sequence.simultaneous(date1).iterator().next());
        Assert.assertEquals(leg2, sequence.simultaneous(date2).iterator().next());
        Assert.assertEquals(1, sequence.simultaneous(date2).size());
        // Exception
        try {
            // Non-existent leg
            sequence.simultaneous(new LinearLeg(new AbsoluteDateInterval(date2, date4), 1, 2));
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        // ================ Test Previous method ================

        // Standard
        Assert.assertEquals(null, sequence.previous(leg1));
        Assert.assertEquals(leg1, sequence.previous(leg2));
        // Exception
        try {
            // Non-existent leg
            sequence.previous(new LinearLeg(new AbsoluteDateInterval(date2, date4), 1, 2));
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        // ================ Test Next method ================

        // Standard
        Assert.assertEquals(null, sequence.next(leg3));
        Assert.assertEquals(leg3, sequence.next(leg2));
        // Exception
        try {
            // Non-existent leg
            sequence.next(new LinearLeg(new AbsoluteDateInterval(date2, date4), 1, 2));
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        // ================ Test Head method ================

        // Standard
        Assert.assertEquals(2, sequence.head(leg2).size());
        Assert.assertEquals(1, sequence.head(leg1).size());
        Assert.assertEquals(2, sequence.head(leg2.getDate()).size());
        Assert.assertEquals(1, sequence.head(leg2.getDate(), true).size());
        // Exception
        try {
            // Non-existent leg
            sequence.head(new LinearLeg(new AbsoluteDateInterval(date2, date4), 1, 2));
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        // ================ Test Tail method ================

        // Standard
        Assert.assertEquals(2, sequence.tail(leg2).size());
        Assert.assertEquals(1, sequence.tail(leg3).size());
        Assert.assertEquals(2, sequence.tail(leg2.getDate()).size());
        Assert.assertEquals(1, sequence.tail(leg2.getDate(), true).size());
        // Exception
        try {
            // Non-existent leg
            sequence.tail(new LinearLeg(new AbsoluteDateInterval(date2, date4), 1, 2));
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        // ================ Test Sub method ================

        // Standard
        Assert.assertEquals(2, sequence.sub(leg1, leg2).size());
        Assert.assertEquals(1, sequence.sub(leg2, leg2).size());
        Assert.assertEquals(2, sequence.sub(leg1.getDate(), leg2.getDate()).size());
        Assert.assertEquals(0, sequence.sub(leg1.getDate(), leg2.getDate(), true).size());
        Assert.assertEquals(2,
                sequence.sub(new AbsoluteDateInterval(leg1.getDate(), leg2.getDate())).size());
        Assert.assertEquals(0,
                sequence.sub(new AbsoluteDateInterval(leg1.getDate(), leg2.getDate()), true).size());

        // Exception
        try {
            // Keys in reverse order
            sequence.sub(leg2, leg1);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        try {
            // Non-existent leg
            sequence.sub(leg1, new LinearLeg(new AbsoluteDateInterval(date2, date4), 1, 2));
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        // ================ Test IsEmpty method ================

        // Standard
        Assert.assertFalse(sequence.isEmpty());
        Assert.assertFalse(sequence.isEmpty(date1, date2));
        Assert.assertFalse(sequence.isEmpty(date1, date3));
        Assert.assertFalse(sequence.isEmpty(date1.shiftedBy(-1.), date1.shiftedBy(1.)));
        Assert.assertFalse(sequence.isEmpty(date1.shiftedBy(-2.), date1.shiftedBy(-1.)));

        // ================ Test Clear method ================

        sequence.clear();

        // Checks
        Assert.assertEquals(0, sequence.size());

        // ================ Test remove, removeAll method ================

        sequence.add(leg1);
        sequence.add(leg2);
        sequence.add(leg3);
        sequence.remove(leg1);

        // Checks
        Assert.assertFalse(sequence.contains(leg1));
        Assert.assertTrue(sequence.contains(leg2));
        Assert.assertTrue(sequence.contains(leg3));

        final List<Leg> collection = new ArrayList<Leg>();
        collection.add(leg1);
        collection.add(leg2);
        collection.add(leg3);
        sequence.removeAll(collection);

        // Checks
        Assert.assertFalse(sequence.contains(leg1));
        Assert.assertFalse(sequence.contains(leg2));
        Assert.assertFalse(sequence.contains(leg3));

        // ================ Test toArray method ================

        sequence.add(leg1);
        sequence.add(leg2);
        sequence.add(leg3);

        final Object[] array1 = sequence.toArray();
        final Object[] array2 = sequence.toArray(new Leg[0]);
        Assert.assertEquals(3, array1.length);
        Assert.assertEquals(3, array2.length);
        Assert.assertEquals(leg1, array1[0]);
        Assert.assertEquals(leg2, array1[1]);
        Assert.assertEquals(leg3, array1[2]);
        Assert.assertEquals(leg1, array2[0]);
        Assert.assertEquals(leg2, array2[1]);
        Assert.assertEquals(leg3, array2[2]);

        // ================ Test toPrettyString method ================

        final String prettyString1 = sequence.toPrettyString();
        final String[] linesPrettyString1 = prettyString1.split("\n");
        Assert.assertEquals(183, prettyString1.length());
        Assert.assertEquals("1 : " + leg1.toPrettyString(), linesPrettyString1[1]);
        Assert.assertEquals("2 : " + leg2.toPrettyString(), linesPrettyString1[2]);
        Assert.assertEquals("3 : " + leg3.toPrettyString(), linesPrettyString1[3]);

        // ================ Test toString method ================

        final String string1 = sequence.toPrettyString();
        final String[] linesString1 = string1.split("\n");
        Assert.assertEquals(183, string1.length());
        Assert.assertEquals("1 : " + leg1.toPrettyString(), linesString1[1]);
        Assert.assertEquals("2 : " + leg2.toPrettyString(), linesString1[2]);
        Assert.assertEquals("3 : " + leg3.toPrettyString(), linesString1[3]);

        // ================ Test getTimeInterval method ================

        Assert.assertEquals(date1, sequence.getTimeInterval().getLowerData());
        Assert.assertEquals(date4, sequence.getTimeInterval().getUpperData());
    }

    /**
     * A simple linear a.x + b leg.
     */
    private class LinearLeg implements Leg {

        /** a. */
        private final double a;

        /** b. */
        private final double b;

        /** Time interval. */
        private final AbsoluteDateInterval timeInterval;

        /**
         * Constructor
         *
         * @param timeInterval time interval of the profile
         * @param a of (slope of linear function)
         * @param b of (0-value of linear function)
         */
        public LinearLeg(final AbsoluteDateInterval timeInterval, final double a, final double b) {
            this.a = a;
            this.b = b;
            this.timeInterval = timeInterval;
        }

        @Override
        public AbsoluteDate getDate() {
            return this.timeInterval.getLowerData();
        }

        @Override
        public AbsoluteDateInterval getTimeInterval() {
            return this.timeInterval;
        }

        @Override
        public LinearLeg copy(final AbsoluteDateInterval newInterval) {
            return new LinearLeg(newInterval, this.a,
                    this.b
                            + ((newInterval.getLowerData().durationFrom(getTimeInterval()
                                    .getLowerData())) * this.a));
        }
    }
}
