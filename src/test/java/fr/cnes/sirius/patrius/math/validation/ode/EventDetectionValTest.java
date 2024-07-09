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
 * @history 15/12/2011
 *
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.validation.ode;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.ode.AbstractIntegrator;
import fr.cnes.sirius.patrius.math.ode.FirstOrderDifferentialEquations;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.events.EventHandler;
import fr.cnes.sirius.patrius.math.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.GraggBulirschStoerIntegrator;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * @description Event detection validation.
 *              Since there is no proper reference data, the Validate class is not needed.
 * 
 * @author Julie Anton
 * 
 * @version $Id: EventDetectionValidationTest.java 17909 2017-09-11 11:57:36Z bignon $
 * 
 * @since 1.0
 * 
 */
public class EventDetectionValTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Event detection parametrization
         * 
         * @featureDescription how to deal with the event detection parametrization to reach the required precision
         *                     without deteriorating the performances?
         * 
         * @coveredRequirements DV-EVT_63
         */
        PARAMETRIZATION
    }

    /** Lower end boundary of the integration interval. */
    private static final double X0 = 0;

    /** Upper end boundary of the integration interval. */
    private static final double X100 = 100;

    /** Number of tries. */
    private static final int TRIES = 10;

    /**
     * 
     * @testType TVT
     * 
     * @testedFeature {@link features#PARAMETRIZATION}
     * 
     * @testedMethod {@link AbstractIntegrator#addEventHandler(EventHandler, double, double, int)}
     * 
     * @description Test the detection event on the integration of the harmonic oscillator problem with the following
     *              integrators : Runge Kutta 4, Gragg Bulirsch Stoer, Dormand Prince 853.
     * 
     * @input allowed convergence (double) on the detection process and allowed maximum check interval length on the
     *        detection.
     * 
     * @output obtained precision and execution time
     * 
     * @testPassCriteria the precision should be the required one when the convergence is below the maximum step size
     *                   used for adaptive step size integrators or below the specified step size for fixed step size
     *                   integrators.
     *                   the execution time should increase when the allowed max check interval length decreases.
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void precisionDetectionTest() {

        // Harmonic Oscillator (with a pulsation of 1 rad/s)

        final FirstOrderDifferentialEquations ode = new FirstOrderDifferentialEquations(){
            @Override
            public int getDimension() {
                return 2;
            }

            @Override
            public void computeDerivatives(final double t, final double[] y, final double[] yDot)
            {
                yDot[0] = -y[1];
                yDot[1] = y[0];
            }
        };

        // Time events

        final int n = 10;
        final double[] times = new double[n];
        for (int i = 0; i < n; i++) {
            times[i] = i + 1.0;
        }

        final int dim = ode.getDimension();
        final double[] y0 = new double[dim];

        // initial point
        y0[0] = -1.0 * MathLib.sin(X0) + 0.1 * MathLib.cos(X0);
        y0[1] = 1.0 * MathLib.cos(X0) + 0.1 * MathLib.sin(X0);

        double[] result1;
        double[] result2;
        double convergence;
        double maxCheckIntervals;

        // Runge Kutta integrator

        final FirstOrderIntegrator rk = new ClassicalRungeKuttaIntegrator(0.2);

        convergence = 0.3;
        maxCheckIntervals = 0.2;
        result1 = getPrecision(ode, rk, y0, times, convergence, maxCheckIntervals);

        Assert.assertTrue(result1[0] < convergence);

        convergence = 0.1;
        maxCheckIntervals = 0.2;
        result1 = getPrecision(ode, rk, y0, times, convergence, maxCheckIntervals);

        Assert.assertTrue(result1[0] < convergence);

        convergence = 0.1;
        maxCheckIntervals = 0.01;
        result2 = getPrecision(ode, rk, y0, times, convergence, maxCheckIntervals);

        Assert.assertTrue(result2[0] < convergence);

        // Assert.assertTrue(result1[1] < result2[1]);

        // Gragg Bulirsch Stoer

        final FirstOrderIntegrator gbs = new GraggBulirschStoerIntegrator(0.01, 2, 1e-4, 1);

        convergence = 2.5;
        maxCheckIntervals = 0.01;
        result1 = getPrecision(ode, gbs, y0, times, convergence, maxCheckIntervals);

        Assert.assertTrue(result1[0] < convergence);

        convergence = 0.1;
        maxCheckIntervals = 0.1;
        result1 = getPrecision(ode, gbs, y0, times, convergence, maxCheckIntervals);

        Assert.assertTrue(result1[0] < convergence);

        convergence = 0.1;
        maxCheckIntervals = 0.01;
        result2 = getPrecision(ode, gbs, y0, times, convergence, maxCheckIntervals);

        Assert.assertTrue(result2[0] < convergence);

        // Assert.assertTrue(result1[1] < result2[1]);

        // Dormand Prince 853

        final FirstOrderIntegrator dop = new DormandPrince853Integrator(0.01, 2, 1e-4, 1);

        convergence = 2.5;
        maxCheckIntervals = 0.01;
        result1 = getPrecision(ode, dop, y0, times, convergence, maxCheckIntervals);

        Assert.assertTrue(result1[0] < convergence);

        convergence = 0.1;
        maxCheckIntervals = 0.1;
        result1 = getPrecision(ode, dop, y0, times, convergence, maxCheckIntervals);

        Assert.assertTrue(result1[0] < convergence);

        convergence = 0.1;
        maxCheckIntervals = 0.01;
        result2 = getPrecision(ode, dop, y0, times, convergence, maxCheckIntervals);

        Assert.assertTrue(result2[0] < convergence);

        // Assert.assertTrue(result1[1] < result2[1]);

    }

    /**
     * @description Integration of the specified problem with detection event.
     * 
     * @param ode
     *        : ordinary differential equation
     * @param integ
     *        : integrator
     * @param y0
     *        : initial condition
     * @param times
     *        : time events
     * @param convergence
     *        : convergence for the event detection
     * @param maxCheckIntervals
     *        : maximum interval length for event detection
     * @return the precision of the detection and the execution time
     * 
     * @since 1.0
     */
    private static double[] getPrecision(final FirstOrderDifferentialEquations ode, final FirstOrderIntegrator integ,
                                         final double[] y0, final double[] times, final double convergence,
                                         final double maxCheckIntervals) {

        final List<EventHandler> eventList = new ArrayList<EventHandler>();
        for (final double time : times) {
            eventList.add(new EventMock(time));
        }

        final int dim = ode.getDimension();
        final double[] y = new double[dim];

        // gap between the theoretical event time and the computed one
        double gap;
        double maxGap;

        // integration with event detection

        gap = -1;
        maxGap = -1;

        // integration execution time
        long start;
        long duree;

        // tic
        start = System.currentTimeMillis();
        for (int i = 0; i < TRIES; i++) {
            for (int j = 0; j < eventList.size(); j++) {
                integ.addEventHandler(eventList.get(j), maxCheckIntervals, convergence, 100);
            }
            integ.integrate(ode, X0, y0, X100, y);
            integ.clearEventHandlers();
        }
        // toc
        duree = (System.currentTimeMillis() - start) / TRIES;

        for (int i = 0; i < eventList.size(); i++) {
            gap = MathLib.abs(((EventMock) eventList.get(i)).getTheoreticalTime()
                - ((EventMock) eventList.get(i)).getEventTime());
            if (gap > maxGap) {
                maxGap = gap;
            }
        }
        final double[] result = new double[2];
        result[0] = maxGap;
        result[1] = duree;
        // System.out.println("maxgap " + maxGap + " duree " + duree);
        return result;
    }
}
