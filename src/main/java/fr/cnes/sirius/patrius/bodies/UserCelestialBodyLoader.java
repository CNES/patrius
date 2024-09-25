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
 * VERSION:4.13:FA:FA-118:08/12/2023:[PATRIUS] Calcul d'union de PyramidalField invalide
 * VERSION:4.13:DM:DM-3:08/12/2023:[PATRIUS] Distinction entre corps celestes et barycentres
 * VERSION:4.13:DM:DM-5:08/12/2023:[PATRIUS] Orientation d'un corps celeste sous forme de quaternions
 * VERSION:4.13:FA:FA-111:08/12/2023:[PATRIUS] Problemes lies à  l'utilisation des bsp
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import fr.cnes.sirius.patrius.bodies.bsp.BSPEphemerisLoader.SpiceJ2000ConventionEnum;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Loader for {@link UserCelestialBody} or {@link BasicCelestialPoint}.
 * <p>
 * This loader is to be used in conjunction with {@link CelestialBodyFactory}.
 * </p>
 *
 * @author Emmanuel Bignon
 * 
 * @since 4.13
 */
public class UserCelestialBodyLoader implements CelestialBodyLoader {

    /** Serial UID. */
    private static final long serialVersionUID = -6500789799012303171L;

    /** PV coordinates provider giving position-velocity of the Body. */
    private final PVCoordinatesProvider pvCoordinatesProvider;

    /** Gravitational constant of the body. */
    private final double gm;

    /** Celestial body orientation. */
    private final CelestialBodyOrientation celestialBodyOrientation;

    /** Parent ICRF. */
    private final Frame parentFrame;

    /** Shape. */
    private final BodyShape shape;

    /** Spice J2000 convention. */
    private final SpiceJ2000ConventionEnum spiceJ2000Convention;

    /**
     * Constructor.
     * <p>
     * SpiceJ2000ConventionEnum is set to ICRF.
     * </p>
     * 
     * @param pvCoordinatesProvider
     *        position-velocity of the body
     * @param gm
     *        the value of the body's gravitational constant
     * @param celestialBodyOrientation
     *        celestial body orientation (may be null)
     * @param parentFrame
     *        parent frame (usually it should be the ICRF centered on the parent body)
     * @param shape
     *        body shape (may be null)
     */
    public UserCelestialBodyLoader(final PVCoordinatesProvider pvCoordinatesProvider, final double gm,
                                   final CelestialBodyOrientation celestialBodyOrientation, final Frame parentFrame,
                                   final BodyShape shape) {
        this(pvCoordinatesProvider, gm, celestialBodyOrientation, parentFrame, shape, SpiceJ2000ConventionEnum.ICRF);
    }

    /**
     * Complete constructor.
     * 
     * @param pvCoordinatesProvider
     *        position-velocity of the body
     * @param gm
     *        the value of the body's gravitational constant
     * @param celestialBodyOrientation
     *        celestial body orientation (may be null)
     * @param parentFrame
     *        parent frame (usually it should be the ICRF centered on the parent body)
     * @param shape
     *        body shape (may be null)
     * @param spiceJ2000Convention
     *        Spice convention
     */
    public UserCelestialBodyLoader(final PVCoordinatesProvider pvCoordinatesProvider, final double gm,
                                   final CelestialBodyOrientation celestialBodyOrientation, final Frame parentFrame,
                                   final BodyShape shape, final SpiceJ2000ConventionEnum spiceJ2000Convention) {
        this.pvCoordinatesProvider = pvCoordinatesProvider;
        this.gm = gm;
        this.celestialBodyOrientation = celestialBodyOrientation;
        this.parentFrame = parentFrame;
        this.shape = shape;
        this.spiceJ2000Convention = spiceJ2000Convention;
    }

    /** {@inheritDoc} */
    @Override
    public CelestialBody loadCelestialBody(final String name) {
        return new UserCelestialBody(name, this.pvCoordinatesProvider, this.gm, this.celestialBodyOrientation,
                this.parentFrame, this.shape, this.spiceJ2000Convention);
    }

    /** {@inheritDoc} */
    @Override
    public CelestialPoint loadCelestialPoint(final String name) throws PatriusException {
        return new BasicCelestialPoint(name, this.pvCoordinatesProvider, this.gm, this.parentFrame,
                this.spiceJ2000Convention);
    }

    /** {@inheritDoc} */
    @Override
    public String getName(final String patriusName) {
        // This loader doesn't override name
        return patriusName;
    }
}