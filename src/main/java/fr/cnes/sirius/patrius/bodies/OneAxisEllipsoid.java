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
 *
 * @history creation 15/06/2012
 *
 * HISTORY
 * VERSION:4.11.1:FA:FA-52:30/06/2023:[PATRIUS] Précision dans la méthode FacetBodyShape.getFieldData
 * VERSION:4.11.1:FA:FA-84:30/06/2023:[PATRIUS] Correction de la DM 3249
 * VERSION:4.11:DM:DM-3249:22/05/2023:[PATRIUS] Amelioration des transformations entre GeodeticPoint et Vector3D
 * VERSION:4.11:FA:FA-3322:22/05/2023:[PATRIUS] Erreur dans le calcul de normale autour d’un OneAxisEllipsoid
 * VERSION:4.11:FA:FA-47:22/05/2023:[PATRIUS] OneAxisEllipsoid.getApparentRadius erreur pour ptedScaled nul
 * VERSION:4.10.3:FA:FA-47:11/05/2023:[PATRIUS] OneAxisEllipsoid.getApparentRadius erreur pour ptedScaled nul
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:DM:DM-3194:03/11/2022:[PATRIUS] Fusion des interfaces GeometricBodyShape et BodyShape 
 * VERSION:4.9.1:FA:FA-3193:01/06/2022:[PATRIUS] Revenir a la signature initiale de la methode getLocalRadius
 * VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3165:10/05/2022:[PATRIUS] Amelioration de la propagation du signal 
 * VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
 * VERSION:4.9:DM:DM-3135:10/05/2022:[PATRIUS] Calcul d'intersection sur BodyShape  
 * VERSION:4.9:DM:DM-3127:10/05/2022:[PATRIUS] Ajout de deux methodes resize a l'interface GeometricBodyShape ...
 * VERSION:4.9:FA:FA-3170:10/05/2022:[PATRIUS] Incoherence de datation entre la methode getLocalRadius...
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:FA:FA-2959:15/11/2021:[PATRIUS] Levee d'exception NullPointerException lors du calcul d'intersection
 * a altitude
 * VERSION:4.8:DM:DM-2958:15/11/2021:[PATRIUS] calcul d'intersection a altitude non nulle pour l'interface BodyShape 
 * VERSION:4.7:FA:FA-2762:18/05/2021:Probleme lors des controles qualite via la PIC 
 * VERSION:4.7:DM:DM-2909:18/05/2021:Ajout des methodes getIntersectionPoint, getClosestPoint(Vector2D) et getAlpha()
 * VERSION:4.6:DM:DM-2586:27/01/2021:[PATRIUS] intersection entre un objet de type «ExtendedOneAxisEllipsoid»
 * et une droite. 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2102:15/05/2019:[Patrius] Refactoring du paquet fr.cnes.sirius.patrius.bodies
 * VERSION:4.3:DM:DM-2099:15/05/2019:[PATRIUS] By-passer le critere du pas min dans l'integrateur numerique
 * DOP853
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:227:09/04/2014:Merged eclipse detectors
 * VERSION::FA:---:11/04/2014:Quality assurance
 * VERSION::FA:471:03/11/2015:Modification method transform(point, frame, date) to take into account negative
 * altitudes
 * VERSION::DM:457:09/11/2015:Add the getflattening method to compute "Glint" direction
 * VERSION::FA:650:22/07/2016: ellipsoid corrections
 * VERSION::DM:611:04/08/2016:New implementation using radii provider for visibility of main/inhibition targets
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::FA:708:13/12/2016: add documentation corrections
 * VERSION::FA:829:25/01/2017:Protection of trigonometric methods
 * VERSION::FA:1308:12/09/2017:correct Ellipsoid non-convergence issue
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Ellipsoid;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.IEllipsoid;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Matrix3D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Sphere;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.SphericalCoordinates;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Spheroid;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.events.AbstractDetector.PropagationDelayType;
import fr.cnes.sirius.patrius.signalpropagation.VacuumSignalPropagationModel;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

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
public class OneAxisEllipsoid implements EllipsoidBodyShape {

    /** Serializable UID. */
    private static final long serialVersionUID = -6515537942032843597L;

    /** Default epsilon (s) for signal propagation computation. */
    private static final double DEFAULT_EPSILON_SIGNAL_PROPAGATION = 1E-14;

