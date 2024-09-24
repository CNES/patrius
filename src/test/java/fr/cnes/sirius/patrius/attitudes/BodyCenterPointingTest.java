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
 * VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3165:10/05/2022:[PATRIUS] Amelioration de la propagation du signal 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2799:18/05/2021:Suppression des pas de temps fixes codes en dur 
 * VERSION:4.6:DM:DM-2603:27/01/2021:[PATRIUS] Ajout de getters pour les 2 LOS de la classe AbstractGroundPointing 
 * VERSION:4.3:FA:FA-1978:15/05/2019:Anomalie calcul orientation corps celeste (UAI)
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:87:05/08/2013:deleted tests testing the obsolete methods of BodyCenterPointing
 * VERSION::FA:306:12/11/2014:coverage
 * VERSION::DM:344:15/04/2015:Construction of an attitude law from a Local Orbital Frame
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:489:15/10/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:583:11/03/2016:simplification of attitude laws architecture
 * VERSION::DM:596:12/04/2016:Improve test coherence
 * VERSION::FA:648:27/07/2016:Corrected minor points staying from V3.2
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
import fr.cnes.sirius.patrius.propagation.BoundedPropagator;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScale;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

public class BodyCenterPointingTest {

    // Computation date
    private AbsoluteDate date;

    // Orbit
    private CircularOrbit circ;

    // Reference frame = ITRF 2005
    private Frame itrf;

    // Transform from EME2000 to ITRF2005
    private Transform eme2000ToItrf;

