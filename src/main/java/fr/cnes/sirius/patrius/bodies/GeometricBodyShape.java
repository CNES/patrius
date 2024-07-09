/**
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
 * @history creation 15/06/2012
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:227:09/04/2014:Merged eclipse detectors
 * VERSION::FA:410:16/04/2015: Anomalies in the Patrius Javadoc
 * VERSION::DM:611:04/08/2016:New implementation using radii provider for visibility of main/inhibition targets
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Extended interface for celestial bodies shapes : extends the OREKIT's BodyShape interface by adding
 * geometric methods.
 * 
 * @see BodyShape
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: GeometricBodyShape.java 18080 2017-10-02 16:53:17Z bignon $
 * 
 * @since 1.2
 */
public interface GeometricBodyShape extends BodyShape, PVCoordinatesProvider {

    /**
     * Compute the intersection points with a line.
     * 
     * @param line
     *        the line
     * @param frame
     *        in which line is expressed
     * @param date
     *        date of the line in given frame
     * @return the intersection points if they exist. If no intersection is found, the dimension is zero
     * @exception PatriusException
     *            if line cannot be converted to body frame
     */
    Vector3D[] getIntersectionPoints(Line line, Frame frame, AbsoluteDate date) throws PatriusException;

    /**
     * Computes the distance to a line.
     * 
     * @param line
     *        the line
     * @param frame
     *        in which line is expressed
     * @param date
     *        date of the line in given frame
     * @return the shortest distance between the the line and the shape
     * @exception PatriusException
     *            if line cannot be converted to body frame
     */
    double distanceTo(Line line, Frame frame, AbsoluteDate date) throws PatriusException;

    /**
     * @return the name of the shape
     */
    String getName();

    /**
     * Calculate the apparent radius.
     * 
     * @param position
     *        spacecraft position
     * @param frame
     *        frame in which position is expressed
     * @param date
     *        date of position
     * @param occultedBody
     *        body occulted by this
     * @return apparent radius
     * @throws PatriusException
     *         if {@link PVCoordinatesProvider} computation fails
     */
    double getLocalRadius(final Vector3D position, final Frame frame, final AbsoluteDate date,
                          final PVCoordinatesProvider occultedBody) throws PatriusException;
}
