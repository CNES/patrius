/**
 * 
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
 * @history creation 29/05/2012
 *
 * HISTORY
 * VERSION:4.9:DM:DM-3181:10/05/2022:[PATRIUS] Passage a protected de la methode setPropagationDelayType
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2460:27/05/2020:Prise en compte des temps de propagation dans les calculs evenements
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:231:03/04/2014:bad updating of the assembly's tree of frames
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:300:22/04/2015:Creation multi propagator
 * VERSION::DM:454:24/11/2015:Add constructors, overload method shouldBeRemoved() and adapt eventOccured()
 * VERSION::DM:1308:28/11/2017:Correct use of secondary spacecraft
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events.sensor;

import java.util.Map;

import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.models.SensorModel;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.AbstractDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.events.multi.MultiEventDetector;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * <p>
 * Mutual spacecraft visibility detector : the g function is positive only if each spacecraft is in the main field of
 * view of the other one's sensor. In a single spacecraft propagation mode ( {@link Propagator}), this event detector
 * shall be used in the propagation of a main spacecraft, and will use internally a given propagator to get the position
 * of the secondary spacecraft.
 * </p>
 * <p>
 * The default implementation behavior is to
 * {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#CONTINUE continue} propagation when entering
 * the visibility zone and to {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#STOP stop}
 * propagation when exiting the visibility zone.
 * </p>
 * <p>
 * This detector can takes into account signal propagation duration through
 * {@link #setPropagationDelayType(PropagationDelayType, fr.cnes.sirius.patrius.frames.Frame)} 
 * (default is signal being instantaneous).
 * </p>
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment the direct use of a not thread-safe Assembly makes this class not
 *                      thread-safe itself
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
@SuppressWarnings("PMD.NullAssignment")
public class SatToSatMutualVisibilityDetector extends AbstractDetector implements
    MultiEventDetector {

    /** serial ID */
    private static final long serialVersionUID = 8838555816491196733L;

    /** the main spacecraft id */
    private final String inMainSpacecraftId;

    /** the secondary spacecraft id */
    private final String inSecondarySpacecraftId;

    /** the main spacecraft sensor */
    private final SensorModel inSensorMainSpacecraft;

    /** the main assembly to consider */
    private final Assembly inMainSpacecraft;

    /** the secondary spacecraft sensor */
    private final SensorModel inSensorSecondarySpacecraft;

    /** the secondary assembly to consider */
    private final Assembly inSecondarySpacecraft;

    /** the propagator for the secondary spacecraft (in single spacecraft propagation mode) */
    private final Propagator secondPropagator;

    /** true if maskings must be computed */
    private final boolean maskingCheck;

    /** True if g() method is called for thr first time. */
    private boolean firstCall;

    /** Type of the propagation (mono or multi). */
    private final PropagationType type;

    /**
     * Propagation type.
     * 
     * @since 3.0
     */
    private static enum PropagationType {
        /** Mono sat propagation */
        MONO,
        /** Multi sat propagation */
        MULTI;
    }

    /**
     * <p>
     * Constructor to be used for single spacecraft propagation only ({@link Propagator}). Constructor for the mutual
     * spacecraft to spacecraft visibility detector.
     * <p>
     * The default implementation behavior is to
     * {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#CONTINUE continue} propagation when
     * entering the visibility zone and to {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#STOP
     * stop} propagation when exiting the visibility zone.
     * </p>
     * 
     * @param mainSensorModel the sensor model to consider for the main spacecraft. The secondary
     *        sensor model must be its main target !
     * @param secondarySensorModel the sensor model to consider for the secondary spacecraft. The
     *        main sensor model must be its main target !
     * @param secondSpacecraftPropagator the propagator for the secondary spacecraft
     * @param withMasking set true to compute maskings of each spacecrafts' sensors : in this case,
     *        there is no visibility if one of the masking bodies of both sensors is between the
     *        satellites.
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     */
    public SatToSatMutualVisibilityDetector(final SensorModel mainSensorModel,
        final SensorModel secondarySensorModel, final Propagator secondSpacecraftPropagator,
        final boolean withMasking, final double maxCheck, final double threshold) {
        this(mainSensorModel, secondarySensorModel, secondSpacecraftPropagator, withMasking,
            maxCheck, threshold, Action.CONTINUE, Action.STOP);
    }

    /**
     * Constructor to be used for single spacecraft propagation only ({@link Propagator}).
     * Constructor for the mutual spacecraft to spacecraft visibility detector.
     * 
     * @param mainSensorModel the sensor model to consider for the main spacecraft. The secondary
     *        sensor model must be its main target !
     * @param secondarySensorModel the sensor model to consider for the secondary spacecraft. The
     *        main sensor model must be its main target !
     * @param secondSpacecraftPropagator the propagator for the secondary spacecraft
     * @param withMasking set true to compute maskings of each spacecrafts' sensors : in this case,
     *        there is no visibility if one of the masking bodies of both sensors is between the
     *        satellites.
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @param entry action performed when entering the visibility zone
     * @param exit action performed when exiting the visibility zone
     */
    public SatToSatMutualVisibilityDetector(final SensorModel mainSensorModel,
        final SensorModel secondarySensorModel, final Propagator secondSpacecraftPropagator,
        final boolean withMasking, final double maxCheck, final double threshold,
        final Action entry, final Action exit) {
        this(mainSensorModel, secondarySensorModel, secondSpacecraftPropagator, withMasking,
            maxCheck, threshold, entry, exit, false, false);
    }

    /**
     * Constructor to be used for single spacecraft propagation only ({@link Propagator}).
     * Constructor for the mutual spacecraft to spacecraft visibility detector.
     * 
     * @param mainSensorModel the sensor model to consider for the main spacecraft. The secondary
     *        sensor model must be its main target
     * @param secondarySensorModel the sensor model to consider for the secondary spacecraft. The
     *        main sensor model must be its main target
     * @param secondSpacecraftPropagator the propagator for the secondary spacecraft
     * @param withMasking true to compute maskings of each spacecrafts' sensors : in this case,
     *        there is no visibility if one of the masking bodies of both sensors is between the
     *        satellites.
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @param entry action performed when entering the visibility zone
     * @param exit action performed when exiting the visibility zone
     * @param removeEntry true if entering the visibility zone
     * @param removeExit true if exiting the visibility zone
     * @since 3.1
     */
    public SatToSatMutualVisibilityDetector(final SensorModel mainSensorModel,
        final SensorModel secondarySensorModel, final Propagator secondSpacecraftPropagator,
        final boolean withMasking, final double maxCheck, final double threshold,
        final Action entry, final Action exit, final boolean removeEntry,
        final boolean removeExit) {
        super(maxCheck, threshold, entry, exit, removeEntry, removeExit);
        this.inMainSpacecraftId = null;
        this.inSecondarySpacecraftId = null;
        this.inSensorMainSpacecraft = mainSensorModel;
        this.inMainSpacecraft = this.inSensorMainSpacecraft.getAssembly();
        this.inSensorSecondarySpacecraft = secondarySensorModel;
        this.inSecondarySpacecraft = this.inSensorSecondarySpacecraft.getAssembly();
        this.secondPropagator = secondSpacecraftPropagator;
        this.maskingCheck = withMasking;
        this.firstCall = true;
        this.type = PropagationType.MONO;
    }

    /**
     * Constructor to be used for multi spacecraft propagation only (
     * {@link fr.cnes.sirius.patrius.propagation.MultiPropagator}). Constructor for the mutual
     * spacecraft to spacecraft visibility detector.
     * <p>
     * The default implementation behavior is to
     * {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#CONTINUE continue} propagation when
     * entering the visibility zone and to {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#STOP
     * stop} propagation when exiting the visibility zone.
     * </p>
     * 
     * @param mainSpacecraftId id of the main spacecraft
     * @param secondarySpacecraftId id of the secondary spacecraft
     * @param mainSensorModel the sensor model to consider for the main spacecraft. The secondary
     *        sensor model must be its main target !
     * @param secondarySensorModel the sensor model to consider for the secondary spacecraft. The
     *        main sensor model must be its main target !
     * @param withMasking set true to compute maskings of each spacecrafts' sensors : in this case,
     *        there is no visibility if one of the masking bodies of both sensors is between the
     *        satellites.
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @since 3.0
     */
    public SatToSatMutualVisibilityDetector(final String mainSpacecraftId,
        final String secondarySpacecraftId, final SensorModel mainSensorModel,
        final SensorModel secondarySensorModel, final boolean withMasking,
        final double maxCheck, final double threshold) {
        this(mainSpacecraftId, secondarySpacecraftId, mainSensorModel, secondarySensorModel,
            withMasking, maxCheck, threshold, Action.CONTINUE, Action.STOP);
    }

    /**
     * Constructor to be used for multi spacecraft propagation only (
     * {@link fr.cnes.sirius.patrius.propagation.MultiPropagator}). Constructor for the mutual
     * spacecraft to spacecraft visibility detector.
     * 
     * @param mainSpacecraftId id of the main spacecraft
     * @param secondarySpacecraftId id of the secondary spacecraft
     * @param mainSensorModel the sensor model to consider for the main spacecraft. The secondary
     *        sensor model must be its main target !
     * @param secondarySensorModel the sensor model to consider for the secondary spacecraft. The
     *        main sensor model must be its main target !
     * @param withMasking set true to compute maskings of each spacecrafts' sensors : in this case,
     *        there is no visibility if one of the masking bodies of both sensors is between the
     *        satellites.
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @param entry action performed when entering the visibility zone
     * @param exit action performed when exiting the visibility zone
     * @since 3.0
     */
    public SatToSatMutualVisibilityDetector(final String mainSpacecraftId,
        final String secondarySpacecraftId, final SensorModel mainSensorModel,
        final SensorModel secondarySensorModel, final boolean withMasking,
        final double maxCheck, final double threshold, final Action entry, final Action exit) {
        this(mainSpacecraftId, secondarySpacecraftId, mainSensorModel, secondarySensorModel,
            withMasking, maxCheck, threshold, entry, exit, false, false);
    }

    /**
     * Constructor to be used for multi-spacecraft propagation only (
     * {@link fr.cnes.sirius.patrius.propagation.MultiPropagator}). Constructor for the mutual
     * spacecraft to spacecraft visibility detector.
     * 
     * @param mainSpacecraftId id of the main spacecraft
     * @param secondarySpacecraftId id of the secondary spacecraft
     * @param mainSensorModel the sensor model to consider for the main spacecraft. The secondary
     *        sensor model must be its main target
     * @param secondarySensorModel the sensor model to consider for the secondary spacecraft. The
     *        main sensor model must be its main target
     * @param withMasking true to compute maskings of each spacecrafts' sensors : in this case,
     *        there is no visibility if one of the masking bodies of both sensors is between the
     *        satellites.
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @param entry action performed when entering the visibility zone
     * @param exit action performed when exiting the visibility zone
     * @param removeEntry true if entering the visibility zone.
     * @param removeExit true if exiting the visibility zone.
     * @since 3.1
     */
    public SatToSatMutualVisibilityDetector(final String mainSpacecraftId,
        final String secondarySpacecraftId, final SensorModel mainSensorModel,
        final SensorModel secondarySensorModel, final boolean withMasking,
        final double maxCheck, final double threshold, final Action entry, final Action exit,
        final boolean removeEntry, final boolean removeExit) {
        super(maxCheck, threshold, entry, exit, removeEntry, removeExit);
        this.inMainSpacecraftId = mainSpacecraftId;
        this.inSecondarySpacecraftId = secondarySpacecraftId;
        this.inSensorMainSpacecraft = mainSensorModel;
        this.inMainSpacecraft = this.inSensorMainSpacecraft.getAssembly();
        this.inSensorSecondarySpacecraft = secondarySensorModel;
        this.inSecondarySpacecraft = this.inSensorSecondarySpacecraft.getAssembly();
        this.secondPropagator = null;
        this.maskingCheck = withMasking;
        this.firstCall = true;
        this.type = PropagationType.MULTI;
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        // Nothing to do
    }

    /** {@inheritDoc} */
    @Override
    public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                final boolean forward) throws PatriusException {
        return this.eventOccurred(increasing);
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public double g(final SpacecraftState s) throws PatriusException {
        if (this.firstCall) {
            // Check if the constructor for single propagation was defined
            if (this.type == PropagationType.MULTI) {
                throw new PatriusException(PatriusMessages.MONO_MULTI_DETECTOR);
            }
            this.firstCall = false;
        }
        final SpacecraftState secondState = this.secondPropagator.propagate(s.getDate());
        return this.g(s, secondState);
    }

    /**
     * Get the main spacecraft sensor.
     * 
     * @return the main spacecraft sensor.
     */
    public SensorModel getSensorMainSpacecraft() {
        return this.inSensorMainSpacecraft;
    }

    /**
     * Get the main assembly to consider.
     * 
     * @return the main assembly to consider.
     */
    public Assembly getMainSpacecraft() {
        return this.inMainSpacecraft;
    }

    /**
     * Get the secondary spacecraft sensor.
     * 
     * @return the secondary spacecraft sensor.
     */
    public SensorModel getSensorSecondarySpacecraft() {
        return this.inSensorSecondarySpacecraft;
    }

    /**
     * Get the secondary assembly to consider.
     * 
     * @return the secondary assembly to consider
     */
    public Assembly getSecondarySpacecraft() {
        return this.inSecondarySpacecraft;
    }

    /**
     * Check maskings.
     * 
     * @return true if maskings must be computed
     */
    public boolean isMaskingCheck() {
        return this.maskingCheck;
    }

    /**
     * Get the main spacecraft id.
     * 
     * @return the main spacecraft id
     */
    public String getInMainSpacecraftId() {
        return this.inMainSpacecraftId;
    }

    /**
     * Get the secondary spacecraft id.
     * 
     * @return the secondary spacecraft id
     */
    public String getInSecondarySpacecraftId() {
        return this.inSecondarySpacecraftId;
    }

    /** {@inheritDoc} */
    @Override
    public void init(final Map<String, SpacecraftState> s0, final AbsoluteDate t) {
        // Nothing to do
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public double g(final Map<String, SpacecraftState> s) throws PatriusException {
        if (this.firstCall) {
            // Check if the constructor for multi propagation was defined
            if (this.type == PropagationType.MONO) {
                throw new PatriusException(PatriusMessages.MONO_MULTI_DETECTOR);
            }
            this.firstCall = false;
        }
        final SpacecraftState firstState = s.get(this.inMainSpacecraftId);
        final SpacecraftState secondState = s.get(this.inSecondarySpacecraftId);
        return this.g(firstState, secondState);
    }

    /** {@inheritDoc} */
    @Override
    public Action eventOccurred(final Map<String, SpacecraftState> s, final boolean increasing,
                                final boolean forward) throws PatriusException {
        return this.eventOccurred(increasing);
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, SpacecraftState>
            resetStates(final Map<String, SpacecraftState> oldStates)
                                                                     throws PatriusException {
        return oldStates;
    }
    
    /** @inheritDoc */
    @Override
    public void setPropagationDelayType(final PropagationDelayType propagationDelayType, final Frame frame) {
        super.setPropagationDelayType(propagationDelayType, frame);
    }
    
    /**
     * Private eventOccured() method for both multi and mono event detection.
     * 
     * @param increasing if true, the value of the switching function increases when times increases
     *        around event (note that increase is measured with respect to physical time, not with
     *        respect to propagation which may go backward in time)
     * @return one of {@link EventDetector.Action#STOP}, {@link EventDetector.Action#RESET_STATE},
     *         {@link EventDetector.Action#RESET_DERIVATIVES}, {@link EventDetector.Action#CONTINUE}
     * @since 3.0
     */
    private Action eventOccurred(final boolean increasing) {
        if (increasing) {
            this.shouldBeRemovedFlag = this.isRemoveAtEntry();
            return this.getActionAtEntry();
        } else {
            this.shouldBeRemovedFlag = this.isRemoveAtExit();
            return this.getActionAtExit();
        }
    }

    /**
     * Private g() method for both multi and mono event detection.
     * 
     * @param main main spacecraft
     * @param second secondary spacecraft
     * @return value of the switching function
     * @throws PatriusException if some specific error occurs
     * 
     * @since 3.0
     */
    @SuppressWarnings("PMD.ShortMethodName")
    private double g(final SpacecraftState main, final SpacecraftState second)
                                                                              throws PatriusException {
        this.inMainSpacecraft.updateMainPartFrame(main);
        this.inSecondarySpacecraft.updateMainPartFrame(second);

        // Get emission date 
        final AbsoluteDate firstEmissionDate = getSignalEmissionDate(main.getOrbit(), second.getOrbit(),
            second.getDate(), getThreshold());
        final AbsoluteDate secondEmissionDate = getSignalEmissionDate(second.getOrbit(), main.getOrbit(),
            main.getDate(), getThreshold());

        // main spacecraft
        final double angularRadiusInMainSpacecraftSensor = this.inSensorMainSpacecraft
            .getTargetCenterFOVAngle(secondEmissionDate);

        // secondary spacecraft
        final double angularRadiusInSecondSpacecraftSensor = this.inSensorSecondarySpacecraft
            .getTargetCenterFOVAngle(firstEmissionDate);
        final double minVisi = MathLib.min(angularRadiusInMainSpacecraftSensor,
            angularRadiusInSecondSpacecraftSensor);

        final double minMaskingDists;
        if (this.maskingCheck) {
            // body shapes masking check for the main spacecraft
            final double bodyMaskingDistanceMain = this.inSensorMainSpacecraft
                .celestialBodiesMaskingDistance(secondEmissionDate);
            // spacecrafts maskings for the main spacecraft
            final double spacecraftsMaskingDistanceMain = this.inSensorMainSpacecraft
                .spacecraftsMaskingDistance(secondEmissionDate);

            final double minMaskingDistMain = MathLib.min(spacecraftsMaskingDistanceMain,
                bodyMaskingDistanceMain);

            // body shapes masking check for the secondary spacecraft
            final double bodyMaskingDistanceSecond = this.inSensorSecondarySpacecraft
                .celestialBodiesMaskingDistance(firstEmissionDate);

            // spacecrafts maskings for the secondary spacecraft
            final double spacecraftsMaskingDistanceSecond = this.inSensorSecondarySpacecraft
                .spacecraftsMaskingDistance(firstEmissionDate);

            final double minMaskingDistSecond = MathLib.min(bodyMaskingDistanceSecond,
                spacecraftsMaskingDistanceSecond);
            // both
            minMaskingDists = MathLib.min(minMaskingDistSecond, minMaskingDistMain);
        } else {
            minMaskingDists = Double.POSITIVE_INFINITY;
        }
        return MathLib.min(minVisi, minMaskingDists);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The following attributes are not deeply copied:
     * <ul>
     * <li>inSensorMainSpacecraft: {@link SensorModel}</li>
     * <li>inSensorSecondarySpacecraft: {@link SensorModel}</li>
     * <li>secondPropagator: {@link Propagator}</li>
     * </ul>
     * </p>
     */
    @Override
    public EventDetector copy() {
        final SatToSatMutualVisibilityDetector res = new SatToSatMutualVisibilityDetector(this.inSensorMainSpacecraft,
            this.inSensorSecondarySpacecraft, this.secondPropagator, this.maskingCheck, this.getMaxCheckInterval(),
            this.getThreshold(), this.getActionAtEntry(), this.getActionAtExit(), this.isRemoveAtEntry(),
            this.isRemoveAtExit());
        res.setPropagationDelayType(getPropagationDelayType(), getInertialFrame());
        return res;
    }
}
