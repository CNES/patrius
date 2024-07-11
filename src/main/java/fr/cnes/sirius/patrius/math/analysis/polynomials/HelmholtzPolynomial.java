/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
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
 *
 * @history Created 02/10/2013
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:90:02/10/2013:Created Helmholtz polynomial
 * VERSION::FA:530:02/02/2016:Corrected anomaly at creation of a Balmino model with order 0, degree 1
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::FA:1405:22/11/2017: computation speed-up
 * VERSION::FA:1475:20/03/2018: computation speed-up
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.polynomials;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * This class represents Helmholtz polynomial.
 * 
 * @version $Id: HelmholtzPolynomial.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.1.2
 */
public final class HelmholtzPolynomial implements Serializable {

    /** Serial UID. */
    private static final long serialVersionUID = -190706229774674033L;

    /** MathLib.sqrt(3) */
    private static final double SQ3 = MathLib.sqrt(3);
    /** MathLib.sqrt(5) */
    private static final double SQ5 = MathLib.sqrt(5);

    /** max order */
    private final int mMax;
    /** max degree */
    private final int lMax;
    /** Hlm coefficients */
    private final double[][] ph;
    /** H'lm coefficients */
    private final double[][] dpph;
    /** H''lm coefficients */
    private final double[][] dsph;
    /** Normalization coefficients */
    private final double[][] alpha;

    /**
     * Create a Helmholtz polynomial with given degree and order
     * 
     * @param degree max degree
     * @param order max order
     */
    public HelmholtzPolynomial(final int degree, final int order) {
        // check lengths
        if (degree < order) {
            throw new IllegalArgumentException();
        }
        // store order and degree
        this.mMax = order;
        this.lMax = degree;
        this.ph = new double[this.lMax + 1][this.mMax + 1];
        this.dpph = new double[this.lMax + 1][this.mMax + 1];
        this.dsph = new double[this.lMax + 1][this.mMax + 1];
        this.alpha = new double[this.lMax + 1][this.mMax + 1];
        // compute constant coefficients
        this.computeConstantCoefficients();
    }

    /**
     * Calculate the value of the polynomial in a given point.
     * 
     * @param point the given point
     * @return value of polynomial
     */
    public double computeHelmholtzPolynomial(final Vector3D point) {
        /*
         * The constant part of the polynomials remains untouched by this method!
         */
        // degree and order
        int l;
        int m;
        int l1;
        int l2;
        double alphalm;
        double alphal1m;
        double phl1m;
        double dpphl1m;

        // director cosine
        final double zu = point.getZ() / point.getNorm();
        // Initialize recurrence
        // The only non constant term with l < 2 to be initialized is H10 = sin(phi) * MathLib.sqrt(3)
        if (this.lMax > 0) {
            this.ph[1][0] = zu * SQ3;
        }
        // Everything below order 2 is initialized
        for (l = 2; l <= this.lMax; l++) {
            l1 = l - 1;
            l2 = l - 2;
            // calculate for m < l - 1
            for (m = 0; m < MathLib.min(l1, this.mMax); m++) {
                alphalm = this.alpha[l][m];
                alphal1m = this.alpha[l1][m];
                phl1m = this.ph[l1][m];
                dpphl1m = this.dpph[l1][m];
                this.ph[l][m] = alphalm * (zu * phl1m - this.ph[l2][m] / alphal1m);
                this.dpph[l][m] = alphalm * (zu * dpphl1m + phl1m - this.dpph[l2][m] / alphal1m);
                this.dsph[l][m] = alphalm * (zu * this.dsph[l1][m] + 2 * dpphl1m - this.dsph[l2][m] / alphal1m);
            }
            // for m = l - 1
            if (l1 <= this.mMax) {
                this.ph[l][l1] = MathLib.sqrt(2 * l1 + 3) * zu * this.ph[l1][l1];
                /*
                 * for m = l - 1 and m = l
                 * Terms are constant and are calculated previously
                 * dpph[l][l - 1] = MathLib.sqrt(2 * (l - 1) + 3) * ph[l - 1][l - 1]; dsph[l][l - 1] = 0;
                 * ph[l][l] = MathLib.sqrt(1 + 1 / (2 * l)) * ph[l - 1][l - 1]; dpph[l][l] = 0; dsph[l][l] =
                 * 0;
                 */
            }
        }
        return 0;
    }

