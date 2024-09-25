/**
 * HISTORY
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11:DM:DM-3197:22/05/2023:[PATRIUS] Deplacement dans PATRIUS de classes façade ALGO DV SIRUS 
 * VERSION:4.11:DM:DM-3295:22/05/2023:[PATRIUS] Conditions meteorologiques variables dans modeles troposphere
 * VERSION:4.11:DM:DM-3235:22/05/2023:[PATRIUS][TEMPS_CALCUL] Attitude spacecraft state lazy
 * END-HISTORY
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
package fr.cnes.sirius.patrius.events.detectors;

import fr.cnes.sirius.patrius.events.AbstractDetector;
import fr.cnes.sirius.patrius.events.EventDetector;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.MeanOsculatingElementsProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Centered argument of latitude detector
 *
 * @author GMV
 */
public final class CenteredAolPassageDetector extends AbstractDetector {

    /** Serial version UID */
    private static final long serialVersionUID = 8379262886080248402L;

    /** Centered AOL triggering the event */
    private final double aol;

    /** Anomaly type */
    private final PositionAngle positionAngle;

    /** Converter from orbit osculating elements to mean elements */
    private final MeanOsculatingElementsProvider provider;

    /** Patrius frame */
    private final Frame pFrame;

    /** Action performed at centered AOL detection when ascending */
    private final Action action;

    /**
     * Constructor
     *
     * @param aol
     *        Centered AOL [rad] triggering the event
     * @param positionAngle
     *        Anomaly type
     * @param provider
     *        Converter from orbit osculating elements to mean elements
     * @param frame the patrius frame
     * @throws PatriusException
     *         Possible exception
     */
    public CenteredAolPassageDetector(final double aol, final PositionAngle positionAngle,
            final MeanOsculatingElementsProvider provider, final Frame frame) throws PatriusException {
        this(aol, positionAngle, provider, frame, DEFAULT_MAXCHECK, DEFAULT_THRESHOLD);
    }

    /**
     * Constructor with complementary parameters
     *
     * @param aol
     *        Centered AOL [rad] triggering the event
     * @param positionAngle
     *        Anomaly type
     * @param provider
     *        Converter from orbit osculating elements to mean elements
     * @param frame the patrius frame
     * @param maxCheck
     *        Maximum check (see {@link AbstractDetector})
     * @param threshold
     *        Threshold (see {@link AbstractDetector})
     * @throws PatriusException
     *         Possible exception
     */
    public CenteredAolPassageDetector(final double aol, final PositionAngle positionAngle,
            final MeanOsculatingElementsProvider provider, final Frame frame, final double maxCheck,
            final double threshold) throws PatriusException {
        this(aol, positionAngle, provider, frame, maxCheck, threshold, Action.STOP);
    }

    /**
     * Constructor with complementary parameters
     *
     * @param aol
     *        Centered AOL [rad triggering the event
     * @param positionAngle
     *        Anomaly type
     * @param provider
     *        Converter from orbit osculating elements to mean elements
     * @param frame the patrius frame
     * @param maxCheck
     *        Maximum check (see {@link AbstractDetector})
     * @param threshold
     *        Threshold (see {@link AbstractDetector})
     * @param action
     *        Action to do when event is detected
     * @throws PatriusException
     *         Possible exception
     */
    public CenteredAolPassageDetector(final double aol, final PositionAngle positionAngle,
            final MeanOsculatingElementsProvider provider, final Frame frame, final double maxCheck,
            final double threshold, final Action action) {

        super(EventDetector.INCREASING, maxCheck, threshold);

        this.aol = aol;
        this.positionAngle = positionAngle;
        this.provider = provider;
        this.pFrame = frame;
        this.action = action;
    }

    /** {@inheritDoc} */
    @Override
    public final Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
            throws PatriusException {
        return this.action;
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public final double g(final SpacecraftState s) throws PatriusException {
        // Compute centered orbit state from osculating one, and retrieve AOL
        final CircularOrbit orbit = new CircularOrbit(s.getOrbit().getPVCoordinates(this.pFrame), this.pFrame,
                s.getDate(), s.getMu());
        final double aolCen = ((CircularOrbit) this.provider.osc2mean(orbit)).getAlpha(this.positionAngle);
        // Return sine of delta to circumvent sign change issues
        return MathLib.sin(aolCen - this.aol);
    }

    /**
     * Get osculating {@link CircularOrbit} from centered counterpart
     *
     * @param centeredOrbit
     *        Centered orbit
     * @return Osculating orbit
     * @throws PatriusException
     *         Possible exception
     */
    public Orbit centeredToOsculating(final CircularOrbit centeredOrbit) throws PatriusException {
        return this.provider.mean2osc(centeredOrbit);

    }

    /** {@inheritDoc} */
    @Override
    public EventDetector copy() {
        return new CenteredAolPassageDetector(aol, positionAngle, provider, pFrame, getMaxCheckInterval(),
            getThreshold(), action);
    }

}
