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
 * HISTORY
 * VERSION:4.11.1:DM:DM-75:30/06/2023:[PATRIUS] Degradation performance Patrius 4.11
 * VERSION:4.11.1:DM:DM-88:30/06/2023:[PATRIUS] Complement FT 3319
 * VERSION:4.10.1:FA:FA-3267:02/12/2022:[PATRIUS] Anomalie gestion acceleration null PVCoordinates (suite)
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:FA:FA-3199:03/11/2022:[PATRIUS] Mise en œuvre PM 2973, gestion coordonnees et referentiel
 * VERSION:4.9:DM:DM-3093:10/05/2022:[PATRIUS] Mise en Oeuvre PM2973 , gestion coordonnees et referentiel 
 * VERSION:4.9:DM:DM-3135:10/05/2022:[PATRIUS] Calcul d'intersection sur BodyShape  
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:FA:FA-2464:27/05/2020:Anomalie dans le calcul du vecteur rotation des LOF
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:85:18/07/2013:Added a transformWrench method
 * VERSION::FA:356:20/03/2015: Performance degradation in conversions
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::FA:423:17/11/2015: improve computation times
 * VERSION::DM:489:06/10/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.transformations;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.utils.TimeStampedPVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeInterpolable;
import fr.cnes.sirius.patrius.time.TimeShiftable;
import fr.cnes.sirius.patrius.time.TimeStamped;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.AngularDerivativesFilter;
import fr.cnes.sirius.patrius.utils.CartesianDerivativesFilter;
import fr.cnes.sirius.patrius.utils.TimeStampedAngularCoordinates;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.wrenches.Wrench;

/**
 * Transformation class in three dimensional space.
 *
 * <p>
 * This class represents the transformation engine between {@link fr.cnes.sirius.patrius.frames.Frame frames}. It is
 * used both to define the relationship between each frame and its parent frame and to gather all individual transforms
 * into one operation when converting between frames far away from each other.
 * </p>
 * <p>
 * A Transform object contain the position, velocity and orientation vectors that describe the "destination"
 * {@link fr.cnes.sirius.patrius.frames.Frame frames} in the "origin" one. This means that, defining X_ref=(1,0,0) in
 * the origin frame, the vector X_destination (X axis of the destination frame, still expressed in the reference frame)
 * is obtained by : rotation.applyTo(X_ref).
 * </p>
 *
 * <p>
 * Instances of this class are guaranteed to be immutable.
 * </p>
 *
 * <h5>Example</h5>
 *
 * <pre>
 *
 * 1 ) Example of translation from R<sub>A</sub> to R<sub>B</sub>:
 * We want to transform the {@link PVCoordinates} PV<sub>A</sub> to PV<sub>B</sub>.
 * 
 * With :  PV<sub>A</sub> = ({1, 0, 0} , {1, 0, 0} , {1, 0, 0});
 * and  :  PV<sub>B</sub> = ({0, 0, 0} , {0, 0, 0} , {0, 0, 0});
 * 
 * The transform to apply then is defined as follows :
 * 
 * Vector3D translation = new Vector3D(1,0,0);
 * Vector3D velocity = new Vector3D(1,0,0);
 * Vector3D acc = new Vector3D(1,0,0);
 * 
 * Transform R1toR2 = new Transform(date, translation, Velocity, acc);
 * 
 * PV<sub>B</sub> = R1toR2.transformPVCoordinates(PV<sub>A</sub>);
 * 
 * 
 * 2 ) Example of rotation from R<sub>A</sub> to R<sub>B</sub>:
 * We want to transform the {@link PVCoordinates} PV<sub>A</sub> to PV<sub>B</sub>.
 * 
 * With :  PV<sub>A</sub> = ({0, 1, 0}, {-2, 1, 0});
 * and  :  PV<sub>B</sub> = ({1, 0, 0}, {1, 0, 0});
 * 
 * The transform to apply then is defined as follows :
 * 
 * Rotation rotation = new Rotation(Vector3D.PLUS_K, FastMath.PI / 2);
 * Vector3D rotationRate = new Vector3D(0, 0, 2);
 * 
 * Transform R1toR2 = new Transform(date, rotation, rotationRate);
 * 
 * PV<sub>B</sub> = R1toR2.transformPVCoordinates(PV<sub>A</sub>);
 *
 * </pre>
 *
 * @author Luc Maisonobe
 * @author Fabien Maussion
 *
 */
public class Transform implements TimeStamped, TimeShiftable<Transform>, TimeInterpolable<Transform>, Serializable {

    /** Identity transform. */
    public static final Transform IDENTITY = new IdentityTransform();

    /** Serializable UID. */
    private static final long serialVersionUID = -8809893979516295102L;

    /** Date of the transform. */
    private final AbsoluteDate date;

    /** Cartesian coordinates of the target frame with respect to the original frame. */
    private final PVCoordinates cartesian;

    /** Angular coordinates of the target frame with respect to the original frame. */
    private final AngularCoordinates angular;

    /**
     * Build a translation transform.
     *
     * @param dateIn
     *        date of the transform
     * @param translation
     *        the position of the "destination" frame expressed in the "origin" one
     */
    public Transform(final AbsoluteDate dateIn, final Vector3D translation) {
        this(dateIn, new PVCoordinates(translation, Vector3D.ZERO, Vector3D.ZERO), AngularCoordinates.IDENTITY);
    }

    /**
     * Build a rotation transform.
     *
     * @param dateIn
     *        date of the transform
     * @param rotation
     *        the orientation of the "destination" frame expressed in the "origin" one
     */
    public Transform(final AbsoluteDate dateIn, final Rotation rotation) {
        this(dateIn, PVCoordinates.ZERO, new AngularCoordinates(rotation, Vector3D.ZERO, Vector3D.ZERO));
    }

