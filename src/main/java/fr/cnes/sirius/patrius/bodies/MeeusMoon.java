/**
 * 
 * Copyright 2011-2022 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * 
 * @history created 02/08/12
 *
 * HISTORY
 * VERSION:4.9:DM:DM-3149:10/05/2022:[PATRIUS] Optimisation des reperes interplanetaires 
 * VERSION:4.9:DM:DM-3159:10/05/2022:[PATRIUS] Implementation de l'interface GeometricBodyShape par CelestialBody
 * VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3165:10/05/2022:[PATRIUS] Amelioration de la propagation du signal 
 * VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2703:18/05/2021: Acces facilite aux donnees d'environnement (application interplanetaire) 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:591:05/04/2016:add IAU pole data
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import fr.cnes.sirius.patrius.bodies.JPLEphemeridesLoader.EphemerisType;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * This class implements the Moon ephemerides according to the algorithm of Meeus. It only provides the position. Note
 * that it is not possible to build this Moon from the CelestialBodyFactory.
 * </p>
 * See "Astronomical Algorithms", chapter 45 "Position of the Moon", Jean Meeus, 1991.
 * 
 * <p>
 * Note that pole information allowing to define inertially-centered frame and rotating frame are defined in
 * {@link IAUPoleFactory} since Meeus model does not provide the information.
 * </p>
 * 
 * @concurrency immutable
 * 
 * @see CelestialBody
 * 
 * @author Julie Anton
 * 
 * @version $Id: MeeusMoon.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.1
 * 
 */
public class MeeusMoon extends AbstractCelestialBody {

    /**
     * IUD
     */
    private static final long serialVersionUID = 755708934677699377L;

    /** Arguments of each sine for longitude computation (linear combinaison of angles : D, M, Mp, F, A1, A2, Lp). */
    private static final double[][] LON_COEFF_SIN = { { 0, 0, 1, 0, 0, 0, 0 }, { 2, 0, -1, 0, 0, 0, 0 },
        { 2, 0, 0, 0, 0, 0, 0 }, { 0, 0, 2, 0, 0, 0, 0 }, { 0, 1, 0, 0, 0, 0, 0 }, { 0, 0, 0, 2, 0, 0, 0 },
        { 2, 0, -2, 0, 0, 0, 0 }, { 2, -1, -1, 0, 0, 0, 0 }, { 2, 0, 1, 0, 0, 0, 0 }, { 2, -1, 0, 0, 0, 0, 0 },
        { 0, 1, -1, 0, 0, 0, 0 }, { 1, 0, 0, 0, 0, 0, 0 }, { 0, 1, 1, 0, 0, 0, 0 }, { 2, 0, 0, -2, 0, 0, 0 },
        { 0, 0, 1, 2, 0, 0, 0 }, { 0, 0, 1, -2, 0, 0, 0 }, { 4, 0, -1, 0, 0, 0, 0 }, { 0, 0, 3, 0, 0, 0, 0 },
        { 4, 0, -2, 0, 0, 0, 0 }, { 2, 1, -1, 0, 0, 0, 0 }, { 2, 1, 0, 0, 0, 0, 0 }, { 1, 0, -1, 0, 0, 0, 0 },
        { 1, 1, 0, 0, 0, 0, 0 }, { 2, -1, 1, 0, 0, 0, 0 }, { 2, 0, 2, 0, 0, 0, 0 }, { 0, 0, 0, 0, 1, 0, 0 },
        { 4, 0, 0, 0, 0, 0, 0 }, { 2, 0, -3, 0, 0, 0, 0 }, { 0, 1, -2, 0, 0, 0, 0 }, { 2, 0, -1, 2, 0, 0, 0 },
        { 2, -1, -2, 0, 0, 0, 0 }, { 1, 0, 1, 0, 0, 0, 0 }, { 2, -2, 0, 0, 0, 0, 0 }, { 0, 1, 2, 0, 0, 0, 0 },
        { 0, 2, 0, 0, 0, 0, 0 }, { 2, -2, -1, 0, 0, 0, 0 }, { 0, 0, 0, -1, 0, 0, 1, 0 }, { 2, 0, 1, -2, 0, 0, 0 },
        { 2, 0, 0, 2, 0, 0, 0 }, { 4, -1, -1, 0, 0, 0, 0 }, { 0, 0, 2, 2, 0, 0, 0 }, { 3, 0, -1, 0, 0, 0, 0 },
        { 2, 1, 1, 0, 0, 0, 0 }, { 4, -1, -2, 0, 0, 0, 0 }, { 0, 2, -1, 0, 0, 0, 0 }, { 2, 2, -1, 0, 0, 0, 0 },
        { 2, 1, -2, 0, 0, 0, 0 }, { 2, -1, 0, -2, 0, 0, 0 }, { 4, 0, 1, 0, 0, 0, 0 }, { 0, 0, 4, 0, 0, 0, 0 },
        { 4, -1, 0, 0, 0, 0, 0 }, { 1, 0, -2, 0, 0, 0, 0 }, { 2, 1, 0, -2, 0, 0, 0 }, { 0, 0, 2, -2, 0, 0, 0 },
        { 1, 1, 1, 0, 0, 0, 0 }, { 3, 0, -2, 0, 0, 0, 0 }, { 4, 0, -3, 0, 0, 0, 0 }, { 2, -1, 2, 0, 0, 0, 0 },
        { 0, 2, 1, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 1, 0 }, { 1, 1, -1, 0, 0, 0, 0 }, { 2, 0, 3, 0, 0, 0, 0 } };

