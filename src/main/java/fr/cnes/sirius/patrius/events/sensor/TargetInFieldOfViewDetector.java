/**
 * 
 * Copyright 2011-2017 CNES
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
 * HISTORY
 * VERSION:4.5:DM:DM-2460:27/05/2020:Prise en compte des temps de propagation dans les calculs evenements
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 *                             (added forward parameter to eventOccurred signature)
 * VERSION::FA:231:03/04/2014:bad updating of the assembly's tree of frames
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:454:24/11/2015:Add constructors, overload method shouldBeRemoved() and adapt eventOccured()
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events.sensor;

import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.models.SensorModel;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.AbstractDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * Event detector for the main target visibility of a sensor. The g function is positive if the main target is in the
 * field of view.
 * </p>
 * <p>
 * The default implementation behavior is to
 * {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#CONTINUE
 * continue} propagation when entering the zone and to
 * {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#STOP stop} propagation when exiting the zone.
 * This can be changed by using one of the provided constructors.
 * </p>
 * <p>
 * This detector can takes into account signal propagation duration through
 * {@link #setPropagationDelayType(PropagationDelayType)} (default is signal being instantaneous).
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
 */
public class TargetInFieldOfViewDetector extends AbstractDetector {

    /** serial ID */
    private static final long serialVersionUID = 5320120546742645596L;

    /** the sensor */
    private final SensorModel sensor;

    /** the assembly to consider */
    private final Assembly inAssembly;

    /** Action performed when entering the visibility zone. */
    private final Action actionAtEntry;

    /** Action performed when exiting the visibility zone. */
    private final Action actionAtExit;

    /** True if detector should be removed when entering the visibility zone. */
    private final boolean removeAtEntry;

    /** True if detector should be removed when exiting the visibility zone. */
    private final boolean removeAtExit;

    /** True if detector should be removed (updated by eventOccured). */
    private boolean shouldBeRemovedFlag = false;

    /**
     * Constructor for the "main target in field of view" detector
     * <p>
     * The default implementation behavior is to
     * {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#CONTINUE continue} propagation when
     * entering the zone and to {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#STOP stop}
     * propagation when exiting the zone.
     * </p>
     * 
     * @param assembly the assembly to consider (its main part frame must have a parent frame !!)
     * @param partName the name of the part that supports the sensor
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     */
    public TargetInFieldOfViewDetector(final Assembly assembly, final String partName,
        final double maxCheck, final double threshold) {
        this(new SensorModel(assembly, partName), maxCheck, threshold, Action.CONTINUE, Action.STOP);
    }

    /**
     * Constructor for the "main target in field of view" detector
     * 
     * @param assembly the assembly to consider (its main part frame must have a parent frame !!)
     * @param partName the name of the part that supports the sensor
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @param entry action performed when entering the visibility zone
     * @param exit action performed when exiting the visibility zone
     */
    public TargetInFieldOfViewDetector(final Assembly assembly, final String partName,
        final double maxCheck, final double threshold, final Action entry, final Action exit) {
        this(new SensorModel(assembly, partName), maxCheck, threshold, entry, exit);
    }

    /**
     * Constructor for the "main target in field of view" detector
     * <p>
     * The default implementation behavior is to
     * {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#CONTINUE continue} propagation when
     * entering the zone and to {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#STOP stop}
     * propagation when exiting the zone.
     * </p>
     * 
     * @param sensorModel the sensor model (the main part frame of the assembly must have a parent
     *        frame !!)
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     */
    public TargetInFieldOfViewDetector(final SensorModel sensorModel, final double maxCheck,
        final double threshold) {
        this(sensorModel, maxCheck, threshold, Action.CONTINUE, Action.STOP);
    }

    /**
     * Constructor for the "main target in field of view" detector
     * 
     * @param sensorModel the sensor model (the main part frame of the assembly must have a parent
     *        frame !!)
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @param entry action performed when entering the visibility zone
     * @param exit action performed when exiting the visibility zone
     */
    public TargetInFieldOfViewDetector(final SensorModel sensorModel, final double maxCheck,
        final double threshold, final Action entry, final Action exit) {
        this(sensorModel, maxCheck, threshold, entry, exit, false, false);
    }

    /**
     * Constructor for the "main target in field of view" detector
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
    public TargetInFieldOfViewDetector(final SensorModel sensorModel, final double maxCheck,
        final double threshold, final Action entry, final Action exit,
        final boolean removeEntry, final boolean removeExit) {
        super(maxCheck, threshold);
        this.sensor = sensorModel;
        this.inAssembly = sensorModel.getAssembly();

        this.actionAtEntry = entry;
        this.actionAtExit = exit;
        this.removeAtEntry = removeEntry;
        this.removeAtExit = removeExit;
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
        final AbsoluteDate targetDate = getSignalEmissionDate(sensor.getMainTarget(), s, getThreshold(),
                getPropagationDelayType());
        final double targetAngularRadius = this.sensor.getMainTargetAngularRadius(targetDate);
        return this.sensor.getTargetCenterFOVAngle(targetDate) + targetAngularRadius;
    }

    /** {@inheritDoc} */
    @Override
    public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                final boolean forward) throws PatriusException {
        if (increasing) {
            this.shouldBeRemovedFlag = this.removeAtEntry;
            return this.actionAtEntry;
        } else {
            this.shouldBeRemovedFlag = this.removeAtExit;
            return this.actionAtExit;
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean shouldBeRemoved() {
        return this.shouldBeRemovedFlag;
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
        final TargetInFieldOfViewDetector res = new TargetInFieldOfViewDetector(this.sensor,
                this.getMaxCheckInterval(), this.getThreshold(), this.actionAtEntry, this.actionAtExit,
                this.removeAtEntry, this.removeAtExit);
        res.setPropagationDelayType(getPropagationDelayType());
        return res;
    }
}
