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
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.tides;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * Test class for {@link NoTidalCorrection} model.
 * 
 * @author Rami Houdroge
 * @version $Id: NoTidalCorrectionTest.java 18088 2017-10-02 17:01:51Z bignon $
 * @since 1.3
 */
public class NoTidalCorrectionTest {

    /** threshold. */
    private final double eps = Precision.EPSILON;

    /**
     * test.
     */
    @Test
    public void testGetPoleCorrection() {
        final NoTidalCorrection no = new NoTidalCorrection();

        Assert.assertEquals(0, no.getPoleCorrection(AbsoluteDate.J2000_EPOCH).getXp(), this.eps);
        Assert.assertEquals(0, no.getPoleCorrection(AbsoluteDate.J2000_EPOCH).getYp(), this.eps);
    }

    /**
     * test.
     */
    @Test
    public void testGetUT1MinusUTCCorrection() {
        final NoTidalCorrection no = new NoTidalCorrection();

        Assert.assertEquals(0, no.getUT1Correction(AbsoluteDate.J2000_EPOCH), this.eps);

    }

    /**
     * test.
     */
    @Test
    public void testGetLODCorrection() {
        final NoTidalCorrection no = new NoTidalCorrection();

        Assert.assertEquals(0, no.getLODCorrection(AbsoluteDate.J2000_EPOCH), this.eps);
        Assert.assertEquals(0, no.getLODCorrection(AbsoluteDate.PAST_INFINITY), this.eps);

    }

}
