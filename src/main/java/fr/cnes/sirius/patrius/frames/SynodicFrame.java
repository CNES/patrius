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
 * VERSION:4.9:FA:FA-3105:10/05/2022:[PATRIUS] Corrections javadoc 
 * VERSION:4.8:DM:DM-2975:15/11/2021:[PATRIUS] creation du repere synodique via un LOF 
 * VERSION:4.8:DM:DM-2992:15/11/2021:[PATRIUS] Possibilite d'interpoler l'attitude en ignorant les rotations rate 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames;

import fr.cnes.sirius.patrius.frames.configuration.FramesConfiguration;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.frames.transformations.TransformProvider;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Synodic frame.
 * A synodic frame is a frame aligned on a {@link LocalOrbitalFrame} and translated by a proportional distance along 
 * the axis defined by the 2 centers (LOF center and LOF parent center).
 * This kind of frame is well suited for the 3rd body problem, for which the classical synodic frame is:
 * <p>LOF frame is of type {@link LOFType#QSW} centered around 2nd body and normalized center position is
 * defined using bodies mass ratio (in [0, 1]). 0 means the synodic frame is centered around the main body,
 * 1 means the synodic frame is centered around the LOF (i.e. the second body in case of three body problem).</p>
 * 
 * @author Emmanuel Bignon
 * 
 * @since 4.8
 */
public class SynodicFrame extends Frame {

    /** Serial UID. */
    private static final long serialVersionUID = -3011792375741753108L;

    /**
     * Constructor.
     * <p>
     * Example: LOF of type {@link LOFType#QSW} centered around 2nd body and normalizedCenterPosition using bodies mass
     * ratio will produce the classical synodic frame for three body problem.
     * </p>
     * 
     * @param parentLOF
     *        parent frame of type {@link LocalOrbitalFrame} (must be non-null)
     * @param name
     *        name of the frame
     * @param normalizedCenterPosition
     *        normalized center position: 1 means this frame is equal to provided LOF (i.e. centered on the 2nd body), 
     *        0 means this frame is translated to the parent frame center (i.e. centered on the main body)
     * @exception IllegalArgumentException
     *            if the parent frame is null
     */
    public SynodicFrame(final LocalOrbitalFrame parentLOF,
            final String name,
            final double normalizedCenterPosition) {
        super(parentLOF, new FrameTranslation(parentLOF, normalizedCenterPosition), name, parentLOF.isPseudoInertial());
    }

    /**
     * Inner class. Transform provider consisting in a simple translation of the origin frame to one position
     * in the axis defined by the 2 centers (LOF center and LOF parent center).
     */
    private static final class FrameTranslation implements TransformProvider {

        /** Generated Serial Version UID. */
        private static final long serialVersionUID = -845044299214912266L;

        /** Origin frame (it states the destination frame axes orientation). */
        private final LocalOrbitalFrame originFrame;

        /**
         * Normalized center position: 1 means this frame is equal to provided LOF (i.e. centered on the 2nd body),
         * 0 means this frame is translated to the parent frame center (i.e. centered on the main body).
         */
        private final double normalizedCenterPosition;

        /**
         * Constructor. It creates a transform provider consisting in a simple translation of the origin frame
         * to the position given by the center of the celestial body.
         *
         * @param frame
         *        origin frame stating the destination frame axes orientation
         * @param normalizedCenterPosition
         *        normalized center position: 1 means this frame is equal to provided LOF (i.e. centered on the 2nd
         *        body), 0 means this frame is translated to the parent frame center (i.e. centered on the main body)
         */
        private FrameTranslation(final LocalOrbitalFrame frame,
                final double normalizedCenterPosition) {
            this.normalizedCenterPosition = normalizedCenterPosition;
            this.originFrame = frame;
        }

        /** {@inheritDoc} */
        @Override
        public Transform getTransform(final AbsoluteDate date,
                final FramesConfiguration config,
                final boolean computeSpinDerivatives) throws PatriusException {
            // Getting transform from LOF to its parent
            final Transform transformToParent = originFrame.getTransformTo(originFrame.getParent(), date);
            // Compute translation
            final double coef = 1. - normalizedCenterPosition;
            final Vector3D translation = transformToParent.getTranslation().scalarMultiply(coef);
            // Translation from LOF frame to synodic frame center
            return new Transform(date, translation);
        }

        /** {@inheritDoc} */
        @Override
        public Transform getTransform(final AbsoluteDate date,
                final boolean computeSpinDerivatives) throws PatriusException {
            return getTransform(date, FramesFactory.getConfiguration(), computeSpinDerivatives);
        }

        /** {@inheritDoc} */
        @Override
        public Transform getTransform(final AbsoluteDate date,
                final FramesConfiguration config) throws PatriusException {
            return getTransform(date, config, false);
        }

        /** {@inheritDoc} */
        @Override
        public Transform getTransform(final AbsoluteDate date) throws PatriusException {
            return getTransform(date, FramesFactory.getConfiguration(), false);
        }
    }
}
