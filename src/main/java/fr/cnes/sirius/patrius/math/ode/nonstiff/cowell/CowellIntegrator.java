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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.6:DM:DM-2571:27/01/2021:[PATRIUS] Integrateur Stormer-Cowell 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.ode.nonstiff.cowell;

import fr.cnes.sirius.patrius.math.ode.AbstractIntegrator;
import fr.cnes.sirius.patrius.math.ode.ExpandableStatefulODE;
import fr.cnes.sirius.patrius.math.ode.FirstOrderDifferentialEquations;
import fr.cnes.sirius.patrius.math.ode.SecondOrderDifferentialEquations;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;

/**
 * This class implements a 2nd order Cowell integrator.
 * <p>
 * The algorithms used in this code are explained in "A Variable-Step Double-Integration Multi-Step Integrator" by
 * Matthew M. Berry. PhD Dissertation, Virginia Polytechnic Institute and State University, Blacksburg, VA. April 2004.
 * </p>
 * <p>
 * This integrator is suited only for cartesian orbit integration (including partial derivatives such as mass equation
 * and state transition matrix).
 * </p>
 * <p>
 * This integrator requires a {@link SecondOrderStateMapper} in order to perform first order state from/to second order
 * state conversion. This conversion is necessary as the integrator only deals with second order state while other
 * PATRIUS features such as interpolation, event detection and any other {@link AbstractIntegrator}-related features
 * deals only with first order state vectors. This mapper is set through method
 * {@link #setMapper(SecondOrderStateMapper)}.
 * </p>
 * <p>
 * For best performances, the integrator parameters should be:
 * <ul>
 * <li>Order: 9</li>
 * <li>Tolerances: between 1E-10 and 1E-12</li>
 * </ul>
 * </p>
 *
 * @author Emmanuel Bignon
 *
 * @since 4.6
 * 
 * @see SecondOrderStateMapper
 */
public class CowellIntegrator extends AbstractIntegrator {

    /** Maximum order for standard formulas. */
    public static final int MAX_STANDARD_ORDER = 9;

    /** Maximum order. */
    private static final int MAX_ORDER = 20;

    /** Maximum number of allowed failed iterations. */
    private static final double MAX_ITERATIONS_FAILED = 10;

    /** Integrator coefficients &lambda; up to order 9. */
    private static final double[] LAMBDA9 = { 0., 1., 0., 1. / 12, 1. / 12, 19. / 240, 3. / 40, 863. / 12096,
        275. / 4032, 33953. / 518400, 8183. / 129600 };

    /** Integrator coefficients &gamma;* for double integration up to order 9. */
    private static final double[] GAMMASTARD9 = { 0, -1., 0.08333333333333333, 0.0, -0.004166666666666666,
        -0.004166666666666666, -0.003654100529100521, -0.003141534391534404, -0.002708608906525564,
        -0.002355324074074072 };

    /** Step interpolator. */
    private final CowellInterpolator interpolator;

    /** Second order / first order state mapper. */
    private SecondOrderStateMapper mapper;
    
    /** Integrator order. */
    private final int order;

    /** Absolute tolerance. */
    private final double absTol;

    /** Relative tolerance. */
    private final double relTol;

    /** Maximum tolerance. */
    private final double eps;

    /** Current integration support points. */
    private Support support;

    /** Previous state to current state. */
    private State previousState;

    /** Current state. */
    private State currentState;

    /** Number of failed iterations during the integration process. */
    private int iterationsFailed;

    /** Integrator coefficients &gamma;* for simple integration. */
    private double[] gammastars;

    /** Integration direction. */
    private boolean forward;

