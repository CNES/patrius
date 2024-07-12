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
 * VERSION:4.11.1:DM:DM-49:30/06/2023:[PATRIUS] Extraction arbre des reperes SPICE et link avec CelestialBodyFactory
 * VERSION:4.11:DM:DM-3311:22/05/2023:[PATRIUS] Evolutions mineures sur CelestialBody, shape et reperes
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration gestion attractions gravitationnelles
 * VERSION:4.11:DM:DM-3300:22/05/2023:[PATRIUS] Nouvelle approche pour calcul position relative de 2 corps
 * VERSION:4.11:FA:FA-3314:22/05/2023:[PATRIUS] Anomalie evaluation ForceModel lorsque SpacecraftState en ITRF
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:FA:FA-3202:03/11/2022:[PATRIUS] Renommage dans UserCelestialBody
 * VERSION:4.9:DM:DM-3149:10/05/2022:[PATRIUS] Optimisation des reperes interplanetaires 
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:DM:DM-3166:10/05/2022:[PATRIUS] Definir l'ICRF comme repere racine 
 * VERSION:4.9:DM:DM-3127:10/05/2022:[PATRIUS] Ajout de deux methodes resize a l'interface GeometricBodyShape ...
 * VERSION:4.9:DM:DM-3164:10/05/2022:[PATRIUS] Amelioration de la gestion du GM (parametre gravitationnel)
 * VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:FA:FA-2948:15/11/2021:[PATRIUS] Harmonisation de l'affichage des informations sur les corps celestes 
 * VERSION:4.7:DM:DM-2888:18/05/2021:ajout des derivee des angles alpha,delta la classe UserIAUPole
 * VERSION:4.7:DM:DM-2872:18/05/2021:Calcul de l'accélération dans la classe QuaternionPolynomialProfile 
 * VERSION:4.7:DM:DM-2703:18/05/2021:Acces facilite aux donnees d'environnement (application interplanetaire) 
 * VERSION:4.7:DM:DM-2590:18/05/2021:Configuration des TransformProvider 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:489:12/01/2016:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:524:25/05/2016:serialization java doc
 * VERSION::FA:1777:04/10/2018:correct ICRF parent frame
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import fr.cnes.sirius.patrius.bodies.bsp.BSPEphemerisLoader.SpiceJ2000ConventionEnum;
import fr.cnes.sirius.patrius.forces.gravity.GravityModel;
import fr.cnes.sirius.patrius.forces.gravity.NewtonianGravityModel;
import fr.cnes.sirius.patrius.frames.CelestialBodyFrame;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfiguration;
import fr.cnes.sirius.patrius.frames.transformations.EME2000Provider;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.frames.transformations.TransformProvider;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Abstract implementation of the {@link CelestialBody} interface.
 * <p>
 * This abstract implementation provides basic services that can be shared by most implementations of the
 * {@link CelestialBody} interface. It holds the gravitational attraction coefficient and build the body-centered frames
 * automatically using the definitions of pole and prime meridian specified by the IAU/IAG Working Group on Cartographic
 * Coordinates and Rotational Elements of the Planets and Satellites (WGCCRE).
 * </p>
 *
 * @see IAUPole
 * @author Luc Maisonobe
 */
@SuppressWarnings("PMD.NullAssignment")
public abstract class AbstractCelestialBody implements CelestialBody {

    /** ICRF frame name. */
    public static final String ICRF_FRAME_NAME = "ICRF frame";

    /** EME2000 frame name. */
    public static final String EME2000_FRAME_NAME = "EME2000 frame";

    /** Constant (equator) inertial, body-centered frame name. */
    public static final String INERTIAL_FRAME_CONSTANT_MODEL = "Inertial frame (constant model)";

    /** Mean (equator) inertial, body-centered frame name. */
    public static final String INERTIAL_FRAME_MEAN_MODEL = "Inertial frame (mean model)";

    /** True (equator) inertial, body-centered frame name. */
    public static final String INERTIAL_FRAME_TRUE_MODEL = "Inertial frame (true model)";

