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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis;

/**
 * An interface representing a real function that depends on one independent
 * variable plus some extra parameters.
 * 
 * @since 3.0
 * @version $Id: ParametricUnivariateFunction.java 18108 2017-10-04 06:45:27Z bignon $
 */
public interface ParametricUnivariateFunction {
    /**
     * Compute the value of the function.
     * 
     * @param x
     *        Point for which the function value should be computed.
     * @param parameters
     *        Function parameters.
     * @return the value.
     */
    double value(double x, double... parameters);

    /**
     * Compute the gradient of the function with respect to its parameters.
     * 
     * @param x
     *        Point for which the function value should be computed.
     * @param parameters
     *        Function parameters.
     * @return the value.
     */
    double[] gradient(double x, double... parameters);
}
