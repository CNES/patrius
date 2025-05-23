/**
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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.analytical.tle;

import org.junit.Test;

import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class LevenbergMarquardtOrbitConverterTest extends AbstractTLEFitterTest {

    @Test
    public void testConversionGeoPositionVelocity() throws PatriusException {
        this.checkFit(this.getGeoTLE(), 86400, 300, 1.0e-3, false, false, 8.600e-8);
    }

    @Test
    public void testConversionGeoPositionOnly() throws PatriusException {
        this.checkFit(this.getGeoTLE(), 86400, 300, 1.0e-3, true, false, 1.059e-7);
    }

    @Test
    public void testConversionLeoPositionVelocityWithoutBStar() throws PatriusException {
        this.checkFit(this.getLeoTLE(), 86400, 300, 1.0e-3, false, false, 10.77);
    }

    @Test
    public void testConversionLeoPositionOnlyWithoutBStar() throws PatriusException {
        this.checkFit(this.getLeoTLE(), 86400, 300, 1.0e-3, true, false, 15.23);
    }

    @Test
    public void testConversionLeoPositionVelocityWithBStar() throws PatriusException {
        this.checkFit(this.getLeoTLE(), 86400, 300, 1.0e-3, false, true, 9.920e-7);
    }

    @Test
    public void testConversionLeoPositionOnlyWithBStar() throws PatriusException {
        this.checkFit(this.getLeoTLE(), 86400, 300, 1.0e-3, true, true, 1.147e-6);
    }

    @Override
    protected AbstractTLEFitter getFitter(final TLE tle) {
        return new LevenbergMarquardtOrbitConverter(1000,
            tle.getSatelliteNumber(), tle.getClassification(),
            tle.getLaunchYear(), tle.getLaunchNumber(), tle.getLaunchPiece(),
            tle.getElementNumber(), tle.getRevolutionNumberAtEpoch());
    }

}
