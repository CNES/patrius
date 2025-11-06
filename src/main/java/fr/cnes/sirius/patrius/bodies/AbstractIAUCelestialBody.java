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
 * VERSION:4.14:OPENFD-161:22/08/2024:[PATRIUS] Adaptation de l'interface CelestialBody
 * car l'orientation n'est pas forcement IAU
 * VERSION:4.14:OPENFD-343:22/08/2024: Ajout de regles de codage dans le standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import fr.cnes.sirius.patrius.bodies.CelestialBodyOrientation.OrientationType;
import fr.cnes.sirius.patrius.bodies.bsp.BSPEphemerisLoader.SpiceJ2000ConventionEnum;
import fr.cnes.sirius.patrius.forces.gravity.GravityModel;
import fr.cnes.sirius.patrius.frames.CelestialBodyFrame;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfiguration;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.frames.transformations.TransformProvider;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;

/**
 * Abstract implementation of the {@link IAUCelestialBody} interface.
 * <p>
 * This abstract implementation provides basic services that can be shared by most implementations of the
 * {@link IAUCelestialBody} interface. It holds the gravitational attraction coefficient and build the body-centered
 * frames automatically using the definitions of pole and prime meridian specified by the IAU/IAG Working Group on
 * Cartographic Coordinates and Rotational Elements of the Planets and Satellites (WGCCRE).
 * </p>
 * 
 * @since 4.14
 */
@SuppressWarnings({ "PMD.NullAssignment", "PMD.ConstructorCallsOverridableMethod" })
public abstract class AbstractIAUCelestialBody extends AbstractCelestialBody implements IAUCelestialBody {

    /** Constant model string. */
    public static final String CONSTANT = "(constant model)";

    /** Mean model string. */
    public static final String MEAN = "(mean model)";

    /** True model string. */
    public static final String TRUE = "(true model)";

    /** Constant (equator) inertial, body-centered frame name. */
    public static final String INERTIAL_FRAME_CONSTANT_MODEL = INERTIAL_FRAME + SPACE + CONSTANT;

    /** Mean (equator) inertial, body-centered frame name. */
    public static final String INERTIAL_FRAME_MEAN_MODEL = INERTIAL_FRAME + SPACE + MEAN;

    /** True (equator) inertial, body-centered frame name. */
    public static final String INERTIAL_FRAME_TRUE_MODEL = INERTIAL_FRAME + SPACE + TRUE;

    /** Constant rotating, body-centered frame name. */
    public static final String ROTATING_FRAME_CONSTANT_MODEL = ROTATING_FRAME + SPACE + CONSTANT;

    /** Mean rotating, body-centered frame name. */
    public static final String ROTATING_FRAME_MEAN_MODEL = ROTATING_FRAME + SPACE + MEAN;

    /** True rotating, body-centered frame name. */
    public static final String ROTATING_FRAME_TRUE_MODEL = ROTATING_FRAME + SPACE + TRUE;

    /** Serializable UID. */
    private static final long serialVersionUID = 2864858425956948190L;

    /** Constant (equator) inertial, body-centered frame. */
    private CelestialBodyFrame constantInertialFrame;

    /** Mean (equator) inertial, body-centered frame. */
    private CelestialBodyFrame meanInertialFrame;

    /** True (equator) inertial, body-centered frame. */
    private CelestialBodyFrame trueInertialFrame;

    /** Constant rotating, body-centered frame. */
    private CelestialBodyFrame constantRotatingFrame;

    /** Mean rotating, body-centered frame. */
    private CelestialBodyFrame meanRotatingFrame;

    /** True rotating, body-centered frame. */
    private CelestialBodyFrame trueRotatingFrame;

