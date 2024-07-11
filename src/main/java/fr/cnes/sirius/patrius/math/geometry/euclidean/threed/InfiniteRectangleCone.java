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
 * @history creation 11/10/2011
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:345:30/10/2014:modified comments ratio
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

import java.io.Serializable;
import java.util.Locale;

import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.utils.UtilsPatrius;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * <p>
 * This is a describing class for a 3D infinite cone, with some algorithm to compute intersections and distances to some
 * other objects.
 * </p>
 * 
 * @useSample <p>
 *            Creation with three Vector3D and two angles between 0.0 and PI/4: Vector3D origin = new Vector3D(1.0, 6.0,
 *            -2.0); Vector3D axis = new Vector3D(6.0, -3.0, -1.0); Vector3D uVector = new Vector3D(-5.0, 3.0, 0.0);
 *            InfiniteRectangleCone cone = new InfiniteRectangleCone(origin, axis, uVector, 0.82, 0.56); Intersection
 *            with a line : boolean intersects = cone(line);
 *            </p>
 * 
 * @concurrency immutable
 * 
 * @see Shape#distanceTo(Line)
 * @see Shape#intersects(Line)
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: InfiniteRectangleCone.java 17583 2017-05-10 13:05:10Z bignon $
 * 
 * @since 1.0
 * 
 */
public final class InfiniteRectangleCone implements InfiniteCone, Serializable {

    /** Serializable version identifier. */
    private static final long serialVersionUID = -2908830751652223671L;

    /** Vectors comparisons epsilon */
    private static final double VECTORSCOMPARISONEPS = UtilsPatrius.GEOMETRY_EPSILON;

    /** Position of the origin of the cone */
    private final Vector3D origin;

    /** Position of the origin of the cone in doubles */
    private final double[] originDoubles;

    /** Axis of the cone, defines its orientation : Z of the local frame */
    private final Vector3D axis;

    /** U vector of the local frame */
    private final Vector3D u;

    /** V vector of the local frame */
    private final Vector3D v;

    /** angle on the U axis */
    private final double angleU;

    /** angle on the U axis */
    private final double angleV;

    /** Side axis */
    private final Vector3D[] sideAxis;

    /** Side axis in doubles */
    private final double[][] sideAxisDoubles;

    /** Side normal vectors in doubles */
    private final double[][] sideNormalDoubles;

