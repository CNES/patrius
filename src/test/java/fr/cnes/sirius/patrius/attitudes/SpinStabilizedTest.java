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
 * VERSION::DM:489:12/01/2016:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:583:11/03/2016:simplification of attitude laws architecture
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.AbstractVector3DFunction;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3DFunction;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class SpinStabilizedTest {

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(SpinStabilizedTest.class.getSimpleName(), "Spin stabilized attitude provider");
    }

    @Test
    public void testBBQMode() throws PatriusException {

        final PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        final AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 01, 01),
            new TimeComponents(3, 25, 45.6789),
            TimeScalesFactory.getTAI());
        final double rate = 2.0 * FastMath.PI / (12 * 60);
        final AttitudeProvider bbq =
            new SpinStabilized(new CelestialBodyPointed(FramesFactory.getEME2000(), sun, Vector3D.PLUS_K,
                Vector3D.PLUS_I, Vector3D.PLUS_K),
                date, Vector3D.PLUS_K, rate);
        final PVCoordinates pv =
            new PVCoordinates(new Vector3D(28812595.32012577, 5948437.4640250085, 0),
                new Vector3D(0, 0, 3680.853673522056));
        final KeplerianOrbit kep = new KeplerianOrbit(pv, FramesFactory.getEME2000(), date, 3.986004415e14);
        final Attitude attitude = bbq.getAttitude(kep, date, kep.getFrame());
        final Vector3D xDirection = attitude.getRotation().applyTo(Vector3D.PLUS_I);

        Report.printMethodHeader("testBBQMode", "Angle computation", "Math", 2.0e-15, ComparisonType.ABSOLUTE);

        Assert.assertEquals(MathLib.atan(1.0 / 5000.0),
            Vector3D.angle(xDirection, sun.getPVCoordinates(date, FramesFactory.getEME2000()).getPosition()),
            2.0e-15);
        Report.printToReport("Angle at date", MathLib.atan(1.0 / 5000.0),
            Vector3D.angle(xDirection, sun.getPVCoordinates(date, FramesFactory.getEME2000()).getPosition()));

        Report.printMethodHeader("testSpin", "Spin computation", "Finite differences", 1.0e-6, ComparisonType.ABSOLUTE);

        Assert.assertEquals(rate, attitude.getSpin().getNorm(), 1.0e-6);
        Report.printToReport("Spin rate at date", rate, attitude.getSpin().getNorm());

    }

    @Test
    public void testSpin() throws PatriusException {

        final AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 01, 01),
            new TimeComponents(3, 25, 45.6789),
            TimeScalesFactory.getUTC());
        final double rate = 2.0 * FastMath.PI / (12 * 60);
        final AbstractAttitudeLaw law =
            new SpinStabilized(new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY),
                date, Vector3D.PLUS_K, rate);
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7178000.0, 1.e-4, MathLib.toRadians(50.),
                MathLib.toRadians(10.), MathLib.toRadians(20.),
                MathLib.toRadians(30.), PositionAngle.MEAN,
                FramesFactory.getEME2000(), date, 3.986004415e14);

        final Propagator propagator = new KeplerianPropagator(orbit, law);

        final double h = 10.0;
        final SpacecraftState sMinus = propagator.propagate(date.shiftedBy(-h));
        final SpacecraftState sPlus = propagator.propagate(date.shiftedBy(h));

        final Rotation rMinus = law.getAttitude(orbit, date.shiftedBy(-h), orbit.getFrame()).getRotation();
        final Rotation r0 = law.getAttitude(orbit, date, orbit.getFrame()).getRotation();
        final Rotation rPlus = law.getAttitude(orbit, date.shiftedBy(h), orbit.getFrame()).getRotation();

        final Vector3D spin0 = law.getAttitude(orbit, date, orbit.getFrame()).getSpin();

        // check spin is consistent with attitude evolution
        final double errorAngleMinus = Rotation.distance(sMinus.shiftedBy(h).getAttitude().getRotation(), r0);
        final double evolutionAngleMinus = Rotation.distance(rMinus, r0);
        Assert.assertTrue(errorAngleMinus <= 1.0e-6 * evolutionAngleMinus);
        final double errorAnglePlus = Rotation.distance(r0, sPlus.shiftedBy(-h).getAttitude().getRotation());
        final double evolutionAnglePlus = Rotation.distance(r0, rPlus);
        Assert.assertTrue(errorAnglePlus <= 1.0e-6 * evolutionAnglePlus);

        // compute spin axis using finite differences
        final Rotation rM = sMinus.getAttitude().getRotation();
        final Rotation rP = sPlus.getAttitude().getRotation();
        final Vector3D reference = AngularCoordinates.estimateRate(rM, rP, 2 * h);

        Assert.assertEquals(2 * FastMath.PI / reference.getNorm(), 2 * FastMath.PI / spin0.getNorm(), 0.05);
        Assert.assertEquals(0.0, MathLib.toDegrees(Vector3D.angle(reference, spin0)), 1.0e-10);
        Assert.assertEquals(0.0, MathLib.toDegrees(Vector3D.angle(Vector3D.PLUS_K, spin0)), 1.0e-10);

    }

    @Test
    public void testRotationAcceleration() throws PatriusException {

        Report.printMethodHeader("testRotationAcceleration", "Rotation acceleration computation", "Finite differences",
            1E-15, ComparisonType.ABSOLUTE);

        Utils.setDataRoot("regular-data");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        // Computation date
        final AbsoluteDate date = new AbsoluteDate(new DateComponents(2008, 04, 07),
            TimeComponents.H00,
            TimeScalesFactory.getUTC());

        // Satellite position as circular parameters
        final double mu = 3.9860047e14;
        final double raan = 270.;
        final CircularOrbit circ =
            new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, MathLib.toRadians(50.), MathLib.toRadians(raan),
                MathLib.toRadians(5.300 - raan), PositionAngle.MEAN,
                FramesFactory.getEME2000(), date, mu);

        final double rate = 2 * FastMath.PI / (12 * 60);
        final ConstantAttitudeLaw cstSpinLaw = new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY);
        final SpinStabilized law = new SpinStabilized(cstSpinLaw, date, Vector3D.PLUS_K, rate);
        law.setSpinDerivativesComputation(true);

        // Check that :
        // - derivation of spin with finite difference method is closed to acceleration
        // - acceleration is null on +K axis
        for (int i = 0; i < 10000; i += 100) {
            final Frame frameToCompute = FramesFactory.getITRF();
            final Vector3D acc = law.getAttitude(circ, date.shiftedBy(i), FramesFactory.getITRF())
                .getRotationAcceleration();
            final Vector3D accDerivateSpin = this.getSpinFunction(law, null, frameToCompute, date.shiftedBy(i))
                .nthDerivative(1).getVector3D(date.shiftedBy(i));
            // derivation of spin with finite difference method is closed to acceleration
            Assert.assertEquals(acc.distance(accDerivateSpin), 0.0, 1e-15);
            // derivation of spin with finite difference method is closed to acceleration
            Assert.assertEquals(acc.getZ(), 0.0, 0.0);
            if (i == 0) {
                Report.printToReport("Rotation acceleration at date", accDerivateSpin, acc);
            }
        }

        // Check rotation acceleration is null when spin derivative is deactivated
        law.setSpinDerivativesComputation(false);
        Assert.assertNull(law.getAttitude(circ).getRotationAcceleration());
        Assert.assertNotNull(cstSpinLaw.toString());
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
    public Vector3DFunction getSpinFunction(final SpinStabilized law, final PVCoordinatesProvider pvProv,
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
        final LofOffset pNonRotatingLaw = new LofOffset(LOFType.QSW);
        final AbsoluteDate pStart = new AbsoluteDate(2005, 12, 12, TimeScalesFactory.getTAI());
        final Vector3D pAxis = new Vector3D(2000, 11500, 140);
        final double pRate = 0.2;
        final SpinStabilized spinStabilized = new SpinStabilized(pNonRotatingLaw, pStart, pAxis, pRate);

        Assert.assertEquals(pNonRotatingLaw.getRotation().getAngle(), ((LofOffset) spinStabilized.getNonRotatingLaw())
            .getRotation().getAngle(), 0);
        Assert.assertEquals(0, pStart.durationFrom(spinStabilized.getStartDate()), 0);
        Assert.assertEquals(pAxis.getX(), spinStabilized.getAxis().getX(), 0);
        Assert.assertEquals(pRate, spinStabilized.getRate(), 0);
        Assert.assertNotNull(spinStabilized.toString());
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
