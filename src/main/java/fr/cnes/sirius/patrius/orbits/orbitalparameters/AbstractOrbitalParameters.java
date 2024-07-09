/**
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
 * @history creation 16/03/2015
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:396:16/03/2015:new architecture for orbital parameters
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.orbits.orbitalparameters;

import java.io.Serializable;

/**
 * Abstract class for orbital parameters.
 * 
 * @concurrency immutable
 * 
 * @author Emmanuel Bignon
 * @since 3.0
 * @version $Id: AbstractOrbitalParameters.java 18071 2017-10-02 16:46:39Z bignon $
 */
public abstract class AbstractOrbitalParameters implements IOrbitalParameters, Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = -5728128534060266772L;

    /** Value of mu used to compute position and velocity (m<sup>3</sup>/s<sup>2</sup>). */
    private final double mu;

    /**
     * Constructor.
     * 
     * @param muIn
     *        central attraction coefficient (m^3/s^2)
     */
    public AbstractOrbitalParameters(final double muIn) {
        this.mu = muIn;
    }

    /** {@inheritDoc} */
    @Override
    public double getMu() {
        return this.mu;
    }
}
