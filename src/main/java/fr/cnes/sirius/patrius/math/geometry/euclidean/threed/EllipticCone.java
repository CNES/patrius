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
 * @history creation 20/10/2011
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
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
 * This is a describing class for a 3D oblique circular cone ended by a plane normal to its axis, with some algorithm to
 * compute intersections and distances to some other objects.
 * </p>
 * 
 * @useSample <p>
 *            Creation with two angles, a height and three Vector3D : Vector3D origin = new Vector3D(1.0, 6.0, -2.0);
 *            Vector3D direction = new Vector3D(6.0, -3.0, -1.0); Vector3D uVector = new Vector3D(-5.0, 3.0, 0.0);
 *            double angleU = 2.0; double angleV = 2.0; double height = 5.0; ObliqueCircularCone cone = new
 *            ObliqueCircularCone(origin, direction, uVector, angleU, angleV, length); Intersection with a line :
 *            boolean intersects = cone(line);
 *            </p>
 * 
 * @concurrency immutable
 * 
 * @see Shape#distanceTo(Line)
 * @see Shape#intersects(Line)
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: EllipticCone.java 17583 2017-05-10 13:05:10Z bignon $
 * 
 * @since 1.0
 * 
 */
public final class EllipticCone implements Cone, Serializable {

     /** Serializable UID. */
    private static final long serialVersionUID = 8807976486767394008L;

    /** length of the cone on its axis */
    private final double height;

    /** The associated infinite cone */
    private final InfiniteEllipticCone infiniteCone;

    /** the ending ellipse */
    private final Ellipse upEllipse;

    /** Position of the origin of the axis */
    private final Vector3D origin;

    /** Direction of the axis, defines the orientation : Z of the local frame */
    private final Vector3D direction;

    /** U vector of the local frame */
    private final Vector3D u;

    /** V vector of the local frame */
    private final Vector3D v;

    /**
     * Build an oblique circular cone from its radius, the height, the origin (apex), the approximative u vector of the
     * local frame and direction of its axis.
     * 
     * @param inOrigin
     *        the origin of the axis
     * @param inDirection
     *        the direction of the axis
     * @param inUvector
     *        approximative U vector of the local frame : corrected to be orthogonal to the direction
     * @param inAngleU
     *        the angle of the cone on U direction
     * @param inAngleV
     *        the angle of the cone on V direction
     * @param inHeight
     *        the height of the cone on its axis
     * @exception IllegalArgumentException
     *            if one angle or the height is negative or null, or if the direction vector or u vector has a null
     *            norm, or if they are parallel.
     * */
    public EllipticCone(final Vector3D inOrigin, final Vector3D inDirection, final Vector3D inUvector,
                        final double inAngleU, final double inAngleV, final double inHeight) {

        final String message = PatriusMessages.ZERO_NOT_ALLOWED.getLocalizedString(Locale.getDefault());

        // The height must be positive
        if (inHeight < 0.0 || Precision.equals(0.0, inHeight)) {
            throw new IllegalArgumentException(message);
        }

        // creation of the associated infinite oblique cone
        this.infiniteCone = new InfiniteEllipticCone(inOrigin, inDirection, inUvector, inAngleU, inAngleV);

        this.origin = new Vector3D(1.0, inOrigin);
        this.direction = inDirection.normalize();
        this.height = inHeight;

        // creation of the right U and V vectors
        this.u =
            inUvector.subtract(new Vector3D(Vector3D.dotProduct(this.direction, inUvector), this.direction))
                .normalize();
        this.v = Vector3D.crossProduct(this.direction, this.u);

        // creation of the ending ellipse
        final double radiusA = this.height * MathLib.tan(inAngleU);
        final double radiusB = this.height * MathLib.tan(inAngleV);
        this.upEllipse =
            new Ellipse(inOrigin.add(this.height, this.direction), this.direction, inUvector, radiusA, radiusB);
    }

