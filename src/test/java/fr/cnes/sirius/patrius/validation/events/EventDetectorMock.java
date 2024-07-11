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
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:454:24/11/2015:Unimplemented method shouldBeRemoved()
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.validation.events;

import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.AbstractDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * This class represents an event detector that triggers an event when actual date = target date. It is based on the
 * validation class with the same name in the commons-math.
 * </p>
 * 
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id: EventDetectorMock.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.1
 * 
 */
public class EventDetectorMock extends AbstractDetector {

    /**
     * Serial UID.
     */
    private static final long serialVersionUID = 5289866150238571119L;

    /**
     * Expected date.
     */
    private final AbsoluteDate trueDate;

    /**
     * Actual date.
     */
    private AbsoluteDate computedDate;

    /**
     * Constructor.
     * 
     * @param date
     *        date
     * @param maxCheck
     *        maxCheck
     * @param threshold
     *        threshold
     */
    public EventDetectorMock(final AbsoluteDate date, final double maxCheck,
        final double threshold) {
        super(maxCheck, threshold);
        this.trueDate = date;
        this.computedDate = AbsoluteDate.PAST_INFINITY;
    }

    /**
     * Returns the date at which the event happened.
     * 
     * @return the computed date
     */
    public AbsoluteDate getEventTime() {
        return this.computedDate;
    }

    /**
     * Returns the date at which the event was expected to happen.
     * 
     * @return the expected date
     */
    public AbsoluteDate getTheoreticalTime() {
        return this.trueDate;
    }

    @Override
    public
            Action
            eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                   throws PatriusException {
        this.computedDate = s.getDate();
        return Action.CONTINUE;
    }

    @Override
    public final double g(final SpacecraftState s) throws PatriusException {
        final double rez = s.getDate().durationFrom(this.trueDate);
        return rez;
    }

    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        // does nothing
    }

    /**
     * 
     * @see fr.cnes.sirius.patrius.propagation.events.AbstractDetector#shouldBeRemoved()
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
