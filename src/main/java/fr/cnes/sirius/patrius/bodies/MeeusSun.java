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
 * @history created 31/07/12
 *
 * HISTORY
 * VERSION:4.7:DM:DM-2703:18/05/2021: Acces facilite aux donnees d'environnement (application interplanetaire) 
 * VERSION:4.5:FA:FA-2257:27/05/2020:Le modèle Meeus bord semble faux 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:591:05/04/2016:add IAU pole data
 * VERSION::DM:605:30/09/2016:gathered Meeus models
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
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;

/**
 * <p>
 * This class implements the Sun ephemerides according to the algorithm of Meeus, it only provides the position. Note
 * that it is not possible to build this Sun from the CelestialBodyFactory.
 * </p>
 * See "Astronomical Algorithms", chapter 24 "Solar Coordinates", Jean Meeus, 1991.
 * This class allows the use of three different Meeus model : the standard Meeus model, the STELA one and
 * the on board model (used for CERES mission for instance).
 * 
 * </p>
 * About Stela's implementation of this model :
 * <p>
 * This class contains methods to store {@link #getInertiallyOrientedFrame()} to integration frame (CIRF) transform to
 * speed up computation during the integration process if Stela model have been chosen. As this transform varies slowly
 * through time, it has been shown it is not necessary to recompute it every time. Warning: these methods should not be
 * used in a stand-alone use (unless you known what you are doing). There are two methods:
 * <ul>
 * <li>{@link #updateTransform(AbsoluteDate, Frame)}: store transform from {@link #getInertiallyOrientedFrame()} to
 * provided frame at provided date.</li>
 * <li>{@link #resetTransform()}: reset stored transform.</li>
 * </ul>
 * </p>
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
 * @version $Id: MeeusSun.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.1
 * 
 */
@SuppressWarnings("PMD.NullAssignment")
public class MeeusSun extends AbstractCelestialBody {

    /**
     * IUD
     */
    private static final long serialVersionUID = 3593147893747468086L;

    /**
     * EOD frame with EOP : used for STANDARD model
     */
    private static Frame eodFrame;
    static {
        try {
            eodFrame = FramesFactory.getEODFrame(true);
        } catch (final PatriusException exc) {
            throw new PatriusRuntimeException(exc.getSpecifier(), exc);
        }
    }

    /**
     * MOD frame without EOP : used for STELA/BOARD model
     */
    private static Frame modFrame;
    static {
        try {
            modFrame = FramesFactory.getMOD(false);
        } catch (final PatriusException exc) {
            throw new PatriusRuntimeException(exc.getSpecifier(), exc);
        }
    }

    // Standard model constants set.

