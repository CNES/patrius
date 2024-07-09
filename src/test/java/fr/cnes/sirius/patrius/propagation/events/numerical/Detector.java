/**
 * 
 * Copyright 2011-2017 CNES
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
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 *  VERSION::DM:226:12/09/2014: problem with event detections.
 *  VERSION::DM:454:24/11/2015:Add method shouldBeRemoved()
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.propagation.events.numerical;

import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.math.ode.events.EventHandler;

/**
 * Generic class for detectors.
 * 
 * @version $Id: Detector.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 2.3
 */
public abstract class Detector implements EventHandler {

    /** Start time. */
    private double t0;

    /** Event time. */
    private final double eventTime;

    /** Action. */
    private final Action action;

    /** Convergence parameter. */
    protected final double convergence;

    /** Event list. */
    protected List<Event> eventList;

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
    public Detector(final double eventTime2, final Action action2, final double convergence2) {
        this.eventTime = eventTime2;
        this.action = action2;
        this.convergence = convergence2;
    }

    @Override
    public void init(final double t0bis, final double[] y0, final double t) {
        this.t0 = t0bis;
        this.eventList = new ArrayList<Event>();
    }

    /**
     * 
     * @param t
     *        double
     * @param y
     *        double[]
     * @param increasing
     *        boolean
     * @param forward
     *        forward
     * @return action Action
     */
    @Override
    public Action eventOccurred(final double t, final double[] y, final boolean increasing, final boolean forward) {
        this.eventList.add(new Event(this.getClass().getSimpleName(), t, y[0], this.convergence));
        return this.action;
    }

    /** {@inheritDoc} */
    @Override
    public abstract boolean shouldBeRemoved();

    @Override
    public void resetState(final double t, final double[] y) {
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
    protected double getT0() {
        return this.t0;
    }

    /**
     * Getter for event time.
     * 
     * @return event time
     */
    protected double getEventTime() {
        return this.eventTime;
    }

}
