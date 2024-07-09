/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 */
package fr.cnes.sirius.patrius.math.optim.nonlinear.vector;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.analysis.MultivariateMatrixFunction;
import fr.cnes.sirius.patrius.math.analysis.MultivariateVectorFunction;
import fr.cnes.sirius.patrius.math.linear.BlockRealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.optim.InitialGuess;
import fr.cnes.sirius.patrius.math.optim.MaxEval;
import fr.cnes.sirius.patrius.math.optim.PointVectorValuePair;
import fr.cnes.sirius.patrius.math.optim.SimpleVectorValueChecker;
import fr.cnes.sirius.patrius.math.optim.nonlinear.vector.jacobian.GaussNewtonOptimizer;
import fr.cnes.sirius.patrius.math.random.GaussianRandomGenerator;
import fr.cnes.sirius.patrius.math.random.JDKRandomGenerator;
import fr.cnes.sirius.patrius.math.random.RandomVectorGenerator;
import fr.cnes.sirius.patrius.math.random.UncorrelatedRandomVectorGenerator;

/**
 * <p>
 * Some of the unit tests are re-implementations of the MINPACK <a
 * href="http://www.netlib.org/minpack/ex/file17">file17</a> and <a
 * href="http://www.netlib.org/minpack/ex/file22">file22</a> test files. The redistribution policy for MINPACK is
 * available <a href="http://www.netlib.org/minpack/disclaimer">here</a>, for convenience, it is reproduced below.
 * </p>
 * 
 * <table border="0" width="80%" cellpadding="10" align="center" bgcolor="#E0E0E0">
 * <tr>
 * <td>
 * Minpack Copyright Notice (1999) University of Chicago. All rights reserved</td>
 * </tr>
 * <tr>
 * <td>
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 * <ol>
 * <li>Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer.</li>
 * <li>Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials provided with the distribution.</li>
 * <li>The end-user documentation included with the redistribution, if any, must include the following acknowledgment:
 * <code>This product includes software developed by the University of
 *           Chicago, as Operator of Argonne National Laboratory.</code> Alternately, this acknowledgment may appear in
 * the software itself, if and wherever such third-party acknowledgments normally appear.</li>
 * <li><strong>WARRANTY DISCLAIMER. THE SOFTWARE IS SUPPLIED "AS IS" WITHOUT WARRANTY OF ANY KIND. THE COPYRIGHT HOLDER,
 * THE UNITED STATES, THE UNITED STATES DEPARTMENT OF ENERGY, AND THEIR EMPLOYEES: (1) DISCLAIM ANY WARRANTIES, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE,
 * TITLE OR NON-INFRINGEMENT, (2) DO NOT ASSUME ANY LEGAL LIABILITY OR RESPONSIBILITY FOR THE ACCURACY, COMPLETENESS, OR
 * USEFULNESS OF THE SOFTWARE, (3) DO NOT REPRESENT THAT USE OF THE SOFTWARE WOULD NOT INFRINGE PRIVATELY OWNED RIGHTS,
 * (4) DO NOT WARRANT THAT THE SOFTWARE WILL FUNCTION UNINTERRUPTED, THAT IT IS ERROR-FREE OR THAT ANY ERRORS WILL BE
 * CORRECTED.</strong></li>
 * <li><strong>LIMITATION OF LIABILITY. IN NO EVENT WILL THE COPYRIGHT HOLDER, THE UNITED STATES, THE UNITED STATES
 * DEPARTMENT OF ENERGY, OR THEIR EMPLOYEES: BE LIABLE FOR ANY INDIRECT, INCIDENTAL, CONSEQUENTIAL, SPECIAL OR PUNITIVE
 * DAMAGES OF ANY KIND OR NATURE, INCLUDING BUT NOT LIMITED TO LOSS OF PROFITS OR LOSS OF DATA, FOR ANY REASON
 * WHATSOEVER, WHETHER SUCH LIABILITY IS ASSERTED ON THE BASIS OF CONTRACT, TORT (INCLUDING NEGLIGENCE OR STRICT
 * LIABILITY), OR OTHERWISE, EVEN IF ANY OF SAID PARTIES HAS BEEN WARNED OF THE POSSIBILITY OF SUCH LOSS OR
 * DAMAGES.</strong></li>
 * <ol></td>
 * </tr>
 * </table>
 * 
 * @author Argonne National Laboratory. MINPACK project. March 1980 (original fortran minpack tests)
 * @author Burton S. Garbow (original fortran minpack tests)
 * @author Kenneth E. Hillstrom (original fortran minpack tests)
 * @author Jorge J. More (original fortran minpack tests)
 * @author Luc Maisonobe (non-minpack tests and minpack tests Java translation)
 */
