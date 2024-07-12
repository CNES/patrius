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
 * @history modified 08/12/2014
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.4:DM:DM-2097:04/10/2019:[PATRIUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:241:08/12/2014:improved tides conception
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity.tides;

import java.io.Serializable;

import fr.cnes.sirius.patrius.forces.gravity.tides.TidesStandards.TidesStandard;

/**
 * Interface that provides terrestrial tides inputs.
 * 
 * @author Julie Anton, Gerald Mercadier
 * 
 * @version $Id: ITerrestrialTidesDataProvider.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.2
 */
public interface ITerrestrialTidesDataProvider extends Serializable {

    /**
     * Get second degree Love number for the third body perturbation.
     * 
     * @return a table of Love numbers
     */
    double[] getAnelasticityCorrectionLoveNumber2();

    /**
     * Get third degree Love number for the third body perturbation.
     * 
     * @return a table of Love numbers
     */
    double[] getAnelasticityCorrectionLoveNumber3();

    /**
     * Get second degree Love number for the ellipticity perturbation.
     * 
     * @return a table of Love numbers
     */
    double[] getEllipticityCorrectionLoveNumber2();

    /**
     * Get the frequency corrections as a table of Love number corrections associated to a Doodson number i.e. a wave.
     * 
     * @return a table of frequency corrections (for the considered wave, double[i][0] is the real part and double[i][1]
     *         is the imaginary part of Love number correction).
     */
    double[][] getFrequencyCorrection();

    /**
     * Get constant coefficients coming from the luni solar nutation theory in order to compute the fundamental
     * arguments.
     * 
     * @return a table of nutation coefficients
     */
    double[][] getNutationCoefficients();

    /**
     * Get the Doodson numbers used by the standard.
     * 
     * @return table of Doodson numbers.
     */
    double[] getDoodsonNumbers();

    /**
     * @return the TidesStandard enum for this standard.
     */
    TidesStandard getStandard();
}
