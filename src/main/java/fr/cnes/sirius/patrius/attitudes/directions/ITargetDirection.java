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
 * @history creation 30/11/2011
 *
 * HISTORY
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11:DM:DM-14:22/05/2023:[PATRIUS] Nombre max d'iterations dans le calcul de la propagation du signal 
 * VERSION:4.11:DM:DM-3303:22/05/2023:[PATRIUS] Modifications mineures dans UserCelestialBody 
 * VERSION:4.11:DM:DM-3318:22/05/2023:[PATRIUS] Besoin de forcer normalisation QuaternionPolynomialSegment
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration de la gestion des attractions gravitationnelles
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:FA:FA-3201:03/11/2022:[PATRIUS] Prise en compte de l'aberration stellaire dans ITargetDirection
 * VERSION:4.9.1:FA:FA-3190:01/06/2022:[PATRIUS] Preciser la javadoc des methodes
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3165:10/05/2022:[PATRIUS] Amelioration de la propagation du signal 
 * VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
 * VERSION:4.9:DM:DM-3127:10/05/2022:[PATRIUS] Ajout de deux methodes resize a l'interface GeometricBodyShape ...
 * VERSION:4.9:DM:DM-3147:10/05/2022:[PATRIUS] Ajout a l'interface ITargetDirection d'une methode getTargetPvProv ...
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:227:09/04/2014:Merged eclipse detectors
 * VERSION::FA:261:13/10/2014:JE V2.2 corrections (move IDirection in package attitudes.directions)
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.attitudes.directions;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.signalpropagation.VacuumSignalPropagation;
import fr.cnes.sirius.patrius.signalpropagation.VacuumSignalPropagationModel;
import fr.cnes.sirius.patrius.signalpropagation.VacuumSignalPropagationModel.FixedDate;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;

/**
 * This interface extends Directions for the directions
 * described by a target point.
 * 
 * @see IDirection#getVector(PVCoordinatesProvider, AbsoluteDate, Frame)
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: ITargetDirection.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.1
 * 
 */
public interface ITargetDirection extends IDirection {

    /** The signal propagation direction. */
    public enum SignalDirection {
        /** The target object is the signal emitter. */
        FROM_TARGET,
        /** The target object is the signal receiver. */
        TOWARD_TARGET;
    }

    /**
     * Enumerate for the aberration corrections to apply, see
     * <a href="https://naif.jpl.nasa.gov/pub/naif/toolkit_docs/C/req/abcorr.html#Types%20of%20Corrections"> aberration
     * corrections</a> for more details about corrections <i>CN</i>, <i>CN+S</i>, <i>XCN</i> and <i>XCN+S</i> referred
     * here under.
     **/
    public enum AberrationCorrection {
        /**
         * Apply no corrections at all (the signal propagation is considered instantaneous).
         */
        NONE(false, false),
        /**
         * Apply light-time aberration correction:
         * <ul>
         * <li>From the <i>receiver</i> point of view (<a
         * href="https://naif.jpl.nasa.gov/pub/naif/toolkit_docs/C/req/abcorr.html#Types%20of%20Corrections">CN</a>
         * value), the source is seen where it was when light departed from it (taking into account the apparent
         * displacement of the source during the propagation of the light arriving from it),</li>
         * <li>From the <i>transmitter</i> point of view (<a
         * href="https://naif.jpl.nasa.gov/pub/naif/toolkit_docs/C/req/abcorr.html#Types%20of%20Corrections">XCN</a>
         * value), the light time aberration correction yields the observer's location as it will be when photons
         * emitted from the source's location arrive to it (taking into account the displacement of the observer during
         * the time of propagation).<br>
         * </ul>
         */
        LIGHT_TIME(true, false),
        /**
         * Apply stellar aberration corrections:
         * <ul>
         * <li>From the <i>receiver</i> point of view, the resulting position vector indicates where the source
         * "appears to be" from the observer's location (taking into account the observer's velocity at reception time),
         * </li>
         * <li>From the <i>transmitter</i> point of view, indicates the direction from the source's location in which
         * radiation should be emitted to reach the observer (taking into account the source's velocity at transmission
         * time).<br>
         * </ul>
         */
        STELLAR(false, true),
        /**
         * Apply light-time and stellar aberration corrections:
         * <ul>
         * <li>From the <i>receiver</i> point of view (<a
         * href="https://naif.jpl.nasa.gov/pub/naif/toolkit_docs/C/req/abcorr.html#Types%20of%20Corrections">CN+S</a>
         * value), the resulting position vector indicates where the source "appears to be" from the observer's location
         * (taking into account both the apparent displacement of the source during the propagation of the light
         * arriving from it, and the observer's velocity at reception time),</li>
         * <li>From the <i>transmitter</i> point of view (<a
         * href="https://naif.jpl.nasa.gov/pub/naif/toolkit_docs/C/req/abcorr.html#Types%20of%20Corrections">XCN+S</a>
         * value), indicates the direction from the source's location in which radiation should be emitted to reach the
         * observer (taking into account both the apparent displacement of the observer during the propagation of the
         * light transmitted to it, and the source's velocity at transmission time).<br>
         * </ul>
         */
        ALL(true, true);
        
