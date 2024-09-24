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
 * @history creation 12/03/2012
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:200:28/08/2014: dealing with a negative mass in the propagator
 * VERSION::DM:284:06/01/2015:New architecture for parameterizable Parameters
 * VERSION::FA:373:12/01/2015: proper handling of mass event detection
 * VERSION::FA:410:16/04/2015: Anomalies in the Patrius Javadoc
 * VERSION::FA:706:13/12/2016: synchronisation problem with the Assemby mass
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.properties;

import fr.cnes.sirius.patrius.assembly.IPartProperty;
import fr.cnes.sirius.patrius.assembly.PropertyType;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.parameter.Parameterizable;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class is a part property for the PATRIUS assembly. It is the mass property of a part.
 * 
 * @concurrency immutable
 * 
 * @author Gerald Mercadier
 * 
 * @version $Id$
 * 
 * @since 1.1
 * 
 */
public final class MassProperty extends Parameterizable implements IPartProperty {

    /** Serializable UID. */
    private static final long serialVersionUID = 4120164826609170722L;

    /**
     * Mass of the part.
     */
    private final Parameter mass;

    /**
     * Constructor of this property.
     * 
     * @param inMass
     *        The mass of the part.
     * @throws PatriusException
     *         if inMass is negative (PatriusMessages.MASS_ARGUMENT_IS_NEGATIVE)
     */
    public MassProperty(final double inMass) throws PatriusException {
        this(new Parameter("MASS", inMass));
    }

    /**
     * Constructor of this property using a {@link Parameter}.
     * 
     * @param inMass
     *        The mass of the part.
     * @throws PatriusException
     *         if inMass is negative (PatriusMessages.MASS_ARGUMENT_IS_NEGATIVE)
     */
    public MassProperty(final Parameter inMass) throws PatriusException {
        super(inMass);
        this.mass = inMass;
        if (this.mass.getValue() < 0.0) {
            throw new PatriusException(PatriusMessages.NOT_POSITIVE_MASS, inMass);
        }
    }

    /**
     * 
     * Gets the mass of the part.
     * 
     * @return the mass
     */
    public double getMass() {
        return this.mass.getValue();
    }

    /**
     * 
     * Updates the mass of the part.
     * 
     * @param newMass
     *        the mass
     * @throws PatriusException
     *         if newMass is negative (PatriusMessages.MASS_ARGUMENT_IS_NEGATIVE)
     */
    public void updateMass(final double newMass) throws PatriusException {
        if (newMass < 0.0) {
            throw new PatriusException(PatriusMessages.NOT_POSITIVE_MASS, newMass);
        }
        this.mass.setValue(newMass);
    }

    /** {@inheritDoc} */
    @Override
    public PropertyType getType() {
        return PropertyType.MASS;
    }
}
