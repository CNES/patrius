/**
 * Copyright 2023-2023 CNES
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
 * VERSION:4.14:OPENFD-160:22/08/2024: [PATRIUS] Repere defini par 2 directions
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames;

import fr.cnes.sirius.patrius.attitudes.directions.IDirection;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfiguration;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.frames.transformations.TransformProvider;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Class for frames built with two directions and the two axes they correspond to.
 * 
 * @serial LocalOrbitalFrame is serializable given serializable {@link LocalProvider} and {@link TransformProvider} (see
 *         {@link Frame})
 * @author Mathilde Lefevre
 *
 */
public class TwoDirectionFrame extends Frame {

    /** Serializable UID. **/
    private static final long serialVersionUID = -2231969458336176006L;

    /** Default finite difference delta value to compute the rotation rate : fixed at 1 second */
    private static final double DEFAULT_DH = 1.;

    /** The first direction that defines the frame. **/
    private final IDirection firstDir;

    /** The second direction that defines the frame. **/
    private final IDirection secondDir;

    /**
     * The vector of the frame the first direction corresponds to. For instance Vector3D.PLUS_I if the first direction
     * has to correspond to the first axis of the frame.
     **/
    private final Vector3D frameFirstAxis;

    /** The vector of the frame the second direction corresponds to. **/
    private final Vector3D frameSecondAxis;

    /**
     * Build a new instance with default finite difference delta value
     *
     * @param parentFrame
     *        parent frame. The frame is defined relatively to this parent frame. Parent
     *        frame is usually inertial or quasi-inertial, although non-inertial frame can also be
     *        used.
     * @param provider
     *        state provider used to compute frame motion.
     *        Frame origin from which the directions are computed
     * @param nameIn
     *        name of the frame
     * @param firstDir
     *        the first direction that defines the frame
     * @param secondDir
     *        the second direction that defines the frame
     * @param frameFirstAxis
     *        the axis of the frame the first direction has to correspond to
     * @param frameSecondAxis
     *        the axis of the frame the second direction has to correspond to
     * @exception IllegalArgumentException
     *            if the parent frame is null
     */
    public TwoDirectionFrame(final Frame parentFrame, final PVCoordinatesProvider provider, final String nameIn,
                             final IDirection firstDir,
                             final IDirection secondDir, final Vector3D frameFirstAxis,
                             final Vector3D frameSecondAxis) {
        this(parentFrame, provider, nameIn, firstDir, secondDir, frameFirstAxis, frameSecondAxis, DEFAULT_DH);
    }

    /**
     * Build a new instance.
     *
     * @param parentFrame
     *        parent frame. The frame is defined relatively to this parent frame. Parent
     *        frame is usually inertial or quasi-inertial, although non-inertial frame can also be
     *        used.
     * @param provider
     *        state provider used to compute frame motion.
     *        Frame origin from which the directions are computed
     * @param nameIn
     *        name of the frame
     * @param firstDir
     *        the first direction that defines the frame
     * @param secondDir
     *        the second direction that defines the frame
     * @param frameFirstAxis
     *        the axis of the frame the first direction has to correspond to
     * @param frameSecondAxis
     *        the axis of the frame the second direction has to correspond to
     * @param dH Finite difference delta value to compute the rotation rate
     *        in the direction frame provider (1 s by default)
     * @exception IllegalArgumentException
     *            if the parent frame is null
     */
    public TwoDirectionFrame(final Frame parentFrame, final PVCoordinatesProvider provider, final String nameIn,
                             final IDirection firstDir,
                             final IDirection secondDir, final Vector3D frameFirstAxis,
                             final Vector3D frameSecondAxis,
                             final double dH) {
        super(parentFrame, new TwoDirectionFrameProvider(provider, parentFrame, firstDir, secondDir, frameFirstAxis,
            frameSecondAxis, dH), nameIn, false);
        this.firstDir = firstDir;
        this.secondDir = secondDir;
        this.frameFirstAxis = frameFirstAxis;
        this.frameSecondAxis = frameSecondAxis;
    }

    /**
     * @return the directionOne
     */
    public IDirection getDirectionOne() {
        return this.firstDir;
    }

    /**
     * @return the directionTwo
     */
    public IDirection getDirectionTwo() {
        return this.secondDir;
    }

    /**
     * @return the axisOne
     */
    public Vector3D getAxisOne() {
        return this.frameFirstAxis;
    }

    /**
     * @return the axisTwo
     */
    public Vector3D getAxisTwo() {
        return this.frameSecondAxis;
    }

