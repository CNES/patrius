/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
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
 * 
 * @history 02/04/2012
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:400:17/03/2015: use class FastMath instead of class Math
 * VERSION::FA:458:19/10/2015: Use class FastMath instead of class Math
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.polynomials;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.integration.IterativeLegendreGaussIntegrator;
import fr.cnes.sirius.patrius.math.analysis.integration.UnivariateIntegrator;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.UtilsPatrius;
//import java.io.File;
//import java.io.FileWriter;
//import java.io.IOException;

/**
 * <p>
 * Test class for {@link FourierDecompositionEngine}
 * </p>
 * 
 * @see FourierDecompositionEngine
 * @see TrigonometricPolynomialFunction
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: FourierDecompositionEngineTest.java 17909 2017-09-11 11:57:36Z bignon $
 * 
 * @since 1.2
 * 
 */
public class FourierDecompositionEngineTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle {@link FourierDecompositionEngine} class
         * 
         * @featureDescription Test creation and usage of a {@link FourierDecompositionEngine}
         * 
         * @coveredRequirements DV-CALCUL_30, DV-CALCUL_40, DV-CALCUL_110
         */
        FOURIER_DECOMPOSITION

    }

    /** Fourier decomposition engine */
    private FourierDecompositionEngine engine;

    /** Integrator to pass to engine */
    private UnivariateIntegrator integrator;

    /** A function */
    private UnivariateFunction function1;

    /** A function */
    private UnivariateFunction function2;

    /** A function */
    private UnivariateFunction function3;

    /** Result container */
    private FourierSeriesApproximation result;

    /** period of function */
    private double period;

    /** fourth function */
    private UnivariateFunction function4;

    /** Doubles comparison */
    private static final double MAXEPS = UtilsPatrius.DOUBLE_COMPARISON_EPSILON;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#FOURIER_DECOMPOSITION}
     * 
     * @testedMethod {@link FourierDecompositionEngine#setOrder(int)}
     * @testedMethod {@link FourierDecompositionEngine#getOrder()}
     * 
     * @description Set the order of the Fourier series
     * 
     * @input decomposition order as a int
     * 
     * @output decomposition order
     * 
     * @testPassCriteria recovered order is as expected - fails when order is <= 0
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testSetOrder() {
        try {
            this.engine.setOrder(-2);
            fail();
        } catch (final IllegalArgumentException e) {
            // expected!!
        }
        try {
            this.engine.setOrder(0);
            fail();
        } catch (final IllegalArgumentException e) {
            // expected!!
        }

        this.engine.setOrder(2);
        assertEquals(2, this.engine.getOrder());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#FOURIER_DECOMPOSITION}
     * 
     * @testedMethod {@link FourierDecompositionEngine#setMaxEvals(int)}
     * @testedMethod {@link FourierDecompositionEngine#getMaxEvals()}
     * 
     * @description Set the max evaluations for the integrator
     * 
     * @input maximum evaluations as a int
     * 
     * @output maximum evaluations
     * 
     * @testPassCriteria recovered maxEval is as expected - fails when maxEvals is <= 0
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testGetMaxEvals() {
        try {
            this.engine.setMaxEvals(-2);
            fail();
        } catch (final IllegalArgumentException e) {
            // expected!!
        }
        try {
            this.engine.setMaxEvals(0);
            fail();
        } catch (final IllegalArgumentException e) {
            // expected!!
        }

        this.engine.setMaxEvals(2);
        assertEquals(2, this.engine.getMaxEvals());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#FOURIER_DECOMPOSITION}
     * 
     * @testedMethod {@link FourierDecompositionEngine#setIntegrator(UnivariateIntegrator)}
     * 
     * @description Set the integrator
     * 
     * @input integrator
     * 
     * @output nothing
     * 
     * @testPassCriteria fails if null
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testSetIntegrator() {
        try {
            this.engine.setIntegrator(null);
            fail();
        } catch (final IllegalArgumentException e) {
            // expected!!
        }

        this.engine.setMaxEvals(2);
        assertEquals(2, this.engine.getMaxEvals());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#FOURIER_DECOMPOSITION}
     * 
     * @testedMethod {@link FourierDecompositionEngine#setFunction(UnivariateFunction, double)}
     * 
     * @description Set the function and period
     * 
     * @input integrator
     * 
     * @output nothing
     * 
     * @testPassCriteria fails if function is null, if period is <= 0 and if the period isnt the functions period.
     *                   Recovered period is same as expected one.
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testSetFunction() {
        try {
            this.engine.setFunction(null, 0);
            fail();
        } catch (final IllegalArgumentException e) {
            // expected!!
        }

        try {
            this.engine.setFunction(this.function1, -2);
            fail();
        } catch (final IllegalArgumentException e) {
            // expected!!
        }

        try {
            this.engine.setFunction(this.function1, 0);
            fail();
        } catch (final IllegalArgumentException e) {
            // expected!!
        }

        this.engine.setFunction(this.function2, FastMath.PI);

        assertEquals(FastMath.PI, this.engine.getPeriod(), MAXEPS);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#FOURIER_DECOMPOSITION}
     * 
     * @testedMethod {@link FourierDecompositionEngine#decompose()}
     * 
     * @description decompose function to fourier series
     * 
     * @input nothing
     * 
     * @output nothing
     * 
     * @testPassCriteria values of decomposed function are the same as reference ones. depending on integrator
     *                   parameters, precision varies. Something close to 1e-14 is set for standard functions 1 is set
     *                   for the square function, to take into account the <a
     *                   href="http://en.wikipedia.org/wiki/Gibbs_phenomenon">Gibbs phenomenon</a>. <br>
     *                   A validation of the computed coefficients is also undertaken. Coefficients are validated to
     *                   1e-14.
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testDecompose() {
        try {
            this.engine.decompose();
            fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }

        try {
            this.engine.setFunction(this.function1, FastMath.PI * 2);
            this.engine.decompose();
            fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }

        this.engine.setOrder(15);

        this.period = 2 * FastMath.PI;
        this.engine.setFunction(this.function1, this.period);
        this.result = this.engine.decompose();
        this.checkApproximation(this.result, MAXEPS);
        this.check(this.result, MAXEPS, FType.COS);
        // writeFiles(result, "cos(x)");

        this.period = FastMath.PI;
        this.engine.setFunction(this.function2, this.period);
        this.result = this.engine.decompose();
        this.checkApproximation(this.result, MAXEPS);
        this.check(this.result, MAXEPS, FType.SIN);
        // writeFiles(result, "sin(2x)");

        this.period = FastMath.PI * 2;
        this.engine.setFunction(this.function3, this.period);
        this.result = this.engine.decompose();
        this.checkApproximation(this.result, 1e-13);
        this.check(this.result, MAXEPS, FType.COMP);
        // writeFiles(result, "cos(4x) + sin(3x)");

        this.period = 2;
        this.engine.setFunction(this.function4, this.period);
        this.result = this.engine.decompose();
        this.checkApproximation(this.result, 1);
        this.check(this.result, MAXEPS, FType.SQUARE);
        // writeFiles(result, "square wave other");

    }

    /**
     * Check the computed coefficients are the same as the theoretical ones for the square wave.<br>
     * The cosine coefficient should be null. The 2*k-1 order (k >= 1) sine coefficient should be equal to :
     * <code>4 / (pi * (2*k -1)</code> and other should be equal to 0.
     * 
     * @param approx
     *        the {@link FourierSeriesApproximation} of the square wave
     * @param eps
     *        the comparison threshold
     * @param type
     *        the type of function approximated
     * 
     * @see <a href="http://en.wikipedia.org/wiki/Square_wave#Examining_the_square_wave">Square waves approximation</a>
     */
    private void check(final FourierSeriesApproximation approx, final double eps, final FType type) {

        final double[] cos = approx.getFourier().getCosArray();
        final double[] sin = approx.getFourier().getSinArray();

        switch (type) {
            case SQUARE:
                for (final double e : cos) {
                    assertEquals(0, e, eps);
                }
                for (int index = 0; index < sin.length; index++) {
                    if ((index + 1) % 2 == 1) {
                        // (index + 1) = 2 * k - 1
                        assertEquals(4 / (FastMath.PI * (index + 1)), sin[index], eps);
                    }
                }
                assertEquals(approx.getPeriod(), 2, eps);
                break;
            case COS:
                for (final double e : sin) {
                    assertEquals(0, e, eps);
                }
                double res;
                for (int index = 0; index < cos.length; index++) {
                    if (index == 0) {
                        res = 1;
                    } else {
                        res = 0;
                    }
                    assertEquals(res, cos[index], eps);
                }
                assertEquals(approx.getPeriod(), FastMath.PI * 2, eps);
                break;
            case SIN:
                for (final double e : cos) {
                    assertEquals(0, e, eps);
                }
                for (int index = 0; index < sin.length; index++) {
                    if (index == 0) {
                        res = 1;
                    } else {
                        res = 0;
                    }
                    assertEquals(res, sin[index], eps);
                }
                assertEquals(approx.getPeriod(), FastMath.PI, eps);
                break;
            case COMP:
                for (int index = 0; index < cos.length; index++) {
                    if (index == 3) {
                        res = 1;
                    } else {
                        res = 0;
                    }
                    assertEquals(res, cos[index], eps);
                }
                for (int index = 0; index < sin.length; index++) {
                    if (index == 2) {
                        res = 1;
                    } else {
                        res = 0;
                    }
                    assertEquals(res, sin[index], eps);
                }
                assertEquals(approx.getPeriod(), 2 * FastMath.PI, eps);
                break;
            default:
                break;
        }

    }

    /**
     * Check if function and fourier seris are equal
     * 
     * @param f
     *        {@link FourierSeriesApproximation}
     * @param d
     *        threshold
     */
    private void checkApproximation(final FourierSeriesApproximation f, final double d) {
        final int numPoints = 100;
        double currentX = -f.getPeriod() / 2;
        final double rate = f.getPeriod() / numPoints;

        for (int i = 0; i < numPoints; i++) {
            assertEquals(f.getFunction().value(currentX), f.getFourier().value(currentX), d);
            currentX += rate;
        }
    }

    /**
     * Set up
     */
    @Before
    public void setUp() {
        this.integrator = new IterativeLegendreGaussIntegrator(5, 1e-14, 1e-14);
        // integrator = new SimpsonIntegrator();
        this.engine = new FourierDecompositionEngine(this.integrator);
        this.engine.setMaxEvals(Integer.MAX_VALUE);

        // test functions
        this.function1 = new UnivariateFunction(){
            @Override
            public double value(final double x) {
                return MathLib.cos(x);
            }
        };

        this.function2 = new UnivariateFunction(){
            @Override
            public double value(final double x) {
                return MathLib.sin(2 * x);
            }
        };

        this.function3 = new UnivariateFunction(){
            @Override
            public double value(final double x) {
                return 1 + MathLib.cos(4 * x) + MathLib.sin(3 * x);
            }
        };

        this.function4 = new UnivariateFunction(){
            @Override
            public double value(final double x) {
                final double local = x - MathLib.floor(x / 2) * 2;
                if (local >= 1) {
                    return -1;
                } else {
                    return 1;
                }
            }
        };
    }

    /**
     * Function types.
     */
    private enum FType {
        /**
         * Square function
         */
        SQUARE,
        /**
         * cos(x)
         */
        COS,
        /**
         * sin(2x)
         */
        SIN,
        /**
         * Composed
         */
        COMP
    }

    // /**
    // * Write data to file
    // *
    // * @param f
    // * {@link FourierSeriesApproximation}
    // * @param label
    // * name of text file to write
    // * @throws IOException
    // * if file writing fails
    // *
    // */
    // public void writeFiles(final FourierSeriesApproximation f, final String label) throws IOException {
    //
    // final FileWriter writer = new FileWriter(new File("D:\\PUBLIC\\TEMP\\" + label + ".res"));
    //
    // writer.append("x, function, approximation, error \n");
    // final String comma = ", ";
    // final double step = .001;
    // for (double i = -5; i <= 5; i += step) {
    // writer.append(i + comma);
    // writer.append(f.getFunction().value(i) + comma);
    // writer.append(f.getFourier().value(i) + comma);
    // writer.append(Math.abs(f.getFunction().value(i) - f.getFourier().value(i)) + "\n");
    // }
    //
    // writer.close();
    // }

}
