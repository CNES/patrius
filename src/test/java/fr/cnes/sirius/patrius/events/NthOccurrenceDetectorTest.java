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
 * @history 15/03/2013
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:86:19/09/2013:New ForceModel interface
 * VERSION::FA:86:22/10/2013:New mass management system
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::FA:381:14/01/2015:Propagator tolerances and default mass and attitude issues
 * VERSION::DM:393:12/03/2015: Constant Attitude Laws
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:470:14/08/2015: Correction for NthOccurrenceDetector coupled with one-way detectors
 * VERSION::DM:454:24/11/2015:Add TU to verify detector suppression at nth occurrence
 * VERSION::FA:558:25/02/2016:Correction of algorithm for simultaneous events detection
 * VERSION::DM:596:12/04/2016:Improve test coherence
 * VERSION::FA:673:12/09/2016: add getTotalMass(state)
 * VERSION::DM:1173:26/06/2017:add propulsive and tank properties
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
import fr.cnes.sirius.patrius.forces.maneuvers.ImpulseManeuver;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.SimpleMassModel;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.propagation.events.AOLDetector;
import fr.cnes.sirius.patrius.propagation.events.AbstractDetector;
import fr.cnes.sirius.patrius.propagation.events.ApsideDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.propagation.events.EventsLogger;
import fr.cnes.sirius.patrius.propagation.events.NodeDetector;
import fr.cnes.sirius.patrius.propagation.events.NthOccurrenceDetector;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScale;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for {@link NthOccurrenceDetector}
 * 
 * @author Rami Houdroge
 * 
 * @version $Id$
 * 
 * @since 1.3
 */
public class NthOccurrenceDetectorTest {

    private final double eps = Precision.EPSILON;
    private Frame gcrf;
    private AbsoluteDate date;
    private KeplerianOrbit orbit;
    private NodeDetector node;

    /** Number of detection. */
    int nthDetector = 0;

