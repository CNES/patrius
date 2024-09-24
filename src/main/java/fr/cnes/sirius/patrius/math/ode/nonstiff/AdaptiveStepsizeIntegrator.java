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
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
 * VERSION:4.3:DM:DM-2099:15/05/2019: Possibilite de by-passer le critere du pas min dans l'integrateur numerique DOP853
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:469:30/11/2015:Truncate step h from first guess to maxStep in initializeStep()
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.math.ode.nonstiff;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.ode.AbstractIntegrator;
import fr.cnes.sirius.patrius.math.ode.ExpandableStatefulODE;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

//CHECKSTYLE: stop MagicNumber check
//Reason: model - Commons-Math code

/**
 * This abstract class holds the common part of all adaptive
 * stepsize integrators for Ordinary Differential Equations.
 * 
 * <p>
 * These algorithms perform integration with stepsize control, which means the user does not specify the integration
 * step but rather a tolerance on error. The error threshold is computed as
 * 
 * <pre>
 * threshold_i = absTol_i + relTol_i * max(abs(ym), abs(ym + 1))
 * </pre>
 * 
 * where absTol_i is the absolute tolerance for component i of the state vector and relTol_i is the relative tolerance
 * for the same component. The user can also use only two scalar values absTol and relTol which will be used for all
 * components.
 * </p>
 * <p>
 * If the Ordinary Differential Equations is an {@link ExpandableStatefulODE
 * extended ODE} rather than a {@link fr.cnes.sirius.patrius.math.ode.FirstOrderDifferentialEquations basic ODE}, then
 * <em>only</em> the {@link ExpandableStatefulODE#getPrimaryState() primary part} of the state vector is used for
 * stepsize control, not the complete state vector.
 * </p>
 * 
 * <p>
 * If the estimated error for ym+1 is such that
 * 
 * <pre>
 * sqrt((sum(errEst_i / threshold_i) &circ; 2) / n) &lt; 1
 * </pre>
 * 
 * (where n is the main set dimension) then the step is accepted, otherwise the step is rejected and a new attempt is
 * made with a new stepsize.
 * </p>
 * 
 * @version $Id: AdaptiveStepsizeIntegrator.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 1.2
 * 
 */
//CHECKSTYLE: stop AbstractClassName check
@SuppressWarnings({"PMD.AbstractNaming", "PMD.NullAssignment", "PMD.ConstructorCallsOverridableMethod"})
public abstract class AdaptiveStepsizeIntegrator extends AbstractIntegrator {
    // CHECKSTYLE: resume AbstractClassName check

    /** Allowed absolute scalar error. */
    protected double scalAbsoluteTolerance;

    /** Allowed relative scalar error. */
    protected double scalRelativeTolerance;

    /** Allowed absolute vectorial error. */
    protected double[] vecAbsoluteTolerance;

    /** Allowed relative vectorial error. */
    protected double[] vecRelativeTolerance;

    /** Main set dimension. */
    protected int mainSetDimension;

    /** User supplied initial step. */
    private double initialStep;

    /** Minimal step. */
    private double minStep;

    /** Maximal step. */
    private double maxStep;

    /**
     * If true, steps smaller than the minimal value are silently increased up to this value, if
     * false such small steps generate an exception
     */
    private boolean acceptSmall;

    /**
     * Build an integrator with the given stepsize bounds. The default step handler does nothing.
     * 
     * @param name name of the method
     * @param minStepIn minimal step (sign is irrelevant, regardless of integration direction,
     *        forward or backward), the last step can be smaller than this
     * @param maxStepIn maximal step (sign is irrelevant, regardless of integration direction,
     *        forward or backward), the last step can be smaller than this
     * @param scalAbsoluteToleranceIn allowed absolute error
     * @param scalRelativeToleranceIn allowed relative error
     * @param acceptSmall if true, steps smaller than the minimal value are silently increased up to
     *        this value, if false such small steps generate an exception
     */
    public AdaptiveStepsizeIntegrator(final String name, final double minStepIn,
        final double maxStepIn, final double scalAbsoluteToleranceIn,
        final double scalRelativeToleranceIn, final boolean acceptSmall) {

        super(name);
        this.setStepSizeControl(minStepIn, maxStepIn, scalAbsoluteToleranceIn, scalRelativeToleranceIn,
            acceptSmall);
        this.resetInternalState();

    }

