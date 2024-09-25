/**
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
 * HISTORY
 * VERSION:4.13:DM:DM-120:08/12/2023:[PATRIUS] Merge de la branche patrius-for-lotus dans Patrius
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;

/**
 * Compute X and Y Cardan angles of a point, defined by its Cartesian coordinates in a frame whose Y, Z axis, local
 * vertical and west
 * directions axis are in the same Plane.
 *
 * The X-angle is the angle from the "Local Vertical", in the (local vertical - west) plane. This angle is defined in [
 * {@code -PI} ; {@code PI}) and oriented trigowise.<br>
 * The Y-angle is the angle of rotation of the mounting around Y'. Y' is the image of West axis by the rotation of
 * X-angle around North axis. This angle is defined in [{@code -PI/2} ; {@code PI/2}] and oriented by Y'.
 *
 * @author amouroum, bonitt
 * 
 * @since 4.13
 */
public final class CardanCalculator {

    /**
     * Private constructor.
     * <p>
     * This class is a utility class, it should neither have a public nor a default constructor.<br>
     * This private constructor prevents the compiler from generating one automatically.
     * </p>
     */
    private CardanCalculator() {
    }

    /**
     * Compute the X-angle of a point given in cartesian coordinates in a local topocentric frame.<br>
     * The X-angle is defined in [{@code -PI} ; {@code PI}), and oriented trigowise.
     *
     * @param extTopo
     *        Point in cartesian coordinates which shall be transformed
     * @param frameOrientation
     *        Topocentric frame orientation around the local north and the X-axis (trigowise)
     * @return X-angle
     */
    public static double computeXangle(final Vector3D extTopo, final double frameOrientation) {
        // Compute the rotation and convert the vector
        final Rotation rotation = new Rotation(Vector3D.PLUS_K, -frameOrientation);
        final Vector3D extTopoConverted = rotation.applyInverseTo(extTopo);

        // Compute the X-angle
        return MathLib.atan2(-extTopoConverted.getY(), extTopoConverted.getZ());
    }

    /**
     * Get the X-angle derivative of a point wrt the local point (dX) expressed in the oriented topocentric frame.<br>
     * The angles are oriented trigowise.
     *
     * @param vectInTopoFrame
     *        Point in cartesian coordinates which shall be transformed
     * @param frameOrientation
     *        Topocentric frame orientation around the local north and the X-axis (trigowise)
     * @return X-angle derivative
     */
    public static Vector3D computeDXangle(final Vector3D vectInTopoFrame, final double frameOrientation) {
        // Compute the rotation and convert the vector
        final Rotation rotation = new Rotation(Vector3D.PLUS_K, -frameOrientation);
        final Vector3D extTopoConverted = rotation.applyInverseTo(vectInTopoFrame);

        // Data extraction and computation factorization
        final double y = extTopoConverted.getY();
        final double z = extTopoConverted.getZ();
        final double yzSquarredNorm = y * y + z * z;

        // Compute the derivative and its rotation
        final Vector3D derivative = new Vector3D(0, -z / yzSquarredNorm, y / yzSquarredNorm);
        return rotation.applyTo(derivative);
    }

    /**
     * Compute the X-angle rate of a point given in Cartesian coordinates in the local topocentric frame.<br>
     * The X-angle rate is oriented trigowise.
     *
     * @param extPVTopoOld
     *        Point in Cartesian coordinates which shall be transformed
     * @param frameOrientation
     *        Topocentric frame orientation around the local north and the X-axis (trigowise)
     * @return X-angle rate
     */
    public static double computeXangleRate(final PVCoordinates extPVTopoOld, final double frameOrientation) {
        // Compute the rotation and convert the vectors
        final Rotation rotation = new Rotation(Vector3D.PLUS_K, -frameOrientation);
        final Vector3D positionTransformed = rotation.applyInverseTo(extPVTopoOld.getPosition());
        final Vector3D velocityTransformed = rotation.applyInverseTo(extPVTopoOld.getVelocity());

        // Data extraction
        final double y = positionTransformed.getY();
        final double z = positionTransformed.getZ();
        final double ydot = velocityTransformed.getY();
        final double zdot = velocityTransformed.getZ();

        // Compute the X-angle rate
        return (zdot * y - ydot * z) / (y * y + z * z);
    }

    /**
     * Compute the Y-angle of a point given in Cartesian coordinates in the local topocentric frame.<br>
     * The Y-angle is defined in [{@code -PI/2} ; {@code PI/2}].
     *
     * @param extTopo
     *        Point in Cartesian coordinates which shall be transformed
     * @param frameOrientation
     *        Topocentric frame orientation around the local north and the X-axis (trigowise)
     * @return Y-angle
     */
    public static double computeYangle(final Vector3D extTopo, final double frameOrientation) {
        // Compute the rotation and convert the vector
        final Rotation rotation = new Rotation(Vector3D.PLUS_K, -frameOrientation);
        final Vector3D extTopoConverted = rotation.applyInverseTo(extTopo);

        // Compute the Y-angle
        return MathLib.asin(MathLib.min(1.0, MathLib.max(-1.0, extTopoConverted.normalize().getX())));
    }

