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
* VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
* VERSION:4.7:DM:DM-2590:18/05/2021:Configuration des TransformProvider 
* VERSION:4.3:DM:DM-2089:15/05/2019:[PATRIUS] passage a Java 8
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:489:12/01/2016:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:524:25/05/2016:serialization java doc
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.transformations;

import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfiguration;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Transformation class for geodetic systems.
 * 
 * <p>
 * The Helmert transformation is mainly used to convert between various realizations of geodetic frames, for example in
 * the ITRF family.
 * </p>
 * 
 * <p>
 * The original Helmert transformation is a 14 parameters transform that includes translation, velocity, rotation,
 * rotation rate and scale factor. The scale factor is useful for coordinates near Earth surface, but it cannot be
 * extended to outer space as it would correspond to a non-unitary transform. Therefore, the scale factor is
 * <em>not</em> used here.
 * </p>
 * 
 * <p>
 * Instances of this class are guaranteed to be immutable.
 * </p>
 * 
 * <p>
 * Spin derivative is never computed and is either 0 or null. No analytical formula is currently available for spin
 * derivative although it could be derived.
 * </p>
 * <p>
 * Frames configuration is unused.
 * </p>
 * 
 * @serial serializable.
 * 
 * @author Luc Maisonobe
 * @since 5.1
 */
@SuppressWarnings("PMD.NullAssignment")
public class HelmertTransformation implements TransformProvider {

    /** serializable UID. */
    private static final long serialVersionUID = -1900615992141291146L;

    /** Cartesian part of the transform. */
    private final PVCoordinates cartesian;

    /** Global rotation vector (applying rotation is done by computing cross product). */
    private final Vector3D rotationVector;

    /** First time derivative of the rotation (norm representing angular rate). */
    private final Vector3D rotationRate;

    /** Reference epoch of the transform. */
    private final AbsoluteDate epoch;

    /**
     * Build a transform from its primitive operations.
     * 
     * @param epochIn
     *        reference epoch of the transform
     * @param t1
     *        translation parameter along X axis (BEWARE, this is in mm)
     * @param t2
     *        translation parameter along Y axis (BEWARE, this is in mm)
     * @param t3
     *        translation parameter along Z axis (BEWARE, this is in mm)
     * @param r1
     *        rotation parameter around X axis (BEWARE, this is in mas)
     * @param r2
     *        rotation parameter around Y axis (BEWARE, this is in mas)
     * @param r3
     *        rotation parameter around Z axis (BEWARE, this is in mas)
     * @param t1Dot
     *        rate of translation parameter along X axis (BEWARE, this is in mm/y)
     * @param t2Dot
     *        rate of translation parameter along Y axis (BEWARE, this is in mm/y)
     * @param t3Dot
     *        rate of translation parameter along Z axis (BEWARE, this is in mm/y)
     * @param r1Dot
     *        rate of rotation parameter around X axis (BEWARE, this is in mas/y)
     * @param r2Dot
     *        rate of rotation parameter around Y axis (BEWARE, this is in mas/y)
     * @param r3Dot
     *        rate of rotation parameter around Z axis (BEWARE, this is in mas/y)
     */
    public HelmertTransformation(final AbsoluteDate epochIn, final double t1, final double t2, final double t3,
        final double r1, final double r2, final double r3, final double t1Dot, final double t2Dot,
        final double t3Dot, final double r1Dot, final double r2Dot, final double r3Dot) {

        // conversion parameters to SI units
        final double mmToM = 1.0e-3;
        final double masToRad = 1.0e-3 * Constants.ARC_SECONDS_TO_RADIANS;

        this.epoch = epochIn;
        this.cartesian = new PVCoordinates(new Vector3D(t1 * mmToM, t2 * mmToM, t3 * mmToM), new Vector3D(t1Dot * mmToM
            /
            Constants.JULIAN_YEAR, t2Dot * mmToM / Constants.JULIAN_YEAR, t3Dot * mmToM / Constants.JULIAN_YEAR));
        this.rotationVector = new Vector3D(r1 * masToRad, r2 * masToRad, r3 * masToRad);
        this.rotationRate = new Vector3D(r1Dot * masToRad / Constants.JULIAN_YEAR, r2Dot * masToRad /
            Constants.JULIAN_YEAR, r3Dot * masToRad / Constants.JULIAN_YEAR);

    }

