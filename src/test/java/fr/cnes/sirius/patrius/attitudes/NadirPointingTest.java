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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2799:18/05/2021:Suppression des pas de temps fixes codes en dur 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION:4.3:DM:DM-2104:15/05/2019:[Patrius] Rendre generiques les classes GroundPointing et NadirPointing
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:489:06/10/2015:Refactoring of rotation acceleration by the AttitudeProvider
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
import fr.cnes.sirius.patrius.bodies.EllipsoidPoint;
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
import fr.cnes.sirius.patrius.propagation.SimpleMassModel;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.CartesianDerivativesFilter;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class NadirPointingTest {

    // Computation date
    private AbsoluteDate date;

    // Body mu
    private double mu;

    // Reference frame = ITRF 2005C
    private Frame frameITRF2005;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(NadirPointingTest.class.getSimpleName(),
            "Nadir pointing attitude provider");
    }

    /**
     * Test impact of Delta-T on spin computation.
     */
    @Test
    public void testDeltaT() throws PatriusException {

        // Build law with default and user-defined delta-t
        final NadirPointing law1 = new NadirPointing(new OneAxisEllipsoid(3678000, 0.001,
            FramesFactory.getGCRF()), Vector3D.PLUS_I, Vector3D.PLUS_J);
        final NadirPointing law2 = new NadirPointing(new OneAxisEllipsoid(3678000, 0.001,
            FramesFactory.getGCRF()), Vector3D.PLUS_I, Vector3D.PLUS_J, 0.5);

        // Get rotation
        final Orbit orbit = new KeplerianOrbit(7000000, 0.001, 0.5, 0.6, 0.7, 0.8, PositionAngle.TRUE,
            FramesFactory.getGCRF(), this.date, Constants.CNES_STELA_MU);
        final Attitude attitude1 = law1.getAttitude(orbit, this.date, FramesFactory.getGCRF());
        final Attitude attitude2 = law2.getAttitude(orbit, this.date, FramesFactory.getGCRF());

        // Check delta-t has been taken into account
        Assert.assertTrue(Vector3D.distance(attitude1.getSpin(), attitude2.getSpin()) > 1E-15);
    }

    /**
     * Test in the case of a spheric earth : nadir pointing shall be the same as earth center
     * pointing
     */
    @Test
    public void testSphericEarth() throws PatriusException {

        // Spheric earth shape
        final OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 0., this.frameITRF2005);

        // Create nadir pointing attitude provider
        final NadirPointing nadirAttitudeLaw = new NadirPointing(earthShape);

        // Create earth center pointing attitude provider
        final BodyCenterPointing earthCenterAttitudeLaw = new BodyCenterPointing(this.frameITRF2005);

        // Create satellite position as circular parameters
        final CircularOrbit circ = new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4,
            MathLib.toRadians(50.), MathLib.toRadians(270.), MathLib.toRadians(5.300),
            PositionAngle.MEAN, FramesFactory.getEME2000(), this.date, this.mu);

        // Get nadir attitude
        final Rotation rotNadir = nadirAttitudeLaw.getAttitude(circ, this.date, circ.getFrame())
            .getRotation();
        nadirAttitudeLaw.setSpinDerivativesComputation(true);

        // Get earth center attitude
        final Rotation rotCenter = earthCenterAttitudeLaw.getAttitude(circ, this.date, circ.getFrame())
            .getRotation();

        // For a spheric earth, earth center pointing attitude and nadir pointing attitude
        // shall be the same, i.e the composition of inverse earth pointing rotation
        // with nadir pointing rotation shall be identity.
        final Rotation rotCompo = rotCenter.applyInverseTo(rotNadir);
        final double angle = rotCompo.getAngle();
        Assert.assertEquals(angle, 0.0, Utils.epsilonAngle);

        Assert.assertNotNull(nadirAttitudeLaw.toString());
    }

    /**
     * Test in the case of an elliptic earth : nadir pointing shall be : - the same as earth center
     * pointing in case of equatorial or polar position - different from earth center pointing in
     * any other case
     */
    @Test
    public void testNonSphericEarth() throws PatriusException {

        Report.printMethodHeader("testNonSphericEarth", "Rotation computation",
            "Orekit v7 reference", Utils.epsilonAngle, ComparisonType.ABSOLUTE);

        // Elliptic earth shape
        final OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 1 / 298.257222101,
            this.frameITRF2005);

        // Create nadir pointing attitude provider
        final NadirPointing nadirAttitudeLaw = new NadirPointing(earthShape);

        // Create earth center pointing attitude provider
        final BodyCenterPointing earthCenterAttitudeLaw = new BodyCenterPointing(this.frameITRF2005);

        // Satellite on equatorial position
        // **********************************
        final KeplerianOrbit kep = new KeplerianOrbit(7178000.0, 1.e-8, MathLib.toRadians(50.), 0.,
            0., 0., PositionAngle.TRUE, FramesFactory.getEME2000(), this.date, this.mu);

        // Get nadir attitude
        Rotation rotNadir = nadirAttitudeLaw.getAttitude(kep, this.date, kep.getFrame()).getRotation();

        // Get earth center attitude
        Rotation rotCenter = earthCenterAttitudeLaw.getAttitude(kep, this.date, kep.getFrame())
            .getRotation();

        // For a satellite at equatorial position, earth center pointing attitude and nadir pointing
        // attitude shall be the same, i.e the composition of inverse earth pointing rotation
        // with nadir pointing rotation shall be identity.
        Rotation rotCompo = rotCenter.applyInverseTo(rotNadir);
        double angle = rotCompo.getAngle();
        Assert.assertEquals(0.0, angle, 5.e-6);

        // Satellite on polar position
        // *****************************
        CircularOrbit circ = new CircularOrbit(7178000.0, 1.e-5, 0., MathLib.toRadians(90.), 0.,
            MathLib.toRadians(90.), PositionAngle.TRUE, FramesFactory.getEME2000(), this.date, this.mu);

        // Get nadir attitude
        rotNadir = nadirAttitudeLaw.getAttitude(circ, this.date, circ.getFrame()).getRotation();

        // Get earth center attitude
        rotCenter = earthCenterAttitudeLaw.getAttitude(circ, this.date, circ.getFrame()).getRotation();

        // For a satellite at polar position, earth center pointing attitude and nadir pointing
        // attitude shall be the same, i.e the composition of inverse earth pointing rotation
        // with nadir pointing rotation shall be identity.
        rotCompo = rotCenter.applyInverseTo(rotNadir);
        angle = rotCompo.getAngle();
        Assert.assertEquals(angle, 0.0, 5.e-6);

        // Satellite on any position
        // ***************************
        circ = new CircularOrbit(7178000.0, 1.e-5, 0., MathLib.toRadians(50.), 0.,
            MathLib.toRadians(90.), PositionAngle.TRUE, FramesFactory.getEME2000(), this.date, this.mu);

        // Get nadir attitude
        rotNadir = nadirAttitudeLaw.getAttitude(circ, this.date, circ.getFrame()).getRotation();

        // Get earth center attitude
        rotCenter = earthCenterAttitudeLaw.getAttitude(circ, this.date, circ.getFrame()).getRotation();

        // For a satellite at any position, earth center pointing attitude and nadir pointing
        // and nadir pointing attitude shall not be the same, i.e the composition of inverse earth
        // pointing rotation with nadir pointing rotation shall be different from identity.
        rotCompo = rotCenter.applyInverseTo(rotNadir);
        angle = rotCompo.getAngle();
        Assert.assertEquals(angle, MathLib.toRadians(0.16797386586252272), Utils.epsilonAngle);

        Report.printToReport("Angle at date", MathLib.toRadians(0.16797386586252272), angle);
    }

    /**
     * Vertical test : check that Z satellite axis is colinear to local vertical axis, which
     * direction is : (cos(lon)*cos(lat), sin(lon)*cos(lat), sin(lat)), where lon et lat stand for
     * observed point coordinates (i.e satellite ones, since they are the same by construction, but
     * that's what is to test.
     */
    @Test
    public void testVertical() throws PatriusException {

        // Elliptic earth shape
        final OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 1 / 298.257222101,
            this.frameITRF2005);

        // Create earth center pointing attitude provider
        final NadirPointing nadirAttitudeLaw = new NadirPointing(earthShape);

        // Satellite on any position
        final CircularOrbit circ = new CircularOrbit(7178000.0, 1.e-5, 0., MathLib.toRadians(50.),
            0., MathLib.toRadians(90.), PositionAngle.TRUE, FramesFactory.getEME2000(), this.date,
            this.mu);

        // Vertical test
        // ***************
        // Get observed ground point position/velocity
        final Vector3D pTargetItrf = nadirAttitudeLaw.getTargetPosition(circ, this.date, this.frameITRF2005);

        // Convert to geodetic coordinates
        final EllipsoidPoint target = earthShape.buildPoint(pTargetItrf, this.frameITRF2005, this.date, "");

        // Compute local vertical axis
        final double xVert = MathLib.cos(target.getLLHCoordinates().getLongitude())
                * MathLib.cos(target.getLLHCoordinates().getLatitude());
        final double yVert = MathLib.sin(target.getLLHCoordinates().getLongitude())
                * MathLib.cos(target.getLLHCoordinates().getLatitude());
        final double zVert = MathLib.sin(target.getLLHCoordinates().getLatitude());
        final Vector3D targetVertical = new Vector3D(xVert, yVert, zVert);

        // Get attitude rotation state
        final Rotation rotSatEME2000 = nadirAttitudeLaw.getAttitude(circ, this.date, circ.getFrame())
            .getRotation();

        // Get satellite Z axis in EME2000 frame
        final Vector3D zSatEME2000 = rotSatEME2000.applyTo(Vector3D.PLUS_K);
        final Vector3D zSatItrf = FramesFactory.getEME2000().getTransformTo(this.frameITRF2005, this.date)
            .transformVector(zSatEME2000);

        // Check that satellite Z axis is colinear to local vertical axis
        final double angle = Vector3D.angle(zSatItrf, targetVertical);
        Assert.assertEquals(0.0, MathLib.sin(angle), Utils.epsilonTest);
    }

    @Test
    public void testSpin() throws PatriusException {

        Report.printMethodHeader("testSpin", "Spin computation", "Finite differences", 2.0e-6,
            ComparisonType.ABSOLUTE);

        // Elliptic earth shape
        final OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 1 / 298.257222101,
            this.frameITRF2005);

        // Create earth center pointing attitude provider
        final NadirPointing law = new NadirPointing(earthShape);

        // Satellite on any position
        final KeplerianOrbit orbit = new KeplerianOrbit(7178000.0, 1.e-4, MathLib.toRadians(50.),
            MathLib.toRadians(10.), MathLib.toRadians(20.), MathLib.toRadians(30.),
            PositionAngle.MEAN, FramesFactory.getEME2000(), this.date, this.mu);

        final SimpleMassModel massModel = new SimpleMassModel(2500.0, "DEFAULT_MASS");
        final Propagator propagator = new KeplerianPropagator(orbit, law, this.mu, massModel);

        final double h = 0.1;
        final SpacecraftState sMinus = propagator.propagate(this.date.shiftedBy(-h));
        final SpacecraftState s0 = propagator.propagate(this.date);
        final SpacecraftState sPlus = propagator.propagate(this.date.shiftedBy(h));

        // check spin is consistent with attitude evolution
        final double errorAngleMinus = Rotation.distance(sMinus.shiftedBy(h).getAttitude()
            .getRotation(), s0.getAttitude().getRotation());
        final double evolutionAngleMinus = Rotation.distance(sMinus.getAttitude().getRotation(), s0
            .getAttitude().getRotation());
        Assert.assertEquals(0.0, errorAngleMinus, 1.0e-6 * evolutionAngleMinus);
        final double errorAnglePlus = Rotation.distance(s0.getAttitude().getRotation(), sPlus
            .shiftedBy(-h).getAttitude().getRotation());
        final double evolutionAnglePlus = Rotation.distance(s0.getAttitude().getRotation(), sPlus
            .getAttitude().getRotation());
        Assert.assertEquals(0.0, errorAnglePlus, 1.0e-6 * evolutionAnglePlus);

        final Vector3D spin0 = s0.getAttitude().getSpin();
        final Rotation rM = sMinus.getAttitude().getRotation();
        final Rotation rP = sPlus.getAttitude().getRotation();
        final Vector3D reference = AngularCoordinates.estimateRate(rM, rP, 2 * h);
        Assert.assertTrue(Rotation.distance(rM, rP) > 2.0e-4);
        Assert.assertEquals(0.0, spin0.subtract(reference).getNorm(), 2.0e-6);

        Report.printToReport("Spin at date", reference, spin0);
    }

    /**
     * Test the derivatives of the sliding target
     */
    @Test
    public void testSlidingDerivatives() throws PatriusException {

        // Elliptic earth shape
        final OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 1 / 298.257222101,
            this.frameITRF2005);

        // Create earth center pointing attitude provider
        final NadirPointing nadirAttitudeLaw = new NadirPointing(earthShape);

        // Satellite on any position
        final CircularOrbit circ = new CircularOrbit(7178000.0, 1.e-5, 0., MathLib.toRadians(50.),
            0., MathLib.toRadians(90.), PositionAngle.TRUE, FramesFactory.getEME2000(), this.date,
            this.mu);

        final List<TimeStampedPVCoordinates> sample = new ArrayList<>();
        for (double dt = -0.1; dt < 0.1; dt += 0.05) {
            final Orbit o = circ.shiftedBy(dt);
            sample.add(nadirAttitudeLaw.getTargetPV(o, o.getDate(), o.getFrame()));
        }
        final TimeStampedPVCoordinates reference = TimeStampedPVCoordinates.interpolate(
            circ.getDate(), CartesianDerivativesFilter.USE_P, sample);

        final TimeStampedPVCoordinates target = nadirAttitudeLaw.getTargetPV(circ, circ.getDate(),
            circ.getFrame());

        Assert.assertEquals(0.0, Vector3D.distance(reference.getPosition(), target.getPosition()),
            1.0e-15 * reference.getPosition().getNorm());
        Assert.assertEquals(0.0, Vector3D.distance(reference.getVelocity(), target.getVelocity()),
            3.0e-9 * reference.getVelocity().getNorm());
        Assert.assertEquals(0.0,
            Vector3D.distance(reference.getAcceleration(), target.getAcceleration()),
            1.0e-4 * reference.getAcceleration().getNorm());
    }

    @Test
    public void testRotationAcceleration() throws PatriusException {

        Report.printMethodHeader("testRotationAcceleration", "Rotation acceleration computation",
            "Finite differences", 2E-9, ComparisonType.ABSOLUTE);

        // Elliptic earth shape
        final OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 1 / 298.257222101,
            this.frameITRF2005);

        // Create earth center pointing attitude provider
        final NadirPointing nadirAttitudeLaw = new NadirPointing(earthShape);

        // Satellite on any position
        final CircularOrbit circ = new CircularOrbit(7178000.0, 1.e-5, 0., MathLib.toRadians(50.),
            0., MathLib.toRadians(90.), PositionAngle.TRUE, FramesFactory.getEME2000(), this.date,
            this.mu);
        // check that actual accelerations and accelerations obtained with finite difference are
        // close
        nadirAttitudeLaw.setSpinDerivativesComputation(true);
        for (int i = 0; i < circ.getKeplerianPeriod(); i += 1) {
            final Vector3D acc = nadirAttitudeLaw.getAttitude(circ, this.date.shiftedBy(i),
                circ.getFrame()).getRotationAcceleration();
            final Vector3D accDerivateSpin = this.getSpinFunction(nadirAttitudeLaw, circ,
                circ.getFrame(), this.date.shiftedBy(i)).nthDerivative(1).getVector3D(
                this.date.shiftedBy(i));
            Assert.assertEquals(acc.distance(accDerivateSpin), 0.0, 2e-9);
            if (i == 0) {
                Report.printToReport("Rotation acceleration at date", accDerivateSpin, acc);
            }
        }

        // Check rotation acceleration is null when spin derivative is deactivated
        nadirAttitudeLaw.setSpinDerivativesComputation(false);
        Assert.assertNull(nadirAttitudeLaw.getAttitude(circ).getRotationAcceleration());
    }

    /**
     * Local function to provide spin function.
     * 
     * @param pvProv local position-velocity provider around current date
     * @param frame reference frame from which spin function of date is computed
     * @param zeroAbscissa the date for which x=0 for spin function of date
     * @param law law
     * @return spin function of date relative
     */
    public Vector3DFunction getSpinFunction(final AttitudeLaw law,
                                            final PVCoordinatesProvider pvProv, final Frame frame,
                                            final AbsoluteDate zeroAbscissa) {
        return new AbstractVector3DFunction(zeroAbscissa){
            @Override
            public Vector3D getVector3D(final AbsoluteDate dateIn) throws PatriusException {
                return law.getAttitude(pvProv, dateIn, frame).getSpin();
            }
        };
    }

    /**
     * Test the axis specification behavior in the {@link GroundPointing} constructor.
     */
    @Test
    public void testFT2104() throws IllegalArgumentException, PatriusException {

        // Spheric earth shape
        final OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 0., this.frameITRF2005);

        // Satellite on any position
        final CircularOrbit circ = new CircularOrbit(7178000.0, 1.e-5, 0., MathLib.toRadians(50.),
            0., MathLib.toRadians(90.), PositionAngle.TRUE, FramesFactory.getEME2000(), this.date,
            this.mu);

        // Create nadir pointing attitude provider using the default constructor
        final NadirPointing nadirDefault = new NadirPointing(earthShape);

        // Create nadir pointing attitude provider using the simple constructor with default values
        final NadirPointing nadirAxisSetDefault = new NadirPointing(earthShape, Vector3D.PLUS_K,
            Vector3D.PLUS_J);

        // Create nadir pointing attitude provider using the simple constructor with random values
        final NadirPointing nadirAxisSet = new NadirPointing(earthShape, new Vector3D(0.02, 1,
            -0.04), new Vector3D(0.05, -0.03, 1));

        final Vector3D attDefault = nadirDefault.getAttitude(circ, this.date, this.frameITRF2005)
            .getRotation()
            .getAxis();
        final Vector3D attAxisSetDefault = nadirAxisSetDefault
            .getAttitude(circ, this.date, this.frameITRF2005)
            .getRotation().getAxis();
        final Vector3D attAxisSet = nadirAxisSet.getAttitude(circ, this.date, this.frameITRF2005)
            .getRotation()
            .getAxis();

        Assert.assertTrue(attDefault.equals(attAxisSetDefault));
        Assert.assertFalse(attDefault.equals(attAxisSet));
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

            // Reference frame = ITRF 2005
            this.frameITRF2005 = FramesFactory.getITRF();
        } catch (final PatriusException oe) {
            Assert.fail(oe.getMessage());
        }
    }

    @After
    public void tearDown() throws PatriusException {
        this.date = null;
        this.frameITRF2005 = null;
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
    }
}
