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
 * VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300) 
 * VERSION:4.6:DM:DM-2591:27/01/2021:[PATRIUS] Intigration et validation JOptimizer
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.optim.joptimizer.functions;

import java.util.Arrays;

import fr.cnes.sirius.patrius.math.linear.ArrayRealVector;
import fr.cnes.sirius.patrius.math.linear.RealVector;
import fr.cnes.sirius.patrius.math.optim.joptimizer.algebra.AlgebraUtils;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Utility class for optimization function building.
 * 
 * @author alberto trivellato (alberto.trivellato@gmail.com)
 * 
 * @since 4.6
 */
public final class FunctionsUtils {
    
    /**
     * Private constructor
     */
    private FunctionsUtils() {
    }

    /**
     * Create a circle
     * 
     * @param dim dimension of the circle
     * @param radius radius of the circle
     * @return ConvexMultivariateRealFunction
     */
    public static ConvexMultivariateRealFunction createCircle(final int dim,
            final double radius) {
        final double[] center = new double[dim];
        return createCircle(dim, radius, center);
    }

    /**
     * Create a circle
     * 
     * @param dim dimension of the circle
     * @param radius radius of the circle
     * @param center the position of the center of the circle
     * @return ConvexMultivariateRealFunction
     */
    public static ConvexMultivariateRealFunction createCircle(final int dim,
            final double radius,
            final double[] center) {

        final RealVector vecC = new ArrayRealVector(center);
        return new ConvexMultivariateRealFunction() {

            /**
             * Sum[ (x[i]-center[i])^2 ] - radius^2.
             */
            @Override
            public double value(final double[] value) {
                final RealVector x = new ArrayRealVector(value);
                final RealVector vecD = x.subtract(vecC);
                final double d = vecD.dotProduct(vecD) - MathLib.pow(radius, 2);
                return d;
            }

            /**
             * Function gradient at point X.
             */
            @Override
            public double[] gradient(final double[] value) {
                final RealVector x = new ArrayRealVector(value);
                final RealVector vecD = x.subtract(vecC);
                return vecD.mapMultiply(2).toArray();
            }

            /**
             * Function hessian at point X
             */
            @Override
            public double[][] hessian(final double[] value) {
                final double[] d = new double[dim];
                Arrays.fill(d, 2);
                return AlgebraUtils.diagonal(new ArrayRealVector(d)).getData(false);
            }

            /**
             * Get dimension
             */
            @Override
            public int getDim() {
                return dim;
            }
        };
    }
}
