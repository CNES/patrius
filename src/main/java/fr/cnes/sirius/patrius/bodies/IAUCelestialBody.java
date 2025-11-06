/**
 * Copyright 2023-2023 CNES
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
 * VERSION:4.14:OPENFD-161:22/08/2024:[PATRIUS] Adaptation de l'interface CelestialBody
 * car l'orientation n'est pas forcement IAU
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import fr.cnes.sirius.patrius.bodies.IAUPoleFunction.IAUTimeDependency;
import fr.cnes.sirius.patrius.frames.CelestialBodyFrame;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Interface for IAU celestial bodies like Sun, Moon or solar system planets.
 * <p>
 * When body orientation is defined by an IAU model, you can choose inertial and rotating
 * frame model precision through {@link IAUCelestialBody#getInertialFrame(IAUPoleModelType)} 
 * and {@link IAUCelestialBody#getRotatingFrame(IAUPoleModelType)} methods.
 * </p>
 * <p>
 * By default in {@link IAUCelestialBody#getInertialFrame()} and {@link IAUCelestialBody#getRotatingFrame()} 
 * the complete model {@link IAUPoleModelType#TRUE} is used.
 * </p>
 * <p>
 * Celestial Barycenters are handled by class {@link BasicCelestialPoint}.
 * </p>
 * 
 * @since 4.14
 */
public interface IAUCelestialBody extends CelestialBody {

    /**
     * Getter for an inertially oriented, body centered frame.
     * <p>
     * <b>Warning: </b>Inertiality of such frame depends on its definition and on its use: if related
     * {@link CelestialBodyOrientation} includes strong precession/nutation effect and temporal horizon of use is long,
     * frame may not be considered inertial. As a rule of thumb, precession/nutation effects of Earth frames such as
     * CIRF/MOD are considered small enough on a horizon of a day to consider them pseudo-inertial. Also frames based on
     * {@link IAUPoleModelType#CONSTANT} will be more inertial than {@link IAUPoleModelType#MEAN} and than
     * {@link IAUPoleModelType#TRUE}. Similarly, models including {@link IAUTimeDependency#DAYS} with high values will
     * tend to be less inertial than models including {@link IAUTimeDependency#CENTURIES} with high values. Definition
     * of sufficiently inertial precession/nutation effects remains on the user responsibility depending on the frame
     * usage.
     * </p>
     *
     * @param iauPole
     *        the type of IAUPole used for the frame
     * 
     * @return an inertially oriented, body centered frame
     * @exception PatriusException
     *            if frame cannot be retrieved
     */
    CelestialBodyFrame getInertialFrame(final IAUPoleModelType iauPole) throws PatriusException;

    /** {@inheritDoc} */
    @Override
    default CelestialBodyFrame getInertialFrame() throws PatriusException {
        // Return the true inertial frame, as the "true" is considered generic,
        // whereas the "mean" and "constant" are inherent to the IAU model
        return this.getInertialFrame(IAUPoleModelType.TRUE);
    }

    /**
     * Getter for a body oriented, body centered frame.
     * 
     * @param iauPole
     *        the type of IAUPole used for the frame
     * @return a body oriented, body centered frame
     * @throws PatriusException
     *         if frame cannot be retrieved
     */
    CelestialBodyFrame getRotatingFrame(final IAUPoleModelType iauPole) throws PatriusException;

    /** {@inheritDoc} */
    @Override
    default CelestialBodyFrame getRotatingFrame() throws PatriusException {
        // Return the true rotating frame, as the "true" is considered generic,
        // whereas the "mean" and "constant" are inherent to the IAU model
        return this.getRotatingFrame(IAUPoleModelType.TRUE);
    }

    /**
     * Getter for the celestial body IAU orientation and primer meridians orientation.
     * 
     * @return the celestial body IAU orientation
     */
    @Override
    CelestialBodyIAUOrientation getOrientation();
}
