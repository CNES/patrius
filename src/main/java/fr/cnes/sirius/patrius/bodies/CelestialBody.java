/**
 * Copyright 2002-2012 CS Systèmes d'Information
 * Copyright 2011-2017 CNES
 *
 * HISTORY
 * VERSION:4.7:DM:DM-2703:18/05/2021: Acces facilite aux donnees d'environnement (application interplanetaire) 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 */
/*
 * 
 */
package fr.cnes.sirius.patrius.bodies;

import java.io.Serializable;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Interface for celestial bodies like Sun, Moon or solar system planets.
 * 
 * @author Luc Maisonobe
 * @see CelestialBodyFactory
 * @see IAUPole
 */
public interface CelestialBody extends Serializable, PVCoordinatesProvider {

    /**
     * Get an inertially oriented, body centered frame.
     * <p>
     * The frame is always bound to the body center, and its axes have a fixed orientation with respect to other
     * inertial frames.
     * </p>
     * 
     * @return an inertially oriented, body centered frame
     * @exception PatriusException
     *            if frame cannot be retrieved
     * @see #getBodyOrientedFrame()
     */
    Frame getInertiallyOrientedFrame() throws PatriusException;

    /**
     * Get a body oriented, body centered frame.
     * <p>
     * The frame is always bound to the body center, and its axes have a fixed orientation with respect to the celestial
     * body.
     * </p>
     * 
     * @return a body oriented, body centered frame
     * @exception PatriusException
     *            if frame cannot be retrieved
     * @see #getInertiallyOrientedFrame()
     */
    Frame getBodyOrientedFrame() throws PatriusException;

    /**
     * Get the name of the body.
     * 
     * @return name of the body
     */
    String getName();

    /**
     * Get the attraction coefficient of the body.
     * 
     * @return attraction coefficient of the body (m<sup>3</sup>/s<sup>2</sup>)
     */
    double getGM();

    /**
     * Returns a string representation of the body and its attributes.
     * @return a string representation of the body and its attributes
     */
    String toString();

}
