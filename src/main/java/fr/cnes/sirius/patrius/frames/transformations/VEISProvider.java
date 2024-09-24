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
* VERSION:4.3:DM:DM-2089:15/05/2019:[PATRIUS] passage a Java 8
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:144:31/10/2013:Added possibility of storing UT1-TAI instead of UT1-UTC
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:489:12/01/2016:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::FA:1301:05/09/2017:correct use of 1980/2000 EOP history
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.transformations;

import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfiguration;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Veis 1950 Frame.
 * <p>
 * Its parent frame is the {@link GTODProvider} without EOP correction application.
 * </p>
 * <p>
 * This frame is mainly provided for consistency with legacy softwares.
 * </p>
 * 
 * <p>Spin derivative, when computed, is always 0.</p>
 * <p>Frames configuration UT1 - TAI is used.</p>
 * 
 * @author Pascal Parraud
 */
@SuppressWarnings("PMD.NullAssignment")
public final class VEISProvider implements TransformProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 6918291423091809232L;

    /** Reference date. */
    private static final AbsoluteDate VST_REFERENCE =
        new AbsoluteDate(DateComponents.FIFTIES_EPOCH, TimeScalesFactory.getTAI());

    /** 1st coef for Veis sidereal time computation in radians (100.075542 deg). */
    private static final double VST0 = 1.746647708617871;

    /** 2nd coef for Veis sidereal time computation in rad/s (0.985612288 deg/s). */
    private static final double VST1 = 0.17202179573714597e-1;

    /** Veis sidereal time derivative in rad/s. */
    private static final double VSTD = 7.292115146705209e-5;

    /**
     * Constructor for the singleton.
     * 
     * @exception PatriusException
     *            if EOP data cannot be read
     */
    public VEISProvider() throws PatriusException {
        // Nothing to do
    }

    /**
     * Get the transform from GTOD at specified date.
     * 
     * @param date
     *        new value of the date
     * @return transform at the specified date
     * @exception PatriusException
     *            if data embedded in the library cannot be read
     */
    @Override
    public Transform getTransform(final AbsoluteDate date) throws PatriusException {
        return this.getTransform(date, FramesFactory.getConfiguration(), false);
    }

    /**
     * Get the transform from GTOD at specified date.
     * <p>
     * Spin derivative, when computed, is always 0.
     * </p>
     * 
     * @param date
     *        new value of the date
     * @param computeSpinDerivatives
     *        unused param
     * @return transform at the specified date
     * @exception PatriusException
     *            if data embedded in the library cannot be read
     */
    @Override
    public Transform getTransform(final AbsoluteDate date,
                                  final boolean computeSpinDerivatives) throws PatriusException {
        return this.getTransform(date, FramesFactory.getConfiguration(), computeSpinDerivatives);
    }

    /** {@inheritDoc}
     * <p>
     * Frames configuration UT1 - TAI is used.
     * </p>
     */
    @Override
    public Transform getTransform(final AbsoluteDate date, final FramesConfiguration config) throws PatriusException {
        return this.getTransform(date, config, false);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Spin derivative, when computed, is always 0.
     * </p>
     * <p>
     * Frames configuration UT1 - TAI is used.
     * </p>
     */
    @Override
    public Transform getTransform(final AbsoluteDate date, final FramesConfiguration config,
                                  final boolean computeSpinDerivatives) throws PatriusException {
        // offset from FIFTIES epoch (UT1 scale)
        final double dtai = date.durationFrom(VST_REFERENCE);
        final double dut1 = config.getEOPHistory().getUT1MinusTAI(date);

        final double tut1 = dtai + dut1;
        final double ttd = tut1 / Constants.JULIAN_DAY;
        final double rdtt = ttd - (int) ttd;

        // compute Veis sideral time, in radians
        final double vst = (VST0 + VST1 * ttd + MathUtils.TWO_PI * rdtt) % MathUtils.TWO_PI;

        // compute angular rotation of Earth, in rad/s
        final Vector3D rotationRate = new Vector3D(-VSTD, Vector3D.PLUS_K);

        // set up the transform from parent GTOD
        final Rotation rot = new Rotation(Vector3D.PLUS_K, -vst);
        final Vector3D acc = computeSpinDerivatives ? Vector3D.ZERO : null;
        return new Transform(date, rot, rotationRate, acc);
    }

}
