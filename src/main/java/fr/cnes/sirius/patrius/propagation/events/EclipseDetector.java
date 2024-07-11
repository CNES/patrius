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
 * VERSION:4.9.1:FA:FA-3193:01/06/2022:[PATRIUS] Revenir a la signature initiale de la methode getLocalRadius
 * VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3165:10/05/2022:[PATRIUS] Amelioration de la propagation du signal 
 * VERSION:4.9:DM:DM-3181:10/05/2022:[PATRIUS] Passage a protected de la methode setPropagationDelayType
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:FA:FA-3167:10/05/2022:[PATRIUS] La methode ObJMeshLoader ne permet pas de lire des fichiers au format .obj 
 * VERSION:4.9:DM:DM-3133:10/05/2022:[PATRIUS] Ajout de plusieurs fonctionnalites a la classe EclipseDetector 
 * VERSION:4.9:FA:FA-3170:10/05/2022:[PATRIUS] Incoherence de datation entre la methode getLocalRadius de GeometricBodyShape...
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2245:27/05/2020:Ameliorations de EclipseDetector 
 * VERSION:4.5:DM:DM-2460:27/05/2020:Prise en compte des temps de propagation dans les calculs evenements
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014:Modified maneuvers in retro-propagation case
 *                            (added forward parameter to eventOccurred signature)
 * VERSION::DM:227:02/10/2014:Merged eclipse detectors and added eclipse detector by lighting ratio
 * VERSION::FA:261:13/10/2014:JE V2.2 corrections (move IDirection in package attitudes.directions)
 * VERSION::FA:382:09/12/2014:Eclipse detector corrections
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::FA:491:04/11/2015: Added possibility to the satellite to be under the occulting body surface
 * VERSION::DM:454:24/11/2015:Add constructors, overload method shouldBeRemoved() and adapt eventOccured()
 * VERSION::DM:611:04/08/2016:New implementation using radii provider for visibility of main/inhibition targets
 * VERSION::FA:829:25/01/2017:Protection of trigonometric methods
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import fr.cnes.sirius.patrius.attitudes.directions.IDirection;
import fr.cnes.sirius.patrius.attitudes.directions.ITargetDirection;
import fr.cnes.sirius.patrius.bodies.GeometricBodyShape;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Finder for satellite eclipse related events.
 * <p>
 * This class finds eclipse events, i.e. satellite within umbra (total eclipse) or penumbra (partial eclipse).
 * </p>
 * <p>
 * The default implementation behavior is to {@link EventDetector.Action#CONTINUE continue} propagation when entering
 * the eclipse and to {@link EventDetector.Action#STOP stop} propagation when exiting the eclipse. This can be changed
 * by using some constructors.
 * <p>
 * This detector can takes into account signal propagation duration through
 * {@link #setPropagationDelayType(PropagationDelayType, fr.cnes.sirius.patrius.frames.Frame)} 
 * (default is signal being instantaneous).
 * </p>
 * 
 * @see fr.cnes.sirius.patrius.propagation.Propagator#addEventDetector(EventDetector)
 * @author Pascal Parraud
 */
@SuppressWarnings("PMD.NullAssignment")
public class EclipseDetector extends AbstractDetector {

    /** Flag for eclipse exit detection (slopeSelection = 0). */
    public static final int EXIT = 0;

    /** Flag for eclipse entry detection (slopeSelection = 1). */
    public static final int ENTRY = 1;

    /** Flag for eclipse entry/exit detection (slopeSelection = 2). */
    public static final int ENTRY_EXIT = 2;

    /** Serializable UID. */
    private static final long serialVersionUID = -541311550206363031L;

    /**
     * Threshold for the lighting ratio: if ratio >= 1 - &epsilon; --> ratio = 1, if ratio <=
     * &epsilon; --> ratio = 0.
     */
    private static final double EPSILON = 1E-10;

    /** Occulting body. */
    private final PVCoordinatesProvider occultingBody;

    /** Occulting body radius provider. */
    private final LocalRadiusProvider occultingRadiusProvider;

    /** True if the occulted {@link PVCoordinatesProvider} is defined, false if it is not. */
    private final boolean isOccultedPVProvDefined;

    /** Occulted direction. */
    private final IDirection occultedDirection;

    /** Occulted body. */
    private transient PVCoordinatesProvider occultedBody;

    /** Occulted body radius (m). */
    private final double occultedRadius;

    /** Umbra, if true, or penumbra, if false, detection flag. */
    private boolean totalEclipse;

    /** True if the eclipse detection is based on a lighting ratio value. */
    private boolean lightingRatioDetection;

    /** The lighting ratio value (0 < ratio < 1, 0 for total eclipses, 1 for penumbra events). */
    private double ratio;

    /**
     * Build a new eclipse detector.
     * <p>
     * The occulting body is a sphere and the occulted body is a direction; the concept of umbra/penumbra does not apply
     * to this detector.
     * </p>
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#CONTINUE continue} propagation when
     * entering the eclipse and to {@link EventDetector.Action#STOP stop} propagation when exiting the eclipse.
     * </p>
     * 
     * @param occulted the direction to be occulted
     * @param occulting the occulting body
     * @param occultingRadius the occulting body radius (m)
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     */
    public EclipseDetector(final IDirection occulted, final PVCoordinatesProvider occulting,
                           final double occultingRadius, final double maxCheck, final double threshold) {
        this(occulted, occulting, occultingRadius, maxCheck, threshold, Action.CONTINUE,
                Action.STOP);
    }

    /**
     * Build a new eclipse detector with defined actions when entering and exiting the eclipse.
     * <p>
     * The occulting body is a sphere and the occulted body is a direction; the concept of umbra/penumbra does not apply
     * to this detector.
     * </p>
     * 
     * @param occulted the direction to be occulted
     * @param occulting the occulting body
     * @param occultingRadius the occulting body radius (m)
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param entry action performed when entering the eclipse
     * @param exit action performed when exiting the eclipse
     */
    public EclipseDetector(final IDirection occulted, final PVCoordinatesProvider occulting,
                           final double occultingRadius, final double maxCheck, final double threshold,
                           final Action entry, final Action exit) {
        this(occulted, occulting, occultingRadius, maxCheck, threshold, entry, exit, false, false);
    }

    /**
     * Build a new eclipse detector with defined actions when entering and exiting the eclipse.
     * <p>
     * The occulting body is a sphere and the occulted body is a direction; the concept of umbra/penumbra does not apply
     * to this detector.
     * </p>
     * 
     * @param occulted the direction to be occulted
     * @param occulting the occulting body
     * @param occultingRadius the occulting body radius (m)
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param entry action performed when entering the eclipse
     * @param exit action performed when exiting the eclipse
     * @param removeEntry when the spacecraft point enters the zone.
     * @param removeExit when the spacecraft point leaves the zone.
     */
    public EclipseDetector(final IDirection occulted, final PVCoordinatesProvider occulting,
                           final double occultingRadius, final double maxCheck, final double threshold,
                           final Action entry, final Action exit, final boolean removeEntry,
                           final boolean removeExit) {
        super(maxCheck, threshold, entry, exit, removeEntry, removeExit);
        this.totalEclipse = true;
        // Occulted body initialisation
        this.occultedDirection = occulted;
        // Check if the occulted direction is an instance of ITargetDirection
        if (this.occultedDirection instanceof ITargetDirection) {
            // The occulted body PV coordinates provider is defined
            this.isOccultedPVProvDefined = true;
        } else {
            // The occulted body PV coordinates provider is not defined
            this.isOccultedPVProvDefined = false;
        }
        this.occultedBody = null;
        this.occultedRadius = 0.;

        // Occulting body initialisation:
        this.occultingBody = occulting;
        this.occultingRadiusProvider = new ConstantRadiusProvider(MathLib.abs(occultingRadius));
    }
    
    /**
     * Build a new eclipse detector with defined actions when entering and exiting the eclipse.
     * <p>
     * The occulting body is a sphere and the occulted body is a direction; the concept of umbra/penumbra does apply to
     * this detector by means of the lightning ratio.
     * </p>
     * 
     * @param occulted the direction to be occulted
     * @param occulting the occulting body
     * @param lightingRatio the lighting ratio: 0 when total eclipse events should be detected, 1 for penumbra events
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param entry action performed when entering the eclipse
     * @param exit action performed when exiting the eclipse
     * @param removeEntry when the spacecraft point enters the zone
     * @param removeExit when the spacecraft point leaves the zone
     */
    public EclipseDetector(final IDirection occulted, final GeometricBodyShape occulting, final double lightingRatio,
                           final double maxCheck, final double threshold, final Action entry, final Action exit,
                           final boolean removeEntry, final boolean removeExit) {
        super(maxCheck, threshold, entry, exit, removeEntry, removeExit);

        // Occulted body initialisation
        this.occultedDirection = occulted;
        // Check if the occulted direction is an instance of ITargetDirection
        if (this.occultedDirection instanceof ITargetDirection) {
            // The occulted body PV coordinates provider is defined
            this.isOccultedPVProvDefined = true;
        } else {
            // The occulted body PV coordinates provider is not defined
            this.isOccultedPVProvDefined = false;
        }
        this.occultedBody = null;
        this.occultedRadius = 0.;

        // Occulting body initialisation:
        this.occultingBody = occulting;
        this.occultingRadiusProvider = new VariableRadiusProvider(occulting);

        // Use the lightning ratio to determine the nature of the detector
        if (lightingRatio >= 1 - EPSILON) {
            // in this case, it is a penumbra eclipse detector:
            this.totalEclipse = false;
            this.lightingRatioDetection = false;
            this.ratio = 1.0;

        } else if (lightingRatio <= EPSILON) {
            // in this case, it is an umbra detector:
            this.totalEclipse = true;
            this.lightingRatioDetection = false;
            this.ratio = 0.0;

        } else {
            // partial detection:
            this.totalEclipse = false;
            this.lightingRatioDetection = true;
            this.ratio = lightingRatio;
        }
    }


    /**
     * Build a new eclipse detector based on a lighting ratio. The occulted and occulting bodies are
     * both spherical.
     * <p>
     * The lighting ratio, whose value is between 0 and 1, establishes when an eclipse event should be triggered. If 0,
     * an event is detected only when the occulted body is completely hidden (equivalent to an umbra detector), if 1, an
     * event is detected every time the occulted body is just partially hidden (equivalent to a penumbra detector). <br>
     * As a general rule, the lighting ratio is equal to 1 - the ratio between the hidden apparent area of the occulted
     * body and its total apparent area.
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#CONTINUE continue} propagation when
     * entering the eclipse and to {@link EventDetector.Action#STOP stop} propagation when exiting the eclipse.
     * </p>
     * 
     * @param occulted the occulted body
     * @param occultedRadiusIn the occulted body radius (m)
     * @param occulting the occulting body
     * @param occultingRadius the occulting body radius (m)
     * @param lightingRatio the lighting ratio: 0 when total eclipse events should be detected, 1
     *        for penumbra events.
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     */
    public EclipseDetector(final PVCoordinatesProvider occulted, final double occultedRadiusIn,
                           final PVCoordinatesProvider occulting, final double occultingRadius,
                           final double lightingRatio, final double maxCheck, final double threshold) {
        this(occulted, occultedRadiusIn, occulting, occultingRadius, lightingRatio, maxCheck,
                threshold, Action.CONTINUE, Action.STOP);
    }

    /**
     * Build a new eclipse detector based on a lighting ratio. The occulted and occulting bodies are
     * both spherical.
     * <p>
     * The lighting ratio, whose value is between 0 and 1, establishes when an eclipse event should be triggered. If 0,
     * an event is detected only when the occulted body is completely hidden (equivalent to an umbra detector), if 1, an
     * event is detected every time the occulted body is just partially hidden (equivalent to a penumbra detector). <br>
     * As a general rule, the lighting ratio is equal to 1 - the ratio between the hidden apparent area of the occulted
     * body and its total apparent area.
     * 
     * @param occulted the occulted body
     * @param occultedRadiusIn the occulted body radius (m)
     * @param occulting the occulting body
     * @param occultingRadius the occulting body radius (m)
     * @param lightingRatio the lighting ratio: 0 when total eclipse events should be detected, 1
     *        for penumbra events.
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param entry action performed when entering the eclipse
     * @param exit action performed when exiting the eclipse
     */
    public EclipseDetector(final PVCoordinatesProvider occulted, final double occultedRadiusIn,
                           final PVCoordinatesProvider occulting, final double occultingRadius,
                           final double lightingRatio, final double maxCheck, final double threshold,
                           final Action entry, final Action exit) {

        this(occulted, occultedRadiusIn, occulting, occultingRadius, lightingRatio, maxCheck,
                threshold, entry, exit, false, false);
    }

    /**
     * Build a new eclipse detector based on a lighting ratio. The occulted and occulting bodies are
     * both spherical.
     * <p>
     * The lighting ratio, whose value is between 0 and 1, establishes when an eclipse event should be triggered. If 0,
     * an event is detected only when the occulted body is completely hidden (equivalent to an umbra detector), if 1, an
     * event is detected every time the occulted body is just partially hidden (equivalent to a penumbra detector). <br>
     * As a general rule, the lighting ratio is equal to 1 - the ratio between the hidden apparent area of the occulted
     * body and its total apparent area.
     * 
     * @param occulted the occulted body
     * @param occultedRadiusIn the occulted body radius (m)
     * @param occulting the occulting body
     * @param occultingRadius the occulting body radius (m)
     * @param lightingRatio the lighting ratio: 0 when total eclipse events should be detected, 1
     *        for penumbra events.
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param entry action performed when entering the eclipse
     * @param exit action performed when exiting the eclipse
     * @param removeEntry when the spacecraft point enters the zone.
     * @param removeExit when the spacecraft point leaves the zone.
     */
    public EclipseDetector(final PVCoordinatesProvider occulted, final double occultedRadiusIn,
                           final PVCoordinatesProvider occulting, final double occultingRadius,
                           final double lightingRatio, final double maxCheck, final double threshold,
                           final Action entry, final Action exit, final boolean removeEntry,
                           final boolean removeExit) {
        super(maxCheck, threshold, entry, exit, removeEntry, removeExit);
        // occulted body PV coordinates provider is defined
        this.isOccultedPVProvDefined = true;
        this.occultedDirection = null;
        this.occultedBody = occulted;
        this.occultedRadius = MathLib.abs(occultedRadiusIn);
        this.occultingBody = occulting;
        this.occultingRadiusProvider = new ConstantRadiusProvider(MathLib.abs(occultingRadius));

        if (lightingRatio >= 1 - EPSILON) {
            // in this case, it is a penumbra eclipse detector:
            this.totalEclipse = false;
            this.lightingRatioDetection = false;
            this.ratio = 1.0;

        } else if (lightingRatio <= EPSILON) {
            // in this case, it is an umbra detector:
            this.totalEclipse = true;
            this.lightingRatioDetection = false;
            this.ratio = 0.0;

        } else {
            // partial detection:
            this.totalEclipse = false;
            this.lightingRatioDetection = true;
            this.ratio = lightingRatio;
        }
    }

    /**
     * Build a new eclipse detector based on a lighting ratio. The occulted body is a sphere and the
     * occulting body is a {@link GeometricBodyShape}.
     * <p>
     * The lighting ratio, whose value is between 0 and 1, establishes when an eclipse event should be triggered. If 0,
     * an event is detected only when the occulted body is completely hidden (equivalent to an umbra detector), if 1, an
     * event is detected every time the occulted body is just partially hidden (equivalent to a penumbra detector). <br>
     * As a general rule, the lighting ratio is equal to 1 - the ratio between the hidden apparent area of the occulted
     * body and its total apparent area.
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#CONTINUE continue} propagation when
     * entering the eclipse and to {@link EventDetector.Action#STOP stop} propagation when exiting the eclipse.
     * </p>
     * 
     * @param occulted the occulted body
     * @param occultedRadiusIn the occulted body radius (m)
     * @param occultingBodyIn the occulting body
     * @param lightingRatio the lighting ratio: 0 when total eclipse events should be detected, 1
     *        for penumbra events.
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     */
    public EclipseDetector(final PVCoordinatesProvider occulted, final double occultedRadiusIn,
                           final GeometricBodyShape occultingBodyIn, final double lightingRatio,
                           final double maxCheck, final double threshold) {
        this(occulted, occultedRadiusIn, occultingBodyIn, lightingRatio, maxCheck, threshold,
                Action.CONTINUE, Action.STOP);
    }

    /**
     * Build a new eclipse detector based on a lighting ratio. The occulted body is a sphere and the
     * occulting body is a {@link GeometricBodyShape}.
     * <p>
     * The lighting ratio, whose value is between 0 and 1, establishes when an eclipse event should be triggered. If 0,
     * an event is detected only when the occulted body is completely hidden (equivalent to an umbra detector), if 1, an
     * event is detected every time the occulted body is just partially hidden (equivalent to a penumbra detector). <br>
     * As a general rule, the lighting ratio is equal to 1 - the ratio between the hidden apparent area of the occulted
     * body and its total apparent area.
     * 
     * @param occulted the occulted body
     * @param occultedRadiusIn the occulted body radius (m)
     * @param occultingBodyIn the occulting body
     * @param lightingRatio the lighting ratio: 0 when total eclipse events should be detected, 1
     *        for penumbra events.
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param entry action performed when entering the eclipse
     * @param exit action performed when exiting the eclipse
     */
    public EclipseDetector(final PVCoordinatesProvider occulted, final double occultedRadiusIn,
                           final GeometricBodyShape occultingBodyIn, final double lightingRatio,
                           final double maxCheck, final double threshold, final Action entry, final Action exit) {
        this(occulted, occultedRadiusIn, occultingBodyIn, lightingRatio, maxCheck, threshold,
                entry, exit, false, false);
    }

    /**
     * Build a new eclipse detector based on a lighting ratio. The occulted body is a sphere and the
     * occulting body is a {@link GeometricBodyShape}.
     * <p>
     * The lighting ratio, whose value is between 0 and 1, establishes when an eclipse event should be triggered. If 0,
     * an event is detected only when the occulted body is completely hidden (equivalent to an umbra detector), if 1, an
     * event is detected every time the occulted body is just partially hidden (equivalent to a penumbra detector). <br>
     * As a general rule, the lighting ratio is equal to 1 - the ratio between the hidden apparent area of the occulted
     * body and its total apparent area.
     * 
     * @param occulted the occulted body
     * @param occultedRadiusIn the occulted body radius (m)
     * @param occultingBodyIn the occulting body
     * @param lightingRatio the lighting ratio: 0 when total eclipse events should be detected, 1
     *        for penumbra events.
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param entry action performed when entering the eclipse
     * @param exit action performed when exiting the eclipse
     * @param removeEntry when the spacecraft point enters the zone.
     * @param removeExit when the spacecraft point leaves the zone.
     * @since 3.1
     */
    public EclipseDetector(final PVCoordinatesProvider occulted, final double occultedRadiusIn,
                           final GeometricBodyShape occultingBodyIn, final double lightingRatio,
                           final double maxCheck, final double threshold, final Action entry, final Action exit,
                           final boolean removeEntry, final boolean removeExit) {
        this(occulted, occultedRadiusIn, occultingBodyIn, lightingRatio, maxCheck, threshold,
                entry, exit, removeEntry, removeExit, ENTRY_EXIT);
    }

    /**
     * Build a new eclipse detector based on a lighting ratio and with slope selection.
     * The occulted body is a sphere and the occulting body is a {@link GeometricBodyShape}.
     * <p>
     * The lighting ratio, whose value is between 0 and 1, establishes when an eclipse event should be triggered. If 0,
     * an event is detected only when the occulted body is completely hidden (equivalent to an umbra detector), if 1, an
     * event is detected every time the occulted body is just partially hidden (equivalent to a penumbra detector). <br>
     * As a general rule, the lighting ratio is equal to 1 - the ratio between the hidden apparent area of the occulted
     * body and its total apparent area.
     * 
     * @param occulted the occulted body
     * @param occultedRadiusIn the occulted body radius (m)
     * @param occultingBodyIn the occulting body
     * @param lightingRatio the lighting ratio: 0 when total eclipse events should be detected, 1
     *        for penumbra events.
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param slopeSelection slope selection
     * @throws ArithmeticException if occultingBodyRadius is NaN.
     * @since 4.5
     */
    public EclipseDetector(final PVCoordinatesProvider occulted, final double occultedRadiusIn,
                           final GeometricBodyShape occultingBodyIn, final double lightingRatio,
                           final double maxCheck, final double threshold, final int slopeSelection)
        throws ArithmeticException {
        this(occulted, occultedRadiusIn, occultingBodyIn, lightingRatio, maxCheck, threshold,
                Action.CONTINUE, Action.STOP, false, false, slopeSelection);
    }

    /**
     * Build a new eclipse detector based on a lighting ratio and with slope selection.
     * The occulted body is a sphere and the occulting body is a {@link GeometricBodyShape}.
     * <p>
     * The lighting ratio, whose value is between 0 and 1, establishes when an eclipse event should be triggered. If 0,
     * an event is detected only when the occulted body is completely hidden (equivalent to an umbra detector), if 1, an
     * event is detected every time the occulted body is just partially hidden (equivalent to a penumbra detector). <br>
     * As a general rule, the lighting ratio is equal to 1 - the ratio between the hidden apparent area of the occulted
     * body and its total apparent area.
     * 
     * @param occulted the occulted body
     * @param occultedRadiusIn the occulted body radius (m)
     * @param occultingBodyIn the occulting body
     * @param lightingRatio the lighting ratio: 0 when total eclipse events should be detected, 1
     *        for penumbra events.
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param action action performed when entering/exiting the eclipse depending on slope selection
     * @param remove when the spacecraft point enters or exit the zone depending on slope selection
     * @param slopeSelection slope selection
     * @throws ArithmeticException if occultingBodyRadius is NaN.
     * @since 4.5
     */

    public EclipseDetector(final PVCoordinatesProvider occulted, final double occultedRadiusIn,
                           final GeometricBodyShape occultingBodyIn, final double lightingRatio,
                           final double maxCheck, final double threshold, final Action action,
                           final boolean remove, final int slopeSelection)
        throws ArithmeticException {
        this(occulted, occultedRadiusIn, occultingBodyIn, lightingRatio, maxCheck, threshold,
                action, action, remove, remove, slopeSelection);
    }

    /**
     * Build a new eclipse detector based on a lighting ratio and with slope selection.
     * The occulted body is a sphere and the occulting body is a {@link GeometricBodyShape}.
     * <p>
     * The lighting ratio, whose value is between 0 and 1, establishes when an eclipse event should be triggered. If 0,
     * an event is detected only when the occulted body is completely hidden (equivalent to an umbra detector), if 1, an
     * event is detected every time the occulted body is just partially hidden (equivalent to a penumbra detector). <br>
     * As a general rule, the lighting ratio is equal to 1 - the ratio between the hidden apparent area of the occulted
     * body and its total apparent area.
     * 
     * @param occulted the occulted body
     * @param occultedRadiusIn the occulted body radius (m)
     * @param occultingBodyIn the occulting body
     * @param lightingRatio the lighting ratio: 0 when total eclipse events should be detected, 1
     *        for penumbra events.
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param entry action performed when entering the eclipse
     * @param exit action performed when exiting the eclipse
     * @param removeEntry when the spacecraft point enters the zone.
     * @param removeExit when the spacecraft point leaves the zone.
     * @param slopeSelection slope selection
     * 
     * @throws ArithmeticException if occultingBodyRadius is NaN.
     * @since 4.5
     */

    public EclipseDetector(final PVCoordinatesProvider occulted, final double occultedRadiusIn,
                           final GeometricBodyShape occultingBodyIn, final double lightingRatio,
                           final double maxCheck, final double threshold,
                           final Action entry, final Action exit,
                           final boolean removeEntry, final boolean removeExit, final int slopeSelection)
        throws ArithmeticException {

        super(slopeSelection, maxCheck, threshold, entry, exit, removeEntry, removeExit);
        // occulted body PV coordinates provider is defined
        this.isOccultedPVProvDefined = true;
        this.occultedDirection = null;
        this.occultedBody = occulted;
        this.occultedRadius = MathLib.abs(occultedRadiusIn);
        this.occultingBody = occultingBodyIn;
        this.occultingRadiusProvider = new VariableRadiusProvider(occultingBodyIn);

        if (lightingRatio >= 1 - EPSILON) {
            // in this case, it is a penumbra eclipse detector:
            this.totalEclipse = false;
            this.lightingRatioDetection = false;
            this.ratio = 1.0;

        } else if (lightingRatio <= EPSILON) {
            // in this case, it is an umbra detector:
            this.totalEclipse = true;
            this.lightingRatioDetection = false;
            this.ratio = 0.0;

        } else {
            // partial detection:
            this.totalEclipse = false;
            this.lightingRatioDetection = true;
            this.ratio = lightingRatio;
        }
    }

    /**
     * Constructor with slope selection.
     * 
     * @param occulted the occulted body
     * @param occultedRadiusIn the occulted body radius (m)
     * @param occulting the occulting body
     * @param occultingRadius the occulting body radius (m)
     * @param lightingRatio the lighting ratio: 0 when total eclipse events should be detected, 1
     *        for penumbra events.
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param slopeSelection slope selection
     * @param action action performed when entering/exiting the eclipse depending on slope selection
     * @param remove when the spacecraft point enters or exit the zone depending on slope selection
     * @throws ArithmeticException if occultingBodyRadius is NaN.
     */
    public EclipseDetector(final PVCoordinatesProvider occulted, final double occultedRadiusIn,
                           final PVCoordinatesProvider occulting, final double occultingRadius,
                           final double lightingRatio, final int slopeSelection, final double maxCheck,
                           final double threshold, final Action action, final boolean remove)
        throws ArithmeticException {
        super(slopeSelection, maxCheck, threshold, action, remove);
        // occulted body PV coordinates provider is defined
        this.isOccultedPVProvDefined = true;
        this.occultedDirection = null;
        this.occultedBody = occulted;
        this.occultedRadius = MathLib.abs(occultedRadiusIn);
        this.occultingBody = occulting;
        this.occultingRadiusProvider = new ConstantRadiusProvider(MathLib.abs(occultingRadius));

        if (lightingRatio >= 1 - EPSILON) {
            // in this case, it is a penumbra eclipse detector:
            this.totalEclipse = false;
            this.lightingRatioDetection = false;
            this.ratio = 1.0;

        } else if (lightingRatio <= EPSILON) {
            // in this case, it is an umbra detector:
            this.totalEclipse = true;
            this.lightingRatioDetection = false;
            this.ratio = 0.0;

        } else {
            // partial detection:
            this.totalEclipse = false;
            this.lightingRatioDetection = true;
            this.ratio = lightingRatio;
        }
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
     * Get the occulted body.
     * 
     * @return the occulted body
     */
    public PVCoordinatesProvider getOcculted() {
        return this.occultedBody;
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
     * Returns the occulted body direction.
     * 
     * @return the occulted direction
     */
    public IDirection getOccultedDirection() {
        return this.occultedDirection;
    }

    /**
     * Returns the occulting radius provider.
     * 
     * @return the occultingRadiusProvider
     */
    public LocalRadiusProvider getOccultingRadiusProvider() {
        return this.occultingRadiusProvider;
    }

    /**
     * Get the total eclipse detection flag.
     * 
     * @return the total eclipse detection flag (true for umbra events detection, false for penumbra
     *         events detection)
     */
    public boolean isTotalEclipse() {
        return this.totalEclipse;
    }

    /**
     * Get the eclipse flag.
     * 
     * @param s the current state information : date, kinematics, attitude
     * @return an eclipse flag indicating whether the object is in eclipse: true if it is in eclipse, false if it is not
     * @throws PatriusException if some specific error occurs while retrieving the value of the switching function g(s)
     */
    public boolean isInEclipse(final SpacecraftState s) throws PatriusException {
        return this.g(s) < 0;
    }

    /**
     * Handle an eclipse event and choose what to do next.
     * 
     * @param s the current state information : date, kinematics, attitude
     * @param increasing if true, the value of the switching function increases when times increases
     *        around event
     * @param forward if true, the integration variable (time) increases during integration.
     * @return the action performed when entering or exiting the eclipse.
     * @exception PatriusException if some specific error occurs
     */
    @Override
    public Action eventOccurred(final SpacecraftState s, final boolean increasing,
            final boolean forward) throws PatriusException {
        final Action result;
        if (this.getSlopeSelection() == EXIT) {
            result = this.getActionAtExit();
            // remove (or not) detector
            this.shouldBeRemovedFlag = this.isRemoveAtExit();
        } else if (this.getSlopeSelection() == ENTRY) {
            result = this.getActionAtEntry();
            // remove (or not) detector
            this.shouldBeRemovedFlag = this.isRemoveAtEntry();
        } else {
            if (forward ^ !increasing) {
                // increasing case
                result = this.getActionAtExit();
                // remove (or not) detector
                this.shouldBeRemovedFlag = this.isRemoveAtExit();
            } else {
                // decreasing case
                result = this.getActionAtEntry();
                // remove (or not) detector
                this.shouldBeRemovedFlag = this.isRemoveAtEntry();
            }
        }
        return result;
    }

    /**
     * Compute the value of the switching function. This function becomes negative when entering the
     * region of shadow and positive when exiting.
     * 
     * @param state state
     * @return value of the switching function
     * @exception PatriusException if some specific error occurs
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    //CHECKSTYLE: stop MethodLength check
    // Reason: Orekit code kept as such
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public double g(final SpacecraftState state) throws PatriusException {
        //CHECKSTYLE: resume MethodLength check
        // CHECKSTYLE: resume CyclomaticComplexity check

        // Satellite position:
        final Vector3D psat = state.getPVCoordinates().getPosition();
        // Computing the occulting body position:
        final AbsoluteDate occultingDate = getSignalEmissionDate(occultingBody, state.getOrbit(), state.getDate(), 
                getThreshold());
        final Vector3D ping = this.occultingBody.getPVCoordinates(occultingDate, state.getFrame()).getPosition();
        // Satellite - occulting body position:
        final Vector3D sing = ping.subtract(psat);

        // Computing the occulted body position:
        final Vector3D pted;
        // Satellite - occulted body position:
        final Vector3D sted;
        // Define the occulted date
        final AbsoluteDate occultedDate;
        if (this.isOccultedPVProvDefined) {
            // the occulted body PV coordinates provider is defined
            if (occultedBody == null) {
                occultedBody = new PVCoordinatesProvider() {
                    /** {@inheritDoc} */
                    @Override
                    public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame)
                        throws PatriusException {
                        return ((ITargetDirection) occultedDirection).getTargetPVCoordinates(date, frame);
                    }

                    /** {@inheritDoc} */
                    @Override
                    public Frame getNativeFrame(final AbsoluteDate date,
                            final Frame frame) throws PatriusException {
                        // Frame is unknown and unused
                        throw new PatriusException(PatriusMessages.INTERNAL_ERROR);
                    }
                };
            }
            occultedDate = getSignalEmissionDate(occultedBody, state.getOrbit(), state.getDate(), getThreshold());
            pted = this.occultedBody.getPVCoordinates(occultedDate, state.getFrame()).getPosition();
            sted = pted.subtract(psat);
        } else {
            // the occulted body PV coordinates provider is not defined
            // In this case, the direction is instantaneous (no propagation delay)
            occultedDate = state.getDate();
            sted = this.occultedDirection.getVector(state.getOrbit(), occultedDate, state.getFrame());
        }
        // Angular distance between occulted and occulting body centers:
        final double gamma = Vector3D.angle(sted, sing);
        final double gamma2 = gamma * gamma;

        // Occulting / occulted true and apparent radii
        final double occultingRadius = this.occultingRadiusProvider.getLocalRadius(psat, state.getFrame(),
            occultedDate, this.occultedBody);

        // initialize g function and ring
        double g = 0.;
        double ring = 0.;
        // boolean to indicate if g function needs to be computed using ring
        boolean computeG = true;
        // boolean to indicate if ring function should be computed
        boolean computeRing = true;

        if (occultingRadius / sing.getNorm() > 1) {
            // the satellite is under occulting body surface

            // alpha occulted-occulting-staellite angle
            final double alpha = Vector3D.angle(sted.subtract(sing), sing.negate());
            // gamma is obtu : total eclipse
            if (alpha > FastMath.PI / 2) {
                // totale eclipse : g < 0
                g = -FastMath.PI / 2;
                computeG = false;

            } else {
                // gamma is less than PI/2
                ring = FastMath.PI / 2;
                computeRing = false;
            }
        }

        if (computeG) {
            if (computeRing) {
                final double value1 = occultingRadius / sing.getNorm();
                ring = MathLib.asin(MathLib.min(1.0, MathLib.max(-1.0, value1)));
            }
            final double value2 = this.occultedRadius / sted.getNorm();
            final double rted = MathLib.asin(MathLib.min(1.0, MathLib.max(-1.0, value2)));
            final double ring2 = ring * ring;
            final double rted2 = rted * rted;

            // Total/partial eclipse g function:
            g = this.totalEclipse ? (gamma - ring + rted) : (gamma - ring - rted);
            if (this.lightingRatioDetection) {
                // Eclipse computation when a lighting ratio is provided:
                double eps = 1;
                if (gamma < (rted + ring)) {
                    // The eclipse is happening:
                    final double x;
                    final double y;
                    final double alpha;
                    final double beta;
                    if (gamma < ring) {
                        // if Ring>Red, the center of the occulted body is hidden by the occulting
                        // body
                        // if Ring<Red, the center of the occulting body covers the occulted body:
                        if (gamma <= MathLib.abs(ring - rted)) {
                            // if Ring>Red, the occulted body is completely hidden by the occulting
                            // body
                            // if Ring<Red, the occulting body projection is "inside" the occulted
                            // body:
                            final double areaRatio = (rted2 - ring2) / rted2;
                            eps = (MathLib.abs(areaRatio)) * (MathLib.signum(areaRatio) + 1) / 2.;
                        } else {
                            x = (ring2 - rted2 - gamma2) / (2 * gamma);
                            y = MathLib.sqrt(MathLib.max(0.0, rted2 - x * x));

                            // angles
                            alpha = FastMath.PI - MathLib.atan2(y, x);
                            beta = MathLib.atan2(y, gamma + x);

                            // occulted area
                            final double aOcc = rted2 * alpha + ring2 * beta - gamma * y;

                            // lighting ratio
                            eps = 1 - aOcc / (FastMath.PI * rted2);
                        }
                    } else {
                        // if Ring>Red, the center of the occulted body is not hidden by the
                        // occulting body
                        // if Ring<Red, the center of the occulting body do not cover the occulted
                        // body:
                        x = (rted2 - ring2 + gamma2) / (2 * gamma);
                        y = MathLib.sqrt(MathLib.max(0.0, rted2 - x * x));

                        // angles
                        alpha = MathLib.atan2(y, x);
                        beta = MathLib.atan2(y, gamma - x);

                        // occulted area
                        final double aOcc = rted2 * alpha + ring2 * beta - gamma * y;

                        // lighting ratio
                        eps = 1 - aOcc / (FastMath.PI * rted2);
                    }
                }
                // g value:
                g = eps - this.ratio;
            }
        }
        if (this.isOccultedPVProvDefined && sted.getNorm() < sing.getNorm()) {
            // when the occulted body - satellite distance is smaller than the occulting body -
            // satellite
            // distance, eclipse should never happen: g function is set equal to PI / 2 to prevent
            // any event triggering.
            g = FastMath.PI / 2;
        }

        return g;
    }
    
    /** @inheritDoc */
    @Override
    public void setPropagationDelayType(final PropagationDelayType propagationDelayType, final Frame frame) {
        super.setPropagationDelayType(propagationDelayType, frame);
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * The following attributes are not deeply copied:
     * <ul>
     * <li>occultingBody: {@link PVCoordinatesProvider}</li>
     * <li>occultedBody: {@link PVCoordinatesProvider}</li>
     * <li>occultingRadiusProvider: {@link LocalRadiusProvider}</li>
     * </ul>
     * </p>
     */
    @Override
    public EventDetector copy() {
        final EclipseDetector result;
        // Check if occulted direction is null
        if (this.occultedDirection == null) {
            // Occulted direction is null
            // Check if occulting radius provider is an instance of ConstantRadiusProvider
            if (this.occultingRadiusProvider instanceof ConstantRadiusProvider) {
                // Occulting radius provider is an instance of ConstantRadiusProvider
                result = new EclipseDetector(this.occultedBody, this.occultedRadius, this.occultingBody,
                    ((ConstantRadiusProvider) this.occultingRadiusProvider).getLocalRadius(null, null,
                        null, null), this.ratio, this.getMaxCheckInterval(),
                    this.getThreshold(),
                    this.getActionAtEntry(), this.getActionAtExit(), isRemoveAtEntry(), isRemoveAtExit());
            } else {
                // Occulting radius provider is not an instance of ConstantRadiusProvider (but of
                // VariableRadiusProvider)
                result = new EclipseDetector(this.occultedBody, this.occultedRadius,
                    ((VariableRadiusProvider) this.occultingRadiusProvider).getBodyShape(), this.ratio,
                    this.getMaxCheckInterval(), this.getThreshold(), this.getActionAtEntry(), this.getActionAtExit(),
                    isRemoveAtEntry(), isRemoveAtExit());
            }
        } else {
            // Occulted direction is not null
            // Check if occulting body is an instance of GeometricBodyShape
            if (this.occultingBody instanceof GeometricBodyShape) {
                // Occulting body is an instance of GeometricBodyShape
                result = new EclipseDetector(this.occultedDirection, (GeometricBodyShape) this.occultingBody,
                    this.ratio, this.getMaxCheckInterval(), this.getThreshold(), this.getActionAtEntry(),
                    this.getActionAtExit(),
                    isRemoveAtEntry(), isRemoveAtExit());
            } else {
                // Occulting body is not an instance of GeometricBodyShape (but of PVCoordinatesProvider)
                result = new EclipseDetector(this.occultedDirection, this.occultingBody,
                    this.ratio, this.getMaxCheckInterval(), this.getThreshold(), this.getActionAtEntry(),
                    this.getActionAtExit(),
                    isRemoveAtEntry(), isRemoveAtExit());
            }
        }
        
        result.setPropagationDelayType(getPropagationDelayType(), getInertialFrame());
        return result;
    }
}
