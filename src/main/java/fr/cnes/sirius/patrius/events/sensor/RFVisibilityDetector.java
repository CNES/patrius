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
 * @history created 06/09/2012
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
 * VERSION::FA:410:16/04/2015: Anomalies in the Patrius Javadoc
 * VERSION::DM:454:24/11/2015:Add constructors, overload method shouldBeRemoved() and adapt eventOccured()
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events.sensor;

import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.models.RFLinkBudgetModel;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.AbstractDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Finder for ground station / satellite RF visibility events.
 * <p>
 * The default implementation behavior is to
 * {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#CONTINUE continue} propagation at raising and
 * to {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#STOP stop} propagation at setting. This can
 * be changed by This can be changed by using provided constructors.
 * </p>
 * <p>
 * This detector can takes into account signal propagation duration through {@link #getPropagationDelayType()} (default
 * is signal being instantaneous).
 * </p>
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment the direct use of a not thread-safe Assembly makes this class not
 *                      thread-safe itself
 * 
 * @see EventDetector
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.2
 */
public class RFVisibilityDetector extends AbstractDetector {

    /**
     * Serial ID
     */
    private static final long serialVersionUID = -8151701971140621219L;

    /**
     * The RF link budget model
     */
    private final RFLinkBudgetModel lbModel;

    /**
     * The RF link budget threshold for the nominal mode
     */
    private final double lbThreshold;

    /**
     * The assembly representing the satellite.
     */
    private final Assembly assembly;

    /**
     * Constructor for the sensor masking detector.
     * 
     * <p>
     * The default implementation behaviour is to
     * {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#CONTINUE continue} propagation when
     * entering the region of RF visibility and to stop when exiting.
     * </p>
     * 
     * @param linkBudgetModel the model for the computation of the link budget
     * @param linkBudgetThreshold link budget threshold for the nominal mode (dB)
     * @param maxCheck the maximal checking interval (s)
     * @param threshold the convergence threshold (s)
     */
    public RFVisibilityDetector(final RFLinkBudgetModel linkBudgetModel,
        final double linkBudgetThreshold, final double maxCheck, final double threshold) {
        this(linkBudgetModel, linkBudgetThreshold, maxCheck, threshold, Action.CONTINUE,
            Action.STOP);
    }

    /**
     * Constructor for the sensor masking detector.
     * 
     * @param linkBudgetModel the model for the computation of the link budget
     * @param linkBudgetThreshold link budget threshold for the nominal mode (dB)
     * @param maxCheck the maximal checking interval (s)
     * @param threshold the convergence threshold (s)
     * @param entry when entering the region of RF visibility.
     * @param exit when exiting the region of RF visibility.
     */
    public RFVisibilityDetector(final RFLinkBudgetModel linkBudgetModel,
        final double linkBudgetThreshold, final double maxCheck, final double threshold,
        final Action entry, final Action exit) {
        this(linkBudgetModel, linkBudgetThreshold, maxCheck, threshold, entry, exit, false, false);
    }

    /**
     * Constructor for the sensor masking detector.
     * 
     * @param linkBudgetModel the model for the computation of the link budget
     * @param linkBudgetThreshold link budget threshold for the nominal mode (dB)
     * @param maxCheck the maximal checking interval (s)
     * @param threshold the convergence threshold (s)
     * @param entry when entering the region of RF visibility.
     * @param exit when exiting the region of RF visibility.
     * @param removeEntry true if entering the region of RF visibility.
     * @param removeExit true if exiting the region of RF visibility.
     * @since 3.1
     */
    public RFVisibilityDetector(final RFLinkBudgetModel linkBudgetModel,
        final double linkBudgetThreshold, final double maxCheck, final double threshold,
        final Action entry, final Action exit, final boolean removeEntry,
        final boolean removeExit) {
        super(maxCheck, threshold, entry, exit, removeEntry, removeExit);
        this.lbModel = linkBudgetModel;
        this.lbThreshold = linkBudgetThreshold;
        this.assembly = this.lbModel.getSatellite();
    }

    /** {@inheritDoc} */
    @Override
    public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                final boolean forward) throws PatriusException {
        if (increasing) {
            this.shouldBeRemovedFlag = this.isRemoveAtExit();
        } else {
            this.shouldBeRemovedFlag = this.isRemoveAtEntry();
        }
        return increasing ? this.getActionAtEntry() : this.getActionAtExit();
    }

    /**
     * Compute the value of the switching function. This function becomes positive when entering the
     * region of RF visibility and negative when exiting.
     * 
     * @param s the current state information: date, kinematics, attitude
     * @return value of the switching function
     * @exception PatriusException if some specific error occurs
     */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public double g(final SpacecraftState s) throws PatriusException {
        this.assembly.updateMainPartFrame(s);
        // Emitter date
        final AbsoluteDate emitterDate = s.getDate();
        // Receiver date
        final AbsoluteDate receiverDate = getSignalReceptionDate(lbModel.getReceiver(), s.getOrbit(), s.getDate(),
            getThreshold());
        // Link Budget computation
        return this.lbModel.computeLinkBudget(emitterDate, receiverDate) - this.lbThreshold;
    }

    /**
     * Get the RF link budget model.
     * 
     * @return the RF link budget model
     */
    public RFLinkBudgetModel getLbModel() {
        return this.lbModel;
    }

    /**
     * Get the RF link budget threshold.
     * 
     * @return the RF link budget threshold
     */
    public double getLbThreshold() {
        return this.lbThreshold;
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
     * <li>lbModel: {@link RFLinkBudgetModel}</li>
     * </ul>
     * </p>
     */
    @Override
    public EventDetector copy() {
        final RFVisibilityDetector res = new RFVisibilityDetector(this.lbModel, this.lbThreshold,
            this.getMaxCheckInterval(), this.getThreshold(), this.getActionAtEntry(), this.getActionAtExit(),
            this.isRemoveAtEntry(), this.isRemoveAtExit());
        res.setPropagationDelayType(getPropagationDelayType(), getInertialFrame());
        return res;
    }
}
