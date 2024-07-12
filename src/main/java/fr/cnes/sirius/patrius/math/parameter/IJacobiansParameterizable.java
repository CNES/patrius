/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
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
 * @history creation 01/09/2014
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:284:01/09/2014:create a class to define jacobian function parameters
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.parameter;

import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class is used to define jacobian function parameters.
 * 
 * @concurrency not thread-safe
 * @concurrency uses internal mutable attributes
 * 
 * @author auguief
 * @since 2.3
 * @version $Id: IJacobiansParameterizable.java 18069 2017-10-02 16:45:28Z bignon $
 */
public interface IJacobiansParameterizable extends IParameterizable {

    /**
     * Compute acceleration derivatives with respect to state parameters.
     * 
     * @param s
     *        spacecraft state
     * @param dAccdPos
     *        acceleration derivatives with respect to position
     * @param dAccdVel
     *        acceleration derivatives with respect to velocity
     * @exception PatriusException
     *            if derivatives cannot be computed
     */
    void addDAccDState(SpacecraftState s, double[][] dAccdPos, double[][] dAccdVel) throws PatriusException;

    /**
     * Compute acceleration derivatives with respect to additional parameters.
     * 
     * @param s
     *        spacecraft state
     * @param param
     *        the parameter with respect to which derivatives are required
     * @param dAccdParam
     *        acceleration derivatives with respect to specified parameters
     * @exception PatriusException
     *            if derivatives cannot be computed
     */
    void addDAccDParam(SpacecraftState s, Parameter param, double[] dAccdParam) throws PatriusException;

    /**
     * Check if a jacobian parameter is supported.
     * 
     * @param param
     *        parameter to check
     * @return true if the parameter is supported.
     */
    boolean supportsJacobianParameter(final Parameter param);

}
