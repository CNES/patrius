/**
 *
 * Copyright 20121-2021 CNES
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
 * VERSION:4.13:DM:DM-120:08/12/2023:[PATRIUS] Merge de la branche patrius-for-lotus dans Patrius
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.7:FA:FA-2883:18/05/2021:Reliquats sur la DM 2871 sur le changement du sens des Azimuts 
 * VERSION:4.7:DM:DM-2767:18/05/2021:Evolutions et corrections diverses 
 * VERSION:4.6.1:DM:DM-2871:15/03/2021:Changement du sens des Azimuts (Annulation de SIRIUS-FT-2558)
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;

/**
 * Compute mathematical Azimuth and Elevation of a Point, defined by its Cartesian coordinates in a
 * Frame whose x, y axis and "Reference Azimuth" axis are in the same Plane.
 * Frame x axis is defined by its frameOrientation (counted trigowise, not clockwise !) from the
 * "Reference Azimuth", z axis points to the zenith and y axis completes the right-handed trihedra.
 *
 * The Point Azimuth is the angle (counted clockwise) from the "Reference Azymuth"
 * The Point Elevation is the angle (counted trigowise) from the x,y plane and the Point
 *
 * @author warrott
 *
 * @since 4.6.1
 */
public final class AzimuthElevationCalculator {

    /**
     * Private constructor.
     * <p>
     * This class is a utility class, it should neither have a public nor a default constructor. This private
     * constructor prevents the compiler from generating one automatically.
     * </p>
     */
    private AzimuthElevationCalculator() {
    }

    /**
     * Compute the Elevation of a point defined by its Cartesian coordinates in a
     * Frame whose x, y and "Reference Azimuth" axis are in the same Plane
     *
     * @param point
     *        Point Cartesian coordinates / Frame
     *
     * @return elevation
     *         The angle (trigowise, radian) between the x,y plane and the point
     */
    public static double getElevation(final Vector3D point) {
        return MathLib.asin(MathLib.min(1.0, MathLib.max(-1.0, point.normalize().getZ())));
    }

    /**
     * Compute the elevation derivative of a point defined by its Cartesian coordinates in a
     * Frame whose x, y and "Reference Azimuth" axis are in the same Plane.
     *
     * @param extTopo
     *        point in Cartesian coordinates which shall be transformed
     * @return the elevation derivative of a point given in Cartesian coordinates in the local
     *         topocentric frame
     */
    public static Vector3D computeDElevation(final Vector3D extTopo) {

        // Extract the vector components
        final double x = extTopo.getX();
        final double y = extTopo.getY();
        final double z = extTopo.getZ();

        // Computation optimization (factoring)
        final double x2 = x * x;
        final double y2 = y * y;
        final double z2 = z * z;
        final double x2PlusY2PlusZ2 = x2 + y2 + z2; // x2 + y2 + z2
        final double sqrtX2plusY2 = MathLib.sqrt(x2 + y2); // sqrt(x2 + y2)

        // Compute derivatives vector
        return new Vector3D(-(x * z) / (x2PlusY2PlusZ2 * sqrtX2plusY2), -(y * z)
                / (x2PlusY2PlusZ2 * sqrtX2plusY2),
            sqrtX2plusY2 / x2PlusY2PlusZ2);
    }

    /**
     * Compute the elevation rate of a point defined by its Cartesian coordinates in a
     * Frame whose x, y and "Reference Azimuth" axis are in the same Plane
     *
     * @param extPVTopo
     *        point in Cartesian coordinates which shall be transformed
     * @return elevation rate
     */
    public static double computeElevationRate(final PVCoordinates extPVTopo) {
        final Vector3D position = extPVTopo.getPosition();

        final Vector3D cross = Vector3D.crossProduct(position, extPVTopo.getVelocity());
        final double x = position.getX();
        final double y = position.getY();
        final double z = position.getZ();
        final double x2 = x * x;
        final double y2 = y * y;
        return ((-x * cross.getY()) + (y * cross.getX()))
                / (MathLib.sqrt(x2 + y2) * (x2 + y2 + (z * z)));
    }

    /**
     * Compute the Azimuth of a point defined by its Cartesian coordinates in a
     * Frame whose x, y and "Reference Azimuth" axis are in the same Plane
     *
     *
     * @param point
     *        Point Cartesian coordinates / Frame
     * @param frameOrientation
     *        Oriented (trigowise) angle (radian) between the "Reference Azimuth" and the Frame's x axis<br>
     *        Example :
     *        If "Reference Azimuth" is aligned with the local North of a local topocentric frame
     *        then a frameOrientation of -0.785 (=> -45°) means that the x axis of the Frame points to
     *        North-East
     *
     * @return azimuth
     *         The angle (clockwise, radian) between the "Reference Azimuth" and the point projected
     *         in x,y plane
     */
    public static double getAzimuth(final Vector3D point, final double frameOrientation) {
        double azimuth = -MathLib.atan2(point.getY(), point.getX()) - frameOrientation;

        if (azimuth < 0.) {
            azimuth += (MathLib.floor(-azimuth / MathUtils.TWO_PI) + 1) * MathUtils.TWO_PI;

        } else if (azimuth > MathUtils.TWO_PI) {
            azimuth -= MathLib.floor(azimuth / MathUtils.TWO_PI) * MathUtils.TWO_PI;
        }

        return azimuth;
    }

