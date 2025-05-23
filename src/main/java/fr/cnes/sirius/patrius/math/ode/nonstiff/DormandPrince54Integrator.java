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
 * VERSION::DM:653:02/08/2016:change error estimation
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.math.ode.nonstiff;

import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * This class implements the 5(4) Dormand-Prince integrator for Ordinary
 * Differential Equations.
 * 
 * <p>
 * This integrator is an embedded Runge-Kutta integrator of order 5(4) used in local extrapolation mode (i.e. the
 * solution is computed using the high order formula) with stepsize control (and automatic step initialization) and
 * continuous output. This method uses 7 functions evaluations per step. However, since this is an <i>fsal</i>, the last
 * evaluation of one step is the same as the first evaluation of the next step and hence can be avoided. So the cost is
 * really 6 functions evaluations per step.
 * </p>
 * 
 * <p>
 * This method has been published (whithout the continuous output that was added by Shampine in 1986) in the following
 * article :
 * 
 * <pre>
 *  A family of embedded Runge-Kutta formulae
 *  J. R. Dormand and P. J. Prince
 *  Journal of Computational and Applied Mathematics
 *  volume 6, no 1, 1980, pp. 19-26
 * </pre>
 * 
 * </p>
 * 
 * @version $Id: DormandPrince54Integrator.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 1.2
 */
// CHECKSTYLE: stop MagicNumber check
public class DormandPrince54Integrator extends EmbeddedRungeKuttaIntegrator {

    /** Integrator method name. */
    private static final String METHOD_NAME = "Dormand-Prince 5(4)";

    /** Time steps Butcher array. */
    private static final double[] STATIC_C = {
        1.0 / 5.0, 3.0 / 10.0, 4.0 / 5.0, 8.0 / 9.0, 1.0, 1.0
    };

    /** Internal weights Butcher array. */
    private static final double[][] STATIC_A = {
        { 1.0 / 5.0 },
        { 3.0 / 40.0, 9.0 / 40.0 },
        { 44.0 / 45.0, -56.0 / 15.0, 32.0 / 9.0 },
        { 19372.0 / 6561.0, -25360.0 / 2187.0, 64448.0 / 6561.0, -212.0 / 729.0 },
        { 9017.0 / 3168.0, -355.0 / 33.0, 46732.0 / 5247.0, 49.0 / 176.0, -5103.0 / 18656.0 },
        { 35.0 / 384.0, 0.0, 500.0 / 1113.0, 125.0 / 192.0, -2187.0 / 6784.0, 11.0 / 84.0 }
    };

    /** Propagation weights Butcher array. */
    private static final double[] STATIC_B = {
        35.0 / 384.0, 0.0, 500.0 / 1113.0, 125.0 / 192.0, -2187.0 / 6784.0, 11.0 / 84.0, 0.0
    };

    /** Error array, element 1. */
    private static final double E1 = 71.0 / 57600.0;

    // element 2 is zero, so it is neither stored nor used

    /** Error array, element 3. */
    private static final double E3 = -71.0 / 16695.0;

    /** Error array, element 4. */
    private static final double E4 = 71.0 / 1920.0;

    /** Error array, element 5. */
    private static final double E5 = -17253.0 / 339200.0;

    /** Error array, element 6. */
    private static final double E6 = 22.0 / 525.0;

    /** Error array, element 7. */
    private static final double E7 = -1.0 / 40.0;

    /**
     * Simple constructor. Build a fifth order Dormand-Prince integrator with the given step bounds
     * 
     * @param minStep minimal step (sign is irrelevant, regardless of integration direction, forward
     *        or backward), the last step can be smaller than this
     * @param maxStep maximal step (sign is irrelevant, regardless of integration direction, forward
     *        or backward), the last step can be smaller than this
     * @param scalAbsoluteTolerance allowed absolute error
     * @param scalRelativeTolerance allowed relative error
     */
    public DormandPrince54Integrator(final double minStep, final double maxStep,
        final double scalAbsoluteTolerance, final double scalRelativeTolerance) {
        this(minStep, maxStep, scalAbsoluteTolerance, scalRelativeTolerance, false);
    }

