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
 * @history creation 19/10/2012
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:660:24/09/2016:add getters to frames configuration
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.tides;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.frames.configuration.FrameConvention;
import fr.cnes.sirius.patrius.frames.configuration.eop.PoleCorrection;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * Test class for {@link TidalCorrection}.
 * 
 * @author Rami Houdroge
 * @since 1.3
 * @version $Id: TidalCorrectionTest.java 18088 2017-10-02 17:01:51Z bignon $
 * 
 */
public class TidalCorrectionTest {

    /** Container. */
    private TidalCorrection tcr;

    /** Test constructor. */
    @Before
    public void testTidesCorrection() {
        this.tcr = new TidalCorrection(AbsoluteDate.J2000_EPOCH, new PoleCorrection(1, 2), 3, 5);
    }

    /**
     * Test pole corrections.
     */
    @Test
    public void testGetPoleCorrection() {
        Assert.assertEquals(1, this.tcr.getPoleCorrection().getXp(), Precision.EPSILON);
        Assert.assertEquals(2, this.tcr.getPoleCorrection().getYp(), Precision.EPSILON);
    }

    /**
     * Test UT1-UTC correction.
     */
    @Test
    public void testGetUT1mUTCCorrection() {
        Assert.assertEquals(3, this.tcr.getUT1Correction(), Precision.EPSILON);
    }

    /**
     * Test LOD correction.
     */
    @Test
    public void testLODCorrection() {
        Assert.assertEquals(5, this.tcr.getLODCorrection(), Precision.EPSILON);
    }

    /**
     * Test date.
     */
    @Test
    public void testGetDate() {
        Assert.assertEquals(0, AbsoluteDate.J2000_EPOCH.durationFrom(this.tcr.getDate()), Precision.EPSILON);
    }

    /**
     * Test methods getOrigin() and isDirect() for all tidal corrections.
     */
    @Test
    public void testGetters() {
        final TidalCorrectionModel tidalCorrection1 = TidalCorrectionModelFactory.NO_TIDE;
        Assert.assertEquals(FrameConvention.NONE, tidalCorrection1.getOrigin());
        Assert.assertEquals(true, tidalCorrection1.isDirect());

        final TidalCorrectionModel tidalCorrection2 = TidalCorrectionModelFactory.TIDE_IERS2003_DIRECT;
        Assert.assertEquals(FrameConvention.IERS2003, tidalCorrection2.getOrigin());
        Assert.assertEquals(true, tidalCorrection2.isDirect());

        final TidalCorrectionModel tidalCorrection3 = TidalCorrectionModelFactory.TIDE_IERS2003_INTERPOLATED;
        Assert.assertEquals(FrameConvention.IERS2003, tidalCorrection3.getOrigin());
        Assert.assertEquals(false, tidalCorrection3.isDirect());

        final TidalCorrectionModel tidalCorrection4 = TidalCorrectionModelFactory.TIDE_IERS2010_DIRECT;
        Assert.assertEquals(FrameConvention.IERS2010, tidalCorrection4.getOrigin());
        Assert.assertEquals(true, tidalCorrection4.isDirect());

        final TidalCorrectionModel tidalCorrection5 = TidalCorrectionModelFactory.TIDE_IERS2010_INTERPOLATED;
        Assert.assertEquals(FrameConvention.IERS2010, tidalCorrection5.getOrigin());
        Assert.assertEquals(false, tidalCorrection5.isDirect());
    }
}
