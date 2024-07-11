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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2099:15/05/2019:[PATRIUS] Possibilite de by-passer le critere du pas min dans l'integrateur
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:653:02/08/2016:change error estimation
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.math.ode.nonstiff;

import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * This class implements the 5(4) Higham and Hall integrator for
 * Ordinary Differential Equations.
 * 
 * <p>
 * This integrator is an embedded Runge-Kutta integrator of order 5(4) used in local extrapolation mode (i.e. the
 * solution is computed using the high order formula) with stepsize control (and automatic step initialization) and
 * continuous output. This method uses 7 functions evaluations per step.
 * </p>
 * 
 * @version $Id: HighamHall54Integrator.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 1.2
 */

// CHECKSTYLE: stop MagicNumber check
public class HighamHall54Integrator extends EmbeddedRungeKuttaIntegrator {

    /** Integrator method name. */
    private static final String METHOD_NAME = "Higham-Hall 5(4)";

    /** Time steps Butcher array. */
    private static final double[] STATIC_C = {
        2.0 / 9.0, 1.0 / 3.0, 1.0 / 2.0, 3.0 / 5.0, 1.0, 1.0
    };

    /** Internal weights Butcher array. */
    private static final double[][] STATIC_A = {
        { 2.0 / 9.0 },
        { 1.0 / 12.0, 1.0 / 4.0 },
        { 1.0 / 8.0, 0.0, 3.0 / 8.0 },
        { 91.0 / 500.0, -27.0 / 100.0, 78.0 / 125.0, 8.0 / 125.0 },
        { -11.0 / 20.0, 27.0 / 20.0, 12.0 / 5.0, -36.0 / 5.0, 5.0 },
        { 1.0 / 12.0, 0.0, 27.0 / 32.0, -4.0 / 3.0, 125.0 / 96.0, 5.0 / 48.0 }
    };

    /** Propagation weights Butcher array. */
    private static final double[] STATIC_B = {
        1.0 / 12.0, 0.0, 27.0 / 32.0, -4.0 / 3.0, 125.0 / 96.0, 5.0 / 48.0, 0.0
    };

    /** Error weights Butcher array. */
    private static final double[] STATIC_E = {
        -1.0 / 20.0, 0.0, 81.0 / 160.0, -6.0 / 5.0, 25.0 / 32.0, 1.0 / 16.0, -1.0 / 10.0
    };

    /**
     * Simple constructor. Build a fifth order Higham and Hall integrator with the given step bounds
     * 
     * @param minStep minimal step (sign is irrelevant, regardless of integration direction, forward
     *        or backward), the last step can be smaller than this
     * @param maxStep maximal step (sign is irrelevant, regardless of integration direction, forward
     *        or backward), the last step can be smaller than this
     * @param scalAbsoluteTolerance allowed absolute error
     * @param scalRelativeTolerance allowed relative error
     */
    public HighamHall54Integrator(final double minStep, final double maxStep,
        final double scalAbsoluteTolerance,
        final double scalRelativeTolerance) {
        this(minStep, maxStep, scalAbsoluteTolerance, scalRelativeTolerance, false);
    }

    /**
     * Simple constructor. Build a fifth order Higham and Hall integrator with the given step bounds
     * 
     * @param minStep minimal step (sign is irrelevant, regardless of integration direction, forward
     *        or backward), the last step can be smaller than this
     * @param maxStep maximal step (sign is irrelevant, regardless of integration direction, forward
     *        or backward), the last step can be smaller than this
     * @param vecAbsoluteTolerance allowed absolute error
     * @param vecRelativeTolerance allowed relative error
     */
    public HighamHall54Integrator(final double minStep, final double maxStep,
        final double[] vecAbsoluteTolerance,
        final double[] vecRelativeTolerance) {
        this(minStep, maxStep, vecAbsoluteTolerance, vecRelativeTolerance, false);
    }

    /**
     * Simple constructor. Build a fifth order Higham and Hall integrator with the given step bounds
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
    public HighamHall54Integrator(final double minStep, final double maxStep,
        final double scalAbsoluteTolerance, final double scalRelativeTolerance,
        final boolean acceptSmall) {
        super(METHOD_NAME, false, STATIC_C, STATIC_A, STATIC_B, new HighamHall54StepInterpolator(),
            minStep, maxStep, scalAbsoluteTolerance, scalRelativeTolerance, acceptSmall);
    }

    /**
     * Simple constructor. Build a fifth order Higham and Hall integrator with the given step bounds
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
    public HighamHall54Integrator(final double minStep, final double maxStep,
        final double[] vecAbsoluteTolerance, final double[] vecRelativeTolerance,
        final boolean acceptSmall) {
        super(METHOD_NAME, false, STATIC_C, STATIC_A, STATIC_B, new HighamHall54StepInterpolator(),
            minStep, maxStep, vecAbsoluteTolerance, vecRelativeTolerance, acceptSmall);
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

        // initialize error
        double error = 0;

        // loop on all states whose error has to be estimated
        for (final int j : this.estimateErrorStates) {
            // compute state error and add to global error
            double errSum = STATIC_E[0] * yDotK[0][j];
            for (int l = 1; l < STATIC_E.length; ++l) {
                errSum += STATIC_E[l] * yDotK[l][j];
            }

            final double yScale = MathLib.max(MathLib.abs(y0[j]), MathLib.abs(y1[j]));
            final double tol = (this.vecAbsoluteTolerance == null) ?
                (this.scalAbsoluteTolerance + this.scalRelativeTolerance * yScale) :
                (this.vecAbsoluteTolerance[j] + this.vecRelativeTolerance[j] * yScale);
            final double ratio = h * errSum / tol;
            error += ratio * ratio;
        }

        // compute and return normalized error
        return MathLib.sqrt(error / this.estimateErrorStates.length);

    }

    // CHECKSTYLE: resume MagicNumber check
}
