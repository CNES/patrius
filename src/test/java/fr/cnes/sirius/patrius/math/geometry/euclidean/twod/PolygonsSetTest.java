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
 * VERSION::DM:525:22/04/2016: add new functionalities existing in LibKernel
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.twod;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.geometry.euclidean.oned.Interval;
import fr.cnes.sirius.patrius.math.geometry.euclidean.oned.IntervalsSet;
import fr.cnes.sirius.patrius.math.geometry.euclidean.oned.Vector1D;
import fr.cnes.sirius.patrius.math.geometry.partitioning.BSPTree;
import fr.cnes.sirius.patrius.math.geometry.partitioning.Region;
import fr.cnes.sirius.patrius.math.geometry.partitioning.Region.Location;
import fr.cnes.sirius.patrius.math.geometry.partitioning.RegionFactory;
import fr.cnes.sirius.patrius.math.geometry.partitioning.SubHyperplane;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;

public class PolygonsSetTest {

    @Test
    public void testEmpty() {
        final Vector2D[][] vertices = new Vector2D[0][0];

        final PolygonsSet set = buildSet(vertices);
        Assert.assertEquals(Double.POSITIVE_INFINITY, set.getSize(), 0.);
        checkVertices(set.getVertices(), vertices);
    }

    @Test
    public void testOpenLoop() {
        // Code coverage
        final Vector2D[][] vertices = new Vector2D[][] {
            new Vector2D[] {
                new Vector2D(36.0, 22.0),
                new Vector2D(39.0, 32.0)
            }
        };
        final ArrayList<SubHyperplane<Euclidean2D>> edges = new ArrayList<>();
        for (final Vector2D[] vertice : vertices) {
            final int l = vertice.length;
            for (int j = 0; j < l - 1; ++j) {
                edges.add(buildSegment(vertice[j], vertice[(j + 1) % l]));
            }
        }

        final PolygonsSet set = new PolygonsSet(edges);
        Assert.assertEquals(Double.POSITIVE_INFINITY, set.getSize(), 0.);
    }

    /**
     * Coverage purpose. Tests the if( (end == null) && !open) of private
     * method followLoop with a parabolic curve y = x^2 evaluated in -1,0 and 1.
     */
    @Test
    public void testWholeSpaceVerticesToTree() {
        final PolygonsSet set = new PolygonsSet(1e-3);
        final Vector2D[][] vertices = set.getVertices();
        Assert.assertEquals(vertices.length, 0);
    }

    /**
     * Coverage purpose. Tests the if( (end == null) && !open) of private
     * method followLoop with a parabolic curve y = x^2 evaluated in -1,0 and 1.
     */
    @Test
    public void testMathInternalErrorFollowLoop() {
        try {
            final Vector2D[][] vertices = new Vector2D[][] {
                new Vector2D[] {
                    new Vector2D(-1.0, 1.0),
                    new Vector2D(0.0, 0.0),
                    new Vector2D(1.0, 1.0)
                }
            };
            final ArrayList<SubHyperplane<Euclidean2D>> edges = new ArrayList<>();
            for (final Vector2D[] vertice : vertices) {
                final int l = vertice.length;
                for (int j = 0; j < l - 1; ++j) {
                    edges.add(buildSegment(vertice[j], vertice[(j + 1) % l]));
                }
            }
            final PolygonsSet set = new PolygonsSet(edges);
            set.getVertices();
            Assert.fail();
        } catch (final Exception e) {
            // expected behavior MathInternalError
        }
    }

    /**
     * Coverage purpose for else if (loop.get(0).getStart() == null) in
     * method getVertices.
     */
    @Test
    public void testOpenLoopGetVertices() {
        final Vector2D[][] vertices = new Vector2D[][] {
            new Vector2D[] {
                new Vector2D(0, 0),
                new Vector2D(1, 1),
                new Vector2D(2, 2),
                new Vector2D(2, 3),
                new Vector2D(1, 3)
            }
        };
        final ArrayList<SubHyperplane<Euclidean2D>> edges = new ArrayList<>();
        for (final Vector2D[] vertice : vertices) {
            final int l = vertice.length;
            for (int j = 0; j < l - 1; ++j) {
                edges.add(buildSegment(vertice[j], vertice[(j + 1) % l]));
            }
        }
        final PolygonsSet set = new PolygonsSet(edges);
        final Vector2D[][] verticesResult = set.getVertices();
        final double eps = Precision.DOUBLE_COMPARISON_EPSILON;

        // it's an open loop, first point equals null
        Assert.assertEquals(verticesResult[0][0], null);

        // because he will compare references otherwise, let's compare coordinates by coordinates
        Assert.assertEquals(verticesResult[0][1].getX(), vertices[0][1].getX(), eps);
        Assert.assertEquals(verticesResult[0][2].getX(), vertices[0][2].getX(), eps);
        Assert.assertEquals(verticesResult[0][3].getX(), vertices[0][3].getX(), eps);
        // last point is dummy point

        Assert.assertEquals(verticesResult[0][1].getY(), vertices[0][1].getY(), eps);
        Assert.assertEquals(verticesResult[0][2].getY(), vertices[0][2].getY(), eps);
        Assert.assertEquals(verticesResult[0][3].getY(), vertices[0][3].getY(), eps);
    }

    @Test
    public void testSimplyConnected() {
        final Vector2D[][] vertices = new Vector2D[][] {
            new Vector2D[] {
                new Vector2D(36.0, 22.0),
                new Vector2D(39.0, 32.0),
                new Vector2D(19.0, 32.0),
                new Vector2D(6.0, 16.0),
                new Vector2D(31.0, 10.0),
                new Vector2D(42.0, 16.0),
                new Vector2D(34.0, 20.0),
                new Vector2D(29.0, 19.0),
                new Vector2D(23.0, 22.0),
                new Vector2D(33.0, 25.0)
            }
        };
        final PolygonsSet set = buildSet(vertices);
        Assert.assertEquals(Region.Location.OUTSIDE, set.checkPoint(new Vector2D(50.0, 30.0)));
        checkPoints(Region.Location.INSIDE, set, new Vector2D[] {
            new Vector2D(30.0, 15.0),
            new Vector2D(15.0, 20.0),
            new Vector2D(24.0, 25.0),
            new Vector2D(35.0, 30.0),
            new Vector2D(19.0, 17.0)
        });
        checkPoints(Region.Location.OUTSIDE, set, new Vector2D[] {
            new Vector2D(50.0, 30.0),
            new Vector2D(30.0, 35.0),
            new Vector2D(10.0, 25.0),
            new Vector2D(10.0, 10.0),
            new Vector2D(40.0, 10.0),
            new Vector2D(50.0, 15.0),
            new Vector2D(30.0, 22.0)
        });
        checkPoints(Region.Location.BOUNDARY, set, new Vector2D[] {
            new Vector2D(30.0, 32.0),
            new Vector2D(34.0, 20.0)
        });
        checkVertices(set.getVertices(), vertices);
    }

    @Test
    public void testStair() {
        final Vector2D[][] vertices = new Vector2D[][] {
            new Vector2D[] {
                new Vector2D(0.0, 0.0),
                new Vector2D(0.0, 2.0),
                new Vector2D(-0.1, 2.0),
                new Vector2D(-0.1, 1.0),
                new Vector2D(-0.3, 1.0),
                new Vector2D(-0.3, 1.5),
                new Vector2D(-1.3, 1.5),
                new Vector2D(-1.3, 2.0),
                new Vector2D(-1.8, 2.0),
                new Vector2D(-1.8 - 1.0 / MathLib.sqrt(2.0),
                    2.0 - 1.0 / MathLib.sqrt(2.0))
            }
        };

        final PolygonsSet set = buildSet(vertices);
        checkVertices(set.getVertices(), vertices);

        Assert.assertEquals(1.1 + 0.95 * MathLib.sqrt(2.0), set.getSize(), 1.0e-10);

    }

    @Test
    public void testHole() {
        final Vector2D[][] vertices = new Vector2D[][] {
            new Vector2D[] {
                new Vector2D(0.0, 0.0),
                new Vector2D(3.0, 0.0),
                new Vector2D(3.0, 3.0),
                new Vector2D(0.0, 3.0)
            }, new Vector2D[] {
                new Vector2D(1.0, 2.0),
                new Vector2D(2.0, 2.0),
                new Vector2D(2.0, 1.0),
                new Vector2D(1.0, 1.0)
            }
        };
        final PolygonsSet set = buildSet(vertices);
        checkPoints(Region.Location.INSIDE, set, new Vector2D[] {
            new Vector2D(0.5, 0.5),
            new Vector2D(1.5, 0.5),
            new Vector2D(2.5, 0.5),
            new Vector2D(0.5, 1.5),
            new Vector2D(2.5, 1.5),
            new Vector2D(0.5, 2.5),
            new Vector2D(1.5, 2.5),
            new Vector2D(2.5, 2.5),
            new Vector2D(0.5, 1.0)
        });
        checkPoints(Region.Location.OUTSIDE, set, new Vector2D[] {
            new Vector2D(1.5, 1.5),
            new Vector2D(3.5, 1.0),
            new Vector2D(4.0, 1.5),
            new Vector2D(6.0, 6.0)
        });
        checkPoints(Region.Location.BOUNDARY, set, new Vector2D[] {
            new Vector2D(1.0, 1.0),
            new Vector2D(1.5, 0.0),
            new Vector2D(1.5, 1.0),
            new Vector2D(1.5, 2.0),
            new Vector2D(1.5, 3.0),
            new Vector2D(3.0, 3.0)
        });
        checkVertices(set.getVertices(), vertices);
    }

