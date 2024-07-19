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
 * @history created 18/03/2015
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:300:18/03/2015:Creation multi propagator
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.sampling.multi;

import java.io.Serializable;
import java.util.Map;

import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * <p>
 * This interface is copied from {@link fr.cnes.sirius.patrius.propagation.sampling.PatriusStepHandler} and adapted to
 * multi propagation.
 * </p>
 * <p>
 * This interface is a space-dynamics aware step handler for propagation with several states.
 * </p>
 * <p>
 * It mirrors the <code>StepHandler</code> interface from <a href="http://commons.apache.org/math/"> commons-math</a>
 * but provides a space-dynamics interface to the methods.
 * </p>
 * </p>
 * 
 * @author maggioranic
 * 
 * @version $Id: MultiOrekitStepHandler.java 18100 2017-10-03 10:04:21Z bignon $
 * 
 * @since 3.0
 * 
 */
public interface MultiPatriusStepHandler extends Serializable  {

    /**
     * Initialize step handler at the start of a propagation.
     * <p>
     * This method is called once at the start of the propagation. It may be used by the step handler to initialize some
     * internal data if needed.
     * </p>
     * 
     * @param s0
     *        map of initial states
     * @param t
     *        target time for the integration
     */
    void init(Map<String, SpacecraftState> s0, AbsoluteDate t);

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
    void handleStep(MultiPatriusStepInterpolator interpolator, boolean isLast) throws PropagationException;

}
