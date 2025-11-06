package fr.cnes.sirius.patrius.events.detectors;

import fr.cnes.sirius.patrius.bodies.ApparentRadiusProvider;
import fr.cnes.sirius.patrius.bodies.BodyPoint;
import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.ConstantRadiusProvider;
import fr.cnes.sirius.patrius.bodies.LLHCoordinatesSystem;
import fr.cnes.sirius.patrius.bodies.VariableRadiusProvider;
import fr.cnes.sirius.patrius.events.detectors.LinkTypeHandler.SignalPropagationRole;
import fr.cnes.sirius.patrius.forces.radiation.LightingRatio;
import fr.cnes.sirius.patrius.frames.CelestialBodyFrame;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.PatriusConfiguration;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;

/**
 * Common parts shared by several events detectors related to eclipse concept.
 * 
 * @see AbstractSignalPropagationDetector
 * 
 * @author Marc BELMONTE
 * HISTORY
 * VERSION:4.16:OPENFD-468:25/04/2025:[PATRIUS] Renommer toutes les mentions du GeodeticPoint
 * VERSION:4.16:OPENFD-442:25/04/2025:[PATRIUS] Calcul des eclipses d'un corps celeste
 * END-HISTORY
 *
 * @since 4.16
 */
public abstract class AbstractEclipseDetector extends AbstractSignalPropagationDetector {

    /** Serializable UID. */
    private static final long serialVersionUID = -6710297681132739803L;

    /** Exception message if the compatibility mode is unsupported. */
    protected static final String UNSUPPORTED_MODE_EXCEPTION = "Unsupported compatibility mode : ";

    /**
     * Threshold for the lighting ratio: if ratio >= 1 - &epsilon; --> ratio = 1, if ratio <=
     * &epsilon; --> ratio = 0.
     */
    protected static final double EPSILON_LIGHTING_RATIO = 1E-10;

    /** Flag for eclipse exit detection (slopeSelection = 0). */
    public static final int EXIT = 0;

    /** Flag for eclipse entry detection (slopeSelection = 1). */
    public static final int ENTRY = 1;

    /** Flag for eclipse entry/exit detection (slopeSelection = 2). */
    public static final int ENTRY_EXIT = 2;

    /** Occulting body. */
    private final PVCoordinatesProvider occultingBody;

    /** Occulting body radius provider. */
    private final ApparentRadiusProvider occultingRadiusProvider;

    /** Occulting body. */
    private final BodyShape occultingBodyShape;

    /** Occulted body. */
    private PVCoordinatesProvider occultedBody;

    /** Occulted body radius (m). */
    private final double occultedRadius;

    /**
     * Build a new eclipse detector with defined actions when entering and exiting the eclipse.
     * <p>
     * The occulting body is a sphere and the occulted body is a direction; the concept of umbra/penumbra does not apply
     * to this detector.
     * </p>
     *
     * @param occulting
     *        the occulting body
     * @param occultingRadius
     *        the occulting body radius (m)
     * @param maxCheck
     *        maximal checking interval (s)
     * @param threshold
     *        convergence threshold (s)
     * @param entry
     *        action performed when entering the eclipse
     * @param exit
     *        action performed when exiting the eclipse
     * @param removeEntry
     *        when the spacecraft point enters the zone.
     * @param removeExit
     *        when the spacecraft point leaves the zone.
     */
    public AbstractEclipseDetector(final PVCoordinatesProvider occulting,
                                   final double occultingRadius, final double maxCheck, final double threshold,
                                   final Action entry, final Action exit, final boolean removeEntry,
                                   final boolean removeExit) {
        super(maxCheck, threshold, entry, exit, removeEntry, removeExit, null);

        this.occultedBody = null;
        this.occultedRadius = 0.;
        this.occultingBody = occulting;
        this.occultingRadiusProvider = new ConstantRadiusProvider(MathLib.abs(occultingRadius));

        switch (PatriusConfiguration.getPatriusCompatibilityMode()) {
            case OLD_MODELS:
            case MIXED_MODELS:
                this.occultingBodyShape = null;
                break;
            case NEW_MODELS:
                // Occulted Body is initialized in the EclipseDetector class for a specific condition. Not cool but only
                // solution...
                this.occultingBodyShape = getOccultingShape();
                break;
            default:
                throw new IllegalArgumentException(
                    UNSUPPORTED_MODE_EXCEPTION + PatriusConfiguration.getPatriusCompatibilityMode());
        }
    }

