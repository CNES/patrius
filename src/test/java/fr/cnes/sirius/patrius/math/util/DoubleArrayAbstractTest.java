/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
 * Copyright 2011-2022 CNES
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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.util;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.stat.StatUtils;

/**
 * This class contains test cases for the ExpandableDoubleArray.
 * 
 * @version $Id: DoubleArrayAbstractTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public abstract class DoubleArrayAbstractTest {

    protected DoubleArray da = null;

    // Array used to test rolling
    protected DoubleArray ra = null;

    @Test
    public void testAdd1000() {

        for (int i = 0; i < 1000; i++) {
            this.da.addElement(i);
        }

        Assert.assertEquals(
            "Number of elements should be equal to 1000 after adding 1000 values",
            1000,
            this.da.getNumElements());

        Assert.assertEquals(
            "The element at the 56th index should be 56",
            56.0,
            this.da.getElement(56),
            Double.MIN_VALUE);

    }

    @Test
    public void testGetValues() {
        final double[] controlArray = { 2.0, 4.0, 6.0 };

        this.da.addElement(2.0);
        this.da.addElement(4.0);
        this.da.addElement(6.0);
        final double[] testArray = this.da.getElements();

        for (int i = 0; i < this.da.getNumElements(); i++) {
            Assert.assertEquals(
                "The testArray values should equal the controlArray values, index i: "
                    + i
                    + " does not match",
                testArray[i],
                controlArray[i],
                Double.MIN_VALUE);
        }

    }

    @Test
    public void testAddElementRolling() {
        this.ra.addElement(0.5);
        this.ra.addElement(1.0);
        this.ra.addElement(1.0);
        this.ra.addElement(1.0);
        this.ra.addElement(1.0);
        this.ra.addElement(1.0);
        this.ra.addElementRolling(2.0);

        Assert.assertEquals(
            "There should be 6 elements in the eda",
            6,
            this.ra.getNumElements());
        Assert.assertEquals(
            "The max element should be 2.0",
            2.0,
            StatUtils.max(this.ra.getElements()),
            Double.MIN_VALUE);
        Assert.assertEquals(
            "The min element should be 1.0",
            1.0,
            StatUtils.min(this.ra.getElements()),
            Double.MIN_VALUE);

        for (int i = 0; i < 1024; i++) {
            this.ra.addElementRolling(i);
        }

        Assert.assertEquals(
            "We just inserted 1024 rolling elements, num elements should still be 6",
            6,
            this.ra.getNumElements());
    }

    @Test
    public void testMinMax() {
        this.da.addElement(2.0);
        this.da.addElement(22.0);
        this.da.addElement(-2.0);
        this.da.addElement(21.0);
        this.da.addElement(22.0);
        this.da.addElement(42.0);
        this.da.addElement(62.0);
        this.da.addElement(22.0);
        this.da.addElement(122.0);
        this.da.addElement(1212.0);

        Assert.assertEquals("Min should be -2.0", -2.0, StatUtils.min(this.da.getElements()), Double.MIN_VALUE);
        Assert.assertEquals(
            "Max should be 1212.0",
            1212.0,
            StatUtils.max(this.da.getElements()),
            Double.MIN_VALUE);
    }

}
