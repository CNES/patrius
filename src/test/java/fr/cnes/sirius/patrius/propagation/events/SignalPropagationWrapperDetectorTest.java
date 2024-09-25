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
 * HISTORY
 * VERSION:4.13.1:FA:FA-177:17/01/2024:[PATRIUS] Reliquat OPENFD
 * VERSION:4.13:DM:DM-3:08/12/2023:[PATRIUS] Distinction entre corps celestes et barycentres
 * VERSION:4.13:DM:DM-37:08/12/2023:[PATRIUS] Date d'evenement et propagation du signal
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.CelestialPoint;
import fr.cnes.sirius.patrius.events.EventDetector;
import fr.cnes.sirius.patrius.events.EventDetector.Action;
import fr.cnes.sirius.patrius.events.detectors.AbstractSignalPropagationDetector;
import fr.cnes.sirius.patrius.events.detectors.AbstractSignalPropagationDetector.DatationChoice;
import fr.cnes.sirius.patrius.events.detectors.AbstractSignalPropagationDetector.PropagationDelayType;
import fr.cnes.sirius.patrius.events.detectors.SolarTimeAngleDetector;
import fr.cnes.sirius.patrius.events.postprocessing.EventsLogger;
import fr.cnes.sirius.patrius.events.postprocessing.EventsLogger.LoggedEvent;
import fr.cnes.sirius.patrius.events.utils.SignalPropagationWrapperDetector;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit test class for the {@link SignalPropagationWrapperDetector} class.
 *
 * @author bonitt
 */
public class SignalPropagationWrapperDetectorTest {

    /**
     * @throws PatriusException
     *         if an error occurs
     * @description Builds a new instance and tests the basic getters.
     *
     * @testedMethod {@link SignalPropagationWrapperDetector#SignalPropagationWrapperDetector(AbstractSignalPropagationDetector)}
     * @testedMethod {@link SignalPropagationWrapperDetector#init(SpacecraftState, AbsoluteDate)}
     * @testedMethod {@link SignalPropagationWrapperDetector#g(SpacecraftState)}
     * @testedMethod {@link SignalPropagationWrapperDetector#shouldBeRemoved()}
     * @testedMethod {@link SignalPropagationWrapperDetector#filterEvent(SpacecraftState, boolean, boolean)}
     * @testedMethod {@link SignalPropagationWrapperDetector#getThreshold()}
     * @testedMethod {@link SignalPropagationWrapperDetector#getMaxCheckInterval()}
     * @testedMethod {@link SignalPropagationWrapperDetector#getMaxIterationCount()}
     * @testedMethod {@link SignalPropagationWrapperDetector#getSlopeSelection()}
     * @testedMethod {@link SignalPropagationWrapperDetector#getNBOccurredEvents()}
     * @testedMethod {@link SignalPropagationWrapperDetector#getDatationChoice()}
     * @testedMethod {@link SignalPropagationWrapperDetector#getWrappedDetector()}
     * @testedMethod {@link SignalPropagationWrapperDetector#getEmitterDatesMap()}
     * @testedMethod {@link SignalPropagationWrapperDetector#getEmitterDatesList()}
     * @testedMethod {@link SignalPropagationWrapperDetector#getReceiverDatesMap()}
     * @testedMethod {@link SignalPropagationWrapperDetector#getReceiverDatesList()}
     * @testedMethod {@link SignalPropagationWrapperDetector#toString()}
     * 
     * @testPassCriteria The instance is build without error and the basic getters return the expected data.
     */
    @Test
    public void testFeatures() throws PatriusException {

        CelestialBodyFactory.clearCelestialBodyLoaders();
        Utils.setDataRoot("regular-dataCNES-2003");

        final AbsoluteDate iniDate = AbsoluteDate.J2000_EPOCH;
        final Orbit orbit = new CircularOrbit(9000000, 0, 0, 0.3, 0, FastMath.PI / 2,
            PositionAngle.TRUE, FramesFactory.getGCRF(), iniDate, Constants.CNES_STELA_MU);
        final CelestialPoint sun = CelestialBodyFactory.getSun();

        // Build the reference event detector
        final AbstractSignalPropagationDetector eventDetector =
            new SolarTimeAngleDetector(0, sun, 100, 0.001, Action.CONTINUE, false);

        // Wrap this event detector
        final SignalPropagationWrapperDetector wrapper = new SignalPropagationWrapperDetector(eventDetector);

        // Add it in the propagator, then propagate
        final Propagator propagator = new KeplerianPropagator(orbit);
        final SpacecraftState initialState = propagator.getInitialState();
        propagator.addEventDetector(wrapper);
        propagator.propagate(iniDate.shiftedBy(4 * 3600.));

        // Evaluate the wrapper methods
        Assert.assertEquals(eventDetector.g(initialState), wrapper.g(initialState), 0.);
        Assert.assertEquals(eventDetector.shouldBeRemoved(), wrapper.shouldBeRemoved());
        Assert.assertEquals(eventDetector.filterEvent(initialState, true, true),
            wrapper.filterEvent(initialState, true, true));
        Assert.assertEquals(eventDetector.getThreshold(), wrapper.getThreshold(), 0.);
        Assert.assertEquals(eventDetector.getMaxCheckInterval(), wrapper.getMaxCheckInterval(), 0.);
        Assert.assertEquals(eventDetector.getMaxIterationCount(), wrapper.getMaxIterationCount(), 0.);
        Assert.assertEquals(eventDetector.getSlopeSelection(), wrapper.getSlopeSelection(), 0.);

        Assert.assertEquals(2, wrapper.getNBOccurredEvents());
        Assert.assertEquals(eventDetector.getDatationChoice(), wrapper.getDatationChoice());
        Assert.assertEquals(eventDetector, wrapper.getWrappedDetector());

        Assert.assertEquals(2, wrapper.getEmitterDatesMap().size());
        Assert.assertEquals(2, wrapper.getEmitterDatesList().size());
        Assert.assertEquals(2, wrapper.getReceiverDatesMap().size());
        Assert.assertEquals(2, wrapper.getReceiverDatesList().size());

        String expectedTxt = "SignalPropagationWrapperDetector: [Wrapped event: SolarTimeAngleDetector ; Occurred events: 2]\n"
                + "                                  Event 1: [emitterDate: 2000-01-01T13:14:24.048 ; receiverDate: 2000-01-01T13:14:24.048]\n"
                + "                                  Event 2: [emitterDate: 2000-01-01T15:36:03.606 ; receiverDate: 2000-01-01T15:36:03.606]\n";
        Assert.assertEquals(expectedTxt, wrapper.toString());

        /*
         * The following methods are already evaluated directly by the event detectors:
         * - init(SpacecraftState, AbsoluteDate)
         * - eventOccurred(SpacecraftState, boolean, boolean)
         * - copy()
         */

        // Reset the wrapper and re-evaluate it
        wrapper.init(initialState, iniDate);
        Assert.assertEquals(0, wrapper.getNBOccurredEvents());
        expectedTxt = "SignalPropagationWrapperDetector: [Wrapped event: SolarTimeAngleDetector ; Occurred events: 0]\n";
        Assert.assertEquals(expectedTxt, wrapper.toString());
    }

