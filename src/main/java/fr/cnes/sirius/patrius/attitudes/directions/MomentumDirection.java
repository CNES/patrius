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
 * @history creation 01/12/2011
 *
 * HISTORY
 * VERSION:4.9:DM:DM-3135:10/05/2022:[PATRIUS] Calcul d'intersection sur BodyShape  
 * VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:FA:FA-2762:18/05/2021:Probleme lors des controles qualite via la PIC 
 * VERSION:4.6:DM:DM-2648:27/01/2021:[PATRIUS] Definir une direction a partir du moment cinetique d'une trajectoire
 * orbitale
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:227:09/04/2014:Merged eclipse detectors
 * VERSION::FA:261:13/10/2014:JE V2.2 corrections (move IDirection in package attitudes.directions)
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.attitudes.directions;

import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.UtilsPatrius;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Direction described either:
 * <ul>
 * <li>By a celestial body (the reference body of the orbit).
 * For a given PVCoordinatesProvider origin point the associated vector is the normalised cross
 * product of the position to the celestial body and the velocity (momentum vector).</li>
 * <li>By a {@link PVCoordinatesProvider}. In this case the momentum direction is simply the cross product between
 * the position and the velocity of the {@link PVCoordinatesProvider} at the required date.</li>
 * </ul>
 * 
 * @concurrency conditionally thread-safe
 * 
 * @concurrency.comment The use of a celestial body linked to the tree of frames
 *                      makes this class thread-safe only if the underlying CelestialBody is too.
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: MomentumDirection.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.1
 * 
 */
public final class MomentumDirection implements IDirection {

    /** Serial UID. */
    private static final long serialVersionUID = -5084380876017048861L;

    /** Vectors comparisons epsilon */
    private static final double VECTORSCOMPARISONEPS = UtilsPatrius.GEOMETRY_EPSILON;

    /** Central celestial body */
    private final CelestialBody body;

    /** PV coordinate provider. */
    private final PVCoordinatesProvider pvProvider;

    /**
     * Build a direction from the celestial body around witch the orbit is defined.
     * Giving an origin point, the associated vector is the normalised cross
     * product of the position to the celestial body associated to the orbit and the velocity
     * at a specific date (momentum vector).
     * 
     * @param body
     *        the celestial body associated to the orbit
     * */
    public MomentumDirection(final CelestialBody body) {
        this(body, null);
    }

    /**
     * Build a direction from a {@link PVCoordinatesProvider}.
     * Giving an origin point, the associated vector is the normalised cross
     * product of the position and velocity vectors of the {@link PVCoordinatesProvider} at a specific date
     * (momentum vector).
     * @param pvProvider
     *        the {@link PVCoordinatesProvider} from which the momentum direction should be computed
     * */
    public MomentumDirection(final PVCoordinatesProvider pvProvider) {
        this(null, pvProvider);
    }

    /**
     * Private generic constructor.
     * 
     * @param body
     *        the celestial body associated to the orbit
     * @param pvProvider
     *        the {@link PVCoordinatesProvider} from which the momentum direction should be computed
     * */
    private MomentumDirection(final CelestialBody body, final PVCoordinatesProvider pvProvider) {
        this.body = body;
        this.pvProvider = pvProvider;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getVector(final PVCoordinatesProvider origin,
                              final AbsoluteDate date, final Frame frame) throws PatriusException {

        if (body != null) {
            // Body case
            // computation of the center of the celestial body point in the output frame
            final Frame bodyCenteredFrame = this.body.getInertialEquatorFrame();
            final Transform bodyFrametoOutputFrame = bodyCenteredFrame.getTransformTo(frame, date);

            // computation of the origin's position in the output frame at the date
            final PVCoordinates originPV =
                (origin == null) ? PVCoordinates.ZERO : origin.getPVCoordinates(date, bodyCenteredFrame);

            final Vector3D originInFrame;
            final Vector3D originVelocity;

            // velocity wrt the reference frame
            if (originPV == PVCoordinates.ZERO) {
                originInFrame = bodyFrametoOutputFrame.getTranslation();
                originVelocity = bodyFrametoOutputFrame.getVelocity();
            } else {
                originInFrame = originPV.getPosition();
                // projection in the given frame of the velocity vector expressed in the reference frame
                originVelocity = originPV.getVelocity();
            }

            // cross product computation
            Vector3D normalVector = Vector3D.crossProduct(originInFrame, originVelocity);

            // the vector is normed : an exception is thrown if its norm is zero
            if (normalVector.getNorm() > VECTORSCOMPARISONEPS) {
                normalVector = normalVector.normalize();
            } else {
                throw new PatriusException(PatriusMessages.POSITION_PARALLEL_TO_VELOCITY);
            }

            return bodyFrametoOutputFrame.transformVector(normalVector);
        } else {
            // PVCoordinateProvider case
            return pvProvider.getPVCoordinates(date, frame).getMomentum().normalize();
        }
    }

    /** {@inheritDoc} */
    @Override
    public Line getLine(final PVCoordinatesProvider origin,
                        final AbsoluteDate date, final Frame frame) throws PatriusException {

        Line line = null;

        // computation of the origin's position in the output frame at the date
        final PVCoordinates originPV = (origin == null) ? PVCoordinates.ZERO : origin.getPVCoordinates(date, frame);
        final Vector3D originPosition = originPV.getPosition();
        final Vector3D normalVector = this.getVector(origin, date, frame);

        // creation of the line
        // the vector returned by the getVector method can't be ZERO
        // no need to catch the exception
        line = new Line(originPosition, originPosition.add(normalVector), originPosition);

        return line;
    }

}
