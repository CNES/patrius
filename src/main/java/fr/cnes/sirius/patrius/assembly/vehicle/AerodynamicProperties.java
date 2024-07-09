/**
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
 * @history creation 23/05/2018
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1489:23/05/2018:add GENOPUS Custom classes
 * VERSION::FA:1796:07/09/2018:Correction vehicle class
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.vehicle;

import java.io.Serializable;

import fr.cnes.sirius.patrius.assembly.AssemblyBuilder;
import fr.cnes.sirius.patrius.assembly.IPartProperty;
import fr.cnes.sirius.patrius.assembly.models.aerocoeffs.AeroCoeffConstant;
import fr.cnes.sirius.patrius.assembly.models.aerocoeffs.AerodynamicCoefficient;
import fr.cnes.sirius.patrius.assembly.models.aerocoeffs.AerodynamicCoefficientType;
import fr.cnes.sirius.patrius.assembly.properties.AeroGlobalProperty;
import fr.cnes.sirius.patrius.assembly.properties.AeroSphereProperty;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.CrossSectionProvider;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Sphere;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.ConstantFunction;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class defines an aerodynamic property to be applyied to aerodynamic parts of a vehicle
 * (PATRIUS assembly). It has a dual nature, actually it is either an {@link AeroGlobalProperty} or
 * an {@link AeroSphereProperty} depending on the constructor used.
 *
 * @author Emmanuel Bignon
 *
 * @version $Id$
 *
 * @since 4.1
 */
public class AerodynamicProperties implements Serializable {

    /** UID. */
    private static final long serialVersionUID = 5112179232025583502L;

    /** Vehicle's surface model. */
    private final VehicleSurfaceModel vehicleSurfaceModel;

    /** Drag coefficient given as a function of the spacecraftstate. */
    private final AerodynamicCoefficient dragCoef;

    /** Lift coefficient given as a function of a the spacecraftstate. */
    private final AerodynamicCoefficient liftCoef;

    /** Aero property. */
    private IPartProperty aeroProperty;

    /**
     * Constructor.
     * 
     * @param vehicleSurface the vehicle surface model
     * @param dragCoefIn drag coefficient given as a function of the spacecraftstate
     * @param liftCoefIn lift coefficient given as a function of the spacecraftstate
     * @throws PatriusException thrown if one of the parameters is null
     */
    private AerodynamicProperties(final VehicleSurfaceModel vehicleSurface,
        final AerodynamicCoefficient dragCoefIn, final AerodynamicCoefficient liftCoefIn)
        throws PatriusException {
        if (vehicleSurface == null) {
            throw new PatriusException(PatriusMessages.NULL_VEHICLE_SURFACE_MODEL);
        }
        if (dragCoefIn == null) {
            throw new PatriusException(PatriusMessages.NULL_DRAG_COEFFICIENT);
        }
        if (liftCoefIn == null) {
            throw new PatriusException(PatriusMessages.NULL_LIFT_COEFFICIENT);
        }

        this.dragCoef = dragCoefIn;
        this.liftCoef = liftCoefIn;
        this.vehicleSurfaceModel = vehicleSurface;
        this.aeroProperty = new AeroGlobalProperty(this.dragCoef, this.liftCoef, this.vehicleSurfaceModel);
    }

    /**
     * Constructor.
     * 
     * @param sphere the spherical vehicle shape
     * @param dragCoefIn drag coefficient given as a function of the spacecraftstate
     * @param liftCoefIn lift coefficient given as a function of the spacecraftstate
     * @throws PatriusException thrown if sphere is null
     */
    public AerodynamicProperties(final Sphere sphere, final AerodynamicCoefficient dragCoefIn,
        final AerodynamicCoefficient liftCoefIn) throws PatriusException {
        this(new VehicleSurfaceModel(sphere), dragCoefIn, liftCoefIn);
    }

    /**
     * Constructor.
     * 
     * @param vehicleSurface the vehicle surface model
     * @param dragCoefIn constant drag coefficient
     * @param liftCoefIn constant lift coefficient
     * @throws PatriusException thrown if the vehicle surface model is null
     */
    public AerodynamicProperties(final VehicleSurfaceModel vehicleSurface, final double dragCoefIn,
        final double liftCoefIn) throws PatriusException {
        this(vehicleSurface, createConstantFunction(AeroGlobalProperty.C_X, dragCoefIn),
            createConstantFunction(AeroGlobalProperty.C_Z, liftCoefIn));
    }

    /**
     * Constructor.
     * 
     * @param sphere the spherical vehicle shape
     * @param dragCoefIn constant drag coefficient
     * @throws PatriusException thrown if sphere is null
     */
    public AerodynamicProperties(final Sphere sphere, final double dragCoefIn)
        throws PatriusException {
        this(new VehicleSurfaceModel(sphere), createConstantFunction(AeroGlobalProperty.C_X,
            dragCoefIn), createConstantFunction(AeroGlobalProperty.C_Z, 0.0));
        this.aeroProperty = new AeroSphereProperty(sphere.getRadius(), dragCoefIn);
    }

    /**
     * Creates a constant aerodynamic coefficient with a new Parameter.
     * 
     * @param paramName parameter name
     * @param coef coefficient value
     * @return the constant aerodynamic coefficient
     */
    private static AeroCoeffConstant createConstantFunction(final String paramName,
                                                            final double coef) {
        return new AeroCoeffConstant(new Parameter(paramName, coef));
    }

