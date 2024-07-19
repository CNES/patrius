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
package fr.cnes.sirius.patrius.signalpropagation.ionosphere;

import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.SolarActivityDataProvider;
import fr.cnes.sirius.patrius.frames.TopocentricFrame;

/**
 * This class describes the ionospheric correction factory around the {@link BentModel Bent model}.
 *
 * @author bonitt
 */
public class BentModelFactory extends AbstractIonosphericCorrectionFactory {

    /** Provider for the R12 value. */
    private final R12Provider r12Provider;

    /** Provider for the solar activity. */
    private final SolarActivityDataProvider solarActivity;

    /** Provider for the model data. */
    private final USKProvider uskProvider;

    /**
     * Bent model factory constructor.
     *
     * @param r12Provider
     *        Provider for the R12 value
     * @param solarActivity
     *        Provider for the solar activity
     * @param uskProvider
     *        Provider for the model data
     */
    public BentModelFactory(final R12Provider r12Provider, final SolarActivityDataProvider solarActivity,
                            final USKProvider uskProvider) {
        super();
        this.r12Provider = r12Provider;
        this.solarActivity = solarActivity;
        this.uskProvider = uskProvider;
    }

    /** {@inheritDoc} */
    @Override
    protected BentModel buildIonoCorrection(final TopocentricFrame topoFrame) {
        return new BentModel(this.r12Provider, this.solarActivity, this.uskProvider, topoFrame);
    }
}
