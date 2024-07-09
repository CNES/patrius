/**
 * Copyright 2002-2012 CS Systèmes d'Information
 * Copyright 2011-2017 CNES
 *
 * HISTORY
* VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
* VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
 * VERSION:4.5:FA:FA-2326:27/05/2020:Orbits - Correction equals & HashCode 
 * VERSION:4.5:FA:FA-2464:27/05/2020:Anomalie dans le calcul du vecteur rotation des LOF
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
package fr.cnes.sirius.patrius.orbits.pvcoordinates;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.analysis.differentiation.DerivativeStructure;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.FieldVector3D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.time.TimeShiftable;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * <p>
 * Simple container for Position/Velocity/Acceleration triplets.
 * </p>
 * <p>
 * The state can be slightly shifted to close dates. This shift is based on a simple quadratic model. It is <em>not</em>
 * intended as a replacement for proper orbit propagation (it is not even Keplerian!) but should be sufficient for
 * either small time shifts or coarse accuracy.
 * </p>
 * <p>
 * This class is the angular counterpart to {@link AngularCoordinates}.
 * </p>
 * <p>
 * Instances of this class are guaranteed to be immutable.
 * </p>
 * 
 * @author Fabien Maussion
 * @author Luc Maisonobe
 */
@SuppressWarnings("PMD.NullAssignment")
public class PVCoordinates implements TimeShiftable<PVCoordinates>, Serializable {

    /** Fixed position/velocity/acceleration at origin (both p, v and a are zero vectors). */
    public static final PVCoordinates ZERO = new PVCoordinates(Vector3D.ZERO, Vector3D.ZERO, Vector3D.ZERO);
    
    /** Root int for hash code. */
    private static final int ROOTINT = 356;

    /** Serializable UID. */
    private static final long serialVersionUID = 4157449919684833834L;

    /** The position. */
    private final Vector3D position;

    /** The velocity. */
    private final Vector3D velocity;

    /** The acceleration. */
    private final Vector3D acceleration;

    /**
     * Simple constructor.
     * <p>
     * Sets the Coordinates to default : (0 0 0) (0 0 0) (0 0 0).
     * </p>
     */
    public PVCoordinates() {
        this.position = Vector3D.ZERO;
        this.velocity = Vector3D.ZERO;
        this.acceleration = Vector3D.ZERO;
    }

    /**
     * Builds a PVCoordinates triplet with zero acceleration.
     *
     * @param x the x component (m)
     * @param y the y component (m)
     * @param z the z component (m)
     * @param vx the vx component (m/s)
     * @param vy the vy component (m/s)
     * @param vz the vz component (m/s)
     *
     */
    public PVCoordinates(final double x, final double y, final double z, final double vx,
            final double vy, final double vz) {
        this(new Vector3D(x, y, z), new Vector3D(vx, vy, vz));
    }

    /**
     * Builds a PVCoordinates triplet with zero acceleration.
     * 
     * @param positionIn
     *        the position vector (m)
     * @param velocityIn
     *        the velocity vector (m/s)
     */
    public PVCoordinates(final Vector3D positionIn, final Vector3D velocityIn) {
        this.position = positionIn;
        this.velocity = velocityIn;
        this.acceleration = null;
    }

    /**
     * Builds a PVCoordinates triplet.
     * 
     * @param positionIn
     *        the position vector (m)
     * @param velocityIn
     *        the velocity vector (m/s)
     * @param accelerationIn
     *        the acceleration vector (m/s²)
     */
    public PVCoordinates(final Vector3D positionIn, final Vector3D velocityIn, final Vector3D accelerationIn) {
        this.position = positionIn;
        this.velocity = velocityIn;
        this.acceleration = accelerationIn;
    }

