/**
 * Copyright 2011-2022 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2102:15/05/2019:[Patrius] Refactoring du paquet fr.cnes.sirius.patrius.bodies
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:525:22/04/2016: add new functionalities existing in LibKernel
 * VERSION::FA:648:27/07/2016:Corrected minor points staying from V3.2
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.projections;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import fr.cnes.sirius.patrius.bodies.EllipsoidBodyShape;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.math.geometry.euclidean.twod.Vector2D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Abstract class for projections.
 * This class is the generic class to handle implemented projections methods.
 * This class was retrieved from the LibKernel and every calculation formula are extracted from the "Map
 * Projection, A working manual" written by John P. Snyder.
 * 
 * @concurrency not thread-safe
 * @author Galpin Thomas
 * @since 3.2
 * @version $Id$
 */
public abstract class AbstractProjection implements IProjection {

    /** Serializable UID. */
    private static final long serialVersionUID = -5639443987042063885L;

    /** Reference shape. */
    private final EllipsoidBodyShape referenceShape;

    /** Pivot point used for projections define by its longitude and latitude. */
    private final GeodeticPoint pivot;

    /**
     * Constructor.
     * 
     * @param pivotIn
     *        pivot point used for projection.
     * @param shape
     *        reference shape used for projection.
     */
    public AbstractProjection(final GeodeticPoint pivotIn, final EllipsoidBodyShape shape) {
        this.referenceShape = shape;
        this.pivot = pivotIn;
    }

    /** {@inheritDoc} */
    @Override
    public final EllipsoidBodyShape getReference() {
        return this.referenceShape;
    }

    /**
     * Get the projection pivot point.
     * 
     * @return the projection pivot point
     */
    public final GeodeticPoint getPivotPoint() {
        return this.pivot;
    }

    /**
     * Returns the scale factor at a specific latitude.
     * The result is the fraction Mercator distance / real distance.
     * 
     * @param latitude
     *        latitude
     * @return the distortion factor
     */
    public abstract double getDistortionFactor(final double latitude);

    /**
     * Generate additional vertices between two points. Can be use after a projection.
     * In this case, p1 and p2 are projected points.
     * <p>
     * 
     * @param p1
     *        Projected point 1
     * @param p2
     *        Projected point 2
     * @param maxLenght
     *        distance consistent with the projection scale.
     *        For a projection, this distance is correct only around pivot point
     * @param p2Included
     *        p2 is included only if true
     * @return A list of discretized Vector2D. P1 is always included. p2 is included as last point, only
     *         if p2Included is true.
     *         For a projection, the distance will be constant between 2 points, but can be variable
     *         after the inverse projection. The distance is exact only near the pivot point of the projection
     */
    public final List<Vector2D> discretize(final Vector2D p1, final Vector2D p2,
                                           final double maxLenght, final boolean p2Included) {
        final double x1 = p1.getX();
        final double y1 = p1.getY();
        final double x2 = p2.getX();
        final double y2 = p2.getY();
        final double dx = x2 - x1;
        final double dy = y2 - y1;
        final double fullLength = MathLib.sqrt(dx * dx + dy * dy);

        // subtract 1 meter on full length to avoid adding useless points for inaccuracy reason
        final int nbPointsToAdd = (int) MathLib.divide((fullLength - 1.), MathLib.abs(maxLenght));

        // Initialization :
        final List<Vector2D> result = new ArrayList<>();

        // put the first original point
        result.add(p1);
        if (nbPointsToAdd > 0) {

            // nbPointsToAdd+1 intervals for nbPointsToAdd
            final double inc = 1d / (nbPointsToAdd + 1);
            double t = inc;
            // add all the vertices in x,y order
            for (int i = 1; i <= nbPointsToAdd; i++, t += inc) {
                result.add(new Vector2D((x1 + dx * t), (y1 + (dy * t))));
            }
        }

        // put the second original point
        if (p2Included) {
            result.add(p2);
        }
        return result;
    }

