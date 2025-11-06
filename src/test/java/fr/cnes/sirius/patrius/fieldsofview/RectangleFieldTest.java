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
 * 
 * @history creation 17/04/2012
 *
 * HISTORY
 * VERSION:4.16:OPENFD-485:25/04/2025:Methode intesectUSide de RectangleField mal orthographiee
 * VERSION:4.15:OPENFD-385:21/11/2024:Execution en parallele des tests concernant EclipticJ2000Provider
 * VERSION:4.14.1:OPENFD-396:10/09/2024:[PATRIUS] Erreurs et oublis dans les classes issues de IGeometricFieldOfView
 * VERSION:4.14:OPENFD-173:22/08/2024: Ajout d'une nouvelle interface IGeometricaFieldOfView
 * VERSION:4.14:OPENFD-311:22/08/2024: [PATRIUS] getInputCoord sur EllipsoidPoint
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3154:10/05/2022:[PATRIUS] Amelioration des methodes permettant l'extraction d'une sous-sequence 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2472:27/05/2020:Ajout d'un getter de sideAxis aux classes RectangleField et PyramidalField
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:217:10/03/2014:Corrected erroneous initialization of base vectors
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.fieldsofview;

import static org.junit.Assert.assertEquals;
import fr.cnes.sirius.patrius.Utils;
import static org.junit.Assert.assertFalse;
import fr.cnes.sirius.patrius.Utils;
import static org.junit.Assert.assertThrows;
import fr.cnes.sirius.patrius.Utils;
import static org.junit.Assert.assertTrue;
import fr.cnes.sirius.patrius.Utils;
import static org.junit.Assert.fail;
import fr.cnes.sirius.patrius.Utils;

import org.junit.Assert;
import fr.cnes.sirius.patrius.Utils;
import org.junit.Before;
import fr.cnes.sirius.patrius.Utils;
import org.junit.Test;
import fr.cnes.sirius.patrius.Utils;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.Utils;

