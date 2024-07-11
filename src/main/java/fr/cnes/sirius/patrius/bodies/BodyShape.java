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
 * VERSION:4.10.1:FA:FA-3265:02/12/2022:[PATRIUS] Calcul KO des points plus proches entre une Line et un FacetBodyShape
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9.1:FA:FA-3193:01/06/2022:[PATRIUS] Revenir a la signature initiale de la methode getLocalRadius
 * VERSION:4.9:DM:DM-3164:10/05/2022:[PATRIUS] Amelioration de la gestion du GM (parametre gravitationnel)
 * VERSION:4.9:DM:DM-3127:10/05/2022:[PATRIUS] Ajout de deux methodes resize a l'interface GeometricBodyShape ...
 * VERSION:4.9:FA:FA-3170:10/05/2022:[PATRIUS] Incoherence de datation entre la methode getLocalRadius...
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
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.events.AbstractDetector.PropagationDelayType;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Extended interface for celestial bodies shapes : extends the OREKIT's BodyShape interface by adding
 * geometric methods.
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: GeometricBodyShape.java 18080 2017-10-02 16:53:17Z bignon $
 * 
 * @since 1.2
 */
public interface BodyShape extends PVCoordinatesProvider {

    /**
     * Epsilon altitude below which {@link #getIntersectionPoint(Line, Vector3D, Frame, AbsoluteDate)} will be
     * automatically used instead of {@link #getIntersectionPoint(Line, Vector3D, Frame, AbsoluteDate, double)}.
     */
    static final double EPS_ALTITUDE = Precision.DOUBLE_COMPARISON_EPSILON;

    /** Margin type. */
    public enum MarginType {
        /** Distance margin type. */
        DISTANCE,

        /** Scale factor margin type. */
        SCALE_FACTOR
    }

    /**
     * @return the name of the shape
     */
    String getName();

    /**
     * Get body frame related to body shape.
     * 
     * @return body frame related to body shape
     */
    Frame getBodyFrame();

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
     * Get the intersection point of a line with the surface of the body for a given altitude.
     * <p>
     * A line may have several intersection points with a closed surface (we consider the one point case as a
     * degenerated two points case). The close parameter is used to select which of these points should be returned. The
     * selected point is the one that is closest to the close point.
     * </p>
     * 
     * @param line
     *        test line (may intersect the body or not)
     * @param close
     *        point used for intersections selection expressed in the body frame
     * @param frame
     *        frame in which line is expressed
     * @param date
     *        date of the line in given frame
     * @param altitude
     *        altitude of the intersection
     * @return intersection point at provided altitude or null if the line does not intersect the surface
     * @exception PatriusException
     *            if line cannot be converted to body frame
     */
    GeodeticPoint getIntersectionPoint(final Line line,
                                       final Vector3D close,
                                       final Frame frame,
                                       final AbsoluteDate date,
                                       final double altitude) throws PatriusException;

    /**
     * Get the intersection point of a line with the surface of the body.
     * <p>
     * A line may have several intersection points with a closed surface (we consider the one point case as a
     * degenerated two points case). The close parameter is used to select which of these points should be returned. The
     * selected point is the one that is closest to the close point.
     * </p>
     * 
     * @param line
     *        test line (may intersect the body or not)
     * @param close
     *        point used for intersections selection expressed in the body frame
     * @param frame
     *        frame in which line is expressed
     * @param date
     *        date of the line in given frame
     * @return intersection point at altitude zero or null if the line does
     *         not intersect the surface
     * @exception PatriusException
     *            if line cannot be converted to body frame
     */
    GeodeticPoint getIntersectionPoint(Line line, Vector3D close,
                                       Frame frame, AbsoluteDate date) throws PatriusException;

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
     * Compute the apparent radius (in meters) of the occulting body from the spacecraft (observer) position. Given a
     * plane containing the spacecraft (observer) position, the center of the occulting body and the center of the
     * occulted body, and given a line contained within this plane, passing by the spacecraft (observer) position and
     * tangent to the mesh of the occulting body, the apparent radius corresponds to the length of the line starting
     * from the center of the occulting body, perpendicular to the first given line and ending at the intersection of
     * the two lines.
     * 
     * @param pvObserver the spacecraft (observer) position-velocity
     * @param date the date at which the signal is received by the observer (reception date)
     * @param occultedBody the body which is occulted to the spacecraft (observer) by the occulting body
     * @param propagationDelayType propagation delay type
     * @return the apparent radius (in meters) of the occulting body from the spacecraft (observer) position
     * @throws PatriusException if the {@link PVCoordinatesProvider} computation fails
     */
    double getApparentRadius(final PVCoordinatesProvider pvObserver,
                             final AbsoluteDate date,
                             final PVCoordinatesProvider occultedBody,
                             final PropagationDelayType propagationDelayType) throws PatriusException;