    /**
     * Build an integrator with the given stepsize bounds. The default step handler does nothing.
     * 
     * @param name name of the method
     * @param minStepIn minimal step (sign is irrelevant, regardless of integration direction,
     *        forward or backward), the last step can be smaller than this
     * @param maxStepIn maximal step (sign is irrelevant, regardless of integration direction,
     *        forward or backward), the last step can be smaller than this
     * @param vecAbsoluteToleranceIn allowed absolute error
     * @param vecRelativeToleranceIn allowed relative error
     * @param acceptSmall if true, steps smaller than the minimal value are silently increased up to
     *        this value, if false such small steps generate an exception
     */
    public AdaptiveStepsizeIntegrator(final String name, final double minStepIn,
        final double maxStepIn, final double[] vecAbsoluteToleranceIn,
        final double[] vecRelativeToleranceIn, final boolean acceptSmall) {

        super(name);
        this.setStepSizeControl(minStepIn, maxStepIn, vecAbsoluteToleranceIn, vecRelativeToleranceIn,
            acceptSmall);
        this.resetInternalState();

    }

    /**
     * Set the adaptive step size control parameters.
     * <p>
     * A side effect of this method is to also reset the initial step so it will be automatically computed by the
     * integrator if {@link #setInitialStepSize(double) setInitialStepSize} is not called by the user.
     * </p>
     * 
     * @param minimalStep minimal step (must be positive even for backward integration), the last
     *        step can be smaller than this
     * @param maximalStep maximal step (must be positive even for backward integration)
     * @param absoluteTolerance allowed absolute error
     * @param relativeTolerance allowed relative error
     * @param acceptSmallIn if true, steps smaller than the minimal value are silently increased up
     *        to this value, if false such small steps generate an exception
     */
    public void setStepSizeControl(final double minimalStep, final double maximalStep,
                                   final double absoluteTolerance, final double relativeTolerance,
                                   final boolean acceptSmallIn) {

        this.minStep = MathLib.abs(minimalStep);
        this.maxStep = MathLib.abs(maximalStep);
        this.initialStep = -1;

        this.scalAbsoluteTolerance = absoluteTolerance;
        this.scalRelativeTolerance = relativeTolerance;
        this.vecAbsoluteTolerance = null;
        this.vecRelativeTolerance = null;

        this.acceptSmall = acceptSmallIn;
    }

    /**
     * Set the adaptive step size control parameters.
     * <p>
     * A side effect of this method is to also reset the initial step so it will be automatically computed by the
     * integrator if {@link #setInitialStepSize(double) setInitialStepSize} is not called by the user.
     * </p>
     * 
     * @param minimalStep minimal step (must be positive even for backward integration), the last
     *        step can be smaller than this
     * @param maximalStep maximal step (must be positive even for backward integration)
     * @param absoluteTolerance allowed absolute error
     * @param relativeTolerance allowed relative error
     * @param acceptSmallIn if true, steps smaller than the minimal value are silently increased up
     *        to this value, if false such small steps generate an exception
     */
    public void setStepSizeControl(final double minimalStep, final double maximalStep,
                                   final double[] absoluteTolerance, final double[] relativeTolerance,
                                   final boolean acceptSmallIn) {

        this.minStep = MathLib.abs(minimalStep);
        this.maxStep = MathLib.abs(maximalStep);
        this.initialStep = -1;

        this.scalAbsoluteTolerance = 0;
        this.scalRelativeTolerance = 0;
        this.vecAbsoluteTolerance = absoluteTolerance.clone();
        this.vecRelativeTolerance = relativeTolerance.clone();

        this.acceptSmall = acceptSmallIn;
    }

