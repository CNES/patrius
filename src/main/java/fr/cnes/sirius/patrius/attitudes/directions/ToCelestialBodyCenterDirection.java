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
 * @history creation 06/12/2011
 * 
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
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
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Direction described by a celestial body :
 * the celestial body's center is the target point.
 * 
 * @concurrency conditionally thread-safe
 * 
 * @concurrency.comment The use of a celestial body linked to the tree of frames
 *                      makes this class thread-safe only if the underlying CelestialBody is too.
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: ToCelestialBodyCenterDirection.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.1
 * 
 */
public final class ToCelestialBodyCenterDirection implements ITargetDirection {

    /** Serial UID. */
    private static final long serialVersionUID = 7407313120126398629L;

    /** central celestial body */
    private final CelestialBody body;

    /**
     * Build a direction described by a celestial body :
     * the celestial body's center is the target point.
     * 
     * @param inBody
     *        the celestial body
     * */
    public ToCelestialBodyCenterDirection(final CelestialBody inBody) {

        // Initialisations
        this.body = inBody;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getVector(final PVCoordinatesProvider origin,
                              final AbsoluteDate date, final Frame frame) throws PatriusException {

        // computation of the origin's position in the output frame at the date
        final PVCoordinates originPV = (origin == null) ? PVCoordinates.ZERO : origin.getPVCoordinates(date, frame);
        final Vector3D originInFrame = originPV.getPosition();

        // computation of the center of the celestial body point in the output frame
        final Frame bodyCenteredFrame = this.body.getInertiallyOrientedFrame();
        final Transform bodyFrametoOutputFrame = bodyCenteredFrame.getTransformTo(frame, date);
        final Vector3D bodyCenterInFrame = bodyFrametoOutputFrame.transformPosition(Vector3D.ZERO);

        // the returned vector is from the origin center to the center of the celestial body to
        return bodyCenterInFrame.subtract(originInFrame);
    }

    /** {@inheritDoc} */
    @Override
    public PVCoordinates getTargetPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {

        // computation of the center of the celestial body point in the output frame
        final Frame bodyCenteredFrame = this.body.getInertiallyOrientedFrame();
        final Transform bodyFrametoOutputFrame = bodyCenteredFrame.getTransformTo(frame, date);

        // null coordinates : the center of the celestial body in its own frame
        final PVCoordinates zeroCoordinates = new PVCoordinates(Vector3D.ZERO, Vector3D.ZERO);

        // computation of the celestial body center PV coordinates
        // in the output frame at the date
        return bodyFrametoOutputFrame.transformPVCoordinates(zeroCoordinates);
    }

    /** {@inheritDoc} */
    @Override
    public Line getLine(final PVCoordinatesProvider origin,
                        final AbsoluteDate date, final Frame frame) throws PatriusException {

        Line line = null;

        // computation of the origin's position in the output frame at the date
        final PVCoordinates originPV = (origin == null) ? PVCoordinates.ZERO : origin.getPVCoordinates(date, frame);
        final Vector3D originPointInFrame = originPV.getPosition();

        // computation of the center of the celestial body point in the output frame
        final Frame bodyCenteredFrame = this.body.getInertiallyOrientedFrame();
        final Transform bodyFrametoOutputFrame = bodyCenteredFrame.getTransformTo(frame, date);
        final Vector3D bodyCenterInFrame = bodyFrametoOutputFrame.transformPosition(Vector3D.ZERO);

        // creation of the line
        try {
            line = new Line(bodyCenterInFrame, originPointInFrame);
        } catch (final IllegalArgumentException e) {
            throw new PatriusException(e, PatriusMessages.ILLEGAL_LINE);
        }
        return line;
    }
}
