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
* VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
* VERSION:4.8:FA:FA-2959:15/11/2021:[PATRIUS] Levee d'exception NullPointerException lors du calcul d'intersection a altitude
 * VERSION:4.5:FA:FA-2464:27/05/2020:Anomalie dans le calcul du vecteur rotation des LOF
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
* VERSION:4.3:DM:DM-2089:15/05/2019:[PATRIUS] passage a Java 8
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:319:05/03/2015:Corrected Rotation class (Step1)
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:489:06/10/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.utils;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.exception.MathArithmeticException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathArrays;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.TimeStampedPVCoordinates;
import fr.cnes.sirius.patrius.time.TimeShiftable;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Simple container for rotation/rotation rate/rotation acceleration triplet.
 * 
 * <p>
 * When applied to frames, the rotation here describes the orientation of the frame of interest in the reference frame.
 * This means that defining X_ref=(1,0,0) in the reference frame, the vector X_interest (X axis of the frame of
 * interest, still expressed in the reference frame) is obtained by : rotation.applyTo(X_ref).
 * </p>
 * 
 * <p>
 * The rotation rate (respectively the rotation acceleration) is the vector describing the rotation velocity (rotation
 * acceleration) of the frame of interest relatively to the reference frame. This rotation rate (rotation acceleration)
 * vector is always expressed in the frame of interest.
 * </p>
 * 
 * <p>
 * The state can be slightly shifted to close dates. This shift is based on an approximate solution of the fixed
 * acceleration motion. It is <em>not</em> intended as a replacement for proper attitude propagation but should be
 * sufficient for either small time shifts or coarse accuracy.
 * </p>
 * <p>
 * This class is the angular counterpart to {@link PVCoordinates}.
 * </p>
 * <p>
 * Instances of this class are guaranteed to be immutable.
 * </p>
 * 
 * @author Luc Maisonobe
 */
@SuppressWarnings({"PMD.NullAssignment", "PMD.ConstructorCallsOverridableMethod"})
public class AngularCoordinates implements TimeShiftable<AngularCoordinates>, Serializable {

    /**
     * Fixed orientation parallel with reference frame (identity rotation, zero rotation rate, zero rotation
     * acceleration).
     */
    public static final AngularCoordinates IDENTITY =
        new AngularCoordinates(Rotation.IDENTITY, Vector3D.ZERO, Vector3D.ZERO);

    /** Serializable UID. */
    private static final long serialVersionUID = 3750363056414336775L;

    /** Rotation. */
    private final Rotation rotation;

    /** Rotation rate. */
    private final Vector3D rotationRate;

    /** Rotation acceleration. */
    private final Vector3D rotationAcceleration;

    /**
     * Simple constructor.
     * <p>
     * Sets the rotation/rotation rate and acceleration to default : Identity (0 0 0).
     * </p>
     */
    public AngularCoordinates() {
        this.rotation = Rotation.IDENTITY;
        this.rotationRate = Vector3D.ZERO;
        this.rotationAcceleration = Vector3D.ZERO;
    }

    /**
     * Builds a rotation/rotation rate triplet (acceleration set to {@link Vector3D#ZERO}).
     * <p>
     * The rotation here describes the orientation of a frame of interest in a reference frame.
     * </p>
     * <p>
     * The rotation rate (rotation acceleration) is the vector describing the rotation velocity (rotation acceleration)
     * of the frame of interest relatively to the reference frame. This rotation rate (rotation acceleration) vector is
     * expressed in the frame of interest.
     * <p>
     * 
     * @param rotationIn
     *        rotation
     * @param rotationRateIn
     *        rotation rate (rad/s)
     */
    public AngularCoordinates(final Rotation rotationIn, final Vector3D rotationRateIn) {
        this(rotationIn, rotationRateIn, Vector3D.ZERO);
    }

    /**
     * Builds a rotation/rotation rate/rotation acceleration triplet.
     * 
     * @param rotationIn
     *        rotation
     * @param rotationRateIn
     *        rotation rate Ω (rad/s)
     * @param rotationAccelerationIn
     *        rotation acceleration dΩ/dt (rad²/s²)
     */
    public AngularCoordinates(final Rotation rotationIn,
        final Vector3D rotationRateIn, final Vector3D rotationAccelerationIn) {
        this.rotation = rotationIn;
        this.rotationRate = rotationRateIn;
        this.rotationAcceleration = rotationAccelerationIn;
    }

