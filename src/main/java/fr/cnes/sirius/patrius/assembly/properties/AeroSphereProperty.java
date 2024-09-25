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
 * @history creation 27/04/2012
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::FA:273:20/10/2014:Minor code problems
 * VERSION::FA:373:12/01/2015: proper handling of mass event detection
 * VERSION::FA:358:09/03/2015:proper handling of vehicle negative surface
 * VERSION::DM:834:04/04/2017:create vehicle object
 * VERSION::DM:571:12/04/2017:Remove useless atmospheric height scale factor
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.properties;

import fr.cnes.sirius.patrius.assembly.PropertyType;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.IParamDiffFunction;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class is a sphere property to use with the aerodynamic part property for the PATRIUS assembly.<br>
 * This property is meant to be used in a LEO average precision aerodynamic context.
 * See the CNES TTVS book (2002 edition : Volume 3, Module XII, $2.4.1.2 ) for information.<br>
 * <p>
 * Note that the use of this class implies a constant area which may not be suited for some application such as reentry.
 * </p>
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
public final class AeroSphereProperty extends AeroCrossSectionProperty {

    /** Serializable UID. */
    private static final long serialVersionUID = -6756221234693713611L;

    /**
     * Sphere radius (m).
     */
    private final double sphereRadius;

    /**
     * Sphere radius area (m<sup>2</sup>).
     */
    private final double sphereArea;

    /**
     * Constructor of this property.
     * This constructor can be used when the user wants to compute the partial derivatives of the drag force
     * with respect to spacecraft position (the default value of the derivatives is set to zero), for the sphere case.
     * However, users should better use the property {@link AeroCrossSectionProperty} which provide the cross section
     * computation of the satellite main's part (could be either a sphere, a cylinder or a parallelepiped).
     * 
     * The derivatives are computed using an analytical formula that involves the value of
     * dRho / dh (the derivative of the atmospheric density with respect to altitude), computed by
     * finite differences with altitude variation.
     * Therefore it is possible to compute the derivatives with respect to position
     * when the considered orbit is highly excentric.
     * 
     * @param inSphereRadius
     *        The sphere radius.
     * @param density
     *        density.
     * @param relativeVelocity
     *        relativeVelocity.
     */
    public AeroSphereProperty(final double inSphereRadius, final double density,
        final Vector3D relativeVelocity) {
        this(inSphereRadius, new Parameter(C_X, -density / 2.0 * relativeVelocity.getNorm()
            * relativeVelocity.getNorm()));
    }

    /**
     * Constructor of this property with default value for C_X.
     * 
     * @param inSphereRadius
     *        The sphere radius.
     */
    public AeroSphereProperty(final double inSphereRadius) {
        this(inSphereRadius, new Parameter(C_X, DEFAULT_C_X));
    }

    /**
     * Constructor of this property giving the drag coef.
     * 
     * @param inSphereRadius
     *        The sphere radius.
     * @param dragCoef
     *        The dragCoef.
     */
    public AeroSphereProperty(final double inSphereRadius, final double dragCoef) {
        this(inSphereRadius, new Parameter(C_X, dragCoef));
    }

    /**
     * Constructor of this property giving the drag coef without the atmospheric height scale.
     * 
     * @param inSphereArea
     *        The sphere area
     * @param dragCoef
     *        The dragCoef
     * @throws PatriusException
     *         thrown if parameter if unknown
     */
    public AeroSphereProperty(final Parameter inSphereArea, final double dragCoef) throws PatriusException {
        super(null, dragCoef);
        this.sphereArea = inSphereArea.getValue();

        // Radius is defined only if area is positive
        if (this.sphereArea >= 0) {
            this.sphereRadius = MathLib.sqrt(this.sphereArea / FastMath.PI);
        } else {
            this.sphereRadius = Double.NaN;
        }
    }

    /**
     * This constructor shall be used for defining the drag coef using {@link Parameter}
     * 
     * @param inSphereRadius
     *        The sphere radius.
     * @param dragCoef
     *        The dragCoef parameter.
     */
    public AeroSphereProperty(final double inSphereRadius, final Parameter dragCoef) {
        super(null, dragCoef);
        this.sphereRadius = inSphereRadius;
        this.sphereArea = FastMath.PI * this.sphereRadius * this.sphereRadius;
    }

    /**
     * Constructor of this property defining the drag coef as a ParamDiffFunction.
     * 
     * @param inSphereRadius
     *        The sphere radius.
     * @param dragCoefFct
     *        The dragCoef parameterizable function.
     */
    public AeroSphereProperty(final double inSphereRadius, final IParamDiffFunction dragCoefFct) {
        super(null, dragCoefFct);
        this.sphereRadius = inSphereRadius;
        this.sphereArea = FastMath.PI * this.sphereRadius * this.sphereRadius;
    }

    /**
     * Get the sphere area.
     * 
     * @return the sphere area (m<sup>2</sup>)
     */
    public double getSphereArea() {
        return this.sphereArea;
    }

    /**
     * Get the sphere radius.
     * 
     * @return the sphere radius (m)
     * @throws PatriusException
     *         thrown if radius is undefined (negative area)
     */
    public double getSphereRadius() throws PatriusException {
        if (Double.isNaN(this.sphereRadius)) {
            throw new PatriusException(PatriusMessages.UNDEFINED_RADIUS);
        }
        return this.sphereRadius;
    }

    /** {@inheritDoc} */
    @Override
    public double getCrossSection(final SpacecraftState state, final Vector3D relativeVelocity,
                                  final Frame mainPartFrame, final Frame partFrame) {
        return this.sphereArea;
    }

    /** {@inheritDoc} */
    @Override
    public PropertyType getType() {
        return PropertyType.AERO_CROSS_SECTION;
    }
}
