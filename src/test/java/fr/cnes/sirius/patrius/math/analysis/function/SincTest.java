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
import fr.cnes.sirius.patrius.math.dfp.Dfp;
import fr.cnes.sirius.patrius.math.dfp.DfpField;
import fr.cnes.sirius.patrius.math.dfp.DfpMath;
import fr.cnes.sirius.patrius.math.util.MathLib;

public class SincTest {

    @Test
    public void testShortcut() {
        final Sinc s = new Sinc();
        final UnivariateFunction f = new UnivariateFunction(){
            /** Serializable UID. */
            private static final long serialVersionUID = -2046199920618172756L;

            @Override
            public double value(final double x) {
                final Dfp dfpX = new DfpField(25).newDfp(x);
                return DfpMath.sin(dfpX).divide(dfpX).toDouble();
            }
        };

        for (double x = 1e-30; x < 1e10; x *= 2) {
            final double fX = f.value(x);
            final double sX = s.value(x);
            Assert.assertEquals("x=" + x, fX, sX, 2.0e-16);
        }
    }

    @Test
    public void testCrossings() {
        final Sinc s = new Sinc(true);
        final int numCrossings = 1000;
        final double tol = 2e-16;
        for (int i = 1; i <= numCrossings; i++) {
            Assert.assertEquals("i=" + i, 0, s.value(i), tol);
        }
    }

    @Test
    public void testZero() {
        final Sinc s = new Sinc();
        Assert.assertEquals(1d, s.value(0), 0);
    }

    @Test
    public void testEuler() {
        final Sinc s = new Sinc();
        final double x = 123456.789;
        double prod = 1;
        double xOverPow2 = x / 2;
        while (xOverPow2 > 0) {
            prod *= MathLib.cos(xOverPow2);
            xOverPow2 /= 2;
        }
        Assert.assertEquals(prod, s.value(x), 1e-13);
    }

    @Test
    public void testDerivativeZero() {
        final DerivativeStructure s0 = new Sinc(true).value(new DerivativeStructure(1, 1, 0, 0.0));
        Assert.assertEquals(0, s0.getPartialDerivative(1), 0);
    }

    @Test
    public void testDerivatives1Dot2Unnormalized() {
        final DerivativeStructure s = new Sinc(false).value(new DerivativeStructure(1, 5, 0, 1.2));
        Assert.assertEquals(0.77669923830602195806, s.getPartialDerivative(0), 1.0e-16);
        Assert.assertEquals(-0.34528456985779031701, s.getPartialDerivative(1), 1.0e-16);
        Assert.assertEquals(-0.2012249552097047631, s.getPartialDerivative(2), 1.0e-16);
        Assert.assertEquals(0.2010975926270339262, s.getPartialDerivative(3), 4.0e-16);
        Assert.assertEquals(0.106373929549242204, s.getPartialDerivative(4), 1.0e-15);
        Assert.assertEquals(-0.1412599110579478695, s.getPartialDerivative(5), 3.0e-15);
    }

    @Test
    public void testDerivatives1Dot2Normalized() {
        final DerivativeStructure s = new Sinc(true).value(new DerivativeStructure(1, 5, 0, 1.2));
        Assert.assertEquals(-0.15591488063143983888, s.getPartialDerivative(0), 6.0e-17);
        Assert.assertEquals(-0.54425176145292298767, s.getPartialDerivative(1), 2.0e-16);
        Assert.assertEquals(2.4459044611635856107, s.getPartialDerivative(2), 9.0e-16);
        Assert.assertEquals(0.5391369206235909586, s.getPartialDerivative(3), 7.0e-16);
        Assert.assertEquals(-16.984649869728849865, s.getPartialDerivative(4), 8.0e-15);
        Assert.assertEquals(5.0980327462666316586, s.getPartialDerivative(5), 9.0e-15);
    }

    @Test
    public void testDerivativeShortcut() {
        final Sinc sinc = new Sinc();
        final UnivariateFunction f = new UnivariateFunction(){
            /** Serializable UID. */
            private static final long serialVersionUID = 74981243243053788L;

            @Override
            public double value(final double x) {
                final Dfp dfpX = new DfpField(25).newDfp(x);
                return DfpMath.cos(dfpX).subtract(DfpMath.sin(dfpX).divide(dfpX)).divide(dfpX).toDouble();
            }
        };

        for (double x = 1e-30; x < 1e10; x *= 2) {
            final double fX = f.value(x);
            final DerivativeStructure sX = sinc.value(new DerivativeStructure(1, 1, 0, x));
            Assert.assertEquals("x=" + x, fX, sX.getPartialDerivative(1), 3.0e-13);
        }
    }
}
