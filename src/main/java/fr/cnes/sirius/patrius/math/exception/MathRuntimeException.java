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

import fr.cnes.sirius.patrius.math.exception.util.ExceptionContext;
import fr.cnes.sirius.patrius.math.exception.util.ExceptionContextProvider;
import fr.cnes.sirius.patrius.math.exception.util.Localizable;

/**
 * As of release 4.0, all exceptions thrown by the Commons Math code (except {@link NullArgumentException}) inherit from
 * this class.
 * In most cases, this class should not be instantiated directly: it should
 * serve as a base class for implementing exception classes that describe a
 * specific "problem".
 * 
 * @since 3.1
 * @version $Id: MathRuntimeException.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class MathRuntimeException extends RuntimeException
    implements ExceptionContextProvider {
     /** Serializable UID. */
    private static final long serialVersionUID = 20120926L;
    /** Context. */
    private final ExceptionContext context;

    /**
     * @param pattern
     *        Message pattern explaining the cause of the error.
     * @param args
     *        Arguments.
     */
    public MathRuntimeException(final Localizable pattern,
        final Object... args) {
        super();
        this.context = new ExceptionContext(this);
        this.context.addMessage(pattern, args);
    }

    /** {@inheritDoc} */
    @Override
    public ExceptionContext getContext() {
        return this.context;
    }

    /** {@inheritDoc} */
    @Override
    public String getMessage() {
        return this.context.getMessage();
    }

    /** {@inheritDoc} */
    @Override
    public String getLocalizedMessage() {
        return this.context.getLocalizedMessage();
    }
}
