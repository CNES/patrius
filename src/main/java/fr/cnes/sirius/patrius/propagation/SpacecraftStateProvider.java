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
 */
/*
 * HISTORY
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.7:DM:DM-2767:18/05/2021:Evolutions et corrections diverses 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation;

import java.io.Serializable;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 ** Interface for spacecraft state providers.
 */
public interface SpacecraftStateProvider extends PVCoordinatesProvider, Serializable {

    /**
     * Get the {@link SpacecraftState} at provided date.
     * 
     * @param date
     *        target date
     * @return spacecraft state
     * @exception PropagationException
     *            if state cannot be computed
     */
    SpacecraftState getSpacecraftState(final AbsoluteDate date) throws PropagationException;

    /**
     * Default implementation for PVCoordinatesProvider feature using the {@link SpacecraftState}
     * object's orbit.
     *
     * <p>
     * {@inheritDoc}
     * </p>
     */
    @Override
    public default PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame)
            throws PatriusException {
        return getSpacecraftState(date).getPVCoordinates(frame);
    }
}
