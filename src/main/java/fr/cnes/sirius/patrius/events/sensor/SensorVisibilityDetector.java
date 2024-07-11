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
 * @history creation 23/04/2012
 *
 * HISTORY
 * VERSION:4.9:DM:DM-3181:10/05/2022:[PATRIUS] Passage a protected de la methode setPropagationDelayType
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2460:27/05/2020:Prise en compte des temps de propagation dans les calculs evenements
 * VERSION::FA:231:03/04/2014:bad updating of the assembly's tree of frames
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case (added forward
 * parameter to eventOccurred signature)
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::FA:410:16/04/2015: Anomalies in the Patrius Javadoc
 * VERSION::DM:454:24/11/2015:Add constructors, overload method shouldBeRemoved() and adapt eventOccured()
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events.sensor;

import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.models.SensorModel;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.AbstractDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * Event detector for the visibility of a sensor. The g function is positive if none of the inhibition target is in its
 * inhibition field, no masking object cuts the line segment between the sensor and the target and if the main target is
 * in the field of view.
 * </p>
 * <p>
 * The default implementation behaviour is to
 * {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#CONTINUE
 * continue} propagation when entering the zone and to
 * {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#STOP stop} when exiting the zone. This can be
 * changed by using provided constructors.
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
 * @see SensorModel
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public class SensorVisibilityDetector extends AbstractDetector {

    /** serial ID */
    private static final long serialVersionUID = 5320120546742645596L;

    /** "none" String */
    private static final String NONE = "none";

    /** the sensor */
    private final SensorModel sensor;

    /** the assembly to consider */
    private final Assembly inAssembly;

    /** the first inhibition target to enter its field */
    private int inhibitionNumber;

    /** first masking object name */
    private String maskingObjectName;

    /** first masking spacecraft's part name (if the first masking part is a spacecraft) */
    private String maskingPartName;

    /**
     * Constructor for the "visibility view" detector The default implementation behaviour is to
     * {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#CONTINUE continue} propagation when
     * entering the zone and to stop when exiting the zone.
     * 
     * @param assembly the assembly to consider (its main part frame must have a parent frame !!)
     * @param partName the name of the part that supports the sensor
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     */
    public SensorVisibilityDetector(final Assembly assembly, final String partName,
        final double maxCheck, final double threshold) {
        this(new SensorModel(assembly, partName), maxCheck, threshold, Action.CONTINUE, Action.STOP);
    }

    /**
     * Constructor for the "visibility view" detector
     * 
     * @param assembly the assembly to consider (its main part frame must have a parent frame !!)
     * @param partName the name of the part that supports the sensor
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @param entry action performed when entering the visibility zone
     * @param exit action performed when exiting the visibility zone
     */
    public SensorVisibilityDetector(final Assembly assembly, final String partName,
        final double maxCheck, final double threshold, final Action entry, final Action exit) {
        this(new SensorModel(assembly, partName), maxCheck, threshold, entry, exit);
    }

    /**
     * Constructor for the "visibility view" detector The default implementation behaviour is to
     * {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#CONTINUE continue} propagation when
     * entering the zone and to stop when exiting the zone.
     * 
     * @param sensorModel the sensor model (the main part frame of the assembly must have a parent
     *        frame !!)
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     */
    public SensorVisibilityDetector(final SensorModel sensorModel, final double maxCheck,
        final double threshold) {
        this(sensorModel, maxCheck, threshold, Action.CONTINUE, Action.STOP);
    }

    /**
     * Constructor for the "visibility" detector
     * 
     * @param sensorModel the sensor model (the main part frame of the assembly must have a parent
     *        frame !!)
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @param entry action performed when entering the visibility zone
     * @param exit action performed when exiting the visibility zone
     */
    public SensorVisibilityDetector(final SensorModel sensorModel, final double maxCheck,
        final double threshold, final Action entry, final Action exit) {
        this(sensorModel, maxCheck, threshold, entry, exit, false, false);
    }

    /**
     * Constructor for the "visibility" detector
     * 
     * @param sensorModel the sensor model (the main part frame of the assembly must have a parent
     *        frame !!)
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @param entry action performed when entering the visibility zone
     * @param exit action performed when exiting the visibility zone
     * @param removeEntry true if entering the visibility zone
     * @param removeExit true if exiting the visibility zone
     * @since 3.1
     */
    public SensorVisibilityDetector(final SensorModel sensorModel, final double maxCheck,
        final double threshold, final Action entry, final Action exit,
        final boolean removeEntry, final boolean removeExit) {
        super(maxCheck, threshold, entry, exit, removeEntry, removeExit);
        this.sensor = sensorModel;
        this.inAssembly = sensorModel.getAssembly();
        this.inhibitionNumber = 0;
        this.maskingObjectName = NONE;
        this.maskingPartName = NONE;
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        // Nothing to do
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public double g(final SpacecraftState s) throws PatriusException {
        this.inAssembly.updateMainPartFrame(s);

        // Target date at emission date
        final AbsoluteDate targetDate = getSignalEmissionDate(sensor.getMainTarget(), s.getOrbit(), s.getDate(),
            getThreshold());

        // initialisations
        final int length = this.sensor.getInhibitionFieldsNumber();
        double inhibitAngle = Double.NEGATIVE_INFINITY;

        // loop on each inhibition fields
        for (int i = 1; i <= length; i++) {
            final AbsoluteDate inhibitionDate = getSignalEmissionDate(sensor.getInhibitionTarget(i - 1), s.getOrbit(),
                s.getDate(), getThreshold());
            final double targetAngularRadius = this.sensor.getInhibitionTargetAngularRadius(inhibitionDate,
                i);
            inhibitAngle = MathLib
                .max(inhibitAngle, this.sensor.getInhibitTargetCenterToFieldAngle(inhibitionDate, i)
                    + targetAngularRadius);

            if (inhibitAngle > 0.) {
                this.inhibitionNumber = i;
            }
        }

        // body shapes masking check
        final double bodyMaskingDistance = this.sensor.celestialBodiesMaskingDistance(targetDate);

        // spacecrafts maskings
        final double spacecraftsMaskingDistance = this.sensor.spacecraftsMaskingDistance(targetDate);
        final double minMaskingDist = MathLib.min(bodyMaskingDistance, spacecraftsMaskingDistance);

        if (bodyMaskingDistance < 0.) {
            this.maskingObjectName = this.sensor.getMaskingBodyName();
            this.maskingPartName = NONE;
        } else if (spacecraftsMaskingDistance < 0.) {
            this.maskingObjectName = this.sensor.getMaskingSpacecraftName();
            this.maskingPartName = this.sensor.getMaskingSpacecraftPartName();
        }

        // maskings and inhibition
        final double minMaskingInhibit = MathLib.min(-inhibitAngle, minMaskingDist);

        // visibility angle computation
        final double visiAngle = this.sensor.getTargetCenterFOVAngle(targetDate)
            + this.sensor.getMainTargetAngularRadius(targetDate);

        return MathLib.min(minMaskingInhibit, visiAngle);
    }

    /** {@inheritDoc} */
    @Override
    public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                final boolean forward) throws PatriusException {
        if (increasing) {
            this.shouldBeRemovedFlag = this.isRemoveAtEntry();
            return this.getActionAtEntry();
        } else {
            this.shouldBeRemovedFlag = this.isRemoveAtExit();
            return this.getActionAtExit();
        }
    }

    /**
     * Get the inhibition number.
     * 
     * @return the first inhibition target to enter the field (to be used in the user eventOccured
     *         method)
     */
    public int getInhibitionNumber() {
        return this.inhibitionNumber;
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
     * Get the assembly.
     * 
     * @return the assembly
     */
    public Assembly getAssembly() {
        return this.inAssembly;
    }

    /**
     * Get the sensor.
     * 
     * @return the sensor
     */
    public SensorModel getSensor() {
        return this.sensor;
    }

    /** {@inheritDoc} */
    @Override
    public void setPropagationDelayType(final PropagationDelayType propagationDelayType, final Frame frame) {
        super.setPropagationDelayType(propagationDelayType, frame);
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
        final SensorVisibilityDetector res = new SensorVisibilityDetector(this.sensor, this.getMaxCheckInterval(),
            this.getThreshold(), this.getActionAtEntry(), this.getActionAtExit(), this.isRemoveAtEntry(),
            this.isRemoveAtExit());
        res.setPropagationDelayType(getPropagationDelayType(), getInertialFrame());
        return res;
    }
}
