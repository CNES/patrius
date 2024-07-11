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
 * @history Created on 05/10/2011
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

/**
 * <p>
 * Interface for Ellipsoid objects. Basic required methods required by objects implementing this interface are
 * <li>getCenter<br>
 * </p>
 * <p>
 * <br>
 * <u>Note :</u> This interface extends {@link SolidShape SolidShape} <br>
 *  
 * </p>
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: IEllipsoid.java 17583 2017-05-10 13:05:10Z bignon $
 * 
 * @since 1.0
 * 
 */
public interface IEllipsoid extends SolidShape {

    /**
     * Get ellipsoids' center
     * 
     * @return The position of the center as a Vector3D
     * */
    Vector3D getCenter();

    /**
     * Computes the point, on the ellipsoid surface, that is the closest to a point of space.
     * 
     * @param point
     *        the point expressed in standard basis
     * @return the closest point to the user point on the ellipsoid surface
     */
    Vector3D closestPointTo(final Vector3D point);

    /**
     * Computes the normal vector to the surface in local basis
     * 
     * @param point
     *        Point as a Vector3D in local basis
     * @return the normal vector in local basis
     */
    Vector3D getNormal(final Vector3D point);

    /**
     * Get semi axis A
     * 
     * @return semi axis a
     */
    double getSemiA();

    /**
     * Get semi axis B
     * 
     * @return semi axis b
     */
    double getSemiB();

    /**
     * Get semi axis C
     * 
     * @return semi axis c
     */
    double getSemiC();

}
