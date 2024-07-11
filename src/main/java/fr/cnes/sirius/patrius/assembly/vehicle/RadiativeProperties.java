/**
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
 * @history creation 23/05/2018
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1489:23/05/2018:add GENOPUS Custom classes
 * VERSION::FA:1796:07/09/2018:Correction vehicle class
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.vehicle;

import java.io.Serializable;

import fr.cnes.sirius.patrius.assembly.AssemblyBuilder;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeFacetProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeIRProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeSphereProperty;
import fr.cnes.sirius.patrius.assembly.properties.features.Facet;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.Comparators;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.CrossSectionProvider;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Cylinder;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.RightCircularCylinder;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.RightParallelepiped;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Sphere;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Class to define vehicle radiative properties.
 *
 * @author Emmanuel Bignon
 *
 * @version $Id$
 *
 * @since 4.1
 */
public class RadiativeProperties implements Serializable {

    /** UID. */
    private static final long serialVersionUID = 7161601821067777071L;

    /** Prefix for surfaces. */
    private static final String SURF_PREFIX = "surf";

    /** Vehicle's surface model. */
    private VehicleSurfaceModel vehicleSurfaceModel;

    /** Radiative property. */
    private final RadiativeProperty radiativeProperty;

    /** Infrared radiative property. */
    private final RadiativeIRProperty radiativeIRProperty;

    /**
     * Constructor.
     * 
     * @param radiativePropertyIn radiative property
     * @param radiativeIRPropertyIn infrared radiative property (it may be null if no infrared
     *        properties have to be considered)
     * @param vehicleSurfaceModelIn vehicleSurfaceModel
     * @throws PatriusException thrown if the radiative property is null or radiative properties
     *         coefficient sum is not one
     */
    public RadiativeProperties(final RadiativeProperty radiativePropertyIn,
        final RadiativeIRProperty radiativeIRPropertyIn,
        final VehicleSurfaceModel vehicleSurfaceModelIn) throws PatriusException {

        // Radiative Property (it cannot be null)
        if (radiativePropertyIn == null) {
            throw new PatriusException(PatriusMessages.NULL_RADIATIVE_PROPERTY);
        }

        this.radiativeProperty = radiativePropertyIn;

        // Check sum of radiative coefficients
        if (!coeffsSumOne(radiativePropertyIn.getAbsorptionRatio().getValue(), radiativePropertyIn
            .getSpecularReflectionRatio().getValue(), radiativePropertyIn
            .getDiffuseReflectionRatio().getValue())) {
            throw new PatriusException(PatriusMessages.SUM_RADIATIVE_COEFFICIENT_NOT_1);
        }

        // Infrared Radiative Property (it might be null)
        this.radiativeIRProperty = radiativeIRPropertyIn;

        // Check sum of infrared radiative coefficients if there is a radiative property
        if (radiativeIRPropertyIn != null
            && !coeffsSumOne(radiativeIRPropertyIn.getAbsorptionCoef().getValue(),
                radiativeIRPropertyIn.getSpecularReflectionCoef().getValue(),
                radiativeIRPropertyIn.getDiffuseReflectionCoef().getValue())) {
            throw new PatriusException(PatriusMessages.SUM_RADIATIVEIR_COEFFICIENT_NOT_1);
        }

        // Vehicle surface model (it cannot be null)
        if (vehicleSurfaceModelIn == null) {
            throw new PatriusException(PatriusMessages.NULL_VEHICLE_SURFACE_MODEL);
        } else {
            this.vehicleSurfaceModel = vehicleSurfaceModelIn;
        }
    }

