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
 * HISTORY
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11.1:DM:DM-88:30/06/2023:[PATRIUS] Complement FT 3319
 * VERSION:4.11:DM:DM-3319:22/05/2023:[PATRIUS] QuaternionPolynomialSegment plus generique et coherent
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.attitudes.profiles;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.analysis.polynomials.DatePolynomialFunctionInterface;
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
 * @concurrency.comment thread-safe if the DatePolynomialFunctionInterface attributes are thread-safe
 *
 * @author Alice Latourte
 *
 * @version $Id$
 *
 * @since 4.11
 */
public final class QuaternionDatePolynomialSegment implements Serializable {

    /** Serial UID. */
    private static final long serialVersionUID = -6893147246865341872L;

    /** Polynomial function of date representing the q0 quaternion component. */
    private final DatePolynomialFunctionInterface q0pf;

    /** Polynomial function of date representing the q1 quaternion component. */
    private final DatePolynomialFunctionInterface q1pf;

    /** Polynomial function of date representing the q2 quaternion component. */
    private final DatePolynomialFunctionInterface q2pf;

    /** Polynomial function of date representing the q3 quaternion component. */
    private final DatePolynomialFunctionInterface q3pf;

    /** The time interval of the guidance profile segment. */
    private final AbsoluteDateInterval interval;

    /** True if the quaternion polynomials need normalization to compute the orientation */
    private final boolean needsNormalization;

    /**
     * Build a quaternion polynomial guidance profile on a segment.<br>
     * The polynomial representing the quaternion components are generic polynomial functions.
     * <p>
     * Real time is used (not reduced time).
     * </p>
     *
     * @param q0 the polynomial function of date representing the q0 quaternion component
     * @param q1 the polynomial function of date representing the q1 quaternion component
     * @param q2 the polynomial function of date representing the q2 quaternion component
     * @param q3 the polynomial function of date representing the q3 quaternion component
     * @param timeInterval the time interval of the guidance profile segment: only used as validity time interval (no
     *        impact on orientation value at any date)
     * @param needsNormalizationIn
     *        true if the quaternion polynomials need normalization
     */
    public QuaternionDatePolynomialSegment(final DatePolynomialFunctionInterface q0,
                                           final DatePolynomialFunctionInterface q1,
                                           final DatePolynomialFunctionInterface q2,
                                           final DatePolynomialFunctionInterface q3,
                                           final AbsoluteDateInterval timeInterval,
                                           final boolean needsNormalizationIn) {
        // Set the polynomial functions of date representing the quaternion components
        this.q0pf = q0;
        this.q1pf = q1;
        this.q2pf = q2;
        this.q3pf = q3;
        // Set the time interval of the guidance profile segment
        this.interval = timeInterval;
        this.needsNormalization = needsNormalizationIn;
    }

    /**
     * Get the orientation from the quaternion polynomials at a given date.
     *
     * @param date the date at which we want to get the orientation from the quaternion polynomials
     * @return the orientation from the quaternion polynomials at the given date
     */
    public Rotation getOrientation(final AbsoluteDate date) {
        // Check if the time interval of the guidance profile segment contains the given date
        if (!this.interval.contains(date)) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.OUT_OF_RANGE_DATE_FOR_ATTITUDE_LAW);
        }

        // Return the orientation at the given date
        return new Rotation(this.needsNormalization, this.q0pf.value(date), this.q1pf.value(date),
            this.q2pf.value(date), this.q3pf.value(date));
    }

    /**
     * Get the time interval of the guidance profile segment.
     *
     * @return the time interval of the guidance profile segment
     */
    public AbsoluteDateInterval getTimeInterval() {
        // Return the time interval of the guidance profile segment
        return this.interval;
    }

    /**
     * Getter for the polynomial function of date representing the q0 quaternion component.
     * 
     * @return the polynomial function of date representing the q0 quaternion component
     */
    public DatePolynomialFunctionInterface getQ0Polynomial() {
        return this.q0pf;
    }

    /**
     * Getter for the polynomial function of date representing the q1 quaternion component.
     * 
     * @return the polynomial function of date representing the q1 quaternion component
     */
    public DatePolynomialFunctionInterface getQ1Polynomial() {
        return this.q1pf;
    }

    /**
     * Getter for the polynomial function of date representing the q2 quaternion component.
     * 
     * @return the polynomial function of date representing the q2 quaternion component
     */
    public DatePolynomialFunctionInterface getQ2Polynomial() {
        return this.q2pf;
    }

    /**
     * Getter for the polynomial function of date representing the q3 quaternion component.
     * 
     * @return the polynomial function of date representing the q3 quaternion component
     */
    public DatePolynomialFunctionInterface getQ3Polynomial() {
        return this.q3pf;
    }
}
