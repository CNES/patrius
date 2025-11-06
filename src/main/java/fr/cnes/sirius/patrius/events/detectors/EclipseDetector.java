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
 * VERSION:4.16:OPENFD-468:25/04/2025:[PATRIUS] Renommer toutes les mentions du GeodeticPoint
 * VERSION:4.16:OPENFD-442:25/04/2025:[PATRIUS] Calcul des eclipses d'un corps celeste
 * VERSION:4.15:OPENFD-341:21/11/2024:[PATRIUS] Dysfonctionnement de EclipseDetector pour
 * le calcul de visibilités d'une cible au sol
 * VERSION:4.14:OPENFD-:22/08/2024:
 * VERSION:4.14:OPENFD-141:22/08/2024: Isolation des algorithmes de somme et produit precis
 * VERSION:4.14:OPENFD-178:22/08/2024: [PATRIUS] Renommage de l'enumere DatationChoice
 * VERSION:4.14:OPENFD-222:22/08/2024: Assurer la compatibilite ascendante
 * VERSION:4.14:OPENFD-283:22/08/2024: Methode filterEvent() non-wrappe dans OneSatEventDetectorWrapper
 * VERSION:4.14:OPENFD-319:22/08/2024: Assurer la compatibilite ascendante de la v4.13
 * VERSION:4.14:OPENFD-253:22/08/2024: [PATRIUS] Problemes e l'utilisation des bsp planetaires
 * VERSION:4.14:OPENFD-343:22/08/2024: Ajout de regles de codage dans le standard de codage DYNVOL
 * VERSION:4.14:OPENFD-292:22/08/2024: Implementation de multi-propagateurs mixtes
 * VERSION:4.14:OPENFD-179:22/08/2024: [PATRIUS] Gestion emetteur/recepteur dans les detecteurs d'evenements
 * VERSION:4.13.5:DM:DM-319:03/07/2024:[PATRIUS] Assurer la compatibilite ascendante de la v4.13
 * VERSION:4.13.2:DM:DM-222:08/03/2024:[PATRIUS] Assurer la compatibilité ascendante
 * VERSION:4.13.1:FA:FA-128:17/01/2024:[PATRIUS] Constructeur de EclipseDetector inutilisable
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.13:FA:FA-128:08/12/2023:[PATRIUS] Constructeur de EclipseDetector inutilisable
 * VERSION:4.13:DM:DM-132:08/12/2023:[PATRIUS] Suppression de la possibilite
 * de convertir les sorties de VacuumSignalPropagation
 * VERSION:4.13:DM:DM-101:08/12/2023:[PATRIUS] Harmonisation des eclipses pour les evenements et pour la PRS
 * VERSION:4.13:DM:DM-37:08/12/2023:[PATRIUS] Date d'evenement et propagation du signal
 * VERSION:4.11:DM:DM-3303:22/05/2023:[PATRIUS] Modifications mineures dans UserCelestialBody 
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:FA:FA-3222:03/11/2022:[PATRIUS] Incoherence getLocalRadius GeometricBodyShape g EclipseDetector
 * VERSION:4.10:DM:DM-3194:03/11/2022:[PATRIUS] Fusion des interfaces GeometricBodyShape et BodyShape 
 * VERSION:4.10:DM:DM-3245:03/11/2022:[PATRIUS] Ajout du sens de propagation du signal dans ...
 * VERSION:4.9.1:FA:FA-3193:01/06/2022:[PATRIUS] Revenir a la signature initiale de la methode getLocalRadius
 * VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3165:10/05/2022:[PATRIUS] Amelioration de la propagation du signal 
 * VERSION:4.9:DM:DM-3181:10/05/2022:[PATRIUS] Passage a protected de la methode setPropagationDelayType
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:DM:DM-3133:10/05/2022:[PATRIUS] Ajout de plusieurs fonctionnalites a la classe EclipseDetector 
 * VERSION:4.9:FA:FA-3170:10/05/2022:[PATRIUS] Incoherence de datation entre la methode getLocalRadius...
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2245:27/05/2020:Ameliorations de EclipseDetector 
 * VERSION:4.5:DM:DM-2460:27/05/2020:Prise en compte des temps de propagation dans les calculs evenements
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014:Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
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
package fr.cnes.sirius.patrius.events.detectors;

