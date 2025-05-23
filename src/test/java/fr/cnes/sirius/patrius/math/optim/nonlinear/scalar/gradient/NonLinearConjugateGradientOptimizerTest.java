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
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.gradient;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.analysis.MultivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.MultivariateVectorFunction;
import fr.cnes.sirius.patrius.math.analysis.solver.BrentSolver;
import fr.cnes.sirius.patrius.math.geometry.euclidean.twod.Vector2D;
import fr.cnes.sirius.patrius.math.linear.BlockRealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.optim.InitialGuess;
import fr.cnes.sirius.patrius.math.optim.MaxEval;
import fr.cnes.sirius.patrius.math.optim.PointValuePair;
import fr.cnes.sirius.patrius.math.optim.SimpleValueChecker;
import fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.GoalType;
import fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.ObjectiveFunction;
import fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.ObjectiveFunctionGradient;

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
public class NonLinearConjugateGradientOptimizerTest {
    @Test
    public void testTrivial() {
        final LinearProblem problem = new LinearProblem(new double[][] { { 2 } }, new double[] { 3 });
        final NonLinearConjugateGradientOptimizer optimizer = new NonLinearConjugateGradientOptimizer(
            NonLinearConjugateGradientOptimizer.Formula.POLAK_RIBIERE,
            new SimpleValueChecker(1e-6, 1e-6));
        final PointValuePair optimum = optimizer.optimize(new MaxEval(100),
            problem.getObjectiveFunction(),
            problem.getObjectiveFunctionGradient(),
            GoalType.MINIMIZE,
            new InitialGuess(new double[] { 0 }));
        Assert.assertEquals(1.5, optimum.getPoint()[0], 1.0e-10);
        Assert.assertEquals(0.0, optimum.getValue(), 1.0e-10);
    }

    @Test
    public void testColumnsPermutation() {
        final LinearProblem problem = new LinearProblem(new double[][] { { 1.0, -1.0 }, { 0.0, 2.0 }, { 1.0, -2.0 } },
            new double[] { 4.0, 6.0, 1.0 });

        final NonLinearConjugateGradientOptimizer optimizer = new NonLinearConjugateGradientOptimizer(
            NonLinearConjugateGradientOptimizer.Formula.POLAK_RIBIERE,
            new SimpleValueChecker(1e-6, 1e-6));
        final PointValuePair optimum = optimizer.optimize(new MaxEval(100),
            problem.getObjectiveFunction(),
            problem.getObjectiveFunctionGradient(),
            GoalType.MINIMIZE,
            new InitialGuess(new double[] { 0, 0 }));
        Assert.assertEquals(7.0, optimum.getPoint()[0], 1.0e-10);
        Assert.assertEquals(3.0, optimum.getPoint()[1], 1.0e-10);
        Assert.assertEquals(0.0, optimum.getValue(), 1.0e-10);

    }

    @Test
    public void testNoDependency() {
        final LinearProblem problem = new LinearProblem(new double[][] {
            { 2, 0, 0, 0, 0, 0 },
            { 0, 2, 0, 0, 0, 0 },
            { 0, 0, 2, 0, 0, 0 },
            { 0, 0, 0, 2, 0, 0 },
            { 0, 0, 0, 0, 2, 0 },
            { 0, 0, 0, 0, 0, 2 }
        }, new double[] { 0.0, 1.1, 2.2, 3.3, 4.4, 5.5 });
        final NonLinearConjugateGradientOptimizer optimizer = new NonLinearConjugateGradientOptimizer(
            NonLinearConjugateGradientOptimizer.Formula.POLAK_RIBIERE,
            new SimpleValueChecker(1e-6, 1e-6));
        final PointValuePair optimum = optimizer.optimize(new MaxEval(100),
            problem.getObjectiveFunction(),
            problem.getObjectiveFunctionGradient(),
            GoalType.MINIMIZE,
            new InitialGuess(new double[] { 0, 0, 0, 0, 0, 0 }));
        for (int i = 0; i < problem.target.length; ++i) {
            Assert.assertEquals(0.55 * i, optimum.getPoint()[i], 1.0e-10);
        }
    }

