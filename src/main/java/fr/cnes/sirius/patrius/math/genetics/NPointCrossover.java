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
package fr.cnes.sirius.patrius.math.genetics;

import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException;
import fr.cnes.sirius.patrius.math.random.RandomGenerator;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * N-point crossover policy. For each iteration a random crossover point is
 * selected and the first part from each parent is copied to the corresponding
 * child, and the second parts are copied crosswise.
 * 
 * Example (2-point crossover):
 * 
 * <pre>
 * -C- denotes a crossover point
 *           -C-       -C-                         -C-        -C-
 * p1 = (1 0  | 1 0 0 1 | 0 1 1)    X    p2 = (0 1  | 1 0 1 0  | 1 1 1)
 *      \----/ \-------/ \-----/              \----/ \--------/ \-----/
 *        ||      (*)       ||                  ||      (**)       ||
 *        VV      (**)      VV                  VV      (*)        VV
 *      /----\ /--------\ /-----\             /----\ /--------\ /-----\
 * c1 = (1 0  | 1 0 1 0  | 0 1 1)    X   c2 = (0 1  | 1 0 0 1  | 0 1 1)
 * </pre>
 * 
 * This policy works only on {@link AbstractListChromosome}, and therefore it
 * is parameterized by T. Moreover, the chromosomes must have same lengths.
 * 
 * @param <T>
 *        generic type of the {@link AbstractListChromosome}s for crossover
 * @since 3.1
 * @version $Id: NPointCrossover.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class NPointCrossover<T> implements CrossoverPolicy {

    /** The number of crossover points. */
    private final int crossoverPoints;

    /**
     * Creates a new {@link NPointCrossover} policy using the given number of points.
     * <p>
     * <b>Note</b>: the number of crossover points must be &lt; <code>chromosome length - 1</code>. This condition can
     * only be checked at runtime, as the chromosome length is not known in advance.
     * 
     * @param crossoverPointsIn
     *        the number of crossover points
     * @throws NotStrictlyPositiveException
     *         if the number of {@code crossoverPoints} is not strictly positive
     */
    public NPointCrossover(final int crossoverPointsIn) {
        if (crossoverPointsIn <= 0) {
            throw new NotStrictlyPositiveException(crossoverPointsIn);
        }
        this.crossoverPoints = crossoverPointsIn;
    }

    /**
     * Returns the number of crossover points used by this {@link CrossoverPolicy}.
     * 
     * @return the number of crossover points
     */
    public int getCrossoverPoints() {
        return this.crossoverPoints;
    }

    /**
     * Performs a N-point crossover. N random crossover points are selected and are used
     * to divide the parent chromosomes into segments. The segments are copied in alternate
     * order from the two parents to the corresponding child chromosomes.
     * 
     * Example (2-point crossover):
     * 
     * <pre>
     * -C- denotes a crossover point
     *           -C-       -C-                         -C-        -C-
     * p1 = (1 0  | 1 0 0 1 | 0 1 1)    X    p2 = (0 1  | 1 0 1 0  | 1 1 1)
     *      \----/ \-------/ \-----/              \----/ \--------/ \-----/
     *        ||      (*)       ||                  ||      (**)       ||
     *        VV      (**)      VV                  VV      (*)        VV
     *      /----\ /--------\ /-----\             /----\ /--------\ /-----\
     * c1 = (1 0  | 1 0 1 0  | 0 1 1)    X   c2 = (0 1  | 1 0 0 1  | 0 1 1)
     * </pre>
     * 
     * @param first
     *        first parent (p1)
     * @param second
     *        second parent (p2)
     * @return pair of two children (c1,c2)
     * @throws MathIllegalArgumentException
     *         iff one of the chromosomes is
     *         not an instance of {@link AbstractListChromosome}
     * @throws DimensionMismatchException
     *         if the length of the two chromosomes is different
     */
    @Override
    @SuppressWarnings("unchecked")
    // OK because of instanceof checks
            public
            ChromosomePair crossover(final Chromosome first, final Chromosome second) {

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
     * @throws NumberIsTooLargeException
     *         if the number of crossoverPoints is too large for the actual chromosomes
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
        if (this.crossoverPoints >= length) {
            throw new NumberIsTooLargeException(this.crossoverPoints, length, false);
        }

        // array representations of the parents
        final List<T> parent1Rep = first.getRepresentation();
        final List<T> parent2Rep = second.getRepresentation();
        // and of the children
        final ArrayList<T> child1Rep = new ArrayList<>(first.getLength());
        final ArrayList<T> child2Rep = new ArrayList<>(second.getLength());

        final RandomGenerator random = GeneticAlgorithm.getRandomGenerator();

        ArrayList<T> c1 = child1Rep;
        ArrayList<T> c2 = child2Rep;

        int remainingPoints = this.crossoverPoints;
        int lastIndex = 0;
        for (int i = 0; i < this.crossoverPoints; i++, remainingPoints--) {
            // select the next crossover point at random
            final int crossoverIndex = 1 + lastIndex + random.nextInt(length - lastIndex - remainingPoints);

            // copy the current segment
            for (int j = lastIndex; j < crossoverIndex; j++) {
                c1.add(parent1Rep.get(j));
                c2.add(parent2Rep.get(j));
            }

            // swap the children for the next segment
            final ArrayList<T> tmp = c1;
            c1 = c2;
            c2 = tmp;

            lastIndex = crossoverIndex;
        }

        // copy the last segment
        for (int j = lastIndex; j < length; j++) {
            c1.add(parent1Rep.get(j));
            c2.add(parent2Rep.get(j));
        }

        return new ChromosomePair(first.newFixedLengthChromosome(child1Rep),
            second.newFixedLengthChromosome(child2Rep));
    }
}
