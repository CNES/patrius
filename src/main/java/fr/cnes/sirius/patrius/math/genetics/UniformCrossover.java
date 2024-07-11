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

import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.random.RandomGenerator;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Perform Uniform Crossover [UX] on the specified chromosomes. A fixed mixing
 * ratio is used to combine genes from the first and second parents, e.g. using a
 * ratio of 0.5 would result in approximately 50% of genes coming from each
 * parent. This is typically a poor method of crossover, but empirical evidence
 * suggests that it is more exploratory and results in a larger part of the
 * problem space being searched.
 * <p>
 * This crossover policy evaluates each gene of the parent chromosomes by chosing a uniform random number {@code p} in
 * the range [0, 1]. If {@code p} &lt; {@code ratio}, the parent genes are swapped. This means with a ratio of 0.7, 30%
 * of the genes from the first parent and 70% from the second parent will be selected for the first offspring (and vice
 * versa for the second offspring).
 * <p>
 * This policy works only on {@link AbstractListChromosome}, and therefore it is parameterized by T. Moreover, the
 * chromosomes must have same lengths.
 * 
 * @see <a href="http://en.wikipedia.org/wiki/Crossover_%28genetic_algorithm%29">Crossover techniques (Wikipedia)</a>
 * @see <a href="http://www.obitko.com/tutorials/genetic-algorithms/crossover-mutation.php">Crossover (Obitko.com)</a>
 * @see <a href="http://www.tomaszgwiazda.com/uniformX.htm">Uniform crossover</a>
 * @param <T>
 *        generic type of the {@link AbstractListChromosome}s for crossover
 * @since 3.1
 * @version $Id: UniformCrossover.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class UniformCrossover<T> implements CrossoverPolicy {

    /** The mixing ratio. */
    private final double ratio;

    /**
     * Creates a new {@link UniformCrossover} policy using the given mixing ratio.
     * 
     * @param ratioIn
     *        the mixing ratio
     * @throws OutOfRangeException
     *         if the mixing ratio is outside the [0, 1] range
     */
    public UniformCrossover(final double ratioIn) {
        if (ratioIn < 0.0d || ratioIn > 1.0d) {
            throw new OutOfRangeException(PatriusMessages.CROSSOVER_RATE, ratioIn, 0.0d, 1.0d);
        }
        this.ratio = ratioIn;
    }

    /**
     * Returns the mixing ratio used by this {@link CrossoverPolicy}.
     * 
     * @return the mixing ratio
     */
    public double getRatio() {
        return this.ratio;
    }

    /**
     * {@inheritDoc}
     * 
     * @throws MathIllegalArgumentException
     *         iff one of the chromosomes is
     *         not an instance of {@link AbstractListChromosome}
     * @throws DimensionMismatchException
     *         if the length of the two chromosomes is different
     */
    @Override
    @SuppressWarnings("unchecked")
    public ChromosomePair crossover(final Chromosome first, final Chromosome second) {

        if (!(first instanceof AbstractListChromosome<?> && second instanceof AbstractListChromosome<?>)) {
            throw new MathIllegalArgumentException(PatriusMessages.INVALID_FIXED_LENGTH_CHROMOSOME);
        }
        return this.mate((AbstractListChromosome<T>) first, (AbstractListChromosome<T>) second);
    }

    /**
     * Helper for {@link #crossover(Chromosome, Chromosome)}. Performs the actual crossover.
     * 
     * @param first
     *        the first chromosome
     * @param second
     *        the second chromosome
     * @return the pair of new chromosomes that resulted from the crossover
     * @throws DimensionMismatchException
     *         if the length of the two chromosomes is different
     */
    // CHECKSTYLE: stop IllegalType check
    // Reason: Commons-Math code kept as such
    private ChromosomePair mate(final AbstractListChromosome<T> first,
                                final AbstractListChromosome<T> second) {
        // CHECKSTYLE: resume IllegalType check
        final int length = first.getLength();
        if (length != second.getLength()) {
            throw new DimensionMismatchException(second.getLength(), length);
        }

        // array representations of the parents
        final List<T> parent1Rep = first.getRepresentation();
        final List<T> parent2Rep = second.getRepresentation();
        // and of the children
        final List<T> child1Rep = new ArrayList<T>(first.getLength());
        final List<T> child2Rep = new ArrayList<T>(second.getLength());

        final RandomGenerator random = GeneticAlgorithm.getRandomGenerator();

        for (int index = 0; index < length; index++) {

            if (random.nextDouble() < this.ratio) {
                // swap the bits -> take other parent
                child1Rep.add(parent2Rep.get(index));
                child2Rep.add(parent1Rep.get(index));
            } else {
                child1Rep.add(parent1Rep.get(index));
                child2Rep.add(parent2Rep.get(index));
            }
        }

        return new ChromosomePair(first.newFixedLengthChromosome(child1Rep),
            second.newFixedLengthChromosome(child2Rep));
    }
}
