/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 */
package fr.cnes.sirius.patrius.math.exception;

import fr.cnes.sirius.patrius.math.exception.util.ExceptionContext;
import fr.cnes.sirius.patrius.math.exception.util.ExceptionContextProvider;
import fr.cnes.sirius.patrius.math.exception.util.Localizable;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Base class for arithmetic exceptions.
 * It is used for all the exceptions that have the semantics of the standard {@link ArithmeticException}, but must also
 * provide a localized
 * message.
 * 
 * @since 3.0
 * @version $Id: MathArithmeticException.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class MathArithmeticException extends ArithmeticException
    implements ExceptionContextProvider {
    /** Serializable version Id. */
    private static final long serialVersionUID = -6024911025449780478L;
    /** Context. */
    private final ExceptionContext context;

    /**
     * Default constructor.
     */
    public MathArithmeticException() {
        super();
        this.context = new ExceptionContext(this);
        this.context.addMessage(PatriusMessages.ARITHMETIC_EXCEPTION);
    }

    /**
     * Constructor with a specific message.
     * 
     * @param pattern
     *        Message pattern providing the specific context of
     *        the error.
     * @param args
     *        Arguments.
     */
    public MathArithmeticException(final Localizable pattern,
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
