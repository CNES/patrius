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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Chromosome represented by an immutable list of a fixed length.
 * 
 * @param <T>
 *        type of the representation list
 * @version $Id: AbstractListChromosome.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.0
 */
public abstract class AbstractListChromosome<T> extends Chromosome {

    /** List representing the chromosome */
    private final List<T> representation;

    /**
     * Constructor.
     * 
     * @param representationIn
     *        inner representation of the chromosome
     * @throws InvalidRepresentationException
     *         iff the <code>representation</code> can not represent a valid chromosome
     */
    public AbstractListChromosome(final List<T> representationIn) {
        super();
        this.checkValidity(representationIn);
        this.representation = Collections.unmodifiableList(new ArrayList<T>(representationIn));
    }

    /**
     * Constructor.
     * 
     * @param representationIn
     *        inner representation of the chromosome
     * @throws InvalidRepresentationException
     *         iff the <code>representation</code> can not represent a valid chromosome
     */
    public AbstractListChromosome(final T[] representationIn) {
        this(Arrays.asList(representationIn));
    }

    /**
     * Asserts that <code>representation</code> can represent a valid chromosome.
     * 
     * @param chromosomeRepresentation
     *        representation of the chromosome
     * @throws InvalidRepresentationException
     *         iff the <code>representation</code> can not represent a valid chromosome
     */
    protected abstract void checkValidity(List<T> chromosomeRepresentation);

    /**
     * Returns the (immutable) inner representation of the chromosome.
     * 
     * @return the representation of the chromosome
     */
    protected List<T> getRepresentation() {
        return this.representation;
    }

    /**
     * Returns the length of the chromosome.
     * 
     * @return the length of the chromosome
     */
    public int getLength() {
        return this.getRepresentation().size();
    }

    /**
     * Creates a new instance of the same class as <code>this</code> is, with a given <code>arrayRepresentation</code>.
     * This is needed in crossover and mutation operators, where we need a new instance of the same class, but with
     * different array representation.
     * <p>
     * Usually, this method just calls a constructor of the class.
     * 
     * @param chromosomeRepresentation
     *        the inner array representation of the new chromosome.
     * @return new instance extended from FixedLengthChromosome with the given arrayRepresentation
     */
    // CHECKSTYLE: stop IllegalType check
    // Reason: Commons-Math code kept as such
    public abstract AbstractListChromosome<T> newFixedLengthChromosome(final List<T> chromosomeRepresentation);

    // CHECKSTYLE: resume IllegalType check

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return String.format("(f=%s %s)", this.getFitness(), this.getRepresentation());
    }
}
