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
package fr.cnes.sirius.patrius.math.linear;

import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * An exception to be thrown when the condition number of a {@link RealLinearOperator} is too high.
 * 
 * @version $Id: IllConditionedOperatorException.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
public class IllConditionedOperatorException
    extends MathIllegalArgumentException {
    /** Serializable version Id. */
    private static final long serialVersionUID = -7883263944530490135L;

    /**
     * Creates a new instance of this class.
     * 
     * @param cond
     *        An estimate of the condition number of the offending linear
     *        operator.
     */
    public IllConditionedOperatorException(final double cond) {
        super(PatriusMessages.ILL_CONDITIONED_OPERATOR, cond);
    }
}
