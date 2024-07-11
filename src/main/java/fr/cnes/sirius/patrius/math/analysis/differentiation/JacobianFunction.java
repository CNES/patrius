/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
 * Copyright 2011-2022 CNES
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
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.differentiation;

import fr.cnes.sirius.patrius.math.analysis.MultivariateMatrixFunction;

/**
 * Class representing the Jacobian of a multivariate vector function.
 * <p>
 * The rows iterate on the model functions while the columns iterate on the parameters; thus, the numbers of rows is
 * equal to the dimension of the underlying function vector value and the number of columns is equal to the number of
 * free parameters of the underlying function.
 * </p>
 * 
 * @version $Id: JacobianFunction.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.1
 */
public class JacobianFunction implements MultivariateMatrixFunction {

    /** Underlying vector-valued function. */
    private final MultivariateDifferentiableVectorFunction f;

    /**
     * Simple constructor.
     * 
     * @param fIn
     *        underlying vector-valued function
     */
    public JacobianFunction(final MultivariateDifferentiableVectorFunction fIn) {
        this.f = fIn;
    }

    /** {@inheritDoc} */
    @Override
    public double[][] value(final double[] point) {

        // set up parameters
        final DerivativeStructure[] dsX = new DerivativeStructure[point.length];
        // loop on the point length
        for (int i = 0; i < point.length; ++i) {
            dsX[i] = new DerivativeStructure(point.length, 1, i, point[i]);
        }

        // compute the derivatives
        final DerivativeStructure[] dsY = this.f.value(dsX);

        // extract the Jacobian
        // initialize y array
        final double[][] y = new double[dsY.length][point.length];
        final int[] orders = new int[point.length];
        for (int i = 0; i < dsY.length; ++i) {
            for (int j = 0; j < point.length; ++j) {
                orders[j] = 1;
                y[i][j] = dsY[i].getPartialDerivative(orders);
                orders[j] = 0;
            }
        }

        return y;

    }

}