    /**
     * STANDARD Sun mean longitude constant (rad).
     */
    private static final double STANDARD_MEAN_LONGITUDE_0 = 280.46645 * MathUtils.DEG_TO_RAD;
    /**
     * STANDARD Sun mean longitude constant (rad).
     */
    private static final double STANDARD_MEAN_LONGITUDE_1 = 36000.76983 * MathUtils.DEG_TO_RAD;
    /**
     * STANDARD Sun mean longitude constant (rad).
     */
    private static final double STANDARD_MEAN_LONGITUDE_2 = 0.0003032 * MathUtils.DEG_TO_RAD;
    /**
     * STANDARD Sun mean anomaly constant (rad).
     */
    private static final double STANDARD_MEAN_ANOMALY_0 = 357.52910 * MathUtils.DEG_TO_RAD;
    /**
     * STANDARD Sun mean anomaly constant (rad).
     */
    private static final double STANDARD_MEAN_ANOMALY_1 = 35999.05030 * MathUtils.DEG_TO_RAD;
    /**
     * Sun mean anomaly constant (rad).
     */
    private static final double STANDARD_MEAN_ANOMALY_2 = -0.0001559 * MathUtils.DEG_TO_RAD;
    /**
     * STANDARD Sun mean anomaly constant (rad).
     */
    private static final double STANDARD_MEAN_ANOMALY_3 = -0.00000048 * MathUtils.DEG_TO_RAD;
    /**
     * STANDARD Earth's orbit eccentricity constant.
     */
    private static final double STANDARD_ECCENTRICITY_0 = 0.016708617;
    /**
     * STANDARD Earth's orbit eccentricity constant.
     */
    private static final double STANDARD_ECCENTRICITY_1 = -0.000042037;
    /**
     * STANDARD Earth's orbit eccentricity constant.
     */
    private static final double STANDARD_ECCENTRICITY_2 = -0.0000001236;
    /**
     * STANDARD Constant of the Sun's equation of center C (rad).
     */
    private static final double STANDARD_SUN_CENTER_10 = 1.914600 * MathUtils.DEG_TO_RAD;
    /**
     * STANDARD Constant of the Sun's equation of center C (rad).
     */
    private static final double STANDARD_SUN_CENTER_11 = -0.004817 * MathUtils.DEG_TO_RAD;
    /**
     * STANDARD Constant of the Sun's equation of center C (rad).
     */
    private static final double STANDARD_SUN_CENTER_12 = -0.000014 * MathUtils.DEG_TO_RAD;
    /**
     * STANDARD Constant of the Sun's equation of center C (rad).
     */
    private static final double STANDARD_SUN_CENTER_20 = 0.019993 * MathUtils.DEG_TO_RAD;
    /**
     * STANDARD Constant of the Sun's equation of center C (rad).
     */
    private static final double STANDARD_SUN_CENTER_21 = -0.000101 * MathUtils.DEG_TO_RAD;
    /**
     * STANDARD Constant of the Sun's equation of center C (rad).
     */
    private static final double STANDARD_SUN_CENTER_30 = 0.000290 * MathUtils.DEG_TO_RAD;
    /**
     * STANDARD Distance from the Earth to the Sun constant (AU).
     */
    private static final double DISTANCE_SUN_EARH = 1.000001018;

    // STELA model constants set.

    /**
     * STELA Sun mean longitude constant (rad).
     */
    private static final double STELA_MEAN_LONGITUDE_0 = 280.46646 * MathUtils.DEG_TO_RAD;
    /**
     * STELA Sun mean longitude constant (rad).
     */
    private static final double STELA_MEAN_LONGITUDE_1 = 36000.76983 * MathUtils.DEG_TO_RAD;
    /**
     * STELA Sun mean longitude constant (rad).
     */
    private static final double STELA_MEAN_LONGITUDE_2 = 0.0003032 * MathUtils.DEG_TO_RAD;
    /**
     * STELA Sun mean anomaly constant (rad).
     */
    private static final double STELA_MEAN_ANOMALY_0 = 357.52911 * MathUtils.DEG_TO_RAD;
    /**
     * STELA Sun mean anomaly constant (rad).
     */
    private static final double STELA_MEAN_ANOMALY_1 = 35999.05029 * MathUtils.DEG_TO_RAD;
    /**
     * STELA Sun mean anomaly constant (rad).
     */
    private static final double STELA_MEAN_ANOMALY_2 = -0.0001537 * MathUtils.DEG_TO_RAD;
    /**
     * STELA Sun mean anomaly constant (rad).
     */
    private static final double STELA_MEAN_ANOMALY_3 = 0.0 * MathUtils.DEG_TO_RAD;
    /**
     * STELA Earth's orbit eccentricity constant.
     */
    private static final double STELA_ECCENTRICITY_0 = 0.016708634;
    /**
     * STELA Earth's orbit eccentricity constant.
     */
    private static final double STELA_ECCENTRICITY_1 = -0.000042037;
    /**
     * STELA Earth's orbit eccentricity constant.
     */
    private static final double STELA_ECCENTRICITY_2 = -0.0000001267;
    /**
     * STELA Obliquity of the ecliptic constant.
     */
    private static final double STELA_OBLIQUITY_0 = 23.439291 * MathUtils.DEG_TO_RAD;
    /**
     * STELA Obliquity of the ecliptic constant.
     */
    private static final double STELA_OBLIQUITY_1 = -0.0130111 * MathUtils.DEG_TO_RAD;
    /**
     * STELA Obliquity of the ecliptic constant.
     */
    private static final double STELA_OBLIQUITY_2 = -1.6410E-07 * MathUtils.DEG_TO_RAD;
    /**
     * STELA Constant of the Sun's equation of center C (rad).
     */
    private static final double STELA_SUN_CENTER_10 = 1.914602 * MathUtils.DEG_TO_RAD;
    /**
     * STELA Constant of the Sun's equation of center C (rad).
     */
    private static final double STELA_SUN_CENTER_11 = -0.004817 * MathUtils.DEG_TO_RAD;
    /**
     * STELA Constant of the Sun's equation of center C (rad).
     */
    private static final double STELA_SUN_CENTER_12 = -0.000014 * MathUtils.DEG_TO_RAD;
    /**
     * STELA Constant of the Sun's equation of center C (rad).
     */
    private static final double STELA_SUN_CENTER_20 = 0.019993 * MathUtils.DEG_TO_RAD;
    /**
     * STELA Constant of the Sun's equation of center C (rad).
     */
    private static final double STELA_SUN_CENTER_21 = -0.000101 * MathUtils.DEG_TO_RAD;
    /**
     * STELA Constant of the Sun's equation of center C (rad).
     */
    private static final double STELA_SUN_CENTER_30 = 0.000289 * MathUtils.DEG_TO_RAD;
    /**
     * STELA Coefficient from the Earth to the Sun constant (AU).
     */
    private static final double STELA_COEFF_SUN_EARH = 149598022.291E3;

