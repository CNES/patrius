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
 */
/*
 *
 * HISTORY
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
* VERSION:4.9:DM:DM-3181:10/05/2022:[PATRIUS] Passage a protected de la methode setPropagationDelayType
* VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2460:27/05/2020:Prise en compte des temps de propagation dans les calculs evenements
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 *                             (added forward parameter to eventOccurred signature)
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:454:24/11/2015:Add constructors, overload method shouldBeRemoved() and adapt eventOccured()
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.TopocentricFrame;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Finder for satellite apparent elevation events.
 * <p>
 * This class finds apparent elevation events (i.e. apparent satellite raising and setting from a terrestrial
 * viewpoint).
 * </p>
 * <p>
 * Apparent elevation is the sum of geometrical elevation and refraction angle, the latter is 0 at zenith, about 1
 * arcminute at 45°, and 34 arcminutes at the horizon for optical wavelengths.
 * </p>
 * <p>
 * This event only makes sense for positive apparent elevation in the Earth environment and it is not suited for near
 * zenithal detection, where the simple {@link ElevationDetector} fits better.
 * </p>
 * <p>
 * Refraction angle is computed according to Saemundssen formula quoted by Meeus. For reference, see <b>Astronomical
 * Algorithms</b> (1998), 2nd ed, (ISBN 0-943396-61-1), chap. 15.
 * </p>
 * <p>
 * This formula is about 30 arcseconds of accuracy very close to the horizon, as variable atmospheric effects become
 * very important.
 * </p>
 * <p>
 * Local pressure and temperature can be set to correct refraction at the viewpoint.
 * </p>
 * <p>
 * The default implementation behavior is to {@link EventDetector.Action#CONTINUE continue} propagation at raising and
 * to {@link EventDetector.Action#STOP stop} propagation at setting. This can be changed by using the constructor
 * {@link #ApparentElevationDetector(double, TopocentricFrame, double, double, 
 * fr.cnes.sirius.patrius.propagation.events.EventDetector.Action, 
 * fr.cnes.sirius.patrius.propagation.events.EventDetector.Action)
 * ApparentElevationDetector}.
 * </p>
 * <p>
 * This detector can takes into account signal propagation duration through
 * {@link #setPropagationDelayType(PropagationDelayType, fr.cnes.sirius.patrius.frames.Frame)} 
 * (default is signal being instantaneous).
 * </p>
 *
 * @see fr.cnes.sirius.patrius.propagation.Propagator#addEventDetector(EventDetector)
 * @author Pascal Parraud
 */
public class ApparentElevationDetector extends AbstractDetector {

    /** Default local pressure at viewpoint (Pa). */
    public static final double DEFAULT_PRESSURE = 101000.0;

    /** Default local temperature at viewpoint (K). */
    public static final double DEFAULT_TEMPERATURE = 283.0;

    /** Serializable UID. */
    private static final long serialVersionUID = 2611286321482306850L;

    /** Elevation min value to compute refraction (under the horizon). */
    private static final double MIN_ELEVATION = -2.0;

    /** Elevation max value to compute refraction (zenithal). */
    private static final double MAX_ELEVATION = 89.89;

    /** C1. */
    private static final double C1 = 10.3;

    /** C2. */
    private static final double C2 = 5.11;

    /** C3. */
    private static final double C3 = 1.02;

    /** C4. */
    private static final double C4 = 60.;

    /** Local pressure. */
    private double pressure = DEFAULT_PRESSURE;

    /** Local temperature. */
    private double temperature = DEFAULT_TEMPERATURE;

    /** Refraction correction from local pressure and temperature. */
    private double correfrac = 1.;

    /** Threshold apparent elevation value. */
    private final double elevation;

    /** Topocentric frame in which elevation should be evaluated. */
    private final TopocentricFrame topo;

