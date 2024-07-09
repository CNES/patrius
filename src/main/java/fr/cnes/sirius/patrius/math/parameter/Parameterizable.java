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
 * Copyright 2010-2011 Centre National d'Études Spatiales
 */
/*
 * HISTORY
* VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:284:01/10/2014:New architecture for parameterizable Parameters
 * VERSION::DM:505:19/08/2015:replace HashMap by ArrayList
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.parameter;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Simple class providing a list and method for handling {@link Parameter parameters}.
 * 
 * @concurrency not thread-safe
 * @concurrency uses internal mutable attributes
 * 
 * @author auguief
 * @since 2.3
 * @version $Id: Parameterizable.java 18069 2017-10-02 16:45:28Z bignon $
 */
@SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
public class Parameterizable implements IParameterizable {

    /** Serializable UID. */
    private static final long serialVersionUID = 3408804493769459294L;

    /**
     * List of parameters.
     * Only one instance of a parameter is allowed in the list.
     */
    @SuppressWarnings("PMD.LooseCoupling")
    private final ArrayList<Parameter> parameters;

    /**
     * Simple constructor.
     */
    public Parameterizable() {
        this.parameters = new ArrayList<Parameter>();
    }

    /**
     * Simple constructor to add in the internal list the given parameters.
     * <p>
     * Note: Only one instance of each parameter is allowed in the list.
     * </p>
     * 
     * @param params
     *        the parameters to add in the internal list
     */
    public Parameterizable(final Parameter... params) {
        this.parameters = new ArrayList<Parameter>();
        this.addAllParameters(params);
    }

    /**
     * Simple constructor to add in the internal list the parameters contained in the given list.
     * <p>
     * Note: Only one instance of each parameter is allowed in the list.
     * </p>
     * 
     * @param paramList
     *        the list of parameters to add in the internal list
     */
    @SuppressWarnings("PMD.LooseCoupling")
    public Parameterizable(final ArrayList<Parameter> paramList) {
        this.parameters = new ArrayList<Parameter>();
        // Call this method instead of storing the list directly, to avoid adding twice the same
        // parameter
        this.addAllParameters(paramList);
    }

    /**
     * Simple constructor to add in the internal list the parameters contained in the given function
     * list.
     * <p>
     * Note: Only one instance of each parameter is allowed in the list.
     * </p>
     * 
     * @param paramDiffFunctions
     *        the list of functions containing the parameters to add in the internal list
     */
    public Parameterizable(final IParamDiffFunction... paramDiffFunctions) {
        // Instantiate the list
        this.parameters = new ArrayList<Parameter>();
        // Loop on functions
        for (final IParamDiffFunction f : paramDiffFunctions) {
            // Storing function parameter in the global list
            this.addAllParameters(f.getParameters());
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean supportsParameter(final Parameter param) {
        // Check
        return this.parameters.contains(param);
    }

    /** {@inheritDoc} */
    @Override
    public ArrayList<Parameter> getParameters() {
        return this.parameters;
    }

    /**
     * Add the given parameter in the internal list.
     * <p>
     * Note: Only one instance of the parameter is allowed in the list.
     * </p>
     * 
     * @param param
     *        parameter to add
     */
    protected void addParameter(final Parameter param) {
        // If the parameter doesn't exist in the list
        if (!this.supportsParameter(param)) {
            // Add it
            this.parameters.add(param);
        }
    }

    /**
     * Add the given parameters in the internal list.
     * <p>
     * Note: Only one instance of each parameter is allowed in the list.
     * </p>
     *
     * @param params
     *        parameters to add
     */
    protected final void addAllParameters(final Parameter... params) {
        // Loop on all the parameters
        for (final Parameter param : params) {
            this.addParameter(param);
        }
    }

    /**
     * Add the given parameters in the internal list.
     * <p>
     * Note: Only one instance of each parameter is allowed in the list.
     * </p>
     *
     * @param params
     *        parameters to add
     */
    protected final void addAllParameters(final Collection<Parameter> params) {
        // Loop on all the parameters
        for (final Parameter param : params) {
            this.addParameter(param);
        }
    }
}