    @Test
    public void testOneSet() {
        final LinearProblem problem = new LinearProblem(new double[][] {
            { 1, 0, 0 },
            { -1, 1, 0 },
            { 0, -1, 1 }
        }, new double[] { 1, 1, 1 });
        final NonLinearConjugateGradientOptimizer optimizer = new NonLinearConjugateGradientOptimizer(
            NonLinearConjugateGradientOptimizer.Formula.POLAK_RIBIERE,
            new SimpleValueChecker(1e-6, 1e-6));
        final PointValuePair optimum = optimizer.optimize(new MaxEval(100),
            problem.getObjectiveFunction(),
            problem.getObjectiveFunctionGradient(),
            GoalType.MINIMIZE,
            new InitialGuess(new double[] { 0, 0, 0 }));
        Assert.assertEquals(1.0, optimum.getPoint()[0], 1.0e-10);
        Assert.assertEquals(2.0, optimum.getPoint()[1], 1.0e-10);
        Assert.assertEquals(3.0, optimum.getPoint()[2], 1.0e-10);

    }

    @Test
    public void testTwoSets() {
        final double epsilon = 1.0e-7;
        final LinearProblem problem = new LinearProblem(new double[][] {
            { 2, 1, 0, 4, 0, 0 },
            { -4, -2, 3, -7, 0, 0 },
            { 4, 1, -2, 8, 0, 0 },
            { 0, -3, -12, -1, 0, 0 },
            { 0, 0, 0, 0, epsilon, 1 },
            { 0, 0, 0, 0, 1, 1 }
        }, new double[] { 2, -9, 2, 2, 1 + epsilon * epsilon, 2 });

        final Preconditioner preconditioner = new Preconditioner(){
            @Override
            public double[] precondition(final double[] point, final double[] r) {
                final double[] d = r.clone();
                d[0] /= 72.0;
                d[1] /= 30.0;
                d[2] /= 314.0;
                d[3] /= 260.0;
                d[4] /= 2 * (1 + epsilon * epsilon);
                d[5] /= 4.0;
                return d;
            }
        };

        final NonLinearConjugateGradientOptimizer optimizer = new NonLinearConjugateGradientOptimizer(
            NonLinearConjugateGradientOptimizer.Formula.POLAK_RIBIERE,
            new SimpleValueChecker(1e-13, 1e-13),
            new BrentSolver(),
            preconditioner);

        final PointValuePair optimum = optimizer.optimize(new MaxEval(100),
            problem.getObjectiveFunction(),
            problem.getObjectiveFunctionGradient(),
            GoalType.MINIMIZE,
            new InitialGuess(new double[] { 0, 0, 0, 0, 0, 0 }));
        Assert.assertEquals(3.0, optimum.getPoint()[0], 1.0e-10);
        Assert.assertEquals(4.0, optimum.getPoint()[1], 1.0e-10);
        Assert.assertEquals(-1.0, optimum.getPoint()[2], 1.0e-10);
        Assert.assertEquals(-2.0, optimum.getPoint()[3], 1.0e-10);
        Assert.assertEquals(1.0 + epsilon, optimum.getPoint()[4], 1.0e-10);
        Assert.assertEquals(1.0 - epsilon, optimum.getPoint()[5], 1.0e-10);

    }

    @Test
    public void testNonInversible() {
        final LinearProblem problem = new LinearProblem(new double[][] {
            { 1, 2, -3 },
            { 2, 1, 3 },
            { -3, 0, -9 }
        }, new double[] { 1, 1, 1 });
        final NonLinearConjugateGradientOptimizer optimizer = new NonLinearConjugateGradientOptimizer(
            NonLinearConjugateGradientOptimizer.Formula.POLAK_RIBIERE,
            new SimpleValueChecker(1e-6, 1e-6));
        final PointValuePair optimum = optimizer.optimize(new MaxEval(100),
            problem.getObjectiveFunction(),
            problem.getObjectiveFunctionGradient(),
            GoalType.MINIMIZE,
            new InitialGuess(new double[] { 0, 0, 0 }));
        Assert.assertTrue(optimum.getValue() > 0.5);
    }

