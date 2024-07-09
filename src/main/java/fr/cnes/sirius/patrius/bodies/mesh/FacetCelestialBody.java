/**
 * Copyright 2011-2020 CNES
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
* VERSION:4.8:FA:FA-2956:15/11/2021:[PATRIUS] Temps de propagation non implemente pour certains evenements 
* VERSION:4.8:DM:DM-2995:15/11/2021:[PATRIUS] Disposer de methodes publiques pour le statut de masquage et de visibilite
* VERSION:4.8:DM:DM-2958:15/11/2021:[PATRIUS] calcul d'intersection a altitude non nulle pour l'interface BodyShape 
* VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
* VERSION:4.7:DM:DM-2870:18/05/2021:Complements pour la manipulation des coordonnees cart.
* VERSION:4.7:DM:DM-2767:18/05/2021:Evolutions et corrections diverses 
 * VERSION:4.6:DM:DM-2544:27/01/2021:Ajouter la definition d'un corps celeste a partir d'un modele de forme 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies.mesh;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import fr.cnes.sirius.patrius.assembly.models.SensorModel;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.ExtendedOneAxisEllipsoid;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.bodies.GeometricBodyShape;
import fr.cnes.sirius.patrius.bodies.IAUPole;
import fr.cnes.sirius.patrius.bodies.UserCelestialBody;
import fr.cnes.sirius.patrius.fieldsofview.IFieldOfView;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.analysis.MultivariateFunction;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.optim.InitialGuess;
import fr.cnes.sirius.patrius.math.optim.MaxEval;
import fr.cnes.sirius.patrius.math.optim.PointValuePair;
import fr.cnes.sirius.patrius.math.optim.SimpleBounds;
import fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.GoalType;
import fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.MultivariateOptimizer;
import fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.ObjectiveFunction;
import fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.noderiv.PowellOptimizer;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.EclipseDetector;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Celestial body defined by a list of facets. A facet is a 3D triangle defined in the body frame.
 * <p>
 * This class offers an optimal storage under a Binary Space Partition Tree (BSP Tree). Each of the body facet (class
 * {@link Triangle}) is linked to its neighbors. Each of the body vertex (class {@link Vertex}) is also linked to its
 * neighboring triangles {@link Triangle}. Hence this class provides very efficient methods (O(log n)) for intersection
 * computation, neighbors computation, etc.
 * </p>
 * <p>
 * This class inherits interfaces {@link CelestialBody} and {@link GeometricBodyShape}:
 * <ul>
 * <li>As a {@link GeometricBodyShape}, this class can be used in conjunction with {@link EclipseDetector} and
 * {@link SensorModel}.</li>
 * <li>As a {@link CelestialBody}, this class can be used with any {@link CelestialBody}-related class (attitude laws,
 * etc.).</li>
 * </ul>
 * </p>
 *
 * @author Emmanuel Bignon
 *
 * @since 4.6
 */
public class FacetCelestialBody extends UserCelestialBody implements GeometricBodyShape {

    /** Serial UID. */
    private static final long serialVersionUID = -7564873573665379652L;

    /** Epsilon (squared) for distance comparison. */
    private static final double EPSILON2 = 1E-12;

    /** Epsilon for reference ellipsoid computation. */
    private static final double EPS_OPT = 1E-8;

    /** Maximum number of criterion evaluation for reference ellipsoid computation. */
    private static final int MAX_EVAL = 1000;
    
    /** Mesh of {@link Triangle} stored under a list of {@link Triangle}. */
    private final Triangle[] triangles;

    /** Mesh of {@link Triangle} stored under a Binary Space Partition Tree. */
    private final TrianglesSet tree;

    /** Reference ellipsoid which is ellipsoid (a, f) which minimize distance to all vertices. */
    private final ExtendedOneAxisEllipsoid referenceEllipsoid;

    /** Inner reference sphere which is largest sphere strictly contained in mesh and centered around (0, 0, 0). */
    private final ExtendedOneAxisEllipsoid innerSphere;

    /** Distance from center to closest vertex to center. */
    private double minNorm;
    
    /** Distance from center to farthest vertex to center. */
    private double maxNorm;
    
    /**
     * Constructor.
     *
     * @param name
     *        body name
     * @param pvBody
     *        Body PV coordinates
     * @param gm
     *        body gravitational parameter
     * @param iauPole
     *        IAU pole model
     * @param meshLoader
     *        mesh loader
     * @throws PatriusException thrown if loading failed
     */
    public FacetCelestialBody(final String name,
            final PVCoordinatesProvider pvBody,
            final double gm,
            final IAUPole iauPole,
            final MeshProvider meshLoader) throws PatriusException {
        super(name, pvBody, gm, iauPole);

        this.triangles = meshLoader.getTriangles();

        // Compute reference ellipsoid and inner sphere
        this.referenceEllipsoid = buildReferenceEllipsoid(meshLoader.getVertices());
        this.innerSphere = buildInnerSphere(meshLoader.getVertices());
        
        // Build BSP tree
        tree = new TrianglesSet(triangles);

        // Link triangles to each other
        linkTriangles();
    }