    /** Constant rotating, body-centered frame name. */
    public static final String ROTATING_FRAME_CONSTANT_MODEL = "Rotating frame (constant model)";

    /** Mean rotating, body-centered frame name. */
    public static final String ROTATING_FRAME_MEAN_MODEL = "Rotating frame (mean model)";

    /** True rotating, body-centered frame name. */
    public static final String ROTATING_FRAME_TRUE_MODEL = "Rotating frame (true model)";

    /** Serializable UID. */
    private static final long serialVersionUID = -8225707171826328799L;

    /** Tolerance for inertial frame rotation rate computation. */
    private static final double TOL = 1E-9;

    /** Space. */
    private static final String SPACE = " ";

    /** Name of the body. */
    private final String name;

    /** ICRF oriented, body-centered frame. */
    private CelestialBodyFrame icrfFrame;

    /** EME2000 oriented, body-centered frame. */
    private CelestialBodyFrame eme2000Frame;

    /** IAU pole. */
    private IAUPole iauPole;

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

    /** Shape of the body. */
    private BodyShape shape;

    /** Gravitational attraction model of the body. */
    private GravityModel gravityModel;

    /** Body ephemeris. */
    private CelestialBodyEphemeris ephemeris;

    /**
     * Build an instance and the underlying frame.
     *
     * @param nameIn
     *        name of the body
     * @param gravityModelIn
     *        gravitationalAttraction model
     * @param iauPoleIn
     *        IAU pole implementation
     * @param parentFrame
     *        parent frame (usually it should be the ICRF centered on the parent body)
     */
    protected AbstractCelestialBody(final String nameIn, final GravityModel gravityModelIn,
            final IAUPole iauPoleIn, final Frame parentFrame) {
        name = nameIn;
        iauPole = iauPoleIn;
        icrfFrame = new CelestialBodyFrame(parentFrame, new ICRFOriented(parentFrame),
                nameIn + SPACE + ICRF_FRAME_NAME, true, this);
        gravityModel = gravityModelIn;

        // Instantiate the other frames
        setFrameTree(icrfFrame);
    }

    /**
     * Build an instance and the underlying frame.
     *
     * @param nameIn
     *        name of the body
     * @param gravityModelIn
     *        gravitationalAttraction model
     * @param iauPoleIn
     *        IAU pole implementation
     * @param parentFrame
     *        parent frame (usually it should be the ICRF centered on the parent body)
     * @param convention spice convention for BSP frames
     */
    protected AbstractCelestialBody(final String nameIn, final GravityModel gravityModelIn,
            final IAUPole iauPoleIn, final Frame parentFrame, final SpiceJ2000ConventionEnum convention) {
        name = nameIn;
        iauPole = iauPoleIn;
        gravityModel = gravityModelIn;
        if (convention.equals(SpiceJ2000ConventionEnum.ICRF)) {
            // ICRF
            icrfFrame = new CelestialBodyFrame(parentFrame, new ICRFOriented(parentFrame),
                    nameIn + SPACE + ICRF_FRAME_NAME, true, this);
            setFrameTree(icrfFrame);
        } else {
            // EME2000
            eme2000Frame = new CelestialBodyFrame(parentFrame, new ICRFOriented(parentFrame), name + SPACE
                    + EME2000_FRAME_NAME, true, this);
            // Instantiate the other frames
            setFrameTree(eme2000Frame);
        }
    }

    /**
     * Build an instance and the underlying frame.
     *
     * @param nameIn
     *        name of the body
     * @param gmIn
     *        gravitational attraction coefficient (in m<sup>3</sup>/s<sup>2</sup>)
     * @param iauPoleIn
     *        IAU pole implementation
     * @param parentFrame
     *        parent frame (usually it should be the ICRF centered on the parent body)
     */
    protected AbstractCelestialBody(final String nameIn, final double gmIn, final IAUPole iauPoleIn,
            final Frame parentFrame) {
        name = nameIn;
        iauPole = iauPoleIn;
        icrfFrame = new CelestialBodyFrame(parentFrame, new ICRFOriented(parentFrame),
            nameIn + SPACE + ICRF_FRAME_NAME, true, this);
        gravityModel = new NewtonianGravityModel(icrfFrame, gmIn);

        // Instantiate the other frames
        setFrameTree(icrfFrame);
    }

