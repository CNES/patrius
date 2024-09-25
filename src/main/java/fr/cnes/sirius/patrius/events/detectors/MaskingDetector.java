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
 * @history creation 18/06/2012
 *
 * HISTORY
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.13:DM:DM-37:08/12/2023:[PATRIUS] Date d'evenement et propagation du signal
 * VERSION:4.10.2:FA:FA-3289:31/01/2023:[PATRIUS] Problemes sur le masquage d une visi avec LIGHT_TIME
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:DM:DM-3238:03/11/2022:[PATRIUS] Masquages par des corps celestes dans VisibilityFromStationDetector
 * VERSION:4.10:DM:DM-3245:03/11/2022:[PATRIUS] Ajout du sens de propagation du signal dans ...
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:454:24/11/2015:Add constructors, overload method shouldBeRemoved() and adapt eventOccured()
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION::FA:231:03/04/2014:bad updating of the assembly's tree of frames
 * VERSION:4.9:DM:DM-3181:10/05/2022:[PATRIUS] Passage a protected de la methode setPropagationDelayType
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:FA:FA-2956:15/11/2021:[PATRIUS] Temps de propagation non implemente pour certains evenements 
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case (added forward
 * parameter to eventOccurred signature)
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events.detectors;

import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.models.SensorModel;
import fr.cnes.sirius.patrius.events.EventDetector;
import fr.cnes.sirius.patrius.events.detectors.VisibilityFromStationDetector.LinkType;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * Sensor masking detector.
 * </p>
 * <p>
 * The default implementation behaviour is to
 * {@link fr.cnes.sirius.patrius.events.EventDetector.Action#CONTINUE continue} propagation at raising and
 * to {@link fr.cnes.sirius.patrius.events.EventDetector.Action#STOP stop} propagation at setting. This can
 * be changed by using provided constructors.
 * </p>
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment The use of a not thread-safe SensorModel makes this class not thread-safe.
 * 
 * @see SensorModel
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.2
 */
public class MaskingDetector extends AbstractSignalPropagationDetector {

    /** Serializable UID. */
    private static final long serialVersionUID = 9023955237855203037L;

    /** "none" String */
    private static final String NONE = "none";

    /** the sensor */
    private final SensorModel sensor;

    /** the assembly to consider */
    private final Assembly inAssembly;

    /** first masking object name */
    private String maskingObjectName;

    /** first masking spacecraft's part name (if the first masking part is a spacecraft) */
    private String maskingPartName;

    /**
     * Constructor for the sensor masking detector.
     * <p>
     * The default implementation behavior is to
     * {@link fr.cnes.sirius.patrius.events.EventDetector.Action#CONTINUE continue} propagation at raising
     * and to {@link fr.cnes.sirius.patrius.events.EventDetector.Action#STOP stop} propagation at setting.
     * </p>
     * 
     * @param sensorModel the spacecraft's sensor model. A main target and some masking objects must
     *        have been set to compute this detection.
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     */
    public MaskingDetector(final SensorModel sensorModel, final double maxCheck,
        final double threshold) {
        this(sensorModel, maxCheck, threshold, Action.CONTINUE, Action.STOP);

    }

    /**
     * Constructor for the sensor masking detector.
     * 
     * @param sensorModel the spacecraft's sensor model. A main target and some masking objects must
     *        have been set to compute this detection.
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param raising action performed when propagation at raising
     * @param setting action performed when propagation at setting
     */
    public MaskingDetector(final SensorModel sensorModel, final double maxCheck,
        final double threshold, final Action raising, final Action setting) {
        this(sensorModel, maxCheck, threshold, raising, setting, false, false);
    }

    /**
     * Constructor for the sensor masking detector.
     * 
     * @param sensorModel the spacecraft's sensor model. A main target and some masking objects must
     *        have been set to compute this detection.
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param raising action performed when propagation at raising
     * @param setting action performed when propagation at setting
     * @param removeRaising true if detector should be removed at raising
     * @param removeSetting true if detector should be removed at setting
     * @since 3.1
     */
    public MaskingDetector(final SensorModel sensorModel, final double maxCheck,
        final double threshold, final Action raising, final Action setting,
        final boolean removeRaising, final boolean removeSetting) {
        super(maxCheck, threshold, raising, setting, removeRaising, removeSetting);
        this.sensor = sensorModel;
        this.inAssembly = sensorModel.getAssembly();
        this.maskingObjectName = NONE;
        this.maskingPartName = NONE;
    }

    /**
     * Constructor for the sensor masking detector.
     * 
     * @param assembly the spacecraft's model
     * @param partName the name of the part supporting the sensor
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     */
    public MaskingDetector(final Assembly assembly, final String partName, final double maxCheck,
        final double threshold) {
        this(new SensorModel(assembly, partName), maxCheck, threshold, Action.CONTINUE, Action.STOP);
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        // Nothing to do
    }

    /**
     * Handle "masking" event and choose what to do next.
     * 
     * @param s the current state information : date, kinematics, attitude
     * @param increasing if true, the value of the switching function increases when times increases
     *        around event.
     * @param forward if true, the integration variable (time) increases during integration.
     * @return the action performed when propagation raising or setting.
     * @exception PatriusException if some specific error occurs
     */
    @Override
    public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                final boolean forward) throws PatriusException {
        if (increasing) {
            this.shouldBeRemovedFlag = this.isRemoveAtEntry();
        } else {
            this.shouldBeRemovedFlag = this.isRemoveAtExit();
        }
        return increasing ? this.getActionAtEntry() : this.getActionAtExit();
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public double g(final SpacecraftState s) throws PatriusException {
        this.inAssembly.updateMainPartFrame(s);

        // Target date (uplink only)
        final AbsoluteDate targetDate = getSignalEmissionDate(s);

        // body shapes masking check
        // Record masking body first
        this.sensor.celestialBodiesMaskingDistance(s.getDate(), targetDate, getPropagationDelayType(), LinkType.UPLINK);
        double bodyMaskingDistance = Double.POSITIVE_INFINITY;
        if (this.sensor.getMaskingBody() != null) {
            bodyMaskingDistance = this.sensor.celestialBodiesMaskingDistance(s.getDate(), targetDate,
                    getPropagationDelayType(), LinkType.UPLINK);
        }

        // spacecrafts maskings
        // Record masking spacecraft first first
        this.sensor.spacecraftsMaskingDistance(s.getDate(), targetDate, getPropagationDelayType(), LinkType.UPLINK);
        double spacecraftsMaskingDistance = Double.POSITIVE_INFINITY;
        if (this.sensor.getMaskingSpacecraft() != null) {
            spacecraftsMaskingDistance = this.sensor.spacecraftsMaskingDistance(s.getDate(), targetDate,
                    getPropagationDelayType(), LinkType.UPLINK);
        }
        final double minMaskingDist = MathLib.min(bodyMaskingDistance, spacecraftsMaskingDistance);

        if (bodyMaskingDistance < 0.) {
            // Body shapes masking
            this.maskingObjectName = this.sensor.getMaskingBodyName();
            this.maskingPartName = NONE;
        } else if (spacecraftsMaskingDistance < 0.) {
            // Spacecraft masking
            this.maskingObjectName = this.sensor.getMaskingSpacecraftName();
            this.maskingPartName = this.sensor.getMaskingSpacecraftPartName();
        }

        return -minMaskingDist;
    }

    /**
     * Get the masking object.
     * 
     * @return the first masking object name (to be used in the user eventOccured method)
     */
    public String getMaskingObjectName() {
        return this.maskingObjectName;
    }

    /**
     * Get the masking part.
     * 
     * @return first masking spacecraft's part name if the first masking part is a spacecraft,
     *         "none" otherwise (to be used in the user eventOccured method)
     */
    public String getMaskingPartName() {
        return this.maskingPartName;
    }

    /**
     * Get the sensor.
     * 
     * @return the sensor
     */
    public SensorModel getSensor() {
        return this.sensor;
    }

    /**
     * Get the assembly.
     * 
     * @return the assembly
     */
    public Assembly getAssembly() {
        return this.inAssembly;
    }
    
    /** {@inheritDoc} */
    @Override
    public void setPropagationDelayType(final PropagationDelayType propagationDelayType, final Frame frame) {
        super.setPropagationDelayType(propagationDelayType, frame);
    }
    
    /** {@inheritDoc} */
    @Override
    public PVCoordinatesProvider getEmitter(final SpacecraftState s) {
        return this.sensor.getMainTarget();
    }

    /** {@inheritDoc} */
    @Override
    public PVCoordinatesProvider getReceiver(final SpacecraftState s) {
        return s.getOrbit();
    }

    /** {@inheritDoc} */
    @Override
    public DatationChoice getDatationChoice() {
        return DatationChoice.RECEIVER;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The following attributes are not deeply copied:
     * <ul>
     * <li>sensor: {@link SensorModel}</li>
     * </ul>
     * </p>
     */
    @Override
    public EventDetector copy() {
        return new MaskingDetector(this.sensor, this.getMaxCheckInterval(), this.getThreshold(),
            this.getActionAtEntry(),
            this.getActionAtExit(), this.isRemoveAtEntry(), this.isRemoveAtExit());
    }
}
