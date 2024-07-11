/**
 * 
 * Copyright 2011-2022 CNES
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
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.properties;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.fieldsofview.OmnidirectionalField;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * @description
 *              <p>
 *              Test class for the omnidirectional field of view
 *              </p>
 * 
 * @see OmnidirectionalField
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public class OmniDirectionalFieldTest {

    /** Features description. */
    enum features {
        /**
         * @featureTitle Omnidirectional field of view
         * 
         * @featureDescription Omnidirectional field of view to be used in sensors description
         * 
         * @coveredRequirements DV-VEHICULE_190, DV-VEHICULE_200, DV-VEHICULE_220,
         *                      DV-VEHICULE_230, DV-VEHICULE_240, DV-VEHICULE_250
         */
        OMNIDIRECTIONAL_FIELD
    }

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#OMNIDIRECTIONAL_FIELD}
     * 
     * @testedMethod {@link OmnidirectionalField#getAngularDistance(Vector3D)}
     * @testedMethod {@link OmnidirectionalField#isInTheField(Vector3D)}
     * @testedMethod {@link OmnidirectionalField#getName()}
     * 
     * @description test of the basic methods of an omnidirectional field of view
     * 
     * @input an omnidirectional field of view, some vectors
     * 
     * @output angular distances
     * 
     * @testPassCriteria the angular distances are 1.0, the vector is always in the field
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public void circularField() {

        final String name = "omniField";

        final OmnidirectionalField field = new OmnidirectionalField(name);

        // test several vectors of space
        Vector3D testedDirection = new Vector3D(1.0, 0.0, 2.0);
        Assert.assertTrue(field.isInTheField(testedDirection));
        Assert.assertEquals(field.getAngularDistance(testedDirection), 1.0, this.comparisonEpsilon);

        testedDirection = new Vector3D(87.0, -9678.0, -0.5);
        Assert.assertTrue(field.isInTheField(testedDirection));

        // name test
        Assert.assertEquals(name, field.getName());
    }

}
