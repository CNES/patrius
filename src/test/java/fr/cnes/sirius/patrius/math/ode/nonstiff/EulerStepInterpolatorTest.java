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
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.math.ode.nonstiff;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;
import fr.cnes.sirius.patrius.math.exception.NoBracketingException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.ode.ContinuousOutputModel;
import fr.cnes.sirius.patrius.math.ode.EquationsMapper;
import fr.cnes.sirius.patrius.math.ode.TestProblem1;
import fr.cnes.sirius.patrius.math.ode.TestProblem3;
import fr.cnes.sirius.patrius.math.ode.sampling.StepHandler;
import fr.cnes.sirius.patrius.math.ode.sampling.StepInterpolatorTestUtils;
import fr.cnes.sirius.patrius.math.util.MathLib;

public class EulerStepInterpolatorTest {

    @Test
    public void noReset() throws MaxCountExceededException {

        final double[] y = { 0.0, 1.0, -2.0 };
        final double[][] yDot = { { 1.0, 2.0, -2.0 } };
        final EulerStepInterpolator interpolator = new EulerStepInterpolator();
        interpolator.reinitialize(new DummyIntegrator(interpolator), y, yDot, true,
            new EquationsMapper(0, y.length),
            new EquationsMapper[0]);
        interpolator.storeTime(0);
        interpolator.shift();
        interpolator.storeTime(1);

        final double[] result = interpolator.getInterpolatedState();
        for (int i = 0; i < result.length; ++i) {
            Assert.assertTrue(MathLib.abs(result[i] - y[i]) < 1.0e-10);
        }

    }

    @Test
    public void interpolationAtBounds() throws MaxCountExceededException {

        final double t0 = 0;
        final double[] y0 = { 0.0, 1.0, -2.0 };

        final double[] y = y0.clone();
        final double[][] yDot = { new double[y0.length] };
        final EulerStepInterpolator interpolator = new EulerStepInterpolator();
        interpolator.reinitialize(new DummyIntegrator(interpolator), y, yDot, true,
            new EquationsMapper(0, y.length),
            new EquationsMapper[0]);
        interpolator.storeTime(t0);

        final double dt = 1.0;
        interpolator.shift();
        y[0] = 1.0;
        y[1] = 3.0;
        y[2] = -4.0;
        yDot[0][0] = (y[0] - y0[0]) / dt;
        yDot[0][1] = (y[1] - y0[1]) / dt;
        yDot[0][2] = (y[2] - y0[2]) / dt;
        interpolator.storeTime(t0 + dt);

        interpolator.setInterpolatedTime(interpolator.getPreviousTime());
        double[] result = interpolator.getInterpolatedState();
        for (int i = 0; i < result.length; ++i) {
            Assert.assertTrue(MathLib.abs(result[i] - y0[i]) < 1.0e-10);
        }

        interpolator.setInterpolatedTime(interpolator.getCurrentTime());
        result = interpolator.getInterpolatedState();
        for (int i = 0; i < result.length; ++i) {
            Assert.assertTrue(MathLib.abs(result[i] - y[i]) < 1.0e-10);
        }

    }

    @Test
    public void interpolationInside() throws MaxCountExceededException {

        final double[] y = { 0.0, 1.0, -2.0 };
        final double[][] yDot = { { 1.0, 2.0, -2.0 } };
        final EulerStepInterpolator interpolator = new EulerStepInterpolator();
        interpolator.reinitialize(new DummyIntegrator(interpolator), y, yDot, true,
            new EquationsMapper(0, y.length),
            new EquationsMapper[0]);
        interpolator.storeTime(0);
        interpolator.shift();
        y[0] = 1.0;
        y[1] = 3.0;
        y[2] = -4.0;
        interpolator.storeTime(1);

        interpolator.setInterpolatedTime(0.1);
        double[] result = interpolator.getInterpolatedState();
        Assert.assertTrue(MathLib.abs(result[0] - 0.1) < 1.0e-10);
        Assert.assertTrue(MathLib.abs(result[1] - 1.2) < 1.0e-10);
        Assert.assertTrue(MathLib.abs(result[2] + 2.2) < 1.0e-10);

        interpolator.setInterpolatedTime(0.5);
        result = interpolator.getInterpolatedState();
        Assert.assertTrue(MathLib.abs(result[0] - 0.5) < 1.0e-10);
        Assert.assertTrue(MathLib.abs(result[1] - 2.0) < 1.0e-10);
        Assert.assertTrue(MathLib.abs(result[2] + 3.0) < 1.0e-10);

    }

    @Test
    public void derivativesConsistency()
                                        throws DimensionMismatchException, NumberIsTooSmallException,
                                        MaxCountExceededException, NoBracketingException {
        final TestProblem3 pb = new TestProblem3();
        final double step = (pb.getFinalTime() - pb.getInitialTime()) * 0.001;
        final EulerIntegrator integ = new EulerIntegrator(step);
        StepInterpolatorTestUtils.checkDerivativesConsistency(integ, pb, 1.0e-10);
    }

    @Test
    public void serialization()
                               throws IOException, ClassNotFoundException,
                               DimensionMismatchException, NumberIsTooSmallException,
                               MaxCountExceededException, NoBracketingException {

        final TestProblem1 pb = new TestProblem1();
        final double step = (pb.getFinalTime() - pb.getInitialTime()) * 0.001;
        final EulerIntegrator integ = new EulerIntegrator(step);
        integ.addStepHandler(new ContinuousOutputModel());
        integ.integrate(pb,
            pb.getInitialTime(), pb.getInitialState(),
            pb.getFinalTime(), new double[pb.getDimension()]);

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(bos);
        for (final StepHandler handler : integ.getStepHandlers()) {
            oos.writeObject(handler);
        }

        final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        final ObjectInputStream ois = new ObjectInputStream(bis);
        final ContinuousOutputModel cm = (ContinuousOutputModel) ois.readObject();

        final Random random = new Random(347588535632l);
        double maxError = 0.0;
        for (int i = 0; i < 1000; ++i) {
            final double r = random.nextDouble();
            final double time = r * pb.getInitialTime() + (1.0 - r) * pb.getFinalTime();
            cm.setInterpolatedTime(time);
            final double[] interpolatedY = cm.getInterpolatedState();
            final double[] theoreticalY = pb.computeTheoreticalState(time);
            final double dx = interpolatedY[0] - theoreticalY[0];
            final double dy = interpolatedY[1] - theoreticalY[1];
            final double error = dx * dx + dy * dy;
            if (error > maxError) {
                maxError = error;
            }
        }
        Assert.assertTrue(maxError < 0.001);

    }

    private static class DummyIntegrator extends RungeKuttaIntegrator {

        protected DummyIntegrator(final RungeKuttaStepInterpolator prototype) {
            super("dummy", new double[0], new double[0][0], new double[0], prototype, 1.);
        }

    }

}
