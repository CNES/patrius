/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 *
 * Copyright 2011-2017 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.cnes.sirius.patrius.math.analysis.interpolation;

import fr.cnes.sirius.patrius.math.analysis.TrivariateFunction;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;

/**
 * 3D-spline function.
 * 
 * @version $Id: TricubicSplineInterpolatingFunction.java 18108 2017-10-04 06:45:27Z bignon $
 */
class TricubicSplineFunction implements TrivariateFunction {

    /** Serial UID. */
    private static final long serialVersionUID = 4143262067405397759L;

    /** Number of points. */
    private static final int N = 4;
    /** Coefficients */
    private final double[][][] a = new double[N][N][N];

    /**
     * @param aV
     *        List of spline coefficients.
     */
    public TricubicSplineFunction(final double[] aV) {
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                for (int k = 0; k < N; k++) {
                    this.a[i][j][k] = aV[i + N * (j + N * k)];
                }
            }
        }
    }

    /**
     * @param x
     *        x-coordinate of the interpolation point.
     * @param y
     *        y-coordinate of the interpolation point.
     * @param z
     *        z-coordinate of the interpolation point.
     * @return the interpolated value.
     * @throws OutOfRangeException
     *         if {@code x}, {@code y} or {@code z} are not in the interval {@code [0, 1]}.
     */
    @Override
    public double value(final double x, final double y, final double z) {
        if (x < 0 || x > 1) {
            // Exception
            throw new OutOfRangeException(x, 0, 1);
        }
        if (y < 0 || y > 1) {
            // Exception
            throw new OutOfRangeException(y, 0, 1);
        }
        if (z < 0 || z > 1) {
            // Exception
            throw new OutOfRangeException(z, 0, 1);
        }

        // Computation
        // x square
        final double x2 = x * x;
        // x cube
        final double x3 = x2 * x;
        final double[] pX = { 1, x, x2, x3 };

        // y square
        final double y2 = y * y;
        // y cube
        final double y3 = y2 * y;
        final double[] pY = { 1, y, y2, y3 };

        final double z2 = z * z;
        final double z3 = z2 * z;
        final double[] pZ = { 1, z, z2, z3 };

        double result = 0;
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                for (int k = 0; k < N; k++) {
                    result += this.a[i][j][k] * pX[i] * pY[j] * pZ[k];
                }
            }
        }

        // Return result
        return result;
    }
}