public class MultiStartMultivariateVectorOptimizerTest {
    @Test(expected = NullPointerException.class)
    public void testGetOptimaBeforeOptimize() {
        new LinearProblem(new double[][] { { 2 } }, new double[] { 3 });
        final JacobianMultivariateVectorOptimizer underlyingOptimizer = new GaussNewtonOptimizer(true,
            new SimpleVectorValueChecker(1e-6, 1e-6));
        final JDKRandomGenerator g = new JDKRandomGenerator();
        g.setSeed(16069223052l);
        final RandomVectorGenerator generator =
            new UncorrelatedRandomVectorGenerator(1, new GaussianRandomGenerator(g));
        final MultiStartMultivariateVectorOptimizer optimizer = new MultiStartMultivariateVectorOptimizer(
            underlyingOptimizer, 10, generator);

        optimizer.getOptima();
    }

    @Test
    public void testTrivial() {
        final LinearProblem problem = new LinearProblem(new double[][] { { 2 } }, new double[] { 3 });
        final JacobianMultivariateVectorOptimizer underlyingOptimizer = new GaussNewtonOptimizer(true,
            new SimpleVectorValueChecker(1e-6, 1e-6));
        final JDKRandomGenerator g = new JDKRandomGenerator();
        g.setSeed(16069223052l);
        final RandomVectorGenerator generator =
            new UncorrelatedRandomVectorGenerator(1, new GaussianRandomGenerator(g));
        final MultiStartMultivariateVectorOptimizer optimizer = new MultiStartMultivariateVectorOptimizer(
            underlyingOptimizer, 10, generator);

        final PointVectorValuePair optimum = optimizer.optimize(new MaxEval(100),
            problem.getModelFunction(),
            problem.getModelFunctionJacobian(),
            problem.getTarget(),
            new Weight(new double[] { 1 }),
            new InitialGuess(new double[] { 0 }));
        Assert.assertEquals(1.5, optimum.getPoint()[0], 1e-10);
        Assert.assertEquals(3.0, optimum.getValue()[0], 1e-10);
        final PointVectorValuePair[] optima = optimizer.getOptima();
        Assert.assertEquals(10, optima.length);
        for (final PointVectorValuePair element : optima) {
            Assert.assertEquals(1.5, element.getPoint()[0], 1e-10);
            Assert.assertEquals(3.0, element.getValue()[0], 1e-10);
        }
        Assert.assertTrue(optimizer.getEvaluations() > 20);
        Assert.assertTrue(optimizer.getEvaluations() < 50);
        Assert.assertEquals(100, optimizer.getMaxEvaluations());
    }

    /**
     * Test demonstrating that the user exception is fnally thrown if none
     * of the runs succeed.
     */
    @Test(expected = TestException.class)
    public void testNoOptimum() {
        final JacobianMultivariateVectorOptimizer underlyingOptimizer = new GaussNewtonOptimizer(true,
            new SimpleVectorValueChecker(1e-6, 1e-6));
        final JDKRandomGenerator g = new JDKRandomGenerator();
        g.setSeed(12373523445l);
        final RandomVectorGenerator generator =
            new UncorrelatedRandomVectorGenerator(1, new GaussianRandomGenerator(g));
        final MultiStartMultivariateVectorOptimizer optimizer = new MultiStartMultivariateVectorOptimizer(
            underlyingOptimizer, 10, generator);
        optimizer.optimize(new MaxEval(100),
            new Target(new double[] { 0 }),
            new Weight(new double[] { 1 }),
            new InitialGuess(new double[] { 0 }),
            new ModelFunction(new MultivariateVectorFunction(){
                @Override
                public double[] value(final double[] point) {
                    throw new TestException();
                }
            }));
    }

    private static class TestException extends RuntimeException {
    }

    private static class LinearProblem {
        private final RealMatrix factors;
        private final double[] target;

        public LinearProblem(final double[][] factors,
            final double[] target) {
            this.factors = new BlockRealMatrix(factors);
            this.target = target;
        }

        public Target getTarget() {
            return new Target(this.target);
        }

        public ModelFunction getModelFunction() {
            return new ModelFunction(new MultivariateVectorFunction(){
                @Override
                public double[] value(final double[] variables) {
                    return LinearProblem.this.factors.operate(variables);
                }
            });
        }

        public ModelFunctionJacobian getModelFunctionJacobian() {
            return new ModelFunctionJacobian(new MultivariateMatrixFunction(){
                @Override
                public double[][] value(final double[] point) {
                    return LinearProblem.this.factors.getData();
                }
            });
        }
    }
}
