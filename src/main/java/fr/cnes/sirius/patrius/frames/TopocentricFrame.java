/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */
/*
 *
 * HISTORY
* VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
* VERSION:4.7:FA:FA-2883:18/05/2021:Reliquats sur la DM 2871 sur le changement du sens des Azimuts 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:524:25/05/2016:serialization java doc
 * VERSION::FA:829:25/01/2017:Protection of trigonometric methods
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames;

import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.solver.BracketingNthOrderBrentSolver;
import fr.cnes.sirius.patrius.math.analysis.solver.UnivariateSolver;
import fr.cnes.sirius.patrius.math.exception.TooManyEvaluationsException;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.AzimuthElevationCalculator;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.CardanMountPV;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.CardanMountPosition;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.TopocentricPV;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.TopocentricPosition;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Topocentric frame.
 * <p>
 * Frame associated to a position at the surface of a body shape.
 * </p>
 * 
 * @serial TopocentricFrame is serializable given a serializable TransformProvider (see {@link Frame})
 * @author V&eacute;ronique Pommier-Maurussane
 */
public final class TopocentricFrame extends Frame implements PVCoordinatesProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = -5997915708080966466L;

    /** Body shape on which the local point is defined. */
    private final BodyShape parentShape;

    /** Point where the topocentric frame is defined. */
    private final GeodeticPoint point;

    /** Point Azimuth, Elevation Calculator */
    private final AzimuthElevationCalculator azimuthElevationCalculator;


    /**
     * This constructor builds an East oriented topocentric frame. Its axis are (x = East, y = North, z = Zenith).
     * 
     * @param bodyParentShape
     *        body shape on which the local point is defined
     * @param geodeticPoint
     *        local surface point where topocentric frame is defined
     * @param name
     *        the string representation
     */
    public TopocentricFrame(final BodyShape bodyParentShape, 
                            final GeodeticPoint geodeticPoint,
                            final String name) {
        
        this(bodyParentShape, geodeticPoint, -MathUtils.HALF_PI, name);
    }

    /**
     * This constructor builds a topocentric frame which Z-axis is the zenith.<br>
     * The angle in radians trigowise between the North direction (= "Reference azymuth") and the x-axis of the
     * Topocentric Frame
     * is frameOrientation<br>
     * <br>
     * 
     * @param bodyParentShape
     *        body shape on which the local point is defined
     * @param geodeticPoint
     *        local surface point where topocentric frame is defined
     * @param frameOrientation
     *        The angle in radians trigowise between the North direction (= "Reference azymuth") and the x-axis of the
     *        Topocentric Frame<br>
     *        <br>
     *        If frameOrientation is 0, then the frame's unit vectors are (x = North, y = West, z = Zenith).<br>
     *        If frameOrientation is -0.785 (=> -45°) means that the x axis of the Frame points to (x = North-East, y =
     *        North-West, z = Zenith)<br>
     *        <br>
     * @param name
     *        the string representation
     */
    public TopocentricFrame(final BodyShape bodyParentShape,
            final GeodeticPoint geodeticPoint,
            final double frameOrientation,
            final String name) {

        super(bodyParentShape.getBodyFrame(), createTransform(bodyParentShape, geodeticPoint, frameOrientation), name,
            false);
        this.parentShape = bodyParentShape;
        this.point = geodeticPoint;
        this.azimuthElevationCalculator = new AzimuthElevationCalculator(frameOrientation);
    }

    /**
     * Compute the transformation from body centered frame to topocentric frame.
     * 
     * @param bodyParentShape
     *        body shape on which the local point is defined
     * @param geodeticPoint
     *        local surface point where topocentric frame is defined
     * @param frameOrientation
     *        the angle in radians between the North direction and the X-axis
     * @return the transformation from body centered frame to topocentric frame
     */
    private static Transform createTransform(final BodyShape bodyParentShape, final GeodeticPoint geodeticPoint,
                                             final double frameOrientation) {
        // 1. Translation from body center to geodetic point
        final Transform translation = new Transform(AbsoluteDate.J2000_EPOCH, bodyParentShape.transform(geodeticPoint));

        // 2. Rotate axes
        // first build the rotation from the parent shape's frame to the North topocentric frame
        final Vector3D xtopo = geodeticPoint.getNorth();
        final Vector3D ztopo = geodeticPoint.getZenith();
        final Rotation firstRotation = new Rotation(Vector3D.PLUS_I, Vector3D.PLUS_K, xtopo, ztopo);

        // then build the rotation from the North topocentric frame to the expected one.
        final Rotation secondRotation = new Rotation(Vector3D.PLUS_K, frameOrientation);

        // finally build the transform that concatenates the two rotations
        final Transform rotation = new Transform(AbsoluteDate.J2000_EPOCH, firstRotation.applyTo(secondRotation),
            Vector3D.ZERO);

        // Compose both transformations
        return new Transform(AbsoluteDate.J2000_EPOCH, translation, rotation);
    }

    /**
     * @return the oriented angle between the local north and the x axis (trigowise).
     */
    public double getOrientation() {
        return getAzimuthElevationCalculator().getFrameOrientation();
    }

    /**
     * Get the body shape on which the local point is defined.
     * 
     * @return body shape on which the local point is defined
     */
    public BodyShape getParentShape() {
        return this.parentShape;
    }

    /**
     * Get the surface point defining the origin of the frame.
     * 
     * @return surface point defining the origin of the frame
     */
    public GeodeticPoint getPoint() {
        return this.point;
    }

    /**
     * Get the zenith direction of topocentric frame, expressed in parent shape frame.
     * <p>
     * The zenith direction is defined as the normal to local horizontal plane.
     * </p>
     * 
     * @return unit vector in the zenith direction
     * @see #getNadir()
     */
    public Vector3D getZenith() {
        return this.point.getZenith();
    }

    /**
     * Get the nadir direction of topocentric frame, expressed in parent shape frame.
     * <p>
     * The nadir direction is the opposite of zenith direction.
     * </p>
     * 
     * @return unit vector in the nadir direction
     * @see #getZenith()
     */
    public Vector3D getNadir() {
        return this.point.getNadir();
    }

    /**
     * Get the north direction of topocentric frame, expressed in parent shape frame.
     * <p>
     * The north direction is defined in the horizontal plane (normal to zenith direction) and following the local
     * meridian.
     * </p>
     * 
     * @return unit vector in the north direction
     * @see #getSouth()
     */
    public Vector3D getNorth() {
        return this.point.getNorth();
    }

    /**
     * Get the south direction of topocentric frame, expressed in parent shape frame.
     * <p>
     * The south direction is the opposite of north direction.
     * </p>
     * 
     * @return unit vector in the south direction
     * @see #getNorth()
     */
    public Vector3D getSouth() {
        return this.point.getSouth();
    }

    /**
     * Get the east direction of topocentric frame, expressed in parent shape frame.
     * <p>
     * The east direction is defined in the horizontal plane in order to complete direct triangle (east, north, zenith).
     * </p>
     * 
     * @return unit vector in the east direction
     * @see #getWest()
     */
    public Vector3D getEast() {
        return this.point.getEast();
    }

    /**
     * Get the west direction of topocentric frame, expressed in parent shape frame.
     * <p>
     * The west direction is the opposite of east direction.
     * </p>
     * 
     * @return unit vector in the west direction
     * @see #getEast()
     */
    public Vector3D getWest() {
        return this.point.getWest();
    }

    /**
     * Get the elevation of a point with regards to the local point.
     * <p>
     * The elevation is the angle between the local horizontal and the direction from local point to given point.
     * </p>
     * 
     * @param extPoint
     *        point for which elevation shall be computed
     * @param frame
     *        frame in which the point is defined
     * @param date
     *        computation date
     * @return elevation of the point
     * @exception PatriusException
     *            if frames transformations cannot be computed
     */
    public double getElevation(final Vector3D extPoint, final Frame frame,
                               final AbsoluteDate date) throws PatriusException {

        // Transform given point from given frame to topocentric frame
        final Transform t = frame.getTransformTo(this, date);
        final Vector3D extPointTopo = t.transformPosition(extPoint);

        // Elevation angle is PI/2 - angle between zenith and given point direction
        return getAzimuthElevationCalculator().getElevation(extPointTopo);
    }

    /**
     * Get the elevation derivative of a point wrt the local point (dElevation) express in the
     * specified frame.
     *
     * @param extPoint
     *        point for which elevation shall be computed
     * @param frame
     *        frame in which the point is defined
     * @param date
     *        computation date
     *
     * @return the elevation derivative of a point wrt the local point
     * @throws PatriusException
     *         if frames transformations cannot be computed
     */
    public Vector3D getDElevation(final Vector3D extPoint, final Frame frame,
            final AbsoluteDate date) throws PatriusException {

        final Vector3D dElevationInOutputFrame;
        if (frame.equals(this)) {
            // No transformation needed
            dElevationInOutputFrame = getAzimuthElevationCalculator().computeDElevation(extPoint);
        } else {
            // Transform the given point (position) from the given frame to the topocentric frame
            final Transform t = frame.getTransformTo(this, date);
            final Vector3D extPointTopo = t.transformPosition(extPoint);

            // Compute the elevation derivative in the topo frame
            final Vector3D dElevationInTopoFrame = getAzimuthElevationCalculator()
                    .computeDElevation(extPointTopo);

            // Transform the derivatives vector back to the given frame
            dElevationInOutputFrame = t.getInverse().transformVector(dElevationInTopoFrame);
        }

        return dElevationInOutputFrame;
    }

    /**
     * Get the elevation rate of a point.
     * 
     * @param extPV
     *        point for which elevation rate shall be computed
     * @param frame
     *        frame in which the point is defined
     * @param date
     *        computation date
     * @return elevation rate of the point
     * @throws PatriusException
     *         if frames transformations cannot be computed
     */
    public double getElevationRate(final PVCoordinates extPV, final Frame frame,
                                   final AbsoluteDate date) throws PatriusException {

        // Transform given point from given frame to topocentric frame
        final Transform t = frame.getTransformTo(this, date);
        final PVCoordinates extPVTopo = t.transformPVCoordinates(extPV);

        // Compute elevation rate
        return getAzimuthElevationCalculator().computeElevationRate(extPVTopo);
    }

    /**
     * Get the azimuth of a point with regards to the topocentric frame center point.
     * <p>
     * The azimuth is the angle between the North direction at local point and the projection in
     * local horizontal plane of the direction from local point to given point. Azimuth angles are
     * counted clockwise, i.e positive towards the East (range [0; 2PI]).
     * </p>
     * 
     * @param extPoint
     *        point for which azimuth shall be computed
     * @param frame
     *        frame in which the point is defined
     * @param date
     *        computation date
     * @return azimuth of the point
     * @exception PatriusException
     *            if frames transformations cannot be computed
     */
    public double getAzimuth(final Vector3D extPoint, final Frame frame,
                             final AbsoluteDate date) throws PatriusException {

        // Transform given point from given frame to topocentric frame
        final Transform t = this.getTransformTo(frame, date).getInverse();
        final Vector3D extPointTopo = t.transformPosition(extPoint);

        // Compute azimuth
        return getAzimuthElevationCalculator().getAzimuth(extPointTopo);
    }

    /**
     * Get the azimuth derivative of a point wrt the local point (dAzimuth) express in the specified
     * frame.
     *
     * @param extPoint
     *        point for which elevation shall be computed
     * @param frame
     *        frame in which the point is defined
     * @param date
     *        computation date
     *
     * @return the azimuth derivative of a point wrt the local point
     * @throws PatriusException
     *         if frames transformations cannot be computed
     */
    public Vector3D
            getDAzimuth(final Vector3D extPoint, final Frame frame, final AbsoluteDate date)
                    throws PatriusException {

        final Vector3D dAzimuthInOutputFrame;
        if (frame.equals(this)) {
            // No transformation needed
            dAzimuthInOutputFrame = getAzimuthElevationCalculator().computeDAzimuth(extPoint);
        } else {
            // Transform the given point (position) from the given frame to the topocentric frame
            final Transform t = frame.getTransformTo(this, date);
            final Vector3D extPointTopo = t.transformPosition(extPoint);

            // Compute the azimuth derivative in the topo frame
            final Vector3D dAzimuthInTopoFrame = getAzimuthElevationCalculator().computeDAzimuth(
                    extPointTopo);

            // Transform the derivatives vector back to the given frame
            dAzimuthInOutputFrame = t.getInverse().transformVector(dAzimuthInTopoFrame);
        }

        return dAzimuthInOutputFrame;
    }

    /**
     * Get the azimuth rate of a point.
     * 
     * @param extPV
     *        point for which the azimuth rate shall be computed
     * @param frame
     *        frame in which the point is defined
     * @param date
     *        computation date
     * @return azimuth rate of the point
     * @throws PatriusException
     *         if frames transformations cannot be computed
     */
    public double getAzimuthRate(final PVCoordinates extPV, final Frame frame,
                                 final AbsoluteDate date) throws PatriusException {

        // Transform given point from given frame to topocentric frame
        final Transform t = frame.getTransformTo(this, date);
        final PVCoordinates extPVTopo = t.transformPVCoordinates(extPV);

        // Compute azimuth rate
        return getAzimuthElevationCalculator().computeAzimuthRate(extPVTopo);
    }

    /**
     * Get the Cardan x angle of a point.
     * <p>
     * The x angle is angle of the rotation of the mounting around local North axis, this angle is between {@code -PI}
     * and {@code PI} and counting clockwise. See Spacefight Dynamics, Part I, chapter 10.4.5.2
     * 
     * @param extPoint
     *        point for which the x angle shall be computed
     * @param frame
     *        frame in which the point is defined
     * @param date
     *        computation date
     * @return x angle of the point.
     * @throws PatriusException
     *         if frames transformations cannot be computed
     */
    public double getXangleCardan(final Vector3D extPoint, final Frame frame,
                                  final AbsoluteDate date) throws PatriusException {

        // Transform given point from given frame to topocentric frame
        final Transform t = frame.getTransformTo(this, date);
        final Transform rotation = new Transform(date, new Rotation(Vector3D.PLUS_K, getOrientation()));
        final Vector3D extPointTopo = new Transform(date, t, rotation).transformPosition(extPoint);

        // Compute x angle
        return this.computeXangle(extPointTopo);
    }

    /**
     * Get the Cardan x angle rate.
     * See Spaceflight Dynamics, Part I, chapter 10.4.5.2
     * 
     * @param extPV
     *        point for which the x angle rate shall be computed
     * @param frame
     *        frame in which the point is defined
     * @param date
     *        computation date
     * @return x angle rate of the point.
     * @throws PatriusException
     *         if frames transformations cannot be computed
     */
    public double getXangleCardanRate(final PVCoordinates extPV, final Frame frame,
                                      final AbsoluteDate date) throws PatriusException {

        // Transform given point from given frame to topocentric frame
        final Transform t = frame.getTransformTo(this, date);
        final Transform rotation = new Transform(date, new Rotation(Vector3D.PLUS_K, getOrientation()));
        final PVCoordinates extPVTopo = new Transform(date, t, rotation).transformPVCoordinates(extPV);

        // Compute x angle rate
        return this.computeXangleRate(extPVTopo);
    }

    /**
     * Get the Cardan y angle of a point with regards to the projection point on the plane defined
     * by the zenith and the west axis.
     * <p>
     * The y angle is angle of the rotation of the mounting around y'. Y' is the image of West axis by the rotation of
     * angle x around North axis, this angle is between {@code -PI/2} and {@code PI/2} and oriented by y'. See
     * Spaceflight Dynamics, Part I, chapter 10.4.5.2
     * 
     * @param extPoint
     *        point for which the y angle shall be computed
     * @param frame
     *        frame in which the point is defined
     * @param date
     *        computation date
     * @return y angle of the point.
     * @throws PatriusException
     *         if frames transformations cannot be computed
     */
    public double getYangleCardan(final Vector3D extPoint, final Frame frame,
                                  final AbsoluteDate date) throws PatriusException {

        // Transform given point from given frame to topocentric frame
        final Transform t = frame.getTransformTo(this, date);
        final Transform rotation = new Transform(date, new Rotation(Vector3D.PLUS_K, getOrientation()));
        final Vector3D extPointTopo = new Transform(date, t, rotation).transformPosition(extPoint);

        // Compute y angle
        return this.computeYangle(extPointTopo);
    }

    /**
     * Get the Cardan y angle rate.
     * See Spaceflight Dynamics, Part I, chapter 10.4.5.2
     * 
     * @param extPV
     *        point for which the y angle rate shall be computed
     * @param frame
     *        frame in which the point is defined
     * @param date
     *        computation date
     * @return y angle rate of the point.
     * @throws PatriusException
     *         if frames transformations cannot be computed
     */
    public double getYangleCardanRate(final PVCoordinates extPV, final Frame frame,
                                      final AbsoluteDate date) throws PatriusException {

        // Transform given point from given frame to topocentric frame
        final Transform t = frame.getTransformTo(this, date);
        final Transform rotation = new Transform(date, new Rotation(Vector3D.PLUS_K, getOrientation()));
        final PVCoordinates extPVTopo = new Transform(date, t, rotation).transformPVCoordinates(extPV);

        // Compute y angle rate
        return this.computeYangleRate(extPVTopo);
    }

    /**
     * Get the range of a point with regards to the topocentric frame center point.
     * 
     * @param extPoint
     *        point for which range shall be computed
     * @param frame
     *        frame in which the point is defined
     * @param date
     *        computation date
     * @return range (distance) of the point
     * @exception PatriusException
     *            if frames transformations cannot be computed
     */
    public double getRange(final Vector3D extPoint, final Frame frame,
                           final AbsoluteDate date) throws PatriusException {

        // Transform given point from given frame to topocentric frame
        final Transform t = frame.getTransformTo(this, date);
        final Vector3D extPointTopo = t.transformPosition(extPoint);

        // Compute range
        return this.computeRange(extPointTopo);
    }

    /**
     * Get the range rate of a point with regards to the topocentric frame center point.
     * 
     * @param extPV
     *        point/velocity for which range rate shall be computed
     * @param frame
     *        frame in which the point is defined
     * @param date
     *        computation date
     * @return range rate of the point (positive if point departs from frame)
     * @exception PatriusException
     *            if frames transformations cannot be computed
     */
    public double getRangeRate(final PVCoordinates extPV, final Frame frame,
                               final AbsoluteDate date) throws PatriusException {

        // Transform given point from given frame to topocentric frame
        final Transform t = frame.getTransformTo(this, date);
        final PVCoordinates extPVTopo = t.transformPVCoordinates(extPV);

        // Compute range rate (doppler) : relative rate along the line of sight
        return this.computeRangeRate(extPVTopo);
    }

    /**
     * Transform a Cartesian position coordinates into topocentric coordinates in this local
     * topocentric frame.
     * 
     * @param extPoint
     *        point in Cartesian coordinates which shall be transformed
     * @param frame
     *        frame in which the point is defined
     * @param date
     *        computation date
     * @return topocentic coordinates of the point
     * @throws PatriusException
     *         OrekitException if frames transformations cannot be computed
     */
    public TopocentricPosition transformFromPositionToTopocentric(final Vector3D extPoint, final Frame frame,
                                                                  final AbsoluteDate date) throws PatriusException {

        // Transform given point from given frame to topocentric frame
        final Transform t = frame.getTransformTo(this, date);
        final Vector3D extPointTopo = t.transformPosition(extPoint);

        // Compute elevation
        final double elevation = getAzimuthElevationCalculator().getElevation(extPointTopo);
        if (MathLib.abs(MathLib.abs(elevation) - MathUtils.HALF_PI) < Precision.EPSILON) {
            throw new PatriusException(PatriusMessages.AZIMUTH_UNDEFINED);
        }
        // Compute azimuth
        final double azimuth = getAzimuthElevationCalculator().getAzimuth(extPointTopo);
        // Compute range
        final double range = this.computeRange(extPointTopo);

        return new TopocentricPosition(elevation, azimuth, range);
    }

    /**
     * Transform a Cartesian position and velocity coordinates into topocentric coordinates in this local
     * topocentric frame.
     * 
     * @param extPV
     *        point in Cartesian coordinates which shall be transformed
     * @param frame
     *        frame in which the point is defined
     * @param date
     *        computation date
     * @return topocentic coordinates of the point
     * @throws PatriusException
     *         OrekitException if frames transformations cannot be computed
     */
    public TopocentricPV transformFromPVToTopocentric(final PVCoordinates extPV, final Frame frame,
                                                      final AbsoluteDate date) throws PatriusException {

        // Transform given point from given frame to topocentric frame
        final Transform t = frame.getTransformTo(this, date);
        final PVCoordinates extPVTopo = t.transformPVCoordinates(extPV);

        final Vector3D position = extPVTopo.getPosition();

        // Compute elevation
        final double elevation = getAzimuthElevationCalculator().getElevation(position);
        if (MathLib.abs(MathLib.abs(elevation) - MathUtils.HALF_PI) < Precision.EPSILON) {
            throw new PatriusException(PatriusMessages.AZIMUTH_UNDEFINED);
        }
        // Compute azimuth
        final double azimuth = getAzimuthElevationCalculator().getAzimuth(position);
        // Compute range
        final double range = this.computeRange(position);

        // Compute elevation rate
        final double elevationRate = getAzimuthElevationCalculator()
                .computeElevationRate(extPVTopo);
        // Compute azimuth rate
        final double azimuthRate = getAzimuthElevationCalculator().computeAzimuthRate(extPVTopo);
        // Compute range rate
        final double rangeRate = this.computeRangeRate(extPVTopo);

        return new TopocentricPV(elevation, azimuth, range, elevationRate, azimuthRate, rangeRate);
    }

    /**
     * Transform a Cartesian position coordinates into Cardan mounting in this local
     * topocentric frame.
     * 
     * @param extPoint
     *        point in Cartesian coordinates which shall be transformed
     * @param frame
     *        frame in which the point is defined
     * @param date
     *        computation date
     * @return Cardan mounting of the point
     * @throws PatriusException
     *         OrekitException if frames transformations cannot be computed
     */
    public CardanMountPosition transformFromPositionToCardan(final Vector3D extPoint, final Frame frame,
                                                             final AbsoluteDate date) throws PatriusException {

        // Transform given point from given frame to topocentric frame
        final Transform t = frame.getTransformTo(this, date);
        final Transform rotation = new Transform(date, new Rotation(Vector3D.PLUS_K, -getOrientation()));
        final Vector3D extPositionTopo = new Transform(date, t, rotation).transformPosition(extPoint);

        // Compute y angle
        final double yAngle = this.computeYangle(extPositionTopo);
        if (MathLib.abs(MathLib.abs(yAngle) - MathUtils.HALF_PI) < Precision.EPSILON) {
            throw new PatriusException(PatriusMessages.CARDAN_MOUNTING_UNDEFINED);
        }
        // Compute x angle
        final double xAngle = this.computeXangle(extPositionTopo);
        // Compute range
        final double range = this.computeRange(extPositionTopo);

        return new CardanMountPosition(xAngle, yAngle, range);
    }

    /**
     * Transform a Cartesian position coordinates into Cardan mounting in this local
     * topocentric frame.
     * 
     * @param extPV
     *        point in Cartesian coordinates which shall be transformed
     * @param frame
     *        frame in which the point is defined
     * @param date
     *        computation date
     * @return Cardan mounting of the point
     * @throws PatriusException
     *         OrekitException if frames transformations cannot be computed
     */
    public CardanMountPV transformFromPVToCardan(final PVCoordinates extPV, final Frame frame,
                                                 final AbsoluteDate date) throws PatriusException {

        // Transform given point from given frame to topocentric frame
        final Transform t = frame.getTransformTo(this, date);
        final Transform rotation = new Transform(date, new Rotation(Vector3D.PLUS_K, -getOrientation()));
        final PVCoordinates extPVTopo = new Transform(date, t, rotation).transformPVCoordinates(extPV);

        final Vector3D position = extPVTopo.getPosition();

        // Compute y angle
        final double yAngle = this.computeYangle(position);
        if (MathLib.abs(MathLib.abs(yAngle) - MathUtils.HALF_PI) < Precision.EPSILON) {
            throw new PatriusException(PatriusMessages.CARDAN_MOUNTING_UNDEFINED);
        }
        // Compute x angle
        final double xAngle = this.computeXangle(position);
        // Compute range
        final double range = this.computeRange(position);
        // Compute x angle rate
        final double xAngleRate = this.computeXangleRate(extPVTopo);
        // Compute y angle rate
        final double yAngleRate = this.computeYangleRate(extPVTopo);
        // Compute range rate (doppler) : relative rate along the line of sight
        final double rangeRate = this.computeRangeRate(extPVTopo);

        return new CardanMountPV(xAngle, yAngle, range, xAngleRate, yAngleRate, rangeRate);
    }

    /**
     * Transform topocentric set of coordinates frame into Cartesian position coordinates expressed in
     * this local topocentric.
     * 
     * @param topoCoord
     *        point in topocentric coordinates which shall be transformed
     * @return Cartesian coordinates of the point (only the position)
     */
    public Vector3D transformFromTopocentricToPosition(final TopocentricPosition topoCoord) {

        // Intermediate variables
        final double e = topoCoord.getElevation();
        final double alpha = topoCoord.getAzimuth() + getOrientation();
        final double d = topoCoord.getRange();
        // Lon/Lat
        final double[] sincosE = MathLib.sinAndCos(e);
        final double sinE = sincosE[0];
        final double cosE = sincosE[1];
        final double[] sincosAlpha = MathLib.sinAndCos(alpha);
        final double sinAlpha = sincosAlpha[0];
        final double cosAlpha = sincosAlpha[1];

        // Cartesian coordinates
        final double x = d * cosE * cosAlpha;
        final double y = -d * cosE * sinAlpha;
        final double z = d * sinE;
        // Return result
        return new Vector3D(x, y, z);
    }

    /**
     * Transform topocentric set of coordinates into Cartesian position and velocity coordinates
     * expressed in this local topocentric frame .
     * 
     * @param topoCoord
     *        point in topocentric coordinates which shall be transformed
     * @return Cartesian coordinates of the point (position and velocity)
     */
    public PVCoordinates transformFromTopocentricToPV(final TopocentricPV topoCoord) {

        // Position
        final Vector3D position = this.transformFromTopocentricToPosition(topoCoord.getTopocentricPosition());
        // Intermediate variables
        final double e = topoCoord.getElevation();
        final double eDot = topoCoord.getElevationRate();
        final double alpha = topoCoord.getAzimuth() + getOrientation();
        final double alphaDot = topoCoord.getAzimuthRate();
        final double d = topoCoord.getRange();
        final double dDot = topoCoord.getRangeRate();
        // Lon/Lat
        final double[] sincosE = MathLib.sinAndCos(e);
        final double sinE = sincosE[0];
        final double cosE = sincosE[1];
        final double[] sincosAlpha = MathLib.sinAndCos(alpha);
        final double sinAlpha = sincosAlpha[0];
        final double cosAlpha = sincosAlpha[1];

        // Compute derivative of position
        final double xDot = dDot * cosE * cosAlpha - d * (sinE * cosAlpha * eDot + cosE * sinAlpha * alphaDot);
        final double yDot = -dDot * cosE * sinAlpha - d * (-sinE * sinAlpha * eDot + cosE * cosAlpha * alphaDot);
        final double zDot = dDot * sinE + d * cosE * eDot;

        // Return PV
        return new PVCoordinates(position, new Vector3D(xDot, yDot, zDot));
    }

    /**
     * Transform a Cardan mounting into Cartesian coordinates (only position) expressed in this local
     * topocentric frame.
     * 
     * @param cardan
     *        Cardan mounting which shall be transformed
     * @return cartesian coordinates (only the position)
     */
    public Vector3D transformFromCardanToPosition(final CardanMountPosition cardan) {
        // Intermediate variables
        final double xAngle = cardan.getXangle();
        final double yAngle = cardan.getYangle();
        final double d = cardan.getRange();
        // Lon/Lat
        final double[] sincosX = MathLib.sinAndCos(xAngle);
        final double sinX = sincosX[0];
        final double cosX = sincosX[1];
        final double[] sincosY = MathLib.sinAndCos(yAngle);
        final double sinY = sincosY[0];
        final double cosY = sincosY[1];
        // Cartesian coordinates
        final double x = d * sinY;
        final double y = d * cosY * sinX;
        final double z = d * cosY * cosX;
        // Result
        final Vector3D intermediatePoint = new Vector3D(x, y, z);
        final Rotation rotation = new Rotation(Vector3D.PLUS_K, getOrientation());
        return rotation.applyInverseTo(intermediatePoint);
    }

    /**
     * Transform a Cardan mounting into Cartesian coordinates (position and velocity) expressed in this
     * local topocentric frame.
     * 
     * @param cardan
     *        Cardan mounting which shall be transformed
     * @return cartesian coordinates (position and velocity)
     */
    public PVCoordinates transformFromCardanToPV(final CardanMountPV cardan) {

        // Position
        final Vector3D position = this.transformFromCardanToPosition(cardan.getCardanMountPosition());
        // Intermediate variables
        final double xAngle = cardan.getXangle();
        final double yAngle = cardan.getYangle();
        final double d = cardan.getRange();
        final double xAngleDot = cardan.getXangleRate();
        final double yAngleDot = cardan.getYangleRate();
        final double dDot = cardan.getRangeRate();
        // Lon/lat
        final double[] sincosX = MathLib.sinAndCos(xAngle);
        final double sinX = sincosX[0];
        final double cosX = sincosX[1];
        final double[] sincosY = MathLib.sinAndCos(yAngle);
        final double sinY = sincosY[0];
        final double cosY = sincosY[1];

        // Compute derivative of position
        final double xDot = dDot * sinY + d * cosY * yAngleDot;
        final double yDot = dDot * cosY * sinX + d * (-sinY * sinX * yAngleDot + cosY * cosX * xAngleDot);
        final double zDot = dDot * cosY * cosX - d * (sinY * cosX * yAngleDot + cosY * sinX * xAngleDot);

        // Return PV
        final Rotation rotation = new Rotation(Vector3D.PLUS_K, getOrientation());
        return new PVCoordinates(position, rotation.applyInverseTo(new Vector3D(xDot, yDot, zDot)));
    }

    /**
     * Compute the limit visibility point for a satellite in a given direction.
     * <p>
     * This method can be used to compute visibility circles around ground stations for example, using a simple loop on
     * azimuth, with either a fixed elevation or an elevation that depends on azimuth to take ground masks into account.
     * </p>
     * 
     * @param radius
     *        satellite distance to Earth center
     * @param azimuth
     *        pointing azimuth from station
     * @param elevation
     *        pointing elevation from station
     * @return limit visibility point for the satellite
     * @throws PatriusException
     *         if point cannot be found
     */
    @SuppressWarnings("PMD.ExceptionAsFlowControl")
    public GeodeticPoint
            computeLimitVisibilityPoint(final double radius,
                                        final double azimuth, final double elevation) throws PatriusException {
        try {
            // convergence threshold on point position: 1mm
            final double deltaP = 0.001;
            // create univariate solver
            final UnivariateSolver solver =
                new BracketingNthOrderBrentSolver(deltaP / Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                    deltaP, deltaP, 5);

            // find the distance such that a point in the specified direction and at the solved-for
            // distance is exactly at the specified radius
            final double distance = solver.solve(1000, new UnivariateFunction(){
                /** {@inheritDoc} */
                @Override
                public double value(final double x) {
                    try {
                        final GeodeticPoint gp = TopocentricFrame.this.pointAtDistance(azimuth, elevation, x);
                        return TopocentricFrame.this.parentShape.transform(gp).getNorm() - radius;
                    } catch (final PatriusException oe) {
                        throw new PatriusExceptionWrapper(oe);
                    }
                }
            }, 0, 2 * radius);

            // return the limit point
            return this.pointAtDistance(azimuth, elevation, distance);

        } catch (final TooManyEvaluationsException tmee) {
            // Exception
            throw new PatriusException(tmee);
        } catch (final PatriusExceptionWrapper lwe) {
            // Exception
            throw lwe.getException();
        }
    }

    /**
     * Compute the point observed from the station at some specified distance.
     * 
     * @param azimuth
     *        pointing azimuth from station
     * @param elevation
     *        pointing elevation from station
     * @param distance
     *        distance to station
     * @return observed point
     * @exception PatriusException
     *            if point cannot be computed
     */
    public GeodeticPoint pointAtDistance(final double azimuth, final double elevation,
                                         final double distance) throws PatriusException {
        final Vector3D observed = new Vector3D(distance, new Vector3D(azimuth, elevation));
        return this.parentShape.transform(observed, this, AbsoluteDate.J2000_EPOCH);
    }

    /**
     * Get the {@link PVCoordinates} of the topocentric frame origin in the selected frame.
     * 
     * @param date
     *        current date
     * @param frame
     *        the frame where to define the position
     * @return position/velocity of the topocentric frame origin (m and m/s)
     * @exception PatriusException
     *            if position cannot be computed in given frame
     */
    @Override
    public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
        return this.getTransformTo(frame, date).transformPVCoordinates(PVCoordinates.ZERO);
    }

    /**
     * Compute the range of a point given in Cartesian coordinates in the local
     * topocentric frame.
     * 
     * @param extTopo
     *        point in Cartesian coordinates which shall be transformed
     * @return range
     */
    private double computeRange(final Vector3D extTopo) {
        return extTopo.getNorm();
    }

    /**
     * Compute the range rate of a point given in Cartesian coordinates in the local
     * topocentric frame.
     * 
     * @param extPVTopo
     *        point in Cartesian coordinates which shall be transformed
     * @return range rate
     */
    private double computeRangeRate(final PVCoordinates extPVTopo) {
        return Vector3D.dotProduct(extPVTopo.getPosition(),
            extPVTopo.getVelocity()) / extPVTopo.getPosition().getNorm();
    }

    /**
     * Compute the x angle of a point given in Cartesian coordinates in the local
     * topocentric frame.
     * 
     * @param extTopo
     *        point in Cartesian coordinates which shall be transformed
     * @return x angle
     */
    private double computeXangle(final Vector3D extTopo) {
        return MathLib.atan2(extTopo.getY(), extTopo.getZ());
    }

    /**
     * Compute the x angle rate of a point given in Cartesian coordinates in the local
     * topocentric frame.
     * 
     * @param extPVTopo
     *        point in Cartesian coordinates which shall be transformed
     * @return x angle rate
     */
    private double computeXangleRate(final PVCoordinates extPVTopo) {
        final Vector3D position = extPVTopo.getPosition();

        final Vector3D cross = Vector3D.crossProduct(position, extPVTopo.getVelocity());
        final double y = position.getY();
        final double z = position.getZ();

        return -cross.getX() / (y * y + z * z);
    }

    /**
     * Compute the y angle of a point given in Cartesian coordinates in the local
     * topocentric frame.
     * 
     * @param extTopo
     *        point in Cartesian coordinates which shall be transformed
     * @return y angle
     */
    private double computeYangle(final Vector3D extTopo) {
        return MathLib.asin(MathLib.min(1.0, MathLib.max(-1.0, extTopo.normalize().getX())));
    }

    /**
     * Compute the y angle rate of a point given in Cartesian coordinates in the local
     * topocentric frame.
     * 
     * @param extPVTopo
     *        point in Cartesian coordinates which shall be transformed
     * @return y angle rate
     */
    private double computeYangleRate(final PVCoordinates extPVTopo) {
        final Vector3D position = extPVTopo.getPosition();
        final Vector3D cross = Vector3D.crossProduct(position, extPVTopo.getVelocity());

        final double y = position.getY();
        final double z = position.getZ();
        final double d = position.getNorm();
        return (-y * cross.getZ() + z * cross.getY()) / (d * d * MathLib.sqrt(y * y + z * z));
    }

    /**
     * Getter for the aimuth elevation calculator.
     * @return
     *      the Azimuth Elevation Calculator
     */
    public AzimuthElevationCalculator getAzimuthElevationCalculator() {
        return azimuthElevationCalculator;
    }
}
