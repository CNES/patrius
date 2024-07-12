/**
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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:454:24/11/2015:Unimplemented method shouldBeRemoved()
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.concurrency.ode.events;

import fr.cnes.sirius.patrius.math.ode.events.EventHandler;

/**
 * 
 * Event mock for concurrency test.
 * 
 * @author Julie Anton
 * 
 * @version $Id: EventMock.java 17909 2017-09-11 11:57:36Z bignon $
 * 
 * @since 1.1
 * 
 */
public class EventMock implements EventHandler {

    /** Event time. */
    private final double tevent;

    /**
     * Constructor
     * 
     * @param eventTime
     *        event time
     * 
     * @since 1.1
     */
    public EventMock(final double eventTime) {
        this.tevent = eventTime;
    }

    /** {@inheritDoc} */
    @Override
    public double g(final double t, final double[] y) {
        return this.tevent - t;
    }

    /** {@inheritDoc} */
    @Override
    public boolean shouldBeRemoved() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public Action eventOccurred(final double t, final double[] y, final boolean increasing, final boolean forward) {
        return Action.CONTINUE;
    }

    /** {@inheritDoc} */
    @Override
    public void resetState(final double t, final double[] y) {

    }

    /** {@inheritDoc} */
    @Override
    public void init(final double t0, final double[] y0, final double t) {
        // does nothing
    }

    /** {@inheritDoc} */
    @Override
    public int getSlopeSelection() {
        return 2;
    }
}
