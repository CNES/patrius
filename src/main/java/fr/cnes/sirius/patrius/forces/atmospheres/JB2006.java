/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */

/*
 *
 * HISTORY
* VERSION:4.13:DM:DM-70:08/12/2023:[PATRIUS] Calcul de jacobienne dans OneAxisEllipsoid
 * VERSION:4.12.1:FA:FA-123:05/09/2023:[PATRIUS] Utilisation de getLLHCoordinates() au 
 *          lieu de getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC) 
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2445:27/05/2020:optimisation de SolarActivityReader 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2089:15/05/2019:[PATRIUS] passage a Java 8
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:268:30/04/2015:drag and lift implementation
 * VERSION::FA:576:22/03/2016:cache mechanism for density
 * VERSION::DM:907:29/03/2017:completion JB2006 javadoc
 * VERSION::FA:1179:01/09/2017:documentation PATRIUSv3.4
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.atmospheres;

import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.EllipsoidBodyShape;
import fr.cnes.sirius.patrius.bodies.EllipsoidPoint;
import fr.cnes.sirius.patrius.bodies.LLHCoordinatesSystem;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

//CHECKSTYLE: stop MagicNumber check
// Reason: model - Orekit code

/**
 * This is the realization of the Jacchia-Bowman 2006 atmospheric model.
 * <p>
 * It is described in the paper: <br>
 *
 * <a href="http://sol.spacenvironment.net/~JB2006/pubs/JB2006_AIAA-6166_model.pdf">A New Empirical Thermospheric
 * Density Model JB2006 Using New Solar Indices</a><br>
 *
 * <i>Bruce R. Bowman, W. Kent Tobiska and Frank A. Marcos</i> <br>
 *
 * AIAA 2006-6166<br>
 * </p>
 * <p>
 * Two computation methods are proposed to the user:
 * <ul>
 * <li>one OREKIT independent and compliant with initial FORTRAN routine entry values:
 * {@link #getDensity(double, double, double, double, double, double, double, double, double, double, 
 * double, double, double)}
 * </li>
 * <li>one compliant with OREKIT Atmosphere interface, necessary to the
 * {@link fr.cnes.sirius.patrius.forces.drag.DragForce drag force model} computation.</li>
 * </ul>
 * </p>
 * <p>
 * This model provides dense output for any position with altitude larger than 90km. Output data are :
 * <ul>
 * <li>Exospheric Temperature above Input Position (deg K)</li>
 * <li>Temperature at Input Position (deg K)</li>
 * <li>Total Mass-Density at Input Position (kg/m<sup>3</sup>)</li>
 * </ul>
 * </p>
 * <p>
 * The model needs geographical and time information to compute general values, but also needs space weather data : mean
 * and daily solar flux, retrieved threw different indices, and planetary geomagnetic indices. <br>
 * More information on these indices can be found on the <a
 * href="http://sol.spacenvironment.net/~JB2006/JB2006_index.html"> official JB2006 website.</a>
 * </p>
 *
 * <p>
 * This class is restricted to be used with {@link EllipsoidBodyShape}.
 * </p>
 *
 * @author Bruce R Bowman (HQ AFSPC, Space Analysis Division), Feb 2006: FORTRAN routine
 * @author Fabien Maussion (java translation)
 */
@SuppressWarnings("PMD.NullAssignment")
public class JB2006 implements Atmosphere {

    /** Serializable UID. */
    private static final long serialVersionUID = -4201270765122160831L;

    /** The alpha are the thermal diffusion coefficients in equation (6). */
    private static final double[] ALPHA = { 0, 0, 0, 0, 0, -0.38 };

    /** Natural logarithm of 10.0. */
    private static final double AL10 = 2.3025851;

    /** Molecular weights in order: N2, O2, O, Ar, He and H. */
    private static final double[] AMW = { 0, 28.0134, 31.9988, 15.9994, 39.9480, 4.0026, 1.00797 };

    /** Avogadro's number in mks units (molecules/kmol). */
    private static final double AVOGAD = 6.02257e26;

    /** Approximate value for 2 &pi;. */
    private static final double TWOPI = 6.2831853;

    /** Approximate value for &pi;. */
    private static final double PI = 3.1415927;

    /** Approximate value for &pi; / 2. */
    private static final double PIOV2 = 1.5707963;

    /** The FRAC are the assumed sea-level volume fractions in order: N2, O2, Ar, and He. */
    private static final double[] FRAC = { 0, 0.78110, 0.20955, 9.3400e-3, 1.2890e-5 };

    /** Universal gas-constant in mks units (joules/K/kmol). */
    private static final double RSTAR = 8314.32;

    /** Value used to establish height step sizes in the regime 90km to 105km. */
    private static final double R1 = 0.010;

    /** Value used to establish height step sizes in the regime 105km to 500km. */
    private static final double R2 = 0.025;

    /** Value used to establish height step sizes in the regime above 500km. */
    private static final double R3 = 0.075;

    /** Weights for the Newton-Cotes five-points quadrature formula. */
    private static final double[] WT = { 0, 0.311111111111111, 1.422222222222222,
        0.533333333333333, 1.422222222222222, 0.311111111111111 };