        /** Includes light-time correction. */
        private final boolean includeLightTime;

        /** Includes stellar aberration correction. */
        private final boolean includeStellarAberration;

        /**
         * Constructor.
         * @param includeLightTime includes light-time correction
         * @param includeStellarAberration includes stellar aberration correction
         */
        private AberrationCorrection(final boolean includeLightTime, final boolean includeStellarAberration) {
            this.includeLightTime = includeLightTime;
            this.includeStellarAberration = includeStellarAberration;
        }
        
        /**
         * Returns true if light-time is included.
         * @return true if light-time is included, false otherwise
         */
        public boolean hasLightTime() {
            return includeLightTime;
        }

        /**
         * Returns true if stellar aberration is included.
         * @return true if stellar aberration is included, false otherwise
         */
        public boolean hasStellarAberration() {
            return includeStellarAberration;
        }
    }

    /**
     * Provides the target point at a given date in a given frame, represented by the
     * associated PVCoordinates object.
     * 
     * @param date
     *        the date
     * @param frame
     *        the frame
     * @return the PVCoordinates of the target
     * @exception PatriusException
     *            if position cannot be computed in given frame
     */
    PVCoordinates getTargetPVCoordinates(AbsoluteDate date, Frame frame) throws PatriusException;

    /**
     * Provides the direction vector (from origin to target) at entered date, taking into account the type of date
     * (emission or reception), corrected for light-time and stellar aberration pending the entered <i>correction</i>
     * parameter.
     * <p>
     * Nota: if entered <i>correction</i> is {@link AberrationCorrection#NONE NONE}, invoking this method is equivalent
     * to invoking the method {@link ITargetDirection#getVector(PVCoordinatesProvider, AbsoluteDate, Frame)}.
     * </p>
     *
     * @param origin
     *        the current coordinates of the origin point of the direction (may be null, in that specific case, the
     *        origin of the direction is the frame origin).
     * @param signalDirection
     *        the signal direction
     * @param correction
     *        the type of correction for light-time and stellar aberration ({@link AberrationCorrection#NONE NONE},
     *        {@link AberrationCorrection#LIGHT_TIME LT}, {@link AberrationCorrection#STELLAR S}, or
     *        {@link AberrationCorrection#ALL LT_S}, see <a
     *        href="https://naif.jpl.nasa.gov/pub/naif/toolkit_docs/C/req/abcorr.html#Types%20of%20Corrections">
     *        aberration corrections</a>). In case of {@link AberrationCorrection#STELLAR S} or of
     *        {@link AberrationCorrection#ALL LT_S} correction, the stellar aberration is corrected at reception if
     *        {@link FixedDate#RECEPTION RECEPTION} date type or at transmission if {@link FixedDate#EMISSION EMISSION}
     *        date type.
     * @param date
     *        the date
     * @param fixedDateType
     *        the type of the previous given date : emission or reception
     * @param frame
     *        the frame to project the vector coordinates
     * @param epsilon
     *        the absolute duration threshold used for convergence of signal propagation computation. The epsilon is a
     *        time absolute error (ex: 1E-14s, in this case, the signal distance accuracy is 1E-14s x 3E8m/s = 3E-6m).
     * @return the direction vector (from origin to target) at the given date in the given frame
     * @exception PatriusException
     *            if some frame specific errors occur
     */
    public Vector3D getVector(final PVCoordinatesProvider origin, final SignalDirection signalDirection,
                              final AberrationCorrection correction, final AbsoluteDate date,
                              final FixedDate fixedDateType, final Frame frame, final double epsilon)
        throws PatriusException;

