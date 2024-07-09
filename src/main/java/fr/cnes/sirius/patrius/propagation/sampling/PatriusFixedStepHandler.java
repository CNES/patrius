/**
 * Copyright 2011-2017 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
package fr.cnes.sirius.patrius.propagation.sampling;

import java.io.Serializable;

import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * This interface is a space-dynamics aware fixed size step handler.
 * 
 * <p>
 * It mirrors the <code>FixedStepHandler</code> interface from <a
 * href="http://commons.apache.org/math/">commons-math</a> but provides a space-dynamics interface to the methods.
 * </p>
 * 
 * @author Luc Maisonobe
 */
public interface PatriusFixedStepHandler extends Serializable {

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
     */
    void init(SpacecraftState s0, AbsoluteDate t);

    /**
     * Handle the current step.
     * 
     * @param currentState
     *        current state at step time
     * @param isLast
     *        if true, this is the last integration step
     * @exception PropagationException
     *            if step cannot be handled
     */
    void handleStep(final SpacecraftState currentState, final boolean isLast) throws PropagationException;

}
