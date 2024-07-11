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
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.signalpropagation.SignalPropagationModel.FixedDate;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

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
    
    /** The signal propagation direction */
    public enum SignalDirection {
        /** The target object is the signal emitter. */
        FROM_TARGET,
        /** The target object is the signal receiver. */
        TOWARD_TARGET;
    }

    /**
     * Provides the target point at a given date in a given frame, represented by the
     * associated PVCoordinates object
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
     * Provides the direction vector taking into account the delay of signal propagation. Both objects can be the signal
     * emitter or the signal receiver. The {@link SignalDirection} provides the signal propagation direction. Regardless
     * to the propagation direction, the output vector is oriented from the origin object to the target object.
     * Moreover, the {@link FixedDate} indicates whether the reference date is the signal emission or the signal
     * reception. To perform the signal propagation, the target inertial frame (or the input frame, if it is not
     * defined) is frozen with respect to the ICRF frame.
     * <p>
     * <b><u>Warning:</u> The returned direction vector is not corrected for the stellar aberration, only a light-time
     * correction is applied, see <a
     * href="https://naif.jpl.nasa.gov/pub/naif/toolkit_docs/C/req/abcorr.html#Types%20of%20Corrections">aberration
     * corrections</a>.</b>
     * </p>
     * 
     * @param origin
     *        the object that set the origin of the output vector.
     * @param signalDirection
     *        indicates the signal propagation direction.
     * @param date
     *        the reference date.
     * @param fixedDateType
     *        indicates whether the reference date is a signal emission or reception.
     * @param frame
     *        the frame to compute the direction vector. To perform the signal propagation, the target inertial frame
     *        (or the input frame, if it is not defined) is frozen with respect to the ICRF frame.
     * @param epsilon
     *        the time tolerance for convergence.
     * 
     * @return the light-time corrected direction vector (from origin to target) at the given date in the given frame
     * @throws PatriusException
     *         if the input frame is not inertial.
     */
    Vector3D getVector(PVCoordinatesProvider origin, SignalDirection signalDirection,
                       AbsoluteDate date, FixedDate fixedDateType, Frame frame,
                       double epsilon)
        throws PatriusException;

    /**
     * Provides the line connecting both objects and taking into account the delay of signal propagation. Both objects
     * can be the signal emitter or the signal receiver. The {@link SignalDirection} provides the signal propagation
     * direction. Moreover, the {@link FixedDate} indicates whether the reference date is the signal emission or the
     * signal reception. To perform the signal propagation, the target inertial frame (or the input frame, if it is not
     * defined) is frozen with respect to the ICRF frame.
     * <p>
     * <b><u>Warning:</u> The returned direction line is not corrected for the stellar aberration, only a light-time
     * correction is applied, see <a
     * href="https://naif.jpl.nasa.gov/pub/naif/toolkit_docs/C/req/abcorr.html#Types%20of%20Corrections">aberration
     * corrections</a>.</b>
     * </p>
     * 
     * @param origin
     *        the object that set the origin of the output line.
     * @param signalDirection
     *        indicates the signal propagation direction.
     * @param date
     *        the reference date.
     * @param fixedDateType
     *        indicates whether the reference date is a signal emission or reception.
     * @param frame
     *        the frame to compute the direction vector.To perform the signal propagation, the target inertial frame (or
     *        the input frame, if it is not defined) is frozen with respect to the ICRF frame.
     * @param epsilon
     *        the time tolerance for convergence.
     * 
     * @return the half Line of space at the given date in the given frame:
     *         <ul>
     *         <li>if RECEPTION date FROM_TARGET: half Line from the origin position at fixed date in the light-time
     *         corrected origin-to-target direction;</li>
     *         <li>if RECEPTION date TOWARD_TARGET: half Line from the target position at fixed date in the light-time
     *         corrected target-to-origin direction;</li>
     *         <li>if EMISSION date TOWARD_TARGET: half Line from the origin position at fixed date in the light-time
     *         corrected origin-to-target direction;</li>
     *         <li>if EMISSION date FROM_TARGET: half Line from the target position at fixed date in the light-time
     *         corrected target-to-origin direction.</li>
     *         </ul>
     * @throws PatriusException if the input frame is not inertial.
     */
    Line getLine(PVCoordinatesProvider origin, SignalDirection signalDirection,
                 AbsoluteDate date, FixedDate fixedDateType, Frame frame,
                 double epsilon)
        throws PatriusException;
    
    /**
     * Provides the {@link PVCoordinatesProvider} associated to the target object.
     * 
     * @return the PV coordinates provider of the target
     */
    PVCoordinatesProvider getTargetPvProvider();
}
