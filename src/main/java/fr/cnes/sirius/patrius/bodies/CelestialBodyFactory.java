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
 * HISTORY
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11.1:DM:DM-49:30/06/2023:[PATRIUS] Extraction arbre des reperes SPICE et link avec CelestialBodyFactory
 * VERSION:4.11:FA:FA-3269:22/05/2023:[PATRIUS] Probleme utilisation CelestialBodyFactory en multi-thread
 * VERSION:4.11:FA:FA-3314:22/05/2023:[PATRIUS] Anomalie evaluation ForceModel lorsque SpacecraftState en ITRF
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2800:18/05/2021:Compatibilite ephemerides planetaires
 * VERSION:4.7:FA:FA-2897:18/05/2021:Alignement Soleil-Sat-Terre non supporté pour la SRP 
 * VERSION:4.7:FA:FA-2923:18/05/2021:Parent frame in class LocalOrbitalFrame 
 * VERSION:4.7:DM:DM-2703:18/05/2021: Acces facilite aux donnees d'environnement (application interplanetaire) 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:274:24/10/2014:third body ephemeris clearing modified
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import fr.cnes.sirius.patrius.data.DataProvidersManager;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

//CHECKSTYLE: stop NestedBlockDepth check
//Reason: model - Orekit code kept as such

/**
 * Factory class for bodies of the solar system.
 * <p>
 * The {@link #getSun() Sun}, the {@link #getMoon() Moon} and the planets (including the Pluto dwarf planet) are
 * provided by this factory. In addition, two important points are provided for convenience: the
 * {@link #getSolarSystemBarycenter() solar system barycenter} and the {@link #getEarthMoonBarycenter() Earth-Moon
 * barycenter}.
 * </p>
 * <p>
 * The underlying body-centered frames are either direct children of
 * {@link fr.cnes.sirius.patrius.frames.FramesFactory#getGCRF() GCRF} (for {@link #getMoon() Moon} and
 * {@link #getEarthMoonBarycenter() Earth-Moon barycenter}) or children from other body-centered frames. For example,
 * the path from GCRF to Jupiter-centered frame is: GCRF, Earth-Moon barycenter centered, solar system barycenter
 * centered, Jupiter-centered. The frame axes are always parallel to
 * {@link fr.cnes.sirius.patrius.frames.FramesFactory#getGCRF() GCRF} frame axes.
 * </p>
 * <p>
 * The position of the bodies provided by this class are interpolated using the JPL DE 4XX, IMCCE INPOP or JPL BSP
 * ephemerides.
 * <br/>
 * Accepted JPL formats are: DE200/DE 202/DE 403/DE 405/DE 406/DE 410/DE 413/DE 414/DE 418/DE 421/DE 422/DE 423 and DE
 * 430. <br/>
 * Accepted IMCCE formats are: INPOP 06b/06c/08a/10a/10b/10e/13c/17a/19a.
 * </p>
 * <p>
 * Loading of Celestial body data follows PATRIUS standard way of handling data:
 * <ul>
 * Data are automatically loaded based on provided {@link CelestialBodyLoader}.
 * </ul>
 * <ul>
 * User can provide its own implementation of these loaders and provide them using
 * {@link #addCelestialBodyLoader(String, CelestialBodyLoader)}.
 * </ul>
 * <ul>
 * By default some loaders are added (see below). Loaders are used in the defined order below. These loaders will use
 * data provided by {@link DataProvidersManager}.
 * </ul>
 * <ul>
 * List of loaders can be cleared and reordered using {@link #clearCelestialBodyLoaders()}.
 * </ul>
 * </p>
 * <p>
 * By default, some loaders are added in the following order:<br/>
 * <ul>
 * <li>{@link JPLCelestialBodyLoader}: JPL DE ephemeris (see above for all accepted formats)</li>
 * <li>{@link JPLCelestialBodyLoader}: IMCCE INPOP ephemeris (see above for all accepted formats)</li>
 * <li>{@link BSPCelestialBodyLoader}: JPL BSP ephemeris (see above for all accepted formats)</li>
 * </ul>
 * There is no included data in PATRIUS by default. <br/>
 * Once loaded, data are stored in static variables and are used to retrieve planetary bodies through the various
 * available getters.
 * </p>
 * 
 * @author Luc Maisonobe
 */
