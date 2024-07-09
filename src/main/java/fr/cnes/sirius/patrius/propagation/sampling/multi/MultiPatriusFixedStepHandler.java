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
 * @history created 18/03/2015
 * 
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:300:18/03/2015:Creation multi propagator
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.sampling.multi;

import java.io.Serializable;
import java.util.Map;

import fr.cnes.sirius.patrius.math.ode.sampling.StepHandler;
import fr.cnes.sirius.patrius.math.ode.sampling.StepNormalizer;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * <p>
 * This interface is copied from {@link fr.cnes.sirius.patrius.propagation.sampling.PatriusFixedStepHandler} and adapted
 * to multi propagation.
 * </p>
 * <p>
 * This interface represents a handler that should be called after each successful fixed step.
 * </p>
 * <p>
 * This interface should be implemented by anyone who is interested in getting the solution of an ordinary differential
 * equation at fixed time steps. Objects implementing this interface should be wrapped within an instance of
 * {@link StepNormalizer} that itself is used as the general {@link StepHandler} by the integrator. The
 * {@link StepNormalizer} object is called according to the integrator internal algorithms and it calls objects
 * implementing this interface as necessary at fixed time steps.
 * </p>
 * 
 * @see StepHandler
 * @see StepNormalizer
 * 
 * @author maggioranic
 * 
 * @version $Id: MultiOrekitFixedStepHandler.java 18109 2017-10-04 06:48:22Z bignon $
 * 
 * @since 3.0
 * 
 */
public interface MultiPatriusFixedStepHandler extends Serializable {

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
     * @param currentStates
     *        map of current states at step time
     * @param isLast
     *        if true, this is the last integration step
     * @exception PropagationException
     *            if step cannot be handled
     */
    void handleStep(final Map<String, SpacecraftState> currentStates, final boolean isLast) throws PropagationException;
}
