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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.utils.exception;

import fr.cnes.sirius.patrius.math.exception.util.ExceptionContextProvider;
import fr.cnes.sirius.patrius.math.exception.util.Localizable;

/**
 * This class is the base class for all specific exceptions thrown by
 * during the propagation computation.
 * 
 * @author Luc Maisonobe
 */
public class PropagationException extends PatriusException {

    /** Serializable UID. */
    private static final long serialVersionUID = 1684307015196169376L;

    /**
     * Simple constructor.
     * Build an exception with a translated and formatted message
     * 
     * @param specifier
     *        format specifier (to be translated)
     * @param parts
     *        parts to insert in the format (no translation)
     */
    public PropagationException(final Localizable specifier, final Object... parts) {
        super(specifier, parts);
    }

    /**
     * Simple constructor.
     * Build an exception from a cause and with a specified message
     * 
     * @param cause
     *        underlying cause
     * @param specifier
     *        format specifier (to be translated)
     * @param parts
     *        parts to insert in the format (no translation)
     */
    public PropagationException(final Throwable cause, final Localizable specifier,
        final Object... parts) {
        super(cause, specifier, parts);
    }

    /**
     * Simple constructor.
     * Build an exception wrapping an {@link PatriusException} instance
     * 
     * @param exception
     *        underlying cause
     */
    public PropagationException(final PatriusException exception) {
        super(exception);
    }

    /**
     * Simple constructor.
     * Build an exception wrapping an Apache Commons Math exception context exception
     * 
     * @param provider
     *        underlying cause
     */
    public PropagationException(final ExceptionContextProvider provider) {
        super(provider);
    }

    /**
     * Recover a PropagationException, possibly embedded in a {@link PatriusException}.
     * <p>
     * If the {@code OrekitException} does not embed a PropagationException, a new one will be created.
     * </p>
     * 
     * @param oe
     *        OrekitException to analyze
     * @return a (possibly embedded) PropagationException
     */
    public static PropagationException unwrap(final PatriusException oe) {

        for (Throwable t = oe; t != null; t = t.getCause()) {
            if (t instanceof PropagationException) {
                return (PropagationException) t;
            }
        }

        return new PropagationException(oe);

    }

    /**
     * Recover a PropagationException, possibly embedded in an {@link ExceptionContextProvider}.
     * <p>
     * If the {@code ExceptionContextProvider} does not embed a PropagationException, a new one will be created.
     * </p>
     * 
     * @param provider
     *        ExceptionContextProvider to analyze
     * @return a (possibly embedded) PropagationException
     */
    public static PropagationException unwrap(final ExceptionContextProvider provider) {

        for (Throwable t = provider.getContext().getThrowable(); t != null; t = t.getCause()) {
            // Loop on all exception embedded in provider
            if (t instanceof PatriusException) {
                final PropagationException res;
                if (t instanceof PropagationException) {
                    // a PropagationException was embedded in provider
                    res = (PropagationException) t;
                } else {
                    // create PropagationException from PatriusException
                    res = new PropagationException((PatriusException) t);
                }
                return res;
            }
        }

        // create new PropagationException
        return new PropagationException(provider);

    }

}
