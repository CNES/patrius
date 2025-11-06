/**
 * HISTORY
 * VERSION:4.15:OPENFD-385:21/11/2024:Execution en parallele des tests concernant EclipticJ2000Provider
 * VERSION:4.14.1:OPENFD-292:10/09/2024:Implémentation de multi-propagateurs mixtes
 * VERSION:4.14:OPENFD-292:22/08/2024: Implementation de multi-propagateurs mixtes
 * END-HISTORY
 */
/*
 */
/*
 */
/*
 */
package fr.cnes.sirius.patrius.propagation.analytical.multi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;

import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.multi.MultiAttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.multi.MultiAttitudeProviderWrapper;
import fr.cnes.sirius.patrius.events.EventDetector;
import fr.cnes.sirius.patrius.events.MultiEventDetector;
import fr.cnes.sirius.patrius.events.utils.EventState;
import fr.cnes.sirius.patrius.events.utils.OneSatEventDetectorWrapper;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.propagation.AbstractPropagator;
import fr.cnes.sirius.patrius.propagation.BoundedPropagator;
import fr.cnes.sirius.patrius.propagation.MultiPropagator;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.SpacecraftStateProvider;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.propagation.precomputed.Ephemeris;
import fr.cnes.sirius.patrius.propagation.sampling.multi.MultiPatriusFixedStepHandler;
import fr.cnes.sirius.patrius.propagation.sampling.multi.MultiPatriusStepHandler;
import fr.cnes.sirius.patrius.propagation.sampling.multi.MultiPatriusStepInterpolator;
import fr.cnes.sirius.patrius.propagation.sampling.multi.MultiPatriusStepNormalizer;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * <p>
 * This class is inspired from {@link AbstractPropagator} and adapted to multi propagation.
 * </p>
 * <p>
 * This class propagates N {@link SpacecraftState} using analytical propagators (any non-numerical propagator is
 * accepted). Each state is identified with an ID of type String.
 * </p>
 * <p>
 * Multi spacecraft analytical propagation requires at least one satellite to be added to the propagator using
 * {@link #addPropagator(Propagator, String)}.
 * </p>
 * <p>
 * The following general parameters can also be set :
 * <ul>
 * <li>the discrete events that should be triggered during propagation
 * ({@link #addEventDetector(EventDetector, String)}, {@link #clearEventsDetectors()})</li>
 * <li>the binding logic with the rest of the application ({@link #setSlaveMode()},
 * {@link #setMasterMode(double, MultiPatriusFixedStepHandler)}, {@link #setMasterMode(MultiPatriusStepHandler)})</li>
 * </ul>
 * </p>
 * <p>
 * <b>Important notes</b>:
 * <ul>
 * <li>trying to add an instance of {@link NumericalPropagator} is forbidden but any other class based on analytical
 * (e.g. {@link Ephemeris}) or semi-analytical (e.g. STELA) propagation is accepted, that is to say all implementations
 * of {@link Propagator} except {@link NumericalPropagator}</li>
 * <li>the ephemeris mode is not implemented in this propagator (not useful as of 4.14)</li>
 * <li>the use of a {@link MultiEventDetector} is <em>not</em> possible through
 * {@link #addEventDetector(MultiEventDetector)}, but can be used with {@link #addEventDetector(EventDetector, String)}
 * under the condition that it also implements {@link EventDetector}</li>
 * <li>methods linked to these points throw a {@link PatriusException} with an {@link PatriusMessages#ILLEGAL_STATE}
 * message</li>
 * </ul>
 * </p>
 * <p>
 * The same instance cannot be used simultaneously by different threads, the class is <em>not</em> thread-safe.
 * </p>
 *
 * @concurrency not thread-safe
 * @concurrency.comment attributes are mutable and related to propagation.
 *
 * @see SpacecraftState
 * @see AbstractPropagator
 * @see MultiPatriusStepHandler
 * @see MultiPatriusFixedStepHandler
 *
 * @author Maxime Astruc
 *
 * @version 4.14
 *
 * @since 4.14
 *
 */
@SuppressWarnings({ "PMD.NullAssignment", "PMD.ConstructorCallsOverridableMethod" })
public class MultiAnalyticalPropagator implements MultiPropagator {

    /** One hundred. */
    private static final double ONE_HUNDRED = 100;

    /** Start date for propagation. */
    private AbsoluteDate startDate;

    /** Map of initial states to propagate. */
    private Map<String, SpacecraftState> initialStates;

    /** Indicator for last step. */
    private boolean isLastStep;

    /** Indicator for last detection of the current detector. */
    private boolean isLastDetection;

    /** Indicator for master or ephemeris modes. */
    private boolean isStepHandled;

    /** Current mode. */
    private int mode;

    /** Fixed step size. */
    private double fixedStepSize;

    /** Step handler in master mode. */
    private transient MultiPatriusStepHandler masterStepHandler;

    /** Reference date, for instance used to check consistency between propagators and compute initial states. */
    private final AbsoluteDate referenceDate;

    /** Internal steps interpolator. */
    private final MultiStepInterpolator interpolator;

    /** Event detectors not related to force models. */
    private final List<MultiEventDetector> multiDetectors;

    /** Global event states list (gathers all event states of all satellites). */
    private final List<EventState> globalEventStatesList;

    /** Initialization indicator of events states. */
    private boolean statesInitialized;

    /** Frames used for propagation. */
    private Map<String, Frame> propagationFrames;

    /** Map of propagators. */
    private final Map<String, Propagator> propagators;

    /** Attitude providers for forces computation defined for each spacecraft. */
    private final Map<String, AttitudeProvider> attitudeProvidersForces;

    /** Attitude providers for events computation defined for each spacecraft. */
    private final Map<String, AttitudeProvider> attitudeProvidersEvents;

    /** Attitude providers given by default for one attitude (defined for each spacecraft). */
    private final Map<String, AttitudeProvider> attitudeProvidersByDefault;

    /**
     * Default constructor with an empty map of propagators.
     * 
     * @param referenceDate
     *        date that is used as a reference to compute initial spacecraft states
     * 
     * @throws PatriusException
     *         if a state cannot be computed or if the propagator is numerical
     */
    public MultiAnalyticalPropagator(final AbsoluteDate referenceDate) throws PatriusException {
        this(new HashMap<>(), referenceDate);
    }

    /**
     * Constructor with an input map of propagators. An initial state is computed for each, at propagator's input
     * reference date.
     * 
     * @param analyticalPropagatorsMap
     *        map of propagators that shall all be analytical
     * @param referenceDate
     *        date that is used as a reference to compute initial spacecraft states
     * 
     * @throws PatriusException
     *         if a state cannot be computed or if the propagator is numerical
     */
    public MultiAnalyticalPropagator(final Map<String, Propagator> analyticalPropagatorsMap,
                                     final AbsoluteDate referenceDate)
        throws PatriusException {

        // Store reference date
        this.referenceDate = referenceDate;

        // Map of propagation frames
        this.propagationFrames = new HashMap<>();

        // Attitude providers fields
        this.attitudeProvidersByDefault = new HashMap<>();
        this.attitudeProvidersForces = new HashMap<>();
        this.attitudeProvidersEvents = new HashMap<>();

        // Create map for propagators and initial states
        this.propagators = new HashMap<>(analyticalPropagatorsMap.size());
        this.initialStates = new HashMap<>(analyticalPropagatorsMap.size());

        // Add propagator entries to the map of the multi propagator, and fill the initial states
        // Some checks are performed on IDs sanity
        for (final Entry<String, Propagator> entry : analyticalPropagatorsMap.entrySet()) {
            addPropagator(entry.getValue(), entry.getKey());
        }

        // Interpolator
        this.interpolator = new MultiStepInterpolator();

        // Event detectors fields
        this.multiDetectors = new ArrayList<>();
        this.globalEventStatesList = new ArrayList<>();
        this.statesInitialized = false;
    }

    /**
     * Check if input propagator is a numerical propagator. An exception is thrown in that case.
     * 
     * @param propagator
     *        propagator to assess
     * 
     * @throws PatriusException
     *         if the propagator is numerical
     */
    private static void checkPropagatorNature(final Propagator propagator) throws PatriusException {
        /*
         * Numerical propagators are not authorized in this class as the multi-propagation of this class is tailored to
         * handle analytical propagators. Numerical propagators would therefore be propagated like analytical ones,
         * which does not make much sense.
         */
        if (propagator instanceof NumericalPropagator) {
            throw new PatriusException(PatriusMessages.ILLEGAL_STATE);
        }
    }

    /**
     * Check if the input spacecraft ID is valid.
     *
     * @param satId
     *        the spacecraft ID
     */
    private static void checkSatId(final String satId) {

        // Check sat ID is not null or empty
        if (satId == null || satId.isEmpty()) {
            throw PatriusException.createIllegalStateException(PatriusMessages.PDB_NULL_STATE_ID);
        }

    }

    /**
     * Check if the input spacecraft ID and present (or not) in propagators map.
     *
     * @param satId
     *        the spacecraft ID
     * @param shallBePresent
     *        the ID shall be present in propagators map when this method is called
     */
    private void checkSatId(final String satId, final boolean shallBePresent) {

        // Check sat ID is not null or empty
        checkSatId(satId);

        final boolean isPresent = this.propagators.containsKey(satId);

        /*
         * Error is raised if :
         * - the ID is expected but not contained in the map
         * - the ID shall not be in the map but is contained anyway
         */
        final boolean notNominal = isPresent ^ shallBePresent;
        if (notNominal) {
            throw PatriusException.createIllegalStateException(PatriusMessages.PDB_SAT_ID_UNEXPECTED, satId);
        }

    }

    /**
     * Add an additional propagator to the map of propagators. At each propagation step, the additional states
     * will be retrieved and added to the map of states along with the propagated states. This allows to pass additional
     * states to the step handler and event detectors during propagation.
     * 
     * @param propagator
     *        additional propagator
     * @param satId
     *        spacecraft ID whose state is propagated
     * 
     * @throws PatriusException
     *         if an initial state is already defined with this ID or if the input ID is null
     */
    public final void addPropagator(final Propagator propagator, final String satId) throws PatriusException {

        // Check satellite ID
        checkSatId(satId, false);
        // Check propagator's nature
        checkPropagatorNature(propagator);

        /*
         * Clear propagator from potential detectors if it has any and ensure it is in slave mode.
         * This is needed to ensure basic propagation when calling method getSpacecraftState (propagation stops exactly
         * at the asked date and no handlers are used).
         */
        propagator.clearEventsDetectors();
        propagator.setSlaveMode();
        this.propagators.put(satId, propagator);

        // Add the initial state if start date is known, otherwise it will be added later
        this.initialStates.put(satId, propagator.getSpacecraftState(this.referenceDate));

        // Add attitude providers
        this.setAttitudeProvider(propagator.getAttitudeProvider(), satId);
        this.setAttitudeProviderForces(propagator.getAttitudeProviderForces(), satId);
        this.setAttitudeProviderEvents(propagator.getAttitudeProviderEvents(), satId);

    }

    /** {@inheritDoc} */
    @Override
    public void addInitialState(final SpacecraftState initialState, final String satId) throws PatriusException {

        // Check date correspondence
        if (!this.referenceDate.equals(initialState.getDate())) {
            throw new PatriusException(PatriusMessages.PDB_MULTI_SAT_DATE_MISMATCH, initialState.getDate(),
                this.referenceDate);
        }

        // Check if ID is valid
        // Propagators map shall have an entry with provided ID so that its state is reseted
        this.checkSatId(satId, true);

        this.propagators.get(satId).resetInitialState(initialState);

    }

    /** {@inheritDoc} */
    @Override
    public Map<String, SpacecraftState> getInitialStates() throws PatriusException {
        return new HashMap<>(this.initialStates);
    }

    /**
     * Get the propagator initial state of a single satellite.
     * 
     * @param satId
     *        ID of the satellite
     * 
     * @return the initial state of the satellite whose ID is provided
     * 
     * @throws PatriusException
     *         if state cannot be retrieved
     */
    public SpacecraftState getInitialState(final String satId) throws PatriusException {
        return this.initialStates.get(satId);
    }

    /**
     * Get spacecraft state providers map.
     * 
     * @return a copy of the spacecraft state providers map
     */
    public Map<String, SpacecraftStateProvider> getPropagators() {
        return new HashMap<>(this.propagators);
    }

    /*
     * MODES PART
     */

    /** {@inheritDoc} */
    @Override
    public int getMode() {
        return this.mode;
    }

    /** {@inheritDoc} */
    @Override
    public void setSlaveMode() {
        this.masterStepHandler = null;
        this.mode = SLAVE_MODE;
        this.isStepHandled = false;
        this.fixedStepSize = Double.NaN;
    }

    /** {@inheritDoc} */
    @Override
    public void setMasterMode(final double h, final MultiPatriusFixedStepHandler handler) {
        this.setMasterMode(new MultiPatriusStepNormalizer(h, handler));
        this.fixedStepSize = h;
    }

    /** {@inheritDoc} */
    @Override
    public void setMasterMode(final MultiPatriusStepHandler handler) {
        this.masterStepHandler = handler;
        this.mode = MASTER_MODE;
        this.isStepHandled = true;
        this.fixedStepSize = Double.NaN;
    }

    /** Not authorized method: throws an unchecked exception when called. */
    @Override
    public void setEphemerisMode() {
        // Not authorized
        throw new PatriusExceptionWrapper(new PatriusException(PatriusMessages.ILLEGAL_STATE));
    }

    /** Not authorized method: throws an unchecked exception when called. */
    @Override
    public BoundedPropagator getGeneratedEphemeris(final String satId) {
        // Not authorized
        throw new PatriusExceptionWrapper(new PatriusException(PatriusMessages.ILLEGAL_STATE));
    }

    /*
     * END OF MODES PART
     * BEGIN DETECTORS PART
     */

    /** Not authorized method: throws an unchecked exception when called. */
    @Override
    public void addEventDetector(final MultiEventDetector detector) {
        // Handled by the other method
        throw new PatriusExceptionWrapper(new PatriusException(PatriusMessages.ILLEGAL_STATE));
    }

    /** {@inheritDoc} */
    @Override
    public void addEventDetector(final EventDetector detector, final String satId) {

        // Check if ID is known
        this.checkSatId(satId, true);

        final EventState eventState = new EventState(detector, satId);
        try {
            final SpacecraftState initialState = this.initialStates.get(satId);
            detector.init(initialState, this.referenceDate);
            eventState.reinitializeBegin(initialState);
        } catch (PatriusException underlyingException) {
            throw new PatriusExceptionWrapper(underlyingException);
        }

        // Multi part
        final OneSatEventDetectorWrapper wrapper = new OneSatEventDetectorWrapper(detector, satId);
        this.multiDetectors.add(wrapper);

        this.globalEventStatesList.add(eventState);
    }

    /** {@inheritDoc} */
    @Override
    public Collection<MultiEventDetector> getEventsDetectors() {
        return Collections.unmodifiableCollection(this.multiDetectors);
    }

    /** {@inheritDoc} */
    @Override
    public void clearEventsDetectors() {
        this.multiDetectors.clear();
        this.globalEventStatesList.clear();
    }

    /*
     * END OF DETECTORS PART
     * BEGIN ATTITUDE PROVIDERS PART
     */

    /** {@inheritDoc} */
    @Override
    public MultiAttitudeProvider getAttitudeProvider(final String satId) {

        // Check if ID is known
        this.checkSatId(satId, true);

        AttitudeProvider attProv = null;
        MultiAttitudeProvider providerOut = null;
        // If two attitude providers were provided
        // Maps necessarily contain an entry with satID, however the value may be null
        if (this.attitudeProvidersByDefault.get(satId) == null) {
            if (this.attitudeProvidersForces.get(satId) != null) {
                attProv = this.attitudeProvidersForces.get(satId);
            } else if (this.attitudeProvidersEvents.get(satId) != null) {
                attProv = this.attitudeProvidersEvents.get(satId);
            }
        } else {
            // If single attitude provider
            attProv = this.attitudeProvidersByDefault.get(satId);
        }

        // If no attitude provider is known then the variable is null still
        if (attProv != null) {
            providerOut = new MultiAttitudeProviderWrapper(attProv, satId);
        }

        return providerOut;
    }

    /** {@inheritDoc} */
    @Override
    public MultiAttitudeProvider getAttitudeProviderForces(final String satId) {
        // Check if ID is known
        this.checkSatId(satId, true);

        MultiAttitudeProvider providerOut = null;
        if (this.attitudeProvidersForces.get(satId) != null) {
            providerOut = new MultiAttitudeProviderWrapper(this.attitudeProvidersForces.get(satId), satId);
        }

        return providerOut;
    }

    /** {@inheritDoc} */
    @Override
    public MultiAttitudeProvider getAttitudeProviderEvents(final String satId) {
        // Check if ID is known
        this.checkSatId(satId, true);

        MultiAttitudeProvider providerOut = null;
        if (this.attitudeProvidersEvents.get(satId) != null) {
            providerOut = new MultiAttitudeProviderWrapper(this.attitudeProvidersEvents.get(satId), satId);
        }

        return providerOut;
    }

    /** {@inheritDoc} */
    @Override
    public void setAttitudeProvider(final AttitudeProvider attitudeProvider, final String satId) {
        this.attitudeProvidersByDefault.put(satId, attitudeProvider);
    }

    /** {@inheritDoc} */
    @Override
    public void setAttitudeProviderForces(final AttitudeProvider attitudeProviderForces, final String satId) {
        this.attitudeProvidersForces.put(satId, attitudeProviderForces);
    }

    /** {@inheritDoc} */
    @Override
    public void setAttitudeProviderEvents(final AttitudeProvider attitudeProviderEvents, final String satId) {
        this.attitudeProvidersEvents.put(satId, attitudeProviderEvents);
    }

    // No methods with MultiAttitudeProvider

    /*
     * END OF ATTITUDE PROVIDERS PART
     */

    /** {@inheritDoc} */
    @Override
    public Frame getFrame(String satId) {

        // Check that the satellite ID exists in the map
        checkSatId(satId, true);

        return this.propagators.get(satId).getFrame();
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, SpacecraftState> propagate(final AbsoluteDate target) throws PropagationException {

        try {
            if (this.startDate == null) {
                final Iterator<String> idsIterator = this.getInitialStates().keySet().iterator();
                if (idsIterator.hasNext()) {
                    final String satId = idsIterator.next();
                    this.startDate = this.getInitialState(satId).getDate();
                }
                // Else case is handled afterwards: an exception will be thrown because the map is empty
            }

            return this.propagate(this.startDate, target);
        } catch (final PatriusException oe) {

            // Recover a possible embedded PropagationException
            for (Throwable t = oe; t != null; t = t.getCause()) {
                if (t instanceof PropagationException) {
                    // Throw the caught propagation exception
                    throw (PropagationException) t;
                }
            }
            // Propagation exception created from Patrius exception
            throw new PropagationException(oe);

        }

    }

    /** {@inheritDoc} */
    @Override
    public Map<String, SpacecraftState> propagate(final AbsoluteDate start, AbsoluteDate target)
        throws PropagationException {
        // CHECKSTYLE: resume CyclomaticComplexity check
        try {
            this.manageStateFrame();
            Map<String, SpacecraftState> scStates = null;
            this.interpolator.storeForwardFlag(start, target);

            // Store an existing ID belonging to the propagation map
            final Iterator<SpacecraftState> iterator = this.initialStates.values().iterator();
            final SpacecraftState exampleState = iterator.next();

            if (!this.globalEventStatesList.isEmpty() || this.isStepHandled) {

                /*
                 * Clean propagators map: they cannot be in a mode different from slave mode, as their handler would
                 * interfere with multi propagator's step handler. This can lead to infinite loop.
                 * As a result, underlying propagators are set to slave mode, which sets their step handler to null.
                 */
                if (this.isStepHandled) {
                    for (final Propagator propagator : this.propagators.values()) {
                        propagator.setSlaveMode();
                    }
                }

                /*
                 * The start date is stored, initializing global and soft current dates and computing with basic
                 * propagation the spacecraft state of each object.
                 */
                this.interpolator.storeDate(start);
                final double dt = target.durationFrom(start);
                final double epsilon = MathLib.ulp(dt);

                // States that were basic-propagated (trajectory only) are retrieved
                scStates = this.interpolator.getInterpolatedStates();

                // Evaluate step size
                final double stepSize;

                if (this.mode == MASTER_MODE) {
                    if (Double.isNaN(this.fixedStepSize)) {
                        // Step size arbitrarily at 1/100th of an orbit
                        // Use mean motion instead of keplerian period for robustness to hyperbolic orbits
                        final double meanMotion = scStates.values().iterator().next().getKeplerianMeanMotion();
                        stepSize = MathLib.copySign(2.0 * MathLib.PI / ONE_HUNDRED / meanMotion, dt);
                    } else {
                        stepSize = MathLib.copySign(this.fixedStepSize, dt);
                    }
                } else {
                    stepSize = dt;
                }

                /*
                 * Initialize event detector linked to each event state.
                 * Note that AbstractDetector's init method does nothing by default.
                 */
                for (final EventState eventState : this.globalEventStatesList) {
                    eventState.getEventDetector().init(scStates.get(eventState.getSpacecraftId()), target);
                }

                // Initialize step handler (master or ephemeris modes)
                if (this.masterStepHandler != null) {
                    this.masterStepHandler.init(scStates, target);
                }

                // Iterate over the propagation range
                this.statesInitialized = false;
                this.isLastStep = false;
                this.isLastDetection = false;
                do {

                    /*
                     * Go ahead one step size.
                     * The method shift stores the current dates (global and soft) values in the previous dates (global
                     * and soft). The soft current date takes the value of the global current (= previous) value.
                     * Once shifted, new call to storeDate can be performed (sets current dates).
                     */
                    this.interpolator.shift();

                    final AbsoluteDate t = this.interpolator.getCurrentDate().shiftedBy(stepSize);
                    if ((MathLib.abs(dt) < Precision.DOUBLE_COMPARISON_EPSILON)
                            || ((dt > 0) ^ (t.compareTo(target) <= 0))) {
                        // Current step exceeds target
                        this.interpolator.storeDate(target);
                    } else {
                        // Current step is within range
                        this.interpolator.storeDate(t);
                    }

                    /*
                     * Accept the step, trigger events and step handlers.
                     * Previous and current dates are set at this point.
                     * Global dates set up limit dates of current step (fixed). In slave mode for instance these dates
                     * are simply equal to the initial and target dates.
                     * Soft dates are dynamic dates used for events management.
                     */
                    scStates = this.acceptStep(target, epsilon);

                } while (!this.isLastStep);
            } else {
                this.interpolator.storeDate(target);
                scStates = this.interpolator.getInterpolatedStates();
            }

            // Return the last computed state
            this.startDate = exampleState.getDate();
            return scStates;

        } catch (final PropagationException pe) {
            // In that specific case we want to keep the exception
            throw pe;
        } catch (final PatriusException oe) {
            throw PropagationException.unwrap(oe);
        }
    }

    /**
     * Propagate an orbit without any fancy features.
     * <p>
     * This method is similar in spirit to the {@link #propagate} method, except that it does <strong>not</strong> call
     * any handler during propagation, nor any discrete events. It always stops exactly at the specified date.
     * </p>
     *
     * @param date
     *        target date for propagation
     *        
     * @return state at specified date
     * 
     * @throws PropagationException
     *         if propagation cannot reach specified date
     */
    protected Map<String, SpacecraftState> basicPropagate(final AbsoluteDate date) throws PropagationException {

        final Map<String, SpacecraftState> basicPropagationMap = new HashMap<>();

        try {
            final Iterator<String> idsIterator = this.getInitialStates().keySet().iterator();

            while (idsIterator.hasNext()) {
                final String satId = idsIterator.next();
                final Propagator propagator = this.propagators.get(satId);

                SpacecraftState spacecraftState = propagator.propagate(date);
                do {
                    /*
                     * Use of a loop here to compensate for very small date shifts error that may occur with long
                     * propagation durations. A simple call to propagator.getSpacecraftState(date) ends up with such
                     * lacks of precision sometimes.
                     */
                    spacecraftState = spacecraftState.shiftedBy(date.durationFrom(spacecraftState.getDate()));
                } while (MathLib.abs(date.durationFrom(spacecraftState.getDate())) > Precision.EPSILON);

                basicPropagationMap.put(satId, spacecraftState);
            }

        } catch (final PatriusException exception) {
            throw new PropagationException(exception);
        }

        return basicPropagationMap;

    }

    /**
     * Accept a step, triggering events and step handlers.
     *
     * @param target
     *        final propagation time
     * @param epsilon
     *        threshold for end date detection
     *        
     * @return state at the end of the step
     * 
     * @exception PatriusException
     *            if the switching function cannot be evaluated
     */
    // CHECKSTYLE: stop MethodLength check
    // CHECKSTYLE: stop CyclomaticComplexity check
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Orekit code adapted to multi propagation
    protected Map<String, SpacecraftState> acceptStep(final AbsoluteDate target, final double epsilon)
        throws PatriusException {
        // CHECKSTYLE: resume MethodLength check
        // CHECKSTYLE: resume CyclomaticComplexity check
        // CHECKSTYLE: resume ReturnCount check

        // Initialize the events states if needed
        if (!this.statesInitialized) {

            // Initialize the events states
            final AbsoluteDate t0 = this.interpolator.getPreviousDate();
            this.interpolator.setInterpolatedDate(t0);
            final Map<String, SpacecraftState> interpolatedScStates = this.interpolator.getInterpolatedStates();
            for (final EventState eventState : this.globalEventStatesList) {
                eventState.reinitializeBegin(interpolatedScStates.get(eventState.getSpacecraftId()));
            }

            this.statesInitialized = true;

        }

        /*
         * Create a sorted set that will contain and automatically order all the event states for which an event is
         * detected during current step.
         */
        final int orderingSign;
        if (this.interpolator.isForward()) {
            orderingSign = +1;
        } else {
            orderingSign = -1;
        }
        final SortedSet<EventState> globalOccuringEventsSet = new TreeSet<>(new Comparator<EventState>(){

            /** {@inheritDoc} */
            @Override
            public int compare(final EventState es0, final EventState es1) {
                int rez = orderingSign * es0.getEventTime().compareTo(es1.getEventTime());
                if (rez == 0) {
                    // First event is considered in case of identical date
                    rez = 1;
                }
                return rez;
            }

        });

        // Search for next events that may occur during the step
        for (final EventState eventState : this.globalEventStatesList) {
            /*
             * When evaluating a step, there are only calls to method setInterpolatedDate.
             * This method performs basic propagation at provided date. Soft dates are not modified.
             * Since spacecraft states of all satellites are actuated at each interpolation substep, this ensures
             * events detection is performed with accurate data (satellites position is up to date for instance).
             */
            if (eventState.evaluateStep(this.interpolator, eventState.getSpacecraftId())) {
                // The event occurs during the current step
                globalOccuringEventsSet.add(eventState);
            }
        }

        /*
         * The set contains all events of all satellites.
         * Events (and related satellite) are sorted chronologically thanks to the SortedSet. The date of this event is
         * used in the following computations.
         */
        // These dates are set: step start and end
        AbsoluteDate previousT = this.interpolator.getGlobalPreviousDate();
        AbsoluteDate currentT = this.interpolator.getGlobalCurrentDate();
        while (!globalOccuringEventsSet.isEmpty()) {

            // Handle the chronologically first event and retrieve related spacecraft ID
            final Iterator<EventState> iterator = globalOccuringEventsSet.iterator();
            final EventState currentEventState = iterator.next();
            final String satId = currentEventState.getSpacecraftId();
            iterator.remove();

            /*
             * Restrict the interpolator to the first part of the step, up to the event.
             * Soft dates are used to handle propagation intervals related to events (they delimit a portion of the
             * global propagation step).
             */
            final AbsoluteDate eventT = currentEventState.getEventTime();
            this.interpolator.setSoftPreviousDate(previousT);
            this.interpolator.setSoftCurrentDate(eventT);

            /*
             * Trigger the event.
             * The method setInterpolatedDate triggers basic propagation at provided date.
             */
            this.interpolator.setInterpolatedDate(eventT);
            final SpacecraftState eventScState = this.interpolator.getInterpolatedStates().get(satId);

            // Specific case: event not treated if end of propagation
            this.isLastStep =
                Precision.equals(target.durationFrom(this.interpolator.getInterpolatedDate()), 0, epsilon);
            if (!this.isLastStep) {
                // Method stepAccepted triggers detector's eventOccurred method
                currentEventState.stepAccepted(eventScState);
                this.isLastStep = currentEventState.stop(); // true if detector asks to stop (Action == STOP)
            }
            this.isLastDetection = currentEventState.removeDetector();

            if (this.masterStepHandler != null) {
                this.masterStepHandler.handleStep(this.interpolator, this.isLastStep);
            }

            if (this.isLastStep) {
                // The event asked to stop integration
                return this.interpolator.getInterpolatedStates();
            }

            /*
             * Enter block below if a detected event is on pending reset.
             * The method stepAccepted (in previous lines) retrieves the Action from the detector when an event occurs:
             * if the action is to RESET_STATE.
             * The code checks that, for a given event on pending reset, no event has been missed.
             */
            if (currentEventState.isPendingReset()) {
                /*
                 * If a reset is about to occur: check we didn't miss any event beforehand on interval [t0,
                 * t(reset_state)]. Event could have been be missed because of maxCheck threshold.
                 */
                boolean found = false;
                for (final EventState eventState : this.globalEventStatesList) {
                    if ((this.interpolator.isForward() && eventState.getT0().compareTo(eventT) > 0) ||
                            (!this.interpolator.isForward() && eventState.getT0().compareTo(eventT) < 0)) {
                        // Particular case of event with slope selection != 2
                        // In that case t0 > eventT means we skipped an event because of slope selection
                        // In that case going backward in time requires to go back to beginning of step
                        this.interpolator.setInterpolatedDate(previousT);
                        eventState.storeState(this.interpolator.getInterpolatedStates().get(satId), true);
                        this.interpolator.setInterpolatedDate(eventT);
                    }
                    // Event too close to be detected
                    final boolean closeEvent = eventState.getT0() != null
                            && MathLib.abs(eventState.getT0().durationFrom(eventT)) <= eventState.getEventDetector()
                                .getThreshold();
                    if (!eventState.equals(currentEventState) && !closeEvent
                            && eventState.evaluateStep(this.interpolator, satId)) {
                        // A missed event has been found during the reduced step
                        // If event occurs exactly at reset_state event time: it should not be considered since this
                        // event will be treated after reset state
                        if (MathLib.abs(eventState.getEventTime().durationFrom(eventT)) > eventState
                            .getEventDetector().getThreshold()) {
                            globalOccuringEventsSet.add(eventState);
                            found = true;
                        }
                    }
                }
                if (found) {
                    // If a missed event has been found, skip current reset state for now
                    // but add it in last to treat it once all new anterior events have been treated
                    // Warning: cancelled reset state will be detected twice (but reset performed once)
                    currentEventState.cancelStepAccepted();
                    globalOccuringEventsSet.add(currentEventState);
                    continue;
                }
            }

            SpacecraftState resetScState = currentEventState.reset(eventScState);
            if (resetScState != null) {
                // Full reset state (including attitude)
                resetScState = this.resetState(resetScState, satId);

                // Check events just after event date and update
                // We cannot use the interpolator which is no longer up-to-date
                for (int i = 0; i < this.globalEventStatesList.size(); i++) {
                    final EventState eventState = this.globalEventStatesList.get(i);
                    if ((!eventState.equals(currentEventState)) && (eventState.evaluateStep(resetScState))) {
                        // Pending event: treat it
                        eventState.stepAccepted(resetScState);

                        // Treat RESET_STATE
                        if (eventState.isPendingReset()) {
                            resetScState = eventState.reset(resetScState);
                            resetScState = this.resetState(resetScState, satId);
                        }

                        // Treat last detection
                        this.isLastDetection = eventState.removeDetector();
                        if (this.isLastDetection) {
                            this.globalEventStatesList.remove(eventState);
                        }

                        // Treat Action.STOP
                        this.isLastStep = eventState.stop();
                        if (this.isLastStep) {
                            resetSingleInitialState(resetScState, satId);
                            return this.initialStates;
                        }
                    }
                }

                // Store all event states
                for (final EventState state : this.globalEventStatesList) {
                    state.storeState(resetScState, false);
                }

                if (this.isLastDetection) {
                    // The current event is removed from the event list
                    this.globalEventStatesList.remove(currentEventState);
                }

                return this.initialStates;
            }

            // Prepare handling of the remaining part of the step
            previousT = eventT;
            this.interpolator.setSoftPreviousDate(eventT);
            this.interpolator.setSoftCurrentDate(currentT);

            if (this.isLastDetection) {
                // The current event is removed from the event list
                this.globalEventStatesList.remove(currentEventState);
            } else {
                // Check if the same event occurs again in the remaining part of the step
                if (!this.isLastDetection && currentEventState.evaluateStep(this.interpolator, satId)) {
                    // The event occurs during the current step
                    globalOccuringEventsSet.add(currentEventState);
                }
            }
        }

        final double remaining = target.durationFrom(currentT);
        this.isLastStep = MathLib.abs(remaining) < epsilon;
        if (this.isLastStep) {
            currentT = target;
        }

        this.interpolator.setInterpolatedDate(currentT);
        final Map<String, SpacecraftState> interpStates = this.interpolator.getInterpolatedStates();
        for (final EventState eventState : this.globalEventStatesList) {
            eventState.stepAccepted(interpStates.get(eventState.getSpacecraftId()));
            this.isLastStep = this.isLastStep || eventState.stop();
        }

        // Handle the remaining part of the step, after all events if any
        if (this.masterStepHandler != null) {
            this.masterStepHandler.handleStep(this.interpolator, this.isLastStep);
        }

        return interpStates;

    }

    /**
     * Get the reference date.
     * 
     * @return the reference date
     */
    public AbsoluteDate getReferenceDate() {
        return this.referenceDate;
    }

    /**
     * Reset state including attitude.
     *
     * @param resetState
     *        current reseted state
     * @param satId
     *        the spacecraft ID
     * 
     * @return reseted state including attitude
     */
    private SpacecraftState resetState(final SpacecraftState resetState, final String satId) {
        // Initialize attitude provider force and event
        AttitudeProvider attitudeProvForce = null;
        AttitudeProvider attitudeProvEvent = null;
        // If single attitude treatment
        if (this.attitudeProvidersByDefault.get(satId) == null) {
            // If two attitudes treatment
            attitudeProvForce = this.attitudeProvidersForces.get(satId);
            attitudeProvEvent = this.attitudeProvidersEvents.get(satId);
        } else {
            // single attitude treatment
            attitudeProvForce = this.attitudeProvidersByDefault.get(satId);
        }
        // Update attitude (orbit is unchanged since date is the same)
        final SpacecraftState res = new SpacecraftState(attitudeProvForce, attitudeProvEvent, resetState.getOrbit(),
            resetState.getAdditionalStates());

        this.resetSingleInitialState(res, satId);

        return res;
    }

    /**
     * Reset the initial state of a single satellite in the initial states map.
     * 
     * @param newSpacecraftState
     *        new spacecraft state that shall be set
     * @param satId
     *        satellite ID whose state shall be reseted
     */
    public void resetSingleInitialState(final SpacecraftState newSpacecraftState, final String satId) {
        this.initialStates.put(satId, newSpacecraftState);
    }

    /**
     * Manage the state frame: propagated orbits are projected in the propagation frame.
     *
     * @throws PatriusException
     *         if the frame of the initial state is not inertial or pseudo-inertial or if the propagator's map is empty
     */
    protected void manageStateFrame() throws PatriusException {

        final Iterator<String> idsIterator = this.getInitialStates().keySet().iterator();
        if (idsIterator.hasNext()) {

            do {

                final String satId = idsIterator.next();

                if (this.propagationFrames.get(satId) == null) {
                    // Propagation frame has not been provided: frame used is orbit frame is inertial or pseudo-inertial
                    final Frame frame = this.getInitialState(satId).getFrame();
                    if (frame.isPseudoInertial()) {
                        this.propagationFrames.put(satId, frame);
                    } else {
                        // Exception
                        throw new PatriusException(PatriusMessages.NOT_INERTIAL_FRAME);
                    }
                } else {
                    // Propagation frame has been provided: convert initial states in propagation frame
                    if (this.getInitialState(satId).getFrame() != this.propagationFrames.get(satId)) {
                        final Orbit initOrbit = this.getInitialState(satId).getOrbit();
                        final OrbitType type = initOrbit.getType();
                        final Orbit propagationOrbit = type.convertOrbit(initOrbit, this.propagationFrames.get(satId));
                        this.initialStates.put(satId, this.getInitialState(satId).updateOrbit(propagationOrbit));
                    }
                }

            } while (idsIterator.hasNext());

        } else {
            // Empty propagation map, exception set so as to avoid null pointer exception with start date (cannot be
            // initialized)
            throw new PatriusException(PatriusMessages.EMPTY_COLLECTION_NOT_ALLOWED, "propagators map");
        }

    }

    /** Internal class for local propagation. */
    private class MultiStepInterpolator implements MultiPatriusStepInterpolator {

        /** Global previous date. */
        private AbsoluteDate globalPreviousDate;

        /** Global current date. */
        private AbsoluteDate globalCurrentDate;

        /** Global forward propagation indicator. */
        private boolean globalForward;

        /** Soft previous date. */
        private AbsoluteDate softPreviousDate;

        /** Soft current date. */
        private AbsoluteDate softCurrentDate;

        /** Interpolated date. */
        private AbsoluteDate interpolatedDate;

        /** Interpolated states. */
        private Map<String, SpacecraftState> interpolatedStates;

        /** Forward propagation indicator. */
        private boolean forward;

        /**
         * Build a new instance from a basic propagator.
         */
        public MultiStepInterpolator() {
            this.globalPreviousDate = AbsoluteDate.PAST_INFINITY;
            this.globalCurrentDate = AbsoluteDate.PAST_INFINITY;
            this.softPreviousDate = AbsoluteDate.PAST_INFINITY;
            this.softCurrentDate = AbsoluteDate.PAST_INFINITY;
        }

        /**
         * Restrict step range to a limited part of the global step.
         * <p>
         * This method can be used to restrict a step and make it appear as if the original step was smaller. Calling
         * this method <em>only</em> changes the value returned by {@link #getPreviousDate()}, it does not change any
         * other property
         * </p>
         *
         * @param softPreviousDateIn
         *        start of the restricted step
         */
        public void setSoftPreviousDate(final AbsoluteDate softPreviousDateIn) {
            this.softPreviousDate = softPreviousDateIn;
        }

        /**
         * Restrict step range to a limited part of the global step.
         * <p>
         * This method can be used to restrict a step and make it appear as if the original step was smaller. Calling
         * this method <em>only</em> changes the value returned by {@link #getCurrentDate()}, it does not change any
         * other property
         * </p>
         *
         * @param softCurrentDateIn
         *        end of the restricted step
         */
        public void setSoftCurrentDate(final AbsoluteDate softCurrentDateIn) {
            this.softCurrentDate = softCurrentDateIn;
        }

        /**
         * Get the previous global grid point time.
         *
         * @return previous global grid point time
         */
        public AbsoluteDate getGlobalPreviousDate() {
            return this.globalPreviousDate;
        }

        /**
         * Get the current global grid point time.
         *
         * @return current global grid point time
         */
        public AbsoluteDate getGlobalCurrentDate() {
            return this.globalCurrentDate;
        }

        /** {@inheritDoc} */
        @Override
        public AbsoluteDate getCurrentDate() {
            return this.softCurrentDate;
        }

        /** {@inheritDoc} */
        @Override
        public AbsoluteDate getInterpolatedDate() {
            return this.interpolatedDate;
        }

        /** {@inheritDoc} */
        @Override
        public Map<String, SpacecraftState> getInterpolatedStates() throws PatriusException {
            return this.interpolatedStates;
        }

        /** {@inheritDoc} */
        @Override
        public AbsoluteDate getPreviousDate() {
            return this.softPreviousDate;
        }

        /** {@inheritDoc} */
        @Override
        public boolean isForward() {
            return this.forward;
        }

        /** {@inheritDoc} */
        @Override
        public void setInterpolatedDate(final AbsoluteDate date) throws PropagationException {
            // Compute raw spacecraft states
            this.interpolatedDate = date;
            this.interpolatedStates = basicPropagate(date);

        }

        /**
         * Shift one step forward.
         * Copy the current date into the previous date, hence preparing the interpolator for future calls to
         * {@link #storeDate storeDate}
         */
        public void shift() {
            this.globalPreviousDate = this.globalCurrentDate;
            this.softPreviousDate = this.globalPreviousDate;
            this.softCurrentDate = this.globalCurrentDate;
        }

        /**
         * Store the current step date and triggers basic propagation.
         *
         * @param date
         *        current date
         * 
         * @exception PropagationException
         *            if the state cannot be propagated at specified date
         */
        public void storeDate(final AbsoluteDate date) throws PropagationException {
            this.globalCurrentDate = date;
            this.softCurrentDate = this.globalCurrentDate;

            if (this.globalCurrentDate.compareTo(this.globalPreviousDate) == 0) {
                // Current date = previous date: the only way to known propagation direction is to compare global
                // propagation direction
                this.forward = this.globalForward;
            } else {
                this.forward = this.globalCurrentDate.compareTo(this.globalPreviousDate) >= 0;
            }

            this.setInterpolatedDate(this.globalCurrentDate);
        }

        /**
         * Store global forward direction flag.
         *
         * @param start
         *        start date
         * @param target
         *        target propagation date
         */
        public void storeForwardFlag(final AbsoluteDate start, final AbsoluteDate target) {
            this.globalForward = target.compareTo(start) >= 0;
        }

    }

}
