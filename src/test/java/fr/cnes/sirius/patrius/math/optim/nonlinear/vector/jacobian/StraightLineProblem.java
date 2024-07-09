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
package fr.cnes.sirius.patrius.math.optim.nonlinear.vector.jacobian;

import java.util.ArrayList;

import fr.cnes.sirius.patrius.math.analysis.MultivariateMatrixFunction;
import fr.cnes.sirius.patrius.math.analysis.MultivariateVectorFunction;
import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.optim.nonlinear.vector.ModelFunction;
import fr.cnes.sirius.patrius.math.optim.nonlinear.vector.ModelFunctionJacobian;
import fr.cnes.sirius.patrius.math.stat.regression.SimpleRegression;

/**
 * Class that models a straight line defined as {@code y = a x + b}.
 * The parameters of problem are:
 * <ul>
 * <li>{@code a}</li>
 * <li>{@code b}</li>
 * </ul>
 * The model functions are:
 * <ul>
 * <li>for each pair (a, b), the y-coordinate of the line.</li>
 * </ul>
 */
class StraightLineProblem {
    /** Cloud of points assumed to be fitted by a straight line. */
    private final ArrayList<double[]> points;
    /** Error (on the y-coordinate of the points). */
    private final double sigma;

    /**
     * @param error
     *        Assumed error for the y-coordinate.
     */
    public StraightLineProblem(final double error) {
        this.points = new ArrayList<double[]>();
        this.sigma = error;
    }

    public void addPoint(final double px, final double py) {
        this.points.add(new double[] { px, py });
    }

    /**
     * @return the list of x-coordinates.
     */
    public double[] x() {
        final double[] v = new double[this.points.size()];
        for (int i = 0; i < this.points.size(); i++) {
            final double[] p = this.points.get(i);
            v[i] = p[0]; // x-coordinate.
        }

        return v;
    }

    /**
     * @return the list of y-coordinates.
     */
    public double[] y() {
        final double[] v = new double[this.points.size()];
        for (int i = 0; i < this.points.size(); i++) {
            final double[] p = this.points.get(i);
            v[i] = p[1]; // y-coordinate.
        }

        return v;
    }

    public double[] target() {
        return this.y();
    }

    public double[] weight() {
        final double weight = 1 / (this.sigma * this.sigma);
        final double[] w = new double[this.points.size()];
        for (int i = 0; i < this.points.size(); i++) {
            w[i] = weight;
        }

        return w;
    }

    public ModelFunction getModelFunction() {
        return new ModelFunction(new MultivariateVectorFunction(){
            @Override
            public double[] value(final double[] params) {
                final Model line = new Model(params[0], params[1]);

                final double[] model = new double[StraightLineProblem.this.points.size()];
                for (int i = 0; i < StraightLineProblem.this.points.size(); i++) {
                    final double[] p = StraightLineProblem.this.points.get(i);
                    model[i] = line.value(p[0]);
                }

                return model;
            }
        });
    }

    public ModelFunctionJacobian getModelFunctionJacobian() {
        return new ModelFunctionJacobian(new MultivariateMatrixFunction(){
            @Override
            public double[][] value(final double[] point) {
                return StraightLineProblem.this.jacobian(point);
            }
        });
    }

    /**
     * Directly solve the linear problem, using the {@link SimpleRegression} class.
     */
    public double[] solve() {
        final SimpleRegression regress = new SimpleRegression(true);
        for (final double[] d : this.points) {
            regress.addData(d[0], d[1]);
        }

        final double[] result = { regress.getSlope(), regress.getIntercept() };
        return result;
    }

    private double[][] jacobian(final double[] params) {
        final double[][] jacobian = new double[this.points.size()][2];

        for (int i = 0; i < this.points.size(); i++) {
            final double[] p = this.points.get(i);
            // Partial derivative wrt "a".
            jacobian[i][0] = p[0];
            // Partial derivative wrt "b".
            jacobian[i][1] = 1;
        }

        return jacobian;
    }

    /**
     * Linear function.
     */
    public static class Model implements UnivariateFunction {
        final double a;
        final double b;

        public Model(final double a,
            final double b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public double value(final double x) {
            return this.a * x + this.b;
        }
    }
}
