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
 * VERSION::DM:484:25/09/2015:Get additional state from an AbsoluteDate
 */
package fr.cnes.sirius.patrius.propagation;

import java.io.Serializable;

import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * This interface represents providers for additional state data beyond {@link SpacecraftState}.
 * <p>
 * This interface is the analytical (read already integrated) counterpart of the
 * {@link fr.cnes.sirius.patrius.propagation.numerical.AdditionalEquations} interface. It allows to append various
 * additional state parameters to any {@link AbstractPropagator abstract propagator}.
 * </p>
 * 
 * @see AbstractPropagator
 * @see fr.cnes.sirius.patrius.propagation.numerical.AdditionalEquations
 * @author Luc Maisonobe
 */
public interface AdditionalStateProvider extends Serializable {

    /**
     * Get the name of the additional state.
     * 
     * @return name of the additional state
     */
    String getName();

    /**
     * Get the additional state.
     * 
     * @param date
     *        date to which additional state is computed
     * @return additional state at this date
     * @exception PropagationException
     *            if additional state cannot be computed
     */
    double[] getAdditionalState(AbsoluteDate date) throws PropagationException;

}
