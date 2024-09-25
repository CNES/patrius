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
 * VERSION::DM:226:12/09/2014: problem with event detections.
 * VERSION::DM:454:24/11/2015:Class test updated according the new implementation for detectors
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.ode.events;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

import fr.cnes.sirius.patrius.math.analysis.solver.PegasusSolver;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;
import fr.cnes.sirius.patrius.math.exception.NoBracketingException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.ode.FirstOrderDifferentialEquations;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.GraggBulirschStoerIntegrator;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class ReappearingEventTest {
    @Test
    public void testDormandPrince()
                                   throws DimensionMismatchException, NumberIsTooSmallException,
                                   MaxCountExceededException, NoBracketingException {
        final double tEnd = this.test(1);
        assertEquals(6.0, tEnd, 1e-7);
    }

    @Test
    public void testGragg()
                           throws DimensionMismatchException, NumberIsTooSmallException,
                           MaxCountExceededException, NoBracketingException {
        final double tEnd = this.test(2);
        assertEquals(6.0, tEnd, 1e-7);
    }

    public double test(final int integratorType)
                                                throws DimensionMismatchException, NumberIsTooSmallException,
                                                MaxCountExceededException, NoBracketingException {
        final double e = 1e-15;
        FirstOrderIntegrator integrator;
        integrator = (integratorType == 1)
            ? new DormandPrince853Integrator(e, 100.0, 1e-7, 1e-7)
            : new GraggBulirschStoerIntegrator(e, 100.0, 1e-7, 1e-7);
        final PegasusSolver rootSolver = new PegasusSolver(e, e);
        integrator.addEventHandler(new Event(), 0.1, e, 1000, rootSolver);
        final double t0 = 6.0;
        final double tEnd = 10.0;
        final double[] y = { 2.0, 2.0, 2.0, 4.0, 2.0, 7.0, 15.0 };
        return integrator.integrate(new Ode(), t0, y, tEnd, y);
    }

    private static class Ode implements FirstOrderDifferentialEquations {
        @Override
        public int getDimension() {
            return 7;
        }

        @Override
        public void computeDerivatives(final double t, final double[] y, final double[] yDot) {
            Arrays.fill(yDot, 1.0);
        }
    }

    /** State events for this unit test. */
    protected static class Event implements EventHandler {

        @Override
        public void init(final double t0, final double[] y0, final double t) {
        }

        @Override
        public double g(final double t, final double[] y) {
            return y[6] - 15.0;
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
            // Never called.
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
