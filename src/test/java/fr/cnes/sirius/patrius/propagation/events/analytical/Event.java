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

package fr.cnes.sirius.patrius.propagation.events.analytical;

import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * Event.
 * 
 * @version $Id: Event.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 2.3
 */
public class Event {

    /** Event name. */
    private final String name;

    /** Event time. */
    private final AbsoluteDate date;

    /** Convergence parameter. */
    private final double convergence;

    /**
     * Constructor.
     * 
     * @param name2
     *        event name
     * @param date2
     *        event time
     * @param convergence2
     *        convergence parameter
     */
    public Event(final String name2, final AbsoluteDate date2, final double convergence2) {
        this.name = name2;
        this.date = date2;
        this.convergence = convergence2;
    }

    @Override
    public boolean equals(final Object o) {
        final Event event = (Event) o;
        final double eps = this.convergence / 2.;
        return this.getClass().getName().equals(event.getClass().getName()) && this.name.equals(event.name)
            && Precision.equals(this.date.durationFrom(event.date), 0, eps);
    }
}