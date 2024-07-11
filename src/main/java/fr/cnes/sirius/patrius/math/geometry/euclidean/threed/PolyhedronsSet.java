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
package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

import java.awt.geom.AffineTransform;
import java.util.Collection;

import fr.cnes.sirius.patrius.math.geometry.Vector;
import fr.cnes.sirius.patrius.math.geometry.euclidean.oned.Euclidean1D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.twod.Euclidean2D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.twod.SubLine;
import fr.cnes.sirius.patrius.math.geometry.euclidean.twod.Vector2D;
import fr.cnes.sirius.patrius.math.geometry.partitioning.AbstractRegion;
import fr.cnes.sirius.patrius.math.geometry.partitioning.BSPTree;
import fr.cnes.sirius.patrius.math.geometry.partitioning.BSPTreeVisitor;
import fr.cnes.sirius.patrius.math.geometry.partitioning.BoundaryAttribute;
import fr.cnes.sirius.patrius.math.geometry.partitioning.Hyperplane;
import fr.cnes.sirius.patrius.math.geometry.partitioning.Region;
import fr.cnes.sirius.patrius.math.geometry.partitioning.RegionFactory;
import fr.cnes.sirius.patrius.math.geometry.partitioning.SubHyperplane;
import fr.cnes.sirius.patrius.math.geometry.partitioning.Transform;
import fr.cnes.sirius.patrius.math.util.MathLib;

//CHECKSTYLE: stop IllegalType check
//Reason: Commons-Math code kept as such

