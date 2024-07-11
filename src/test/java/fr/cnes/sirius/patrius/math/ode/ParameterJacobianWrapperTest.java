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
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION::FA:306:21/11/2014: coverage
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.ode;

import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * Test class for ParameterJacobianWrapper, created for coverage in PATRIUS V2.4
 * 
 * @version $Id: ParameterJacobianWrapperTest.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.4
 * 
 */
public class ParameterJacobianWrapperTest {

    /**
     * For coverage purposes, tests the else of if (pode.isSupported(paramName))
     * in method computeParameterJacobian.
     */
    @Test
    public void testComputeParameterJacobian() {
        final double b = 2.88;
        final String pName = ParamBrusselator.B;
        final MainStateJacobianProvider jode = new Brusselator(b);
        final ParameterizedODE pode = new ParamBrusselator(b);
        pode.setParameter(pName, b);
        final ParameterConfiguration[] selectedParameters = { new ParameterConfiguration(pName, Double.NaN) };

        final ParameterJacobianWrapper wrapper = new ParameterJacobianWrapper(jode, pode, selectedParameters);

        // tests method getParametersNames in the same time
        final Collection<String> list = wrapper.getParametersNames();
        Assert.assertEquals(list.size(), 1);
        Assert.assertTrue(list.contains(pName));

        final double t = 0;
        final double[] y = { 14, 7 };
        final double[] yDot = { 0, 0 };
        final String paramName = "pName2";
        Assert.assertFalse(list.contains(paramName));
        final double[] dFdP = y;
        wrapper.computeParameterJacobian(t, y, yDot, paramName, dFdP);
        Assert.assertArrayEquals(yDot, dFdP, Precision.DOUBLE_COMPARISON_EPSILON);

    }

    // -----------------------------------------------------------------------
    private static class Brusselator extends AbstractParameterizable
        implements MainStateJacobianProvider, ParameterJacobianProvider {

        public static final String B = "b";

        private final double b;

        public Brusselator(final double b) {
            super(B);
            this.b = b;
        }

        @Override
        public int getDimension() {
            return 2;
        }

        @Override
        public void computeDerivatives(final double t, final double[] y, final double[] yDot) {
            final double prod = y[0] * y[0] * y[1];
            yDot[0] = 1 + prod - (this.b + 1) * y[0];
            yDot[1] = this.b * y[0] - prod;
        }

        @Override
        public void computeMainStateJacobian(final double t, final double[] y, final double[] yDot,
                                             final double[][] dFdY) {
            final double p = 2 * y[0] * y[1];
            final double y02 = y[0] * y[0];
            dFdY[0][0] = p - (1 + this.b);
            dFdY[0][1] = y02;
            dFdY[1][0] = this.b - p;
            dFdY[1][1] = -y02;
        }

        @Override
        public void computeParameterJacobian(final double t, final double[] y, final double[] yDot,
                                             final String paramName, final double[] dFdP) {
            if (this.isSupported(paramName)) {
                dFdP[0] = -y[0];
                dFdP[1] = y[0];
            } else {
                dFdP[0] = 0;
                dFdP[1] = 0;
            }
        }

    }

    private static class ParamBrusselator extends AbstractParameterizable
        implements FirstOrderDifferentialEquations, ParameterizedODE {

        public static final String B = "b";

        private double b;

        public ParamBrusselator(final double b) {
            super(B);
            this.b = b;
        }

        @Override
        public int getDimension() {
            return 2;
        }

        /** {@inheritDoc} */
        @Override
        public double getParameter(final String name)
                                                     throws UnknownParameterException {
            this.complainIfNotSupported(name);
            return this.b;
        }

        /** {@inheritDoc} */
        @Override
        public void setParameter(final String name, final double value)
                                                                       throws UnknownParameterException {
            this.complainIfNotSupported(name);
            this.b = value;
        }

        @Override
        public void computeDerivatives(final double t, final double[] y, final double[] yDot) {
            final double prod = y[0] * y[0] * y[1];
            yDot[0] = 1 + prod - (this.b + 1) * y[0];
            yDot[1] = this.b * y[0] - prod;
        }

    }

}