    /**
     * Build a new apparent elevation detector.
     * <p>
     * This simple constructor takes default values for maximal checking interval ( {@link #DEFAULT_MAXCHECK}) and
     * convergence threshold ({@link #DEFAULT_THRESHOLD}).
     * </p>
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#CONTINUE continue} propagation at raising
     * and to {@link EventDetector.Action#STOP stop} propagation at setting.
     * </p>
     *
     * @param elevationIn threshold elevation value
     * @param topoIn topocentric frame in which elevation should be evaluated
     */
    public ApparentElevationDetector(final double elevationIn, final TopocentricFrame topoIn) {
        this(elevationIn, topoIn, DEFAULT_MAXCHECK, DEFAULT_THRESHOLD);
    }

    /**
     * Build a new apparent elevation detector.
     * <p>
     * This constructor takes default value for convergence threshold ({@link #DEFAULT_THRESHOLD}).
     * </p>
     * <p>
     * The maximal interval between elevation checks should be smaller than the half duration of the minimal pass to
     * handle, otherwise some short passes could be missed.
     * </p>
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#CONTINUE continue} propagation at raising
     * and to {@link EventDetector.Action#STOP stop} propagation at setting.
     * </p>
     *
     * @param elevationIn threshold elevation value (rad)
     * @param topoIn topocentric frame in which elevation should be evaluated
     * @param maxCheck maximal checking interval (s)
     */
    public ApparentElevationDetector(final double elevationIn, final TopocentricFrame topoIn,
        final double maxCheck) {
        this(elevationIn, topoIn, maxCheck, DEFAULT_THRESHOLD);
    }

    /**
     * Build a new apparent elevation detector.
     * <p>
     * The maximal interval between elevation checks should be smaller than the half duration of the minimal pass to
     * handle, otherwise some short passes could be missed.
     * </p>
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#CONTINUE continue} propagation at raising
     * and to {@link EventDetector.Action#STOP stop} propagation at setting.
     * </p>
     *
     * @param elevationIn threshold elevation value (rad)
     * @param topoIn topocentric frame in which elevation should be evaluated
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     */
    public ApparentElevationDetector(final double elevationIn, final TopocentricFrame topoIn,
        final double maxCheck, final double threshold) {
        this(elevationIn, topoIn, maxCheck, threshold, Action.CONTINUE, Action.STOP);
    }

    /**
     * Build a new apparent elevation detector with specified actions at raising and setting.
     * <p>
     * The maximal interval between elevation checks should be smaller than the half duration of the minimal pass to
     * handle, otherwise some short passes could be missed.
     * </p>
     *
     * @param elevationIn threshold elevation value (rad)
     * @param topoIn topocentric frame in which elevation should be evaluated
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param raisingAction action performed at raising
     * @param settingAction action performed at setting
     */
    public ApparentElevationDetector(final double elevationIn, final TopocentricFrame topoIn,
        final double maxCheck, final double threshold, final Action raisingAction,
        final Action settingAction) {
        this(elevationIn, topoIn, maxCheck, threshold, raisingAction, settingAction, false, false);
    }

    /**
     * Build a new apparent elevation detector with specified actions at raising and setting.
     * <p>
     * The maximal interval between elevation checks should be smaller than the half duration of the minimal pass to
     * handle, otherwise some short passes could be missed.
     * </p>
     *
     * @param elevationIn threshold elevation value (rad)
     * @param topoIn topocentric frame in which elevation should be evaluated
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param raisingAction action performed at raising
     * @param settingAction action performed at setting
     * @param raisingRemove true if detector should be removed at raising
     * @param settingRemove true if detector should be removed at setting
     * @since 3.1
     */
    public ApparentElevationDetector(final double elevationIn, final TopocentricFrame topoIn,
        final double maxCheck, final double threshold, final Action raisingAction,
        final Action settingAction, final boolean raisingRemove, final boolean settingRemove) {
        super(maxCheck, threshold, raisingAction, settingAction, raisingRemove, settingRemove);
        this.elevation = elevationIn;
        this.topo = topoIn;
    }

