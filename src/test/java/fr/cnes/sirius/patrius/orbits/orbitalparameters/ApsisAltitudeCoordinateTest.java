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
 * Copyright 2010-2011 Centre National d'Études Spatiales
 */
package fr.cnes.sirius.patrius.orbits.orbitalparameters;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;

/**
 * Unit tests for {@linkplain ApsisAltitudeCoordinate}
 *
 * @author Pierre Seimandi (GMV)
* HISTORY
* VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
* END-HISTORY
 */
public class ApsisAltitudeCoordinateTest {

    /**
     * Tests the different enumeration values.
     */
    @Test
    public void testEnum() {
        ApsisAltitudeCoordinate coordinate;

        // Periapsis altitude
        coordinate = ApsisAltitudeCoordinate.PERIAPSIS_ALTITUDE;
        Assert.assertEquals(0, coordinate.getStateVectorIndex());
        Assert.assertEquals(OrbitType.APSIS, coordinate.getOrbitType());

        // Apoapsis altitude
        coordinate = ApsisAltitudeCoordinate.APOAPSIS_ALTITUDE;
        Assert.assertEquals(1, coordinate.getStateVectorIndex());
        Assert.assertEquals(OrbitType.APSIS, coordinate.getOrbitType());

        // Inclination
        coordinate = ApsisAltitudeCoordinate.INCLINATION;
        Assert.assertEquals(2, coordinate.getStateVectorIndex());
        Assert.assertEquals(OrbitType.APSIS, coordinate.getOrbitType());

        // Periapsis argument
        coordinate = ApsisAltitudeCoordinate.PERIAPSIS_ARGUMENT;
        Assert.assertEquals(3, coordinate.getStateVectorIndex());
        Assert.assertEquals(OrbitType.APSIS, coordinate.getOrbitType());

        // Right ascension of the ascending node
        coordinate = ApsisAltitudeCoordinate.RIGHT_ASCENSION_OF_ASCENDING_NODE;
        Assert.assertEquals(4, coordinate.getStateVectorIndex());
        Assert.assertEquals(OrbitType.APSIS, coordinate.getOrbitType());

        // True anomaly
        coordinate = ApsisAltitudeCoordinate.TRUE_ANOMALY;
        Assert.assertEquals(5, coordinate.getStateVectorIndex());
        Assert.assertEquals(OrbitType.APSIS, coordinate.getOrbitType());

        // Mean anomaly
        coordinate = ApsisAltitudeCoordinate.MEAN_ANOMALY;
        Assert.assertEquals(5, coordinate.getStateVectorIndex());
        Assert.assertEquals(OrbitType.APSIS, coordinate.getOrbitType());

        // Eccentric anomaly
        coordinate = ApsisAltitudeCoordinate.ECCENTRIC_ANOMALY;
        Assert.assertEquals(5, coordinate.getStateVectorIndex());
        Assert.assertEquals(OrbitType.APSIS, coordinate.getOrbitType());
    }

    /**
     * Tests the method that returns the value corresponding to a given state vector index.
     * <p>
     * Tested method:<br>
     * {@linkplain ApsisAltitudeCoordinate#valueOf(int, PositionAngle)}
     * </p>
     */
    @Test
    public void testValueOf() {
        ApsisAltitudeCoordinate coordinate;

        // Test every position angle for the first 5 state vector indices,
        // since it should not have any impact on the result
        for (final PositionAngle positionAngle : PositionAngle.values()) {
            // Periapsis altitude
            coordinate = ApsisAltitudeCoordinate.valueOf(0, positionAngle);
            Assert.assertEquals(ApsisAltitudeCoordinate.PERIAPSIS_ALTITUDE, coordinate);

            // Apoapsis altitude
            coordinate = ApsisAltitudeCoordinate.valueOf(1, positionAngle);
            Assert.assertEquals(ApsisAltitudeCoordinate.APOAPSIS_ALTITUDE, coordinate);

            // Inclination
            coordinate = ApsisAltitudeCoordinate.valueOf(2, positionAngle);
            Assert.assertEquals(ApsisAltitudeCoordinate.INCLINATION, coordinate);

            // Periapsis argument
            coordinate = ApsisAltitudeCoordinate.valueOf(3, positionAngle);
            Assert.assertEquals(ApsisAltitudeCoordinate.PERIAPSIS_ARGUMENT, coordinate);

            // Right ascension of the ascending node
            coordinate = ApsisAltitudeCoordinate.valueOf(4, positionAngle);
            Assert.assertEquals(ApsisAltitudeCoordinate.RIGHT_ASCENSION_OF_ASCENDING_NODE,
                    coordinate);
        }

        // True anomaly
        coordinate = ApsisAltitudeCoordinate.valueOf(5, PositionAngle.TRUE);
        Assert.assertEquals(ApsisAltitudeCoordinate.TRUE_ANOMALY, coordinate);

        // Mean anomaly
        coordinate = ApsisAltitudeCoordinate.valueOf(5, PositionAngle.MEAN);
        Assert.assertEquals(ApsisAltitudeCoordinate.MEAN_ANOMALY, coordinate);

        // Eccentric anomaly
        coordinate = ApsisAltitudeCoordinate.valueOf(5, PositionAngle.ECCENTRIC);
        Assert.assertEquals(ApsisAltitudeCoordinate.ECCENTRIC_ANOMALY, coordinate);

        // Invalid state vector indices
        for (final PositionAngle positionAngle : PositionAngle.values()) {
            try {
                ApsisAltitudeCoordinate.valueOf(-1, positionAngle);
                Assert.fail();
            } catch (final IllegalArgumentException e) {
                Assert.assertTrue(true);
            }

            try {
                ApsisAltitudeCoordinate.valueOf(6, positionAngle);
                Assert.fail();
            } catch (final IllegalArgumentException e) {
                Assert.assertTrue(true);
            }
        }

        // Null position angle
        try {
            ApsisAltitudeCoordinate.valueOf(5, null);
            Assert.fail();
        } catch (final NullPointerException e) {
            final String expectedMessage = null;
            Assert.assertEquals(NullPointerException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }
}