    /**
     * Build an instance and the underlying frame.
     *
     * @param icrf
     *        ICRF frame
     * @param nameIn
     *        name of the body
     * @param gravityModelIn
     *        gravitational attraction model
     * @param iauPoleIn
     *        IAU pole implementation
     */
    protected AbstractCelestialBody(final CelestialBodyFrame icrf, final String nameIn,
                                    final GravityModel gravityModelIn, final IAUPole iauPoleIn) {
        name = nameIn;
        iauPole = iauPoleIn;
        icrfFrame = icrf;
        gravityModel = gravityModelIn;

        // Instantiate the other frames
        setFrameTree(icrfFrame);
    }

    /**
     * Instantiate all the frames linked to the body.
     * @param rootFrame root frame
     */
    private final void setFrameTree(final Frame rootFrame) {
        // Transform from EME2000 body-centered parent (= ICRF body-centered) to EME2000
        // body-centered is same as transformation from GCRF to EME2000 provided by EME2000Provider
        if (eme2000Frame == null) {
            eme2000Frame = new CelestialBodyFrame(rootFrame, new EME2000Provider(), name + SPACE + EME2000_FRAME_NAME,
                    true, this);
        }
        if (icrfFrame == null) {
            icrfFrame = new CelestialBodyFrame(rootFrame, new EME2000Provider().getTransform(null).getInverse(), name
                    + SPACE + ICRF_FRAME_NAME, true, this);
        }
        constantInertialFrame = new CelestialBodyFrame(icrfFrame, new InertiallyOriented(IAUPoleModelType.CONSTANT),
                name + SPACE + INERTIAL_FRAME_CONSTANT_MODEL, true, this);
        meanInertialFrame = new CelestialBodyFrame(icrfFrame, new InertiallyOriented(IAUPoleModelType.MEAN), name
                + SPACE + INERTIAL_FRAME_MEAN_MODEL, true, this);
        trueInertialFrame = new CelestialBodyFrame(icrfFrame, new InertiallyOriented(IAUPoleModelType.TRUE), name
                + SPACE + INERTIAL_FRAME_TRUE_MODEL, true, this);
        constantRotatingFrame = new CelestialBodyFrame(constantInertialFrame, new BodyOriented(
                IAUPoleModelType.CONSTANT), name + SPACE + ROTATING_FRAME_CONSTANT_MODEL, false, this);
        meanRotatingFrame = new CelestialBodyFrame(meanInertialFrame, new BodyOriented(IAUPoleModelType.MEAN), name
                + SPACE + ROTATING_FRAME_MEAN_MODEL, false, this);
        trueRotatingFrame = new CelestialBodyFrame(trueInertialFrame, new BodyOriented(IAUPoleModelType.TRUE), name
                + SPACE + ROTATING_FRAME_TRUE_MODEL, false, this);
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return name;
    }

    /** {@inheritDoc} */
    @Override
    public CelestialBodyFrame getICRF() {
        return icrfFrame;
    }

