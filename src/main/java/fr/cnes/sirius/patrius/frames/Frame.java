/**
 * Copyright 2002-2012 CS Systèmes d'Information
 * Copyright 2011-2017 CNES
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
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:481:05/10/2015: method to compute the jacobian related to the conversion from a frame to another
 * VERSION::DM:489:12/01/2016:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:513:09/03/2016:Make Frame class multithread safe
 * VERSION::DM:524:25/05/2016:serialization java doc
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames;

import java.io.Serializable;

import fr.cnes.sirius.patrius.frames.configuration.FramesConfiguration;
import fr.cnes.sirius.patrius.frames.transformations.FixedTransformProvider;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.frames.transformations.TransformProvider;
import fr.cnes.sirius.patrius.math.linear.Array2DRowRealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Tridimensional references frames class.
 * 
 * <h5>Frame Presentation</h5>
 * <p>
 * This class is the base class for all frames in OREKIT. The frames are linked together in a tree with some specific
 * frame chosen as the root of the tree. Each frame is defined by {@link Transform transforms} combining any number of
 * translations and rotations from a reference frame which is its parent frame in the tree structure.
 * </p>
 * <p>
 * When we say a {@link Transform transform} t is <em>from frame<sub>A</sub>
 * to frame<sub>B</sub></em>, we mean that if the coordinates of some absolute vector (say the direction of a distant
 * star for example) has coordinates u<sub>A</sub> in frame<sub>A</sub> and u<sub>B</sub> in frame<sub>B</sub>, then
 * u<sub>B</sub>={@link Transform#transformVector(fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D)
 * t.transformVector(u<sub>A</sub>)}.
 * <p>
 * The transforms may be constant or varying, depending on the implementation of the {@link TransformProvider transform
 * provider} used to define the frame. For simple fixed transforms, using {@link FixedTransformProvider} is sufficient.
 * For varying transforms (time-dependent or telemetry-based for example), it may be useful to define specific
 * implementations of {@link TransformProvider transform provider}.
 * </p>
 * 
 * @serial Frame is serializable given a serializable {@link TransformProvider}
 * 
 * @author Guylaine Prat
 * @author Luc Maisonobe
 * @author Pascal Parraud
 */
