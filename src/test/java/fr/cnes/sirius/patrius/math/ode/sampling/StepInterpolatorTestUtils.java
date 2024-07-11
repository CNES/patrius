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

import org.junit.Assert;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;
import fr.cnes.sirius.patrius.math.exception.NoBracketingException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.TestProblemAbstract;
import fr.cnes.sirius.patrius.math.util.MathLib;

public class StepInterpolatorTestUtils {

    public static void checkDerivativesConsistency(final FirstOrderIntegrator integrator,
                                                   final TestProblemAbstract problem,
                                                   final double threshold)
                                                                          throws DimensionMismatchException,
                                                                          NumberIsTooSmallException,
                                                                          MaxCountExceededException,
                                                                          NoBracketingException {
        integrator.addStepHandler(new StepHandler(){

            @Override
            public
                    void
                    handleStep(final StepInterpolator interpolator, final boolean isLast)
                                                                                         throws MaxCountExceededException {

                final double h = 0.001 * (interpolator.getCurrentTime() - interpolator.getPreviousTime());
                final double t = interpolator.getCurrentTime() - 300 * h;

                if (MathLib.abs(h) < 10 * MathLib.ulp(t)) {
                    return;
                }

                interpolator.setInterpolatedTime(t - 4 * h);
                final double[] yM4h = interpolator.getInterpolatedState().clone();
                interpolator.setInterpolatedTime(t - 3 * h);
                final double[] yM3h = interpolator.getInterpolatedState().clone();
                interpolator.setInterpolatedTime(t - 2 * h);
                final double[] yM2h = interpolator.getInterpolatedState().clone();
                interpolator.setInterpolatedTime(t - h);
                final double[] yM1h = interpolator.getInterpolatedState().clone();
                interpolator.setInterpolatedTime(t + h);
                final double[] yP1h = interpolator.getInterpolatedState().clone();
                interpolator.setInterpolatedTime(t + 2 * h);
                final double[] yP2h = interpolator.getInterpolatedState().clone();
                interpolator.setInterpolatedTime(t + 3 * h);
                final double[] yP3h = interpolator.getInterpolatedState().clone();
                interpolator.setInterpolatedTime(t + 4 * h);
                final double[] yP4h = interpolator.getInterpolatedState().clone();

                interpolator.setInterpolatedTime(t);
                final double[] yDot = interpolator.getInterpolatedDerivatives();

                for (int i = 0; i < yDot.length; ++i) {
                    final double approYDot = (-3 * (yP4h[i] - yM4h[i]) +
                        32 * (yP3h[i] - yM3h[i]) +
                        -168 * (yP2h[i] - yM2h[i]) +
                        672 * (yP1h[i] - yM1h[i])) / (840 * h);
                    Assert.assertEquals(approYDot, yDot[i], threshold);
                }

            }

            @Override
            public void init(final double t0, final double[] y0, final double t) {
            }

        });

        integrator.integrate(problem,
            problem.getInitialTime(), problem.getInitialState(),
            problem.getFinalTime(), new double[problem.getDimension()]);

    }
}
