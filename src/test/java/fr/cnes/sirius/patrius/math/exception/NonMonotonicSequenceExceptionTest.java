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
package fr.cnes.sirius.patrius.math.exception;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.util.MathArrays;

/**
 * Test for {@link NonMonotonicSequenceException}.
 * 
 * @version $Id: NonMonotonicSequenceExceptionTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class NonMonotonicSequenceExceptionTest {
    @Test
    public void testAccessors() {
        NonMonotonicSequenceException e = new NonMonotonicSequenceException(0, -1, 1,
            MathArrays.OrderDirection.DECREASING, false);
        Assert.assertEquals(0, e.getArgument());
        Assert.assertEquals(-1, e.getPrevious());
        Assert.assertEquals(1, e.getIndex());
        Assert.assertTrue(e.getDirection() == MathArrays.OrderDirection.DECREASING);
        Assert.assertFalse(e.getStrict());

        e = new NonMonotonicSequenceException(-1, 0, 1);
        Assert.assertEquals(-1, e.getArgument());
        Assert.assertEquals(0, e.getPrevious());
        Assert.assertEquals(1, e.getIndex());
        Assert.assertTrue(e.getDirection() == MathArrays.OrderDirection.INCREASING);
        Assert.assertTrue(e.getStrict());
    }
}