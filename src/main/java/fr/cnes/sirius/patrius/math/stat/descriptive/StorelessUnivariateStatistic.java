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
 * Extends the definition of {@link UnivariateStatistic} with {@link #increment} and {@link #incrementAll(double[])}
 * methods for adding
 * values and updating internal state.
 * <p>
 * This interface is designed to be used for calculating statistics that can be computed in one pass through the data
 * without storing the full array of sample values.
 * </p>
 * 
 * @version $Id: StorelessUnivariateStatistic.java 18108 2017-10-04 06:45:27Z bignon $
 */
public interface StorelessUnivariateStatistic extends UnivariateStatistic {

    /**
     * Updates the internal state of the statistic to reflect the addition of the new value.
     * 
     * @param d
     *        the new value.
     */
    void increment(double d);

    /**
     * Updates the internal state of the statistic to reflect addition of
     * all values in the values array. Does not clear the statistic first --
     * i.e., the values are added <strong>incrementally</strong> to the dataset.
     * 
     * @param values
     *        array holding the new values to add
     * @throws MathIllegalArgumentException
     *         if the array is null
     */
    void incrementAll(double[] values);

    /**
     * Updates the internal state of the statistic to reflect addition of
     * the values in the designated portion of the values array. Does not
     * clear the statistic first -- i.e., the values are added
     * <strong>incrementally</strong> to the dataset.
     * 
     * @param values
     *        array holding the new values to add
     * @param start
     *        the array index of the first value to add
     * @param length
     *        the number of elements to add
     * @throws MathIllegalArgumentException
     *         if the array is null or the index
     */
    void incrementAll(double[] values, int start, int length);

    /**
     * Returns the current value of the Statistic.
     * 
     * @return value of the statistic, <code>Double.NaN</code> if it
     *         has been cleared or just instantiated.
     */
    double getResult();

    /**
     * Returns the number of values that have been added.
     * 
     * @return the number of values.
     */
    long getN();

    /**
     * Clears the internal state of the Statistic
     */
    void clear();

    /**
     * Returns a copy of the statistic with the same internal state.
     * 
     * @return a copy of the statistic
     */
    @Override
    StorelessUnivariateStatistic copy();

}
