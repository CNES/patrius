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
 * @history Creation 18/04/2012
 * 
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.properties;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.fieldsofview.CircularField;
import fr.cnes.sirius.patrius.fieldsofview.InvertField;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * @description
 *              <p>
 *              Test class for the invert field of view
 *              </p>
 * 
 * @see InvertField
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public class InvertFieldTest {

    /** Features description. */
    enum features {
        /**
         * @featureTitle Invert field of view
         * 
         * @featureDescription Invert field of view to be used in sensors description
         * 
         * @coveredRequirements DV-VEHICULE_190, DV-VEHICULE_200, DV-VEHICULE_220,
         *                      DV-VEHICULE_230, DV-VEHICULE_240, DV-VEHICULE_250
         */
        INVERT_FIELD
    }

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INVERT_FIELD}
     * 
     * @testedMethod {@link InvertField#getAngularDistance(Vector3D)}
     * @testedMethod {@link InvertField#isInTheField(Vector3D)}
     * @testedMethod {@link InvertField#getName()}
     * 
     * @description test of the basic methods of a circular field of view
     * 
     * @input a circular field of view, some vectors
     * 
     * @output angular distances
     * 
     * @testPassCriteria the angular distances are right (the opposite of the one given by the circular field),
     *                   with the expected signs (positive if the vector is out of the circular field)
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public void invertField() {

        final String nameCircular = "circularField";
        final String name = "invertField";

        final Vector3D mainDirection = Vector3D.PLUS_K;

        final CircularField circularField = new CircularField(nameCircular, FastMath.PI / 4.0, mainDirection);

        final InvertField field = new InvertField(name, circularField);

        // test with a vector in the field
        Vector3D testedDirection = new Vector3D(1.0, 0.0, 2.0);

        Assert.assertTrue(!field.isInTheField(testedDirection));
        Assert.assertEquals(field.getAngularDistance(testedDirection),
            -FastMath.PI / 4.0 + MathLib.atan2(1.0, 2.0), this.comparisonEpsilon);

        // test with a vector out of the field
        testedDirection = new Vector3D(1.0, 0.0, 0.5);

        Assert.assertTrue(field.isInTheField(testedDirection));
        Assert.assertEquals(field.getAngularDistance(testedDirection),
            FastMath.PI / 4.0 - MathLib.atan2(1.0, 2.0), this.comparisonEpsilon);

        Assert.assertEquals(name, field.getName());
    }

}
