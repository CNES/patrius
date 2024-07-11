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
 * VERSION:4.3:DM:DM-2099:15/05/2019: Possibilite de by-passer critere pas min dans l'integrateur numerique DOP853
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.ode.nonstiff;

import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.linear.Array2DRowRealMatrix;
import fr.cnes.sirius.patrius.math.ode.ExpandableStatefulODE;
import fr.cnes.sirius.patrius.math.ode.sampling.NordsieckStepInterpolator;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * This class implements explicit Adams-Bashforth integrators for Ordinary
 * Differential Equations.
 * 
 * <p>
 * Adams-Bashforth methods (in fact due to Adams alone) are explicit multistep ODE solvers. This implementation is a
 * variation of the classical one: it uses adaptive stepsize to implement error control, whereas classical
 * implementations are fixed step size. The value of state vector at step n+1 is a simple combination of the value at
 * step n and of the derivatives at steps n, n-1, n-2 ... Depending on the number k of previous steps one wants to use
 * for computing the next value, different formulas are available:
 * </p>
 * <ul>
 * <li>k = 1: y<sub>n+1</sub> = y<sub>n</sub> + h y'<sub>n</sub></li>
 * <li>k = 2: y<sub>n+1</sub> = y<sub>n</sub> + h (3y'<sub>n</sub>-y'<sub>n-1</sub>)/2</li>
 * <li>k = 3: y<sub>n+1</sub> = y<sub>n</sub> + h (23y'<sub>n</sub>-16y'<sub>n-1</sub>+5y'<sub>n-2</sub>)/12</li>
 * <li>k = 4: y<sub>n+1</sub> = y<sub>n</sub> + h
 * (55y'<sub>n</sub>-59y'<sub>n-1</sub>+37y'<sub>n-2</sub>-9y'<sub>n-3</sub>)/24</li>
 * <li>...</li>
 * </ul>
 * 
 * <p>
 * A k-steps Adams-Bashforth method is of order k.
 * </p>
 * 
 * <h3>Implementation details</h3>
 * 
 * <p>
 * We define scaled derivatives s<sub>i</sub>(n) at step n as:
 * 
 * <pre>
 * s<sub>1</sub>(n) = h y'<sub>n</sub> for first derivative
 * s<sub>2</sub>(n) = h<sup>2</sup>/2 y''<sub>n</sub> for second derivative
 * s<sub>3</sub>(n) = h<sup>3</sup>/6 y'''<sub>n</sub> for third derivative
 * ...
 * s<sub>k</sub>(n) = h<sup>k</sup>/k! y<sup>(k)</sup><sub>n</sub> for k<sup>th</sup> derivative
 * </pre>
 * 
 * </p>
 * 
 * <p>
 * The definitions above use the classical representation with several previous first derivatives. Lets define
 * 
 * <pre>
 *   q<sub>n</sub> = [ s<sub>1</sub>(n-1) s<sub>1</sub>(n-2) ... s<sub>1</sub>(n-(k-1)) ]<sup>T</sup>
 * </pre>
 * 
 * (we omit the k index in the notation for clarity). With these definitions, Adams-Bashforth methods can be written:
 * <ul>
 * <li>k = 1: y<sub>n+1</sub> = y<sub>n</sub> + s<sub>1</sub>(n)</li>
 * <li>k = 2: y<sub>n+1</sub> = y<sub>n</sub> + 3/2 s<sub>1</sub>(n) + [ -1/2 ] q<sub>n</sub></li>
 * <li>k = 3: y<sub>n+1</sub> = y<sub>n</sub> + 23/12 s<sub>1</sub>(n) + [ -16/12 5/12 ] q<sub>n</sub></li>
 * <li>k = 4: y<sub>n+1</sub> = y<sub>n</sub> + 55/24 s<sub>1</sub>(n) + [ -59/24 37/24 -9/24 ] q<sub>n</sub></li>
 * <li>...</li>
 * </ul>
 * </p>
 * 
 * <p>
 * Instead of using the classical representation with first derivatives only (y<sub>n</sub>, s<sub>1</sub>(n) and
 * q<sub>n</sub>), our implementation uses the Nordsieck vector with higher degrees scaled derivatives all taken at the
 * same step (y<sub>n</sub>, s<sub>1</sub>(n) and r<sub>n</sub>) where r<sub>n</sub> is defined as:
 * 
 * <pre>
 * r<sub>n</sub> = [ s<sub>2</sub>(n), s<sub>3</sub>(n) ... s<sub>k</sub>(n) ]<sup>T</sup>
 * </pre>
 * 
 * (here again we omit the k index in the notation for clarity)
 * </p>
 * 
 * <p>
 * Taylor series formulas show that for any index offset i, s<sub>1</sub>(n-i) can be computed from s<sub>1</sub>(n),
 * s<sub>2</sub>(n) ... s<sub>k</sub>(n), the formula being exact for degree k polynomials.
 * 
 * <pre>
 * s<sub>1</sub>(n-i) = s<sub>1</sub>(n) + &sum;<sub>j</sub> j (-i)<sup>j-1</sup> s<sub>j</sub>(n)
 * </pre>
 * 
 * The previous formula can be used with several values for i to compute the transform between classical representation
 * and Nordsieck vector. The transform between r<sub>n</sub> and q<sub>n</sub> resulting from the Taylor series formulas
 * above is:
 * 
 * <pre>
 * q<sub>n</sub> = s<sub>1</sub>(n) u + P r<sub>n</sub>
 * </pre>
 * 
 * where u is the [ 1 1 ... 1 ]<sup>T</sup> vector and P is the (k-1)&times;(k-1) matrix built with the j
 * (-i)<sup>j-1</sup> terms:
 * 
 * <pre>
 *        [  -2   3   -4    5  ... ]
 *        [  -4  12  -32   80  ... ]
 *   P =  [  -6  27 -108  405  ... ]
 *        [  -8  48 -256 1280  ... ]
 *        [          ...           ]
 * </pre>
 * 
 * </p>
 * 
 * <p>
 * Using the Nordsieck vector has several advantages:
 * <ul>
 * <li>it greatly simplifies step interpolation as the interpolator mainly applies Taylor series formulas,</li>
 * <li>it simplifies step changes that occur when discrete events that truncate the step are triggered,</li>
 * <li>it allows to extend the methods in order to support adaptive stepsize.</li>
 * </ul>
 * </p>
 * 
 * <p>
 * The Nordsieck vector at step n+1 is computed from the Nordsieck vector at step n as follows:
 * <ul>
 * <li>y<sub>n+1</sub> = y<sub>n</sub> + s<sub>1</sub>(n) + u<sup>T</sup> r<sub>n</sub></li>
 * <li>s<sub>1</sub>(n+1) = h f(t<sub>n+1</sub>, y<sub>n+1</sub>)</li>
 * <li>r<sub>n+1</sub> = (s<sub>1</sub>(n) - s<sub>1</sub>(n+1)) P<sup>-1</sup> u+P<sup>-1</sup> A P r<sub>n</sub></li>
 * </ul>
 * where A is a rows shifting matrix (the lower left part is an identity matrix):
 * 
 * <pre>
 *        [ 0 0   ...  0 0 | 0 ]
 *        [ ---------------+---]
 *        [ 1 0   ...  0 0 | 0 ]
 *    A = [ 0 1   ...  0 0 | 0 ]
 *        [       ...      | 0 ]
 *        [ 0 0   ...  1 0 | 0 ]
 *        [ 0 0   ...  0 1 | 0 ]
 * </pre>
 * 
 * </p>
 * 
 * <p>
 * The P<sup>-1</sup>u vector and the P<sup>-1</sup> A P matrix do not depend on the state, they only depend on k and
 * therefore are precomputed once for all.
 * </p>
 * 
 * @version $Id: AdamsBashforthIntegrator.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.0
 */
public class AdamsBashforthIntegrator extends AdamsIntegrator {

    /** Integrator method name. */
    private static final String METHOD_NAME = "Adams-Bashforth";
    
    /** Initial error for integration */
    private static final double ERROR = 10;

    /**
     * Build an Adams-Bashforth integrator with the given order and step control parameters.
     * 
     * @param nSteps number of steps of the method excluding the one being computed
     * @param minStep minimal step (sign is irrelevant, regardless of integration direction, forward
     *        or backward), the last step can be smaller than this
     * @param maxStep maximal step (sign is irrelevant, regardless of integration direction, forward
     *        or backward), the last step can be smaller than this
     * @param scalAbsoluteTolerance allowed absolute error
     * @param scalRelativeTolerance allowed relative error
     * @exception NumberIsTooSmallException if order is 1 or less
     */
    public AdamsBashforthIntegrator(final int nSteps, final double minStep, final double maxStep,
        final double scalAbsoluteTolerance, final double scalRelativeTolerance) {
        this(nSteps, minStep, maxStep, scalAbsoluteTolerance, scalRelativeTolerance, false);
    }

    /**
     * Build an Adams-Bashforth integrator with the given order and step control parameters.
     * 
     * @param nSteps number of steps of the method excluding the one being computed
     * @param minStep minimal step (sign is irrelevant, regardless of integration direction, forward
     *        or backward), the last step can be smaller than this
     * @param maxStep maximal step (sign is irrelevant, regardless of integration direction, forward
     *        or backward), the last step can be smaller than this
     * @param vecAbsoluteTolerance allowed absolute error
     * @param vecRelativeTolerance allowed relative error
     * @exception IllegalArgumentException if order is 1 or less
     */
    public AdamsBashforthIntegrator(final int nSteps, final double minStep, final double maxStep,
        final double[] vecAbsoluteTolerance, final double[] vecRelativeTolerance) {
        this(nSteps, minStep, maxStep, vecAbsoluteTolerance, vecRelativeTolerance, false);
    }

    /**
     * Build an Adams-Bashforth integrator with the given order and step control parameters.
     * 
     * @param nSteps number of steps of the method excluding the one being computed
     * @param minStep minimal step (sign is irrelevant, regardless of integration direction, forward
     *        or backward), the last step can be smaller than this
     * @param maxStep maximal step (sign is irrelevant, regardless of integration direction, forward
     *        or backward), the last step can be smaller than this
     * @param scalAbsoluteTolerance allowed absolute error
     * @param scalRelativeTolerance allowed relative error
     * @param acceptSmall if true, steps smaller than the minimal value are silently increased up to
     *        this value, if false such small steps generate an exception
     * @exception NumberIsTooSmallException if order is 1 or less
     */
    public AdamsBashforthIntegrator(final int nSteps, final double minStep, final double maxStep,
        final double scalAbsoluteTolerance, final double scalRelativeTolerance,
        final boolean acceptSmall) {
        super(METHOD_NAME, nSteps, nSteps, minStep, maxStep, scalAbsoluteTolerance,
            scalRelativeTolerance, acceptSmall);
    }

    /**
     * Build an Adams-Bashforth integrator with the given order and step control parameters.
     * 
     * @param nSteps number of steps of the method excluding the one being computed
     * @param minStep minimal step (sign is irrelevant, regardless of integration direction, forward
     *        or backward), the last step can be smaller than this
     * @param maxStep maximal step (sign is irrelevant, regardless of integration direction, forward
     *        or backward), the last step can be smaller than this
     * @param vecAbsoluteTolerance allowed absolute error
     * @param vecRelativeTolerance allowed relative error
     * @param acceptSmall if true, steps smaller than the minimal value are silently increased up to
     *        this value, if false such small steps generate an exception
     * @exception IllegalArgumentException if order is 1 or less
     */
    public AdamsBashforthIntegrator(final int nSteps, final double minStep, final double maxStep,
        final double[] vecAbsoluteTolerance, final double[] vecRelativeTolerance,
        final boolean acceptSmall) {
        super(METHOD_NAME, nSteps, nSteps, minStep, maxStep, vecAbsoluteTolerance,
            vecRelativeTolerance, acceptSmall);
    }

    /** {@inheritDoc} */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    @Override
    public void integrate(final ExpandableStatefulODE equations, final double t) {
        // CHECKSTYLE: resume CyclomaticComplexity check
        this.sanityChecks(equations, t);
        this.setEquations(equations);
        final boolean forward = t > equations.getTime();

        // initialize working arrays
        final double[] y0 = equations.getCompleteState();
        final double[] y = y0.clone();
        final double[] yDot = new double[y.length];

        // set up an interpolator sharing the integrator arrays
        final NordsieckStepInterpolator interpolator = new NordsieckStepInterpolator();
        interpolator.reinitialize(y, forward, equations.getPrimaryMapper(), equations.getSecondaryMappers());

        // set up integration control objects
        this.initIntegration(equations.getTime(), y0, t);

        // compute the initial Nordsieck vector using the configured starter integrator
        this.start(equations.getTime(), y, t);
        interpolator.reinitialize(this.stepStart, this.stepSize, this.scaled, this.nordsieck);
        interpolator.storeTime(this.stepStart);
        final int lastRow = this.nordsieck.getRowDimension() - 1;

        // reuse the step that was chosen by the starter integrator
        double hNew = this.stepSize;
        interpolator.rescale(hNew);

        // main integration loop
        this.isLastStep = false;
        do {

            double error = ERROR;
            while (error >= 1.0) {

                this.stepSize = hNew;

                // evaluate error using the last term of the Taylor expansion
                error = 0;
                for (int i = 0; i < this.mainSetDimension; ++i) {
                    final double yScale = MathLib.abs(y[i]);
                    final double tol = (this.vecAbsoluteTolerance == null) ?
                        (this.scalAbsoluteTolerance + this.scalRelativeTolerance * yScale) :
                        (this.vecAbsoluteTolerance[i] + this.vecRelativeTolerance[i] * yScale);
                    final double ratio = this.nordsieck.getEntry(lastRow, i) / tol;
                    error += ratio * ratio;
                }
                error = MathLib.sqrt(error / this.mainSetDimension);

                if (error >= 1.0) {
                    // reject the step and attempt to reduce error by stepsize control
                    final double factor = this.computeStepGrowShrinkFactor(error);
                    hNew = this.filterStep(this.stepSize * factor, forward, false);
                    if (MathLib.abs(hNew) <= this.getMinStep()) {
                        error = 0.;
                    }
                    interpolator.rescale(hNew);

                }
            }

            // predict a first estimate of the state at step end
            final double stepEnd = this.stepStart + this.stepSize;
            interpolator.shift();
            interpolator.setInterpolatedTime(stepEnd);
            System.arraycopy(interpolator.getInterpolatedState(), 0, y, 0, y0.length);

            // evaluate the derivative
            this.computeDerivatives(stepEnd, y, yDot);

            // update Nordsieck vector
            final double[] predictedScaled = new double[y0.length];
            for (int j = 0; j < y0.length; ++j) {
                predictedScaled[j] = this.stepSize * yDot[j];
            }
            final Array2DRowRealMatrix nordsieckTmp = this.updateHighOrderDerivativesPhase1(this.nordsieck);
            this.updateHighOrderDerivativesPhase2(this.scaled, predictedScaled, nordsieckTmp);
            interpolator.reinitialize(stepEnd, this.stepSize, predictedScaled, nordsieckTmp);

            // discrete events handling
            interpolator.storeTime(stepEnd);
            this.stepStart = this.acceptStep(interpolator, y, yDot, t);
            this.scaled = predictedScaled;
            this.nordsieck = nordsieckTmp;
            interpolator.reinitialize(stepEnd, this.stepSize, this.scaled, this.nordsieck);

            if (!this.isLastStep) {

                // prepare next step
                interpolator.storeTime(this.stepStart);

                if (this.resetOccurred) {
                    // some events handler has triggered changes that
                    // invalidate the derivatives, we need to restart from scratch
                    this.start(this.stepStart, y, t);
                    interpolator.reinitialize(this.stepStart, this.stepSize, this.scaled, this.nordsieck);
                }

                // stepsize control for next step
                final double factor = this.computeStepGrowShrinkFactor(error);
                final double scaledH = this.stepSize * factor;
                final double nextT = this.stepStart + scaledH;
                final boolean nextIsLast = forward ? (nextT >= t) : (nextT <= t);
                hNew = this.filterStep(scaledH, forward, nextIsLast);

                final double filteredNextT = this.stepStart + hNew;
                final boolean filteredNextIsLast = forward ? (filteredNextT >= t) : (filteredNextT <= t);
                if (filteredNextIsLast) {
                    hNew = t - this.stepStart;
                }

                interpolator.rescale(hNew);

            }

        } while (!this.isLastStep);

        // dispatch results
        equations.setTime(this.stepStart);
        equations.setCompleteState(y);

        this.resetInternalState();

    }

}
