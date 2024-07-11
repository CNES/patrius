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
package fr.cnes.sirius.patrius.math.analysis.function;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.differentiation.DerivativeStructure;
import fr.cnes.sirius.patrius.math.analysis.differentiation.UnivariateDifferentiableFunction;
import fr.cnes.sirius.patrius.math.util.MathLib;

public class SqrtTest {
    @Test
    public void testComparison() {
        final Sqrt s = new Sqrt();
        final UnivariateFunction f = new UnivariateFunction(){
            /** Serializable UID. */
            private static final long serialVersionUID = 371307977209594992L;

            @Override
            public double value(final double x) {
                return Math.sqrt(x);
            }
        };

        for (double x = 1e-30; x < 1e10; x *= 2) {
            final double fX = f.value(x);
            final double sX = s.value(x);
            Assert.assertEquals("x=" + x, fX, sX, 0);
        }
    }

    @Test
    public void testDerivativeComparison() {
        final UnivariateDifferentiableFunction sPrime = new Sqrt();
        final UnivariateFunction f = new UnivariateFunction(){
            /** Serializable UID. */
            private static final long serialVersionUID = -7591146995441034888L;

            @Override
            public double value(final double x) {
                return 1 / (2 * Math.sqrt(x));
            }
        };

        for (double x = 1e-30; x < 1e10; x *= 2) {
            final double fX = f.value(x);
            final double sX = sPrime.value(new DerivativeStructure(1, 1, 0, x)).getPartialDerivative(1);
            Assert.assertEquals("x=" + x, fX, sX, MathLib.ulp(fX));
        }
    }

    @Test
    public void testDerivativesHighOrder() {
        final DerivativeStructure s = new Sqrt().value(new DerivativeStructure(1, 5, 0, 1.2));
        Assert.assertEquals(1.0954451150103322269, s.getPartialDerivative(0), 1.0e-16);
        Assert.assertEquals(0.45643546458763842789, s.getPartialDerivative(1), 1.0e-16);
        Assert.assertEquals(-0.1901814435781826783, s.getPartialDerivative(2), 1.0e-16);
        Assert.assertEquals(0.23772680447272834785, s.getPartialDerivative(3), 1.0e-16);
        Assert.assertEquals(-0.49526417598485072465, s.getPartialDerivative(4), 1.0e-16);
        Assert.assertEquals(1.4445205132891479465, s.getPartialDerivative(5), 5.0e-16);
    }

}
