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
 *
 * HISTORY
 * VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION::FA:280:08/10/2014:propagator modified in order to use the mu of gravitational forces
 * VERSION::DM:284:06/01/2015:New architecture for parameterizable Parameters
 * VERSION::FA:410:16/04/2015: Anomalies in the Patrius Javadoc
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity;

/**
 * This interface represents a gravitation attraction model.
 * It only contains the parameter name for central attraction coefficient.
 * 
 * @author Tiziana Sabatini
 * @version $Id: AttractionModel.java 18082 2017-10-02 16:54:17Z bignon $
 * @since 2.3
 */
public interface AttractionModel {

    /**
     * Parameter name for central attraction coefficient.
     */
    String MU = "central attraction coefficient";

    /**
     * Get the central attraction coefficient.
     * 
     * @return central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * 
     * @since 2.3.1
     */
    double getMu();
    
    /**
     * Set the central attraction coefficient.
     * @param muIn the central attraction coefficient.
     */
    void setMu(final double muIn);
    
    /**
     * Get the force multiplicative factor.
     * @return the force multiplicative factor
     */
    double getMultiplicativeFactor();
    
    /**
     * Set the multiplicative factor.
     * @param factor the factor to set.
     */
    void setMultiplicativeFactor(final double factor);
}
