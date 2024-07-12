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
 * VERSION:4.11.1:DM:DM-88:30/06/2023:[PATRIUS] Complement FT 3319
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import fr.cnes.sirius.patrius.bodies.bsp.BSPEphemerisLoader;
import fr.cnes.sirius.patrius.forces.gravity.NewtonianGravityModel;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Loader for JPL ephemerides binary files BSP type.
 * It loads the whole {@link CelestialBody}. For {@link CelestialBodyEphemeris} loader only,
 * see dedicated class.
 * <p>
 * BSP ephemerides binary files contain ephemerides for any solar system body (depending on what is included in the
 * file).
 * </p>
 *
 * @author Emmanuel Bignon
 * 
 * @since 4.11.1
 */
public class BSPCelestialBodyLoader extends AbstractJPLCelestialBodyLoader {

    /** Default supported files name pattern for BSP files. */
    public static final String DEFAULT_BSP_SUPPORTED_NAMES = "^.*\\.bsp$";

    /** Serializable UID. */
    private static final long serialVersionUID = -8215287568823783794L;

    /**
     * Create a loader for JPL ephemerides binary files (BSP type).
     *
     * @param supportedNamesIn
     *        regular expression for supported files names
     */
    public BSPCelestialBodyLoader(final String supportedNamesIn) {
        super(supportedNamesIn, new BSPEphemerisLoader(supportedNamesIn));
    }

    /**
     * Load celestial body.
     *
     * @param name
     *        name of the celestial body
     * @return loaded celestial body
     * @throws PatriusException
     *         if the body cannot be loaded or body name is unknown
     */
    @Override
    public CelestialBody loadCelestialBody(final String name) throws PatriusException {
        // AS of 4.11.1, Earth is temporarily handled as regular BSP body
        //        final String nameUpper = name.toUpperCase(Locale.US);
        //        if (nameUpper.equals(EphemerisType.EARTH.getName().toUpperCase(Locale.US))) {
        //            // Specific Earth case
        //            return new Earth(nameUpper, getLoadedGravitationalCoefficient(EphemerisType.EARTH));
        //        } else {
        // Build body (generic case)
        // If name does not match any known ephemeris type, an exception will be thrown
        final CelestialBodyEphemeris ephemeris = getEphemerisLoader().loadCelestialBodyEphemeris(name);
        // Parent frame may be either ICRF or EME2000 depending on the convention
        final Frame parentFrame = ephemeris.getNativeFrame(null, null);
        final EphemerisType ephemerisType = EphemerisType.getEphemerisType(name);
        return new BSPCelestialBody(name, ephemeris, (BSPEphemerisLoader) getEphemerisLoader(), parentFrame,
                ephemerisType);
        //        }
    }

    /** Local celestial body class. */
    private class BSPCelestialBody extends AbstractCelestialBody {

        /** Serializable UID. */
        private static final long serialVersionUID = -2941415197776129165L;

        /**
         * Constructor.
         *
         * @param name
         *        name of the body
         * @param ephemeris
         *        ephemeris
         * @param ephemerisLoader
         *        ephemeris loader
         * @param parentFrame
         *        parent frame
         * @param ephemerisType
         *        ephemeris type
         * @exception PatriusException
         *            if gravitational coefficient cannot be retrieved
         */
        public BSPCelestialBody(final String name,
                final CelestialBodyEphemeris ephemeris,
                final BSPEphemerisLoader ephemerisLoader,
                final Frame parentFrame,
                final EphemerisType ephemerisType) throws PatriusException {
            super(name, null, IAUPoleFactory.getIAUPole(ephemerisType), parentFrame,
                    ephemerisLoader.getConvention());
            setShape(buildDefaultBodyShape(name, getRotatingFrame(IAUPoleModelType.TRUE), ephemerisType));
            setEphemeris(ephemeris);
            setGravityModel(new NewtonianGravityModel(getICRF(), getLoadedGravitationalCoefficient(ephemerisType)));
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            final String result = super.toString();
            final StringBuilder builder = new StringBuilder(result);
            builder.append("- Ephemeris: JPL files (BSP type, matching " + getSupportedNames() + ")");
            return builder.toString();
        }
    }
}
