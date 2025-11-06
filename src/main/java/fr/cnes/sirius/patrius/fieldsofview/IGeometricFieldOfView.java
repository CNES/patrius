/**
 * 
 * Copyright 2011-2022 CNES
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
 * @history Creation 18/03/2024
 *
 * HISTORY
 * VERSION:4.14.1:OPENFD-396:10/09/2024:[PATRIUS] Erreurs et oublis dans les classes issues de IGeometricFieldOfView
 * VERSION:4.14:OPENFD-173:22/08/2024: Ajout d'une nouvelle interface IGeometricaFieldOfView
 * VERSION:4.14:OPENFD-311:22/08/2024: [PATRIUS] getInputCoord sur EllipsoidPoint
 * VERSION:4.14:DM:DM-173:18/03/2024:[PATRIUS] Creation de la classe à partir de {@link IFieldOfView}
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.fieldsofview;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;

/**
 * This interface specifies the generic concept of {@link IFieldOfView} with conical shapes based on a polygonal
 * section.
 * For such models, a main direction can be defined as the {@link Vector3D}, from the center of the FOV, passing through
 * the center of each section of the cone.
 * 
 * @author Tommaso Capano
 * 
 * @version 4.14
 * 
 * @since 4.14
 * 
 */
public interface IGeometricFieldOfView extends IFieldOfView {

    /**
     * Get the main direction of the geometrical FOV. For every orthogonal section of the FOV, the main direction
     * contains the center of such section.
     *
     * @return The main direction of the FOV
     */
    public Vector3D getMainDirection();

    /**
     * Computes the angular distance between a vector and the border of the field.
     * The result is positive if the direction is in the field, negative otherwise.
     * 
     * <p>
     * Several methods can be defined for the computation. The user can choose the more appropriate one from the enum
     * {@link AngularDistanceType}.
     * </p>
     * 
     * @param direction
     *        the direction vector (expressed in the topocentric coordinate system of the object)
     * @param type
     *        Defines the method to compute the distance from the enum {@link AngularDistanceType}
     * @return the angular distance
     */
    double getAngularDistance(final Vector3D direction, final AngularDistanceType type);

    /**
     * Computes the angular distance between a vector and the border of the field.
     * The result is positive if the direction is in the field, negative otherwise.
     * 
     * <p>
     * For a geometric FOV, the distance can be computed in several ways. This signature uses the
     * {@link AngularDistanceType#MINIMAL} method by default.
     * </p>
     * 
     * @param direction
     *        the direction vector (expressed in the topocentric coordinate system of the object)
     * @return the angular distance
     */
    @Override
    default double getAngularDistance(final Vector3D direction) {
        return this.getAngularDistance(direction, AngularDistanceType.MINIMAL);
    }
    
    
    /**
     * Get the angular opening of the Field Of View (FOV) over a given direction. Considering the half-plane containing
     * both the main direction of the FOV and
     * the input direction, the angular opening is defined as the angle between the main direction and the intersection
     * of the FOV border with the half-plane.
     * 
     * @param directionIn the direction vector (expressed in the topocentric coordinate system of the object)
     * @return the angular opening along the input direction
     */
    public double getAngularOpening(final Vector3D directionIn);
}
