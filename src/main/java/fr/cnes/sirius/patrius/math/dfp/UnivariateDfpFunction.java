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
package fr.cnes.sirius.patrius.math.dfp;

/**
 * An interface representing a univariate {@link Dfp} function.
 * 
 * @version $Id: UnivariateDfpFunction.java 18108 2017-10-04 06:45:27Z bignon $
 */
public interface UnivariateDfpFunction {

    /**
     * Compute the value of the function.
     * 
     * @param x
     *        Point at which the function value should be computed.
     * @return the value.
     * @throws IllegalArgumentException
     *         when the activated method itself can
     *         ascertain that preconditions, specified in the API expressed at the
     *         level of the activated method, have been violated. In the vast
     *         majority of cases where Commons-Math throws IllegalArgumentException,
     *         it is the result of argument checking of actual parameters immediately
     *         passed to a method.
     */
    Dfp value(Dfp x);

}
