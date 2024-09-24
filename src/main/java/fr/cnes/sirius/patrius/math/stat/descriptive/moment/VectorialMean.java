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
package fr.cnes.sirius.patrius.math.stat.descriptive.moment;

import java.io.Serializable;
import java.util.Arrays;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;

/**
 * Returns the arithmetic mean of the available vectors.
 * 
 * @since 1.2
 * @version $Id: VectorialMean.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class VectorialMean implements Serializable {

     /** Serializable UID. */
    private static final long serialVersionUID = 8223009086481006892L;

    /** Means for each component. */
    private final Mean[] means;

    /**
     * Constructs a VectorialMean.
     * 
     * @param dimension
     *        vectors dimension
     */
    public VectorialMean(final int dimension) {
        this.means = new Mean[dimension];
        for (int i = 0; i < dimension; ++i) {
            this.means[i] = new Mean();
        }
    }

    /**
     * Add a new vector to the sample.
     * 
     * @param v
     *        vector to add
     * @throws DimensionMismatchException
     *         if the vector does not have the right dimension
     */
    public void increment(final double[] v) {
        if (v.length != this.means.length) {
            throw new DimensionMismatchException(v.length, this.means.length);
        }
        for (int i = 0; i < v.length; ++i) {
            this.means[i].increment(v[i]);
        }
    }

    /**
     * Get the mean vector.
     * 
     * @return mean vector
     */
    public double[] getResult() {
        final double[] result = new double[this.means.length];
        for (int i = 0; i < result.length; ++i) {
            result[i] = this.means[i].getResult();
        }
        return result;
    }

    /**
     * Get the number of vectors in the sample.
     * 
     * @return number of vectors in the sample
     */
    public long getN() {
        return (this.means.length == 0) ? 0 : this.means[0].getN();
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(this.means);
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof VectorialMean)) {
            return false;
        }
        final VectorialMean other = (VectorialMean) obj;
        return Arrays.equals(this.means, other.means);
    }

}