    /** Arguments of each sine for latitude computation (linear combinaison of angles : D, M, Mp, F, A1, A3, Lp). */
    private static final double[][] LAT_COEFF_SIN = { { 0, 0, 0, 1, 0, 0, 0 }, { 0, 0, 1, 1, 0, 0, 0 },
        { 0, 0, 1, -1, 0, 0, 0 }, { 2, 0, 0, -1, 0, 0, 0 }, { 2, 0, -1, 1, 0, 0, 0 }, { 2, 0, -1, -1, 0, 0, 0 },
        { 2, 0, 0, 1, 0, 0, 0 }, { 0, 0, 2, 1, 0, 0, 0 }, { 2, 0, 1, -1, 0, 0, 0 }, { 0, 0, 2, -1, 0, 0, 0 },
        { 2, -1, 0, -1, 0, 0, 0 }, { 2, 0, -2, -1, 0, 0, 0 }, { 2, 0, 1, 1, 0, 0, 0 }, { 2, 1, 0, -1, 0, 0, 0 },
        { 2, -1, -1, 1, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0, 1 }, { 2, -1, 0, 1, 0, 0, 0 }, { 2, -1, -1, -1, 0, 0, 0 },
        { 0, 1, -1, -1, 0, 0, 0 }, { 4, 0, -1, -1, 0, 0, 0 }, { 0, 1, 0, 1, 0, 0, 0 }, { 0, 0, 0, 3, 0, 0, 0 },
        { 0, 1, -1, 1, 0, 0, 0 }, { 1, 0, 0, 1, 0, 0, 0 }, { 0, 1, 1, 1, 0, 0, 0 }, { 0, 1, 1, -1, 0, 0, 0 },
        { 0, 1, 0, -1, 0, 0, 0 }, { 1, 0, 0, -1, 0, 0, 0 }, { 0, 0, 3, 1, 0, 0, 0 }, { 4, 0, 0, -1, 0, 0, 0 },
        { 4, 0, -1, 1, 0, 0, 0 }, { 0, 0, 1, -3, 0, 0, 0 }, { 4, 0, -2, 1, 0, 0, 0 }, { 2, 0, 0, -3, 0, 0, 0 },
        { 2, 0, 2, -1, 0, 0, 0 }, { 2, -1, 1, -1, 0, 0, 0 }, { 2, 0, -2, 1, 0, 0, 0 }, { 0, 0, 3, -1, 0, 0, 0 },
        { 2, 0, 2, 1, 0, 0, 0 }, { 2, 0, -3, -1, 0, 0, 0 }, { 0, 0, 0, 0, 0, 1, 0 }, { 2, 1, -1, 1, 0, 0, 0 },
        { 2, 1, 0, 1, 0, 0, 0 }, { 4, 0, 0, 1, 0, 0, 0 }, { 2, -1, 1, 1, 0, 0, 0 }, { 2, -2, 0, -1, 0, 0, 0 },
        { 0, 0, 1, 3, 0, 0, 0 }, { 2, 1, 1, -1, 0, 0, 0 }, { 1, 1, 0, -1, 0, 0, 0 }, { 1, 1, 0, 1, 0, 0, 0 },
        { 0, 1, -2, -1, 0, 0, 0 }, { 2, 1, -1, -1, 0, 0, 0 }, { 1, 0, 1, 1, 0, 0, 0 }, { 2, -1, -2, -1, 0, 0, 0 },
        { 0, 1, 2, 1, 0, 0, 0 }, { 4, 0, -2, -1, 0, 0, 0 }, { 0, 0, 0, -1, 1, 0, 0 }, { 0, 0, 0, 1, 1, 0, 0 },
        { 4, -1, -1, -1, 0, 0, 0 }, { 1, 0, 1, -1, 0, 0, 0 }, { 4, 0, 1, -1, 0, 0, 0 }, { 0, 0, -1, 0, 0, 0, 1 },
        { 1, 0, -1, -1, 0, 0, 0 }, { 0, 0, 1, 0, 0, 0, 1 }, { 4, -1, 0, -1, 0, 0, 0 }, { 2, -2, 0, 1, 0, 0, 0 } };

