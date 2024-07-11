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
package fr.cnes.sirius.patrius.math.geometry.euclidean.oned;

import fr.cnes.sirius.patrius.math.geometry.partitioning.AbstractSubHyperplane;
import fr.cnes.sirius.patrius.math.geometry.partitioning.Hyperplane;
import fr.cnes.sirius.patrius.math.geometry.partitioning.Region;
import fr.cnes.sirius.patrius.math.geometry.partitioning.Side;

/**
 * This class represents sub-hyperplane for {@link OrientedPoint}.
 * <p>
 * An hyperplane in 1D is a simple point, its orientation being a boolean.
 * </p>
 * <p>
 * Instances of this class are guaranteed to be immutable.
 * </p>
 * 
 * @version $Id: SubOrientedPoint.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
public class SubOrientedPoint extends AbstractSubHyperplane<Euclidean1D, Euclidean1D> {

    /** Threshold. */
    private static final double THRESHOLD = 1.0e-10;

    /**
     * Simple constructor.
     * 
     * @param hyperplane
     *        underlying hyperplane
     * @param remainingRegion
     *        remaining region of the hyperplane
     */
    public SubOrientedPoint(final Hyperplane<Euclidean1D> hyperplane,
        final Region<Euclidean1D> remainingRegion) {
        super(hyperplane, remainingRegion);
    }

    /** {@inheritDoc} */
    @Override
    public double getSize() {
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    // CHECKSTYLE: stop IllegalType check
    // Reason: Commons-Math code kept as such
            protected
            AbstractSubHyperplane<Euclidean1D, Euclidean1D> buildNew(final Hyperplane<Euclidean1D> hyperplane,
                                                                     final Region<Euclidean1D> remainingRegion) {
        // CHECKSTYLE: resume IllegalType check
        return new SubOrientedPoint(hyperplane, remainingRegion);
    }

    /** {@inheritDoc} */
    @Override
    public Side side(final Hyperplane<Euclidean1D> hyperplane) {
        final double global = hyperplane.getOffset(((OrientedPoint) this.getHyperplane()).getLocation());
        return (global < -THRESHOLD) ? Side.MINUS : ((global > THRESHOLD) ? Side.PLUS : Side.HYPER);
    }

    /** {@inheritDoc} */
    @Override
    public SplitSubHyperplane<Euclidean1D> split(final Hyperplane<Euclidean1D> hyperplane) {
        final double global = hyperplane.getOffset(((OrientedPoint) this.getHyperplane()).getLocation());
        return (global < -THRESHOLD) ?
            new SplitSubHyperplane<>(null, this) :
            new SplitSubHyperplane<>(this, null);
    }
}
