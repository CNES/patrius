/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */
/*
 *
 * HISTORY
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2445:27/05/2020:optimisation de SolarActivityReader 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:268:30/04/2015:drag and lift implementation
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.forces.atmospheres;

import java.io.Serializable;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Interface for atmospheric models.
 * 
 * @author Luc Maisonobe
 */
public interface Atmosphere extends Serializable {

    /**
     * Get the local density.
     * 
     * @param date current date
     * @param position current position in frame
     * @param frame the frame in which is defined the position
     * @return local density (kg/m<sup>3</sup>)
     * @exception PatriusException if date is out of range of solar activity model or if some frame
     *            conversion cannot be performed
     */
    double getDensity(AbsoluteDate date, Vector3D position, Frame frame) throws PatriusException;

    /**
     * Get the spacecraft velocity relative to the atmosphere.
     * 
     * @param date current date
     * @param position current position in frame
     * @param frame the frame in which is defined the position
     * @return velocity (m/s) (defined in the same frame as the position)
     * @exception PatriusException if some conversion cannot be performed
     */
    Vector3D getVelocity(AbsoluteDate date, Vector3D position, Frame frame) throws PatriusException;

    /**
     * Get the local speed of sound.
     * 
     * @param date current date
     * @param position current position in frame
     * @param frame the frame in which is defined the position
     * @return speed of sound (m/s)
     * @exception PatriusException if some conversion cannot be performed
     */
    double getSpeedOfSound(AbsoluteDate date, Vector3D position, Frame frame) throws PatriusException;

    /**
     * A copy of the atmosphere. By default copy is deep. If not, atmosphere javadoc will specify
     * which attribute is not fully copied. In that case, the attribute reference is passed.
     * 
     * @return a atmosphere of the detector.
     */
    Atmosphere copy();
    
    /**
     * This methods throws an exception if the user did not provide solar activity on the provided interval [start,
     * end].
     * All models should implement their own method since the required data interval depends on the model.
     * @param start range start date
     * @param end range end date
     * @throws PatriusException thrown if some solar activity data is missing
     */
    void checkSolarActivityData(final AbsoluteDate start, final AbsoluteDate end) throws PatriusException;
}