    /** Test status. */
    boolean testStatus = true;

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validate the nth detector
         * 
         * @featureDescription Validate the nth detector
         * 
         * @coveredRequirements ???
         */
        VALIDATE_NTH_DETECTOR
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_NTH_DETECTOR}
     * 
     * @testedMethod {@link NthOccurrenceDetector#NthOccurrenceDetector(EventDetector, int, Action)}
     * @testedMethod {@link NthOccurrenceDetector#getMaxCheckInterval()}
     * @testedMethod {@link NthOccurrenceDetector#getMaxIterationCount()}
     * @testedMethod {@link NthOccurrenceDetector#getSlopeSelection()}
     * @testedMethod {@link NthOccurrenceDetector#getThreshold()}
     * @testedMethod {@link NthOccurrenceDetector#resetState(SpacecraftState)}
     * @testedMethod {@link NthOccurrenceDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description test the detector in a static context
     * 
     * @input an orbit
     * 
     * @output actions
     * 
     * @testPassCriteria the Action is as expected, as well as other parameters. Accuracy is 1e-14
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void test() throws PatriusException {

        Utils.setDataRoot("regular-dataPBASE");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        final Frame gcrf = FramesFactory.getGCRF();
        final AbsoluteDate date = new AbsoluteDate();
        final KeplerianOrbit orbit = new KeplerianOrbit(Constants.EGM96_EARTH_EQUATORIAL_RADIUS + 400000, .00001,
            MathLib.toRadians(43), 0, 0, 0, PositionAngle.MEAN, gcrf, date, Constants.EGM96_EARTH_MU);

        final NodeDetector node = new NodeDetector(orbit, gcrf, NodeDetector.ASCENDING){
            private static final long serialVersionUID = 7247687933687883866L;

            @Override
            public Action eventOccurred(final SpacecraftState s,
                                        final boolean increasing,
                                        final boolean forward) throws PatriusException {
                System.out.println(s.getDate() + " : RAAN detected");
                return Action.STOP;
            }
        };

        // detect third occurrence
        final NthOccurrenceDetector occur = new NthOccurrenceDetector(node, 3, Action.RESET_STATE);

        // make sure that the detect behaves as expected on the 3rd occurrence behaves
        Assert.assertEquals(Action.CONTINUE, occur.eventOccurred(null, true, true));
        Assert.assertEquals(occur.getCurrentOccurrence(), 1);
        Assert.assertEquals(Action.CONTINUE, occur.eventOccurred(null, true, true));
        Assert.assertEquals(occur.getCurrentOccurrence(), 2);
        Assert.assertEquals(Action.RESET_STATE, occur.eventOccurred(null, true, true));
        Assert.assertEquals(occur.getCurrentOccurrence(), 3);

        // parameters
        Assert.assertEquals(node.getMaxCheckInterval(), occur.getMaxCheckInterval(), this.eps);
        Assert.assertEquals(node.getMaxIterationCount(), occur.getMaxIterationCount(), this.eps);
        Assert.assertEquals(node.getSlopeSelection(), occur.getSlopeSelection(), this.eps);
        Assert.assertEquals(node.getThreshold(), occur.getThreshold(), this.eps);

        // no mods to state
        final SpacecraftState state = new SpacecraftState(orbit);
        Assert.assertEquals(state, occur.resetState(state));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_NTH_DETECTOR}
     * 
     * @testedMethod {@link NthOccurrenceDetector#g(SpacecraftState)}
     * 
     * @description test the detector in a dynamic context (propagator)
     * 
     * @input an orbit
     * 
     * @output actions
     * 
     * @testPassCriteria the impulse maneuver is carried out once only. strict integer comparision
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testImpulse() throws PatriusException {

        final NthOccurrenceDetector occur = new NthOccurrenceDetector(this.node, 4, Action.STOP);
        final MyImpulseManeuver impulse = new MyImpulseManeuver(occur, Vector3D.PLUS_I, 300);

        // g function
        final KeplerianPropagator kep = new KeplerianPropagator(this.orbit);
        kep.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY));
        kep.addEventDetector(impulse);
        kep.propagate(this.date.shiftedBy(this.orbit.getKeplerianPeriod() * 12));

        Assert.assertEquals(1, impulse.count);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_NTH_DETECTOR}
     * 
     * @testedMethod {@link NthOccurrenceDetector#shouldBeRemoved()}
     * 
     * @description test that the detector is successfully removed
     *              after nth occurrence detection in a dynamic context (propagator)
     * 
     * @input an orbit
     * @input a logger
     * 
     * @output actions
     * 
     * @testPassCriteria the logger record exactly n events. Strict integer comparision
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testNthOccurrence() throws PatriusException {

        // Apogee detector
        final ApsideDetector apogee = new ApsideDetector(ApsideDetector.APOGEE, this.orbit.getKeplerianPeriod() / 3,
            1.0e-13 * this.orbit.getKeplerianPeriod(), Action.CONTINUE);
        final NthOccurrenceDetector occur = new NthOccurrenceDetector(apogee, 4, Action.STOP, true);

        // g function
        final KeplerianPropagator kep = new KeplerianPropagator(this.orbit);
        kep.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY));

        // logger to count detections
        final EventsLogger logger = new EventsLogger();
        kep.addEventDetector(logger.monitorDetector(occur));
        kep.propagate(this.date.shiftedBy(this.orbit.getKeplerianPeriod() * 12));
        final int nbOccurence = logger.getLoggedEvents().size();

        Assert.assertEquals(4, nbOccurence);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_NTH_DETECTOR}
     * 
     * @testedMethod {@link NthOccurrenceDetector#NthOccurrenceDetector(EventDetector eventToDetect,int occurrence, Action actionAtOccurrence, Action actionUnderOccurence)}
     * 
     * @description simple constructor test
     * 
     * @input constructor parameters
     * 
     * @output a {@link NthOccurrenceDetector}
     * 
     * @testPassCriteria the {@link NthOccurrenceDetector} is successfully created
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     * 
     */
    @Test
    public void testConstructeur() {

        final NthOccurrenceDetector occur = new NthOccurrenceDetector(this.node, 4, Action.STOP);
        // Test getter
        Assert.assertEquals(this.node, occur.getEvent());
        Assert.assertEquals(4, occur.getOccurence());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_NTH_DETECTOR}
     * 
     * @testedMethod {@link NthOccurrenceDetector#NthOccurrenceDetector(EventDetector eventToDetect,int occurrence, Action actionAtOccurrence, Action actionUnderOccurence)}
     * 
     * @description test detector with event to detect only on increasing g function (AOL)
     * 
     * @input an orbit
     * 
     * @output Action
     * 
     * @testPassCriteria Manoeuver occurs exactly at 6th occurrence of AOL detection
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     * 
     */
    @Test
    public void testAOLDetector() throws PatriusException {

        // *********************************************************************//

        // Constants
        final double mu = Constants.GRIM5C1_EARTH_MU;
        final TimeScale scale = TimeScalesFactory.getTAI();
        final AbsoluteDate initialDate = new AbsoluteDate(2001, 1, 1, scale);
        final Frame inertialFrame = FramesFactory.getEME2000();
        // Keplerian orbit
        final Orbit orbit = new KeplerianOrbit(7000000, 0, MathLib.toRadians(40), 0, 0, -10, PositionAngle.TRUE,
            inertialFrame, initialDate, mu);

        // Extrapolators

        // DOP8
        final double[] vecAbsoluteTolerance = { 1e-5, 1e-5, 1e-5, 1e-8, 1e-8, 1e-8 };
        final double[] vecRelativeTolerance = { 1e-10, 1e-10, 1e-10, 1e-10, 1e-10, 1e-10 };
        final FirstOrderIntegrator extrapolator1 = new DormandPrince853Integrator(0.01, 10, vecAbsoluteTolerance,
            vecRelativeTolerance);
        // Numerical propagator
        final NumericalPropagator numP1 = new NumericalPropagator(extrapolator1);

        // *********************************************************************//

        // AOL detector
        final EventDetector aolDetector = new AOLDetector(MathLib.toRadians(50), PositionAngle.TRUE, inertialFrame);
        // Nthocurrence event detector
        final NthOccurrenceDetector secondAol = new NthOccurrenceDetector(aolDetector, 6, Action.STOP);

        // Node detector
        final EventDetector nodeDetector = new NodeDetector(inertialFrame, 0, AbstractDetector.DEFAULT_MAXCHECK,
            AbstractDetector.DEFAULT_THRESHOLD, Action.CONTINUE){
            /** Serializable UID. */
            private static final long serialVersionUID = -4179580036489924059L;

            @Override
            public Action eventOccurred(final SpacecraftState s,
                                        final boolean increasing,
                                        final boolean forward) throws PatriusException {

                NthOccurrenceDetectorTest.this.nthDetector = NthOccurrenceDetectorTest.this.nthDetector + 1;

                // Manoeuver expected after 6th node detection (6th AOL detection is right after 6th node detection)
                NthOccurrenceDetectorTest.this.testStatus &= (NthOccurrenceDetectorTest.this.nthDetector <= 6
                        && s.getA() == 7000000 || NthOccurrenceDetectorTest.this.nthDetector > 6 && s.getA() != 7000000);

                return super.eventOccurred(s, increasing, forward);
            }
        };

        // *********************************************************************//

        // Mass Model
        final MassProvider massModel = new SimpleMassModel(1000, "body");

        // Initial state
        final SpacecraftState initialState = new SpacecraftState(orbit, massModel);

        // Add state
        numP1.setInitialState(initialState);
        numP1.setMassProviderEquation(massModel);
        numP1.addEventDetector(nodeDetector);

        // Add maneuvers
        final ImpulseManeuver impMan1 = new ImpulseManeuver(secondAol, new Vector3D(100, 0, 0), 300., massModel,
            "body", LOFType.LVLH);
        numP1.addEventDetector(impMan1);

        // Extrapolation
        numP1.propagate(new AbsoluteDate(initialDate, 86400.));

        // Check
        Assert.assertTrue(this.testStatus);
    }

    @Before
    public void setup() throws PatriusException {
        Utils.setDataRoot("regular-dataPBASE");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        this.gcrf = FramesFactory.getGCRF();
        this.date = new AbsoluteDate();
        this.orbit = new KeplerianOrbit(Constants.EGM96_EARTH_EQUATORIAL_RADIUS + 400000, .00001,
            MathLib.toRadians(43), 0, 0, 0, PositionAngle.MEAN, this.gcrf, this.date, Constants.EGM96_EARTH_MU);

        this.node = new NodeDetector(this.orbit, this.gcrf, NodeDetector.ASCENDING);

    }

    class MyImpulseManeuver extends ImpulseManeuver {
        /** Serializable UID. */
        private static final long serialVersionUID = -2917816263999617600L;

        public MyImpulseManeuver(final EventDetector trigger,
                                 final Vector3D deltaVSat,
                                 final double isp) throws PatriusException {
            super(trigger, deltaVSat, isp, new SimpleMassModel(1000., "thruster"), "thruster");
        }

        int count = 0;

        @Override
        public Action eventOccurred(final SpacecraftState s,
                                    final boolean increasing,
                                    final boolean forward) throws PatriusException {

            final Action result = super.eventOccurred(s, increasing, forward);
            if (result == Action.RESET_STATE) {
                this.count++;
                System.out.println("Fired at " + s.getDate() + "   "
                        + s.getPVCoordinates(NthOccurrenceDetectorTest.this.gcrf));
            }
            return result;
        }
    }
}
