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
 * VERSION:4.12:FA:FA-116:17/08/2023:[PATRIUS] Gestion du timeInterval dans
 * les classes QuaternionPolynomialProfile et QuaternionDatePolynomialProfile
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11.1:DM:DM-88:30/06/2023:[PATRIUS] Complement FT 3319
 * VERSION:4.11:DM:DM-3319:22/05/2023:[PATRIUS] QuaternionPolynomialSegment plus generique et coherent
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.attitudes.profiles;

import java.util.List;

import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeLeg;
import fr.cnes.sirius.patrius.attitudes.kinematics.AbstractOrientationFunction;
import fr.cnes.sirius.patrius.attitudes.kinematics.OrientationFunction;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.AbstractVector3DFunction;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3DFunction;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Represents a quaternion guidance profile, computed with polynomial functions.
 *
 * @concurrency conditionally thread-safe
 *
 * @concurrency.comment thread-safe if the attributes are thread-safe
 *
 * @author Alice Latourte
 *
 * @version $Id$
 *
 * @since 4.11
 */
public class QuaternionDatePolynomialProfile extends AbstractAttitudeProfile {

    /** Generated Serial UID. */
    private static final long serialVersionUID = -8642294471802958076L;

    /** Quaternion polynomial profile nature. */
    private static final String DEFAULT_NATURE = "QUATERNION_POLYNOMIAL_PROFILE";

    /** The reference frame of the polynomial functions. */
    private final Frame referenceFrame;

    /** List of polynomial functions on segments. */
    private final List<QuaternionDatePolynomialSegment> segments;

    /** Flag to compute or not the acceleration. */
    private boolean computeSpinDerivativesFlag = false;

    /**
     * Create a polynomial quaternion guidance profile.
     *
     * @param frame the reference frame of the polynomial functions
     * @param timeInterval the time interval of validity of the guidance profile
     * @param polynomialSegments the list of polynomial guidance profile segments
     * @param nature the nature of the quaternion polynomial profile
     */
    public QuaternionDatePolynomialProfile(final Frame frame, final AbsoluteDateInterval timeInterval,
                                           final List<QuaternionDatePolynomialSegment> polynomialSegments,
                                           final String nature) {
        // Set the time interval and nature
        super(timeInterval, nature);
        // Set the reference frame
        this.referenceFrame = frame;
        // Set the segments
        this.segments = polynomialSegments;
    }

    /**
     * Create a polynomial quaternion guidance profile.
     *
     * @param frame the reference frame of the polynomial functions
     * @param timeInterval the time interval of validity of the guidance profile
     * @param polynomialSegments the list of polynomial guidance profile segments
     */
    public QuaternionDatePolynomialProfile(final Frame frame, final AbsoluteDateInterval timeInterval,
                                           final List<QuaternionDatePolynomialSegment> polynomialSegments) {
        // Call the other constructor with the default nature
        this(frame, timeInterval, polynomialSegments, DEFAULT_NATURE);
    }

    /**
     * Create a polynomial quaternion guidance profile.
     *
     * @param frame the reference frame of the polynomial functions
     * @param timeInterval the time interval of validity of the guidance profile
     * @param polynomialSegments the list of polynomial guidance profile segments
     * @param nature the nature of the quaternion polynomial profile
     * @param spinDeltaT the delta-t used for spin/acceleration computation by finite differences
     */
    public QuaternionDatePolynomialProfile(final Frame frame, final AbsoluteDateInterval timeInterval,
                                           final List<QuaternionDatePolynomialSegment> polynomialSegments,
                                           final String nature, final double spinDeltaT) {
        // Set the time interval, nature and spin delta-t
        super(timeInterval, nature, spinDeltaT);
        // Set the reference frame
        this.referenceFrame = frame;
        // Set the segments
        this.segments = polynomialSegments;
    }

    /**
     * Create a polynomial quaternion guidance profile.
     *
     * @param frame the reference frame of the polynomial functions
     * @param timeInterval the time interval of validity of the guidance profile
     * @param polynomialSegments the list of polynomial guidance profile segments
     * @param spinDeltaT the delta-t used for spin computation by finite differences
     */
    public QuaternionDatePolynomialProfile(final Frame frame, final AbsoluteDateInterval timeInterval,
                                           final List<QuaternionDatePolynomialSegment> polynomialSegments,
                                           final double spinDeltaT) {
        // Call the other constructor with the default nature
        this(frame, timeInterval, polynomialSegments, DEFAULT_NATURE, spinDeltaT);
    }

    /**
     * Get the quaternion date polynomial segment whose time interval contains a given date.
     * 
     * @param date the date contained within the time interval of the quaternion date polynomial segment that we want to
     *        get
     * @return the quaternion date polynomial segment whose time interval contains the given date
     */
    private QuaternionDatePolynomialSegment getSegment(final AbsoluteDate date) {
        // Loop on the quaternion date polynomial segments
        for (final QuaternionDatePolynomialSegment segment : this.segments) {
            // Check if the time interval of the current quaternion date polynomial segment contains the given date
            if (segment.getTimeInterval().contains(date)) {
                // Return the current segment
                return segment;
            }
        }
        // Throw an exception
        throw PatriusException.createIllegalArgumentException(PatriusMessages.OUT_OF_RANGE_DATE_FOR_ATTITUDE_LAW);
    }

