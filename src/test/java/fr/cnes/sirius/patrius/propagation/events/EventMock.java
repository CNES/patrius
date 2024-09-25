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
 * HISTORY
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:454:24/11/2015:Unimplemented method shouldBeRemoved()
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import fr.cnes.sirius.patrius.events.AbstractDetector;
import fr.cnes.sirius.patrius.events.EventDetector;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Mock event added to cover special cases which can occur during the event detection.
 * <ul>
 * <li>- method evaluateStep of EventState</li>
 * <li>- OrekitException in g() method</li>
 * </ul>
 * 
 * @author antonj
 * @author cardosop
 * 
 * 
 */
public class EventMock extends AbstractDetector {

    /** Serializable UID. */
    private static final long serialVersionUID = -5816981327544267839L;

    /**
     * Make g fail.
     */
    private boolean makeGFail = false;

    /**
     * Make g fail after n g calls (counter).
     */
    private int gFailCallsCounter;

    /**
     * G call counter.
     */
    private int gCallCounter;

    private final AbsoluteDate date;

    /**
     * Constructor.
     * 
     * @param date
     *        date
     */
    public EventMock(final AbsoluteDate date, final int maxIntervalCheck, final double threshold) {
        super(maxIntervalCheck, threshold);
        this.date = date;
    }

    @Override
    public double g(final SpacecraftState s) throws PatriusException {

        this.gCallCounter++;

        if (this.makeGFail && (this.gCallCounter >= this.gFailCallsCounter)) {
            throw new PatriusException(PatriusMessages.INTERNAL_ERROR);
        }

        return MathLib.cos(100 * s.getDate().durationFrom(this.date));
    }

    @Override
    public
            Action
            eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                   throws PatriusException {
        return Action.CONTINUE;
    }

    /**
     * Makes a call to g() raise an OrekitException (or not).
     * 
     * @param mgf
     *        true enables OrekitException, false disables.
     * @param count
     *        fails only at g call number "count".
     */
    public void makeGFail(final boolean mgf, final int count) {
        this.makeGFail = mgf;
        this.gFailCallsCounter = count;
    }

    /**
     * Does nothing.
     * 
     * @see fr.cnes.sirius.patrius.events.EventDetector#init(fr.cnes.sirius.patrius.propagation.SpacecraftState,
     *      fr.cnes.sirius.patrius.time.AbsoluteDate)
     */
    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        // does nothing
    }

    /**
     * 
     * @see fr.cnes.sirius.patrius.events.AbstractDetector#shouldBeRemoved()
     */
    @Override
    public boolean shouldBeRemoved() {
        return false;
    }

    @Override
    public EventDetector copy() {
        return null;
    }
}