    /** Magnitude of each sine for the computation of the longitude (unit : 0.0000001 deg). */
    private static final double[] SIN_LON = { 6288774, 1274027, 658314, 213618, -185116, -114332, 58793, 57066, 53322,
        45758, -40923, -34720, -30383, 15327, -12528, 10980, 10675, 10034, 8548, -7888, -6766, -5163, 4987, 4036,
        3994, 3958, 3861, 3665, -2689, -2602, 2390, -2348, 2236, -2120, -2069, 2048, 1962, -1773, -1595, 1215,
        -1110, -892, -810, 759, -713, -700, 691, 596, 549, 537, 520, -487, -399, -381, 351, -340, 330, 327, -323,
        318, 299, 294 };

    /** Magnitude of each sine for the computation of the latitude (unit : 0.0000001 deg). */
    private static final double[] SIN_LAT = { 5128122, 280602, 277693, 173273, 55413, 46271, 32573, 17198, 9266, 8822,
        8216, 4324, 4200, -3359, 2463, -2235, 2211, 2065, -1870, 1828, -1794, -1749, -1565, -1491, -1475, -1410,
        -1344, -1335, 1107, 1021, 833, 777, 671, 607, 596, 491, -451, 439, 422, 421, 382, -366, -351, 331, 315,
        302, -283, -229, 223, 223, -220, -220, -185, 181, -177, 176, 175, 175, 166, -164, 132, 127, -119, -115,
        115, 107 };

    /** Magnitude of each cosine for the computation of the longitude (unit : 0.0000001 deg). */
    private static final double[] COS_LON = { -20905355, -3699111, -2955968, -569925, 246158, -204586, -170733,
        -152138, -129620, 108743, 104755, 79661, 48888, -34782, 30824, 24208, -23210, -21636, -16675, 14403,
        -12831, -11650, -10445, 10321, 10056, -9884, 8752, -8379, -7003, 6322, 5751, -4950, -4421, 4130, -3958,
        3258, -3149, 2616, 2354, -2117, -1897, -1739, -1571, -1423, 1165, -1117 };

