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
 * VERSION:4.13:DM:DM-3:08/12/2023:[PATRIUS] Distinction entre corps celestes et barycentres
 * VERSION:4.13:DM:DM-32:08/12/2023:[PATRIUS] Ajout d'un ThreeAxisEllipsoid
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.13:DM:DM-70:08/12/2023:[PATRIUS] Calcul de jacobienne dans OneAxisEllipsoid
 * VERSION:4.13:DM:DM-37:08/12/2023:[PATRIUS] Date d'evenement et propagation du signal
 * VERSION:4.13:FA:FA-144:08/12/2023:[PATRIUS] la methode BodyShape.getBodyFrame devrait
 * retourner un CelestialBodyFrame
 * VERSION:4.12:DM:DM-7:17/08/2023:[PATRIUS] Symétriser les méthodes closestPointTo de BodyShape
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11.1:FA:FA-52:30/06/2023:[PATRIUS] Précision dans la méthode FacetBodyShape.getFieldData
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11:DM:DM-7:22/05/2023:[PATRIUS] Symetriser les methodes closestPointTo de BodyShape
 * VERSION:4.11:DM:DM-3318:22/05/2023:[PATRIUS] Forcer normalisation dans QuaternionPolynomialSegment
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

import fr.cnes.sirius.patrius.bodies.BodyPoint.BodyPointName;
import fr.cnes.sirius.patrius.events.detectors.AbstractSignalPropagationDetector.PropagationDelayType;
import fr.cnes.sirius.patrius.frames.CelestialBodyFrame;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;

/**
 * Extended interface for celestial bodies shapes : extends the OREKIT's BodyShape interface by adding geometric
 * methods.
 *
 * @author Thomas Trapier
 *
 * @version $Id: GeometricBodyShape.java 18080 2017-10-02 16:53:17Z bignon $
 *
 * @since 1.2
 */
public interface BodyShape extends PVCoordinatesProvider {

    /**
     * Default value of distance epsilon below which the height coordinate is neglected: below this value,the method
     * {@link #getIntersectionPoint(Line, Vector3D, Frame, AbsoluteDate)} will be automatically used instead of
     * {@link #getIntersectionPoint(Line, Vector3D, Frame, AbsoluteDate, double)}. This distance epsilon is also used to
     * assess if a body point is on the shape surface or not (method {@link BodyPoint#isOnShapeSurface()}.
     */
    public static final double DEFAULT_DISTANCE_EPSILON = 1e-8;

    /**
     * Factor to be multiplied to the direction in order to improve the accuracy of the line creation by increasing the
     * distance between the origin and the second point.
     */
    public static final double DIRECTION_FACTOR = 1e14;

    /** Margin type. */
    public enum MarginType {
        /** Distance margin type. */
        DISTANCE,

        /** Scale factor margin type. */
        SCALE_FACTOR
    }

    /**
     * Getter for the name of the shape.
     * 
     * @return the name of the shape
     */
    public String getName();

    /**
     * Getter for the body frame related to body shape.
     *
     * @return the body frame related to body shape
     */
    public CelestialBodyFrame getBodyFrame();

    /**
     * Getter for the intersection point of a line with the surface of the body.
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
     * @return intersection point at altitude zero or null if the line does not intersect the surface
     * @throws PatriusException
     *         if line cannot be converted to body frame
     */
    public BodyPoint getIntersectionPoint(Line line, Vector3D close, Frame frame, AbsoluteDate date)
        throws PatriusException;

    /**
     * Getwter for the intersection point of a line with the surface of the body.
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
     * @param name
     *        name of the point
     * @return intersection point at altitude zero or null if the line does not intersect the surface
     * @throws PatriusException
     *         if line cannot be converted to body frame
     */
    public BodyPoint getIntersectionPoint(Line line, Vector3D close, Frame frame, AbsoluteDate date, String name)
        throws PatriusException;

    /**
     * Getter for the intersection point of a line with the surface of the body for a given altitude.
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
     * @throws PatriusException
     *         if line cannot be converted to body frame
     */
    public BodyPoint getIntersectionPoint(Line line, Vector3D close, Frame frame, AbsoluteDate date, double altitude)
        throws PatriusException;

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
     * @throws PatriusException
     *         if line cannot be converted to body frame
     */
    public BodyPoint[] getIntersectionPoints(Line line, Frame frame, AbsoluteDate date) throws PatriusException;

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
     * @throws PatriusException
     *         if line cannot be converted to body frame
     */
    public double distanceTo(Line line, Frame frame, AbsoluteDate date) throws PatriusException;

