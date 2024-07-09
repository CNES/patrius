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
 * VERSION::DM:393:12/03/2015: Constant Attitude Laws
 * VERSION::DM:489:06/10/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:583:11/03/2016:simplification of attitude laws architecture
 * VERSION::DM:603:29/08/2016:deleted deprecated methods and classes in package attitudes
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.AbstractVector3DFunction;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.RotationOrder;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3DFunction;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class InertialAttitudeTest {

    private AbsoluteDate t0;
    private Orbit orbit0;
    private Orbit orbit1;

    @Test
    public void testIsInertial() throws PatriusException {
        final ConstantAttitudeLaw law =
            new ConstantAttitudeLaw(FramesFactory.getEME2000(), new Rotation(new Vector3D(0.6,
                0.48, 0.64), 0.9));
        final KeplerianPropagator propagator = new KeplerianPropagator(this.orbit0, law);
        final Attitude initial = propagator.propagate(this.t0).getAttitude();
        for (double t = 0; t < 10000.0; t += 100) {
            final Attitude attitude = propagator.propagate(this.t0.shiftedBy(t)).getAttitude();
            final Rotation evolution = attitude.getRotation().applyTo(initial.getRotation().revert());
            Assert.assertEquals(0, evolution.getAngle(), 1.0e-10);
            Assert.assertEquals(FramesFactory.getEME2000(), attitude.getReferenceFrame());
        }
    }

    @Test
    public void testIsConstantAttitudeLawEME2000() throws PatriusException {
        final ConstantAttitudeLaw law =
            new ConstantAttitudeLaw(FramesFactory.getEME2000(), new Rotation(new Vector3D(0.6,
                0.48, 0.64), 0.9));
        final KeplerianPropagator propagator = new KeplerianPropagator(this.orbit0, law);
        final Attitude initial = propagator.propagate(this.t0).getAttitude();
        for (double t = 0; t < 10000.0; t += 100) {
            final Attitude attitude = propagator.propagate(this.t0.shiftedBy(t)).getAttitude();
            final Rotation evolution = attitude.getRotation().applyTo(initial.getRotation().revert());
            Assert.assertEquals(0, evolution.getAngle(), 1.0e-10);
            Assert.assertEquals(FramesFactory.getEME2000(), attitude.getReferenceFrame());
        }
    }

    @Test
    public void testConstantAttitudeLaw() throws PatriusException {
        // Check that rotation with respect to some frame (here TEME) remains constant through time
        final ConstantAttitudeLaw law = new ConstantAttitudeLaw(FramesFactory.getTEME(), new Rotation(new Vector3D(0.6,
            0.48, 0.64), 0.9));
        law.setSpinDerivativesComputation(true);
        final KeplerianPropagator propagator = new KeplerianPropagator(this.orbit0, law);
        for (double t = 0; t < 10000.0; t += 100) {
            final AbsoluteDate date = this.t0.shiftedBy(t);
            final Attitude attitude = propagator.propagate(date).getAttitude();
            final Rotation rotation = attitude.getRotation();
            final Transform transform = new Transform(date, FramesFactory.getTEME().getTransformTo(
                FramesFactory.getEME2000(), date), new Transform(date, rotation));
            Assert.assertEquals(0.9, transform.getRotation().getAngle(), 1E-15);
            Assert.assertEquals(0.6, transform.getRotation().getAxis().getX(), 1E-15);
            Assert.assertEquals(0.48, transform.getRotation().getAxis().getY(), 1E-15);
            Assert.assertEquals(0.64, transform.getRotation().getAxis().getZ(), 1E-15);

            final Rotation rot = law.getAttitude(null, date, FramesFactory.getTEME()).getRotation();
            Assert.assertEquals(0.9, rot.getAngle(), 1E-15);
            Assert.assertEquals(0.6, rot.getAxis().getX(), 1E-15);
            Assert.assertEquals(0.48, rot.getAxis().getY(), 1E-15);
            Assert.assertEquals(0.64, rot.getAxis().getZ(), 1E-15);
            final Vector3D spin = law.getAttitude(null, date, FramesFactory.getTEME()).getSpin();
            Assert.assertEquals(0.0, spin.getX(), 1E-15);
            Assert.assertEquals(0.0, spin.getY(), 1E-15);
            Assert.assertEquals(0.0, spin.getZ(), 1E-15);
            final Vector3D acc = law.getAttitude(null, date, FramesFactory.getTEME()).getRotationAcceleration();
            Assert.assertEquals(0.0, acc.getX(), 1E-15);
            Assert.assertEquals(0.0, acc.getY(), 1E-15);
            Assert.assertEquals(0.0, acc.getZ(), 1E-15);
        }
    }

    @Test
    public void testConstantAttitudeLawAcceleration() throws PatriusException {
        // Check that derivation of acceleration (with finite difference method) is close to actual rotation
        // acceleration
        final ConstantAttitudeLaw law = new ConstantAttitudeLaw(FramesFactory.getTEME(), new Rotation(new Vector3D(0.6,
            0.48, 0.64), 0.9));
        law.setSpinDerivativesComputation(true);
        for (double t = 0; t < 10000.0; t += 100) {
            final AbsoluteDate date = this.t0.shiftedBy(t);
            final Vector3D acc = law.getAttitude(null, date, FramesFactory.getGCRF()).getRotationAcceleration();
            final Vector3D accDerivateFromSpin = this.getSpinFunction(law, null, FramesFactory.getGCRF(), date)
                .nthDerivative(1).getVector3D(date);
            Assert.assertEquals(acc.distance(accDerivateFromSpin), 0.0, 1e-15);
        }

        // Check rotation acceleration is null when spin derivative is deactivated
        law.setSpinDerivativesComputation(false);
        Assert.assertNull(law.getAttitude(this.orbit0).getRotationAcceleration());
    }

    @Test
    public void testCompensateMomentum() throws PatriusException {
        final ConstantAttitudeLaw law =
            new ConstantAttitudeLaw(FramesFactory.getEME2000(), new Rotation(new Vector3D(-0.64,
                0.6, 0.48), 0.2));
        final KeplerianPropagator propagator = new KeplerianPropagator(this.orbit0, law);
        final Attitude initial = propagator.propagate(this.t0).getAttitude();
        for (double t = 0; t < 10000.0; t += 100) {
            final Attitude attitude = propagator.propagate(this.t0.shiftedBy(t)).getAttitude();
            final Rotation evolution = attitude.getRotation().applyTo(initial.getRotation().revert());
            Assert.assertEquals(0, evolution.getAngle(), 1.0e-10);
            Assert.assertEquals(FramesFactory.getEME2000(), attitude.getReferenceFrame());
        }
    }

    @Test
    public void testCompensateMomentumConstantAttitudeLaw() throws PatriusException {
        final ConstantAttitudeLaw law =
            new ConstantAttitudeLaw(FramesFactory.getEME2000(), new Rotation(new Vector3D(-0.64,
                0.6, 0.48), 0.2));
        final KeplerianPropagator propagator = new KeplerianPropagator(this.orbit0, law);
        final Attitude initial = propagator.propagate(this.t0).getAttitude();
        for (double t = 0; t < 10000.0; t += 100) {
            final Attitude attitude = propagator.propagate(this.t0.shiftedBy(t)).getAttitude();
            final Rotation evolution = attitude.getRotation().applyTo(initial.getRotation().revert());
            Assert.assertEquals(0, evolution.getAngle(), 1.0e-10);
            Assert.assertEquals(FramesFactory.getEME2000(), attitude.getReferenceFrame());
        }
    }

    @Test
    public void testSpin() throws PatriusException {

        final AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 01, 01),
            new TimeComponents(3, 25, 45.6789),
            TimeScalesFactory.getUTC());

        final AbstractAttitudeLaw law = new ConstantAttitudeLaw(FramesFactory.getEME2000(), new Rotation(new Vector3D(
            -0.64, 0.6, 0.48), 0.2));

        final KeplerianOrbit orbit =
            new KeplerianOrbit(7178000.0, 1.e-4, MathLib.toRadians(50.),
                MathLib.toRadians(10.), MathLib.toRadians(20.),
                MathLib.toRadians(30.), PositionAngle.MEAN,
                FramesFactory.getEME2000(), date, 3.986004415e14);

        final Propagator propagator = new KeplerianPropagator(orbit, law);

        final double h = 100.0;
        final SpacecraftState sMinus = propagator.propagate(date.shiftedBy(-h));
        final SpacecraftState sPlus = propagator.propagate(date.shiftedBy(h));

        final Rotation rMinus = law.getAttitude(orbit, date.shiftedBy(-h), orbit.getFrame()).getRotation();
        final Rotation r0 = law.getAttitude(orbit, date, orbit.getFrame()).getRotation();
        final Rotation rPlus = law.getAttitude(orbit, date.shiftedBy(h), orbit.getFrame()).getRotation();

        // check spin is consistent with attitude evolution
        final double errorAngleMinus = Rotation.distance(sMinus.shiftedBy(h).getAttitude().getRotation(), r0);
        final double evolutionAngleMinus = Rotation.distance(rMinus, r0);
        Assert.assertEquals(0.0, errorAngleMinus, 1.0e-6 * evolutionAngleMinus);
        final double errorAnglePlus = Rotation.distance(r0, sPlus.shiftedBy(-h).getAttitude().getRotation());
        final double evolutionAnglePlus = Rotation.distance(r0, rPlus);
        Assert.assertEquals(0.0, errorAnglePlus, 1.0e-6 * evolutionAnglePlus);

        // compute spin axis using finite differences
        final Rotation dr = rPlus.applyTo(rMinus.revert());
        Assert.assertEquals(0, dr.getAngle(), 1.0e-10);

        final Vector3D spin0 = law.getAttitude(orbit, date, orbit.getFrame()).getSpin();
        Assert.assertEquals(0, spin0.getNorm(), 1.0e-10);

    }

    @Test
    public void testSpinConstantAttitudeLaw() throws PatriusException {

        final AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 01, 01),
            new TimeComponents(3, 25, 45.6789),
            TimeScalesFactory.getUTC());

        final AttitudeProvider law =
            new ConstantAttitudeLaw(FramesFactory.getEME2000(), (new Rotation(new Vector3D(-0.64,
                0.6, 0.48), 0.2)));

        final KeplerianOrbit orbit =
            new KeplerianOrbit(7178000.0, 1.e-4, MathLib.toRadians(50.),
                MathLib.toRadians(10.), MathLib.toRadians(20.),
                MathLib.toRadians(30.), PositionAngle.MEAN,
                FramesFactory.getEME2000(), date, 3.986004415e14);

        final Propagator propagator = new KeplerianPropagator(orbit, law);

        final double h = 100.0;
        final SpacecraftState sMinus = propagator.propagate(date.shiftedBy(-h));
        final SpacecraftState s0 = propagator.propagate(date);
        final SpacecraftState sPlus = propagator.propagate(date.shiftedBy(h));

        // check spin is consistent with attitude evolution
        final double errorAngleMinus = Rotation.distance(sMinus.shiftedBy(h).getAttitude().getRotation(),
            s0.getAttitude().getRotation());
        final double evolutionAngleMinus = Rotation.distance(sMinus.getAttitude().getRotation(),
            s0.getAttitude().getRotation());
        Assert.assertEquals(0.0, errorAngleMinus, 1.0e-6 * evolutionAngleMinus);
        final double errorAnglePlus = Rotation.distance(s0.getAttitude().getRotation(),
            sPlus.shiftedBy(-h).getAttitude().getRotation());
        final double evolutionAnglePlus = Rotation.distance(s0.getAttitude().getRotation(),
            sPlus.getAttitude().getRotation());
        Assert.assertEquals(0.0, errorAnglePlus, 1.0e-6 * evolutionAnglePlus);

        // compute spin axis using finite differences
        final Rotation rMinus = sMinus.getAttitude().getRotation();
        final Rotation rPlus = sPlus.getAttitude().getRotation();
        final Rotation dr = rPlus.applyTo(rMinus.revert());
        Assert.assertEquals(0, dr.getAngle(), 1.0e-10);

        final Vector3D spin0 = s0.getAttitude().getSpin();
        Assert.assertEquals(0, spin0.getNorm(), 1.0e-10);

    }

    /**
     * Local function to provide spin function.
     * 
     * @param pvProv
     *        local position-velocity provider around current date
     * @param frame
     *        reference frame from which spin function of date is computed
     * @param zeroAbscissa
     *        the date for which x=0 for spin function of date
     * @param law
     *        law
     * @return spin function of date relative
     */
    public Vector3DFunction getSpinFunction(final ConstantAttitudeLaw law, final PVCoordinatesProvider pvProv,
                                            final Frame frame,
                                            final AbsoluteDate zeroAbscissa) {
        return new AbstractVector3DFunction(zeroAbscissa){
            @Override
            public Vector3D getVector3D(final AbsoluteDate date) throws PatriusException {
                return law.getAttitude(pvProv, date, frame).getSpin();
            }
        };
    }

    /**
     * @testType UT
     * 
     * 
     * @description Test the getters of a class.
     * 
     * @input the class parameters
     * 
     * @output the class parameters
     * 
     * @testPassCriteria the parameters of the class are the same in input and
     *                   output
     * 
     * @referenceVersion 4.1
     * 
     * @nonRegressionVersion 4.1
     */
    @Test
    public void testGetters() {
        final Frame referenceFrame = FramesFactory.getGCRF();
        final RotationOrder order = RotationOrder.XYZ;
        final double alpha1 = 0;
        final double alpha2 = 1;
        final double alpha3 = 2;
        final Rotation rotation = new Rotation(order, alpha1, alpha2, alpha3);
        final ConstantAttitudeLaw cstAttitudeLaw = new ConstantAttitudeLaw(referenceFrame, rotation);
        Assert.assertEquals(referenceFrame.getName(), cstAttitudeLaw.getReferenceFrame().getName());
        Assert.assertEquals(rotation.getAngle(), cstAttitudeLaw.getRotation().getAngle(), 0);

    }

    @Before
    public void setUp() {
        try {
            Utils.setDataRoot("regular-data");

            this.t0 = new AbsoluteDate(new DateComponents(2008, 06, 03), TimeComponents.H12,
                TimeScalesFactory.getUTC());
            this.orbit0 =
                new KeplerianOrbit(12345678.9, 0.001, 2.3, 0.1, 3.04, 2.4,
                    PositionAngle.TRUE, FramesFactory.getEME2000(),
                    this.t0, 3.986004415e14);
            this.orbit1 =
                new KeplerianOrbit(12345678.9, 0.001, 2.3, 0.1, 3.04, 2.4,
                    PositionAngle.TRUE, FramesFactory.getGCRF(),
                    this.t0, 3.986004415e14);

        } catch (final PatriusException oe) {
            Assert.fail(oe.getMessage());
        }
    }

    @After
    public void tearDown() {
        this.t0 = null;
        this.orbit0 = null;
    }

}
