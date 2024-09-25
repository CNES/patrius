/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11:DM:DM-3197:22/05/2023:[PATRIUS] Deplacement dans PATRIUS de classes façade ALGO DV SIRUS 
 * VERSION:4.11:DM:DM-3295:22/05/2023:[PATRIUS] Conditions meteorologiques variables dans modeles troposphere
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2975:15/11/2021:[PATRIUS] creation du repere synodique via un LOF 
 * VERSION:4.7:DM:DM-2590:18/05/2021:Configuration des TransformProvider 
 * VERSION:4.7:FA:FA-2923:18/05/2021:Parent frame in class LocalOrbitalFrame 
 * VERSION:4.5:DM:DM-2301:27/05/2020:Ajout de getter a LocalOrbitalFrame 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:489:12/01/2016:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::DM:524:25/05/2016:serialization java doc
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames;

import fr.cnes.sirius.patrius.frames.configuration.FramesConfiguration;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.frames.transformations.TransformProvider;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Class for frames moving with an orbiting satellite.
 * <p>
 * There are several local orbital frames available. They are specified by the {@link LOFType}
 * enumerate.
 * </p>
 *
 * @serial LocalOrbitalFrame is serializable given serializable {@link LocalProvider} and
 *         {@link TransformProvider} (see {@link Frame})
 * @author Luc Maisonobe
 */
public class LocalOrbitalFrame extends Frame {

    /** Serializable UID. */
    private static final long serialVersionUID = -4469440345574964950L;

    /** Local frame type. */
    private final LOFType lofType;

    /** Center of LOF. */
    private PVCoordinatesProvider center;

    /**
     * Build a new instance.
     *
     * @param parent
     *        parent frame. Local orbital frame is defined relatively to this parent frame. Parent
     *        frame is usually inertial or quasi-inertial, although non-inertial frame can also be
     *        used (in this case, this is not exactly a local orbital frame).
     * @param type
     *        frame type
     * @param provider
     *        provider used to compute frame motion
     * @param name
     *        name of the frame
     * @exception IllegalArgumentException
     *            if the parent frame is null
     */
    public LocalOrbitalFrame(final Frame parent, final LOFType type, final PVCoordinatesProvider provider,
            final String name) {
        super(parent, new LocalProvider(type, provider, parent), name, false);
        this.lofType = type;
        this.center = provider;
    }

    /**
     * Returns the local orbital frame type.
     * @return the local orbital frame type
     */
    public LOFType getLofType() {
        return lofType;
    }

    /**
     * Returns the center of the LOF.
     * @return the center of the LOF
     */
    public PVCoordinatesProvider getCenter() {
        return center;
    }

    /**
     * @param center the center to set
     */
    public void setCenter(final PVCoordinatesProvider center) {
        this.center = center;
        // Update transform provider with updated center
        ((LocalProvider) getTransformProvider()).provider = center;
    }

    /**
     * Local provider for transforms.
     *
     * <p>
     * Spin derivative is computed when required.
     * </p>
     * <p>
     * Frames configuration is unused.
     * </p>
     *
     * @serial serializable given a serializable attribut {@link PVCoordinatesProvider}.
     */
    private static class LocalProvider implements TransformProvider {

        /** Serializable UID. */
        private static final long serialVersionUID = 386815086579675823L;

        /** Frame type. */
        private final LOFType type;

        /** Provider used to compute frame motion. */
        private PVCoordinatesProvider provider;

        /** Reference frame. */
        private final Frame reference;

        /**
         * Simple constructor.
         *
         * @param typeIn
         *        frame type
         * @param providerIn
         *        provider used to compute frame motion
         * @param referenceIn
         *        reference frame
         */
        public LocalProvider(final LOFType typeIn, final PVCoordinatesProvider providerIn, final Frame referenceIn) {
            this.type = typeIn;
            this.provider = providerIn;
            this.reference = referenceIn;
        }

        /** {@inheritDoc} */
        @Override
        public Transform getTransform(final AbsoluteDate date) throws PatriusException {
            return this.type.transformFromInertial(date, this.provider.getPVCoordinates(date, this.reference));
        }

        /**
         * {@inheritDoc}
         * <p>
         * Frames configuration is unused.
         * </p>
         */
        @Override
        public Transform getTransform(final AbsoluteDate date, final FramesConfiguration config)
                throws PatriusException {
            return this.getTransform(date);
        }

        /**
         * {@inheritDoc}
         * <p>
         * Spin derivative is computed when required.
         * </p>
         */
        @Override
        public Transform getTransform(final AbsoluteDate date, final boolean computeSpinDerivatives)
                throws PatriusException {
            return this.type.transformFromInertial(date, this.provider.getPVCoordinates(date, this.reference),
                    computeSpinDerivatives);
        }

        /**
         * {@inheritDoc}
         * <p>
         * Spin derivative is computed when required.
         * </p>
         * <p>
         * Frames configuration is unused.
         * </p>
         */
        @Override
        public Transform getTransform(final AbsoluteDate date, final FramesConfiguration config,
                final boolean computeSpinDerivatives) throws PatriusException {
            return this.getTransform(date, computeSpinDerivatives);
        }
    }
}
