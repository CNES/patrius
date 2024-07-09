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
 * @history creation 17/04/2012
 *
 * HISTORY
 * VERSION:4.5:DM:DM-2472:27/05/2020:Ajout d'un getter de sideAxis aux classes RectangleField et PyramidalField
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.properties;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.fieldsofview.PyramidalField;
import fr.cnes.sirius.patrius.fieldsofview.RectangleField;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * @description
 *              <p>
 *              Test class for the pyramidal field of view.
 *              </p>
 * 
 * @see PyramidalField
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public class PyramidalFieldTest {

    /** Features description. */
    enum features {
        /**
         * @featureTitle Pyramidal field of view
         * 
         * @featureDescription Pyramidal field of view to be used in sensors description
         * 
         * @coveredRequirements DV-VEHICULE_190, DV-VEHICULE_200, DV-VEHICULE_220,
         *                      DV-VEHICULE_230, DV-VEHICULE_240, DV-VEHICULE_250
         */
        PYRAMIDAL_FIELD
    }

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PYRAMIDAL_FIELD}
     * 
     * @testedMethod {@link PyramidalField#getAngularDistance(Vector3D)}
     * @testedMethod {@link PyramidalField#isInTheField(Vector3D)}
     * @testedMethod {@link PyramidalField#getName()}
     * 
     * @description test of the basic methods of a pyramidal field of view
     * 
     * @input a pyramidal field of view, some vectors
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
    public void pyramidalTest() {

        final String name = "pyramidalField";

        // test with a zero-normed vector
        try {
            final Vector3D[] directions = new Vector3D[4];
            directions[0] = new Vector3D(0.0, 0.0, 0.0);
            directions[1] = new Vector3D(1.0, -1.0, 1.0);
            directions[2] = new Vector3D(-1.0, -1.0, 1.0);
            directions[3] = new Vector3D(-1.0, 1.0, 1.0);
            new PyramidalField(name, directions);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }

        // test with too few vectors
        try {
            final Vector3D[] directions = new Vector3D[2];
            directions[0] = new Vector3D(1.0, 1.0, 1.0);
            directions[1] = new Vector3D(1.0, -1.0, 1.0);
            new PyramidalField(name, directions);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }

        // CONVEX FIELD
        Vector3D[] directions = new Vector3D[4];
        directions[0] = new Vector3D(1.0, 1.0, 1.0);
        directions[1] = new Vector3D(-1.0, 1.0, 1.0);
        directions[2] = new Vector3D(-1.0, -1.0, 1.0);
        directions[3] = new Vector3D(1.0, -1.0, 1.0);

        PyramidalField field = new PyramidalField(name, directions);

        // test with a vector in this field
        Vector3D testedDirection = new Vector3D(1.0, 0.0, 2.0);

        Assert.assertTrue(field.isInTheField(testedDirection));
        Assert.assertEquals(field.getAngularDistance(testedDirection),
            FastMath.PI / 4.0 - MathLib.atan2(1.0, 2.0), this.comparisonEpsilon);

        // test with a vector out of this field
        testedDirection = new Vector3D(0.0, 2.0, 1.0);

        Assert.assertTrue(!field.isInTheField(testedDirection));
        Assert.assertEquals(field.getAngularDistance(testedDirection),
            -FastMath.PI / 4.0 + MathLib.atan2(1.0, 2.0), this.comparisonEpsilon);

        // CONVEX FIELD 2
        directions = new Vector3D[5];
        directions[0] = new Vector3D(1.0, 1.0, 1.0);
        directions[1] = new Vector3D(-1.0, 1.0, 1.0);
        directions[2] = new Vector3D(-1.0, -1.0, 1.0);
        directions[3] = new Vector3D(0.0, -4.0, 1.0);
        directions[4] = new Vector3D(1.0, -1.0, 1.0);

        field = new PyramidalField(name, directions);

        // tests
        testedDirection = new Vector3D(1.0, -5.0, 1.0);
        Assert.assertTrue(!field.isInTheField(testedDirection));

        testedDirection = new Vector3D(-1.0, -5.0, 1.0);
        Assert.assertTrue(!field.isInTheField(testedDirection));

        testedDirection = new Vector3D(0.0, -3.0, 1.0);
        Assert.assertTrue(field.isInTheField(testedDirection));

        // CONCAVE FIELD
        directions = new Vector3D[5];
        directions[0] = new Vector3D(1.0, 1.0, 1.0);
        directions[1] = new Vector3D(-1.0, 1.0, 1.0);
        directions[2] = new Vector3D(-1.0, -1.0, 1.0);
        directions[3] = new Vector3D(0.0, -0.5, 1.0);
        directions[4] = new Vector3D(1.0, -1.0, 1.0);

        field = new PyramidalField(name, directions);

        // test with a vector in this field
        testedDirection = new Vector3D(0.0, 0.0, 2.0);

        Assert.assertTrue(field.isInTheField(testedDirection));
        Assert.assertEquals(field.getAngularDistance(testedDirection),
            MathLib.atan2(1.0, 2.0), this.comparisonEpsilon);

        // test with a vector out of this field
        testedDirection = new Vector3D(0.0, -0.75, 1.0);

        Assert.assertTrue(!field.isInTheField(testedDirection));

        // CONCAVE FIELD 2
        directions = new Vector3D[7];
        directions[0] = new Vector3D(0.0, -0.5, 1.0);
        directions[1] = new Vector3D(1.0, -2.0, 1.0);
        directions[2] = new Vector3D(2.0, -2.0, 1.0);
        directions[3] = new Vector3D(2.0, 2.0, 1.0);
        directions[4] = new Vector3D(-2.0, 2.0, 1.0);
        directions[5] = new Vector3D(-2.0, -2.0, 1.0);
        directions[6] = new Vector3D(-1.0, -2.0, 1.0);

        field = new PyramidalField(name, directions);

        // tests with vectors in this field
        testedDirection = new Vector3D(0.0, 0.0, 2.0);

        Assert.assertTrue(field.isInTheField(testedDirection));
        Assert.assertEquals(field.getAngularDistance(testedDirection),
            MathLib.atan2(1.0, 2.0), this.comparisonEpsilon);

        testedDirection = new Vector3D(0.25, -0.25, 1.0);
        Assert.assertTrue(field.isInTheField(testedDirection));

        testedDirection = new Vector3D(-0.25, -0.25, 1.0);
        Assert.assertTrue(field.isInTheField(testedDirection));

        // test with a vector out of this field
        testedDirection = new Vector3D(0.0, -0.75, 1.0);
        Assert.assertTrue(!field.isInTheField(testedDirection));

        // test with zero direction
        testedDirection = Vector3D.ZERO;
        Assert.assertEquals(field.getAngularDistance(testedDirection),
            0.0, this.comparisonEpsilon);

        // name test
        Assert.assertEquals(name, field.getName());

        // Test side axis (reference: math)
        final RectangleField rectangleSideField = new RectangleField("", Vector3D.PLUS_I, Vector3D.PLUS_J, FastMath.PI / 2., FastMath.PI / 2.);
        final PyramidalField pyramidalSideField = new PyramidalField("", rectangleSideField.getSideAxis());
        final Vector3D[] sideAxis = pyramidalSideField.getSideAxis();
        final double sqrt2Over2 = MathLib.sqrt(2.) / 2.;
        Assert.assertEquals(0., sideAxis[0].distance(new Vector3D(0, sqrt2Over2, sqrt2Over2)), 1E-16);
        Assert.assertEquals(0., sideAxis[1].distance(new Vector3D(0, -sqrt2Over2, sqrt2Over2)), 1E-16);
        Assert.assertEquals(0., sideAxis[2].distance(new Vector3D(0, -sqrt2Over2, -sqrt2Over2)), 1E-16);
        Assert.assertEquals(0., sideAxis[3].distance(new Vector3D(0, sqrt2Over2, -sqrt2Over2)), 1E-16);
    }

}