    // Earth center pointing attitude provider
    private BodyCenterPointing earthCenterAttitudeLaw;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(BodyCenterPointingTest.class.getSimpleName(), "Body center pointing attitude provider");
    }

    /**
     * Test if body center belongs to the direction pointed by the satellite
     */
    @Test
    public void testBodyCenterInPointingDirection() throws PatriusException {

        Report.printMethodHeader("testBodyCenterInPointingDirection", "Direction computation", "Math", 1.e-8,
            ComparisonType.ABSOLUTE);

        // Transform satellite position to position/velocity parameters in EME2000 frame
        final PVCoordinates pvSatEME2000 = this.circ.getPVCoordinates();

        // Pointing direction
        // ********************
        // Get satellite attitude rotation, i.e rotation from EME2000 frame to satellite frame
        final Rotation rotSatEME2000 =
            this.earthCenterAttitudeLaw.getAttitude(this.circ, this.date, this.circ.getFrame()).getRotation();

        // Transform Z axis from satellite frame to EME2000
        final Vector3D zSatEME2000 = rotSatEME2000.applyTo(Vector3D.PLUS_K);

        // Transform Z axis from EME2000 to ITRF2005
        final Vector3D zSatITRF2005C = this.eme2000ToItrf.transformVector(zSatEME2000);

        // Transform satellite position/velocity from EME2000 to ITRF2005
        final PVCoordinates pvSatITRF2005C = this.eme2000ToItrf.transformPVCoordinates(pvSatEME2000);

        // Line containing satellite point and following pointing direction
        final Line pointingLine = new Line(pvSatITRF2005C.getPosition(),
            pvSatITRF2005C.getPosition().add(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                zSatITRF2005C));

        // Check that the line contains earth center (distance from line to point less than 1.e-8 m)
        final double distance = pointingLine.distance(Vector3D.ZERO);

        Assert.assertTrue(distance < 1.e-8);

        Report.printToReport("Direction at date", 0, distance);
    }

    /**
     * Test if body center belongs to the direction pointed by the satellite
     * Test the use of constructor without an inertial frame as parameter.
     * {@link fr.cnes.sirius.patrius.attitudes.BodyCenterPointing#BodyCenterPointing()
     * BodyCenterPointing()}.
     */
    @Test
    public void testBodyCenterInPointingDirectionGCRF() throws PatriusException {

        // Transform satellite position to position/velocity parameters in EME2000 frame
        final PVCoordinates pvSatEME2000 = this.circ.getPVCoordinates();

        // Pointing direction
        // ********************

        final BodyCenterPointing centerPointingAttLaw = new BodyCenterPointing();
        // Get satellite attitude rotation, i.e rotation from EME2000 frame to satellite frame
        final Rotation rotSatEME2000 =
            centerPointingAttLaw.getAttitude(this.circ, this.date, this.circ.getFrame()).getRotation();

        // Transform Z axis from satellite frame to EME2000
        final Vector3D zSatEME2000 = rotSatEME2000.applyTo(Vector3D.PLUS_K);

        // Transform Z axis from EME2000 to ITRF2005
        final Vector3D zSatITRF2005C = this.eme2000ToItrf.transformVector(zSatEME2000);

        // Transform satellite position/velocity from EME2000 to ITRF2005
        final PVCoordinates pvSatITRF2005C = this.eme2000ToItrf.transformPVCoordinates(pvSatEME2000);

        // Line containing satellite point and following pointing direction
        final Line pointingLine = new Line(pvSatITRF2005C.getPosition(),
            pvSatITRF2005C.getPosition().add(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                zSatITRF2005C));

        // Check that the line contains earth center (distance from line to point less than 1.e-8 m)
        final double distance = pointingLine.distance(Vector3D.ZERO);

        Assert.assertTrue(distance < 1.e-8);
    }

    @Test
    public void testSpin() throws PatriusException {

        Report
            .printMethodHeader("testSpin", "Spin computation", "Finite differences", 1.0e-13, ComparisonType.ABSOLUTE);

        final Propagator propagator = new KeplerianPropagator(this.circ, this.earthCenterAttitudeLaw);

        final double h = 0.01;
        final SpacecraftState sMinus = propagator.propagate(this.date.shiftedBy(-h));
        final SpacecraftState sPlus = propagator.propagate(this.date.shiftedBy(h));

        final Rotation rotationMinus =
            this.earthCenterAttitudeLaw.getAttitude(this.circ, this.date.shiftedBy(-h), this.circ.getFrame())
                .getRotation();
        final Rotation rotation0 =
            this.earthCenterAttitudeLaw.getAttitude(this.circ, this.date, this.circ.getFrame()).getRotation();
        final Rotation rotationPlus =
            this.earthCenterAttitudeLaw.getAttitude(this.circ, this.date.shiftedBy(h), this.circ.getFrame())
                .getRotation();

        // check spin is consistent with attitude evolution
        final double errorAngleMinus = Rotation.distance(sMinus.shiftedBy(h).getAttitude().getRotation(),
            rotation0);
        final double evolutionAngleMinus = Rotation.distance(rotationMinus,
            rotation0);
        Assert.assertEquals(0.0, errorAngleMinus, 1.0e-6 * evolutionAngleMinus);
        final double errorAnglePlus = Rotation.distance(rotation0,
            sPlus.shiftedBy(-h).getAttitude().getRotation());
        final double evolutionAnglePlus = Rotation.distance(rotation0,
            rotationPlus);
        Assert.assertEquals(0.0, errorAnglePlus, 1.0e-6 * evolutionAnglePlus);

        final Vector3D spin0 =
            this.earthCenterAttitudeLaw.getAttitude(this.circ, this.date, this.circ.getFrame()).getSpin();
        final Vector3D reference = AngularCoordinates.estimateRate(rotationMinus,
            rotationPlus,
            2 * h);
        Assert.assertTrue(spin0.getNorm() > 1.0e-3);
        Assert.assertEquals(0.0, spin0.subtract(reference).getNorm(), 1.0e-13);

        Report.printToReport("Spin at date", reference, spin0);
    }

    @Test
    public void testCoverage() {
        final BodyCenterPointing attitudeLaw = new BodyCenterPointing(this.itrf);
        final PVCoordinatesProvider pvProv = new PVCoordinatesProvider(){
            /** Serializable UID. */
            private static final long serialVersionUID = -6160408969537441420L;

            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
                // return the null vector for position and velocity
                return new PVCoordinates(Vector3D.ZERO, Vector3D.ZERO);
            }

            /** {@inheritDoc} */
            @Override
            public Frame getNativeFrame(final AbsoluteDate date,
                                        final Frame frame) throws PatriusException {
                throw new PatriusException(PatriusMessages.INTERNAL_ERROR);
            }
        };
        try {
            attitudeLaw.getAttitude(pvProv, this.date, this.itrf).getRotation();
            Assert.fail();
        } catch (final PatriusException e) {
            // expected
            Assert.assertTrue(true);
        }
        Assert.assertNotNull(attitudeLaw.toString());
    }

    /**
     * Test impact of Delta-T on spin computation.
     */
    @Test
    public void testDeltaT() throws PatriusException {

        // Build law with default and user-defined delta-t
        final BodyCenterGroundPointing law1 = new BodyCenterGroundPointing(new OneAxisEllipsoid(3678000, 0.001,
            FramesFactory.getGCRF()), Vector3D.PLUS_I, Vector3D.PLUS_J);
        final BodyCenterGroundPointing law2 = new BodyCenterGroundPointing(new OneAxisEllipsoid(3678000, 0.001,
            FramesFactory.getGCRF()), Vector3D.PLUS_I, Vector3D.PLUS_J, 0.5);

        // Get rotation
        final Orbit orbit = new KeplerianOrbit(7000000, 0.001, 0.5, 0.6, 0.7, 0.8, PositionAngle.TRUE,
            FramesFactory.getGCRF(), this.date, Constants.CNES_STELA_MU);
        final Attitude attitude1 = law1.getAttitude(orbit, this.date, FramesFactory.getGCRF());
        final Attitude attitude2 = law2.getAttitude(orbit, this.date, FramesFactory.getGCRF());

        // Check delta-t has been taken into account
        Assert.assertTrue(Vector3D.distance(attitude1.getSpin(), attitude2.getSpin()) > 1E-15);
    }

    @Test
    public void testRotationAcceleration() throws PatriusException {

        Report.printMethodHeader("testRotationAcceleration", "Rotation acceleration computation", "Finite differences",
            9e-7, ComparisonType.ABSOLUTE);

        // BodyCenterPointing law
        // Check that derivation of spin with finite difference method is closed to acceleration
        for (int i = 0; i < this.circ.getKeplerianPeriod(); i += 1) {
            final Vector3D acc =
                this.earthCenterAttitudeLaw.getAttitude(this.circ, this.date.shiftedBy(i), this.circ.getFrame())
                    .getRotationAcceleration();
            final Vector3D accDerivateSpin =
                this.getSpinFunction(this.earthCenterAttitudeLaw, this.circ, this.circ.getFrame(),
                    this.date.shiftedBy(i)).nthDerivative(1).getVector3D(this.date.shiftedBy(i));
            Assert.assertEquals(acc.distance(accDerivateSpin), 0.0, 1e-14);
        }

        // BodyCenterGroundPointing law

        // Elliptic earth shape
        final OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 1 / 298.257222101,
            this.itrf);

        // Create earth center pointing attitude provider */
        final BodyCenterGroundPointing bodyCenterGroundPointingLaw =
            new BodyCenterGroundPointing(earthShape, Vector3D.PLUS_J, Vector3D.PLUS_K);
        Assert.assertNotNull(bodyCenterGroundPointingLaw.getTargetPosition(this.circ, this.date, this.itrf));

        // Check that derivation of spin with finite difference method is closed to acceleration
        bodyCenterGroundPointingLaw.setSpinDerivativesComputation(true);
        for (int i = 0; i < this.circ.getKeplerianPeriod(); i += 1) {
            final Vector3D acc =
                bodyCenterGroundPointingLaw.getAttitude(this.circ, this.date.shiftedBy(i), this.circ.getFrame())
                    .getRotationAcceleration();
            final Vector3D accDerivateSpin =
                this.getSpinFunction(bodyCenterGroundPointingLaw, this.circ, this.circ.getFrame(),
                    this.date.shiftedBy(i)).nthDerivative(1).getVector3D(this.date.shiftedBy(i));
            Assert.assertEquals(acc.distance(accDerivateSpin), 0.0, 9e-7);
            if (i == 0) {
                Report.printToReport("Rotation acceleration at date", accDerivateSpin, acc);
            }
        }

        // Check rotation acceleration is null when spin derivative is deactivated
        bodyCenterGroundPointingLaw.setSpinDerivativesComputation(false);
        Assert.assertNull(bodyCenterGroundPointingLaw.getAttitude(this.circ).getRotationAcceleration());
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedMethod {@link GroundPointing# getAttitude(PVCoordinatesProvider, AbsoluteDate, Frame)}
     *               {@link GroundPointing# getTargetPV(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description This test verifies that no propagation exception is thrown in the particular
     *              case where, in a propagation with a ground pointing attitude law, the attitude is accessed in
     *              the propagation process. Indeed, this kind of law performs a finite differences scheme in the
     *              attitude computation, and a particular attention must be taken when attitude is wanted at a date
     *              being at the lower/upper bound of the ephemeris.
     * 
     * @input a propagator, an orbit, an attitude law
     * 
     * @output none
     * @testPassCriteria No propagation exception must be raised when getting the attitude at any date
     * @referenceVersion 3.3
     * 
     * @nonRegressionVersion 3.3
     */
    @Test
    public void testOutsideEphemeris() throws PatriusException {

        // paramètres orbitaux
        final double a = 42164173.550572;
        final double e = .41;
        final double i = MathLib.toRadians(63.388);
        final double pa = MathLib.toRadians(270);
        final double raan = MathLib.toRadians(188);
        final double w = 0;

        final TimeScale tai = TimeScalesFactory.getTAI();

        // constants
        final double ae = Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS;
        final double f = Constants.GRIM5C1_EARTH_FLATTENING;
        final double mu = Constants.GRIM5C1_EARTH_MU;

        // Earth
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(ae, f, FramesFactory.getITRF());

        // Attitude law : earth center ground pointing
        final BodyCenterGroundPointing law = new BodyCenterGroundPointing(earth);

        // Initial date
        final AbsoluteDate initialDate = new AbsoluteDate(2010, 1, 1, 6, 0, 0, tai);

        // Reference frame
        final Frame EME2000 = FramesFactory.getEME2000();

        // Initial orbit
        final KeplerianOrbit initialOrbit = new KeplerianOrbit(a, e, i, pa,
            raan, w, PositionAngle.TRUE, EME2000, initialDate, mu);

        // Final date
        final AbsoluteDate finalDate = initialDate.shiftedBy(2 * 86400);

        /*
         * Propagation
         */
        // Propagator
        final KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit, law);
        propagator.setEphemerisMode();

        // propagate
        propagator.propagate(finalDate);

        // Get generated ephemeris
        final BoundedPropagator ephemeris = propagator.getGeneratedEphemeris();

        AbsoluteDate currentDate = initialDate;
        // Loop on generated ephemeris : propagate on segments
        // from first ephemeris date to the final date
        while (currentDate.offsetFrom(finalDate, tai) <= 0.) {
            try {
                ephemeris.propagate(currentDate).getAttitude();
            } catch (final PropagationException exception) {
                // Should never happen !
                Assert.fail();
            }
            currentDate = currentDate.shiftedBy(60.);
        }

        // Complementary check: check AbstractGroundPointing getters
        Assert.assertEquals(0., law.getLosInSatFrame().getPosition().distance(Vector3D.PLUS_K), 0.);
        Assert.assertEquals(0., law.getLosInSatFrame().getVelocity().distance(Vector3D.ZERO), 0.);
        Assert.assertEquals(0., law.getLosNormalInSatFrame().getPosition().distance(Vector3D.PLUS_J), 0.);
        Assert.assertEquals(0., law.getLosNormalInSatFrame().getVelocity().distance(Vector3D.ZERO), 0.);
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

            // Satellite position as circular parameters
            final double mu = 3.9860047e14;
            final double raan = 270.;
            this.circ =
                new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, MathLib.toRadians(50.), MathLib.toRadians(raan),
                    MathLib.toRadians(5.300 - raan), PositionAngle.MEAN,
                    FramesFactory.getEME2000(), this.date, mu);

            // Reference frame = ITRF 2005
            this.itrf = FramesFactory.getITRF();

            // Transform from EME2000 to ITRF2005
            this.eme2000ToItrf = FramesFactory.getEME2000().getTransformTo(this.itrf, this.date);
            // eme2000ToItrf = Transform.IDENTITY;

            // Create earth center pointing attitude provider */
            this.earthCenterAttitudeLaw = new BodyCenterPointing(this.itrf);
            this.earthCenterAttitudeLaw.setSpinDerivativesComputation(true);

        } catch (final PatriusException oe) {
            Assert.fail(oe.getMessage());
        }

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

    @After
    public void tearDown() throws PatriusException {
        this.date = null;
        this.itrf = null;
        this.eme2000ToItrf = null;
        this.earthCenterAttitudeLaw = null;
        this.circ = null;
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
    }

}
