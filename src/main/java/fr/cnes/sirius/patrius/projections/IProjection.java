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
 * HISTORY
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:525:22/04/2016: add new functionalities existing in LibKernel
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.projections;

import java.io.Serializable;

import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.EllipsoidPoint;
import fr.cnes.sirius.patrius.math.geometry.euclidean.twod.Vector2D;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Interface for projections on an ellipsoid.
 * 
 * @author Galpin Thomas
 * @since 3.2
 * @version $Id$
 */
public interface IProjection extends Serializable {

    /**
     * Returns a boolean depending if the geodetic point can be map with the selected projection method.
     * 
     * @param point
     *        point to test if representable
     * @return true if the geodetic point can be represented on the map with the chosen projection method
     */
    boolean canMap(final EllipsoidPoint point);

    /**
     * Returns Easting value and Northing value in meters from the point coordinates.
     * 
     * @param point
     *        the point to transform
     * @return Vector2D containing Easting value and Northing value in meters
     * @throws PatriusException
     *         if projection could not be computed
     */
    Vector2D applyTo(final EllipsoidPoint point) throws PatriusException;

    /**
     * Returns Easting value and Northing value in meters from latitude and longitude coordinates.
     * 
     * @param lat
     *        latitude of the point to project
     * @param lon
     *        longitude of the point to project
     * @return Vector2D containing Easting value and Northing value in meters
     * @throws PatriusException
     *         if projection could not be computed
     */
    Vector2D applyTo(final double lat, final double lon) throws PatriusException;

    /**
     * Inverse projection.<br>
     * Returns geodetic coordinates.
     * 
     * @param x
     *        abscissa coordinate
     * @param y
     *        ordinate coordinate
     * @return geodetic coordinates
     * @throws PatriusException
     *         if inverse projection could not be computed
     */
    EllipsoidPoint applyInverseTo(final double x, final double y) throws PatriusException;

    /**
     * This is the Two standard parallel Mercator Projection model. The latitude and the longitude of the given point to
     * convert can be defined from any natural origin and the user can set the altitude.
     * 
     * @param x
     *        abscissa coordinate
     * @param y
     *        ordinate coordinate
     * @param alt
     *        altitude coordinate
     * @return coordinate
     * @throws PatriusException
     *         if inverse projection could not be computed
     */
    EllipsoidPoint applyInverseTo(final double x, final double y, final double alt) throws PatriusException;

    /**
     * Inform the user if the direct transformation is a conformal 's one (If yes, it preserves angles).
     * 
     * @return a boolean
     */
    boolean isConformal();

    /**
     * Inform the user if the direct transformation is an equivalent 's one (If yes, it preserves surfaces).
     * 
     * @return a boolean
     */
    boolean isEquivalent();

    /**
     * Getter for the line property.
     * 
     * @return line property
     */
    EnumLineProperty getLineProperty();

    /**
     * Getter for the maximum latitude that the projection can map.
     * 
     * @return the maximum latitude that the projection can map
     */
    double getMaximumLatitude();

    /**
     * Getter for the maximum value for X projected.
     * 
     * @return the maximum value for X projected
     */
    double getMaximumEastingValue();

    /**
     * Getter for the system of reference used.
     * 
     * @return the system of reference
     */
    BodyShape getReference();
}
