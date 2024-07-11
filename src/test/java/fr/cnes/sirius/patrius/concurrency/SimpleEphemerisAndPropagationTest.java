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
 * @history creation 04/04/12
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:262:29/04/2014:Removed standard gravitational parameter from constructor
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::FA:416:12/02/2015:Changed EcksteinHechlerPropagator constructor signature
 * VERSION::DM:368:20/03/2015:Eckstein-Heschler : Back at the "mu"
 * VERSION::DM:480:15/02/2016: new analytical propagators and mean/osculating conversion
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.concurrency;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import junit.framework.Assert;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationFactory;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.BoundedPropagator;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.ParametersType;
import fr.cnes.sirius.patrius.propagation.SimpleMassModel;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.EcksteinHechlerPropagator;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.propagation.events.EclipseDetector;
import fr.cnes.sirius.patrius.propagation.precomputed.Ephemeris;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusFixedStepHandler;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * Parallelism test for an event detection using an ephemeris
 * and a propagation running alongside on a different time period.
 * This test is unrealistic because the propagations are very simple.
 */
public class SimpleEphemerisAndPropagationTest {

    /** Epsilon. */
    private static final double EPSM8 = 1e-8;
    /**
     * Printed string.
     */
    private static final String STR_S_PAREN = " s)";
    /** Init date. */
    private AbsoluteDate initDate;
    /** Final date. */
    private AbsoluteDate finalDate;
    /** Eclipse start counter. */
    private volatile int inEclipsecounter;
    /** Eclipse end counter. */
    private volatile int outEclipsecounter;

    /** Last ephemeris state. */
    private SpacecraftState lastKnownState;

    /** All states array. */
    private List<SpacecraftState> statesTab;

    /**
     * Builds an "ephemeris" array of spacecraftStates using a propagation.
     * 
     * @throws PatriusException
     *         should not happen.
     */
    private void buildEphemArray() throws PatriusException {

        final double mass = 2500;
        final double a = 7187990.1979844316;
        final double e = 0.5e-4;
        final double i = 1.7105407051081795;
        final double omega = 1.9674147913622104;
        final double OMEGA = MathLib.toRadians(261);
        final double lv = 0;
        final double mu = 3.9860047e14;
        final double ae = 6.378137e6;
        final double c20 = -1.08263e-3;
        final double c30 = 2.54e-6;
        final double c40 = 1.62e-6;
        final double c50 = 2.3e-7;
        final double c60 = -5.5e-7;

        this.initDate = new AbsoluteDate(new DateComponents(2000, 01, 01),
            TimeComponents.H00,
            TimeScalesFactory.getUTC());

        this.finalDate = new AbsoluteDate(new DateComponents(2002, 01, 02),
            TimeComponents.H00,
            TimeScalesFactory.getUTC());

        final double deltaT = this.finalDate.durationFrom(this.initDate);

        final Orbit transPar = new KeplerianOrbit(a, e, i, omega, OMEGA, lv, PositionAngle.TRUE,
            FramesFactory.getEME2000(), this.initDate, mu);

        final int nbIntervals = 20000;
        final MassProvider massModel = new SimpleMassModel(mass, "default");
        final EcksteinHechlerPropagator eck =
            new EcksteinHechlerPropagator(transPar, ae, mu, transPar.getFrame(),
                c20, c30, c40, c50, c60, massModel, ParametersType.OSCULATING);

        this.statesTab = new ArrayList<SpacecraftState>();
        for (int j = 0; j <= nbIntervals; j++) {
            final AbsoluteDate current = this.initDate.shiftedBy((j * deltaT) / nbIntervals);
            this.statesTab.add(eck.propagate(current));
        }

        this.lastKnownState = this.statesTab.get(nbIntervals);
    }

    /**
     * Builds an eclipse detector.
     * 
     * @throws PatriusException
     *         should not happen.
     * @return an eclipse detector.
     */
    private EclipseDetector buildEclipsDetector() throws PatriusException {

        final double sunRadius = 696000000.;
        final double earthRadius = 6400000.;

        // Reset the eclipse counters
        this.inEclipsecounter = 0;
        this.outEclipsecounter = 0;

        final EclipseDetector ecl = new EclipseDetector(CelestialBodyFactory.getSun(), sunRadius,
            CelestialBodyFactory.getEarth(), earthRadius, 0,
            60., 1.e-3){
            private static final long serialVersionUID = 1L;

            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                if (increasing) {
                    ++SimpleEphemerisAndPropagationTest.this.inEclipsecounter;
                } else {
                    ++SimpleEphemerisAndPropagationTest.this.outEclipsecounter;
                }
                return Action.CONTINUE;
            }
        };

