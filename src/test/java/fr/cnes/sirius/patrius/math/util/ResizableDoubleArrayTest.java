/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 *
 * Copyright 2011-2017 CNES
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
 */
package fr.cnes.sirius.patrius.math.util;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.distribution.IntegerDistribution;
import fr.cnes.sirius.patrius.math.distribution.UniformIntegerDistribution;
import fr.cnes.sirius.patrius.math.util.ResizableDoubleArray.ExpansionMode;

/**
 * This class contains test cases for the ResizableDoubleArray.
 * 
 * @version $Id: ResizableDoubleArrayTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class ResizableDoubleArrayTest extends DoubleArrayAbstractTest {

    @After
    public void tearDown() throws Exception {
        this.da = null;
        this.ra = null;
    }

    @Before
    public void setUp() throws Exception {
        this.da = new ResizableDoubleArray();
        this.ra = new ResizableDoubleArray();
    }

    @Test
    public void testConstructors() {
        ResizableDoubleArray testDa = new ResizableDoubleArray(2);
        Assert.assertEquals(0, testDa.getNumElements());
        Assert.assertEquals(2, testDa.getCapacity());

        try {
            this.da = new ResizableDoubleArray(-1);
            Assert.fail("Expecting IllegalArgumentException");
        } catch (final IllegalArgumentException ex) {
            // expected
        }

        testDa = new ResizableDoubleArray((double[]) null);
        Assert.assertEquals(0, testDa.getNumElements());

        final double[] initialArray = new double[] { 0, 1, 2 };
        testDa = new ResizableDoubleArray(initialArray);
        Assert.assertEquals(3, testDa.getNumElements());

        testDa = new ResizableDoubleArray(2, 2.0, 3.0);
        Assert.assertEquals(0, testDa.getNumElements());
        Assert.assertEquals(2, testDa.getCapacity());

        try {
            this.da = new ResizableDoubleArray(2, 2.0, 1.5);
            Assert.fail("Expecting IllegalArgumentException");
        } catch (final IllegalArgumentException ex) {
            // expected
        }

        testDa = new ResizableDoubleArray(2, 2.0, 3.0,
            ExpansionMode.ADDITIVE, null);
        Assert.assertEquals(0, testDa.getNumElements());
        Assert.assertEquals(2, testDa.getCapacity());

        // Copy constructor
        testDa = new ResizableDoubleArray(2, 2.0, 3.0,
            ExpansionMode.ADDITIVE, null);
        testDa.addElement(2.0);
        testDa.addElement(3.2);
        final ResizableDoubleArray copyDa = new ResizableDoubleArray(testDa);
        Assert.assertEquals(copyDa, testDa);
        Assert.assertEquals(testDa, copyDa);
    }

    @Test
    public void testSetElementArbitraryExpansion1() {

        // MULTIPLICATIVE_MODE
        this.da.addElement(2.0);
        this.da.addElement(4.0);
        this.da.addElement(6.0);
        this.da.setElement(1, 3.0);

        // Expand the array arbitrarily to 1000 items
        this.da.setElement(1000, 3.4);

        Assert.assertEquals("The number of elements should now be 1001, it isn't",
            this.da.getNumElements(), 1001);

        Assert.assertEquals("Uninitialized Elements are default value of 0.0, index 766 wasn't", 0.0,
            this.da.getElement(760), Double.MIN_VALUE);

        Assert.assertEquals("The 1000th index should be 3.4, it isn't", 3.4, this.da.getElement(1000),
            Double.MIN_VALUE);
        Assert.assertEquals("The 0th index should be 2.0, it isn't", 2.0, this.da.getElement(0),
            Double.MIN_VALUE);
    }

    @Test
    public void testSetElementArbitraryExpansion2() {
        // Make sure numElements and expansion work correctly for expansion boundary cases
        this.da.addElement(2.0);
        this.da.addElement(4.0);
        this.da.addElement(6.0);
        Assert.assertEquals(16, ((ResizableDoubleArray) this.da).getCapacity());
        Assert.assertEquals(3, this.da.getNumElements());
        this.da.setElement(3, 7.0);
        Assert.assertEquals(16, ((ResizableDoubleArray) this.da).getCapacity());
        Assert.assertEquals(4, this.da.getNumElements());
        this.da.setElement(10, 10.0);
        Assert.assertEquals(16, ((ResizableDoubleArray) this.da).getCapacity());
        Assert.assertEquals(11, this.da.getNumElements());
        this.da.setElement(9, 10.0);
        Assert.assertEquals(16, ((ResizableDoubleArray) this.da).getCapacity());
        Assert.assertEquals(11, this.da.getNumElements());

        try {
            this.da.setElement(-2, 3);
            Assert.fail("Expecting ArrayIndexOutOfBoundsException for negative index");
        } catch (final ArrayIndexOutOfBoundsException ex) {
            // expected
        }

        // ADDITIVE_MODE

        final ResizableDoubleArray testDa = new ResizableDoubleArray(2, 2.0, 3.0,
            ExpansionMode.ADDITIVE, null);
        Assert.assertEquals(2, testDa.getCapacity());
        testDa.addElement(1d);
        testDa.addElement(1d);
        Assert.assertEquals(2, testDa.getCapacity());
        testDa.addElement(1d);
        Assert.assertEquals(4, testDa.getCapacity());
    }

    @Override
    @Test
    public void testAdd1000() {
        super.testAdd1000();
        Assert.assertEquals("Internal Storage length should be 1024 if we started out with initial capacity of " +
            "16 and an expansion factor of 2.0",
            1024, ((ResizableDoubleArray) this.da).getCapacity());
    }

    @Test
    public void testAddElements() {
        ResizableDoubleArray testDa = new ResizableDoubleArray();

        // MULTIPLICATIVE_MODE
        testDa.addElements(new double[] { 4, 5, 6 });
        Assert.assertEquals(3, testDa.getNumElements(), 0);
        Assert.assertEquals(4, testDa.getElement(0), 0);
        Assert.assertEquals(5, testDa.getElement(1), 0);
        Assert.assertEquals(6, testDa.getElement(2), 0);

        testDa.addElements(new double[] { 4, 5, 6 });
        Assert.assertEquals(6, testDa.getNumElements());

        // ADDITIVE_MODE (x's are occupied storage locations, 0's are open)
        testDa = new ResizableDoubleArray(2, 2.0, 2.5,
            ExpansionMode.ADDITIVE, null);
        Assert.assertEquals(2, testDa.getCapacity());
        testDa.addElements(new double[] { 1d }); // x,0
        testDa.addElements(new double[] { 2d }); // x,x
        testDa.addElements(new double[] { 3d }); // x,x,x,0 -- expanded
        Assert.assertEquals(1d, testDa.getElement(0), 0);
        Assert.assertEquals(2d, testDa.getElement(1), 0);
        Assert.assertEquals(3d, testDa.getElement(2), 0);
        Assert.assertEquals(4, testDa.getCapacity()); // x,x,x,0
        Assert.assertEquals(3, testDa.getNumElements());
    }

    @Override
    @Test
    public void testAddElementRolling() {
        super.testAddElementRolling();

        // MULTIPLICATIVE_MODE
        this.da.clear();
        this.da.addElement(1);
        this.da.addElement(2);
        this.da.addElementRolling(3);
        Assert.assertEquals(3, this.da.getElement(1), 0);
        this.da.addElementRolling(4);
        Assert.assertEquals(3, this.da.getElement(0), 0);
        Assert.assertEquals(4, this.da.getElement(1), 0);
        this.da.addElement(5);
        Assert.assertEquals(5, this.da.getElement(2), 0);
        this.da.addElementRolling(6);
        Assert.assertEquals(4, this.da.getElement(0), 0);
        Assert.assertEquals(5, this.da.getElement(1), 0);
        Assert.assertEquals(6, this.da.getElement(2), 0);

        // ADDITIVE_MODE (x's are occupied storage locations, 0's are open)
        final ResizableDoubleArray testDa = new ResizableDoubleArray(2, 2.0, 2.5,
            ExpansionMode.ADDITIVE, null);
        Assert.assertEquals(2, testDa.getCapacity());
        testDa.addElement(1d); // x,0
        testDa.addElement(2d); // x,x
        testDa.addElement(3d); // x,x,x,0 -- expanded
        Assert.assertEquals(1d, testDa.getElement(0), 0);
        Assert.assertEquals(2d, testDa.getElement(1), 0);
        Assert.assertEquals(3d, testDa.getElement(2), 0);
        Assert.assertEquals(4, testDa.getCapacity()); // x,x,x,0
        Assert.assertEquals(3, testDa.getNumElements());
        testDa.addElementRolling(4d);
        Assert.assertEquals(2d, testDa.getElement(0), 0);
        Assert.assertEquals(3d, testDa.getElement(1), 0);
        Assert.assertEquals(4d, testDa.getElement(2), 0);
        Assert.assertEquals(4, testDa.getCapacity()); // 0,x,x,x
        Assert.assertEquals(3, testDa.getNumElements());
        testDa.addElementRolling(5d); // 0,0,x,x,x,0 -- time to contract
        Assert.assertEquals(3d, testDa.getElement(0), 0);
        Assert.assertEquals(4d, testDa.getElement(1), 0);
        Assert.assertEquals(5d, testDa.getElement(2), 0);
        Assert.assertEquals(4, testDa.getCapacity()); // contracted -- x,x,x,0
        Assert.assertEquals(3, testDa.getNumElements());
        try {
            testDa.getElement(4);
            Assert.fail("Expecting ArrayIndexOutOfBoundsException");
        } catch (final ArrayIndexOutOfBoundsException ex) {
            // expected
        }
        try {
            testDa.getElement(-1);
            Assert.fail("Expecting ArrayIndexOutOfBoundsException");
        } catch (final ArrayIndexOutOfBoundsException ex) {
            // expected
        }
    }

    @Test
    public void testSetNumberOfElements() {
        this.da.addElement(1.0);
        this.da.addElement(1.0);
        this.da.addElement(1.0);
        this.da.addElement(1.0);
        this.da.addElement(1.0);
        this.da.addElement(1.0);
        Assert.assertEquals("Number of elements should equal 6", this.da.getNumElements(), 6);

        ((ResizableDoubleArray) this.da).setNumElements(3);
        Assert.assertEquals("Number of elements should equal 3", this.da.getNumElements(), 3);

        try {
            ((ResizableDoubleArray) this.da).setNumElements(-3);
            Assert.fail("Setting number of elements to negative should've thrown an exception");
        } catch (final IllegalArgumentException iae) {
        }

        ((ResizableDoubleArray) this.da).setNumElements(1024);
        Assert.assertEquals("Number of elements should now be 1024", this.da.getNumElements(), 1024);
        Assert.assertEquals("Element 453 should be a default double", this.da.getElement(453), 0.0, Double.MIN_VALUE);

    }

    @Test
    public void testWithInitialCapacity() {

        final ResizableDoubleArray eDA2 = new ResizableDoubleArray(2);
        Assert.assertEquals("Initial number of elements should be 0", 0, eDA2.getNumElements());

        final IntegerDistribution randomData = new UniformIntegerDistribution(100, 1000);
        final int iterations = randomData.sample();

        for (int i = 0; i < iterations; i++) {
            eDA2.addElement(i);
        }

        Assert.assertEquals("Number of elements should be equal to " + iterations, iterations, eDA2.getNumElements());

        eDA2.addElement(2.0);

        Assert.assertEquals("Number of elements should be equals to " + (iterations + 1),
            iterations + 1, eDA2.getNumElements());
    }

    @Test
    public void testWithInitialCapacityAndExpansionFactor() {

        final ResizableDoubleArray eDA3 = new ResizableDoubleArray(3, 3.0, 3.5);
        Assert.assertEquals("Initial number of elements should be 0", 0, eDA3.getNumElements());

        final IntegerDistribution randomData = new UniformIntegerDistribution(100, 3000);
        final int iterations = randomData.sample();

        for (int i = 0; i < iterations; i++) {
            eDA3.addElement(i);
        }

        Assert.assertEquals("Number of elements should be equal to " + iterations, iterations, eDA3.getNumElements());

        eDA3.addElement(2.0);

        Assert.assertEquals("Number of elements should be equals to " + (iterations + 1),
            iterations + 1, eDA3.getNumElements());
    }

    @Test
    public void testDiscard() {
        this.da.addElement(2.0);
        this.da.addElement(2.0);
        this.da.addElement(2.0);
        this.da.addElement(2.0);
        this.da.addElement(2.0);
        this.da.addElement(2.0);
        this.da.addElement(2.0);
        this.da.addElement(2.0);
        this.da.addElement(2.0);
        this.da.addElement(2.0);
        this.da.addElement(2.0);
        Assert.assertEquals("Number of elements should be 11", 11, this.da.getNumElements());

        ((ResizableDoubleArray) this.da).discardFrontElements(5);
        Assert.assertEquals("Number of elements should be 6", 6, this.da.getNumElements());

        this.da.addElement(2.0);
        this.da.addElement(2.0);
        this.da.addElement(2.0);
        this.da.addElement(2.0);
        Assert.assertEquals("Number of elements should be 10", 10, this.da.getNumElements());

        ((ResizableDoubleArray) this.da).discardMostRecentElements(2);
        Assert.assertEquals("Number of elements should be 8", 8, this.da.getNumElements());

        try {
            ((ResizableDoubleArray) this.da).discardFrontElements(-1);
            Assert.fail("Trying to discard a negative number of element is not allowed");
        } catch (final Exception e) {
        }

        try {
            ((ResizableDoubleArray) this.da).discardMostRecentElements(-1);
            Assert.fail("Trying to discard a negative number of element is not allowed");
        } catch (final Exception e) {
        }

        try {
            ((ResizableDoubleArray) this.da).discardFrontElements(10000);
            Assert.fail("You can't discard more elements than the array contains");
        } catch (final Exception e) {
        }

        try {
            ((ResizableDoubleArray) this.da).discardMostRecentElements(10000);
            Assert.fail("You can't discard more elements than the array contains");
        } catch (final Exception e) {
        }

    }

    @Test
    public void testSubstitute() {

        this.da.addElement(2.0);
        this.da.addElement(2.0);
        this.da.addElement(2.0);
        this.da.addElement(2.0);
        this.da.addElement(2.0);
        this.da.addElement(2.0);
        this.da.addElement(2.0);
        this.da.addElement(2.0);
        this.da.addElement(2.0);
        this.da.addElement(2.0);
        this.da.addElement(2.0);
        Assert.assertEquals("Number of elements should be 11", 11, this.da.getNumElements());

        ((ResizableDoubleArray) this.da).substituteMostRecentElement(24);

        Assert.assertEquals("Number of elements should be 11", 11, this.da.getNumElements());

        try {
            ((ResizableDoubleArray) this.da).discardMostRecentElements(10);
        } catch (final Exception e) {
            Assert.fail("Trying to discard a negative number of element is not allowed");
        }

        ((ResizableDoubleArray) this.da).substituteMostRecentElement(24);

        Assert.assertEquals("Number of elements should be 1", 1, this.da.getNumElements());

    }

    @Test
    public void testEqualsAndHashCode() throws Exception {

        // Wrong type
        final ResizableDoubleArray first = new ResizableDoubleArray();
        Double other = new Double(2);
        Assert.assertFalse(first.equals(other));

        // Null
        other = null;
        Assert.assertFalse(first.equals(other));

        // Reflexive
        Assert.assertTrue(first.equals(first));

        // Argumentless constructor
        final ResizableDoubleArray second = new ResizableDoubleArray();
        this.verifyEquality(first, second);

        // Equals iff same data, same properties
        final ResizableDoubleArray third = new ResizableDoubleArray(3, 2.0, 2.0);
        this.verifyInequality(third, first);
        final ResizableDoubleArray fourth = new ResizableDoubleArray(3, 2.0, 2.0);
        final ResizableDoubleArray fifth = new ResizableDoubleArray(2, 2.0, 2.0);
        this.verifyEquality(third, fourth);
        this.verifyInequality(third, fifth);
        third.addElement(4.1);
        third.addElement(4.2);
        third.addElement(4.3);
        fourth.addElement(4.1);
        fourth.addElement(4.2);
        fourth.addElement(4.3);
        this.verifyEquality(third, fourth);

        // expand
        fourth.addElement(4.4);
        this.verifyInequality(third, fourth);
        third.addElement(4.4);
        this.verifyEquality(third, fourth);
        fourth.addElement(4.4);
        this.verifyInequality(third, fourth);
        third.addElement(4.4);
        this.verifyEquality(third, fourth);
        fourth.addElementRolling(4.5);
        third.addElementRolling(4.5);
        this.verifyEquality(third, fourth);

        // discard
        third.discardFrontElements(1);
        this.verifyInequality(third, fourth);
        fourth.discardFrontElements(1);
        this.verifyEquality(third, fourth);

        // discard recent
        third.discardMostRecentElements(2);
        fourth.discardMostRecentElements(2);
        this.verifyEquality(third, fourth);

        // wrong order
        third.addElement(18);
        fourth.addElement(17);
        third.addElement(17);
        fourth.addElement(18);
        this.verifyInequality(third, fourth);

        // copy
        ResizableDoubleArray.copy(fourth, fifth);
        this.verifyEquality(fourth, fifth);

        // Copy constructor
        this.verifyEquality(fourth, new ResizableDoubleArray(fourth));

        // Instance copy
        this.verifyEquality(fourth, fourth.copy());

    }

    @Test
    public void testGetArrayRef() {
        final ResizableDoubleArray a = new ResizableDoubleArray();

        // Modify "a" through the public API.
        final int index = 20;
        final double v1 = 1.2;
        a.setElement(index, v1);

        // Modify the internal storage through the protected API.
        final double v2 = v1 + 3.4;
        final double[] aInternalArray = a.getArrayRef();
        aInternalArray[a.getStartIndex() + index] = v2;

        Assert.assertEquals(v2, a.getElement(index), 0d);
    }

    @Test
    public void testCompute() {
        final ResizableDoubleArray a = new ResizableDoubleArray();
        final int max = 20;
        for (int i = 1; i <= max; i++) {
            a.setElement(i, i);
        }

        final MathArrays.Function add = new MathArrays.Function(){
            @Override
            public double evaluate(final double[] a, final int index, final int num) {
                double sum = 0;
                final int max = index + num;
                for (int i = index; i < max; i++) {
                    sum += a[i];
                }
                return sum;
            }

            @Override
            public double evaluate(final double[] a) {
                return this.evaluate(a, 0, a.length);
            }
        };

        final double sum = a.compute(add);
        Assert.assertEquals(0.5 * max * (max + 1), sum, 0);
    }

    private void verifyEquality(final ResizableDoubleArray a, final ResizableDoubleArray b) {
        Assert.assertTrue(b.equals(a));
        Assert.assertTrue(a.equals(b));
        Assert.assertEquals(a.hashCode(), b.hashCode());
    }

    private void verifyInequality(final ResizableDoubleArray a, final ResizableDoubleArray b) {
        Assert.assertFalse(b.equals(a));
        Assert.assertFalse(a.equals(b));
        Assert.assertFalse(a.hashCode() == b.hashCode());
    }

}