    /** Coefficients for high altitude density correction. */
    private static final double[] CHT = { 0, 0.22, -0.20e-02, 0.115e-02, -0.211e-05 };

    /** FZ global model values (1978-2004 fit). */
    private static final double[] FZM = { 0, 0.111613e+00, -0.159000e-02, 0.126190e-01,
        -0.100064e-01, -0.237509e-04, 0.260759e-04 };

    /** GT global model values (1978-2004 fit). */
    private static final double[] GTM = { 0, -0.833646e+00, -0.265450e+00, 0.467603e+00,
        -0.299906e+00, -0.105451e+00, -0.165537e-01, -0.380037e-01, -0.150991e-01,
        -0.541280e-01, 0.119554e-01, 0.437544e-02, -0.369016e-02, 0.206763e-02, -0.142888e-02,
        -0.867124e-05, 0.189032e-04, 0.156988e-03, 0.491286e-03, -0.391484e-04, -0.126854e-04,
        0.134078e-04, -0.614176e-05, 0.343423e-05 };

    /** XAMBAR relative data. */
    private static final double[] CXAMB = { 0, 28.15204, -8.5586e-2, +1.2840e-4, -1.0056e-5,
        -1.0210e-5, +1.5044e-6, +9.9826e-8 };

    /** DTSUB relative data. */
    private static final double[] BDT_SUB = { 0, -0.457512297e+01, -0.512114909e+01,
        -0.693003609e+02, 0.203716701e+03, 0.703316291e+03, -0.194349234e+04, 0.110651308e+04,
        -0.174378996e+03, 0.188594601e+04, -0.709371517e+04, 0.922454523e+04, -0.384508073e+04,
        -0.645841789e+01, 0.409703319e+02, -0.482006560e+03, 0.181870931e+04, -0.237389204e+04,
        0.996703815e+03, 0.361416936e+02 };

    /** DTSUB relative data. */
    private static final double[] CDT_SUB = { 0, -0.155986211e+02, -0.512114909e+01,
        -0.693003609e+02, 0.203716701e+03, 0.703316291e+03, -0.194349234e+04, 0.110651308e+04,
        -0.220835117e+03, 0.143256989e+04, -0.318481844e+04, 0.328981513e+04, -0.135332119e+04,
        0.199956489e+02, -0.127093998e+02, 0.212825156e+02, -0.275555432e+01, 0.110234982e+02,
        0.148881951e+03, -0.751640284e+03, 0.637876542e+03, 0.127093998e+02, -0.212825156e+02,
        0.275555432e+01 };

    /** Adiabatic constant. */
    private static final double GAMMA = 1.4;

    /**
     * Cache mecanism - Output temperatures.
     * <ul>
     * <li>TEMP(1): Exospheric Temperature above Input Position (deg K)</li>
     * <li>TEMP(2): Temperature at Input Position (deg K)</li>
     * </ul>
     */
    private final double[] cachedTemperature = new double[3];

    /** Sun position. */
    private final PVCoordinatesProvider sun;

    /** External data container. */
    private final JB2006InputParameters inputParams;

    /** Earth body shape. */
    private final EllipsoidBodyShape earth;

    /** Cache mecanism - Output density. */
    private double cachedDensity;

    /** Cache mecanism - Input date. */
    private AbsoluteDate cachedDate;

    /** Cache mecanism - Input frame. */
    private Frame cachedFrame;

    /** Cache mecanism - Input position. */
    private Vector3D cachedPosition;

    /**
     * Constructor with space environment information for internal computation.
     *
     * @param parameters
     *        the solar and magnetic activity data
     * @param sunIn
     *        the sun position
     * @param earthIn
     *        the earth body shape
     */
    public JB2006(final JB2006InputParameters parameters, final PVCoordinatesProvider sunIn,
                  final EllipsoidBodyShape earthIn) {

        this.earth = earthIn;
        this.sun = sunIn;
        this.inputParams = parameters;
        this.cachedDate = AbsoluteDate.PAST_INFINITY;
        this.cachedFrame = null;
        this.cachedPosition = Vector3D.ZERO;
        this.cachedDensity = Double.NaN;
    }

