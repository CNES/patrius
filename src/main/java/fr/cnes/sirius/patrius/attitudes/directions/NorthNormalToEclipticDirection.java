/**
 * HISTORY
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11:DM:DM-3197:22/05/2023:[PATRIUS] Deplacement dans PATRIUS de classes façade ALGO DV SIRUS 
 * VERSION:4.11:FA:FA-3320:22/05/2023:[PATRIUS] Mauvaise implementation de la methode hashCode de Vector3D
 * END-HISTORY
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
package fr.cnes.sirius.patrius.attitudes.directions;

import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 *
 * Direction towards normal of ecliptic plane as computed in GCRF
 *
 * @concurrency immutable
 *
 * @author GMV
 */
public class NorthNormalToEclipticDirection implements IDirection {

    /** Serializable UID. */
    private static final long serialVersionUID = 7530638117827639871L;

    /** Earth center frame: GCRF. */
    private static final Frame EARTH_FRAME = FramesFactory.getGCRF();

    /** Celestial body for the SUN. */
    private final CelestialBody sunBody;

    /**
     * Constructor for celestial body center direction from Earth center : the celestial body's center is the target
     * point.
     *
     * @param sun
     *            the Sun celestial body
     */
    public NorthNormalToEclipticDirection(final CelestialBody sun) {
        this.sunBody = sun;
    }

    /**
     * compute direction of normal to ecliptic in GCRF
     *
     * @param date
     *            desired date
     * @return unit direction of normal to ecliptic in GCRF
     * @throws PatriusException
     *             if some frame specific errors occur
     */
    private final Vector3D computeDirNormalToEcliptic(final AbsoluteDate date) throws PatriusException {
        // get Sun PV as seen from Earth
        final PVCoordinates sunPV = sunBody.getPVCoordinates(date, EARTH_FRAME);

        // normal to Ecliptic in North sense (orbital momentum)
        final Vector3D normalToEcliptic;
        final Vector3D sunVelocity = sunPV.getVelocity();
        if (MathLib.abs(sunVelocity.getX()) <= Precision.DOUBLE_COMPARISON_EPSILON
                && MathLib.abs(sunVelocity.getY()) <= Precision.DOUBLE_COMPARISON_EPSILON
                && MathLib.abs(sunVelocity.getZ()) <= Precision.DOUBLE_COMPARISON_EPSILON) {
            // finite differences O(2) with a eps of 32 seconds
            final PVCoordinates sunPosPrev = sunBody.getPVCoordinates(date.shiftedBy(-32.), EARTH_FRAME);
            final PVCoordinates sunPosNext = sunBody.getPVCoordinates(date.shiftedBy(32.), EARTH_FRAME);
            final Vector3D velocity =
                sunPosNext.getPosition().subtract(sunPosPrev.getPosition()).scalarMultiply(1. / 64.);
            normalToEcliptic = Vector3D.crossProduct(sunPV.getPosition(), velocity);
        } else {
            normalToEcliptic = sunPV.getMomentum();
        }

        return normalToEcliptic.normalize();
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getVector(final PVCoordinatesProvider pvCoord, final AbsoluteDate date, final Frame frame)
        throws PatriusException {

        // normal to Ecliptic in North sense (orbital momentum)
        final Vector3D normalToEcliptic = computeDirNormalToEcliptic(date);

        // Transform to output frame
        final Transform t = EARTH_FRAME.getTransformTo(frame, date);

        // Return normal to Ecliptic in output frame
        return t.transformVector(normalToEcliptic);
    }

    /** {@inheritDoc} */
    @Override
    public Line getLine(final PVCoordinatesProvider pvCoord, final AbsoluteDate date, final Frame frame)
        throws PatriusException {

        // normal to Ecliptic in North sense (orbital momentum)
        final Vector3D normalToEcliptic = computeDirNormalToEcliptic(date);

        // Transform to output frame
        final Transform t = EARTH_FRAME.getTransformTo(frame, date);

        // Earth center in frame
        final Vector3D pvPosInFrame;
        if (pvCoord == null) {
            pvPosInFrame = t.transformPosition(Vector3D.ZERO);
        } else {
            pvPosInFrame = pvCoord.getPVCoordinates(date, frame).getPosition();
        }

        // Desired direction in frame
        final Vector3D dirInFrame = t.transformVector(normalToEcliptic);

        try {
            // creation of the line
            return new Line(pvPosInFrame, pvPosInFrame.add(dirInFrame));
        } catch (final IllegalArgumentException e) {
            throw new PatriusException(e, PatriusMessages.ILLEGAL_LINE);
        }
    }

}
