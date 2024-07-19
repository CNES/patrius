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
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:306:12/11/2014: coverage
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import org.junit.Assert;
import org.junit.Test;

import bsh.ParseException;
import fr.cnes.sirius.patrius.math.exception.MathArithmeticException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.MathRuntimeException;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.PolyhedronsSet.BRep;
import fr.cnes.sirius.patrius.math.geometry.euclidean.twod.Euclidean2D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.twod.PolygonsSet;
import fr.cnes.sirius.patrius.math.geometry.euclidean.twod.SubLine;
import fr.cnes.sirius.patrius.math.geometry.euclidean.twod.Vector2D;
import fr.cnes.sirius.patrius.math.geometry.partitioning.BSPTree;
import fr.cnes.sirius.patrius.math.geometry.partitioning.BSPTreeVisitor;
import fr.cnes.sirius.patrius.math.geometry.partitioning.BoundaryAttribute;
import fr.cnes.sirius.patrius.math.geometry.partitioning.Hyperplane;
import fr.cnes.sirius.patrius.math.geometry.partitioning.Region;
import fr.cnes.sirius.patrius.math.geometry.partitioning.RegionFactory;
import fr.cnes.sirius.patrius.math.geometry.partitioning.SubHyperplane;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;

public class PolyhedronsSetTest {

    @Test
    public void testBox() {
        final PolyhedronsSet tree = new PolyhedronsSet(0, 1, 0, 1, 0, 1);
        Assert.assertEquals(1.0, tree.getSize(), 1.0e-10);
        Assert.assertEquals(6.0, tree.getBoundarySize(), 1.0e-10);
        final Vector3D barycenter = (Vector3D) tree.getBarycenter();
        Assert.assertEquals(0.5, barycenter.getX(), 1.0e-10);
        Assert.assertEquals(0.5, barycenter.getY(), 1.0e-10);
        Assert.assertEquals(0.5, barycenter.getZ(), 1.0e-10);

        for (double x = -0.25; x < 1.25; x += 0.1) {
            final boolean xOK = (x >= 0.0) && (x <= 1.0);
            for (double y = -0.25; y < 1.25; y += 0.1) {
                final boolean yOK = (y >= 0.0) && (y <= 1.0);
                for (double z = -0.25; z < 1.25; z += 0.1) {
                    final boolean zOK = (z >= 0.0) && (z <= 1.0);
                    final Region.Location expected =
                        (xOK && yOK && zOK) ? Region.Location.INSIDE : Region.Location.OUTSIDE;
                    Assert.assertEquals(expected, tree.checkPoint(new Vector3D(x, y, z)));
                }
            }
        }

        checkPoints(Region.Location.BOUNDARY, tree, new Vector3D[] {
            new Vector3D(0.0, 0.5, 0.5),
            new Vector3D(1.0, 0.5, 0.5),
            new Vector3D(0.5, 0.0, 0.5),
            new Vector3D(0.5, 1.0, 0.5),
            new Vector3D(0.5, 0.5, 0.0),
            new Vector3D(0.5, 0.5, 1.0)
        });

        checkPoints(Region.Location.OUTSIDE, tree, new Vector3D[] {
            new Vector3D(0.0, 1.2, 1.2),
            new Vector3D(1.0, 1.2, 1.2),
            new Vector3D(1.2, 0.0, 1.2),
            new Vector3D(1.2, 1.0, 1.2),
            new Vector3D(1.2, 1.2, 0.0),
            new Vector3D(1.2, 1.2, 1.0)
        });

        // Intersection
        final Vector3D point = new Vector3D(0.5, 0.5, -2.);
        final Line line = new Line(point, point.add(Vector3D.PLUS_K));
        final SubHyperplane<Euclidean3D> subplane = tree.firstIntersection(point, line);
        final Hyperplane<Euclidean3D> plane = subplane.getHyperplane();

        // The intersection is on the XY plane
        Assert.assertEquals(0., plane.getOffset(Vector3D.ZERO), 0.);
        Assert.assertEquals(0., plane.getOffset(Vector3D.PLUS_I), 0.);
        Assert.assertEquals(0., plane.getOffset(Vector3D.PLUS_J), 0.);
    }
    
    @Test
    public void testBRepExtractor() {
        final double x = 1.0;
        final double y = 2.0;
        final double z = 3.0;
        final double w = 0.1;
        final double l = 1.0;

        final PolyhedronsSet polyhedron =
            new PolyhedronsSet(x - l, x + l, y - w, y + w, z - w, z + w);
        final PolyhedronsSet.BRep brep = polyhedron.getBRep();
        Assert.assertEquals(6, brep.getFacets().size());
        Assert.assertEquals(8, brep.getVertices().size());
    }

    @Test
    public void testEmptyBRepIfEmpty() {
        final PolyhedronsSet empty = (PolyhedronsSet) new RegionFactory<Euclidean3D>().getComplement(new PolyhedronsSet());
        Assert.assertTrue(empty.isEmpty());
        Assert.assertEquals(0.0, empty.getSize(), 1.0e-10);

        final PolyhedronsSet.BRep brep = empty.getBRep();
        Assert.assertEquals(0, brep.getFacets().size());
        Assert.assertEquals(0, brep.getVertices().size());
    }

