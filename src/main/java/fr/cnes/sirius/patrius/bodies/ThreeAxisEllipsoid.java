/**
 *
 * Copyright 2011-2022 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * HISTORY
 * VERSION:4.13:DM:DM-32:08/12/2023:[PATRIUS] Ajout d'un ThreeAxisEllipsoid
 * VERSION:4.13:FA:FA-112:08/12/2023:[PATRIUS] Probleme si Earth est utilise comme corps pivot pour mar097.bsp
 * VERSION:4.13:FA:FA-144:08/12/2023:[PATRIUS] la methode BodyShape.getBodyFrame devrait
 * retourner un CelestialBodyFrame
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import fr.cnes.sirius.patrius.bodies.BodyPoint.BodyPointName;
import fr.cnes.sirius.patrius.frames.CelestialBodyFrame;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Ellipsoid;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.IEllipsoid;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Three axis ellipsoid representation.
 * <p>
 * This ellipsoid is fully defined by its three axis radius (along the directions {@link Vector3D#PLUS_I} /
 * {@link Vector3D#PLUS_J} / {@link Vector3D#PLUS_K}) and its associated body frame.
 * </p>
 *
 * @concurrency not thread-safe
 *
 * @concurrency.comment the use of frames makes this class not thread-safe
 *
 * @see AbstractEllipsoidBodyShape
 *
 * @author Thibaut BONIT
 *
 * @since 4.13
 */
public class ThreeAxisEllipsoid extends AbstractEllipsoidBodyShape {

    /** Default ellipsoid name. */
    public static final String DEFAULT_THREE_AXIS_ELLIPSOID_NAME = "THREE_AXIS_ELLIPSOID";

    /** Serializable UID. */
    private static final long serialVersionUID = -4003786935879984705L;

    /**
     * Constructor for the three axis ellipsoid with default name.
     *
     * @param aRadius
     *        Transverse radius A: semi axis of the ellipsoid along the direction {@link Vector3D#PLUS_I}
     * @param bRadius
     *        Transverse radius B: semi axis of the ellipsoid along the direction {@link Vector3D#PLUS_J}
     * @param cRadius
     *        Conjugate radius C: semi axis of the ellipsoid along the direction {@link Vector3D#PLUS_K}
     * @param bodyFrame
     *        Body frame related to the ellipsoid
     */
    public ThreeAxisEllipsoid(final double aRadius,
            final double bRadius,
            final double cRadius,
            final CelestialBodyFrame bodyFrame) {
        this(aRadius, bRadius, cRadius, bodyFrame, DEFAULT_THREE_AXIS_ELLIPSOID_NAME);
    }

    /**
     * Constructor for the three axis ellipsoid.
     * 
     * @param aRadius
     *        Transverse radius A: semi axis of the ellipsoid along the direction {@link Vector3D#PLUS_I}
     * @param bRadius
     *        Transverse radius B: semi axis of the ellipsoid along the direction {@link Vector3D#PLUS_J}
     * @param cRadius
     *        Conjugate radius C: semi axis of the ellipsoid along the direction {@link Vector3D#PLUS_K}
     * @param bodyFrame
     *        Body frame related to the ellipsoid
     * @param name
     *        Name of the ellipsoid
     */
    public ThreeAxisEllipsoid(final double aRadius,
            final double bRadius,
            final double cRadius,
            final CelestialBodyFrame bodyFrame,
            final String name) {
        super(buildEllipsoid(aRadius, bRadius, cRadius), bodyFrame, isSpherical(aRadius, bRadius, cRadius), name);
    }

    /**
     * Internal method to build the ellipsoid according to its three axis radius.
     * 
     * @param aRadius
     *        Transverse radius A: semi axis of the ellipsoid along the direction {@link Vector3D#PLUS_I}
     * @param bRadius
     *        Transverse radius B: semi axis of the ellipsoid along the direction {@link Vector3D#PLUS_J}
     * @param cRadius
     *        Conjugate radius C: semi axis of the ellipsoid along the direction {@link Vector3D#PLUS_K}
     * @return the ellipsoid geometric representation
     */
    private static IEllipsoid buildEllipsoid(final double aRadius, final double bRadius, final double cRadius) {
        return new Ellipsoid(Vector3D.ZERO, Vector3D.PLUS_K, Vector3D.PLUS_I, aRadius, bRadius, cRadius);
    }

    /**
     * Internal method to determine if the ellipsoid can be considered as a sphere.
     * 
     * @param aRadius
     *        Transverse radius A: semi axis of the ellipsoid along the direction {@link Vector3D#PLUS_I}
     * @param bRadius
     *        Transverse radius B: semi axis of the ellipsoid along the direction {@link Vector3D#PLUS_J}
     * @param cRadius
     *        Conjugate radius C: semi axis of the ellipsoid along the direction {@link Vector3D#PLUS_K}
     * @return {@code true} if the ellipsoid can be considered as a sphere, {@code false} otherwise
     */
    private static boolean isSpherical(final double aRadius, final double bRadius, final double cRadius) {
        // Compute the flattening between the largest and the smallest radius
        final double minRadius = MathLib.min(aRadius, MathLib.min(bRadius, cRadius));
        final double maxRadius = MathLib.max(aRadius, MathLib.max(bRadius, cRadius));
        final double f = (maxRadius - minRadius) / maxRadius;

        // Evaluate the flattening to determine if the ellipsoid can be considered as a sphere
        final boolean isSpherical;
        if (MathLib.abs(f) < Precision.DOUBLE_COMPARISON_EPSILON) {
            isSpherical = true;
        } else {
            isSpherical = false;
        }
        return isSpherical;
    }

