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
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.13:FA:FA-112:08/12/2023:[PATRIUS] Probleme si Earth est utilise comme corps pivot pour mar097.bsp
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9.1:FA:FA-3193:01/06/2022:[PATRIUS] Revenir a la signature initiale de la methode getLocalRadius
 * VERSION:4.9:DM:DM-3127:10/05/2022:[PATRIUS] Ajout de deux methodes resize a l'interface GeometricBodyShape ...
 * VERSION:4.9:FA:FA-3170:10/05/2022:[PATRIUS] Incoherence de datation entre la methode getLocalRadius...
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:611:02/08/2016:Creation of the class
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import fr.cnes.sirius.patrius.events.detectors.AbstractSignalPropagationDetector.PropagationDelayType;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;

/**
 * Implementation for variable radius providers.
 */
public class VariableRadiusProvider implements ApparentRadiusProvider {

     /** Serializable UID. */
    private static final long serialVersionUID = 6707592469714471270L;

    /** Geometric body shape. */
    private final BodyShape bodyShape;

    /**
     * Constructor with a body shape capable of calculating apparent radii.
     * 
     * @param shape
     *        body shape
     */
    public VariableRadiusProvider(final BodyShape shape) {
        this.bodyShape = shape;
    }

    /** {@inheritDoc} */
    @Override
    public double getApparentRadius(final PVCoordinatesProvider pvObserver,
            final AbsoluteDate date,
            final PVCoordinatesProvider occultedBody,
            final PropagationDelayType propagationDelayType) {
        try {
            return this.bodyShape.getApparentRadius(pvObserver, date, occultedBody, propagationDelayType);
        } catch (final PatriusException e) {
            throw new PatriusExceptionWrapper(e);
        }
    }

    /**
     * Getter for boy shape.
     * 
     * @return body shape
     */
    public BodyShape getBodyShape() {
        return this.bodyShape;
    }
}
