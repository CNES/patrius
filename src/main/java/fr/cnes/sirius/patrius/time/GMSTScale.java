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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.time;

import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.Constants;

/**
 * Greenwich Mean Sidereal Time.
 * <p>
 * The Greenwich Mean Sidereal Time is the hour angle between the meridian of Greenwich and mean equinox of date at 0h
 * UT1.
 * </p>
 * <p>
 * This is intended to be accessed thanks to the {@link TimeScalesFactory} class, so there is no public constructor.
 * </p>
 * 
 * @author Luc Maisonobe
 * @see AbsoluteDate
 * @since 5.1
 */
public class GMSTScale implements TimeScale {

    /** Duration of one julian day. */
    private static final double FULL_DAY = Constants.JULIAN_DAY;

    /** Duration of an half julian day. */
    private static final double HALF_DAY = Constants.JULIAN_DAY / 2.0;

    /** Coefficient for degree 0. */
    private static final double C0 = 24110.54841;

    /** Coefficient for degree 1. */
    private static final double C1 = 8640184.812866;

    /** Coefficient for degree 2. */
    private static final double C2 = 0.093104;

    /** Coefficient for degree 3. */
    private static final double C3 = -0.0000062;

    /** Twentieth century. */
    private static final int TWENTIETH = 2000;

    /** December. */
    private static final int DECEMBER = 12;
    
    /** 8. */
    private static final int EIGHT = 8;

    /** Universal Time 1 time scale. */
    private final UT1Scale ut1;

    /** Reference date for GMST. */
    private final AbsoluteDate referenceDate;

    // GST 1982: 24110.54841 + 8640184.812866 t + 0.093104 t2 - 6.2e-6 t3
    // GST 2000: 24110.5493771 + 8639877.3173760 tu + 307.4771600 te + 0.0931118 te2 - 0.0000062 te3 + 0.0000013 te4

    /**
     * Package private constructor for the factory.
     * 
     * @param ut1In
     *        Universal Time 1 scale
     */
    GMSTScale(final UT1Scale ut1In) {
        this.ut1 = ut1In;
        this.referenceDate = new AbsoluteDate(TWENTIETH, 1, 1, DECEMBER, 0, 0.0, ut1In);
    }

    /** {@inheritDoc} */
    @Override
    public double offsetFromTAI(final AbsoluteDate date) {

        // julian seconds since reference date
        final double ts = date.durationFrom(this.referenceDate);

        // julian centuries since reference date
        final double tc = ts / Constants.JULIAN_CENTURY;

        // GMST at 0h00 UT1 in seconds = offset with respect to UT1
        final double gmst0h = C0 + tc * (C1 + tc * (C2 + tc * C3));

        // offset with respect to TAI
        final double offset = gmst0h + this.ut1.offsetFromTAI(date);

        // normalize offset between -43200 and +43200 seconds
        return offset - FULL_DAY * MathLib.floor((offset + HALF_DAY) / FULL_DAY);

    }

    /** {@inheritDoc} */
    @Override
    public double offsetToTAI(final DateComponents date, final TimeComponents time) {
        final AbsoluteDate reference = new AbsoluteDate(date, time, TimeScalesFactory.getTAI());
        double offset = 0;
        for (int i = 0; i < EIGHT; i++) {
            offset = -this.offsetFromTAI(reference.shiftedBy(offset));
        }
        return offset;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "GMST";
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return this.getName();
    }

}
