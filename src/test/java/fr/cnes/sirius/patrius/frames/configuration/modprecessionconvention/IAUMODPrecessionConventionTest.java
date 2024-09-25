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

import fr.cnes.sirius.patrius.frames.configuration.modprecession.IAUMODPrecessionConvention;
import fr.cnes.sirius.patrius.utils.Constants;

/**
 * Test class for {@link IAUMODPrecessionConvention}.
 * 
 * @author Emmanuel Bignon
 * 
 * @since 4.13
 */
public class IAUMODPrecessionConventionTest {

    /**
     * @testType UT
     * 
     * @description check values of IAU 76 model (in radians)
     * 
     * @testPassCriteria values are as expected (reference: IAU 76, absolute threshold: 0)
     * 
     * @referenceVersion 4.13
     * 
     * @nonRegressionVersion 4.13
     */
    @Test
    public void testIAU76() {
        final IAUMODPrecessionConvention convention = IAUMODPrecessionConvention.IAU1976;

        // Obliquity
        Assert.assertEquals(84381.448 * Constants.ARC_SECONDS_TO_RADIANS, convention.getObliquityCoefs()[0], 0.);
        Assert.assertEquals(-46.8150 * Constants.ARC_SECONDS_TO_RADIANS, convention.getObliquityCoefs()[1], 0.);
        Assert.assertEquals(-0.00059 * Constants.ARC_SECONDS_TO_RADIANS, convention.getObliquityCoefs()[2], 0.);
        Assert.assertEquals(0.001813 * Constants.ARC_SECONDS_TO_RADIANS, convention.getObliquityCoefs()[3], 0.);

        // Precession Zeta
        Assert.assertEquals(0. * Constants.ARC_SECONDS_TO_RADIANS, convention.getPrecessionZetaCoefs()[0], 0.);
        Assert.assertEquals(2306.2181 * Constants.ARC_SECONDS_TO_RADIANS, convention.getPrecessionZetaCoefs()[1], 0.);
        Assert.assertEquals(0.30188 * Constants.ARC_SECONDS_TO_RADIANS, convention.getPrecessionZetaCoefs()[2], 0.);
        Assert.assertEquals(0.017998 * Constants.ARC_SECONDS_TO_RADIANS, convention.getPrecessionZetaCoefs()[3], 0.);

        // Precession Theta
        Assert.assertEquals(0. * Constants.ARC_SECONDS_TO_RADIANS, convention.getPrecessionThetaCoefs()[0], 0.);
        Assert.assertEquals(2004.3109 * Constants.ARC_SECONDS_TO_RADIANS, convention.getPrecessionThetaCoefs()[1], 0.);
        Assert.assertEquals(-0.42665 * Constants.ARC_SECONDS_TO_RADIANS, convention.getPrecessionThetaCoefs()[2], 0.);
        Assert.assertEquals(-0.041833 * Constants.ARC_SECONDS_TO_RADIANS, convention.getPrecessionThetaCoefs()[3], 0.);

        // Precession Z
        Assert.assertEquals(0. * Constants.ARC_SECONDS_TO_RADIANS, convention.getPrecessionZCoefs()[0], 0.);
        Assert.assertEquals(2306.2181 * Constants.ARC_SECONDS_TO_RADIANS, convention.getPrecessionZCoefs()[1], 0.);
        Assert.assertEquals(1.09468 * Constants.ARC_SECONDS_TO_RADIANS, convention.getPrecessionZCoefs()[2], 0.);
        Assert.assertEquals(0.018203 * Constants.ARC_SECONDS_TO_RADIANS, convention.getPrecessionZCoefs()[3], 0.);
    }