    /**
     * Build a new eclipse detector with defined actions when entering and exiting the eclipse.
     * <p>
     * The occulting body is a sphere and the occulted body is a direction;
     * the concept of umbra/penumbra does apply to this detector by means of the lighting ratio.
     * </p>
     *
     * @param occulting
     *        the occulting body
     * @param maxCheck
     *        maximal checking interval (s)
     * @param threshold
     *        convergence threshold (s)
     * @param entry
     *        action performed when entering the eclipse
     * @param exit
     *        action performed when exiting the eclipse
     * @param removeEntry
     *        when the spacecraft point enters the zone
     * @param removeExit
     *        when the spacecraft point leaves the zone
     */
    public AbstractEclipseDetector(final BodyShape occulting,
                                   final double maxCheck, final double threshold,
                                   final Action entry, final Action exit,
                                   final boolean removeEntry, final boolean removeExit) {
        super(maxCheck, threshold, entry, exit, removeEntry, removeExit, null);

        this.occultedBody = null;
        this.occultedRadius = 0.;
        this.occultingBody = occulting;
        this.occultingRadiusProvider = new VariableRadiusProvider(occulting);

        switch (PatriusConfiguration.getPatriusCompatibilityMode()) {
            case OLD_MODELS:
            case MIXED_MODELS:
                this.occultingBodyShape = null;
                break;
            case NEW_MODELS:
                // Check if the occulted direction is an instance of ITargetDirection
                // Occulted Body is initialized in the EclipseDetector class for a specific condition. Not cool but only
                // solution...
                this.occultingBodyShape = getOccultingShape();
                break;

            default:
                throw new IllegalArgumentException(
                    UNSUPPORTED_MODE_EXCEPTION + PatriusConfiguration.getPatriusCompatibilityMode());
        }
    }

    /**
     * Build a new eclipse detector based on a lighting ratio. The occulted and occulting bodies are
     * both spherical.
     * <p>
     * The lighting ratio, whose value is between 0 and 1, establishes when an eclipse event should
     * be triggered. If 0, an event is detected only when the occulted body is completely hidden
     * (equivalent to an umbra detector), if 1, an event is detected every time the occulted body is
     * just partially hidden (equivalent to a penumbra detector). <br>
     * As a general rule, the lighting ratio is equal to 1 - the ratio between the hidden apparent
     * area of the occulted body and its total apparent area.
     * <p>
     *
     * @param occulted
     *        the occulted body
     * @param occultedRadiusIn
     *        the occulted body radius (m)
     * @param occulting
     *        the occulting body
     * @param occultingRadius
     *        the occulting body radius (m)
     * @param slopeSelection
     *        slope selection
     * @param maxCheck
     *        maximal checking interval (s)
     * @param threshold
     *        convergence threshold (s)
     * @param entry
     *        action performed when entering the eclipse
     * @param exit
     *        action performed when exiting the eclipse
     * @param removeEntry
     *        when the spacecraft point enters the zone.
     * @param removeExit
     *        when the spacecraft point leaves the zone.
     */
    public AbstractEclipseDetector(final PVCoordinatesProvider occulted, final double occultedRadiusIn,
                                   final PVCoordinatesProvider occulting, final double occultingRadius,
                                   final int slopeSelection, final double maxCheck, final double threshold,
                                   final Action entry, final Action exit, final boolean removeEntry,
                                   final boolean removeExit) {

        super(slopeSelection, maxCheck, threshold, entry, exit, removeEntry, removeExit,
                new LinkTypeHandler(SignalPropagationRole.RECEIVER, occulted));

        this.occultedBody = occulted;
        this.occultedRadius = MathLib.abs(occultedRadiusIn);
        this.occultingBody = occulting;
        this.occultingRadiusProvider = new ConstantRadiusProvider(MathLib.abs(occultingRadius));

        switch (PatriusConfiguration.getPatriusCompatibilityMode()) {
            case OLD_MODELS:
            case MIXED_MODELS:
                // occulted body PV coordinates provider is defined
                this.occultingBodyShape = null;
                break;

            case NEW_MODELS:
                // occulted body PV coordinates provider is defined
                this.occultingBodyShape = getOccultingShape();
                break;

            default:
                throw new IllegalArgumentException(
                    UNSUPPORTED_MODE_EXCEPTION + PatriusConfiguration.getPatriusCompatibilityMode());
        }
    }

