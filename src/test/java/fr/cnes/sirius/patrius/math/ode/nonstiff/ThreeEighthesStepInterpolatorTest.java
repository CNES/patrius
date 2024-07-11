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
import fr.cnes.sirius.patrius.math.ode.sampling.StepInterpolatorTestUtils;

public class ThreeEighthesStepInterpolatorTest {

    @Test
    public void derivativesConsistency()
                                        throws DimensionMismatchException, NumberIsTooSmallException,
                                        MaxCountExceededException, NoBracketingException {
        final TestProblem3 pb = new TestProblem3();
        final double step = (pb.getFinalTime() - pb.getInitialTime()) * 0.001;
        final ThreeEighthesIntegrator integ = new ThreeEighthesIntegrator(step);
        StepInterpolatorTestUtils.checkDerivativesConsistency(integ, pb, 1.0e-10);
    }

    @Test
    public void serialization()
                               throws IOException, ClassNotFoundException,
                               DimensionMismatchException, NumberIsTooSmallException,
                               MaxCountExceededException, NoBracketingException {

        final TestProblem3 pb = new TestProblem3(0.9);
        final double step = (pb.getFinalTime() - pb.getInitialTime()) * 0.0003;
        final ThreeEighthesIntegrator integ = new ThreeEighthesIntegrator(step);
        integ.addStepHandler(new ContinuousOutputModel());
        integ.integrate(pb,
            pb.getInitialTime(), pb.getInitialState(),
            pb.getFinalTime(), new double[pb.getDimension()]);

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(bos);
        for (final StepHandler handler : integ.getStepHandlers()) {
            oos.writeObject(handler);
        }

        Assert.assertTrue(bos.size() > 880000);
        Assert.assertTrue(bos.size() < 900000);

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

        Assert.assertTrue(maxError > 0.005);

    }

}
