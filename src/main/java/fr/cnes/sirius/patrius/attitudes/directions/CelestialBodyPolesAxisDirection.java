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
 * @history creation 02/12/2011
 *
 * HISTORY
 * VERSION:4.11:DM:DM-3311:22/05/2023:[PATRIUS] Evolutions mineures sur CelestialBody, shape et reperes
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3135:10/05/2022:[PATRIUS] Calcul d'intersection sur BodyShape  
 * VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:227:09/04/2014:Merged eclipse detectors
 * VERSION::FA:261:13/10/2014:JE V2.2 corrections (move IDirection in package attitudes.directions)
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.attitudes.directions;

import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.IAUPoleModelType;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This direction is the axis defined by the two poles of a
 * celestial body.
 * 
 * @concurrency conditionally thread-safe
 * 
 * @concurrency.comment this class is thread-safe only if the underlying CelestialBody is too.
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: CelestialBodyPolesAxisDirection.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.1
 * 
 */
public final class CelestialBodyPolesAxisDirection implements IDirection {

     /** Serializable UID. */
    private static final long serialVersionUID = -5306048170840847315L;

    /** central celestial body */
    private final CelestialBody body;

    /**
     * Build a direction defined by the frame associated to a celestial body.
     * The vector is the Z axis of its oriented frame.
     * 
     * @param inBody
     *        the celestial body associated to the orbit
     * */
    public CelestialBodyPolesAxisDirection(final CelestialBody inBody) {
        // Initialization
        this.body = inBody;
    }

    /**
     * Provides the direction vector at a given date in a given frame.
     * This Direction has no origin, so the pv coordinates are unused.
     * 
     * @param pvCoord
     *        UNUSED : null is accepted
     * @param date
     *        the date
     * @param frame
     *        the frame to project the vector's coordinates
     * @return the direction vector at the given date in the given frame
     * @exception PatriusException
     *            if some frame specific errors occur
     */
    @Override
    public Vector3D getVector(final PVCoordinatesProvider pvCoord,
                              final AbsoluteDate date, final Frame frame) throws PatriusException {

        // creation of the oriented frame locked to the body
        final Frame bodyFrame = this.body.getRotatingFrame(IAUPoleModelType.TRUE);

        // transformation of its Z vector to the output frame
        final Transform toOutputFrame = bodyFrame.getTransformTo(frame, date);

        return toOutputFrame.transformVector(Vector3D.PLUS_K);
    }

    /**
     * Provides the line containing the origin (given PV coordinates) and directed by the direction vector.
     * This Direction has no origin, so the pv coordinates are unused.
     * 
     * @param pvCoord
     *        UNUSED : null is accepted
     * @param date
     *        the current date
     * @param frame
     *        the expression frame of the line
     * @return the Line of space containing the origin and direction vector
     * @throws PatriusException
     *         if some frame specific errors occur
     */
    @Override
    public Line getLine(final PVCoordinatesProvider pvCoord,
                        final AbsoluteDate date, final Frame frame) throws PatriusException {

        // creation of the oriented frame locked to the body
        final Frame bodyFrame = this.body.getRotatingFrame(IAUPoleModelType.TRUE);

        // computation of the origin's PV coordinates
        // in the output frame at the date
        final PVCoordinates centerInOutputFrame = bodyFrame.getPVCoordinates(date, frame);
        final Vector3D positionInOutputVector = centerInOutputFrame.getPosition();

        // transformation of its Z vector to the output frame
        final Transform toOutputFrame = bodyFrame.getTransformTo(frame, date);
        final Vector3D polesVector = toOutputFrame.transformVector(Vector3D.PLUS_K);

        // creation of the line
        return new Line(positionInOutputVector, positionInOutputVector.add(polesVector), positionInOutputVector);
    }

}