    /**
     * Discretizes a polygon conforming to a line property directive, and a maximum length of discretization.
     * If this length <= 0, no discretization will be done, only projection.
     * If the line property of the given projection is not conform to the line property directive,
     * the intermediate points will be computed by an other way, before being projected.
     * 
     * @param list
     *        of points defining the polygon
     * @param ltype
     *        line type
     * @param maxLength
     *        This distance is used when a discretization occurs, i.e : when the line property is not
     *        coherent with the projection ({@link EnumLineProperty}). This parameter represent a <b>distance</b>
     *        expressed in
     *        meters. This will be the maximal distance between two points of the projected polygon. If you set the
     *        parameter to a value <=0, no discretization will be done.<br>
     * @return List of projected points
     * @throws PatriusException
     *         if complex polygon discretization fails
     */
    public final List<Vector2D> discretizeAndApplyTo(final List<GeodeticPoint> list, final EnumLineProperty ltype,
                                                     final double maxLength) throws PatriusException {
        // initialize output array list
        List<Vector2D> result = new ArrayList<>();
        switch (ltype) {
            case STRAIGHT_RHUMB_LINE:
                result = this.discretizeRhumbAndApplyTo(list, maxLength);
                break;
            case GREAT_CIRCLE:
                result = this.discretizeCircleAndApplyTo(list, maxLength);
                break;
            case STRAIGHT:
                // Should never happens
                result = new ArrayList<>(0);
                break;
            default:
                // raise an exception for an unknown type
                throw PatriusException.createIllegalArgumentException(PatriusMessages.PDB_UNSUPPORTED_PARAMETER_1_2_3,
                    ltype.toString(), EnumLineProperty.GREAT_CIRCLE.toString(), EnumLineProperty.STRAIGHT.toString(),
                    EnumLineProperty.STRAIGHT_RHUMB_LINE.toString());
        }
        return result;
    }

    /**
     * Discretize following great circle lines between vertices of polygon and project obtained points.
     * 
     * @param list
     *        geodetic points list
     * @param maxLength
     *        This distance is used when a discretization occurs : i.e : when the line property is not
     *        coherent with the projection ( {@link EnumLineProperty}). This parameter represent a <b>distance</b>
     *        expressed in
     *        meters. This will be the maximal distance between two points of the projected polygon. If you set the
     *        parameter to a value <=0 no discretization will be done.<br>
     * @see EnumLineProperty#GREAT_CIRCLE
     * @see EnumLineProperty#STRAIGHT
     * @return List of Vector2D (latitude/longitude).
     * @throws PatriusException
     *         thrown if one projection could not be computed
     */
    public final List<Vector2D> discretizeCircleAndApplyTo(final List<GeodeticPoint> list,
                                                           final double maxLength) throws PatriusException {

        // Intermediate variables :
        GeodeticPoint from = list.get(0);
        GeodeticPoint to = null;
        // Results :
        final List<GeodeticPoint> result;

        // calculate extra vertices between all the original segments.
        if (maxLength > 0) {
            result = new ArrayList<>();
            for (int i = 1; i < list.size(); i++) {
                to = list.get(i);
                // Compute intermediate points between 'from' and 'to' :
                final List<GeodeticPoint> xyIntermediateP = ProjectionEllipsoidUtils.discretizeGreatCircle(from, to,
                    maxLength, this.referenceShape);
                // add transformed data :
                result.addAll(xyIntermediateP);
                // Set 'from' at 'to'
                from = to;
            }
        } else {
            // no discretization
            result = list;
        }
        // now project the result
        return this.applyTo(result);
    }

