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
* VERSION:4.8:DM:DM-2975:15/11/2021:[PATRIUS] creation du repere synodique via un LOF 
* VERSION:4.6:FA:FA-2539:27/01/2021:[PATRIUS] Anomalie dans le calcul du vecteur rotation des LOF [iteration 2] 
 * VERSION:4.5:DM:DM-2465:27/05/2020:Refactoring calculs de ralliements
 * VERSION:4.5:FA:FA-2464:27/05/2020:Anomalie dans le calcul du vecteur rotation des LOF
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:271:05/09/2014:Definitions anomalies LVLH and VVLH
 * VERSION::FA:367:21/11/2014:Recette V2.3 corrections (from FA 271)
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::FA:1873:05/10/2018:javadoc correction
 * VERSION::FA:1867:22/10/2018: Fix the TNW frame's rotation issue and update the LOFType
 * class
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames;

import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * Enumerate for different types of Local Orbital Frames.
 * Formulas comes from the CNES document: DYNVOL-NT-ORB/MOD-1245-CNES Ed. 01 Rev. 00.
 * 
 * @author Luc Maisonobe
 */
public enum LOFType {

    /**
     * Constant for TNW frame
     * (X axis aligned with velocity, Z axis aligned with orbital momentum).
     * <p>
     * The axes of this frame are parallel to the axes of the {@link #VNC} frame:
     * <ul>
     * <li>X<sub>TNW</sub> = X<sub>VNC</sub></li>
     * <li>Y<sub>TNW</sub> = -Z<sub>VNC</sub></li>
     * <li>Z<sub>TNW</sub> = Y<sub>VNC</sub></li>
     * </ul>
     * </p>
     *
     * <p>
     * Important notice: if PV acceleration is null, an approximation is used considering a dynamic circular orbit.
     * </p>
     * @see #VNC
     */
    TNW {
        /** {@inheritDoc} */
        @Override
        protected Rotation rotationFromInertial(final PVCoordinates pv) {
            return new Rotation(Vector3D.PLUS_I, Vector3D.PLUS_K, pv.getVelocity(),
                pv.getMomentum());
        }

        /** {@inheritDoc} */
        @Override
        protected Vector3D computeOmega(final PVCoordinates pv) {
            if (pv.getAcceleration() != null) {
                // Take acceleration into account
                final Vector3D omegaQSW = QSW.computeOmega(pv);

                final Vector3D omegaTNWoQSW = new Vector3D(1.0 / pv.getVelocity().getNormSq(), pv
                    .getVelocity().crossProduct(
                        pv.getAcceleration().subtract(omegaQSW.crossProduct(pv.getVelocity()))));

                return omegaQSW.add(omegaTNWoQSW);
            } else {
                // Use simplified formula
                // This formula consider a dynamic circular orbit but a kinetic elliptic orbit
                final Vector3D h = pv.getMomentum();
                final double p2 = pv.getPosition().getNormSq();
                final double v2 = pv.getVelocity().getNormSq();
                final double pDotv = pv.getPosition().dotProduct(pv.getVelocity());
                return new Vector3D((1. - pDotv * pDotv / (p2 * v2)) / p2, h);
            }
        }
    },

    /**
     * Constant for QSW frame
     * (X axis aligned with position, Z axis aligned with orbital momentum).
     * <p>
     * The axes of these frames are parallel to the axes of the {@link #LVLH} frame:
     * <ul>
     * <li>X<sub>QSW</sub> = -Z<sub>LVLH</sub></li>
     * <li>Y<sub>QSW</sub> = X<sub>LVLH</sub></li>
     * <li>Z<sub>QSW</sub> = -Y<sub>LVLH</sub></li>
     * </ul>
     * </p>
     *
     * @see #LVLH
     */
    QSW {
        /** {@inheritDoc} */
        @Override
        protected Rotation rotationFromInertial(final PVCoordinates pv) {
            return new Rotation(Vector3D.PLUS_I, Vector3D.PLUS_K, pv.getPosition(),
                pv.getMomentum());
        }

        /** {@inheritDoc} */
        @Override
        protected Vector3D computeOmega(final PVCoordinates pv) {
            // Without acceleration: p ^ v / |p|²
            // With acceleration: (without acceleration) + ((p ^ v).a / h²)p
            final Vector3D h = pv.getMomentum();
            Vector3D deltaAcc = Vector3D.ZERO;
            if (pv.getAcceleration() != null) {
                // Acceleration contribution
                deltaAcc = new Vector3D(Vector3D.dotProduct(h, pv.getAcceleration()) / h.getNormSq(), pv.getPosition());
            }
            return new Vector3D(1.0 / pv.getPosition().getNormSq(), h).add(deltaAcc);
        }
    },

