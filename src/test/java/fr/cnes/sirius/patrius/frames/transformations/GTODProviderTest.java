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
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:489:12/01/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::FA:1301:06/09/2017:Generalized EOP history
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.transformations;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.DiurnalRotation;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfiguration;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationBuilder;
import fr.cnes.sirius.patrius.frames.configuration.eop.EOPHistoryFactory;
import fr.cnes.sirius.patrius.frames.configuration.libration.LibrationCorrectionModelFactory;
import fr.cnes.sirius.patrius.frames.configuration.tides.TidalCorrectionModelFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class GTODProviderTest {

    private FramesConfiguration svgConfig = null;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(GTODProviderTest.class.getSimpleName(), "GTOD frame");
    }

    @Test
    public void testAASReferenceLEO() throws PatriusException {

        Report.printMethodHeader("testAASReferenceLEO", "Frame conversion", "Vallado paper", 0.01,
            ComparisonType.ABSOLUTE);

        // this reference test has been extracted from the following paper:
        // Implementation Issues Surrounding the New IAU Reference Systems for Astrodynamics
        // David A. Vallado, John H. Seago, P. Kenneth Seidelmann
        // http://www.centerforspace.com/downloads/files/pubs/AAS-06-134.pdf
        final AbsoluteDate t0 = new AbsoluteDate(new DateComponents(2004, 04, 06),
            new TimeComponents(07, 51, 28.386009),
            TimeScalesFactory.getUTC());

        // GTOD iau76
        final PVCoordinates pvGTOD =
            new PVCoordinates(new Vector3D(-1033475.0313, 7901305.5856, 6380344.5328),
                new Vector3D(-3225.632747, -2872.442511, 5531.931288));

        // TOD iau76
        final PVCoordinates pvTOD =
            new PVCoordinates(new Vector3D(5094514.7804, 6127366.4612, 6380344.5328),
                new Vector3D(-4746.088567, 786.077222, 5531.931288));

        Transform t = FramesFactory.getTOD(true).getTransformTo(FramesFactory.getGTOD(true), t0);
        // Test the getTrasform methods of GTODProvider:
        final Transform t2 = new GTODProvider().getTransform(t0);
        final Transform t3 = new GTODProvider().getTransform(t0, false);
        final double gap = t.getRotation().applyInverseTo(t2.getRotation()).getAngle();
        final double gap2 = t.getRotation().applyInverseTo(t3.getRotation()).getAngle();
        Assert.assertEquals(0.0, gap, Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(0.0, gap2, Precision.DOUBLE_COMPARISON_EPSILON);

        // this test gives worse result than GTODFrameAlternateConfigurationTest because
        // at 2004-04-06 there is a 0.471ms difference in dut1 and a 0.077ms difference
        // in lod with the data used by Vallado to set up this test case
        PVCoordinates delta = new PVCoordinates(t.transformPVCoordinates(pvTOD), pvGTOD);
        Assert.assertEquals(0.29, delta.getPosition().getNorm(), 0.01);
        Assert.assertEquals(1.6e-4, delta.getVelocity().getNorm(), 1.0e-5);

        Report.printToReport("Position (with EOP correction)", pvGTOD.getPosition(), t.transformPVCoordinates(pvTOD)
            .getPosition());
        Report.printToReport("Velocity (with EOP correction)", pvGTOD.getVelocity(), t.transformPVCoordinates(pvTOD)
            .getVelocity());

        // even if lod correction is ignored, results are quite the same
        t = FramesFactory.getTOD(false).getTransformTo(FramesFactory.getGTOD(false), t0);
        delta = new PVCoordinates(t.transformPVCoordinates(pvTOD), pvGTOD);
        Assert.assertEquals(0.29, delta.getPosition().getNorm(), 0.01);
        Assert.assertEquals(1.6e-4, delta.getVelocity().getNorm(), 1.0e-5);

        Report.printToReport("Position (no EOP correction)", pvGTOD.getPosition(), t.transformPVCoordinates(pvTOD)
            .getPosition());
        Report.printToReport("Velocity (no EOP correction)", pvGTOD.getVelocity(), t.transformPVCoordinates(pvTOD)
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

        Transform t = FramesFactory.getTOD(true).getTransformTo(FramesFactory.getGTOD(true), t0);

        // TOD iau76
        final PVCoordinates pvTOD =
            new PVCoordinates(new Vector3D(-40577427.7501, -11500096.1306, 10293.2583),
                new Vector3D(837.552338, -2957.524176, -0.928772));

        // GTOD iau76
        final PVCoordinates pvGTOD =
            new PVCoordinates(new Vector3D(24796919.2956, -34115870.9001, 10293.2583),
                new Vector3D(-0.979178, -1.476540, -0.928772));

        // this test gives worst result than GTODFrameAlternateConfigurationTest because
        // at 2004-06-01 there is a 0.047ms difference in dut1 and a 0.416ms difference
        // in lod with the data used by Vallado to set up this test case
        PVCoordinates delta = new PVCoordinates(t.transformPVCoordinates(pvTOD), pvGTOD);
        Assert.assertEquals(0.195813, delta.getPosition().getNorm(), 1.0e-6);
        Assert.assertEquals(1.427468e-5, delta.getVelocity().getNorm(), 9.0e-12);

        // even if lod correction is ignored, results are quite the same
        t = FramesFactory.getTOD(false).getTransformTo(FramesFactory.getGTOD(false), t0);
        delta = new PVCoordinates(t.transformPVCoordinates(pvTOD), pvGTOD);
        Assert.assertEquals(0.195813, delta.getPosition().getNorm(), 5.0e-7);
        Assert.assertEquals(1.427468e-5, delta.getVelocity().getNorm(), 9.0e-12);

    }

    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("compressed-data");

        this.svgConfig = FramesFactory.getConfiguration();
        final FramesConfigurationBuilder builder = new FramesConfigurationBuilder(this.svgConfig);
        builder.setDiurnalRotation(new DiurnalRotation(TidalCorrectionModelFactory.NO_TIDE,
            LibrationCorrectionModelFactory.NO_LIBRATION));
        builder.setEOPHistory(EOPHistoryFactory.getEOP1980History());
        FramesFactory.setConfiguration(builder.getConfiguration());
    }

    @After
    public void tearDown() {
        FramesFactory.setConfiguration(this.svgConfig);
    }

}