    @Test
    public void testIllConditioned() {
        final LinearProblem problem1 = new LinearProblem(new double[][] {
            { 10.0, 7.0, 8.0, 7.0 },
            { 7.0, 5.0, 6.0, 5.0 },
            { 8.0, 6.0, 10.0, 9.0 },
            { 7.0, 5.0, 9.0, 10.0 }
        }, new double[] { 32, 23, 33, 31 });
        final NonLinearConjugateGradientOptimizer optimizer = new NonLinearConjugateGradientOptimizer(
            NonLinearConjugateGradientOptimizer.Formula.POLAK_RIBIERE,
            new SimpleValueChecker(1e-13, 1e-13),
            new BrentSolver(1e-15, 1e-15));
        final PointValuePair optimum1 = optimizer.optimize(new MaxEval(200),
            problem1.getObjectiveFunction(),
            problem1.getObjectiveFunctionGradient(),
            GoalType.MINIMIZE,
            new InitialGuess(new double[] { 0, 1, 2, 3 }));
        Assert.assertEquals(1.0, optimum1.getPoint()[0], 1.0e-4);
        Assert.assertEquals(1.0, optimum1.getPoint()[1], 1.0e-4);
        Assert.assertEquals(1.0, optimum1.getPoint()[2], 1.0e-4);
        Assert.assertEquals(1.0, optimum1.getPoint()[3], 1.0e-4);

        final LinearProblem problem2 = new LinearProblem(new double[][] {
            { 10.00, 7.00, 8.10, 7.20 },
            { 7.08, 5.04, 6.00, 5.00 },
            { 8.00, 5.98, 9.89, 9.00 },
            { 6.99, 4.99, 9.00, 9.98 }
        }, new double[] { 32, 23, 33, 31 });
        final PointValuePair optimum2 = optimizer.optimize(new MaxEval(200),
            problem2.getObjectiveFunction(),
            problem2.getObjectiveFunctionGradient(),
            GoalType.MINIMIZE,
            new InitialGuess(new double[] { 0, 1, 2, 3 }));
        Assert.assertEquals(-81.0, optimum2.getPoint()[0], 1.0e-1);
        Assert.assertEquals(137.0, optimum2.getPoint()[1], 1.0e-1);
        Assert.assertEquals(-34.0, optimum2.getPoint()[2], 1.0e-1);
        Assert.assertEquals(22.0, optimum2.getPoint()[3], 1.0e-1);

    }

    @Test
    public void testMoreEstimatedParametersSimple() {
        final LinearProblem problem = new LinearProblem(new double[][] {
            { 3.0, 2.0, 0.0, 0.0 },
            { 0.0, 1.0, -1.0, 1.0 },
            { 2.0, 0.0, 1.0, 0.0 }
        }, new double[] { 7.0, 3.0, 5.0 });

        final NonLinearConjugateGradientOptimizer optimizer = new NonLinearConjugateGradientOptimizer(
            NonLinearConjugateGradientOptimizer.Formula.POLAK_RIBIERE,
            new SimpleValueChecker(1e-6, 1e-6));
        final PointValuePair optimum = optimizer.optimize(new MaxEval(100),
            problem.getObjectiveFunction(),
            problem.getObjectiveFunctionGradient(),
            GoalType.MINIMIZE,
            new InitialGuess(new double[] { 7, 6, 5, 4 }));
        Assert.assertEquals(0, optimum.getValue(), 1.0e-10);

    }

    @Test
    public void testMoreEstimatedParametersUnsorted() {
        final LinearProblem problem = new LinearProblem(new double[][] {
            { 1.0, 1.0, 0.0, 0.0, 0.0, 0.0 },
            { 0.0, 0.0, 1.0, 1.0, 1.0, 0.0 },
            { 0.0, 0.0, 0.0, 0.0, 1.0, -1.0 },
            { 0.0, 0.0, -1.0, 1.0, 0.0, 1.0 },
            { 0.0, 0.0, 0.0, -1.0, 1.0, 0.0 }
        }, new double[] { 3.0, 12.0, -1.0, 7.0, 1.0 });
        final NonLinearConjugateGradientOptimizer optimizer = new NonLinearConjugateGradientOptimizer(
            NonLinearConjugateGradientOptimizer.Formula.POLAK_RIBIERE,
            new SimpleValueChecker(1e-6, 1e-6));
        final PointValuePair optimum = optimizer.optimize(new MaxEval(100),
            problem.getObjectiveFunction(),
            problem.getObjectiveFunctionGradient(),
            GoalType.MINIMIZE,
            new InitialGuess(new double[] { 2, 2, 2, 2, 2, 2 }));
        Assert.assertEquals(0, optimum.getValue(), 1.0e-10);
    }

