/**
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
 * @history created 29/03/2013
 * HISTORY
* VERSION:4.7:DM:DM-2914:18/05/2021:Ajout d'un attribut reducedTimes à la classe QuaternionPolynomialSegment
* VERSION:4.7:DM:DM-2653:18/05/2021:generalisation des sequences et correction/refonte des sequences de segments 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage
 * VERSION::DM:319:05/03/2015:Corrected Rotation class (Step1)
 * VERSION::DM:1950:11/12/2018:move guidance package to attitudes.profiles
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes.profiles;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunction;
import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunctionLagrangeForm;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class represents a quaternion polynomial guidance profile on a segment.
 * 
 * @concurrency conditionally thread-safe
 * 
 * @concurrency.comment thread-safe if the UnivariateFunction attributes are thread-safe
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.3
 */
public final class QuaternionPolynomialSegment implements Serializable {

    /** Serial UID. */
    private static final long serialVersionUID = -6893147246865341872L;

    /** The date zero of the polynomial functions. */
    private final AbsoluteDate dateZero;

    /** The time interval of the segment. */
    private final AbsoluteDateInterval interval;

    /** Flag stating if reduced times in [0, 1] are used. */
    private final boolean reducedTimes;

    /** Polynomial function representing the q0 quaternion component. */
    private final UnivariateFunction q0pf;

    /** Polynomial function representing the q1 quaternion component. */
    private final UnivariateFunction q1pf;

    /** Polynomial function representing the q2 quaternion component. */
    private final UnivariateFunction q2pf;

    /** Polynomial function representing the q3 quaternion component. */
    private final UnivariateFunction q3pf;

    /** The coefficients of the polynomial function representing q0. */
    private final double[] coefficientsQ0pf;

    /** The coefficients of the polynomial function representing q1. */
    private final double[] coefficientsQ1pf;

    /** The coefficients of the polynomial function representing q2. */
    private final double[] coefficientsQ2pf;

    /** The coefficients of the polynomial function representing q3. */
    private final double[] coefficientsQ3pf;

    /**
     * Build a quaternion polynomial guidance profile on a segment.<br>
     * The polynomial representing the quaternion components are on lagrange form.
     * <p>Real time is used (not reduced time).</p>
     *
     * @param q0
     *        the polynomial function representing the q0 quaternion component
     * @param q1
     *        the polynomial function representing the q1 quaternion component
     * @param q2
     *        the polynomial function representing the q2 quaternion component
     * @param q3
     *        the polynomial function representing the q3 quaternion component
     * @param date0
     *        the date zero of the polynomial functions
     * @param timeInterval
     *        the time interval of the segment
     */
    public QuaternionPolynomialSegment(final PolynomialFunctionLagrangeForm q0,
            final PolynomialFunctionLagrangeForm q1,
            final PolynomialFunctionLagrangeForm q2,
            final PolynomialFunctionLagrangeForm q3,
            final AbsoluteDate date0,
            final AbsoluteDateInterval timeInterval) {

        this.dateZero = date0;
        this.interval = timeInterval;
        this.reducedTimes = false;
        this.q0pf = q0;
        this.q1pf = q1;
        this.q2pf = q2;
        this.q3pf = q3;
        this.coefficientsQ0pf = q0.getCoefficients();
        this.coefficientsQ1pf = q1.getCoefficients();
        this.coefficientsQ2pf = q2.getCoefficients();
        this.coefficientsQ3pf = q3.getCoefficients();
    }

    /**
     * Build a quaternion polynomial guidance profile on a segment.<br>
     * The polynomial representing the quaternion components are generic polynomial functions.
     * <p>Real time is used (not reduced time).</p>
     *
     * @param q0
     *        the polynomial function representing the q0 quaternion component
     * @param q1
     *        the polynomial function representing the q1 quaternion component
     * @param q2
     *        the polynomial function representing the q2 quaternion component
     * @param q3
     *        the polynomial function representing the q3 quaternion component
     * @param date0
     *        the date zero of the polynomial functions
     * @param timeInterval
     *        the time interval of the segment
     */
    public QuaternionPolynomialSegment(final PolynomialFunction q0,
            final PolynomialFunction q1,
            final PolynomialFunction q2,
            final PolynomialFunction q3,
            final AbsoluteDate date0,
            final AbsoluteDateInterval timeInterval) {

        this.dateZero = date0;
        this.interval = timeInterval;
        this.reducedTimes = false;
        this.q0pf = q0;
        this.q1pf = q1;
        this.q2pf = q2;
        this.q3pf = q3;
        this.coefficientsQ0pf = q0.getCoefficients();
        this.coefficientsQ1pf = q1.getCoefficients();
        this.coefficientsQ2pf = q2.getCoefficients();
        this.coefficientsQ3pf = q3.getCoefficients();
    }

