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
 * HISTORY
 * VERSION:4.15:OPENFD-385:21/11/2024:Execution en parallele des tests concernant EclipticJ2000Provider
 * VERSION:4.15:OPENFD-221:21/11/2024:[STELA-PATRIUS] Interpolateur STELA précis
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:400:17/03/2015: use class FastMath instead of class Math
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.ode.nonstiff;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.MeeusSun;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.forces.atmospheres.MSISE2000;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.ConstantSolarActivity;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.specialized.ClassicalMSISE2000SolarData;
import fr.cnes.sirius.patrius.forces.gravity.potential.PotentialCoefficientsProvider;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.ode.FirstOrderDifferentialEquations;
import fr.cnes.sirius.patrius.math.ode.sampling.StepHandler;
import fr.cnes.sirius.patrius.math.ode.sampling.StepInterpolator;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.KeplerianParameters;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.stela.PotentialCoefficientsProviderTest;
import fr.cnes.sirius.patrius.stela.bodies.MeeusMoonStela;
import fr.cnes.sirius.patrius.stela.forces.StelaForceModel;
import fr.cnes.sirius.patrius.stela.forces.drag.StelaAeroModel;
import fr.cnes.sirius.patrius.stela.forces.drag.StelaAtmosphericDrag;
import fr.cnes.sirius.patrius.stela.forces.drag.StelaConstantDragCoef;
import fr.cnes.sirius.patrius.stela.forces.gravity.StelaThirdBodyAttraction;
import fr.cnes.sirius.patrius.stela.forces.gravity.recurrence.StelaRecurrenceZonalAttraction;
import fr.cnes.sirius.patrius.stela.propagation.StelaDifferentialEquations;
import fr.cnes.sirius.patrius.stela.propagation.StelaGTOPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @description test class for RungeKutta6StepInterpolator
 * 
 * @author Cedric Dental
 * 
 * @version $Id: RungeKutta6StepInterpolatorTest.java 17909 2017-09-11 11:57:36Z bignon $
 * 
 * @since 1.3
 */
public class RungeKutta6StepInterpolatorTest {

