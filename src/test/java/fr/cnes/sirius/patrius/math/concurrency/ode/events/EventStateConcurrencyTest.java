/**
 * 
 * Copyright 2011-2022 CNES
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
 * @history creation 02/02/2012
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.concurrency.ode.events;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import fr.cnes.sirius.patrius.math.analysis.solver.BrentSolver;
import fr.cnes.sirius.patrius.math.ode.FirstOrderDifferentialEquations;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.events.EventHandler;
import fr.cnes.sirius.patrius.math.ode.events.EventState;
import fr.cnes.sirius.patrius.math.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import fr.cnes.sirius.patrius.math.ode.sampling.StepInterpolator;

/**
 * 
 * Concurrency tests which aim at showing that the event state object is not thread safe.
 * 
 * @author Julie Anton
 * 
 * @version $Id: EventStateConcurrencyTest.java 17909 2017-09-11 11:57:36Z bignon $
 * 
 * @since 1.1
 * 
 */
public class EventStateConcurrencyTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Event state concurrency
         * 
         * @featureDescription EventState is not thread safe
         * 
         * @coveredRequirements None
         */
        EVENTSTATE_THREAD_SAFETY
    }

    /** Thread pool for TestNG. */
    private static final int NUMBER_OF_THREADS = 10;

    /** Invocations for TestNG. */
    private static final int NUMBER_OF_INVOCATIONS = 1000;

    /** First order integrator shared between the threads. */
    private FirstOrderIntegrator integ;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#EVENTSTATE_THREAD_SAFETY}
     * 
     * @testedMethod {@link EventState#evaluateStep(StepInterpolator)}
     * 
     * @description proves that the EventState is not thread safe
     * 
     * @input first order integrator with an event handler
     * 
     * @output result of the integration of the keplerian problem
     * 
     * @testPassCriteria an internal error is produced due to an incoherent state of EventState
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test(invocationCount = NUMBER_OF_INVOCATIONS, threadPoolSize = NUMBER_OF_THREADS)
    public void testWithEventDetection() {
        final double[] v = { 6.4688587830467382E+06, -1.8805091845627432E+06, -1.3293159229471583E+04,
            2.1471807451962504E+03, 7.3823935125280523E+03, -1.1409758242487955E+01 };
        final FirstOrderDifferentialEquations ode = new KeplerProblem();
        final double[] y = new double[6];
        final int end = 10;
        this.integ.integrate(ode, 0, v, end, y);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#EVENTSTATE_THREAD_SAFETY}
     * 
     * @testedMethod {@link FirstOrderIntegrator#integrate(FirstOrderDifferentialEquations, double, double[], double, double[])}
     * 
     * @description proves that without the event detection the integration is thread safe
     * 
     * @input first order integrator without any event handlers
     * 
     * @output result of the integration of the keplerian problem
     * 
     * @testPassCriteria no exception is thrown
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test(invocationCount = NUMBER_OF_INVOCATIONS, threadPoolSize = NUMBER_OF_THREADS)
    public void testWithoutEventDetection() {
        this.integ.clearEventHandlers();
        final double[] v = { 6.4688587830467382E+06, -1.8805091845627432E+06, -1.3293159229471583E+04,
            2.1471807451962504E+03, 7.3823935125280523E+03, -1.1409758242487955E+01 };
        final FirstOrderDifferentialEquations ode = new KeplerProblem();
        final double[] y = new double[6];
        final int end = 10;
        this.integ.integrate(ode, 0, v, end, y);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#EVENTSTATE_THREAD_SAFETY}
     * 
     * @testedMethod {@link FirstOrderIntegrator#integrate(FirstOrderDifferentialEquations, double, double[], double, double[])}
     * 
     * @description proves that with an unshared event detectors the integration is thread safe, only the integrator is
     *              shared between the threads
     * 
     * @input first order integrator with an unshared event handler
     * 
     * @output result of the integration of the keplerian problem
     * 
     * @testPassCriteria no exception is thrown
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test(invocationCount = NUMBER_OF_INVOCATIONS, threadPoolSize = NUMBER_OF_THREADS)
    public void testWithUnsharedEventDetection2() {
        this.integ.clearEventHandlers();
        final EventHandler handler = new EventMock(3);
        this.integ.addEventHandler(handler, 1, 0.1, 100, new BrentSolver());
        final double[] v = { 6.4688587830467382E+06, -1.8805091845627432E+06, -1.3293159229471583E+04,
            2.1471807451962504E+03, 7.3823935125280523E+03, -1.1409758242487955E+01 };
        final FirstOrderDifferentialEquations ode = new KeplerProblem();
        final double[] y = new double[6];
        final int end = 10;
        this.integ.integrate(ode, 0, v, end, y);
    }

    /**
     * Initialisation of the tests
     * 
     * @since 1.1
     */
    @BeforeClass
    public void setUp() {
        final EventHandler handler = new EventMock(3);
        this.integ = new ClassicalRungeKuttaIntegrator(10);
        this.integ.addEventHandler(handler, 1, 0.1, 100, new BrentSolver());
    }

}
