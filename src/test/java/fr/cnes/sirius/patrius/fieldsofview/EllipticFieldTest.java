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
 * @history Creation 16/04/2012
 *
 * HISTORY
 * VERSION:4.15:OPENFD-385:21/11/2024:Execution en parallele des tests concernant EclipticJ2000Provider
 * VERSION:4.14.1:OPENFD-396:10/09/2024:[PATRIUS] Erreurs et oublis dans les classes issues de IGeometricFieldOfView
 * VERSION:4.14:OPENFD-173:22/08/2024: Ajout d'une nouvelle interface IGeometricaFieldOfView
 * VERSION:4.14:OPENFD-311:22/08/2024: [PATRIUS] getInputCoord sur EllipsoidPoint
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:400:17/03/2015: use class FastMath instead of class Math
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 * */
package fr.cnes.sirius.patrius.fieldsofview;

import static org.junit.Assert.assertEquals;
import fr.cnes.sirius.patrius.Utils;
import static org.junit.Assert.assertThrows;
import fr.cnes.sirius.patrius.Utils;
import static org.junit.Assert.assertTrue;
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
 *              Test class for the elliptic field of view
 *              </p>
 * 
 * @see EllipticField
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public class EllipticFieldTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Elliptic field of view
         * 
         * @featureDescription Elliptic field of view to be used in sensors description
         * 
         * @coveredRequirements DV-VEHICULE_190, DV-VEHICULE_230, DV-VEHICULE_240, DV-VEHICULE_250, DV-VEHICULE_260
         */
        ELLIPTIC_FIELD
    }

    /**
     * Directional distance.
     * It defined as the angle between the current direction d and the FOV border intersection with the half-plane
     * containing both d and the FOV's main direction.
     */
    private static final AngularDistanceType DIRECTIONAL = AngularDistanceType.DIRECTIONAL;

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    /** Test default tolerance */
    private static final double TOL = Precision.DOUBLE_COMPARISON_EPSILON;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ELLIPTIC_FIELD}
     * 
     * @testedMethod {@link CircularField#getAngularDistance(Vector3D)}
     * @testedMethod {@link CircularField#isInTheField(Vector3D)}
     * @testedMethod {@link CircularField#getName()}
     * 
     * @description test of the basic methods of an acute elliptic field of view
     * 
     * @input an acute elliptic field of view, some vectors
     * 
     * @output angular distances, inside checks, and name
     * 
     * @testPassCriteria the created field
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public void ellipticField() {

        final String name = "ellipticField";

        final Vector3D mainDirection = Vector3D.PLUS_K;
        final Vector3D semiADirection = Vector3D.PLUS_I;

        final Vector3D center = Vector3D.ZERO;

        // tests with wrong a angular aperture
        try {
            new EllipticField(name, center, mainDirection, semiADirection,
                FastMath.PI * 1.1, FastMath.PI * .2);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        try {
            new EllipticField(name, Vector3D.NEGATIVE_INFINITY, mainDirection,
                semiADirection, FastMath.PI * 1.1, FastMath.PI * .2);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        try {
            new EllipticField(name, center, mainDirection, semiADirection,
                FastMath.PI * .2, FastMath.PI * 1.1);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        try {
            new EllipticField(name, center, mainDirection, semiADirection,
                FastMath.PI * .2, 0);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        try {
            new EllipticField(name, center, Vector3D.ZERO, semiADirection,
                FastMath.PI * .2, FastMath.PI * .3);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        try {
            new EllipticField(name, center, mainDirection, Vector3D.ZERO, FastMath.PI * .2,
                FastMath.PI * .3);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        try {
            new EllipticField(name, center, mainDirection, Vector3D.ZERO, FastMath.PI * .2,
                FastMath.PI * .7);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        try {
            new EllipticField(name, center, mainDirection, Vector3D.ZERO, FastMath.PI * .7,
                FastMath.PI * .2);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        try {
            new EllipticField(name, center, mainDirection, Vector3D.ZERO, FastMath.PI * .5,
                FastMath.PI * .4);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        try {
            new EllipticField(name, center, mainDirection, Vector3D.ZERO, FastMath.PI * .4,
                FastMath.PI * .5);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        try {
            new EllipticField(name, center, mainDirection, Vector3D.ZERO, FastMath.PI * .5,
                FastMath.PI * .5);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }

        final EllipticField field = new EllipticField(name, center, mainDirection, semiADirection, FastMath.PI / 4,
            FastMath.PI / 3);

        // test with a vector in the field
        Vector3D testedDirection = new Vector3D(MathLib.cos(FastMath.PI / 4), 0, MathLib.sin(FastMath.PI / 4) + .1);
        // System.out.println();
        Assert.assertTrue(field.isInTheField(testedDirection));

        // test with a vector in the field
        testedDirection = new Vector3D(MathLib.cos(FastMath.PI / 4), 0, MathLib.sin(FastMath.PI / 4) - .1);

        Assert.assertFalse(field.isInTheField(testedDirection));

        // test with a vector in the field
        testedDirection = new Vector3D(0, MathLib.cos(FastMath.PI / 6), MathLib.sin(FastMath.PI / 6) + .1);
        Assert.assertTrue(field.isInTheField(testedDirection));

        // test with a vector in the field
        testedDirection = new Vector3D(0, MathLib.cos(FastMath.PI / 6), MathLib.sin(FastMath.PI / 6) - .1);
        Assert.assertFalse(field.isInTheField(testedDirection));

        // test angular separation
        testedDirection = new Vector3D(0, MathLib.cos(FastMath.PI / 12), MathLib.sin(FastMath.PI / 12));
        Assert.assertEquals(-FastMath.PI / 6 + FastMath.PI / 12, field.getAngularDistance(testedDirection),
            this.comparisonEpsilon);

        // test angular separation
        testedDirection = new Vector3D(MathLib.cos(FastMath.PI / 12), 0, MathLib.sin(FastMath.PI / 12));
        Assert.assertEquals(-FastMath.PI / 4 + FastMath.PI / 12, field.getAngularDistance(testedDirection),
            this.comparisonEpsilon);

        // test angular separation
        testedDirection = new Vector3D(MathLib.cos(FastMath.PI / 12), 0, -MathLib.sin(FastMath.PI / 12));
        Assert.assertEquals(-FastMath.PI / 3, field.getAngularDistance(testedDirection), this.comparisonEpsilon);

        // test angular separation
        testedDirection = new Vector3D(MathLib.cos(FastMath.PI / 3), 0, MathLib.sin(FastMath.PI / 3));
        Assert.assertEquals(-FastMath.PI / 4 + FastMath.PI / 3, field.getAngularDistance(testedDirection),
            this.comparisonEpsilon);

        // test angular separation
        testedDirection = new Vector3D(-5, -5, -5);
        Assert.assertEquals(-FastMath.PI / 2, field.getAngularDistance(testedDirection), this.comparisonEpsilon);

        // test name
        Assert.assertSame(name, field.getName());

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ELLIPTIC_FIELD}
     * 
     * @testedMethod {@link CircularField#getAngularDistance(Vector3D)}
     * @testedMethod {@link CircularField#isInTheField(Vector3D)}
     * @testedMethod {@link CircularField#getName()}
     * 
     * @description test of the basic methods of an obtuse elliptic field of view
     * 
     * @input an obtuse elliptic field of view, some vectors
     * 
     * @output angular distances, inside checks, and name
     * 
     * @testPassCriteria the created field
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public void obtuseEllipticField() {

        final String name = "ellipticField";

        final Vector3D mainDirection = Vector3D.PLUS_K;
        final Vector3D semiADirection = Vector3D.PLUS_I;

        // tests with wrong a angular aperture
        final EllipticField field = new EllipticField(name, Vector3D.ZERO, mainDirection, semiADirection,
            FastMath.PI * 3 / 4, FastMath.PI * 3.5 / 4);

        Vector3D point = new Vector3D(0, 0, -5);
        Assert.assertFalse(field.isInTheField(point));

        point = new Vector3D(MathLib.cos(FastMath.PI * .5 / 4), 0, -MathLib.sin(FastMath.PI * .5 / 4));
        Assert.assertTrue(field.isInTheField(point));

        point = new Vector3D(MathLib.cos(FastMath.PI * 1.5 / 4), 0, -MathLib.sin(FastMath.PI * 1.5 / 4));
        Assert.assertFalse(field.isInTheField(point));

        point = new Vector3D(0, MathLib.cos(FastMath.PI * .7 / 4), -MathLib.sin(FastMath.PI * .7 / 4));
        Assert.assertTrue(field.isInTheField(point));

        point = new Vector3D(0, MathLib.cos(FastMath.PI * 1.7 / 4), -MathLib.sin(FastMath.PI * 1.7 / 4));
        Assert.assertFalse(field.isInTheField(point));

        // test angles
        point = new Vector3D(0, 0, 5);
        Assert.assertEquals(FastMath.PI / 2, field.getAngularDistance(point), Precision.DOUBLE_COMPARISON_EPSILON);

        point = new Vector3D(0, 0, -5);
        Assert.assertEquals(-(FastMath.PI - FastMath.PI * 3.5 / 4), field.getAngularDistance(point),
            Precision.DOUBLE_COMPARISON_EPSILON);

        point = new Vector3D(5, 0, 0);
        Assert.assertEquals(FastMath.PI / 4, field.getAngularDistance(point), Precision.DOUBLE_COMPARISON_EPSILON);

        point = new Vector3D(0, 4, 0);
        Assert
            .assertEquals(FastMath.PI * 1.5 / 4, field.getAngularDistance(point), Precision.DOUBLE_COMPARISON_EPSILON);

        // System.out.println(FastMath.PI * 3 / 4 + " " + FastMath.PI * 3.5 / 4);
        final String expected =
            "EllipticField{Origin{0; 0; 0},Direction{0; 0; 1},U vector{1; 0; 0},Angle on U{2.356194490192345},Angle on V{2.748893571891069}}";
        Assert.assertEquals(expected, field.toString());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ELLIPTIC_FIELD}
     * 
     * @testedMethod {@link EllipticField#getAngularDistance(Vector3D, AngularDistanceType)}
     * 
     * 
     * @objective Ensure that the angular distance, considering the {@link AngularDistanceType#DIRECTIONAL} method
     *            provides the correct values. In this unit test, elementary directions are tested.
     *            To be sure that results do not depend on the choice of the axis U and W (main direction), we run the
     *            same test case twice considering a simple case (U, W equals to I, K axes of the canonic basis) and a
     *            more realistic one.
     *
     * @description For each of the two cases, the following directions are tested:
     *              <ul>
     *              <li>Directions parallel to FOV axes U, V</li>
     *              <li>The four FOV diagonals U, V</li>
     *              </ul>
     * 
     * @testPassCriteria the angular distances are correct, with the expected signs (positive
     *                   if the vector is in the field)
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
        EllipticField field = new EllipticField(fovName, Vector3D.ZERO, mainDir, uDirection, uOpening, vOpening);

        // Assert that the angular distance along the FOV axes is 0;
        Vector3D ax = uDirection.scalarMultiply(Math.tan(uOpening)).add(mainDir);
        assertEquals(0., field.getAngularDistance(ax, DIRECTIONAL), TOL);
        ax = uDirection.negate().scalarMultiply(Math.tan(uOpening)).add(mainDir);
        assertEquals(0., field.getAngularDistance(ax, DIRECTIONAL), TOL);
        ax = vDirection.scalarMultiply(Math.tan(vOpening)).add(mainDir);
        assertEquals(0., field.getAngularDistance(ax, DIRECTIONAL), TOL);
        ax = vDirection.scalarMultiply(Math.tan(vOpening)).add(mainDir);
        assertEquals(0., field.getAngularDistance(ax, DIRECTIONAL), TOL);

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
        mainDir = new Vector3D(1.0, 2.6, 9.8).normalize();
        uDirection = new Vector3D(0.48, 7.63, 6.21).normalize();
        uOpening = Math.toRadians(15);
        vOpening = Math.toRadians(30);

        final Vector3D expectedUDir =
            uDirection.subtract(mainDir.scalarMultiply(uDirection.dotProduct(mainDir))).normalize();
        final Vector3D expectedVDir = Vector3D.crossProduct(mainDir, expectedUDir).normalize();
        // Create field
        field = new EllipticField(fovName, Vector3D.ZERO, mainDir, uDirection, uOpening, vOpening);

        // Assert that the angular distance along the FOV axes is 0;

        ax = expectedUDir.scalarMultiply(Math.tan(uOpening)).add(mainDir);
        assertEquals(0., field.getAngularDistance(ax, DIRECTIONAL), TOL);
        ax = expectedUDir.negate().scalarMultiply(Math.tan(uOpening)).add(mainDir);
        assertEquals(0., field.getAngularDistance(ax, DIRECTIONAL), TOL);
        ax = expectedVDir.scalarMultiply(Math.tan(vOpening)).add(mainDir);
        assertEquals(0., field.getAngularDistance(ax, DIRECTIONAL), TOL);
        ax = expectedVDir.scalarMultiply(Math.tan(vOpening)).add(mainDir);
        assertEquals(0., field.getAngularDistance(ax, DIRECTIONAL), TOL);

        // Test Over the Field directions +U, +V, -U, -V
        // The distance should be equal to PI/2 minus the opening in that direction
        testDir = expectedUDir;
        assertEquals(uOpening - Math.PI / 2, field.getAngularDistance(testDir, DIRECTIONAL), TOL);
        testDir = expectedVDir;
        assertEquals(vOpening - Math.PI / 2, field.getAngularDistance(testDir, DIRECTIONAL), TOL);
        testDir = expectedUDir.negate();
        assertEquals(uOpening - Math.PI / 2, field.getAngularDistance(testDir, DIRECTIONAL), TOL);
        testDir = expectedVDir.negate();
        assertEquals(vOpening - Math.PI / 2, field.getAngularDistance(testDir, DIRECTIONAL), TOL);

        // Test over linear combination of one axis + Main direction

        // The distance should be equal to PI/2 minus the angle between the test direction and the mainDir
        testDir = expectedUDir.add(mainDir);
        assertEquals(uOpening - Vector3D.angle(mainDir, testDir), field.getAngularDistance(testDir, DIRECTIONAL), TOL);
        testDir = expectedVDir.add(mainDir);
        assertEquals(vOpening - Vector3D.angle(mainDir, testDir), field.getAngularDistance(testDir, DIRECTIONAL), TOL);
        testDir = expectedUDir.add(mainDir).negate();
        assertEquals(uOpening - Vector3D.angle(mainDir, testDir), field.getAngularDistance(testDir, DIRECTIONAL), TOL);
        testDir = expectedVDir.add(mainDir).negate();
        assertEquals(vOpening - Vector3D.angle(mainDir, testDir), field.getAngularDistance(testDir, DIRECTIONAL), TOL);

    }

    /**
     * @testType UT
     * 
     * 
     * @testedMethod {@link EllipticField#getAngularDistance(Vector3D, AngularDistanceType)}
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
     * @testPassCriteria from the computed angular distance we retrieve the corresponding angular opening. We ensure
     *                   then that this value satisfies the elliptic cone equation
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
        final Vector3D mainDir = new Vector3D(1.0, 2.6, 9.8).normalize();

        // Define the angle step on theta for this test
        final double dTheta = Math.toRadians(5.11);

        // Large Opening
        final Vector3D uDirection = new Vector3D(0.48, 7.63, 6.21).normalize();
        final double uOpening = Math.toRadians(15);

        // Small Opening
        final double vOpening = Math.toRadians(30);

        // Create field
        final EllipticField field = new EllipticField(fovName, Vector3D.ZERO, mainDir, uDirection, uOpening, vOpening);

        final double tanU = Math.tan(uOpening);
        final double tanV = Math.tan(vOpening);

        final Vector3D expectedUDir =
            uDirection.subtract(mainDir.scalarMultiply(uDirection.dotProduct(mainDir))).normalize();
        final Vector3D expectedVDir = Vector3D.crossProduct(mainDir, expectedUDir).normalize();

        // FIRST CASE: The testDirections belong to a cone internal to the FOV
        double theta = 0.;
        while (theta < (2 * Math.PI)) {
            final double coneRadius = mainDir.getNorm() * Math.sin(vOpening) * 0.8;

            // Compute the direction on the cone's surface
            final Vector3D testDir = this.computeTestDir(mainDir, expectedUDir, expectedVDir, theta, coneRadius);

            final double distance = field.getAngularDistance(testDir, DIRECTIONAL);
            final double opening = distance + Vector3D.angle(testDir, field.getMainDirection());
            final double rho = Math.tan(opening);
            final double cosT = Math.cos(theta);
            final double sinT = Math.sin(theta);
            assertEquals(1, Math.pow(rho * cosT / tanU, 2) + Math.pow(rho * sinT / tanV, 2), 1e-14);
            theta += dTheta;
        }

        // SECOND CASE: The testDirections belong to a cone external to the FOV
        theta = 0.;
        while (theta < (2 * Math.PI)) {
            final double coneRadius = mainDir.getNorm() * Math.sin(vOpening) * 1.2;

            // Compute the direction on the cone's surface
            final Vector3D testDir = this.computeTestDir(mainDir, expectedUDir, expectedVDir, theta, coneRadius);
            final double distance = field.getAngularDistance(testDir, DIRECTIONAL);
            final double opening = distance + Vector3D.angle(testDir, field.getMainDirection());
            final double rho = Math.tan(opening);
            final double cosT = Math.cos(theta);
            final double sinT = Math.sin(theta);
            assertEquals(1, Math.pow(rho * cosT / tanU, 2) + Math.pow(rho * sinT / tanV, 2), 1e-14);
            theta += dTheta;
        }

        // THIRD CASE: The testDirections belong to a cone external to the FOV centered on -mainDir
        theta = 0.;
        while (theta < (2 * Math.PI)) {
            final double coneRadius = mainDir.getNorm() * Math.sin(vOpening) * 1.2;
            final Vector3D testDir =
                this.computeTestDir(mainDir.negate(), expectedUDir, expectedVDir, theta, coneRadius);

            // Compute the direction on the cone's surface
            final double distance = field.getAngularDistance(testDir, DIRECTIONAL);
            final double opening = distance + Vector3D.angle(testDir, field.getMainDirection());
            final double rho = Math.tan(opening);
            final double cosT = Math.cos(theta);
            final double sinT = Math.sin(theta);
            assertEquals(1, Math.pow(rho * cosT / tanU, 2) + Math.pow(rho * sinT / tanV, 2), 1e-14);
            theta += dTheta;
        }
    }

    /**
     * @testType UT
     * 
     * 
     * @testedMethod {@link EllipticField#getAngularDistance(Vector3D, AngularDistanceType)}
     * 
     * 
     * @objective Ensure that the angular distance, considering the {@link AngularDistanceType#DIRECTIONAL} method
     *            provides the correct values.
     *            In this unit test, we verify that the angular opening is the same for specific directions over the
     *            same plane.
     * 
     * @description The following scenario is build:
     *              <ul>
     *              <li>An elliptic field is created from a two random vectors (not parallel)</li>
     *              <li>the field's main direction W and a second direction Y, orthogonal to W, define a plane
     *              containing both the directions</li>
     *              <li>A series of test directions, belonging to this plane, are build as a linear combination of W and
     *              Y</li>
     *              <li>The angular opening for each of these directions is computed and compared with a reference
     *              value</li>
     *              </ul>
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
        final EllipticField field = new EllipticField(fovName, Vector3D.ZERO, mainDir, uDirection, uOpening, vOpening);

        // Let's now define two orthogonal directions that define a plane containing W.
        // This two directions will be used to create test direction using the following expression:
        // dirTest = dirX * cos(theta) + dirY * sin(theta)
        final Vector3D dirW = field.getMainDirection().normalize();
        final Vector3D dirY = field.getMainDirection().orthogonal().normalize();

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
            assertEquals(refOpening, openingTheta, 1e-15);
            theta += dTheta;
        }
    }

    /**
     * @testType UT
     * 
     * 
     * @testedMethod {@link EllipticField#getAngularDistance(Vector3D, AngularDistanceType)}
     * 
     * 
     * @objective Cover the case when {@link EllipticField#getAngularDistance(Vector3D, AngularDistanceType)} is invoked
     *            with null or parallel to W direction.
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
        final double uOpening = Math.toRadians(30);
        final double vOpening = Math.toRadians(15);

        // Create field
        final EllipticField field = new EllipticField(fovName, Vector3D.ZERO, mainDir, uDirection, uOpening, vOpening);

        // Let's now define two orthogonal directions that define a plane containing W.
        // This two directions will be used to create test direction using the following expression:
        // dirTest = dirW * cos(theta) + dirY * sin(theta)
        final Vector3D dirW = field.getMainDirection();

        // Assert that an exception is thrown for a null direction
        assertThrows(
            IllegalArgumentException.class,
            () -> {
                field.getAngularDistance(Vector3D.ZERO, DIRECTIONAL);
            });

        // Assert that the distance correspond to the highest angular opening if the direction is parallel to W
        final double expectedDistance = uOpening;
        assertEquals(expectedDistance, field.getAngularDistance(dirW, DIRECTIONAL), 0.);

    }

    /**
     * @testType UT
     * 
     * 
     * @testedMethod {@link EllipticField#getAngularDistance(Vector3D, AngularDistanceType)}
     * 
     * 
     * @objective Cover the case when {@link EllipticField#getAngularDistance(Vector3D, AngularDistanceType)} is invoked
     *            with null or parallel to W direction, for an inverted field.
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
    public void testSpecialDirectionsInverted() throws PatriusException {

        final String fovName = "FOV_Name";
        final Vector3D mainDir = new Vector3D(1.0, 2.6, 9.8);
        final Vector3D uDirection = mainDir.orthogonal();
        final double uOpening = Math.toRadians(160);
        final double vOpening = Math.toRadians(150);

        // Create field
        final EllipticField field = new EllipticField(fovName, Vector3D.ZERO, mainDir, uDirection, uOpening, vOpening);

        // Let's now define two orthogonal directions that define a plane containing W.
        // This two directions will be used to create test direction using the following expression:
        // dirTest = dirW * cos(theta) + dirY * sin(theta)
        final Vector3D dirW = field.getMainDirection();

        // Assert that an exception is thrown for a null direction
        assertThrows(
            IllegalArgumentException.class,
            () -> {
                field.getAngularDistance(Vector3D.ZERO, DIRECTIONAL);
            });

        // Assert that the distance correspond to the highest angular opening if the direction is parallel to W
        final double expectedDistance = uOpening;
        assertEquals(expectedDistance, field.getAngularDistance(dirW, DIRECTIONAL), 0.);

        // Tests getAngularOpening
        assertEquals(uOpening, field.getAngularOpening(dirW), 0.);
        assertEquals(uOpening, field.getAngularOpening(uDirection), 0.);

        // Assert that the distance correspond to the highest angular opening if the direction is parallel to W

        // Test getAngularDistance
        // Create other dir shifted 5° in plane (mainDir, uDir), inside inverted FoV
        final double fiveDegInRad = MathLib.toRadians(5.);
        final Vector3D targetDir1 = new Vector3D(1., dirW, MathLib.tan(fiveDegInRad), uDirection).normalize();
        final double expectedDistance1 = uOpening - fiveDegInRad;

        assertEquals(expectedDistance1, field.getAngularDistance(targetDir1, DIRECTIONAL), 0.);
        assertTrue(field.isInTheField(targetDir1));

        // Create other dir shifted 5° in plane (-mainDir, uDir), outside inverted FoV
        final Vector3D targetDir2 = new Vector3D(-1., dirW, MathLib.tan(fiveDegInRad), uDirection).normalize();
        final double expectedDistance2 = -Math.PI + uOpening + fiveDegInRad;
        assertEquals(expectedDistance2, field.getAngularDistance(targetDir2, DIRECTIONAL),
            Precision.DOUBLE_COMPARISON_EPSILON);
        assertTrue(!field.isInTheField(targetDir2));

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
     *              <li>A circular field of view is build from a generic direction W. U and V defines the edges
     *              directions</li>
     *              <li>A series of test directions D are considered over the two semi-planes containing (U, W) and (V,
     *              W), respectively and such that dot(D,W)>0</li>
     *              <li>For each of these directions, we expect that the directional and minimal angular distances are
     *              the same</li>
     *              </ul>
     * 
     * @testPassCriteria the directional and minimal angular distances are the equals for each of the tested directions
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
        final EllipticField field = new EllipticField(fovName, Vector3D.ZERO, mainDir, uDirection, uOpening, vOpening);

        // Let's now define two orthogonal directions that define a plane containing W.
        // This two directions will be used to create test direction using the following expression:
        // dirTest = dirW * cos(theta) + dirY * sin(theta)
        final Vector3D dirW = field.getMainDirection().normalize();

        // FIRST CASE: consider test directions belonging to the plane defined by W and U
        Vector3D dirY = uDirection;

        // Theta initially equal to 1deg because theta= 0 correspond to W direction
        // The angular opening computed for W direction is a special case that we are not interested to test now
        double theta = -Math.PI / 2 + Math.toRadians(1.);
        while (theta < Math.PI / 2) {

            final Vector3D dirTheta = dirW.scalarMultiply(Math.cos(theta)).add(dirY.scalarMultiply(Math.sin(theta)));
            final double distanceDirectional = field.getAngularDistance(dirTheta, DIRECTIONAL);
            final double distanceMinimal = field.getAngularDistance(dirTheta, AngularDistanceType.MINIMAL);
            assertEquals(distanceDirectional, distanceMinimal, 1e-13);
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
            assertEquals(distanceDirectional, distanceMinimal, 1e-13);
            theta += dTheta;
        }
    }

    /**
     * Compute the test direction belonging to a cone centered on W, with its section lying in the plane defined by U
     * and V.
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
     * @param gamma
     *        multiplicative factor giving the length of the vector projection in UV plane
     * @return the test direction belonging to the cone
     */
    private Vector3D computeTestDir(final Vector3D W, final Vector3D U, final Vector3D V, final double theta,
                                    final double gamma) {

        final Vector3D uProj = U.scalarMultiply(Math.cos(theta));
        final Vector3D vProj = V.scalarMultiply(Math.sin(theta));

        final Vector3D uvProj = uProj.add(vProj).scalarMultiply(gamma);
        return W.add(uvProj);
    }


    @Before
    public void setUp() {
        Utils.clear();
    }
}