    /**
     * Provides the half line containing both origin and target, taking into account the type of date (emission or
     * reception), corrected for light-time and stellar aberration pending the entered <i>correction</i> parameter.
     * <p>
     * Nota: if entered <i>correction</i> is {@link AberrationCorrection#NONE NONE}, invoking this method is not fully
     * equivalent to invoking the method {@link ITargetDirection#getLine(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * due to orientation of the half line (from the origin position at entered date in the target direction, or from
     * the target position at entered date in the origin direction).
     * </p>
     *
     * @param origin
     *        the current coordinates of the origin point of the direction (may be null, in that specific case, the
     *        origin of the direction is the frame origin).
     * @param signalDirection
     *        the signal direction
     * @param correction
     *        the type of correction for light-time and stellar aberration ({@link AberrationCorrection#NONE NONE},
     *        {@link AberrationCorrection#LIGHT_TIME LT}, {@link AberrationCorrection#STELLAR S}, or
     *        {@link AberrationCorrection#ALL LT_S}, see <a
     *        href="https://naif.jpl.nasa.gov/pub/naif/toolkit_docs/C/req/abcorr.html#Types%20of%20Corrections">
     *        aberration corrections</a>). In case of {@link AberrationCorrection#STELLAR S} or of
     *        {@link AberrationCorrection#ALL LT_S} correction, the stellar aberration is corrected at reception if
     *        {@link FixedDate#RECEPTION RECEPTION} date type or at transmission if {@link FixedDate#EMISSION EMISSION}
     *        date type.
     * @param date
     *        the date for origin
     * @param fixedDateType
     *        the type of the previous given date : emission or reception
     * @param frame
     *        the frame to project the line
     * @param epsilon
     *        the absolute duration threshold used for convergence of signal propagation computation. The epsilon is a
     *        time absolute error (ex: 1E-14s, in this case, the signal distance accuracy is 1E-14s x 3E8m/s = 3E-6m).
     * @return the half line of space, at the given date in the given frame, corrected for the given aberration:
     *         <ul>
     *         <li>if RECEPTION date FROM_TARGET: half line from the origin position at fixed date, in the target
     *         apparent direction;</li>
     *         <li>if RECEPTION date TOWARD_TARGET: half line from the target position at fixed date, in the origin
     *         apparent direction;</li>
     *         <li>if EMISSION date TOWARD_TARGET: half line from the origin position at fixed date, in the target
     *         corrected direction;</li>
     *         <li>if EMISSION date FROM_TARGET: half line from the target position at fixed date, in the origin
     *         corrected direction.</li>
     *         </ul>
     * @throws PatriusException if some frame specific errors occur
     */
    public Line getLine(final PVCoordinatesProvider origin, final SignalDirection signalDirection,
                        final AberrationCorrection correction, final AbsoluteDate date, final FixedDate fixedDateType,
                        final Frame frame, final double epsilon)
        throws PatriusException;

    /**
     * Provides the {@link PVCoordinatesProvider} associated to the target object.
     * 
     * @return the PV coordinates provider of the target
     */
    public PVCoordinatesProvider getTargetPvProvider();