    /**
     * Set radiative properties.
     * 
     * @param builder the assembly builder
     * @param mainPartName the main part name
     * @param multiplicativeFactor multiplicative factor
     * @throws PatriusException thrown if shape is not supported
     */
    public void setRadiativeProperties(final AssemblyBuilder builder, final String mainPartName,
                                       final double multiplicativeFactor) throws PatriusException {

        // Check shape type
        final CrossSectionProvider shape = this.vehicleSurfaceModel.getMainPartShape();
        // if the shape is a sphere, set sphereRadiativeProperties
        if (shape instanceof Sphere) {
            this.setSphereRadiativeProperties(builder, mainPartName, multiplicativeFactor);
         // if the shape is a cylinder, set cylinderRadiativeProperties
        } else if (shape instanceof Cylinder) {
            this.setCylinderRadiativeProperties(builder, mainPartName, multiplicativeFactor);
         // if the shape is a right parallelepiped, set ParallelepipedRadiativeProperties
        } else if (shape instanceof RightParallelepiped) {
            final RightParallelepiped parallelepiped = (RightParallelepiped) shape;
            this.setParallelepipedRadiativeProperties(builder, mainPartName, SURF_PREFIX,
                parallelepiped.getSurfX(), parallelepiped.getSurfY(),
                parallelepiped.getSurfZ(), multiplicativeFactor);
        } else {
            // Unknown shape => exception
            throw new PatriusException(PatriusMessages.UNSUPPORTED_SHAPE);
        }

        // Check if solar panels
        if (this.vehicleSurfaceModel.getSolarPanelsShape() != null) {
            this.setSolarPanelsRadiativeProperties(builder, mainPartName, multiplicativeFactor);
        }
    }

    /**
     * Set radiative properties for a sphere.
     * 
     * @param builder assembly builder
     * @param mainPartName main part frame
     * @param multiplicativeFactor multiplicative factor (applied to the surface)
     */
    private void setSphereRadiativeProperties(final AssemblyBuilder builder,
                                              final String mainPartName, final double multiplicativeFactor) {

        // Sphere shape
        final Sphere sphere = (Sphere) this.vehicleSurfaceModel.getMainPartShape();

        // Radiative sphere property
        final Parameter surfParameter = new Parameter("RadiativeSurfaceParameter",
            sphere.getSurface() * multiplicativeFactor);
        final RadiativeSphereProperty sphereProperty = new RadiativeSphereProperty(surfParameter);
        builder.addProperty(sphereProperty, mainPartName);

        // Add radiative property
        builder.addProperty(this.radiativeProperty, mainPartName);

        // Check if infrared has to be added too
        if (this.radiativeIRProperty != null) {
            builder.addProperty(this.radiativeIRProperty, mainPartName);
        }
    }

    /**
     * Set cylinder radiative properties.
     * 
     * @param builder assembly builder
     * @param mainPartName main part frame
     * @param multiplicativeFactor the multiplicative factor (applied to radiative surfaces)
     */
    private void setCylinderRadiativeProperties(final AssemblyBuilder builder,
                                                final String mainPartName, final double multiplicativeFactor) {

        // Cylinder shape
        final RightCircularCylinder cylinder = (RightCircularCylinder) this.vehicleSurfaceModel
            .getMainPartShape();

        // Get surf X
        final double surfX = cylinder.getBaseSurface();
        // Get transversal surface
        final double surfTrans = cylinder.getEquivalentTransversalSurf();

        // Set equivalent parallelepiped radiative properties
        this.setParallelepipedRadiativeProperties(builder, mainPartName, SURF_PREFIX, surfX, surfTrans,
            surfTrans, multiplicativeFactor);
    }

