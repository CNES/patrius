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
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11.1:FA:FA-69:30/06/2023:[PATRIUS] Amélioration de la gestion des attractions gravitationnelles dans le
 * propagateur
 * VERSION:4.11.1:DM:DM-95:30/06/2023:[PATRIUS] Utilisation de types gen. dans les classes internes d'interp. de
 * AbstractEOPHistory
 * VERSION:4.11:DM:DM-40:22/05/2023:[PATRIUS] Gestion derivees coefficient k dans GravityModel
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration gestion attractions gravitationnelles
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity;

import java.io.Serializable;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This interface represents a gravitational attraction model.
 * 
 * @since 4.11
 * 
 * @author Alex Nardi
 */
public interface GravityModel extends Serializable {

    /**
     * Get the central attraction coefficient.
     * 
     * @return central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     */
    public abstract double getMu();

    /**
     * Set the central attraction coefficient.
     * 
     * @param muIn the central attraction coefficient.
     */
    public abstract void setMu(final double muIn);

    /**
     * Get the central body frame.
     * 
     * @return the bodyFrame
     */
    public Frame getBodyFrame();

    /**
     * Compute the acceleration due to the gravitational attraction.
     *
     * @param positionInBodyFrame
     *        the position expressed in the {@link #getBodyFrame() body frame}
     * @param date
     *        The date for which the computation needs to be performed
     * @return acceleration in the body frame
     * @exception PatriusException
     *            if some specific error occurs
     */
    Vector3D computeAcceleration(final Vector3D positionInBodyFrame, final AbsoluteDate date) throws PatriusException;
    
    /**
     * Compute acceleration derivatives with respect to the position of the spacecraft.
     * 
     * @param positionInBodyFrame
     *        the position expressed in the {@link #getBodyFrame() body frame}
     * @param date
     *        The date for which the computation needs to be performed
     * @return acceleration derivatives with respect to the position of the spacecraft in the body frame
     * @exception PatriusException if derivatives cannot be computed
     */
    double[][] computeDAccDPos(final Vector3D positionInBodyFrame, final AbsoluteDate date)
        throws PatriusException;
}
