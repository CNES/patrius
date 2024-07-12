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
package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

import fr.cnes.sirius.patrius.math.geometry.euclidean.oned.Vector1D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.twod.Euclidean2D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.twod.PolygonsSet;
import fr.cnes.sirius.patrius.math.geometry.euclidean.twod.Vector2D;
import fr.cnes.sirius.patrius.math.geometry.partitioning.AbstractSubHyperplane;
import fr.cnes.sirius.patrius.math.geometry.partitioning.BSPTree;
import fr.cnes.sirius.patrius.math.geometry.partitioning.Hyperplane;
import fr.cnes.sirius.patrius.math.geometry.partitioning.Region;
import fr.cnes.sirius.patrius.math.geometry.partitioning.Side;
import fr.cnes.sirius.patrius.math.geometry.partitioning.SubHyperplane;

/**
 * This class represents a sub-hyperplane for {@link Plane}.
 * 
 * @version $Id: SubPlane.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
public class SubPlane extends AbstractSubHyperplane<Euclidean3D, Euclidean2D> {

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
    public SubPlane(final Hyperplane<Euclidean3D> hyperplane,
        final Region<Euclidean2D> remainingRegion) {
        super(hyperplane, remainingRegion);
    }

    /** {@inheritDoc} */
    @Override
    // CHECKSTYLE: stop IllegalType check
    // Reason: Commons-Math code kept as such
            protected
            AbstractSubHyperplane<Euclidean3D, Euclidean2D> buildNew(final Hyperplane<Euclidean3D> hyperplane,
                                                                     final Region<Euclidean2D> remainingRegion) {
        // CHECKSTYLE: resume IllegalType check
        return new SubPlane(hyperplane, remainingRegion);
    }

    /** {@inheritDoc} */
    @Override
    public Side side(final Hyperplane<Euclidean3D> hyperplane) {

        final Plane otherPlane = (Plane) hyperplane;
        final Plane thisPlane = (Plane) this.getHyperplane();
        final Line inter = otherPlane.intersection(thisPlane);

        if (inter == null) {
            // the hyperplanes are parallel,
            // any point can be used to check their relative position
            final double global = otherPlane.getOffset(thisPlane);
            return (global < -THRESHOLD) ? Side.MINUS : ((global > THRESHOLD) ? Side.PLUS : Side.HYPER);
        }

        // create a 2D line in the otherPlane canonical 2D frame such that:
        // - the line is the crossing line of the two planes in 3D
        // - the line splits the otherPlane in two half planes with an
        // orientation consistent with the orientation of the instance
        // (i.e. the 3D half space on the plus side (resp. minus side)
        // of the instance contains the 2D half plane on the plus side
        // (resp. minus side) of the 2D line
        Vector2D p = thisPlane.toSubSpace(inter.toSpace(Vector1D.ZERO));
        Vector2D q = thisPlane.toSubSpace(inter.toSpace(Vector1D.ONE));
        final Vector3D crossP = Vector3D.crossProduct(inter.getDirection(), thisPlane.getNormal());
        if (crossP.dotProduct(otherPlane.getNormal()) < 0) {
            final Vector2D tmp = p;
            p = q;
            q = tmp;
        }
        final fr.cnes.sirius.patrius.math.geometry.euclidean.twod.Line line2D =
            new fr.cnes.sirius.patrius.math.geometry.euclidean.twod.Line(p, q);

        // check the side on the 2D plane
        return this.getRemainingRegion().side(line2D);
    }

    /**
     * Split the instance in two parts by an hyperplane.
     * 
     * @param hyperplane
     *        splitting hyperplane
     * @return an object containing both the part of the instance
     *         on the plus side of the instance and the part of the
     *         instance on the minus side of the instance
     */
    @Override
    public SplitSubHyperplane<Euclidean3D> split(final Hyperplane<Euclidean3D> hyperplane) {

        // Initialization
        final Plane otherPlane = (Plane) hyperplane;
        final Plane thisPlane = (Plane) this.getHyperplane();
        final Line inter = otherPlane.intersection(thisPlane);

        if (inter == null) {
            // the hyperplanes are parallel
            final double global = otherPlane.getOffset(thisPlane);
            return (global < -THRESHOLD) ?
                new SplitSubHyperplane<>(null, this) :
                new SplitSubHyperplane<>(this, null);
        }

        // the hyperplanes do intersect
        Vector2D p = thisPlane.toSubSpace(inter.toSpace(Vector1D.ZERO));
        Vector2D q = thisPlane.toSubSpace(inter.toSpace(Vector1D.ONE));
        final Vector3D crossP = Vector3D.crossProduct(inter.getDirection(), thisPlane.getNormal());
        if (crossP.dotProduct(otherPlane.getNormal()) < 0) {
            final Vector2D tmp = p;
            p = q;
            q = tmp;
        }
        final SubHyperplane<Euclidean2D> l2DMinus =
            new fr.cnes.sirius.patrius.math.geometry.euclidean.twod.Line(p, q).wholeHyperplane();
        final SubHyperplane<Euclidean2D> l2DPlus =
            new fr.cnes.sirius.patrius.math.geometry.euclidean.twod.Line(q, p).wholeHyperplane();

        final BSPTree<Euclidean2D> splitTree = this.getRemainingRegion().getTree(false).split(l2DMinus);
        final BSPTree<Euclidean2D> plusTree = this.getRemainingRegion().isEmpty(splitTree.getPlus()) ?
            new BSPTree<>(Boolean.FALSE) :
            new BSPTree<>(l2DPlus, new BSPTree<Euclidean2D>(Boolean.FALSE),
                splitTree.getPlus(), null);

        final BSPTree<Euclidean2D> minusTree = this.getRemainingRegion().isEmpty(splitTree.getMinus()) ?
            new BSPTree<>(Boolean.FALSE) :
            new BSPTree<>(l2DMinus, new BSPTree<Euclidean2D>(Boolean.FALSE),
                splitTree.getMinus(), null);

        // Return result
        //
        return new SplitSubHyperplane<>(new SubPlane(thisPlane.copySelf(), new PolygonsSet(plusTree)),
            new SubPlane(thisPlane.copySelf(), new PolygonsSet(minusTree)));
    }
}
