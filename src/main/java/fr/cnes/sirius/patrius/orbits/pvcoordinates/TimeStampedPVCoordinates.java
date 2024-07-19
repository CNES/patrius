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
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:FA:FA-2326:27/05/2020:Orbits - Correction equals & HashCode 
 * VERSION:4.5:FA:FA-2464:27/05/2020:Anomalie dans le calcul du vecteur rotation des LOF
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:489:06/10/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.orbits.pvcoordinates;

import java.util.Collection;
import java.util.function.Function;

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
                                    final Vector3D position, final Vector3D velocity) {
        super(position, velocity);
        this.date = dateIn;
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
        super(a, pv);
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
        super(start, end);
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
        super(a1, pv1, a2, pv2);
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
        super(a1, pv1, a2, pv2, a3, pv3);
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
        super(a1, pv1, a2, pv2, a3, pv3, a4, pv4);
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
     * Return a string representation of this position/velocity pair.
     * 
     * @return string representation of this position/velocity pair
     */
    @Override
    public String toString() {
        final String comma = ", ";
        final StringBuffer buffer = new StringBuffer().append('{').append(this.date).append(", P(")
            .append(this.getPosition().getX())
            .append(comma).append(this.getPosition().getY()).append(comma).append(this.getPosition().getZ())
            .append("), V(")
            .append(this.getVelocity().getX()).append(comma).append(this.getVelocity().getY()).append(comma)
            .append(this.getVelocity().getZ()).append(")");
        if (this.getAcceleration() != null) {
            buffer.append(", A(").append(this.getAcceleration().getX()).append(comma)
                .append(this.getAcceleration().getY()).append(comma)
                .append(this.getAcceleration().getZ()).append(")}");
        } else {
            buffer.append("}");
        }
        return buffer.toString();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object object) {
        // parameters : date
        // x, y, z, xDot, yDot, zDot
        // acceleration
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
        final int result = super.hashCode();
        // An efficient multiplier (JVM optimizes 31 * i as (i << 5) - 1 )
        final int effMult = 31;
        // Good hashcode : it's the same
        // for "equal" orbits, but
        // reasonably sure it's different otherwise.
        return effMult * result + getDate().hashCode();
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
                        },
                        new double[] {
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
                        },
                        new double[] {
                            velocity.getX(), velocity.getY(), velocity.getZ()
                        },
                        new double[] {
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
     * Build an interpolation function for the {@link TimeStampedPVCoordinates}.
     * <p>
     * Note: this interpolation function can produce small numerical differences compared to the
     * {@link #interpolate(AbsoluteDate, CartesianDerivativesFilter, Collection)} method, as both methods don't build
     * the dataset abscissas the same way: this one with the first date of the samples as origin, the other with the
     * interpolated date as origin.
     * </p>
     * 
     * @param samples
     *        The samples of time stamped PV data. It should be sorted.
     * @param indexInf
     *        Inferior index
     * @param indexSup
     *        Superior index
     * @param filter
     *        Filter describing which derivatives to use in {@link TimeStampedPVCoordinates} and interpolation
     * @param computeAcceleration
     *        {@code true} if the acceleration should be computed, {@code false} otherwise
     * @return interpolation function for the {@link TimeStampedPVCoordinates}
     */
    public static Function<AbsoluteDate, TimeStampedPVCoordinates>
        buildInterpolationFunction(final TimeStampedPVCoordinates[] samples,
                                   final int indexInf, final int indexSup,
                                   final CartesianDerivativesFilter filter,
                                   final boolean computeAcceleration) {
        return new HermiteTimeStampedPVCoordinatesInterpolationFunction(samples, indexInf, indexSup, filter,
            computeAcceleration);
    }

    /** Hermite interpolation function for the {@link TimeStampedPVCoordinates}. */
    private static class HermiteTimeStampedPVCoordinatesInterpolationFunction
        implements Function<AbsoluteDate, TimeStampedPVCoordinates> {

        /** Hermite interpolator used to interpolate the {@link TimeStampedPVCoordinates}. */
        private final HermiteInterpolator interpolator;

        /** Filter describing which derivatives to use in {@link TimeStampedPVCoordinates} and interpolation. */
        private final CartesianDerivativesFilter filter;

        /** Indicates if the acceleration should be computed or not. */
        private final boolean computeAcceleration;

        /** Reference date */
        private AbsoluteDate refDate;

        /**
         * Standard constructor.
         * 
         * @param samples
         *        The samples of time stamped PV data. It should be sorted.
         * @param indexInf
         *        Inferior index
         * @param indexSup
         *        Superior index
         * @param filter
         *        Filter describing which derivatives to use in {@link TimeStampedPVCoordinates} and interpolation
         * @param computeAcceleration
         *        {@code true} if the acceleration should be computed, {@code false} otherwise
         */
        public HermiteTimeStampedPVCoordinatesInterpolationFunction(final TimeStampedPVCoordinates[] samples,
                                                                    final int indexInf,
                                                                    final int indexSup,
                                                                    final CartesianDerivativesFilter filter,
                                                                    final boolean computeAcceleration) {

            this.interpolator = new HermiteInterpolator();
            this.filter = filter;
            this.computeAcceleration = computeAcceleration;

            for (int i = indexInf; i < indexSup; i++) {
                final TimeStampedPVCoordinates sample = samples[i];
                final AbsoluteDate date = sample.getDate();
                if (this.refDate == null) {
                    this.refDate = date;
                }

                final Vector3D position = sample.getPosition();
                final Vector3D velocity;
                final Vector3D acceleration;

                final double[] value = new double[] { position.getX(), position.getY(), position.getZ() };
                final double[] derivativeValue;
                final double[] doubleDerivative;

                switch (this.filter) {
                    case USE_P:
                        this.interpolator.addSamplePoint(date.durationFrom(this.refDate), value);
                        break;
                    case USE_PV:
                        velocity = sample.getVelocity();
                        derivativeValue = new double[] { velocity.getX(), velocity.getY(), velocity.getZ() };
                        this.interpolator.addSamplePoint(date.durationFrom(this.refDate), value, derivativeValue);
                        break;
                    case USE_PVA:
                        acceleration = sample.getAcceleration();
                        if (acceleration == null) {
                            throw new IllegalStateException(
                                "The acceleration must be initialized for a PVA CartesianDerivativeFilter");
                        }
                        velocity = sample.getVelocity();
                        derivativeValue = new double[] { velocity.getX(), velocity.getY(), velocity.getZ() };
                        doubleDerivative =
                            new double[] { acceleration.getX(), acceleration.getY(), acceleration.getZ() };
                        this.interpolator.addSamplePoint(date.durationFrom(this.refDate), value, derivativeValue,
                            doubleDerivative);
                        break;
                    default:
                        throw new EnumConstantNotPresentException(CartesianDerivativesFilter.class, "filter");
                }
            }
        }

        /** {@inheritDoc} */
        @Override
        public TimeStampedPVCoordinates apply(final AbsoluteDate date) {
            // The interpolator uses the duration from the reference date to interpolate
            final double duration = date.durationFrom(this.refDate);
            // Compute the value and derivative
            final double[][] values = this.interpolator.valueAndDerivative(duration, this.computeAcceleration);
            final Vector3D p = new Vector3D(values[0]);
            final Vector3D v = new Vector3D(values[1]);

            // Compute the acceleration if needed, then build the TimeStampedPVCoordinates output
            final TimeStampedPVCoordinates out;
            if (this.computeAcceleration) {
                final Vector3D a = new Vector3D(values[2]);
                out = new TimeStampedPVCoordinates(date, p, v, a);
            } else {
                out = new TimeStampedPVCoordinates(date, p, v);
            }

            return out;
        }
    }
}
