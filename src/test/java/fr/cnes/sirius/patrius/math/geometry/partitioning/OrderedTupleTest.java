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
 * VERSION::FA:306:12/11/2014: (creation) coverage
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.math.geometry.partitioning;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.math.geometry.partitioning.utilities.OrderedTuple;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * Tests the class OrderedTuple
 * 
 * @since 2.4
 * @version $Id: OrderedTupleTest.java 18108 2017-10-04 06:45:27Z bignon $
 * 
 */
public class OrderedTupleTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle OrderedTuple
         * @featureDescription OrderedTuple
         */
        ORDERED_TUPLE_TEST
    }

    /**
     * For coverage purpose, tests the constructor of class OrderedTuple when some of the components
     * are plus and minus infinity. To ensure the correct behavior, the method compareTo is called.
     * Therefore, in the same time, this test also covers the if (components.length == ot.components.length)
     * of method compareTo.
     */
    @Test
    public void coverageTest() {
        final double plusInfinity = Double.POSITIVE_INFINITY;
        final double minusInfinity = Double.NEGATIVE_INFINITY;
        final double nan = Double.NaN;

        final OrderedTuple vecInfinity = new OrderedTuple(plusInfinity, minusInfinity);
        // in that case posInf = false; negInf = false; and nan = true;

        final double eps = Precision.DOUBLE_COMPARISON_EPSILON;

        // the comparison of vecInfinity with ref should lead to +1 bcs nan = true
        final OrderedTuple ref = new OrderedTuple(1, 2);
        double result = vecInfinity.compareTo(ref);
        Assert.assertEquals(result, 1, eps);

        // the comparison of ref with vecInfinity should lead to -1 bcs nan = true for the parameter this time
        result = ref.compareTo(vecInfinity);
        Assert.assertEquals(result, -1, eps);

        new OrderedTuple(nan, 1);

        // the comparison of vecNan with ref should lead to +1 bcs nan = true
        result = vecInfinity.compareTo(ref);
        Assert.assertEquals(result, 1, eps);

        // the comparison of ref with vecNan should lead to -1 bcs nan = true for the parameter this time
        result = ref.compareTo(vecInfinity);
        Assert.assertEquals(result, -1, eps);

        final OrderedTuple ref2 = new OrderedTuple(plusInfinity, -1);
        result = ref.compareTo(ref2);
        Assert.assertEquals(result, -1, eps);
        result = ref2.compareTo(ref);
        Assert.assertEquals(result, 1, eps);

        final OrderedTuple ref3 = new OrderedTuple(1, minusInfinity);
        result = ref.compareTo(ref3);
        Assert.assertEquals(result, 1, eps);
        result = ref3.compareTo(ref);
        Assert.assertEquals(result, -1, eps);
    }

    /**
     * For coverage purpose, tests the method compareTo for the test
     * else if (encoding.length > ot.encoding.length)
     */
    @Test
    public void coverageTestEncoding() {
        OrderedTuple vec1 = new OrderedTuple(0.0, 1.0, 2.0);
        OrderedTuple vec2 = new OrderedTuple(3.0, 0);

        final double eps = Precision.DOUBLE_COMPARISON_EPSILON;

        double result = vec1.compareTo(vec2);
        Assert.assertEquals(result, 1, eps);
        result = vec2.compareTo(vec1);
        Assert.assertEquals(result, -1, eps);

        vec1 = new OrderedTuple(36, 22);
        vec2 = new OrderedTuple(36.00000000010001, 22.000000000099995);

        result = vec1.compareTo(vec2);
        Assert.assertEquals(result, -1, eps);
        result = vec2.compareTo(vec1);
        Assert.assertEquals(result, 1, eps);

    }
}
