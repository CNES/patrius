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
 * HISTORY
 * VERSION:4.13:DM:DM-70:08/12/2023:[PATRIUS] Calcul de jacobienne dans OneAxisEllipsoid
 * VERSION:4.12.1:FA:FA-123:05/09/2023:[PATRIUS] Utilisation de getLLHCoordinates() au
 * lieu de getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC)
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11.1:FA:FA-72:30/06/2023:[PATRIUS] Mauvaise prise en compte du MeteoConditionProvider dans les
 * AbstractTropoFactory
 * VERSION:4.11:DM:DM-3295:22/05/2023:[PATRIUS] Conditions meteorologiques variables dans modeles troposphere
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.signalpropagation.troposphere;

import fr.cnes.sirius.patrius.bodies.LLHCoordinatesSystem;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;

/**
 * This class describes the tropospheric correction factory around the {@link MariniMurrayModel Marini Murray model}.
 *
 * @author bonitt
 */
public class MariniMurrayModelFactory extends AbstractMeteoBasedCorrectionFactory<MariniMurrayModel> {

    /** Wavelength of radiation [nm]. */
    private final double lambda;

    /**
     * Marini Murray model factory constructor.
     *
     * @param lambda
     *        Wavelength of radiation [nm]
     * @throws NotStrictlyPositiveException
     *         if {@code lambda <= 0}
     */
    public MariniMurrayModelFactory(final double lambda) {
        super();

        if (lambda <= 0.) {
            throw new NotStrictlyPositiveException(lambda);
        }
        this.lambda = lambda;
    }

    /** {@inheritDoc} */
    @Override
    protected MariniMurrayModel buildMeteoBasedCorrection(final MeteoBasedKey key) {
        return new MariniMurrayModel(key.getMeteoConditionsProvider(),
            key.getPoint().getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getLatitude(), this.lambda,
            key.getPoint().getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getHeight());
    }
}
