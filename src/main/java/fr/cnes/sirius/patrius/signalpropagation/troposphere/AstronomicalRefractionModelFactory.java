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
 * VERSION:4.13:DM:DM-120:08/12/2023:[PATRIUS] Merge de la branche patrius-for-lotus dans Patrius
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.signalpropagation.troposphere;

import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;

/**
 * This class describes the {@link AstronomicalRefractionModel} correction factory.
 *
 * @author veuillh
 * 
 * @since 4.13
 */
public class AstronomicalRefractionModelFactory extends
    AbstractMeteoBasedCorrectionFactory<AstronomicalRefractionModel> {

    /** The wavelength of the signal [nanometer]. */
    private final double wavelengthNanometer;

    /**
     * Astronomical refraction model factory constructor.
     *
     * @param wavelengthNanometer
     *        The wavelength of the signal [nanometer]
     * @throws NotStrictlyPositiveException
     *         if {@code wavelengthNanometer <= 0}
     */
    public AstronomicalRefractionModelFactory(final double wavelengthNanometer) {
        super();

        if (wavelengthNanometer <= 0.) {
            throw new NotStrictlyPositiveException(wavelengthNanometer);
        }

        this.wavelengthNanometer = wavelengthNanometer;
    }

    /** {@inheritDoc} */
    @Override
    protected AstronomicalRefractionModel buildMeteoBasedCorrection(final MeteoBasedKey key) {
        return new AstronomicalRefractionModel(key.getPoint(), key.getMeteoConditionsProvider(),
            this.wavelengthNanometer);
    }
}