    /**
     * Constructor.
     *
     * @param name
     *        name of the body
     * @param gravityModel
     *        gravity model
     * @param celestialBodyIAUOrientation
     *        celestial body IAU orientation
     * @param parentFrame
     *        parent frame (usually it should be the ICRF centered on the parent body)
     * @param convention spice convention for BSP frames
     * @param ephemeris ephemeris
     * @throws IllegalStateException
     *         if the celestial body orientation is nor a {@link CelestialBodyIAUOrientation} or a
     *         {@link CelestialBodyTabulatedOrientation} implementation
     */
    protected AbstractIAUCelestialBody(final String name, final GravityModel gravityModel,
                                       final CelestialBodyIAUOrientation celestialBodyIAUOrientation,
                                       final Frame parentFrame,
                                       final SpiceJ2000ConventionEnum convention,
                                       final CelestialBodyEphemeris ephemeris) {
        // Gravity model may not be set at this point (workaround)
        super(name, gravityModel, celestialBodyIAUOrientation, parentFrame, convention, ephemeris);

        // Instantiate the other frames
        this.setFrameTree();
    }

    /**
     * Constructor without ephemeris. Ephemeris can be defined later.
     *
     * @param name
     *        name of the body
     * @param gm
     *        gravitational attraction coefficient (in m<sup>3</sup>/s<sup>2</sup>)
     * @param celestialBodyIAUOrientation
     *        celestial body orientation
     * @param parentFrame
     *        parent frame (usually it should be the ICRF centered on the parent body)
     * @throws IllegalStateException
     *         if the celestial body orientation is nor a {@link CelestialBodyIAUOrientation} or a
     *         {@link CelestialBodyTabulatedOrientation} implementation
     */
    protected AbstractIAUCelestialBody(final String name, final double gm,
                                       final CelestialBodyIAUOrientation celestialBodyIAUOrientation,
                                       final Frame parentFrame) {
        this(name, gm, celestialBodyIAUOrientation, parentFrame, null);
    }

    /**
     * Constructor.
     *
     * @param name
     *        name of the body
     * @param gm
     *        gravitational attraction coefficient (in m<sup>3</sup>/s<sup>2</sup>)
     * @param celestialBodyIAUOrientation
     *        celestial body orientation
     * @param parentFrame
     *        parent frame (usually it should be the ICRF centered on the parent body)
     * @param ephemeris ephemeris
     * @throws IllegalStateException
     *         if the celestial body orientation is nor a {@link CelestialBodyIAUOrientation} or a
     *         {@link CelestialBodyTabulatedOrientation} implementation
     */
    protected AbstractIAUCelestialBody(final String name, final double gm,
                                       final CelestialBodyIAUOrientation celestialBodyIAUOrientation,
                                       final Frame parentFrame, final CelestialBodyEphemeris ephemeris) {
        super(name, gm, celestialBodyIAUOrientation, parentFrame, ephemeris);

        // Instantiate the other frames
        this.setFrameTree();
    }

    /**
     * Instantiate all the frames linked to the body.
     */
    private final void setFrameTree() {

        this.constantInertialFrame = new CelestialBodyFrame(this.getICRF(), new InertiallyOriented(
            this.getOrientation(), OrientationType.ICRF_TO_INERTIAL, IAUPoleModelType.CONSTANT),
            this.getName() + SPACE + INERTIAL_FRAME_CONSTANT_MODEL, true, this);
        this.meanInertialFrame = new CelestialBodyFrame(this.getICRF(), new InertiallyOriented(
            this.getOrientation(), OrientationType.ICRF_TO_INERTIAL, IAUPoleModelType.MEAN),
            this.getName() + SPACE + INERTIAL_FRAME_MEAN_MODEL, true, this);
        this.trueInertialFrame = new CelestialBodyFrame(this.getICRF(), new InertiallyOriented(
            this.getOrientation(), OrientationType.ICRF_TO_INERTIAL, IAUPoleModelType.TRUE),
            this.getName() + SPACE + INERTIAL_FRAME_TRUE_MODEL, true, this);

        // BodyOriented frames with IAU orientations are centered with inertial frames
        this.constantRotatingFrame = new CelestialBodyFrame(this.constantInertialFrame, new BodyOriented(
            this.getOrientation(), OrientationType.INERTIAL_TO_ROTATING, IAUPoleModelType.CONSTANT),
            this.getName() + SPACE + ROTATING_FRAME_CONSTANT_MODEL, false, this);
        this.meanRotatingFrame = new CelestialBodyFrame(this.meanInertialFrame, new BodyOriented(
            this.getOrientation(), OrientationType.INERTIAL_TO_ROTATING, IAUPoleModelType.MEAN),
            this.getName() + SPACE + ROTATING_FRAME_MEAN_MODEL, false, this);
        this.trueRotatingFrame = new CelestialBodyFrame(this.trueInertialFrame, new BodyOriented(
            this.getOrientation(), OrientationType.INERTIAL_TO_ROTATING, IAUPoleModelType.TRUE),
            this.getName() + SPACE + ROTATING_FRAME_TRUE_MODEL, false, this);
    }

