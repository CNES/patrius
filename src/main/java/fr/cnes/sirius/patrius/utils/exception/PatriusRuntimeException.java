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
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.utils.exception;

import java.util.Locale;

import fr.cnes.sirius.patrius.math.exception.util.Localizable;

/**
 * Orekit Runtime Exception.
 * 
 * @since 1.3
 */
public class PatriusRuntimeException extends RuntimeException {

    /** Serializable UID. */
    private static final long serialVersionUID = 879013611768983409L;

    /**
     * Constructs a new Orekit runtime exception with the specified detail message and
     * cause.
     * <p>
     * Note that the detail message associated with <code>cause</code> is <i>not</i> automatically incorporated in this
     * runtime exception's detail message.
     * 
     * @param message
     *        the detail message (which is saved for later retrieval
     *        by the {@link #getMessage()} method).
     * @param cause
     *        the cause (which is saved for later retrieval by the {@link #getCause()} method). (A <tt>null</tt> value
     *        is
     *        permitted, and indicates that the cause is nonexistent or
     *        unknown.)
     */
    public PatriusRuntimeException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor with a Localizable specifier instead of a String.
     * 
     * @param specifier
     *        the detail message (which is saved for later retrieval
     *        by the {@link #getMessage()} method) as a Localizable object.
     * @param cause
     *        the cause (which is saved for later retrieval by the {@link #getCause()} method). (A <tt>null</tt> value
     *        is
     *        permitted, and indicates that the cause is nonexistent or
     *        unknown.)
     */
    public PatriusRuntimeException(final Localizable specifier, final Throwable cause) {
        this(specifier.getLocalizedString(Locale.getDefault()), cause);
    }
}
