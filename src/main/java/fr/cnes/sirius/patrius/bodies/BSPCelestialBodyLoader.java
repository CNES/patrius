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
 * VERSION:4.13:FA:FA-112:08/12/2023:[PATRIUS] Probleme si Earth est utilise comme corps pivot pour mar097.bsp
 * VERSION:4.13:DM:DM-3:08/12/2023:[PATRIUS] Distinction entre corps celestes et barycentres
 * VERSION:4.13:DM:DM-132:08/12/2023:[PATRIUS] Suppression de la possibilite
 * de convertir les sorties de VacuumSignalPropagation
 * VERSION:4.13:DM:DM-120:08/12/2023:[PATRIUS] Merge de la branche patrius-for-lotus dans Patrius
 * VERSION:4.13:FA:FA-111:08/12/2023:[PATRIUS] Problemes lies à  l'utilisation des bsp
 * VERSION:4.11.1:DM:DM-49:30/06/2023:[PATRIUS] Extraction arbre des reperes SPICE et link avec CelestialBodyFactory
 * VERSION:4.11.1:DM:DM-88:30/06/2023:[PATRIUS] Complement FT 3319
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import fr.cnes.sirius.patrius.bodies.bsp.BSPEphemerisLoader;
import fr.cnes.sirius.patrius.bodies.bsp.BSPEphemerisLoader.SpiceJ2000ConventionEnum;
import fr.cnes.sirius.patrius.forces.gravity.NewtonianGravityModel;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Loader for JPL ephemerides binary files BSP type.<br>
 * It loads the whole {@link CelestialPoint}. For {@link CelestialBodyEphemeris} loader only, see dedicated class.
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

    /** Predefined name for Earth-Moon barycenter in BSP files. */
    public static final String EARTH_MOON = "EARTH BARYCENTER";

    /** Serializable UID. */
    private static final long serialVersionUID = -8215287568823783794L;

    /** Objects to be built as @link {@link CelestialPoint} instead of {@link CelestialBody}. */
    private final List<String> celestialPointsNames;

    /**
     * Create a loader for JPL ephemerides binary files (BSP type).
     *
     * @param supportedNamesIn
     *        regular expression for supported files names
     */
    public BSPCelestialBodyLoader(final String supportedNamesIn) {
        super(supportedNamesIn, new BSPEphemerisLoader(supportedNamesIn));
        this.celestialPointsNames = new ArrayList<>();
    }

    /**
     * Declare a name of a body as a {@link CelestialPoint}.<br>
     * Object will be built as {@link CelestialPoint} instead of {@link CelestialBody}.
     * 
     * @param name
     *        a body name
     */
    public void declareAsCelestialPoint(final String name) {
        this.celestialPointsNames.add(name);
    }

    /** {@inheritDoc} */
    @Override
    public CelestialPoint loadCelestialPoint(final String name) throws PatriusException {
        // Get ephemeris type
        final EphemerisType ephemerisType = EphemerisType.getEphemerisType(name);

        final boolean isBarycenter = EphemerisType.SOLAR_SYSTEM_BARYCENTER.equals(ephemerisType)
                || EphemerisType.EARTH_MOON.equals(ephemerisType);

        // Build body
        if (isBarycenter || this.celestialPointsNames.contains(name) || ephemerisType == null) {
            // Barycenter or unknown body
            // Ephemeris
            final CelestialBodyEphemeris ephemeris;
            if (CelestialBodyFactory.SOLAR_SYSTEM_BARYCENTER.equals(name)) {
                // Specific case for SSB which is not defined in BSP files
                ephemeris = new SSBEphemeris();
            } else {
                // General case
                ephemeris = getEphemerisLoader().loadCelestialBodyEphemeris(name);
            }
            final double gm = ephemerisType == null ? 0 : getLoadedGravitationalCoefficient(ephemerisType);
            final SpiceJ2000ConventionEnum convention = ((BSPEphemerisLoader) getEphemerisLoader()).getConvention();
            if (convention.equals(SpiceJ2000ConventionEnum.ICRF)) {
                // Specific handling of known barycenter: link to predefined keys
                if (EphemerisType.SOLAR_SYSTEM_BARYCENTER.equals(ephemerisType)) {
                    return new BasicCelestialPoint(name, gm, ephemeris, FramesFactory.getICRF());
                }
                if (EphemerisType.EARTH_MOON.equals(ephemerisType)) {
                    return new BasicCelestialPoint(name, gm, ephemeris, FramesFactory.getEMB());
                }
            }
            final Frame parentFrame = ephemeris.getNativeFrame(null);
            return new BasicCelestialPoint(name, gm, ephemeris, parentFrame, convention);
        }
        // Else: Celestial body
        return buildBSPCelestialBody(name, ephemerisType);
    }

    /** {@inheritDoc} */
    @Override
    public CelestialBody loadCelestialBody(final String name) throws PatriusException {
        // Get ephemeris type
        final EphemerisType ephemerisType = EphemerisType.getEphemerisType(name);

        final boolean isBarycenter = EphemerisType.SOLAR_SYSTEM_BARYCENTER.equals(ephemerisType)
                || EphemerisType.EARTH_MOON.equals(ephemerisType);

        // Build body
        if (isBarycenter || this.celestialPointsNames.contains(name) || ephemerisType == null) {
            // CelestialBody cannot be built
            throw new PatriusException(PatriusMessages.NOT_A_CELESTIAL_BODY, ephemerisType);
        }

        // General case
        return buildBSPCelestialBody(name, ephemerisType);
    }

    /**
     * Build BSP CelestialBody.
     * 
     * @param name
     *        CelestialBody name
     * @param ephemerisType
     *        Ephemeris type
     * @return built BSP CelestialBody
     * @throws PatriusException
     *         if build failed
     */
    private CelestialBody buildBSPCelestialBody(final String name,
                                                final EphemerisType ephemerisType) throws PatriusException {
        final CelestialBodyEphemeris ephemeris = getEphemerisLoader().loadCelestialBodyEphemeris(name);
        final Frame parentFrame = ephemeris.getNativeFrame(null);
        return new BSPCelestialBody(name, ephemeris, (BSPEphemerisLoader) getEphemerisLoader(), parentFrame,
            ephemerisType);
    }

    /** {@inheritDoc} */
    @Override
    public String getName(final String patriusName) {
        return toSpiceName(patriusName);
    }
    
    /**
     * Convert a PATRIUS body name to a SPICE body name.
     * @param patriusName PATRIUS body name
     * @return SPICE body name
     */
    public static String toSpiceName(final String patriusName) {
        if (CelestialBodyFactory.EARTH_MOON.equals(patriusName)) {
            // Specific name for Earth-Moon barycenter
            return EARTH_MOON;
        }
        return patriusName.toUpperCase(Locale.US);
    }

    /**
     * Solar System Barycenter ephemeris specifically defined in its own class since SSB is not in BSP files, hence
     * cannot be handled by {@link BSPEphemerisLoader}.
     * 
     * @since 4.13
     */
    public static class SSBEphemeris implements CelestialBodyEphemeris {
        // SSB case
        // Not defined in BSP ephemeris data

        /** Serializable UID. */
        private static final long serialVersionUID = -7252030617159851153L;

        /** {@inheritDoc} */
        @Override
        public PVCoordinates getPVCoordinates(final AbsoluteDate date,
                                              final Frame frame) throws PatriusException {
            // Specific implementation for SSB
            // The SSB is always exactly at the origin of its own inertial frame
            return FramesFactory.getICRF().getPVCoordinates(date, frame);
        }

        /** {@inheritDoc} */
        @Override
        public Frame getNativeFrame(final AbsoluteDate date) {
            return FramesFactory.getICRF();
        }
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
         * @throws PatriusException
         *         if gravitational coefficient cannot be retrieved
         */
        public BSPCelestialBody(final String name,
                                final CelestialBodyEphemeris ephemeris,
                                final BSPEphemerisLoader ephemerisLoader,
                                final Frame parentFrame,
                                final EphemerisType ephemerisType) throws PatriusException {
            super(name, null, IAUPoleFactory.getIAUPole(ephemerisType), parentFrame, ephemerisLoader.getConvention(),
                    ephemeris);
            if (ephemerisType != null) {
                setShape(buildDefaultBodyShape(name, getRotatingFrame(IAUPoleModelType.TRUE), ephemerisType));
                setGravityModel(new NewtonianGravityModel(getICRF(), getLoadedGravitationalCoefficient(ephemerisType)));
            }
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