        return ecl;
    }

    /**
     * Single thread, the two operations run sequentially.
     * 
     * @throws PatriusException
     *         should not happen.
     */
    // @Test(priority = 1)
    public void testSingleThread() throws PatriusException {
        this.buildEphemArray();
        // Detect event in the ephemeris propagation
        this.testEphem();
        // Propagate further from the last known ephemeris state
        this.testPropag();
    }

    /**
     * Init for the Multi-threaded test.
     * 
     * @throws PatriusException
     *         should not happen.
     */
    @Test(priority = 2)
    public void testMultiThreadPart1() throws PatriusException {
        this.buildEphemArray();
    }

    /** Used to choose which thread runs what. */
    private volatile boolean swap = true;

    /**
     * Multi-threaded, the two operations run in parallel.
     * 
     * @throws PatriusException
     *         should not happen.
     */
    @Test(priority = 3, invocationCount = 2, threadPoolSize = 2)
    public void testMultiThreadPart2() throws PatriusException {
        if (this.swap) {
            this.swap = !this.swap;
            // Detect event in the ephemeris propagation
            this.testEphem();
        } else {
            this.swap = !this.swap;
            // Propagate further from the last known ephemeris state
            this.testPropag();
        }
    }

    /**
     * Implements the first test : event detection in an ephemeris.
     * 
     * @throws PatriusException
     *         should not happen.
     */
    private void testEphem() throws PatriusException {

        final BoundedPropagator ephem = new Ephemeris(this.statesTab, 2);

        System.out.println("START testEphem");
        final double startTime = (new Date()).getTime();
        ephem.addEventDetector(this.buildEclipsDetector());

        final AbsoluteDate computeEnd = new AbsoluteDate(this.finalDate, -1000.0);

        ephem.setSlaveMode();
        final SpacecraftState state = ephem.propagate(computeEnd);
        Assert.assertEquals(0., state.getDate().durationFrom(computeEnd), EPSM8);
        Assert.assertEquals(9326, this.inEclipsecounter);
        Assert.assertEquals(9325, this.outEclipsecounter);
        // Assert on some numerical value to ensure consistency
        Assert.assertEquals(7192526.142168622, state.getA(), EPSM8);
        final double endTime = (new Date()).getTime();
        final double duration = (endTime - startTime) / 1000.;
        System.out.println("END testEphem (duration " + duration + STR_S_PAREN);

    }

    /**
     * Implements the second test : propagation.
     * 
     * @throws PropagationException
     *         should not happen.
     */
    private void testPropag() throws PropagationException {
        System.out.println("START testPropag");
        final double startTime = (new Date()).getTime();

        // Dummy step handler
        final PatriusFixedStepHandler dummyHandler = new PatriusFixedStepHandler(){

            /**
             * Serial UID.
             */
            private static final long serialVersionUID = -8551469038638270536L;

            @Override
            public void init(final SpacecraftState s0, final AbsoluteDate t) {
                // does nothing.
            }

            @Override
            public void handleStep(final SpacecraftState currentState, final boolean isLast)
                                                                                            throws PropagationException {
                // does nothing.
            }
        };

        final KeplerianPropagator kp = new KeplerianPropagator(this.lastKnownState.getOrbit());
        final AbsoluteDate target = new AbsoluteDate(this.finalDate, 1.E7);
        kp.setMasterMode(5, dummyHandler);
        final SpacecraftState finalState = kp.propagate(target);
        Assert.assertEquals(0., finalState.getDate().durationFrom(target), EPSM8);
        // Assert on some numerical value to ensure consistency
        Assert.assertEquals(7193207.039623823, finalState.getA(), EPSM8);
        final double endTime = (new Date()).getTime();
        final double duration = (endTime - startTime) / 1000.;
        System.out.println("END testPropag (duration " + duration + STR_S_PAREN);
    }

    /**
     * Orekit setup.
     */
    @BeforeClass
    public void setUp() {
        Utils.setDataRoot("regular-dataCNES-2003");
        FramesFactory.setConfiguration(FramesConfigurationFactory.getIERS2003Configuration(true));
        this.inEclipsecounter = 0;
        this.outEclipsecounter = 0;
    }

}
