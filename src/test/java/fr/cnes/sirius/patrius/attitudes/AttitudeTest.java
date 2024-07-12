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
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
* VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:95:12/07/2013:Fixed Attitude bug - spin parameter taken as is (same for both conventions)
 * VERSION::FA:262:29/04/2014:Removed standard gravitational parameter from constructor
 * VERSION::DM:319:05/03/2015:Corrected Rotation class (Step1)
 * VERSION::DM:368:20/03/2015:Eckstein-Heschler : Back at the "mu"
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:480:15/02/2016: new analytical propagators and mean/osculating conversion
 * VERSION::DM:489:25/09/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::DM:455:05/11/2015:add Slerp method
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:524:10/03/2016:serialization test
 * VERSION::DM:603:29/08/2016:deleted deprecated methods and classes in package attitudes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.TimeStampedAngularCoordinates;
import fr.cnes.sirius.patrius.utils.TimeStampedAngularCoordinatesTest;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Attitude test class.
 * 
 * @author Rami Houdroge
 */
public class AttitudeTest {

    /** Used epsilon for double comparison */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    @Test
    public void testSpinDerivativeConstructor1() throws PatriusException {
        // Test the new constructor with rotation acceleration input and the
        // associated getter:
        final Vector3D derivative1 = new Vector3D(0.0, 4.5, 0.5);

        final Attitude attitude = new Attitude(AbsoluteDate.J2000_EPOCH, FramesFactory.getEME2000(), Rotation.IDENTITY,
            new Vector3D(0, 0, 0), derivative1);

        Assert.assertEquals(derivative1, attitude.getRotationAcceleration());

        // test the getRotationAcceleration() fails when no Rotation
        // acceleration is added to the Attitude:
        final Attitude attitude2 = new Attitude(AbsoluteDate.J2000_EPOCH, FramesFactory.getEME2000(),
            Rotation.IDENTITY, Vector3D.ZERO);

        // no rotation accelerationin constructor : set to 0 by default
        Assert.assertEquals(attitude2.getRotationAcceleration(), Vector3D.ZERO);
    }

    @Test
    public void testSpinDerivativeNewConstructor() throws PatriusException {
        // Test the new constructor with rotation acceleration input and the
        // associated getter:
        final Vector3D derivative1 = new Vector3D(0.0, 4.5, 0.5);

        final Attitude attitude = new Attitude(AbsoluteDate.J2000_EPOCH, FramesFactory.getEME2000(), Rotation.IDENTITY,
            new Vector3D(0, 0, 0), derivative1);
        Assert.assertEquals(derivative1, attitude.getRotationAcceleration());

        // test the getRotationAcceleration() fails when no Rotation
        // acceleration is added to the Attitude:
        final Attitude attitude2 = new Attitude(AbsoluteDate.J2000_EPOCH, FramesFactory.getEME2000(),
            new AngularCoordinates());

        Assert.assertEquals(new Vector3D(0, 0, 0), attitude2.getRotationAcceleration());

    }

    @Test
    public void testSpinDerivative() throws PatriusException {
        // Test the new constructor with the spin derivative input and the
        // associated getter:
        final Vector3D derivative1 = new Vector3D(0.0, 4.5, 0.5);

        // const with TimeStampedAngularCoordinates
        final TimeStampedAngularCoordinates ar = new TimeStampedAngularCoordinates(AbsoluteDate.J2000_EPOCH,
            Rotation.IDENTITY, Vector3D.ZERO, derivative1);
        final TimeStampedAngularCoordinates arNoAcc = new TimeStampedAngularCoordinates(AbsoluteDate.J2000_EPOCH,
            Rotation.IDENTITY, Vector3D.ZERO, Vector3D.ZERO);

        final Attitude attitude = new Attitude(FramesFactory.getEME2000(), ar);

        Assert.assertEquals(derivative1, attitude.getRotationAcceleration());

        // test the getSpinDerivativesFunction() fails when no spin derivative
        // is added to the Attitude:
        final Attitude attitude2 = new Attitude(FramesFactory.getEME2000(), arNoAcc);
        Assert.assertEquals(attitude2.getRotationAcceleration(), Vector3D.ZERO);
    }

