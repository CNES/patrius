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
package fr.cnes.sirius.patrius.math.optim.nonlinear.vector;

import fr.cnes.sirius.patrius.math.linear.DiagonalMatrix;
import fr.cnes.sirius.patrius.math.linear.NonSquareMatrixException;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.optim.OptimizationData;

/**
 * Weight matrix of the residuals between model and observations. <br/>
 * Immutable class.
 * 
 * @version $Id: Weight.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.1
 */
public class Weight implements OptimizationData {
    /** Weight matrix. */
    private final RealMatrix weightMatrix;

    /**
     * Creates a diagonal weight matrix.
     * 
     * @param weight
     *        List of the values of the diagonal.
     */
    public Weight(final double[] weight) {
        this.weightMatrix = new DiagonalMatrix(weight);
    }

    /**
     * @param weight
     *        Weight matrix.
     * @throws NonSquareMatrixException
     *         if the argument is not
     *         a square matrix.
     */
    public Weight(final RealMatrix weight) {
        if (weight.getColumnDimension() != weight.getRowDimension()) {
            throw new NonSquareMatrixException(weight.getColumnDimension(),
                weight.getRowDimension());
        }

        this.weightMatrix = weight.copy();
    }

    /**
     * Gets the initial guess.
     * 
     * @return the initial guess.
     */
    public RealMatrix getWeight() {
        return this.weightMatrix.copy();
    }
}
