/**
 * 
 * Copyright 2011-2017 CNES
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
 * 
 * @history creation 15/06/2012
 * 
 *          HISTORY
* VERSION:4.8:FA:FA-2959:15/11/2021:[PATRIUS] Levee d'exception NullPointerException lors du calcul d'intersection a altitude
* VERSION:4.8:DM:DM-2958:15/11/2021:[PATRIUS] calcul d'intersection a altitude non nulle pour l'interface BodyShape 
* VERSION:4.7:FA:FA-2762:18/05/2021:Probleme lors des controles qualite via la PIC 
* VERSION:4.7:DM:DM-2909:18/05/2021:Ajout des methodes getIntersectionPoint, getClosestPoint(Vector2D) et getAlpha()
 * VERSION:4.6:DM:DM-2586:27/01/2021:[PATRIUS] intersection entre un objet de type «ExtendedOneAxisEllipsoid» 
 * et une droite. 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 *          VERSION:4.3:DM:DM-2102:15/05/2019:[Patrius] Refactoring du paquet fr.cnes.sirius.patrius.bodies
 *          VERSION:4.3:DM:DM-2099:15/05/2019:[PATRIUS] By-passer le critere du pas min dans l'integrateur numerique
 *          DOP853
 *          VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 *          VERSION::DM:227:09/04/2014:Merged eclipse detectors
 *          VERSION::FA:---:11/04/2014:Quality assurance
 *          VERSION::FA:471:03/11/2015:Modification method transform(point, frame, date) to take into account negative
 *          altitudes
 *          VERSION::DM:457:09/11/2015:Add the getflattening method to compute "Glint" direction
 *          VERSION::FA:650:22/07/2016: ellipsoid corrections
 *          VERSION::DM:611:04/08/2016:New implementation using radii provider for visibility of main/inhibition targets
 *          VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 *          VERSION::FA:708:13/12/2016: add documentation corrections
 *          VERSION::FA:829:25/01/2017:Protection of trigonometric methods
 *          VERSION::FA:1308:12/09/2017:correct Ellipsoid non-convergence issue
 *          VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 *          END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Ellipsoid;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.IEllipsoid;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Matrix3D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Sphere;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Spheroid;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * Implementation of the EllipsoidBodyShape interface : this an extended spheroid model to represent celestial bodies
 * shapes.
 * </p>
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment the use of frames makes this class not thread-safe
 * 
 * @see EllipsoidBodyShape
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: ExtendedOneAxisEllipsoid.java 17944 2017-09-12 15:24:41Z bignon $
 * 
 * @since 1.2
 * 
 */
public class ExtendedOneAxisEllipsoid implements EllipsoidBodyShape {

    /** Serializable UID. */
    private static final long serialVersionUID = -5136554736087949714L;

    /** the spheroid */
    private final IEllipsoid ellipsoid;

    /** the body frame */
    private final Frame inFrame;

    /** Flattening */
    private final double fIn;

    /** Equatorial radius. */
    private final double aeIn;

    /** Eccentricity power 2. */
    private final double e2;

    /** g * g. */
    private final double g2;

    /** shape name */
    private final String inName;

    /**
     * Constructor for the body spheroid.
     * 
     * @param ae
     *        equatorial radius
     * @param f
     *        the flattening (f = (a-b)/a)
     * @param bodyFrame
     *        body frame related to body shape
     * @param name
     *        the name of this shape
     */
    public ExtendedOneAxisEllipsoid(final double ae, final double f, final Frame bodyFrame, final String name) {

        // choice : sphere or spheroid ?
        if (MathLib.abs(f) < Precision.DOUBLE_COMPARISON_EPSILON) {
            this.ellipsoid = new Sphere(Vector3D.ZERO, ae);
        } else {
            this.ellipsoid = new Spheroid(Vector3D.ZERO, Vector3D.PLUS_K, ae, ae * (1. - f));
        }

        // Initialization
        this.inFrame = bodyFrame;
        this.fIn = f;
        this.aeIn = ae;
        this.e2 = f * (2.0 - f);
        final double g = 1.0 - f;
        this.g2 = g * g;
        this.inName = name;
    }