    /** Close approach threshold. */
    private static final double CLOSE_APPROACH_THRESHOLD = 1.0e-10;

    /** Default ellipsoid name */
    private static final String DEFAULT_ONE_AXIS_ELLIPSOID_NAME = "ONE_AXIS_ELLIPSOID";

    /** Epsilon for signal propagation computation. */
    private double epsSignalPropagation = DEFAULT_EPSILON_SIGNAL_PROPAGATION;

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
     * Constructor for the body spheroid with default name.
     *
     * @param ae
     *        equatorial radius
     * @param f
     *        the flattening (f = (a-b)/a)
     * @param bodyFrame
     *        body frame related to body shape
     */
    public OneAxisEllipsoid(final double ae,
            final double f,
            final Frame bodyFrame) {
        this(ae, f, bodyFrame, DEFAULT_ONE_AXIS_ELLIPSOID_NAME);
    }

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
    public OneAxisEllipsoid(final double ae,
            final double f,
            final Frame bodyFrame,
            final String name) {

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
    public GeodeticPoint getIntersectionPoint(final Line line,
            final Vector3D close,
            final Frame frame,
            final AbsoluteDate date) throws PatriusException {
        // frame transformation of the line
        final Line newLine = this.frameTransformLine(line, frame, date);

        // get the intersection points
        final Vector3D[] points = this.ellipsoid.getIntersectionPoints(newLine);

        // no intersection points
        if (points.length == 0) {
            return null;
        }

        // initialize Vector3D result in input frame
        Vector3D vectToConsider = null;

        // define the minimum distance to positive infinity to be sure that the distToPoint of the first valid point
        // will be smaller than this minimum distance
        Double minDist = Double.POSITIVE_INFINITY;
        // initialize the boolean which identifies the first valid point to true
        boolean firstValidPoint = true;
        // loop on all the points
        for (final Vector3D point : points) {
            // check if the intersection point is on the right side of the line by comparing its abscissa with the
            // minimum abscissa
            if (newLine.getAbscissa(point) > newLine.getMinAbscissa()) {
                if (firstValidPoint) {
                    minDist = point.distance(close);
                    vectToConsider = point;
                    // set the boolean which identifies the first valid point to false
                    firstValidPoint = false;
                }
                final double distToPoint = point.distance(close);
                if (distToPoint < minDist) {
                    minDist = distToPoint;
                    vectToConsider = point;
                }
            }
        }

        // Check if there are valid intersection points
        if (vectToConsider == null) {
            return null;
        }

        // return in demanded frame
        return this.transform(vectToConsider, this.inFrame, date);
    }

    /** {@inheritDoc} */
    @Override
    public GeodeticPoint getIntersectionPoint(final Line line,
            final Vector3D close,
            final Frame frame,
            final AbsoluteDate date,
            final double altitude) throws PatriusException {
        if (MathLib.abs(altitude) < EPS_ALTITUDE) {
            // Altitude is considered to be 0
            return getIntersectionPoint(line, close, frame, date);
        }

        // Build ellipsoid of required altitude
        final double aAlt = this.aeIn + altitude;
        final double fAlt = 1. - (this.aeIn * (1. - this.fIn) + altitude) / aAlt;
        final OneAxisEllipsoid ellipsoidAlt = new OneAxisEllipsoid(aAlt, fAlt, getBodyFrame(), getName());
        // Compute and return intersection point
        final GeodeticPoint pointInAltitudeEllipsoid = ellipsoidAlt.getIntersectionPoint(line, close, frame, date);
        GeodeticPoint res = null;
        if (pointInAltitudeEllipsoid != null) {
            final Vector3D pointInBodyFrame = ellipsoidAlt.transform(pointInAltitudeEllipsoid);
            res = transform(pointInBodyFrame, this.inFrame, date);
        }
        return res;
    }

    /** {@inheritDoc} */
    @Override
    public GeodeticPoint transform(final Vector3D point,
            final Frame frame,
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
        final double altitude = (relativeDistance > 0.0) ? pointOnSurface.distance(transformedPoint) : -pointOnSurface
                .distance(transformedPoint);

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
        final double n = MathLib.divide(this.aeIn, MathLib.sqrt(1.0 - this.e2 * sPhi * sPhi));
        final double r = (n + h) * cPhi;

        // resulting vector
        return new Vector3D(r * cLambda, r * sLambda, (this.g2 * n + h) * sPhi);
    }

    /** {@inheritDoc} */
    @Override
    public GeodeticPoint transformFromZenith(final Vector3D point,final Vector3D zenith, final Frame frame,
                                          final AbsoluteDate date) throws PatriusException {

        if (zenith == null) {
            // Use regular transform of the ellipsoid
            return this.transform(point, frame, date);
        } else {
            // User-defined zenith: use spherical coordinates

            // Compute transform from frame to body frame
            final Transform transform = frame.getTransformTo(this.inFrame, date);

            // Transform zenith vector in body frame
            final Vector3D transformedZenith = transform.transformPosition(zenith);

            // Compute normal spherical coordinates
            final SphericalCoordinates sphericalCoordinates = new SphericalCoordinates(transformedZenith);

            // Return geodetic point computed from these coordinates
            return new GeodeticPoint(sphericalCoordinates.getDelta(), sphericalCoordinates.getAlpha(), 0);
        }

    }

    /** {@inheritDoc} */
    @Override
    public Vector3D[] getIntersectionPoints(final Line line,
            final Frame frame,
            final AbsoluteDate date) throws PatriusException {

        // frame transformation of the line
        final Line newLine = this.frameTransformLine(line, frame, date);

        final Vector3D[] points = this.ellipsoid.getIntersectionPoints(newLine);

        final Transform trans = this.inFrame.getTransformTo(frame, date);

        // define the list containing the intersection points on the right side of the line
        final List<Vector3D> pointsList = new ArrayList<>();
        // compute the number of intersection points on the right side of the line
        for (final Vector3D point : points) {
            // check if the intersection point is on the right side of the line by comparing its abscissa with the
            // minimum abscissa
            if (newLine.getAbscissa(point) > newLine.getMinAbscissa()) {
                pointsList.add(point);
            }
        }

        // define the array of transformed points with the correct length
        final Vector3D[] transformedPoints = new Vector3D[pointsList.size()];
        // loop on the list of valid intersection points
        for (int i = 0; i < pointsList.size(); i++) {
            // fill in the array of valid transformed points
            transformedPoints[i] = trans.transformPosition(pointsList.get(i));
        }

        return transformedPoints;
    }

    /** {@inheritDoc} */
    @Override
    public double distanceTo(final Line line,
            final Frame frame,
            final AbsoluteDate date) throws PatriusException {

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
    private Line frameTransformLine(final Line line,
            final Frame frame,
            final AbsoluteDate date) throws PatriusException {

        // frame transformation
        final Transform trans = frame.getTransformTo(this.inFrame, date);

        // new line creation
        final Vector3D originLine = trans.transformPosition(line.getOrigin());
        final Vector3D secondPointLineInFrame = line.getOrigin().add(
                line.getDirection().scalarMultiply(DIRECTION_FACTOR));
        final Vector3D secondPointLine = trans.transformPosition(secondPointLineInFrame);
        final Vector3D minAbscissaLine = trans.transformPosition(line.pointAt(line.getMinAbscissa()));

        return new Line(originLine, secondPointLine, minAbscissaLine);
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return this.inName;
    }

    /** {@inheritDoc} */
    @Override
    public PVCoordinates getPVCoordinates(final AbsoluteDate date,
            final Frame frame) throws PatriusException {
        return this.inFrame.getPVCoordinates(date, frame);
    }

    /** {@inheritDoc} */
    @Override
    public Frame getNativeFrame(final AbsoluteDate date,
            final Frame frame) throws PatriusException {
        return this.inFrame;
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
     *
     * {@inheritDoc}
     * <p>
     * Warning: When used with earth ellipsoid, the computation of sight axis local elevation on earth uses a rapid
     * algorithm with limited accuracy : typically 1.3e-4 degrees (worst case, satellite altitude 250 Km, latitude 45
     * deg., East sight direction) if sight axis is 25 degrees above horizon, and 1.4e-6 degrees (worst case) if sight
     * axis is 0,25 degrees above horizon.
     * </p>
     */
    @Override
    public double getApparentRadius(final PVCoordinatesProvider pvObserver,
            final AbsoluteDate date,
            final PVCoordinatesProvider occultedBody,
            final PropagationDelayType propagationDelayType) throws PatriusException {

        if (MathLib.abs(1 - this.g2) > Precision.DOUBLE_COMPARISON_EPSILON) {

            // get Ellipsoid frame and semi radiis
            final double oA = this.getTransverseRadius();
            final double oC = this.getConjugateRadius();

            // Initialization (case of instantaneous propagation)
            AbsoluteDate occultingReceptionDate = date;
            Frame frameAtOccultingDate = this.inFrame;
            AbsoluteDate emissionDate = date;

            // Case of light speed propagation (dedicated in order to optimize computation times)
            if (propagationDelayType.equals(PropagationDelayType.LIGHT_SPEED)) {
                // Native frames
                final Frame nativeFrameObserver = pvObserver.getNativeFrame(date, this.inFrame);
                final Frame nativeFrameOccultingBody = this.getNativeFrame(date, this.inFrame);
                final Frame nativeFrameOccultedBody = occultedBody.getNativeFrame(date, this.inFrame);

                // Position of emitter is at date of emission taking into account light speed if required
                // Signal propagation is performed in closest (in term of distance on frames tree) inertial frame
                final Frame inertialFrame = nativeFrameObserver
                        .getFirstCommonPseudoInertialAncestor(nativeFrameOccultedBody);
                emissionDate = VacuumSignalPropagationModel.getSignalEmissionDate(occultedBody, pvObserver, date,
                    this.epsSignalPropagation, propagationDelayType, inertialFrame);

                // Occulting body data (defined by its frame) are computed when signal from occulted body is received
                // This frame is then frozen in order to perform computations since this occulting body is only defined
                // by its frame
                final Frame occultingInertialFrame = nativeFrameOccultingBody
                        .getFirstCommonPseudoInertialAncestor(nativeFrameOccultedBody);
                occultingReceptionDate = VacuumSignalPropagationModel.getSignalReceptionDate(this, occultedBody,
                    emissionDate, this.epsSignalPropagation, propagationDelayType, occultingInertialFrame);
                frameAtOccultingDate = this.inFrame.getFrozenFrame(FramesFactory.getICRF(), occultingReceptionDate,
                        this.inFrame.getName() + "-Frozen");
            }

            // positions of satellite and occulted body in ellipsoid frame
            final Vector3D pted = occultedBody.getPVCoordinates(emissionDate, frameAtOccultingDate).getPosition();
            final Vector3D psat = pvObserver.getPVCoordinates(date, frameAtOccultingDate).getPosition();

            // scaled positions wrt unit sphere
            final Vector3D ptedScaled = new Vector3D(MathLib.divide(pted.getX(), oA), MathLib.divide(pted.getY(), oA),
                    MathLib.divide(pted.getZ(), oC));
            final Vector3D psatScaled = new Vector3D(MathLib.divide(psat.getX(), oA), MathLib.divide(psat.getY(), oA),
                    MathLib.divide(psat.getZ(), oC));

            // transformations to and from unit sphere with
            // - X axis aligned with the (C,S') direction
            // - Z axis aligned with the (C,A') direction
            final Vector3D sphereZ;
            // Compute sphereZ
            if (ptedScaled.equals(Vector3D.ZERO)) {
                sphereZ = ptedScaled;
            } else {
                sphereZ = ptedScaled.normalize();
            }
            final Vector3D sphereX = psatScaled.subtract(psatScaled.dotProduct(sphereZ), sphereZ).normalize();
            final Vector3D sphereY = sphereZ.crossProduct(sphereX);
            final double[][] matrixData = { { sphereX.getX(), sphereY.getX(), sphereZ.getX() },
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
            return MathLib.min(MathLib.max(
                    psat.getNorm()
                            * MathLib.sin(Vector3D.angle(h.subtract(psat).normalize(), psat.negate().normalize())),
                    this.getConjugateRadius()), this.getEquatorialRadius());
        }

        return this.aeIn;
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
    public Vector3D getNormal(final Vector3D point) throws PatriusException {
        // Compute the zenith of the geodetic point
        return this.transform(point, this.inFrame, null).getZenith();
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
    public IEllipsoid getEllipsoid() {
        return this.ellipsoid;
    }

    /** {@inheritDoc} */
    @Override
    public OneAxisEllipsoid resize(final MarginType marginType,
            final double marginValue) throws PatriusException {
        // Initialize equatorial radius
        double aAlt = 0.;
        // Initialize flattening
        double fAlt = 0.;
        // Check the margin type
        if (marginType.equals(MarginType.DISTANCE)) {
            // The margin type is distance
            // Check if the margin value is larger than the opposite of the polar (smallest) radius, to be sure that
            // the resulting polar (smallest) radius will be positive
            if (marginValue > -this.getEquatorialRadius() * (1 - this.getFlattening())) {
                // Modify equatorial radius
                aAlt = this.aeIn + marginValue;
                // Modify flattening
                fAlt = 1. - (this.aeIn * (1. - this.fIn) + marginValue) / aAlt;
            } else {
                // Invalid margin value
                throw PatriusException
                        .createIllegalArgumentException(PatriusMessages.INVALID_MARGIN_VALUE, marginValue);
            }
        } else {
            // The margin type is scale factor
            // Check if the margin value is positive, to be sure that the scale factor has a physical meaning
            if (marginValue > 0) {
                // Modify equatorial radius
                aAlt = this.aeIn * marginValue;
                // To modify the polar radius, there is need to modify the flattening
                fAlt = this.fIn;
            } else {
                // Invalid margin value
                throw PatriusException
                        .createIllegalArgumentException(PatriusMessages.INVALID_MARGIN_VALUE, marginValue);
            }
        }

        // Return new OneAxisEllipsoid with modified equatorial radius and flattening
        return new OneAxisEllipsoid(aAlt, fAlt, this.inFrame, this.inName);
    }

    /**
     * Transform a surface-relative point to a cartesian point and compute the jacobian of
     * the transformation.
     *
     * @param geodeticPoint
     *        geodetic point
     * @param jacobian
     *        the jacobian matrix which will be computed (input and output of the method)
     *
     * @return point at the same location but as a cartesian point
     * @throws PatriusException
     *         e
     */
    public Vector3D transformAndComputeJacobian(final GeodeticPoint geodeticPoint,
            final double[][] jacobian) throws PatriusException {
        final Vector3D transformedPoint = this.transform(geodeticPoint);
        this.computeJacobian(transformedPoint, geodeticPoint, jacobian);
        return transformedPoint;
    }

    /**
     * Transform a cartesian point to a surface-relative point and compute the jacobian of
     * the transformation.
     *
     * @param point
     *        cartesian point
     * @param frame
     *        frame in which cartesian point is expressed
     * @param date
     *        date of the point in given frame
     * @param jacobian
     *        the jacobian matrix which will be computed (input and output of the method)
     *
     * @return point at the same location but as a surface-relative point,
     *         expressed in body frame
     *
     * @exception PatriusException
     *            if point cannot be converted to body frame
     */
    public GeodeticPoint transformAndComputeJacobian(final Vector3D point,
            final Frame frame,
            final AbsoluteDate date,
            final double[][] jacobian) throws PatriusException {
        final GeodeticPoint transformedPoint = this.transform(point, frame, date);
        this.computeJacobian(transformedPoint, point, jacobian);
        return transformedPoint;
    }

    /**
     * Compute the jacobian matrix of the transformation from Cartesian point to geodetic point.
     *
     * See "Algorithmes des routines du th&egraveme "changement de variables et de rep&egravere pour
     * la trajectographie" de la MSLIB (edition 4)"
     *
     * @param geodeticPoint
     *        geodetic point
     * @param cartesianPoint
     *        cartesian point
     * @param jacobian
     *        the jacobian matrix which will be computed (input and output of the method)
     * @throws PatriusException
     *         e
     *
     */
    private void computeJacobian(final GeodeticPoint geodeticPoint,
            final Vector3D cartesianPoint,
            final double[][] jacobian) throws PatriusException {

        // Cartesian coordinates
        final double x = cartesianPoint.getX();
        final double y = cartesianPoint.getY();
        final double dist = MathLib.sqrt(x * x + y * y);

        // case : the point is close to the poles
        if (dist < (CLOSE_APPROACH_THRESHOLD * this.aeIn)) {
            // the point is close to one of the poles, the jacobian matrix cannot be computed
            throw new PatriusException(PatriusMessages.JACOBIAN_UNDEFINED);
        }
        final double sinLat = MathLib.sin(geodeticPoint.getLatitude());
        final double r = this.aeIn / MathLib.sqrt(1 - (1 - this.g2) * sinLat * sinLat);
        final double k = this.g2 / (1 - (1 - this.g2) * sinLat * sinLat);

        // the altitude of the point is "too negative"
        if ((k * r + geodeticPoint.getAltitude()) < (CLOSE_APPROACH_THRESHOLD * this.aeIn)) {
            throw new PatriusException(PatriusMessages.JACOBIAN_UNDEFINED);
        }

        // Temporary variables
        final double[] sincos = MathLib.sinAndCos(geodeticPoint.getLongitude());
        final double sinLon = sincos[0];
        final double cosLon = sincos[1];

        final double alt = geodeticPoint.getAltitude();
        final double cosLat = MathLib.cos(geodeticPoint.getLatitude());

        // components of the jacobian matrix
        // Parameter is directly modified
        jacobian[0][0] = -sinLat * cosLon / (k * r + alt);
        jacobian[0][1] = -sinLat * sinLon / (k * r + alt);
        jacobian[0][2] = cosLat / (k * r + alt);
        jacobian[1][0] = -sinLon / ((r + alt) * cosLat);
        jacobian[1][1] = cosLon / ((r + alt) * cosLat);
        jacobian[1][2] = 0.0;
        jacobian[2][0] = cosLat * cosLon;
        jacobian[2][1] = cosLat * sinLon;
        jacobian[2][2] = sinLat;
    }

    /**
     * Compute the jacobian matrix of the transformation from geodetic point to Cartesian point.
     *
     * See "Algorithmes des routines du th&egraveme "changement de variables et de rep&egravere pour
     * la trajectographie" de la MSLIB (edition 4)"
     *
     * @param cartesianPoint
     *        cartesian point
     * @param geodeticPoint
     *        geodetic point
     * @param jacobian
     *        the jacobian matrix which will be computed (input and output of the method)
     *
     */
    private void computeJacobian(final Vector3D cartesianPoint,
            final GeodeticPoint geodeticPoint,
            final double[][] jacobian) {

        // Temporary variables
        final double[] sincosLon = MathLib.sinAndCos(geodeticPoint.getLongitude());
        final double sinLon = sincosLon[0];
        final double cosLon = sincosLon[1];
        final double[] sincosLat = MathLib.sinAndCos(geodeticPoint.getLatitude());
        final double sinLat = sincosLat[0];
        final double cosLat = sincosLat[1];

        final double alt = geodeticPoint.getAltitude();
        // local radius
        final double r = this.aeIn / MathLib.sqrt(MathLib.max(0.0, 1 - (1 - this.g2) * sinLat * sinLat));
        final double k = this.g2 / (1 - (1 - this.g2) * sinLat * sinLat);

        // components of the jacobian matrix
        jacobian[0][0] = -(k * r + alt) * sinLat * cosLon;
        jacobian[0][1] = -(r + alt) * cosLat * sinLon;
        jacobian[0][2] = cosLat * cosLon;
        jacobian[1][0] = -(k * r + alt) * sinLat * sinLon;
        jacobian[1][1] = (r + alt) * cosLat * cosLon;
        jacobian[1][2] = cosLat * sinLon;
        jacobian[2][0] = (k * r + alt) * cosLat;
        jacobian[2][1] = 0.0;
        jacobian[2][2] = sinLat;

        // No result to return, jacobian modified directly
    }

    /**
     * Set the epsilon for signal propagation used in
     * {@link #getApparentRadius(PVCoordinatesProvider, AbsoluteDate, PVCoordinatesProvider, PropagationDelayType)}
     * method.
     * This epsilon (in s) directly reflect the accuracy of signal propagation (1s of accuracy = 3E8m of accuracy on
     * distance between emitter and receiver)
     * @param epsilon epsilon for signal propagation
     */
    public void setEpsilonSignalPropagation(final double epsilon) {
        this.epsSignalPropagation = epsilon;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D[] closestPointTo(final Line line) {
        // Compute points thanks to the method of the ellipsoid
        return this.ellipsoid.closestPointTo(line);
    }
}
