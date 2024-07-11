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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.cnesmerge.propagation.events;

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
import fr.cnes.sirius.patrius.propagation.events.EclipseDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.events.EventsLogger;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class EventsLoggerTest {

    private double mu;
    private AbsoluteDate iniDate;
    private SpacecraftState initialState;
    private NumericalPropagator propagator;
    private EventDetector umbraDetector;
    private EventDetector penumbraDetector;

    private EventDetector buildDetector(final boolean totalEclipse) throws PatriusException {
        return new EclipseDetector(CelestialBodyFactory.getSun(), 696000000, CelestialBodyFactory.getEarth(), 6400000,
            totalEclipse ? 0 : 1, 60., 1.e-3){

            /** Serializable UID. */
            private static final long serialVersionUID = 456402663128670409L;

            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                return Action.CONTINUE;
            }
        };
    }

    @Before
    public void setUp() {
        try {
            Utils.setDataRoot("regular-dataCNES-2003");
            this.mu = 3.9860047e14;
            final Vector3D position = new Vector3D(-6142438.668, 3492467.560, -25767.25680);
            final Vector3D velocity = new Vector3D(505.8479685, 942.7809215, 7435.922231);
            this.iniDate = new AbsoluteDate(1969, 7, 28, 4, 0, 0.0, TimeScalesFactory.getTT());
            final Orbit orbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
                FramesFactory.getEME2000(), this.iniDate, this.mu);
            this.initialState = new SpacecraftState(orbit);
            final double[] absTolerance = {
                0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6, 0.001
            };
            final double[] relTolerance = {
                1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7, 1.0e-7
            };
            final AdaptiveStepsizeIntegrator integrator =
                new DormandPrince853Integrator(0.001, 1000, absTolerance, relTolerance);
            integrator.setInitialStepSize(60);
            this.propagator = new NumericalPropagator(integrator);
            this.propagator.setInitialState(this.initialState);
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
        this.umbraDetector = null;
        this.penumbraDetector = null;
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
         * @featureTitle Validation of the events logger
         * 
         * @featureDescription Validation of the events logger
         * 
         * @coveredRequirements DV-INTEG_70
         */
        VALIDATION_EVENTS_LOGGER;
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATION_EVENTS_LOGGER}
     * 
     * @testedMethod {@link EventsLogger#monitorDetector(EventDetector)}
     * 
     * @description test for code coverage of the EventsLogger class
     * 
     * @input constructor parameters for a detector
     * 
     * @output EventsLogger and associated detector
     * 
     * @testPassCriteria call on resetState of the detector wrapper successful
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should never happen
     */
    @Test
    public void testLoggingWrapperResetState() throws PatriusException {
        // Test aim : call resetState once on the wrapper
        final EventsLogger logger = new EventsLogger();
        final EventDetector wrapper = logger.monitorDetector(this.umbraDetector);
        this.propagator.addEventDetector(wrapper);
        final SpacecraftState oldState = this.propagator.getInitialState();
        final SpacecraftState oldStateAgain = wrapper.resetState(oldState);
        // Obvious result
        Assert.assertEquals(oldStateAgain, oldState);
    }
}