    /** Arguments of each cosine for longitude computation (linear combinaison of angles : D, M, Mp, F). */
    private static final double[][] LON_COEFF_COS = { { 0, 0, 1, 0 }, { 2, 0, -1, 0 }, { 2, 0, 0, 0 }, { 0, 0, 2, 0 },
        { 2, 0, -2, 0 }, { 2, -1, 0, 0 }, { 2, 0, 1, 0 }, { 2, -1, -1, 0 }, { 0, 1, -1, 0 }, { 1, 0, 0, 0 },
        { 0, 1, 1, 0 }, { 0, 0, 1, -2 }, { 0, 1, 0, 0 }, { 4, 0, -1, 0 }, { 2, 1, 0, 0 }, { 2, 1, -1, 0 },
        { 0, 0, 3, 0 }, { 4, 0, -2, 0 }, { 1, 1, 0, 0 }, { 2, 0, -3, 0 }, { 2, -1, 1, 0 }, { 4, 0, 0, 0 },
        { 2, 0, 2, 0 }, { 2, 0, 0, -2 }, { 2, -1, -2, 0 }, { 2, -2, 0, 0 }, { 2, 0, -1, -2 }, { 1, 0, -1, 0 },
        { 0, 1, -2, 0 }, { 1, 0, 1, 0 }, { 0, 1, 2, 0 }, { 2, -2, -1, 0 }, { 0, 0, 2, -2 }, { 2, 0, 1, -2 },
        { 4, -1, -1, 0 }, { 3, 0, -1, 0 }, { 0, 0, 0, 2 }, { 2, 1, 1, 0 }, { 2, 2, -1, 0 }, { 0, 2, -1, 0 },
        { 4, -1, -2, 0 }, { 1, 0, -2, 0 }, { 4, -1, 0, 0 }, { 4, 0, 1, 0 }, { 0, 2, 1, 0 }, { 0, 0, 4, 0 } };