    @Test
    public void testNoBRepHalfSpace() {
        final BSPTree<Euclidean3D> bsp = new BSPTree<>();
        bsp.insertCut(new Plane(Vector3D.PLUS_K));
        bsp.getPlus().setAttribute(Boolean.FALSE);
        bsp.getMinus().setAttribute(Boolean.TRUE);
        final PolyhedronsSet polyhedron = new PolyhedronsSet(bsp);
        Assert.assertEquals(Double.POSITIVE_INFINITY, polyhedron.getSize(), 1E-10);

        try {
            polyhedron.getBRep();
            Assert.fail("an exception should have been thrown");
        } catch (final MathRuntimeException mre) {
            Assert.assertEquals("an outline boundary loop is open", mre.getMessage());
        }
    }

    @Test
    public void testNoBRepHolesInFacet() {
        final PolyhedronsSet cube       = new PolyhedronsSet(-1.0, 1.0, -1.0, 1.0, -1.0, 1.0);
        final PolyhedronsSet tubeAlongX = new PolyhedronsSet(-2.0, 2.0, -0.5, 0.5, -0.5, 0.5);
        final PolyhedronsSet tubeAlongY = new PolyhedronsSet(-0.5, 0.5, -2.0, 2.0, -0.5, 0.5);
        final PolyhedronsSet tubeAlongZ = new PolyhedronsSet(-0.5, 0.5, -0.5, 0.5, -2.0, 2.0);
        final RegionFactory<Euclidean3D> factory = new RegionFactory<>();
        final PolyhedronsSet cubeWithHoles = (PolyhedronsSet) factory.difference(cube,
                                                                           factory.union(tubeAlongX,
                                                                                         factory.union(tubeAlongY, tubeAlongZ)));
        Assert.assertEquals(4.0, cubeWithHoles.getSize(), 1.0e-10);

        try {
            cubeWithHoles.getBRep();
            Assert.fail("an exception should have been thrown");
        } catch (final MathRuntimeException mre) {
            Assert.assertEquals("A facet has several boundary loops.", mre.getMessage());
        }
    }
    
    @Test
    public void testTetrahedron() throws MathArithmeticException {
        final Vector3D vertex1 = new Vector3D(1, 2, 3);
        final Vector3D vertex2 = new Vector3D(2, 2, 4);
        final Vector3D vertex3 = new Vector3D(2, 3, 3);
        final Vector3D vertex4 = new Vector3D(1, 3, 4);

        @SuppressWarnings("unchecked")
        final PolyhedronsSet tree =
            (PolyhedronsSet) new RegionFactory<Euclidean3D>().buildConvex(
                new Plane(vertex3, vertex2, vertex1),
                new Plane(vertex2, vertex3, vertex4),
                new Plane(vertex4, vertex3, vertex1),
                new Plane(vertex1, vertex2, vertex4));
        Assert.assertEquals(1.0 / 3.0, tree.getSize(), 1.0e-10);
        Assert.assertEquals(2.0 * MathLib.sqrt(3.0), tree.getBoundarySize(), 1.0e-10);

        final Vector3D barycenter = (Vector3D) tree.getBarycenter();
        Assert.assertEquals(1.5, barycenter.getX(), 1.0e-10);
        Assert.assertEquals(2.5, barycenter.getY(), 1.0e-10);
        Assert.assertEquals(3.5, barycenter.getZ(), 1.0e-10);

        final double third = 1.0 / 3.0;
        checkPoints(Region.Location.BOUNDARY, tree, new Vector3D[] {
            vertex1, vertex2, vertex3, vertex4,
            new Vector3D(third, vertex1, third, vertex2, third, vertex3),
            new Vector3D(third, vertex2, third, vertex3, third, vertex4),
            new Vector3D(third, vertex3, third, vertex4, third, vertex1),
            new Vector3D(third, vertex4, third, vertex1, third, vertex2)
        });

        checkPoints(Region.Location.OUTSIDE, tree, new Vector3D[] {
            new Vector3D(1, 2, 4),
            new Vector3D(2, 2, 3),
            new Vector3D(2, 3, 4),
            new Vector3D(1, 3, 3)
        });
    }

