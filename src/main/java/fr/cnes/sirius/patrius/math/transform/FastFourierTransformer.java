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
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:285:21/08/2014: FFT adapted to all orders
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.math.transform;

import fr.cnes.sirius.patrius.math.complex.Complex;
import fr.cnes.sirius.patrius.math.util.ArithmeticUtils;

/**
 * This class allows the computation of a Fast Fourier Transform for all kind (odd or powers of two) orders.
 * 
 * @version $Id: FastFourierTransformer.java 18108 2017-10-04 06:45:27Z bignon $
 * 
 * @since 2.3
 * 
 */

public class FastFourierTransformer extends AbstractFastFourierTransformer {

    /**
     * Constructor of the class FastFourierTransformer, inherited from the one of the abstract class
     * AbstractFastFourierTransformer
     * 
     * @param dftNormalization
     *        an enum with two possible values : STANDARD or UNITARY
     */
    public FastFourierTransformer(final DftNormalization dftNormalization) {
        super(dftNormalization);
    }

    /** {@inheritDoc} */
    @Override
    public Complex[] transform(final double[] f, final TransformType type) {

        // CHECKSTYLE: stop IllegalType check
        // Reason: Commons-Math code kept as such
        final AbstractFastFourierTransformer algo;
        // CHECKSTYLE: resume IllegalType check

        final int n = f.length;
        // power of 2 algorithm
        if (ArithmeticUtils.isPowerOfTwo(n)) {
            algo = new FFTpowerOfTwoOrder(this.getNormalization());
        } else {
            // odd algorithm
            algo = new FFToddOrder(this.getNormalization());
        }

        return algo.transform(f, type);
    }

    /** {@inheritDoc} */
    @Override
    public Complex[] transform(final Complex[] f, final TransformType type) {

        // CHECKSTYLE: stop IllegalType check
        // Reason: Commons-Math code kept as such
        final AbstractFastFourierTransformer algo;
        // CHECKSTYLE: resume IllegalType check

        final int n = f.length;
        // power of 2 algorithm
        if (ArithmeticUtils.isPowerOfTwo(n)) {
            algo = new FFTpowerOfTwoOrder(this.getNormalization());
        } else {
            // odd algorithm
            algo = new FFToddOrder(this.getNormalization());
        }

        return algo.transform(f, type);
    }

}
