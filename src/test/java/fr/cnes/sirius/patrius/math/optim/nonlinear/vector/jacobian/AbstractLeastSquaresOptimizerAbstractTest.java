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
package fr.cnes.sirius.patrius.math.optim.nonlinear.vector.jacobian;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.analysis.MultivariateMatrixFunction;
import fr.cnes.sirius.patrius.math.analysis.MultivariateVectorFunction;
import fr.cnes.sirius.patrius.math.exception.ConvergenceException;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.geometry.euclidean.twod.Vector2D;
import fr.cnes.sirius.patrius.math.linear.BlockRealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.optim.InitialGuess;
import fr.cnes.sirius.patrius.math.optim.MaxEval;
import fr.cnes.sirius.patrius.math.optim.PointVectorValuePair;
import fr.cnes.sirius.patrius.math.optim.nonlinear.vector.ModelFunction;
import fr.cnes.sirius.patrius.math.optim.nonlinear.vector.ModelFunctionJacobian;
import fr.cnes.sirius.patrius.math.optim.nonlinear.vector.Target;
import fr.cnes.sirius.patrius.math.optim.nonlinear.vector.Weight;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;

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
 * @version $Id: AbstractLeastSquaresOptimizerAbstractTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public abstract class AbstractLeastSquaresOptimizerAbstractTest {

    public abstract AbstractLeastSquaresOptimizer createOptimizer();

    @Test
    public void testTrivial() {
        final LinearProblem problem = new LinearProblem(new double[][] { { 2 } }, new double[] { 3 });
        final AbstractLeastSquaresOptimizer optimizer = this.createOptimizer();
        final PointVectorValuePair optimum =
            optimizer.optimize(new MaxEval(100),
                problem.getModelFunction(),
                problem.getModelFunctionJacobian(),
                problem.getTarget(),
                new Weight(new double[] { 1 }),
                new InitialGuess(new double[] { 0 }));
        Assert.assertEquals(0, optimizer.getRMS(), 1e-10);
        Assert.assertEquals(1.5, optimum.getPoint()[0], 1e-10);
        Assert.assertEquals(3.0, optimum.getValue()[0], 1e-10);
    }

    @Test
    public void testQRColumnsPermutation() {

        final LinearProblem problem = new LinearProblem(new double[][] { { 1, -1 }, { 0, 2 }, { 1, -2 } },
            new double[] { 4, 6, 1 });

        final AbstractLeastSquaresOptimizer optimizer = this.createOptimizer();
        final PointVectorValuePair optimum =
            optimizer.optimize(new MaxEval(100),
                problem.getModelFunction(),
                problem.getModelFunctionJacobian(),
                problem.getTarget(),
                new Weight(new double[] { 1, 1, 1 }),
                new InitialGuess(new double[] { 0, 0 }));
        Assert.assertEquals(0, optimizer.getRMS(), 1e-10);
        Assert.assertEquals(7, optimum.getPoint()[0], 1e-10);
        Assert.assertEquals(3, optimum.getPoint()[1], 1e-10);
        Assert.assertEquals(4, optimum.getValue()[0], 1e-10);
        Assert.assertEquals(6, optimum.getValue()[1], 1e-10);
        Assert.assertEquals(1, optimum.getValue()[2], 1e-10);
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
        }, new double[] { 0, 1.1, 2.2, 3.3, 4.4, 5.5 });
        final AbstractLeastSquaresOptimizer optimizer = this.createOptimizer();
        final PointVectorValuePair optimum =
            optimizer.optimize(new MaxEval(100),
                problem.getModelFunction(),
                problem.getModelFunctionJacobian(),
                problem.getTarget(),
                new Weight(new double[] { 1, 1, 1, 1, 1, 1 }),
                new InitialGuess(new double[] { 0, 0, 0, 0, 0, 0 }));
        Assert.assertEquals(0, optimizer.getRMS(), 1e-10);
        for (int i = 0; i < problem.target.length; ++i) {
            Assert.assertEquals(0.55 * i, optimum.getPoint()[i], 1e-10);
        }
    }

    @Test
    public void testOneSet() {

        final LinearProblem problem = new LinearProblem(new double[][] {
            { 1, 0, 0 },
            { -1, 1, 0 },
            { 0, -1, 1 }
        }, new double[] { 1, 1, 1 });
        final AbstractLeastSquaresOptimizer optimizer = this.createOptimizer();
        final PointVectorValuePair optimum =
            optimizer.optimize(new MaxEval(100),
                problem.getModelFunction(),
                problem.getModelFunctionJacobian(),
                problem.getTarget(),
                new Weight(new double[] { 1, 1, 1 }),
                new InitialGuess(new double[] { 0, 0, 0 }));
        Assert.assertEquals(0, optimizer.getRMS(), 1e-10);
        Assert.assertEquals(1, optimum.getPoint()[0], 1e-10);
        Assert.assertEquals(2, optimum.getPoint()[1], 1e-10);
        Assert.assertEquals(3, optimum.getPoint()[2], 1e-10);
    }

    @Test
    public void testTwoSets() {
        final double epsilon = 1e-7;
        final LinearProblem problem = new LinearProblem(new double[][] {
            { 2, 1, 0, 4, 0, 0 },
            { -4, -2, 3, -7, 0, 0 },
            { 4, 1, -2, 8, 0, 0 },
            { 0, -3, -12, -1, 0, 0 },
            { 0, 0, 0, 0, epsilon, 1 },
            { 0, 0, 0, 0, 1, 1 }
        }, new double[] { 2, -9, 2, 2, 1 + epsilon * epsilon, 2 });

        final AbstractLeastSquaresOptimizer optimizer = this.createOptimizer();
        final PointVectorValuePair optimum =
            optimizer.optimize(new MaxEval(100),
                problem.getModelFunction(),
                problem.getModelFunctionJacobian(),
                problem.getTarget(),
                new Weight(new double[] { 1, 1, 1, 1, 1, 1 }),
                new InitialGuess(new double[] { 0, 0, 0, 0, 0, 0 }));
        Assert.assertEquals(0, optimizer.getRMS(), 1e-10);
        Assert.assertEquals(3, optimum.getPoint()[0], 1e-10);
        Assert.assertEquals(4, optimum.getPoint()[1], 1e-10);
        Assert.assertEquals(-1, optimum.getPoint()[2], 1e-10);
        Assert.assertEquals(-2, optimum.getPoint()[3], 1e-10);
        Assert.assertEquals(1 + epsilon, optimum.getPoint()[4], 1e-10);
        Assert.assertEquals(1 - epsilon, optimum.getPoint()[5], 1e-10);
    }

    @Test(expected = ConvergenceException.class)
    public void testNonInvertible() throws Exception {

        final LinearProblem problem = new LinearProblem(new double[][] {
            { 1, 2, -3 },
            { 2, 1, 3 },
            { -3, 0, -9 }
        }, new double[] { 1, 1, 1 });

        final AbstractLeastSquaresOptimizer optimizer = this.createOptimizer();

        optimizer.optimize(new MaxEval(100),
            problem.getModelFunction(),
            problem.getModelFunctionJacobian(),
            problem.getTarget(),
            new Weight(new double[] { 1, 1, 1 }),
            new InitialGuess(new double[] { 0, 0, 0 }));
    }

    @Test
    public void testIllConditioned() {
        final LinearProblem problem1 = new LinearProblem(new double[][] {
            { 10, 7, 8, 7 },
            { 7, 5, 6, 5 },
            { 8, 6, 10, 9 },
            { 7, 5, 9, 10 }
        }, new double[] { 32, 23, 33, 31 });
        final AbstractLeastSquaresOptimizer optimizer = this.createOptimizer();
        final PointVectorValuePair optimum1 =
            optimizer.optimize(new MaxEval(100),
                problem1.getModelFunction(),
                problem1.getModelFunctionJacobian(),
                problem1.getTarget(),
                new Weight(new double[] { 1, 1, 1, 1 }),
                new InitialGuess(new double[] { 0, 1, 2, 3 }));
        Assert.assertEquals(0, optimizer.getRMS(), 1e-10);
        Assert.assertEquals(1, optimum1.getPoint()[0], 1e-10);
        Assert.assertEquals(1, optimum1.getPoint()[1], 1e-10);
        Assert.assertEquals(1, optimum1.getPoint()[2], 1e-10);
        Assert.assertEquals(1, optimum1.getPoint()[3], 1e-10);

        final LinearProblem problem2 = new LinearProblem(new double[][] {
            { 10.00, 7.00, 8.10, 7.20 },
            { 7.08, 5.04, 6.00, 5.00 },
            { 8.00, 5.98, 9.89, 9.00 },
            { 6.99, 4.99, 9.00, 9.98 }
        }, new double[] { 32, 23, 33, 31 });
        final PointVectorValuePair optimum2 =
            optimizer.optimize(new MaxEval(100),
                problem2.getModelFunction(),
                problem2.getModelFunctionJacobian(),
                problem2.getTarget(),
                new Weight(new double[] { 1, 1, 1, 1 }),
                new InitialGuess(new double[] { 0, 1, 2, 3 }));
        Assert.assertEquals(0, optimizer.getRMS(), 1e-10);
        Assert.assertEquals(-81, optimum2.getPoint()[0], 1e-8);
        Assert.assertEquals(137, optimum2.getPoint()[1], 1e-8);
        Assert.assertEquals(-34, optimum2.getPoint()[2], 1e-8);
        Assert.assertEquals(22, optimum2.getPoint()[3], 1e-8);
    }

    @Test
    public void testMoreEstimatedParametersSimple() {

        final LinearProblem problem = new LinearProblem(new double[][] {
            { 3, 2, 0, 0 },
            { 0, 1, -1, 1 },
            { 2, 0, 1, 0 }
        }, new double[] { 7, 3, 5 });

        final AbstractLeastSquaresOptimizer optimizer = this.createOptimizer();
        optimizer.optimize(new MaxEval(100),
            problem.getModelFunction(),
            problem.getModelFunctionJacobian(),
            problem.getTarget(),
            new Weight(new double[] { 1, 1, 1 }),
            new InitialGuess(new double[] { 7, 6, 5, 4 }));
        Assert.assertEquals(0, optimizer.getRMS(), 1e-10);
    }

    @Test
    public void testMoreEstimatedParametersUnsorted() {
        final LinearProblem problem = new LinearProblem(new double[][] {
            { 1, 1, 0, 0, 0, 0 },
            { 0, 0, 1, 1, 1, 0 },
            { 0, 0, 0, 0, 1, -1 },
            { 0, 0, -1, 1, 0, 1 },
            { 0, 0, 0, -1, 1, 0 }
        }, new double[] { 3, 12, -1, 7, 1 });

        final AbstractLeastSquaresOptimizer optimizer = this.createOptimizer();
        final PointVectorValuePair optimum =
            optimizer.optimize(new MaxEval(100),
                problem.getModelFunction(),
                problem.getModelFunctionJacobian(),
                problem.getTarget(),
                new Weight(new double[] { 1, 1, 1, 1, 1 }),
                new InitialGuess(new double[] { 2, 2, 2, 2, 2, 2 }));
        Assert.assertEquals(0, optimizer.getRMS(), 1e-10);
        Assert.assertEquals(3, optimum.getPointRef()[2], 1e-10);
        Assert.assertEquals(4, optimum.getPointRef()[3], 1e-10);
        Assert.assertEquals(5, optimum.getPointRef()[4], 1e-10);
        Assert.assertEquals(6, optimum.getPointRef()[5], 1e-10);
    }

    @Test
    public void testRedundantEquations() {
        final LinearProblem problem = new LinearProblem(new double[][] {
            { 1, 1 },
            { 1, -1 },
            { 1, 3 }
        }, new double[] { 3, 1, 5 });

        final AbstractLeastSquaresOptimizer optimizer = this.createOptimizer();
        final PointVectorValuePair optimum =
            optimizer.optimize(new MaxEval(100),
                problem.getModelFunction(),
                problem.getModelFunctionJacobian(),
                problem.getTarget(),
                new Weight(new double[] { 1, 1, 1 }),
                new InitialGuess(new double[] { 1, 1 }));
        Assert.assertEquals(0, optimizer.getRMS(), 1e-10);
        Assert.assertEquals(2, optimum.getPointRef()[0], 1e-10);
        Assert.assertEquals(1, optimum.getPointRef()[1], 1e-10);
    }

    @Test
    public void testInconsistentEquations() {
        final LinearProblem problem = new LinearProblem(new double[][] {
            { 1, 1 },
            { 1, -1 },
            { 1, 3 }
        }, new double[] { 3, 1, 4 });

        final AbstractLeastSquaresOptimizer optimizer = this.createOptimizer();
        optimizer.optimize(new MaxEval(100),
            problem.getModelFunction(),
            problem.getModelFunctionJacobian(),
            problem.getTarget(),
            new Weight(new double[] { 1, 1, 1 }),
            new InitialGuess(new double[] { 1, 1 }));
        Assert.assertTrue(optimizer.getRMS() > 0.1);
    }

    @Test(expected = DimensionMismatchException.class)
    public void testInconsistentSizes1() {
        final LinearProblem problem = new LinearProblem(new double[][] { { 1, 0 }, { 0, 1 } },
            new double[] { -1, 1 });
        final AbstractLeastSquaresOptimizer optimizer = this.createOptimizer();
        final PointVectorValuePair optimum =
            optimizer.optimize(new MaxEval(100),
                problem.getModelFunction(),
                problem.getModelFunctionJacobian(),
                problem.getTarget(),
                new Weight(new double[] { 1, 1 }),
                new InitialGuess(new double[] { 0, 0 }));
        Assert.assertEquals(0, optimizer.getRMS(), 1e-10);
        Assert.assertEquals(-1, optimum.getPoint()[0], 1e-10);
        Assert.assertEquals(1, optimum.getPoint()[1], 1e-10);

        optimizer.optimize(new MaxEval(100),
            problem.getModelFunction(),
            problem.getModelFunctionJacobian(),
            problem.getTarget(),
            new Weight(new double[] { 1 }),
            new InitialGuess(new double[] { 0, 0 }));
    }

    @Test(expected = DimensionMismatchException.class)
    public void testInconsistentSizes2() {
        final LinearProblem problem = new LinearProblem(new double[][] { { 1, 0 }, { 0, 1 } },
            new double[] { -1, 1 });
        final AbstractLeastSquaresOptimizer optimizer = this.createOptimizer();
        final PointVectorValuePair optimum = optimizer.optimize(new MaxEval(100),
            problem.getModelFunction(),
            problem.getModelFunctionJacobian(),
            problem.getTarget(),
            new Weight(new double[] { 1, 1 }),
            new InitialGuess(new double[] { 0, 0 }));
        Assert.assertEquals(0, optimizer.getRMS(), 1e-10);
        Assert.assertEquals(-1, optimum.getPoint()[0], 1e-10);
        Assert.assertEquals(1, optimum.getPoint()[1], 1e-10);

        optimizer.optimize(new MaxEval(100),
            problem.getModelFunction(),
            problem.getModelFunctionJacobian(),
            new Target(new double[] { 1 }),
            new Weight(new double[] { 1 }),
            new InitialGuess(new double[] { 0, 0 }));
    }

    @Test
    public void testCircleFitting() {
        final CircleVectorial circle = new CircleVectorial();
        circle.addPoint(30, 68);
        circle.addPoint(50, -6);
        circle.addPoint(110, -20);
        circle.addPoint(35, 15);
        circle.addPoint(45, 97);
        final AbstractLeastSquaresOptimizer optimizer = this.createOptimizer();
        PointVectorValuePair optimum = optimizer.optimize(new MaxEval(100),
            circle.getModelFunction(),
            circle.getModelFunctionJacobian(),
            new Target(new double[] { 0, 0, 0, 0, 0 }),
            new Weight(new double[] { 1, 1, 1, 1, 1 }),
            new InitialGuess(new double[] { 98.680, 47.345 }));
        Assert.assertTrue(optimizer.getEvaluations() < 10);
        final double rms = optimizer.getRMS();
        Assert.assertEquals(1.768262623567235, MathLib.sqrt(circle.getN()) * rms, 1e-10);
        final Vector2D center = new Vector2D(optimum.getPointRef()[0], optimum.getPointRef()[1]);
        Assert.assertEquals(69.96016176931406, circle.getRadius(center), 1e-6);
        Assert.assertEquals(96.07590211815305, center.getX(), 1e-6);
        Assert.assertEquals(48.13516790438953, center.getY(), 1e-6);
        double[][] cov = optimizer.computeCovariances(optimum.getPoint(), 1e-14);
        Assert.assertEquals(1.839, cov[0][0], 0.001);
        Assert.assertEquals(0.731, cov[0][1], 0.001);
        Assert.assertEquals(cov[0][1], cov[1][0], 1e-14);
        Assert.assertEquals(0.786, cov[1][1], 0.001);

        // add perfect measurements and check errors are reduced
        final double r = circle.getRadius(center);
        for (double d = 0; d < 2 * FastMath.PI; d += 0.01) {
            circle.addPoint(center.getX() + r * MathLib.cos(d), center.getY() + r * MathLib.sin(d));
        }
        final double[] target = new double[circle.getN()];
        Arrays.fill(target, 0);
        final double[] weights = new double[circle.getN()];
        Arrays.fill(weights, 2);
        optimum = optimizer.optimize(new MaxEval(100),
            circle.getModelFunction(),
            circle.getModelFunctionJacobian(),
            new Target(target),
            new Weight(weights),
            new InitialGuess(new double[] { 98.680, 47.345 }));
        cov = optimizer.computeCovariances(optimum.getPoint(), 1e-14);
        Assert.assertEquals(0.0016, cov[0][0], 0.001);
        Assert.assertEquals(3.2e-7, cov[0][1], 1e-9);
        Assert.assertEquals(cov[0][1], cov[1][0], 1e-14);
        Assert.assertEquals(0.0016, cov[1][1], 0.001);
    }

    @Test
    public void testCircleFittingBadInit() {
        final CircleVectorial circle = new CircleVectorial();
        final double[][] points = this.circlePoints;
        final double[] target = new double[points.length];
        Arrays.fill(target, 0);
        final double[] weights = new double[points.length];
        Arrays.fill(weights, 2);
        for (final double[] point : points) {
            circle.addPoint(point[0], point[1]);
        }
        final AbstractLeastSquaresOptimizer optimizer = this.createOptimizer();
        final PointVectorValuePair optimum = optimizer.optimize(new MaxEval(100),
            circle.getModelFunction(),
            circle.getModelFunctionJacobian(),
            new Target(target),
            new Weight(weights),
            new InitialGuess(new double[] { -12, -12 }));
        final Vector2D center = new Vector2D(optimum.getPointRef()[0], optimum.getPointRef()[1]);
        Assert.assertTrue(optimizer.getEvaluations() < 25);
        Assert.assertEquals(0.043, optimizer.getRMS(), 1e-3);
        Assert.assertEquals(0.292235, circle.getRadius(center), 1e-6);
        Assert.assertEquals(-0.151738, center.getX(), 1e-6);
        Assert.assertEquals(0.2075001, center.getY(), 1e-6);
    }

    @Test
    public void testCircleFittingGoodInit() {
        final CircleVectorial circle = new CircleVectorial();
        final double[][] points = this.circlePoints;
        final double[] target = new double[points.length];
        Arrays.fill(target, 0);
        final double[] weights = new double[points.length];
        Arrays.fill(weights, 2);
        for (final double[] point : points) {
            circle.addPoint(point[0], point[1]);
        }
        final AbstractLeastSquaresOptimizer optimizer = this.createOptimizer();
        final PointVectorValuePair optimum =
            optimizer.optimize(new MaxEval(100),
                circle.getModelFunction(),
                circle.getModelFunctionJacobian(),
                new Target(target),
                new Weight(weights),
                new InitialGuess(new double[] { 0, 0 }));
        Assert.assertEquals(-0.1517383071957963, optimum.getPointRef()[0], 1e-6);
        Assert.assertEquals(0.2074999736353867, optimum.getPointRef()[1], 1e-6);
        Assert.assertEquals(0.04268731682389561, optimizer.getRMS(), 1e-8);
    }

    private final double[][] circlePoints = new double[][] {
        { -0.312967, 0.072366 }, { -0.339248, 0.132965 }, { -0.379780, 0.202724 },
        { -0.390426, 0.260487 }, { -0.361212, 0.328325 }, { -0.346039, 0.392619 },
        { -0.280579, 0.444306 }, { -0.216035, 0.470009 }, { -0.149127, 0.493832 },
        { -0.075133, 0.483271 }, { -0.007759, 0.452680 }, { 0.060071, 0.410235 },
        { 0.103037, 0.341076 }, { 0.118438, 0.273884 }, { 0.131293, 0.192201 },
        { 0.115869, 0.129797 }, { 0.072223, 0.058396 }, { 0.022884, 0.000718 },
        { -0.053355, -0.020405 }, { -0.123584, -0.032451 }, { -0.216248, -0.032862 },
        { -0.278592, -0.005008 }, { -0.337655, 0.056658 }, { -0.385899, 0.112526 },
        { -0.405517, 0.186957 }, { -0.415374, 0.262071 }, { -0.387482, 0.343398 },
        { -0.347322, 0.397943 }, { -0.287623, 0.458425 }, { -0.223502, 0.475513 },
        { -0.135352, 0.478186 }, { -0.061221, 0.483371 }, { 0.003711, 0.422737 },
        { 0.065054, 0.375830 }, { 0.108108, 0.297099 }, { 0.123882, 0.222850 },
        { 0.117729, 0.134382 }, { 0.085195, 0.056820 }, { 0.029800, -0.019138 },
        { -0.027520, -0.072374 }, { -0.102268, -0.091555 }, { -0.200299, -0.106578 },
        { -0.292731, -0.091473 }, { -0.356288, -0.051108 }, { -0.420561, 0.014926 },
        { -0.471036, 0.074716 }, { -0.488638, 0.182508 }, { -0.485990, 0.254068 },
        { -0.463943, 0.338438 }, { -0.406453, 0.404704 }, { -0.334287, 0.466119 },
        { -0.254244, 0.503188 }, { -0.161548, 0.495769 }, { -0.075733, 0.495560 },
        { 0.001375, 0.434937 }, { 0.082787, 0.385806 }, { 0.115490, 0.323807 },
        { 0.141089, 0.223450 }, { 0.138693, 0.131703 }, { 0.126415, 0.049174 },
        { 0.066518, -0.010217 }, { -0.005184, -0.070647 }, { -0.080985, -0.103635 },
        { -0.177377, -0.116887 }, { -0.260628, -0.100258 }, { -0.335756, -0.056251 },
        { -0.405195, -0.000895 }, { -0.444937, 0.085456 }, { -0.484357, 0.175597 },
        { -0.472453, 0.248681 }, { -0.438580, 0.347463 }, { -0.402304, 0.422428 },
        { -0.326777, 0.479438 }, { -0.247797, 0.505581 }, { -0.152676, 0.519380 },
        { -0.071754, 0.516264 }, { 0.015942, 0.472802 }, { 0.076608, 0.419077 },
        { 0.127673, 0.330264 }, { 0.159951, 0.262150 }, { 0.153530, 0.172681 },
        { 0.140653, 0.089229 }, { 0.078666, 0.024981 }, { 0.023807, -0.037022 },
        { -0.048837, -0.077056 }, { -0.127729, -0.075338 }, { -0.221271, -0.067526 }
    };

    public void doTestStRD(final StatisticalReferenceDataset dataset,
                           final double errParams,
                           final double errParamsSd) {
        final AbstractLeastSquaresOptimizer optimizer = this.createOptimizer();
        final double[] w = new double[dataset.getNumObservations()];
        Arrays.fill(w, 1);

        final double[][] data = dataset.getData();
        final double[] initial = dataset.getStartingPoint(0);
        final StatisticalReferenceDataset.LeastSquaresProblem problem = dataset.getLeastSquaresProblem();
        final PointVectorValuePair optimum = optimizer.optimize(new MaxEval(100),
            problem.getModelFunction(),
            problem.getModelFunctionJacobian(),
            new Target(data[1]),
            new Weight(w),
            new InitialGuess(initial));

        final double[] actual = optimum.getPoint();
        for (int i = 0; i < actual.length; i++) {
            final double expected = dataset.getParameter(i);
            final double delta = MathLib.abs(errParams * expected);
            Assert.assertEquals(dataset.getName() + ", param #" + i,
                expected, actual[i], delta);
        }
    }

    @Test
    public void testKirby2() throws IOException {
        this.doTestStRD(StatisticalReferenceDatasetFactory.createKirby2(), 1E-7, 1E-7);
    }

    @Test
    public void testHahn1() throws IOException {
        this.doTestStRD(StatisticalReferenceDatasetFactory.createHahn1(), 1E-7, 1E-4);
    }

    static class LinearProblem {
        private final RealMatrix factors;
        private final double[] target;

        public LinearProblem(final double[][] factors, final double[] target) {
            this.factors = new BlockRealMatrix(factors);
            this.target = target;
        }

        public Target getTarget() {
            return new Target(this.target);
        }

        public ModelFunction getModelFunction() {
            return new ModelFunction(new MultivariateVectorFunction(){
                @Override
                public double[] value(final double[] params) {
                    return LinearProblem.this.factors.operate(params);
                }
            });
        }

        public ModelFunctionJacobian getModelFunctionJacobian() {
            return new ModelFunctionJacobian(new MultivariateMatrixFunction(){
                @Override
                public double[][] value(final double[] params) {
                    return LinearProblem.this.factors.getData();
                }
            });
        }
    }
}
