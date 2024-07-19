/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
 * Copyright 2011-2022 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
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
package fr.cnes.sirius.patrius.math.geometry.euclidean.twod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.geometry.euclidean.oned.IntervalsSet;
import fr.cnes.sirius.patrius.math.geometry.partitioning.Region;
import fr.cnes.sirius.patrius.math.geometry.partitioning.RegionFactory;
import fr.cnes.sirius.patrius.math.geometry.partitioning.SubHyperplane;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class represent a tree of nested 2D boundary loops.
 * 
 * <p>
 * This class is used for piecewise polygons construction. Polygons are built using the outline edges as representative
 * of boundaries, the orientation of these lines are meaningful. However, we want to allow the user to specify its
 * outline loops without having to take care of this orientation. This class is devoted to correct mis-oriented loops.
 * <p>
 * 
 * <p>
 * Orientation is computed assuming the piecewise polygon is finite, i.e. the outermost loops have their exterior side
 * facing points at infinity, and hence are oriented counter-clockwise. The orientation of internal loops is computed as
 * the reverse of the orientation of their immediate surrounding loop.
 * </p>
 * 
 * @version $Id: NestedLoops.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
class NestedLoops {

    /** Boundary loop. */
    private Vector2D[] loop;

    /** Surrounded loops. */
    private final List<NestedLoops> surrounded;

    /** Polygon enclosing a finite region. */
    private Region<Euclidean2D> polygon;

    /** Indicator for original loop orientation. */
    private boolean originalIsClockwise;

    /**
     * Simple Constructor.
     * <p>
     * Build an empty tree of nested loops. This instance will become the root node of a complete tree, it is not
     * associated with any loop by itself, the outermost loops are in the root tree child nodes.
     * </p>
     */
    public NestedLoops() {
        this.surrounded = new ArrayList<>();
    }

    /**
     * Constructor.
     * <p>
     * Build a tree node with neither parent nor children
     * </p>
     * 
     * @param loopIn
     *        boundary loop (will be reversed in place if needed)
     * @exception MathIllegalArgumentException
     *            if an outline has an open boundary loop
     */
    private NestedLoops(final Vector2D[] loopIn) {

        if (loopIn[0] == null) {
            throw new MathIllegalArgumentException(PatriusMessages.OUTLINE_BOUNDARY_LOOP_OPEN);
        }

        this.loop = loopIn;
        this.surrounded = new ArrayList<>();

        // build the polygon defined by the loop
        final ArrayList<SubHyperplane<Euclidean2D>> edges = new ArrayList<>();
        Vector2D current = loopIn[loopIn.length - 1];
        for (final Vector2D element : loopIn) {
            final Vector2D previous = current;
            current = element;
            final Line line = new Line(previous, current);
            final IntervalsSet region =
                new IntervalsSet(line.toSubSpace(previous).getX(), line.toSubSpace(current).getX());
            edges.add(new SubLine(line, region));
        }
        this.polygon = new PolygonsSet(edges);

        // ensure the polygon encloses a finite region of the plane
        if (Double.isInfinite(this.polygon.getSize())) {
            this.polygon = new RegionFactory<Euclidean2D>().getComplement(this.polygon);
            this.originalIsClockwise = false;
        } else {
            this.originalIsClockwise = true;
        }
    }

    /**
     * Add a loop in a tree.
     * 
     * @param bLoop
     *        boundary loop (will be reversed in place if needed)
     * @exception MathIllegalArgumentException
     *            if an outline has crossing
     *            boundary loops or open boundary loops
     */
    public void add(final Vector2D[] bLoop) {
        this.add(new NestedLoops(bLoop));
    }

    /**
     * Add a loop in a tree.
     * 
     * @param node
     *        boundary loop (will be reversed in place if needed)
     * @exception MathIllegalArgumentException
     *            if an outline has boundary
     *            loops that cross each other
     */
    private void add(final NestedLoops node) {

        // check if we can go deeper in the tree
        for (final NestedLoops child : this.surrounded) {
            if (child.polygon.contains(node.polygon)) {
                child.add(node);
                return;
            }
        }

        // check if we can absorb some of the instance children
        for (final Iterator<NestedLoops> iterator = this.surrounded.iterator(); iterator.hasNext();) {
            final NestedLoops child = iterator.next();
            if (node.polygon.contains(child.polygon)) {
                node.surrounded.add(child);
                iterator.remove();
            }
        }

        // we should be separate from the remaining children
        final RegionFactory<Euclidean2D> factory = new RegionFactory<>();
        for (final NestedLoops child : this.surrounded) {
            if (!factory.intersection(node.polygon, child.polygon).isEmpty()) {
                // raise an exception if there is no intersection
                throw new MathIllegalArgumentException(PatriusMessages.CROSSING_BOUNDARY_LOOPS);
            }
        }

        this.surrounded.add(node);
    }

    /**
     * Correct the orientation of the loops contained in the tree.
     * <p>
     * This is this method that really inverts the loops that where provided through the {@link #add(Vector2D[]) add}
     * method if they are mis-oriented
     * </p>
     */
    public void correctOrientation() {
        for (final NestedLoops child : this.surrounded) {
            child.setClockWise(true);
        }
    }

    /**
     * Set the loop orientation.
     * 
     * @param clockwise
     *        if true, the loop should be set to clockwise
     *        orientation
     */
    @SuppressWarnings("PMD.AssignmentInOperand")
    private void setClockWise(final boolean clockwise) {

        if (this.originalIsClockwise ^ clockwise) {
            // we need to inverse the original loop
            int min = -1;
            int max = this.loop.length;
            while (++min < --max) {
                final Vector2D tmp = this.loop[min];
                this.loop[min] = this.loop[max];
                this.loop[max] = tmp;
            }
        }

        // go deeper in the tree
        for (final NestedLoops child : this.surrounded) {
            child.setClockWise(!clockwise);
        }
    }
}
