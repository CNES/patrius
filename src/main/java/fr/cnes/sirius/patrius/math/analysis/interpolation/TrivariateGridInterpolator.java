/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
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
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.cnes.sirius.patrius.math.analysis.interpolation;

import fr.cnes.sirius.patrius.math.analysis.TrivariateFunction;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NoDataException;

/**
 * Interface representing a trivariate real interpolating function where the
 * sample points must be specified on a regular grid.
 * 
 * @since 2.2
 * @version $Id: TrivariateGridInterpolator.java 18108 2017-10-04 06:45:27Z bignon $
 */
public interface TrivariateGridInterpolator {
    /**
     * Compute an interpolating function for the dataset.
     * 
     * @param xval
     *        All the x-coordinates of the interpolation points, sorted
     *        in increasing order.
     * @param yval
     *        All the y-coordinates of the interpolation points, sorted
     *        in increasing order.
     * @param zval
     *        All the z-coordinates of the interpolation points, sorted
     *        in increasing order.
     * @param fval
     *        the values of the interpolation points on all the grid knots:
     *        {@code fval[i][j][k] = f(xval[i], yval[j], zval[k])}.
     * @return a function that interpolates the data set.
     * @throws NoDataException
     *         if any of the arrays has zero length.
     * @throws DimensionMismatchException
     *         if the array lengths are inconsistent.
     */
    TrivariateFunction interpolate(double[] xval, double[] yval, double[] zval,
                                   double[][][] fval);
}
