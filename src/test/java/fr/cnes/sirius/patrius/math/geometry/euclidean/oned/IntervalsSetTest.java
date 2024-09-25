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
 * VERSION::FA:306:07/11/2014: coverage
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.oned;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.geometry.partitioning.Region;
import fr.cnes.sirius.patrius.math.geometry.partitioning.RegionFactory;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;

public class IntervalsSetTest {

    @Test
    public void testInterval() {
        final IntervalsSet set = new IntervalsSet(2.3, 5.7);
        Assert.assertEquals(3.4, set.getSize(), 1.0e-10);
        Assert.assertEquals(4.0, ((Vector1D) set.getBarycenter()).getX(), 1.0e-10);
        Assert.assertEquals(Region.Location.BOUNDARY, set.checkPoint(new Vector1D(2.3)));
        Assert.assertEquals(Region.Location.BOUNDARY, set.checkPoint(new Vector1D(5.7)));
        Assert.assertEquals(Region.Location.OUTSIDE, set.checkPoint(new Vector1D(1.2)));
        Assert.assertEquals(Region.Location.OUTSIDE, set.checkPoint(new Vector1D(8.7)));
        Assert.assertEquals(Region.Location.INSIDE, set.checkPoint(new Vector1D(3.0)));
        Assert.assertEquals(2.3, set.getInf(), 1.0e-10);
        Assert.assertEquals(5.7, set.getSup(), 1.0e-10);
    }

    @Test
    public void testInfinite() {
        IntervalsSet set = new IntervalsSet(9.0, Double.POSITIVE_INFINITY);
        Assert.assertEquals(Region.Location.BOUNDARY, set.checkPoint(new Vector1D(9.0)));
        Assert.assertEquals(Region.Location.OUTSIDE, set.checkPoint(new Vector1D(8.4)));
        for (double e = 1.0; e <= 6.0; e += 1.0) {
            Assert.assertEquals(Region.Location.INSIDE,
                set.checkPoint(new Vector1D(MathLib.pow(10.0, e))));
        }
        Assert.assertTrue(Double.isInfinite(set.getSize()));
        Assert.assertEquals(9.0, set.getInf(), 1.0e-10);
        Assert.assertTrue(Double.isInfinite(set.getSup()));

        set = (IntervalsSet) new RegionFactory<Euclidean1D>().getComplement(set);
        Assert.assertEquals(9.0, set.getSup(), 1.0e-10);
        Assert.assertTrue(Double.isInfinite(set.getInf()));

    }

    /**
     * For coverage purposes of tests if in method buildTree
     */
    @Test
    public void testPlusAndMinusInfinity() {
        final IntervalsSet set = new IntervalsSet(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        Assert.assertTrue(Double.isInfinite(set.getSize()));
        Assert.assertTrue(Double.isInfinite(set.getSup()));
        Assert.assertTrue(Double.isInfinite(set.getInf()));
    }

    @Test
    public void testMultiple() {
        final RegionFactory<Euclidean1D> factory = new RegionFactory<>();
        final IntervalsSet set = (IntervalsSet)
            factory.intersection(factory.union(factory.difference(new IntervalsSet(1.0, 6.0),
                new IntervalsSet(3.0, 5.0)),
                new IntervalsSet(9.0, Double.POSITIVE_INFINITY)),
                new IntervalsSet(Double.NEGATIVE_INFINITY, 11.0));
        Assert.assertEquals(5.0, set.getSize(), 1.0e-10);
        Assert.assertEquals(5.9, ((Vector1D) set.getBarycenter()).getX(), 1.0e-10);
        Assert.assertEquals(Region.Location.OUTSIDE, set.checkPoint(new Vector1D(0.0)));
        Assert.assertEquals(Region.Location.OUTSIDE, set.checkPoint(new Vector1D(4.0)));
        Assert.assertEquals(Region.Location.OUTSIDE, set.checkPoint(new Vector1D(8.0)));
        Assert.assertEquals(Region.Location.OUTSIDE, set.checkPoint(new Vector1D(12.0)));
        Assert.assertEquals(Region.Location.INSIDE, set.checkPoint(new Vector1D(1.2)));
        Assert.assertEquals(Region.Location.INSIDE, set.checkPoint(new Vector1D(5.9)));
        Assert.assertEquals(Region.Location.INSIDE, set.checkPoint(new Vector1D(9.01)));
        Assert.assertEquals(Region.Location.BOUNDARY, set.checkPoint(new Vector1D(5.0)));
        Assert.assertEquals(Region.Location.BOUNDARY, set.checkPoint(new Vector1D(11.0)));
        Assert.assertEquals(1.0, set.getInf(), 1.0e-10);
        Assert.assertEquals(11.0, set.getSup(), 1.0e-10);

        final List<Interval> list = set.asList();
        Assert.assertEquals(3, list.size());
        Assert.assertEquals(1.0, list.get(0).getInf(), 1.0e-10);
        Assert.assertEquals(3.0, list.get(0).getSup(), 1.0e-10);
        Assert.assertEquals(5.0, list.get(1).getInf(), 1.0e-10);
        Assert.assertEquals(6.0, list.get(1).getSup(), 1.0e-10);
        Assert.assertEquals(9.0, list.get(2).getInf(), 1.0e-10);
        Assert.assertEquals(11.0, list.get(2).getSup(), 1.0e-10);

    }

    @Test
    public void testSinglePoint() {
        final IntervalsSet set = new IntervalsSet(1.0, 1.0);
        Assert.assertEquals(0.0, set.getSize(), Precision.SAFE_MIN);
        Assert.assertEquals(1.0, ((Vector1D) set.getBarycenter()).getX(), Precision.EPSILON);
    }

}
