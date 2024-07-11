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
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.8:DM:DM-2992:15/11/2021:[PATRIUS] Possibilite d'interpoler l'attitude en ignorant les rotations rate 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:95:12/07/2013:Fixed Attitude bug - spin parameter taken as is (same for both conventions)
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:319:05/03/2015:Corrected Rotation class (Step1)
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:489:06/10/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::DM:455:05/11/2015:add Slerp method
 * VERSION::FA:559:26/02/2016:minor corrections
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:603:29/08/2016:deleted deprecated methods and classes in package attitudes
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.complex.Quaternion;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeInterpolable;
import fr.cnes.sirius.patrius.time.TimeShiftable;
import fr.cnes.sirius.patrius.time.TimeStamped;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.AngularDerivativesFilter;
import fr.cnes.sirius.patrius.utils.TimeStampedAngularCoordinates;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class handles attitude definition at a given date.
 * 
 * <p>
 * This class represents the rotation from a reference frame to a "frame of interest", as well as its spin (axis and
 * rotation rate).
 * </p>
 * <p>
 * The angular coordinates describe the orientation and angular velocity of the frame of interest in the reference
 * frame.
 * </p>
 * <p>
 * Consequently, defining xSat_Rsat = Vector3D.PLUS_I, one can compute xSat_Rref = rot.applyTo(xSat_Rsat).
 * </p>
 * <p>
 * The state can be slightly shifted to close dates. This shift is based on a linear extrapolation for attitude taking
 * the spin rate into account. It is <em>not</em> intended as a replacement for proper attitude propagation but should
 * be sufficient for either small time shifts or coarse accuracy.
 * </p>
 * <p>
 * The instance <code>Attitude</code> is guaranteed to be immutable.
 * </p>
 * 
 * @see fr.cnes.sirius.patrius.orbits.Orbit
 * @see AttitudeProvider
 * @author V&eacute;ronique Pommier-Maurussane
 */
