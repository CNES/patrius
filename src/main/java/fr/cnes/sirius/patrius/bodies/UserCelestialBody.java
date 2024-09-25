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
 * @history created 17/02/17
 *
 * HISTORY
 * VERSION:4.13:DM:DM-3:08/12/2023:[PATRIUS] Distinction entre corps celestes et barycentres
 * VERSION:4.13:DM:DM-5:08/12/2023:[PATRIUS] Orientation d'un corps celeste sous forme de quaternions
 * VERSION:4.13:FA:FA-111:08/12/2023:[PATRIUS] Problemes lies à  l'utilisation des bsp
 * VERSION:4.13:DM:DM-132:08/12/2023:[PATRIUS] Suppression de la possibilite
 * de convertir les sorties de VacuumSignalPropagation
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11:DM:DM-3303:22/05/2023:[PATRIUS] Modifications mineures dans UserCelestialBody 
 * VERSION:4.11:FA:FA-3314:22/05/2023:[PATRIUS] Anomalie evaluation ForceModel SpacecraftState en ITRF
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration gestion attractions gravitationnelles
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3149:10/05/2022:[PATRIUS] Optimisation des reperes interplanetaires 
 * VERSION:4.9:DM:DM-3127:10/05/2022:[PATRIUS] Ajout de deux methodes resize a l'interface GeometricBodyShape ...
 * VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3165:10/05/2022:[PATRIUS] Amelioration de la propagation du signal 
 * VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:FA:FA-2948:15/11/2021:[PATRIUS] Harmonisation de l'affichage des informations sur les corps celestes 
 * VERSION:4.7:DM:DM-2703:18/05/2021: Acces facilite aux donnees d'environnement (application interplanetaire) 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:769:17/02/2017:add UserCelestialBody
 * VERSION::FA:1777:04/10/2018:correct ICRF parent frame
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
 * User-defined celestial body.
 * It can be used to define any celestial body with:
 * <ul>
 * <li>Its name</li>
 * <li>A {@link PVCoordinatesProvider} providing body position-velocity through time</li>
 * <li>Its gravitational constant</li>
 * <li>Its pole motion (reference data are provided by IAU)</li>
 * </ul>
 * 
 * @concurrency immutable
 * @author Emmanuel Bignon
 * @version $Id: UserCelestialBody.java 17582 2017-05-10 12:58:16Z bignon $
 * @since 3.4
 */
public class UserCelestialBody extends AbstractCelestialBody {

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
     * @param celestialBodyOrientation
     *        celestial body orientation
     * @param parentFrame
     *        parent frame (usually it should be the ICRF centered on the parent body)
     * @param shape body shape
     * @param spiceJ2000Convention Spice convention
     */
    @SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
    // Reason: super attributes have been built at this point
    public UserCelestialBody(final String name,
            final PVCoordinatesProvider aPVCoordinateProvider,
            final GravityModel gravityModel,
            final CelestialBodyOrientation celestialBodyOrientation,
            final Frame parentFrame,
            final BodyShape shape,
            final SpiceJ2000ConventionEnum spiceJ2000Convention) {
        super(name, gravityModel, celestialBodyOrientation, parentFrame, spiceJ2000Convention,
                new CelestialBodyEphemeris() {

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
        });

        // Create user celestial body string
        final String abstractBodyString = super.toString();
        final StringBuilder builder = new StringBuilder(abstractBodyString);
        builder.append("- Ephemeris origin: " + aPVCoordinateProvider.toString() + " ("
                + aPVCoordinateProvider.getClass() + ")");
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
     * @param celestialBodyOrientation
     *        celestial body orientation
     * @param parentFrame
     *        parent frame (usually it should be the ICRF centered on the parent body)
     * @param shape body shape
     */
    public UserCelestialBody(final String name,
            final PVCoordinatesProvider aPVCoordinateProvider,
            final GravityModel gravityModel,
            final CelestialBodyOrientation celestialBodyOrientation,
            final Frame parentFrame,
            final BodyShape shape) {
        // Initial gravity model is required because of gm store for toString() method
        this(name, aPVCoordinateProvider, gravityModel, celestialBodyOrientation, parentFrame, shape,
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
     * @param celestialBodyOrientation
     *        celestial body orientation
     * @param parentFrame
     *        parent frame (usually it should be the ICRF centered on the parent body)
     * @param shape body shape
     * @param spiceJ2000Convention Spice convention
     */
    public UserCelestialBody(final String name,
            final PVCoordinatesProvider aPVCoordinateProvider,
            final double gm,
            final CelestialBodyOrientation celestialBodyOrientation,
            final Frame parentFrame,
            final BodyShape shape,
            final SpiceJ2000ConventionEnum spiceJ2000Convention) {
        // Initial gravity model is required because of gm store for toString() method
        this(name, aPVCoordinateProvider, new NewtonianGravityModel(parentFrame, gm), celestialBodyOrientation,
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
     * @param celestialBodyOrientation
     *        celestial body orientation
     * @param parentFrame
     *        parent frame (usually it should be the ICRF centered on the parent body)
     * @param shape body shape
     */
    public UserCelestialBody(final String name,
            final PVCoordinatesProvider aPVCoordinateProvider,
            final double gm,
            final CelestialBodyOrientation celestialBodyOrientation,
            final Frame parentFrame,
            final BodyShape shape) {
        this(name, aPVCoordinateProvider, gm, celestialBodyOrientation, parentFrame, shape,
                SpiceJ2000ConventionEnum.ICRF);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return this.bodyString;
    }
}
