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
package fr.cnes.sirius.patrius.math.analysis;

/**
 * An interface representing a multivariate real function.
 * 
 * @version $Id: MultivariateFunction.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.0
 */
public interface MultivariateFunction {

    /**
     * Compute the value for the function at the given point.
     * 
     * @param point
     *        Point at which the function must be evaluated.
     * @return the function value for the given point.
     * @throws fr.cnes.sirius.patrius.math.exception.DimensionMismatchException
     *         if the parameter's dimension is wrong for the function being evaluated.
     * @throws fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException
     *         when the activated method itself can ascertain that preconditions,
     *         specified in the API expressed at the level of the activated method,
     *         have been violated. In the vast majority of cases where Commons Math
     *         throws this exception, it is the result of argument checking of actual
     *         parameters immediately passed to a method.
     */
    double value(double[] point);
}
