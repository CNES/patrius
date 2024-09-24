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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.exception;

import fr.cnes.sirius.patrius.math.exception.util.Localizable;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Exception triggered when something that shouldn't happen does happen.
 * 
 * @since 2.2
 * @version $Id: MathInternalError.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class MathInternalError extends MathIllegalStateException {
     /** Serializable UID. */
    private static final long serialVersionUID = -6276776513966934846L;
    /** URL for reporting problems. */
    private static final String REPORT_URL = "https://issues.apache.org/jira/browse/MATH";

    /**
     * Simple constructor.
     */
    public MathInternalError() {
        super();
        this.getContext().addMessage(PatriusMessages.INTERNAL_ERROR, REPORT_URL);
    }

    /**
     * Simple constructor.
     * 
     * @param cause
     *        root cause
     */
    public MathInternalError(final Throwable cause) {
        super(cause, PatriusMessages.INTERNAL_ERROR, REPORT_URL);
    }

    /**
     * Constructor accepting a localized message.
     * 
     * @param pattern
     *        Message pattern explaining the cause of the error.
     * @param args
     *        Arguments.
     */
    public MathInternalError(final Localizable pattern, final Object... args) {
        super(pattern, args);
    }
}
