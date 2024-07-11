/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:FA:FA-3202:03/11/2022:[PATRIUS] Renommage dans UserCelestialBody
 * VERSION:4.10:DM:DM-3211:03/11/2022:[PATRIUS] Ajout de fonctionnalites aux PyramidalField
 * VERSION:4.9:DM:DM-3149:10/05/2022:[PATRIUS] Optimisation des reperes interplanetaires 
 * VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3165:10/05/2022:[PATRIUS] Amelioration de la propagation du signal 
 * VERSION:4.9:DM:DM-3159:10/05/2022:[PATRIUS] Implementation de l'interface GeometricBodyShape par CelestialBody
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:DM:DM-3147:10/05/2022:[PATRIUS] Ajout a l'interface ITargetDirection d'une methode getTargetPvProv ...
 * VERSION:4.9:DM:DM-3166:10/05/2022:[PATRIUS] Definir l'ICRF comme repere racine 
 * VERSION:4.9:DM:DM-3127:10/05/2022:[PATRIUS] Ajout de deux methodes resize a l'interface GeometricBodyShape ...
 * VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
 * VERSION:4.9:DM:DM-3164:10/05/2022:[PATRIUS] Amelioration de la gestion du GM (parametre gravitationnel)
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2703:18/05/2021: Acces facilite aux donnees d'environnement (application interplanetaire) 
 * VERSION:4.7:FA:FA-2897:18/05/2021:Alignement Soleil-Sat-Terre non supporté pour la SRP 
 * VERSION:4.5:DM:DM-2449:27/05/2020:amelioration des performances multi-thread de TimeStampedCache
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:276:08/09/2014:Moon-Earth barycenter problem
 * VERSION::FA:1773:04/10/2018:correct bodies parent frame (= ICRF)
 * VERSION::FA:1777:04/10/2018:correct ICRF parent frame
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import java.io.IOException;
import java.io.ObjectInputStream;

import fr.cnes.sirius.patrius.forces.gravity.AbstractAttractionModel;
import fr.cnes.sirius.patrius.forces.gravity.NewtonianAttractionModel;
import fr.cnes.sirius.patrius.frames.CelestialBodyFrame;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Loader for JPL ephemerides binary files (DE 4xx) and similar formats (INPOP 06/08/10).
 * It loads the whole {@link CelestialBody}. For {@link CelestialBodyEphemeris} loader only,
 * see dedicated class.
 * <p>
 * JPL ephemerides binary files contain ephemerides for all solar system planets.
 * </p>
 * <p>
 * The JPL ephemerides binary files are recognized thanks to their base names, which must match the pattern
 * <code>[lu]nx[mp]####.ddd</code> (or <code>[lu]nx[mp]####.ddd.gz</code> for gzip-compressed files) where # stands for
 * a digit character and where ddd is an ephemeris type (typically 405 or 406).
 * </p>
 * <p>
 * Currently accepted JPL formats are: DE200/DE 202/DE 403/DE 405/DE 406/DE 410/DE 413/DE 414/DE 418/DE 421/DE 422/DE
 * 423 and DE 430.
 * </p>
 * <p>
 * The loader supports files encoded in big-endian as well as in little-endian notation. Usually, big-endian files are
 * named <code>unx[mp]####.ddd</code>, while little-endian files are named <code>lnx[mp]####.ddd</code>.
 * </p>
 * <p>
 * The IMCCE ephemerides binary files are recognized thanks to their base names, which must match the pattern
 * <code>inpop*.dat</code> (or <code>inpop*.dat.gz</code> for gzip-compressed files) where * stands for any string.
 * </p>
 * <p>
 * Currently accepted IMCCE formats are: INPOP 06b/06c/08a/10a/10b/10e/13c/17a/19a.
 * </p>
 * <p>
 * The loader supports files encoded in big-endian as well as in little-endian notation. Usually, big-endian files
 * contain <code>bigendian</code> in their names, while little-endian files contain <code>littleendian</code> in their
 * names.
 * </p>
 * <p>
 * The loader supports files in TDB or TCB time scales.
 * </p>
 * <p>
 * Note: the time scale isn't serialized.
 * </p>
 *
 * @author Luc Maisonobe
 */
