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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.time;

/**
 * Terrestrial Time as defined by IAU(1991) recommendation IV.
 * <p>
 * Coordinate time at the surface of the Earth. IT is the successor of Ephemeris Time TE.
 * </p>
 * <p>
 * By convention, TT = TAI + 32.184 s.
 * </p>
 * <p>
 * This is intended to be accessed thanks to the {@link TimeScalesFactory} class, so there is no public constructor.
 * </p>
 * 
 * @author Luc Maisonobe
 * @see AbsoluteDate
 */
public class TTScale implements TimeScale {

    /** UTC - TAI (s). */
    private static final double UTC_MINUS_TAI = 32.184;

    /**
     * Package private constructor for the factory.
     */
    TTScale() {
        // Nothing to do
    }

    /** {@inheritDoc} */
    @Override
    public double offsetFromTAI(final AbsoluteDate date) {
        return UTC_MINUS_TAI;
    }

    /** {@inheritDoc} */
    @Override
    public double offsetToTAI(final DateComponents date, final TimeComponents time) {
        return -UTC_MINUS_TAI;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "TT";
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return this.getName();
    }

}
