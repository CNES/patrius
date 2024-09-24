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
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11:DM:DM-3259:22/05/2023:[PATRIUS] Creer une interface StarConvexBodyShape
 * VERSION:4.11:FA:FA-3322:22/05/2023:[PATRIUS] Erreur dans le calcul de normale autour d’un OneAxisEllipsoid
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

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.IEllipsoid;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Extended interface for spheroid model to represent celestial bodies shape : extends the {@link StarConvexBodyShape}
 * interface by adding getters to access the spheroid parameters.
 *
 * @see StarConvexBodyShape
 *
 * @since 4.3
 */
public interface EllipsoidBodyShape extends StarConvexBodyShape {

    /**
     * Getter for the equatorial radius of the body.
     *
     * @return the equatorial radius of the body (m)
     */
    public double getEquatorialRadius();

    /**
     * Getter for the transverse radius (major semi axis).
     *
     * @return the transverse radius
     */
    public double getTransverseRadius();

    /**
     * Getter for the conjugate radius (minor semi axis).
     *
     * @return the conjugate radius
     */
    public double getConjugateRadius();

    /**
     * Getter for the flattening.
     *
     * @return the flattening
     */
    public double getFlattening();

    /**
     * Getter for the normal vector to the surface from the ellipsoid.
     *
     * @param point
     *        Point as a Vector3D in local basis
     * @return the normal vector in local basis
     * @deprecated as of 4.12, should use {@link BodyShape#buildPoint(Vector3D, String)} and
     *             {@link BodyPoint#getNormal()} instead
     */
    @Deprecated
    public Vector3D getNormal(Vector3D point);

    /**
     * Getter for the {@link IEllipsoid spheroid} object.
     *
     * @return the spheroid
     */
    public IEllipsoid getEllipsoid();

    /** {@inheritDoc} */
    @Override
    public EllipsoidPoint[] closestPointTo(final Line line, final Frame frame, final AbsoluteDate date)
        throws PatriusException;

    /** {@inheritDoc} */
    @Override
    public EllipsoidPoint[] closestPointTo(final Line line);

    /** {@inheritDoc} */
    @Override
    public EllipsoidPoint closestPointTo(final Vector3D point, final Frame frame, final AbsoluteDate date)
        throws PatriusException;

    /** {@inheritDoc} */
    @Override
    public EllipsoidPoint closestPointTo(final Vector3D point);

    /** {@inheritDoc} */
    @Override
    public EllipsoidPoint closestPointTo(final Vector3D point, final String name);

    /** {@inheritDoc} */
    @Override
    public EllipsoidPoint buildPoint(Vector3D position, String name);

    /** {@inheritDoc} */
    @Override
    public EllipsoidPoint buildPoint(Vector3D position, Frame frame, AbsoluteDate date, String name)
        throws PatriusException;

    /** {@inheritDoc} */
    @Override
    public EllipsoidPoint buildPoint(LLHCoordinatesSystem coordSystem, double latitude, double longitude,
                                     double height, String name);

    /**
     * Getter for the e2 (shape parameter).
     *
     * @return the e2
     */
    public double getE2();

    /**
     * Getter for the g2 (shape parameter).
     *
     * @return the g2
     */
    public double getG2();

    /** {@inheritDoc} */
    @Override
    public EllipsoidPoint getIntersectionPoint(Line line, Vector3D close, Frame frame, AbsoluteDate date)
        throws PatriusException;

    /** {@inheritDoc} */
    @Override
    public EllipsoidPoint getIntersectionPoint(Line line, Vector3D close, Frame frame, AbsoluteDate date, String name)
        throws PatriusException;

    /** {@inheritDoc} */
    @Override
    public EllipsoidPoint getIntersectionPoint(Line line, Vector3D close, Frame frame, AbsoluteDate date,
                                               double altitude)
        throws PatriusException;

    /** {@inheritDoc} */
    @Override
    public EllipsoidPoint[] getIntersectionPoints(Line line, Frame frame, AbsoluteDate date) throws PatriusException;

    /**
     * Compute the ellipsodetic position in body frame.
     *
     * @param latitude
     *        latitude coordinate
     * @param longitude
     *        longitude coordinate
     * @param height
     *        height coordinate (signed value)
     * @return ellipsodetic position in body frame as {@link Vector3D}
     */
    public Vector3D computeEllipsodeticPosition(double latitude, double longitude, double height);
}