    /** Cached transform from {@link #getInertiallyOrientedFrame()} to integration frame (CIRF). */
    private static Transform cachedTransform = null;

    // Board constants set.

    /**
     * BOARD Obliquity of the ecliptic constant.
     */
    private static final double BOARD_OBLIQUITY_0 = 23.43929111 * MathUtils.DEG_TO_RAD;
    /**
     * BOARD Obliquity of the ecliptic constant.
     */
    private static final double BOARD_OBLIQUITY_1 = -0.0130041666 * MathUtils.DEG_TO_RAD;
    /**
     * BOARD Obliquity of the ecliptic constant.
     */
    private static final double BOARD_OBLIQUITY_2 = -1.63888E-07 * MathUtils.DEG_TO_RAD;

    /**
     * BOARD Obliquity of the ecliptic constant.
     */
    private static final double BOARD_OBLIQUITY_3 = 5.036111E-07 * MathUtils.DEG_TO_RAD;

    /**
     * BOARD constant used in Sun longitude conversion to J2000 frame.
     */
    private static final double SUN_LONG_TO_J2000 = 1.397 * MathUtils.DEG_TO_RAD;

    /** Enumerate to choose the used Meeus model : standard, Stela or board model. */
    public enum MODEL {
        /** Standard model. */
        STANDARD(STANDARD_MEAN_LONGITUDE_0, STANDARD_MEAN_LONGITUDE_1, STANDARD_MEAN_LONGITUDE_2,
                STANDARD_MEAN_ANOMALY_0, STANDARD_MEAN_ANOMALY_1, STANDARD_MEAN_ANOMALY_2, STANDARD_MEAN_ANOMALY_3,
                STANDARD_ECCENTRICITY_0, STANDARD_ECCENTRICITY_1, STANDARD_ECCENTRICITY_2, 0., 0., 0., 0.,
                STANDARD_SUN_CENTER_10, STANDARD_SUN_CENTER_11, STANDARD_SUN_CENTER_12, STANDARD_SUN_CENTER_20,
                STANDARD_SUN_CENTER_21, STANDARD_SUN_CENTER_30, DISTANCE_SUN_EARH *
                    Constants.JPL_SSD_ASTRONOMICAL_UNIT, eodFrame, 0.),

        /** STELA model. */
        STELA(STELA_MEAN_LONGITUDE_0, STELA_MEAN_LONGITUDE_1, STELA_MEAN_LONGITUDE_2, STELA_MEAN_ANOMALY_0,
                STELA_MEAN_ANOMALY_1, STELA_MEAN_ANOMALY_2, STELA_MEAN_ANOMALY_3, STELA_ECCENTRICITY_0,
                STELA_ECCENTRICITY_1, STELA_ECCENTRICITY_2, STELA_OBLIQUITY_0, STELA_OBLIQUITY_1, STELA_OBLIQUITY_2,
                0., STELA_SUN_CENTER_10, STELA_SUN_CENTER_11, STELA_SUN_CENTER_12, STELA_SUN_CENTER_20,
                STELA_SUN_CENTER_21, STELA_SUN_CENTER_30, STELA_COEFF_SUN_EARH, modFrame, 0.),

