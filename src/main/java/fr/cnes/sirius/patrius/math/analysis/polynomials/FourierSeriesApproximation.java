/**
 * Copyright 2011-2017 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 * @history Created 02/04/2012
 */
package fr.cnes.sirius.patrius.math.analysis.polynomials;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;

/**
 * Holder for a {@link UnivariateFunction} and its {@link FourierSeries} approximation
 * 
 * @concurrency immutable if function is immutable
 * 
 * @see FourierDecompositionEngine
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: FourierSeriesApproximation.java 17603 2017-05-18 08:28:32Z bignon $
 * 
 * @since 1.2
 * 
 */
public class FourierSeriesApproximation implements Serializable {

    /**
     * Generated Serial Version UID
     */
    private static final long serialVersionUID = 173810426201847455L;

    /**
     * Original function
     */
    private final UnivariateFunction function;

    /**
     * Fourier series approximation
     */
    private final FourierSeries fourier;

    /**
     * Constructor. A holder for a {@link UnivariateFunction} and its approximated {@link FourierSeries}
     * 
     * @param fun
     *        univariate real function
     * @param fou
     *        approximated fourier series
     */
    protected FourierSeriesApproximation(final UnivariateFunction fun, final FourierSeries fou) {
        this.function = fun;
        this.fourier = fou;
    }

    /**
     * @return the function
     */
    public UnivariateFunction getFunction() {
        return this.function;
    }

    /**
     * @return the fourier series approximation
     */
    public FourierSeries getFourier() {
        return this.fourier;
    }

    /**
     * @return period of functions
     */
    public double getPeriod() {
        return this.fourier.getPeriod();
    }

    /**
     * Get String representation of Fourier Series
     * 
     * @return string
     */
    @Override
    public String toString() {
        return this.fourier.toString();
    }
}