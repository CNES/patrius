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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3135:10/05/2022:[PATRIUS] Calcul d'intersection sur BodyShape  
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:227:09/04/2014:Merged eclipse detectors
 * VERSION::FA:261:13/10/2014:JE V2.2 corrections (move IDirection in package attitudes.directions)
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.attitudes.directions;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Direction defined for any PVCoordinatesProvider origin by its velocity vector, expressed in a reference frame
 * (parameter of the constructor). The vector is then only projected in the input frame of the getVector or getLine
 * methods.
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment Not thread-safe by default. No use case for sharing an instance between threads found.
 * 
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: VelocityDirection.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.1
 * 
 */
public final class VelocityDirection implements IDirection {

     /** Serializable UID. */
    private static final long serialVersionUID = 1528874572071204725L;

    /**
     * Reference frame
     */
    private final Frame refFrame;

    /**
     * Build a Direction defined for any PVCoordinatesProvider origin by its velocity vector with respect to a reference
     * frame.
     * 
     * @param referenceFrame
     *        the reference expression frame of the velocity vector
     * */
    public VelocityDirection(final Frame referenceFrame) {
        this.refFrame = referenceFrame;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getVector(final PVCoordinatesProvider origin, final AbsoluteDate date,
                              final Frame frame) throws PatriusException {
        // computation of the origin's velocity in the output frame at the date
        final PVCoordinates originPV =
            (origin == null) ? PVCoordinates.ZERO : origin.getPVCoordinates(date, this.refFrame);
        // transformation between the reference frame and the projection frame
        final Transform toProjectionFrame = this.refFrame.getTransformTo(frame, date);

        // velocity wrt the reference frame
        final Vector3D velocity;
        if (originPV == PVCoordinates.ZERO) {
            velocity = toProjectionFrame.getVelocity();
        } else {
            // projection in the given frame of the velocity vector expressed in the reference frame
            velocity = originPV.getVelocity();
        }

        // projection of the velocity vector onto the given frame
        return toProjectionFrame.transformVector(velocity);
    }

    /** {@inheritDoc} */
    @Override
    public Line getLine(final PVCoordinatesProvider origin, final AbsoluteDate date,
                        final Frame frame) throws PatriusException {

        Line line = null;

        // computation of the origin's position and velocity in the output frame at the date
        final PVCoordinates originPV = (origin == null) ? PVCoordinates.ZERO : origin.getPVCoordinates(date, frame);
        final Vector3D originPosition = originPV.getPosition();

        // creation of the line containing the origin and directed by the velocity
        try {
            line = new Line(originPosition, originPosition.add(this.getVector(origin, date, frame)), originPosition);
        } catch (final IllegalArgumentException e) {
            throw new PatriusException(e, PatriusMessages.ILLEGAL_LINE);
        }
        return line;
    }

}