    @Test
    public void testIsometry() throws MathArithmeticException, MathIllegalArgumentException {
        final Vector3D vertex1 = new Vector3D(1.1, 2.2, 3.3);
        final Vector3D vertex2 = new Vector3D(2.0, 2.4, 4.2);
        final Vector3D vertex3 = new Vector3D(2.8, 3.3, 3.7);
        final Vector3D vertex4 = new Vector3D(1.0, 3.6, 4.5);

        @SuppressWarnings("unchecked")
        PolyhedronsSet tree =
            (PolyhedronsSet) new RegionFactory<Euclidean3D>().buildConvex(
                new Plane(vertex3, vertex2, vertex1),
                new Plane(vertex2, vertex3, vertex4),
                new Plane(vertex4, vertex3, vertex1),
                new Plane(vertex1, vertex2, vertex4));

        final Vector3D barycenter = (Vector3D) tree.getBarycenter();
        final Vector3D s = new Vector3D(10.2, 4.3, -6.7);
        final Vector3D c = new Vector3D(-0.2, 2.1, -3.2);
        final Rotation r = new Rotation(new Vector3D(6.2, -4.4, 2.1), 0.12);

        tree = tree.rotate(c, r).translate(s);

        final Vector3D newB =
            new Vector3D(1.0, s,
                1.0, c,
                1.0, r.applyTo(barycenter.subtract(c)));
        Assert.assertEquals(0.0,
            newB.subtract(tree.getBarycenter()).getNorm(),
            1.0e-10);

        final Vector3D[] expectedV = new Vector3D[] {
            new Vector3D(1.0, s,
                1.0, c,
                1.0, r.applyTo(vertex1.subtract(c))),
            new Vector3D(1.0, s,
                1.0, c,
                1.0, r.applyTo(vertex2.subtract(c))),
            new Vector3D(1.0, s,
                1.0, c,
                1.0, r.applyTo(vertex3.subtract(c))),
            new Vector3D(1.0, s,
                1.0, c,
                1.0, r.applyTo(vertex4.subtract(c)))
        };

        tree.getTree(true).visit(new BSPTreeVisitor<Euclidean3D>(){

            @Override
            public Order visitOrder(final BSPTree<Euclidean3D> node) {
                return Order.MINUS_SUB_PLUS;
            }

            @Override
            public void visitInternalNode(final BSPTree<Euclidean3D> node) {
                @SuppressWarnings("unchecked")
                final BoundaryAttribute<Euclidean3D> attribute =
                    (BoundaryAttribute<Euclidean3D>) node.getAttribute();
                if (attribute.getPlusOutside() != null) {
                    checkFacet((SubPlane) attribute.getPlusOutside());
                }
                if (attribute.getPlusInside() != null) {
                    checkFacet((SubPlane) attribute.getPlusInside());
                }
            }

            @Override
            public void visitLeafNode(final BSPTree<Euclidean3D> node) {
                // nothing to do
            }

            private void checkFacet(final SubPlane facet) {
                final Plane plane = (Plane) facet.getHyperplane();
                final Vector2D[][] vertices =
                    ((PolygonsSet) facet.getRemainingRegion()).getVertices();
                Assert.assertEquals(1, vertices.length);
                for (int i = 0; i < vertices[0].length; ++i) {
                    final Vector3D v = plane.toSpace(vertices[0][i]);
                    double d = Double.POSITIVE_INFINITY;
                    for (final Vector3D element : expectedV) {
                        d = MathLib.min(d, v.subtract(element).getNorm());
                    }
                    Assert.assertEquals(0, d, 1.0e-10);
                }
            }
        });
    }

    /**
     * For coverage purpose for class BSPTree, method visit() and all the subcases
     * 
     * @throws MathArithmeticException
     *         MathArithmeticException
     * @throws MathIllegalArgumentException
     *         MathIllegalArgumentException
     */
    @Test
    public void testIsometrySUB_PLUS_MINUS() throws MathArithmeticException, MathIllegalArgumentException {
        final Vector3D vertex1 = new Vector3D(1.1, 2.2, 3.3);
        final Vector3D vertex2 = new Vector3D(2.0, 2.4, 4.2);
        final Vector3D vertex3 = new Vector3D(2.8, 3.3, 3.7);
        final Vector3D vertex4 = new Vector3D(1.0, 3.6, 4.5);

        @SuppressWarnings("unchecked")
        PolyhedronsSet tree =
            (PolyhedronsSet) new RegionFactory<Euclidean3D>().buildConvex(
                new Plane(vertex3, vertex2, vertex1),
                new Plane(vertex2, vertex3, vertex4),
                new Plane(vertex4, vertex3, vertex1),
                new Plane(vertex1, vertex2, vertex4));

        final Vector3D barycenter = (Vector3D) tree.getBarycenter();
        final Vector3D s = new Vector3D(10.2, 4.3, -6.7);
        final Vector3D c = new Vector3D(-0.2, 2.1, -3.2);
        final Rotation r = new Rotation(new Vector3D(6.2, -4.4, 2.1), 0.12);

        tree = tree.rotate(c, r).translate(s);

        final Vector3D newB =
            new Vector3D(1.0, s,
                1.0, c,
                1.0, r.applyTo(barycenter.subtract(c)));
        Assert.assertEquals(0.0,
            newB.subtract(tree.getBarycenter()).getNorm(),
            1.0e-10);

        final Vector3D[] expectedV = new Vector3D[] {
            new Vector3D(1.0, s,
                1.0, c,
                1.0, r.applyTo(vertex1.subtract(c))),
            new Vector3D(1.0, s,
                1.0, c,
                1.0, r.applyTo(vertex2.subtract(c))),
            new Vector3D(1.0, s,
                1.0, c,
                1.0, r.applyTo(vertex3.subtract(c))),
            new Vector3D(1.0, s,
                1.0, c,
                1.0, r.applyTo(vertex4.subtract(c)))
        };

        tree.getTree(true).visit(new BSPTreeVisitor<Euclidean3D>(){

            @Override
            public Order visitOrder(final BSPTree<Euclidean3D> node) {
                return Order.SUB_PLUS_MINUS;
            }

            @Override
            public void visitInternalNode(final BSPTree<Euclidean3D> node) {
                @SuppressWarnings("unchecked")
                final BoundaryAttribute<Euclidean3D> attribute =
                    (BoundaryAttribute<Euclidean3D>) node.getAttribute();
                if (attribute.getPlusOutside() != null) {
                    checkFacet((SubPlane) attribute.getPlusOutside());
                }
                if (attribute.getPlusInside() != null) {
                    checkFacet((SubPlane) attribute.getPlusInside());
                }
            }

            @Override
            public void visitLeafNode(final BSPTree<Euclidean3D> node) {
                // nothing to do
            }

            private void checkFacet(final SubPlane facet) {
                final Plane plane = (Plane) facet.getHyperplane();
                final Vector2D[][] vertices =
                    ((PolygonsSet) facet.getRemainingRegion()).getVertices();
                Assert.assertEquals(1, vertices.length);
                for (int i = 0; i < vertices[0].length; ++i) {
                    final Vector3D v = plane.toSpace(vertices[0][i]);
                    double d = Double.POSITIVE_INFINITY;
                    for (final Vector3D element : expectedV) {
                        d = MathLib.min(d, v.subtract(element).getNorm());
                    }
                    Assert.assertEquals(0, d, 1.0e-10);
                }
            }
        });
    }

