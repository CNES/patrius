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
 * @history creation 21/12/2011
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:133:02/10/2013:Javadoc completed
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes;

import java.io.Serializable;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This interface represents an orientation law, i.e. a law providing an orientation at a given date with respect to a
 * given frame.
 * 
 * @author Julie Anton
 * 
 * @version $Id: IOrientationLaw.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.1
 * 
 */
public interface IOrientationLaw extends Serializable {

    /**
     * Gets the rotation defining the orientation with respect to a given frame at a given date.
     * 
     * @param date
     *        date
     * @param frame
     *        frame
     * @return the orientation
     * @throws PatriusException
     *         if a problem occurs during frames transformations
     */
    Rotation getOrientation(AbsoluteDate date, Frame frame) throws PatriusException;
}
