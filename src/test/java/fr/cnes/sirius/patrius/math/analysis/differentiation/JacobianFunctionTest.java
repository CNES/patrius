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
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.differentiation;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Test for class {@link JacobianFunction}.
 */
public class JacobianFunctionTest {

    @Test
    public void testSphere() {
        final SphereMapping f = new SphereMapping(10.0);
        final JacobianFunction j = new JacobianFunction(f);
        for (double latitude = -1.5; latitude < 1.5; latitude += 0.1) {
            for (double longitude = -3.1; longitude < 3.1; longitude += 0.1) {
                final double[] point = new double[] { latitude, longitude };
                final double[][] referenceJacobian = f.jacobian(point);
                final double[][] testJacobian = j.value(point);
                Assert.assertEquals(referenceJacobian.length, testJacobian.length);
                for (int i = 0; i < 3; ++i) {
                    TestUtils.assertEquals(referenceJacobian[i], testJacobian[i], 2.0e-15);
                }
            }
        }
    }

    /* Maps (latitude, longitude) to (x, y, z) */
    private static class SphereMapping implements MultivariateDifferentiableVectorFunction {

        private final double radius;

        public SphereMapping(final double radius) {
            this.radius = radius;
        }

        @Override
        public double[] value(final double[] point) {
            final double cLat = MathLib.cos(point[0]);
            final double sLat = MathLib.sin(point[0]);
            final double cLon = MathLib.cos(point[1]);
            final double sLon = MathLib.sin(point[1]);
            return new double[] {
                this.radius * cLon * cLat,
                this.radius * sLon * cLat,
                this.radius * sLat
            };
        }

        @Override
        public DerivativeStructure[] value(final DerivativeStructure[] point) {
            final DerivativeStructure cLat = point[0].cos();
            final DerivativeStructure sLat = point[0].sin();
            final DerivativeStructure cLon = point[1].cos();
            final DerivativeStructure sLon = point[1].sin();
            return new DerivativeStructure[] {
                cLon.multiply(cLat).multiply(this.radius),
                sLon.multiply(cLat).multiply(this.radius),
                sLat.multiply(this.radius)
            };
        }

        public double[][] jacobian(final double[] point) {
            final double cLat = MathLib.cos(point[0]);
            final double sLat = MathLib.sin(point[0]);
            final double cLon = MathLib.cos(point[1]);
            final double sLon = MathLib.sin(point[1]);
            return new double[][] {
                { -this.radius * cLon * sLat, -this.radius * sLon * cLat },
                { -this.radius * sLon * sLat, this.radius * cLon * cLat },
                { this.radius * cLat, 0 }
            };
        }

    }

}
