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
package fr.cnes.sirius.patrius.math.optim.nonlinear.vector.jacobian;

import java.util.ArrayList;

import fr.cnes.sirius.patrius.math.analysis.MultivariateMatrixFunction;
import fr.cnes.sirius.patrius.math.analysis.MultivariateVectorFunction;
import fr.cnes.sirius.patrius.math.optim.nonlinear.vector.ModelFunction;
import fr.cnes.sirius.patrius.math.optim.nonlinear.vector.ModelFunctionJacobian;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;

/**
 * Class that models a circle.
 * The parameters of problem are:
 * <ul>
 * <li>the x-coordinate of the circle center,</li>
 * <li>the y-coordinate of the circle center,</li>
 * <li>the radius of the circle.</li>
 * </ul>
 * The model functions are:
 * <ul>
 * <li>for each triplet (cx, cy, r), the (x, y) coordinates of a point on the corresponding circle.</li>
 * </ul>
 */
class CircleProblem {
    /** Cloud of points assumed to be fitted by a circle. */
    private final ArrayList<double[]> points;
    /** Error on the x-coordinate of the points. */
    private final double xSigma;
    /** Error on the y-coordinate of the points. */
    private final double ySigma;
    /**
     * Number of points on the circumference (when searching which
     * model point is closest to a given "observation".
     */
    private final int resolution;

    /**
     * @param xError
     *        Assumed error for the x-coordinate of the circle points.
     * @param yError
     *        Assumed error for the y-coordinate of the circle points.
     * @param searchResolution
     *        Number of points to try when searching the one
     *        that is closest to a given "observed" point.
     */
    public CircleProblem(final double xError,
        final double yError,
        final int searchResolution) {
        this.points = new ArrayList<double[]>();
        this.xSigma = xError;
        this.ySigma = yError;
        this.resolution = searchResolution;
    }

    /**
     * @param xError
     *        Assumed error for the x-coordinate of the circle points.
     * @param yError
     *        Assumed error for the y-coordinate of the circle points.
     */
    public CircleProblem(final double xError,
        final double yError) {
        this(xError, yError, 500);
    }

    public void addPoint(final double px, final double py) {
        this.points.add(new double[] { px, py });
    }

    public double[] target() {
        final double[] t = new double[this.points.size() * 2];
        for (int i = 0; i < this.points.size(); i++) {
            final double[] p = this.points.get(i);
            final int index = i * 2;
            t[index] = p[0];
            t[index + 1] = p[1];
        }

        return t;
    }

    public double[] weight() {
        final double wX = 1 / (this.xSigma * this.xSigma);
        final double wY = 1 / (this.ySigma * this.ySigma);
        final double[] w = new double[this.points.size() * 2];
        for (int i = 0; i < this.points.size(); i++) {
            final int index = i * 2;
            w[index] = wX;
            w[index + 1] = wY;
        }

        return w;
    }

    public ModelFunction getModelFunction() {
        return new ModelFunction(new MultivariateVectorFunction(){
            @Override
            public double[] value(final double[] params) {
                final double cx = params[0];
                final double cy = params[1];
                final double r = params[2];

                final double[] model = new double[CircleProblem.this.points.size() * 2];

                final double deltaTheta = MathUtils.TWO_PI / CircleProblem.this.resolution;
                for (int i = 0; i < CircleProblem.this.points.size(); i++) {
                    final double[] p = CircleProblem.this.points.get(i);
                    final double px = p[0];
                    final double py = p[1];

                    double bestX = 0;
                    double bestY = 0;
                    double dMin = Double.POSITIVE_INFINITY;

                    // Find the angle for which the circle passes closest to the
                    // current point (using a resolution of 100 points along the
                    // circumference).
                    for (double theta = 0; theta <= MathUtils.TWO_PI; theta += deltaTheta) {
                        final double currentX = cx + r * MathLib.cos(theta);
                        final double currentY = cy + r * MathLib.sin(theta);
                        final double dX = currentX - px;
                        final double dY = currentY - py;
                        final double d = dX * dX + dY * dY;
                        if (d < dMin) {
                            dMin = d;
                            bestX = currentX;
                            bestY = currentY;
                        }
                    }

                    final int index = i * 2;
                    model[index] = bestX;
                    model[index + 1] = bestY;
                }

                return model;
            }
        });
    }

    public ModelFunctionJacobian getModelFunctionJacobian() {
        return new ModelFunctionJacobian(new MultivariateMatrixFunction(){
            @Override
            public double[][] value(final double[] point) {
                return CircleProblem.this.jacobian(point);
            }
        });
    }

    private double[][] jacobian(final double[] params) {
        final double[][] jacobian = new double[this.points.size() * 2][3];

        for (int i = 0; i < this.points.size(); i++) {
            final int index = i * 2;
            // Partial derivative wrt x-coordinate of center.
            jacobian[index][0] = 1;
            jacobian[index + 1][0] = 0;
            // Partial derivative wrt y-coordinate of center.
            jacobian[index][1] = 0;
            jacobian[index + 1][1] = 1;
            // Partial derivative wrt radius.
            final double[] p = this.points.get(i);
            jacobian[index][2] = (p[0] - params[0]) / params[2];
            jacobian[index + 1][2] = (p[1] - params[1]) / params[2];
        }

        return jacobian;
    }
}
