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
package fr.cnes.sirius.patrius.math.stat.ranking;

/**
 * Strategies for handling NaN values in rank transformations.
 * <ul>
 * <li>MINIMAL - NaNs are treated as minimal in the ordering, equivalent to (that is, tied with)
 * <code>Double.NEGATIVE_INFINITY</code>.</li>
 * <li>MAXIMAL - NaNs are treated as maximal in the ordering, equivalent to <code>Double.POSITIVE_INFINITY</code></li>
 * <li>REMOVED - NaNs are removed before the rank transform is applied</li>
 * <li>FIXED - NaNs are left "in place," that is the rank transformation is applied to the other elements in the input
 * array, but the NaN elements are returned unchanged.</li>
 * <li>FAILED - If any NaN is encountered in the input array, an appropriate exception is thrown</li>
 * </ul>
 * 
 * @since 2.0
 * @version $Id: NaNStrategy.java 18108 2017-10-04 06:45:27Z bignon $
 */
public enum NaNStrategy {

    /** NaNs are considered minimal in the ordering */
    MINIMAL,

    /** NaNs are considered maximal in the ordering */
    MAXIMAL,

    /** NaNs are removed before computing ranks */
    REMOVED,

    /** NaNs are left in place */
    FIXED,

    /**
     * NaNs result in an exception
     * 
     * @since 3.1
     */
    FAILED
}
