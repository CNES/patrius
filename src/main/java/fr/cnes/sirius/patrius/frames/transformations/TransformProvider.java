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
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:489:12/01/2016:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.frames.transformations;

import java.io.Serializable;

import fr.cnes.sirius.patrius.frames.configuration.FramesConfiguration;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Interface for Transform providers.
 * <p>
 * The transform provider interface is mainly used to define the transform between a frame and its parent frame.
 * </p>
 * 
 * @author Luc Maisonobe
 */
public interface TransformProvider extends Serializable {

    /**
     * Get the {@link Transform} corresponding to specified date.
     * <p>
     * <b>Warning: </b>spin derivative is not computed.
     * </p>
     * 
     * @param date
     *        current date
     * @return transform at specified date
     * @exception PatriusException
     *            if transform cannot be computed at given date
     */
    Transform getTransform(AbsoluteDate date) throws PatriusException;

    /**
     * Get the {@link Transform} corresponding to specified date.
     * <p>
     * <b>Warning: </b>spin derivative is not computed.
     * </p>
     * 
     * @param date
     *        current date
     * @param config
     *        frames configuration to use
     * @return transform at specified date
     * @exception PatriusException
     *            if transform cannot be computed at given date
     */
    Transform getTransform(AbsoluteDate date, FramesConfiguration config) throws PatriusException;

    /**
     * Get the {@link Transform} corresponding to specified date.
     * 
     * @param date
     *        current date
     * @param computeSpinDerivatives
     *        true if spin derivatives should be computed. If not, spin derivative is set to <i>null</i>
     * @return transform at specified date
     * @exception PatriusException
     *            if transform cannot be computed at given date
     */
    Transform getTransform(AbsoluteDate date, boolean computeSpinDerivatives) throws PatriusException;

    /**
     * Get the {@link Transform} corresponding to specified date.
     * 
     * @param date
     *        current date
     * @param config
     *        frames configuration to use
     * @param computeSpinDerivatives
     *        true if spin derivatives should be computed. If not, spin derivative is set to <i>null</i>
     * @return transform at specified date
     * @exception PatriusException
     *            if transform cannot be computed at given date
     */
    Transform getTransform(AbsoluteDate date, FramesConfiguration config,
                           boolean computeSpinDerivatives) throws PatriusException;
}