        /** Board model. */
        BOARD(STELA_MEAN_LONGITUDE_0, STELA_MEAN_LONGITUDE_1, STELA_MEAN_LONGITUDE_2, STELA_MEAN_ANOMALY_0,
                STELA_MEAN_ANOMALY_1, STELA_MEAN_ANOMALY_2, STELA_MEAN_ANOMALY_3, STELA_ECCENTRICITY_0,
                STELA_ECCENTRICITY_1, STELA_ECCENTRICITY_2, BOARD_OBLIQUITY_0, BOARD_OBLIQUITY_1, BOARD_OBLIQUITY_2,
                BOARD_OBLIQUITY_3, STELA_SUN_CENTER_10, STELA_SUN_CENTER_11, STELA_SUN_CENTER_12, STELA_SUN_CENTER_20,
                STELA_SUN_CENTER_21, STELA_SUN_CENTER_30, STELA_COEFF_SUN_EARH, FramesFactory.getEME2000(),
                SUN_LONG_TO_J2000);

        // Enum attributes

        /**
         * Sun mean longitude constant (rad).
         */
        protected final double meanLongitude0;
        /**
         * Sun mean longitude constant (rad).
         */
        protected final double meanLongitude1;
        /**
         * Sun mean longitude constant (rad).
         */
        protected final double meanLongitude2;
        /**
         * Sun mean anomaly constant (rad).
         */
        protected final double meanAnomaly0;
        /**
         * Sun mean anomaly constant (rad).
         */
        protected final double meanAnomaly1;
        /**
         * Sun mean anomaly constant (rad).
         */
        protected final double meanAnomaly2;
        /**
         * Sun mean anomaly constant (rad).
         */
        protected final double meanAnomaly3;
        /**
         * Earth's orbit eccentricity constant.
         */
        protected final double eccentricity0;
        /**
         * Earth's orbit eccentricity constant.
         */
        protected final double eccentricity1;
        /**
         * Earth's orbit eccentricity constant.
         */
        protected final double eccentricity2;
        /**
         * Obliquity of the ecliptic constant.
         */
        protected final double obliquity0;
        /**
         * Obliquity of the ecliptic constant.
         */
        protected final double obliquity1;
        /**
         * Obliquity of the ecliptic constant.
         */
        protected final double obliquity2;
        /**
         * Obliquity of the ecliptic constant.
         */
        protected final double obliquity3;
        /**
         * Constant of the Sun's equation of center C (rad).
         */
        protected final double sunCenter10;
        /**
         * Constant of the Sun's equation of center C (rad).
         */
        protected final double sunCenter11;
        /**
         * Constant of the Sun's equation of center C (rad).
         */
        protected final double sunCenter12;
        /**
         * Constant of the Sun's equation of center C (rad).
         */
        protected final double sunCenter20;
        /**
         * Constant of the Sun's equation of center C (rad).
         */
        protected final double sunCenter21;
        /**
         * Constant of the Sun's equation of center C (rad).
         */
        protected final double sunCenter30;
        /**
         * Coefficient from the Earth to the Sun constant (AU).
         */
        protected final double sunEarthCoeff;

        /**
         * Sun inertially oriented frame.
         */
        protected final Frame inertialSunFrame;

        /**
         * Sun longitude in J2000.
         */
        protected final double sunLongToJ2000;