    /**
     * Build an infinite rectangle cone from the position of its origin, its axis, a vector defining the orientation of
     * the rectangle and two angles
     * 
     * @param inOrigin
     *        the origin of the cone
     * @param inDirection
     *        the direction of the axis of the cone
     * @param inUvector
     *        the input u vector : will be recomputed to be orthogonal to the axis and normalised
     * @param inAngleU
     *        the angle of the pyramid on the U axis
     * @param inAngleV
     *        the angle of the pyramid on the V axis
     * @exception IllegalArgumentException
     *            if one of the angles is not between 0.0 and PI/2 or if the axis has a null norm
     */
    public InfiniteRectangleCone(final Vector3D inOrigin, final Vector3D inDirection, final Vector3D inUvector,
        final double inAngleU, final double inAngleV) {

        // The axis' norm must be positive
        // The e-10 epsilon is chosen to be coherent with the Plane class
        String message = PatriusMessages.NUMBER_TOO_SMALL.getLocalizedString(Locale.getDefault());
        if (Vector3D.crossProduct(inDirection, inUvector).getNorm() < VECTORSCOMPARISONEPS) {
            throw new IllegalArgumentException(message);
        }

        // The angles must be between 0.0 and PI/2
        message = PatriusMessages.ARGUMENT_OUTSIDE_DOMAIN.getLocalizedString(Locale.getDefault());
        if (inAngleU <= 0.0 || inAngleU >= MathUtils.HALF_PI) {
            throw new IllegalArgumentException(message);
        }
        if (inAngleV <= 0.0 || inAngleV >= MathUtils.HALF_PI) {
            throw new IllegalArgumentException(message);
        }

        this.origin = new Vector3D(1.0, inOrigin);
        this.axis = inDirection.normalize();
        this.angleU = inAngleU;
        this.angleV = inAngleV;

        this.originDoubles = new double[3];
        this.originDoubles[0] = this.origin.getX();
        this.originDoubles[1] = this.origin.getY();
        this.originDoubles[2] = this.origin.getZ();

        // creation of the right U and V vectors
        this.u = inUvector.subtract(new Vector3D(Vector3D.dotProduct(this.axis, inUvector), this.axis)).normalize();
        this.v = Vector3D.crossProduct(this.axis, this.u);

        // rotation matrix
        final double[][] matrixData = {
            { this.u.getX(), this.u.getY(), this.u.getZ() },
            { this.v.getX(), this.v.getY(), this.v.getZ() },
            { this.axis.getX(), this.axis.getY(), this.axis.getZ() } };
        final Matrix3D rotationMatrix = new Matrix3D(matrixData);

        // creation of the side axis
        this.sideAxis = new Vector3D[5];
        final double tanU = MathLib.tan(this.angleU);
        final double tanV = MathLib.tan(this.angleV);
        final double norm = MathLib.sqrt(1 + tanU * tanU + tanV * tanV);

        // one is repeated for the loops
        // Norm cannot be 0
        this.sideAxis[0] = rotationMatrix.transposeAndMultiply(new Vector3D(tanU / norm, tanV / norm, 1 / norm));
        this.sideAxis[1] = rotationMatrix.transposeAndMultiply(new Vector3D(-tanU / norm, tanV / norm, 1 / norm));
        this.sideAxis[2] = rotationMatrix.transposeAndMultiply(new Vector3D(-tanU / norm, -tanV / norm, 1 / norm));
        this.sideAxis[3] = rotationMatrix.transposeAndMultiply(new Vector3D(tanU / norm, -tanV / norm, 1 / norm));
        this.sideAxis[4] = this.sideAxis[0];

        // Creation of the side's normal vectors
        final Vector3D[] sideNormal = new Vector3D[5];
        sideNormal[0] = Vector3D.crossProduct(this.sideAxis[0], this.sideAxis[1]).normalize();
        sideNormal[1] = Vector3D.crossProduct(this.sideAxis[1], this.sideAxis[2]).normalize();
        sideNormal[2] = Vector3D.crossProduct(this.sideAxis[2], this.sideAxis[3]).normalize();
        sideNormal[3] = Vector3D.crossProduct(this.sideAxis[3], this.sideAxis[0]).normalize();
        sideNormal[4] = sideNormal[0];

        // filling of the vectors "in doubles"
        this.sideAxisDoubles = new double[5][3];
        this.sideNormalDoubles = new double[5][3];
        for (int i = 0; i < 5; i++) {
            this.sideAxisDoubles[i][0] = this.sideAxis[i].getX();
            this.sideAxisDoubles[i][1] = this.sideAxis[i].getY();
            this.sideAxisDoubles[i][2] = this.sideAxis[i].getZ();
            this.sideNormalDoubles[i][0] = sideNormal[i].getX();
            this.sideNormalDoubles[i][1] = sideNormal[i].getY();
            this.sideNormalDoubles[i][2] = sideNormal[i].getZ();
        }
    }

    /**
     * @return the origin
     */
    @Override
    public Vector3D getOrigin() {
        return this.origin;
    }

    /**
     * @return the axis
     */
    public Vector3D getAxis() {
        return this.axis;
    }

    /**
     * @return the U local frame vector
     */
    public Vector3D getU() {
        return this.u;
    }

    /**
     * @return the V local frame vector
     */
    public Vector3D getV() {
        return this.v;
    }

    /**
     * @return the angle on the U axis
     */
    public double getAngleU() {
        return this.angleU;
    }

    /**
     * @return the angle on the V axis
     */
    public double getAngleV() {
        return this.angleV;
    }

