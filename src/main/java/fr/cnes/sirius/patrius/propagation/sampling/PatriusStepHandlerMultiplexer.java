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

import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * This class gathers several {@link PatriusStepHandler} instances into one.
 * 
 * @author Luc Maisonobe
 */
public class PatriusStepHandlerMultiplexer implements PatriusStepHandler {

    /** Serializable UID. */
    private static final long serialVersionUID = -5957903354538173269L;

    /** Underlying step handlers. */
    private final List<PatriusStepHandler> handlers;

    /**
     * Simple constructor.
     */
    public PatriusStepHandlerMultiplexer() {
        this.handlers = new ArrayList<PatriusStepHandler>();
    }

    /**
     * Add a step handler.
     * 
     * @param handler
     *        step handler to add
     */
    public void add(final PatriusStepHandler handler) {
        this.handlers.add(handler);
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        for (final PatriusStepHandler handler : this.handlers) {
            handler.init(s0, t);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void handleStep(final PatriusStepInterpolator interpolator,
                           final boolean isLast) throws PropagationException {
        for (final PatriusStepHandler handler : this.handlers) {
            handler.handleStep(interpolator, isLast);
        }
    }

}
