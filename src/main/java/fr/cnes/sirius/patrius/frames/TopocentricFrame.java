/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */

/*
 *
 * HISTORY
 * VERSION:4.12.1:FA:FA-124:05/09/2023:[PATRIUS] Problème de calcul d'accélération dans TopocentricFrame
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:DM:DM-3223:03/11/2022:[PATRIUS] Frame implements PVCoordinatesProvider
 * VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3165:10/05/2022:[PATRIUS] Amelioration de la propagation du signal 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
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

import fr.cnes.sirius.patrius.bodies.BodyPoint;
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
public final class TopocentricFrame extends Frame {

    /** Serializable UID. */
    private static final long serialVersionUID = -5997915708080966466L;

    /**
     * Body point used as origin of topocentric frame.<br>
     * <b><u>Warning</u></b>: Make sure to distinguish the topocentric zenith direction and the direction normal to
     * shape at this origin
     * point.
     */
    private final BodyPoint frameOrigin;

    /**
     * Zenith direction of this topocentric frame, which <u>can be different from the direction to shape at the frame
     * origin position.</u>
     */
    private final Vector3D zenith;

    /** Nadir direction. */
    private transient Vector3D nadir;

    /** North direction. */
    private transient Vector3D north;

    /** South direction. */
    private transient Vector3D south;

    /** East direction. */
    private transient Vector3D east;

    /** West direction. */
    private transient Vector3D west;

    /**
     * Oriented angle (trigowise, radian) between the local North and the Frame's x axis.<br>
     * Example :<br>
     * If "Reference Azimuth" is aligned with the local North of a local topocentric frame, then a frameOrientation of
     * -0.785 (=> -45°) means that the x axis of the Frame points to North-East.
     */
    private final double frameOrientation;

    /**
     * This constructor builds an East oriented topocentric frame, whose zenith direction (Z-axis) is aligned with the
     * direction normal to shape at entered frame origin position. Its axis are (x = East, y = North, z = Zenith).
     *
     * @param origin
     *        point on which the Topocentric frame is centered
     * @param name
     *        the string representation
     */
    public TopocentricFrame(final BodyPoint origin, final String name) {
        this(origin, -MathUtils.HALF_PI, name);
    }

    /**
     * This constructor builds an East oriented topocentric frame with user-defined zenith direction. Its axis are (x =
     * East, y = North, z = Zenith).
     *
     * @param origin
     *        point on which the Topocentric frame is centered (its normal direction is not used)
     * @param zenith
     *        local normal (expressed in body frame) used as topocentric zenith
     * @param name
     *        the string representation
     */
    public TopocentricFrame(final BodyPoint origin, final Vector3D zenith, final String name) {
        this(origin, zenith, -MathUtils.HALF_PI, name);
    }

    /**
     * Constructor with user-defined frame orientation and zenith direction (Z-axis) aligned with the direction normal
     * to shape at entered frame origin position.
     *
     * @param origin
     *        point on which the Topocentric frame is centered
     * @param frameOrientation
     *        oriented angle (trigowise, radian) between the local North and the Frame's x axis.<br>
     *        Example :<br>
     *        If "Reference Azimuth" is aligned with the local North of a local topocentric frame, then a
     *        frameOrientation of -0.785 (=> -45°) means that the x axis of the Frame points to North-East
     * @param name
     *        the string representation
     */
    public TopocentricFrame(final BodyPoint origin, final double frameOrientation, final String name) {
        this(origin, origin.getNormal(), frameOrientation, name);
    }

    /**
     * Constructor with user-defined zenith direction and frame orientation. Its axis are (x = East, y = North, z =
     * Zenith).
     *
     * @param origin
     *        point on which the Topocentric frame is centered (its normal direction is not used)
     * @param zenith
     *        local normal (expressed in body frame) used as topocentric zenith
     * @param frameOrientation
     *        oriented angle (trigowise, radian) between the local North and the Frame's x axis.<br>
     *        Example :<br>
     *        If "Reference Azimuth" is aligned with the local North of a local topocentric frame, then a
     *        frameOrientation of -0.785 (=> -45°) means that the x axis of the Frame points to North-East
     * @param name
     *        the string representation
     */
    public TopocentricFrame(final BodyPoint origin, final Vector3D zenith, final double frameOrientation,
                            final String name) {
        super(origin.getBodyShape().getBodyFrame(), createTransform(origin.getPosition(), zenith, frameOrientation),
                name, false);
        this.frameOrigin = origin;
        this.zenith = zenith;
        this.frameOrientation = frameOrientation;
    }

    /**
     * Compute the transformation from body centered frame to topocentric frame.
     *
     * @param cartesianPoint
     *        local cartesian point (expressed in body frame) where topocentric frame is centered
     * @param zenith
     *        local normal (expressed in body frame) used as topocentric zenith
     * @param frameOrientation
     *        the angle in radians between the North direction and the X-axis
     * @return the transformation from body centered frame to topocentric frame
     */
    private static Transform createTransform(final Vector3D cartesianPoint, final Vector3D zenith,
                                             final double frameOrientation) {

        // 1. Translation from body center to cartesian point
        // Works since the cartesian point is expressed in body frame (from constructor)
        final Transform translation = new Transform(AbsoluteDate.J2000_EPOCH, cartesianPoint);

        // 2. Rotate axes
        // Compute the rotation from the body centered frame to the topocentric frame
        final Transform rotation = computeTransformRotation(zenith, frameOrientation);

        // Compose both transformations
        return new Transform(AbsoluteDate.J2000_EPOCH, translation, rotation, true);
    }

    /**
     * Compute the rotation component of the transformation from body centered frame to topocentric frame.
     *
     * @param zenith
     *        zenith direction in parent body frame
     * @param frameOrientation
     *        the angle in radians between the North direction and the X-axis
     * @return the rotation component of the transformation
     */
    private static Transform computeTransformRotation(final Vector3D zenith, final double frameOrientation) {

        // First build the rotation from the parent shape's frame to the North topocentric frame
        final Vector3D east = computeEastFromZenith(zenith);
        final Vector3D xtopo = computeNorthFromZenithEast(zenith, east);
        final Vector3D ztopo = zenith;
        final Rotation firstRotation = new Rotation(Vector3D.PLUS_I, Vector3D.PLUS_K, xtopo, ztopo);

        // Then build the rotation from the North topocentric frame to the expected one.
        final Rotation secondRotation = new Rotation(Vector3D.PLUS_K, frameOrientation);

        // Finally build the transform that concatenates the two rotations
        return new Transform(AbsoluteDate.J2000_EPOCH, firstRotation.applyTo(secondRotation), Vector3D.ZERO);
    }

    /**
     * Compute north direction in parent body frame, given zenith and east direction.
     *
     * @param zenith
     *        zenith direction in parent body frame
     * @param east
     *        east direction in parent body frame
     * @return north direction in parent body frame
     */
    private static Vector3D computeNorthFromZenithEast(final Vector3D zenith, final Vector3D east) {
        return Vector3D.crossProduct(zenith, east).normalize();
    }

    /**
     * Compute east direction in parent body frame, given zenith direction.
     *
     * @param zenith
     *        zenith direction in parent body frame
     * @return east direction in parent body frame
     */
    private static Vector3D computeEastFromZenith(final Vector3D zenith) {
        Vector3D res = Vector3D.crossProduct(Vector3D.PLUS_K, zenith).normalize();
        if (res.getNorm() < Precision.DOUBLE_COMPARISON_EPSILON) {
            // Case zenith is aligned with +K, east is set arbitrarily to +J (former PATRIUS convention)
            res = Vector3D.PLUS_J;
        }
        return res;
    }

    /**
     * Getter for the oriented angle between the local north and the x axis (trigowise).
     *
     * @return the frame orientation
     */
    public double getOrientation() {
        return this.frameOrientation;
    }

    /**
     * Getter for the frame origin point.
     * <p>
     * <u><b>Warning:</b></u> Make sure to distinguish the direction normal to shape at this origin point and the
     * topocentric zenith direction (Z-axis) which can be retrieved invoking the getter {@link #getZenith()}.
     * </p>
     *
     * @return the frame origin point
     */
    public BodyPoint getFrameOrigin() {
        return this.frameOrigin;
    }

    /**
     * Getter for the zenith direction of topocentric frame, expressed in the body frame of body shape to which the
     * frame center point is attached.
     * <p>
     * <u><b>Warning:</b></u> Be aware that the zenith direction is not necessarily aligned with the direction normal to
     * shape at the frame origin point.
     * </p>
     *
     * @return unit vector in the zenith direction
     * @see #getNadir()
     */
    public Vector3D getZenith() {
        return this.zenith;
    }

    /**
     * Getter for the nadir direction of topocentric frame, expressed in the body frame of body shape to which the frame
     * center point is attached.
     * <p>
     * The nadir direction is defined as the opposite to zenith direction.
     * </p>
     *
     * @return unit vector in the nadir direction
     * @see #getZenith()
     */
    public Vector3D getNadir() {
        if (this.nadir == null) {
            this.nadir = this.zenith.negate();
        }
        return this.nadir;
    }

    /**
     * Getter for the north direction of topocentric frame, expressed in the body frame of body shape to which the frame
     * center point is attached.
     * <p>
     * The north direction is defined in the horizontal plane (normal to zenith direction) and following the local
     * meridian.
     * </p>
     *
     * @return unit vector in the north direction
     * @see #getSouth()
     */
    public Vector3D getNorth() {
        if (this.north == null) {
            this.north = computeNorthFromZenithEast(getZenith(), getEast());
        }
        return this.north;
    }

    /**
     * Getter for the south direction of topocentric frame, expressed in the body frame of body shape to which the frame
     * center point is attached.
     * <p>
     * The south direction is the opposite of north direction.
     * </p>
     *
     * @return unit vector in the south direction
     * @see #getNorth()
     */
    public Vector3D getSouth() {
        if (this.south == null) {
            this.south = getNorth().negate();
        }
        return this.south;
    }

    /**
     * Getter for the east direction of topocentric frame, expressed in the body frame of body shape to which the frame
     * center point is attached.
     * <p>
     * The east direction is defined in the horizontal plane in order to complete direct triangle (east, north, zenith).
     * </p>
     *
     * @return unit vector in the east direction
     * @see #getWest()
     */
    public Vector3D getEast() {
        if (this.east == null) {
            this.east = computeEastFromZenith(getZenith());
        }
        return this.east;
    }

    /**
     * Getter for the west direction of topocentric frame, expressed in the body frame of body shape to which the frame
     * center point is attached.
     * <p>
     * The west direction is the opposite of east direction.
     * </p>
     *
     * @return unit vector in the west direction
     * @see #getEast()
     */
    public Vector3D getWest() {
        if (this.west == null) {
            this.west = Vector3D.crossProduct(getZenith(), getNorth());
        }
        return this.west;
    }

    /**
     * Getter for the elevation of a point with regards to the local point.
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
     * @throws PatriusException
     *         if frames transformations cannot be computed
     */
    public double getElevation(final Vector3D extPoint, final Frame frame, final AbsoluteDate date)
        throws PatriusException {
        // Transform given point from given frame to topocentric frame
        final Transform t = frame.getTransformTo(this, date);
        final Vector3D extPointTopo = t.transformPosition(extPoint);

        // Elevation angle is PI/2 - angle between zenith and given point direction
        return AzimuthElevationCalculator.getElevation(extPointTopo);
    }

    /**
     * Getter for the elevation derivative of a point wrt the local point (dElevation) express in the specified frame.
     *
     * @param extPoint
     *        point for which elevation shall be computed
     * @param frame
     *        frame in which the point is defined
     * @param date
     *        computation date
     * @return the elevation derivative of a point wrt the local point
     * @throws PatriusException
     *         if frames transformations cannot be computed
     */
    public Vector3D getDElevation(final Vector3D extPoint, final Frame frame, final AbsoluteDate date)
        throws PatriusException {
        final Vector3D dElevationInOutputFrame;
        if (frame.equals(this)) {
            // No transformation needed
            dElevationInOutputFrame = AzimuthElevationCalculator.computeDElevation(extPoint);
        } else {
            // Transform the given point (position) from the given frame to the topocentric frame
            final Transform t = frame.getTransformTo(this, date);
            final Vector3D extPointTopo = t.transformPosition(extPoint);

            // Compute the elevation derivative in the topo frame
            final Vector3D dElevationInTopoFrame = AzimuthElevationCalculator.computeDElevation(extPointTopo);

            // Transform the derivatives vector back to the given frame
            dElevationInOutputFrame = t.getInverse().transformVector(dElevationInTopoFrame);
        }

        return dElevationInOutputFrame;
    }

    /**
     * Getter for the elevation rate of a point.
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
    public double getElevationRate(final PVCoordinates extPV, final Frame frame, final AbsoluteDate date)
        throws PatriusException {
        // Transform given point from given frame to topocentric frame
        final Transform t = frame.getTransformTo(this, date);
        final PVCoordinates extPVTopo = t.transformPVCoordinates(extPV);

        // Compute elevation rate
        return AzimuthElevationCalculator.computeElevationRate(extPVTopo);
    }

    /**
     * Getter for the azimuth of a point with regards to the topocentric frame center point.
     * <p>
     * The azimuth is the angle between the North direction at local point and the projection in local horizontal plane
     * of the direction from local point to given point. Azimuth angles are counted clockwise, i.e positive towards the
     * East (range [0; 2PI]).
     * </p>
     *
     * @param extPoint
     *        point for which azimuth shall be computed
     * @param frame
     *        frame in which the point is defined
     * @param date
     *        computation date
     * @return azimuth of the point
     * @throws PatriusException
     *         if frames transformations cannot be computed
     */
    public double getAzimuth(final Vector3D extPoint, final Frame frame, final AbsoluteDate date)
        throws PatriusException {
        // Transform given point from given frame to topocentric frame
        final Transform t = this.getTransformTo(frame, date).getInverse();
        final Vector3D extPointTopo = t.transformPosition(extPoint);

        // Compute azimuth
        return AzimuthElevationCalculator.getAzimuth(extPointTopo, this.frameOrientation);
    }

    /**
     * Getter for the azimuth derivative of a point wrt the local point (dAzimuth) express in the specified frame.
     *
     * @param extPoint
     *        point for which elevation shall be computed
     * @param frame
     *        frame in which the point is defined
     * @param date
     *        computation date
     * @return the azimuth derivative of a point wrt the local point
     * @throws PatriusException
     *         if frames transformations cannot be computed
     */
    public Vector3D getDAzimuth(final Vector3D extPoint, final Frame frame, final AbsoluteDate date)
        throws PatriusException {
        final Vector3D dAzimuthInOutputFrame;
        if (frame.equals(this)) {
            // No transformation needed
            dAzimuthInOutputFrame = AzimuthElevationCalculator.computeDAzimuth(extPoint);
        } else {
            // Transform the given point (position) from the given frame to the topocentric frame
            final Transform t = frame.getTransformTo(this, date);
            final Vector3D extPointTopo = t.transformPosition(extPoint);

            // Compute the azimuth derivative in the topo frame
            final Vector3D dAzimuthInTopoFrame = AzimuthElevationCalculator.computeDAzimuth(extPointTopo);

            // Transform the derivatives vector back to the given frame
            dAzimuthInOutputFrame = t.getInverse().transformVector(dAzimuthInTopoFrame);
        }

        return dAzimuthInOutputFrame;
    }

    /**
     * Getter for the azimuth rate of a point.
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
    public double getAzimuthRate(final PVCoordinates extPV, final Frame frame, final AbsoluteDate date)
        throws PatriusException {
        // Transform given point from given frame to topocentric frame
        final Transform t = frame.getTransformTo(this, date);
        final PVCoordinates extPVTopo = t.transformPVCoordinates(extPV);

        // Compute azimuth rate
        return AzimuthElevationCalculator.computeAzimuthRate(extPVTopo);
    }

    /**
     * Getter for the Cardan x angle of a point.
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
    public double getXangleCardan(final Vector3D extPoint, final Frame frame, final AbsoluteDate date)
        throws PatriusException {
        // Transform given point from given frame to topocentric frame
        final Transform t = frame.getTransformTo(this, date);
        final Transform rotation = new Transform(date, new Rotation(Vector3D.PLUS_K, getOrientation()));
        final Vector3D extPointTopo = new Transform(date, t, rotation).transformPosition(extPoint);

        // Compute x angle
        return computeXangle(extPointTopo);
    }

    /**
     * Getter for the Cardan x angle rate. See Spaceflight Dynamics, Part I, chapter 10.4.5.2
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
    public double getXangleCardanRate(final PVCoordinates extPV, final Frame frame, final AbsoluteDate date)
        throws PatriusException {
        // Transform given point from given frame to topocentric frame
        final Transform t = frame.getTransformTo(this, date);
        final Transform rotation = new Transform(date, new Rotation(Vector3D.PLUS_K, getOrientation()));
        final PVCoordinates extPVTopo = new Transform(date, t, rotation).transformPVCoordinates(extPV);

        // Compute x angle rate
        return computeXangleRate(extPVTopo);
    }

    /**
     * Getter for the Cardan y angle of a point with regards to the projection point on the plane defined by the zenith
     * and the west axis.
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
    public double getYangleCardan(final Vector3D extPoint, final Frame frame, final AbsoluteDate date)
        throws PatriusException {
        // Transform given point from given frame to topocentric frame
        final Transform t = frame.getTransformTo(this, date);
        final Transform rotation = new Transform(date, new Rotation(Vector3D.PLUS_K, getOrientation()));
        final Vector3D extPointTopo = new Transform(date, t, rotation).transformPosition(extPoint);

        // Compute y angle
        return computeYangle(extPointTopo);
    }

    /**
     * Getter for the Cardan y angle rate. See Spaceflight Dynamics, Part I, chapter 10.4.5.2
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
    public double getYangleCardanRate(final PVCoordinates extPV, final Frame frame, final AbsoluteDate date)
        throws PatriusException {
        // Transform given point from given frame to topocentric frame
        final Transform t = frame.getTransformTo(this, date);
        final Transform rotation = new Transform(date, new Rotation(Vector3D.PLUS_K, getOrientation()));
        final PVCoordinates extPVTopo = new Transform(date, t, rotation).transformPVCoordinates(extPV);

        // Compute y angle rate
        return computeYangleRate(extPVTopo);
    }

    /**
     * Getter for the range of a point with regards to the topocentric frame center point.
     *
     * @param extPoint
     *        point for which range shall be computed
     * @param frame
     *        frame in which the point is defined
     * @param date
     *        computation date
     * @return range (distance) of the point
     * @throws PatriusException
     *         if frames transformations cannot be computed
     */
    public double getRange(final Vector3D extPoint, final Frame frame, final AbsoluteDate date)
        throws PatriusException {
        // Transform given point from given frame to topocentric frame
        final Transform t = frame.getTransformTo(this, date);
        final Vector3D extPointTopo = t.transformPosition(extPoint);

        // Compute range
        return computeRange(extPointTopo);
    }

    /**
     * Getter for the range rate of a point with regards to the topocentric frame center point.
     *
     * @param extPV
     *        point/velocity for which range rate shall be computed
     * @param frame
     *        frame in which the point is defined
     * @param date
     *        computation date
     * @return range rate of the point (positive if point departs from frame)
     * @throws PatriusException
     *         if frames transformations cannot be computed
     */
    public double getRangeRate(final PVCoordinates extPV, final Frame frame, final AbsoluteDate date)
        throws PatriusException {
        // Transform given point from given frame to topocentric frame
        final Transform t = frame.getTransformTo(this, date);
        final PVCoordinates extPVTopo = t.transformPVCoordinates(extPV);

        // Compute range rate (doppler) : relative rate along the line of sight
        return computeRangeRate(extPVTopo);
    }

    /**
     * Transform a Cartesian position coordinates into topocentric coordinates in this local topocentric frame.
     *
     * @param extPoint
     *        point in Cartesian coordinates which shall be transformed
     * @param frame
     *        frame in which the point is defined
     * @param date
     *        computation date
     * @return topocentic coordinates of the point
     * @throws PatriusException
     *         if frames transformations cannot be computed
     */
    public TopocentricPosition transformFromPositionToTopocentric(final Vector3D extPoint, final Frame frame,
                                                                  final AbsoluteDate date)
        throws PatriusException {
        // Transform given point from given frame to topocentric frame
        final Transform t = frame.getTransformTo(this, date);
        final Vector3D extPointTopo = t.transformPosition(extPoint);

        // Compute elevation
        final double elevation = AzimuthElevationCalculator.getElevation(extPointTopo);
        if (MathLib.abs(MathLib.abs(elevation) - MathUtils.HALF_PI) < Precision.EPSILON) {
            throw new PatriusException(PatriusMessages.AZIMUTH_UNDEFINED);
        }
        // Compute azimuth
        final double azimuth = AzimuthElevationCalculator.getAzimuth(extPointTopo, this.frameOrientation);
        // Compute range
        final double range = computeRange(extPointTopo);

        return new TopocentricPosition(elevation, azimuth, range);
    }

    /**
     * Transform a Cartesian position and velocity coordinates into topocentric coordinates in this local topocentric
     * frame.
     *
     * @param extPV
     *        point in Cartesian coordinates which shall be transformed
     * @param frame
     *        frame in which the point is defined
     * @param date
     *        computation date
     * @return topocentic coordinates of the point
     * @throws PatriusException
     *         if frames transformations cannot be computed
     */
    public TopocentricPV transformFromPVToTopocentric(final PVCoordinates extPV, final Frame frame,
                                                      final AbsoluteDate date)
        throws PatriusException {
        // Transform given point from given frame to topocentric frame
        final Transform t = frame.getTransformTo(this, date);
        final PVCoordinates extPVTopo = t.transformPVCoordinates(extPV);

        final Vector3D position = extPVTopo.getPosition();

        // Compute elevation
        final double elevation = AzimuthElevationCalculator.getElevation(position);
        if (MathLib.abs(MathLib.abs(elevation) - MathUtils.HALF_PI) < Precision.EPSILON) {
            throw new PatriusException(PatriusMessages.AZIMUTH_UNDEFINED);
        }
        // Compute azimuth
        final double azimuth = AzimuthElevationCalculator.getAzimuth(position, this.frameOrientation);
        // Compute range
        final double range = computeRange(position);

        // Compute elevation rate
        final double elevationRate = AzimuthElevationCalculator.computeElevationRate(extPVTopo);
        // Compute azimuth rate
        final double azimuthRate = AzimuthElevationCalculator.computeAzimuthRate(extPVTopo);
        // Compute range rate
        final double rangeRate = computeRangeRate(extPVTopo);

        return new TopocentricPV(elevation, azimuth, range, elevationRate, azimuthRate, rangeRate);
    }

    /**
     * Transform a Cartesian position coordinates into Cardan mounting in this local topocentric frame.
     *
     * @param extPoint
     *        point in Cartesian coordinates which shall be transformed
     * @param frame
     *        frame in which the point is defined
     * @param date
     *        computation date
     * @return Cardan mounting of the point
     * @throws PatriusException
     *         if frames transformations cannot be computed
     */
    public CardanMountPosition transformFromPositionToCardan(final Vector3D extPoint, final Frame frame,
                                                             final AbsoluteDate date)
        throws PatriusException {
        // Transform given point from given frame to topocentric frame
        final Transform t = frame.getTransformTo(this, date);
        final Transform rotation = new Transform(date, new Rotation(Vector3D.PLUS_K, -getOrientation()));
        final Vector3D extPositionTopo = new Transform(date, t, rotation).transformPosition(extPoint);

        // Compute y angle
        final double yAngle = computeYangle(extPositionTopo);
        if (MathLib.abs(MathLib.abs(yAngle) - MathUtils.HALF_PI) < Precision.EPSILON) {
            throw new PatriusException(PatriusMessages.CARDAN_MOUNTING_UNDEFINED);
        }
        // Compute x angle
        final double xAngle = computeXangle(extPositionTopo);
        // Compute range
        final double range = computeRange(extPositionTopo);

        return new CardanMountPosition(xAngle, yAngle, range);
    }

    /**
     * Transform a Cartesian position coordinates into Cardan mounting in this local topocentric frame.
     *
     * @param extPV
     *        point in Cartesian coordinates which shall be transformed
     * @param frame
     *        frame in which the point is defined
     * @param date
     *        computation date
     * @return Cardan mounting of the point
     * @throws PatriusException
     *         if frames transformations cannot be computed
     */
    public CardanMountPV transformFromPVToCardan(final PVCoordinates extPV, final Frame frame, final AbsoluteDate date)
        throws PatriusException {
        // Transform given point from given frame to topocentric frame
        final Transform t = frame.getTransformTo(this, date);
        final Transform rotation = new Transform(date, new Rotation(Vector3D.PLUS_K, -getOrientation()));
        final PVCoordinates extPVTopo = new Transform(date, t, rotation).transformPVCoordinates(extPV);

        final Vector3D position = extPVTopo.getPosition();

        // Compute y angle
        final double yAngle = computeYangle(position);
        if (MathLib.abs(MathLib.abs(yAngle) - MathUtils.HALF_PI) < Precision.EPSILON) {
            throw new PatriusException(PatriusMessages.CARDAN_MOUNTING_UNDEFINED);
        }
        // Compute x angle
        final double xAngle = computeXangle(position);
        // Compute range
        final double range = computeRange(position);
        // Compute x angle rate
        final double xAngleRate = computeXangleRate(extPVTopo);
        // Compute y angle rate
        final double yAngleRate = computeYangleRate(extPVTopo);
        // Compute range rate (doppler) : relative rate along the line of sight
        final double rangeRate = computeRangeRate(extPVTopo);

        return new CardanMountPV(xAngle, yAngle, range, xAngleRate, yAngleRate, rangeRate);
    }

    /**
     * Transform topocentric set of coordinates frame into Cartesian position coordinates expressed in this local
     * topocentric.
     *
     * @param topoCoord
     *        point in topocentric coordinates which shall be transformed
     * @return cartesian coordinates of the point (only the position)
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
     * Transform topocentric set of coordinates into Cartesian position and velocity coordinates expressed in this local
     * topocentric frame.
     *
     * @param topoCoord
     *        point in topocentric coordinates which shall be transformed
     * @return cartesian coordinates of the point (position and velocity)
     */
    public PVCoordinates transformFromTopocentricToPV(final TopocentricPV topoCoord) {
        // Position
        final Vector3D position = transformFromTopocentricToPosition(topoCoord.getTopocentricPosition());
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
     * Transform a Cardan mounting into Cartesian coordinates (only position) expressed in this local topocentric frame.
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
     * Transform a Cardan mounting into Cartesian coordinates (position and velocity) expressed in this local
     * topocentric frame.
     *
     * @param cardan
     *        Cardan mounting which shall be transformed
     * @return cartesian coordinates (position and velocity)
     */
    public PVCoordinates transformFromCardanToPV(final CardanMountPV cardan) {
        // Position
        final Vector3D position = transformFromCardanToPosition(cardan.getCardanMountPosition());
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
     * Compute the range of a point given in Cartesian coordinates in the local topocentric frame.
     *
     * @param extTopo
     *        point in Cartesian coordinates which shall be transformed
     * @return range
     */
    private static double computeRange(final Vector3D extTopo) {
        return extTopo.getNorm();
    }

    /**
     * Compute the range rate of a point given in Cartesian coordinates in the local topocentric frame.
     *
     * @param extPVTopo
     *        point in Cartesian coordinates which shall be transformed
     * @return range rate
     */
    private static double computeRangeRate(final PVCoordinates extPVTopo) {
        return Vector3D.dotProduct(extPVTopo.getPosition(), extPVTopo.getVelocity())
                / extPVTopo.getPosition().getNorm();
    }

    /**
     * Compute the x angle of a point given in Cartesian coordinates in the local topocentric frame.
     *
     * @param extTopo
     *        point in Cartesian coordinates which shall be transformed
     * @return x angle
     */
    private static double computeXangle(final Vector3D extTopo) {
        return MathLib.atan2(extTopo.getY(), extTopo.getZ());
    }

    /**
     * Compute the x angle rate of a point given in Cartesian coordinates in the local topocentric frame.
     *
     * @param extPVTopo
     *        point in Cartesian coordinates which shall be transformed
     * @return x angle rate
     */
    private static double computeXangleRate(final PVCoordinates extPVTopo) {
        final Vector3D position = extPVTopo.getPosition();

        final Vector3D cross = Vector3D.crossProduct(position, extPVTopo.getVelocity());
        final double y = position.getY();
        final double z = position.getZ();

        return -cross.getX() / (y * y + z * z);
    }

    /**
     * Compute the y angle of a point given in Cartesian coordinates in the local topocentric frame.
     *
     * @param extTopo
     *        point in Cartesian coordinates which shall be transformed
     * @return y angle
     */
    private static double computeYangle(final Vector3D extTopo) {
        return MathLib.asin(MathLib.min(1.0, MathLib.max(-1.0, extTopo.normalize().getX())));
    }

    /**
     * Compute the y angle rate of a point given in Cartesian coordinates in the local topocentric frame.
     *
     * @param extPVTopo
     *        point in Cartesian coordinates which shall be transformed
     * @return y angle rate
     */
    private static double computeYangleRate(final PVCoordinates extPVTopo) {
        final Vector3D position = extPVTopo.getPosition();
        final Vector3D cross = Vector3D.crossProduct(position, extPVTopo.getVelocity());

        final double y = position.getY();
        final double z = position.getZ();
        final double d = position.getNorm();
        return (-y * cross.getZ() + z * cross.getY()) / (d * d * MathLib.sqrt(y * y + z * z));
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
    public BodyPoint computeLimitVisibilityPoint(final double radius, final double azimuth, final double elevation)
        throws PatriusException {

        try {
            // convergence threshold on point position: 1mm
            final double deltaP = 0.001;
            // create univariate solver
            final UnivariateSolver solver =
                new BracketingNthOrderBrentSolver(deltaP / Constants.WGS84_EARTH_EQUATORIAL_RADIUS, deltaP, deltaP, 5);

            // find the distance such that a point in the specified direction and at the solved-for
            // distance is exactly at the specified radius
            final double distance = solver.solve(1000, new UnivariateFunction(){

                /** Serializable UID. */
                private static final long serialVersionUID = -4720023892134584255L;

                /** {@inheritDoc} */
                @Override
                public double value(final double x) {

                    try {
                        final BodyPoint gp = TopocentricFrame.this.pointAtDistance(azimuth, elevation, x);
                        return gp.getPosition().getNorm() - radius;
                    } catch (final PatriusException oe) {
                        throw new PatriusExceptionWrapper(oe);
                    }
                }
            }, 0, 2 * radius);

            // return the limit point
            return pointAtDistance(azimuth, elevation, distance);

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
     * @throws PatriusException
     *         if point cannot be computed
     */
    public BodyPoint pointAtDistance(final double azimuth, final double elevation, final double distance)
        throws PatriusException {
        final Vector3D observed = new Vector3D(distance, new Vector3D(azimuth, elevation));
        return this.frameOrigin.getBodyShape().buildPoint(observed, this, null, "observedPoint");
    }
}