    /**
     * Build a quaternion polynomial guidance profile on a segment.<br>
     * The polynomial representing the quaternion components are on lagrange form.<br>
     * Reduced time is used (hence time is considered to be in [0, 1] with 0 the time interval lower boundary and 1 the
     * time interval upper boundary), and dateZero corresponds to the lower data of
     * entered time interval.
     *
     * @param q0
     *        the polynomial function representing the q0 quaternion component
     * @param q1
     *        the polynomial function representing the q1 quaternion component
     * @param q2
     *        the polynomial function representing the q2 quaternion component
     * @param q3
     *        the polynomial function representing the q3 quaternion component
     * @param timeInterval
     *        the time interval of the segment
     */
    public QuaternionPolynomialSegment(final PolynomialFunctionLagrangeForm q0, final PolynomialFunctionLagrangeForm q1,
                                       final PolynomialFunctionLagrangeForm q2, final PolynomialFunctionLagrangeForm q3,
                                       final AbsoluteDateInterval timeInterval) {
        this(q0, q1, q2, q3, q0.getCoefficients(), q1.getCoefficients(), q2.getCoefficients(), q3.getCoefficients(),
                timeInterval);
    }

    /**
     * Build a quaternion polynomial guidance profile on a segment.<br>
     * The polynomial representing the quaternion components are generic polynomial functions.<br>
     * Reduced time is used (hence time is considered to be in [0, 1] with 0 the time interval lower boundary and 1 the
     * time interval upper boundary), and dateZero corresponds to the lower data of
     * entered time interval.
     *
     * @param q0
     *            the polynomial function representing the q0 quaternion component
     * @param q1
     *            the polynomial function representing the q1 quaternion component
     * @param q2
     *            the polynomial function representing the q2 quaternion component
     * @param q3
     *            the polynomial function representing the q3 quaternion component
     * @param timeInterval
     *            the time interval of the segment
     */
    public QuaternionPolynomialSegment(final PolynomialFunction q0,
            final PolynomialFunction q1,
            final PolynomialFunction q2,
            final PolynomialFunction q3,
            final AbsoluteDateInterval timeInterval) {
        this(q0, q1, q2, q3, q0.getCoefficients(), q1.getCoefficients(), q2.getCoefficients(), q3.getCoefficients(),
                timeInterval);
    }

    /**
     * Build a quaternion polynomial guidance profile on a segment.<br>
     * The polynomial representing the quaternion components are generic polynomial functions.<br>
     * Reduced time is used (hence time is considered to be in [0, 1] with 0 the time interval lower boundary and 1 the
     * time interval upper boundary), and dateZero corresponds to the lower data of
     * entered time interval.
     *
     * @param q0
     *            the function representing the q0 quaternion component
     * @param q1
     *            the function representing the q1 quaternion component
     * @param q2
     *            the function representing the q2 quaternion component
     * @param q3
     *            the function representing the q3 quaternion component
     * @param q0coeffs
     *            the q0 polynomial coefficients
     * @param q1coeffs
     *            the q1 polynomial coefficients
     * @param q2coeffs
     *            the q2 polynomial coefficients
     * @param q3coeffs
     *            the q3 polynomial coefficients
     * @param timeInterval
     *            the time interval of the segment
     */
    private QuaternionPolynomialSegment(final UnivariateFunction q0,
            final UnivariateFunction q1,
            final UnivariateFunction q2,
            final UnivariateFunction q3,
            final double[] q0coeffs,
            final double[] q1coeffs,
            final double[] q2coeffs,
            final double[] q3coeffs,
            final AbsoluteDateInterval timeInterval) {
        this.dateZero = timeInterval.getLowerData();
        this.interval = timeInterval;
        this.reducedTimes = true;
        this.q0pf = q0;
        this.q1pf = q1;
        this.q2pf = q2;
        this.q3pf = q3;
        this.coefficientsQ0pf = q0coeffs;
        this.coefficientsQ1pf = q1coeffs;
        this.coefficientsQ2pf = q2coeffs;
        this.coefficientsQ3pf = q3coeffs;
    }

    /**
     * Get the orientation from the quaternion polynomials at a given date.
     * 
     * @param date
     *        the date
     * @return the orientation at a given date
     */
    public Rotation getOrientation(final AbsoluteDate date) {

        if (this.interval.contains(date)) {
            double t = date.durationFrom(this.dateZero);
            if (this.reducedTimes) {
                // Reduced time in [0, 1] case
                t /= this.interval.getDuration();
            }
            return new Rotation(false, this.q0pf.value(t), this.q1pf.value(t), this.q2pf.value(t), this.q3pf.value(t));
        } else {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.OUT_OF_RANGE_DATE_FOR_ATTITUDE_LAW);
        }
    }

    /**
     * Get the time interval of the guidance profile segment.
     * 
     * @return the time interval of the guidance profile segment.
     */
    public AbsoluteDateInterval getTimeInterval() {
        return this.interval;
    }

    /**
     * @return the coefficients of the polynomial function representing q0.
     */
    public double[] getQ0Coefficients() {
        return this.coefficientsQ0pf.clone();
    }

    /**
     * @return the coefficients of the polynomial function representing q1.
     */
    public double[] getQ1Coefficients() {
        return this.coefficientsQ1pf.clone();
    }

    /**
     * @return the coefficients of the polynomial function representing q2.
     */
    public double[] getQ2Coefficients() {
        return this.coefficientsQ2pf.clone();
    }

    /**
     * @return the coefficients of the polynomial function representing q3.
     */
    public double[] getQ3Coefficients() {
        return this.coefficientsQ3pf.clone();
    }
}
