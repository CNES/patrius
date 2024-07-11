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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
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
 * Unit tests for {@linkplain AlternateEquinoctialCoordinate}
 *
 * @author Pierre Seimandi (GMV)
 */
public class AlternateEquinoctialCoordinateTest {

    /**
     * Tests the different enumeration values.
     */
    @Test
    public void testEnum() {
        AlternateEquinoctialCoordinate coordinate;

        // Semi-major axis
        coordinate = AlternateEquinoctialCoordinate.MEAN_MOTION;
        Assert.assertEquals(0, coordinate.getStateVectorIndex());
        Assert.assertEquals(OrbitType.ALTERNATE_EQUINOCTIAL, coordinate.getOrbitType());

        // Ex
        coordinate = AlternateEquinoctialCoordinate.E_X;
        Assert.assertEquals(1, coordinate.getStateVectorIndex());
        Assert.assertEquals(OrbitType.ALTERNATE_EQUINOCTIAL, coordinate.getOrbitType());

        // Ey
        coordinate = AlternateEquinoctialCoordinate.E_Y;
        Assert.assertEquals(2, coordinate.getStateVectorIndex());
        Assert.assertEquals(OrbitType.ALTERNATE_EQUINOCTIAL, coordinate.getOrbitType());

        // Hx
        coordinate = AlternateEquinoctialCoordinate.H_X;
        Assert.assertEquals(3, coordinate.getStateVectorIndex());
        Assert.assertEquals(OrbitType.ALTERNATE_EQUINOCTIAL, coordinate.getOrbitType());

        // Hy
        coordinate = AlternateEquinoctialCoordinate.H_Y;
        Assert.assertEquals(4, coordinate.getStateVectorIndex());
        Assert.assertEquals(OrbitType.ALTERNATE_EQUINOCTIAL, coordinate.getOrbitType());

        // True longitude argument
        coordinate = AlternateEquinoctialCoordinate.TRUE_LONGITUDE_ARGUMENT;
        Assert.assertEquals(5, coordinate.getStateVectorIndex());
        Assert.assertEquals(OrbitType.ALTERNATE_EQUINOCTIAL, coordinate.getOrbitType());

        // Mean longitude argument
        coordinate = AlternateEquinoctialCoordinate.MEAN_LONGITUDE_ARGUMENT;
        Assert.assertEquals(5, coordinate.getStateVectorIndex());
        Assert.assertEquals(OrbitType.ALTERNATE_EQUINOCTIAL, coordinate.getOrbitType());

        // Eccentric longitude argument
        coordinate = AlternateEquinoctialCoordinate.ECCENTRIC_LONGITUDE_ARGUMENT;
        Assert.assertEquals(5, coordinate.getStateVectorIndex());
        Assert.assertEquals(OrbitType.ALTERNATE_EQUINOCTIAL, coordinate.getOrbitType());
    }

    /**
     * Tests the method that returns the value corresponding to a given state vector index.
     * <p>
     * Tested method:<br>
     * {@linkplain AlternateEquinoctialCoordinate#valueOf(int, PositionAngle)}
     * </p>
     */
    @Test
    public void testValueOf() {
        AlternateEquinoctialCoordinate coordinate;

        // Test every position angle for the first 5 state vector indices,
        // since it should not have any impact on the result
        for (final PositionAngle positionAngle : PositionAngle.values()) {
            // Semi-major axis
            coordinate = AlternateEquinoctialCoordinate.valueOf(0, positionAngle);
            Assert.assertEquals(AlternateEquinoctialCoordinate.MEAN_MOTION, coordinate);

            // Ex
            coordinate = AlternateEquinoctialCoordinate.valueOf(1, positionAngle);
            Assert.assertEquals(AlternateEquinoctialCoordinate.E_X, coordinate);

            // Ey
            coordinate = AlternateEquinoctialCoordinate.valueOf(2, positionAngle);
            Assert.assertEquals(AlternateEquinoctialCoordinate.E_Y, coordinate);

            // Hx
            coordinate = AlternateEquinoctialCoordinate.valueOf(3, positionAngle);
            Assert.assertEquals(AlternateEquinoctialCoordinate.H_X, coordinate);

            // Hy
            coordinate = AlternateEquinoctialCoordinate.valueOf(4, positionAngle);
            Assert.assertEquals(AlternateEquinoctialCoordinate.H_Y, coordinate);
        }

        // True longitude argument
        coordinate = AlternateEquinoctialCoordinate.valueOf(5, PositionAngle.TRUE);
        Assert.assertEquals(AlternateEquinoctialCoordinate.TRUE_LONGITUDE_ARGUMENT, coordinate);

        // Mean longitude argument
        coordinate = AlternateEquinoctialCoordinate.valueOf(5, PositionAngle.MEAN);
        Assert.assertEquals(AlternateEquinoctialCoordinate.MEAN_LONGITUDE_ARGUMENT, coordinate);

        // Eccentric longitude argument
        coordinate = AlternateEquinoctialCoordinate.valueOf(5, PositionAngle.ECCENTRIC);
        Assert.assertEquals(AlternateEquinoctialCoordinate.ECCENTRIC_LONGITUDE_ARGUMENT, coordinate);

        // Invalid state vector indices
        for (final PositionAngle positionAngle : PositionAngle.values()) {
            try {
                AlternateEquinoctialCoordinate.valueOf(-1, positionAngle);
                Assert.fail();
            } catch (final IllegalArgumentException e) {
                Assert.assertTrue(true);
            }

            try {
                AlternateEquinoctialCoordinate.valueOf(6, positionAngle);
                Assert.fail();
            } catch (final IllegalArgumentException e) {
                Assert.assertTrue(true);
            }
        }

        // Null position angle
        try {
            AlternateEquinoctialCoordinate.valueOf(5, null);
            Assert.fail();
        } catch (final NullPointerException e) {
            final String expectedMessage = null;
            Assert.assertEquals(NullPointerException.class, e.getClass());
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }
}