    /**
     * Build a translation transform, with its first time derivative.
     *
     * @param dateIn
     *        date of the transform
     * @param translation
     *        the position of the "destination" frame expressed in the "origin" one
     * @param velocity
     *        the velocity of the "destination" frame expressed in the "origin" one
     */
    public Transform(final AbsoluteDate dateIn, final Vector3D translation, final Vector3D velocity) {
        this(dateIn, new PVCoordinates(translation, velocity, Vector3D.ZERO), AngularCoordinates.IDENTITY);
    }

    /**
     * Build a translation transform, with its first and second time derivatives.
     *
     * @param dateIn
     *        date of the transform
     * @param translation
     *        the position of the "destination" frame expressed in the "origin" one
     * @param velocity
     *        the velocity of the "destination" frame expressed in the "origin" one
     * @param acceleration
     *        the acceleration of the "destination" frame expressed in the "origin" one
     */
    public Transform(final AbsoluteDate dateIn, final Vector3D translation,
        final Vector3D velocity, final Vector3D acceleration) {
        this(dateIn,
            new PVCoordinates(translation, velocity, acceleration),
            AngularCoordinates.IDENTITY);
    }

    /**
     * Build a translation transform, with its first time derivative.
     *
     * @param dateIn
     *        date of the transform
     * @param cartesianIn
     *        position and velocity of the "destination" frame expressed in the "origin" one
     */
    public Transform(final AbsoluteDate dateIn, final PVCoordinates cartesianIn) {
        this(dateIn, cartesianIn, AngularCoordinates.IDENTITY);
    }

    /**
     * Build a rotation transform.
     *
     * @param dateIn
     *        date of the transform
     * @param rotation
     *        the orientation of the "destination" frame expressed in the "origin" one
     * @param rotationRate
     *        the rotation rate of the "destination" frame in the "origin" one, expressed in the
     *        "destination" frame
     */
    public Transform(final AbsoluteDate dateIn, final Rotation rotation, final Vector3D rotationRate) {
        this(dateIn, PVCoordinates.ZERO, new AngularCoordinates(rotation, rotationRate, Vector3D.ZERO));
    }

    /**
     * Build a rotation transform.
     *
     * @param dateIn
     *        date of the transform
     * @param rotation
     *        the orientation of the "destination" frame expressed in the "origin" one
     * @param rotationRate
     *        the rotation rate of the "destination" frame in the "origin" one, expressed in the
     *        "destination" frame
     * @param rotationAcceleration
     *        the axis of the instant rotation
     *        expressed in the new frame. (norm representing angular rate)
     */
    public Transform(final AbsoluteDate dateIn, final Rotation rotation, final Vector3D rotationRate,
        final Vector3D rotationAcceleration) {
        this(dateIn,
            PVCoordinates.ZERO,
            new AngularCoordinates(rotation, rotationRate, rotationAcceleration));
    }

    /**
     * Build a rotation transform.
     *
     * @param dateIn
     *        date of the transform
     * @param angularIn
     *        {@link AngularCoordinates} of the "destination" frame in the "origin" one
     */
    public Transform(final AbsoluteDate dateIn, final AngularCoordinates angularIn) {
        this(dateIn, PVCoordinates.ZERO, angularIn);
    }

    /**
     * Build a transform by combining two existing ones without computing spin derivatives.
     *
     * <p>
     * <b>Warning: </b>spin derivative is not computed.
     * </p>
     *
     * @param dateIn
     *        date of the transform
     * @param first
     *        first transform applied
     * @param second
     *        second transform applied
     */
    public Transform(final AbsoluteDate dateIn, final Transform first, final Transform second) {
        this(dateIn, first, second, false);
    }

    /**
     * Build a transform by combining two existing ones, while not simply projecting velocity and acceleration.
     * <p>
     * Note that the dates of the two existing transformed are <em>ignored</em>, and the combined transform date is set
     * to the date supplied in this constructor without any attempt to shift the raw transforms. This is a design choice
     * allowing user full control of the combination.
     * </p>
     *
     * @param dateIn
     *        date of the transform
     * @param first
     *        first transform applied
     * @param second
     *        second transform applied
     * @param computeSpinDerivatives
     *        true if spin derivatives should be computed. If not, spin derivative is set to <i>null</i>
     */
    public Transform(final AbsoluteDate dateIn, final Transform first, final Transform second,
                     final boolean computeSpinDerivatives) {
        this(dateIn, first, second, computeSpinDerivatives, false);
    }

    /**
     * Build a transform by combining two existing ones.
     * <p>
     * Note that the dates of the two existing transformed are <em>ignored</em>, and the combined transform date is set
     * to the date supplied in this constructor without any attempt to shift the raw transforms. This is a design choice
     * allowing user full control of the combination.
     * </p>
     *
     * @param dateIn
     *        date of the transform
     * @param first
     *        first transform applied
     * @param second
     *        second transform applied
     * @param computeSpinDerivatives
     *        true if spin derivatives should be computed. If not, spin derivative is set to <i>null</i>
     * @param projectVelocityAndAcceleration
     *        true if velocity and acceleration should be simply projected, false otherwise
     */
    public Transform(final AbsoluteDate dateIn, final Transform first, final Transform second,
                     final boolean computeSpinDerivatives, final boolean projectVelocityAndAcceleration) {
        this(dateIn, compositePVCoordinates(first, second, projectVelocityAndAcceleration),
                compositeAngularCoordinates(first, second, computeSpinDerivatives, projectVelocityAndAcceleration));
    }

    /**
     * Build a transform from its primitive operations.
     *
     * @param dateIn
     *        date of the transform
     * @param cartesianIn
     *        position and velocity of the "destination" frame expressed in the "origin" one
     * @param angularIn
     *        {@link AngularCoordinates} of the "destination" frame in the "origin" one
     */
    public Transform(final AbsoluteDate dateIn, final PVCoordinates cartesianIn, final AngularCoordinates angularIn) {
        this.date = dateIn;
        this.cartesian = cartesianIn;
        this.angular = angularIn;
    }

