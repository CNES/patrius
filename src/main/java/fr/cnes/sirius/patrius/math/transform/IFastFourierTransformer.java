/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
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
 *
 *
 * @history created 21/08/2014
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:285:21/08/2014: (creation) FFT adapted to all orders
 * END-HISTORY
 *
 */
package fr.cnes.sirius.patrius.math.transform;

import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.complex.Complex;

/**
 * This interface gathers all the FFT algorithms of this library.
 * 
 * @version $Id: IFastFourierTransformer.java 18108 2017-10-04 06:45:27Z bignon $
 * 
 * @since 2.3
 * 
 */
public interface IFastFourierTransformer {

    /**
     * Returns the (forward, inverse) transform of the specified real data set.
     * 
     * @param f
     *        the real data array to be transformed
     * @param type
     *        the type of transform (forward, inverse) to be performed
     * @return the complex transformed array
     */
    Complex[] transform(final double[] f, final TransformType type);

    /**
     * Returns the (forward, inverse) transform of the specified complex data set.
     * 
     * @param f
     *        the complex data array to be transformed
     * @param type
     *        the type of transform (forward, inverse) to be performed
     * @return the complex transformed array
     */
    Complex[] transform(final Complex[] f, final TransformType type);

    /**
     * Returns the (forward, inverse) transform of the specified real function,
     * sampled on the specified interval.
     * 
     * @param f
     *        the function to be sampled and transformed
     * @param min
     *        the (inclusive) lower bound for the interval
     * @param max
     *        the (exclusive) upper bound for the interval
     * @param n
     *        the number of sample points
     * @param type
     *        the type of transform (forward, inverse) to be performed
     * @return the complex transformed array
     * @throws fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException
     *         if the lower bound is greater than, or equal to the upper bound
     * @throws fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException
     *         if the number of sample points {@code n} is negative
     */
    Complex[] transform(final UnivariateFunction f,
                        final double min, final double max, final int n,
                        final TransformType type);

}
