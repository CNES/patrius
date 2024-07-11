/**
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
 *
 * @history 23/08/2012
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2590:18/05/2021:Configuration des TransformProvider 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:319:05/03/2015:Corrected Rotation class (Step1)
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:489:12/01/2016:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:524:25/05/2016:serialization java doc
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.transformations;

import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfiguration;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * {@link TransformProvider} for {@link fr.cnes.sirius.patrius.frames.FramesFactory#getEODFrame(boolean)}.
 * 
 * <p>
 * Spin derivative is either 0 or null since rotation is linear in time.
 * </p>
 * <p>Frames configuration is unused.</p>
 * 
 * @author Tournebize Johann
 * 
 * @version $Id: EODProvider.java 18074 2017-10-02 16:48:51Z bignon $
 * 
 * @since 1.3
 * @serial serializable.
 */
@SuppressWarnings("PMD.NullAssignment")
public class EODProvider implements TransformProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 5619295588275758784L;

    /**
     * Constant for the obliquity formula (rad).
     */
    private static final double OBLI_1 = 23.43928640175 * MathUtils.DEG_TO_RAD;

    /**
     * Constant for the obliquity formula (rad).
     */
    private static final double OBLI_2 = 0.01301239645 * MathUtils.DEG_TO_RAD;

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
    public Transform getTransform(final AbsoluteDate date, final FramesConfiguration config) throws PatriusException {
        return this.getTransform(date, config, false);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Spin derivative is either 0 or null since rotation is linear in time.
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
     * Spin derivative is either 0 or null since rotation is linear in time.
     * </p>
     * <p>
     * Frames configuration is unused.
     * </p>
     */
    @Override
    public Transform getTransform(final AbsoluteDate date, final FramesConfiguration config,
                                  final boolean computeSpinDerivatives) throws PatriusException {
        // time measured in Julian centuries of 36525 ephemeris days from the epoch J2000
        final double t = date.offsetFrom(AbsoluteDate.J2000_EPOCH, TimeScalesFactory.getTT()) /
            Constants.JULIAN_CENTURY;
        // obliquity (between 0 and 2PI
        double obliquity = OBLI_1 - OBLI_2 * t;
        obliquity = MathUtils.normalizeAngle(obliquity, FastMath.PI);

        // rotation between MOD and EOD
        final double[] sincos = MathLib.sinAndCos(obliquity / 2.);
        final double sin = sincos[0];
        final double cos = sincos[1];
        final Rotation rotation = new Rotation(false, cos, sin, 0., 0.);

        final Vector3D acc = computeSpinDerivatives ? Vector3D.ZERO : null;

        return new Transform(date, rotation, Vector3D.ZERO, acc);
    }
}