public class JPLCelestialBodyLoader implements CelestialBodyLoader {

    /** Default supported files name pattern for JPL DE files. */
    public static final String DEFAULT_DE_SUPPORTED_NAMES = "^[lu]nx[mp](\\d\\d\\d\\d)\\.(?:4\\d\\d)$";

    /** Default supported files name pattern for IMCCE INPOP files. */
    public static final String DEFAULT_INPOP_SUPPORTED_NAMES = "^inpop.*\\.dat$";

    /** Serializable UID. */
    private static final long serialVersionUID = -8215287568823783794L;

    /** Sun's radius constant (2009 IAU report). */
    private static final double SUN_RADIUS = 696000000.;
    /** Mercury's radius constant (2009 IAU report). */
    private static final double MERCURY_RADIUS = 2439700.;
    /** Venus's radius constant (2009 IAU report). */
    private static final double VENUS_RADIUS = 6051800.;
    /** Earth's equatorial radius constant (2009 IAU report). */
    private static final double EARTH_EQUATORIAL_RADIUS = 6378136.6;
    /** Earth's polar radius constant (2009 IAU report). */
    private static final double EARTH_POLAR_RADIUS = 6356751.9;
    /** Moon's radius constant (2009 IAU report). */
    private static final double MOON_RADIUS = 1737400.;
    /** Mars's equatorial radius constant (2009 IAU report). */
    private static final double MARS_EQUATORIAL_RADIUS = 3396190.;
    /** Mars's polar radius constant (2009 IAU report). */
    private static final double MARS_POLAR_RADIUS = 3376200.;
    /** Jupiter's equatorial radius constant (2009 IAU report). */
    private static final double JUPITER_EQUATORIAL_RADIUS = 71492000.;
    /** Jupiter's polar radius constant (2009 IAU report). */
    private static final double JUPITER_POLAR_RADIUS = 66854000.;
    /** Saturn's equatorial radius constant (2009 IAU report). */
    private static final double SATURN_EQUATORIAL_RADIUS = 60268000.;
    /** Saturn's polar radius constant (2009 IAU report). */
    private static final double SATURN_POLAR_RADIUS = 54364000.;
    /** Uranus equatorial radius constant (2009 IAU report). */
    private static final double URANUS_EQUATORIAL_RADIUS = 25559000.;
    /** Uranus polar radius constant (2009 IAU report). */
    private static final double URANUS_POLAR_RADIUS = 24973000.;
    /** Neptune's equatorial radius constant (2009 IAU report). */
    private static final double NEPTUNE_EQUATORIAL_RADIUS = 24764000.;
    /** Neptune's polar radius constant (2009 IAU report). */
    private static final double NEPTUNE_POLAR_RADIUS = 24341000.;
    /** Sun's radius constant (2009 IAU report). */
    private static final double PLUTO_RADIUS = 1195000.;

    /** Regular expression for supported files names. */
    private final String supportedNames;

    /** Ephemeris type to generate. */
    private final EphemerisType generateType;

    /** Attraction model. */
    private final AbstractAttractionModel attractionModel;

    /** Ephemeris loader. */
    private final JPLEphemerisLoader ephemerisLoader;

    /**
     * Create a loader for JPL ephemerides binary files.
     *
     * @param supportedNamesIn
     *        regular expression for supported files names
     * @param generateTypeIn
     *        ephemeris type to generate
     * @exception PatriusException
     *            if the header constants cannot be read
     */
    public JPLCelestialBodyLoader(final String supportedNamesIn, final EphemerisType generateTypeIn)
            throws PatriusException {
        this(supportedNamesIn, generateTypeIn, null);
    }

