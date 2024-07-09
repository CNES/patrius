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
 * VERSION:4.5:FA:FA-2353:27/05/2020:Correction de bug dans YawCompensation 
* VERSION:4.3:FA:FA-1978:15/05/2019:Anomalie calcul orientation corps celeste (UAI)
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:185:11/04/2014:the getCompensation method has been modified
 * VERSION::FA:306:12/11/2014:coverage
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:489:15/10/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:583:11/03/2016:simplification of attitude laws architecture
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::DM:603:29/08/2016:deleted deprecated methods and classes in package attitudes
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
import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.AbstractVector3DFunction;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3DFunction;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
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

public class YawCompensationTest {

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
        Report.printClassHeader(KeplerianPropagatorTest.class.getSimpleName(), "Yaw compensation attitude provider");
    }

    /**
     * Test that pointed target remains the same with or without yaw compensation
     */
    @Test
    public void testTarget() throws PatriusException {

        // Attitude laws
        // **************
        // Target pointing attitude provider without yaw compensation
        final NadirPointing nadirLaw = new NadirPointing(this.earthShape);

        // Target pointing attitude provider with yaw compensation
        final YawCompensation yawCompensLaw = new YawCompensation(nadirLaw);

        // Check the underlying ground pointing law
        Assert.assertEquals(yawCompensLaw.getUnderlyingAttitudeLaw().getClass(), NadirPointing.class);
        // Check target
        // *************
        // without yaw compensation
        final Vector3D noYawObserved = nadirLaw.getTargetPoint(this.circOrbit, this.date, this.frameITRF2005);

        // with yaw compensation
        final Vector3D yawObserved = yawCompensLaw.getTargetPoint(this.circOrbit, this.date, this.frameITRF2005);

        // Check difference
        final Vector3D observedDiff = noYawObserved.subtract(yawObserved);

        Assert.assertEquals(0.0, observedDiff.getNorm(), Utils.epsilonTest);

    }

    /**
     * Test the derivatives of the sliding target
     */
    @Test
    public void testSlidingDerivatives() throws PatriusException {

        final AbstractGroundPointing law = new YawCompensation(new NadirPointing(this.earthShape));
        law.setSpinDerivativesComputation(true);

        final List<TimeStampedPVCoordinates> sample = new ArrayList<TimeStampedPVCoordinates>();
        for (double dt = -0.1; dt < 0.1; dt += 0.01) {
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
            3.0e-10 * reference.getVelocity().getNorm());
        Assert.assertEquals(0.0,
            Vector3D.distance(reference.getAcceleration(), target.getAcceleration()),
            7.0e-5 * reference.getAcceleration().getNorm());

    }

    /**
     * Test that pointed target motion is along -X sat axis
     */
    @Test
    public void testAlignment() throws PatriusException {

        Report.printMethodHeader("testAlignment", "Rotation computation", "Orekit v7", 2E-5, ComparisonType.ABSOLUTE);

        final Frame inertFrame = this.circOrbit.getFrame();
        final Frame earthFrame = this.earthShape.getBodyFrame();
        final AttitudeProvider law = new YawCompensation(new NadirPointing(this.earthShape));
        final Attitude att0 = law.getAttitude(this.circOrbit, this.date, this.circOrbit.getFrame());

        // ground point in satellite Z direction
        final Vector3D satInert = this.circOrbit.getPVCoordinates().getPosition();
        final Vector3D zInert = att0.getRotation().applyTo(Vector3D.PLUS_K);
        final GeodeticPoint gp = this.earthShape.getIntersectionPoint(new Line(satInert,
            satInert.add(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, zInert)),
            satInert,
            inertFrame, this.circOrbit.getDate());
        final Vector3D pEarth = this.earthShape.transform(gp);

        // velocity of ground point, in inertial frame
        final double h = 1.0;
        final double s2 = 1.0 / (12 * h);
        final double s1 = 8.0 * s2;
        final Transform tM2h = earthFrame.getTransformTo(inertFrame, this.circOrbit.getDate().shiftedBy(-2 * h));
        final Vector3D pM2h = tM2h.transformPosition(pEarth);
        final Transform tM1h = earthFrame.getTransformTo(inertFrame, this.circOrbit.getDate().shiftedBy(-h));
        final Vector3D pM1h = tM1h.transformPosition(pEarth);
        final Transform tP1h = earthFrame.getTransformTo(inertFrame, this.circOrbit.getDate().shiftedBy(h));
        final Vector3D pP1h = tP1h.transformPosition(pEarth);
        final Transform tP2h = earthFrame.getTransformTo(inertFrame, this.circOrbit.getDate().shiftedBy(2 * h));
        final Vector3D pP2h = tP2h.transformPosition(pEarth);
        final Vector3D velInert = new Vector3D(s1, pP1h, -s1, pM1h, -s2, pP2h, s2, pM2h);
        final double r = pEarth.getNorm() / satInert.getNorm();
        final Vector3D relativeVelocity =
            velInert.subtract(this.circOrbit.getPVCoordinates().getVelocity().scalarMultiply(r));

        // relative velocity in satellite frame, must be in (X, Z) plane
        final Vector3D relVelSat = att0.getRotation().applyInverseTo(relativeVelocity);
        Assert.assertEquals(0.0, relVelSat.getY(), 2.0e-5);

        Report.printToReport("Relative velocity at date", 0, relVelSat.getY());
    }

    /**
     * Test that maximum yaw compensation is at ascending/descending node,
     * and minimum yaw compensation is at maximum latitude.
     */
    @Test
    public void testCompensMinMax() throws PatriusException {

        // Attitude laws
        // **************
        // Target pointing attitude provider over satellite nadir at date, without yaw compensation
        final NadirPointing nadirLaw = new NadirPointing(this.earthShape);

        // Target pointing attitude provider with yaw compensation
        final YawCompensation yawCompensLaw = new YawCompensation(nadirLaw);

        // Extrapolation over one orbital period (sec)
        final double duration = this.circOrbit.getKeplerianPeriod();
        final KeplerianPropagator extrapolator = new KeplerianPropagator(this.circOrbit);

        // Extrapolation initializations
        final double delta_t = 15.0; // extrapolation duration in seconds
        AbsoluteDate extrapDate = this.date; // extrapolation start date

        // Min initialization
        double yawMin = 1.e+12;
        double latMin = 0.;

        while (extrapDate.durationFrom(this.date) < duration) {
            extrapDate = extrapDate.shiftedBy(delta_t);

            // Extrapolated orbit state at date
            final Orbit extrapOrbit = extrapolator.propagate(extrapDate).getOrbit();
            final PVCoordinates extrapPvSatEME2000 = extrapOrbit.getPVCoordinates();

            // Satellite latitude at date
            final double extrapLat =
                this.earthShape.transform(extrapPvSatEME2000.getPosition(), FramesFactory.getEME2000(), extrapDate)
                    .getLatitude();

            // Compute yaw compensation angle -- rotations composition
            final double yawAngle = yawCompensLaw.getYawAngle(extrapOrbit, extrapDate, extrapOrbit.getFrame());

            // Update minimum yaw compensation angle
            if (MathLib.abs(yawAngle) <= yawMin) {
                yawMin = MathLib.abs(yawAngle);
                latMin = extrapLat;
            }

            // Checks
            // ------------------
            // 1/ Check yaw values around ascending node (max yaw)
            if ((MathLib.abs(extrapLat) < MathLib.toRadians(20.)) &&
                (extrapPvSatEME2000.getVelocity().getZ() >= 0.)) {
                Assert.assertTrue((MathLib.abs(yawAngle) >= MathLib.toRadians(2.9)) &&
                    (MathLib.abs(yawAngle) <= MathLib.toRadians(3.5)));
            }

            // 2/ Check yaw values around maximum positive latitude (min yaw)
            if (extrapLat > MathLib.toRadians(50.)) {
                Assert.assertTrue(MathLib.abs(yawAngle) <= MathLib.toRadians(0.26));
            }

            // 3/ Check yaw values around descending node (max yaw)
            if ((MathLib.abs(extrapLat) < MathLib.toRadians(2.))
                && (extrapPvSatEME2000.getVelocity().getZ() <= 0.)) {
                Assert.assertTrue((MathLib.abs(yawAngle) >= MathLib.toRadians(2.9)) &&
                    (MathLib.abs(yawAngle) <= MathLib.toRadians(3.5)));
            }

            // 4/ Check yaw values around maximum negative latitude (min yaw)
            if (extrapLat < MathLib.toRadians(-50.)) {
                Assert.assertTrue(MathLib.abs(yawAngle) <= MathLib.toRadians(0.26));
            }

        }

        // 5/ Check that minimum yaw compensation value is around maximum latitude
        Assert.assertEquals(0.0, MathLib.toDegrees(yawMin), 0.004);
        Assert.assertEquals(50.0, MathLib.toDegrees(latMin), 0.22);

    }

    /**
     * Test that compensation rotation axis is Zsat, yaw axis
     */
    @Test
    public void testCompensAxis() throws PatriusException {

        // Attitude laws
        // **************
        // Target pointing attitude provider over satellite nadir at date, without yaw compensation
        final NadirPointing nadirLaw = new NadirPointing(this.earthShape);

        // Target pointing attitude provider with yaw compensation
        final YawCompensation yawCompensLaw = new YawCompensation(nadirLaw, Vector3D.PLUS_J, Vector3D.PLUS_K);

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

        Report.printMethodHeader("testSpin", "Spin computation", "Finite differences", 9.5E-5, ComparisonType.ABSOLUTE);

        final NadirPointing nadirLaw = new NadirPointing(this.earthShape);

        // Target pointing attitude provider with yaw compensation
        final AttitudeProvider law = new YawCompensation(nadirLaw);

        final KeplerianOrbit orbit =
            new KeplerianOrbit(7178000.0, 1.e-4, MathLib.toRadians(50.),
                MathLib.toRadians(10.), MathLib.toRadians(20.),
                MathLib.toRadians(30.), PositionAngle.MEAN,
                FramesFactory.getEME2000(),
                this.date.shiftedBy(-300.0), 3.986004415e14);

        final Propagator propagator = new KeplerianPropagator(orbit, law);

        final double h = 0.01;
        final SpacecraftState sMinus = propagator.propagate(this.date.shiftedBy(-h));
        final SpacecraftState s0 = propagator.propagate(this.date);
        final SpacecraftState sPlus = propagator.propagate(this.date.shiftedBy(h));

        // check spin is consistent with attitude evolution
        final Vector3D spin0 = s0.getAttitude().getSpin();
        final Vector3D reference = AngularCoordinates.estimateRate(sMinus.getAttitude().getRotation(),
            sPlus.getAttitude().getRotation(),
            2 * h);
        Assert.assertTrue(spin0.getNorm() > 1.0e-3);
        Assert.assertEquals(0.0, spin0.subtract(reference).getNorm(), 9.5e-5);

        Report.printToReport("Spin at date", reference, spin0);
    }

    @Test
    public void testRotationAcceleration() throws PatriusException {

        Report.printMethodHeader("testRotationAcceleration", "Rotation acceleration computation", "Finite differences",
            9E-7, ComparisonType.ABSOLUTE);

        // Attitude laws
        // **************
        // Target pointing attitude provider without yaw compensation
        final NadirPointing nadirLaw = new NadirPointing(this.earthShape);

        // Target pointing attitude provider with yaw compensation
        final YawCompensation yawCompensLaw = new YawCompensation(nadirLaw);
        yawCompensLaw.setSpinDerivativesComputation(true);

        final CircularOrbit circOrbit = new CircularOrbit(7178000.0, 0., 0, 0, 0, 0, PositionAngle.MEAN,
            FramesFactory.getEME2000(), this.date, Constants.EGM96_EARTH_MU);

        // Check that derivation of spin with finite difference method is closed to acceleration
        for (int i = 1; i < circOrbit.getKeplerianPeriod(); i += 10) {
            final Vector3D accActual =
                yawCompensLaw.getAttitude(circOrbit, this.date.shiftedBy(i), circOrbit.getFrame())
                    .getRotationAcceleration();
            final Vector3D accDerivateSpin = this.getSpinFunction(yawCompensLaw, circOrbit, circOrbit.getFrame(),
                this.date.shiftedBy(i)).nthDerivative(1).getVector3D(this.date.shiftedBy(i));
            Assert.assertEquals(accActual.distance(accDerivateSpin), 0.0, 9.5E-7);
            if (i == 11) {
                Report.printToReport("Rotation acceleration at date", accDerivateSpin, accActual);
            }
        }

        // Check rotation acceleration is null when spin derivative is deactivated
        yawCompensLaw.setSpinDerivativesComputation(false);
        Assert.assertNull(yawCompensLaw.getAttitude(circOrbit).getRotationAcceleration());
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
     * Check that YawCompensation law is frame independent
     */
    @Test
    public void testFrameIndependency() throws PatriusException {
		
		BodyShape earth = new OneAxisEllipsoid(Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS, 
				Constants.GRIM5C1_EARTH_FLATTENING, FramesFactory.getTIRF());
		final AbstractGroundPointing groundPointingLaw = new NadirPointing(earth); 
		final AttitudeProvider yawComp = new YawCompensation(groundPointingLaw, Vector3D.PLUS_K, Vector3D.PLUS_J); 
		 
		PVCoordinatesProvider pvProv = this.circOrbit;
		AbsoluteDate start = this.date;
		final Attitude attIcrf = yawComp.getAttitude(pvProv, start, FramesFactory.getICRF()); 
		final Attitude attEme2000 = yawComp.getAttitude(pvProv, start, FramesFactory.getEME2000()); 
		final Attitude attIcrfInEme2000 = attIcrf.withReferenceFrame(FramesFactory.getEME2000()); 
		final double error = Rotation.distance(attEme2000.getRotation(), attIcrfInEme2000.getRotation()); 
        Assert.assertEquals(0., error, 0.);
        
    }

    @Before
    public void setUp() {
        try {
            Utils.setDataRoot("regular-data");
            FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

            // Computation date
            this.date = new AbsoluteDate(new DateComponents(2008, 04, 07),
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
