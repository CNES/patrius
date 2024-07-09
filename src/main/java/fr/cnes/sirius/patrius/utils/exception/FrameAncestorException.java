/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */
/*
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:410:16/04/2015: Anomalies in the Patrius Javadoc
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.utils.exception;

import fr.cnes.sirius.patrius.math.exception.util.Localizable;
import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * This class is the base class for exception thrown by
 * the {@link fr.cnes.sirius.patrius.frames.UpdatableFrame#updateTransform(Frame, Frame, Transform, AbsoluteDate)}
 * method.
 */
public class FrameAncestorException extends PatriusException {

    /** Serializable UID. */
    private static final long serialVersionUID = -8279818119798166504L;

    /**
     * Simple constructor.
     * Build an exception with a translated and formatted message
     * 
     * @param specifier
     *        format specifier (to be translated)
     * @param parts
     *        parts to insert in the format (no translation)
     */
    public FrameAncestorException(final Localizable specifier, final Object... parts) {
        super(specifier, parts);
    }

}