    /**
     * Get the local density with initial entries.
     *
     * @param dateMJD
     *        date and time, in modified julian days and fraction
     * @param sunRA
     *        Right Ascension of Sun (radians)
     * @param sunDecli
     *        Declination of Sun (radians)
     * @param satLon
     *        Right Ascension of position (radians)
     * @param satLat
     *        Geocentric latitude of position (radians)
     * @param satAlt
     *        Height of position (m)
     * @param f10
     *        10.7-cm Solar flux (1e<sup>-22</sup>*Watt/(m<sup>2</sup>*Hertz)). Tabular time 1.0 day earlier
     * @param f10B
     *        10.7-cm Solar Flux, averaged 81-day centered on the input time
     * @param ap
     *        Geomagnetic planetary 3-hour index A<sub>p</sub> for a tabular time 6.7 hours earlier
     * @param s10
     *        EUV index (26-34 nm) scaled to F10. Tabular time 1 day earlier.
     * @param s10B
     *        UV 81-day averaged centered index
     * @param xm10
     *        MG2 index scaled to F10
     * @param xm10B
     *        MG2 81-day ave. centered index. Tabular time 5.0 days earlier.
     * @return total mass-Density at input position (kg/m<sup>3</sup>)
     */
    // CHECKSTYLE: stop MethodLength check
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Orekit code kept as such
    public double getDensity(final double dateMJD, final double sunRA, final double sunDecli, final double satLon,
                       final double satLat, final double satAlt, final double f10, final double f10B, final double ap,
                       final double s10, final double s10B, final double xm10, final double xm10B) {

        // CHECKSTYLE: resume MethodLength check
        // CHECKSTYLE: resume CyclomaticComplexity check
        final double scaledSatAlt = satAlt / 1000.0;

        // Equation (14)
        final double tc = 379 + 3.353 * f10B + 0.358 * (f10 - f10B) + 2.094 * (s10 - s10B) + 0.343 * (xm10 - xm10B);

        // Equation (15)
        final double eta = 0.5 * MathLib.abs(satLat - sunDecli);
        final double theta = 0.5 * MathLib.abs(satLat + sunDecli);

        // Equation (16)
        final double h = satLon - sunRA;
        final double tau = h - 0.64577182 + 0.10471976 * MathLib.sin(h + 0.75049158);
        double solTimeHour = MathLib.toDegrees(h + PI) / 15.0;
        // solTimeHour must be between 0 and 24
        if (solTimeHour >= 24) {
            // solTimeHour is too high, it is reduced by 24
            solTimeHour = solTimeHour - 24.;
        }
        if (solTimeHour < 0) {
            // solTimeHour is too low, it is increased by 24
            solTimeHour = solTimeHour + 24.;
        }

        // Equation (17)
        final double c = MathLib.pow(MathLib.cos(eta), 2.5);
        final double s = MathLib.pow(MathLib.sin(theta), 2.5);
        final double tmp = MathLib.abs(MathLib.cos(0.5 * tau));
        final double df = s + (c - s) * tmp * tmp * tmp;
        final double tsubl = tc * (1. + 0.31 * df);

        // Equation (18)
        final double expap = MathLib.exp(-0.08 * ap);
        final double dtg = ap + 100. * (1. - expap);

        // Compute correction to dTc for local solar time and lat correction
        final double dtclst = dTc(f10, solTimeHour, satLat, scaledSatAlt);

        // Compute the local exospheric temperature.
        final double tinf = tsubl + dtg + dtclst;
        // Save computed result
        this.cachedTemperature[1] = tinf;

        // Equation (9)
        final double tsubx = 444.3807 + 0.02385 * tinf - 392.8292 * MathLib.exp(-0.0021357 * tinf);

        // Equation (11)
        final double gsubx = 0.054285714 * (tsubx - 183.);

        // The TC array will be an argument in the call to
        // XLOCAL, which evaluates Equation (10) or Equation (13)
        final double[] tcArray = new double[4];
        tcArray[0] = tsubx;
        tcArray[1] = gsubx;

        // A AND GSUBX/A OF Equation (13)
        tcArray[2] = (tinf - tsubx) / PIOV2;
        tcArray[3] = gsubx / tcArray[2];

        // Equation (5)
        final double z1 = 90.;
        final double z2 = MathLib.min(scaledSatAlt, 105.0);
        double al = MathLib.log(z2 / z1);
        int n = (int) MathLib.floor(al / R1) + 1;
        double zr = MathLib.exp(al / n);
        final double ambar1 = xAmbar(z1);
        final double tloc1 = xLocal(z1, tcArray);
        double zend = z1;
        // Intermediate sum used in equation (5)
        double sum2 = 0.;
        // Local variables to compute sum2
        double ain = ambar1 * xGrav(z1) / tloc1;
        double ambar2 = 0;
        double tloc2 = 0;
        double z = 0;
        double gravl = 0;

        for (int i = 1; i <= n; ++i) {
            z = zend;
            zend = zr * z;
            final double dz = 0.25 * (zend - z);
            double sum1 = WT[1] * ain;
            for (int j = 2; j <= 5; ++j) {
                z += dz;
                ambar2 = xAmbar(z);
                tloc2 = xLocal(z, tcArray);
                gravl = xGrav(z);
                ain = ambar2 * gravl / tloc2;
                sum1 += WT[j] * ain;
            }
            sum2 = sum2 + dz * sum1;
        }
        final double fact1 = 1000.0 / RSTAR;
        double rho = 3.46e-6 * ambar2 * tloc1 * MathLib.exp(-fact1 * sum2) / (ambar1 * tloc2);

        // Equation (2)
        final double anm = AVOGAD * rho;
        double an = anm / ambar2;

        // Equation (3)
        double fact2 = anm / 28.960;
        final double[] aln = new double[7];
        aln[1] = MathLib.log(FRAC[1] * fact2);
        aln[4] = MathLib.log(FRAC[3] * fact2);
        aln[5] = MathLib.log(FRAC[4] * fact2);

        // Equation (4)
        aln[2] = MathLib.log(fact2 * (1. + FRAC[2]) - an);
        aln[3] = MathLib.log(2. * (an - fact2));

        if (scaledSatAlt <= 105.0) {
            this.cachedTemperature[2] = tloc2;
            // Put in negligible hydrogen for use in DO-LOOP 13
            aln[6] = aln[5] - 25.0;
        } else {
            // Equation (6)
            final double z3 = MathLib.min(scaledSatAlt, 500.0);
            al = MathLib.log(z3 / z);
            n = (int) MathLib.floor(al / R2) + 1;
            zr = MathLib.exp(al / n);
            // Intermediate sum used in equation (6)
            sum2 = 0.;
            ain = gravl / tloc2;

            double tloc3 = 0;
            // Loop to compute sum2
            for (int i = 1; i <= n; ++i) {
                z = zend;
                zend = zr * z;
                final double dz = 0.25 * (zend - z);
                double sum1 = WT[1] * ain;
                for (int j = 2; j <= 5; ++j) {
                    z += dz;
                    tloc3 = xLocal(z, tcArray);
                    gravl = xGrav(z);
                    ain = gravl / tloc3;
                    sum1 = sum1 + WT[j] * ain;
                }
                sum2 = sum2 + dz * sum1;
            }

            final double z4 = MathLib.max(scaledSatAlt, 500.0);
            al = MathLib.log(z4 / z);
            // If scaledSatAlt <= 500 r = R2 else r = R3
            double r = R2;
            if (scaledSatAlt > 500.0) {
                r = R3;
            }
            n = (int) MathLib.floor(al / r) + 1;
            zr = MathLib.exp(al / n);
            double sum3 = 0.;
            double tloc4 = 0;
            for (int i = 1; i <= n; ++i) {
                z = zend;
                zend = zr * z;
                final double dz = 0.25 * (zend - z);
                double sum1 = WT[1] * ain;
                for (int j = 2; j <= 5; ++j) {
                    z += dz;
                    tloc4 = xLocal(z, tcArray);
                    gravl = xGrav(z);
                    ain = gravl / tloc4;
                    sum1 = sum1 + WT[j] * ain;
                }
                sum3 = sum3 + dz * sum1;
            }
            final double altg;
            final double hsign;
            if (scaledSatAlt <= 500.) {
                this.cachedTemperature[2] = tloc3;
                altg = MathLib.log(tloc3 / tloc2);
                fact2 = fact1 * sum2;
                hsign = 1.0;

            } else {
                this.cachedTemperature[2] = tloc4;
                altg = MathLib.log(tloc4 / tloc2);
                fact2 = fact1 * (sum2 + sum3);
                hsign = -1.0;
            }
            for (int i = 1; i <= 5; ++i) {
                aln[i] = aln[i] - (1.0 + ALPHA[i]) * altg - fact2 * AMW[i];
            }

            // Equation (7) - Note that in CIRA72, AL10T5 = DLOG10(T500)
            final double al10t5 = MathLib.log(tinf) / MathLib.log(10);
            final double alnh5 = (5.5 * al10t5 - 39.40) * al10t5 + 73.13;
            aln[6] = AL10 * (alnh5 + 6.) + hsign * (MathLib.log(tloc4 / tloc3) + fact1 * sum3 * AMW[6]);

        }

        // Equation (24) - J70 Seasonal-Latitudinal Variation
        final double capphi = (dateMJD - 36204.0) / 365.2422 % 1;
        final int signum = satLat >= 0 ? 1 : -1;
        final double sinLat = MathLib.sin(satLat);
        final double dlrsl = 0.02 * (scaledSatAlt - 90.) * MathLib.exp(-0.045 * (scaledSatAlt - 90.)) * signum
                * MathLib.sin(TWOPI * capphi + 1.72) * sinLat * sinLat;

        // Equation (23) - Computes the semiannual variation
        double dlrsa = 0;
        if (z < 2000.0) {
            final double d1950 = dateMJD - 33281.0;
            // Use new semiannual model DELTA LOG RHO
            dlrsa = semian(dayOfYear(d1950), scaledSatAlt, f10B);
        }

        // Sum the delta-log-rhos and apply to the number densities.
        // In CIRA72 the following equation contains an actual sum,
        // namely DLR = AL10 * (DLRGM + DLRSA + DLRSL)
        // However, for Jacchia 70, there is no DLRGM or DLRSA.
        final double dlr = AL10 * (dlrsl + dlrsa);
        for (int i = 1; i <= 6; ++i) {
            aln[i] += dlr;
        }

        // Compute mass-density and mean-molecular-weight and
        // convert number density logs from natural to common.

        double sumnm = 0.0;

        for (int i = 1; i <= 6; ++i) {
            an = MathLib.exp(aln[i]);
            sumnm += an * AMW[i];
        }

        rho = sumnm / AVOGAD;

        // Compute the high altitude exospheric density correction factor
        double fex = 1.0;
        // For altitude in [1000.0,1500.0[
        if (scaledSatAlt >= 1000.0 && scaledSatAlt < 1500.0) {
            final double zeta = (scaledSatAlt - 1000.) * 0.002;
            final double zeta2 = zeta * zeta;
            final double zeta3 = zeta * zeta2;
            final double f15c = CHT[1] + CHT[2] * f10B + CHT[3] * 1500.0 + CHT[4] * f10B * 1500.0;
            final double f15cZeta = (CHT[3] + CHT[4] * f10B) * 500.0;
            final double fex2 = 3.0 * f15c - f15cZeta - 3.0;
            final double fex3 = f15cZeta - 2.0 * f15c + 2.0;
            fex = 1.0 + fex2 * zeta2 + fex3 * zeta3;
        }
        // For altitude above 1500.0
        if (scaledSatAlt >= 1500.0) {
            fex = CHT[1] + CHT[2] * f10B + CHT[3] * scaledSatAlt + CHT[4] * f10B * scaledSatAlt;
        }

        // Apply the exospheric density correction factor.
        rho *= fex;

        // Return final result
        return rho;

    }

