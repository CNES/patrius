/**
 * Copyright 2023-2023 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * HISTORY
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies.bsp.spice;

import java.util.Arrays;

import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class class defines the object CounterArray and all the methods to manipulate it.
 * These methods where originally defined in ZZCTR.for
 * <p>
 * A counter array consists of SpiceCommon.ctrsiz elements representing cascading counters. The fastest counter is at
 * index 0, the slowest counter is at index CTRSIZ. At the start of counting all counter array elements are set to
 * INTMIN. In the process of counting the fastest element is incremented by one. As with any cascading counters when the
 * fastest counter reaches INTMAX it rolls back to INTMIN and the next counter is incremented by 1. When all counters
 * reach INTMAX, Increment signals an error.
 * </p>
 *
 * @author T0281925
 *
 * @since 4.11
 */
public class CounterArray {

    /** Maximum value possible for a counter array. */
    private static final int HIGH = Integer.MAX_VALUE;

    /** Minimum value possible for a counter array. */
    private static final int LOW = Integer.MIN_VALUE;

    /** Counter array. */
    private final int[] counter = new int[SpiceCommon.CTRSIZ];

    /**
     * Initializes the counter taking into account if it is a subsystem counter
     * or an user counter.<br>
     * Makes the role of ZZCTRUIN and ZZCTRSIN from the SPICE library.
     * 
     * @param type
     *        Type of CounterArray
     * @throws PatriusException
     *         if the CounterArray type is not recognized
     */
    public CounterArray(final String type) throws PatriusException {
        if ("USER".equalsIgnoreCase(type)) {
            // For user methods, ensure update at the first check
            this.counter[0] = HIGH;
            this.counter[1] = HIGH;
        } else if ("SUBSYSTEM".equalsIgnoreCase(type)) {
            // For subsystems methods
            this.counter[0] = LOW;
            this.counter[1] = LOW;
        } else {
            throw new PatriusException(PatriusMessages.PDB_WRONG_COUNTER_ARRAY_TYPE, type);
        }
    }

    /**
     * Increment counter array.
     * 
     * @throws PatriusException
     *         if the counter is not about to overflow
     */
    public void increment() throws PatriusException {
        // Check counter is not about to overflow
        if (this.counter[0] == HIGH && this.counter[1] == HIGH) {
            throw new PatriusException(PatriusMessages.PDB_COUNTER_ARRAY_OVERFLOW);
        }

        // If first element is to overflow, reset it and increment the second.
        // If not, increment the first element.
        if (this.counter[0] == HIGH) {
            this.counter[0] = LOW;
            this.counter[1] += 1;
        } else {
            this.counter[0] += 1;
        }
    }

    /**
     * Check and update, if needed, counter array.
     * 
     * @param newCounter
     *        to compare with the current counter
     * @return true if there has been an update
     */
    public boolean checkAndUpdate(final CounterArray newCounter) {
        final boolean update = !this.equals(newCounter);
        if (update) {
            this.counter[0] = newCounter.counter[0];
            this.counter[1] = newCounter.counter[1];
        }
        return update;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Arrays.hashCode(this.counter);
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        // Check the object could be a Counter array
        boolean isEqual = false;

        if (obj == this) {
            // Identity
            isEqual = true;
        } else if ((obj != null) && (obj.getClass() == this.getClass())) {
            final CounterArray other = (CounterArray) obj;
            isEqual = Arrays.equals(this.counter, other.counter);
        }

        return isEqual;
    }
}
