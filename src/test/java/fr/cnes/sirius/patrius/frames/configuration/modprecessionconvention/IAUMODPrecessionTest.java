/**
 * Copyright 2023-2023 CNES
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
 * HISTORY
 * VERSION:4.13:DM:DM-108:08/12/2023:[PATRIUS] Modele d'obliquite et de precession de la Terre
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.modprecessionconvention;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.frames.configuration.modprecession.IAUMODPrecession;
import fr.cnes.sirius.patrius.frames.configuration.modprecession.IAUMODPrecessionConvention;

/**
 * Test class for {@link IAUMODPrecession}.
 * 
 * @author Emmanuel Bignon
 * 
 * @since 4.13
 */
public class IAUMODPrecessionTest {

    /**
     * @testType UT
     * 
     * @description functional check of {@link IAUMODPrecession} class (getters exception)
     * 
     * @testPassCriteria getters returns expected values, exceptions are thrown as expected
     * 
     * @referenceVersion 4.13
     * 
     * @nonRegressionVersion 4.13
     */
    @Test
    public void testFunctional() {
        // Test getters
        final IAUMODPrecession iauMODPrecession = new IAUMODPrecession(IAUMODPrecessionConvention.IAU2000, 3, 2);
        Assert.assertEquals(IAUMODPrecessionConvention.IAU2000, iauMODPrecession.getConvention());
        Assert.assertEquals(3, iauMODPrecession.getObliquityDegree());
        Assert.assertEquals(2, iauMODPrecession.getPrecessionDegree());
        Assert.assertEquals(3, iauMODPrecession.getPolynomialObliquity().getDegree());
        Assert.assertEquals(2, iauMODPrecession.getPolynomialPrecessionZeta().getDegree());
        Assert.assertEquals(2, iauMODPrecession.getPolynomialPrecessionTheta().getDegree());
        Assert.assertEquals(2, iauMODPrecession.getPolynomialPrecessionZ().getDegree());

        // Test exceptions

        // Degree obliquity < 0
        try {
            new IAUMODPrecession(IAUMODPrecessionConvention.IAU2000, -1, 3);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        // Degree precession < 0
        try {
            new IAUMODPrecession(IAUMODPrecessionConvention.IAU2000, 3, -1);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        // Degree obliquity too high
        try {
            new IAUMODPrecession(IAUMODPrecessionConvention.IAU2000, 10, 3);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        // Degree precession too high
        try {
            new IAUMODPrecession(IAUMODPrecessionConvention.IAU2000, 3, 10);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
    }
}