    /**
     * Provides the direction vector (from origin to target) at entered date, taking into account the type of date
     * (emission or reception), corrected for light-time and stellar aberration pending the entered <i>correction</i>
     * parameter.
     * <p>
     * Nota: if entered <i>correction</i> is {@link AberrationCorrection#NONE NONE}, invoking this method is equivalent
     * to invoking the method {@link ITargetDirection#getVector(PVCoordinatesProvider, AbsoluteDate, Frame)}.
     * </p>
     *
     * @param origin
     *        the current coordinates of the origin point of the direction (may be null, in that specific case, the
     *        origin of the direction is the frame origin).
     * @param target
     *        the target
     * @param signalDirection
     *        the signal direction
     * @param correction
     *        the type of correction for light-time and stellar aberration ({@link AberrationCorrection#NONE NONE},
     *        {@link AberrationCorrection#LIGHT_TIME LT}, {@link AberrationCorrection#STELLAR S}, or
     *        {@link AberrationCorrection#ALL LT_S}, see <a
     *        href="https://naif.jpl.nasa.gov/pub/naif/toolkit_docs/C/req/abcorr.html#Types%20of%20Corrections">
     *        aberration corrections</a>). In case of {@link AberrationCorrection#STELLAR S} or of
     *        {@link AberrationCorrection#ALL LT_S} correction, the stellar aberration is corrected at reception if
     *        {@link FixedDate#RECEPTION RECEPTION} date type or at transmission if {@link FixedDate#EMISSION EMISSION}
     *        date type.
     * @param date
     *        the date
     * @param fixedDateType
     *        the type of the previous given date : emission or reception
     * @param frame
     *        the frame to project the vector coordinates
     * @param epsilon
     *        the absolute duration threshold used for convergence of signal propagation computation. The epsilon is a
     *        time absolute error (ex: 1E-14s, in this case, the signal distance accuracy is 1E-14s x 3E8m/s = 3E-6m).
     * @param referenceFrame reference frame for geometric computation
     * @return the direction vector (from origin to target) at the given date in the given frame
     * @exception PatriusException
     *            if some frame specific errors occur
     */
    default Vector3D getVector(final PVCoordinatesProvider origin,
            final PVCoordinatesProvider target,
            final SignalDirection signalDirection,
            final AberrationCorrection correction,
            final AbsoluteDate date,
            final FixedDate fixedDateType,
            final Frame frame,
            final double epsilon,
            final Frame referenceFrame) throws PatriusException {
        // PV provider of the emitter and receiver
        final PVCoordinatesProvider emitter;
        final PVCoordinatesProvider receiver;
        // Factor related to the signal direction and useful to determine the direction vector
        final double dirFactorSignalDirection;
        switch (signalDirection) {
            case FROM_TARGET :
                emitter = target;
                receiver = origin;
                dirFactorSignalDirection = -1.;
                break;
            case TOWARD_TARGET :
                emitter = origin;
                receiver = target;
                dirFactorSignalDirection = 1.;
                break;
            default:
                // Cannot happen
                throw new PatriusRuntimeException(PatriusMessages.INTERNAL_ERROR, null);
        }

        // Initialize the direction vector (from emitter to receiver)
        Vector3D dirVector = null;
        // Compute the light-time-dependent direction if apply
        if (correction.hasLightTime()) {
            // freeze the input frame at the origin date with respect to itself
            final Frame frozenFrame = referenceFrame.getFrozenFrame(FramesFactory.getICRF(), date, "InertialFrozen");
            // Create the signal propagation model taking into account the light speed
            final VacuumSignalPropagationModel model = new VacuumSignalPropagationModel(frozenFrame, epsilon,
                    VacuumSignalPropagationModel.DEFAULT_MAX_ITER);
            // Compute the result of the signal propagation
            final VacuumSignalPropagation signal = model.computeSignalPropagation(emitter, receiver, date,
                fixedDateType);
            // Get the light-time corrected direction vector
            dirVector = signal.getVector(frame);
        } else {
            // Compute the uncorrected direction vector
            dirVector = this.getVector(origin, date, frame).scalarMultiply(dirFactorSignalDirection);
        }

        // Apply stellar aberration correction if required
        if (correction.hasStellarAberration()) {
            if (fixedDateType.equals(FixedDate.EMISSION)) {
                // Fixed date is emission date
                // Apply stellar aberration correction to the direction vector
                dirVector = StellarAberrationCorrection.applyInverseTo(emitter, dirVector, frame,
                    date);
            } else {
                // Fixed date is reception date
                // Apply stellar aberration correction to the direction vector
                dirVector = StellarAberrationCorrection.applyTo(receiver, dirVector.negate(), frame,
                    date).negate();
            }
        }

        return dirVector.scalarMultiply(dirFactorSignalDirection);
    }

}
