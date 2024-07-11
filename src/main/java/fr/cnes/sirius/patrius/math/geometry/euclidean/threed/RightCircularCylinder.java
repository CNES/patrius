/**
 * 
 * Copyright 2011-2022 CNES
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
 * @history creation 17/10/2011
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:345:30/10/2014:modified comments ratio
 * VERSION::DM:834:28/03/2017:add object Vehicule
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::FA:1796:03/10/2018:Correction vehicle class
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

import java.io.Serializable;
import java.util.Locale;

import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.utils.UtilsPatrius;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * <p>
 * This is a describing class for a 3D right circular cylinder ended by two planes normal to its axis, with some
 * algorithm to compute intersections and distances to some other objects.
 * </p>
 * 
 * @useSample <p>
 *            Creation with a radius, a height and two Vector3D : Vector3D origin = new Vector3D(1.0, 6.0, -2.0);
 *            Vector3D direction = new Vector3D(6.0, -3.0, -1.0); double radius = 2.0; double length = 5.0;
 *            RightCircularCylinder cylinder = new RightCircularCylinder(origin, direction, radius, height); Creation
 *            with a line, a height and a radius : RightCircularCylinder cylinder = new RightCircularCylinder(line,
 *            radius, height); Intersection with a line : boolean intersects = cylinder(line);
 *            </p>
 * 
 * @concurrency immutable
 * 
 * @see Shape#distanceTo(Line)
 * @see Shape#intersects(Line)
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: RightCircularCylinder.java 17583 2017-05-10 13:05:10Z bignon $
 * 
 * @since 1.0
 * 
 */
public final class RightCircularCylinder implements CrossSectionProvider, Cylinder, Serializable {

    /** Serializable version identifier. */
    private static final long serialVersionUID = 4760299772648614616L;

    /** Vectors comparisons epsilon */
    private static final double VECTORSCOMPARISONEPS = UtilsPatrius.GEOMETRY_EPSILON;

    /** height of the cylinder on its axis */
    private final double height;

    /** The associated infinite cylinder */
    private final InfiniteRightCircularCylinder infiniteCylinder;

    /** the up side disk */
    private final Disk upDisk;

    /** the bottom side disk */
    private final Disk bottomDisk;

    /** Position of the origin of the axis */
    private final Vector3D origin;

    /** Direction of the axis, defines the orientation : Z of the local frame */
    private final Vector3D direction;

    /** Radius of the cylinder */
    private final double radius;

    /**
     * Build a right circular cylinder from its radius and its axis as a line
     * 
     * @param inDirection the direction of the axis of the cylinder
     * @param inRadius the radius of the cylinder
     * @param inLength the height of the cylinder on its axis
     * @exception IllegalArgumentException if the radius or the height is negative or null, or if
     *            the direction vector has a null norm.
     * */
    public RightCircularCylinder(final Line inDirection, final double inRadius,
        final double inLength) {
        this(inDirection.getOrigin(), inDirection.getDirection(), inRadius, inLength);
    }

    /**
     * Build a right circular cylinder from its radius and the origin and direction of its axis
     * 
     * @param inOrigin the origin of the axis
     * @param inDirection the direction of the axis
     * @param inRadius the radius of the cylinder
     * @param inHeight the height of the cylinder on its axis
     * @exception IllegalArgumentException if the radius or the height is negative or null, or if
     *            the direction vector has a null norm.
     * */
    public RightCircularCylinder(final Vector3D inOrigin, final Vector3D inDirection,
        final double inRadius, final double inHeight) {

        final String message = PatriusMessages.ARGUMENT_OUTSIDE_DOMAIN.getLocalizedString(Locale
            .getDefault());

        // test of the height
        if (inHeight < 0.0 || Precision.equals(inHeight, 0.0)) {
            throw new IllegalArgumentException(message);
        }

        // Initialisations
        this.infiniteCylinder = new InfiniteRightCircularCylinder(inOrigin, inDirection, inRadius);
        this.height = inHeight;
        this.direction = this.infiniteCylinder.getDirection();
        this.radius = inRadius;
        this.origin = new Vector3D(1.0, inOrigin);

        // disks creation

        this.upDisk = new Disk(this.origin.add(this.height / 2.0, this.direction), this.direction, this.radius);
        this.bottomDisk = new Disk(this.origin.add(-this.height / 2.0, this.direction), this.direction, this.radius);
    }

    /**
     * @return the origin
     */
    public Vector3D getOrigin() {
        return this.origin;
    }

    /**
     * @return the direction
     */
    public Vector3D getDirection() {
        return this.direction;
    }

    /**
     * @return the height
     */
    public double getLength() {
        return this.height;
    }

    /**
     * @return the radius
     */
    public double getRadius() {
        return this.radius;
    }

    /**
     * Returns the surface of the cylinder base.
     * 
     * @return the surface of the cylinder base
     */
    public double getBaseSurface() {
        return FastMath.PI * this.radius * this.radius;
    }

