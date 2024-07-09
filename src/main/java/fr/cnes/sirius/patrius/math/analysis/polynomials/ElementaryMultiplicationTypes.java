/**
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
 * @history Created 23/05/2012
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:400:17/03/2015: use class FastMath instead of class Math
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 *
 */
package fr.cnes.sirius.patrius.math.analysis.polynomials;

import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;

/**
 * This class is used to represent elementary trigonometric polynomials cos and sin.
 * 
 * @concurrency unconditionally thread-safe
 * 
 * @concurrency.comment utility class without attributes
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: ElementaryMultiplicationTypes.java 17603 2017-05-18 08:28:32Z bignon $
 * 
 * @since 1.2
 */
public final class ElementaryMultiplicationTypes {

    /**
     * This is a utility class, its constructor is private.
     */
    private ElementaryMultiplicationTypes() {
    }

    /**
     * Elementary trigonometric types
     */
    public enum ElementaryType {
        /**
         * Used to represent the elementary trigonometric polynomial cos(t)
         */
        COS,
        /**
         * Used to represent the elementary trigonometric polynomial sin(t)
         */
        SIN
    }

    /**
     * This method provides the {@link UnivariateFunction} cos(intermediateOrder * omega * x) or sin
     * 
     * @return function as a {@link UnivariateFunction}
     * 
     * @param intermediateType
     *        cos or sin
     * @param intermediateOrder
     *        order of elementary function
     * @param period
     *        period such as <code>omega = 2 * pi / period</code>
     * 
     * @see FourierDecompositionEngine
     */
    public static UnivariateFunction componentProvider(final ElementaryType intermediateType,
                                                       final int intermediateOrder, final double period) {

        return new UnivariateFunction(){
            /** {@inheritDoc} */
            @Override
            public double value(final double x) {

                // Output variable
                final double temp;

                // Compute value depending on intermediateType
                switch (intermediateType) {
                    case COS:
                        temp = MathLib.cos(MathLib.divide(x * intermediateOrder * 2 * FastMath.PI, period));
                        break;
                    case SIN:
                        temp = MathLib.sin(MathLib.divide(x * intermediateOrder * 2 * FastMath.PI, period));
                        break;
                    default:
                        // this should never happen
                        throw new PatriusRuntimeException(PatriusMessages.INTERNAL_ERROR, null);

                }

                return temp;
            }
        };
    }
}