    /** {@inheritDoc} */
    @Override
    public Frame getBodyFrame() {
        return this.inFrame;
    }

    /** {@inheritDoc} */
    @Override
    public GeodeticPoint getIntersectionPoint(final Line line, final Vector3D close, final Frame frame,
                                              final AbsoluteDate date) throws PatriusException {
        // frame transformation of the line
        final Line newLine = this.frameTransformLine(line, frame, date);

        // get the intersection points
        final Vector3D[] points = this.ellipsoid.getIntersectionPoints(newLine);

        // initialize result
        Vector3D vectToConsider = null;

        // no intersection points
        if (points.length == 0) {
            return null;
        } else {
            // take the closest one
            double dist = points[0].distance(close);
            vectToConsider = points[0];

            // loop on others
            for (int i = 1; i < points.length; i++) {
                final double distToPoint = points[i].distance(close);
                if (distToPoint < dist) {
                    dist = distToPoint;
                    vectToConsider = points[i];
                }
            }
        }

        // return in demanded frame
        return this.transform(vectToConsider, this.inFrame, date);

    }

    /** {@inheritDoc} */
    @Override
    public GeodeticPoint getIntersectionPoint(final Line line, final Vector3D close, final Frame frame,
                                              final AbsoluteDate date, final double altitude) throws PatriusException {
        if (MathLib.abs(altitude) < EPS_ALTITUDE) {
            // Altitude is considered to be 0
            return getIntersectionPoint(line, close, frame, date);
        } else {
            // Build ellipsoid of required altitude
            final double aAlt = aeIn + altitude;
            final double fAlt = 1. - (aeIn * (1. - fIn) + altitude) / aAlt;
            final ExtendedOneAxisEllipsoid ellipsoidAlt = new ExtendedOneAxisEllipsoid(aAlt, fAlt, getBodyFrame(),
                    getName());
            // Compute and return intersection point
            final GeodeticPoint pointInAltitudeEllipsoid = ellipsoidAlt.getIntersectionPoint(line, close, frame, date);
            GeodeticPoint res = null;
            if (pointInAltitudeEllipsoid != null) {
                final Vector3D pointInBodyFrame = ellipsoidAlt.transform(pointInAltitudeEllipsoid);
                res = transform(pointInBodyFrame, inFrame, date);
            }
            return res;
        }
    }