    /**
     * Get aerodynamic property with multiplicative factor.
     * 
     * @param multiplicativeFactor the multiplicative factor (applied to the reference surface)
     * @return the aerodynamic property
     */
    private IPartProperty getAerodynamicProperty(final double multiplicativeFactor) {

        if (this.aeroProperty instanceof AeroSphereProperty) {
            final AeroSphereProperty aeroProperty2 = (AeroSphereProperty) this.aeroProperty;

            // Surface including multiplicative factor
            final double surfDispersed = aeroProperty2.getSphereArea() * multiplicativeFactor;
            final double cx = ((ConstantFunction) aeroProperty2.getDragForce()).value();
            this.aeroProperty = new AeroSphereProperty(MathLib.sqrt(surfDispersed / FastMath.PI), cx);

        } else if (this.aeroProperty instanceof AeroGlobalProperty) {
            final AeroGlobalProperty aeroProperty2 = (AeroGlobalProperty) this.aeroProperty;

            // Build new CrossSectionProvider including multiplicatif factor
            final CrossSectionProvider csProvider = new CrossSectionProvider(){
                /** {@inheritDoc} */
                @Override
                public double getCrossSection(final Vector3D direction) {
                    return AerodynamicProperties.this.vehicleSurfaceModel.getCrossSection(direction)
                        * multiplicativeFactor;
                }
            };
            this.aeroProperty = new AeroGlobalProperty(aeroProperty2.getDragCoef(),
                aeroProperty2.getLiftCoef(), csProvider);
        }

        return this.aeroProperty;
    }

    /**
     * Set aerodynamic property to a part (it modifies vehicle surface model as a function of the
     * multplicative factor).
     * 
     * @param builder assembly builder
     * @param mainPartName main part name
     * @param multiplicativeFactor the multiplicative factor (applied to the reference surface)
     */
    public void setAerodynamicProperties(final AssemblyBuilder builder, final String mainPartName,
                                         final double multiplicativeFactor) {
        builder.addProperty(this.getAerodynamicProperty(multiplicativeFactor), mainPartName);
    }

    /**
     * Get the type of the aerodynamic coefficients functions among: <br>
     * - Coefficients as a function of altitude, <br>
     * - Coefficients as a function of angle of attack<br>
     * - Coefficients as a function of Mach number <br>
     * - Coefficients as a function of Mach number and angle of attack.
     *
     * @return the type of function (see {@link AerodynamicCoefficientType} enumeration)
     * @throws PatriusException thrown if drag and lift coefficients are not of the same type
     *
     */
    public AerodynamicCoefficientType getFunctionType() throws PatriusException {
        if (this.dragCoef.getType() != this.liftCoef.getType()) {
            throw new PatriusException(
                PatriusMessages.UNSUPORTED_COMBINATION_DRAG_LIFT_COEFFICIENTS);
        }
        return this.dragCoef.getType();
    }

    /**
     * Get the drag coefficient.
     * 
     * @return the drag coefficient
     */
    public AerodynamicCoefficient getDragCoef() {
        return this.dragCoef;
    }

    /**
     * Get the lift coefficient.
     * 
     * @return the lift coefficient
     */
    public AerodynamicCoefficient getLiftCoef() {
        return this.liftCoef;
    }

    /**
     * Get the surface model.
     * 
     * @return the surface model
     */
    public VehicleSurfaceModel getVehicleSurfaceModel() {
        return this.vehicleSurfaceModel;
    }

    /**
     * Get the drag coefficient.
     * 
     * @return the drag coefficient
     * @throws PatriusException if the drag coefficient is not constant (use {@link #getDragCoef()} instead)
     */
    public double getConstantDragCoef() throws PatriusException {
        if (this.dragCoef instanceof AeroCoeffConstant) {
            final AeroCoeffConstant dragFunction = (AeroCoeffConstant) this.dragCoef;
            return dragFunction.getAerodynamicCoefficient();
        } else {
            throw new PatriusException(PatriusMessages.NOT_CONSTANT_DRAG_COEF);
        }
    }

    /**
     * Get the lift coefficient.
     * 
     * @return the lift coefficient
     * @throws PatriusException if the lift coefficient is not constant (use {@link #getLiftCoef()} instead)
     */
    public double getConstantLiftCoef() throws PatriusException {
        if (this.liftCoef instanceof AeroCoeffConstant) {
            final AeroCoeffConstant liftFunction = (AeroCoeffConstant) this.liftCoef;
            return liftFunction.getAerodynamicCoefficient();
        } else {
            throw new PatriusException(PatriusMessages.NOT_CONSTANT_LIFT_COEF);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        // Drag coef.
        String dragCoeffStr = null;
        if (this.getDragCoef() instanceof AeroCoeffConstant) {
            final AeroCoeffConstant dragFunction = (AeroCoeffConstant) this.dragCoef;
            dragCoeffStr = Double.toString(dragFunction.getAerodynamicCoefficient());
        } else {
            dragCoeffStr = this.getDragCoef().toString();
        }

        // Lift coef.
        String liftCoeffStr = null;
        if (this.getLiftCoef() instanceof AeroCoeffConstant) {
            final AeroCoeffConstant liftFunction = (AeroCoeffConstant) this.liftCoef;
            liftCoeffStr = Double.toString(liftFunction.getAerodynamicCoefficient());
        } else {
            liftCoeffStr = this.getLiftCoef().toString();
        }

        // Build result
        try {
            return String.format(
                "%s:[%ntype=%s, %ndragCoef=%s, %nliftCoef=%s, %nvehicleSurfaceModel=%s%n]",
                this.getClass().getSimpleName(), this.getFunctionType().name(), dragCoeffStr,
                liftCoeffStr, this.getVehicleSurfaceModel().toString());
        } catch (final PatriusException e) {
            // Exception
            throw new PatriusExceptionWrapper(e);
        }
    }
}
