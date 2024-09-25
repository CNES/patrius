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
 * VERSION:4.13:DM:DM-108:08/12/2023:[PATRIUS] Modele d'obliquite et de precession de la Terre
 * VERSION:4.11:FA:FA-3314:22/05/2023:[PATRIUS] Anomalie lors de l'evaluation d'un ForceModel lorsque le SpacecraftState est en ITRF
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:529:23/02/2016: relativistic effects
 * VERSION::DM:660:24/09/2016:add getters to frames configuration
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
import fr.cnes.sirius.patrius.frames.configuration.FrameConvention;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationBuilder;
import fr.cnes.sirius.patrius.frames.configuration.PolarMotion;
import fr.cnes.sirius.patrius.frames.configuration.eop.PoleCorrection;
import fr.cnes.sirius.patrius.frames.configuration.libration.LibrationCorrectionModel;
import fr.cnes.sirius.patrius.frames.configuration.precessionnutation.PrecessionNutation;
import fr.cnes.sirius.patrius.frames.configuration.precessionnutation.PrecessionNutationModelFactory;
import fr.cnes.sirius.patrius.frames.configuration.sp.SPrimeModelFactory;
import fr.cnes.sirius.patrius.frames.configuration.tides.TidalCorrectionModelFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.TimeDerivativesEquations;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * Class test for {@link LenseThirringRelativisticEffect} class.
 * 
 * 
 * @author rodriguest
 * 
 * @version $Id: LenseThirringRelativisticEffectTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 3.2
 * 
 */