    /**
     * Get the Y-angle derivative of a point wrt the local point (dY) expressed in the oriented topocentric frame.
     *
     * @param vectInTopoFrame
     *        point in cartesian coordinates which shall be transformed
     * @param frameOrientation
     *        Topocentric frame orientation around the local north and the X-axis (trigowise)
     * @return Y-angle derivative
     */
    public static Vector3D computeDYangle(final Vector3D vectInTopoFrame, final double frameOrientation) {

        // Compute the rotation and convert the vector
        final Rotation rotation = new Rotation(Vector3D.PLUS_K, -frameOrientation);
        final Vector3D extTopoConverted = rotation.applyInverseTo(vectInTopoFrame);

        // Data extraction and computation factorization
        final double x = extTopoConverted.getX();
        final double y = extTopoConverted.getY();
        final double z = extTopoConverted.getZ();

        final double yzSquarredNorm = y * y + z * z;
        final double yzNorm = MathLib.sqrt(yzSquarredNorm);
        final double xyzSquarredNorm = x * x + yzSquarredNorm;

        // Compute the derivative and its rotation
        final Vector3D derivative = new Vector3D(yzNorm / xyzSquarredNorm, -(x * y) / (yzNorm * xyzSquarredNorm),
            -(x * z) / (yzNorm * xyzSquarredNorm));
        return rotation.applyTo(derivative);
    }

    /**
     * Compute the Y-angle rate of a point given in Cartesian coordinates in the local topocentric frame.
     *
     * @param extPVTopoOld
     *        Point in Cartesian coordinates which shall be transformed
     * @param frameOrientation
     *        Topocentric frame orientation around the local north and the x-axis (trigowise)
     * @return Y-angle rate
     */
    public static double computeYangleRate(final PVCoordinates extPVTopoOld, final double frameOrientation) {
        // Compute the rotation and convert the vectors
        final Rotation rotation = new Rotation(Vector3D.PLUS_K, -frameOrientation);
        final Vector3D positionTransformed = rotation.applyInverseTo(extPVTopoOld.getPosition());
        final Vector3D velocityTransformed = rotation.applyInverseTo(extPVTopoOld.getVelocity());

        // Data extraction and computation factorization
        final double x = positionTransformed.getX();
        final double y = positionTransformed.getY();
        final double z = positionTransformed.getZ();

        final double xdot = velocityTransformed.getX();
        final double ydot = velocityTransformed.getY();
        final double zdot = velocityTransformed.getZ();

        final double yzSquarredNorm = y * y + z * z;

        // Compute the Y-angle rate
        return (y * (xdot * y - ydot * x) + z * (xdot * z - zdot * x))
                / ((x * x + yzSquarredNorm) * MathLib.sqrt(yzSquarredNorm));
    }

    /**
     * Compute the cartesian unit vector corresponding to the provided cardan angles.
     *
     * @param xAngle
     *        The cardan x-angle
     * @param yAngle
     *        The cardan y-angle
     * @param frameOrientation
     *        Topocentric frame orientation around the local north and the x-axis (trigowise)
     * @return the cartesian unit vector
     */
    public static Vector3D computeCartesianUnitPosition(final double xAngle, final double yAngle,
                                                        final double frameOrientation) {
        // Compute the sin/cos of the X/Y angles
        final double[] sincosX = MathLib.sinAndCos(xAngle);
        final double sinX = sincosX[0];
        final double cosX = sincosX[1];
        final double[] sincosY = MathLib.sinAndCos(yAngle);
        final double sinY = sincosY[0];
        final double cosY = sincosY[1];

        // Cartesian coordinates
        final double x = sinY;
        final double y = -cosY * sinX;
        final double z = cosY * cosX;

        // Intermediate result
        final Vector3D intermediatePoint = new Vector3D(x, y, z);

        // Compute the rotation and convert the vector
        final Rotation rotation = new Rotation(Vector3D.PLUS_K, frameOrientation);
        return rotation.applyInverseTo(intermediatePoint);
    }

    /**
     * Compute the cartesian unit vector corresponding to the provided cardan angles.
     *
     * @param xAngle
     *        The cardan x-angle
     * @param xAngleDot
     *        The cardan x-angle derivative
     * @param yAngle
     *        The cardan y-angle
     * @param yAngleDot
     *        The cardan y-angle derivative
     * @param frameOrientation
     *        Topocentric frame orientation around the local north and the x-axis (trigowise)
     * @return the cartesian unit vector
     */
    public static PVCoordinates computeCartesianUnitPV(final double xAngle, final double xAngleDot,
                                                       final double yAngle, final double yAngleDot,
                                                       final double frameOrientation) {
        // Compute the sin/cos of the X/Y angles
        final double[] sincosX = MathLib.sinAndCos(xAngle);
        final double sinX = sincosX[0];
        final double cosX = sincosX[1];
        final double[] sincosY = MathLib.sinAndCos(yAngle);
        final double sinY = sincosY[0];
        final double cosY = sincosY[1];

        // Cartesian coordinates
        final double x = sinY;
        final double y = -cosY * sinX;
        final double z = cosY * cosX;

        final double xDot = yAngleDot * cosY;
        final double yDot = yAngleDot * sinY * sinX - xAngleDot * cosY * cosX;
        final double zDot = -yAngleDot * sinY * cosX - xAngleDot * cosY * sinX;

        // Intermediate result
        final Vector3D intermediatePosition = new Vector3D(x, y, z);
        final Vector3D intermediateVelocity = new Vector3D(xDot, yDot, zDot);

        // Compute the rotation and convert the vectors
        final Rotation rotation = new Rotation(Vector3D.PLUS_K, frameOrientation);
        return new PVCoordinates(rotation.applyInverseTo(intermediatePosition),
            rotation.applyInverseTo(intermediateVelocity));
    }
}
