/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
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
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
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
import fr.cnes.sirius.patrius.math.ode.TestProblem3;
import fr.cnes.sirius.patrius.math.ode.sampling.StepHandler;
import fr.cnes.sirius.patrius.math.ode.sampling.StepInterpolator;
import fr.cnes.sirius.patrius.math.ode.sampling.StepInterpolatorTestUtils;
import fr.cnes.sirius.patrius.math.util.MathLib;

public class HighamHall54StepInterpolatorTest {

    @Test
    public void derivativesConsistency()
                                        throws DimensionMismatchException, NumberIsTooSmallException,
                                        MaxCountExceededException, NoBracketingException {
        final TestProblem3 pb = new TestProblem3(0.1);
        final double minStep = 0;
        final double maxStep = pb.getFinalTime() - pb.getInitialTime();
        final double scalAbsoluteTolerance = 1.0e-8;
        final double scalRelativeTolerance = scalAbsoluteTolerance;
        final HighamHall54Integrator integ = new HighamHall54Integrator(minStep, maxStep,
            scalAbsoluteTolerance,
            scalRelativeTolerance);
        StepInterpolatorTestUtils.checkDerivativesConsistency(integ, pb, 1.1e-10);
    }

    @Test
    public void serialization()
                               throws IOException, ClassNotFoundException,
                               DimensionMismatchException, NumberIsTooSmallException,
                               MaxCountExceededException, NoBracketingException {

        final TestProblem3 pb = new TestProblem3(0.9);
        final double minStep = 0;
        final double maxStep = pb.getFinalTime() - pb.getInitialTime();
        final double scalAbsoluteTolerance = 1.0e-8;
        final double scalRelativeTolerance = scalAbsoluteTolerance;
        final HighamHall54Integrator integ = new HighamHall54Integrator(minStep, maxStep,
            scalAbsoluteTolerance,
            scalRelativeTolerance);
        integ.addStepHandler(new ContinuousOutputModel());
        integ.integrate(pb,
            pb.getInitialTime(), pb.getInitialState(),
            pb.getFinalTime(), new double[pb.getDimension()]);

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(bos);
        for (final StepHandler handler : integ.getStepHandlers()) {
            oos.writeObject(handler);
        }

        Assert.assertTrue(bos.size() > 185000);
        Assert.assertTrue(bos.size() < 195000);

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

        Assert.assertTrue(maxError < 1.6e-10);

    }

    @Test
    public void checkClone()
                            throws DimensionMismatchException, NumberIsTooSmallException,
                            MaxCountExceededException, NoBracketingException {
        final TestProblem3 pb = new TestProblem3(0.9);
        final double minStep = 0;
        final double maxStep = pb.getFinalTime() - pb.getInitialTime();
        final double scalAbsoluteTolerance = 1.0e-8;
        final double scalRelativeTolerance = scalAbsoluteTolerance;
        final HighamHall54Integrator integ = new HighamHall54Integrator(minStep, maxStep,
            scalAbsoluteTolerance,
            scalRelativeTolerance);
        integ.addStepHandler(new StepHandler(){
            @Override
            public
                    void
                    handleStep(final StepInterpolator interpolator, final boolean isLast)
                                                                                         throws MaxCountExceededException {
                final StepInterpolator cloned = interpolator.copy();
                final double tA = cloned.getPreviousTime();
                final double tB = cloned.getCurrentTime();
                final double halfStep = MathLib.abs(tB - tA) / 2;
                Assert.assertEquals(interpolator.getPreviousTime(), tA, 1.0e-12);
                Assert.assertEquals(interpolator.getCurrentTime(), tB, 1.0e-12);
                for (int i = 0; i < 10; ++i) {
                    final double t = (i * tB + (9 - i) * tA) / 9;
                    interpolator.setInterpolatedTime(t);
                    Assert.assertTrue(MathLib.abs(cloned.getInterpolatedTime() - t) > (halfStep / 10));
                    cloned.setInterpolatedTime(t);
                    Assert.assertEquals(t, cloned.getInterpolatedTime(), 1.0e-12);
                    final double[] referenceState = interpolator.getInterpolatedState();
                    final double[] cloneState = cloned.getInterpolatedState();
                    for (int j = 0; j < referenceState.length; ++j) {
                        Assert.assertEquals(referenceState[j], cloneState[j], 1.0e-12);
                    }
                }
            }

            @Override
            public void init(final double t0, final double[] y0, final double t) {
            }
        });
        integ.integrate(pb,
            pb.getInitialTime(), pb.getInitialState(),
            pb.getFinalTime(), new double[pb.getDimension()]);

    }

}