    /**
     * Set the initial step size.
     * <p>
     * This method allows the user to specify an initial positive step size instead of letting the integrator guess it
     * by itself. If this method is not called before integration is started, the initial step size will be estimated by
     * the integrator.
     * </p>
     * 
     * @param initialStepSize
     *        initial step size to use (must be positive even
     *        for backward integration ; providing a negative value or a value
     *        outside of the min/max step interval will lead the integrator to
     *        ignore the value and compute the initial step size by itself)
     */
    public void setInitialStepSize(final double initialStepSize) {
        if ((initialStepSize < this.minStep) || (initialStepSize > this.maxStep)) {
            this.initialStep = -1.0;
        } else {
            this.initialStep = initialStepSize;
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void sanityChecks(final ExpandableStatefulODE equations, final double t) {

        super.sanityChecks(equations, t);

        this.mainSetDimension = equations.getPrimaryMapper().getDimension();

        if ((this.vecAbsoluteTolerance != null) && (this.vecAbsoluteTolerance.length != this.mainSetDimension)) {
            throw new DimensionMismatchException(this.mainSetDimension, this.vecAbsoluteTolerance.length);
        }

        if ((this.vecRelativeTolerance != null) && (this.vecRelativeTolerance.length != this.mainSetDimension)) {
            throw new DimensionMismatchException(this.mainSetDimension, this.vecRelativeTolerance.length);
        }

    }

    /**
     * Initialize the integration step.
     * 
     * @param forward forward integration indicator
     * @param order order of the method
     * @param scale scaling vector for the state vector (can be shorter than state vector)
     * @param t0 start time
     * @param y0 state vector at t0
     * @param yDot0 first time derivative of y0
     * @param y1 work array for a state vector
     * @param yDot1 work array for the first time derivative of y1
     * @param t final integration time
     * @return first integration step
     * @exception MaxCountExceededException if the number of functions evaluations is exceeded
     * @exception DimensionMismatchException if arrays dimensions do not match equations settings
     * @exception NumberIsTooSmallException if the step is too small and acceptSmall is false
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    public double initializeStep(final boolean forward, final int order, final double[] scale,
                                 final double t0, final double[] y0, final double[] yDot0,
                                 final double[] y1, final double[] yDot1, final double t) {
        // CHECKSTYLE: resume CyclomaticComplexity check
        if (this.initialStep > 0) {
            // use the user provided value
            return forward ? this.initialStep : -this.initialStep;
        }

        // very rough first guess : h = 0.01 * ||y/scale|| / ||y'/scale||
        // this guess will be used to perform an Euler step
        double ratio;
        double yOnScale2 = 0;
        double yDotOnScale2 = 0;
        for (int j = 0; j < scale.length; ++j) {
            ratio = y0[j] / scale[j];
            yOnScale2 += ratio * ratio;
            ratio = yDot0[j] / scale[j];
            yDotOnScale2 += ratio * ratio;
        }

        double h = ((yOnScale2 < 1.0e-10) || (yDotOnScale2 < 1.0e-10)) ?
            1.0e-6 : (0.01 * MathLib.sqrt(yOnScale2 / yDotOnScale2));
        if (!forward) {
            h = -h;
        }

        // perform an Euler step using the preceding rough guess
        h =
            forward ? MathLib.min(h, MathLib.min(t - t0, this.maxStep)) : MathLib.max(h,
                MathLib.max(t - t0, -this.maxStep));
        for (int j = 0; j < y0.length; ++j) {
            y1[j] = y0[j] + h * yDot0[j];
        }
        this.computeDerivatives(t0 + h, y1, yDot1);

        // estimate the second derivative of the solution
        double yDDotOnScale = 0;
        for (int j = 0; j < scale.length; ++j) {
            ratio = (yDot1[j] - yDot0[j]) / scale[j];
            yDDotOnScale += ratio * ratio;
        }
        yDDotOnScale = MathLib.sqrt(yDDotOnScale) / h;

        // step size is computed such that
        // h^order * max (||y'/tol||, ||y''/tol||) = 0.01
        final double maxInv2 = MathLib.max(MathLib.sqrt(yDotOnScale2), yDDotOnScale);
        final double h1 = (maxInv2 < 1.0e-15) ?
            MathLib.max(1.0e-6, 0.001 * MathLib.abs(h)) :
            MathLib.pow(0.01 / maxInv2, 1.0 / order);
        h = MathLib.min(100.0 * MathLib.abs(h), h1);
        // avoids cancellation when computing t1 - t0
        h = MathLib.max(h, 1.0e-12 * MathLib.abs(t0));
        if (h < this.getMinStep()) {
            h = this.getMinStep();
        }
        if (h > this.getMaxStep()) {
            h = this.getMaxStep();
        }
        if (!forward) {
            h = -h;
        }

        return h;

    }

    /**
     * Filter the integration step.
     * 
     * @param h signed step
     * @param forward forward integration indicator
     * @param acceptSmallIn if true, steps smaller than the minimal value are silently increased up
     *        to this value, if false such small steps generate an exception
     * @return a bounded integration step (h if no bound is reach, or a bounded value)
     * @exception NumberIsTooSmallException if the step is too small and acceptSmall is false
     */
    protected double filterStep(final double h, final boolean forward, final boolean acceptSmallIn) {

        double filteredH = h;

        // Control the step value compared to the minimum step, if it's smaller the acceptSmallIn
        // boolean value is checked.
        if (MathLib.abs(h) < this.minStep) {
            if (acceptSmallIn || this.acceptSmall) {
                // return minimum step size or - min step size depending on integration direction
                if (forward) {
                    filteredH = this.minStep;
                } else {
                    filteredH = -this.minStep;
                }
            } else {
                // Exception : min step size reached
                throw new NumberIsTooSmallException(PatriusMessages.MINIMAL_STEPSIZE_REACHED_DURING_INTEGRATION,
                    MathLib.abs(h), this.minStep, true);
            }
        }

        if (filteredH > this.maxStep) {
            filteredH = this.maxStep;
        } else if (filteredH < -this.maxStep) {
            filteredH = -this.maxStep;
        }

        return filteredH;

    }

    /** {@inheritDoc} */
    @Override
    public abstract void integrate(ExpandableStatefulODE equations, double t);

    /** {@inheritDoc} */
    @Override
    public double getCurrentStepStart() {
        return this.stepStart;
    }

    /** Reset internal state to dummy values. */
    protected void resetInternalState() {
        this.stepStart = Double.NaN;
        this.stepSize = MathLib.sqrt(this.minStep * this.maxStep);
    }

    /**
     * Get the minimal step.
     * 
     * @return minimal step
     */
    public double getMinStep() {
        return this.minStep;
    }

    /**
     * Get the maximal step.
     * 
     * @return maximal step
     */
    public double getMaxStep() {
        return this.maxStep;
    }

    /**
     * Returns the vector of absolute tolerances.
     * @return the vector of absolute tolerances, if defined by a vector, null otherwise
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    // Reason: performance
    public double[] getVecAbsoluteTolerance() {
        return this.vecAbsoluteTolerance;
    }

    /**
     * Set the vector of absolute tolerances.
     * @param vecAbsoluteTolerance the vector of absolute tolerances to set
     */
    public void setVecAbsoluteTolerance(final double[] vecAbsoluteTolerance) {
        this.vecAbsoluteTolerance = vecAbsoluteTolerance;
    }

    /**
     * Returns the vector of relative tolerances.
     * @return the vector of relative tolerances, if defined by a vector, null otherwise
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    // Reason: performance
    public double[] getVecRelativeTolerance() {
        return this.vecRelativeTolerance;
    }

    /**
     * Set the vector of relative tolerances.
     * @param vecRelativeTolerance the vector of relative tolerances to set
     */
    public void setVecRelativeTolerance(final double[] vecRelativeTolerance) {
        this.vecRelativeTolerance = vecRelativeTolerance;
    }

    /**
     * Returns the scalar absolute tolerances.
     * @return the scalar absolute tolerances, if defined by a scalar, zero otherwise
     */
    public double getScalAbsoluteTolerance() {
        return this.scalAbsoluteTolerance;
    }

    /**
     * Returns the scalar relative tolerances.
     * @return the scalar relative tolerances, if defined by a scalar, zero otherwise
     */
    public double getScalRelativeTolerance() {
        return this.scalRelativeTolerance;
    }

    // CHECKSTYLE: resume MagicNumber check
}
