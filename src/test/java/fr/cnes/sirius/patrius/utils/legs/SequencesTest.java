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
 * VERSION:4.9:DM:DM-3154:10/05/2022:[PATRIUS] Amelioration des methodes permettant l'extraction d'une sous-sequence 
 * VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.utils.legs;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;

/**
 * Test class for {@link Sequences} class.
 * 
 * @author Emmanuel Bignon
 * 
 * @since 4.8
 */
public class SequencesTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle sequences handling
         * 
         * @featureDescription object describing sequences
         * 
         * @coveredRequirements DM-3044
         */
        LEGS_SEQUENCE
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#LEGS_SEQUENCE}
     * 
     * @testedMethod {@link Sequences#emptyTimeSequence()}
     * 
     * @description check that an empty time sequence is properly created
     * 
     * @input nothing
     * 
     * @output an empty time sequence
     * 
     * @testPassCriteria result is as expected (functional test)
     * 
     * @referenceVersion 4.8
     * 
     * @nonRegressionVersion 4.8
     */
    @Test
    public final void testEmptyTimeSequence() {
        // Create an empty legs sequence
        final TimeSequence<Leg> sequence = Sequences.emptyTimeSequence();

        final AbsoluteDate date1 = AbsoluteDate.J2000_EPOCH.shiftedBy(0.);
        final AbsoluteDate date2 = AbsoluteDate.J2000_EPOCH.shiftedBy(10.);
        final Leg leg1 = new LinearLeg(new AbsoluteDateInterval(date1, date2), 1, 2);

        // ================ Test Add method ================
        
        try {
            sequence.add(leg1);
            Assert.fail();
        } catch (final UnsupportedOperationException e) {
            Assert.assertTrue(true);
        }
        
        // ================ Test First method ================
        
        Assert.assertNull(sequence.first());
        Assert.assertNull(sequence.first(AbsoluteDate.J2000_EPOCH));

        // ================ Test Last method ================
        
        Assert.assertNull(sequence.last());
        Assert.assertNull(sequence.last(AbsoluteDate.J2000_EPOCH));

        // ================ Test Simultaneous method ================
        
        Assert.assertTrue(sequence.simultaneous(leg1).isEmpty());
        Assert.assertTrue(sequence.simultaneous(AbsoluteDate.J2000_EPOCH).isEmpty());

        // ================ Test Previous method ================

        Assert.assertNull(sequence.previous(leg1));

        // ================ Test Next method ================

        Assert.assertNull(sequence.next(leg1));

        // ================ Test Head method ================

        Assert.assertTrue(sequence.head(leg1).isEmpty());

        // ================ Test Tail method ================

        Assert.assertTrue(sequence.tail(leg1).isEmpty());

        // ================ Test Sub method ================

        Assert.assertTrue(sequence.sub(leg1, leg1).isEmpty());

        // ================ Test IsEmpty method ================
        
        // Standard
        Assert.assertTrue(sequence.isEmpty());

        // ================ Test size method ================
        
        Assert.assertEquals(0, sequence.size());

        // ================ Test contains/containsAll method ================
        
        final List<Leg> collection1 = new ArrayList<Leg>();
        collection1.add(leg1);

        Assert.assertEquals(false, sequence.contains(leg1));
        Assert.assertEquals(false, sequence.containsAll(collection1));

        // ================ Test Clear method ================
        
        try {
            sequence.clear();
            Assert.fail();
        } catch (final UnsupportedOperationException e) {
            Assert.assertTrue(true);
        }

        // ================ Test AddAll method ================

        final List<Leg> collection = new ArrayList<Leg>();
        collection.add(leg1);
        
        try {
            sequence.addAll(collection);
            Assert.fail();
        } catch (final UnsupportedOperationException e) {
            Assert.assertTrue(true);
        }

        // ================ Test remove, removeAll method ================

        try {
            sequence.remove(leg1);
            Assert.fail();
        } catch (final UnsupportedOperationException e) {
            Assert.assertTrue(true);
        }

        try {
            sequence.removeAll(collection);
            Assert.fail();
        } catch (final UnsupportedOperationException e) {
            Assert.assertTrue(true);
        }

        // ================ Test retainAll method ================

        try {
            sequence.retainAll(collection);
            Assert.fail();
        } catch (final UnsupportedOperationException e) {
            Assert.assertTrue(true);
        }
        
        // ================ Test toArray method ================

        final Object[] array1 = sequence.toArray();
        final Object[] array2 = sequence.toArray(new Leg[0]);
        final Object[] array3 = sequence.toArray(new Leg[1]);
        Assert.assertEquals(0, array1.length);
        Assert.assertEquals(0, array2.length);
        Assert.assertEquals(1, array3.length);

        // ================ Test iterator method ================

        Assert.assertNotNull(sequence.iterator());
        Assert.assertFalse(sequence.iterator().hasNext());
        try {
            sequence.iterator().next();
            Assert.fail();
        } catch (final NoSuchElementException e) {
            Assert.assertTrue(true);
        }

        // ================ Test copy method ================

        Assert.assertNotNull(sequence.copy());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#LEGS_SEQUENCE}
     * 
     * @testedMethod {@link Sequences#emptyLegsSequence()}
     * 
     * @description check that an empty legs sequence is properly created
     * 
     * @input nothing
     * 
     * @output an empty sequence
     * 
     * @testPassCriteria result is as expected (functional test)
     * 
     * @referenceVersion 4.8
     * 
     * @nonRegressionVersion 4.8
     */
    @Test
    public final void testEmptyLegsSequence() {
        // Create an empty legs sequence
        final LegsSequence<Leg> sequence = Sequences.emptyLegsSequence();

        final AbsoluteDate date1 = AbsoluteDate.J2000_EPOCH.shiftedBy(0.);
        final AbsoluteDate date2 = AbsoluteDate.J2000_EPOCH.shiftedBy(10.);
        final Leg leg1 = new LinearLeg(new AbsoluteDateInterval(date1, date2), 1, 2);

        // ================ Test Add method ================
        
        try {
            sequence.add(leg1);
            Assert.fail();
        } catch (final UnsupportedOperationException e) {
            Assert.assertTrue(true);
        }
        
        // ================ Test Current method ================
        
        Assert.assertNull(sequence.current(AbsoluteDate.J2000_EPOCH));

        // ================ Test First method ================
        
        Assert.assertNull(sequence.first());
        Assert.assertNull(sequence.first(AbsoluteDate.J2000_EPOCH));

        // ================ Test Last method ================
        
        Assert.assertNull(sequence.last());
        Assert.assertNull(sequence.last(AbsoluteDate.J2000_EPOCH));

        // ================ Test Simultaneous method ================
        
        Assert.assertTrue(sequence.simultaneous(leg1).isEmpty());
        Assert.assertTrue(sequence.simultaneous(AbsoluteDate.J2000_EPOCH).isEmpty());

        // ================ Test Previous method ================

        Assert.assertNull(sequence.previous(leg1));

        // ================ Test Next method ================

        Assert.assertNull(sequence.next(leg1));

        // ================ Test Head method ================

        Assert.assertTrue(sequence.head(leg1).isEmpty());
        Assert.assertTrue(sequence.head(leg1.getDate()).isEmpty());
        Assert.assertTrue(sequence.head(leg1.getDate(), true).isEmpty());

        // ================ Test Tail method ================

        Assert.assertTrue(sequence.tail(leg1).isEmpty());
        Assert.assertTrue(sequence.tail(leg1.getDate()).isEmpty());
        Assert.assertTrue(sequence.tail(leg1.getDate(), true).isEmpty());

        // ================ Test Sub method ================

        Assert.assertTrue(sequence.sub(leg1, leg1).isEmpty());
        Assert.assertTrue(sequence.sub(leg1.getDate(), leg1.getDate()).isEmpty());
        Assert.assertTrue(sequence.sub(leg1.getDate(), leg1.getDate(), true).isEmpty());
        Assert.assertTrue(sequence.sub(new AbsoluteDateInterval(leg1.getDate(), leg1.getDate())).isEmpty());
        Assert.assertTrue(sequence.sub(new AbsoluteDateInterval(leg1.getDate(), leg1.getDate()), true).isEmpty());

        // ================ Test IsEmpty method ================
        
        // Standard
        Assert.assertTrue(sequence.isEmpty());
        Assert.assertTrue(sequence.isEmpty(date1, date2));

        // ================ Test size method ================
        
        Assert.assertEquals(0, sequence.size());

        // ================ Test contains/containsAll method ================
        
        final List<Leg> collection1 = new ArrayList<Leg>();
        collection1.add(leg1);

        Assert.assertEquals(false, sequence.contains(leg1));
        Assert.assertEquals(false, sequence.containsAll(collection1));

        // ================ Test Clear method ================
        
        try {
            sequence.clear();
            Assert.fail();
        } catch (final UnsupportedOperationException e) {
            Assert.assertTrue(true);
        }

        // ================ Test AddAll method ================

        final List<Leg> collection = new ArrayList<Leg>();
        collection.add(leg1);
        
        try {
            sequence.addAll(collection);
            Assert.fail();
        } catch (final UnsupportedOperationException e) {
            Assert.assertTrue(true);
        }

        // ================ Test remove, removeAll method ================

        try {
            sequence.remove(leg1);
            Assert.fail();
        } catch (final UnsupportedOperationException e) {
            Assert.assertTrue(true);
        }

        try {
            sequence.removeAll(collection);
            Assert.fail();
        } catch (final UnsupportedOperationException e) {
            Assert.assertTrue(true);
        }

        // ================ Test retainAll method ================

        try {
            sequence.retainAll(collection);
            Assert.fail();
        } catch (final UnsupportedOperationException e) {
            Assert.assertTrue(true);
        }
        
        // ================ Test toArray method ================

        final Object[] array1 = sequence.toArray();
        final Object[] array2 = sequence.toArray(new Leg[0]);
        final Object[] array3 = sequence.toArray(new Leg[1]);
        Assert.assertEquals(0, array1.length);
        Assert.assertEquals(0, array2.length);
        Assert.assertEquals(1, array3.length);

        // ================ Test getTimeInterval method ================

        Assert.assertNull(sequence.getTimeInterval());
        Assert.assertNull(sequence.getTimeInterval());

        // ================ Test iterator method ================

        Assert.assertNotNull(sequence.iterator());
        Assert.assertFalse(sequence.iterator().hasNext());
        try {
            sequence.iterator().next();
            Assert.fail();
        } catch (final NoSuchElementException e) {
            Assert.assertTrue(true);
        }

        // ================ Test copy method ================

        Assert.assertNotNull(sequence.copy());
        Assert.assertNotNull(sequence.copy(new AbsoluteDateInterval(date1, date2)));
        Assert.assertNotNull(sequence.copy(new AbsoluteDateInterval(date1, date2), false));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#LEGS_SEQUENCE}
     * 
     * @testedMethod all legs sequence methods
     * 
     * @description test all functional methods of legs sequence for un unmodifiable sequence
     * 
     * @input a standard continuous leg sequence with 3 segments
     * 
     * @output output of all methods
     * 
     * @testPassCriteria result is as expected (functional test)
     * 
     * @referenceVersion 4.8
     * 
     * @nonRegressionVersion 4.8
     */
    @Test
    public final void testUnmodifiableLegsSequence() {
        
        // Build sequence [date1; date2] U [date2; date3] U [date3; date4]
        final StrictLegsSequence<Leg> sequenceOriginal = new StrictLegsSequence<Leg>();
        final AbsoluteDate date1 = AbsoluteDate.J2000_EPOCH.shiftedBy(0.);
        final AbsoluteDate date2 = AbsoluteDate.J2000_EPOCH.shiftedBy(10.);
        final AbsoluteDate date3 = AbsoluteDate.J2000_EPOCH.shiftedBy(20.);
        final AbsoluteDate date4 = AbsoluteDate.J2000_EPOCH.shiftedBy(30.);
        final Leg leg1 = new LinearLeg(new AbsoluteDateInterval(date1, date2), 1, 2);
        final Leg leg2 = new LinearLeg(new AbsoluteDateInterval(date2, date3), 2, 3);
        final Leg leg3 = new LinearLeg(new AbsoluteDateInterval(date3, date4), 3, 4);
        sequenceOriginal.add(leg3);
        sequenceOriginal.add(leg1);
        sequenceOriginal.add(leg2);

        // Create unmodifiable sequence
        final LegsSequence<Leg> sequence = Sequences.unmodifiableLegsSequence(sequenceOriginal);

        // ================ Test Add method ================
        
        try {
            sequence.add(leg1);
            Assert.fail();
        } catch (final UnsupportedOperationException e) {
            Assert.assertTrue(true);
        }
        
        // ================ Test First method ================
        
        Assert.assertEquals(sequenceOriginal.first(), sequence.first());
        Assert.assertEquals(sequenceOriginal.first(AbsoluteDate.J2000_EPOCH), sequence.first(AbsoluteDate.J2000_EPOCH));

        // ================ Test Last method ================
        
        Assert.assertEquals(sequenceOriginal.last(), sequence.last());
        Assert.assertEquals(sequenceOriginal.last(AbsoluteDate.J2000_EPOCH), sequence.last(AbsoluteDate.J2000_EPOCH));

        // ================ Test Simultaneous method ================
        
        Assert.assertEquals(sequenceOriginal.simultaneous(leg1), sequence.simultaneous(leg1));
        Assert.assertEquals(sequenceOriginal.simultaneous(AbsoluteDate.J2000_EPOCH), sequence.simultaneous(AbsoluteDate.J2000_EPOCH));

        // ================ Test Previous method ================

        Assert.assertEquals(sequenceOriginal.previous(leg1), sequence.previous(leg1));

        // ================ Test Next method ================

        Assert.assertEquals(sequenceOriginal.next(leg1), sequence.next(leg1));

        // ================ Test Next method ================

        Assert.assertEquals(sequenceOriginal.current(AbsoluteDate.J2000_EPOCH), sequence.current(AbsoluteDate.J2000_EPOCH));

        // ================ Test Head method ================

        Assert.assertEquals(sequenceOriginal.head(leg1).size(), sequence.head(leg1).size());
        Assert.assertEquals(sequenceOriginal.head(leg1.getDate()).size(), sequence.head(leg1.getDate()).size());
        Assert.assertEquals(sequenceOriginal.head(leg1.getDate(), true).size(), sequence.head(leg1.getDate(), true).size());

        // ================ Test Tail method ================

        Assert.assertEquals(sequenceOriginal.tail(leg1).size(), sequence.tail(leg1).size());
        Assert.assertEquals(sequenceOriginal.tail(leg1.getDate()).size(), sequence.tail(leg1.getDate()).size());
        Assert.assertEquals(sequenceOriginal.tail(leg1.getDate(), true).size(), sequence.tail(leg1.getDate(), true).size());

        // ================ Test Sub method ================

        Assert.assertEquals(sequenceOriginal.sub(leg1, leg2).size(), sequence.sub(leg1, leg2).size());
        Assert.assertEquals(sequenceOriginal.sub(leg1.getDate(), leg2.getDate()).size(), sequence.sub(leg1.getDate(), leg2.getDate()).size());
        Assert.assertEquals(sequenceOriginal.sub(leg1.getDate(), leg2.getDate(), true).size(), sequence.sub(leg1.getDate(), leg2.getDate(), true).size());
        Assert.assertEquals(sequenceOriginal.sub(new AbsoluteDateInterval(leg1.getDate(), leg2.getDate())).size(), sequence.sub(new AbsoluteDateInterval(leg1.getDate(), leg2.getDate())).size());
        Assert.assertEquals(sequenceOriginal.sub(new AbsoluteDateInterval(leg1.getDate(), leg2.getDate()), true).size(), sequence.sub(new AbsoluteDateInterval(leg1.getDate(), leg2.getDate()), true).size());

        // ================ Test IsEmpty method ================
        
        Assert.assertEquals(sequenceOriginal.isEmpty(), sequence.isEmpty());
        Assert.assertEquals(sequenceOriginal.isEmpty(date1, date2), sequence.isEmpty(date1, date2));

        // ================ Test size method ================
        
        Assert.assertEquals(sequenceOriginal.size(), sequence.size());

        // ================ Test contains/containsAll method ================
        
        final List<Leg> collection1 = new ArrayList<Leg>();
        collection1.add(leg1);
        collection1.add(leg2);
        collection1.add(leg3);

        Assert.assertEquals(sequenceOriginal.contains(leg1), sequence.contains(leg1));
        Assert.assertEquals(sequenceOriginal.containsAll(collection1), sequence.containsAll(collection1));

        // ================ Test Clear method ================
        
        try {
            sequence.clear();
            Assert.fail();
        } catch (final UnsupportedOperationException e) {
            Assert.assertTrue(true);
        }

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

        try {
            sequence.remove(leg1);
            Assert.fail();
        } catch (final UnsupportedOperationException e) {
            Assert.assertTrue(true);
        }

        try {
            sequence.removeAll(collection);
            Assert.fail();
        } catch (final UnsupportedOperationException e) {
            Assert.assertTrue(true);
        }

        // ================ Test retainAll method ================

        try {
            sequence.retainAll(collection);
            Assert.fail();
        } catch (final UnsupportedOperationException e) {
            Assert.assertTrue(true);
        }
        
        // ================ Test toArray method ================

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

        // ================ Test getTimeInterval method ================

        Assert.assertEquals(sequenceOriginal.getTimeInterval().getLowerData(), sequence.getTimeInterval().getLowerData());
        Assert.assertEquals(sequenceOriginal.getTimeInterval().getUpperData(), sequence.getTimeInterval().getUpperData());

        // ================ Test iterator method ================

        Assert.assertNotNull(sequence.iterator());

        // ================ Test copy method ================

        Assert.assertNotNull(sequence.copy());
        Assert.assertNotNull(sequence.copy(new AbsoluteDateInterval(date1, date2)));
        Assert.assertNotNull(sequence.copy(new AbsoluteDateInterval(date1, date2), false));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#LEGS_SEQUENCE}
     * 
     * @testedMethod all legs sequence methods
     * 
     * @description test all functional methods of legs sequence for un unmodifiable sequence
     * 
     * @input a standard continuous leg sequence with 3 segments
     * 
     * @output output of all methods
     * 
     * @testPassCriteria result is as expected (functional test)
     * 
     * @referenceVersion 4.8
     * 
     * @nonRegressionVersion 4.8
     */
    @Test
    public final void testUnmodifiableTimeSequence() {
        
        // Build sequence [date1; date2] U [date2; date3] U [date3; date4]
        final StrictLegsSequence<Leg> sequenceOriginal = new StrictLegsSequence<Leg>();
        final AbsoluteDate date1 = AbsoluteDate.J2000_EPOCH.shiftedBy(0.);
        final AbsoluteDate date2 = AbsoluteDate.J2000_EPOCH.shiftedBy(10.);
        final AbsoluteDate date3 = AbsoluteDate.J2000_EPOCH.shiftedBy(20.);
        final AbsoluteDate date4 = AbsoluteDate.J2000_EPOCH.shiftedBy(30.);
        final Leg leg1 = new LinearLeg(new AbsoluteDateInterval(date1, date2), 1, 2);
        final Leg leg2 = new LinearLeg(new AbsoluteDateInterval(date2, date3), 2, 3);
        final Leg leg3 = new LinearLeg(new AbsoluteDateInterval(date3, date4), 3, 4);
        sequenceOriginal.add(leg3);
        sequenceOriginal.add(leg1);
        sequenceOriginal.add(leg2);

        // Create unmodifiable sequence
        final TimeSequence<Leg> sequence = Sequences.unmodifiableTimeSequence(sequenceOriginal);

        // ================ Test Add method ================
        
        try {
            sequence.add(leg1);
            Assert.fail();
        } catch (final UnsupportedOperationException e) {
            Assert.assertTrue(true);
        }
        
        // ================ Test First method ================
        
        Assert.assertEquals(sequenceOriginal.first(), sequence.first());
        Assert.assertEquals(sequenceOriginal.first(AbsoluteDate.J2000_EPOCH), sequence.first(AbsoluteDate.J2000_EPOCH));

        // ================ Test Last method ================
        
        Assert.assertEquals(sequenceOriginal.last(), sequence.last());
        Assert.assertEquals(sequenceOriginal.last(AbsoluteDate.J2000_EPOCH), sequence.last(AbsoluteDate.J2000_EPOCH));

        // ================ Test Simultaneous method ================
        
        Assert.assertEquals(sequenceOriginal.simultaneous(leg1), sequence.simultaneous(leg1));

        // ================ Test Previous method ================

        Assert.assertEquals(sequenceOriginal.previous(leg1), sequence.previous(leg1));

        // ================ Test Next method ================

        Assert.assertEquals(sequenceOriginal.next(leg1), sequence.next(leg1));

        // ================ Test Head method ================

        Assert.assertEquals(sequenceOriginal.head(leg1).size(), sequence.head(leg1).size());

        // ================ Test Tail method ================

        Assert.assertEquals(sequenceOriginal.tail(leg1).size(), sequence.tail(leg1).size());

        // ================ Test Sub method ================

        Assert.assertEquals(sequenceOriginal.sub(leg1, leg2).size(), sequence.sub(leg1, leg2).size());

        // ================ Test IsEmpty method ================
        
        Assert.assertEquals(sequenceOriginal.isEmpty(), sequence.isEmpty());

        // ================ Test size method ================
        
        Assert.assertEquals(sequenceOriginal.size(), sequence.size());

        // ================ Test contains/containsAll method ================
        
        final List<Leg> collection1 = new ArrayList<Leg>();
        collection1.add(leg1);
        collection1.add(leg2);
        collection1.add(leg3);

        Assert.assertEquals(sequenceOriginal.contains(leg1), sequence.contains(leg1));
        Assert.assertEquals(sequenceOriginal.containsAll(collection1), sequence.containsAll(collection1));

        // ================ Test Clear method ================
        
        try {
            sequence.clear();
            Assert.fail();
        } catch (final UnsupportedOperationException e) {
            Assert.assertTrue(true);
        }

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

        try {
            sequence.remove(leg1);
            Assert.fail();
        } catch (final UnsupportedOperationException e) {
            Assert.assertTrue(true);
        }

        try {
            sequence.removeAll(collection);
            Assert.fail();
        } catch (final UnsupportedOperationException e) {
            Assert.assertTrue(true);
        }

        // ================ Test retainAll method ================

        try {
            sequence.retainAll(collection);
            Assert.fail();
        } catch (final UnsupportedOperationException e) {
            Assert.assertTrue(true);
        }
        
        // ================ Test toArray method ================

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

        // ================ Test iterator method ================

        Assert.assertNotNull(sequence.iterator());

        // ================ Test copy method ================

        Assert.assertNotNull(sequence.copy());
    }

    /**
     * FA-3125: an unmodifiable sequence cannot be modified if the original sequence is modified
     */
    @Test
    public final void testUnmodifiableSequence() {
        // Legs sequence
        final LegsSequence<MyLeg> legsSequence = new StrictLegsSequence<>(); 
        final MyLeg myLeg1 = new MyLeg(new AbsoluteDateInterval(AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY)); 
        legsSequence.add(myLeg1); 

        final LegsSequence<MyLeg> unmodifiable = Sequences.unmodifiableLegsSequence(legsSequence); 
        Assert.assertTrue(unmodifiable.contains(myLeg1)); 
        legsSequence.remove(myLeg1);
        Assert.assertTrue(unmodifiable.contains(myLeg1));

        // Time sequence
        final TimeSequence<MyLeg> timesSequence = new StrictLegsSequence<>(); 
        final MyLeg myTimeLeg = new MyLeg(new AbsoluteDateInterval(AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY)); 
        timesSequence.add(myTimeLeg); 

        final TimeSequence<MyLeg> unmodifiableTimeSequence = Sequences.unmodifiableTimeSequence(timesSequence); 
        Assert.assertTrue(unmodifiableTimeSequence.contains(myTimeLeg)); 
        timesSequence.remove(myTimeLeg);
        Assert.assertTrue(unmodifiableTimeSequence.contains(myTimeLeg));
    }

    private class MyLeg implements Leg { 
        
        final AbsoluteDateInterval interval; 

        public MyLeg(final AbsoluteDateInterval interval) { 
            super(); 
            this.interval = interval; 
        } 

        @Override 
        public AbsoluteDateInterval getTimeInterval() { 
            return this.interval; 
        } 

        @Override 
        public Leg copy(final AbsoluteDateInterval newInterval) { 
            throw new UnsupportedOperationException(); 
        } 
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
        public LinearLeg(final AbsoluteDateInterval timeInterval,
            final double a, final double b) {
            this.a = a;
            this.b = b;
            this.timeInterval = timeInterval;
        }

        @Override
        public AbsoluteDate getDate() {
            return timeInterval.getLowerData();
        }

        @Override
        public AbsoluteDateInterval getTimeInterval() {
            return timeInterval;
        }

        @Override
        public LinearLeg copy(final AbsoluteDateInterval newInterval) {
            return new LinearLeg(newInterval, a, b + (newInterval.getLowerData().durationFrom(getTimeInterval().getLowerData())) * a);
        }
    }
}