    /** {@inheritDoc} */
    @Override
    public CelestialBodyFrame getInertialFrame(final IAUPoleModelType iauPoleIn) throws PatriusException {
        final CelestialBodyFrame frame;
        switch (iauPoleIn) {
            case CONSTANT:
                // Get an inertially oriented, body centered frame taking into account only
                // constant part of IAU pole data with respect to ICRF frame. The frame is
                // always bound to the body center, and its axes have a fixed orientation with
                // respect to other inertial frames.
                frame = this.constantInertialFrame;
                break;
            case MEAN:
                // Get an inertially oriented, body centered frame taking into account only
                // constant and secular part of IAU pole data with respect to ICRF frame.
                frame = this.meanInertialFrame;
                break;
            case TRUE:
                // Get an inertially oriented, body centered frame taking into account constant,
                // secular and harmonics part of IAU pole data with respect to ICRF frame.
                frame = this.trueInertialFrame;
                break;
            default:
                // The iauPole given as input is not implemented in this method.
                throw new PatriusException(PatriusMessages.INVALID_IAUPOLEMODELTYPE);
        }
        return frame;
    }

    /** {@inheritDoc} */
    @Override
    public CelestialBodyFrame getInertialFrame() throws PatriusException {
        // Return the true inertial frame, as the "true" is considered generic,
        // whereas the "mean" and "constant" are inherent to the IAU model
        return this.getInertialFrame(IAUPoleModelType.TRUE);
    }

    /** {@inheritDoc} */
    @Override
    public CelestialBodyFrame getRotatingFrame(final IAUPoleModelType iauPoleIn) throws PatriusException {
        final CelestialBodyFrame frame;
        switch (iauPoleIn) {
            case CONSTANT:
                // Get a body oriented, body centered frame taking into account only constant part
                // of IAU pole data with respect to inertially-oriented frame. The frame is always
                // bound to the body center, and its axes have a fixed orientation with respect to
                // the celestial body.
                frame = this.constantRotatingFrame;
                break;
            case MEAN:
                // Get a body oriented, body centered frame taking into account constant and secular
                // part of IAU pole data with respect to mean equator frame. The frame is always
                // bound to the body center, and its axes have a fixed orientation with respect to
                // the celestial body.
                frame = this.meanRotatingFrame;
                break;
            case TRUE:
                // Get a body oriented, body centered frame taking into account constant, secular
                // and harmonics part of IAU pole data with respect to true equator frame. The frame
                // is always bound to the body center, and its axes have a fixed orientation with
                // respect to the celestial body.
                frame = this.trueRotatingFrame;
                break;
            default:
                // The iauPole given as input is not implemented in this method.
                throw new PatriusException(PatriusMessages.INVALID_IAUPOLEMODELTYPE);
        }
        return frame;
    }

    /** {@inheritDoc} */
    @Override
    public final CelestialBodyFrame getRotatingFrame() throws PatriusException {
        // Return the true rotating frame, as the "true" is considered generic,
        // whereas the "mean" and "constant" are inherent to the IAU model
        return this.getRotatingFrame(IAUPoleModelType.TRUE);
    }

    /** {@inheritDoc} */
    @Override
    public CelestialBodyIAUOrientation getOrientation() {
        return (CelestialBodyIAUOrientation) super.getOrientation();
    }

