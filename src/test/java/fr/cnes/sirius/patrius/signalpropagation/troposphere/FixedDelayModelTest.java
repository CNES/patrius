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
 * VERSION::DM:88:18/11/2013: update due to the refactoring of the tropospheric correction classes
 * VERSION::FA:306:12/11/2014:coverage
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.signalpropagation.troposphere;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.signalpropagation.troposphere.FixedDelayModel;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class FixedDelayModelTest {

    private static double epsilon = 1e-6;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(FixedDelayModelTest.class.getSimpleName(), "Fixed delay tropospheric correction");
    }

    @Test
    public void testModel1() throws PatriusException {

        Report.printMethodHeader("testModel1", "Path delay computation", "Unknown", epsilon, ComparisonType.ABSOLUTE);

        // check with (artificial) test values from tropospheric-delay.txt
        final FixedDelayModel model = FixedDelayModel.getDefaultModel(0d);
        Assert.assertEquals(2.5d, model.computePathDelay(90d), epsilon);
        Assert.assertEquals(20.8d, model.computePathDelay(0d), epsilon);

        Report.printToReport("Path delay (elevation: 90°)", 2.5d, model.computePathDelay(90d));
        Report.printToReport("Path delay (elevation: 0°)", 20.8d, model.computePathDelay(0d));
    }

    @Test
    public void testModel2() throws PatriusException {
        // check with (artificial) test values from tropospheric-delay.txt
        final FixedDelayModel model = new FixedDelayModel("^tropospheric-delay\\.txt$", 5000d);
        Assert.assertEquals(12.1d, model.computePathDelay(0d), epsilon);
        Assert.assertEquals(2.5d, model.computePathDelay(90d), epsilon);
        // sanity checks
        Assert.assertEquals(12.1d, model.computePathDelay(-20d), epsilon);
    }

    @Test
    public void testModel3() throws PatriusException {
        // interpolation between two elevation angles in the table
        final FixedDelayModel model = new FixedDelayModel("^tropospheric-delay\\.txt$", 1200d);
        final double delay = model.computePathDelay(35d);
        Assert.assertTrue(Precision.compareTo(delay, 6.4d, epsilon) < 0);
        Assert.assertTrue(Precision.compareTo(delay, 3.2d, epsilon) > 0);
    }

    @Test
    public void testModel4() throws PatriusException {
        // interpolation between two elevation angles in the table
        final FixedDelayModel model = new FixedDelayModel("^tropospheric-delay\\.txt$", 100000d);
        // sanity checks
        Assert.assertEquals(2.5d, model.computePathDelay(90d), epsilon);
    }

    // test for coverage of the class FixedDelayModel
    @Test
    public void testCoverage() throws PatriusException {
        final double[] xArr = new double[] { 0., 1., 2., 3. };
        final double[] yArr = new double[] { 0., 1., 2., 3. };
        final double[][] fArr = new double[][] { { 0., 2., 4., 6. }, { 3., 4., 5., 6. },
            { 3., 5., 8., 15. }, { 0., 5., 15., 20. } };
        // Test the first constructor:
        Assert.assertNotNull(new FixedDelayModel(xArr, yArr, fArr, 100.0));
        // Test the exception in the second constructor:
        boolean rez = false;
        try {
            new FixedDelayModel("wrong_name", 100000d);
        } catch (final PatriusException e) {
            rez = true;
        }
        Assert.assertTrue(rez);
        // Test the computePathDelay method:
        final FixedDelayModel model = new FixedDelayModel("^tropospheric-delay\\.txt$", 100000d);
        Assert.assertEquals(model.computePathDelay(90d) / Constants.SPEED_OF_LIGHT, model.computeSignalDelay(90d),
            epsilon);

    }

    @Test
    public void testSymmetry() throws PatriusException {
        for (int elevation = 0; elevation < 90; elevation += 10) {
            final FixedDelayModel model = new FixedDelayModel("^tropospheric-delay\\.txt$", 100d);
            final double delay1 = model.computePathDelay(elevation);
            final double delay2 = model.computePathDelay(180 - elevation);

            Assert.assertEquals(delay1, delay2, epsilon);
        }
    }

    @BeforeClass
    public static void setUpGlobal() {
        Utils.setDataRoot("atmosphereOrekit");
    }
}