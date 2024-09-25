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
 */
/*
 *
 * HISTORY
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.7:DM:DM-2590:18/05/2021:Configuration des TransformProvider 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:489:12/01/2016:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::DM:524:25/05/2016:serialization java doc
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames;

import java.util.concurrent.atomic.AtomicReference;

import fr.cnes.sirius.patrius.frames.configuration.FramesConfiguration;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.frames.transformations.TransformProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.FrameAncestorException;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Frame whose transform from its parent can be updated.
 * 
 * @serial UpdatableFrame is serializable given a serializable {@link TransformProvider} (see {@link Frame})
 * @author Luc Maisonobe
 */
public class UpdatableFrame extends Frame {

    /** Serializable UID. */
    private static final long serialVersionUID = -2075893064211339303L;

    /**
     * Build a non-inertial frame from its transform with respect to its parent.
     * <p>
     * calling this constructor is equivalent to call {@link #UpdatableFrame(Frame, Transform, String, boolean)
     * UpdatableFrame(parent, transform, name, false)}.
     * </p>
     * 
     * @param parent
     *        parent frame (must be non-null)
     * @param transform
     *        transform from parent frame to instance
     * @param name
     *        name of the frame
     * @exception IllegalArgumentException
     *            if the parent frame is null
     */
    public UpdatableFrame(final Frame parent, final Transform transform, final String name) {
        this(parent, transform, name, false);
    }

    /**
     * Build a frame from its transform with respect to its parent.
     * <p>
     * The convention for the transform is that it is from parent frame to instance. This means that the two following
     * frames are similar:
     * </p>
     * 
     * <pre>
     * Frame frame1 = new Frame(FramesFactory.getGCRF(), new Transform(t1, t2));
     * Frame frame2 = new Frame(new Frame(FramesFactory.getGCRF(), t1), t2);
     * </pre>
     * 
     * @param parent
     *        parent frame (must be non-null)
     * @param transform
     *        transform from parent frame to instance
     * @param name
     *        name of the frame
     * @param pseudoInertial
     *        true if frame is considered pseudo-inertial
     *        (i.e. suitable for propagating orbit)
     * @exception IllegalArgumentException
     *            if the parent frame is null
     */
    public UpdatableFrame(final Frame parent, final Transform transform, final String name,
        final boolean pseudoInertial) {
        super(parent, new UpdatableProvider(transform), name, pseudoInertial);
    }

    /**
     * Update the transform from the parent frame to the instance.
     * 
     * @param transform
     *        new transform from parent frame to instance
     */
    public void setTransform(final Transform transform) {
        ((UpdatableProvider) this.getTransformProvider()).setTransform(transform);
    }