    /**
     * Build the rotation that transforms a pair of pv coordinates into another one.
     * <p>
     * <em>WARNING</em>! This method requires much more stringent assumptions on its parameters than the similar
     * {@link Rotation#Rotation(Vector3D, Vector3D, Vector3D, Vector3D) constructor} from the {@link Rotation Rotation}
     * class. As a reminder, the rotation r describes the orientation of the frame of interest in the reference frame As
     * far as the Rotation constructor is concerned, the {@code v₂} vector from the second pair can be slightly
     * misaligned. The Rotation constructor will compensate for this misalignment and create a rotation that ensure
     * {@code v₁ = r(u₁)} and {@code v₂ ∈ plane (r(u₁), r(u₂))}. <em>THIS IS NOT
     * TRUE ANYMORE IN THIS CLASS</em>! As derivatives are involved and must be preserved, this constructor works
     * <em>only</em> if the two pairs are fully consistent, i.e. if a rotation exists that fulfill all the requirements:
     * {@code v₁ = r(u₁)}, {@code v₂ = r(u₂)}, {@code dv₁/dt = dr(u₁)/dt}, {@code dv₂/dt
     * = dr(u₂)/dt}, {@code d²v₁/dt² = d²r(u₁)/dt²}, {@code d²v₂/dt² = d²r(u₂)/dt²}.
     * </p>
     * 
     * @param u1
     *        first vector of the origin pair
     * @param u2
     *        second vector of the origin pair
     * @param v1
     *        desired image of u1 by the rotation
     * @param v2
     *        desired image of u2 by the rotation
     * @param tolerance
     *        relative tolerance factor used to check singularities
     * @param spinDerivativesComputation
     *        true if the spin derivative has to be computed. If not, spin derivative is set to <i>null</i>
     * @exception PatriusException
     *            if the vectors are inconsistent for the
     *            rotation to be found (null, aligned, ...)
     */
    public AngularCoordinates(final PVCoordinates u1, final PVCoordinates u2,
        final PVCoordinates v1, final PVCoordinates v2,
        final double tolerance, final boolean spinDerivativesComputation) throws PatriusException {

        try {
            this.rotation = new Rotation(u1.getPosition(), u2.getPosition(),
                v1.getPosition(), v2.getPosition()).revert();

            // find rotation rate Ω such that
            // Ω ⨯ v₁ = inv(r)(dot(u₁)) - dot(v₁)
            // Ω ⨯ v₂ = inv(r)(dot(u₂)) - dot(v₂)
            final Vector3D ru1Dot = this.rotation.applyInverseTo(u1.getVelocity());
            final Vector3D ru2Dot = this.rotation.applyInverseTo(u2.getVelocity());
            this.rotationRate = Vector3D.inverseCrossProducts(v1.getPosition(), ru1Dot.subtract(v1.getVelocity()),
                v2.getPosition(), ru2Dot.subtract(v2.getVelocity()),
                tolerance);

            if (spinDerivativesComputation) {
                // find rotation acceleration dot(Ω) such that
                // dot(Ω) ⨯ v₁ = inv(r)(dotdot(u₁)) - 2 Ω ⨯ dot(v₁) - Ω ⨯ (Ω ⨯ v₁) - dotdot(v₁)
                // dot(Ω) ⨯ v₂ = inv(r)(dotdot(u₂)) - 2 Ω ⨯ dot(v₂) - Ω ⨯ (Ω ⨯ v₂) - dotdot(v₂)
                final Vector3D ru1DotDot = this.rotation.applyInverseTo(u1.getAcceleration() == null
                        ? Vector3D.ZERO : u1.getAcceleration());
                final Vector3D oDotv1 = Vector3D.crossProduct(this.rotationRate, v1.getVelocity());
                final Vector3D oov1 = Vector3D.crossProduct(this.rotationRate,
                    Vector3D.crossProduct(this.rotationRate, v1.getPosition()));
                final Vector3D c1 = new Vector3D(1, ru1DotDot, -2, oDotv1, -1, oov1, -1, v1.getAcceleration() == null
                        ? Vector3D.ZERO : v1.getAcceleration());
                final Vector3D ru2DotDot = this.rotation.applyInverseTo(u2.getAcceleration() == null
                        ? Vector3D.ZERO : u2.getAcceleration());
                final Vector3D oDotv2 = Vector3D.crossProduct(this.rotationRate, v2.getVelocity());
                final Vector3D oov2 = Vector3D.crossProduct(this.rotationRate,
                    Vector3D.crossProduct(this.rotationRate, v2.getPosition()));
                final Vector3D c2 = new Vector3D(1, ru2DotDot, -2, oDotv2, -1, oov2, -1, v2.getAcceleration() == null
                        ? Vector3D.ZERO : v2.getAcceleration());
                this.rotationAcceleration = Vector3D.inverseCrossProducts(v1.getPosition(), c1, v2.getPosition(), c2,
                        tolerance);
            } else {
                this.rotationAcceleration = null;
            }

        } catch (final MathIllegalArgumentException miae) {
            throw new PatriusException(miae);
        } catch (final MathArithmeticException mae) {
            throw new PatriusException(mae);
        }

    }