    /**
     * Get transversal surface.
     * 
     * @return the transversal surface
     */
    public double getTransversalSurf() {
        return RightCircularSurfaceCylinder.getTSurfaceFromRadiusAndLength(this.radius, this.height);
    }

    /** {@inheritDoc} */
    @Override
    public boolean intersects(final Line line) {

        // pre-computation of the intersection points with the infinite cylinder
        final Vector3D[] infiniteCylinderInterPoints = this.infiniteCylinder.getIntersectionPoints(line);

        // computation of the intersection with the finite cylinder : search of intersection
        final boolean intersectsCylinder = this.intersectionsWithCylinder(infiniteCylinderInterPoints).length > 0;

        // search of intersections on the disks
        // A line can't intersect the bottom disk but not something else, so it is
        // not necessary to be tested
        final boolean intersectsDisk = this.upDisk.intersects(line);

        // one intersection is needed for the return to be true
        return intersectsDisk || intersectsCylinder;

    }

    /**
     * Computes the intersection points with the finite cylinder amongst the ones with the infinite
     * cylinder
     * 
     * @param infiniteCylinderInterPoints the intersection points with the infinite cylinder
     * @return the intersection points
     */
    private Vector3D[] intersectionsWithCylinder(final Vector3D[] infiniteCylinderInterPoints) {

        // Initialisations
        final int nbPoints = infiniteCylinderInterPoints.length;
        final double[] originDbl = { this.origin.getX(), this.origin.getY(), this.origin.getZ() };
        final double[] dirDbl = { this.direction.getX(), this.direction.getY(), this.direction.getZ() };
        Vector3D[] points = new Vector3D[0];
        Vector3D[] temp;
        int nbPointsOK = 0;

        // search of intersections on the finite cylinder
        for (int i = 0; i < nbPoints; i++) {

            // Initialisation of the current intersection point in doubles
            final double[] currentPoint = { infiniteCylinderInterPoints[i].getX(),
                infiniteCylinderInterPoints[i].getY(), infiniteCylinderInterPoints[i].getZ() };

            // computation of the third coordinate (on the axis)
            // of this point on the local frame of the cylinder
            final double projOnTheAxis = (currentPoint[0] - originDbl[0]) * dirDbl[0]
                + (currentPoint[1] - originDbl[1]) * dirDbl[1]
                + (currentPoint[2] - originDbl[2]) * dirDbl[2];

            // the point is on the finite cylinder...
            if ((projOnTheAxis < this.height / 2.0) && (projOnTheAxis > -this.height / 2.0)) {

                // ..it is added to the array of results
                nbPointsOK++;
                temp = points;
                points = new Vector3D[nbPointsOK];
                System.arraycopy(temp, 0, points, 0, nbPointsOK - 1);
                points[nbPointsOK - 1] = infiniteCylinderInterPoints[i];
            }
        }

        return points;

    }

    /** {@inheritDoc} */
    @Override
    public Vector3D[] getIntersectionPoints(final Line line) {

        // pre-computation of the intersection points with the infinite cylinder
        final Vector3D[] infiniteCylinderInterPoints = this.infiniteCylinder.getIntersectionPoints(line);

        // Test of the intersections on the finite cylinder
        final Vector3D[] intersectionsWithCyl = this.intersectionsWithCylinder(infiniteCylinderInterPoints);
        final int nbIntWithCyl = intersectionsWithCyl.length;

        // search of intersections on the disks
        final Vector3D[] intersectionsWithDisk1 = this.upDisk.getIntersectionPoints(line);
        final int nbIntWithDisk1 = intersectionsWithDisk1.length;

        final Vector3D[] intersectionsWithDisk2 = this.bottomDisk.getIntersectionPoints(line);
        final int nbIntWithDisk2 = intersectionsWithDisk2.length;

        // Initialisation of the result array
        final Vector3D[] intersections = new Vector3D[nbIntWithCyl + nbIntWithDisk1
            + nbIntWithDisk2];

        // concatenation of the found points
        System.arraycopy(intersectionsWithCyl, 0, intersections, 0, nbIntWithCyl);
        if (nbIntWithDisk1 > 0) {
            intersections[nbIntWithCyl] = intersectionsWithDisk1[0];
        }
        if (nbIntWithDisk2 > 0) {
            intersections[nbIntWithCyl + nbIntWithDisk1] = intersectionsWithDisk2[0];
        }

        return intersections;
    }

