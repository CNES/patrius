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
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.time;

/**
 * Geocentric Coordinate Time.
 * <p>
 * Coordinate time at the center of mass of the Earth. This time scale depends linearly from {@link TTScale Terrestrial
 * Time}.
 * </p>
 * <p>
 * This is intended to be accessed thanks to the {@link TimeScalesFactory} class, so there is no public constructor.
 * </p>
 * 
 * @author Luc Maisonobe
 * @see AbsoluteDate
 */
public class TCGScale implements TimeScale {

    /** Serial UID. */
    private static final long serialVersionUID = -8954331196492191709L;

    /** LG rate. */
    private static final double LG_RATE = 6.969290134e-10;

    /**
     * Reference date for TCG.
     * <p>
     * The reference date is such that the four following instants are equal:
     * </p>
     * <ul>
     * <li>1977-01-01T00:00:32.184 TT</li>
     * <li>1977-01-01T00:00:32.184 TCG</li>
     * <li>1977-01-01T00:00:32.184 TCB</li>
     * <li>1977-01-01T00:00:00.000 TAI</li>
     * </ul>
     */
    private static final AbsoluteDate REFERENCE_DATE =
        new AbsoluteDate(1977, 01, 01, TimeScalesFactory.getTAI());

    /** Offset between TT and TAI scales. */
    private static final double TT_OFFSET =
        TimeScalesFactory.getTT().offsetFromTAI(REFERENCE_DATE);

    /**
     * Package private constructor for the factory.
     */
    TCGScale() {
        // Nothing to do
    }

    /** {@inheritDoc} */
    @Override
    public double offsetFromTAI(final AbsoluteDate date) {
        return TT_OFFSET + LG_RATE * date.durationFrom(REFERENCE_DATE);
    }

    /** {@inheritDoc} */
    @Override
    public double offsetToTAI(final DateComponents date, final TimeComponents time) {
        final AbsoluteDate reference = new AbsoluteDate(date, time, TimeScalesFactory.getTAI());
        double offset = 0;
        for (int i = 0; i < 3; i++) {
            offset = -this.offsetFromTAI(reference.shiftedBy(offset));
        }
        return offset;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "TCG";
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return this.getName();
    }

}