    /**
     * Compute the azimuth derivative of a point defined by its Cartesian coordinates in a Frame
     * whose x, y and "Reference Azimuth" axis are
     * in the same Plane.
     *
     * @param extTopo
     *        point in Cartesian coordinates which shall be transformed
     * @return the azimuth derivative of a point given in Cartesian coordinates in the local
     *         topocentric frame
     */
    public static Vector3D computeDAzimuth(final Vector3D extTopo) {

        // Extract the vector components
        final double x = extTopo.getX();
        final double y = extTopo.getY();

        // Computation optimization (factoring)
        final double x2PlusY2 = (x * x) + (y * y);

        // Compute derivatives vector
        return new Vector3D(y / x2PlusY2, -x / x2PlusY2, 0.);
    }

    /**
     * Compute the azimuth rate of a point defined by its Cartesian coordinates in a
     * Frame whose x, y and "Reference Azimuth" axis are in the same Plane.
     *
     * @param extPVTopo
     *        point in Cartesian coordinates which shall be transformed
     * @return azimuth rate
     */
    public static double computeAzimuthRate(final PVCoordinates extPVTopo) {
        final Vector3D position = extPVTopo.getPosition();
        final double x = position.getX();
        final double y = position.getY();
        return -Vector3D.crossProduct(position, extPVTopo.getVelocity()).getZ()
                / ((x * x) + (y * y));
    }

    /**
     * Compute the cartesian unit vector corresponding to the provided azimuth/elevation angles.
     *
     * @param azimuth
     *        The azimuth
     * @param elevation
     *        The elevation
     * @param frameOrientation
     *        Oriented (trigowise) angle between the "Reference Azimuth" and the Frame's x axis
     * @return the cartesian unit vector
     */
    public static Vector3D computeCartesianUnitPosition(final double azimuth, final double elevation,
                                                        final double frameOrientation) {
        // Compute sin/cos of the elevation angle
        final double[] sincosEl = MathLib.sinAndCos(elevation);
        final double sinEl = sincosEl[0];
        final double cosEl = sincosEl[1];

        // Compute sin/cos of the alpha angle
        final double alpha = azimuth + frameOrientation;
        final double[] sincosAlpha = MathLib.sinAndCos(alpha);
        final double sinAlpha = sincosAlpha[0];
        final double cosAlpha = sincosAlpha[1];

        // Cartesian coordinates
        final double x = cosEl * cosAlpha;
        final double y = -cosEl * sinAlpha;
        final double z = sinEl;
        // Return result
        return new Vector3D(x, y, z);
    }

    /**
     * Compute the cartesian unit vector corresponding to the provided azimuth/elevation angles and their derivatives.
     *
     * @param azimuth
     *        The azimuth
     * @param azimuthDot
     *        The azimuth derivative
     * @param elevation
     *        The elevation
     * @param elevationDot
     *        The elevation derivative
     * @param frameOrientation
     *        Oriented (trigowise) angle between the "Reference Azimuth" and the Frame's x axis
     * @return the cartesian unit vector
     */
    public static PVCoordinates computeCartesianUnitPV(final double azimuth, final double azimuthDot,
                                                       final double elevation,
                                                       final double elevationDot, final double frameOrientation) {
        // Compute sin/cos of the elevation angle
        final double[] sincosEl = MathLib.sinAndCos(elevation);
        final double sinEl = sincosEl[0];
        final double cosEl = sincosEl[1];

        // Compute sin/cos of the alpha angle
        final double alpha = azimuth + frameOrientation;
        final double[] sincosAlpha = MathLib.sinAndCos(alpha);
        final double sinAlpha = sincosAlpha[0];
        final double cosAlpha = sincosAlpha[1];

        // Cartesian coordinates
        final double x = cosEl * cosAlpha;
        final double y = -cosEl * sinAlpha;
        final double z = sinEl;

        final double xDot = -elevationDot * sinEl * cosAlpha - azimuthDot * sinAlpha * cosEl;
        final double yDot = elevationDot * sinEl * sinAlpha - azimuthDot * cosEl * cosAlpha;
        final double zDot = elevationDot * cosEl;
        // Return result
        return new PVCoordinates(x, y, z, xDot, yDot, zDot);
    }
}
