/**
 * 
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
 * 
 * @history Created 17/02/2016
 *
 * HISTORY
 * VERSION:4.13:DM:DM-5:08/12/2023:[PATRIUS] Orientation d'un corps celeste sous forme de quaternions
 * VERSION:4.13:DM:DM-3:08/12/2023:[PATRIUS] Distinction entre corps celestes et barycentres
 * VERSION:4.13:DM:DM-132:08/12/2023:[PATRIUS] Suppression de la possibilite
 * de convertir les sorties de VacuumSignalPropagation
 * VERSION:4.11:DM:DM-3311:22/05/2023:[PATRIUS] Evolutions mineures sur CelestialBody, shape et reperes
 * VERSION:4.11:FA:FA-3314:22/05/2023:[PATRIUS] Anomalie lors de l'evaluation d'un ForceModel lorsque le SpacecraftState est en ITRF
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3149:10/05/2022:[PATRIUS] Optimisation des reperes interplanetaires 
 * VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
 * VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3165:10/05/2022:[PATRIUS] Amelioration de la propagation du signal 
 * VERSION:4.9:DM:DM-3164:10/05/2022:[PATRIUS] Amelioration de la gestion du GM (parametre gravitationnel)
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:529:23/02/2016: relativistic effects
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.relativistic;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyEphemeris;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.CelestialBodyIAUOrientation;
import fr.cnes.sirius.patrius.bodies.CelestialBodyOrientation;
import fr.cnes.sirius.patrius.bodies.CelestialPoint;
import fr.cnes.sirius.patrius.bodies.EphemerisType;
import fr.cnes.sirius.patrius.bodies.IAUPoleModelType;
import fr.cnes.sirius.patrius.bodies.JPLCelestialBodyLoader;
import fr.cnes.sirius.patrius.forces.gravity.GravityModel;
import fr.cnes.sirius.patrius.frames.CelestialBodyFrame;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.TimeDerivativesEquations;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * Class test for {@link CoriolisRelativisticEffect} class.
 * 
 * @author rodriguest
 * 
 * @version $Id: CoriolisRelativisticEffectTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 3.2
 * 
 */
public class CoriolisRelativisticEffectTest {