public final class CelestialBodyFactory {

    /**
     * Predefined name for solar system barycenter.
     * 
     * @see #getBody(String)
     */
    public static final String SOLAR_SYSTEM_BARYCENTER = "solar system barycenter";

    /**
     * Predefined name for Sun.
     * 
     * @see #getBody(String)
     */
    public static final String SUN = "Sun";

    /**
     * Predefined name for Mercury.
     * 
     * @see #getBody(String)
     */
    public static final String MERCURY = "Mercury";

    /**
     * Predefined name for Venus.
     * 
     * @see #getBody(String)
     */
    public static final String VENUS = "Venus";

    /**
     * Predefined name for Earth-Moon barycenter.
     * 
     * @see #getBody(String)
     */
    public static final String EARTH_MOON = "Earth-Moon barycenter";

    /**
     * Predefined name for Earth-Moon barycenter in BSP files.
     * 
     * @see #getBody(String)
     */
    public static final String EARTH_MOON_BSP = "EARTH BARYCENTER";
    
    /**
     * Predefined name for Earth.
     * 
     * @see #getBody(String)
     */
    public static final String EARTH = "Earth";

    /**
     * Predefined name for Moon.
     * 
     * @see #getBody(String)
     */
    public static final String MOON = "Moon";

    /**
     * Predefined name for Mars.
     * 
     * @see #getBody(String)
     */
    public static final String MARS = "Mars";

    /**
     * Predefined name for Jupiter.
     * 
     * @see #getBody(String)
     */
    public static final String JUPITER = "Jupiter";

    /**
     * Predefined name for Saturn.
     * 
     * @see #getBody(String)
     */
    public static final String SATURN = "Saturn";

    /**
     * Predefined name for Uranus.
     * 
     * @see #getBody(String)
     */
    public static final String URANUS = "Uranus";

    /**
     * Predefined name for Neptune.
     * 
     * @see #getBody(String)
     */
    public static final String NEPTUNE = "Neptune";

    /**
     * Predefined name for Pluto.
     * 
     * @see #getBody(String)
     */
    public static final String PLUTO = "Pluto";

    /** Celestial body loaders map. */
    private static final Map<String, CopyOnWriteArrayList<CelestialBodyLoader>> LOADERS_MAP = new ConcurrentHashMap<>();

    /** Celestial body map. */
    private static final Map<String, CelestialBody> CELESTIAL_BODIES_MAP = new ConcurrentHashMap<>();

    /**
     * Private constructor.
     * <p>
     * This class is a utility class, it should neither have a public nor a default constructor. This private
     * constructor prevents the compiler from generating one automatically.
     * </p>
     */
    private CelestialBodyFactory() {
    }

    /**
     * Add a loader for celestial bodies.
     * 
     * @param name
     *        name of the body (may be one of the predefined names or a user-defined name)
     * @param loader
     *        custom loader to add for the body
     * @see #addDefaultCelestialBodyLoader(String)
     * @see #clearCelestialBodyLoaders(String)
     * @see #clearCelestialBodyLoaders()
     */
    public static void addCelestialBodyLoader(final String name,
            final CelestialBodyLoader loader) {
        CopyOnWriteArrayList<CelestialBodyLoader> loaders = LOADERS_MAP.get(name);
        if (loaders == null) {
            loaders = new CopyOnWriteArrayList<>();
            LOADERS_MAP.put(name, loaders);
        }
        loaders.add(loader);
    }

