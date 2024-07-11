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
 * @history creation 21/10/2011
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:345:30/10/2014:modified comments ratio
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

import java.io.Serializable;
import java.util.Locale;

import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * <p>
 * This is a describing class for a 3D oblique circular cylinder ended by two planes normal to its axis, with some
 * algorithm to compute intersections and distances to some other objects.
 * </p>
 * 
 * @useSample <p>
 *            Creation with two radiuses, a height and three Vector3D : Vector3D origin = new Vector3D(1.0, 6.0, -2.0);
 *            Vector3D direction = new Vector3D(6.0, -3.0, -1.0); Vector3D uVector = new Vector3D(-5.0, 3.0, 0.0);
 *            double radiusA = 2.0; double radiusB = 2.0; double height = 5.0; ObliqueCircularCylinder cylinder = new
 *            ObliqueCircularCylinder(origin, direction, uVector, radiusA, radiusB, length); Intersection with a line :
 *            boolean intersects = cylinder(line);
 *            </p>
 * 
 * @concurrency immutable
 * 
 * @see Shape#distanceTo(Line)
 * @see Shape#intersects(Line)
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: EllipticCylinder.java 17583 2017-05-10 13:05:10Z bignon $
 * 
 * @since 1.0
 * 
 */
public final class EllipticCylinder implements Cylinder, Serializable {

    /** Serializable version identifier. */
    private static final long serialVersionUID = 9040808449742261729L;

    /** length of the cone on its axis */
    private final double height;

    /** radius on U axis */
    private final double radiusA;

    /** radius on V axis */
    private final double radiusB;

    /** The associated infinite cone */
    private final InfiniteEllipticCylinder infiniteCylinder;

    /** the up ending ellipse */
    private final Ellipse upEllipse;

    /** the bottom ending ellipse */
    private final Ellipse bottomEllipse;

    /** Position of the origin of the axis */
    private final Vector3D origin;

    /** Direction of the axis, defines the orientation : Z of the local frame */
    private final Vector3D direction;

    /** U vector of the local frame */
    private final Vector3D u;

    /** V vector of the local frame */
    private final Vector3D v;

    /**
     * Build an oblique circular cylinder from its radiuses, the height, the origin, the approximative u vector of the
     * local frame and the direction of its axis.
     * 
     * @param inOrigin
     *        the origin of the axis
     * @param inDirection
     *        the direction of the axis
     * @param inUvector
     *        approximative U vector of the local frame : corrected to be orthogonal to the direction
     * @param inRadiusA
     *        the angle of the cone on U direction
     * @param inRadiusB
     *        the angle of the cone on V direction
     * @param inHeight
     *        the height of the cone on its axis
     * @exception IllegalArgumentException
     *            if one radius or the height is negative or null, or if the direction vector or u vector has a null
     *            norm, or if they are parallel.
     * */
    public EllipticCylinder(final Vector3D inOrigin, final Vector3D inDirection, final Vector3D inUvector,
        final double inRadiusA, final double inRadiusB, final double inHeight) {

        // The height must be positive
        final String message = PatriusMessages.NUMBER_TOO_SMALL.getLocalizedString(Locale.getDefault());
        if (inHeight < 0.0 || Precision.equals(0.0, inHeight)) {
            throw new IllegalArgumentException(message);
        }

        // creation of the associated infinite oblique cone
        this.infiniteCylinder = new InfiniteEllipticCylinder(inOrigin, inDirection, inUvector, inRadiusA, inRadiusB);

        // Initialisations
        this.origin = new Vector3D(1.0, inOrigin);
        this.direction = inDirection.normalize();
        this.height = inHeight;
        this.radiusA = inRadiusA;
        this.radiusB = inRadiusB;

        // creation of the right U and V vectors
        this.u =
            inUvector.subtract(new Vector3D(Vector3D.dotProduct(this.direction, inUvector), this.direction))
                .normalize();
        this.v = Vector3D.crossProduct(this.direction, this.u);

        // creation of the ending ellipses
        this.upEllipse =
            new Ellipse(inOrigin.add(this.height / 2.0, this.direction), this.direction, inUvector, this.radiusA,
                this.radiusB);
        this.bottomEllipse =
            new Ellipse(inOrigin.add(-this.height / 2.0, this.direction), this.direction, inUvector, this.radiusA,
                this.radiusB);
    }