    /** Sun. */
    private CelestialPoint sun;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(CoriolisRelativisticEffectTest.class.getSimpleName(), "Coriolis force");
    }

    /**
     * 
     * @testType UT
     * 
     * @throws PatriusException
     *         when an error occurs.
     * 
     * @testedMethod {@link CoriolisEffectEffect#computeAcceleration(fr.cnes.sirius.patrius.propagation.SpacecraftState)}
     * @testedMethod {@link CoriolisRelativisticEffect#addContribution(fr.cnes.sirius.patrius.propagation.SpacecraftState, fr.cnes.sirius.patrius.propagation.numerical.TimeDerivativesEquations)}
     * 
     * @description Test for the method computeAcceleration() and addContribution()
     * 
     * @input orbit
     * 
     * @output acceleration
     * 
     * @testPassCriteria acceleration is as expected (ZOOM reference, relative tolerance: 1E-15)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     * 
     */
    @Test
    public void testAcceleration() throws PatriusException {

        final double eps = 5E-15;

        Report.printMethodHeader("testAcceleration", "Acceleration computation", "ZOOM", eps, ComparisonType.RELATIVE);

        // Initialization
        final CoriolisRelativisticEffect force = new CoriolisRelativisticEffect(this.sun.getGM(), this.sun);

        final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(23967 * 86400. + 55320.);
        final PVCoordinates pv = new PVCoordinates(new Vector3D(-0.439584017658778E+06, -0.296038007930370E+05,
            -0.709617640153498E+07), new Vector3D(0.573899359984705E+04, 0.477793965015048E+04,
            -0.371114951980137E+03));
        final Orbit orbit = new CartesianOrbit(pv, FramesFactory.getGCRF(), date, 0.39860044150000E+15);
        final SpacecraftState state = new SpacecraftState(orbit);

        // Computation
        final double[] actual = force.computeAcceleration(state).toArray();
        final double[] actual2 = new double[3];
        final TimeDerivativesEquations adder = new TimeDerivativesEquations(){
            /** Serializable UID. */
            private static final long serialVersionUID = -3071190323039120569L;

            @Override
            public void initDerivatives(final double[] yDot, final Orbit currentOrbit) throws PropagationException {
            }

            @Override
            public void addXYZAcceleration(final double x, final double y, final double z) {
                actual2[0] = x;
                actual2[1] = y;
                actual2[2] = z;
            }

            @Override
            public void addAdditionalStateDerivative(final String name, final double[] pDot) {
            }

            @Override
            public void addAcceleration(final Vector3D gamma, final Frame frame) throws PatriusException {
            }
        };
        force.addContribution(state, adder);

        // Check
        final double[] expected = { -0.241903292895666E-10, 0.300686361935433E-10, 0.130363492450285E-10 };
        Assert.assertEquals(0., (actual[0] - expected[0]) / expected[0], eps);
        Assert.assertEquals(0., (actual[1] - expected[1]) / expected[1], eps);
        Assert.assertEquals(0., (actual[2] - expected[2]) / expected[2], eps);
        Report.printToReport("Acceleration", expected, actual);
    }

    /**
     * 
     * @testType UT
     * 
     * @throws PatriusException
     *         when an error occurs.
     * 
     * @testedMethod {@link CoriolisEffectEffect#computeAcceleration(fr.cnes.sirius.patrius.propagation.SpacecraftState)}
     * 
     * @description Test that the method computeAcceleration() throw an exception when the frame is
     *              not pseudo-inertial
     * 
     * @input orbit
     * 
     * @output PatriusException
     * 
     * @testPassCriteria acceleration is as expected (ZOOM reference, relative tolerance: 1E-15)
     * 
     * @referenceVersion 4.11
     * 
     */
    @Test
    public void testNotPseudoInertial() throws PatriusException {

        // Initialization
        final CoriolisRelativisticEffect force = new CoriolisRelativisticEffect(this.sun.getGM(), this.sun);

        final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(23967 * 86400. + 55320.);
        final PVCoordinates pv = new PVCoordinates(new Vector3D(-0.439584017658778E+06, -0.296038007930370E+05,
            -0.709617640153498E+07), new Vector3D(0.573899359984705E+04, 0.477793965015048E+04,
            -0.371114951980137E+03));
        final Orbit orbit = new CartesianOrbit(pv, FramesFactory.getITRF(), date, 0.39860044150000E+15);
        final SpacecraftState state = new SpacecraftState(orbit);

        // Computation
        try {
            force.computeAcceleration(state);
            Assert.fail();
        } catch (final PatriusException pe) {
            Assert.assertEquals(pe.getMessage(), PatriusMessages.NOT_INERTIAL_FRAME.getSourceString());
        }
    }

    /**
     * Test the partial derivatives computation.
     * 
     * @throws PatriusException
     *         when an error occurs.
     * 
     * @testedMethod {@link CoriolisRelativisticEffect#addDAccDState(fr.cnes.sirius.patrius.propagation.SpacecraftState, double[][], double[][])}
     * 
     * @description Test the computation of acceleration partial derivatives wrt position and
     *              velocity
     * 
     * @input orbit
     * 
     * @output partial derivatives
     * 
     * @testPassCriteria partial derivatives are as expected (ZOOM reference, relative tolerance:
     *                   1E-15)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     * 
     */
    @Test
    public void testPartialDerivatives() throws PatriusException {

        final double eps = 4E-15;
        Report.printMethodHeader("testPartialDerivatives", "Partial derivatives computation", "ZOOM", eps,
            ComparisonType.RELATIVE);

        // Initialization
        final CoriolisRelativisticEffect force = new CoriolisRelativisticEffect(this.sun.getGM(), this.sun);

        final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(23967 * 86400. + 55320.);
        final PVCoordinates pv = new PVCoordinates(new Vector3D(-0.439584017658778E+06, -0.296038007930370E+05,
            -0.709617640153498E+07), new Vector3D(0.573899359984705E+04, 0.477793965015048E+04,
            -0.371114951980137E+03));
        final Orbit orbit = new CartesianOrbit(pv, FramesFactory.getGCRF(), date, 0.39860044150000E+15);
        final SpacecraftState state = new SpacecraftState(orbit);

        // Computation
        final double[][] dAccdPos = new double[3][3];
        final double[][] dAccdVel = new double[3][3];
        force.addDAccDState(state, dAccdPos, dAccdVel);

        // Check
        final double[][] expectedPos = new double[3][3];
        final double[][] expectedVel = { { 0.000000000000000E+00, -0.523935698313806E-14, -0.227153925478780E-14 },
            { 0.523935698313806E-14, 0.0000000000000000E+00, 0.0000000000000000E+00 },
            { 0.227153925478780E-14, 0.0000000000000000E+00, 0.0000000000000000E+00 }, };

        for (int i = 0; i < expectedPos.length; i++) {
            for (int j = 0; j < expectedPos[0].length; j++) {
                Assert.assertEquals(0., (dAccdPos[i][j] - expectedPos[i][j]), eps);
                if (expectedVel[i][j] != 0) {
                    Assert.assertEquals(0., (dAccdVel[i][j] - expectedVel[i][j]) / expectedVel[i][j], eps);
                } else {
                    Assert.assertEquals(0., (dAccdVel[i][j] - expectedVel[i][j]), eps);
                }
            }
        }

        Report.printToReport("Partial derivatives / pos", expectedPos, dAccdPos);
        Report.printToReport("Partial derivatives / vel", expectedVel, dAccdVel);

        // Check dAccdParam (no supported parameter)
        try {
            force.addDAccDParam(null, null, null);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link CoriolisRelativisticEffect#CoriolisEffect(org.orekit.utils.PVCoordinatesProvider, CelestialPoint, boolean)}
     * @testedMethod {@link CoriolisRelativisticEffect#CoriolisEffect(org.orekit.utils.PVCoordinatesProvider, CelestialPoint, fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D, boolean)}
     * 
     * @description compute acceleration partial derivatives wrt position
     * 
     * @input instances of {@link CoriolisRelativisticEffect}
     * 
     * @output partial derivatives
     * 
     * @testPassCriteria partial derivatives must be all null, since computation is deactivated at
     *                   construction :
     *                   instantiation is done with null tabs of normalized coefficients used for
     *                   partial derivatives
     *                   computation
     * 
     * @throws PatriusException
     *         when an Orekit error occurs
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testNullPD() throws PatriusException {

        // SpacecraftState
        final KeplerianOrbit orbit = new KeplerianOrbit(7E7, 0.001, 0.93, 0, 0, 0, PositionAngle.TRUE,
            FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, 0.39860044150000E+15);
        final SpacecraftState state = new SpacecraftState(orbit);

        // Instances
        final CoriolisRelativisticEffect cor1 = new CoriolisRelativisticEffect(this.sun.getGM(), this.sun, false);
        Assert.assertEquals(cor1.getEventsDetectors().length, 0);

        // Check partial derivatives computation is deactivated
        // Partial derivatives wrt position are always null in this force model
        Assert.assertFalse(cor1.computeGradientPosition());
        Assert.assertFalse(cor1.computeGradientVelocity());

        // Partial derivatives
        final double[][] dAccdPos = new double[3][3];
        final double[][] dAccdVel = new double[3][3];

        cor1.addDAccDState(state, dAccdPos, dAccdVel);

        // Check all derivatives are null
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Assert.assertEquals(0, dAccdPos[i][j], 0);
                Assert.assertEquals(0, dAccdVel[i][j], 0);
            }
        }
    }

    /**
     * Set up
     * 
     * @throws PatriusException
     */
    @Before
    public void setUp() throws PatriusException {

        Utils.setDataRoot("regular-dataCNES-2003");

        final JPLCelestialBodyLoader loaderSun = new JPLCelestialBodyLoader("unxp2000.405", EphemerisType.SUN);
        final CelestialBody sunJPL = (CelestialBody) loaderSun.loadCelestialPoint(CelestialBodyFactory.SUN);

        // Specific Sun
        this.sun = new CelestialBody(){

            /** Serializable UID. */
            private static final long serialVersionUID = -812434433805055185L;

            // default sun shape
            private BodyShape shape = sunJPL.getShape();

            // default sun gravitational attraction model
            private GravityModel model = sunJPL.getGravityModel();

            @Override
            public CelestialBodyEphemeris getEphemeris() {
                return null;
            }

            @Override
            public void setEphemeris(final CelestialBodyEphemeris ephemerisIn) {
            }

            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
                // u vector hard encoded in ZOOM. From that vector can be deduced Sun position and
                // velocity
                // Only velocity and p ^ v is important hence position is chosen arbitrarily only
                // norm must match
                final Vector3D u = new Vector3D(0, -0.3977771559141, 0.9174820620769);
                final double r = 0.151518880297225E12;
                final Vector3D pos = new Vector3D(1, 0, 0).scalarMultiply(r);
                final double velocity = MathLib.sqrt(this.getGM() / r);
                final Vector3D vel = Vector3D.crossProduct(u, pos.normalize()).scalarMultiply(velocity);
                return new PVCoordinates(pos, vel);
            }

            @Override
            public String getName() {
                return sunJPL.getName();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public CelestialBodyFrame getInertialFrame(final IAUPoleModelType iauPole) throws PatriusException {
                final CelestialBodyFrame frame;
                switch (iauPole) {
                    case CONSTANT:
                        // Get an inertially oriented, body centered frame taking into account
                        // only constant part of IAU pole data with respect to ICRF frame. The frame
                        // is always bound to the body center, and its axes have a fixed
                        // orientation with respect to other inertial frames.
                        frame = sunJPL.getInertialFrame(IAUPoleModelType.CONSTANT);
                        break;
                    case MEAN:
                        // Get an inertially oriented, body centered frame taking into account only
                        // constant and secular part of IAU pole data with respect to ICRF frame.
                        frame = sunJPL.getInertialFrame(IAUPoleModelType.MEAN);
                        break;
                    case TRUE:
                        // Get an inertially oriented, body centered frame taking into account
                        // constant, secular and harmonics part of IAU pole data with respect to
                        // ICRF frame.
                        frame = sunJPL.getInertialFrame(IAUPoleModelType.TRUE);
                        break;
                    default:
                        // The iauPole given as input is not implemented in this method.
                        throw new PatriusException(PatriusMessages.INVALID_IAUPOLEMODELTYPE);
                }
                return frame;
            }

            @Override
            public double getGM() {
                return 0.132712437742476E+21;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public CelestialBodyFrame getRotatingFrame(final IAUPoleModelType iauPole) throws PatriusException {
                final CelestialBodyFrame frame;
                switch (iauPole) {
                    case CONSTANT:
                        // Get a body oriented, body centered frame taking into account only constant part
                        // of IAU pole data with respect to inertially-oriented frame. The frame is always
                        // bound to the body center, and its axes have a fixed orientation with respect to
                        // the celestial body.
                        frame = sunJPL.getRotatingFrame(IAUPoleModelType.CONSTANT);
                        break;
                    case MEAN:
                        // Get a body oriented, body centered frame taking into account constant and secular
                        // part of IAU pole data with respect to mean equator frame. The frame is always
                        // bound to the body center, and its axes have a fixed orientation with respect to
                        // the celestial body.
                        frame = sunJPL.getRotatingFrame(IAUPoleModelType.MEAN);
                        break;
                    case TRUE:
                        // Get a body oriented, body centered frame taking into account constant, secular
                        // and harmonics part of IAU pole data with respect to true equator frame. The frame
                        // is always bound to the body center, and its axes have a fixed orientation with
                        // respect to the celestial body.
                        frame = sunJPL.getRotatingFrame(IAUPoleModelType.TRUE);
                        break;
                    default:
                        // The iauPole given as input is not implemented in this method.
                        throw new PatriusException(PatriusMessages.INVALID_IAUPOLEMODELTYPE);
                }
                return frame;
            }

            @Override
            public CelestialBodyFrame getICRF() throws PatriusException {
                return sunJPL.getICRF();
            }

            @Override
            public CelestialBodyFrame getEME2000() throws PatriusException {
                return sunJPL.getEME2000();
            }

            @Override
            public BodyShape getShape() {
                return this.shape;
            }

            @Override
            public void setShape(final BodyShape shapeIn) {
                this.shape = shapeIn;
            }

            @Override
            public GravityModel getGravityModel() {
                return this.model;
            }

            @Override
            public void setGravityModel(final GravityModel gravityModelIn) {
                this.model = gravityModelIn;
            }

            /** {@inheritDoc} */
            @Override
            public Frame getNativeFrame(final AbsoluteDate date) throws PatriusException {
                return null;
            }

            @Override
            public CelestialBodyIAUOrientation getOrientation() {
                return null;
            }

            @Override
            public void setOrientation(final CelestialBodyOrientation celestialBodyOrientation) {
            }

            @Override
            public void setGM(final double gmIn) {
            }

            @Override
            public CelestialBodyFrame getEclipticJ2000() throws PatriusException {
                // nothing to do
                return null;
            }
        };
    }
}
