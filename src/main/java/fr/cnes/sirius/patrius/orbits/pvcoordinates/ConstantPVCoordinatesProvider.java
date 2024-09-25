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
 * @history creation 10/05/2022
 *
 *
 * HISTORY
 * VERSION:4.13:DM:DM-132:08/12/2023:[PATRIUS] Suppression de la possibilite
 * de convertir les sorties de VacuumSignalPropagation
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9.1:DM:DM-3168:01/06/2022:[PATRIUS] Ajout de la classe ConstantPVCoordinatesProvider
 * VERSION:4.9:DM:DM-3168:10/05/2022:[PATRIUS] Ajout de la classe FixedPVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3165:10/05/2022:[PATRIUS] Amelioration de la propagation du signal 
 * VERSION:4.9:DM:DM-3181:10/05/2022:[PATRIUS] Passage a protected de la methode setPropagationDelayType
 * VERSION:4.9:FA:FA-3170:10/05/2022:[PATRIUS] Incoherence de datation entre la methode getLocalRadius...
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.orbits.pvcoordinates;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class implements the {@link PVCoordinatesProvider} to store the position and the velocity of an object and the
 * frame used for computation.
 * 
 * @author Hugo Barrere
 */
public class ConstantPVCoordinatesProvider implements PVCoordinatesProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = -9171615094880728424L;

    /** Frame of PV coordinates computation. */
    private final Frame frame;

    /** Object position and velocity coordinates. */
    private final PVCoordinates coordinates;

    /**
     * Simple constructor.
     * 
     * @param coordinatesIn
     *        the pv coordinates to store
     * @param frameIn
     *        the frame of used for computation
     */
    public ConstantPVCoordinatesProvider(final PVCoordinates coordinatesIn, final Frame frameIn) {
        this.coordinates = coordinatesIn;
        this.frame = frameIn;
    }
    
    /**
     * Builds an instance from the position vector only. The object is supposed to be motionless in the computation
     * frame.
     * 
     * @param positionIn
     *        the position of the object
     * @param frameIn
     *        the frame of computation
     */
    public ConstantPVCoordinatesProvider(final Vector3D positionIn, final Frame frameIn) {
        this(new PVCoordinates(positionIn, Vector3D.ZERO), frameIn);
    }

    /** @inheritDoc */
    @Override
    public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frameIn) throws PatriusException {
        // get the transform between the stored frame and the provided one.
        final Transform transform = this.frame.getTransformTo(frameIn, date);
        return transform.transformPVCoordinates(this.coordinates);
    }

    /** @inheritDoc */
    @Override
    public Frame getNativeFrame(final AbsoluteDate date) throws PatriusException {
        return this.frame;
    }
}