    /**
     * Calculate the constant coefficients of the polynomial as per
     * 
     * <pre>
     * ph(m,m)        = MathLib.sqrt(1 + 1/2m) * ph(m-1,m-1)
     * dpph(m+1,m)    = MathLib.sqrt(2m+3) * ph(m,m)
     * alpha(l,m)     = MathLib.sqrt( (2l+1)*(2l-1) / (l-m)*(l+m) )        for l >= m + 1
     *                = MathLib.sqrt( (2l+1)*(2l-1) )                      for l = m
     * </pre>
     * 
     * The a(l,m) are represents as a unidimentional tab. The dimension of this tab is:
     * 
     * <pre>
     * (lmax * (lmax + 1) - ((lmax + 1) - (mmax + 1)) * ((lmax + 1) - (mmax + 1) + 1)) / 2
     * </pre>
     * 
     * For a pair l,m given, we have:
     * 
     * <pre>
     * a(l, m) = aph(i)
     * </pre>
     * 
     * with i :
     * 
     * <pre>
     * i = (lmax * (lmax + 1) - ((lmax + 1) - m) * ((lmax + 1) - m + 1)) / 2 + (l - m) + 1
     * </pre>
     * 
     * Initial conditions are :
     * 
     * <pre>
     * ph(0,0)     = 1.0
     * ph(1,1)     = MathLib.sqrt(3)
     * </pre>
     * 
     */
    private void computeConstantCoefficients() {

        // order and degree
        int l = 0;
        int m = 0;

        /*
         * Recurrence relation
         * ph(m,m) = MathLib.sqrt(1 + 1/2m) * ph(m-1,m-1)
         */

        // Initialization
        if (this.mMax > 0) {
            /*
             * H00 = 1 <------- H'00 = 0 H''00 = 0
             * H10 with recurrence (non constant) H'10 = MathLib.sqrt(3) <------- H''10 = 0
             * H11 = MathLib.sqrt(3) <------- H'11 = 0 H''11 = 0
             * H20 with recurrence (non constant) H'20 with recurrence (non constant) H''20 with
             * recurrence (non constant)
             * H21 with recurrence (non constant) H'21 = MathLib.sqrt(3) * MathLib.sqrt(5) <------- H''21 = 0
             * everything else is calculated later on, either in this method for constants Hmm and
             * H'_m+1_m with m >= 2 or in the main method for the rest
             */
            this.ph[0][0] = 1;
            this.dpph[1][0] = SQ3;
            this.ph[1][1] = SQ3;
        }
        if (this.mMax > 1) {
            this.dpph[2][1] = SQ5 * SQ3;
            // Calculating Hmm and H'_m+1_m for m >= 2
            for (m = 2; m <= this.mMax; m++) {
                // At loop entry, ph[m - 1][m - 1] exists and is correct
                this.ph[m][m] = this.ph[m - 1][m - 1] * MathLib.sqrt(1. + 1. / (m * 2.));
                // H'(m)(m) = 0
                // H'(m+1)(m) = MathLib.sqrt(2m + 3)*ph(m)(m)
                if (m + 1 <= this.lMax) {
                    this.dpph[m + 1][m] = MathLib.sqrt(2 * m + 3) * this.ph[m][m];
                }
            }
        }

        // Calculate normalization coefficients
        // alpha(l,m) = MathLib.sqrt( (2l+1)*(2l-1) / (l-m)*(l+m) ) for l >= m + 1
        for (l = 1; l <= this.lMax; l++) {
            final double temp = (2 * l + 1.) * (2 * l - 1.);
            for (m = 0; m <= this.mMax; m++) {
                if (l > m) {
                    this.alpha[l][m] = MathLib.sqrt(temp / ((l + m) * (l - m)));
                }
            }
        }
    }

    /**
     * Get the Hlm coefficients
     * 
     * @return the Hlm coefficients
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public double[][] getPh() {
        return this.ph;
    }

    /**
     * Get the H'lm coefficients
     * 
     * @return the H'lm coefficients
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public double[][] getDpph() {
        return this.dpph;
    }

    /**
     * Get the H''lm coefficients
     * 
     * @return the H''lm coefficients
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public double[][] getDsph() {
        return this.dsph;
    }
}