    /**
     * Build the rotation that transforms a pair of pv coordinates into another one.
     * <p>
     * <em>WARNING</em>! This method requires much more stringent assumptions on its parameters than the similar
     * {@link Rotation#Rotation(Vector3D, Vector3D, Vector3D, Vector3D) constructor} from the {@link Rotation Rotation}
     * class. As a reminder, the rotation r describes the orientation of the frame of interest in the reference frame As
     * far as the Rotation constructor is concerned, the {@code v₂} vector from the second pair can be slightly
     * misaligned. The Rotation constructor will compensate for this misalignment and create a rotation that ensure
     * {@code v₁ = r(u₁)} and {@code v₂ ∈ plane (r(u₁), r(u₂))}. <em>THIS IS NOT
     * TRUE ANYMORE IN THIS CLASS</em>! As derivatives are involved and must be preserved, this constructor works
     * <em>only</em> if the two pairs are fully consistent, i.e. if a rotation exists that fulfill all the requirements:
     * {@code v₁ = r(u₁)}, {@code v₂ = r(u₂)}, {@code dv₁/dt = dr(u₁)/dt}, {@code dv₂/dt
     * = dr(u₂)/dt}, {@code d²v₁/dt² = d²r(u₁)/dt²}, {@code d²v₂/dt² = d²r(u₂)/dt²}.
     * </p>
     * <p>
     * <b>Warning: </b>spin derivative is not computed.
     * </p>
     * 
     * @param u1
     *        first vector of the origin pair
     * @param u2
     *        second vector of the origin pair
     * @param v1
     *        desired image of u1 by the rotation
     * @param v2
     *        desired image of u2 by the rotation
     * @param tolerance
     *        relative tolerance factor used to check singularities
     * @exception PatriusException
     *            if the vectors are inconsistent for the
     *            rotation to be found (null, aligned, ...)
     */
    public AngularCoordinates(final PVCoordinates u1, final PVCoordinates u2,
        final PVCoordinates v1, final PVCoordinates v2,
        final double tolerance) throws PatriusException {
        this(u1, u2, v1, v2, tolerance, false);
    }

    /**
     * Estimate rotation rate between two orientations.
     * <p>
     * Estimation is based on a simple fixed rate rotation during the time interval between the two orientations.
     * </p>
     * <p>
     * Those two orientation must be expressed in the same frame.
     * </p>
     * 
     * @param start
     *        start orientation
     * @param end
     *        end orientation
     * @param dt
     *        time elapsed between the dates of the two orientations
     * @return rotation rate allowing to go from start to end orientations
     */
    public static Vector3D estimateRate(final Rotation start, final Rotation end, final double dt) {
        final Rotation evolution = end.applyTo(start.revert());
        return new Vector3D(evolution.getAngle() / dt, start.applyInverseTo(evolution.getAxis()));
    }

    /**
     * Revert a rotation/rotation rate/rotation acceleration triplet.
     * Build a triplet which reverse the effect of another triplet.
     * 
     * @param computeSpinDerivatives
     *        true if spin derivatives must be computed. If not, spin derivative is set to <i>null</i>
     * @return a new triplet whose effect is the reverse of the effect
     *         of the instance
     */
    public AngularCoordinates revert(final boolean computeSpinDerivatives) {
        Vector3D acc = null;
        if (computeSpinDerivatives && this.rotationAcceleration != null) {
            acc = this.rotation.applyTo(this.rotationAcceleration.negate());
        }
        return new AngularCoordinates(this.rotation.revert(), this.rotation.applyTo(this.rotationRate.negate()), acc);
    }

    /**
     * Revert a rotation/rotation rate/rotation acceleration triplet.
     * Build a triplet which reverse the effect of another triplet.
     * <p>
     * <b>Warning: </b>spin derivative is not computed.
     * </p>
     * 
     * @return a new triplet whose effect is the reverse of the effect
     *         of the instance
     */
    public AngularCoordinates revert() {
        return this.revert(false);
    }

