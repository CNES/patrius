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
package fr.cnes.sirius.patrius.math.genetics;

import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.util.Localizable;

/**
 * Exception indicating that the representation of a chromosome is not valid.
 * 
 * @version $Id: InvalidRepresentationException.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.0
 */
public class InvalidRepresentationException extends MathIllegalArgumentException {

    /** Serialization version id */
    private static final long serialVersionUID = 1L;

    /**
     * Construct an InvalidRepresentationException with a specialized message.
     * 
     * @param pattern
     *        Message pattern.
     * @param args
     *        Arguments.
     */
    public InvalidRepresentationException(final Localizable pattern, final Object... args) {
        super(pattern, args);
    }

}