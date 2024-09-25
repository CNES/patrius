/**
 * Copyright 2023-2023 CNES
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
 * VERSION:4.13:DM:DM-68:08/12/2023:[PATRIUS] Ajout du repere G50 CNES
 * VERSION:4.13:DM:DM-120:08/12/2023:[PATRIUS] Merge de la branche patrius-for-lotus dans Patrius
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
 * Abstract class for {@link VEISProvider} and {@link G50Provider} which only differ in UT1/UTC handling.
 * The transformation remains the same, their parent frame is the {@link GTODProvider} without EOP correction
 * application.
 * 
 * @author Emmanuel Bignon
 * 
 * @since 4.13
 */
public abstract class AbstractVeisProvider implements TransformProvider {

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
    @SuppressWarnings("PMD.NullAssignment")
    public Transform getTransform(final AbsoluteDate date, final FramesConfiguration config,
                                  final boolean computeSpinDerivatives) throws PatriusException {
        // offset from FIFTIES epoch (UT1 scale)
        final double dtai = date.durationFrom(VST_REFERENCE);
        final double dut1 = getUT1MinusTAI(date, config);
        
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

    /**
     * Returns UT1 - TAI value.
     * 
     * @param date a date
     * @param config frames configuration
     * @return UT1 - TAI value
     * @throws PatriusException thrown if failed to load data
     */
    protected abstract double getUT1MinusTAI(final AbsoluteDate date,
                                             final FramesConfiguration config) throws PatriusException;
}
