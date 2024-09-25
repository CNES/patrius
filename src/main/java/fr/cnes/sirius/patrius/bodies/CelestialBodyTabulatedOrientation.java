/**
 * Copyright 2002-2012 CS Systèmes d'Information
 * Copyright 2011-2022 CNES
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
 * VERSION:4.13:DM:DM-5:08/12/2023:[PATRIUS] Orientation d'un corps celeste sous forme de quaternions
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import fr.cnes.sirius.patrius.attitudes.TabulatedAttitude;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.TimeStampedAngularCoordinates;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Celestial body orientation represented by a tabulated attitude leg (quaternions).
 * 
 * @author Thibaut BONIT
 * 
 * @since 4.13
 */
public class CelestialBodyTabulatedOrientation implements CelestialBodyOrientation {

    /** Serializable UID. */
    private static final long serialVersionUID = -3610784796481081368L;

    /** The tabulated attitude leg representing the celestial body orientation. */
    private final TabulatedAttitude tabulatedAttitude;

    /** Finite difference delta value (0.5s by default). */
    private double dH;

    /**
     * Constructor.
     * 
     * @param tabulatedAttitude
     *        The tabulated attitude leg representing the celestial body orientation
     */
    public CelestialBodyTabulatedOrientation(final TabulatedAttitude tabulatedAttitude) {
        this.tabulatedAttitude = tabulatedAttitude;
        this.dH = 0.5; // default value
    }

    /**
     * Getter for the tabulated attitude leg representing the celestial body orientation.
     * 
     * @return the tabulated attitude leg
     */
    public TabulatedAttitude getTabulatedAttitude() {
        return this.tabulatedAttitude;
    }

    /**
     * Getter for the celestial body orientation at the specified date in the tabulated attitude leg's reference frame.
     * 
     * @param date
     *        current date
     * @return the celestial body orientation
     * @throws PatriusException
     *         if attitude cannot be computed for provided date
     */
    public TimeStampedAngularCoordinates getOrientation(final AbsoluteDate date) throws PatriusException {
        return this.tabulatedAttitude.getAttitude(date).getOrientation();
    }

    /**
     * Getter for the celestial body orientation at the specified date.
     * 
     * @param date
     *        current date
     * @param frame
     *        reference frame from which attitude is computed
     * @return the celestial body orientation
     * @throws PatriusException
     *         if attitude cannot be computed for provided date
     */
    public TimeStampedAngularCoordinates getOrientation(final AbsoluteDate date, final Frame frame)
        throws PatriusException {
        return this.tabulatedAttitude.getAttitude(date, frame).getOrientation();
    }

    /**
     * Getter for the finite difference delta value.
     * <p>
     * This value is used to compute the body pole directions by finite difference (see {@link #getPole} and
     * {@link #getPoleDerivative}.
     * </p>
     * <p>
     * Note that the body pole directions can only be computed on the interval:<br>
     * {@code [tabulated attitude's lower date + 2 * dH ; tabulated attitude's upper date - 2 * dH]}.
     * </p>
     * 
     * @return the finite difference delta value
     */
    public double getDH() {
        return this.dH;
    }