    /**
     * Compute daily temperature correction for Jacchia-Bowman model.
     *
     * @param f10
     *        solar flux index
     * @param solTimeHour
     *        local solar time (hours 0-23.999)
     * @param satLat
     *        sat lat (radians)
     * @param satAlt
     *        height (km)
     * @return dTc correction
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // CHECKSTYLE: stop CommentRatio check
    // CHECKSTYLE: stop MethodLength check
    // Reason: Orekit code kept as such
    private static double dTc(final double f10, final double solTimeHour, final double satLat, final double satAlt) {

        // CHECKSTYLE: resume MethodLength check
        // CHECKSTYLE: resume CyclomaticComplexity check
        // CHECKSTYLE: resume CommentRatio check
        double dtc = 0;
        final double tx = solTimeHour / 24.0;
        final double tx2 = tx * tx;
        final double tx3 = tx2 * tx;
        final double tx4 = tx3 * tx;
        final double tx5 = tx4 * tx;
        final double ycs = MathLib.cos(satLat);
        final double f = (f10 - 100.0) / 100.0;
        double h;
        double sum;

        // Calculates dTc
        if (satAlt >= 120 && satAlt <= 200) {
            final double dtc200 =
                CDT_SUB[17] + CDT_SUB[18] * tx * ycs + CDT_SUB[19] * tx2 * ycs + CDT_SUB[20]
                        * tx3 * ycs + CDT_SUB[21] * f * ycs + CDT_SUB[22] * tx * f * ycs
                        + CDT_SUB[23] * tx2 * f * ycs;
            sum =
                CDT_SUB[1] + BDT_SUB[2] * f + CDT_SUB[3] * tx * f + CDT_SUB[4] * tx2 * f
                        + CDT_SUB[5] * tx3 * f + CDT_SUB[6] * tx4 * f + CDT_SUB[7] * tx5 * f
                        + CDT_SUB[8] * tx * ycs + CDT_SUB[9] * tx2 * ycs + CDT_SUB[10] * tx3
                        * ycs + CDT_SUB[11] * tx4 * ycs + CDT_SUB[12] * tx5 * ycs + CDT_SUB[13]
                        * ycs + CDT_SUB[14] * f * ycs + CDT_SUB[15] * tx * f * ycs
                        + CDT_SUB[16] * tx2 * f * ycs;
            final double dtc200dz = sum;
            final double cc = 3.0 * dtc200 - dtc200dz;
            final double dd = dtc200 - cc;
            final double zp = (satAlt - 120.0) / 80.0;
            dtc = cc * zp * zp + dd * zp * zp * zp;
        }

        if (satAlt > 200.0 && satAlt <= 240.0) {
            h = (satAlt - 200.0) / 50.0;
            sum =
                CDT_SUB[1] * h + BDT_SUB[2] * f * h + CDT_SUB[3] * tx * f * h
                        + CDT_SUB[4] * tx2 * f * h + CDT_SUB[5] * tx3 * f * h
                        + CDT_SUB[6] * tx4 * f * h + CDT_SUB[7] * tx5 * f * h
                        + CDT_SUB[8] * tx * ycs * h + CDT_SUB[9] * tx2 * ycs * h
                        + CDT_SUB[10] * tx3 * ycs * h + CDT_SUB[11] * tx4 * ycs * h
                        + CDT_SUB[12] * tx5 * ycs * h + CDT_SUB[13] * ycs * h
                        + CDT_SUB[14] * f * ycs * h + CDT_SUB[15] * tx * f * ycs * h
                        + CDT_SUB[16] * tx2 * f * ycs * h + CDT_SUB[17]
                        + CDT_SUB[18] * tx * ycs + CDT_SUB[19] * tx2 * ycs
                        + CDT_SUB[20] * tx3 * ycs + CDT_SUB[21] * f * ycs
                        + CDT_SUB[22] * tx * f * ycs + CDT_SUB[23] * tx2 * f * ycs;
            dtc = sum;
        }

        if (satAlt > 240.0 && satAlt <= 300.0) {
            h = 40.0 / 50.0;
            sum =
                CDT_SUB[1] * h + BDT_SUB[2] * f * h + CDT_SUB[3] * tx * f * h
                        + CDT_SUB[4] * tx2 * f * h + CDT_SUB[5] * tx3 * f * h
                        + CDT_SUB[6] * tx4 * f * h + CDT_SUB[7] * tx5 * f * h
                        + CDT_SUB[8] * tx * ycs * h + CDT_SUB[9] * tx2 * ycs * h
                        + CDT_SUB[10] * tx3 * ycs * h + CDT_SUB[11] * tx4 * ycs * h
                        + CDT_SUB[12] * tx5 * ycs * h + CDT_SUB[13] * ycs * h
                        + CDT_SUB[14] * f * ycs * h + CDT_SUB[15] * tx * f * ycs * h
                        + CDT_SUB[16] * tx2 * f * ycs * h + CDT_SUB[17]
                        + CDT_SUB[18] * tx * ycs + CDT_SUB[19] * tx2 * ycs
                        + CDT_SUB[20] * tx3 * ycs + CDT_SUB[21] * f * ycs
                        + CDT_SUB[22] * tx * f * ycs + CDT_SUB[23] * tx2 * f * ycs;
            final double aa = sum;
            final double bb =
                CDT_SUB[1] + BDT_SUB[2] * f + CDT_SUB[3] * tx * f + CDT_SUB[4] * tx2 * f
                        + CDT_SUB[5] * tx3 * f + CDT_SUB[6] * tx4 * f + CDT_SUB[7] * tx5 * f
                        + CDT_SUB[8] * tx * ycs + CDT_SUB[9] * tx2 * ycs + CDT_SUB[10] * tx3
                        * ycs + CDT_SUB[11] * tx4 * ycs + CDT_SUB[12] * tx5 * ycs + CDT_SUB[13]
                        * ycs + CDT_SUB[14] * f * ycs + CDT_SUB[15] * tx * f * ycs
                        + CDT_SUB[16] * tx2 * f * ycs;
            h = 300.0 / 100.0;
            sum =
                BDT_SUB[1] + BDT_SUB[2] * f + BDT_SUB[3] * tx * f + BDT_SUB[4] * tx2 * f
                        + BDT_SUB[5] * tx3 * f + BDT_SUB[6] * tx4 * f + BDT_SUB[7] * tx5 * f
                        + BDT_SUB[8] * tx * ycs + BDT_SUB[9] * tx2 * ycs + BDT_SUB[10] * tx3
                        * ycs + BDT_SUB[11] * tx4 * ycs + BDT_SUB[12] * tx5 * ycs + BDT_SUB[13]
                        * h * ycs + BDT_SUB[14] * tx * h * ycs + BDT_SUB[15] * tx2 * h * ycs
                        + BDT_SUB[16] * tx3 * h * ycs + BDT_SUB[17] * tx4 * h * ycs
                        + BDT_SUB[18] * tx5 * h * ycs + BDT_SUB[19] * ycs;
            final double dtc300 = sum;
            sum =
                BDT_SUB[13] * ycs + BDT_SUB[14] * tx * ycs + BDT_SUB[15] * tx2 * ycs
                        + BDT_SUB[16] * tx3 * ycs + BDT_SUB[17] * tx4 * ycs + BDT_SUB[18] * tx5
                        * ycs;
            final double dtc300dz = sum;
            final double cc = 3.0 * dtc300 - dtc300dz - 3.0 * aa - 2.0 * bb;
            final double dd = dtc300 - aa - bb - cc;
            final double zp = (satAlt - 240.0) / 60.0;
            dtc = aa + bb * zp + cc * zp * zp + dd * zp * zp * zp;
        }

        if (satAlt > 300.0 && satAlt <= 600.0) {
            h = satAlt / 100.0;
            sum =
                BDT_SUB[1] + BDT_SUB[2] * f + BDT_SUB[3] * tx * f + BDT_SUB[4] * tx2 * f
                        + BDT_SUB[5] * tx3 * f + BDT_SUB[6] * tx4 * f + BDT_SUB[7] * tx5 * f
                        + BDT_SUB[8] * tx * ycs + BDT_SUB[9] * tx2 * ycs + BDT_SUB[10] * tx3
                        * ycs + BDT_SUB[11] * tx4 * ycs + BDT_SUB[12] * tx5 * ycs + BDT_SUB[13]
                        * h * ycs + BDT_SUB[14] * tx * h * ycs + BDT_SUB[15] * tx2 * h * ycs
                        + BDT_SUB[16] * tx3 * h * ycs + BDT_SUB[17] * tx4 * h * ycs
                        + BDT_SUB[18] * tx5 * h * ycs + BDT_SUB[19] * ycs;
            dtc = sum;
        }

        if (satAlt > 600.0 && satAlt <= 800.0) {
            final double zp = (satAlt - 600.0) / 100.0;
            final double hp = 600.0 / 100.0;
            final double aa =
                BDT_SUB[1] + BDT_SUB[2] * f + BDT_SUB[3] * tx * f + BDT_SUB[4] * tx2 * f
                        + BDT_SUB[5] * tx3 * f + BDT_SUB[6] * tx4 * f + BDT_SUB[7] * tx5 * f
                        + BDT_SUB[8] * tx * ycs + BDT_SUB[9] * tx2 * ycs + BDT_SUB[10] * tx3
                        * ycs + BDT_SUB[11] * tx4 * ycs + BDT_SUB[12] * tx5 * ycs + BDT_SUB[13]
                        * hp * ycs + BDT_SUB[14] * tx * hp * ycs + BDT_SUB[15] * tx2 * hp * ycs
                        + BDT_SUB[16] * tx3 * hp * ycs + BDT_SUB[17] * tx4 * hp * ycs
                        + BDT_SUB[18] * tx5 * hp * ycs + BDT_SUB[19] * ycs;
            final double bb =
                BDT_SUB[13] * ycs + BDT_SUB[14] * tx * ycs + BDT_SUB[15] * tx2 * ycs
                        + BDT_SUB[16] * tx3 * ycs + BDT_SUB[17] * tx4 * ycs + BDT_SUB[18] * tx5
                        * ycs;
            final double cc = -(3.0 * aa + 4.0 * bb) / 4.0;
            final double dd = (aa + bb) / 4.0;
            dtc = aa + bb * zp + cc * zp * zp + dd * zp * zp * zp;
        }

        return dtc;
    }

    /**
     * Evaluates Equation (1).
     *
     * @param z
     *        altitude
     * @return equation (1) value
     */
    private static double xAmbar(final double z) {

        final double dz = z - 100.;
        double amb = CXAMB[7];
        for (int i = 6; i >= 1; --i) {
            amb = dz * amb + CXAMB[i];
        }
        return amb;
    }

