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
 * VERSION:4.13:DM:DM-37:08/12/2023:[PATRIUS] Date d'evenement et propagation du signal
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.13:FA:FA-112:08/12/2023:[PATRIUS] Probleme si Earth est utilise comme corps pivot pour mar097.bsp
 * VERSION:4.13:FA:FA-118:08/12/2023:[PATRIUS] Calcul d'union de PyramidalField invalide
 * VERSION:4.13:DM:DM-32:08/12/2023:[PATRIUS] Ajout d'un ThreeAxisEllipsoid
 * VERSION:4.13:DM:DM-68:08/12/2023:[PATRIUS] Ajout du repere G50 CNES
 * VERSION:4.13:DM:DM-132:08/12/2023:[PATRIUS] Suppression de la possibilite
 * de convertir les sorties de VacuumSignalPropagation
 * VERSION:4.13:FA:FA-133:08/12/2023:[PATRIUS] Conversion en trop dans OneAxisEllipsoid#getIntersectionPoints
 * VERSION:4.13:FA:FA-144:08/12/2023:[PATRIUS] la methode BodyShape.getBodyFrame devrait
 * retourner un CelestialBodyFrame
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.bodies.BodyPoint.BodyPointName;
import fr.cnes.sirius.patrius.events.detectors.AbstractSignalPropagationDetector.PropagationDelayType;
import fr.cnes.sirius.patrius.frames.CelestialBodyFrame;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Ellipsoid;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.IEllipsoid;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Matrix3D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.SphericalCoordinates;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.signalpropagation.VacuumSignalPropagationModel;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Abstract class for an ellipsoid body shape to mutualize parameters and features.
 *
 * @see AbstractBodyShape
 * @see EllipsoidBodyShape
 * 
 * @author Thibaut BONIT
 *
 * @since 4.13
 */
public abstract class AbstractEllipsoidBodyShape extends AbstractBodyShape implements EllipsoidBodyShape {

    /** Ellipsodetic used as default LLH coordinates system for the computed {@link EllipsoidPoint}. */
    public static final LLHCoordinatesSystem DEFAULT_LLH_COORD_SYSTEM = LLHCoordinatesSystem.ELLIPSODETIC;

    /** Close approach threshold. */
    public static final double CLOSE_APPROACH_THRESHOLD = 1e-10;

    /** Serializable UID. */
    private static final long serialVersionUID = -1975100194734673504L;

    /** Ellipsoid geometric representation. */
    protected final IEllipsoid ellipsoid;

    /** Maximum number of iterations for signal propagation computation */
    protected int maxIterSignalPropagation = VacuumSignalPropagationModel.DEFAULT_MAX_ITER;

    /** Indicate if the ellipsoid can be considered as a sphere or not. */
    private final boolean spherical;

    /**
     * Constructor for the ellipsoid.
     * <p>
     * By default, the {@link #DEFAULT_LLH_COORD_SYSTEM} is used and the ellipsoid isn't considered as a sphere.
     * </p>
     * 
     * @param ellipsoid
     *        Ellipsoid geometric representation
     * @param bodyFrame
     *        Body frame related to the ellipsoid
     * @param name
     *        Name of the ellipsoid
     */
    public AbstractEllipsoidBodyShape(final IEllipsoid ellipsoid,
            final CelestialBodyFrame bodyFrame,
            final String name) {
        this(ellipsoid, bodyFrame, false, name);
    }

    /**
     * Constructor for the ellipsoid.
     * <p>
     * By default, the {@link #DEFAULT_LLH_COORD_SYSTEM} is used.
     * </p>
     * 
     * @param ellipsoid
     *        Ellipsoid geometric representation
     * @param bodyFrame
     *        Body frame related to the ellipsoid
     * @param isSpherical
     *        Indicate if the ellipsoid can be considered as a sphere or not
     * @param name
     *        Name of the ellipsoid
     */
    public AbstractEllipsoidBodyShape(final IEllipsoid ellipsoid,
            final CelestialBodyFrame bodyFrame,
            final boolean isSpherical,
            final String name) {
        this(ellipsoid, bodyFrame, isSpherical, name, DEFAULT_LLH_COORD_SYSTEM);
    }

    /**
     * Constructor for the ellipsoid.
     * 
     * @param ellipsoid
     *        Ellipsoid geometric representation
     * @param bodyFrame
     *        Body frame related to the ellipsoid
     * @param spherical
     *        Indicate if the ellipsoid can be considered as a sphere or not
     * @param name
     *        Name of the ellipsoid
     * @param lLHCoordinatesSystem
     *        Ellipsoid coordinates system
     */
    public AbstractEllipsoidBodyShape(final IEllipsoid ellipsoid,
            final CelestialBodyFrame bodyFrame,
            final boolean spherical,
            final String name,
            final LLHCoordinatesSystem lLHCoordinatesSystem) {
        super(name, bodyFrame);

        // Initialization
        this.ellipsoid = ellipsoid;
        this.spherical = spherical;
        this.lLHCoordinatesSystem = lLHCoordinatesSystem;
    }

