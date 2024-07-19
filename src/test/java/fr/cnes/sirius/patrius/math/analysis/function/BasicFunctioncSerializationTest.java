/**
 * Copyright 2011-2022 CNES
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
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.function;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.analysis.BivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;

/**
 * Unit test class to evaluate the basic univariate/bivariate functions serialization /
 * deserialization process.
 *
 * @author bonitt
 */
public class BasicFunctioncSerializationTest {

    /**
     * @description Evaluate the basic univariate functions serialization / deserialization process.
     *
     * @testPassCriteria The basic functions can be serialized and deserialized.
     */
    @Test
    public void testUnivariateFunctionSerialization() {

        final double a = 1.1; // Default values
        final double b = -0.5;
        UnivariateFunction fct;

        // Abs
        fct = new Abs();
        Assert.assertEquals(fct.value(b), TestUtils.serializeAndRecover(fct).value(b), 0.);

        // Acos
        fct = new Acos();
        Assert.assertEquals(fct.value(b), TestUtils.serializeAndRecover(fct).value(b), 0.);

        // Acosh
        fct = new Acosh();
        Assert.assertEquals(fct.value(a), TestUtils.serializeAndRecover(fct).value(a), 0.);

        // Asin
        fct = new Asin();
        Assert.assertEquals(fct.value(b), TestUtils.serializeAndRecover(fct).value(b), 0.);

        // Asinh
        fct = new Asinh();
        Assert.assertEquals(fct.value(a), TestUtils.serializeAndRecover(fct).value(a), 0.);

        // Atan
        fct = new Atan();
        Assert.assertEquals(fct.value(a), TestUtils.serializeAndRecover(fct).value(a), 0.);

        // Atanh
        fct = new Atanh();
        Assert.assertEquals(fct.value(b), TestUtils.serializeAndRecover(fct).value(b), 0.);

        // Cbrt
        fct = new Cbrt();
        Assert.assertEquals(fct.value(a), TestUtils.serializeAndRecover(fct).value(a), 0.);

        // Ceil
        fct = new Ceil();
        Assert.assertEquals(fct.value(a), TestUtils.serializeAndRecover(fct).value(a), 0.);

        // Constant
        fct = new Constant(0.5);
        Assert.assertEquals(fct.value(a), TestUtils.serializeAndRecover(fct).value(a), 0.);

        // Cos
        fct = new Cos();
        Assert.assertEquals(fct.value(a), TestUtils.serializeAndRecover(fct).value(a), 0.);

        // Cosh
        fct = new Cosh();
        Assert.assertEquals(fct.value(a), TestUtils.serializeAndRecover(fct).value(a), 0.);

        // CosineFunction
        fct = new CosineFunction(0.5, new Exp());
        Assert.assertEquals(fct.value(a), TestUtils.serializeAndRecover(fct).value(a), 0.);

        // Exp
        fct = new Exp();
        Assert.assertEquals(fct.value(a), TestUtils.serializeAndRecover(fct).value(a), 0.);

        // Expm1
        fct = new Expm1();
        Assert.assertEquals(fct.value(a), TestUtils.serializeAndRecover(fct).value(a), 0.);

        // Floor
        fct = new Floor();
        Assert.assertEquals(fct.value(a), TestUtils.serializeAndRecover(fct).value(a), 0.);

        // Gaussian
        fct = new Gaussian();
        Assert.assertEquals(fct.value(a), TestUtils.serializeAndRecover(fct).value(a), 0.);

        // HarmonicOscillator
        fct = new HarmonicOscillator(0.2, 3.4, 4.1);
        Assert.assertEquals(fct.value(a), TestUtils.serializeAndRecover(fct).value(a), 0.);

        // Identity
        fct = new Identity();
        Assert.assertEquals(fct.value(a), TestUtils.serializeAndRecover(fct).value(a), 0.);

        // Inverse
        fct = new Inverse();
        Assert.assertEquals(fct.value(a), TestUtils.serializeAndRecover(fct).value(a), 0.);

        // Log
        fct = new Log();
        Assert.assertEquals(fct.value(a), TestUtils.serializeAndRecover(fct).value(a), 0.);

        // Log10
        fct = new Log10();
        Assert.assertEquals(fct.value(a), TestUtils.serializeAndRecover(fct).value(a), 0.);

        // Log1p
        fct = new Log1p();
        Assert.assertEquals(fct.value(a), TestUtils.serializeAndRecover(fct).value(a), 0.);

        // Logistic
        fct = new Logistic(3., 0., 1., 1., 2., 1.);
        Assert.assertEquals(fct.value(a), TestUtils.serializeAndRecover(fct).value(a), 0.);

        // Logit
        fct = new Logit();
        Assert.assertEquals(fct.value(0.3), TestUtils.serializeAndRecover(fct).value(0.3), 0.);

        // Minus
        fct = new Minus();
        Assert.assertEquals(fct.value(a), TestUtils.serializeAndRecover(fct).value(a), 0.);

        // Power
        fct = new Power(2.);
        Assert.assertEquals(fct.value(a), TestUtils.serializeAndRecover(fct).value(a), 0.);

        // Rint
        fct = new Rint();
        Assert.assertEquals(fct.value(a), TestUtils.serializeAndRecover(fct).value(a), 0.);

        // Sigmoid
        fct = new Sigmoid();
        Assert.assertEquals(fct.value(a), TestUtils.serializeAndRecover(fct).value(a), 0.);

        // Signum
        fct = new Signum();
        Assert.assertEquals(fct.value(a), TestUtils.serializeAndRecover(fct).value(a), 0.);

        // Sin
        fct = new Sin();
        Assert.assertEquals(fct.value(a), TestUtils.serializeAndRecover(fct).value(a), 0.);

        // Sinc
        fct = new Sinc();
        Assert.assertEquals(fct.value(a), TestUtils.serializeAndRecover(fct).value(a), 0.);

        // SineFunction
        fct = new SineFunction(0.5, new Exp());
        Assert.assertEquals(fct.value(a), TestUtils.serializeAndRecover(fct).value(a), 0.);

        // Sinh
        fct = new Sinh();
        Assert.assertEquals(fct.value(a), TestUtils.serializeAndRecover(fct).value(a), 0.);

        // Sqrt
        fct = new Sqrt();
        Assert.assertEquals(fct.value(a), TestUtils.serializeAndRecover(fct).value(a), 0.);

        // StepFunction
        fct = new StepFunction(new double[] { 0, 1, 2, 3 }, new double[] { 1, 2, 3, 4 });
        Assert.assertEquals(fct.value(a), TestUtils.serializeAndRecover(fct).value(a), 0.);

        // Tan
        fct = new Tan();
        Assert.assertEquals(fct.value(a), TestUtils.serializeAndRecover(fct).value(a), 0.);

        // Tanh
        fct = new Tanh();
        Assert.assertEquals(fct.value(a), TestUtils.serializeAndRecover(fct).value(a), 0.);

        // Ulp
        fct = new Ulp();
        Assert.assertEquals(fct.value(a), TestUtils.serializeAndRecover(fct).value(a), 0.);
    }