        /**
         * 
         * Constructor for enumerate : constants related to the chosen
         * model are instantiated using available static values.
         * 
         * @param meanLong0
         *        Sun mean longitude constant (rad).
         * @param meanLong1
         *        Sun mean longitude constant (rad).
         * @param meanLong2
         *        Sun mean longitude constant (rad).
         * @param meanAno0
         *        Sun mean anomaly constant (rad).
         * @param meanAno1
         *        Sun mean anomaly constant (rad).
         * @param meanAno2
         *        Sun mean anomaly constant (rad).
         * @param meanAno3
         *        Sun mean anomaly constant (rad).
         * @param ecc0
         *        Earth's orbit eccentricity constant.
         * @param ecc1
         *        Earth's orbit eccentricity constant.
         * @param ecc2
         *        Earth's orbit eccentricity constant.
         * @param obliq0
         *        Obliquity of the ecliptic constant.
         * @param obliq1
         *        Obliquity of the ecliptic constant.
         * @param obliq2
         *        Obliquity of the ecliptic constant.
         * @param obliq3
         *        Obliquity of the ecliptic constant.
         * @param center10
         *        Constant of the Sun's equation of center C (rad).
         * @param center11
         *        Constant of the Sun's equation of center C (rad).
         * @param center12
         *        Constant of the Sun's equation of center C (rad).
         * @param center20
         *        Constant of the Sun's equation of center C (rad).
         * @param center21
         *        Constant of the Sun's equation of center C (rad).
         * @param center30
         *        Constant of the Sun's equation of center C (rad).
         * @param coeff
         *        Coefficient from the Earth to the Sun constant (AU).
         * @param inertialFrame
         *        Sun inertially oriented frame.
         * @param sunLongInj2000
         *        Sun longitude in J2000
         * 
         */
        private MODEL(final double meanLong0, final double meanLong1, final double meanLong2, final double meanAno0,
            final double meanAno1, final double meanAno2, final double meanAno3, final double ecc0, final double
            ecc1, final double ecc2, final double obliq0, final double obliq1, final double obliq2, final double
            obliq3, final double center10, final double center11, final double center12, final double center20,
            final double center21, final double center30, final double coeff, final Frame inertialFrame,
            final double sunLongInj2000) {
            this.meanLongitude0 = meanLong0;
            this.meanLongitude1 = meanLong1;
            this.meanLongitude2 = meanLong2;
            this.meanAnomaly0 = meanAno0;
            this.meanAnomaly1 = meanAno1;
            this.meanAnomaly2 = meanAno2;
            this.meanAnomaly3 = meanAno3;
            this.eccentricity0 = ecc0;
            this.eccentricity1 = ecc1;
            this.eccentricity2 = ecc2;
            this.obliquity0 = obliq0;
            this.obliquity1 = obliq1;
            this.obliquity2 = obliq2;
            this.obliquity3 = obliq3;
            this.sunCenter10 = center10;
            this.sunCenter11 = center11;
            this.sunCenter12 = center12;
            this.sunCenter20 = center20;
            this.sunCenter21 = center21;
            this.sunCenter30 = center30;
            this.sunEarthCoeff = coeff;
            this.inertialSunFrame = inertialFrame;
            this.sunLongToJ2000 = sunLongInj2000;
        }

    }

    /** Meeus model to be used. */
    private final MODEL meeusModel;

    /**
     * Simple constructor for standard Meeus model.
     */
    public MeeusSun() {
        this(MODEL.STANDARD);
    }

    /**
     * Constructor to build wished Meeus model : standard model,
     * STELA model or board model.
     * 
     * @param model
     *        Meeus model to be used
     */
    public MeeusSun(final MODEL model) {
        super("Meeus Sun", Constants.JPL_SSD_SUN_GM, IAUPoleFactory.getIAUPole(EphemerisType.SUN),
            model.inertialSunFrame, "inertial Sun frame", "Sun frame");

        this.meeusModel = model;
    }