    @Test
    public void testDisjointPolygons() {
        final Vector2D[][] vertices = new Vector2D[][] {
            new Vector2D[] {
                new Vector2D(0.0, 1.0),
                new Vector2D(2.0, 1.0),
                new Vector2D(1.0, 2.0)
            }, new Vector2D[] {
                new Vector2D(4.0, 0.0),
                new Vector2D(5.0, 1.0),
                new Vector2D(3.0, 1.0)
            }
        };
        final PolygonsSet set = buildSet(vertices);
        Assert.assertEquals(Region.Location.INSIDE, set.checkPoint(new Vector2D(1.0, 1.5)));
        checkPoints(Region.Location.INSIDE, set, new Vector2D[] {
            new Vector2D(1.0, 1.5),
            new Vector2D(4.5, 0.8)
        });
        checkPoints(Region.Location.OUTSIDE, set, new Vector2D[] {
            new Vector2D(1.0, 0.0),
            new Vector2D(3.5, 1.2),
            new Vector2D(2.5, 1.0),
            new Vector2D(3.0, 4.0)
        });
        checkPoints(Region.Location.BOUNDARY, set, new Vector2D[] {
            new Vector2D(1.0, 1.0),
            new Vector2D(3.5, 0.5),
            new Vector2D(0.0, 1.0)
        });
        checkVertices(set.getVertices(), vertices);
    }

    @Test
    public void testOppositeHyperplanes() {
        final Vector2D[][] vertices = new Vector2D[][] {
            new Vector2D[] {
                new Vector2D(1.0, 0.0),
                new Vector2D(2.0, 1.0),
                new Vector2D(3.0, 1.0),
                new Vector2D(2.0, 2.0),
                new Vector2D(1.0, 1.0),
                new Vector2D(0.0, 1.0)
            }
        };
        final PolygonsSet set = buildSet(vertices);
        checkVertices(set.getVertices(), vertices);
    }

    @Test
    public void testSingularPoint() {
        final Vector2D[][] vertices = new Vector2D[][] {
            new Vector2D[] {
                new Vector2D(0.0, 0.0),
                new Vector2D(1.0, 0.0),
                new Vector2D(1.0, 1.0),
                new Vector2D(0.0, 1.0),
                new Vector2D(0.0, 0.0),
                new Vector2D(-1.0, 0.0),
                new Vector2D(-1.0, -1.0),
                new Vector2D(0.0, -1.0)
            }
        };
        final PolygonsSet set = buildSet(vertices);
        checkVertices(set.getVertices(), vertices);
    }

    @Test
    public void testLineIntersection() {
        final Vector2D[][] vertices = new Vector2D[][] {
            new Vector2D[] {
                new Vector2D(0.0, 0.0),
                new Vector2D(2.0, 0.0),
                new Vector2D(2.0, 1.0),
                new Vector2D(3.0, 1.0),
                new Vector2D(3.0, 3.0),
                new Vector2D(1.0, 3.0),
                new Vector2D(1.0, 2.0),
                new Vector2D(0.0, 2.0)
            }
        };
        final PolygonsSet set = buildSet(vertices);
        set.getClassification();

        final Line l1 = new Line(new Vector2D(-1.5, 0.0), FastMath.PI / 4);
        final SubLine s1 = (SubLine) set.intersection(l1.wholeHyperplane());
        final List<Interval> i1 = ((IntervalsSet) s1.getRemainingRegion()).asList();
        Assert.assertEquals(2, i1.size());
        final Interval v10 = i1.get(0);
        final Vector2D p10Lower = l1.toSpace(new Vector1D(v10.getInf()));
        Assert.assertEquals(0.0, p10Lower.getX(), 1.0e-10);
        Assert.assertEquals(1.5, p10Lower.getY(), 1.0e-10);
        final Vector2D p10Upper = l1.toSpace(new Vector1D(v10.getSup()));
        Assert.assertEquals(0.5, p10Upper.getX(), 1.0e-10);
        Assert.assertEquals(2.0, p10Upper.getY(), 1.0e-10);
        final Interval v11 = i1.get(1);
        final Vector2D p11Lower = l1.toSpace(new Vector1D(v11.getInf()));
        Assert.assertEquals(1.0, p11Lower.getX(), 1.0e-10);
        Assert.assertEquals(2.5, p11Lower.getY(), 1.0e-10);
        final Vector2D p11Upper = l1.toSpace(new Vector1D(v11.getSup()));
        Assert.assertEquals(1.5, p11Upper.getX(), 1.0e-10);
        Assert.assertEquals(3.0, p11Upper.getY(), 1.0e-10);

        final Line l2 = new Line(new Vector2D(-1.0, 2.0), 0);
        final SubLine s2 = (SubLine) set.intersection(l2.wholeHyperplane());
        final List<Interval> i2 = ((IntervalsSet) s2.getRemainingRegion()).asList();
        Assert.assertEquals(1, i2.size());
        final Interval v20 = i2.get(0);
        final Vector2D p20Lower = l2.toSpace(new Vector1D(v20.getInf()));
        Assert.assertEquals(1.0, p20Lower.getX(), 1.0e-10);
        Assert.assertEquals(2.0, p20Lower.getY(), 1.0e-10);
        final Vector2D p20Upper = l2.toSpace(new Vector1D(v20.getSup()));
        Assert.assertEquals(3.0, p20Upper.getX(), 1.0e-10);
        Assert.assertEquals(2.0, p20Upper.getY(), 1.0e-10);

    }

    @Test
    public void testUnlimitedSubHyperplane() {
        final Vector2D[][] vertices1 = new Vector2D[][] {
            new Vector2D[] {
                new Vector2D(0.0, 0.0),
                new Vector2D(4.0, 0.0),
                new Vector2D(1.4, 1.5),
                new Vector2D(0.0, 3.5)
            }
        };
        final PolygonsSet set1 = buildSet(vertices1);
        final Vector2D[][] vertices2 = new Vector2D[][] {
            new Vector2D[] {
                new Vector2D(1.4, 0.2),
                new Vector2D(2.8, -1.2),
                new Vector2D(2.5, 0.6)
            }
        };
        final PolygonsSet set2 = buildSet(vertices2);

        final PolygonsSet set =
            (PolygonsSet) new RegionFactory<Euclidean2D>().union(set1.copySelf(),
                set2.copySelf());
        checkVertices(set1.getVertices(), vertices1);
        checkVertices(set2.getVertices(), vertices2);
        checkVertices(set.getVertices(), new Vector2D[][] {
            new Vector2D[] {
                new Vector2D(0.0, 0.0),
                new Vector2D(1.6, 0.0),
                new Vector2D(2.8, -1.2),
                new Vector2D(2.6, 0.0),
                new Vector2D(4.0, 0.0),
                new Vector2D(1.4, 1.5),
                new Vector2D(0.0, 3.5)
            }
        });

    }

    @Test
    public void testUnion() {
        final Vector2D[][] vertices1 = new Vector2D[][] {
            new Vector2D[] {
                new Vector2D(0.0, 0.0),
                new Vector2D(2.0, 0.0),
                new Vector2D(2.0, 2.0),
                new Vector2D(0.0, 2.0)
            }
        };
        final PolygonsSet set1 = buildSet(vertices1);
        final Vector2D[][] vertices2 = new Vector2D[][] {
            new Vector2D[] {
                new Vector2D(1.0, 1.0),
                new Vector2D(3.0, 1.0),
                new Vector2D(3.0, 3.0),
                new Vector2D(1.0, 3.0)
            }
        };
        final PolygonsSet set2 = buildSet(vertices2);
        final PolygonsSet set = (PolygonsSet) new RegionFactory<Euclidean2D>().union(set1.copySelf(),
            set2.copySelf());
        checkVertices(set1.getVertices(), vertices1);
        checkVertices(set2.getVertices(), vertices2);
        checkVertices(set.getVertices(), new Vector2D[][] {
            new Vector2D[] {
                new Vector2D(0.0, 0.0),
                new Vector2D(2.0, 0.0),
                new Vector2D(2.0, 1.0),
                new Vector2D(3.0, 1.0),
                new Vector2D(3.0, 3.0),
                new Vector2D(1.0, 3.0),
                new Vector2D(1.0, 2.0),
                new Vector2D(0.0, 2.0)
            }
        });
        checkPoints(Region.Location.INSIDE, set, new Vector2D[] {
            new Vector2D(1.0, 1.0),
            new Vector2D(0.5, 0.5),
            new Vector2D(2.0, 2.0),
            new Vector2D(2.5, 2.5),
            new Vector2D(0.5, 1.5),
            new Vector2D(1.5, 1.5),
            new Vector2D(1.5, 0.5),
            new Vector2D(1.5, 2.5),
            new Vector2D(2.5, 1.5),
            new Vector2D(2.5, 2.5)
        });
        checkPoints(Region.Location.OUTSIDE, set, new Vector2D[] {
            new Vector2D(-0.5, 0.5),
            new Vector2D(0.5, 2.5),
            new Vector2D(2.5, 0.5),
            new Vector2D(3.5, 2.5)
        });
        checkPoints(Region.Location.BOUNDARY, set, new Vector2D[] {
            new Vector2D(0.0, 0.0),
            new Vector2D(0.5, 2.0),
            new Vector2D(2.0, 0.5),
            new Vector2D(2.5, 1.0),
            new Vector2D(3.0, 2.5)
        });

    }

