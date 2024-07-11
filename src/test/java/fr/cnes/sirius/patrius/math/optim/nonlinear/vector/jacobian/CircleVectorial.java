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
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.optim.nonlinear.vector.jacobian;

import java.util.ArrayList;

import fr.cnes.sirius.patrius.math.analysis.MultivariateMatrixFunction;
import fr.cnes.sirius.patrius.math.analysis.MultivariateVectorFunction;
import fr.cnes.sirius.patrius.math.geometry.euclidean.twod.Vector2D;
import fr.cnes.sirius.patrius.math.optim.nonlinear.vector.ModelFunction;
import fr.cnes.sirius.patrius.math.optim.nonlinear.vector.ModelFunctionJacobian;

/**
 * Class used in the tests.
 */
class CircleVectorial {
    private final ArrayList<Vector2D> points;

    public CircleVectorial() {
        this.points = new ArrayList<Vector2D>();
    }

    public void addPoint(final double px, final double py) {
        this.points.add(new Vector2D(px, py));
    }

    public int getN() {
        return this.points.size();
    }

    public double getRadius(final Vector2D center) {
        double r = 0;
        for (final Vector2D point : this.points) {
            r += point.distance(center);
        }
        return r / this.points.size();
    }

    public ModelFunction getModelFunction() {
        return new ModelFunction(new MultivariateVectorFunction(){
            @Override
            public double[] value(final double[] params) {
                final Vector2D center = new Vector2D(params[0], params[1]);
                final double radius = CircleVectorial.this.getRadius(center);
                final double[] residuals = new double[CircleVectorial.this.points.size()];
                for (int i = 0; i < residuals.length; i++) {
                    residuals[i] = CircleVectorial.this.points.get(i).distance(center) - radius;
                }

                return residuals;
            }
        });
    }

    public ModelFunctionJacobian getModelFunctionJacobian() {
        return new ModelFunctionJacobian(new MultivariateMatrixFunction(){
            @Override
            public double[][] value(final double[] params) {
                final int n = CircleVectorial.this.points.size();
                final Vector2D center = new Vector2D(params[0], params[1]);

                double dRdX = 0;
                double dRdY = 0;
                for (final Vector2D pk : CircleVectorial.this.points) {
                    final double dk = pk.distance(center);
                    dRdX += (center.getX() - pk.getX()) / dk;
                    dRdY += (center.getY() - pk.getY()) / dk;
                }
                dRdX /= n;
                dRdY /= n;

                // Jacobian of the radius residuals.
                final double[][] jacobian = new double[n][2];
                for (int i = 0; i < n; i++) {
                    final Vector2D pi = CircleVectorial.this.points.get(i);
                    final double di = pi.distance(center);
                    jacobian[i][0] = (center.getX() - pi.getX()) / di - dRdX;
                    jacobian[i][1] = (center.getY() - pi.getY()) / di - dRdY;
                }

                return jacobian;
            }
        });
    }
}
