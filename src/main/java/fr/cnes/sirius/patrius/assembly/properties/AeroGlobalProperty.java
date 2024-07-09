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
 * @history creation 10/05/2017
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:834:10/05/2017:create vehicle object
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.properties;

import fr.cnes.sirius.patrius.assembly.IPartProperty;
import fr.cnes.sirius.patrius.assembly.PropertyType;
import fr.cnes.sirius.patrius.assembly.models.DragLiftModel;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.CrossSectionProvider;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.ConstantFunction;
import fr.cnes.sirius.patrius.math.parameter.IParamDiffFunction;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.parameter.Parameterizable;

/**
 * This class is a cross section property providing the cross section of shapes such as
 * sphere, cylinder or parallelepiped.
 * This cross section is to be used in {@link DragLiftModel} model for drag and lift computation.
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
public class AeroGlobalProperty extends Parameterizable implements IPartProperty {

    /** Default drag force coefficient parameter name. */
    public static final String C_X = "C_X";

    /** Default lift coefficient parameter name. */
    public static final String C_Z = "C_Z";

    /** Generated serial UID. */
    private static final long serialVersionUID = -9193369357464080913L;

    /** Drag force coefficient. */
    private final IParamDiffFunction dragCoeff;

    /** Lift force coefficient. */
    private final IParamDiffFunction liftCoeff;

    /** Main satellite shape. */
    private final CrossSectionProvider mainShape;

    /**
     * This constructor shall be used for defining the aero coefficients as constants.
     * 
     * @param dragCoef
     *        constant drag coefficient
     * @param liftCoef
     *        constant lift coefficient
     * @param shape
     *        main shape
     */
    public AeroGlobalProperty(final double dragCoef, final double liftCoef, final CrossSectionProvider shape) {
        super(new Parameter(C_X, dragCoef));
        this.dragCoeff = new ConstantFunction(new Parameter(C_X, dragCoef));
        this.liftCoeff = new ConstantFunction(new Parameter(C_Z, liftCoef));
        this.mainShape = shape;
    }

    /**
     * This constructor shall be used for defining the aero coefficients as ParamDiffFunction.
     * 
     * @param dragCoef
     *        function drag coefficient
     * @param liftCoef
     *        function lift coefficient
     * @param shape
     *        main shape
     */
    public AeroGlobalProperty(final IParamDiffFunction dragCoef, final IParamDiffFunction liftCoef,
        final CrossSectionProvider shape) {
        super();
        this.dragCoeff = dragCoef;
        this.liftCoeff = liftCoef;
        this.mainShape = shape;
    }

    /**
     * Compute the cross section of main shape using the relative velocity in the
     * part (having the aero property) frame as the direction to provider to the
     * {@link CrossSectionProvider#getCrossSection(Vector3D)}.
     * 
     * @param velocityPartFrame
     *        the spacecraft velocity relative to the atmosphere in part frame
     * @return the cross section of the main shape.
     */
    public double getCrossSection(final Vector3D velocityPartFrame) {

        // Return null cross section if relative velocity is null vector (since aero force is then 0)
        if (velocityPartFrame.getNorm() == 0.) {
            return 0.;
        }

        return this.mainShape.getCrossSection(velocityPartFrame);
    }

    /**
     * Get the drag coefficient.
     * 
     * @return the drag coefficient
     */
    public IParamDiffFunction getDragCoef() {
        return this.dragCoeff;
    }

    /**
     * Get the lift coefficient.
     * 
     * @return the lift coefficient
     */
    public IParamDiffFunction getLiftCoef() {
        return this.liftCoeff;
    }

    /** {@inheritDoc} */
    @Override
    public PropertyType getType() {
        return PropertyType.AERO_GLOBAL;
    }
}
