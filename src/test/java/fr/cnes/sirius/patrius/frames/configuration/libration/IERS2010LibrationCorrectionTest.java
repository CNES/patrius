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
 * @history creation 18/10/2012
 * 
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:144:17/12/2013:Corrected elapsed seconds computation (was in UTC)
 * VERSION::DM:660:24/09/2016:add getters to frames configuration
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.libration;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.configuration.FrameConvention;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for {@link IERS2010LibrationCorrection} model.
 * 
 * @author Rami Houdroge
 * @since 1.3
 * @version $Id: IERS2010LibrationCorrectionTest.java 18089 2017-10-02 17:02:50Z bignon $
 */
public class IERS2010LibrationCorrectionTest {

    /** threshold. */
    private final double eps = Precision.EPSILON;

    /**
     * test.
     * 
     * @throws PatriusException
     *         if fails
     */
    @Test
    public void testGetPoleCorrection() throws PatriusException {

        Utils.setDataRoot("regular-data");

        final IERS2010LibrationCorrection lib = new IERS2010LibrationCorrection();

        final double MICRO_ARC_SECONDS_TO_RADIANS = Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6;

        // new values provided by CNES as of 17/12/2013
        //
        // rmkd 54335
        // dx 24.65518398386097942
        // dy -14.11070254891893327
        final AbsoluteDate date = AbsoluteDate.MODIFIED_JULIAN_EPOCH.shiftedBy(54335. * 86400.);
        final double expXp = 24.65518398386097942 * MICRO_ARC_SECONDS_TO_RADIANS;
        final double expYp = -14.11070254891893327 * MICRO_ARC_SECONDS_TO_RADIANS;
        final double eps = 1e-6;

        final double actXp = lib.getPoleCorrection(date).getXp();
        final double actYp = lib.getPoleCorrection(date).getYp();

        final double relXp = (expXp - actXp) / expXp;
        final double relYp = (expYp - actYp) / expYp;

        System.out.println((expXp - actXp));
        System.out.println((expYp - actYp));

        Assert.assertEquals(0, relXp, eps);
        Assert.assertEquals(0, relYp, eps);
    }

    /**
     * test.
     */
    @Test
    public void testGetUT1MinusUTCCorrection() {
        final IERS2010LibrationCorrection lib = new IERS2010LibrationCorrection();

        Assert.assertEquals(0, lib.getUT1Correction(AbsoluteDate.J2000_EPOCH), this.eps);
        Assert.assertEquals(0, lib.getUT1Correction(AbsoluteDate.MODIFIED_JULIAN_EPOCH), this.eps);
    }

    /**
     * Test method getOrigin().
     */
    @Test
    public void testGetters() {
        final LibrationCorrectionModel librationCorrection2 = LibrationCorrectionModelFactory.LIBRATION_IERS2010;
        Assert.assertEquals(FrameConvention.IERS2010, librationCorrection2.getOrigin());
    }

}
