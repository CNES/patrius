/**
 * Copyright 2011-2017 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * HISTORY
* VERSION:4.8:DM:DM-2958:15/11/2021:[PATRIUS] calcul d'intersection a altitude non nulle pour l'interface BodyShape 
* VERSION:4.7:DM:DM-2909:18/05/2021:Ajout des methodes getIntersectionPoint, getClosestPoint(Vector2D) et getAlpha()
* VERSION:4.3:DM:DM-2099:15/05/2019:[PATRIUS] By-passer le critere du pas min dans l'integrateur numerique DOP853
* VERSION:4.3:DM:DM-2102:15/05/2019:[Patrius] Refactoring du paquet fr.cnes.sirius.patrius.bodies
* VERSION:4.3:DM:DM-2090:15/05/2019:[PATRIUS] ajout de fonctionnalites aux bibliotheques mathematiques
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 */
package fr.cnes.sirius.patrius.bodies;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.IEllipsoid;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * Extended interface for spheroid model to represent celestial bodies shape : extends the {@link GeometricBodyShape}
 * interface by adding getters to access the spheroid parameters.
 * </p>
 * 
 * @see GeometricBodyShape
 * 
 * @since 4.3
 */
public interface EllipsoidBodyShape extends GeometricBodyShape {

    /**
     * Get the equatorial radius of the body.
     * 
     * @return equatorial radius of the body (m)
     */
    public double getEquatorialRadius();

    /**
     * Return transverse radius (major semi axis)
     * 
     * @return transverse radius
     */
    public double getTransverseRadius();

    /**
     * Return conjugate radius (minor semi axis)
     * 
     * @return conjugate radius
     */
    public double getConjugateRadius();

    /**
     * Calculate the apparent radius.
     * 
     * @param position
     *        spacecraft position
     * @param frame
     *        frame in which position is expressed
     * @param date
     *        date of position
     * @param occultedBody
     *        body occulted by this
     * @return apparent radius
     * @throws PatriusException
     *         if {@link PVCoordinatesProvider} computation fails
     */
    double getApparentRadius(Vector3D position, Frame frame, AbsoluteDate date,
                             PVCoordinatesProvider occultedBody) throws PatriusException;

    /**
     * Return the flattening
     * 
     * @return fIn
     */
    public double getFlattening();

    /**
     * Return the normal vector to the surface from the ellipsoid
     * 
     * @param point Point as a Vector3D in local basis
     * @return the normal vector in local basis
     */
    public Vector3D getNormal(final Vector3D point);

    /**
     * Return the {@link IEllipsoid spheroid} object
     * 
     * @return the spheroid
     */
    IEllipsoid getEllipsoid();
}