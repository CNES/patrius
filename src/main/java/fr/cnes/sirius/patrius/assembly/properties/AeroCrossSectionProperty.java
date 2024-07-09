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
 * @history creation 04/04/2017
 * 
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:834:04/04/2017:create vehicle object
 * VERSION::DM:571:12/04/2017:Remove useless atmospheric height scale factor
 * VERSION::DM:1420:24/11/2017:updateMainPartFrame() speed-up
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.properties;

import fr.cnes.sirius.patrius.assembly.IPartProperty;
import fr.cnes.sirius.patrius.assembly.PropertyType;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.CrossSectionProvider;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.ConstantFunction;
import fr.cnes.sirius.patrius.math.parameter.IParamDiffFunction;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.parameter.Parameterizable;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class is a cross section property providing the cross section of shapes such as
 * sphere, cylinder or parallelepiped.
 * This cross section is to be used in aero models for drag force computation.
 * 
 * @concurrency immutable
 * 
 * @author rodriguest
 * 
 * @version $Id$
 * 
 * @since 3.4
 * 
 */
public class AeroCrossSectionProperty extends Parameterizable implements IPartProperty {

    /** Default normal force coefficient value. */
    public static final double DEFAULT_C_X = 1.7;

    /** Default drag force coefficient parameter name. */
    public static final String C_X = "C_X";

    /** Serializable UID. */
    private static final long serialVersionUID = -6756221234693713611L;

    /**
     * Main satellite shape.
     */
    private final CrossSectionProvider mainShape;

    /**
     * Drag force coefficient.
     */
    private final IParamDiffFunction dragCoeff;

    /**
     * Constructor of this property with default value for C_X.
     * 
     * @param shape
     *        the main shape
     */
    public AeroCrossSectionProperty(final CrossSectionProvider shape) {
        this(shape, new ConstantFunction(DEFAULT_C_X));
    }

    /**
     * Constructor of this property giving the drag coef.
     * 
     * @param shape
     *        the main shape
     * @param dragCoef
     *        The dragCoef.
     */
    public AeroCrossSectionProperty(final CrossSectionProvider shape, final double dragCoef) {
        this(shape, new ConstantFunction(dragCoef));
    }

    /**
     * Constructor of this property defining the drag coef as a {@link IParamDiffFunction}.
     * 
     * @param shape
     *        the main shape
     * @param dragCoefFct
     *        The dragCoef parameterizable function.
     */
    public AeroCrossSectionProperty(final CrossSectionProvider shape, final Parameter dragCoefFct) {
        this(shape, new ConstantFunction(dragCoefFct));
    }

    /**
     * Constructor of this property defining the drag coef as a {@link IParamDiffFunction}.
     * 
     * @param shape
     *        the main shape
     * @param dragCoefFct
     *        The dragCoef parameterizable function.
     */
    public AeroCrossSectionProperty(final CrossSectionProvider shape, final IParamDiffFunction dragCoefFct) {
        super(dragCoefFct);
        this.mainShape = shape;
        this.dragCoeff = dragCoefFct;
    }

    /**
     * Compute the cross section of main shape using the relative velocity in the
     * part (having the aero property) frame as the direction to provider to the
     * {@link CrossSectionProvider#getCrossSection(Vector3D)}.
     * 
     * @param state
     *        the current state of the spacecraft
     * @param relativeVelocity
     *        the spacecraft velocity relative to the atmosphere in state frame.
     * @param mainPartFrame
     *        main frame
     * @param partFrame
     *        frame of part owning the property
     * @return the cross section of the main shape.
     * @throws PatriusException
     *         if some frame specific error occurs
     */
    public double getCrossSection(final SpacecraftState state, final Vector3D relativeVelocity,
                                  final Frame mainPartFrame, final Frame partFrame) throws PatriusException {

        // Return null cross section if relative velocity is null vector (since aero force is then 0)
        if (relativeVelocity.getNorm() == 0.) {
            return 0.;
        }

        final Transform stateToPart = state.getFrame().getTransformTo(partFrame, state.getDate());
        final Vector3D velocityPartFrame = stateToPart.transformVector(relativeVelocity);
        return this.mainShape.getCrossSection(velocityPartFrame);
    }

    /** {@inheritDoc} */
    @Override
    public PropertyType getType() {
        return PropertyType.AERO_CROSS_SECTION;
    }

    /**
     * Get the drag force coefficient derivative value with respect to the given parameter.
     * 
     * @param parameter
     *        the parameter
     * @param s
     *        the current state
     * @return the drag force coefficient derivative value
     */

    public double getDragForceDerivativeValue(final Parameter parameter, final SpacecraftState s) {
        // call to derivativeValue
        return this.dragCoeff.derivativeValue(parameter, s);
    }

    /**
     * Get the drag force coefficient parametrizable function.
     * 
     * @return force coefficient parametrizable function
     */

    public IParamDiffFunction getDragForce() {
        // return the function
        return this.dragCoeff;
    }

}
