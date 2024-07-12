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
 * @history creation 8/02/2012
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.4:DM:DM-2148:04/10/2019:[PATRIUS] Creations de parties mobiles dans un Assembly
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:231:03/04/2014:bad updating of the assembly's tree of frames
 * VERSION::FA:1192:30/08/2017:update parts frame
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly;

import java.io.Serializable;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * -Interface for the assembly's parts.
 * 
 * @see Part
 * @see MainPart
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.1
 * 
 */
public interface IPart extends Serializable {

    /**
     * @return the name of the part
     */
    String getName();

    /**
     * @return the parent part
     */
    IPart getParent();

    /**
     * @return the associated frame
     */
    Frame getFrame();

    /**
     * Returns a property of the part : if in this part, one exists of the given type.
     * 
     * @param propertyType
     *        the type of the wanted property
     * @return the property
     */
    IPartProperty getProperty(PropertyType propertyType);

    /**
     * Adds a property to the part.
     * 
     * @param property
     *        the property
     */
    void addProperty(IPartProperty property);

    /**
     * Checks if a property of the given type exists in this part.
     * 
     * @param propertyType
     *        the type
     * @return true if the property exists
     */
    boolean hasProperty(PropertyType propertyType);

    /**
     * @return the level of the part in the tree
     */
    int getPartLevel();

    /**
     * @param t
     *        new transformation to be applied
     *        Updates the part's frame with a new definition of its Transform.
     * @throws PatriusException
     *         if update fails
     */
    void updateFrame(final Transform t) throws PatriusException;

    /**
     * Update frame at provided date.
     * 
     * @param date
     *        date
     */
    void updateFrame(final AbsoluteDate date);

    /**
     * Update frame with provided spacecraft state.
     * @param s spacecraft state
     * @throws PatriusException thrown if update fails
     */
    void updateFrame(final SpacecraftState s) throws PatriusException;
}
