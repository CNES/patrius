/**
 * Copyright 2021-2021 CNES
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
 * VERSION:4.13:FA:FA-131:08/12/2023:[PATRIUS] TranslatedFrame pas obligatoirement inertiel si son parent l'est
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2968:15/11/2021:[PATRIUS] gestion du centre des reperes 
 * VERSION:4.8:DM:DM-2958:15/11/2021:[PATRIUS] calcul d'intersection a altitude non nulle pour l'interface BodyShape 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames;

import fr.cnes.sirius.patrius.frames.configuration.FramesConfiguration;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.frames.transformations.TransformProvider;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Frame which differs from a reference frame by translation provided by a
 * {@link PVCoordinatesProvider}.
 * Provided {@link PVCoordinatesProvider} represents the center of this frame.</br>
 * Typically this can be used to translate a frame, with keeping the axis aligned with parent frame.
 * 
 * @author Emmanuel Bignon
 * 
 * @since 4.8
 */
public class TranslatedFrame extends Frame {

     /** Serializable UID. */
    private static final long serialVersionUID = 6202577341890453744L;

    /** Frame center. */
    private final PVCoordinatesProvider center;
    
    /**
     * Constructor.
     * 
     * @param parent
     *        parent frame (must be non-null)
     * @param center
     *        center of this frame
     * @param name
     *        name of the frame
     * @param pseudoInertial
     *        true if frame is considered pseudo-inertial (i.e. suitable for propagating orbit)
     * @exception IllegalArgumentException
     *            if the parent frame is null
     */
    public TranslatedFrame(final Frame parent,
            final PVCoordinatesProvider center,
            final String name,
            final boolean pseudoInertial) {
        super(parent, new FrameTranslation(parent, center), name, pseudoInertial);
        this.center = center;
    }

    /**
     * Returns the center of this frame.
     * @return the center of this frame
     */
    public PVCoordinatesProvider getCenter() {
        return center;
    }
    
    /**
     * Inner class. Transform provider consisting in a simple translation of the origin frame to the position
     * given by the center of the celestial body. This is needed in the constructor of TranslatedFrame.
     */
    private static final class FrameTranslation implements TransformProvider {

        /** Serializable UID. */
        private static final long serialVersionUID = -845044299214912266L;

        /** Origin frame (it states the destination frame axes orientation). */
        private final Frame originFrame;

        /** PVCoordinatesProvider where to center the destination frame. */
        private final PVCoordinatesProvider center;

        /**
         * Constructor. It creates a transform provider consisting in a simple translation of the origin frame
         * to the position given by the center of the celestial body.
         *
         * @param frame
         *        origin frame stating the destination frame axes orientation
         * @param center
         *        PVCoordinatesProvider where to center the destination frame
         */
        private FrameTranslation(final Frame frame, final PVCoordinatesProvider center) {
            this.center = center;
            this.originFrame = frame;
        }

        /** {@inheritDoc} */
        @Override
        public Transform getTransform(final AbsoluteDate date, final FramesConfiguration config,
                final boolean computeSpinDerivatives) throws PatriusException {
            return getTransform(date);
        }

        /** {@inheritDoc} */
        @Override
        public Transform
                getTransform(final AbsoluteDate date, final boolean computeSpinDerivatives)
                        throws PatriusException {
            return getTransform(date);
        }

        /** {@inheritDoc} */
        @Override
        public Transform getTransform(final AbsoluteDate date, final FramesConfiguration config)
                throws PatriusException {
            return getTransform(date);
        }

        /** {@inheritDoc} */
        @Override
        public Transform getTransform(final AbsoluteDate date) throws PatriusException {
            // Getting coordinates of central body
            final PVCoordinates pvFrameCenter = center.getPVCoordinates(date, originFrame);
            // Translation from origin frame to central body center
            return new Transform(date, pvFrameCenter);
        }
    }
}
