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
package fr.cnes.sirius.patrius.math.geometry.partitioning;

import fr.cnes.sirius.patrius.math.geometry.Space;

/**
 * This class implements the dimension-independent parts of {@link SubHyperplane}.
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
 * @param <T>
 *        Type of the embedded sub-space.
 * 
 * @version $Id: AbstractSubHyperplane.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
@SuppressWarnings("PMD.NullAssignment")
public abstract class AbstractSubHyperplane<S extends Space, T extends Space>
    implements SubHyperplane<S> {

    /** Underlying hyperplane. */
    private final Hyperplane<S> hyperplane;

    /** Remaining region of the hyperplane. */
    private final Region<T> remainingRegion;

    /**
     * Build a sub-hyperplane from an hyperplane and a region.
     * 
     * @param hyperplaneIn
     *        underlying hyperplane
     * @param remainingRegionIn
     *        remaining region of the hyperplane
     */
    protected AbstractSubHyperplane(final Hyperplane<S> hyperplaneIn,
        final Region<T> remainingRegionIn) {
        this.hyperplane = hyperplaneIn;
        this.remainingRegion = remainingRegionIn;
    }

    /**
     * Build a sub-hyperplane from an hyperplane and a region.
     * 
     * @param hyper
     *        underlying hyperplane
     * @param remaining
     *        remaining region of the hyperplane
     * @return a new sub-hyperplane
     */
    // CHECKSTYLE: stop IllegalType check
    // Reason: Commons-Math code kept as such
    protected abstract AbstractSubHyperplane<S, T> buildNew(final Hyperplane<S> hyper,
                                                            final Region<T> remaining);

    /** {@inheritDoc} */
    @Override
    public AbstractSubHyperplane<S, T> copySelf() {
        return this.buildNew(this.hyperplane, this.remainingRegion);
    }

    // CHECKSTYLE: resume IllegalType check

    /**
     * Get the underlying hyperplane.
     * 
     * @return underlying hyperplane
     */
    @Override
    public Hyperplane<S> getHyperplane() {
        return this.hyperplane;
    }

    /**
     * Get the remaining region of the hyperplane.
     * <p>
     * The returned region is expressed in the canonical hyperplane frame and has the hyperplane dimension. For example
     * a chopped hyperplane in the 3D euclidean is a 2D plane and the corresponding region is a convex 2D polygon.
     * </p>
     * 
     * @return remaining region of the hyperplane
     */
    public Region<T> getRemainingRegion() {
        return this.remainingRegion;
    }

    /** {@inheritDoc} */
    @Override
    public double getSize() {
        return this.remainingRegion.getSize();
    }

    /** {@inheritDoc} */
    // CHECKSTYLE: stop IllegalType check
    // Reason: Commons-Math code kept as such
    @Override
    public AbstractSubHyperplane<S, T> reunite(final SubHyperplane<S> other) {
        @SuppressWarnings("unchecked")
        final AbstractSubHyperplane<S, T> o = (AbstractSubHyperplane<S, T>) other;
        // CHECKSTYLE: resume IllegalType check
        return this.buildNew(this.hyperplane,
            new RegionFactory<T>().union(this.remainingRegion, o.remainingRegion));
    }

    /**
     * Apply a transform to the instance.
     * <p>
     * The instance must be a (D-1)-dimension sub-hyperplane with respect to the transform <em>not</em> a
     * (D-2)-dimension sub-hyperplane the transform knows how to transform by itself. The transform will consist in
     * transforming first the hyperplane and then the all region using the various methods provided by the transform.
     * </p>
     * 
     * @param transform
     *        D-dimension transform to apply
     * @return the transformed instance
     */
    // CHECKSTYLE: stop IllegalType check
    // Reason: Commons-Math code kept as such
    public AbstractSubHyperplane<S, T> applyTransform(final Transform<S, T> transform) {
        final Hyperplane<S> tHyperplane = transform.apply(this.hyperplane);
        final BSPTree<T> tTree =
            this.recurseTransform(this.remainingRegion.getTree(false), tHyperplane, transform);
        return this.buildNew(tHyperplane, this.remainingRegion.buildNew(tTree));
    }

    // CHECKSTYLE: resume IllegalType check

    /**
     * Recursively transform a BSP-tree from a sub-hyperplane.
     * 
     * @param node
     *        current BSP tree node
     * @param transformed
     *        image of the instance hyperplane by the transform
     * @param transform
     *        transform to apply
     * @return a new tree
     */
    private BSPTree<T> recurseTransform(final BSPTree<T> node,
                                        final Hyperplane<S> transformed,
                                        final Transform<S, T> transform) {
        if (node.getCut() == null) {
            return new BSPTree<T>(node.getAttribute());
        }

        @SuppressWarnings("unchecked")
        BoundaryAttribute<T> attribute =
            (BoundaryAttribute<T>) node.getAttribute();
        if (attribute != null) {
            final SubHyperplane<T> tPO = (attribute.getPlusOutside() == null) ?
                null : transform.apply(attribute.getPlusOutside(), this.hyperplane, transformed);
            final SubHyperplane<T> tPI = (attribute.getPlusInside() == null) ?
                null : transform.apply(attribute.getPlusInside(), this.hyperplane, transformed);
            attribute = new BoundaryAttribute<T>(tPO, tPI);
        }

        return new BSPTree<T>(transform.apply(node.getCut(), this.hyperplane, transformed),
            this.recurseTransform(node.getPlus(), transformed, transform),
            this.recurseTransform(node.getMinus(), transformed, transform),
            attribute);

    }

    /** {@inheritDoc} */
    @Override
    public abstract Side side(Hyperplane<S> hyper);

    /** {@inheritDoc} */
    @Override
    public abstract SplitSubHyperplane<S> split(Hyperplane<S> hyper);

    /** {@inheritDoc} */
    @Override
    public boolean isEmpty() {
        return this.remainingRegion.isEmpty();
    }

}
