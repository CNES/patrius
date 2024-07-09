/**
 * Copyright 2011-2021 CNES
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
* VERSION:4.7:DM:DM-2653:18/05/2021:generalisation des sequences et correction/refonte des sequences de segments 
 * VERSION:4.4:DM:DM-2208:04/10/2019:[PATRIUS] Ameliorations de Leg, LegsSequence et AbstractLegsSequence
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1948:14/11/2018:new attitude leg sequence design
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.utils.legs;

import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.time.TimeStamped;

/**
 * A <i>leg</i> is an object which is valid between two dates.
 * <p>
 * It’s also {@link TimeStamped} by the beginning date.
 * <p>
 * Please note a {@code Leg} <strong>should be immutable</strong>, and please see
 * {@code Leg#copy(AbsoluteDateInterval)} method.
 *
 * @see TimeStamped
 * @see LegsSequence
 *
 * @author Julien Anxionnat (CNES — DSO/DV/MP)
 * 
 * @since 4.7
 */
@SuppressWarnings("PMD.ShortClassName")
public interface Leg extends TimeStamped {

    /** Default nature. */
    public static String LEG_NATURE = "LEG";

    /**
     * Returns the nature of the leg.
     * @return The “nature” of the leg.
     */
    default String getNature() {
        return LEG_NATURE;
    }

    /**
     * Returns the time interval of the leg.
     * @return the time interval of the leg.
     */
    AbsoluteDateInterval getTimeInterval();

    /**
     * Returns the leg start date.
     * @return the leg start date
     */
    @Override
    default AbsoluteDate getDate() {
        return this.getTimeInterval().getLowerData();
    }

    /**
     * Returns the leg end date.
     * @return the leg end date
     */
    default AbsoluteDate getEnd() {
        return this.getTimeInterval().getUpperData();
    }

    /**
     * Creates a new leg from this one.
     *
     * @param newInterval
     *        The time interval of the leg to create
     *
     * @return A new {@code Leg} valid on provided interval
     *
     * @throws IllegalArgumentException
     *         If the given {@code newInterval} is problematic (too long, too short, whatever)
     */
    Leg copy(final AbsoluteDateInterval newInterval);

    /**
     * Returns a nice {@link String} representation.
     * @return A nice {@link String} representation.
     */
    default String toPrettyString() {
        // A default method cannot override a method from java.lang.Object
        return this.getTimeInterval() + " — " + this.getNature();
    }
}
