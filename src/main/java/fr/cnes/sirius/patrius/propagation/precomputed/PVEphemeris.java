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
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.precomputed;

import java.util.Arrays;
import java.util.List;

import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.TimeStampedPVCoordinates;
import fr.cnes.sirius.patrius.propagation.AbstractPropagator;
import fr.cnes.sirius.patrius.propagation.BoundedPropagator;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.interpolation.TimeStampedInterpolableEphemeris;
import fr.cnes.sirius.patrius.time.interpolation.TimeStampedInterpolableEphemeris.SearchMethod;
import fr.cnes.sirius.patrius.time.interpolation.TimeStampedInterpolationFunctionBuilder;
import fr.cnes.sirius.patrius.tools.cache.FIFOThreadSafeCache;
import fr.cnes.sirius.patrius.utils.CartesianDerivativesFilter;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * This class is designed to accept and handle tabulated orbital entries described by {@link TimeStampedPVCoordinates
 * time-stamped PVCoordinates}.<br>
 * Tabulated entries are classified and then extrapolated in a way to obtain continuous output, with accuracy and
 * computation methods configured by the user.
 * 
 * <p>
 * Note: This implementation does not support all the methods of the {@link Propagator} interface in the case the
 * provided frame is not pseudo-inertial. In particular, the propagate methods.
 * </p>
 *
 * @author veuillh
 */
public class PVEphemeris extends AbstractPropagator implements BoundedPropagator {

    /** Serializable UID. */
    private static final long serialVersionUID = -2394619088533425296L;

    /** Frame in which the time-stamped PVCoordinates are defined. */
    private final Frame frame;

    /** Central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>). */
    private final double mu;

    /** Interpolable ephemeris (cache for the interpolations). */
    private final TimeStampedInterpolableEphemeris<TimeStampedPVCoordinates, TimeStampedPVCoordinates> ephem;

    /**
     * Standard constructor. This ephemeris doesn't describe an attitude provider.
     * 
     * <p>
     * By default, the dates outside of the optimal interval aren't allowed.
     * </p>
     * 
     * <p>
     * By default, the cache size is set to {@link FIFOThreadSafeCache#DEFAULT_MAX_SIZE DEFAULT_MAX_SIZE}.
     * </p>
     * 
     * <p>
     * By default, the acceleration is computed.
     * </p>
     *
     * @param pvts
     *        Tabulated time-stamped PVCoordinates
     * @param order
     *        Interpolation order (number of points to use for the interpolation). It must be even.
     * @param frame
     *        The frame in which the time-stamped PVCoordinates are defined
     * @param mu
     *        Central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @param filter
     *        Filter describing which derivatives to use in {@link TimeStampedPVCoordinates} and interpolation
     * @throws NullArgumentException
     *         if {@code pvts}, {@code frame} or {@code filter} is null
     * @throws IllegalArgumentException
     *         if the tabulated time-stamped PVCoordinates contained elements defined at the same date
     *         if the order is an odd number or is lower than 2
     *         if the tabulated time-stamped PVCoordinates array length is lower than the order
     */
    public PVEphemeris(final List<TimeStampedPVCoordinates> pvts, final int order, final Frame frame, final double mu,
                       final CartesianDerivativesFilter filter) {
        this(pvts, order, frame, mu, filter, null);
    }

    /**
     * Standard constructor. This ephemeris describes an attitude provider.
     * 
     * <p>
     * By default, the dates outside of the optimal interval aren't allowed.
     * </p>
     * 
     * <p>
     * By default, the cache size is set to {@link FIFOThreadSafeCache#DEFAULT_MAX_SIZE DEFAULT_MAX_SIZE}.
     * </p>
     * 
     * <p>
     * By default, the acceleration is computed.
     * </p>
     * 
     * @param pvts
     *        Tabulated time-stamped PVCoordinates
     * @param order
     *        Interpolation order (number of points to use for the interpolation)
     * @param frame
     *        The frame in which the time-stamped PVCoordinates are defined
     * @param mu
     *        Central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @param filter
     *        Filter describing which derivatives to use in {@link TimeStampedPVCoordinates} and interpolation
     * @param attitudeProvider
     *        Provider for attitude computation in case of both forces and events computation. Can be null.
     * @throws NullArgumentException
     *         if {@code pvts}, {@code frame} or {@code filter} is null
     * @throws IllegalArgumentException
     *         if the tabulated time-stamped PVCoordinates contained elements defined at the same date
     *         if the order is an odd number or is lower than 2
     *         if the tabulated time-stamped PVCoordinates array length is lower than the order
     */
    public PVEphemeris(final List<TimeStampedPVCoordinates> pvts, final int order, final Frame frame, final double mu,
                       final CartesianDerivativesFilter filter, final AttitudeProvider attitudeProvider) {
        this(pvts, order, frame, mu, filter, attitudeProvider, false);
    }

