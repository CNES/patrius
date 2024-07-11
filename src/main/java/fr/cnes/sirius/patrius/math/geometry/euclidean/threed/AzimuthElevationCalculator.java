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
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.7:FA:FA-2883:18/05/2021:Reliquats sur la DM 2871 sur le changement du sens des Azimuts 
 * VERSION:4.7:DM:DM-2767:18/05/2021:Evolutions et corrections diverses 
 * VERSION:4.6.1:DM:DM-2871:15/03/2021:Changement du sens des Azimuts (Annulation de SIRIUS-FT-2558)
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

import java.io.Serializable;

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
public class AzimuthElevationCalculator implements Serializable {

    /** Serial UID. */
    private static final long serialVersionUID = 3034184740810982255L;

    /**
     * Oriented (trigowise) angle (radian) between the "Reference Azimuth" and the Frame's x axis
     *
     * Example :
     * If "Reference Azimuth" is aligned with the local North of a local topocentric frame
     * then a frameOrientation of -0.785 (=> -45°) means that the x axis of the Frame points to
     * North-East
     */
    private final double frameOrientation;

    /**
     * Constructor
     *
     * @param frameOrientation
     *        Oriented angle (trigowise, radian) between the "Reference Azimuth" and the Frame's x
     *        axis
     *
     *        Example :
     *        If "Reference Azimuth" is aligned with the local North of a local topocentric frame
     *        then a frameOrientation of -0.785 (=> -45°) means that the x axis of the Frame points
     *        to North-East
     */
    public AzimuthElevationCalculator(final double frameOrientation) {
        super();
        this.frameOrientation = frameOrientation;
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
    public double getElevation(final Vector3D point) {
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
    public Vector3D computeDElevation(final Vector3D extTopo) {

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
                / (x2PlusY2PlusZ2 * sqrtX2plusY2), sqrtX2plusY2 / x2PlusY2PlusZ2);
    }

    /**
     * Compute the elevation rate of a point defined by its Cartesian coordinates in a
     * Frame whose x, y and "Reference Azimuth" axis are in the same Plane
     *
     * @param extPVTopo
     *        point in Cartesian coordinates which shall be transformed
     * @return elevation rate
     */
    public double computeElevationRate(final PVCoordinates extPVTopo) {
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
     *
     * @return azimuth
     *         The angle (clockwise, radian) between the "Reference Azimuth" and the point projected
     *         in x,y plane
     */
    public double getAzimuth(final Vector3D point) {
        double azimuth = -MathLib.atan2(point.getY(), point.getX()) - this.frameOrientation;

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
    public Vector3D computeDAzimuth(final Vector3D extTopo) {

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
    public double computeAzimuthRate(final PVCoordinates extPVTopo) {
        final Vector3D position = extPVTopo.getPosition();
        final double x = position.getX();
        final double y = position.getY();
        return -Vector3D.crossProduct(position, extPVTopo.getVelocity()).getZ()
                / ((x * x) + (y * y));
    }

    /**
     * Returns the frame orientation.
     *
     * @return the frame orientation
     */
    public double getFrameOrientation() {
        return this.frameOrientation;
    }
}
