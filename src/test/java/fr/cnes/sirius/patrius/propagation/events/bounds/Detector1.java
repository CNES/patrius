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
 * VERSION::DM:454:24/11/2015:Unimplemented method shouldBeRemoved()
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.propagation.events.bounds;

import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Detector at date t. Action.STOP.
 * 
 * @version $Id: Detector1.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 2.3
 * 
 */
public class Detector1 implements EventDetector {

    /** Serializable UID. */
    private static final long serialVersionUID = 7020069158070687281L;
    /** Event list. */
    protected List<Event> eventList;
    /** Convergence parameter. */
    protected final double convergence;
    /** Event time. */
    private final AbsoluteDate eventTime;

    /**
     * Constructor.
     * 
     * @param eventTime2
     *        event time
     * @param convergence2
     *        convergence parameter
     */
    public Detector1(final AbsoluteDate eventTime2, final double convergence2) {
        this.eventTime = eventTime2;
        this.convergence = convergence2;
        this.eventList = new ArrayList<>();
    }

    /**
     * Initializes.
     * 
     * @param s0
     *        SpacecraftState
     * @param t
     *        AbsoluteDate
     */
    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        // nothing to do
    }

    /**
     * Detects variation.
     * 
     * @param s
     *        SpacecraftState
     * @return the g value
     * @throws PatriusException
     *         bcs SpacecraftState
     */
    @Override
    public double g(final SpacecraftState s) throws PatriusException {
        return s.getDate().durationFrom(this.eventTime);
    }

    /** {@inheritDoc} */
    @Override
    public boolean shouldBeRemoved() {
        return false;
    }

    /**
     * What happens if the event occurs ?
     * 
     * @param s
     *        SpacecraftState
     * @param increasing
     *        boolean
     * @param forward
     *        boolean
     * @return Action Action.STOP
     * @throws PatriusException
     *         bcs SpacecraftState
     */
    @Override
    public
            Action
            eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                   throws PatriusException {
        this.eventList.add(new Event(this.getClass().getSimpleName(), s.getDate(), this.convergence));
        return Action.STOP;
    }

    /**
     * Resets state.
     * 
     * @param oldState
     *        SpacecraftState
     * @return oldState SpacecraftState
     * @throws PatriusException
     *         bcs SpacecraftState
     */
    @Override
    public SpacecraftState resetState(final SpacecraftState oldState) throws PatriusException {
        return oldState;
    }

    @Override
    public int getSlopeSelection() {
        return INCREASING_DECREASING;
    }

    /**
     * Returns event list.
     * 
     * @return event list
     */
    public List<Event> getEventList() {
        return this.eventList;
    }

    @Override
    public double getThreshold() {
        return this.convergence;
    }

    @Override
    public double getMaxCheckInterval() {
        return 10;
    }

    @Override
    public int getMaxIterationCount() {
        return 100;
    }

    @Override
    public EventDetector copy() {
        return null;
    }
}