    /** {@inheritDoc} */
    @Override
    public boolean intersects(final Line line) {

        // This algorithm uses vectors expressed as doubles to avoid using
        // Vector3D objects for performances issues

        // Initialisations
        final Vector3D originLine = line.getOrigin();
        final Vector3D directionLine = line.getDirection();
        final double[] originTab = { originLine.getX(), originLine.getY(), originLine.getZ() };
        final double[] directionTab = { directionLine.getX(), directionLine.getY(), directionLine.getZ() };
        boolean intersects = false;
        double[] localResult;

        int i = 0;

        // test on each side
        while (i < 4 && !intersects) {
            localResult = this.intersectionWithDoubles(originTab, directionTab, i);
            intersects = localResult.length > 0;
            i++;
        }

        return intersects;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D[] getIntersectionPoints(final Line line) {

        // Initialisations
        final Vector3D originLine = line.getOrigin();
        final Vector3D directionLine = line.getDirection();
        final double[] originTab = { originLine.getX(), originLine.getY(), originLine.getZ() };
        final double[] directionTab = { directionLine.getX(), directionLine.getY(), directionLine.getZ() };
        Vector3D[] intersections = new Vector3D[0];
        int currentLength = 0;
        Vector3D[] temp;
        double[] intersectionWithFace;
        boolean equalityTest;

        // The computation is made on each face
        for (int i = 0; i < 4; i++) {

            // call to the side's method
            intersectionWithFace = this.intersectionWithDoubles(originTab, directionTab, i);

            // if a new intersection is found, the point is added
            // to the list.
            if (intersectionWithFace.length != 0) {
                // initialisations
                currentLength = intersections.length + 1;
                temp = intersections;
                intersections = new Vector3D[currentLength];
                equalityTest = false;
                final Vector3D intersectionWithFaceVect = new Vector3D(intersectionWithFace[0],
                    intersectionWithFace[1], intersectionWithFace[2]);

                // creation of the new array and test of the new intersection point
                for (int j = 0; j < currentLength - 1; j++) {
                    intersections[j] = temp[j];
                    // if the point is identical to a previously found point...
                    equalityTest = intersectionWithFaceVect.subtract(intersections[j]).getNorm() < VECTORSCOMPARISONEPS
                        || equalityTest;
                }

                // the intersection point is added only if it is different to
                // the previously found points
                if (equalityTest) {
                    intersections = temp;
                } else {
                    intersections[currentLength - 1] = intersectionWithFaceVect;
                }
            }
        }

        if (intersections.length != 0 && intersections[0].subtract(this.origin).getNorm() < VECTORSCOMPARISONEPS) {
            // case : the intersection point is the origin and appears 4 times
            intersections = new Vector3D[1];
            intersections[0] = this.origin;
        }

        return intersections;
    }

    /** {@inheritDoc} */
    @Override
    public double distanceTo(final Line line) {
        // Initialisations
        final double distance;

        if (this.intersects(line)) {
            distance = 0.0;
        } else {
            // closest points computation
            final Vector3D[] closestPoints = this.closestPointTo(line);
            // the distance is the one between the two points
            distance = closestPoints[1].subtract(closestPoints[0]).getNorm();
        }

        return distance;
    }

    /**
     * Computes the intersection point between the the plane containing the side and a line, using only doubles (and no
     * Vector3D) to describe them. and the line have already been tested.
     * 
     * @param originTab
     *        the line's origin point
     * @param directionTab
     *        the line's direction
     * @param side
     *        number of the side to be tested
     * @return the intersection point coordinates
     */
    private double[] intersectionWithDoubles(final double[] originTab, final double[] directionTab, final int side) {

        double[] result = new double[0];

        // Dot product of the line's direction and the normal vector of the plate.
        // There is no intersection if the result is zero.
        final double dotProdDirW = directionTab[0] * this.sideNormalDoubles[side][0] + directionTab[1]
            * this.sideNormalDoubles[side][1] + directionTab[2] * this.sideNormalDoubles[side][2];

        if (MathLib.abs(dotProdDirW) > VECTORSCOMPARISONEPS) {

            final double distOriginToPlane = (originTab[0] - this.originDoubles[0]) * this.sideNormalDoubles[side][0]
                + (originTab[1] - this.originDoubles[1]) * this.sideNormalDoubles[side][1]
                + (originTab[2] - this.originDoubles[2]) * this.sideNormalDoubles[side][2];

            // Computation of the intersection point of the line and the plane
            final double alpha = -MathLib.divide(distOriginToPlane, dotProdDirW);
            result = new double[3];
            result[0] = originTab[0] + alpha * directionTab[0];
            result[1] = originTab[1] + alpha * directionTab[1];
            result[2] = originTab[2] + alpha * directionTab[2];

            // result vector minus the cone's origin
            final double[] resMinOrigin = new double[3];
            resMinOrigin[0] = result[0] - this.originDoubles[0];
            resMinOrigin[1] = result[1] - this.originDoubles[1];
            resMinOrigin[2] = result[2] - this.originDoubles[2];

            // dot product with each side vector
            final double dot1 = resMinOrigin[0] * this.sideAxisDoubles[side][0]
                + resMinOrigin[1] * this.sideAxisDoubles[side][1]
                + resMinOrigin[2] * this.sideAxisDoubles[side][2];
            final double dot2 = resMinOrigin[0] * this.sideAxisDoubles[side + 1][0]
                + resMinOrigin[1] * this.sideAxisDoubles[side + 1][1]
                + resMinOrigin[2] * this.sideAxisDoubles[side + 1][2];

            // intermediary vectors computation.
            final double[] testedVectorU = new double[3];
            testedVectorU[0] = resMinOrigin[0] - dot2 * this.sideAxisDoubles[side + 1][0];
            testedVectorU[1] = resMinOrigin[1] - dot2 * this.sideAxisDoubles[side + 1][1];
            testedVectorU[2] = resMinOrigin[2] - dot2 * this.sideAxisDoubles[side + 1][2];
            final double[] testedVectorV = new double[3];
            testedVectorV[0] = resMinOrigin[0] - dot1 * this.sideAxisDoubles[side][0];
            testedVectorV[1] = resMinOrigin[1] - dot1 * this.sideAxisDoubles[side][1];
            testedVectorV[2] = resMinOrigin[2] - dot1 * this.sideAxisDoubles[side][2];

            // expressions tested to know if the result vector is between the sides vectors
            final double test1 = testedVectorU[0] * this.sideAxisDoubles[side][0]
                + testedVectorU[1] * this.sideAxisDoubles[side][1]
                + testedVectorU[2] * this.sideAxisDoubles[side][2];
            final double test2 = testedVectorV[0] * this.sideAxisDoubles[side + 1][0]
                + testedVectorV[1] * this.sideAxisDoubles[side + 1][1]
                + testedVectorV[2] * this.sideAxisDoubles[side + 1][2];

            if (test1 < 0.0 || test2 < 0.0) {
                result = new double[0];
            }
        }

        return result;

    }

    /** {@inheritDoc} */
    @Override
    public Vector3D[] closestPointTo(final Line line) {

        // Initialisations
        double distance = 0.0;
        Vector3D[] points = new Vector3D[2];
        double axisDistance = 0.0;
        boolean firstPass = true;
        boolean distIsOk = false;
        final Vector3D originLine = line.getOrigin();
        final Vector3D directionLine = line.getDirection();

        if (this.intersects(line)) {
            points[1] = this.getIntersectionPoints(line)[0];
            points[0] = points[1];
        } else {
            // The computation is made on each edge
            for (int i = 1; i < 5; i++) {

                Vector3D normal = Vector3D.crossProduct(directionLine, this.sideAxis[i]);

                // if the line is parallel to an edge, the distance is the one
                // to the origin point, that will be computed
                if (!(normal.getNorm() < VECTORSCOMPARISONEPS)) {

                    // Vector from the origin of the edge to the origin of the line
                    final Vector3D origLineToOrigEdge = originLine.subtract(this.origin);

                    // projection of this vector on the normed normal vector
                    normal = normal.normalize();
                    axisDistance = Vector3D.dotProduct(origLineToOrigEdge, normal);

                    // creation of the shortest vector from the edge to the line
                    normal = normal.scalarMultiply(axisDistance);

                    // computation of the points origin and origin + direction
                    // translated into the plane parallel to the separating middle plane
                    // and containing the input line.
                    final Vector3D translatedOrig = this.origin.add(normal);
                    final Vector3D translatedOrigPlusDir = this.origin.add(this.sideAxis[i]).add(normal);

                    // Computation of the distances of these points to the input line
                    final double distOrig = line.distance(translatedOrig);
                    final double distOrigPLusDir = line.distance(translatedOrigPlusDir);

                    final Vector3D signumVectorOrig = line.toSpace(line.toSubSpace(translatedOrig)).subtract(
                        translatedOrig);
                    final Vector3D signumVectorOrigplusDir = line.toSpace(line.toSubSpace(translatedOrigPlusDir))
                        .subtract(translatedOrigPlusDir);

                    final double dotProductOnLine = Vector3D.dotProduct(signumVectorOrig, signumVectorOrigplusDir);

                    // Thales theorem : computation of the point of the edge
                    // witch is the closest to the input line
                    final double alpha;
                    if (dotProductOnLine > 0.0) {
                        alpha = distOrig / (distOrig - distOrigPLusDir);
                    } else {
                        // if the lines are crossing between the translated origin and the
                        // translated origin + the direction
                        alpha = distOrig / (distOrig + distOrigPLusDir);
                    }

                    // this distance is Ok if the alpha projection is positive
                    distIsOk = alpha >= 0.0;
                }

                if ((axisDistance < distance || firstPass) && distIsOk) {
                    distance = axisDistance;
                    firstPass = false;
                    distIsOk = false;
                    final Line edge = new Line(this.origin, this.origin.add(this.sideAxis[i]));
                    points = edge.closestPointTo(line);
                }
            }

            // if no half axis contains a closest point to the line,
            // the closest point is the origin
            if (firstPass) {
                points[1] = this.origin;
                points[0] = line.toSpace(line.toSubSpace(points[1]));
            }
        }

        return points;
    }

    /**
     * Get a representation for this infinite rectangle cone.
     * The given parameters are in the same order as
     * in the constructor.
     * 
     * @return a representation for this infinite rectangle cone
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
        res.append(this.axis.toString());
        res.append(comma);
        // "U vector":
        res.append("U vector");
        res.append(this.u.toString());
        res.append(comma);
        // "Angle on U":
        res.append("Angle on U").append(open);
        res.append(this.angleU).append(close);
        res.append(comma);
        // "Angle on V":
        res.append("Angle on V").append(open);
        res.append(this.angleV).append(close);
        res.append(close);

        return res.toString();
    }
}