    /**
     * Get a time-shifted state.
     * <p>
     * The state can be slightly shifted to close dates. This shift is based on an approximate solution of the fixed
     * acceleration motion. It is <em>not</em> intended as a replacement for proper attitude propagation but should be
     * sufficient for either small time shifts or coarse accuracy.
     * </p>
     * <p>
     * <b>Warning: </b>spin derivative is not computed.
     * </p>
     * 
     * @param dt
     *        time shift in seconds
     * @return a new state, shifted with respect to the instance (which is immutable)
     */
    @Override
    public AngularCoordinates shiftedBy(final double dt) {
        return this.shiftedBy(dt, false);
    }

    /**
     * Get a time-shifted state.
     * <p>
     * The state can be slightly shifted to close dates. This shift is based on an approximate solution of the fixed
     * acceleration motion. It is <em>not</em> intended as a replacement for proper attitude propagation but should be
     * sufficient for either small time shifts or coarse accuracy.
     * </p>
     * 
     * @param dt
     *        time shift in seconds
     * @param computeSpinDerivatives
     *        true if spin derivatives should be computed. If not, spin derivative is set to <i>null</i>
     * @return a new state, shifted with respect to the instance (which is immutable)
     */
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Orekit code kept as such
    public AngularCoordinates shiftedBy(final double dt, final boolean computeSpinDerivatives) {
        // CHECKSTYLE: resume ReturnCount check
        final double rate = this.rotationRate.getNorm();
        if (rate == 0.0) {
            // special case for fixed rotations
            return this;
        }

        final Rotation evolution = new Rotation(this.rotation.applyTo(this.rotationRate), rate * dt);

        // linear part
        final AngularCoordinates linearPart =
            new AngularCoordinates(evolution.applyTo(this.rotation), this.rotationRate,
                computeSpinDerivatives ? Vector3D.ZERO : null);

        final double acc = computeSpinDerivatives ? this.rotationAcceleration.getNorm() : 0.;
        if (acc == 0.0) {
            // no acceleration, the linear part is sufficient
            return linearPart;
        }

        // At this point, acceleration cannot be [0; 0; 0] or null

        // compute the quadratic contribution, ignoring initial rotation and rotation rate
        final Vector3D res1 = this.rotation.applyTo(this.rotationAcceleration);
        final AngularCoordinates quadraticContribution =
            new AngularCoordinates(new Rotation(res1, 0.5 * acc * dt * dt),
                new Vector3D(dt, res1), new Vector3D(1, res1));

        // the quadratic contribution is a small rotation:
        // its initial angle and angular rate are both zero.
        // small rotations are almost commutative, so we append the small
        // quadratic part after the linear part as a simple offset
        return quadraticContribution.addOffset(linearPart, computeSpinDerivatives);
    }

    /**
     * Get the rotation.
     * 
     * @return the rotation.
     */
    public Rotation getRotation() {
        return this.rotation;
    }

    /**
     * Get the rotation rate.
     * 
     * @return the rotation rate vector (rad/s).
     */
    public Vector3D getRotationRate() {
        return this.rotationRate;
    }

    /**
     * Get the rotation acceleration.
     * 
     * @return the rotation acceleration vector dΩ/dt (rad²/s²). May be null if not computed at some point
     */
    public Vector3D getRotationAcceleration() {
        return this.rotationAcceleration;
    }

