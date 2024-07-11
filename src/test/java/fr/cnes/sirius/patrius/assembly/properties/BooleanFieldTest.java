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
 * @history creation 18/04/2012
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.properties;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.fieldsofview.BooleanField;
import fr.cnes.sirius.patrius.fieldsofview.BooleanField.BooleanCombination;
import fr.cnes.sirius.patrius.fieldsofview.CircularField;
import fr.cnes.sirius.patrius.fieldsofview.IFieldOfView;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * @description
 *              <p>
 *              Test class for the boolean field of view
 *              </p>
 * 
 * @see BooleanField
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public class BooleanFieldTest {

    /** Features description. */
    enum features {
        /**
         * @featureTitle boolean field of view
         * 
         * @featureDescription boolean field of view to be used in sensors description
         * 
         * @coveredRequirements DV-VEHICULE_190, DV-VEHICULE_200, DV-VEHICULE_220,
         *                      DV-VEHICULE_230, DV-VEHICULE_240, DV-VEHICULE_250
         */
        COMPLEX_FIELD
    }

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#COMPLEX_FIELD}
     * 
     * @testedMethod {@link BooleanField#getAngularDistance(Vector3D)}
     * @testedMethod {@link BooleanField#isInTheField(Vector3D)}
     * @testedMethod {@link BooleanField#getName()}
     * 
     * @description test of the basic methods of a complex field of view
     * 
     * @input a boolean field of view (made of two existing fields), some vectors
     * 
     * @output angular distances
     * 
     * @testPassCriteria the angular distances are right, with the expected signs (positive
     *                   if the vector is n the field)
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public void complexField() {

        // OR case
        // =========

        // creation of a circular field
        final String nameCirc1 = "circularField";
        Vector3D mainDirection = new Vector3D(1.0, 0.0, 2.0);
        final IFieldOfView fieldCirc1 = new CircularField(nameCirc1, FastMath.PI / 4.0, mainDirection);

        // creation of rectangle field
        mainDirection = new Vector3D(2.0, 0.0, 1.0);
        final String nameCirc2 = "circularField2";
        final IFieldOfView fieldCirc2 = new CircularField(nameCirc2, FastMath.PI / 4.0, mainDirection);

        // complex field
        final String name = "complexField";
        IFieldOfView field = new BooleanField(name, fieldCirc1, fieldCirc2, BooleanCombination.OR);

        // test of directions
        Assert.assertTrue(field.isInTheField(new Vector3D(1.0, 1.0, 2.0)));
        Assert.assertTrue(!field.isInTheField(new Vector3D(-1.0, -1.0, 0.0)));

        Assert.assertEquals(-FastMath.PI / 4.0 - MathLib.atan2(1, 2),
            field.getAngularDistance(new Vector3D(-1.0, 0.0, 0.0)), this.comparisonEpsilon);

        // AND case
        // =========

        // complex field
        field = new BooleanField(name, fieldCirc1, fieldCirc2, BooleanCombination.AND);

        // test of directions
        Assert.assertTrue(field.isInTheField(new Vector3D(1.0, 0.0, 1.0)));
        Assert.assertTrue(!field.isInTheField(new Vector3D(0.0, 0.0, 1.0)));

        Assert.assertEquals(MathLib.atan2(1, 2),
            field.getAngularDistance(new Vector3D(1.0, 0.0, 1.0)), this.comparisonEpsilon);

        Assert.assertEquals(name, field.getName());

    }

}