    /** Step counter. */
    private static int ji = 0;

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Stela Interpolator
         * 
         * @featureDescription Adding 6th order Runge-Kutta Step Interpolator
         * 
         * @coveredRequirements
         */
        INTERRK6
    }

    /** Internal first order differential equations. */
    private static class CircleDiffEq implements FirstOrderDifferentialEquations {

        /** {@inheritDoc} */
        @Override
        public void computeDerivatives(final double t, final double[] y, final double[] yDot) {
            // Circle equation
            yDot[0] = -y[1];
            yDot[1] = y[0];
        }

        /** {@inheritDoc} */
        @Override
        public int getDimension() {
            return 2;
        }
    }

    /** Internal step handler to store ephemeris and interpolators. */
    private static class CircleStepHandler implements StepHandler {

        /** {@inheritDoc} */
        @Override
        public void handleStep(final StepInterpolator interpolator, final boolean isLast) {

            // Expected reference values
            /*
             * Note: Since
             * "OPENFD-221 : Replace linear RK interpolator by more precise, non-linear interpolator from STELA-LOS",
             * the RungeKutta6StepInterpolator#computeInterpolatedStateAndDerivatives method has evolved.
             * This method is already validated by others tests.
             * New references are generated to keep track of the new computations by non-regression.
             */
            final double[] RESULTS =
                { 0.1963495408493621, 0.9807852803484617, 0.19509032945638108, 0.9568328220448965, 0.2904852546283032 };

            // final double startT = interpolator.getPreviousTime();
            // final double[] endState = interpolator.getInterpolatedState();
            // final double endT = interpolator.getCurrentTime();

            // Increase the step counter
            ji++;
            // Evaluate only the first step
            if (ji == 1) {

                Assert.assertEquals(RESULTS[0], interpolator.getInterpolatedTime(), 1e-15);
                Assert.assertEquals(RESULTS[1], interpolator.getInterpolatedState()[0], 1e-15);
                Assert.assertEquals(RESULTS[2], interpolator.getInterpolatedState()[1], 1e-15);

                final double theta = 1.5;
                final double oneMinusThetaH = 1 - theta;
                ((RungeKutta6StepInterpolator) interpolator).computeInterpolatedStateAndDerivatives(theta,
                    oneMinusThetaH);

                Assert.assertEquals(RESULTS[3], interpolator.getInterpolatedState()[0], 1e-15);
                Assert.assertEquals(RESULTS[4], interpolator.getInterpolatedState()[1], 1e-15);
            }
            // System.out.println("STEP "+endT+" "+endState[0]+" "+endState[1]);
        }

        /** {@inheritDoc} */
        @Override
        public void init(final double t0, final double[] y0, final double t) {
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INTERRK6}
     * 
     * @testedMethod {@link RungeKutta6StepInterpolator#RungeKutta6StepInterpolator()}
     * 
     * @description Test on the Linear Step Interpolator.
     *
     * @input circleEq, circleSH, integrator, interpolator
     * 
     * @output Interpolated Time and State
     * 
     * @testPassCriteria Results according to Stela RK6 Linear Step Interpolator
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testInterpolator() {

        // definition of the integrator
        final double integrationStep = MathLib.PI / 16.;
        final RungeKutta6Integrator integrator = new RungeKutta6Integrator(integrationStep);
        final FirstOrderDifferentialEquations circleEq = new CircleDiffEq();

        // StepHandler
        final StepHandler circleSH = new CircleStepHandler();
        integrator.addStepHandler(circleSH);

        // Inputs
        final double startTime = 0.;
        final double[] initialState = { 1., 0 };
        final double endTime = 2 * MathLib.PI;
        final double[] finalState = new double[2];

        integrator.integrate(circleEq, startTime, initialState, endTime, finalState);
    }

    @Test
    public void testIsIncoherentState() {

        final RungeKutta6StepInterpolator interpolator = new RungeKutta6StepInterpolator();

        // Case where currY has fewer than 6 elements
        final double[] shortArray = { 1.0, 2.0, 3.0 };
        Assert.assertFalse(interpolator.isIncoherentState(shortArray));

        // Case where perigee is negative (test1)
        final double[] negativePerigee = { 0.5, 0.0, 0.6, 0.8, 0.0, 0.0 };
        Assert.assertTrue(interpolator.isIncoherentState(negativePerigee));

        // Case where eccentricity is greater than or equal to 1 (test2)
        final double[] highEccentricity = { 1.0, 0.0, 1.1, 0.9, 0.0, 0.0 };
        Assert.assertTrue(interpolator.isIncoherentState(highEccentricity));

        // Case where sin(i/2) >= 1 (test3)
        final double[] highSinIOver2 = { 1.0, 0.0, 0.1, 0.1, 1.0, 1.0 };
        Assert.assertTrue(interpolator.isIncoherentState(highSinIOver2));

        // Case where one of the inclination values is NaN (test4)
        final double[] nanInclination = { 1.0, 0.0, 0.1, 0.1, Double.NaN, 0.0 };
        Assert.assertTrue(interpolator.isIncoherentState(nanInclination));

        // Case where one of the inclination values is infinite (test4)
        final double[] infinityInclination = { 1.0, 0.0, 0.1, 0.1, Double.POSITIVE_INFINITY, 0.0 };
        Assert.assertTrue(interpolator.isIncoherentState(infinityInclination));

        // Case where an ArithmeticException is triggered
        final double[] arithmeticException = { 1.0, 0.0, Double.NaN, 0.0, 0.0, 0.0 };
        Assert.assertTrue(interpolator.isIncoherentState(arithmeticException));
    }

    @Before
    public void setUp() {
        Utils.clear();
    }

    /**
     * Test interpolation of RK4: performs integration with large time step and then with small timestep.<br>
     * 1st ephemeris is then sub-sampled and should be coherent with 2nd ephemeris.
     */
    @Test
    public void testInterpolationRK4() throws PatriusException {
        // Set test resources
        Utils.clear();

        // Next line clears data set by other tests, are overridden later
        Utils.setDataRoot("regular-dataPBASE");

        // Initial common data
        final KeplerianOrbit keplerianOrbit = new KeplerianOrbit(7000e3, 0, 0, 0, 0, 0, PositionAngle.MEAN,
            FramesFactory.getCIRF(), new AbsoluteDate(17532, TimeScalesFactory.getTAI()), Constants.CNES_STELA_MU);
        final SpacecraftState state = new SpacecraftState(keplerianOrbit);
        final double[] initialState = { keplerianOrbit.getA(), 0, 0, 0, 0, 0 };
        final int duration = 100;
        final List<StelaForceModel> forces = new ArrayList<>();
        final PotentialCoefficientsProvider provider = new PotentialCoefficientsProviderTest();
        forces.add(new StelaRecurrenceZonalAttraction(provider, 10));
        forces.add(new StelaThirdBodyAttraction(new MeeusMoonStela(Constants.CNES_STELA_AE), 4, 2, 0));
        final double f = 1 / 0.29825765000000E+03;
        forces.add(new StelaAtmosphericDrag(new StelaAeroModel(1000, new StelaConstantDragCoef(2.2), 10),
            new MSISE2000(new ClassicalMSISE2000SolarData(new ConstantSolarActivity(140, 15)),
                new OneAxisEllipsoid(Constants.CNES_STELA_AE, f, FramesFactory.getCIRF()),
                new MeeusSun(MeeusSun.MODEL.STELA)),
            33, Constants.CNES_STELA_AE, 2500E3, 1));

        // RK4 integration with 1 day time step
        final ClassicalRungeKuttaIntegrator integratorRK4_1day = new ClassicalRungeKuttaIntegrator(1);
        final StelaGTOPropagator propagator_1day = new StelaGTOPropagator(integratorRK4_1day);
        propagator_1day.setInitialState(state, 1000, false);
        for (final StelaForceModel force : forces) {
            propagator_1day.addForceModel(force);
        }
        final StelaDifferentialEquations diffEq_1day = new StelaDifferentialEquations(propagator_1day);
        final MyStepHandler stepHandlerRK4_1day = new MyStepHandler();
        integratorRK4_1day.addStepHandler(stepHandlerRK4_1day);
        integratorRK4_1day.integrate(diffEq_1day, 0, initialState, duration, new double[6]);

        // RK4 integration with 10 days time step
        final ClassicalRungeKuttaIntegrator integratorRK4_10day = new ClassicalRungeKuttaIntegrator(10);
        final StelaGTOPropagator propagator_10day = new StelaGTOPropagator(integratorRK4_10day);
        propagator_10day.setInitialState(state, 1000, false);
        for (final StelaForceModel force : forces) {
            propagator_10day.addForceModel(force);
        }
        final StelaDifferentialEquations diffEq_10day = new StelaDifferentialEquations(propagator_10day);
        final MyStepHandler stepHandlerRK4_10day = new MyStepHandler();
        integratorRK4_10day.addStepHandler(stepHandlerRK4_10day);
        integratorRK4_10day.integrate(diffEq_10day, 0, initialState, duration, new double[6]);

        // Check sub-sampled ephemeris are close
        double maxDiffPos = 0.;
        for (int i = 0; i < duration; i++) {
            final PVCoordinates pvRef = stepHandlerRK4_1day.ephemeris.get(i);
            final StepInterpolator interpolator = stepHandlerRK4_10day.interpolators.get((int) MathLib.floor(i / 10.));
            interpolator.setInterpolatedTime(i);
            final double[] interpolatedState = interpolator.getInterpolatedState();
            final PVCoordinates pvInter = new KeplerianParameters(interpolatedState[0], interpolatedState[2],
                interpolatedState[3], interpolatedState[4], interpolatedState[5], interpolatedState[1],
                PositionAngle.MEAN, Constants.CNES_STELA_MU).getCartesianParameters().getPVCoordinates();
            final double diffPos = pvRef.getPosition().distance(pvInter.getPosition());
            maxDiffPos = MathLib.max(maxDiffPos, diffPos);
            Assert.assertEquals(0., diffPos, 2e-8);
        }
        // System.out.println("max = " + maxDiffPos);
        Assert.assertEquals(0., maxDiffPos, 2e-8);
    }

    /**
     * Test interpolation of RK6: performs integration with large time step and then with small timestep.<br>
     * 1st ephemeris is then sub-sampled and should be coherent with 2nd ephemeris.
     */
    @Test
    public void testInterpolationRK6() throws PatriusException {
        // Set test resources
        Utils.clear();

        // Next line clears data set by other tests, are overridden later
        Utils.setDataRoot("regular-dataPBASE");

        // Initial common data
        final KeplerianOrbit keplerianOrbit = new KeplerianOrbit(7000e3, 0, 0, 0, 0, 0, PositionAngle.MEAN,
            FramesFactory.getCIRF(), new AbsoluteDate(17532, TimeScalesFactory.getTAI()), Constants.CNES_STELA_MU);
        final SpacecraftState state = new SpacecraftState(keplerianOrbit);
        final double[] initialState = { keplerianOrbit.getA(), 0, 0, 0, 0, 0 };
        final int duration = 100;
        final List<StelaForceModel> forces = new ArrayList<>();
        final PotentialCoefficientsProvider provider = new PotentialCoefficientsProviderTest();
        forces.add(new StelaRecurrenceZonalAttraction(provider, 10));
        forces.add(new StelaThirdBodyAttraction(new MeeusMoonStela(Constants.CNES_STELA_AE), 4, 2, 0));
        final double f = 1 / 0.29825765000000E+03;
        forces.add(new StelaAtmosphericDrag(new StelaAeroModel(1000, new StelaConstantDragCoef(2.2), 10),
            new MSISE2000(new ClassicalMSISE2000SolarData(new ConstantSolarActivity(140, 15)),
                new OneAxisEllipsoid(Constants.CNES_STELA_AE, f, FramesFactory.getCIRF()),
                new MeeusSun(MeeusSun.MODEL.STELA)),
            33, Constants.CNES_STELA_AE, 2500E3, 1));

        // RK6 integration with 1 day timestep
        final RungeKutta6Integrator integratorRK6_1day = new RungeKutta6Integrator(1);
        final StelaGTOPropagator propagator_1day = new StelaGTOPropagator(integratorRK6_1day);
        propagator_1day.setInitialState(state, 1000, false);
        for (final StelaForceModel force : forces) {
            propagator_1day.addForceModel(force);
        }
        final StelaDifferentialEquations diffEq_1day = new StelaDifferentialEquations(propagator_1day);
        final MyStepHandler stepHandlerRK6_1day = new MyStepHandler();
        integratorRK6_1day.addStepHandler(stepHandlerRK6_1day);
        integratorRK6_1day.integrate(diffEq_1day, 0, initialState, duration, new double[6]);

        // RK6 integration with 10 days timestep
        final RungeKutta6Integrator integratorRK6_10day = new RungeKutta6Integrator(10);
        final StelaGTOPropagator propagator_10day = new StelaGTOPropagator(integratorRK6_10day);
        propagator_10day.setInitialState(state, 1000, false);
        for (final StelaForceModel force : forces) {
            propagator_10day.addForceModel(force);
        }
        final StelaDifferentialEquations diffEq_10day = new StelaDifferentialEquations(propagator_10day);
        final MyStepHandler stepHandlerRK6_10day = new MyStepHandler();
        integratorRK6_10day.addStepHandler(stepHandlerRK6_10day);
        integratorRK6_10day.integrate(diffEq_10day, 0, initialState, duration, new double[6]);

        // Check sub-sampled ephemeris are close
        double maxDiffPos = 0.;
        for (int i = 0; i < duration; i++) {
            final PVCoordinates pvRef = stepHandlerRK6_1day.ephemeris.get(i);
            final StepInterpolator interpolator = stepHandlerRK6_10day.interpolators.get((int) MathLib.floor(i / 10.));
            interpolator.setInterpolatedTime(i);
            final double[] interpolatedState = interpolator.getInterpolatedState();
            final PVCoordinates pvInter = new KeplerianParameters(interpolatedState[0], interpolatedState[2],
                interpolatedState[3], interpolatedState[4], interpolatedState[5], interpolatedState[1],
                PositionAngle.MEAN, Constants.CNES_STELA_MU).getCartesianParameters().getPVCoordinates();
            final double diffPos = pvRef.getPosition().distance(pvInter.getPosition());
            maxDiffPos = MathLib.max(maxDiffPos, diffPos);
            Assert.assertEquals(0., diffPos, 2.7e-6);
        }
        // System.out.println("max = " + maxDiffPos);
        Assert.assertEquals(0., maxDiffPos, 2.7e-6);
    }

    /** Internal step handler to store ephemeris and interpolators. */
    private static class MyStepHandler implements StepHandler {

        /** Ephemeris. */
        private final List<PVCoordinates> ephemeris = new ArrayList<>();

        /** Interpolators. */
        private final List<StepInterpolator> interpolators = new ArrayList<>();

        /**
         * Constructor.
         */
        public MyStepHandler() {
        }

        @Override
        public void handleStep(final StepInterpolator interpolator, final boolean isLast) {
            final double[] interpolatedState = interpolator.getInterpolatedState();
            this.ephemeris.add(new KeplerianParameters(interpolatedState[0], interpolatedState[2],
                interpolatedState[3], interpolatedState[4], interpolatedState[5], interpolatedState[1],
                PositionAngle.MEAN, Constants.CNES_STELA_MU).getCartesianParameters().getPVCoordinates());
            this.interpolators.add(interpolator.copy());
        }

        /** {@inheritDoc} */
        @Override
        public void init(final double t0, final double[] y0, final double t) {
            this.ephemeris.add(new KeplerianParameters(y0[0], y0[1], y0[2], y0[3], y0[4], y0[5], PositionAngle.MEAN,
                Constants.CNES_STELA_MU).getCartesianParameters().getPVCoordinates());
        }
    }
}
