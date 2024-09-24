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
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2653:18/05/2021:generalisation des sequences et correction/refonte des sequences de segments 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1949:14/11/2018:add new orientation feature
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes.orientations;

import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class aims at creating an orientation angle law whose orientation angle is constant.
 * 
 * @author delaygni
 *
 * @since 4.2
 */
public class ConstantOrientationAngleLaw implements OrientationAngleLaw {

    /** Serial ID. */
    private static final long serialVersionUID = -6834082822835084248L;

    /** Orientation angle. */
    private final double orientationAngle;

    /**
     * Constructor
     * 
     * @param orientationAngleIn constant value of the orientation angle.
     */
    public ConstantOrientationAngleLaw(final double orientationAngleIn) {
        this.orientationAngle = orientationAngleIn;
    }

    /** {@inheritDoc} */
    @Override
    public Double getOrientationAngle(final PVCoordinatesProvider pvProv,
            final AbsoluteDate date) throws PatriusException {
        return this.orientationAngle;
    }

}
