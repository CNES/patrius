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
 */
/* Copyright 2002-2015 CS Systèmes d'Information
 *
 * HISTORY
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:FA:FA-2326:27/05/2020:Orbits - Correction equals & HashCode 
 * VERSION:4.5:FA:FA-2464:27/05/2020:Anomalie dans le calcul du vecteur rotation des LOF
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:489:06/10/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.orbits.pvcoordinates;

import java.util.Collection;

import fr.cnes.sirius.patrius.math.analysis.differentiation.DerivativeStructure;
import fr.cnes.sirius.patrius.math.analysis.interpolation.HermiteInterpolator;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.FieldVector3D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeStamped;
import fr.cnes.sirius.patrius.utils.CartesianDerivativesFilter;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * {@link TimeStamped time-stamped} version of {@link PVCoordinates}.
 * <p>
 * Instances of this class are guaranteed to be immutable.
 * </p>
 * 
 * @author Luc Maisonobe
 * @since 3.1
 */
@SuppressWarnings("PMD.NullAssignment")
public class TimeStampedPVCoordinates extends PVCoordinates implements TimeStamped {

    /** Serializable UID. */
    private static final long serialVersionUID = 20140723L;

    /** The date. */
    private final AbsoluteDate date;

    /**
     * Builds a TimeStampedPVCoordinates triplet.
     * 
     * @param dateIn
     *        coordinates date
     * @param position
     *        the position vector (m)
     * @param velocity
     *        the velocity vector (m/s)
     * @param acceleration
     *        the acceleration vector (m/s²)
     */
    public TimeStampedPVCoordinates(final AbsoluteDate dateIn,
        final Vector3D position, final Vector3D velocity, final Vector3D acceleration) {
        super(position, velocity, acceleration);
        this.date = dateIn;
    }

    /**
     * Build from position and velocity. Acceleration is set to zero.
     * 
     * @param dateIn
     *        coordinates date
     * @param position
     *        the position vector (m)
     * @param velocity
     *        the velocity vector (m/s)
     */
    public TimeStampedPVCoordinates(final AbsoluteDate dateIn,
        final Vector3D position,
        final Vector3D velocity) {
        this(dateIn, position, velocity, Vector3D.ZERO);
    }

    /**
     * Build from position velocity acceleration coordinates.
     * 
     * @param dateIn
     *        coordinates date
     * @param pv
     *        position velocity, and acceleration coordinates, in meters and seconds.
     */
    public TimeStampedPVCoordinates(final AbsoluteDate dateIn, final PVCoordinates pv) {
        this(dateIn, pv.getPosition(), pv.getVelocity(), pv.getAcceleration());
    }

    /**
     * Multiplicative constructor
     * <p>
     * Build a TimeStampedPVCoordinates from another one and a scale factor.
     * </p>
     * <p>
     * The TimeStampedPVCoordinates built will be a * pv
     * </p>
     * 
     * @param dateIn
     *        date of the built coordinates
     * @param a
     *        scale factor
     * @param pv
     *        base (unscaled) PVCoordinates
     */
    public TimeStampedPVCoordinates(final AbsoluteDate dateIn,
        final double a, final PVCoordinates pv) {
        super(new Vector3D(a, pv.getPosition()),
            new Vector3D(a, pv.getVelocity()),
            pv.getAcceleration() == null ? null : new Vector3D(a, pv.getAcceleration()));
        this.date = dateIn;
    }

    /**
     * Subtractive constructor
     * <p>
     * Build a relative TimeStampedPVCoordinates from a start and an end position.
     * </p>
     * <p>
     * The TimeStampedPVCoordinates built will be end - start.
     * </p>
     * 
     * @param dateIn
     *        date of the built coordinates
     * @param start
     *        Starting PVCoordinates
     * @param end
     *        ending PVCoordinates
     */
    public TimeStampedPVCoordinates(final AbsoluteDate dateIn,
        final PVCoordinates start, final PVCoordinates end) {
        super(end.getPosition().subtract(start.getPosition()), end.getVelocity().subtract(start.getVelocity()), start
                .getAcceleration() == null || end.getAcceleration() == null ? null : end.getAcceleration().subtract(
                start.getAcceleration()));
        this.date = dateIn;
    }

    /**
     * Linear constructor
     * <p>
     * Build a TimeStampedPVCoordinates from two other ones and corresponding scale factors.
     * </p>
     * <p>
     * The TimeStampedPVCoordinates built will be a1 * u1 + a2 * u2
     * </p>
     * 
     * @param dateIn
     *        date of the built coordinates
     * @param a1
     *        first scale factor
     * @param pv1
     *        first base (unscaled) PVCoordinates
     * @param a2
     *        second scale factor
     * @param pv2
     *        second base (unscaled) PVCoordinates
     */
    public TimeStampedPVCoordinates(final AbsoluteDate dateIn,
        final double a1, final PVCoordinates pv1,
        final double a2, final PVCoordinates pv2) {
        super(new Vector3D(a1, pv1.getPosition(), a2, pv2.getPosition()),
            new Vector3D(a1, pv1.getVelocity(), a2, pv2.getVelocity()),
            pv1.getAcceleration() == null || pv2.getAcceleration() == null ? null
                    : new Vector3D(a1, pv1.getAcceleration(), a2, pv2.getAcceleration()));
        this.date = dateIn;
    }

