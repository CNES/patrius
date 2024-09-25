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
 * @history Created on 09/11/2015
 *
 * HISTORY
 * VERSION:4.13:DM:DM-132:08/12/2023:[PATRIUS] Suppression de la possibilite
 * de convertir les sorties de VacuumSignalPropagation
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11:DM:DM-3232:22/05/2023:[PATRIUS] Detection d'extrema dans la classe ExtremaGenericDetector
 * VERSION:4.11:DM:DM-3217:22/05/2023:[PATRIUS] Modeles broadcast et almanach GNSS
 * VERSION:4.11:DM:DM-3197:22/05/2023:[PATRIUS] Deplacement dans PATRIUS de classes façade ALGO DV SIRUS 
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3165:10/05/2022:[PATRIUS] Amelioration de la propagation du signal 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:497:09/11/2015:Creation
 * VERSION::FA:564:31/03/2016: Issues related with GNSS almanac and PVCoordinatesPropagator
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::FA:1421:13/03/2018: Correction of GST epoch
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.orbits.pvcoordinates;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.CartesianParameters;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.KeplerianParameters;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * <p>
 * This class implements the PVCoordinatesProvider to compute position velocity of a GPS, Galileo or
 * BeiDou constellation satellite from its almanac/broadcast model parameters.
 * </p>
 *
 * @concurrency immutable
 *
 * @author fteilhard
 *
 *
 */
