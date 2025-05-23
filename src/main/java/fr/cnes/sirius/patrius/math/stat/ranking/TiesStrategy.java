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
 * Strategies for handling tied values in rank transformations.
 * <ul>
 * <li>SEQUENTIAL - Ties are assigned ranks in order of occurrence in the original array, for example (1,3,4,3) is
 * ranked as (1,2,4,3)</li>
 * <li>MINIMUM - Tied values are assigned the minimum applicable rank, or the rank of the first occurrence. For example,
 * (1,3,4,3) is ranked as (1,2,4,2)</li>
 * <li>MAXIMUM - Tied values are assigned the maximum applicable rank, or the rank of the last occurrence. For example,
 * (1,3,4,3) is ranked as (1,3,4,3)</li>
 * <li>AVERAGE - Tied values are assigned the average of the applicable ranks. For example, (1,3,4,3) is ranked as
 * (1,2.5,4,2.5)</li>
 * <li>RANDOM - Tied values are assigned a random integer rank from among the applicable values. The assigned rank will
 * always be an integer, (inclusively) between the values returned by the MINIMUM and MAXIMUM strategies.</li>
 * </ul>
 * 
 * @since 2.0
 * @version $Id: TiesStrategy.java 18108 2017-10-04 06:45:27Z bignon $
 */
public enum TiesStrategy {

    /** Ties assigned sequential ranks in order of occurrence */
    SEQUENTIAL,

    /** Ties get the minimum applicable rank */
    MINIMUM,

    /** Ties get the maximum applicable rank */
    MAXIMUM,

    /** Ties get the average of applicable ranks */
    AVERAGE,

    /** Ties get a random integral value from among applicable ranks */
    RANDOM
}
