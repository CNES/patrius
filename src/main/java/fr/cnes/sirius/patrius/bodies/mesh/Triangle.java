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

    /** Serial UID. */
    private static final long serialVersionUID = -4963154552975036743L;

    /** Absolute epsilon for intersection calculation. */
    private static final double EPSILON = 1E-14;

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
    
    /**
     * Constructor.
     * @param id triangle ID
     * @param v1 first vertex
     * @param v2 second vertex
     * @param v3 third vertex
     */
    public Triangle(final int id,
            final Vertex v1,
            final Vertex v2,
            final Vertex v3) {

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

        // Edges
        final Vector3D v12 = v2.getPosition().subtract(v1.getPosition());
        final Vector3D v13 = v3.getPosition().subtract(v1.getPosition());
        final Vector3D v23 = v3.getPosition().subtract(v2.getPosition());

        // Normal direction to the triangle
        final Vector3D n = crossProduct(v12, v13).normalize();
        // Sign chosen to get the normal directed towards outer space
        if (dotProduct(n, this.vertices[0].getPosition()) > 0) {
            this.normal = n;
        } else {
            this.normal = n.scalarMultiply(-1);
        }

        // Surface
        final double v12Norm = v12.getNorm();
        final double v13Norm = v13.getNorm();
        final double v23Norm = v23.getNorm();
        final double tmp = 0.5 * (v12Norm + v13Norm + v23Norm);
        this.surface = MathLib.sqrt((tmp * (tmp - v12Norm) * (tmp - v13Norm) * (tmp - v23Norm)));

        // Initialize neighbors
        // Filled-in once all triangles have been initialized
        this.neighbors = new ArrayList<Triangle>();
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

        // Initial computations (edges are not stored in triangle on purpose)
        final Vector3D edge1 = vertices[1].getPosition().subtract(vertices[0].getPosition());
        final Vector3D edge2 = vertices[2].getPosition().subtract(vertices[0].getPosition());
        final Vector3D pvect = crossProduct(line.getDirection(), edge2);

        // Narrower and narrower checks
        final double det = dotProduct(edge1, pvect);
        if (MathLib.abs(det) < EPSILON) {
            // Line is in triangle plane
            // No intersection point
            return null;
        }
        final double invDet = 1. / det;
        final Vector3D tvect = line.getOrigin().subtract(vertices[0].getPosition());
        final double u = dotProduct(tvect, pvect) * invDet;
        if (u < 0 || u > 1) {
            // Out of triangle plane and without intersection
            return null;
        }
        final Vector3D qvect = crossProduct(tvect, edge1);
        final double v = dotProduct(line.getDirection(), qvect) * invDet;
        if (v < 0 || u + v > 1) {
            // No intersection
            return null;
        }

        // Intersection
        final double t = dotProduct(edge2, qvect) * invDet;
        return line.pointAt(t);
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
    private final Vector3D crossProduct(final Vector3D v1,
            final Vector3D v2) {
        return new Vector3D(v1.getY() * v2.getZ() - v1.getZ() * v2.getY(), v1.getZ() * v2.getX() - v1.getX()
                * v2.getZ(), v1.getX() * v2.getY() - v1.getY() * v2.getX());
    }

    /**
     * The triangle vertices.
     * @return the triangle vertices
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public Vertex[] getVertices() {
        return vertices;
    }

    /**
     * Returns the triangle barycenter.
     * @return the triangle barycenter
     */
    public Vector3D getCenter() {
        return center;
    }

    /**
     * Returns the normal to the triangle.
     * @return the normal to the triangle
     */
    public Vector3D getNormal() {
        return normal;
    }

    /**
     * Returns the triangle surface.
     * @return the triangle surface
     */
    public double getSurface() {
        return surface;
    }

    /**
     * Returns the triangle neighbors (i.e. which have a side in common).
     * @return the triangle neighbors
     */
    public List<Triangle> getNeighbors() {
        return neighbors;
    }

    /**
     * Add a triangle neighbor (package method only to be used at initialization of {@link FacetCelestialBody}).
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
            if (vertices[i].getID() == triangle.vertices[0].getID()
                    || vertices[i].getID() == triangle.vertices[1].getID()
                    || vertices[i].getID() == triangle.vertices[2].getID()) {
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
        return dotProduct(normal, position.subtract(center)) > 0;
    }
    
    /**
     * Computes minimal distance between provided line and triangle <b>provided that line does not cross triangle</b>.
     * <p>
     * This method is package-protected and is not supposed to be used by user. It assumes line does not cross triangle
     * </p>
     * @param line line
     * @return minimal distance between provided line and triangle
     */
    protected double distanceTo(final Line line) {
        // Minimum distance lies on a segment
        // Compute distance to each segment
        final double dist1 = distanceToSegment(line, new Segment(vertices[0].getPosition(), vertices[1].getPosition(),
                null));
        final double dist2 = distanceToSegment(line, new Segment(vertices[1].getPosition(), vertices[2].getPosition(),
                null));
        final double dist3 = distanceToSegment(line, new Segment(vertices[2].getPosition(), vertices[0].getPosition(),
                null));
        return MathLib.min(dist1, MathLib.min(dist2, dist3));
    }
    
    /**
     * Computes distance from line to segment.
     * @param line a line
     * @param segment a segment
     * @return distance from line to segment
     */
    private double distanceToSegment(final Line line, final Segment segment) {
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
        
        // Compute distance
        return line.distance(closestPoint);
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
        return handled;
    }
}