    /**
     * Multiplicative constructor.
     * <p>
     * Build a PVCoordinates from another one and a scale factor.
     * </p>
     * <p>
     * The PVCoordinates built will be a * pv
     * </p>
     * 
     * @param a
     *        scale factor
     * @param pv
     *        base (unscaled) PVCoordinates
     */
    public PVCoordinates(final double a, final PVCoordinates pv) {
        this.position = new Vector3D(a, pv.position);
        this.velocity = new Vector3D(a, pv.velocity);
        this.acceleration = pv.acceleration == null ? null : new Vector3D(a, pv.acceleration);
    }

    /**
     * Subtractive constructor.
     * <p>
     * Build a relative PVCoordinates from a start and an end position.
     * </p>
     * <p>
     * The PVCoordinates built will be end - start.
     * </p>
     * 
     * @param start
     *        Starting PVCoordinates
     * @param end
     *        ending PVCoordinates
     */
    public PVCoordinates(final PVCoordinates start, final PVCoordinates end) {
        this.position = end.position.subtract(start.position);
        this.velocity = end.velocity.subtract(start.velocity);
        this.acceleration = start.acceleration == null || end.acceleration == null ? null : end.acceleration
                .subtract(start.acceleration);
    }

    /**
     * Linear constructor.
     * <p>
     * Build a PVCoordinates from two other ones and corresponding scale factors.
     * </p>
     * <p>
     * The PVCoordinates built will be a1 * u1 + a2 * u2
     * </p>
     * 
     * @param a1
     *        first scale factor
     * @param pv1
     *        first base (unscaled) PVCoordinates
     * @param a2
     *        second scale factor
     * @param pv2
     *        second base (unscaled) PVCoordinates
     */
    public PVCoordinates(final double a1, final PVCoordinates pv1,
        final double a2, final PVCoordinates pv2) {
        this.position = new Vector3D(a1, pv1.position, a2, pv2.position);
        this.velocity = new Vector3D(a1, pv1.velocity, a2, pv2.velocity);
        this.acceleration = pv1.acceleration == null || pv2.acceleration == null ? null : new Vector3D(a1,
                pv1.acceleration, a2, pv2.acceleration);
    }

    /**
     * Linear constructor.
     * <p>
     * Build a PVCoordinates from three other ones and corresponding scale factors.
     * </p>
     * <p>
     * The PVCoordinates built will be a1 * u1 + a2 * u2 + a3 * u3
     * </p>
     * 
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
    public PVCoordinates(final double a1, final PVCoordinates pv1,
        final double a2, final PVCoordinates pv2,
        final double a3, final PVCoordinates pv3) {
        this.position = new Vector3D(a1, pv1.position, a2, pv2.position, a3, pv3.position);
        this.velocity = new Vector3D(a1, pv1.velocity, a2, pv2.velocity, a3, pv3.velocity);
        this.acceleration = pv1.acceleration == null || pv2.acceleration == null || pv3.acceleration == null ? null
                : new Vector3D(a1, pv1.acceleration, a2, pv2.acceleration, a3, pv3.acceleration);
    }

    /**
     * Linear constructor.
     * <p>
     * Build a PVCoordinates from four other ones and corresponding scale factors.
     * </p>
     * <p>
     * The PVCoordinates built will be a1 * u1 + a2 * u2 + a3 * u3 + a4 * u4
     * </p>
     * 
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
    public PVCoordinates(final double a1, final PVCoordinates pv1,
        final double a2, final PVCoordinates pv2,
        final double a3, final PVCoordinates pv3,
        final double a4, final PVCoordinates pv4) {
        this.position = new Vector3D(a1, pv1.position, a2, pv2.position,
            a3, pv3.position, a4, pv4.position);
        this.velocity = new Vector3D(a1, pv1.velocity, a2, pv2.velocity,
            a3, pv3.velocity, a4, pv4.velocity);
        this.acceleration = pv1.acceleration == null || pv2.acceleration == null || pv3.acceleration == null
                || pv4.acceleration == null ? null : new Vector3D(a1, pv1.acceleration, a2, pv2.acceleration, a3,
                pv3.acceleration, a4, pv4.acceleration);
    }

    /**
     * Builds a PVCoordinates triplet from a {@link FieldVector3D}&lt;{@link DerivativeStructure}&gt;.
     * <p>
     * The vector components must have time as their only derivation parameter and have consistent derivation orders.
     * </p>
     * 
     * @param p
     *        vector with time-derivatives embedded within the coordinates
     */
    public PVCoordinates(final FieldVector3D<DerivativeStructure> p) {
        this.position = new Vector3D(p.getX().getReal(), p.getY().getReal(), p.getZ().getReal());
        if (p.getX().getOrder() >= 1) {
            this.velocity = new Vector3D(p.getX().getPartialDerivative(1),
                p.getY().getPartialDerivative(1),
                p.getZ().getPartialDerivative(1));
            if (p.getX().getOrder() >= 2) {
                this.acceleration = new Vector3D(p.getX().getPartialDerivative(2),
                    p.getY().getPartialDerivative(2),
                    p.getZ().getPartialDerivative(2));
            } else {
                this.acceleration = Vector3D.ZERO;
            }
        } else {
            this.velocity = Vector3D.ZERO;
            this.acceleration = Vector3D.ZERO;
        }
    }

