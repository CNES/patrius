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

import java.util.List;

/**
 * Interface indicating that the chromosome represents a permutation of objects.
 * 
 * @param <T>
 *        type of the permuted objects
 * @since 2.0
 * @version $Id: PermutationChromosome.java 18108 2017-10-04 06:45:27Z bignon $
 */
public interface PermutationChromosome<T> {

    /**
     * Permutes the <code>sequence</code> of objects of type T according to the
     * permutation this chromosome represents. For example, if this chromosome
     * represents a permutation (3,0,1,2), and the unpermuted sequence is
     * (a,b,c,d), this yields (d,a,b,c).
     * 
     * @param sequence
     *        the unpermuted (original) sequence of objects
     * @return permutation of <code>sequence</code> represented by this permutation
     */
    List<T> decode(List<T> sequence);

}
