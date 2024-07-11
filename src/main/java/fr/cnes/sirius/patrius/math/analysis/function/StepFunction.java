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
package fr.cnes.sirius.patrius.math.analysis.function;

import java.util.Arrays;

import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NoDataException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.util.MathArrays;

/**
 * <a href="http://en.wikipedia.org/wiki/Step_function">
 * Step function</a>.
 * 
 * @since 3.0
 * @version $Id: StepFunction.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class StepFunction implements UnivariateFunction {
    /** Abscissae. */
    private final double[] abscissa;
    /** Ordinates. */
    private final double[] ordinate;

    /**
     * Builds a step function from a list of arguments and the corresponding
     * values. Specifically, returns the function h(x) defined by
     * 
     * <pre>
     * <code>
     * h(x) = y[0] for all x < x[1]
     *        y[1] for x[1] <= x < x[2]
     *        ...
     *        y[y.length - 1] for x >= x[x.length - 1]
     * </code>
     * </pre>
     * 
     * The value of {@code x[0]} is ignored, but it must be strictly less than {@code x[1]}.
     * 
     * @param x
     *        Domain values where the function changes value.
     * @param y
     *        Values of the function.
     * @throws fr.cnes.sirius.patrius.math.exception.NonMonotonicSequenceException
     *         if the {@code x} array is not sorted in strictly increasing order.
     * @throws NullArgumentException
     *         if {@code x} or {@code y} are {@code null}.
     * @throws NoDataException
     *         if {@code x} or {@code y} are zero-length.
     * @throws DimensionMismatchException
     *         if {@code x} and {@code y} do not
     *         have the same length.
     */
    public StepFunction(final double[] x,
        final double[] y) {
        if (x == null ||
            y == null) {
            throw new NullArgumentException();
        }
        if (x.length == 0 ||
            y.length == 0) {
            throw new NoDataException();
        }
        if (y.length != x.length) {
            throw new DimensionMismatchException(y.length, x.length);
        }
        MathArrays.checkOrder(x);

        this.abscissa = MathArrays.copyOf(x);
        this.ordinate = MathArrays.copyOf(y);
    }

    /** {@inheritDoc} */
    @Override
    public double value(final double x) {
        final int index = Arrays.binarySearch(this.abscissa, x);
        double fx = 0;

        if (index < -1) {
            // "x" is between "abscissa[-index-2]" and "abscissa[-index-1]".
            fx = this.ordinate[-index - 2];
        } else if (index >= 0) {
            // "x" is exactly "abscissa[index]".
            fx = this.ordinate[index];
        } else {
            // Otherwise, "x" is smaller than the first value in "abscissa"
            // (hence the returned value should be "ordinate[0]").
            fx = this.ordinate[0];
        }

        return fx;
    }
}
