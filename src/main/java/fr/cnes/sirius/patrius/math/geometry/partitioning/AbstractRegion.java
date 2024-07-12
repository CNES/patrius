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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;

import fr.cnes.sirius.patrius.math.exception.MathInternalError;
import fr.cnes.sirius.patrius.math.geometry.Space;
import fr.cnes.sirius.patrius.math.geometry.Vector;

//CHECKSTYLE: stop IllegalType check
//Reason: Commons-Math code kept as such

/**
 * Abstract class for all regions, independently of geometry type or dimension.
 * 
 * @param <S>
 *        Type of the space.
 * @param <T>
 *        Type of the sub-space.
 * 
 * @version $Id: AbstractRegion.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
@SuppressWarnings("PMD.NullAssignment")
public abstract class AbstractRegion<S extends Space, T extends Space> implements Region<S> {

    /** Unchecked class cast exception. */
    private static final String UNCHECKED = "unchecked";

    /** Inside/Outside BSP tree. */
    private BSPTree<S> tree;

    /** Size of the instance. */
    private double size;

    /** Barycenter. */
    @SuppressWarnings("PMD.LooseCoupling")
    private Vector<S> barycenter;

    /**
     * Build a region representing the whole space.
     */
    protected AbstractRegion() {
        this.tree = new BSPTree<>(Boolean.TRUE);
    }

    /**
     * Build a region from an inside/outside BSP tree.
     * <p>
     * The leaf nodes of the BSP tree <em>must</em> have a {@code Boolean} attribute representing the inside status of
     * the corresponding cell (true for inside cells, false for outside cells). In order to avoid building too many
     * small objects, it is recommended to use the predefined constants {@code Boolean.TRUE} and {@code Boolean.FALSE}.
     * The tree also <em>must</em> have either null internal nodes or internal nodes representing the boundary as
     * specified in the {@link #getTree getTree} method).
     * </p>
     * 
     * @param treeIn
     *        inside/outside BSP tree representing the region
     */
    protected AbstractRegion(final BSPTree<S> treeIn) {
        this.tree = treeIn;
    }

    /**
     * Build a Region from a Boundary REPresentation (B-rep).
     * <p>
     * The boundary is provided as a collection of {@link SubHyperplane sub-hyperplanes}. Each sub-hyperplane has the
     * interior part of the region on its minus side and the exterior on its plus side.
     * </p>
     * <p>
     * The boundary elements can be in any order, and can form several non-connected sets (like for example polygons
     * with holes or a set of disjoints polyhedrons considered as a whole). In fact, the elements do not even need to be
     * connected together (their topological connections are not used here). However, if the boundary does not really
     * separate an inside open from an outside open (open having here its topological meaning), then subsequent calls to
     * the {@link #checkPoint(Vector) checkPoint} method will not be meaningful anymore.
     * </p>
     * <p>
     * If the boundary is empty, the region will represent the whole space.
     * </p>
     * 
     * @param boundary
     *        collection of boundary elements, as a
     *        collection of {@link SubHyperplane SubHyperplane} objects
     */
    protected AbstractRegion(final Collection<SubHyperplane<S>> boundary) {

        if (boundary.isEmpty()) {
            // the tree represents the whole space
            this.tree = new BSPTree<>(Boolean.TRUE);
        } else {

            // sort the boundary elements in decreasing size order
            // (we don't want equal size elements to be removed, so
            // we use a trick to fool the TreeSet)
            final TreeSet<SubHyperplane<S>> ordered = new TreeSet<>(new Comparator<SubHyperplane<S>>(){
                /** {@inheritDoc} */
                @Override
                public int compare(final SubHyperplane<S> o1, final SubHyperplane<S> o2) {
                    final double size1 = o1.getSize();
                    final double size2 = o2.getSize();
                    return (size2 < size1) ? -1 : ((o1.equals(o2)) ? 0 : +1);
                }
            });
            ordered.addAll(boundary);

            // build the tree top-down
            this.tree = new BSPTree<>();
            this.insertCuts(this.tree, ordered);

            // set up the inside/outside flags
            this.tree.visit(new BSPTreeVisitor<S>(){

                /** {@inheritDoc} */
                @Override
                public Order visitOrder(final BSPTree<S> node) {
                    return Order.PLUS_SUB_MINUS;
                }

                /** {@inheritDoc} */
                @Override
                public void visitInternalNode(final BSPTree<S> node) {
                    // Nothing to do
                }

                /** {@inheritDoc} */
                @Override
                public void visitLeafNode(final BSPTree<S> node) {
                    node.setAttribute((node == node.getParent().getPlus()) ?
                        Boolean.FALSE : Boolean.TRUE);
                }
            });
        }
    }

    /**
     * Build a convex region from an array of bounding hyperplanes.
     * 
     * @param hyperplanes
     *        array of bounding hyperplanes (if null, an
     *        empty region will be built)
     */
    public AbstractRegion(final Hyperplane<S>[] hyperplanes) {
        if ((hyperplanes == null) || (hyperplanes.length == 0)) {
            this.tree = new BSPTree<>(Boolean.FALSE);
        } else {

            // use the first hyperplane to build the right class
            this.tree = hyperplanes[0].wholeSpace().getTree(false);

            // chop off parts of the space
            BSPTree<S> node = this.tree;
            node.setAttribute(Boolean.TRUE);
            for (final Hyperplane<S> hyperplane : hyperplanes) {
                if (node.insertCut(hyperplane)) {
                    node.setAttribute(null);
                    node.getPlus().setAttribute(Boolean.FALSE);
                    node = node.getMinus();
                    node.setAttribute(Boolean.TRUE);
                }
            }
        }
    }

    /** {@inheritDoc} */
    // CHECKSTYLE: stop IllegalType check
    // Reason: Commons-Math code kept as such
    @Override
    public abstract AbstractRegion<S, T> buildNew(BSPTree<S> newTree);

    // CHECKSTYLE: resume IllegalType check

    /**
     * Recursively build a tree by inserting cut sub-hyperplanes.
     * 
     * @param node
     *        current tree node (it is a leaf node at the beginning
     *        of the call)
     * @param boundary
     *        collection of edges belonging to the cell defined
     *        by the node
     */
    private void insertCuts(final BSPTree<S> node, final Collection<SubHyperplane<S>> boundary) {

        final Iterator<SubHyperplane<S>> iterator = boundary.iterator();

        // build the current level
        Hyperplane<S> inserted = null;
        while ((inserted == null) && iterator.hasNext()) {
            inserted = iterator.next().getHyperplane();
            if (!node.insertCut(inserted.copySelf())) {
                inserted = null;
            }
        }

        // last element
        if (!iterator.hasNext()) {
            return;
        }

        // distribute the remaining edges in the two sub-trees
        final ArrayList<SubHyperplane<S>> plusList = new ArrayList<>();
        final ArrayList<SubHyperplane<S>> minusList = new ArrayList<>();
        // loop on all edges
        while (iterator.hasNext()) {
            final SubHyperplane<S> other = iterator.next();
            switch (other.side(inserted)) {
                case PLUS:
                    plusList.add(other);
                    break;
                case MINUS:
                    minusList.add(other);
                    break;
                case BOTH:
                    final SubHyperplane.SplitSubHyperplane<S> split = other.split(inserted);
                    plusList.add(split.getPlus());
                    minusList.add(split.getMinus());
                    break;
                default:
                    // ignore the sub-hyperplanes belonging to the cut hyperplane
            }
        }

        // recurse through lower levels
        this.insertCuts(node.getPlus(), plusList);
        this.insertCuts(node.getMinus(), minusList);
    }

    /** {@inheritDoc} */
    // CHECKSTYLE: stop IllegalType check
    // Reason: Commons-Math code kept as such
    @Override
    public AbstractRegion<S, T> copySelf() {
        return this.buildNew(this.tree.copySelf());
    }

    // CHECKSTYLE: resume IllegalType check

    /** {@inheritDoc} */
    @Override
    public boolean isEmpty() {
        return this.isEmpty(this.tree);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isEmpty(final BSPTree<S> node) {

        // we use a recursive function rather than the BSPTreeVisitor
        // interface because we can stop visiting the tree as soon as we
        // have found an inside cell

        if (node.getCut() == null) {
            // if we find an inside node, the region is not empty
            return !((Boolean) node.getAttribute());
        }

        // check both sides of the sub-tree
        return this.isEmpty(node.getMinus()) && this.isEmpty(node.getPlus());

    }

    /** {@inheritDoc} */
    @Override
    public boolean contains(final Region<S> region) {
        return new RegionFactory<S>().difference(region, this).isEmpty();
    }

    /** {@inheritDoc} */
    @Override
    public Location checkPoint(final Vector<S> point) {
        return this.checkPoint(this.tree, point);
    }

    /**
     * Check a point with respect to the region starting at a given node.
     * 
     * @param node
     *        root node of the region
     * @param point
     *        point to check
     * @return a code representing the point status: either {@link Region.Location#INSIDE INSIDE},
     *         {@link Region.Location#OUTSIDE
     *         OUTSIDE} or {@link Region.Location#BOUNDARY BOUNDARY}
     */
    @SuppressWarnings("PMD.LooseCoupling")
    protected Location checkPoint(final BSPTree<S> node, final Vector<S> point) {
        final BSPTree<S> cell = node.getCell(point);
        if (cell.getCut() == null) {
            // the point is in the interior of a cell, just check the attribute
            return ((Boolean) cell.getAttribute()) ? Location.INSIDE : Location.OUTSIDE;
        }

        // the point is on a cut-sub-hyperplane, is it on a boundary ?
        final Location minusCode = this.checkPoint(cell.getMinus(), point);
        final Location plusCode = this.checkPoint(cell.getPlus(), point);
        return (minusCode.equals(plusCode)) ? minusCode : Location.BOUNDARY;

    }

    /** {@inheritDoc} */
    @Override
    public BSPTree<S> getTree(final boolean includeBoundaryAttributes) {
        if (includeBoundaryAttributes && (this.tree.getCut() != null) && (this.tree.getAttribute() == null)) {
            // we need to compute the boundary attributes
            this.tree.visit(new BoundaryBuilder<S>());
        }
        return this.tree;
    }

    /** {@inheritDoc} */
    @Override
    public double getBoundarySize() {
        final BoundarySizeVisitor<S> visitor = new BoundarySizeVisitor<>();
        this.getTree(true).visit(visitor);
        return visitor.getSize();
    }

    /** {@inheritDoc} */
    @Override
    public double getSize() {
        if (this.barycenter == null) {
            this.computeGeometricalProperties();
        }
        return this.size;
    }

    /**
     * Set the size of the instance.
     * 
     * @param sizeIn
     *        size of the instance
     */
    protected void setSize(final double sizeIn) {
        this.size = sizeIn;
    }

    /** {@inheritDoc} */
    @Override
    public Vector<S> getBarycenter() {
        if (this.barycenter == null) {
            this.computeGeometricalProperties();
        }
        return this.barycenter;
    }

    /**
     * Set the barycenter of the instance.
     * 
     * @param barycenterIn
     *        barycenter of the instance
     */
    @SuppressWarnings("PMD.LooseCoupling")
    protected void setBarycenter(final Vector<S> barycenterIn) {
        this.barycenter = barycenterIn;
    }

    /**
     * Compute some geometrical properties.
     * <p>
     * The properties to compute are the barycenter and the size.
     * </p>
     */
    protected abstract void computeGeometricalProperties();

    /** {@inheritDoc} */
    @Override
    public Side side(final Hyperplane<S> hyperplane) {
        final Sides sides = new Sides();
        this.recurseSides(this.tree, hyperplane.wholeHyperplane(), sides);
        return sides.plusFound() ?
            (sides.minusFound() ? Side.BOTH : Side.PLUS) :
            (sides.minusFound() ? Side.MINUS : Side.HYPER);
    }

    /**
     * Search recursively for inside leaf nodes on each side of the given hyperplane.
     * 
     * <p>
     * The algorithm used here is directly derived from the one described in section III (<i>Binary Partitioning of a
     * BSP Tree</i>) of the Bruce Naylor, John Amanatides and William Thibault paper <a
     * href="http://www.cs.yorku.ca/~amana/research/bsptSetOp.pdf">Merging BSP Trees Yields Polyhedral Set
     * Operations</a> Proc. Siggraph '90, Computer Graphics 24(4), August 1990, pp 115-124, published by the Association
     * for Computing Machinery (ACM)..
     * </p>
     * 
     * @param node
     *        current BSP tree node
     * @param sub
     *        sub-hyperplane
     * @param sides
     *        object holding the sides found
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    private void recurseSides(final BSPTree<S> node, final SubHyperplane<S> sub, final Sides sides) {
        // CHECKSTYLE: resume CyclomaticComplexity check

        if (node.getCut() == null) {
            if ((Boolean) node.getAttribute()) {
                // this is an inside cell expanding across the hyperplane
                sides.rememberPlusFound();
                sides.rememberMinusFound();
            }
            return;
        }

        // get the hyperplane
        final Hyperplane<S> hyperplane = node.getCut().getHyperplane();
        switch (sub.side(hyperplane)) {
            case PLUS:
                // the sub-hyperplane is entirely in the plus sub-tree
                if (node.getCut().side(sub.getHyperplane()) == Side.PLUS) {
                    if (!this.isEmpty(node.getMinus())) {
                        sides.rememberPlusFound();
                    }
                } else {
                    if (!this.isEmpty(node.getMinus())) {
                        sides.rememberMinusFound();
                    }
                }
                if (!(sides.plusFound() && sides.minusFound())) {
                    this.recurseSides(node.getPlus(), sub, sides);
                }
                break;
            case MINUS:
                // the sub-hyperplane is entirely in the minus sub-tree
                if (node.getCut().side(sub.getHyperplane()) == Side.PLUS) {
                    if (!this.isEmpty(node.getPlus())) {
                        sides.rememberPlusFound();
                    }
                } else {
                    if (!this.isEmpty(node.getPlus())) {
                        sides.rememberMinusFound();
                    }
                }
                if (!(sides.plusFound() && sides.minusFound())) {
                    // explore the sub-tree
                    this.recurseSides(node.getMinus(), sub, sides);
                }
                break;
            case BOTH:
                // the sub-hyperplane extends in both sub-trees
                final SubHyperplane.SplitSubHyperplane<S> split = sub.split(hyperplane);

                // explore first the plus sub-tree
                this.recurseSides(node.getPlus(), split.getPlus(), sides);

                // if needed, explore the minus sub-tree
                if (!(sides.plusFound() && sides.minusFound())) {
                    this.recurseSides(node.getMinus(), split.getMinus(), sides);
                }
                break;
            default:
                // the sub-hyperplane and the cut sub-hyperplane share the same hyperplane
                if (node.getCut().getHyperplane().sameOrientationAs(sub.getHyperplane())) {
                    if ((node.getPlus().getCut() != null) || ((Boolean) node.getPlus().getAttribute())) {
                        sides.rememberPlusFound();
                    }
                    if ((node.getMinus().getCut() != null) || ((Boolean) node.getMinus().getAttribute())) {
                        sides.rememberMinusFound();
                    }
                } else {
                    if ((node.getPlus().getCut() != null) || ((Boolean) node.getPlus().getAttribute())) {
                        sides.rememberMinusFound();
                    }
                    if ((node.getMinus().getCut() != null) || ((Boolean) node.getMinus().getAttribute())) {
                        sides.rememberPlusFound();
                    }
                }
                break;
        }

    }

    /** {@inheritDoc} */
    @Override
    public SubHyperplane<S> intersection(final SubHyperplane<S> sub) {
        return this.recurseIntersection(this.tree, sub);
    }

    /**
     * Recursively compute the parts of a sub-hyperplane that are
     * contained in the region.
     * 
     * @param node
     *        current BSP tree node
     * @param sub
     *        sub-hyperplane traversing the region
     * @return filtered sub-hyperplane
     */
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    private SubHyperplane<S> recurseIntersection(final BSPTree<S> node, final SubHyperplane<S> sub) {
        // CHECKSTYLE: resume ReturnCount check

        // Check the cut sub-hyperplane
        if (node.getCut() == null) {
            return (Boolean) node.getAttribute() ? sub.copySelf() : null;
        }

        // get hyperplane
        final Hyperplane<S> hyperplane = node.getCut().getHyperplane();
        switch (sub.side(hyperplane)) {
            case PLUS:
                return this.recurseIntersection(node.getPlus(), sub);
            case MINUS:
                return this.recurseIntersection(node.getMinus(), sub);
            case BOTH:
                final SubHyperplane.SplitSubHyperplane<S> split = sub.split(hyperplane);
                final SubHyperplane<S> plus = this.recurseIntersection(node.getPlus(), split.getPlus());
                final SubHyperplane<S> minus = this.recurseIntersection(node.getMinus(), split.getMinus());
                if (plus == null) {
                    // return the minus sub-hyperplane
                    return minus;
                } else if (minus == null) {
                    // return the plus sub-hyperplane
                    return plus;
                } else {
                    // return the union between the plus and minus sub-hyperplanes
                    return plus.reunite(minus);
                }
            default:
                return this.recurseIntersection(node.getPlus(),
                    this.recurseIntersection(node.getMinus(), sub));
        }

    }

    /**
     * Transform a region.
     * <p>
     * Applying a transform to a region consist in applying the transform to all the hyperplanes of the underlying BSP
     * tree and of the boundary (and also to the sub-hyperplanes embedded in these hyperplanes) and to the barycenter.
     * The instance is not modified, a new instance is built.
     * </p>
     * 
     * @param transform
     *        transform to apply
     * @return a new region, resulting from the application of the
     *         transform to the instance
     */
    // CHECKSTYLE: stop IllegalType check
    // Reason: Commons-Math code kept as such
    public AbstractRegion<S, T> applyTransform(final Transform<S, T> transform) {
        return this.buildNew(this.recurseTransform(this.getTree(false), transform));
    }

    // CHECKSTYLE: resume IllegalType check

    /**
     * Recursively transform an inside/outside BSP-tree.
     * 
     * @param node
     *        current BSP tree node
     * @param transform
     *        transform to apply
     * @return a new tree
     */
    @SuppressWarnings(UNCHECKED)
    private BSPTree<S> recurseTransform(final BSPTree<S> node, final Transform<S, T> transform) {

        if (node.getCut() == null) {
            return new BSPTree<>(node.getAttribute());
        }

        final SubHyperplane<S> sub = node.getCut();
        final SubHyperplane<S> tSub = ((AbstractSubHyperplane<S, T>) sub).applyTransform(transform);
        BoundaryAttribute<S> attribute = (BoundaryAttribute<S>) node.getAttribute();
        if (attribute != null) {
            final SubHyperplane<S> tPO = (attribute.getPlusOutside() == null) ?
                null : ((AbstractSubHyperplane<S, T>) attribute.getPlusOutside()).applyTransform(transform);
            final SubHyperplane<S> tPI = (attribute.getPlusInside() == null) ?
                null : ((AbstractSubHyperplane<S, T>) attribute.getPlusInside()).applyTransform(transform);
            attribute = new BoundaryAttribute<>(tPO, tPI);
        }

        return new BSPTree<>(tSub,
            this.recurseTransform(node.getPlus(), transform),
            this.recurseTransform(node.getMinus(), transform),
            attribute);
    }


    /**
     * Visitor building boundary shell tree.
     * <p>
     * The boundary shell is represented as {@link BoundaryAttribute boundary attributes} at each internal node.
     * </p>
     * 
     * @param <S> space
     */
    private static class BoundaryBuilder<S extends Space> implements BSPTreeVisitor<S> {

        /** {@inheritDoc} */
        @Override
        public Order visitOrder(final BSPTree<S> node) {
            return Order.PLUS_MINUS_SUB;
        }

        /** {@inheritDoc} */
        @Override
        public void visitInternalNode(final BSPTree<S> node) {

            SubHyperplane<S> plusOutside = null;
            SubHyperplane<S> plusInside = null;

            // characterize the cut sub-hyperplane,
            // first with respect to the plus sub-tree
            @SuppressWarnings(UNCHECKED)
            final SubHyperplane<S>[] plusChar = (SubHyperplane<S>[]) Array.newInstance(SubHyperplane.class, 2);
            this.characterize(node.getPlus(), node.getCut().copySelf(), plusChar);

            if (plusChar[0] != null && !plusChar[0].isEmpty()) {
                // plusChar[0] corresponds to a subset of the cut sub-hyperplane known to have
                // outside cells on its plus side, we want to check if parts of this subset
                // do have inside cells on their minus side
                @SuppressWarnings(UNCHECKED)
                final SubHyperplane<S>[] minusChar = (SubHyperplane<S>[]) Array.newInstance(SubHyperplane.class, 2);
                this.characterize(node.getMinus(), plusChar[0], minusChar);
                if (minusChar[1] != null && !minusChar[1].isEmpty()) {
                    // this part belongs to the boundary,
                    // it has the outside on its plus side and the inside on its minus side
                    plusOutside = minusChar[1];
                }
            }

            if (plusChar[1] != null && !plusChar[1].isEmpty()) {
                // plusChar[1] corresponds to a subset of the cut sub-hyperplane known to have
                // inside cells on its plus side, we want to check if parts of this subset
                // do have outside cells on their minus side
                @SuppressWarnings(UNCHECKED)
                final SubHyperplane<S>[] minusChar = (SubHyperplane<S>[]) Array.newInstance(SubHyperplane.class, 2);
                this.characterize(node.getMinus(), plusChar[1], minusChar);
                if (minusChar[0] != null && !minusChar[0].isEmpty()) {
                    // this part belongs to the boundary,
                    // it has the inside on its plus side and the outside on its minus side
                    plusInside = minusChar[0];
                }
            }

            // set the boundary attribute at non-leaf nodes
            node.setAttribute(new BoundaryAttribute<>(plusOutside, plusInside));
        }

        /** {@inheritDoc} */
        @Override
        public void visitLeafNode(final BSPTree<S> node) {
            // Nothing to do
        }

        /**
         * Filter the parts of an hyperplane belonging to the boundary.
         * <p>
         * The filtering consist in splitting the specified sub-hyperplane into several parts lying in inside and
         * outside cells of the tree. The principle is to call this method twice for each cut sub-hyperplane in the
         * tree, once one the plus node and once on the minus node. The parts that have the same flag (inside/inside or
         * outside/outside) do not belong to the boundary while parts that have different flags (inside/outside or
         * outside/inside) do belong to the boundary.
         * </p>
         * 
         * @param node
         *        current BSP tree node
         * @param sub
         *        sub-hyperplane to characterize
         * @param characterization
         *        placeholder where to put the characterized parts
         */
        private void characterize(final BSPTree<S> node, final SubHyperplane<S> sub,
                                  final SubHyperplane<S>[] characterization) {
            if (node.getCut() == null) {
                // we have reached a leaf node
                final boolean inside = (Boolean) node.getAttribute();
                if (inside) {
                    // add sub-hyperplane to inside characterization
                    if (characterization[1] == null) {
                        characterization[1] = sub;
                    } else {
                        characterization[1] = characterization[1].reunite(sub);
                    }
                } else {
                    // add sub-hyperplane to outside characterization
                    if (characterization[0] == null) {
                        characterization[0] = sub;
                    } else {
                        characterization[0] = characterization[0].reunite(sub);
                    }
                }
            } else {
                // Tree node
                //
                final Hyperplane<S> hyperplane = node.getCut().getHyperplane();
                // characterize the plus and/or minus sides of the tree node depending on the
                // relative position of the input sub-hyperplane with respect to the cut sub-hyperplane
                // of the tree node
                switch (sub.side(hyperplane)) {
                    case PLUS:
                        this.characterize(node.getPlus(), sub, characterization);
                        break;
                    case MINUS:
                        this.characterize(node.getMinus(), sub, characterization);
                        break;
                    case BOTH:
                        final SubHyperplane.SplitSubHyperplane<S> split = sub.split(hyperplane);
                        this.characterize(node.getPlus(), split.getPlus(), characterization);
                        this.characterize(node.getMinus(), split.getMinus(), characterization);
                        break;
                    default:
                        // this should not happen
                        throw new MathInternalError();
                }
            }
        }
    }


    /** Utility class holding the already found sides. */
    private static final class Sides {

        /** Indicator of inside leaf nodes found on the plus side. */
        private boolean plusFoundFlag;

        /** Indicator of inside leaf nodes found on the plus side. */
        private boolean minusFoundFlag;

        /**
         * Simple constructor.
         */
        public Sides() {
            this.plusFoundFlag = false;
            this.minusFoundFlag = false;
        }

        /**
         * Remember the fact that inside leaf nodes have been found on the plus side.
         */
        public void rememberPlusFound() {
            this.plusFoundFlag = true;
        }

        /**
         * Check if inside leaf nodes have been found on the plus side.
         * 
         * @return true if inside leaf nodes have been found on the plus side
         */
        public boolean plusFound() {
            return this.plusFoundFlag;
        }

        /**
         * Remember the fact that inside leaf nodes have been found on the minus side.
         */
        public void rememberMinusFound() {
            this.minusFoundFlag = true;
        }

        /**
         * Check if inside leaf nodes have been found on the minus side.
         * 
         * @return true if inside leaf nodes have been found on the minus side
         */
        public boolean minusFound() {
            return this.minusFoundFlag;
        }
    }
    // CHECKSTYLE: resume IllegalType check
}