    /**
     * Create a loader for JPL ephemerides binary files.
     *
     * @param supportedNamesIn
     *        regular expression for supported files names
     * @param generateTypeIn
     *        ephemeris type to generate
     * @param attractionModelIn
     *        attraction model
     * @exception PatriusException
     *            if the header constants cannot be read
     */
    public JPLCelestialBodyLoader(final String supportedNamesIn, final EphemerisType generateTypeIn,
            final AbstractAttractionModel attractionModelIn) throws PatriusException {
        supportedNames = supportedNamesIn;
        generateType = generateTypeIn;
        attractionModel = attractionModelIn;
        ephemerisLoader = new JPLEphemerisLoader(supportedNames, generateType);
    }

    /**
     * Load celestial body.
     *
     * @param name
     *        name of the celestial body
     * @return loaded celestial body
     * @throws PatriusException
     *         if the body cannot be loaded
     */
    @Override
    public CelestialBody loadCelestialBody(final String name) throws PatriusException {

        // Initialization
        final CelestialBody res;

        switch (generateType) {
            case SOLAR_SYSTEM_BARYCENTER:
                res = new JPLCelestialBody(name, EphemerisType.EARTH_MOON,
                    CelestialBodyFactory.EARTH_MOON, FramesFactory.getICRF());
                break;
            case EARTH_MOON:
                res = new JPLCelestialBody(name,
                    FramesFactory.getGCRF(), FramesFactory.getEMB());
                break;
            case EARTH:
                res = new Earth(name);
                break;
            case MOON:
                res = new JPLCelestialBody(name, FramesFactory.getGCRF());
                break;
            default:
                // General case
                res = new JPLCelestialBody(name, FramesFactory.getICRF());
                break;
        }

        // Return result
        return res;
    }

    /**
     * Get astronomical unit.
     *
     * @return astronomical unit in meters
     * @exception PatriusException
     *            if constants cannot be loaded
     */
    public double getLoadedAstronomicalUnit() throws PatriusException {
        return ephemerisLoader.getLoadedAstronomicalUnit();
    }

    /**
     * Get Earth/Moon mass ratio.
     *
     * @return Earth/Moon mass ratio
     * @exception PatriusException
     *            if constants cannot be loaded
     */
    public double getLoadedEarthMoonMassRatio() throws PatriusException {
        return ephemerisLoader.getLoadedEarthMoonMassRatio();
    }

    /**
     * Get the gravitational coefficient of a body.
     *
     * @param body
     *        body for which the gravitational coefficient is requested
     * @return gravitational coefficient in m<sup>3</sup>/s<sup>2</sup>
     * @exception PatriusException
     *            if constants cannot be loaded
     */
    public double getLoadedGravitationalCoefficient(final EphemerisType body)
        throws PatriusException {
        return ephemerisLoader.getLoadedGravitationalCoefficient(body);
    }

    /**
     * Get a constant defined in the ephemerides headers.
     * <p>
     * Note that since constants are defined in the JPL headers files, they are available as soon as one file is
     * available, even if it doesn't match the desired central date. This is because the header must be parsed before
     * the dates can be checked.
     * </p>
     * <p>
     * There are alternate names for constants since for example JPL names are different from INPOP names (Sun gravity:
     * GMS or GM_Sun, Mercury gravity: GM4 or GM_Mar...).
     * </p>
     *
     * @param names
     *        alternate names of the constant
     * @return value of the constant of NaN if the constant is not defined
     * @exception PatriusException
     *            if constants cannot be loaded
     */
    public double getLoadedConstant(final String... names) throws PatriusException {
        return ephemerisLoader.getLoadedConstant(names);
    }

    /**
     * Get the maximal chunks duration.
     *
     * @return chunks maximal duration in seconds
     */
    public double getMaxChunksDuration() {
        return ephemerisLoader.getMaxChunksDuration();
    }

