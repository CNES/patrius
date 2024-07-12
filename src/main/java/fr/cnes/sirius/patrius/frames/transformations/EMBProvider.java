/**
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
 * VERSION:4.11:DM:DM-3300:22/05/2023:[PATRIUS] Nouvelle approche calcul position relative de 2 corps
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3166:10/05/2022:[PATRIUS] Definir l'ICRF comme repere racine 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.transformations;

import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfiguration;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Earth-Moon barycenter frame Reference Frame.
 * <p>
 * This frame is Earth-Moon barycenter-centered pseudo-inertial reference frame.
 * </p>
 * <p>
 * Its parent frame is the Solar System barycenter frame (ICRF).
 * <p>
 * 
 * @serial serializable.
 */
public final class EMBProvider implements TransformProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 1348242067511943173L;

    /**
     * Get the transform from Earth-Moon barycenter frame to Solar System barycenter frame (ICRF) at the specified
     * date.
     * 
     * @param date
     *        new value of the date
     * @param config
     *        frames configuration to use
     * @return transform at the specified date
     *         library cannot be read
     * @throws PatriusException
     *         if the JPL ephemeris data cannot be retrieved
     */
    @Override
    public Transform getTransform(final AbsoluteDate date, final FramesConfiguration config) throws PatriusException {
        return this.getTransform(date, config, false);
    }

    /**
     * Get the transform from Earth-Moon barycenter frame to Solar System barycenter frame (ICRF) at the specified
     * date.
     * 
     * @param date
     *        new value of the date
     * @return transform at the specified date
     * @exception PatriusException
     *         if the JPL ephemeris data cannot be retrieved
     */
    @Override
    public Transform getTransform(final AbsoluteDate date) throws PatriusException {
        return this.getTransform(date, FramesFactory.getConfiguration(), false);
    }

    /**
     * Get the transform from Earth-Moon barycenter frame to Solar System barycenter frame (ICRF) at the specified
     * date.
     * <p>
     * Spin derivative is never computed and is either 0 or null.
     * </p>
     * 
     * @param date
     *        new value of the date
     * @param computeSpinDerivatives true if spin derivatives shall be computed, false otherwise
     * @return transform at the specified date
     * @exception PatriusException
     *            if the JPL ephemeris data cannot be retrieved
     */
    @Override
    public Transform getTransform(final AbsoluteDate date,
                                  final boolean computeSpinDerivatives) throws PatriusException {
        return this.getTransform(date, FramesFactory.getConfiguration(), computeSpinDerivatives);
    }

    /**
     * Get the transform from Earth-Moon barycenter frame to Solar System barycenter frame (ICRF) at the specified
     * date.
     * <p>
     * Spin derivative is never computed and is either 0 or null.
     * </p>
     * 
     * @param date
     *        new value of the date
     * @param config
     *        frames configuration to use
     * @param computeSpinDerivatives true if spin derivatives shall be computed, false otherwise
     * @return transform at the specified date
     * @throws PatriusException
     *         if the JPL ephemeris data cannot be retrieved
     */
    @Override
    public Transform getTransform(final AbsoluteDate date, final FramesConfiguration config,
                                  final boolean computeSpinDerivatives) throws PatriusException {
        // Compulsory use of JPL ephemeris in order to link EMB and its parent frame SSB
        final PVCoordinates pv = CelestialBodyFactory.getSolarSystemBarycenter().getEphemeris()
            .getPVCoordinates(date, CelestialBodyFactory.getEarthMoonBarycenter().getICRF()).negate();
        final AngularCoordinates angular;
        if (computeSpinDerivatives) {
            angular = AngularCoordinates.IDENTITY;
        } else {
            angular = new AngularCoordinates(Rotation.IDENTITY, Vector3D.ZERO, null);
        }
        return new Transform(date, pv, angular);
    }
}
