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
 * @history creation 19/10/2012
 *
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:660:24/09/2016:add getters to frames configuration
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.precessionnutation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.frames.configuration.FrameConvention;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;

public class NoPrecessionNutationTest {

    private final double eps = Precision.EPSILON;

    @Test
    public void testGetCIPMotion() {

        final AbsoluteDate date = new AbsoluteDate(2000, 11, 20, TimeScalesFactory.getTAI());
        assertEquals(0, PrecessionNutationModelFactory.NO_PN.getCIPMotion(date)[0], this.eps);
        assertEquals(0, PrecessionNutationModelFactory.NO_PN.getCIPMotion(date)[1], this.eps);
        assertEquals(0, PrecessionNutationModelFactory.NO_PN.getCIPMotion(date)[2], this.eps);
    }

    @Test
    public void testGetCIPMotionTimeDerivatives() {

        final AbsoluteDate date = new AbsoluteDate(2000, 11, 20, TimeScalesFactory.getTAI());
        assertEquals(0, PrecessionNutationModelFactory.NO_PN.getCIPMotionTimeDerivative(date)[0], this.eps);
        assertEquals(0, PrecessionNutationModelFactory.NO_PN.getCIPMotionTimeDerivative(date)[1], this.eps);
        assertEquals(0, PrecessionNutationModelFactory.NO_PN.getCIPMotionTimeDerivative(date)[2], this.eps);
    }

    @Test
    public void testIsDirect() {
        assertTrue(PrecessionNutationModelFactory.NO_PN.isDirect());
    }

    /**
     * Test methods getOrigin() and isConstant().
     */
    @Test
    public void testGetters() {
        final PrecessionNutationModel pnModel = PrecessionNutationModelFactory.NO_PN;
        Assert.assertEquals(FrameConvention.NONE, pnModel.getOrigin());
        Assert.assertEquals(true, pnModel.isConstant());
    }

}
