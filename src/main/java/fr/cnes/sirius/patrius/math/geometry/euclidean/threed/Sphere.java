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
 * @history creation 04/10/2011
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:345:30/10/2014:modified comments ratio
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::FA:1970:03/01/2019:quality corrections
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
 * This is a describing class for a 3D spherical shape, with some algorithm to compute intersections and distances to
 * some other objects.
 * </p>
 * 
 * @useSample <p>
 *            Creation with a double and a Vector3D : Vector3D center = new Vector3D(1.0, 6.0, -2.0); Sphere sphere =
 *            new Sphere(center, 2.0); Intersection with a line : boolean intersects = sphere.intersects(line);
 *            </p>
 * 
 * @concurrency immutable
 * 
 * @see Shape#distanceTo(Line)
 * @see Shape#intersects(Line)
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: Sphere.java 17583 2017-05-10 13:05:10Z bignon $
 * 
 * @since 1.0
 * 
 */
public final class Sphere implements IEllipsoid, CrossSectionProvider, Serializable {

    /** Serializable version identifier. */
    private static final long serialVersionUID = -7540625758165659580L;

    /** Double comparison */
    private static final double EPS = UtilsPatrius.DOUBLE_COMPARISON_EPSILON;

    /** Position of the center of the sphere */
    private final Vector3D center;

    /** Radius of the sphere */
    private final double radius;

    /** Position of the center of the sphere (doubles) */
    private final double[] centerTab;

    /**
     * Build a sphere from its radius and the position of its center
     * 
     * @param inCenter the center of the sphere
     * @param inRadius the radius of the sphere
     * @exception IllegalArgumentException if the radius is negative
     * */
    public Sphere(final Vector3D inCenter, final double inRadius) {

        final String message = PatriusMessages.ARGUMENT_OUTSIDE_DOMAIN.getLocalizedString(Locale
            .getDefault());

        // test of the radius
        if (inRadius < 0.0 || Precision.equals(inRadius, 0.0)) {
            throw new IllegalArgumentException(message);
        }

        // build of the object
        this.center = new Vector3D(1.0, inCenter);
        this.centerTab = new double[3];
        this.centerTab[0] = inCenter.getX();
        this.centerTab[1] = inCenter.getY();
        this.centerTab[2] = inCenter.getZ();
        this.radius = inRadius;

    }

    /**
     * Build a sphere centered in [0, 0, 0] from its projected surface.
     * 
     * @param inSurface the sphere transversal surface
     * */
    public Sphere(final double inSurface) {
        this.center = Vector3D.ZERO;
        this.centerTab = new double[] { 0., 0., 0. };
        this.radius = MathLib.sqrt(inSurface / FastMath.PI);
    }

    /**
     * @return the radius
     */
    public double getRadius() {
        return this.radius;
    }

    /**
     * @return the center
     */
    @Override
    public Vector3D getCenter() {
        return this.center;
    }

    /** {@inheritDoc} */
    @Override
    public boolean intersects(final Line line) {
        // the line intersects the sphere if the distance is not strictly positive
        // the "<=" operator is used for performances issues
        return this.absoluteDistanceTo(line) <= 0.0;
    }

    /** {@inheritDoc} */
    @Override
    public double distanceTo(final Line line) {
        return MathLib.max(this.absoluteDistanceTo(line), 0.0);
    }

    /**
     * Computes the distance between the line and the center of the sphere minus the radius.
     * 
     * @param line the Line
     * @return the absolute distance
     */
    private double absoluteDistanceTo(final Line line) {
        final Vector3D origVect = line.getOrigin();
        final Vector3D dirVect = line.getDirection().normalize();
        final double[] origin = { origVect.getX(), origVect.getY(), origVect.getZ() };
        final double[] direction = { dirVect.getX(), dirVect.getY(), dirVect.getZ() };

        // Dot product between vector origin of the line - center of the sphere
        // and the vector direction of the line (normalised).
        final double dotProduct = (this.centerTab[0] - origin[0]) * direction[0]
            + (this.centerTab[1] - origin[1]) * direction[1] + (this.centerTab[2] - origin[2])
            * direction[2];

        // Computation of the vector between the point of the line closest to
        // the sphere and the center.
        final double[] closestPointToCenter = {
            origin[0] + dotProduct * direction[0] - this.centerTab[0],
            origin[1] + dotProduct * direction[1] - this.centerTab[1],
            origin[2] + dotProduct * direction[2] - this.centerTab[2] };

        // The return is the norm of this vector minus the radius
        final double distance = MathLib.sqrt(closestPointToCenter[0] * closestPointToCenter[0]
            + closestPointToCenter[1] * closestPointToCenter[1] + closestPointToCenter[2]
            * closestPointToCenter[2]);

        return distance - this.radius;
    }

