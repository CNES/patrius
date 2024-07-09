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
 * @history created 04/03/2013
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:317:04/03/2015: STELA integration in CIRF with referential choice (ICRF, CIRF or MOD)
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::FA:829:25/01/2017:Protection of trigonometric methods
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.bodies;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.stela.forces.drag.StelaAeroModel;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Class representing the geodetic representation of a position.<br>
 * It is used to compute the spacecraft geodetic latitude when computing the atmospheric drag acceleration.
 * 
 * @concurrency immutable
 * 
 * @see StelaAeroModel
 * 
 * @author Vincent Ruch
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.3
 * 
 */
public final class GeodPosition implements Serializable {

    /** Serial UID. */
    private static final long serialVersionUID = 8116050377752754673L;

    /** Convergence threshold used in the geodetic latitude computation. */
    private static final double GEODETIC_THRESHOLD_1 = 1.0E-05;

    /** Convergence threshold used in the geodetic latitude computation. */
    private static final double GEODETIC_THRESHOLD_2 = 1.0E-06;
    
    /** Number of hours in a day */
    private static final int HOURS_IN_DAY = 24;

    /**
     * Flattening of the Earth.
     */
    private final double flattening;

    /**
     * Earth Radius.
     */
    private final double rEquatorial;

    /**
     * Constructor
     * 
     * @param rEq
     *        the Earth radius
     * @param f
     *        the Earth flattening
     */
    public GeodPosition(final double rEq, final double f) {
        this.rEquatorial = rEq;
        this.flattening = f;
    }

    /**
     * Compute geodetic altitude.
     * 
     * @param position
     *        the spacecraft position
     * @return the geodetic altitude (m)
     * @throws PatriusException
     *         Exception raised if : <br>
     *         <ul>
     *         <li>geocentric latitude is Infinite or NaN</li>
     *         <li>geodetic altitude cannot be compute</li>
     *         <li>the algorithm does not converge after the max authorised number of iterations</li>
     *         </ul>
     */
    public double getGeodeticAltitude(final Vector3D position) throws PatriusException {
        final double[] result = this.computeAltLat(position);
        return result[1];
    }

    /**
     * Compute geodetic latitude.
     * 
     * @param position
     *        the spacecraft position
     * @return the geodetic latitude (rad)
     * @throws PatriusException
     *         exception raised if : <br>
     *         <ul>
     *         <li>geocentric latitude is Infinite or NaN</li>
     *         <li>geodetic altitude cannot be compute</li>
     *         <li>the algorithm does not converge after the max authorised number of iterations</li>
     *         </ul>
     */
    public double getGeodeticLatitude(final Vector3D position) throws PatriusException {
        final double[] result = this.computeAltLat(position);
        return result[0];
    }

    /**
     * Compute the geodetic altitude and latitude.
     * 
     * @param position
     *        the spacecraft position
     * @return the geodetic altitude and latitude (m and rad).
     * @throws PatriusException thrown if computation failed
     */
    private double[] computeAltLat(final Vector3D position) throws PatriusException {
        // initialisations
        final double pvX = position.getX();
        final double pvY = position.getY();
        final double pvZ = position.getZ();
        final double r = MathLib.sqrt(pvX * pvX + pvY * pvY + pvZ * pvZ);
        final double geocLat;
        try {
            final double value = MathLib.divide(pvZ, r);
            geocLat = MathLib.asin(MathLib.min(1.0, MathLib.max(-1.0, value)));
        } catch (final ArithmeticException e) {
            // arithmetic exception
            throw new PatriusException(e, PatriusMessages.PDB_GEODETIC_PARAMETERS_COMPUTATION_FAILED);
        }

        if (Double.isInfinite(geocLat)) {
            // if r=0, an exception is thrown:
            throw new PatriusException(PatriusMessages.PDB_GEODETIC_PARAMETERS_COMPUTATION_FAILED);
        }
        // result[0] = latitude, result[1] = altitude
        final double[] result = new double[2];

        final double rpol = this.rEquatorial * (1.0 - this.flattening);

        // Latitude very close to 90 degrees.
        if (FastMath.PI / 2. - MathLib.abs(geocLat) < GEODETIC_THRESHOLD_2) {
            result[0] = geocLat;
            result[1] = r - rpol;
        } else if (MathLib.abs(geocLat) < GEODETIC_THRESHOLD_2) {
            // Latitude very close to 0 degrees.
            result[0] = geocLat;
            result[1] = r - this.rEquatorial;
        } else {
            // Other cases, an iteration is necessary.
            final double e2 = this.flattening * (2 - this.flattening);
            final double p = MathLib.sqrt(pvX * pvX + pvY * pvY);
            double c;
            double temp = geocLat;
            double sinTemp = MathLib.sin(temp);
            int i = 0;
            final int loopControler = 10;
            // Loop to compute latitude and altitude.
            while (i < loopControler) {

                try {
                    c = MathLib.divide(this.rEquatorial, MathLib.sqrt(MathLib.max(0., 1.0 - e2 * sinTemp * sinTemp)));
                    // Update latitude
                    result[0] = MathLib.atan(MathLib.divide(pvZ + c * e2 * sinTemp, p));
                    if (MathLib.abs(result[0] - temp) < GEODETIC_THRESHOLD_1) {
                        // Update altitude
                        result[1] = MathLib.divide(p, MathLib.cos(result[0])) - c;
                        if (Double.isInfinite(result[1])) {
                            // arithmetic exception
                            throw new PatriusException(PatriusMessages.PDB_GEODETIC_PARAMETERS_COMPUTATION_FAILED);
                        }
                        i = loopControler - 1;
                    } else {
                        temp = result[0];
                        sinTemp = MathLib.sin(temp);
                        if (i == loopControler - 1) {
                            // exception
                            throw new PatriusException(PatriusMessages.PDB_GEODETIC_PARAMETERS_COMPUTATION_FAILED);
                        }
                    }
                    i++;
                } catch (final ArithmeticException e) {
                    // arithmetic exception
                    throw new PatriusException(e, PatriusMessages.PDB_GEODETIC_PARAMETERS_COMPUTATION_FAILED);
                }
            }
        }
        return result;
    }

    /**
     * Compute the geodetic longitude at a given date.
     * 
     * @param position
     *        the spacecraft position
     * @param date
     *        the date
     * @return the geodetic longitude (rad)
     * @throws PatriusException
     *         error in the GMST computation
     */
    public double getGeodeticLongitude(final Vector3D position, final AbsoluteDate date) throws PatriusException {

        // compute thetaLST:
        final double thetaLST = MathLib.atan2(position.getY(), position.getX());
        // compute thetaGMST:
        final double thetaERA = EarthRotation.getERA(date);

        // longitude is in radians.
        return (thetaLST - thetaERA) % (2 * FastMath.PI);
    }

    /**
     * Compute the local solar time at a given date.
     * 
     * @param position
     *        the spacecraft position
     * @param positionSun
     *        the Sun position
     * @param date
     *        the date
     * @return the local solar time
     */
    public double getTloc(final Vector3D position, final Vector3D positionSun, final AbsoluteDate date) {

        // compute thetaLST:
        final double thetaLST = MathLib.atan2(position.getY(), position.getX());
        // compute thetaSun:
        final double thetaSun = MathLib.atan2(positionSun.getY(), positionSun.getX());

        // longitude is in radians.
        // local solar time is defined in [0;24[
        double tLoc = (FastMath.PI + thetaLST - thetaSun) * HOURS_IN_DAY / (2 * FastMath.PI);
        tLoc = (tLoc + HOURS_IN_DAY) % HOURS_IN_DAY;
        return tLoc;
    }
}