    /**
     * Set a celestial body IAU orientation to define the body frames.
     * 
     * @param celestialBodyIAUOrientation
     *        the celestial body IAU orientation
     * @throws IllegalArgumentException
     *         if the celestial body orientation isn't an instance of {@link CelestialBodyIAUOrientation}
     */
    @Override
    public void setOrientation(final CelestialBodyOrientation celestialBodyIAUOrientation) {
        if (!(celestialBodyIAUOrientation instanceof CelestialBodyIAUOrientation)) {
            throw PatriusException
                .createIllegalArgumentException(PatriusMessages.ONLY_SUPPORTS_CELESTIAL_BODY_IAU_ORIENTATION);
        }
        super.setOrientation(celestialBodyIAUOrientation);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        // End commentary
        final String end = "\n";

        // String builder
        final StringBuilder builder = new StringBuilder(super.toString());

        // Add all frames
        try {
            builder
                .append("- Constant inertial frame: " + this.getInertialFrame(IAUPoleModelType.CONSTANT) + end);
            builder.append("- Mean inertial frame: " + this.getInertialFrame(IAUPoleModelType.MEAN) + end);
            builder.append("- True inertial frame: " + this.getInertialFrame(IAUPoleModelType.TRUE) + end);
        } catch (final PatriusException e) {
            builder.append("No inertial frame computed" + end);
            throw new PatriusRuntimeException(PatriusMessages.NO_INERTIAL_FRAME_COMPUTED, e);
        }

        try {
            builder
                .append("- Constant rotating frame: " + this.getRotatingFrame(IAUPoleModelType.CONSTANT) + end);
            builder.append("- Mean rotating frame: " + this.getRotatingFrame(IAUPoleModelType.MEAN) + end);
            builder.append("- True rotating frame: " + this.getRotatingFrame(IAUPoleModelType.TRUE) + end);
        } catch (final PatriusException e) {
            builder.append("No rotating frame computed" + end);
            throw new PatriusRuntimeException(PatriusMessages.NO_ROTATING_FRAME_COMPUTED, e);
        }

        // Return builder.toString
        return builder.toString();
    }

    /**
     * Provider for inertially oriented body centered frame transform.<br>
     * This include inertially oriented, mean of date and true of date frames which are different only with IAU pole
     * data taken into account.
     *
     * <p>
     * Spin derivative is never computed and is either 0 or null.<br>
     * No analytical formula is available for spin derivative.
     * </p>
     * <p>
     * Frames configuration is unused.
     * </p>
     * 
     * @serial serializable
     */
    private static class InertiallyOriented implements TransformProvider {

        /** Serializable UID. */
        private static final long serialVersionUID = 580403061742111487L;

        /** Celestial body IAU orientation. */
        private final CelestialBodyIAUOrientation celestialBodyIAUOrientation;

        /** Indicates the expected orientation type. */
        private final OrientationType orientationType;

        /** IAU pole type (only used for {@link CelestialBodyIAUOrientation}, can be {@code null} otherwise. */
        private final IAUPoleModelType iauPoleType;

        /**
         * Constructor.
         * 
         * @param celestialBodyIAUOrientation
         *        Celestial body IAU orientation
         * @param orientationType
         *        Indicates the expected orientation type
         * @param iauPoleType
         *        IAU pole type (only used for {@link CelestialBodyIAUOrientation}, can be {@code null} otherwise)
         */
        public InertiallyOriented(final CelestialBodyIAUOrientation celestialBodyIAUOrientation,
                                  final OrientationType orientationType, final IAUPoleModelType iauPoleType) {
            this.celestialBodyIAUOrientation = celestialBodyIAUOrientation;
            this.orientationType = orientationType;
            this.iauPoleType = iauPoleType;
        }

        /** {@inheritDoc} */
        @Override
        public Transform getTransform(final AbsoluteDate date) throws PatriusException {
            return this.getTransform(date, false);
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
            return this.getTransform(date, config, false);
        }

        /**
         * {@inheritDoc}
         * <p>
         * Spin derivative is never computed and is either 0 or null.<br>
         * No analytical formula is available for spin derivative.
         * </p>
         */
        @Override
        public Transform getTransform(final AbsoluteDate date, final boolean computeSpinDerivatives)
            throws PatriusException {
            return this.getTransform(date, FramesFactory.getConfiguration(), computeSpinDerivatives);
        }

