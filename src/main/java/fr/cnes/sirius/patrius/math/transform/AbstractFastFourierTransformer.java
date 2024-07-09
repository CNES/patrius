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

import fr.cnes.sirius.patrius.math.analysis.FunctionUtils;
import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.complex.Complex;
import fr.cnes.sirius.patrius.math.exception.MathIllegalStateException;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * This abstract class is common to all FFT algorithms of this library.
 * 
 * @version $Id: AbstractFastFourierTransformer.java 18108 2017-10-04 06:45:27Z bignon $
 * 
 * @since 2.3
 * 
 */
public abstract class AbstractFastFourierTransformer implements IFastFourierTransformer {

    /** The type of DFT to be performed. */
    private final DftNormalization normalization;

    /**
     * Creates a new instance of this class, with various normalization
     * conventions.
     * 
     * @param dftNormalization
     *        the type of normalization to be applied to the
     *        transformed data
     */
    public AbstractFastFourierTransformer(final DftNormalization dftNormalization) {
        this.normalization = dftNormalization;
    }

    /** {@inheritDoc} */
    @Override
    public Complex[] transform(final UnivariateFunction f, final double min, final double max, final int n,
                               final TransformType type) {
        final double[] data = FunctionUtils.sample(f, min, max, n);
        return this.transform(data, type);
    }

    /**
     * Applies the proper normalization to the specified transformed data.
     * 
     * @param dataRI
     *        the unscaled transformed data
     * @param normalization
     *        the normalization to be applied
     * @param type
     *        the type of transform (forward, inverse) which resulted in the specified data
     */
    protected static void normalizeTransformedData(final double[][] dataRI,
                                                   final DftNormalization normalization, final TransformType type) {

        final double[] dataR = dataRI[0];
        final double[] dataI = dataRI[1];
        final int n = dataR.length;

        switch (normalization) {
            case STANDARD:
                if (type == TransformType.INVERSE) {
                    final double scaleFactor = 1.0 / n;
                    for (int i = 0; i < n; i++) {
                        dataR[i] *= scaleFactor;
                        dataI[i] *= scaleFactor;
                    }
                }
                break;
            case UNITARY:
                final double scaleFactor = 1.0 / MathLib.sqrt(n);
                for (int i = 0; i < n; i++) {
                    dataR[i] *= scaleFactor;
                    dataI[i] *= scaleFactor;
                }
                break;
            default:
                /*
                 * This should never occur in normal conditions. However this
                 * clause has been added as a safeguard if other types of
                 * normalizations are ever implemented, and the corresponding
                 * test is forgotten in the present switch.
                 */
                throw new MathIllegalStateException();
        }
        dataRI[0] = dataR;
        dataRI[1] = dataI;
    }

    /**
     * Gets the private attribute normalization.
     * 
     * @return normalization : an enum DftNormalization equal to STANDARD or UNITARY
     */
    public DftNormalization getNormalization() {
        return this.normalization;
    }

}
