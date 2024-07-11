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
 * VERSION:4.9:DM:DM-3172:10/05/2022:[PATRIUS] Ajout d'un throws PatriusException a la methode init de l'interface EventDetector
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.sampling;

import java.io.Serializable;

import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * This interface is a space-dynamics aware step handler.
 * 
 * <p>
 * It mirrors the <code>StepHandler</code> interface from <a href="http://commons.apache.org/math/"> commons-math</a>
 * but provides a space-dynamics interface to the methods.
 * </p>
 * 
 * @author Luc Maisonobe
 */
public interface PatriusStepHandler extends Serializable {

    /**
     * Initialize step handler at the start of a propagation.
     * <p>
     * This method is called once at the start of the propagation. It may be used by the step handler to initialize some
     * internal data if needed.
     * </p>
     * 
     * @param s0
     *        initial state
     * @param t
     *        target time for the integration
     * @throws PatriusException thrown if initialization failed
     */
    void init(SpacecraftState s0, AbsoluteDate t) throws PatriusException;

    /**
     * Handle the current step.
     * 
     * @param interpolator
     *        interpolator set up for the current step
     * @param isLast
     *        if true, this is the last integration step
     * @exception PropagationException
     *            if step cannot be handled
     */
    void handleStep(PatriusStepInterpolator interpolator, boolean isLast) throws PropagationException;

}
