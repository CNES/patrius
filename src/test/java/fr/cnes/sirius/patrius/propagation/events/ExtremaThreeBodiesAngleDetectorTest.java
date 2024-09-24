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
 * @history created 11/07/12
 *
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2460:27/05/2020:Prise en compte des temps de propagation dans les calculs evenements
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:393:12/03/2015: Constant Attitude Laws
 * VERSION::DM:300:22/04/2015: Creation multi propagator
 * VERSION::DM:454:24/11/2015:Class test updated according the new implementation for detectors
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.propagation.events;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
import fr.cnes.sirius.patrius.attitudes.directions.BasicPVCoordinatesProvider;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.propagation.events.ExtremaThreeBodiesAngleDetector.BodyOrder;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Unit tests for {@link ExtremaThreeBodiesAngleDetector}.
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: ExtremaThreeBodiesAngleDetectorTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.1
 * 
 */
public class ExtremaThreeBodiesAngleDetectorTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validate the extrema three bodies angle detector
         * 
         * @featureDescription Validate extrema the three bodies angle detector
         * 
         * @coveredRequirements DV-EVT_121
         */
        VALIDATE_EXTREMA_THREE_BODIES_ANGLE_DETECTOR,

        /**
         * @featureTitle Validate the extrema three bodies angle detector in multi propagation context
         * 
         * @featureDescription Validate the extrema three bodies angle detector
         */
        VALIDATE_MULTI_EXTREMA_THREE_BODIES_ANGLE_DETECTOR

    }

    /** Epsilon for dates comparison. */
    private final double datesComparisonEpsilon = 1.0e-3;

    /** EME2000. */
    private Frame eme2000Frame;

    /** Start date. */
    private AbsoluteDate startDate;

    /** Attitude provider. */
    private AttitudeProvider attitudeProv;

    /** Initial orbit. */
    private KeplerianOrbit initialOrbit;

    /** Period. */
    private double period;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_EXTREMA_THREE_BODIES_ANGLE_DETECTOR}
     * 
     * @testedMethod {@link ExtremaThreeBodiesAngleDetector#g(SpacecraftState)}
     * @testedMethod {@link ExtremaThreeBodiesAngleDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description propagates an orbit + test getters
     * 
     * @input constructor parameters (three points of space with linear movements) and a propagator
     * 
     * @output resulting SpacecraftState from the propagation
     * 
     * @testPassCriteria check that when the propagation stops, the angle between the three bodies is equal to
     *                   the given angle.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 3.0
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public void testMovingPoints() throws PatriusException {

        // date
        final double duration = 10000.;
        final AbsoluteDate endDate = this.startDate.shiftedBy(duration);

        // propagator
        final Propagator propagator = new KeplerianPropagator(this.initialOrbit, this.attitudeProv);

        // body 1
        final PVCoordinates pvBody1_1 =
            new PVCoordinates(new Vector3D(-1.0e3, 1.0e2, 1.0e2), new Vector3D(1.0e6 / duration, 0.0, 0.0));
        final PVCoordinates pvBody1_2 =
            new PVCoordinates(new Vector3D(1.0e3, 1.0e2, 1.0e2), new Vector3D(1.0e6 / duration, 0.0, 0.0));

        final PVCoordinatesProvider body1 = new LinearTwoPointsPVProvider(
            pvBody1_1, this.startDate, pvBody1_2, endDate, this.eme2000Frame);

        // body 2
        final PVCoordinates pvBody2 = new PVCoordinates(Vector3D.ZERO, Vector3D.ZERO);
        final PVCoordinatesProvider body2 = new BasicPVCoordinatesProvider(pvBody2, this.eme2000Frame);

        // body 3
        final PVCoordinates pvBody3_1 =
            new PVCoordinates(new Vector3D(0.0, 0.0, 0.0), new Vector3D(0.0, 0.0, 2.0e2 / duration));
        final PVCoordinates pvBody3_2 =
            new PVCoordinates(new Vector3D(0.0, 0.0, 2.0e2), new Vector3D(0.0, 0.0, 2.0e2 / duration));

        final PVCoordinatesProvider body3 = new LinearTwoPointsPVProvider(
            pvBody3_1, this.startDate, pvBody3_2, endDate, this.eme2000Frame);

        // MIN detector
        final ExtremaThreeBodiesAngleDetector detectorMIN =
            new ExtremaThreeBodiesAngleDetector(body1, body2, body3, ExtremaThreeBodiesAngleDetector.MIN);

        new ExtremaThreeBodiesAngleDetector(body1, body2, body3, ExtremaThreeBodiesAngleDetector.MAX);

        new ExtremaThreeBodiesAngleDetector(body1, body2, body3, ExtremaThreeBodiesAngleDetector.MIN_MAX);

        new ExtremaThreeBodiesAngleDetector(body1, body2, body3, AbstractDetector.DEFAULT_MAXCHECK,
            AbstractDetector.DEFAULT_THRESHOLD, Action.CONTINUE, Action.STOP);

        // test of MIN detection
        propagator.addEventDetector(detectorMIN);
        try {
            propagator.propagate(this.startDate.shiftedBy(duration));
            Assert.fail();
        } catch (final ArithmeticException e) {
            // Expected
            Assert.assertTrue(true);
        }

        final ExtremaThreeBodiesAngleDetector detector2 = (ExtremaThreeBodiesAngleDetector) detectorMIN.copy();
        /*
         * Test getters
         */
        Assert.assertEquals(body1.hashCode(), detector2.getFirstBody().hashCode());
        Assert.assertEquals(body2.hashCode(), detector2.getSecondBody().hashCode());
        Assert.assertEquals(body3.hashCode(), detector2.getThirdBody().hashCode());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_EXTREMA_THREE_BODIES_ANGLE_DETECTOR}
     * 
     * @testedMethod {@link ExtremaThreeBodiesAngleDetector#g(SpacecraftState)}
     * @testedMethod {@link ExtremaThreeBodiesAngleDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description propagates an orbit
     * 
     * @input constructor parameters (three points of space with linear movements) and a propagator
     * 
     * @output resulting SpacecraftState from the propagation
     * 
     * @testPassCriteria check that when the propagation stops, the angle between the three bodies is equal to
     *                   the given angle.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 3.0
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public void testWithSpacecraft() throws PatriusException {

        // frame and date
        final double duration = 100000.;

        // propagator
        Propagator propagator = new KeplerianPropagator(this.initialOrbit, this.attitudeProv);

        // point A : north pole point
        final PVCoordinates pvBodyA = new PVCoordinates(new Vector3D(0.0, 0.0, 6000000.), Vector3D.ZERO);
        final PVCoordinatesProvider bodyA = new BasicPVCoordinatesProvider(pvBodyA, this.eme2000Frame);

        // point B : earth center
        final PVCoordinates pvBodyB = new PVCoordinates(Vector3D.ZERO, Vector3D.ZERO);
        final PVCoordinatesProvider bodyB = new BasicPVCoordinatesProvider(pvBodyB, this.eme2000Frame);

        /*
         * Spacecraft being the FIRST body
         */
        // MAX detector=
        final EventDetector detectorMAX_FIRST =
            new ExtremaThreeBodiesAngleDetector(bodyB, bodyA, BodyOrder.FIRST, ExtremaThreeBodiesAngleDetector.MAX).copy();

        // MIN MAX detector
        final EventDetector detectorMINMAX_FIRST =
            new ExtremaThreeBodiesAngleDetector(bodyB, bodyA, BodyOrder.FIRST,
                ExtremaThreeBodiesAngleDetector.MIN_MAX).copy();

        // MIN MAX detector
        final EventDetector detectorMINMAX_FIRST_continue =
            new ExtremaThreeBodiesAngleDetector(bodyB, bodyA, BodyOrder.FIRST, AbstractDetector.DEFAULT_MAXCHECK,
                AbstractDetector.DEFAULT_THRESHOLD, Action.CONTINUE, Action.STOP);

        /*
         * Spacecraft being the SECOND body
         */
        // MIN detector
        final EventDetector detectorMIN_SEC =
            new ExtremaThreeBodiesAngleDetector(bodyA, bodyB, BodyOrder.SECOND, ExtremaThreeBodiesAngleDetector.MIN);

        // MAX detector
        final EventDetector detectorMAX_SEC = new ExtremaThreeBodiesAngleDetector(bodyA, bodyB, BodyOrder.SECOND,
            ExtremaThreeBodiesAngleDetector.MAX);

        // MIN MAX detector
        final EventDetector detectorMINMAX_SEC =
            new ExtremaThreeBodiesAngleDetector(bodyA, bodyB, BodyOrder.SECOND,
                ExtremaThreeBodiesAngleDetector.MIN_MAX);

        // MIN MAX detector
        final EventDetector detectorMINMAX_SEC_2 =
            new ExtremaThreeBodiesAngleDetector(bodyA, bodyB, BodyOrder.SECOND, AbstractDetector.DEFAULT_MAXCHECK,
                AbstractDetector.DEFAULT_THRESHOLD, Action.STOP, Action.CONTINUE);

        /*
         * Spacecraft being the THIRD body
         */
        // MAX detector
        final EventDetector detectorMAX_THIRD =
            new ExtremaThreeBodiesAngleDetector(bodyA, bodyB, BodyOrder.THIRD, ExtremaThreeBodiesAngleDetector.MAX);

        // MAX detector
        final EventDetector detectorMINMAX_THIRD =
            new ExtremaThreeBodiesAngleDetector(bodyA, bodyB, BodyOrder.THIRD,
                ExtremaThreeBodiesAngleDetector.MIN_MAX);

        // MAX detector
        final EventDetector detectorMINMAX_THIRD_2 =
            new ExtremaThreeBodiesAngleDetector(bodyA, bodyB, BodyOrder.THIRD, AbstractDetector.DEFAULT_MAXCHECK,
                AbstractDetector.DEFAULT_THRESHOLD, Action.CONTINUE, Action.STOP);

        /*
         * Test "first body"
         */
        // the max angle is reached when the spacecraft is on the north pole
        propagator.addEventDetector(detectorMAX_FIRST);
        SpacecraftState endState = propagator.propagate(this.startDate.shiftedBy(duration));
        double expectedTime = this.period * 3 / 4.;
        Assert.assertEquals(expectedTime, endState.getDate().durationFrom(this.startDate), this.datesComparisonEpsilon);

        // the max angle is reached when the spacecraft is on the north pole with MIN_MAX detector
        propagator = new KeplerianPropagator(this.initialOrbit, this.attitudeProv);
        propagator.propagate(this.startDate.shiftedBy(this.period * 1 / 2));
        propagator.addEventDetector(detectorMINMAX_FIRST);
        endState = propagator.propagate(this.startDate.shiftedBy(duration));
        expectedTime = this.period * 3 / 4.;
        Assert.assertEquals(expectedTime, endState.getDate().durationFrom(this.startDate), this.datesComparisonEpsilon);

        // the max angle is reached when the spacecraft is on the north pole with MIN_MAX detector
        propagator = new KeplerianPropagator(this.initialOrbit, this.attitudeProv);
        propagator.addEventDetector(detectorMINMAX_FIRST_continue);
        endState = propagator.propagate(this.startDate.shiftedBy(duration));
        expectedTime = this.period * 3 / 4.;
        Assert.assertEquals(expectedTime, endState.getDate().durationFrom(this.startDate), this.datesComparisonEpsilon);

        /*
         * Test "second body"
         */
        // the min angle is reached when the spacecraft is on the south pole
        propagator = new KeplerianPropagator(this.initialOrbit, this.attitudeProv);
        propagator.addEventDetector(detectorMIN_SEC);
        endState = propagator.propagate(this.startDate.shiftedBy(duration));
        expectedTime = this.period * 1 / 4.;
        System.out.println("min " + expectedTime);
        Assert.assertEquals(expectedTime, endState.getDate().durationFrom(this.startDate), this.datesComparisonEpsilon);

        // the min angle is reached when the spacecraft is on the south pole with MIN_MAX detector
        propagator = new KeplerianPropagator(this.initialOrbit, this.attitudeProv);
        propagator.addEventDetector(detectorMAX_SEC);
        final SpacecraftState endStateMAX = propagator.propagate(this.startDate.shiftedBy(duration));
        propagator = new KeplerianPropagator(this.initialOrbit, this.attitudeProv);
        propagator.propagate(endStateMAX.getDate().shiftedBy(10.));
        propagator.addEventDetector(detectorMINMAX_SEC);
        endState = propagator.propagate(this.startDate.shiftedBy(duration));
        Assert.assertEquals(expectedTime, endState.getDate().durationFrom(this.startDate), this.datesComparisonEpsilon);

        // the min angle is reached when the spacecraft is on the south pole with MIN_MAX detector
        propagator = new KeplerianPropagator(this.initialOrbit, this.attitudeProv);
        propagator.propagate(endStateMAX.getDate().shiftedBy(10.));
        propagator.addEventDetector(detectorMINMAX_SEC_2);
        endState = propagator.propagate(this.startDate.shiftedBy(duration));
        Assert.assertEquals(expectedTime, endState.getDate().durationFrom(this.startDate), this.datesComparisonEpsilon);

        /*
         * Test "third body"
         */
        // the max angle is reached when the spacecraft is on the south pole
        propagator = new KeplerianPropagator(this.initialOrbit, this.attitudeProv);
        propagator.addEventDetector(detectorMAX_THIRD);
        endState = propagator.propagate(this.startDate.shiftedBy(duration));
        expectedTime = this.period * 3 / 4.;
        Assert.assertEquals(expectedTime, endState.getDate().durationFrom(this.startDate), this.datesComparisonEpsilon);

        // the max angle is reached when the spacecraft is on the south pole with MIN_MAX detector
        propagator = new KeplerianPropagator(this.initialOrbit, this.attitudeProv);
        propagator.propagate(this.startDate.shiftedBy(this.period * 1 / 2));
        propagator.addEventDetector(detectorMINMAX_THIRD);
        endState = propagator.propagate(this.startDate.shiftedBy(duration));
        expectedTime = this.period * 3 / 4.;
        Assert.assertEquals(expectedTime, endState.getDate().durationFrom(this.startDate), this.datesComparisonEpsilon);

        // the max angle is reached when the spacecraft is on the south pole with MIN_MAX detector
        propagator = new KeplerianPropagator(this.initialOrbit, this.attitudeProv);
        propagator.propagate(this.startDate.shiftedBy(this.period * 1 / 2));
        propagator.addEventDetector(detectorMINMAX_THIRD);
        endState = propagator.propagate(this.startDate.shiftedBy(duration));
        expectedTime = this.period * 3 / 4.;
        Assert.assertEquals(expectedTime, endState.getDate().durationFrom(this.startDate), this.datesComparisonEpsilon);

        // the max angle is reached when the spacecraft is on the south pole with MIN_MAX detector
        propagator = new KeplerianPropagator(this.initialOrbit, this.attitudeProv);
        propagator.propagate(this.startDate.shiftedBy(this.period * 1 / 2));
        propagator.addEventDetector(detectorMINMAX_THIRD_2);
        endState = propagator.propagate(this.startDate.shiftedBy(duration));
        expectedTime = this.period * 3 / 4.;
        Assert.assertEquals(expectedTime, endState.getDate().durationFrom(this.startDate), this.datesComparisonEpsilon);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_MULTI_EXTREMA_THREE_BODIES_ANGLE_DETECTOR}
     * 
     * @testedMethod {@link ExtremaThreeBodiesAngleDetector#g(Map)}
     * @testedMethod {@link ExtremaThreeBodiesAngleDetector#g(SpacecraftState)}
     * @testedMethod {@link ExtremaThreeBodiesAngleDetector#getInSpacecraftId1()}
     * @testedMethod {@link ExtremaThreeBodiesAngleDetector#getInSpacecraftId2()}
     * @testedMethod {@link ExtremaThreeBodiesAngleDetector#getInSpacecraftId3()}
     * 
     * @description Test exceptions raised by g() method. Test for coverage purposes
     * 
     * @testPassCriteria exception raised
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testExceptions() throws PatriusException {
        /** Sat id 1 */
        final String ID1 = "state1";
        /** Sat id 2 */
        final String ID2 = "state2";
        /** Sat id 3 */
        final String ID3 = "state3";

        final ExtremaThreeBodiesAngleDetector detector1 = new ExtremaThreeBodiesAngleDetector(this.initialOrbit,
            this.initialOrbit, this.initialOrbit, ExtremaThreeBodiesAngleDetector.MAX);

        final Map<String, SpacecraftState> states = new HashMap<>();
        states.put(ID1, new SpacecraftState(this.initialOrbit));
        states.put(ID2, new SpacecraftState(this.initialOrbit.shiftedBy(10)));
        states.put(ID3, new SpacecraftState(this.initialOrbit.shiftedBy(20)));
        boolean testOk = false;
        try {
            detector1.g(states);
            Assert.fail();
        } catch (final PatriusException e) {
            testOk = true;
            Assert.assertEquals(PatriusMessages.MONO_MULTI_DETECTOR.getSourceString(), e.getMessage());
        }
        Assert.assertTrue(testOk);

        final ExtremaThreeBodiesAngleDetector detector2 = new ExtremaThreeBodiesAngleDetector(ID1, ID2, ID3,
            AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD, Action.STOP, Action.CONTINUE);

        testOk = false;
        try {
            detector2.g(new SpacecraftState(this.initialOrbit));
            Assert.fail();
        } catch (final PatriusException e) {
            testOk = true;
            Assert.assertEquals(PatriusMessages.MONO_MULTI_DETECTOR.getSourceString(), e.getMessage());
        }
        Assert.assertTrue(testOk);

        // Coverage tests
        // ==============
        // init() method
        detector2.init(states, this.startDate);

        // reset states method
        Assert.assertEquals(states.hashCode(), detector2.resetStates(states).hashCode());

        // Constructor with two actions (MIN_MAX case)
        Assert.assertEquals(ExtremaThreeBodiesAngleDetector.MIN_MAX, detector2.getSlopeSelection());
        Assert.assertEquals(Action.STOP, detector2.eventOccurred(states, false, false));
        Assert.assertEquals(Action.STOP, detector2.eventOccurred(states, true, true));
        Assert.assertEquals(Action.CONTINUE, detector2.eventOccurred(states, false, true));
        Assert.assertEquals(Action.CONTINUE, detector2.eventOccurred(states, true, false));

        // Constructor with one action (MIN)
        final ExtremaThreeBodiesAngleDetector detector3 = new ExtremaThreeBodiesAngleDetector(ID1, ID2, ID3,
            ExtremaThreeBodiesAngleDetector.MIN,
            AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD, Action.STOP);
        Assert.assertEquals(ExtremaThreeBodiesAngleDetector.MIN, detector3.getSlopeSelection());
        Assert.assertEquals(Action.STOP, detector3.eventOccurred(states, false, false));

        // Constructor with one action (MAX)
        final ExtremaThreeBodiesAngleDetector detector4 = new ExtremaThreeBodiesAngleDetector(ID1, ID2, ID3,
            ExtremaThreeBodiesAngleDetector.MAX,
            AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD, Action.STOP);
        Assert.assertEquals(ExtremaThreeBodiesAngleDetector.MAX, detector4.getSlopeSelection());
        Assert.assertEquals(Action.STOP, detector4.eventOccurred(states, false, false));

        // Constructor with one action (MIN_MAX)
        final ExtremaThreeBodiesAngleDetector detector5 = new ExtremaThreeBodiesAngleDetector(ID1, ID2, ID3,
            ExtremaThreeBodiesAngleDetector.MIN_MAX,
            AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD, Action.STOP);
        Assert.assertEquals(ExtremaThreeBodiesAngleDetector.MIN_MAX, detector5.getSlopeSelection());
        Assert.assertEquals(Action.STOP, detector5.eventOccurred(states, false, false));
        Assert.assertNotNull(detector5.g(states));

        // Test getters
        Assert.assertEquals(ID1, detector5.getInSpacecraftId1());
        Assert.assertEquals(ID2, detector5.getInSpacecraftId2());
        Assert.assertEquals(ID3, detector5.getInSpacecraftId3());
    }

    /**
     * Initializations
     * 
     * @since 3.0
     */
    @Before
    public void setUp() {
        // frame and date
        this.eme2000Frame = FramesFactory.getEME2000();
        this.startDate = AbsoluteDate.J2000_EPOCH;

        this.attitudeProv = new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY);
        final double a = 7000000.0;
        this.initialOrbit = new KeplerianOrbit(a, 0.0, MathUtils.HALF_PI, 0.0, 0.0, 0.0,
            PositionAngle.TRUE, this.eme2000Frame, this.startDate, Utils.mu);
        this.period = 2.0 * FastMath.PI * MathLib.sqrt(a * a * a / Utils.mu);
    }

}
