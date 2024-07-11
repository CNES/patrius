/**
 * 
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
 * 
 * @history Created 20/08/2012
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2445:27/05/2020:optimisation de SolarActivityReader 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:131:28/10/2013:New Implementation of MSISE data
 * VERSION::FA:183:14/03/2014:Improved javadoc
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.atmospheres;

import java.io.Serializable;

import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Container for solar activity data, compatible with {@link MSISE2000} Atmosphere model.
 * 
 * This model needs mean and instantaneous solar flux and geomagnetic incides to
 * compute the local density.
 * 
 * @author Rami Houdroge
 * @since 2.1
 * @version $Id: MSISE2000InputParameters.java 17582 2017-05-10 12:58:16Z bignon $
 */
public interface MSISE2000InputParameters extends Serializable {

    /**
     * Gets the available data range minimum date.
     * 
     * @return the minimum date.
     */
    AbsoluteDate getMinDate();

    /**
     * Gets the available data range maximum date.
     * 
     * @return the maximum date.
     */
    AbsoluteDate getMaxDate();

    /**
     * Get the value of the instantaneous solar flux.
     * 
     * @param date
     *        the current date
     * @return the instantaneous solar flux
     * @exception PatriusException
     *            if the date is out of range of available data
     */
    double getInstantFlux(AbsoluteDate date) throws PatriusException;

    /**
     * Get the 81 day average of F10.7 flux.
     * 
     * @param date
     *        the current date
     * @return the mean solar flux
     * @exception PatriusException
     *            if the date is out of range of available data
     */
    double getMeanFlux(AbsoluteDate date) throws PatriusException;

    /**
     * Get the array containing the 7 ap values<br>
     * 
     * @param date
     *        the current date
     * @return the Ap values
     * @exception PatriusException
     *            if the date is out of range of available data
     */
    double[] getApValues(AbsoluteDate date) throws PatriusException;
    
    /**
     * This methods throws an exception if the user did not provide solar activity on the provided interval [start,
     * end].
     * All models should implement their own method since the required data interval depends on the model.
     * @param start range start date
     * @param end range end date
     * @throws PatriusException thrown if some solar activity data is missing
     */
    void checkSolarActivityData(final AbsoluteDate start, final AbsoluteDate end) throws PatriusException;
}
