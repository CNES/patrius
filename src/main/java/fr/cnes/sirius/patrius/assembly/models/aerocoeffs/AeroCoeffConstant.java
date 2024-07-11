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
 *
 * @history creation 23/05/2018
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1489:23/05/2018:add GENOPUS Custom classes
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.models.aerocoeffs;

import java.util.ArrayList;

import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;

/**
 * Constant aerodynamic coefficient.
 *
 * @author Emmanuel Bignon
 *
 * @version $Id$
 *
 * @since 4.1
 *
 */
public class AeroCoeffConstant implements AerodynamicCoefficient {

    /** Serializable UID. */
    private static final long serialVersionUID = -8711405951225638618L;

    /** Aerodynamic coefficient. */
    private final Parameter aeroCoeff;

    /**
     * Constructor.
     *
     * @param aeroCoeffIn aerodynamic coefficient
     */
    public AeroCoeffConstant(final Parameter aeroCoeffIn) {
        this.aeroCoeff = aeroCoeffIn;
    }

    /** {@inheritDoc} */
    @Override
    public double value(final SpacecraftState state) {
        return this.aeroCoeff.getValue();
    }

    /**
     * Getter for the aerodynamic coefficient.
     *
     * @return the aerodynamic coefficient
     */
    public double getAerodynamicCoefficient() {
        return this.aeroCoeff.getValue();
    }

    /** {@inheritDoc} */
    @Override
    public double derivativeValue(final Parameter p, final SpacecraftState s) {
        // return value
        double value = 0;

        // check if the function is differentiable by p
        if (this.isDifferentiableBy(p)) {
            value = 1;
        }
        return value;
    }

    /** {@inheritDoc} */
    @Override
    public AerodynamicCoefficientType getType() {
        return AerodynamicCoefficientType.CONSTANT;
    }

    /** {@inheritDoc} */
    @Override
    public boolean supportsParameter(final Parameter param) {
        return this.aeroCoeff.equals(param);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDifferentiableBy(final Parameter p) {
        return this.supportsParameter(p);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * The list is returned in a shallow copy.
     * </p>
     */
    @Override
    public ArrayList<Parameter> getParameters() {
        final ArrayList<Parameter> list = new ArrayList<>();
        list.add(this.aeroCoeff);
        return list;
    }
}