    /**
     * @testType UT
     * 
     * @description check values of IAU 2000 model (in radians)
     * 
     * @testPassCriteria values are as expected (reference: IAU 2000, absolute threshold: 0)
     * 
     * @referenceVersion 4.13
     * 
     * @nonRegressionVersion 4.13
     */
    @Test
    public void testIAU2000() {
        final IAUMODPrecessionConvention convention = IAUMODPrecessionConvention.IAU2000;

        // Obliquity
        Assert.assertEquals(84381.448 * Constants.ARC_SECONDS_TO_RADIANS, convention.getObliquityCoefs()[0], 0.);
        Assert.assertEquals(-46.84024 * Constants.ARC_SECONDS_TO_RADIANS, convention.getObliquityCoefs()[1], 0.);
        Assert.assertEquals(-0.00059 * Constants.ARC_SECONDS_TO_RADIANS, convention.getObliquityCoefs()[2], 0.);
        Assert.assertEquals(0.001813 * Constants.ARC_SECONDS_TO_RADIANS, convention.getObliquityCoefs()[3], 0.);

        // Precession Zeta
        Assert.assertEquals(2.5976176 * Constants.ARC_SECONDS_TO_RADIANS, convention.getPrecessionZetaCoefs()[0], 0.);
        Assert.assertEquals(2306.0809506 * Constants.ARC_SECONDS_TO_RADIANS, convention.getPrecessionZetaCoefs()[1], 0.);
        Assert.assertEquals(0.3019015 * Constants.ARC_SECONDS_TO_RADIANS, convention.getPrecessionZetaCoefs()[2], 0.);
        Assert.assertEquals(0.0179663 * Constants.ARC_SECONDS_TO_RADIANS, convention.getPrecessionZetaCoefs()[3], 0.);
        Assert.assertEquals(-0.0000327 * Constants.ARC_SECONDS_TO_RADIANS, convention.getPrecessionZetaCoefs()[4], 0.);
        Assert.assertEquals(-0.0000002 * Constants.ARC_SECONDS_TO_RADIANS, convention.getPrecessionZetaCoefs()[5], 0.);

        // Precession Theta
        Assert.assertEquals(0. * Constants.ARC_SECONDS_TO_RADIANS, convention.getPrecessionThetaCoefs()[0], 0.);
        Assert.assertEquals(2004.1917476 * Constants.ARC_SECONDS_TO_RADIANS, convention.getPrecessionThetaCoefs()[1], 0.);
        Assert.assertEquals(-0.4269353 * Constants.ARC_SECONDS_TO_RADIANS, convention.getPrecessionThetaCoefs()[2], 0.);
        Assert.assertEquals(-0.0418251 * Constants.ARC_SECONDS_TO_RADIANS, convention.getPrecessionThetaCoefs()[3], 0.);
        Assert.assertEquals(-0.0000601 * Constants.ARC_SECONDS_TO_RADIANS, convention.getPrecessionThetaCoefs()[4], 0.);
        Assert.assertEquals(-0.0000001 * Constants.ARC_SECONDS_TO_RADIANS, convention.getPrecessionThetaCoefs()[5], 0.);

        // Precession Z
        Assert.assertEquals(-2.5976176 * Constants.ARC_SECONDS_TO_RADIANS, convention.getPrecessionZCoefs()[0], 0.);
        Assert.assertEquals(2306.0803226 * Constants.ARC_SECONDS_TO_RADIANS, convention.getPrecessionZCoefs()[1], 0.);
        Assert.assertEquals(1.0947790 * Constants.ARC_SECONDS_TO_RADIANS, convention.getPrecessionZCoefs()[2], 0.);
        Assert.assertEquals(0.0182273 * Constants.ARC_SECONDS_TO_RADIANS, convention.getPrecessionZCoefs()[3], 0.);
        Assert.assertEquals(0.0000470 * Constants.ARC_SECONDS_TO_RADIANS, convention.getPrecessionZCoefs()[4], 0.);
        Assert.assertEquals(-0.0000003 * Constants.ARC_SECONDS_TO_RADIANS, convention.getPrecessionZCoefs()[5], 0.);
    }
}