    /**
     * Add an offset from the instance.
     * <p>
     * The instance rotation is applied first and the offset is applied afterward. Note that angular coordinates do
     * <em>not</em> commute under this operation, i.e. {@code a.addOffset(b)} and {@code b.addOffset(a)} lead to
     * <em>different</em> results in most cases.
     * </p>
     * <p>
     * The rotation of the angular coordinates returned here is a composition of R_instance first and then R_offset. But
     * to compose them, we first have to express them in the same frame : R_offset has to be expressed in the reference
     * frame of the instance. So it becomes : R_instance o R_offset o R_instance^-1. The total composed rotation is then
     * : (R_instance o R_offset o R_instance^-1) o R_instance, wich can be simplified as R_instance o R_offset.
     * </p>
     * <p>
     * The two methods {@link #addOffset(AngularCoordinates) addOffset} and {@link #subtractOffset(AngularCoordinates)
     * subtractOffset} are designed so that round trip applications are possible. This means that both
     * {@code ac1.subtractOffset(ac2).addOffset(ac2)} and {@code ac1.addOffset(ac2).subtractOffset(ac2)} return angular
     * coordinates equal to ac1.
     * </p>
     * 
     * @param offset
     *        offset to subtract
     * @param computeSpinDerivatives
     *        true if spin derivatives must be computed. If not, spin derivative is set to <i>null</i>
     * @return new instance, with offset subtracted
     * @see #subtractOffset(AngularCoordinates)
     */
    public AngularCoordinates addOffset(final AngularCoordinates offset, final boolean computeSpinDerivatives) {
        final Vector3D rOmega = offset.getRotation().applyInverseTo(this.rotationRate);
        final Rotation newRot = this.rotation.applyTo(offset.rotation);
        final Vector3D newSpin = offset.rotationRate.add(rOmega);
        Vector3D newAcc = null;

        if (computeSpinDerivatives) {
            final Vector3D rOmegaDot = this.rotationAcceleration == null ? Vector3D.ZERO :
                offset.getRotation().applyInverseTo(this.rotationAcceleration);
            final Vector3D offsetAcc = offset.getRotationAcceleration() == null ?
                Vector3D.ZERO : offset.getRotationAcceleration();
            newAcc = new Vector3D(1.0, offsetAcc,
                1.0, rOmegaDot,
                -1.0, Vector3D.crossProduct(offset.getRotationRate(), rOmega));
        }
        return new AngularCoordinates(newRot, newSpin, newAcc);
    }

    /**
     * Add an offset from the instance.
     * <p>
     * The instance rotation is applied first and the offset is applied afterward. Note that angular coordinates do
     * <em>not</em> commute under this operation, i.e. {@code a.addOffset(b)} and {@code b.addOffset(a)} lead to
     * <em>different</em> results in most cases.
     * </p>
     * <p>
     * The rotation of the angular coordinates returned here is a composition of R_instance first and then R_offset. But
     * to compose them, we first have to express them in the same frame : R_offset has to be expressed in the reference
     * frame of the instance. So it becomes : R_instance o R_offset o R_instance^-1. The total composed rotation is then
     * : (R_instance o R_offset o R_instance^-1) o R_instance, wich can be simplified as R_instance o R_offset.
     * </p>
     * <p>
     * The two methods {@link #addOffset(AngularCoordinates) addOffset} and {@link #subtractOffset(AngularCoordinates)
     * subtractOffset} are designed so that round trip applications are possible. This means that both
     * {@code ac1.subtractOffset(ac2).addOffset(ac2)} and {@code ac1.addOffset(ac2).subtractOffset(ac2)} return angular
     * coordinates equal to ac1.
     * </p>
     * <p>
     * <b>Warning: </b>spin derivative is not computed.
     * </p>
     * 
     * @param offset
     *        offset to subtract
     * @return new instance, with offset subtracted
     * @see #subtractOffset(AngularCoordinates)
     */
    public AngularCoordinates addOffset(final AngularCoordinates offset) {
        return this.addOffset(offset, false);
    }

    /**
     * Subtract an offset from the instance.
     * <p>
     * The instance rotation is applied first and the offset is applied afterward. Note that angular coordinates do
     * <em>not</em> commute under this operation, i.e. {@code a.subtractOffset(b)} and {@code b.subtractOffset(a)} lead
     * to <em>different</em> results in most cases.
     * </p>
     * <p>
     * The two methods {@link #addOffset(AngularCoordinates) addOffset} and {@link #subtractOffset(AngularCoordinates)
     * subtractOffset} are designed so that round trip applications are possible. This means that both
     * {@code ac1.subtractOffset(ac2).addOffset(ac2)} and {@code ac1.addOffset(ac2).subtractOffset(ac2)} return angular
     * coordinates equal to ac1.
     * </p>
     * 
     * @param offset
     *        offset to subtract
     * @param computeSpinDerivatives
     *        true if spin derivatives must be computed If not, spin derivative is set to <i>null</i>
     * @return new instance, with offset subtracted
     * @see #addOffset(AngularCoordinates)
     */
    public AngularCoordinates subtractOffset(final AngularCoordinates offset, final boolean computeSpinDerivatives) {
        return this.addOffset(offset.revert(computeSpinDerivatives), computeSpinDerivatives);
    }

