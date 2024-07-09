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
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.vehicle;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.CrossSectionProvider;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.RightParallelepiped;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Vehicle surface model class. It defines a derivative function parameterizable where the variable
 * is the spacecraft state and the function is the cross section surface (including solar panels) as
 * seen from the velocity vector direction. It includes a multiplicative factor as a parameter.
 *
 * @author Emmanuel Bignon
 *
 * @version $Id$
 *
 * @since 4.1
 */
public class VehicleSurfaceModel implements CrossSectionProvider, Serializable {

    /** Multiplicative factor parameter name. */
    public static final String MULTIPLICATIVE_FACTOR = "MultiplicativeFactor";

    /** Serial UID. */
    private static final long serialVersionUID = 4082310673896050752L;

    /** Hash code. */
    private static final int HASH_CODE = 41;

    /** Main part shape. */
    private final CrossSectionProvider mainPartShape;

    /** Solar panels. */
    private final RightParallelepiped solarPanelsShape;

    /** Surface multiplicative factor. */
    private final Parameter multiplicativeFactor;

    /**
     * Constructor without solar panels and with default multiplicative factor set to 1.0.
     * 
     * @param vehicleShape main part shape (cannot be null)
     * @throws PatriusException thrown if main part is null
     */
    public VehicleSurfaceModel(final CrossSectionProvider vehicleShape) throws PatriusException {
        this(vehicleShape, null);
    }

    /**
     * Constructor with default multiplicative factor set to 1.0.
     * 
     * @param vehicleShape main part shape (cannot be null)
     * @param solarPanels solar panels shape (it may be null)
     * @throws PatriusException thrown if main part is null
     */
    public VehicleSurfaceModel(final CrossSectionProvider vehicleShape,
        final RightParallelepiped solarPanels) throws PatriusException {
        this(vehicleShape, solarPanels, 1.0);
    }

    /**
     * Constructor.
     * 
     * @param mainPart main part shape (cannot be null)
     * @param solarPanels solar panels shape (it may be null)
     * @param multiplicativeFactorIn multiplicative factor for the total vehicle surface
     * @throws PatriusException thrown if main part is null
     */
    public VehicleSurfaceModel(final CrossSectionProvider mainPart,
        final RightParallelepiped solarPanels, final double multiplicativeFactorIn)
        throws PatriusException {

        // Vehicle shape (cannot be null)
        this.mainPartShape = mainPart;
        if (mainPart == null) {
            throw new PatriusException(PatriusMessages.PDB_NO_MAIN_PART);
        }

        // Solar panels (it might be null)
        this.solarPanelsShape = solarPanels;

        // Multiplicative factor
        this.multiplicativeFactor = new Parameter(MULTIPLICATIVE_FACTOR, multiplicativeFactorIn);
    }

    /** {@inheritDoc} */
    @Override
    public double getCrossSection(final Vector3D direction) {
        double surface = this.getMainPartShape().getCrossSection(direction);
        if (this.solarPanelsShape != null) {
            surface += this.getSolarPanelsShape().getCrossSection(direction);
        }
        return surface;
    }

    /**
     * Get the multiplicative factor applied to the reference surface as a parameter.
     * 
     * @return the multiplicative factor parameter
     */
    public Parameter getMultiplicativeFactor() {
        return this.multiplicativeFactor;
    }

    /**
     * Set the multiplicative factor applied to the reference surface.
     * 
     * @param multiplicativeFactorIn the multiplicative factor
     */
    public void setMultiplicativeFactor(final double multiplicativeFactorIn) {
        this.multiplicativeFactor.setValue(multiplicativeFactorIn);
    }

    /**
     * Get the main part vehicle shape.
     * 
     * @return the shape
     */
    public CrossSectionProvider getMainPartShape() {
        return this.mainPartShape;
    }

    /**
     * Get solar panels. If no solar panels are defined, null is returned.
     * 
     * @return the solar panels
     */
    public RightParallelepiped getSolarPanelsShape() {
        return this.solarPanelsShape;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object other) {
        boolean result = (this == other);
        if (!result && other instanceof VehicleSurfaceModel) {
            final VehicleSurfaceModel that = (VehicleSurfaceModel) other;
            // Comparing main parts
            final boolean equalMainParts = this.mainPartShape.equals(that.mainPartShape);
            // Comparing solar panels
            final boolean bothSolarPanelsNull = this.solarPanelsShape == null
                && that.solarPanelsShape == null;
            final boolean nonNullEqualSolarPanels = this.solarPanelsShape != null
                && this.solarPanelsShape.equals(that.solarPanelsShape);
            final boolean equalSolarPanels = nonNullEqualSolarPanels || bothSolarPanelsNull;
            // Comparing multiplicative factors
            final boolean eqMultFact = this.multiplicativeFactor.getValue() == that.multiplicativeFactor
                .getValue()
                && this.multiplicativeFactor.getName().equals(that.multiplicativeFactor.getName());
            result = equalMainParts && equalSolarPanels && eqMultFact;
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return HASH_CODE
            * (HASH_CODE * (HASH_CODE + this.mainPartShape.hashCode()) + this.solarPanelsShape.hashCode())
            + new Double(this.multiplicativeFactor.getValue()).hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return String.format("%s:[mainPartShape=%s, solarPanelsShape=%s, multiplicativeFactor=%s]",
            this.getClass().getSimpleName(), this.getMainPartShape().toString(), this.getSolarPanelsShape(),
            this.getMultiplicativeFactor().getValue());
    }
}
