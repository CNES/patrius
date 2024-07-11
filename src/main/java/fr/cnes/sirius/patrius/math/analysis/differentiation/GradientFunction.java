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
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.differentiation;

import fr.cnes.sirius.patrius.math.analysis.MultivariateVectorFunction;

/**
 * Class representing the gradient of a multivariate function.
 * <p>
 * The vectorial components of the function represent the derivatives with respect to each function parameters.
 * </p>
 * 
 * @version $Id: GradientFunction.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.1
 */
public class GradientFunction implements MultivariateVectorFunction {

    /** Underlying real-valued function. */
    private final MultivariateDifferentiableFunction f;

    /**
     * Simple constructor.
     * 
     * @param fIn
     *        underlying real-valued function
     */
    public GradientFunction(final MultivariateDifferentiableFunction fIn) {
        this.f = fIn;
    }

    /** {@inheritDoc} */
    @Override
    public double[] value(final double[] point) {

        // set up parameters
        final DerivativeStructure[] dsX = new DerivativeStructure[point.length];
        for (int i = 0; i < point.length; ++i) {
            dsX[i] = new DerivativeStructure(point.length, 1, i, point[i]);
        }

        // compute the derivatives
        final DerivativeStructure dsY = this.f.value(dsX);

        // extract the gradient
        final double[] y = new double[point.length];
        final int[] orders = new int[point.length];
        for (int i = 0; i < point.length; ++i) {
            orders[i] = 1;
            y[i] = dsY.getPartialDerivative(orders);
            orders[i] = 0;
        }

        return y;

    }

}
