/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */
/*
 *
 * HISTORY
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:144:31/10/2013:Added possibility of storing UT1-TAI instead of UT1-UTC
 * VERSION::FA:1301:06/09/2017:Generalized EOP history
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.eop;

import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeStamped;
import fr.cnes.sirius.patrius.utils.exception.TimeStampedCacheException;

/**
 * Interface for retrieving Earth Orientation Parameters history throughout a large time range.
 * 
 * @author Luc Maisonobe
 */
public interface EOPHistory extends Iterable<TimeStamped> {

    /**
     * Add an Earth Orientation Parameters entry.
     * 
     * @param entry
     *        entry to add
     */
    void addEntry(final EOPEntry entry);

    /**
     * Get the number of entries in the history.
     * 
     * @return number of entries in the history
     */
    int size();

    /**
     * Get the date of the first available Earth Orientation Parameters.
     * 
     * @return the start date of the available data
     */
    AbsoluteDate getStartDate();

    /**
     * Get the date of the last available Earth Orientation Parameters.
     * 
     * @return the end date of the available data
     */
    AbsoluteDate getEndDate();

    /**
     * Get the UT1-UTC value.
     * <p>
     * The data provided comes from the IERS files. It is smoothed data.
     * </p>
     * 
     * @param date
     *        date at which the value is desired
     * @return UT1-UTC in seconds (0 if date is outside covered range)
     */
    double getUT1MinusUTC(final AbsoluteDate date);

    /**
     * Get the UT1-TAI value.
     * <p>
     * The data provided comes from the IERS files. It is smoothed data.
     * </p>
     * 
     * @param date
     *        date at which the value is desired
     * @return UT1-TAI in seconds (0 if date is outside covered range)
     */
    double getUT1MinusTAI(final AbsoluteDate date);

    /**
     * Get the LoD (Length of Day) value.
     * <p>
     * The data provided comes from the IERS files. It is smoothed data.
     * </p>
     * 
     * @param date
     *        date at which the value is desired
     * @return LoD in seconds (0 if date is outside covered range)
     */
    double getLOD(final AbsoluteDate date);

    /**
     * Return the EOP interpolation method.
     * 
     * @return eop interpolation method
     */
    EOPInterpolators getEOPInterpolationMethod();

    /**
     * Get the pole IERS Reference Pole correction.
     * <p>
     * The data provided comes from the IERS files. It is smoothed data.
     * </p>
     * 
     * @param date
     *        date at which the correction is desired
     * @return pole correction ({@link PoleCorrection#NULL_CORRECTION
     *         PoleCorrection.NULL_CORRECTION} if date is outside covered range)
     * @throws TimeStampedCacheException
     *         for TimeStampedCache problems
     */
    PoleCorrection getPoleCorrection(final AbsoluteDate date) throws TimeStampedCacheException;

    /**
     * Get the correction to the nutation parameters.
     * <p>
     * The data provided comes from the IERS files. It is smoothed data.
     * </p>
     * 
     * @param date
     *        date at which the correction is desired
     * @return nutation correction ({@link NutationCorrection#NULL_CORRECTION
     *         NutationCorrection.NULL_CORRECTION} if date is outside covered range)
     */
    NutationCorrection getNutationCorrection(final AbsoluteDate date);

}
