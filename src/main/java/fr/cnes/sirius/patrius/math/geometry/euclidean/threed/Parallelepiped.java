/**
 * 
 * Copyright 2011-2017 CNES
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
 * @history creation 07/10/2011
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:345:30/10/2014:modified comments ratio
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

import java.io.Serializable;
import java.util.Locale;

import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.utils.UtilsPatrius;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * <p>
 * This is a describing class for a rectangle parallelepiped shape, with some algorithm to compute intersections and
 * distances to some other objects.
 * </p>
 * 
 * @useSample <p>
 *            Creation with three Vector3D and two doubles : Parallelepiped parallelepiped = new Parallelepiped(center,
 *            vectorU, pseudoVectorV, length, width, height); Intersection with a line : boolean intersects =
 *            parallelepiped(line);
 *            </p>
 * 
 * @concurrency immutable
 * 
 * @see Shape#distanceTo(Line)
 * @see Shape#intersects(Line)
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: Parallelepiped.java 17583 2017-05-10 13:05:10Z bignon $
 * 
 * @since 1.0
 * 
 */
public class Parallelepiped implements SolidShape, CrossSectionProvider, Serializable {

    /** Serializable version identifier. */
    private static final long serialVersionUID = 7729404150529575478L;

    /** Vectors comparisons epsilon */
    private static final double VECTORSCOMPARISONEPS = UtilsPatrius.GEOMETRY_EPSILON;
    
    /** Number of corners in parallelepiped */
    private static final int CORNERS_NB = 8;

    /** Position of the center of the parallelepiped */
    private final Vector3D center;

    /** U vector of the Frame */
    private final Vector3D u;

    /** V vector of the Frame */
    private final Vector3D v;

    /** W vector of the Frame */
    private final Vector3D w;

    /** Dimension in the u direction */
    private final double length;

    /** Dimension in the v direction */
    private final double width;

    /** Dimension in the w direction */
    private final double height;

    /** Corners of the rectangle */
    private final Vector3D[] corners;

    /** The face 1 */
    private final Plate[] faces;