        /**
         * {@inheritDoc}
         * <p>
         * Spin derivative is never computed and is either 0 or null.<br>
         * No analytical formula is available for spin derivative.
         * </p>
         * <p>
         * Frames configuration is unused.
         * </p>
         */
        @Override
        public Transform getTransform(final AbsoluteDate date, final FramesConfiguration config,
                                      final boolean computeSpinDerivatives)
            throws PatriusException {

            // Compute the angular coordinates
            final AngularCoordinates angularCoord =
                this.celestialBodyIAUOrientation.getAngularCoordinates(date, this.orientationType, this.iauPoleType);

            // Extract the rotation and rotation rate
            final Rotation r = angularCoord.getRotation();
            final Vector3D rDot = angularCoord.getRotationRate();

            // Manage acceleration initialization
            final Vector3D acc;
            if (computeSpinDerivatives) {
                acc = Vector3D.ZERO;
            } else {
                acc = null;
            }

            // Return rotation
            return new Transform(date, r, rDot, acc);
        }
    }

    /**
     * Provider for body oriented body centered frame transform.
     *
     * <p>
     * Spin derivative is never computed and is either 0 or null.<br>
     * No analytical formula is available for spin derivative. Spin is already computed by finite differences.
     * </p>
     * <p>
     * Frames configuration is unused.
     * </p>
     *
     * @serial serializable
     */
    private static class BodyOriented implements TransformProvider {

        /** Serializable UID. */
        private static final long serialVersionUID = 231174234869143938L;

        /** Celestial body IAU orientation. */
        private final CelestialBodyIAUOrientation celestialBodyIAUOrientation;

        /** Indicates the expected orientation type. */
        private final OrientationType orientationType;

        /** IAU pole type (only used for {@link CelestialBodyIAUOrientation}, can be {@code null} otherwise. */
        private final IAUPoleModelType iauPoleType;

        /**
         * Constructor.
         * 
         * @param celestialBodyIAUOrientation
         *        Celestial body IAU orientation
         * @param orientationType
         *        Indicates the expected orientation type
         * @param iauPoleType
         *        IAU pole type (only used for {@link CelestialBodyIAUOrientation}, can be {@code null} otherwise)
         */
        public BodyOriented(final CelestialBodyIAUOrientation celestialBodyIAUOrientation,
                            final OrientationType orientationType, final IAUPoleModelType iauPoleType) {
            this.celestialBodyIAUOrientation = celestialBodyIAUOrientation;
            this.orientationType = orientationType;
            this.iauPoleType = iauPoleType;
        }

        /** {@inheritDoc} */
        @Override
        public Transform getTransform(final AbsoluteDate date) throws PatriusException {
            return this.getTransform(date, false);
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
            return this.getTransform(date, config, false);
        }

        /**
         * {@inheritDoc}
         * <p>
         * Spin derivative is never computed and is either 0 or null.<br>
         * No analytical formula is available for spin derivative. Spin is already computed by finite differences.
         * </p>
         */
        @Override
        public Transform getTransform(final AbsoluteDate date, final boolean computeSpinDerivatives)
            throws PatriusException {
            return this.getTransform(date, FramesFactory.getConfiguration(), computeSpinDerivatives);
        }

        /**
         * {@inheritDoc}
         * <p>
         * Spin derivative is never computed and is either 0 or null.<br>
         * No analytical formula is available for spin derivative. Spin is already computed by finite differences.
         * </p>
         * <p>
         * Frames configuration is unused.
         * </p>
         */
        @Override
        public Transform getTransform(final AbsoluteDate date, final FramesConfiguration config,
                                      final boolean computeSpinDerivatives)
            throws PatriusException {

            // Compute the angular coordinates
            final AngularCoordinates angularCoord =
                this.celestialBodyIAUOrientation.getAngularCoordinates(date, this.orientationType, this.iauPoleType);

            // Extract the rotation and rotation rate
            final Rotation r = angularCoord.getRotation();
            final Vector3D rDot = angularCoord.getRotationRate();

            // Manage acceleration initialization
            final Vector3D acc;
            if (computeSpinDerivatives) {
                acc = Vector3D.ZERO;
            } else {
                acc = null;
            }

            // Return rotation
            return new Transform(date, r, rDot, acc);
        }
    }
}
