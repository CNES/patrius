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
 * VERSION:4.7:FA:FA-2853:18/05/2021:Erreurs documentation javadoc suite au refactoring modèle d'atmosphère DTM 
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
 * Container for solar activity data, compatible with all DTM Atmosphere models.
 * 
 * This model needs mean and instantaneous solar flux and geomagnetic incides to
 * compute the local density. Mean solar flux is (for the moment) represented by
 * the F10.7 indices. Instantaneous flux can be set to the mean value if the
 * data is not available. Geomagnetic acivity is represented by the Kp indice,
 * which goes from 1 (very low activity) to 9 (high activity).
 * <p>
 * All needed solar activity data can be found on the <a href="http://sec.noaa.gov/Data/index.html"> NOAA (National
 * Oceanic and Atmospheric Administration) website.</a>
 * </p>
 * 
 * @author Fabien Maussion
 */
public interface DTMInputParameters extends Serializable {

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
     * Get the value of the mean solar flux.
     * 
     * @param date
     *        the current date
     * @return the mean solar flux
     * @exception PatriusException
     *            if the date is out of range of available data
     */
    double getMeanFlux(AbsoluteDate date) throws PatriusException;

    /**
     * Get the value of the 3 hours geomagnetic index.
     * With a delay of 3 hours at pole to 6 hours at equator using:
     * delay=6-abs(lat)*0.033 (lat in deg.)
     * 
     * @param date
     *        the current date
     * @return the 3H geomagnetic index
     * @exception PatriusException
     *            if the date is out of range of available data
     */
    double getThreeHourlyKP(AbsoluteDate date) throws PatriusException;

    /**
     * Get the last 24H mean geomagnetic index.
     * 
     * @param date
     *        the current date
     * @return the 24H geomagnetic index
     * @exception PatriusException
     *            if the date is out of range of available data
     */
    double get24HoursKp(AbsoluteDate date) throws PatriusException;
    
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
