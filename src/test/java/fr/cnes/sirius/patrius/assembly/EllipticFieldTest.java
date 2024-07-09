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
 * 
 * @history Creation 16/04/2012
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:400:17/03/2015: use class FastMath instead of class Math
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 * */
package fr.cnes.sirius.patrius.assembly;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.fieldsofview.CircularField;
import fr.cnes.sirius.patrius.fieldsofview.EllipticField;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * @description <p>
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

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

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

        // System.out.println(FastMath.PI * 3 / 4 + "     " + FastMath.PI * 3.5 / 4);
        final String expected =
            "EllipticField{Origin{0; 0; 0},Direction{0; 0; 1},U vector{1; 0; 0},Angle on U{2.356194490192345},Angle on V{2.748893571891069}}";
        Assert.assertEquals(expected, field.toString());
    }

}