    @Test
    public void testZeroRate() {
        final Attitude attitude =
            new Attitude(AbsoluteDate.J2000_EPOCH, FramesFactory.getEME2000(), new Rotation(false,
                0.48, 0.64, 0.36, 0.48), Vector3D.ZERO);
        Assert.assertEquals(Vector3D.ZERO, attitude.getSpin());
        final double dt = 10.0;
        final Attitude shifted = attitude.shiftedBy(dt);
        Assert.assertEquals(Vector3D.ZERO, shifted.getSpin());
        Assert.assertEquals(attitude.getRotation(), shifted.getRotation());
    }

    @Test
    public void testShift() {
        final double rate = 2 * FastMath.PI / (12 * 60);
        final Attitude attitude = new Attitude(AbsoluteDate.J2000_EPOCH, FramesFactory.getEME2000(), Rotation.IDENTITY,
            new Vector3D(rate, Vector3D.PLUS_K));
        Assert.assertEquals(rate, attitude.getSpin().getNorm(), 1.0e-10);
        final double dt = 10.0;
        final double alpha = rate * dt;
        final Attitude shifted = attitude.shiftedBy(dt);
        Assert.assertEquals(rate, shifted.getSpin().getNorm(), 1.0e-10);
        Assert.assertEquals(alpha, Rotation.distance(attitude.getRotation(), shifted.getRotation()), 1.0e-10);

        final Vector3D xSat = shifted.getRotation().applyTo(Vector3D.PLUS_I);
        Assert.assertEquals(0.0, xSat.subtract(new Vector3D(MathLib.cos(alpha), MathLib.sin(alpha), 0)).getNorm(),
            1.0e-10);
        final Vector3D ySat = shifted.getRotation().applyTo(Vector3D.PLUS_J);
        Assert.assertEquals(0.0, ySat.subtract(new Vector3D(-MathLib.sin(alpha), MathLib.cos(alpha), 0)).getNorm(),
            1.0e-10);
        final Vector3D zSat = shifted.getRotation().applyTo(Vector3D.PLUS_K);
        Assert.assertEquals(0.0, zSat.subtract(Vector3D.PLUS_K).getNorm(), 1.0e-10);

    }

    final double eps = Precision.DOUBLE_COMPARISON_EPSILON;

    @Test
    public void testSpin() {
        final double rate = 2 * FastMath.PI / (12 * 60);
        final Attitude attitude =
            new Attitude(AbsoluteDate.J2000_EPOCH, FramesFactory.getEME2000(), new Rotation(false,
                0.48, 0.64, 0.36, 0.48), new Vector3D(rate, Vector3D.PLUS_K));
        Assert.assertEquals(rate, attitude.getSpin().getNorm(), 1.0e-10);
        final double dt = 10.0;
        final Attitude shifted = attitude.shiftedBy(dt);
        Assert.assertEquals(rate, shifted.getSpin().getNorm(), 1.0e-10);
        Assert.assertEquals(rate * dt, Rotation.distance(attitude.getRotation(), shifted.getRotation()), 1.0e-10);

        final Vector3D shiftedX = shifted.getRotation().applyTo(Vector3D.PLUS_I);
        final Vector3D shiftedY = shifted.getRotation().applyTo(Vector3D.PLUS_J);
        final Vector3D shiftedZ = shifted.getRotation().applyTo(Vector3D.PLUS_K);
        final Vector3D originalX = attitude.getRotation().applyTo(Vector3D.PLUS_I);
        final Vector3D originalY = attitude.getRotation().applyTo(Vector3D.PLUS_J);
        final Vector3D originalZ = attitude.getRotation().applyTo(Vector3D.PLUS_K);
        Assert.assertEquals(MathLib.cos(rate * dt), Vector3D.dotProduct(shiftedX, originalX), 1.0e-10);
        Assert.assertEquals(MathLib.sin(rate * dt), Vector3D.dotProduct(shiftedX, originalY), 1.0e-10);
        Assert.assertEquals(0.0, Vector3D.dotProduct(shiftedX, originalZ), 1.0e-10);
        Assert.assertEquals(-MathLib.sin(rate * dt), Vector3D.dotProduct(shiftedY, originalX), 1.0e-10);
        Assert.assertEquals(MathLib.cos(rate * dt), Vector3D.dotProduct(shiftedY, originalY), 1.0e-10);
        Assert.assertEquals(0.0, Vector3D.dotProduct(shiftedY, originalZ), 1.0e-10);
        Assert.assertEquals(0.0, Vector3D.dotProduct(shiftedZ, originalX), 1.0e-10);
        Assert.assertEquals(0.0, Vector3D.dotProduct(shiftedZ, originalY), 1.0e-10);
        Assert.assertEquals(1.0, Vector3D.dotProduct(shiftedZ, originalZ), 1.0e-10);

        final Vector3D forward = AngularCoordinates.estimateRate(attitude.getRotation(), shifted.getRotation(), dt);
        Assert.assertEquals(0.0, forward.subtract(attitude.getSpin()).getNorm(), 1.0e-10);

        final Vector3D reversed = AngularCoordinates.estimateRate(shifted.getRotation(), attitude.getRotation(), dt);
        Assert.assertEquals(0.0, reversed.add(attitude.getSpin()).getNorm(), 1.0e-10);

    }