    /**
     * Simple constructor. Build a fifth order Dormand-Prince integrator with the given step bounds
     * 
     * @param minStep minimal step (sign is irrelevant, regardless of integration direction, forward
     *        or backward), the last step can be smaller than this
     * @param maxStep maximal step (sign is irrelevant, regardless of integration direction, forward
     *        or backward), the last step can be smaller than this
     * @param vecAbsoluteTolerance allowed absolute error
     * @param vecRelativeTolerance allowed relative error
     */
    public DormandPrince54Integrator(final double minStep, final double maxStep,
        final double[] vecAbsoluteTolerance, final double[] vecRelativeTolerance) {
        this(minStep, maxStep, vecAbsoluteTolerance, vecRelativeTolerance, false);
    }

    /**
     * Simple constructor. Build a fifth order Dormand-Prince integrator with the given step bounds
     * 
     * @param minStep minimal step (sign is irrelevant, regardless of integration direction, forward
     *        or backward), the last step can be smaller than this
     * @param maxStep maximal step (sign is irrelevant, regardless of integration direction, forward
     *        or backward), the last step can be smaller than this
     * @param scalAbsoluteTolerance allowed absolute error
     * @param scalRelativeTolerance allowed relative error
     * @param acceptSmall if true, steps smaller than the minimal value are silently increased up to
     *        this value, if false such small steps generate an exception
     */
    public DormandPrince54Integrator(final double minStep, final double maxStep,
        final double scalAbsoluteTolerance, final double scalRelativeTolerance,
        final boolean acceptSmall) {
        super(METHOD_NAME, true, STATIC_C, STATIC_A, STATIC_B,
            new DormandPrince54StepInterpolator(), minStep, maxStep, scalAbsoluteTolerance,
            scalRelativeTolerance, acceptSmall);
    }

    /**
     * Simple constructor. Build a fifth order Dormand-Prince integrator with the given step bounds
     * 
     * @param minStep minimal step (sign is irrelevant, regardless of integration direction, forward
     *        or backward), the last step can be smaller than this
     * @param maxStep maximal step (sign is irrelevant, regardless of integration direction, forward
     *        or backward), the last step can be smaller than this
     * @param vecAbsoluteTolerance allowed absolute error
     * @param vecRelativeTolerance allowed relative error
     * @param acceptSmall if true, steps smaller than the minimal value are silently increased up to
     *        this value, if false such small steps generate an exception
     */
    public DormandPrince54Integrator(final double minStep, final double maxStep,
        final double[] vecAbsoluteTolerance, final double[] vecRelativeTolerance,
        final boolean acceptSmall) {
        super(METHOD_NAME, true, STATIC_C, STATIC_A, STATIC_B,
            new DormandPrince54StepInterpolator(), minStep, maxStep, vecAbsoluteTolerance,
            vecRelativeTolerance, acceptSmall);
    }

    /** {@inheritDoc} */
    @Override
    public int getOrder() {
        return 5;
    }

    /** {@inheritDoc} */
    @Override
    protected double estimateError(final double[][] yDotK,
                                   final double[] y0, final double[] y1,
                                   final double h) {

        double error = 0;

        for (final int j : this.estimateErrorStates) {
            final double errSum = E1 * yDotK[0][j] + E3 * yDotK[2][j] +
                E4 * yDotK[3][j] + E5 * yDotK[4][j] +
                E6 * yDotK[5][j] + E7 * yDotK[6][j];

            final double yScale = MathLib.max(MathLib.abs(y0[j]), MathLib.abs(y1[j]));
            final double tol = (this.vecAbsoluteTolerance == null) ?
                (this.scalAbsoluteTolerance + this.scalRelativeTolerance * yScale) :
                (this.vecAbsoluteTolerance[j] + this.vecRelativeTolerance[j] * yScale);
            final double ratio = h * errSum / tol;
            error += ratio * ratio;
        }

        return MathLib.sqrt(error / this.estimateErrorStates.length);

    }

    // CHECKSTYLE: resume MagicNumber check
}
