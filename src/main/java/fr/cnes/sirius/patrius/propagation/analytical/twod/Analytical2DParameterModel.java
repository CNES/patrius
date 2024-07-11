/**
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
 * @history 09/04/2013
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:94:30/08/2013:Completion of Javadoc
 * VERSION::FA:216:02/04/2014:Corrected javadoc
 * VERSION::DM:211:08/04/2014:Modified analytical 2D propagator
 * VERSION::DM:211:30/04/2014:Added missing methods
 * VERSION::FA:345:30/10/2014:modified comments ratio
 * VERSION::DM:266:29/04/2015:add various centered analytical models
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.analytical.twod;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * This class represents an analytical 2D orbital parameter model. The adapted circular parameter represented by this
 * model is decomposed into a linear and a trigonometric part.
 * 
 * @concurrency immutable
 * 
 * @see Analytical2DOrbitModel
 * @see Analytical2DPropagator
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: Analytical2DParameterModel.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.3
 */
public class Analytical2DParameterModel implements Serializable {

    /** Generated serial UID */
    private static final long serialVersionUID = 2274624455870068728L;

    /** Centered part of the analytical model. */
    private final UnivariateDateFunction centeredModel;

    /** Trigonometric development coefficients. */
    private final double[][] trigonometricCoefficients;

    /**
     * Create a new analytical 2D orbital parameter model, with given polynomial and trigonometric developments.
     * 
     * @param centeredFunction
     *        centered part of the model
     * @param trigonometricCoefs
     *        trigonometric development coefficients [ ..., [n, k, amp, phi], ...].
     *        Lines must be ordered along decreasing amplitudes.
     */
    public Analytical2DParameterModel(final UnivariateDateFunction centeredFunction,
        final double[][] trigonometricCoefs) {

        // Centered part
        this.centeredModel = centeredFunction;

        // Trigonometric part
        this.trigonometricCoefficients = new double[trigonometricCoefs.length][];
        for (int i = 0; i < trigonometricCoefs.length; i++) {
            this.trigonometricCoefficients[i] = trigonometricCoefs[i].clone();
        }

        // Check on inputs
        this.checkTrigonometricCoefficients();
    }

    /**
     * Check that the trigonometric coefficients are sorted by decreasing amplitudes.
     */
    private void checkTrigonometricCoefficients() {
        // Check size of trigonometric coefficients
        if (this.trigonometricCoefficients.length > 0 && this.trigonometricCoefficients[0].length != 4) {
            throw new IllegalArgumentException();
        }

        // Check that the trigonometric coefficients are sorted by decreasing amplitudes
        for (int i = 1; i < this.trigonometricCoefficients.length; i++) {
            if (this.trigonometricCoefficients[i][2] > this.trigonometricCoefficients[i - 1][2]) {
                throw new IllegalArgumentException();
            }
        }
    }

    /**
     * Get the value of the model at provided date.
     * 
     * @param date
     *        date
     * @param pso
     *        centered latitude argument
     * @param lna
     *        longitude of ascending node
     * @return the value of the model at provided date
     */
    public double getValue(final AbsoluteDate date, final double pso, final double lna) {
        return this.centeredModel.value(date) + this.getTrigonometricValue(pso, lna, this.getMaxTrigonometricOrder());
    }

    /**
     * Get the value of the model at provided date.
     * 
     * @param date
     *        date
     * @param pso
     *        centered latitude argument
     * @param lna
     *        longitude of ascending node
     * @param order
     *        order of the trigonometric development
     * @return the value of the model at provided date
     */
    public double getValue(final AbsoluteDate date, final double pso, final double lna, final int order) {
        return this.centeredModel.value(date) + this.getTrigonometricValue(pso, lna, order);
    }

    /**
     * Get the centered value of the model.
     * 
     * @param date
     *        a date
     * @return centered value of the model
     */
    public double getCenteredValue(final AbsoluteDate date) {
        return this.centeredModel.value(date);
    }

    /**
     * Get the value of the trigonometric contribution with maximum order.
     * 
     * @param pso
     *        centered latitude argument
     * @param lna
     *        longitude of ascending node
     * @return the value of the trigonometric contribution
     */
    public double getTrigonometricValue(final double pso, final double lna) {
        return this.getTrigonometricValue(pso, lna, this.getMaxTrigonometricOrder());
    }

    /**
     * Get the value of the trigonometric contribution up to provided order.
     * 
     * @param pso
     *        centered latitude argument
     * @param lna
     *        longitude of ascending node
     * @param order
     *        trigonometric order of the development
     * @return the value of the trigonometric contribution
     */
    public double getTrigonometricValue(final double pso, final double lna, final int order) {

        this.checkTrigRange(order);

        double result = 0;
        for (int i = order - 1; i >= 0; i--) {
            result += this.getOneHarmonicValue(pso, lna, i);
        }
        return result;
    }

    /**
     * Get the value of the nth trigonometric contribution.
     * 
     * @param pso
     *        centered value of mean aol
     * @param lna
     *        centered value of the longitude of ascending node
     * @param order
     *        order of the development
     * @return the value of the nth harmonic contribution
     */
    public double getOneHarmonicValue(final double pso, final double lna, final int order) {

        this.checkTrigRange(order);

        final double n = this.trigonometricCoefficients[order][0];
        final double k = this.trigonometricCoefficients[order][1];
        final double amp = this.trigonometricCoefficients[order][2];
        final double phi = this.trigonometricCoefficients[order][3];

        return amp * MathLib.cos(n * pso + k * lna + phi);
    }

    /**
     * Make sure trigonometric development order is within range.
     * 
     * @param order
     *        trigonometric order
     */
    private void checkTrigRange(final int order) {
        if (order > this.getMaxTrigonometricOrder() || order < 0) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Get model for centered part of analytical model.
     * 
     * @return model for centered part of analytical model
     */
    public UnivariateDateFunction getCenteredModel() {
        return this.centeredModel;
    }

    /**
     * Returns the trigonometric coefficients array.
     * 
     * @return the trigonometric coefficients array
     */
    public double[][] getTrigonometricCoefficients() {
        final double[][] clone = new double[this.trigonometricCoefficients.length][];
        for (int i = 0; i < clone.length; i++) {
            clone[i] = this.trigonometricCoefficients[i].clone();
        }
        return clone;
    }

    /**
     * Return the highest trigonometric order.
     * 
     * @return highest trigonometric order
     */
    public int getMaxTrigonometricOrder() {
        return this.trigonometricCoefficients.length;
    }
}
