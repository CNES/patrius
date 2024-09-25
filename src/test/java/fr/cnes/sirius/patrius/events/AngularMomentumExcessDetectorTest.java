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
 * @history creation 06/08/2012
 *
 * HISTORY
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.11:DM:DM-3197:22/05/2023:[PATRIUS] Deplacement dans PATRIUS de classes definies dans la façade ALGO DV SIRUS 
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
package fr.cnes.sirius.patrius.events;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.events.detectors.AngularMomentumExcessDetector;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.complex.Quaternion;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.linear.BlockRealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.util.FastMath;
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
 * <p>
 * Test class for the angular momentum excess detector.
 * </p>
 *
 * @see AngularMomentumExcessDetector
 *
 * @author Florian Teilhard
 *
 *
 * @since 4.11
 *
 */
public class AngularMomentumExcessDetectorTest {

    /** tolerance for date comparisons */
    private static final double DATE_COMPARISON_EPSILON = 1e-8;

    // Create an attitude provider with a decreasing rotation rate over time
    final AttitudeProvider attitudeProviderDecreasingVel = new AttitudeProvider() {

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
            final Rotation initialRotation = new Rotation(true, new Quaternion(0.2, 0.4, 1, 0.7));

            final AngularCoordinates angularCoord = new AngularCoordinates(initialRotation,
                    new Vector3D(0.1, 0, 0.05).scalarMultiply(3000 - date.durationFrom(AbsoluteDate.J2000_EPOCH)),
                    new Vector3D(0.01, 0.03, 0.005));
            return new Attitude(date, FramesFactory.getEME2000(), angularCoord);
        }
    };

    // Create an attitude provider with an increasing rotation rate over time
    final AttitudeProvider attitudeProviderIncreasingVel = new AttitudeProvider() {

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
            final Rotation initialRotation = new Rotation(true, new Quaternion(0.2, 0.4, 1, 0.7));

            final AngularCoordinates angularCoord = new AngularCoordinates(initialRotation,
                    new Vector3D(0.1, 0, 0.05).scalarMultiply(date.durationFrom(AbsoluteDate.J2000_EPOCH)),
                    new Vector3D(0.01, 0.03, 0.005));
            return new Attitude(date, FramesFactory.getEME2000(), angularCoord);
        }
    };

    /**
     * @testType UT
     *
     * @testedMethod {@link AngularMomentumExcessDetector#g(SpacecraftState)}
     * @testedMethod {@link AngularMomentumExcessDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     *
     * @description Here we test the dates at which the event "exceeding the angular momentum" has
     *              occured
     *
     * @input a spacecraft, its orbit, its attitude law
     *
     * @output dates of the detected event
     *
     * @testPassCriteria the stop date is correct, depending on the action given by the detector
     *                   (Continue or Stop). The reference dates are generated for non regression
     *                   test.
     * @throws PatriusException if a frame problem occurs
     *
     * @referenceVersion 4.11
     *
     * @nonRegressionVersion 4.11
     */
    @Test
    public void detectorTest() throws PatriusException {

        // Working frames and initial date
        final Frame eme2000Frame = FramesFactory.getEME2000();
        final Frame gcrfFrame = FramesFactory.getGCRF();
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        // Working orbit
        final double a = 7000000.0;
        final Orbit initialOrbit = new KeplerianOrbit(a, 0.0, FastMath.PI / 2., 0.0, 0.0, 0.0, PositionAngle.TRUE,
                eme2000Frame, date, Utils.mu);
        // Propagation will be over a half orbit period :
        final double time = 0.5 * initialOrbit.getKeplerianPeriod();

        // detector creation
        final AngularMomentumExcessDetector detector = new AngularMomentumExcessDetector(3000, eme2000Frame, 2);
        detector.setAttitudeRepresentedFrame(eme2000Frame);

        // 3x3 Data array to represent the inertia matrix
        final double[][] data = { { 8.07905, 4.25766, 6.12480 }, { 4.34826, 4.46287, 1.70499 },
                { 1.60868, 5.30236, 1.52292 } };
        final RealMatrix inertiaMatrix = new BlockRealMatrix(data);
        detector.setInertia(inertiaMatrix, gcrfFrame);

        // propagator with decreasing rotation rate over time
        // Creating the propagator
        final KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit, attitudeProviderDecreasingVel);
        propagator.addEventDetector(detector);
        // Propagation
        final SpacecraftState endState = propagator.propagate(date.shiftedBy(time));
        // Check the end date of the propagation
        Assert.assertEquals(0.0,
                MathLib.abs(new AbsoluteDate("2000-01-01T12:09:32.008522902631940").durationFrom(endState.getDate())),
                DATE_COMPARISON_EPSILON);

        // propagator with an increasing rotation rate over time
        // Creating the propagator
        final KeplerianPropagator propagatorInc = new KeplerianPropagator(initialOrbit, attitudeProviderIncreasingVel);
        propagatorInc.addEventDetector(detector);
        // Propagation
        final SpacecraftState endStateInc = propagatorInc.propagate(date.shiftedBy(time));
        // Check the end date of the propagation
        Assert.assertEquals(0.0, MathLib.abs(date.shiftedBy(time)
                .durationFrom(endStateInc.getDate())), DATE_COMPARISON_EPSILON);
    }

    /**
     * @testType UT
     *
     * @testedMethod {@link AngularMomentumExcessDetector#copy()}
     * @testedMethod {@link AngularMomentumExcessDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     *
     * @description Here we test the copy method
     *
     * @input an event detector with random data
     *
     * @output a copy of the event detector
     *
     * @testPassCriteria the slope selection and the actions are identical between the original
     *                   detector and its copy.
     * @throws PatriusException if a frame problem occurs
     *
     * @referenceVersion 4.11
     *
     * @nonRegressionVersion 4.11
     */
    @Test
    public void testCopy() throws PatriusException {
        final Frame eme2000Frame = FramesFactory.getEME2000();
        final Frame gcrfFrame = FramesFactory.getGCRF();

        // detector creation
        final AngularMomentumExcessDetector detector = new AngularMomentumExcessDetector(3000, eme2000Frame, 2);
        detector.setAttitudeRepresentedFrame(eme2000Frame);

        // 3x3 Data array to represent the inertia matrix
        final double[][] data = { { 8.07905, 4.25766, 6.12480 }, { 4.34826, 4.46287, 1.70499 },
                { 1.60868, 5.30236, 1.52292 } };
        final RealMatrix inertiaMatrix = new BlockRealMatrix(data);
        detector.setInertia(inertiaMatrix, gcrfFrame);

        final EventDetector detectorCopy = detector.copy();

        // Test the copy of the slope selection
        Assert.assertEquals(detector.getSlopeSelection(), detectorCopy.getSlopeSelection());
        // Test the copy of the Actions via eventOccured method
        Assert.assertEquals(detector.eventOccurred(null, true, false), detectorCopy.eventOccurred(null, true, false));
        Assert.assertEquals(detector.eventOccurred(null, false, false), detectorCopy.eventOccurred(null, false, false));
    }
}
