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
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11.1:FA:FA-60:30/06/2023:[PATRIUS] Erreur dans les méthodes getNeighbors de FacetBodyShape
 * VERSION:4.11.1:FA:FA-50:30/06/2023:[PATRIUS] Calcul d'intersections sur un FacetBodyShape
 * VERSION:4.11:DM:DM-3288:22/05/2023:[PATRIUS] ID de facette pour un FacetBodyShape
 * VERSION:4.11:DM:DM-3259:22/05/2023:[PATRIUS] Creer une interface StarConvexBodyShape
 * VERSION:4.11:FA:FA-3314:22/05/2023:[PATRIUS] Anomalie evaluation ForceModel SpacecraftState en ITRF
 * VERSION:4.11:DM:DM-3297:22/05/2023:[PATRIUS] Optimisation calculs distance entre Line et FacetBodyShape 
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3127:10/05/2022:[PATRIUS] Ajout de deux methodes resize a l'interface GeometricBodyShape ...
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.6:DM:DM-2544:27/01/2021:Ajouter la definition d'un corps celeste a partir d'un modele de forme 
 * VERSION:4.6:DM:DM-2528:27/01/2021:[PATRIUS] Integration du modele DTM 
 * VERSION:4.6:DM:DM-2591:27/01/2021:[PATRIUS] Intigration et validation JOptimizer
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies.mesh;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Plane;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Segment;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * 3D triangle definition. A triangle contains 3 3D-points or "vertices" (defined by {@link Vertex} class).
 * This class also stores data related to the triangle for efficient computation (center, surface, normal vector,
 * neighboring {@link Triangle}).
 * 
 * @author Emmanuel Bignon
 *
 * @since 4.6
 */
