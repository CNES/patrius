/**
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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.6:DM:DM-2571:27/01/2021:[PATRIUS] Integrateur Stormer-Cowell 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.numerical;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Internal class for additional equations and tolerances management.
 */
public class AdditionalEquationsAndTolerances implements Externalizable {

     /** Serializable UID. */
    private static final long serialVersionUID = 3200948850076089681L;

    /** Additional equations. */
    private AdditionalEquations equations;

    /** Absolute tolerance. */
    private double[] absTol = null;

    /** Relative tolerance. */
    private double[] relTol = null;

    /** Position of equations in first order state vector. */
    private int index1stOrder;

    /** Position of equations in second order state vector. */
    private int index2ndOrder;

    /**
     * Empty constructor for {@link Externalizable} use.
     */
    public AdditionalEquationsAndTolerances() {
        this(null);
    }

    /**
     * Simple constructor.
     * 
     * @param equationsIn additional equations
     */
    public AdditionalEquationsAndTolerances(final AdditionalEquations equationsIn) {
        this.equations = equationsIn;
    }

    /**
     * Get the additional equations.
     * 
     * @return additional equations
     */
    public AdditionalEquations getEquations() {
        return this.equations;
    }

    /**
     * Set tolerances (no size check).
     * 
     * @param aTol absoluteTolerances
     * @param rTol relativeTolerances
     */
    public void setTolerances(final double[] aTol, final double[] rTol) {
        this.absTol = aTol.clone();
        this.relTol = rTol.clone();
    }

    /**
     * Set position of equations in first order state vector (initially unknown).
     * @param index index to set
     */
    public void setIndex1stOrder(final int index) {
        this.index1stOrder = index;
    }

    /**
     * Set position of equations in second order state vector (initially unknown).
     * @param index index to set
     */
    public void setIndex2ndOrder(final int index) {
        this.index2ndOrder = index;
    }
    
    /**
     * Returns absolute tolerance vector.
     * 
     * @return absolute tolerance vector
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public double[] getAbsTol() {
        return this.absTol;
    }

    /**
     * Returns relative tolerance vector.
     * 
     * @return relative tolerance vector
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public double[] getRelTol() {
        return this.relTol;
    }
    
    /**
     * Returns position of equations in first order state vector.
     * @return position of equations in first order state vector
     */
    public int getIndex1stOrder() {
        return index1stOrder;
    }
    
    /**
     * Returns position of equations in second order state vector.
     * @return position of equations in second order state vector
     */
    public int getIndex2ndOrder() {
        return index2ndOrder;
    }

    /** {@inheritDoc} */
    @Override
    public void writeExternal(final ObjectOutput oo) throws IOException {
        oo.writeInt(index1stOrder);
        oo.writeInt(index2ndOrder);
        // Tolerances are not stored because unused for step interpolation
        oo.writeObject(equations);
    }

    /** {@inheritDoc} */
    @Override
    public void readExternal(final ObjectInput oi) throws IOException, ClassNotFoundException {
        index1stOrder = oi.readInt();
        index2ndOrder = oi.readInt();
        equations = (AdditionalEquations) oi.readObject();
    }
}
