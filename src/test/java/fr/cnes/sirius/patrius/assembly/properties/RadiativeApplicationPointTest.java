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
 * @history Created 22/07/2013
 * HISTORY
* VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:85:22/07/2013:Created radiative application point property
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.properties;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.assembly.PropertyType;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * Test class for radiative forces application point
 * 
 * @author Rami Houdroge
 * 
 * @version $Id$
 * 
 * @since 2.1
 */
public class RadiativeApplicationPointTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Radiative model wrench
         * 
         * @featureDescription Computation of the radiation pressure wrench
         * 
         * @coveredRequirements DV-COUPLES_30, DV-COUPLES_50
         */
        RADIATIVE_MODEL
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#RADIATIVE_MODEL}
     * 
     * @testedMethod {@link RadiativeApplicationPoint#getApplicationPoint()}
     * 
     * @description Property application point validation
     * 
     * @input Vector3D
     * 
     * @output Vector3D
     * 
     * @testPassCriteria same vector, to machine epsilon
     * 
     * @referenceVersion 2.1
     */
    @Test
    public void testGetApplicationPoint() {
        Assert.assertEquals(0,
            Vector3D.PLUS_I.subtract(new RadiativeApplicationPoint(Vector3D.PLUS_I).getApplicationPoint())
                .getNorm(), Precision.EPSILON);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#RADIATIVE_MODEL}
     * 
     * @testedMethod {@link RadiativeApplicationPoint#getApplicationPoint()}
     * 
     * @description Validate the property type
     * 
     * @input Vector3D
     * 
     * @output type
     * 
     * @testPassCriteria same vector, to machine epsilon
     * 
     * @referenceVersion 2.1
     */
    @Test
    public void testGetType() {
        Assert.assertTrue(PropertyType.RADIATION_APPLICATION_POINT.equals(new RadiativeApplicationPoint(
            Vector3D.MINUS_J).getType()));
    }

}