    /**
     * Subtract an offset from the instance.
     * <p>
     * The instance rotation is applied first and the offset is applied afterward. Note that angular coordinates do
     * <em>not</em> commute under this operation, i.e. {@code a.subtractOffset(b)} and {@code b.subtractOffset(a)} lead
     * to <em>different</em> results in most cases.
     * </p>
     * <p>
     * The two methods {@link #addOffset(AngularCoordinates) addOffset} and {@link #subtractOffset(AngularCoordinates)
     * subtractOffset} are designed so that round trip applications are possible. This means that both
     * {@code ac1.subtractOffset(ac2).addOffset(ac2)} and {@code ac1.addOffset(ac2).subtractOffset(ac2)} return angular
     * coordinates equal to ac1.
     * </p>
     * <p>
     * <b>Warning: </b>spin derivative is not computed.
     * </p>
     * 
     * @param offset
     *        offset to subtract
     * @return new instance, with offset subtracted
     * @see #addOffset(AngularCoordinates)
     */
    public AngularCoordinates subtractOffset(final AngularCoordinates offset) {
        return this.subtractOffset(offset, false);
    }

    /**
     * Apply the rotation to a pv coordinates.
     * 
     * @param pv
     *        vector to apply the rotation to
     * @return a new pv coordinates which is the image of u by the rotation
     */
    public PVCoordinates applyTo(final PVCoordinates pv) {

        final Vector3D transformedP = this.rotation.applyInverseTo(pv.getPosition());
        final Vector3D crossP = Vector3D.crossProduct(this.rotationRate, transformedP);
        final Vector3D transformedV = this.rotation.applyInverseTo(pv.getVelocity()).subtract(crossP);
        final Vector3D crossV = Vector3D.crossProduct(this.rotationRate, transformedV);
        final Vector3D crossCrossP = Vector3D.crossProduct(this.rotationRate, crossP);
        final Vector3D crossDotP = this.rotationAcceleration == null ? Vector3D.ZERO :
            Vector3D.crossProduct(this.rotationAcceleration, transformedP);
        final Vector3D transformedA = new Vector3D(1, this.rotation.applyInverseTo(pv.getAcceleration() == null
                ? Vector3D.ZERO : pv.getAcceleration()),
            -2, crossV,
            -1, crossCrossP,
            -1, crossDotP);

        return new PVCoordinates(transformedP, transformedV, transformedA);

    }

    /**
     * Apply the rotation to a pv coordinates.
     * 
     * @param pv
     *        vector to apply the rotation to
     * @return a new pv coordinates which is the image of u by the rotation
     */
    public TimeStampedPVCoordinates applyTo(final TimeStampedPVCoordinates pv) {

        final Vector3D transformedP = this.getRotation().applyInverseTo(pv.getPosition());
        final Vector3D crossP = Vector3D.crossProduct(this.getRotationRate(), transformedP);
        final Vector3D transformedV = this.getRotation().applyInverseTo(pv.getVelocity()).subtract(crossP);
        final Vector3D crossV = Vector3D.crossProduct(this.getRotationRate(), transformedV);
        final Vector3D crossCrossP = Vector3D.crossProduct(this.getRotationRate(), crossP);
        final Vector3D crossDotP = this.rotationAcceleration == null ? Vector3D.ZERO :
            Vector3D.crossProduct(this.rotationAcceleration, transformedP);
        final Vector3D transformedA = new Vector3D(1, this.getRotation().applyInverseTo(pv.getAcceleration() == null
                ? Vector3D.ZERO : pv.getAcceleration()),
            -2, crossV,
            -1, crossCrossP,
            -1, crossDotP);

        return new TimeStampedPVCoordinates(pv.getDate(), transformedP, transformedV, transformedA);

    }

    /**
     * Convert rotation, rate and acceleration to modified Rodrigues vector and derivatives.
     * <p>
     * The modified Rodrigues vector is tan(θ/4) u where θ and u are the rotation angle and axis respectively.
     * </p>
     * <p>
     * <b>Warning: </b>spin derivative is not computed.
     * </p>
     * 
     * @param sign
     *        multiplicative sign for quaternion components
     * @return modified Rodrigues vector and derivatives (vector on row 0, first derivative
     *         on row 1, second derivative on row 2)
     * @see #createFromModifiedRodrigues(double[][])
     */
    public double[][] getModifiedRodrigues(final double sign) {
        return this.getModifiedRodrigues(sign, false);
    }

