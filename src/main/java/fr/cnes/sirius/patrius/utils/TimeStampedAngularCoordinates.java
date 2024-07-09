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
 */
/* Copyright 2002-2015 CS Systèmes d'Information
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:489:06/10/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.utils;

import java.util.Collection;

import fr.cnes.sirius.patrius.math.analysis.differentiation.DerivativeStructure;
import fr.cnes.sirius.patrius.math.analysis.interpolation.HermiteInterpolator;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathArrays;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeStamped;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * {@link TimeStamped time-stamped} version of {@link AngularCoordinates}.
 * <p>
 * Instances of this class are guaranteed to be immutable.
 * </p>
 * 
 * @author Luc Maisonobe
 * @since 3.1
 */
public class TimeStampedAngularCoordinates extends AngularCoordinates implements TimeStamped {

    /** Serializable UID. */
    private static final long serialVersionUID = 20140723L;

    /** Epsilon. */
    private static final double EPSILON = 1.0e-4;

    /** The date. */
    private final AbsoluteDate date;

    /**
     * Builds a rotation/rotation rate pair.
     * 
     * @param dateIn
     *        coordinates date
     * @param rotation
     *        rotation
     * @param rotationRate
     *        rotation rate Ω (rad/s)
     * @param rotationAcceleration
     *        rotation acceleration dΩ/dt (rad²/s²)
     */
    public TimeStampedAngularCoordinates(final AbsoluteDate dateIn,
        final Rotation rotation,
        final Vector3D rotationRate,
        final Vector3D rotationAcceleration) {
        super(rotation, rotationRate, rotationAcceleration);
        this.date = dateIn;
    }

    /**
     * Builds from angular coordinates.
     * 
     * @param dateIn
     *        coordinates date
     * @param angular
     *        angular coordinates
     */
    public TimeStampedAngularCoordinates(final AbsoluteDate dateIn, final AngularCoordinates angular) {
        super(angular.getRotation(), angular.getRotationRate(), angular.getRotationAcceleration());
        this.date = dateIn;
    }

    /**
     * Build the rotation that transforms a pair of pv coordinates into another pair.
     * <p>
     * <em>WARNING</em>! This method requires much more stringent assumptions on its parameters than the similar
     * {@link Rotation#Rotation(Vector3D, Vector3D, Vector3D, Vector3D) constructor} from the {@link Rotation Rotation}
     * class. As far as the Rotation constructor is concerned, the {@code v₂} vector from the second pair can be
     * slightly misaligned. The Rotation constructor will compensate for this misalignment and create a rotation that
     * ensure {@code v₁ = r(u₁)} and {@code v₂ ∈ plane (r(u₁), r(u₂))}. <em>THIS IS NOT
     * TRUE ANYMORE IN THIS CLASS</em>! As derivatives are involved and must be preserved, this constructor works
     * <em>only</em> if the two pairs are fully consistent, i.e. if a rotation exists that fulfill all the requirements:
     * {@code v₁ = r(u₁)}, {@code v₂ = r(u₂)}, {@code dv₁/dt = dr(u₁)/dt}, {@code dv₂/dt
     * = dr(u₂)/dt}, {@code d²v₁/dt² = d²r(u₁)/dt²}, {@code d²v₂/dt² = d²r(u₂)/dt²}.
     * </p>
     * <p>
     * <b>Warning: </b>spin derivative is not computed.
     * </p>
     * 
     * @param dateIn
     *        coordinates date
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
     *            if the vectors components cannot be converted to {@link DerivativeStructure} with proper order
     */
    public TimeStampedAngularCoordinates(final AbsoluteDate dateIn,
        final PVCoordinates u1, final PVCoordinates u2,
        final PVCoordinates v1, final PVCoordinates v2,
        final double tolerance) throws PatriusException {
        super(u1, u2, v1, v2, tolerance);
        this.date = dateIn;
    }

