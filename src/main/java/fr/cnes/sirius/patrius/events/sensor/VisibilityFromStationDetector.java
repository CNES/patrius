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
 * @history created 29/05/12
 *
 * HISTORY
* VERSION:4.8:DM:DM-2929:15/11/2021:[PATRIUS] Harmonisation des modeles de troposphere 
 * VERSION:4.5:DM:DM-2460:27/05/2020:Prise en compte des temps de propagation dans les calculs evenements
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:88:18/11/2013: update due to the refactoring of the signal correction classes
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 *                             (added forward parameter to eventOccurred signature)
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::FA:410:16/04/2015: Anomalies in the Patrius Javadoc
 * VERSION::DM:454:24/11/2015:Add shouldBeRemoved method
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events.sensor;

import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.frames.TopocentricFrame;
import fr.cnes.sirius.patrius.groundstation.GeometricStationAntenna;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.signalpropagation.AngularCorrection;
import fr.cnes.sirius.patrius.signalpropagation.troposphere.TroposphericCorrection;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Finder for satellite apparent entering in a station's field of view.
 * <p>
 * The tropospheric correction used can be set by the user.
 * </p>
 * <p>
 * The default implementation behavior is to
 * {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#CONTINUE continue} propagation at raising and
 * to {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#STOP stop} propagation at setting. This can
 * be changed by using one of the provided constructors.
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
 * @see TroposphericCorrection
 * @see EventDetector
 * @see GeometricStationAntenna
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.1
 * 
 */
@SuppressWarnings("PMD.NullAssignment")
public class VisibilityFromStationDetector extends AbstractDetectorWithTropoCorrection {

    /** Flag for raising detection (slopeSelection = 0). */
    public static final int RAISING = 0;

    /** Flag for setting detection (slopeSelection = 1). */
    public static final int SETTING = 1;

    /** Flag for raising/setting detection (slopeSelection = 2). */
    public static final int RAISING_SETTING = 2;

    /** Serializable UID. */
    private static final long serialVersionUID = 764052454048950887L;

    /** Action performed when propagation at raising. */
    private final Action actionAtRaising;

    /** Action performed when propagation at setting. */
    private final Action actionAtSetting;

    /** True if detector should be removed at raising. */
    private final boolean removeAtRaisingFlag;

    /** True if detector should be removed at setting. */
    private final boolean removeAtSettingFlag;

    /** True if detector should be removed (updated by eventOccured). */
    private boolean shouldBeRemovedFlag = false;

    /**
     * Build a new apparent elevation detector with parameterizable tropospheric correction.
     * <p>
     * The maximal interval between elevation checks should be smaller than the half duration of the minimal pass to
     * handle, otherwise some short passes could be missed.
     * </p>
     * <p>
     * The default implementation behavior is to
     * {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#CONTINUE continue} propagation at raising
     * and to {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#STOP stop} propagation at setting.
     * </p>
     * 
     * @param topoFrame the station's topocentric frame
     * @param azimElevMask azimElevMask the azimuth - elevation mask (rad)
     * @param correctionModel the elevation correction model for the station (set null to ignore
     *        tropospheric correction)
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     */
    public VisibilityFromStationDetector(final TopocentricFrame topoFrame,
        final double[][] azimElevMask, final AngularCorrection correctionModel,
        final double maxCheck, final double threshold) {
        this(topoFrame, azimElevMask, correctionModel, maxCheck, threshold, Action.CONTINUE,
            Action.STOP);
    }

    /**
     * Build a new apparent elevation detector with parameterizable tropospheric correction.
     * <p>
     * The maximal interval between elevation checks should be smaller than the half duration of the minimal pass to
     * handle, otherwise some short passes could be missed.
     * </p>
     * 
     * @param topoFrame the station's topocentric frame
     * @param azimElevMask azimElevMask the azimuth - elevation mask (rad)
     * @param correctionModel the elevation correction model for the station (set null to ignore
     *        tropospheric correction)
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param raising action performed when propagation at raising
     * @param setting action performed when propagation at setting
     */
    public VisibilityFromStationDetector(final TopocentricFrame topoFrame,
        final double[][] azimElevMask, final AngularCorrection correctionModel,
        final double maxCheck, final double threshold, final Action raising,
        final Action setting) {
        this(topoFrame, azimElevMask, correctionModel, maxCheck, threshold, raising, setting,
            false, false);
    }

    /**
     * Build a new apparent elevation detector with parameterizable tropospheric correction.
     * <p>
     * The maximal interval between elevation checks should be smaller than the half duration of the minimal pass to
     * handle, otherwise some short passes could be missed.
     * </p>
     * 
     * @param topoFrame the station's topocentric frame
     * @param azimElevMask azimElevMask the azimuth - elevation mask (rad)
     * @param correctionModel the elevation correction model for the station (set null to ignore
     *        tropospheric correction)
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param raising action performed when propagation at raising
     * @param setting action performed when propagation at setting
     * @param removeRaising true if detector should be removed at raising
     * @param removeSetting true if detector should be removed at setting
     */
    public VisibilityFromStationDetector(final TopocentricFrame topoFrame,
        final double[][] azimElevMask, final AngularCorrection correctionModel,
        final double maxCheck, final double threshold, final Action raising,
        final Action setting, final boolean removeRaising, final boolean removeSetting) {
        super(new GeometricStationAntenna(topoFrame, azimElevMask), correctionModel, maxCheck,
            threshold);
        // action
        this.actionAtRaising = raising;
        this.actionAtSetting = setting;
        // remove (or not) detector
        this.removeAtRaisingFlag = removeRaising;
        this.removeAtSettingFlag = removeSetting;
    }

    /**
     * Build a new apparent elevation detector with parameterizable tropospheric correction.
     * <p>
     * The maximal interval between elevation checks should be smaller than the half duration of the minimal pass to
     * handle, otherwise some short passes could be missed.
     * </p>
     * <p>
     * The default implementation behavior is to
     * {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#CONTINUE continue} propagation at raising
     * and to {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#STOP stop} propagation at setting.
     * </p>
     * 
     * @param stationModel the station sensor geometric model
     * @param correctionModel the elevation correction model for the station (set null to ignore
     *        tropospheric correction)
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     */
    public VisibilityFromStationDetector(final GeometricStationAntenna stationModel,
        final AngularCorrection correctionModel, final double maxCheck, final double threshold) {
        this(stationModel, correctionModel, maxCheck, threshold, Action.CONTINUE, Action.STOP);
    }

    /**
     * Build a new apparent elevation detector with parameterizable tropospheric correction.
     * <p>
     * The maximal interval between elevation checks should be smaller than the half duration of the minimal pass to
     * handle, otherwise some short passes could be missed.
     * </p>
     * 
     * @param stationModel the station sensor geometric model
     * @param correctionModel the elevation correction model for the station (set null to ignore
     *        tropospheric correction)
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param raising action performed when propagation at raising
     * @param setting action performed when propagation at setting
     */
    public VisibilityFromStationDetector(final GeometricStationAntenna stationModel,
        final AngularCorrection correctionModel, final double maxCheck, final double threshold,
        final Action raising, final Action setting) {
        this(stationModel, correctionModel, maxCheck, threshold, raising, setting, false, false);
    }

    /**
     * Build a new apparent elevation detector with parameterizable tropospheric correction.
     * <p>
     * The maximal interval between elevation checks should be smaller than the half duration of the minimal pass to
     * handle, otherwise some short passes could be missed.
     * </p>
     * 
     * @param stationModel the station sensor geometric model
     * @param correctionModel the elevation correction model for the station (set null to ignore
     *        tropospheric correction)
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param raising action performed when propagation at raising
     * @param setting action performed when propagation at setting
     * @param removeRaising true if detector should be removed at raising
     * @param removeSetting true if detector should be removed at setting
     */
    public VisibilityFromStationDetector(final GeometricStationAntenna stationModel,
        final AngularCorrection correctionModel, final double maxCheck, final double threshold,
        final Action raising, final Action setting, final boolean removeRaising,
        final boolean removeSetting) {
        super(stationModel, correctionModel, maxCheck, threshold);
        // action
        this.actionAtRaising = raising;
        this.actionAtSetting = setting;
        // remove (or not) detector
        this.removeAtRaisingFlag = removeRaising;
        this.removeAtSettingFlag = removeSetting;
    }

    /**
     * Build a new apparent elevation detector with parameterizable tropospheric correction.
     * <p>
     * The maximal interval between elevation checks should be smaller than the half duration of the minimal pass to
     * handle, otherwise some short passes could be missed.
     * </p>
     * 
     * @param stationModel the station sensor geometric model
     * @param correctionModel the elevation correction model for the station (set null to ignore
     *        tropospheric correction)
     * @param slopeSelection slope selection
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param action action performed when propagation at raising/setting depending on slope
     *        selection
     * @param remove true if detector should be removed at raising/setting depending on slope
     *        selection
     */
    public VisibilityFromStationDetector(final GeometricStationAntenna stationModel,
        final AngularCorrection correctionModel, final int slopeSelection,
        final double maxCheck, final double threshold, final Action action,
        final boolean remove) {
        super(stationModel, correctionModel, slopeSelection, maxCheck, threshold);
        if (slopeSelection == RAISING) {
            this.actionAtRaising = action;
            this.actionAtSetting = null;
            this.removeAtRaisingFlag = remove;
            this.removeAtSettingFlag = false;
        } else if (slopeSelection == SETTING) {
            this.actionAtRaising = null;
            this.actionAtSetting = action;
            this.removeAtRaisingFlag = false;
            this.removeAtSettingFlag = remove;
        } else {
            // detection at ascending and descending node
            this.actionAtRaising = action;
            this.actionAtSetting = action;
            this.removeAtRaisingFlag = remove;
            this.removeAtSettingFlag = remove;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        // Nothing to do
    }

    /**
     * Handle "visibility from station" event and choose what to do next.
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
        final Action result;
        if (this.getSlopeSelection() == RAISING) {
            result = this.actionAtRaising;
            this.shouldBeRemovedFlag = this.removeAtRaisingFlag;
        } else if (this.getSlopeSelection() == SETTING) {
            result = this.actionAtSetting;
            this.shouldBeRemovedFlag = this.removeAtSettingFlag;
        } else {
            if (forward ^ !increasing) {
                // ascending node case
                result = this.actionAtRaising;
                this.shouldBeRemovedFlag = this.removeAtRaisingFlag;
            } else {
                // descending node case
                result = this.actionAtSetting;
                this.shouldBeRemovedFlag = this.removeAtSettingFlag;
            }
        }
        // Return result
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public boolean shouldBeRemoved() {
        return this.shouldBeRemovedFlag;
    }

    /**
     * Compute the value of the switching function.
     * <p>
     * This function measures the angular distance between the current apparent vector to the spacecraft and the border
     * of the station's field of view. It is positive when the spacecraft is in the field
     * </p>
     * 
     * @param s the current state information: date, kinematics, attitude
     * @return value of the switching function
     * @exception PatriusException if some specific error occurs
     */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public double g(final SpacecraftState s) throws PatriusException {
        final Vector3D correctedVector = this.getCorrectedVector(s);
        return this.getStation().getFOV().getAngularDistance(correctedVector);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The following attributes are not deeply copied:
     * <ul>
     * <li>correction: {@link AngularCorrection}</li>
     * <li>station: {@link GeometricStationAntenna}</li>
     * </ul>
     * </p>
     */
    @Override
    public EventDetector copy() {
        final VisibilityFromStationDetector detector;
        if (this.getSlopeSelection() == RAISING) {
            detector = new VisibilityFromStationDetector(this.getStation(), this.getCorrection(),
                this.getSlopeSelection(), this.getMaxCheckInterval(), this.getThreshold(), this.actionAtRaising,
                this.removeAtRaisingFlag);
        } else if (this.getSlopeSelection() == SETTING) {
            detector = new VisibilityFromStationDetector(this.getStation(), this.getCorrection(),
                this.getSlopeSelection(), this.getMaxCheckInterval(), this.getThreshold(), this.actionAtSetting,
                this.removeAtSettingFlag);
        } else {
            detector = new VisibilityFromStationDetector(this.getStation(), this.getCorrection(),
                this.getMaxCheckInterval(), this.getThreshold(), this.actionAtRaising, this.actionAtSetting,
                this.removeAtRaisingFlag, this.removeAtSettingFlag);
        }
        detector.setPropagationDelayType(getPropagationDelayType());
        return detector;
    }

    /**
     * Getter for the station geodetic point.
     * 
     * @return the station geodetic point.
     */
    public GeodeticPoint getStationGeodeticPoint() {
        return this.getStation().getTopoFrame().getPoint();
    }

    /**
     * Getter for the Earth shape.
     * 
     * @return the Earth shape.
     */
    public BodyShape getBodyShape() {
        return this.getStation().getTopoFrame().getParentShape();
    }

    /**
     * Returns action at raising detection.
     * 
     * @return action at raising detection
     */
    public Action getActionAtRaising() {
        return this.actionAtRaising;
    }

    /**
     * Returns action at setting detection.
     * 
     * @return action at setting detection
     */
    public Action getActionAtSetting() {
        return this.actionAtSetting;
    }

    /**
     * Returns true if detection is removed after raising detection.
     * 
     * @return true if detection is removed after raising detection
     */
    public boolean removeAtRaising() {
        return this.removeAtRaisingFlag;
    }

    /**
     * Returns true if detection is removed after setting detection.
     * 
     * @return true if detection is removed after setting detection
     */
    public boolean removeAtSetting() {
        return this.removeAtSettingFlag;
    }
}
