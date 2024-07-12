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
 *
 * @history created 11/03/2013
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:FA:FA-2440:27/05/2020:difference finie en debut de segment QuaternionPolynomialProfile 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes.kinematics;

import fr.cnes.sirius.patrius.math.analysis.UnivariateVectorFunction;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3DFunction;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This interface is a time-dependent function representing a generic orientation.
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id: OrientationFunction.java 18065 2017-10-02 16:42:02Z bignon $
 * 
 * @since 1.3
 * 
 */
public interface OrientationFunction extends UnivariateVectorFunction {

    /**
     * Get the orientation at a given date. <br>
     * The orientation is an instance of the {@link Rotation} class.
     * 
     * @param date
     *        the date
     * @return the orientation at a given date.
     * @throws PatriusException
     *         if orientation cannot be computed
     */
    Rotation getOrientation(final AbsoluteDate date) throws PatriusException;

    /**
     * Estimate the {@link Vector3DFunction} from the current {@link OrientationFunction} using the
     * {@link fr.cnes.sirius.patrius.utils.AngularCoordinates#estimateRate(Rotation, Rotation, double)} method.
     * 
     * @param dt
     *        time elapsed between the dates of the two orientations
     * @param interval validity interval of the function (necessary for handling derivatives at boundaries)
     * @return the spin function.
     */
    Vector3DFunction estimateRateFunction(final double dt, final AbsoluteDateInterval interval);

    /**
     * Compute the {@link OrientationFunction} representing the first derivative of the current orientation function
     * components.<br>
     * The derivation can be analytical or numerical, depending on the current orientation function.
     * 
     * @return a new {@link OrientationFunction} containing the first derivative of the orientation function components.
     */
    OrientationFunction derivative();

}