    /**
     * Inner class. Transform provider consisting in a rotation and a translation of the origin frame
     */
    private static final class TwoDirectionFrameProvider implements TransformProvider {

        /** Serializable UID. */
        private static final long serialVersionUID = -845044299214912266L;

        /** Finite difference delta value to compute the rotation rate (1 s by default). */
        private final double dH;

        /** Destination frame (it states the destination frame axes orientation). **/
        private final Frame destFrame;

        /** PVCoordinatesProvider where to center the destination frame. **/
        private final PVCoordinatesProvider destPvCoordProv;

        /** The first direction that defines the frame the provider relies on. **/
        private final IDirection firstDir;

        /** The second direction that defines the frame the provider relies on. **/
        private final IDirection secondDir;

        /** The axis the first direction of the frame corresponds to. **/
        private final Vector3D frameFirstAxis;

        /** The axis the second direction of the frame corresponds to. **/
        private final Vector3D frameSecondAxis;

        /**
         * Constructor of the provider for the {@link TwoDirectionFrame}. It allows to apply a translation and a
         * rotation between the provided reference frame and two chosen directions.
         * 
         * @param destPvCoordProv PVCoordinates provider to be used for the transform provider
         * @param destFrame the destination reference frame
         * @param firstDir the first direction
         * @param secondDir the second direction
         * @param frameFirstAxis the vector defining the first axis of the reference frame
         * @param frameSecondAxis the vector defining the second axis of the reference frame
         * @param dH Finite difference delta value to compute the rotation rate (1 s by default)
         */
        private TwoDirectionFrameProvider(final PVCoordinatesProvider destPvCoordProv, final Frame destFrame,
                                          final IDirection firstDir, final IDirection secondDir,
                                          final Vector3D frameFirstAxis, final Vector3D frameSecondAxis,
                                          final double dH) {
            this.dH = dH;
            this.destPvCoordProv = destPvCoordProv;
            this.destFrame = destFrame;
            this.firstDir = firstDir;
            this.secondDir = secondDir;
            this.frameFirstAxis = frameFirstAxis;
            this.frameSecondAxis = frameSecondAxis;
        }

        /**
         * Computes the Rotation object corresponding to the rotation from the considered frame to the destination
         * frame.
         * 
         * @param pvProv
         *        the coordinates provider for the considered frame.
         * @param date
         *        the date when the frame is considered.
         * @param frame
         *        the destination frame.
         * @return Rotation to get from the considered frame to the destination frame.
         * @exception PatriusException
         *            if the rotation is undefined
         * 
         **/
        private Rotation computeRotationFromDirections(final PVCoordinatesProvider pvProv, final AbsoluteDate date,
                                                       final Frame frame)
            throws PatriusException {
            final Vector3D u1 = this.firstDir.getVector(pvProv, date, frame);
            final Vector3D u2 = this.secondDir.getVector(pvProv, date, frame);
            return new Rotation(this.frameFirstAxis, this.frameSecondAxis, u1, u2);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Transform getTransform(final AbsoluteDate date) throws PatriusException {
            // Translation computation
            final PVCoordinates translation = this.destPvCoordProv.getPVCoordinates(date, this.destFrame);
            // Rotation computation starting from given directions
            final Rotation rotation = this.computeRotationFromDirections(this.destPvCoordProv, date, this.destFrame);
            // Rotation rate computation by finite differences on a fixed step around the provided date
            final Rotation rotM =
                this.computeRotationFromDirections(this.destPvCoordProv, date.shiftedBy(-this.dH),
                    this.destFrame);
            final Rotation rotP =
                this.computeRotationFromDirections(this.destPvCoordProv, date.shiftedBy(this.dH),
                    this.destFrame);
            final Vector3D rotationRate = AngularCoordinates.estimateRate(rotM, rotP, 2. * this.dH);
            // Return the transform at the specific date
            return new Transform(date, translation, new AngularCoordinates(rotation, rotationRate));
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
         * Spin derivative is unused.
         * </p>
         */
        @Override
        public Transform getTransform(final AbsoluteDate date, final boolean computeSpinDerivatives)
            throws PatriusException {
            return this.getTransform(date);
        }

        /**
         * {@inheritDoc}
         * <p>
         * Frames configuration and spin derivatives are unused.
         * </p>
         */
        @Override
        public Transform getTransform(final AbsoluteDate date, final FramesConfiguration config,
                                      final boolean computeSpinDerivatives)
            throws PatriusException {
            return this.getTransform(date);
        }

    }

}
