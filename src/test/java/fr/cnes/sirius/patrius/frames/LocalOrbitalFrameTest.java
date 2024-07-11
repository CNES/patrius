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
* VERSION:4.8:DM:DM-2975:15/11/2021:[PATRIUS] creation du repere synodique via un LOF 
* VERSION:4.6:FA:FA-2539:27/01/2021:[PATRIUS] Anomalie dans le calcul du vecteur rotation des LOF [iteration 2] 
 * VERSION:4.5:DM:DM-2301:27/05/2020:Ajout de getter a LocalOrbitalFrame 
 * VERSION:4.5:FA:FA-2464:27/05/2020:Anomalie dans le calcul du vecteur rotation des LOF
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:271:05/09/2014:Definitions anomalies LVLH and VVLH
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::DM:524:10/03/2016:serialization test
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.TestUtils;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.KeplerianParameters;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

public class LocalOrbitalFrameTest {

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(LocalOrbitalFrameTest.class.getSimpleName(), "Local orbital frame frame");
    }

    @Test
    public void testOmegaTNW() throws PatriusException {
        // Circular orbit
        final KeplerianParameters params = new KeplerianParameters(7000000, 0, 1, 2, 3, 4, PositionAngle.TRUE, Constants.EIGEN5C_EARTH_MU);
        // Acceleration is computed
        final PVCoordinates pvWithAcc = params.getCartesianParameters().getPVCoordinates();
        final Vector3D omegaRef = LOFType.TNW.computeOmega(pvWithAcc);
        // Case without acceleration
        final PVCoordinates pvNoAcc = new PVCoordinates(pvWithAcc.getPosition(), pvWithAcc.getVelocity());
        final Vector3D omegaAct = LOFType.TNW.computeOmega(pvNoAcc);
        // Check, results should be similar
        Assert.assertEquals(0., Vector3D.distance(omegaRef, omegaAct), 1E-18);
    }

    @Test
    public void testOmegaLVLH() throws PatriusException {
        // Elliptic orbit
        final KeplerianParameters params = new KeplerianParameters(7000000, 0.1, 1, 2, 3, 4, PositionAngle.TRUE, Constants.EIGEN5C_EARTH_MU);
        // Acceleration is computed
        final PVCoordinates pvWithAcc = params.getCartesianParameters().getPVCoordinates();
        final Vector3D omegaRef = LOFType.QSW.computeOmega(pvWithAcc);
        // Case without acceleration
        final Vector3D omegaAct = LOFType.LVLH.computeOmega(pvWithAcc);
        // Check, results should be similar (omega QSW = omega LVLH)
        Assert.assertEquals(0., Vector3D.distance(omegaRef, omegaAct), 0);
    }

    @Test
    public void testTNW() throws PatriusException {
        Report.printMethodHeader("testTNW", "Frame conversion", "Math", 2.0e-15, ComparisonType.ABSOLUTE);
        final AbsoluteDate date = this.initDate.shiftedBy(400);
        final PVCoordinates pv = this.provider.getPVCoordinates(date, this.inertialFrame);
        this.checkFrame(LOFType.TNW, date,
            pv.getVelocity(),
            Vector3D.crossProduct(pv.getMomentum(), pv.getVelocity()),
            pv.getMomentum(),
            pv.getMomentum().negate());
    }

    @Test
    public void testQSW() throws PatriusException {
        Report.printMethodHeader("testQSW", "Frame conversion", "Math", 2.0e-15, ComparisonType.ABSOLUTE);
        final AbsoluteDate date = this.initDate.shiftedBy(400);
        final PVCoordinates pv = this.provider.getPVCoordinates(date, this.inertialFrame);
        this.checkFrame(LOFType.QSW, date,
            pv.getPosition(),
            Vector3D.crossProduct(pv.getMomentum(), pv.getPosition()),
            pv.getMomentum(),
            pv.getMomentum().negate());
    }

    @Test
    public void testQmSmW() throws PatriusException {
        Report.printMethodHeader("testQmSmW", "Frame conversion", "Math", 2.0e-15, ComparisonType.ABSOLUTE);
        final AbsoluteDate date = this.initDate.shiftedBy(400);
        final PVCoordinates pv = this.provider.getPVCoordinates(date, this.inertialFrame);
        this.checkFrame(LOFType.mQmSW, date,
            pv.getPosition().negate(),
            Vector3D.crossProduct(pv.getMomentum(), pv.getPosition().negate()),
            pv.getMomentum(),
            pv.getMomentum().negate());
    }

    @Test
    public void testLVLH() throws PatriusException {
        Report.printMethodHeader("testLVLH", "Frame conversion", "Math", 2.0e-15, ComparisonType.ABSOLUTE);
        final AbsoluteDate date = this.initDate.shiftedBy(400);
        final PVCoordinates pv = this.provider.getPVCoordinates(date, this.inertialFrame);
        this.checkFrame(LOFType.LVLH, date,
            Vector3D.crossProduct(pv.getMomentum(), pv.getPosition()),
            pv.getMomentum().negate(),
            pv.getPosition().negate(),
            pv.getMomentum().negate());
    }