    @Test
    public void testIntersection() {
        final Vector2D[][] vertices1 = new Vector2D[][] {
            new Vector2D[] {
                new Vector2D(0.0, 0.0),
                new Vector2D(2.0, 0.0),
                new Vector2D(2.0, 2.0),
                new Vector2D(0.0, 2.0)
            }
        };
        final PolygonsSet set1 = buildSet(vertices1);
        final Vector2D[][] vertices2 = new Vector2D[][] {
            new Vector2D[] {
                new Vector2D(1.0, 1.0),
                new Vector2D(3.0, 1.0),
                new Vector2D(3.0, 3.0),
                new Vector2D(1.0, 3.0)
            }
        };
        final PolygonsSet set2 = buildSet(vertices2);
        final PolygonsSet set = (PolygonsSet) new RegionFactory<Euclidean2D>().intersection(set1.copySelf(),
            set2.copySelf());
        checkVertices(set1.getVertices(), vertices1);
        checkVertices(set2.getVertices(), vertices2);
        checkVertices(set.getVertices(), new Vector2D[][] {
            new Vector2D[] {
                new Vector2D(1.0, 1.0),
                new Vector2D(2.0, 1.0),
                new Vector2D(2.0, 2.0),
                new Vector2D(1.0, 2.0)
            }
        });
        checkPoints(Region.Location.INSIDE, set, new Vector2D[] {
            new Vector2D(1.5, 1.5)
        });
        checkPoints(Region.Location.OUTSIDE, set, new Vector2D[] {
            new Vector2D(0.5, 1.5),
            new Vector2D(2.5, 1.5),
            new Vector2D(1.5, 0.5),
            new Vector2D(0.5, 0.5)
        });
        checkPoints(Region.Location.BOUNDARY, set, new Vector2D[] {
            new Vector2D(1.0, 1.0),
            new Vector2D(2.0, 2.0),
            new Vector2D(1.0, 1.5),
            new Vector2D(1.5, 2.0)
        });
    }

    @Test
    public void testXor() {
        final Vector2D[][] vertices1 = new Vector2D[][] {
            new Vector2D[] {
                new Vector2D(0.0, 0.0),
                new Vector2D(2.0, 0.0),
                new Vector2D(2.0, 2.0),
                new Vector2D(0.0, 2.0)
            }
        };
        final PolygonsSet set1 = buildSet(vertices1);
        final Vector2D[][] vertices2 = new Vector2D[][] {
            new Vector2D[] {
                new Vector2D(1.0, 1.0),
                new Vector2D(3.0, 1.0),
                new Vector2D(3.0, 3.0),
                new Vector2D(1.0, 3.0)
            }
        };
        final PolygonsSet set2 = buildSet(vertices2);
        final PolygonsSet set = (PolygonsSet) new RegionFactory<Euclidean2D>().xor(set1.copySelf(),
            set2.copySelf());
        checkVertices(set1.getVertices(), vertices1);
        checkVertices(set2.getVertices(), vertices2);
        checkVertices(set.getVertices(), new Vector2D[][] {
            new Vector2D[] {
                new Vector2D(0.0, 0.0),
                new Vector2D(2.0, 0.0),
                new Vector2D(2.0, 1.0),
                new Vector2D(3.0, 1.0),
                new Vector2D(3.0, 3.0),
                new Vector2D(1.0, 3.0),
                new Vector2D(1.0, 2.0),
                new Vector2D(0.0, 2.0)
            },
            new Vector2D[] {
                new Vector2D(1.0, 1.0),
                new Vector2D(1.0, 2.0),
                new Vector2D(2.0, 2.0),
                new Vector2D(2.0, 1.0)
            }
        });
        checkPoints(Region.Location.INSIDE, set, new Vector2D[] {
            new Vector2D(0.5, 0.5),
            new Vector2D(2.5, 2.5),
            new Vector2D(0.5, 1.5),
            new Vector2D(1.5, 0.5),
            new Vector2D(1.5, 2.5),
            new Vector2D(2.5, 1.5),
            new Vector2D(2.5, 2.5)
        });
        checkPoints(Region.Location.OUTSIDE, set, new Vector2D[] {
            new Vector2D(-0.5, 0.5),
            new Vector2D(0.5, 2.5),
            new Vector2D(2.5, 0.5),
            new Vector2D(1.5, 1.5),
            new Vector2D(3.5, 2.5)
        });
        checkPoints(Region.Location.BOUNDARY, set, new Vector2D[] {
            new Vector2D(1.0, 1.0),
            new Vector2D(2.0, 2.0),
            new Vector2D(1.5, 1.0),
            new Vector2D(2.0, 1.5),
            new Vector2D(0.0, 0.0),
            new Vector2D(0.5, 2.0),
            new Vector2D(2.0, 0.5),
            new Vector2D(2.5, 1.0),
            new Vector2D(3.0, 2.5)
        });
    }

    @Test
    public void testDifference() {
        final Vector2D[][] vertices1 = new Vector2D[][] {
            new Vector2D[] {
                new Vector2D(0.0, 0.0),
                new Vector2D(2.0, 0.0),
                new Vector2D(2.0, 2.0),
                new Vector2D(0.0, 2.0)
            }
        };
        final PolygonsSet set1 = buildSet(vertices1);
        final Vector2D[][] vertices2 = new Vector2D[][] {
            new Vector2D[] {
                new Vector2D(1.0, 1.0),
                new Vector2D(3.0, 1.0),
                new Vector2D(3.0, 3.0),
                new Vector2D(1.0, 3.0)
            }
        };
        final PolygonsSet set2 = buildSet(vertices2);
        final PolygonsSet set = (PolygonsSet) new RegionFactory<Euclidean2D>().difference(set1.copySelf(),
            set2.copySelf());
        checkVertices(set1.getVertices(), vertices1);
        checkVertices(set2.getVertices(), vertices2);
        checkVertices(set.getVertices(), new Vector2D[][] {
            new Vector2D[] {
                new Vector2D(0.0, 0.0),
                new Vector2D(2.0, 0.0),
                new Vector2D(2.0, 1.0),
                new Vector2D(1.0, 1.0),
                new Vector2D(1.0, 2.0),
                new Vector2D(0.0, 2.0)
            }
        });
        checkPoints(Region.Location.INSIDE, set, new Vector2D[] {
            new Vector2D(0.5, 0.5),
            new Vector2D(0.5, 1.5),
            new Vector2D(1.5, 0.5)
        });
        checkPoints(Region.Location.OUTSIDE, set, new Vector2D[] {
            new Vector2D(2.5, 2.5),
            new Vector2D(-0.5, 0.5),
            new Vector2D(0.5, 2.5),
            new Vector2D(2.5, 0.5),
            new Vector2D(1.5, 1.5),
            new Vector2D(3.5, 2.5),
            new Vector2D(1.5, 2.5),
            new Vector2D(2.5, 1.5),
            new Vector2D(2.0, 1.5),
            new Vector2D(2.0, 2.0),
            new Vector2D(2.5, 1.0),
            new Vector2D(2.5, 2.5),
            new Vector2D(3.0, 2.5)
        });
        checkPoints(Region.Location.BOUNDARY, set, new Vector2D[] {
            new Vector2D(1.0, 1.0),
            new Vector2D(1.5, 1.0),
            new Vector2D(0.0, 0.0),
            new Vector2D(0.5, 2.0),
            new Vector2D(2.0, 0.5)
        });
    }

    @Test
    public void testEmptyDifference() {
        final Vector2D[][] vertices1 = new Vector2D[][] {
            new Vector2D[] {
                new Vector2D(0.5, 3.5),
                new Vector2D(0.5, 4.5),
                new Vector2D(-0.5, 4.5),
                new Vector2D(-0.5, 3.5)
            }
        };
        final PolygonsSet set1 = buildSet(vertices1);
        final Vector2D[][] vertices2 = new Vector2D[][] {
            new Vector2D[] {
                new Vector2D(1.0, 2.0),
                new Vector2D(1.0, 8.0),
                new Vector2D(-1.0, 8.0),
                new Vector2D(-1.0, 2.0)
            }
        };
        final PolygonsSet set2 = buildSet(vertices2);
        Assert.assertTrue(new RegionFactory<Euclidean2D>().difference(set1.copySelf(), set2.copySelf()).isEmpty());
    }

    @Test
    public void testChoppedHexagon() {
        final double pi6 = FastMath.PI / 6.0;
        final double sqrt3 = MathLib.sqrt(3.0);
        final SubLine[] hyp = {
            new Line(new Vector2D(0.0, 1.0), 5 * pi6).wholeHyperplane(),
            new Line(new Vector2D(-sqrt3, 1.0), 7 * pi6).wholeHyperplane(),
            new Line(new Vector2D(-sqrt3, 1.0), 9 * pi6).wholeHyperplane(),
            new Line(new Vector2D(-sqrt3, 0.0), 11 * pi6).wholeHyperplane(),
            new Line(new Vector2D(0.0, 0.0), 13 * pi6).wholeHyperplane(),
            new Line(new Vector2D(0.0, 1.0), 3 * pi6).wholeHyperplane(),
            new Line(new Vector2D(-5.0 * sqrt3 / 6.0, 0.0), 9 * pi6).wholeHyperplane()
        };
        hyp[1] = (SubLine) hyp[1].split(hyp[0].getHyperplane()).getMinus();
        hyp[2] = (SubLine) hyp[2].split(hyp[1].getHyperplane()).getMinus();
        hyp[3] = (SubLine) hyp[3].split(hyp[2].getHyperplane()).getMinus();
        hyp[4] = (SubLine) hyp[4].split(hyp[3].getHyperplane()).getMinus().split(hyp[0].getHyperplane()).getMinus();
        hyp[5] = (SubLine) hyp[5].split(hyp[4].getHyperplane()).getMinus().split(hyp[0].getHyperplane()).getMinus();
        hyp[6] = (SubLine) hyp[6].split(hyp[3].getHyperplane()).getMinus().split(hyp[1].getHyperplane()).getMinus();
        BSPTree<Euclidean2D> tree = new BSPTree<>(Boolean.TRUE);
        for (int i = hyp.length - 1; i >= 0; --i) {
            tree = new BSPTree<>(hyp[i], new BSPTree<Euclidean2D>(Boolean.FALSE), tree, null);
        }
        final PolygonsSet set = new PolygonsSet(tree);
        final SubLine splitter =
            new Line(new Vector2D(-2.0 * sqrt3 / 3.0, 0.0), 9 * pi6).wholeHyperplane();
        final PolygonsSet slice =
            new PolygonsSet(new BSPTree<>(splitter,
                set.getTree(false).split(splitter).getPlus(),
                new BSPTree<Euclidean2D>(Boolean.FALSE), null));
        Assert.assertEquals(Region.Location.OUTSIDE,
            slice.checkPoint(new Vector2D(0.1, 0.5)));
        Assert.assertEquals(11.0 / 3.0, slice.getBoundarySize(), 1.0e-10);

    }