    /**
     * Constant for -Q-SW frame
     * (X axis aligned with opposite of position, Z axis aligned with orbital momentum).
     * <p>
     * The axes of these frames are parallel to the axes of the {@link #QSW} frame:
     * <ul>
     * <li>X<sub>mQmSW</sub> = -X<sub>QSW</sub></li>
     * <li>Y<sub>mQmSW</sub> = -Y<sub>QSW</sub></li>
     * <li>Z<sub>mQmSW</sub> = Z<sub>QSW</sub></li>
     * </ul>
     * </p>
     *
     * @see #QSW
     */
    mQmSW {
        /** {@inheritDoc} */
        @Override
        protected Rotation rotationFromInertial(final PVCoordinates pv) {
            return new Rotation(Vector3D.PLUS_I, Vector3D.PLUS_K, pv.getPosition().negate(),
                pv.getMomentum());
        }

        /** {@inheritDoc} */
        @Override
        protected Vector3D computeOmega(final PVCoordinates pv) {
            return QSW.computeOmega(pv);
        }
    },

    /**
     * Constant for Local Vertical, Local Horizontal frame CCSDS definition (Z axis aligned with
     * opposite of position, Y axis aligned with opposite of orbital momentum)
     * <p>
     * The axes of this frame are parallel to the axes of the {@link #QSW} frame:
     * <ul>
     * <li>X<sub>LVLH</sub> = Y<sub>QSW</sub></li>
     * <li>Y<sub>LVLH</sub> = -Z<sub>QSW</sub></li>
     * <li>Z<sub>LVLH</sub> = -X<sub>QSW</sub></li>
     * </ul>
     * </p>
     *
     * WARNING : The current LVLH frame replaces the VVLH frame from old versions.
     *
     * @see #QSW
     */
    LVLH {
        /** {@inheritDoc} */
        @Override
        protected Rotation rotationFromInertial(final PVCoordinates pv) {
            return new Rotation(Vector3D.MINUS_K, Vector3D.MINUS_J, pv.getPosition(),
                pv.getMomentum());
        }

        /** {@inheritDoc} */
        @Override
        protected Vector3D computeOmega(final PVCoordinates pv) {
            return QSW.computeOmega(pv);
        }
    },

    /**
     * Constant for Velocity - Normal - Co-normal frame
     * (X axis aligned with velocity, Y axis aligned with orbital momentum).
     * <p>
     * The axes of this frame are parallel to the axes of the {@link #TNW} frame:
     * <ul>
     * <li>X<sub>VNC</sub> = X<sub>TNW</sub></li>
     * <li>Y<sub>VNC</sub> = Z<sub>TNW</sub></li>
     * <li>Z<sub>VNC</sub> = -Y<sub>TNW</sub></li>
     * </ul>
     * </p>
     *
     * @see #TNW
     */
    VNC {
        /** {@inheritDoc} */
        @Override
        protected Rotation rotationFromInertial(final PVCoordinates pv) {
            return new Rotation(Vector3D.PLUS_I, Vector3D.PLUS_J, pv.getVelocity(),
                pv.getMomentum());
        }

        /** {@inheritDoc} */
        @Override
        protected Vector3D computeOmega(final PVCoordinates pv) {
            return TNW.computeOmega(pv);
        }
    };

    /**
     * Get the transform from an inertial frame defining position-velocity and the local orbital
     * frame.
     *
     * @param date current date
     * @param pv position-velocity of the spacecraft in some inertial frame
     * @param computeSpinDerivatives true if spin derivatives has to be computed
     * @return transform from the frame where position-velocity are defined to local orbital frame
     */
    public Transform transformFromInertial(final AbsoluteDate date, final PVCoordinates pv,
                                           final boolean computeSpinDerivatives) {

        // compute the translation part of the transform
        final Transform translation = new Transform(date, pv);

        // compute the rotation part of the transform
        final Rotation r = this.rotationFromInertial(pv);
        final Vector3D omega = this.computeOmega(pv);
        final Transform rotation = new Transform(date, r, r.applyInverseTo(omega));

        return new Transform(date, translation, rotation, computeSpinDerivatives);
    }

    /**
     * Get the transform from an inertial frame defining position-velocity and the local orbital
     * frame.
     *
     * @param date current date
     * @param pv position-velocity of the spacecraft in some inertial frame
     * @return transform from the frame where position-velocity are defined to local orbital frame
     */
    public Transform transformFromInertial(final AbsoluteDate date, final PVCoordinates pv) {
        return this.transformFromInertial(date, pv, false);
    }

    /**
     * Get the rotation from inertial frame to local orbital frame.
     *
     * @param pv
     *        position-velocity of the spacecraft in some inertial frame
     * @return rotation from inertial frame to local orbital frame
     */
    protected abstract Rotation rotationFromInertial(final PVCoordinates pv);

    /**
     * Get the rotation rate vector expressed in inertial frame.
     *
     * @param pv position-velocity of the spacecraft in some inertial frame
     * @return rotation rate expressed in some inertial frame
     */
    protected abstract Vector3D computeOmega(final PVCoordinates pv);
}
