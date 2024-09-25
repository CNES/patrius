/**
 * Copyright 2019-2020 CNES
 * Copyright 2011-2014 JOptimizer
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.6:DM:DM-2591:27/01/2021:[PATRIUS] Intigration et validation JOptimizer
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.optim.joptimizer.util;

import java.util.Random;

import fr.cnes.sirius.patrius.math.linear.BlockRealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealVector;
import fr.cnes.sirius.patrius.math.optim.joptimizer.algebra.AlgebraUtils;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Utility class.
 *
 * @author alberto trivellato (alberto.trivellato@gmail.com)
 * 
 * @since 4.6
 */
public final class Utils {

    /** Relative machine epsilon. */
    private static Double relativeMachineEpsilon = Double.NaN;

    /**
     * Private constructor.
     */
    private Utils() {
        // Nothing to do
    }


    /**
     * The smallest positive (epsilon) such that 1.0 + epsilon != 1.0.
     * @return smallest positive (epsilon) such that 1.0 + epsilon != 1.0
     * See http://en.wikipedia.org/wiki/Machine_epsilon#Approximation_using_Java
     */
    public static final double getDoubleMachineEpsilon() {
        // if it is Double.NaN directly return the value
        if (!Double.isNaN(relativeMachineEpsilon)) {
            return relativeMachineEpsilon;
        }
        // only one thread executing inside at a time
        synchronized (relativeMachineEpsilon) {
            if (!Double.isNaN(relativeMachineEpsilon)) {
                return relativeMachineEpsilon;
            }
            // compute new value of epsilon
            double eps = 1.;
            do {
                eps /= 2.;
            } while ((double) (1. + (eps / 2.)) != 1.);

            relativeMachineEpsilon = eps;
        }

        return relativeMachineEpsilon;
    }

    /**
     * Calculate the scaled residual <br>
     * ||Ax-b||_oo/( ||A||_oo . ||x||_oo + ||b||_oo ), with <br>
     * ||x||_oo = max(||x[i]||)
     * @param a A matrix
     * @param x X matrix
     * @param b B matrix
     * @return scaled residual
     */
    public static double calculateScaledResidual(final RealMatrix a,
            final RealMatrix x,
            final RealMatrix b) {
        final double niX = x.getNorm();
        final double niB = b.getNorm();
        if (Double.compare(niX, 0.) == 0 && Double.compare(niB, 0.) == 0) {
            return 0;
        } else {
            final double num = a.multiply(x).subtract(b).getNorm();
            final double den = a.getNorm() * niX + niB;
            return num / den;
        }
    }

    /**
     * Calculate the scaled residual <br>
     * ||Ax-b||_oo/( ||A||_oo . ||x||_oo + ||b||_oo ), with <br>
     * ||x||_oo = max(||x[i]||)
     * @param a A matrix
     * @param x X matrix
     * @param b B matrix
     * @return scaled residual
     */
    public static double calculateScaledResidual(final RealMatrix a,
            final RealVector x,
            final RealVector b) {
        final double nix = x.getLInfNorm();
        final double nib = b.getLInfNorm();
        if (Double.compare(nix, 0.) == 0 && Double.compare(nib, 0.) == 0) {
            return 0;
        } else {
            final double num = (AlgebraUtils.zMult(a, x, b, -1)).getLInfNorm();
            final double den = a.getNorm() * nix + nib;
            return num / den;
        }
    }

    /**
     * Returns matrix filled with random values.
     * @param rows number of rows
     * @param cols number of columns
     * @param min min
     * @param max max
     * @param seed seed
     * @return matrix filled with random values
     */
    public static RealMatrix randomValuesMatrix(final int rows,
            final int cols,
            final double min,
            final double max,
            final Long seed) {
        final Random random = (seed != null) ? new Random(seed) : new Random();

        final double[][] matrix = new double[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                matrix[i][j] = min + random.nextDouble() * (max - min);
            }
        }
        return new BlockRealMatrix(matrix);
    }

    /**
     * Returns matrix filled with random positive values.
     * @param rows number of rows
     * @param cols number of columns
     * @param min min
     * @param max max
     * @param seed seed
     * @return matrix filled with random positive values
     * @see "http://mathworld.wolfram.com/PositiveDefiniteMatrix.html"
     */
    public static RealMatrix randomValuesPositiveMatrix(final int rows,
            final int cols,
            final double min,
            final double max,
            final Long seed) {
        final RealMatrix q = Utils.randomValuesMatrix(rows, cols, min, max, seed);
        final RealMatrix p = q.multiply(q.copy().transpose());
        return p.multiply(p);
    }

    /**
     * Return a new array with all the occurrences of oldValue replaced by
     * newValue.
     * @param v array
     * @param oldValue old value
     * @param newValue new value
     * @return updated array
     */
    public static final double[] replaceValues(final double[] v,
            final double oldValue,
            final double newValue) {
        final double[] ret = new double[v.length];
        for (int i = 0; i < v.length; i++) {
            final double vi = v[i];
            if (Double.compare(oldValue, vi) != 0) {
                // no substitution
                ret[i] = vi;
            } else {
                ret[i] = newValue;
            }
        }
        return ret;
    }
    
    /**
     * Returns max(a, b) or NaN if one value is a NaN.
     * @param a a
     * @param b b
     * @return max(a, b) or NaN if one value is a NaN
     */
    public static double max(final double a, final double b) {
        if (Double.isNaN(a) || Double.isNaN(b)) {
            return Double.NaN;
        } else {
            return MathLib.max(a, b);
        }
    }
    
    /**
     * Returns min(a, b) or NaN if one value is a NaN.
     * @param a a
     * @param b b
     * @return min(a, b) or NaN if one value is a NaN
     */
    public static double min(final double a, final double b) {
        if (Double.isNaN(a) || Double.isNaN(b)) {
            return Double.NaN;
        } else {
            return MathLib.min(a, b);
        }
    }
}