/**
 * @description
 *              <p>
 *              Test class for the rectangle field of view
 *              </p>
 * 
 * @see RectangleField
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public class RectangleFieldTest {

    /** Angular distance type : DIRECTIONAL */
    private static final AngularDistanceType DIRECTIONAL = AngularDistanceType.DIRECTIONAL;
    /** PI */
    private static final double PI = Math.PI;

    /** Test default tolerance */
    private static final double TOL = Precision.DOUBLE_COMPARISON_EPSILON;

    /** Features description. */
    enum features {
        /**
         * @featureTitle Rectangle field of view
         * 
         * @featureDescription Rectangle field of view to be used in sensors description
         * 
         * @coveredRequirements DV-VEHICULE_190, DV-VEHICULE_200, DV-VEHICULE_220,
         *                      DV-VEHICULE_230, DV-VEHICULE_240, DV-VEHICULE_250
         */
        RECTANGLE_FIELD
    }

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    /**
     * FA-3110: specific case with U-direction perpendicular to facet and direction exactly lying exactly on FOV
     * pyramidal facet.
     */
    @Test
    public void perpRectFieldTest() {
        final String name = "rectangleField";
        final Vector3D mainDirection = new Vector3D(1.0, 0.0, 0.0);
        final Vector3D approximationU = new Vector3D(0.0, 1.0, 0.0);

        // Instantiation of the RectangleField
        final RectangleField rectField = new RectangleField(name, mainDirection,
            approximationU, MathLib.toRadians(45.0), MathLib.toRadians(15.0));

        final Vector3D direction = new Vector3D(1.0, 1.0, 0.0);
        final double angDist = rectField.getAngularDistance(direction);
        assertEquals(0., angDist, 0.);
    }

    /**
     * FA-3110: projection on field faces is larger than distance to vectors defining the field.
     * This occurs for very small FOV (1E-10 rad)
     */
    @Test
    public void rectFieldEpsTest() {
        // epsilon is 1e-10
        final double epsilon = 1e-10;
        final Vector3D mainDirection = new Vector3D(1.0, 0.0, 0.0);
        final Vector3D approximationU = new Vector3D(0.0, 1.0, 0.0);
        final RectangleField rectField = new RectangleField("", mainDirection, approximationU, epsilon, epsilon);

        // Test both cases
        final Vector3D direction1 = new Vector3D(0.8, 0.0, 1.0);
        assertFalse(rectField.isInTheField(direction1));

        final Vector3D direction2 = new Vector3D(0.8, 1.0, 0.0);
        assertFalse(rectField.isInTheField(direction2));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#RECTANGLE_FIELD}
     * 
     * @testedMethod {@link RectangleField#RectangleField(String, Vector3D, Vector3D, double, double)}
     * @testedMethod {@link RectangleField#getU()}
     * @testedMethod {@link RectangleField#getV()}
     * @testedMethod {@link RectangleField#getW()}
     * 
     * @description test of the constructor for erroneous initialisation
     * 
     * @input mainDirection = (1,0,1) and approximativeU = (1,0,0)
     * 
     * @output u = (1, 0, -1) / sqrt(2)
     * 
     * @testPassCriteria U is correctly calculated (erroneous case was u = (0, 0, -1))
     * 
     * @referenceVersion 2.2
     * 
     * @nonRegressionVersion 2.2
     */
    @Test
    public void testCorrectedConstructor() {

        final Vector3D mainDirection = new Vector3D(1, 0, 1);
        final Vector3D approximativeU = new Vector3D(1, 0, 0);

        final RectangleField field = new RectangleField("rect", mainDirection, approximativeU, .5, .4);

        // not the old result
        assertFalse(new Vector3D(0, 0, -1).equals(field.getW()));

        // a correct result!
        // normalize
        final Vector3D mainDir = mainDirection.normalize();
        // compute parallel component of U
        final Vector3D mainDirComponentOfU = new Vector3D(Vector3D.dotProduct(mainDir, approximativeU), mainDir);
        // subtract it and normalize result
        final Vector3D correctedU = approximativeU.subtract(mainDirComponentOfU).normalize();

        // the new result!
        assertTrue(correctedU.equals(field.getU()));

        // test the other base vectors
        assertTrue(mainDir.equals(field.getW()));
        assertTrue(Vector3D.crossProduct(mainDir, correctedU).equals(field.getV()));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#RECTANGLE_FIELD}
     * 
     * @testedMethod {@link RectangleField#getAngularDistance(Vector3D)}
     * @testedMethod {@link RectangleField#isInTheField(Vector3D)}
     * @testedMethod {@link RectangleField#getName()}
     * 
     * @description test of the basic methods of a rectangle field of view
     * 
     * @input a rectangle field of view, some vectors
     * 
     * @output angular distances
     * 
     * @testPassCriteria the angular distances are right, with the expected signs (positive
     *                   if the vector is n the field)
     * 
     * @referenceVersion 4.5
     * 
     * @nonRegressionVersion 4.5
     */
    @Test
    public void rectangleField() {

        final String name = "rectangleField";

        final Vector3D mainDirection = Vector3D.PLUS_K;
        final Vector3D uVector = new Vector3D(0.5, 0.0, 0.5);

        // tests with wrong angular aperture
        try {
            new RectangleField(name, mainDirection, uVector, 0.5, -0.1);
            fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        try {
            new RectangleField(name, mainDirection, uVector, 3.7, 1.0);
            fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        try {
            new RectangleField(name, mainDirection, uVector, -0.5, 1.2);
            fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        try {
            new RectangleField(name, mainDirection, uVector, 0.5, 4.0);
            fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        try {
            new RectangleField(name, mainDirection, uVector,
                FastMath.PI / 4.0, 3.0 * FastMath.PI / 4.0);
            fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        try {
            new RectangleField(name, mainDirection, uVector,
                3.0 * FastMath.PI / 4.0, FastMath.PI / 4.0);
            fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }

        // test with wrong direction vector
        try {
            new RectangleField(name, Vector3D.ZERO, uVector, 0.5, 0.5);
            fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }

        // tests with wrong U vector
        try {
            new RectangleField(name, mainDirection, Vector3D.ZERO, 0.5, 0.5);
            fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        try {
            new RectangleField(name, mainDirection, mainDirection, 0.5, 0.5);
            fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }

        // tests with a right field
        RectangleField field = new RectangleField(name, mainDirection, uVector,
            FastMath.PI / 4.0, FastMath.PI / 8.0);

        // test with a vector in the field
        Vector3D testedDirection = new Vector3D(1.0, 0.0, 2.0);

        assertTrue(field.isInTheField(testedDirection));
        assertEquals(field.getAngularDistance(testedDirection),
            FastMath.PI / 4.0 - MathLib.atan2(1.0, 2.0), this.comparisonEpsilon);

        // test with a vector out of the field
        testedDirection = new Vector3D(0.0, 2.0, 1.0);

        assertTrue(!field.isInTheField(testedDirection));
        assertEquals(field.getAngularDistance(testedDirection),
            -FastMath.PI / 4.0 + MathLib.atan2(1.0, 2.0) - FastMath.PI / 8.0, this.comparisonEpsilon);

        // test with a vector closest to an edge, outside
        field = new RectangleField(name, mainDirection, uVector,
            FastMath.PI / 4.0, FastMath.PI / 4.0);

        testedDirection = new Vector3D(2.0, 2.0, 0.0);
        assertTrue(!field.isInTheField(testedDirection));

        assertEquals(name, field.getName());

        // test with reversed cone, and a vector closest to an edge, inside
        field = new RectangleField(name, mainDirection, uVector,
            3.0 * FastMath.PI / 4.0, 3.0 * FastMath.PI / 4.0);

        testedDirection = new Vector3D(2.0, 2.0, 0.0);
        assertTrue(field.isInTheField(testedDirection));

        assertEquals(name, field.getName());

        // test with zero direction
        testedDirection = Vector3D.ZERO;
        assertEquals(field.getAngularDistance(testedDirection),
            0.0, this.comparisonEpsilon);

        // Test side axis (reference: math)
        final RectangleField sideField =
            new RectangleField("", Vector3D.PLUS_I, Vector3D.PLUS_J, FastMath.PI / 2., FastMath.PI / 2.);
        final Vector3D[] sideAxis = sideField.getSideAxis();
        final double sqrt2Over2 = MathLib.sqrt(2.) / 2.;
        assertEquals(0., sideAxis[0].distance(new Vector3D(0, sqrt2Over2, sqrt2Over2)), 1E-16);
        assertEquals(0., sideAxis[1].distance(new Vector3D(0, -sqrt2Over2, sqrt2Over2)), 1E-16);
        assertEquals(0., sideAxis[2].distance(new Vector3D(0, -sqrt2Over2, -sqrt2Over2)), 1E-16);
        assertEquals(0., sideAxis[3].distance(new Vector3D(0, sqrt2Over2, -sqrt2Over2)), 1E-16);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#RECTANGLE_FIELD}
     * 
     * @testedMethod {@link RectangleField#getAngularDistance(Vector3D, AngularDistanceType)}
     * 
     * 
     * @objective Ensure that the angular distance, considering the {@link AngularDistanceType#DIRECTIONAL} method
     *            provides the correct values. In this unit test, elementary directions are tested.
     *            To be sure that results do not depend on the choice of the axis U and W, we run the same test case
     *            twice considering a simple case (U, W equals to I, K axes of the canonic basis) and a more realistic
     *            one.
     *
     * @description For each of the two cases, the following directions are tested:
     *              <ul>
     *              <li>Directions parallel to FOV axes U, V</li>
     *              <li>The four FOV diagonals U, V</li>
     *              </ul>
     * 
     * @testPassCriteria the angular distances are correct, with the expected signs (positive
     *                   if the vector is n the field)
     * 
     * @referenceVersion 4.14
     * 
     * @nonRegressionVersion 4.14
     * 
     * @throws PatriusException
     *         if the eclipse computer fails
     * 
     */
    @Test
    public void testDirectionalDistance() throws PatriusException {

        final String fovName = "FOV_Name";
        Vector3D mainDir = Vector3D.PLUS_K;

        // Large Opening
        Vector3D uDirection = Vector3D.PLUS_I;
        double uOpening = Math.toRadians(85);

        // Small Opening
        final Vector3D vDirection = Vector3D.PLUS_J;
        double vOpening = Math.toRadians(80);

        // Create field
        RectangleField field = new RectangleField(fovName, mainDir, uDirection, uOpening, vOpening);

        // Assert that the angular distance along the FOV axes is 0;
        for (final Vector3D ax : field.getSideAxis()) {
            assertEquals(0., field.getAngularDistance(ax, DIRECTIONAL), TOL);

        }

        // Test Over the Field directions +U, +V, -U, -V
        // The distance should be equal to PI/2 minus the opening in that direction
        Vector3D testDir = uDirection;
        assertEquals(uOpening - Math.PI / 2, field.getAngularDistance(testDir, DIRECTIONAL), TOL);
        testDir = vDirection;
        assertEquals(vOpening - Math.PI / 2, field.getAngularDistance(testDir, DIRECTIONAL), TOL);
        testDir = uDirection.negate();
        assertEquals(uOpening - Math.PI / 2, field.getAngularDistance(testDir, DIRECTIONAL), TOL);
        testDir = vDirection.negate();
        assertEquals(vOpening - Math.PI / 2, field.getAngularDistance(testDir, DIRECTIONAL), TOL);

        // Test over linear combination of one axis + Main direction
        // The distance should be equal to PI/2 minus the angle between the test direction and the mainDir
        testDir = uDirection.add(mainDir);
        assertEquals(uOpening - Vector3D.angle(mainDir, testDir), field.getAngularDistance(testDir, DIRECTIONAL), TOL);
        testDir = vDirection.add(mainDir);
        assertEquals(vOpening - Vector3D.angle(mainDir, testDir), field.getAngularDistance(testDir, DIRECTIONAL), TOL);
        testDir = uDirection.add(mainDir).negate();
        assertEquals(uOpening - Vector3D.angle(mainDir, testDir), field.getAngularDistance(testDir, DIRECTIONAL), TOL);
        testDir = vDirection.add(mainDir).negate();
        assertEquals(vOpening - Vector3D.angle(mainDir, testDir), field.getAngularDistance(testDir, DIRECTIONAL), TOL);

        // Second case: define a new FOV with random directions for mainDir and U.
        mainDir = new Vector3D(1.0, 2.6, 9.8);
        uDirection = new Vector3D(0.48, 7.63, 6.21);
        uOpening = Math.toRadians(15);
        vOpening = Math.toRadians(30);

        // Create field
        field = new RectangleField(fovName, mainDir, uDirection, uOpening, vOpening);

        // Assert that the angular distance along the FOV axes is 0;
        for (final Vector3D ax : field.getSideAxis()) {
            assertEquals(0., field.getAngularDistance(ax, DIRECTIONAL), TOL);

        }

        // Test Over the Field directions +U, +V, -U, -V
        testDir = field.getU();
        assertEquals(uOpening - Math.PI / 2, field.getAngularDistance(testDir, DIRECTIONAL), TOL);
        testDir = field.getV();
        assertEquals(vOpening - Math.PI / 2, field.getAngularDistance(testDir, DIRECTIONAL), TOL);
        testDir = field.getU().negate();
        assertEquals(uOpening - Math.PI / 2, field.getAngularDistance(testDir, DIRECTIONAL), TOL);
        testDir = field.getV().negate();
        assertEquals(vOpening - Math.PI / 2, field.getAngularDistance(testDir, DIRECTIONAL), TOL);

        // Test over linear combination of one axis + Main direction
        testDir = field.getU().add(mainDir);
        assertEquals(uOpening - Vector3D.angle(mainDir, testDir), field.getAngularDistance(testDir, DIRECTIONAL), TOL);
        testDir = field.getV().add(mainDir);
        assertEquals(vOpening - Vector3D.angle(mainDir, testDir), field.getAngularDistance(testDir, DIRECTIONAL), TOL);
        testDir = field.getU().add(mainDir).negate();
        assertEquals(uOpening - Vector3D.angle(mainDir, testDir), field.getAngularDistance(testDir, DIRECTIONAL), TOL);
        testDir = field.getV().add(mainDir).negate();
        assertEquals(vOpening - Vector3D.angle(mainDir, testDir), field.getAngularDistance(testDir, DIRECTIONAL), TOL);

    }

    /**
     * @testType UT
     * 
     * 
     * @testedMethod {@link RectangleField#getAngularDistance(Vector3D, AngularDistanceType)}
     * 
     * 
     * @objective Ensure that the angular distance, considering the {@link AngularDistanceType#DIRECTIONAL} method
     *            provides the correct values. In this unit test, all the possible directions are tested.
     * 
     * @description We create three cones:
     *              <ul>
     *              <li>A first cone, internal to the FOV and centered on the FOV's main direction</li>
     *              <li>A second cone, external to the FOV and centered on the FOV's main direction</li>
     *              <li>A third cone, external to the FOV and centered in the opposite direction of the FOV's main
     *              direction</li>
     *              </ul>
     * 
     *              For each of the three cones, we create test directions belonging to the surface of the cone, with a
     *              delta of 5 degrees. Then we compute the angular distance over that direction and test if the result
     *              is consistent.
     * 
     * @testPassCriteria the computed angular distance is such that the following relations are respected:
     *                   <ul>
     *                   <li>tanU = tanW/cos(theta)</li> if the test direction intersects the small side of the
     *                   rectangular field
     *                   <li>tanV = tanW/sin(theta)</li> if the test direction intersects the large side of the
     *                   rectangular field
     *                   </ul>
     * 
     * @referenceVersion 4.14
     * 
     * @nonRegressionVersion 4.14
     * 
     * @throws PatriusException
     *         if the eclipse computer fails
     * 
     */
    @Test
    public void testDirectionalDistance2() throws PatriusException {

        final String fovName = "FOV_Name";
        final Vector3D mainDir = new Vector3D(1.0, 2.6, 9.8);
        final Vector3D uDirection = new Vector3D(0.48, 7.63, 6.21);
        final double uOpening = Math.toRadians(15);
        final double vOpening = Math.toRadians(30);

        // Define the angle step on theta for this test
        final double dTheta = Math.toRadians(5.);

        // Create field
        final RectangleField field = new RectangleField(fovName, mainDir, uDirection, uOpening, vOpening);

        final double tanU = Math.tan(uOpening);
        final double tanV = Math.tan(vOpening);

        // IMPLEMENTATION NOTE
        // Within the context of this test, it is not demonstrated that the formula to compute diagOpening is correct.
        // In other words, we use the same formula here in the test and within the main class.
        // To ensure that the calculation is correct, a dedicated test method was implemented (testIntersectUSide)
        final double diagOpening = Math.atan2(tanV, tanU);

        // FIRST CASE: The testDirections belong to a cone internal to the FOV
        double theta = 0.;
        while (theta < (2 * Math.PI)) {
            final double coneRadius = mainDir.getNorm() * Math.sin(vOpening) * 0.8;

            // Compute the direction on the cone's surface
            final Vector3D testDir = this.computeTestDir(mainDir, field.getU(), field.getV(), theta, coneRadius);

            final double distance = field.getAngularDistance(testDir, DIRECTIONAL);
            final double opening = distance + Vector3D.angle(testDir, field.getMainDirection());

            // Check if the relation is respected
            if (theta < diagOpening || (theta > PI - diagOpening && theta < PI + diagOpening)
                    || theta > 2 * PI - diagOpening) {
                assertEquals(Math.tan(uOpening), Math.tan(opening) * Math.abs(Math.cos(theta)), TOL);
            } else {
                assertEquals(Math.tan(vOpening), Math.tan(opening) * Math.abs(Math.sin(theta)), TOL);
            }
            theta += dTheta;
        }

        // SECOND CASE: The testDirections belong to a cone external to the FOV
        theta = 0.;
        while (theta < (2 * Math.PI)) {
            final double coneRadius = mainDir.getNorm() * Math.sin(vOpening) * 1.2;

            // Compute the direction on the cone's surface
            final Vector3D testDir = this.computeTestDir(mainDir, field.getU(), field.getV(), theta, coneRadius);
            final double distance = field.getAngularDistance(testDir, DIRECTIONAL);
            final double opening = distance + Vector3D.angle(testDir, field.getMainDirection());

            if (theta < diagOpening || (theta > PI - diagOpening && theta < PI + diagOpening)
                    || theta > 2 * PI - diagOpening) {
                assertEquals(Math.tan(uOpening), Math.tan(opening) * Math.abs(Math.cos(theta)), TOL);
            } else {
                assertEquals(Math.tan(vOpening), Math.tan(opening) * Math.abs(Math.sin(theta)), TOL);
            }
            theta += dTheta;
        }

        // THIRD CASE: The testDirections belong to a cone external to the FOV centered on -mainDir
        theta = 0.;
        while (theta < (2 * Math.PI)) {
            final double coneRadius = mainDir.getNorm() * Math.sin(vOpening) * 1.2;
            final Vector3D testDir =
                this.computeTestDir(mainDir.negate(), field.getU(), field.getV(), theta, coneRadius);

            // Compute the direction on the cone's surface
            final double distance = field.getAngularDistance(testDir, DIRECTIONAL);
            final double opening = distance + Vector3D.angle(testDir, field.getMainDirection());
            if (theta < diagOpening || (theta > PI - diagOpening && theta < PI + diagOpening)
                    || theta > 2 * PI - diagOpening) {
                assertEquals(Math.tan(uOpening), Math.tan(opening) * Math.abs(Math.cos(theta)), TOL);
            } else {
                assertEquals(Math.tan(vOpening), Math.tan(opening) * Math.abs(Math.sin(theta)), TOL);
            }
            theta += dTheta;
        }
    }

    /**
     * 
     * @testType UT
     * 
     * 
     * @testedMethod {@link RectangleField#intersectUSide(double)}
     * 
     * 
     * @objective Ensure that the method
     *            {@link RectangleField#intersectUSide(double)} is correct. In particular, this test aims at validating
     *            the calculation diagOpening = Math.atan2(tanV, tanU) without explicitly referencing at it. This test
     *            is an extension of {@link RectangleFieldTest#testDirectionalDistance2()}
     * 
     * @description A square field of view is created. It must be ensured that the method returns true only when theta
     *              intersect the U direction, that is:
     *              <ul>
     *              <li>[-45; 45]deg</li>
     *              <li>[135; 180]deg</li>
     *              <li>[-180; -135deg
     *              </ul>
     * 
     * @testPassCriteria Given the four values of diagonal angles (&#177 45deg, &#177 135deg), we test values slightly
     *                   smaller and bigger to ensure that the computation is consistent with what is expected.
     * 
     * @referenceVersion 4.14
     * 
     * @nonRegressionVersion 4.14
     * 
     * @throws PatriusException if the eclipse computer fails
     */
    @Test
    public void testIntersectUSide() throws PatriusException {

        final String fovName = "FOV_Name";
        final Vector3D mainDir = new Vector3D(1.0, 2.6, 9.8);
        final Vector3D uDirection = mainDir.orthogonal();

        final double uOpening = Math.toRadians(45);
        final double vOpening = Math.toRadians(45);

        // Create field
        final RectangleField field = new RectangleField(fovName, mainDir, uDirection, uOpening, vOpening);

        // Test in proximity of the first quadrant's diagonal
        assertTrue(field.intersectUSide(Math.toRadians(44.99)));
        assertTrue(!field.intersectUSide(Math.toRadians(45.01)));

        // Test in proximity of the second quadrant's diagonal
        assertTrue(!field.intersectUSide(Math.toRadians(134.99)));
        assertTrue(field.intersectUSide(Math.toRadians(135.01)));

        // Test in proximity of the third quadrant's diagonal
        assertTrue(!field.intersectUSide(Math.toRadians(-134.99)));
        assertTrue(field.intersectUSide(Math.toRadians(-135.01)));

        // Test in proximity of the fourth quadrant's diagonal
        assertTrue(!field.intersectUSide(Math.toRadians(-45.01)));
        assertTrue(field.intersectUSide(Math.toRadians(-44.99)));

    }

    /**
     * @testType UT
     * 
     * 
     * @testedMethod {@link RectangleField#getAngularDistance(Vector3D, AngularDistanceType)}
     * 
     * 
     * @objective Ensure that the angular distance, considering the {@link AngularDistanceType#DIRECTIONAL} method
     *            provides the correct values.
     *            In this unit test, we verify that the angular opening is the same for specific directions over the
     *            same plane.
     * 
     * @description The following scenario is build:
     *              <ul>
     *              <li>A rectangular field is created from a two random vectors (not parallel)</li>
     *              <li>the rectangular field's main direction W and a second direction Y, orthogonal to W, define a
     *              plane containing both the directions</li>
     *              <li>A series of test directions, belonging to this plane, are build as a linear combination of W and
     *              Y</li>
     *              <li>The angular opening for each of these directions is computed and compared with a reference
     *              value</li>
     *              </ul>
     * 
     * 
     * @testPassCriteria the angular opening computed over each of the test directions has the same value
     * 
     * @referenceVersion 4.14
     * 
     * @nonRegressionVersion 4.14
     * 
     * @throws PatriusException
     *         if the eclipse computer fails
     * 
     */
    @Test
    public void testDirectionalDistance3() throws PatriusException {

        final String fovName = "FOV_Name";
        final Vector3D mainDir = new Vector3D(1.0, 2.6, 9.8);
        final Vector3D uDirection = new Vector3D(0.48, 7.63, 6.21);
        final double uOpening = Math.toRadians(15);
        final double vOpening = Math.toRadians(30);

        // Define the angle step on theta for this test
        final double dTheta = Math.toRadians(5.);

        // Create field
        final RectangleField field = new RectangleField(fovName, mainDir, uDirection, uOpening, vOpening);

        // Let's now define two orthogonal directions that define a plane containing W.
        // This two directions will be used to create test direction using the following expression:
        // dirTest = dirX * cos(theta) + dirY * sin(theta)
        final Vector3D dirW = field.getW().normalize();
        final Vector3D dirY = field.getW().orthogonal().normalize();

        // Compute a reference opening value
        final double refOpening =
            field.getAngularDistance(dirY, DIRECTIONAL) + Vector3D.angle(dirY, field.getMainDirection());

        // Theta initially equal to 1deg because theta= 0 correspond to W direction
        // The angular opening computed along W is a particular case that we are not interested to test now
        double theta = Math.toRadians(1.);
        while (theta < 2 * Math.PI) {

            final Vector3D dirTheta = dirW.scalarMultiply(Math.cos(theta)).add(dirY.scalarMultiply(Math.sin(theta)));
            final double distanceTheta = field.getAngularDistance(dirTheta, DIRECTIONAL);
            final double openingTheta = distanceTheta + Vector3D.angle(dirTheta, field.getMainDirection());
            assertEquals(refOpening, openingTheta, TOL);
            theta += dTheta;
        }
    }

    /**
     * @testType UT
     * 
     * 
     * @testedMethod {@link RectangleField#getAngularDistance(Vector3D, AngularDistanceType)}
     * 
     * 
     * @objective Ensure that the two methods to compute the angular distance given in the enum
     *            {@link AngularDistanceType} provide the same result for specific cases
     * 
     * @description The following scenario is build:
     *              <ul>
     *              <li>A square field of view is build from a generic direction W. U and V defines the edges
     *              directions</li>
     *              <li>A series of test directions D are considered over the two semi-planes containing (U, W) and (V,
     *              W), respectively and such that dot(D,W)>0</li>
     *              <li>For each of these directions, we expect that the directional and minimal angular distances are
     *              the same</li>
     *              </ul>
     * 
     * @testPassCriteria the directional and minimal angular distances are the equals for each of the tested directions.
     *                   <p>
     *                   In this case, the test tolerance is defined based on the ULP of the difference between the
     *                   DIRECTIONAL and MINIMAL distance computed. This is a good practice since we are comparing
     *                   results that are computed with two different methods that do not necessarily provide the same
     *                   numerical value
     *                   </p>
     * 
     * @referenceVersion 4.14
     * 
     * @nonRegressionVersion 4.14
     * 
     * @throws PatriusException
     *         if the eclipse computer fails
     * 
     */
    @Test
    public void testDirectionalDistance4() throws PatriusException {

        final String fovName = "FOV_Name";
        final Vector3D mainDir = new Vector3D(1.0, 2.6, 9.8);
        final Vector3D uDirection = mainDir.orthogonal();
        final Vector3D vDirection = Vector3D.crossProduct(mainDir, uDirection);
        final double uOpening = Math.toRadians(15);
        final double vOpening = Math.toRadians(15);

        // Define the angle step on theta for this test
        final double dTheta = Math.toRadians(5.);

        // Create field
        final RectangleField field = new RectangleField(fovName, mainDir, uDirection, uOpening, vOpening);

        // Let's now define two orthogonal directions that define a plane containing W.
        // This two directions will be used to create test direction using the following expression:
        // dirTest = dirW * cos(theta) + dirY * sin(theta)
        final Vector3D dirW = field.getW().normalize();

        // FIRST CASE: consider test directions belonging to the plane defined by W and U
        Vector3D dirY = uDirection;

        // Theta initially equal to 1deg because theta= 0 correspond to W direction
        // The angular opening computed for W direction is a special case that we are not interested to test now
        double theta = -Math.PI / 2 + Math.toRadians(1.);
        while (theta < Math.PI / 2) {

            final Vector3D dirTheta = dirW.scalarMultiply(Math.cos(theta)).add(dirY.scalarMultiply(Math.sin(theta)));
            final double distanceDirectional = field.getAngularDistance(dirTheta, DIRECTIONAL);
            final double distanceMinimal = field.getAngularDistance(dirTheta, AngularDistanceType.MINIMAL);

            final double ulpTolerance = MathLib.ulp(distanceDirectional - distanceMinimal);
            Assert.assertTrue(Math.abs(distanceMinimal - distanceMinimal) <= ulpTolerance);
            theta += dTheta;
        }

        // SECOND CASE: consider test directions belonging to the plane defined by W and U
        dirY = vDirection;

        // Theta initially equal to 1deg because theta= 0 correspond to W direction
        // The angular opening computed for W direction is a special case that we are not interested to test now
        theta = -Math.PI / 2 + Math.toRadians(1.);
        while (theta < Math.PI / 2) {
            final Vector3D dirTheta = dirW.scalarMultiply(Math.cos(theta)).add(dirY.scalarMultiply(Math.sin(theta)));
            final double distanceDirectional = field.getAngularDistance(dirTheta, DIRECTIONAL);
            final double distanceMinimal = field.getAngularDistance(dirTheta, AngularDistanceType.MINIMAL);
            assertEquals(distanceDirectional, distanceMinimal, TOL);
            theta += dTheta;
        }
    }

    /**
     * @testType UT
     * 
     * 
     * @testedMethod {@link RectangleField#getAngularDistance(Vector3D, AngularDistanceType)}
     * 
     * 
     * @objective Cover the case when {@link RectangleField#getAngularDistance(Vector3D, AngularDistanceType)} is
     *            invoked with null or parallel to W direction.
     * 
     * @description If a null direction is used as input, an exception should be thrown. If the input direction is
     *              parallel to W, the highest value of angular opening is returned
     * 
     * @testPassCriteria If a null direction is used as input, an exception should be thrown. If the input direction is
     *                   parallel to W, the highest value of angular opening is returned
     * 
     * @referenceVersion 4.14
     * 
     * @nonRegressionVersion 4.14
     * 
     * @throws PatriusException
     *         if the eclipse computer fails
     * 
     */
    @Test
    public void testSpecialDirections() throws PatriusException {

        final String fovName = "FOV_Name";
        final Vector3D mainDir = new Vector3D(1.0, 2.6, 9.8);
        final Vector3D uDirection = mainDir.orthogonal();
        final double uOpening = Math.toRadians(15);
        final double vOpening = Math.toRadians(15);

        // Create field
        final RectangleField field = new RectangleField(fovName, mainDir, uDirection, uOpening, vOpening);

        // Let's now define two orthogonal directions that define a plane containing W.
        // This two directions will be used to create test direction using the following expression:
        // dirTest = dirW * cos(theta) + dirY * sin(theta)
        final Vector3D dirW = field.getW().normalize();

        // Assert that an exception is thrown for a null direction
        assertThrows(
            IllegalArgumentException.class,
            () -> {
                field.getAngularDistance(Vector3D.ZERO, DIRECTIONAL);
            });

        // Assert that the distance correspond to the highest angular opening if the direction is parallel to W
        final double expectedDistance = Vector3D.angle(dirW, field.getSideAxis()[0]);
        assertEquals(expectedDistance, field.getAngularDistance(dirW, DIRECTIONAL), 0.);

    }

    /**
     * Compute the test direction belonging to a cone centered on W direction.
     *
     * The length and orientation of the testDirection are defined by theta (the angle wrt U in the UV plane) and gamma
     * (multiplicative factor giving the length of the vector projection in UV plane)
     * 
     * @param W
     *        Cone axis
     * @param U
     *        First direction, orthogonal to W and defining the cone's cross section plane
     * @param V
     *        Second direction, orthogonal to W and defining the cone's cross section plane
     * @param theta
     *        Angle that the test direction projection on UV plane forms with U
     * @param radius
     *        radius of the cone section, for Z=1
     * @return the test direction belonging to the cone
     */
    private Vector3D computeTestDir(final Vector3D W, final Vector3D U, final Vector3D V, final double theta,
                                    final double radius) {

        final Vector3D uProj = U.scalarMultiply(Math.cos(theta));
        final Vector3D vProj = V.scalarMultiply(Math.sin(theta));

        final Vector3D uvProj = uProj.add(vProj).scalarMultiply(radius);
        return W.add(uvProj);
    }


    @Before
    public void setUp() {
        Utils.clear();
    }
}
