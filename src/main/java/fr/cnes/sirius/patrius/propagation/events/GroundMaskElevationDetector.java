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
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import java.util.Arrays;
import java.util.Comparator;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.TopocentricFrame;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Finder for satellite azimuth-elevation events with respect to a mask.
 * <p>
 * This class finds elevation events (i.e. satellite raising and setting) with respect to an azimuth-elevation mask.
 * </p>
 * <p>
 * An azimuth-elevation mask defines the physical horizon for a local point, origin of some topocentric frame.
 * </p>
 * <p>
 * Azimuth is defined according to
 * {@link TopocentricFrame#getAzimuth(fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D, 
 * fr.cnes.sirius.patrius.frames.Frame, fr.cnes.sirius.patrius.time.AbsoluteDate)
 * getAzimuth}. Elevation is defined according to
 * {@link TopocentricFrame#getElevation(fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D, 
 * fr.cnes.sirius.patrius.frames.Frame, fr.cnes.sirius.patrius.time.AbsoluteDate)
 * getElevation}.
 * </p>
 * <p>
 * The azimuth elevation mask must be supplied as a twodimensional array with multiples lines of pairs of
 * azimuth-elevation angles. First row will be filled with azimuth values, second row with elevation values, as in the
 * following snippet:
 * 
 * <pre>
 * double[][] mask = { { FastMath.toRadians(0), FastMath.toRadians(10) },
 *     { FastMath.toRadians(45), FastMath.toRadians(8) },
 *     { FastMath.toRadians(90), FastMath.toRadians(6) },
 *     { FastMath.toRadians(135), FastMath.toRadians(4) },
 *     { FastMath.toRadians(180), FastMath.toRadians(5) },
 *     { FastMath.toRadians(225), FastMath.toRadians(6) },
 *     { FastMath.toRadians(270), FastMath.toRadians(8) },
 *     { FastMath.toRadians(315), FastMath.toRadians(9) } };
 * </pre>
 * 
 * </p>
 * <p>
 * No assumption is made on azimuth values and ordering. The only restraint is that only one elevation value can be
 * associated to identical azimuths modulo 2PI.
 * </p>
 * <p>
 * The default implementation behavior is to {@link EventDetector.Action#CONTINUE continue} propagation at raising and
 * to {@link EventDetector.Action#STOP stop} propagation at setting. This can be changed by using provided constructors.
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
public class GroundMaskElevationDetector extends AbstractDetector {

    /** Serializable UID. */
    private static final long serialVersionUID = -8124322408349693773L;

    /** Azimuth-elevation mask. */
    private final double[][] azelmask;

    /** Topocentric frame in which azimuth and elevation should be evaluated. */
    private final TopocentricFrame topo;

    /**
     * Build a new azimuth-elevation detector.
     * <p>
     * This simple constructor takes default values for maximal checking interval ( {@link #DEFAULT_MAXCHECK}) and
     * convergence threshold ({@link #DEFAULT_THRESHOLD}).
     * </p>
     * 
     * @param azimelev azimuth-elevation mask (rad)
     * @param topoIn topocentric frame in which elevation should be evaluated
     * @exception IllegalArgumentException if azimuth-elevation mask is not supported
     */
    public GroundMaskElevationDetector(final double[][] azimelev, final TopocentricFrame topoIn) {
        this(azimelev, topoIn, DEFAULT_MAXCHECK, DEFAULT_THRESHOLD);
    }

    /**
     * Build a new azimuth-elevation detector.
     * <p>
     * This constructor takes default value for convergence threshold ({@link #DEFAULT_THRESHOLD}).
     * </p>
     * <p>
     * The maximal interval between elevation checks should be smaller than the half duration of the minimal pass to
     * handle, otherwise some short passes could be missed.
     * </p>
     * 
     * @param azimelev azimuth-elevation mask (rad)
     * @param topoIn topocentric frame in which elevation should be evaluated
     * @param maxCheck maximal checking interval (s)
     * @exception IllegalArgumentException if azimuth-elevation mask is not supported
     */
    public GroundMaskElevationDetector(final double[][] azimelev, final TopocentricFrame topoIn,
        final double maxCheck) {
        this(azimelev, topoIn, maxCheck, DEFAULT_THRESHOLD);
    }

    /**
     * Build a new azimuth-elevation detector.
     * <p>
     * The maximal interval between elevation checks should be smaller than the half duration of the minimal pass to
     * handle, otherwise some short passes could be missed.
     * </p>
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#CONTINUE continue} propagation at raising
     * and to {@link EventDetector.Action#STOP stop} propagation at setting.
     * </p>
     * 
     * @param azimelev azimuth-elevation mask (rad)
     * @param topoIn topocentric frame in which elevation should be evaluated
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @exception IllegalArgumentException if azimuth-elevation mask is not supported
     */
    public GroundMaskElevationDetector(final double[][] azimelev, final TopocentricFrame topoIn,
        final double maxCheck, final double threshold) {
        this(azimelev, topoIn, maxCheck, threshold, Action.CONTINUE, Action.STOP);
    }

    /**
     * Build a new azimuth-elevation detector.
     * <p>
     * The maximal interval between elevation checks should be smaller than the half duration of the minimal pass to
     * handle, otherwise some short passes could be missed.
     * </p>
     * 
     * @param azimelev azimuth-elevation mask (rad)
     * @param topoIn topocentric frame in which elevation should be evaluated
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param raising action performed when propagation at raising
     * @param setting action performed when propagation at setting
     * @exception IllegalArgumentException if azimuth-elevation mask is not supported
     */
    public GroundMaskElevationDetector(final double[][] azimelev, final TopocentricFrame topoIn,
        final double maxCheck, final double threshold, final Action raising,
        final Action setting) {
        this(azimelev, topoIn, maxCheck, threshold, raising, setting, false, false);
    }

    /**
     * Build a new azimuth-elevation detector.
     * <p>
     * The maximal interval between elevation checks should be smaller than the half duration of the minimal pass to
     * handle, otherwise some short passes could be missed.
     * </p>
     * 
     * @param azimelev azimuth-elevation mask (rad)
     * @param topoIn topocentric frame in which elevation should be evaluated
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param raising action performed when propagation at raising
     * @param setting action performed when propagation at setting
     * @param removeRaising if detector should be removed at raising
     * @param removeSetting if detector should be removed at setting
     * @exception IllegalArgumentException if azimuth-elevation mask is not supported
     */
    public GroundMaskElevationDetector(final double[][] azimelev, final TopocentricFrame topoIn,
        final double maxCheck, final double threshold, final Action raising,
        final Action setting, final boolean removeRaising, final boolean removeSetting) {
        super(maxCheck, threshold, raising, setting, removeRaising, removeSetting);
        this.azelmask = checkMask(azimelev);
        this.topo = topoIn;
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
     * Handle an azimuth-elevation event and choose what to do next.
     * 
     * @param s the current state information : date, kinematics, attitude
     * @param increasing if true, the value of the switching function increases when times increases
     *        around event
     * @param forward if true, the integration variable (time) increases during integration.
     * @return the action performed when propagation raising or setting.
     * @exception PatriusException if some specific error occurs
     */
    @Override
    public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                final boolean forward) throws PatriusException {
        return super.eventOccurred(s, increasing, forward);
    }

    /**
     * {@inheritDoc}
     * 
     * Compute the value of the switching function. This function measures the difference between
     * the current elevation and the elevation for current azimuth interpolated from
     * azimuth-elevation mask.
     */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public double g(final SpacecraftState state) throws PatriusException {
        // Emitter is the satellite, station is the receiver (since elevation is wrt to station)
        final AbsoluteDate recDate = getSignalReceptionDate(this.topo, state.getOrbit(), state.getDate());
        final double azimuth = this.topo.getAzimuth(state.getPVCoordinates().getPosition(), state.getFrame(), recDate);
        return this.topo.getElevation(state.getPVCoordinates().getPosition(), state.getFrame(), recDate)
                - this.getElevation(azimuth);
    }

    /**
     * Get the interpolated elevation for a given azimuth according to the mask.
     * 
     * @param azimuth azimuth (rad)
     * @return elevation angle (rad)
     */
    public double getElevation(final double azimuth) {
        // Initialization
        double elevation = 0.0;
        boolean fin = false;
        // Loop on elements of Azimuth-elevation mask until the one corresponding to azimuth is found
        for (int i = 1; i < this.azelmask.length & !fin; i++) {
            if (azimuth <= this.azelmask[i][0]) {
                // the correct mask has been found
                fin = true;
                final double azd = this.azelmask[i - 1][0];
                final double azf = this.azelmask[i][0];
                final double eld = this.azelmask[i - 1][1];
                final double elf = this.azelmask[i][1];
                // Compute elevation for azimuth
                elevation = eld + (azimuth - azd) * (elf - eld) / (azf - azd);
            }
        }
        return elevation;
    }
    
    /** @inheritDoc */
    @Override
    public void setPropagationDelayType(final PropagationDelayType propagationDelayType, final Frame frame) {
        super.setPropagationDelayType(propagationDelayType, frame);
    }
    
    /**
     * Checking and ordering the azimuth-elevation tabulation.
     * 
     * @param azimelev azimuth-elevation tabulation to be checked and ordered
     * @return ordered azimuth-elevation tabulation ordered
     */
    private static double[][] checkMask(final double[][] azimelev) {

        /* Copy of the given mask */
        final double[][] mask = new double[azimelev.length + 2][azimelev[0].length];
        for (int i = 0; i < azimelev.length; i++) {
            System.arraycopy(azimelev[i], 0, mask[i + 1], 0, azimelev[i].length);
            /* Reducing azimuth between 0 and 2*Pi */
            mask[i + 1][0] = MathUtils.normalizeAngle(mask[i + 1][0], FastMath.PI);
        }

        /* Sorting the mask with respect to azimuth */
        Arrays.sort(mask, 1, mask.length - 1, new Comparator<double[]>(){
            /** {@inheritDoc} */
            @Override
            public int compare(final double[] d1, final double[] d2) {
                return Double.compare(d1[0], d2[0]);
            }
        });

        /* Extending the mask in order to cover [0, 2PI] in azimuth */
        mask[0][0] = mask[mask.length - 2][0] - MathUtils.TWO_PI;
        mask[0][1] = mask[mask.length - 2][1];
        mask[mask.length - 1][0] = mask[1][0] + MathUtils.TWO_PI;
        mask[mask.length - 1][1] = mask[1][1];

        /* Checking the sorted mask: same azimuth modulo 2PI must have same elevation */
        for (int i = 1; i < mask.length; i++) {
            if (Double.compare(mask[i - 1][0], mask[i][0]) == 0) {
                if (Double.compare(mask[i - 1][1], mask[i][1]) != 0) {
                    // Exception, two elevation values for the same azimuth
                    throw PatriusException.createIllegalArgumentException(
                        PatriusMessages.UNEXPECTED_TWO_ELEVATION_VALUES_FOR_ONE_AZIMUTH,
                        mask[i - 1][1], mask[i][1], mask[i][0]);
                }
            }
        }

        // return mask
        return mask;
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
        final double[][] azelmaskNew = new double[this.azelmask.length][];
        for (int i = 0; i < this.azelmask.length; i++) {
            azelmaskNew[i] = this.azelmask[i].clone();
        }
        final GroundMaskElevationDetector res = new GroundMaskElevationDetector(azelmaskNew, this.topo,
            this.getMaxCheckInterval(), this.getThreshold(), this.getActionAtEntry(), this.getActionAtExit(),
            this.isRemoveAtEntry(), this.isRemoveAtExit());
        res.setPropagationDelayType(getPropagationDelayType(), getInertialFrame());
        return res;
    }
}