    /** {@inheritDoc} */
    @Override
    public CelestialBodyFrame getEME2000() {
        return eme2000Frame;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CelestialBodyFrame getInertialFrame(final IAUPoleModelType iauPoleIn) throws PatriusException {
        final CelestialBodyFrame frame;
        switch (iauPoleIn) {
            case CONSTANT:
                // Get an inertially oriented, body centered frame taking into account only
                // constant part of IAU pole data with respect to ICRF frame. The frame is
                // always bound to the body center, and its axes have a fixed orientation with
                // respect to other inertial frames.
                frame = constantInertialFrame;
                break;
            case MEAN:
                // Get an inertially oriented, body centered frame taking into account only
                // constant and secular part of IAU pole data with respect to ICRF frame.
                frame = meanInertialFrame;
                break;
            case TRUE:
                // Get an inertially oriented, body centered frame taking into account constant,
                // secular and harmonics part of IAU pole data with respect to ICRF frame.
                frame = trueInertialFrame;
                break;
            default:
                // The iauPole given as input is not implemented in this method.
                throw new PatriusException(PatriusMessages.INVALID_IAUPOLEMODELTYPE);
        }
        return frame;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CelestialBodyFrame getRotatingFrame(final IAUPoleModelType iauPoleIn) throws PatriusException {
        final CelestialBodyFrame frame;
        switch (iauPoleIn) {
            case CONSTANT:
                // Get a body oriented, body centered frame taking into account only constant part
                // of IAU pole data with respect to inertially-oriented frame. The frame is always
                // bound to the body center, and its axes have a fixed orientation with respect to
                // the celestial body.
                frame = constantRotatingFrame;
                break;
            case MEAN:
                // Get a body oriented, body centered frame taking into account constant and secular
                // part of IAU pole data with respect to mean equator frame. The frame is always
                // bound to the body center, and its axes have a fixed orientation with respect to
                // the celestial body.
                frame = meanRotatingFrame;
                break;
            case TRUE:
                // Get a body oriented, body centered frame taking into account constant, secular
                // and harmonics part of IAU pole data with respect to true equator frame. The frame
                // is always bound to the body center, and its axes have a fixed orientation with
                // respect to the celestial body.
                frame = trueRotatingFrame;
                break;
            default:
                // The iauPole given as input is not implemented in this method.
                throw new PatriusException(PatriusMessages.INVALID_IAUPOLEMODELTYPE);
        }
        return frame;
    }

    /** {@inheritDoc} */
    @Override
    public double getGM() {
        return getGravityModel().getMu();
    }

    /** {@inheritDoc} */
    @Override
    public void setGM(final double gmIn) {
        getGravityModel().setMu(gmIn);
    }

    /** {@inheritDoc} */
    @Override
    public IAUPole getIAUPole() {
        return iauPole;
    }

    /** {@inheritDoc} */
    @Override
    public void setIAUPole(final IAUPole iauPoleIn) {
        iauPole = iauPoleIn;
    }

    /** {@inheritDoc} */
    @Override
    public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
        return icrfFrame.getPVCoordinates(date, frame);
    }

    /** {@inheritDoc} */
    @Override
    public Frame getNativeFrame(final AbsoluteDate date, final Frame frame) throws PatriusException {
        return icrfFrame;
    }

    /** {@inheritDoc} */
    @Override
    public GravityModel getGravityModel() {
        return gravityModel;
    }

    /** {@inheritDoc} */
    @Override
    public void setGravityModel(final GravityModel modelIn) {
        gravityModel = modelIn;
    }

    /** {@inheritDoc} */
    @Override
    public BodyShape getShape() {
        return shape;
    }

    /** {@inheritDoc} */
    @Override
    public void setShape(final BodyShape shapeIn) {
        shape = shapeIn;
    }

    /** {@inheritDoc} */
    @Override
    public CelestialBodyEphemeris getEphemeris() {
        return ephemeris;
    }

    /** {@inheritDoc} */
    @Override
    public void setEphemeris(final CelestialBodyEphemeris ephemerisIn) {
        ephemeris = ephemerisIn;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        // End commentary
        final String end = "\n";
        // String builder
        final StringBuilder builder = new StringBuilder();
        // Add data
        builder.append("- Name: " + name + end);
        builder.append("- Corps type: " + this.getClass().getSimpleName() + " class" + end);
        builder.append("- GM: " + getGM() + end);
        // Add all frames
        builder.append("- ICRF frame: " + getICRF().toString() + end);
        builder.append("- EME2000 frame: " + getEME2000().toString() + end);
        try {
            builder.append("- Inertial frame: " + getInertialFrame(IAUPoleModelType.CONSTANT).toString() + end);
            builder.append("- Mean equator frame: " + getInertialFrame(IAUPoleModelType.MEAN).toString() + end);
            builder.append("- True equator frame: " + getInertialFrame(IAUPoleModelType.TRUE).toString() + end);
        } catch (final PatriusException e) {
            builder.append("No inertial frame computed" + end);
        }
        try {
            builder.append("- Constant rotating frame: " + getRotatingFrame(IAUPoleModelType.CONSTANT).toString());
            builder.append(end);
            builder.append("- Mean rotating frame: " + getRotatingFrame(IAUPoleModelType.MEAN).toString() + end);
            builder.append("- True rotating frame: " + getRotatingFrame(IAUPoleModelType.TRUE).toString() + end);
        } catch (final PatriusException e) {
            builder.append("No rotating frame computed" + end);
        }
        if (iauPole == null) {
            builder.append("- IAU pole origin: undefined" + end);
        } else {
            builder.append("- IAU pole origin: " + iauPole.toString() + " (" + iauPole.getClass() + ")" + end);
        }
        // Return builder.toString
        return builder.toString();
    }

    /**
     * Provider for ICRF oriented body centered frame transform.
     *
     * <p>
     * Spin derivative is never computed and is either 0 or null. No analytical formula is available
     * for spin derivative.
     * </p>
     * <p>
     * Frames configuration is unused.
     * </p>
     */
    private class ICRFOriented implements TransformProvider {

        /** Serializable UID. */
        private static final long serialVersionUID = -8849993808761896559L;

        /** parent frame (usually it should be the ICRF centered on the parent body) */
        private final Frame parentFrame;

        /**
         * Simple constructor.
         *
         * @param parentFrame
         *        parent frame (usually it should be the ICRF centered on the parent body)
         */
        public ICRFOriented(final Frame parentFrame) {
            this.parentFrame = parentFrame;
        }

        /** {@inheritDoc} */
        @Override
        public Transform getTransform(final AbsoluteDate date) throws PatriusException {
            return this.getTransform(date, FramesFactory.getConfiguration(), false);
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
         * Spin derivative is never computed and is either 0 or null. No analytical formula is
         * available for spin derivative.
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
         * Spin derivative is never computed and is either 0 or null.
         * </p>
         * <p>
         * Frames configuration is unused.
         * </p>
         */
        @Override
        public Transform getTransform(final AbsoluteDate date, final FramesConfiguration config,
                final boolean computeSpinDerivatives) throws PatriusException {
            // compute translation from parent frame to self
            final PVCoordinates pv = getEphemeris().getPVCoordinates(date, parentFrame);
            return new Transform(date, pv);
        }
    }

    /**
     * Provider for inertially oriented body centered frame transform.
     * This include inertially oriented, mean of date and true of date frames which are different
     * only with IAU pole
     * data taken into account.
     *
     * <p>
     * Spin derivative is never computed and is either 0 or null. No analytical formula is available
     * for spin derivative.
     * </p>
     * <p>
     * Frames configuration is unused.
     * </p>
     */
    private class InertiallyOriented implements TransformProvider {

        /** Serializable UID. */
        private static final long serialVersionUID = -8849993808761896559L;

        /** IAU pole type. */
        private final IAUPoleModelType iauPoleType;

        /**
         * Constructor.
         * @param iauPoleType IAUPole type
         */
        public InertiallyOriented(final IAUPoleModelType iauPoleType) {
            this.iauPoleType = iauPoleType;
        }

        /** {@inheritDoc} */
        @Override
        public Transform getTransform(final AbsoluteDate date) throws PatriusException {
            return this.getTransform(date, FramesFactory.getConfiguration(), false);
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
         * Spin derivative is never computed and is either 0 or null. No analytical formula is
         * available for spin derivative.
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
         * Spin derivative is never computed and is either 0 or null. No analytical formula is
         * available for spin derivative.
         * </p>
         * <p>
         * Frames configuration is unused.
         * </p>
         */
        @Override
        public Transform getTransform(final AbsoluteDate date, final FramesConfiguration config,
                final boolean computeSpinDerivatives) throws PatriusException {

            // compute rotation from EME2000 frame to self,
            // as per the "Report of the IAU/IAG Working Group on Cartographic
            // Coordinates and Rotational Elements of the Planets and Satellites"
            // These definitions are common for all recent versions of this report
            // published every three years, the precise values of pole direction
            // and W angle coefficients may vary from publication year as models are
            // adjusted. These coefficients are not in this class, they are in the
            // specialized classes that do implement the getPole and getPrimeMeridianAngle
            // methods
            final Vector3D pole = iauPole.getPole(date, iauPoleType);
            Vector3D qNode = Vector3D.crossProduct(Vector3D.PLUS_K, pole);
            if (qNode.getNormSq() < Precision.SAFE_MIN) {
                qNode = Vector3D.PLUS_I;
            }

            // Rotation and spin in one single pass
            Rotation r;
            Vector3D rDot;
            if (!pole.equals(Vector3D.PLUS_K)) {
                final Vector3D poleDerivative = iauPole.getPoleDerivative(date, iauPoleType);
                try {
                    final AngularCoordinates coord = new AngularCoordinates(new PVCoordinates(Vector3D.PLUS_K,
                            Vector3D.ZERO), new PVCoordinates(Vector3D.PLUS_I, Vector3D.ZERO), new PVCoordinates(pole,
                            poleDerivative), new PVCoordinates(qNode, Vector3D.crossProduct(Vector3D.PLUS_K,
                            poleDerivative)), TOL).revert();
                    r = coord.getRotation();
                    rDot = coord.getRotationRate();
                } catch (final PatriusException e) {
                    // Spin cannot be computed (inconsistent pole derivative)
                    r = new Rotation(Vector3D.PLUS_K, Vector3D.PLUS_I, pole, qNode);
                    rDot = Vector3D.ZERO;
                }
            } else {
                // Specific case: pole is along +k, rotation is identity
                r = Rotation.IDENTITY;
                rDot = Vector3D.ZERO;
            }

            // compute rotation from parent frame to self
            Vector3D acc = null;
            if (computeSpinDerivatives) {
                acc = Vector3D.ZERO;
            }
            // Return rotation
            return new Transform(date, r, rDot, acc);
        }
    }

    /**
     * Provider for body oriented body centered frame transform.
     *
     * <p>
     * Spin derivative is never computed and is either 0 or null. No analytical formula is available
     * for spin derivative. Spin is already computed by finite differences.
     * </p>
     * <p>
     * Frames configuration is unused.
     * </p>
     *
     * @serial serializable.
     * */
    private class BodyOriented implements TransformProvider {

        /** Serializable UID. */
        private static final long serialVersionUID = -1859795611761959145L;

        /** IAU pole type. */
        private final IAUPoleModelType iauPoleType;

        /**
         * Constructor.
         * @param iauPoleType IAUPole type
         */
        public BodyOriented(final IAUPoleModelType iauPoleType) {
            this.iauPoleType = iauPoleType;
        }

        /** {@inheritDoc} */
        @Override
        public Transform getTransform(final AbsoluteDate date) throws PatriusException {
            return this.getTransform(date, FramesFactory.getConfiguration(), false);
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
         * Spin derivative is never computed and is either 0 or null. No analytical formula is
         * available for spin derivative. Spin is already computed by finite differences.
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
         * Spin derivative is never computed and is either 0 or null. No analytical formula is
         * available for spin derivative. Spin is already computed by finite differences.
         * </p>
         * <p>
         * Frames configuration is unused.
         * </p>
         */
        @Override
        public Transform getTransform(final AbsoluteDate date, final FramesConfiguration config,
                final boolean computeSpinDerivatives) throws PatriusException {
            final double w = iauPole.getPrimeMeridianAngle(date, iauPoleType);
            final double wdot = iauPole.getPrimeMeridianAngleDerivative(date, iauPoleType);
            final Vector3D acc = computeSpinDerivatives ? Vector3D.ZERO : null;
            return new Transform(date, new Rotation(Vector3D.PLUS_K, w), new Vector3D(wdot, Vector3D.PLUS_K), acc);
        }
    }
}