    /**
     * Retrieves the equatorial radius and the flatness coefficient of a celestial body from the polar and equatorial
     * radiuses.
     *
     * @param ae
     *        the equatorial radius (m)
     * @param ap
     *        the polar radius (m)
     *
     * @return flatness coefficient
     */
    private static double computeFlatness(final double ae, final double ap) {
        // the flatness coefficient
        return MathLib.divide(ae - ap, ae);
    }

    /**
     * Builds a {@link OneAxisEllipsoid} as a default shape for the loaded celestial body.
     * In the case the celestial body is not physically defined, typically the case for barycenters, the default shape
     * returned is a null shape.
     *
     * @param name
     *            the body name
     * @param bodyFrame
     *            parent frame (usually it should be the ICRF centered on the parent body)
     *
     * @return the shape of the celestial body
     */
    private BodyShape buildDefaultBodyShape(final String name, final Frame bodyFrame) {

        // flatness coefficient of the celestial body
        final double flatness;
        // Ellipsoid to return
        final BodyShape shape;

        switch (generateType) {
            case SUN:
                // Sun
                shape = new OneAxisEllipsoid(SUN_RADIUS, 0, bodyFrame, name);
                break;
            case MERCURY:
                // Mercury
                shape = new OneAxisEllipsoid(MERCURY_RADIUS, 0, bodyFrame, name);
                break;
            case VENUS:
                // Venus
                shape = new OneAxisEllipsoid(VENUS_RADIUS, 0, bodyFrame, name);
                break;
            case EARTH:
                // Earth
                flatness = computeFlatness(EARTH_EQUATORIAL_RADIUS, EARTH_POLAR_RADIUS);
                shape = new OneAxisEllipsoid(EARTH_EQUATORIAL_RADIUS, flatness, bodyFrame, name);
                break;
            case MOON:
                // Moon
                shape = new OneAxisEllipsoid(MOON_RADIUS, 0, bodyFrame, name);
                break;
            case MARS:
                // Mars
                flatness = computeFlatness(MARS_EQUATORIAL_RADIUS, MARS_POLAR_RADIUS);
                shape = new OneAxisEllipsoid(MARS_EQUATORIAL_RADIUS, flatness, bodyFrame, name);
                break;
            case JUPITER:
                // Jupiter
                flatness = computeFlatness(JUPITER_EQUATORIAL_RADIUS, JUPITER_POLAR_RADIUS);
                shape = new OneAxisEllipsoid(JUPITER_EQUATORIAL_RADIUS, flatness, bodyFrame, name);
                break;
            case SATURN:
                // Saturn
                flatness = computeFlatness(SATURN_EQUATORIAL_RADIUS, SATURN_POLAR_RADIUS);
                shape = new OneAxisEllipsoid(SATURN_EQUATORIAL_RADIUS, flatness, bodyFrame, name);
                break;
            case URANUS:
                // Uranus
                flatness = computeFlatness(URANUS_EQUATORIAL_RADIUS, URANUS_POLAR_RADIUS);
                shape = new OneAxisEllipsoid(URANUS_EQUATORIAL_RADIUS, flatness, bodyFrame, name);
                break;
            case NEPTUNE:
                // Neptune
                flatness = computeFlatness(NEPTUNE_EQUATORIAL_RADIUS, NEPTUNE_POLAR_RADIUS);
                shape = new OneAxisEllipsoid(NEPTUNE_EQUATORIAL_RADIUS, flatness, bodyFrame, name);
                break;
            case PLUTO:
                // Pluto
                shape = new OneAxisEllipsoid(PLUTO_RADIUS, 0, bodyFrame, name);
                break;
            default:
                // A default null shape is set to Earth-Moon and Solar System barycenters
                shape = null;
                break;
        }

        return shape;
    }

    /**
     * Custom deserialization is needed.
     *
     * @param stream
     *        Object stream
     * @throws IOException if an I/O error occurs
     * @throws ClassNotFoundException if the class of a serialized object cannot be found
     */
    private void readObject(final ObjectInputStream stream) throws IOException,
        ClassNotFoundException {
        stream.defaultReadObject();
    }

