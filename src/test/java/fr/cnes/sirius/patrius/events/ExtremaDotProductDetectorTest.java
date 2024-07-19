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
 * @history created 23/04/12
 *
 *
 * HISTORY
 * VERSION:4.11:DM:DM-3197:22/05/2023:[PATRIUS] Deplacement dans PATRIUS de classes definies dans la façade ALGO DV SIRUS 
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::FA:558:25/02/2016:Correction of algorithm for simultaneous events detection
 * VERSION::FA:612:21/07/2016:Bug in same date events with Action.STOP
 * VERSION::FA:673:12/09/2016: add getTotalMass(state)
 * VERSION::DM:1173:26/06/2017:add propulsive and tank properties
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.directions.BasicPVCoordinatesProvider;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.propagation.events.ExtremaDotProductDetector;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Testing the extrema dot product detector (dot product between a spacecraft and a target
 * positions)
 *
 * @author Florian Teilhard
 *
 * @since 4.11
 *
 */
public class ExtremaDotProductDetectorTest {

    /**
     * @testType UT
     *
     * @testedMethod {@link ExtremaDotProductDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     *
     * @description tests that the position of the satellite when the event occurs is correct
     *
     * @input an extrema dot product detector, a keplerian propagator, and a target vector
     *
     * @testPassCriteria the distance between the end state (when the event occurs) and the
     *                   reference position is lower than 1e-5
     *
     * @referenceVersion 4.11
     *
     * @nonRegressionVersion 4.11
     */
    @Test
    public void testDotProductKeplerianPropagator() throws PatriusException {

        // propagator
        final Frame eme2000Frame = FramesFactory.getEME2000();
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final double a = 7000000.0;

        final Orbit initialOrbit = new KeplerianOrbit(a, 0.0, 0.0, 0.0, 0.0, 0.0, PositionAngle.TRUE, eme2000Frame,
                date, Utils.mu);

        final KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit);
        final SpacecraftState initialState = new SpacecraftState(initialOrbit);

        propagator.resetInitialState(initialState);
        // Propagate over a half orbit period :
        final double time = 5 * initialOrbit.getKeplerianPeriod();

        // Target PV: (5,0,0),(0,0,0) in EME2000
        PVCoordinatesProvider pvCoordTarget = new BasicPVCoordinatesProvider(new PVCoordinates(new Vector3D(0, 5, 0),
                new Vector3D(0, 0, 0)), eme2000Frame);

        // Testing the detector for different target positions/extremum type/normalizing

        // detector creation with Target position (0,5,0), searching minimum
        EventDetector detector = new ExtremaDotProductDetector(pvCoordTarget, true, false, eme2000Frame, 0);
        propagator.addEventDetector(detector);
        SpacecraftState endState = propagator.propagate(date.shiftedBy(time));

        Assert.assertEquals(0.0, new Vector3D(0.0, -a, 0.0).distance(endState.getPVCoordinates().getPosition()), 1e-5);

        // detector creation with Target position (0,5,0), searching maximum
        detector = new ExtremaDotProductDetector(pvCoordTarget, false, true, eme2000Frame, 1);
        propagator.addEventDetector(detector);
        endState = propagator.propagate(date.shiftedBy(time));

        Assert.assertEquals(0.0, new Vector3D(0.0, a, 0.0).distance(endState.getPVCoordinates().getPosition()), 1e-5);

        // detector creation with Target position (0,5,0), searching minimum or maximum
        detector = new ExtremaDotProductDetector(pvCoordTarget, true, true, eme2000Frame, 2);
        propagator.addEventDetector(detector);
        endState = propagator.propagate(date.shiftedBy(time));

        Assert.assertEquals(0.0, new Vector3D(0.0, -a, 0.0).distance(endState.getPVCoordinates().getPosition()), 1e-5);

        // detector tests with different PV of target
        pvCoordTarget = new BasicPVCoordinatesProvider(
                new PVCoordinates(new Vector3D(-15, 0, 0), new Vector3D(0, 0, 0)), eme2000Frame);
        detector = new ExtremaDotProductDetector(pvCoordTarget, false, false, eme2000Frame, 2);
        propagator.addEventDetector(detector);
        endState = propagator.propagate(date.shiftedBy(time));

