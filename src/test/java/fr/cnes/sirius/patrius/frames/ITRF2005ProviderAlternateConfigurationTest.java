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
package fr.cnes.sirius.patrius.frames;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class ITRF2005ProviderAlternateConfigurationTest {

    @Test
    public void testAASReferenceLEO() throws PatriusException {

        // this reference test has been extracted from the following paper:
        // Implementation Issues Surrounding the New IAU Reference Systems for Astrodynamics
        // David A. Vallado, John H. Seago, P. Kenneth Seidelmann
        // http://www.centerforspace.com/downloads/files/pubs/AAS-06-134.pdf
        // Reference position & velocity from : "Fundamentals of Astrodynamics and Applications", Third edition, David
        // A. Vallado
        final AbsoluteDate t0 = new AbsoluteDate(new DateComponents(2004, 04, 06),
            new TimeComponents(07, 51, 28.386009),
            TimeScalesFactory.getUTC());

        // Positions LEO
        final Frame itrfA = FramesFactory.getITRF();
        final PVCoordinates pvITRF =
            new PVCoordinates(new Vector3D(-1033479.3830, 7901295.2754, 6380356.5958),
                new Vector3D(-3225.636520, -2872.451450, 5531.924446));

        // Reference coordinates
        final PVCoordinates pvGcrfIau2000A =
            new PVCoordinates(new Vector3D(5102508.9579, 6123011.4038, 6378136.9252),
                new Vector3D(-4743.220156, 790.536497, 5533.755728));
        this.checkPV(pvGcrfIau2000A,
            itrfA.getTransformTo(FramesFactory.getGCRF(), t0).transformPVCoordinates(pvITRF),
            0.012, 2.3e-5);

        final PVCoordinates pvEME2000EqA =
            new PVCoordinates(new Vector3D(5102509.0383, 6123011.9758, 6378136.3118),
                new Vector3D(-4743.219766, 790.536344, 5533.756084));
        this.checkPV(pvEME2000EqA,
            itrfA.getTransformTo(FramesFactory.getEME2000(), t0).transformPVCoordinates(pvITRF),
            0.012, 2.3e-5);

    }

    @Test
    public void testAASReferenceGEO() throws PatriusException {

        // this reference test has been extracted from the following paper:
        // Implementation Issues Surrounding the New IAU Reference Systems for Astrodynamics
        // David A. Vallado, John H. Seago, P. Kenneth Seidelmann
        // http://www.centerforspace.com/downloads/files/pubs/AAS-06-134.pdf
        final AbsoluteDate t0 = new AbsoluteDate(new DateComponents(2004, 06, 01),
            TimeComponents.H00,
            TimeScalesFactory.getUTC());

        // Positions GEO
        final Frame itrfA = FramesFactory.getITRF();
        final PVCoordinates pvITRF =
            new PVCoordinates(new Vector3D(24796919.2915, -34115870.9234, 10226.0621),
                new Vector3D(-0.979178, -1.476538, -0.928776));

        final PVCoordinates pvGCRFiau2000A =
            new PVCoordinates(new Vector3D(-40588150.3617, -11462167.0397, 27143.1974),
                new Vector3D(834.787458, -2958.305691, -1.172993));
        this.checkPV(pvGCRFiau2000A,
            itrfA.getTransformTo(FramesFactory.getGCRF(), t0).transformPVCoordinates(pvITRF),
            0.061, 1.1e-4);

        final PVCoordinates pvEME2000EqA =
            new PVCoordinates(new Vector3D(-40588149.5482, -11462169.9118, 27146.8462),
                new Vector3D(834.787667, -2958.305632, -1.172963));
        this.checkPV(pvEME2000EqA,
            itrfA.getTransformTo(FramesFactory.getEME2000(), t0).transformPVCoordinates(pvITRF),
            0.061, 1.1e-4);

    }

    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("testitrf-data");
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
    }

    private void checkPV(final PVCoordinates reference, final PVCoordinates result,
                         final double positionThreshold, final double velocityThreshold) {

        final Vector3D dP = result.getPosition().subtract(reference.getPosition());
        final Vector3D dV = result.getVelocity().subtract(reference.getVelocity());
        Assert.assertEquals(0, dP.getNorm(), positionThreshold);
        Assert.assertEquals(0, dV.getNorm(), velocityThreshold);
    }

}
