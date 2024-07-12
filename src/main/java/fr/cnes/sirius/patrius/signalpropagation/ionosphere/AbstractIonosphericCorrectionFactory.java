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

import java.util.concurrent.ConcurrentHashMap;

import fr.cnes.sirius.patrius.frames.TopocentricFrame;

/**
 * Ionospheric correction model factory.
 * <p>
 * This class can initialize and store in cache {@link IonosphericCorrection} correction models.
 * </p>
 * <p>
 * The ionospheric corrections models are organized within a {@link ConcurrentHashMap} to assure multi-thread usage
 * safety.
 * </p>
 *
 * @author bonitt
 */
public abstract class AbstractIonosphericCorrectionFactory {

    /** Cached {@link IonosphericCorrection} correction models. */
    private final ConcurrentHashMap<TopocentricFrame, IonosphericCorrection> correctionModelsCache =
        new ConcurrentHashMap<>();

    /**
     * Getter for an ionospheric correction model.
     * <p>
     * This method looks if the asking model is already initialized.<br>
     * If it's the case the model is directly returned, otherwise the model is initialized, stored and returned.
     * </p>
     *
     * @param topoFrame
     *        Topocentric frame associated to the ionospheric correction
     * @return the ionospheric correction model
     */
    public IonosphericCorrection getIonoCorrection(final TopocentricFrame topoFrame) {
        return this.correctionModelsCache.computeIfAbsent(topoFrame, (keyTopoFrame) -> {
            return buildIonoCorrection(keyTopoFrame);
        });
    }

    /**
     * Return the ionospheric correction model provided by the factory.
     *
     * @param topoFrame
     *        Topocentric frame associated to the ionospheric correction
     * @return the ionospheric correction model
     */
    protected abstract IonosphericCorrection buildIonoCorrection(TopocentricFrame topoFrame);
}