    @Test
    public void testEventsLogger() throws PatriusException {

        CelestialBodyFactory.clearCelestialBodyLoaders();
        Utils.setDataRoot("regular-dataCNES-2003");

        final AbsoluteDate iniDate = AbsoluteDate.J2000_EPOCH;
        final Frame gcrf = FramesFactory.getGCRF();
        final Orbit orbit = new CircularOrbit(9000000, 0, 0, 0.3, 0, FastMath.PI / 2,
            PositionAngle.TRUE, gcrf, iniDate, Constants.CNES_STELA_MU);
        final CelestialPoint sun = CelestialBodyFactory.getSun();

        // Build the reference event detector
        final AbstractSignalPropagationDetector eventDetector =
            new SolarTimeAngleDetector(0, sun, 100, 0.001, Action.CONTINUE, false);
        eventDetector.setPropagationDelayType(PropagationDelayType.LIGHT_SPEED, gcrf);

        // Wrap this event detector
        final SignalPropagationWrapperDetector wrapper = new SignalPropagationWrapperDetector(eventDetector);

        final EventsLogger logger = new EventsLogger();
        final EventDetector logEvent = logger.monitorDetector(wrapper);

        // Add it in the propagator, then propagate
        final Propagator propagator = new KeplerianPropagator(orbit);
        propagator.getInitialState();
        propagator.addEventDetector(logEvent);
        propagator.propagate(iniDate.shiftedBy(4 * 3600.));

        final List<LoggedEvent> loggedEvent = logger.getLoggedEvents();

        /*
         * Expected:
         * - state: 2000-01-01T13:14:23.910
         * - LoggedEvent 1 : [emitterDate: 2000-01-01T13:06:13.255 ; receiverDate: 2000-01-01T13:14:23.910]
         * - LoggedEvent 2 : [emitterDate: 2000-01-01T15:27:52.814 ; receiverDate: 2000-01-01T15:36:03.469]
         */

        Assert.assertEquals(2, loggedEvent.size());
        Assert.assertTrue(new AbsoluteDate("2000-01-01T13:06:13.255")
            .equals(loggedEvent.get(0).getEventDate(DatationChoice.EMITTER), 1e-3));
        Assert.assertTrue(new AbsoluteDate("2000-01-01T13:14:23.910")
            .equals(loggedEvent.get(0).getEventDate(DatationChoice.RECEIVER), 1e-3));
        Assert.assertTrue(new AbsoluteDate("2000-01-01T15:27:52.814")
            .equals(loggedEvent.get(1).getEventDate(DatationChoice.EMITTER), 1e-3));
        Assert.assertTrue(new AbsoluteDate("2000-01-01T15:36:03.469")
            .equals(loggedEvent.get(1).getEventDate(DatationChoice.RECEIVER), 1e-3));
    }
}