    @Test
    public void testInterpolation() throws PatriusException {

        final double ehMu = 3.9860047e14;
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        final Vector3D position = new Vector3D(3220103., 69623., 6449822.);
        final Vector3D velocity = new Vector3D(6414.7, -2006., -3180.);
        final CircularOrbit initialOrbit = new CircularOrbit(new PVCoordinates(position, velocity),
            FramesFactory.getEME2000(), date, ehMu);

        final KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit);
        propagator.setAttitudeProvider(new BodyCenterPointing(FramesFactory.getITRF()));
        final Attitude initialAttitude = propagator.propagate(initialOrbit.getDate()).getAttitude();

        // set up a 5 points sample
        final List<Attitude> sample = new ArrayList<>();
        for (double dt = 0; dt < 251.0; dt += 60.0) {
            sample.add(propagator.propagate(date.shiftedBy(dt)).getAttitude());
        }

        // well inside the sample, interpolation should be better than linear shift
        double maxShiftAngleError = 0;
        double maxInterpolationAngleError = 0;
        double maxShiftRateError = 0;
        double maxInterpolationRateError = 0;
        for (double dt = 0; dt < 240.0; dt += 1.0) {
            final AbsoluteDate t = initialOrbit.getDate().shiftedBy(dt);
            final Attitude propagated = propagator.propagate(t).getAttitude();
            final double shiftAngleError = Rotation.distance(propagated.getRotation(), initialAttitude.shiftedBy(dt)
                .getRotation());
            final double interpolationAngleError = Rotation.distance(propagated.getRotation(),
                initialAttitude.interpolate(t, sample).getRotation());
            final double shiftRateError =
                Vector3D.distance(propagated.getSpin(), initialAttitude.shiftedBy(dt).getSpin());
            final double interpolationRateError = Vector3D.distance(propagated.getSpin(),
                initialAttitude.interpolate(t, sample).getSpin());
            maxShiftAngleError = MathLib.max(maxShiftAngleError, shiftAngleError);
            maxInterpolationAngleError = MathLib.max(maxInterpolationAngleError, interpolationAngleError);
            maxShiftRateError = MathLib.max(maxShiftRateError, shiftRateError);
            maxInterpolationRateError = MathLib.max(maxInterpolationRateError, interpolationRateError);
        }
        Assert.assertTrue(maxShiftAngleError > 4.0e-6);
        Assert.assertTrue(maxInterpolationAngleError < 1.5e-13);
        Assert.assertTrue(maxShiftRateError > 4.0e-8);
        Assert.assertTrue(maxInterpolationRateError < 2.5e-14);

