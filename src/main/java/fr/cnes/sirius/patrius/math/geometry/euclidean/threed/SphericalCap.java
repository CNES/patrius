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
 * @history creation 06/10/11
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:345:30/10/2014:modified comments ratio
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import fr.cnes.sirius.patrius.math.exception.MathInternalError;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Implements a representation of a spherical cap solid.
 * <p>
 * This class implements the SolidShape interface, so it provides the expected : intersection with a line, distance from
 * the surface of the shape, intersection points...
 * </p>
 * 
 * @useSample <p>
 *            <code>
 *       // A half-sphere :<br>
 *       // - sphere at the origin, radius 1.<br>
 *       // - plane (x,y), the normal is the z unit vector<br>
 *       final Sphere originSphere = new Sphere(new Vector3D(0., 0., 0.) , 1.);<br>
 *       final Plane xyPlane = new Plane(new Vector3D(0., 0., 1.));<br>
 *       final SphericalCap halfSphere = new SphericalCap(originSphere, xyPlane);<br>
 * </code>
 *            </p>
 * 
 * @concurrency immutable
 * 
 * @see SolidShape
 * 
 * @author cardosop
 * 
 * @version $Id: SphericalCap.java 17583 2017-05-10 13:05:10Z bignon $
 * 
 * @since 1.0
 * 
 */
public final class SphericalCap implements SolidShape {

    /**
     * Sphere enclosing the spherical cap.
     */
    private final Sphere sphere;

    /**
     * Plane delimiting the spherical cap. This plane must intersect with the sphere by more than a single point, if not
     * the spherical cap is considered undefined. The direction of the plane is used to determine in which side the
     * spherical cap is : it is on the side of the normal vector.
     */
    private final Plane plane;

    /**
     * Disk at the sphere and plane intersection.
     */
    private final Disk capDisk;

    /**
     * Constructor of a <code>SphericalCap</code> using a {@link Sphere} and a {@link Plane}.
     * 
     * @param s
     *        the sphere enclosing the spherical cap
     * @param p
     *        plane delimiting the spherical cap.
     *        This plane must intersect with the sphere by more than a single point, if not
     *        the spherical cap is considered undefined. The direction of the plane is used to determine in which
     *        side the
     *        spherical cap is : it is on the side of the normal vector.
     * 
     * @since 1.0
     */
    public SphericalCap(final Sphere s, final Plane p) {
        this.sphere = s;
        this.plane = p;

        final String message = PatriusMessages.ARGUMENT_OUTSIDE_DOMAIN.getLocalizedString(Locale.getDefault());

        if (!this.validateSphericalCap()) {
            throw new IllegalArgumentException(message);
        }
        // The spherical cap is valid.
        // We compute the disk.
        final Vector3D centSphere = this.sphere.getCenter();
        final double sphereRadius = this.sphere.getRadius();
        // normal to the plane
        final Vector3D normPlane = this.plane.getNormal().normalize();
        // oriented distance from plane to center of the sphere
        // the spherical cap is always on the normal's side so the sign matters
        final double planeToCentSphere = this.plane.getOffset(centSphere);
        final Vector3D toCenterDisk = normPlane.scalarMultiply(-planeToCentSphere);
        final double tcdNorm = toCenterDisk.getNorm();
        // Vector representing the center of the disk
        final Vector3D centerDisk = centSphere.add(toCenterDisk);
        // Radius of the disk
        final double diskRadius = MathLib.sqrt(sphereRadius * sphereRadius - tcdNorm * tcdNorm);
        // The normal to the plane , the center , and the radius of the disk fully define
        // it.
        this.capDisk = new Disk(centerDisk, normPlane, diskRadius);
    }

    /**
     * Validates the creation parameters to see if the shape is correctly defined. The plane must cut the sphere, which
     * means the distance between the sphere and the plane must be strictly below the radius of the sphere.
     * 
     * @return true when the shape is valid
     */
    private boolean validateSphericalCap() {
        final double pDist = this.plane.distanceTo(this.sphere.getCenter());
        return (pDist < this.sphere.getRadius());
    }

