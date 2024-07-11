/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2445:27/05/2020:optimisation de SolarActivityReader 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
/*
 * 
 */
package fr.cnes.sirius.patrius.forces.atmospheres;

import java.io.Serializable;

import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Interface for solar activity and magnetic activity data.
 * 
 * @author Fabien Maussion
 */
public interface JB2006InputParameters extends Serializable {

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
     * Get the value of the instantaneous solar flux index
     * (1e<sup>-22</sup>*Watt/(m<sup>2</sup>*Hertz)).
     * Tabular time 1.0 day earlier.
     * 
     * @param date
     *        the current date
     * @return the instantaneous F10.7 index
     * @exception PatriusException
     *            if the date is out of range of available data
     */
    double getF10(AbsoluteDate date) throws PatriusException;

    /**
     * Get the value of the mean solar flux.
     * Averaged 81-day centered F10.7 B index on the input time.
     * 
     * @param date
     *        the current date
     * @return the mean solar flux F10.7B index
     * @exception PatriusException
     *            if the date is out of range of available data
     */
    double getF10B(AbsoluteDate date) throws PatriusException;

    /**
     * Get the EUV index (26-34 nm) scaled to F10.
     * Tabular time 1 day earlier.
     * 
     * @param date
     *        the current date
     * @return the the EUV S10 index
     * @exception PatriusException
     *            if the date is out of range of available data
     */
    double getS10(AbsoluteDate date) throws PatriusException;

    /**
     * Get the EUV 81-day averaged centered index.
     * 
     * @param date
     *        the current date
     * @return the the mean EUV S10B index
     * @exception PatriusException
     *            if the date is out of range of available data
     */
    double getS10B(AbsoluteDate date) throws PatriusException;

    /**
     * Get the MG2 index scaled to F10.
     * 
     * @param date
     *        the current date
     * @return the the EUV S10 index
     * @exception PatriusException
     *            if the date is out of range of available data
     */
    double getXM10(AbsoluteDate date) throws PatriusException;

    /**
     * Get the MG2 81-day average centered index.
     * Tabular time 5.0 days earlier.
     * 
     * @param date
     *        the current date
     * @return the the mean EUV S10B index
     * @exception PatriusException
     *            if the date is out of range of available data
     */
    double getXM10B(AbsoluteDate date) throws PatriusException;

    /**
     * Get the Geomagnetic planetary 3-hour index A<sub>p</sub>.
     * Tabular time 6.7 hours earlier.
     * 
     * @param date
     *        the current date
     * @return the A<sub>p</sub> index
     * @exception PatriusException
     *            if the date is out of range of available data
     */
    double getAp(AbsoluteDate date) throws PatriusException;
    
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
