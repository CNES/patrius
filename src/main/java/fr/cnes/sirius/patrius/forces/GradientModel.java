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
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces;

/**
 * Interface for gradient model.
 * <p>
 * Gradient model provide information on partial derivatives computation.
 * </p>
 * 
 * @author Emmanuel Bignon
 */
public interface GradientModel {

    /**
     * This method returns true if the acceleration partial derivatives with
     * respect to position have to be computed.
     * 
     * @return true if the derivatives have to be computed, false otherwise
     */
    boolean computeGradientPosition();

    /**
     * This method returns true if the acceleration partial derivatives with
     * respect to velocity have to be computed.
     * 
     * @return true if the derivatives have to be computed, false otherwise
     */
    boolean computeGradientVelocity();
}