    /** {@inheritDoc} */
    @Override
    public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {

        // time measured in Julian centuries of 36525 ephemeris days from the epoch J2000
        final double t = date.durationFrom(AbsoluteDate.J2000_EPOCH) / Constants.JULIAN_CENTURY;
        final double t2 = t * t;
        final double t3 = t2 * t;

        // geometric mean longitude of the Sun
        double l0 =
            this.meeusModel.meanLongitude0 + this.meeusModel.meanLongitude1 * t + this.meeusModel.meanLongitude2 * t2;
        l0 = MathUtils.normalizeAngle(l0, FastMath.PI);

        // mean anomaly of the Sun
        double m = this.meeusModel.meanAnomaly0 + this.meeusModel.meanAnomaly1 * t + this.meeusModel.meanAnomaly2 * t2 +
            this.meeusModel.meanAnomaly3 * t3;
        m = MathUtils.normalizeAngle(m, FastMath.PI);

        // the Sun's equation of center C
        double c = (this.meeusModel.sunCenter10 + this.meeusModel.sunCenter11 * t + this.meeusModel.sunCenter12 * t2)
            * MathLib.sin(m)
            + (this.meeusModel.sunCenter20 + this.meeusModel.sunCenter21 * t) * MathLib.sin(2 * m)
            + this.meeusModel.sunCenter30 * MathLib.sin(3 * m);
        c = MathUtils.normalizeAngle(c, FastMath.PI);

        // the Sun's true geometric longitude
        double theta = l0 + c;
        // Compute longitude in J2000
        theta -= this.meeusModel.sunLongToJ2000 * t;

        final double[] sincosTheta = MathLib.sinAndCos(theta);
        final double sinTheta = sincosTheta[0];
        final double cosTheta = sincosTheta[1];

        // eccentricity of the Earth's orbit
        final double e =
            this.meeusModel.eccentricity0 + this.meeusModel.eccentricity1 * t + this.meeusModel.eccentricity2 * t2;

        // the Sun's true anomaly
        final double v0 = m + c;

        // distance from the Earth to the Sun expressed in astronomical units
        final double r = this.meeusModel.sunEarthCoeff * (1 - e * e) / (1 + e * MathLib.cos(v0));

        // Obliquity of the ecliptic
        final double epsSun =
            this.meeusModel.obliquity0 + this.meeusModel.obliquity1 * t + this.meeusModel.obliquity2 * t2
                + this.meeusModel.obliquity3 * t3;

        // Direct cosines :
        final double[] sincosEpsSun = MathLib.sinAndCos(epsSun);
        final double sinEpsSun = sincosEpsSun[0];
        final double cosEpsSun = sincosEpsSun[1];

        final double x = cosTheta;
        final double y = sinTheta * cosEpsSun;
        final double z = sinTheta * sinEpsSun;

        // Sun's position in EOD frame
        final Vector3D position = new Vector3D(r * x, r * y, r * z);
        final PVCoordinates pv = new PVCoordinates(position, Vector3D.ZERO);

        final Transform transform;
        if (this.meeusModel == MODEL.STELA) {
            // transformation from EOD to frame
            if (cachedTransform == null) {
                // Independent use: return normal computation
                transform = this.getInertiallyOrientedFrame().getParent().getTransformTo(frame, date);
            } else {
                // Cached transform is used (integration process): return PV obtained with cached transform
                transform = cachedTransform;
            }
        } else {
            // transformation from J2000 to frame
            transform = this.getInertiallyOrientedFrame().getParent().getTransformTo(frame, date);
        }
        return transform.transformPVCoordinates(pv);
    }

    /**
     * Update cached transform from {@link FramesFactory#getMOD(boolean)} to provided frame.
     * Once called, this transform will always be used when calling {@link #getPVCoordinates(AbsoluteDate, Frame)}
     * unless a call to {@link #resetTransform()} has been made.
     * <p>
     * Warning : this method must only be called if the chosen model is the Meeus STELA model.
     * </p>
     * 
     * @param date
     *        a date
     * @param frame
     *        a frame
     * @throws PatriusException
     *         thrown if transformation failed
     */
    public static void updateTransform(final AbsoluteDate date, final Frame frame) throws PatriusException {
        cachedTransform = FramesFactory.getMOD(false).getTransformTo(frame, date);
    }

    /**
     * Reset cached transform.
     * <p>
     * Warning : this method must only be called if the chosen model is the Meeus STELA model.
     * </p>
     */
    public static void resetTransform() {
        cachedTransform = null;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        final String result = super.toString();
        final StringBuilder builder = new StringBuilder(result);
        builder.append("- Ephemeris origin: Meeus Sun model (" + meeusModel + " model)");
        return builder.toString();
    }
}
