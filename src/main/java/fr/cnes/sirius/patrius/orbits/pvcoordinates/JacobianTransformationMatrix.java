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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:398:11/04/2018: Jacobian matrices of Cartesian <-> Spherical transformations
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.orbits.pvcoordinates;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Class to define jacobian transformation matrix
 *
 * @author mcapot
 *
 * @since 4.1
 */
public final class JacobianTransformationMatrix {

    /**
     * Private constructor.
     */
    private JacobianTransformationMatrix() {
        // Private constructor
    }

    /**
     * Get Jacobian for spheric coordinates transformation to cartesian
     * coordinates
     *
     * @param pvCoordinates
     *        cartesian coordinates
     * @return jacobian
     * @throws PatriusException
     *         the jacobian matrix cannot be computed because the position
     *         norm is negative
     */

    public static double[][] getJacobianCartesianToSpheric(final PVCoordinates pvCoordinates)
                                                                                             throws PatriusException {
        // Cartesian coordinates
        final Vector3D position = pvCoordinates.getPosition();

        // Position
        final double x = position.getX();
        final double y = position.getY();
        final double z = position.getZ();

        // Temporary variables
        final double d2 = x * x + y * y + z * z;
        final double d = MathLib.sqrt(d2);
        if (d < Precision.EPSILON || MathLib.abs(x) < Precision.EPSILON
            || MathLib.abs(y) < Precision.EPSILON) {
            // Exception if values are too small
            throw new PatriusException(PatriusMessages.JACOBIAN_UNDEFINED);
        }

        final double x2Py2 = x * x + y * y;
        final double sqrtx2Py2 = MathLib.sqrt(x2Py2);

        final double[][] result = new double[6][6];
        // Latitude : x y z vx vy vz
        result[0][0] = -x * z / (sqrtx2Py2 * d2);
        result[0][1] = -y * z / (sqrtx2Py2 * d2);
        result[0][2] = sqrtx2Py2 / d2;
        result[0][3] = 0;
        result[0][4] = 0;
        result[0][5] = 0;

        // Longitude : x y z vx vy vz
        result[1][0] = -y / x2Py2;
        result[1][1] = x / x2Py2;
        result[1][2] = 0;
        result[1][3] = 0;
        result[1][4] = 0;
        result[1][5] = 0;

        // Geocentric distance : x y z vx vy vz
        result[2][0] = x / d;
        result[2][1] = y / d;
        result[2][2] = z / d;
        result[2][3] = 0;
        result[2][4] = 0;
        result[2][5] = 0;

        // Velocity
        final Vector3D velocity = pvCoordinates.getVelocity();
        final double vx = velocity.getX();
        final double vy = velocity.getY();
        final double vz = velocity.getZ();

        final double vD = result[2][0] * vx + result[2][1] * vy + result[2][2] * vz;
        final double vLat = (vz - (z * vD / d)) / sqrtx2Py2;

        // V longitude : x y z vx vy vz
        result[4][0] = (vy - (2 * x * (x * vy - y * vx) / x2Py2)) / x2Py2;
        result[4][1] = -(vx + (2 * y * (x * vy - y * vx) / x2Py2)) / x2Py2;
        result[4][2] = 0;
        result[4][3] = result[1][0];
        result[4][4] = result[1][1];
        result[4][5] = 0;
        // V distance : x y z vx vy vz
        result[5][0] = vx / d - x * vD / d2;
        result[5][1] = vy / d - y * vD / d2;
        result[5][2] = vz / d - z * vD / d2;
        result[5][3] = x / d;
        result[5][4] = y / d;
        result[5][5] = z / d;
        // V longitude : x y z vx vy vz
        result[3][0] = (((z * vD * x) / (d2 * d)) - (z * result[5][0] / d)
            - (vLat * x / sqrtx2Py2))
            / sqrtx2Py2;
        result[3][1] = ((z * vD * y / (d2 * d)) - (z * result[5][1] / d) - (vLat * y / sqrtx2Py2))
            / sqrtx2Py2;
        result[3][2] = ((z * vD * z / (d2 * d)) - (z * result[5][2] / d) - (vD / d)) / sqrtx2Py2;
        result[3][3] = result[0][0];
        result[3][4] = result[0][1];
        result[3][5] = -((z * z / d2) - 1) / sqrtx2Py2;
        // Jacobian matrix
        //
        return result;
    }