    /** {@inheritDoc} */
    @Override
    public GeodeticPoint transform(final Vector3D point, final Frame frame,
                                   final AbsoluteDate date) throws PatriusException {

        // frame transformation
        final Transform trans = frame.getTransformTo(this.inFrame, date);
        final Vector3D transformedPoint = trans.transformPosition(point);

        // projection on the surface
        final Vector3D pointOnSurface = this.ellipsoid.closestPointTo(transformedPoint);

        // check relative distance : altitude can be negative
        final double distCenterPointOnSurface = this.ellipsoid.getCenter().distance(pointOnSurface);
        final double distCenterTransformedPoint = this.ellipsoid.getCenter().distance(transformedPoint);
        final double relativeDistance = distCenterTransformedPoint - distCenterPointOnSurface;

        // altitude (can be negative)
        final double altitude = (relativeDistance > 0.0) ? pointOnSurface.distance(transformedPoint)
            : -pointOnSurface.distance(transformedPoint);

        // longitude
        final double longitude = MathLib.atan2(pointOnSurface.getY(), pointOnSurface.getX());

        // latitude
        final Vector3D verticalVect = this.ellipsoid.getNormal(pointOnSurface);

        final double latitude = MathLib.asin(MathLib.min(1.0, MathLib.max(-1.0, verticalVect.getZ())));

        return new GeodeticPoint(latitude, longitude, altitude);
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D transform(final GeodeticPoint point) {
        // longitude, cosine and sine
        final double longitude = point.getLongitude();
        final double[] sincosLon = MathLib.sinAndCos(longitude);
        final double sLambda = sincosLon[0];
        final double cLambda = sincosLon[1];

        // latitude, cosine and sine
        final double latitude = point.getLatitude();
        final double[] sincosLat = MathLib.sinAndCos(latitude);
        final double sPhi = sincosLat[0];
        final double cPhi = sincosLat[1];

        // altitude
        final double h = point.getAltitude();
        final double n = MathLib.divide(this.aeIn, MathLib.sqrt(MathLib.max(0.0, 1.0 - this.e2 * sPhi * sPhi)));
        final double r = (n + h) * cPhi;

        // resulting vector
        return new Vector3D(r * cLambda, r * sLambda, (this.g2 * n + h) * sPhi);
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D[] getIntersectionPoints(final Line line, final Frame frame,
                                            final AbsoluteDate date) throws PatriusException {

        // frame transformation of the line
        final Line newLine = this.frameTransformLine(line, frame, date);

        final Vector3D[] points = this.ellipsoid.getIntersectionPoints(newLine);

        final Transform trans = this.inFrame.getTransformTo(frame, date);

        final Vector3D[] transformedPoints = new Vector3D[points.length];

        for (int i = 0; i < points.length; i++) {
            transformedPoints[i] = trans.transformPosition(points[i]);
        }

        return transformedPoints;
    }

    /** {@inheritDoc} */
    @Override
    public double distanceTo(final Line line, final Frame frame, final AbsoluteDate date) throws PatriusException {

        // frame transformation of the line
        final Line newLine = this.frameTransformLine(line, frame, date);

        // computation
        return this.ellipsoid.distanceTo(newLine);
    }

    /**
     * Transform a line in the body frame.
     * 
     * @param line
     *        the original line
     * @param frame
     *        the line's frame
     * @param date
     *        the current date
     * @return the transformed line
     * @exception PatriusException
     *            if line cannot be converted to body frame
     */
    private Line frameTransformLine(final Line line, final Frame frame,
                                    final AbsoluteDate date) throws PatriusException {

        // frame transformation
        final Transform trans = frame.getTransformTo(this.inFrame, date);

        // new line creation
        final Vector3D originLine = trans.transformPosition(line.getOrigin());
        final Vector3D secondPointLineInFrame = line.getOrigin().add(line.getDirection());
        final Vector3D secondPointLine = trans.transformPosition(secondPointLineInFrame);

        return new Line(originLine, secondPointLine);
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return this.inName;
    }

    /** {@inheritDoc} */
    @Override
    public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
        return this.inFrame.getTransformTo(frame, date).transformPVCoordinates(PVCoordinates.ZERO);
    }

    /** {@inheritDoc} */
    @Override
    public double getTransverseRadius() {
        return this.ellipsoid.getSemiA();
    }

    /** {@inheritDoc} */
    @Override
    public double getConjugateRadius() {
        return this.ellipsoid.getSemiC();
    }

    /**
     * Calculate the apparent radius.
     * Warning: When used with earth ellipsoid, the computation of sight axis local elevation on earth uses a
     * rapid algorithm with limited accuracy : typically 1.3e-4 degrees (worst case, satellite altitude 250 Km,
     * latitude 45 deg., East sight direction) if sight axis is 25 degrees above horizon, and 1.4e-6 degrees
     * (worst case) if sight axis is 0,25 degrees above horizon.
     * 
     * @param position
     *        spacecraft position
     * @param frame
     *        frame in which position is expressed
     * @param date
     *        date of position
     * @param occultedBody
     *        body occulted by this
     * @return apparent radius
     * @throws PatriusException
     *         if {@link PVCoordinatesProvider} computation fails
     */
    @Override
    public double getLocalRadius(final Vector3D position, final Frame frame, final AbsoluteDate date,
                                 final PVCoordinatesProvider occultedBody) throws PatriusException {

        if (MathLib.abs(1 - this.g2) > Precision.DOUBLE_COMPARISON_EPSILON) {

            // get Ellipsoid frame and semi radiis
            final double oA = this.getTransverseRadius();
            final double oC = this.getConjugateRadius();

            // positions of satellite and occulted body in ellipsoid frame
            final Vector3D pted = occultedBody.getPVCoordinates(date, this.inFrame).getPosition();
            final Vector3D psat = frame.getTransformTo(this.inFrame, date).transformPosition(position);

            // scaled positions wrt unit sphere
            final Vector3D ptedScaled = new Vector3D(MathLib.divide(pted.getX(), oA),
                MathLib.divide(pted.getY(), oA), MathLib.divide(pted.getZ(), oC));
            final Vector3D psatScaled = new Vector3D(MathLib.divide(psat.getX(), oA),
                MathLib.divide(psat.getY(), oA), MathLib.divide(psat.getZ(), oC));

            // transformations to and from unit sphere with
            // - X axis aligned with the (C,S') direction
            // - Z axis aligned with the (C,A') direction
            final Vector3D sphereZ = ptedScaled.normalize();
            final Vector3D sphereX = psatScaled.subtract(psatScaled.dotProduct(sphereZ), sphereZ).normalize();
            final Vector3D sphereY = sphereZ.crossProduct(sphereX);
            final double[][] matrixData = {
                { sphereX.getX(), sphereY.getX(), sphereZ.getX() },
                { sphereX.getY(), sphereY.getY(), sphereZ.getY() },
                { sphereX.getZ(), sphereY.getZ(), sphereZ.getZ() } };
            final Matrix3D standardBasisTransform = new Matrix3D(matrixData);
            final Matrix3D localBasisTransform = standardBasisTransform.transpose();

            // get H - point on horizon of unit sphere
            final Vector3D psatLocal = localBasisTransform.multiply(psatScaled);
            final double psatLocalNorm = psatLocal.getNorm();
            final double value1 = MathLib.divide(psatLocal.getZ(), psatLocalNorm);
            final double omega = MathLib.asin(MathLib.min(1.0, MathLib.max(-1.0, value1)));
            final double value2 = MathLib.divide(1, psatLocalNorm);
            final double alpha = MathLib.acos(MathLib.min(1.0, value2));
            final double theta = alpha + omega;
            final double[] sincosTheta = MathLib.sinAndCos(theta);
            final double sinTheta = sincosTheta[0];
            final double cosTheta = sincosTheta[1];
            final Vector3D hLocal = new Vector3D(cosTheta, psatLocal.getY(), sinTheta);

            // get back to standard basis
            final Vector3D hScaled = standardBasisTransform.multiply(hLocal);

            // unscale
            final Vector3D h = new Vector3D(hScaled.getX() * oA, hScaled.getY() * oA, hScaled.getZ() * oC);
            return psat.getNorm() * MathLib.sin(Vector3D.angle(h.subtract(psat).normalize(),
                psat.negate().normalize()));
        } else {
            return this.aeIn;
        }
    }

    /** {@inheritDoc} */
    @Override
    public double getEquatorialRadius() {
        return this.aeIn;
    }

    /** {@inheritDoc} */
    @Override
    public double getFlattening() {
        return this.fIn;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getNormal(final Vector3D point) {
        return this.ellipsoid.getNormal(point);
    }

    /**
     * Setter for Newton algorithm threshold used to compute distance to the ellipsoid using method
     * {@link #distanceTo(Line, Frame, AbsoluteDate)}.
     * Method {@link #distanceTo(Line, Frame, AbsoluteDate)} is used in particular in SensorModel.
     * Default value for this threshold is 1E-11.
     * 
     * @param newThreshold
     *        new threshold to set
     */
    public void setConvergenceThreshold(final double newThreshold) {
        if (this.ellipsoid instanceof Ellipsoid) {
            ((Ellipsoid) this.ellipsoid).setNewtonThreshold(newThreshold);
        }
    }

    /** {@inheritDoc} */
    @Override
    public double
        getApparentRadius(final Vector3D position, final Frame frame,
                          final AbsoluteDate date, final PVCoordinatesProvider occultedBody)
            throws PatriusException {
        return this.getLocalRadius(position, frame, date, occultedBody);
    }

    /** {@inheritDoc} */
    @Override
    public IEllipsoid getEllipsoid() {
        return this.ellipsoid;
    }
}