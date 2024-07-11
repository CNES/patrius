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

import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.util.MathArrays;
import fr.cnes.sirius.patrius.math.util.MathUtils;

/**
 * Adapter for classes implementing the {@link UnivariateInterpolator} interface.
 * The data to be interpolated is assumed to be periodic. Thus values that are
 * outside of the range can be passed to the interpolation function: They will
 * be wrapped into the initial range before being passed to the class that
 * actually computes the interpolation.
 * 
 * @version $Id: UnivariatePeriodicInterpolator.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class UnivariatePeriodicInterpolator
    implements UnivariateInterpolator {
    /** Default number of extension points of the samples array. */
    public static final int DEFAULT_EXTEND = 5;
    /** Interpolator. */
    private final UnivariateInterpolator interpolator;
    /** Period. */
    private final double period;
    /** Number of extension points. */
    private final int extend;

    /**
     * Builds an interpolator.
     * 
     * @param interpolatorIn
     *        Interpolator.
     * @param periodIn
     *        Period.
     * @param extendIn
     *        Number of points to be appended at the beginning and
     *        end of the sample arrays in order to avoid interpolation failure at
     *        the (periodic) boundaries of the orginal interval. The value is the
     *        number of sample points which the original {@code interpolator} needs
     *        on each side of the interpolated point.
     */
    public UnivariatePeriodicInterpolator(final UnivariateInterpolator interpolatorIn,
        final double periodIn,
        final int extendIn) {
        this.interpolator = interpolatorIn;
        this.period = periodIn;
        this.extend = extendIn;
    }

    /**
     * Builds an interpolator.
     * Uses {@link #DEFAULT_EXTEND} as the number of extension points on each side
     * of the original abscissae range.
     * 
     * @param interpolatorIn
     *        Interpolator.
     * @param periodIn
     *        Period.
     */
    public UnivariatePeriodicInterpolator(final UnivariateInterpolator interpolatorIn,
        final double periodIn) {
        this(interpolatorIn, periodIn, DEFAULT_EXTEND);
    }

    /**
     * {@inheritDoc}
     * 
     * @throws NumberIsTooSmallException
     *         if the number of extension points
     *         iss larger then the size of {@code xval}.
     */
    @Override
    public UnivariateFunction interpolate(final double[] xval,
                                          final double[] yval) {
        if (xval.length < this.extend) {
            // Exception
            throw new NumberIsTooSmallException(xval.length, this.extend, true);
        }

        // Check
        MathArrays.checkOrder(xval);
        final double offset = xval[0];

        final int len = xval.length + this.extend * 2;
        final double[] x = new double[len];
        final double[] y = new double[len];
        for (int i = 0; i < xval.length; i++) {
            final int index = i + this.extend;
            x[index] = MathUtils.reduce(xval[i], this.period, offset);
            y[index] = yval[i];
        }

        // Wrap to enable interpolation at the boundaries.
        for (int i = 0; i < this.extend; i++) {
            int index = xval.length - this.extend + i;
            x[i] = MathUtils.reduce(xval[index], this.period, offset) - this.period;
            y[i] = yval[index];

            index = len - this.extend + i;
            x[index] = MathUtils.reduce(xval[i], this.period, offset) + this.period;
            y[index] = yval[i];
        }

        // Sort data
        MathArrays.sortInPlace(x, y);

        // Build interpolating function
        final UnivariateFunction f = this.interpolator.interpolate(x, y);
        return new UnivariateFunction(){
            /** {@inheritDoc} */
            @Override
            public double value(final double x) {
                return f.value(MathUtils.reduce(x, UnivariatePeriodicInterpolator.this.period, offset));
            }
        };
    }
}