    /**
     * Resize the geometric body shape by a margin.
     * 
     * @param marginType margin type to be used
     * @param marginValue margin value to be used (in meters if the margin type is DISTANCE)
     * @return resized geometric body shape with the margin
     * @throws PatriusException if the margin value is invalid
     */
    BodyShape resize(final MarginType marginType, final double marginValue)
        throws PatriusException;

    /**
     * Transform a cartesian point to a surface-relative point.
     * 
     * @param point
     *        cartesian point
     * @param frame
     *        frame in which cartesian point is expressed
     * @param date
     *        date of the computation (used for frames conversions)
     * @return point at the same location but as a surface-relative point
     * @exception PatriusException
     *            if point cannot be converted to body frame
     */
    GeodeticPoint transform(Vector3D point, Frame frame, AbsoluteDate date) throws PatriusException;

    /**
     * Transform a surface-relative point to a cartesian point.
     * 
     * @param point
     *        surface-relative point
     * @return point at the same location but as a cartesian point
     */
    Vector3D transform(GeodeticPoint point);
    
    /**
     * Computes a geodetic point from a normal vector by using spherical coordinates.<br>
     * <p>If no normal is provided (null) then it transforms the cartesian point to a surface-relative point, using
     * the true shape of the body: in case of a facet shape the normal used is the normal of the closest triangle.</p>
     * <p>Spherical coordinates are then applied to provided normal or shape's normal so as to compute corresponding
     * longitude and latitude (spherical azimuth and elevation).</p>
     * <p>The difference of this method with the classic transform method is that either the user defines the normal
     * or the normal is based on model's true shape. The result is that resulting point's longitude and latitude are
     * the same than those of a point with the same normal on a sphere. Therefore, users shall be aware that for
     * instance a vertical facet at North pole leads to horizontal normal, meaning that returned point's latitude will
     * be zero as if the point belonged to the equator.</p>
     * 
     * @param point
     *        cartesian point
     * @param zenith
     *        local surface normal used as topocentric zenith. May be null, if so true local zenith is recomputed
     * @param frame
     *        frame in which point and zenith are expressed
     * @param date
     *        date of the computation (used for frames conversions)
     * @return point at the same location but as a surface-relative point, computed thanks to the local normal of
     *         model's true shape and a spherical transformation
     * @exception PatriusException
     *            if point cannot be converted to body frame
     */
    GeodeticPoint transformFromZenith(final Vector3D point, final Vector3D zenith, final Frame frame,
                                   final AbsoluteDate date) throws PatriusException;

    /**
     * This method computes the two points, on the line and on the body, that are the closest to each other. The
     * returned points are identical if the line intersects the shape: this point is the one with the lowest abscissa.
     * The returned point with index 0 is the point on the Line, while the returned point with index 1 is the point on
     * this.
     * <p>
     * Note: calculations take the line's minimum abscissa into account.
     * </p>
     * 
     * @param line
     *        the original line for the shortest distance computation
     * @param frame
     *        the line's frame
     * @param date
     *        the current date
     * 
     * @return an array of length 2 containing the point of the line (slot [0]) and the point of the shape (slot
     *         [1]) expressed as {@link Vector3D}
     * 
     * @exception PatriusException
     *            if line cannot be converted to body frame
     */
    Vector3D[] closestPointTo(final Line line, final Frame frame, final AbsoluteDate date) throws PatriusException;

}
