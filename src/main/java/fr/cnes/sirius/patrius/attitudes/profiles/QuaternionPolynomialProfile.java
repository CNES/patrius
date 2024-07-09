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
 * @history created 26/03/2013
 * 
 * HISTORY
* VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
* VERSION:4.7:DM:DM-2767:18/05/2021:Evolutions et corrections diverses 
* VERSION:4.7:DM:DM-2872:18/05/2021:Calcul de l'accélération dans la classe QuaternionPolynomialProfile 
* VERSION:4.7:FA:FA-2857:18/05/2021:Test unitaire manquant pour validation de la FA 2466 
* VERSION:4.7:DM:DM-2653:18/05/2021:generalisation des sequences et correction/refonte des sequences de segments 
 * VERSION:4.6:DM:DM-2565:27/01/2021:[PATRIUS] Modification de QuaternionPolynomialProfile pour pouvoir 
 * definir la nature du profil 
 * VERSION:4.6:DM:DM-2656:27/01/2021:[PATRIUS] delTa parametrable utilise pour le calcul de vitesse dans 
 * QuaternionPolynomialProfile
 * VERSION:4.5:FA:FA-2440:27/05/2020:difference finie en debut de segment QuaternionPolynomialProfile 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION:4.3:FA:FA-2084:15/05/2019:[PATRIUS] Suppression de la classe AbstractGuidanceProfile
 * VERSION::FA:180:27/03/2014:Removed DynamicsElements - frames transformations derivatives unknown
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:489:06/10/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:1948:14/11/2018:new attitude leg sequence design
 * VERSION::DM:1950:11/12/2018:move guidance package to attitudes.profiles
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes.profiles;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeLeg;
import fr.cnes.sirius.patrius.attitudes.kinematics.AbstractOrientationFunction;
import fr.cnes.sirius.patrius.attitudes.kinematics.OrientationFunction;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
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
 * Represents a quaternion guidance profile, calculated with polynomial functions
 * 
 * @concurrency conditionally thread-safe
 * 
 * @concurrency.comment thread-safe if the attributes are thread-safe
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.3
 */
public final class QuaternionPolynomialProfile extends AbstractAttitudeProfile {

    /** Generated Serial UID. */
    private static final long serialVersionUID = -8642294471802958076L;

    /** Nature. */
    private static final String DEFAULT_NATURE = "QUATERNION_POLYNOMIAL_PROFILE";

    /** The reference frame of the polynomial functions. */
    private final Frame referenceFrame;

    /** List of polynomial functions on segments. */
    private final List<QuaternionPolynomialSegment> segments;

    /** Flag to compute or not acceleration. */
    private boolean computeSpinDerivativesFlag = false;

    /**
     * Create a polynomial, quaternion guidance profile.
     * 
     * @param frame
     *        the reference frame of the polynomial functions
     * @param timeInterval
     *        interval of validity of the guidance profile
     * @param polynomials
     *        the list of polynomial guidance profile segments
     * @param nature
     *        nature
     */
    public QuaternionPolynomialProfile(final Frame frame,
            final AbsoluteDateInterval timeInterval,
            final List<QuaternionPolynomialSegment> polynomials,
            final String nature) {
        super(timeInterval, nature);
        this.referenceFrame = frame;
        this.segments = polynomials;
    }

    /**
     * Create a polynomial, quaternion guidance profile.
     * 
     * @param frame
     *        the reference frame of the polynomial functions
     * @param timeInterval
     *        interval of validity of the guidance profile
     * @param polynomials
     *        the list of polynomial guidance profile segments
     */
    public QuaternionPolynomialProfile(final Frame frame,
            final AbsoluteDateInterval timeInterval,
            final List<QuaternionPolynomialSegment> polynomials) {
        this(frame, timeInterval, polynomials, DEFAULT_NATURE);
    }

    /**
     * Create a polynomial, quaternion guidance profile.
     * 
     * @param frame
     *        the reference frame of the polynomial functions
     * @param timeInterval
     *        interval of validity of the guidance profile
     * @param polynomials
     *        the list of polynomial guidance profile segments
     * @param nature
     *        nature
     * @param spinDeltaT delta-t used for spin/acceleration computation by finite differences
     */
    public QuaternionPolynomialProfile(final Frame frame,
            final AbsoluteDateInterval timeInterval,
            final List<QuaternionPolynomialSegment> polynomials,
            final String nature,
            final double spinDeltaT) {
        super(timeInterval, nature, spinDeltaT);
        this.referenceFrame = frame;
        this.segments = polynomials;
    }

    /**
     * Create a polynomial, quaternion guidance profile.
     * 
     * @param frame
     *        the reference frame of the polynomial functions
     * @param timeInterval
     *        interval of validity of the guidance profile
     * @param polynomials
     *        the list of polynomial guidance profile segments
     * @param spinDeltaT delta-t used for spin computation by finite differences
     */
    public QuaternionPolynomialProfile(final Frame frame,
            final AbsoluteDateInterval timeInterval,
            final List<QuaternionPolynomialSegment> polynomials,
            final double spinDeltaT) {
        this(frame, timeInterval, polynomials, DEFAULT_NATURE, spinDeltaT);
    }

    /**
     * @return the map containing the coefficients of the polynomial function representing q0,
     *         and their time interval of validity.
     */
    public Map<AbsoluteDateInterval, double[]> getQ0Coefficients() {
        final Map<AbsoluteDateInterval, double[]> coeffs = new ConcurrentHashMap<AbsoluteDateInterval, double[]>();
        for (int i = 0; i < this.segments.size(); i++) {
            final QuaternionPolynomialSegment segment = this.segments.get(i);
            coeffs.put(segment.getTimeInterval(), segment.getQ0Coefficients());
        }
        return coeffs;
    }

