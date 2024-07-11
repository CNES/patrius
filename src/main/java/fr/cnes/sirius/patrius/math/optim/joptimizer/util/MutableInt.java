/**
 * Copyright 2019-2020 CNES
 * Copyright 2011-2014 JOptimizer
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
 * VERSION:4.6:DM:DM-2591:27/01/2021:[PATRIUS] Intigration et validation JOptimizer
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.optim.joptimizer.util;

/**
 * A mutable {@code int} wrapper.
 * <p>
 * Note that as MutableInt does not extend Integer, it is not treated by String.format as an Integer parameter.
 *
 * @see Integer
 * 
 * @since 4.6
 */
class MutableInt {

    /** The mutable value. */
    private int value;

    /**
     * Constructs a new MutableInt with the specified value.
     *
     * @param val the initial value to store
     */
    public MutableInt(final int val) {
        super();
        this.value = val;
    }

    /**
     * Increments the value.
     */
    public void increment() {
        value++;
    }

    /**
     * Decrements the value.
     */
    public void decrement() {
        value--;
    }
    
    /**
     * Get the value
     * @return value
     */
    public int getValue() {
        return value;
    }

}