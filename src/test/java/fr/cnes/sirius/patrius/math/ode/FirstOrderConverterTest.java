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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.ode;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;
import fr.cnes.sirius.patrius.math.exception.NoBracketingException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import fr.cnes.sirius.patrius.math.util.MathLib;

public class FirstOrderConverterTest {

    @Test
    public void testDoubleDimension() {
        for (int i = 1; i < 10; ++i) {
            final SecondOrderDifferentialEquations eqn2 = new Equations(i, 0.2);
            final FirstOrderConverter eqn1 = new FirstOrderConverter(eqn2);
            Assert.assertTrue(eqn1.getDimension() == (2 * eqn2.getDimension()));
        }
    }

    @Test
    public void testDecreasingSteps()
                                     throws DimensionMismatchException, NumberIsTooSmallException,
                                     MaxCountExceededException,
                                     NoBracketingException {

        double previousError = Double.NaN;
        for (int i = 0; i < 10; ++i) {

            final double step = MathLib.pow(2.0, -(i + 1));
            final double error = this.integrateWithSpecifiedStep(4.0, 0.0, 1.0, step)
                - MathLib.sin(4.0);
            if (i > 0) {
                Assert.assertTrue(MathLib.abs(error) < MathLib.abs(previousError));
            }
            previousError = error;

        }
    }

    @Test
    public void testSmallStep()
                               throws DimensionMismatchException, NumberIsTooSmallException, MaxCountExceededException,
                               NoBracketingException {
        final double error = this.integrateWithSpecifiedStep(4.0, 0.0, 1.0, 1.0e-4)
            - MathLib.sin(4.0);
        Assert.assertTrue(MathLib.abs(error) < 1.0e-10);
    }

    @Test
    public void testBigStep()
                             throws DimensionMismatchException, NumberIsTooSmallException, MaxCountExceededException,
                             NoBracketingException {
        final double error = this.integrateWithSpecifiedStep(4.0, 0.0, 1.0, 0.5)
            - MathLib.sin(4.0);
        Assert.assertTrue(MathLib.abs(error) > 0.1);
    }

    private static class Equations
        implements SecondOrderDifferentialEquations {

        private final int n;

        private final double omega2;

        public Equations(final int n, final double omega) {
            this.n = n;
            this.omega2 = omega * omega;
        }

        @Override
        public int getDimension() {
            return this.n;
        }

        @Override
        public void computeSecondDerivatives(final double t, final double[] y, final double[] yDot,
                                             final double[] yDDot) {
            for (int i = 0; i < this.n; ++i) {
                yDDot[i] = -this.omega2 * y[i];
            }
        }

    }

    private double integrateWithSpecifiedStep(final double omega,
                                              final double t0, final double t,
                                              final double step)
                                                                throws DimensionMismatchException,
                                                                NumberIsTooSmallException, MaxCountExceededException,
                                                                NoBracketingException {
        final double[] y0 = new double[2];
        y0[0] = MathLib.sin(omega * t0);
        y0[1] = omega * MathLib.cos(omega * t0);
        final ClassicalRungeKuttaIntegrator i = new ClassicalRungeKuttaIntegrator(step);
        final double[] y = new double[2];
        i.integrate(new FirstOrderConverter(new Equations(1, omega)), t0, y0, t, y);
        return y[0];
    }

}