    /**
     * For coverage purpose for class BSPTree, method visit() and all the subcases
     * 
     * @throws MathArithmeticException
     *         MathArithmeticException
     * @throws MathIllegalArgumentException
     *         MathIllegalArgumentException
     */
    @Test
    public void testIsometryMINUS_PLUS_SUB() throws MathArithmeticException, MathIllegalArgumentException {
        final Vector3D vertex1 = new Vector3D(1.1, 2.2, 3.3);
        final Vector3D vertex2 = new Vector3D(2.0, 2.4, 4.2);
        final Vector3D vertex3 = new Vector3D(2.8, 3.3, 3.7);
        final Vector3D vertex4 = new Vector3D(1.0, 3.6, 4.5);

        @SuppressWarnings("unchecked")
        PolyhedronsSet tree =
            (PolyhedronsSet) new RegionFactory<Euclidean3D>().buildConvex(
                new Plane(vertex3, vertex2, vertex1),
                new Plane(vertex2, vertex3, vertex4),
                new Plane(vertex4, vertex3, vertex1),
                new Plane(vertex1, vertex2, vertex4));
        final Vector3D barycenter = (Vector3D) tree.getBarycenter();
        final Vector3D s = new Vector3D(10.2, 4.3, -6.7);
        final Vector3D c = new Vector3D(-0.2, 2.1, -3.2);
        final Rotation r = new Rotation(new Vector3D(6.2, -4.4, 2.1), 0.12);

        tree = tree.rotate(c, r).translate(s);

        final Vector3D newB =
            new Vector3D(1.0, s,
                1.0, c,
                1.0, r.applyTo(barycenter.subtract(c)));
        Assert.assertEquals(0.0,
            newB.subtract(tree.getBarycenter()).getNorm(),
            1.0e-10);

        final Vector3D[] expectedV = new Vector3D[] {
            new Vector3D(1.0, s,
                1.0, c,
                1.0, r.applyTo(vertex1.subtract(c))),
            new Vector3D(1.0, s,
                1.0, c,
                1.0, r.applyTo(vertex2.subtract(c))),
            new Vector3D(1.0, s,
                1.0, c,
                1.0, r.applyTo(vertex3.subtract(c))),
            new Vector3D(1.0, s,
                1.0, c,
                1.0, r.applyTo(vertex4.subtract(c)))
        };

        tree.getTree(true).visit(new BSPTreeVisitor<Euclidean3D>(){

            @Override
            public Order visitOrder(final BSPTree<Euclidean3D> node) {
                return Order.MINUS_PLUS_SUB;
            }

            @Override
            public void visitInternalNode(final BSPTree<Euclidean3D> node) {
                @SuppressWarnings("unchecked")
                final BoundaryAttribute<Euclidean3D> attribute =
                    (BoundaryAttribute<Euclidean3D>) node.getAttribute();
                if (attribute.getPlusOutside() != null) {
                    checkFacet((SubPlane) attribute.getPlusOutside());
                }
                if (attribute.getPlusInside() != null) {
                    checkFacet((SubPlane) attribute.getPlusInside());
                }
            }

            @Override
            public void visitLeafNode(final BSPTree<Euclidean3D> node) {
                // nothing to do
            }

            private void checkFacet(final SubPlane facet) {
                final Plane plane = (Plane) facet.getHyperplane();
                final Vector2D[][] vertices =
                    ((PolygonsSet) facet.getRemainingRegion()).getVertices();
                Assert.assertEquals(1, vertices.length);
                for (int i = 0; i < vertices[0].length; ++i) {
                    final Vector3D v = plane.toSpace(vertices[0][i]);
                    double d = Double.POSITIVE_INFINITY;
                    for (final Vector3D element : expectedV) {
                        d = MathLib.min(d, v.subtract(element).getNorm());
                    }
                    Assert.assertEquals(0, d, 1.0e-10);
                }
            }
        });
    }

