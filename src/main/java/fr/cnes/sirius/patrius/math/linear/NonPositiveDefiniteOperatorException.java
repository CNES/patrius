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
package fr.cnes.sirius.patrius.math.linear;

import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Exception to be thrown when a symmetric, definite positive {@link RealLinearOperator} is expected.
 * Since the coefficients of the matrix are not accessible, the most
 * general definition is used to check that {@code A} is not positive
 * definite, i.e. there exists {@code x} such that {@code x' A x <= 0}.
 * In the terminology of this exception, {@code A} is the "offending"
 * linear operator and {@code x} the "offending" vector.
 * 
 * @version $Id: NonPositiveDefiniteOperatorException.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
public class NonPositiveDefiniteOperatorException
    extends MathIllegalArgumentException {
     /** Serializable UID. */
    private static final long serialVersionUID = 917034489420549847L;

    /** Creates a new instance of this class. */
    public NonPositiveDefiniteOperatorException() {
        super(PatriusMessages.NON_POSITIVE_DEFINITE_OPERATOR);
    }
}
