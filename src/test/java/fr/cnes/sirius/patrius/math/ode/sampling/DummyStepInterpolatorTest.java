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
package fr.cnes.sirius.patrius.math.ode.sampling;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;
import fr.cnes.sirius.patrius.math.util.MathLib;

public class DummyStepInterpolatorTest {

    @Test
    public void testNoReset() throws MaxCountExceededException {

        final double[] y = { 0.0, 1.0, -2.0 };
        final DummyStepInterpolator interpolator = new DummyStepInterpolator(y, new double[y.length], true);
        interpolator.storeTime(0);
        interpolator.shift();
        interpolator.storeTime(1);

        final double[] result = interpolator.getInterpolatedState();
        for (int i = 0; i < result.length; ++i) {
            Assert.assertTrue(MathLib.abs(result[i] - y[i]) < 1.0e-10);
        }

    }

    @Test
    public void testFixedState() throws MaxCountExceededException {

        final double[] y = { 1.0, 3.0, -4.0 };
        final DummyStepInterpolator interpolator = new DummyStepInterpolator(y, new double[y.length], true);
        interpolator.storeTime(0);
        interpolator.shift();
        interpolator.storeTime(1);

        interpolator.setInterpolatedTime(0.1);
        double[] result = interpolator.getInterpolatedState();
        for (int i = 0; i < result.length; ++i) {
            Assert.assertTrue(MathLib.abs(result[i] - y[i]) < 1.0e-10);
        }

        interpolator.setInterpolatedTime(0.5);
        result = interpolator.getInterpolatedState();
        for (int i = 0; i < result.length; ++i) {
            Assert.assertTrue(MathLib.abs(result[i] - y[i]) < 1.0e-10);
        }

    }

    @Test
    public void testSerialization()
                                   throws IOException, ClassNotFoundException, MaxCountExceededException {

        final double[] y = { 0.0, 1.0, -2.0 };
        final DummyStepInterpolator interpolator = new DummyStepInterpolator(y, new double[y.length], true);
        interpolator.storeTime(0);
        interpolator.shift();
        interpolator.storeTime(1);

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(interpolator);

        Assert.assertTrue(bos.size() > 300);
        Assert.assertTrue(bos.size() < 500);

        final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        final ObjectInputStream ois = new ObjectInputStream(bis);
        final DummyStepInterpolator dsi = (DummyStepInterpolator) ois.readObject();

        dsi.setInterpolatedTime(0.5);
        final double[] result = dsi.getInterpolatedState();
        for (int i = 0; i < result.length; ++i) {
            Assert.assertTrue(MathLib.abs(result[i] - y[i]) < 1.0e-10);
        }

    }

    @Test
    public void testImpossibleSerialization()
                                             throws IOException {

        final double[] y = { 0.0, 1.0, -2.0 };
        final AbstractStepInterpolator interpolator = new BadStepInterpolator(y, true);
        interpolator.storeTime(0);
        interpolator.shift();
        interpolator.storeTime(1);

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(bos);
        try {
            oos.writeObject(interpolator);
            Assert.fail("an exception should have been thrown");
        } catch (final LocalException le) {
            // expected behavior
            Assert.assertTrue(true);
        }
    }

    private static class BadStepInterpolator extends DummyStepInterpolator {
        @SuppressWarnings("unused")
        public BadStepInterpolator() {
        }

        public BadStepInterpolator(final double[] y, final boolean forward) {
            super(y, new double[y.length], forward);
        }

        @Override
        protected void doFinalize() {
            throw new LocalException();
        }
    }

    private static class LocalException extends RuntimeException {

        /** Serializable UID. */
        private static final long serialVersionUID = 1573688720575606366L;
    }
}