    /**
     * Compute a composite translation.
     *
     * @param first
     *        first applied transform
     * @param second
     *        second applied transform
     * @param projectVelocityAndAcceleration
     *        true if velocity and acceleration should be simply projected, false otherwise
     * @return translation part of the composite transform
     */
    private static PVCoordinates compositePVCoordinates(final Transform first, final Transform second,
                                                        final boolean projectVelocityAndAcceleration) {


        // Initialization
        final Vector3D p1 = first.cartesian.getPosition();
        final Rotation r1 = first.angular.getRotation();
        final Vector3D p2 = second.cartesian.getPosition();
        final Vector3D v1 = first.cartesian.getVelocity();
        final Vector3D o1 = first.angular.getRotationRate();
        final Vector3D v2 = second.cartesian.getVelocity();
        final Vector3D a1 = first.cartesian.getAcceleration();
        final Vector3D oDot1 = first.angular.getRotationAcceleration();
        final Vector3D a2 = second.cartesian.getAcceleration();

        // Check data is zero for optimizations
        final boolean isP2Zero = p2.isZero();
        final boolean isO1Zero = o1.isZero();

        // Position
        final Vector3D p = composePositions(p1, r1, p2, isP2Zero);

        // Velocity
        final Vector3D v = composeVelocities(v1, r1, o1, p2, v2, projectVelocityAndAcceleration, isO1Zero, isP2Zero);

        // Acceleration (null if a1 or a2 are not provided)
        final Vector3D a = composeAccelerations(a1, r1, o1, oDot1, p2, v2, a2, projectVelocityAndAcceleration,
            isO1Zero, isP2Zero);

        // Return result
        return new PVCoordinates(p, v, a);
    }

    /**
     * Compose positions.
     *
     * @param p1 first position
     * @param r1 first rotation
     * @param p2 second position
     * @param isP2Zero boolean to specify whether the second position vector is a zero vector (true) or not (false)
     * 
     * @return the composed position
     */
    private static Vector3D composePositions(final Vector3D p1, final Rotation r1, final Vector3D p2,
                                             final boolean isP2Zero) {
        // Sum positions in the Transform 1 frame
        // p1 + M * p2
        return vectorComposition(p1, r1, p2, isP2Zero);
    }

    /**
     * Compose velocities.
     *
     * @param v1 first velocity
     * @param r1 first rotation
     * @param o1 first rotation rate
     * @param p2 second position
     * @param v2 second velocity
     * @param projectVelocity true if velocity should be simply projected, false otherwise
     * @param isO1Zero boolean to specify whether the first rotation rate vector is a zero vector (true) or not (false)
     * @param isP2Zero boolean to specify whether the second position vector is a zero vector (true) or not (false)
     * 
     * @return the composed velocity
     */
    private static Vector3D composeVelocities(final Vector3D v1, final Rotation r1, final Vector3D o1,
                                              final Vector3D p2, final Vector3D v2,
                                              final boolean projectVelocity, final boolean isO1Zero,
                                              final boolean isP2Zero) {
        final Vector3D v;
        // Sum velocities in the Transform 1 frame
        // v1 + M (v2 + v_frame)
        // v_frame = Omega ^ p2
        if (projectVelocity || isO1Zero || isP2Zero) {
            // Velocity is whether simply projected or v_frame is ZERO
            v = vectorComposition(v1, r1, v2, v2.isZero());
        } else {
            final Vector3D o1CrossP2 = Vector3D.crossProduct(o1, p2);
            v = vectorComposition(v1, r1, v2.add(o1CrossP2), false);
        }
        return v;
    }