    /**
     * Transform the instance to a {@link FieldVector3D}&lt;{@link DerivativeStructure}&gt;.
     * <p>
     * The {@link DerivativeStructure} coordinates correspond to time-derivatives up to the user-specified order.
     * </p>
     * 
     * @param order
     *        derivation order for the vector components
     * @return vector with time-derivatives embedded within the coordinates
     * @exception PatriusException
     *            if the user specified order is too large
     */
    public FieldVector3D<DerivativeStructure> toDerivativeStructureVector(final int order) throws PatriusException {

        // Initialization
        final DerivativeStructure x;
        final DerivativeStructure y;
        final DerivativeStructure z;
        switch (order) {
            case 0:
                x = new DerivativeStructure(1, 0, this.position.getX());
                y = new DerivativeStructure(1, 0, this.position.getY());
                z = new DerivativeStructure(1, 0, this.position.getZ());
                break;
            case 1:
                x = new DerivativeStructure(1, 1, this.position.getX(), this.velocity.getX());
                y = new DerivativeStructure(1, 1, this.position.getY(), this.velocity.getY());
                z = new DerivativeStructure(1, 1, this.position.getZ(), this.velocity.getZ());
                break;
            case 2:
                x = new DerivativeStructure(1, 2, this.position.getX(), this.velocity.getX(), this.acceleration.getX());
                y = new DerivativeStructure(1, 2, this.position.getY(), this.velocity.getY(), this.acceleration.getY());
                z = new DerivativeStructure(1, 2, this.position.getZ(), this.velocity.getZ(), this.acceleration.getZ());
                break;
            default:
                // Not available
                throw new PatriusException(PatriusMessages.OUT_OF_RANGE_DERIVATION_ORDER, order);
        }

        // Return result
        //
        return new FieldVector3D<DerivativeStructure>(x, y, z);

    }

