/**
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
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2872:18/05/2021:Calcul de l'accélération dans la classe QuaternionPolynomialProfile 
 * VERSION:4.7:DM:DM-2653:18/05/2021:generalisation des sequences et correction/refonte des sequences de segments 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1949:14/11/2018:add new orientation feature
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes.orientations;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This interface represents an orientation angle provider model set. An orientation angle provider
 * provides a way to compute an angle from a date and position-velocity local provider.
 *
 * @author morerem
 *
 * @since 4.2
 */
public interface OrientationAngleProvider extends Serializable {

    /**
     * Compute the orientation angle corresponding to an orbital state.
     * 
     * @param pvProv position-velocity provider around current date
     * @param date date
     * @return orientation angle at the specified date and position-velocity state, null if outside interval
     * @throws PatriusException thrown if the angle cannot be computed
     */
    public Double getOrientationAngle(final PVCoordinatesProvider pvProv,
            final AbsoluteDate date) throws PatriusException;

    /**
     * Compute the orientation derivative at a given date by finite differences (using 2nd order centered finite
     * differences).
     *
     * @param pvProv
     *        PV Coordinates Provider.
     * @param date
     *        Date at which the orientation derivative is desired.
     * @param computationStep
     *        Computation step used for the finite differences (in seconds).
     * @return The orientation derivative of the satellite
     * @throws PatriusException
     *         When the orientation cannot be computed
     */
    default double computeSpinByFD(final PVCoordinatesProvider pvProv,
            final AbsoluteDate date,
            final double computationStep) throws PatriusException {
        // Standard case without time boundaries: 2nd order centered difference
        final double orientation1 = getOrientationAngle(pvProv, date.shiftedBy(-computationStep / 2.));
        final double orientation2 = getOrientationAngle(pvProv, date.shiftedBy(computationStep / 2.));

        // Compute derivative by finite differences
        return computeSpinNumerical(orientation1, orientation2, computationStep);
    }

    /**
     * Compute the orientation acceleration vector at a given date by finite differences
     * (using 2nd order centered finite differences).
     *
     * @param pvProv
     *        PV Coordinates Provider.
     * @param date
     *        Date at which the orientation acceleration is desired.
     * @param computationStep
     *        Computation step used for the finite differences (in seconds).
     * @return The orientation acceleration of the satellite
     * @throws PatriusException
     *         When the orientation cannot be computed
     */
    default double computeSpinDerivativeByFD(final PVCoordinatesProvider pvProv,
            final AbsoluteDate date,
            final double computationStep) throws PatriusException {
        // Standard case without time boundaries: 2nd order centered difference
        final double spin1 = computeSpinByFD(pvProv, date.shiftedBy(-computationStep / 2.), computationStep);
        final double spin2 = computeSpinByFD(pvProv, date.shiftedBy(computationStep / 2.), computationStep);

        // Compute derivative by finite differences
        return (spin2 - spin1) / computationStep;
    }

    /**
     * Computes the spin as a finite difference given two angles and the computation step between them.
     * <p>
     * <b>WARNING</b> : It is considered that the difference between the two angle points used for the finite difference
     * spin computation is never larger than &pi; in the sense of the rotation
     *
     * @param angle1
     *        Angle at t<sub>1</sub>
     * @param angle2
     *        Angle at t<sub>2</sub>
     * @param step
     *        Computation step, elapsed time between t<sub>1</sub> and t<sub>2</sub>
     *
     * @return the Spin computed as finite difference
     */
    public static double computeSpinNumerical(final double angle1,
            final double angle2,
            final double step) {
        // Check if there is a discontinuity around 2PI, considering that the difference between two points used for the
        // finite difference is never larger than PI
        double angle = angle2;
        if ((angle1 - angle2) > MathLib.PI) {
            angle = angle2 + MathUtils.TWO_PI;
        }
        return (angle - angle1) / step;
    }
}
