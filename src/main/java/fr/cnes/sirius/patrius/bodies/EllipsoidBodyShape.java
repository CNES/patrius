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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9.1:FA:FA-3193:01/06/2022:[PATRIUS] Revenir a la signature initiale de la methode getLocalRadius
 * VERSION:4.9:DM:DM-3127:10/05/2022:[PATRIUS] Ajout de deux methodes resize a l'interface GeometricBodyShape ...
 * VERSION:4.9:FA:FA-3170:10/05/2022:[PATRIUS] Incoherence de datation entre la methode getLocalRadius...
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2958:15/11/2021:[PATRIUS] calcul d'intersection a altitude non nulle pour l'interface BodyShape 
 * VERSION:4.7:DM:DM-2909:18/05/2021:Ajout des methodes getIntersectionPoint, getClosestPoint(Vector2D) et getAlpha()
 * VERSION:4.3:DM:DM-2099:15/05/2019:[PATRIUS] By-passer le critere du pas min dans l'integrateur numerique DOP853
 * VERSION:4.3:DM:DM-2102:15/05/2019:[Patrius] Refactoring du paquet fr.cnes.sirius.patrius.bodies
 * VERSION:4.3:DM:DM-2090:15/05/2019:[PATRIUS] ajout de fonctionnalites aux bibliotheques mathematiques
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.IEllipsoid;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;

/**
 * <p>
 * Extended interface for spheroid model to represent celestial bodies shape : extends the {@link BodyShape}
 * interface by adding getters to access the spheroid parameters.
 * </p>
 * 
 * @see BodyShape
 * 
 * @since 4.3
 */
public interface EllipsoidBodyShape extends BodyShape {

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