    /**
     * Convert rotation, rate and acceleration to modified Rodrigues vector and derivatives.
     * <p>
     * The modified Rodrigues vector is tan(θ/4) u where θ and u are the rotation angle and axis respectively.
     * </p>
     * 
     * @param sign
     *        multiplicative sign for quaternion components
     * @param computeSpinDerivative
     *        true if spin derivatives should be computed. If not, spin derivative is set to <i>null</i>
     * @return modified Rodrigues vector and derivatives (vector on row 0, first derivative
     *         on row 1, second derivative on row 2)
     * @see #createFromModifiedRodrigues(double[][])
     */
    public double[][] getModifiedRodrigues(final double sign, final boolean computeSpinDerivative) {

        // Getters
        final double q0 = sign * this.getRotation().getQuaternion().getQ0();
        final double q1 = sign * this.getRotation().getQuaternion().getQ1();
        final double q2 = sign * this.getRotation().getQuaternion().getQ2();
        final double q3 = sign * this.getRotation().getQuaternion().getQ3();
        final double oX = this.getRotationRate().getX();
        final double oY = this.getRotationRate().getY();
        final double oZ = this.getRotationRate().getZ();

        // first time-derivatives of the quaternion
        final double q0Dot = 0.5 * MathArrays.linearCombination(-q1, oX, -q2, oY, -q3, oZ);
        final double q1Dot = 0.5 * MathArrays.linearCombination(q0, oX, -q3, oY, q2, oZ);
        final double q2Dot = 0.5 * MathArrays.linearCombination(q3, oX, q0, oY, -q1, oZ);
        final double q3Dot = 0.5 * MathArrays.linearCombination(-q2, oX, q1, oY, q0, oZ);

        // the modified Rodrigues is tan(θ/4) u where θ and u are the rotation angle and axis respectively
        // this can be rewritten using quaternion components:
        // r (q₁ / (1+q₀), q₂ / (1+q₀), q₃ / (1+q₀))
        // applying the derivation chain rule to previous expression gives rDot and rDotDot
        final double inv = 1.0 / (1.0 + q0);
        final double mTwoInvQ0Dot = -2 * inv * q0Dot;

        // Rodrigues vector elements
        final double r1 = inv * q1;
        final double r2 = inv * q2;
        final double r3 = inv * q3;

        final double mInvR1 = -inv * r1;
        final double mInvR2 = -inv * r2;
        final double mInvR3 = -inv * r3;

        // Rodrigues first derivatives
        // r1Dot = (1 / (1 + q0)) * q1Dot - (1 / (1 + q0))² * q1 * q0Dot
        final double r1Dot = MathArrays.linearCombination(inv, q1Dot, mInvR1, q0Dot);
        // r2Dot = (1 / (1 + q0)) * q2Dot - (1 / (1 + q0))² * q2 * q0Dot
        final double r2Dot = MathArrays.linearCombination(inv, q2Dot, mInvR2, q0Dot);
        // r3Dot = (1 / (1 + q0)) * q3Dot - (1 / (1 + q0))² * q3 * q0Dot
        final double r3Dot = MathArrays.linearCombination(inv, q3Dot, mInvR3, q0Dot);

        // second time-derivatives of the quaternion
        double r1DotDot = 0.;
        double r2DotDot = 0.;
        double r3DotDot = 0.;

        if (computeSpinDerivative && this.rotationAcceleration != null) {
            // 2nd derivative
            final double oXDot = this.rotationAcceleration.getX();
            final double oYDot = this.rotationAcceleration.getY();
            final double oZDot = this.rotationAcceleration.getZ();
            final double q0DotDot = -0.5 * MathArrays.linearCombination(
                new double[] { q1, q2, q3, q1Dot, q2Dot, q3Dot },
                new double[] { oXDot, oYDot, oZDot, oX, oY, oZ });
            final double q1DotDot = 0.5 * MathArrays.linearCombination(
                new double[] { q0, q2, -q3, q0Dot, q2Dot, -q3Dot },
                new double[] { oXDot, oZDot, oYDot, oX, oZ, oY });
            final double q2DotDot = 0.5 * MathArrays.linearCombination(
                new double[] { q0, q3, -q1, q0Dot, q3Dot, -q1Dot },
                new double[] { oYDot, oXDot, oZDot, oY, oX, oZ });
            final double q3DotDot = 0.5 * MathArrays.linearCombination(
                new double[] { q0, q1, -q2, q0Dot, q1Dot, -q2Dot },
                new double[] { oZDot, oYDot, oXDot, oZ, oY, oX });
            r1DotDot = MathArrays.linearCombination(inv, q1DotDot, mTwoInvQ0Dot, r1Dot, mInvR1, q0DotDot);
            r2DotDot = MathArrays.linearCombination(inv, q2DotDot, mTwoInvQ0Dot, r2Dot, mInvR2, q0DotDot);
            r3DotDot = MathArrays.linearCombination(inv, q3DotDot, mTwoInvQ0Dot, r3Dot, mInvR3, q0DotDot);
        }

        // Return result
        return new double[][] { { r1, r2, r3 }, { r1Dot, r2Dot, r3Dot }, { r1DotDot, r2DotDot, r3DotDot } };

    }

