/**
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
 *
 * HISTORY
 * VERSION:4.11.1:DM:DM-88:30/06/2023:[PATRIUS] Complement FT 3319
 * VERSION:4.11:DM:DM-3319:22/05/2023:[PATRIUS] Rendre la classe QuaternionPolynomialSegment plus generique et ajouter de la coherence dans le package polynomials
 * VERSION:4.10.2:FA:FA-3290:31/01/2023:[PATRIUS] Erreur dans la methode getChebyshevAbscissas dans DatePolynomialChebyshevFunction
 * VERSION:4.10.1:FA:FA-3263:02/12/2022:[PATRIUS] Implementation incorrecte de la classe DatePolynomialChebyshevFunction
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:DM:DM-3204:03/11/2022:[PATRIUS] Evolutions autour des polynômes de Chebyshev
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.analytical.twod;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.polynomials.ChebyshevDecompositionEngine;
import fr.cnes.sirius.patrius.math.analysis.polynomials.DatePolynomialChebyshevFunction;
import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialChebyshevFunction;
import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunction;
import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialType;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;

/**
 * This class test {@link DatePolynomialChebyshevFunction} class.
 * 
 * @author Alex Nardi
 * 
 */
public class DatePolynomialChebyshevFunctionTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Polynomial functions
         * 
         * @featureDescription Validate polynomial functions
         * 
         * @coveredRequirements
         */
        POLYNOMIAL_FUNCTION
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#POLYNOMIAL_FUNCTION}
     * 
     * @testedMethod {@link DatePolynomialChebyshevFunction#DatePolynomialChebyshevFunction(AbsoluteDate, AbsoluteDate, AbsoluteDate, double[])}
     * @testedMethod {@link DatePolynomialChebyshevFunction#DatePolynomialChebyshevFunction(AbsoluteDate, PolynomialChebyshevFunction)}
     * 
     * @description test function construction
     * 
     * @input originDate, tStart, tEnd and polynomialCoefs (or originDate and polyFunction)
     * 
     * @output DatePolynomialChebyshevFunction
     * 
     * @testPassCriteria the attributes of the DatePolynomialChebyshevFunction built are exactly as expected
     * 
     * @referenceVersion 4.10.1
     * 
     * @nonRegressionVersion 4.10.1
     */
    @Test
    public void testContructors() {
        // Initialization
        final AbsoluteDate originDate = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate tStart = new AbsoluteDate(originDate, 30.);
        final AbsoluteDate tEnd = new AbsoluteDate(tStart, 60.);
        final double[] coefs = { 2., 3., 4., 5. };
        // Function construction with first constructor
        final DatePolynomialChebyshevFunction f1 = new DatePolynomialChebyshevFunction(originDate, tStart, tEnd, coefs);
        // Function construction with second constructor
        final DatePolynomialChebyshevFunction f2 = new DatePolynomialChebyshevFunction(originDate, tStart, tEnd, coefs);
        // Functions attributes verification
        Assert.assertEquals(0., f1.getT0().preciseDurationFrom(originDate), 1E-14);
        Assert.assertEquals(0., f2.getT0().preciseDurationFrom(originDate), 1E-14);
        Assert.assertEquals(0., f1.getStart().preciseDurationFrom(tStart), 1E-14);
        Assert.assertEquals(0., f2.getStart().preciseDurationFrom(tStart), 1E-14);
        Assert.assertEquals(0., f1.getEnd().preciseDurationFrom(tEnd), 1E-14);
        Assert.assertEquals(0., f2.getEnd().preciseDurationFrom(tEnd), 1E-14);
        final double[] f1coefPoly = f1.getCoefficients();
        final double[] f2coefPoly = f2.getCoefficients();
        Assert.assertEquals(4, f1coefPoly.length);
        Assert.assertEquals(4, f2coefPoly.length);
        for (int i = 0; i < f1coefPoly.length; i++) {
            Assert.assertEquals(coefs[i], f1.getCoefficients()[i], 1E-14);
            Assert.assertEquals(coefs[i], f2.getCoefficients()[i], 1E-14);
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#POLYNOMIAL_FUNCTION}
     * 
     * @testedMethod {@link DatePolynomialChebyshevFunction#value(AbsoluteDate)}
     * 
     * @description test value function
     * 
     * @input DatePolynomialChebyshevFunction
     * 
     * @output value
     * 
     * @testPassCriteria values are exactly as expected (mathematical cases)
     * 
     * @referenceVersion 4.10
     * 
     * @nonRegressionVersion 4.10
     */
    @Test
    public void testValue() {
        // Initialization
        final AbsoluteDate date0 = AbsoluteDate.J2000_EPOCH;        
        final double duration = 60.;
        final AbsoluteDate date1 = new AbsoluteDate(date0, duration);

        // Normal case (check before, and after reference date)
        final double[] coefs = { 2., 3., 4., 5. };
        final DatePolynomialChebyshevFunction f = new DatePolynomialChebyshevFunction(date0, date0, date1, coefs);
        final PolynomialChebyshevFunction fRef = new PolynomialChebyshevFunction(0., duration, coefs);

        Assert.assertEquals(fRef.value(-10), f.value(date0.shiftedBy(-10)), 0.);
        Assert.assertEquals(fRef.value(10), f.value(date0.shiftedBy(10)), 0.);

        // Test cache
        final double[] coefs2 = { 2., 3., 4., 5. };
        final DatePolynomialChebyshevFunction f2 = new DatePolynomialChebyshevFunction(date0, date0, date1, coefs2);
        Assert.assertEquals(fRef.value(10), f2.value(date0.shiftedBy(10)), 0.);

        final double[] coefs3 = { 2., 3., 4., 5., 6. };
        final DatePolynomialChebyshevFunction f3 = new DatePolynomialChebyshevFunction(date0, date0, date1, coefs3);
        final PolynomialChebyshevFunction f2Ref = new PolynomialChebyshevFunction(0., duration, coefs3);
        Assert.assertEquals(f2Ref.value(10), f3.value(date0.shiftedBy(10)), 0.);
        Assert.assertEquals(fRef.value(10), f2.value(date0.shiftedBy(10)), 0.);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#POLYNOMIAL_FUNCTION}
     * 
     * @testedMethod {@link DatePolynomialChebyshevFunction#DatePolynomialChebyshevFunction(AbsoluteDate, AbsoluteDate, double[])}
     * @testedMethod {@link DatePolynomialChebyshevFunction#DatePolynomialChebyshevFunction(DatePolynomialChebyshevFunction)}
     * @testedMethod {@link DatePolynomialChebyshevFunction#getCoefficients()}
     * @testedMethod {@link DatePolynomialChebyshevFunction#getOrder()}
     * @testedMethod {@link DatePolynomialChebyshevFunction#getDegree()}
     * @testedMethod {@link DatePolynomialChebyshevFunction#getStart()}
     * @testedMethod {@link DatePolynomialChebyshevFunction#getEnd()}
     * @testedMethod {@link DatePolynomialChebyshevFunction#getPolynomialType()}
     * @testedMethod {@link DatePolynomialChebyshevFunction#dateToDouble(AbsoluteDate)}
     * @testedMethod {@link DatePolynomialChebyshevFunction#doubleToDate(double)}
     * @testedMethod {@link DatePolynomialChebyshevFunction#getTimeFactor()}
     * @testedMethod {@link DatePolynomialChebyshevFunction#getStartAsDouble()}
     * @testedMethod {@link DatePolynomialChebyshevFunction#getEndAsDouble()}
     * 
     * @description test getters and copy
     * 
     * @input DatePolynomialChebyshevFunction
     * 
     * @output getters
     * 
     * @testPassCriteria values are exactly as expected
     * 
     * @referenceVersion 4.10
     * 
     * @nonRegressionVersion 4.10
     */
    @Test
    public void testGetters() {
        // Initialization
        final AbsoluteDate date0 = AbsoluteDate.J2000_EPOCH;
        final double duration = 60.;
        final AbsoluteDate date1 = new AbsoluteDate(date0, duration);
        final double[] coefs = { 2., 3., 4, 5 };
        final DatePolynomialChebyshevFunction f = new DatePolynomialChebyshevFunction(date0, date0, date1, coefs);

        // Test getters
        Assert.assertEquals(3., f.getOrder(), 0.);
        Assert.assertEquals(3., f.getDegree(), 0.);
        Assert.assertEquals(date0, f.getStart());
        Assert.assertEquals(date1, f.getEnd());
        Assert.assertEquals(date0, f.getT0());
        Assert.assertEquals(new AbsoluteDateInterval(date0, date1), f.getRange());
        Assert.assertEquals(2., f.getCoefficients()[0], 0.);
        Assert.assertEquals(3., f.getCoefficients()[1], 0.);
        Assert.assertEquals(4., f.getCoefficients()[2], 0.);
        Assert.assertEquals(5., f.getCoefficients()[3], 0.);
        Assert.assertEquals(PolynomialType.CHEBYSHEV, f.getPolynomialType());
        Assert.assertEquals(date1.durationFrom(date0), f.dateToDouble(date1), 1E-14);
        Assert.assertEquals(date0.shiftedBy(50.), f.doubleToDate(50.));
        Assert.assertNull(f.getTimeFactor());
        
        final PolynomialChebyshevFunction expectedPolyFunction = new PolynomialChebyshevFunction(0., duration, coefs);
        Assert.assertEquals(expectedPolyFunction.getStart(), f.getStartAsDouble(), 1E-14);
        Assert.assertEquals(expectedPolyFunction.getEnd(), f.getEndAsDouble(), 1E-14);

        // Test copy
        final DatePolynomialChebyshevFunction f2 = f.copy();
        Assert.assertEquals(3., f2.getDegree(), 0.);
        Assert.assertEquals(3., f2.getOrder(), 0.);
        Assert.assertEquals(date0, f2.getStart());
        Assert.assertEquals(date1, f2.getEnd());
        Assert.assertEquals(date0, f2.getT0());
        Assert.assertEquals(new AbsoluteDateInterval(date0, date1), f2.getRange());
        Assert.assertEquals(2., f2.getCoefficients()[0], 0.);
        Assert.assertEquals(3., f2.getCoefficients()[1], 0.);
        Assert.assertEquals(4., f2.getCoefficients()[2], 0.);
        Assert.assertEquals(5., f2.getCoefficients()[3], 0.);
        Assert.assertFalse(f.getCoefficients().hashCode() == f2.getCoefficients().hashCode());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#POLYNOMIAL_FUNCTION}
     * 
     * @testedMethod {@link DatePolynomialChebyshevFunction#getChebyshevAbscissas(int)}
     * 
     * @description test getters and copy
     * 
     * @input DatePolynomialChebyshevFunction
     * 
     * @output Chebyshev abscissas
     * 
     * @testPassCriteria values are exactly as expected
     * 
     * @referenceVersion 4.10
     * 
     * @nonRegressionVersion 4.10
     */
    @Test
    public void testGetChebyshevAbscissas() {
        // Initialization
        final AbsoluteDate originDate = AbsoluteDate.J2000_EPOCH;
        final double duration = 60.;
        final AbsoluteDate tStart = new AbsoluteDate(originDate, duration);
        final AbsoluteDate tEnd = new AbsoluteDate(tStart, duration);
        final double[] coefs = { 2., 3., 4, 5 };
        final DatePolynomialChebyshevFunction f = new DatePolynomialChebyshevFunction(originDate, tStart, tEnd,
            coefs);
        final PolynomialChebyshevFunction fRef = new PolynomialChebyshevFunction(tStart.durationFrom(originDate),
            tEnd.durationFrom(originDate), coefs);

        // Test getChebyshevAbscissas(int)
        final int n = 10;
        final AbsoluteDate[] fChebyshevAbscissas = f.getChebyshevAbscissas(n);
        final double[] fRefChebyshevAbscissas = fRef.getChebyshevAbscissas(n);
        for (int i = 0; i < n; i++) {
            Assert.assertEquals(new AbsoluteDate(originDate, fRefChebyshevAbscissas[i]), fChebyshevAbscissas[i]);
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#POLYNOMIAL_FUNCTION}
     * 
     * @testedMethod {@link DatePolynomialChebyshevFunction#DatePolynomialChebyshevFunction(AbsoluteDate, AbsoluteDate, double[])}
     * @testedMethod {@link DatePolynomialChebyshevFunction#DatePolynomialChebyshevFunction(DatePolynomialChebyshevFunction)}
     * 
     * @description test exception at initialization
     * 
     * @input DatePolynomialChebyshevFunction
     * 
     * @output exception
     * 
     * @testPassCriteria exceptions are raised as expected
     * 
     * @referenceVersion 4.10
     * 
     * @nonRegressionVersion 4.10
     */
    @Test
    public void testExceptions() {
        // Not enough coefficients in constructor
        final AbsoluteDate date0 = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate date1 = new AbsoluteDate(date0, 60.);
        final double[] coefs = {};
        try {
            new DatePolynomialChebyshevFunction(date0, date0, date1, coefs);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        // End date before start date in constructor
        final AbsoluteDate date2 = new AbsoluteDate(date0, -60.);
        final double[] coefs2 = { 1, 2 };
        try {
            new DatePolynomialChebyshevFunction(date0, date0, date2, coefs2);
            Assert.fail();
        } catch (final MathIllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        // End date same as start date in constructor
        final AbsoluteDate date3 = date0;
        try {
            new DatePolynomialChebyshevFunction(date0, date0, date3, coefs2);
            Assert.fail();
        } catch (final MathIllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        // Not enough points (-1) in getChebyshevAbscissas(int)
        final DatePolynomialChebyshevFunction f = new DatePolynomialChebyshevFunction(date0, date0, date1, coefs2);
        try {
            f.getChebyshevAbscissas(-1);
            Assert.fail();
        } catch (final NotStrictlyPositiveException e) {
            Assert.assertTrue(true);
        }

        // Not enough points (0) in getChebyshevAbscissas(int)
        try {
            f.getChebyshevAbscissas(0);
            Assert.fail();
        } catch (final NotStrictlyPositiveException e) {
            Assert.assertTrue(true);
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#POLYNOMIAL_FUNCTION}
     * 
     * @testedMethod {@link DatePolynomialChebyshevFunction#derivative()}
     * 
     * @description test polynomial derivative function
     * 
     * @input DatePolynomialChebyshevFunction
     * 
     * @output derivative
     * 
     * @testPassCriteria derivative is exactly as expected
     * 
     * @referenceVersion 4.11
     * 
     * @nonRegressionVersion 4.11
     */
    @Test
    public void testDerivative() {
        // Initialization
        final AbsoluteDate date0 = AbsoluteDate.J2000_EPOCH;
        final double[] coefs = { 8., 7., -2.5, 1.5 };
        final UnivariateFunction function = new PolynomialFunction(coefs);
        final PolynomialChebyshevFunction poly = ChebyshevDecompositionEngine.interpolateChebyshevFunction(function,
            50, -1, 1.5);
        final DatePolynomialChebyshevFunction f = new DatePolynomialChebyshevFunction(date0, poly);
        final DatePolynomialChebyshevFunction der = f.derivative();
        final double[] derCoefs = { 7., -5., 4.5 };
        final UnivariateFunction fDerivativeRef = new PolynomialFunction(derCoefs); // f'(x)
        // Check several places (should be equal to the f'(x) function values)
        for (double i = -1; i <= 1.5; i += 0.1) {
            Assert.assertEquals(fDerivativeRef.value(i), der.value(date0.shiftedBy(i)), 1e-9);
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#POLYNOMIAL_FUNCTION}
     * 
     * @testedMethod {@link DatePolynomialChebyshevFunction#derivative()}
     * 
     * @description test primitive function
     * 
     * @input DatePolynomialChebyshevFunction
     * 
     * @output primitive
     * 
     * @testPassCriteria primitive is exactly as expected
     * 
     * @referenceVersion 4.11
     * 
     * @nonRegressionVersion 4.11
     */
    @Test
    public void testPrimitive() {
        // Initialization
        final AbsoluteDate date0 = AbsoluteDate.J2000_EPOCH;
        final double[] coefs = { 7., -5., 4.5 };
        // The reference polynomial f(x)
        final UnivariateFunction function = new PolynomialFunction(coefs);
        // The Chebyshev polynomial approximating the reference polynomial
        final PolynomialChebyshevFunction poly = ChebyshevDecompositionEngine.interpolateChebyshevFunction(function,
            50, -1, 1.5);
        final DatePolynomialChebyshevFunction f = new DatePolynomialChebyshevFunction(date0, poly);
        // Reference primitive coefficients built with (0,8) point
        final double[] primitiveCoefs = { 8., 7., -2.5, 1.5 };
        // Reference primitive polynomial F(x)
        final PolynomialFunction fPrimitiveRef = new PolynomialFunction(primitiveCoefs);
        // Actual Chebyshev polynomial primitive with the (date0,8) point
        final DatePolynomialChebyshevFunction polyPrim = f.primitive(date0, 8);
        // Check at several places (should be close to the F(x) function values)
        for (double i = -1; i <= 1.5; i += 0.1) {
            Assert.assertEquals(fPrimitiveRef.value(i), polyPrim.value(date0.shiftedBy(i)), 1E-9);
        }
        // Check that the derivative of the primitive is identity (check at several places)
        for (double i = -1; i <= 1.5; i += 0.1) {
            Assert.assertEquals(poly.value(i), polyPrim.derivative().value(date0.shiftedBy(i)), 1E-9);
        }
    }
}
