/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
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
 * VERSION:4.3:DM:DM-2099:15/05/2019: Possibilite de by-passer le critere du pas min dans l'integrateur numerique DOP853
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:469:30/11/2015:Change signature of method initializeStep() called by integrate()
 * VERSION::DM:653:02/08/2016:change error estimation
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.math.ode.nonstiff;

import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.math.ode.ExpandableStatefulODE;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * This class implements the common part of all embedded Runge-Kutta
 * integrators for Ordinary Differential Equations.
 * 
 * <p>
 * These methods are embedded explicit Runge-Kutta methods with two sets of coefficients allowing to estimate the error,
 * their Butcher arrays are as follows :
 * 
 * <pre>
 *    0  |
 *   c2  | a21
 *   c3  | a31  a32
 *   ... |        ...
 *   cs  | as1  as2  ...  ass-1
 *       |--------------------------
 *       |  b1   b2  ...   bs-1  bs
 *       |  b'1  b'2 ...   b's-1 b's
 * </pre>
 * 
 * </p>
 * 
 * <p>
 * In fact, we rather use the array defined by ej = bj - b'j to compute directly the error rather than computing two
 * estimates and then comparing them.
 * </p>
 * 
 * <p>
 * Some methods are qualified as <i>fsal</i> (first same as last) methods. This means the last evaluation of the
 * derivatives in one step is the same as the first in the next step. Then, this evaluation can be reused from one step
 * to the next one and the cost of such a method is really s-1 evaluations despite the method still has s stages. This
 * behaviour is true only for successful steps, if the step is rejected after the error estimation phase, no evaluation
 * is saved. For an <i>fsal</i> method, we have cs = 1 and asi = bi for all i.
 * </p>
 * 
 * @version $Id: EmbeddedRungeKuttaIntegrator.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 1.2
 */
