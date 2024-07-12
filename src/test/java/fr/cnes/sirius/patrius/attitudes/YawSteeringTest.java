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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2102:15/05/2019:[Patrius] Refactoring du paquet fr.cnes.sirius.patrius.bodies
 * VERSION:4.3:FA:FA-1978:15/05/2019:Anomalie calcul orientation corps celeste (UAI)
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:489:15/10/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:583:11/03/2016:simplification of attitude laws architecture
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.AbstractVector3DFunction;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3DFunction;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.TimeStampedPVCoordinates;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagatorTest;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.CartesianDerivativesFilter;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class YawSteeringTest {

    // Computation date
    private AbsoluteDate date;

    // Reference frame = ITRF 2005C
    private Frame frameITRF2005;

    // Satellite position
    CircularOrbit circOrbit;

    // Earth shape
    OneAxisEllipsoid earthShape;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(KeplerianPropagatorTest.class.getSimpleName(), "Yaw steering attitude provider");
    }

    @Test
    public void testTarget() throws PatriusException {

        // Attitude laws
        // **************
        // Target pointing attitude provider without yaw compensation
        final NadirPointing nadirLaw = new NadirPointing(this.earthShape);

        // Target pointing attitude provider with yaw compensation
        final YawSteering yawCompensLaw =
            new YawSteering(nadirLaw, CelestialBodyFactory.getSun(), Vector3D.MINUS_I);

        // Check observed ground point
        // *****************************
        // without yaw compensation
        final Vector3D noYawObserved = nadirLaw.getTargetPoint(this.circOrbit, this.date, this.frameITRF2005);

        // with yaw compensation
        final Vector3D yawObserved = yawCompensLaw.getTargetPoint(this.circOrbit, this.date, this.frameITRF2005);

        // Check difference
        final Vector3D observedDiff = noYawObserved.subtract(yawObserved);

        Assert.assertTrue(observedDiff.getNorm() < Utils.epsilonTest);

        new YawSteering(new NadirPointing(this.earthShape),
            CelestialBodyFactory.getSun(),
            Vector3D.MINUS_I);
    }

    @Test
    public void testSunAligned() throws PatriusException {

        Report.printMethodHeader("testSunAligned", "Rotation computation", "Orekit v7", 1E-7, ComparisonType.ABSOLUTE);

        // Attitude laws
        // **************
        // Target pointing attitude provider over satellite nadir at date, without yaw compensation
        final NadirPointing nadirLaw = new NadirPointing(this.earthShape);

        // Target pointing attitude provider with yaw compensation
        final PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        final YawSteering yawCompensLaw = new YawSteering(nadirLaw, sun, Vector3D.MINUS_I);

        // Get sun direction in satellite frame
        final Rotation rotYaw =
            yawCompensLaw.getAttitude(this.circOrbit, this.date, this.circOrbit.getFrame()).getRotation();
        final Vector3D sunEME2000 = sun.getPVCoordinates(this.date, FramesFactory.getEME2000()).getPosition();
        final Vector3D sunSat = rotYaw.applyInverseTo(sunEME2000);

        // Check sun is in (X,Z) plane
        Assert.assertEquals(0.0, MathLib.sin(sunSat.getAlpha()), 1.0e-7);

        Report.printToReport("Sun elevation at date", 0, MathLib.sin(sunSat.getAlpha()));
    }

    @Test
    public void testCompensAxis() throws PatriusException {

        // Attitude laws
        // **************
        // Target pointing attitude provider over satellite nadir at date, without yaw compensation
        final NadirPointing nadirLaw = new NadirPointing(this.earthShape);

        // Target pointing attitude provider with yaw compensation
        final YawSteering yawCompensLaw =
            new YawSteering(nadirLaw, CelestialBodyFactory.getSun(), Vector3D.MINUS_I, Vector3D.PLUS_J, Vector3D.PLUS_K);

        // Get attitude rotations from non yaw compensated / yaw compensated laws
        final Rotation rotNoYaw =
            nadirLaw.getAttitude(this.circOrbit, this.date, this.circOrbit.getFrame()).getRotation();
        final Rotation rotYaw =
            yawCompensLaw.getAttitude(this.circOrbit, this.date, this.circOrbit.getFrame()).getRotation();

        // Compose rotations composition
        final Rotation compoRot = rotYaw.applyTo(rotNoYaw.revert());
        final Vector3D yawAxis = rotNoYaw.applyInverseTo(compoRot.getAxis());

        // Check axis
        Assert.assertEquals(0., Vector3D.crossProduct(yawAxis, Vector3D.PLUS_K).getNorm(), Utils.epsilonTest);

    }

    @Test
    public void testSpin() throws PatriusException {

        Report
            .printMethodHeader("testSpin", "Spin computation", "Finite differences", 2.0e-12, ComparisonType.ABSOLUTE);

        final NadirPointing nadirLaw = new NadirPointing(this.earthShape);

        // Target pointing attitude provider with yaw compensation
        final AbstractAttitudeLaw law = new YawSteering(nadirLaw, CelestialBodyFactory.getSun(), Vector3D.MINUS_I);

        final KeplerianOrbit orbit =
            new KeplerianOrbit(7178000.0, 1.e-4, MathLib.toRadians(50.),
                MathLib.toRadians(10.), MathLib.toRadians(20.),
                MathLib.toRadians(30.), PositionAngle.MEAN,
                FramesFactory.getEME2000(),
                this.date.shiftedBy(-300.0),
                3.986004415e14);

        final Propagator propagator = new KeplerianPropagator(orbit, law);

        final double h = 0.01;
        final SpacecraftState sMinus = propagator.propagate(this.date.shiftedBy(-h));
        final SpacecraftState sPlus = propagator.propagate(this.date.shiftedBy(h));

        final Rotation rMinus = law.getAttitude(orbit, this.date.shiftedBy(-h), orbit.getFrame()).getRotation();
        final Rotation r0 = law.getAttitude(orbit, this.date, orbit.getFrame()).getRotation();
        final Rotation rPlus = law.getAttitude(orbit, this.date.shiftedBy(h), orbit.getFrame()).getRotation();

        // check spin is consistent with attitude evolution
        final double errorAngleMinus = Rotation.distance(sMinus.shiftedBy(h).getAttitude().getRotation(), r0);
        final double evolutionAngleMinus = Rotation.distance(rMinus, r0);
        Assert.assertEquals(0.0, errorAngleMinus, 1.0e-5 * evolutionAngleMinus);
        final double errorAnglePlus = Rotation.distance(r0, sPlus.shiftedBy(-h).getAttitude().getRotation());
        final double evolutionAnglePlus = Rotation.distance(r0, rPlus);
        Assert.assertEquals(0.0, errorAnglePlus, 1.0e-5 * evolutionAnglePlus);

        final Vector3D spin0 = law.getAttitude(orbit, this.date, orbit.getFrame()).getSpin();
        final Vector3D reference = AngularCoordinates.estimateRate(sMinus.getAttitude().getRotation(),
            sPlus.getAttitude().getRotation(),
            2 * h);
        Assert.assertTrue(spin0.getNorm() > 1.0e-3);
        Assert.assertEquals(0.0, spin0.subtract(reference).getNorm(), 2.0e-12);

        Report.printToReport("Spin at date", reference, spin0);
    }

    /**
     * Test the derivatives of the sliding target
     */
    @Test
    public void testSlidingDerivatives() throws PatriusException {

        final AbstractGroundPointing law = new YawSteering(new NadirPointing(this.earthShape),
            CelestialBodyFactory.getSun(),
            Vector3D.MINUS_I);
        law.setSpinDerivativesComputation(true);

        final List<TimeStampedPVCoordinates> sample = new ArrayList<>();
        for (double dt = -0.1; dt < 0.1; dt += 0.05) {
            final Orbit o = this.circOrbit.shiftedBy(dt);
            sample.add(law.getTargetPV(o, o.getDate(), o.getFrame()));
        }
        final TimeStampedPVCoordinates reference =
            TimeStampedPVCoordinates.interpolate(this.circOrbit.getDate(),
                CartesianDerivativesFilter.USE_P, sample);

        final TimeStampedPVCoordinates target =
            law.getTargetPV(this.circOrbit, this.circOrbit.getDate(), this.circOrbit.getFrame());

        Assert.assertEquals(0.0,
            Vector3D.distance(reference.getPosition(), target.getPosition()),
            1.0e-15 * reference.getPosition().getNorm());
        Assert.assertEquals(0.0,
            Vector3D.distance(reference.getVelocity(), target.getVelocity()),
            4.0e-11 * reference.getVelocity().getNorm());
        Assert.assertEquals(0.0,
            Vector3D.distance(reference.getAcceleration(), target.getAcceleration()),
            8.0e-5 * reference.getAcceleration().getNorm());

    }

    @Test
    public void testRotationAcceleration() throws PatriusException {

        Report.printMethodHeader("testRotationAcceleration", "Rotation acceleration computation", "Finite differences",
            2E-8, ComparisonType.ABSOLUTE);

        // Attitude laws
        // **************
        // Target pointing attitude provider without yaw compensation
        final NadirPointing nadirLaw = new NadirPointing(this.earthShape);

        // Target pointing attitude provider with yaw compensation
        final YawSteering yawCompensLaw =
            new YawSteering(nadirLaw, CelestialBodyFactory.getSun(), Vector3D.MINUS_I);
        yawCompensLaw.setSpinDerivativesComputation(true);

        // Check that derivation of spin with finite difference method is closed to acceleration
        for (int i = 1; i < this.circOrbit.getKeplerianPeriod(); i += 1) {
            final Vector3D accActual =
                yawCompensLaw.getAttitude(this.circOrbit, this.date.shiftedBy(i), this.circOrbit.getFrame())
                    .getRotationAcceleration();
            final Vector3D accDerivateSpin =
                this.getSpinFunction(yawCompensLaw, this.circOrbit, this.circOrbit.getFrame(),
                    this.date.shiftedBy(i)).nthDerivative(1).getVector3D(this.date.shiftedBy(i));
            Assert.assertEquals(accActual.distance(accDerivateSpin), 0.0, 2e-8);
            if (i == 0) {
                Report.printToReport("Rotation acceleration at date", accDerivateSpin, accActual);
            }
        }

        // Check rotation acceleration is null when spin derivative is deactivated
        yawCompensLaw.setSpinDerivativesComputation(false);
        Assert.assertNull(yawCompensLaw.getAttitude(this.circOrbit).getRotationAcceleration());
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
    public Vector3DFunction getSpinFunction(final AttitudeLaw law, final PVCoordinatesProvider pvProv,
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
     * @throws PatriusException
     *         get class parameters
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
    public void testGetters() throws PatriusException {

        final TargetGroundPointing groundPointingLaw = new TargetGroundPointing(
            new OneAxisEllipsoid(
                Constants.CNES_STELA_AE, Constants.GRIM5C1_EARTH_FLATTENING, FramesFactory.getGCRF(), "earth"),
            new Vector3D(15200, 3200, 2000));
        final Vector3D phasingAxis = new Vector3D(200, 50, 30);
        final YawSteering yawSteering = new YawSteering(groundPointingLaw, CelestialBodyFactory.getSun(), phasingAxis);
        Assert.assertEquals(groundPointingLaw.getTargetGeo().getLatitude(),
            ((TargetGroundPointing) yawSteering.getGroundPointingLaw()).getTargetGeo().getLatitude(), 0);
        Assert.assertEquals(phasingAxis.getX(), yawSteering.getPhasingAxis().getX(), 0);
        Assert.assertNotNull(yawSteering.toString());
    }

    @Before
    public void setUp() {
        try {
            Utils.setDataRoot("regular-data");
            FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

            // Computation date
            this.date = new AbsoluteDate(new DateComponents(1970, 04, 07),
                TimeComponents.H00,
                TimeScalesFactory.getUTC());

            // Body mu
            final double mu = 3.9860047e14;

            // Reference frame = ITRF 2005
            this.frameITRF2005 = FramesFactory.getITRF();

            // Satellite position
            this.circOrbit =
                new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, MathLib.toRadians(50.), MathLib.toRadians(270.),
                    MathLib.toRadians(5.300), PositionAngle.MEAN,
                    FramesFactory.getEME2000(), this.date, mu);

            // Elliptic earth shape */
            this.earthShape =
                new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, this.frameITRF2005);

        } catch (final PatriusException oe) {
            Assert.fail(oe.getMessage());
        }

    }

    @After
    public void tearDown() throws PatriusException {
        this.date = null;
        this.frameITRF2005 = null;
        this.circOrbit = null;
        this.earthShape = null;
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
    }

}
