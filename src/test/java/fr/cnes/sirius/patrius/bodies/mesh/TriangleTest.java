/**
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
* VERSION:4.6:DM:DM-2544:27/01/2021:Ajouter la definition d'un corps celeste a partir d'un modele de forme 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies.mesh;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.bodies.mesh.Triangle;
import fr.cnes.sirius.patrius.bodies.mesh.Vertex;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit tests for {@link Triangle} class.
 * 
 * @author Emmanuel Bignon
 * 
 * @since 4.6
 */
public class TriangleTest {

    /** Epsilon for double comparison. */
    private static final double EPS = 1E-15;

    /**
     * @testType UT
     * 
     * @description check that intersection point between a ray and a triangle is properly performed in any possible situation.
     * 
     * @testPassCriteria intersection point is as expected (reference: math, threshold: 0)
     * 
     * @referenceVersion 4.6
     * 
     * @nonRegressionVersion 4.6
     */
    @Test
    public final void constructionTest() {
        // Initialization
        final Vertex v1 = new Vertex(1, new Vector3D(0, 0, 1));
        final Vertex v2 = new Vertex(2, new Vector3D(1, 0, 1));
        final Vertex v3 = new Vertex(3, new Vector3D(0, 1, 1));
        final Vertex v4 = new Vertex(4, new Vector3D(0, 2, 1));
        final Vertex v5 = new Vertex(5, new Vector3D(1, 2, 1));
        final Vertex v6 = new Vertex(6, new Vector3D(-1, -2, -1));
        final Triangle triangle = new Triangle(10, v1, v2, v3);
        final Triangle triangle2 = new Triangle(10, v1, v2, v4);
        final Triangle triangle3 = new Triangle(10, v1, v5, v4);
        final Triangle triangle4 = new Triangle(10, v6, v5, v4);

        // Checks on all triangles data
        Assert.assertEquals(0.5, triangle.getSurface(), EPS);
        Assert.assertEquals(0., triangle.getCenter().distance(new Vector3D(1. / 3., 1. / 3., 1)), 0.);
        Assert.assertEquals(0., triangle.getNormal().distance(Vector3D.PLUS_K), 0.);
        Assert.assertEquals(3, triangle.getVertices().length);
        Assert.assertEquals(0., triangle.getVertices()[0].getPosition().distance(v1.getPosition()), 0.);
        Assert.assertEquals(0., triangle.getVertices()[1].getPosition().distance(v2.getPosition()), 0.);
        Assert.assertEquals(0., triangle.getVertices()[2].getPosition().distance(v3.getPosition()), 0.);
        Assert.assertTrue(triangle.isVisible(Vector3D.PLUS_K.scalarMultiply(2.)));
        Assert.assertFalse(triangle.isVisible(Vector3D.MINUS_K.scalarMultiply(2.)));
        
        // Neighbors checks
        Assert.assertEquals(0, triangle.getNeighbors().size());
        triangle.addNeighbors(triangle);
        Assert.assertEquals(1, triangle.getNeighbors().size());
        
        // Same triangle: not neighbors
        Assert.assertFalse(triangle.isNeighborByVertexID(triangle));
        // 2 common vertices: neighbors
        Assert.assertTrue(triangle.isNeighborByVertexID(triangle2));
        Assert.assertTrue(triangle2.isNeighborByVertexID(triangle));
        // 1 common vertex: not neighbors
        Assert.assertFalse(triangle.isNeighborByVertexID(triangle3));
        // 0 common vertex: not neighbors
        Assert.assertFalse(triangle.isNeighborByVertexID(triangle4));
    }

    /**
     * @testType UT
     * 
     * @description check that intersection point between a ray and a triangle is properly performed in any possible situation.
     * 
     * @testPassCriteria intersection point is as expected (reference: math, threshold: 0)
     * 
     * @referenceVersion 4.6
     * 
     * @nonRegressionVersion 4.6
     */
    @Test
    public final void intersectionTest() {
        // Initialization
        final Triangle triangle = new Triangle(0, new Vertex(0, new Vector3D(0, 0, 0)),
                new Vertex(1, new Vector3D(1, 0, 0)), new Vertex(2, new Vector3D(0, 1, 0)));
        
        // Various checks
        
        // Basic case: intersection
        final Line line1 = new Line(new Vector3D(0.25, 0.25, 0), new Vector3D(0, 0, 1));
        final Vector3D actual1 = triangle.getIntersection(line1);
        final Vector3D expected1 = new Vector3D(0.25, 0.25, 0);
        Assert.assertEquals(0, actual1.distance(expected1), EPS);

        // Basic case2: intersection
        final Line line2 = new Line(new Vector3D(0.25, 0.25, 0), new Vector3D(1, 1, 1));
        final Vector3D actual2 = triangle.getIntersection(line2);
        final Vector3D expected2 = new Vector3D(0.25, 0.25, 0);
        Assert.assertEquals(0, actual2.distance(expected2), EPS);

        // Case of triangle perimeter: intersection
        final Line line3 = new Line(new Vector3D(1, 0, 0), new Vector3D(1, 1, 1));
        final Vector3D actual3 = triangle.getIntersection(line3);
        final Vector3D expected3 = new Vector3D(1, 0, 0);
        Assert.assertEquals(0, actual3.distance(expected3), EPS);

        // Case out of triangle: no intersection
        final Line line4 = new Line(new Vector3D(-0.1, 0, 0), new Vector3D(1, 1, 1));
        final Vector3D actual4 = triangle.getIntersection(line4);
        Assert.assertNull(actual4);

        // Case in plane with intersection: no intersection
        final Line line5 = new Line(new Vector3D(0.25, 0.25, 0), new Vector3D(1, 1, 0));
        final Vector3D actual5 = triangle.getIntersection(line5);
        Assert.assertNull(actual5);

        // Case in plane with intersection on triangle perimeter: no intersection
        final Line line6 = new Line(new Vector3D(1, 0, 0), new Vector3D(1, 1, 0));
        final Vector3D actual6 = triangle.getIntersection(line6);
        Assert.assertNull(actual6);

        // Case out of triangle and in plane: no intersection
        final Line line7 = new Line(new Vector3D(1.1, 0, 0), new Vector3D(1, 1, 0));
        final Vector3D actual7 = triangle.getIntersection(line7);
        Assert.assertNull(actual7);
    }

    /**
     * @testType UT
     * 
     * @description check that visibility is properly detected in any situation.
     * 
     * @testPassCriteria visibility status is as expected (reference: math, threshold: 0)
     * 
     * @referenceVersion 4.6
     * 
     * @nonRegressionVersion 4.6
     */
    @Test
    public final void visibilityTest() {
        // Initialization
        final Triangle triangle = new Triangle(0, new Vertex(0, new Vector3D(0, 0, 0.6)),
                new Vertex(1, new Vector3D(3, 0, 0)), new Vertex(2, new Vector3D(0, 3, 0)));
        
        // Various checks
        
        // Visible case
        Assert.assertTrue(triangle.isVisible(new Vector3D(0, 0, 10)));
        
        // Not visible case
        Assert.assertFalse(triangle.isVisible(new Vector3D(0, 0, -10)));
        
        // On the edge: not visible case
        Assert.assertFalse(triangle.isVisible(Vector3D.PLUS_I.scalarMultiply(3)));

    }
}