    /**
     * Add the default loaders for all predefined celestial bodies.
     * 
     * @param supportedNames
     *        regular expression for supported files names
     *        (may be null if the default JPL file names are used)
     *        <p>
     *        The default loaders look for DE405 or DE406 JPL ephemerides.
     *        </p>
     * @see <a href="ftp://ssd.jpl.nasa.gov/pub/eph/planets/unix/de405">DE405 JPL ephemerides</a>
     * @see <a href="ftp://ssd.jpl.nasa.gov/pub/eph/planets/unix/de406">DE406 JPL ephemerides</a>
     * @see #addCelestialBodyLoader(String, CelestialBodyLoader)
     * @see #addDefaultCelestialBodyLoader(String)
     * @see #clearCelestialBodyLoaders(String)
     * @see #clearCelestialBodyLoaders()
     * @exception PatriusException
     *            if the header constants cannot be read
     */
    public static void addDefaultCelestialBodyLoader(final String supportedNames) throws PatriusException {
        // Add all loaders
        // Defaults loaders are JPLEphemeridesLoader (DE or INPOP)
        // Hence all JPL bodies are included
        //
        addDefaultCelestialBodyLoader(SOLAR_SYSTEM_BARYCENTER, supportedNames);
        addDefaultCelestialBodyLoader(SUN, supportedNames);
        addDefaultCelestialBodyLoader(MERCURY, supportedNames);
        addDefaultCelestialBodyLoader(VENUS, supportedNames);
        addDefaultCelestialBodyLoader(EARTH_MOON, supportedNames);
        addDefaultCelestialBodyLoader(EARTH, supportedNames);
        addDefaultCelestialBodyLoader(MOON, supportedNames);
        addDefaultCelestialBodyLoader(MARS, supportedNames);
        addDefaultCelestialBodyLoader(JUPITER, supportedNames);
        addDefaultCelestialBodyLoader(SATURN, supportedNames);
        addDefaultCelestialBodyLoader(URANUS, supportedNames);
        addDefaultCelestialBodyLoader(NEPTUNE, supportedNames);
        addDefaultCelestialBodyLoader(PLUTO, supportedNames);
    }

    /**
     * Add the default loaders for celestial bodies.
     * 
     * @param name
     *        name of the body (if not one of the predefined names, the method does nothing)
     * @param supportedNames
     *        regular expression for supported files names
     *        (may be null if the default JPL file names are used)
     *        <p>
     *        The default loaders look for DE405 or DE406 JPL ephemerides.
     *        </p>
     * @see <a href="ftp://ssd.jpl.nasa.gov/pub/eph/planets/unix/de405">DE405 JPL ephemerides</a>
     * @see <a href="ftp://ssd.jpl.nasa.gov/pub/eph/planets/unix/de406">DE406 JPL ephemerides</a>
     * @see #addCelestialBodyLoader(String, CelestialBodyLoader)
     * @see #addDefaultCelestialBodyLoader(String)
     * @see #clearCelestialBodyLoaders(String)
     * @see #clearCelestialBodyLoaders()
     * @exception PatriusException
     *            if the header constants cannot be read
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    public static void addDefaultCelestialBodyLoader(final String name,
            final String supportedNames) throws PatriusException {
        // CHECKSTYLE: resume CyclomaticComplexity check

        // Add all loaders
        // Defaults loaders are JPLEphemeridesLoader
        // Hence all JPL bodies are included:
        // - Earth
        // - Planets including Pluto
        // - Sun

        // Get loader
        CelestialBodyLoader loader = null;
        if (name.equals(SOLAR_SYSTEM_BARYCENTER)) {
            loader = new JPLCelestialBodyLoader(supportedNames, EphemerisType.SOLAR_SYSTEM_BARYCENTER);
        } else if (name.equals(SUN)) {
            loader = new JPLCelestialBodyLoader(supportedNames, EphemerisType.SUN);
        } else if (name.equals(MERCURY)) {
            loader = new JPLCelestialBodyLoader(supportedNames, EphemerisType.MERCURY);
        } else if (name.equals(VENUS)) {
            loader = new JPLCelestialBodyLoader(supportedNames, EphemerisType.VENUS);
        } else if (name.equals(EARTH_MOON)) {
            loader = new JPLCelestialBodyLoader(supportedNames, EphemerisType.EARTH_MOON);
        } else if (name.equals(EARTH)) {
            loader = new JPLCelestialBodyLoader(supportedNames, EphemerisType.EARTH);
        } else if (name.equals(MOON)) {
            loader = new JPLCelestialBodyLoader(supportedNames, EphemerisType.MOON);
        } else if (name.equals(MARS)) {
            loader = new JPLCelestialBodyLoader(supportedNames, EphemerisType.MARS);
        } else if (name.equals(JUPITER)) {
            loader = new JPLCelestialBodyLoader(supportedNames, EphemerisType.JUPITER);
        } else if (name.equals(SATURN)) {
            loader = new JPLCelestialBodyLoader(supportedNames, EphemerisType.SATURN);
        } else if (name.equals(URANUS)) {
            loader = new JPLCelestialBodyLoader(supportedNames, EphemerisType.URANUS);
        } else if (name.equals(NEPTUNE)) {
            loader = new JPLCelestialBodyLoader(supportedNames, EphemerisType.NEPTUNE);
        } else if (name.equals(PLUTO)) {
            loader = new JPLCelestialBodyLoader(supportedNames, EphemerisType.PLUTO);
        }

        // Add loader if not null
        if (loader != null) {
            addCelestialBodyLoader(name, loader);
        }

    }

    /**
     * Clear loaders for one celestial body.
     * 
     * @param name
     *        name of the body
     * @see #addCelestialBodyLoader(String, CelestialBodyLoader)
     * @see #clearCelestialBodyLoaders()
     */
    public static void clearCelestialBodyLoaders(final String name) {
        LOADERS_MAP.remove(name);
        CELESTIAL_BODIES_MAP.remove(name);
    }

