/**
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
 * @history 01/10/2014:creation
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:269:01/10/2014:piecewise linear interpolations
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.interpolation;

/**
 * Class representing a TrivariateGridInterpolator for linear interpolation in dimension 3.
 * 
 * @author Sophie LAURENS
 * @concurrency unconditionally thread-safe
 * @version $Id: TriLinearIntervalsInterpolator.java 17603 2017-05-18 08:28:32Z bignon $
 * @since 2.3
 * 
 */
public final class TriLinearIntervalsInterpolator implements TrivariateGridInterpolator {

    /**
     * 
     * Compute an interpolating function for the dataset.
     * 
     * @param xval
     *        1st component for the interpolation points.
     * @param yval
     *        2nd component for the interpolation points.
     * @param zval
     *        3rd component for the interpolation points.
     * @param fval
     *        function values for the interpolation points.
     * @return a function which interpolates the dataset.
     */
    @Override
    public TriLinearIntervalsFunction interpolate(final double[] xval, final double[] yval, final double[] zval,
                                                  final double[][][] fval) {
        return new TriLinearIntervalsFunction(xval, yval, zval, fval);
    }

}
