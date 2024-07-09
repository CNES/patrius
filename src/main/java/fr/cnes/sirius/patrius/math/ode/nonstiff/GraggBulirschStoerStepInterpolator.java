/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 */
package fr.cnes.sirius.patrius.math.ode.nonstiff;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import fr.cnes.sirius.patrius.math.ode.EquationsMapper;
import fr.cnes.sirius.patrius.math.ode.sampling.AbstractStepInterpolator;
import fr.cnes.sirius.patrius.math.ode.sampling.StepInterpolator;
import fr.cnes.sirius.patrius.math.util.MathLib;

//CHECKSTYLE: stop CommentRatio check
//Reason: model - Commons-Math code kept as such

/**
 * This class implements an interpolator for the Gragg-Bulirsch-Stoer
 * integrator.
 * 
 * <p>
 * This interpolator compute dense output inside the last step produced by a Gragg-Bulirsch-Stoer integrator.
 * </p>
 * 
 * <p>
 * This implementation is basically a reimplementation in Java of the <a
 * href="http://www.unige.ch/math/folks/hairer/prog/nonstiff/odex.f">odex</a> fortran code by E. Hairer and G. Wanner.
 * The redistribution policy for this code is available <a href="http://www.unige.ch/~hairer/prog/licence.txt">here</a>,
 * for convenience, it is reproduced below.
 * </p>
 * </p>
 * 
 * <table border="0" width="80%" cellpadding="10" align="center" bgcolor="#E0E0E0">
 * <tr>
 * <td>Copyright (c) 2004, Ernst Hairer</td>
 * </tr>
 * 
 * <tr>
 * <td>Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 * <ul>
 * <li>Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer.</li>
 * <li>Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials provided with the distribution.</li>
 * </ul>
 * </td>
 * </tr>
 * 
 * <tr>
 * <td><strong>THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.</strong></td>
 * </tr>
 * </table>
 * 
 * @see GraggBulirschStoerIntegrator
 * @version $Id: GraggBulirschStoerStepInterpolator.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 1.2
 */