    @Test
    public void testConcentric() {
        final double h = MathLib.sqrt(3.0) / 2.0;
        final Vector2D[][] vertices1 = new Vector2D[][] {
            new Vector2D[] {
                new Vector2D(0.00, 0.1 * h),
                new Vector2D(0.05, 0.1 * h),
                new Vector2D(0.10, 0.2 * h),
                new Vector2D(0.05, 0.3 * h),
                new Vector2D(-0.05, 0.3 * h),
                new Vector2D(-0.10, 0.2 * h),
                new Vector2D(-0.05, 0.1 * h)
            }
        };
        final PolygonsSet set1 = buildSet(vertices1);
        final Vector2D[][] vertices2 = new Vector2D[][] {
            new Vector2D[] {
                new Vector2D(0.00, 0.0 * h),
                new Vector2D(0.10, 0.0 * h),
                new Vector2D(0.20, 0.2 * h),
                new Vector2D(0.10, 0.4 * h),
                new Vector2D(-0.10, 0.4 * h),
                new Vector2D(-0.20, 0.2 * h),
                new Vector2D(-0.10, 0.0 * h)
            }
        };
        final PolygonsSet set2 = buildSet(vertices2);
        Assert.assertTrue(set2.contains(set1));
    }

    @Test
    public void testBox() {
        final PolygonsSet box = new PolygonsSet(0, 3, 0, 3);
        Assert.assertEquals(Location.INSIDE, box.checkPoint(new Vector2D(1, 1)));
        Assert.assertEquals(Location.OUTSIDE, box.checkPoint(new Vector2D(4, 1)));
        Assert.assertEquals(MathLib.sqrt(4.5), box.getBarycenter().getNorm(), 1.0e-10);
    }

    @Test
    public void testBug20040520() {
        final BSPTree<Euclidean2D> a0 =
            new BSPTree<>(buildSegment(new Vector2D(0.85, -0.05),
                new Vector2D(0.90, -0.10)),
                new BSPTree<Euclidean2D>(Boolean.FALSE),
                new BSPTree<Euclidean2D>(Boolean.TRUE),
                null);
        final BSPTree<Euclidean2D> a1 =
            new BSPTree<>(buildSegment(new Vector2D(0.85, -0.10),
                new Vector2D(0.90, -0.10)),
                new BSPTree<Euclidean2D>(Boolean.FALSE), a0, null);
        final BSPTree<Euclidean2D> a2 =
            new BSPTree<>(buildSegment(new Vector2D(0.90, -0.05),
                new Vector2D(0.85, -0.05)),
                new BSPTree<Euclidean2D>(Boolean.FALSE), a1, null);
        final BSPTree<Euclidean2D> a3 =
            new BSPTree<>(buildSegment(new Vector2D(0.82, -0.05),
                new Vector2D(0.82, -0.08)),
                new BSPTree<Euclidean2D>(Boolean.FALSE),
                new BSPTree<Euclidean2D>(Boolean.TRUE),
                null);
        final BSPTree<Euclidean2D> a4 =
            new BSPTree<>(buildHalfLine(new Vector2D(0.85, -0.05),
                new Vector2D(0.80, -0.05),
                false),
                new BSPTree<Euclidean2D>(Boolean.FALSE), a3, null);
        final BSPTree<Euclidean2D> a5 =
            new BSPTree<>(buildSegment(new Vector2D(0.82, -0.08),
                new Vector2D(0.82, -0.18)),
                new BSPTree<Euclidean2D>(Boolean.FALSE),
                new BSPTree<Euclidean2D>(Boolean.TRUE),
                null);
        final BSPTree<Euclidean2D> a6 =
            new BSPTree<>(buildHalfLine(new Vector2D(0.82, -0.18),
                new Vector2D(0.85, -0.15),
                true),
                new BSPTree<Euclidean2D>(Boolean.FALSE), a5, null);
        final BSPTree<Euclidean2D> a7 =
            new BSPTree<>(buildHalfLine(new Vector2D(0.85, -0.05),
                new Vector2D(0.82, -0.08),
                false),
                a4, a6, null);
        final BSPTree<Euclidean2D> a8 =
            new BSPTree<>(buildLine(new Vector2D(0.85, -0.25),
                new Vector2D(0.85, 0.05)),
                a2, a7, null);
        final BSPTree<Euclidean2D> a9 =
            new BSPTree<>(buildLine(new Vector2D(0.90, 0.05),
                new Vector2D(0.90, -0.50)),
                a8, new BSPTree<Euclidean2D>(Boolean.FALSE), null);

        final BSPTree<Euclidean2D> b0 =
            new BSPTree<>(buildSegment(new Vector2D(0.92, -0.12),
                new Vector2D(0.92, -0.08)),
                new BSPTree<Euclidean2D>(Boolean.FALSE), new BSPTree<Euclidean2D>(Boolean.TRUE),
                null);
        final BSPTree<Euclidean2D> b1 =
            new BSPTree<>(buildHalfLine(new Vector2D(0.92, -0.08),
                new Vector2D(0.90, -0.10),
                true),
                new BSPTree<Euclidean2D>(Boolean.FALSE), b0, null);
        final BSPTree<Euclidean2D> b2 =
            new BSPTree<>(buildSegment(new Vector2D(0.92, -0.18),
                new Vector2D(0.92, -0.12)),
                new BSPTree<Euclidean2D>(Boolean.FALSE), new BSPTree<Euclidean2D>(Boolean.TRUE),
                null);
        final BSPTree<Euclidean2D> b3 =
            new BSPTree<>(buildSegment(new Vector2D(0.85, -0.15),
                new Vector2D(0.90, -0.20)),
                new BSPTree<Euclidean2D>(Boolean.FALSE), b2, null);
        final BSPTree<Euclidean2D> b4 =
            new BSPTree<>(buildSegment(new Vector2D(0.95, -0.15),
                new Vector2D(0.85, -0.05)),
                b1, b3, null);
        final BSPTree<Euclidean2D> b5 =
            new BSPTree<>(buildHalfLine(new Vector2D(0.85, -0.05),
                new Vector2D(0.85, -0.25),
                true),
                new BSPTree<Euclidean2D>(Boolean.FALSE), b4, null);
        final BSPTree<Euclidean2D> b6 =
            new BSPTree<>(buildLine(new Vector2D(0.0, -1.10),
                new Vector2D(1.0, -0.10)),
                new BSPTree<Euclidean2D>(Boolean.FALSE), b5, null);

        final PolygonsSet c =
            (PolygonsSet) new RegionFactory<Euclidean2D>().union(new PolygonsSet(a9),
                new PolygonsSet(b6));

        checkPoints(Region.Location.INSIDE, c, new Vector2D[] {
            new Vector2D(0.83, -0.06),
            new Vector2D(0.83, -0.15),
            new Vector2D(0.88, -0.15),
            new Vector2D(0.88, -0.09),
            new Vector2D(0.88, -0.07),
            new Vector2D(0.91, -0.18),
            new Vector2D(0.91, -0.10)
        });

        checkPoints(Region.Location.OUTSIDE, c, new Vector2D[] {
            new Vector2D(0.80, -0.10),
            new Vector2D(0.83, -0.50),
            new Vector2D(0.83, -0.20),
            new Vector2D(0.83, -0.02),
            new Vector2D(0.87, -0.50),
            new Vector2D(0.87, -0.20),
            new Vector2D(0.87, -0.02),
            new Vector2D(0.91, -0.20),
            new Vector2D(0.91, -0.08),
            new Vector2D(0.93, -0.15)
        });

        checkVertices(c.getVertices(),
            new Vector2D[][] {
            new Vector2D[] {
                new Vector2D(0.85, -0.15),
                new Vector2D(0.90, -0.20),
                new Vector2D(0.92, -0.18),
                new Vector2D(0.92, -0.08),
                new Vector2D(0.90, -0.10),
                new Vector2D(0.90, -0.05),
                new Vector2D(0.82, -0.05),
                new Vector2D(0.82, -0.18),
            }
            });

    }

