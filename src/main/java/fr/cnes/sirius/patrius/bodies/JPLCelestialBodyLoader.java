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
 * VERSION:4.13:DM:DM-5:08/12/2023:[PATRIUS] Orientation d'un corps celeste sous forme de quaternions
 * VERSION:4.13:DM:DM-3:08/12/2023:[PATRIUS] Distinction entre corps celestes et barycentres
 * VERSION:4.13:FA:FA-111:08/12/2023:[PATRIUS] Problemes lies à  l'utilisation des bsp
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11.1:DM:DM-49:30/06/2023:[PATRIUS] Extraction arbre des reperes SPICE et link avec CelestialBodyFactory
 * VERSION:4.11:FA:FA-3278:22/05/2023:[PATRIUS] Doublon de classes pour le corps celeste Earth
 * VERSION:4.11:DM:DM-3311:22/05/2023:[PATRIUS] Evolutions mineures sur CelestialBody, shape et reperes
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration gestion attractions gravitationnelles
 * VERSION:4.11:DM:DM-3248:22/05/2023:[PATRIUS] Renommage de GeodeticPoint en GeodeticCoordinates
 * VERSION:4.11:FA:FA-3314:22/05/2023:[PATRIUS] Anomalie evaluation ForceModel lorsque SpacecraftState en ITRF
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

import fr.cnes.sirius.patrius.bodies.bsp.BSPEphemerisLoader.SpiceJ2000ConventionEnum;
import fr.cnes.sirius.patrius.forces.gravity.GravityModel;
import fr.cnes.sirius.patrius.forces.gravity.NewtonianGravityModel;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Loader for JPL ephemerides binary files (DE 4xx and BSP) and similar formats (INPOP 06/08/10).
 * It loads the whole {@link CelestialPoint}. For {@link CelestialBodyEphemeris} loader only,
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
 * 423/DE 430 and BSP.
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
 * @author Emmanuel Bignon
 * 
 * @since 4.11.1
 */
public class JPLCelestialBodyLoader extends AbstractJPLCelestialBodyLoader {

    /** Default supported files name pattern for JPL DE files. */
    public static final String DEFAULT_DE_SUPPORTED_NAMES = "^[lu]nx[mp](\\d\\d\\d\\d)\\.(?:4\\d\\d)$";

    /** Default supported files name pattern for IMCCE INPOP files. */
    public static final String DEFAULT_INPOP_SUPPORTED_NAMES = "^inpop.*\\.dat$";

    /** Serializable UID. */
    private static final long serialVersionUID = -8215287568823783794L;

    /** Gravitational attraction model. */
    private final GravityModel gravityModel;

    /** Ephemeris type to generate. */
    private final EphemerisType ephemerisType;

    /**
     * Create a loader for JPL ephemerides binary files (DE-INPOP type).
     *
     * @param supportedNamesIn
     *        regular expression for supported files names
     * @param generateTypeIn
     *        ephemeris type to generate
     */
    public JPLCelestialBodyLoader(final String supportedNamesIn,
            final EphemerisType generateTypeIn) {
        this(supportedNamesIn, generateTypeIn, null);
    }

    /**
     * Create a loader for JPL ephemerides binary files (DE-INPOP type).
     *
     * @param supportedNamesIn
     *        regular expression for supported files names
     * @param generateTypeIn
     *        ephemeris type to generate
     * @param gravityModelIn
     *        gravitational attraction model
     */
    public JPLCelestialBodyLoader(final String supportedNamesIn,
            final EphemerisType generateTypeIn,
            final GravityModel gravityModelIn) {
        super(supportedNamesIn, new JPLHistoricEphemerisLoader(supportedNamesIn, generateTypeIn));
        this.gravityModel = gravityModelIn;
        this.ephemerisType = generateTypeIn;
    }

    /** {@inheritDoc} */
    @Override
    public CelestialPoint loadCelestialPoint(final String name) throws PatriusException {

        // Initialization
        final CelestialPoint res;

        switch (this.ephemerisType) {
            case SOLAR_SYSTEM_BARYCENTER:
                final double gmSSB = getLoadedGravitationalCoefficient(this.ephemerisType);
                final CelestialBodyEphemeris ephemerisSSB = getEphemerisLoader().loadCelestialBodyEphemeris(name);
                res = new BasicCelestialPoint(FramesFactory.getICRF(), name, gmSSB, ephemerisSSB);
                break;
            case EARTH_MOON:
                final double gmEMB = getLoadedGravitationalCoefficient(this.ephemerisType);
                final CelestialBodyEphemeris ephemerisEMB = getEphemerisLoader().loadCelestialBodyEphemeris(name);
                res = new BasicCelestialPoint(FramesFactory.getEMB(), name, gmEMB, ephemerisEMB);
                break;
            default:
                // General case: CelestialBody
                res = loadCelestialBody(name);
                break;
        }

        // Return result
        return res;
    }

    /** {@inheritDoc} */
    @Override
    public CelestialBody loadCelestialBody(final String name) throws PatriusException {

        // Initialization
        final CelestialBody res;

        switch (ephemerisType) {
            case SOLAR_SYSTEM_BARYCENTER:
                // CelestialBody cannot be built
                throw new PatriusException(PatriusMessages.NOT_A_CELESTIAL_BODY, ephemerisType);
            case EARTH_MOON:
                // CelestialBody cannot be built
                throw new PatriusException(PatriusMessages.NOT_A_CELESTIAL_BODY, ephemerisType);
            case EARTH:
                res = new Earth(name, getLoadedGravitationalCoefficient(EphemerisType.EARTH));
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

    /** {@inheritDoc} */
    @Override
    public String getName(final String patriusName) {
        // Body name is exactly PATRIUS body name
        return patriusName;
    }

    /** Local celestial body class. */
    private class JPLCelestialBody extends AbstractCelestialBody {

        /** Serializable UID. */
        private static final long serialVersionUID = -2941415197776129165L;

        /**
         * Simple constructor.
         *
         * @param name
         *        name of the body
         * @param parentFrame
         *        parent frame (usually it should be the ICRF centered on the parent body)
         * @exception PatriusException
         *            if gravitational coefficient cannot be retrieved
         */
        public JPLCelestialBody(final String name,
                final Frame parentFrame) throws PatriusException {
            super(name, JPLCelestialBodyLoader.this.gravityModel, IAUPoleFactory
                .getIAUPole(JPLCelestialBodyLoader.this.ephemerisType), parentFrame,
                    SpiceJ2000ConventionEnum.ICRF, getEphemerisLoader().loadCelestialBodyEphemeris(name));
            // ellipsoid default shape
            if (JPLCelestialBodyLoader.this.ephemerisType != null) {
                setShape(buildDefaultBodyShape(name, getRotatingFrame(IAUPoleModelType.TRUE),
                    JPLCelestialBodyLoader.this.ephemerisType));
                setGravityModel(new NewtonianGravityModel(getICRF(),
                    getLoadedGravitationalCoefficient(JPLCelestialBodyLoader.this.ephemerisType)));
            }
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            final String result = super.toString();
            final StringBuilder builder = new StringBuilder(result);
            builder.append("- Ephemeris: JPL files (matching " + getSupportedNames() + ")");
            return builder.toString();
        }
    }
}
