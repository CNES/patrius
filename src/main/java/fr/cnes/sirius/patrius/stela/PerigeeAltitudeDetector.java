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
 * @history created 20/03/13
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::FA:410:16/04/2015: Anomalies in the Patrius Javadoc
 * VERSION::DM:454:24/11/2015:Add constructors, overload method shouldBeRemoved() and adapt eventOccured()
 * VERSION::FA:1286:05/09/2017:correct osculating orbit propagation
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela;

import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.ApsisOrbit;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.AbstractDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.stela.orbits.OrbitNatureConverter;
import fr.cnes.sirius.patrius.stela.orbits.StelaEquinoctialOrbit;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Finder for satellite altitude crossing events in Semi-analytical theory.
 * <p>
 * This class finds altitude events (i.e. satellite crossing a predefined altitude level above ground). The altitude
 * computed here is the one of the osculating perigee
 * </p>
 * <p>
 * The default implementation behavior is to
 * {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#CONTINUE continue} propagation when ascending
 * and to {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#STOP stop} propagation when descending.
 * This can be changed by overriding the {@link #eventOccurred(SpacecraftState, boolean, boolean) eventOccurred} method
 * in a derived class.
 * </p>
 * 
 * @concurrency conditionally thread-safe
 * @concurrency.comment the OrbitNatureConverter attribute needs to be thread-safe
 * 
 * @see fr.cnes.sirius.patrius.propagation.Propagator#addEventDetector(EventDetector)
 * @see fr.cnes.sirius.patrius.propagation.events.AltitudeDetector
 * 
 * @author Luc Maisonobe, Cedric Dental
 * 
 * @version $Id$
 * 
 * @since 1.3
 * 
 */
@SuppressWarnings("PMD.NullAssignment")
public class PerigeeAltitudeDetector extends AbstractDetector {

    /** Serializable UID. */
    private static final long serialVersionUID = -1552109617025755015L;

    /** Threshold altitude value (m). */
    private final double altitude;

    /** earth Radius with respect to which altitude should be evaluated. */
    private final double earthRadius;

    /** Is the perigee to be computed in osculating elements. */
    private final boolean isOsculating;

    /** Orbit convertor to compute osculating perigee. */
    private final OrbitNatureConverter orbitConverter;

    /**
     * Build a new altitude detector.
     * <p>
     * This simple constructor takes default values for maximal checking interval ( {@link #DEFAULT_MAXCHECK}) and
     * convergence threshold ({@link #DEFAULT_THRESHOLD}).
     * </p>
     * 
     * @param altitudeIn threshold altitude value
     * @param earthRadiusIn earth Radius (m) with respect to which altitude should be evaluated
     */
    public PerigeeAltitudeDetector(final double altitudeIn, final double earthRadiusIn) {
        super(DEFAULT_MAXCHECK, DEFAULT_THRESHOLD, Action.CONTINUE, Action.STOP, false, false);
        this.altitude = altitudeIn;
        this.earthRadius = earthRadiusIn;
        this.isOsculating = false;
        this.orbitConverter = null;
    }

    /**
     * Build a new altitude detector.
     * <p>
     * This simple constructor takes default value for convergence threshold ( {@link #DEFAULT_THRESHOLD}).
     * </p>
     * <p>
     * The maximal interval between altitude checks should be smaller than the half duration of the minimal pass to
     * handle, otherwise some short passes could be missed.
     * </p>
     * 
     * @param maxCheck maximal checking interval (s)
     * @param altitudeIn threshold altitude value (m)
     * @param earthRadiusIn earth Radius (m) with respect to which altitude should be evaluated
     */
    public PerigeeAltitudeDetector(final double maxCheck, final double altitudeIn,
        final double earthRadiusIn) {
        super(maxCheck, DEFAULT_THRESHOLD, Action.CONTINUE, Action.STOP, false, false);
        this.altitude = altitudeIn;
        this.earthRadius = earthRadiusIn;
        this.isOsculating = false;
        this.orbitConverter = null;
    }

    /**
     * Build a new altitude detector.
     * <p>
     * The maximal interval between altitude checks should be smaller than the half duration of the minimal pass to
     * handle, otherwise some short passes could be missed.
     * </p>
     * <p>
     * The default implementation behavior is to
     * {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#CONTINUE continue} propagation when
     * ascending and to {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#STOP stop} propagation
     * when descending.
     * </p>
     * 
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param altitudeIn threshold altitude value (m)
     * @param earthRadiusIn earth Radius (m) with respect to which altitude should be evaluated
     */
    public PerigeeAltitudeDetector(final double maxCheck, final double threshold,
        final double altitudeIn, final double earthRadiusIn) {
        this(maxCheck, threshold, altitudeIn, earthRadiusIn, Action.CONTINUE, Action.STOP);
    }

    /**
     * Build a new altitude detector.
     * <p>
     * The maximal interval between altitude checks should be smaller than the half duration of the minimal pass to
     * handle, otherwise some short passes could be missed.
     * </p>
     * 
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param altitudeIn threshold altitude value (m)
     * @param earthRadiusIn earth Radius (m) with respect to which altitude should be evaluated
     * @param ascending action performed when ascending
     * @param descending action performed when descending
     */
    public PerigeeAltitudeDetector(final double maxCheck, final double threshold,
        final double altitudeIn, final double earthRadiusIn, final Action ascending,
        final Action descending) {
        this(maxCheck, threshold, altitudeIn, earthRadiusIn, ascending, descending, false, false);
    }

    /**
     * Build a new altitude detector.
     * <p>
     * The maximal interval between altitude checks should be smaller than the half duration of the minimal pass to
     * handle, otherwise some short passes could be missed.
     * </p>
     * 
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param altitudeIn threshold altitude value (m)
     * @param earthRadiusIn earth Radius (m) with respect to which altitude should be evaluated
     * @param ascending action performed when ascending
     * @param descending action performed when descending
     * @param removeAscending true if detector should be removed when ascending
     * @param removeDescending true if detector should be removed when descending
     * @since 3.1
     */
    public PerigeeAltitudeDetector(final double maxCheck, final double threshold,
        final double altitudeIn, final double earthRadiusIn, final Action ascending,
        final Action descending, final boolean removeAscending, final boolean removeDescending) {
        super(maxCheck, threshold, ascending, descending, removeAscending, removeDescending);
        this.altitude = altitudeIn;
        this.earthRadius = earthRadiusIn;
        this.isOsculating = false;
        this.orbitConverter = null;
    }

    /**
     * Build a new altitude detector, with osculating perigee.
     * <p>
     * The maximal interval between altitude checks should be smaller than the half duration of the minimal pass to
     * handle, otherwise some short passes could be missed.
     * </p>
     * <p>
     * The default implementation behavior is to
     * {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#CONTINUE continue} propagation when
     * ascending and to {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#STOP stop} propagation
     * when descending.
     * </p>
     * 
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param altitudeIn threshold altitude value (m)
     * @param earthRadiusIn earth Radius (m) with respect to which altitude should be evaluated
     * @param orbitConverterIn orbit convertor to be used when computing perigee altitude
     */
    public PerigeeAltitudeDetector(final double maxCheck, final double threshold,
        final double altitudeIn, final double earthRadiusIn,
        final OrbitNatureConverter orbitConverterIn) {
        this(maxCheck, threshold, altitudeIn, earthRadiusIn, orbitConverterIn, Action.CONTINUE,
            Action.STOP);
    }

    /**
     * Build a new altitude detector, with osculating perigee.
     * <p>
     * The maximal interval between altitude checks should be smaller than the half duration of the minimal pass to
     * handle, otherwise some short passes could be missed.
     * </p>
     * 
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param altitudeIn threshold altitude value (m)
     * @param earthRadiusIn Earth Radius (m) with respect to which altitude should be evaluated
     * @param orbitConverterIn orbit convertor to be used when computing perigee altitude
     * @param ascending action performed when ascending
     * @param descending action performed when descending
     */
    public PerigeeAltitudeDetector(final double maxCheck, final double threshold,
        final double altitudeIn, final double earthRadiusIn,
        final OrbitNatureConverter orbitConverterIn, final Action ascending,
        final Action descending) {
        this(maxCheck, threshold, altitudeIn, earthRadiusIn, orbitConverterIn, ascending,
            descending, false, false);
    }

    /**
     * Build a new altitude detector, with osculating perigee.
     * <p>
     * The maximal interval between altitude checks should be smaller than the half duration of the minimal pass to
     * handle, otherwise some short passes could be missed.
     * </p>
     * 
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param altitudeIn threshold altitude value (m)
     * @param earthRadiusIn earth Radius (m) with respect to which altitude should be evaluated
     * @param orbitConverterIn orbit convertor to be used when computing perigee altitude
     * @param ascending action performed when ascending
     * @param descending action performed when descending
     * @param removeAscending true if detector should be removed when ascending
     * @param removeDescending true if detector should be removed when descending
     * @since 3.1
     */
    public PerigeeAltitudeDetector(final double maxCheck, final double threshold,
        final double altitudeIn, final double earthRadiusIn,
        final OrbitNatureConverter orbitConverterIn, final Action ascending,
        final Action descending, final boolean removeAscending, final boolean removeDescending) {
        super(maxCheck, threshold, ascending, descending, removeAscending, removeDescending);
        this.altitude = altitudeIn;
        this.earthRadius = earthRadiusIn;
        this.orbitConverter = orbitConverterIn;
        this.isOsculating = true;

    }

    /**
     * Get the threshold altitude value.
     * 
     * @return the threshold altitude value (m)
     */
    public double getAltitude() {
        return this.altitude;
    }

    /**
     * Get the earth radius.
     * 
     * @return the body shape
     */
    public double getEarthRadius() {
        return this.earthRadius;
    }

    /**
     * Handle an altitude event and choose what to do next.
     * <p>
     * The perigee Altitude is computed using IERS92 value for Earth Equatorial Radius.
     * </p>
     * 
     * @param s the current state information : date, kinematics, attitude
     * @param increasing if true, the value of the switching function increases when times increases
     *        around event
     * @param forward if true, the integration variable (time) increases during integration.
     * @return {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#STOP stop} or
     *         {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#CONTINUE
     *         continue}
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

    /**
     * Compute the value of the switching function. This function measures the difference between
     * the osculating perigee altitude and the threshold altitude.
     * 
     * @param s the current state information: date, kinematics, attitude
     * @return value of the switching function
     * @exception PatriusException if some specific error occurs
     */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public double g(final SpacecraftState s) throws PatriusException {
        // get the orbit
        final ApsisOrbit apsOrb;
        if (this.isOsculating) {
            final StelaEquinoctialOrbit meanOrbit = new StelaEquinoctialOrbit(s.getOrbit());
            // Mean anomaly to zero for perigee computations
            final double ksiAnomZero = MathLib.atan2(meanOrbit.getEquinoctialEy(),
                meanOrbit.getEquinoctialEx());
            final StelaEquinoctialOrbit meanOrbit2 = new StelaEquinoctialOrbit(meanOrbit.getA(),
                meanOrbit.getEquinoctialEx(), meanOrbit.getEquinoctialEy(), meanOrbit.getIx(),
                meanOrbit.getIy(), ksiAnomZero, meanOrbit.getFrame(), meanOrbit.getDate(),
                meanOrbit.getMu());

            final StelaEquinoctialOrbit oscOrbit = this.orbitConverter.toOsculating(meanOrbit2);
            apsOrb = new ApsisOrbit(oscOrbit);
        } else {
            apsOrb = new ApsisOrbit(s.getOrbit());

        }

        // compute Perigee Altitude
        final double zp = apsOrb.getPeriapsis() - this.earthRadius;
        return zp - this.altitude;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The following attributes are not deeply copied:
     * <ul>
     * <li>orbitConverter: {@link OrbitNatureConverter}</li>
     * </ul>
     * </p>
     */
    @Override
    public EventDetector copy() {
        return new PerigeeAltitudeDetector(this.getMaxCheckInterval(), this.getThreshold(), this.altitude,
            this.earthRadius, this.orbitConverter, this.getActionAtEntry(), this.getActionAtExit(),
            this.isRemoveAtEntry(), this.isRemoveAtExit());
    }
}
