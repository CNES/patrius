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
* VERSION:4.8:FA:FA-2941:15/11/2021:[PATRIUS] Correction anomalies suite a DM 2767 
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:306:12/11/2014:coverage
 * VERSION::DM:489:12/01/2016:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::DM:596:12/04/2016:Improve test coherence
 * VERSION::FA:1301:06/09/2017:Generalized EOP history
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationBuilder;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationFactory;
import fr.cnes.sirius.patrius.frames.configuration.eop.EOPHistoryFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.frames.transformations.VEISProvider;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class VEISFrameTest {

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(VEISFrameTest.class.getSimpleName(), "VEIS frame");
    }

    @Test
    public void testRefLEO() throws PatriusException {

        Report.printMethodHeader("testRefLEO", "Frame conversion", "Orekit", 9.0e-4, ComparisonType.ABSOLUTE);

        final AbsoluteDate date0 = new AbsoluteDate(new DateComponents(2004, 04, 06),
            new TimeComponents(07, 51, 28.386009),
            TimeScalesFactory.getUTC());

        final Transform t0 = FramesFactory.getEME2000().getTransformTo(FramesFactory.getVeis1950(), date0);

        // J2000
        final PVCoordinates pvJ2000 =
            new PVCoordinates(new Vector3D(5102509.6000, 6123011.5200, 6378136.3000),
                new Vector3D(-4743.219600, 790.536600, 5533.756190));

        // Veis (mslib90)
        final PVCoordinates pvVEIS =
            new PVCoordinates(new Vector3D(5168161.598034, 6065377.671130, 6380344.532758),
                new Vector3D(-4736.246465, 843.352600, 5531.931275));

        final PVCoordinates delta0 = new PVCoordinates(t0.transformPVCoordinates(pvJ2000), pvVEIS);
        Assert.assertEquals(0.0, delta0.getPosition().getNorm(), 9.0e-4);
        Assert.assertEquals(0.0, delta0.getVelocity().getNorm(), 5.0e-5);

        Report.printToReport("Position", pvVEIS.getPosition(), t0.transformPVCoordinates(pvJ2000).getPosition());
        Report.printToReport("Velocity", pvVEIS.getVelocity(), t0.transformPVCoordinates(pvJ2000).getVelocity());

        // Coverage test for VEISProvider: re-implment method
        // offset from FIFTIES epoch (UT1 scale)
        final double dtai = date0.durationFrom(new AbsoluteDate(DateComponents.FIFTIES_EPOCH, TimeScalesFactory
            .getTAI()));
        final double dut1 = EOPHistoryFactory.getEOP1980History().getUT1MinusTAI(date0);

        final double tut1 = dtai + dut1;
        final double ttd = tut1 / Constants.JULIAN_DAY;
        final double rdtt = ttd - (int) ttd;

        // compute Veis sidereal time, in radians
        /** 1st coef for Veis sidereal time computation in radians (100.075542 deg). */
        final double vst0 = 1.746647708617871;
        /** 2nd coef for Veis sidereal time computation in rad/s (0.985612288 deg/s). */
        final double vst1 = 0.17202179573714597e-1;
        final double vst = (vst0 + vst1 * ttd + MathUtils.TWO_PI * rdtt) % MathUtils.TWO_PI;
        /** Veis sidereal time derivative in rad/s. */
        final double vstd = 7.292115146705209e-5;

        // compute angular rotation of Earth, in rad/s
        final Vector3D rotationRate = new Vector3D(-vstd, Vector3D.PLUS_K);

        // set up the transform from parent GTOD
        final Rotation rot = new Rotation(Vector3D.PLUS_K, -vst);
        final Transform expected = new Transform(date0, rot, rotationRate, null);

        this.checkTransforms(new VEISProvider().getTransform(date0), expected, Precision.EPSILON);
        this.checkTransforms(new VEISProvider().getTransform(date0, false), expected, Precision.EPSILON);
        this.checkTransforms(new VEISProvider().getTransform(date0, FramesFactory.getConfiguration()), expected,
            Precision.EPSILON);
    }

    @Test
    public void testRefGEO() throws PatriusException {

        // this reference test has been extracted from the following paper:
        final AbsoluteDate date0 = new AbsoluteDate(new DateComponents(2004, 06, 01),
            TimeComponents.H00,
            TimeScalesFactory.getUTC());

        final Transform t0 = FramesFactory.getEME2000().getTransformTo(FramesFactory.getVeis1950(), date0);

        // J2000
        final PVCoordinates pvJ2000 =
            new PVCoordinates(new Vector3D(-40588150.3620, -11462167.0280, 27147.6490),
                new Vector3D(834.787457, -2958.305691, -1.173016));

        // VEIS (mslib90)
        final PVCoordinates pvVEIS =
            new PVCoordinates(new Vector3D(-40713785.134055, -11007613.451052, 10293.258344),
                new Vector3D(801.657321, -2967.454926, -0.928881));

        final PVCoordinates delta0 = new PVCoordinates(t0.transformPVCoordinates(pvJ2000), pvVEIS);
        Assert.assertEquals(0.0, delta0.getPosition().getNorm(), 2.5e-3);
        Assert.assertEquals(0.0, delta0.getVelocity().getNorm(), 1.5e-4);

    }

    // private method to check transform esquality
    private void checkTransforms(final Transform exp, final Transform act, final double eps) {
        Assert.assertEquals(0, exp.getRotation().applyInverseTo(act.getRotation()).getAngle(), eps);
        Assert.assertEquals(0, exp.getRotationRate().negate().add(act.getRotationRate()).getNorm(), eps);
        Assert.assertEquals(0, exp.getTranslation().negate().add(act.getTranslation()).getNorm(), eps);
        Assert.assertEquals(0, exp.getVelocity().negate().add(act.getVelocity()).getNorm(), eps);

        Assert.assertEquals(0, exp.getRotation().applyInverseTo(act.getRotation()).getAngle(), eps);
        Assert.assertEquals(0, exp.getRotationRate().negate().add(act.getRotationRate()).getNorm(), eps);
        Assert.assertEquals(0, exp.getTranslation().negate().add(act.getTranslation()).getNorm(), eps);
        Assert.assertEquals(0, exp.getVelocity().negate().add(act.getVelocity()).getNorm(), eps);
    }

    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("compressed-data");

        // Add EOP data
        final FramesConfigurationBuilder builder = 
                new FramesConfigurationBuilder(FramesConfigurationFactory.getIERS2003Configuration(true));
        builder.setEOPHistory(EOPHistoryFactory.getEOP1980History());
        FramesFactory.setConfiguration(builder.getConfiguration());
    }

}