    /**
     * Clear loaders for all celestial bodies.
     * 
     * @see #addCelestialBodyLoader(String, CelestialBodyLoader)
     * @see #clearCelestialBodyLoaders(String)
     */
    public static void clearCelestialBodyLoaders() {
        LOADERS_MAP.clear();
        CELESTIAL_BODIES_MAP.clear();
    }

    /**
     * Get the solar system barycenter aggregated body.
     * 
     * @return solar system barycenter aggregated body
     * @exception PatriusException
     *            if the celestial body cannot be built
     */
    public static CelestialBody getSolarSystemBarycenter() throws PatriusException {
        return getBody(SOLAR_SYSTEM_BARYCENTER);
    }

    /**
     * Get the Sun singleton body.
     * 
     * @return Sun body
     * @exception PatriusException
     *            if the celestial body cannot be built
     */
    public static CelestialBody getSun() throws PatriusException {
        return getBody(SUN);
    }

    /**
     * Get the Mercury singleton body.
     * 
     * @return Sun body
     * @exception PatriusException
     *            if the celestial body cannot be built
     */
    public static CelestialBody getMercury() throws PatriusException {
        return getBody(MERCURY);
    }

    /**
     * Get the Venus singleton body.
     * 
     * @return Venus body
     * @exception PatriusException
     *            if the celestial body cannot be built
     */
    public static CelestialBody getVenus() throws PatriusException {
        return getBody(VENUS);
    }

    /**
     * Get the Earth-Moon barycenter singleton bodies pair.
     * 
     * @return Earth-Moon barycenter bodies pair
     * @exception PatriusException
     *            if the celestial body cannot be built
     */
    public static CelestialBody getEarthMoonBarycenter() throws PatriusException {
        CelestialBody res;
        try {
            // Try JPL ephemeris
            res = getBody(EARTH_MOON);
        } catch (final PatriusException e) {
            // Try BSP ephemeris
            res = getBody(EARTH_MOON_BSP);
        }
        return res;
    }

    /**
     * Get the Earth singleton body.
     * 
     * @return Earth body
     * @exception PatriusException
     *            if the celestial body cannot be built
     */
    public static CelestialBody getEarth() throws PatriusException {
        return getBody(EARTH);
    }

    /**
     * Get the Moon singleton body.
     * 
     * @return Moon body
     * @exception PatriusException
     *            if the celestial body cannot be built
     */
    public static CelestialBody getMoon() throws PatriusException {
        return getBody(MOON);
    }

    /**
     * Get the Mars singleton body.
     * 
     * @return Mars body
     * @exception PatriusException
     *            if the celestial body cannot be built
     */
    public static CelestialBody getMars() throws PatriusException {
        return getBody(MARS);
    }

