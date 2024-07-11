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

    /** Position-Velocity of celestial body. */
    private final PVCoordinatesProvider pvCoordinateProvider;

    /**
     * Build an instance and the underlying frame.
     * 
     * @param name
     *        name of the body
     * @param aPVCoordinateProvider
     *        Position-Velocity of celestial body
     * @param gm
     *        attraction coefficient (in m<sup>3</sup>/s<sup>2</sup>)
     * @param iauPole
     *        IAU pole implementation
     * @param definingFrameIn
     *        frame in which celestial body coordinates are defined
     * @param shapeIn body shape
     */
    public UserCelestialBody(final String name, final PVCoordinatesProvider aPVCoordinateProvider,
        final double gm, final IAUPole iauPole, final Frame definingFrameIn, final GeometricBodyShape shapeIn) {
        super(name, gm, iauPole, definingFrameIn);
        this.pvCoordinateProvider = aPVCoordinateProvider;
        this.setShape(shapeIn);
    }

    /**
     * Get the PV coordinates provider.
     * 
     * @return the PV coordinates provider
     */
    public PVCoordinatesProvider getPVCoordinatesProvider() {
        return this.pvCoordinateProvider;
    }

    /** {@inheritDoc} */
    @Override
    public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
        return this.pvCoordinateProvider.getPVCoordinates(date, frame);
    }

    /** {@inheritDoc} */
    @Override
    public Frame getNativeFrame(final AbsoluteDate date,
            final Frame frame) throws PatriusException {
        return this.pvCoordinateProvider.getNativeFrame(date, frame);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        final String result = super.toString();
        final StringBuilder builder = new StringBuilder(result);
        builder.append("- Ephemeris origin: " + pvCoordinateProvider.toString() + " ("
                + pvCoordinateProvider.getClass() + ")");
        return builder.toString();
    }
}