    /**
     * Build the rotation that transforms a pair of pv coordinates into another pair.
     * 
     * <p>
     * <em>WARNING</em>! This method requires much more stringent assumptions on its parameters than the similar
     * {@link Rotation#Rotation(Vector3D, Vector3D, Vector3D, Vector3D) constructor} from the {@link Rotation Rotation}
     * class. As far as the Rotation constructor is concerned, the {@code v₂} vector from the second pair can be
     * slightly misaligned. The Rotation constructor will compensate for this misalignment and create a rotation that
     * ensure {@code v₁ = r(u₁)} and {@code v₂ ∈ plane (r(u₁), r(u₂))}. <em>THIS IS NOT
     * TRUE ANYMORE IN THIS CLASS</em>! As derivatives are involved and must be preserved, this constructor works
     * <em>only</em> if the two pairs are fully consistent, i.e. if a rotation exists that fulfill all the requirements:
     * {@code v₁ = r(u₁)}, {@code v₂ = r(u₂)}, {@code dv₁/dt = dr(u₁)/dt}, {@code dv₂/dt
     * = dr(u₂)/dt}, {@code d²v₁/dt² = d²r(u₁)/dt²}, {@code d²v₂/dt² = d²r(u₂)/dt²}.
     * </p>
     * 
     * @param dateIn
     *        coordinates date
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
     *        true if the spin derivative should be computed If not, spin derivative is set to <i>null</i>
     * @exception PatriusException
     *            if the vectors components cannot be converted to {@link DerivativeStructure} with proper order
     */
    public TimeStampedAngularCoordinates(final AbsoluteDate dateIn,
        final PVCoordinates u1, final PVCoordinates u2,
        final PVCoordinates v1, final PVCoordinates v2,
        final double tolerance, final boolean spinDerivativesComputation) throws PatriusException {
        super(u1, u2, v1, v2, tolerance, spinDerivativesComputation);
        this.date = dateIn;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getDate() {
        return this.date;
    }

    /**
     * Revert a rotation/rotation rate/rotation acceleration triplet.
     * Build a triplet which reverse the effect of another triplet.
     * 
     * @param computeSpinDerivative
     *        true if spin derivative should be computed. If not, spin derivative is set to <i>null</i>
     * @return a new triplet whose effect is the reverse of the effect
     *         of the instance
     */
    @Override
    public TimeStampedAngularCoordinates revert(final boolean computeSpinDerivative) {
        Vector3D acc = null;
        if (computeSpinDerivative && this.getRotationAcceleration() != null) {
            acc = this.getRotation().applyTo(this.getRotationAcceleration().negate());
        }
        return new TimeStampedAngularCoordinates(this.date,
            this.getRotation().revert(),
            this.getRotation().applyTo(this.getRotationRate().negate()),
            acc);
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
    @Override
    public TimeStampedAngularCoordinates revert() {
        return this.revert(false);
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
     * @param computeSpinDerivative
     *        true if spin derivative should be computed. If not, spin derivative is set to <i>null</i>
     * @return a new state, shifted with respect to the instance (which is immutable)
     */
    @Override
    public TimeStampedAngularCoordinates shiftedBy(final double dt, final boolean computeSpinDerivative) {
        final AngularCoordinates sac = super.shiftedBy(dt, computeSpinDerivative);
        return new TimeStampedAngularCoordinates(this.date.shiftedBy(dt),
            sac.getRotation(), sac.getRotationRate(), sac.getRotationAcceleration());
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
    public TimeStampedAngularCoordinates shiftedBy(final double dt) {
        return this.shiftedBy(dt, false);
    }

    /**
     * Add an offset from the instance.
     * <p>
     * We consider here that the offset rotation is applied first and the instance is applied afterward. Note that
     * angular coordinates do <em>not</em> commute under this operation, i.e. {@code a.addOffset(b)} and
     * {@code b.addOffset(a)} lead to <em>different</em> results in most cases.
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
    @Override
    public TimeStampedAngularCoordinates addOffset(final AngularCoordinates offset) {
        return this.addOffset(offset, false);
    }

    /**
     * Add an offset from the instance.
     * <p>
     * We consider here that the offset rotation is applied first and the instance is applied afterward. Note that
     * angular coordinates do <em>not</em> commute under this operation, i.e. {@code a.addOffset(b)} and
     * {@code b.addOffset(a)} lead to <em>different</em> results in most cases.
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
     *        true if spin derivatives should be computed. If not, spin derivative is set to <i>null</i>
     * @return new instance, with offset subtracted
     * @see #subtractOffset(AngularCoordinates)
     */
    @Override
    public TimeStampedAngularCoordinates addOffset(final AngularCoordinates offset,
                                                   final boolean computeSpinDerivatives) {
        final Vector3D rOmega = offset.getRotation().applyInverseTo(this.getRotationRate());
        Vector3D acc = null;
        if (computeSpinDerivatives && this.getRotationAcceleration() != null
            && offset.getRotationAcceleration() != null) {
            final Vector3D rOmegaDot = offset.getRotation().applyInverseTo(this.getRotationAcceleration());
            acc = new Vector3D(1.0, offset.getRotationAcceleration(), 1.0, rOmegaDot,
                -1.0, Vector3D.crossProduct(offset.getRotationRate(), rOmega));
        }
        return new TimeStampedAngularCoordinates(this.date,
            this.getRotation().applyTo(offset.getRotation()),
            offset.getRotationRate().add(rOmega),
            acc);
    }

    /**
     * Subtract an offset from the instance.
     * <p>
     * We consider here that the offset rotation is applied first and the instance is applied afterward. Note that
     * angular coordinates do <em>not</em> commute under this operation, i.e. {@code a.subtractOffset(b)} and
     * {@code b.subtractOffset(a)} lead to <em>different</em> results in most cases.
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
    @Override
    public TimeStampedAngularCoordinates subtractOffset(final AngularCoordinates offset) {
        return this.subtractOffset(offset, false);
    }

    /**
     * Subtract an offset from the instance.
     * <p>
     * We consider here that the offset rotation is applied first and the instance is applied afterward. Note that
     * angular coordinates do <em>not</em> commute under this operation, i.e. {@code a.subtractOffset(b)} and
     * {@code b.subtractOffset(a)} lead to <em>different</em> results in most cases.
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
     *        true if spin derivatives should be computed. If not, spin derivative is set to <i>null</i>
     * @return new instance, with offset subtracted
     * @see #addOffset(AngularCoordinates)
     */
    @Override
    public TimeStampedAngularCoordinates subtractOffset(final AngularCoordinates offset,
                                                        final boolean computeSpinDerivatives) {
        return this.addOffset(offset.revert(computeSpinDerivatives), computeSpinDerivatives);
    }

    /**
     * Interpolate angular coordinates.
     * <p>
     * The interpolated instance is created by polynomial Hermite interpolation on Rodrigues vector ensuring rotation
     * rate remains the exact derivative of rotation.
     * </p>
     * <p>
     * This method is based on Sergei Tanygin's paper <a
     * href="http://www.agi.com/downloads/resources/white-papers/Attitude-interpolation.pdf">Attitude Interpolation</a>,
     * changing the norm of the vector to match the modified Rodrigues vector as described in Malcolm D. Shuster's paper
     * <a href=
     * "http://www.ladispe.polito.it/corsi/Meccatronica/02JHCOR/2011-12/Slides/Shuster_Pub_1993h_J_Repsurv_scan.pdf">A
     * Survey of Attitude Representations</a>. This change avoids the singularity at π. There is still a singularity at
     * 2π, which is handled by slightly offsetting all rotations when this singularity is detected.
     * </p>
     * <p>
     * Note that even if first and second time derivatives (rotation rates and acceleration) from sample can be ignored,
     * the interpolated instance always includes interpolated derivatives. This feature can be used explicitly to
     * compute these derivatives when it would be too complex to compute them from an analytical formula: just compute a
     * few sample points from the explicit formula and set the derivatives to zero in these sample points, then use
     * interpolation to add derivatives consistent with the rotations.
     * </p>
     * <p>
     * <b>Warning: </b>spin derivative is not computed.
     * </p>
     * 
     * @param date
     *        interpolation date
     * @param filter
     *        filter for derivatives from the sample to use in interpolation
     * @param sample
     *        sample points on which interpolation should be done
     * @return a new position-velocity, interpolated at specified date
     * @exception PatriusException
     *            if the number of point is too small for interpolating
     */
    public static TimeStampedAngularCoordinates
            interpolate(final AbsoluteDate date,
                        final AngularDerivativesFilter filter,
                        final Collection<TimeStampedAngularCoordinates> sample) throws PatriusException {
        return interpolate(date, filter, sample, false);
    }

    /**
     * Interpolate angular coordinates.
     * <p>
     * The interpolated instance is created by polynomial Hermite interpolation on Rodrigues vector ensuring rotation
     * rate remains the exact derivative of rotation.
     * </p>
     * <p>
     * This method is based on Sergei Tanygin's paper <a
     * href="http://www.agi.com/downloads/resources/white-papers/Attitude-interpolation.pdf">Attitude Interpolation</a>,
     * changing the norm of the vector to match the modified Rodrigues vector as described in Malcolm D. Shuster's paper
     * <a href=
     * "http://www.ladispe.polito.it/corsi/Meccatronica/02JHCOR/2011-12/Slides/Shuster_Pub_1993h_J_Repsurv_scan.pdf">A
     * Survey of Attitude Representations</a>. This change avoids the singularity at π. There is still a singularity at
     * 2π, which is handled by slightly offsetting all rotations when this singularity is detected.
     * </p>
     * <p>
     * Note that even if first and second time derivatives (rotation rates and acceleration) from sample can be ignored,
     * the interpolated instance always includes interpolated derivatives. This feature can be used explicitly to
     * compute these derivatives when it would be too complex to compute them from an analytical formula: just compute a
     * few sample points from the explicit formula and set the derivatives to zero in these sample points, then use
     * interpolation to add derivatives consistent with the rotations.
     * </p>
     * 
     * @param date
     *        interpolation date
     * @param filter
     *        filter for derivatives from the sample to use in interpolation
     * @param sample
     *        sample points on which interpolation should be done
     * @param computeSpinDerivatives
     *        true if spin derivative should be computed. If not, spin derivative is set to <i>null</i>
     * @return a new position-velocity, interpolated at specified date
     * @exception PatriusException
     *            if the number of point is too small for interpolating
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Orekit code kept as such
    public static TimeStampedAngularCoordinates
            interpolate(final AbsoluteDate date,
                        final AngularDerivativesFilter filter,
                        final Collection<TimeStampedAngularCoordinates> sample,
                        final boolean computeSpinDerivatives) throws PatriusException {
        // CHECKSTYLE: resume CyclomaticComplexity check

        // set up a linear model canceling mean rotation rate
        final Vector3D meanRate;
        if (filter == AngularDerivativesFilter.USE_R) {
            if (sample.size() < 2) {
                throw new PatriusException(PatriusMessages.NOT_ENOUGH_DATA_FOR_INTERPOLATION, sample.size());
            }
            Vector3D sum = Vector3D.ZERO;
            TimeStampedAngularCoordinates previous = null;
            for (final TimeStampedAngularCoordinates datedAC : sample) {
                if (previous != null) {
                    sum = sum.add(estimateRate(previous.getRotation(), datedAC.getRotation(),
                        datedAC.date.durationFrom(previous.date)));
                }
                previous = datedAC;
            }
            meanRate = new Vector3D(1.0 / (sample.size() - 1), sum);
        } else {
            Vector3D sum = Vector3D.ZERO;
            for (final TimeStampedAngularCoordinates datedAC : sample) {
                final Rotation rot = datedAC.getRotation();
                // sum = sum.add(datedAC.getRotationRate());
                sum = sum.add(rot.applyTo(datedAC.getRotationRate()));
            }
            meanRate = new Vector3D(1.0 / sample.size(), sum);
        }
        TimeStampedAngularCoordinates offset =
            new TimeStampedAngularCoordinates(date, Rotation.IDENTITY, meanRate, Vector3D.ZERO);

        // set up safety elements for 2π singularity avoidance
        final double epsilon = 2 * FastMath.PI / sample.size();
        final double threshold = MathLib.min(-(1.0 - EPSILON), -MathLib.cos(epsilon / 4));

        boolean restart = true;
        for (int i = 0; restart && i < sample.size() + 2; ++i) {

            // offset adaptation parameters
            restart = false;

            // set up an interpolator taking derivatives into account
            final HermiteInterpolator interpolator = new HermiteInterpolator();

            // add sample points
            double sign = +1.0;
            Rotation previous = Rotation.IDENTITY;

            for (final TimeStampedAngularCoordinates ac : sample) {

                // remove linear offset from the current coordinates
                final double dt = ac.date.durationFrom(date);
                final TimeStampedAngularCoordinates fixed = ac.subtractOffset(
                    offset.shiftedBy(dt, computeSpinDerivatives), computeSpinDerivatives);

                // make sure all interpolated points will be on the same branch
                final double dot = MathArrays.linearCombination(fixed.getRotation().getQuaternion().getQ0(), previous
                    .getQuaternion().getQ0(), fixed.getRotation().getQuaternion().getQ1(), previous.getQuaternion()
                    .getQ1(), fixed.getRotation().getQuaternion().getQ2(), previous.getQuaternion().getQ2(),
                    fixed.getRotation().getQuaternion().getQ3(), previous.getQuaternion().getQ3());
                sign = MathLib.copySign(1.0, dot * sign);
                previous = fixed.getRotation();

                // check modified Rodrigues vector singularity
                if (fixed.getRotation().getQuaternion().getQ0() * sign < threshold) {
                    // the sample point is close to a modified Rodrigues vector singularity
                    // we need to change the linear offset model to avoid this
                    restart = true;
                    break;
                }

                final double[][] rodrigues = fixed.getModifiedRodrigues(sign, computeSpinDerivatives);
                switch (filter) {
                    case USE_RRA:
                        // populate sample with rotation, rotation rate and acceleration data
                        interpolator.addSamplePoint(dt, rodrigues[0], rodrigues[1], rodrigues[2]);
                        break;
                    case USE_RR:
                        // populate sample with rotation and rotation rate data
                        interpolator.addSamplePoint(dt, rodrigues[0], rodrigues[1]);
                        break;
                    case USE_R:
                        // populate sample with rotation data only
                        interpolator.addSamplePoint(dt, rodrigues[0]);
                        break;
                    default:
                        // this should never happen
                        throw PatriusException.createInternalError(null);
                }
            }

            if (restart) {
                // interpolation failed, some intermediate rotation was too close to 2π
                // we need to offset all rotations to avoid the singularity
                offset = offset.addOffset(new AngularCoordinates(new Rotation(Vector3D.PLUS_I, epsilon),
                    Vector3D.ZERO, Vector3D.ZERO), computeSpinDerivatives);
            } else {
                // interpolation succeeded with the current offset
                final DerivativeStructure zero = new DerivativeStructure(1, 2, 0, 0.0);
                final DerivativeStructure[] p = interpolator.value(zero);
                final AngularCoordinates ac = createFromModifiedRodrigues(new double[][] {
                    { p[0].getValue(), p[1].getValue(), p[2].getValue() },
                    { p[0].getPartialDerivative(1), p[1].getPartialDerivative(1), p[2].getPartialDerivative(1) },
                    { p[0].getPartialDerivative(2), p[1].getPartialDerivative(2), p[2].getPartialDerivative(2) }
                }, computeSpinDerivatives);
                return new TimeStampedAngularCoordinates(offset.getDate(), ac.getRotation(),
                    ac.getRotationRate(), ac.getRotationAcceleration()).addOffset(offset, computeSpinDerivatives);
            }

        }

        // this should never happen
        throw PatriusException.createInternalError(null);
    }
}
