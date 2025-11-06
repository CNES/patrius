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
 * VERSION:4.13:DM:DM-5:08/12/2023:[PATRIUS] Orientation d'un corps celeste sous forme de quaternions
 * VERSION:4.13:DM:DM-3:08/12/2023:[PATRIUS] Distinction entre corps celestes et barycentres
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import fr.cnes.sirius.patrius.forces.gravity.GravityModel;
import fr.cnes.sirius.patrius.frames.CelestialBodyFrame;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Interface for celestial bodies like Sun, Moon or solar system planets.
 * <p>
 * Celestial Barycenters are handled by class {@link BasicCelestialPoint}.
 * </p>
 * 
 * @author Emmanuel Bignon
 *
 * @since 4.13
 */
public interface CelestialBody extends CelestialPoint {

    /**
     * Getter for an inertially oriented, body centered frame.
     *
     * @return an inertially oriented, body centered frame
     * @throws PatriusException
     *         if frame cannot be retrieved
     */
    CelestialBodyFrame getInertialFrame() throws PatriusException;

    /**
     * Getter for a body oriented, body centered frame.
     * 
     * @return a body oriented, body centered frame
     * @throws PatriusException
     *         if frame cannot be retrieved
     */
    CelestialBodyFrame getRotatingFrame() throws PatriusException;

    /**
     * Getter for the geometric shape of the body.
     * 
     * @return geometric shape of the body
     */
    BodyShape getShape();

    /**
     * Setter for a geometric shape to the body.
     * 
     * @param shapeIn
     *        the shape of the body
     */
    void setShape(BodyShape shapeIn);

    /**
     * Getter for the name of the body.
     * 
     * @return name of the body
     */
    @Override
    String getName();

    /**
     * Getter for the celestial body orientation and primer meridians orientation.
     * 
     * @return the celestial body orientation
     */
    CelestialBodyOrientation getOrientation();

    /**
     * Setter for a celestial body orientation to define the body frames.
     * 
     * @param celestialBodyOrientation
     *        the celestial body orientation
     */
    void setOrientation(final CelestialBodyOrientation celestialBodyOrientation);

    /**
     * Getter for the gravitational attraction model of the body.
     * 
     * @return the gravitational attraction model
     */
    GravityModel getGravityModel();

    /**
     * Setter for a gravitational attraction model to the body.
     * 
     * @param gravityModelIn
     *        the gravitational attraction model
     */
    void setGravityModel(final GravityModel gravityModelIn);

    /**
     * Getter for the celestial body nature.
     * 
     * @return the celestial body nature
     */
    @Override
    default BodyNature getBodyNature() {
        return BodyNature.PHYSICAL_BODY;
    }
}