    /**
     * Standard constructor. This ephemeris describes an attitude provider.
     * 
     * <p>
     * By default, the cache size is set to {@link FIFOThreadSafeCache#DEFAULT_MAX_SIZE DEFAULT_MAX_SIZE}.
     * </p>
     * 
     * <p>
     * By default, the acceleration is computed.
     * </p>
     * 
     * @param pvts
     *        Tabulated time-stamped PVCoordinates
     * @param order
     *        Interpolation order (number of points to use for the interpolation)
     * @param frame
     *        The frame in which the time-stamped PVCoordinates are defined
     * @param mu
     *        Central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @param filter
     *        Filter describing which derivatives to use in {@link TimeStampedPVCoordinates} and interpolation
     * @param attitudeProvider
     *        Provider for attitude computation in case of both forces and events computation. Can be null.
     * @param acceptOutOfOptimalRange
     *        Indicates whether accept dates outside of the optimal interval which is a sub-interval from the full
     *        interval required for interpolation with respect to the interpolation order
     * @throws NullArgumentException
     *         if {@code pvts}, {@code frame} or {@code filter} is null
     * @throws IllegalArgumentException
     *         if the tabulated time-stamped PVCoordinates contained elements defined at the same date
     *         if the order is an odd number or is lower than 2
     *         if the tabulated time-stamped PVCoordinates array length is lower than the order
     */
    public PVEphemeris(final List<TimeStampedPVCoordinates> pvts, final int order, final Frame frame,
                       final double mu, final CartesianDerivativesFilter filter,
                       final AttitudeProvider attitudeProvider,
                       final boolean acceptOutOfOptimalRange) {
        this(pvts, order, frame, mu, filter, attitudeProvider, acceptOutOfOptimalRange,
                FIFOThreadSafeCache.DEFAULT_MAX_SIZE);
    }

    /**
     * Standard constructor. This ephemeris describes an attitude provider.
     * 
     * <p>
     * By default, the acceleration is computed.
     * </p>
     * 
     * @param pvts
     *        Tabulated time-stamped PVCoordinates
     * @param order
     *        Interpolation order (number of points to use for the interpolation)
     * @param frame
     *        The frame in which the time-stamped PVCoordinates are defined
     * @param mu
     *        Central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @param filter
     *        Filter describing which derivatives to use in {@link TimeStampedPVCoordinates} and interpolation
     * @param attitudeProvider
     *        Provider for attitude computation in case of both forces and events computation. Can be null.
     * @param acceptOutOfOptimalRange
     *        Indicates whether accept dates outside of the optimal interval which is a sub-interval from the full
     *        interval required for interpolation with respect to the interpolation order
     * @param cacheSize
     *        The size of the cache. 0 is a legitimate value emulating the absence of cache.
     * @throws NullArgumentException
     *         if {@code pvts}, {@code frame} or {@code filter} is null
     * @throws IllegalArgumentException
     *         if the tabulated time-stamped PVCoordinates contained elements defined at the same date
     *         if the order is an odd number or is lower than 2
     *         if the tabulated time-stamped PVCoordinates array length is lower than the order
     */
    public PVEphemeris(final List<TimeStampedPVCoordinates> pvts, final int order, final Frame frame,
                       final double mu, final CartesianDerivativesFilter filter,
                       final AttitudeProvider attitudeProvider,
                       final boolean acceptOutOfOptimalRange, final int cacheSize) {
        this(pvts, order, frame, mu, filter, attitudeProvider, acceptOutOfOptimalRange, cacheSize, true);
    }