    /**
     * Get the Jupiter singleton body.
     * 
     * @return Jupiter body
     * @exception PatriusException
     *            if the celestial body cannot be built
     */
    public static CelestialBody getJupiter() throws PatriusException {
        return getBody(JUPITER);
    }

    /**
     * Get the Saturn singleton body.
     * 
     * @return Saturn body
     * @exception PatriusException
     *            if the celestial body cannot be built
     */
    public static CelestialBody getSaturn() throws PatriusException {
        return getBody(SATURN);
    }

    /**
     * Get the Uranus singleton body.
     * 
     * @return Uranus body
     * @exception PatriusException
     *            if the celestial body cannot be built
     */
    public static CelestialBody getUranus() throws PatriusException {
        return getBody(URANUS);
    }

    /**
     * Get the Neptune singleton body.
     * 
     * @return Neptune body
     * @exception PatriusException
     *            if the celestial body cannot be built
     */
    public static CelestialBody getNeptune() throws PatriusException {
        return getBody(NEPTUNE);
    }

    /**
     * Get the Pluto singleton body.
     * 
     * @return Pluto body
     * @exception PatriusException
     *            if the celestial body cannot be built
     */
    public static CelestialBody getPluto() throws PatriusException {
        return getBody(PLUTO);
    }

    /**
     * Get a celestial body.
     * <p>
     * If no {@link CelestialBodyLoader} has been added by calling
     * {@link #addCelestialBodyLoader(String, CelestialBodyLoader)
     * addCelestialBodyLoader} or if {@link #clearCelestialBodyLoaders(String)
     * clearCelestialBodyLoaders} has been called afterwards, the {@link #addDefaultCelestialBodyLoader(String, String)
     * addDefaultCelestialBodyLoader} method will be called automatically, once with the default name for JPL DE
     * ephemerides and once with the default name for IMCCE INPOP files.
     * </p>
     * 
     * @param name
     *        name of the celestial body
     * @return celestial body
     * @exception PatriusException
     *            if the celestial body cannot be built
     */
    public static CelestialBody getBody(final String name) throws PatriusException {
        // Initialization
        CelestialBody body = null;
        // Get body
        body = CELESTIAL_BODIES_MAP.get(name);
        if (body == null) {
            // Body has not been loaded: try to load it
            List<CelestialBodyLoader> loaders = LOADERS_MAP.get(name);
            if ((loaders == null) || loaders.isEmpty()) {
                // No loaders: add default loaders
                addDefaultCelestialBodyLoader(name, JPLCelestialBodyLoader.DEFAULT_DE_SUPPORTED_NAMES);
                addDefaultCelestialBodyLoader(name, JPLCelestialBodyLoader.DEFAULT_INPOP_SUPPORTED_NAMES);
                addCelestialBodyLoader(name, new BSPCelestialBodyLoader(
                        BSPCelestialBodyLoader.DEFAULT_BSP_SUPPORTED_NAMES));
                loaders = LOADERS_MAP.get(name);
            }
            PatriusException delayedException = null;
            CelestialBody body2 = null;
            for (final CelestialBodyLoader loader : loaders) {
                try {
                    // Try to load body
                    body2 = loader.loadCelestialBody(name);
                    if (body2 != null) {
                        break;
                    }
                } catch (final PatriusException oe) {
                    delayedException = oe;
                }
            }
            if (body2 == null) {
                // No data found for required body
                throw (delayedException == null) ? new PatriusException(
                        PatriusMessages.NO_DATA_LOADED_FOR_CELESTIAL_BODY, name) : delayedException;
            }
            body = body2;

            // save the body
            CELESTIAL_BODIES_MAP.put(name, body);
        }

        // Return the body
        return body;

    }

    /**
     * Returns the celestial bodies map available in the factory.
     * @return the celestial bodies map available in the factory
     */
    public static Map<String, CelestialBody> getBodies() {
        return CELESTIAL_BODIES_MAP;
    }

    // CHECKSTYLE: resume NestedBlockDepth check
}
