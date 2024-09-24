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
 * HISTORY
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11.1:FA:FA-86:30/06/2023:[PATRIUS] Retours JE Alice
 * VERSION:4.11.1:DM:DM-88:30/06/2023:[PATRIUS] Complement FT 3319
 * VERSION:4.11:DM:DM-3319:22/05/2023:[PATRIUS] Rendre QuaternionPolynomialSegment plus generique et coherent
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.math.analysis.polynomials;
import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;


/**
 * Represents an interface for polynomial functions.
 * 
 * @author Alice Latourte
 *
 * @version $Id$
 *
 * @since 4.11
 *
 */
public interface PolynomialFunctionInterface extends UnivariateFunction {

    /**
     * Return the polynomial degree.
     * 
     * @return the polynomial degree
     */
    public int getDegree();

    /**
     * Get the polynomial coefficients.
     * 
     * @return the polynomial coefficients
     */
    public double[] getCoefficients();

    /**
     * Return the derivative date polynomial function.
     * 
     * @return the derivative date polynomial function
     */
    public PolynomialFunctionInterface derivative();

    /**
     * Return the primitive polynomial function.
     *
     * @param abscissa0 the abscissa of interest
     * @param ordinate0 the function value at abscissa0
     * @return the primitive polynomial function
     */
    public PolynomialFunctionInterface primitive(final double abscissa0, final double ordinate0);

    /**
     * Return the type of this polynomial function.
     * 
     * @return the type of this polynomial function
     */
    public PolynomialType getPolynomialType();

}
