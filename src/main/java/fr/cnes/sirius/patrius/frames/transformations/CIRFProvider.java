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
 * VERSION:4.5:FA:FA-2370:27/05/2020:Optimisation CIRFProvider si CIP motion nul 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:319:05/03/2015:Corrected Rotation class (Step1)
 * VERSION::FA:380:23/04/2015:correction and optimization for no precession/nutation case
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:489:12/01/2016:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:524:25/05/2016:serialization java doc
 * VERSION::FA:829:25/01/2017:Protection of trigonometric methods
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.transformations;

import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfiguration;
import fr.cnes.sirius.patrius.frames.configuration.precessionnutation.PrecessionNutationModelFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Celestial Intermediate Reference Frame 2000.
 * <p>
 * This frame includes both precession and nutation effects according to the new IAU-2000 model. The single model
 * replaces the two separate models used before: IAU-76 precession (Lieske) and IAU-80 theory of nutation (Wahr). It
 * <strong>must</strong> be used with the Earth Rotation Angle (REA) defined by Capitaine's model and
 * <strong>not</strong> IAU-82 sidereal time which is consistent with the previous models only.
 * </p>
 * <p>
 * Its parent frame is the GCRF frame.
 * <p>
 * 
 * <p>
 * Spin derivative is never computed and is either 0 or null. No analytical formula is available for spin derivative
 * since data only provide CIP motion and its first derivative.
 * </p>
 * <p>Frames configuration precession-nutation model is used for computation.</p>
 * 
 * @serial serializable.
 */
@SuppressWarnings("PMD.NullAssignment")
public final class CIRFProvider implements TransformProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = -8378289692425977657L;

    /**
     * Get the transform from GCRF to CIRF2000 at the specified date.
     * <p>
     * The transform considers the nutation and precession effects from IERS data.
     * </p>
     * <p>
     * Frames configuration precession-nutation model is used for computation.
     * </p>
     * 
     * @param date
     *        new value of the date
     * @param config
     *        frames configuration to use
     * @return transform at the specified date
     *         library cannot be read
     * @throws PatriusException
     *         if the nutation model data embedded in the
     *         library cannot be read
     */
    @Override
    public Transform getTransform(final AbsoluteDate date, final FramesConfiguration config) throws PatriusException {
        return this.getTransform(date, config, false);
    }

    /**
     * Get the transform from GCRF to CIRF2000 at the specified date.
     * <p>
     * The transform considers the nutation and precession effects from IERS data.
     * </p>
     * 
     * @param date
     *        new value of the date
     * @return transform at the specified date
     * @exception PatriusException
     *            if the nutation model data embedded in the
     *            library cannot be read
     */
    @Override
    public Transform getTransform(final AbsoluteDate date) throws PatriusException {
        return this.getTransform(date, FramesFactory.getConfiguration(), false);
    }

    /**
     * Get the transform from GCRF to CIRF2000 at the specified date.
     * <p>
     * The transform considers the nutation and precession effects from IERS data.
     * </p>
     * <p>
     * Spin derivative is never computed and is either 0 or null. No analytical formula is available for spin derivative
     * since data only provide CIP motion and its first derivative.
     * </p>
     * 
     * @param date
     *        new value of the date
     * @param computeSpinDerivatives
     *        not used
     * @return transform at the specified date
     * @exception PatriusException
     *            if the nutation model data embedded in the
     *            library cannot be read
     */
    @Override
    public Transform getTransform(final AbsoluteDate date,
                                  final boolean computeSpinDerivatives) throws PatriusException {
        return this.getTransform(date, FramesFactory.getConfiguration(), computeSpinDerivatives);
    }

    /**
     * Get the transform from GCRF to CIRF2000 at the specified date.
     * <p>
     * The transform considers the nutation and precession effects from IERS data.
     * </p>
     * <p>
     * Spin derivative is never computed and is either 0 or null. No analytical formula is available for spin derivative
     * since data only provide CIP motion and its first derivative.
     * </p>
     * <p>
     * Frames configuration precession-nutation model is used for computation.
     * </p>
     * 
     * @param date
     *        new value of the date
     * @param config
     *        frames configuration to use
     * @param computeSpinDerivatives
     *        not used
     * @return transform at the specified date
     *         library cannot be read
     * @throws PatriusException
     *         if the nutation model data embedded in the
     *         library cannot be read
     */
    @Override
    public Transform getTransform(final AbsoluteDate date, final FramesConfiguration config,
                                  final boolean computeSpinDerivatives) throws PatriusException {

        Transform result = Transform.IDENTITY;
        if (!config.getPrecessionNutationModel().getPrecessionNutationModel()
                .equals(PrecessionNutationModelFactory.NO_PN)) {
            // Computation only if configuration requires CIP motion computation

            // evaluate pole motion in celestial frame
            final double[] cip = config.getCIPMotion(date);

            // No precession/nutation case
            if (cip[0] == 0 && cip[1] == 0 && cip[2] == 0) {
                return Transform.IDENTITY;
            }

            final double xCurrent = cip[0];
            final double yCurrent = cip[1];
            final double sCurrent = cip[2];

            // Get CIP derivative
            final double[] cipP = config.getCIPMotionTimeDerivative(date);
            final double xPCurrent = cipP[0];
            final double yPCurrent = cipP[1];
            final double sPCurrent = cipP[2];

            // set up the bias, precession and nutation rotation
            final double x2Py2 = xCurrent * xCurrent + yCurrent * yCurrent;
            final double zP1 = 1 + MathLib.sqrt(MathLib.max(0.0, 1 - x2Py2));
            final double r = MathLib.sqrt(x2Py2);
            final double sPe2 = 0.5 * (sCurrent + MathLib.atan2(yCurrent, xCurrent));
            final double[] sincos = MathLib.sinAndCos(sPe2);
            final double sin = sincos[0];
            final double cos = sincos[1];
            final double xPr = xCurrent + r;
            final double xPrCos = xPr * cos;
            final double xPrSin = xPr * sin;
            final double yCos = yCurrent * cos;
            final double ySin = yCurrent * sin;
            final Rotation bpn = new Rotation(true, zP1 * (xPrCos + ySin),
                    -r * (yCos + xPrSin), r * (xPrCos - ySin),
                    zP1 * (yCos - xPrSin));

            // rotation rate of the transformation from CIRF to GCRF
            final Vector3D rotationRate = new Vector3D(yPCurrent + sPCurrent * xCurrent,
                    -xPCurrent + sPCurrent * yCurrent, 0);

            // Acceleration (zero)
            final Vector3D acc = computeSpinDerivatives ? Vector3D.ZERO : null;

            // Result
            result = new Transform(date, bpn, bpn.applyInverseTo(rotationRate.negate()), acc);
        }

        // Return result
        return result;
    }

}