    /**
     * Compose accelerations.
     *
     * @param a1 first acceleration
     * @param r1 first rotation
     * @param o1 first rotation rate
     * @param oDot1 first rotation acceleration
     * @param p2 second position
     * @param v2 second velocity
     * @param a2 second acceleration
     * @param projectAcceleration true if acceleration should be simply projected, false otherwise
     * @param isO1Zero boolean to specify whether the first rotation rate vector is a zero vector (true) or not (false)
     * @param isP2Zero boolean to specify whether the second position vector is a zero vector (true) or not (false)
     * 
     * @return the composed acceleration
     */
    //CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: computation time optimizations particularizing numerous null or ZERO cases
    private static Vector3D composeAccelerations(final Vector3D a1, final Rotation r1, final Vector3D o1,
                                                 final Vector3D oDot1,
                                                 final Vector3D p2, final Vector3D v2, final Vector3D a2,
                                                 final boolean projectAcceleration,
                                                 final boolean isO1Zero, final boolean isP2Zero) {
        //CHECKSTYLE: resume CyclomaticComplexity check

        // Sum accelerations in the Transform 1 frame
        // a1 + M (a2 + aCentrifugal + aEuler + aCoriolis)
        // aCentrifugal = Omega ^ (Omega ^ p2)
        // aEuler = Omega_dot ^ p2
        // aCoriolis = 2 Omega ^ v2

        final Vector3D a;
        if (a1 == null || a2 == null) {
            // At least one of the acceleration is unknown
            a = null;
        } else {
            // Both accelerations are known
            if (projectAcceleration) {
                // Accelerations should only be projected: aCentrifugal, aEuler, aCoriolis are ZERO
                a = vectorComposition(a1, r1, a2, a2.isZero());
            } else {
                // Acceleration composition should be calculated
                final Vector3D composedAcceleration;
                if (isO1Zero) {
                    // aCentrifugal and aCoriolis are ZERO
                    if (isP2Zero || oDot1 == null || oDot1.isZero()) {
                        // aEuler is ZERO
                        composedAcceleration = a2;
                    } else {
                        final Vector3D aEuler = Vector3D.crossProduct(oDot1, p2);
                        composedAcceleration = a2.add(aEuler);
                    }
                } else {
                    // o1 is not ZERO
                    if (isP2Zero) {
                        // aCentrifugal and aEuler are ZERO
                        if (v2.isZero()) {
                            // aCoriolis is ZERO
                            composedAcceleration = a2;
                        } else {
                            final Vector3D aCoriolis = Vector3D.crossProduct(o1, v2).scalarMultiply(2);
                            composedAcceleration = a2.add(aCoriolis);
                        }
                    } else {
                        // At this stage, aCentrifugal is not ZERO
                        final Vector3D aCentrifugal = Vector3D.crossProduct(o1, Vector3D.crossProduct(o1, p2));
                        if (v2.isZero()) {
                            // aCoriolis is ZERO
                            if (oDot1 == null || oDot1.isZero()) {
                                // aEuler is ZERO
                                composedAcceleration = a2.add(aCentrifugal);
                            } else {
                                final Vector3D aEuler = Vector3D.crossProduct(oDot1, p2);
                                composedAcceleration = new Vector3D(1, a2, 1, aCentrifugal, 1, aEuler);
                            }
                        } else {
                            // At this stage, aCoriolis is not ZERO
                            final Vector3D aCoriolis = Vector3D.crossProduct(o1, v2).scalarMultiply(2);
                            if (oDot1 == null || oDot1.isZero()) {
                                // aEuler is ZERO
                                composedAcceleration = new Vector3D(1, a2, 1, aCoriolis, 1, aCentrifugal);
                            } else {
                                final Vector3D aEuler = Vector3D.crossProduct(oDot1, p2);
                                composedAcceleration = new Vector3D(1, a2, 1, aCoriolis, 1, aCentrifugal, 1, aEuler);
                            }
                        }
                    }
                }
                a = vectorComposition(a1, r1, composedAcceleration, composedAcceleration.isZero());
            }
        }
        return a;
    }

    /**
     * Perform the vector composition a + r.applyTo(b)
     * <p>
     * Specific optimization when b is zero, avoids a rotation and an addition
     * </p>
     *
     * @param a first vector
     * @param r rotation
     * @param b second vector
     * @param isBZero boolean to specify whether the second vector is a zero vector (true) or not (false)
     * 
     * @return the vector composition
     */
    private static Vector3D vectorComposition(final Vector3D a, final Rotation r, final Vector3D b,
                                              final boolean isBZero) {
        final Vector3D c;
        if (isBZero) {
            c = a;
        } else {
            c = a.add(r.applyTo(b));
        }
        return c;
    }