    @Test
    public void testBug20041003() {

        final Line[] l = {
            new Line(new Vector2D(0.0, 0.625000007541172),
                new Vector2D(1.0, 0.625000007541172)),
            new Line(new Vector2D(-0.19204433621902645, 0.0),
                new Vector2D(-0.19204433621902645, 1.0)),
            new Line(new Vector2D(-0.40303524786887, 0.4248364535319128),
                new Vector2D(-1.12851149797877, -0.2634107480798909)),
            new Line(new Vector2D(0.0, 2.0),
                new Vector2D(1.0, 2.0))
        };

        final BSPTree<Euclidean2D> node1 =
            new BSPTree<>(new SubLine(l[0],
                new IntervalsSet(intersectionAbscissa(l[0], l[1]),
                    intersectionAbscissa(l[0], l[2]))),
                new BSPTree<Euclidean2D>(Boolean.TRUE), new BSPTree<Euclidean2D>(Boolean.FALSE),
                null);
        final BSPTree<Euclidean2D> node2 =
            new BSPTree<>(new SubLine(l[1],
                new IntervalsSet(intersectionAbscissa(l[1], l[2]),
                    intersectionAbscissa(l[1], l[3]))),
                node1, new BSPTree<Euclidean2D>(Boolean.FALSE), null);
        final BSPTree<Euclidean2D> node3 =
            new BSPTree<>(new SubLine(l[2],
                new IntervalsSet(intersectionAbscissa(l[2], l[3]),
                    Double.POSITIVE_INFINITY)),
                node2, new BSPTree<Euclidean2D>(Boolean.FALSE), null);
        final BSPTree<Euclidean2D> node4 =
            new BSPTree<>(l[3].wholeHyperplane(), node3, new BSPTree<Euclidean2D>(Boolean.FALSE), null);

        final PolygonsSet set = new PolygonsSet(node4);
        Assert.assertEquals(0, set.getVertices().length);

    }

    @Test
    public void testSqueezedHexa() {
        final PolygonsSet set = new PolygonsSet(1.0e-10,
            new Vector2D(-6, -4), new Vector2D(-8, -8), new Vector2D(8, -8),
            new Vector2D(6, -4), new Vector2D(10, 4), new Vector2D(-10, 4));
        Assert.assertEquals(Location.OUTSIDE, set.checkPoint(new Vector2D(0, 6)));
    }

    @Test
    public void testIssue880Simplified() {

        final Vector2D[] vertices1 = new Vector2D[] {
            new Vector2D(90.13595870833188, 38.33604606376991),
            new Vector2D(90.14047850603913, 38.34600084496253),
            new Vector2D(90.11045289492762, 38.36801537312368),
            new Vector2D(90.10871471476526, 38.36878044144294),
            new Vector2D(90.10424901707671, 38.374300101757),
            new Vector2D(90.0979455456843, 38.373578376172475),
            new Vector2D(90.09081227075944, 38.37526295920463),
            new Vector2D(90.09081378927135, 38.375193883266434)
        };
        final PolygonsSet set1 = new PolygonsSet(1.0e-10, vertices1);
        Assert.assertEquals(Location.OUTSIDE, set1.checkPoint(new Vector2D(90.12, 38.32)));
        Assert.assertEquals(Location.OUTSIDE, set1.checkPoint(new Vector2D(90.135, 38.355)));

    }

    @Test
    public void testIssue880Complete() {
        final Vector2D[] vertices1 = new Vector2D[] {
            new Vector2D(90.08714908223715, 38.370299337260235),
            new Vector2D(90.08709517675004, 38.3702895991413),
            new Vector2D(90.08401538704919, 38.368849330127944),
            new Vector2D(90.08258210430711, 38.367634558585564),
            new Vector2D(90.08251455106665, 38.36763409247078),
            new Vector2D(90.08106599752608, 38.36761621664249),
            new Vector2D(90.08249585300035, 38.36753627557965),
            new Vector2D(90.09075743352184, 38.35914647644972),
            new Vector2D(90.09099945896571, 38.35896264724079),
            new Vector2D(90.09269383800086, 38.34595756121246),
            new Vector2D(90.09638631543191, 38.3457988093121),
            new Vector2D(90.09666417351019, 38.34523360999418),
            new Vector2D(90.1297082145872, 38.337670454923625),
            new Vector2D(90.12971687748956, 38.337669827794684),
            new Vector2D(90.1240820219179, 38.34328502001131),
            new Vector2D(90.13084259656404, 38.34017811765017),
            new Vector2D(90.13378567942857, 38.33860579180606),
            new Vector2D(90.13519557833206, 38.33621054663689),
            new Vector2D(90.13545616732307, 38.33614965452864),
            new Vector2D(90.13553111202748, 38.33613962818305),
            new Vector2D(90.1356903436448, 38.33610227127048),
            new Vector2D(90.13576283227428, 38.33609255422783),
            new Vector2D(90.13595870833188, 38.33604606376991),
            new Vector2D(90.1361556630693, 38.3360024198866),
            new Vector2D(90.13622408795709, 38.335987048115726),
            new Vector2D(90.13696189099994, 38.33581914328681),
            new Vector2D(90.13746655304897, 38.33616706665265),
            new Vector2D(90.13845973716064, 38.33650776167099),
            new Vector2D(90.13950901827667, 38.3368469456463),
            new Vector2D(90.14393814424852, 38.337591835857495),
            new Vector2D(90.14483839716831, 38.337076122362475),
            new Vector2D(90.14565474433601, 38.33769000964429),
            new Vector2D(90.14569421179482, 38.3377117256905),
            new Vector2D(90.14577067124333, 38.33770883625908),
            new Vector2D(90.14600350631684, 38.337714326520995),
            new Vector2D(90.14600355139731, 38.33771435193319),
            new Vector2D(90.14600369112401, 38.33771443882085),
            new Vector2D(90.14600382486884, 38.33771453466096),
            new Vector2D(90.14600395205912, 38.33771463904344),
            new Vector2D(90.14600407214999, 38.337714751520764),
            new Vector2D(90.14600418462749, 38.337714871611695),
            new Vector2D(90.14600422249327, 38.337714915811034),
            new Vector2D(90.14867838361471, 38.34113888210675),
            new Vector2D(90.14923750157374, 38.341582537502575),
            new Vector2D(90.14877083250991, 38.34160685841391),
            new Vector2D(90.14816667319519, 38.34244232585684),
            new Vector2D(90.14797696744586, 38.34248455284745),
            new Vector2D(90.14484318014337, 38.34385573215269),
            new Vector2D(90.14477919958296, 38.3453797747614),
            new Vector2D(90.14202393306448, 38.34464324839456),
            new Vector2D(90.14198920640195, 38.344651155237216),
            new Vector2D(90.14155207025175, 38.34486424263724),
            new Vector2D(90.1415196143314, 38.344871730519),
            new Vector2D(90.14128611910814, 38.34500196593859),
            new Vector2D(90.14047850603913, 38.34600084496253),
            new Vector2D(90.14045907000337, 38.34601860032171),
            new Vector2D(90.14039496493928, 38.346223030432384),
            new Vector2D(90.14037626063737, 38.346240203360026),
            new Vector2D(90.14030005823724, 38.34646920000705),
            new Vector2D(90.13799164754806, 38.34903093011013),
            new Vector2D(90.11045289492762, 38.36801537312368),
            new Vector2D(90.10871471476526, 38.36878044144294),
            new Vector2D(90.10424901707671, 38.374300101757),
            new Vector2D(90.10263482039932, 38.37310041316073),
            new Vector2D(90.09834601753448, 38.373615053823414),
            new Vector2D(90.0979455456843, 38.373578376172475),
            new Vector2D(90.09086514328669, 38.37527884194668),
            new Vector2D(90.09084931407364, 38.37590801712463),
            new Vector2D(90.09081227075944, 38.37526295920463),
            new Vector2D(90.09081378927135, 38.375193883266434)
        };
        final PolygonsSet set1 = new PolygonsSet(1.0e-8, vertices1);
        Assert.assertEquals(Location.OUTSIDE, set1.checkPoint(new Vector2D(90.0905, 38.3755)));
        Assert.assertEquals(Location.INSIDE, set1.checkPoint(new Vector2D(90.09084, 38.3755)));
        Assert.assertEquals(Location.OUTSIDE, set1.checkPoint(new Vector2D(90.0913, 38.3755)));
        Assert.assertEquals(Location.INSIDE, set1.checkPoint(new Vector2D(90.1042, 38.3739)));
        Assert.assertEquals(Location.INSIDE, set1.checkPoint(new Vector2D(90.1111, 38.3673)));
        Assert.assertEquals(Location.OUTSIDE, set1.checkPoint(new Vector2D(90.0959, 38.3457)));

        final Vector2D[] vertices2 = new Vector2D[] {
            new Vector2D(90.13067558880044, 38.36977255037573),
            new Vector2D(90.12907570488, 38.36817308242706),
            new Vector2D(90.1342774136516, 38.356886880294724),
            new Vector2D(90.13090330629757, 38.34664392676211),
            new Vector2D(90.13078571364593, 38.344904617518466),
            new Vector2D(90.1315602208914, 38.3447185040846),
            new Vector2D(90.1316336226821, 38.34470643148342),
            new Vector2D(90.134020944832, 38.340936644972885),
            new Vector2D(90.13912536387306, 38.335497255122334),
            new Vector2D(90.1396178806582, 38.334878075552126),
            new Vector2D(90.14083049696671, 38.33316530644106),
            new Vector2D(90.14145252901329, 38.33152722916191),
            new Vector2D(90.1404779335565, 38.32863516047786),
            new Vector2D(90.14282712131586, 38.327504432532066),
            new Vector2D(90.14616669875488, 38.3237354115015),
            new Vector2D(90.14860976050608, 38.315714862457924),
            new Vector2D(90.14999277782437, 38.3164932507504),
            new Vector2D(90.15005207194997, 38.316534677663356),
            new Vector2D(90.15508513859612, 38.31878731691609),
            new Vector2D(90.15919938519221, 38.31852743183782),
            new Vector2D(90.16093758658837, 38.31880662005153),
            new Vector2D(90.16099420184912, 38.318825953291594),
            new Vector2D(90.1665411125756, 38.31859497874757),
            new Vector2D(90.16999653861313, 38.32505772048029),
            new Vector2D(90.17475243391698, 38.32594398441148),
            new Vector2D(90.17940844844992, 38.327427213761325),
            new Vector2D(90.20951909541378, 38.330616833491774),
            new Vector2D(90.2155400467941, 38.331746223670336),
            new Vector2D(90.21559881391778, 38.33175551425302),
            new Vector2D(90.21916646426041, 38.332584299620805),
            new Vector2D(90.23863749852285, 38.34778978875795),
            new Vector2D(90.25459855175802, 38.357790570608984),
            new Vector2D(90.25964298227257, 38.356918010203174),
            new Vector2D(90.26024593994703, 38.361692743151366),
            new Vector2D(90.26146187570015, 38.36311080550837),
            new Vector2D(90.26614159359622, 38.36510808579902),
            new Vector2D(90.26621342936448, 38.36507942500333),
            new Vector2D(90.26652190211962, 38.36494042196722),
            new Vector2D(90.26621240678867, 38.365113172030874),
            new Vector2D(90.26614057102057, 38.365141832826794),
            new Vector2D(90.26380080055299, 38.3660381760273),
            new Vector2D(90.26315345241, 38.36670658276421),
            new Vector2D(90.26251574942881, 38.367490323488084),
            new Vector2D(90.26247873448426, 38.36755266444749),
            new Vector2D(90.26234628016698, 38.36787989125406),
            new Vector2D(90.26214559424784, 38.36945909356126),
            new Vector2D(90.25861728442555, 38.37200753430875),
            new Vector2D(90.23905557537864, 38.375405314295904),
            new Vector2D(90.22517251874075, 38.38984691662256),
            new Vector2D(90.22549955153215, 38.3911564273979),
            new Vector2D(90.22434386063355, 38.391476432092134),
            new Vector2D(90.22147729457276, 38.39134652252034),
            new Vector2D(90.22142070120117, 38.391349167741964),
            new Vector2D(90.20665060751588, 38.39475580900313),
            new Vector2D(90.20042268367109, 38.39842558622888),
            new Vector2D(90.17423771242085, 38.402727751805344),
            new Vector2D(90.16756796257476, 38.40913898597597),
            new Vector2D(90.16728283954308, 38.411255399912875),
            new Vector2D(90.16703538220418, 38.41136059866693),
            new Vector2D(90.16725865657685, 38.41013618805954),
            new Vector2D(90.16746107640665, 38.40902614307544),
            new Vector2D(90.16122795307462, 38.39773101873203)
        };
        final PolygonsSet set2 = new PolygonsSet(1.0e-8, vertices2);
        final PolygonsSet set = (PolygonsSet) new
            RegionFactory<Euclidean2D>().difference(set1.copySelf(),
                set2.copySelf());

        final Vector2D[][] verticies = set.getVertices();
        Assert.assertTrue(verticies[0][0] != null);
        Assert.assertEquals(1, verticies.length);
    }

