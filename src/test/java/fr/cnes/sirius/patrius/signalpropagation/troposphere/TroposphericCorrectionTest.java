/**
 * Copyright 2011-2021 CNES
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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.8:DM:DM-2929:15/11/2021:[PATRIUS] Harmonisation des modeles de troposphere 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.signalpropagation.troposphere;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for the {@link TroposphericCorrection} interface.
 * 
 * @author tbonit
 */
public class TroposphericCorrectionTest {

    /** Validity threshold. */
    private static final double epsilon = 1e-10;

    /**
     * Pure non-regression test: there is not associated reference.
     * 
     * @testedMethod {@link TroposphericCorrection#computeStandardValues(double)}
     * @testedMethod {@link TroposphericCorrection#computeStandardValues(double, double, double, double, double)}
     */
    @Test
    public void testComputeStandardValues() {

        double[] standardValues = TroposphericCorrection.computeStandardValues(0.);
        Assert.assertEquals(291.15, standardValues[0], epsilon);
        Assert.assertEquals(101325.0, standardValues[1], epsilon);
        Assert.assertEquals(0.5, standardValues[2], epsilon);

        standardValues = TroposphericCorrection.computeStandardValues(10.);
        Assert.assertEquals(291.085, standardValues[0], epsilon);
        Assert.assertEquals(101205.40748359638, standardValues[1], epsilon);
        Assert.assertEquals(0.4968122054344217, standardValues[2], epsilon);

        standardValues = TroposphericCorrection.computeStandardValues(100.);
        Assert.assertEquals(290.5, standardValues[0], epsilon);
        Assert.assertEquals(100134.20224900974, standardValues[1], epsilon);
        Assert.assertEquals(0.46902126024056234, standardValues[2], epsilon);

        standardValues = TroposphericCorrection.computeStandardValues(1000.);
        Assert.assertEquals(284.65, standardValues[0], epsilon);
        Assert.assertEquals(89917.56989589504, standardValues[1], epsilon);
        Assert.assertEquals(0.26375169160084233, standardValues[2], epsilon);

        standardValues = TroposphericCorrection.computeStandardValues(10000.);
        Assert.assertEquals(226.14999999999998, standardValues[0], epsilon);
        Assert.assertEquals(26569.790617728395, standardValues[1], epsilon);
        Assert.assertEquals(8.341084062329147E-4, standardValues[2], epsilon);
    }
}