    /**
     * Evaluates Equation (10) or Equation (13), depending on Z.
     *
     * @param z
     *        altitude
     * @param tc
     *        tc array ???
     * @return equation (10) value
     */
    private static double xLocal(final double z, final double[] tc) {
        final double dz = z - 125;
        final double xLocal;
        if (dz <= 0) {
            xLocal = ((-9.8204695e-6 * dz - 7.3039742e-4) * dz * dz + 1.0) * dz * tc[1] + tc[0];
        } else {
            xLocal = tc[0] + tc[2] * MathLib.atan(tc[3] * dz * (1 + 4.5e-6 * MathLib.pow(dz, 2.5)));
        }
        return xLocal;
    }

    /**
     * Evaluates Equation (8) of gravity field.
     *
     * @param z
     *        altitude
     * @return the gravity field
     */
    private static double xGrav(final double z) {
        final double temp = 1.0 + z / 6356.766;
        return 9.80665 / (temp * temp);
    }

    /**
     * Compute semi-annual variation (delta log(rho)).
     *
     * @param day
     *        day of year
     * @param height
     *        height (km)
     * @param f10Bar
     *        average 81-day centered f10
     * @return semi-annual variation
     */
    private static double semian(final double day, final double height, final double f10Bar) {

        // Initialization
        final double f10Bar2 = f10Bar * f10Bar;
        final double htz = height / 1000.0;

        // SEMIANNUAL AMPLITUDE
        final double fzz =
            FZM[1] + FZM[2] * f10Bar + FZM[3] * f10Bar * htz + FZM[4] * f10Bar * htz * htz
                    + FZM[5] * f10Bar * f10Bar * htz + FZM[6] * f10Bar * f10Bar * htz * htz;

        // SEMIANNUAL PHASE FUNCTION
        final double tau = TWOPI * (day - 1.0) / 365;
        final double[] sincosTau = MathLib.sinAndCos(tau);
        final double[] sincos2Tau = MathLib.sinAndCos(2. * tau);
        final double[] sincos3Tau = MathLib.sinAndCos(3. * tau);
        final double[] sincos4Tau = MathLib.sinAndCos(4. * tau);
        final double sin1P = sincosTau[0];
        final double cos1P = sincosTau[1];
        final double sin2P = sincos2Tau[0];
        final double cos2P = sincos2Tau[1];
        final double sin3P = sincos3Tau[0];
        final double cos3P = sincos3Tau[1];
        final double sin4P = sincos4Tau[0];
        final double cos4P = sincos4Tau[1];
        final double gtz =
            GTM[1] + GTM[2] * sin1P + GTM[3] * cos1P + GTM[4] * sin2P + GTM[5] * cos2P
                    + GTM[6] * sin3P + GTM[7] * cos3P + GTM[8] * sin4P + GTM[9] * cos4P
                    + GTM[10] * f10Bar + GTM[11] * f10Bar * sin1P + GTM[12] * f10Bar * cos1P
                    + GTM[13] * f10Bar * sin2P + GTM[14] * f10Bar * cos2P
                    + GTM[15] * f10Bar * sin3P + GTM[16] * f10Bar * cos3P
                    + GTM[17] * f10Bar * sin4P + GTM[18] * f10Bar * cos4P + GTM[19] * f10Bar2
                    + GTM[20] * f10Bar2 * sin1P + GTM[21] * f10Bar2 * cos1P
                    + GTM[22] * f10Bar2 * sin2P + GTM[23] * f10Bar2 * cos2P;

        // Return result
        //
        return MathLib.max(1.0e-6, fzz) * gtz;

    }