    /**
     * Compute the apparent radius (in meters) of the occulting body from the spacecraft (observer) position. Given a
     * plane containing the spacecraft (observer) position, the center of the occulting body and the center of the
     * occulted body, and given a line contained within this plane, passing by the spacecraft (observer) position and
     * tangent to the mesh of the occulting body, the apparent radius corresponds to the length of the line starting
     * from the center of the occulting body, perpendicular to the first given line and ending at the intersection of
     * the two lines.
     *
     * @param pvObserver
     *        the spacecraft (observer) position-velocity
     * @param date
     *        the date at which the signal is received by the observer (reception date)
     * @param occultedBody
     *        the body which is occulted to the spacecraft (observer) by the occulting body
     * @param propagationDelayType
     *        propagation delay type
     * @return the apparent radius (in meters) of the occulting body from the spacecraft (observer) position
     * @throws PatriusException
     *         if the {@link PVCoordinatesProvider} computation fails
     */
    public double getApparentRadius(PVCoordinatesProvider pvObserver, AbsoluteDate date,
                                    PVCoordinatesProvider occultedBody, PropagationDelayType propagationDelayType)
        throws PatriusException;

    /**
     * Resize the geometric body shape by a margin.
     *
     * @param marginType
     *        margin type to be used
     * @param marginValue
     *        margin value to be used (in meters if the margin type is DISTANCE)
     * @return resized geometric body shape with the margin
     * @throws PatriusException
     *         if the margin value is invalid
     */
    public BodyShape resize(MarginType marginType, double marginValue) throws PatriusException;

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
     * @return an array of length 2 containing the point of the line (slot [0]) and the point of the shape (slot [1])
     *         expressed as {@link BodyPoint} (depending on body shape)
     * @throws PatriusException
     *         if line cannot be converted to body frame
     */
    public BodyPoint[] closestPointTo(final Line line, final Frame frame, final AbsoluteDate date)
        throws PatriusException;

    /**
     * This method computes the two points, on the line and on the body, that are the closest to each other. The
     * returned points are identical if the line intersects the shape: this point is the one with the lowest abscissa.
     * The returned point with index 0 is the point on the Line, while the returned point with index 1 is the point on
     * this.
     * <p>
     * Note: calculations take the line's minimum abscissa into account.
     * </p>
     * <p>
     * In this method we consider that the line's frame is the body frame, and the date is the
     * {@link AbsoluteDate#J2000_EPOCH}.
     * </p>
     *
     * @param line
     *        the original line for the shortest distance computation
     * @return an array of length 2 containing the point of the line (slot [0]) and the point of the shape (slot [1])
     *         expressed as {@link BodyPoint} (depending on body shape)
     */
    public BodyPoint[] closestPointTo(final Line line);

    /**
     * Computes the point on body surface that is the closest to provided point.
     *
     * @param point
     *        a point expressed in provided frame
     * @param frame
     *        frame
     * @param date
     *        date
     * @return the closest point to the provided point on the body surface
     * @throws PatriusException
     *         if computation failed
     */
    public BodyPoint closestPointTo(final Vector3D point, final Frame frame, final AbsoluteDate date)
        throws PatriusException;

    /**
     * Computes the point on body surface that is the closest to provided point.
     * 
     * @param point
     *        a point expressed in body frame
     * @return the closest point to the provided point on the body surface
     */
    public BodyPoint closestPointTo(final Vector3D point);

    /**
     * Computes the point on body surface that is the closest to provided point.
     *
     * @param point
     *        a point expressed in body frame
     * @param name
     *        name of the point
     * @return the closest point to the provided point on the body surface
     */
    public BodyPoint closestPointTo(final Vector3D point, final String name);

    /**
     * Build a {@link BodyPoint} from LLH coordinates. Type of returned body point depends on body.
     *
     * @param coordSystem
     *        LLH coordinates system in which are expressed the entered lat/long/height
     * @param latitude
     *        input latitude
     * @param longitude
     *        input longitude
     * @param height
     *        input height
     * @param name
     *        name of the point
     * @return a body point
     */
    public BodyPoint buildPoint(LLHCoordinatesSystem coordSystem, double latitude, double longitude, double height,
                                String name);

    /**
     * Build a {@link BodyPoint} from position in body frame. Type of returned body point depends on body.
     *
     * @param position
     *        position in body frame
     * @param name
     *        name of the point
     * @return {@link BodyPoint} from position in body frame
     */
    public BodyPoint buildPoint(Vector3D position, String name);

