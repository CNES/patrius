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
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.13:FA:FA-79:08/12/2023:[PATRIUS] Probleme dans la fonction g de LocalTimeAngleDetector
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:226:12/09/2014: problem with event detections.
 * VERSION::DM:454:24/11/2015:Unimplemented method shouldBeRemoved()
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.events.semianalytical;

import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Generic class for detectors.
 * 
 * @version $Id$
 * 
 * @since 2.3
 */
public abstract class Detector implements EventDetector {

    /** Serializable UID. */
    private static final long serialVersionUID = -892256863612109901L;

    /** Start time. */
    private AbsoluteDate t0;

    /** Event time. */
    private final AbsoluteDate eventTime;

    /** Action. */
    private final Action action;

    /** Event list. */
    protected List<Event> eventList;

    /** Convergence parameter. */
    protected final double convergence;

    /**
     * Constructor.
     * 
     * @param eventTime2
     *        event time
     * @param action2
     *        action
     * @param convergence2
     *        convergence parameter
     */
    public Detector(final AbsoluteDate eventTime2, final Action action2, final double convergence2) {
        this.eventTime = eventTime2;
        this.action = action2;
        this.convergence = convergence2;
    }

    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        this.t0 = s0.getDate();
        this.eventList = new ArrayList<>();
    }

    @Override
    public boolean shouldBeRemoved() {
        return false;
    }

    @Override
    public Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward) {
        this.eventList.add(new Event(this.getClass().getSimpleName(), s.getDate(), this.convergence));
        return this.action;
    }

    @Override
    public SpacecraftState resetState(final SpacecraftState oldState) {
        return oldState;
    }

    @Override
    public double getThreshold() {
        return this.convergence;
    }

    @Override
    public double getMaxCheckInterval() {
        return 0.1;
    }

    @Override
    public int getMaxIterationCount() {
        return 100;
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

    /**
     * Getter for t0.
     * 
     * @return t0
     */
    protected AbsoluteDate getT0() {
        return this.t0;
    }

    /**
     * Getter for event time.
     * 
     * @return event time
     */
    protected AbsoluteDate getEventTime() {
        return this.eventTime;
    }

    @Override
    public EventDetector copy() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean filterEvent(final SpacecraftState state,
            final boolean increasing,
            final boolean forward) throws PatriusException {
        return false;
    }
}