    /**
     * Link triangles to each other. Triangles are linked to their neighbors.
     */
    private void linkTriangles() {
        for (final Triangle triangle : triangles) {
            for (final Vertex vertex : triangle.getVertices()) {
                for (final Triangle triangle2 : vertex.getNeighbors()) {
                    // Check if triangles are neighbors, if so update list of neighbors
                    if (triangle.isNeighborByVertexID(triangle2) && !triangle.getNeighbors().contains(triangle2)) {
                        triangle.addNeighbors(triangle2);
                        triangle2.addNeighbors(triangle);
                    }
                }
            }
        }
    }

    /**
     * Build reference ellipsoid which is ellipsoid (a, f) which minimize distance to all vertices.
     * Minimization is reached with a {@link PowellOptimizer}.
     * @param vertices list of vertices
     * @return reference ellipsoid
     */
    private ExtendedOneAxisEllipsoid buildReferenceEllipsoid(final Map<Integer, Vertex> vertices) {
        // Get min and max possible semi-major axis
        double a0max = 0;
        double a0min = Double.POSITIVE_INFINITY;
        for(final Vertex v : vertices.values()) {
            final double pos = v.getPosition().getNorm();
            a0max = MathLib.max(a0max, pos);
            a0min = MathLib.min(a0min, pos);
        }
        this.minNorm = a0min;
        this.maxNorm = a0max;

        // Precompute sin and cos of all vertices position
        final double[] cosLon = new double[vertices.size()];
        final double[] sinLon = new double[vertices.size()];
        final double[] cosLat = new double[vertices.size()];
        final double[] sinLat = new double[vertices.size()];
        int i = 0;
        for (final Vertex v : vertices.values()) {
            // Geodetic point
            final Vector3D normedPoint = v.getPosition().normalize();
            final double latitude = MathLib.asin(normedPoint.getZ());
            final double longitude = MathLib.atan2(normedPoint.getY(), normedPoint.getX());

            // cos/sin for given geodetic point
            final double[] sincosLon = MathLib.sinAndCos(longitude);
            sinLon[i] = sincosLon[0];
            cosLon[i] = sincosLon[1];
            final double[] sincosLat = MathLib.sinAndCos(latitude);
            sinLat[i] = sincosLat[0];
            cosLat[i] = sincosLat[1];
            i++;
        }
        
        // Use optimizer
        final MultivariateOptimizer optimizer = new PowellOptimizer(EPS_OPT, EPS_OPT);
        // Cost function to minimize 
        final MultivariateFunction func = new MultivariateFunction() {
            /** {@inheritDoc} */
            @Override
            public double value(final double[] point) {
                final double a = point[0];
                final double f = point[1];
                double cost = 0;
                int i = 0;
                for (final Vertex v : vertices.values()) {
                    // Theoretical point for current a and f values
                    final Vector3D vTh = new Vector3D(a * cosLat[i] * cosLon[i], a * cosLat[i] * sinLon[i], a
                            * (1.0 - f) * sinLat[i]);
                    // Distance squared: add to cost function
                    cost += vTh.distanceSq(v.getPosition());
                    i++;
                }
                return cost;
            }
        };
        // Run optimizer
        // Semi-major axis is in [a0min, a0max]
        // Flattening is in [0, 1]
        final PointValuePair res = optimizer.optimize(
                new MaxEval(MAX_EVAL),
                new ObjectiveFunction(func),
                GoalType.MINIMIZE,
                new InitialGuess(new double[] { (a0min + a0max) / 2., 0 } ),
                new SimpleBounds(new double[] { a0min, 0. },
                        new double[] { a0max, 1. }));

        // Build ellipsoid with optimum values (a, f)
        return new ExtendedOneAxisEllipsoid(res.getPoint()[0], res.getPoint()[1], getBodyFrame(), getName());
    }
    
    /**
     * Build inner sphere which is largest sphere strictly contained in mesh and centered around (0, 0, 0).
     * @param vertices list of vertices
     * @return inner sphere
     */
    private ExtendedOneAxisEllipsoid buildInnerSphere(final Map<Integer, Vertex> vertices) {
        // Get min distance to center
        double min = Double.POSITIVE_INFINITY;
        for(final Vertex v : vertices.values()) {
            min = MathLib.min(min, v.getPosition().getNorm());
        }
        
        return new ExtendedOneAxisEllipsoid(min, 0, getBodyFrame(), getName()); 
    }

    /**
     * Returns the mesh under a list of triangles. The triangles are not sorted in any particular way.
     * @return the mesh under a list of triangles
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    // Reason: performances
    public Triangle[] getTriangles() {
        return triangles;
    }

    /** {@inheritDoc} */
    @Override
    public final Frame getBodyFrame() {
        return getBodyOrientedFrame();
    }

