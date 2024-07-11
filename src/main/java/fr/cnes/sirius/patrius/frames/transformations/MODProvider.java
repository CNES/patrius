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
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.7:DM:DM-2590:18/05/2021:Configuration des TransformProvider 
* VERSION:4.3:DM:DM-2089:15/05/2019:[PATRIUS] passage a Java 8
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:317:06/03/2015: STELA integration in CIRF with referential choice (ICRF, CIRF or MOD)
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
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
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Mean Equator, Mean Equinox Frame.
 * <p>
 * This frame handles precession effects according to the IAU-76 model (Lieske).
 * </p>
 * <p>
 * Its parent frame is the GCRF frame.
 * </p>
 * <p>
 * It is sometimes called Mean of Date (MoD) frame.
 * </p>
 * 
 * <p>
 * Spin derivative is never computed and is either 0 or null. No analytical formula is currently available for spin
 * derivative although a formula could be derived. Spin is also 0.
 * </p>
 * <p>
 * Frames configuration is unused.
 * </p>
 * 
 * @serial serializable.
 * @author Pascal Parraud
 */
@SuppressWarnings("PMD.NullAssignment")
public final class MODProvider implements TransformProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 8795437689936129851L;

    /** 1st coefficient for ZETA precession angle. */
    private static final double ZETA_1 = 2306.2181 * Constants.ARC_SECONDS_TO_RADIANS;
    /** 2nd coefficient for ZETA precession angle. */
    private static final double ZETA_2 = 0.30188 * Constants.ARC_SECONDS_TO_RADIANS;
    /** 3rd coefficient for ZETA precession angle. */
    private static final double ZETA_3 = 0.017998 * Constants.ARC_SECONDS_TO_RADIANS;

    /** 1st coefficient for THETA precession angle. */
    private static final double THETA_1 = 2004.3109 * Constants.ARC_SECONDS_TO_RADIANS;
    /** 2nd coefficient for THETA precession angle. */
    private static final double THETA_2 = -0.42665 * Constants.ARC_SECONDS_TO_RADIANS;
    /** 3rd coefficient for THETA precession angle. */
    private static final double THETA_3 = -0.041833 * Constants.ARC_SECONDS_TO_RADIANS;

    /** 1st coefficient for Z precession angle. */
    private static final double Z_1 = 2306.2181 * Constants.ARC_SECONDS_TO_RADIANS;
    /** 2nd coefficient for Z precession angle. */
    private static final double Z_2 = 1.09468 * Constants.ARC_SECONDS_TO_RADIANS;
    /** 3rd coefficient for Z precession angle. */
    private static final double Z_3 = 0.018203 * Constants.ARC_SECONDS_TO_RADIANS;

    /**
     * Get the transfrom from parent frame.
     * <p>
     * The update considers the precession effects.
     * </p>
     * <p>
     * Frames configuration is unused.
     * </p>
     * 
     * @param date
     *        new value of the date
     * @param config
     *        frames configuration to use
     * @return transform at the specified date
     */
    @Override
    public Transform getTransform(final AbsoluteDate date, final FramesConfiguration config) {
        return this.getTransform(date, config, false);
    }

    /**
     * Get the transfrom from parent frame.
     * <p>
     * The update considers the precession effects.
     * </p>
     * 
     * @param date
     *        new value of the date
     * @return transform at the specified date
     * @throws PatriusException
     *         if the default configuration cannot be retrieved
     */
    @Override
    public Transform getTransform(final AbsoluteDate date) throws PatriusException {
        return this.getTransform(date, FramesFactory.getConfiguration());
    }

    /**
     * Get the transfrom from parent frame.
     * <p>
     * The update considers the precession effects.
     * </p>
     * <p>
     * Spin derivative is never computed and is either 0 or null. No analytical formula is currently available for spin
     * derivative although a formula could be derived. Spin is also 0.
     * </p>
     * 
     * @param date
     *        new value of the date
     * @param computeSpinDerivatives
     *        not used
     * @return transform at the specified date
     * @throws PatriusException
     *         if the default configuration cannot be retrieved
     */
    @Override
    public Transform getTransform(final AbsoluteDate date,
                                  final boolean computeSpinDerivatives) throws PatriusException {
        return this.getTransform(date, FramesFactory.getConfiguration(), computeSpinDerivatives);
    }

    /**
     * Get the transfrom from parent frame.
     * <p>
     * The update considers the precession effects.
     * </p>
     * <p>
     * Spin derivative is never computed and is either 0 or null. No analytical formula is currently available for spin
     * derivative although a formula could be derived. Spin is also 0.
     * </p>
     * <p>
     * Frames configuration is unused.
     * </p>
     * 
     * @param date
     *        new value of the date
     * @param config
     *        frames configuration to use
     * @param computeSpinDerivatives
     *        not used
     * @return transform at the specified date
     */
    @Override
    public Transform getTransform(final AbsoluteDate date, final FramesConfiguration config,
                                  final boolean computeSpinDerivatives) {

        // offset from J2000 epoch in julian centuries
        final double tts = date.durationFrom(AbsoluteDate.J2000_EPOCH);
        final double ttc = tts / Constants.JULIAN_CENTURY;

        // compute the zeta precession angle
        final double zeta = ((ZETA_3 * ttc + ZETA_2) * ttc + ZETA_1) * ttc;

        // compute the theta precession angle
        final double theta = ((THETA_3 * ttc + THETA_2) * ttc + THETA_1) * ttc;

        // compute the z precession angle
        final double z = ((Z_3 * ttc + Z_2) * ttc + Z_1) * ttc;

        // elementary rotations for precession
        final Rotation r1 = new Rotation(Vector3D.PLUS_K, z);
        final Rotation r2 = new Rotation(Vector3D.PLUS_J, -theta);
        final Rotation r3 = new Rotation(Vector3D.PLUS_K, zeta);

        // complete precession
        final Rotation precession = r1.applyTo(r2.applyTo(r3));

        final Vector3D acc = computeSpinDerivatives ? Vector3D.ZERO : null;

        // set up the transform from parent GCRF
        return new Transform(date, precession.revert(), Vector3D.ZERO, acc);
    }

    /**
     * Compute Euler angles (3, 2, 3) of MOD => EME2000 transformation and their derivatives with respect to time.
     * 
     * @param date
     *        a date
     * @return Euler angles (3, 2, 3) of MOD => EME2000 transformation and their derivatives with respect to time
     */
    public double[] getEulerAngles(final AbsoluteDate date) {

        // offset from J2000 epoch in julian centuries
        final double tts = date.durationFrom(AbsoluteDate.J2000_EPOCH);
        final double ttc = tts / Constants.JULIAN_CENTURY;

        // Euler angles
        final double zeta = ((ZETA_3 * ttc + ZETA_2) * ttc + ZETA_1) * ttc;
        final double theta = ((THETA_3 * ttc + THETA_2) * ttc + THETA_1) * ttc;
        final double z = ((Z_3 * ttc + Z_2) * ttc + Z_1) * ttc;

        // Euler angles derivatives
        final double dzeta = (3. * ZETA_3 * ttc + 2. * ZETA_2) * ttc + ZETA_1;
        final double dtheta = (3. * THETA_3 * ttc + 2. * THETA_2) * ttc + THETA_1;
        final double dz = (3. * Z_3 * ttc + 2. * Z_2) * ttc + Z_1;

        // Return result
        return new double[] { zeta, theta, z,
            dzeta / Constants.JULIAN_CENTURY, dtheta / Constants.JULIAN_CENTURY, dz / Constants.JULIAN_CENTURY };
    }

}