    /**
     * @return the height
     */
    public double getHeight() {
        return this.height;
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
     * @return the angle in U/W plane
     */
    public double getAngleU() {
        return this.infiniteCone.getAngleX();
    }

    /**
     * @return the angle in U/W plane
     */
    public double getAngleV() {
        return this.infiniteCone.getAngleY();
    }

    /**
     * @return the u vector for the local frame
     */
    public Vector3D getU() {
        return this.u;
    }

    /**
     * @return the v vector for the local frame
     */
    public Vector3D getV() {
        return this.v;
    }

    /** {@inheritDoc} */
    @Override
    public boolean intersects(final Line line) {

        // computation of the intersection with the finite cone
        // A line can't intersect the ending ellipse but not the finite cone, so it is
        // not necessary to be tested
        return this.intersectionsWithCone(line).length > 0;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D[] getIntersectionPoints(final Line line) {

        // computation of the intersection with the finite cone
        final Vector3D[] intersectionsWithCone = this.intersectionsWithCone(line);
        final int nbIntWithCyl = intersectionsWithCone.length;

        // search of intersections on the ellipse
        final Vector3D[] intersectionsWithEllipse = this.upEllipse.getIntersectionPoints(line);
        final int nbIntWithEllipse = intersectionsWithEllipse.length;

        // Initialisation of the result array
        final Vector3D[] intersections = new Vector3D[nbIntWithCyl + nbIntWithEllipse];

        // concatenation of the found points
        System.arraycopy(intersectionsWithCone, 0, intersections, 0, nbIntWithCyl);
        if (nbIntWithEllipse > 0) {
            intersections[nbIntWithCyl] = intersectionsWithEllipse[0];
        }

        return intersections;
    }

    /**
     * Computes the intersection points with the finite cone amongst the ones with the infinite cone
     * 
     * @param line
     *        the considered line
     * @return the intersection points
     */
    private Vector3D[] intersectionsWithCone(final Line line) {

        // computation of the intersections with the infinite cone
        final Vector3D[] infiniteConeInterPoints = this.infiniteCone.getIntersectionPoints(line);

        // Initialisations
        Vector3D[] points = new Vector3D[0];
        Vector3D[] temp;
        int nbPointsOK = 0;
        final int nbPoints = infiniteConeInterPoints.length;
        final double[] originDbl = { this.origin.getX(), this.origin.getY(), this.origin.getZ() };
        final double[] dirDbl = { this.direction.getX(), this.direction.getY(), this.direction.getZ() };

        // search of intersections on the finite cone
        for (int i = 0; i < nbPoints; i++) {

            // Initialisation of the current intersection point in doubles
            final double[] currentPoint = { infiniteConeInterPoints[i].getX(), infiniteConeInterPoints[i].getY(),
                infiniteConeInterPoints[i].getZ() };

            // computation of the third coordinate (on the axis)
            // of this point on the local frame of the cone
            final double projOnTheAxis = (currentPoint[0] - originDbl[0]) * dirDbl[0]
                    + (currentPoint[1] - originDbl[1]) * dirDbl[1] + (currentPoint[2] - originDbl[2]) * dirDbl[2];

            // the point is on the finite cone...
            if (projOnTheAxis < this.height) {

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
            // point of the cone that is the closest to the line
            final Vector3D[] infConeClosestPointToLine = this.infiniteCone.closestPointTo(line);

            // Z coordinate of this point in the local frame computation
            final double projOnAxis = Vector3D.dotProduct(infConeClosestPointToLine[0], this.direction);

            // if it belongs to the finite cone, the distance is with the cone's side
            if (projOnAxis <= this.height) {
                distance = this.infiniteCone.distanceTo(line);
            } else {

                // if the closest point on the infinite cone is not on the finite cone,
                // the distance must be computed to the up ending ellipse
                distance = this.upEllipse.distanceTo(line);
            }
        }

        return distance;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D[] closestPointTo(final Line line) {
        Vector3D[] points = new Vector3D[2];

        if (this.intersects(line)) {
            // Intersections
            final Vector3D[] iscs = this.getIntersectionPoints(line);

            // Return intersection with lowest abscissa on line
            points[0] = line.pointOfMinAbscissa(iscs);
            points[1] = points[0];
        } else {
            // point of the infinite cone that is the closest to the line
            final Vector3D[] infConeClosestPointToLine = this.infiniteCone.closestPointTo(line);

            // Z coordinate of this point in the local frame computation
            final double projOnAxis = Vector3D.dotProduct(infConeClosestPointToLine[0], this.direction);

            // test of the Z coordinate
            if (projOnAxis > this.height) {
                // if the coordinate is greater than H/2, the point is searched on the
                // up ending disk
                points = this.upEllipse.closestPointTo(line);
            } else {
                // if it belongs to the finite cone
                points = infConeClosestPointToLine;
            }
        }

        return points;
    }

    /**
     * Get a representation for this oblique circular cone.
     * The given parameters are in the same order as
     * in the constructor.
     * 
     * @return a representation for this oblique circular cone
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
        // "Angle on U":
        res.append("Angle on U").append(open);
        res.append(this.infiniteCone.getAngleX()).append(close).append(comma);
        // "Angle on V":
        res.append("Angle on V").append(open);
        res.append(this.infiniteCone.getAngleY()).append(close).append(comma);
        // "Height":
        res.append("Height").append(open);
        res.append(this.height).append(close);
        res.append(close);

        return res.toString();
    }

}