    @Test
    public void testRedundantEquations() {
        final LinearProblem problem = new LinearProblem(new double[][] {
            { 1.0, 1.0 },
            { 1.0, -1.0 },
            { 1.0, 3.0 }
        }, new double[] { 3.0, 1.0, 5.0 });

        final NonLinearConjugateGradientOptimizer optimizer = new NonLinearConjugateGradientOptimizer(
            NonLinearConjugateGradientOptimizer.Formula.POLAK_RIBIERE,
            new SimpleValueChecker(1e-6, 1e-6));
        final PointValuePair optimum = optimizer.optimize(new MaxEval(100),
            problem.getObjectiveFunction(),
            problem.getObjectiveFunctionGradient(),
            GoalType.MINIMIZE,
            new InitialGuess(new double[] { 1, 1 }));
        Assert.assertEquals(2.0, optimum.getPoint()[0], 1.0e-8);
        Assert.assertEquals(1.0, optimum.getPoint()[1], 1.0e-8);

    }

    @Test
    public void testInconsistentEquations() {
        final LinearProblem problem = new LinearProblem(new double[][] {
            { 1.0, 1.0 },
            { 1.0, -1.0 },
            { 1.0, 3.0 }
        }, new double[] { 3.0, 1.0, 4.0 });

        final NonLinearConjugateGradientOptimizer optimizer = new NonLinearConjugateGradientOptimizer(
            NonLinearConjugateGradientOptimizer.Formula.POLAK_RIBIERE,
            new SimpleValueChecker(1e-6, 1e-6));
        final PointValuePair optimum = optimizer.optimize(new MaxEval(100),
            problem.getObjectiveFunction(),
            problem.getObjectiveFunctionGradient(),
            GoalType.MINIMIZE,
            new InitialGuess(new double[] { 1, 1 }));
        Assert.assertTrue(optimum.getValue() > 0.1);

    }

    @Test
    public void testCircleFitting() {
        final CircleScalar problem = new CircleScalar();
        problem.addPoint(30.0, 68.0);
        problem.addPoint(50.0, -6.0);
        problem.addPoint(110.0, -20.0);
        problem.addPoint(35.0, 15.0);
        problem.addPoint(45.0, 97.0);
        final NonLinearConjugateGradientOptimizer optimizer = new NonLinearConjugateGradientOptimizer(
            NonLinearConjugateGradientOptimizer.Formula.POLAK_RIBIERE,
            new SimpleValueChecker(1e-30, 1e-30),
            new BrentSolver(1e-15, 1e-13));
        final PointValuePair optimum = optimizer.optimize(new MaxEval(100),
            problem.getObjectiveFunction(),
            problem.getObjectiveFunctionGradient(),
            GoalType.MINIMIZE,
            new InitialGuess(new double[] { 98.680, 47.345 }));
        final Vector2D center = new Vector2D(optimum.getPointRef()[0], optimum.getPointRef()[1]);
        Assert.assertEquals(69.960161753, problem.getRadius(center), 1.0e-8);
        Assert.assertEquals(96.075902096, center.getX(), 1.0e-8);
        Assert.assertEquals(48.135167894, center.getY(), 1.0e-8);
    }

    private static class LinearProblem {
        final RealMatrix factors;
        final double[] target;

        public LinearProblem(final double[][] factors,
            final double[] target) {
            this.factors = new BlockRealMatrix(factors);
            this.target = target;
        }

        public ObjectiveFunction getObjectiveFunction() {
            return new ObjectiveFunction(new MultivariateFunction(){
                @Override
                public double value(final double[] point) {
                    final double[] y = LinearProblem.this.factors.operate(point);
                    double sum = 0;
                    for (int i = 0; i < y.length; ++i) {
                        final double ri = y[i] - LinearProblem.this.target[i];
                        sum += ri * ri;
                    }
                    return sum;
                }
            });
        }

        public ObjectiveFunctionGradient getObjectiveFunctionGradient() {
            return new ObjectiveFunctionGradient(new MultivariateVectorFunction(){
                @Override
                public double[] value(final double[] point) {
                    final double[] r = LinearProblem.this.factors.operate(point);
                    for (int i = 0; i < r.length; ++i) {
                        r[i] -= LinearProblem.this.target[i];
                    }
                    final double[] p = LinearProblem.this.factors.transpose().operate(r);
                    for (int i = 0; i < p.length; ++i) {
                        p[i] *= 2;
                    }
                    return p;
                }
            });
        }
    }
}
