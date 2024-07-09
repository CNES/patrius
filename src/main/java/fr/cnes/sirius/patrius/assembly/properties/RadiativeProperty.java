/**
 *
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
 *
 * @history creation 12/03/2012
 *
 * HISTORY
* VERSION:4.7:FA:FA-2762:18/05/2021:Probleme lors des controles qualite via la PIC 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::FA:412:11/02/2014:Changed IParamDiffFunction into Parameter in RadiativeProperty
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
 * Three optical coefficients are defined in the visible domain (for absorption, specular reflection
 * and diffuse reflection).
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
public final class RadiativeProperty implements IPartProperty, Serializable {

    /** Parameter name for absorption coefficient. */
    public static final String ABSORPTION_COEFFICIENT = "absorption coefficient";

    /** Parameter name for reflection coefficient. */
    public static final String SPECULAR_COEFFICIENT = "specular reflection coefficient";

    /** Parameter name for diffusion coefficient. */
    public static final String DIFFUSION_COEFFICIENT = "diffusion reflection coefficient";

    /** UID. */
    private static final long serialVersionUID = 5238518184043419677L;

    /**
     * The ratio of light absorbed ka = &alpha;.
     */
    private final Parameter absRatio;

    /**
     * The ratio of light undergoing specular reflectance ks = (1 - &alpha;) &tau;.
     */
    private final Parameter specRatio;

    /**
     * The ratio of light undergoing diffuse reflectance kd = (1 - &alpha;) (1 - &tau;).
     */
    private final Parameter diffRatio;

    /**
     * Constructor of this property.
     * 
     * @param ka The ratio ka of light absorbed of the part (between 0 and 1).
     * @param ks The ratio ks of light undergoing specular reflectance (between 0 and 1).
     * @param kd The ratio kd of light undergoing diffuse reflectance (between 0 and 1).
     */
    public RadiativeProperty(final double ka, final double ks, final double kd) {
        this(new Parameter(ABSORPTION_COEFFICIENT, ka), new Parameter(SPECULAR_COEFFICIENT, ks),
            new Parameter(DIFFUSION_COEFFICIENT, kd));
    }

    /**
     * Constructor of this property using {@link Parameter}.
     * 
     * @param ka The parameter representing the ratio ka of light absorbed of the part (between 0
     *        and 1).
     * @param ks The parameter representing the ratio ks of light undergoing specular reflectance
     *        (between 0 and 1).
     * @param kd The parameter representing the ratio kd of light undergoing diffuse reflectance
     *        (between 0 and 1).
     */
    public RadiativeProperty(final Parameter ka, final Parameter ks, final Parameter kd) {
        this.absRatio = ka;
        this.specRatio = ks;
        this.diffRatio = kd;
    }

    /**
     * Get the parameter representing the ratio of light absorbed: Ka = &alpha;.
     * 
     * @return the parameter representing the absorption ratio coefficient
     */
    public Parameter getAbsorptionRatio() {
        return this.absRatio;
    }

    /**
     * 
     * Get the parameter representing the ratio of light subjected to specular reflectance : Ks = (1
     * - &alpha;) &tau;.
     * 
     * @return the parameter representing the specular reflection ratio coefficient
     */
    public Parameter getSpecularReflectionRatio() {
        return this.specRatio;
    }

    /**
     * 
     * Get the parameter representing the ratio of light subjected to diffuse reflectance : Kd = (1
     * - &alpha;) (1 - &tau;).
     * 
     * @return the parameter representing the diffuse reflection ratio coefficient
     */
    public Parameter getDiffuseReflectionRatio() {
        return this.diffRatio;
    }

    /** {@inheritDoc} */
    @Override
    public PropertyType getType() {
        return PropertyType.RADIATIVE;
    }
}
