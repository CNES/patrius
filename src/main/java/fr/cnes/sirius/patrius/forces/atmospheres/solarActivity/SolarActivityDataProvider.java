/**
 * 
 * Copyright 2011-2022 CNES
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
 * 
 * @history Created 25/04/2012
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:FA:FA-3009:15/11/2021:[PATRIUS] IllegalArgumentException SolarActivityToolbox
 * VERSION:4.5:DM:DM-2445:27/05/2020:optimisation de SolarActivityReader 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.atmospheres.solarActivity;

import java.io.Serializable;
import java.util.SortedMap;

import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Interface for solar activity data providers, to be used for atmosphere models
 * 
 * @author Rami Houdroge
 * @version $Id: SolarActivityDataProvider.java 17582 2017-05-10 12:58:16Z bignon $
 * @since 1.1
 */
public interface SolarActivityDataProvider extends Serializable {

    /**
     * Get minimum date at which both flux and ap values are available
     * 
     * @return a date
     */
    AbsoluteDate getMinDate();

    /**
     * Get maximum date at which both flux and ap values are available
     * 
     * @return a date
     */
    AbsoluteDate getMaxDate();

    /**
     * Get minimum date of flux values
     * 
     * @return a date
     */
    AbsoluteDate getFluxMinDate();

    /**
     * Get maximum date of flux values
     * 
     * @return a date
     */
    AbsoluteDate getFluxMaxDate();

    /**
     * Get minimum date of ap / kp values
     * 
     * @return a date
     */
    AbsoluteDate getApKpMinDate();

    /**
     * Get maximum date of ap / kp values
     * 
     * @return a date
     */
    AbsoluteDate getApKpMaxDate();

    /**
     * Get raw instant flux values between the given dates
     * 
     * @param date1
     *        first date
     * @param date2
     *        second date
     * @return submap of instant flux values sorted according to date
     * @throws PatriusException
     *         if no solar activity at date
     */
    SortedMap<AbsoluteDate, Double> getInstantFluxValues(final AbsoluteDate date1,
                                                         final AbsoluteDate date2) throws PatriusException;

    /**
     * Get instant flux values at the given dates (possibly interpolated)
     * 
     * @param date
     *        user date
     * @return instant flux values
     * @throws PatriusException
     *         if no solar activity at date
     */
    double getInstantFluxValue(final AbsoluteDate date) throws PatriusException;

    /**
     * Get ap / kp values between the given dates
     * 
     * @param date1
     *        first date
     * @param date2
     *        second date
     * @return submap of instant flux values sorted according to date
     * @throws PatriusException
     *         if no solar activity at date
     */
    SortedMap<AbsoluteDate, Double[]> getApKpValues(final AbsoluteDate date1,
                                                    final AbsoluteDate date2) throws PatriusException;

    /**
     * Get Ap value at given user date
     * 
     * @param date
     *        user date
     * @return Ap value
     * @throws PatriusException thrown if computation failed
     */
    double getAp(AbsoluteDate date) throws PatriusException;

    /**
     * Get Kp value at given user date
     * 
     * @param date
     *        user date
     * @return Kp value
     * @throws PatriusException thrown if computation failed
     */
    double getKp(AbsoluteDate date) throws PatriusException;

    /**
     * Check that solar data (flux) are available in the user range [start; end].
     * 
     * @param start range start date
     * @param end range end date
     */
    default void checkFluxValidity(final AbsoluteDate start, final AbsoluteDate end) {
        if (start.compareTo(getFluxMinDate()) < 0) {
            throw new PatriusExceptionWrapper(new PatriusException(
                    PatriusMessages.NO_SOLAR_ACTIVITY_AT_DATE, start, getFluxMinDate(), getFluxMaxDate()));
        }
        if (end.compareTo(getFluxMaxDate()) > 0) {
            throw new PatriusExceptionWrapper(new PatriusException(
                    PatriusMessages.NO_SOLAR_ACTIVITY_AT_DATE, end, getFluxMinDate(), getFluxMaxDate()));
        }
    }

    /**
     * Check that solar data (ap/kp) are available in the user range [start; end].
     * 
     * @param start range start date
     * @param end range end date
     */
    default void checkApKpValidity(final AbsoluteDate start, final AbsoluteDate end) {
        if (start.compareTo(getApKpMinDate()) < 0) {
            throw new PatriusExceptionWrapper(new PatriusException(
                    PatriusMessages.NO_SOLAR_ACTIVITY_AT_DATE, start, getApKpMinDate(), getApKpMaxDate()));
        }
        if (end.compareTo(getApKpMaxDate()) > 0) {
            throw new PatriusExceptionWrapper(new PatriusException(
                    PatriusMessages.NO_SOLAR_ACTIVITY_AT_DATE, end, getApKpMinDate(), getApKpMaxDate()));
        }
    }

    /**
     * Returns the step for Ap/Kp values.
     * @return the step for Ap/Kp values
     * @throws PatriusException thrown if computation failed
     */
    double getStepApKp() throws PatriusException;
    
    /**
     * Returns the step for F107 values.
     * @return the step for F107 values
     * @throws PatriusException thrown if computation failed
     */
    double getStepF107() throws PatriusException;
}
