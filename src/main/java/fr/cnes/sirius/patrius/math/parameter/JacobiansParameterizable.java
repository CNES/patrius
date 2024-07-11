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
 * @history creation 16/12/2014
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:284:02/12/2014:create abstract class JacobiansParameterizable
 * VERSION::DM:505:19/08/2015:replace HashMap by ArrayList
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.parameter;

import java.util.ArrayList;

/**
 * <p>
 * Abstract class to define generic function of {@link IJacobiansParameterizable }.
 * </p>
 * 
 * @concurrency not thread-safe
 * @concurrency uses internal mutable attributes
 * 
 * @author Charlotte Maggiorani
 * 
 * @version $Id: JacobiansParameterizable.java 18069 2017-10-02 16:45:28Z bignon $
 * 
 * @since 2.3.1
 * 
 */
//CHECKSTYLE: stop AbstractClassName check
@SuppressWarnings("PMD.AbstractNaming")
public abstract class JacobiansParameterizable extends Parameterizable implements IJacobiansParameterizable {
    // CHECKSTYLE: resume AbstractClassName check

    /** Serializable UID. */
    private static final long serialVersionUID = 152206315837807025L;

    /**
     * List of jacobian parameters.
     * Only one instance of a parameter is allowed in the list.
     * */
    @SuppressWarnings("PMD.LooseCoupling")
    private final ArrayList<Parameter> jacobiansParameters;

    /**
     * Simple constructor.
     */
    public JacobiansParameterizable() {
        super();
        this.jacobiansParameters = new ArrayList<>();
    }

    /**
     * Simple constructor.
     * 
     * @param params
     *        the supported parameters
     */
    public JacobiansParameterizable(final Parameter... params) {
        super(params);
        this.jacobiansParameters = new ArrayList<>();
    }

    /**
     * Simple constructor.
     * 
     * @param paramList
     *        the supported parameters
     */
    @SuppressWarnings("PMD.LooseCoupling")
    public JacobiansParameterizable(final ArrayList<Parameter> paramList) {
        super(paramList);
        this.jacobiansParameters = new ArrayList<>();
    }

    /**
     * Simple constructor.
     * 
     * @param paramDiffFunctions
     *        IParamDiffFunction with their parameters
     */
    public JacobiansParameterizable(final IParamDiffFunction... paramDiffFunctions) {
        super(paramDiffFunctions);
        this.jacobiansParameters = new ArrayList<>();
    }

    /** {@inheritDoc} */
    @Override
    public boolean supportsJacobianParameter(final Parameter param) {
        return this.jacobiansParameters.contains(param);
    }

    /**
     * Add a new parameter in the jacobians parameters list.
     * 
     * @param param
     *        parameter
     */
    protected void addJacobiansParameter(final Parameter param) {

        // if the parameter doesn't exist in the list
        if (!this.supportsJacobianParameter(param)) {
            // add it to parameter list
            this.addParameter(param);
            // add it to jacobians parameter list
            this.jacobiansParameters.add(param);
        }
    }

    /**
     * Add a list of parameters in the jacobians parameters list.
     * 
     * @param params
     *        list of parameters
     */
    @SuppressWarnings("PMD.LooseCoupling")
    protected void addJacobiansParameter(final ArrayList<Parameter> params) {
        for (final Parameter p : params) {
            this.addJacobiansParameter(p);
        }
    }

    /**
     * Add a parameters in the jacobians parameters list.
     * 
     * @param params
     *        parameters
     */
    protected void addJacobiansParameter(final Parameter... params) {
        for (final Parameter p : params) {
            this.addJacobiansParameter(p);
        }
    }
}
