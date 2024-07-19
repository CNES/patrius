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
* VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:FA:FA-3199:03/11/2022:[PATRIUS] Mise en œuvre PM 2973, gestion coordonnees et referentiel
 * VERSION:4.9:DM:DM-3093:10/05/2022:[PATRIUS] Mise en Oeuvre PM2973 , gestion coordonnees et referentiel 
 * VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
 * VERSION:4.9:DM:DM-3166:10/05/2022:[PATRIUS] Definir l'ICRF comme repere racine 
 * VERSION:4.9:DM:DM-3130:10/05/2022:[PATRIUS] Robustifier le calcul des phenomenes des CodedEventsLogger, ...
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300) 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:306:12/11/2014:coverage
 * VERSION::DM:319:05/03/2015:Corrected Rotation class (Step1)
 * VERSION::DM:481:05/10/2015: TU for method getTransformJacobian in class Frame
 * VERSION::DM:489:12/01/2016:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:513:09/03/2016:Make Frame class multithread safe
 * VERSION::DM:524:10/03/2016:serialization test
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames;

import java.util.Random;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.EllipsoidPoint;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.frames.transformations.EME2000Provider;
import fr.cnes.sirius.patrius.frames.transformations.FixedTransformProvider;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class FrameTest {

    /**
     * Test referential in a simple case:
     * - Frame1 = Transform t from frame
     * - Frame2 = Transform t from frame
     * - Referential of frame1 is frame1
     * - Referential of frame2 is a translated and rotating frame
     */
    @Test
    public void testReferentialSimple1() throws PatriusException {
        final Frame frame = OrphanFrame.getNewOrphanFrame("root");

        // Transform with velocity
        final PVCoordinates pv = new PVCoordinates(Vector3D.PLUS_I, Vector3D.PLUS_J, Vector3D.PLUS_K);
        final Rotation r = new Rotation(Vector3D.PLUS_K, MathLib.PI / 2.);
        final Vector3D rotationRate = Vector3D.PLUS_K;
        final AngularCoordinates angular = new AngularCoordinates(r, rotationRate);
        final Transform t = new Transform(AbsoluteDate.J2000_EPOCH, pv, angular);

        // Frame1 = frame and frame2 = frame but with moving referential
        final Frame referential = new Frame(frame, t, "referentials");
        final Frame frame1 = new Frame(frame, Transform.IDENTITY, "frame1");
        final Frame frame2 = new Frame(frame, Transform.IDENTITY, "frame2");
        frame2.setReferential(referential);

        // Compute transform
        final Transform tres1 = frame.getTransformTo(frame1, AbsoluteDate.J2000_EPOCH);
        final Transform tres2 = frame.getTransformTo(frame2, AbsoluteDate.J2000_EPOCH);

        // Check velocity depends properly on referential
        Assert.assertTrue(tres1.getCartesian().getVelocity().isZero());
        Assert.assertTrue(tres2.getCartesian().getVelocity().equals(Vector3D.PLUS_J));
    }

    /**
     * Test referential in a simple case:
     * - Frame1 = Transform t from frame
     * - Frame2 = Transform t from frame
     * - Referential of frame is a translated and rotating frame
     */
    @Test
    public void testReferentialSimple2() throws PatriusException {
        final Frame frame = OrphanFrame.getNewOrphanFrame("root");

        // Transform with velocity
        final PVCoordinates pv = new PVCoordinates(Vector3D.PLUS_I, Vector3D.PLUS_J, Vector3D.PLUS_K);
        final Rotation r = new Rotation(Vector3D.PLUS_K, MathLib.PI / 2.);
        final Vector3D rotationRate = Vector3D.PLUS_K;
        final AngularCoordinates angular = new AngularCoordinates(r, rotationRate);
        final Transform t = new Transform(AbsoluteDate.J2000_EPOCH, pv, angular);

        // Frame1 = frame and frame2 = frame but with moving referential
        final Frame referential = new Frame(frame, t, "referentials");
        final Frame frame1 = new Frame(frame, Transform.IDENTITY, "frame1");
        final Frame frame2 = new Frame(frame, Transform.IDENTITY, "frame2");

        // Compute transform
        final Transform tres1 = frame.getTransformTo(frame1, AbsoluteDate.J2000_EPOCH);
        frame.setReferential(referential);
        final Transform tres2 = frame.getTransformTo(frame2, AbsoluteDate.J2000_EPOCH);

        // Check velocity depends properly on referential
        Assert.assertTrue(tres1.getCartesian().getVelocity().isZero());
        Assert.assertTrue(tres2.getCartesian().getVelocity().equals(Vector3D.PLUS_J));
    }

    /**
     * Test referential using AngularCoordinates:
     * - Position in ITRF is the same independently of referential
     * - Velocity is the same when referential is the same
     */
    @Test
    public void testFrameReferentialAngularCoordinates() throws PatriusException {

        // Initialization
        Utils.setDataRoot("regular-dataPBASE");
        final PVCoordinates pv = new PVCoordinates(7000000., 0., 0., 0., 7000., 0.);

        // Frame definitions
        // GCRF, GCRF referential
        final Frame gcrf = FramesFactory.getGCRF();
        // ITRF, ITRF referential
        final Frame itrf = FramesFactory.getITRF();
        // ITRF, GCRF referential
        final Frame itrf_gcrf = new Frame(itrf, Transform.IDENTITY, "itrf_gcrf");
        itrf_gcrf.setReferential(gcrf);

        final CartesianOrbit orbit = new CartesianOrbit(pv, gcrf, AbsoluteDate.J2000_EPOCH.shiftedBy(4 * 365 * 86400.),
            Constants.GRS80_EARTH_MU);

        // Checks
        // Position in ITRF is the same independently of referential
        Assert.assertEquals(0.,
            orbit.getPVCoordinates(itrf).getPosition().subtract(orbit.getPVCoordinates(itrf_gcrf).getPosition())
                .getNorm(), 1E-12);
        // Velocity is the same when referential is the same
        Assert.assertEquals(pv.getVelocity().getNorm(), orbit.getPVCoordinates(itrf_gcrf).getVelocity().getNorm(),
            1E-12);
    }

    @Test
    public void testGetPVCoordinates() throws PatriusException {
        // root frame
        final Frame root = FramesFactory.getGCRF();

        // build an arbitrary frame instance
        final Frame frame = new Frame(root, Transform.IDENTITY, "test-frame");

        PVCoordinates actual;

        // build a new frame from translation
        final Vector3D translation1 = new Vector3D(100, 100, 100);
        final Frame frame1 = new Frame(frame, new Transform(AbsoluteDate.J2000_EPOCH, translation1), "child1");
        actual = frame.getPVCoordinates(AbsoluteDate.J2000_EPOCH, frame1);

        // check frame origin position and velocity computation
        Assert.assertEquals(translation1.negate(), actual.getPosition());
        Assert.assertEquals(Vector3D.ZERO, actual.getVelocity());

        // build a new frame from cartesian coordinates transformation
        final Vector3D translation2 = new Vector3D(1000, 220, 640);
        final Vector3D velocity = new Vector3D(7421, 1225, 564);
        final Frame frame2 = new Frame(frame, new Transform(AbsoluteDate.J2000_EPOCH, new PVCoordinates(translation2,
            velocity)), "child2");
        actual = frame.getPVCoordinates(AbsoluteDate.J2000_EPOCH, frame2);

        // check frame origin position and velocity computation
        Assert.assertEquals(translation2.negate(), actual.getPosition());
        Assert.assertEquals(velocity.negate(), actual.getVelocity());

        // build a new frame from translation and rotation
        final AngularCoordinates angular = new AngularCoordinates(new Rotation(Vector3D.PLUS_K, 0), Vector3D.PLUS_K);
        final Frame frame3 = new Frame(frame, new Transform(AbsoluteDate.J2000_EPOCH, new PVCoordinates(translation1,
            Vector3D.ZERO), angular), "child3");
        actual = frame.getPVCoordinates(AbsoluteDate.J2000_EPOCH, frame3);

        // check frame origin position and velocity computation
        Assert.assertEquals(translation1.negate(), actual.getPosition());
        Assert.assertEquals(new Vector3D(-100, 100, 0), actual.getVelocity());

        // the native frame computation is not time dependant
        Assert.assertEquals(frame, frame.getNativeFrame(AbsoluteDate.J2000_EPOCH, null));
    }

    @Test
    public void testSameFrameRoot() throws PatriusException {
        final Random random = new Random(0x29448c7d58b95565l);
        final Frame frame = FramesFactory.getEME2000();
        checkNoTransform(frame.getTransformTo(frame, new AbsoluteDate()), random);
    }

    @Test
    public void testSameFrameNoRoot() throws PatriusException {
        final Random random = new Random(0xc6e88d0f53e29116l);
        final Transform t = randomTransform(random);
        final Frame frame = new Frame(FramesFactory.getEME2000(), t, null, true);
        checkNoTransform(frame.getTransformTo(frame, new AbsoluteDate()), random);
    }

    @Test
    public void testSimilarFrames() throws PatriusException {
        final Random random = new Random(0x1b868f67a83666e5l);
        final Transform t = randomTransform(random);
        final Frame frame1 = new Frame(FramesFactory.getEME2000(), t, null, true);
        final Frame frame2 = new Frame(FramesFactory.getEME2000(), t, null, false);
        checkNoTransform(frame1.getTransformTo(frame2, new AbsoluteDate()), random);
    }

    @Test
    public void testFromParent() throws PatriusException {
        final Random random = new Random(0xb92fba1183fe11b8l);
        final Transform fromEME2000 = randomTransform(random);
        final Frame frame = new Frame(FramesFactory.getEME2000(), fromEME2000, null);
        final Transform toEME2000 = frame.getTransformTo(FramesFactory.getEME2000(), new AbsoluteDate());
        checkNoTransform(new Transform(fromEME2000.getDate(), fromEME2000, toEME2000), random);
    }

    @Test
    public void testDecomposedTransform() throws PatriusException {
        final Random random = new Random(0xb7d1a155e726da57l);
        final Transform t1 = randomTransform(random);
        final Transform t2 = randomTransform(random);
        final Transform t3 = randomTransform(random);
        final Frame frame1 =
            new Frame(FramesFactory.getEME2000(),
                new Transform(t1.getDate(), new Transform(t1.getDate(), t1, t2), t3),
                null);
        final Frame frame2 =
            new Frame(new Frame(new Frame(FramesFactory.getEME2000(), t1, null), t2, null), t3, null);
        checkNoTransform(frame1.getTransformTo(frame2, new AbsoluteDate()), random);
    }

    @Test
    public void testGetDepth() throws PatriusException {
        final Frame frame = FramesFactory.getVeis1950();
        Assert.assertEquals(frame.getDepth(), 7);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetAncestor() {
        final Frame frame = FramesFactory.getEME2000();
        frame.getAncestor(4);
    }

    @Test
    public void testConstructor() throws PatriusException {
        final Random random = new Random(0xb92fba1183fe11b8l);
        final Transform fromEME2000 = randomTransform(random);
        final Frame frame1 = new Frame(FramesFactory.getEME2000(), fromEME2000, "frame1");
        final Frame frame2 = new Frame(frame1, new FixedTransformProvider(Transform.IDENTITY), "frame2");
        checkNoTransform(frame1.getTransformTo(frame2, new AbsoluteDate()), random);
    }

    @Test
    public void testFindCommon() throws PatriusException {

        final Random random = new Random(0xb7d1a155e726da57l);
        final Transform t1 = randomTransform(random);
        final Transform t2 = randomTransform(random);
        final Transform t3 = randomTransform(random);

        final Frame R1 = new Frame(FramesFactory.getEME2000(), t1, "R1");
        final Frame R2 = new Frame(R1, t2, "R2");
        final Frame R3 = new Frame(R2, t3, "R3");

        final Transform T = R1.getTransformTo(R3, new AbsoluteDate());

        final Transform S = new Transform(t2.getDate(), t2, t3);

        checkNoTransform(new Transform(T.getDate(), T, S.getInverse()), random);
    }

    @Test
    public void testIsChildOf() {
        final Random random = new Random(0xb7d1a155e726da78l);
        final Frame eme2000 = FramesFactory.getEME2000();

        final Frame f1 = new Frame(eme2000, randomTransform(random), "f1");
        final Frame f2 = new Frame(f1, randomTransform(random), "f2");
        final Frame f4 = new Frame(f2, randomTransform(random), "f4");
        final Frame f5 = new Frame(f4, randomTransform(random), "f5");
        final Frame f6 = new Frame(eme2000, randomTransform(random), "f6");
        final Frame f7 = new Frame(f6, randomTransform(random), "f7");
        final Frame f8 = new Frame(f6, randomTransform(random), "f8");
        final Frame f9 = new Frame(f7, randomTransform(random), "f9");

        // check if the root frame can be an ancestor of another frame
        Assert.assertEquals(false, eme2000.isChildOf(f5));

        // check if a frame which belongs to the same branch than the 2nd frame is a branch of it
        Assert.assertEquals(true, f5.isChildOf(f1));

        // check if a random frame is the child of the root frame
        Assert.assertEquals(true, f9.isChildOf(eme2000));

        // check that a frame is not its own child
        Assert.assertEquals(false, f4.isChildOf(f4));

        // check if a frame which belong to a different branch than the 2nd frame can be a child for it
        Assert.assertEquals(false, f9.isChildOf(f5));

        // check if the root frame is not a child of itself
        Assert.assertEquals(false, eme2000.isChildOf(eme2000));

        Assert.assertEquals(false, f9.isChildOf(f8));

        // added tests

        // check if the frame f8 is a child of f6
        Assert.assertEquals(true, f8.isChildOf(f6));

        // check if the frame f9 is not a child of f2
        Assert.assertEquals(false, f9.isChildOf(f2));
    }

    @Test
    public void testH0m9() throws PatriusException {
        final AbsoluteDate h0 = new AbsoluteDate("2010-07-01T10:42:09", TimeScalesFactory.getUTC());
        final Frame itrf = FramesFactory.getITRF();
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
            Constants.WGS84_EARTH_FLATTENING, itrf);
        final Frame rotatingPadFrame = new TopocentricFrame(new EllipsoidPoint(earth, earth.getLLHCoordinatesSystem(),
            MathLib.toRadians(5.0), MathLib.toRadians(-100.0), 0., ""), "launch pad");

        // create a new inertially oriented frame that is aligned with ITRF2005 at h0 - 9 seconds
        final AbsoluteDate h0M9 = h0.shiftedBy(-9.0);
        final Frame eme2000 = FramesFactory.getEME2000();
        final Frame frozenLaunchFrame = rotatingPadFrame.getFrozenFrame(eme2000, h0M9, "launch frame");

        // check velocity module is unchanged
        final Vector3D pEme2000 = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        final Vector3D vEme2000 = new Vector3D(-2194.0, -2141.0, -8.0);
        final PVCoordinates pvEme2000 = new PVCoordinates(pEme2000, vEme2000);
        final PVCoordinates pvH0m9 = eme2000.getTransformTo(frozenLaunchFrame, h0M9).transformPVCoordinates(pvEme2000);
        Assert.assertEquals(vEme2000.getNorm(), pvH0m9.getVelocity().getNorm(), 1.0e-6);

        // this frame is fixed with respect to EME2000 but rotates with respect to the non-frozen one
        // the following loop should have a fixed angle a1 and an evolving angle a2
        double minA1 = Double.POSITIVE_INFINITY;
        double maxA1 = Double.NEGATIVE_INFINITY;
        double minA2 = Double.POSITIVE_INFINITY;
        double maxA2 = Double.NEGATIVE_INFINITY;
        double dt;
        for (dt = 0; dt < 86164; dt += 300.0) {
            final AbsoluteDate date = h0M9.shiftedBy(dt);
            final double a1 = frozenLaunchFrame.getTransformTo(eme2000, date).getRotation().getAngle();
            final double a2 = frozenLaunchFrame.getTransformTo(rotatingPadFrame, date).getRotation().getAngle();
            minA1 = MathLib.min(minA1, a1);
            maxA1 = MathLib.max(maxA1, a1);
            minA2 = MathLib.min(minA2, a2);
            maxA2 = MathLib.max(maxA2, a2);
        }

        Assert.assertEquals(0, maxA1 - minA1, 1.0e-12);
        Assert.assertEquals(FastMath.PI, maxA2 - minA2, 0.01);
    }

    @Test
    public void testNoSpecifiedParent() {
        final Random random = new Random(0x2f6769c23e53e96el);
        try {
            new Frame(null, randomTransform(random), "frame");
            Assert.fail("The frame has no parent frame.");
        } catch (final IllegalArgumentException ex) {
            // expected
        }
    }

    @Test
    public void testFactoryManagedFrame() {
        final Predefined factoryKey = Predefined.EME2000;
        final FactoryManagedFrame frame = new FactoryManagedFrame(FramesFactory.getGCRF(),
            new EME2000Provider(), true, factoryKey);
        Assert.assertEquals("EME2000", frame.getFactoryKey().getName());
    }

    @Test
    public void testCoverage() {
        final Predefined factoryKey = Predefined.EME2000;
        final FactoryManagedFrame frame = new FactoryManagedFrame(FramesFactory.getGCRF(),
            new EME2000Provider(), true, factoryKey);
        Assert.assertEquals(true, frame.isPseudoInertial());
        Assert.assertEquals(factoryKey.getName(), frame.toString());
        frame.setName("My frame");
        Assert.assertEquals("My frame", frame.toString());
    }

    @Test
    public void testCoverageFixedTransformProvider() throws PatriusException {
        final Transform ident = Transform.IDENTITY;
        final FixedTransformProvider transform = new FixedTransformProvider(ident);
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        Assert.assertEquals(transform.getTransform(date, FramesFactory.getConfiguration()), ident);
        Assert.assertEquals(transform.getTransform(date, false), ident);

    }

    private static Transform randomTransform(final Random random) {
        Transform transform = Transform.IDENTITY;
        for (int i = random.nextInt(10); i > 0; --i) {
            if (random.nextBoolean()) {
                final Vector3D u = new Vector3D(random.nextDouble() * 1000.0,
                    random.nextDouble() * 1000.0,
                    random.nextDouble() * 1000.0);
                transform = new Transform(transform.getDate(), transform, new Transform(transform.getDate(), u));
            } else {
                final double q0 = random.nextDouble() * 2 - 1;
                final double q1 = random.nextDouble() * 2 - 1;
                final double q2 = random.nextDouble() * 2 - 1;
                final double q3 = random.nextDouble() * 2 - 1;
                final double q = MathLib.sqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3);
                final Rotation r = new Rotation(false, q0 / q, q1 / q, q2 / q, q3 / q);
                transform = new Transform(transform.getDate(), transform, new Transform(transform.getDate(), r));
            }
        }
        return transform;
    }

    private static void checkNoTransform(final Transform transform, final Random random) {
        for (int i = 0; i < 100; ++i) {
            final Vector3D a = new Vector3D(random.nextDouble(),
                random.nextDouble(),
                random.nextDouble());
            final Vector3D b = transform.transformVector(a);
            Assert.assertEquals(0, a.subtract(b).getNorm(), 1.0e-10);
            final Vector3D c = transform.transformPosition(a);
            Assert.assertEquals(0, a.subtract(c).getNorm(), 1.0e-10);
        }
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedMethod {@link Frame#getTransformJacobian(Frame, AbsoluteDate)}
     * 
     * @description Validation of the jacobian related to the conversion towards wished Frame at given date
     * 
     * @input Frame from = FramesFactory.getEME2000() ,
     * @input AbsoluteDate h0 = "2011-07-01T10:42:09" ,
     * @input Rotation rot = Rotation(Vector3D.PLUS_K, FastMath.PI / 6) ,
     * @input Transform t = Transform(h0,rot) ,
     * @input Frame to = Frame(from,t,"to")
     * 
     * @output The jacobian of the conversion
     * 
     * @testPassCriteria the jacobian computed is the same as the one computed by the class Transform
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test
    public void testgetTransformJacobian() throws PatriusException {
        final Frame from = FramesFactory.getEME2000();
        final Frame to = FramesFactory.getMOD(false);

        final double[][] actual = from.getTransformJacobian(to, AbsoluteDate.GPS_EPOCH).getData(false);
        final double[][] refJacobian = new double[6][6];
        from.getTransformTo(to, AbsoluteDate.GPS_EPOCH).getJacobian(refJacobian);

        for (int i = 0; i < refJacobian.length; ++i) {
            final double[] rowConv = actual[i];
            final double[] rowJac = refJacobian[i];

            for (int j = 0; j < rowJac.length; ++j) {
                Assert.assertEquals(rowConv[j], rowJac[j], 1.0E-14);
            }
        }
    }

    @Test
    public void seriaTest() throws PatriusException {
        Utils.setDataRoot("regular-data");

        final Random random = new Random(0xb7d1a155e726da78l);
        final Frame eme2000 = FramesFactory.getEME2000();

        final Frame f1 = new Frame(eme2000, randomTransform(random), "f1");
        final Frame f2 = new Frame(f1, randomTransform(random), "f2");
        final Frame f4 = new Frame(f2, randomTransform(random), "f4");
        final Frame f5 = new Frame(f4, randomTransform(random), "f5");
        final Frame f6 = new Frame(eme2000, randomTransform(random), "f6");
        final Frame f7 = new Frame(f6, randomTransform(random), "f7");
        final Frame f8 = new Frame(f6, randomTransform(random), "f8");
        final Frame f9 = new Frame(f7, randomTransform(random), "f9");

        final Frame[] frames = { Frame.getRoot(), FramesFactory.getGCRF(), FramesFactory.getCIRF(),
            FramesFactory.getEME2000(), FramesFactory.getITRF(), FramesFactory.getTOD(false),
            FramesFactory.getTOD(true), FramesFactory.getITRFEquinox(), FramesFactory.getTEME(),
            FramesFactory.getTIRF(), f1, f2, f4, f5, f6, f7, f8, f9 };
        for (final Frame f : frames) {
            final Frame frameRecover = TestUtils.serializeAndRecover(f);
            frameEq(f, frameRecover);
        }
    }

    /**
     * Test the method {@link Frame#getFirstPseudoInertialAncestor()} through different simple cases.
     */
    @Test
    public void testGetFirstPseudoInertialAncestor() throws PatriusException {
        Utils.setDataRoot("regular-data");
        Assert.assertEquals(FramesFactory.getGCRF(), FramesFactory.getGCRF().getFirstPseudoInertialAncestor());
        Assert.assertEquals(FramesFactory.getCIRF(), FramesFactory.getITRF().getFirstPseudoInertialAncestor());
        Assert.assertEquals(FramesFactory.getICRF(), FramesFactory.getICRF().getFirstPseudoInertialAncestor());
        Assert.assertEquals(null, OrphanFrame.getNewOrphanFrame("Orphan frame").getFirstPseudoInertialAncestor());
    }

    /**
     * Test the method {@link Frame#getFirstCommonPseudoInertialAncestor()} through different simple cases.
     */
    @Test
    public void testGetFirstCommonPseudoInertialAncestor() throws PatriusException {
        Utils.setDataRoot("regular-data");
        Assert.assertEquals(FramesFactory.getGCRF(),
            FramesFactory.getGCRF().getFirstCommonPseudoInertialAncestor(FramesFactory.getGCRF()));
        Assert.assertEquals(FramesFactory.getCIRF(),
            FramesFactory.getITRF().getFirstCommonPseudoInertialAncestor(FramesFactory.getTIRF()));
        Assert.assertEquals(FramesFactory.getGCRF(),
            FramesFactory.getITRF().getFirstCommonPseudoInertialAncestor(FramesFactory.getVeis1950()));
        Assert
            .assertEquals(
                null,
                OrphanFrame.getNewOrphanFrame("Orphan frame").getFirstCommonPseudoInertialAncestor(
                    FramesFactory.getGCRF()));
    }

    public static void frameEq(final Frame f1, final Frame f2) {
        Assert.assertEquals(f1.getName(), f2.getName());
    }

    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("compressed-data");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));
    }

    @After
    public void tearDown() throws PatriusException {
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
    }
}
