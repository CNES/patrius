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
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.utils;

import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeStamped;

/**
 * An array of {@link String} object that as a {@link AbsoluteDate} attached to it.
 *
 * @author tgalpin
 * 
 * @since4.13
 */
public class TimeStampedString implements TimeStamped {

    /** String array. */
    private final String[] strings;

    /** Date. */
    private final AbsoluteDate date;

    /**
     * Constructor.
     *
     * @param stringsIn array of strings to be stored
     * @param date
     *        {@link AbsoluteDate} attached to the value
     *
     */
    public TimeStampedString(final String[] stringsIn, final AbsoluteDate date) {
        super();
        this.strings = stringsIn;
        this.date = date;
    }
    
    /**
     * Constructor.
     *
     * @param stringIn string to be stored
     * @param date
     *        {@link AbsoluteDate} attached to the value
     *
     */
    public TimeStampedString(final String stringIn, final AbsoluteDate date) {
        super();
        this.strings = new String[] { stringIn };
        this.date = date;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getDate() {
        return this.date;
    }

    /**
     * Get the strings.
     *
     * @return the array of strings
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    // Reason: performance
    public String[] getStrings() {
        return this.strings;
    }
    
    /**
     * Get the string (first component of the array if it was constructed with an array).
     *
     * @return the string
     */
    public String getString() {
        return this.strings[0];
    }

}