    /**
     * Estimate velocity between two positions.
     * <p>
     * Estimation is based on a simple fixed velocity translation during the time interval between the two positions.
     * </p>
     * 
     * @param start
     *        start position
     * @param end
     *        end position
     * @param dt
     *        time elapsed between the dates of the two positions
     * @return velocity allowing to go from start to end positions
     */
    public static Vector3D estimateVelocity(final Vector3D start, final Vector3D end, final double dt) {
        final double scale = 1.0 / dt;
        return new Vector3D(scale, end, -scale, start);
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
     * @see fr.cnes.sirius.patrius.time.AbsoluteDate#shiftedBy(double)
     * @see fr.cnes.sirius.patrius.attitudes.Attitude#shiftedBy(double)
     * @see fr.cnes.sirius.patrius.orbits.Orbit#shiftedBy(double)
     * @see fr.cnes.sirius.patrius.propagation.SpacecraftState#shiftedBy(double)
     */
    @Override
    public PVCoordinates shiftedBy(final double dt) {
        final Vector3D acc = this.acceleration == null ? Vector3D.ZERO : this.acceleration;
        return new PVCoordinates(new Vector3D(1, this.position, dt, this.velocity, dt * dt / 2., acc),
            new Vector3D(1, this.velocity, dt, acc),
            this.acceleration);
    }

    /**
     * Gets the position.
     * 
     * @return the position vector (m).
     */
    public Vector3D getPosition() {
        return this.position;
    }

    /**
     * Gets the velocity.
     * 
     * @return the velocity vector (m/s).
     */
    public Vector3D getVelocity() {
        return this.velocity;
    }

    /**
     * Gets the acceleration.
     * 
     * @return the acceleration vector (m/s²).
     */
    public Vector3D getAcceleration() {
        return this.acceleration;
    }

    /**
     * Gets the momentum.
     * <p>
     * This vector is the p &otimes; v where p is position, v is velocity and &otimes; is cross product. To get the real
     * physical angular momentum you need to multiply this vector by the mass.
     * </p>
     * <p>
     * The returned vector is recomputed each time this method is called, it is not cached.
     * </p>
     * 
     * @return a new instance of the momentum vector (m<sup>2</sup>/s).
     */
    public Vector3D getMomentum() {
        return Vector3D.crossProduct(this.position, this.velocity);
    }

    /**
     * Get the angular velocity (spin) of this point as seen from the origin.
     * <p/>
     * The angular velocity vector is parallel to the {@link #getMomentum() angular
     * momentum} and is computed by ω = p &times; v / ||p||²
     * 
     * @return the angular velocity vector
     * @see <a href="http://en.wikipedia.org/wiki/Angular_velocity">Angular Velocity on Wikipedia</a>
     */
    public Vector3D getAngularVelocity() {
        return this.getMomentum().scalarMultiply(1.0 / this.getPosition().getNormSq());
    }

    /**
     * Get the opposite of the instance.
     * 
     * @return a new position-velocity which is opposite to the instance
     */
    public PVCoordinates negate() {
        final Vector3D negAcc = this.acceleration == null ? null : this.acceleration.negate();
        return new PVCoordinates(this.position.negate(), this.velocity.negate(), negAcc);
    }

    /**
     * Compute the cross-product of two instances.
     * 
     * @param pv1
     *        first instances
     * @param pv2
     *        second instances
     * @return the cross product v1 ^ v2 as a new instance
     */
    public static PVCoordinates crossProduct(final PVCoordinates pv1, final PVCoordinates pv2) {
        final Vector3D p1 = pv1.position;
        final Vector3D v1 = pv1.velocity;
        final Vector3D a1 = pv1.acceleration;
        final Vector3D p2 = pv2.position;
        final Vector3D v2 = pv2.velocity;
        final Vector3D a2 = pv2.acceleration;
        final Vector3D resP = Vector3D.crossProduct(p1, p2);
        final Vector3D resV = new Vector3D(1, Vector3D.crossProduct(p1, v2), 1, Vector3D.crossProduct(v1, p2));
        final Vector3D resA = pv1.acceleration == null || pv2.acceleration == null ? null : new Vector3D(1,
                Vector3D.crossProduct(p1, a2), 2, Vector3D.crossProduct(v1, v2), 1, Vector3D.crossProduct(a1, p2));
        return new PVCoordinates(resP, resV, resA);
    }

    /**
     * Normalize the position part of the instance.
     * <p>
     * The computed coordinates first component (position) will be a normalized vector, the second component (velocity)
     * will be the derivative of the first component (hence it will generally not be normalized), and the third
     * component (acceleration) will be the derivative of the second component (hence it will generally not be
     * normalized).
     * </p>
     * 
     * @return a new instance, with first component normalized and
     *         remaining component computed to have consistent derivatives
     */
    public PVCoordinates normalize() {
        // Pos, vel
        final double inv = 1.0 / this.position.getNorm();
        final Vector3D u = new Vector3D(inv, this.position);
        final Vector3D v = new Vector3D(inv, this.velocity);
        final double uv = Vector3D.dotProduct(u, v);
        final double v2 = Vector3D.dotProduct(v, v);
        final Vector3D uDot = new Vector3D(1, v, -uv, u);
        // Acceleration
        Vector3D uDotDot = null;
        if (this.acceleration != null) {
            final Vector3D w = new Vector3D(inv, this.acceleration);
            final double uw = Vector3D.dotProduct(u, w);
            uDotDot = new Vector3D(1, w, -uv - uv, v, 3 * uv * uv - v2 - uw, u);
        }
        // Return result
        return new PVCoordinates(u, uDot, uDotDot);
    }

    /**
     * Get the vector PV coordinates as a dimension 9 or 6 array (if the acceleration is or is not
     * included).
     *
     * @param withAcceleration
     *        Indicates if the acceleration data should be included (length = 9) or not (length = 6)
     * @return vector PV coordinates
     * @throws IllegalStateException if the acceleration should be returned but it is not
     *         initialized
     */
    public double[] toArray(final boolean withAcceleration) {
        final double[] array;
        if (withAcceleration) {
            if (this.acceleration == null) {
                throw PatriusException
                        .createIllegalStateException(PatriusMessages.ACCELERATION_NOT_INITIALIZED);
            }
            array = new double[] { this.position.getX(), this.position.getY(),
                    this.position.getZ(), this.velocity.getX(), this.velocity.getY(),
                    this.velocity.getZ(), this.acceleration.getX(), this.acceleration.getY(),
                    this.acceleration.getZ() };
        } else {
            array = new double[] { this.position.getX(), this.position.getY(),
                    this.position.getZ(), this.velocity.getX(), this.velocity.getY(),
                    this.velocity.getZ() };
        }
        return array;
    }

    /**
     * Return a string representation of this position/velocity/acceleration triplet.
     * 
     * @return string representation of this position/velocity/acceleration triplet
     */
    @Override
    public String toString() {
        final String comma = ", ";
        final StringBuffer buffer = new StringBuffer().append('{').append("P(").
            append(this.position.getX()).append(comma).
            append(this.position.getY()).append(comma).
            append(this.position.getZ()).append("), V(").
            append(this.velocity.getX()).append(comma).
            append(this.velocity.getY()).append(comma).
            append(this.velocity.getZ()).append(")");
        if (this.acceleration != null) {
            buffer.append(", A(").
            append(this.acceleration.getX()).append(comma).
            append(this.acceleration.getY()).append(comma).
            append(this.acceleration.getZ()).append(")}");
        } else {
            buffer.append("}");
        }
        return buffer.toString();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object object) {
        // parameters : x, y, z, xDot, yDot, zDot
        //              acceleration
        boolean isEqual = true;
        
        if (object == this) { 
            isEqual = true; 
        } else if (object instanceof PVCoordinates) {
            final PVCoordinates other = (PVCoordinates) object;

            // PV Coordinates
            isEqual &= (getPosition().equals(other.getPosition()));
            isEqual &= (getVelocity().equals(other.getVelocity()));
            if (this.acceleration != null) {
                isEqual &= (getAcceleration().equals(other.getAcceleration()));
            } else {
                isEqual &= other.getAcceleration() == null;
            }
            
        } else {
            isEqual = false;
        }
        
        return isEqual;
    }
        
    /** {@inheritDoc} */
    @Override 
    public int hashCode() { 
        
        // A not zero random "root int"
        int result = ROOTINT;
        // An efficient multiplier (JVM optimizes 31 * i as (i << 5) - 1 )
        final int effMult = 31;
        // Good hashcode : it's the same
        // for "equal" orbits, but
        // reasonably sure it's different otherwise.
        result = effMult * result + getPosition().hashCode();
        result = effMult * result + getVelocity().hashCode();
        if (this.acceleration != null) {
            result = effMult * result + getAcceleration().hashCode();
        }
 
        return result; 
    }
}
