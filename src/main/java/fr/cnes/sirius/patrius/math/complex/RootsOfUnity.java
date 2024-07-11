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
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.complex;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalStateException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.exception.ZeroException;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * A helper class for the computation and caching of the {@code n}-th roots of
 * unity.
 * 
 * @version $Id: RootsOfUnity.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
@SuppressWarnings("PMD.NullAssignment")
public class RootsOfUnity implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 20120201L;

    /** Number of roots of unity. */
    private int omegaCount;

    /** Real part of the roots. */
    private double[] omegaReal;

    /**
     * Imaginary part of the {@code n}-th roots of unity, for positive values
     * of {@code n}. In this array, the roots are stored in counter-clockwise
     * order.
     */
    private double[] omegaImaginaryCounterClockwise;

    /**
     * Imaginary part of the {@code n}-th roots of unity, for negative values
     * of {@code n}. In this array, the roots are stored in clockwise order.
     */
    private double[] omegaImaginaryClockwise;

    /**
     * {@code true} if {@link #computeRoots(int)} was called with a positive
     * value of its argument {@code n}. In this case, counter-clockwise ordering
     * of the roots of unity should be used.
     */
    private boolean counterClockWise;

    /**
     * Build an engine for computing the {@code n}-th roots of unity.
     */
    public RootsOfUnity() {

        this.omegaCount = 0;
        this.omegaReal = null;
        this.omegaImaginaryCounterClockwise = null;
        this.omegaImaginaryClockwise = null;
        this.counterClockWise = true;
    }

    /**
     * Returns {@code true} if {@link #computeRoots(int)} was called with a
     * positive value of its argument {@code n}. If {@code true}, then
     * counter-clockwise ordering of the roots of unity should be used.
     * 
     * @return {@code true} if the roots of unity are stored in
     *         counter-clockwise order
     * @throws MathIllegalStateException
     *         if no roots of unity have been computed
     *         yet
     */
    public synchronized boolean isCounterClockWise() {

        if (this.omegaCount == 0) {
            throw new MathIllegalStateException(
                PatriusMessages.ROOTS_OF_UNITY_NOT_COMPUTED_YET);
        }
        return this.counterClockWise;
    }

    /**
     * <p>
     * Computes the {@code n}-th roots of unity. The roots are stored in {@code omega[]}, such that
     * {@code omega[k] = w ^ k}, where {@code k = 0, ..., n - 1}, {@code w = exp(2 * pi * i / n)} and
     * {@code i = sqrt(-1)}.
     * </p>
     * <p>
     * Note that {@code n} can be positive of negative
     * </p>
     * <ul>
     * <li>{@code abs(n)} is always the number of roots of unity.</li>
     * <li>If {@code n > 0}, then the roots are stored in counter-clockwise order.</li>
     * <li>If {@code n < 0}, then the roots are stored in clockwise order.</p>
     * </ul>
     * 
     * @param n
     *        the (signed) number of roots of unity to be computed
     * @throws ZeroException
     *         if {@code n = 0}
     */
    public synchronized void computeRoots(final int n) {

        if (n == 0) {
            // Exception
            throw new ZeroException(
                PatriusMessages.CANNOT_COMPUTE_0TH_ROOT_OF_UNITY);
        }

        this.counterClockWise = n > 0;

        // avoid repetitive calculations
        final int absN = MathLib.abs(n);

        if (absN == this.omegaCount) {
            return;
        }

        // calculate everything from scratch
        final double t = 2.0 * FastMath.PI / absN;
        final double[] sincos = MathLib.sinAndCos(t);
        final double sinT = sincos[0];
        final double cosT = sincos[1];
        this.omegaReal = new double[absN];
        this.omegaImaginaryCounterClockwise = new double[absN];
        this.omegaImaginaryClockwise = new double[absN];
        this.omegaReal[0] = 1.0;
        this.omegaImaginaryCounterClockwise[0] = 0.0;
        this.omegaImaginaryClockwise[0] = 0.0;
        for (int i = 1; i < absN; i++) {
            // Compute all roots
            //
            this.omegaReal[i] = this.omegaReal[i - 1] * cosT -
                this.omegaImaginaryCounterClockwise[i - 1] * sinT;
            this.omegaImaginaryCounterClockwise[i] = this.omegaReal[i - 1] * sinT +
                this.omegaImaginaryCounterClockwise[i - 1] * cosT;
            this.omegaImaginaryClockwise[i] = -this.omegaImaginaryCounterClockwise[i];
        }
        this.omegaCount = absN;
    }

    /**
     * Get the real part of the {@code k}-th {@code n}-th root of unity.
     * 
     * @param k
     *        index of the {@code n}-th root of unity
     * @return real part of the {@code k}-th {@code n}-th root of unity
     * @throws MathIllegalStateException
     *         if no roots of unity have been
     *         computed yet
     * @throws MathIllegalArgumentException
     *         if {@code k} is out of range
     */
    public synchronized double getReal(final int k) {

        if (this.omegaCount == 0) {
            throw new MathIllegalStateException(
                PatriusMessages.ROOTS_OF_UNITY_NOT_COMPUTED_YET);
        }
        if ((k < 0) || (k >= this.omegaCount)) {
            throw new OutOfRangeException(
                PatriusMessages.OUT_OF_RANGE_ROOT_OF_UNITY_INDEX,
                Integer.valueOf(k),
                Integer.valueOf(0),
                Integer.valueOf(this.omegaCount - 1));
        }

        return this.omegaReal[k];
    }

    /**
     * Get the imaginary part of the {@code k}-th {@code n}-th root of unity.
     * 
     * @param k
     *        index of the {@code n}-th root of unity
     * @return imaginary part of the {@code k}-th {@code n}-th root of unity
     * @throws MathIllegalStateException
     *         if no roots of unity have been
     *         computed yet
     * @throws OutOfRangeException
     *         if {@code k} is out of range
     */
    public synchronized double getImaginary(final int k) {

        if (this.omegaCount == 0) {
            throw new MathIllegalStateException(
                PatriusMessages.ROOTS_OF_UNITY_NOT_COMPUTED_YET);
        }
        if ((k < 0) || (k >= this.omegaCount)) {
            throw new OutOfRangeException(
                PatriusMessages.OUT_OF_RANGE_ROOT_OF_UNITY_INDEX,
                Integer.valueOf(k),
                Integer.valueOf(0),
                Integer.valueOf(this.omegaCount - 1));
        }

        return this.counterClockWise ? this.omegaImaginaryCounterClockwise[k] :
            this.omegaImaginaryClockwise[k];
    }

    /**
     * Returns the number of roots of unity currently stored. If {@link #computeRoots(int)} was called with {@code n},
     * then this method
     * returns {@code abs(n)}. If no roots of unity have been computed yet, this
     * method returns 0.
     * 
     * @return the number of roots of unity currently stored
     */
    public synchronized int getNumberOfRoots() {
        return this.omegaCount;
    }
}
