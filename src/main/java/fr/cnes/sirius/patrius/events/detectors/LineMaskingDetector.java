package fr.cnes.sirius.patrius.events.detectors;

import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.assembly.models.SensorModel;
import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.events.EventDetector;
import fr.cnes.sirius.patrius.events.detectors.LinkTypeHandler.SignalPropagationRole;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.Pair;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Detector for events relative to masking of two {@link PVCoordinatesProvider} points by a list of {@link BodyShape}
 * masking bodies. The two points are linked by a {@link Line} representation over time.
 * <p>
 * The g function is positive if the line (not infinite) does not intersect any masking body. If one masking body is
 * intersected, then the g function becomes negative.
 * </p>
 * <p>
 * <b><u>Warning:</u> </b> Constructing the detector providing the propagator as the mainElement PVCoordinatesProvider
 * might cause a StackOverFlow error in LIGHT_SPEED mode, because the g function will ask the propagator to evaluate a
 * specific date and the propagator will then call the g function.
 * <br>
 * </p>
 * <p>
 * This detector can take into account both signal propagation types
 * {@link fr.cnes.sirius.patrius.events.detectors.AbstractSignalPropagationDetector.PropagationDelayType#INSTANTANEOUS
 * INSTANTANEOUS} and
 * {@link fr.cnes.sirius.patrius.events.detectors.AbstractSignalPropagationDetector.PropagationDelayType#LIGHT_SPEED
 * LIGHT_SPEED}.<br>
 * For the latter case, the main element (spacecraft) is considered as the signal emitter or receiver depending on its
 * defined role and the other element being the receiver or emitter as a result.
 * </p>
 *
 * @author Thibaut BONIT
 * HISTORY
 * VERSION:4.16:OPENFD-550:25/04/2025:[PATRIUS] Detecteur de masquage par un corps celeste
 * END-HISTORY
 *
 * @since 4.16
 */
public class LineMaskingDetector extends AbstractSignalPropagationDetector {

    /** Serializable UID. */
    private static final long serialVersionUID = -8917493778977096213L;

    /** Main element position point */
    private final PVCoordinatesProvider mainElement;

    /** Masking bodies. */
    private final List<BodyShape> maskingBodies;

    /**
     * Builds a line masking detector.
     * <p>
     * This constructor takes default values for maximal checking interval ({@link #DEFAULT_MAXCHECK}) and convergence
     * threshold ({@link #DEFAULT_THRESHOLD}).
     * </p>
     * <p>
     * The default implementation slope selection is set to {@link EventDetector#INCREASING_DECREASING}, hence the
     * detector will detect start & end masking events.
     * </p>
     * <p>
     * The default implementation behavior is to {@link fr.cnes.sirius.patrius.events.EventDetector.Action#CONTINUE
     * continue} propagation at start masking and to {@link fr.cnes.sirius.patrius.events.EventDetector.Action#CONTINUE
     * continue} propagation when masking ends while keeping the detector.
     * </p>
     *
     * @param mainElement
     *        PVCoordinatesProvider of the initial point from which to compute the line of sight (if {@code null},
     *        {@code s.getOrbit()} will be used in the {@code g(s)} function)
     * @param mainRole
     *        role of the main element (EMITTER or RECEIVER)
     * @param otherElement
     *        PVCoordinatesProvider of the other element to compute the line of sight
     * @param maskingBodies
     *        list of bodies to check for masking
     * @throws IllegalArgumentException
     *         if the masking bodies list is empty
     */
    public LineMaskingDetector(final PVCoordinatesProvider mainElement, final SignalPropagationRole mainRole,
                               final PVCoordinatesProvider otherElement, final List<BodyShape> maskingBodies) {
        this(mainElement, mainRole, otherElement, maskingBodies, DEFAULT_MAXCHECK, DEFAULT_THRESHOLD);
    }

    /**
     * Builds a line masking detector.
     * <p>
     * The default implementation slope selection is set to {@link EventDetector#INCREASING_DECREASING}, hence the
     * detector will detect start & end masking events.
     * </p>
     * <p>
     * The default implementation behavior is to {@link fr.cnes.sirius.patrius.events.EventDetector.Action#CONTINUE
     * continue} propagation at start masking and to {@link fr.cnes.sirius.patrius.events.EventDetector.Action#CONTINUE
     * continue} propagation when masking ends while keeping the detector.
     * </p>
     *
     * @param mainElement
     *        PVCoordinatesProvider of the initial point from which to compute the line of sight (if {@code null},
     *        {@code s.getOrbit()} will be used in the {@code g(s)} function)
     * @param mainRole
     *        role of the main element (EMITTER or RECEIVER)
     * @param otherElement
     *        PVCoordinatesProvider of the other element to compute the line of sight
     * @param maskingBodies
     *        list of bodies to check for masking
     * @param maxCheck
     *        value for maximal checking interval
     * @param threshold
     *        convergence threshold
     * @throws IllegalArgumentException
     *         if the masking bodies list is empty
     */
    public LineMaskingDetector(final PVCoordinatesProvider mainElement, final SignalPropagationRole mainRole,
                               final PVCoordinatesProvider otherElement, final List<BodyShape> maskingBodies,
                               final double maxCheck, final double threshold) {
        this(mainElement, mainRole, otherElement, maskingBodies, maxCheck, threshold, Action.CONTINUE, Action.CONTINUE,
                false, false);
    }