//    @Test
//    public void testLVLHBis() throws PatriusException {
//        Report.printMethodHeader("testLVLHBis", "Frame conversion", "Math", 2.0e-15, ComparisonType.ABSOLUTE);
//        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
//        final Orbit orbit = new KeplerianOrbit(7000000, 0, 0, 0, 0, 0, PositionAngle.TRUE, FramesFactory.getGCRF(), date, Constants.CNES_STELA_MU);
//        final PVCoordinates pva = orbit.getPVCoordinates(date, orbit.getFrame());
//        final PVCoordinates pv = new PVCoordinates(pva.getPosition(), pva.getVelocity(), Vector3D.ZERO);
//        this.checkFrame(LOFType.LVLH, date,
//            Vector3D.crossProduct(pv.getMomentum().negate(), pv.getPosition().negate()),
//            pv.getMomentum().negate(),
//            pv.getPosition().negate(),
//            pv.getMomentum().negate(),
//            orbit);
//    }

    @Test
    public void testVNC() throws PatriusException {
        Report.printMethodHeader("testVNC", "Frame conversion", "Math", 2.0e-15, ComparisonType.ABSOLUTE);
        final AbsoluteDate date = this.initDate.shiftedBy(400);
        final PVCoordinates pv = this.provider.getPVCoordinates(date, this.inertialFrame);
        this.checkFrame(LOFType.VNC, date,
            pv.getVelocity(),
            pv.getMomentum(),
            Vector3D.crossProduct(pv.getVelocity(), pv.getMomentum()),
            pv.getMomentum().negate());
    }

    private void checkFrame(final LOFType type,
            final AbsoluteDate date,
            final Vector3D expectedXDirection,
            final Vector3D expectedYDirection,
            final Vector3D expectedZDirection,
            final Vector3D expectedRotationDirection) throws PatriusException {
        checkFrame(type, date, expectedXDirection, expectedYDirection, expectedZDirection, expectedRotationDirection,
                this.provider);
    }

    private void checkFrame(final LOFType type,
            final AbsoluteDate date,
            final Vector3D expectedXDirection,
            final Vector3D expectedYDirection,
            final Vector3D expectedZDirection,
            final Vector3D expectedRotationDirection,
            final PVCoordinatesProvider provider) throws PatriusException {
        
        final LocalOrbitalFrame lof = new LocalOrbitalFrame(FramesFactory.getGCRF(), type, provider, type.name());

        final Transform t = lof.getTransformTo(FramesFactory.getGCRF(), date);
        final PVCoordinates pv1 = t.transformPVCoordinates(PVCoordinates.ZERO);
        final Vector3D p1 = pv1.getPosition();
        final Vector3D v1 = pv1.getVelocity();
        final PVCoordinates pv2 = provider.getPVCoordinates(date, FramesFactory.getGCRF());
        final Vector3D p2 = pv2.getPosition();
        final Vector3D v2 = pv2.getVelocity();
        Assert.assertEquals(0, p1.subtract(p2).getNorm(), 1.0e-14 * p1.getNorm());
        Assert.assertEquals(0, v1.subtract(v2).getNorm(), 1.0e-14 * v1.getNorm());

        final Vector3D xDirection = t.transformVector(Vector3D.PLUS_I);
        final Vector3D yDirection = t.transformVector(Vector3D.PLUS_J);
        final Vector3D zDirection = t.transformVector(Vector3D.PLUS_K);
        Assert.assertEquals(0, Vector3D.angle(expectedXDirection, xDirection), 2.0e-15);
        Assert.assertEquals(0, Vector3D.angle(expectedYDirection, yDirection), 1.0e-15);
        Assert.assertEquals(0, Vector3D.angle(expectedZDirection, zDirection), 1.0e-15);
        Assert.assertEquals(0, Vector3D.angle(expectedRotationDirection, t.getRotationRate()), 1.0e-15);

        Report.printToReport("Transformed I", expectedXDirection.normalize(), xDirection.normalize());
        Report.printToReport("Transformed J", expectedYDirection.normalize(), yDirection.normalize());
        Report.printToReport("Transformed K", expectedZDirection.normalize(), zDirection.normalize());

        Assert.assertEquals(this.initialOrbit.getKeplerianMeanMotion(), t.getRotationRate().getNorm(), 1.0e-7);

        // Check getters
        Assert.assertEquals(type, lof.getLofType());
    }

    @Test
    public final void seriaTest() throws PatriusException {
        final LocalOrbitalFrame lof = new LocalOrbitalFrame(FramesFactory.getGCRF(), LOFType.LVLH, this.provider,
            LOFType.LVLH.name());
        final Frame[] frames = { lof };
        for (final Frame f : frames) {
            final Frame frameRecover = TestUtils.serializeAndRecover(f);
            FrameTest.frameEq(f, frameRecover);
        }
    }

    @Before
    public void setUp() throws PropagationException {
        this.inertialFrame = FramesFactory.getGCRF();
        this.initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        this.initialOrbit =
            new KeplerianOrbit(7209668.0, 0.5e-4, 1.7, 2.1, 2.9, 6.2, PositionAngle.TRUE,
                this.inertialFrame, this.initDate, 3.986004415e14);
        this.provider = new KeplerianPropagator(this.initialOrbit);

    }

    private Frame inertialFrame;
    private AbsoluteDate initDate;
    private Orbit initialOrbit;
    private PVCoordinatesProvider provider;

}
