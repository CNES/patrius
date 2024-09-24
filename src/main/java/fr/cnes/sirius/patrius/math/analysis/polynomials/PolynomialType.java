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
 * HISTORY
 * VERSION:4.12:DM:DM-102:17/08/2023:[PATRIUS] Ajout méthode getNature à l'enum PolynomialType
 * VERSION:4.11.1:DM:DM-88:30/06/2023:[PATRIUS] Complement FT 3319
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.polynomials;

/**
 * Describe the polynomial function type.
 * 
 * @author Thibaut BONIT
 *
 * @version $Id$
 *
 * @since 4.11
 */
public enum PolynomialType {

    /** Classical polynomial function. */
    CLASSICAL("CLASSICAL_POLYNOMIAL_FUNCTION"),

    /** Chebyshev polynomial function. */
    CHEBYSHEV("CHEBYSHEV_POLYNOMIAL_FUNCTION");

    /** The nature of the polynomial function. */
    private final String nature;

    /**
     * Simple constructor.
     * 
     * @param natureIn
     *        the nature of the polynomial function
     */
    private PolynomialType(final String natureIn) {
        this.nature = natureIn;
    }

    /**
     * Getter for the nature of the polynomial function.
     * 
     * @return the nature of the polynomial function
     */
    public String getNature() {
        return this.nature;
    }
}
