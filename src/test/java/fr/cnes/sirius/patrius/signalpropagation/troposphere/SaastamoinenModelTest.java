/**
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
 */
/* Copyright 2011-2012 Space Applications Services
 *
 * HISTORY
* VERSION:4.8:DM:DM-2929:15/11/2021:[PATRIUS] Harmonisation des modeles de troposphere 
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:88:18/11/2013: update due to the refactoring of the SaastamoinenModel class
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.signalpropagation.troposphere;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.signalpropagation.troposphere.SaastamoinenModel;
import fr.cnes.sirius.patrius.utils.Constants;

public class SaastamoinenModelTest {

    private static double epsilon = 1e-6;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(SaastamoinenModelTest.class.getSimpleName(), "Saastamoinen tropospheric correction");
    }

    /**
     * Pure non-regression test: there is not associated reference.
     */
    @Test
    public void testDelay() {

        Report.printMethodHeader("testDelay", "Path delay computation", "Non-regression", 0, ComparisonType.ABSOLUTE);

        final double elevation = MathLib.toRadians(10d);
        final double height = 100d;
        final SaastamoinenModel model = SaastamoinenModel.getStandardModel(height);
        final double delay = model.computeSignalDelay(elevation);
        final double path = model.calculatePathDelay(elevation);

        Assert.assertEquals(path / Constants.SPEED_OF_LIGHT, delay, 0);
        Assert.assertEquals(path, 1.3187868028620693E1, 0);

        Report.printToReport("Path delay (elevation: 10°)", 1.3187868028620693E1, path);
    }

    @Test
    public void testFixedElevation() {
        double lastDelay = Double.MAX_VALUE;
        // delay shall decline with increasing height of the station
        for (double height = 0; height < 5000; height += 100) {
            final SaastamoinenModel model = SaastamoinenModel.getStandardModel(height);
            final double delay = model.calculatePathDelay(MathLib.toRadians(5));
            Assert.assertTrue(Precision.compareTo(delay, lastDelay, epsilon) < 0);
            lastDelay = delay;
        }
    }

    @Test
    public void testFixedHeight() {
        double lastDelay = Double.MAX_VALUE;
        // delay shall decline with increasing elevation angle
        final SaastamoinenModel model = SaastamoinenModel.getStandardModel(350);
        for (double elev = 10d; elev < 90d; elev += 8d) {
            final double delay = model.calculatePathDelay(MathLib.toRadians(elev));
            Assert.assertTrue(Precision.compareTo(delay, lastDelay, epsilon) < 0);
            lastDelay = delay;
        }
    }

    @Test
    @Ignore
    public void testPerformance() {
        final double elevation = 10d;

        final long RUNS = 100000;
        final long start = System.currentTimeMillis();
        final SaastamoinenModel model = SaastamoinenModel.getStandardModel(350);
        for (int i = 0; i < RUNS; i++) {
            model.computeSignalDelay(MathLib.toRadians(elevation));
        }

        System.out.println(RUNS + " runs took " + (System.currentTimeMillis() - start) + "ms");
    }

    @BeforeClass
    public static void setUpGlobal() {
        Utils.setDataRoot("atmosphereOrekit");
    }
}