    /** {@inheritDoc} */
    @Override
    public double getARadius() {
        return this.ellipsoid.getSemiA();
    }

    /** {@inheritDoc} */
    @Override
    public double getBRadius() {
        return this.ellipsoid.getSemiB();
    }

    /** {@inheritDoc} */
    @Override
    public double getCRadius() {
        return this.ellipsoid.getSemiC();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSpherical() {
        return this.spherical;
    }

    /** {@inheritDoc} */
    @Override
    public double getEncompassingSphereRadius() {
        return MathLib.max(this.getARadius(), MathLib.max(this.getBRadius(), this.getCRadius()));
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDefaultLLHCoordinatesSystem() {
        return this.lLHCoordinatesSystem == DEFAULT_LLH_COORD_SYSTEM;
    }

    /** {@inheritDoc} */
    @Override
    public EllipsoidPoint getIntersectionPoint(final Line line, final Vector3D close, final Frame frame,
                                               final AbsoluteDate date) throws PatriusException {
        return getIntersectionPoint(line, close, frame, date, BodyPointName.INTERSECTION);
    }

    /** {@inheritDoc} */
    @Override
    public EllipsoidPoint getIntersectionPoint(final Line line, final Vector3D close, final Frame frame,
                                               final AbsoluteDate date, final String name) throws PatriusException {

        // Express line in body frame
        final Line lineBodyFrame = frameTransformLine(line, frame, date);

        // get the intersection points
        final Vector3D[] points = this.ellipsoid.getIntersectionPoints(lineBodyFrame);

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
            if (lineBodyFrame.getAbscissa(point) > lineBodyFrame.getMinAbscissa()) {
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
        return new EllipsoidPoint(this, vectToConsider, true, name);
    }

    /** {@inheritDoc} */
    @Override
    public EllipsoidPoint[] getIntersectionPoints(final Line line, final Frame frame, final AbsoluteDate date)
        throws PatriusException {

        // Express line in body frame
        final Line lineBodyFrame = frameTransformLine(line, frame, date);

        final Vector3D[] points = this.ellipsoid.getIntersectionPoints(lineBodyFrame);

        // define the list containing the intersection points on the right side of the line
        final List<Vector3D> pointsList = new ArrayList<>();
        // compute the number of intersection points on the right side of the line
        for (final Vector3D point : points) {
            // check if the intersection point is on the right side of the line by comparing its abscissa with the
            // minimum abscissa
            if (lineBodyFrame.getAbscissa(point) > lineBodyFrame.getMinAbscissa()) {
                pointsList.add(point);
            }
        }

        // define the array of transformed points with the correct length
        final EllipsoidPoint[] transformedPoints = new EllipsoidPoint[pointsList.size()];
        // loop on the list of valid intersection points
        for (int i = 0; i < pointsList.size(); i++) {
            // fill in the array of valid transformed points
            transformedPoints[i] = buildPoint(pointsList.get(i), BodyPointName.INTERSECTION);
        }

        return transformedPoints;
    }

    /** {@inheritDoc} */
    @Override
    public double distanceTo(final Line line, final Frame frame, final AbsoluteDate date) throws PatriusException {
        // frame transformation of the line
        final Line newLine = frameTransformLine(line, frame, date);

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
    private Line frameTransformLine(final Line line, final Frame frame, final AbsoluteDate date)
        throws PatriusException {
        // frame transformation
        final Transform trans = frame.getTransformTo(getBodyFrame(), date);

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
    public IEllipsoid getEllipsoid() {
        return this.ellipsoid;
    }

    /** {@inheritDoc} */
    @Override
    public EllipsoidPoint[] closestPointTo(final Line line, final Frame frame, final AbsoluteDate date)
        throws PatriusException {
        // Express line in body frame
        final Line lineBodyFrame = frameTransformLine(line, frame, date);
        return closestPointTo(lineBodyFrame);
    }

    /** {@inheritDoc} */
    @Override
    public EllipsoidPoint[] closestPointTo(final Line line) {

        // Compute points thanks to the method of the ellipsoid
        final Vector3D[] closestPoints = this.ellipsoid.closestPointTo(line);

        // Return closest points as EllipsoidPoints
        final EllipsoidPoint closestOnShape = new EllipsoidPoint(this, closestPoints[1], true,
            BodyPointName.CLOSEST_ON_SHAPE);
        final EllipsoidPoint closestOnLine = new EllipsoidPoint(this, closestPoints[0], false,
            BodyPointName.CLOSEST_ON_LINE);
        closestOnLine.setClosestPointOnShape(closestOnShape);

        return new EllipsoidPoint[] { closestOnLine, closestOnShape };
    }

    /** {@inheritDoc} */
    @Override
    public EllipsoidPoint closestPointTo(final Vector3D point, final Frame frame, final AbsoluteDate date)
        throws PatriusException {
        // Convert point to body frame
        final Transform t = frame.getTransformTo(getBodyFrame(), date);
        final Vector3D pointInBodyFrame = t.transformPosition(point);

        return closestPointTo(pointInBodyFrame);
    }

    /** {@inheritDoc} */
    @Override
    public EllipsoidPoint closestPointTo(final Vector3D point) {
        return closestPointTo(point, BodyPointName.CLOSEST_ON_SHAPE);
    }

    /** {@inheritDoc} */
    @Override
    public EllipsoidPoint closestPointTo(final Vector3D point, final String name) {
        return new EllipsoidPoint(this, this.ellipsoid.closestPointTo(point), true, name);
    }

    /** {@inheritDoc} */
    @Override
    public EllipsoidPoint buildPoint(final LLHCoordinatesSystem coordSystem, final double latitude,
                                     final double longitude, final double height, final String name) {
        return new EllipsoidPoint(this, coordSystem, latitude, longitude, height, name);
    }

    /** {@inheritDoc} */
    @Override
    public EllipsoidPoint buildPoint(final Vector3D position, final String name) {
        return new EllipsoidPoint(this, position, name);

    }

    /** {@inheritDoc} */
    @Override
    public EllipsoidPoint buildPoint(final Vector3D position, final Frame frame, final AbsoluteDate date,
                                     final String name)
        throws PatriusException {
        // frame transformation
        final Transform trans = frame.getTransformTo(getBodyFrame(), date);
        return buildPoint(trans.transformPosition(position), name);
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D computePositionFromEllipsodeticCoordinates(final double latitude, final double longitude,
                                                               final double height) {

        // Compute the targeted normal to ellipsoid, and the two vectors in the tangent plan
        final Vector3D normalEll = new SphericalCoordinates(latitude, longitude, 1.0).getCartesianCoordinates();
        Vector3D v1Ell = Vector3D.PLUS_K.crossProduct(normalEll);
        if (v1Ell.getNorm() < Precision.DOUBLE_COMPARISON_EPSILON) {
            v1Ell = Vector3D.PLUS_I;
        }
        final Vector3D v2Ell = normalEll.crossProduct(v1Ell);

        // Apply deformation (ellipsoid to normalized spherical) on the two tangent vectors
        final Vector3D v1Sph = toNormalizedSpherical(v1Ell);
        final Vector3D v2Sph = toNormalizedSpherical(v2Ell);

        // Compute normal direction in spherical environment, which is also the position on the sphere
        final Vector3D normalSph = v1Sph.crossProduct(v2Sph);
        final Vector3D posSph = normalSph.normalize();

        // Apply deformation (normalized spherical to ellipsoid) of this position
        final Vector3D posEll = toEllipsoidal(posSph);

        // Add the height, along the targeted normal
        return posEll.add(normalEll.scalarMultiply(height));
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * Warning: When used with earth ellipsoid, the computation of sight axis local elevation on earth uses a rapid
     * algorithm with limited accuracy : typically 1.3e-4 degrees (worst case, satellite altitude 250 Km, latitude 45
     * deg., East sight direction) if sight axis is 25 degrees above horizon, and 1.4e-6 degrees (worst case) if sight
     * axis is 0,25 degrees above horizon.
     * </p>
     */
    @Override
    public double getApparentRadius(final PVCoordinatesProvider pvObserver, final AbsoluteDate date,
                                    final PVCoordinatesProvider occultedBody,
                                    final PropagationDelayType propagationDelayType)
        throws PatriusException {

        // Quick escape if the ellipsoid can be considered as a sphere)
        if (this.spherical) {
            return getARadius();
        }

        // Get Ellipsoid frame and radius
        final Frame frame = getBodyFrame();
        final double oA = getARadius();
        final double oB = getARadius();
        final double oC = getCRadius();

        // Initialization (case of instantaneous propagation)
        AbsoluteDate occultingReceptionDate = date;
        Frame frameAtOccultingDate = frame;
        AbsoluteDate emissionDate = date;

        // Case of light speed propagation (dedicated in order to optimize computation times)
        if (propagationDelayType.equals(PropagationDelayType.LIGHT_SPEED)) {
            // Native frames
            final Frame nativeFrameObserver = pvObserver.getNativeFrame(date);
            final Frame nativeFrameOccultingBody = getNativeFrame(date);
            final Frame nativeFrameOccultedBody = occultedBody.getNativeFrame(date);

            // Position of emitter is at date of emission taking into account light speed if required
            // Signal propagation is performed in closest (in term of distance on frames tree) inertial frame
            final Frame inertialFrame = nativeFrameObserver
                .getFirstCommonPseudoInertialAncestor(nativeFrameOccultedBody);
            emissionDate = VacuumSignalPropagationModel.getSignalEmissionDate(occultedBody, pvObserver, date,
                getEpsilonSignalPropagation(), propagationDelayType, inertialFrame, this.maxIterSignalPropagation);

            // Occulting body data (defined by its frame) are computed when signal from occulted body is received
            // This frame is then frozen in order to perform computations since this occulting body is only defined
            // by its frame
            final Frame occultingInertialFrame = nativeFrameOccultingBody
                .getFirstCommonPseudoInertialAncestor(nativeFrameOccultedBody);
            occultingReceptionDate = VacuumSignalPropagationModel.getSignalReceptionDate(this, occultedBody,
                emissionDate, getEpsilonSignalPropagation(), propagationDelayType, occultingInertialFrame,
                this.maxIterSignalPropagation);
            frameAtOccultingDate = frame.getFrozenFrame(FramesFactory.getICRF(), occultingReceptionDate,
                frame.getName() + "-Frozen");
        }

        // Positions of satellite and occulted body in ellipsoid frame
        final Vector3D pted = occultedBody.getPVCoordinates(emissionDate, frameAtOccultingDate).getPosition();
        final Vector3D psat = pvObserver.getPVCoordinates(date, frameAtOccultingDate).getPosition();

        // Scaled positions wrt unit sphere
        final Vector3D ptedScaled = toNormalizedSpherical(pted);
        final Vector3D psatScaled = toNormalizedSpherical(psat);

        // Transformations to and from unit sphere with
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
            { sphereX.getY(), sphereY.getY(), sphereZ.getY() }, { sphereX.getZ(), sphereY.getZ(), sphereZ.getZ() } };
        final Matrix3D standardBasisTransform = new Matrix3D(matrixData);
        final Matrix3D localBasisTransform = standardBasisTransform.transpose();

        // Get H - point on horizon of unit sphere
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

        // Get back to standard basis
        final Vector3D hScaled = standardBasisTransform.multiply(hLocal);

        // Unscale
        final Vector3D h = toEllipsoidal(hScaled);
        final double maxRadius = MathLib.max(oA, MathLib.max(oB, oC));
        final double minRadius = MathLib.min(oA, MathLib.min(oB, oC));
        return MathLib.min(MathLib.max(
            psat.getNorm() * MathLib.sin(Vector3D.angle(h.subtract(psat).normalize(), psat.negate().normalize())),
            minRadius), maxRadius);
    }

    /**
     * Convert a given vector expressed in ellipsoidal coordinates to normalized spherical coordinates.
     * 
     * @param vIn
     *        Vector to convert
     * @return the vector in normalized spherical coordinates
     */
    private Vector3D toNormalizedSpherical(final Vector3D vIn) {
        return new Vector3D(MathLib.divide(vIn.getX(), getARadius()),
            MathLib.divide(vIn.getY(), getBRadius()),
            MathLib.divide(vIn.getZ(), getCRadius()));
    }

    /**
     * Convert a given vector expressed in normalized spherical coordinates to ellipsoidal coordinates.
     * 
     * @param vIn
     *        Vector to convert
     * @return the vector in ellipsoidal coordinates
     */
    private Vector3D toEllipsoidal(final Vector3D vIn) {
        return new Vector3D(vIn.getX() * getARadius(), vIn.getY() * getBRadius(), vIn.getZ() * getCRadius());
    }

    /**
     * Setter for the Newton algorithm threshold used to compute distance to the ellipsoid using method
     * {@link #distanceTo(Line, Frame, AbsoluteDate)}. Method {@link #distanceTo(Line, Frame, AbsoluteDate)} is used in
     * particular in SensorModel. Default value for this threshold is 1E-11.
     *
     * @param newThreshold
     *        new threshold to set
     */
    public void setConvergenceThreshold(final double newThreshold) {
        if (this.ellipsoid instanceof Ellipsoid) {
            ((Ellipsoid) this.ellipsoid).setNewtonThreshold(newThreshold);
        }
    }

    /**
     * Get the maximum number of iterations for signal propagation when signal
     * propagation is taken into account.
     * 
     * @return returns the maximum number of iterations for signal propagation
     *         when signal propagation is taken into account.
     */
    public int getMaxIterSignalPropagation() {
        return this.maxIterSignalPropagation;
    }

    /**
     * Set the maximum number of iterations for signal propagation when signal propagation is taken into account.
     * 
     * @param maxIterSignalPropagationIn maximum number of iterations for signal propagation
     */
    public void setMaxIterSignalPropagation(final int maxIterSignalPropagationIn) {
        this.maxIterSignalPropagation = maxIterSignalPropagationIn;
    }
}
