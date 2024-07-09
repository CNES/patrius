/**
 *
 * Copyright 2011-2017 CNES
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
 * @history creation 26/04/2012
 *
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:86:22/10/2013:New mass management system
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.models;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Matrix3D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @description
 *              <p>
 *              This is the interface for inertia models that can provide the mass, mass center and inertia matrix of
 *              the whole satellite.
 *              </p>
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public interface IInertiaModel extends MassProvider {

    /**
     * Getter for the mass center.
     * 
     * @param frame
     *        the expression frame of the mass center position
     * @param date
     *        the current date (unused for simple the model)
     * @return the mass center position vector
     * @throws PatriusException
     *         if a problem occurs during frames transformations
     */
    Vector3D getMassCenter(Frame frame, AbsoluteDate date) throws PatriusException;

    /**
     * Getter for the inertia matrix of the spacecraft,
     * expressed with respect to the MASS CENTER in a given frame.
     * 
     * @param frame
     *        the expression frame of the inertia matrix
     * @param date
     *        the current date (unused for simple the model)
     * @return the inertia matrix
     * @throws PatriusException
     *         if a problem occurs during frames transformations
     */
    Matrix3D getInertiaMatrix(Frame frame, AbsoluteDate date) throws PatriusException;

    /**
     * Getter for the inertia matrix of the spacecraft,
     * once expressed with respect to a point
     * that can be different from the mass center. This point must be
     * defined in the reference frame of expression of the matrix.
     * 
     * @param frame
     *        the expression frame of the inertia matrix
     * @param date
     *        the current date (unused for simple the model)
     * @param inertiaReferencePoint
     *        the point with respect to the inertia matrix is expressed (in the reference frame)
     * @return the inertia matrix
     * @throws PatriusException
     *         if a problem occurs during frames transformations
     */
    Matrix3D getInertiaMatrix(Frame frame, AbsoluteDate date, Vector3D inertiaReferencePoint) throws PatriusException;

}
