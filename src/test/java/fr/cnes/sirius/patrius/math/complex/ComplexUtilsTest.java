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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.math.complex;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.util.FastMath;

/**
 * @version $Id: ComplexUtilsTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class ComplexUtilsTest {

    private final double inf = Double.POSITIVE_INFINITY;
    private final double negInf = Double.NEGATIVE_INFINITY;
    private final double nan = Double.NaN;
    private final double pi = FastMath.PI;

    private final Complex negInfInf = new Complex(this.negInf, this.inf);
    private final Complex infNegInf = new Complex(this.inf, this.negInf);
    private final Complex infInf = new Complex(this.inf, this.inf);
    private final Complex negInfNegInf = new Complex(this.negInf, this.negInf);
    private final Complex infNaN = new Complex(this.inf, this.nan);

    @Test
    public void testPolar2Complex() {
        TestUtils.assertEquals(Complex.ONE,
            ComplexUtils.polar2Complex(1, 0), 10e-12);
        TestUtils.assertEquals(Complex.ZERO,
            ComplexUtils.polar2Complex(0, 1), 10e-12);
        TestUtils.assertEquals(Complex.ZERO,
            ComplexUtils.polar2Complex(0, -1), 10e-12);
        TestUtils.assertEquals(Complex.I,
            ComplexUtils.polar2Complex(1, this.pi / 2), 10e-12);
        TestUtils.assertEquals(Complex.I.negate(),
            ComplexUtils.polar2Complex(1, -this.pi / 2), 10e-12);
        double r = 0;
        for (int i = 0; i < 5; i++) {
            r += i;
            double theta = 0;
            for (int j = 0; j < 20; j++) {
                theta += this.pi / 6;
                TestUtils.assertEquals(this.altPolar(r, theta),
                    ComplexUtils.polar2Complex(r, theta), 10e-12);
            }
            theta = -2 * this.pi;
            for (int j = 0; j < 20; j++) {
                theta -= this.pi / 6;
                TestUtils.assertEquals(this.altPolar(r, theta),
                    ComplexUtils.polar2Complex(r, theta), 10e-12);
            }
        }
    }

    protected Complex altPolar(final double r, final double theta) {
        return Complex.I.multiply(new Complex(theta, 0)).exp().multiply(new Complex(r, 0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPolar2ComplexIllegalModulus() {
        ComplexUtils.polar2Complex(-1, 0);
    }

    @Test
    public void testPolar2ComplexNaN() {
        TestUtils.assertSame(Complex.NaN, ComplexUtils.polar2Complex(this.nan, 1));
    }

    @Test
    public void testPolar2ComplexInf() {
        TestUtils.assertSame(this.infInf, ComplexUtils.polar2Complex(this.inf, this.pi / 4));
        TestUtils.assertSame(this.infNaN, ComplexUtils.polar2Complex(this.inf, 0));
        TestUtils.assertSame(this.infNegInf, ComplexUtils.polar2Complex(this.inf, -this.pi / 4));
        TestUtils.assertSame(this.negInfInf, ComplexUtils.polar2Complex(this.inf, 3 * this.pi / 4));
        TestUtils.assertSame(this.negInfNegInf, ComplexUtils.polar2Complex(this.inf, 5 * this.pi / 4));
    }

    @Test
    public void testConvertToComplex() {
        final double[] real = new double[] { this.negInf, -123.45, 0, 1, 234.56, this.pi, this.inf };
        final Complex[] complex = ComplexUtils.convertToComplex(real);

        for (int i = 0; i < real.length; i++) {
            Assert.assertEquals(real[i], complex[i].getReal(), 0d);
        }
    }
}