    /**
     * Set parallelepiped radiative properties.
     * 
     * @param builder assembly builder
     * @param mainPartName main part frame
     * @param partPrefix prefix for the part name
     * @param surfX surface perpendicular to x axis (m2)
     * @param surfY surface perpendicular to y axis (m2)
     * @param surfZ surface perpendicular to z axis (m2)
     * @param multiplicativeFactor multiplicative factor (applied to radiative surfaces)
     */
    private void setParallelepipedRadiativeProperties(final AssemblyBuilder builder,
                                                      final String mainPartName, final String partPrefix,
                                                      final double surfX,
                                                      final double surfY, final double surfZ,
                                                      final double multiplicativeFactor) {

        // Add Facets and radiative properties for all 6 faces of parallelepiped
        final Vector3D[] normals = { Vector3D.PLUS_I, Vector3D.PLUS_J, Vector3D.PLUS_K,
            Vector3D.MINUS_I, Vector3D.MINUS_J, Vector3D.MINUS_K };
        final double[] surfaces = { surfX, surfY, surfZ, surfX, surfY, surfZ };
        // For all 6 faces of parallelepiped
        for (int i = 0; i < normals.length; i++) {
            final String partName = partPrefix + normals[i];
            final Facet facet = new Facet(normals[i], surfaces[i] * multiplicativeFactor);
            // Add part
            builder.addPart(partName, mainPartName, Transform.IDENTITY);
            // Add associated properties
            builder.addProperty(new RadiativeFacetProperty(facet), partName);
            builder.addProperty(this.radiativeProperty, partName);
            if (this.radiativeIRProperty != null) {
                builder.addProperty(this.radiativeIRProperty, partName);
            }
        }
    }

    /**
     * Set solar panels radiative properties.
     * 
     * @param builder the assembly builder
     * @param mainPartName the main part name
     * @param multiplicativeFactor the multiplicativeFactor (applied to radiative surfaces)
     */
    private void setSolarPanelsRadiativeProperties(final AssemblyBuilder builder,
                                                   final String mainPartName, final double multiplicativeFactor) {

        final RightParallelepiped solarPanels = this.vehicleSurfaceModel.getSolarPanelsShape();

        this.setParallelepipedRadiativeProperties(builder, mainPartName, "panel",
            solarPanels.getSurfX(), solarPanels.getSurfY(), solarPanels.getSurfZ(),
            multiplicativeFactor);
    }

    /**
     * Get radiative properties.
     * 
     * @return the radiativeProperty (null if not provided)
     */
    public RadiativeProperty getRadiativeProperty() {
        return this.radiativeProperty;
    }

    /**
     * Get infrared radiative properties.
     * 
     * @return the radiativeIRProperty (null if not provided)
     */
    public RadiativeIRProperty getRadiativeIRProperty() {
        return this.radiativeIRProperty;
    }

    /**
     * Get vehicle surface model.
     * 
     * @return the vehicleSurfaceModel
     */
    public VehicleSurfaceModel getVehicleSurfaceModel() {
        return this.vehicleSurfaceModel;
    }

    /**
     * Function to check if the absortion, specular and difusse coefficients sum is one.
     * 
     * @param ka absortion coefficient
     * @param ks specular coefficient
     * @param kd diffuse coefficient
     * @return true if they sum one, false otherwise.
     */
    public static boolean coeffsSumOne(final double ka, final double ks, final double kd) {
        // Precision to check if radiative coefficients sum is equal to 0
        final double precision = 1E-2;
        return Comparators.equals(ka + kd + ks, 1.0, precision);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        String irRadPropStr = null;
        if (this.getRadiativeIRProperty() != null) {
            irRadPropStr = String.format("%s:[ka=%s, ks=%s, kd=%s]", this.getRadiativeIRProperty()
                .getClass().getSimpleName(), this.getRadiativeIRProperty().getAbsorptionCoef()
                .getValue(), this.getRadiativeIRProperty().getSpecularReflectionCoef().getValue(),
                this.getRadiativeIRProperty().getDiffuseReflectionCoef().getValue());
        }
        return String.format(
            // MSG_STATEMENT
            "%s:[%nvisibleRadiativeProperties=%s:[ka=%s, ks=%s, kd=%s], "
                + "%ninfraredRadiativeProperties=%s, %nvehicleSurfaceModel=%s%n]",
            this.getClass().getSimpleName(), this.getRadiativeProperty().getClass().getSimpleName(),
            this.getRadiativeProperty().getAbsorptionRatio().getValue(), this.getRadiativeProperty()
                .getSpecularReflectionRatio().getValue(), this.getRadiativeProperty()
                .getDiffuseReflectionRatio().getValue(), irRadPropStr,
            this.getVehicleSurfaceModel().toString());
    }
}
