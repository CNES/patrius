/**
 * 
 * Copyright 2011-2017 CNES
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
 * HISTORY
* VERSION:4.8:DM:DM-2929:15/11/2021:[PATRIUS] Harmonisation des modeles de troposphere 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:88:18/11/2013: interface creation
 * VERSION::FA:410:16/04/2015: Anomalies in the Patrius Javadoc
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.signalpropagation;

import fr.cnes.sirius.patrius.signalpropagation.ionosphere.IonosphericCorrection;
import fr.cnes.sirius.patrius.signalpropagation.troposphere.TroposphericCorrection;

/**
 * This interface is an angular correction model enabling the computation of the satellite
 * elevation angular correction.
 * 
 * @see TroposphericCorrection
 * @see IonosphericCorrection
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 2.1
 * 
 */
public interface AngularCorrection {

    /**
     * Computes the correction for the signal elevation.
     * 
     * @param elevation
     *        the elevation of the satellite [rad]
     * @return the angular correction of the signal elevation [rad]
     */
    double computeElevationCorrection(final double elevation);
}