        Assert.assertEquals(0.0, new Vector3D(a, 0.0, 0.0).distance(endState.getPVCoordinates().getPosition()), 1e-5);

        // detector creation
        pvCoordTarget = new BasicPVCoordinatesProvider(
                new PVCoordinates(new Vector3D(15, 15, 0), new Vector3D(0, 0, 0)), eme2000Frame);
        detector = new ExtremaDotProductDetector(pvCoordTarget, false, false, eme2000Frame, 2);
        propagator.addEventDetector(detector);
        endState = propagator.propagate(date.shiftedBy(time));
        Assert.assertEquals(0.0, new Vector3D(a * MathLib.cos(MathLib.PI / 4), a * MathLib.sin(MathLib.PI / 4), 0.0)
                .distance(endState.getPVCoordinates().getPosition()), 1e-5);

        // detector creation with no frame specified
        pvCoordTarget = new BasicPVCoordinatesProvider(
                new PVCoordinates(new Vector3D(15, 0, 0), new Vector3D(0, 0, 0)), eme2000Frame);
        detector = new ExtremaDotProductDetector(pvCoordTarget, false, false, null, 2);
        propagator.addEventDetector(detector);
        endState = propagator.propagate(date.shiftedBy(time));
        Assert.assertEquals(0.0, new Vector3D(0.0, a, 0.0)
                .distance(endState.getPVCoordinates().getPosition()), 1e-5);
    }

    /**
     * @testType UT
     *
     * @testedMethod {@link ExtremaDotProductDetector#getBody()}
     *
     * @description tests the getBody() method of the detector
     *
     * @input a random PVCoordinates for the target
     *
     * @testPassCriteria the output is the target PVCoordinates
     *
     * @referenceVersion 4.11
     */
    @Test
    public void testGetBody() {

        // propagator
        final Frame eme2000Frame = FramesFactory.getEME2000();

        final PVCoordinatesProvider pvCoordTarget = new BasicPVCoordinatesProvider(new PVCoordinates(new Vector3D(0, 5, 0),
                new Vector3D(0, 0, 0)), eme2000Frame);

        // Testing the detector for different target positions/extremum type/normalizing

        // detector creation with Target position (0,5,0), searching minimum
        final ExtremaDotProductDetector detector = new ExtremaDotProductDetector(pvCoordTarget, true, false, eme2000Frame, 0);

        Assert.assertEquals(pvCoordTarget, detector.getBody());
    }

    /**
     * @throws PatriusException
     * @testType UT
     *
     * @testedMethod {@link ExtremaDotProductDetector#copy()}
     *
     * @description tests the copy() method of the detector
     *
     * @input extrema dot product detectors (with min and max detection)
     *
     * @testPassCriteria The Action object is well copied
     *
     * @referenceVersion 4.11
     */
    @Test
    public void testCopy() throws PatriusException {
        // initial orbit
        final Frame eme2000Frame = FramesFactory.getEME2000();
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final double a = 7000000.0;

        final Orbit initialOrbit = new KeplerianOrbit(a, 0.0, 0.0, 0.0, 0.0, 0.0, PositionAngle.TRUE, eme2000Frame,
                date, Utils.mu);

        final PVCoordinatesProvider pvCoordTarget = new BasicPVCoordinatesProvider(new PVCoordinates(new Vector3D(0, 5,
                0), new Vector3D(0, 0, 0)), eme2000Frame);

        // Testing the detector for different target positions/extremum type/normalizing

        final ExtremaDotProductDetector detectorMin = new ExtremaDotProductDetector(pvCoordTarget, true, false,
                eme2000Frame, 0);

        final EventDetector detectorMin2 = detectorMin.copy();
        SpacecraftState s = new SpacecraftState(initialOrbit);
        Assert.assertEquals(Action.STOP, detectorMin2.eventOccurred(s, true, false));

        final ExtremaDotProductDetector detectorMax = new ExtremaDotProductDetector(pvCoordTarget, true, false,
                eme2000Frame, 1);
        final EventDetector detectorMax2 = detectorMax.copy();
        s = new SpacecraftState(initialOrbit);
        Assert.assertEquals(Action.STOP, detectorMax2.eventOccurred(s, true, false));
    }

}
