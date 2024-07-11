/**
 *
 * Copyright 2011-2022 CNES
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
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.sp;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * Test class for NoSP model.
 * 
 * @author Rami Houdroge
 * @since 1.3
 * @version $Id: NoSpCorrectionTest.java 18089 2017-10-02 17:02:50Z bignon $
 */
public class NoSpCorrectionTest {

    /** threshold. */
    private final double eps = Precision.EPSILON;

    /** model. */
    private final NoSpCorrection nosp = new NoSpCorrection();

    /**
     * test.
     */
    @Test
    public void testNoSP() {
        Assert.assertEquals(0, this.nosp.getSP(AbsoluteDate.J2000_EPOCH), this.eps);
        Assert.assertEquals(0, this.nosp.getSP(AbsoluteDate.PAST_INFINITY), this.eps);
        Assert.assertEquals(0, this.nosp.getSP(AbsoluteDate.FUTURE_INFINITY), this.eps);
        Assert.assertEquals(0, this.nosp.getSP(AbsoluteDate.FIFTIES_EPOCH_TT), this.eps);
    }

}