/**
 * This class represents a 3D region: a set of polyhedrons.
 * 
 * @version $Id: PolyhedronsSet.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
public class PolyhedronsSet extends AbstractRegion<Euclidean3D, Euclidean2D> {

    /** Threshold. */
    private static final double THRESHOLD = 1.0e-10;

    /**
     * Build a polyhedrons set representing the whole real line.
     */
    public PolyhedronsSet() {
        super();
    }

    /**
     * Build a polyhedrons set from a BSP tree.
     * <p>
     * The leaf nodes of the BSP tree <em>must</em> have a {@code Boolean} attribute representing the inside status of
     * the corresponding cell (true for inside cells, false for outside cells). In order to avoid building too many
     * small objects, it is recommended to use the predefined constants {@code Boolean.TRUE} and {@code Boolean.FALSE}
     * </p>
     * 
     * @param tree
     *        inside/outside BSP tree representing the region
     */
    public PolyhedronsSet(final BSPTree<Euclidean3D> tree) {
        super(tree);
    }

    /**
     * Build a polyhedrons set from a Boundary REPresentation (B-rep).
     * <p>
     * The boundary is provided as a collection of {@link SubHyperplane sub-hyperplanes}. Each sub-hyperplane has the
     * interior part of the region on its minus side and the exterior on its plus side.
     * </p>
     * <p>
     * The boundary elements can be in any order, and can form several non-connected sets (like for example polyhedrons
     * with holes or a set of disjoint polyhedrons considered as a whole). In fact, the elements do not even need to be
     * connected together (their topological connections are not used here). However, if the boundary does not really
     * separate an inside open from an outside open (open having here its topological meaning), then subsequent calls to
     * the {@link Region#checkPoint(Vector) checkPoint} method will not be meaningful anymore.
     * </p>
     * <p>
     * If the boundary is empty, the region will represent the whole space.
     * </p>
     * 
     * @param boundary
     *        collection of boundary elements, as a
     *        collection of {@link SubHyperplane SubHyperplane} objects
     */
    public PolyhedronsSet(final Collection<SubHyperplane<Euclidean3D>> boundary) {
        super(boundary);
    }

    /**
     * Build a parallellepipedic box.
     * 
     * @param xMin
     *        low bound along the x direction
     * @param xMax
     *        high bound along the x direction
     * @param yMin
     *        low bound along the y direction
     * @param yMax
     *        high bound along the y direction
     * @param zMin
     *        low bound along the z direction
     * @param zMax
     *        high bound along the z direction
     */
    public PolyhedronsSet(final double xMin, final double xMax,
        final double yMin, final double yMax,
        final double zMin, final double zMax) {
        super(buildBoundary(xMin, xMax, yMin, yMax, zMin, zMax));
    }

    /**
     * Build a parallellepipedic box boundary.
     * 
     * @param xMin
     *        low bound along the x direction
     * @param xMax
     *        high bound along the x direction
     * @param yMin
     *        low bound along the y direction
     * @param yMax
     *        high bound along the y direction
     * @param zMin
     *        low bound along the z direction
     * @param zMax
     *        high bound along the z direction
     * @return boundary tree
     */
    private static BSPTree<Euclidean3D> buildBoundary(final double xMin, final double xMax,
                                                      final double yMin, final double yMax,
                                                      final double zMin, final double zMax) {
        final Plane pxMin = new Plane(new Vector3D(xMin, 0, 0), Vector3D.MINUS_I);
        final Plane pxMax = new Plane(new Vector3D(xMax, 0, 0), Vector3D.PLUS_I);
        final Plane pyMin = new Plane(new Vector3D(0, yMin, 0), Vector3D.MINUS_J);
        final Plane pyMax = new Plane(new Vector3D(0, yMax, 0), Vector3D.PLUS_J);
        final Plane pzMin = new Plane(new Vector3D(0, 0, zMin), Vector3D.MINUS_K);
        final Plane pzMax = new Plane(new Vector3D(0, 0, zMax), Vector3D.PLUS_K);
        @SuppressWarnings("unchecked")
        final Region<Euclidean3D> boundary =
            new RegionFactory<Euclidean3D>().buildConvex(pxMin, pxMax, pyMin, pyMax, pzMin, pzMax);
        return boundary.getTree(false);
    }

    /** {@inheritDoc} */
    @Override
    public PolyhedronsSet buildNew(final BSPTree<Euclidean3D> tree) {
        return new PolyhedronsSet(tree);
    }

    /** {@inheritDoc} */
    @Override
    protected void computeGeometricalProperties() {

        // compute the contribution of all boundary facets
        this.getTree(true).visit(new FacetsContributionVisitor());

        if (this.getSize() < 0) {
            // the polyhedrons set as a finite outside
            // surrounded by an infinite inside
            this.setSize(Double.POSITIVE_INFINITY);
            this.setBarycenter(Vector3D.NaN);
        } else {
            // the polyhedrons set is finite, apply the remaining scaling factors
            this.setSize(this.getSize() / 3.0);
            this.setBarycenter(new Vector3D(1.0 / (4 * this.getSize()), (Vector3D) this.getBarycenter()));
        }

    }

    /**
     * Get the first sub-hyperplane crossed by a semi-infinite line.
     * 
     * @param point
     *        start point of the part of the line considered
     * @param line
     *        line to consider (contains point)
     * @return the first sub-hyperplaned crossed by the line after the
     *         given point, or null if the line does not intersect any
     *         sub-hyperplaned
     */
    public SubHyperplane<Euclidean3D> firstIntersection(final Vector3D point, final Line line) {
        return this.recurseFirstIntersection(this.getTree(true), point, line);
    }

    /**
     * Get the first sub-hyperplane crossed by a semi-infinite line.
     * 
     * @param node
     *        current node
     * @param point
     *        start point of the part of the line considered
     * @param line
     *        line to consider (contains point)
     * @return the first sub-hyperplaned crossed by the line after the
     *         given point, or null if the line does not intersect any
     *         sub-hyperplaned
     */
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    private SubHyperplane<Euclidean3D> recurseFirstIntersection(final BSPTree<Euclidean3D> node,
                                                                final Vector3D point,
                                                                final Line line) {
        // CHECKSTYLE: resume ReturnCount check

        final SubHyperplane<Euclidean3D> cut = node.getCut();
        if (cut == null) {
            return null;
        }
        final BSPTree<Euclidean3D> minus = node.getMinus();
        final BSPTree<Euclidean3D> plus = node.getPlus();
        final Plane plane = (Plane) cut.getHyperplane();

        // establish search order
        final double offset = plane.getOffset(point);
        final boolean in = MathLib.abs(offset) < THRESHOLD;
        final BSPTree<Euclidean3D> near;
        final BSPTree<Euclidean3D> far;
        if (offset < 0) {
            near = minus;
            far = plus;
        } else {
            near = plus;
            far = minus;
        }

        if (in) {
            // search in the cut hyperplane
            final SubHyperplane<Euclidean3D> facet = this.boundaryFacet(point, node);
            if (facet != null) {
                return facet;
            }
        }

        // search in the near branch
        final SubHyperplane<Euclidean3D> crossed = this.recurseFirstIntersection(near, point, line);
        if (crossed != null) {
            return crossed;
        }

        if (!in) {
            // search in the cut hyperplane
            final Vector3D hit3D = plane.intersection(line);
            if (hit3D != null) {
                final SubHyperplane<Euclidean3D> facet = this.boundaryFacet(hit3D, node);
                if (facet != null) {
                    return facet;
                }
            }
        }

        // search in the far branch
        return this.recurseFirstIntersection(far, point, line);

    }

    /**
     * Check if a point belongs to the boundary part of a node.
     * 
     * @param point
     *        point to check
     * @param node
     *        node containing the boundary facet to check
     * @return the boundary facet this points belongs to (or null if it
     *         does not belong to any boundary facet)
     */
    private SubHyperplane<Euclidean3D> boundaryFacet(final Vector3D point,
                                                     final BSPTree<Euclidean3D> node) {
        final Vector2D point2D = ((Plane) node.getCut().getHyperplane()).toSubSpace(point);
        @SuppressWarnings("unchecked")
        final BoundaryAttribute<Euclidean3D> attribute =
            (BoundaryAttribute<Euclidean3D>) node.getAttribute();
        final SubHyperplane<Euclidean3D> res;
        if ((attribute.getPlusOutside() != null) &&
            (((SubPlane) attribute.getPlusOutside()).getRemainingRegion().checkPoint(point2D) == Location.INSIDE)) {
            res = attribute.getPlusOutside();
        } else if ((attribute.getPlusInside() != null) &&
            (((SubPlane) attribute.getPlusInside()).getRemainingRegion().checkPoint(point2D) == Location.INSIDE)) {
            res = attribute.getPlusInside();
        } else {
            res = null;
        }
        return res;
    }

    /**
     * Rotate the region around the specified point.
     * <p>
     * The instance is not modified, a new instance is created.
     * </p>
     * 
     * @param center
     *        rotation center
     * @param rotation
     *        vectorial rotation operator
     * @return a new instance representing the rotated region
     */
    public PolyhedronsSet rotate(final Vector3D center, final Rotation rotation) {
        return (PolyhedronsSet) this.applyTransform(new RotationTransform(center, rotation));
    }

    /**
     * Translate the region by the specified amount.
     * <p>
     * The instance is not modified, a new instance is created.
     * </p>
     * 
     * @param translation
     *        translation to apply
     * @return a new instance representing the translated region
     */
    public PolyhedronsSet translate(final Vector3D translation) {
        return (PolyhedronsSet) this.applyTransform(new TranslationTransform(translation));
    }

    /** 3D rotation as a Transform. */
    private static class RotationTransform implements Transform<Euclidean3D, Euclidean2D> {

        /** Center point of the rotation. */
        private final Vector3D center;

        /** Vectorial rotation. */
        private final Rotation rotation;

        /** Cached original hyperplane. */
        private Plane cachedOriginal;

        /** Cached 2D transform valid inside the cached original hyperplane. */
        private Transform<Euclidean2D, Euclidean1D> cachedTransform;

        /**
         * Build a rotation transform.
         * 
         * @param centerIn
         *        center point of the rotation
         * @param rotationIn
         *        vectorial rotation
         */
        public RotationTransform(final Vector3D centerIn, final Rotation rotationIn) {
            this.center = centerIn;
            this.rotation = rotationIn;
        }

        /** {@inheritDoc} */
        @Override
        public Vector3D apply(final Vector<Euclidean3D> point) {
            final Vector3D delta = ((Vector3D) point).subtract(this.center);
            return new Vector3D(1.0, this.center, 1.0, this.rotation.applyTo(delta));
        }

        /** {@inheritDoc} */
        @Override
        public Plane apply(final Hyperplane<Euclidean3D> hyperplane) {
            return ((Plane) hyperplane).rotate(this.center, this.rotation);
        }

        /** {@inheritDoc} */
        @Override
        public SubHyperplane<Euclidean2D> apply(final SubHyperplane<Euclidean2D> sub,
                                                final Hyperplane<Euclidean3D> original,
                                                final Hyperplane<Euclidean3D> transformed) {
            if (original != this.cachedOriginal) {
                // we have changed hyperplane, reset the in-hyperplane transform
                // Cache has been invalidated

                final Plane oPlane = (Plane) original;
                final Plane tPlane = (Plane) transformed;
                final Vector3D p00 = oPlane.getOrigin();
                final Vector3D p10 = oPlane.toSpace(new Vector2D(1.0, 0.0));
                final Vector3D p01 = oPlane.toSpace(new Vector2D(0.0, 1.0));
                final Vector2D tP00 = tPlane.toSubSpace(this.apply(p00));
                final Vector2D tP10 = tPlane.toSubSpace(this.apply(p10));
                final Vector2D tP01 = tPlane.toSubSpace(this.apply(p01));
                final AffineTransform at =
                    new AffineTransform(tP10.getX() - tP00.getX(), tP10.getY() - tP00.getY(),
                        tP01.getX() - tP00.getX(), tP01.getY() - tP00.getY(),
                        tP00.getX(), tP00.getY());

                this.cachedOriginal = (Plane) original;
                this.cachedTransform = fr.cnes.sirius.patrius.math.geometry.euclidean.twod.Line.getTransform(at);

            }
            // Return transformed sub-line
            return ((SubLine) sub).applyTransform(this.cachedTransform);
        }

    }

    /** 3D translation as a transform. */
    private static class TranslationTransform implements Transform<Euclidean3D, Euclidean2D> {

        /** Translation vector. */
        private final Vector3D translation;

        /** Cached original hyperplane. */
        private Plane cachedOriginal;

        /** Cached 2D transform valid inside the cached original hyperplane. */
        private Transform<Euclidean2D, Euclidean1D> cachedTransform;

        /**
         * Build a translation transform.
         * 
         * @param translationIn
         *        translation vector
         */
        public TranslationTransform(final Vector3D translationIn) {
            this.translation = translationIn;
        }

        /** {@inheritDoc} */
        @Override
        public Vector3D apply(final Vector<Euclidean3D> point) {
            return new Vector3D(1.0, (Vector3D) point, 1.0, this.translation);
        }

        /** {@inheritDoc} */
        @Override
        public Plane apply(final Hyperplane<Euclidean3D> hyperplane) {
            return ((Plane) hyperplane).translate(this.translation);
        }

        /** {@inheritDoc} */
        @Override
        public SubHyperplane<Euclidean2D> apply(final SubHyperplane<Euclidean2D> sub,
                                                final Hyperplane<Euclidean3D> original,
                                                final Hyperplane<Euclidean3D> transformed) {
            if (original != this.cachedOriginal) {
                // we have changed hyperplane, reset the in-hyperplane transform

                final Plane oPlane = (Plane) original;
                final Plane tPlane = (Plane) transformed;
                final Vector2D shift = tPlane.toSubSpace(this.apply(oPlane.getOrigin()));
                final AffineTransform at =
                    AffineTransform.getTranslateInstance(shift.getX(), shift.getY());

                this.cachedOriginal = (Plane) original;
                this.cachedTransform =
                    fr.cnes.sirius.patrius.math.geometry.euclidean.twod.Line.getTransform(at);

            }

            return ((SubLine) sub).applyTransform(this.cachedTransform);

        }

    }


    /** Visitor computing geometrical properties. */
    private class FacetsContributionVisitor implements BSPTreeVisitor<Euclidean3D> {

        /** Simple constructor. */
        public FacetsContributionVisitor() {
            PolyhedronsSet.this.setSize(0);
            PolyhedronsSet.this.setBarycenter(new Vector3D(0, 0, 0));
        }

        /** {@inheritDoc} */
        @Override
        public Order visitOrder(final BSPTree<Euclidean3D> node) {
            return Order.MINUS_SUB_PLUS;
        }

        /** {@inheritDoc} */
        @Override
        public void visitInternalNode(final BSPTree<Euclidean3D> node) {
            @SuppressWarnings("unchecked")
            final BoundaryAttribute<Euclidean3D> attribute =
                (BoundaryAttribute<Euclidean3D>) node.getAttribute();
            if (attribute.getPlusOutside() != null) {
                this.addContribution(attribute.getPlusOutside(), false);
            }
            if (attribute.getPlusInside() != null) {
                this.addContribution(attribute.getPlusInside(), true);
            }
        }

        /** {@inheritDoc} */
        @Override
        public void visitLeafNode(final BSPTree<Euclidean3D> node) {
            // Nothing to do
        }

        /**
         * Add he contribution of a boundary facet.
         * 
         * @param facet
         *        boundary facet
         * @param reversed
         *        if true, the facet has the inside on its plus side
         */
        private void addContribution(final SubHyperplane<Euclidean3D> facet, final boolean reversed) {

            // Get polygon
            final Region<Euclidean2D> polygon = ((SubPlane) facet).getRemainingRegion();
            final double area = polygon.getSize();

            if (Double.isInfinite(area)) {
                // Infinite area
                PolyhedronsSet.this.setSize(Double.POSITIVE_INFINITY);
                PolyhedronsSet.this.setBarycenter(Vector3D.NaN);
            } else {
                // General case
                final Plane plane = (Plane) facet.getHyperplane();
                final Vector3D facetB = plane.toSpace(polygon.getBarycenter());
                double scaled = area * facetB.dotProduct(plane.getNormal());
                if (reversed) {
                    scaled = -scaled;
                }

                PolyhedronsSet.this.setSize(PolyhedronsSet.this.getSize() + scaled);
                PolyhedronsSet.this.setBarycenter(new Vector3D(1.0, (Vector3D) PolyhedronsSet.this.getBarycenter(),
                    scaled, facetB));
            }
        }

    }
    // CHECKSTYLE: resume IllegalType check
}