    /** Local celestial body class. */
    private class JPLCelestialBody extends AbstractCelestialBody {

        /** Serializable UID. */
        private static final long serialVersionUID = -2941415197776129165L;

        /**
         * Simple constructor.
         *
         * @param name
         *            name of the body
         * @param parentFrame
         *            parent frame (usually it should be the ICRF centered on the parent body)
         * @exception PatriusException
         *                if gravitational coefficient cannot be retrieved
         */
        public JPLCelestialBody(final String name, final Frame parentFrame)
            throws PatriusException {
            super(name, attractionModel == null ? new NewtonianAttractionModel(
                    parentFrame, getLoadedGravitationalCoefficient(generateType))
                    : attractionModel, IAUPoleFactory
                    .getIAUPole(generateType), parentFrame);
            // ellipsoid default shape
            setShape(buildDefaultBodyShape(name, getRotatingFrameTrueModel()));
            setEphemeris(ephemerisLoader.loadCelestialBodyEphemeris(name));
        }

        /**
         * Simple constructor.
         *
         * @param name
         *            name of the body
         * @param parentFrame
         *            parent frame (usually it should be the ICRF centered on the parent body)
         * @param icrf
         *            ICRF frame
         * @exception PatriusException
         *                if gravitational coefficient cannot be retrieved
         */
        public JPLCelestialBody(final String name, final Frame parentFrame,
                final CelestialBodyFrame icrf)
            throws PatriusException {
            super(icrf, name, attractionModel == null ? new NewtonianAttractionModel(
                    parentFrame, getLoadedGravitationalCoefficient(generateType))
                    : attractionModel, IAUPoleFactory
                    .getIAUPole(generateType));
            // ellipsoid default shape
            setShape(buildDefaultBodyShape(name, getRotatingFrameTrueModel()));
            setEphemeris(ephemerisLoader.loadCelestialBodyEphemeris(name));
        }

        /**
         * Simple constructor.
         *
         * @param name
         *        name of the body
         * @param parentBody
         *        celestial body with respect to which this one is defined
         * @param parentName
         *        name of the parent body
         * @param icrf ICRF frame
         * @exception PatriusException
         *            if gravitational coefficient cannot be retrieved
         */
        public JPLCelestialBody(final String name,
                                final EphemerisType parentBody, final String parentName,
                                final CelestialBodyFrame icrf) throws PatriusException {
            this(name, CelestialBodyFactory.getBody(parentName).getICRF(), icrf);
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            final String result = super.toString();
            final StringBuilder builder = new StringBuilder(result);
            builder.append("- Ephemeris: JPL files (matching "
                    + supportedNames + ")");
            return builder.toString();
        }
    }

    /**
     * Earth body.
     */
    private class Earth implements CelestialBody {
        // Earth case
        // Not defined in JPL ephemeris data

        /** Serializable UID. */
        private static final long serialVersionUID = 800054277277715849L;

        /** Gravitational constant. */
        private final double gm;

        /** Default shape of Earth. */
        private BodyShape shape;

        /** Default gravitational attraction model. */
        private AbstractAttractionModel attractionModel;

        /** Name. */
        private final String name;

        /**
         * Constructor.
         * @param name name
         * @throws PatriusException thrown if failed
         */
        public Earth(final String name) throws PatriusException {
            this.name = name;
            shape = buildDefaultBodyShape(name, getRotatingFrameTrueModel());
            gm = getLoadedGravitationalCoefficient(EphemerisType.EARTH);
            attractionModel = new NewtonianAttractionModel(gm);
        }

        /** {@inheritDoc} */
        @Override
        public PVCoordinates getPVCoordinates(final AbsoluteDate date,
                                              final Frame frame) throws PatriusException {
            // Specific implementation for Earth:
            // The Earth is always exactly at the origin of its own inertial frame
            return getInertialFrameConstantModel().getPVCoordinates(date, frame);
        }

