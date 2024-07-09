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
 * VERSION::FA:410:16/04/2015: Anomalies in the Patrius Javadoc
 * VERSION::DM:454:24/11/2015:Add constructors, overload method shouldBeRemoved() and adapt eventOccured()
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events.sensor;

import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.models.SensorModel;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.AbstractDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * Event detector for the inhibition of a sensor. The g function is positive if one of the inhibition target is in its
 * inhibition field.
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
public class SensorInhibitionDetector extends AbstractDetector {

    /** serial ID */
    private static final long serialVersionUID = 5320120546742645596L;

    /** the sensor */
    private final SensorModel sensor;

    /** the assembly to consider */
    private final Assembly inAssembly;

    /** the first inhibition target to enter its field */
    private int inhibitionNumber;

    /** Action performed when entering the inhibition zone. */
    private final Action actionAtEntry;

    /** Action performed when exiting the inhibition zone. */
    private final Action actionAtExit;

    /** True if detector should be removed when entering the inhibition zone. */
    private final boolean removeAtEntry;

    /** True if detector should be removed when exiting the inhibition zone. */
    private final boolean removeAtExit;

    /** True if detector should be removed (updated by eventOccured). */
    private boolean shouldBeRemovedFlag = false;

    /**
     * Constructor for the "inhibition" detector The default implementation behaviour is to
     * {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#CONTINUE continue} propagation when
     * entering the zone and to {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#STOP stop} when
     * exiting
     * the zone.
     * 
     * @param assembly the assembly to consider (its main part frame must have a parent frame !!)
     * @param partName the name of the part that supports the sensor
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     */
    public SensorInhibitionDetector(final Assembly assembly, final String partName,
        final double maxCheck, final double threshold) {
        this(new SensorModel(assembly, partName), maxCheck, threshold, Action.CONTINUE, Action.STOP);
    }

    /**
     * Constructor for the "inhibition" detector
     * 
     * @param assembly the assembly to consider (its main part frame must have a parent frame !!)
     * @param partName the name of the part that supports the sensor
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @param entry action performed when entering the inhibition zone
     * @param exit action performed when exiting the inhibition zone
     */
    public SensorInhibitionDetector(final Assembly assembly, final String partName,
        final double maxCheck, final double threshold, final Action entry, final Action exit) {
        this(new SensorModel(assembly, partName), maxCheck, threshold, entry, exit);
    }

    /**
     * Constructor for the "inhibition" detector The default implementation behaviour is to
     * {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#CONTINUE continue} propagation when
     * entering the zone and to stop when exiting the zone.
     * 
     * @param sensorModel the sensor model (the main part frame of the assembly must have a parent
     *        frame !!)
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     */
    public SensorInhibitionDetector(final SensorModel sensorModel, final double maxCheck,
        final double threshold) {
        this(sensorModel, maxCheck, threshold, Action.CONTINUE, Action.STOP);

    }

    /**
     * Constructor for the "inhibition" detector
     * 
     * @param sensorModel the sensor model (the main part frame of the assembly must have a parent
     *        frame !!)
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @param entry action performed when entering the inhibition zone
     * @param exit action performed when exiting the inhibition zone
     */
    public SensorInhibitionDetector(final SensorModel sensorModel, final double maxCheck,
        final double threshold, final Action entry, final Action exit) {
        this(sensorModel, maxCheck, threshold, entry, exit, false, false);
    }

    /**
     * Constructor for the "inhibition" detector
     * 
     * @param sensorModel the sensor model (the main part frame of the assembly must have a parent
     *        frame !!)
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @param entry action performed when entering the inhibition zone
     * @param exit action performed when exiting the inhibition zone
     * @param removeEntry true if entering the inhibition zone
     * @param removeExit true if exiting the inhibition zone
     * @since 3.1
     */
    public SensorInhibitionDetector(final SensorModel sensorModel, final double maxCheck,
        final double threshold, final Action entry, final Action exit,
        final boolean removeEntry, final boolean removeExit) {
        super(maxCheck, threshold);
        this.sensor = sensorModel;
        this.inAssembly = sensorModel.getAssembly();
        this.inhibitionNumber = 0;
        // actions
        this.actionAtEntry = entry;
        this.actionAtExit = exit;
        // remove (or not) detector
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

        // initialisations
        final int length = this.sensor.getInhibitionFieldsNumber();
        double angle = Double.NEGATIVE_INFINITY;

        // loop on each inhibition fields
        for (int i = 1; i <= length; i++) {
            // Inhibition target at emission date
            final AbsoluteDate inhibitionDate = getSignalEmissionDate(sensor.getInhibitionTarget(i - 1), s,
                    getThreshold(), getPropagationDelayType());

            // Get target angular radius for date and current field
            final double targetAngularRadius = this.sensor.getInhibitionTargetAngularRadius(inhibitionDate,
                i);
            // Compute angle value
            angle = MathLib.max(angle, this.sensor.getInhibitTargetCenterToFieldAngle(inhibitionDate, i)
                + targetAngularRadius);

            if (angle > 0.) {
                this.inhibitionNumber = i;
            }
        }

        return angle;
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
     * Get the inhibition number.
     * 
     * @return the first inhibition target to enter the field (to be used in the user eventOccured
     *         method) The first inhibition target is the number 1.
     */
    public int getInhibitionNumber() {
        return this.inhibitionNumber;
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
        final SensorInhibitionDetector res = new SensorInhibitionDetector(this.sensor, this.getMaxCheckInterval(),
                this.getThreshold(), this.actionAtEntry, this.actionAtExit, this.removeAtEntry, this.removeAtExit);
        res.setPropagationDelayType(getPropagationDelayType());
        return res;
    }
}
