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
 * HISTORY
* VERSION:4.6:DM:DM-2571:27/01/2021:[PATRIUS] Integrateur Stormer-Cowell 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.numerical;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.math.ode.nonstiff.cowell.SecondOrderStateMapper;

/**
 * Second-order / first order integrator state mapper.
 * 
 * @author Emmanuel Bignon
 * 
 * @since 4.6
 */
public class SecondOrderMapper implements SecondOrderStateMapper {

    /** Additional equations and tolerances. */
    private List<AdditionalEquationsAndTolerances> addEquationsAndTolerances;

    /** First order state vector dimension. */
    private int dimension1;

    /** Second order state vector dimension. */
    private int dimension2;
    
    /**
     * Empty constructor used for {@link Externalizable} implementation.
     */
    public SecondOrderMapper() {
        this(new ArrayList<AdditionalEquationsAndTolerances>());
    }
    
    /**
     * Constructor.
     * @param addEquationsAndTolerances additional equations and tolerances
     */
    public SecondOrderMapper(final List<AdditionalEquationsAndTolerances> addEquationsAndTolerances) {
        this.addEquationsAndTolerances = addEquationsAndTolerances;
        this.dimension1 = 6;
        for (final AdditionalEquationsAndTolerances stateAndTol : addEquationsAndTolerances) {
            this.dimension1 += stateAndTol.getEquations().getFirstOrderDimension();
        }
        this.dimension2 = 3;
        for (final AdditionalEquationsAndTolerances stateAndTol : addEquationsAndTolerances) {
            this.dimension2 += stateAndTol.getEquations().getSecondOrderDimension();
        }
    }
    
    /** {@inheritDoc} */
    @Override
    public double[] buildFullState(final double[] y,
            final double[] yDot) {
        final double[] res = new double[dimension1];
        // Position
        System.arraycopy(y, 0, res, 0, 3);
        // Velocity
        System.arraycopy(yDot, 0, res, 3, 3);
        // Additional states
        for (final AdditionalEquationsAndTolerances stateAndTol : addEquationsAndTolerances) {
            final int dim2 = stateAndTol.getEquations().getSecondOrderDimension();
            final double[] yAddtionalState = new double[dim2];
            final double[] yDotAddtionalState = new double[dim2];
            System.arraycopy(y, stateAndTol.getIndex2ndOrder(), yAddtionalState, 0, dim2);
            System.arraycopy(yDot, stateAndTol.getIndex2ndOrder(), yDotAddtionalState, 0, dim2);
            final double[] additionalState = stateAndTol.getEquations().buildAdditionalState(yAddtionalState,
                    yDotAddtionalState);
            System.arraycopy(additionalState, 0, res, stateAndTol.getIndex1stOrder(), additionalState.length);
        }
        return res;
    }

    /** {@inheritDoc} */
    @Override
    public double[] extractY(final double[] fullState) {
        final double[] y = new double[dimension2];
        // Position
        System.arraycopy(fullState, 0, y, 0, 3);
        // Additional states
        for (final AdditionalEquationsAndTolerances stateAndTol : addEquationsAndTolerances) {
            final int dim1 = stateAndTol.getEquations().getFirstOrderDimension();
            final double[] additionalState = new double[dim1];
            System.arraycopy(fullState, stateAndTol.getIndex1stOrder(), additionalState, 0, dim1);
            final double[] yAddtionalState = stateAndTol.getEquations().extractY(additionalState);
            System.arraycopy(yAddtionalState, 0, y, stateAndTol.getIndex2ndOrder(), yAddtionalState.length);
        }
        return y;
    }

    /** {@inheritDoc} */
    @Override
    public double[] extractYDot(final double[] fullState) {
        final double[] yDot = new double[dimension2];
        // Velocity
        System.arraycopy(fullState, 3, yDot, 0, 3);
        // Additional states
        for (final AdditionalEquationsAndTolerances stateAndTol : addEquationsAndTolerances) {
            final int dim1 = stateAndTol.getEquations().getFirstOrderDimension();
            final double[] additionalState = new double[dim1];
            System.arraycopy(fullState, stateAndTol.getIndex1stOrder(), additionalState, 0, dim1);
            final double[] yAddtionalState = stateAndTol.getEquations().extractYDot(additionalState);
            System.arraycopy(yAddtionalState, 0, yDot, stateAndTol.getIndex2ndOrder(), yAddtionalState.length);
        }
        return yDot;
    }

    /** {@inheritDoc} */
    @Override
    public void writeExternal(final ObjectOutput oo) throws IOException {
        oo.writeInt(dimension1);
        oo.writeInt(dimension2);
        oo.writeInt(addEquationsAndTolerances.size());
        for (int i = 0; i < addEquationsAndTolerances.size(); i++) {
            oo.writeObject(addEquationsAndTolerances.get(i));
        }
    }

    /** {@inheritDoc} */
    @Override
    public void readExternal(final ObjectInput oi) throws IOException, ClassNotFoundException {
        dimension1 = oi.readInt();
        dimension2 = oi.readInt();
        addEquationsAndTolerances = new ArrayList<AdditionalEquationsAndTolerances>();
        final int n = oi.readInt();
        for (int i = 0; i < n; i++) {
            addEquationsAndTolerances.add((AdditionalEquationsAndTolerances) oi.readObject());
        }
    }
}