    /** Constant for the expression of the Moon's mean longitude. */
    private static final double LP_0 = 218.3164591;
    /** Constant for the expression of the Moon's mean longitude. */
    private static final double LP_1 = 481267.88134236;
    /** Constant for the expression of the Moon's mean longitude. */
    private static final double LP_2 = -0.0013268;
    /** Constant for the expression of the Moon's mean longitude. */
    private static final double LP_3 = 538841.;
    /** Constant for the expression of the Moon's mean longitude. */
    private static final double LP_4 = -65194000.;
    /** Constant for the expression of the mean elongation of the Moon. */
    private static final double D_0 = 297.8502042;
    /** Constant for the expression of the mean elongation of the Moon. */
    private static final double D_1 = 445267.1115168;
    /** Constant for the expression of the mean elongation of the Moon. */
    private static final double D_2 = -0.00163;
    /** Constant for the expression of the mean elongation of the Moon. */
    private static final double D_3 = 545868.;
    /** Constant for the expression of the mean elongation of the Moon. */
    private static final double D_4 = -113065000.;
    /** Constant for the expression of the Sun's mean anomaly. */
    private static final double M_0 = 357.5291092;
    /** Constant for the expression of the Sun's mean anomaly. */
    private static final double M_1 = 35999.0502909;
    /** Constant for the expression of the Sun's mean anomaly. */
    private static final double M_2 = -0.0001536;
    /** Constant for the expression of the Sun's mean anomaly. */
    private static final double M_3 = 24490000.;
    /** Constant for the expression of the Moon's mean anomaly. */
    private static final double MP_0 = 134.9634114;
    /** Constant for the expression of the Moon's mean anomaly. */
    private static final double MP_1 = 477198.8676313;
    /** Constant for the expression of the Moon's mean anomaly. */
    private static final double MP_2 = 0.0089970;
    /** Constant for the expression of the Moon's mean anomaly. */
    private static final double MP_3 = 69699.;
    /** Constant for the expression of the Moon's mean anomaly. */
    private static final double MP_4 = -14712000.;
    /**
     * Constant for the expression of the Moon's argument of latitude (mean distance of the Moon from its ascending
     * node).
     */
    private static final double F_0 = 93.2720993;
    /**
     * Constant for the expression of the Moon's argument of latitude (mean distance of the Moon from its ascending
     * node).
     */
    private static final double F_1 = 483202.0175273;
    /**
     * Constant for the expression of the Moon's argument of latitude (mean distance of the Moon from its ascending
     * node).
     */
    private static final double F_2 = -0.0034029;
    /**
     * Constant for the expression of the Moon's argument of latitude (mean distance of the Moon from its ascending
     * node).
     */
    private static final double F_3 = -3526000.;
    /**
     * Constant for the expression of the Moon's argument of latitude (mean distance of the Moon from its ascending
     * node).
     */
    private static final double F_4 = 863310000.;
    /** Constant for the expression that describes the action of Venus. */
    private static final double A1_0 = 119.75;
    /** Constant for the expression that describes the action of Venus. */
    private static final double A1_1 = 131.849;
    /** Constant for the expression that describes the action of Jupiter. */
    private static final double A2_0 = 53.09;
    /** Constant for the expression that describes the action of Jupiter. */
    private static final double A2_1 = 479264.290;
    /** Constant for an additional expression. */
    private static final double A3_0 = 313.45;
    /** Constant for an additional expression. */
    private static final double A3_1 = 481266.484;
    /** Constant for the eccentricity of the Earth's orbit around the Sun. */
    private static final double E_0 = 1.0;
    /** Constant for the eccentricity of the Earth's orbit around the Sun. */
    private static final double E_1 = -0.002516;
    /** Constant for the eccentricity of the Earth's orbit around the Sun. */
    private static final double E_2 = -0.0000074;
    /** Constant for the expression of the distance from the Earth's center to the Moon (m). */
    private static final double R = 385000560.;
    /** Constant for the Moon's equatorial radius (m). */
    private static final double MOON_RADIUS = 1737400.;
    /** 0.0000001 deg. */
    private static final double EPS = 1e-6 * MathUtils.DEG_TO_RAD;
    /** Max number of terms for longitude computation. */
    private static final int MAX_LONG = 62;
    /** Max number of terms for latitude computation. */
    private static final int MAX_LAT = 66;
    /** Max number of terms for distance computation. */
    private static final int MAX_DIST = 46;
    /** Number of terms for longitude computation. */
    private final int longitudeTerms;
    /** Number of terms for latitude computation. */
    private final int latitudeTerms;
    /** Number of terms for distance computation. */
    private final int distanceTerms;

    /**
     * Simple constructor.
     * 
     * @throws PatriusException
     *         if data embedded in the library cannot be read
     */
    public MeeusMoon() throws PatriusException {
        this(MAX_LONG, MAX_LAT, MAX_DIST);
    }

    /**
     * Simple constructor.
     * 
     * @param numberOfLongitudeTerms
     *        number of terms taken into account to compute the longitude (up to 62)
     * @param numberOfLatitudeTerms
     *        number of terms taken into account to compute the latitude (up to 66)
     * @param numberOfDistanceTerms
     *        number of terms taken into account to compute the distance (up to 46)
     * @throws PatriusException
     *         if data embedded in the library cannot be read
     */
    public MeeusMoon(final int numberOfLongitudeTerms, final int numberOfLatitudeTerms,
                     final int numberOfDistanceTerms) throws PatriusException {
        // EOD frame (the Meeus formulation of the Sun ephemerides is given in this frame)
        super("Meeus Moon", Constants.JPL_SSD_MOON_GM, IAUPoleFactory.getIAUPole(EphemerisType.MOON),
                FramesFactory.getEODFrame(true));
        this.longitudeTerms = numberOfLongitudeTerms;
        this.latitudeTerms = numberOfLatitudeTerms;
        this.distanceTerms = numberOfDistanceTerms;
        this.setShape(new ExtendedOneAxisEllipsoid(MOON_RADIUS, 0., this.getTrueRotatingFrame(), "Moon"));
    }

