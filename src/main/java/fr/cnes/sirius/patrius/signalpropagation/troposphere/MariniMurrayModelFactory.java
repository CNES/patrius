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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.signalpropagation.troposphere;

import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;

/**
 * This class describes the tropospheric correction factory around the {@link MariniMurrayModel Marini Murray model}.
 *
 * @author bonitt
 */
public class MariniMurrayModelFactory extends AbstractTroposphericCorrectionFactory {

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
    protected TroposphericCorrection buildTropoCorrection(final TroposphericCorrectionKey key) {
        return new MariniMurrayModel(key.getPressure(), key.getPoint().getLatitude(), key.getHumidity(),
            key.getTemperature(), this.lambda, key.getPoint().getAltitude());
    }
}
