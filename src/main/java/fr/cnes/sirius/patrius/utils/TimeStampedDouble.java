/**
 * Copyright 2023-2023 CNES
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
 * VERSION:4.13:DM:DM-30:08/12/2023:[PATRIUS] Deplacement et modification des
 * classes TimeStampedString et TimeStampedDouble
 * VERSION:4.13:FA:FA-112:08/12/2023:[PATRIUS] Probleme si Earth est utilise comme corps pivot pour mar097.bsp
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.utils;

import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeStamped;

/**
 * An array of {@link Double} object that as a {@link AbsoluteDate} attached to it.
 *
 * @author tgalpin
 * 
 * @since 4.13
 */
public class TimeStampedDouble implements TimeStamped {

    /** Doubles array. */
    private final double[] doubles;

    /** Date. */
    private final AbsoluteDate date;

    /**
     * Constructor.
     *
     * @param doubleIn
     *        double to be stored
     * @param date
     *        {@link AbsoluteDate} attached to the value
     *
     */
    public TimeStampedDouble(final double doubleIn, final AbsoluteDate date) {
        this(new double[] { doubleIn }, date);
    }
    
    /**
     * Constructor.
     *
     * @param doublesIn
     *        array of doubles to be stored
     * @param date
     *        {@link AbsoluteDate} attached to the value
     *
     */
    public TimeStampedDouble(final double[] doublesIn, final AbsoluteDate date) {
        super();
        this.doubles = doublesIn;
        this.date = date;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getDate() {
        return this.date;
    }

    /**
     * Getter for the array of doubles.
     *
     * @return the doubles
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    // Reason: performance
    public double[] getDoubles() {
        return this.doubles;
    }
    
    /**
     * Getter for the double attached to the date.<br>
     * If there is a more than one component array attached, first component is returned.
     *
     * @return a double associated with the date
     */
    public double getDouble() {
        return this.doubles[0];
    }
}
