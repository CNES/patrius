/**
 * 
 * Copyright 2011-2022 CNES
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
 * @history creation 01/12/2011
 *
 * HISTORY
 * VERSION:4.13:DM:DM-132:08/12/2023:[PATRIUS] Suppression de la possibilite
 * de convertir les sorties de VacuumSignalPropagation
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3165:10/05/2022:[PATRIUS] Amelioration de la propagation du signal 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.attitudes.directions;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @description This class is only used in the tests of the package Directions. This
 *              is a basic PVCoordinatesProvider : the attributes are a PVCoordinates object that can
 *              be returned and its frame of expression.
 * 
 * @concurrency not thread-safe
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: BasicPVCoordinatesProvider.java 17910 2017-09-11 11:58:16Z bignon $
 * 
 * @since 1.1
 * 
 */
public class BasicPVCoordinatesProvider implements PVCoordinatesProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 3796887407499337431L;

    /** PVCoordinates point. */
    private final PVCoordinates coordinates;

    /** Expression frame. */
    private final Frame frame;

    /**
     * Build a direction from an origin and a target described by their
     * PVCoordinatesProvider
     * 
     * @param inCoordinates
     *        the PVCoordinates
     * @param inFrame
     *        the frame in which the coordinates are expressed
     * */
    public BasicPVCoordinatesProvider(final PVCoordinates inCoordinates, final Frame inFrame) {

        // Initialisation
        this.coordinates = inCoordinates;
        this.frame = inFrame;
    }

    /** {@inheritDoc} */
    @Override
    public final PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame inFrame) throws PatriusException {

        // the coordinates are expressed in the output frame
        final Transform toOutputFrame = this.frame.getTransformTo(inFrame, date);
        final PVCoordinates outCoordinates = toOutputFrame.transformPVCoordinates(this.coordinates);

        return outCoordinates;
    }

    /** {@inheritDoc} */
    @Override
    public Frame getNativeFrame(final AbsoluteDate date) throws PatriusException {
        return frame;
    }
}
