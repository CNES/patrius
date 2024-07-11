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
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.7:DM:DM-2872:18/05/2021:Calcul de l'accélération dans la classe QuaternionPolynomialProfile 
* VERSION:4.7:DM:DM-2653:18/05/2021:generalisation des sequences et correction/refonte des sequences de segments 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:403:20/10/2015:Improving ergonomic
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.attitudes;

import java.io.Serializable;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This interface represents an attitude provider model set.
 * <p>
 * An attitude provider provides a way to compute an {@link Attitude Attitude} from an date and position-velocity local
 * provider.
 * </p>
 * 
 * @author V&eacute;ronique Pommier-Maurussane
 */
public interface AttitudeProvider extends Serializable {

    /**
     * Compute the attitude corresponding to an orbital state.
     * 
     * @param pvProv
     *        local position-velocity provider around current date
     * @param date
     *        current date
     * @param frame
     *        reference frame from which attitude is computed
     * @return attitude attitude on the specified date and position-velocity state
     * @throws PatriusException
     *         if attitude cannot be computed for provided date
     */
    Attitude getAttitude(final PVCoordinatesProvider pvProv,
            final AbsoluteDate date,
            final Frame frame) throws PatriusException;

    /**
     * Compute the attitude corresponding to an orbital state.
     * 
     * @param orbit
     *        current orbit
     * @return attitude attitude on the current orbit
     * @throws PatriusException
     *         if attitude cannot be computed
     */
    default Attitude getAttitude(final Orbit orbit) throws PatriusException {
        return this.getAttitude(orbit, orbit.getDate(), orbit.getFrame());
    }

    /**
     * Method to activate spin derivative computation.
     * 
     * @param computeSpinDerivatives
     *        true if spin derivatives should be computed
     */
    void setSpinDerivativesComputation(final boolean computeSpinDerivatives);

    /**
     * Compute the rotation rate vector at a given date by finite differences (using 2nd order centered finite
     * differences).
     * <p>
     * Warning: this method calls {@link #getAttitude(PVCoordinatesProvider, AbsoluteDate, Frame)} hence it should not
     * be called by this method.
     * </p>
     *
     * @param pvProv
     *        PV Coordinates Provider.
     * @param frame
     *        Frame of expression of the attitude. The rotation rate will then be relative to this frame.
     * @param date
     *        Date at which the rotation rate vector is desired.
     * @param computationStep
     *        Computation step used for the finite differences (in seconds).
     * @return The rotation rate vector of the satellite with respect to the specified frame, expressed in
     *         satellite frame.
     * @throws PatriusException
     *         When the attitude cannot be computed
     */
    default Vector3D computeSpinByFD(final PVCoordinatesProvider pvProv,
            final Frame frame,
            final AbsoluteDate date,
            final double computationStep) throws PatriusException {
        // Standard case without time boundaries: 2nd order centered difference
        final Attitude attitude1 = getAttitude(pvProv, date.shiftedBy(-computationStep / 2.), frame);
        final Attitude attitude2 = getAttitude(pvProv, date.shiftedBy(computationStep / 2.), frame);

        // Compute derivative by finite differences
        return AngularCoordinates.estimateRate(attitude1.getRotation(), attitude2.getRotation(), computationStep);
    }

    /**
     * Compute the rotation acceleration vector at a given date by finite differences (using 2nd order centered finite
     * differences).
     * <p>
     * Warning: this method calls {@link #getAttitude(PVCoordinatesProvider, AbsoluteDate, Frame)} hence it should not
     * be called by this method.
     * </p>
     *
     * @param pvProv
     *        PV Coordinates Provider.
     * @param frame
     *        Frame of expression of the attitude. The rotation acceleration will then be relative to this frame.
     * @param date
     *        Date at which the rotation acceleration vector is desired.
     * @param computationStep
     *        Computation step used for the finite differences (in seconds).
     * @return The rotation acceleration vector of the satellite with respect to the specified frame, expressed in
     *         satellite frame.
     * @throws PatriusException
     *         When the attitude cannot be computed
     */
    default Vector3D computeSpinDerivativeByFD(final PVCoordinatesProvider pvProv,
            final Frame frame,
            final AbsoluteDate date,
            final double computationStep) throws PatriusException {
        // Standard case without time boundaries: 2nd order centered difference
        final Attitude attitude1 = getAttitude(pvProv, date.shiftedBy(-computationStep / 2.), frame);
        final Attitude attitude2 = getAttitude(pvProv, date.shiftedBy(computationStep / 2.), frame);

        // Compute derivative by finite differences
        return attitude2.getSpin().subtract(attitude1.getSpin()).scalarMultiply(1 / computationStep);
    }
}