    /**
     * Linear constructor
     * <p>
     * Build a TimeStampedPVCoordinates from three other ones and corresponding scale factors.
     * </p>
     * <p>
     * The TimeStampedPVCoordinates built will be a1 * u1 + a2 * u2 + a3 * u3
     * </p>
     * 
     * @param dateIn
     *        date of the built coordinates
     * @param a1
     *        first scale factor
     * @param pv1
     *        first base (unscaled) PVCoordinates
     * @param a2
     *        second scale factor
     * @param pv2
     *        second base (unscaled) PVCoordinates
     * @param a3
     *        third scale factor
     * @param pv3
     *        third base (unscaled) PVCoordinates
     */
    public TimeStampedPVCoordinates(final AbsoluteDate dateIn,
        final double a1, final PVCoordinates pv1,
        final double a2, final PVCoordinates pv2,
        final double a3, final PVCoordinates pv3) {
        super(new Vector3D(a1, pv1.getPosition(), a2, pv2.getPosition(), a3, pv3.getPosition()),
            new Vector3D(a1, pv1.getVelocity(), a2, pv2.getVelocity(), a3, pv3.getVelocity()),
            pv1.getAcceleration() == null
            || pv2.getAcceleration() == null || pv3.getAcceleration() == null ? null : new Vector3D(a1,
                    pv1.getAcceleration(), a2, pv2.getAcceleration(), a3, pv3.getAcceleration()));
        this.date = dateIn;
    }

    /**
     * Linear constructor
     * <p>
     * Build a TimeStampedPVCoordinates from four other ones and corresponding scale factors.
     * </p>
     * <p>
     * The TimeStampedPVCoordinates built will be a1 * u1 + a2 * u2 + a3 * u3 + a4 * u4
     * </p>
     * 
     * @param dateIn
     *        date of the built coordinates
     * @param a1
     *        first scale factor
     * @param pv1
     *        first base (unscaled) PVCoordinates
     * @param a2
     *        second scale factor
     * @param pv2
     *        second base (unscaled) PVCoordinates
     * @param a3
     *        third scale factor
     * @param pv3
     *        third base (unscaled) PVCoordinates
     * @param a4
     *        fourth scale factor
     * @param pv4
     *        fourth base (unscaled) PVCoordinates
     */
    public TimeStampedPVCoordinates(final AbsoluteDate dateIn,
        final double a1, final PVCoordinates pv1,
        final double a2, final PVCoordinates pv2,
        final double a3, final PVCoordinates pv3,
        final double a4, final PVCoordinates pv4) {
        super(
            new Vector3D(a1, pv1.getPosition(), a2, pv2.getPosition(), a3, pv3.getPosition(),
                a4, pv4.getPosition()),
            new Vector3D(a1, pv1.getVelocity(), a2, pv2.getVelocity(), a3, pv3.getVelocity(),
                a4, pv4.getVelocity()),
                pv1.getAcceleration() == null || pv2.getAcceleration() == null || pv3.getAcceleration() == null
                        || pv4.getAcceleration() == null ? null : new Vector3D(a1, pv1.getAcceleration(), a2,
                        pv2.getAcceleration(), a3, pv3.getAcceleration(), a4, pv4.getAcceleration()));
        this.date = dateIn;
    }

