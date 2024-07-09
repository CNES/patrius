/**
 * Copyright 2002-2012 CS Systèmes d'Information
 * Copyright 2011-2017 CNES
 *
 * HISTORY
* VERSION:4.8:DM:DM-2958:15/11/2021:[PATRIUS] calcul d'intersection a altitude non nulle pour l'interface BodyShape 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 */
/*
 * 
 */
package fr.cnes.sirius.patrius.bodies;

import java.io.Serializable;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Interface representing the rigid surface shape of a natural body.
 * <p>
 * The shape is not provided as a single complete geometric model, but single points can be queried (
 * {@link #getIntersectionPoint}).
 * </p>
 * 
 * @author Luc Maisonobe
 */
public interface BodyShape extends Serializable {

    /** Epsilon altitude below which {@link #getIntersectionPoint(Line, Vector3D, Frame, AbsoluteDate)} will be 
     * automatically used instead of {@link #getIntersectionPoint(Line, Vector3D, Frame, AbsoluteDate, double)}. */
    static final double EPS_ALTITUDE = Precision.DOUBLE_COMPARISON_EPSILON;
    
    /**
     * Get body frame related to body shape.
     * 
     * @return body frame related to body shape
     */
    Frame getBodyFrame();

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
     *        point used for intersections selection
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
     *        point used for intersections selection
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
}
