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
package fr.cnes.sirius.patrius.propagation.numerical.multi;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import fr.cnes.sirius.patrius.math.ode.nonstiff.cowell.SecondOrderStateMapper;
import fr.cnes.sirius.patrius.propagation.numerical.AdditionalEquationsAndTolerances;

/**
 * Second-order / first order integrator state mapper for multi-satellites integrator.
 * 
 * @author Emmanuel Bignon
 * 
 * @since 4.6
 */
class SecondOrderMapper implements SecondOrderStateMapper {

    /** Additional equations and tolerances. */
    private Map<String, List<AdditionalEquationsAndTolerances>> addEquationsAndTolerances;

    /** Satellites ID list. */
    private List<String> satIdList;
    
    /** First order state vector dimension. */
    private int dimension1;

    /** Second order state vector dimension. */
    private int dimension2;
    
    /**
     * Empty constructor used for {@link Externalizable} implementation.
     */
    public SecondOrderMapper() {
        this(new HashMap<String, List<AdditionalEquationsAndTolerances>>(), new ArrayList<String>());
    }
    
    /**
     * Constructor.
     * @param addEquationsAndTolerances additional equations and tolerances
     * @param satIdList satellites ID list
     */
    public SecondOrderMapper(final Map<String, List<AdditionalEquationsAndTolerances>> addEquationsAndTolerances,
            final List<String> satIdList) {
        this.addEquationsAndTolerances = addEquationsAndTolerances;
        this.satIdList = satIdList;
        this.dimension1 = get1stOrderDimension();
        this.dimension2 = get2ndOrderDimension();
    }

    /** {@inheritDoc} */
    @Override
    public double[] buildFullState(final double[] y,
            final double[] yDot) {
        final double[] res = new double[dimension1];
        // Loop on each spacecraft
        int pos = 0;
        int pos2 = 0;
        for(final String id : satIdList) {
            // Perform mapping on one satellite
            final SecondOrderMapper1Sat mapper1Sat = new SecondOrderMapper1Sat(addEquationsAndTolerances.get(id));
            final int dim2 = mapper1Sat.get2ndOrderDimension();
            final double[] y1Sat = new double[dim2];
            final double[] yDot1Sat = new double[dim2];
            System.arraycopy(y, pos2, y1Sat, 0, y1Sat.length);
            System.arraycopy(yDot, pos2, yDot1Sat, 0, yDot1Sat.length);
            final double[] res1Sat = mapper1Sat.buildFullState(y1Sat, yDot1Sat);
            // Update result
            System.arraycopy(res1Sat, 0, res, pos, res1Sat.length);
            // Update indices
            pos += res1Sat.length;
            pos2 += dim2;
        }
        return res;
    }

    /** {@inheritDoc} */
    @Override
    public double[] extractY(final double[] fullState) {
        final double[] y = new double[dimension2];
        // Loop on each spacecraft
        int pos = 0;
        int pos1 = 0;
        for(final String id : satIdList) {
            // Perform mapping on one satellite
            final SecondOrderMapper1Sat mapper1Sat = new SecondOrderMapper1Sat(addEquationsAndTolerances.get(id));
            final int dim1 = mapper1Sat.get1stOrderDimension();
            final double[] fullState1Sat = new double[dim1];
            System.arraycopy(fullState, pos1, fullState1Sat, 0, fullState1Sat.length);
            final double[] y1Sat = mapper1Sat.extractY(fullState1Sat);
            // Update result
            System.arraycopy(y1Sat, 0, y, pos, y1Sat.length);
            // Update indices
            pos += y1Sat.length;
            pos1 += dim1;
            
        }
        return y;
    }

