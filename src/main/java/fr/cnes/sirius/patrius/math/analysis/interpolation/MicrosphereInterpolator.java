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
package fr.cnes.sirius.patrius.math.analysis.interpolation;

import fr.cnes.sirius.patrius.math.analysis.MultivariateFunction;
import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.random.UnitSphereRandomVectorGenerator;

/**
 * Interpolator that implements the algorithm described in <em>William Dudziak</em>'s
 * <a href="http://www.dudziak.com/microsphere.pdf">MS thesis</a>.
 * 
 * @since 2.1
 * @version $Id: MicrosphereInterpolator.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class MicrosphereInterpolator
    implements MultivariateInterpolator {
    /**
     * Default number of surface elements that composes the microsphere.
     */
    public static final int DEFAULT_MICROSPHERE_ELEMENTS = 2000;
    /**
     * Default exponent used the weights calculation.
     */
    public static final int DEFAULT_BRIGHTNESS_EXPONENT = 2;
    /**
     * Number of surface elements of the microsphere.
     */
    private final int microsphereElements;
    /**
     * Exponent used in the power law that computes the weights of the
     * sample data.
     */
    private final int brightnessExponent;

    /**
     * Create a microsphere interpolator with default settings.
     * Calling this constructor is equivalent to call {@link #MicrosphereInterpolator(int, int)
     * MicrosphereInterpolator(MicrosphereInterpolator.DEFAULT_MICROSPHERE_ELEMENTS,
     * MicrosphereInterpolator.DEFAULT_BRIGHTNESS_EXPONENT)}.
     */
    public MicrosphereInterpolator() {
        this(DEFAULT_MICROSPHERE_ELEMENTS, DEFAULT_BRIGHTNESS_EXPONENT);
    }

    /**
     * Create a microsphere interpolator.
     * 
     * @param elements
     *        Number of surface elements of the microsphere.
     * @param exponent
     *        Exponent used in the power law that computes the
     *        weights (distance dimming factor) of the sample data.
     * @throws NotPositiveException
     *         if {@code exponent < 0}.
     * @throws NotStrictlyPositiveException
     *         if {@code elements <= 0}.
     */
    public MicrosphereInterpolator(final int elements,
        final int exponent) {
        if (exponent < 0) {
            throw new NotPositiveException(exponent);
        }
        if (elements <= 0) {
            throw new NotStrictlyPositiveException(elements);
        }

        this.microsphereElements = elements;
        this.brightnessExponent = exponent;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MultivariateFunction interpolate(final double[][] xval,
                                            final double[] yval) {
        final UnitSphereRandomVectorGenerator rand = new UnitSphereRandomVectorGenerator(xval[0].length);
        return new MicrosphereInterpolatingFunction(xval, yval,
            this.brightnessExponent,
            this.microsphereElements,
            rand);
    }
}