public class LenseThirringRelativisticEffectTest {

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(LenseThirringRelativisticEffectTest.class.getSimpleName(), "Lense-Thirring force");
    }

    /**
     * 
     * @testType UT
     * 
     * @throws PatriusException
     *         when an error occurs.
     * 
     * @testedMethod {@link LenseThirringEffectEffect#computeAcceleration(fr.cnes.sirius.patrius.propagation.SpacecraftState)}
     * @testedMethod {@link LenseThirringRelativisticEffect#addContribution(fr.cnes.sirius.patrius.propagation.SpacecraftState, fr.cnes.sirius.patrius.propagation.numerical.TimeDerivativesEquations)}
     * 
     * @description Test for the method computeAcceleration() and addContribution()
     * 
     * @input orbit
     * 
     * @output acceleration
     * 
     * @testPassCriteria acceleration is as expected (PATRIUS reference, relative tolerance: 0). Result has been checked
     *                   first
     *                   with respect to ZOOM reference (J value of 11.9E8, relative threshold: 1E-15)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     * 
     */
    @Test
    public void testAcceleration() throws PatriusException {

        final double eps = 0;
        Report.printMethodHeader("testAcceleration", "Acceleration computation", "PATRIUS", eps,
            ComparisonType.RELATIVE);

        // Initialization
        final LenseThirringRelativisticEffect force = new LenseThirringRelativisticEffect(0.39860044150000E+15,
            FramesFactory.getCIRF());

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
            private static final long serialVersionUID = -2995218717107013941L;

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
        final double[] expected = { -2.299038453963706E-10, 2.776748984468113E-10, 1.9662936757454297E-11 };
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
     * @testedMethod {@link LenseThirringEffectEffect#computeAcceleration(fr.cnes.sirius.patrius.propagation.SpacecraftState)}
     * 
     * @description Test that the method computeAcceleration() throw an exception when the frame is not pseudo-inertial
     * 
     * @input orbit
     * 
     * @output PatriusException
     * 
     * @testPassCriteria PatriusException thrown for non pseudo-inertial frame
     * 
     * @referenceVersion 4.11
     * 
     */
    @Test
    public void testNotPseudoInertial() throws PatriusException {

        // Initialization
        final LenseThirringRelativisticEffect force = new LenseThirringRelativisticEffect(0.39860044150000E+15,
            FramesFactory.getGCRF());

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
     * @testedMethod {@link LenseThirringRelativisticEffect#addDAccDState(fr.cnes.sirius.patrius.propagation.SpacecraftState, double[][], double[][])}
     * @testedMethod {@link LenseThirringRelativisticEffect#addDAccDParam(fr.cnes.sirius.patrius.propagation.SpacecraftState, org.orekit.parameter.Parameter, double[])}
     * 
     * @description Test the computation of acceleration partial derivatives wrt position and velocity
     * 
     * @input orbit
     * 
     * @output partial derivatives
     * 
     * @testPassCriteria acceleration is as expected (PATRIUS reference, relative tolerance: 0). Result has been checked
     *                   first
     *                   with respect to ZOOM reference (J value of 11.9E8, relative threshold: 1E-15)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     * 
     */
    @Test
    public void testPartialDerivatives() throws PatriusException {

        final double eps = 0;
        Report.printMethodHeader("testPartialDerivatives", "Partial derivatives computation", "PATRIUS", eps,
            ComparisonType.RELATIVE);

        // Initialization
        final LenseThirringRelativisticEffect force = new LenseThirringRelativisticEffect(0.39860044150000E+15,
            FramesFactory.getCIRF());

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
        final double[][] expectedPos = new double[][] {
            { -1.2006140199928068E-17, 2.9715810040350952E-18, -9.646346993114024E-17 },
            { 1.0707374638843148E-17, 9.756618302776057E-19, 1.1672328510611442E-16 },
            { -4.7812603172234705E-17, 5.85143163640804E-17, 1.1030478369650464E-17 }
        };
        final double[][] expectedVel = new double[][] {
            { 0.0, -4.8094357790335076E-14, 3.0154096677572594E-16 },
            { 4.8094357790335076E-14, 0.0, -4.477553088222472E-15 },
            { -3.0154096677572594E-16, 4.477553088222472E-15, 0.0 }
        };

        for (int i = 0; i < expectedPos.length; i++) {
            for (int j = 0; j < expectedPos[0].length; j++) {
                Assert.assertEquals(0., (dAccdPos[i][j] - expectedPos[i][j]) / expectedPos[i][j], eps);
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
     * @testedMethod {@link LenseThirringRelativisticEffect#LenseThirringEffect(CelestialPoint, boolean, boolean)}
     * @testedMethod {@link LenseThirringRelativisticEffect#LenseThirringEffect(CelestialPoint, double, double, boolean, boolean)}
     * 
     * @description compute acceleration partial derivatives wrt position
     * 
     * @input instances of {@link LenseThirringRelativisticEffect}
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
        final LenseThirringRelativisticEffect force = new LenseThirringRelativisticEffect(0.39860044150000E+15,
            FramesFactory.getCIRF(), false, false);
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

        // FramesFactory.setConfiguration(Utils.getZOOMConfiguration());

        // Frame configuration
        final FramesConfigurationBuilder builder = new FramesConfigurationBuilder();
        // final PrecessionNutationModel pn = new PrecessionNutationModel() {
        // @Override
        // public boolean isDirect() {
        // return false;
        // }
        // @Override
        // public double[] getCIPMotionTimeDerivative(AbsoluteDate t) {
        // return null;
        // }
        // @Override
        // public double[] getCIPMotion(AbsoluteDate t) {
        // return new double[3];
        // }
        // };
        builder.setCIRFPrecessionNutation(new PrecessionNutation(false, PrecessionNutationModelFactory.NO_PN));
        final LibrationCorrectionModel libration = new LibrationCorrectionModel(){
            /**
             * 
             */
            private static final long serialVersionUID = 9208848141364356124L;

            @Override
            public double getUT1Correction(final AbsoluteDate t) {
                return 0.289415;
            }

            @Override
            public PoleCorrection getPoleCorrection(final AbsoluteDate t) throws PatriusException {
                return new PoleCorrection(0.217017 * Constants.ARC_SECONDS_TO_RADIANS,
                    0.398023 * Constants.ARC_SECONDS_TO_RADIANS);
            }

            @Override
            public FrameConvention getOrigin() {
                return null;
            }
        };
        final PolarMotion pm = new PolarMotion(false, TidalCorrectionModelFactory.NO_TIDE, libration,
            SPrimeModelFactory.NO_SP);
        builder.setPolarMotion(pm);
        FramesFactory.setConfiguration(builder.getConfiguration());
    }
}
