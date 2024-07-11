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
 * HISTORY
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import fr.cnes.sirius.patrius.events.CodedEventsLogger;
import fr.cnes.sirius.patrius.events.CodingEventDetector;
import fr.cnes.sirius.patrius.events.GenericCodingEventDetector;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Detector for extrema of the switching value of {@link #underlyingDetector}.
 * <p>
 * The extrema detector switching function simply returns the difference of the switching function values of underlying
 * detector assessed at {date+{@link #halfComputationStep} and {date-{@link #halfComputationStep} .
 * </p>
 * <p>
 * The default implementation behavior is to
 * {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#CONTINUE continue} propagation at min detection
 * and to {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#CONTINUE continue} propagation at max
 * detection. This can be changed by using provided constructors.
 * </p>
 * 
 * @param <D> an event detector
 *
 * @author Alice Latourte
 *
 * @since 4.9
 *
 */
public class ExtremaGenericDetector<D extends EventDetector> extends AbstractDetector {

    /**
     * Default maximum checking interval in seconds
     */
    public static final double DEFAULT_MAXCHECK = 120;

    /**
     * Default half computation step.
     */
    public static final double DEFAULT_HALF_COMPUTATION_STEP = 5E-4;

    /**
     * Serializable UID
     */
    private static final long serialVersionUID = 5819396315727972356L;

    /**
     * Underlying detector
     */
    private final D underlyingDetector;

    /**
     * Half computation step in seconds.
     */
    private final double halfComputationStep;

    /**
     * Builds an extrema direction occultation detector.
     * <p>
     * This constructor takes default values for half computation step ({@link #DEFAULT_HALF_COMPUTATION_STEP}
     * ), maximal checking interval ({@link #DEFAULT_MAXCHECK}) and convergence threshold ({@link #DEFAULT_THRESHOLD}).
     * </p>
     * <p>
     * The default implementation behavior is to
     * {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#CONTINUE continue} propagation at min
     * detection and to {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#CONTINUE continue}
     * propagation at max detection.
     * </p>
     * <p>
     * The default implementation behavior is to keep the detector at min detection and to keep the detector at max
     * detection.
     * </p>
     *
     * @param underlyingDetector
     *        zero crossing detector whose extrema are looked for
     * @param extremumType
     *        extremum type ({@link ExtremumType#MIN MIN}, {@link ExtremumType#MAX MAX} or {@link ExtremumType#MIN_MAX
     *        MIN_MAX})
     */
    public ExtremaGenericDetector(final D underlyingDetector, final ExtremumType extremumType) {

        this(underlyingDetector, extremumType, DEFAULT_HALF_COMPUTATION_STEP);
    }

    /**
     * Builds an extrema direction occultation detector.
     * <p>
     * This constructor takes default values for maximal checking interval ({@link #DEFAULT_MAXCHECK}) and convergence
     * threshold ({@link #DEFAULT_THRESHOLD}).
     * </p>
     * <p>
     * The default implementation behavior is to
     * {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#CONTINUE continue} propagation at min
     * detection and to {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#CONTINUE continue}
     * propagation at max detection.
     * </p>
     * <p>
     * The default implementation behavior is to keep the detector at min detection and to keep the detector at max
     * detection.
     * </p>
     *
     * @param underlyingDetector
     *        zero crossing detector whose extrema are looked for
     * @param extremumType
     *        extremum type ({@link ExtremumType#MIN MIN}, {@link ExtremumType#MAX MAX} or {@link ExtremumType#MIN_MAX
     *        MIN_MAX})
     * @param halfComputationStep
     *        current extrema detector g function is computed by difference of g function values of underlyingDetector
     *        between
     *        {date-halfComputationStep} and {date+halfComputationStep}
     */
    public ExtremaGenericDetector(final D underlyingDetector, final ExtremumType extremumType,
                                       final double halfComputationStep) {

        this(underlyingDetector, extremumType, halfComputationStep, DEFAULT_MAXCHECK);
    }

    /**
     * Builds an extrema events detector.
     * <p>
     * This constructor takes default value for convergence threshold ({@link #DEFAULT_THRESHOLD}).
     * </p>
     * <p>
     * The default implementation behavior is to
     * {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#CONTINUE continue} propagation at min
     * detection and to {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#CONTINUE continue}
     * propagation at max detection.
     * </p>
     * <p>
     * The default implementation behavior is to keep the detector at min detection and to keep the detector at max
     * detection.
     * </p>
     *
     * @param underlyingDetector
     *        zero crossing detector whose extrema are looked for
     * @param extremumType
     *        extremum type ({@link ExtremumType#MIN MIN}, {@link ExtremumType#MAX MAX} or {@link ExtremumType#MIN_MAX
     *        MIN_MAX})
     * @param halfComputationStep
     *        current extrema detector g function is computed by difference of g function values of underlyingDetector
     *        between
     *        {date-halfComputationStep} and {date+halfComputationStep}
     * @param maxCheck
     *        default maximum checking interval in seconds (see {@link AbstractDetector})
     */
    public ExtremaGenericDetector(final D underlyingDetector, final ExtremumType extremumType,
                                       final double halfComputationStep, final double maxCheck) {

        this(underlyingDetector, extremumType, halfComputationStep, maxCheck, DEFAULT_THRESHOLD);
    }

    /**
     * Builds an extrema direction occultation detector.
     * <p>
     * The default implementation behavior is to
     * {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#CONTINUE continue} propagation at min
     * detection and to {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#CONTINUE continue}
     * propagation at max detection.
     * </p>
     * <p>
     * The default implementation behavior is to keep the detector at min detection and to keep the detector at max
     * detection.
     * </p>
     *
     * @param underlyingDetector
     *        zero crossing detector whose extrema are looked for
     * @param extremumType
     *        extremum type ({@link ExtremumType#MIN MIN}, {@link ExtremumType#MAX MAX} or {@link ExtremumType#MIN_MAX
     *        MIN_MAX})
     * @param halfComputationStep
     *        current extrema detector g function is computed by difference of g function values of underlyingDetector
     *        between
     *        {date-halfComputationStep} and {date+halfComputationStep}
     * @param maxCheck
     *        default maximum checking interval in seconds (see {@link AbstractDetector})
     * @param threshold
     *        default convergence threshold (see {@link AbstractDetector})
     */
    public ExtremaGenericDetector(final D underlyingDetector, final ExtremumType extremumType,
                                       final double halfComputationStep, final double maxCheck,
                                       final double threshold) {

        this(underlyingDetector, extremumType, halfComputationStep, maxCheck, threshold, Action.CONTINUE,
                Action.CONTINUE);
    }

    /**
     * Builds an extrema events detector.
     * <p>
     * The default implementation behavior is to keep the detector at min detection and to keep the detector at max
     * detection.
     * </p>
     *
     * @param underlyingDetector
     *        zero crossing detector whose extrema are looked for
     * @param extremumType
     *        extremum type ({@link ExtremumType#MIN MIN}, {@link ExtremumType#MAX MAX} or {@link ExtremumType#MIN_MAX
     *        MIN_MAX})
     * @param halfComputationStep
     *        current extrema detector g function is computed by difference of g function values of underlyingDetector
     *        between
     *        {date-halfComputationStep} and {date+halfComputationStep}
     * @param maxCheck
     *        default maximum checking interval in seconds (see {@link AbstractDetector})
     * @param threshold
     *        default convergence threshold (see {@link AbstractDetector})
     * @param actionMin
     *        action performed at masking
     * @param actionMax
     *        action performed when masking ends
     */
    public ExtremaGenericDetector(final D underlyingDetector, final ExtremumType extremumType,
                                       final double halfComputationStep, final double maxCheck,
                                       final double threshold, final Action actionMin, final Action actionMax) {

        this(underlyingDetector, extremumType, halfComputationStep, maxCheck, threshold, actionMin, actionMax,
                false, false);
    }

    /**
     * Builds an extrema events detector.
     *
     * @param underlyingDetector
     *        zero crossing detector whose extrema are looked for
     * @param extremumType
     *        extremum type ({@link ExtremumType#MIN MIN}, {@link ExtremumType#MAX MAX} or {@link ExtremumType#MIN_MAX
     *        MIN_MAX})
     * @param halfComputationStep
     *        current extrema detector g function is computed by difference of g function values of underlyingDetector
     *        between
     *        {date-halfComputationStep} and {date+halfComputationStep}
     * @param maxCheck
     *        default maximum checking interval in seconds (see {@link AbstractDetector})
     * @param threshold
     *        default convergence threshold (see {@link AbstractDetector})
     * @param actionMin
     *        action performed at masking
     * @param actionMax
     *        action performed when masking ends
     * @param removeAtMin
     *        true if detector shall be removed at masking
     * @param removeAtMax
     *        true if detector shall be removed when masking ends
     */
    public ExtremaGenericDetector(final D underlyingDetector, final ExtremumType extremumType,
                                       final double halfComputationStep, final double maxCheck,
                                       final double threshold, final Action actionMin, final Action actionMax,
                                       final boolean removeAtMin, final boolean removeAtMax) {

        super(extremumType.getSlopeSelection(), maxCheck, threshold, actionMin, actionMax, removeAtMin, removeAtMax);

        // underlying detector
        this.underlyingDetector = underlyingDetector;

        // half computation step
        this.halfComputationStep = halfComputationStep;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
        throws PatriusException {
        final Action result;
        if (this.getSlopeSelection() == 0) {
            result = this.getActionAtEntry();
            // remove (or not) detector
            this.shouldBeRemovedFlag = this.isRemoveAtEntry();
        } else if (this.getSlopeSelection() == 1) {
            result = this.getActionAtExit();
            // remove (or not) detector
            this.shouldBeRemovedFlag = this.isRemoveAtExit();
        } else {
            if (forward ^ !increasing) {
                // minimum case
                result = this.getActionAtEntry();
                // remove (or not) detector
                this.shouldBeRemovedFlag = this.isRemoveAtEntry();
            } else {
                // maximum case
                result = this.getActionAtExit();
                // remove (or not) detector
                this.shouldBeRemovedFlag = this.isRemoveAtExit();
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExtremaGenericDetector<D> copy() {

        return new ExtremaGenericDetector<>(this.underlyingDetector, ExtremumType.find(super.getSlopeSelection()),
            this.halfComputationStep, super.getMaxCheckInterval(), super.getThreshold(), this.getActionAtEntry(),
            this.getActionAtExit(), this.isRemoveAtEntry(), this.isRemoveAtExit());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public double g(final SpacecraftState s) throws PatriusException {
        return (this.underlyingDetector.g(s.shiftedBy(this.halfComputationStep)) - this.underlyingDetector.g(s
            .shiftedBy(-this.halfComputationStep))) / (2 * this.halfComputationStep);

    }

    /**
     * Log extrema events into the entered events logger.<br>
     * <br>
     * <b>Warning</b>: The propagation is actually performed on an enlarged time interval with respect to the entered
     * one, in order to
     * detect all the extrema, included these shortly after interval beginning or shortly before interval end:
     * <ul>
     * <li>The propagation starts -2*maxCheck prior to the interval beginning date</li>
     * <li>The propagation stops 2*maxCheck after the interval end date</li>
     * </ul>
     * Therefore, there may be some events detected out of input time interval.<br>
     * <br>
     *
     * @param eventsLogger
     *        input events logger in which the events must be recorded
     * @param satProp
     *        spacecraft orbit propagator
     * @param interval
     *        time interval on which the events are computed
     *
     * @return the spacecraft state after propagation
     * @throws PatriusException
     *         if spacecraft state cannot be propagated
     */
    public SpacecraftState logExtremaEventsOverTimeInterval(final CodedEventsLogger eventsLogger,
                                                            final Propagator satProp,
                                                            final AbsoluteDateInterval interval)
        throws PatriusException {

        // encapsulate this detector to be able to detect coded events
        final boolean notUsed = false;
        final CodingEventDetector extremaCoder = new GenericCodingEventDetector(this, "MIN", "MAX", notUsed,
            "EXTREMUM");

        // let's the logger monitor the visi detector
        final EventDetector monitoredDetector = eventsLogger.monitorDetector(extremaCoder);

        // add the detector to satProp
        satProp.addEventDetector(monitoredDetector);

        // detector max check value
        final double maxCheck = getMaxCheckInterval();

        // propagation
        final SpacecraftState spacecraftState = satProp.propagate(interval.getLowerData().shiftedBy(-2 * maxCheck),
            interval.getUpperData().shiftedBy(2 * maxCheck));

        // clear events detectors
        satProp.clearEventsDetectors();

        return spacecraftState;

    }

    /**
     * Enumerate defining the type of extrema looked for.
     *
     * @author Alice Latourte
     *
     */
    public enum ExtremumType {

        /**
         * Detection of switching function minima
         */
        MIN(0),

        /**
         * Detection of switching function maxima
         */
        MAX(1),

        /**
         * Detection of both switching function minima and maxima
         */
        MIN_MAX(2);

        /**
         * Enumerate value
         */
        private final int value;

        /**
         * Constructor
         *
         * @param type
         *        integer value
         */
        private ExtremumType(final int type) {

            this.value = type;
        }

        /**
         * @return the slope selection
         */
        public int getSlopeSelection() {

            return this.value;
        }

        /**
         * Return the extremum type corresponding to the entered slope selection
         *
         * @param slopeSelection
         *        slope selection
         * @return the corresponding extremum type
         */
        public static ExtremumType find(final int slopeSelection) {

            // initiate output
            final ExtremumType extremumType;

            switch (slopeSelection) {
                case 0:
                    extremumType = ExtremumType.MIN;
                    break;
                case 1:
                    extremumType = ExtremumType.MAX;
                    break;
                default:
                    // case 2
                    extremumType = ExtremumType.MIN_MAX;
                    break;
            }

            return extremumType;
        }
    }

}
