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
 * @history 24/04/2012
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.utils;

import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.events.CodedEventsList;
import fr.cnes.sirius.patrius.events.CodedEventsLogger;
import fr.cnes.sirius.patrius.events.GenericCodingEventDetector;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.interval.IntervalEndpointType;
import fr.cnes.sirius.patrius.math.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.events.NodeDetector;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @description
 *              <p>
 *              - implicit interval test
 *              </p>
 * 
 * @author ClaudeD
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public class ImplicitIntervalTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validate the implicit interval
         * 
         * @featureDescription Validate the implicit interval (implicit event)
         * 
         * @coveredRequirements DV-DATES_170, DV-EVT_50
         */
        VALIDATE_IMPLICIT_INTERVAL
    }

    /**
     * A Cartesian orbit used for the test.
     */
    private static KeplerianOrbit orbit;

    /**
     * CodedEvent logger.
     */
    private static CodedEventsLogger logger;

    /**
     * Setup for all unit tests in the class.
     * Provides an {@link Orbit}.
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @BeforeClass
    public static void setUpBeforeClass() throws PatriusException {

        // Orekit initialization
        Utils.setDataRoot("regular-dataPBASE");

        // . input keplerian parameters
        final double mu = 3.9860047e14;
        final AbsoluteDate iniDate = AbsoluteDate.J2000_EPOCH;
        final double a1 = 2464000.0;
        final double e1 = 0.7311;
        final double i1 = 0.122138;
        final double aop1 = 3.10686;
        final double raan1 = 1.00681;
        final double M1 = 0.048363;

        orbit = new KeplerianOrbit(a1, e1, i1, aop1, raan1, M1, PositionAngle.MEAN,
            FramesFactory.getEME2000(), iniDate, mu);

        final NodeDetectorTest node = new NodeDetectorTest(orbit, orbit.getFrame());

        final GenericCodingEventDetector nodeDet =
            new GenericCodingEventDetector(node, "Ascending node", "Descending node", true, "Nodes");

        logger = new CodedEventsLogger();
        final EventDetector detector = logger.monitorDetector(nodeDet);

        final NumericalPropagator propagator =
            new NumericalPropagator(new ClassicalRungeKuttaIntegrator(10.0));
        propagator.addEventDetector(detector);
        propagator.setEphemerisMode();
        propagator.resetInitialState(new SpacecraftState(orbit));
        propagator.propagate(iniDate.shiftedBy(6000));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_IMPLICIT_INTERVAL}
     * 
     * @testedMethod {@link AbsoluteDateInterval#AbsoluteDateInterval(IntervalEndpointType, AbsoluteDate, AbsoluteDate, IntervalEndpointType)}
     * 
     * @description simple implicit interval
     * 
     * @input dates of ascending nodes
     * 
     * @output an {@link AbsoluteDateInterval}
     * 
     * @testPassCriteria the {@link AbsoluteDateInterval} is successfully created
     * 
     * @throws PatriusException
     *         e
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testImplicitInterval() throws PatriusException {

        // recovery events
        final CodedEventsList list = logger.getCodedEventsList();

        System.out.println(list.getList().size());
        for (int i = 0; i < list.getList().size(); i++) {
            System.out.println(list.getList().get(i));
        }

        // Creating an implicit interval
        // with the date of the first ascending node and the date of the third ascending node
        final AbsoluteDate lowEnd = list.getList().get(0).getDate();
        final AbsoluteDate upEnd = list.getList().get(4).getDate();

        final AbsoluteDateInterval NAdateInterval =
            new AbsoluteDateInterval(IntervalEndpointType.CLOSED, lowEnd,
                upEnd, IntervalEndpointType.CLOSED);
        assertNotNull(NAdateInterval);
    }

    /**
     * @description
     *              Node detector that does not stop the propagation
     * 
     */
    private static class NodeDetectorTest extends NodeDetector {
        
        /** Serializable UID. */
        private static final long serialVersionUID = -727540730020133272L;

        /**
         * Constructor
         * 
         * @param orbit
         * @param frame
         */
        public NodeDetectorTest(final KeplerianOrbit orbit, final Frame frame) {
            super(orbit, frame, NodeDetector.ASCENDING_DESCENDING);
        }

        /**
         * eventOccurred
         */
        @Override
        public Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward) {
            return Action.CONTINUE;
        }
    }
}