    /**
     * For coverage purpose for class BSPTree, method visit() and all the subcases
     * 
     * @throws MathArithmeticException
     *         MathArithmeticException
     * @throws MathIllegalArgumentException
     *         MathIllegalArgumentException
     */
    @Test
    public void testIsometrySUB_MINUS_PLUS() throws MathArithmeticException, MathIllegalArgumentException {
        final Vector3D vertex1 = new Vector3D(1.1, 2.2, 3.3);
        final Vector3D vertex2 = new Vector3D(2.0, 2.4, 4.2);
        final Vector3D vertex3 = new Vector3D(2.8, 3.3, 3.7);
        final Vector3D vertex4 = new Vector3D(1.0, 3.6, 4.5);

        @SuppressWarnings("unchecked")
        PolyhedronsSet tree =
            (PolyhedronsSet) new RegionFactory<Euclidean3D>().buildConvex(
                new Plane(vertex3, vertex2, vertex1),
                new Plane(vertex2, vertex3, vertex4),
                new Plane(vertex4, vertex3, vertex1),
                new Plane(vertex1, vertex2, vertex4));
        final Vector3D barycenter = (Vector3D) tree.getBarycenter();
        final Vector3D s = new Vector3D(10.2, 4.3, -6.7);
        final Vector3D c = new Vector3D(-0.2, 2.1, -3.2);
        final Rotation r = new Rotation(new Vector3D(6.2, -4.4, 2.1), 0.12);

        tree = tree.rotate(c, r).translate(s);

        final Vector3D newB =
            new Vector3D(1.0, s,
                1.0, c,
                1.0, r.applyTo(barycenter.subtract(c)));
        Assert.assertEquals(0.0,
            newB.subtract(tree.getBarycenter()).getNorm(),
            1.0e-10);

        final Vector3D[] expectedV = new Vector3D[] {
            new Vector3D(1.0, s,
                1.0, c,
                1.0, r.applyTo(vertex1.subtract(c))),
            new Vector3D(1.0, s,
                1.0, c,
                1.0, r.applyTo(vertex2.subtract(c))),
            new Vector3D(1.0, s,
                1.0, c,
                1.0, r.applyTo(vertex3.subtract(c))),
            new Vector3D(1.0, s,
                1.0, c,
                1.0, r.applyTo(vertex4.subtract(c)))
        };

        tree.getTree(true).visit(new BSPTreeVisitor<Euclidean3D>(){

            @Override
            public Order visitOrder(final BSPTree<Euclidean3D> node) {
                return Order.SUB_MINUS_PLUS;
            }

            @Override
            public void visitInternalNode(final BSPTree<Euclidean3D> node) {
                @SuppressWarnings("unchecked")
                final BoundaryAttribute<Euclidean3D> attribute =
                    (BoundaryAttribute<Euclidean3D>) node.getAttribute();
                if (attribute.getPlusOutside() != null) {
                    checkFacet((SubPlane) attribute.getPlusOutside());
                }
                if (attribute.getPlusInside() != null) {
                    checkFacet((SubPlane) attribute.getPlusInside());
                }
            }

            @Override
            public void visitLeafNode(final BSPTree<Euclidean3D> node) {
                // nothing to do
            }

            private void checkFacet(final SubPlane facet) {
                final Plane plane = (Plane) facet.getHyperplane();
                final Vector2D[][] vertices =
                    ((PolygonsSet) facet.getRemainingRegion()).getVertices();
                Assert.assertEquals(1, vertices.length);
                for (int i = 0; i < vertices[0].length; ++i) {
                    final Vector3D v = plane.toSpace(vertices[0][i]);
                    double d = Double.POSITIVE_INFINITY;
                    for (final Vector3D element : expectedV) {
                        d = MathLib.min(d, v.subtract(element).getNorm());
                    }
                    Assert.assertEquals(0, d, 1.0e-10);
                }
            }
        });
    }

    @Test
    public void testBuildBox() {
        final double x = 1.0;
        final double y = 2.0;
        final double z = 3.0;
        final double w = 0.1;
        final double l = 1.0;
        final PolyhedronsSet tree =
            new PolyhedronsSet(x - l, x + l, y - w, y + w, z - w, z + w);
        final Vector3D barycenter = (Vector3D) tree.getBarycenter();

        Assert.assertEquals(x, barycenter.getX(), 1.0e-10);
        Assert.assertEquals(y, barycenter.getY(), 1.0e-10);
        Assert.assertEquals(z, barycenter.getZ(), 1.0e-10);
        Assert.assertEquals(8 * l * w * w, tree.getSize(), 1.0e-10);
        Assert.assertEquals(8 * w * (2 * l + w), tree.getBoundarySize(), 1.0e-10);
    }

    @Test
    public void testCross() {

        final double x = 1.0;
        final double y = 2.0;
        final double z = 3.0;
        final double w = 0.1;
        final double l = 1.0;
        final PolyhedronsSet xBeam =
            new PolyhedronsSet(x - l, x + l, y - w, y + w, z - w, z + w);
        final PolyhedronsSet yBeam =
            new PolyhedronsSet(x - w, x + w, y - l, y + l, z - w, z + w);
        final PolyhedronsSet zBeam =
            new PolyhedronsSet(x - w, x + w, y - w, y + w, z - l, z + l);
        final RegionFactory<Euclidean3D> factory = new RegionFactory<>();
        final PolyhedronsSet tree = (PolyhedronsSet) factory.union(xBeam, factory.union(yBeam, zBeam));
        final Vector3D barycenter = (Vector3D) tree.getBarycenter();

        Assert.assertEquals(x, barycenter.getX(), 1.0e-10);
        Assert.assertEquals(y, barycenter.getY(), 1.0e-10);
        Assert.assertEquals(z, barycenter.getZ(), 1.0e-10);
        Assert.assertEquals(8 * w * w * (3 * l - 2 * w), tree.getSize(), 1.0e-10);
        Assert.assertEquals(24 * w * (2 * l - w), tree.getBoundarySize(), 1.0e-10);
    }

