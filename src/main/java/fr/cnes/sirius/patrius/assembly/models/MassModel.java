/**
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
 * @history creation 25/04/2012
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:86:19/09/2013:New mass model
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:200:28/08/2014: dealing with a negative mass in the propagator
 * VERSION::FA:373:12/01/2015: proper handling of mass event detection
 * VERSION::FA:410:16/04/2015: Anomalies in the Patrius Javadoc
 * VERSION::FA:673:12/09/2016: add getTotalMass(state)
 * VERSION::FA:706:06/01/2017: synchronisation problem with the Assemby mass
 * VERSION::DM:570:05/04/2017:add PropulsiveProperty and TankProperty
 * VERSION::FA:1449:15/03/2018:part can have either a Tank or a Mass property, not both
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.IPart;
import fr.cnes.sirius.patrius.assembly.PropertyType;
import fr.cnes.sirius.patrius.assembly.properties.MassEquation;
import fr.cnes.sirius.patrius.assembly.properties.MassProperty;
import fr.cnes.sirius.patrius.assembly.properties.TankProperty;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.AdditionalEquations;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class represents a mass model for an assembly with parts that have mass properties.
 * 
 * <p>
 * Note : when using this model within a propagation, it is necessary to feed the additional equations to the
 * propagator. This has to be done prior to any propagation, to allow this model to account mass variations (i.e. due to
 * maneuvers), using the method
 * {@link fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator#setMassProviderEquation(MassProvider)} which
 * will register the additional equation and initialize the initial additional state.
 * </p>
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment uses internal mutable attributes
 * 
 * @see Assembly
 * @see MassProperty
 * 
 * @author Thomas Trapier, Rami Houdroge
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public class MassModel implements MassProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 6525277629407038860L;

    /** Map for equations */
    private final Map<String, MassEquation> eqs = new ConcurrentHashMap<>();
    /** Map for mass properties */
    private final Map<String, MassProperty> prs = new ConcurrentHashMap<>();
    /** Parts names. */
    private final List<String> partsNames;

    /**
     * Builds a mass model from an assembly and proceed to a first mass computation.
     * 
     * @param assembly the assembly
     */
    public MassModel(final Assembly assembly) {
        String temp;
        this.partsNames = new ArrayList<>();

        for (final IPart part : assembly.getParts().values()) {

            // Check part does not have both mass and tank properties
            if (part.hasProperty(PropertyType.MASS) && part.hasProperty(PropertyType.TANK)) {
                throw PatriusException
                    .createIllegalArgumentException(PatriusMessages.PDB_REDUNDANT_MASS_TANK_PROP);
            }

            if (part.hasProperty(PropertyType.MASS)) {

                // part name as key
                temp = part.getName();
                this.partsNames.add(temp);

                this.prs.put(temp, (MassProperty) part.getProperty(PropertyType.MASS));
                this.eqs.put(temp, new MassEquation(temp));
            }
            if (part.hasProperty(PropertyType.TANK)) {

                // part name as key
                temp = part.getName();
                this.partsNames.add(temp);
                final TankProperty tankProperty = (TankProperty) part
                    .getProperty(PropertyType.TANK);

                this.prs.put(temp, tankProperty.getMassProperty());
                this.eqs.put(temp, new MassEquation(temp));
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public double getTotalMass() {

        // loop on each part that contains a mass property
        double mass = 0.0;
        for (final MassProperty props : this.prs.values()) {
            // adding of the value
            mass += props.getMass();
        }

        return mass;
    }

    /** {@inheritDoc} */
    @Override
    public double getTotalMass(final SpacecraftState state) {

        double mass = 0.;
        // Try to retrieve mass from state vector
        if (state.getAdditionalStatesMass().size() == 0) {
            // Mass is not in state vector, retrieve mass from mass provider
            mass = this.getTotalMass();
        } else {
            for (final double[] massi : state.getAdditionalStatesMass().values()) {
                mass += massi[0];
            }
        }
        return mass;
    }

    /** {@inheritDoc} */
    @Override
    public double getMass(final String partName) {
        this.checkProperty(partName);
        return this.prs.get(partName).getMass();
    }

    /** {@inheritDoc} */
    @Override
    public void updateMass(final String partName, final double newMass) throws PatriusException {
        this.checkProperty(partName);
        this.prs.get(partName).updateMass(newMass);
    }

    /** {@inheritDoc} */
    @Override
    public void addMassDerivative(final String partName, final double flowRate) {
        this.checkProperty(partName);
        this.eqs.get(partName).addMassDerivative(flowRate);
    }

    /** {@inheritDoc} */
    @Override
    public void setMassDerivativeZero(final String partName) {
        this.checkProperty(partName);
        this.eqs.get(partName).setMassDerivativeZero();
    }

    /**
     * Make sure the given part has a mass property
     * 
     * @param partName name of part subject to mass flow variation
     */
    private void checkProperty(final String partName) {
        if (!this.prs.containsKey(partName)) {
            throw new IllegalArgumentException();
        }
    }

    /** {@inheritDoc} */
    @Override
    public AdditionalEquations getAdditionalEquation(final String name) {
        this.checkProperty(name);
        return this.eqs.get(name);
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getAllPartsNames() {
        return this.partsNames;
    }
}
