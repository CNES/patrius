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
package fr.cnes.sirius.patrius.math.stat.descriptive;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.stat.descriptive.moment.Mean;

/**
 * Tests for AbstractUnivariateStatistic
 * 
 * @version $Id: AbstractUnivariateStatisticTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class AbstractUnivariateStatisticTest {

    protected double[] testArray = { 0, 1, 2, 3, 4, 5 };
    protected double[] testWeightsArray = { 0.3, 0.2, 1.3, 1.1, 1.0, 1.8 };
    protected double[] testNegativeWeightsArray = { -0.3, 0.2, -1.3, 1.1, 1.0, 1.8 };
    protected double[] nullArray = null;
    protected double[] singletonArray = { 0 };
    protected Mean testStatistic = new Mean();

    @Test
    public void testTestPositive() {
        for (int j = 0; j < 6; j++) {
            for (int i = 1; i < (7 - j); i++) {
                Assert.assertTrue(this.testStatistic.test(this.testArray, 0, i));
            }
        }
        Assert.assertTrue(this.testStatistic.test(this.singletonArray, 0, 1));
        Assert.assertTrue(this.testStatistic.test(this.singletonArray, 0, 0, true));
    }

    @Test
    public void testTestNegative() {
        Assert.assertFalse(this.testStatistic.test(this.singletonArray, 0, 0));
        Assert.assertFalse(this.testStatistic.test(this.testArray, 0, 0));
        try {
            this.testStatistic.test(this.singletonArray, 2, 1); // start past end
            Assert.fail("Expecting MathIllegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // expected
        }
        try {
            this.testStatistic.test(this.testArray, 0, 7); // end past end
            Assert.fail("Expecting MathIllegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // expected
        }
        try {
            this.testStatistic.test(this.testArray, -1, 1); // start negative
            Assert.fail("Expecting MathIllegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // expected
        }
        try {
            this.testStatistic.test(this.testArray, 0, -1); // length negative
            Assert.fail("Expecting MathIllegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // expected
        }
        try {
            this.testStatistic.test(this.nullArray, 0, 1); // null array
            Assert.fail("Expecting MathIllegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // expected
        }
        try {
            this.testStatistic.test(this.testArray, this.nullArray, 0, 1); // null weights array
            Assert.fail("Expecting MathIllegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // expected
        }
        try {
            this.testStatistic.test(this.singletonArray, this.testWeightsArray, 0, 1); // weights.length != value.length
            Assert.fail("Expecting MathIllegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // expected
        }
        try {
            this.testStatistic.test(this.testArray, this.testNegativeWeightsArray, 0, 6); // can't have negative weights
            Assert.fail("Expecting MathIllegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // expected
        }
    }
}
