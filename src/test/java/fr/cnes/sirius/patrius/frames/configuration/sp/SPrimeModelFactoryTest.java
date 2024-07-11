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
 * @history creation 18/10/2012
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:660:24/09/2016:add getters to frames configuration
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.sp;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.frames.configuration.FrameConvention;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;

/**
 * Test class for {@link SPrimeModelFactory}.
 * 
 * @author Rami Houdroge
 * @since 1.3
 * @version $Id: SPrimeModelFactoryTest.java 18089 2017-10-02 17:02:50Z bignon $
 */
public class SPrimeModelFactoryTest {

    /** threshold. */
    private final double eps = Precision.EPSILON;

    /** model. */
    private final SPrimeModel sp = SPrimeModelFactory.SP_IERS2010;

    /**
     * test.
     */
    @Test
    public void testGetSP() {
        Assert.assertEquals(0, this.sp.getSP(AbsoluteDate.J2000_EPOCH), this.eps);
        Assert.assertEquals(AbsoluteDate.FIFTIES_EPOCH_TT.durationFrom(AbsoluteDate.J2000_EPOCH) /
            Constants.JULIAN_CENTURY * -47e-6 * Constants.ARC_SECONDS_TO_RADIANS,
            this.sp.getSP(AbsoluteDate.FIFTIES_EPOCH_TT), this.eps);
    }

    /**
     * Test method getOrigin() for all s' corrections.
     */
    @Test
    public void testGetters() {
        final SPrimeModel spModel1 = SPrimeModelFactory.NO_SP;
        Assert.assertEquals(FrameConvention.NONE, spModel1.getOrigin());

        final SPrimeModel spModel2 = SPrimeModelFactory.SP_IERS2003;
        Assert.assertEquals(FrameConvention.IERS2003, spModel2.getOrigin());

        final SPrimeModel spModel3 = SPrimeModelFactory.SP_IERS2010;
        Assert.assertEquals(FrameConvention.IERS2010, spModel3.getOrigin());
    }

}
