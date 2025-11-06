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
 * HISTORY
 * VERSION:4.14:OPENFD-161:22/08/2024:[PATRIUS] Adaptation de l'interface CelestialBody
 * car l'orientation n'est pas forcement IAU
 * VERSION:4.14:OPENFD-179:22/08/2024: [PATRIUS] Gestion emetteur/recepteur dans les detecteurs d'evenements
 * VERSION:4.14:OPENFD-343:22/08/2024: Ajout de regles de codage dans le standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import fr.cnes.sirius.patrius.bodies.bsp.BSPEphemerisLoader.SpiceJ2000ConventionEnum;
import fr.cnes.sirius.patrius.forces.gravity.GravityModel;
import fr.cnes.sirius.patrius.forces.gravity.NewtonianGravityModel;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * User-defined IAU celestial body.
 * It can be used to define any celestial body with:
 * <ul>
 * <li>Its name</li>
 * <li>A {@link PVCoordinatesProvider} providing body position-velocity through time</li>
 * <li>Its gravitational constant</li>
 * <li>Its pole motion (reference data are provided by IAU)</li>
 * </ul>
 * 
 * @concurrency immutable
 * @author Thibaut BONIT
 * @since 4.14
 */
public class UserIAUCelestialBody extends AbstractIAUCelestialBody {

    /** Serializable UID. */
    private static final long serialVersionUID = -749020299406400630L;

    /** User celestial body string. */
    private final String bodyString;

    /**
     * Constructor.
     * 
     * @param name
     *        name of the body
     * @param aPVCoordinateProvider
     *        Position-Velocity of celestial body. It is recommended that the native frame of aPVCoordinateProvider
     *        should be identical (or near) to the given parentFrame, in order to minimize the frames transformations.
     * @param gravityModel
     *        gravitational attraction model
     * @param iauOrientation
     *        celestial body IAU orientation
     * @param parentFrame
     *        parent frame (usually it should be the ICRF centered on the parent body)
     * @param shape body shape
     * @param spiceJ2000Convention Spice convention
     */
    @SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
    // Reason: super attributes have been built at this point
    public UserIAUCelestialBody(final String name,
                                final PVCoordinatesProvider aPVCoordinateProvider,
                                final GravityModel gravityModel,
                                final CelestialBodyIAUOrientation iauOrientation,
                                final Frame parentFrame,
                                final BodyShape shape,
                                final SpiceJ2000ConventionEnum spiceJ2000Convention) {
        super(name, gravityModel, iauOrientation, parentFrame, spiceJ2000Convention, new CelestialBodyEphemeris(){

            /** Serializable UID. */
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
        });

        // Create user celestial body string
        final String abstractBodyString = super.toString();
        final StringBuilder builder = new StringBuilder(abstractBodyString);
        builder.append("- Ephemeris origin: ").append(aPVCoordinateProvider).append(" (")
            .append(aPVCoordinateProvider.getClass()).append(')');
        this.bodyString = builder.toString();

        this.setShape(shape);
    }

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
     * @param gravityModel
     *        gravitational attraction model
     * @param celestialBodyIAUOrientation
     *        celestial body IAU orientation
     * @param parentFrame
     *        parent frame (usually it should be the ICRF centered on the parent body)
     * @param shape body shape
     */
    public UserIAUCelestialBody(final String name,
                                final PVCoordinatesProvider aPVCoordinateProvider,
                                final GravityModel gravityModel,
                                final CelestialBodyIAUOrientation celestialBodyIAUOrientation,
                                final Frame parentFrame,
                                final BodyShape shape) {
        // Initial gravity model is required because of gm store for toString() method
        this(name, aPVCoordinateProvider, gravityModel, celestialBodyIAUOrientation, parentFrame, shape,
                SpiceJ2000ConventionEnum.ICRF);
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
     * @param celestialBodyIAUOrientation
     *        celestial body IAU orientation
     * @param parentFrame
     *        parent frame (usually it should be the ICRF centered on the parent body)
     * @param shape body shape
     * @param spiceJ2000Convention Spice convention
     */
    public UserIAUCelestialBody(final String name,
                                final PVCoordinatesProvider aPVCoordinateProvider,
                                final double gm,
                                final CelestialBodyIAUOrientation celestialBodyIAUOrientation,
                                final Frame parentFrame,
                                final BodyShape shape,
                                final SpiceJ2000ConventionEnum spiceJ2000Convention) {
        // Initial gravity model is required because of gm store for toString() method
        this(name, aPVCoordinateProvider, new NewtonianGravityModel(parentFrame, gm), celestialBodyIAUOrientation,
                parentFrame, shape, spiceJ2000Convention);
        // Workaround: gravity model in this case is centered on ICRF
        setGravityModel(new NewtonianGravityModel(getICRF(), gm));
    }

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
     * @param celestialBodyIAUOrientation
     *        celestial body IAU orientation
     * @param parentFrame
     *        parent frame (usually it should be the ICRF centered on the parent body)
     * @param shape body shape
     */
    public UserIAUCelestialBody(final String name,
                                final PVCoordinatesProvider aPVCoordinateProvider,
                                final double gm,
                                final CelestialBodyIAUOrientation celestialBodyIAUOrientation,
                                final Frame parentFrame,
                                final BodyShape shape) {
        this(name, aPVCoordinateProvider, gm, celestialBodyIAUOrientation, parentFrame, shape,
                SpiceJ2000ConventionEnum.ICRF);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return this.bodyString;
    }
}