    /**
     * Build a new eclipse detector based on a lighting ratio and with slope selection.
     * The occulted body is a sphere and the occulting body is a {@link BodyShape}.
     * <p>
     * The lighting ratio, whose value is between 0 and 1, establishes when an eclipse event should
     * be triggered. If 0, an event is detected only when the occulted body is completely hidden
     * (equivalent to an umbra detector), if 1, an event is detected every time the occulted body is
     * just partially hidden (equivalent to a penumbra detector). <br>
     * As a general rule, the lighting ratio is equal to 1 - the ratio between the hidden apparent
     * area of the occulted body and its total apparent area.
     * <p>
     *
     * @param occulted
     *        the occulted body
     * @param occultedRadiusIn
     *        the occulted body radius (m)
     * @param occultingBodyIn
     *        the occulting body
     * @param maxCheck
     *        maximal checking interval (s)
     * @param threshold
     *        convergence threshold (s)
     * @param entry
     *        action performed when entering the eclipse
     * @param exit
     *        action performed when exiting the eclipse
     * @param removeEntry
     *        when the spacecraft point enters the zone.
     * @param removeExit
     *        when the spacecraft point leaves the zone.
     * @param slopeSelection
     *        slope selection
     *
     * @throws ArithmeticException
     *         if occultingBodyRadius is NaN.
     * @since 4.5
     */
    public AbstractEclipseDetector(final PVCoordinatesProvider occulted, final double occultedRadiusIn,
                                   final BodyShape occultingBodyIn, final double maxCheck,
                                   final double threshold, final Action entry, final Action exit,
                                   final boolean removeEntry, final boolean removeExit, final int slopeSelection) {

        super(slopeSelection, maxCheck, threshold, entry, exit, removeEntry, removeExit,
                new LinkTypeHandler(SignalPropagationRole.RECEIVER, occulted));

        // occulted body PV coordinates provider is defined
        this.occultedBody = occulted;
        this.occultedRadius = MathLib.abs(occultedRadiusIn);
        this.occultingBody = occultingBodyIn;
        this.occultingRadiusProvider = new VariableRadiusProvider(occultingBodyIn);

        switch (PatriusConfiguration.getPatriusCompatibilityMode()) {
            case OLD_MODELS:
            case MIXED_MODELS:
                this.occultingBodyShape = null;
                break;
            case NEW_MODELS:
                this.occultingBodyShape = getOccultingShape();
                break;
            default:
                throw new IllegalArgumentException(UNSUPPORTED_MODE_EXCEPTION
                        + PatriusConfiguration.getPatriusCompatibilityMode());
        }
    }

    /**
     * Constructor with slope selection.
     *
     * @param occulted
     *        the occulted body
     * @param occultedRadiusIn
     *        the occulted body radius (m)
     * @param occulting
     *        the occulting body
     * @param occultingRadius
     *        the occulting body radius (m)
     * @param slopeSelection
     *        slope selection
     * @param maxCheck
     *        maximal checking interval (s)
     * @param threshold
     *        convergence threshold (s)
     * @param action
     *        action performed when entering/exiting the eclipse depending on slope selection
     * @param remove
     *        when the spacecraft point enters or exit the zone depending on slope selection
     */
    public AbstractEclipseDetector(final PVCoordinatesProvider occulted, final double occultedRadiusIn,
                                   final PVCoordinatesProvider occulting, final double occultingRadius,
                                   final int slopeSelection, final double maxCheck,
                                   final double threshold, final Action action, final boolean remove) {
        this(occulted, occultedRadiusIn, occulting, occultingRadius, slopeSelection, maxCheck, threshold, action,
                action, remove, remove);
    }

    /**
     * Get the occulting body.
     *
     * @return the occulting body
     */
    public PVCoordinatesProvider getOcculting() {
        return this.occultingBody;
    }

    /**
     * Get the occulting body shape
     *
     * @return the occulting body shape
     */
    protected BodyShape getOccultingBodyShape() {
        return this.occultingBodyShape;
    }

    /**
     * Get the occulted body.
     *
     * @return the occulted body
     */
    public PVCoordinatesProvider getOcculted() {
        return this.occultedBody;
    }

