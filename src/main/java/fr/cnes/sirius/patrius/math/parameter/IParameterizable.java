/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */
/*
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.parameter;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * This interface is used to handle a list of parameterizable parameters.
 * 
 * @author Pascal Parraud
 */

public interface IParameterizable extends Serializable {

    /**
     * Check if a parameter is supported.
     * 
     * @param param
     *        parameter to check
     * @return true if the parameter is supported
     */
    boolean supportsParameter(Parameter param);

    /**
     * Get the supported parameters.
     * 
     * @return list of supported parameters
     */
    @SuppressWarnings("PMD.LooseCoupling")
    ArrayList<Parameter> getParameters();
}