@SuppressWarnings("PMD.NullAssignment")
public class Frame implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = -6981146543760234087L;

    /** Parent frame (only the root frame doesn't have a parent). */
    private final Frame parent;

    /** Provider for transform from parent frame to instance. */
    private final TransformProvider transformProvider;

    /** Depth of the frame with respect to tree root. */
    private final int depth;

    /** Instance name. */
    private final String name;

    /** Indicator for pseudo-inertial frames. */
    private final boolean pseudoInertial;

    /**
     * Private constructor used only for the root frame.
     * 
     * @param nameIn
     *        name of the frame
     * @param pseudoInertialIn
     *        true if frame is considered pseudo-inertial (i.e. suitable for propagating orbit)
     */
    protected Frame(final String nameIn, final boolean pseudoInertialIn) {
        this.parent = null;
        this.transformProvider = new FixedTransformProvider(Transform.IDENTITY);
        this.depth = 0;
        this.name = nameIn;
        this.pseudoInertial = pseudoInertialIn;
    }

    /**
     * Build a non-inertial frame from its transform with respect to its parent.
     * <p>
     * calling this constructor is equivalent to call <code>{link {@link #Frame(Frame, Transform, String, boolean)
     * Frame(parent, transform, name, false)}</code>.
     * </p>
     * 
     * @param parentIn
     *        parent frame (must be non-null)
     * @param transformIn
     *        transform from parent frame to instance
     * @param nameIn
     *        name of the frame
     * @exception IllegalArgumentException
     *            if the parent frame is null
     */
    public Frame(final Frame parentIn, final Transform transformIn,
        final String nameIn) {
        this(parentIn, transformIn, nameIn, false);
    }

    /**
     * Build a non-inertial frame from its transform with respect to its parent.
     * <p>
     * calling this constructor is equivalent to call <code>{link {@link #Frame(Frame, Transform, String, boolean)
     * Frame(parent, transform, name, false)}</code>.
     * </p>
     * 
     * @param parentIn
     *        parent frame (must be non-null)
     * @param transformProviderIn
     *        provider for transform from parent frame to instance
     * @param nameIn
     *        name of the frame
     * @exception IllegalArgumentException
     *            if the parent frame is null
     */
    public Frame(final Frame parentIn, final TransformProvider transformProviderIn, final String nameIn) {
        this(parentIn, transformProviderIn, nameIn, false);
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
     * @param parentIn
     *        parent frame (must be non-null)
     * @param transformIn
     *        transform from parent frame to instance
     * @param nameIn
     *        name of the frame
     * @param pseudoInertialIn
     *        true if frame is considered pseudo-inertial (i.e. suitable for propagating orbit)
     * @exception IllegalArgumentException
     *            if the parent frame is null
     */
    public Frame(final Frame parentIn, final Transform transformIn, final String nameIn,
        final boolean pseudoInertialIn) {
        this(parentIn, new FixedTransformProvider(transformIn), nameIn, pseudoInertialIn);
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
     * @param parentIn
     *        parent frame (must be non-null)
     * @param transformProviderIn
     *        provider for transform from parent frame to instance
     * @param nameIn
     *        name of the frame
     * @param pseudoInertialIn
     *        true if frame is considered pseudo-inertial (i.e. suitable for propagating orbit)
     * @exception IllegalArgumentException
     *            if the parent frame is null
     */
    public Frame(final Frame parentIn, final TransformProvider transformProviderIn, final String nameIn,
        final boolean pseudoInertialIn) {

        if (parentIn == null) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.NULL_PARENT_FOR_FRAME, nameIn);
        }
        this.name = nameIn;
        this.pseudoInertial = pseudoInertialIn;
        this.parent = parentIn;
        this.transformProvider = transformProviderIn;
        this.depth = parentIn.depth + 1;

    }

    /**
     * Get the name.
     * 
     * @return the name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Get the depth of the frame.
     * <p>
     * The depth of a frame is the number of parents frame between it and the frames tree root. It is 0 for the root
     * frame, and the depth of a frame is the depth of its parent frame plus one.
     * </p>
     * 
     * @return depth of the frame
     */
    protected int getDepth() {
        return this.depth;
    }

    /**
     * Get the n<sup>th</sup> ancestor of the frame.
     * 
     * @param n
     *        index of the ancestor (0 is the instance, 1 is its parent, 2
     *        is the parent of its parent...)
     * @return n<sup>th</sup> ancestor of the frame (must be between 0 and the
     *         depth of the frame)
     * @exception IllegalArgumentException
     *            if n is larger than the depth of the instance
     */
    protected Frame getAncestor(final int n) {

        // safety check
        if (n > this.depth) {
            throw PatriusException.createIllegalArgumentException(
                PatriusMessages.FRAME_NO_NTH_ANCESTOR, this.name, this.depth, n);
        }

        // go upward to find ancestor
        Frame current = this;
        for (int i = 0; i < n; ++i) {
            current = current.parent;
        }

        return current;

    }

    /**
     * Check if the frame is pseudo-inertial.
     * <p>
     * Pseudo-inertial frames are frames that do have a linear motion and either do not rotate or rotate at a very low
     * rate resulting in neglectible inertial forces. This means they are suitable for orbit definition and propagation
     * using Newtonian mechanics. Frames that are <em>not</em> pseudo-inertial are <em>not</em> suitable for orbit
     * definition and propagation.
     * </p>
     * 
     * @return true if frame is pseudo-inertial
     */
    public boolean isPseudoInertial() {
        return this.pseudoInertial;
    }

    /**
     * New definition of the java.util toString() method.
     * 
     * @return the name
     */
    @Override
    public String toString() {
        return this.name;
    }

    /**
     * Get the parent frame.
     * 
     * @return parent frame
     */
    public Frame getParent() {
        return this.parent;
    }

    /**
     * Get the transform from the instance to another frame.
     * 
     * @param destination
     *        destination frame to which we want to transform vectors
     * @param date
     *        the date (can be null if it is sure than no date dependent frame is used)
     * @return transform from the instance to the destination frame
     * @exception PatriusException
     *            if some frame specific error occurs
     */
    public Transform getTransformTo(final Frame destination, final AbsoluteDate date) throws PatriusException {

        return this.getTransformTo(destination, date, false);

    }

    /**
     * Get the transform from the instance to another frame.
     * 
     * @param destination
     *        destination frame to which we want to transform vectors
     * @param date
     *        the date (can be null if it is sure than no date dependent frame is used)
     * @param computeSpinDerivatives
     *        spin derivatives are computed : true, or not : false
     * @return transform from the instance to the destination frame
     * @exception PatriusException
     *            if some frame specific error occurs
     */
    public Transform getTransformTo(final Frame destination, final AbsoluteDate date,
                                    final boolean computeSpinDerivatives) throws PatriusException {

        return this.getTransformTo(destination, date, FramesFactory.getConfiguration(), computeSpinDerivatives);

    }

    /**
     * Get the transform from the instance to another frame.
     * 
     * @param destination
     *        destination frame to which we want to transform vectors
     * @param date
     *        the date (can be null if it is sure than no date dependent frame is used)
     * @param config
     *        frames configuration to use
     * @return transform from the instance to the destination frame
     * @exception PatriusException
     *            if some frame specific error occurs
     */
    public Transform getTransformTo(final Frame destination, final AbsoluteDate date,
                                    final FramesConfiguration config) throws PatriusException {

        return this.getTransformTo(destination, date, config, false);

    }

    /**
     * Get the transform from the instance to another frame.
     * 
     * @param destination
     *        destination frame to which we want to transform vectors
     * @param date
     *        the date (can be null if it is sure than no date dependent frame is used)
     * @param config
     *        frames configuration to use
     * @param computeSpinDerivatives
     *        spin derivatives are computed : true, or not : false
     * @return transform from the instance to the destination frame
     * @exception PatriusException
     *            if some frame specific error occurs
     */
    public Transform getTransformTo(final Frame destination, final AbsoluteDate date, final FramesConfiguration config,
            final boolean computeSpinDerivatives) throws PatriusException {

        if (this == destination) {
            // shortcut for special case that may be frequent
            return Transform.IDENTITY;
        }

        // common ancestor to both frames in the frames tree
        final Frame common = findCommon(this, destination);
        if (common == null) {
            throw new PatriusException(PatriusMessages.NO_COMMON_FRAME, this.name, destination.getName());
        }

        // transform from common to instance
        Transform commonToInstance = Transform.IDENTITY;
        for (Frame frame = this; !frame.equals(common); frame = frame.parent) {
            commonToInstance = new Transform(date, frame.transformProvider.getTransform(date, config,
                computeSpinDerivatives), commonToInstance, computeSpinDerivatives);
        }

        // transform from destination up to common
        Transform commonToDestination = Transform.IDENTITY;
        for (Frame frame = destination; !frame.equals(common); frame = frame.parent) {
            commonToDestination = new Transform(date, frame.transformProvider.getTransform(date, config,
                computeSpinDerivatives), commonToDestination, computeSpinDerivatives);
        }

        // transform from instance to destination via common
        return new Transform(date, commonToInstance.getInverse(computeSpinDerivatives), commonToDestination,
            computeSpinDerivatives);

    }

    /**
     * Get the provider for transform from parent frame to instance.
     * 
     * @return provider for transform from parent frame to instance
     */
    public TransformProvider getTransformProvider() {
        return this.transformProvider;
    }

    /**
     * Find the deepest common ancestor of two frames in the frames tree.
     * 
     * @param from
     *        origin frame
     * @param to
     *        destination frame
     * @return an ancestor frame of both <code>from</code> and <code>to</code>. Returns <code>null</code> if such an
     *         ancestor does not exist
     */
    private static Frame findCommon(final Frame from, final Frame to) {

        // select deepest frames that could be the common ancestor
        Frame currentF = from.depth > to.depth ? from.getAncestor(from.depth - to.depth) : from;
        Frame currentT = from.depth > to.depth ? to : to.getAncestor(to.depth - from.depth);

        // go upward until we find a match
        while (currentT != null && !currentT.equals(currentF)) {
            currentF = currentF.parent;
            currentT = currentT.parent;
        }

        return currentF;

    }

    /**
     * Determine if a Frame is a child of another one.
     * 
     * @param potentialAncestor
     *        supposed ancestor frame
     * @return true if the potentialAncestor belongs to the path from instance to the root frame
     */
    public boolean isChildOf(final Frame potentialAncestor) {
        for (Frame frame = this.parent; frame != null; frame = frame.parent) {
            if (frame.equals(potentialAncestor)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the unique root frame.
     * 
     * @return the unique instance of the root frame
     */
    protected static Frame getRoot() {
        return LazyRootHolder.INSTANCE;
    }

    /**
     * Get a new version of the instance, frozen with respect to a reference frame.
     * <p>
     * Freezing a frame consist in computing its position and orientation with respect to another frame at some freezing
     * date and fixing them so they do not depend on time anymore. This means the frozen frame is fixed with respect to
     * the reference frame.
     * </p>
     * <p>
     * One typical use of this method is to compute an inertial launch reference frame by freezing a
     * {@link TopocentricFrame topocentric frame} at launch date with respect to an inertial frame. Another use is to
     * freeze an equinox-related celestial frame at a reference epoch date.
     * </p>
     * <p>
     * Only the frame returned by this method is frozen, the instance by itself is not affected by calling this method
     * and still moves freely.
     * </p>
     * 
     * @param reference
     *        frame with respect to which the instance will be frozen
     * @param freezingDate
     *        freezing date
     * @param frozenName
     *        name of the frozen frame
     * @return a frozen version of the instance
     * @exception PatriusException
     *            if transform between reference frame and instance cannot be computed at freezing frame
     */
    public Frame getFrozenFrame(final Frame reference, final AbsoluteDate freezingDate,
                                final String frozenName) throws PatriusException {
        return new Frame(reference, reference.getTransformTo(this, freezingDate).freeze(), frozenName,
            reference.isPseudoInertial());
    }

    /**
     * Compute the Jacobian from current frame to target frame at provided date.
     * 
     * @param to
     *        target frame
     * @param date
     *        date
     * @return transform Jacobian from current frame to target frame
     * @exception PatriusException
     *            Failed to compute frame transformation
     */
    public RealMatrix getTransformJacobian(final Frame to, final AbsoluteDate date) throws PatriusException {

        // Get frame transformation
        final Transform transform = this.getTransformTo(to, date);

        // Get jacobian of transformation
        final double[][] frameJacobian = new double[6][6];
        transform.getJacobian(frameJacobian);

        return new Array2DRowRealMatrix(frameJacobian);
    }

    // We use the Initialization on demand holder idiom to store
    // the singletons, as it is both thread-safe, efficient (no
    // synchronization) and works with all versions of java.

    /** Holder for the root frame singleton. */
    private static final class LazyRootHolder {

        /** Unique instance. */
        private static final Frame INSTANCE = new Frame("GCRF", true){

            /** Serializable UID. */
            private static final long serialVersionUID = -2654403496396721543L;

            /**
             * Replace deserialized objects by singleton instance.
             * 
             * @return singleton instance
             */
            private Object readResolve() {
                return getRoot();
            }

        };

        /**
         * Private constructor.
         * <p>
         * This class is a utility class, it should neither have a public nor a default constructor. This private
         * constructor prevents the compiler from generating one automatically.
         * </p>
         */
        private LazyRootHolder() {
        }

    }

}