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
 * Copyright 2010-2011 Centre National d'Études Spatiales
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.orbits.orbitalparameters;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;

/**
 * Unit tests for {@linkplain CircularCoordinate}
 *
 * @author Pierre Seimandi (GMV)
 */
public class CircularCoordinateTest {

    /**
     * Tests the different enumeration values.
     */
    @Test
    public void testEnum() {
        CircularCoordinate coordinate;

        // Semi-major axis
        coordinate = CircularCoordinate.SEMI_MAJOR_AXIS;
        Assert.assertEquals(0, coordinate.getStateVectorIndex());
        Assert.assertEquals(OrbitType.CIRCULAR, coordinate.getOrbitType());

        // Ex
        coordinate = CircularCoordinate.E_X;
        Assert.assertEquals(1, coordinate.getStateVectorIndex());
        Assert.assertEquals(OrbitType.CIRCULAR, coordinate.getOrbitType());

        // Ey
        coordinate = CircularCoordinate.E_Y;
        Assert.assertEquals(2, coordinate.getStateVectorIndex());
        Assert.assertEquals(OrbitType.CIRCULAR, coordinate.getOrbitType());

        // Inclination
        coordinate = CircularCoordinate.INCLINATION;
        Assert.assertEquals(3, coordinate.getStateVectorIndex());
        Assert.assertEquals(OrbitType.CIRCULAR, coordinate.getOrbitType());

        // Right ascension of the ascending node
        coordinate = CircularCoordinate.RIGHT_ASCENSION_OF_ASCENDING_NODE;
        Assert.assertEquals(4, coordinate.getStateVectorIndex());
        Assert.assertEquals(OrbitType.CIRCULAR, coordinate.getOrbitType());

        // True latitude argument
        coordinate = CircularCoordinate.TRUE_LATITUDE_ARGUMENT;
        Assert.assertEquals(5, coordinate.getStateVectorIndex());
        Assert.assertEquals(OrbitType.CIRCULAR, coordinate.getOrbitType());

        // Mean latitude argument
        coordinate = CircularCoordinate.MEAN_LATITUDE_ARGUMENT;
        Assert.assertEquals(5, coordinate.getStateVectorIndex());
        Assert.assertEquals(OrbitType.CIRCULAR, coordinate.getOrbitType());

        // Eccentric latitude argument
        coordinate = CircularCoordinate.ECCENTRIC_LATITUDE_ARGUMENT;
        Assert.assertEquals(5, coordinate.getStateVectorIndex());
        Assert.assertEquals(OrbitType.CIRCULAR, coordinate.getOrbitType());
    }

    /**
     * Tests the method that returns the value corresponding to a given state vector index.
     * <p>
     * Tested method:<br>
     * {@linkplain CircularCoordinate#valueOf(int, PositionAngle)}
     * </p>
     */
    @Test
    public void testValueOf() {
        CircularCoordinate coordinate;

        // Test every position angle for the first 5 state vector indices,
        // since it should not have any impact on the result
        for (final PositionAngle positionAngle : PositionAngle.values()) {
            // Semi-major axis
            coordinate = CircularCoordinate.valueOf(0, positionAngle);
            Assert.assertEquals(CircularCoordinate.SEMI_MAJOR_AXIS, coordinate);

            // Ex
            coordinate = CircularCoordinate.valueOf(1, positionAngle);
            Assert.assertEquals(CircularCoordinate.E_X, coordinate);

            // Ey
            coordinate = CircularCoordinate.valueOf(2, positionAngle);
            Assert.assertEquals(CircularCoordinate.E_Y, coordinate);

            // Inclination
            coordinate = CircularCoordinate.valueOf(3, positionAngle);
            Assert.assertEquals(CircularCoordinate.INCLINATION, coordinate);

            // Right ascension of the ascending node
            coordinate = CircularCoordinate.valueOf(4, positionAngle);
            Assert.assertEquals(CircularCoordinate.RIGHT_ASCENSION_OF_ASCENDING_NODE, coordinate);
        }

        // True latitude argument
        coordinate = CircularCoordinate.valueOf(5, PositionAngle.TRUE);
        Assert.assertEquals(CircularCoordinate.TRUE_LATITUDE_ARGUMENT, coordinate);

        // Mean latitude argument
        coordinate = CircularCoordinate.valueOf(5, PositionAngle.MEAN);
        Assert.assertEquals(CircularCoordinate.MEAN_LATITUDE_ARGUMENT, coordinate);

        // Eccentric latitude argument
        coordinate = CircularCoordinate.valueOf(5, PositionAngle.ECCENTRIC);
        Assert.assertEquals(CircularCoordinate.ECCENTRIC_LATITUDE_ARGUMENT, coordinate);

        // Invalid state vector indices
        for (final PositionAngle positionAngle : PositionAngle.values()) {
            try {
                CircularCoordinate.valueOf(-1, positionAngle);
                Assert.fail();
            } catch (final IllegalArgumentException e) {
                Assert.assertTrue(true);
            }

            try {
                CircularCoordinate.valueOf(6, positionAngle);
                Assert.fail();
            } catch (final IllegalArgumentException e) {
                Assert.assertTrue(true);
            }
        }

        // Null position angle
        try {
            CircularCoordinate.valueOf(5, null);
            Assert.fail();
        } catch (final NullPointerException e) {
            final String expectedMessage = null;
            Assert.assertEquals(NullPointerException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }
}
