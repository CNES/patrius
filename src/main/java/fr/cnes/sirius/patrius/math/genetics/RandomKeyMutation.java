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

import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Mutation operator for {@link RandomKey}s. Changes a randomly chosen element
 * of the array representation to a random value uniformly distributed in [0,1].
 * 
 * @since 2.0
 * @version $Id: RandomKeyMutation.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class RandomKeyMutation implements MutationPolicy {

    /**
     * {@inheritDoc}
     * 
     * @throws MathIllegalArgumentException
     *         if <code>original</code> is not a {@link RandomKey} instance
     */
    @Override
    public Chromosome mutate(final Chromosome original) {
        if (!(original instanceof RandomKey<?>)) {
            throw new MathIllegalArgumentException(PatriusMessages.RANDOMKEY_MUTATION_WRONG_CLASS,
                original.getClass().getSimpleName());
        }

        final RandomKey<?> originalRk = (RandomKey<?>) original;
        final List<Double> repr = originalRk.getRepresentation();
        final int rInd = GeneticAlgorithm.getRandomGenerator().nextInt(repr.size());

        final List<Double> newRepr = new ArrayList<Double>(repr);
        newRepr.set(rInd, GeneticAlgorithm.getRandomGenerator().nextDouble());

        return originalRk.newFixedLengthChromosome(newRepr);
    }

}
