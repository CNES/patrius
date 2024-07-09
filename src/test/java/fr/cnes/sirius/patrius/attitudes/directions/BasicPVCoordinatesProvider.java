/**
 * 
 * Copyright 2011-2017 CNES
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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

}
