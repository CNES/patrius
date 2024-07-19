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
 * VERSION::FA:273:20/10/2014:Minor code problems
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.utils.exception;

/**
 * This class allows to wrap {@link PatriusException} instances in {@code RuntimeException}.
 * 
 * <p>
 * Wrapping {@link PatriusException} instances is useful when a low level method throws one such exception and this
 * method must be called from another one which does not allow this exception. Typical examples are propagation methods
 * that are used inside Apache Commons optimizers, integrators or solvers.
 * </p>
 * 
 * @author Luc Maisonobe
 */
public class PatriusExceptionWrapper extends RuntimeException {

    /** serializable UID. */
    private static final long serialVersionUID = -2369002825757407992L;

    /** Underlying Orekit exception. */
    private final PatriusException wrappedException;

    /**
     * Simple constructor.
     * 
     * @param wrappedExceptionIn
     *        Orekit exception to wrap
     */
    public PatriusExceptionWrapper(final PatriusException wrappedExceptionIn) {
        super(wrappedExceptionIn);
        this.wrappedException = wrappedExceptionIn;
    }

    /**
     * Get the wrapped exception.
     * 
     * @return wrapped exception
     */
    public PatriusException getException() {
        return this.wrappedException;
    }
}