    /**
     * Set the local pressure at topocentric frame origin if needed.
     * <p>
     * Otherwise the default value for the local pressure is set to {@link #DEFAULT_PRESSURE}.
     * </p>
     *
     * @param pressureIn the pressure to set (Pa)
     */
    public void setPressure(final double pressureIn) {
        this.pressure = pressureIn;
        this.correfrac = (pressureIn / DEFAULT_PRESSURE) * (DEFAULT_TEMPERATURE / this.temperature);
    }

    /**
     * Set the local temperature at topocentric frame origin if needed.
     * <p>
     * Otherwise the default value for the local temperature is set to {@link #DEFAULT_TEMPERATURE}.
     * </p>
     *
     * @param temperatureIn the temperature to set (K)
     */
    public void setTemperature(final double temperatureIn) {
        this.temperature = temperatureIn;
        this.correfrac = (this.pressure / DEFAULT_PRESSURE) * (DEFAULT_TEMPERATURE / temperatureIn);
    }

    /**
     * Get the threshold apparent elevation value.
     *
     * @return the threshold apparent elevation value (rad)
     */
    public double getElevation() {
        return this.elevation;
    }

    /**
     * Get the topocentric frame.
     *
     * @return the topocentric frame
     */
    public TopocentricFrame getTopocentricFrame() {
        return this.topo;
    }

    /**
     * Get the local pressure at topocentric frame origin.
     *
     * @return the pressure
     */
    public double getPressure() {
        return this.pressure;
    }

    /**
     * Get the local temperature at topocentric frame origin.
     *
     * @return the temperature
     */
    public double getTemperature() {
        return this.temperature;
    }

    /**
     * Handle an apparent elevation event and choose what to do next.
     *
     * @param s the current state information : date, kinematics, attitude
     * @param increasing if true, the value of the switching function increases when times increases
     *        around event
     * @param forward if true, the integration variable (time) increases during integration.
     * @return the action performed when apparent elevation is reached.
     * @exception PatriusException if some specific error occurs
     */
    @Override
    public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                final boolean forward) throws PatriusException {
        return super.eventOccurred(s, increasing, forward);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This function measures the difference between the current apparent elevation and the threshold apparent
     * elevation.
     * </p>
     */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public double g(final SpacecraftState state) throws PatriusException {
        // Emitter is the satellite, station is the receiver (since elevation is wrt to station)
        final AbsoluteDate recDate = getSignalReceptionDate(this.topo, state.getOrbit(), state.getDate());

        final double trueElevation = this.topo.getElevation(state.getPVCoordinates().getPosition(),
                state.getFrame(), recDate);
        return trueElevation + this.getRefraction(trueElevation) - this.elevation;
    }
    
    /** @inheritDoc */
    @Override
    public void setPropagationDelayType(final PropagationDelayType propagationDelayType, final Frame frame) {
        super.setPropagationDelayType(propagationDelayType, frame);
    }
    
    /**
     * Compute the refraction angle from the true (geometrical) elevation.
     *
     * @param trueElevation true elevation (rad)
     * @return refraction angle (rad)
     */
    private double getRefraction(final double trueElevation) {
        double refraction = 0.0;
        final double eld = MathLib.toDegrees(trueElevation);
        if (eld > MIN_ELEVATION && eld < MAX_ELEVATION) {
            final double tmp = eld + C1 / (eld + C2);
            final double ref = C3 / MathLib.tan(MathLib.toRadians(tmp)) / C4;
            refraction = MathLib.toRadians(this.correfrac * ref);
        }
        return refraction;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The following attributes are not deeply copied:
     * <ul>
     * <li>topo: {@link TopocentricFrame}</li>
     * </ul>
     * </p>
     */
    @Override
    public EventDetector copy() {
        final ApparentElevationDetector result = new ApparentElevationDetector(this.elevation, this.topo,
            this.getMaxCheckInterval(), this.getThreshold(), this.getActionAtEntry(), this.getActionAtExit(),
            this.isRemoveAtEntry(), this.isRemoveAtExit());
        result.setPressure(this.pressure);
        result.setTemperature(this.temperature);
        result.setPropagationDelayType(getPropagationDelayType(), getInertialFrame());
        return result;
    }
}
