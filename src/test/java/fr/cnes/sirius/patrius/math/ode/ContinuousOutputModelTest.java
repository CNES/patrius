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

import java.util.Random;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;
import fr.cnes.sirius.patrius.math.exception.NoBracketingException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince54Integrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.ode.sampling.DummyStepInterpolator;
import fr.cnes.sirius.patrius.math.ode.sampling.StepInterpolator;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;

public class ContinuousOutputModelTest {

    public ContinuousOutputModelTest() {
        this.pb = null;
        this.integ = null;
    }

    @Test
    public void testBoundaries() throws DimensionMismatchException, NumberIsTooSmallException,
                                MaxCountExceededException, NoBracketingException {
        this.integ.addStepHandler(new ContinuousOutputModel());
        this.integ.integrate(this.pb,
            this.pb.getInitialTime(), this.pb.getInitialState(),
            this.pb.getFinalTime(), new double[this.pb.getDimension()]);
        final ContinuousOutputModel cm = (ContinuousOutputModel) this.integ.getStepHandlers().iterator().next();
        cm.setInterpolatedTime(2.0 * this.pb.getInitialTime() - this.pb.getFinalTime());
        cm.setInterpolatedTime(2.0 * this.pb.getFinalTime() - this.pb.getInitialTime());
        cm.setInterpolatedTime(0.5 * (this.pb.getFinalTime() + this.pb.getInitialTime()));
    }

    @Test
    public void testRandomAccess() throws DimensionMismatchException, NumberIsTooSmallException,
                                  MaxCountExceededException, NoBracketingException {

        final ContinuousOutputModel cm = new ContinuousOutputModel();
        this.integ.addStepHandler(cm);
        this.integ.integrate(this.pb,
            this.pb.getInitialTime(), this.pb.getInitialState(),
            this.pb.getFinalTime(), new double[this.pb.getDimension()]);

        final Random random = new Random(347588535632l);
        double maxError = 0.0;
        for (int i = 0; i < 1000; ++i) {
            final double r = random.nextDouble();
            final double time = r * this.pb.getInitialTime() + (1.0 - r) * this.pb.getFinalTime();
            cm.setInterpolatedTime(time);
            final double[] interpolatedY = cm.getInterpolatedState();
            final double[] theoreticalY = this.pb.computeTheoreticalState(time);
            final double dx = interpolatedY[0] - theoreticalY[0];
            final double dy = interpolatedY[1] - theoreticalY[1];
            final double error = dx * dx + dy * dy;
            if (error > maxError) {
                maxError = error;
            }
        }

        Assert.assertTrue(maxError < 1.0e-9);

    }

    @Test
    public void testModelsMerging() throws MaxCountExceededException, MathIllegalArgumentException {

        // theoretical solution: y[0] = cos(t), y[1] = sin(t)
        final FirstOrderDifferentialEquations problem =
            new FirstOrderDifferentialEquations(){
                @Override
                public void computeDerivatives(final double t, final double[] y, final double[] dot) {
                    dot[0] = -y[1];
                    dot[1] = y[0];
                }

                @Override
                public int getDimension() {
                    return 2;
                }
            };

        // integrate backward from &pi; to 0;
        final ContinuousOutputModel cm1 = new ContinuousOutputModel();
        final FirstOrderIntegrator integ1 =
            new DormandPrince853Integrator(0, 1.0, 1.0e-8, 1.0e-8);
        integ1.addStepHandler(cm1);
        integ1.integrate(problem, FastMath.PI, new double[] { -1.0, 0.0 },
            0, new double[2]);

        // integrate backward from 2&pi; to &pi;
        final ContinuousOutputModel cm2 = new ContinuousOutputModel();
        final FirstOrderIntegrator integ2 =
            new DormandPrince853Integrator(0, 0.1, 1.0e-12, 1.0e-12);
        integ2.addStepHandler(cm2);
        integ2.integrate(problem, 2.0 * FastMath.PI, new double[] { 1.0, 0.0 },
            FastMath.PI, new double[2]);

        // merge the two half circles
        final ContinuousOutputModel cm = new ContinuousOutputModel();
        cm.append(cm2);
        cm.append(new ContinuousOutputModel());
        cm.append(cm1);

        // check circle
        Assert.assertEquals(2.0 * FastMath.PI, cm.getInitialTime(), 1.0e-12);
        Assert.assertEquals(0, cm.getFinalTime(), 1.0e-12);
        Assert.assertEquals(cm.getFinalTime(), cm.getInterpolatedTime(), 1.0e-12);
        for (double t = 0; t < 2.0 * FastMath.PI; t += 0.1) {
            cm.setInterpolatedTime(t);
            final double[] y = cm.getInterpolatedState();
            Assert.assertEquals(MathLib.cos(t), y[0], 1.0e-7);
            Assert.assertEquals(MathLib.sin(t), y[1], 1.0e-7);
        }

    }

    @Test
    public void testErrorConditions() throws MaxCountExceededException, MathIllegalArgumentException {

        final ContinuousOutputModel cm = new ContinuousOutputModel();
        cm.handleStep(this.buildInterpolator(0, new double[] { 0.0, 1.0, -2.0 }, 1), true);

        // dimension mismatch
        Assert.assertTrue(this.checkAppendError(cm, 1.0, new double[] { 0.0, 1.0 }, 2.0));

        // hole between time ranges
        Assert.assertTrue(this.checkAppendError(cm, 10.0, new double[] { 0.0, 1.0, -2.0 }, 20.0));

        // propagation direction mismatch
        Assert.assertTrue(this.checkAppendError(cm, 1.0, new double[] { 0.0, 1.0, -2.0 }, 0.0));

        // no errors
        Assert.assertFalse(this.checkAppendError(cm, 1.0, new double[] { 0.0, 1.0, -2.0 }, 2.0));

    }

    private boolean
            checkAppendError(final ContinuousOutputModel cm,
                             final double t0, final double[] y0, final double t1)
                                                                                 throws MaxCountExceededException,
                                                                                 MathIllegalArgumentException {
        try {
            final ContinuousOutputModel otherCm = new ContinuousOutputModel();
            otherCm.handleStep(this.buildInterpolator(t0, y0, t1), true);
            cm.append(otherCm);
        } catch (final IllegalArgumentException iae) {
            return true; // there was an allowable error
        }
        return false; // no allowable error
    }

    private StepInterpolator buildInterpolator(final double t0, final double[] y0, final double t1) {
        final DummyStepInterpolator interpolator = new DummyStepInterpolator(y0, new double[y0.length], t1 >= t0);
        interpolator.storeTime(t0);
        interpolator.shift();
        interpolator.storeTime(t1);
        return interpolator;
    }

    public void checkValue(final double value, final double reference) {
        Assert.assertTrue(MathLib.abs(value - reference) < 1.0e-10);
    }

    @Before
    public void setUp() {
        this.pb = new TestProblem3(0.9);
        final double minStep = 0;
        final double maxStep = this.pb.getFinalTime() - this.pb.getInitialTime();
        this.integ = new DormandPrince54Integrator(minStep, maxStep, 1.0e-8, 1.0e-8);
    }

    @After
    public void tearDown() {
        this.pb = null;
        this.integ = null;
    }

    TestProblem3 pb;
    FirstOrderIntegrator integ;

}
