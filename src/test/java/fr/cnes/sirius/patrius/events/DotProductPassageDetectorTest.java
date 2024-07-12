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
 * VERSION:4.11:FA:FA-3277:22/05/2023:[PATRIUS] Ellipsoïde ajuste sur un FacetBodyShape
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
import fr.cnes.sirius.patrius.propagation.events.DotProductPassageDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Testing the dot product passage detector (dot product between a spacecraft and a target
 * positions)
 *
 * @author Florian Teilhard
 *
 * @since 4.11
 *
 */
public class DotProductPassageDetectorTest {

    /** tolerance for distance comparisons */
    private static final double DISTANCE_COMPARISON_EPSILON = 1e-2;

    /** tolerance for angle comparisons */
    private static final double ANGLE_COMPARISON_EPSILON = 1e-8;


    /**
     * @testType UT
     *
     * @testedMethod {@link DotProductPassageDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     *
     * @description tests that the position of the satellite when the event occurs is correct
     *
     * @input a dot product passage detector, a keplerian propagator, and a target vector
     *
     * @testPassCriteria the distance between the end state (when the event occurs) and the
     *                   reference position is lower than a distance epsilon. Also compares the true
     *                   anomalies
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

        KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit);
        // Propagate over two orbit periods :
        final double time = 2 * initialOrbit.getKeplerianPeriod();

        // Target PV: (5000,0,0),(0,0,0) in EME2000
        PVCoordinatesProvider pvCoordTarget = new BasicPVCoordinatesProvider(new PVCoordinates(
                new Vector3D(5000, 0, 0), new Vector3D(0, 0, 0)), eme2000Frame);

        // Testing the detector for different target positions/slope selection type/normalizing
        // options

        // detector creation with Target position (5000,0,0), searching all zero-crossing
        EventDetector detector = new DotProductPassageDetector(pvCoordTarget, false, false, 0, eme2000Frame, 2);
        propagator.addEventDetector(detector);
        SpacecraftState endState = propagator.propagate(date.shiftedBy(time));

        // check that the distance of the end state of propagation is close to the expected one
        Assert.assertEquals(0.0, new Vector3D(0.0, a, 0.0).distance(endState.getPVCoordinates().getPosition()),
                DISTANCE_COMPARISON_EPSILON);
        // check the anomaly angle of the end state of propagation
        Assert.assertEquals(MathLib.PI / 2,
                endState.getOrbit().getParameters().getKeplerianParameters().getAnomaly(PositionAngle.TRUE),
                ANGLE_COMPARISON_EPSILON);

        // detector creation with Target position (5000,0,0), searching zero-crossing with
        // ascending slope only
        propagator = new KeplerianPropagator(initialOrbit);
        detector = new DotProductPassageDetector(pvCoordTarget, false, false, 0, eme2000Frame, 0);
        propagator.addEventDetector(detector);
        endState = propagator.propagate(date.shiftedBy(time));

        // check that the distance of the end state of propagation is close to the expected one
        Assert.assertEquals(0.0, new Vector3D(a, 0.0, 0.0).distance(endState.getPVCoordinates().getPosition()),
                DISTANCE_COMPARISON_EPSILON);
        // check the anomaly angle of the end state of propagation
        Assert.assertEquals(4 * MathLib.PI,
                endState.getOrbit().getParameters().getKeplerianParameters().getAnomaly(PositionAngle.TRUE),
                ANGLE_COMPARISON_EPSILON);

        // detector creation with Target position (5000,0,0), searching all zero-crossing,
        // normalized position
        propagator = new KeplerianPropagator(initialOrbit);
        detector = new DotProductPassageDetector(pvCoordTarget, true, false, 5000.0, eme2000Frame, 2);
        propagator.addEventDetector(detector);
        endState = propagator.propagate(date.shiftedBy(time));

        // check that the distance of the end state of propagation is close to the expected one
        Assert.assertEquals(0.0, new Vector3D(a, 0.0, 0.0).distance(endState.getPVCoordinates().getPosition()),
                DISTANCE_COMPARISON_EPSILON);
        // check the anomaly angle of the end state of propagation
        Assert.assertEquals(4 * MathLib.PI,
                endState.getOrbit().getParameters().getKeplerianParameters().getAnomaly(PositionAngle.TRUE),
                ANGLE_COMPARISON_EPSILON);

        Assert.assertEquals(0.0, detector.g(endState), DISTANCE_COMPARISON_EPSILON);

        // detector creation with Target position (5000,0,0), searching all zero-crossing,
        // normalized target
        propagator = new KeplerianPropagator(initialOrbit);
        detector = new DotProductPassageDetector(pvCoordTarget, false, true, a, eme2000Frame, 2);
        propagator.addEventDetector(detector);
        endState = propagator.propagate(date.shiftedBy(time));

        // check that the distance of the end state of propagation is close to the expected one
        Assert.assertEquals(0.0, new Vector3D(a, 0.0, 0.0).distance(endState.getPVCoordinates().getPosition()),
                DISTANCE_COMPARISON_EPSILON);
        // check the anomaly angle of the end state of propagation
        Assert.assertEquals(0.0,
                endState.getOrbit().getParameters().getKeplerianParameters().getAnomaly(PositionAngle.TRUE),
                ANGLE_COMPARISON_EPSILON);

        Assert.assertEquals(0.0, detector.g(endState), DISTANCE_COMPARISON_EPSILON);

        // detector creation with Target position (5000,0,0), searching zero-crossing with
        // ascending slope only
        propagator = new KeplerianPropagator(initialOrbit);
        detector = new DotProductPassageDetector(pvCoordTarget, false, false, 0, eme2000Frame, 0);
        propagator.addEventDetector(detector);
        endState = propagator.propagate(date.shiftedBy(time));

        // check that the distance of the end state of propagation is close to the expected one
        Assert.assertEquals(0.0, new Vector3D(a, 0.0, 0.0).distance(endState.getPVCoordinates().getPosition()),
                DISTANCE_COMPARISON_EPSILON);
        // check the anomaly angle of the end state of propagation
        Assert.assertEquals(4 * MathLib.PI,
                endState.getOrbit().getParameters().getKeplerianParameters().getAnomaly(PositionAngle.TRUE),
                ANGLE_COMPARISON_EPSILON);

        // detector creation with no frame specified
        propagator = new KeplerianPropagator(initialOrbit);
        pvCoordTarget = new BasicPVCoordinatesProvider(
                new PVCoordinates(new Vector3D(15, 0, 0), new Vector3D(0, 0, 0)), eme2000Frame);
        detector = new DotProductPassageDetector(pvCoordTarget, false, false, a, null, 0);
        propagator.addEventDetector(detector);
        endState = propagator.propagate(date.shiftedBy(time));

        Assert.assertEquals(0.0, new Vector3D(a, 0.0, 0.0).distance(endState.getPVCoordinates().getPosition()),
                DISTANCE_COMPARISON_EPSILON);
    }

    /**
     * @throws PatriusException
     * @testType UT
     *
     * @testedMethod {@link DotProductPassageDetector#copy()}
     *
     * @description tests the copy() method of the detector
     *
     * @input dot product passage detectors
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

        // Testing the detector copy
        final DotProductPassageDetector detectorMin = new DotProductPassageDetector(pvCoordTarget, true, false, 5.0,
                eme2000Frame, 0);

        final EventDetector detectorMin2 = detectorMin.copy();
        final SpacecraftState s = new SpacecraftState(initialOrbit);
        Assert.assertEquals(Action.CONTINUE, detectorMin2.eventOccurred(s, true, false));
    }

}