//CHECKSTYLE: stop AbstractClassName check
@SuppressWarnings("PMD.AbstractNaming")
public abstract class EmbeddedRungeKuttaIntegrator
    extends AdaptiveStepsizeIntegrator {
    // CHECKSTYLE: resume AbstractClassName check

    /** Default safety factor. */
    private static final double DEFAULT_SAFETY = 0.9;

    /** Default min reduction factor. */
    private static final double DEFAULT_MIN_REDUCTION = 0.2;
    
    /** Default max growth. */
    private static final double DEFAULT_MAX_GROWTH = 10.0;
    
    /** Initial error for integration. */
    private static final double INITIAL_ERROR = 10;

    /** Array of states whose error has to be estimated. */
    protected int[] estimateErrorStates;

    /** Indicator for <i>fsal</i> methods. */
    private final boolean fsal;

    /** Time steps from Butcher array (without the first zero). */
    private final double[] c;

    /** Internal weights from Butcher array (without the first empty row). */
    private final double[][] a;

    /** External weights for the high order method from Butcher array. */
    private final double[] b;

    /** Prototype of the step interpolator. */
    private final RungeKuttaStepInterpolator prototype;

    /** Stepsize control exponent. */
    private final double exp;

    /** Safety factor for stepsize control. */
    private double safety;

    /** Minimal reduction factor for stepsize control. */
    private double minReduction;

    /** Maximal growth factor for stepsize control. */
    private double maxGrowth;

    /**
     * Build a Runge-Kutta integrator with the given Butcher array.
     * 
     * @param name name of the method
     * @param fsalIn indicate that the method is an <i>fsal</i>
     * @param cIn time steps from Butcher array (without the first zero)
     * @param aIn internal weights from Butcher array (without the first empty row)
     * @param bIn propagation weights for the high order method from Butcher array
     * @param prototypeIn prototype of the step interpolator to use
     * @param minStep minimal step (sign is irrelevant, regardless of integration direction, forward
     *        or backward), the last step can be smaller than this
     * @param maxStep maximal step (sign is irrelevant, regardless of integration direction, forward
     *        or backward), the last step can be smaller than this
     * @param scalAbsoluteTolerance allowed absolute error
     * @param scalRelativeTolerance allowed relative error
     * @param acceptSmall if true, steps smaller than the minimal value are silently increased up to
     *        this value, if false such small steps generate an exception
     */
    protected EmbeddedRungeKuttaIntegrator(final String name, final boolean fsalIn,
        final double[] cIn, final double[][] aIn, final double[] bIn,
        final RungeKuttaStepInterpolator prototypeIn, final double minStep,
        final double maxStep, final double scalAbsoluteTolerance,
        final double scalRelativeTolerance, final boolean acceptSmall) {

        super(name, minStep, maxStep, scalAbsoluteTolerance, scalRelativeTolerance, acceptSmall);

        this.fsal = fsalIn;
        this.c = cIn;
        this.a = aIn;
        this.b = bIn;
        this.prototype = prototypeIn;

        this.exp = -1.0 / this.getOrder();

        // set the default values of the algorithm control parameters
        this.setSafety(DEFAULT_SAFETY);
        this.setMinReduction(DEFAULT_MIN_REDUCTION);
        this.setMaxGrowth(DEFAULT_MAX_GROWTH);

    }

    /**
     * Build a Runge-Kutta integrator with the given Butcher array.
     * 
     * @param name name of the method
     * @param fsalIn indicate that the method is an <i>fsal</i>
     * @param cIn time steps from Butcher array (without the first zero)
     * @param aIn internal weights from Butcher array (without the first empty row)
     * @param bIn propagation weights for the high order method from Butcher array
     * @param prototypeIn prototype of the step interpolator to use
     * @param minStep minimal step (must be positive even for backward integration), the last step
     *        can be smaller than this
     * @param maxStep maximal step (must be positive even for backward integration)
     * @param vecAbsoluteTolerance allowed absolute error
     * @param vecRelativeTolerance allowed relative error
     * @param acceptSmall if true, steps smaller than the minimal value are silently increased up to
     *        this value, if false such small steps generate an exception
     */
    protected EmbeddedRungeKuttaIntegrator(final String name, final boolean fsalIn,
        final double[] cIn, final double[][] aIn, final double[] bIn,
        final RungeKuttaStepInterpolator prototypeIn, final double minStep,
        final double maxStep, final double[] vecAbsoluteTolerance,
        final double[] vecRelativeTolerance, final boolean acceptSmall) {

        super(name, minStep, maxStep, vecAbsoluteTolerance, vecRelativeTolerance, acceptSmall);

        this.fsal = fsalIn;
        this.c = cIn;
        this.a = aIn;
        this.b = bIn;
        this.prototype = prototypeIn;

        this.exp = -1.0 / this.getOrder();

        // set the default values of the algorithm control parameters
        this.setSafety(DEFAULT_SAFETY);
        this.setMinReduction(DEFAULT_MIN_REDUCTION);
        this.setMaxGrowth(DEFAULT_MAX_GROWTH);
    }

    /**
     * Get the order of the method.
     * 
     * @return order of the method
     */
    public abstract int getOrder();

    /**
     * Get the safety factor for stepsize control.
     * 
     * @return safety factor
     */
    public double getSafety() {
        return this.safety;
    }

    /**
     * Set the safety factor for stepsize control.
     * 
     * @param safetyIn
     *        safety factor
     */
    public void setSafety(final double safetyIn) {
        this.safety = safetyIn;
    }

    /** {@inheritDoc} */
    // CHECKSTYLE: stop MethodLength check
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    @Override
    public void integrate(final ExpandableStatefulODE equations, final double t) {
        // CHECKSTYLE: resume MethodLength check
        // CHECKSTYLE: resume CyclomaticComplexity check

        // Sanity checks
        this.sanityChecks(equations, t);
        this.setEquations(equations);
        // get integration direction
        final boolean forward = t > equations.getTime();

        // create some internal working arrays
        final double[] y0 = equations.getCompleteState();
        final double[] y = y0.clone();
        final int stages = this.c.length + 1;
        final double[][] yDotK = new double[stages][y.length];
        final double[] yTmp = y0.clone();
        final double[] yDotTmp = new double[y.length];

        // set up an interpolator sharing the integrator arrays
        final RungeKuttaStepInterpolator interpolator = (RungeKuttaStepInterpolator) this.prototype.copy();
        interpolator.reinitialize(this, yTmp, yDotK, forward,
            equations.getPrimaryMapper(), equations.getSecondaryMappers());
        interpolator.storeTime(equations.getTime());

        // set up integration control objects
        this.stepStart = equations.getTime();
        double hNew = 0;
        boolean firstTime = true;
        this.initIntegration(equations.getTime(), y0, t);

        // main integration loop
        this.isLastStep = false;
        do {

            interpolator.shift();

            // iterate over step size, ensuring local normalized error is smaller than 1
            double error = INITIAL_ERROR;
            while (error >= 1.0) {

                if (firstTime || !this.fsal) {
                    // first stage
                    this.computeDerivatives(this.stepStart, y, yDotK[0]);
                }

                if (firstTime) {
                    // first stage
                    final double[] scale = new double[this.mainSetDimension];
                    if (this.vecAbsoluteTolerance == null) {
                        // initialize scale from scalar absolute and relative tolerances
                        for (int i = 0; i < scale.length; ++i) {
                            scale[i] = this.scalAbsoluteTolerance + this.scalRelativeTolerance * MathLib.abs(y[i]);
                        }
                    } else {
                        // initialize scale from vectorial absolute and relative tolerances
                        for (int i = 0; i < scale.length; ++i) {
                            scale[i] = this.vecAbsoluteTolerance[i] + this.vecRelativeTolerance[i] * MathLib.abs(y[i]);
                        }
                    }
                    hNew = this.initializeStep(forward, this.getOrder(), scale,
                        this.stepStart, y, yDotK[0], yTmp, yDotK[1], t);
                    firstTime = false;
                }

                this.stepSize = hNew;
                if (forward) {
                    if (this.stepStart + this.stepSize > t) {
                        this.stepSize = t - this.stepStart;
                        // Ensure that stepStart + stepSize is equal to t
                        this.stepSize = avoidOvershoot(this.stepStart, t, this.stepSize, forward);
                    }
                } else {
                    if (this.stepStart + this.stepSize < t) {
                        this.stepSize = t - this.stepStart;
                        // Ensure that stepStart + stepSize is equal to t
                        this.stepSize = avoidOvershoot(this.stepStart, t, this.stepSize, forward);
                    }
                }

                // next stages
                for (int k = 1; k < stages; ++k) {

                    for (int j = 0; j < y0.length; ++j) {
                        double sum = this.a[k - 1][0] * yDotK[0][j];
                        for (int l = 1; l < k; ++l) {
                            sum += this.a[k - 1][l] * yDotK[l][j];
                        }
                        yTmp[j] = y[j] + this.stepSize * sum;
                    }

                    this.computeDerivatives(this.stepStart + this.c[k - 1] * this.stepSize, yTmp, yDotK[k]);
                }

                // estimate the state at the end of the step
                for (int j = 0; j < y0.length; ++j) {
                    double sum = this.b[0] * yDotK[0][j];
                    for (int l = 1; l < stages; ++l) {
                        sum += this.b[l] * yDotK[l][j];
                    }
                    yTmp[j] = y[j] + this.stepSize * sum;
                }

                // estimate the error at the end of the step
                error = this.estimateError(yDotK, y, yTmp, this.stepSize);
                if (error >= 1.0) {
                    // reject the step and attempt to reduce error by stepsize control
                    final double factor =
                        MathLib.min(this.maxGrowth,
                            MathLib.max(this.minReduction, this.safety * MathLib.pow(error, this.exp)));
                    hNew = this.filterStep(this.stepSize * factor, forward, false);
                    if (MathLib.abs(hNew) <= this.getMinStep()) {
                        error = 0.;
                    }
                }
            }

            // local error is small enough: accept the step, trigger events and step handlers
            interpolator.storeTime(this.stepStart + this.stepSize);
            System.arraycopy(yTmp, 0, y, 0, y0.length);
            System.arraycopy(yDotK[stages - 1], 0, yDotTmp, 0, y0.length);
            this.stepStart = this.acceptStep(interpolator, y, yDotTmp, t);
            System.arraycopy(y, 0, yTmp, 0, y.length);

            if (!this.isLastStep) {

                // prepare next step
                interpolator.storeTime(this.stepStart);

                if (this.fsal) {
                    // save the last evaluation for the next step
                    System.arraycopy(yDotTmp, 0, yDotK[0], 0, y0.length);
                }

                // stepsize control for next step
                final double factor =
                    MathLib.min(this.maxGrowth,
                        MathLib.max(this.minReduction, this.safety * MathLib.pow(error, this.exp)));
                final double scaledH = this.stepSize * factor;
                final double nextT = this.stepStart + scaledH;
                final boolean nextIsLast = forward ? (nextT > t) : (nextT < t);
                hNew = this.filterStep(scaledH, forward, nextIsLast);

                final double filteredNextT = this.stepStart + hNew;
                final boolean filteredNextIsLast = forward ? (filteredNextT > t) : (filteredNextT < t);
                if (filteredNextIsLast) {
                    hNew = t - this.stepStart;
                    // Ensure that stepStart + hNew is equal to t
                    hNew = avoidOvershoot(this.stepStart, t, hNew, forward);
                }
            }

        } while (!this.isLastStep);

        // dispatch results
        equations.setTime(this.stepStart);
        equations.setCompleteState(y);

        this.resetInternalState();
    }

    /**
     * Get the minimal reduction factor for stepsize control.
     * 
     * @return minimal reduction factor
     */
    public double getMinReduction() {
        return this.minReduction;
    }

    /**
     * Set the minimal reduction factor for stepsize control.
     * 
     * @param minReductionIn
     *        minimal reduction factor
     */
    public void setMinReduction(final double minReductionIn) {
        this.minReduction = minReductionIn;
    }

    /**
     * Get the maximal growth factor for stepsize control.
     * 
     * @return maximal growth factor
     */
    public double getMaxGrowth() {
        return this.maxGrowth;
    }

    /**
     * Set the maximal growth factor for stepsize control.
     * 
     * @param maxGrowthIn
     *        maximal growth factor
     */
    public void setMaxGrowth(final double maxGrowthIn) {
        this.maxGrowth = maxGrowthIn;
    }

    /**
     * Compute the error ratio.
     * 
     * @param yDotK
     *        derivatives computed during the first stages
     * @param y0
     *        estimate of the step at the start of the step
     * @param y1
     *        estimate of the step at the end of the step
     * @param h
     *        current step
     * @return error ratio, greater than 1 if step should be rejected
     */
    protected abstract double estimateError(double[][] yDotK,
                                            double[] y0, double[] y1,
                                            double h);

    /** {@inheritDoc} */
    @Override
    protected void initIntegration(final double t0, final double[] y0, final double t) {
        super.initIntegration(t0, y0, t);

        // Prepare array of state whose error should be computed
        // Error should not be computed if abs tol = +inf and rel tol = 0
        final List<Integer> estimateErrorList = new ArrayList<>();
        for (int i = 0; i < this.mainSetDimension; i++) {
            final boolean computed = this.vecAbsoluteTolerance == null ||
                !(this.vecAbsoluteTolerance[i] == Double.POSITIVE_INFINITY && this.vecRelativeTolerance[i] == 0);
            if (computed) {
                // add state to list of states whose error has to be estimated
                estimateErrorList.add(i);
            }
        }
        // initialize array of states whose error has to be estimated
        this.estimateErrorStates = new int[estimateErrorList.size()];
        for (int i = 0; i < this.estimateErrorStates.length; i++) {
            this.estimateErrorStates[i] = estimateErrorList.get(i);
        }
    }
}