    /**
     * Compute day of year.
     *
     * @param d1950
     *        (days since 1950)
     * @return the number days in year
     */
    private static double dayOfYear(final double d1950) {

        int iyday = (int) d1950;
        final double frac = d1950 - iyday;
        iyday = iyday + 364;

        int itemp = iyday / 1461;

        iyday = iyday - itemp * 1461;
        itemp = iyday / 365;
        if (itemp >= 3) {
            itemp = 3;
        }
        iyday = iyday - 365 * itemp + 1;
        return iyday + frac;
    }

    // OUTPUT:

    /**
     * Get the exospheric temperature above input position.
     * {@link #getDensity(double, double, double, double, double, double, double, double, double, double, double, 
     * double, double)}
     * <b>must</b> be called before calling this function.
     *
     * @return the exospheric temperature (deg K)
     */
    public double getExosphericTemp() {
        return this.cachedTemperature[1];
    }

    /**
     * Get the temperature at input position.
     * {@link #getDensity(double, double, double, double, double, double, double, double, double, double, double, 
     * double, double)}
     * <b>must</b> be called before calling this function.
     *
     * @return the local temperature (deg K)
     */
    public double getLocalTemp() {
        return this.cachedTemperature[2];
    }

    /**
     * Get the local density.
     *
     * @param date
     *        current date
     * @param position
     *        current position in frame
     * @param frame
     *        the frame in which is defined the position
     * @return local density (kg/m<sup>3</sup>)
     * @throws PatriusException
     *         if date is out of range of solar activity
     */
    @Override
    public double getDensity(final AbsoluteDate date, final Vector3D position, final Frame frame)
        throws PatriusException {
        computeTempDensity(date, position, frame);
        return this.cachedDensity;
    }

