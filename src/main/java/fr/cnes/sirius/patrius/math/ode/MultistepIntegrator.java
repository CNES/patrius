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
 * VERSION:4.3:DM:DM-2099:15/05/2019:[PATRIUS] Possibilite de by-passer le critere du pas min dans l'integrateur
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.ode;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;
import fr.cnes.sirius.patrius.math.exception.NoBracketingException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.linear.Array2DRowRealMatrix;
import fr.cnes.sirius.patrius.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.ode.sampling.StepHandler;
import fr.cnes.sirius.patrius.math.ode.sampling.StepInterpolator;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class is the base class for multistep integrators for Ordinary
 * Differential Equations.
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
 * <p>
 * Rather than storing several previous steps separately, this implementation uses the Nordsieck vector with higher
 * degrees scaled derivatives all taken at the same step (y<sub>n</sub>, s<sub>1</sub>(n) and r<sub>n</sub>) where
 * r<sub>n</sub> is defined as:
 * 
 * <pre>
 * r<sub>n</sub> = [ s<sub>2</sub>(n), s<sub>3</sub>(n) ... s<sub>k</sub>(n) ]<sup>T</sup>
 * </pre>
 * 
 * (we omit the k index in the notation for clarity)
 * </p>
 * <p>
 * Multistep integrators with Nordsieck representation are highly sensitive to large step changes because when the step
 * is multiplied by factor a, the k<sup>th</sup> component of the Nordsieck vector is multiplied by a<sup>k</sup> and
 * the last components are the least accurate ones. The default max growth factor is therefore set to a quite low value:
 * 2<sup>1/order</sup>.
 * </p>
 * 
 * @see fr.cnes.sirius.patrius.math.ode.nonstiff.AdamsBashforthIntegrator
 * @see fr.cnes.sirius.patrius.math.ode.nonstiff.AdamsMoultonIntegrator
 * @version $Id: MultistepIntegrator.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.0
 */
//CHECKSTYLE: stop AbstractClassName check
@SuppressWarnings("PMD.AbstractNaming")
public abstract class MultistepIntegrator extends AdaptiveStepsizeIntegrator {
    // CHECKSTYLE: resume AbstractClassName check

    /** Default safety factor. */
    private static final double DEFAULT_SAFETY = 0.9;

    /** Default min reduction factor. */
    private static final double DEFAULT_MIN_REDUCTION = 0.2;

    /** First scaled derivative (h y'). */
    protected double[] scaled;

    /**
     * Nordsieck matrix of the higher scaled derivatives.
     * <p>
     * (h<sup>2</sup>/2 y'', h<sup>3</sup>/6 y''' ..., h<sup>k</sup>/k! y<sup>(k)</sup>)
     * </p>
     */
    protected Array2DRowRealMatrix nordsieck;

    /** Starter integrator. */
    private FirstOrderIntegrator starter;

    /** Number of steps of the multistep method (excluding the one being computed). */
    private final int nSteps;

    /** Stepsize control exponent. */
    private final double exp;

    /** Safety factor for stepsize control. */
    private double safety;

    /** Minimal reduction factor for stepsize control. */
    private double minReduction;

    /** Maximal growth factor for stepsize control. */
    private double maxGrowth;

    /**
     * Build a multistep integrator with the given stepsize bounds.
     * <p>
     * The default starter integrator is set to the {@link DormandPrince853Integrator Dormand-Prince
     * 8(5,3)} integrator with some defaults settings.
     * </p>
     * <p>
     * The default max growth factor is set to a quite low value: 2<sup>1/order</sup>.
     * </p>
     * 
     * @param name name of the method
     * @param nStepsIn number of steps of the multistep method (excluding the one being computed)
     * @param order order of the method
     * @param minStep minimal step (must be positive even for backward integration), the last step
     *        can be smaller than this
     * @param maxStep maximal step (must be positive even for backward integration)
     * @param scalAbsoluteTolerance allowed absolute error
     * @param scalRelativeTolerance allowed relative error
     * @param acceptSmall if true, steps smaller than the minimal value are silently increased up to
     *        this value, if false such small steps generate an exception
     * @exception NumberIsTooSmallException if number of steps is smaller than 2
     */
    protected MultistepIntegrator(final String name, final int nStepsIn, final int order,
        final double minStep, final double maxStep, final double scalAbsoluteTolerance,
        final double scalRelativeTolerance, final boolean acceptSmall) {

        super(name, minStep, maxStep, scalAbsoluteTolerance, scalRelativeTolerance, acceptSmall);

        if (nStepsIn < 2) {
            throw new NumberIsTooSmallException(
                PatriusMessages.INTEGRATION_METHOD_NEEDS_AT_LEAST_TWO_PREVIOUS_POINTS,
                nStepsIn, 2, true);
        }

        this.starter = new DormandPrince853Integrator(minStep, maxStep,
            scalAbsoluteTolerance,
            scalRelativeTolerance);
        this.nSteps = nStepsIn;

        this.exp = -1.0 / order;

        // set the default values of the algorithm control parameters
        this.setSafety(DEFAULT_SAFETY);
        this.setMinReduction(DEFAULT_MIN_REDUCTION);
        this.setMaxGrowth(MathLib.pow(2.0, -this.exp));

    }

