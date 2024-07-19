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
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.7:DM:DM-2590:18/05/2021:Configuration des TransformProvider 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:489:12/01/2016:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:524:25/05/2016:serialization java doc
 * VERSION::FA:1301:05/09/2017:correct use of 1980/2000 EOP history
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
 * International Terrestrial Reference Frame, based on old equinox conventions.
 * <p>
 * Handles pole motion effects and depends on {@link GTODProvider}, its parent frame.
 * </p>
 * 
 * <p>
 * Spin derivative is never computed and is either 0 or null. No analytical formula is available for spin derivative
 * since data only provide pole correction without derivatives. Spin is also 0.
 * </p>
 * <p>
 * Frames configuration Pole correction is used.
 * </p>
 * 
 * @serial not serializable, used through FactoryManagedFrame. An instance of ITRFEquinoxProvider
 *         does not need to be serialized if a FactoryManagedFrame ITRFE is serialized.
 * @author Luc Maisonobe
 */
@SuppressWarnings("PMD.NullAssignment")
public final class ITRFEquinoxProvider implements TransformProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 6723470955343821599L;

    /**
     * Simple constructor.
     * 
     * @exception PatriusException
     *            if EOP parameters cannot be read
     */
    public ITRFEquinoxProvider() throws PatriusException {
        // Nothing to do
    }

    /**
     * Get the transform from GTOD at specified date.
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
     * Get the transform from GTOD at specified date.
     * <p>
     * The update considers the pole motion from IERS data.
     * </p>
     * <p>
     * Frames configuration Pole correction is used.
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
     * Get the transform from GTOD at specified date.
     * <p>
     * The update considers the pole motion from IERS data.
     * </p>
     * <p>
     * Spin derivative is never computed and is either 0 or null. No analytical formula is available for spin derivative
     * since data only provide pole correction without derivatives. Spin is also 0.
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
     * Get the transform from GTOD at specified date.
     * <p>
     * The update considers the pole motion from IERS data.
     * </p>
     * <p>
     * Spin derivative is never computed and is either 0 or null. No analytical formula is available for spin derivative
     * since data only provide pole correction without derivatives. Spin is also 0.
     * </p>
     * <p>
     * Frames configuration Pole correction is used.
     * </p>
     * 
     * @param date
     *        new value of the date
     * @param config
     *        frames configuration to use
     * @param computeSpinDerivatives
     *        not used
     * @return transform at the specified date
     * @exception PatriusException
     *            if the nutation model data embedded in the
     *            library cannot be read
     */
    @Override
    public Transform getTransform(final AbsoluteDate date, final FramesConfiguration config,
                                  final boolean computeSpinDerivatives) throws PatriusException {
        // pole correction parameters
        final PoleCorrection pCorr = config.getEOPHistory().getPoleCorrection(date);

        // elementary rotations due to pole motion in terrestrial frame
        final Rotation r1 = new Rotation(Vector3D.PLUS_I, -pCorr.getYp());
        final Rotation r2 = new Rotation(Vector3D.PLUS_J, -pCorr.getXp());

        // complete pole motion in terrestrial frame
        final Rotation wRot = r2.applyTo(r1);

        final Vector3D acc = computeSpinDerivatives ? Vector3D.ZERO : null;

        // set up the transform from parent GTOD
        return new Transform(date, wRot, Vector3D.ZERO, acc);
    }

}
