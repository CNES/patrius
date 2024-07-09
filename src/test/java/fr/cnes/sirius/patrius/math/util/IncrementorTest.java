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

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;
import fr.cnes.sirius.patrius.math.exception.TooManyEvaluationsException;

/**
 * Test for {@link Incrementor}.
 */
public class IncrementorTest {
    @Test
    public void testConstructor1() {
        final Incrementor i = new Incrementor();
        Assert.assertEquals(0, i.getMaximalCount());
        Assert.assertEquals(0, i.getCount());
    }

    @Test
    public void testConstructor2() {
        final Incrementor i = new Incrementor(10);
        Assert.assertEquals(10, i.getMaximalCount());
        Assert.assertEquals(0, i.getCount());
    }

    @Test
    public void testCanIncrement1() {
        final Incrementor i = new Incrementor(3);
        Assert.assertTrue(i.canIncrement());
        i.incrementCount();
        Assert.assertTrue(i.canIncrement());
        i.incrementCount();
        Assert.assertTrue(i.canIncrement());
        i.incrementCount();
        Assert.assertFalse(i.canIncrement());
    }

    @Test
    public void testCanIncrement2() {
        final Incrementor i = new Incrementor(3);
        while (i.canIncrement()) {
            i.incrementCount();
        }

        // Must keep try/catch because the exception must be generated here,
        // and not in the previous loop.
        try {
            i.incrementCount();
            Assert.fail("MaxCountExceededException expected");
        } catch (final MaxCountExceededException e) {
            // Expected.
        }
    }

    @Test
    public void testAccessor() {
        final Incrementor i = new Incrementor();

        i.setMaximalCount(10);
        Assert.assertEquals(10, i.getMaximalCount());
        Assert.assertEquals(0, i.getCount());
    }

    @Test
    public void testBelowMaxCount() {
        final Incrementor i = new Incrementor();

        i.setMaximalCount(3);
        i.incrementCount();
        i.incrementCount();
        i.incrementCount();

        Assert.assertEquals(3, i.getCount());
    }

    @Test(expected = MaxCountExceededException.class)
    public void testAboveMaxCount() {
        final Incrementor i = new Incrementor();

        i.setMaximalCount(3);
        i.incrementCount();
        i.incrementCount();
        i.incrementCount();
        i.incrementCount();
    }

    @Test(expected = TooManyEvaluationsException.class)
    public void testAlternateException() {
        final Incrementor.MaxCountExceededCallback cb = new Incrementor.MaxCountExceededCallback(){
            /** {@inheritDoc} */
            @Override
            public void trigger(final int max) {
                throw new TooManyEvaluationsException(max);
            }
        };

        final Incrementor i = new Incrementor(0, cb);
        i.incrementCount();
    }

    @Test
    public void testReset() {
        final Incrementor i = new Incrementor();

        i.setMaximalCount(3);
        i.incrementCount();
        i.incrementCount();
        i.incrementCount();
        Assert.assertEquals(3, i.getCount());
        i.resetCount();
        Assert.assertEquals(0, i.getCount());
    }

    @Test
    public void testBulkIncrement() {
        final Incrementor i = new Incrementor();

        i.setMaximalCount(3);
        i.incrementCount(2);
        Assert.assertEquals(2, i.getCount());
        i.incrementCount(1);
        Assert.assertEquals(3, i.getCount());
    }
}