    /**
     * Setter for the finite difference delta value.
     * <p>
     * This value is used to compute the body pole directions by finite difference (see {@link #getPole} and
     * {@link #getPoleDerivative}.
     * </p>
     * <p>
     * Note that the body pole directions can only be computed on the interval:<br>
     * {@code [tabulated attitude's lower date + 2 * dH ; tabulated attitude's upper date - 2 * dH]}.
     * </p>
     * 
     * @param dHIn
     *        the finite difference delta value to set (0.5s by default)
     * @throws NotStrictlyPositiveException
     *         if {@code dH < Precision.DOUBLE_COMPARISON_EPSILON}
     */
    public void setDH(final double dHIn) {
        if (dHIn < Precision.DOUBLE_COMPARISON_EPSILON) {
            throw new NotStrictlyPositiveException(dHIn);
        }
        this.dH = dHIn;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Tabulated celestial body orientation";
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getPole(final AbsoluteDate date) throws PatriusException {
        checkDate(date);
        // Compute the pole by finite difference
        return AngularCoordinates.estimateRate(
            getOrientation(date.shiftedBy(-this.dH)).getRotation(),
            getOrientation(date.shiftedBy(this.dH)).getRotation(), 2. * this.dH);
    }

    /**
     * Check that the date is compatible with the available tabulated orientation interval.
     * 
     * @param date
     *        Date to check
     * @throws PatriusException
     *         if the date is outside the supported tabulated orientation interval
     */
    private void checkDate(final AbsoluteDate date) throws PatriusException {
        final AbsoluteDateInterval interval = this.tabulatedAttitude.getTimeInterval();
        if (date.durationFrom(interval.getLowerData().shiftedBy(this.dH)) < 0
                || date.durationFrom(interval.getUpperData().shiftedBy(-this.dH)) > 0) {
            throw new PatriusException(PatriusMessages.DATE_OUTSIDE_INTERVAL);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getPoleDerivative(final AbsoluteDate date) throws PatriusException {
        // Compute the pole derivative by finite difference
        final Vector3D poleMinusH = getPole(date.shiftedBy(-this.dH));
        final Vector3D polePlusH = getPole(date.shiftedBy(this.dH));
        return polePlusH.subtract(poleMinusH).scalarMultiply(1 / (2. * this.dH));
    }

    /** {@inheritDoc} */
    @Override
    public double getPrimeMeridianAngle(final AbsoluteDate date) throws PatriusException {
        return getAngularCoordinates(date, OrientationType.INERTIAL_TO_ROTATING).getRotation().getAngle();
    }

    /** {@inheritDoc} */
    @Override
    public double getPrimeMeridianAngleDerivative(final AbsoluteDate date) throws PatriusException {
        checkDate(date);

        return MathLib.divide(
            getPrimeMeridianAngle(date.shiftedBy(this.dH)) - getPrimeMeridianAngle(date.shiftedBy(-this.dH)),
            (2. * this.dH));
    }

    /** {@inheritDoc} */
    @Override
    public AngularCoordinates getAngularCoordinates(final AbsoluteDate date, final OrientationType orientationType)
        throws PatriusException {

        final AngularCoordinates coord;
        switch (orientationType) {
            case ICRF_TO_ROTATING:
                // Compute the orientation from the ICRF frame to the rotating frame
                coord = getOrientation(date);
                break;

            case ICRF_TO_INERTIAL:
                // Compute the orientation from the ICRF frame to the inertial frame
                final Vector3D pole = getPole(date);

                // If the frame isn't rotating, it is built as a BodyOrientedTabulated transform
                if (pole.equals(Vector3D.ZERO)) {
                    // Body orientation
                    coord = getOrientation(date);
                } else {

                    // Compute the rotation & the rotation rate
                    final Rotation rotation = getRotation(pole);
                    final Vector3D rotationRate = AngularCoordinates.estimateRate(
                        getRotation(getPole(date.shiftedBy(-this.dH))),
                        getRotation(getPole(date.shiftedBy(this.dH))), 2. * this.dH);

                    coord = new AngularCoordinates(rotation, rotationRate);
                }
                break;

            case INERTIAL_TO_ROTATING:
                // Compute the orientation from the inertial frame to the rotating frame
                // Frames composition: INERTIAL_TO_ROTATING = inv(ICRF_TO_INERTIAL) + ICRF_TO_ROTATING
                final AngularCoordinates angCoord1 = getAngularCoordinates(date, OrientationType.ICRF_TO_INERTIAL);
                final AngularCoordinates angCoord2 = getAngularCoordinates(date, OrientationType.ICRF_TO_ROTATING);
                final Frame icrf = FramesFactory.getICRF();
                final Frame f1 = new Frame(icrf, new Transform(date, angCoord1).getInverse(), "f1");
                final Frame f2 = new Frame(f1, new Transform(date, angCoord2), "f2");
                final Transform t = icrf.getTransformTo(f2, date);
                coord = t.getAngular();
                break;

            default:
                // Shouldn't happened (internal error)
                throw new EnumConstantNotPresentException(OrientationType.class, orientationType.toString());
        }
        return coord;
    }

    /**
     * Compute the rotation from a pole.
     * 
     * @param pole
     *        Pole
     * @return rotation
     */
    private static Rotation getRotation(final Vector3D pole) {
        Vector3D qNode = Vector3D.crossProduct(Vector3D.PLUS_K, pole);
        if (qNode.getNormSq() < Precision.SAFE_MIN) {
            qNode = Vector3D.PLUS_I;
        }
        return new Rotation(Vector3D.PLUS_K, Vector3D.PLUS_I, pole, qNode);
    }
}
