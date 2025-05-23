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
 * @history Created 15/04/2015
 *
 * HISTORY
 * VERSION:4.13:DM:DM-103:08/12/2023:[PATRIUS] Optimisation du CIRFProvider
 * VERSION:4.13:DM:DM-108:08/12/2023:[PATRIUS] Modele d'obliquite et de precession de la Terre
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:317:15/04/2015: STELA integration in CIRF with referential choice (ICRF, CIRF or MOD)
 * VERSION::DM:660:24/09/2016:add getters to frames configuration
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.frames.configuration.eop.PoleCorrection;
import fr.cnes.sirius.patrius.frames.configuration.precessionnutation.CIPCoordinates;
import fr.cnes.sirius.patrius.frames.configuration.precessionnutation.StelaPrecessionNutationModel;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for Stela frame configuration and underlying {@link StelaPrecessionNutationModel}.
 * 
 * @author Emmanuel Bignon
 * @version $Id: StelaFramesConfigurationTest.java 18088 2017-10-02 17:01:51Z bignon $
 * @since 3.0
 */
public class StelaFramesConfigurationTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle STELA frame configuration test
         * 
         * @featureDescription test STELA specific frame configuration (CIRF frame model)
         * 
         * @coveredRequirements
         */
        STELA_FRAME_CONFIGURATION,
    }

    /**
     * @throws PatriusException
     *         thrown if computation fails
     * @testType UT
     * @testedFeature {@link features#STELA_FRAME_CONFIGURATION}
     * @testedMethod {@link StelaPrecessionNutationModel#getCIPMotion(AbsoluteDate)} and
     *               {@link StelaPrecessionNutationModel#getCIPMotionTimeDerivative(AbsoluteDate)}
     * @description tests STELA specific frame configuration (in particular CIP motion)
     * @input date
     * @output CIP parameters and its time derivatives
     * @testPassCriteria CIP parameters and its time derivatives are the same as STELA-LOS at 1E-14 (x, y, s) and 1E-11
     *                   (xdot, ydot, sdot).
     *                   Tolerance are smaller and time derivatives since values are very close to zero (array 1E-25).
     *                   Other parameters return 0 or null.
     * @referenceVersion 3.0
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testConfiguration() throws PatriusException {

        final FramesConfiguration config = FramesConfigurationFactory.getStelaConfiguration();

        // Input data
        final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(86400. * 36525 + 35);
        final CIPCoordinates cipCoor = config.getCIPCoordinates(date);

        // Check CIP motion time derivatives
        final double[] cipMotionDot = cipCoor.getCIPMotionTimeDerivatives();
        final double[] expcipMotionDot = new double[] { 3.943038448025564E-12, -3.772267703167182E-14,
            1.200522242427558E-16 };
        checkDoubleRel(expcipMotionDot, cipMotionDot, 1E-11);
        // Check CIP motion
        final double[] cipMotion = cipCoor.getCIPMotion();
        final double[] expcipMotion = new double[] { 0.0048864281689152385, -5.35914364524393E-5, 9.61875385627665E-8 };
        checkDoubleRel(expcipMotion, cipMotion, 1E-14);
        // Check others
        final PoleCorrection poleCorr = config.getPolarMotion(date);
        Assert.assertEquals(0., poleCorr.getXp(), 0.);
        Assert.assertEquals(0., poleCorr.getYp(), 0.);
        Assert.assertEquals(0., config.getSprime(date), 0.);
        Assert.assertNotNull(config.getCIRFPrecessionNutationModel());
        Assert.assertEquals(FrameConvention.STELA, config.getCIRFPrecessionNutationModel().getPrecessionNutationModel()
            .getOrigin());
    }

    /**
     * Relative check for double[] array.
     */
    public static void checkDoubleRel(final double[] dExpected, final double[] dActual, final double tol) {
        Assert.assertEquals(dExpected.length, dActual.length);
        for (int i = 0; i < dExpected.length; i++) {
            Assert.assertEquals(0, MathLib.abs((dExpected[i] - dActual[i]) / dExpected[i]), tol);
        }
    }
}
