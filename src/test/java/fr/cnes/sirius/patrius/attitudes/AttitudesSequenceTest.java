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
 * VERSION:4.5:FA:FA-2341:27/05/2020:Bug lors de la recuperation de l'attitude issue d'une sequence d'attitude 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 *                             (added forward parameter to eventOccurred signature)
 * VERSION::DM:226:12/09/2014: problem with event detections.
 * VERSION::DM:403:20/10/2015:Improving ergonomic
 * VERSION::DM:489:06/10/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:596:12/04/2016:Improve test coherence
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.RotationOrder;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.propagation.events.DateDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class AttitudesSequenceTest {

    /** Computation date. */
    private AbsoluteDate date;

    /** Orbit. */
    private CircularOrbit circ;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(AttitudesSequenceTest.class.getSimpleName(), "Attitude sequence attitude provider");
    }

    /**
     * Test of main AttitudesSequence methods.
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public void testAttitudesSequence() throws PatriusException {

        Report.printMethodHeader("testAttitudesSequence", "Attitude computation", "Math", 0, ComparisonType.ABSOLUTE);

        final Frame gcrf = FramesFactory.getGCRF();

        final AttitudesSequence aseq = new AttitudesSequence();
        // Only for coverage
        aseq.setSpinDerivativesComputation(true);

        final EventDetector switcherToTwo = new DateDetector(this.date.shiftedBy(100)) {

            private static final long serialVersionUID = 2688174304875690100L;

            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                    throws PatriusException {
                return Action.RESET_STATE;
            }
        };
        final EventDetector switcherToOne = new DateDetector(this.date.shiftedBy(200)) {

            private static final long serialVersionUID = 2688174304875690200L;

            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                    throws PatriusException {
                // For coverage
                return Action.RESET_STATE;
            }
        };

        final AttitudeLaw lawOne = new LofOffset(gcrf, LOFType.QSW);
        lawOne.setSpinDerivativesComputation(true);
        final AttitudeLaw lawTwo = new LofOffset(gcrf, LOFType.TNW);
        lawTwo.setSpinDerivativesComputation(true);

        aseq.addSwitchingCondition(lawTwo, switcherToOne, true, true, lawOne);
        aseq.addSwitchingCondition(lawOne, switcherToTwo, true, true, lawTwo);

        aseq.resetActiveProvider(lawOne);

        // Properly register switching events
        final Propagator propagator = new KeplerianPropagator(this.circ, aseq);
        aseq.registerSwitchEvents(propagator);

        Attitude att;
        Attitude attRef;
        attRef = lawOne.getAttitude(this.circ, this.date, gcrf);
        att = aseq.getAttitude(this.circ, this.date, gcrf);
        // Current law is lawOne
        Assert.assertArrayEquals(attRef.getRotation().getAngles(RotationOrder.XYX),
                att.getRotation().getAngles(RotationOrder.XYX), 0.);

        // Propagation
        SpacecraftState sstate;
        sstate = propagator.propagate(this.date.shiftedBy(150));
        attRef = lawTwo.getAttitude(propagator, sstate.getDate(), gcrf);
        att = aseq.getAttitude(propagator, sstate.getDate(), gcrf);
        // Current law is now lawTwo
        Assert.assertArrayEquals(attRef.getRotation().getAngles(RotationOrder.XYX),
                att.getRotation().getAngles(RotationOrder.XYX), 0.);
        // Propagation
        sstate = propagator.propagate(this.date.shiftedBy(300));
        attRef = lawOne.getAttitude(propagator, sstate.getDate(), gcrf);
        att = aseq.getAttitude(propagator, sstate.getDate(), gcrf);
        // Current law is lawOne again
        Assert.assertArrayEquals(attRef.getRotation().getAngles(RotationOrder.XYX),
                att.getRotation().getAngles(RotationOrder.XYX), 0.);

        Report.printToReport("Rotation at date", attRef.getRotation(), att.getRotation());
        Report.printToReport("Spin at date", attRef.getSpin(), att.getSpin());
        Report.printToReport("Spin derivative at date", attRef.getRotationAcceleration(), att.getRotationAcceleration());

        // reset active provider with a law initially not of the map, check that it was added and check rotation
        final AttitudeLaw lawBogus = new LofOffset(gcrf, LOFType.VNC);
        aseq.resetActiveProvider(lawBogus);
        Assert.assertEquals(
                Rotation.distance(aseq.getAttitude(this.circ, this.date, gcrf).getRotation(),
                        lawBogus.getAttitude(this.circ, this.date, gcrf).getRotation()), 0,
                Precision.DOUBLE_COMPARISON_EPSILON);

        Assert.assertNotNull(aseq.toString());
    }

    /**
     * Check that retrieving a past attitude law from the sequence returns the right results.
     */
    @Test
    public void testPastAttitudesSequence() throws PatriusException {

        // Build sequence
        final AttitudesSequence aseq = new AttitudesSequence();
        final EventDetector switcherToTwo = new DateDetector(this.date.shiftedBy(100), 600, 1E-6, Action.RESET_STATE);
        final EventDetector switcherToOne = new DateDetector(this.date.shiftedBy(200), 600, 1E-6, Action.RESET_STATE);
        final AttitudeLaw lawOne = new LofOffset(FramesFactory.getGCRF(), LOFType.QSW);
        final AttitudeLaw lawTwo = new LofOffset(FramesFactory.getGCRF(), LOFType.TNW);
        aseq.addSwitchingCondition(lawTwo, switcherToOne, true, true, lawOne);
        aseq.addSwitchingCondition(lawOne, switcherToTwo, true, true, lawTwo);

        aseq.resetActiveProvider(lawOne);

        // Properly register switching events
        final Propagator propagator = new KeplerianPropagator(this.circ, aseq);
        aseq.registerSwitchEvents(propagator);

        // Propagation
        propagator.propagate(this.date.shiftedBy(500));

        // Check law
        Attitude att;
        Attitude attRef;

        // Law 1 (t < 100s)
        attRef = lawOne.getAttitude(this.circ, this.date, FramesFactory.getGCRF());
        att = aseq.getAttitude(this.circ, this.date, FramesFactory.getGCRF());
        Assert.assertEquals(Rotation.distance(attRef.getRotation(), att.getRotation()), 0., 0.);

        // Law 2 (100s < t < 200s)
        attRef = lawTwo.getAttitude(this.circ, this.date.shiftedBy(101), FramesFactory.getGCRF());
        att = aseq.getAttitude(this.circ, this.date.shiftedBy(101), FramesFactory.getGCRF());
        Assert.assertEquals(Rotation.distance(attRef.getRotation(), att.getRotation()), 0., 0.);

        // Law 1 (t > 200s)
        attRef = lawOne.getAttitude(this.circ, this.date.shiftedBy(201), FramesFactory.getGCRF());
        att = aseq.getAttitude(this.circ, this.date.shiftedBy(201), FramesFactory.getGCRF());
        Assert.assertEquals(Rotation.distance(attRef.getRotation(), att.getRotation()), 0., 0.);
    }

    /**
     * Test method for
     * {@link fr.cnes.sirius.patrius.attitudes.AbstractAttitudeLaw#getAttitude(org.orekit.utils.PVCoordinatesProvider)}
     * .
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public final void testGetAttitude() throws PatriusException {

        final LofOffset attProv = new LofOffset(FramesFactory.getGCRF(), LOFType.QSW);
        final Attitude att = attProv.getAttitude(this.circ);
        final Attitude att2 = attProv.getAttitude(this.circ, this.circ.getDate(), this.circ.getFrame());

        Assert.assertEquals(att.getDate(), att2.getDate());
        Assert.assertEquals(att.getReferenceFrame(), att2.getReferenceFrame());
        Assert.assertTrue(att.getRotation().isEqualTo(att2.getRotation()));
        Assert.assertEquals(0, att.getSpin().getNorm(), att2.getSpin().getNorm());
    }

    @Before
    public void setUp() {
        try {

            Utils.setDataRoot("regular-data");
            FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

            // Computation date
            this.date = new AbsoluteDate(new DateComponents(2008, 04, 07), TimeComponents.H00,
                    TimeScalesFactory.getUTC());

            // Satellite position as circular parameters
            final double mu = 3.9860047e14;
            final double raan = 270.;
            this.circ = new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, MathLib.toRadians(50.), MathLib.toRadians(raan),
                    MathLib.toRadians(5.300 - raan), PositionAngle.MEAN, FramesFactory.getEME2000(), this.date, mu);

        } catch (final PatriusException oe) {
            Assert.fail(oe.getMessage());
        }

    }

    @After
    public void tearDown() throws PatriusException {
        this.date = null;
        this.circ = null;
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
    }
}
