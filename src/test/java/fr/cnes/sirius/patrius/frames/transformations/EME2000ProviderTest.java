/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */
/*
 * 
 * HISTORY
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.transformations;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class EME2000ProviderTest {

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(EME2000ProviderTest.class.getSimpleName(), "EME2000 frame");
    }

    @Test
    public void testAASReferenceLEO() throws PatriusException {

        Report.printMethodHeader("testAASReferenceLEO", "Frame conversion", "Vallado paper", 7.4e-5,
            ComparisonType.ABSOLUTE);

        // this reference test has been extracted from the following paper:
        // Implementation Issues Surrounding the New IAU Reference Systems for Astrodynamics
        // David A. Vallado, John H. Seago, P. Kenneth Seidelmann
        // http://www.centerforspace.com/downloads/files/pubs/AAS-06-134.pdf
        final AbsoluteDate t0 = new AbsoluteDate(new DateComponents(2004, 04, 06),
            new TimeComponents(07, 51, 28.386009),
            TimeScalesFactory.getUTC());

        final Transform t = FramesFactory.getGCRF().getTransformTo(FramesFactory.getEME2000(), t0);

        final PVCoordinates pvGcrfIau2000A =
            new PVCoordinates(new Vector3D(5102508.9579, 6123011.4038, 6378136.9252),
                new Vector3D(-4743.220156, 790.536497, 5533.755728));
        final PVCoordinates pvEME2000EqA =
            new PVCoordinates(new Vector3D(5102509.0383, 6123011.9758, 6378136.3118),
                new Vector3D(-4743.219766, 790.536344, 5533.756084));
        this.checkPV(pvEME2000EqA, t.transformPVCoordinates(pvGcrfIau2000A), 1.1e-4, 2.6e-7);

        final PVCoordinates pvGcrfIau2000B =
            new PVCoordinates(new Vector3D(5102508.9579, 6123011.4012, 6378136.9277),
                new Vector3D(-4743.220156, 790.536495, 5533.755729));
        final PVCoordinates pvEME2000EqB =
            new PVCoordinates(new Vector3D(5102509.0383, 6123011.9733, 6378136.3142),
                new Vector3D(-4743.219766, 790.536342, 5533.756085));
        this.checkPV(pvEME2000EqB, t.transformPVCoordinates(pvGcrfIau2000B), 7.4e-5, 2.6e-7);

        Report.printToReport("Position", pvEME2000EqB.getPosition(), t.transformPVCoordinates(pvGcrfIau2000B)
            .getPosition());
        Report.printToReport("Velocity", pvEME2000EqB.getVelocity(), t.transformPVCoordinates(pvGcrfIau2000B)
            .getVelocity());
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

        final Transform t = FramesFactory.getGCRF().getTransformTo(FramesFactory.getEME2000(), t0);

        final PVCoordinates pvGCRFiau2000A =
            new PVCoordinates(new Vector3D(-40588150.3617, -11462167.0397, 27143.1974),
                new Vector3D(834.787458, -2958.305691, -1.172993));
        final PVCoordinates pvEME2000EqA =
            new PVCoordinates(new Vector3D(-40588149.5482, -11462169.9118, 27146.8462),
                new Vector3D(834.787667, -2958.305632, -1.172963));
        this.checkPV(pvEME2000EqA, t.transformPVCoordinates(pvGCRFiau2000A), 5.8e-5, 6.4e-7);

        final PVCoordinates pvGCRFiau2000B =
            new PVCoordinates(new Vector3D(-40588150.3617, -11462167.0397, 27143.2125),
                new Vector3D(834.787458, -2958.305691, -1.172999));
        final PVCoordinates pvEME2000EqB =
            new PVCoordinates(new Vector3D(-40588149.5481, -11462169.9118, 27146.8613),
                new Vector3D(834.787667, -2958.305632, -1.172968));
        this.checkPV(pvEME2000EqB, t.transformPVCoordinates(pvGCRFiau2000B), 1.1e-4, 5.5e-7);

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("compressed-data");
    }

    private void checkPV(final PVCoordinates reference,
                         final PVCoordinates result, final double positionThreshold,
                         final double velocityThreshold) {

        final Vector3D dP = result.getPosition().subtract(reference.getPosition());
        final Vector3D dV = result.getVelocity().subtract(reference.getVelocity());
        Assert.assertEquals(0, dP.getNorm(), positionThreshold);
        Assert.assertEquals(0, dV.getNorm(), velocityThreshold);
    }

}