    /**
     * Constructor.
     * @param order integrator order (<= 20)
     * @param absTol absolute tolerance
     * @param relTol relative tolerance
     */
    public CowellIntegrator(final int order,
            final double absTol,
            final double relTol) {
        super();
        this.order = order;

        // Tolerances
        this.eps = MathLib.max(relTol, absTol);
        this.absTol = absTol / this.eps;
        this.relTol = relTol / this.eps;

        // Precompute coefficients c / gamma / gamma*
        precomputeCoefficients();

        // Class variables
        this.support = new Support(order);
        this.previousState = new State(0, new double[3], new double[3]);

        // Interpolator
        this.interpolator = new CowellInterpolator(this);

        // Checks
        if (order > MAX_ORDER) {
            // Order is limited in order to limit numerical quality issues
            throw new PatriusRuntimeException(PatriusMessages.COWELL_ORDER, null);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Warning:
     * <ul>
     * <li>Equations should both implements {@link FirstOrderDifferentialEquations} and
     * {@link SecondOrderDifferentialEquations}.</li>
     * <li>This integrator is suitable only for cartesian orbit integration and state vector should hence be of the
     * form: [x y z vx vy vz...] (... being for additional states).</li>
     * </ul>
     * </p>
     */
    @Override
    public void integrate(final ExpandableStatefulODE equations,
            final double t) {

        if (!(equations.getPrimary() instanceof SecondOrderDifferentialEquations)) {
            // Not available: equations must be of second order as well
            throw new PatriusRuntimeException(PatriusMessages.NOT_SECOND_ORDER_EQUATIONS, null);
        }
        final SecondOrderDifferentialEquations equations2 = (SecondOrderDifferentialEquations) equations.getPrimary();

        // Sanity check
        sanityChecks(equations, t);
        this.setEquations(equations);
        // Get integration direction
        this.forward = t > equations.getTime();

        // Create some internal working arrays
        final double[] y0 = equations.getCompleteState();
        double[] y = y0.clone();

        // Set up an interpolator sharing the integrator arrays
        this.interpolator.reinitialize(y, this.forward, equations.getPrimaryMapper(), equations.getSecondaryMappers());
        this.interpolator.storeTime(equations.getTime());

        // set up integration control objects
        this.stepStart = equations.getTime();
        this.initIntegration(equations.getTime(), y0, t);

        // Initialize initial state, split pos/vel parts
        double[] pos0 = this.mapper.extractY(y);
        double[] vel0 = this.mapper.extractYDot(y);
        final double[] pos = new double[pos0.length];
        final double[] vel = new double[vel0.length];

        // Initialization of current time to force Cowell initialization
        double tNext = this.stepStart;
        this.resetOccurred = true;
        this.isLastStep = false;

        // Main integration loop
        do {
            // PV part
            if (this.resetOccurred) {
                // Initialization
                tNext = initialize(equations2, tNext, pos0, vel0, t, pos, vel);
                this.resetOccurred = false;
            } else {
                // Regular step
                final double t0 = tNext;
                tNext = integrateOneStep(equations2, tNext, pos0, vel0, t, pos, vel);
                if (Double.isNaN(tNext)) {
                    // Re-initialization required: re-start step
                    tNext = t0;
                    this.resetOccurred = true;
                    continue;
                }
            }

            // Shift one step forward
            this.interpolator.shift();

            // Discrete events handling - May require a reset
            this.interpolator.storeTime(tNext);
            y = this.mapper.buildFullState(pos, vel);
            this.stepStart = this.acceptStep(this.interpolator, y, new double[y.length], t);

            if (!this.isLastStep) {
                // Prepare next step
                this.interpolator.storeTime(this.stepStart);

                if (this.resetOccurred) {
                    // Reset state: set state to reseted time and state and call initialization 
                    tNext = this.stepStart;
                    pos0 = this.mapper.extractY(y);
                    vel0 = this.mapper.extractYDot(y);
                } else {
                    // No reset state: prepare next step
                    this.isLastStep = this.forward ? this.stepStart >= t : this.stepStart <= t;
                    System.arraycopy(pos, 0, pos0, 0, pos.length);
                    System.arraycopy(vel, 0, vel0, 0, vel.length);
                }
            }

        } while (!this.isLastStep);

        // Dispatch results
        equations.setTime(this.stepStart);
        equations.setCompleteState(y);

        // Reset step start time and size
        this.stepStart = Double.NaN;
        this.stepSize = Double.NaN;
    }

    /**
     * Returns integrator order.
     * @return integrator order
     */
    public int getOrder() {
        return this.order;
    }

    /**
     * Returns the integration support.
     * @return the integration support
     */
    protected Support getSupport() {
        return this.support;
    }

    /**
     * Returns integrator previous state.
     * @return integrator previous state
     */
    protected State getPreviousState() {
        return this.previousState;
    }

    /**
     * Returns integrator current state.
     * @return integrator current state
     */
    protected State getCurrentState() {
        return this.currentState;
    }

    /**
     * Precompute coefficients gammastars.
     */
    private void precomputeCoefficients() {
        // Precompute:
        // c, gamma (internal arrays) and gammastars
        final double[] c = new double[this.order + 1];
        final double[] gamma = new double[this.order];
        this.gammastars = new double[this.order + 1];
        c[0] = 0;
        c[1] = 1;
        gamma[0] = 0;
        this.gammastars[0] = 0;
        this.gammastars[1] = -1. / 2.;
        for (int j = 2; j <= this.order; j++) {
            // c
            double s = 0.;
            double n = 0.;
            for (int i = 0; i < j; i++) {
                s -= c[i] / (j + 1 - i);
            }
            c[j] = s;
            // page 17 (2.43)
            for (int i = 0; i < j; i++) {
                n += c[i];
            }
            // Gamma
            gamma[j - 1] = n;

            // Gammastars
            this.gammastars[j] = gamma[j - 1] - gamma[j - 2];
        }
    }

    /**
     * Initialize integrator.
     * @param equations equations
     * @param t0 initial time
     * @param y0 initial state vector
     * @param yDot0 initial state vector derivative
     * @param t target time
     * @param y state vector at the end of the step (may not be at target time)
     * @param yDot state vector derivative at the end of the step (may not be at target time)
     * @return computation time at the end of the step
     */
    private double initialize(final SecondOrderDifferentialEquations equations,
            final double t0,
            final double[] y0,
            final double[] yDot0,
            final double t,
            final double[] y,
            final double[] yDot) {
        
        // Initialize integration support
        this.support = new Support(this.order);

        // Get initial value of acceleration
        final double[] acc = new double[y0.length];
        equations.computeSecondDerivatives(t0, y0, yDot0, acc);
        this.support.initDeltaAcc(acc);

        // Initialize previous state
        this.previousState = new State(t0, y0.clone(), yDot0.clone());

        // Get initial error estimate
        double terrs = estimateError(yDot0, this.support.deltaAcc[2]);
        double terrd = estimateError(y0, yDot0);

        // Get initial step estimate for single and double integration
        final double h1s = 0.25 * MathLib.sqrt(this.eps / terrs);
        final double h1d = 0.25 * MathLib.sqrt(this.eps / terrd);

        // Get initial properly bounded step size and set step size in the right direction
        this.stepSize = MathLib.min(h1s, h1d);
        clampStepSize(t0, t);

        // Performs a first iteration of prediction-correction
        // Find optimized time step compliant with error thresholds
        this.iterationsFailed = 0;
        while (this.iterationsFailed < MAX_ITERATIONS_FAILED) {
            final double stepsize2 = this.stepSize * this.stepSize;
            this.support.init(this.stepSize);

            // Prediction
            for (int i = 0; i < y0.length; i++) {
                yDot[i] = yDot0[i] + this.stepSize * this.support.deltaAcc[1][i];
                y[i] = y0[i] + this.stepSize * yDot0[i] + stepsize2 * this.support.deltaAcc[1][i] / 2.0;
            }

            // Evaluation
            equations.computeSecondDerivatives(t0 + this.stepSize, y, yDot, acc);

            // Correction
            final double[][] newdiff = new double[this.support.getSize() + 2][y0.length];
            this.support.computeDiff(acc, newdiff);
            for (int i = 0; i < y0.length; i++) {
                yDot[i] += this.stepSize * newdiff[2][i] / 2.0;
                y[i] += stepsize2 * newdiff[2][i] / 6.0;
            }

            // Get error estimate
            terrs = estimateError(yDot0, newdiff[this.support.getSize() + 1]);
            terrd = estimateError(y0, newdiff[this.support.getSize() + 1]);

            // Check error
            final double errd = MathLib.abs(stepsize2 / 3.) * terrd;
            final double errs = MathLib.abs(this.stepSize / 2.) * terrs;
            if (errd > this.eps || errs > this.eps) {
                // Error too large: try again with half step
                this.iterationsFailed++;
                this.stepSize *= 0.5;
                continue;
            } else if (this.iterationsFailed == 0) {
                // Step success: try again with a larger step (if allowed), to find maximum initial step
                this.stepSize *= 2.0;
                if ((t0 + this.stepSize <= t) ^ this.forward) {
                    // Clamp time step if too large, and stop iterations
                    clampStepSize(t0, t);
                    break;
                }
                continue;
            }
            break;
        }

        if (this.iterationsFailed == MAX_ITERATIONS_FAILED) {
            // Failed to find initial step size for required tolerances
            throw new PatriusRuntimeException(PatriusMessages.INTERNAL_ERROR, null);
        }

        // Move time forward
        final double tNext = t0 + this.stepSize;

        // Re-evaluation of acceleration
        equations.computeSecondDerivatives(tNext, y, yDot, acc);

        // Update delta
        this.support.updateDeltaAcc(acc);

        // Update variables
        this.iterationsFailed = 0;
        this.support.updateIndices();
        this.stepSize *= 2.;

        // Return first integration state
        this.currentState = new State(tNext, y, yDot);
        return tNext;
    }

    /**
     * Performs one integration step.
     * @param equations equations
     * @param t0 initial time
     * @param y0 initial state vector
     * @param yDot0 initial state vector derivative
     * @param t target time
     * @param y state vector at the end of the step (may not be at target time)
     * @param yDot state vector derivative at the end of the step (may not be at target time)
     * @return computation time at the end of the step, NaN if re-initialization is required
     */
    @SuppressWarnings("PMD.AvoidArrayLoops")
    // Reason : false positive (2D array)
    private double integrateOneStep(final SecondOrderDifferentialEquations equations,
            final double t0,
            final double[] y0,
            final double[] yDot0,
            final double t,
            final double[] y,
            final double[] yDot) {

        // Temporary arrays
        final double[] acc = new double[y0.length];

        // Store previous data
        final State previousState2 = new State(this.previousState);
        this.previousState = new State(t0, y0.clone(), yDot0.clone());
        final double[][] previousDeltaAcc = new double[this.order + 1][y0.length];
        for (int m = 1; m <= this.support.getSize(); m++) {
            previousDeltaAcc[m] = this.support.deltaAcc[m].clone();
        }

        // Step size
        // Clamp step size in order to stop exactly at required time
        this.stepSize = this.forward ? MathLib.min(this.stepSize, t - t0) : MathLib.max(this.stepSize, t - t0);
        // Ensure that stepStart + stepSize does not overshoot t
        this.stepSize = avoidOvershoot(t0, t, this.stepSize, this.forward);
        final double tNext = t0 + this.stepSize;

        // Shift the support one step forward and precompute arrays Psi - Alpha - Beta - G and G' of support
        this.support.shiftForward(this.stepSize);

        // Prediction
        this.support.prediction(y0, yDot0, y, yDot, previousState2.y);

        // Evaluate force model
        equations.computeSecondDerivatives(tNext, y, yDot, acc);

        // Correction
        final double[][] newdiff = this.support.correction(y, yDot, acc);

        // Estimate errors
        final double terrs = estimateError(this.previousState.yDot, newdiff[this.support.getSize() + 1]);
        final double terrd = estimateError(this.previousState.y, newdiff[this.support.getSize() + 1]);
        final double errd = this.support.computeErrorCoefficientD() * terrd;
        final double errs = this.support.computeErrorCoefficientS() * terrs;

        if (errd > this.eps || errs > this.eps) {
            // Error is too large: step failed
            // Reset state to step start and use half step next time, hard reset if too many failed steps
            return resetIntegrator(equations, t0, y0, yDot0, t, y, yDot, previousState2, previousDeltaAcc);
        }

        // Step succeeded, reset fail counter
        this.iterationsFailed = 0;

        if (this.support.getSize() == this.order) {
            // Support has reached full size
            // Calculate step for next time
            final double sigma = this.support.computeSigma();
            
            // Compute lambda and gammastard
            final double[] lambda = new double[this.order + 1];
            final double[] gammastard = new double[this.order + 1];
            System.arraycopy(LAMBDA9, 0, lambda, 0, MathLib.min(LAMBDA9.length, this.order + 1));
            System.arraycopy(GAMMASTARD9, 0, gammastard, 0, MathLib.min(GAMMASTARD9.length, this.order + 1));
            for (int j = MAX_STANDARD_ORDER + 1; j <= this.order; j++) {
                lambda[j] = this.support.computeLambda(j);
                gammastard[j] = lambda[j] - lambda[j - 1];
            }

            // Compute error estimate
            final double erks = MathLib.abs(this.stepSize * this.gammastars[this.support.getSize()] * sigma) * terrs;
            final double erkd = MathLib.abs(this.stepSize * this.stepSize * gammastard[this.support.getSize()] * sigma)
                    * terrd;
            // compute r for single and double integration
            final double rs = MathLib.pow(this.eps / 2. / erks, 1. / (this.support.getSize() + 1));
            final double rd = MathLib.pow(this.eps / 2. / erkd, 1. / (this.support.getSize() + 2));
            // Use smallest r. Bound r in [0.5, 2]
            this.stepSize *= MathLib.min(MathLib.max(MathLib.min(rs, rd), 0.5), 2.);
        } else {
            // Support-size is still increasing
            equations.computeSecondDerivatives(tNext, y, yDot, acc);

            // Compute new differences
            this.support.computeDiff(acc, newdiff);

            this.stepSize *= 2.0;
        }

        // Update indices
        this.support.updateIndices();

        // Update integration state
        for (int m = 1; m <= this.support.getPreviousSize() + 1; m++) {
            this.support.deltaAcc[m] = newdiff[m];
        }

        this.currentState = new State(tNext, y, yDot);
        return tNext;
    }

    /**
     * Reset integrator: either performs integration with smaller time step or ask for full integrator reset if too many
     * steps failed.
     * @param equations equations
     * @param t0 initial time
     * @param y0 initial state vector
     * @param yDot0 initial state vector derivative
     * @param t target time
     * @param y state vector at the end of the step (may not be at target time)
     * @param yDot state vector derivative at the end of the step (may not be at target time)
     * @param previousState2 previous state
     * @param previousDeltaAcc previous delta-acceleration
     * @return computation time at the end of the step, NaN if re-initialization is required
     */
    private double resetIntegrator(final SecondOrderDifferentialEquations equations,
            final double t0,
            final double[] y0,
            final double[] yDot0,
            final double t,
            final double[] y,
            final double[] yDot,
            final State previousState2,
            final double[][] previousDeltaAcc) {
        this.iterationsFailed++;
        this.stepSize *= 0.5;

        // SHift supprt backward
        if (this.support.getPreviousSize() == this.order) {
            this.support.shiftBackward();
        }

        // Retrieve previous state
        System.arraycopy(this.previousState.y, 0, y, 0, y0.length);
        System.arraycopy(this.previousState.yDot, 0, yDot, 0, y0.length);
        this.previousState = previousState2;
        for (int m = 1; m <= this.support.getSize(); m++) {
            this.support.deltaAcc[m] = previousDeltaAcc[m].clone();
        }

        if (this.iterationsFailed >= MAX_ITERATIONS_FAILED) {
            // Too many failed iterations, starts initialization again
            return Double.NaN;
        }
        // Re-start step with smaller timestep
        return integrateOneStep(equations, t0, y0, yDot0, t, y, yDot);
    }

    /**
     * Clamp step size such that it is at least 4 ULP and does not go beyond final integration date.
     * @param t0 initial integration time
     * @param t final integration time
     */
    private void clampStepSize(final double t0,
            final double t) {
        // Do not go beyond final integration date
        this.stepSize = MathLib.min(this.stepSize, MathLib.abs(t - t0));
        // Step size should be at least 4 ULP
        this.stepSize = MathLib.max(this.stepSize, 4 * MathLib.ulp(1.) * t0);
        // Account for numerical quality issues in first line
        this.stepSize = avoidOvershoot(t0, t, this.stepSize, this.forward);
        // Account for integration direction
        this.stepSize = this.forward ? this.stepSize : -this.stepSize;
    }

    /**
     * Estimate error.
     * @param vect state vector or state vector derivative
     * @param diff difference
     * @return error
     */
    private double estimateError(final double[] vect,
            final double[] diff) {
        double terr = 0.0;
        for (int i = 0; i < vect.length; i++) {
            final double wtd = MathLib.abs(vect[i]) * this.relTol + this.absTol;
            terr += MathLib.pow(diff[i] / wtd, 2);
        }
        return MathLib.sqrt(terr);
    }
    
    /**
     * Set second order / first order state mapper.
     * @param mapper second order / first order state mapper
     */
    public void setMapper(final SecondOrderStateMapper mapper) {
        this.mapper = mapper;
        this.interpolator.setMapper(mapper);
    }
    
    /**
     * Returns the second order / first order state mapper.
     * @return the second order / first order state mapper
     */
    public SecondOrderStateMapper getMapper() {
        return this.mapper;
    }
}