    /**
     * Build a multistep integrator with the given stepsize bounds.
     * <p>
     * The default starter integrator is set to the {@link DormandPrince853Integrator Dormand-Prince
     * 8(5,3)} integrator with some defaults settings.
     * </p>
     * <p>
     * The default max growth factor is set to a quite low value: 2<sup>1/order</sup>.
     * </p>
     * 
     * @param name name of the method
     * @param nStepsIn number of steps of the multistep method (excluding the one being computed)
     * @param order order of the method
     * @param minStep minimal step (must be positive even for backward integration), the last step
     *        can be smaller than this
     * @param maxStep maximal step (must be positive even for backward integration)
     * @param vecAbsoluteTolerance allowed absolute error
     * @param vecRelativeTolerance allowed relative error
     * @param acceptSmall if true, steps smaller than the minimal value are silently increased up to
     *        this value, if false such small steps generate an exception
     */
    protected MultistepIntegrator(final String name, final int nStepsIn, final int order,
        final double minStep, final double maxStep, final double[] vecAbsoluteTolerance,
        final double[] vecRelativeTolerance, final boolean acceptSmall) {
        super(name, minStep, maxStep, vecAbsoluteTolerance, vecRelativeTolerance, acceptSmall);
        this.starter = new DormandPrince853Integrator(minStep, maxStep,
            vecAbsoluteTolerance,
            vecRelativeTolerance);
        this.nSteps = nStepsIn;

        this.exp = -1.0 / order;

        // set the default values of the algorithm control parameters
        this.setSafety(DEFAULT_SAFETY);
        this.setMinReduction(DEFAULT_MIN_REDUCTION);
        this.setMaxGrowth(MathLib.pow(2.0, -this.exp));

    }

    /**
     * Get the starter integrator.
     * 
     * @return starter integrator
     */
    public ODEIntegrator getStarterIntegrator() {
        return this.starter;
    }

    /**
     * Set the starter integrator.
     * <p>
     * The various step and event handlers for this starter integrator will be managed automatically by the multi-step
     * integrator. Any user configuration for these elements will be cleared before use.
     * </p>
     * 
     * @param starterIntegrator
     *        starter integrator
     */
    public void setStarterIntegrator(final FirstOrderIntegrator starterIntegrator) {
        this.starter = starterIntegrator;
    }

    /**
     * Start the integration.
     * <p>
     * This method computes one step using the underlying starter integrator, and initializes the Nordsieck vector at
     * step start. The starter integrator purpose is only to establish initial conditions, it does not really change
     * time by itself. The top level multistep integrator remains in charge of handling time propagation and events
     * handling as it will starts its own computation right from the beginning. In a sense, the starter integrator can
     * be seen as a dummy one and so it will never trigger any user event nor call any user step handler.
     * </p>
     * 
     * @param t0
     *        initial time
     * @param y0
     *        initial value of the state vector at t0
     * @param t
     *        target time for the integration
     *        (can be set to a value smaller than <code>t0</code> for backward integration)
     * @exception DimensionMismatchException
     *            if arrays dimension do not match equations settings
     * @exception NumberIsTooSmallException
     *            if integration step is too small
     * @exception MaxCountExceededException
     *            if the number of functions evaluations is exceeded
     * @exception NoBracketingException
     *            if the location of an event cannot be bracketed
     */
    @SuppressWarnings("PMD.EmptyCatchBlock")
    protected void start(final double t0, final double[] y0, final double t) {

        // make sure NO user event nor user step handler is triggered,
        // this is the task of the top level integrator, not the task
        // of the starter integrator
        this.starter.clearEventHandlers();
        this.starter.clearStepHandlers();

        // set up one specific step handler to extract initial Nordsieck vector
        this.starter.addStepHandler(new NordsieckInitializer(this.nSteps, y0.length));

        // start integration, expecting a InitializationCompletedMarkerException
        try {
            this.starter.integrate(new CountingDifferentialEquations(y0.length),
                t0, y0, t, new double[y0.length]);
        } catch (final InitializationCompletedMarkerException icme) {
            // this is the expected nominal interruption of the start integrator
        }

        // remove the specific step handler
        this.starter.clearStepHandlers();

    }

    /**
     * Initialize the high order scaled derivatives at step start.
     * 
     * @param h
     *        step size to use for scaling
     * @param t
     *        first steps times
     * @param y
     *        first steps states
     * @param yDot
     *        first steps derivatives
     * @return Nordieck vector at first step (h<sup>2</sup>/2 y''<sub>n</sub>,
     *         h<sup>3</sup>/6 y'''<sub>n</sub> ... h<sup>k</sup>/k! y<sup>(k)</sup><sub>n</sub>)
     */
    protected abstract Array2DRowRealMatrix initializeHighOrderDerivatives(final double h, final double[] t,
                                                                           final double[][] y,
                                                                           final double[][] yDot);

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

