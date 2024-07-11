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
package fr.cnes.sirius.patrius.math.ode.sampling;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;
import fr.cnes.sirius.patrius.math.exception.NoBracketingException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.TestProblem3;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince54Integrator;
import fr.cnes.sirius.patrius.math.util.MathLib;

public class StepNormalizerTest {

    public StepNormalizerTest() {
        this.pb = null;
        this.integ = null;
    }

    @Test
    public void testBoundaries()
                                throws DimensionMismatchException, NumberIsTooSmallException,
                                MaxCountExceededException, NoBracketingException {
        final double range = this.pb.getFinalTime() - this.pb.getInitialTime();
        this.setLastSeen(false);
        this.integ.addStepHandler(new StepNormalizer(range / 10.0,
            new FixedStepHandler(){
                private boolean firstCall = true;

                @Override
                public void init(final double t0, final double[] y0, final double t) {
                }

                @Override
                public void handleStep(final double t,
                                       final double[] y,
                                       final double[] yDot,
                                       final boolean isLast) {
                    if (this.firstCall) {
                        StepNormalizerTest.this.checkValue(t, StepNormalizerTest.this.pb.getInitialTime());
                        this.firstCall = false;
                    }
                    if (isLast) {
                        StepNormalizerTest.this.setLastSeen(true);
                        StepNormalizerTest.this.checkValue(t, StepNormalizerTest.this.pb.getFinalTime());
                    }
                }
            }));
        this.integ.integrate(this.pb,
            this.pb.getInitialTime(), this.pb.getInitialState(),
            this.pb.getFinalTime(), new double[this.pb.getDimension()]);
        Assert.assertTrue(this.lastSeen);
    }

    @Test
    public void testBeforeEnd()
                               throws DimensionMismatchException, NumberIsTooSmallException,
                               MaxCountExceededException, NoBracketingException {
        final double range = this.pb.getFinalTime() - this.pb.getInitialTime();
        this.setLastSeen(false);
        this.integ.addStepHandler(new StepNormalizer(range / 10.5,
            new FixedStepHandler(){
                @Override
                public void init(final double t0, final double[] y0, final double t) {
                }

                @Override
                public void handleStep(final double t,
                                       final double[] y,
                                       final double[] yDot,
                                       final boolean isLast) {
                    if (isLast) {
                        StepNormalizerTest.this.setLastSeen(true);
                        StepNormalizerTest.this.checkValue(t,
                            StepNormalizerTest.this.pb.getFinalTime() - range / 21.0);
                    }
                }
            }));
        this.integ.integrate(this.pb,
            this.pb.getInitialTime(), this.pb.getInitialState(),
            this.pb.getFinalTime(), new double[this.pb.getDimension()]);
        Assert.assertTrue(this.lastSeen);
    }

    public void checkValue(final double value, final double reference) {
        Assert.assertTrue(MathLib.abs(value - reference) < 1.0e-10);
    }

    public void setLastSeen(final boolean lastSeen) {
        this.lastSeen = lastSeen;
    }

    @Before
    public void setUp() {
        this.pb = new TestProblem3(0.9);
        final double minStep = 0;
        final double maxStep = this.pb.getFinalTime() - this.pb.getInitialTime();
        this.integ = new DormandPrince54Integrator(minStep, maxStep, 10.e-8, 1.0e-8);
        this.lastSeen = false;
    }

    @After
    public void tearDown() {
        this.pb = null;
        this.integ = null;
    }

    TestProblem3 pb;
    FirstOrderIntegrator integ;
    boolean lastSeen;

}
