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
 * @history creation 22/03/2012
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.properties;

import fr.cnes.sirius.patrius.assembly.IPartProperty;
import fr.cnes.sirius.patrius.assembly.PropertyType;
import fr.cnes.sirius.patrius.assembly.properties.features.Facet;
import fr.cnes.sirius.patrius.math.parameter.ConstantFunction;
import fr.cnes.sirius.patrius.math.parameter.IParamDiffFunction;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.parameter.Parameterizable;

/**
 * This class is a facet property to use with the aerodynamic part property for the PATRIUS assembly.<br>
 * This property is meant to be used in a LEO average precision aerodynamic context.
 * See the CNES TTVS book (2002 edition : Volume 3, Module XII, $2.4.1.2 ) for information.<br>
 * 
 * @concurrency immutable
 * 
 * @author cardosop
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public final class AeroFacetProperty extends Parameterizable implements IPartProperty {

    /** Default normal force coefficient value. */
    public static final double DEFAULT_C_N = 1.7;

    /** Default tangential force coefficient value. */
    public static final double DEFAULT_C_T = 2.3;

    /** Default normal force coefficient parameter name. */
    public static final String C_N = "C_N";

    /** Default tangential force coefficient parameter name. */
    public static final String C_T = "C_T";

    /** Serializable UID. */
    private static final long serialVersionUID = -7402315627770135950L;

    /**
     * Facet.
     */
    private final Facet facet;

    /**
     * Normal force coefficient.
     */
    private final IParamDiffFunction normalCoeff;

    /**
     * Tangential force coefficient.
     */
    private final IParamDiffFunction tangentialCoeff;

    /**
     * Constructor of this property for default coefficient values.
     * 
     * @param inFacet
     *        The facet.
     */
    public AeroFacetProperty(final Facet inFacet) {
        this(inFacet, new Parameter(C_N, DEFAULT_C_N), new Parameter(C_T, DEFAULT_C_T));
    }

    /**
     * Constructor of this property.
     * 
     * @param normalCoef
     *        The value for the normal force coefficient of the part.
     * @param tangentialCoef
     *        The value for the tangential force coefficient of the part.
     * @param inFacet
     *        The facet.
     */
    public AeroFacetProperty(final Facet inFacet, final double normalCoef, final double tangentialCoef) {
        this(inFacet, new Parameter(C_N, normalCoef), new Parameter(C_T, tangentialCoef));
    }

    /**
     * Constructor of this property using {@link Parameter}.
     * 
     * @param normalCoef
     *        The parameter for the normal force coefficient of the part.
     * @param tangentialCoef
     *        The parameter for the tangential force coefficient of the part.
     * @param inFacet
     *        The facet.
     */
    public AeroFacetProperty(final Facet inFacet, final Parameter normalCoef, final Parameter tangentialCoef) {
        super(normalCoef, tangentialCoef);
        this.facet = inFacet;
        this.normalCoeff = new ConstantFunction(normalCoef);
        this.tangentialCoeff = new ConstantFunction(tangentialCoef);
    }

    /**
     * Constructor of this property.
     * 
     * @param normalCoefFct
     *        The value for the normal force coefficient of the part.
     * @param tangentialCoefFct
     *        The value for the tangential force coefficient of the part.
     * @param inFacet
     *        The facet.
     */
    public AeroFacetProperty(final Facet inFacet,
        final IParamDiffFunction normalCoefFct,
        final IParamDiffFunction tangentialCoefFct) {
        super(normalCoefFct, tangentialCoefFct);
        this.facet = inFacet;
        this.normalCoeff = normalCoefFct;
        this.tangentialCoeff = tangentialCoefFct;
    }

    /**
     * Get the facet.
     * 
     * @return the facet
     */
    public Facet getFacet() {
        return this.facet;
    }

    /** {@inheritDoc} */
    @Override
    public PropertyType getType() {
        return PropertyType.AERO_FACET;
    }

    /**
     * Get the normal force parametrizable function.
     * 
     * @return normal force parametrizable function
     */

    public IParamDiffFunction getTangentialCoef() {
        // return the function
        return this.tangentialCoeff;
    }

    /**
     * Get the normal force parametrizable function.
     * 
     * @return normal force parametrizable function
     */

    public IParamDiffFunction getNormalCoef() {
        // return the function
        return this.normalCoeff;
    }

}
