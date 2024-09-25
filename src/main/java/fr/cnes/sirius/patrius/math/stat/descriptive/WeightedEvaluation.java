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
package fr.cnes.sirius.patrius.math.stat.descriptive;

import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;

/**
 * Weighted evaluation for statistics.
 * 
 * @since 2.1
 * @version $Id: WeightedEvaluation.java 18108 2017-10-04 06:45:27Z bignon $
 */
public interface WeightedEvaluation {

    /**
     * Returns the result of evaluating the statistic over the input array,
     * using the supplied weights.
     * 
     * @param values
     *        input array
     * @param weights
     *        array of weights
     * @return the value of the weighted statistic applied to the input array
     * @throws MathIllegalArgumentException
     *         if either array is null, lengths
     *         do not match, weights contain NaN, negative or infinite values, or
     *         weights does not include at least on positive value
     */
    double evaluate(double[] values, double[] weights);

    /**
     * Returns the result of evaluating the statistic over the specified entries
     * in the input array, using corresponding entries in the supplied weights array.
     * 
     * @param values
     *        the input array
     * @param weights
     *        array of weights
     * @param begin
     *        the index of the first element to include
     * @param length
     *        the number of elements to include
     * @return the value of the weighted statistic applied to the included array entries
     * @throws MathIllegalArgumentException
     *         if either array is null, lengths
     *         do not match, indices are invalid, weights contain NaN, negative or
     *         infinite values, or weights does not include at least on positive value
     */
    double evaluate(double[] values, double[] weights, int begin, int length);

}
