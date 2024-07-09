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
 * @history 23/01/2012
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:85:18/07/2013:Created the WrenchModel interface
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.wrenches;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Interface to represents wrench models.
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: WrenchModel.java 18067 2017-10-02 16:44:20Z bignon $
 * 
 * @since 2.1
 * 
 */
public interface WrenchModel {

    /**
     * Compute the resulting wrench at the mass centre of the spacecraft in the frame of the main part.
     * 
     * @param s
     *        spacecraft state
     * @return resulting wrench
     * @throws PatriusException
     *         if some error occurs
     */
    Wrench computeWrench(SpacecraftState s) throws PatriusException;

    /**
     * Compute the resulting wrench.
     * 
     * @param s
     *        spacecraft state
     * @param origin
     *        point in which to express wrench
     * @param frame
     *        frame in which point is expressed
     * @return resulting wrench
     * @throws PatriusException
     *         if some error occurs
     */
    Wrench computeWrench(SpacecraftState s, Vector3D origin, Frame frame) throws PatriusException;

    /**
     * Compute the resulting torque at the mass centre of the spacecraft in the frame of the main part.
     * 
     * @param s
     *        spacecraft state
     * @return resulting torque at center of spacecraft state frame
     * @throws PatriusException
     *         if some error occurs
     */
    Vector3D computeTorque(SpacecraftState s) throws PatriusException;

    /**
     * Compute the resulting wrench.
     * 
     * @param s
     *        spacecraft state
     * @param origin
     *        point in which to express torque
     * @param frame
     *        frame in which point is expressed
     * @return resulting torque expressed at origin
     * @throws PatriusException
     *         if some error occurs
     */
    Vector3D computeTorque(SpacecraftState s, Vector3D origin, Frame frame) throws PatriusException;

}
