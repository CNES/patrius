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
 * @history creation 22/03/13
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.numerical;

import java.io.Serializable;

/**
 * Utility class that describes in a minimal fashion the structure of an additional state.
 * An instance contains the size of an additional state and its index in the state vector.
 * The instance <code>AdditionalStateInfo</code> is guaranteed to be immutable.
 */
public final class AdditionalStateInfo implements Serializable, Cloneable {

    /** Serializable UID. */
    private static final long serialVersionUID = 5113897187247487175L;

    /** Size of the additional state. */
    private final int size;

    /** Index of the additional state in the state vector. */
    private final int index;

    /**
     * Constructor.
     * 
     * @param sizeIn
     *        additional state size.
     * @param indexIn
     *        additional state state index in the state vector.
     */
    public AdditionalStateInfo(final int sizeIn, final int indexIn) {
        this.size = sizeIn;
        this.index = indexIn;
    }

    /**
     * Get the size of the additional state.
     * 
     * @return additional state size
     */
    public int getSize() {
        return this.size;
    }

    /**
     * Get the index of the additional state in the state vector.
     * 
     * @return additional state index in the state vector
     */
    public int getIndex() {
        return this.index;
    }

    /**
     * Copy of the AdditionalStateInfo.
     * 
     * @return a copy of the AdditionalStateInfo object.
     */
    //CHECKSTYLE: stop NoClone check
    @Override
    public AdditionalStateInfo clone() {
        //CHECKSTYLE: resume NoClone check
        return new AdditionalStateInfo(this.getSize(), this.getIndex());
    }

}
