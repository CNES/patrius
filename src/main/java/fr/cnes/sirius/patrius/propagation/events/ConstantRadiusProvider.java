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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9.1:FA:FA-3193:01/06/2022:[PATRIUS] Revenir a la signature initiale de la methode getLocalRadius
 * VERSION:4.9:FA:FA-3170:10/05/2022:[PATRIUS] Incoherence de datation entre la methode getLocalRadius...
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:611:02/08/2016:Creation of the class
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.events.AbstractDetector.PropagationDelayType;
import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * Implementation for constant radius provider.
 */
public class ConstantRadiusProvider implements ApparentRadiusProvider {

     /** Serializable UID. */
    private static final long serialVersionUID = 8975277578915733365L;

    /** Constant radius value. */
    private final double constantRadius;

    /**
     * Constructor with constant value.
     * 
     * @param radius
     *        radius value
     */
    public ConstantRadiusProvider(final double radius) {
        this.constantRadius = radius;
    }

    /** {@inheritDoc} */
    @Override
    public double getApparentRadius(final PVCoordinatesProvider pvObserver,
            final AbsoluteDate date,
            final PVCoordinatesProvider occultedBody,
            final PropagationDelayType propagationDelayType) {
        return this.constantRadius;
    }
}
