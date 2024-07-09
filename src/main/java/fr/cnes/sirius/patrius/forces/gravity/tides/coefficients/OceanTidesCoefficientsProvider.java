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
 * @history Created 12/07/2012
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:241:01/10/2014:improved tides conception
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity.tides.coefficients;

import java.io.Serializable;

/**
 * Interface for ocean tides coefficients provider. <br>
 * The proper way to use this it to call the {@link OceanTidesCoefficientsFactory#getCoefficientsProvider()
 * getCoefficientProvider} method. Indeed, the {@link OceanTidesCoefficientsFactory} will determine the best reader to
 * use, depending on file available in the file system.
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: OceanTidesCoefficientsProvider.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.2
 * 
 */
public interface OceanTidesCoefficientsProvider extends Serializable {

    /**
     * Get the C<sub>lm</sub><sup>±</sup> and S<sub>lm</sub><sup>±</sup> for given wave
     * 
     * @param nDoodson
     *        doodson number
     * @param l
     *        order
     * @param m
     *        degree
     * @return double[4] array containing {C<sub>lm</sub><sup>+</sup>, C<sub>lm</sub><sup>-</sup>,
     *         S<sub>lm</sub><sup>+</sup>, S<sub>lm</sub><sup>-</sup>}
     */
    double[] getCpmSpm(final double nDoodson, final int l, final int m);

    /**
     * Get the C<sub>lm</sub><sup>±</sup> and ε<sub>lm</sub><sup>±</sup> for given wave
     * 
     * @param nDoodson
     *        doodson number doodson number
     * @param l
     *        order
     * @param m
     *        degree
     * @return double[4] array containing {C<sub>lm</sub><sup>+</sup>, C<sub>lm</sub><sup>-</sup>,
     *         ε<sub>lm</sub><sup>+</sup>, ε<sub>lm</sub><sup>-</sup>}
     */
    double[] getCpmEpm(final double nDoodson, final int l, final int m);

    /**
     * Get available Doodson numbers
     * 
     * @return array of Doodson numbers
     */
    double[] getDoodsonNumbers();

    /**
     * Get maximum degree for given wave and order
     * 
     * @param doodson
     *        number
     * @param order
     *        of wave
     * 
     * @return Max degree for given wave
     */
    int getMaxDegree(final double doodson, final int order);

    /**
     * Get min degree for given wave and order
     * 
     * @param doodson
     *        number
     * @param order
     *        of wave
     * 
     * @return Min degree for given wave
     */
    int getMinDegree(final double doodson, final int order);

    /**
     * Get maximum order for given wave
     * 
     * @param doodson
     *        number
     * 
     * @return Max order for given wave
     */
    int getMaxOrder(final double doodson);

}
