/**
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
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2090:15/05/2019:[PATRIUS] ajout de fonctionnalites aux bibliotheques mathematiques
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.framework;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.math.analysis.BivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for math.framework package.
 * 
 * @author Emmanuel Bignon
 */
public class MathLibraryTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle Math library
         * 
         * @featureDescription Math library
         * 
         * @coveredRequirements DM-1782
         */
        MATH_LIBRARY
    }

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(MathLibraryTest.class.getSimpleName(), "Math library");
    }

    @AfterClass
    public static void tearDownAfterClass() {
        // Set back result
        MathLib.setMathLibrary(MathLibraryType.FASTMATH);
    }

//    @Test
//    public final void testBenchMark() throws PatriusException {
//
//        // Benchmark
//        final int nb = 100000000;
//        final double t0 = System.currentTimeMillis();
//        for (int i = 0; i < nb; i++) {
//            final double x = i / (double) nb;
//            final double sin = MathLib.sin(x);
//            final double cos = MathLib.cos(x);
//        }
//        System.out.println("sin and cos séparés: " + (System.currentTimeMillis() - t0) + "ms");
//
//        final double t1 = System.currentTimeMillis();
//        for (int i = 0; i < nb; i++) {
//            final double x = i / (double) nb;
//            final double[] res2 = MathLib.sinAndCos(x);
//            final double sin = res2[0];
//            final double cos = res2[1];
//        }
//        System.out.println("sin and cos en une passe avec création tableau: " + (System.currentTimeMillis() - t1)
//                + "ms");
//
//        final double[] res = { 0., 0. };
//        final double t2 = System.currentTimeMillis();
//        for (int i = 0; i < nb; i++) {
//            final double x = i / (double) nb;
//            MathLib.sinAndCos(x, res);
//            final double sin = res[0];
//            final double cos = res[1];
//        }
//        System.out.println("sin and cos en une passe sans création tableau: " + (System.currentTimeMillis() - t2)
//                + "ms");
//    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MATH_LIBRARY}
     * 
     * @testedMethod {@link MathLib#sinAndCos(double)}, and {@link MathLib#sinAndCos(double, double[])}
     * 
     * @description test method computing sin and cos in one single call
     * 
     * @input numbers
     * 
     * @output sin and cos
     * 
     * @testPassCriteria result is as expected (reference: Math, threshold: 0)
     * 
     * @referenceVersion 4.5
     * 
     * @nonRegressionVersion 4.5
     */
    @Test
    public final void testSinCos() throws PatriusException {
        MathLib.setMathLibrary(MathLibraryType.FASTMATH);
        checkAllSinCos();
        MathLib.setMathLibrary(MathLibraryType.FASTEST_MATHLIB);
        checkAllSinCos();
        MathLib.setMathLibrary(MathLibraryType.JAFAMA_FASTMATH);
        checkAllSinCos();
        MathLib.setMathLibrary(MathLibraryType.JAFAMA_STRICT_FASTMATH);
        checkAllSinCos();
        MathLib.setMathLibrary(MathLibraryType.MATH);
        checkAllSinCos();
        MathLib.setMathLibrary(MathLibraryType.STRICTMATH);
        checkAllSinCos();
    }
    
    /**
     * Check all possible sin/cos cases
     */
    private void checkAllSinCos() {
        // Regular cases from -2 Pi to 2 Pi
        for (int i = -1000; i < 1000; i++) {
            final double x = i / 1000. * 2. * MathLib.PI;
            checkSinCos(x);
        }
        // Specific cases
        checkSinCos(13000);
        checkSinCos(13002);
        checkSinCos(13003.5);
        checkSinCos(13005);
        checkSinCos(830000);
        checkSinCos(MathLib.pow(2, 30) + 0.5);
        checkSinCos(MathLib.pow(2, 50) + 0.5);
        
        try {
            MathLib.sinAndCos(Double.POSITIVE_INFINITY);
            Assert.fail();
        } catch (final ArithmeticException e) {
            // Expected
            Assert.assertTrue(true);
        }
    }
    
    /**
     * Check all versions of sin and cos.
     * @param x x
     */
    private void checkSinCos(final double x) {
        // Threshold
        final double eps = 0;

        // Reference
        final double sin = MathLib.sin(x);
        final double cos = MathLib.cos(x);

        // Sin and cos
        final double[] res = MathLib.sinAndCos(x);
        final double sin2 = res[0];
        final double cos2 = res[1];
        MathLib.sinAndCos(x, res);
        final double sin3 = res[0];
        final double cos3 = res[1];

        // Check
        Assert.assertEquals(sin, sin2, eps);
        Assert.assertEquals(cos, cos2, eps);
        Assert.assertEquals(sin, sin3, eps);
        Assert.assertEquals(cos, cos3, eps);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MATH_LIBRARY}
     * 
     * @testedMethod {@link MathLib#sinhAndCosh(double)}, and {@link MathLib#sinhAndCosh(double, double[])}
     * 
     * @description test method computing sinh and cosh in one single call
     * 
     * @input numbers
     * 
     * @output sinh and cosh
     * 
     * @testPassCriteria result is as expected (reference: Math, threshold: 0)
     * 
     * @referenceVersion 4.5
     * 
     * @nonRegressionVersion 4.5
     */
    @Test
    public final void testSinhCosh() throws PatriusException {
        MathLib.setMathLibrary(MathLibraryType.FASTMATH);
        checkAllSinhCosh();
        MathLib.setMathLibrary(MathLibraryType.FASTEST_MATHLIB);
        checkAllSinhCosh();
        MathLib.setMathLibrary(MathLibraryType.JAFAMA_FASTMATH);
        checkAllSinhCosh();
        MathLib.setMathLibrary(MathLibraryType.JAFAMA_STRICT_FASTMATH);
        checkAllSinhCosh();
        MathLib.setMathLibrary(MathLibraryType.MATH);
        checkAllSinhCosh();
        MathLib.setMathLibrary(MathLibraryType.STRICTMATH);
        checkAllSinhCosh();
    }

    /**
     * Check all possible sinh/cosh cases
     */
    private void checkAllSinhCosh() {
        // Regular cases from -100 to 100
        for (int i = -1000; i < 1000; i++) {
            final double x = i / 10. * 2. * MathLib.PI;
            checkSinhCosh(x);
        }
        // Specific cases
        checkSinhCosh(MathLib.log(Double.MAX_VALUE) + 1);
        
        try {
            MathLib.sinhAndCosh(Double.NaN);
            Assert.fail();
        } catch (final ArithmeticException e) {
            // Expected
            Assert.assertTrue(true);
        }
    }

    /**
     * Check all versions of sinh and cosh.
     * @param x x
     */
    private void checkSinhCosh(final double x) {
        // Threshold
        final double eps = 2E-16;

        // Reference
        final double sinh = MathLib.sinh(x);
        final double cosh = MathLib.cosh(x);

        // Sin and cos
        final double[] res = MathLib.sinhAndCosh(x);
        final double sinh2 = res[0];
        final double cosh2 = res[1];
        MathLib.sinhAndCosh(x, res);
        final double sinh3 = res[0];
        final double cosh3 = res[1];

        // Check
        checkDouble(sinh, sinh2, eps);
        checkDouble(cosh, cosh2, eps);
        checkDouble(sinh, sinh3, eps);
        checkDouble(cosh, cosh3, eps);
    }
    
    /**
     * Check a double.
     * @param expected expected
     * @param actual actual
     * @param eps epsilon for comparison
     */
    private void checkDouble(final double expected, final double actual, final double eps) {
        if (expected != 0 && !Double.isInfinite(expected)) {
            Assert.assertEquals(0., (expected - actual) / expected, eps);
        } else {
            Assert.assertEquals(expected, actual, eps);
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MATH_LIBRARY}
     * 
     * @testedMethod all methods of {@link MathWrapper}
     * 
     * @description test all methods of {@link MathWrapper} such as sin(), cos(), etc.
     * 
     * @input numbers
     * 
     * @output number, output of methods
     * 
     * @testPassCriteria result is as expected (reference: Math, threshold: 0)
     * 
     * @referenceVersion 4.3
     * 
     * @nonRegressionVersion 4.3
     */
    @Test
    public final void testMath() throws PatriusException {
        Report.printMethodHeader("testMath", "Math methods", "Math class", 0, ComparisonType.RELATIVE);
        MathLib.setMathLibrary(MathLibraryType.MATH);
        this.testMethodsVsMath(0.);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MATH_LIBRARY}
     * 
     * @testedMethod all methods of {@link StrictMathWrapper}
     * 
     * @description test all methods of {@link StrictMathWrapper} such as sin(), cos(), etc.
     * 
     * @input numbers
     * 
     * @output number, output of methods
     * 
     * @testPassCriteria result is as expected (reference: StrictMath, threshold: 0)
     * 
     * @referenceVersion 4.3
     * 
     * @nonRegressionVersion 4.3
     */
    @Test
    public final void testStrictMath() throws PatriusException {
        Report.printMethodHeader("testStrictMath", "StrictMath methods", "StrictMath class", 0, ComparisonType.RELATIVE);
        MathLib.setMathLibrary(MathLibraryType.STRICTMATH);
        this.testMethodsVsStrictMath(0.);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MATH_LIBRARY}
     * 
     * @testedMethod all methods of {@link FastestMathLibWrapper}
     * 
     * @description test all methods of {@link FastestMathLibWrapper} such as sin(), cos(), etc.
     * 
     * @input numbers
     * 
     * @output number, output of methods
     * 
     * @testPassCriteria result is as expected (reference: FastMath/Jafama, threshold: 0)
     * 
     * @referenceVersion 4.3
     * 
     * @nonRegressionVersion 4.3
     */
    @Test
    public final void testFastestMathLib() throws PatriusException {
        Report.printMethodHeader("testFastestMathLib", "FastestMathLib methods", "FastMath/Jafama class", 0,
                ComparisonType.RELATIVE);
        MathLib.setMathLibrary(MathLibraryType.FASTEST_MATHLIB);
        this.testMethodsVsFastestMathLib(0.);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MATH_LIBRARY}
     * 
     * @testedMethod all methods of {@link JafamaFastMathWrapper}
     * 
     * @description test all methods of {@link JafamaFastMathWrapper} such as sin(), cos(), etc.
     * 
     * @input numbers
     *        - Reg. number
     *        - Particular cases: 0, 1
     *        - Degraded cases: +Inf., -Inf, NaN
     * 
     * @output number, output of methods
     * 
     * @testPassCriteria result is as expected (reference: FastMath, threshold: 1E-15)
     * 
     * @referenceVersion 4.2
     * 
     * @nonRegressionVersion 4.2
     */
    @Test
    public final void testJafamaFastMath() throws PatriusException {
        Report.printMethodHeader("testJafamaFastMath", "JAFAMA FastMath methods", "FastMath class", 1E-15,
                ComparisonType.RELATIVE);
        MathLib.setMathLibrary(MathLibraryType.JAFAMA_FASTMATH);
        this.testMethodsVsFastMath(1E-15);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MATH_LIBRARY}
     * 
     * @testedMethod all methods of {@link JafamaStrictFastMathWrapper}
     * 
     * @description test all methods of {@link JafamaStrictFastMathWrapper} such as sin(), cos(), etc.
     * 
     * @input numbers
     *        - Reg. number
     *        - Particular cases: 0, 1
     *        - Degraded cases: +Inf., -Inf, NaN
     * 
     * @output number, output of methods
     * 
     * @testPassCriteria result is as expected (reference: FastMath, threshold: 1E-15)
     * 
     * @referenceVersion 4.2
     * 
     * @nonRegressionVersion 4.2
     */
    @Test
    public final void testJafamaStrictFastMath() throws PatriusException {
        Report.printMethodHeader("testJafamaStrictFastMath", "JAFAMA FastMath methods", "FastMath class", 1E-15,
                ComparisonType.RELATIVE);
        MathLib.setMathLibrary(MathLibraryType.JAFAMA_STRICT_FASTMATH);
        this.testMethodsVsFastMath(1E-15);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MATH_LIBRARY}
     * 
     * @testedMethod none
     * 
     * @description test that a math library can be defined by the user
     * 
     * @input a user-defined Math library
     * 
     * @output MathLib methods returns user-defined methods
     * 
     * @testPassCriteria result of MathLib.sin() is result of user-defined library sin() method.
     * 
     * @referenceVersion 4.2
     * 
     * @nonRegressionVersion 4.2
     */
    @Test
    public final void testUserDefinedLibrary() throws PatriusException {

        // Define a user library based on FastMath and which redefines only sin() method
        final MathLibrary userLib = new FastMathWrapper() {
            @Override
            public double sin(final double x) {
                return -2.;
            }
        };
        MathLib.setMathLibrary(userLib);

        // Check
        Assert.assertEquals(-2., MathLib.sin(0.5));
        Assert.assertEquals(FastMath.cos(0.5), MathLib.cos(0.5));
    }

    /**
     * Test all math methods vs Math.
     * 
     * @param eps comparison epsilon
     */
    private void testMethodsVsMath(final double eps) throws PatriusException {

        // Reg. values used in tests
        final double x = 0.5;
        final double x2 = 1.5;
        final double y = 0.6;
        final int yInt = 3;

        // Only one check is enough
        this.check("Sqrt (Reg. data)", MathLib.sqrt(x), Math.sqrt(x), eps);
        this.check("Sqrt (Reg. data)", MathLib.cosh(x), Math.cosh(x), eps);
        this.check("Sqrt (Reg. data)", MathLib.sinh(x), Math.sinh(x), eps);
        this.check("Sqrt (Reg. data)", MathLib.tanh(x), Math.tanh(x), eps);
        this.check("Sqrt (Reg. data)", MathLib.acosh(x2), FastMath.acosh(x2), eps);
        this.check("Sqrt (Reg. data)", MathLib.asinh(x), FastMath.asinh(x), eps);
        this.check("Sqrt (Reg. data)", MathLib.atanh(x), FastMath.atanh(x), eps);
        this.check("Sqrt (Reg. data)", MathLib.signum(x), Math.signum(x), eps);
        this.check("Sqrt (Reg. data)", MathLib.signum((float) x), Math.signum((float) x), eps);
        this.check("Sqrt (Reg. data)", MathLib.nextUp(x), Math.nextUp(x), eps);
        this.check("Sqrt (Reg. data)", MathLib.nextUp((float) x), Math.nextUp((float) x), eps);
        this.check("Sqrt (Reg. data)", MathLib.exp(x), Math.exp(x), eps);
        this.check("Sqrt (Reg. data)", MathLib.expm1(x), Math.expm1(x), eps);
        this.check("Sqrt (Reg. data)", MathLib.log(x), Math.log(x), eps);
        this.check("Sqrt (Reg. data)", MathLib.log1p(x), Math.log1p(x), eps);
        this.check("Sqrt (Reg. data)", MathLib.log10(x), Math.log10(x), eps);
        this.check("Sqrt (Reg. data)", MathLib.sin(x), Math.sin(x), eps);
        this.check("Sqrt (Reg. data)", MathLib.cos(x), Math.cos(x), eps);
        this.check("Sqrt (Reg. data)", MathLib.tan(x), Math.tan(x), eps);
        this.check("Sqrt (Reg. data)", MathLib.atan(x), Math.atan(x), eps);
        this.check("Sqrt (Reg. data)", MathLib.atan2(x, y), Math.atan2(x, y), eps);
        this.check("Sqrt (Reg. data)", MathLib.asin(x), Math.asin(x), eps);
        this.check("Sqrt (Reg. data)", MathLib.acos(x), Math.acos(x), eps);
        this.check("Sqrt (Reg. data)", MathLib.cbrt(x), Math.cbrt(x), eps);
        this.check("Sqrt (Reg. data)", MathLib.toRadians(x), Math.toRadians(x), eps);
        this.check("Sqrt (Reg. data)", MathLib.toDegrees(x), Math.toDegrees(x), eps);
        this.check("Sqrt - int (Reg. data)", MathLib.abs((int) x), Math.abs((int) x), eps);
        this.check("Sqrt - long (Reg. data)", MathLib.abs((long) x), Math.abs((long) x), eps);
        this.check("Sqrt (Reg. data)", MathLib.abs(x), Math.abs(x), eps);
        this.check("Sqrt - float (Reg. data)", MathLib.abs((float) x), Math.abs((float) x), eps);
        this.check("Sqrt (Reg. data)", MathLib.ulp(x), Math.ulp(x), eps);
        this.check("Sqrt - float (Reg. data)", MathLib.ulp((float) x), Math.ulp((float) x), eps);
        this.check("Sqrt (Reg. data)", MathLib.ulp(x), Math.ulp(x), eps);
        this.check("Sqrt - int (Reg. data)", MathLib.scalb(x, (int) y), Math.scalb(x, (int) y), eps);
        this.check("Sqrt - float (Reg. data)", MathLib.scalb((float) x, (int) y), Math.scalb((float) x, (int) y), eps);
        this.check("Sqrt (Reg. data)", MathLib.nextAfter(x, y), Math.nextAfter(x, y), eps);
        this.check("Sqrt - float (Reg. data)", MathLib.nextAfter((float) x, y), Math.nextAfter((float) x, y), eps);
        this.check("Sqrt (Reg. data)", MathLib.floor(x), Math.floor(x), eps);
        this.check("Sqrt (Reg. data)", MathLib.ceil(x), Math.ceil(x), eps);
        this.check("Sqrt (Reg. data)", MathLib.rint(x), Math.rint(x), eps);
        this.check("Sqrt (Reg. data)", MathLib.round(x), Math.round(x), eps);
        this.check("Sqrt - float (Reg. data)", MathLib.round((float) x), Math.round((float) x), eps);
        this.check("Sqrt (Reg. data)", MathLib.min(x, y), Math.min(x, y), eps);
        this.check("Sqrt - float (Reg. data)", MathLib.min((float) x, (float) y), Math.min((float) x, (float) y), eps);
        this.check("Sqrt - int (Reg. data)", MathLib.min((int) x, (int) y), Math.min((int) x, (int) y), eps);
        this.check("Sqrt - long (Reg. data)", MathLib.min((long) x, (long) y), Math.min((long) x, (long) y), eps);
        this.check("Sqrt (Reg. data)", MathLib.max(x, y), Math.max(x, y), eps);
        this.check("Sqrt - float (Reg. data)", MathLib.max((float) x, (float) y), Math.max((float) x, (float) y), eps);
        this.check("Sqrt - int (Reg. data)", MathLib.max((int) x, (int) y), Math.max((int) x, (int) y), eps);
        this.check("Sqrt - long (Reg. data)", MathLib.max((long) x, (long) y), Math.max((long) x, (long) y), eps);
        this.check("Sqrt (Reg. data)", MathLib.hypot(x, y), Math.hypot(x, y), eps);
        this.check("Sqrt (Reg. data)", MathLib.copySign(x, y), Math.copySign(x, y), eps);
        this.check("Sqrt - float (Reg. data)", MathLib.copySign((float) x, (float) y),
                Math.copySign((float) x, (float) y), eps);
        this.check("Sqrt (Reg. data)", MathLib.getExponent(x), Math.getExponent(x), eps);
        this.check("Sqrt - float (Reg. data)", MathLib.getExponent((float) x), Math.getExponent((float) x), eps);
        this.check("Pow (Reg. data - Reg. data)", MathLib.pow(x, y), Math.pow(x, y), eps);
        this.check("Pow - int (Reg. data - Reg. data)", MathLib.pow(x, yInt), Math.pow(x, yInt), eps);
        this.check("IEEERemainder (Reg. data - Reg. data)", MathLib.IEEEremainder(x, y), Math.IEEEremainder(x, y), eps);

        // Other functions
        final double ran = MathLib.random();
        Assert.assertTrue(0 <= ran && ran <= 1);
    }

    /**
     * Test all math methods vs StrictMath.
     * 
     * @param eps comparison epsilon
     */
    private void testMethodsVsStrictMath(final double eps) throws PatriusException {

        // Reg. values used in tests
        final double x = 0.5;
        final double x2 = 1.5;
        final double y = 0.6;
        final int yInt = 3;

        // Only one check is enough
        this.check("Sqrt (Reg. data)", MathLib.sqrt(x), StrictMath.sqrt(x), eps);
        this.check("Sqrt (Reg. data)", MathLib.cosh(x), StrictMath.cosh(x), eps);
        this.check("Sqrt (Reg. data)", MathLib.sinh(x), StrictMath.sinh(x), eps);
        this.check("Sqrt (Reg. data)", MathLib.tanh(x), StrictMath.tanh(x), eps);
        this.check("Sqrt (Reg. data)", MathLib.acosh(x2), FastMath.acosh(x2), eps);
        this.check("Sqrt (Reg. data)", MathLib.asinh(x), FastMath.asinh(x), eps);
        this.check("Sqrt (Reg. data)", MathLib.atanh(x), FastMath.atanh(x), eps);
        this.check("Sqrt (Reg. data)", MathLib.signum(x), StrictMath.signum(x), eps);
        this.check("Sqrt (Reg. data)", MathLib.signum((float) x), StrictMath.signum((float) x), eps);
        this.check("Sqrt (Reg. data)", MathLib.nextUp(x), StrictMath.nextUp(x), eps);
        this.check("Sqrt (Reg. data)", MathLib.nextUp((float) x), StrictMath.nextUp((float) x), eps);
        this.check("Sqrt (Reg. data)", MathLib.exp(x), StrictMath.exp(x), eps);
        this.check("Sqrt (Reg. data)", MathLib.expm1(x), StrictMath.expm1(x), eps);
        this.check("Sqrt (Reg. data)", MathLib.log(x), StrictMath.log(x), eps);
        this.check("Sqrt (Reg. data)", MathLib.log1p(x), StrictMath.log1p(x), eps);
        this.check("Sqrt (Reg. data)", MathLib.log10(x), StrictMath.log10(x), eps);
        this.check("Sqrt (Reg. data)", MathLib.sin(x), StrictMath.sin(x), eps);
        this.check("Sqrt (Reg. data)", MathLib.cos(x), StrictMath.cos(x), eps);
        this.check("Sqrt (Reg. data)", MathLib.tan(x), StrictMath.tan(x), eps);
        this.check("Sqrt (Reg. data)", MathLib.atan(x), StrictMath.atan(x), eps);
        this.check("Sqrt (Reg. data)", MathLib.atan2(x, y), StrictMath.atan2(x, y), eps);
        this.check("Sqrt (Reg. data)", MathLib.asin(x), StrictMath.asin(x), eps);
        this.check("Sqrt (Reg. data)", MathLib.acos(x), StrictMath.acos(x), eps);
        this.check("Sqrt (Reg. data)", MathLib.cbrt(x), StrictMath.cbrt(x), eps);
        this.check("Sqrt (Reg. data)", MathLib.toRadians(x), StrictMath.toRadians(x), eps);
        this.check("Sqrt (Reg. data)", MathLib.toDegrees(x), StrictMath.toDegrees(x), eps);
        this.check("Sqrt - int (Reg. data)", MathLib.abs((int) x), StrictMath.abs((int) x), eps);
        this.check("Sqrt - long (Reg. data)", MathLib.abs((long) x), StrictMath.abs((long) x), eps);
        this.check("Sqrt (Reg. data)", MathLib.abs(x), StrictMath.abs(x), eps);
        this.check("Sqrt - float (Reg. data)", MathLib.abs((float) x), StrictMath.abs((float) x), eps);
        this.check("Sqrt (Reg. data)", MathLib.ulp(x), StrictMath.ulp(x), eps);
        this.check("Sqrt - float (Reg. data)", MathLib.ulp((float) x), StrictMath.ulp((float) x), eps);
        this.check("Sqrt (Reg. data)", MathLib.ulp(x), StrictMath.ulp(x), eps);
        this.check("Sqrt - int (Reg. data)", MathLib.scalb(x, (int) y), StrictMath.scalb(x, (int) y), eps);
        this.check("Sqrt - float (Reg. data)", MathLib.scalb((float) x, (int) y), StrictMath.scalb((float) x, (int) y),
                eps);
        this.check("Sqrt (Reg. data)", MathLib.nextAfter(x, y), StrictMath.nextAfter(x, y), eps);
        this.check("Sqrt - float (Reg. data)", MathLib.nextAfter((float) x, y), StrictMath.nextAfter((float) x, y), eps);
        this.check("Sqrt (Reg. data)", MathLib.floor(x), StrictMath.floor(x), eps);
        this.check("Sqrt (Reg. data)", MathLib.ceil(x), StrictMath.ceil(x), eps);
        this.check("Sqrt (Reg. data)", MathLib.rint(x), StrictMath.rint(x), eps);
        this.check("Sqrt (Reg. data)", MathLib.round(x), StrictMath.round(x), eps);
        this.check("Sqrt - float (Reg. data)", MathLib.round((float) x), StrictMath.round((float) x), eps);
        this.check("Sqrt (Reg. data)", MathLib.min(x, y), StrictMath.min(x, y), eps);
        this.check("Sqrt - float (Reg. data)", MathLib.min((float) x, (float) y), StrictMath.min((float) x, (float) y),
                eps);
        this.check("Sqrt - int (Reg. data)", MathLib.min((int) x, (int) y), StrictMath.min((int) x, (int) y), eps);
        this.check("Sqrt - long (Reg. data)", MathLib.min((long) x, (long) y), StrictMath.min((long) x, (long) y), eps);
        this.check("Sqrt (Reg. data)", MathLib.max(x, y), StrictMath.max(x, y), eps);
        this.check("Sqrt - float (Reg. data)", MathLib.max((float) x, (float) y), StrictMath.max((float) x, (float) y),
                eps);
        this.check("Sqrt - int (Reg. data)", MathLib.max((int) x, (int) y), StrictMath.max((int) x, (int) y), eps);
        this.check("Sqrt - long (Reg. data)", MathLib.max((long) x, (long) y), StrictMath.max((long) x, (long) y), eps);
        this.check("Sqrt (Reg. data)", MathLib.hypot(x, y), StrictMath.hypot(x, y), eps);
        this.check("Sqrt (Reg. data)", MathLib.copySign(x, y), StrictMath.copySign(x, y), eps);
        this.check("Sqrt - float (Reg. data)", MathLib.copySign((float) x, (float) y),
                StrictMath.copySign((float) x, (float) y), eps);
        this.check("Sqrt (Reg. data)", MathLib.getExponent(x), StrictMath.getExponent(x), eps);
        this.check("Sqrt - float (Reg. data)", MathLib.getExponent((float) x), StrictMath.getExponent((float) x), eps);
        this.check("Pow (Reg. data - Reg. data)", MathLib.pow(x, y), StrictMath.pow(x, y), eps);
        this.check("Pow - int (Reg. data - Reg. data)", MathLib.pow(x, yInt), StrictMath.pow(x, yInt), eps);
        this.check("IEEERemainder (Reg. data - Reg. data)", MathLib.IEEEremainder(x, y),
                StrictMath.IEEEremainder(x, y), eps);

        // Other functions
        final double ran = MathLib.random();
        Assert.assertTrue(0 <= ran && ran <= 1);
    }

    /**
     * Test all math methods vs FastMath/Jafama.
     * 
     * @param eps comparison epsilon
     */
    private void testMethodsVsFastestMathLib(final double eps) throws PatriusException {

        // Reg. values used in tests
        final double x = 0.5;
        final double x2 = 1.5;
        final double y = 0.6;
        final int yInt = 3;

        // Only one check is enough
        this.check("Sqrt (Reg. data)", MathLib.sqrt(x), net.jafama.FastMath.sqrt(x), eps);
        this.check("Cosh (Reg. data)", MathLib.cosh(x), net.jafama.FastMath.cosh(x), eps);
        this.check("Sinh (Reg. data)", MathLib.sinh(x), net.jafama.FastMath.sinh(x), eps);
        this.check("Tanh (Reg. data)", MathLib.tanh(x), net.jafama.FastMath.tanh(x), eps);
        this.check("Acosh (Reg. data)", MathLib.acosh(x2), net.jafama.StrictFastMath.acosh(x2), eps);
        this.check("Asinh (Reg. data)", MathLib.asinh(x), net.jafama.StrictFastMath.asinh(x), eps);
        this.check("Atanh (Reg. data)", MathLib.atanh(x), net.jafama.FastMath.atanh(x), eps);
        this.check("Signum (Reg. data)", MathLib.signum(x), FastMath.signum(x), eps);
        this.check("Signum (Reg. data)", MathLib.signum((float) x), FastMath.signum((float) x), eps);
        this.check("NextUp (Reg. data)", MathLib.nextUp(x), net.jafama.FastMath.nextUp(x), eps);
        this.check("NextUp - float (Reg. data)", MathLib.nextUp((float) x), net.jafama.FastMath.nextUp((float) x), eps);
        this.check("Exp (Reg. data)", MathLib.exp(x), net.jafama.FastMath.exp(x), eps);
        this.check("Expm1 (Reg. data)", MathLib.expm1(x), net.jafama.FastMath.expm1(x), eps);
        this.check("Log (Reg. data)", MathLib.log(x), net.jafama.StrictFastMath.log(x), eps);
        this.check("Log1p (Reg. data)", MathLib.log1p(x), net.jafama.FastMath.log1p(x), eps);
        this.check("Log10 (Reg. data)", MathLib.log10(x), net.jafama.StrictFastMath.log10(x), eps);
        this.check("Sin (Reg. data)", MathLib.sin(x), net.jafama.FastMath.sin(x), eps);
        this.check("Cos (Reg. data)", MathLib.cos(x), net.jafama.FastMath.cos(x), eps);
        this.check("Tan (Reg. data)", MathLib.tan(x), net.jafama.FastMath.tan(x), eps);
        this.check("Atan (Reg. data)", MathLib.atan(x), net.jafama.FastMath.atan(x), eps);
        this.check("Atan2 (Reg. data)", MathLib.atan2(x, y), net.jafama.FastMath.atan2(x, y), eps);
        this.check("Asin (Reg. data)", MathLib.asin(x), net.jafama.FastMath.asin(x), eps);
        this.check("Acos (Reg. data)", MathLib.acos(x), net.jafama.FastMath.acos(x), eps);
        this.check("Cbrt (Reg. data)", MathLib.cbrt(x), net.jafama.FastMath.cbrt(x), eps);
        this.check("ToRadians (Reg. data)", MathLib.toRadians(x), net.jafama.FastMath.toRadians(x), eps);
        this.check("ToDegrees (Reg. data)", MathLib.toDegrees(x), net.jafama.FastMath.toDegrees(x), eps);
        this.check("Abs - int (Reg. data)", MathLib.abs((int) x), FastMath.abs((int) x), eps);
        this.check("Abs - long (Reg. data)", MathLib.abs((long) x), FastMath.abs((long) x), eps);
        this.check("Abs (Reg. data)", MathLib.abs(x), FastMath.abs(x), eps);
        this.check("Abs - float (Reg. data)", MathLib.abs((float) x), FastMath.abs((float) x), eps);
        this.check("Ulp (Reg. data)", MathLib.ulp(x), net.jafama.FastMath.ulp(x), eps);
        this.check("Ulp - float (Reg. data)", MathLib.ulp((float) x), net.jafama.FastMath.ulp((float) x), eps);
        this.check("Scalb - int (Reg. data)", MathLib.scalb(x, (int) y), net.jafama.FastMath.scalb(x, (int) y), eps);
        this.check("Scalb - float (Reg. data)", MathLib.scalb((float) x, (int) y),
                net.jafama.FastMath.scalb((float) x, (int) y), eps);
        this.check("NextAfter (Reg. data)", MathLib.nextAfter(x, y), net.jafama.FastMath.nextAfter(x, y), eps);
        this.check("NextAfter - float (Reg. data)", MathLib.nextAfter((float) x, y),
                net.jafama.FastMath.nextAfter((float) x, y), eps);
        this.check("Floor (Reg. data)", MathLib.floor(x), StrictMath.floor(x), eps);
        this.check("Ceil (Reg. data)", MathLib.ceil(x), StrictMath.ceil(x), eps);
        this.check("Rint (Reg. data)", MathLib.rint(x), FastMath.rint(x), eps);
        this.check("Round (Reg. data)", MathLib.round(x), StrictMath.round(x), eps);
        this.check("Round - float (Reg. data)", MathLib.round((float) x), StrictMath.round((float) x), eps);
        this.check("Min (Reg. data)", MathLib.min(x, y), net.jafama.FastMath.min(x, y), eps);
        this.check("Min - float (Reg. data)", MathLib.min((float) x, (float) y),
                net.jafama.FastMath.min((float) x, (float) y), eps);
        this.check("Min - int (Reg. data)", MathLib.min((int) x, (int) y), FastMath.min((int) x, (int) y), eps);
        this.check("Min - long (Reg. data)", MathLib.min((long) x, (long) y),
                net.jafama.FastMath.min((long) x, (long) y), eps);
        this.check("Max (Reg. data)", MathLib.max(x, y), net.jafama.FastMath.max(x, y), eps);
        this.check("Max - float (Reg. data)", MathLib.max((float) x, (float) y),
                net.jafama.FastMath.max((float) x, (float) y), eps);
        this.check("Max - int (Reg. data)", MathLib.max((int) x, (int) y), FastMath.max((int) x, (int) y), eps);
        this.check("Max - long (Reg. data)", MathLib.max((long) x, (long) y),
                net.jafama.FastMath.max((long) x, (long) y), eps);
        this.check("Hypot (Reg. data)", MathLib.hypot(x, y), net.jafama.FastMath.hypot(x, y), eps);
        this.check("CopySign (Reg. data)", MathLib.copySign(x, y), FastMath.copySign(x, y), eps);
        this.check("CopySign - float (Reg. data)", MathLib.copySign((float) x, (float) y),
                FastMath.copySign((float) x, (float) y), eps);
        this.check("GetExponent (Reg. data)", MathLib.getExponent(x), StrictMath.getExponent(x), eps);
        this.check("GetExponent - float (Reg. data)", MathLib.getExponent((float) x),
                StrictMath.getExponent((float) x), eps);
        this.check("Pow (Reg. data - Reg. data)", MathLib.pow(x, y), net.jafama.FastMath.pow(x, y), eps);
        this.check("Pow - int (Reg. data - Reg. data)", MathLib.pow(x, yInt), FastMath.pow(x, yInt), eps);
        this.check("IEEERemainder (Reg. data - Reg. data)", MathLib.IEEEremainder(x, y),
                net.jafama.FastMath.IEEEremainder(x, y), eps);

        // Other functions
        final double ran = MathLib.random();
        Assert.assertTrue(0 <= ran && ran <= 1);
    }

    /**
     * Test all math methods vs FastMath.
     * 
     * @param eps comparison epsilon
     */
    private void testMethodsVsFastMath(final double eps) throws PatriusException {

        // Reg. values used in tests
        final double x = 0.5;
        final double x2 = 1.5;
        final double y = 0.6;
        final int xInt = 2;
        final int yInt = 3;
        final long xLong = 2;
        final long yLong = 3;

        // Sqrt
        this.checkPositiveFunction("Sqrt", x, eps, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return MathLib.sqrt(x);
            }
        }, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return FastMath.sqrt(x);
            }
        });

        // Cosh
        this.checkFunction("Cosh", x, eps, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return MathLib.cosh(x);
            }
        }, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return FastMath.cosh(x);
            }
        });

        // Sinh
        this.checkFunction("Sinh", x, eps, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return MathLib.sinh(x);
            }
        }, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return FastMath.sinh(x);
            }
        });

        // Tanh
        this.checkFunction("Tanh", x, eps, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return MathLib.tanh(x);
            }
        }, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return FastMath.tanh(x);
            }
        });

        // Acosh
        this.checkLargerThan1Function("Acosh", x2, eps, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return MathLib.acosh(x);
            }
        }, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return FastMath.acosh(x);
            }
        });

        // Asinh
        this.checkFunction("Asinh", x, eps, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return MathLib.asinh(x);
            }
        }, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return FastMath.asinh(x);
            }
        });

        // Atanh
        this.checkClampedFunction("Atanh", x, eps, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return MathLib.atanh(x);
            }
        }, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return FastMath.atanh(x);
            }
        });

        // Signum
        this.checkFunction("Signum", x, eps, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return MathLib.signum(x);
            }
        }, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return FastMath.signum(x);
            }
        });

        // Signum (float)
        this.checkFunction("Signum - float", x, eps, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return MathLib.signum((float) x);
            }
        }, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return FastMath.signum((float) x);
            }
        });

        // NextUp
        this.checkFunction("NextUp", x, eps, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return MathLib.nextUp(x);
            }
        }, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return FastMath.nextUp(x);
            }
        });

        // NextUp (float)
        this.checkFunction("NextUp - float", x, eps, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return MathLib.nextUp((float) x);
            }
        }, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return FastMath.nextUp((float) x);
            }
        });

        // Exp
        this.checkFunction("Exp", x, eps, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return MathLib.exp(x);
            }
        }, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return FastMath.exp(x);
            }
        });

        // Expm1
        this.checkFunction("Expm1", x, eps, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return MathLib.expm1(x);
            }
        }, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return FastMath.expm1(x);
            }
        });

        // Log
        this.checkPositiveFunction("Log", x, eps, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return MathLib.log(x);
            }
        }, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return FastMath.log(x);
            }
        });

        // Log1p
        this.checkPositiveFunction("Log1p", x, eps, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return MathLib.log1p(x);
            }
        }, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return FastMath.log1p(x);
            }
        });

        // Log10
        this.checkPositiveFunction("Log10", x, eps, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return MathLib.log10(x);
            }
        }, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return FastMath.log10(x);
            }
        });

        // Sin
        this.checkClampedFunction("Sin", x, eps, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return MathLib.sin(x);
            }
        }, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return FastMath.sin(x);
            }
        });

        // Cos
        this.checkClampedFunction("Cos", x, eps, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return MathLib.cos(x);
            }
        }, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return FastMath.cos(x);
            }
        });

        // Tan
        this.checkClampedFunction("Tan", x, eps, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return MathLib.tan(x);
            }
        }, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return FastMath.tan(x);
            }
        });

        // Atan
        this.checkFunction("Atan", x, eps, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return MathLib.atan(x);
            }
        }, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return FastMath.atan(x);
            }
        });

        // Atan2
        this.checkFunction("Atan2", x, y, eps, new BivariateFunction() {
            @Override
            public double value(final double x,
                    final double y) {
                return MathLib.atan2(x, y);
            }
        }, new BivariateFunction() {
            @Override
            public double value(final double x,
                    final double y) {
                return FastMath.atan2(x, y);
            }
        });

        // Asin
        this.checkClampedFunction("Asin", x, eps, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return MathLib.asin(x);
            }
        }, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return FastMath.asin(x);
            }
        });

        // Acos
        this.checkClampedFunction("Acos", x, eps, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return MathLib.acos(x);
            }
        }, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return FastMath.acos(x);
            }
        });

        // Cbrt
        this.checkFunction("Cbrt", x, eps, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return MathLib.cbrt(x);
            }
        }, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return FastMath.cbrt(x);
            }
        });

        // ToRadians
        this.checkFunction("ToRadians", x, eps, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return MathLib.toRadians(x);
            }
        }, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return FastMath.toRadians(x);
            }
        });

        // ToDegrees
        this.checkFunction("ToDegrees", x, eps, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return MathLib.toDegrees(x);
            }
        }, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return FastMath.toDegrees(x);
            }
        });

        // Abs (int)
        this.checkFunction("Abs - int", xInt, eps, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return MathLib.abs((int) x);
            }
        }, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return FastMath.abs((int) x);
            }
        });

        // Abs (long)
        this.checkFunction("Abs - long", xLong, eps, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return MathLib.abs((long) x);
            }
        }, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return FastMath.abs((long) x);
            }
        });

        // Abs (double)
        this.checkFunction("Abs", x, eps, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return MathLib.abs(x);
            }
        }, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return FastMath.abs(x);
            }
        });

        // Abs (float)
        this.checkFunction("Abs - float", x, eps, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return MathLib.abs((float) x);
            }
        }, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return FastMath.abs((float) x);
            }
        });

        // Ulp
        this.checkFunction("Ulp", x, eps, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return MathLib.ulp(x);
            }
        }, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return FastMath.ulp(x);
            }
        });

        // Ulp - float
        this.checkFunction("Ulp - float", x, eps, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return MathLib.ulp((float) x);
            }
        }, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return FastMath.ulp((float) x);
            }
        });

        // Scalb
        this.checkFunction("Scalb", x, yInt, eps, new BivariateFunction() {
            @Override
            public double value(final double x,
                    final double y) {
                return MathLib.scalb(x, (int) y);
            }
        }, new BivariateFunction() {
            @Override
            public double value(final double x,
                    final double y) {
                return FastMath.scalb(x, (int) y);
            }
        });

        // Scalb - float
        this.checkFunction("Scalb - float", x, yInt, eps, new BivariateFunction() {
            @Override
            public double value(final double x,
                    final double y) {
                return MathLib.scalb((float) x, (int) y);
            }
        }, new BivariateFunction() {
            @Override
            public double value(final double x,
                    final double y) {
                return FastMath.scalb((float) x, (int) y);
            }
        });

        // NextAfter
        this.checkFunction("NextAfter", x, y, eps, new BivariateFunction() {
            @Override
            public double value(final double x,
                    final double y) {
                return MathLib.nextAfter(x, y);
            }
        }, new BivariateFunction() {
            @Override
            public double value(final double x,
                    final double y) {
                return FastMath.nextAfter(x, y);
            }
        });

        // NextAfter - float
        this.checkFunction("NextAfter - float", x, yInt, eps, new BivariateFunction() {
            @Override
            public double value(final double x,
                    final double y) {
                return MathLib.nextAfter((float) x, y);
            }
        }, new BivariateFunction() {
            @Override
            public double value(final double x,
                    final double y) {
                return FastMath.nextAfter((float) x, y);
            }
        });

        // Floor
        this.checkFunction("Floor", x, eps, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return MathLib.floor(x);
            }
        }, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return FastMath.floor(x);
            }
        });

        // Ceil
        this.checkFunction("Ceil", x, eps, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return MathLib.ceil(x);
            }
        }, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return FastMath.ceil(x);
            }
        });

        // Rint
        this.checkFunction("Rint", x, eps, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return MathLib.rint(x);
            }
        }, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return FastMath.rint(x);
            }
        });

        // Round
        this.checkFunction("Round", x, eps, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return MathLib.round(x);
            }
        }, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return FastMath.round(x);
            }
        });

        // Round - float
        this.checkFunction("Round - float", x, eps, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return MathLib.round((float) x);
            }
        }, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return FastMath.round((float) x);
            }
        });

        // Min (double)
        this.checkFunction("Min", x, y, eps, new BivariateFunction() {
            @Override
            public double value(final double x,
                    final double y) {
                return MathLib.min(x, y);
            }
        }, new BivariateFunction() {
            @Override
            public double value(final double x,
                    final double y) {
                return FastMath.min(x, y);
            }
        });

        // Min (float)
        this.checkFunction("Min - float", x, y, eps, new BivariateFunction() {
            @Override
            public double value(final double x,
                    final double y) {
                return MathLib.min((float) x, (float) y);
            }
        }, new BivariateFunction() {
            @Override
            public double value(final double x,
                    final double y) {
                return FastMath.min((float) x, (float) y);
            }
        });

        // Min (int)
        this.checkFunction("Min - int", xInt, yInt, eps, new BivariateFunction() {
            @Override
            public double value(final double x,
                    final double y) {
                return MathLib.min((int) x, (int) y);
            }
        }, new BivariateFunction() {
            @Override
            public double value(final double x,
                    final double y) {
                return FastMath.min((int) x, (int) y);
            }
        });

        // Min (long)
        this.checkFunction("Min - long", xLong, yLong, eps, new BivariateFunction() {
            @Override
            public double value(final double x,
                    final double y) {
                return MathLib.min((long) x, (long) y);
            }
        }, new BivariateFunction() {
            @Override
            public double value(final double x,
                    final double y) {
                return FastMath.min((long) x, (long) y);
            }
        });

        // Max
        this.checkFunction("Max", x, y, eps, new BivariateFunction() {
            @Override
            public double value(final double x,
                    final double y) {
                return MathLib.max(x, y);
            }
        }, new BivariateFunction() {
            @Override
            public double value(final double x,
                    final double y) {
                return FastMath.max(x, y);
            }
        });

        // Max (float)
        this.checkFunction("Max - float", x, y, eps, new BivariateFunction() {
            @Override
            public double value(final double x,
                    final double y) {
                return MathLib.max((float) x, (float) y);
            }
        }, new BivariateFunction() {
            @Override
            public double value(final double x,
                    final double y) {
                return FastMath.max((float) x, (float) y);
            }
        });

        // Max (int)
        this.checkFunction("Max - int", xInt, yInt, eps, new BivariateFunction() {
            @Override
            public double value(final double x,
                    final double y) {
                return MathLib.max((int) x, (int) y);
            }
        }, new BivariateFunction() {
            @Override
            public double value(final double x,
                    final double y) {
                return FastMath.max((int) x, (int) y);
            }
        });

        // Max (long)
        this.checkFunction("Max - long", xLong, yLong, eps, new BivariateFunction() {
            @Override
            public double value(final double x,
                    final double y) {
                return MathLib.max((long) x, (long) y);
            }
        }, new BivariateFunction() {
            @Override
            public double value(final double x,
                    final double y) {
                return FastMath.max((long) x, (long) y);
            }
        });

        // Hypot
        this.checkFunction("Hypot", x, y, eps, new BivariateFunction() {
            @Override
            public double value(final double x,
                    final double y) {
                return MathLib.hypot(x, y);
            }
        }, new BivariateFunction() {
            @Override
            public double value(final double x,
                    final double y) {
                return FastMath.hypot(x, y);
            }
        });

        // CopySign
        this.checkFunction("CopySign", x, y, eps, new BivariateFunction() {
            @Override
            public double value(final double x,
                    final double y) {
                return MathLib.copySign(x, y);
            }
        }, new BivariateFunction() {
            @Override
            public double value(final double x,
                    final double y) {
                return FastMath.copySign(x, y);
            }
        });

        // CopySign - float
        this.checkFunction("CopySign - float", x, y, eps, new BivariateFunction() {
            @Override
            public double value(final double x,
                    final double y) {
                return MathLib.copySign((float) x, (float) y);
            }
        }, new BivariateFunction() {
            @Override
            public double value(final double x,
                    final double y) {
                return FastMath.copySign((float) x, (float) y);
            }
        });

        // GetExponent
        this.checkFunction("GetExponent", x, eps, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return MathLib.getExponent(x);
            }
        }, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return FastMath.getExponent(x);
            }
        });

        // GetExponent - float
        this.checkFunction("GetExponent - float", x, eps, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return MathLib.getExponent((float) x);
            }
        }, new UnivariateFunction() {
            @Override
            public double value(final double x) {
                return FastMath.getExponent((float) x);
            }
        });

        // Pow (double)
        final BivariateFunction fapow = new BivariateFunction() {
            @Override
            public double value(final double x,
                    final double y) {
                return MathLib.pow(x, y);
            }
        };
        final BivariateFunction fepow = new BivariateFunction() {
            @Override
            public double value(final double x,
                    final double y) {
                return FastMath.pow(x, y);
            }
        };
        this.check("Pow (Reg. data - Reg. data)", MathLib.pow(x, y), FastMath.pow(x, y), eps);
        this.check("Pow (Reg. data - 0)", MathLib.pow(x, 0.), FastMath.pow(x, 0.), eps);
        this.check("Pow (Reg. data - 1)", MathLib.pow(x, 1.), FastMath.pow(x, 1.), eps);
        this.check("Pow (Reg. data - +Inf.)", MathLib.pow(x, Double.POSITIVE_INFINITY),
                FastMath.pow(x, Double.POSITIVE_INFINITY), eps);
        this.check("Pow (Reg. data - -Inf.)", MathLib.pow(x, Double.NEGATIVE_INFINITY),
                FastMath.pow(x, Double.NEGATIVE_INFINITY), eps);
        this.check("Pow (0 - Reg. data)", MathLib.pow(0, y), FastMath.pow(0, y), eps);
        this.check("Pow (0 - 0)", MathLib.pow(0, 0.), FastMath.pow(0, 0.), eps);
        this.check("Pow (0 - 1)", MathLib.pow(0, 1.), FastMath.pow(0, 1.), eps);
        this.check("Pow (0 - +Inf.)", MathLib.pow(0, Double.POSITIVE_INFINITY),
                FastMath.pow(0, Double.POSITIVE_INFINITY), eps);
        this.check("Pow (0 - -Inf.)", MathLib.pow(0, Double.NEGATIVE_INFINITY),
                FastMath.pow(0, Double.NEGATIVE_INFINITY), eps);
        this.check("Pow (1 - Reg. data)", MathLib.pow(1, y), FastMath.pow(1, y), eps);
        this.check("Pow (1 - 0)", MathLib.pow(1, 0.), FastMath.pow(1, 0.), eps);
        this.check("Pow (1 - 1)", MathLib.pow(1, 1.), FastMath.pow(1, 1.), eps);
        this.checkException("Pow (1 - +Inf.)", 1, Double.POSITIVE_INFINITY, fapow, fepow);
        this.check("Pow (1 - -Inf.)", MathLib.pow(1, Double.NEGATIVE_INFINITY),
                FastMath.pow(1, Double.NEGATIVE_INFINITY), eps);
        this.check("Pow (+Inf - Reg. data)", MathLib.pow(Double.POSITIVE_INFINITY, y),
                FastMath.pow(Double.POSITIVE_INFINITY, y), eps);
        this.check("Pow (+Inf - 0)", MathLib.pow(Double.POSITIVE_INFINITY, 0.),
                FastMath.pow(Double.POSITIVE_INFINITY, 0.), eps);
        this.check("Pow (+Inf - 1)", MathLib.pow(Double.POSITIVE_INFINITY, 1.),
                FastMath.pow(Double.POSITIVE_INFINITY, 1.), eps);
        this.check("Pow (+Inf - +Inf.)", MathLib.pow(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY),
                FastMath.pow(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY), eps);
        this.check("Pow (+Inf - -Inf.)", MathLib.pow(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY),
                FastMath.pow(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY), eps);
        this.check("Pow (-Inf. - Reg. data)", MathLib.pow(Double.NEGATIVE_INFINITY, y),
                FastMath.pow(Double.NEGATIVE_INFINITY, y), eps);
        this.check("Pow (-Inf. - 0)", MathLib.pow(Double.NEGATIVE_INFINITY, 0.),
                FastMath.pow(Double.NEGATIVE_INFINITY, 0.), eps);
        this.check("Pow (-Inf. - 1)", MathLib.pow(Double.NEGATIVE_INFINITY, 1.),
                FastMath.pow(Double.NEGATIVE_INFINITY, 1.), eps);
        this.check("Pow (-Inf. - +Inf.)", MathLib.pow(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY),
                FastMath.pow(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY), eps);
        this.check("Pow (-Inf. - -Inf.)", MathLib.pow(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY),
                FastMath.pow(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY), eps);

        // Pow (int)
        this.check("Pow - int (Reg. data - Reg. data)", MathLib.pow(x, yInt), FastMath.pow(x, yInt), eps);
        this.check("Pow - int (Reg. data - 0)", MathLib.pow(x, 0), FastMath.pow(x, 0), eps);
        this.check("Pow - int (Reg. data - 1)", MathLib.pow(x, 1), FastMath.pow(x, 1), eps);
        this.check("Pow - int (0 - Reg. data)", MathLib.pow(0, yInt), FastMath.pow(0, yInt), eps);
        this.check("Pow - int (0 - 0)", MathLib.pow(0, 0), FastMath.pow(0, 0), eps);
        this.check("Pow - int (0 - 1)", MathLib.pow(0, 1), FastMath.pow(0, 1), eps);
        this.check("Pow - int (1 - Reg. data)", MathLib.pow(1, yInt), FastMath.pow(1, yInt), eps);
        this.check("Pow - int (1 - 0)", MathLib.pow(1, 0), FastMath.pow(1, 0), eps);
        this.check("Pow - int (1 - 1)", MathLib.pow(1, 1), FastMath.pow(1, 1), eps);
        this.check("Pow - int (+Inf - Reg. data)", MathLib.pow(Double.POSITIVE_INFINITY, yInt),
                new FastMathWrapper().pow(Double.POSITIVE_INFINITY, yInt), eps);
        this.check("Pow - int (+Inf - 0)", MathLib.pow(Double.POSITIVE_INFINITY, 0),
                FastMath.pow(Double.POSITIVE_INFINITY, 0), eps);
        this.check("Pow - int (+Inf - 1)", MathLib.pow(Double.POSITIVE_INFINITY, 1),
                new FastMathWrapper().pow(Double.POSITIVE_INFINITY, 1), eps);
        this.check("Pow - int (-Inf. - Reg. data)", MathLib.pow(Double.NEGATIVE_INFINITY, yInt),
                new FastMathWrapper().pow(Double.NEGATIVE_INFINITY, yInt), eps);
        this.check("Pow - int (-Inf. - 0)", MathLib.pow(Double.NEGATIVE_INFINITY, 0),
                FastMath.pow(Double.NEGATIVE_INFINITY, 0), eps);
        this.check("Pow - int (-Inf. - 1)", MathLib.pow(Double.NEGATIVE_INFINITY, 1),
                new FastMathWrapper().pow(Double.NEGATIVE_INFINITY, 1), eps);

        // IEEERemainder
        final BivariateFunction fa = new BivariateFunction() {
            @Override
            public double value(final double x,
                    final double y) {
                return MathLib.IEEEremainder(x, y);
            }
        };
        final BivariateFunction fe = new BivariateFunction() {
            @Override
            public double value(final double x,
                    final double y) {
                return FastMath.IEEEremainder(x, y);
            }
        };
        this.check("IEEERemainder (Reg. data - Reg. data)", fa.value(x, y), fe.value(x, y), eps);
        this.checkException("IEEEremainder (Reg. data - 0)", x, 0., fa, fe);
        this.check("IEEEremainder (Reg. data - 1)", fa.value(x, 1.), fe.value(x, 1.), eps);
        this.check("IEEEremainder (Reg. data - +Inf.)", fa.value(x, Double.POSITIVE_INFINITY),
                fe.value(x, Double.POSITIVE_INFINITY), eps);
        this.check("IEEEremainder (Reg. data - -Inf.)", fa.value(x, Double.NEGATIVE_INFINITY),
                fe.value(x, Double.NEGATIVE_INFINITY), eps);
        this.check("IEEEremainder (0 - Reg. data)", fa.value(0, y), fe.value(0, y), eps);
        this.checkException("IEEEremainder (0 - 0)", 0., 0., fa, fe);
        this.check("IEEEremainder (0 - 1)", fa.value(0, 1.), fe.value(0, 1.), eps);
        this.check("IEEEremainder (0 - +Inf.)", fa.value(0, Double.POSITIVE_INFINITY),
                fe.value(0, Double.POSITIVE_INFINITY), eps);
        this.check("IEEEremainder (0 - -Inf.)", fa.value(0, Double.NEGATIVE_INFINITY),
                fe.value(0, Double.NEGATIVE_INFINITY), eps);
        this.check("IEEEremainder (1 - Reg. data)", fa.value(1, y), fe.value(1, y), eps);
        this.checkException("IEEEremainder (1 - 0)", 1., 0., fa, fe);
        this.check("IEEEremainder (1 - 1)", fa.value(1, 1.), fe.value(1, 1.), eps);
        this.check("IEEEremainder (1 - +Inf.)", fa.value(1, Double.POSITIVE_INFINITY),
                fe.value(1, Double.POSITIVE_INFINITY), eps);
        this.check("IEEEremainder (1 - -Inf.)", fa.value(1, Double.NEGATIVE_INFINITY),
                fe.value(1, Double.NEGATIVE_INFINITY), eps);
        this.checkException("IEEEremainder (+Inf - Reg. data)", Double.POSITIVE_INFINITY, y, fa, fe);
        this.checkException("IEEEremainder (+Inf - 0)", Double.POSITIVE_INFINITY, 0., fa, fe);
        this.checkException("IEEEremainder (+Inf - 1)", Double.POSITIVE_INFINITY, 1., fa, fe);
        this.checkException("IEEEremainder (+Inf - +Inf.)", Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, fa, fe);
        this.checkException("IEEEremainder (+Inf - -Inf.)", Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, fa, fe);
        this.checkException("IEEEremainder (-Inf. - Reg. data)", Double.NEGATIVE_INFINITY, y, fa, fe);
        this.checkException("IEEEremainder (-Inf. - 0)", Double.NEGATIVE_INFINITY, 0., fa, fe);
        this.checkException("IEEEremainder (-Inf. - 1)", Double.NEGATIVE_INFINITY, 1., fa, fe);
        this.checkException("IEEEremainder (-Inf. - +Inf.)", Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, fa, fe);
        this.checkException("IEEEremainder (-Inf. - -Inf.)", Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, fa, fe);

        // Other functions
        final double ran = MathLib.random();
        Assert.assertTrue(0 <= ran && ran <= 1);
    }

    /**
     * Check a function.
     * 
     * @param label label
     * @param x x
     * @param a actual function
     * @param e expected function
     * @param eps comparison epsilon
     */
    private void checkLargerThan1Function(final String label,
            final double x,
            final double eps,
            final UnivariateFunction a,
            final UnivariateFunction e) {
        this.check(label + " (Reg. data)", a.value(x), e.value(x), eps);
        this.checkException(label + " (0)", 0, a, e);
        this.check(label + " (1)", a.value(1.), e.value(1.), eps);
        this.check(label + " (+Inf.)", a.value(Double.POSITIVE_INFINITY), e.value(Double.POSITIVE_INFINITY), eps);
        this.checkException(label + " (-Inf.)", Double.NEGATIVE_INFINITY, a, e);
    }

    /**
     * Check a function.
     * 
     * @param label label
     * @param x x
     * @param a actual function
     * @param e expected function
     * @param eps comparison epsilon
     */
    private void checkPositiveFunction(final String label,
            final double x,
            final double eps,
            final UnivariateFunction a,
            final UnivariateFunction e) {
        this.check(label + " (Reg. data)", a.value(x), e.value(x), eps);
        this.check(label + " (0)", a.value(0.), e.value(0.), eps);
        this.check(label + " (1)", a.value(1.), e.value(1.), eps);
        this.check(label + " (+Inf.)", a.value(Double.POSITIVE_INFINITY), e.value(Double.POSITIVE_INFINITY), eps);
        this.checkException(label + " (-Inf.)", Double.NEGATIVE_INFINITY, a, e);
    }

    /**
     * Check a function.
     * 
     * @param label label
     * @param x x
     * @param a actual function
     * @param e expected function
     * @param eps comparison epsilon
     */
    private void checkClampedFunction(final String label,
            final double x,
            final double eps,
            final UnivariateFunction a,
            final UnivariateFunction e) {
        this.check(label + " (Reg. data)", a.value(x), e.value(x), eps);
        this.check(label + " (0)", a.value(0.), e.value(0.), eps);
        this.check(label + " (1)", a.value(1.), e.value(1.), eps);
        this.checkException(label + " (+Inf.)", Double.POSITIVE_INFINITY, a, e);
        this.checkException(label + " (-Inf.)", Double.NEGATIVE_INFINITY, a, e);
    }

    /**
     * Check a function.
     * 
     * @param label label
     * @param x x
     * @param a actual function
     * @param e expected function
     * @param eps comparison epsilon
     */
    private void checkFunction(final String label,
            final double x,
            final double eps,
            final UnivariateFunction a,
            final UnivariateFunction e) {
        this.check(label + " (Reg. data)", a.value(x), e.value(x), eps);
        this.check(label + " (0)", a.value(0.), e.value(0.), eps);
        this.check(label + " (1)", a.value(1.), e.value(1.), eps);
        this.check(label + " (+Inf.)", a.value(Double.POSITIVE_INFINITY), e.value(Double.POSITIVE_INFINITY), eps);
        this.check(label + " (-Inf.)", a.value(Double.NEGATIVE_INFINITY), e.value(Double.NEGATIVE_INFINITY), eps);
    }

    /**
     * Check a function.
     * 
     * @param label label
     * @param x x
     * @param a actual function
     * @param e expected function
     * @param eps comparison epsilon
     */
    private void checkFunction(final String label,
            final int x,
            final double eps,
            final UnivariateFunction a,
            final UnivariateFunction e) {
        this.check(label + " (Reg. data)", a.value(x), e.value(x), eps);
        this.check(label + " (0)", a.value(0), e.value(0), eps);
        this.check(label + " (1)", a.value(1), e.value(1), eps);
    }

    /**
     * Check a function.
     * 
     * @param label label
     * @param x x
     * @param y y
     * @param a actual function
     * @param e expected function
     * @param eps comparison epsilon
     */
    private void checkFunction(final String label,
            final double x,
            final double y,
            final double eps,
            final BivariateFunction a,
            final BivariateFunction e) {
        this.check(label + " (Reg. data - Reg. data)", a.value(x, y), e.value(x, y), eps);
        this.check(label + " (Reg. data - 0)", a.value(x, 0.), e.value(x, 0.), eps);
        this.check(label + " (Reg. data - 1)", a.value(x, 1.), e.value(x, 1.), eps);
        this.check(label + " (Reg. data - +Inf.)", a.value(x, Double.POSITIVE_INFINITY),
                e.value(x, Double.POSITIVE_INFINITY), eps);
        this.check(label + " (Reg. data - -Inf.)", a.value(x, Double.NEGATIVE_INFINITY),
                e.value(x, Double.NEGATIVE_INFINITY), eps);
        this.check(label + " (0 - Reg. data)", a.value(0, y), e.value(0, y), eps);
        this.check(label + " (0 - 0)", a.value(0, 0.), e.value(0, 0.), eps);
        this.check(label + " (0 - 1)", a.value(0, 1.), e.value(0, 1.), eps);
        this.check(label + " (0 - +Inf.)", a.value(0, Double.POSITIVE_INFINITY), e.value(0, Double.POSITIVE_INFINITY),
                eps);
        this.check(label + " (0 - -Inf.)", a.value(0, Double.NEGATIVE_INFINITY), e.value(0, Double.NEGATIVE_INFINITY),
                eps);
        this.check(label + " (1 - Reg. data)", a.value(1, y), e.value(1, y), eps);
        this.check(label + " (1 - 0)", a.value(1, 0.), e.value(1, 0.), eps);
        this.check(label + " (1 - 1)", a.value(1, 1.), e.value(1, 1.), eps);
        this.check(label + " (1 - +Inf.)", a.value(1, Double.POSITIVE_INFINITY), e.value(1, Double.POSITIVE_INFINITY),
                eps);
        this.check(label + " (1 - -Inf.)", a.value(1, Double.NEGATIVE_INFINITY), e.value(1, Double.NEGATIVE_INFINITY),
                eps);
        this.check(label + " (+Inf - Reg. data)", a.value(Double.POSITIVE_INFINITY, y),
                e.value(Double.POSITIVE_INFINITY, y), eps);
        this.check(label + " (+Inf - 0)", a.value(Double.POSITIVE_INFINITY, 0.), e.value(Double.POSITIVE_INFINITY, 0.),
                eps);
        this.check(label + " (+Inf - 1)", a.value(Double.POSITIVE_INFINITY, 1.), e.value(Double.POSITIVE_INFINITY, 1.),
                eps);
        this.check(label + " (+Inf - +Inf.)", a.value(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY),
                e.value(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY), eps);
        this.check(label + " (+Inf - -Inf.)", a.value(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY),
                e.value(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY), eps);
        this.check(label + " (-Inf. - Reg. data)", a.value(Double.NEGATIVE_INFINITY, y),
                e.value(Double.NEGATIVE_INFINITY, y), eps);
        this.check(label + " (-Inf. - 0)", a.value(Double.NEGATIVE_INFINITY, 0.),
                e.value(Double.NEGATIVE_INFINITY, 0.), eps);
        this.check(label + " (-Inf. - 1)", a.value(Double.NEGATIVE_INFINITY, 1.),
                e.value(Double.NEGATIVE_INFINITY, 1.), eps);
        this.check(label + " (-Inf. - +Inf.)", a.value(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY),
                e.value(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY), eps);
        this.check(label + " (-Inf. - -Inf.)", a.value(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY),
                e.value(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY), eps);
    }

    /**
     * Check a function.
     * 
     * @param label label
     * @param x x
     * @param y y
     * @param a actual function
     * @param e expected function
     * @param eps comparison epsilon
     */
    private void checkFunction(final String label,
            final double x,
            final int yInt,
            final double eps,
            final BivariateFunction a,
            final BivariateFunction e) {
        this.check(label + " (Reg. data - Reg. data)", a.value(x, yInt), e.value(x, yInt), eps);
        this.check(label + " (Reg. data - 0)", a.value(x, 0), e.value(x, 0), eps);
        this.check(label + " (Reg. data - 1)", a.value(x, 1), e.value(x, 1), eps);
        this.check(label + " (0 - Reg. data)", a.value(0, yInt), e.value(0, yInt), eps);
        this.check(label + " (0 - 0)", a.value(0, 0), e.value(0, 0), eps);
        this.check(label + " (0 - 1)", a.value(0, 1), e.value(0, 1), eps);
        this.check(label + " (1 - Reg. data)", a.value(1, yInt), e.value(1, yInt), eps);
        this.check(label + " (1 - 0)", a.value(1, 0), e.value(1, 0), eps);
        this.check(label + " (1 - 1)", a.value(1, 1), e.value(1, 1), eps);
        this.check(label + " (+Inf - Reg. data)", a.value(Double.POSITIVE_INFINITY, yInt),
                e.value(Double.POSITIVE_INFINITY, yInt), eps);
        this.check(label + " (+Inf - 0)", a.value(Double.POSITIVE_INFINITY, 0), e.value(Double.POSITIVE_INFINITY, 0),
                eps);
        this.check(label + " (+Inf - 1)", a.value(Double.POSITIVE_INFINITY, 1), e.value(Double.POSITIVE_INFINITY, 1),
                eps);
        this.check(label + " (-Inf. - Reg. data)", a.value(Double.NEGATIVE_INFINITY, yInt),
                e.value(Double.NEGATIVE_INFINITY, yInt), eps);
        this.check(label + " (-Inf. - 0)", a.value(Double.NEGATIVE_INFINITY, 0), e.value(Double.NEGATIVE_INFINITY, 0),
                eps);
        this.check(label + " (-Inf. - 1)", a.value(Double.NEGATIVE_INFINITY, 1), e.value(Double.NEGATIVE_INFINITY, 1),
                eps);
    }

    /**
     * Check a function.
     * 
     * @param label label
     * @param x x
     * @param y y
     * @param a actual function
     * @param e expected function
     * @param eps comparison epsilon
     */
    private void checkFunction(final String label,
            final int xInt,
            final int yInt,
            final BivariateFunction a,
            final BivariateFunction e,
            final double eps) {
        this.check(label + " (Reg. data - Reg. data)", a.value(xInt, yInt), e.value(xInt, yInt), eps);
        this.check(label + " (Reg. data - 0)", a.value(xInt, 0), e.value(xInt, 0), eps);
        this.check(label + " (Reg. data - 1)", a.value(xInt, 1), e.value(xInt, 1), eps);
        this.check(label + " (0 - Reg. data)", a.value(0, yInt), e.value(0, yInt), eps);
        this.check(label + " (0 - 0)", a.value(0, 0), e.value(0, 0), eps);
        this.check(label + " (0 - 1)", a.value(0, 1), e.value(0, 1), eps);
        this.check(label + " (1 - Reg. data)", a.value(1, yInt), e.value(1, yInt), eps);
        this.check(label + " (1 - 0)", a.value(1, 0), e.value(1, 0), eps);
        this.check(label + " (1 - 1)", a.value(1, 1), e.value(1, 1), eps);
    }

    /**
     * Check a function.
     * 
     * @param label label
     * @param x x
     * @param y y
     * @param a actual function
     * @param e expected function
     * @param eps comparison epsilon
     */
    private void checkFunction(final String label,
            final long xLong,
            final long yLong,
            final BivariateFunction a,
            final BivariateFunction e,
            final double eps) {
        this.check(label + " (Reg. data - Reg. data)", a.value(xLong, yLong), e.value(xLong, yLong), eps);
        this.check(label + " (Reg. data - 0)", a.value(xLong, 0), e.value(xLong, 0), eps);
        this.check(label + " (Reg. data - 1)", a.value(xLong, 1), e.value(xLong, 1), eps);
        this.check(label + " (0 - Reg. data)", a.value(0, yLong), e.value(0, yLong), eps);
        this.check(label + " (0 - 0)", a.value(0, 0), e.value(0, 0), eps);
        this.check(label + " (0 - 1)", a.value(0, 1), e.value(0, 1), eps);
        this.check(label + " (1 - Reg. data)", a.value(1, yLong), e.value(1, yLong), eps);
        this.check(label + " (1 - 0)", a.value(1, 0), e.value(1, 0), eps);
        this.check(label + " (1 - 1)", a.value(1, 1), e.value(1, 1), eps);
    }

    /**
     * Check an exception is thrown as expected.
     * 
     * @param label label
     * @param x x
     * @param a actual function
     * @param e expected function
     */
    private void checkException(final String label,
            final double x,
            final UnivariateFunction a,
            final UnivariateFunction e) {
        Report.printToReport(label, "Exception", "Exception");
        try {
            a.value(x);
            Assert.fail();
        } catch (final ArithmeticException ex) {
            Assert.assertTrue(true);
        }
        try {
            e.value(x);
            Assert.fail();
        } catch (final ArithmeticException ex) {
            Assert.assertTrue(true);
        }
    }

    /**
     * Check an exception is thrown as expected.
     * 
     * @param label label
     * @param x x
     * @param y y
     * @param a actual function
     * @param e expected function
     */
    private void checkException(final String label,
            final double x,
            final double y,
            final BivariateFunction a,
            final BivariateFunction e) {
        Report.printToReport(label, "Exception", "Exception");
        try {
            a.value(x, y);
            Assert.fail();
        } catch (final ArithmeticException ex) {
            Assert.assertTrue(true);
        }
        try {
            e.value(x, y);
            Assert.fail();
        } catch (final ArithmeticException ex) {
            Assert.assertTrue(true);
        }
    }

    /**
     * Check a value.
     * 
     * @param label label to report
     * @param actual actual value
     * @param expected expected value
     * @param eps comparison epsilon
     */
    private void check(final String label,
            final double actual,
            final double expected,
            final double eps) {
        // Print to report
        Report.printToReport(label, expected, actual);

        // Check
        if (Double.isNaN(expected) || Double.isNaN(actual)) {
            // NaN case treatment
            Assert.assertTrue(Double.isNaN(expected) && Double.isNaN(actual));
        } else if (Double.isInfinite(expected) || Double.isInfinite(actual)) {
            // Infinite case treatment
            Assert.assertTrue(Double.isInfinite(expected) && Double.isInfinite(actual));
        } else {
            // Reg. case
            if (expected != 0) {
                Assert.assertEquals(0., (expected - actual) / expected, eps);
            } else {
                Assert.assertEquals(expected, actual, eps);
            }
        }
    }
}
