/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
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
 *
 * HISTORY
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.math.linear;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests for {@link RealVector}.
 */
public class RealVectorTest extends RealVectorAbstractTest {

    @Override
    public RealVector create(final double[] data) {
        return new RealVectorTestImpl(data);
    }

    @Test
    @Ignore("Abstract class RealVector does not implement append(RealVector).")
    @Override
    public void testAppendVector() {
        // Do nothing
    }

    @Test
    @Ignore("Abstract class RealVector does not implement append(double)")
    @Override
    public void testAppendScalar() {
        // Do nothing
    }

    @Test
    @Ignore("Abstract class RealVector does not implement getSubvector(int, int)")
    @Override
    public void testGetSubVector() {
        // Do nothing
    }

    @Test
    @Ignore("Abstract class RealVector does not implement getSubvector(int, int)")
    @Override
    public void testGetSubVectorInvalidIndex1() {
        // Do nothing
    }

    @Test
    @Ignore("Abstract class RealVector does not implement getSubvector(int, int)")
    @Override
    public void testGetSubVectorInvalidIndex2() {
        // Do nothing
    }

    @Test
    @Ignore("Abstract class RealVector does not implement getSubvector(int, int)")
    @Override
    public void testGetSubVectorInvalidIndex3() {
        // Do nothing
    }

    @Test
    @Ignore("Abstract class RealVector does not implement getSubvector(int, int)")
    @Override
    public void testGetSubVectorInvalidIndex4() {
        // Do nothing
    }

    @Test
    @Ignore("Abstract class RealVector does not implement setSubvector(int, RealVector)")
    @Override
    public void testSetSubVectorSameType() {
        // Do nothing
    }

    @Test
    @Ignore("Abstract class RealVector does not implement setSubvector(int, RealVector)")
    @Override
    public void testSetSubVectorMixedType() {
        // Do nothing
    }

    @Test
    @Ignore("Abstract class RealVector does not implement setSubvector(int, RealVector)")
    @Override
    public void testSetSubVectorInvalidIndex1() {
        // Do nothing
    }

    @Test
    @Ignore("Abstract class RealVector does not implement setSubvector(int, RealVector)")
    @Override
    public void testSetSubVectorInvalidIndex2() {
        // Do nothing
    }

    @Test
    @Ignore("Abstract class RealVector does not implement setSubvector(int, RealVector)")
    @Override
    public void testSetSubVectorInvalidIndex3() {
        // Do nothing
    }

    @Test
    @Ignore("Abstract class RealVector does not implement isNaN()")
    @Override
    public void testIsNaN() {
        // Do nothing
    }

    @Test
    @Ignore("Abstract class RealVector does not implement isNaN()")
    @Override
    public void testIsInfinite() {
        // Do nothing
    }

    @Test
    @Ignore("Abstract class RealVector does not implement ebeMultiply(RealVector)")
    @Override
    public void testEbeMultiplySameType() {
        // Do nothing
    }

    @Test
    @Ignore("Abstract class RealVector does not implement ebeMultiply(RealVector)")
    @Override
    public void testEbeMultiplyMixedTypes() {
        // Do nothing
    }

    @Test
    @Ignore("Abstract class RealVector does not implement ebeMultiply(RealVector)")
    @Override
    public void testEbeMultiplyDimensionMismatch() {
        // Do nothing
    }

    @Test
    @Ignore("Abstract class RealVector does not implement ebeDivide(RealVector)")
    @Override
    public void testEbeDivideSameType() {
        // Do nothing
    }

    @Test
    @Ignore("Abstract class RealVector does not implement ebeDivide(RealVector)")
    @Override
    public void testEbeDivideMixedTypes() {
        // Do nothing
    }

    @Test
    @Ignore("Abstract class RealVector does not implement ebeDivide(RealVector)")
    @Override
    public void testEbeDivideDimensionMismatch() {
        // Do nothing
    }

    @Test
    @Ignore("Abstract class RealVector does not implement getL1Norm()")
    @Override
    public void testGetL1Norm() {
        // Do nothing
    }

    @Test
    @Ignore("Abstract class RealVector does not implement getLInfNorm()")
    @Override
    public void testGetLInfNorm() {
        // Do nothing
    }

    @Test
    @Ignore("Abstract class RealVector is not serializable.")
    @Override
    public void testSerial() {
        // Do nothing
    }

    @Test
    @Ignore("Abstract class RealVector does not override equals(Object).")
    @Override
    public void testEquals() {
        // Do nothing
    }
}
