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
 * @history created 12/09/2014
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:226:12/09/2014: problem with event detections.
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events.bounds;

import java.util.List;

import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * Test case, natural propagation end.
 * 
 * @version $Id: UC.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 2.3
 * 
 */
public abstract class UC {

    /** Result (propagation). */
    private boolean result1;

    /** Result (retropolation). */
    private boolean result2;

    /**
     * Constructor.
     * 
     * @param propagator
     *        propagator
     * @param t0
     *        t0
     * @param tf
     *        tf
     * @param eventDate
     *        event date
     * @param convergence
     *        convergence parameter
     */
    public UC(final Propagator propagator, final AbsoluteDate t0, final AbsoluteDate tf, final AbsoluteDate eventDate,
        final double convergence) {

        // Initialization
        try {
            propagator.clearEventsDetectors();
            // Move to start date
            propagator.propagate(t0);
        } catch (final PropagationException e) {
            this.result1 = false;
            e.printStackTrace();
        }

        // ================================== PROPAGATION ==================================

        this.result1 = true;

        final Detector2 detector1 = new Detector2(eventDate, convergence);
        final Detector2 detector2 = new Detector2(eventDate.shiftedBy(-100), convergence);
        final Detector2 detector3 = new Detector2(eventDate.shiftedBy(100), convergence);
        propagator.addEventDetector(detector1);
        propagator.addEventDetector(detector2);
        propagator.addEventDetector(detector3);

        try {
            propagator.propagate(tf);
        } catch (final PropagationException e) {
            this.result1 = false;
            e.printStackTrace();
        }

        // Check event list
        final List<Event> expectedEventList = this.getExpectedEventList();
        final List<Event> actualEventList = detector1.getEventList();
        actualEventList.addAll(detector2.getEventList());
        actualEventList.addAll(detector3.getEventList());
        this.result1 &= expectedEventList.equals(actualEventList);

        // ================================== RETROPOLATION ==================================

        this.result2 = true;

        propagator.clearEventsDetectors();
        final Detector2 detector4 = new Detector2(eventDate, convergence);
        final Detector2 detector5 = new Detector2(eventDate.shiftedBy(-100), convergence);
        final Detector2 detector6 = new Detector2(eventDate.shiftedBy(100), convergence);
        propagator.addEventDetector(detector4);
        propagator.addEventDetector(detector5);
        propagator.addEventDetector(detector6);

        try {
            propagator.propagate(t0);
        } catch (final PropagationException e) {
            this.result2 = false;
            e.printStackTrace();
        }

        // Check event list
        final List<Event> expectedEventList2 = this.getExpectedEventListRetropolation();
        final List<Event> actualEventList2 = detector4.getEventList();
        actualEventList2.addAll(detector5.getEventList());
        actualEventList2.addAll(detector6.getEventList());
        this.result2 &= expectedEventList2.equals(actualEventList2);

    }

    /**
     * Returns expected events list.
     * 
     * @return expected events list
     */
    public abstract List<Event> getExpectedEventList();

    /**
     * Returns expected events list for retropolation process.
     * 
     * @return expected events list for retropolation process
     */
    public abstract List<Event> getExpectedEventListRetropolation();

    /**
     * Returns test result.
     * 
     * @return test result
     */
    public boolean getTestResult() {
        return this.result1 && this.result2;
    }
}
