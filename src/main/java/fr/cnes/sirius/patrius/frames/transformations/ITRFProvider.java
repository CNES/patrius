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
* VERSION:4.7:DM:DM-2590:18/05/2021:Configuration des TransformProvider 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:489:12/01/2016:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:524:25/05/2016:serialization java doc
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.transformations;

import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfiguration;
import fr.cnes.sirius.patrius.frames.configuration.eop.PoleCorrection;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * International Terrestrial Reference Frame.
 * <p>
 * Handles pole motion effects and depends on {@link TIRFProvider}, its parent frame.
 * </p>
 * 
 * <p>
 * Spin derivative is never computed and is either 0 or null. No analytical formula is available for spin derivative
 * since data only provide pole correction without derivatives. Spin is also 0. Spin is also 0.
 * </p>
 * <p>
 * Frames configuration polar motion and S' is used.
 * </p>
 * 
 * @serial serializable.
 * @author Luc Maisonobe
 */
@SuppressWarnings("PMD.NullAssignment")
public final class ITRFProvider implements TransformProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = -8320047148885526349L;

    /**
     * Get the transform from TIRF 2000 at specified date.
     * <p>
     * The update considers the pole motion from IERS data.
     * </p>
     * <p>
     * Frames configuration polar motion and S' is used.
     * </p>
     * 
     * @param date
     *        new value of the date
     * @param config
     *        frames configuration to use
     * @return transform at the specified date
     * @exception PatriusException
     *            if the nutation model data embedded in the
     *            library cannot be read
     */
    @Override
    public Transform getTransform(final AbsoluteDate date, final FramesConfiguration config) throws PatriusException {
        return this.getTransform(date, config, false);
    }

    /**
     * Get the transform from TIRF 2000 at specified date.
     * <p>
     * The update considers the pole motion from IERS data.
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
        return this.getTransform(date, FramesFactory.getConfiguration());
    }

    /**
     * Get the transform from TIRF 2000 at specified date.
     * <p>
     * The update considers the pole motion from IERS data.
     * </p>
     * <p>
     * Spin derivative is never computed and is either 0 or null. No analytical formula is available for spin derivative
     * since data only provide pole correction without derivatives. Spin is also 0. Spin is also 0.
     * </p>
     * 
     * @param date
     *        new value of the date
     * @param computeSpinDerivatives
     *        unused param
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
     * Get the transform from TIRF 2000 at specified date.
     * <p>
     * The update considers the pole motion from IERS data.
     * </p>
     * <p>
     * Spin derivative is never computed and is either 0 or null. No analytical formula is available for spin derivative
     * since data only provide pole correction without derivatives. Spin is also 0. Spin is also 0.
     * </p>
     * <p>
     * Frames configuration polar motion and S' is used.
     * </p>
     * 
     * @param date
     *        new value of the date
     * @param config
     *        frames configuration to use
     * @param computeSpinDerivatives
     *        unused param
     * @return transform at the specified date
     * @exception PatriusException
     *            if the nutation model data embedded in the
     *            library cannot be read
     */
    @Override
    public Transform getTransform(final AbsoluteDate date, final FramesConfiguration config,
                                  final boolean computeSpinDerivatives) throws PatriusException {
        // corrected pole parameters (u,v)
        final double[] polarCoordinates = config.getPolarMotion(date);
        final PoleCorrection nCorr = this.nutationCorrection(date);

        // s prime
        final double sPrime = config.getSprime(date);

        // elementary rotations due to pole motion in terrestrial frame
        final Rotation r1 = new Rotation(Vector3D.PLUS_I, -(polarCoordinates[1] + nCorr.getYp()));
        final Rotation r2 = new Rotation(Vector3D.PLUS_J, -(polarCoordinates[0] + nCorr.getXp()));
        final Rotation r3 = new Rotation(Vector3D.PLUS_K, sPrime);

        // complete pole motion in terrestrial frame
        final Rotation wRot = r3.applyTo(r2.applyTo(r1));

        // combined effects
        final Rotation combined = wRot;

        final Vector3D acc = computeSpinDerivatives ? Vector3D.ZERO : null;

        // set up the transform from parent TIRF
        return new Transform(date, combined, Vector3D.ZERO, acc);
    }

    /**
     * Compute nutation correction due to tidal gravity.
     * 
     * @param date
     *        current date
     * @return nutation correction
     */
    private PoleCorrection nutationCorrection(final AbsoluteDate date) {
        // this factor seems to be of order of magnitude a few tens of
        // micro arcseconds. It is computed from the classical approach
        // (not the new one used here) and hence requires computation
        // of GST, IAU2000A nutation, equations of equinox ...
        // For now, this term is ignored
        return PoleCorrection.NULL_CORRECTION;
    }

}
