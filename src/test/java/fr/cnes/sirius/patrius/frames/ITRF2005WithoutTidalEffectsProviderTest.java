/**
 * Copyright 2002-2012 CS Systèmes d'Information
 * Copyright 2011-2017 CNES
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
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:306:12/11/2014:coverage
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:489:12/01/2016:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.transformations.ITRFProvider;
import fr.cnes.sirius.patrius.frames.transformations.TIRFProvider;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class ITRF2005WithoutTidalEffectsProviderTest {

    @Test
    public void testRoughRotation() throws PatriusException {

        final AbsoluteDate date1 = new AbsoluteDate(new DateComponents(2006, 02, 24),
            new TimeComponents(15, 38, 00),
            TimeScalesFactory.getUTC());
        final Frame ITRF2005 = FramesFactory.getITRF();
        final Transform t0 = ITRF2005.getTransformTo(FramesFactory.getEME2000(), date1);
        // Cover the getTransform methods of the ITRFProvider class:
        Assert.assertNotNull(new ITRFProvider().getTransform(date1));
        Assert.assertNotNull(new ITRFProvider().getTransform(date1, false));
        final double dt = 10.0;
        final AbsoluteDate date2 = date1.shiftedBy(dt);
        final Transform t1 = ITRF2005.getTransformTo(FramesFactory.getEME2000(), date2);
        final Transform evolution = new Transform(date2, t0.getInverse(), t1);

        final Vector3D p = new Vector3D(6000, 6000, 0);
        Assert.assertEquals(0.0, evolution.transformPosition(Vector3D.ZERO).getNorm(), 1.0e-15);
        Assert.assertEquals(0.0, evolution.transformVector(p).getZ(), 0.003);
        Assert.assertEquals(2 * FastMath.PI * dt / 86164,
            Vector3D.angle(t0.transformVector(p), t1.transformVector(p)),
            1.0e-9);

    }

    @Test
    public void testRoughOrientation() throws PatriusException {

        AbsoluteDate date = new AbsoluteDate(2001, 03, 21, 0, 4, 0, TimeScalesFactory.getUTC());
        final Frame ITRF2005 = FramesFactory.getITRF();

        Vector3D u = ITRF2005.getTransformTo(FramesFactory.getEME2000(), date).transformVector(Vector3D.PLUS_I);
        Assert.assertTrue(Vector3D.angle(u, Vector3D.MINUS_I) < MathLib.toRadians(0.5));

        date = date.shiftedBy(6 * 3600);
        u = ITRF2005.getTransformTo(FramesFactory.getEME2000(), date).transformVector(Vector3D.PLUS_I);
        Assert.assertTrue(Vector3D.angle(u, Vector3D.MINUS_J) < MathLib.toRadians(0.5));

        date = date.shiftedBy(6 * 3600);
        u = ITRF2005.getTransformTo(FramesFactory.getEME2000(), date).transformVector(Vector3D.PLUS_I);
        Assert.assertTrue(Vector3D.angle(u, Vector3D.PLUS_I) < MathLib.toRadians(0.5));

        date = date.shiftedBy(6 * 3600);
        u = ITRF2005.getTransformTo(FramesFactory.getEME2000(), date).transformVector(Vector3D.PLUS_I);
        Assert.assertTrue(Vector3D.angle(u, Vector3D.PLUS_J) < MathLib.toRadians(0.5));

    }

    @Test
    public void testRoughERA() throws PatriusException {

        AbsoluteDate date = new AbsoluteDate(2001, 03, 21, 0, 4, 0, TimeScalesFactory.getUTC());
        final TIRFProvider TIRF2000 = (TIRFProvider) FramesFactory.getTIRF().getTransformProvider();

        Assert.assertEquals(180, MathLib.toDegrees(TIRFProvider.getEarthRotationAngle(date)), 0.5);

        date = date.shiftedBy(6 * 3600);
        Assert.assertEquals(-90, MathLib.toDegrees(TIRFProvider.getEarthRotationAngle(date)), 0.5);

        date = date.shiftedBy(6 * 3600);
        Assert.assertEquals(0, MathLib.toDegrees(TIRFProvider.getEarthRotationAngle(date)), 0.5);

        date = date.shiftedBy(6 * 3600);
        Assert.assertEquals(90, MathLib.toDegrees(TIRFProvider.getEarthRotationAngle(date)), 0.5);
        // Cover the getTransform and getEarthRotationRate methods of the TIRFProvider class:
        Assert.assertNotNull(new TIRFProvider().getTransform(date));
        Assert.assertNotNull(new TIRFProvider().getTransform(date, false));
        new TIRFProvider();
        Assert.assertNotNull(TIRFProvider.getEarthRotationRate());
    }

    @Test
    public void testMSLIBTransformJ2000_TerVrai() throws PatriusException {

        final AbsoluteDate date = new AbsoluteDate(new DateComponents(2003, 10, 14),
            new TimeComponents(02, 00, 00),
            TimeScalesFactory.getUTC());
        final Transform trans = FramesFactory.getEME2000().getTransformTo(FramesFactory.getTIRF(), date);

        // Positions
        final Vector3D posTIRF =
            trans.transformPosition(new Vector3D(6500000.0, -1234567.0, 4000000.0));
        Assert.assertEquals(0, 3011109.361 - posTIRF.getX(), 0.38);
        Assert.assertEquals(0, -5889822.669 - posTIRF.getY(), 0.38);
        Assert.assertEquals(0, 4002170.039 - posTIRF.getZ(), 0.27);

    }

    @Test
    public void testMSLIBTransformJ2000_TerRef() throws PatriusException {

        final AbsoluteDate t0 = new AbsoluteDate(new DateComponents(2003, 10, 14),
            new TimeComponents(02, 00, 00),
            TimeScalesFactory.getUTC());
        final Frame itrf = FramesFactory.getITRF();
        final Transform trans = FramesFactory.getEME2000().getTransformTo(itrf, t0);

        // Coordinates in EME2000
        final PVCoordinates pvEME2000 =
            new PVCoordinates(new Vector3D(6500000.0, -1234567.0, 4000000.0),
                new Vector3D(3609.28229, 3322.88979, -7083.950661));

        // Reference coordinates in ITRF
        final PVCoordinates pvRef =
            new PVCoordinates(new Vector3D(3011113.971820046, -5889827.854375269, 4002158.938875904),
                new Vector3D(4410.393506651586, -1033.61784235127, -7082.633883124906));

        // tests using direct transform
        this.checkPV(pvRef, trans.transformPVCoordinates(pvEME2000), 0.61, 2.0e-4);

        // compute local evolution using finite differences
        final double h = 0.1;
        final Rotation r0 = trans.getRotation();
        AbsoluteDate date = t0.shiftedBy(-2 * h);
        final Rotation evoM2h = FramesFactory.getEME2000().getTransformTo(itrf, date).getRotation().applyInverseTo(r0);
        final double alphaM2h = -evoM2h.getAngle();
        final Vector3D axisM2h = evoM2h.getAxis();
        date = t0.shiftedBy(-h);
        final Rotation evoM1h = FramesFactory.getEME2000().getTransformTo(itrf, date).getRotation().applyInverseTo(r0);
        final double alphaM1h = -evoM1h.getAngle();
        final Vector3D axisM1h = evoM1h.getAxis();
        date = t0.shiftedBy(h);
        final Rotation evoP1h = FramesFactory.getEME2000().getTransformTo(itrf, date).getRotation().applyInverseTo(r0);
        final double alphaP1h = evoP1h.getAngle();
        final Vector3D axisP1h = evoP1h.getAxis().negate();
        date = t0.shiftedBy(2 * h);
        final Rotation evoP2h = FramesFactory.getEME2000().getTransformTo(itrf, date).getRotation().applyInverseTo(r0);
        final double alphaP2h = evoP2h.getAngle();
        final Vector3D axisP2h = evoP2h.getAxis().negate();
        final double w = (8 * (alphaP1h - alphaM1h) - (alphaP2h - alphaM2h)) / (12 * h);
        final Vector3D axis = axisM2h.add(axisM1h).add(axisP1h.add(axisP2h)).normalize();
        final Transform finiteDiffTransform = new Transform(t0, trans.getRotation(), new Vector3D(w, axis));

        this.checkPV(pvRef, finiteDiffTransform.transformPVCoordinates(pvEME2000), 0.61, 2.0e-4);

    }

    @Test
    public void testMontenbruck() throws PatriusException {
        final AbsoluteDate t0 = new AbsoluteDate(new DateComponents(1999, 3, 4), TimeComponents.H00,
            TimeScalesFactory.getGPS());
        final Transform trans = FramesFactory.getITRF().getTransformTo(FramesFactory.getGCRF(), t0);
        final PVCoordinates pvWGS =
            new PVCoordinates(new Vector3D(19440953.805, 16881609.273, -6777115.092),
                new Vector3D(-811.1827456, -257.3799137, -3068.9508125));
        System.out.println(trans.transformPVCoordinates(pvWGS));
        this.checkPV(new PVCoordinates(new Vector3D(-23830592.685, -9747073.881, -6779831.010),
            new Vector3D(1561.9646362, -1754.3454485, -3068.8504996)),
            trans.transformPVCoordinates(pvWGS), 0.12, 2.6e-5);

    }

    private void checkPV(final PVCoordinates reference, final PVCoordinates result,
                         final double positionThreshold, final double velocityThreshold) {

        final Vector3D dP = result.getPosition().subtract(reference.getPosition());
        final Vector3D dV = result.getVelocity().subtract(reference.getVelocity());
        Assert.assertEquals(0, dP.getNorm(), positionThreshold);
        Assert.assertEquals(0, dV.getNorm(), velocityThreshold);
    }

    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("compressed-data");

        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
    }

}
