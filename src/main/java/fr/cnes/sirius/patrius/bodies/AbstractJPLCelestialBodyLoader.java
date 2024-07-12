/**
 * Copyright 2023-2023 CNES
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
 * VERSION:4.11.1:DM:DM-49:30/06/2023:[PATRIUS] Extraction arbre des reperes SPICE et link avec CelestialBodyFactory
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Abstract class for all JPL celestial body loaders.
 *
 * @author Emmanuel Bignon
 * 
 * @since 4.11.1
 */
public abstract class AbstractJPLCelestialBodyLoader implements CelestialBodyLoader {

    /** Serializable UID. */
    private static final long serialVersionUID = -8215287568823783794L;

    /** Sun's radius constant (2009 IAU report). */
    private static final double SUN_RADIUS = 696000000.;
    /** Mercury's radius constant (2009 IAU report). */
    private static final double MERCURY_RADIUS = 2439700.;
    /** Venus's radius constant (2009 IAU report). */
    private static final double VENUS_RADIUS = 6051800.;
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

    /** Ephemeris loader. */
    private final JPLEphemerisLoader ephemerisLoader;

    /**
     * Create a loader for JPL ephemerides binary files (DE-INPOP type).
     *
     * @param supportedNamesIn
     *        regular expression for supported files names
     * @param ephemerisLoader
     *        ephemeris loader
     */
    public AbstractJPLCelestialBodyLoader(final String supportedNamesIn,
            final JPLEphemerisLoader ephemerisLoader) {
        this.supportedNames = supportedNamesIn;
        this.ephemerisLoader = ephemerisLoader;
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
    public double getLoadedGravitationalCoefficient(final EphemerisType body) throws PatriusException {
        return ephemerisLoader.getLoadedGravitationalCoefficient(body);
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
    private static double computeFlatness(final double ae,
            final double ap) {
        // the flatness coefficient
        return MathLib.divide(ae - ap, ae);
    }

    /**
     * Builds a {@link OneAxisEllipsoid} as a default shape for the loaded celestial body.
     * In the case the celestial body is not physically defined, typically the case for barycenters, the default shape
     * returned is a null shape.
     *
     * @param name
     *        the body name
     * @param bodyFrame
     *        parent frame (usually it should be the ICRF centered on the parent body)
     * @param ephemerisType
     *        ephemeris type
     *
     * @return the shape of the celestial body
     */
    protected BodyShape buildDefaultBodyShape(final String name,
            final Frame bodyFrame,
            final EphemerisType ephemerisType) {

        // flatness coefficient of the celestial body
        final double flatness;
        // Ellipsoid to return
        final BodyShape shape;

        switch (ephemerisType) {
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
     * Returns the ephemeris loader.
     * @return the ephemeris loader
     */
    public JPLEphemerisLoader getEphemerisLoader() {
        return ephemerisLoader;
    }

    /**
     * Returns the supported file names.
     * @return the supported file names
     */
    public String getSupportedNames() {
        return supportedNames;
    }
}