    /**
     * For coverage purpose.
     * Creation of a [0,1]x[0,1]x[0,1] box with a hole [0.25,0.75]x[0.25,0.75]x[0,1].
     */
    @Test
    public void testGetPlusInside() {

        final PolyhedronsSet box1 = new PolyhedronsSet(0, 0.25, 0, 1, 0, 1);
        final PolyhedronsSet box2 = new PolyhedronsSet(0.25, 0.75, 0, 0.25, 0, 1);
        final PolyhedronsSet box3 = new PolyhedronsSet(0.25, 0.75, 0.75, 1, 0, 1);
        final PolyhedronsSet box4 = new PolyhedronsSet(0.75, 1, 0, 1, 0, 1);
        final RegionFactory<Euclidean3D> factory = new RegionFactory<>();
        final PolyhedronsSet tree =
            (PolyhedronsSet) factory.union(box1, factory.union(box2, factory.union(box3, box4)));
        tree.getBarycenter();
        checkPoints(Region.Location.BOUNDARY, tree, new Vector3D[] {
            new Vector3D(0.0, 0.5, 0.5),
            new Vector3D(1.0, 0.5, 0.5),
            new Vector3D(0.5, 0.0, 0.5),
            new Vector3D(0.5, 1.0, 0.5)
        });

        checkPoints(Region.Location.OUTSIDE, tree, new Vector3D[] {
            new Vector3D(0.5, 0.5, 0.0),
            new Vector3D(0.5, 0.5, 1.0)
        });

        // Intersection
        final Vector3D pointA = new Vector3D(0.25, 0.25, 0.25);
        final Vector3D pointB = new Vector3D(0.75, 0.25, 0.25);
        final Line line = new Line(pointA, pointB);
        final SubHyperplane<Euclidean3D> subplane = tree.firstIntersection(pointA, line);
        final Hyperplane<Euclidean3D> plane = subplane.getHyperplane();

        Assert.assertEquals(-1.0, plane.getOffset(Vector3D.ZERO), 0.);
        Assert.assertEquals(0.0, plane.getOffset(Vector3D.PLUS_I), 0.);
        Assert.assertEquals(-1.0, plane.getOffset(Vector3D.PLUS_J), 0.);
        Assert.assertEquals(-1.0, plane.getOffset(Vector3D.PLUS_K), 0.);

        // Intersection
        final Vector3D pointA2 = new Vector3D(0.5, 0.5, 0.5);
        final Vector3D pointB2 = new Vector3D(-1, 0.5, 0.5);
        final Line line2 = new Line(pointA2, pointB2);
        final SubHyperplane<Euclidean3D> subplane2 = tree.firstIntersection(pointA2, line2);
        final Hyperplane<Euclidean3D> plane2 = subplane2.getHyperplane();

        Assert.assertEquals(-0.75, plane2.getOffset(Vector3D.ZERO), 0.);
        Assert.assertEquals(0.25, plane2.getOffset(Vector3D.PLUS_I), 0.);
        Assert.assertEquals(-0.75, plane2.getOffset(Vector3D.PLUS_J), 0.);
        Assert.assertEquals(-0.75, plane2.getOffset(Vector3D.PLUS_K), 0.);
    }

    @Test
    public void testIssue780() throws MathArithmeticException {
        final float[] coords = {
            1.000000f, -1.000000f, -1.000000f,
            1.000000f, -1.000000f, 1.000000f,
            -1.000000f, -1.000000f, 1.000000f,
            -1.000000f, -1.000000f, -1.000000f,
            1.000000f, 1.000000f, -1f,
            0.999999f, 1.000000f, 1.000000f, // 1.000000f, 1.000000f, 1.000000f,
            -1.000000f, 1.000000f, 1.000000f,
            -1.000000f, 1.000000f, -1.000000f };
        final int[] indices = {
            0, 1, 2, 0, 2, 3,
            4, 7, 6, 4, 6, 5,
            0, 4, 5, 0, 5, 1,
            1, 5, 6, 1, 6, 2,
            2, 6, 7, 2, 7, 3,
            4, 0, 3, 4, 3, 7 };
        final ArrayList<SubHyperplane<Euclidean3D>> subHyperplaneList = new ArrayList<>();

        for (int idx = 0; idx < indices.length; idx += 3) {
            final int idxA = indices[idx] * 3;
            final int idxB = indices[idx + 1] * 3;
            final int idxC = indices[idx + 2] * 3;
            final Vector3D v_1 = new Vector3D(coords[idxA], coords[idxA + 1], coords[idxA + 2]);
            final Vector3D v_2 = new Vector3D(coords[idxB], coords[idxB + 1], coords[idxB + 2]);
            final Vector3D v_3 = new Vector3D(coords[idxC], coords[idxC + 1], coords[idxC + 2]);
            final Vector3D[] vertices = { v_1, v_2, v_3 };
            final Plane polyPlane = new Plane(v_1, v_2, v_3);
            final ArrayList<SubHyperplane<Euclidean2D>> lines = new ArrayList<>();

            final Vector2D[] projPts = new Vector2D[vertices.length];
            for (int ptIdx = 0; ptIdx < projPts.length; ptIdx++) {
                projPts[ptIdx] = polyPlane.toSubSpace(vertices[ptIdx]);
            }

            SubLine lineInPlane = null;
            for (int ptIdx = 0; ptIdx < projPts.length; ptIdx++) {
                lineInPlane = new SubLine(projPts[ptIdx], projPts[(ptIdx + 1) % projPts.length]);
                lines.add(lineInPlane);
            }
            final Region<Euclidean2D> polyRegion = new PolygonsSet(lines);
            final SubPlane polygon = new SubPlane(polyPlane, polyRegion);
            subHyperplaneList.add(polygon);
        }

        final PolyhedronsSet polyhedronsSet = new PolyhedronsSet(subHyperplaneList);
        Assert.assertEquals(8.0, polyhedronsSet.getSize(), 3.0e-6);
        Assert.assertEquals(24.0, polyhedronsSet.getBoundarySize(), 5.0e-6);
    }
    
    @Test
    public void testConnectedFacets() throws IOException, ParseException {
        final String file = "src/test/resources/fr/cnes/sirius/patrius/math/geometry/pentomino-N.ply";
        final InputStream stream =  new FileInputStream(file);
        final PLYParser   parser = new PLYParser(stream);
        stream.close();
        final BRep brep = new BRep(parser.getVertices(), parser.getFaces());
        final PolyhedronsSet polyhedron = new PolyhedronsSet(brep, 1.0E-10);
        Assert.assertEquals( 5.0, polyhedron.getSize(), 1.0e-10);
        Assert.assertEquals(22.0, polyhedron.getBoundarySize(), 1.0e-10);
    }