    /** {@inheritDoc} */
    @Override
    public GeodeticPoint getIntersectionPoint(final Line line,
            final Vector3D close,
            final Frame frame,
            final AbsoluteDate date) throws PatriusException {
        final Intersection intersection = getIntersection(line, close, frame, date);
        GeodeticPoint res = null;
        if (intersection != null) {
            res = transform(intersection.getPoint(), frame, date);
        }
        return res;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D[] getIntersectionPoints(final Line line,
            final Frame frame,
            final AbsoluteDate date) throws PatriusException {

        // Convert line to body frame if necessary
        Transform t = Transform.IDENTITY;
        Line lineInBodyFrame = line;
        if (!frame.equals(getBodyFrame())) {
            t = frame.getTransformTo(getBodyFrame(), date);
            lineInBodyFrame = t.transformLine(line);
        }

        // Get list of intersection points from triangles which intersect line
        final Intersection[] intersections = tree.getIntersections(lineInBodyFrame);

        final Vector3D[] result;
        if (intersections != null) {
            // Remove duplicates
            // May happen if intersection exactly lies on some triangles edge
            final List<Vector3D> intersectionPoints = new ArrayList<Vector3D>();
            for (int i = 0; i < intersections.length; i++) {
                boolean isDuplicate = false;
                for (int j = 0; j < i; j++) {
                    if (intersections[i].getPoint().distanceSq(intersections[j].getPoint()) < EPSILON2) {
                        isDuplicate = true;
                        break;
                    }
                }
                if (!isDuplicate) {
                    intersectionPoints.add(intersections[i].getPoint());
                }
            }

            // Convert points to output frame if necessary
            result = new Vector3D[intersectionPoints.size()];
            if (!frame.equals(getBodyFrame())) {
                // Conversion
                final Transform tInv = t.getInverse();
                for (int i = 0; i < result.length; i++) {
                    result[i] = tInv.transformPosition(intersectionPoints.get(i));
                }
            } else {
                // No conversion
                for (int i = 0; i < result.length; i++) {
                    result[i] = intersectionPoints.get(i);
                }
            }
        } else {
            // No intersection case
            result = new Vector3D[0];
        }

        // Return result
        return result;
    }

    /**
     * Get the intersection point with associated triangle of a line with the surface of the body in the body frame.
     * <p>
     * A line may have several intersection points with a closed surface (we consider the one point case as a
     * degenerated two points case). The close parameter is used to select which of these points should be returned. The
     * selected point is the one that is closest to the close point.
     * </p>
     * 
     * <p>
     * Warning: does not take into account sight direction. The line is considered to be infinite.
     * </p>
     * 
     * @param line
     *        test line (may intersect the body or not)
     * @param close
     *        point used for intersections selection in frame
     * @param frame
     *        frame in which line is expressed
     * @param date
     *        date of the line in given frame
     * @return <intersection triangle, intersection point in frame> or null if the line does not intersect the surface
     * @exception PatriusException
     *            if line cannot be converted to body frame
     */
    public Intersection getIntersection(final Line line,
            final Vector3D close,
            final Frame frame,
            final AbsoluteDate date) throws PatriusException {

        // Convert line to body frame
        final Transform t = frame.getTransformTo(getBodyFrame(), date);
        final Line lineInBodyFrame = t.transformLine(line);
        final Vector3D closeInBodyFrame = t.transformPosition(close);

        // Get list of intersection points from triangles which intersect line
        final Intersection[] intersections = tree.getIntersections(lineInBodyFrame);

        // Get closest intersection point from "close" point
        Intersection res = null;
        if (intersections != null) {
            int closest = 0;
            double closestDist2 = Vector3D.distanceSq(closeInBodyFrame, intersections[0].getPoint());
            for (int i = 1; i < intersections.length; i++) {
                final double dist2 = Vector3D.distanceSq(closeInBodyFrame, intersections[i].getPoint());
                if (dist2 < closestDist2) {
                    closest = i;
                    closestDist2 = dist2;
                }
            }

            // Convert point to output frame
            final Transform tInv = t.getInverse();
            res = new Intersection(intersections[closest].getTriangle(), tInv.transformPosition(intersections[closest]
                    .getPoint()));
        }

        return res;
    }

    /** {@inheritDoc}
     * <p>
     * Warning: this method returns {@link #getIntersectionPoint(Line, Vector3D, Frame, AbsoluteDate)} if altitude is 
     * close to 0. An exception is thrown otherwise.
     * </p>
     */
    @Override
    public GeodeticPoint getIntersectionPoint(final Line line,
            final Vector3D close,
            final Frame frame,
            final AbsoluteDate date,
            final double altitude) throws PatriusException {
        if (MathLib.abs(altitude) < EPS_ALTITUDE) {
            // Altitude is considered to be 0
            return getIntersectionPoint(line, close, frame, date);
        } else {
            throw new PatriusException(PatriusMessages.UNAVAILABLE_FACETCELESTIALBODY_INTERSECTION_POINT_METHOD);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Warning: this method considers the body as the inner sphere (which is largest sphere strictly contained in the
     * mesh.
     * </p>
     */
    @Override
    public GeodeticPoint transform(final Vector3D point,
            final Frame frame,
            final AbsoluteDate date) throws PatriusException {
        // Approximation of body by inner sphere
        return innerSphere.transform(point, frame, date);
    }

    /** {@inheritDoc}
     * <p>
     * Warning: this method considers the body as the inner sphere (which is largest sphere strictly contained in the
     * mesh.
     * </p>
     */
    @Override
    public Vector3D transform(final GeodeticPoint point) {
        // Approximation of body by inner sphere
        return innerSphere.transform(point);
    }

    /**
     * Returns the altitude of body at point (latitude, longitude). The returned altitude is the altitude of the point
     * belonging to the mesh with provided latitude, longitude.
     * <p>
     * Altitude is relative to the inner sphere to the body (which is the largest sphere strictly inside the mesh).
     * </p>
     * @param latitude latitude
     * @param longitude longitude
     * @return the altitude of body at point (latitude, longitude)
     * @throws PatriusException thrown if failed to compute intersection
     */
    public double getLocalAltitude(final double latitude, final double longitude) throws PatriusException {
        final Vector3D point = transform(new GeodeticPoint(latitude, longitude, 0.));
        final Line line = new Line(point, Vector3D.ZERO);
        final Intersection intersection = getIntersection(line, point, getBodyFrame(), null);
        final double distance = intersection.getPoint().distance(point);
        return intersection.getPoint().getNormSq() > point.getNormSq() ? distance : -distance;
    }

    /**
     * Returns the altitude of body given provided direction in body frame.
     * <p>
     * Altitude is relative to the inner sphere to the body (which is the largest sphere strictly inside the mesh).
     * </p>
     * @param direction direction in body frame
     * @return altitude of body given provided direction in body frame
     * @throws PatriusException thrown if failed to compute intersection
     */
    public double getLocalAltitude(final Vector3D direction) throws PatriusException {
        final GeodeticPoint point = transform(direction, getBodyFrame(), null);
        return getLocalAltitude(point.getLatitude(), point.getLongitude());
    }

    /** {@inheritDoc}
     * <p>Warning: in case of strongly not convex bodies, this methods may not return exact minimal distance.</p>
     */
    @Override
    public double distanceTo(final Line line,
            final Frame frame,
            final AbsoluteDate date) throws PatriusException {
        // Convert line to body frame
        final Transform t = frame.getTransformTo(getBodyFrame(), date);
        final Line lineInBodyFrame = t.transformLine(line);
        // Compute distance to tree
//        double minDistance = Double.POSITIVE_INFINITY;
//        for (final Triangle triangle : triangles) {
//            final double dist = triangle.distanceTo(lineInBodyFrame);
//            minDistance = MathLib.min(minDistance, dist);
//        }
//        return minDistance;
        return tree.distanceTo(lineInBodyFrame);
    }

    /** {@inheritDoc}
     * <p>
     * Warning: this method considers the body as the closest one axis ellipsoid to the body (which is the one
     * minimizing the distance to each vertex).
     * </p>
     */
    @Override
    public double getLocalRadius(final Vector3D position,
            final Frame frame,
            final AbsoluteDate date,
            final PVCoordinatesProvider occultedBody) throws PatriusException {
        // Approximation of body by reference ellipsoid
        return referenceEllipsoid.getLocalRadius(position, frame, date, occultedBody);
    }

    /**
     * Returns the neighbors of provided triangle whose center is closer than provided distance of provided triangle
     * center.
     * <p>
     * Provided triangle is included in the list of neighbors.
     * </p>
     * <p>Beware not to confuse this method with {@link #getNeighbors(Triangle, int)}</p> 
     * @param triangle a triangle
     * @param maxDistance max distance
     * @return the neighbors of provided triangle whose center is closer than provided distance of provided triangle
     *         center
     */
    public List<Triangle> getNeighbors(final Triangle triangle,
            final double maxDistance) {
        return getNeighbors(triangle, triangle.getCenter(), maxDistance, Integer.MAX_VALUE);
    }

    /**
     * Returns the neighbors of provided triangle whose distance in terms of triangle is closer or equal to provided
     * order of "neighborhood".
     * For example:
     * <ul>
     * <li>Order 0 returns provided triangle</li>
     * <li>Order 1 returns provided triangle and the immediate neighbors of provided triangle</li>
     * <li>Order 2 returns provided triangle, the immediate neighbors of provided triangle and also their own
     * immediate neighbors</li>
     * </ul>
     * <p>Beware not to confuse this method with {@link #getNeighbors(Triangle, double)}</p> 
     * @param triangle a triangle
     * @param order order of "neighborhood"
     * @return the neighbors of provided triangle whose distance in terms of triangle is closer or equal to provided
     * order of "neighborhood"
     */
    public List<Triangle> getNeighbors(final Triangle triangle,
            final int order) {
        return getNeighbors(triangle, triangle.getCenter(), Double.POSITIVE_INFINITY, order);
    }

    /**
     * Returns the neighbors of provided geodetic point whose center is closer than provided distance.
     * @param point a geodetic point
     * @param maxDistance max distance
     * @return the neighbors of provided geodetic point whose center is closer than provided distance
     * @throws PatriusException thrown if computation failed (should not happen)
     */
    public List<Triangle> getNeighbors(final GeodeticPoint point,
            final double maxDistance) throws PatriusException {
        final Vector3D pos = transform(point);
        return getNeighbors(pos, maxDistance);
    }

    /**
     * Returns the neighbor triangles of provided geodetic point which are closer or equal to provided
     * order of "neighborhood".
     * For example:
     * <ul>
     * <li>Order 0 returns closest triangle</li>
     * <li>Order 1 returns closest triangle and the immediate neighbors of closest triangle</li>
     * <li>Order 2 returns closest triangle, the immediate neighbors of closest triangle and also
     * their own immediate neighbors</li>
     * </ul>
     * <p>Beware not to confuse this method with {@link #getNeighbors(GeodeticPoint, double)}</p> 
     * @param point a geodetic point
     * @param order order of "neighborhood"
     * @return the neighbor triangles of provided geodetic point which are closer or equal to provided
     * order of "neighborhood"
     * @throws PatriusException thrown if computation failed (should not happen)
     */
    public List<Triangle> getNeighbors(final GeodeticPoint point,
            final int order) throws PatriusException {
        final Vector3D pos = transform(point);
        return getNeighbors(pos, order);
    }

    /**
     * Returns the neighbors of provided cartesian point whose center is closer than provided distance.
     * @param pos a point in body frame
     * @param maxDistance max distance
     * @return the neighbors of provided cartesian point whose center is closer than provided distance
     * @throws PatriusException thrown if computation failed (should not happen)
     */
    public List<Triangle> getNeighbors(final Vector3D pos,
            final double maxDistance) throws PatriusException {
        // Get closest triangle, which is considered to be triangle at same latitude/longitude of provided point
        final Line line = new Line(pos, Vector3D.ZERO);
        final Intersection intersection = getIntersection(line, pos, getBodyFrame(), null);
        return getNeighbors(intersection.getTriangle(), pos, maxDistance, Integer.MAX_VALUE);
    }

    /**
     * Returns the neighbor triangles of provided cartesian point which are closer or equal to provided
     * order of "neighborhood".
     * For example:
     * <ul>
     * <li>Order 0 returns closest triangle</li>
     * <li>Order 1 returns closest triangle and the immediate neighbors of closest triangle</li>
     * <li>Order 2 returns closest triangle, the immediate neighbors of closest triangle and also
     * their own immediate neighbors</li>
     * </ul>
     * <p>Beware not to confuse this method with {@link #getNeighbors(Vector3D, double)}</p> 
     * @param pos a point in body frame
     * @param order order of "neighborhood"
     * @return the neighbor triangles of provided cartesian point which are closer or equal to provided
     * order of "neighborhood"
     * @throws PatriusException thrown if computation failed (should not happen)
     */
    public List<Triangle> getNeighbors(final Vector3D pos,
            final int order) throws PatriusException {
        // Get closest triangle, which is considered to be triangle at same latitude/longitude of provided point
        final Line line = new Line(pos, Vector3D.ZERO);
        final Intersection intersection = getIntersection(line, pos, getBodyFrame(), null);
        return getNeighbors(intersection.getTriangle(), pos, Double.POSITIVE_INFINITY, order);
    }

    /**
     * Returns the neighbors of provided triangle whose center is closer than provided distance of provided reference
     * point AND closer in terms of "neighborhood" order.
     * <p>
     * Provided triangle is included in the list of neighbors.
     * </p>
     * @param triangle a triangle
     * @param referencePoint reference point from which distance are computed
     * @param maxDistance max distance
     * @param order order of "neighborhood"
     * @return the neighbors of provided triangle whose center is closer than provided distance of provided reference
     *         point AND closer in terms of "neighborhood" order
     */
    private List<Triangle> getNeighbors(final Triangle triangle,
            final Vector3D referencePoint,
            final double maxDistance,
            final int order) {
        final double maxDistance2 = maxDistance * maxDistance;

        // Reset all triangles
        for (final Triangle t : triangles) {
            t.setHandled(false);
        }
        
        // List of remaining triangles to treat in recursive algorithm
        final List<Triangle> remainingTriangles = new ArrayList<Triangle>();
        final List<Integer> remainingOrders = new ArrayList<Integer>();
        remainingTriangles.add(triangle);
        triangle.setHandled(true);
        remainingOrders.add(0);

        // Iterative algorithm starting from neighbors of provided triangle
        final List<Triangle> res = new ArrayList<Triangle>();
        while (!remainingTriangles.isEmpty()) {
            final Triangle t = remainingTriangles.get(0);
            final int o = remainingOrders.get(0);
            remainingTriangles.remove(0);
            remainingOrders.remove(0);
            if (t.getCenter().distanceSq(referencePoint) <= maxDistance2 && o <= order) {
                res.add(t);
                // Add neighbors only if not treated yet
                // Neighbors are an order further of provided triangle
                for (final Triangle neighbor : t.getNeighbors()) {
                    if (!neighbor.isHandled()) {
                        remainingTriangles.add(neighbor);
                        remainingOrders.add(o + 1);
                        neighbor.setHandled(true);
                    }
                }
            }
        }

        return res;
    }

    /**
     * Returns the body surface pointed data as {@link SurfacePointedData} for each state in provided list.
     * @param states list of spacecraft states
     * @param lineOfSight line of sight in spacecraft frame
     * @param sun Sun body
     * @param pixelFOV aperture of field of view for one pixel 
     * @return list of {@link SurfacePointedData}, one per provided date
     * @throws PatriusException thrown if state position could not be retrieved in body frame or if intersection is null
     *         or Sun frame conversion failed
     */
    public List<SurfacePointedData> getSurfacePointedDataEphemeris(final List<SpacecraftState> states,
            final Vector3D lineOfSight,
            final PVCoordinatesProvider sun,
            final double pixelFOV) throws PatriusException {
        final List<SurfacePointedData> res = new ArrayList<SurfacePointedData>();
        for (final SpacecraftState state : states) {
            // Transform from body frame to spacecraft frame and inverse
            final Transform t = new Transform(state.getDate(), state.getAttitude(getBodyFrame()).getRotation());
            final Transform tInv = t.getInverse();
            // Line of sight in body frame
            final Vector3D pos = state.getPVCoordinates(getBodyFrame()).getPosition();
            final Vector3D dir = tInv.transformVector(lineOfSight);
            res.add(new SurfacePointedData(this, state.getDate(), pos, dir, getBodyFrame(), sun, pixelFOV));
        }
        return res;
    }

    /**
     * Returns the field data as {@link FieldData} for each state in provided list.
     * @param state spacecraft state
     * @param fieldOfView sensor field of view
     * @param lineOfSight optional parameter in order to indicate field of view main line of sight in spacecraft frame.
     *        If provided, this parameter may fasten algorithm by several order of magnitudes (O(log n) vs O(n)).
     *        Be careful, in this case, strongly not convex bodies may lead to missing some triangles.
     * @return {@link FieldData} for corresponding state
     * @throws PatriusException thrown if state position could not be retrieved in body frame
     */
    public FieldData getFieldData(final SpacecraftState state,
            final IFieldOfView fieldOfView,
            final Vector3D lineOfSight) throws PatriusException {

        // Transform from body frame to spacecraft frame and inverse
        final Transform t = new Transform(state.getDate(), state.getAttitude(getBodyFrame()).getRotation());
        final Transform tInv = t.getInverse();
        // Position in body frame
        final Vector3D pos = state.getPVCoordinates(getBodyFrame()).getPosition();

        // Check if fast algorithm can be used (if line of sight has been provided and intersect body)
        boolean useFastAlgorithm = false;
        Intersection intersection = null;
        if (lineOfSight != null) {
            // Get intersection between body and main line of sight
            final Vector3D lineOfSightBodyFrame = tInv.transformVector(lineOfSight);
            final Line line = new Line(pos, pos.add(lineOfSightBodyFrame));
            intersection = getIntersection(line, pos, getBodyFrame(), state.getDate());
            useFastAlgorithm = (intersection != null);
        }

        // List of visible triangles
        final List<Triangle> visibleTriangles = new ArrayList<Triangle>();

        if (useFastAlgorithm) {
            // Fast algorithm can be used
            visibleTriangles.addAll(getFastVisibleTriangles(intersection, fieldOfView, pos, t));
        } else {
            // Standard algorithm (loop on all triangles)
            for (final Triangle triangle : triangles) {
                // A triangle is visible if in the field of view and oriented toward the field of view
                if (isVisible(triangle, pos, fieldOfView, t)) {
                    visibleTriangles.add(triangle);
                }
            }
        }

        // Build field data
        return new FieldData(state.getDate(), visibleTriangles, this);
    }

    /**
     * Returns the list of visible triangles in the field of view using a fast iterative algorithm starting from the
     * triangle at the center of the field of view.
     * @param intersection intersection between line of sight and body
     * @param fieldOfView field of view
     * @param pos spacecraft position in body frame
     * @param t transform from body frame to spacecraft frame
     * @return list of visible triangles in the field of view
     * @throws PatriusException thrown if failed to compute masked triangles
     */
    private List<Triangle> getFastVisibleTriangles(final Intersection intersection,
            final IFieldOfView fieldOfView,
            final Vector3D pos,
            final Transform t) throws PatriusException {
        // List of remaining triangles to treat in recursive algorithm
        final List<Triangle> remainingTriangles = new ArrayList<Triangle>();
        remainingTriangles.add(intersection.getTriangle());

        // Reset all triangles
        for (final Triangle triangle : triangles) {
            triangle.setHandled(false);
        }

        // Iterative algorithm starting from neighbors of provided triangle
        intersection.getTriangle().setHandled(true);
        final List<Triangle> res = new ArrayList<Triangle>();
        int index = 0;
        while (index < remainingTriangles.size()) {
            final Triangle triangle = remainingTriangles.get(index);
            index++;
            if (isVisible(triangle, pos, fieldOfView, t)) {
                res.add(triangle);
                // Add neighbors only if not treated yet
                final List<Triangle> neighbors = triangle.getNeighbors();
                for (final Triangle neighbor : neighbors) {
                    if (!neighbor.isHandled()) {
                        remainingTriangles.add(neighbor);
                        neighbor.setHandled(true);
                    }
                }
            }
        }

        return res;
    }

    /**
     * Returns true if the triangle is visible from the field of view, i.e.:
     * <ul>
     * <li>Is oriented toward the field of view</li>
     * <li>All points of the triangle are in the field of view</li>
     * <li>Triangle not masked by another facet</li>
     * </ul> 
     * @param triangle a triangle
     * @param fieldOfView field of view
     * @param pos spacecraft position in body frame
     * @param t transform from body frame to spacecraft frame
     * @return true if the triangle is visible from the field of view
     * @throws PatriusException thrown if intersection computation failed
     */
    public boolean isVisible(final Triangle triangle,
            final Vector3D pos,
            final IFieldOfView fieldOfView,
            final Transform t) throws PatriusException {
        // Check orientation
        boolean res = triangle.isVisible(pos);
        if (res) {
            // Check all triangles vertices are in the field (if provided)
            if (fieldOfView != null) {
                for (final Vertex v : triangle.getVertices()) {
                    final Vector3D vertexDirInBodyFrame = v.getPosition().subtract(pos);
                    final Vector3D vertexDirInSensorFrame = t.transformVector(vertexDirInBodyFrame);
                    res &= fieldOfView.isInTheField(vertexDirInSensorFrame);
                }
            }

            // Check potential masking
            if (res) {
                res = !isMasked(triangle, pos);
            }
        }
        return res;
    }

    /**
     * Returns true if the triangle is masked by another triangle as seen from the provided position.
     * A triangle is masked if all its vertices are masked by at least another triangle.
     * @param triangle a triangle
     * @param pos spacecraft position in body frame
     * @return true if the triangle is masked by another triangle as seen from the provided position
     * @throws PatriusException thrown if intersection computation failed
     */
    public boolean isMasked(final Triangle triangle,
            final Vector3D pos) throws PatriusException {
        for (final Vertex v : triangle.getVertices()) {
            // Compute potential intersection point
            final Line line = new Line(pos, v.getPosition());
            final Vector3D[] intersections = getIntersectionPoints(line, getBodyFrame(), null);
            boolean res = false;
            for (int j = 0; j < intersections.length; j++) {
                // Discard current vertex which is an obvious intersection point
                if (intersections[j].distanceSq(v.getPosition()) > EPSILON2) {
                    // Vertex is hidden if intersection point is between vertex and position
                    final Vector3D v1 = v.getPosition().subtract(intersections[j]);
                    final Vector3D v2 = pos.subtract(intersections[j]);
                    res |= Triangle.dotProduct(v1, v2) < 0;
                }
            }
            if (!res) {
                // One vertex is not masked: no masking
                return false;
            }
        }
        // All vertices are masked
        return true;
    }

    /**
     * Returns true if provided position in provided frame at provided date in in eclipse or not.
     * <p>
     * Computed eclipse status is exact and takes into account full body shape. This methods cannot however be used in
     * conjunction with {@link EclipseDetector} since they use a different framework.
     * </p>
     * @param date date
     * @param position position
     * @param frame frame in which position is expressed
     * @param sun Sun body
     * @return true if provided position in provided frame at provided date in in eclipse or not
     * @throws PatriusException thrown if failed to retrieve Sun position of intersection points with body
     */
    public boolean isInEclipse(final AbsoluteDate date,
            final Vector3D position,
            final Frame frame,
            final PVCoordinatesProvider sun) throws PatriusException {
        final Vector3D sunPos = sun.getPVCoordinates(date, frame).getPosition();
        final Line line = new Line(position, sunPos);
        final Intersection intersection = getIntersection(line, position, frame, date);
        // In eclipse if intersection point with (pos, Sun) vector is not null and body is between satellite and Sun
        boolean res = false;
        if (intersection != null) {
            final Vector3D posToIntersection = position.subtract(intersection.getPoint());
            final Vector3D sunToIntersection = sunPos.subtract(intersection.getPoint());
            res = Vector3D.dotProduct(posToIntersection, sunToIntersection) < 0;
        }
        return res;
    }

    /**
     * Returns the list of triangles never visible from the satellite field of view during the whole ephemeris.
     * A visible triangle must be properly oriented, in the field of view and not masked by any other triangle.
     * @param states list of spacecraft states
     * @param fieldOfView field of view
     * @return list of {@link Triangle} never visible from the satellite field of view during the whole ephemeris
     * @throws PatriusException thrown if state position could not be retrieved in body frame
     */
    public List<Triangle> getNeverVisibleTriangles(final List<SpacecraftState> states,
            final IFieldOfView fieldOfView) throws PatriusException {
        // Initially add all triangles
        final List<Triangle> res = new ArrayList<Triangle>();
        for (final Triangle triangle : triangles) {
            res.add(triangle);
        }

        // Remove triangles not matching conditions
        for (final SpacecraftState state : states) {
            // Transform from body frame to spacecraft frame
            final Transform t = new Transform(state.getDate(), state.getAttitude(getBodyFrame()).getRotation());
            // Position in body frame
            final Vector3D pos = state.getPVCoordinates(getBodyFrame()).getPosition();
            for (int i = res.size() - 1; i >= 0; i--) {
                final Triangle triangle = res.get(i);
                if (isVisible(triangle, pos, fieldOfView, t)) {
                    res.remove(triangle);
                }
            }
        }
        
        return res;
    }

    /**
     * Returns the list of triangles never enlightened by the Sun at provided dates. An enlightened triangle must be
     * properly oriented toward the Sun and not masked by any other triangle.
     * @param dates list of dates
     * @param sun Sun body
     * @return list of {@link Triangle} never enlightened by the Sun
     * @throws PatriusException thrown if state position could not be retrieved in body frame
     */
    public List<Triangle> getNeverEnlightenedTriangles(final List<AbsoluteDate> dates,
            final PVCoordinatesProvider sun) throws PatriusException {
        // Initially add all triangles
        final List<Triangle> res = new ArrayList<Triangle>();
        for (final Triangle triangle : triangles) {
            res.add(triangle);
        }

        // Remove triangles not matching conditions
        for (final AbsoluteDate date : dates) {
            // Position in body frame
            final Vector3D pos = sun.getPVCoordinates(date, getBodyFrame()).getPosition();
            for (int i = res.size() - 1; i >= 0; i--) {
                final Triangle triangle = res.get(i);
                if (isVisible(triangle, pos, null, null)) {
                    res.remove(triangle);
                }
            }
        }

        return res;
    }

    /**
     * Returns the list of triangles enlightened (by the Sun) and visible at least once during the whole ephemeris
     * from the satellite field of view. A visible triangle must be properly oriented, in the field of view and not
     * masked by any other triangle. An enlightened triangle must be properly oriented toward the Sun and not masked by
     * any other triangle.
     * @param states list of spacecraft states
     * @param sun sun
     * @param fieldOfView field of view
     * @return list of {@link Triangle} enlightened and visible at least once from the satellite field of view
     * @throws PatriusException thrown if state position could not be retrieved in body frame
     */
    public List<Triangle> getVisibleAndEnlightenedTriangles(final List<SpacecraftState> states,
            final PVCoordinatesProvider sun,
            final IFieldOfView fieldOfView) throws PatriusException {
        // Initialization
        final List<Triangle> res = new ArrayList<Triangle>();

        // Add triangles matching conditions
        for (final SpacecraftState state : states) {
            // Transform from body frame to spacecraft frame
            final Transform t = new Transform(state.getDate(), state.getAttitude(getBodyFrame()).getRotation());
            // Position in body frame
            final Vector3D pos = state.getPVCoordinates(getBodyFrame()).getPosition();
            // Sun position in body frame
            final Vector3D sunPos = sun.getPVCoordinates(state.getDate(), getBodyFrame()).getPosition();

            for (final Triangle triangle : triangles) {
                if (!res.contains(triangle)) {
                    if (isVisible(triangle, pos, fieldOfView, t) && isVisible(triangle, sunPos, null, null)) {
                        res.add(triangle);
                    }
                }
            }
        }

        return res;
    }

    /**
     * Returns reference ellipsoid which is ellipsoid (a, f) which minimize distance to all vertices.
     * @return reference ellipsoid
     */
    public ExtendedOneAxisEllipsoid getReferenceEllipsoid() {
        return referenceEllipsoid;
    }

    /**
     * Returns Inner reference sphere which is largest sphere strictly contained in mesh and centered around (0, 0, 0).
     * @return inner sphere
     */
    public ExtendedOneAxisEllipsoid getInnerSphere() {
        return innerSphere;
    }

    /**
     * Returns the distance from center to closest vertex to center of body.
     * @return the distance from center to closest vertex to center of body
     */
    public double getMinNorm() {
        return minNorm;
    }

    /**
     * Returns the distance from center to farthest vertex to center of body.
     * @return the distance from center to farthest vertex to center of body
     */
    public double getMaxNorm() {
        return maxNorm;
    }
}
