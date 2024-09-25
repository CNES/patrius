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
 *
 * HISTORY
 * VERSION:4.13.1:FA:FA-177:17/01/2024:[PATRIUS] Reliquat OPENFD
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.13:FA:FA-118:08/12/2023:[PATRIUS] Calcul d'union de PyramidalField invalide
 * VERSION:4.13:DM:DM-37:08/12/2023:[PATRIUS] Date d'evenement et propagation du signal
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.BodyCenterPointing;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.events.EventDetector;
import fr.cnes.sirius.patrius.events.EventDetector.Action;
import fr.cnes.sirius.patrius.events.detectors.AbstractSignalPropagationDetector.DatationChoice;
import fr.cnes.sirius.patrius.events.detectors.AbstractSignalPropagationDetector.PropagationDelayType;
import fr.cnes.sirius.patrius.events.detectors.DihedralFieldOfViewDetector;
import fr.cnes.sirius.patrius.events.utils.SignalPropagationWrapperDetector;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.EquinoctialOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class DihedralFieldOfViewDetectorTest {

    // Body mu
    private double mu;

    // Computation date
    private AbsoluteDate initDate;

    // Orbit
    private Orbit initialOrbit;

    // Reference frame = ITRF 2005
    private Frame itrf;

    // Earth center pointing attitude provider
    private BodyCenterPointing earthCenterAttitudeLaw;

    @Test
    public void testDihedralFielOfView() throws PatriusException {

        // Definition of initial conditions with position and velocity
        // ------------------------------------------------------------

        // Extrapolator definition
        final KeplerianPropagator propagator = new KeplerianPropagator(this.initialOrbit, this.earthCenterAttitudeLaw);

        // Event definition : circular field of view, along X axis, aperture 35°
        final double maxCheck = 1.;
        final PVCoordinatesProvider sunPV = CelestialBodyFactory.getSun();
        final Vector3D center = Vector3D.MINUS_J;
        final Vector3D axis1 = Vector3D.PLUS_K;
        final Vector3D axis2 = Vector3D.PLUS_I;
        final double aperture1 = MathLib.toRadians(28);
        final double aperture2 = MathLib.toRadians(120);

        final EventDetector sunVisi = new DihedralSunVisiDetector(maxCheck, sunPV, center, axis1, aperture1, axis2,
            aperture2, Action.CONTINUE, Action.CONTINUE);

        // Add event to be detected
        propagator.addEventDetector(sunVisi);

        // Extrapolate from the initial to the final date
        propagator.propagate(this.initDate.shiftedBy(6000.));

        // Define new constructor
        final EventDetector sunVisi2 = new DihedralFieldOfViewDetector(sunPV, center, axis1, aperture1, axis2,
            aperture2, maxCheck);
        final EventDetector sunVisi3 = new DihedralFieldOfViewDetector(sunPV, center, axis1, aperture1, axis2,
            aperture2, maxCheck, 1.0E-15);
        final DihedralFieldOfViewDetector detector2 = (DihedralFieldOfViewDetector) sunVisi3.copy();
        final SpacecraftState s = new SpacecraftState(this.initialOrbit);
        Assert.assertEquals(Action.CONTINUE, sunVisi2.eventOccurred(s, true, true));
        Assert.assertEquals(Action.CONTINUE, sunVisi2.eventOccurred(s, true, false));
        Assert.assertEquals(Action.STOP, sunVisi2.eventOccurred(s, false, true));
        Assert.assertEquals(Action.STOP, sunVisi2.eventOccurred(s, false, false));
        Assert.assertEquals(Action.CONTINUE, detector2.eventOccurred(s, true, true));
        Assert.assertEquals(Action.CONTINUE, detector2.eventOccurred(s, true, false));
        Assert.assertEquals(Action.STOP, detector2.eventOccurred(s, false, true));
        Assert.assertEquals(Action.STOP, detector2.eventOccurred(s, false, false));
    }

    /**
     * @description Test this event detector wrap feature in {@link SignalPropagationWrapperDetector}
     * 
     * @input this event detector in INSTANTANEOUS & LIGHT_SPEED
     * 
     * @output the emitter & receiver dates
     * 
     * @testPassCriteria The results containers as expected (non regression)
     * 
     * @referenceVersion 4.13
     * 
     * @nonRegressionVersion 4.13
     */
    @Test
    public void testSignalPropagationWrapperDetector() throws PatriusException {

        // Build two identical event detectors (the first in INSTANTANEOUS, the second in LIGHT_SPEED)
        final CelestialBody sun = CelestialBodyFactory.getSun();
        final DihedralFieldOfViewDetector eventDetector1 = new DihedralFieldOfViewDetector(sun, Vector3D.MINUS_J,
            Vector3D.PLUS_K, MathLib.toRadians(28), Vector3D.PLUS_I, MathLib.toRadians(120), 10, Action.CONTINUE,
            Action.CONTINUE);
        final DihedralFieldOfViewDetector eventDetector2 = (DihedralFieldOfViewDetector) eventDetector1.copy();
        eventDetector2.setPropagationDelayType(PropagationDelayType.LIGHT_SPEED, FramesFactory.getGCRF());

        // Wrap these event detectors
        final SignalPropagationWrapperDetector wrapper1 = new SignalPropagationWrapperDetector(eventDetector1);
        final SignalPropagationWrapperDetector wrapper2 = new SignalPropagationWrapperDetector(eventDetector2);

        // Add them in the propagator, then propagate
        final KeplerianPropagator propagator = new KeplerianPropagator(this.initialOrbit, this.earthCenterAttitudeLaw);
        propagator.addEventDetector(wrapper1);
        propagator.addEventDetector(wrapper2);

        final SpacecraftState finalState = propagator.propagate(this.initDate.shiftedBy(7000));

        // Evaluate the first event detector wrapper (INSTANTANEOUS) (emitter dates should be equal to receiver dates)
        Assert.assertEquals(2, wrapper1.getNBOccurredEvents());
        Assert.assertTrue(wrapper1.getEmitterDatesList().get(0)
            .equals(new AbsoluteDate("1969-08-28T01:07:58.537"), 1e-3));
        Assert.assertTrue(wrapper1.getReceiverDatesList().get(0)
            .equals(new AbsoluteDate("1969-08-28T01:07:58.537"), 1e-3));
        Assert.assertTrue(wrapper1.getEmitterDatesList().get(1)
            .equals(new AbsoluteDate("1969-08-28T01:52:09.164"), 1e-3));
        Assert.assertTrue(wrapper1.getReceiverDatesList().get(1)
            .equals(new AbsoluteDate("1969-08-28T01:52:09.164"), 1e-3));

        // Evaluate the second event detector wrapper (LIGHT_SPEED) (emitter dates should be before receiver dates)
        Assert.assertEquals(2, wrapper1.getNBOccurredEvents());
        Assert.assertTrue(wrapper2.getEmitterDatesList().get(0)
            .equals(new AbsoluteDate("1969-08-28T00:59:34.159"), 1e-3));
        Assert.assertTrue(wrapper2.getReceiverDatesList().get(0)
            .equals(new AbsoluteDate("1969-08-28T01:07:58.124"), 1e-3));
        Assert.assertTrue(wrapper2.getEmitterDatesList().get(1)
            .equals(new AbsoluteDate("1969-08-28T01:43:44.732"), 1e-3));
        Assert.assertTrue(wrapper2.getReceiverDatesList().get(1)
            .equals(new AbsoluteDate("1969-08-28T01:52:08.691"), 1e-3));

        // Evaluate the AbstractSignalPropagationDetector's abstract methods implementation
        Assert.assertEquals(sun, eventDetector1.getEmitter(null));
        Assert.assertEquals(finalState.getOrbit(), eventDetector1.getReceiver(finalState));
        Assert.assertEquals(DatationChoice.RECEIVER, eventDetector1.getDatationChoice());
    }

    @Before
    public void setUp() {
        try {

            Utils.setDataRoot("regular-data");
            FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

            // Computation date
            // Satellite position as circular parameters
            this.mu = 3.9860047e14;

            this.initDate = new AbsoluteDate(new DateComponents(1969, 8, 28),
                TimeComponents.H00,
                TimeScalesFactory.getUTC());

            final Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
            final Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
            this.initialOrbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
                FramesFactory.getEME2000(), this.initDate, this.mu);

            // Reference frame = ITRF 2005
            this.itrf = FramesFactory.getITRF();

            // Create earth center pointing attitude provider */
            this.earthCenterAttitudeLaw = new BodyCenterPointing(this.itrf);

        } catch (final PatriusException oe) {
            Assert.fail(oe.getMessage());
        }

    }

    @After
    public void tearDown() throws PatriusException {
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
    }

    /**
     * Finder for visibility event.
     * <p>
     * This class extends the elevation detector modifying the event handler.
     * <p>
     */
    private static class DihedralSunVisiDetector extends DihedralFieldOfViewDetector {

        /** Serializable UID. */
        private static final long serialVersionUID = 1181779674621070074L;

        public DihedralSunVisiDetector(final double maxCheck,
            final PVCoordinatesProvider pvTarget, final Vector3D center, final Vector3D axis1,
            final double aperture1,
            final Vector3D axis2, final double aperture2, final Action actionEntry, final Action actionExit)
            throws PatriusException {
            super(pvTarget, center, axis1, aperture1, axis2, aperture2, maxCheck, actionEntry, actionExit);
        }

        @Override
        public
                Action
                eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                       throws PatriusException {
            if (increasing) {
                // System.err.println(" Sun visibility starts " + s.getDate());
                final AbsoluteDate startVisiDate = new AbsoluteDate(new DateComponents(1969, 8, 28),
                    new TimeComponents(1, 19, 00.381),
                    TimeScalesFactory.getUTC());

                Assert.assertTrue(s.getDate().durationFrom(startVisiDate) <= 1);
            } else {
                final AbsoluteDate endVisiDate = new AbsoluteDate(new DateComponents(1969, 8, 28),
                    new TimeComponents(1, 39, 42.674),
                    TimeScalesFactory.getUTC());
                Assert.assertTrue(s.getDate().durationFrom(endVisiDate) <= 1);
                // System.err.println(" Sun visibility ends at " + s.getDate());
            }
            return super.eventOccurred(s, increasing, forward);
        }

    }

}
