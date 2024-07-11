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
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::FA:461:11/06/2015:Changed IParamDiffFunction into Parameter (RadiativeIRProperty class)
 * VERSION::FA:1796:07/09/2018:Correction vehicle class
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.properties;

import java.io.Serializable;

import fr.cnes.sirius.patrius.assembly.IPartProperty;
import fr.cnes.sirius.patrius.assembly.PropertyType;
import fr.cnes.sirius.patrius.math.parameter.Parameter;

/**
 * This class is a part property for the PATRIUS assembly. It is the radiative property of a part.
 * Three optical coefficients are defined in the infrared domain.
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
public final class RadiativeIRProperty implements IPartProperty, Serializable {

    /** Parameter name for absorption coefficient. */
    public static final String ABSORPTION_COEFFICIENT = "absorption coefficient";

    /** Parameter name for reflection coefficient. */
    public static final String SPECULAR_COEFFICIENT = "specular reflection coefficient";

    /** Parameter name for diffusion coefficient. */
    public static final String DIFFUSION_COEFFICIENT = "diffusion reflection coefficient";

    /** UID. */
    private static final long serialVersionUID = -3411390303612653012L;

    /**
     * Absorption coefficient.
     */
    private final Parameter absorptionCoeff;

    /**
     * Diffuse reflection coefficient.
     */
    private final Parameter diffuseReflectionCoeff;

    /**
     * Specular reflection coefficient.
     */
    private final Parameter specularReflectionCoeff;

    /**
     * Constructor of this property.
     * 
     * @param absorptionCoef The absorption coefficient of the part.
     * @param specularCoef The specular reflection coefficient of the part.
     * @param diffuseCoef The diffuse reflection coefficient of the part.
     */
    public RadiativeIRProperty(final double absorptionCoef, final double specularCoef,
        final double diffuseCoef) {
        this(new Parameter(ABSORPTION_COEFFICIENT, absorptionCoef), new Parameter(
            SPECULAR_COEFFICIENT, specularCoef), new Parameter(DIFFUSION_COEFFICIENT,
            diffuseCoef));
    }

    /**
     * Constructor of this property using {@link Parameter}.
     * 
     * @param absorptionCoef The parameter representing the absorption coefficient of the part.
     * @param specularCoef The parameter representing the specular reflection coefficient of the
     *        part.
     * @param diffuseCoef The parameter representing the diffuse reflection coefficient of the part.
     */
    public RadiativeIRProperty(final Parameter absorptionCoef, final Parameter specularCoef,
        final Parameter diffuseCoef) {
        this.absorptionCoeff = absorptionCoef;
        this.diffuseReflectionCoeff = diffuseCoef;
        this.specularReflectionCoeff = specularCoef;
    }

    /**
     * 
     * Get the parameter representing the absorption coefficient of the part.
     * 
     * @return the parameter representing the absorption coefficient
     */
    public Parameter getAbsorptionCoef() {
        return this.absorptionCoeff;
    }

    /**
     * 
     * Get the parameter representing the specular reflection coefficient of the part.
     * 
     * @return the parameter representing the specular reflection coefficient
     */
    public Parameter getSpecularReflectionCoef() {
        return this.specularReflectionCoeff;
    }

    /**
     * 
     * Get the parameter representing the diffuse reflection coefficient of the part.
     * 
     * @return the parameter representing the diffuse reflection coefficient
     */
    public Parameter getDiffuseReflectionCoef() {
        return this.diffuseReflectionCoeff;
    }

    /** {@inheritDoc} */
    @Override
    public PropertyType getType() {
        return PropertyType.RADIATIVEIR;
    }
}
