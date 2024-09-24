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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:FA:FA-2338:27/05/2020:Correction dans IntervalOccurenceDetector 
 * VERSION:4.4:DM:DM-2210:04/10/2019:[PATRIUS] Ameliorations de IntervalOccurenceDetector
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.propagation.events.IntervalOccurrenceDetector;
import fr.cnes.sirius.patrius.propagation.events.NodeDetector;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for {@link IntervalOccurrenceDetector}.
 * Most getters and simple methods are tested in the class {@link NthOccurrenceDetectorTest}.
 * 
 * @author Emmanuel Bignon
 * 
 * @version $Id$
 * 
 * @since 4.1
 */
public class IntervalOccurrenceDetectorTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validate the {@link IntervalOccurrenceDetector}
         * 
         * @featureDescription Validate the {@link IntervalOccurrenceDetector}
         * 
         * @coveredRequirements ???
         */
        INTERVAL_OCCURRENCE_DETECTOR
    }

    /** Count number of occurrences. */
    int count = 0;

    /** Count number of occurrences. */
    int count2 = 0;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INTERVAL_OCCURRENCE_DETECTOR}
     * 
     * @testedMethod {@link IntervalOccurrenceDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description test the {@link IntervalOccurrenceDetector}: check that occurrence between 4th and 10th occurrence
     *              with a step of two are properly detected,
     *              others should not be detected
     * 
     * @input orbit, {@link IntervalOccurrenceDetector}
     * 
     * @output actions
     * 
     * @testPassCriteria detected occurrences are as expected
     * 
     * @referenceVersion 4.4
     * 
     * @nonRegressionVersion 4.4
     */
    @Test
    public void testIntervalOccurrenceDetector() throws PatriusException {

        // Initialization
        final Frame gcrf = FramesFactory.getGCRF();
        final AbsoluteDate initialDate = AbsoluteDate.J2000_EPOCH;
        final KeplerianOrbit initialOrbit =
            new KeplerianOrbit(7000000, 0, 0.1, 0, 0, 0, PositionAngle.MEAN, gcrf, initialDate,
                Constants.EGM96_EARTH_MU);
        IntervalOccurrenceDetectorTest.this.count = 0;

        final int[] expected = { 4, 6, 8, 10 };
        // Detectors
        final NodeDetector node = new NodeDetector(initialOrbit, gcrf, NodeDetector.ASCENDING);
        final IntervalOccurrenceDetector intervalOccurrenceDetector = (IntervalOccurrenceDetector)
            new IntervalOccurrenceDetector(node, 4, 10, 2, Action.RESET_STATE){
                /** Serializable UID. */
                private static final long serialVersionUID = 4311388664802569626L;

                @Override
                public Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                    throws PatriusException {
                    final Action action = super.eventOccurred(s, increasing, forward);
                    if (action == Action.RESET_STATE) {
                        // Check that occurrences match 4, 6, 8, 10
                        Assert.assertEquals(expected[IntervalOccurrenceDetectorTest.this.count],
                            this.getCurrentOccurrence());
                        IntervalOccurrenceDetectorTest.this.count++;
                    }
                    return action;
                }

                @Override
                public void processEventOccurrence(final SpacecraftState s, final boolean increasing,
                                                   final boolean forward) {
                    Assert.assertEquals(expected[IntervalOccurrenceDetectorTest.this.count],
                        this.getCurrentOccurrence());
                }
            }.copy();

        // Perform propagation
        final KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit);
        propagator.addEventDetector(intervalOccurrenceDetector);
        propagator.propagate(initialDate.shiftedBy(86400.));

        // Check getters
        Assert.assertEquals(intervalOccurrenceDetector.getFirstOccurrence(), 4);
        Assert.assertEquals(intervalOccurrenceDetector.getLastOccurrence(), 10);
        Assert.assertEquals(intervalOccurrenceDetector.getStep(), 2);
        Assert.assertNotNull(intervalOccurrenceDetector.toString());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INTERVAL_OCCURRENCE_DETECTOR}
     * 
     * @testedMethod {@link IntervalOccurrenceDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description test the {@link IntervalOccurrenceDetector}: check that occurrence between 4th and 10th occurrence
     *              with a step of two are properly detected,
     *              others should not be detected. Detector should be removed afterwards
     * 
     * @input orbit, {@link IntervalOccurrenceDetector}
     * 
     * @output actions
     * 
     * @testPassCriteria detected occurrences are as expected
     * 
     * @referenceVersion 4.5
     * 
     * @nonRegressionVersion 4.5
     */
    @Test
    public void testIntervalOccurrenceDetectorRemove() throws PatriusException {

        // Initialization
        final Frame gcrf = FramesFactory.getGCRF();
        final AbsoluteDate initialDate = AbsoluteDate.J2000_EPOCH;
        final KeplerianOrbit initialOrbit =
            new KeplerianOrbit(7000000, 0, 0.1, 0, 0, 0, PositionAngle.MEAN, gcrf, initialDate,
                Constants.EGM96_EARTH_MU);
        IntervalOccurrenceDetectorTest.this.count = 0;
        IntervalOccurrenceDetectorTest.this.count2 = 0;

        final int[] expected = { 4, 6, 8, 10 };
        // Detectors
        final NodeDetector node = new NodeDetector(initialOrbit, gcrf, NodeDetector.ASCENDING);
        final IntervalOccurrenceDetector intervalOccurrenceDetector =
            new IntervalOccurrenceDetector(node, 4, 10, 2, Action.RESET_STATE, true){
                /** Serializable UID. */
                private static final long serialVersionUID = 9122681541187075532L;

                @Override
                public Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                    throws PatriusException {
                    final Action action = super.eventOccurred(s, increasing, forward);
                    if (action == Action.RESET_STATE) {
                        // Check that occurrences match 4, 6, 8, 10
                        Assert.assertEquals(expected[IntervalOccurrenceDetectorTest.this.count],
                            this.getCurrentOccurrence());
                        IntervalOccurrenceDetectorTest.this.count++;
                    }
                    IntervalOccurrenceDetectorTest.this.count2++;
                    // Check no occurrence is detected after 10th occurrence
                    Assert.assertTrue(IntervalOccurrenceDetectorTest.this.count2 <= 10);
                    return action;
                }

                @Override
                public void processEventOccurrence(final SpacecraftState s, final boolean increasing,
                                                   final boolean forward) {
                    Assert.assertEquals(expected[IntervalOccurrenceDetectorTest.this.count],
                        this.getCurrentOccurrence());
                }
            };

        // Perform propagation
        final KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit);
        propagator.addEventDetector(intervalOccurrenceDetector);
        propagator.propagate(initialDate.shiftedBy(86400.));

        // Check getters
        Assert.assertEquals(intervalOccurrenceDetector.getFirstOccurrence(), 4);
        Assert.assertEquals(intervalOccurrenceDetector.getLastOccurrence(), 10);
        Assert.assertEquals(intervalOccurrenceDetector.getStep(), 2);
    }
}