    /**
     * @return the height
     */
    public double getHeight() {
        return this.height;
    }

    /**
     * @return the radiusA, on U axis of the local frame
     */
    public double getRadiusA() {
        return this.radiusA;
    }

    /**
     * @return the radiusB, on V axis of the local frame
     */
    public double getRadiusB() {
        return this.radiusB;
    }

    /**
     * @return the origin
     */
    public Vector3D getOrigin() {
        return this.origin;
    }

    /**
     * @return the u vector of the local frame
     */
    public Vector3D getU() {
        return this.u;
    }

    /**
     * @return the v vector of the local frame
     */
    public Vector3D getV() {
        return this.v;
    }

    /**
     * @return the direction of the axis
     */
    public Vector3D getDirection() {
        return this.direction;
    }

    /** {@inheritDoc} */
    @Override
    public boolean intersects(final Line line) {

        // computation of the intersection with the finite cylinder
        final boolean intersectsCylinder = this.getIntersectionPoints(line).length > 0;

        // search of intersections on the bottom ellipse
        // A line can't intersect the bottom ellipse but not something else, so it is
        // not necessary to be tested
        final boolean intersectsEllipse = this.upEllipse.intersects(line);

        // one intersection is needed for the return to be true
        return intersectsEllipse || intersectsCylinder;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D[] getIntersectionPoints(final Line line) {
        // computation of the intersection with the finite cylinder
        final Vector3D[] intersectionsWithCylinder = this.intersectionsWithCylinder(line);
        final int nbIntWithCyl = intersectionsWithCylinder.length;

        // search of intersections on the up ellipse
        final Vector3D[] intersectionsWithUpEllipse = this.upEllipse.getIntersectionPoints(line);
        final int nbIntWithUpEllipse = intersectionsWithUpEllipse.length;

        // search of intersections on the up ellipse
        final Vector3D[] intersectionsWithBottomEllipse = this.bottomEllipse.getIntersectionPoints(line);
        final int nbIntWithBottomEllipse = intersectionsWithBottomEllipse.length;

        // Initialisation of the result array
        final Vector3D[] intersections = new Vector3D[nbIntWithCyl + nbIntWithUpEllipse + nbIntWithBottomEllipse];

        // concatenation of the found points
        System.arraycopy(intersectionsWithCylinder, 0, intersections, 0, nbIntWithCyl);
        if (nbIntWithUpEllipse > 0) {
            intersections[nbIntWithCyl] = intersectionsWithUpEllipse[0];
        }
        if (nbIntWithBottomEllipse > 0) {
            intersections[nbIntWithCyl + 1] = intersectionsWithBottomEllipse[0];
        }

        return intersections;
    }

    /**
     * Computes the intersection points with the finite cylinder amongst the ones with the infinite cylinder
     * 
     * @param line
     *        the considered line
     * @return the intersection points
     */
    private Vector3D[] intersectionsWithCylinder(final Line line) {

        // computation of the intersections with the infinite cone
        final Vector3D[] infiniteConeInterPoints = this.infiniteCylinder.getIntersectionPoints(line);

        // Initialisations
        final int nbPoints = infiniteConeInterPoints.length;
        final double[] originDbl = { this.origin.getX(), this.origin.getY(), this.origin.getZ() };
        final double[] dirDbl = { this.direction.getX(), this.direction.getY(), this.direction.getZ() };
        Vector3D[] points = new Vector3D[0];
        Vector3D[] temp;
        int nbPointsOK = 0;

        // search of intersections on the finite cylinder
        for (int i = 0; i < nbPoints; i++) {

            // Initialisation of the current intersection point in doubles
            final double[] currentPoint = { infiniteConeInterPoints[i].getX(), infiniteConeInterPoints[i].getY(),
                infiniteConeInterPoints[i].getZ() };

            // computation of the third coordinate (on the axis)
            // of this point on the local frame of the cylinder
            final double projOnTheAxis = (currentPoint[0] - originDbl[0]) * dirDbl[0]
                + (currentPoint[1] - originDbl[1]) * dirDbl[1] + (currentPoint[2] - originDbl[2]) * dirDbl[2];

            // the point is on the finite cone...
            if (MathLib.abs(projOnTheAxis) < this.height / 2.0) {

                // ..it is added to the array of results
                nbPointsOK++;
                temp = points;
                points = new Vector3D[nbPointsOK];
                System.arraycopy(temp, 0, points, 0, nbPointsOK - 1);
                points[nbPointsOK - 1] = infiniteConeInterPoints[i];
            }
        }
        return points;

    }

    /** {@inheritDoc} */
    @Override
    public double distanceTo(final Line line) {
        double distance = 0.0;

        if (!this.intersects(line)) {
            // point of the infinite cylinder that is the closest to the line
            final Vector3D[] infConeClosestPointToLine = this.infiniteCylinder.closestPointTo(line);

            // Z coordinate of this point in the local frame computation
            final double projOnAxis = Vector3D.dotProduct(infConeClosestPointToLine[0], this.direction);

            // test of the Z coordinate
            if (projOnAxis < -this.height / 2.0) {
                // if the coordinate is lower than H/2, the distance is measured from the
                // bottom ending ellipse
                distance = this.bottomEllipse.distanceTo(line);
            } else if (projOnAxis > this.height / 2.0) {
                // if the coordinate is greater than H/2, the distance is measured from the
                // up ending ellipse
                distance = this.upEllipse.distanceTo(line);
            } else {
                // if it belongs to the finite cone, the distance is with the cone's side
                distance = this.infiniteCylinder.distanceTo(line);
            }
        }
        return distance;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D[] closestPointTo(final Line line) {
        Vector3D[] points = new Vector3D[2];

        if (!this.intersects(line)) {
            // point of the infinite cylinder that is the closest to the line
            final Vector3D[] infCylClosestPointToLine = this.infiniteCylinder.closestPointTo(line);

            // Z coordinate of this point in the local frame computation
            final double projOnAxis = Vector3D.dotProduct(infCylClosestPointToLine[0], this.direction);

            // test of the Z coordinate
            if (projOnAxis < -this.height / 2.0) {
                // if the coordinate is lower than H/2, the point is searched on the
                // bottom ending ellipse
                points = this.bottomEllipse.closestPointTo(line);
            } else if (projOnAxis > this.height / 2.0) {
                // if the coordinate is greater than H/2, the point is searched on the
                // up ending ellipse
                points = this.upEllipse.closestPointTo(line);
            } else {
                // if it belongs to the finite cone
                points = infCylClosestPointToLine;
            }
        } else {
            points[0] = this.getIntersectionPoints(line)[0];
            points[1] = points[0];
        }

        return points;
    }

    /**
     * Get a string representation for this elliptic cylinder.
     * The given parameters are in the same order as
     * in the constructor.
     * 
     * @return a string representation for this elliptic cylinder
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
        res.append(this.origin.toString()).append(comma);
        // "Direction":
        res.append("Direction");
        res.append(this.direction.toString()).append(comma);
        // "U vector":
        res.append("U vector");
        res.append(this.u.toString()).append(comma);
        // "Radius A":
        res.append("Radius A").append(open);
        res.append(this.radiusA).append(close).append(comma);
        // "Radius B":
        res.append("Radius B").append(open);
        res.append(this.radiusB).append(close).append(comma);
        // "Height"
        res.append("Height").append(open);
        res.append(this.height).append(close);
        res.append(close);
        return res.toString();
    }

}
