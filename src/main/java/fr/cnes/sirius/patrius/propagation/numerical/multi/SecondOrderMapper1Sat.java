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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.6:DM:DM-2571:27/01/2021:[PATRIUS] Integrateur Stormer-Cowell 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.numerical.multi;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.math.ode.nonstiff.cowell.SecondOrderStateMapper;
import fr.cnes.sirius.patrius.propagation.numerical.AdditionalEquationsAndTolerances;

/**
 * Second-order / first order integrator state mapper for one satellite.
 * 
 * @author Emmanuel Bignon
 * 
 * @since 4.6
 */
class SecondOrderMapper1Sat implements SecondOrderStateMapper {

    /** Additional equations and tolerances. */
    private List<AdditionalEquationsAndTolerances> addEquationsAndTolerances;

    /**
     * Constructor for {@link Externalizable} use.
     */
    public SecondOrderMapper1Sat() {
        this(new ArrayList<AdditionalEquationsAndTolerances>());
    }
    
    /**
     * Constructor.
     * @param addEquationsAndTolerances additional equations and tolerances
     */
    public SecondOrderMapper1Sat(final List<AdditionalEquationsAndTolerances> addEquationsAndTolerances) {
        this.addEquationsAndTolerances = addEquationsAndTolerances;
    }
    
    /** {@inheritDoc} */
    @Override
    public double[] buildFullState(final double[] y,
            final double[] yDot) {
        final double[] res = new double[get1stOrderDimension()];
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
        final double[] y = new double[get2ndOrderDimension()];
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
        final double[] yDot = new double[get2ndOrderDimension()];
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

    /**
     * Returns the 1st order state vector dimension.
     * @return the 1st order state vector dimension
     */
    public int get1stOrderDimension() {
        // Position
        int dim = 6;
        // Additional equations
        if (addEquationsAndTolerances != null) {
            for (final AdditionalEquationsAndTolerances stateAndTol : addEquationsAndTolerances) {
                dim += stateAndTol.getEquations().getFirstOrderDimension();
            }
        }
        return dim;
    }

    /**
     * Returns the 2nd order state vector dimension.
     * @return the 2nd order state vector dimension
     */
    public int get2ndOrderDimension() {
        // Position
        int dim = 3;
        // Additional equations
        if (addEquationsAndTolerances != null) {
            for (final AdditionalEquationsAndTolerances stateAndTol : addEquationsAndTolerances) {
                dim += stateAndTol.getEquations().getSecondOrderDimension();
            }
        }
        return dim;
    }

    /** {@inheritDoc} */
    @Override
    public void writeExternal(final ObjectOutput oo) throws IOException {
        // Nothing to store (interpolation use only)
    }

    /** {@inheritDoc} */
    @Override
    public void readExternal(final ObjectInput oi) throws IOException, ClassNotFoundException {
        // Nothing to store (interpolation use only)
    }
}
