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
 * VERSION:4.7:DM:DM-2799:18/05/2021:Suppression des pas de temps fixes codes en dur 
 * VERSION:4.3:DM:DM-2102:15/05/2019:[Patrius] Refactoring du paquet fr.cnes.sirius.patrius.bodies
 * VERSION:4.3:FA:FA-1978:15/05/2019:Anomalie calcul orientation corps celeste (UAI)
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:87:05/08/2013:deleted tests testing the obsolete methods of TargetPointing
 * VERSION::FA:306:12/11/2014:coverage
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:489:15/10/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:583:11/03/2016:simplification of attitude laws architecture
 * VERSION::DM:596:12/04/2016:Improve test coherence
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.ExtendedOneAxisEllipsoid;
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
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

// Test the TargetPointing and TargetGroundPointing classes.
public class TargetPointingTest {

    // Computation date
    private AbsoluteDate date;

    // Body mu
    private double mu;

    // Reference frame = ITRF
    private Frame frameITRF;

    // Transform from EME2000 to ITRF
    private Transform eme2000ToItrf;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(SpinStabilizedTest.class.getSimpleName(), "Target pointing attitude provider");
    }

    /**
     * Test if both constructors are equivalent
     */
    @Test
    public void testConstructors() throws PatriusException {

        // Satellite position
        // ********************
        final CircularOrbit circ =
            new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, MathLib.toRadians(50.), MathLib.toRadians(270.),
                MathLib.toRadians(5.300), PositionAngle.MEAN,
                FramesFactory.getEME2000(), this.date, this.mu);

        // Attitude laws
        // ***************
        // Elliptic earth shape
        final OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, this.frameITRF);

        // Target definition as a geodetic point AND as a position/velocity vector
        final GeodeticPoint geoTargetITRF2005C = new GeodeticPoint(MathLib.toRadians(43.36), MathLib.toRadians(1.26),
            600.);
        final Vector3D pTargetITRF2005C = earthShape.transform(geoTargetITRF2005C);

        // Attitude law definition from geodetic point target
        final TargetPointing geoTargetAttitudeLaw = new TargetPointing(geoTargetITRF2005C, earthShape);

        // Attitude law definition from position/velocity target
        final TargetPointing pvTargetAttitudeLaw = new TargetPointing(this.frameITRF, pTargetITRF2005C);

        // Check that both attitude are the same
        // Get satellite rotation for target pointing law
        final Rotation rotPv = pvTargetAttitudeLaw.getAttitude(circ, this.date, circ.getFrame()).getRotation();
        // Get satellite rotation for nadir pointing law
        final Rotation rotGeo = geoTargetAttitudeLaw.getAttitude(circ, this.date, circ.getFrame()).getRotation();

        // Rotations composition
        final Rotation rotCompo = rotGeo.applyInverseTo(rotPv);
        final double angle = rotCompo.getAngle();
        Assert.assertEquals(angle, 0.0, Utils.epsilonAngle);
    }

    /**
     * Test if both constructors for the target ground pointing are equivalent
     */
    @Test
    public void testTargetGroundPointing() throws PatriusException {

        // Satellite position
        // ********************
        final CircularOrbit circ =
            new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, MathLib.toRadians(50.), MathLib.toRadians(270.),
                MathLib.toRadians(5.300), PositionAngle.MEAN,
                FramesFactory.getEME2000(), this.date, this.mu);

        // Attitude laws
        // ***************
        // Elliptic earth shape
        final OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, this.frameITRF);

        // Target definition as a geodetic point AND as a position/velocity vector
        final GeodeticPoint geoTargetITRF2005C = new GeodeticPoint(MathLib.toRadians(43.36), MathLib.toRadians(1.26),
            600.);
        final Vector3D pTargetITRF2005C = earthShape.transform(geoTargetITRF2005C);

        // Attitude law definition from vector 3D target, using a target ground pointing law
        final TargetGroundPointing vectorTargetAttitudeLaw =
            new TargetGroundPointing(earthShape, pTargetITRF2005C, Vector3D.PLUS_J, Vector3D.PLUS_K);

        // Attitude law definition from geodetic point target, using a target ground pointing law
        final TargetGroundPointing geoTargetAttitudeLaw =
            new TargetGroundPointing(earthShape, geoTargetITRF2005C, Vector3D.PLUS_J, Vector3D.PLUS_K);

        // Check that both attitude are the same
        // Get satellite rotation for vector target:
        final Rotation rotPv = vectorTargetAttitudeLaw.getAttitude(circ, this.date, circ.getFrame()).getRotation();
        geoTargetAttitudeLaw.getAttitude(circ, this.date, circ.getFrame()).getRotation();

        // Rotations composition
        final Rotation rotCompo = rotPv.applyInverseTo(rotPv);
        final double angle = rotCompo.getAngle();
        Assert.assertEquals(angle, 0.0, Utils.epsilonAngle);
    }

    /**
     * Test with nadir target : Check that when the target is the same as nadir target at date,
     * satellite attitude is the same as nadir attitude at the same date, but different at a different date.
     */
    @Test
    public void testNadirTarget() throws PatriusException {

        Report.printMethodHeader("testNadirTarget", "Rotation computation", "Orekit v7", Utils.epsilonAngle,
            ComparisonType.ABSOLUTE);

        // Elliptic earth shape
        final OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, this.frameITRF);

        // Satellite on any position
        final CircularOrbit circOrbit =
            new CircularOrbit(7178000.0, 1.e-5, 0., MathLib.toRadians(50.), 0.,
                MathLib.toRadians(90.), PositionAngle.TRUE,
                FramesFactory.getEME2000(), this.date, this.mu);

        // Target attitude provider with target under satellite nadir
        // *******************************************************
        // Definition of nadir target
        // Create nadir pointing attitude provider
        final NadirPointing nadirAttitudeLaw = new NadirPointing(earthShape);

        // Check nadir target
        final Vector3D pNadirTarget = nadirAttitudeLaw.getTargetPoint(circOrbit, this.date, this.frameITRF);
        final GeodeticPoint geoNadirTarget = earthShape.transform(pNadirTarget, this.frameITRF, this.date);

        // Create target attitude provider
        final TargetPointing targetAttitudeLaw = new TargetPointing(geoNadirTarget, earthShape);

        // 1/ Test that attitudes are the same at date
        // *********************************************
        // i.e the composition of inverse earth pointing rotation
        // with nadir pointing rotation shall be identity.

        // Get satellite rotation from target pointing law at date
        final Rotation rotTarget =
            targetAttitudeLaw.getAttitude(circOrbit, this.date, circOrbit.getFrame()).getRotation();

        // Get satellite rotation from nadir pointing law at date
        final Rotation rotNadir =
            nadirAttitudeLaw.getAttitude(circOrbit, this.date, circOrbit.getFrame()).getRotation();

        // Compose attitude rotations
        final Rotation rotCompo = rotTarget.applyInverseTo(rotNadir);
        final double angle = rotCompo.getAngle();
        Assert.assertEquals(angle, 0.0, Utils.epsilonAngle);

        // 2/ Test that attitudes are different at a different date
        // **********************************************************

        // Extrapolation one minute later
        final KeplerianPropagator extrapolator = new KeplerianPropagator(circOrbit);
        final double delta_t = 60.0; // extrapolation duration in seconds
        final AbsoluteDate extrapDate = this.date.shiftedBy(delta_t);
        final Orbit extrapOrbit = extrapolator.propagate(extrapDate).getOrbit();

        // Get satellite rotation from target pointing law at date + 1min
        final Rotation extrapRotTarget = targetAttitudeLaw.getAttitude(extrapOrbit, extrapDate, extrapOrbit.getFrame())
            .getRotation();

        // Get satellite rotation from nadir pointing law at date
        final Rotation extrapRotNadir = nadirAttitudeLaw.getAttitude(extrapOrbit, extrapDate, extrapOrbit.getFrame())
            .getRotation();

        // Compose attitude rotations
        final Rotation extrapRotCompo = extrapRotTarget.applyInverseTo(extrapRotNadir);
        final double extrapAngle = extrapRotCompo.getAngle();
        Assert.assertEquals(extrapAngle, MathLib.toRadians(24.684793905118823), Utils.epsilonAngle);

        Report.printToReport("Angle at date", MathLib.toRadians(24.684793905118823), extrapAngle);
    }

    /**
     * Test if defined target belongs to the direction pointed by the satellite
     */
    @Test
    public void testTargetInPointingDirection() throws PatriusException {

        // Create computation date
        final AbsoluteDate date = new AbsoluteDate(new DateComponents(2008, 04, 07),
            TimeComponents.H00,
            TimeScalesFactory.getUTC());

        // Elliptic earth shape
        final OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, this.frameITRF);

        // Create target pointing attitude provider
        final GeodeticPoint geoTarget = new GeodeticPoint(MathLib.toRadians(43.36), MathLib.toRadians(1.26), 600.);
        final TargetPointing targetAttitudeLaw = new TargetPointing(geoTarget, earthShape);

        // Satellite position
        // ********************
        // Create satellite position as circular parameters
        final CircularOrbit circ =
            new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, MathLib.toRadians(50.), MathLib.toRadians(270.),
                MathLib.toRadians(5.300), PositionAngle.MEAN,
                FramesFactory.getEME2000(), date, this.mu);

        // Transform satellite position to position/velocity parameters in EME2000 frame
        final PVCoordinates pvSatEME2000 = circ.getPVCoordinates();

        // Pointing direction
        // ********************
        // Get satellite attitude rotation, i.e rotation from EME2000 frame to satellite frame
        final Rotation rotSatEME2000 = targetAttitudeLaw.getAttitude(circ, date, circ.getFrame()).getRotation();

        // Transform Z axis from satellite frame to EME2000
        final Vector3D zSatEME2000 = rotSatEME2000.applyTo(Vector3D.PLUS_K);

        // Line containing satellite point and following pointing direction
        final Vector3D p = this.eme2000ToItrf.transformPosition(pvSatEME2000.getPosition());
        final Line pointingLine = new Line(p,
            p.add(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                this.eme2000ToItrf.transformVector(zSatEME2000)));

        // Check that the line contains earth center
        final double distance = pointingLine.distance(earthShape.transform(geoTarget));

        Assert.assertEquals(0, distance, 1.e-7);
    }

    /**
     * Test the difference between pointing over two longitudes separated by 5°
     */
    @Test
    public void testSlewedTarget() throws PatriusException {

        // Spheric earth shape
        final OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 0., this.frameITRF);

        // Satellite position
        // ********************
        // Create satellite position as circular parameters
        final CircularOrbit circ =
            new CircularOrbit(42164000.0, 0.5e-8, -0.5e-8, 0., 0.,
                MathLib.toRadians(5.300), PositionAngle.MEAN,
                FramesFactory.getEME2000(), this.date, this.mu);

        // Create nadir pointing attitude provider
        // **********************************
        final NadirPointing nadirAttitudeLaw = new NadirPointing(earthShape);

        // Get observed ground point from nadir pointing law
        final Vector3D pNadirObservedEME2000 =
            nadirAttitudeLaw.getTargetPoint(circ, this.date, FramesFactory.getEME2000());
        final Vector3D pNadirObservedITRF2005 = this.eme2000ToItrf.transformPosition(pNadirObservedEME2000);

        final GeodeticPoint geoNadirObserved = earthShape.transform(pNadirObservedITRF2005, this.frameITRF, this.date);

        // Create target pointing attitude provider with target equal to nadir target
        // *********************************************************************
        final TargetPointing targetLawRef = new TargetPointing(this.frameITRF, pNadirObservedITRF2005);

        // Get attitude rotation in EME2000
        final Rotation rotSatRefEME2000 = targetLawRef.getAttitude(circ, this.date, circ.getFrame()).getRotation();

        // Create target pointing attitude provider with target 5° from nadir target
        // ********************************************************************
        final GeodeticPoint geoTarget = new GeodeticPoint(geoNadirObserved.getLatitude(),
            geoNadirObserved.getLongitude() - MathLib.toRadians(5), geoNadirObserved.getAltitude());
        final Vector3D pTargetITRF2005C = earthShape.transform(geoTarget);
        final TargetPointing targetLaw = new TargetPointing(this.frameITRF, pTargetITRF2005C);

        // Get attitude rotation
        final Rotation rotSatEME2000 = targetLaw.getAttitude(circ, this.date, circ.getFrame()).getRotation();

        // Compute difference between both attitude providers
        // *********************************************
        // Difference between attitudes
        // expected
        final double tanDeltaExpected = (6378136.460 / (42164000.0 - 6378136.460))
            * MathLib.tan(MathLib.toRadians(5));
        final double deltaExpected = MathLib.atan(tanDeltaExpected);

        // real
        final double deltaReal = rotSatEME2000.applyInverseTo(rotSatRefEME2000).getAngle();

        Assert.assertEquals(deltaReal, deltaExpected, 1.e-4);

    }

    @Test
    public void testSpin() throws PatriusException {

        Report
            .printMethodHeader("testSpin", "Spin computation", "Finite differences", 1.1e-10, ComparisonType.ABSOLUTE);

        // Elliptic earth shape
        final OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, this.frameITRF);

        // Create target pointing attitude provider
        final GeodeticPoint geoTarget = new GeodeticPoint(MathLib.toRadians(43.36), MathLib.toRadians(1.26), 600.);
        final AbstractAttitudeLaw law = new TargetPointing(geoTarget, earthShape);

        final KeplerianOrbit orbit =
            new KeplerianOrbit(7178000.0, 1.e-4, MathLib.toRadians(50.),
                MathLib.toRadians(10.), MathLib.toRadians(20.),
                MathLib.toRadians(30.), PositionAngle.MEAN,
                FramesFactory.getEME2000(), this.date, 3.986004415e14);

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
        final double errorAnglePlus = Rotation.distance(r0,
            sPlus.shiftedBy(-h).getAttitude().getRotation());
        final double evolutionAnglePlus = Rotation.distance(r0, rPlus);
        Assert.assertEquals(0.0, errorAnglePlus, 1.0e-5 * evolutionAnglePlus);

        final Vector3D spin0 = law.getAttitude(orbit, this.date, orbit.getFrame()).getSpin();
        final Vector3D reference = AngularCoordinates.estimateRate(sMinus.getAttitude().getRotation(),
            sPlus.getAttitude().getRotation(),
            2 * h);
        Assert.assertEquals(0.0, spin0.subtract(reference).getNorm(), 1.1e-10);

        Report.printToReport("Spin at date", reference, spin0);
    }

    @Test
    public void testRotationAcceleration() throws PatriusException {

        Report.printMethodHeader("testRotationAcceleration", "Rotation acceleration computation", "Finite differences",
            2E-10, ComparisonType.ABSOLUTE);

        // TargetPointing law
        // *******************

        // Elliptic earth shape
        final OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, this.frameITRF);

        // Satellite on any position
        final CircularOrbit circOrbit =
            new CircularOrbit(7178000.0, 1.e-5, 0., MathLib.toRadians(50.), 0.,
                MathLib.toRadians(90.), PositionAngle.TRUE,
                FramesFactory.getEME2000(), this.date, this.mu);

        // Definition of nadir target
        // Create nadir pointing attitude provider
        final NadirPointing nadirAttitudeLaw = new NadirPointing(earthShape);

        // Check nadir target
        final Vector3D pNadirTarget = nadirAttitudeLaw.getTargetPoint(circOrbit, this.date, this.frameITRF);
        final GeodeticPoint geoNadirTarget = earthShape.transform(pNadirTarget, this.frameITRF, this.date);

        // Create target attitude provider
        final TargetPointing targetAttitudeLaw = new TargetPointing(geoNadirTarget, earthShape);
        targetAttitudeLaw.setSpinDerivativesComputation(true);

        // Check that derivation of spin with finite difference method is closed to acceleration
        for (int i = 0; i < circOrbit.getKeplerianPeriod(); i += 1) {
            final Vector3D acc = targetAttitudeLaw.getAttitude(circOrbit, this.date.shiftedBy(i), circOrbit.getFrame())
                .getRotationAcceleration();
            final Vector3D accDerivateSpin = this.getSpinFunction(targetAttitudeLaw, circOrbit, circOrbit.getFrame(),
                this.date.shiftedBy(i)).nthDerivative(1).getVector3D(this.date.shiftedBy(i));
            Assert.assertEquals(acc.distance(accDerivateSpin), 0.0, 2e-10);
            if (i == 0) {
                Report.printToReport("Rotation acceleration at date", accDerivateSpin, acc);
            }
        }

        // TargetGroundPointing law
        // *************************

        // Target definition as a geodetic point AND as a position/velocity vector
        final GeodeticPoint geoTargetITRF2005C = new GeodeticPoint(MathLib.toRadians(43.36), MathLib.toRadians(1.26),
            600.);
        final Vector3D pTargetITRF2005C = earthShape.transform(geoTargetITRF2005C);

        // Attitude law definition from vector 3D target, using a target ground pointing law
        final TargetGroundPointing vectorTargetAttitudeLaw = new TargetGroundPointing(earthShape, pTargetITRF2005C);
        vectorTargetAttitudeLaw.setSpinDerivativesComputation(true);

        // Check that derivation of spin with finite difference method is closed to acceleration
        for (int i = 0; i < circOrbit.getKeplerianPeriod(); i += 1) {
            final Vector3D acc = vectorTargetAttitudeLaw
                .getAttitude(circOrbit, this.date.shiftedBy(i), circOrbit.getFrame()).getRotationAcceleration();
            final Vector3D accDerivateSpin =
                this.getSpinFunction(vectorTargetAttitudeLaw, circOrbit, circOrbit.getFrame(),
                    this.date.shiftedBy(i)).nthDerivative(1).getVector3D(this.date.shiftedBy(i));
            Assert.assertEquals(acc.distance(accDerivateSpin), 0.0, 3e-8);
        }

        // Check rotation acceleration is null when spin derivative is deactivated
        vectorTargetAttitudeLaw.setSpinDerivativesComputation(false);
        Assert.assertNull(vectorTargetAttitudeLaw.getAttitude(circOrbit).getRotationAcceleration());
    }

    @Test
    public void testException() throws PatriusException {

        final TargetPointing attitudeLaw = new TargetPointing(this.frameITRF, Vector3D.ZERO);
        final PVCoordinatesProvider pvProv = new PVCoordinatesProvider(){

            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
                // return the null vector for position and velocity
                return new PVCoordinates(Vector3D.ZERO, Vector3D.ZERO);
            }

        };
        boolean testOk = false;
        try {
            attitudeLaw.getAttitude(pvProv, this.date, this.frameITRF).getRotation();
            Assert.fail();
        } catch (final PatriusException e) {
            testOk = true;
        }
        Assert.assertTrue(testOk);
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
            this.mu = 3.9860047e14;

            // Reference frame = ITRF
            this.frameITRF = FramesFactory.getITRF();

            // Transform from EME2000 to ITRF
            this.eme2000ToItrf = FramesFactory.getEME2000().getTransformTo(this.frameITRF, this.date);

        } catch (final PatriusException oe) {
            Assert.fail(oe.getMessage());
        }

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
        final GeodeticPoint targetGeo = new GeodeticPoint(MathLib.toRadians(25), MathLib.toRadians(14), 150000);
        final TargetGroundPointing groundPointing =
            new TargetGroundPointing(
                new ExtendedOneAxisEllipsoid(
                    Constants.CNES_STELA_AE, Constants.GRIM5C1_EARTH_FLATTENING, FramesFactory.getGCRF(), "earth"),
                targetGeo);
        Assert.assertEquals(targetGeo.getLatitude(), groundPointing.getTargetGeo().getLatitude(), 0);
        Assert.assertNotNull(groundPointing.toString());
    }

    /**
     * Test impact of Delta-T on spin computation.
     */
    @Test
    public void testDeltaT() throws PatriusException {

        // Build law with default and user-defined delta-t
        final TargetGroundPointing law1 = new TargetGroundPointing(new OneAxisEllipsoid(3678000, 0.001, FramesFactory.getTIRF()), Vector3D.PLUS_J, Vector3D.PLUS_K, Vector3D.PLUS_I);
        final TargetGroundPointing law2 = new TargetGroundPointing(new OneAxisEllipsoid(3678000, 0.001, FramesFactory.getTIRF()), Vector3D.PLUS_J, Vector3D.PLUS_K, Vector3D.PLUS_I, 50);

        // Get rotation
        final Orbit orbit = new KeplerianOrbit(7000000, 0.001, 0.5, 0.6, 0.7, 0.8, PositionAngle.TRUE, FramesFactory.getGCRF(),
                date, Constants.CNES_STELA_MU);
        final Attitude attitude1 = law1.getAttitude(orbit, this.date, FramesFactory.getGCRF());
        final Attitude attitude2 = law2.getAttitude(orbit, this.date, FramesFactory.getGCRF());
        
        // Check delta-t has been taken into account
        Assert.assertTrue(Vector3D.distance(attitude1.getSpin(), attitude2.getSpin()) > 1E-15);
    }

    @After
    public void tearDown() throws PatriusException {
        this.date = null;
        this.frameITRF = null;
        this.eme2000ToItrf = null;
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
    }

}
