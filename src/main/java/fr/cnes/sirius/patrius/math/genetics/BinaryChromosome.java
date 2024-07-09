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

import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Chromosome represented by a vector of 0s and 1s.
 * 
 * @version $Id: BinaryChromosome.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.0
 */
//CHECKSTYLE: stop AbstractClassName check
@SuppressWarnings("PMD.AbstractNaming")
public abstract class BinaryChromosome extends AbstractListChromosome<Integer> {
    // CHECKSTYLE: resume AbstractClassName check

    /**
     * Constructor.
     * 
     * @param representation
     *        list of {0,1} values representing the chromosome
     * @throws InvalidRepresentationException
     *         iff the <code>representation</code> can not represent a valid chromosome
     */
    public BinaryChromosome(final List<Integer> representation) {
        super(representation);
    }

    /**
     * Constructor.
     * 
     * @param representation
     *        array of {0,1} values representing the chromosome
     * @throws InvalidRepresentationException
     *         iff the <code>representation</code> can not represent a valid chromosome
     */
    public BinaryChromosome(final Integer[] representation) {
        super(representation);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void checkValidity(final List<Integer> chromosomeRepresentation) {
        for (final int i : chromosomeRepresentation) {
            if (i < 0 || i > 1) {
                throw new InvalidRepresentationException(PatriusMessages.INVALID_BINARY_DIGIT,
                    i);
            }
        }
    }

    /**
     * Returns a representation of a random binary array of length <code>length</code>.
     * 
     * @param length
     *        length of the array
     * @return a random binary array of length <code>length</code>
     */
    public static List<Integer> randomBinaryRepresentation(final int length) {
        // random binary list
        final List<Integer> rList = new ArrayList<Integer>(length);
        for (int j = 0; j < length; j++) {
            rList.add(GeneticAlgorithm.getRandomGenerator().nextInt(2));
        }
        return rList;
    }

    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    /** {@inheritDoc} */
    @Override
    protected boolean isSame(final Chromosome another) {
        // CHECKSTYLE: resume ReturnCount check
        // type check
        if (!(another instanceof BinaryChromosome)) {
            return false;
        }
        final BinaryChromosome anotherBc = (BinaryChromosome) another;
        // size check
        if (this.getLength() != anotherBc.getLength()) {
            return false;
        }

        for (int i = 0; i < this.getRepresentation().size(); i++) {
            if (!(this.getRepresentation().get(i).equals(anotherBc.getRepresentation().get(i)))) {
                return false;
            }
        }
        // all is ok
        return true;
    }
}
