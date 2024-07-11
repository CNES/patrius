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
 * Unit tests for {@linkplain EquatorialCoordinate}
 *
 * @author Pierre Seimandi (GMV)
 */
public class EquatorialCoordinateTest {

    /**
     * Tests the different enumeration values.
     */
    @Test
    public void testEnum() {
        EquatorialCoordinate coordinate;

        // Semi-major axis
        coordinate = EquatorialCoordinate.SEMI_MAJOR_AXIS;
        Assert.assertEquals(0, coordinate.getStateVectorIndex());
        Assert.assertEquals(OrbitType.EQUATORIAL, coordinate.getOrbitType());

        // Eccentricity
        coordinate = EquatorialCoordinate.ECCENTRICITY;
        Assert.assertEquals(1, coordinate.getStateVectorIndex());
        Assert.assertEquals(OrbitType.EQUATORIAL, coordinate.getOrbitType());

        // Periapsis longitude
        coordinate = EquatorialCoordinate.PERIAPSIS_LONGITUDE;
        Assert.assertEquals(2, coordinate.getStateVectorIndex());
        Assert.assertEquals(OrbitType.EQUATORIAL, coordinate.getOrbitType());

        // Ix
        coordinate = EquatorialCoordinate.I_X;
        Assert.assertEquals(3, coordinate.getStateVectorIndex());
        Assert.assertEquals(OrbitType.EQUATORIAL, coordinate.getOrbitType());

        // Iy
        coordinate = EquatorialCoordinate.I_Y;
        Assert.assertEquals(4, coordinate.getStateVectorIndex());
        Assert.assertEquals(OrbitType.EQUATORIAL, coordinate.getOrbitType());

        // True anomaly
        coordinate = EquatorialCoordinate.TRUE_ANOMALY;
        Assert.assertEquals(5, coordinate.getStateVectorIndex());
        Assert.assertEquals(OrbitType.EQUATORIAL, coordinate.getOrbitType());

        // Mean anomaly
        coordinate = EquatorialCoordinate.MEAN_ANOMALY;
        Assert.assertEquals(5, coordinate.getStateVectorIndex());
        Assert.assertEquals(OrbitType.EQUATORIAL, coordinate.getOrbitType());

        // Eccentric anomaly
        coordinate = EquatorialCoordinate.ECCENTRIC_ANOMALY;
        Assert.assertEquals(5, coordinate.getStateVectorIndex());
        Assert.assertEquals(OrbitType.EQUATORIAL, coordinate.getOrbitType());
    }

    /**
     * Tests the method that returns the value corresponding to a given state vector index.
     * <p>
     * Tested method:<br>
     * {@linkplain EquatorialCoordinate#valueOf(int, PositionAngle)}
     * </p>
     */
    @Test
    public void testValueOf() {
        EquatorialCoordinate coordinate;

        // Test every position angle for the first 5 state vector indices,
        // since it should not have any impact on the result
        for (final PositionAngle positionAngle : PositionAngle.values()) {
            // Semi-major axis
            coordinate = EquatorialCoordinate.valueOf(0, positionAngle);
            Assert.assertEquals(EquatorialCoordinate.SEMI_MAJOR_AXIS, coordinate);

            // Ex
            coordinate = EquatorialCoordinate.valueOf(1, positionAngle);
            Assert.assertEquals(EquatorialCoordinate.ECCENTRICITY, coordinate);

            // Ey
            coordinate = EquatorialCoordinate.valueOf(2, positionAngle);
            Assert.assertEquals(EquatorialCoordinate.PERIAPSIS_LONGITUDE, coordinate);

            // Hx
            coordinate = EquatorialCoordinate.valueOf(3, positionAngle);
            Assert.assertEquals(EquatorialCoordinate.I_X, coordinate);

            // Hy
            coordinate = EquatorialCoordinate.valueOf(4, positionAngle);
            Assert.assertEquals(EquatorialCoordinate.I_Y, coordinate);
        }

        // True anomaly
        coordinate = EquatorialCoordinate.valueOf(5, PositionAngle.TRUE);
        Assert.assertEquals(EquatorialCoordinate.TRUE_ANOMALY, coordinate);

        // Mean anomaly
        coordinate = EquatorialCoordinate.valueOf(5, PositionAngle.MEAN);
        Assert.assertEquals(EquatorialCoordinate.MEAN_ANOMALY, coordinate);

        // Eccentric anomaly
        coordinate = EquatorialCoordinate.valueOf(5, PositionAngle.ECCENTRIC);
        Assert.assertEquals(EquatorialCoordinate.ECCENTRIC_ANOMALY, coordinate);

        // Invalid state vector indices
        for (final PositionAngle positionAngle : PositionAngle.values()) {
            try {
                EquatorialCoordinate.valueOf(-1, positionAngle);
                Assert.fail();
            } catch (final IllegalArgumentException e) {
                Assert.assertTrue(true);
            }

            try {
                EquatorialCoordinate.valueOf(6, positionAngle);
                Assert.fail();
            } catch (final IllegalArgumentException e) {
                Assert.assertTrue(true);
            }
        }

        // Null position angle
        try {
            EquatorialCoordinate.valueOf(5, null);
            Assert.fail();
        } catch (final NullPointerException e) {
            final String expectedMessage = null;
            Assert.assertEquals(NullPointerException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }
}
