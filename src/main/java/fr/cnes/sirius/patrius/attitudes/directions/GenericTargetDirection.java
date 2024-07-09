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
 * @history creation 30/11/2011
 * 
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.attitudes.directions;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Direction described by a target PVCoordinatesProvider.
 * The vector is at any date computed from the given PVCoordinate origin
 * to the target.
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment Not thread-safe by default.
 *                      No use case for sharing an instance between threads found.
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: GenericTargetDirection.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.1
 * 
 */
public final class GenericTargetDirection implements ITargetDirection {

    /** Serial UID. */
    private static final long serialVersionUID = 3773927800981656340L;

    /** Target point. */
    private final PVCoordinatesProvider target;

    /**
     * Build a direction from a target described by its
     * PVCoordinatesProvider
     * 
     * @param inTarget
     *        the PVCoordinatesProvider describing the target point
     * */
    public GenericTargetDirection(final PVCoordinatesProvider inTarget) {

        // Initialisation
        this.target = inTarget;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getVector(final PVCoordinatesProvider origin,
                              final AbsoluteDate date, final Frame frame) throws PatriusException {

        // computation of the origin's position in the output frame at the date
        final PVCoordinates originPV = (origin == null) ? PVCoordinates.ZERO : origin.getPVCoordinates(date, frame);
        final Vector3D originPosition = originPV.getPosition();

        // computation of the target's position in the output frame at the date
        final PVCoordinates targetPV = this.target.getPVCoordinates(date, frame);
        final Vector3D targetPosition = targetPV.getPosition();

        // the return is the vector from the origin to the target
        return targetPosition.subtract(originPosition);
    }

    /** {@inheritDoc} */
    @Override
    public PVCoordinates getTargetPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {

        // computation of the target's PV coordinates
        // in the output frame at the date
        return this.target.getPVCoordinates(date, frame);
    }

    /** {@inheritDoc} */
    @Override
    public Line getLine(final PVCoordinatesProvider origin,
                        final AbsoluteDate date, final Frame frame) throws PatriusException {

        Line line = null;

        // computation of the origin's position in the output frame at the date
        final PVCoordinates originPV = (origin == null) ? PVCoordinates.ZERO : origin.getPVCoordinates(date, frame);
        final Vector3D originPosition = originPV.getPosition();

        // computation of the target's position in the output frame at the date
        final PVCoordinates targetPV = this.target.getPVCoordinates(date, frame);
        final Vector3D targetPosition = targetPV.getPosition();

        // creation of the line
        try {
            line = new Line(originPosition, targetPosition);
        } catch (final IllegalArgumentException e) {
            throw new PatriusException(e, PatriusMessages.ILLEGAL_LINE);
        }
        return line;
    }

}
