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
package fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.gradient;

import java.util.ArrayList;

import fr.cnes.sirius.patrius.math.analysis.MultivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.MultivariateVectorFunction;
import fr.cnes.sirius.patrius.math.geometry.euclidean.twod.Vector2D;
import fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.ObjectiveFunction;
import fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.ObjectiveFunctionGradient;

/**
 * Class used in the tests.
 */
public class CircleScalar {
    private final ArrayList<Vector2D> points;

    public CircleScalar() {
        this.points = new ArrayList<Vector2D>();
    }

    public void addPoint(final double px, final double py) {
        this.points.add(new Vector2D(px, py));
    }

    public double getRadius(final Vector2D center) {
        double r = 0;
        for (final Vector2D point : this.points) {
            r += point.distance(center);
        }
        return r / this.points.size();
    }

    public ObjectiveFunction getObjectiveFunction() {
        return new ObjectiveFunction(new MultivariateFunction(){
            @Override
            public double value(final double[] params) {
                final Vector2D center = new Vector2D(params[0], params[1]);
                final double radius = CircleScalar.this.getRadius(center);
                double sum = 0;
                for (final Vector2D point : CircleScalar.this.points) {
                    final double di = point.distance(center) - radius;
                    sum += di * di;
                }
                return sum;
            }
        });
    }

    public ObjectiveFunctionGradient getObjectiveFunctionGradient() {
        return new ObjectiveFunctionGradient(new MultivariateVectorFunction(){
            @Override
            public double[] value(final double[] params) {
                final Vector2D center = new Vector2D(params[0], params[1]);
                final double radius = CircleScalar.this.getRadius(center);
                // gradient of the sum of squared residuals
                double dJdX = 0;
                double dJdY = 0;
                for (final Vector2D pk : CircleScalar.this.points) {
                    final double dk = pk.distance(center);
                    dJdX += (center.getX() - pk.getX()) * (dk - radius) / dk;
                    dJdY += (center.getY() - pk.getY()) * (dk - radius) / dk;
                }
                dJdX *= 2;
                dJdY *= 2;

                return new double[] { dJdX, dJdY };
            }
        });
    }
}