    /** {@inheritDoc} */
    @Override
    public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {

        // time measured in Julian centuries of 36525 ephemeris days from the epoch J2000
        final double t = date.offsetFrom(AbsoluteDate.J2000_EPOCH, TimeScalesFactory.getTT())
                / (Constants.JULIAN_CENTURY);
        final double t2 = t * t;
        final double t3 = t2 * t;
        final double t4 = t2 * t2;

        // Moon's mean longitude
        double lp = LP_0 + LP_1 * t + LP_2 * t2 + MathLib.divide(1., LP_3) * t3 + MathLib.divide(1., LP_4) * t4;
        lp = MathUtils.normalizeAngle(MathLib.toRadians(lp), FastMath.PI);
        // Mean elongation of the Moon
        double d = D_0 + D_1 * t + D_2 * t2 + MathLib.divide(1., D_3) * t3 + MathLib.divide(1., D_4) * t4;
        d = MathUtils.normalizeAngle(MathLib.toRadians(d), FastMath.PI);
        // Sun's mean anomaly
        double m = M_0 + M_1 * t + M_2 * t2 + MathLib.divide(1., M_3) * t3;
        m = MathUtils.normalizeAngle(MathLib.toRadians(m), FastMath.PI);
        // Moon's mean anomaly
        double mp = MP_0 + MP_1 * t + MP_2 * t2 + MathLib.divide(1., MP_3) * t3 + MathLib.divide(1., MP_4) * t4;
        mp = MathUtils.normalizeAngle(MathLib.toRadians(mp), FastMath.PI);
        // Moon's argument of latitude (mean distance of the Moon from its ascending node)
        double f = F_0 + F_1 * t + F_2 * t2 + MathLib.divide(1., F_3) * t3 + MathLib.divide(1., F_4) * t4;
        f = MathUtils.normalizeAngle(MathLib.toRadians(f), FastMath.PI);

        // further arguments
        double a1 = A1_0 + A1_1 * t;
        a1 = MathUtils.normalizeAngle(MathLib.toRadians(a1), FastMath.PI);
        double a2 = A2_0 + A2_1 * t;
        a2 = MathUtils.normalizeAngle(MathLib.toRadians(a2), FastMath.PI);
        double a3 = A3_0 + A3_1 * t;
        a3 = MathUtils.normalizeAngle(MathLib.toRadians(a3), FastMath.PI);

        // eccentricity of the Earth (this term is used to correct harmonics that contains M or 2M)
        final double e = E_0 + E_1 * t + E_2 * t2;
        final double e2 = e * e;

        final double[] argSinL = { d, m, mp, f, a1, a2, lp };
        final double[] argSinB = { d, m, mp, f, a1, a3, lp };
        final double[] argCos = { d, m, mp, f };

        // sum longitude
        final double sumL = this.sumSinComputation(LON_COEFF_SIN, argSinL, SIN_LON, e, e2, this.longitudeTerms);
        // sum latitude
        final double sumB = this.sumSinComputation(LAT_COEFF_SIN, argSinB, SIN_LAT, e, e2, this.latitudeTerms);
        // sum distance
        final double sumR = this.sumCosComputation(LON_COEFF_COS, argCos, COS_LON, e, e2, this.distanceTerms);

        // longitude
        final double lon = lp + sumL;
        // latitude
        final double lat = sumB;
        // distance between the center of the Earth and the Moon
        final double r = R + sumR;

        // Moon's position in EOD frame
        final double[] sincosLat = MathLib.sinAndCos(lat);
        final double sinLat = sincosLat[0];
        final double cosLat = sincosLat[1];
        final double[] sincosLon = MathLib.sinAndCos(lon);
        final double sinLon = sincosLon[0];
        final double cosLon = sincosLon[1];
        final Vector3D position = new Vector3D(r * cosLat * cosLon, r * cosLat * sinLon, r * sinLat);

        final PVCoordinates pv = new PVCoordinates(position, Vector3D.ZERO);

        // transformation from EOD to frame
        final Transform transform = this.getICRF().getParent().getTransformTo(frame, date);

        return transform.transformPVCoordinates(pv);
    }

