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
 * VERSION:4.13:DM:DM-3:08/12/2023:[PATRIUS] Distinction entre corps celestes et barycentres
 * VERSION:4.11:FA:FA-3314:22/05/2023:[PATRIUS] Anomalie lors de l'evaluation d'un ForceModel lorsque le SpacecraftState est en ITRF
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
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
import fr.cnes.sirius.patrius.bodies.CelestialPoint;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
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
 * Class test for {@link SchwarzschildRelativisticEffect} class.
 * 
 * @author rodriguest
 * 
 * @version $Id: SchwarzschildRelativisticEffectTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 3.2
 * 
 */
public class SchwarzschildRelativisticEffectTest {

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(SchwarzschildRelativisticEffectTest.class.getSimpleName(), "Schwarzschild force");
    }

    /**
     * 
     * @testType UT
     * 
     * @throws PatriusException
     *         when an error occurs.
     * 
     * @testedMethod {@link SchwarzschildRelativisticEffect#computeAcceleration(fr.cnes.sirius.patrius.propagation.SpacecraftState)}
     * @testedMethod {@link SchwarzschildRelativisticEffect#addContribution(fr.cnes.sirius.patrius.propagation.SpacecraftState, fr.cnes.sirius.patrius.propagation.numerical.TimeDerivativesEquations)}
     * 
     * @description Test for the method computeAcceleration() and addContribution()
     * 
     * @input orbit
     * 
     * @output acceleration
     * 
     * @testPassCriteria acceleration is as expected (ZOOM reference, relative tolerance: 1E-14)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     * 
     */
    @Test
    public void testAcceleration() throws PatriusException {

        final double eps = 1E-14;
        Report.printMethodHeader("testAcceleration", "Acceleration computation", "ZOOM", eps, ComparisonType.RELATIVE);

        // Initialization
        final SchwarzschildRelativisticEffect force = new SchwarzschildRelativisticEffect(0.39860044150000E+15);

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
            /** Serializable UID. */
            private static final long serialVersionUID = 4458275700612417218L;

            @Override
            public void initDerivatives(final double[] yDot, final Orbit currentOrbit) throws PropagationException {
                // nothing to do
            }

            @Override
            public void addXYZAcceleration(final double x, final double y, final double z) {
                actual2[0] = x;
                actual2[1] = y;
                actual2[2] = z;
            }

            @Override
            public void addAdditionalStateDerivative(final String name, final double[] pDot) {
                // nothing to do
            }

            @Override
            public void addAcceleration(final Vector3D gamma, final Frame frame) throws PatriusException {
                // nothing to do
            }
        };
        force.addContribution(state, adder);

        // Check
        final double[] expected = { -0.921916014622375E-09, -0.687449532624502E-10, -0.147413971249663E-07 };
        Assert.assertEquals(0., (actual[0] - expected[0]) / expected[0], eps);
        Assert.assertEquals(0., (actual[1] - expected[1]) / expected[1], eps);
        Assert.assertEquals(0., (actual[2] - expected[2]) / expected[2], eps);
        Assert.assertEquals(actual[0], actual2[0], eps);
        Assert.assertEquals(actual[1], actual2[1], eps);
        Assert.assertEquals(actual[2], actual2[2], eps);

        Report.printToReport("Acceleration", expected, actual);
    }

    /**
     * 
     * @testType UT
     * 
     * @throws PatriusException
     *         when an error occurs.
     * 
     * @testedMethod {@link SchwarzschildRelativisticEffect#computeAcceleration(fr.cnes.sirius.patrius.propagation.SpacecraftState)}
     * 
     * @description Test that the method computeAcceleration() throw a PatriusException when the frame is not
     *              pseudo-inertial
     * 
     * @input orbit
     * 
     * @output PatriusException
     * 
     * @testPassCriteria A PatriusException is thrown if the input frame is not pseudo inertial
     * 
     * @referenceVersion 4.11
     * 
     */
    @Test
    public void testNotPseudoInertial() throws PatriusException {

        // Initialization
        final SchwarzschildRelativisticEffect force = new SchwarzschildRelativisticEffect(0.39860044150000E+15);

        final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(23967 * 86400. + 55320.);
        final PVCoordinates pv = new PVCoordinates(new Vector3D(-0.439584017658778E+06, -0.296038007930370E+05,
            -0.709617640153498E+07),
            new Vector3D(0.573899359984705E+04, 0.477793965015048E+04, -0.371114951980137E+03));
        final Orbit orbit = new CartesianOrbit(pv, FramesFactory.getITRF(), date, 0.39860044150000E+15);
        final SpacecraftState state = new SpacecraftState(orbit);

        // Computation
        try {
            force.computeAcceleration(state);
            Assert.fail();
        } catch (PatriusException pe) {
            Assert.assertEquals(pe.getMessage(), PatriusMessages.NOT_INERTIAL_FRAME.getSourceString());
        }
    }

    /**
     * Test the partial derivatives computation.
     * 
     * @throws PatriusException
     *         when an error occurs.
     * 
     * @testedMethod {@link SchwarzschildRelativisticEffect#addDAccDState(fr.cnes.sirius.patrius.propagation.SpacecraftState, double[][], double[][])}
     * @testedMethod {@link SchwarzschildRelativisticEffect#addDAccDParam(fr.cnes.sirius.patrius.propagation.SpacecraftState, org.orekit.parameter.Parameter, double[])}
     * 
     * @description Test the computation of acceleration partial derivatives wrt position and velocity
     * 
     * @input orbit
     * 
     * @output partial derivatives
     * 
     * @testPassCriteria partial derivatives are as expected (ZOOM reference, relative tolerance: 1E-14)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     * 
     */
    @Test
    public void testPartialDerivatives() throws PatriusException {

        final double eps = 2E-14;
        Report.printMethodHeader("testPartialDerivatives", "Partial derivatives computation", "ZOOM", eps,
            ComparisonType.RELATIVE);

        // Initialization
        final SchwarzschildRelativisticEffect force = new SchwarzschildRelativisticEffect(0.39860044150000E+15);

        final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(23967 * 86400. + 55320.);
        final PVCoordinates pv = new PVCoordinates(new Vector3D(-0.439584017658778E+06, -0.296038007930370E+05,
            -0.709617640153498E+07),
            new Vector3D(0.573899359984705E+04, 0.477793965015048E+04, -0.371114951980137E+03));
        final Orbit orbit = new CartesianOrbit(pv, FramesFactory.getEME2000(), date, 0.39860044150000E+15);
        final SpacecraftState state = new SpacecraftState(orbit);

        // Computation
        final double[][] dAccdPos = new double[3][3];
        final double[][] dAccdVel = new double[3][3];
        force.addDAccDState(state, dAccdPos, dAccdVel);

        // Check
        final double[][] expectedPos = {
            { 0.366855120816093E-14, 0.135115214469127E-14, -0.664149834323562E-15 },
            { 0.135097843700304E-14, 0.320411169542991E-14, -0.127975205586269E-15 },
            { -0.660470528594218E-15, -0.124923270625172E-15, -0.688059905244968E-14 },
        };
        final double[][] expectedVel = {
            { -0.637784383179532E-13, 0.434496482225179E-13, -0.201421550756593E-11 },
            { -0.994784461063463E-13, -0.500712024320431E-14, -0.167383340873252E-11 },
            { 0.101314707905826E-11, 0.837323422934707E-12, 0.634787185227780E-13 }
        };

        for (int i = 0; i < expectedPos.length; i++) {
            for (int j = 0; j < expectedPos[0].length; j++) {
                Assert.assertEquals(0., (dAccdPos[i][j] - expectedPos[i][j]) / expectedPos[i][j], eps);
                Assert.assertEquals(0., (dAccdVel[i][j] - expectedVel[i][j]) / expectedVel[i][j], eps);
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
     * @testedMethod {@link SchwarzschildRelativisticEffect#SchwarzschildEffect(CelestialPoint, boolean, boolean)}
     * 
     * @description compute acceleration partial derivatives wrt position
     * 
     * @input instances of {@link SchwarzschildRelativisticEffect}
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

        // Instance
        final SchwarzschildRelativisticEffect force = new SchwarzschildRelativisticEffect(0.39860044150000E+15, false,
            false);
        Assert.assertEquals(force.getEventsDetectors().length, 0);

        // Check partial derivatives computation is deactivated
        Assert.assertFalse(force.computeGradientPosition());
        Assert.assertFalse(force.computeGradientVelocity());

        // Partial derivatives
        final double[][] dAccdPos = new double[3][3];
        final double[][] dAccdVel = new double[3][3];

        force.addDAccDState(state, dAccdPos, dAccdVel);

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
    }
}