    /**
     * Project a rhumb line polygon, with the given projection.
     * <p>
     * If the projection type of line is rhumb, points will be directly projected and then discretized.
     * </p>
     * <p>
     * Else the discretization will be done by using {@link ProjectionEllipsoidUtils} methods to compute points along a
     * loxodrom, and projection will be done afterwards.
     * </p>
     * 
     * @param list
     *        list of geodetic vectors
     * @param maxLength
     *        This distance is used when a discretization occurs :
     *        i.e when the line property is not coherent with
     *        the projection ( {@link EnumLineProperty}). This parameter represent a <b>distance</b> expressed in
     *        meters. This will be the maximal distance between two points of the projected polygon. If you set the
     *        parameter to a value <=0 , no dicretization will be done.<br>
     * @return List of Vector2D as projected polygon (lat/lon).
     * @throws PatriusException
     *         thrown if complex polygon discretization fails
     */
    public final List<Vector2D> discretizeRhumbAndApplyTo(final List<GeodeticPoint> list,
                                                          final double maxLength) throws PatriusException {

        // Results :
        final List<Vector2D> result;

        // indicates if the projected points between two vertices form a straight or a curved line.
        // true also if no discretization
        final boolean straightLine =
            (this.getLineProperty() == EnumLineProperty.STRAIGHT_RHUMB_LINE || maxLength <= 0.);

        if (straightLine) {
            // project, then discretize
            result = this.applyToAndDiscretize(list, maxLength);
        } else {
            // discretization first, then projection
            // Need to compute intermediate points before projection
            result = this.discretizeRhumbFirstAndApplyTo(list, maxLength);
        }
        return result;
    }

    /**
     * Project two points, then discretize 2D the line.
     * A discontinuity is considered if the difference between the two X projected values is > at
     * half of the X projection world at the end of the projection world.
     * 
     * @param from
     *        the initial geodetic point
     * @param to
     *        the final geodetic point
     * @param maxLength
     *        maximal length for a leg, after discretization. If maxlength <=0, no discretization is done.
     * @param lastIncluded
     *        the last point (to) is included if true
     * @return the list of projected points
     * @throws PatriusException
     *         thrown if projection of start or end point could not be computed
     */
    public final List<Vector2D>
        applyToAndDiscretize(final GeodeticPoint from, final GeodeticPoint to,
                             final double maxLength, final boolean lastIncluded) throws PatriusException {

        // Get transformation of initial and final points
        final Vector2D p1 = this.applyTo(from);
        final Vector2D p2 = this.applyTo(to);

        // Initialize result
        final List<Vector2D> res;

        if (maxLength <= 0.) {
            // no discretization : return one or both points
            res = new ArrayList<>(2);
            res.add(p1);
            if (lastIncluded) {
                res.add(p2);
            }
            return res;
        }

        // Compute intermediate points between 'from' and 'to' :
        return this.discretize(p1, p2, maxLength, lastIncluded);
    }

    /**
     * Project a list of GeodeticPoints with a given projection. A check and correction
     * are done on each point if latitude > the maximum value of the projection.
     * 
     * @param list
     *        list of geodetic points
     * @return list of projected points
     * @throws PatriusException
     *         thrown if one projection could not be computed
     */
    public final List<Vector2D> applyTo(final List<GeodeticPoint> list) throws PatriusException {
        final List<Vector2D> result = new ArrayList<>();
        final Iterator<GeodeticPoint> iter = list.iterator();
        while (iter.hasNext()) {
            final GeodeticPoint currentGeodetic = iter.next();
            final Vector2D projected = this.applyTo(currentGeodetic);
            result.add(projected);
        }
        return result;
    }

    /**
     * Inverse Projects a list of Vector2D (projected points) with a given projection.
     * 
     * @param list
     *        list of projected points
     * @return list of geodetic points
     * @throws PatriusException
     *         thrown if one inverse projection could not be computed
     */
    public final List<GeodeticPoint> applyInverseTo(final List<Vector2D> list) throws PatriusException {
        final List<GeodeticPoint> result = new ArrayList<>();
        final Iterator<Vector2D> iter = list.iterator();
        while (iter.hasNext()) {
            final Vector2D projected = iter.next();
            final GeodeticPoint geodetic = this.applyInverseTo(projected.getX(), projected.getY());
            result.add(geodetic);
        }
        return result;
    }

