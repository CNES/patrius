/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */
/*
 *
 * HISTORY
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 *                             (added forward parameter to eventOccurred signature)
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.orbits.EquinoctialOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class EventsLoggerTest {

    private double mu;
    private AbsoluteDate iniDate;
    private SpacecraftState initialState;
    private NumericalPropagator propagator;
    private int count;
    private EventDetector umbraDetector;
    private EventDetector penumbraDetector;

    @Test
    public void testLogUmbra() throws PatriusException {
        final EventsLogger logger = new EventsLogger();
        this.propagator.addEventDetector(logger.monitorDetector(this.umbraDetector));
        this.propagator.addEventDetector(this.penumbraDetector);
        this.count = 0;
        this.propagator.propagate(this.iniDate.shiftedBy(16215)).getDate();
        Assert.assertEquals(11, this.count);
        this.checkCounts(logger, 3, 3, 0, 0);
    }

    @Test
    public void testLogPenumbra() throws PatriusException {
        final EventsLogger logger = new EventsLogger();
        this.propagator.addEventDetector(this.umbraDetector);
        this.propagator.addEventDetector(logger.monitorDetector(this.penumbraDetector));
        this.count = 0;
        this.propagator.propagate(this.iniDate.shiftedBy(16215)).getDate();
        Assert.assertEquals(11, this.count);
        this.checkCounts(logger, 0, 0, 2, 3);
    }

    @Test
    public void testLogAll() throws PatriusException {
        final EventsLogger logger = new EventsLogger();
        this.propagator.addEventDetector(logger.monitorDetector(this.umbraDetector));
        this.propagator.addEventDetector(logger.monitorDetector(this.penumbraDetector));
        this.count = 0;
        this.propagator.propagate(this.iniDate.shiftedBy(16215));
        Assert.assertEquals(11, this.count);
        this.checkCounts(logger, 3, 3, 2, 3);
    }

    @Test
    public void testImmutableList() throws PatriusException {
        final EventsLogger logger = new EventsLogger();
        this.propagator.addEventDetector(logger.monitorDetector(this.umbraDetector));
        this.propagator.addEventDetector(logger.monitorDetector(this.penumbraDetector));
        this.count = 0;
        this.propagator.propagate(this.iniDate.shiftedBy(16215));
        final List<EventsLogger.LoggedEvent> firstList = logger.getLoggedEvents();
        Assert.assertEquals(11, firstList.size());
        this.propagator.propagate(this.iniDate.shiftedBy(30000));
        final List<EventsLogger.LoggedEvent> secondList = logger.getLoggedEvents();
        Assert.assertEquals(11, firstList.size());
        Assert.assertEquals(20, secondList.size());
        for (int i = 0; i < firstList.size(); ++i) {

            final EventsLogger.LoggedEvent e1 = firstList.get(i);
            final EventsLogger.LoggedEvent e2 = secondList.get(i);
            final PVCoordinates pv1 = e1.getState().getPVCoordinates();
            final PVCoordinates pv2 = e2.getState().getPVCoordinates();

            Assert.assertTrue(e1.getEventDetector() == e2.getEventDetector());
            Assert.assertEquals(0, pv1.getPosition().subtract(pv2.getPosition()).getNorm(), 1.0e-10);
            Assert.assertEquals(0, pv1.getVelocity().subtract(pv2.getVelocity()).getNorm(), 1.0e-10);
            Assert.assertEquals(e1.isIncreasing(), e2.isIncreasing());

        }
    }

    @Test
    public void testClearLog() throws PatriusException {
        final EventsLogger logger = new EventsLogger();
        this.propagator.addEventDetector(logger.monitorDetector(this.umbraDetector));
        this.propagator.addEventDetector(logger.monitorDetector(this.penumbraDetector));
        this.count = 0;
        this.propagator.propagate(this.iniDate.shiftedBy(16215));
        final List<EventsLogger.LoggedEvent> firstList = logger.getLoggedEvents();
        Assert.assertEquals(11, firstList.size());
        logger.clearLoggedEvents();
        this.propagator.propagate(this.iniDate.shiftedBy(30000));
        final List<EventsLogger.LoggedEvent> secondList = logger.getLoggedEvents();
        Assert.assertEquals(11, firstList.size());
        Assert.assertEquals(9, secondList.size());
    }

    private void checkCounts(final EventsLogger logger,
                             final int expectedUmbraIncreasingCount, final int expectedUmbraDecreasingCount,
                             final int expectedPenumbraIncreasingCount, final int expectedPenumbraDecreasingCount) {
        int umbraIncreasingCount = 0;
        int umbraDecreasingCount = 0;
        int penumbraIncreasingCount = 0;
        int penumbraDecreasingCount = 0;
        for (final EventsLogger.LoggedEvent event : logger.getLoggedEvents()) {
            if (event.getEventDetector() == this.umbraDetector) {
                if (event.isIncreasing()) {
                    ++umbraIncreasingCount;
                } else {
                    ++umbraDecreasingCount;
                }
            }
            if (event.getEventDetector() == this.penumbraDetector) {
                if (event.isIncreasing()) {
                    ++penumbraIncreasingCount;
                } else {
                    ++penumbraDecreasingCount;
                }
            }
        }
        Assert.assertEquals(expectedUmbraIncreasingCount, umbraIncreasingCount);
        Assert.assertEquals(expectedUmbraDecreasingCount, umbraDecreasingCount);
        Assert.assertEquals(expectedPenumbraIncreasingCount, penumbraIncreasingCount);
        Assert.assertEquals(expectedPenumbraDecreasingCount, penumbraDecreasingCount);
    }

    private EventDetector buildDetector(final boolean totalEclipse) throws PatriusException {
        return new EclipseDetector(CelestialBodyFactory.getSun(), 696000000, CelestialBodyFactory.getEarth(), 6400000,
            totalEclipse ? 0 : 1, 60., 1.e-3){
            private static final long serialVersionUID = 1L;

            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                ++EventsLoggerTest.this.count;
                return Action.CONTINUE;
            }
        };
    }

    @Before
    public void setUp() {
        try {
            Utils.setDataRoot("regular-data");
            this.mu = 3.9860047e14;
            final Vector3D position = new Vector3D(-6142438.668, 3492467.560, -25767.25680);
            final Vector3D velocity = new Vector3D(505.8479685, 942.7809215, 7435.922231);
            this.iniDate = new AbsoluteDate(1969, 7, 28, 4, 0, 0.0, TimeScalesFactory.getTT());
            final Orbit orbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
                FramesFactory.getEME2000(), this.iniDate, this.mu);
            this.initialState = new SpacecraftState(orbit);
            final double[] absTolerance = {
                0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6
            };
            final double[] relTolerance = {
                1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7
            };
            final AdaptiveStepsizeIntegrator integrator =
                new DormandPrince853Integrator(0.001, 1000, absTolerance, relTolerance);
            integrator.setInitialStepSize(60);
            this.propagator = new NumericalPropagator(integrator);
            this.propagator.setInitialState(this.initialState);
            this.count = 0;
            this.umbraDetector = this.buildDetector(true);
            this.penumbraDetector = this.buildDetector(false);
        } catch (final PatriusException oe) {
            Assert.fail(oe.getLocalizedMessage());
        }
    }

    @After
    public void tearDown() {
        this.iniDate = null;
        this.initialState = null;
        this.propagator = null;
        this.count = 0;
        this.umbraDetector = null;
        this.penumbraDetector = null;
    }

}
