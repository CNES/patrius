/**
 * 
 * Copyright 2023-2023 CNES
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
 * HISTORY
 * VERSION:4.13:DM:DM-3:08/12/2023:[PATRIUS] Distinction entre corps celestes et barycentres
 * VERSION:4.13:FA:FA-111:08/12/2023:[PATRIUS] Problemes lies à  l'utilisation des bsp
 * VERSION:4.13:DM:DM-132:08/12/2023:[PATRIUS] Suppression de la possibilite
 * de convertir les sorties de VacuumSignalPropagation
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import fr.cnes.sirius.patrius.bodies.bsp.BSPEphemerisLoader.SpiceJ2000ConventionEnum;
import fr.cnes.sirius.patrius.frames.CelestialBodyFrame;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Basic celestial point.
 * It can be used to define any celestial point with:
 * <ul>
 * <li>Its name</li>
 * <li>A {@link PVCoordinatesProvider} or a {@link CelestialBodyEphemeris} providing body position-velocity through time
 * </li>
 * </ul>
 * <p>
 * For user-defined Celestial bodies, use {@link UserCelestialBody}
 * </p>
 * 
 * @author Emmanuel Bignon
 *
 * @since 4.13
 */
public class BasicCelestialPoint extends AbstractCelestialPoint {

    /** Serializable UID. */
    private static final long serialVersionUID = 6246154413369382222L;

    /** Basic celestial point string. */
    private final String bodyString;

    /**
     * Constructor.
     * <p>
     * SpiceJ2000ConventionEnum is set to ICRF.
     * </p>
     * 
     * @param name
     *        name of the body
     * @param aPVCoordinateProvider
     *        Position-Velocity of celestial body. It is recommended that the native frame of aPVCoordinateProvider
     *        should be identical (or near) to the given parentFrame, in order to minimize the frames transformations.
     * @param gm
     *        gravitational attraction coefficient (in m<sup>3</sup>/s<sup>2</sup>)
     * @param parentFrame
     *        parent frame (usually it should be the ICRF centered on the parent body)
     */
    public BasicCelestialPoint(final String name,
                               final PVCoordinatesProvider aPVCoordinateProvider,
                               final double gm,
                               final Frame parentFrame) {
        this(name, aPVCoordinateProvider, gm, parentFrame, SpiceJ2000ConventionEnum.ICRF);
    }

    /**
     * Constructor.
     * 
     * @param name
     *        name of the body
     * @param aPVCoordinateProvider
     *        Position-Velocity of celestial body. It is recommended that the native frame of aPVCoordinateProvider
     *        should be identical (or near) to the given parentFrame, in order to minimize the frames transformations.
     * @param gm
     *        gravitational attraction coefficient (in m<sup>3</sup>/s<sup>2</sup>)
     * @param parentFrame
     *        parent frame (usually it should be the ICRF centered on the parent body)
     * @param spiceJ2000Convention
     *        Spice convention
     */
    @SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
    // Reason: super attributes have been built at this point
    public BasicCelestialPoint(final String name,
                               final PVCoordinatesProvider aPVCoordinateProvider,
                               final double gm,
                               final Frame parentFrame,
                               final SpiceJ2000ConventionEnum spiceJ2000Convention) {
        super(name, gm, new CelestialBodyEphemeris(){

            /** Serial UID. */
            private static final long serialVersionUID = -6984943550925347950L;

            /** {@inheritDoc} */
            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date,
                                                  final Frame frame) throws PatriusException {
                return aPVCoordinateProvider.getPVCoordinates(date, frame);
            }

            /** {@inheritDoc} */
            @Override
            public Frame getNativeFrame(final AbsoluteDate date) throws PatriusException {
                return aPVCoordinateProvider.getNativeFrame(date);
            }
        }, parentFrame, spiceJ2000Convention);

        // Create basic celestial point string
        final String abstractBodyString = super.toString();
        final StringBuilder builder = new StringBuilder(abstractBodyString);
        builder.append("- Ephemeris origin: " + aPVCoordinateProvider.toString() + " ("
                + aPVCoordinateProvider.getClass() + ")");
        this.bodyString = builder.toString();
    }

    /**
     * Constructor.
     * 
     * @param name
     *        name
     * @param gm
     *        gravitational parameter
     * @param ephemeris
     *        ephemeris
     * @param parentFrame
     *        parent frame (usually it should be the ICRF centered on the parent body)
     * @param convention
     *        spice convention for BSP frames
     */
    public BasicCelestialPoint(final String name,
                               final double gm,
                               final CelestialBodyEphemeris ephemeris,
                               final Frame parentFrame,
                               final SpiceJ2000ConventionEnum convention) {
        super(name, gm, ephemeris, parentFrame, convention);
        this.bodyString = name;
    }

    /**
     * Constructor.
     * 
     * @param name
     *        name
     * @param gm
     *        gravitational parameter
     * @param ephemeris
     *        ephemeris
     * @param icrf
     *        icrf frame centered on this celestial point
     */
    public BasicCelestialPoint(final String name,
                               final double gm,
                               final CelestialBodyEphemeris ephemeris,
                               final CelestialBodyFrame icrf) {
        super(name, gm, ephemeris, icrf);
        this.bodyString = name;
    }

    /**
     * Constructor.
     * 
     * @param icrf
     *        ICRF frame
     * @param name
     *        name
     * @param gm
     *        gravitational parameter
     * @param ephemeris
     *        ephemeris
     */
    public BasicCelestialPoint(final CelestialBodyFrame icrf,
                               final String name,
                               final double gm,
                               final CelestialBodyEphemeris ephemeris) {
        super(name, gm, ephemeris, icrf);
        this.bodyString = name;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return this.bodyString;
    }
}