    /**
     * Inversion transformation of arrays of x and y projected coordinates.
     * 
     * @param x
     *        x coordinates of projected points
     * @param y
     *        y coordinates of projected points
     * @return list of geodetic points obtained by inverse projection
     * @throws PatriusException
     *         thrown if arrays have not the same length
     */
    public final List<GeodeticPoint> applyInverseTo(final double[] x, final double[] y) throws PatriusException {
        // Check data length :
        if (x.length != y.length) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.PDB_BAD_LENGTH);
        }
        final List<GeodeticPoint> result = new ArrayList<>();
        // loop on each (x,y) point
        for (int i = 0; i < x.length; i++) {
            result.add(this.applyInverseTo(x[i], y[i]));
        }
        return result;
    }

    /**
     * Project a list of geodetic points and then apply the discretization.
     * 
     * @param list
     *        of geodetic points
     * @param maxLength
     *        This distance is used when a discretization occurs : i.e : when the line property is not
     *        coherent with the projection ( {@link EnumLineProperty}). This parameter represent a <b>distance</b>
     *        expressed in
     *        meters. This will be the maximal distance between two points of the projected polygon. If you set the
     *        parameter to a value <=0 no discretization will be done.<br>
     * @return List of projected discretized vectors
     * @throws PatriusException
     *         thrown if one projection could not be computed
     */
    private List<Vector2D> applyToAndDiscretize(final List<GeodeticPoint> list,
                                                final double maxLength) throws PatriusException {
        // Result
        final List<Vector2D> res = new ArrayList<>();
        Vector2D from = this.applyTo(list.get(0));
        Vector2D to = null;
        for (int i = 1; i < list.size(); i++) {
            to = this.applyTo(list.get(i));
            if (maxLength > 0.) {
                // discretize the segment (without last point) and add the result in the list
                final List<Vector2D> resSegment = this.discretize(from, to, maxLength, false);
                // add all the Vector2D found
                res.addAll(resSegment);
            } else {
                // add directly the point
                res.add(from);
            }
            from = to;
        }

        // close the polygon is necessary
        to = this.applyTo(list.get(0));
        if (!to.equals(from)) {
            if (maxLength > 0.) {
                final List<Vector2D> resSegment = this.discretize(from, to, maxLength, false);
                // add all the Vector2D found
                res.addAll(resSegment);
            } else {
                res.add(from);
            }
        }
        return res;
    }

    /**
     * Discretize a list of geodetic points following rhumb line, and then perform the projection.
     * 
     * @param list
     *        of geodetic points
     * @param maxLength
     *        This distance is used when a discretization occurs : i.e : when the line property is not
     *        coherent with the projection ( {@link EnumLineProperty}). This parameter represent a <b>distance</b>
     *        expressed in
     *        meters. This will be the maximal distance between two points of the projected polygon. If you set the
     *        parameter to a value <=0 no discretization will be done.<br>
     * @return List of discretized projected vectors
     * @throws PatriusException
     *         thrown thrown if one projection could not be computed
     */
    private List<Vector2D> discretizeRhumbFirstAndApplyTo(final List<GeodeticPoint> list,
                                                          final double maxLength) throws PatriusException {
        // Result
        final List<Vector2D> res = new ArrayList<>();
        // discretization first, then projection
        // Need to compute intermediate points before projection
        GeodeticPoint from = list.get(0);
        GeodeticPoint to = null;
        for (int i = 1; i < list.size(); i++) {
            to = list.get(i);
            // discretize GeodeticPoints
            if (maxLength > 0) {
                final List<GeodeticPoint> resDiscretize = ProjectionEllipsoidUtils
                    .discretizeRhumbLine(from, to, maxLength, this.referenceShape);
                // project
                final List<Vector2D> resSegment = this.applyTo(resDiscretize);
                res.addAll(resSegment);
            } else {
                // no discretization. The point is just projected and added
                res.add(this.applyTo(from));
            }
            from = to;
        }

        // close the polygon is necessary
        to = list.get(0);
        if (!to.equals(from)) {
            if (maxLength > 0) {
                final List<GeodeticPoint> resDiscretize = ProjectionEllipsoidUtils.discretizeRhumbLine(from, to,
                    maxLength, this.referenceShape);
                // project
                final List<Vector2D> resSegment = this.applyTo(resDiscretize);
                res.addAll(resSegment);
            } else {
                res.add(this.applyTo(from));
            }
        }
        return res;
    }
}
