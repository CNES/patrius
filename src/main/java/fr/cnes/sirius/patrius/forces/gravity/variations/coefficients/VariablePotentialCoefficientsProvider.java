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
 * @history Created 07/11/2012
 */
package fr.cnes.sirius.patrius.forces.gravity.variations.coefficients;

import java.util.Map;

import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * Interface used to provide gravity field coefficients.
 * 
 * @see VariableGravityFieldFactory
 * @author Rami Houdroge
 * @since 1.3
 * @version $Id: VariablePotentialCoefficientsProvider.java 17582 2017-05-10 12:58:16Z bignon $
 */
public interface VariablePotentialCoefficientsProvider {

    /**
     * Get the normalized variable potential coefficients
     * 
     * @return a list of coefficients
     */
    Map<Integer, Map<Integer, VariablePotentialCoefficientsSet>> getData();

    /**
     * Get the central body attraction coefficient.
     * 
     * @return mu (m<sup>3</sup>/s<sup>2</sup>)
     */
    double getMu();

    /**
     * Get the value of the central body reference radius.
     * 
     * @return ae (m)
     */
    double getAe();

    /**
     * Get the reference date of the file
     * 
     * @return reference date of gravity file
     */
    AbsoluteDate getDate();

    /**
     * Get the max degree available
     * 
     * @return max degree
     */
    int getMaxDegree();
}