        /** {@inheritDoc} */
        @Override
        public CelestialBodyEphemeris getEphemeris() {
            return new EarthEphemeris();
        }

        /** {@inheritDoc} */
        @Override
        public void setEphemeris(final CelestialBodyEphemeris ephemerisIn) {
            // Nothing to do, Earth ephemeris cannot be set
        }

        /** {@inheritDoc} */
        @Override
        public Frame getNativeFrame(final AbsoluteDate date,
                final Frame frame) {
            return getInertialFrameConstantModel();
        }

        /** {@inheritDoc} */
        @Override
        public CelestialBodyFrame getICRF() {
            return FramesFactory.getGCRF();
        }

        /** {@inheritDoc} */
        @Override
        public CelestialBodyFrame getEME2000() {
            return FramesFactory.getEME2000();
        }

        /** {@inheritDoc} */
        @Override
        public CelestialBodyFrame getInertialFrameConstantModel() {
            return FramesFactory.getGCRF();
        }

        /** {@inheritDoc} */
        @Override
        public final CelestialBodyFrame getRotatingFrameTrueModel() throws PatriusException {
            return FramesFactory.getITRF();
        }

        /** {@inheritDoc} */
        @Override
        public String getName() {
            return name;
        }

        /** {@inheritDoc} */
        @Override
        public CelestialBodyFrame getInertialFrameMeanModel() {
            return FramesFactory.getMOD(true);
        }

        /** {@inheritDoc} */
        @Override
        public CelestialBodyFrame getInertialFrameTrueModel() throws PatriusException {
            return FramesFactory.getTOD(true);
        }

        /** {@inheritDoc} */
        @Override
        public CelestialBodyFrame getRotatingFrameConstantModel() throws PatriusException {
            throw new PatriusException(PatriusMessages.UNDEFINED_FRAME, "Earth constant rotating frame");
        }

        /** {@inheritDoc} */
        @Override
        public CelestialBodyFrame getRotatingFrameMeanModel() throws PatriusException {
            throw new PatriusException(PatriusMessages.UNDEFINED_FRAME, "Earth mean rotating frame");
        }

        /** {@inheritDoc} */
        @Override
        public BodyShape getShape() {
            return shape;
        }

        /** {@inheritDoc} */
        @Override
        public void setShape(final BodyShape shapeIn) {
            shape = shapeIn;
        }

        /** {@inheritDoc} */
        @Override
        public AbstractAttractionModel getAttractionModel() {
            return attractionModel;
        }

        /** {@inheritDoc} */
        @Override
        public void setAttractionModel(final AbstractAttractionModel modelIn) {
            attractionModel = modelIn;
        }

        /** {@inheritDoc} */
        @Override
        public double getGM() {
            return getAttractionModel().getMu();
        }

        /** {@inheritDoc} */
        @Override
        public void setGM(final double gmIn) {
            getAttractionModel().setMu(gmIn);
        }

        /** {@inheritDoc} */
        @Override
        public IAUPole getIAUPole() {
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public void setIAUPole(final IAUPole iauPoleIn) {
            // Nothing to do, this method is not supposed to be called
        }

        /**
         * Earth ephemeris.
         * 
         * @since 4.10
         */
        private class EarthEphemeris implements CelestialBodyEphemeris {

            /** Serial UID. */
            private static final long serialVersionUID = -6402878823738057930L;

            /** {@inheritDoc} */
            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date,
                    final Frame frame) throws PatriusException {
                return Earth.this.getPVCoordinates(date, frame);
            }

            /** {@inheritDoc} */
            @Override
            public Frame getNativeFrame(final AbsoluteDate date,
                    final Frame frame) throws PatriusException {
                return Earth.this.getInertialFrameConstantModel();
            }
        };
    }
}
