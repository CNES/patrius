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
 */
package fr.cnes.sirius.patrius.bodies.bsp.spice;

import java.util.Arrays;

import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class class defines the object CounterArray and all the methods to manipulate it.
 * These methods where originally defined in ZZCTR.for
 * 
 * A counter array consists of SpiceCommon.ctrsiz elements representing
 * cascading counters. The fastest counter is at index 0, the
 * slowest counter is at index CTRSIZ. At the start of counting all
 * counter array elements are set to INTMIN. In the process of
 * counting the fastest element is incremented by one. As with any
 * cascading counters when the fastest counter reaches INTMAX it
 * rolls back to INTMIN and the next counter is incremented by 1.
 * When all counters reach INTMAX, Increment signals an error.
 * <p>
 *
 * @author
 *
 * @since 4.11
 */
public class CounterArray {

    /**
     * Maximum value possible for a counter array
     */
    private static final int HIGH = Integer.MAX_VALUE;
    
    /**
     * Minimum value possible for a counter array
     */
    private static final int LOW = Integer.MIN_VALUE;
    
    /**
     * Counter array
     */
    private final int[] counter = new int[SpiceCommon.CTRSIZ];

    /**
     * Initializes the counter taking into account if it is a subsystem counter
     * or an user counter.
     * Makes the role of ZZCTRUIN and ZZCTRSIN from the SPICE library
     * @param type Type of CounterArray
     * @throws PatriusException 
     */
    public CounterArray(final String type) throws PatriusException {
        if ("USER".equalsIgnoreCase(type)) {
            // For user methods, ensure update at the first check
            counter[0] = HIGH;
            counter[1] = HIGH;
        } else if ("SUBSYSTEM".equalsIgnoreCase(type)) {
            // For subsystems methods
            counter[0] = LOW;
            counter[1] = LOW;
        } else {
            throw new PatriusException(PatriusMessages.PDB_WRONG_COUNTER_ARRAY_TYPE,type);
        }

    }

    /**
     * Increment counter array.
     * @throws PatriusException 
     */
    public void increment() throws PatriusException {
        // Check counter is not about to overflow
        if (counter[0] == HIGH && counter[1] == HIGH) {
            throw new PatriusException(PatriusMessages.PDB_COUNTER_ARRAY_OVERFLOW);
        }
        
        // If first element is to overflow, reset it and increment the second.
        // If not, increment the first element.
        if (counter[0] == HIGH) {
            counter[0] = LOW;
            counter[1] += 1;
        } else {
            counter[0] += 1;
        }
    }

    /**
     * Check and update, if needed, counter array.
     * @param newCounter to compare with the current counter
     * @return true if there has been an update
     */
    public boolean checkAndUpdate(final CounterArray newCounter) {

        final boolean update = !this.equals(newCounter);

        if (update) {
            counter[0] = newCounter.counter[0];
            counter[1] = newCounter.counter[1];
        }

        return update;
    }
    
    /**
     * Calculate the hash code for a CounterArray object
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        // Perform a calculation of th hashcode
        result = prime * result + Arrays.hashCode(counter);
        return result;
    }

    /**
     * Check if 2 counter arrays are the same or not
     */
    @Override
    public boolean equals(final Object obj) {
        // Check the object could be a Counter array
        if (this == obj) {
            return true;
        }          
        if (obj == null) {
            return false;
        }           
        if (getClass() != obj.getClass()) {
            return false;
        }           
        // Instantiate the counter array and compare
        final CounterArray other = (CounterArray) obj;
        return Arrays.equals(counter, other.counter);
    }
}
