/**
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
 * Copyright 2002-2011 CS Communication & Systèmes
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:262:29/04/2014:Removed standard gravitational parameter from constructor
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:368:20/03/2015:Eckstein-Heschler : Back at the "mu"
 * VERSION::DM:480:15/02/2016: new analytical propagators and mean/osculating conversion
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.cnesmerge.propagation.events;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.EquinoctialOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.ParametersType;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.EcksteinHechlerPropagator;
import fr.cnes.sirius.patrius.propagation.events.EclipseDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.events.EventShifter;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

public class EventShifterTest {

    /*
     * The following code is lifted form Orekit.
     * It should NOT BE MERGED BACK into Orekit!
     */

    private double mu;
    private AbsoluteDate iniDate;
    private Propagator propagator;
    private List<EventEntry> log;

    private final double sunRadius = 696000000.;
    private final double earthRadius = 6400000.;

    private EventDetector createRawDetector(final String nameIncreasing, final String nameDecreasing,
                                            final double tolerance)
                                                                   throws PatriusException {
        return new EclipseDetector(CelestialBodyFactory.getSun(), this.sunRadius,
            CelestialBodyFactory.getEarth(), this.earthRadius, 0,
            60., 1.e-10){
            private static final long serialVersionUID = 1L;

            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                EventShifterTest.this.log.add(new EventEntry(s.getDate().durationFrom(EventShifterTest.this.iniDate),
                    tolerance,
                    increasing ? nameIncreasing : nameDecreasing));
                return Action.CONTINUE;
            }
        };
    }

    @Before
    public void setUp() throws PatriusException {
        try {
            Utils.setDataRoot("regular-dataCNES-2003");
            FramesFactory.setConfiguration(fr.cnes.sirius.patrius.Utils.getIERS2003Configuration(true));
            this.mu = 3.9860047e14;
            final double ae = 6.378137e6;
            final double c20 = -1.08263e-3;
            final double c30 = 2.54e-6;
            final double c40 = 1.62e-6;
            final double c50 = 2.3e-7;
            final double c60 = -5.5e-7;
            final Vector3D position = new Vector3D(-6142438.668, 3492467.560, -25767.25680);
            final Vector3D velocity = new Vector3D(505.8479685, 942.7809215, 7435.922231);
            this.iniDate = new AbsoluteDate(1969, 7, 28, 4, 0, 0.0, TimeScalesFactory.getTT());
            final Orbit orbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
                FramesFactory.getEME2000(), this.iniDate, this.mu);
            this.propagator =
                new EcksteinHechlerPropagator(orbit, ae, this.mu, orbit.getFrame(), c20, c30, c40, c50, c60,
                    ParametersType.OSCULATING);
            this.log = new ArrayList<EventEntry>();
        } catch (final PropagationException pe) {
            Assert.fail(pe.getLocalizedMessage());
        }
    }

    @After
    public void tearDown() {
        this.iniDate = null;
        this.propagator = null;
        this.log = null;
    }

    private static class EventEntry {

        private final double dt;
        private double expectedDT;
        private final double tolerance;
        private final String name;

        public EventEntry(final double dt, final double tolerance, final String name) {
            this.dt = dt;
            this.expectedDT = Double.NaN;
            this.tolerance = tolerance;
            this.name = name;
        }

    }

    /*
     * ****
     * The unit tests below are to be merged within Orekit eventually.
     * Consider handling the SIRIUS specific data in some proper way.
     * ****
     */

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validation of the event shifter
         * 
         * @featureDescription Validation of the event shifter
         * 
         * @coveredRequirements DV-INTEG_70
         */
        VALIDATION_EVENT_SHIFTER;
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATION_EVENT_SHIFTER}
     * 
     * @testedMethod {@link EventShifter#getIncreasingTimeShift()}
     * @testedMethod {@link EventShifter#getDecreasingTimeShift()}
     * 
     * @description test for code coverage of the simple getters
     * 
     * @input constructor parameters for an EventShifter
     * 
     * @output outputs of the simple getters
     * 
     * @testPassCriteria outputs of the simple getters as expected
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should never happen
     */
    @Test
    public final void testEventShifterSimpleGetters() throws PatriusException {
        final double incr = -15;
        final double decr = -20;
        final EventShifter evtShifter =
            new EventShifter(this.createRawDetector("shifted increasing", "shifted decreasing",
                1.0e-3),
                true, incr, decr);
        // simple getters' tests
        Assert.assertEquals(incr, -evtShifter.getIncreasingTimeShift(), 0.);
        Assert.assertEquals(decr, -evtShifter.getDecreasingTimeShift(), 0.);
    }
}