    /**
     * Returns true when the line intersects the spherical cap.
     * 
     * @param line
     *        to intersect
     * @return true when the line intersects the spherical cap
     * @see Shape#intersects(Line)
     */
    @Override
    public boolean intersects(final Line line) {
        final boolean rez;
        // We first need the intersection points with the sphere
        final Vector3D[] sphereIntersectors = this.sphere.getIntersectionPoints(line);

        switch (sphereIntersectors.length) {
            case 0:
                // no intersection on the sphere - cannot intersect the spherical cap either
                rez = false;
                break;
            case 1:
                // sub case : one intersection point with the sphere
                // only one thing to know : if the point is on the "right" side of the plane
                // (or belongs to the plane).
                // Given how the spherical cap is defined, that means exactly this :
                // the plane's getOffset method returns a positive result for this point.
                final double offset = this.plane.getOffset(sphereIntersectors[0]);
                rez = (offset >= 0.);
                break;
            case 2:
                // if one of the points is on the right side of the plane
                // (or belongs to the plane), the line intersects the spherical cap
                final double offset0 = this.plane.getOffset(sphereIntersectors[0]);
                final double offset1 = this.plane.getOffset(sphereIntersectors[1]);
                rez = ((offset0 >= 0.) || (offset1 >= 0.));
                break;
            default:
                // should never happen, unless the sphere intersections method is buggy
                throw new MathInternalError();
        }

        return rez;
    }

    /**
     * Gives the distance from the line to the spherical cap.<br>
     * When the line intersects the spherical cap, the distance is 0.
     * 
     * @param line
     *        to get the distance from
     * @return the distance (>=0)
     * @see Shape#distanceTo(Line)
     */
    @Override
    public double distanceTo(final Line line) {
        double rez = -1.;
        boolean closeToDisk = false;
        final Vector3D centSphere = this.sphere.getCenter();
        final double sphereRadius = this.sphere.getRadius();
        if (this.intersects(line)) {
            // If the line intersects the spherical cap, the distance is 0.
            rez = 0;
        } else {
            if (this.sphere.intersects(line)) {
                // The line intersects the part of the sphere that isn't part
                // of the spherical cap.
                // This means the closest part of the spherical cap to the line
                // is on the disk "closing" the cap.
                closeToDisk = true;
            } else {
                // There is a single point on the sphere that is the closest
                // to the line.
                // Here we find this point.
                final Vector3D center = centSphere;
                final Vector3D origLine = line.getOrigin();
                final Vector3D dirLineNorm = line.getDirection().normalize();
                // Vector circle center minus line origin
                final Vector3D cMinusOrig = center.add(-1., origLine);
                // dot product of cMinusOrig and line direction
                final double dotP = cMinusOrig.dotProduct(dirLineNorm);
                // Vector : the point of the line closest to the sphere
                final Vector3D closestPtLine = origLine.add(dotP, dirLineNorm);
                // Vector from the center to the point of the line closest to the sphere
                final Vector3D centClos = closestPtLine.add(-1., center);
                // Vector : the point of the sphere closest to the line
                final Vector3D ourPoint = center.add(sphereRadius, centClos.normalize());
                // Is this point on the spherical cap (i.e. on the right side of the plane) ?
                if (this.plane.getOffset(ourPoint) >= 0) {
                    // Our point is on the spherical cap
                    // The distance is the distance form our point
                    // to the point of the line closest to the sphere
                    rez = centClos.getNorm() - sphereRadius;
                } else {
                    // Our point is not on the spherical cap.
                    // This means the closest part of the spherical cap to the line
                    // is on the disk "closing" the cap.
                    closeToDisk = true;
                }
            }
        }
        if (closeToDisk) {
            // The distance we want is the distance to the disk
            // closing the spherical cap.
            rez = this.capDisk.distanceTo(line);
        }
        return rez;
    }