    /**
     * Compute step grow/shrink factor according to normalized error.
     * 
     * @param error
     *        normalized error of the current step
     * @return grow/shrink factor for next step
     */
    protected double computeStepGrowShrinkFactor(final double error) {
        return MathLib.min(this.maxGrowth, MathLib.max(this.minReduction, this.safety * MathLib.pow(error, this.exp)));
    }

    /** Transformer used to convert the first step to Nordsieck representation. */
    public interface NordsieckTransformer {
        /**
         * Initialize the high order scaled derivatives at step start.
         * 
         * @param h
         *        step size to use for scaling
         * @param t
         *        first steps times
         * @param y
         *        first steps states
         * @param yDot
         *        first steps derivatives
         * @return Nordieck vector at first step (h<sup>2</sup>/2 y''<sub>n</sub>,
         *         h<sup>3</sup>/6 y'''<sub>n</sub> ... h<sup>k</sup>/k! y<sup>(k)</sup><sub>n</sub>)
         */
        Array2DRowRealMatrix initializeHighOrderDerivatives(final double h, final double[] t,
                                                            final double[][] y,
                                                            final double[][] yDot);
    }

    /** Specialized step handler storing the first step. */
    private class NordsieckInitializer implements StepHandler {

        /** Steps counter. */
        private int count;

        /** First steps times. */
        private final double[] t;

        /** First steps states. */
        private final double[][] y;

        /** First steps derivatives. */
        private final double[][] yDot;

        /**
         * Simple constructor.
         * 
         * @param nStepsIn
         *        number of steps of the multistep method (excluding the one being computed)
         * @param nIn
         *        problem dimension
         */
        public NordsieckInitializer(final int nStepsIn, final int nIn) {
            this.count = 0;
            this.t = new double[nStepsIn];
            this.y = new double[nStepsIn][nIn];
            this.yDot = new double[nStepsIn][nIn];
        }

        /** {@inheritDoc} */
        @Override
        public void handleStep(final StepInterpolator interpolator, final boolean isLast) {

            final double prev = interpolator.getPreviousTime();
            final double curr = interpolator.getCurrentTime();

            if (this.count == 0) {
                // first step, we need to store also the beginning of the step
                interpolator.setInterpolatedTime(prev);
                this.t[0] = prev;
                System.arraycopy(interpolator.getInterpolatedState(), 0,
                    this.y[0], 0, this.y[0].length);
                System.arraycopy(interpolator.getInterpolatedDerivatives(), 0,
                    this.yDot[0], 0, this.yDot[0].length);
            }

            // store the end of the step
            ++this.count;
            interpolator.setInterpolatedTime(curr);
            this.t[this.count] = curr;
            System.arraycopy(interpolator.getInterpolatedState(), 0,
                this.y[this.count], 0, this.y[this.count].length);
            System.arraycopy(interpolator.getInterpolatedDerivatives(), 0,
                this.yDot[this.count], 0, this.yDot[this.count].length);

            if (this.count == this.t.length - 1) {

                // this was the last step we needed, we can compute the derivatives
                MultistepIntegrator.this.stepStart = this.t[0];
                MultistepIntegrator.this.stepSize = (this.t[this.t.length - 1] - this.t[0]) / (this.t.length - 1);

                // first scaled derivative
                MultistepIntegrator.this.scaled = this.yDot[0].clone();
                for (int j = 0; j < MultistepIntegrator.this.scaled.length; ++j) {
                    MultistepIntegrator.this.scaled[j] *= MultistepIntegrator.this.stepSize;
                }

                // higher order derivatives
                MultistepIntegrator.this.nordsieck =
                    MultistepIntegrator.this.initializeHighOrderDerivatives(MultistepIntegrator.this.stepSize, this.t,
                        this.y, this.yDot);

                // stop the integrator now that all needed steps have been handled
                throw new InitializationCompletedMarkerException();

            }

        }

        /** {@inheritDoc} */
        @Override
        public void init(final double t0, final double[] y0, final double time) {
            // nothing to do
        }

    }

    /** Marker exception used ONLY to stop the starter integrator after first step. */
    private static class InitializationCompletedMarkerException
        extends RuntimeException {

        /** Serializable version identifier. */
        private static final long serialVersionUID = -1914085471038046418L;

        /** Simple constructor. */
        public InitializationCompletedMarkerException() {
            super((Throwable) null);
        }

    }

    /** Wrapper for differential equations, ensuring start evaluations are counted. */
    private class CountingDifferentialEquations implements FirstOrderDifferentialEquations {

        /** Dimension of the problem. */
        private final int dimension;

        /**
         * Simple constructor.
         * 
         * @param dimensionIn
         *        dimension of the problem
         */
        public CountingDifferentialEquations(final int dimensionIn) {
            this.dimension = dimensionIn;
        }

        /** {@inheritDoc} */
        @Override
        public void computeDerivatives(final double t, final double[] y, final double[] dot) {
            MultistepIntegrator.this.computeDerivatives(t, y, dot);
        }

        /** {@inheritDoc} */
        @Override
        public int getDimension() {
            return this.dimension;
        }

    }

}