import fr.cnes.sirius.patrius.attitudes.directions.IDirection;
import fr.cnes.sirius.patrius.attitudes.directions.ITargetDirection;
import fr.cnes.sirius.patrius.bodies.ApparentRadiusProvider;
import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.ConstantRadiusProvider;
import fr.cnes.sirius.patrius.bodies.VariableRadiusProvider;
import fr.cnes.sirius.patrius.events.EventDetector;
import fr.cnes.sirius.patrius.forces.radiation.LightingRatio;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.ConstantPVCoordinatesProvider;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.PatriusConfiguration;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Finder for satellite eclipse related events.
 * <p>
 * This class finds eclipse events, i.e. satellite within umbra (total eclipse) or penumbra (partial
 * eclipse).
 * </p>
 * <p>
 * The default implementation behavior is to {@link EventDetector.Action#CONTINUE} continue
 * propagation when entering
 * the eclipse and to {@link EventDetector.Action#STOP} stop propagation when exiting the eclipse.
 * This can be changed
 * by using some constructors.
 * <p>
 * This detector can takes into account signal propagation duration through
 * {@link #setPropagationDelayType(PropagationDelayType, Frame)} (default is signal being
 * instantaneous).<br>
 * It can be taken only if the occulted body is defined through a {@link PVCoordinatesProvider} and
 * not an
 * {@link IDirection}.
 * </p>
 *
 * <p>
 * <b>WARNING</b> : Do not use this detector to detect ground target visibilities or occulted bodies
 * with null radius.
 * Actually, the lighting ratio used to compute g function is based on geometric angle comparison
 * and considers that
 * if the vehicle is closer to the occulted body than to the occulting body then the occulted body
 * is not occulted.
 * This is not true if the occulted body is a target on the ground of the occulting body and In this
 * case,
 * it will detect a period out of eclipse longer than it should be.
 * </p>
 *
 * @see fr.cnes.sirius.patrius.propagation.Propagator#addEventDetector(EventDetector)
 * @author Pascal Parraud
 */
@SuppressWarnings("PMD.NullAssignment")
public class EclipseDetector extends AbstractEclipseDetector {

    /** Serializable UID. */
    private static final long serialVersionUID = -541311550206363031L;

    /** Far distance (10 billions km) for fictitious body build. */
    private static final double FAR_DISTANCE = 1E13;

    /** Occulted direction. */
    private final IDirection occultedDirection;

    /** True if the occulted {@link PVCoordinatesProvider} is defined, false if it is not. */
    private final Boolean isOccultedPVProvDefined;

    /** True if the eclipse detection is based on a lighting ratio value. */
    private final Boolean lightingRatioDetection;

    /** The lighting ratio value (0 < ratio < 1, 0 for total eclipses, 1 for penumbra events). */
    private final double ratio;

    /** Umbra, if true, or penumbra, if false, detection flag. */
    private final Boolean totalEclipse;

    /**
     * Build a new eclipse detector.
     * <p>
     * The occulting body is a sphere and the occulted body is a direction; the concept of
     * umbra/penumbra does not apply
     * to this detector.
     * </p>
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#CONTINUE} continue
     * propagation when
     * entering the eclipse and to {@link EventDetector.Action#STOP} stop propagation when exiting
     * the eclipse.
     * </p>
     *
     * @param occulted
     *        the direction to be occulted
     * @param occulting
     *        the occulting body
     * @param occultingRadius
     *        the occulting body radius (m)
     * @param maxCheck
     *        maximal checking interval (s)
     * @param threshold
     *        convergence threshold (s)
     */
    public EclipseDetector(final IDirection occulted, final PVCoordinatesProvider occulting,
                           final double occultingRadius, final double maxCheck, final double threshold) {
        this(occulted, occulting, occultingRadius, maxCheck, threshold, Action.CONTINUE,
                Action.STOP);
    }

    /**
     * Build a new eclipse detector with defined actions when entering and exiting the eclipse.
     * <p>
     * The occulting body is a sphere and the occulted body is a direction; the concept of
     * umbra/penumbra does not apply
     * to this detector.
     * </p>
     *
     * @param occulted
     *        the direction to be occulted
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
     */
    public EclipseDetector(final IDirection occulted, final PVCoordinatesProvider occulting,
                           final double occultingRadius, final double maxCheck, final double threshold,
                           final Action entry, final Action exit) {
        this(occulted, occulting, occultingRadius, maxCheck, threshold, entry, exit, false, false);
    }

    /**
     * Build a new eclipse detector with defined actions when entering and exiting the eclipse.
     * <p>
     * The occulting body is a sphere and the occulted body is a direction; the concept of
     * umbra/penumbra does not apply
     * to this detector.
     * </p>
     *
     * @param occulted
     *        the direction to be occulted
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
    public EclipseDetector(final IDirection occulted, final PVCoordinatesProvider occulting,
                           final double occultingRadius, final double maxCheck, final double threshold,
                           final Action entry, final Action exit, final boolean removeEntry,
                           final boolean removeExit) {
        super(occulting, occultingRadius, maxCheck, threshold, entry, exit, removeEntry,
                removeExit);

        this.lightingRatioDetection = null;
        this.ratio = 0.;

        switch (PatriusConfiguration.getPatriusCompatibilityMode()) {
            case OLD_MODELS:
            case MIXED_MODELS:
                // Occulted body initialisation
                this.totalEclipse = Boolean.TRUE;
                this.occultedDirection = occulted;
                // Check if the occulted direction is an instance of ITargetDirection
                if (this.occultedDirection instanceof ITargetDirection) {
                    // The occulted body PV coordinates provider is defined
                    this.isOccultedPVProvDefined = Boolean.TRUE;
                } else {
                    // The occulted body PV coordinates provider is not defined
                    this.isOccultedPVProvDefined = Boolean.FALSE;
                }
                break;
            case NEW_MODELS:

                this.totalEclipse = null;
                this.isOccultedPVProvDefined = null;

                // Occulted body initialisation
                this.occultedDirection = occulted;
                // Check if the occulted direction is an instance of ITargetDirection
                if (this.occultedDirection instanceof ITargetDirection) {
                    // The occulted body PV coordinates provider is defined

                    // This should not be done like this... but only solution right now
                    setOcculted(
                        ((ITargetDirection) this.occultedDirection).getTargetPvProvider());
                }
                break;

            default:
                throw new IllegalArgumentException(UNSUPPORTED_MODE_EXCEPTION
                        + PatriusConfiguration.getPatriusCompatibilityMode());
        }
    }

    /**
     * Build a new eclipse detector with defined actions when entering and exiting the eclipse.
     * <p>
     * The occulting body is a sphere and the occulted body is a direction; the concept of
     * umbra/penumbra does apply to
     * this detector by means of the lighting ratio.
     * </p>
     *
     * @param occulted
     *        the direction to be occulted
     * @param occulting
     *        the occulting body
     * @param lightingRatio
     *        the lighting ratio: 0 when total eclipse events should be detected, 1
     *        for penumbra events
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
    public EclipseDetector(final IDirection occulted, final BodyShape occulting,
                           final double lightingRatio, final double maxCheck, final double threshold,
                           final Action entry, final Action exit, final boolean removeEntry,
                           final boolean removeExit) {
        super(occulting, maxCheck, threshold, entry, exit, removeEntry, removeExit);

        switch (PatriusConfiguration.getPatriusCompatibilityMode()) {
            case OLD_MODELS:
            case MIXED_MODELS:
                // Occulted body initialisation
                this.occultedDirection = occulted;
                // Check if the occulted direction is an instance of ITargetDirection
                if (this.occultedDirection instanceof ITargetDirection) {
                    // The occulted body PV coordinates provider is defined
                    this.isOccultedPVProvDefined = Boolean.TRUE;
                } else {
                    // The occulted body PV coordinates provider is not defined
                    this.isOccultedPVProvDefined = Boolean.FALSE;
                }

                // Use the lightning ratio to determine the nature of the detector
                if (lightingRatio >= 1 - EPSILON_LIGHTING_RATIO) {
                    // in this case, it is a penumbra eclipse detector:
                    this.lightingRatioDetection = Boolean.FALSE;
                    this.totalEclipse = Boolean.FALSE;
                    this.ratio = 1.0;
                } else if (lightingRatio <= EPSILON_LIGHTING_RATIO) {
                    // in this case, it is an umbra detector:
                    this.lightingRatioDetection = Boolean.FALSE;
                    this.totalEclipse = Boolean.TRUE;
                    this.ratio = 0.0;
                } else {
                    // partial detection:
                    this.lightingRatioDetection = Boolean.TRUE;
                    this.totalEclipse = Boolean.FALSE;
                    this.ratio = lightingRatio;
                }

                break;
            case NEW_MODELS:

                this.totalEclipse = null;
                this.isOccultedPVProvDefined = null;
                this.lightingRatioDetection = null;

                // Occulted body initialisation
                this.occultedDirection = occulted;
                // Check if the occulted direction is an instance of ITargetDirection
                if (this.occultedDirection instanceof ITargetDirection) {
                    // The occulted body PV coordinates provider is defined
                    // This should not be done like this... but only solution right now
                    setOcculted(
                        ((ITargetDirection) this.occultedDirection).getTargetPvProvider());
                }
                this.ratio = lightingRatio;
                break;
            default:
                throw new IllegalArgumentException(UNSUPPORTED_MODE_EXCEPTION
                        + PatriusConfiguration.getPatriusCompatibilityMode());
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
     * The default implementation behavior is to {@link EventDetector.Action#CONTINUE} continue
     * propagation when
     * entering the eclipse and to {@link EventDetector.Action#STOP} stop propagation when exiting
     * the eclipse.
     * </p>
     *
     * @param occulted
     *        the occulted body
     * @param occultedRadiusIn
     *        the occulted body radius (m)
     * @param occulting
     *        the occulting body
     * @param occultingRadius
     *        the occulting body radius (m)
     * @param lightingRatio
     *        the lighting ratio: 0 when total eclipse events should be detected, 1
     *        for penumbra events.
     * @param maxCheck
     *        maximal checking interval (s)
     * @param threshold
     *        convergence threshold (s)
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
     * @param lightingRatio
     *        the lighting ratio: 0 when total eclipse events should be detected, 1
     *        for penumbra events.
     * @param maxCheck
     *        maximal checking interval (s)
     * @param threshold
     *        convergence threshold (s)
     * @param entry
     *        action performed when entering the eclipse
     * @param exit
     *        action performed when exiting the eclipse
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
     * @param lightingRatio
     *        the lighting ratio: 0 when total eclipse events should be detected, 1
     *        for penumbra events.
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
    public EclipseDetector(final PVCoordinatesProvider occulted, final double occultedRadiusIn,
                           final PVCoordinatesProvider occulting, final double occultingRadius,
                           final double lightingRatio, final double maxCheck, final double threshold,
                           final Action entry, final Action exit, final boolean removeEntry,
                           final boolean removeExit) {

        super(occulted, occultedRadiusIn, occulting, occultingRadius, EventDetector.INCREASING_DECREASING, maxCheck,
                threshold, entry, exit, removeEntry, removeExit);

        switch (PatriusConfiguration.getPatriusCompatibilityMode()) {
            case OLD_MODELS:
            case MIXED_MODELS:
                // occulted body PV coordinates provider is defined
                this.isOccultedPVProvDefined = Boolean.TRUE;
                this.occultedDirection = null;

                if (lightingRatio >= 1 - EPSILON_LIGHTING_RATIO) {
                    // in this case, it is a penumbra eclipse detector:
                    this.lightingRatioDetection = Boolean.FALSE;
                    this.totalEclipse = Boolean.FALSE;
                    this.ratio = 1.0;

                } else if (lightingRatio <= EPSILON_LIGHTING_RATIO) {
                    // in this case, it is an umbra detector:
                    this.lightingRatioDetection = Boolean.FALSE;
                    this.totalEclipse = Boolean.TRUE;
                    this.ratio = 0.0;

                } else {
                    // partial detection:
                    this.lightingRatioDetection = Boolean.TRUE;
                    this.totalEclipse = Boolean.FALSE;
                    this.ratio = lightingRatio;
                }

                break;

            case NEW_MODELS:

                this.totalEclipse = null;
                this.isOccultedPVProvDefined = null;
                this.lightingRatioDetection = null;

                // occulted body PV coordinates provider is defined
                this.occultedDirection = null;
                this.ratio = lightingRatio;
                break;
            default:
                throw new IllegalArgumentException(UNSUPPORTED_MODE_EXCEPTION
                        + PatriusConfiguration.getPatriusCompatibilityMode());
        }
    }

    /**
     * Build a new eclipse detector based on a lighting ratio. The occulted body is a sphere and the
     * occulting body is a {@link BodyShape}.
     * <p>
     * The lighting ratio, whose value is between 0 and 1, establishes when an eclipse event should
     * be triggered. If 0, an event is detected only when the occulted body is completely hidden
     * (equivalent to an umbra detector), if 1, an event is detected every time the occulted body is
     * just partially hidden (equivalent to a penumbra detector). <br>
     * As a general rule, the lighting ratio is equal to 1 - the ratio between the hidden apparent
     * area of the occulted body and its total apparent area.
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#CONTINUE} continue
     * propagation when
     * entering the eclipse and to {@link EventDetector.Action#STOP} stop propagation when exiting
     * the eclipse.
     * </p>
     *
     * @param occulted
     *        the occulted body
     * @param occultedRadiusIn
     *        the occulted body radius (m)
     * @param occultingBodyIn
     *        the occulting body
     * @param lightingRatio
     *        the lighting ratio: 0 when total eclipse events should be detected, 1
     *        for penumbra events.
     * @param maxCheck
     *        maximal checking interval (s)
     * @param threshold
     *        convergence threshold (s)
     */
    public EclipseDetector(final PVCoordinatesProvider occulted, final double occultedRadiusIn,
                           final BodyShape occultingBodyIn, final double lightingRatio, final double maxCheck,
                           final double threshold) {
        this(occulted, occultedRadiusIn, occultingBodyIn, lightingRatio, maxCheck, threshold,
                Action.CONTINUE, Action.STOP);
    }

    /**
     * Build a new eclipse detector based on a lighting ratio. The occulted body is a sphere and the
     * occulting body is a {@link BodyShape}.
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
     * @param lightingRatio
     *        the lighting ratio: 0 when total eclipse events should be detected, 1
     *        for penumbra events.
     * @param maxCheck
     *        maximal checking interval (s)
     * @param threshold
     *        convergence threshold (s)
     * @param entry
     *        action performed when entering the eclipse
     * @param exit
     *        action performed when exiting the eclipse
     */
    public EclipseDetector(final PVCoordinatesProvider occulted, final double occultedRadiusIn,
                           final BodyShape occultingBodyIn, final double lightingRatio, final double maxCheck,
                           final double threshold, final Action entry, final Action exit) {
        this(occulted, occultedRadiusIn, occultingBodyIn, lightingRatio, maxCheck, threshold, entry,
                exit, false, false);
    }

    /**
     * Build a new eclipse detector based on a lighting ratio. The occulted body is a sphere and the
     * occulting body is a {@link BodyShape}.
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
     * @param lightingRatio
     *        the lighting ratio: 0 when total eclipse events should be detected, 1
     *        for penumbra events.
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
     * @since 3.1
     */
    public EclipseDetector(final PVCoordinatesProvider occulted, final double occultedRadiusIn,
                           final BodyShape occultingBodyIn, final double lightingRatio, final double maxCheck,
                           final double threshold, final Action entry, final Action exit,
                           final boolean removeEntry, final boolean removeExit) {
        this(occulted, occultedRadiusIn, occultingBodyIn, lightingRatio, maxCheck, threshold, entry,
                exit, removeEntry, removeExit, ENTRY_EXIT);
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
     * @param lightingRatio
     *        the lighting ratio: 0 when total eclipse events should be detected, 1
     *        for penumbra events.
     * @param maxCheck
     *        maximal checking interval (s)
     * @param threshold
     *        convergence threshold (s)
     * @param slopeSelection
     *        slope selection
     * @throws ArithmeticException
     *         if occultingBodyRadius is NaN.
     * @since 4.5
     */
    public EclipseDetector(final PVCoordinatesProvider occulted, final double occultedRadiusIn,
                           final BodyShape occultingBodyIn, final double lightingRatio, final double maxCheck,
                           final double threshold, final int slopeSelection) {
        this(occulted, occultedRadiusIn, occultingBodyIn, lightingRatio, maxCheck, threshold,
                Action.CONTINUE, Action.STOP, false, false, slopeSelection);
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
     * @param lightingRatio
     *        the lighting ratio: 0 when total eclipse events should be detected, 1
     *        for penumbra events.
     * @param maxCheck
     *        maximal checking interval (s)
     * @param threshold
     *        convergence threshold (s)
     * @param action
     *        action performed when entering/exiting the eclipse depending on slope selection
     * @param remove
     *        when the spacecraft point enters or exit the zone depending on slope selection
     * @param slopeSelection
     *        slope selection
     * @throws ArithmeticException
     *         if occultingBodyRadius is NaN.
     * @since 4.5
     */

    public EclipseDetector(final PVCoordinatesProvider occulted, final double occultedRadiusIn,
                           final BodyShape occultingBodyIn, final double lightingRatio, final double maxCheck,
                           final double threshold, final Action action, final boolean remove,
                           final int slopeSelection) {
        this(occulted, occultedRadiusIn, occultingBodyIn, lightingRatio, maxCheck, threshold,
                action, action, remove, remove, slopeSelection);
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
     * @param lightingRatio
     *        the lighting ratio: 0 when total eclipse events should be detected, 1
     *        for penumbra events.
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
    public EclipseDetector(final PVCoordinatesProvider occulted, final double occultedRadiusIn,
                           final BodyShape occultingBodyIn, final double lightingRatio, final double maxCheck,
                           final double threshold, final Action entry, final Action exit,
                           final boolean removeEntry, final boolean removeExit, final int slopeSelection) {

        super(occulted, occultedRadiusIn, occultingBodyIn, maxCheck, threshold,
                entry, exit, removeEntry, removeExit, slopeSelection);

        switch (PatriusConfiguration.getPatriusCompatibilityMode()) {
            case OLD_MODELS:
            case MIXED_MODELS:
                // occulted body PV coordinates provider is defined
                this.isOccultedPVProvDefined = Boolean.TRUE;
                this.occultedDirection = null;

                if (lightingRatio >= 1 - EPSILON_LIGHTING_RATIO) {
                    // in this case, it is a penumbra eclipse detector:
                    this.lightingRatioDetection = Boolean.FALSE;
                    this.totalEclipse = Boolean.FALSE;
                    this.ratio = 1.0;

                } else if (lightingRatio <= EPSILON_LIGHTING_RATIO) {
                    // in this case, it is an umbra detector:
                    this.lightingRatioDetection = Boolean.FALSE;
                    this.totalEclipse = Boolean.TRUE;
                    this.ratio = 0.0;

                } else {
                    // partial detection:
                    this.lightingRatioDetection = Boolean.TRUE;
                    this.totalEclipse = Boolean.FALSE;
                    this.ratio = lightingRatio;
                }

                break;

            case NEW_MODELS:

                this.totalEclipse = null;
                this.isOccultedPVProvDefined = null;
                this.lightingRatioDetection = null;

                // occulted body PV coordinates provider is defined
                this.occultedDirection = null;
                this.ratio = lightingRatio;
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
     * @param lightingRatio
     *        the lighting ratio: 0 when total eclipse events should be detected, 1
     *        for penumbra events.
     * @param maxCheck
     *        maximal checking interval (s)
     * @param threshold
     *        convergence threshold (s)
     * @param slopeSelection
     *        slope selection
     * @param action
     *        action performed when entering/exiting the eclipse depending on slope selection
     * @param remove
     *        when the spacecraft point enters or exit the zone depending on slope selection
     */
    public EclipseDetector(final PVCoordinatesProvider occulted, final double occultedRadiusIn,
                           final PVCoordinatesProvider occulting, final double occultingRadius,
                           final double lightingRatio, final int slopeSelection, final double maxCheck,
                           final double threshold, final Action action, final boolean remove) {
        super(occulted, occultedRadiusIn, occulting, occultingRadius, slopeSelection,
                maxCheck, threshold, action, remove);

        switch (PatriusConfiguration.getPatriusCompatibilityMode()) {

            case OLD_MODELS:
            case MIXED_MODELS:
                // occulted body PV coordinates provider is defined
                this.isOccultedPVProvDefined = Boolean.TRUE;
                this.occultedDirection = null;
                new ConstantRadiusProvider(MathLib.abs(occultingRadius));

                if (lightingRatio >= 1 - EPSILON_LIGHTING_RATIO) {
                    // in this case, it is a penumbra eclipse detector:
                    this.lightingRatioDetection = Boolean.FALSE;
                    this.totalEclipse = Boolean.FALSE;
                    this.ratio = 1.0;
                } else if (lightingRatio <= EPSILON_LIGHTING_RATIO) {
                    // in this case, it is an umbra detector:
                    this.lightingRatioDetection = Boolean.FALSE;
                    this.totalEclipse = Boolean.TRUE;
                    this.ratio = 0.0;
                } else {
                    // partial detection:
                    this.lightingRatioDetection = Boolean.TRUE;
                    this.totalEclipse = Boolean.FALSE;
                    this.ratio = lightingRatio;
                }

                break;

            case NEW_MODELS:

                this.totalEclipse = null;
                this.isOccultedPVProvDefined = null;
                this.lightingRatioDetection = null;

                // occulted body PV coordinates provider is defined
                this.occultedDirection = null;
                new ConstantRadiusProvider(MathLib.abs(occultingRadius));
                this.ratio = lightingRatio;
                break;
            default:
                throw new IllegalArgumentException(UNSUPPORTED_MODE_EXCEPTION
                        + PatriusConfiguration.getPatriusCompatibilityMode());
        }

    }

    /**
     * Returns the occulted body direction.
     *
     * @return the occulted direction
     */
    public IDirection getOccultedDirection() {
        return this.occultedDirection;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isTotalEclipse() {
        final boolean isTotEclipse;
        switch (PatriusConfiguration.getPatriusCompatibilityMode()) {
            case OLD_MODELS:
            case MIXED_MODELS:
                isTotEclipse = this.totalEclipse;
                break;
            case NEW_MODELS:
                isTotEclipse = MathLib.abs(this.ratio - 0.) < Precision.DOUBLE_COMPARISON_EPSILON;
                break;
            default:
                throw new IllegalArgumentException(
                    UNSUPPORTED_MODE_EXCEPTION + PatriusConfiguration.getPatriusCompatibilityMode());
        }
        return isTotEclipse;
    }

    /**
     * Compute the value of the switching function. This function becomes negative when entering the
     * region of shadow and positive when exiting.
     *
     * @param state
     *        state
     * @return value of the switching function
     * @exception PatriusException
     *            if some specific error occurs
     */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public double g(final SpacecraftState state) throws PatriusException {

        // initialize g function
        double g = 0.;
        switch (PatriusConfiguration.getPatriusCompatibilityMode()) {
            case OLD_MODELS:
            case MIXED_MODELS:
                // CHECKSTYLE: resume MethodLength check
                // CHECKSTYLE: resume CyclomaticComplexity check

                // Geometric computation frame: in case of light speed computation, it must be
                // frozen wrt to ICRF frame
                Frame referenceFrame = state.getFrame();
                // Case of light speed propagation (dedicated in order to optimize computation
                // times)
                if (getPropagationDelayType() == PropagationDelayType.LIGHT_SPEED) {
                    referenceFrame = state.getFrame().getFrozenFrame(FramesFactory.getICRF(),
                        state.getDate(), state.getFrame() + "-Frozen");
                }

                // Satellite position:
                final Vector3D psat = state.getPVCoordinates(referenceFrame).getPosition();
                // Computing the occulting body position:
                final AbsoluteDate occultingDate = this.getSignalEmissionDate(getOcculting(),
                    state.getOrbit(), state.getDate());
                final Vector3D ping = getOcculting()
                    .getPVCoordinates(occultingDate, referenceFrame).getPosition();
                // Satellite - occulting body position:
                final Vector3D sing = ping.subtract(psat);

                // Satellite - occulted body position:
                final Vector3D sted =
                    computeSatOccultBodyPosition(state, referenceFrame, psat);
                // Angular distance between occulted and occulting body centers:
                final double gamma = Vector3D.angle(sted, sing);

                // Occulting / occulted true and apparent radii
                final double occultingRadius = getOccultingRadiusProvider().getApparentRadius(
                    state.getOrbit(), state.getDate(), getOcculted(),
                    getPropagationDelayType());

                // initialize ring
                double ring = 0.;
                // boolean to indicate if g function needs to be computed using ring
                boolean computeG = true;
                // boolean to indicate if ring function should be computed
                boolean computeRing = true;

                final int unitValue = 1;
                if (occultingRadius / sing.getNorm() > unitValue) {
                    // the satellite is under occulting body surface

                    // alpha occulted-occulting-satellite angle
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
                    // compute g function using ring
                    g = gComputeUsingRing(sted, gamma, ring);
                }
                if (this.isOccultedPVProvDefined && sted.getNorm() < sing.getNorm()) {
                    // when the occulted body - satellite distance is smaller than the occulting
                    // body -
                    // satellite
                    // distance, eclipse should never happen: g function is set equal to PI / 2 to
                    // prevent
                    // any event triggering.
                    g = FastMath.PI / 2;
                }
                break;

            case NEW_MODELS:
                // Define occulted body in case a direction has been provided (on the fly since it
                // depends on current
                // state)
                PVCoordinatesProvider occultedBody2 = getOcculted();
                if (getOcculted() == null) {
                    occultedBody2 = getOccultedBodyFromDirection(state);
                }

                // Compute lighting ratio
                final LightingRatio lightingRatioComputer = buildLightingRatioComputer(occultedBody2);
                final double lightingRatio =
                    lightingRatioComputer.computeExtended(state.getOrbit(), state.getDate());

                // Compute g = current LR - target LR
                g = lightingRatio - this.ratio;
                break;
            default:
                throw new IllegalArgumentException(UNSUPPORTED_MODE_EXCEPTION
                        + PatriusConfiguration.getPatriusCompatibilityMode());
        }
        return g;
    }

    /**
     * Computes the g function using ring
     *
     * @param sted
     *        satellite - occulted body position
     * @param gamma
     *        angular distance between occulted and occulting body centers
     * @param ring
     *        the ring to be used for the computation
     * @return the g function computation using ring
     */
    private double gComputeUsingRing(final Vector3D sted, final double gamma, final double ring) {
        double g;
        final double value2 = getOccultedRadius() / sted.getNorm();
        final double rted = MathLib.asin(MathLib.min(1.0, MathLib.max(-1.0, value2)));
        final double ring2 = ring * ring;
        final double rted2 = rted * rted;
        final double squaredGamma = gamma * gamma;

        // Total/partial eclipse g function:
        if (isTotalEclipse()) {
            g = (gamma - ring + rted);
        } else {
            g = (gamma - ring - rted);
        }
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
                        eps = MathLib.abs(areaRatio) * (MathLib.signum(areaRatio) + 1) / 2.;
                    } else {
                        x = (ring2 - rted2 - squaredGamma) / (2 * gamma);
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
                    x = (rted2 - ring2 + squaredGamma) / (2 * gamma);
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
        return g;
    }

    /**
     * Computes the satellite-occulted body position vector
     *
     * @param state
     *        the spacecraft state
     * @param referenceFrame
     *        the reference frame of the S/C state
     * @param psat
     *        the satellite position
     * @return the satellite-occulted body position vector
     * @throws PatriusException
     *         if problems when retrieving the signal emission date, the occulted
     *         body PV coordinates
     *         or the occulted direction vector
     */
    private Vector3D computeSatOccultBodyPosition(final SpacecraftState state,
                                                  final Frame referenceFrame, final Vector3D psat)
        throws PatriusException {
        // Computing the occulted body position:
        final Vector3D pted;
        // Satellite - occulted body position:
        final Vector3D sted;
        // Define the occulted date
        final AbsoluteDate occultedDate;
        if (this.isOccultedPVProvDefined) {
            // the occulted body PV coordinates provider is defined
            if (getOcculted() == null) {
                final PVCoordinatesProvider newOccultedBody = new PVCoordinatesProvider(){
                    /** Serializable UID. */
                    private static final long serialVersionUID = 2321605280215656365L;

                    /** {@inheritDoc} */
                    @Override
                    public PVCoordinates getPVCoordinates(final AbsoluteDate date,
                                                          final Frame frame)
                        throws PatriusException {
                        return ((ITargetDirection) EclipseDetector.this.occultedDirection)
                            .getTargetPVCoordinates(date, frame);
                    }

                    /** {@inheritDoc} */
                    @Override
                    public Frame getNativeFrame(final AbsoluteDate date) throws PatriusException {
                        // Frame is unknown and unused
                        throw new PatriusException(PatriusMessages.INTERNAL_ERROR);
                    }
                };
                // Set the occulted body
                setOcculted(newOccultedBody);
            }
            occultedDate = this.getSignalEmissionDate(getOcculted(), state.getOrbit(),
                state.getDate());
            pted = getOcculted().getPVCoordinates(occultedDate, referenceFrame).getPosition();
            sted = pted.subtract(psat);
        } else {
            // the occulted body PV coordinates provider is not defined
            // In this case, the direction is instantaneous (no propagation delay)
            occultedDate = state.getDate();
            sted = this.occultedDirection.getVector(state.getOrbit(), occultedDate, referenceFrame);

            // Define fictitious occulted body in direction of occulted direction
            final PVCoordinatesProvider newOccultedBody = new PVCoordinatesProvider(){
                /** Serializable UID. */
                private static final long serialVersionUID = 2321605280215656365L;

                /** {@inheritDoc} */
                @Override
                public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame)
                    throws PatriusException {
                    final PVCoordinates pvSat = state.getPVCoordinates(frame);
                    // Occulted body is set far enough (10 billion km) in direction of occulted
                    // direction
                    final Vector3D dir =
                        EclipseDetector.this.occultedDirection
                            .getVector(
                                new ConstantPVCoordinatesProvider(
                                    state.getPVCoordinates(), state.getFrame()),
                                date, frame)
                            .scalarMultiply(FAR_DISTANCE);
                    // Velocity is no computable/unused
                    return new PVCoordinates(pvSat.getPosition().add(dir), Vector3D.ZERO);
                }

                /** {@inheritDoc} */
                @Override
                public Frame getNativeFrame(final AbsoluteDate date) throws PatriusException {
                    // Frame is unknown and unused
                    throw new PatriusException(PatriusMessages.INTERNAL_ERROR);
                }
            };
            // Set the occulted body
            setOcculted(newOccultedBody);
        }
        return sted;
    }

    /**
     * Get fictitious occulted body from generic {@link IDirection}.
     *
     * @param state
     *        spacecraft state
     * @return fictitious occulted body from generic {@link IDirection}
     */
    private PVCoordinatesProvider getOccultedBodyFromDirection(final SpacecraftState state) {
        return new PVCoordinatesProvider(){
            /** Serializable UID. */
            private static final long serialVersionUID = 2321605280215656365L;

            /** {@inheritDoc} */
            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame)
                throws PatriusException {
                final PVCoordinates pvSat = state.getPVCoordinates(frame);
                // Occulted body is set far enough (10 billion km) in direction of occulted
                // direction
                final Vector3D dir =
                    EclipseDetector.this.occultedDirection
                        .getVector(new ConstantPVCoordinatesProvider(
                            state.getPVCoordinates(), state.getFrame()), date, frame)
                        .scalarMultiply(FAR_DISTANCE);
                // Velocity is no computable/unused
                return new PVCoordinates(pvSat.getPosition().add(dir), Vector3D.ZERO);
            }

            /** {@inheritDoc} */
            @Override
            public Frame getNativeFrame(final AbsoluteDate date) throws PatriusException {
                // Frame is unknown and unused
                throw new PatriusException(PatriusMessages.INTERNAL_ERROR);
            }
        };
    }

    /**
     * {@inheritDoc}
     * <p>
     * The following attributes are not deeply copied:
     * <ul>
     * <li>occultingBody: {@link PVCoordinatesProvider}</li>
     * <li>occultedBody: {@link PVCoordinatesProvider}</li>
     * <li>occultingRadiusProvider: {@link ApparentRadiusProvider}</li>
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
            if (getOccultingRadiusProvider() instanceof ConstantRadiusProvider) {
                // Occulting radius provider is an instance of ConstantRadiusProvider
                result = new EclipseDetector(getOcculted(), getOccultedRadius(),
                    getOcculting(),
                    ((ConstantRadiusProvider) getOccultingRadiusProvider())
                        .getApparentRadius(null, null, null, null),
                    this.ratio, getMaxCheckInterval(), getThreshold(),
                    getActionAtEntry(), getActionAtExit(), isRemoveAtEntry(),
                    isRemoveAtExit());
            } else {
                // Occulting radius provider is not an instance of ConstantRadiusProvider (but of
                // VariableRadiusProvider)
                result = new EclipseDetector(getOcculted(), getOccultedRadius(),
                    ((VariableRadiusProvider) getOccultingRadiusProvider()).getBodyShape(),
                    this.ratio, getMaxCheckInterval(), getThreshold(),
                    getActionAtEntry(), getActionAtExit(), isRemoveAtEntry(),
                    isRemoveAtExit());
            }
        } else // Occulted direction is not null
        // Check if occulting body is an instance of BodyShape
        if (getOcculting() instanceof BodyShape) {
            // Occulting body is an instance of BodyShape
            result = new EclipseDetector(this.occultedDirection,
                (BodyShape) getOcculting(), this.ratio,
                getMaxCheckInterval(), getThreshold(), getActionAtEntry(),
                getActionAtExit(), isRemoveAtEntry(), isRemoveAtExit());
        } else {
            // Occulting body is not an instance of BodyShape (but of PVCoordinatesProvider)
            result = new EclipseDetector(this.occultedDirection, getOcculting(),
                this.ratio, getMaxCheckInterval(), getThreshold(),
                getActionAtEntry(), getActionAtExit(), isRemoveAtEntry(),
                isRemoveAtExit());
        }

        result.setPropagationDelayType(getPropagationDelayType(), getInertialFrame());
        return result;
    }

}