    /**
     * Get Jacobian for cartesian coordinates transformation to spheric
     * coordinates. Be careful the third composant is the geocentric distance
     *
     * @param pvCoordinates
     *        spheric coordinates
     * @return jacobian
     * @throws PatriusException
     *         the latitude or longitude is out of range.
     */

    public static double[][] getJacobianSphericToCartesian(final PVCoordinates pvCoordinates)
                                                                                             throws PatriusException {
        // Geocentric position
        final Vector3D sphericPosition = pvCoordinates.getPosition();

        // Lat/Lng
        final double lat = sphericPosition.getX();
        final double lon = sphericPosition.getY();

        // Limit cases
        if (MathLib.abs(lat) > FastMath.PI / 2 || (lon < 0) || (lon > 2 * FastMath.PI)) {
            // Exception if values out of bounds
            //
            throw new PatriusException(PatriusMessages.PDB_ANGLE_OUTSIDE_INTERVAL);
        }

        final double d = sphericPosition.getZ();

        // Geocentric velocity
        final Vector3D sphericVelocity = pvCoordinates.getVelocity();
        final double velLat = sphericVelocity.getX();
        final double velLon = sphericVelocity.getY();
        final double velD = sphericVelocity.getZ();

        // Cos/Sin of lat/lng
        final double[] sincosLat = MathLib.sinAndCos(lat);
        final double sinLat = sincosLat[0];
        final double cosLat = sincosLat[1];
        final double[] sincosLon = MathLib.sinAndCos(lon);
        final double sinLon = sincosLon[0];
        final double cosLon = sincosLon[1];

        // Define parameters
        final double cosLatCLon = cosLat * cosLon;
        final double sLatCLon = sinLat * cosLon;
        final double cLatSLon = cosLat * sinLon;
        final double sLatSLon = sinLat * sinLon;

        // Jacobian computation
        final double[][] result = new double[6][6];
        // X : lat lon d velLat velLon velD
        result[0][0] = -d * sLatCLon;
        result[0][1] = -d * cLatSLon;
        result[0][2] = cosLatCLon;
        result[0][3] = 0;
        result[0][4] = 0;
        result[0][5] = 0;

        // Y : lat lon d velLat velLon velD
        result[1][0] = -d * sLatSLon;
        result[1][1] = d * cosLatCLon;
        result[1][2] = cLatSLon;
        result[1][3] = 0;
        result[1][4] = 0;
        result[1][5] = 0;

        // Z : lat lon d velLat velLon velD
        result[2][0] = d * cosLat;
        result[2][1] = 0;
        result[2][2] = sinLat;
        result[2][3] = 0;
        result[2][4] = 0;
        result[2][5] = 0;

        // velX : lat lon d velLat velLon velD
        result[3][0] = (-velD * sLatCLon) - (d * cosLatCLon * velLat) + (d * sLatSLon * velLon);
        result[3][1] = (-velD * cLatSLon) + (d * sLatSLon * velLat) - (d * cosLatCLon * velLon);
        result[3][2] = -((sLatCLon * velLat) + (cLatSLon * velLon));
        result[3][3] = -d * sLatCLon;
        result[3][4] = -d * cLatSLon;
        result[3][5] = cosLatCLon;

        // velY : lat lon d velLat velLon velD
        result[4][0] = (-velD * sLatSLon) - (d * cLatSLon * velLat) - (d * sLatCLon * velLon);
        result[4][1] = (velD * cosLatCLon) - (d * sLatCLon * velLat) - (d * cLatSLon * velLon);
        result[4][2] = -((sLatSLon * velLat) - (cosLatCLon * velLon));
        result[4][3] = -d * sLatSLon;
        result[4][4] = d * cosLatCLon;
        result[4][5] = cLatSLon;

        // velZ : lat lon d velLat velLon velD
        result[5][0] = (velD * cosLat) - (d * sinLat * velLat);
        result[5][1] = 0;
        result[5][2] = cosLat * velLat;
        result[5][3] = d * cosLat;
        result[5][4] = 0;
        result[5][5] = sinLat;

        // Return result
        //
        return result;
    }
}
