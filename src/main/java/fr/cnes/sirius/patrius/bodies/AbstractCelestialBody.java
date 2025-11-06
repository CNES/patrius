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
 * VERSION:4.14:OPENFD-:22/08/2024:
 * VERSION:4.14:OPENFD-161:22/08/2024:[PATRIUS] Adaptation de l'interface CelestialBody
 * car l'orientation n'est pas forcement IAU
 * VERSION:4.14:OPENFD-245:22/08/2024: Ajout d'un constructeur dans AbstractCelestialBody
 * VERSION:4.14:OPENFD-343:22/08/2024: Ajout de regles de codage dans le standard de codage DYNVOL
 * VERSION:4.13:FA:FA-112:08/12/2023:[PATRIUS] Probleme si Earth est utilise comme corps pivot pour mar097.bsp
 * VERSION:4.13:DM:DM-103:08/12/2023:[PATRIUS] Optimisation du CIRFProvider
 * VERSION:4.13:DM:DM-3:08/12/2023:[PATRIUS] Distinction entre corps celestes et barycentres
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.13:DM:DM-120:08/12/2023:[PATRIUS] Merge de la branche patrius-for-lotus dans Patrius
 * VERSION:4.13:DM:DM-5:08/12/2023:[PATRIUS] Orientation d'un corps celeste sous forme de quaternions
 * VERSION:4.13:FA:FA-111:08/12/2023:[PATRIUS] Problemes lies à  l'utilisation des bsp
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

import fr.cnes.sirius.patrius.bodies.CelestialBodyOrientation.OrientationType;
import fr.cnes.sirius.patrius.bodies.bsp.BSPEphemerisLoader.SpiceJ2000ConventionEnum;
import fr.cnes.sirius.patrius.forces.gravity.GravityModel;
import fr.cnes.sirius.patrius.forces.gravity.NewtonianGravityModel;
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

/**
 * Abstract implementation of the {@link CelestialBody} interface.
 * <p>
 * This abstract implementation provides basic services that can be shared by most implementations of the
 * {@link CelestialBody} interface. It holds the gravitational attraction coefficient and build the body-centered
 * frames.
 * </p>
 *
 * @see CelestialBodyOrientation
 * @author Luc Maisonobe
 */
@SuppressWarnings({ "PMD.NullAssignment", "PMD.ConstructorCallsOverridableMethod" })
public abstract class AbstractCelestialBody extends AbstractCelestialPoint implements CelestialBody {

    /** Inertial, body-centered frame name. */
    public static final String INERTIAL_FRAME = "Inertial frame";

    /** Body-centered frame name. */
    public static final String ROTATING_FRAME = "Rotating frame";

    /** Space. */
    public static final char SPACE = ' ';

    /** Serializable UID. */
    private static final long serialVersionUID = -8225707171826328799L;

    /** Celestial body orientation. */
    private CelestialBodyOrientation celestialBodyOrientation;

    /** Inertial body-centered frame. **/
    private CelestialBodyFrame inertialFrame;

    /** Rotating body-centered frame. **/
    private CelestialBodyFrame rotatingFrame;

    /** Shape of the body. */
    private BodyShape shape;

    /** Gravitational attraction model of the body. */
    private GravityModel gravityModel;