    /**
     * Returns a list of intersection points between the line and the spherical cap.<br>
     * Only the border points are given. Since the spherical cap is convex, there will always be at most two points
     * returned by this method.<br>
     * When there are none, an empty array is returned.
     * 
     * @param line
     *        to intersect
     * @return an array of intersection points.
     * @see Shape#getIntersectionPoints(Line)
     */
    @Override
    public Vector3D[] getIntersectionPoints(final Line line) {
        Vector3D[] rez = new Vector3D[0];
        final List<Vector3D> intersectionsList = new ArrayList<>();
        if (this.intersects(line)) {
            // There are intersection points.
            // We first need the intersection points with the sphere.
            // (There cannot be intersection points without at least one
            // on the sphere)
            final Vector3D[] sphereIntersectors = this.sphere.getIntersectionPoints(line);
            // The intersection points on the sphere are intersection candidates.
            // They can be kept if they are on the right side of the plane.
            for (final Vector3D intPoint : sphereIntersectors) {
                final double offset = this.plane.getOffset(intPoint);
                if (offset >= 0) {
                    // The point is a proper intersection point
                    intersectionsList.add(intPoint);
                }
            }
            if (intersectionsList.size() < 2) {
                // The sphere intersections are less than two (in fact, just one)
                // So there may be one or two
                // intersection points on the cap's disk.
                final Vector3D[] diskInterceptors = this.capDisk.getIntersectionPoints(line);
                final Vector3D spherePoint = intersectionsList.get(0);
                for (final Vector3D intPoint : diskInterceptors) {
                    // any point on the disk is a proper intersection point
                    // IF not the same from the sphere point we may already have!
                    final double spDist = spherePoint.distance(intPoint);
                    if (!Precision.equals(spDist, 0.)) {
                        // The newfound point is not close from an existing one
                        intersectionsList.add(intPoint);
                    }
                }
            }
        }
        // Copy intersectionsList to rez
        if (!intersectionsList.isEmpty()) {
            rez = intersectionsList.toArray(new Vector3D[intersectionsList.size()]);
        }
        return rez;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D[] closestPointTo(final Line line) {
        Vector3D[] points = new Vector3D[2];
        boolean closeToDisk = false;
        final Vector3D centSphere = this.sphere.getCenter();
        final double sphereRadius = this.sphere.getRadius();

        // if the line does'nt intersect the cap
        if (this.intersects(line)) {
            // if an intersection is found the points are identical

            // Intersections
            final Vector3D[] iscs = this.getIntersectionPoints(line);

            // Return intersection with lowest abscissa on line
            points[0] = line.pointOfMinAbscissa(iscs);
            points[1] = points[0];
        } else {
            if (this.sphere.intersects(line)) {
                // The line intersects the part of the sphere that isn't part
                // of the spherical cap.
                // This means the closest part of the spherical cap to the line
                // is on the disk "closing" the cap.
                closeToDisk = true;
            } else {
                // There is a single point on the sphere that is the closest
                // to the line.
                // Here we find this point.
                final Vector3D center = centSphere;
                final Vector3D origLine = line.getOrigin();
                final Vector3D dirLineNorm = line.getDirection().normalize();
                // Vector circle center minus line origin
                final Vector3D cMinusOrig = center.add(-1., origLine);
                // dot product of cMinusOrig and line direction
                final double dotP = cMinusOrig.dotProduct(dirLineNorm);
                // Vector : the point of the line closest to the sphere
                final Vector3D closestPtLine = origLine.add(dotP, dirLineNorm);
                // Vector from the center to the point of the line closest to the sphere
                final Vector3D centClos = closestPtLine.add(-1., center);
                // Vector : the point of the sphere closest to the line
                final Vector3D ourPoint = center.add(sphereRadius, centClos.normalize());
                // Is this point on the spherical cap (i.e. on the right side of the plane) ?
                if (this.plane.getOffset(ourPoint) >= 0) {
                    // Our point is on the spherical cap
                    // The closest points are found using the sphere
                    points = this.sphere.closestPointTo(line);
                } else {
                    // Our point is not on the spherical cap.
                    // This means the closest part of the spherical cap to the line
                    // is on the disk.
                    closeToDisk = true;
                }
            }
            if (closeToDisk) {
                // The closest point are found using the disk.
                points = this.capDisk.closestPointTo(line);
            }
        }

        return points;
    }

    /**
     * Get a representation for this sphere.
     * The given parameters are in the same order as
     * in the constructor.
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
        // "Sphere center":
        res.append("Sphere center");
        res.append(this.sphere.getCenter().toString());
        res.append(comma);
        // "Sphere radius":
        res.append("Sphere radius").append(open);
        res.append(this.sphere.getRadius()).append(close);
        res.append(comma);
        // "Plane origin":
        res.append("Plane origin");
        res.append(this.plane.getOrigin());
        res.append(comma);
        // "Plane normal":
        res.append("Plane normal");
        res.append(this.plane.getNormal());
        res.append(close);

        return res.toString();
    }
}