    @Test
    public void testTooClose() {
        final String file = "src/test/resources/fr/cnes/sirius/patrius/math/geometry/pentomino-N-too-close.ply";
        checkError(file, "Too close vertices near point (0, 0, 0).");
    }

    @Test
    public void testHole() {
        final String file = "src/test/resources/fr/cnes/sirius/patrius/math/geometry/pentomino-N-hole.ply";
        checkError(file, "The edge joining points (0, 0, 0) and (0, 0, 1) is connected to one facet only.");
    }

    @Test
    public void testNonPlanar() {
        final String file = "src/test/resources/fr/cnes/sirius/patrius/math/geometry/pentomino-N-out-of-plane.ply";
        checkError(file, "The point (0, 0, 0) is out of plane.");
    }

    @Test
    public void testOrientation() {
        final String file = "src/test/resources/fr/cnes/sirius/patrius/math/geometry/pentomino-N-bad-orientation.ply";
        checkError(file, "Facets orientation mismatch around edge joining points (1, 1, 0) and (2, 1, 0).");
    }

    @Test
    public void testFacet2Vertices() {
        checkError(Arrays.asList(Vector3D.ZERO, Vector3D.PLUS_I, Vector3D.PLUS_J, Vector3D.PLUS_K),
                   Arrays.asList(new int[] { 0, 1, 2 }, new int[] {2, 3}),
                   "3 points are required, got only 2");
    }

    private static void checkError(final String fileLocation, final String expected) {
        try {
            final InputStream stream = new FileInputStream(fileLocation);
            final PLYParser   parser = new PLYParser(stream);
            stream.close();
            checkError(parser.getVertices(), parser.getFaces(), expected);
        } catch (final IOException ioe) {
            Assert.fail(ioe.getLocalizedMessage());
        } catch (final ParseException pe) {
            Assert.fail(pe.getLocalizedMessage());
        }
    }
    
    private static void checkError(final List<Vector3D> vertices, final List<int[]> facets,
                            final String expected) {
        try {
            new PolyhedronsSet(vertices, facets, 1.0e-10);
            Assert.fail("an exception should have been thrown");
        } catch (final MathIllegalArgumentException miae) {
            Assert.assertEquals(expected, miae.getMessage());
        }
    }
    
    private static void checkPoints(final Region.Location expected, final PolyhedronsSet tree, final Vector3D[] points) {
        for (final Vector3D point : points) {
            Assert.assertEquals(expected, tree.checkPoint(point));
        }
    }
    
    /** This class is a small and incomplete parser for PLY files.
     * <p>
     * This parser is only intended for test purposes, it does not
     * parse the full header, it does not handle all properties,
     * it has rudimentary error handling.
     * </p>
     */
    private static class PLYParser {

        /** Parsed vertices. */
        private Vector3D[] vertices;

        /** Parsed faces. */
        private int[][] faces;

        /** Reader for PLY data. */
        private BufferedReader br;

        /** Last parsed line. */
        private String line;

