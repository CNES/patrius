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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:284:01/09/2014:create a class to define a parameterizable function
 * VERSION::FA:411:10/02/2015:javadoc
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.parameter;

import fr.cnes.sirius.patrius.propagation.SpacecraftState;

/**
 * <p>
 * This class is used to define a parameterizable function. This function is composed of Parameter such as : f(t) = a *
 * t + b, with a and b Parameter
 * </p>
 * <p>
 * The method {@link #value(SpacecraftState) value} return the value of the function depending on the spacecraft state
 * state
 * </p>
 * 
 * @concurrency not thread-safe
 * @concurrency uses internal mutable attributes
 * 
 * @author auguief
 * @since 2.3
 * @version $Id: IParameterizableFunction.java 18069 2017-10-02 16:45:28Z bignon $
 */
public interface IParameterizableFunction extends IParameterizable {

    /**
     * Getting the value of the function.
     * 
     * @param state
     *        the spacecraft state
     * @return the value of the function.
     */
    double value(final SpacecraftState state);

}