    /**
     * Computes the distance to a point of space.
     * 
     * @param point the point
     * @return the shortest distance between the surface of the sphere and the point
     */
    public double distanceTo(final Vector3D point) {
        // computation of the vector between the point and the center
        final Vector3D toRadius = point.subtract(this.center);

        return toRadius.getNorm() - this.radius;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D[] getIntersectionPoints(final Line line) {
        final Vector3D[] intersections;
        final double absoluteDistance = this.absoluteDistanceTo(line);

        // There is a solution only if the line isn't parallel to
        // the plane.
        if (absoluteDistance <= 0.0) {
            Vector3D originLine = line.getOrigin();
            final Vector3D directionLine = line.getDirection().normalize();
            Vector3D originToCenterVect = this.center.subtract(originLine);
            double originToCenter = originToCenterVect.getNorm();

            if (MathLib.abs(originToCenter - (absoluteDistance + this.radius)) < 1.) {
                // Special case : the origin of the line is too close
                // to the center of the sphere. This leads to imprecise
                // computation of bDist later.
                // A solution is to "move" the origin of the line
                // and recompute accordingly
                originLine = line.getOrigin().add(5., directionLine);
                originToCenterVect = this.center.subtract(originLine);
                originToCenter = originToCenterVect.getNorm();
            }

            // computation of the sign of the direction to go from
            // the origin of the line to the point between the two intersection points
            // (the point itself if only one solution)
            final double directionSide = Vector3D.dotProduct(originToCenterVect, directionLine);
            final double directionSign;
            if (MathLib.abs(directionSide) < EPS) {
                directionSign = 1.0;
            } else {
                directionSign = MathLib.signum(directionSide);
            }

            // If the line is tangent to the sphere, there is one intersection point
            if (MathLib.abs(absoluteDistance) < EPS) {
                intersections = new Vector3D[1];

                // computation of the position of the intersections relative to the
                // origin of the line
                final double alpha = MathLib.sqrt(originToCenter * originToCenter - this.radius
                    * this.radius);
                intersections[0] = originLine
                    .add(new Vector3D(directionSign * alpha, directionLine));
            } else {
                // If the line passes through the sphere, there are two intersection points
                intersections = new Vector3D[2];

                // A few geometric considerations...
                final double aDist = MathLib.sqrt(this.radius * this.radius - (absoluteDistance + this.radius)
                    * (absoluteDistance + this.radius));
                final double bDist = MathLib.sqrt(originToCenter * originToCenter
                    - (absoluteDistance + this.radius) * (absoluteDistance + this.radius));

                // computation of the position of the intersections relative to the
                // origin of the line
                final double alpha1 = bDist - aDist;
                final double alpha2 = bDist + aDist;
                intersections[0] = originLine.add(new Vector3D(directionSign * alpha1,
                    directionLine));
                intersections[1] = originLine.add(new Vector3D(directionSign * alpha2,
                    directionLine));
            }

        } else {
            intersections = new Vector3D[0];
        }

        return intersections;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D[] closestPointTo(final Line line) {
        final Vector3D[] points = new Vector3D[2];

        // if no intersection, computation of the two points
        if (this.intersects(line)) {
            // if an intersection is found, the points are identical
            points[1] = this.getIntersectionPoints(line)[0];
            points[0] = points[1];
        } else {
            final Vector3D origLine = line.getOrigin();
            final Vector3D dirLine = line.getDirection().normalize();

            // Dot product between vector origin of the line - center of the sphere
            // and the vector direction of the line (normalised).
            final double dotProduct = Vector3D.dotProduct(this.center.subtract(origLine), dirLine);

            // Computation of the point of the line closest to the sphere
            points[0] = origLine.add(dotProduct, dirLine);

            // vector from this point to the center
            final Vector3D closestPointToCenter = this.center.subtract(points[0]);

            // The length of this vector is corrected to touch the surface of the sphere.
            final double distance = closestPointToCenter.getNorm() - this.radius;

            // The sphere's closest point to the line is the then the vector sum.
            points[1] = points[0].add(distance, closestPointToCenter.normalize());
        }

        return points;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D closestPointTo(final Vector3D point) {
        final Vector3D centerToPointDir = point.subtract(this.center).normalize();

        return this.center.add(centerToPointDir.scalarMultiply(this.radius));
    }

    /**
     * Computes the normal vector to the surface
     * 
     * @param point Point as a Vector3D
     * @return the normal vector in local basis
     */
    @Override
    public Vector3D getNormal(final Vector3D point) {
        return point.subtract(this.center).normalize();
    }

    /**
     * Get a representation for this sphere. The given parameters are in the same order as in the
     * constructor.
     * 
     * @return a representation for this sphere
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
        // "Center":
        res.append("Center").append(this.center.toString()).append(comma);
        // "Radius":
        res.append("Radius").append(open).append(this.radius).append(close).append(close);

        return res.toString();
    }

    /** {@inheritDoc} */
    @Override
    public double getCrossSection(final Vector3D direction) {
        return FastMath.PI * this.radius * this.radius;
    }

    /** {@inheritDoc} */
    @Override
    public double getSemiA() {
        return this.radius;
    }

    /** {@inheritDoc} */
    @Override
    public double getSemiB() {
        return this.radius;
    }

    /** {@inheritDoc} */
    @Override
    public double getSemiC() {
        return this.radius;
    }

    /**
     * Get surface
     * 
     * @return the sphere surface
     */
    public double getSurface() {
        return getSurfaceFromRadius(this.radius);
    }

    /**
     * Get surface from radius value.
     * 
     * @param radius radius
     * @return surface
     */
    public static double getSurfaceFromRadius(final double radius) {
        return FastMath.PI * radius * radius;
    }

    /**
     * Get radius from surface value.
     * 
     * @param surface surface
     * @return radius
     */
    public static double getRadiusFromSurface(final double surface) {
        double radius = 0.0;
        if (surface > 0.0) {
            // (This is to avoid sqrt(negative)->NaN for the case surface=-eps)
            radius = MathLib.sqrt(surface / FastMath.PI);
        }
        return radius;
    }
}
