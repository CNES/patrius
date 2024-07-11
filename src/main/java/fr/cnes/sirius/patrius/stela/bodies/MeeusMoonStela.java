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
 * @history created 20/02/2013
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3149:10/05/2022:[PATRIUS] Optimisation des reperes interplanetaires 
 * VERSION:4.9:DM:DM-3159:10/05/2022:[PATRIUS] Implementation de l'interface GeometricBodyShape par CelestialBody
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3165:10/05/2022:[PATRIUS] Amelioration de la propagation du signal 
 * VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2703:18/05/2021: Acces facilite aux donnees d'environnement (application interplanetaire) 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:91:26/07/2013:transform used only when needed, DEG_TO_RAD factorised for cancelation problems
 * VERSION::FT:63:19/08/2013:comments changes
 * VERSION::DM:317:04/03/2015: STELA integration in CIRF with referential choice (ICRF, CIRF or MOD)
 * VERSION::FA:591:05/04/2016:add IAU pole data
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.bodies;

import fr.cnes.sirius.patrius.bodies.AbstractCelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyEphemeris;
import fr.cnes.sirius.patrius.bodies.EphemerisType;
import fr.cnes.sirius.patrius.bodies.IAUPoleFactory;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.frames.CelestialBodyFrame;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * This class implements the Moon ephemerides according to the algorithm of Meeus, it only provides the position. Note
 * that it is not possible to build this Moon from the CelestialBodyFactory.
 * </p>
 * See Stela's implementation of this model
 * <p>
 * This class contains methods to store {@link #getInertialFrameConstantModel()} to integration frame (CIRF) transform
 * to speed up computation during the integration process. As this transform varies slowly through time, it has been
 * demonstrated it is not necessary to recompute it every time. Warning: these methods should not be used in a
 * stand-alone use (unless you known what you are doing). There are two methods:
 * <ul>
 * <li>{@link #updateTransform(AbsoluteDate, Frame)}: store transform from {@link #getInertialFrameConstantModel()} to
 * provided frame at provided date.</li>
 * <li>{@link #resetTransform()}: reset stored transform</li>
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
 * @author Cedric Dental
 * 
 * @since 1.3
 * 
 */
@SuppressWarnings("PMD.NullAssignment")
public class MeeusMoonStela extends AbstractCelestialBody {

    /** Serializable UID. */
    private static final long serialVersionUID = 6952697176025508036L;

    /**
     * MU Constant of the Sun.EARTH_RADIUS
     */
    private static final double MOON_MU = 4.902777900000E12;
    /**
     * Moon's radius.
     */
    private static final double MOON_RADIUS = 1737400.;

    /** Cached transform from {@link #getInertialFrameConstantModel()} to integration frame (CIRF). */
    private static Transform cachedTransform = null;

    /** The Earth Radius. */
    private final double earthRadius;

    /**
     * Simple constructor.
     * 
     * @param inEarthRadius
     *        the Earth Radius
     */
    public MeeusMoonStela(final double inEarthRadius) {
        // EME2000 frame (in Stela software, EOD and MOD are considered equal for sun ephemerides)
        super("Meeus Moon Stela", MOON_MU, IAUPoleFactory.getIAUPole(EphemerisType.MOON), FramesFactory.getMOD(false));
        this.earthRadius = inEarthRadius;
        this.setEphemeris(new MeeusMoonStelaEphemeris(getICRF()));
        this.setShape(new OneAxisEllipsoid(MOON_RADIUS, 0., this.getRotatingFrameTrueModel(), "Moon"));
    }

    /**
     * Update cached transform from {@link FramesFactory#getMOD(boolean)} to provided frame.
     * Once called, this transform will always be used when calling {@link #getPVCoordinates(AbsoluteDate, Frame)}
     * unless a call to {@link #resetTransform()} has been made
     * 
     * @param date
     *        a date
     * @param frame
     *        a frame
     * @throws PatriusException
     *         thrown if transformation computation failed
     */
    public static void updateTransform(final AbsoluteDate date, final Frame frame) throws PatriusException {
        cachedTransform = FramesFactory.getMOD(false).getTransformTo(frame, date);
    }

    /**
     * Reset cached transform.
     */
    public static void resetTransform() {
        cachedTransform = null;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        final String result = super.toString();
        final StringBuilder builder = new StringBuilder(result);
        builder.append("- Ephemeris origin: Meeus Moon STELA model");
        return builder.toString();
    }
    
    /**
     * <p>
     * This class implements the Moon ephemerides according to the algorithm of Meeus, it only provides the position.
     * Note that it is not possible to build this Moon from the CelestialBodyFactory.
     * </p>
     * See Stela's implementation of this model
     * <p>
     * This class contains methods to store {@link #getInertialFrameConstantModel()} to integration frame (CIRF)
     * transform to speed up computation during the integration process. As this transform varies slowly through time,
     * it has been demonstrated it is not necessary to recompute it every time. Warning: these methods should not be
     * used in a stand-alone use (unless you known what you are doing). There are two methods:
     * <ul>
     * <li>{@link #updateTransform(AbsoluteDate, Frame)}: store transform from {@link #getInertialFrameConstantModel()}
     * to provided frame at provided date.</li>
     * <li>{@link #resetTransform()}: reset stored transform</li>
     * </ul>
     * </p>
     * 
     * <p>
     * Note that pole information allowing to define inertially-centered frame and rotating frame are defined in
     * {@link IAUPoleFactory} since Meeus model does not provide the information.
     * </p>
     * 
     * @author Emmanuel Bignon
     * 
     * @since 4.10
     */
    @SuppressWarnings("PMD.NullAssignment")
    private class MeeusMoonStelaEphemeris implements CelestialBodyEphemeris {

        /** Serializable UID. */
        private static final long serialVersionUID = 6952697176025508036L;
       
        /**
         * Moon Sun mean longitude constant (rad).
         */
        private static final double MS_MEAN_LONGITUDE_0 = 297.85019547;
        /**
         * Moon Sun mean longitude constant (rad).
         */
        private static final double MS_MEAN_LONGITUDE_1 = 445267.1114469;
        /**
         * Moon Sun mean longitude constant (rad).
         */
        private static final double MS_MEAN_LONGITUDE_2 = 0.0017696;
        /**
         * Moon Sun mean longitude constant (rad).
         */
        private static final double MS_MEAN_LONGITUDE_3 = 1.831e-06;
        /**
         * Moon Sun mean longitude constant (rad).
         */
        private static final double MS_MEAN_LONGITUDE_4 = 8.8e-09;
        /**
         * Moon mean longitude constant (rad).
         */
        private static final double MEAN_ANOMALY_0 = 134.96340251;
        /**
         * Moon mean anomaly constant (rad).
         */
        private static final double MEAN_ANOMALY_1 = 477198.8675605;
        /**
         * Moon mean anomaly constant (rad).
         */
        private static final double MEAN_ANOMALY_2 = 0.0088553;
        /**
         * Moon mean anomaly constant (rad).
         */
        private static final double MEAN_ANOMALY_3 = 1.4343e-05;
        /**
         * Moon mean anomaly constant (rad).
         */
        private static final double MEAN_ANOMALY_4 = 6.797e-06;
        /**
         * Moon Sun mean longitude constant (rad).
         */
        private static final double MS_MEAN_ANOMALY_0 = 357.52910918;
        /**
         * Moon Sun mean anomaly constant (rad).
         */
        private static final double MS_MEAN_ANOMALY_1 = 35999.0502911;
        /**
         * Moon Sun mean anomaly constant (rad).
         */
        private static final double MS_MEAN_ANOMALY_2 = 0.0001537;
        /**
         * Moon Sun mean anomaly constant (rad).
         */
        private static final double MS_MEAN_ANOMALY_3 = 3.8e-08;
        /**
         * Moon Sun mean anomaly constant (rad).
         */
        private static final double MS_MEAN_ANOMALY_4 = 3.19e-09;
        /**
         * Obliquity of the ecliptic constant.
         */
        private static final double OBLIQUITY_0 = 23.439291;
        /**
         * Obliquity of the ecliptic constant.
         */
        private static final double OBLIQUITY_1 = -0.0130111;
        /**
         * Obliquity of the ecliptic constant.
         */
        private static final double OBLIQUITY_2 = -1.64E-07;
        /**
         * Obliquity of the ecliptic constant.
         */
        private static final double OBLIQUITY_3 = 5.04e-07;
        /**
         * Moon mean argument constant (rad).
         */
        private static final double MEAN_ARGUMENT_0 = 93.27209062;
        /**
         * Moon mean argument constant (rad).
         */
        private static final double MEAN_ARGUMENT_1 = 483202.0174577;
        /**
         * Moon mean argument constant (rad).
         */
        private static final double MEAN_ARGUMENT_2 = 0.003542;
        /**
         * Moon mean argument constant (rad).
         */
        private static final double MEAN_ARGUMENT_3 = 2.88e-07;
        /**
         * Moon mean argument constant (rad).
         */
        private static final double MEAN_ARGUMENT_4 = 1.16e-09;
        /**
         * Longitude of the ecliptic constant.
         */
        private static final double LONG_ECL_0 = 218.32;
        /**
         * Longitude of the ecliptic constant.
         */
        private static final double LONG_ECL_1 = 481267.883;
        /**
         * Longitude of the ecliptic constant.
         */
        private static final double LONG_ECL_2 = 6.29;
        /**
         * Longitude of the ecliptic constant.
         */
        private static final double LONG_ECL_3 = 1.27;
        /**
         * Longitude of the ecliptic constant.
         */
        private static final double LONG_ECL_4 = 0.66;
        /**
         * Longitude of the ecliptic constant.
         */
        private static final double LONG_ECL_5 = 0.21;
        /**
         * Longitude of the ecliptic constant.
         */
        private static final double LONG_ECL_6 = 0.19;
        /**
         * Longitude of the ecliptic constant.
         */
        private static final double LONG_ECL_7 = 0.11;
        /**
         * Latitude of the ecliptic constant.
         */
        private static final double LAT_ECL_0 = 5.13;
        /**
         * Latitude of the ecliptic constant.
         */
        private static final double LAT_ECL_1 = 0.28;
        /**
         * Latitude of the ecliptic constant.
         */
        private static final double LAT_ECL_2 = 0.28;
        /**
         * Latitude of the ecliptic constant.
         */
        private static final double LAT_ECL_3 = 0.17;
        /**
         * Parallax of the ecliptic constant.
         */
        private static final double PAR_ECL_0 = 0.9508;
        /**
         * Parallax of the ecliptic constant.
         */
        private static final double PAR_ECL_1 = 0.0518;
        /**
         * Parallax of the ecliptic constant.
         */
        private static final double PAR_ECL_2 = 0.0095;
        /**
         * Parallax of the ecliptic constant.
         */
        private static final double PAR_ECL_3 = 0.0078;
        /**
         * Parallax of the ecliptic constant.
         */
        private static final double PAR_ECL_4 = 0.0028;

        /** ICRF frame linked to the body. */
        private final CelestialBodyFrame icrf;
        
        /**
         * Simple constructor.
         * 
         * @param icrf
         *        icrf linked to the body
         */
        public MeeusMoonStelaEphemeris(final CelestialBodyFrame icrf) {
            this.icrf = icrf;
        }

        /** {@inheritDoc} */
        @Override
        public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {

            // time measured in Julian centuries of 36525 ephemeris days from the epoch J2000
            final double t = date.offsetFrom(AbsoluteDate.J2000_EPOCH, TimeScalesFactory.getTT())
                / (Constants.JULIAN_CENTURY);
            final double t2 = t * t;
            final double t3 = t2 * t;
            final double t4 = t3 * t;

            // Obliquity of the ecliptic
            final double epsMoon = MathUtils.DEG_TO_RAD * (OBLIQUITY_0 + OBLIQUITY_1 * t +
                OBLIQUITY_2 * t2 + OBLIQUITY_3 * t3);
            final double[] sincosEpsMoon = MathLib.sinAndCos(epsMoon);
            final double sinEpsMoon = sincosEpsMoon[0];
            final double cosEpsMoon = sincosEpsMoon[1];

            // Mean Anomalies
            final double ml = MathUtils.DEG_TO_RAD * (MEAN_ANOMALY_0 + MEAN_ANOMALY_1 * t +
                MEAN_ANOMALY_2 * t2 + MEAN_ANOMALY_3 * t3
                - MEAN_ANOMALY_4 * t4);

            final double ms = MathUtils.DEG_TO_RAD * (MS_MEAN_ANOMALY_0 + MS_MEAN_ANOMALY_1 * t - MS_MEAN_ANOMALY_2 * t2
                - MS_MEAN_ANOMALY_3 * t3 - MS_MEAN_ANOMALY_4 * t4);

            final double ds = MathUtils.DEG_TO_RAD * (MS_MEAN_LONGITUDE_0 + MS_MEAN_LONGITUDE_1 * t -
                MS_MEAN_LONGITUDE_2 * t2
                + MS_MEAN_LONGITUDE_3 * t3 - MS_MEAN_LONGITUDE_4 * t4);

            final double ul = MathUtils.DEG_TO_RAD * (MEAN_ARGUMENT_0 + MEAN_ARGUMENT_1 * t -
                MEAN_ARGUMENT_2 * t2 + MEAN_ARGUMENT_3 * t3
                + MEAN_ARGUMENT_4 * t4);

            // Longitude and latitude of the ecliptic
            final double lonl = MathUtils.DEG_TO_RAD * (LONG_ECL_0 + LONG_ECL_1 * t +
                LONG_ECL_2 * MathLib.sin(ml) - LONG_ECL_3
                * MathLib.sin(ml - 2.0 * ds) + LONG_ECL_4 * MathLib.sin(2.0 * ds) + LONG_ECL_5
                * MathLib.sin(2.0 * ml) - LONG_ECL_6 * MathLib.sin(ms) - LONG_ECL_7 * MathLib.sin(2.0 * ul));

            final double latl = MathUtils.DEG_TO_RAD * (LAT_ECL_0 * MathLib.sin(ul)
                + LAT_ECL_1 * MathLib.sin(ml + ul) - LAT_ECL_2
                * MathLib.sin(ul - ml) - LAT_ECL_3 * MathLib.sin(ul - 2.0 * ds));

            // Parallax in the ecliptic
            final double parl = MathUtils.DEG_TO_RAD * (PAR_ECL_0 + PAR_ECL_1 * MathLib.cos(ml)
                + PAR_ECL_2 * MathLib.cos(ml - 2.0 * ds)
                + PAR_ECL_3 * MathLib.cos(2.0 * ds) + PAR_ECL_4 * MathLib.cos(2.0 * ml));

            // Longitude sine and cosine
            final double[] sincoslonl = MathLib.sinAndCos(lonl);
            final double sinlonl = sincoslonl[0];
            final double coslonl = sincoslonl[1];
            // Latitude sine and cosine
            final double[] sincoslatl = MathLib.sinAndCos(latl);
            final double sinlatl = sincoslatl[0];
            final double coslatl = sincoslatl[1];

            // distance from the Earth to the Sun expressed in astronomical units
            final double r = MathLib.divide(earthRadius, parl);

            // Direct cosines :
            final double x = coslatl * coslonl;
            final double y = cosEpsMoon * coslatl * sinlonl - sinEpsMoon * sinlatl;
            final double z = sinEpsMoon * coslatl * sinlonl + cosEpsMoon * sinlatl;

            // Moon's position in EOD frame
            final Vector3D position = new Vector3D(r * x, r * y, r * z);

            final PVCoordinates pv = new PVCoordinates(position, Vector3D.ZERO, Vector3D.ZERO);

            // Transformation from EOD to frame
            if (cachedTransform == null) {
                // Stand-alone use: return normal computation
                final Transform transform = icrf.getParent().getTransformTo(frame, date);
                return transform.transformPVCoordinates(pv);
            }
            // Cached transform is used (integration process): return PV obtained with cached transform
            return cachedTransform.transformPVCoordinates(pv);
        }

        /** {@inheritDoc} */
        @Override
        public Frame getNativeFrame(final AbsoluteDate date,
                final Frame frame) throws PatriusException {
            return icrf.getParent();
        }
    }
    
}
