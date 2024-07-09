/**
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
 * @history 02/04/2012
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:400:17/03/2015: use class FastMath instead of class Math
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.polynomials;

import java.util.Locale;

import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.integration.UnivariateIntegrator;
import fr.cnes.sirius.patrius.math.analysis.polynomials.ElementaryMultiplicationTypes.ElementaryType;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Decompose a {@link UnivariateFunction} as a Fourier Series using {@link TrigonometricPolynomialFunction}
 * representation.
 * 
 * @useSample Given an <code>integrator</code>, the user must create an instance of this class, set the parameters and
 *            then call the <code>decompose()</code> method :<br>
 *            <code><br>
 *            FourierDecompositionEngine engine = new FourierDecompositionEngine(integrator);<br>
 *            engine.setOrder(10);<br>
 *            engine.setFunction(userFunction, userFunctionPeriod);<br>
 *            FourierSeriesApproximation approximation = engine.decompose();<br>
 *            </code><br>
 * 
 * 
 * @concurrency not thread-safe
 * @concurrency.comment not thread safe because of the setFunction method that can change the UnivariateFunction being
 *                      decomposed
 * 
 * @see TrigonometricPolynomialFunction
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: FourierDecompositionEngine.java 17603 2017-05-18 08:28:32Z bignon $
 * 
 * @since 1.2
 * 
 */
public final class FourierDecompositionEngine {

    /** Default evals */
    private static final int DEFAULT = 100;

    /** Maximum evaluations of the integrator */
    private int maxEvals = DEFAULT;

    /** Integrator */
    private UnivariateIntegrator integrator;

    /** Order of Fourier Series decomposition */
    private int order = -1;

    /** Period */
    private double t = -1;

    /** function */
    private UnivariateFunction f = null;

    /**
     * Constructor. Create a new instance of the {@link FourierDecompositionEngine} class with the specified integrator.
     * 
     * @param newIntegrator
     *        integrator to use
     */
    public FourierDecompositionEngine(final UnivariateIntegrator newIntegrator) {
        this.setIntegrator(newIntegrator);
    }

    /**
     * @return the order
     */
    public int getOrder() {
        return this.order;
    }

    /**
     * @param newOrder
     *        the order to set
     */
    public void setOrder(final int newOrder) {
        if (newOrder <= 0) {
            throw new IllegalArgumentException(
                PatriusMessages.ZERO_NOT_ALLOWED.getLocalizedString(Locale.getDefault()));
        } else {
            this.order = newOrder;
        }
    }

    /**
     * @return the period of function f
     */
    public double getPeriod() {
        return this.t;
    }

    /**
     * @return the maximum evaluations for the integrator
     */
    public int getMaxEvals() {
        return this.maxEvals;
    }

    /**
     * Set the maximum evaluations allowed for the integrator. Default is 100
     * 
     * @param maxEvaluations
     *        the maximum evaluations for the integrator
     */
    public void setMaxEvals(final int maxEvaluations) {
        if (maxEvaluations <= 0) {
            throw new IllegalArgumentException(
                PatriusMessages.ZERO_NOT_ALLOWED.getLocalizedString(Locale.getDefault()));
        } else {
            this.maxEvals = maxEvaluations;
        }
    }

    /**
     * Set the {@link UnivariateIntegrator} to use for the serires coefficient computation.
     * 
     * @param newIntegrator
     *        to use
     */
    public void setIntegrator(final UnivariateIntegrator newIntegrator) {
        if (newIntegrator == null) {
            throw new IllegalArgumentException(
                PatriusMessages.NULL_NOT_ALLOWED.getLocalizedString(Locale.getDefault()));
        } else {
            this.integrator = newIntegrator;
        }
    }

    /**
     * Set the {@link UnivariateFunction} to decompose and its period. <br>
     * <b>Warning :</b> The user should make sure the period specified is coherent with the function.
     * 
     * @param function
     *        to decompose
     * @param period
     *        period of function
     */
    public void setFunction(final UnivariateFunction function, final double period) {
        checkSanity(function, period, 1);
        this.f = function;
        this.t = period;
    }

    /**
     * Decompose function <code>f</code>, using user given period <code>t</code> and <code>integrator</code>, into a
     * Fourier Series of order <code>order</code>.
     * 
     * <b>Warning :</b> the user must make sure the given period <code>t</code> is coherent with the function
     * <code>f</code>.
     * 
     * @return A {@link FourierSeriesApproximation} instance containing the Fourier Series approximation of the user
     *         function and the latter.
     */
    public FourierSeriesApproximation decompose() {

        checkSanity(this.f, this.t, this.order);

        // get a0
        final double a0 =
            MathLib.divide(1., this.t) * this.integrator.integrate(this.maxEvals, this.f, -this.t / 2, this.t / 2);

        // get a
        final double[] a = new double[this.order];
        // get b
        final double[] b = new double[this.order];
        for (int intermediateOrder = 1; intermediateOrder <= this.order; intermediateOrder++) {

            a[intermediateOrder - 1] =
                MathLib.divide(2., this.t)
                    * this.integrator.integrate(this.maxEvals,
                        this.componentProvider(intermediateOrder, ElementaryType.COS), -this.t / 2,
                        this.t / 2);

            b[intermediateOrder - 1] =
                MathLib.divide(2., this.t)
                    * this.integrator.integrate(this.maxEvals,
                        this.componentProvider(intermediateOrder, ElementaryType.SIN), -this.t / 2,
                        this.t / 2);
        }

        final FourierSeries approx = new FourierSeries(MathLib.divide(2 * FastMath.PI, this.t), a0, a, b);
        return new FourierSeriesApproximation(this.f, approx);
    }

    /**
     * Depending on type {@link ElementaryType#COS} or {@link ElementaryType#SIN}, provides the
     * {@link UnivariateFunction} <br>
     * <code>f(x) * cos(intermediateOrder * x * 2 * pi / t)</code>
     * 
     * @param intermediateOrder
     *        order of elementary function
     * @param intermediateType
     *        type of elementary function
     * @return {@link UnivariateFunction} <br>
     *         <code>f(x) * cos(intermediateOrder * x * 2 * pi / t)</code>
     */
    private UnivariateFunction componentProvider(final int intermediateOrder, final ElementaryType intermediateType) {

        return new UnivariateFunction(){
            /** {@inheritDoc} */
            @Override
            public double value(final double x) {
                return FourierDecompositionEngine.this.f.value(x)
                    * ElementaryMultiplicationTypes.componentProvider(intermediateType, intermediateOrder,
                        FourierDecompositionEngine.this.t)
                        .value(x);
            }
        };

    }

    /**
     * Checks arguments sanity
     * 
     * @param f
     *        function
     * @param period
     *        function period
     * @param order
     *        order of decomposition
     * 
     * @since 1.2
     */
    private static void checkSanity(final UnivariateFunction f, final double period, final int order) {
        if (f == null || order <= 0 || period <= 0) {
            throw new IllegalArgumentException(PatriusMessages.ILLEGAL_STATE.getLocalizedString(Locale.getDefault()));
        }
    }

}
