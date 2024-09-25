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
 * HISTORY
 * VERSION:4.13:DM:DM-120:08/12/2023:[PATRIUS] Merge de la branche patrius-for-lotus dans Patrius
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11:DM:DM-3295:22/05/2023:[PATRIUS] Conditions meteorologiques variables dans modeles troposphere
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2929:15/11/2021:[PATRIUS] Harmonisation des modeles de troposphere 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:88:18/11/2013: interface creation
 * VERSION::FA:410:16/04/2015: Anomalies in the Patrius Javadoc
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.signalpropagation;

import fr.cnes.sirius.patrius.math.parameter.IParameterizable;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.signalpropagation.ionosphere.IonosphericCorrection;
import fr.cnes.sirius.patrius.signalpropagation.troposphere.TroposphericCorrection;
import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * This interface is an angular correction model enabling the computation of the satellite
 * elevation angular correction.
 *
 * @see TroposphericCorrection
 * @see IonosphericCorrection
 *
 * @author Tiziana Sabatini
 *
 * @version $Id$
 *
 * @since 2.1
 *
 */
public interface AngularCorrection extends IParameterizable {

    /**
     * Getter for the minimal tolerated apparent elevation for this model (some models cannot compute correction for too
     * low elevations).
     *
     * @return the minimal tolerated apparent elevation [rad]
     */
    double getMinimalToleratedApparentElevation();

    /**
     * Compute the angular correction from the apparent elevation.
     *
     * @param date
     *        The date at which we want to compute the angular correction
     * @param apparentElevation
     *        The apparent elevation (with atmosphere) [rad]
     * @return the elevation correction [rad] so that
     *         {@code apparent_elevation = geometric_elevation + elevation_correction}
     */
    double computeElevationCorrectionFromApparentElevation(final AbsoluteDate date, final double apparentElevation);

    /**
     * Compute the angular correction from the geometric elevation.
     *
     * @param date
     *        The date at which we want to compute the angular correction
     * @param geometricElevation
     *        The geometric elevation (without atmosphere) [rad]
     * @return the elevation correction [rad] so that
     *         {@code apparent_elevation = geometric_elevation + elevation_correction}
     */
    double computeElevationCorrectionFromGeometricElevation(final AbsoluteDate date, final double geometricElevation);

    /**
     * Compute the elevation correction derivative value with respect to the input parameter.
     *
     * @param p
     *        Parameter
     * @param apparentElevation
     *        The apparent elevation (with atmosphere) of the satellite [rad]
     * @return the elevation derivative value
     */
    double derivativeValueFromApparentElevation(final Parameter p, final double apparentElevation);

    /**
     * Compute the elevation correction derivative value with respect to the input parameter.
     *
     * @param p
     *        Parameter
     * @param geometricElevation
     *        The geometric elevation (without atmosphere) of the satellite [rad]
     * @return the elevation derivative value
     */
    double derivativeValueFromGeometricElevation(final Parameter p, final double geometricElevation);

    /**
     * Tell if the function is differentiable by the given parameter.
     *
     * @param p
     *        Parameter
     * @return {@code true} if the function is differentiable by the given parameter
     */
    boolean isDifferentiableBy(final Parameter p);
}
