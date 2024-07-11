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
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.stat.clustering;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.util.MathLib;

public class EuclideanDoublePointTest {

    @Test
    public void testArrayIsReference() {
        final double[] array = { -3.0, -2.0, -1.0, 0.0, 1.0 };
        Assert.assertArrayEquals(array, new EuclideanDoublePoint(array).getPoint(), 1.0e-15);
    }

    @Test
    public void testDistance() {
        final EuclideanDoublePoint e1 = new EuclideanDoublePoint(new double[] { -3.0, -2.0, -1.0, 0.0, 1.0 });
        final EuclideanDoublePoint e2 = new EuclideanDoublePoint(new double[] { 1.0, 0.0, -1.0, 1.0, 1.0 });
        Assert.assertEquals(MathLib.sqrt(21.0), e1.distanceFrom(e2), 1.0e-15);
        Assert.assertEquals(0.0, e1.distanceFrom(e1), 1.0e-15);
        Assert.assertEquals(0.0, e2.distanceFrom(e2), 1.0e-15);
    }

    @Test
    public void testCentroid() {
        final List<EuclideanDoublePoint> list = new ArrayList<>();
        list.add(new EuclideanDoublePoint(new double[] { 1.0, 3.0 }));
        list.add(new EuclideanDoublePoint(new double[] { 2.0, 2.0 }));
        list.add(new EuclideanDoublePoint(new double[] { 3.0, 3.0 }));
        list.add(new EuclideanDoublePoint(new double[] { 2.0, 4.0 }));
        final EuclideanDoublePoint c = list.get(0).centroidOf(list);
        Assert.assertEquals(2.0, c.getPoint()[0], 1.0e-15);
        Assert.assertEquals(3.0, c.getPoint()[1], 1.0e-15);
    }

    @Test
    public void testSerial() {
        final EuclideanDoublePoint p = new EuclideanDoublePoint(new double[] { -3.0, -2.0, -1.0, 0.0, 1.0 });
        Assert.assertEquals(p, TestUtils.serializeAndRecover(p));
    }
}
