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
 * @history creation 26/04/2012
 *
 * HISTORY
* VERSION:4.4:DM:DM-2097:04/10/2019:[PATRIUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:93:31/03/2014:changed partial derivatives API
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.numerical;

import java.util.ArrayList;

import fr.cnes.sirius.patrius.math.parameter.Parameter;

/**
 * Interface for classes that can provide parameters for computing jacobians.
 * 
 * @author Rami Houdroge
 * @since 2.2
 * @version $Id: JacobianParametersProvider.java 18084 2017-10-02 16:55:24Z bignon $
 */
public interface JacobianParametersProvider {

    /**
     * Get the list of all jacobian parameters supported.
     * 
     * @return the list of additional parameters for which the jacobians can be computed.
     */
    @SuppressWarnings("PMD.LooseCoupling")
    ArrayList<Parameter> getJacobianParameters();

}
