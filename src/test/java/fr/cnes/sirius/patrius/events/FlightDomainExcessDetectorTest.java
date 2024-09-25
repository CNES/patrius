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
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
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
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.events.EventDetector.Action;
import fr.cnes.sirius.patrius.events.detectors.FlightDomainExcessDetector;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.RotationOrder;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Testing the flight domain excess detector (
 *
 * @author Florian Teilhard
 *
 * @since 4.11
 *
 */
public class FlightDomainExcessDetectorTest {

    /** tolerance for angle comparisons */
    private static final double ANGLE_COMPARISON_EPSILON = 1e-8;

    /** tolerance for date comparisons */
    private static final double DATE_COMPARISON_EPSILON = 1e-6;

    /** Create an attitude provider with an increasing rotation quaternions over time */
    final AttitudeProvider attitudeProviderIncreasingRotation = new AttitudeProvider() {

        /** Serializable UID. */
        private static final long serialVersionUID = 8349380468661246626L;

        /**
         * Set the spin derivatives computations.
         */
        @Override
        public void setSpinDerivativesComputation(final boolean computeSpinDeriv) {
            // Nothing needs to be done here
        }

        /**
         * Get the attitude. In this case, the rotation rate is linear decreasing with regards to
         * the time, but the acceleration and the rotation are constant.
         */
        @Override
        public Attitude getAttitude(final PVCoordinatesProvider pvProv, final AbsoluteDate date, final Frame frame)
                throws PatriusException {
            // attitude of the spacecraft
            final Rotation rotation = new Rotation(RotationOrder.XYZ, -0.4
                    * MathLib.sin(date.durationFrom(AbsoluteDate.J2000_EPOCH)),
                    0.8 * date.durationFrom(AbsoluteDate.J2000_EPOCH), 1.98 * MathLib.cos(date
                    .durationFrom(AbsoluteDate.J2000_EPOCH)));

            final AngularCoordinates angularCoord = new AngularCoordinates(rotation, new Vector3D(0.1, 0.3, 0.05),
                    new Vector3D(0.01, 0.03, 0.005));
            return new Attitude(date, FramesFactory.getGCRF(), angularCoord);
        }
    };

    /**
     * @testType UT
     *
     * @testedMethod {@link FlightDomainExcessDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     *
     * @description tests that the position of the satellite when the event occurs is correct
     *
     * @input a flight domain excess detector, a keplerian propagator
     *
     * @testPassCriteria the distance between the end state (when the event occurs) and the
     *                   reference position is lower than a distance epsilon
     *
     * @referenceVersion 4.11
     *
     * @nonRegressionVersion 4.11
     *
     * @throws PatriusException PatriusException
     */
    @Test
    public void testFlightDomainExcessKeplerianPropagator() throws PatriusException {

        // initial orbit
        final Frame eme2000Frame = FramesFactory.getEME2000();
        final Frame gcrfFrame = FramesFactory.getGCRF();
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final double a = 7000000.0;
        final Orbit initialOrbit = new KeplerianOrbit(a, 0.0, 0.0, 0.0, 0.0, 0.0, PositionAngle.TRUE, gcrfFrame, date,
                Utils.mu);

        // Angles extrema array
        final double[][] angleMinMax = { { 0.2, -0.3, 0.4 }, { 0.5, 0.6, 0.7 } };

        // Creating the detector
        final FlightDomainExcessDetector detector = new FlightDomainExcessDetector(RotationOrder.XYZ, angleMinMax,
                gcrfFrame);
        detector.setMaxCheckInterval(10.);
        detector.setAttitudeRepresentedFrame(eme2000Frame, gcrfFrame);

        // Creating the propagator
        final KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit, attitudeProviderIncreasingRotation);
        // Propagate over two orbit periods :
        final double time = 2 * initialOrbit.getKeplerianPeriod();

        // Testing the detector
        propagator.addEventDetector(detector);
        final SpacecraftState endState = propagator.propagate(date.shiftedBy(time));

        // check that the distance of the end state of propagation is close to the expected one
        Assert.assertEquals(0.0,
                MathLib.abs(new AbsoluteDate("2000-01-01T13:45:06.340526440848757").durationFrom(endState.getDate())),
                DATE_COMPARISON_EPSILON);

        Assert.assertEquals(0.37416845838759555, endState.getAttitude().getRotation().getAngles(RotationOrder.XYZ)[0],
                ANGLE_COMPARISON_EPSILON);
        Assert.assertEquals(0.2890783137300955, endState.getAttitude().getRotation().getAngles(RotationOrder.XYZ)[1],
                ANGLE_COMPARISON_EPSILON);
        Assert.assertEquals(0.700000093738707, endState.getAttitude().getRotation().getAngles(RotationOrder.XYZ)[2],
                ANGLE_COMPARISON_EPSILON);
    }

    /**
     * @testType UT
     *
     * @testedMethod {@link FlightDomainExcessDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     *
     * @description tests that the position of the satellite when the event occurs is still correct
     *              with negative angle widths
     *
     * @input a flight domain excess detector, a keplerian propagator
     *
     * @testPassCriteria the distance between the end state (when the event occurs) and the
     *                   reference position is lower than a distance epsilon
     *
     * @referenceVersion 4.11
     *
     * @nonRegressionVersion 4.11
     *
     * @throws PatriusException PatriusException
     */
    @Test
    public void testNegativeAngleWidth() throws PatriusException {

        // initial orbit
        final Frame gcrfFrame = FramesFactory.getGCRF();
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final double a = 7000000.0;
        final Orbit initialOrbit = new KeplerianOrbit(a, 0.0, 0.0, 0.0, 0.0, 0.0, PositionAngle.TRUE, gcrfFrame, date,
                Utils.mu);

        // Angles extrema array with inversed second angles and a 2*PI slide on first angle max
        final double[][] angleMinMax = { { 0.2, 0.6, 0.4 }, { 0.5 - 2 * MathLib.PI, -0.3, 0.7 } };

        // Creating the detector
        final FlightDomainExcessDetector detector = new FlightDomainExcessDetector(RotationOrder.XYZ, angleMinMax,
                gcrfFrame);
        detector.setMaxCheckInterval(10.);
        detector.setAttitudeRepresentedFrame(gcrfFrame, gcrfFrame);

        // Creating the propagator
        final KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit, attitudeProviderIncreasingRotation);
        // Propagate over two orbit periods :
        final double time = 2 * initialOrbit.getKeplerianPeriod();

        // Testing the detector
        propagator.addEventDetector(detector);
        final SpacecraftState endState = propagator.propagate(date.shiftedBy(time));

        // check that the distance of the end state of propagation is close to the expected one
        Assert.assertEquals(0.0,
                MathLib.abs(new AbsoluteDate("2000-01-01T13:45:06.340526440848757").durationFrom(endState.getDate())),
                DATE_COMPARISON_EPSILON);

        Assert.assertEquals(0.37416842456507104, endState.getAttitude().getRotation().getAngles(RotationOrder.XYZ)[0],
                ANGLE_COMPARISON_EPSILON);
        Assert.assertEquals(0.28907850506868144, endState.getAttitude().getRotation().getAngles(RotationOrder.XYZ)[1],
                ANGLE_COMPARISON_EPSILON);
        Assert.assertEquals(0.7000005367199521, endState.getAttitude().getRotation().getAngles(RotationOrder.XYZ)[2],
                ANGLE_COMPARISON_EPSILON);
    }

    /**
     * @throws PatriusException PatriusException
     * @testType UT
     *
     * @testedMethod {@link FlightDomainExcessDetector#copy()}
     *
     * @description tests the copy() method of the detector
     *
     * @input flight domain excess detectors
     *
     * @testPassCriteria The Action objects and the angle thresholds array are well copied
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

        final double[][] angleMinMax = { { 0.2, -0.3, 0.4 }, { 0.5, 0.6, 0.7 } };
        // Testing the detector copy
        final FlightDomainExcessDetector detectorInit = new FlightDomainExcessDetector(RotationOrder.XYZ, angleMinMax,
                eme2000Frame);

        final EventDetector detectorCopied = detectorInit.copy();
        final SpacecraftState s = new SpacecraftState(initialOrbit);
        Assert.assertEquals(Action.CONTINUE, detectorCopied.eventOccurred(s, true, false));
    }

    /**
     * Setup before class.
     *
     * @throws PatriusException should not happen
     */
    @BeforeClass
    public static void setUpBeforeClass() throws PatriusException {
        // Orekit initialisation
        Utils.setDataRoot("regular-dataPBASE");
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
    }

}
