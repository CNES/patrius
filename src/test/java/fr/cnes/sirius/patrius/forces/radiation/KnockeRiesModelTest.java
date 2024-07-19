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
 * @history created 23/04/12
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.radiation;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;

/**
 * This class tests the Knocke-Ries model.
 * 
 * @author clauded
 * 
 */
public class KnockeRiesModelTest {

    /** Epsilon for this model. */
    private static final double MDLEPS = 1.0e-14;

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Knocke-Ries Model
         * 
         * @featureDescription Knocke-Ries emissivity model
         * 
         * @coveredRequirements DV-MOD_300
         */
        KNOCKERIES_MODEL
    }

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(KnockeRiesModelTest.class.getSimpleName(), "Knocke-Ries emissivity model");
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#KNOCKERIES_MODEL}
     * 
     * @testedMethod {@link fr.cnes.sirius.patrius.forces.radiation.KnockeRiesModel#getEmissivity(AbsoluteDate, double, double)}
     * 
     * @description test the emissivities of Knocke-Ries Model
     * 
     * @input latitude = 0 and any day, latitude = PI / 2 and reference day
     * 
     * @output emissivities (albedo et infrared)
     * 
     * @testPassCriteria correct emissivities
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testgetKnockeRiesEmissivity() {

        Report.printMethodHeader("testgetKnockeRiesEmissivity", "Emissivity computation", "Unknown", MDLEPS,
            ComparisonType.ABSOLUTE);

        double[] e;
        AbsoluteDate d;

        final IEmissivityModel IModel = new KnockeRiesModel();

        final double eAlLat0 = 0.34 - 0.29 / 2;
        final double eIrLat0 = 0.68 + 0.18 / 2;
        d = new AbsoluteDate(AbsoluteDate.FIFTIES_EPOCH_TT, 20000 * Constants.JULIAN_DAY);
        e = IModel.getEmissivity(d, 0., 0.);
        Assert.assertEquals(eAlLat0, e[0], MDLEPS);
        Assert.assertEquals(eIrLat0, e[1], MDLEPS);

        d = new AbsoluteDate(AbsoluteDate.FIFTIES_EPOCH_TT, 100 * Constants.JULIAN_DAY);
        e = IModel.getEmissivity(d, 0., 0.);
        Assert.assertEquals(eAlLat0, e[0], MDLEPS);
        Assert.assertEquals(eIrLat0, e[1], MDLEPS);

        final double eAlJjul0 = 0.34 + 0.10 + 0.29;
        final double eIrJjul0 = 0.68 - 0.07 - 0.18;
        d = new AbsoluteDate(AbsoluteDate.FIFTIES_EPOCH_TT, 11678 * Constants.JULIAN_DAY);
        e = IModel.getEmissivity(d, FastMath.PI / 2., 0.);
        Assert.assertEquals(eAlJjul0, e[0], MDLEPS);
        Assert.assertEquals(eIrJjul0, e[1], MDLEPS);

        final double eAlJjul365 = eAlJjul0;
        final double eIrJjul365 = eIrJjul0;
        d = new AbsoluteDate(AbsoluteDate.FIFTIES_EPOCH_TT, (11678 + 4 * 365.25) * Constants.JULIAN_DAY);
        e = IModel.getEmissivity(d, FastMath.PI / 2., 0.);
        Assert.assertEquals(eAlJjul365, e[0], MDLEPS);
        Assert.assertEquals(eIrJjul365, e[1], MDLEPS);

        d = new AbsoluteDate(2005, 3, 5, 0, 24, 0.0, TimeScalesFactory.getTAI());
        e = IModel.getEmissivity(d, -0.8571953966, 0.);
        Assert.assertEquals(0.42051010216695, e[0], MDLEPS);
        Assert.assertEquals(0.63186142942111, e[1], MDLEPS);

        Report.printToReport("Albedo emissivity", 0.42051010216695, e[0]);
        Report.printToReport("IR emissivity", 0.63186142942111, e[1]);
    }
}
