/**
 * Copyright 2021-2021 CNES
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
 */
package fr.cnes.sirius.patrius.bodies;

import java.io.Serializable;


/**
 * IAU coefficients for generic IAU pole model for pole and prime meridian orientations.
 * <p>
 * This class represents the coefficients compliant with the report of the IAU/IAG Working Group on Cartographic
 * Coordinates and Rotational Elements of the Planets and Satellites (WGCCRE). These definitions are common for all
 * recent versions of this report published every three years.
 * </p>
 * <p>
 * The precise values of pole direction and W angle coefficients may vary from publication year as models are adjusted.
 * The latest value of constants for implementing this interface can be found in the <a
 * href="http://astrogeology.usgs.gov/Projects/WGCCRE/">working group site</a>.
 * </p>
 * <p>Expressions for pole and prime meridian orientation have the same structure. They are defined with class
 * {@link IAUPoleCoefficients1D}.</p>
 * 
 * @author Emmanuel Bignon
 * 
 * @since 4.7
 */
public class IAUPoleCoefficients implements Serializable {

    /** Serial UID. */
    private static final long serialVersionUID = -8490972570774890485L;

    /** Coefficients for &alpha;<sub>0</sub> component. */
    private final IAUPoleCoefficients1D alpha0Coeffs;

    /** Coefficients for &delta;<sub>0</sub> component. */
    private final IAUPoleCoefficients1D delta0Coeffs;

    /** Coefficients for W component. */
    private final IAUPoleCoefficients1D wCoeffs;
    
    /**
     * Constructor.
     * @param alpha0Coeffs coefficients for &alpha;<sub>0</sub> component
     * @param delta0Coeffs coefficients for &delta;<sub>0</sub> component
     * @param wCoeffs coefficients for W component
     * @see IAUPoleCoefficients1D
     */
    public IAUPoleCoefficients(final IAUPoleCoefficients1D alpha0Coeffs,
            final IAUPoleCoefficients1D delta0Coeffs,
            final IAUPoleCoefficients1D wCoeffs) {
        this.alpha0Coeffs = alpha0Coeffs;
        this.delta0Coeffs = delta0Coeffs;
        this.wCoeffs = wCoeffs;
    }

    /**
     * Returns the coefficients for &alpha;<sub>0</sub> component.
     * @return the coefficients for &alpha;<sub>0</sub> component
     */
    public IAUPoleCoefficients1D getAlpha0Coeffs() {
        return alpha0Coeffs;
    }

    /**
     * Returns the coefficients for &delta;<sub>0</sub> component.
     * @return the coefficients for &delta;<sub>0</sub> component
     */
    public IAUPoleCoefficients1D getDelta0Coeffs() {
        return delta0Coeffs;
    }

    /**
     * Returns the coefficients for W component.
     * @return the coefficients for W component
     */
    public IAUPoleCoefficients1D getWCoeffs() {
        return wCoeffs;
    }
}
