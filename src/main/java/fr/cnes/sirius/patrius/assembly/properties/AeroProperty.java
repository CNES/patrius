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
 * @history creation 13/09/2016
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:599:13/09/2016: new tabulated aero model
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.properties;

import fr.cnes.sirius.patrius.assembly.IPartProperty;
import fr.cnes.sirius.patrius.assembly.PropertyType;
import fr.cnes.sirius.patrius.assembly.models.cook.AlphaProvider;

/**
 * Aero property.
 * <p>
 * This property has to be applied to Main part only.
 * </p>
 * 
 * @concurrency not thread-safe
 * 
 * @author Emmanuel Bignon
 * 
 * @version $Id$
 * 
 * @since 3.3
 */
public final class AeroProperty implements IPartProperty {

    /** Serializable UID. */
    private static final long serialVersionUID = -6928364205287822505L;

    /** Specular reemission percentage in [0; 1]. */
    private final double epsilon;

    /** Wall temperature. */
    private final double wallTemperature;

    /** Alpha. */
    private final AlphaProvider alpha;

    /**
     * Constructor.
     * 
     * @param epsilonIn
     *        specular reemission percentage in [0; 1]
     * @param wallTemperatureIn
     *        wall temperature
     * @param alphaIn
     *        alpha
     */
    public AeroProperty(final double epsilonIn, final double wallTemperatureIn, final AlphaProvider alphaIn) {
        this.epsilon = epsilonIn;
        this.wallTemperature = wallTemperatureIn;
        this.alpha = alphaIn;
    }

    /**
     * Getter for the specular reemission percentage.
     * 
     * @return the specular reemission percentage
     */
    public double getEpsilon() {
        return this.epsilon;
    }

    /**
     * Getter for the wall temperature.
     * 
     * @return the wall temperature
     */
    public double getWallTemperature() {
        return this.wallTemperature;
    }

    /**
     * Getter for the alpha.
     * 
     * @return the alpha
     */
    public AlphaProvider getAlpha() {
        return this.alpha;
    }

    /** {@inheritDoc} */
    @Override
    public PropertyType getType() {
        return PropertyType.WALL;
    }
}
