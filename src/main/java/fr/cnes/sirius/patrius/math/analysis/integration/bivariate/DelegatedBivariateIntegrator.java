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
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1490:26/04/2018: major change to Coppola architecture
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.integration.bivariate;

import fr.cnes.sirius.patrius.math.analysis.BivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.integration.UnivariateIntegrator;
import fr.cnes.sirius.patrius.math.analysis.solver.UnivariateSolverUtils;
import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.exception.TooManyEvaluationsException;
import fr.cnes.sirius.patrius.math.util.Incrementor;
import fr.cnes.sirius.patrius.math.util.MathUtils;

/**
 * Bivariate integrator based on two univariate integrators.
 *
 * @author GMV
 * 
 * @since 4.1
 *
 * @version $Id: SimpsonIntegrator.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class DelegatedBivariateIntegrator implements BivariateIntegrator {

    /** Univariate integrator associated to the 1st axis. */
    private final UnivariateIntegrator integratorX;

    /** Univariate integrator associated to the 2nd axis. */
    private final UnivariateIntegrator integratorY;

    /** Functions evaluation count. */
    private final Incrementor evaluations;

    /** Function to integrate. */
    private BivariateFunction function;

    /** Lower bound of the integration interval for the x value. */
    private double xmin;

    /** Upper bound of the integration interval for the x value. */
    private double xmax;

    /** Lower bound of the integration interval for the y value. */
    private double ymin;

    /** Upper bound of the integration interval for the y value. */
    private double ymax;

    /**
     * Build a bivariate integrator with the two specified univariate
     * integrators.
     * 
     * @param integratorXIn integrator along 1st variable
     * @param integratorYIn integrator along 2nd variable
     */
    public DelegatedBivariateIntegrator(final UnivariateIntegrator integratorXIn,
        final UnivariateIntegrator integratorYIn) {
        // Univariate integrators for the 1st and 2nd axis
        this.integratorX = integratorXIn;
        this.integratorY = integratorYIn;

        // Evaluations counter
        this.evaluations = new Incrementor();
    }

    /**
     * Prepares for computation. Subclasses must call this method if they
     * override any of the {@code solve} methods.
     *
     * @param maxEval
     *        the maximum number of evaluations.
     * @param f
     *        the objective function
     * @param xminIn lower x bound
     * @param xmaxIn upper x bound
     * @param yminIn lower y bound
     * @param ymaxIn upper y bound
     *
     * @throws NullArgumentException
     *         if {@code f} is {@code null}.
     */
    protected void setup(final int maxEval,
                         final BivariateFunction f,
                         final double xminIn,
                         final double xmaxIn,
                         final double yminIn,
                         final double ymaxIn) {

        // Checks
        MathUtils.checkNotNull(f);
        UnivariateSolverUtils.verifyInterval(xminIn, xmaxIn);
        UnivariateSolverUtils.verifyInterval(yminIn, ymaxIn);

        // Initialize function and integration boundaries
        this.function = f;

        this.xmin = xminIn;
        this.xmax = xmaxIn;
        this.ymin = yminIn;
        this.ymax = ymaxIn;

        // Reset evaluation counter
        this.evaluations.setMaximalCount(maxEval);
        this.evaluations.resetCount();
    }

    /** {@inheritDoc} */
    @Override
    public double integrate(final int maxEval,
                            final BivariateFunction f,
                            final double xminIn,
                            final double xmaxIn,
                            final double yminIn,
                            final double ymaxIn) {
        // Initialization.
        this.setup(maxEval, f, xminIn, xmaxIn, yminIn, ymaxIn);

        // Perform computation.
        return this.doIntegrate();

    }

    /**
     * Computes the objective function value.
     *
     * @param x
     *        x value at which the objective function must be evaluated.
     * @param y
     *        y value at which the objective function must be evaluated.
     *
     * @return the objective function value at specified point.
     *
     * @throws TooManyEvaluationsException
     *         if the maximal number of function evaluations is exceeded.
     */
    @SuppressWarnings("PMD.PreserveStackTrace")
    private double computeObjectiveValue(final double x, final double y) {
        try {
            this.evaluations.incrementCount();
        } catch (final MaxCountExceededException e) {
            throw new TooManyEvaluationsException(e.getMax());
        }

        return this.function.value(x, y);
    }

    /**
     * Computes the objective value integrated on the 2nd axis.
     *
     * @param x
     *        x value at which the objective function must be evaluated.
     *
     * @return the integral of objective function along the y-axis for the
     *         specified x.
     *
     */
    private double computeIntegratedObjectiveValue(final double x) {
        // Univariate objective function:
        // x is fixed to the given value and y is the variable
        final UnivariateFunction fy = new UnivariateFunction(){
            /** {@inheritDoc} */
            @Override
            public double value(final double y) {
                return DelegatedBivariateIntegrator.this.computeObjectiveValue(x, y);
            }
        };

        // Returns the integral of fy between ymin and ymax with the given x value
        return this.integratorY.integrate(this.getMaxEvaluations(), fy, this.getYMin(), this.getYMax());
    }

    /**
     * Method for implementing actual integration algorithms in derived classes.
     *
     * @return the root
     * @throws TooManyEvaluationsException
     *         if the maximal number of evaluations is exceeded.
     * @throws MaxCountExceededException
     *         if the maximum iteration count is exceeded or if a convergence
     *         problem is detected for one of the integrators
     */
    private double doIntegrate() {
        // Univariate objective function
        final UnivariateFunction fx = new UnivariateFunction(){
            /** {@inheritDoc} */
            @Override
            public double value(final double x) {
                return DelegatedBivariateIntegrator.this.computeIntegratedObjectiveValue(x);
            }
        };

        // Return the integral of fx between xmin and xmax
        return this.integratorX.integrate(this.getMaxEvaluations(), fx, this.getXMin(), this.getXMax());
    }

    /**
     * Gets the univariate integrator associated to the 1st axis.
     *
     * @return the univariate integrator associated to the 1st axis
     */
    public UnivariateIntegrator getIntegratorX() {
        return this.integratorX;
    }

    /**
     * Gets the univariate integrator associated to the 2nd axis.
     *
     * @return the univariate integrator associated to the 2nd axis
     */
    public UnivariateIntegrator getIntegratorY() {
        return this.integratorY;
    }

    /** {@inheritDoc} */
    @Override
    public BivariateFunction getFunction() {
        return this.function;
    }

    /** {@inheritDoc} */
    @Override
    public int getEvaluations() {
        return this.evaluations.getCount();
    }

    /** {@inheritDoc} */
    @Override
    public int getMaxEvaluations() {
        return this.evaluations.getMaximalCount();
    }

    /** {@inheritDoc} */
    @Override
    public double getXMin() {
        return this.xmin;
    }

    /** {@inheritDoc} */
    @Override
    public double getXMax() {
        return this.xmax;
    }

    /** {@inheritDoc} */
    @Override
    public double getYMin() {
        return this.ymin;
    }

    /** {@inheritDoc} */
    @Override
    public double getYMax() {
        return this.ymax;
    }
}
