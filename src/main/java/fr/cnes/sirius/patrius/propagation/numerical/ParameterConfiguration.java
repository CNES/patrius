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
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::DM:570:05/04/2017:add PropulsiveProperty and TankProperty
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.numerical;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.parameter.IJacobiansParameterizable;
import fr.cnes.sirius.patrius.math.parameter.Parameter;

/**
 * Simple container associating a parameter name with a step to compute its jacobian
 * and the provider thant manages it.
 * 
 * @author V&eacute;ronique Pommier-Maurussane
 */
@SuppressWarnings("PMD.NullAssignment")
public class ParameterConfiguration implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 2247518849090889379L;

    /** Jacobian Parameter name. */
    private final Parameter jacobianParameter;

    /** Parameter step for finite difference computation of partial derivative with respect to that parameter. */
    private final double hP;

    /** Provider handling this parameter. */
    private IJacobiansParameterizable provider;

    /**
     * Parameter name and step pair constructor.
     * 
     * @param parameter
     *        parameter name
     * @param hPIn
     *        parameter step
     */
    public ParameterConfiguration(final Parameter parameter, final double hPIn) {
        this.jacobianParameter = parameter;
        this.hP = hPIn;
        this.provider = null;
    }

    /**
     * Get parameter.
     * 
     * @return parameter
     */
    public Parameter getParameter() {
        return this.jacobianParameter;
    }

    /**
     * Get parameter step.
     * 
     * @return hP parameter step
     */
    public double getHP() {
        return this.hP;
    }

    /**
     * Set the povider handling this parameter.
     * 
     * @param providerIn
     *        provider handling this parameter
     */
    public void setProvider(final IJacobiansParameterizable providerIn) {
        this.provider = providerIn;
    }

    /**
     * Get the povider handling this parameter.
     * 
     * @return provider handling this parameter
     */
    public IJacobiansParameterizable getProvider() {
        return this.provider;
    }

}
