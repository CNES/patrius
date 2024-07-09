/**
 * 
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
 * 
 * @history Created 17/02/2016
 *
 * HISTORY
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
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.JPLEphemeridesLoader;
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
    private CelestialBody sun;

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
            -0.709617640153498E+07),
            new Vector3D(0.573899359984705E+04, 0.477793965015048E+04, -0.371114951980137E+03));
        final Orbit orbit = new CartesianOrbit(pv, FramesFactory.getGCRF(), date, 0.39860044150000E+15);
        final SpacecraftState state = new SpacecraftState(orbit);

        // Computation
        final double[] actual = force.computeAcceleration(state).toArray();
        final double[] actual2 = new double[3];
        final TimeDerivativesEquations adder = new TimeDerivativesEquations(){
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
            public void addKeplerContribution(final double mu) {
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
     * Test the partial derivatives computation.
     * 
     * @throws PatriusException
     *         when an error occurs.
     * 
     * @testedMethod {@link CoriolisRelativisticEffect#addDAccDState(fr.cnes.sirius.patrius.propagation.SpacecraftState, double[][], double[][])}
     * 
     * @description Test the computation of acceleration partial derivatives wrt position and velocity
     * 
     * @input orbit
     * 
     * @output partial derivatives
     * 
     * @testPassCriteria partial derivatives are as expected (ZOOM reference, relative tolerance: 1E-15)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     * 
     */
    @Test
    public final void testPartialDerivatives() throws PatriusException {

        final double eps = 4E-15;
        Report.printMethodHeader("testPartialDerivatives", "Partial derivatives computation", "ZOOM", eps,
            ComparisonType.RELATIVE);

        // Initialization
        final CoriolisRelativisticEffect force = new CoriolisRelativisticEffect(this.sun.getGM(), this.sun);

        final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(23967 * 86400. + 55320.);
        final PVCoordinates pv = new PVCoordinates(new Vector3D(-0.439584017658778E+06, -0.296038007930370E+05,
            -0.709617640153498E+07),
            new Vector3D(0.573899359984705E+04, 0.477793965015048E+04, -0.371114951980137E+03));
        final Orbit orbit = new CartesianOrbit(pv, FramesFactory.getGCRF(), date, 0.39860044150000E+15);
        final SpacecraftState state = new SpacecraftState(orbit);

        // Computation
        final double[][] dAccdPos = new double[3][3];
        final double[][] dAccdVel = new double[3][3];
        force.addDAccDState(state, dAccdPos, dAccdVel);

        // Check
        final double[][] expectedPos = new double[3][3];
        final double[][] expectedVel = {
            { 0.000000000000000E+00, -0.523935698313806E-14, -0.227153925478780E-14 },
            { 0.523935698313806E-14, 0.0000000000000000E+00, 0.0000000000000000E+00 },
            { 0.227153925478780E-14, 0.0000000000000000E+00, 0.0000000000000000E+00 },
        };

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
     * @testedMethod {@link CoriolisRelativisticEffect#CoriolisEffect(org.orekit.utils.PVCoordinatesProvider, CelestialBody, boolean)}
     * @testedMethod {@link CoriolisRelativisticEffect#CoriolisEffect(org.orekit.utils.PVCoordinatesProvider, CelestialBody, fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D, boolean)}
     * 
     * @description compute acceleration partial derivatives wrt position
     * 
     * @input instances of {@link CoriolisRelativisticEffect}
     * 
     * @output partial derivatives
     * 
     * @testPassCriteria partial derivatives must be all null, since computation is deactivated at construction :
     *                   instantiation is done with null tabs of normalized coefficients used for partial derivatives
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
            FramesFactory.getGCRF(),
            AbsoluteDate.J2000_EPOCH, 0.39860044150000E+15);
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

        final JPLEphemeridesLoader loaderSun = new JPLEphemeridesLoader("unxp2000.405",
            JPLEphemeridesLoader.EphemerisType.SUN);
        final CelestialBody sunJPL = loaderSun.loadCelestialBody(CelestialBodyFactory.SUN);

        // Specific Sun
        this.sun = new CelestialBody(){

            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
                // u vector hard encoded in ZOOM. From that vector can be deduced Sun position and velocity
                // Only velocity and p ^ v is important hence position is chosen arbitrarily only norm must match
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

            @Override
            public Frame getInertiallyOrientedFrame() throws PatriusException {
                return sunJPL.getInertiallyOrientedFrame();
            }

            @Override
            public double getGM() {
                return 0.132712437742476E+21;
            }

            @Override
            public Frame getBodyOrientedFrame() throws PatriusException {
                return sunJPL.getBodyOrientedFrame();
            }
        };
    }
}