    /**
     * Compute a composite angular coordinates.
     *
     * @param first
     *        first applied transform
     * @param second
     *        second applied transform
     * @param computeSpinDerivatives
     *        true if spin derivatives should be computed. If not, spin derivative is set to <i>null</i>
     * @param projectVelocityAndAcceleration
     *        true if velocity and acceleration should be simply projected, false otherwise
     * @return composite angular coordinates
     */
    private static AngularCoordinates compositeAngularCoordinates(final Transform first, final Transform second,
                                                                  final boolean computeSpinDerivatives,
                                                                  final boolean projectVelocityAndAcceleration) {

        final AngularCoordinates r1 = first.angular;
        final AngularCoordinates r2 = second.angular;
        final AngularCoordinates res = r1.addOffset(r2, computeSpinDerivatives);
        return new AngularCoordinates(res.getRotation(), res.getRotationRate(), res.getRotationAcceleration(),
            projectVelocityAndAcceleration);
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getDate() {
        return this.date;
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Warning: </b>spin derivative is not computed.
     * </p>
     */
    @Override
    public Transform shiftedBy(final double dt) {
        return new Transform(this.date.shiftedBy(dt), this.cartesian.shiftedBy(dt), this.angular.shiftedBy(dt));
    }

    /**
     * Get a time-shifted instance.
     *
     * @param dt
     *        time shift in seconds
     * @param computeSpinDerivative
     *        true if spin derivatives should be computed. If not, spin derivative is set to <i>null</i>
     * @return a new instance, shifted with respect to instance (which is not changed)
     */
    public Transform shiftedBy(final double dt, final boolean computeSpinDerivative) {
        return new Transform(this.date.shiftedBy(dt), this.cartesian.shiftedBy(dt), this.angular.shiftedBy(dt,
            computeSpinDerivative));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Calling this method is equivalent to call {@link #interpolate(AbsoluteDate, boolean, boolean, Collection)} with
     * both {@code useVelocities} and {@code useRotationRates} set to true.
     * </p>
     * <p>
     * <b>Warning: </b>spin derivative is not computed.
     * </p>
     */
    @Override
    public Transform interpolate(final AbsoluteDate interpolationDate,
                                 final Collection<Transform> sample) throws PatriusException {
        return interpolate(interpolationDate, true, true, sample, false);
    }

    /**
     * Get an interpolated instance.
     * <p>
     * Note that the state of the current instance may not be used in the interpolation process, only its type and non
     * interpolable fields are used (for example central attraction coefficient or frame when interpolating orbits). The
     * interpolable fields taken into account are taken only from the states of the sample points. So if the state of
     * the instance must be used, the instance should be included in the sample points.
     * </p>
     * <p>
     * Calling this method is equivalent to call {@link #interpolate(AbsoluteDate, boolean, boolean, Collection)} with
     * both {@code useVelocities} and {@code useRotationRates} set to true.
     * </p>
     *
     * @param interpolationDate
     *        interpolation date
     * @param sample
     *        sample points on which interpolation should be done
     * @param computeSpinDerivative
     *        true if spin derivatives should be computed. If not, spin derivative is set to <i>null</i>
     * @return a new instance, interpolated at specified date
     * @throws PatriusException
     *         if the sample points are inconsistent
     */
    public Transform interpolate(final AbsoluteDate interpolationDate, final Collection<Transform> sample,
                                 final boolean computeSpinDerivative) throws PatriusException {
        return interpolate(interpolationDate, true, true, sample, computeSpinDerivative);
    }

    /**
     * Interpolate a transform from a sample set of existing transforms.
     * <p>
     * Note that even if first time derivatives (velocities and rotation rates) from sample can be ignored, the
     * interpolated instance always includes interpolated derivatives. This feature can be used explicitly to compute
     * these derivatives when it would be too complex to compute them from an analytical formula: just compute a few
     * sample points from the explicit formula and set the derivatives to zero in these sample points, then use
     * interpolation to add derivatives consistent with the positions and rotations.
     * </p>
     *
     * <p>
     * <b>Warning: </b>spin derivative is not computed.
     * </p>
     *
     * @param date
     *        interpolation date
     * @param useVelocities
     *        if true, use sample transforms velocities, otherwise ignore them and use only positions
     * @param useRotationRates
     *        if true, use sample points rotation rates, otherwise ignore them and use only rotations
     * @param sample
     *        sample points on which interpolation should be done
     * @return a new instance, interpolated at specified date
     * @throws PatriusException
     *         OrekitException if the number of point is too small for interpolating
     */
    public static Transform
            interpolate(final AbsoluteDate date, final boolean useVelocities,
                        final boolean useRotationRates, final Collection<Transform> sample) throws PatriusException {
        return interpolate(date, useVelocities, useRotationRates, sample, false);
    }

    /**
     * Interpolate a transform from a sample set of existing transforms.
     * <p>
     * Note that even if first time derivatives (velocities and rotation rates) from sample can be ignored, the
     * interpolated instance always includes interpolated derivatives. This feature can be used explicitly to compute
     * these derivatives when it would be too complex to compute them from an analytical formula: just compute a few
     * sample points from the explicit formula and set the derivatives to zero in these sample points, then use
     * interpolation to add derivatives consistent with the positions and rotations.
     * </p>
     *
     * @param date
     *        interpolation date
     * @param useVelocities
     *        if true, use sample transforms velocities, otherwise ignore them and use only positions
     * @param useRotationRates
     *        if true, use sample points rotation rates, otherwise ignore them and use only rotations
     * @param sample
     *        sample points on which interpolation should be done
     * @param computeSpinDerivative
     *        true if spin derivatives should be computed. If not, spin derivative is set to <i>null</i>
     * @return a new instance, interpolated at specified date
     * @throws PatriusException
     *         if the number of point is too small for interpolating
     */
    public static Transform interpolate(final AbsoluteDate date, final boolean useVelocities,
                                        final boolean useRotationRates, final Collection<Transform> sample,
                                        final boolean computeSpinDerivative) throws PatriusException {

        // Initialization
        final List<TimeStampedPVCoordinates> datedPV = new ArrayList<>();
        final List<TimeStampedAngularCoordinates> datedAC = new ArrayList<>();

        // Retrieve data
        boolean isPVNul = true;
        for (final Transform transform : sample) {
            // Loop on samples
            final PVCoordinates pvCoord = transform.getCartesian();
            final Vector3D pos = pvCoord.getPosition();
            isPVNul &= pos.getX() == 0;
            isPVNul &= pos.getY() == 0;
            isPVNul &= pos.getZ() == 0;

            datedPV.add(new TimeStampedPVCoordinates(transform.getDate(), pvCoord));
            datedAC.add(new TimeStampedAngularCoordinates(transform.getDate(), transform.getAngular()));
        }

        // Perform interpolation
        PVCoordinates interpolatedPV = new PVCoordinates();
        if (!isPVNul) {
            final CartesianDerivativesFilter cdf = useVelocities ? CartesianDerivativesFilter.USE_PV
                : CartesianDerivativesFilter.USE_P;
            interpolatedPV = TimeStampedPVCoordinates.interpolate(date, cdf, datedPV);
        }
        final AngularDerivativesFilter adf = useRotationRates ? AngularDerivativesFilter.USE_RR
            : AngularDerivativesFilter.USE_R;
        final AngularCoordinates interpolatedAC = TimeStampedAngularCoordinates.interpolate(date, adf, datedAC,
            computeSpinDerivative);

        // Return result
        return new Transform(date, interpolatedPV, interpolatedAC);
    }

    /**
     * Get the inverse transform of the instance.
     *
     * @param computeSpinDerivatives
     *        true if spin derivatives should be computed. If not, spin derivative is set to <i>null</i>
     * @return inverse transform of the instance
     */
    public Transform getInverse(final boolean computeSpinDerivatives) {
        // Operations that should be done
        // pInv = - MInv * p;
        // vInv = - MInv * v + Omega ^ (MInv * p);
        // aInv = - MInv * a + 2 Omega ^ (MInv * v) - Omega ^ (Omega ^ (MInv * p)) + Omega_dot ^ (MInv * p)

        // Retrieve attributes

        final Rotation r = this.angular.getRotation();
        final Vector3D o = this.angular.getRotationRate();
        final Vector3D oDot = this.angular.getRotationAcceleration();

        final PVCoordinates pvInv;
        if (o.isZero()) {
            pvInv = reversePVWithoutRotationVelocity(this.cartesian, r, oDot);
        } else {
            pvInv = reversePVWithRotationVelocity(this.cartesian, r, o, oDot);
        }

        // Return result
        return new Transform(this.date, pvInv, this.angular.revert(computeSpinDerivatives));
    }

    /**
     * Reverse the position-velocity coordinates assuming the rotation rate of the transform is ZERO (leads to
     * simplified computation).
     *
     * @param pv position-velocity coordinates
     * @param r rotation
     * @param oDot rotation acceleration
     * 
     * @return reverse position-velocity coordinates without rotation velocity
     */
    private static PVCoordinates reversePVWithoutRotationVelocity(final PVCoordinates pv, final Rotation r,
                                                                  final Vector3D oDot) {
        // Operations that should be done
        // pInv = - MInv * p;
        // vInv = - MInv * v;
        // aInv = - MInv * a + Omega_dot ^ (MInv * p)

        final Vector3D p = pv.getPosition();
        final Vector3D v = pv.getVelocity();
        final Vector3D a = pv.getAcceleration();

        final Vector3D pInv;
        final Vector3D vInv;
        final Vector3D aInv;

        // Handle position and acceleration together since they both depend on the position
        if (p.isZero()) {
            // Position is ZERO
            pInv = Vector3D.ZERO;

            // Acceleration
            if (a == null) {
                aInv = null;
            } else if (a.isZero()) {
                aInv = Vector3D.ZERO;
            } else {
                aInv = r.applyInverseTo(a).negate();
            }
        } else {
            // Position is different from ZERO
            final Vector3D rp = r.applyInverseTo(p);
            pInv = rp.negate();

            // Acceleration
            if (a == null) {
                aInv = null;
            } else {
                final Vector3D crossDotP = oDot == null ? Vector3D.ZERO : Vector3D.crossProduct(oDot, rp);
                if (a.isZero()) {
                    aInv = crossDotP;
                } else {
                    final Vector3D ra = r.applyInverseTo(a);
                    aInv = crossDotP.subtract(ra);
                }
            }
        }

        // Handle velocity separately since it does not depend on the position
        if (v.isZero()) {
            // Velocity is ZERO
            vInv = Vector3D.ZERO;
        } else {
            // Velocity is different from ZERO
            vInv = r.applyInverseTo(v).negate();
        }

        return new PVCoordinates(pInv, vInv, aInv);
    }

    /**
     * Reverse the position-velocity coordinates knowing the rotation rate of the transform is not ZERO.
     *
     * @param pv position-velocity coordinates
     * @param r rotation
     * @param o rotation rate
     * @param oDot rotation acceleration
     * 
     * @return reverse position-velocity coordinates with rotation velocity
     */
    //CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: computation time optimizations particularizing numerous null or ZERO cases
    private static PVCoordinates reversePVWithRotationVelocity(final PVCoordinates pv, final Rotation r,
                                                               final Vector3D o,
                                                               final Vector3D oDot) {
        //CHECKSTYLE: resume CyclomaticComplexity check

        // Operations that should be done
        // pInv = - MInv * p;
        // vInv = - MInv * v + Omega ^ (MInv * p);
        // aInv = - MInv * a + 2 Omega ^ (MInv * v) - Omega ^ (Omega ^ (MInv * p)) + Omega_dot ^ (MInv * p)

        final Vector3D p = pv.getPosition();
        final Vector3D v = pv.getVelocity();
        final Vector3D a = pv.getAcceleration();

        final Vector3D pInv;
        final Vector3D vInv;
        final Vector3D aInv;

        if (p.isZero()) {
            // Operations that should be done
            // pInv = ZERO;
            // vInv = - MInv * v;
            // aInv = - MInv * a + 2 Omega ^ (MInv * v)

            // Position is ZERO
            pInv = Vector3D.ZERO;

            // Velocity and acceleration
            if (v.isZero()) {
                // Velocity is ZERO
                vInv = Vector3D.ZERO;

                // Acceleration
                if (a == null) {
                    aInv = null;
                } else if (a.isZero()) {
                    aInv = Vector3D.ZERO;
                } else {
                    aInv = r.applyInverseTo(a).negate();
                }
            } else {
                // Velocity is different from ZERO
                final Vector3D rv = r.applyInverseTo(v);
                vInv = rv.negate();

                // Acceleration
                if (a == null) {
                    aInv = null;
                } else {
                    final Vector3D crossV = Vector3D.crossProduct(o, rv);
                    if (a.isZero()) {
                        aInv = crossV.scalarMultiply(2);
                    } else {
                        final Vector3D ra = r.applyInverseTo(a);
                        aInv = new Vector3D(-1, ra, 2, crossV);
                    }
                }
            }
        } else {
            // Position is different from ZERO
            final Vector3D rp = r.applyInverseTo(p);
            pInv = rp.negate();

            // Velocity and acceleration
            final Vector3D crossP = Vector3D.crossProduct(o, rp);
            if (v.isZero()) {
                // Operations that should be done
                // vInv = Omega ^ (MInv * p);
                // aInv = - MInv * a - Omega ^ (Omega ^ (MInv * p)) + Omega_dot ^ (MInv * p)

                // Velocity is ZERO
                vInv = crossP;

                // Acceleration
                if (a == null) {
                    aInv = null;
                } else {
                    final Vector3D crossDotP = oDot == null ? Vector3D.ZERO : Vector3D.crossProduct(oDot, rp);
                    final Vector3D crossCrossP = Vector3D.crossProduct(o, crossP);
                    if (a.isZero()) {
                        aInv = crossDotP.subtract(crossCrossP);
                    } else {
                        final Vector3D ra = r.applyInverseTo(a);
                        aInv = new Vector3D(-1, ra, 1, crossDotP, -1, crossCrossP);
                    }
                }
            } else {
                // Velocity is different from ZERO
                final Vector3D rv = r.applyInverseTo(v);
                vInv = crossP.subtract(rv);

                // Acceleration
                if (a == null) {
                    aInv = null;
                } else {
                    // Operations that should be done
                    // aInv = - MInv * a + 2 Omega ^ (MInv * v) - Omega ^ (Omega ^ (MInv * p)) + Omega_dot ^ (MInv * p)
                    final Vector3D crossV = Vector3D.crossProduct(o, rv);
                    final Vector3D crossDotP = oDot == null ? Vector3D.ZERO : Vector3D.crossProduct(oDot, rp);
                    final Vector3D crossCrossP = Vector3D.crossProduct(o, crossP);
                    if (a.isZero()) {
                        aInv = new Vector3D(2, crossV, 1, crossDotP, -1, crossCrossP);
                    } else {
                        final Vector3D ra = r.applyInverseTo(a);
                        aInv = new Vector3D(-1, ra, 2, crossV, 1, crossDotP, -1, crossCrossP);
                    }
                }
            }
        }
        return new PVCoordinates(pInv, vInv, aInv);
    }

    /**
     * Get the inverse transform of the instance.
     * <p>
     * <b>Warning: </b>spin derivative is not computed.
     * </p>
     *
     * @return inverse transform of the instance
     */
    public Transform getInverse() {
        return this.getInverse(false);
    }

    /**
     * Get a freezed transform.
     * <p>
     * This method creates a copy of the instance but frozen in time, i.e. with velocity, rotation rate, acceleration
     * and rotation acceleration set to zero.
     * </p>
     *
     * @return a new transform, without any time-dependent parts
     */
    public Transform freeze() {
        final Vector3D acc = this.angular.getRotationAcceleration() == null ? null : Vector3D.ZERO;
        return new Transform(this.date, new PVCoordinates(this.cartesian.getPosition(), Vector3D.ZERO),
            new AngularCoordinates(
                this.angular.getRotation(), Vector3D.ZERO, acc));
    }

    /**
     * Transform a position vector (including translation effects).
     *
     * @param position
     *        vector to transform
     * @return transformed position
     */
    public Vector3D transformPosition(final Vector3D position) {
        return this.angular.getRotation().applyInverseTo(position.subtract(this.cartesian.getPosition()));
    }

    /**
     * Transform a vector (ignoring translation effects).
     *
     * @param vector
     *        vector to transform
     * @return transformed vector
     */
    public Vector3D transformVector(final Vector3D vector) {
        return this.angular.getRotation().applyInverseTo(vector);
    }

    /**
     * Transform a wrench (ignoring translation effects).
     *
     * @param wrench
     *        wrench to transform
     * @return transformed wrench
     */
    public Wrench transformWrench(final Wrench wrench) {
        final Vector3D newOrigin = this.transformPosition(wrench.getOrigin());
        final Vector3D newForce = this.transformVector(wrench.getForce());
        final Vector3D newTorque = this.transformVector(wrench.getTorque());
        return new Wrench(newOrigin, newForce, newTorque);
    }

    /**
     * Transform a line from an algebraic point of view.
     *
     * @param line
     *        to transform
     * @return transformed line
     */
    public Line transformLine(final Line line) {
        final Vector3D transformedP0 = this.transformPosition(line.getOrigin());
        final Vector3D transformedP1 = this.transformPosition(line.pointAt(1.0e14));
        final Vector3D transformedPointMinAbscissa = this.transformPosition(line.pointAt(line.getMinAbscissa()));
        return new Line(transformedP0, transformedP1, transformedPointMinAbscissa);
    }

    /**
     * Transform {@link PVCoordinates} including kinematic effects from an algebraic point of view.
     *
     * @param pv
     *        the couple position-velocity to transform.
     * @return transformed position/velocity
     */
    public PVCoordinates transformPVCoordinates(final PVCoordinates pv) {
        return this.angular.applyTo(new PVCoordinates(1, pv, -1, this.cartesian));
    }

    /**
     * Transform {@link TimeStampedPVCoordinates} including kinematic effects.
     * <p>
     * In order to allow the user more flexibility, this method does <em>not</em> check for consistency between the
     * transform {@link #getDate() date} and the time-stamped position-velocity
     * {@link TimeStampedPVCoordinates#getDate() date}. The returned value will always have the same
     * {@link TimeStampedPVCoordinates#getDate() date} as the input argument, regardless of the instance
     * {@link #getDate() date}.
     * </p>
     *
     * @param pv
     *        time-stamped position-velocity to transform.
     * @return transformed time-stamped position-velocity
     * @since 7.0
     */
    public TimeStampedPVCoordinates transformPVCoordinates(final TimeStampedPVCoordinates pv) {
        return this.angular.applyTo(new TimeStampedPVCoordinates(pv.getDate(), 1, pv, -1, this.cartesian));
    }

    /**
     * Compute the Jacobian of the {@link #transformPVCoordinates(PVCoordinates)} method of the transform.
     * <p>
     * Element {@code jacobian[i][j]} is the derivative of Cartesian coordinate i of the transformed
     * {@link PVCoordinates} with respect to Cartesian coordinate j of the input {@link PVCoordinates} in method
     * {@link #transformPVCoordinates(PVCoordinates)}.
     * </p>
     * <p>
     * This definition implies that if we define position-velocity coordinates
     *
     * <pre>
     * PV<sub>1</sub> = transform.transformPVCoordinates(PV<sub>0</sub>), then
     * </pre>
     *
     * their differentials dPV<sub>1</sub> and dPV<sub>0</sub> will obey the following relation where J is the matrix
     * computed by this method:<br/>
     *
     * <pre>
     * dPV<sub>1</sub> = J &times; dPV<sub>0</sub>
     * </pre>
     *
     * </p>
     *
     * @param jacobian
     *        placeholder 6x6 (or larger) matrix to be filled with the Jacobian, if matrix is larger than 6x6, only
     *        the 6x6 upper left corner will be modified
     */
    public void getJacobian(final double[][] jacobian) {

        // elementary matrix for rotation
        final double[][] mData = this.angular.getRotation().revert().getMatrix();

        // dP1/dP0
        System.arraycopy(mData[0], 0, jacobian[0], 0, 3);
        System.arraycopy(mData[1], 0, jacobian[1], 0, 3);
        System.arraycopy(mData[2], 0, jacobian[2], 0, 3);

        // dP1/dV0
        Arrays.fill(jacobian[0], 3, 6, 0.0);
        Arrays.fill(jacobian[1], 3, 6, 0.0);
        Arrays.fill(jacobian[2], 3, 6, 0.0);

        // dV1/dP0
        final Vector3D o = this.angular.getRotationRate();
        final double mOx = -o.getX();
        final double mOy = -o.getY();
        final double mOz = -o.getZ();
        for (int i = 0; i < 3; ++i) {
            jacobian[3][i] = mOy * mData[2][i] - mOz * mData[1][i];
            jacobian[4][i] = mOz * mData[0][i] - mOx * mData[2][i];
            jacobian[5][i] = mOx * mData[1][i] - mOy * mData[0][i];
        }

        // dV1/dV0
        System.arraycopy(mData[0], 0, jacobian[3], 3, 3);
        System.arraycopy(mData[1], 0, jacobian[4], 3, 3);
        System.arraycopy(mData[2], 0, jacobian[5], 3, 3);

    }

    /**
     * Get the position and velocity of the "destination" frame in the "origin" one.
     * <p>
     * NB : A transform can be uniquely represented as an elementary translation followed by an elementary rotation.
     * This method returns this unique elementary translation with its derivative.
     * </p>
     *
     * @return elementary cartesian part
     * @see #getTranslation()
     * @see #getVelocity()
     *
     */
    public PVCoordinates getCartesian() {
        return this.cartesian;
    }

    /**
     * Get the position of the "destination" frame in the "origin" one.
     * <p>
     * NB : A transform can be uniquely represented as an elementary translation followed by an elementary rotation.
     * This method returns this unique elementary translation.
     * </p>
     *
     * @return elementary translation
     * @see #getCartesian()
     * @see #getVelocity()
     *
     */
    public Vector3D getTranslation() {
        return this.cartesian.getPosition();
    }

    /**
     * Get the velocity of the "destination" frame in the "origin" one.
     *
     * @return first time derivative of the translation
     * @see #getCartesian()
     * @see #getTranslation()
     *
     */
    public Vector3D getVelocity() {
        return this.cartesian.getVelocity();
    }

    /**
     * Get the second time derivative of the translation.
     *
     * @return second time derivative of the translation
     * @see #getCartesian()
     * @see #getTranslation()
     * @see #getVelocity()
     */
    public Vector3D getAcceleration() {
        return this.cartesian.getAcceleration();
    }

    /**
     * Get the orientation and rotation rate of the "destination" frame in the "origin" one.
     * <p>
     * NB : A transform can be uniquely represented as an elementary translation followed by an elementary rotation.
     * This method returns this unique elementary rotation with its derivative.
     * </p>
     *
     * @return elementary angular coordinates
     * @see #getRotation()
     * @see #getRotationRate()
     */
    public AngularCoordinates getAngular() {
        return this.angular;
    }

    /**
     * Get the orientation of the "destination" frame in the "origin" one.
     * <p>
     * NB : A transform can be uniquely represented as an elementary translation followed by an elementary rotation.
     * This method returns this unique elementary rotation.
     * </p>
     *
     * @return underlying elementary rotation
     * @see #getAngular()
     * @see #getRotationRate()
     */
    public Rotation getRotation() {
        return this.angular.getRotation();
    }

    /**
     * Get the rotation rate of the "destination" frame in the "origin" one.
     * <p>
     * The norm represents the angular rate.
     * </p>
     *
     * @return First time derivative of the rotation
     * @see #getAngular()
     * @see #getRotation()
     */
    public Vector3D getRotationRate() {
        return this.angular.getRotationRate();
    }

    /**
     * Get the second time derivative of the rotation. May be <i>null</i>
     *
     * @return Second time derivative of the rotation
     * @see #getAngular()
     * @see #getRotation()
     * @see #getRotationRate()
     */
    public Vector3D getRotationAcceleration() {
        return this.angular.getRotationAcceleration();
    }

    /** Specialized class for identity transform. */
    private static class IdentityTransform extends Transform {

        /** Serializable UID. */
        private static final long serialVersionUID = -9042082036141830517L;

        /** Simple constructor. */
        public IdentityTransform() {
            super(AbsoluteDate.J2000_EPOCH, PVCoordinates.ZERO, AngularCoordinates.IDENTITY);
        }

        /** {@inheritDoc} */
        @Override
        public Transform shiftedBy(final double dt) {
            return this;
        }

        /** {@inheritDoc} */
        @Override
        public Transform getInverse() {
            return this;
        }

        /** {@inheritDoc} */
        @Override
        public Transform getInverse(final boolean computeSpinDerivatives) {
            return this;
        }

        /** {@inheritDoc} */
        @Override
        public Vector3D transformPosition(final Vector3D position) {
            return position;
        }

        /** {@inheritDoc} */
        @Override
        public Vector3D transformVector(final Vector3D vector) {
            return vector;
        }

        /** {@inheritDoc} */
        @Override
        public Line transformLine(final Line line) {
            return line;
        }

        /** {@inheritDoc} */
        @Override
        public PVCoordinates transformPVCoordinates(final PVCoordinates pv) {
            return pv;
        }

        /** {@inheritDoc} */
        @Override
        public void getJacobian(final double[][] jacobian) {
            for (int i = 0; i < 6; ++i) {
                Arrays.fill(jacobian[i], 0, 6, 0.0);
                jacobian[i][i] = 1.0;
            }
        }
    }
}
