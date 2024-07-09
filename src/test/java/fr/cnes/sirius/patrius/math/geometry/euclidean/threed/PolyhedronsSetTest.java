/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
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
 *
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:306:12/11/2014: coverage
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.MathArithmeticException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
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
        this.checkPoints(Region.Location.BOUNDARY, tree, new Vector3D[] {
            new Vector3D(0.0, 0.5, 0.5),
            new Vector3D(1.0, 0.5, 0.5),
            new Vector3D(0.5, 0.0, 0.5),
            new Vector3D(0.5, 1.0, 0.5),
            new Vector3D(0.5, 0.5, 0.0),
            new Vector3D(0.5, 0.5, 1.0)
        });
        this.checkPoints(Region.Location.OUTSIDE, tree, new Vector3D[] {
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
        this.checkPoints(Region.Location.BOUNDARY, tree, new Vector3D[] {
            vertex1, vertex2, vertex3, vertex4,
            new Vector3D(third, vertex1, third, vertex2, third, vertex3),
            new Vector3D(third, vertex2, third, vertex3, third, vertex4),
            new Vector3D(third, vertex3, third, vertex4, third, vertex1),
            new Vector3D(third, vertex4, third, vertex1, third, vertex2)
        });
        this.checkPoints(Region.Location.OUTSIDE, tree, new Vector3D[] {
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
                    this.checkFacet((SubPlane) attribute.getPlusOutside());
                }
                if (attribute.getPlusInside() != null) {
                    this.checkFacet((SubPlane) attribute.getPlusInside());
                }
            }

            @Override
            public void visitLeafNode(final BSPTree<Euclidean3D> node) {
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
                    this.checkFacet((SubPlane) attribute.getPlusOutside());
                }
                if (attribute.getPlusInside() != null) {
                    this.checkFacet((SubPlane) attribute.getPlusInside());
                }
            }

            @Override
            public void visitLeafNode(final BSPTree<Euclidean3D> node) {
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
                    this.checkFacet((SubPlane) attribute.getPlusOutside());
                }
                if (attribute.getPlusInside() != null) {
                    this.checkFacet((SubPlane) attribute.getPlusInside());
                }
            }

            @Override
            public void visitLeafNode(final BSPTree<Euclidean3D> node) {
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
                    this.checkFacet((SubPlane) attribute.getPlusOutside());
                }
                if (attribute.getPlusInside() != null) {
                    this.checkFacet((SubPlane) attribute.getPlusInside());
                }
            }

            @Override
            public void visitLeafNode(final BSPTree<Euclidean3D> node) {
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
        final RegionFactory<Euclidean3D> factory = new RegionFactory<Euclidean3D>();
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
        final RegionFactory<Euclidean3D> factory = new RegionFactory<Euclidean3D>();
        final PolyhedronsSet tree =
            (PolyhedronsSet) factory.union(box1, factory.union(box2, factory.union(box3, box4)));
        tree.getBarycenter();
        this.checkPoints(Region.Location.BOUNDARY, tree, new Vector3D[] {
            new Vector3D(0.0, 0.5, 0.5),
            new Vector3D(1.0, 0.5, 0.5),
            new Vector3D(0.5, 0.0, 0.5),
            new Vector3D(0.5, 1.0, 0.5)
        });
        this.checkPoints(Region.Location.OUTSIDE, tree, new Vector3D[] {
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
        final ArrayList<SubHyperplane<Euclidean3D>> subHyperplaneList = new ArrayList<SubHyperplane<Euclidean3D>>();
        for (int idx = 0; idx < indices.length; idx += 3) {
            final int idxA = indices[idx] * 3;
            final int idxB = indices[idx + 1] * 3;
            final int idxC = indices[idx + 2] * 3;
            final Vector3D v_1 = new Vector3D(coords[idxA], coords[idxA + 1], coords[idxA + 2]);
            final Vector3D v_2 = new Vector3D(coords[idxB], coords[idxB + 1], coords[idxB + 2]);
            final Vector3D v_3 = new Vector3D(coords[idxC], coords[idxC + 1], coords[idxC + 2]);
            final Vector3D[] vertices = { v_1, v_2, v_3 };
            final Plane polyPlane = new Plane(v_1, v_2, v_3);
            final ArrayList<SubHyperplane<Euclidean2D>> lines = new ArrayList<SubHyperplane<Euclidean2D>>();

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

    private void checkPoints(final Region.Location expected, final PolyhedronsSet tree, final Vector3D[] points) {
        for (final Vector3D point : points) {
            Assert.assertEquals(expected, tree.checkPoint(point));
        }
    }

}