    /**
     * If input parameters are different from cached parameters, re compute cached density and temperature.
     *
     * @param date
     *        current date
     * @param position
     *        current position in frame
     * @param frame
     *        the frame in which is defined the position
     * @throws PatriusException
     *         if date is out of range of solar activity
     */
    private void computeTempDensity(final AbsoluteDate date, final Vector3D position, final Frame frame)
        throws PatriusException {
        if (this.cachedDate.compareTo(date) != 0 || position.distance(this.cachedPosition) != 0
                || !frame.equals(this.cachedFrame)) {
            // check if data are available
            if (date.compareTo(this.inputParams.getMaxDate()) > 0
                    || date.compareTo(this.inputParams.getMinDate()) < 0) {
                throw new PatriusException(PatriusMessages.NO_SOLAR_ACTIVITY_AT_DATE, date,
                    this.inputParams.getMinDate(),
                    this.inputParams.getMaxDate());
            }

            // compute modified julian days date
            final double dateMJD = date.durationFrom(AbsoluteDate.MODIFIED_JULIAN_EPOCH) / Constants.JULIAN_DAY;

            // compute geodetic position
            final EllipsoidPoint inBody = this.earth.buildPoint(position, frame, date, "satPoint");

            // compute sun position
            final EllipsoidPoint sunInBody = this.earth.buildPoint(
                this.sun.getPVCoordinates(date, frame).getPosition(), frame, date, "sunPoint");
            this.cachedDensity = this.getDensity(dateMJD,
                sunInBody.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getLongitude(),
                sunInBody.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getLatitude(),
                inBody.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getLongitude(),
                inBody.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getLatitude(),
                inBody.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getHeight(),
                this.inputParams.getF10(date), this.inputParams.getF10B(date), this.inputParams.getAp(date),
                this.inputParams.getS10(date), this.inputParams.getS10B(date), this.inputParams.getXM10(date),
                this.inputParams.getXM10B(date));

            // store input params used to compute these results in cache
            this.cachedDate = date;
            this.cachedPosition = position;
            this.cachedFrame = frame;
        }
    }

