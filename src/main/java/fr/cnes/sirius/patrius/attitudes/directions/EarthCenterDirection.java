/**
 * 
 * Copyright 2011-2017 CNES
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
 * @history creation 22/10/2015
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:419:22/10/2015: Creation direction to central body center
 * VERSION::DM:557:15/02/2016: class rename
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes.directions;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Direction to Earth body center : the central body's center is the target point.
 * This direction is directed toward GCRF frame origin (i.e. Earth center).
 * 
 * @concurrency immutable
 * 
 * @author maggioranic
 * 
 * @version $Id: EarthCenterDirection.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 3.1
 * 
 */
public class EarthCenterDirection implements ITargetDirection {

    /** Serial UID. */
    private static final long serialVersionUID = -6945414164388862443L;

    /** Body centre frame: GCRF. */
    private static final Frame BODY_FRAME = FramesFactory.getGCRF();

    /** {@inheritDoc} */
    @Override
    public Vector3D getVector(final PVCoordinatesProvider pvCoord, final AbsoluteDate date,
                              final Frame frame) throws PatriusException {

        final Transform t = frame.getTransformTo(BODY_FRAME, date);

        // computation of the origin's position in the output frame at the date
        final Vector3D posInFrame = (pvCoord == null) ? Vector3D.ZERO : pvCoord.getPVCoordinates(date, frame)
            .getPosition();

        // computation of the origin's position in the GCRF frame at the date
        final Vector3D posInGCRF = t.transformPosition(posInFrame);

        // computation of the negated origin's position in the GCRF frame at the date
        final Vector3D vectorInGCRF = posInGCRF.negate();

        // return the negated origin's position in the output frame
        return t.getInverse().transformVector(vectorInGCRF);
    }

    /** {@inheritDoc} */
    @Override
    public Line getLine(final PVCoordinatesProvider pvCoord, final AbsoluteDate date,
                        final Frame frame) throws PatriusException {

        // computation of the origin's PV in the output frame at the date
        final Vector3D originPosition = (pvCoord == null) ? Vector3D.ZERO : pvCoord.getPVCoordinates(date, frame)
            .getPosition();

        // computation of the target's position in the output frame at the date
        final Vector3D targetPosition = this.getTargetPVCoordinates(date, frame).getPosition();

        // creation of the line
        try {
            return new Line(originPosition, targetPosition);
        } catch (final IllegalArgumentException e) {
            throw new PatriusException(e, PatriusMessages.ILLEGAL_LINE);
        }
    }

    /** {@inheritDoc} */
    @Override
    public PVCoordinates getTargetPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
        // computation of the target's PV coordinates
        // in the output frame at the date
        final Transform t = BODY_FRAME.getTransformTo(frame, date);
        return t.transformPVCoordinates(PVCoordinates.ZERO);
    }

}
