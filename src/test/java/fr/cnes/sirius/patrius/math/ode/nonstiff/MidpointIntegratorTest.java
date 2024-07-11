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

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;
import fr.cnes.sirius.patrius.math.exception.NoBracketingException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.ode.FirstOrderDifferentialEquations;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.TestProblem1;
import fr.cnes.sirius.patrius.math.ode.TestProblem5;
import fr.cnes.sirius.patrius.math.ode.TestProblemAbstract;
import fr.cnes.sirius.patrius.math.ode.TestProblemFactory;
import fr.cnes.sirius.patrius.math.ode.TestProblemHandler;
import fr.cnes.sirius.patrius.math.ode.events.EventHandler;
import fr.cnes.sirius.patrius.math.ode.sampling.StepHandler;
import fr.cnes.sirius.patrius.math.ode.sampling.StepInterpolator;
import fr.cnes.sirius.patrius.math.util.MathLib;

public class MidpointIntegratorTest {

    @Test(expected = DimensionMismatchException.class)
    public void testDimensionCheck()
                                    throws DimensionMismatchException, NumberIsTooSmallException,
                                    MaxCountExceededException, NoBracketingException {
        final TestProblem1 pb = new TestProblem1();
        new MidpointIntegrator(0.01).integrate(pb,
            0.0, new double[pb.getDimension() + 10],
            1.0, new double[pb.getDimension() + 10]);
        Assert.fail("an exception should have been thrown");
    }

    @Test
    public void testDecreasingSteps()
                                     throws DimensionMismatchException, NumberIsTooSmallException,
                                     MaxCountExceededException, NoBracketingException {

        final TestProblemAbstract[] problems = TestProblemFactory.getProblems();
        for (final TestProblemAbstract problem : problems) {

            double previousValueError = Double.NaN;
            double previousTimeError = Double.NaN;
            for (int i = 4; i < 10; ++i) {

                final TestProblemAbstract pb = problem.copy();
                final double step = (pb.getFinalTime() - pb.getInitialTime()) * MathLib.pow(2.0, -i);
                final FirstOrderIntegrator integ = new MidpointIntegrator(step);
                final TestProblemHandler handler = new TestProblemHandler(pb, integ);
                integ.addStepHandler(handler);
                final EventHandler[] functions = pb.getEventsHandlers();
                for (final EventHandler function : functions) {
                    integ.addEventHandler(function,
                        Double.POSITIVE_INFINITY, 1.0e-6 * step, 1000);
                }
                final double stopTime = integ.integrate(pb,
                    pb.getInitialTime(), pb.getInitialState(),
                    pb.getFinalTime(), new double[pb.getDimension()]);
                if (functions.length == 0) {
                    Assert.assertEquals(pb.getFinalTime(), stopTime, 1.0e-10);
                }

                final double valueError = handler.getMaximalValueError();
                if (i > 4) {
                    Assert.assertTrue(valueError < MathLib.abs(previousValueError));
                }
                previousValueError = valueError;

                final double timeError = handler.getMaximalTimeError();
                if (i > 4) {
                    Assert.assertTrue(timeError <= MathLib.abs(previousTimeError));
                }
                previousTimeError = timeError;

            }

        }

    }

    @Test
    public void testSmallStep()
                               throws DimensionMismatchException, NumberIsTooSmallException,
                               MaxCountExceededException, NoBracketingException {

        final TestProblem1 pb = new TestProblem1();
        final double step = (pb.getFinalTime() - pb.getInitialTime()) * 0.001;

        final FirstOrderIntegrator integ = new MidpointIntegrator(step);
        final TestProblemHandler handler = new TestProblemHandler(pb, integ);
        integ.addStepHandler(handler);
        integ.integrate(pb,
            pb.getInitialTime(), pb.getInitialState(),
            pb.getFinalTime(), new double[pb.getDimension()]);

        Assert.assertTrue(handler.getLastError() < 2.0e-7);
        Assert.assertTrue(handler.getMaximalValueError() < 1.0e-6);
        Assert.assertEquals(0, handler.getMaximalTimeError(), 1.0e-12);
        Assert.assertEquals("midpoint", integ.getName());

    }

    @Test
    public void testBigStep()
                             throws DimensionMismatchException, NumberIsTooSmallException,
                             MaxCountExceededException, NoBracketingException {

        final TestProblem1 pb = new TestProblem1();
        final double step = (pb.getFinalTime() - pb.getInitialTime()) * 0.2;

        final FirstOrderIntegrator integ = new MidpointIntegrator(step);
        final TestProblemHandler handler = new TestProblemHandler(pb, integ);
        integ.addStepHandler(handler);
        integ.integrate(pb,
            pb.getInitialTime(), pb.getInitialState(),
            pb.getFinalTime(), new double[pb.getDimension()]);

        Assert.assertTrue(handler.getLastError() > 0.01);
        Assert.assertTrue(handler.getMaximalValueError() > 0.05);
        Assert.assertEquals(0, handler.getMaximalTimeError(), 1.0e-12);

    }

    @Test
    public void testBackward()
                              throws DimensionMismatchException, NumberIsTooSmallException,
                              MaxCountExceededException, NoBracketingException {

        final TestProblem5 pb = new TestProblem5();
        final double step = MathLib.abs(pb.getFinalTime() - pb.getInitialTime()) * 0.001;

        final FirstOrderIntegrator integ = new MidpointIntegrator(step);
        final TestProblemHandler handler = new TestProblemHandler(pb, integ);
        integ.addStepHandler(handler);
        integ.integrate(pb, pb.getInitialTime(), pb.getInitialState(),
            pb.getFinalTime(), new double[pb.getDimension()]);

        Assert.assertTrue(handler.getLastError() < 6.0e-4);
        Assert.assertTrue(handler.getMaximalValueError() < 6.0e-4);
        Assert.assertEquals(0, handler.getMaximalTimeError(), 1.0e-12);
        Assert.assertEquals("midpoint", integ.getName());
    }

    @Test
    public void testStepSize()
                              throws DimensionMismatchException, NumberIsTooSmallException,
                              MaxCountExceededException, NoBracketingException {
        final double step = 1.23456;
        final FirstOrderIntegrator integ = new MidpointIntegrator(step);
        integ.addStepHandler(new StepHandler(){
            @Override
            public void handleStep(final StepInterpolator interpolator, final boolean isLast) {
                if (!isLast) {
                    Assert.assertEquals(step,
                        interpolator.getCurrentTime() - interpolator.getPreviousTime(),
                        1.0e-12);
                }
            }

            @Override
            public void init(final double t0, final double[] y0, final double t) {
            }
        });
        integ.integrate(new FirstOrderDifferentialEquations(){
            @Override
            public void computeDerivatives(final double t, final double[] y, final double[] dot) {
                dot[0] = 1.0;
            }

            @Override
            public int getDimension() {
                return 1;
            }
        }, 0.0, new double[] { 0.0 }, 5.0, new double[1]);
    }

}
