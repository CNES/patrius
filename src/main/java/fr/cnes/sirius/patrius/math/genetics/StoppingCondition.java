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
package fr.cnes.sirius.patrius.math.genetics;

/**
 * Algorithm used to determine when to stop evolution.
 * 
 * @since 2.0
 * @version $Id: StoppingCondition.java 18108 2017-10-04 06:45:27Z bignon $
 */
public interface StoppingCondition {
    /**
     * Determine whether or not the given population satisfies the stopping condition.
     * 
     * @param population
     *        the population to test.
     * @return <code>true</code> if this stopping condition is met by the given population, <code>false</code>
     *         otherwise.
     */
    boolean isSatisfied(Population population);
}