    /**
     * Build a {@link BodyPoint} from position in provided frame at provided date. Type of returned body point depends
     * on body.
     *
     * @param position
     *        position in provided frame at provided date
     * @param frame
     *        frame
     * @param date
     *        date
     * @param name
     *        name of the point
     * @return {@link BodyPoint} from position in provided frame at provided date
     * @throws PatriusException
     *         if failed to build point
     */
    public BodyPoint buildPoint(Vector3D position, Frame frame, AbsoluteDate date, String name)
        throws PatriusException;

    /**
     * Build a body point on the radial direction corresponding to entered bodycentric latitude and longitude: if more
     * than one intersection, the method considers the one farthest to the body frame origin (having the largest norm).
     *
     * @param bodycentricLatitude
     *        bodycentric latitude
     * @param bodycentricLongitude
     *        bodycentric longitude
     * @return the radial point at the surface of the body shape
     */
    public default BodyPoint buildRadialPointOnShapeSurface(final double bodycentricLatitude,
                                                            final double bodycentricLongitude) {

        // longitude, cosine and sine
        final double[] sincosLon = MathLib.sinAndCos(bodycentricLongitude);
        final double sinLon = sincosLon[0];
        final double cosLon = sincosLon[1];

        // latitude, cosine and sine
        final double[] sincosLat = MathLib.sinAndCos(bodycentricLatitude);
        final double sinLat = sincosLat[0];
        final double cosLat = sincosLat[1];

        final Vector3D direction = new Vector3D(cosLat * cosLon, cosLat * sinLon, sinLat)
            .scalarMultiply(DIRECTION_FACTOR);
        final Line line = new Line(Vector3D.ZERO, direction, Vector3D.ZERO);

        // date not used
        try {
            return getIntersectionPoint(line, direction, getBodyFrame(), null, BodyPointName.RADIAL_ON_SHAPE);
        } catch (final PatriusException e) {
            // cannot happen as no frames transformation is performed
            throw new PatriusExceptionWrapper(e);
        }
    }

    /**
     * Return the distance epsilon below which the height coordinate is neglected. This epsilon value can be modified
     * using dedicated setter.
     * <ul>
     * <li>Below this distance epsilon, the method {@link #getIntersectionPoint(Line, Vector3D, Frame, AbsoluteDate)}
     * will be automatically used instead of {@link #getIntersectionPoint(Line, Vector3D, Frame, AbsoluteDate, double)},
     * </li>
     * <li>The method {@link BodyPoint#isOnShapeSurface()} will return true if the absolute value of its normal height
     * is lower than this distance epsilon.</li>
     * </ul>
     *
     * @return the altitude epsilon
     */
    public double getDistanceEpsilon();

    /**
     * Setter for the distance epsilon below which the height coordinate is neglected.
     *
     * @param epsilon
     *        distance epsilon to be set
     */
    public void setDistanceEpsilon(double epsilon);

    /**
     * Getter for the LLH coordinates system used by the computed {@link BodyPoint}.
     *
     * @return the coordinates system
     */
    public LLHCoordinatesSystem getLLHCoordinatesSystem();

    /**
     * Setter for the LLH coordinates system to be used by the computed {@link BodyPoint}.
     *
     * @param coordSystem
     *        LLH coordinates system to be set
     */
    public void setLLHCoordinatesSystem(LLHCoordinatesSystem coordSystem);

    /**
     * Indicate if the current LLH coordinates system set for the body is the default one or not.
     * 
     * @return {@code true} if the current LLH coordinates system is the default one, {@code false} otherwise
     */
    public boolean isDefaultLLHCoordinatesSystem();

    /**
     * Getter for the radius, in meters, of a sphere centered on the body frame origin and encompassing the shape.
     *
     * @return the encompassing radius
     */
    public double getEncompassingSphereRadius();

    /**
     * Getter for the epsilon for signal propagation used in
     * {@link #getApparentRadius(PVCoordinatesProvider, AbsoluteDate, PVCoordinatesProvider, PropagationDelayType)}
     * method. This epsilon (in s) directly reflect the accuracy of signal propagation (1s of accuracy = 3E8m of
     * accuracy on distance between emitter and receiver).
     *
     * @return the epsilon for signal propagation
     */
    public double getEpsilonSignalPropagation();

    /**
     * Setter for the epsilon for signal propagation used in
     * {@link #getApparentRadius(PVCoordinatesProvider, AbsoluteDate, PVCoordinatesProvider, PropagationDelayType)}
     * method. This epsilon (in s) directly reflect the accuracy of signal propagation (1s of accuracy = 3E8m of
     * accuracy on distance between emitter and receiver).
     *
     * @param epsilon
     *        epsilon for signal propagation
     */
    public void setEpsilonSignalPropagation(final double epsilon);
}
