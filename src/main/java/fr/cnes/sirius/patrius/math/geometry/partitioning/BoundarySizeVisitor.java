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
package fr.cnes.sirius.patrius.math.geometry.partitioning;

import fr.cnes.sirius.patrius.math.geometry.Space;

/**
 * Visitor computing the boundary size.
 * 
 * @param <S>
 *        Type of the space.
 * @version $Id: BoundarySizeVisitor.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
class BoundarySizeVisitor<S extends Space> implements BSPTreeVisitor<S> {

    /** Size of the boundary. */
    private double boundarySize;

    /**
     * Simple constructor.
     */
    public BoundarySizeVisitor() {
        this.boundarySize = 0;
    }

    /** {@inheritDoc} */
    @Override
    public Order visitOrder(final BSPTree<S> node) {
        return Order.MINUS_SUB_PLUS;
    }

    /** {@inheritDoc} */
    @Override
    public void visitInternalNode(final BSPTree<S> node) {
        @SuppressWarnings("unchecked")
        final BoundaryAttribute<S> attribute =
            (BoundaryAttribute<S>) node.getAttribute();
        if (attribute.getPlusOutside() != null) {
            this.boundarySize += attribute.getPlusOutside().getSize();
        }
        if (attribute.getPlusInside() != null) {
            this.boundarySize += attribute.getPlusInside().getSize();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void visitLeafNode(final BSPTree<S> node) {
        // Nothing to do
    }

    /**
     * Get the size of the boundary.
     * 
     * @return size of the boundary
     */
    public double getSize() {
        return this.boundarySize;
    }

}
