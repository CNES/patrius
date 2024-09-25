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

/**
 * Unit tests for {@linkplain CartesianCoordinate}
 *
 * @author Pierre Seimandi (GMV)
 */
public class CartesianCoordinateTest {

    /**
     * Tests the different enumeration values.
     */
    @Test
    public void testEnum() {
        CartesianCoordinate coordinate;

        // X
        coordinate = CartesianCoordinate.X;
        Assert.assertEquals(0, coordinate.getStateVectorIndex());
        Assert.assertEquals(OrbitType.CARTESIAN, coordinate.getOrbitType());

        // Y
        coordinate = CartesianCoordinate.Y;
        Assert.assertEquals(1, coordinate.getStateVectorIndex());
        Assert.assertEquals(OrbitType.CARTESIAN, coordinate.getOrbitType());

        // Z
        coordinate = CartesianCoordinate.Z;
        Assert.assertEquals(2, coordinate.getStateVectorIndex());
        Assert.assertEquals(OrbitType.CARTESIAN, coordinate.getOrbitType());

        // Vx
        coordinate = CartesianCoordinate.VX;
        Assert.assertEquals(3, coordinate.getStateVectorIndex());
        Assert.assertEquals(OrbitType.CARTESIAN, coordinate.getOrbitType());

        // Vy
        coordinate = CartesianCoordinate.VY;
        Assert.assertEquals(4, coordinate.getStateVectorIndex());
        Assert.assertEquals(OrbitType.CARTESIAN, coordinate.getOrbitType());

        // Vz
        coordinate = CartesianCoordinate.VZ;
        Assert.assertEquals(5, coordinate.getStateVectorIndex());
        Assert.assertEquals(OrbitType.CARTESIAN, coordinate.getOrbitType());
    }

    /**
     * Tests the method that returns the value corresponding to a given state vector index.
     * <p>
     * Tested method:<br>
     * {@linkplain CartesianCoordinate#valueOf(int)}
     * </p>
     */
    @Test
    public void testValueOf() {
        CartesianCoordinate coordinate;

        // Semi-major axis
        coordinate = CartesianCoordinate.valueOf(0);
        Assert.assertEquals(CartesianCoordinate.X, coordinate);

        // Eccentricity
        coordinate = CartesianCoordinate.valueOf(1);
        Assert.assertEquals(CartesianCoordinate.Y, coordinate);

        // Inclination
        coordinate = CartesianCoordinate.valueOf(2);
        Assert.assertEquals(CartesianCoordinate.Z, coordinate);

        // Perigee argument
        coordinate = CartesianCoordinate.valueOf(3);
        Assert.assertEquals(CartesianCoordinate.VX, coordinate);

        // Right ascension of the ascending node
        coordinate = CartesianCoordinate.valueOf(4);
        Assert.assertEquals(CartesianCoordinate.VY, coordinate);

        // Right ascension of the ascending node
        coordinate = CartesianCoordinate.valueOf(5);
        Assert.assertEquals(CartesianCoordinate.VZ, coordinate);

        // Invalid state vector indices
        try {
            CartesianCoordinate.valueOf(-1);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        try {
            CartesianCoordinate.valueOf(6);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
    }
}
