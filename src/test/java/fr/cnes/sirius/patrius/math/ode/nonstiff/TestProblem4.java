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
 * VERSION:4.13:FA:FA-79:08/12/2023:[PATRIUS] Probleme dans la fonction g de LocalTimeAngleDetector
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:454:24/11/2015:Class test updated according the new implementation for detectors
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.math.ode.nonstiff;

import fr.cnes.sirius.patrius.math.ode.events.EventHandler;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class is used in the junit tests for the ODE integrators.
 * 
 * <p>
 * This specific problem is the following differential equation :
 * 
 * <pre>
 *    x'' = -x
 * </pre>
 * 
 * And when x decreases down to 0, the state should be changed as follows :
 * 
 * <pre>
 *   x' -> -x'
 * </pre>
 * 
 * The theoretical solution of this problem is x = |sin(t+a)|
 * </p>
 */
public class TestProblem4
    extends TestProblemAbstract {

    /** Time offset. */
    private final double a;

    /** theoretical state */
    private final double[] y;

    /** Simple constructor. */
    public TestProblem4() {
        super();
        this.a = 1.2;
        final double[] y0 = { MathLib.sin(this.a), MathLib.cos(this.a) };
        this.setInitialConditions(0.0, y0);
        this.setFinalConditions(15);
        final double[] errorScale = { 1.0, 0.0 };
        this.setErrorScale(errorScale);
        this.y = new double[y0.length];
    }

    /**
     * Copy constructor.
     * 
     * @param problem
     *        problem to copy
     */
    public TestProblem4(final TestProblem4 problem) {
        super(problem);
        this.a = problem.a;
        this.y = problem.y.clone();
    }

    /** {@inheritDoc} */
    @Override
    public TestProblem4 copy() {
        return new TestProblem4(this);
    }

    @Override
    public EventHandler[] getEventsHandlers() {
        return new EventHandler[] { new Bounce(), new Stop() };
    }

    /**
     * Get the theoretical events times.
     * 
     * @return theoretical events times
     */
    @Override
    public double[] getTheoreticalEventsTimes() {
        return new double[] {
            1 * FastMath.PI - this.a,
            2 * FastMath.PI - this.a,
            3 * FastMath.PI - this.a,
            4 * FastMath.PI - this.a,
            12.0
        };
    }

    @Override
    public void doComputeDerivatives(final double t, final double[] y, final double[] yDot) {
        yDot[0] = y[1];
        yDot[1] = -y[0];
    }

    @Override
    public double[] computeTheoreticalState(final double t) {
        final double sin = MathLib.sin(t + this.a);
        final double cos = MathLib.cos(t + this.a);
        this.y[0] = MathLib.abs(sin);
        this.y[1] = (sin >= 0) ? cos : -cos;
        return this.y;
    }

    private static class Bounce implements EventHandler {

        private int sign;

        public Bounce() {
            this.sign = +1;
        }

        @Override
        public void init(final double t0, final double[] y0, final double t) {
        }

        @Override
        public double g(final double t, final double[] y) {
            return this.sign * y[0];
        }

        @Override
        public boolean shouldBeRemoved() {
            return false;
        }

        @Override
        public Action eventOccurred(final double t, final double[] y, final boolean increasing, final boolean forward) {
            // this sign change is needed because the state will be reset soon
            this.sign = -this.sign;
            return Action.RESET_STATE;
        }

        @Override
        public void resetState(final double t, final double[] y) {
            y[0] = -y[0];
            y[1] = -y[1];
        }

        @Override
        public int getSlopeSelection() {
            return 2;
        }

        /** {@inheritDoc} */
        @Override
        public boolean filterEvent(final double t,
                final double[] y,
                final boolean increasing,
                final boolean forward) {
            return false;
        }
    }

    private static class Stop implements EventHandler {

        public Stop() {
        }

        @Override
        public void init(final double t0, final double[] y0, final double t) {
        }

        @Override
        public double g(final double t, final double[] y) {
            return t - 12.0;
        }

        @Override
        public boolean shouldBeRemoved() {
            return false;
        }

        @Override
        public Action eventOccurred(final double t, final double[] y, final boolean increasing, final boolean forward) {
            return Action.STOP;
        }

        @Override
        public void resetState(final double t, final double[] y) {
        }

        @Override
        public int getSlopeSelection() {
            return 2;
        }

        /** {@inheritDoc} */
        @Override
        public boolean filterEvent(final double t,
                final double[] y,
                final boolean increasing,
                final boolean forward) {
            return false;
        }
    }

}
