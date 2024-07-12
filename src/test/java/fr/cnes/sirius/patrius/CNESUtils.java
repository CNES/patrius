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
 * @history 22/08/2012
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius;

import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.SolarActivityDataFactory;
import fr.cnes.sirius.patrius.forces.gravity.tides.coefficients.OceanTidesCoefficientsFactory;
import fr.cnes.sirius.patrius.forces.gravity.variations.coefficients.VariableGravityFieldFactory;

/**
 * Utility class
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: CNESUtils.java 17910 2017-09-11 11:58:16Z bignon $
 * 
 * @since 1.2
 * 
 */
public final class CNESUtils {

    /**
     * Utility class, private constructor
     */
    private CNESUtils() {

    }

    /**
     * Clear loaded data and set new root for resources.
     * 
     * Since new factories have been added in the orekit-addons project, it is necessary to clear them in order for the
     * tests to succeed. When these will be contributed, it is important to add the clear factory line to the org.orekit
     * Utils class setDataRoot method.
     * 
     * @param root
     *        resources directory
     * 
     * @see Utils#setDataRoot(String)
     * 
     * @since 1.2
     */
    public static void clearNewFactoriesAndCallSetDataRoot(final String root) {

        SolarActivityDataFactory.clearSolarActivityDataReaders();
        OceanTidesCoefficientsFactory.clearOceanTidesCoefficientsReaders();
        VariableGravityFieldFactory.clearVariablePotentialCoefficientsReaders();

        Utils.setDataRoot(root);

    }

}
