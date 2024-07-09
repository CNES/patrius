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

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfiguration;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.frames.transformations.TransformProvider;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Abstract implementation of the {@link CelestialBody} interface.
 * <p>
 * This abstract implementation provides basic services that can be shared by most implementations of the
 * {@link CelestialBody} interface. It holds the attraction coefficient and build the body-centered frames automatically
 * using the definitions of pole and prime meridianspecified by the IAU/IAG Working Group on Cartographic Coordinates
 * and Rotational Elements of the Planets and Satellites (WGCCRE).
 * </p>
 * 
 * @see IAUPole
 * @author Luc Maisonobe
 */
@SuppressWarnings("PMD.NullAssignment")
public abstract class AbstractCelestialBody implements CelestialBody {

    /** Serializable UID. */
    private static final long serialVersionUID = -8225707171826328799L;

    /** Tolerance for inertial frame rotation rate computation. */
    private static final double TOL = 1E-9;
    
    /** Name of the body. */
    private final String name;

    /** Attraction coefficient of the body (m<sup>3</sup>/s<sup>2</sup>). */
    private final double gm;

    /** IAU pole. */
    private final IAUPole iauPole;

    /** Inertially oriented, body-centered frame. */
    private final Frame inertialFrame;

    /** Body oriented, body-centered frame. */
    private final Frame bodyFrame;

    /**
     * Build an instance and the underlying frame.
     * 
     * @param nameIn
     *        name of the body
     * @param gmIn
     *        attraction coefficient (in m<sup>3</sup>/s<sup>2</sup>)
     * @param iauPoleIn
     *        IAU pole implementation
     * @param definingFrameIn
     *        frame in which celestial body coordinates are defined
     * @param inertialFrameName
     *        name to use for inertially oriented body centered frame
     * @param bodyFrameName
     *        name to use for body oriented body centered frame
     */
    protected AbstractCelestialBody(final String nameIn, final double gmIn,
        final IAUPole iauPoleIn, final Frame definingFrameIn,
        final String inertialFrameName, final String bodyFrameName) {
        this.name = nameIn;
        this.gm = gmIn;
        this.iauPole = iauPoleIn;
        this.inertialFrame =
            new Frame(definingFrameIn, new InertiallyOriented(definingFrameIn), inertialFrameName, true);
        this.bodyFrame =
            new Frame(this.inertialFrame, new BodyOriented(), bodyFrameName, false);
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return this.name;
    }

    /** {@inheritDoc} */
    @Override
    public double getGM() {
        return this.gm;
    }

    /** {@inheritDoc} */
    @Override
    public Frame getInertiallyOrientedFrame() {
        return this.inertialFrame;
    }

    /** {@inheritDoc} */
    @Override
    public Frame getBodyOrientedFrame() {
        return this.bodyFrame;
    }

    /** {@inheritDoc} */
    @Override
    public abstract PVCoordinates getPVCoordinates(AbsoluteDate date, Frame frame) throws PatriusException;

    /** {@inheritDoc} */
    @Override
    public String toString() {
        final String end = "\n";
        final StringBuilder builder = new StringBuilder();
        builder.append("- Name: " + name + end);
        builder.append("- Corps type: " + this.getClass().getSimpleName() + " class" + end);
        builder.append("- GM: " + gm + end);
        builder.append("- Inertial frame: " + getInertiallyOrientedFrame().toString() + end);
        builder.append("- Body frame: " + getBodyOrientedFrame().toString() + end);
        builder.append("- IAU pole origin: " + iauPole.toString() + " (" + iauPole.getClass() + ")" + end);
        return builder.toString();
    }
    

    /** Provider for inertially oriented body centered frame transform.
     * 
     * <p>
     * Spin derivative is never computed and is either 0 or null. No analytical formula is available for spin
     * derivative.
     * </p>
     * <p>Frames configuration is unused.</p>
     */
    private class InertiallyOriented implements TransformProvider {

        /** Serializable UID. */
        private static final long serialVersionUID = -8849993808761896559L;

        /** Frame in which celestial body coordinates are defined. */
        private final Frame definingFrame;

