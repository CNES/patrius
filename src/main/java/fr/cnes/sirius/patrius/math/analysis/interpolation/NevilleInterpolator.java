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

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunctionLagrangeForm;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NonMonotonicSequenceException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;

/**
 * Implements the <a href="http://mathworld.wolfram.com/NevillesAlgorithm.html">
 * Neville's Algorithm</a> for interpolation of real univariate functions. For
 * reference, see <b>Introduction to Numerical Analysis</b>, ISBN 038795452X,
 * chapter 2.
 * <p>
 * The actual code of Neville's algorithm is in PolynomialFunctionLagrangeForm, this class provides an easy-to-use
 * interface to it.
 * </p>
 * 
 * @version $Id: NevilleInterpolator.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 1.2
 */
public class NevilleInterpolator implements UnivariateInterpolator,
    Serializable {

    /** serializable version identifier */
    private static final long serialVersionUID = 3003707660147873733L;

    /**
     * Computes an interpolating function for the data set.
     * 
     * @param x
     *        Interpolating points.
     * @param y
     *        Interpolating values.
     * @return a function which interpolates the data set
     * @throws DimensionMismatchException
     *         if the array lengths are different.
     * @throws NumberIsTooSmallException
     *         if the number of points is less than 2.
     * @throws NonMonotonicSequenceException
     *         if two abscissae have the same
     *         value.
     */
    @Override
    public PolynomialFunctionLagrangeForm interpolate(final double[] x, final double[] y) {
        return new PolynomialFunctionLagrangeForm(x, y);
    }
}