public class Attitude implements TimeStamped, TimeShiftable<Attitude>, TimeInterpolable<Attitude>, Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = -947817502698754209L;

    /** Quaternion length. */
    private static final int QUAT_LENGTH = 4;

    /** Vector length. */
    private static final int VECT_LENGTH = 3;

    /** Reference frame. */
    private final Frame referenceFrame;

    /** Attitude, spin and spin derivatives. */
    private final TimeStampedAngularCoordinates orientation;

    /**
     * Creates a new instance.
     * 
     * @param referenceFrameIn
     *        reference frame from which attitude is defined
     * @param orientationIn
     *        complete orientation between reference frame and satellite frame,
     *        including rotation rate and rotation acceleration
     */
    public Attitude(final Frame referenceFrameIn, final TimeStampedAngularCoordinates orientationIn) {
        this.referenceFrame = referenceFrameIn;
        this.orientation = orientationIn;
    }

    /**
     * Creates a new instance.
     * 
     * The angular coordinates describe the orientation and angular velocity of the frame of interest
     * in the reference frame.
     * 
     * @param date
     *        date at which attitude is defined
     * @param referenceFrameIn
     *        reference frame from which attitude is defined
     * @param orientationIn
     *        complete orientation from reference frame to the frame of interest, including rotation rate
     */
    public Attitude(final AbsoluteDate date, final Frame referenceFrameIn, final AngularCoordinates orientationIn) {
        this(referenceFrameIn, new TimeStampedAngularCoordinates(date, orientationIn.getRotation(),
            orientationIn.getRotationRate(), orientationIn.getRotationAcceleration()));
    }

    /**
     * Creates a new instance.
     * 
     * @param date
     *        date at which attitude is defined
     * @param referenceFrameIn
     *        reference frame from which attitude is defined
     * @param attitude
     *        rotation between reference frame and satellite frame
     * @param spin
     *        satellite spin (axis and velocity, in <strong>satellite</strong> frame)
     * @param acceleration
     *        satellite rotation acceleration (in <strong>satellite</strong> frame)
     */
    public Attitude(final AbsoluteDate date, final Frame referenceFrameIn,
                    final Rotation attitude, final Vector3D spin, final Vector3D acceleration) {
        this(referenceFrameIn, new TimeStampedAngularCoordinates(date, attitude, spin, acceleration));
    }

    /**
     * Creates a new instance.
     * 
     * @param date
     *        date at which attitude is defined
     * @param referenceFrameIn
     *        reference frame from which attitude is defined
     * @param attitude
     *        rotation between reference frame and satellite frame
     * @param spin
     *        satellite spin (axis and velocity, in <strong>satellite</strong> frame)
     */
    public Attitude(final AbsoluteDate date, final Frame referenceFrameIn,
                    final Rotation attitude, final Vector3D spin) {
        this(referenceFrameIn, new TimeStampedAngularCoordinates(date, attitude, spin, Vector3D.ZERO));
    }

    /**
     * Creates a new instance from an array containing a quaternion and a spin vector.
     * 
     * The quaternion describe the orientation of the frame of interest
     * in the reference frame.
     * 
     * @param y
     *        attitude representation in the state vector (quaternion + spin vector)
     * @param date
     *        date at which attitude is defined
     * @param referenceFrameIn
     *        reference frame from which attitude is defined
     */
    public Attitude(final double[] y, final AbsoluteDate date, final Frame referenceFrameIn) {
        this(date, referenceFrameIn, new Rotation(true, y[0], y[1], y[2], y[3]), new Vector3D(Arrays.copyOfRange(y,
            QUAT_LENGTH, QUAT_LENGTH + VECT_LENGTH)), Vector3D.ZERO);
    }

    /**
     * Get a time-shifted attitude.
     * <p>
     * The state can be slightly shifted to close dates. This shift is based on a linear extrapolation for attitude
     * taking the spin rate into account. It is <em>not</em> intended as a replacement for proper attitude propagation
     * but should be sufficient for either small time shifts or coarse accuracy. This method does not take into account
     * the derivatives of spin: the new attitude does not contain the spin derivatives.
     * </p>
     * 
     * @param dt
     *        time shift in seconds
     * @return a new attitude, shifted with respect to the instance (which is immutable)
     */
    @Override
    public Attitude shiftedBy(final double dt) {
        return new Attitude(this.referenceFrame, this.orientation.shiftedBy(dt));
    }

    /**
     * Get a similar attitude with a specific reference frame.
     * <p>
     * If the instance reference frame is already the specified one, the instance itself is returned without any object
     * creation. Otherwise, a new instance will be created with the specified reference frame. In this case, the
     * required intermediate rotation and spin between the specified and the original reference frame will be inserted.
     * <br>
     * The spin derivatives are not transformed in the specified reference frame: the spinDerivatives attribute of the
     * new instance is null, unless the reference frame is already the specified one (in that case, the spinDerivatives
     * attribute itself is returned with the attitude).
     * </p>
     * 
     * @param newReferenceFrame
     *        desired reference frame for attitude
     * @param spinDerivativesComputation
     *        true if spin derivatives should be computed. If not, spin derivative is set to <i>null</i>
     * @return an attitude that has the same orientation and motion as the instance, but guaranteed to have the
     *         specified reference frame
     * @exception PatriusException
     *            if conversion between reference frames fails
     */
    public final Attitude withReferenceFrame(final Frame newReferenceFrame,
            final boolean spinDerivativesComputation) throws PatriusException {

        if (newReferenceFrame == this.referenceFrame) {
            // simple case, the instance is already compliant
            return this;
        }

        // we have to take an intermediate rotation into account
        final Transform t = newReferenceFrame.getTransformTo(this.referenceFrame, this.orientation.getDate(),
            spinDerivativesComputation);
        final Vector3D acceleration;
        if (spinDerivativesComputation) {
            acceleration = this.orientation.getRotationAcceleration().add(
                this.orientation.getRotation().applyInverseTo(t.getRotationAcceleration()));
        } else {
            acceleration = null;
        }
        return new Attitude(this.orientation.getDate(), newReferenceFrame,
            t.getRotation().applyTo(this.orientation.getRotation()),
            this.orientation.getRotationRate().add(this.orientation.getRotation().applyInverseTo(t.getRotationRate())),
            acceleration);
    }

    /**
     * Get a similar attitude with a specific reference frame.
     * <p>
     * If the instance reference frame is already the specified one, the instance itself is returned without any object
     * creation. Otherwise, a new instance will be created with the specified reference frame. In this case, the
     * required intermediate rotation and spin between the specified and the original reference frame will be inserted.
     * <br>
     * The spin derivatives are not transformed in the specified reference frame: the spinDerivatives attribute of the
     * new instance is null, unless the reference frame is already the specified one (in that case, the spinDerivatives
     * attribute itself is returned with the attitude).
     * </p>
     * 
     * <p>
     * <b>Warning: </b>spin derivative is not computed.
     * </p>
     * 
     * @param newReferenceFrame
     *        desired reference frame for attitude
     * @return an attitude that has the same orientation and motion as the instance, but guaranteed to have the
     *         specified reference frame
     * @exception PatriusException
     *            if conversion between reference frames fails
     */
    public final Attitude withReferenceFrame(final Frame newReferenceFrame) throws PatriusException {
        return this.withReferenceFrame(newReferenceFrame, false);
    }

    /**
     * Get the date of attitude parameters.
     * 
     * @return date of the attitude parameters
     */
    @Override
    public final AbsoluteDate getDate() {
        return this.orientation.getDate();
    }

    /**
     * Get the reference frame.
     * 
     * @return referenceFrame reference frame from which attitude is defined.
     */
    public final Frame getReferenceFrame() {
        return this.referenceFrame;
    }

    /**
     * Get the complete orientation including spin and spin derivatives.
     * 
     * @return complete orientation including spin and spin derivatives
     * @see #getRotation()
     * @see #getSpin()
     * @see #getRotationAcceleration()
     */
    public TimeStampedAngularCoordinates getOrientation() {
        return this.orientation;
    }

    /**
     * Get the attitude rotation.
     * 
     * @return attitude satellite rotation from reference frame.
     * @see #getOrientation()
     * @see #getSpin()
     * 
     */
    public Rotation getRotation() {
        return this.orientation.getRotation();
    }

    /**
     * Get the satellite spin.
     * <p>
     * The spin vector is defined in <strong>satellite</strong> frame.
     * </p>
     * 
     * @return spin satellite spin (axis and velocity).
     * @see #getOrientation()
     * @see #getRotation()
     */
    public Vector3D getSpin() {
        return this.orientation.getRotationRate();
    }

    /**
     * Get the satellite rotation acceleration. May be <i>null</i>
     * <p>
     * The rotation acceleration. vector is defined in <strong>satellite</strong> frame.
     * </p>
     * 
     * @return rotation acceleration
     * @see #getOrientation()
     * @see #getRotation()
     * @throws PatriusException
     *         is the rotation acceleration is not available
     */
    public Vector3D getRotationAcceleration() throws PatriusException {
        return this.orientation.getRotationAcceleration();
    }

    /**
     * {@inheritDoc}
     * <p>
     * The interpolated instance is created by polynomial Hermite interpolation on Rodrigues vector ensuring rotation
     * rate remains the exact derivative of rotation.
     * </p>
     * <p>
     * As this implementation of interpolation is polynomial, it should be used only with small samples (about 10-20
     * points) in order to avoid <a href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's phenomenon</a> and
     * numerical problems (including NaN appearing).
     * </p>
     * <p>
     * <b>Warning: </b>spin derivative is not computed.
     * </p>
     * <p>
     * Note that the state of the current instance may not be used in the interpolation process, only its type and non
     * interpolable fields are used (for example central attraction coefficient or frame when interpolating orbits). The
     * interpolable fields taken into account are taken only from the states of the sample points. So if the state of
     * the instance must be used, the instance should be included in the sample points.
     * </p>
     * <p>
     * The rotation rates will be used for interpolation.
     * </p>
     * 
     * @param interpolationDate
     *        interpolation date
     * @param sample
     *        attitude samples
     * @return interpolated attitude
     */
    @Override
    public Attitude interpolate(final AbsoluteDate interpolationDate,
            final Collection<Attitude> sample) throws PatriusException {
        return this.interpolate(interpolationDate, sample, false);
    }

    /**
     * Get an interpolated instance.
     * <p>
     * The interpolated instance is created by polynomial Hermite interpolation on Rodrigues vector ensuring rotation
     * rate remains the exact derivative of rotation.
     * </p>
     * <p>
     * As this implementation of interpolation is polynomial, it should be used only with small samples (about 10-20
     * points) in order to avoid <a href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's phenomenon</a> and
     * numerical problems (including NaN appearing).
     * </p>
     * <p>
     * <b>Warning: </b>spin derivative is not computed.
     * </p>
     * <p>
     * Note that the state of the current instance may not be used in the interpolation process, only its type and non
     * interpolable fields are used (for example central attraction coefficient or frame when interpolating orbits). The
     * interpolable fields taken into account are taken only from the states of the sample points. So if the state of
     * the instance must be used, the instance should be included in the sample points.
     * </p>
     * <p>
     * The rotation rates might or might not be used for interpolation depending on the filter setting.
     * </p>
     * 
     * @param interpolationDate
     *        interpolation date
     * @param sample
     *        attitude samples
     * @param filter
     *        filter for derivatives from the sample to use in interpolation
     * @return interpolated attitude
     * @throws PatriusException thrown if interpolation failed
     */
    public Attitude interpolate(final AbsoluteDate interpolationDate,
            final Collection<Attitude> sample, final AngularDerivativesFilter filter) throws PatriusException {
        return this.interpolate(interpolationDate, sample, false, filter);
    }

    /**
     * Interpolates attitude.
     * <p>
     * The interpolated instance is created by polynomial Hermite interpolation on Rodrigues vector ensuring rotation
     * rate remains the exact derivative of rotation.
     * </p>
     * <p>
     * As this implementation of interpolation is polynomial, it should be used only with small samples (about 10-20
     * points) in order to avoid <a href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's phenomenon</a> and
     * numerical problems (including NaN appearing).
     * </p>
     * <p>
     * The rotation rates will be used for interpolation.
     * </p>
     * 
     * @param interpolationDate
     *        interpolation date
     * @param sample
     *        attitude samples
     * @param computeSpinDerivatives
     *        true if spin derivatives should be computed. If not, spin derivative is set to <i>null</i>
     * @return interpolated attitude
     * @throws PatriusException
     *         thrown if interpolation failed
     */
    public Attitude interpolate(final AbsoluteDate interpolationDate, final Collection<Attitude> sample,
            final boolean computeSpinDerivatives) throws PatriusException {
        return this.interpolate(interpolationDate, sample, computeSpinDerivatives, AngularDerivativesFilter.USE_RR);
    }

    /**
     * Interpolates attitude.
     * <p>
     * The interpolated instance is created by polynomial Hermite interpolation on Rodrigues vector ensuring rotation
     * rate remains the exact derivative of rotation.
     * </p>
     * <p>
     * As this implementation of interpolation is polynomial, it should be used only with small samples (about 10-20
     * points) in order to avoid <a href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's phenomenon</a> and
     * numerical problems (including NaN appearing).
     * </p>
     * <p>
     * The rotation rates might or might not be used for interpolation depending on the filter setting.
     * </p>
     * 
     * @param interpolationDate
     *        interpolation date
     * @param sample
     *        attitude samples
     * @param computeSpinDerivatives
     *        true if spin derivatives should be computed. If not, spin derivative is set to <i>null</i>
     * @param filter
     *        filter for derivatives from the sample to use in interpolation
     * @return interpolated attitude
     * @throws PatriusException
     *         thrown if interpolation failed
     */
    public Attitude interpolate(final AbsoluteDate interpolationDate, final Collection<Attitude> sample,
            final boolean computeSpinDerivatives, final AngularDerivativesFilter filter) throws PatriusException {
        final List<TimeStampedAngularCoordinates> datedPV = new ArrayList<TimeStampedAngularCoordinates>(sample.size());
        for (final Attitude attitude : sample) {
            datedPV.add(attitude.orientation);
        }
        final TimeStampedAngularCoordinates interpolated =
            TimeStampedAngularCoordinates.interpolate(interpolationDate, filter, datedPV, computeSpinDerivatives);
        return new Attitude(this.referenceFrame, interpolated);
    }

    /**
     * Convert Attitude to state array.
     * 
     * @return the state vector representing the Attitude.
     */
    public double[] mapAttitudeToArray() {
        final double[] array = new double[QUAT_LENGTH + VECT_LENGTH];
        final Quaternion rotation = this.getRotation().getQuaternion();
        array[0] = rotation.getScalarPart();
        System.arraycopy(rotation.getVectorPart(), 0, array, 1, rotation.getVectorPart().length);
        System.arraycopy(this.getSpin().toArray(), 0, array, QUAT_LENGTH, VECT_LENGTH);
        return array;
    }

    /**
     * The slerp interpolation method is efficient but is less accurate than the interpolate method.
     * 
     * @param date
     *        the date to interpolate
     * @param attitude1
     *        the {@link Attitude} of the satellite at the previous date
     * @param attitude2
     *        the {@link Attitude} of the satellite at the next date
     * @param frame
     *        the expression frame
     * @param computeSpinDerivative
     *        true if spin derivatives should be computed. If not, spin derivative is set to <i>null</i>
     * @return the interpolated attitude
     * @throws PatriusException
     *         if conversion between reference frames fails
     */
    public static Attitude slerp(final AbsoluteDate date, final Attitude attitude1, final Attitude attitude2,
            final Frame frame,
            final boolean computeSpinDerivative) throws PatriusException {
        // transformations of the attitudes in the expression frame
        final Attitude init = attitude1.withReferenceFrame(frame, computeSpinDerivative);
        final Attitude end = attitude2.withReferenceFrame(frame, computeSpinDerivative);

        // computes the duration of the maneuver:
        final double h = end.getDate().durationFrom(init.getDate());
        // computes the duration from the beginning of the maneuver to the given date:
        final double k = date.durationFrom(init.getDate());

        final double kOverh = k / h;

        if ((kOverh < 0.) || (kOverh > 1.)) {
            // k / h parameter must be in [0;1] range:
            throw new OutOfRangeException(kOverh, 0, 1);
        }
        // computes the rotation:
        final Rotation initialRot = init.getRotation();
        final Rotation finalRot = end.getRotation();
        final Rotation rotation;
        if (initialRot.isEqualTo(finalRot)) {
            rotation = initialRot;
        } else {
            rotation = Rotation.slerp(initialRot, finalRot, kOverh);
        }
        // computes the spin:
        final Vector3D initialSpin = init.getSpin();
        final Vector3D finalSpin = end.getSpin();
        double x = initialSpin.getX() + kOverh * (finalSpin.getX() - initialSpin.getX());
        double y = initialSpin.getY() + kOverh * (finalSpin.getY() - initialSpin.getY());
        double z = initialSpin.getZ() + kOverh * (finalSpin.getZ() - initialSpin.getZ());
        final Vector3D spin = new Vector3D(x, y, z);

        // computes the spin derivatives:
        Vector3D spinDerivative = null;
        final Vector3D initialDer = init.getRotationAcceleration();
        final Vector3D finalDer = end.getRotationAcceleration();
        if (computeSpinDerivative && initialDer != null && finalDer != null) {
            // linear interpolation of the spin derivatives:
            x = initialDer.getX() + kOverh * (finalDer.getX() - initialDer.getX());
            y = initialDer.getY() + kOverh * (finalDer.getY() - initialDer.getY());
            z = initialDer.getZ() + kOverh * (finalDer.getZ() - initialDer.getZ());
            // Spin derivative
            spinDerivative = new Vector3D(x, y, z);
        }

        // return the new attitude:
        return new Attitude(date, frame, rotation, spin, spinDerivative);
    }
}
