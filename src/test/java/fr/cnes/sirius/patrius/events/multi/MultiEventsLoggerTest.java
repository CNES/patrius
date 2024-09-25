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
 * @history created 18/03/2015
 *
 * HISTORY
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:300:18/03/2015:Creation multi propagator
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events.multi;

import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.events.EventDetector;
import fr.cnes.sirius.patrius.events.MultiEventDetector;
import fr.cnes.sirius.patrius.events.detectors.EclipseDetector;
import fr.cnes.sirius.patrius.events.postprocessing.MultiEventsLogger;
import fr.cnes.sirius.patrius.events.utils.OneSatEventDetectorWrapper;
import fr.cnes.sirius.patrius.forces.gravity.DirectBodyAttraction;
import fr.cnes.sirius.patrius.forces.gravity.NewtonianGravityModel;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.orbits.EquinoctialOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.multi.MultiNumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * Test class {@link MultiEventsLogger}
 * </p>
 * <p>
 * This test class is copied from {@link fr.cnes.sirius.patrius.events.CodedEventsLoggerTest}
 * </p>
 * 
 * @author maggioranic
 * 
 * @version $Id$
 * 
 * @since 3.0
 * 
 */
public class MultiEventsLoggerTest {

    private static final String STATE1 = "state1";
    private double mu;
    private AbsoluteDate iniDate;
    private SpacecraftState initialState;
    private MultiNumericalPropagator propagator;
    private int count;
    private MultiEventDetector umbraDetector;
    private MultiEventDetector penumbraDetector;

    @Test
    public void testLogUmbra() throws PatriusException {
        final MultiEventsLogger logger = new MultiEventsLogger();
        this.propagator.addEventDetector(logger.monitorDetector(this.umbraDetector));
        this.propagator.addEventDetector(this.penumbraDetector);
        this.count = 0;
        propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(propagator.getInitialStates()
            .get(STATE1).getMu())), STATE1);
        this.propagator.propagate(this.iniDate.shiftedBy(16215)).get(STATE1).getDate();
        Assert.assertEquals(11, this.count);
        this.checkCounts(logger, 3, 3, 0, 0);
    }

    @Test
    public void testLogPenumbra() throws PatriusException {
        propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(propagator.getInitialStates()
            .get(STATE1).getMu())), STATE1);
        final MultiEventsLogger logger = new MultiEventsLogger();
        this.propagator.addEventDetector(this.umbraDetector);
        this.propagator.addEventDetector(logger.monitorDetector(this.penumbraDetector));
        this.count = 0;
        this.propagator.propagate(this.iniDate.shiftedBy(16215)).get(STATE1).getDate();
        Assert.assertEquals(11, this.count);
        this.checkCounts(logger, 0, 0, 2, 3);
    }

    @Test
    public void testLogAll() throws PatriusException {
        propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(propagator.getInitialStates()
            .get(STATE1).getMu())), STATE1);
        final MultiEventsLogger logger = new MultiEventsLogger();
        this.propagator.addEventDetector(logger.monitorDetector(this.umbraDetector));
        this.propagator.addEventDetector(logger.monitorDetector(this.penumbraDetector));
        this.count = 0;
        this.propagator.propagate(this.iniDate.shiftedBy(16215));
        Assert.assertEquals(11, this.count);
        this.checkCounts(logger, 3, 3, 2, 3);
    }

    @Test
    public void testImmutableList() throws PatriusException {
        propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(propagator.getInitialStates()
            .get(STATE1).getMu())), STATE1);
        final MultiEventsLogger logger = new MultiEventsLogger();
        this.propagator.addEventDetector(logger.monitorDetector(this.umbraDetector));
        this.propagator.addEventDetector(logger.monitorDetector(this.penumbraDetector));
        this.count = 0;
        this.propagator.propagate(this.iniDate.shiftedBy(16215));
        final List<MultiEventsLogger.MultiLoggedEvent> firstList = logger.getLoggedEvents();
        Assert.assertEquals(11, firstList.size());
        this.propagator.propagate(this.iniDate.shiftedBy(30000));
        final List<MultiEventsLogger.MultiLoggedEvent> secondList = logger.getLoggedEvents();
        Assert.assertEquals(11, firstList.size());
        Assert.assertEquals(20, secondList.size());
        for (int i = 0; i < firstList.size(); ++i) {

            final MultiEventsLogger.MultiLoggedEvent e1 = firstList.get(i);
            final MultiEventsLogger.MultiLoggedEvent e2 = secondList.get(i);
            final PVCoordinates pv1 = e1.getStates().get(STATE1).getPVCoordinates();
            final PVCoordinates pv2 = e2.getStates().get(STATE1).getPVCoordinates();

            Assert.assertTrue(e1.getEventDetector() == e2.getEventDetector());
            Assert.assertEquals(0, pv1.getPosition().subtract(pv2.getPosition()).getNorm(), 1.0e-10);
            Assert.assertEquals(0, pv1.getVelocity().subtract(pv2.getVelocity()).getNorm(), 1.0e-10);
            Assert.assertEquals(e1.isIncreasing(), e2.isIncreasing());

        }
    }

    @Test
    public void testClearLog() throws PatriusException {
        propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(propagator.getInitialStates()
            .get(STATE1).getMu())), STATE1);
        final MultiEventsLogger logger = new MultiEventsLogger();
        this.propagator.addEventDetector(logger.monitorDetector(this.umbraDetector));
        this.propagator.addEventDetector(logger.monitorDetector(this.penumbraDetector));
        this.count = 0;
        this.propagator.propagate(this.iniDate.shiftedBy(16215));
        final List<MultiEventsLogger.MultiLoggedEvent> firstList = logger.getLoggedEvents();
        Assert.assertEquals(11, firstList.size());
        logger.clearLoggedEvents();
        this.propagator.propagate(this.iniDate.shiftedBy(30000));
        final List<MultiEventsLogger.MultiLoggedEvent> secondList = logger.getLoggedEvents();
        Assert.assertEquals(11, firstList.size());
        Assert.assertEquals(9, secondList.size());
    }

    private void checkCounts(final MultiEventsLogger logger,
                             final int expectedUmbraIncreasingCount, final int expectedUmbraDecreasingCount,
                             final int expectedPenumbraIncreasingCount, final int expectedPenumbraDecreasingCount) {
        int umbraIncreasingCount = 0;
        int umbraDecreasingCount = 0;
        int penumbraIncreasingCount = 0;
        int penumbraDecreasingCount = 0;
        for (final MultiEventsLogger.MultiLoggedEvent event : logger.getLoggedEvents()) {
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

            /** Serializable UID. */
            private static final long serialVersionUID = -1979273088227042502L;

            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                ++MultiEventsLoggerTest.this.count;
                return Action.CONTINUE;
            }
        };
    }

    @Before
    public void setUp() {
        try {
            Utils.setDataRoot("regular-dataPBASE");
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
            this.propagator = new MultiNumericalPropagator(integrator);
            this.propagator.addInitialState(this.initialState, STATE1);
            this.count = 0;
            this.umbraDetector = new OneSatEventDetectorWrapper(this.buildDetector(true), STATE1);
            this.penumbraDetector = new OneSatEventDetectorWrapper(this.buildDetector(false), STATE1);
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
