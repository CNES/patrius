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

/**
 * Reporting interface for basic univariate statistics.
 * 
 * @version $Id: StatisticalSummary.java 18108 2017-10-04 06:45:27Z bignon $
 */
public interface StatisticalSummary {

    /**
     * Returns the <a href="http://www.xycoon.com/arithmetic_mean.htm">
     * arithmetic mean </a> of the available values
     * 
     * @return The mean or Double.NaN if no values have been added.
     */
    double getMean();

    /**
     * Returns the variance of the available values.
     * 
     * @return The variance, Double.NaN if no values have been added
     *         or 0.0 for a single value set.
     */
    double getVariance();

    /**
     * Returns the standard deviation of the available values.
     * 
     * @return The standard deviation, Double.NaN if no values have been added
     *         or 0.0 for a single value set.
     */
    double getStandardDeviation();

    /**
     * Returns the maximum of the available values
     * 
     * @return The max or Double.NaN if no values have been added.
     */
    double getMax();

    /**
     * Returns the minimum of the available values
     * 
     * @return The min or Double.NaN if no values have been added.
     */
    double getMin();

    /**
     * Returns the number of available values
     * 
     * @return The number of available values
     */
    long getN();

    /**
     * Returns the sum of the values that have been added to Univariate.
     * 
     * @return The sum or Double.NaN if no values have been added
     */
    double getSum();

}
