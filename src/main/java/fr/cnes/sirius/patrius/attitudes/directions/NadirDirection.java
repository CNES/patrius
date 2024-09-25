/**
 *
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
 * @history creation 20/06/2012
 *
 * HISTORY
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration gestion attractions gravitationnelles
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3135:10/05/2022:[PATRIUS] Calcul d'intersection sur GeometricBodyShape  
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:227:02/10/2014:Merged eclipse detectors and added eclipse detector by lighting ratio
 * VERSION::FA:261:13/10/2014:JE V2.2 corrections (move IDirection in package attitudes.directions)
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes.directions;

import fr.cnes.sirius.patrius.bodies.EllipsoidBodyShape;
import fr.cnes.sirius.patrius.bodies.EllipsoidPoint;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Nadir direction. This direction depends on the body shape and the satellite position.
 * <p>
 * This class is restricted to be used with {@link EllipsoidBodyShape}.
 * </p>
 *
 * @concurrency conditionally thread-safe
 *
 * @concurrency.comment The use of a celestial body linked to the tree of frames
 *                      makes this class thread-safe only if the underlying CelestialBody is too.
 *
 * @author Julie Anton
 *
 * @version $Id: NadirDirection.java 17582 2017-05-10 12:58:16Z bignon $
 *
 * @since 1.2
 *
 */
public final class NadirDirection implements IDirection {

    /** Serializable UID. */
    private static final long serialVersionUID = -2814307443509757488L;

    /** Multiplicative factor for increased precision. */
    private static final double MULTIPLICATIVE_FACTOR = 1E14;

    /** Body shape. */
    private final EllipsoidBodyShape shape;

    /**
     * Simple constructor
     *
     * @param bodyShape
     *        The body shape
     */
    public NadirDirection(final EllipsoidBodyShape bodyShape) {

        this.shape = bodyShape;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getVector(final PVCoordinatesProvider pvCoord, final AbsoluteDate date,
                              final Frame frame) throws PatriusException {
        return this.getLine(pvCoord, date, frame).getDirection();
    }

    /** {@inheritDoc} */
    @Override
    public Line getLine(final PVCoordinatesProvider pvCoord, final AbsoluteDate date, final Frame frame)
        throws PatriusException {

        // satellite position in specified frame
        final Vector3D satPosition = pvCoord == null ? Vector3D.ZERO : pvCoord.getPVCoordinates(date, frame)
            .getPosition();
        // Obtain the nadir point
        final EllipsoidPoint nadirPoint = this.shape.buildPoint(satPosition, frame, date, "nadirPoint");
        // Compute the transform from body frame to given frame
        final Transform transform = this.shape.getBodyFrame().getTransformTo(frame, date);
        // Retrieve the nadir direction
        final Vector3D nadirDirection = nadirPoint.getNormal().negate();
        // Create and return the requested line
        // A multiplicative factor is used for increased precision
        // Note: transformLine is not used, since it leads to some numerical quality issues in some cases
        return Line.createLine(satPosition,
            transform.transformVector(nadirDirection.scalarMultiply(MULTIPLICATIVE_FACTOR)));
    }
}