    /** {@inheritDoc} */
    @Override
    public double distanceTo(final Line line) {

        // Initialisations
        final double distance;
        final Vector3D directionLine = line.getDirection();

        // cross product between the two normed directions
        Vector3D normal = Vector3D.crossProduct(this.direction, directionLine);

        // if the line is parallel to the axis
        if (normal.getNorm() < VECTORSCOMPARISONEPS) {

            // computation of the distance to the axis
            final double axisToLineDist = line.distance(this.origin);

            // the distance is 0.0 if the line passes through the cylinder,
            // positive other wise
            distance = MathLib.max(axisToLineDist - this.radius, 0.0);

        } else {

            // The line is not parallel to the axis
            final Vector3D originLine = line.getOrigin();

            // Vector from the origin of the cylinder to the origin of the line
            final Vector3D origToOrig = originLine.subtract(this.origin);

            // projection of this vector on the normed normal vector
            normal = normal.normalize();
            final double projectionOnNormal = Vector3D.dotProduct(origToOrig, normal);

            // creation of the shortest vector from the axis to the line
            normal = normal.scalarMultiply(projectionOnNormal);
            final double axisToLineDist = MathLib.abs(projectionOnNormal);

            // computation of the points "origin" and "origin + direction"
            // translated into the plane parallel to the separating middle plane
            // and containing the input line.
            final Vector3D translatedOrigin = this.origin.add(normal);
            final Vector3D translatedOrigDir = this.origin.add(this.direction).add(normal);

            // Computation of the distances of these points to the input line
            final double distOrigin = line.distance(translatedOrigin);
            final double distOrigDir = line.distance(translatedOrigDir);

            // Thales theorem : computation of the point of the axis
            // witch is the closest to the input line
            // distOrigin - distOrigDir can't be null because the lines aren't parallel
            final double alpha = distOrigin / (distOrigin - distOrigDir);

            if (alpha < this.height / 2.0 && alpha > -this.height / 2.0) {
                // the closest point is on the finite cylinder
                distance = MathLib.max(axisToLineDist - this.radius, 0.0);
            } else {
                // the closest point is out of the finite cylinder,
                // so the shortest distance is from the edge of a disk
                distance = MathLib.min(this.upDisk.distanceTo(line), this.bottomDisk.distanceTo(line));
            }
        }

        return distance;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D[] closestPointTo(final Line line) {
        Vector3D[] points = new Vector3D[2];

        if (this.intersects(line)) {
            points[0] = this.getIntersectionPoints(line)[0];
            points[1] = points[0];
        } else {
            // point of the infinite cylinder that is the closest to the line
            final Vector3D[] infCylClosestPointToLine = this.infiniteCylinder.closestPointTo(line);

            // Z coordinate of this point in the local frame computation
            final double projOnAxis = Vector3D.dotProduct(infCylClosestPointToLine[0], this.direction);

            // test of the Z coordinate
            if (projOnAxis < -this.height / 2.0) {
                // if the coordinate is lower than H/2, the point is searched on the
                // bottom ending disk
                points = this.bottomDisk.closestPointTo(line);
            } else if (projOnAxis > this.height / 2.0) {
                // if the coordinate is greater than H/2, the point is searched on the
                // up ending disk
                points = this.upDisk.closestPointTo(line);
            } else {
                // if it belongs to the finite cone
                points = infCylClosestPointToLine;
            }
        }

        return points;
    }

    /**
     * Get a representation for this right circular cylinder. The given parameters are in the same
     * order as in the constructor.
     * 
     * @return a representation for this right circular cylinder
     */
    @Override
    public String toString() {
        final StringBuilder res = new StringBuilder();
        final String fullClassName = this.getClass().getName();
        final String shortClassName = fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
        final String open = "{";
        final String close = "}";
        final String comma = ",";
        res.append(shortClassName).append(open);
        // "Origin":
        res.append("Origin");
        res.append(this.origin.toString());
        res.append(comma);
        // "Direction":
        res.append("Direction");
        res.append(this.direction.toString());
        res.append(comma);
        // "Radius":
        res.append("Radius").append(open);
        res.append(this.radius).append(close);
        res.append(comma);
        // "Height":
        res.append("Height").append(open);
        res.append(this.height).append(close);
        res.append(close);

        return res.toString();
    }

    /** {@inheritDoc} */
    @Override
    public double getCrossSection(final Vector3D crossDirection) {

        // The cylinder is seen from the direction with angle alpha from the main cylinder axis
        // Compute cosAlpha as the dot product of the direction and the cylinder main axis
        final double cosAlpha = MathLib.abs(Vector3D.dotProduct(crossDirection.normalize(),
            this.direction));

        // Compute sinAlpha
        final double sinAlpha = MathLib.sqrt(1. - cosAlpha * cosAlpha);

        // Compute the cross section :
        // the surface seen from input direction is equal to S1 cosAlpha + S2 sinAlpha where :
        // S1 is the axial seen area (rectangle of area 2rh)
        // S2 is the above or bottom seen area ( circle of area PI * r * r)
        return 2. * this.radius * this.height * sinAlpha + FastMath.PI * this.radius * this.radius
            * cosAlpha;
    }

    /**
     * Get equivalent transversal surface. This surface is used in order to modelize the cylinder as
     * a parallelepiped.
     * 
     * @return the transveral surface of the equivalent parallelepiped
     */
    public double getEquivalentTransversalSurf() {
        return (MathLib.sqrt(2.) + 2.) / 4. * this.getTransversalSurf();
    }

}
