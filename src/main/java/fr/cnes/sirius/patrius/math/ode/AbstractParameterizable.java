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

import java.util.ArrayList;
import java.util.Collection;

/**
 * This abstract class provides boilerplate parameters list.
 * 
 * @version $Id: AbstractParameterizable.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */

public abstract class AbstractParameterizable implements Parameterizable {

    /** List of the parameters names. */
    private final Collection<String> parametersNames;

    /**
     * Simple constructor.
     * 
     * @param names
     *        names of the supported parameters
     */
    protected AbstractParameterizable(final String... names) {
        this.parametersNames = new ArrayList<String>();
        for (final String name : names) {
            this.parametersNames.add(name);
        }
    }

    /**
     * Simple constructor.
     * 
     * @param names
     *        names of the supported parameters
     */
    protected AbstractParameterizable(final Collection<String> names) {
        this.parametersNames = new ArrayList<String>();
        this.parametersNames.addAll(names);
    }

    /** {@inheritDoc} */
    @Override
    public Collection<String> getParametersNames() {
        return this.parametersNames;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSupported(final String name) {
        for (final String supportedName : this.parametersNames) {
            if (supportedName.equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if a parameter is supported and throw an IllegalArgumentException if not.
     * 
     * @param name
     *        name of the parameter to check
     * @exception UnknownParameterException
     *            if the parameter is not supported
     * @see #isSupported(String)
     */
    public void complainIfNotSupported(final String name) {
        if (!this.isSupported(name)) {
            throw new UnknownParameterException(name);
        }
    }

}