    /**
     * Builds a TimeStampedPVCoordinates triplet from a {@link FieldVector3D}&lt;{@link DerivativeStructure}&gt;.
     * <p>
     * The vector components must have time as their only derivation parameter and have consistent derivation orders.
     * </p>
     * 
     * @param dateIn
     *        date of the built coordinates
     * @param p
     *        vector with time-derivatives embedded within the coordinates
     */
    public TimeStampedPVCoordinates(final AbsoluteDate dateIn,
        final FieldVector3D<DerivativeStructure> p) {
        super(p);
        this.date = dateIn;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getDate() {
        return this.date;
    }

    /**
     * Get a time-shifted state.
     * <p>
     * The state can be slightly shifted to close dates. This shift is based on a simple Taylor expansion. It is
     * <em>not</em> intended as a replacement for proper orbit propagation (it is not even Keplerian!) but should be
     * sufficient for either small time shifts or coarse accuracy.
     * </p>
     * 
     * @param dt
     *        time shift in seconds
     * @return a new state, shifted with respect to the instance (which is immutable)
     */
    @Override
    public TimeStampedPVCoordinates shiftedBy(final double dt) {
        final PVCoordinates spv = super.shiftedBy(dt);
        return new TimeStampedPVCoordinates(this.date.shiftedBy(dt),
            spv.getPosition(), spv.getVelocity(), spv.getAcceleration());
    }

    /**
     * Interpolate position-velocity.
     * <p>
     * The interpolated instance is created by polynomial Hermite interpolation ensuring velocity remains the exact
     * derivative of position.
     * </p>
     * <p>
     * Note that even if first time derivatives (velocities) from sample can be ignored, the interpolated instance
     * always includes interpolated derivatives. This feature can be used explicitly to compute these derivatives when
     * it would be too complex to compute them from an analytical formula: just compute a few sample points from the
     * explicit formula and set the derivatives to zero in these sample points, then use interpolation to add
     * derivatives consistent with the positions.
     * </p>
     * 
     * @param date
     *        interpolation date
     * @param filter
     *        filter for derivatives from the sample to use in interpolation
     * @param sample
     *        sample points on which interpolation should be done
     * @return a new position-velocity, interpolated at specified date
     */
    public static TimeStampedPVCoordinates interpolate(final AbsoluteDate date,
                                                       final CartesianDerivativesFilter filter,
                                                       final Collection<TimeStampedPVCoordinates> sample) {

        // set up an interpolator taking derivatives into account
        final HermiteInterpolator interpolator = new HermiteInterpolator();

        // add sample points
        switch (filter) {
            case USE_P:
                // populate sample with position data, ignoring velocity
                for (final TimeStampedPVCoordinates pv : sample) {
                    final Vector3D position = pv.getPosition();
                    interpolator.addSamplePoint(pv.getDate().durationFrom(date),
                        new double[] {
                            position.getX(), position.getY(), position.getZ()
                        });
                }
                break;
            case USE_PV:
                // populate sample with position and velocity data
                for (final TimeStampedPVCoordinates pv : sample) {
                    final Vector3D position = pv.getPosition();
                    final Vector3D velocity = pv.getVelocity();
                    interpolator.addSamplePoint(pv.getDate().durationFrom(date),
                        new double[] {
                            position.getX(), position.getY(), position.getZ()
                        }, new double[] {
                            velocity.getX(), velocity.getY(), velocity.getZ()
                        });
                }
                break;
            case USE_PVA:
                // populate sample with position, velocity and acceleration data
                for (final TimeStampedPVCoordinates pv : sample) {
                    final Vector3D position = pv.getPosition();
                    final Vector3D velocity = pv.getVelocity();
                    final Vector3D acceleration = pv.getAcceleration();
                    interpolator.addSamplePoint(pv.getDate().durationFrom(date),
                        new double[] {
                            position.getX(), position.getY(), position.getZ()
                        }, new double[] {
                            velocity.getX(), velocity.getY(), velocity.getZ()
                        }, new double[] {
                            acceleration.getX(), acceleration.getY(), acceleration.getZ()
                        });
                }
                break;
            default:
                // this should never happen
                throw PatriusException.createInternalError(null);
        }

        // interpolate
        final DerivativeStructure zero = new DerivativeStructure(1, 2, 0, 0.0);
        final DerivativeStructure[] p = interpolator.value(zero);

        // build a new interpolated instance
        return new TimeStampedPVCoordinates(date,
            new Vector3D(p[0].getValue(),
                p[1].getValue(),
                p[2].getValue()),
            new Vector3D(p[0].getPartialDerivative(1),
                p[1].getPartialDerivative(1),
                p[2].getPartialDerivative(1)),
            new Vector3D(p[0].getPartialDerivative(2),
                p[1].getPartialDerivative(2),
                p[2].getPartialDerivative(2)));

    }

    /**
     * Return a string representation of this position/velocity pair.
     * 
     * @return string representation of this position/velocity pair
     */
    @Override
    public String toString() {
        final String comma = ", ";
        final StringBuffer buffer = new StringBuffer().append('{').append(this.date).append(", P(").
                append(this.getPosition().getX()).append(comma).
                append(this.getPosition().getY()).append(comma).
                append(this.getPosition().getZ()).append("), V(").
                append(this.getVelocity().getX()).append(comma).
                append(this.getVelocity().getY()).append(comma).
                append(this.getVelocity().getZ()).append(")");
        if (this.getAcceleration() != null) {
            buffer.append(", A(").
            append(this.getAcceleration().getX()).append(comma).
            append(this.getAcceleration().getY()).append(comma).
            append(this.getAcceleration().getZ()).append(")}");
        } else {
            buffer.append("}");
        }
        return buffer.toString();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object object) {
        // parameters : date
        //              x, y, z, xDot, yDot, zDot
        //              acceleration
        boolean isEqual = super.equals(object);
        
        if (isEqual) {
            if (object instanceof TimeStampedPVCoordinates) {
                final TimeStampedPVCoordinates other = (TimeStampedPVCoordinates) object;

                // Date
                isEqual &= (getDate().equals(other.getDate()));
                
            } else {
                isEqual = false;
            }
        }
        
        return isEqual;
    }
        
    /** {@inheritDoc} */
    @Override 
    public int hashCode() { 
        // PVCoordinates hashcode
        int result = super.hashCode();
        // An efficient multiplier (JVM optimizes 31 * i as (i << 5) - 1 )
        final int effMult = 31;
        // Good hashcode : it's the same
        // for "equal" orbits, but
        // reasonably sure it's different otherwise.
        result = effMult * result + getDate().hashCode();
 
        return result; 
    }
}