    /**
     * Builds a line masking detector.
     *
     * <p>
     * The default implementation slope selection is set to {@link EventDetector#INCREASING_DECREASING}, hence the
     * detector will detect start & end masking events.
     * </p>
     *
     *
     * @param mainElement
     *        PVCoordinatesProvider of the initial point from which to compute the line of sight (if {@code null},
     *        {@code s.getOrbit()} will be used in the {@code g(s)} function)
     * @param mainRole
     *        role of the main element (EMITTER or RECEIVER)
     * @param otherElement
     *        PVCoordinatesProvider of the other element to compute the line of sight
     * @param maskingBodies
     *        list of bodies to check for masking
     * @param maxCheck
     *        value for maximal checking interval
     * @param threshold
     *        convergence threshold
     * @param start
     *        Action for propagation at start masking
     * @param end
     *        Action for propagation at end masking
     * @param removeAtStart
     *        States if the detector should be removed at start masking event detection
     * @param removeAtEnd
     *        States if the detector should be removed at end masking event detection
     * @throws IllegalArgumentException
     *         if the masking bodies list is empty
     */
    public LineMaskingDetector(final PVCoordinatesProvider mainElement, final SignalPropagationRole mainRole,
                               final PVCoordinatesProvider otherElement, final List<BodyShape> maskingBodies,
                               final double maxCheck, final double threshold, final Action start, final Action end,
                               final boolean removeAtStart, final boolean removeAtEnd) {
        this(mainElement, mainRole, otherElement, maskingBodies, EventDetector.INCREASING_DECREASING, maxCheck,
                threshold, start, end, removeAtStart, removeAtEnd);
    }

    /**
     * Builds a line masking detector.
     *
     *
     * @param mainElement
     *        PVCoordinatesProvider of the initial point from which to compute the line of sight (if {@code null},
     *        {@code s.getOrbit()} will be used in the {@code g(s)} function)
     * @param mainRole
     *        role of the main element (EMITTER or RECEIVER)
     * @param otherElement
     *        PVCoordinatesProvider of the other element to compute the line of sight
     * @param maskingBodies
     *        list of bodies to check for masking
     * @param slopeSelection
     *        g-function slope selection (0, 1, or 2)
     * @param maxCheck
     *        value for maximal checking interval
     * @param threshold
     *        convergence threshold
     * @param start
     *        Action for propagation at start masking
     * @param end
     *        Action for propagation at end masking
     * @param removeAtStart
     *        States if the detector should be removed at start masking event detection
     * @param removeAtEnd
     *        States if the detector should be removed at end masking event detection
     * @throws IllegalArgumentException
     *         if the masking bodies list is empty
     */
    public LineMaskingDetector(final PVCoordinatesProvider mainElement, final SignalPropagationRole mainRole,
                               final PVCoordinatesProvider otherElement, final List<BodyShape> maskingBodies,
                               final int slopeSelection, final double maxCheck, final double threshold,
                               final Action start, final Action end, final boolean removeAtStart,
                               final boolean removeAtEnd) {

        super(slopeSelection, maxCheck, threshold, start, end, removeAtStart, removeAtEnd,
                new LinkTypeHandler(mainRole, otherElement));

        // Check the masking bodies list is not empty (purpose of the detector
        if (maskingBodies.isEmpty()) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.EMPTY_MASKING_BODIES_LIST);
        }

        this.mainElement = mainElement;
        this.maskingBodies = maskingBodies;
    }

    /**
     * Getter for the main element position point (can be {@code null}).
     *
     * @return the main element position point
     */
    public PVCoordinatesProvider getMainElement() {
        return this.mainElement;
    }

    /**
     * Getter for the masking bodies.
     *
     * @return the masking bodies
     */
    public List<BodyShape> getMaskingBodies() {
        return this.maskingBodies;
    }

    /**
     * Assess if direction is occulted for entered spacecraft state.
     *
     * @param satProp
     *        spacecraft orbit propagator
     * @param date
     *        assessment date
     * @return {@code true} if direction is occulted, {@code false} if not occulted
     * @throws PatriusException
     *         if some specific error occurs
     */
    public boolean isDirectionOcculted(final Propagator satProp, final AbsoluteDate date) throws PatriusException {
        // Propagate the spacecraft at the date, then evaluate the g function on the state
        return g(satProp.propagate(date)) < 0.;
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public double g(final SpacecraftState s) throws PatriusException {

        final AbsoluteDate mainDate = s.getDate();
        final AbsoluteDate otherDate = getOtherDate(s);
        final Frame frame = s.getFrame();

        final PVCoordinatesProvider orbit = s.getOrbit();
        final Transform transformToSpacecraft = s.toTransform();
        final Frame frameCenteredMain = new Frame(frame, transformToSpacecraft, "");

        final Vector3D targetInFrameCenteredMain =
            getLinkTypeHandler().getOtherElement().getPVCoordinates(otherDate, frameCenteredMain).getPosition();

        // Minimal distance to the masking bodies
        final Pair<BodyShape, Double> minDistToMaskBody =
            SensorModel.computeMinDistToMaskingBodies(mainDate, orbit, getLinkTypeHandler().getMainRole(),
                frameCenteredMain, targetInFrameCenteredMain, this.maskingBodies, getPropagationDelayType(),
                getEpsilonSignalPropagation(), getMaxIterSignalPropagation());

        // Return result
        return minDistToMaskBody.getSecond();
    }

    /** {@inheritDoc} */
    @Override
    public LineMaskingDetector copy() {
        final LineMaskingDetector copiedDetector = new LineMaskingDetector(this.mainElement,
            getLinkTypeHandler().getMainRole(), getLinkTypeHandler().getOtherElement(),
            new ArrayList<>(this.maskingBodies), getSlopeSelection(), getMaxCheckInterval(), getThreshold(),
            getActionAtEntry(), getActionAtExit(), isRemoveAtEntry(), isRemoveAtExit());
        copiedDetector.setEpsilonSignalPropagation(this.getEpsilonSignalPropagation());
        copiedDetector.setPropagationDelayType(this.getPropagationDelayType(), getInertialFrame());
        return copiedDetector;
    }
}
