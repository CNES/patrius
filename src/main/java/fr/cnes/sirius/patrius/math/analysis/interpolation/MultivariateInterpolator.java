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
package fr.cnes.sirius.patrius.math.analysis.interpolation;

import fr.cnes.sirius.patrius.math.analysis.MultivariateFunction;

/**
 * Interface representing a univariate real interpolating function.
 * 
 * @since 2.1
 * @version $Id: MultivariateInterpolator.java 18108 2017-10-04 06:45:27Z bignon $
 */
public interface MultivariateInterpolator {

    /**
     * Computes an interpolating function for the data set.
     * 
     * @param xval
     *        the arguments for the interpolation points. {@code xval[i][0]} is the first component of interpolation
     *        point {@code i}, {@code xval[i][1]} is the second component, and so on
     *        until {@code xval[i][d-1]}, the last component of that interpolation
     *        point (where {@code d} is thus the dimension of the space).
     * @param yval
     *        the values for the interpolation points
     * @return a function which interpolates the data set
     * @throws fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException
     *         if the arguments violate assumptions made by the interpolation
     *         algorithm.
     * @throws fr.cnes.sirius.patrius.math.exception.DimensionMismatchException
     *         when the array dimensions are not consistent.
     * @throws fr.cnes.sirius.patrius.math.exception.NoDataException
     *         if an
     *         array has zero-length.
     * @throws fr.cnes.sirius.patrius.math.exception.NullArgumentException
     *         if
     *         the arguments are {@code null}.
     */
    MultivariateFunction interpolate(double[][] xval, double[] yval);
}