        // past sample end, interpolation error should increase, but still be far better than linear shif
        maxShiftAngleError = 0;
        maxInterpolationAngleError = 0;
        maxShiftRateError = 0;
        maxInterpolationRateError = 0;
        for (double dt = 250.0; dt < 300.0; dt += 1.0) {
            final AbsoluteDate t = initialOrbit.getDate().shiftedBy(dt);
            final Attitude propagated = propagator.propagate(t).getAttitude();
            final double shiftAngleError = Rotation.distance(propagated.getRotation(), initialAttitude.shiftedBy(dt)
                .getRotation());
            final double interpolationAngleError = Rotation.distance(propagated.getRotation(),
                initialAttitude.interpolate(t, sample).getRotation());
            final double shiftRateError =
                Vector3D.distance(propagated.getSpin(), initialAttitude.shiftedBy(dt).getSpin());
            final double interpolationRateError = Vector3D.distance(propagated.getSpin(),
                initialAttitude.interpolate(t, sample).getSpin());
            maxShiftAngleError = MathLib.max(maxShiftAngleError, shiftAngleError);
            maxInterpolationAngleError = MathLib.max(maxInterpolationAngleError, interpolationAngleError);
            maxShiftRateError = MathLib.max(maxShiftRateError, shiftRateError);
            maxInterpolationRateError = MathLib.max(maxInterpolationRateError, interpolationRateError);
        }
        Assert.assertTrue(maxShiftAngleError > 9.0e-6);
        Assert.assertTrue(maxInterpolationAngleError < 6.0e-11);
        Assert.assertTrue(maxShiftRateError > 6.0e-8);
        Assert.assertTrue(maxInterpolationRateError < 4.0e-12);
    }

    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("regular-data");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));
    }

    @Test
    public void testWithRefFrame() throws PatriusException {

        final double rate = 2 * FastMath.PI / (12 * 60);
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Frame frame = FramesFactory.getEME2000();

        // FRAME BASED
        final Rotation rot = new Rotation(false, 0.48, 0.64, 0.36, 0.48);
        final Vector3D spin = new Vector3D(rate, Vector3D.PLUS_K);
        final Vector3D acc = new Vector3D(rate * rate, Vector3D.PLUS_K);

        // Attitudes
        final Attitude a = new Attitude(date, frame, new AngularCoordinates(rot, spin, acc));

        final Frame gcrf = FramesFactory.getGCRF();

        final Transform t = gcrf.getTransformTo(frame, date, true);

        final Attitude aG = a.withReferenceFrame(gcrf, true);

        Assert.assertEquals(0, t.getRotation().applyTo(a.getRotation()).applyInverseTo(aG.getRotation()).getAngle(),
            this.eps);
        Assert.assertEquals(0, a.getSpin().add(a.getRotation().applyTo(t.getRotationRate())).negate().add(aG.getSpin())
            .getNorm(), this.eps);
        Assert.assertEquals(0, a.getRotationAcceleration().add(a.getRotation().applyTo(t.getRotationAcceleration()))
            .negate().add(aG.getRotationAcceleration()).getNorm(), this.eps);
        final Attitude aG2 = a.withReferenceFrame(a.getReferenceFrame());

        Assert.assertTrue(a.equals(aG2));
    }

    @Test
    public void testSlerp() throws PatriusException {

        // attitudes creation
        final Rotation rot1 = new Rotation(false, 0.48, 0.64, 0.36, 0.48);
        final Rotation rot2 = new Rotation(false, 0.64, 0.48, 0.48, 0.36);

        final AbsoluteDate date0 = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate date1 = AbsoluteDate.J2000_EPOCH.shiftedBy(10.0);
        final AbsoluteDate date2 = AbsoluteDate.J2000_EPOCH.shiftedBy(20.0);
        final AbsoluteDate dateTest1 = AbsoluteDate.J2000_EPOCH.shiftedBy(7.0);
        final AbsoluteDate dateTest2 = AbsoluteDate.J2000_EPOCH.shiftedBy(13.5);
        final AbsoluteDate dateOut = AbsoluteDate.J2000_EPOCH.shiftedBy(50.0);

        final Attitude attitude0 = new Attitude(date0, FramesFactory.getEME2000(), rot1, Vector3D.ZERO);
        final Attitude attitude1 = new Attitude(date1, FramesFactory.getEME2000(), rot2, Vector3D.ZERO);
        final Attitude attitude2 = new Attitude(date2, FramesFactory.getEME2000(), rot1, Vector3D.ZERO);

        // test between the two first attitudes
        // =====================================
        Attitude resultAtt = Attitude.slerp(dateTest1, attitude0, attitude1, FramesFactory.getEME2000(), false);
        Rotation resultRot = resultAtt.getRotation();

        // expected rotation ; computed with the validated slerp method
        Rotation expectedRot = Rotation.slerp(rot1, rot2, 7.0 / 10.0);

        // comparison
        double nullAngle = expectedRot.applyInverseTo(resultRot).getAngle();
        Assert.assertEquals(nullAngle, 0.0, this.comparisonEpsilon);

        // test between the 2 and 3 attitudes
        // =====================================

        resultAtt = Attitude.slerp(dateTest2, attitude1, attitude2, FramesFactory.getEME2000(), true);
        resultRot = resultAtt.getRotation();

        // expected rotation ; computed with the validated slerp method
        expectedRot = Rotation.slerp(rot2, rot1, 3.5 / 10.0);

        // comparison
        nullAngle = expectedRot.applyInverseTo(resultRot).getAngle();
        Assert.assertEquals(nullAngle, 0.0, this.comparisonEpsilon);

        // test right on the 2nd attitude superior limit
        // ==============================================

        resultAtt = Attitude.slerp(date1, attitude0, attitude1, FramesFactory.getEME2000(), true);
        resultRot = resultAtt.getRotation();

        // comparison
        nullAngle = rot2.applyInverseTo(resultRot).getAngle();
        Assert.assertEquals(nullAngle, 0.0, this.comparisonEpsilon);

        // test right on the 2nd attitude inferior limit
        // ===============================================

        resultAtt = Attitude.slerp(date1, attitude1, attitude2, FramesFactory.getEME2000(), true);
        resultRot = resultAtt.getRotation();

        // comparison
        nullAngle = rot2.applyInverseTo(resultRot).getAngle();
        Assert.assertEquals(nullAngle, 0.0, this.comparisonEpsilon);

        // test with same attitude
        // ===============================================

        resultAtt = Attitude.slerp(date1, attitude1, attitude1, FramesFactory.getEME2000(), true);
        resultRot = resultAtt.getRotation();

        // comparison
        nullAngle = attitude1.getRotation().applyInverseTo(resultRot).getAngle();
        Assert.assertEquals(nullAngle, 0.0, this.comparisonEpsilon);

        // test right out of the time interval
        // ==================================
        try {
            resultAtt = Attitude.slerp(dateOut, attitude1, attitude2, FramesFactory.getEME2000(), true);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected !
        }
    }

    @Test
    public void testSerialisation() {
        final double rate = 2 * FastMath.PI / (12 * 60);
        final double dt = 10.0;
        final Attitude attitude1 =
            new Attitude(AbsoluteDate.J2000_EPOCH, FramesFactory.getEME2000(), new Rotation(false,
                0.48, 0.64, 0.36, 0.48), new Vector3D(rate, Vector3D.PLUS_K));
        final Attitude attitude2 = attitude1.shiftedBy(dt);

        final Attitude attitude3 =
            new Attitude(AbsoluteDate.J2000_EPOCH, FramesFactory.getEME2000(), new Rotation(false,
                0.64, 0.36, 0.48, 0.48), Vector3D.ZERO, Vector3D.ZERO);
        final Attitude attitude4 = attitude3.shiftedBy(dt);

        final Attitude[] attitudes = { attitude1, attitude2, attitude3, attitude4 };

        for (final Attitude attitude : attitudes) {
            final Attitude attitudeBis = TestUtils.serializeAndRecover(attitude);
            assertEqualsAttitude(attitudeBis, attitude);
        }
    }

    private static void assertEqualsAttitude(final Attitude attitudeBis, final Attitude attitude) {
        TimeStampedAngularCoordinatesTest.assertEqualsTimeStampedAngularCoordinates(attitudeBis.getOrientation(),
            attitude.getOrientation());
        Assert.assertEquals(attitude.getReferenceFrame().getName(), attitudeBis.getReferenceFrame().getName());
    }
}