    /** {@inheritDoc} */
    @Override
    public Frame getNativeFrame(final AbsoluteDate date,
            final Frame frame) throws PatriusException {
        return this.getICRF().getParent();
    }

    /**
     * Compute the sum of sine terms.
     * 
     * @param coeff
     *        : coefficients for the linear combinaison of D, M, Mp, F, A1, A2, A3, Lp
     * @param arg
     *        : expression of D, M, Mp, F, A1, A2, A3, Lp (time depending)
     * @param magnitude
     *        : magnitude of the sine/cosine of the argument (ie linear combinaison of D, M, Mp, F, A1, A2, A3, Lp)
     * @param e
     *        : eccenticity
     * @param e2
     *        : eccentricity square
     * @param terms
     *        : number of terms taken into account
     * @return the sum
     */
    private double sumSinComputation(final double[][] coeff, final double[] arg, final double[] magnitude,
                                     final double e, final double e2, final int terms) {
        double sum = 0.;
        double argument;
        // loop on the magnitude
        for (int i = 0; i < terms; i++) {
            argument = 0.;
            // loop on the coefficients
            for (int j = 0; j < arg.length; j++) {
                argument += coeff[i][j] * arg[j];
            }
            if (MathLib.abs(coeff[i][1]) == 1) {
                // sin calculation with eccentricity
                sum += e * (magnitude[i] * EPS) * MathLib.sin(argument);
            } else if (MathLib.abs(coeff[i][1]) == 2) {
                // sin calculation with squared eccentricity
                sum += e2 * (magnitude[i] * EPS) * MathLib.sin(argument);
            } else {
                // sin calculation
                sum += magnitude[i] * EPS * MathLib.sin(argument);
            }
        }
        return sum;
    }

    /**
     * Compute the sum of cosine terms.
     * 
     * @param coeff
     *        : coefficients for the linear combinaison of D, M, Mp, F
     * @param arg
     *        : expression of D, M, Mp, F, A1, A2, A3, Lp (time depending)
     * @param magnitude
     *        : magnitude of the sine/cosine of the argument (ie linear combinaison of D, M, Mp, F)
     * @param e
     *        : eccenticity
     * @param e2
     *        : eccentricity square
     * @param terms
     *        : number of terms taken into account
     * @return the sum
     */
    private double sumCosComputation(final double[][] coeff, final double[] arg, final double[] magnitude,
                                     final double e, final double e2, final int terms) {
        double sum = 0.;
        double argument;
        // loop on the magnitude
        for (int i = 0; i < terms; i++) {
            argument = 0.;
            // loop on the coefficients
            for (int j = 0; j < arg.length; j++) {
                argument += coeff[i][j] * arg[j];
            }
            if (MathLib.abs(coeff[i][1]) == 1) {
                // cosinus calculation with eccentricity
                sum += e * magnitude[i] * MathLib.cos(argument);
            } else if (MathLib.abs(coeff[i][1]) == 2) {
                // cosinus calculation with squared eccentricity
                sum += e2 * magnitude[i] * MathLib.cos(argument);
            } else {
                // cosinus calculation
                sum += magnitude[i] * MathLib.cos(argument);
            }
        }
        return sum;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        final String result = super.toString();
        final StringBuilder builder = new StringBuilder(result);
        builder.append("- Ephemeris origin: Meeus Moon model");
        return builder.toString();
    }
}
