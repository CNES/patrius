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
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:317:02/03/2015: STELA integration in CIRF with referential choice (ICRF, CIRF or MOD)
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.bodies;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for {@link EarthRotation}.
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.3
 * 
 */
public class EarthRotationTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validate the Stela atmospheric drag aero model
         * 
         * @featureDescription Computation of atmospheric drag perturbations, and partial derivatives.
         * 
         * @coveredRequirements
         */
        STELA_ATMOSPHERIC_DRAG_MODEL
    }

    /** The epsilon for this test. */
    private static final double EPS = 1E-14;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(EarthRotationTest.class.getSimpleName(), "STELA Earth rotation");
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_ATMOSPHERIC_DRAG_MODEL}
     * 
     * @testedMethod {@link EarthRotation#getGMST(AbsoluteDate)}
     * 
     * @description tests the computation of the GMST (used for the atmospheric drag computation)
     * 
     * @input the date
     * 
     * @output the Greenwich Mean Sidereal Time.
     * 
     * @testPassCriteria references from Satlight V0
     * 
     * @referenceVersion 1.3
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testGetGMST() throws PatriusException {
        Report.printMethodHeader("testGetGMST", "GMST computation", "Satlight V0", EPS, ComparisonType.ABSOLUTE);
        final DateComponents datec = new DateComponents(1987, 11, 15);
        final TimeComponents timec = new TimeComponents(18, 37, 20.21273203194);
        final AbsoluteDate date = new AbsoluteDate(datec, timec, TimeScalesFactory.getTAI());
        final double actual = EarthRotation.getGMST(date) + 2. * FastMath.PI;
        final double expected = 5.820720993078614;
        Assert.assertEquals(expected, actual, EPS);
        Report.printToReport("GMST", expected, actual);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * @testedFeature {@link features#STELA_ATMOSPHERIC_DRAG_MODEL}
     * @testedMethod {@link EarthRotation#getERA(AbsoluteDate)}
     * @description tests the computation of the ERA
     * @input a date
     * @output ERA
     * @testPassCriteria same value as STELA v2.6
     * @referenceVersion 3.0
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testGetERA() throws PatriusException {
        Report.printMethodHeader("testGetERA", "ERA computation", "STELA 2.6", 0, ComparisonType.ABSOLUTE);
        final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(18264 * 86400 + 35);
        final double actual = EarthRotation.getERA(date);
        final double expected = 1.77917182859683;
        Assert.assertEquals(expected, actual, 0);
        Report.printToReport("ERA", expected, actual);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * @testedFeature {@link features#STELA_ATMOSPHERIC_DRAG_MODEL}
     * @testedMethod {@link EarthRotation#getERADerivative(AbsoluteDate)}
     * @description tests the computation of the ERA derivative
     * @input a date
     * @output ERA derivative
     * @testPassCriteria same value as STELA v2.6
     * @referenceVersion 3.0
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testGetERADerivative() throws PatriusException {
        Report.printMethodHeader("testGetERADerivative", "ERA derivative computation", "STELA 2.6", 0,
            ComparisonType.ABSOLUTE);
        final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(18264 * 86400 + 35);
        final double actual = EarthRotation.getERADerivative(date);
        final double expected = 7.29211514670698E-5;
        Assert.assertEquals(expected, actual, 0);
        Report.printToReport("ERA derivative", expected, actual);
    }
}