public class GNSSPVCoordinates implements PVCoordinatesProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = -394105238058559989L;

    /** Angle limit for detecting GEO orbits (in rad) */
    private static final double ANGLE_LIMIT_GEO = 0.3;

    /** Angle of 5° necessary to change from a user-defined inertial system to ITRF */
    private static final double CONVERTING_ANGLE_BEIDOU = -5. * MathLib.PI / 180.;

    /** Number of seconds in one week */
    private static final double WEEK_TO_SECONDS = 604800.;

    /** GNSS model parameters. */
    private final GNSSParameters gnssParams;

    /**
     * Date of the week. The date of the week is in relation with the reference epoch of the chosen
     * navigation system.
     */
    private final AbsoluteDate weekStartDate;

    /**
     * Creates an instance of AlmanacPVCoordinates.
     *
     * @param parameters
     *        Almanac parameters
     * @param weekStartDate
     *        the date of the beginning of the week
     * @since 3.1
     */
    public GNSSPVCoordinates(final GNSSParameters parameters, final AbsoluteDate weekStartDate) {
        this.gnssParams = parameters;
        // The date of the week must be in relation with the reference epoch of the chosen
        // navigation system: the number of weeks spent from the epoch date must be an integer.
        final double modulusWeekDateAndEpoch = MathLib.abs(weekStartDate.durationFrom(parameters.getGnssType()
                .getEpochDate()) % WEEK_TO_SECONDS);
        if (modulusWeekDateAndEpoch > Precision.DOUBLE_COMPARISON_EPSILON) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.WEEK_BEGINNING_DATE_INVALID,
                    parameters.getGnssType(), parameters.getGnssType().getEpochDate());
        }
        this.weekStartDate = weekStartDate;

    }

    /**
     * Geometric computation of the position to a date.
     * Computes a finite difference between position at date minus step and position at date plus
     * step.
     * If Input frame is null, results are expressed in WGS84.
     *
     * @param date
     *        Date to compute coordinates
     * @param frame
     *        Results expression frame.
     * @return position velocity coordinates at input date in frame
     * @throws PatriusException
     *         if input frame is different from WGS84 and configuration has not been defined
     */
    @Override
    public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {

        // Cartesian parameters
        final CartesianParameters cartesianParams = this.getCartesianParameters(date);

        // compute position in WGS84
        final Vector3D position = cartesianParams.getPosition();

        // compute velocity in WGS84
        final Vector3D velocity = cartesianParams.getVelocity();

        // coordinates in WGS84
        PVCoordinates gPVCoord = new PVCoordinates(position, velocity);

        // Convert coordinates in output frame
        if (frame != null && frame != FramesFactory.getITRF()) {
            final Transform wgs84ToOutputFrame = FramesFactory.getITRF().getTransformTo(frame, date);
            gPVCoord = wgs84ToOutputFrame.transformPVCoordinates(gPVCoord);
        }

        return gPVCoord;
    }

    /** {@inheritDoc} */
    @Override
    public Frame getNativeFrame(final AbsoluteDate date) throws PatriusException {
        return FramesFactory.getITRF();
    }

    /**
     * Compute the Cartesian parameters from the GNSS parameters in WGS84.
     *
     * @param date
     *        Position's date
     * @return Cartesian parameters at input date in WGS84
     * @throws PatriusException if UTC data can't be load
     */
    private CartesianParameters getCartesianParameters(final AbsoluteDate date) {

        // time gap between the time of applicability and the time elapsed from the week beginning
        // date to the input date
        final double timek = date.durationFrom(weekStartDate) - this.gnssParams.gettRef();

        // Computation of the spacecraft position
        // Eccentricity at reference time
        final double eccentricity = this.gnssParams.getEccentricity();
        // Eccentric anomaly
        final double eccentricAnomalyk = computeEccentricAnomaly(timek);
        // True anomaly
        final double vk = 2 * MathLib.atan(MathLib.sqrt((1 + eccentricity) / (1 - eccentricity))
                * MathLib.tan(eccentricAnomalyk / 2));
        // Argument of latitude
        final double phik = vk + this.gnssParams.getW();
        // Argument of lattitude correction (second harmonic perturbations)
        final double deltaUk = this.gnssParams.getCus() * MathLib.sin(2 * phik) + this.gnssParams.getCuc()
                * MathLib.cos(2 * phik);
        // Radial correction (second harmonic perturbations)
        final double deltaRk = this.gnssParams.getCrs() * MathLib.sin(2 * phik) + this.gnssParams.getCrc()
                * MathLib.cos(2 * phik);
        // Semi-major axis
        final double ak = this.gnssParams.getSqrtA() * this.gnssParams.getSqrtA() + this.gnssParams.getaRate() * timek;
        // Corrected argument of latitude
        final double uk = phik + deltaUk;
        // Corrected radius
        final double rk = ak * (1 - eccentricity * MathLib.cos(eccentricAnomalyk)) + deltaRk;
        // x Position in orbital plane
        final double xkPrime = rk * MathLib.cos(uk);
        // y Position in orbital plane
        final double ykPrime = rk * MathLib.sin(uk);

        // Computation of the spacecraft velocity
        // Computed mean motion
        final double n0 = MathLib.sqrt(this.gnssParams.getGnssType().getMu())
                / MathLib.pow(this.gnssParams.getSqrtA(), 3);
        // Corrected mean motion
        final double nCorrected = n0 + this.gnssParams.getDeltaN() + 1. / 2 * this.gnssParams.getDeltaNRate() * timek;

        // Eccentric anomaly rate
        final double eccentricAnomalykDot = nCorrected / (1 - eccentricity * MathLib.cos(eccentricAnomalyk));
        // True anomaly rate
        final double vkDot = eccentricAnomalykDot * MathLib.sqrt(1 - eccentricity * eccentricity)
                / (1 - eccentricity * MathLib.cos(eccentricAnomalyk));

        // Corrected argument of latitude rate
        final double ukDot = vkDot + 2 * vkDot
                * (this.gnssParams.getCus() * MathLib.cos(2 * phik) - this.gnssParams.getCuc() * MathLib.sin(2 * phik));
        // Corrected radius rate for CNAV (for other models, aRate=0 so the same equation can be
        // used)
        final double rkDot = this.gnssParams.getaRate() * (1 - eccentricity * MathLib.cos(eccentricAnomalyk)) + ak
                * eccentricity * MathLib.sin(eccentricAnomalyk) * eccentricAnomalykDot + 2
                * (this.gnssParams.getCrs() * MathLib.cos(2 * phik) - this.gnssParams.getCrc() * MathLib.sin(2 * phik))
                * vkDot;
        // In-plane x velocity
        final double xkPrimeDot = rkDot * MathLib.cos(uk) - rk * ukDot * MathLib.sin(uk);
        // In-plane y velocity
        final double ykPrimeDot = rkDot * MathLib.sin(uk) + rk * ukDot * MathLib.cos(uk);

        // // Build a PV object from the computed position and velocity
        final PVCoordinates pvCoords;
        // Particular case of the broadcast model for BeiDou satellites in GEO
        if ((this.gnssParams.getGnssType() == GNSSType.BeiDou)
                && (this.gnssParams instanceof LNAVGNSSParameters || this.gnssParams instanceof CNAVGNSSParameters)
                && (this.gnssParams.getI() < ANGLE_LIMIT_GEO)) {

            // Build a PV object from the computed position and velocity in case of a BeiDou
            // satellite in GEO
            pvCoords = computePVBeidouGEO(timek, phik, xkPrime, ykPrime, xkPrimeDot, ykPrimeDot, vkDot);
            // General case
        } else {
            // Build a PV object from the computed position and velocity in the general case
            pvCoords = computePVGeneralCase(timek, phik, xkPrime, ykPrime, xkPrimeDot, ykPrimeDot, vkDot);
        }
        return new CartesianParameters(pvCoords, this.gnssParams.getGnssType().getMu());
    }

    /**
     * @param timek current time - time of applicability
     * @param phik Argument of latitude
     * @param xkPrime x Position in orbital plane
     * @param ykPrime y Position in orbital plane
     * @param xkPrimeDot In-plane x velocity
     * @param ykPrimeDot In-plane y velocity
     * @param vkDot True anomaly rate
     * @return the PV coordinates computed from the input values for a BeiDou satellite in GEO
     */
    private PVCoordinates computePVBeidouGEO(final double timek, final double phik, final double xkPrime,
            final double ykPrime, final double xkPrimeDot, final double ykPrimeDot, final double vkDot) {
        // Earth rotation rate from the model according to the GNSS type
        final double earthRotationRate = this.gnssParams.getGnssType().getEarthRotationRate();
        // Time of applicability
        final double timeReferenceEphemeris = this.gnssParams.gettRef();
        // Corrected longitude of ascending node in inertial coordinate system
        final double omegak = this.gnssParams.getOmegaInit() + this.gnssParams.getOmegaRate() * timek
                - earthRotationRate * timeReferenceEphemeris;
        final double cosOmegak = MathLib.cos(omegak);
        final double sinOmegak = MathLib.sin(omegak);

        // Inclination correction (second harmonic perturbations)
        final double deltaIk = this.gnssParams.getCis() * MathLib.sin(2 * phik) + this.gnssParams.getCic()
                * MathLib.cos(2 * phik);
        // Corrected inclination
        final double ik = this.gnssParams.getI() + this.gnssParams.getiRate() * timek + deltaIk;
        final double cosIk = MathLib.cos(ik);
        final double sinIk = MathLib.sin(ik);

        // x component of spacecraft in inertial coordinate system
        final double xgk = xkPrime * cosOmegak - ykPrime * cosIk * sinOmegak;
        // y component of spacecraft in inertial coordinate system
        final double ygk = xkPrime * sinOmegak + ykPrime * cosIk * cosOmegak;
        // z component of spacecraft in inertial coordinate system
        final double zgk = ykPrime * sinIk;
        // Transition angle to convert inertial coordinates into ITRF
        final double alpha = earthRotationRate * timek;
        final double cosAlpha = MathLib.cos(alpha);
        final double sinAlpha = MathLib.sin(alpha);

        final double cosAngleBeidou = MathLib.cos(CONVERTING_ANGLE_BEIDOU);
        final double sinAngleBeidou = MathLib.sin(CONVERTING_ANGLE_BEIDOU);

        // Compute the earth-fixed coordinates
        // Earth-fixed x coordinate of spacecraft
        final double xk = cosAlpha * xgk + sinAlpha * cosAngleBeidou * ygk + sinAlpha * sinAngleBeidou * zgk;
        // Earth-fixed y coordinate of spacecraft
        final double yk = -sinAlpha * xgk + cosAlpha * cosAngleBeidou * ygk + cosAlpha * sinAngleBeidou * zgk;
        // Earth-fixed z coordinate of spacecraft
        final double zk = -sinAngleBeidou * ygk + cosAngleBeidou * zgk;

        // Longitude of ascending node rate
        final double omegakDot = this.gnssParams.getOmegaRate();
        // Time derivated alpha angle
        final double alphaDot = earthRotationRate;

        // Corrected inclination angle rate
        final double ikDot = this.gnssParams.getiRate() + 2 * vkDot
                * (this.gnssParams.getCis() * MathLib.cos(2 * phik) - this.gnssParams.getCic() * MathLib.sin(2 * phik));
        // Velocity computation in user-defined inertial frame
        // xgk velocity
        final double xgkDot = cosOmegak * xkPrimeDot - omegakDot * sinOmegak * xkPrime - cosIk * sinOmegak * ykPrimeDot
                + ikDot * sinIk * sinOmegak * ykPrime - cosIk * omegakDot * cosOmegak * ykPrime;
        // ygk velocity
        final double ygkDot = sinOmegak * xkPrimeDot + omegakDot * cosOmegak * xkPrime + cosIk * cosOmegak * ykPrimeDot
                - ikDot * sinIk * cosOmegak * ykPrime - cosIk * omegakDot * sinOmegak * ykPrime;
        // zgk velocity
        final double zgkDot = sinIk * ykPrimeDot + ykPrime * ikDot * cosIk;

        // Earth-fixed x velocity (m/s)
        final double xkDot = -alphaDot * sinAlpha * xgk + cosAlpha * xgkDot + alphaDot * cosAlpha * cosAngleBeidou
                * ygk + sinAlpha * cosAngleBeidou * ygkDot + alphaDot * cosAlpha * sinAngleBeidou * zgk + sinAlpha
                * sinAngleBeidou * zgkDot;
        // Earth-fixed y velocity (m/s)
        final double ykDot = -alphaDot * cosAlpha * xgk - sinAlpha * xgkDot - alphaDot * sinAlpha * cosAngleBeidou
                * ygk + cosAlpha * cosAngleBeidou * ygkDot - alphaDot * sinAlpha * sinAngleBeidou * zgk + cosAlpha
                * sinAngleBeidou * zgkDot;
        // Earth-fixed z velocity (m/s)
        final double zkDot = -ygkDot * sinAngleBeidou + zgkDot * cosAngleBeidou;

        return new PVCoordinates(new Vector3D(xk, yk, zk), new Vector3D(xkDot, ykDot, zkDot));
    }

    /**
     * @param timek current time - time of applicability
     * @param phik Argument of latitude
     * @param xkPrime x Position in orbital plane
     * @param ykPrime y Position in orbital plane
     * @param xkPrimeDot In-plane x velocity
     * @param ykPrimeDot In-plane y velocity
     * @param vkDot True anomaly rate
     * @return the PV coordinates computed from the input values for a BeiDou satellite in GEO
     */
    private PVCoordinates computePVGeneralCase(final double timek, final double phik, final double xkPrime,
            final double ykPrime, final double xkPrimeDot, final double ykPrimeDot, final double vkDot) {
        // Earth rotation rate from the model according to the GNSS type
        final double earthRotationRate = this.gnssParams.getGnssType().getEarthRotationRate();
        // Time of applicability
        final double timeReferenceEphemeris = this.gnssParams.gettRef();

        // Corrected longitude of ascending node in inertial coordinate system
        final double omegak = this.gnssParams.getOmegaInit() + (this.gnssParams.getOmegaRate() - earthRotationRate)
                * timek - earthRotationRate * timeReferenceEphemeris;
        final double cosOmegak = MathLib.cos(omegak);
        final double sinOmegak = MathLib.sin(omegak);

        // Inclination correction (second harmonic perturbations)
        final double deltaIk = this.gnssParams.getCis() * MathLib.sin(2 * phik) + this.gnssParams.getCic()
                * MathLib.cos(2 * phik);
        // Corrected inclination
        final double ik = this.gnssParams.getI() + this.gnssParams.getiRate() * timek + deltaIk;
        final double cosIk = MathLib.cos(ik);
        final double sinIk = MathLib.sin(ik);

        // Earth-fixed x coordinate of spacecraft
        final double xk = xkPrime * cosOmegak - ykPrime * cosIk * sinOmegak;
        // Earth-fixed y coordinate of spacecraft
        final double yk = xkPrime * sinOmegak + ykPrime * cosIk * cosOmegak;
        // Earth-fixed z coordinate of spacecraft
        final double zk = ykPrime * sinIk;

        // Longitude of ascending node rate
        final double omegakDot = this.gnssParams.getOmegaRate() - earthRotationRate;

        // Corrected inclination angle rate
        final double ikDot = this.gnssParams.getiRate() + 2 * vkDot
                * (this.gnssParams.getCis() * MathLib.cos(2 * phik) - this.gnssParams.getCic() * MathLib.sin(2 * phik));

        // Earth-fixed x velocity (m/s)
        final double xkDot = -xkPrime * omegakDot * sinOmegak + xkPrimeDot * cosOmegak - ykPrimeDot * sinOmegak * cosIk
                - ykPrime * (omegakDot * cosOmegak * cosIk - ikDot * sinOmegak * sinIk);
        // Earth-fixed y velocity (m/s)
        final double ykDot = xkPrime * omegakDot * cosOmegak + xkPrimeDot * sinOmegak + ykPrimeDot * cosOmegak * cosIk
                - ykPrime * (omegakDot * sinOmegak * cosIk + ikDot * cosOmegak * sinIk);
        // Earth-fixed z velocity (m/s)
        final double zkDot = ykPrimeDot * sinIk + ykPrime * ikDot * cosIk;

        // Build a PV object from the computed position and velocity
        return new PVCoordinates(new Vector3D(xk, yk, zk), new Vector3D(xkDot, ykDot, zkDot));
    }

    /**
     * Compute the correction term for the offset of the satellite's transmission time of signal
     * @param date
     *        Position's date
     * @return the clock correction
     */
    public double getClockCorrection(final AbsoluteDate date) {
        // time elapsed between the input date and the week beginning date
        final double systemTime = date.durationFrom(weekStartDate);
        // time of applicability of the input GNSS parameters
        final double referenceTime = this.gnssParams.gettRef();
        final double deltaTime = systemTime - referenceTime;
        final double relativisticCorrection = getRelativisticCorrection(deltaTime);
        final double af0 = this.gnssParams.getAf0();
        final double af1 = this.gnssParams.getAf1();
        final double af2 = this.gnssParams.getAf2();
        return af0 + af1 * deltaTime + af2 * deltaTime * deltaTime + relativisticCorrection;
    }

    /**
     * Compute the relativistic correction term for the satellite time correction
     * @param timek time gap between the time of applicability (tref) and the time of the sought
     *        position
     * @return the relativistic correction of the clock
     */
    public double getRelativisticCorrection(final double timek) {
        // F depends on the value of the constellation's mu.
        final double fValue = -2 * MathLib.sqrt(this.gnssParams.getGnssType().getMu())
                / MathLib.pow(Constants.SPEED_OF_LIGHT, 2);

        // Semi-major axis
        final double ak = this.gnssParams.getSqrtA() * this.gnssParams.getSqrtA() + this.gnssParams.getaRate() * timek;

        return fValue * gnssParams.getEccentricity() * MathLib.sqrt(ak) * MathLib.sin(computeEccentricAnomaly(timek));
    }

    /**
     * Compute the eccentric anomaly given by Kepler's equation. Calls a Patrius interative solver,
     * see {@link KeplerianParameters#solveKeplerEquationEccentricAnomaly(double, double)
     * solveKeplerEquationEccentricAnomaly} .
     * @param timek time gap between the time of applicability (tref) and the time of the sought
     *        position
     * @return the eccentricity anomaly
     */
    private double computeEccentricAnomaly(final double timek) {

        // Computed mean motion
        final double n0 = MathLib.sqrt(this.gnssParams.getGnssType().getMu())
                / MathLib.pow(this.gnssParams.getSqrtA(), 3);
        // Corrected mean motion
        final double nCorrected = n0 + this.gnssParams.getDeltaN() + 1. / 2 * this.gnssParams.getDeltaNRate() * timek;
        // Mean anomaly
        final double meanAnomalyk = this.gnssParams.getMeanAnomalyInit() + nCorrected * timek;
        // Eccentricity anomaly computation with a Kepler's equation iterative solver
        return KeplerianParameters.solveKeplerEquationEccentricAnomaly(meanAnomalyk, this.gnssParams.getEccentricity());
    }
}
