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

import java.io.ObjectStreamException;
import java.util.Random;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.TestUtils;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.frames.transformations.EME2000Provider;
import fr.cnes.sirius.patrius.frames.transformations.FixedTransformProvider;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class FrameTest {

    @Test
    public void testSameFrameRoot() throws PatriusException {
        final Random random = new Random(0x29448c7d58b95565l);
        final Frame frame = FramesFactory.getEME2000();
        this.checkNoTransform(frame.getTransformTo(frame, new AbsoluteDate()), random);
    }

    @Test
    public void testSameFrameNoRoot() throws PatriusException {
        final Random random = new Random(0xc6e88d0f53e29116l);
        final Transform t = this.randomTransform(random);
        final Frame frame = new Frame(FramesFactory.getEME2000(), t, null, true);
        this.checkNoTransform(frame.getTransformTo(frame, new AbsoluteDate()), random);
    }

    @Test
    public void testSimilarFrames() throws PatriusException {
        final Random random = new Random(0x1b868f67a83666e5l);
        final Transform t = this.randomTransform(random);
        final Frame frame1 = new Frame(FramesFactory.getEME2000(), t, null, true);
        final Frame frame2 = new Frame(FramesFactory.getEME2000(), t, null, false);
        this.checkNoTransform(frame1.getTransformTo(frame2, new AbsoluteDate()), random);
    }

    @Test
    public void testFromParent() throws PatriusException {
        final Random random = new Random(0xb92fba1183fe11b8l);
        final Transform fromEME2000 = this.randomTransform(random);
        final Frame frame = new Frame(FramesFactory.getEME2000(), fromEME2000, null);
        final Transform toEME2000 = frame.getTransformTo(FramesFactory.getEME2000(), new AbsoluteDate());
        this.checkNoTransform(new Transform(fromEME2000.getDate(), fromEME2000, toEME2000), random);
    }

    @Test
    public void testDecomposedTransform() throws PatriusException {
        final Random random = new Random(0xb7d1a155e726da57l);
        final Transform t1 = this.randomTransform(random);
        final Transform t2 = this.randomTransform(random);
        final Transform t3 = this.randomTransform(random);
        final Frame frame1 =
            new Frame(FramesFactory.getEME2000(),
                new Transform(t1.getDate(), new Transform(t1.getDate(), t1, t2), t3),
                null);
        final Frame frame2 =
            new Frame(new Frame(new Frame(FramesFactory.getEME2000(), t1, null), t2, null), t3, null);
        this.checkNoTransform(frame1.getTransformTo(frame2, new AbsoluteDate()), random);
    }

    @Test
    public void testGetDepth() throws PatriusException {
        final Frame frame = FramesFactory.getVeis1950();
        Assert.assertEquals(frame.getDepth(), 5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetAncestor() throws PatriusException {
        final Frame frame = FramesFactory.getEME2000();
        frame.getAncestor(4);
    }

    @Test
    public void testConstructor() throws PatriusException {
        final Random random = new Random(0xb92fba1183fe11b8l);
        final Transform fromEME2000 = this.randomTransform(random);
        final Frame frame1 = new Frame(FramesFactory.getEME2000(), fromEME2000, "frame1");
        final Frame frame2 = new Frame(frame1, new FixedTransformProvider(Transform.IDENTITY), "frame2");
        this.checkNoTransform(frame1.getTransformTo(frame2, new AbsoluteDate()), random);
    }

    @Test
    public void testFindCommon() throws PatriusException {

        final Random random = new Random(0xb7d1a155e726da57l);
        final Transform t1 = this.randomTransform(random);
        final Transform t2 = this.randomTransform(random);
        final Transform t3 = this.randomTransform(random);

        final Frame R1 = new Frame(FramesFactory.getEME2000(), t1, "R1");
        final Frame R2 = new Frame(R1, t2, "R2");
        final Frame R3 = new Frame(R2, t3, "R3");

        final Transform T = R1.getTransformTo(R3, new AbsoluteDate());

        final Transform S = new Transform(t2.getDate(), t2, t3);

        this.checkNoTransform(new Transform(T.getDate(), T, S.getInverse()), random);

    }

    @Test
    public void testIsChildOf() throws PatriusException {
        final Random random = new Random(0xb7d1a155e726da78l);
        final Frame eme2000 = FramesFactory.getEME2000();

        final Frame f1 = new Frame(eme2000, this.randomTransform(random), "f1");
        final Frame f2 = new Frame(f1, this.randomTransform(random), "f2");
        final Frame f4 = new Frame(f2, this.randomTransform(random), "f4");
        final Frame f5 = new Frame(f4, this.randomTransform(random), "f5");
        final Frame f6 = new Frame(eme2000, this.randomTransform(random), "f6");
        final Frame f7 = new Frame(f6, this.randomTransform(random), "f7");
        final Frame f8 = new Frame(f6, this.randomTransform(random), "f8");
        final Frame f9 = new Frame(f7, this.randomTransform(random), "f9");

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
        final Frame rotatingPadFrame =
            new TopocentricFrame(new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING,
                itrf),
                new GeodeticPoint(MathLib.toRadians(5.0),
                    MathLib.toRadians(-100.0),
                    0.0),
                "launch pad");

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
            new Frame(null, this.randomTransform(random), "frame");
            Assert.fail("The frame has no parent frame.");
        } catch (final IllegalArgumentException ex) {
            // expected
        }
    }

    @Test
    public void testFactoryManagedFrame() throws ObjectStreamException {
        final Predefined factoryKey = Predefined.EME2000;
        final FactoryManagedFrame frame = new FactoryManagedFrame(FramesFactory.getGCRF(),
            new EME2000Provider(), true, factoryKey);
        Assert.assertEquals("EME2000", frame.getFactoryKey().getName());
    }

    @Test
    public void testCoverage() throws ObjectStreamException {
        final Predefined factoryKey = Predefined.EME2000;
        final FactoryManagedFrame frame = new FactoryManagedFrame(FramesFactory.getGCRF(),
            new EME2000Provider(), true, factoryKey);
        Assert.assertEquals(true, frame.isPseudoInertial());
        Assert.assertEquals(factoryKey.getName(), frame.toString());
    }

    @Test
    public void testCoverageFixedTransformProvider() throws PatriusException {
        final Transform ident = Transform.IDENTITY;
        final FixedTransformProvider transform = new FixedTransformProvider(ident);
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        Assert.assertEquals(transform.getTransform(date, FramesFactory.getConfiguration()), ident);
        Assert.assertEquals(transform.getTransform(date, false), ident);

    }

    private Transform randomTransform(final Random random) {
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

    private void checkNoTransform(final Transform transform, final Random random) {
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
    public final void testgetTransformJacobian() throws PatriusException {
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
    public final void seriaTest() throws PatriusException {
        Utils.setDataRoot("regular-data");

        final Random random = new Random(0xb7d1a155e726da78l);
        final Frame eme2000 = FramesFactory.getEME2000();

        final Frame f1 = new Frame(eme2000, this.randomTransform(random), "f1");
        final Frame f2 = new Frame(f1, this.randomTransform(random), "f2");
        final Frame f4 = new Frame(f2, this.randomTransform(random), "f4");
        final Frame f5 = new Frame(f4, this.randomTransform(random), "f5");
        final Frame f6 = new Frame(eme2000, this.randomTransform(random), "f6");
        final Frame f7 = new Frame(f6, this.randomTransform(random), "f7");
        final Frame f8 = new Frame(f6, this.randomTransform(random), "f8");
        final Frame f9 = new Frame(f7, this.randomTransform(random), "f9");

        final Frame[] frames = { Frame.getRoot(), FramesFactory.getGCRF(), FramesFactory.getCIRF(),
            FramesFactory.getEME2000(), FramesFactory.getITRF(), FramesFactory.getTOD(false),
            FramesFactory.getTOD(true), FramesFactory.getITRFEquinox(), FramesFactory.getTEME(),
            FramesFactory.getTIRF(), f1, f2, f4, f5, f6, f7, f8, f9 };
        for (final Frame f : frames) {
            final Frame frameRecover = TestUtils.serializeAndRecover(f);
            frameEq(f, frameRecover);
        }

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