    /**
     * @description Evaluate the basic bivariate functions serialization / deserialization process.
     *
     * @testPassCriteria The basic bivariate functions can be serialized and deserialized.
     */
    @Test
    public void testBivariateFunctionSerialization() {

        final double a = 1.1; // Default values
        final double b = 2.3;
        BivariateFunction fct;

        // Add
        fct = new Add();
        Assert.assertEquals(fct.value(a, b), TestUtils.serializeAndRecover(fct).value(a, b), 0.);

        // Atan2
        fct = new Atan2();
        Assert.assertEquals(fct.value(a, b), TestUtils.serializeAndRecover(fct).value(a, b), 0.);

        // Divide
        fct = new Divide();
        Assert.assertEquals(fct.value(a, b), TestUtils.serializeAndRecover(fct).value(a, b), 0.);

        // Max
        fct = new Max();
        Assert.assertEquals(fct.value(a, b), TestUtils.serializeAndRecover(fct).value(a, b), 0.);

        // Min
        fct = new Min();
        Assert.assertEquals(fct.value(a, b), TestUtils.serializeAndRecover(fct).value(a, b), 0.);

        // Multiply
        fct = new Multiply();
        Assert.assertEquals(fct.value(a, b), TestUtils.serializeAndRecover(fct).value(a, b), 0.);

        // Pow
        fct = new Pow();
        Assert.assertEquals(fct.value(a, b), TestUtils.serializeAndRecover(fct).value(a, b), 0.);

        // Subtract
        fct = new Subtract();
        Assert.assertEquals(fct.value(a, b), TestUtils.serializeAndRecover(fct).value(a, b), 0.);
    }
}
