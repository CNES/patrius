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
 * @history creation 20/06/2012
 * 
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:227:02/10/2014:Merged eclipse detectors and added eclipse detector by lighting ratio
 * VERSION::FA:261:13/10/2014:JE V2.2 corrections (move IDirection in package attitudes.directions)
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes.directions;

import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Nadir direction. This direction depends on the body shape and the satellite position.
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

    /** Serial UID. */
    private static final long serialVersionUID = -2814307443509757488L;

    /** Body shape. */
    private final BodyShape shape;

    /**
     * Simple constructor
     * 
     * @param bodyShape
     *        : body shape
     */
    public NadirDirection(final BodyShape bodyShape) {
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
    public Line getLine(final PVCoordinatesProvider pvCoord, final AbsoluteDate date,
                        final Frame frame) throws PatriusException {
        // satellite position in specified frame
        final Vector3D satPosition = (pvCoord == null) ? Vector3D.ZERO : pvCoord.getPVCoordinates(date, frame)
            .getPosition();

        // geodetic position of the satellite
        final Vector3D nadirPosition = this.getTargetPoint(pvCoord, date, frame);

        // line
        return new Line(satPosition, nadirPosition);
    }

    /**
     * Compute the nadir point position.
     * 
     * @param pvProv
     *        the pv coordinates provider (satellite orbit)
     * @param date
     *        date at which target point is requested
     * @param frame
     *        frame in which observed ground point should be provided
     * @return nadir point position as a {@link Vector3D}
     * @throws PatriusException
     *         if position cannot be computed in given frame
     */
    private Vector3D getTargetPoint(final PVCoordinatesProvider pvProv, final AbsoluteDate date,
                                    final Frame frame) throws PatriusException {
        // satellite position in specified frame
        final Vector3D satPosition = (pvProv == null) ? Vector3D.ZERO : pvProv.getPVCoordinates(date, frame)
            .getPosition();

        // geodetic position of the satellite
        final GeodeticPoint nadirPoint = this.shape.transform(satPosition, frame, date);

        // nadir ground point
        final GeodeticPoint gpNadirPoint = new GeodeticPoint(nadirPoint.getLatitude(), nadirPoint.getLongitude(), 0.);

        // cartesian coordinates of the nadir point in the given frame
        return this.shape.getBodyFrame().getTransformTo(frame, date)
            .transformPosition(this.shape.transform(gpNadirPoint));
    }
}