/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
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
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.exception;

import fr.cnes.sirius.patrius.math.exception.util.Localizable;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Error thrown when a numerical computation can not be performed because the
 * numerical result failed to converge to a finite value.
 * 
 * @since 2.2
 * @version $Id: ConvergenceException.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class ConvergenceException extends MathIllegalStateException {
    /** Serializable version Id. */
    private static final long serialVersionUID = 4330003017885151975L;

    /**
     * Construct the exception.
     */
    public ConvergenceException() {
        this(PatriusMessages.CONVERGENCE_FAILED);
    }

    /**
     * Construct the exception with a specific context and arguments.
     * 
     * @param pattern
     *        Message pattern providing the specific context of
     *        the error.
     * @param args
     *        Arguments.
     */
    public ConvergenceException(final Localizable pattern,
        final Object... args) {
        super();
        this.getContext().addMessage(pattern, args);
    }
}
