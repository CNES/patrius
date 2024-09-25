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

import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.MathRuntimeException;
import fr.cnes.sirius.patrius.math.geometry.Vector;
import fr.cnes.sirius.patrius.math.geometry.euclidean.oned.Euclidean1D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.twod.Euclidean2D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.twod.PolygonsSet;
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
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

//CHECKSTYLE: stop IllegalType check
//Reason: Commons-Math code kept as such

/**
 * This class represents a 3D region: a set of polyhedrons.
 * 
 * @version $Id: PolyhedronsSet.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
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
    
    /** Build a polyhedrons set from a Boundary REPresentation (B-rep) specified by connected vertices.
     * <p>
     * The boundary is provided as a list of vertices and a list of facets.
     * Each facet is specified as an integer array containing the arrays vertices
     * indices in the vertices list. Each facet normal is oriented by right hand
     * rule to the facet vertices list.
     * </p>
     * <p>
     * Some basic sanity checks are performed but not everything is thoroughly
     * assessed, so it remains under caller responsibility to ensure the vertices
     * and facets are consistent and properly define a polyhedrons set.
     * </p>
     * @param vertices list of polyhedrons set vertices
     * @param facets list of facets, as vertices indices in the vertices list
     * @param tolerance tolerance below which points are considered identical
     * @exception MathIllegalArgumentException if some basic sanity checks fail
     */
    public PolyhedronsSet(final List<Vector3D> vertices, final List<int[]> facets, final double tolerance) {
        super(buildBoundary(vertices, facets, tolerance));
    }
    
    /** Build a polyhedrons set from a Boundary REPresentation (B-rep) specified by connected vertices.
     * <p>
     * Some basic sanity checks are performed but not everything is thoroughly
     * assessed, so it remains under caller responsibility to ensure the vertices
     * and facets are consistent and properly define a polyhedrons set.
     * </p>
     * @param brep Boundary REPresentation of the polyhedron to build
     * @param tolerance tolerance below which points are considered identical
     * @exception MathIllegalArgumentException if some basic sanity checks fail
     * @since 1.2
     */
    public PolyhedronsSet(final BRep brep, final double tolerance) {
        super(buildBoundary(brep.getVertices(), brep.getFacets(), tolerance));
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
    
    /** Build boundary from vertices and facets.
     * @param vertices list of polyhedrons set vertices
     * @param facets list of facets, as vertices indices in the vertices list
     * @param tolerance tolerance below which points are considered identical
     * @return boundary as a list of sub-hyperplanes
     * @exception MathIllegalArgumentException if some basic sanity checks fail
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    private static List<SubHyperplane<Euclidean3D>> buildBoundary(final List<Vector3D> vertices,
                                                                  final List<int[]> facets,
                                                                  final double tolerance) {
        // CHECKSTYLE: resume CyclomaticComplexity check
        // check vertices distances
        for (int i = 0; i < vertices.size() - 1; ++i) {
            final Vector3D vi = vertices.get(i);
            for (int j = i + 1; j < vertices.size(); ++j) {
                if (Vector3D.distance(vi, vertices.get(j)) <= tolerance) {
                    throw new MathIllegalArgumentException(PatriusMessages.CLOSE_VERTICES,
                                                           vi.getX(), vi.getY(), vi.getZ());
                }
            }
        }

        // find how vertices are referenced by facets
        final int[][] references = findReferences(vertices, facets);

        // find how vertices are linked together by edges along the facets they belong to
        final int[][] successors = successors(vertices, facets, references);

        // check edges orientations
        for (int vA = 0; vA < vertices.size(); ++vA) {
            for (final int vB : successors[vA]) {

                if (vB >= 0) {
                    // when facets are properly oriented, if vB is the successor of vA on facet f1,
                    // then there must be an adjacent facet f2 where vA is the successor of vB
                    boolean found = false;
                    for (final int v : successors[vB]) {
                        found = found || (v == vA);
                    }
                    if (!found) {
                        final Vector3D start = vertices.get(vA);
                        final Vector3D end = vertices.get(vB);
                        throw new MathIllegalArgumentException(PatriusMessages.EDGE_CONNECTED_TO_ONE_FACET,
                            start.getX(), start.getY(), start.getZ(),
                            end.getX(), end.getY(), end.getZ());
                    }
                }
            }
        }

        final List<SubHyperplane<Euclidean3D>> boundary = new ArrayList<>();
        
        for (final int[] facet : facets) {

            // define facet plane from the first 3 points
            final Plane plane = new Plane(vertices.get(facet[0]), vertices.get(facet[1]), vertices.get(facet[2]));
           
            // check all points are in the plane
            final Vector2D[] two2Points = new Vector2D[facet.length];
            for (int i = 0; i < facet.length; ++i) {
                final Vector3D v = vertices.get(facet[i]);
                if (!plane.contains(v)) {
                    throw new MathIllegalArgumentException(PatriusMessages.OUT_OF_PLANE,
                        v.getX(), v.getY(), v.getZ());
                }
                two2Points[i] = plane.toSubSpace(v);
            }
            
            // create the polygonal facet
            boundary.add(new SubPlane(plane, new PolygonsSet(tolerance, two2Points)));

        }

        return boundary;

    }

    /**
     * Find the facets that reference each edges.
     * 
     * @param vertices list of polyhedrons set vertices
     * @param facets list of facets, as vertices indices in the vertices list
     * @return references array such that r[v][k] = f for some k if facet f contains vertex v
     * @exception MathIllegalArgumentException if some facets have fewer than 3 vertices
     */
    // CHECKSTYLE: stop CommentRatio check
    // Reason: Commons-Math code kept as such
    private static int[][] findReferences(final List<Vector3D> vertices, final List<int[]> facets) {
        // CHECKSTYLE: resume CommentRatio check
        // find the maximum number of facets a vertex belongs to
        final int[] nbFacets = new int[vertices.size()];
        int maxFacets = 0;
        for (final int[] facet : facets) {
            if (facet.length < 3) {
                throw new MathIllegalArgumentException(PatriusMessages.WRONG_NUMBER_OF_POINTS,
                    3, facet.length);
            }
            for (final int index : facet) {
                maxFacets = MathLib.max(maxFacets, ++nbFacets[index]);
            }
        }
        
        // set up the references array
        final int[][] references = new int[vertices.size()][maxFacets];
        for (final int[] r : references) {
            Arrays.fill(r, -1);
        }
        for (int f = 0; f < facets.size(); ++f) {
            for (final int v : facets.get(f)) {
                // vertex v is referenced by facet f
                int k = 0;
                while (k < maxFacets && references[v][k] >= 0) {
                    ++k;
                }
                references[v][k] = f;
            }
        }

        return references;

    }

    /**
     * Find the successors of all vertices among all facets they belong to.
     * 
     * @param vertices list of polyhedrons set vertices
     * @param facets list of facets, as vertices indices in the vertices list
     * @param references facets references array
     * @return indices of vertices that follow vertex v in some facet (the array
     *         may contain extra entries at the end, set to negative indices)
     * @exception MathIllegalArgumentException if the same vertex appears more than
     *            once in the successors list (which means one facet orientation is wrong)
     */
    // CHECKSTYLE: stop CommentRatio check
    // Reason: Commons-Math code kept as such
    private static int[][] successors(final List<Vector3D> vertices, final List<int[]> facets,
                                      final int[][] references) {
        // CHECKSTYLE: resume CommentRatio check

        // create an array large enough
        final int[][] successors = new int[vertices.size()][references[0].length];
        for (final int[] s : successors) {
            Arrays.fill(s, -1);
        }

        for (int v = 0; v < vertices.size(); ++v) {
            for (int k = 0; k < successors[v].length && references[v][k] >= 0; ++k) {

                // look for vertex v
                final int[] facet = facets.get(references[v][k]);
                int i = 0;
                while (i < facet.length && facet[i] != v) {
                    ++i;
                }

                // we have found vertex v, we deduce its successor on current facet
                successors[v][k] = facet[(i + 1) % facet.length];
                for (int l = 0; l < k; ++l) {
                    if (successors[v][l] == successors[v][k]) {
                        final Vector3D start = vertices.get(v);
                        final Vector3D end = vertices.get(successors[v][k]);
                        throw new MathIllegalArgumentException(PatriusMessages.FACET_ORIENTATION_MISMATCH,
                            start.getX(), start.getY(), start.getZ(),
                            end.getX(), end.getY(), end.getZ());
                    }
                }

            }
        }

        return successors;

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
            final SubHyperplane<Euclidean3D> facet = boundaryFacet(point, node);
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
                final SubHyperplane<Euclidean3D> facet = boundaryFacet(hit3D, node);
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
    private static SubHyperplane<Euclidean3D> boundaryFacet(final Vector3D point,
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
    

    /** Get the boundary representation of the instance.
     * <p>
     * The boundary representation can be extracted <em>only</em> from
     * bounded polyhedrons sets. If the polyhedrons set is unbounded,
     * a {@link MathRuntimeException} will be thrown.
     * </p>
     * <p>
     * The boundary representation extracted is not minimal, as for
     * example canonical facets may be split into several smaller
     * independent sub-facets sharing the same plane and connected by
     * their edges.
     * </p>
     * <p>
     * As the {@link BRep B-Rep} representation does not support
     * facets with several boundary loops (for example facets with
     * holes), an exception is triggered when attempting to extract
     * B-Rep from such complex polyhedrons sets.
     * </p>
     * @return boundary representation of the instance
     * @exception MathRuntimeException if polyhedrons is unbounded
     * @since 1.2
     */
    public BRep getBRep() throws MathRuntimeException {
        final BRepExtractor extractor = new BRepExtractor(1E-14);
        getTree(true).visit(extractor);
        return extractor.getBRep();
    }

    /** Visitor extracting BRep. */
    private static class BRepExtractor implements BSPTreeVisitor<Euclidean3D> {

        /** Tolerance for vertices identification. */
        private final double tolerance;

        /** Extracted vertices. */
        private final List<Vector3D> vertices;

        /** Extracted facets. */
        private final List<int[]> facets;

        /** Simple constructor.
         * @param toleranceIn tolerance for vertices identification
         */
        BRepExtractor(final double toleranceIn) {
            this.tolerance = toleranceIn;
            this.vertices  = new ArrayList<>();
            this.facets    = new ArrayList<>();
        }

        /** Get the BRep.
         * @return extracted BRep
         */
        public BRep getBRep() {
            return new BRep(this.vertices, this.facets);
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
                addContribution(attribute.getPlusOutside(), false);
            }
            if (attribute.getPlusInside() != null) {
                addContribution(attribute.getPlusInside(), true);
            }
        }

        /** {@inheritDoc} */
        @Override
        @SuppressWarnings("PMD.UncommentedEmptyMethodBody")
        public void visitLeafNode(final BSPTree<Euclidean3D> node) {
            // nothing to do
        }

        /** Add he contribution of a boundary facet.
         * @param facet boundary facet
         * @param reversed if true, the facet has the inside on its plus side
         * @exception MathRuntimeException if facet is unbounded
         */
        // CHECKSTYLE: stop CommentRatio check
        // Reason: Commons-Math code kept as such
        private void addContribution(final SubHyperplane<Euclidean3D> facet, final boolean reversed)
            throws MathRuntimeException {
            // CHECKSTYLE: resume CommentRatio check
            final Plane plane = (Plane) facet.getHyperplane();
            final PolygonsSet polygon = (PolygonsSet) ((SubPlane) facet).getRemainingRegion();
            final Vector2D[][] loops2D = polygon.getVertices();
            if (loops2D.length == 0) {
                throw new MathRuntimeException(PatriusMessages.OUTLINE_BOUNDARY_LOOP_OPEN);
            } else if (loops2D.length > 1) {
                throw new MathRuntimeException(PatriusMessages.FACET_WITH_SEVERAL_BOUNDARY_LOOPS);
            } else {
                for (final Vector2D[] loop2D : polygon.getVertices()) {
                    final int[] loop3D = new int[loop2D.length];
                    for (int i = 0; i < loop2D.length ; ++i) {
                        if (loop2D[i] == null) {
                            throw new MathRuntimeException(PatriusMessages.OUTLINE_BOUNDARY_LOOP_OPEN);
                        }
                        loop3D[reversed ? loop2D.length - 1 - i : i] = getVertexIndex(plane.toSpace(loop2D[i]));
                    }
                    this.facets.add(loop3D);
                }
            }

        }

        /** Get the index of a vertex.
         * @param vertex vertex as a 3D point
         * @return index of the vertex
         */
        private int getVertexIndex(final Vector3D vertex) {

            for (int i = 0; i < this.vertices.size(); ++i) {
                if (Vector3D.distance(vertex, this.vertices.get(i)) <= this.tolerance) {
                    // the vertex is already known
                    return i;
                }
            }

            // the vertex is a new one, add it
            this.vertices.add(vertex);
            return this.vertices.size() - 1;

        }

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
    
    /** Container for Boundary REPresentation (B-Rep).
     * <p>
     * The boundary is provided as a list of vertices and a list of facets.
     * Each facet is specified as an integer array containing the arrays vertices
     * indices in the vertices list. Each facet normal is oriented by right hand
     * rule to the facet vertices list.
     * </p>
     * @see PolyhedronsSet#getBRep()
     * @since 1.2
     */
    @SuppressWarnings("PMD.ShortClassName")
    public static class BRep {

        /** List of polyhedrons set vertices. */
        private final List<Vector3D> vertices;

        /** List of facets, as vertices indices in the vertices list. */
        private final List<int[]> facets;

        /** Simple constructor.
         * @param verticesIn list of polyhedrons set vertices
         * @param facetsIn list of facets, as vertices indices in the vertices list
         */
        public BRep(final List<Vector3D> verticesIn, final List<int[]> facetsIn) {
            this.vertices = verticesIn;
            this.facets = facetsIn;
        }

        /** Get the extracted vertices.
         * @return extracted vertices
         */
        public List<Vector3D> getVertices() {
            return this.vertices;
        }

        /** Get the extracted facets.
         * @return extracted facets
         */
        public List<int[]> getFacets() {
            return this.facets;
        }

    }
}