    /**
     * Get the inertial velocity of atmosphere molecules. Here the case is simplified : atmosphere is supposed to have a
     * null velocity in earth frame.
     *
     * @param date
     *        current date
     * @param position
     *        current position in frame
     * @param frame
     *        the frame in which is defined the position
     * @return velocity (m/s) (defined in the same frame as the position)
     * @throws PatriusException
     *         if some frame conversion cannot be performed
     */
    @Override
    public Vector3D getVelocity(final AbsoluteDate date, final Vector3D position, final Frame frame)
        throws PatriusException {
        final Transform bodyToFrame = this.earth.getBodyFrame().getTransformTo(frame, date);
        final Vector3D posInBody = bodyToFrame.getInverse().transformPosition(position);
        final PVCoordinates pvBody = new PVCoordinates(posInBody, new Vector3D(0, 0, 0));
        final PVCoordinates pvFrame = bodyToFrame.transformPVCoordinates(pvBody);
        return pvFrame.getVelocity();
    }

    /** {@inheritDoc} */
    @Override
    public double getSpeedOfSound(final AbsoluteDate date, final Vector3D position, final Frame frame)
        throws PatriusException {
        computeTempDensity(date, position, frame);
        return MathLib.sqrt(GAMMA * 287.058 * this.cachedTemperature[2]);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The following attributes are not deeply copied:
     * <ul>
     * <li>inputParams: {@link JB2006InputParameters}</li>
     * <li>sun: {@link PVCoordinatesProvider}</li>
     * <li>earth: {@link BodyShape}</li>
     * </ul>
     * </p>
     */
    @Override
    public Atmosphere copy() {
        return new JB2006(this.inputParams, this.sun, this.earth);
    }

    /** {@inheritDoc} */
    @Override
    public void checkSolarActivityData(final AbsoluteDate start, final AbsoluteDate end) throws PatriusException {
        this.inputParams.checkSolarActivityData(start, end);
    }

    // CHECKSTYLE: resume MagicNumber check
}
