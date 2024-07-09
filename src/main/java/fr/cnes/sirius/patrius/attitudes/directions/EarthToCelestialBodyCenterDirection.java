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
 * @history creation 30/09/2016
 * 
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:605:30/09/2016:gathered Meeus models
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes.directions;

import fr.cnes.sirius.patrius.bodies.CelestialBody;
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
 * 
 * Direction from Earth center to celestial body center : the central body's center is the target point.
 * 
 * @concurrency immutable
 * 
 * @author rodriguest
 * 
 * @version $Id: EarthToCelestialBodyCenterDirection.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 3.3
 * 
 */
public class EarthToCelestialBodyCenterDirection implements ITargetDirection {

    /** Serial UID. */
    private static final long serialVersionUID = 2212101322761333705L;

    /** Earth center frame: GCRF. */
    private static final Frame EARTH_FRAME = FramesFactory.getGCRF();

    /** Celestial body. */
    private final CelestialBody bodyTo;

    /**
     * Constructor for celestial body center direction from Earth center :
     * the celestial body's center is the target point.
     * 
     * @param body
     *        the celestial body
     */
    public EarthToCelestialBodyCenterDirection(final CelestialBody body) {
        this.bodyTo = body;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getVector(final PVCoordinatesProvider pvCoord, final AbsoluteDate date,
                              final Frame frame) throws PatriusException {

        // Transform to output frame
        final Transform t = EARTH_FRAME.getTransformTo(frame, date);

        // Expressed body center direction in output frame
        // Return Earth => celestial body vector in frame
        return t.transformVector(this.bodyTo.getPVCoordinates(date, EARTH_FRAME).getPosition());
    }

    /** {@inheritDoc} */
    @Override
    public Line getLine(final PVCoordinatesProvider pvCoord, final AbsoluteDate date,
                        final Frame frame) throws PatriusException {

        // Transform to output frame
        final Transform t = EARTH_FRAME.getTransformTo(frame, date);

        // Earth center in frame
        final Vector3D earthPosInFrame = t.transformVector(Vector3D.ZERO);

        // Body center in frame
        final Vector3D bodyPosInFrame =
            t.transformVector(this.bodyTo.getPVCoordinates(date, EARTH_FRAME).getPosition());

        // creation of the line
        try {
            return new Line(bodyPosInFrame, earthPosInFrame);
        } catch (final IllegalArgumentException e) {
            throw new PatriusException(e, PatriusMessages.ILLEGAL_LINE);
        }
    }

    /** {@inheritDoc} */
    @Override
    public PVCoordinates getTargetPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
        return new ToCelestialBodyCenterDirection(this.bodyTo).getTargetPVCoordinates(date, frame);
    }
}