    /**
     * Build a parallelepiped from the position of its center, two vectors to describe its local frame and dimensions.
     * 
     * @param inCenter
     *        the center of the plate
     * @param inU
     *        a non-normalised vector parallel to u
     * @param inV
     *        the second vector defining the plate's plane : corrected to be orthogonal to U
     * @param inLength
     *        the parallelepiped's length along U
     * @param inWidth
     *        the parallelepiped's width along V (once corrected)
     * @param inHeight
     *        the parallelepiped's height
     * @exception IllegalArgumentException
     *            if the vectors do not define a plane, or if the dimensions are negative or null
     * */
    public Parallelepiped(final Vector3D inCenter, final Vector3D inU, final Vector3D inV, final double inLength,
        final double inWidth, final double inHeight) {
        // The vectors u and v must not be parallel
        // The e-10 epsilon is chosen to be coherent with the Plane class
        final String message = PatriusMessages.ZERO_NOT_ALLOWED.getLocalizedString(Locale.getDefault());

        if (Vector3D.crossProduct(inV.normalize(), inU.normalize()).getNorm() < VECTORSCOMPARISONEPS) {
            throw new IllegalArgumentException(message);
        }
        if (inLength < 0.0 || Precision.equals(inLength, 0.0)) {
            throw new IllegalArgumentException(message);
        }
        if (inWidth < 0.0 || Precision.equals(inWidth, 0.0)) {
            throw new IllegalArgumentException(message);
        }
        if (inHeight < 0.0 || Precision.equals(inHeight, 0.0)) {
            throw new IllegalArgumentException(message);
        }

        this.center = new Vector3D(1.0, inCenter);
        this.length = inLength;
        this.width = inWidth;
        this.height = inHeight;
        // inU.getNorm() cannot be 0
        this.u = new Vector3D(1.0 / inU.getNorm(), inU);

        // the v vector is obtained by removing to inV its projection on u to itself,
        // and then by normalising it.
        // In the end, v is normal to u, normalised, and "in the direction" of inV
        this.v = inV.subtract(new Vector3D(Vector3D.dotProduct(this.u, inV), this.u)).normalize();

        // The W vector is a cross product of u and v.
        this.w = Vector3D.crossProduct(this.u, this.v);

        // Corners computation
        this.corners = new Vector3D[CORNERS_NB];
        this.corners[0] =
            new Vector3D(1.0, this.center, this.length / 2.0, this.u, this.width / 2.0, this.v, this.height / 2.0,
                this.w);
        this.corners[1] =
            new Vector3D(1.0, this.center, -this.length / 2.0, this.u, this.width / 2.0, this.v, this.height / 2.0,
                this.w);
        this.corners[2] =
            new Vector3D(1.0, this.center, -this.length / 2.0, this.u, -this.width / 2.0, this.v, this.height / 2.0,
                this.w);
        this.corners[3] =
            new Vector3D(1.0, this.center, this.length / 2.0, this.u, -this.width / 2.0, this.v, this.height / 2.0,
                this.w);
        this.corners[4] =
            new Vector3D(1.0, this.center, this.length / 2.0, this.u, this.width / 2.0, this.v, -this.height / 2.0,
                this.w);
        this.corners[5] =
            new Vector3D(1.0, this.center, -this.length / 2.0, this.u, this.width / 2.0, this.v, -this.height / 2.0,
                this.w);
        this.corners[6] =
            new Vector3D(1.0, this.center, -this.length / 2.0, this.u, -this.width / 2.0, this.v, -this.height / 2.0,
                this.w);
        this.corners[CORNERS_NB - 1] =
            new Vector3D(1.0, this.center, this.length / 2.0, this.u, -this.width / 2.0, this.v, -this.height / 2.0,
                this.w);

        // Faces computation
        this.faces = new Plate[6];
        this.faces[0] = new Plate(this.center.add(this.length / 2.0, this.u), this.v, this.w, this.width, this.height);
        this.faces[1] = new Plate(this.center.add(this.width / 2.0, this.v), this.u, this.w, this.length, this.height);
        this.faces[2] = new Plate(this.center.add(-this.length / 2.0, this.u), this.v, this.w, this.width, this.height);
        this.faces[3] = new Plate(this.center.add(-this.width / 2.0, this.v), this.u, this.w, this.length, this.height);
        this.faces[4] = new Plate(this.center.add(this.height / 2.0, this.w), this.u, this.v, this.length, this.width);
        this.faces[5] = new Plate(this.center.add(-this.height / 2.0, this.w), this.u, this.v, this.length, this.width);
    }

    /**
     * @return the center
     */
    public Vector3D getCenter() {
        return this.center;
    }

    /**
     * @return the u
     */
    public Vector3D getU() {
        return this.u;
    }

    /**
     * @return the v
     */
    public Vector3D getV() {
        return this.v;
    }

    /**
     * @return the w
     */
    public Vector3D getW() {
        return this.w;
    }

    /**
     * @return the length
     */
    public double getLength() {
        return this.length;
    }

    /**
     * @return the width
     */
    public double getWidth() {
        return this.width;
    }

    /**
     * @return the height
     */
    public double getHeight() {
        return this.height;
    }

    /**
     * @return the corners
     */
    public Vector3D[] getCorners() {
        final Vector3D[] copy = new Vector3D[this.corners.length];
        System.arraycopy(this.corners, 0, copy, 0, this.corners.length);
        return copy;
    }

    /**
     * @return the faces
     */
    public Plate[] getFaces() {
        final Plate[] copy = new Plate[this.faces.length];
        System.arraycopy(this.faces, 0, copy, 0, this.faces.length);
        return copy;
    }