    /** {@inheritDoc} */
    @Override
    public double[] extractYDot(final double[] fullState) {
        final double[] yDot = new double[dimension2];
        // Loop on each spacecraft
        int pos = 0;
        int pos1 = 0;
        for(final String id : satIdList) {
            // Perform mapping on one satellite
            final SecondOrderMapper1Sat mapper1Sat = new SecondOrderMapper1Sat(addEquationsAndTolerances.get(id));
            final int dim1 = mapper1Sat.get1stOrderDimension();
            final double[] fullState1Sat = new double[dim1];
            System.arraycopy(fullState, pos1, fullState1Sat, 0, fullState1Sat.length);
            final double[] yDot1Sat = mapper1Sat.extractYDot(fullState1Sat);
            // Update result
            System.arraycopy(yDot1Sat, 0, yDot, pos, yDot1Sat.length);
            // Update indices
            pos += yDot1Sat.length;
            pos1 += dim1;
            
        }
        return yDot;
    }

    /**
     * Returns the 1st order state vector dimension.
     * @return the 1st order state vector dimension
     */
    private int get1stOrderDimension() {
        int dim = 0;
        for(final String id : satIdList) {
            // Position - velocity
            dim += 6;
            // Additional equations
            final List<AdditionalEquationsAndTolerances> addEq = addEquationsAndTolerances.get(id);
            if (addEq != null) {
                for (final AdditionalEquationsAndTolerances stateAndTol : addEq) {
                    dim += stateAndTol.getEquations().getFirstOrderDimension();
                }
            }
        }
        return dim;
    }

    /**
     * Returns the 2nd order state vector dimension.
     * @return the 2nd order state vector dimension
     */
    private int get2ndOrderDimension() {
        int dim = 0;
        for(final String id : satIdList) {
            // Position
            dim += 3;
            // Additional equations
            final List<AdditionalEquationsAndTolerances> addEq = addEquationsAndTolerances.get(id);
            if (addEq != null) {
                for (final AdditionalEquationsAndTolerances stateAndTol : addEq) {
                    dim += stateAndTol.getEquations().getSecondOrderDimension();
                }
            }
        }
        return dim;
    }

    /** {@inheritDoc} */
    @Override
    public void writeExternal(final ObjectOutput oo) throws IOException {
        // Dimensions
        oo.writeInt(dimension1);
        oo.writeInt(dimension2);
        oo.writeInt(satIdList.size());
        // Satellite ID list
        for (final String id : satIdList) {
            oo.writeObject(id);
        }
        // Additional equations and tolerances
        oo.writeInt(addEquationsAndTolerances.size());
        for (final Entry<String, List<AdditionalEquationsAndTolerances>> entry : addEquationsAndTolerances
                .entrySet()) {
            // ID
            oo.writeObject(entry.getKey());
            // Number of additional equations
            final List<AdditionalEquationsAndTolerances> listi = entry.getValue();
            oo.writeInt(listi.size());
            // Additional equations and tolerances
            for (int j = 0; j < listi.size(); j++) {
                oo.writeObject(listi.get(j));
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void readExternal(final ObjectInput oi) throws IOException, ClassNotFoundException {
        // Dimensions
        dimension1 = oi.readInt();
        dimension2 = oi.readInt();
        // Satellite ID list
        satIdList = new ArrayList<String>();
        final int m = oi.readInt();
        for (int i = 0; i < m; i++) {
            satIdList.add((String) oi.readObject());

        }
        // Additional equations and tolerances
        addEquationsAndTolerances = new HashMap<String, List<AdditionalEquationsAndTolerances>>();
        final int n = oi.readInt();
        for (int i = 0; i < n; i++) {
            // ID
            final String satID = (String) oi.readObject();
            // Number of additional equations
            final int listSize = oi.readInt();
            final List<AdditionalEquationsAndTolerances> listi = new ArrayList<AdditionalEquationsAndTolerances>();
            // Additional equations and tolerances
            for (int j = 0; j < listSize; j++) {
                listi.add((AdditionalEquationsAndTolerances) oi.readObject());
            }
            addEquationsAndTolerances.put(satID, listi);
        }
    }
}
