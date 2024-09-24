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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:660:24/09/2016:add getters to frames configuration
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.libration;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.frames.configuration.FrameConvention;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * Test class for {@link NoLibrationCorrection} model.
 * 
 * @author Rami Houdroge
 * @since 1.3
 * @version $Id: NoLibrationCorrectionTest.java 18089 2017-10-02 17:02:50Z bignon $
 */
public class NoLibrationCorrectionTest {

    /** threshold. */
    private final double eps = Precision.EPSILON;

    /** model. */
    private final NoLibrationCorrection nolib = new NoLibrationCorrection();

    /**
     * test.
     */
    @Test
    public void testNoSP() {
        Assert.assertEquals(0, this.nolib.getUT1Correction(AbsoluteDate.J2000_EPOCH), this.eps);
        Assert.assertEquals(0, this.nolib.getPoleCorrection(AbsoluteDate.J2000_EPOCH).getXp(), this.eps);
        Assert.assertEquals(0, this.nolib.getPoleCorrection(AbsoluteDate.J2000_EPOCH).getYp(), this.eps);
    }

    /**
     * Test method getOrigin().
     */
    @Test
    public void testGetters() {
        final LibrationCorrectionModel librationCorrection1 = LibrationCorrectionModelFactory.NO_LIBRATION;
        Assert.assertEquals(FrameConvention.NONE, librationCorrection1.getOrigin());
    }

}