    /** {@inheritDoc} */
    @Override
    public boolean intersects(final Line line) {
        // The test is made on each face with a stop if an intersection is found
        boolean intersects = false;
        int i = 0;

        // Only five faces are tested :
        // the last one can't be the only one to be intersected !
        while (i < 5 && !intersects) {
            intersects = this.faces[i].intersects(line);
            i++;
        }
        return intersects;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D[] getIntersectionPoints(final Line line) {
        // Initialisations
        Vector3D[] intersections = new Vector3D[0];
        int currentLength = 0;
        Vector3D[] temp;
        Vector3D[] intersectionsWithFace;
        boolean equalityTest;

        // The computation is made on each face
        for (int i = 0; i < 6; i++) {

            // call to the face's method
            intersectionsWithFace = this.faces[i].getIntersectionPoints(line);

            // if a new intersection is found, the point is added
            // to the list.
            if (intersectionsWithFace.length != 0) {
                // initialisations
                currentLength = currentLength + 1;
                temp = intersections;
                intersections = new Vector3D[currentLength];
                equalityTest = false;
                final Vector3D intersectionWithFaceVect = intersectionsWithFace[0];

                // creation of the new array and test of the new intersection point
                for (int j = 0; j < currentLength - 1; j++) {
                    intersections[j] = temp[j];
                    // if the point is identical to a previously found point...
                    equalityTest = intersectionWithFaceVect.subtract(intersections[j]).getNorm() < VECTORSCOMPARISONEPS
                        || equalityTest;
                }

                // the intersection point is added only if it is different to
                // the previously found points
                if (!equalityTest) {
                    intersections[currentLength - 1] = intersectionWithFaceVect;
                } else {
                    intersections = temp;
                }
            }
        }

        return intersections;
    }

    /** {@inheritDoc} */
    @Override
    public double distanceTo(final Line line) {
        // The computation is made on each face
        double distance = Double.POSITIVE_INFINITY;
        for (int i = 0; i < 6; i++) {
            distance = MathLib.min(distance, this.faces[i].distanceTo(line));
        }
        return distance;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D[] closestPointTo(final Line line) {
        Vector3D[] points = new Vector3D[2];

        // if the line does'nt intersect the parallelepiped
        if (!this.intersects(line)) {
            // The computation is made on each face
            double distance = Double.POSITIVE_INFINITY;
            double currentDistance;
            for (int i = 0; i < 6; i++) {

                // the face considered is the one realizing the shortest distance
                currentDistance = this.faces[i].distanceTo(line);
                if (distance > currentDistance) {
                    distance = currentDistance;
                    points = this.faces[i].closestPointTo(line);
                }
            }
        } else {
            // if an intersection is found the points are identical
            points[1] = this.getIntersectionPoints(line)[0];
            points[0] = points[1];
        }

        return points;
    }

    /**
     * Get a representation for this parallelepiped.
     * The given parameters are in the same order as
     * in the constructor.
     * 
     * @return a representation for this parallelepiped
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
        res.append("Center");
        res.append(this.center.toString());
        res.append(comma);
        // "U vector":
        res.append("U vector");
        res.append(this.u.toString());
        res.append(comma);
        // "V vector":
        res.append("V vector");
        res.append(this.v.toString());
        res.append(comma);
        // "Length":
        res.append("Length").append(open);
        res.append(this.length).append(close);
        res.append(comma);
        // "Width":
        res.append("Width").append(open);
        res.append(this.width).append(close);
        res.append(comma);
        // "Height":
        res.append("Height").append(open);
        res.append(this.height).append(close);
        res.append(close);

        return res.toString();
    }

    /** {@inheritDoc} */
    @Override
    public double getCrossSection(final Vector3D direction) {

        // initialisation
        double totalCrossSection = 0.0;

        // loop on the faces 3, 4, 5
        // Those three are enough to compute the cross section
        for (int i = 2; i < 5; i++) {

            // cross section of this face
            final double positiveArea = this.faces[i].getCrossSection(direction);

            // adding to the total
            totalCrossSection += positiveArea;
        }

        return totalCrossSection;
    }
}