        /**
         * Simple constructor.
         * 
         * @param definingFrameIn
         *        frame in which celestial body coordinates are defined
         */
        public InertiallyOriented(final Frame definingFrameIn) {
            this.definingFrame = definingFrameIn;
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
        public Transform getTransform(final AbsoluteDate date,
                                      final FramesConfiguration config) throws PatriusException {
            return this.getTransform(date, config, false);
        }

        /**
         * {@inheritDoc}
         * <p>
         * Spin derivative is never computed and is either 0 or null. No analytical formula is available for spin
         * derivative.
         * </p>
         */
        @Override
        public Transform getTransform(final AbsoluteDate date,
                                      final boolean computeSpinDerivatives) throws PatriusException {
            return this.getTransform(date, FramesFactory.getConfiguration(), computeSpinDerivatives);
        }

        /**
         * {@inheritDoc}
         * <p>
         * Spin derivative is never computed and is either 0 or null. No analytical formula is available for spin
         * derivative.
         * </p>
         * <p>
         * Frames configuration is unused.
         * </p>
         */
        @Override
        public Transform getTransform(final AbsoluteDate date, final FramesConfiguration config,
                                      final boolean computeSpinDerivatives) throws PatriusException {
            // compute translation from parent frame to self
            final PVCoordinates pv = AbstractCelestialBody.this.getPVCoordinates(date, this.definingFrame);
            final Transform translation = new Transform(date, pv);

            // compute rotation from EME2000 frame to self,
            // as per the "Report of the IAU/IAG Working Group on Cartographic
            // Coordinates and Rotational Elements of the Planets and Satellites"
            // These definitions are common for all recent versions of this report
            // published every three years, the precise values of pole direction
            // and W angle coefficients may vary from publication year as models are
            // adjusted. These coefficients are not in this class, they are in the
            // specialized classes that do implement the getPole and getPrimeMeridianAngle
            // methods
            final Vector3D pole = AbstractCelestialBody.this.iauPole.getPole(date);
            Vector3D qNode = Vector3D.crossProduct(Vector3D.PLUS_K, pole);
            if (qNode.getNormSq() < Precision.SAFE_MIN) {
                qNode = Vector3D.PLUS_I;
            }

            // Rotation and spin in one single pass
            Rotation r;
            Vector3D rDot;
            if (!pole.equals(Vector3D.PLUS_K)) {
                final Vector3D poleDerivative = AbstractCelestialBody.this.iauPole.getPoleDerivative(date);
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
            final Transform rotation = new Transform(date, r, rDot, acc);

            // update transform from parent to self
            return new Transform(date, translation, rotation);
        }
    }

    /**
     * Provider for body oriented body centered frame transform.
     *
     * <p>
     * Spin derivative is never computed and is either 0 or null.
     * No analytical formula is available for spin derivative.
     * Spin is already computed by finite differences.
     * </p>
     * <p>Frames configuration is unused.</p>
     * 
     * @serial serializable.
     * */
    private class BodyOriented implements TransformProvider {

        /** Serializable UID. */
        private static final long serialVersionUID = -1859795611761959145L;

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
        public Transform getTransform(final AbsoluteDate date,
                                      final FramesConfiguration config) throws PatriusException {
            return this.getTransform(date, config, false);
        }

        /**
         * {@inheritDoc}
         * <p>
         * Spin derivative is never computed and is either 0 or null. No analytical formula is available for spin
         * derivative. Spin is already computed by finite differences.
         * </p>
         */
        @Override
        public Transform getTransform(final AbsoluteDate date,
                                      final boolean computeSpinDerivatives) throws PatriusException {
            return this.getTransform(date, FramesFactory.getConfiguration(), computeSpinDerivatives);
        }

        /**
         * {@inheritDoc}
         * <p>
         * Spin derivative is never computed and is either 0 or null. No analytical formula is available for spin
         * derivative. Spin is already computed by finite differences.
         * </p>
         * <p>
         * Frames configuration is unused.
         * </p>
         */
        @Override
        public Transform getTransform(final AbsoluteDate date, final FramesConfiguration config,
                                      final boolean computeSpinDerivatives) throws PatriusException {
            final double w = AbstractCelestialBody.this.iauPole.getPrimeMeridianAngle(date);
            final double wdot = AbstractCelestialBody.this.iauPole.getPrimeMeridianAngleDerivative(date);
            final Vector3D acc = computeSpinDerivatives ? Vector3D.ZERO : null;
            return new Transform(date, new Rotation(Vector3D.PLUS_K, w), new Vector3D(wdot, Vector3D.PLUS_K), acc);
        }

    }

}
