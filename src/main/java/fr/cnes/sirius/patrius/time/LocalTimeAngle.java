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
 * @history creation 23/04/2015
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2324:27/05/2020:Ajout de la conversion LTAN => RAAN dans LocalTimeAngle 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:346:23/04/2015:creation of a local time class
 * VERSION::FA:680:27/09/2016:correction local time computation
 * VERSION::FA:902:13/12/2016:corrected anomaly on local time computation
 * VERSION::DM:710:22/03/2016:local time angle computation in [-PI, PI[
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.time;

import java.io.Serializable;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.TIRFProvider;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class provides methods to compute local time angle (true local time angle and mean local time angle).
 * <p>
 * The local time is represented by the angle between the projections of the Sun and the satellite in the equatorial
 * plane; therefore this angle is equal to zero when the local time is 12.00h and &Pi; when the local time is 0.00h
 * (Local Time In Hours = 12.00h + local time angle * 12 / &Pi;).
 * </p>
 * 
 * @concurrency not thread-safe
 * 
 * @author Emmanuel Bignon
 * 
 * @version $Id: LocalTimeAngle.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 3.0
 * 
 */
public class LocalTimeAngle implements Serializable {

     /** Serializable UID. */
    private static final long serialVersionUID = -7119451482370001739L;

    /** Sun ephemeris. */
    private final PVCoordinatesProvider sun;

    /**
     * Constructor
     * 
     * @param sunEphemeris
     *        Sun ephemeris
     */
    public LocalTimeAngle(final PVCoordinatesProvider sunEphemeris) {
        this.sun = sunEphemeris;
    }

    /**
     * Compute true local time angle in TIRF frame in the range [-&Pi;, &Pi;[.
     * 
     * @param date
     *        date
     * @param pos
     *        position in provided frame frame
     * @param frame
     *        frame in which position is defined
     * @return true local time angle in TIRF frame in rad (Local Time In Hours = 12.00h + local time angle * 12 / &Pi;)
     * @throws PatriusException
     *         thrown if Sun PV coordinates could not be retrieved
     */
    public double computeTrueLocalTimeAngle(final AbsoluteDate date, final Vector3D pos,
                                            final Frame frame) throws PatriusException {

        Vector3D posTIRF = pos;
        if (!frame.equals(FramesFactory.getTIRF())) {
            posTIRF = frame.getTransformTo(FramesFactory.getTIRF(), date).transformVector(pos);
        }

        // Compute satellite position
        final Vector3D posProj = new Vector3D(posTIRF.getX(), posTIRF.getY(), .0);

        // Compute Sun position
        final Vector3D sunPV = this.sun.getPVCoordinates(date, FramesFactory.getTIRF()).getPosition();
        final Vector3D sunPVproj = new Vector3D(sunPV.getX(), sunPV.getY(), .0);

        // Compute the angle between the sun and satellite projections over the equatorial plane
        double angle = Vector3D.angle(sunPVproj, posProj);

        if (sunPVproj.getX() * posProj.getY() - sunPVproj.getY() * posProj.getX() < 0) {
            // The "angle" function returns a value between 0 and PI, while we are working with angle between -PI and PI
            // when z-component of the cross product between the two vectors is negative, -angle is returned
            angle = -angle;
        }

        // Set angle in [-PI, PI[ if necessary
        angle = (angle >= FastMath.PI) ? angle - 2 * FastMath.PI : angle;

        return angle;
    }

    /**
     * Compute true local time angle in TIRF frame in the range [-&Pi;, &Pi;[.
     * 
     * @param orbit
     *        orbit
     * @return true local time in TIRF frame in rad (Local Time In Hours = 12.00h + local time angle * 12 / &Pi;)
     * @throws PatriusException
     *         thrown if Sun PV coordinates could not be retrieved
     */
    public double computeTrueLocalTimeAngle(final Orbit orbit) throws PatriusException {
        return this
            .computeTrueLocalTimeAngle(orbit.getDate(), orbit.getPVCoordinates().getPosition(), orbit.getFrame());
    }

    /**
     * Compute mean local time angle in TIRF frame in the range [-&Pi;, &Pi;[.
     * <p>
     * Mean local time is equal to true local time minus equation of time.
     * </p>
     * 
     * @param date
     *        date
     * @param pos
     *        position in provided frame
     * @param frame
     *        frame in which position is defined
     * @return mean local time in TIRF frame in rad (Local Time In Hours = 12.00h + local time angle * 12 / &Pi;)
     * @throws PatriusException
     *         thrown if true local time could not be computed
     */
    public double computeMeanLocalTimeAngle(final AbsoluteDate date, final Vector3D pos,
                                            final Frame frame) throws PatriusException {
        // Return mean local time (in s)
        return this.computeTrueLocalTimeAngle(date, pos, frame) + this.computeEquationOfTime(date)
            / Constants.RADIANS_TO_SEC;
    }

    /**
     * Compute mean local time angle in TIRF frame in the range [-&Pi;, &Pi;[.
     * <p>
     * Mean local time is equal to true local time minus equation of time.
     * </p>
     * 
     * @param orbit
     *        orbit
     * @return mean local time in TIRF frame in rad (Local Time In Hours = 12.00h + local time angle * 12 / &Pi;)
     * @throws PatriusException
     *         thrown if Sun PV coordinates could not be retrieved
     */
    public double computeMeanLocalTimeAngle(final Orbit orbit) throws PatriusException {
        return this.computeTrueLocalTimeAngle(orbit) + this.computeEquationOfTime(orbit.getDate())
            / Constants.RADIANS_TO_SEC;
    }

    /**
     * Compute equation of time in TIRF in the range [-43200s; 43200s].
     * 
     * @param date
     *        date
     * @return equation of time in TIRF frame (in s)
     * @throws PatriusException
     *         thrown if equation of time could not be computed
     */
    public double computeEquationOfTime(final AbsoluteDate date) throws PatriusException {

        final double halfDay = Constants.JULIAN_DAY / 2.;

        // Compute Greenwich true local time (in s)
        final Vector3D posGMST = Vector3D.PLUS_I;
        final double trueLocalTimeGMST =
            halfDay + this.computeTrueLocalTimeAngle(date, posGMST, FramesFactory.getTIRF())
                * Constants.RADIANS_TO_SEC;

        // Get seconds in day
        final double sec = date.getComponents(TimeScalesFactory.getUT1()).getTime().getSecondsInDay();

        // Compute time equation (in s) in the range ]-43200s; 43200s]
        double timeEquation = trueLocalTimeGMST - sec;
        if (timeEquation < -halfDay) {
            timeEquation += Constants.JULIAN_DAY;
        }
        if (timeEquation > halfDay) {
            timeEquation -= Constants.JULIAN_DAY;
        }

        return -timeEquation;
    }
    
    /**
     * Compute RAAN from true local time angle
     * @param date date
     * @param trueLocalTime true local time angle in rad
     * @param frame frame for which Earth equator should be considered. This frame should be pseudo-inertial
     * @return RAAN corresponding to provided local time
     * @throws PatriusException thrown if Sun position could not be retrieved in frame or frame is not pseudo-inertial
     */
    public double computeRAANFromTrueLocalTime(final AbsoluteDate date,
            final double trueLocalTime,
            final Frame frame) throws PatriusException {

        // Pseudo-inertial frame is required
        if (!frame.isPseudoInertial()) {
            throw new PatriusException(PatriusMessages.PDB_NOT_INERTIAL_FRAME);
        }

        // Position of Sun in inertial frame
        final Vector3D sunPV = this.sun.getPVCoordinates(date, frame).getPosition();
        // Sun right ascension in ECI frame
        final double sunLon = MathUtils.normalizeAngle(FastMath.atan2(sunPV.getY(), sunPV.getX()), FastMath.PI);
        // Compute and return RAAN
        return trueLocalTime + sunLon;
    }
    
    /**
     * Compute RAAN from mean local time angle.
     * @param date date
     * @param meanLocalTime mean local time angle in rad
     * @return RAAN corresponding to provided local time
     * @throws PatriusException thrown if sideral time could not be computed
     */
    public double computeRAANFromMeanLocalTime(final AbsoluteDate date,
            final double meanLocalTime) throws PatriusException {
        // Earth rotation angle
        final double sideralTime = TIRFProvider.getEarthRotationAngle(date);
        // Right ascension of "mean" Sun in G50
        final double secUT1 = date.getComponents(TimeScalesFactory.getUT1()).getTime().getSecondsInDay();
        final double smra = sideralTime + FastMath.PI - secUT1 / Constants.JULIAN_DAY * 2 * FastMath.PI;
        // Compute and return RAAN
        return meanLocalTime + smra;
    }
}