    /**
     * @return the map containing the coefficients of the polynomial function representing q1,
     *         and their time interval of validity.
     */
    public Map<AbsoluteDateInterval, double[]> getQ1Coefficients() {
        final Map<AbsoluteDateInterval, double[]> coeffs = new ConcurrentHashMap<AbsoluteDateInterval, double[]>();
        for (int i = 0; i < this.segments.size(); i++) {
            final QuaternionPolynomialSegment segment = this.segments.get(i);
            coeffs.put(segment.getTimeInterval(), segment.getQ1Coefficients());
        }
        return coeffs;
    }

    /**
     * @return the map containing the coefficients of the polynomial function representing q2,
     *         and their time interval of validity.
     */
    public Map<AbsoluteDateInterval, double[]> getQ2Coefficients() {
        final Map<AbsoluteDateInterval, double[]> coeffs = new ConcurrentHashMap<AbsoluteDateInterval, double[]>();
        for (int i = 0; i < this.segments.size(); i++) {
            final QuaternionPolynomialSegment segment = this.segments.get(i);
            coeffs.put(segment.getTimeInterval(), segment.getQ2Coefficients());
        }
        return coeffs;
    }

    /**
     * @return the map containing the coefficients of the polynomial function representing q3,
     *         and their time interval of validity.
     */
    public Map<AbsoluteDateInterval, double[]> getQ3Coefficients() {
        final Map<AbsoluteDateInterval, double[]> coeffs = new ConcurrentHashMap<AbsoluteDateInterval, double[]>();
        for (int i = 0; i < this.segments.size(); i++) {
            final QuaternionPolynomialSegment segment = this.segments.get(i);
            coeffs.put(segment.getTimeInterval(), segment.getQ3Coefficients());
        }
        return coeffs;
    }

    /**
     * @param date
     *        the date
     * @return the segment containing the date.
     */
    private QuaternionPolynomialSegment getSegment(final AbsoluteDate date) {
        for (int i = 0; i < this.segments.size(); i++) {
            final QuaternionPolynomialSegment segment = this.segments.get(i);
            if (segment.getTimeInterval().contains(date)) {
                return segment;
            }
        }
        throw PatriusException.createIllegalArgumentException(PatriusMessages.OUT_OF_RANGE_DATE_FOR_ATTITUDE_LAW);
    }

    /** {@inheritDoc} */
    @Override
    public Attitude getAttitude(final PVCoordinatesProvider pvProv,
            final AbsoluteDate date,
            final Frame frame) throws PatriusException {
        final AttitudeLeg attitudeProviderNoAcceleration = new AttitudeProviderNoAcceleration();
        final Attitude attitudeNoAcceleration = attitudeProviderNoAcceleration.getAttitude(pvProv, date, frame);
        
        // Spin derivative
        Vector3D acc = null;
        if (computeSpinDerivativesFlag) {
            acc = attitudeProviderNoAcceleration.computeSpinDerivativeByFD(pvProv, frame, date, getSpinDeltaT());
        }

        return new Attitude(date, frame, attitudeNoAcceleration.getRotation(), attitudeNoAcceleration.getSpin(), acc);
    }

    /** {@inheritDoc} */
    @Override
    public void setSpinDerivativesComputation(final boolean computeSpinDerivatives) {
        this.computeSpinDerivativesFlag = computeSpinDerivatives;
    }

    /** {@inheritDoc} */
    @Override
    public QuaternionPolynomialProfile copy(final AbsoluteDateInterval newInterval) {
        final QuaternionPolynomialProfile res = new QuaternionPolynomialProfile(referenceFrame, newInterval, segments,
                getNature(), getSpinDeltaT());
        res.setSpinDerivativesComputation(computeSpinDerivativesFlag);
        return res;
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
        public Attitude getAttitude(final PVCoordinatesProvider pvProv,
                final AbsoluteDate date,
                final Frame frame) throws PatriusException {
            checkDate(date);

            // Orientation in reference frame
            final QuaternionPolynomialSegment segment = QuaternionPolynomialProfile.this.getSegment(date);
            final Rotation rotation = segment.getOrientation(date);

            // transform the rotation from the reference frame to the new frame:
            final Transform toReferenceFrame = frame.getTransformTo(QuaternionPolynomialProfile.this.referenceFrame,
                    date);
            final Rotation inUserFrame = rotation.applyTo(toReferenceFrame.getRotation());

            // Orientation function
            final OrientationFunction rot = new AbstractOrientationFunction(date) {
                /** {@inheritDoc} */
                @Override
                public Rotation getOrientation(final AbsoluteDate date) throws PatriusException {
                    final QuaternionPolynomialSegment segment = QuaternionPolynomialProfile.this.getSegment(date);
                    return segment.getOrientation(date);
                }
            };

            // Spin function
            final double h = getSpinDeltaT();
            final Vector3DFunction spin = new AbstractVector3DFunction(date) {
                /** {@inheritDoc} */
                @Override
                public Vector3D getVector3D(final AbsoluteDate date) throws PatriusException {
                    return rot.estimateRateFunction(h, getTimeInterval()).getVector3D(date);
                }
            };

            // Build attitude without acceleration
            return new Attitude(date, frame, inUserFrame, spin.getVector3D(date));
        }
        
        /** {@inheritDoc} */
        @Override
        public AbsoluteDateInterval getTimeInterval() {
            return QuaternionPolynomialProfile.this.getTimeInterval();
        }
        
        /** {@inheritDoc} */
        @Override
        public AttitudeProviderNoAcceleration copy(final AbsoluteDateInterval newInterval) {
            // Unused
            return null;
        }
    };
}
