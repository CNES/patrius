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
package fr.cnes.sirius.patrius.math.geometry.partitioning;

import fr.cnes.sirius.patrius.math.geometry.Space;

/**
 * This interface represents the remaining parts of an hyperplane after
 * other parts have been chopped off.
 * 
 * <p>
 * sub-hyperplanes are obtained when parts of an {@link Hyperplane hyperplane} are chopped off by other hyperplanes that
 * intersect it. The remaining part is a convex region. Such objects appear in {@link BSPTree BSP trees} as the
 * intersection of a cut hyperplane with the convex region which it splits, the chopping hyperplanes are the cut
 * hyperplanes closer to the tree root.
 * </p>
 * 
 * @param <S>
 *        Type of the embedding space.
 * 
 * @version $Id: SubHyperplane.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
public interface SubHyperplane<S extends Space> {

    /**
     * Copy the instance.
     * <p>
     * The instance created is completely independent of the original one. A deep copy is used, none of the underlying
     * objects are shared (except for the nodes attributes and immutable objects).
     * </p>
     * 
     * @return a new sub-hyperplane, copy of the instance
     */
    SubHyperplane<S> copySelf();

    /**
     * Get the underlying hyperplane.
     * 
     * @return underlying hyperplane
     */
    Hyperplane<S> getHyperplane();

    /**
     * Check if the instance is empty.
     * 
     * @return true if the instance is empty
     */
    boolean isEmpty();

    /**
     * Get the size of the instance.
     * 
     * @return the size of the instance (this is a length in 1D, an area
     *         in 2D, a volume in 3D ...)
     */
    double getSize();

    /**
     * Compute the relative position of the instance with respect
     * to an hyperplane.
     * 
     * @param hyperplane
     *        hyperplane to check instance against
     * @return one of {@link Side#PLUS}, {@link Side#MINUS}, {@link Side#BOTH}, {@link Side#HYPER}
     */
    Side side(Hyperplane<S> hyperplane);

    /**
     * Split the instance in two parts by an hyperplane.
     * 
     * @param hyperplane
     *        splitting hyperplane
     * @return an object containing both the part of the instance
     *         on the plus side of the instance and the part of the
     *         instance on the minus side of the instance
     */
    SplitSubHyperplane<S> split(Hyperplane<S> hyperplane);

    /**
     * Compute the union of the instance and another sub-hyperplane.
     * 
     * @param other
     *        other sub-hyperplane to union (<em>must</em> be in the
     *        same hyperplane as the instance)
     * @return a new sub-hyperplane, union of the instance and other
     */
    SubHyperplane<S> reunite(SubHyperplane<S> other);

    /**
     * Class holding the results of the {@link #split split} method.
     * 
     * @param <U>
     *        Type of the embedding space.
     */
    public static class SplitSubHyperplane<U extends Space> {

        /** Part of the sub-hyperplane on the plus side of the splitting hyperplane. */
        private final SubHyperplane<U> plus;

        /** Part of the sub-hyperplane on the minus side of the splitting hyperplane. */
        private final SubHyperplane<U> minus;

        /**
         * Build a SplitSubHyperplane from its parts.
         * 
         * @param plusIn
         *        part of the sub-hyperplane on the plus side of the
         *        splitting hyperplane
         * @param minusIn
         *        part of the sub-hyperplane on the minus side of the
         *        splitting hyperplane
         */
        public SplitSubHyperplane(final SubHyperplane<U> plusIn,
            final SubHyperplane<U> minusIn) {
            this.plus = plusIn;
            this.minus = minusIn;
        }

        /**
         * Get the part of the sub-hyperplane on the plus side of the splitting hyperplane.
         * 
         * @return part of the sub-hyperplane on the plus side of the splitting hyperplane
         */
        public SubHyperplane<U> getPlus() {
            return plus;
        }

        /**
         * Get the part of the sub-hyperplane on the minus side of the splitting hyperplane.
         * 
         * @return part of the sub-hyperplane on the minus side of the splitting hyperplane
         */
        public SubHyperplane<U> getMinus() {
            return minus;
        }

    }

}
