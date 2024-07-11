/**
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
 * VERSION:4.9:DM:DM-3127:10/05/2022:[PATRIUS] Ajout de deux methodes resize a l'interface GeometricBodyShape ...
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.6:DM:DM-2544:27/01/2021:Ajouter la definition d'un corps celeste a partir d'un modele de forme 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies.mesh;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;

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
    public void constructionTest() {
        // Initialization
        final Vertex v1 = new Vertex(1, new Vector3D(0, 0, 1));
        final Vertex v2 = new Vertex(2, new Vector3D(1, 0, 1));
        final Vertex v3 = new Vertex(3, new Vector3D(0, 1, 1));
        final Vertex v4 = new Vertex(4, new Vector3D(0, 2, 1));
        final Vertex v5 = new Vertex(5, new Vector3D(1, 2, 1));
        final Vertex v6 = new Vertex(6, new Vector3D(-1, -2, -1));
        final Triangle triangle = new Triangle(v1, v2, v3);
        final Triangle triangle2 = new Triangle(v1, v2, v4);
        final Triangle triangle3 = new Triangle(v1, v5, v4);
        final Triangle triangle4 = new Triangle(v6, v5, v4);

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
    public void intersectionTest() {
        // Initialization
        final Triangle triangle = new Triangle(new Vertex(0, new Vector3D(0, 0, 0)),
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
     * @description check that the closest point between a line and a triangle is correct in any possible situation.
     * Line may be infinite or semi-finite.
     * 
     * @testPassCriteria closest point is as expected (reference: math, threshold: 0)
     * 
     * @referenceVersion 4.10
     * 
     * @nonRegressionVersion 4.10
     */
    @Test
    public void closestPointToTest() {
        // Initialization
        final Triangle triangle = new Triangle(new Vertex(0, new Vector3D(0, 0, 0)),
                new Vertex(1, new Vector3D(1, 0, 0)), new Vertex(2, new Vector3D(0, 1, 0)));
        
        /*
         * Various checks with infinite lines
         */
        // Case 1: infinite line and intersection
        final Line line1 = new Line(new Vector3D(0.25, 0.25, 0), new Vector3D(2, 3, 1));
        final Vector3D[] actual1 = triangle.closestPointTo(line1);
        final Vector3D expected1 = new Vector3D(0.25, 0.25, 0);
        Assert.assertEquals(0, actual1[0].distance(expected1), EPS);
        Assert.assertEquals(0, actual1[1].distance(expected1), EPS);

        // Case 2: infinite line and intersection on triangle perimeter
        final Line line2 = new Line(new Vector3D(1, 0, 0), new Vector3D(1, 1, 1));
        final Vector3D actual2[] = triangle.closestPointTo(line2);
        final Vector3D expected2 = new Vector3D(1, 0, 0);
        Assert.assertEquals(0, actual2[0].distance(expected2), EPS);
        Assert.assertEquals(0, actual2[1].distance(expected2), EPS);

        // Case 3: infinite line out of triangle without intersection
        final Line line3 = new Line(new Vector3D(0.75, 0.75, 0), new Vector3D(1, 1, 1));
        final Vector3D actual3[] = triangle.closestPointTo(line3);
        final Vector3D expected3 = new Vector3D(0.5, 0.5, 0);
        Assert.assertEquals(0, actual3[1].distance(expected3), EPS);
        Assert.assertEquals(0, actual3[0].distance(line3.pointAt(line3.getAbscissa(expected3))), EPS);

        // Case 4: infinite line in triangle's plane (with intersection but leading to no intersection)
        final Line line4 = new Line(new Vector3D(0.25, 0.25, 0), new Vector3D(1, 1, 0));
        final Vector3D actual4[] = triangle.closestPointTo(line4);
        final Vector3D expected4 = new Vector3D(0, 0, 0);
        Assert.assertEquals(0, actual4[0].distance(expected4), EPS);
        Assert.assertEquals(0, actual4[1].distance(expected4), EPS);

        // Case 5: infinite line in a plane parallel to triangle's plane
        final Line line5 = new Line(new Vector3D(0.25, 0.25, 1), new Vector3D(1, 1, 1));
        final Vector3D actual5[] = triangle.closestPointTo(line5);
        Assert.assertEquals(0, actual5[0].distance(new Vector3D(0, 0, 1)), EPS);
        Assert.assertEquals(0, actual5[1].distance(new Vector3D(0, 0, 0)), EPS);

        // Case 6: infinite line in plane with intersection on triangle perimeter
        final Line line6 = new Line(new Vector3D(1, 0, 0), new Vector3D(1, 1, 0));
        final Vector3D actual6[] = triangle.closestPointTo(line6);
        final Vector3D expected6 = new Vector3D(1, 0, 0);
        Assert.assertEquals(0, actual6[0].distance(expected6), EPS);
        Assert.assertEquals(0, actual6[1].distance(expected6), EPS);

        // Case 7: infinite line in plane but out of triangle (no intersection)
        final Line line7 = new Line(new Vector3D(1.1, 0, 0), new Vector3D(1.1, 1, 0));
        final Vector3D[] actual7 = triangle.closestPointTo(line7);
        final Vector3D expected7 = new Vector3D(1, 0, 0);
        Assert.assertEquals(0, actual7[1].distance(expected7), EPS);
        Assert.assertEquals(0, actual7[0].distance(new Vector3D(1.1, 0, 0)), EPS);
        
        /*
         * Test again with semi-finite lines
         */
        // Case 8: semi-finite line and intersection with high enough abscissa
        final Vector3D minAbs8 = new Vector3D(-5, -5, -5);
        final Line line8 = new Line(new Vector3D(0.25, 0.25, 0), new Vector3D(1.25, 1.25, 1), minAbs8);
        final Vector3D[] actual8 = triangle.closestPointTo(line8);
        final Vector3D expected8 = new Vector3D(0.25, 0.25, 0);
        Assert.assertEquals(0, actual8[0].distance(expected8), EPS);
        Assert.assertEquals(0, actual8[1].distance(expected8), EPS);
        
        // Case 9: semi-finite line and intersection with too low abscissa, min abscissa projection out of triangle
        final Vector3D minAbs9 = new Vector3D(3.25, 3.25, 3);
        final Line line9 = new Line(new Vector3D(0.25, 0.25, 0), new Vector3D(1.25, 1.25, 1), minAbs9);
        final Vector3D[] actual9 = triangle.closestPointTo(line9);
        final Vector3D expected9 = new Vector3D(0.5, 0.5, 0);
        Assert.assertEquals(0, actual9[0].distance(minAbs9), EPS);
        Assert.assertEquals(0, actual9[1].distance(expected9), EPS);
        
        // Case 10: semi-finite line and intersection with min abscissa belonging to triangle's plane
        final Vector3D minAbs10 = new Vector3D(0.25, 0.25, 0);
        final Line line10 = new Line(new Vector3D(0.25, 0.25, 0), new Vector3D(1.25, 1.25, 1), minAbs10);
        final Vector3D[] actual10 = triangle.closestPointTo(line10);
        final Vector3D expected10 = new Vector3D(0.25, 0.25, 0);
        Assert.assertEquals(0, actual10[0].distance(minAbs10), EPS);
        Assert.assertEquals(0, actual10[1].distance(expected10), EPS);

        // Case 11: semi-finite line and intersection with too low abscissa, min abscissa projection inside triangle
        final Vector3D minAbs11 = new Vector3D(0.25, 0.25, 1);
        final Line line11 = new Line(new Vector3D(0.25, 0.25, 0), new Vector3D(0.25, 0.25, 1), minAbs11);
        final Vector3D actual11[] = triangle.closestPointTo(line11);
        final Vector3D expected11 = new Vector3D(0.25, 0.25, 0);
        Assert.assertEquals(0, actual11[0].distance(minAbs11), EPS);
        Assert.assertEquals(0, actual11[1].distance(expected11), EPS);

        // Case 12: semi-finite line without intersection, low min abscissa (same results than infinite line)
        // In plane case
        final Vector3D minAbs12 = new Vector3D(1.1, -1, 0);
        final Line line12 = new Line(new Vector3D(1.1, 0, 0), new Vector3D(1.1, 1, 0), minAbs12);
        final Vector3D actual12[] = triangle.closestPointTo(line12);
        final Vector3D expected12 = new Vector3D(1, 0, 0);
        Assert.assertEquals(0, actual12[1].distance(expected12), EPS);
        Assert.assertEquals(0, actual12[0].distance(new Vector3D(1.1, 0, 0)), EPS);

        // Case 13: semi-finite line without intersection, min abscissa too high (results different than infinite line)
        // In plane case
        final Vector3D minAbs13 = new Vector3D(1.1, 1.1, 0);
        final Line line13 = new Line(new Vector3D(1.1, 0, 0), new Vector3D(1.1, 1, 0), minAbs13);
        final Vector3D actual13[] = triangle.closestPointTo(line13);
        final Vector3D expected13 = new Vector3D(0.5, 0.5, 0);
        Assert.assertEquals(0, actual13[1].distance(expected13), EPS);
        Assert.assertEquals(0, actual13[0].distance(minAbs13), EPS);
        
        // Case 14: semi-finite line without intersection, low min abscissa (same results than infinite line)
        // Out of plane case
        final Vector3D minAbs14 = new Vector3D(0, 0, -3);
        final Line line14 = new Line(new Vector3D(0.75, 0.75, 0), new Vector3D(1, 1, 1), minAbs14);
        final Vector3D actual14[] = triangle.closestPointTo(line14);
        final Vector3D expected14 = new Vector3D(0.5, 0.5, 0);
        Assert.assertEquals(0, actual14[1].distance(expected14), EPS);
        Assert.assertEquals(0, actual14[0].distance(line14.pointAt(line14.getAbscissa(expected14))), EPS);
        Assert.assertEquals(0, line14.pointAt(line14.getAbscissa(expected14)).distance(line3.pointAt(line3.getAbscissa(expected3))), EPS);
        
        // Case 15: semi-finite line without intersection, min abscissa too high (results different than infinite line)
        // Out of plane case
        final Vector3D minAbs15 = new Vector3D(1, 1, 1);
        final Line line15 = new Line(new Vector3D(0.75, 0.75, 0), new Vector3D(1, 1, 1), minAbs15);
        final Vector3D actual15[] = triangle.closestPointTo(line15);
        final Vector3D expected15 = new Vector3D(0.5, 0.5, 0);
        Assert.assertEquals(0, actual15[1].distance(expected15), EPS);
        Assert.assertEquals(0, actual15[0].distance(minAbs15), EPS);
        
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
    public void visibilityTest() {
        // Initialization
        final Triangle triangle = new Triangle(new Vertex(0, new Vector3D(0, 0, 0.6)),
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