    /**
     * Update the transform from parent frame implicitly according to two other
     * frames.
     * 
     * <p>
     * This method allows to control the relative position of two parts of the global frames tree using any two frames
     * in each part as control handles. Consider the following simplified frames tree as an example:
     * </p>
     * 
     * <pre>
     *              GCRF
     *                |
     *  --------------------------------
     *  |             |                |
     * Sun        satellite          Earth
     *                |                |
     *        on-board antenna   ground station
     *                                 |
     *                          tracking antenna
     * </pre>
     * <p>
     * Tracking measurements really correspond to the link between the ground and on-board antennas. This is tightly
     * linked to the transform between these two frames, however neither frame is the direct parent frame of the other
     * one: the path involves four intermediate frames. When we process a measurement, what we really want to update is
     * the transform that defines the satellite frame with respect to its parent GCRF frame. This is the purpose of this
     * method. This update is done by the following call, where <code>measurementTransform</code> represents the
     * measurement as a simple translation transform between the two antenna frames:
     * </p>
     * 
     * <pre>
     * <code>
     * satellite.updateTransform(onBoardAntenna, trackingAntenna,
     *                           measurementTransform, date);
     * </code>
     * </pre>
     * <p>
     * One way to represent the behavior of the method is to consider the sub-tree rooted at the instance on one hand
     * (satellite and on-board antenna in the example above) and the tree containing all the other frames on the other
     * hand (GCRF, Sun, Earth, ground station, tracking antenna). Both tree are considered as solid sets linked by a
     * flexible spring, which is the transform we want to update. The method stretches the spring to make sure the
     * transform between the two specified frames (one in each tree part) matches the specified transform.
     * </p>
     * 
     * @param f1
     *        first control frame (may be the instance itself)
     * @param f2
     *        second control frame (may be the instance itself)
     * @param f1Tof2
     *        desired transform from first to second control frame
     * @param date
     *        date of the transform
     * @exception PatriusException
     *            if the path between the two control frames does
     *            not cross the link between instance and its parent frame or if some
     *            intermediate transform fails
     * @see #setTransform(Transform)
     */
    public void updateTransform(final Frame f1, final Frame f2, final Transform f1Tof2,
                                final AbsoluteDate date) throws PatriusException {

        Frame fA = f1;
        Frame fB = f2;
        Transform fAtoB = f1Tof2;

        // make sure f1 is not a child of the instance
        if (fA.isChildOf(this) || (fA == this)) {

            if (fB.isChildOf(this) || (fB == this)) {
                throw new FrameAncestorException(PatriusMessages.FRAME_ANCESTOR_OF_BOTH_FRAMES,
                    this.getName(), fA.getName(), fB.getName());
            }

            // swap f1 and f2 to make sure the child is f2
            final Frame tmp = fA;
            fA = fB;
            fB = tmp;
            fAtoB = fAtoB.getInverse();

        } else if (!(fB.isChildOf(this) || (fB == this))) {
            throw new FrameAncestorException(PatriusMessages.FRAME_ANCESTOR_OF_NEITHER_FRAME,
                this.getName(), fA.getName(), fB.getName());
        }

        // rebuild the transform by traveling from parent to self
        // WITHOUT using the existing this.transform that will be updated
        final Transform parentTofA = this.getParent().getTransformTo(fA, date);
        final Transform fBtoSelf = fB.getTransformTo(this, date);
        final Transform fAtoSelf = new Transform(date, fAtoB, fBtoSelf);
        this.setTransform(new Transform(date, parentTofA, fAtoSelf));
    }

    /**
     * Local provider for transforms.
     * 
     * <p>
     * Spin derivative available only if defined in the transformation of the parent class {@link UpdatableFrame}.
     * </p>
     * <p>Frames configuration is unused.</p>
     * 
     * @serial serializable.
     */
    private static class UpdatableProvider implements TransformProvider {

        /** Serializable UID. */
        private static final long serialVersionUID = 4436954500689776331L;

        /** Current transform. */
        private final AtomicReference<Transform> transform;

        /**
         * Simple constructor.
         * 
         * @param transformIn
         *        initial value of the transform
         */
        public UpdatableProvider(final Transform transformIn) {
            this.transform = new AtomicReference<>(transformIn);
        }

        /**
         * Update the transform from the parent frame to the instance.
         * 
         * @param transformIn
         *        new transform from parent frame to instance
         */
        public void setTransform(final Transform transformIn) {
            this.transform.set(transformIn);
        }

        /** {@inheritDoc} */
        @Override
        public Transform getTransform(final AbsoluteDate date) {
            return this.transform.get();
        }

        /**
         * {@inheritDoc}
         * <p>
         * Spin derivative available only if defined in the transformation of the parent class {@link UpdatableFrame}.
         * </p>
         */
        @Override
        public Transform getTransform(final AbsoluteDate date, final boolean computeSpinDerivatives) {
            return this.transform.get();
        }

        /**
         * {@inheritDoc}
         * <p>
         * Frames configuration is unused.
         * </p>
         */
        @Override
        public Transform getTransform(final AbsoluteDate date,
                                      final FramesConfiguration config) throws PatriusException {
            return this.transform.get();
        }

        /**
         * {@inheritDoc}
         * <p>
         * Spin derivative available only if defined in the transformation of the parent class {@link UpdatableFrame}.
         * </p>
         * <p>
         * Frames configuration is unused.
         * </p>
         */
        @Override
        public Transform getTransform(final AbsoluteDate date, final FramesConfiguration config,
                                      final boolean computeSpinDerivatives) throws PatriusException {
            return this.transform.get();
        }
    }

}
