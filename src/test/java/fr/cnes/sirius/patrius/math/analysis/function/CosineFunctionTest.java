/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
 * Copyright 2011-2021 CNES
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
 * VERSION:4.8:FA:FA-2945:15/11/2021:[PATRIUS] Utilisation des degres dans des fonctions mathematiques 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.function;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.analysis.differentiation.DerivativeStructure;
import fr.cnes.sirius.patrius.math.analysis.differentiation.UnivariateDifferentiableFunction;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Unit test class for the {@link CosineFunction} class.
 *
 * @author bonitt
 */
public class CosineFunctionTest {

    private UnivariateDifferentiableFunction f;

    private final double EPS = 1e-14;

    @Test
    public void testValue() {
        double k = 1.;
        CosineFunction cosF = new CosineFunction(k, f); // f(x) = k * cos(1 / x)

        Assert.assertEquals(MathLib.cos(2.), cosF.value(0.5), EPS);
        Assert.assertEquals(MathLib.cos(1.), cosF.value(1), EPS);
        Assert.assertEquals(MathLib.cos(0.5), cosF.value(2.), EPS);

        k = -1.5;
        cosF = new CosineFunction(k, f); // f(x) = k * cos(1 / x)

        Assert.assertEquals(k * MathLib.cos(2.), cosF.value(0.5), EPS);
        Assert.assertEquals(k * MathLib.cos(1.), cosF.value(1), EPS);
        Assert.assertEquals(k * MathLib.cos(0.5), cosF.value(2.), EPS);
    }

    @Test
    public void testDerivative() {
        final double k = 1.;
        final CosineFunction cosF = new CosineFunction(k, f); // f(x) = k * cos(1 / x)

        DerivativeStructure t = new DerivativeStructure(1, 1, 0.5, 0.5);
        Assert.assertEquals(k * MathLib.cos(2.), cosF.value(t).getValue(), EPS);
        Assert.assertEquals(-k * -2 * MathLib.sin(2), cosF.value(t).getPartialDerivative(1), EPS);

        t = new DerivativeStructure(1, 1, 0.5, 6.);
        Assert.assertEquals(k * MathLib.cos(2.), cosF.value(t).getValue(), EPS);
        Assert.assertEquals(-k * -24 * MathLib.sin(2), cosF.value(t).getPartialDerivative(1), EPS);
    }

    @Test
    public void testToString() {
        Assert.assertTrue(new CosineFunction(1., f).toString().equals("1.0 * cos(1 / x)"));
        Assert.assertTrue(new CosineFunction(-1.5, f).toString().equals("-1.5 * cos(1 / x)"));
    }

    @Before
    public void setUp() {
        // f(x) = 1 / x
        this.f = new UnivariateDifferentiableFunction() {

            private static final long serialVersionUID = -3992897263993445606L;
            private final UnivariateDifferentiableFunction internalF = new Inverse();

            @Override
            public double value(final double x) {
                return internalF.value(x);
            }

            @Override
            public DerivativeStructure value(final DerivativeStructure t) {
                return internalF.value(t);
            }

            @Override
            public String toString() {
                return "1 / x";
            }
        };
    }
}