    /**
     * Set the occulted body.
     *
     * @param occultedBody
     *        the occulted body
     *
     */
    protected final void setOcculted(final PVCoordinatesProvider occultedBody) {
        this.occultedBody = occultedBody;
    }

    /**
     * Get the occulted body radius (m).
     *
     * @return the occulted body radius
     */
    public double getOccultedRadius() {
        return this.occultedRadius;
    }

    /**
     * Returns the occulting radius provider.
     *
     * @return the occultingRadiusProvider
     */
    public ApparentRadiusProvider getOccultingRadiusProvider() {
        return this.occultingRadiusProvider;
    }

    /**
     * Get the total eclipse detection flag.
     *
     * @return the total eclipse detection flag (true for umbra events detection, false for penumbra
     *         events detection)
     */
    public abstract boolean isTotalEclipse();

    /**
     * Get the eclipse flag.
     *
     * @param s
     *        the current state information : date, kinematics, attitude
     * @return an eclipse flag indicating whether the object is in eclipse: true if it is in eclipse, false if it is not
     * @throws PatriusException
     *         if some specific error occurs while retrieving the value of the switching function g(s)
     */
    public boolean isInEclipse(final SpacecraftState s) throws PatriusException {
        return g(s) < 0;
    }

    /**
     * Handle an eclipse event and choose what to do next.
     *
     * @param s
     *        the current state information : date, kinematics, attitude
     * @param increasing
     *        if true, the value of the switching function increases when times increases
     *        around event
     * @param forward
     *        if true, the integration variable (time) increases during integration.
     * @return the action performed when entering or exiting the eclipse.
     */
    @Override
    public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                final boolean forward) {
        final Action result;
        if (getSlopeSelection() == EXIT || getSlopeSelection() == ENTRY_EXIT && forward ^ !increasing) {
            // exist or decreasing case
            result = getActionAtExit();
            // remove (or not) detector
            this.shouldBeRemovedFlag = isRemoveAtExit();
        } else {
            // entry or decreasing case
            result = getActionAtEntry();
            // remove (or not) detector
            this.shouldBeRemovedFlag = isRemoveAtEntry();
        }
        return result;
    }

    /**
     * Build occulting shape.
     *
     * @return occulting shape
     */
    // CHECKSTYLE: stop MethodLength check
    // Reason: anonymous class
    private BodyShape getOccultingShape() {
        // CHECKSTYLE: resume MethodLength check

        final PVCoordinatesProvider occulting = getOcculting();
        final ApparentRadiusProvider occultingRadProvider = getOccultingRadiusProvider();

        return new BodyShape(){

            /** Serial UID. */
            private static final long serialVersionUID = 3000641605447850184L;

            /** {@inheritDoc} */
            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date,
                                                  final Frame frame)
                throws PatriusException {
                return occulting.getPVCoordinates(date, frame);
            }

            /** {@inheritDoc} */
            @Override
            public CelestialBodyFrame getBodyFrame() {
                try {
                    return (CelestialBodyFrame) occulting
                        .getNativeFrame(AbsoluteDate.J2000_EPOCH);
                } catch (final PatriusException e) {
                    throw new PatriusRuntimeException(PatriusMessages.INTERNAL_ERROR, e);
                }
            }

            /** {@inheritDoc} */
            @Override
            public double getApparentRadius(final PVCoordinatesProvider pvObserver,
                                            final AbsoluteDate date,
                                            final PVCoordinatesProvider occultedBodyIn,
                                            final PropagationDelayType propagationDelayType) {
                return occultingRadProvider
                    .getApparentRadius(pvObserver, date, occultedBodyIn, propagationDelayType);
            }

            /** {@inheritDoc} */
            @Override
            public Frame getNativeFrame(final AbsoluteDate date) {
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public void setLLHCoordinatesSystem(final LLHCoordinatesSystem coordSystem) {
                // Nothing to do
            }

            /** {@inheritDoc} */
            @Override
            public void setEpsilonSignalPropagation(final double epsilon) {
                // Nothing to do
            }

            /** {@inheritDoc} */
            @Override
            public void setDistanceEpsilon(final double epsilon) {
                // Nothing to do
            }

            /** {@inheritDoc} */
            @Override
            public BodyShape resize(final MarginType marginType,
                                    final double marginValue) {
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public boolean isDefaultLLHCoordinatesSystem() {
                return false;
            }

            /** {@inheritDoc} */
            @Override
            public String getName() {
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public LLHCoordinatesSystem getLLHCoordinatesSystem() {
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public BodyPoint[] getIntersectionPoints(final Line line,
                                                     final Frame frame,
                                                     final AbsoluteDate date) {
                return new BodyPoint[0];
            }

            /** {@inheritDoc} */
            @Override
            public BodyPoint getIntersectionPoint(final Line line,
                                                  final Vector3D close,
                                                  final Frame frame,
                                                  final AbsoluteDate date,
                                                  final double altitude) {
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public BodyPoint getIntersectionPoint(final Line line,
                                                  final Vector3D close,
                                                  final Frame frame,
                                                  final AbsoluteDate date,
                                                  final String name) {
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public BodyPoint getIntersectionPoint(final Line line,
                                                  final Vector3D close,
                                                  final Frame frame,
                                                  final AbsoluteDate date) {
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public double getEpsilonSignalPropagation() {
                return 0;
            }

            /** {@inheritDoc} */
            @Override
            public double getEncompassingSphereRadius() {
                return 0;
            }

            /** {@inheritDoc} */
            @Override
            public double getDistanceEpsilon() {
                return 0;
            }

            /** {@inheritDoc} */
            @Override
            public double distanceTo(final Line line,
                                     final Frame frame,
                                     final AbsoluteDate date) {
                return 0;
            }

            /** {@inheritDoc} */
            @Override
            public BodyPoint closestPointTo(final Vector3D point,
                                            final String name) {
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public BodyPoint closestPointTo(final Vector3D point) {
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public BodyPoint closestPointTo(final Vector3D point,
                                            final Frame frame,
                                            final AbsoluteDate date) {
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public BodyPoint[] closestPointTo(final Line line) {
                return new BodyPoint[0];
            }

            /** {@inheritDoc} */
            @Override
            public BodyPoint[] closestPointTo(final Line line,
                                              final Frame frame,
                                              final AbsoluteDate date) {
                return new BodyPoint[0];
            }

            /** {@inheritDoc} */
            @Override
            public BodyPoint buildPoint(final Vector3D position,
                                        final Frame frame,
                                        final AbsoluteDate date,
                                        final String name) {
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public BodyPoint buildPoint(final Vector3D position,
                                        final String name) {
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public BodyPoint buildPoint(final LLHCoordinatesSystem coordSystem,
                                        final double latitude,
                                        final double longitude,
                                        final double height,
                                        final String name) {
                return null;
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public void setPropagationDelayType(final PropagationDelayType propagationDelayType, final Frame frame) {
        if (this.occultedBody == null && PropagationDelayType.LIGHT_SPEED == propagationDelayType) {
            // In case if IDirection, PropagationDelayType.LIGHT_SPEED is not allowed
            throw PatriusException.createIllegalArgumentException(PatriusMessages.LIGHT_SPEED_FORBIDDEN);
        }
        super.setPropagationDelayType(propagationDelayType, frame);
    }

    /** {@inheritDoc} */
    @Override
    public PVCoordinatesProvider getEmitter(final SpacecraftState s) {
        return this.occultedBody;
    }

    /** {@inheritDoc} */
    @Override
    public PVCoordinatesProvider getReceiver(final SpacecraftState s) {
        return s.getOrbit();
    }

    /** {@inheritDoc} */
    @Override
    public EventDatationType getEventDatationType() {
        return EventDatationType.RECEIVER;
    }

    /**
     * Builds the LightingRatio object
     *
     * @param occultedBody
     *        occulted body
     *
     * @return the LightingRatio object
     */
    protected LightingRatio buildLightingRatioComputer(final PVCoordinatesProvider occultedBody) {
        final LightingRatio lightingRatioComputer = new LightingRatio(getOccultingBodyShape(), occultedBody,
            getOccultedRadius());
        lightingRatioComputer.setPropagationDelayType(getPropagationDelayType(), getInertialFrame());
        lightingRatioComputer.setEpsilonSignalPropagation(getEpsilonSignalPropagation());
        lightingRatioComputer.setMaxIterSignalPropagation(getMaxIterSignalPropagation());
        return lightingRatioComputer;
    }
}