    /**
     * {@inheritDoc}
     * 
     * @return an {@link UnsupportedOperationException}
     * @deprecated since 4.13 as this method isn't relevant to this class description. This method will be only kept in
     *             the {@link OneAxisEllipsoid} class.
     */
    @Override
    @Deprecated
    public double getE2() {
        // Note: when the parent's deprecated method will be deleted, this method should be deleted too
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @return an {@link UnsupportedOperationException}
     * @deprecated since 4.13 as this method isn't relevant to this class description. This method will be only kept in
     *             the {@link OneAxisEllipsoid} class.
     */
    @Override
    @Deprecated
    public double getG2() {
        // Note: when the parent's deprecated method will be deleted, this method should be deleted too
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @return an {@link UnsupportedOperationException}
     * @deprecated since 4.13 as this method isn't relevant to this class description. This method will be only kept in
     *             the {@link OneAxisEllipsoid} class.
     */
    @Override
    @Deprecated
    public double getTransverseRadius() {
        // Note: when the parent's deprecated method will be deleted, this method should be deleted too
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @return an {@link UnsupportedOperationException}
     * @deprecated since 4.13 as this method isn't relevant to this class description. This method will be only kept in
     *             the {@link OneAxisEllipsoid} class.
     */
    @Override
    @Deprecated
    public double getConjugateRadius() {
        // Note: when the parent's deprecated method will be deleted, this method should be deleted too
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @return an {@link UnsupportedOperationException}
     * @deprecated since 4.13 as this method isn't relevant to this class description. This method will be only kept in
     *             the {@link OneAxisEllipsoid} class.
     */
    @Override
    @Deprecated
    public double getEquatorialRadius() {
        // Note: when the parent's deprecated method will be deleted, this method should be deleted too
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @return an {@link UnsupportedOperationException}
     * @deprecated since 4.13 as this method isn't relevant to this class description. This method will be only kept in
     *             the {@link OneAxisEllipsoid} class.
     */
    @Override
    @Deprecated
    public double getFlattening() {
        // Note: when the parent's deprecated method will be deleted, this method should be deleted too
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public EllipsoidPoint getIntersectionPoint(final Line line, final Vector3D close, final Frame frame,
                                               final AbsoluteDate date, final double altitude)
        throws PatriusException {

        if (MathLib.abs(altitude) < this.distanceEpsilon) {
            // Altitude is considered to be 0
            return getIntersectionPoint(line, close, frame, date);
        }

        // Build ellipsoid of required altitude
        final double aAlt = getARadius() + altitude;
        final double bAlt = getBRadius() + altitude;
        final double cAlt = getCRadius() + altitude;
        final ThreeAxisEllipsoid ellipsoidAlt = new ThreeAxisEllipsoid(aAlt, bAlt, cAlt, getBodyFrame(), getName());

        // Compute and return intersection point
        final EllipsoidPoint pointInAltitudeEllipsoid = ellipsoidAlt.getIntersectionPoint(line, close, frame, date);
        EllipsoidPoint res = null;
        if (pointInAltitudeEllipsoid != null) {
            final Vector3D pointInBodyFrame = pointInAltitudeEllipsoid.getPosition();
            res = new EllipsoidPoint(this, pointInBodyFrame, false, BodyPointName.INTERSECTION_AT_ALTITUDE);
        }
        return res;
    }

    /** {@inheritDoc} */
    @Override
    public ThreeAxisEllipsoid resize(final MarginType marginType, final double marginValue) {

        // Get Ellipsoid radius
        final double oA = getARadius();
        final double oB = getBRadius();
        final double oC = getCRadius();

        // Initialize radius
        final double newA;
        final double newB;
        final double newC;

        // Check the margin type
        if (marginType.equals(MarginType.DISTANCE)) {
            // The margin type is distance
            // Check if the margin value is larger than the opposite of the smallest radius, to be sure that the
            // resulting smallest radius will be positive
            if (marginValue > -MathLib.min(oA, MathLib.min(oB, oC))) {
                // Modify radius
                newA = oA + marginValue;
                newB = oB + marginValue;
                newC = oC + marginValue;
            } else {
                // Invalid margin value
                throw PatriusException
                    .createIllegalArgumentException(PatriusMessages.INVALID_MARGIN_VALUE, marginValue);
            }
        } else {
            // The margin type is scale factor
            // Check if the margin value is positive, to be sure that the scale factor has a physical meaning
            if (marginValue > 0) {
                // Modify radius
                newA = oA * marginValue;
                newB = oB * marginValue;
                newC = oC * marginValue;
            } else {
                // Invalid margin value
                throw PatriusException
                    .createIllegalArgumentException(PatriusMessages.INVALID_MARGIN_VALUE, marginValue);
            }
        }

        // Return new ThreeAxisEllipsoid with modified radius
        return new ThreeAxisEllipsoid(newA, newB, newC, getBodyFrame(), getName());
    }
}
