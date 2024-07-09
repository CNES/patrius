/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
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
 * @history creation 01/09/2014
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:284:01/09/2014:create a class for derivative function parameterizable
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.parameter;

import fr.cnes.sirius.patrius.propagation.SpacecraftState;

/**
 * This class is used to define a derivative function parameterizable.
 * 
 * @concurrency not thread-safe
 * @concurrency uses internal mutable attributes
 * 
 * @author auguief
 * @since 2.3
 * @version $Id: IParamDiffFunction.java 18069 2017-10-02 16:45:28Z bignon $
 */
public interface IParamDiffFunction extends IParameterizableFunction {

    /**
     * Compute the derivative value with respect to the input parameter.
     * 
     * @param p
     *        parameter
     * @param s
     *        current state
     * @return the derivative value
     */
    double derivativeValue(final Parameter p, final SpacecraftState s);

    /**
     * Tell if the function is differentiable by the given parameter.
     * 
     * @param p
     *        function parameter
     * @return true if the function is differentiable by the given parameter.
     */
    boolean isDifferentiableBy(final Parameter p);

}
