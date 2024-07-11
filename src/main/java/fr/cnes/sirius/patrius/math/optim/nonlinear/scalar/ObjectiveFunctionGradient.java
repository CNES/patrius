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
package fr.cnes.sirius.patrius.math.optim.nonlinear.scalar;

import fr.cnes.sirius.patrius.math.analysis.MultivariateVectorFunction;
import fr.cnes.sirius.patrius.math.optim.OptimizationData;

/**
 * Gradient of the scalar function to be optimized.
 * 
 * @version $Id: ObjectiveFunctionGradient.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.1
 */
public class ObjectiveFunctionGradient implements OptimizationData {
    /** Function to be optimized. */
    private final MultivariateVectorFunction gradient;

    /**
     * @param g
     *        Gradient of the function to be optimized.
     */
    public ObjectiveFunctionGradient(final MultivariateVectorFunction g) {
        this.gradient = g;
    }

    /**
     * Gets the gradient of the function to be optimized.
     * 
     * @return the objective function gradient.
     */
    public MultivariateVectorFunction getObjectiveFunctionGradient() {
        return this.gradient;
    }
}
