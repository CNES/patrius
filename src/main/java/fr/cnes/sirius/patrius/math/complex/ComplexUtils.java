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
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.complex;

import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Static implementations of common {@link fr.cnes.sirius.patrius.math.complex.Complex} utilities functions.
 * 
 * @version $Id: ComplexUtils.java 18108 2017-10-04 06:45:27Z bignon $
 */
public final class ComplexUtils {

    /**
     * Default constructor.
     */
    private ComplexUtils() {
    }

    /**
     * Creates a complex number from the given polar representation.
     * <p>
     * The value returned is <code>r&middot;e<sup>i&middot;theta</sup></code>, computed as
     * <code>r&middot;cos(theta) + r&middot;sin(theta)i</code>
     * </p>
     * <p>
     * If either <code>r</code> or <code>theta</code> is NaN, or <code>theta</code> is infinite, {@link Complex#NaN} is
     * returned.
     * </p>
     * <p>
     * If <code>r</code> is infinite and <code>theta</code> is finite, infinite or NaN values may be returned in parts
     * of the result, following the rules for double arithmetic.
     * 
     * <pre>
     * Examples:
     * <code>
     * polar2Complex(INFINITY, &pi;/4) = INFINITY + INFINITY i
     * polar2Complex(INFINITY, 0) = INFINITY + NaN i
     * polar2Complex(INFINITY, -&pi;/4) = INFINITY - INFINITY i
     * polar2Complex(INFINITY, 5&pi;/4) = -INFINITY - INFINITY i </code>
     * </pre>
     * 
     * </p>
     * 
     * @param r
     *        the modulus of the complex number to create
     * @param theta
     *        the argument of the complex number to create
     * @return <code>r&middot;e<sup>i&middot;theta</sup></code>
     * @throws MathIllegalArgumentException
     *         if {@code r} is negative.
     * @since 1.1
     */
    public static Complex polar2Complex(final double r, final double theta) {
        if (r < 0) {
            throw new MathIllegalArgumentException(
                PatriusMessages.NEGATIVE_COMPLEX_MODULE, r);
        }
        final double[] sincos = MathLib.sinAndCos(theta);
        final double sin = sincos[0];
        final double cos = sincos[1];
        return new Complex(r * cos, r * sin);
    }

    /**
     * Convert an array of primitive doubles to an array of {@code Complex} objects.
     * 
     * @param real
     *        Array of numbers to be converted to their {@code Complex} equivalent.
     * @return an array of {@code Complex} objects.
     * 
     * @since 3.1
     */
    public static Complex[] convertToComplex(final double[] real) {
        final Complex[] c = new Complex[real.length];
        for (int i = 0; i < real.length; i++) {
            c[i] = new Complex(real[i], 0);
        }

        return c;
    }
}
