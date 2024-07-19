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
 * @history creation 05/04/2017
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:570:05/04/2017:add PropulsiveProperty and TankProperty
 * VERSION::FA:1449:15/03/2018:remove TankProperty name attribute
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.properties;

import fr.cnes.sirius.patrius.assembly.IPartProperty;
import fr.cnes.sirius.patrius.assembly.PropertyType;
import fr.cnes.sirius.patrius.math.parameter.Parameterizable;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;

/**
 * Tank property: gathers all properties of a fuel tank.<br>
 * <p>
 * Warning: a part should either have a MassProperty or a TankProperty, never both
 * </p>
 * 
 * @concurrency immutable
 * 
 * @author Emmanuel Bignon
 * 
 * @version $Id$
 * 
 * @since 3.4
 * 
 */
public final class TankProperty extends Parameterizable implements IPartProperty {

     /** Serializable UID. */
    private static final long serialVersionUID = 7714453843267281713L;

    /** Part name. */
    private String partName;

    /** Mass property. */
    private final MassProperty massProperty;

    /**
     * Constructor.
     * 
     * @param massIn mass
     * @throws PatriusException thrown if mass is negative
     */
    public TankProperty(final double massIn) throws PatriusException {
        super();
        // Name is later defined in set method
        this.partName = "";
        this.massProperty = new MassProperty(massIn);
    }

    /**
     * Copy constructor.
     * 
     * @param tankPropertyIn tank property
     */
    public TankProperty(final TankProperty tankPropertyIn) {
        super();
        try {
            this.partName = tankPropertyIn.getPartName();
            this.massProperty = new MassProperty(tankPropertyIn.getMass());
        } catch (final PatriusException e) {
            // It cannot happen since mass cannot be negative at this point
            throw new PatriusRuntimeException(PatriusMessages.INTERNAL_ERROR, e);
        }
    }

    /**
     * Setter for the part name owning the property. <b>Warning</b>: this setter should not be used.
     * It is used internally in PATRIUS.
     * 
     * @param nameIn the part name owning the property
     */
    public void setPartName(final String nameIn) {
        this.partName = nameIn;
    }

    /**
     * Getter for the part name owning the property.
     * 
     * @return the part name owning the property
     */
    public String getPartName() {
        return this.partName;
    }

    /**
     * Getter for the mass.
     * 
     * @return tank mass
     */
    public double getMass() {
        return this.massProperty.getMass();
    }

    /**
     * Getter for the underlying mass property.
     * 
     * @return the underlying mass property
     */
    public MassProperty getMassProperty() {
        return this.massProperty;
    }

    /** {@inheritDoc} */
    @Override
    public PropertyType getType() {
        return PropertyType.TANK;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("TankProperty: part name=%s, mass=%s", this.partName,
            this.massProperty.getMass());
    }
}
