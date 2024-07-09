/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 */
package fr.cnes.sirius.patrius.math.ode;

/**
 * Interface to compute by finite difference Jacobian matrix for some parameter
 * when computing {@link JacobianMatrices partial derivatives equations}.
 * 
 * @version $Id: ParameterizedODE.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */

public interface ParameterizedODE extends Parameterizable {

    /**
     * Get parameter value from its name.
     * 
     * @param name
     *        parameter name
     * @return parameter value
     * @exception UnknownParameterException
     *            if parameter is not supported
     */
    double getParameter(String name);

    /**
     * Set the value for a given parameter.
     * 
     * @param name
     *        parameter name
     * @param value
     *        parameter value
     * @exception UnknownParameterException
     *            if parameter is not supported
     */
    void setParameter(String name, double value);

}
