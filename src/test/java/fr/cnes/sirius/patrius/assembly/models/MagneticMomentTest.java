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
 * @history creation 23/07/2012
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:85:04/09/2013:Electromagntic sensitive spacecraft
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.models;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * Test class for magnetic moment
 * 
 * @author Rami Houdroge
 * @since 2.1
 * @version $$
 * 
 */
public class MagneticMomentTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle Magnetic wrench model
         * 
         * @featureDescription Magnetic wrench model
         * 
         * @coveredRequirements DV-COUPLES_10, DV-COUPLES_20, DV-COUPLES_30, DV-COUPLES_60
         */
        MAG_WRENCH,

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MAG_WRENCH}
     * 
     * @testedMethod {@link MagneticMoment#MagneticMoment(Vector3D)}
     * @testedMethod {@link MagneticMoment#getMagneticMoment(AbsoluteDate)}
     * 
     * @description Test for the class methods
     * 
     * @input a vector
     * 
     * @output the same vector (magnetic moment)
     * 
     * @testPassCriteria the recovered magnetic moment is as expected, to 1e-14 on a relative scale for the norm
     * 
     * @referenceVersion 2.1
     * 
     * @nonRegressionVersion 2.1
     * 
     */
    @Test
    public void testMagneticMoment() {
        final MagneticMoment mag = new MagneticMoment(new Vector3D(1., 2., 3.));
        Assert.assertEquals(1., mag.getMagneticMoment(AbsoluteDate.CCSDS_EPOCH).getX(),
            Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(2., mag.getMagneticMoment(AbsoluteDate.CCSDS_EPOCH).getY(),
            Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(3., mag.getMagneticMoment(AbsoluteDate.CCSDS_EPOCH).getZ(),
            Precision.DOUBLE_COMPARISON_EPSILON);
    }

}