public class Triangle implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = -4963154552975036743L;

    /** Absolute epsilon for intersection calculation. */
    private static final double EPSILON = 1E-14;

    /** Multiplicative factor for increased precision. */
    private static final double MULTIPLICATIVE_FACTOR = 1E14;

    /** Triangle identifier. */
    private final int id;

    /** List of vertices defining the triangle. */
    private final Vertex[] vertices;

    /** Triangle center in body frame. */
    private final Vector3D center;

    /** Normal direction (outside) in body frame. */
    private final Vector3D normal;

    /** Triangle surface. */
    private final double surface;

    /** Direct neighbors (having a sides in common). */
    private final List<Triangle> neighbors;

    /** Boolean representing triangle status for fast algorithms. This boolean can be set alternatively to
     * true/false. */
    private boolean handled;
    
    /** Encompassing sphere radius squared. */
    private final double sphereRadius;
    
    /**
     * Constructor.
     * 
     * @param idIn the triangle identifier
     * @param v1 first vertex
     * @param v2 second vertex
     * @param v3 third vertex
     */
    public Triangle(final int idIn, final Vertex v1, final Vertex v2, final Vertex v3) {

        this.id = idIn;

        this.vertices = new Vertex[] { v1, v2, v3 };

        // Add neighbors to vertices
        v1.addNeighbors(this);
        v2.addNeighbors(this);
        v3.addNeighbors(this);

        // Triangle center position
        final double coef = 1. / 3.;
        final double x = coef * (v1.getPosition().getX() + v2.getPosition().getX() + v3.getPosition().getX());
        final double y = coef * (v1.getPosition().getY() + v2.getPosition().getY() + v3.getPosition().getY());
        final double z = coef * (v1.getPosition().getZ() + v2.getPosition().getZ() + v3.getPosition().getZ());
        this.center = new Vector3D(x, y, z);
        
        // Compute radius square of encompassing sphere
        double sphereRadius2 = 0;
        sphereRadius2 = MathLib.max(sphereRadius2,
            this.getVertices()[0].getPosition().distanceSq(this.center));
        sphereRadius2 = MathLib.max(sphereRadius2,
            this.getVertices()[1].getPosition().distanceSq(this.center));
        sphereRadius2 = MathLib.max(sphereRadius2,
            this.getVertices()[2].getPosition().distanceSq(this.center));
        this.sphereRadius = MathLib.sqrt(sphereRadius2);

        // Edges
        final Vector3D v12 = v2.getPosition().subtract(v1.getPosition());
        final Vector3D v13 = v3.getPosition().subtract(v1.getPosition());
        final Vector3D v23 = v3.getPosition().subtract(v2.getPosition());

        // Normal direction to the triangle
        this.normal = crossProduct(v12, v13).normalize();

        // Surface
        final double v12Norm = v12.getNorm();
        final double v13Norm = v13.getNorm();
        final double v23Norm = v23.getNorm();
        final double tmp = 0.5 * (v12Norm + v13Norm + v23Norm);
        this.surface = MathLib.sqrt((tmp * (tmp - v12Norm) * (tmp - v13Norm) * (tmp - v23Norm)));

        // Initialize neighbors
        // Filled-in once all triangles have been initialized
        this.neighbors = new ArrayList<>();
        this.handled = false;
    }

    /**
     * Returns intersection point with triangle, null if there is no intersection or if line is included in triangle.
     * <p>
     * Algorithm from article "Fast, Minimum Storage Ray/Triangle Intersection" from Thomas Moller, 1997.
     * </p>
     * 
     * @param line
     *        line of sight (considered infinite)
     * @return intersection point with triangle, null if there is no intersection
     */
    public Vector3D getIntersection(final Line line) {

        // Quick check if out of encompassing sphere
        final Vector3D d = this.center.subtract(line.getOrigin());
        final Vector3D n = new Vector3D(1.0, d, -dotProduct(d, line.getDirection()), line.getDirection());
        if (n.getNormSq() > this.sphereRadius * this.sphereRadius) {
            return null;
        }

        // Initial computations (edges are not stored in triangle on purpose)
        final Vector3D edge1 = this.vertices[1].getPosition().subtract(this.vertices[0].getPosition());
        final Vector3D edge2 = this.vertices[2].getPosition().subtract(this.vertices[0].getPosition());
        final Vector3D pvect = crossProduct(line.getDirection(), edge2);

        // Narrower and narrower checks
        final double det = dotProduct(edge1, pvect);
        if (MathLib.abs(det) < EPSILON) {
            // Line is in triangle plane
            // No intersection point
            return null;
        }
        final double invDet = 1. / det;
        final Vector3D tvect = line.getOrigin().subtract(this.vertices[0].getPosition());
        final double u = dotProduct(tvect, pvect) * invDet;
        if (u < -EPSILON || u > 1 + EPSILON) {
            // Out of triangle plane and without intersection
            return null;
        }
        final Vector3D qvect = crossProduct(tvect, edge1);
        final double v = dotProduct(line.getDirection(), qvect) * invDet;
        if (v < -EPSILON || u + v > 1 + EPSILON) {
            // No intersection
            return null;
        }

        // Intersection
        final double t = dotProduct(edge2, qvect) * invDet;
        return line.pointAt(t);
    }
    
    /**
     * <p>
     * Computes the points of the triangle and the line realizing the shortest distance.
     * </p>
     * <p>
     * If the line intersects the triangle, the returned points are identical. Semi-finite lines are handled by this
     * method.<br>
     * </p>
     * 
     * @param line
     *        the line
     *        
     * @return the two points : first the one from the line, and the one from the shape.
     */
    public Vector3D[] closestPointTo(final Line line) {

        // Check intersection case (regardless abscissas)
        final Vector3D intersection = this.getIntersection(line);

        // The line intersects when considered infinite, check if semi-finite line intersects
        boolean intersects = false;
        if (intersection != null && line.getAbscissa(intersection) >= line.getMinAbscissa()) {
            intersects = true;
        }

        final Vector3D[] points = new Vector3D[2];
        if (intersects) {
            // Line (infinite or not) intersects: return intersection point
            points[0] = intersection;
            points[1] = intersection;
        } else {
            // Line does not intersect the triangle or is included in its plane

            // Check if line is semi-finite and the minimum absicssa's point projection is in the triangle
            final Vector3D minAbsPoint = line.pointAt(line.getMinAbscissa());
            if (line.getMinAbscissa() != Double.NEGATIVE_INFINITY && pointInTriangle(minAbsPoint)) {
                // Triangle's plane
                final Plane plane = new Plane(vertices[0].getPosition(), vertices[1].getPosition(),
                    vertices[2].getPosition());

                // Return the minimum abscissa point and its projection on the triangle
                return new Vector3D[] { minAbsPoint, plane.toSpace(plane.toSubSpace(minAbsPoint)) };
            }

            // Triangle's closest point is necessarily on one edge if this line of code is reached
            // Candidate triangle points: closest point of each edge
            final Vector3D[] candidatePoints = new Vector3D[] {
                closestSegmentPoint(line, new Segment(vertices[0].getPosition(), vertices[1].getPosition(), null)),
                closestSegmentPoint(line, new Segment(vertices[1].getPosition(), vertices[2].getPosition(), null)),
                closestSegmentPoint(line, new Segment(vertices[2].getPosition(), vertices[0].getPosition(), null)),
            };

            // Compute distance for each candidate point and store the index of the closest one
            int indexMin = 0;
            double distance = Double.POSITIVE_INFINITY;
            for (int index = 0; index < 3; index++) {
                final double distToSegment = line.distance(candidatePoints[index]);
                if (distToSegment < distance) {
                    distance = distToSegment;
                    indexMin = index;
                }
            }

            // Closest point of the triangle
            points[1] = candidatePoints[indexMin];
            // Closest point of the line
            final double maxAbscissa = MathLib.max(line.getMinAbscissa(), line.getAbscissa(points[1]));
            points[0] = line.pointAt(maxAbscissa);

        }

        return points;
    }
    
    /**
     * Check if the projection on triangle's plane of a point of space belongs to the triangle.
     * 
     * @param point
     *        a point of space
     * 
     * @return true if the point's projection belongs to the triangle, false otherwise
     */
    public final boolean pointInTriangle(final Vector3D point) {

        // Vertices
        final Vector3D vertex0 = vertices[0].getPosition();
        final Vector3D vertex1 = vertices[1].getPosition();
        final Vector3D vertex2 = vertices[2].getPosition();

        // Edges
        final Vector3D edge01 = vertex1.subtract(vertex0);
        final Vector3D edge12 = vertex2.subtract(vertex1);
        final Vector3D edge20 = vertex0.subtract(vertex2);

        // Compute triangle's signed normal
        final Vector3D signedNormal = edge01.crossProduct(edge12);
        
        // Check for each edge if the point is on the correct side of the edge, i.e. towards triangle's center
        final boolean sideOk01 = dotProduct(signedNormal, crossProduct(edge01, point.subtract(vertex0))) >= 0;
        final boolean sideOk12 = dotProduct(signedNormal, crossProduct(edge12, point.subtract(vertex1))) >= 0;
        final boolean sideOk20 = dotProduct(signedNormal, crossProduct(edge20, point.subtract(vertex2))) >= 0;

        // Projection lies within the triangle if the point is on the correct side of each edge
        return sideOk01 && sideOk12 && sideOk20;
    }

    /**
     * Fast dot product of two 3D vectors.
     * @param v1 first vector
     * @param v2 second vector
     * @return dot product of two 3D vectors
     */
    public static final double dotProduct(final Vector3D v1,
            final Vector3D v2) {
        return v1.getX() * v2.getX() + v1.getY() * v2.getY() + v1.getZ() * v2.getZ();
    }

    /**
     * Fast cross product of two 3D vectors.
     * @param v1 first vector
     * @param v2 second vector
     * @return cross product of two 3D vectors
     */
    private static final Vector3D crossProduct(final Vector3D v1, final Vector3D v2) {
        return new Vector3D(v1.getY() * v2.getZ() - v1.getZ() * v2.getY(), v1.getZ() * v2.getX() - v1.getX()
                * v2.getZ(), v1.getX() * v2.getY() - v1.getY() * v2.getX());
    }

    /**
     * The triangle identifier.
     * 
     * @return the triangle identifier
     */
    public int getID() {
        return this.id;
    }

    /**
     * The triangle vertices.
     * 
     * @return the triangle vertices
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public Vertex[] getVertices() {
        return this.vertices;
    }

    /**
     * Returns the triangle barycenter.
     * @return the triangle barycenter
     */
    public Vector3D getCenter() {
        return this.center;
    }

    /**
     * Returns the normal to the triangle.
     * @return the normal to the triangle
     */
    public Vector3D getNormal() {
        return this.normal;
    }

    /**
     * Returns the triangle surface.
     * @return the triangle surface
     */
    public double getSurface() {
        return this.surface;
    }

    /**
     * Returns the triangle neighbors (i.e. which have a side in common).
     * @return the triangle neighbors
     */
    public List<Triangle> getNeighbors() {
        return this.neighbors;
    }
    
    /**
     * Returns the encompassing sphere radius squared.
     * 
     * @return the encompassing sphere radius squared
     */
    public double getSphereRadius() {
        return this.sphereRadius;
    }

    /**
     * Add a triangle neighbor (package method only to be used at initialization of {@link FacetBodyShape}).
     * <p>
     * Warning: no check is performed in order to verify is the provided triangle is really a neighbor.
     * </p>
     * @param neighbor the triangle neighbor to set
     */
    protected void addNeighbors(final Triangle neighbor) {
        this.neighbors.add(neighbor);
    }

    /**
     * Returns true if provided triangle is a neighbor by checking their vertices ID (i.e. has 2 identical vertex ID).
     * @param triangle a triangle
     * @return true if provided triangle is a neighbor by checking their vertices ID (i.e. has 2 identical vertex ID)
     */
    public boolean isNeighborByVertexID(final Triangle triangle) {
        int numberSameVertex = 0;
        for (int i = 0; i < 3; i++) {
            if (this.vertices[i].getID() == triangle.vertices[0].getID()
                    || this.vertices[i].getID() == triangle.vertices[1].getID()
                    || this.vertices[i].getID() == triangle.vertices[2].getID()) {
                numberSameVertex++;
            }
        }
        return numberSameVertex == 2;
    }

    /**
     * Returns true if the triangle is visible from the provided position (culling test).
     * @param position position
     * @return true if the triangle is visible
     */
    public boolean isVisible(final Vector3D position) {
        return dotProduct(this.normal, position.subtract(this.center)) > 0;
    }
    
    /**
     * Computes minimal distance between provided line and triangle <b>provided that the line does not cross this
     * triangle</b>.
     * <p>
     * This method is package-protected and is not supposed to be used by user. It assumes that the line does not cross
     * this triangle.
     * </p>
     * 
     * @param line line
     * @return minimal distance between the provided line and this triangle
     */
    protected double distanceTo(final Line line) {
        // Minimum distance lies on a segment
        final Vector3D[] points = this.closestPointTo(line);
        
        // Compute distance between the two points
        return points[0].distance(points[1]);
    }
    
    /**
     * Computes minimal distance between provided point and triangle <b>provided that the line parallel to the
     * normal of this triangle and passing by the given point does not cross this triangle</b>.
     * 
     * <p>
     * This method is package-protected and is not supposed to be used by user. It assumes that the line parallel to the
     * normal of this triangle and passing by the given point does not cross the triangle.
     * </p>
     * 
     * @param point point
     * @return minimal distance between the provided point and this triangle
     */
    protected double distanceTo(final Vector3D point) {
        // Define the distance to be computed
        final double distance;
        // Check if the given point belongs to this triangle
        // Create the line parallel to the normal of this triangle and passing by the given point
        final Line line = Line.createLine(point, this.normal.scalarMultiply(MULTIPLICATIVE_FACTOR));
        // Compute the distance between this triangle and the line parallel to the normal of this triangle and
        // passing by the given point
        final Vector3D[] closestLineTrianglePoints = this.closestPointTo(line);
        // Compute the distance between the given point and the line point closest to the the line parallel to
        // the normal of this triangle and passing by the given point
        distance = point.distance(closestLineTrianglePoints[0]);

        // Return the computed distance
        return distance;
    }

    /**
     * Computes the segment point that is the closest to a line.
     * 
     * @param line a line
     * @param segment a segment
     * @return segment's closest point to the line
     */
    private static Vector3D closestSegmentPoint(final Line line, final Segment segment) {
        final Line line1 = new Line(segment.getStart(), segment.getEnd());
        Vector3D closestPoint = line1.closestPoint(line);
        final double abcissa = line1.getAbscissa(closestPoint);
        final double abcissa1 = line1.getAbscissa(segment.getStart());
        final double abcissa2 = line1.getAbscissa(segment.getEnd());

        // Clamp closest point to segment boundaries if necessary
        if (abcissa < abcissa1) {
            closestPoint = segment.getStart();
        } else if (abcissa > abcissa2) {
            closestPoint = segment.getEnd();
        }
        
        // Return closest point
        return closestPoint;
    }
    
    /**
     * Set a boolean representing triangle status for fast algorithms. This boolean can be set alternatively to
     * true/false.
     * @param handled status to set
     */
    public void setHandled(final boolean handled) {
        this.handled = handled;
    }
    
    /**
     * Returns a boolean representing triangle status for fast algorithms. This boolean can be set alternatively to
     * true/false.
     * @return triangle status
     */
    public boolean isHandled() {
        return this.handled;
    }
}
