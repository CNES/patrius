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
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.ode.nonstiff;

import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * This class is used in the junit tests for the ODE integrators.
 * 
 * <p>
 * This specific problem is the following differential equation :
 * 
 * <pre>
 *    y1'' = -y1/r^3  y1 (0) = 1-e  y1' (0) = 0
 *    y2'' = -y2/r^3  y2 (0) = 0    y2' (0) =sqrt((1+e)/(1-e))
 *    r = sqrt (y1^2 + y2^2), e = 0.9
 * </pre>
 * 
 * This is a two-body problem in the plane which can be solved by Kepler's equation
 * 
 * <pre>
 *   y1 (t) = ...
 * </pre>
 * 
 * </p>
 */
public class TestProblem3
    extends TestProblemAbstract {

    /** Eccentricity */
    double e;

    /** theoretical state */
    private final double[] y;

    /**
     * Simple constructor.
     * 
     * @param e
     *        eccentricity
     */
    public TestProblem3(final double e) {
        super();
        this.e = e;
        final double[] y0 = { 1 - e, 0, 0, MathLib.sqrt((1 + e) / (1 - e)) };
        this.setInitialConditions(0.0, y0);
        this.setFinalConditions(20.0);
        final double[] errorScale = { 1.0, 1.0, 1.0, 1.0 };
        this.setErrorScale(errorScale);
        this.y = new double[y0.length];
    }

    /**
     * Simple constructor.
     */
    public TestProblem3() {
        this(0.1);
    }

    /**
     * Copy constructor.
     * 
     * @param problem
     *        problem to copy
     */
    public TestProblem3(final TestProblem3 problem) {
        super(problem);
        this.e = problem.e;
        this.y = problem.y.clone();
    }

    /** {@inheritDoc} */
    @Override
    public TestProblem3 copy() {
        return new TestProblem3(this);
    }

    @Override
    public void doComputeDerivatives(final double t, final double[] y, final double[] yDot) {

        // current radius
        final double r2 = y[0] * y[0] + y[1] * y[1];
        final double invR3 = 1 / (r2 * MathLib.sqrt(r2));

        // compute the derivatives
        yDot[0] = y[2];
        yDot[1] = y[3];
        yDot[2] = -invR3 * y[0];
        yDot[3] = -invR3 * y[1];

    }

    @Override
    public double[] computeTheoreticalState(final double t) {

        // solve Kepler's equation
        double E = t;
        double d = 0;
        double corr = 999.0;
        for (int i = 0; (i < 50) && (MathLib.abs(corr) > 1.0e-12); ++i) {
            final double f2 = this.e * MathLib.sin(E);
            final double f0 = d - f2;
            final double f1 = 1 - this.e * MathLib.cos(E);
            final double f12 = f1 + f1;
            corr = f0 * f12 / (f1 * f12 - f0 * f2);
            d -= corr;
            E = t + d;
        }

        final double cosE = MathLib.cos(E);
        final double sinE = MathLib.sin(E);

        this.y[0] = cosE - this.e;
        this.y[1] = MathLib.sqrt(1 - this.e * this.e) * sinE;
        this.y[2] = -sinE / (1 - this.e * cosE);
        this.y[3] = MathLib.sqrt(1 - this.e * this.e) * cosE / (1 - this.e * cosE);

        return this.y;
    }

}
