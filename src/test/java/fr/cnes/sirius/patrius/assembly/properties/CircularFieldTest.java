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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.6:FA:FA-2542:27/01/2021:[PATRIUS] Definition d'un champ de vue avec demi-angle de 180° 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION:4.1.1:FA:1797:07/09/2018: Add getter
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.properties;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.fieldsofview.CircularField;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * @description <p>
 *              Test class for the circular field of view
 *              </p>
 * 
 * @see CircularField
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public class CircularFieldTest {

    /** Features description. */
    enum features {
        /**
         * @featureTitle Circular field of view
         * 
         * @featureDescription Circular field of view to be used in sensors description
         * 
         * @coveredRequirements DV-VEHICULE_190, DV-VEHICULE_200, DV-VEHICULE_220, DV-VEHICULE_230,
         *                      DV-VEHICULE_240, DV-VEHICULE_250
         */
        CIRCULAR_FIELD
    }

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#CIRCULAR_FIELD}
     * 
     * @testedMethod {@link CircularField#getAngularDistance(Vector3D)}
     * @testedMethod {@link CircularField#isInTheField(Vector3D)}
     * @testedMethod {@link CircularField#getName()}
     * 
     * @description test of the basic methods of a circular field of view
     * 
     * @input a circular field of view, some vectors
     * 
     * @output angular distances
     * 
     * @testPassCriteria the angular distances are right, with the expected signs (positive if the
     *                   vector is n the field)
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public void circularField() {

        final String name = "circularField";

        final Vector3D mainDirection = Vector3D.PLUS_K;

        // tests with wrong a angular aperture
        try {
            new CircularField(name, -1.0, mainDirection);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        try {
            new CircularField(name, 0.0, mainDirection);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        try {
            new CircularField(name, 3.2, mainDirection);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }

        try {
            new CircularField(name, 0.2, Vector3D.ZERO);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }

        final CircularField field = new CircularField(name, FastMath.PI / 4.0, mainDirection);

        // test with a vector in the field
        Vector3D testedDirection = new Vector3D(1.0, 0.0, 2.0);
        Assert.assertEquals(FastMath.PI / 4.0, field.getHalfAngularAperture(), 0);
        Assert.assertFalse(field.isInTheField(new Vector3D(0, 0, 0)));

        Assert.assertTrue(field.isInTheField(testedDirection));
        Assert.assertEquals(field.getAngularDistance(testedDirection),
            FastMath.PI / 4.0 - MathLib.atan2(1.0, 2.0), this.comparisonEpsilon);

        // test with a vector out of the field
        testedDirection = new Vector3D(1.0, 0.0, 0.5);

        Assert.assertTrue(!field.isInTheField(testedDirection));
        Assert.assertEquals(field.getAngularDistance(testedDirection), -FastMath.PI / 4.0
            + MathLib.atan2(1.0, 2.0), this.comparisonEpsilon);

        Assert.assertEquals(name, field.getName());

        // test with zero direction
        testedDirection = Vector3D.ZERO;
        Assert.assertEquals(field.getAngularDistance(testedDirection), 0.0, this.comparisonEpsilon);
        
        // Test omni-directional field
        Assert.assertTrue(new CircularField("", MathLib.PI, Vector3D.PLUS_J).isInTheField(Vector3D.MINUS_J));
    }

}