    /**
     * Get the reference epoch of the transform.
     * 
     * @return reference epoch of the transform
     */
    public AbsoluteDate getEpoch() {
        return this.epoch;
    }

    /**
     * Returns the Cartesian part of the transform.
     * @return the Cartesian part of the transform
     */
    public PVCoordinates getCartesian() {
        return cartesian;
    }

    /**
     * Returns the global rotation vector (applying rotation is done by computing cross product).
     * @return the global rotation vector (applying rotation is done by computing cross product)
     */
    public Vector3D getRotationVector() {
        return rotationVector;
    }

    /**
     * Returns the first time derivative of the rotation (norm representing angular rate).
     * @return the first time derivative of the rotation (norm representing angular rate)
     */
    public Vector3D getRotationRate() {
        return rotationRate;
    }

    /**
     * Compute the transform at some date.
     * 
     * @param date
     *        date at which the transform is desired
     * @return computed transform at specified date
     * @throws PatriusException
     *         if the default config cannot be retrieved
     */
    @Override
    public Transform getTransform(final AbsoluteDate date) throws PatriusException {
        return this.getTransform(date, FramesFactory.getConfiguration(), false);
    }

    /**
     * Compute the transform at some date.
     * <p>
     * Frames configuration is unused.
     * </p>
     * 
     * @param date
     *        date at which the transform is desired
     * @param config
     *        frames configuration to use
     * @return computed transform at specified date
     * @throws PatriusException
     *         if problem in the frame configuration
     */
    @Override
    public Transform getTransform(final AbsoluteDate date, final FramesConfiguration config) throws PatriusException {
        return this.getTransform(date, config, false);
    }

    /**
     * Compute the transform at some date.
     * <p>
     * Spin derivative is never computed and is either 0 or null. No analytical formula is currently available for spin
     * derivative although it could be derived.
     * </p>
     * 
     * @param date
     *        date at which the transform is desired
     * @param computeSpinDerivatives
     *        Spin derivatives are computed : true or not : false
     * @return computed transform at specified date
     * @throws PatriusException
     *         if default frame configuration cannot be retrieved
     */
    @Override
    public Transform getTransform(final AbsoluteDate date,
                                  final boolean computeSpinDerivatives) throws PatriusException {
        return this.getTransform(date, FramesFactory.getConfiguration(), computeSpinDerivatives);
    }

    /**
     * Compute the transform at some date.
     * <p>
     * Spin derivative is never computed and is either 0 or null. No analytical formula is currently available for spin
     * derivative although it could be derived.
     * </p>
     * <p>
     * Frames configuration is unused.
     * </p>
     * 
     * @param date
     *        date at which the transform is desired
     * @param config
     *        frames configuration to use
     * @param computeSpinDerivatives
     *        Spin derivatives are computed : true or not : false
     * @return computed transform at specified date
     * @throws PatriusException
     *         if problem in the frame configuration
     */
    @Override
    public Transform getTransform(final AbsoluteDate date, final FramesConfiguration config,
                                  final boolean computeSpinDerivatives) throws PatriusException {
        // compute parameters evolution since reference epoch
        final double dt = date.durationFrom(this.epoch);
        final Vector3D dR = new Vector3D(1, this.rotationVector, dt, this.rotationRate);

        // build translation part
        final Transform translationTransform = new Transform(date, this.cartesian.shiftedBy(dt));

        // build rotation part
        final Vector3D acc = computeSpinDerivatives ? Vector3D.ZERO : null;
        final double angle = dR.getNorm();
        final Transform rotationTransform = new Transform(date, (angle < Precision.SAFE_MIN) ? Rotation.IDENTITY :
            new Rotation(dR, angle), this.rotationRate, acc);

        // combine both parts
        return new Transform(date, translationTransform, rotationTransform, computeSpinDerivatives);
    }

}
