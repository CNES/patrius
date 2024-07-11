/**
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
 * @history creation 15/06/2012
 *
 * HISTORY
 * VERSION:4.9.1:FA:FA-3193:01/06/2022:[PATRIUS] Revenir a la signature initiale de la methode getLocalRadius
 * VERSION:4.9:DM:DM-3164:10/05/2022:[PATRIUS] Amelioration de la gestion du GM (parametre gravitationnel)
 * VERSION:4.9:DM:DM-3127:10/05/2022:[PATRIUS] Ajout de deux methodes resize a l'interface GeometricBodyShape ...
 * VERSION:4.9:FA:FA-3170:10/05/2022:[PATRIUS] Incoherence de datation entre la methode getLocalRadius de GeometricBodyShape...
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
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

    /** Margin type. */
    public enum MarginType {
        /** Distance margin type. */
        DISTANCE,

        /** Scale factor margin type. */
        SCALE_FACTOR
    };

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
     * Compute the apparent radius (in meters) of the occulting body from the spacecraft (observer) position. Given a
     * plane containing the spacecraft (observer) position, the center of the occulting body and the center of the
     * occulted body, and given a line contained within this plane, passing by the spacecraft (observer) position and
     * tangent to the mesh of the occulting body, the apparent radius corresponds to the length of the line starting
     * from the center of the occulting body, perpendicular to the first given line and ending at the intersection of
     * the two lines.
     * <p>
     * Please notice that this method will for the moment be used only with an instantaneous propagation delay type.
     * <p>
     * 
     * @param posObserver the spacecraft (observer) position
     * @param frame the reference frame in which the spacecraft (observer) position is expressed
     * @param date the date at which the spacecraft (observer) position is expressed
     * @param occultedBody the body which is occulted to the spacecraft (observer) by the occulting body
     * @return the apparent radius (in meters) of the occulting body from the spacecraft (observer) position
     * @throws PatriusException if the {@link PVCoordinatesProvider} computation fails
     */
    double getLocalRadius(final Vector3D posObserver, final Frame frame, final AbsoluteDate date,
                          final PVCoordinatesProvider occultedBody)
        throws PatriusException;

    /**
     * Resize the geometric body shape by a margin.
     * 
     * @param marginType margin type to be used
     * @param marginValue margin value to be used (in meters if the margin type is DISTANCE)
     * @return resized geometric body shape with the margin
     * @throws PatriusException if the margin value is invalid
     */
    GeometricBodyShape resize(final MarginType marginType, final double marginValue)
        throws PatriusException;
}
