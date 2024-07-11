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

/**
 * This class describes the tropospheric correction factory around the {@link AzoulayModel Azoulay model}.
 *
 * @author bonitt
 */
public class AzoulayModelFactory extends AbstractTroposphericCorrectionFactory {

    /**
     * True if the correction due to the troposphere is computed from the geometric value of the elevation, to get the
     * apparent elevation value; false if the correction is computed from the apparent value of the elevation (the
     * measure), to get the geometric value.
     */
    private final boolean isGeometricElevation;

    /**
     * Azoulay model factory constructor.
     * <p>
     * By default, the {@link #isGeometricElevation} value is set to {@code true}.
     * </p>
     */
    public AzoulayModelFactory() {
        this(true);
    }

    /**
     * Azoulay model factory constructor.
     * 
     * @param isGeometricElevation
     *        True if the correction due to the troposphere is computed from the geometric value of the elevation, to
     *        get the apparent elevation value; false if the correction is computed from the apparent value of the
     *        elevation (the measure), to get the geometric value.
     */
    public AzoulayModelFactory(final boolean isGeometricElevation) {
        super();
        this.isGeometricElevation = isGeometricElevation;
    }

    /** {@inheritDoc} */
    @Override
    protected TroposphericCorrection buildTropoCorrection(final TroposphericCorrectionKey key) {
        return new AzoulayModel(key.getPressure(), key.getTemperature(), key.getHumidity(), key.getPoint()
            .getAltitude(), this.isGeometricElevation);
    }
}
