/**
 * Copyright 2021-2021 CNES
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
 * VERSION:4.7:DM:DM-2682:18/05/2021: Echelle de temps TDB (diff. PATRIUS - SPICE) 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.time;

import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.Constants;

/**
 * Barycentric Dynamic Time default model.
 * <p>
 * TDB = TT + 0.001658 sin(g) + 0.000014 sin(2g)seconds where g = 357.53 + 0.9856003 (JD - 2451545) degrees.
 * </p>
 * @see TDBModel
 * 
 * @author Emmanuel Bignon
 * 
 * @since 4.7
 */
public class TDBDefaultModel implements TDBModel {

    /** Coef. sin(g). */
    private static final double COEF_SIN = 0.001658;

    /** Coef. sin(2g). */
    private static final double COEF_SIN2 = 0.000014;

    /** C1. */
    private static final double C1 = 357.53;

    /** C2. */
    private static final double C2 = 0.9856003;

    /** {@inheritDoc} */
    @Override
    public double offsetFromTAI(final AbsoluteDate date) {
        final double dtDays = date.durationFrom(AbsoluteDate.J2000_EPOCH) / Constants.JULIAN_DAY;
        final double g = MathLib.toRadians(C1 + C2 * dtDays);
        return TimeScalesFactory.getTT().offsetFromTAI(date)
            + (COEF_SIN * MathLib.sin(g) + COEF_SIN2 * MathLib.sin(2 * g));
    }
}