    /** {@inheritDoc} */
    @Override
    public Attitude getAttitude(final PVCoordinatesProvider pvProv, final AbsoluteDate date, final Frame frame)
        throws PatriusException {
        // Create an attitude provider without acceleration
        final AttitudeLeg attitudeProviderNoAcceleration = new AttitudeProviderNoAcceleration();
        // Get the attitude at the given PVCoordinatesProvider, date and frame
        final Attitude attitudeNoAcceleration = attitudeProviderNoAcceleration.getAttitude(pvProv, date, frame);
        // Initialize the spin derivatives
        Vector3D acc = null;
        // Check if the spin derivatives shall be computed
        if (this.computeSpinDerivativesFlag) {
            // Compute the spin derivatives
            acc = attitudeProviderNoAcceleration.computeSpinDerivativeByFD(pvProv, frame, date, getSpinDeltaT());
        }

        // Return the desired attitude
        return new Attitude(date, frame, attitudeNoAcceleration.getRotation(), attitudeNoAcceleration.getSpin(), acc);
    }

    /**
     * Return the size of the segments list.
     * 
     * @return the size of the segments list
     */
    public int size() {
        return this.segments.size();
    }

    /**
     * Return an individual segment of the list, specified by the index.
     * 
     * @param indexSegment
     *        index of the segment
     * @return the individual segment
     * @throws OutOfRangeException
     *         if the index of the segment is not compatible with the segments list size
     */
    public QuaternionDatePolynomialSegment getSegment(final int indexSegment) {
        if (indexSegment < 0 || indexSegment >= size()) {
            throw new OutOfRangeException(indexSegment, 0, size() - 1);
        }
        return this.segments.get(indexSegment);
    }

    /** {@inheritDoc} */
    @Override
    public void setSpinDerivativesComputation(final boolean computeSpinDerivatives) {
        // Set the flag for the spin derivatives computation
        this.computeSpinDerivativesFlag = computeSpinDerivatives;
    }

    /** {@inheritDoc} */
    @Override
    public QuaternionDatePolynomialProfile copy(final AbsoluteDateInterval newInterval) {
        if (!getTimeInterval().includes(newInterval)) {
            // New interval must be included in current interval
            throw PatriusException.createIllegalArgumentException(PatriusMessages.INTERVAL_MUST_BE_INCLUDED);
        }
        // Create a quaternion date polynomial profile
        final QuaternionDatePolynomialProfile res = new QuaternionDatePolynomialProfile(this.referenceFrame,
            newInterval, this.segments, getNature(), getSpinDeltaT());
        // Set the flag for the spin derivatives computation
        res.setSpinDerivativesComputation(this.computeSpinDerivativesFlag);

        // Return the quaternion date polynomial profile
        return res;
    }

    /**
     * Returns the reference frame of the polynomial functions.
     * @return the reference frame of the polynomial functions
     */
    public Frame getReferenceFrame() {
        return this.referenceFrame;
    }

    /**
     * Attitude provider without acceleration: returns the same attitude as
     * {@link #getAttitude(PVCoordinatesProvider, AbsoluteDate, Frame)} but without acceleration.
     */
    private class AttitudeProviderNoAcceleration implements AttitudeLeg {

        /** Serial UID. */
        private static final long serialVersionUID = 4830168703921250751L;

        /** {@inheritDoc} */
        @Override
        public void setSpinDerivativesComputation(final boolean computeSpinDerivatives) {
            // Unused
        }

        /** {@inheritDoc} */
        @Override
        public Attitude getAttitude(final PVCoordinatesProvider pvProv, final AbsoluteDate date, final Frame frame)
            throws PatriusException {
            // Check the date
            checkDate(date);
            // Get the orientation in the given reference frame
            final QuaternionDatePolynomialSegment segment = getSegment(date);
            final Rotation rotation = segment.getOrientation(date);
            // Transform the rotation from the reference frame to the new frame
            final Transform toReferenceFrame = frame.getTransformTo(
                QuaternionDatePolynomialProfile.this.referenceFrame, date);
            final Rotation inUserFrame = toReferenceFrame.getRotation().applyTo(rotation);
            // Create the orientation function
            final OrientationFunction rot = new AbstractOrientationFunction(date){
                /** {@inheritDoc} */
                @Override
                public Rotation getOrientation(final AbsoluteDate date) {
                    // Get the orientation of the quaternion date polynomial segment at the given date
                    final QuaternionDatePolynomialSegment segment = getSegment(date);
                    return segment.getOrientation(date);
                }
            };
            // Get the spin function
            final double h = getSpinDeltaT();
            final Vector3DFunction spin = new AbstractVector3DFunction(date){
                /** {@inheritDoc} */
                @Override
                public Vector3D getVector3D(final AbsoluteDate date) throws PatriusException {
                    // Estimate and return the rate function at the given date
                    return rot.estimateRateFunction(h, getTimeInterval()).getVector3D(date);
                }
            };

            // Build and return the attitude without acceleration
            return new Attitude(date, frame, inUserFrame, spin.getVector3D(date));
        }

        /** {@inheritDoc} */
        @Override
        public AbsoluteDateInterval getTimeInterval() {
            // Return the time interval of this quaternion date polynomial profile
            return QuaternionDatePolynomialProfile.this.getTimeInterval();
        }

        /** {@inheritDoc} */
        @Override
        public AttitudeProviderNoAcceleration copy(final AbsoluteDateInterval newInterval) {
            // Unused
            return null;
        }
    }
}
