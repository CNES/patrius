/**
 * Copyright 2011-2017 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 */
package fr.cnes.sirius.patrius.time;

/**
 * International Atomic Time.
 * <p>
 * This is intended to be accessed thanks to the {@link TimeScalesFactory} class, so there is no public constructor.
 * </p>
 * 
 * @author Luc Maisonobe
 * @see AbsoluteDate
 */
public class TAIScale implements TimeScale {

    /**
     * Package private constructor for the factory.
     */
    TAIScale() {
        // Nothing to do
    }

    /** {@inheritDoc} */
    @Override
    public double offsetFromTAI(final AbsoluteDate taiTime) {
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public double offsetToTAI(final DateComponents date, final TimeComponents time) {
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "TAI";
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return this.getName();
    }

}