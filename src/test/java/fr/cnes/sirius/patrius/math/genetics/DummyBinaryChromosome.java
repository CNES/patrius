/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
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
 *
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.genetics;

import java.util.List;

/**
 * Implementation of BinaryChromosome for testing purposes
 */
public class DummyBinaryChromosome extends BinaryChromosome {

    public DummyBinaryChromosome(final List<Integer> representation) {
        super(representation);
    }

    public DummyBinaryChromosome(final Integer[] representation) {
        super(representation);
    }

    @Override
    public AbstractListChromosome<Integer> newFixedLengthChromosome(final List<Integer> chromosomeRepresentation) {
        return new DummyBinaryChromosome(chromosomeRepresentation);
    }

    @Override
    public double fitness() {
        // uninteresting
        return 0;
    }

}
