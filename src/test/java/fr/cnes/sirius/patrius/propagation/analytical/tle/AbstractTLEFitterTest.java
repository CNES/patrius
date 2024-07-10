/**
 * Copyright 2011-2017 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 */
package fr.cnes.sirius.patrius.propagation.analytical.tle;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Before;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public abstract class AbstractTLEFitterTest {

    private TLE geoTLE;

    private TLE leoTLE;

    protected TLE getGeoTLE() {
        return this.geoTLE;
    }

    protected TLE getLeoTLE() {
        return this.leoTLE;
    }

    protected abstract AbstractTLEFitter getFitter(TLE tle);

    protected void checkFit(final TLE tle, final double duration, final double stepSize,
                            final double positionTolerance, final boolean positionOnly,
                            final boolean withBStar, final double expectedRMS)
                                                                              throws PatriusException {

        final Propagator p = TLEPropagator.selectExtrapolator(tle);
        final List<SpacecraftState> sample = new ArrayList<SpacecraftState>();
        for (double dt = 0; dt < duration; dt += stepSize) {
            sample.add(p.propagate(tle.getDate().shiftedBy(dt)));
        }

        final AbstractTLEFitter fitter = this.getFitter(tle);

        fitter.toTLE(sample, positionTolerance, positionOnly, withBStar);
        Assert.assertEquals(expectedRMS, fitter.getRMS(), 0.01 * expectedRMS);

        final TLE fitted = fitter.getTLE();
        Assert.assertEquals(tle.getSatelliteNumber(), fitted.getSatelliteNumber());
        Assert.assertEquals(tle.getClassification(), fitted.getClassification());
        Assert.assertEquals(tle.getLaunchYear(), fitted.getLaunchYear());
        Assert.assertEquals(tle.getLaunchNumber(), fitted.getLaunchNumber());
        Assert.assertEquals(tle.getLaunchPiece(), fitted.getLaunchPiece());
        Assert.assertEquals(tle.getElementNumber(), fitted.getElementNumber());
        Assert.assertEquals(tle.getRevolutionNumberAtEpoch(), fitted.getRevolutionNumberAtEpoch());

        final double eps = 1.0e-5;
        Assert.assertEquals(tle.getMeanMotion(), fitted.getMeanMotion(), eps * tle.getMeanMotion());
        Assert.assertEquals(tle.getE(), fitted.getE(), eps * tle.getE());
        Assert.assertEquals(tle.getI(), fitted.getI(), eps * tle.getI());
        Assert.assertEquals(tle.getPerigeeArgument(), fitted.getPerigeeArgument(), eps * tle.getPerigeeArgument());
        Assert.assertEquals(tle.getRaan(), fitted.getRaan(), eps * tle.getRaan());
        Assert.assertEquals(tle.getMeanAnomaly(), fitted.getMeanAnomaly(), eps * tle.getMeanAnomaly());

    }

    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("regular-data");
        this.geoTLE =
            new TLE("1 27508U 02040A   12021.25695307 -.00000113  00000-0  10000-3 0  7326",
                "2 27508   0.0571 356.7800 0005033 344.4621 218.7816  1.00271798 34501");
        this.leoTLE =
            new TLE("1 31135U 07013A   11003.00000000  .00000816  00000+0  47577-4 0    11",
                "2 31135   2.4656 183.9084 0021119 236.4164  60.4567 15.10546832    15");
    }

}