    /**
     * This TU was added to test new functionalities added due to FT-525
     * 
     * @testType UT
     * 
     * @testedMethod {@link PolygonsSet#getMaxX()}
     * @testedMethod {@link PolygonsSet#getMaxY()}
     * @testedMethod {@link PolygonsSet#getMinX()}
     * @testedMethod {@link PolygonsSet#getMinY()}
     * 
     * @description test the min/max/size/classification computation
     * 
     * @input array of vertices
     * 
     * @output polygon
     * 
     * @testPassCriteria min, max, size and classification are as expected
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testMinMaxSize() {

        // test min and max computation + remove duplicates
        final double precision = 1e-12;
        final Vector2D v1 = new Vector2D(-3., 1.);
        final Vector2D v2 = new Vector2D(-1, 5.);
        final Vector2D v3 = new Vector2D(2., 3.);
        final Vector2D v4 = new Vector2D(5., 4.);
        final Vector2D v5 = new Vector2D(5., -2.);
        final Vector2D v6 = new Vector2D(-1., -2.);
        final Vector2D v7 = new Vector2D(-1., 4.);

        // Define PolygonSet
        final Vector2D[][] arrayVertices1 = { { v1, v2, v2, v3, v4, v1 } };
        PolygonsSet polygon = new PolygonsSet(arrayVertices1);

        // test 1 : duplicates v1 and v2 should be removed + min/max + polygon Concave
        // Polygon is defined if clock wise sense : size is infinite
        final Vector2D[][] vertices = polygon.getVertices();
        Assert.assertEquals(4, vertices[0].length);
        checkVertices(vertices, arrayVertices1);
        Assert.assertEquals(EnumPolygon.CONCAVE, polygon.getClassification());
        Assert.assertEquals(-3., polygon.getMinX(), precision);
        Assert.assertEquals(5., polygon.getMaxX(), precision);
        Assert.assertEquals(1., polygon.getMinY(), precision);
        Assert.assertEquals(5., polygon.getMaxY(), precision);
        Assert.assertEquals(12.5, polygon.getSize(), precision);

        // test 2 min/max + polygon Convexe
        final Vector2D[][] arrayVertices2 = { { v7, v6, v5, v4 } };
        polygon = new PolygonsSet(arrayVertices2);
        Assert.assertEquals(EnumPolygon.CONVEX, polygon.getClassification());
        Assert.assertEquals(-1., polygon.getMinX(), precision);
        Assert.assertEquals(5., polygon.getMaxX(), precision);
        Assert.assertEquals(-2., polygon.getMinY(), precision);
        Assert.assertEquals(4., polygon.getMaxY(), precision);
        // square of 6x6 = 36
        Assert.assertEquals(36., polygon.getSize(), precision);

        // Coverage
        polygon = new PolygonsSet(arrayVertices2);
        Assert.assertEquals(-1., polygon.getMinX(), precision);
        polygon = new PolygonsSet(arrayVertices2);
        Assert.assertEquals(5., polygon.getMaxX(), precision);
        polygon = new PolygonsSet(arrayVertices2);
        Assert.assertEquals(-2., polygon.getMinY(), precision);
        polygon = new PolygonsSet(arrayVertices2);
        Assert.assertEquals(4., polygon.getMaxY(), precision);
        polygon = new PolygonsSet(arrayVertices2);
        Assert.assertEquals(36., polygon.getSize(), precision);

    }

    /**
     * This TU was added to test new functionalities added due to FT-525
     * 
     * @testType UT
     * 
     * @testedMethod {@link PolygonsSet#sortVerticies(Vector2D[][], boolean)}
     * 
     * @description test the sorting method
     * 
     * @input array of vertices
     * 
     * @output polygon
     * 
     * @testPassCriteria sorted polygon is as expected
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testSort() {
        final double precision = 1e-12;

        // // // Test case 1 // // //
        // // // // // // // // // //
        Vector2D v4 = new Vector2D(5., 4.);
        Vector2D v5 = new Vector2D(5., -2.);
        Vector2D v6 = new Vector2D(-1., -2.);
        Vector2D v7 = new Vector2D(-1., 4.);

        // test 1 : arrayVerticesBadSorted is bad sorted
        final Vector2D[][] arrayVerticesBadSorted = { { v4, v5, v7, v6 } };

        // trigo sens
        final Vector2D[][] sortedVerticesTrigo = PolygonsSet.sortVerticies(arrayVerticesBadSorted, true);
        Assert.assertEquals(v4, sortedVerticesTrigo[0][0]);
        Assert.assertEquals(v7, sortedVerticesTrigo[0][1]);
        Assert.assertEquals(v6, sortedVerticesTrigo[0][2]);
        Assert.assertEquals(v5, sortedVerticesTrigo[0][3]);
        PolygonsSet polygonSortedTrigo = new PolygonsSet(sortedVerticesTrigo);
        checkVertices(polygonSortedTrigo.getVertices(), sortedVerticesTrigo);
        Assert.assertEquals(EnumPolygon.CONVEX, polygonSortedTrigo.getClassification());
        Assert.assertEquals(-1., polygonSortedTrigo.getMinX(), precision);
        Assert.assertEquals(5., polygonSortedTrigo.getMaxX(), precision);
        Assert.assertEquals(-2., polygonSortedTrigo.getMinY(), precision);
        Assert.assertEquals(4., polygonSortedTrigo.getMaxY(), precision);
        Assert.assertEquals(36., polygonSortedTrigo.getSize(), precision);

        // clockwise sense
        final Vector2D[][] sortedVerticesClockWise = PolygonsSet.sortVerticies(arrayVerticesBadSorted, false);
        Assert.assertEquals(v5, sortedVerticesClockWise[0][0]);
        Assert.assertEquals(v6, sortedVerticesClockWise[0][1]);
        Assert.assertEquals(v7, sortedVerticesClockWise[0][2]);
        Assert.assertEquals(v4, sortedVerticesClockWise[0][3]);
        final PolygonsSet polygonSortedClockWise = new PolygonsSet(sortedVerticesClockWise);
        checkVertices(polygonSortedClockWise.getVertices(), sortedVerticesClockWise);

        // Polygons created with sortedVerticesTrigo or sortedVerticesClockWise should give the same results
        Assert.assertTrue(checkPolygonsEquality(polygonSortedTrigo, polygonSortedClockWise));

        // // // Test case 2 // // //
        // // // // // // // // // //
        final Vector2D v1 = new Vector2D(0.0, 0.0);
        final Vector2D v2 = new Vector2D(1.0, 0.0);
        final Vector2D v3 = new Vector2D(0.0, 1.0);
        v4 = new Vector2D(1.0, 1.0);

        // Array of vertices has crossing borders but sorting of polygon is done at polygon creation
        final Vector2D[][] verticesCrossingBorders = { { v1, v2, v3, v4 } };
        final PolygonsSet polygonCrossingBorders = new PolygonsSet(verticesCrossingBorders);
        checkVertices(polygonCrossingBorders.getVertices(), verticesCrossingBorders);

        // Now : sort vertices (trigo sense) : and re-create polygon
        final Vector2D[][] verticesSortedTrigo = PolygonsSet.sortVerticies(verticesCrossingBorders, true);
        polygonSortedTrigo = new PolygonsSet(verticesSortedTrigo);
        checkVertices(polygonSortedTrigo.getVertices(), verticesSortedTrigo);
        Assert.assertEquals(EnumPolygon.CONVEX, polygonSortedTrigo.getClassification());
        Assert.assertEquals(0, polygonSortedTrigo.getMinX(), precision);
        Assert.assertEquals(1, polygonSortedTrigo.getMaxX(), precision);
        Assert.assertEquals(0., polygonSortedTrigo.getMinY(), precision);
        Assert.assertEquals(1, polygonSortedTrigo.getMaxY(), precision);
        Assert.assertEquals(1., polygonSortedTrigo.getSize(), precision);

        // Now : sort vertices (clock wise sense => infinite size) : and re-create polygon
        final Vector2D[][] verticesSortedClock = PolygonsSet.sortVerticies(verticesCrossingBorders, false);
        final PolygonsSet polygonSortedClock = new PolygonsSet(verticesSortedClock);
        checkVertices(polygonSortedClock.getVertices(), verticesSortedTrigo);

        // Polygons created with sortedVerticesTrigo or sortedVerticesClockWise should give the same results
        Assert.assertTrue(checkPolygonsEquality(polygonSortedTrigo, polygonSortedClock));
        Assert.assertTrue(checkPolygonsEquality(polygonSortedTrigo, polygonCrossingBorders));

        // // // Test case 3 // // //
        // // // // // // // // // //
        v5 = new Vector2D(2.0, 1.0);
        v6 = new Vector2D(0.0, 3.0);
        v7 = new Vector2D(-2.0, 1.0);
        final Vector2D v8 = new Vector2D(2.0, -1.0);
        final Vector2D v9 = new Vector2D(-2.0, -1.0);
        final Vector2D v10 = new Vector2D(1.5, 0.0);

        // Array of vertices has crossing borders but sorting of polygon is done at polygon creation
        final Vector2D[][] verticesCrossingBorders2 = { { v5, v6, v7, v8, v9, v10 } };
        final PolygonsSet polygonCrossingBorders2 = new PolygonsSet(verticesCrossingBorders2);
        checkVertices(polygonCrossingBorders2.getVertices(), verticesCrossingBorders2);

        // Now : sort vertices (trigo sense)
        final Vector2D[][] verticesSortedTrigo2 = PolygonsSet.sortVerticies(verticesCrossingBorders2, true);
        Assert.assertEquals(v5, verticesSortedTrigo2[0][0]);
        Assert.assertEquals(v6, verticesSortedTrigo2[0][1]);
        Assert.assertEquals(v7, verticesSortedTrigo2[0][2]);
        Assert.assertEquals(v9, verticesSortedTrigo2[0][3]);
        Assert.assertEquals(v8, verticesSortedTrigo2[0][4]);
        Assert.assertEquals(v10, verticesSortedTrigo2[0][5]);
        final PolygonsSet polygonSortedTrigo2 = new PolygonsSet(verticesSortedTrigo2);
        checkVertices(polygonSortedTrigo2.getVertices(), verticesSortedTrigo2);
        Assert.assertEquals(EnumPolygon.CONCAVE, polygonSortedTrigo2.getClassification());
        Assert.assertEquals(-2, polygonSortedTrigo2.getMinX(), precision);
        Assert.assertEquals(2, polygonSortedTrigo2.getMaxX(), precision);
        Assert.assertEquals(-1., polygonSortedTrigo2.getMinY(), precision);
        Assert.assertEquals(3, polygonSortedTrigo2.getMaxY(), precision);
        Assert.assertEquals(11.5, polygonSortedTrigo2.getSize(), precision);

        // Now : sort vertices (clock wise sense)
        final Vector2D[][] verticesSortedCW = PolygonsSet.sortVerticies(verticesCrossingBorders2, false);
        Assert.assertEquals(v10, verticesSortedCW[0][0]);
        Assert.assertEquals(v8, verticesSortedCW[0][1]);
        Assert.assertEquals(v9, verticesSortedCW[0][2]);
        Assert.assertEquals(v7, verticesSortedCW[0][3]);
        Assert.assertEquals(v6, verticesSortedCW[0][4]);
        Assert.assertEquals(v5, verticesSortedCW[0][5]);
        final PolygonsSet polygonSortedCW = new PolygonsSet(verticesSortedCW);
        checkVertices(polygonSortedCW.getVertices(), verticesSortedCW);

        // Polygons created with sortedVerticesTrigo or sortedVerticesClockWise should give the same results
        Assert.assertTrue(checkPolygonsEquality(polygonSortedTrigo2, polygonSortedCW));
        Assert.assertTrue(checkPolygonsEquality(polygonSortedTrigo2, polygonCrossingBorders2));
    }

    /**
     * This TU was added to test new functionalities added due to FT-525
     * 
     * @testType UT
     * 
     * @testedMethod {@link PolygonsSet#getClassification()}
     * 
     * @description test the classification computation
     * 
     * @input array of vertices
     * 
     * @output polygon
     * 
     * @testPassCriteriaclassification is as expected
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testClassification() {

        // List of vertices
        final Vector2D v1 = new Vector2D(1, 1);
        final Vector2D v2 = new Vector2D(-1, 1);
        final Vector2D v3 = new Vector2D(-1, -1);
        final Vector2D v4 = new Vector2D(1, -1);
        final Vector2D v5 = new Vector2D(0, 0.5);
        final Vector2D v6 = new Vector2D(-0.5, 0);
        final Vector2D v7 = new Vector2D(1, -2);

        // case 1 : CONVEX
        final Vector2D[][] array1 = { { v1, v2, v3, v4 } };
        final PolygonsSet polygon1 = new PolygonsSet(array1);
        Assert.assertEquals(EnumPolygon.CONVEX, polygon1.getClassification());

        // case 2 : CONVEX
        final Vector2D[][] array2 = { { v4, v3, v2, v1 } };
        final PolygonsSet polygon2 = new PolygonsSet(array2);
        Assert.assertEquals(EnumPolygon.CONVEX, polygon2.getClassification());

        // case 3 : CONCAVE
        final Vector2D[][] array3 = { { v1, v5, v2, v3, v4 } };
        final PolygonsSet polygon3 = new PolygonsSet(array3);
        Assert.assertEquals(EnumPolygon.CONCAVE, polygon3.getClassification());

        // case 4 : CONCAVE
        final Vector2D[][] array4 = { { v1, v2, v6, v3, v4 } };
        final PolygonsSet polygon4 = new PolygonsSet(array4);
        Assert.assertEquals(EnumPolygon.CONCAVE, polygon4.getClassification());

        // case 5 : DEGENERATED
        final Vector2D[][] array5 = { { v1, v2 } };
        final PolygonsSet polygon5 = new PolygonsSet(array5);
        Assert.assertEquals(EnumPolygon.DEGENERATED, polygon5.getClassification());

        // case 6 : DEGENERATED
        final Vector2D[][] array6 = { { v4, v6 } };
        final PolygonsSet polygon6 = new PolygonsSet(array6);
        Assert.assertEquals(EnumPolygon.DEGENERATED, polygon6.getClassification());

        // case 7 : DEGENERATED
        final Vector2D[][] array7 = { { v1, v4, v7 } };
        final PolygonsSet polygon7 = new PolygonsSet(array7);
        Assert.assertEquals(EnumPolygon.DEGENERATED, polygon7.getClassification());

        // case 8 : DEGENERATED
        final Vector2D[][] array8 = { {} };
        final PolygonsSet polygon8 = new PolygonsSet(array8);
        Assert.assertEquals(EnumPolygon.DEGENERATED, polygon8.getClassification());
    }

    /**
     * This TU was added to test new functionalities added due to FT-525
     * 
     * @testType UT
     * 
     * @testedMethod {@link PolygonsSet#checkPolygonSet()}
     * 
     * @description test the checkPolygonSet method
     * 
     * @input different arrays of vertices
     * 
     * @output boolean indicating if polygon is viable
     * 
     * @testPassCriteriaclassification different behavior are expeteced :
     *                                 - if polygon is well formed : true is returned by checkPolygonSet method
     *                                 - if polygon is not well formed : an exception will be thrown
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testCheckPolygonSet() {

        // List of vertices
        final Vector2D v1 = new Vector2D(1, 1);
        final Vector2D v2 = new Vector2D(1, 2);
        final Vector2D v3 = new Vector2D(1, 3);
        final Vector2D v4 = new Vector2D(1, 2.3);
        final Vector2D v5 = new Vector2D(1, 9.6);

        final Vector2D v6 = new Vector2D(-2, -2);
        final Vector2D v7 = new Vector2D(0, -2);
        final Vector2D v8 = new Vector2D(1, -2);
        final Vector2D v9 = new Vector2D(2, -2);

        // case 1 : DEGENERATED
        final Vector2D[][] array1 = { { v1, v2, v3, v4, v5 } };
        final PolygonsSet polygon1 = new PolygonsSet(array1);
        Assert.assertEquals(EnumPolygon.DEGENERATED, polygon1.getClassification());
        try {
            polygon1.checkPolygonSet();
            Assert.fail();
        } catch (final MathIllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        // case 1 : DEGENERATED
        final Vector2D[][] array2 = { { v6, v7, v8, v9 } };
        final PolygonsSet polygon2 = new PolygonsSet(array2);
        Assert.assertEquals(EnumPolygon.DEGENERATED, polygon2.getClassification());
        try {
            polygon2.checkPolygonSet();
            Assert.fail();
        } catch (final MathIllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        // case 3 : CROSSING BORDERS
        final Vector2D[][] vertices = new Vector2D[][] {
            new Vector2D[] {
                new Vector2D(2.0, 1.0),
                new Vector2D(0.0, 3.0),
                new Vector2D(-2.0, 1.0),
                new Vector2D(2.0, -1.0),
                new Vector2D(-2.0, -1.0),
                new Vector2D(1.5, 0.0)
            }
        };
        final ArrayList<SubHyperplane<Euclidean2D>> edges = new ArrayList<>();
        for (final Vector2D[] vertice : vertices) {
            final int l = vertice.length;
            for (int j = 0; j < l; ++j) {
                edges.add(buildSegment(vertice[j], vertice[(j + 1) % l]));
            }
        }
        final PolygonsSet polygon3 = new PolygonsSet(edges);
        Assert.assertEquals(EnumPolygon.CROSSING_BORDER, polygon3.getClassification());
        try {
            polygon3.checkPolygonSet();
            Assert.fail();
        } catch (final MathIllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        // case 4 : INVALID_POLYGON_SIZE (define array of vertices in CW sense)
        final Vector2D[][] vertices4 = new Vector2D[][] {
            new Vector2D[] {
                new Vector2D(0.0, 0.0),
                new Vector2D(1.0, -1.0),
                new Vector2D(-1.0, -1.0),
            }
        };
        final ArrayList<SubHyperplane<Euclidean2D>> edges4 = new ArrayList<>();
        for (int i = 0; i < vertices.length; ++i) {
            final int l = vertices4[i].length;
            for (int j = 0; j < l; ++j) {
                edges4.add(buildSegment(vertices4[i][j], vertices4[i][(j + 1) % l]));
            }
        }
        final PolygonsSet polygon4 = new PolygonsSet(edges4);
        Assert.assertEquals(EnumPolygon.CONVEX, polygon4.getClassification());
        try {
            polygon4.checkPolygonSet();
            Assert.fail();
        } catch (final MathIllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        // case 5 : Valid polygon
        final Vector2D[][] array5 = { { v1, v8, v9 } };
        final PolygonsSet polygon5 = new PolygonsSet(array5);
        Assert.assertEquals(EnumPolygon.CONVEX, polygon5.getClassification());
        Assert.assertTrue(polygon5.checkPolygonSet());
    }

    /**
     * This TU was added to test new functionalities added due to FT-525
     * 
     * @testType UT
     * 
     * @testedMethod {@link PolygonsSet#getBiggerLength()}
     * 
     * @description test the getBiggerLength method
     * 
     * @input different arrays of vertices
     * 
     * @output the bigger length of the polygon
     * 
     * @testPassCriteriaclassification value is as expected
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testBiggerLength() {
        final Vector2D v0 = new Vector2D(4, 0);
        final Vector2D v1 = new Vector2D(2, -2);
        final Vector2D v2 = new Vector2D(0, -1);
        final Vector2D v3 = new Vector2D(-2, -2);
        final Vector2D v4 = new Vector2D(-4, 0);
        final Vector2D v5 = new Vector2D(-2, 2);
        final Vector2D v6 = new Vector2D(0, 1);
        final Vector2D v7 = new Vector2D(2, 2);
        final Vector2D[][] array1 = { { v0, v1, v2, v3, v4, v5, v6, v7 } };

        // check 1 : length = 8
        final PolygonsSet poly = new PolygonsSet(array1);
        Assert.assertTrue(poly.getBiggerLength() == 8);

        // check 2 : length = 0
        final PolygonsSet poly2 = new PolygonsSet();
        Assert.assertTrue(poly2.getBiggerLength() == 0);

        // check 3 : open loop polygon, length = Infinity
        final Vector2D[][] vertices = new Vector2D[][] {
            new Vector2D[] {
                new Vector2D(0, 0),
                new Vector2D(1, 1),
                new Vector2D(2, 2),
                new Vector2D(2, 3),
                new Vector2D(1, 3)
            }
        };
        final ArrayList<SubHyperplane<Euclidean2D>> edges = new ArrayList<>();
        for (final Vector2D[] vertice : vertices) {
            final int l = vertice.length;
            for (int j = 0; j < l - 1; ++j) {
                edges.add(buildSegment(vertice[j], vertice[(j + 1) % l]));
            }
        }
        final PolygonsSet poly3 = new PolygonsSet(edges);
        Assert.assertTrue(poly3.getBiggerLength() == Double.POSITIVE_INFINITY);
    }

    // private method to test equality of to polygons
    private static boolean checkPolygonsEquality(final PolygonsSet p1, final PolygonsSet p2) {
        final boolean sizeEq = p1.getSize() == p2.getSize() ? true : false;
        final boolean minXEq = p1.getMinX() == p2.getMinX() ? true : false;
        final boolean minYEq = p1.getMinY() == p2.getMinY() ? true : false;
        final boolean maxXEq = p1.getMaxX() == p2.getMaxX() ? true : false;
        final boolean maxYEq = p1.getMaxY() == p2.getMaxY() ? true : false;
        final boolean barycenterEq = p1.getBarycenter().subtract(p2.getBarycenter()).getNorm() == 0 ? true : false;
        final boolean biggerLengthEq = p1.getBiggerLength() == p2.getBiggerLength() ? true : false;
        final boolean classificationEq = p1.getClassification() == p2.getClassification() ? true : false;
        if (!sizeEq || !minXEq || !minYEq || !maxXEq || !maxYEq || !barycenterEq || !biggerLengthEq
            || !classificationEq) {
            return false;
        }
            return true;
    }

    private static PolygonsSet buildSet(final Vector2D[][] vertices) {
        final ArrayList<SubHyperplane<Euclidean2D>> edges = new ArrayList<>();
        for (final Vector2D[] vertice : vertices) {
            final int l = vertice.length;
            for (int j = 0; j < l; ++j) {
                edges.add(buildSegment(vertice[j], vertice[(j + 1) % l]));
            }
        }
        return new PolygonsSet(edges);
    }

    private static SubHyperplane<Euclidean2D> buildLine(final Vector2D start, final Vector2D end) {
        return new Line(start, end).wholeHyperplane();
    }

    private static double intersectionAbscissa(final Line l0, final Line l1) {
        final Vector2D p = l0.intersection(l1);
        return (l0.toSubSpace(p)).getX();
    }

    private static SubHyperplane<Euclidean2D> buildHalfLine(final Vector2D start, final Vector2D end,
                                                     final boolean startIsVirtual) {
        final Line line = new Line(start, end);
        final double lower = startIsVirtual
            ? Double.NEGATIVE_INFINITY
            : (line.toSubSpace(start)).getX();
        final double upper = startIsVirtual
            ? (line.toSubSpace(end)).getX()
            : Double.POSITIVE_INFINITY;
        return new SubLine(line, new IntervalsSet(lower, upper));
    }

    private static SubHyperplane<Euclidean2D> buildSegment(final Vector2D start, final Vector2D end) {
        final Line line = new Line(start, end);
        final double lower = (line.toSubSpace(start)).getX();
        final double upper = (line.toSubSpace(end)).getX();
        return new SubLine(line, new IntervalsSet(lower, upper));
    }

    private static void checkPoints(final Region.Location expected, final PolygonsSet set,
                             final Vector2D[] points) {
        for (final Vector2D point : points) {
            Assert.assertEquals(expected, set.checkPoint(point));
        }
    }

    private static boolean checkInSegment(final Vector2D p,
                                   final Vector2D p1, final Vector2D p2,
                                   final double tolerance) {
        final Line line = new Line(p1, p2);
        if (line.getOffset(p) < tolerance) {
            final double x = (line.toSubSpace(p)).getX();
            final double x1 = (line.toSubSpace(p1)).getX();
            final double x2 = (line.toSubSpace(p2)).getX();
            return (((x - x1) * (x - x2) <= 0.0)
                || (p1.distance(p) < tolerance)
                || (p2.distance(p) < tolerance));
        }
        return false;
    }

    private static void checkVertices(final Vector2D[][] rebuiltVertices,
                               final Vector2D[][] vertices) {

        // each rebuilt vertex should be in a segment joining two original vertices
        for (final Vector2D[] rebuiltVertice : rebuiltVertices) {
            for (int j = 0; j < rebuiltVertice.length; ++j) {
                boolean inSegment = false;
                final Vector2D p = rebuiltVertice[j];
                for (final Vector2D[] loop : vertices) {
                    final int length = loop.length;
                    for (int l = 0; (!inSegment) && (l < length); ++l) {
                        inSegment = checkInSegment(p, loop[l], loop[(l + 1) % length], 1.0e-10);
                    }
                }
                Assert.assertTrue(inSegment);
            }
        }

        // each original vertex should have a corresponding rebuilt vertex
        for (final Vector2D[] vertice : vertices) {
            for (int l = 0; l < vertice.length; ++l) {
                double min = Double.POSITIVE_INFINITY;
                for (final Vector2D[] rebuiltVertice : rebuiltVertices) {
                    for (int j = 0; j < rebuiltVertice.length; ++j) {
                        min = MathLib.min(vertice[l].distance(rebuiltVertice[j]),
                            min);
                    }
                }
                Assert.assertEquals(0.0, min, 1.0e-10);
            }
        }
    }
}