    /**
     * Convert a modified Rodrigues vector and derivatives to angular coordinates.
     * 
     * @param r
     *        modified Rodrigues vector (with first and second times derivatives)
     * @param computeSpinDerivatives
     *        true if spin derivatives should be computed. If not, spin derivative is set to <i>null</i>
     * @return angular coordinates
     * @see #getModifiedRodrigues(double)
     */
    public static AngularCoordinates createFromModifiedRodrigues(final double[][] r,
                                                                 final boolean computeSpinDerivatives) {

        // rotation
        //
        final double rSquared = r[0][0] * r[0][0] + r[0][1] * r[0][1] + r[0][2] * r[0][2];
        final double oPQ0 = 2 / (1 + rSquared);
        final double q0 = oPQ0 - 1;
        final double q1 = oPQ0 * r[0][0];
        final double q2 = oPQ0 * r[0][1];
        final double q3 = oPQ0 * r[0][2];

        // rotation rate
        final double oPQ02 = oPQ0 * oPQ0;
        final double q0Dot = -oPQ02
            * MathArrays.linearCombination(r[0][0], r[1][0], r[0][1], r[1][1], r[0][2], r[1][2]);
        final double q1Dot = oPQ0 * r[1][0] + r[0][0] * q0Dot;
        final double q2Dot = oPQ0 * r[1][1] + r[0][1] * q0Dot;
        final double q3Dot = oPQ0 * r[1][2] + r[0][2] * q0Dot;
        final double oX = 2 * MathArrays.linearCombination(-q1, q0Dot, q0, q1Dot, q3, q2Dot, -q2, q3Dot);
        final double oY = 2 * MathArrays.linearCombination(-q2, q0Dot, -q3, q1Dot, q0, q2Dot, q1, q3Dot);
        final double oZ = 2 * MathArrays.linearCombination(-q3, q0Dot, q2, q1Dot, -q1, q2Dot, q0, q3Dot);

        // rotation acceleration
        Vector3D acc = null;
        if (computeSpinDerivatives) {
            // Acceleration
            final double q0DotDot = (1 - q0) / oPQ0 * q0Dot * q0Dot -
                oPQ02 * MathArrays.linearCombination(r[0][0], r[2][0], r[0][1], r[2][1], r[0][2], r[2][2]) -
                (q1Dot * q1Dot + q2Dot * q2Dot + q3Dot * q3Dot);
            final double q1DotDot = MathArrays.linearCombination(oPQ0, r[2][0], 2 * r[1][0], q0Dot, r[0][0], q0DotDot);
            final double q2DotDot = MathArrays.linearCombination(oPQ0, r[2][1], 2 * r[1][1], q0Dot, r[0][1], q0DotDot);
            final double q3DotDot = MathArrays.linearCombination(oPQ0, r[2][2], 2 * r[1][2], q0Dot, r[0][2], q0DotDot);
            final double oXDot = 2 * MathArrays.linearCombination(-q1, q0DotDot, q0, q1DotDot, q3, q2DotDot, -q2,
                q3DotDot);
            final double oYDot = 2 * MathArrays.linearCombination(-q2, q0DotDot, -q3, q1DotDot, q0, q2DotDot, q1,
                q3DotDot);
            final double oZDot = 2 * MathArrays.linearCombination(-q3, q0DotDot, q2, q1DotDot, -q1, q2DotDot, q0,
                q3DotDot);
            acc = new Vector3D(oXDot, oYDot, oZDot);
        }

        // Return result
        return new AngularCoordinates(new Rotation(false, q0, q1, q2, q3), new Vector3D(oX, oY, oZ), acc);
    }

    /**
     * Convert a modified Rodrigues vector and derivatives to angular coordinates.
     * <p>
     * <b>Warning: </b>spin derivative is not computed.
     * </p>
     * 
     * @param r
     *        modified Rodrigues vector (with first and second times derivatives)
     * @return angular coordinates
     * @see #getModifiedRodrigues(double)
     */
    public static AngularCoordinates createFromModifiedRodrigues(final double[][] r) {
        return createFromModifiedRodrigues(r, false);
    }
}
