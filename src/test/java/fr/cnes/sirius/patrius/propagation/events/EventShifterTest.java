/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */
/*
 * 
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:262:29/04/2014:Removed standard gravitational parameter from constructor
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 *                             (added forward parameter to eventOccurred signature)
 * VERSION::DM:368:20/03/2015:Eckstein-Heschler : Back at the "mu"
 * VERSION::DM:480:15/02/2016: new analytical propagators and mean/osculating conversion
 * VERSION::DM:489:17/12/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION::FA:1777:04/10/2018:correct ICRF parent frame
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

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
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.EquinoctialOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.ParametersType;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.EcksteinHechlerPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

public class EventShifterTest {

    private double mu;
    private AbsoluteDate iniDate;
    private Propagator propagator;
    private List<EventEntry> log;

    private final double sunRadius = 696000000.;
    private final double earthRadius = 6400000.;

    @Test
    public void testNegNeg() throws PatriusException {
        this.propagator.addEventDetector(this.createRawDetector("raw increasing", "raw decreasing", 1.0e-9));
        this.propagator.addEventDetector(new EventShifter(this.createRawDetector("shifted increasing",
            "shifted decreasing",
            1.0e-3),
            true, -15, -20));
        this.propagator.addEventDetector(new EventShifter(this.createRawDetector("unshifted increasing",
            "unshifted decreasing",
            1.0e-3),
            false, -5, -10));
        this.propagator.propagate(this.iniDate.shiftedBy(6000));
        Assert.assertEquals(6, this.log.size());
        this.log.get(0).checkExpected(2280.238432465, "shifted decreasing");
        this.log.get(1).checkExpected(2300.238432465, "unshifted decreasing");
        this.log.get(2).checkExpected(2300.2384370846935, "raw decreasing");
        this.log.get(3).checkExpected(4361.986163327, "shifted increasing");
        this.log.get(4).checkExpected(4376.986163327, "unshifted increasing");
        this.log.get(5).checkExpected(4376.986183794259, "raw increasing");
    }

    @Test
    public void testNegPos() throws PatriusException {
        this.propagator.addEventDetector(this.createRawDetector("raw increasing", "raw decreasing", 1.0e-9));
        this.propagator.addEventDetector(new EventShifter(this.createRawDetector("shifted increasing",
            "shifted decreasing",
            1.0e-3),
            true, -15, 20));
        this.propagator.addEventDetector(new EventShifter(this.createRawDetector("unshifted increasing",
            "unshifted decreasing",
            1.0e-3),
            false, -5, 10));
        this.propagator.propagate(this.iniDate.shiftedBy(6000));
        Assert.assertEquals(6, this.log.size());
        this.log.get(0).checkExpected(2300.2384370846935, "raw decreasing");
        this.log.get(1).checkExpected(2300.238432465, "unshifted decreasing");
        this.log.get(2).checkExpected(2320.238432465, "shifted decreasing");
        this.log.get(3).checkExpected(4361.986163327, "shifted increasing");
        this.log.get(4).checkExpected(4376.986163327, "unshifted increasing");
        this.log.get(5).checkExpected(4376.986183794259, "raw increasing");
    }

    @Test
    public void testPosNeg() throws PatriusException {
        this.propagator.addEventDetector(this.createRawDetector("raw increasing", "raw decreasing", 1.0e-9));
        this.propagator.addEventDetector(new EventShifter(this.createRawDetector("shifted increasing",
            "shifted decreasing",
            1.0e-3),
            true, 15, -20));
        this.propagator.addEventDetector(new EventShifter(this.createRawDetector("unshifted increasing",
            "unshifted decreasing",
            1.0e-3),
            false, 5, -10));
        this.propagator.propagate(this.iniDate.shiftedBy(6000));
        Assert.assertEquals(6, this.log.size());
        this.log.get(0).checkExpected(2280.238432465, "shifted decreasing");
        this.log.get(1).checkExpected(2300.238432465, "unshifted decreasing");
        this.log.get(2).checkExpected(2300.2384370846935, "raw decreasing");
        this.log.get(3).checkExpected(4376.986183794259, "raw increasing");
        this.log.get(4).checkExpected(4376.986163327, "unshifted increasing");
        this.log.get(5).checkExpected(4391.986163327, "shifted increasing");
    }

    @Test
    public void testPosPos() throws PatriusException {
        this.propagator.addEventDetector(this.createRawDetector("raw increasing", "raw decreasing", 1.0e-9));
        this.propagator.addEventDetector(new EventShifter(this.createRawDetector("shifted increasing",
            "shifted decreasing",
            1.0e-3),
            true, 15, 20));
        this.propagator.addEventDetector(new EventShifter(this.createRawDetector("unshifted increasing",
            "unshifted decreasing",
            1.0e-3),
            false, 5, 10));
        this.propagator.propagate(this.iniDate.shiftedBy(6000));
        Assert.assertEquals(6, this.log.size());
        this.log.get(0).checkExpected(2300.2384370846935, "raw decreasing");
        this.log.get(1).checkExpected(2300.238432465, "unshifted decreasing");
        this.log.get(2).checkExpected(2320.238432465, "shifted decreasing");
        this.log.get(3).checkExpected(4376.986183794259, "raw increasing");
        this.log.get(4).checkExpected(4376.986163327, "unshifted increasing");
        this.log.get(5).checkExpected(4391.986163327, "shifted increasing");
    }

    @Test
    public void testIncreasingError() throws PatriusException {
        this.propagator.addEventDetector(this.createRawDetector("raw increasing", "raw decreasing", 2.0e-9));
        this.propagator.addEventDetector(new EventShifter(this.createRawDetector("-10s increasing", "-10s decreasing",
            2.0e-3),
            true, -10, -10));
        this.propagator.addEventDetector(new EventShifter(this.createRawDetector("-100s increasing",
            "-100s decreasing", 3.0e-2),
            true, -100, -100));
        this.propagator.addEventDetector(new EventShifter(this.createRawDetector("-1000s increasing",
            "-1000s decreasing", 5.0),
            true, -1000, -1000));
        this.propagator.propagate(this.iniDate.shiftedBy(20000));

        // the raw eclipses (not all within the propagation range) are at times:
        // [ 2300.23843246594, 4376.986163326932]
        // [ 8210.85851802963, 10287.572940950127]
        // [14121.478252940502, 16198.159277277191]
        // [20032.097637495113, 22108.745172638683]
        // [25942.716671989547, 28019.330627364776]
        // [31853.335356719457, 33929.91564178527]
        // [ 37763.95369198012, 39840.50021622965]
        Assert.assertEquals(26, this.log.size());
        this.log.get(0).checkExpected(1300.238432465, "-1000s decreasing");
        this.log.get(1).checkExpected(2200.238432465, "-100s decreasing");
        this.log.get(2).checkExpected(2290.238432465, "-10s decreasing");
        this.log.get(3).checkExpected(2300.2384370846935, "raw decreasing");
        this.log.get(4).checkExpected(3376.986163327, "-1000s increasing");
        this.log.get(5).checkExpected(4276.986163327, "-100s increasing");
        this.log.get(6).checkExpected(4366.986163327, "-10s increasing");
        this.log.get(7).checkExpected(4376.986183794306, "raw increasing");
        this.log.get(8).checkExpected(7210.858518030, "-1000s decreasing");
        this.log.get(9).checkExpected(8110.858518030, "-100s decreasing");
        this.log.get(10).checkExpected(8200.858518030, "-10s decreasing");
        this.log.get(11).checkExpected(8210.858523412584, "raw decreasing");
        this.log.get(12).checkExpected(9287.572940950, "-1000s increasing");
        this.log.get(13).checkExpected(10187.572940950, "-100s increasing");
        this.log.get(14).checkExpected(10277.572940950, "-10s increasing");
        this.log.get(15).checkExpected(10287.572964339717, "raw increasing");
        this.log.get(16).checkExpected(13121.478252941, "-1000s decreasing");
        this.log.get(17).checkExpected(14021.478252941, "-100s decreasing");
        this.log.get(18).checkExpected(14111.478252941, "-10s decreasing");
        this.log.get(19).checkExpected(14121.478259085387, "raw decreasing");
        this.log.get(20).checkExpected(15198.159277277, "-1000s increasing");
        this.log.get(21).checkExpected(16098.159277277, "-100s increasing");
        this.log.get(22).checkExpected(16188.159277277, "-10s increasing");
        this.log.get(23).checkExpected(16198.159303586766, "raw increasing");
        this.log.get(24).checkExpected(19032.097637495, "-1000s decreasing");
        this.log.get(25).checkExpected(19932.097637495, "-100s decreasing");

        for (final EventEntry entry : this.log) {
            final double error = entry.getTimeError();
            if (entry.name.contains("10s")) {
                Assert.assertTrue(error > 0.00001);
                Assert.assertTrue(error < 0.0004);
            } else if (entry.name.contains("100s")) {
                Assert.assertTrue(error > 0.001);
                Assert.assertTrue(error < 0.03);
            } else if (entry.name.contains("1000s")) {
                Assert.assertTrue(error > 0.7);
                Assert.assertTrue(error < 3.35);
            }
        }
    }

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
    public void setUp() {
        try {
            Utils.setDataRoot("regular-data");
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

        public void checkExpected(final double expectedDT, final String name) {
            this.expectedDT = expectedDT;
            Assert.assertEquals(expectedDT, this.dt, this.tolerance);
            Assert.assertEquals(name, this.name);
        }

        public double getTimeError() {
            return MathLib.abs(this.dt - this.expectedDT);
        }

    }

}