    /**
     * Constructor.
     *
     * @param name
     *        name of the body
     * @param gravityModel
     *        gravity model
     * @param celestialBodyOrientation
     *        celestial body orientation
     * @param parentFrame
     *        parent frame (usually it should be the ICRF centered on the parent body)
     * @param convention spice convention for BSP frames
     * @param ephemeris ephemeris
     * @throws IllegalStateException
     *         if the celestial body orientation is nor a {@link CelestialBodyIAUOrientation} or a
     *         {@link CelestialBodyTabulatedOrientation} implementation
     */
    protected AbstractCelestialBody(final String name, final GravityModel gravityModel,
                                    final CelestialBodyOrientation celestialBodyOrientation,
                                    final Frame parentFrame,
                                    final SpiceJ2000ConventionEnum convention,
                                    final CelestialBodyEphemeris ephemeris) {
        // Gravity model may not be set at this point (workaround)
        super(name, gravityModel == null ? 0. : gravityModel.getMu(), ephemeris, parentFrame, convention);
        this.celestialBodyOrientation = celestialBodyOrientation;
        this.gravityModel = gravityModel;

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
     * @param celestialBodyOrientation
     *        celestial body orientation
     * @param parentFrame
     *        parent frame (usually it should be the ICRF centered on the parent body)
     * @throws IllegalStateException
     *         if the celestial body orientation is nor a {@link CelestialBodyIAUOrientation} or a
     *         {@link CelestialBodyTabulatedOrientation} implementation
     */
    protected AbstractCelestialBody(final String name, final double gm,
                                    final CelestialBodyOrientation celestialBodyOrientation,
                                    final Frame parentFrame) {
        this(name, gm, celestialBodyOrientation, parentFrame, null);
    }

    /**
     * Constructor.
     *
     * @param name
     *        name of the body
     * @param gm
     *        gravitational attraction coefficient (in m<sup>3</sup>/s<sup>2</sup>)
     * @param celestialBodyOrientation
     *        celestial body orientation
     * @param parentFrame
     *        parent frame (usually it should be the ICRF centered on the parent body)
     * @param ephemeris ephemeris
     * @throws IllegalStateException
     *         if the celestial body orientation is nor a {@link CelestialBodyIAUOrientation} or a
     *         {@link CelestialBodyTabulatedOrientation} implementation
     */
    protected AbstractCelestialBody(final String name, final double gm,
                                    final CelestialBodyOrientation celestialBodyOrientation,
                                    final Frame parentFrame, final CelestialBodyEphemeris ephemeris) {
        super(name, gm, parentFrame, ephemeris);
        this.celestialBodyOrientation = celestialBodyOrientation;
        this.gravityModel = new NewtonianGravityModel(this.getICRF(), gm);

        // Instantiate the other frames
        this.setFrameTree();
    }

    /**
     * Constructor with user-defined ICRF frame.
     *
     * @param name
     *        name of the body
     * @param gm
     *        gravitational attraction coefficient (in m<sup>3</sup>/s<sup>2</sup>)
     * @param ephemeris
     *        ephemeris
     * @param icrf
     *        icrf frame centered on this body
     */
    protected AbstractCelestialBody(final String name, final double gm,
                                    final CelestialBodyEphemeris ephemeris, final CelestialBodyFrame icrf) {
        super(name, gm, ephemeris, icrf);
        this.gravityModel = new NewtonianGravityModel(icrf, gm);
    }

    /**
     * Instantiate all the frames linked to the body.
     *
     * @throws IllegalStateException
     *         if the {@link getOrientation() celestial body orientation} is nor a {@link CelestialBodyIAUOrientation}
     *         or a {@link CelestialBodyTabulatedOrientation} implementation
     */
    private final void setFrameTree() {

        if (this.celestialBodyOrientation instanceof CelestialBodyIAUOrientation
                || this.celestialBodyOrientation == null) {
            // IAU orientation case: methods using these elements should be overloaded in AbstractIAUCelestialBody
            this.inertialFrame = null;
            this.rotatingFrame = null;

        } else if (this.celestialBodyOrientation instanceof CelestialBodyTabulatedOrientation) {
            // Tabulated orientation case:
            this.inertialFrame = new CelestialBodyFrame(this.getICRF(), new InertiallyOriented(
                this.celestialBodyOrientation, OrientationType.ICRF_TO_INERTIAL),
                this.getName() + SPACE + INERTIAL_FRAME, true, this);

            // BodyOriented frames with tabulated orientation are centered with ICRF
            this.rotatingFrame = new CelestialBodyFrame(this.getICRF(), new BodyOriented(
                this.celestialBodyOrientation, OrientationType.ICRF_TO_ROTATING),
                this.getName() + SPACE + ROTATING_FRAME, false, this);

        } else {
            // Non supported celestial body orientation type
            throw PatriusException.createIllegalStateException(PatriusMessages.NON_SUPPORTED_BODY_ORIENTATION_TYPE);
        }
    }

    /** {@inheritDoc} */
    @Override
    public CelestialBodyOrientation getOrientation() {
        return this.celestialBodyOrientation;
    }

    /** {@inheritDoc} */
    @Override
    public void setOrientation(final CelestialBodyOrientation celestialBodyOrientationIn) {
        this.celestialBodyOrientation = celestialBodyOrientationIn;
    }

    /** {@inheritDoc} */
    @Override
    public CelestialBodyFrame getInertialFrame() throws PatriusException {
        return this.inertialFrame;
    }

    /** {@inheritDoc} */
    @Override
    public CelestialBodyFrame getRotatingFrame() throws PatriusException {
        return this.rotatingFrame;
    }

    /** {@inheritDoc} */
    @Override
    public BodyShape getShape() {
        return this.shape;
    }

    /** {@inheritDoc} */
    @Override
    public final void setShape(final BodyShape shapeIn) {
        this.shape = shapeIn;
    }

    /** {@inheritDoc} */
    @Override
    public GravityModel getGravityModel() {
        return this.gravityModel;
    }

    /** {@inheritDoc} */
    @Override
    public final void setGravityModel(final GravityModel modelIn) {
        this.gravityModel = modelIn;
    }

    /** {@inheritDoc} */
    @Override
    public double getGM() {
        return this.getGravityModel().getMu();
    }

    /** {@inheritDoc} */
    @Override
    public void setGM(final double gmIn) {
        super.setGM(gmIn);
        this.getGravityModel().setMu(gmIn);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        // End commentary
        final String end = "\n";

        // String builder
        final StringBuilder builder = new StringBuilder(super.toString());
        // Add orientation
        if (this.celestialBodyOrientation == null) {
            builder.append("- orientation: undefined" + end);
        } else {
            builder.append("- orientation: " + this.celestialBodyOrientation + " ("
                    + this.celestialBodyOrientation.getClass() + ')' + end);
        }

        // Add all frames
        if (this.inertialFrame != null) {
            builder.append("- Inertial frame: " + this.inertialFrame + end);
        }
        if (this.rotatingFrame != null) {
            builder.append("- Rotating frame: " + this.rotatingFrame + end);
        }

        // Return builder.toString
        return builder.toString();
    }

    /**
     * Provider for inertially oriented body centered frame transform.<br>
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
        private static final long serialVersionUID = -8849993808761896559L;

        /** Celestial body orientation. */
        private final CelestialBodyOrientation celestialBodyOrientation;

        /** Indicates the expected orientation type. */
        private final OrientationType orientationType;

        /**
         * Constructor.
         *
         * @param celestialBodyOrientation
         *        Celestial body orientation
         * @param orientationType
         *        Indicates the expected orientation type
         */
        public InertiallyOriented(final CelestialBodyOrientation celestialBodyOrientation,
                                  final OrientationType orientationType) {
            this.celestialBodyOrientation = celestialBodyOrientation;
            this.orientationType = orientationType;
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
                this.celestialBodyOrientation.getAngularCoordinates(date, this.orientationType);

            // Extrat the rotation and rotation rate
            final Rotation r = angularCoord.getRotation();
            final Vector3D rDot = angularCoord.getRotationRate();

            // Manage acceleration initialisation
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
        private static final long serialVersionUID = -1859795611761959145L;

        /** Celestial body orientation. */
        private final CelestialBodyOrientation celestialBodyOrientation;

        /** Indicates the expected orientation type. */
        private final OrientationType orientationType;

        /**
         * Constructor.
         *
         * @param celestialBodyOrientation
         *        Celestial body orientation
         * @param orientationType
         *        Indicates the expected orientation type
         */
        public BodyOriented(final CelestialBodyOrientation celestialBodyOrientation,
                            final OrientationType orientationType) {
            this.celestialBodyOrientation = celestialBodyOrientation;
            this.orientationType = orientationType;
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
                this.celestialBodyOrientation.getAngularCoordinates(date, this.orientationType);

            // Extrat the rotation and rotation rate
            final Rotation r = angularCoord.getRotation();
            final Vector3D rDot = angularCoord.getRotationRate();

            // Manage acceleration initialisation
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