@SuppressWarnings("PMD.NullAssignment")
class GraggBulirschStoerStepInterpolator
    extends AbstractStepInterpolator {

    /** 16. */
    private static final int SIXTEEN = 16;
    
    /** 7. */
    private static final int SEVEN = 7;

    /** 0.5. */
    private static final double HALF = 0.5;

    /** 0.25. */
    private static final double QUARTER = 0.25;

    /** 0.125. */
    private static final double EIGHTH = 0.125;

    /** Serializable version identifier. */
    private static final long serialVersionUID = 20110928L;

    /** Slope at the beginning of the step. */
    private final double[] y0Dot;

    /** State at the end of the step. */
    private final double[] y1;

    /** Slope at the end of the step. */
    private final double[] y1Dot;

    /**
     * Derivatives at the middle of the step.
     * element 0 is state at midpoint, element 1 is first derivative ...
     */
    private final double[][] yMidDots;

    /** Interpolation polynomials. */
    private double[][] polynomials;

    /** Error coefficients for the interpolation. */
    private double[] errfac;

    /** Degree of the interpolation polynomials. */
    private int currentDegree;

    /**
     * Simple constructor.
     * This constructor should not be used directly, it is only intended
     * for the serialization process.
     */
    public GraggBulirschStoerStepInterpolator() {
        super();
        this.y0Dot = null;
        this.y1 = null;
        this.y1Dot = null;
        this.yMidDots = null;
        this.resetTables(-1);
    }

    /**
     * Simple constructor.
     * 
     * @param y
     *        reference to the integrator array holding the current state
     * @param y0DotIn
     *        reference to the integrator array holding the slope
     *        at the beginning of the step
     * @param y1In
     *        reference to the integrator array holding the state at
     *        the end of the step
     * @param y1DotIn
     *        reference to the integrator array holding the slope
     *        at the end of the step
     * @param yMidDotsIn
     *        reference to the integrator array holding the
     *        derivatives at the middle point of the step
     * @param forward
     *        integration direction indicator
     * @param primaryMapper
     *        equations mapper for the primary equations set
     * @param secondaryMappers
     *        equations mappers for the secondary equations sets
     */
    public GraggBulirschStoerStepInterpolator(final double[] y, final double[] y0DotIn,
        final double[] y1In, final double[] y1DotIn,
        final double[][] yMidDotsIn,
        final boolean forward,
        final EquationsMapper primaryMapper,
        final EquationsMapper[] secondaryMappers) {

        super(y, forward, primaryMapper, secondaryMappers);
        this.y0Dot = y0DotIn;
        this.y1 = y1In;
        this.y1Dot = y1DotIn;
        this.yMidDots = yMidDotsIn;

        this.resetTables(yMidDotsIn.length + 4);

    }

    /**
     * Copy constructor.
     * 
     * @param interpolator
     *        interpolator to copy from. The copy is a deep
     *        copy: its arrays are separated from the original arrays of the
     *        instance
     */
    public GraggBulirschStoerStepInterpolator(final GraggBulirschStoerStepInterpolator interpolator) {

        super(interpolator);

        final int dimension = this.currentState.length;

        // the interpolator has been finalized,
        // the following arrays are not needed anymore
        this.y0Dot = null;
        this.y1 = null;
        this.y1Dot = null;
        this.yMidDots = null;

        // copy the interpolation polynomials (up to the current degree only)
        if (interpolator.polynomials == null) {
            this.polynomials = null;
            this.currentDegree = -1;
        } else {
            this.resetTables(interpolator.currentDegree);
            for (int i = 0; i < this.polynomials.length; ++i) {
                this.polynomials[i] = new double[dimension];
                System.arraycopy(interpolator.polynomials[i], 0,
                    this.polynomials[i], 0, dimension);
            }
            this.currentDegree = interpolator.currentDegree;
        }

    }

    /**
     * Reallocate the internal tables.
     * Reallocate the internal tables in order to be able to handle
     * interpolation polynomials up to the given degree
     * 
     * @param maxDegree
     *        maximal degree to handle
     */
    private void resetTables(final int maxDegree) {

        if (maxDegree < 0) {
            this.polynomials = null;
            this.errfac = null;
            this.currentDegree = -1;
        } else {

            final double[][] newPols = new double[maxDegree + 1][];
            if (this.polynomials == null) {
                for (int i = 0; i < newPols.length; ++i) {
                    newPols[i] = new double[this.currentState.length];
                }
            } else {
                System.arraycopy(this.polynomials, 0, newPols, 0, this.polynomials.length);
                for (int i = this.polynomials.length; i < newPols.length; ++i) {
                    newPols[i] = new double[this.currentState.length];
                }
            }
            this.polynomials = newPols;

            // initialize the error factors array for interpolation
            if (maxDegree <= 4) {
                this.errfac = null;
            } else {
                this.errfac = new double[maxDegree - 4];
                for (int i = 0; i < this.errfac.length; ++i) {
                    final int ip5 = i + 5;
                    this.errfac[i] = 1.0 / (ip5 * ip5);
                    final double e = 0.5 * MathLib.sqrt(((double) (i + 1)) / ip5);
                    for (int j = 0; j <= i; ++j) {
                        this.errfac[i] *= e / (j + 1);
                    }
                }
            }

            this.currentDegree = 0;

        }

    }

    /** {@inheritDoc} */
    @Override
    protected StepInterpolator doCopy() {
        return new GraggBulirschStoerStepInterpolator(this);
    }

    /**
     * Compute the interpolation coefficients for dense output.
     * 
     * @param mu
     *        degree of the interpolation polynomial
     * @param h
     *        current step
     */
    public void computeCoefficients(final int mu, final double h) {

        if ((this.polynomials == null) || (this.polynomials.length <= (mu + 4))) {
            this.resetTables(mu + 4);
        }

        this.currentDegree = mu + 4;

        for (int i = 0; i < this.currentState.length; ++i) {

            final double yp0 = h * this.y0Dot[i];
            final double yp1 = h * this.y1Dot[i];
            final double ydiff = this.y1[i] - this.currentState[i];
            final double aspl = ydiff - yp1;
            final double bspl = yp0 - ydiff;

            this.polynomials[0][i] = this.currentState[i];
            this.polynomials[1][i] = ydiff;
            this.polynomials[2][i] = aspl;
            this.polynomials[3][i] = bspl;

            if (mu < 0) {
                return;
            }

            // compute the remaining coefficients
            final double ph0 = HALF * (this.currentState[i] + this.y1[i]) + EIGHTH * (aspl + bspl);
            this.polynomials[4][i] = SIXTEEN * (this.yMidDots[0][i] - ph0);

            if (mu > 0) {
                final double ph1 = ydiff + QUARTER * (aspl - bspl);
                this.polynomials[5][i] = SIXTEEN * (this.yMidDots[1][i] - ph1);

                if (mu > 1) {
                    final double ph2 = yp1 - yp0;
                    this.polynomials[6][i] = SIXTEEN * (this.yMidDots[2][i] - ph2 + this.polynomials[4][i]);

                    if (mu > 2) {
                        final double ph3 = 6 * (bspl - aspl);
                        this.polynomials[SEVEN][i] = SIXTEEN * (this.yMidDots[3][i] - ph3 + 3 * this.polynomials[5][i]);

                        for (int j = 4; j <= mu; ++j) {
                            final double fac1 = HALF * j * (j - 1);
                            final double fac2 = 2 * fac1 * (j - 2) * (j - 3);
                            this.polynomials[j + 4][i] =
                                SIXTEEN * (this.yMidDots[j][i] + fac1 * this.polynomials[j + 2][i]
                                    - fac2 * this.polynomials[j][i]);
                        }

                    }
                }
            }
        }

    }

    /**
     * Estimate interpolation error.
     * 
     * @param scale
     *        scaling array
     * @return estimate of the interpolation error
     */
    public double estimateError(final double[] scale) {
        double error = 0;
        if (this.currentDegree >= 5) {
            for (int i = 0; i < scale.length; ++i) {
                final double e = this.polynomials[this.currentDegree][i] / scale[i];
                error += e * e;
            }
            error = MathLib.sqrt(error / scale.length) * this.errfac[this.currentDegree - 5];
        }
        return error;
    }

    /** {@inheritDoc} */
    @Override
    protected void computeInterpolatedStateAndDerivatives(final double theta,
                                                          final double oneMinusThetaH) {

        final int dimension = this.currentState.length;

        final double oneMinusTheta = 1.0 - theta;
        final double theta05 = theta - HALF;
        final double tOmT = theta * oneMinusTheta;
        final double t4 = tOmT * tOmT;
        final double t4Dot = 2 * tOmT * (1 - 2 * theta);
        final double dot1 = 1.0 / this.h;
        final double dot2 = theta * (2 - 3 * theta) / this.h;
        final double dot3 = ((3 * theta - 4) * theta + 1) / this.h;

        for (int i = 0; i < dimension; ++i) {

            final double p0 = this.polynomials[0][i];
            final double p1 = this.polynomials[1][i];
            final double p2 = this.polynomials[2][i];
            final double p3 = this.polynomials[3][i];
            this.interpolatedState[i] = p0 + theta * (p1 + oneMinusTheta * (p2 * theta + p3 * oneMinusTheta));
            this.interpolatedDerivatives[i] = dot1 * p1 + dot2 * p2 + dot3 * p3;

            if (this.currentDegree > 3) {
                double cDot = 0;
                double c = this.polynomials[this.currentDegree][i];
                for (int j = this.currentDegree - 1; j > 3; --j) {
                    final double d = 1.0 / (j - 3);
                    cDot = d * (theta05 * cDot + c);
                    c = this.polynomials[j][i] + c * d * theta05;
                }
                this.interpolatedState[i] += t4 * c;
                this.interpolatedDerivatives[i] += (t4 * cDot + t4Dot * c) / this.h;
            }

        }

        if (this.h == 0) {
            // in this degenerated case, the previous computation leads to NaN for derivatives
            // we fix this by using the derivatives at midpoint
            System.arraycopy(this.yMidDots[1], 0, this.interpolatedDerivatives, 0, dimension);
        }

    }

    /** {@inheritDoc} */
    @Override
    public void writeExternal(final ObjectOutput oo) throws IOException {

        final int dimension = (this.currentState == null) ? -1 : this.currentState.length;

        // save the state of the base class
        this.writeBaseExternal(oo);

        // save the local attributes (but not the temporary vectors)
        oo.writeInt(this.currentDegree);
        for (int k = 0; k <= this.currentDegree; ++k) {
            for (int l = 0; l < dimension; ++l) {
                oo.writeDouble(this.polynomials[k][l]);
            }
        }

    }

    /** {@inheritDoc} */
    @Override
    public void readExternal(final ObjectInput oi) throws IOException, ClassNotFoundException {

        // read the base class
        final double t = this.readBaseExternal(oi);
        final int dimension = (this.currentState == null) ? -1 : this.currentState.length;

        // read the local attributes
        final int degree = oi.readInt();
        this.resetTables(degree);
        this.currentDegree = degree;

        for (int k = 0; k <= this.currentDegree; ++k) {
            for (int l = 0; l < dimension; ++l) {
                this.polynomials[k][l] = oi.readDouble();
            }
        }

        // we can now set the interpolated time and state
        this.setInterpolatedTime(t);

    }

    // CHECKSTYLE: resume CommentRatio check
}