        /** Simple constructor.
         * @param stream stream to parse (closing it remains caller responsibility)
         * @exception IOException if stream cannot be read
         * @exception ParseException if stream content cannot be parsed
         */
        public PLYParser(final InputStream stream)
            throws IOException, ParseException {

            try {
                this.br = new BufferedReader(new InputStreamReader(stream, "UTF-8"));

                // parse the header
                List<Field> fields = parseNextLine();
                if (fields.size() != 1 || fields.get(0).getToken() != Token.PLY) {
                    complain();
                }

                boolean parsing       = true;
                int nbVertices        = -1;
                int nbFaces           = -1;
                int xIndex            = -1;
                int yIndex            = -1;
                int zIndex            = -1;
                int vPropertiesNumber = -1;
                boolean inVertexElt   = false;
                boolean inFaceElt     = false;
                while (parsing) {
                    fields = parseNextLine();
                    if (fields.size() < 1) {
                        complain();
                    }
                    switch (fields.get(0).getToken()) {
                        case FORMAT:
                            if (fields.size() != 3 ||
                            fields.get(1).getToken() != Token.ASCII ||
                            fields.get(2).getToken() != Token.UNKNOWN ||
                            !Precision.equals(Double.parseDouble(fields.get(2).getValue()), 1.0, 0.001)) {
                                complain();
                            }
                            inVertexElt = false;
                            inFaceElt   = false;
                            break;
                        case COMMENT:
                            // we just ignore this line
                            break;
                        case ELEMENT:
                            if (fields.size() != 3 ||
                            (fields.get(1).getToken() != Token.VERTEX && fields.get(1).getToken() != Token.FACE) ||
                            fields.get(2).getToken() != Token.UNKNOWN) {
                                complain();
                            }
                            if (fields.get(1).getToken() == Token.VERTEX) {
                                nbVertices  = Integer.parseInt(fields.get(2).getValue());
                                inVertexElt = true;
                                inFaceElt   = false;
                            } else {
                                nbFaces     = Integer.parseInt(fields.get(2).getValue());
                                inVertexElt = false;
                                inFaceElt   = true;
                            }
                            break;
                        case PROPERTY:
                            if (inVertexElt) {
                                ++vPropertiesNumber;
                                if (fields.size() != 3 ||
                                    (fields.get(1).getToken() != Token.CHAR   &&
                                     fields.get(1).getToken() != Token.UCHAR  &&
                                     fields.get(1).getToken() != Token.SHORT  &&
                                     fields.get(1).getToken() != Token.USHORT &&
                                     fields.get(1).getToken() != Token.INT    &&
                                     fields.get(1).getToken() != Token.UINT   &&
                                     fields.get(1).getToken() != Token.FLOAT  &&
                                     fields.get(1).getToken() != Token.DOUBLE)) {
                                    complain();
                                }
                                if (fields.get(2).getToken() == Token.X) {
                                    xIndex = vPropertiesNumber;
                                }else if (fields.get(2).getToken() == Token.Y) {
                                    yIndex = vPropertiesNumber;
                                }else if (fields.get(2).getToken() == Token.Z) {
                                    zIndex = vPropertiesNumber;
                                }
                            } else if (inFaceElt) {
                                if (fields.size() != 5 ||
                                    fields.get(1).getToken()  != Token.LIST   &&
                                    (fields.get(2).getToken() != Token.CHAR   &&
                                     fields.get(2).getToken() != Token.UCHAR  &&
                                     fields.get(2).getToken() != Token.SHORT  &&
                                     fields.get(2).getToken() != Token.USHORT &&
                                     fields.get(2).getToken() != Token.INT    &&
                                     fields.get(2).getToken() != Token.UINT) ||
                                    (fields.get(3).getToken() != Token.CHAR   &&
                                     fields.get(3).getToken() != Token.UCHAR  &&
                                     fields.get(3).getToken() != Token.SHORT  &&
                                     fields.get(3).getToken() != Token.USHORT &&
                                     fields.get(3).getToken() != Token.INT    &&
                                     fields.get(3).getToken() != Token.UINT) ||
                                     fields.get(4).getToken() != Token.VERTEX_INDICES) {
                                    complain();
                                }
                            } else {
                                complain();
                            }
                            break;
                        case END_HEADER:
                            inVertexElt = false;
                            inFaceElt   = false;
                            parsing     = false;
                            break;
                        default:
                            throw new ParseException(String.format("unable to parse line: %s", this.line));
                    }
                }
                ++vPropertiesNumber;

                // parse vertices
                this.vertices = new Vector3D[nbVertices];
                for (int i = 0; i < nbVertices; ++i) {
                    fields = parseNextLine();
                    if (fields.size() != vPropertiesNumber ||
                        fields.get(xIndex).getToken() != Token.UNKNOWN ||
                        fields.get(yIndex).getToken() != Token.UNKNOWN ||
                        fields.get(zIndex).getToken() != Token.UNKNOWN) {
                        complain();
                    }
                    this.vertices[i] = new Vector3D(Double.parseDouble(fields.get(xIndex).getValue()),
                                               Double.parseDouble(fields.get(yIndex).getValue()),
                                               Double.parseDouble(fields.get(zIndex).getValue()));
                }

                // parse faces
                this.faces = new int[nbFaces][];
                for (int i = 0; i < nbFaces; ++i) {
                    fields = parseNextLine();
                    if (fields.isEmpty() ||
                        fields.size() != (Integer.parseInt(fields.get(0).getValue()) + 1)) {
                        complain();
                    }
                    this.faces[i] = new int[fields.size() - 1];
                    for (int j = 0; j < this.faces[i].length; ++j) {
                        this.faces[i][j] = Integer.parseInt(fields.get(j + 1).getValue());
                    }
                }

            } catch (final NumberFormatException nfe) {
                complain();
            }
        }

        /** Complain about a bad line.
         * @exception ParseException always thrown
         */
        private void complain() throws ParseException {
            throw new ParseException(String.format("unable to parse line: %s", this.line));
        }

        /** Parse next line.
         * @return parsed fields
         * @exception IOException if stream cannot be read
         * @exception ParseException if the line does not contain the expected number of fields
         */
        private List<Field> parseNextLine()
            throws IOException, ParseException {
            final List<Field> fields = new ArrayList<>();
            this.line = this.br.readLine();
            if (this.line == null) {
                throw new EOFException();
            }
            final StringTokenizer tokenizer = new StringTokenizer(this.line);
            while (tokenizer.hasMoreTokens()) {
                fields.add(new Field(tokenizer.nextToken()));
            }
            return fields;
        }

        /** Get the parsed vertices.
         * @return parsed vertices
         */
        public List<Vector3D> getVertices() {
            return Arrays.asList(this.vertices);
        }

        /** Get the parsed faces.
         * @return parsed faces
         */
        public List<int[]> getFaces() {
            return Arrays.asList(this.faces);
        }

        /** Tokens from PLY files. */
        private static enum Token {
            PLY, FORMAT, ASCII, BINARY_BIG_ENDIAN, BINARY_LITTLE_ENDIAN,
            COMMENT, ELEMENT, VERTEX, FACE, PROPERTY, LIST, OBJ_INFO,
            CHAR, UCHAR, SHORT, USHORT, INT, UINT, FLOAT, DOUBLE,
            X, Y, Z, VERTEX_INDICES, END_HEADER, UNKNOWN;
        }

        /** Parsed line fields. */
        private static class Field {

            /** Token. */
            private final Token token;

            /** Value. */
            private final String value;

            /** Simple constructor.
             * @param value field value
             */
            public Field(final String value) {
                Token parsedToken = null;
                try {
                    parsedToken = Token.valueOf(value.toUpperCase());
                } catch (final IllegalArgumentException iae) {
                    parsedToken = Token.UNKNOWN;
                }
                this.token = parsedToken;
                this.value = value;
            }

            /** Get the recognized token.
             * @return recognized token
             */
            public Token getToken() {
                return this.token;
            }

            /** Get the field value.
             * @return field value
             */
            public String getValue() {
                return this.value;
            }

        }

    }
}
