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
 * VERSION::FA:306:12/11/2014:coverage
 * VERSION::DM:317:06/03/2015: STELA integration in CIRF with referential choice (ICRF, CIRF or MOD)
 * VERSION::DM:489:12/01/2016:Refactoring of rotation acceleration by the AttitudeProvider
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

public class MODProviderTest {

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(MODProviderTest.class.getSimpleName(), "MOD frame");
    }

    @Test
    public void testAASReferenceLEO() throws PatriusException {

        Report.printMethodHeader("testAASReferenceLEO", "Frame conversion", "Vallado paper", 4.3e-5,
            ComparisonType.ABSOLUTE);

        // this reference test has been extracted from the following paper:
        // Implementation Issues Surrounding the New IAU Reference Systems for Astrodynamics
        // David A. Vallado, John H. Seago, P. Kenneth Seidelmann
        // http://www.centerforspace.com/downloads/files/pubs/AAS-06-134.pdf
        final AbsoluteDate t0 = new AbsoluteDate(new DateComponents(2004, 04, 06),
            new TimeComponents(07, 51, 28.386009),
            TimeScalesFactory.getUTC());

        final Transform tt = FramesFactory.getGCRF().getTransformTo(FramesFactory.getMOD(true), t0);
        // GCRF iau76 w corr
        final PVCoordinates pvGCRFiau76 =
            new PVCoordinates(new Vector3D(5102508.9579, 6123011.4007, 6378136.9282),
                new Vector3D(-4743.220157, 790.536497, 5533.755727));
        // MOD iau76 w corr
        final PVCoordinates pvMODiau76Wcorr =
            new PVCoordinates(new Vector3D(5094028.3745, 6127870.8164, 6380248.5164),
                new Vector3D(-4746.263052, 786.014045, 5531.790562));

        this.checkPV(pvMODiau76Wcorr, tt.transformPVCoordinates(pvGCRFiau76), 2.6e-5, 7.2e-7);

        final Transform tf = FramesFactory.getEME2000().getTransformTo(FramesFactory.getMOD(false), t0);
        // J2000 iau76
        final PVCoordinates pvJ2000iau76 =
            new PVCoordinates(new Vector3D(5102509.6000, 6123011.5200, 6378136.3000),
                new Vector3D(-4743.219600, 790.536600, 5533.756190));
        // MOD iau76
        final PVCoordinates pvMODiau76 =
            new PVCoordinates(new Vector3D(5094029.0167, 6127870.9363, 6380247.8885),
                new Vector3D(-4746.262495, 786.014149, 5531.791025));
        this.checkPV(pvMODiau76, tf.transformPVCoordinates(pvJ2000iau76), 4.3e-5, 2.7e-7);

        Report.printToReport("Position", pvMODiau76.getPosition(), tf.transformPVCoordinates(pvJ2000iau76)
            .getPosition());
        Report.printToReport("Velocity", pvMODiau76.getVelocity(), tf.transformPVCoordinates(pvJ2000iau76)
            .getVelocity());

        // Cover the getTransform methods of the MODProvider class:
        Assert.assertNotNull(new MODProvider().getTransform(t0));
        Assert.assertNotNull(new MODProvider().getTransform(t0, false));
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

        final Transform tt = FramesFactory.getGCRF().getTransformTo(FramesFactory.getMOD(true), t0);
        // GCRF iau76 w corr
        final PVCoordinates pvGCRFiau76 =
            new PVCoordinates(new Vector3D(-40588150.3649, -11462167.0282, 27143.2028),
                new Vector3D(834.787457, -2958.305691, -1.172994));
        // MOD iau76 w corr
        final PVCoordinates pvMODiau76Wcorr =
            new PVCoordinates(new Vector3D(-40576822.6395, -11502231.5015, 9733.7842),
                new Vector3D(837.708020, -2957.480117, -0.814253));
        this.checkPV(pvMODiau76Wcorr, tt.transformPVCoordinates(pvGCRFiau76), 2.5e-5, 6.9e-7);

        final Transform tf = FramesFactory.getEME2000().getTransformTo(FramesFactory.getMOD(false), t0);
        // J2000 iau76
        final PVCoordinates pvJ2000iau76 =
            new PVCoordinates(new Vector3D(-40588150.3620, -11462167.0280, 27147.6490),
                new Vector3D(834.787457, -2958.305691, -1.173016));
        // MOD iau76
        final PVCoordinates pvMODiau76 =
            new PVCoordinates(new Vector3D(-40576822.6385, -11502231.5013, 9738.2304),
                new Vector3D(837.708020, -2957.480118, -0.814275));
        this.checkPV(pvMODiau76, tf.transformPVCoordinates(pvJ2000iau76), 3.3e-5, 6.9e-7);

    }

    @Test
    public void testGetEulerAngles() throws PatriusException {
        final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(86400. * 36525 + 35);
        final double[] euler = new MODProvider().getEulerAngles(date);
        // Expected Euler angles
        // Reference : STELA
        final double[] expectedAngles = new double[] { 0.005590807466558654, 0.004858044468503607,
            0.005591768491589964, 3.543485519560285E-12, 3.078482802373367E-12, 3.544703720066214E-12 };
        Assert.assertEquals(6, euler.length);
        for (int i = 0; i < euler.length; i++) {
            Assert.assertEquals(expectedAngles[i], euler[i], 1e-14);
        }
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("compressed-data");
    }

    private void checkPV(final PVCoordinates reference,
                         final PVCoordinates result,
                         final double positionThreshold,
                         final double velocityThreshold) {

        final Vector3D dP = result.getPosition().subtract(reference.getPosition());
        final Vector3D dV = result.getVelocity().subtract(reference.getVelocity());
        Assert.assertEquals(0, dP.getNorm(), positionThreshold);
        Assert.assertEquals(0, dV.getNorm(), velocityThreshold);
    }

}