    /**
     * Full constructor.
     * 
     * @param pvts
     *        Tabulated time-stamped PVCoordinates
     * @param order
     *        Interpolation order (number of points to use for the interpolation)
     * @param frame
     *        The frame in which the time-stamped PVCoordinates are defined
     * @param mu
     *        Central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @param filter
     *        Filter describing which derivatives to use in {@link TimeStampedPVCoordinates} and interpolation
     * @param attitudeProvider
     *        Provider for attitude computation in case of both forces and events computation. Can be null.
     * @param acceptOutOfOptimalRange
     *        Indicates whether accept dates outside of the optimal interval which is a sub-interval from the full
     *        interval required for interpolation with respect to the interpolation order
     * @param cacheSize
     *        The size of the cache. 0 is a legitimate value emulating the absence of cache.
     * @param computeAcceleration
     *        {@code true} if the acceleration should be computed, {@code false} otherwise
     * @throws NullArgumentException
     *         if {@code pvts}, {@code frame} or {@code filter} is null
     * @throws IllegalArgumentException
     *         if the tabulated time-stamped PVCoordinates contained elements defined at the same date
     *         if the order is an odd number or is lower than 2
     *         if the tabulated time-stamped PVCoordinates array length is lower than the order
     * @throws NotPositiveException
     *         if {@code cacheSize < 0}
     */
    public PVEphemeris(final List<TimeStampedPVCoordinates> pvts, final int order, final Frame frame,
                       final double mu, final CartesianDerivativesFilter filter,
                       final AttitudeProvider attitudeProvider,
                       final boolean acceptOutOfOptimalRange, final int cacheSize, final boolean computeAcceleration) {
        super(attitudeProvider);

        if (pvts == null || frame == null || filter == null) {
            throw new NullArgumentException(PatriusMessages.NULL_NOT_ALLOWED, pvts, frame, filter);
        }

        this.frame = frame;
        this.mu = mu;
        final TimeStampedPVCoordinates[] pvtsArray = pvts.toArray(new TimeStampedPVCoordinates[pvts.size()]);
        Arrays.sort(pvtsArray, (pvt1, pvt2) -> {
            final int comp = pvt1.getDate().compareTo(pvt2.getDate());
            if (comp == 0) {
                throw new IllegalArgumentException(pvt1.getDate() + "is stored several times");
            }
            return comp;
        });

        final TimeStampedInterpolationFunctionBuilder<TimeStampedPVCoordinates, TimeStampedPVCoordinates> 
            interpolationFunctionBuilder =
                (samples, indexInf, indexSup) -> TimeStampedPVCoordinates.buildInterpolationFunction(samples, indexInf,
                    indexSup, filter, computeAcceleration);
        this.ephem = new TimeStampedInterpolableEphemeris<>(pvtsArray, order, interpolationFunctionBuilder,
            acceptOutOfOptimalRange, false, false, cacheSize);
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IllegalStateException
     *         if the date is outside the supported interval
     *         if the instance has been built with the setting {@code acceptOutOfOptimalRange = false} and the date is
     *         outside the optimal interval which is a sub-interval from the full interval interval required for
     *         interpolation with respect to the interpolation order
     */
    @Override
    public Orbit propagateOrbit(final AbsoluteDate date) {
        final TimeStampedPVCoordinates pvt = this.ephem.interpolate(date);
        return new CartesianOrbit(pvt, this.frame, date, this.mu);
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IllegalStateException
     *         if the date is outside the supported interval
     *         if the instance has been built with the setting {@code acceptOutOfOptimalRange = false} and the date is
     *         outside the optimal interval which is a sub-interval from the full interval interval required for
     *         interpolation with respect to the interpolation order
     */
    @Override
    public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frameOut) throws PatriusException {
        TimeStampedPVCoordinates pv = this.ephem.interpolate(date);
        // If needed, convert position, velocity to the right frame
        if ((frameOut != null) && (this.frame != frameOut)) {
            final Transform t = this.frame.getTransformTo(frameOut, date);
            pv = t.transformPVCoordinates(pv);
        }
        return pv;
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IllegalStateException
     *         if the date is outside the supported interval
     *         if the instance has been built with the setting {@code acceptOutOfOptimalRange = false} and the date is
     *         outside the optimal interval which is a sub-interval from the full interval interval required for
     *         interpolation with respect to the interpolation order
     */
    @Override
    public SpacecraftState getSpacecraftState(final AbsoluteDate date) throws PropagationException {
        return this.basicPropagate(date);
    }

    /**
     * Try (and fail) to reset the initial state.
     * <p>
     * This method always throws an exception, as ephemeris cannot be reset.
     * </p>
     *
     * @param state
     *        New initial state to consider
     * @exception PropagationException
     *            always thrown as ephemeris cannot be reset
     */
    @Override
    public void resetInitialState(final SpacecraftState state) throws PropagationException {
        throw new PropagationException(PatriusMessages.NON_RESETABLE_STATE);
    }

    /** {@inheritDoc} */
    @Override
    public SpacecraftState getInitialState() throws PropagationException {
        return this.basicPropagate(this.getMinDate());
    }

    /**
     * Getter for the first time-stamped PVCoordinates.
     * 
     * @return the first time-stamped PVCoordinates
     */
    public TimeStampedPVCoordinates getFirstTimeStampedPVCoordinates() {
        return this.ephem.getFirstSample();
    }

    /**
     * Getter for the last time-stamped PVCoordinates.
     * 
     * @return the last time-stamped PVCoordinates
     */
    public TimeStampedPVCoordinates getLastTimeStampedPVCoordinates() {
        return this.ephem.getLastSample();
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getMinDate() {
        return this.ephem.getFirstDate();
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getMaxDate() {
        return this.ephem.getLastDate();
    }

    /**
     * Getter for the time-stamped PVCoordinates size.
     * 
     * @return the time-stamped PVCoordinates size
     */
    public int getTimeStampedPVCoordinatesSize() {
        return this.ephem.getSampleSize();
    }

    /**
     * Getter for the time-stamped PVCoordinates array.
     * 
     * @param copy
     *        if {@code true} return a copy of the time-stamped PVCoordinates array, otherwise return the stored array
     * @return the time-stamped PVCoordinates array
     */
    public TimeStampedPVCoordinates[] getTimeStampedPVCoordinates(final boolean copy) {
        return this.ephem.getSamples(copy);
    }

    /**
     * Provides the ratio of reusability of the internal cache. This method can help to chose the size of the cache.
     * 
     * @return the reusability ratio (0 means no reusability at all, 0.5 means that the supplier is called only half
     *         time compared to computeIf method)
     */
    public double getCacheReusabilityRatio() {
        return this.ephem.getCacheReusabilityRatio();
    }

    /**
     * Indicates whether accept dates outside of the optimal interval which is a sub-interval from the full interval
     * interval required for interpolation with respect to the interpolation order.
     *
     * @return {@code true} if the dates outside of the optimal interval are accepted, {@code false} otherwise
     */
    public boolean isAcceptOutOfOptimalRange() {
        return this.ephem.isAcceptOutOfOptimalRange();
    }

    /**
     * Getter for the search method.
     *
     * @return the search method
     */
    public SearchMethod getSearchMethod() {
        return this.ephem.getSearchMethod();
    }

    /**
     * Setter for the search method.
     *
     * @param searchMethod
     *        the search method to set
     * @throws NullArgumentException
     *         if {@code searchMethod} is null
     */
    public void setSearchMethod(final SearchMethod searchMethod) {
        this.ephem.setSearchMethod(searchMethod);
    }

    /**
     * Getter for the frame in which the time-stamped PVCoordinates are defined.
     * 
     * @return frame in which the time-stamped PVCoordinates are defined.
     */
    @Override
    public Frame getFrame() {
        return this.frame;
    }

    /**
     * Getter for the frame in which the time-stamped PVCoordinates are defined.
     * 
     * @return frame in which the time-stamped PVCoordinates are defined.
     */
    @Override
    public Frame getNativeFrame(final AbsoluteDate date, final Frame frameIn) {
        return this.getFrame();
    }

    /**
     * Set propagation frame.
     * <p>
     * This feature isn't supported by this implementation as the frame in which the time-stamped PVCoordinates are
     * defined is set in the constructor and this frame is used to propagate the time-stamped PVCoordinates.
     * </p>
     * 
     * @param frameIn
     *        the frame to use
     * @throws UnsupportedOperationException
     *         always thrown by this implementation
     */
    @Override
    public void setOrbitFrame(final Frame frameIn) {
        throw new UnsupportedOperationException("The frame can only be set in the constructor.");
    }
}
