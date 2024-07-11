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
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:144:31/10/2013:Added possibility of storing UT1-TAI instead of UT1-UTC
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:489:12/01/2016:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:524:25/05/2016:serialization java doc
 * VERSION::FA:1301:06/09/2017:Generalized EOP history
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
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Greenwich True Of Date Frame, also known as True of Date Rotating frame (TDR)
 * or Greenwich Rotating Coordinate frame (GCR).
 * <p>
 * This frame handles the sidereal time according to IAU-82 model.
 * </p>
 * <p>
 * Its parent frame is the {@link TODProvider}.
 * </p>
 * <p>
 * The pole motion is not applied here.
 * </p>
 * 
 * <p>
 * Spin derivative is never computed and is either 0 or null. No analytical formula is currently available for spin
 * derivative although a formula could be derived.
 * </p>
 * <p>
 * Frames configuration LOD and UT1 - TAI is used.
 * </p>
 * 
 * @serial serializable.
 * @author Pascal Parraud
 * @author Thierry Ceolin
 */
@SuppressWarnings("PMD.NullAssignment")
public final class GTODProvider implements TransformProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = -1727797229994466102L;

    /** Radians per second of time. */
    private static final double RADIANS_PER_SECOND = MathUtils.TWO_PI / Constants.JULIAN_DAY;

    /** Angular velocity of the Earth, in rad/s. */
    private static final double AVE = 7.292115146706979e-5;

    /** Reference date for IAU 1982 GMST-UT1 model. */
    private static final AbsoluteDate GMST_REFERENCE =
        new AbsoluteDate(DateComponents.J2000_EPOCH, TimeComponents.H12, TimeScalesFactory.getTAI());

    /** First coefficient of IAU 1982 GMST-UT1 model. */
    private static final double GMST_0 = 24110.54841;

    /** Second coefficient of IAU 1982 GMST-UT1 model. */
    private static final double GMST_1 = 8640184.812866;

    /** Third coefficient of IAU 1982 GMST-UT1 model. */
    private static final double GMST_2 = 0.093104;

    /** Fourth coefficient of IAU 1982 GMST-UT1 model. */
    private static final double GMST_3 = -6.2e-6;

    /**
     * Simple constructor.
     * 
     * @exception PatriusException
     *            if EOP parameters are desired but cannot be read
     */
    public GTODProvider() throws PatriusException {
        // Nothing to do
    }

    /**
     * Get the transform from TOD at specified date.
     * <p>
     * The update considers the Earth rotation from IERS data.
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
     * Get the transform from TOD at specified date.
     * <p>
     * The update considers the Earth rotation from IERS data.
     * </p>
     * <p>
     * Frames configuration LOD and UT1 - TAI is used.
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
     * Get the transform from TOD at specified date.
     * <p>
     * The update considers the Earth rotation from IERS data.
     * </p>
     * <p>
     * Spin derivative is never computed and is either 0 or null. No analytical formula is currently available for spin
     * derivative although a formula could be derived.
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
     * Get the transform from TOD at specified date.
     * <p>
     * The update considers the Earth rotation from IERS data.
     * </p>
     * <p>
     * Spin derivative is never computed and is either 0 or null. No analytical formula is currently available for spin
     * derivative although a formula could be derived.
     * </p>
     * <p>
     * Frames configuration LOD and UT1 - TAI is used.
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
        // compute Greenwich apparent sidereal time, in radians
        final double gast = this.getGAST(date, config);

        final Vector3D rotationRate = new Vector3D(getRotationRate(date, config), Vector3D.PLUS_K);

        final Vector3D acc = computeSpinDerivatives ? Vector3D.ZERO : null;

        // set up the transform from parent TOD
        return new Transform(date, new Rotation(Vector3D.PLUS_K, gast), rotationRate, acc);
    }

    /**
     * Get the Greenwich mean sidereal time, in radians.
     * 
     * @param date
     *        current date
     * @return Greenwich mean sidereal time, in radians
     * @exception PatriusException
     *            if UTS taime scale cannot be retrieved
     * @see #getGAST(AbsoluteDate)
     */
    public static double getGMST(final AbsoluteDate date) throws PatriusException {
        return getGMST(date, FramesFactory.getConfiguration());
    }

    /**
     * Get the Greenwich mean sidereal time, in radians.
     * 
     * @param date
     *        current date
     * @param config
     *        frames configuration to use
     * @return Greenwich mean sidereal time, in radians
     * @exception PatriusException
     *            if UTS taime scale cannot be retrieved
     * @see #getGAST(AbsoluteDate)
     */
    public static double getGMST(final AbsoluteDate date, final FramesConfiguration config) throws PatriusException {

        // offset in julian centuries from J2000 epoch (UT1 scale)
        final double dtai = date.durationFrom(GMST_REFERENCE);
        final double dut1 = config.getUT1MinusTAI(date);
        final double tut1 = dtai + dut1;
        final double tt = tut1 / Constants.JULIAN_CENTURY;

        // Seconds in the day, adjusted by 12 hours because the
        // UT1 is supplied as a Julian date beginning at noon.
        final double sd = (tut1 + Constants.JULIAN_DAY / 2.) % Constants.JULIAN_DAY;

        // compute Greenwich mean sidereal time, in radians
        return (((GMST_3 * tt + GMST_2) * tt + GMST_1) * tt + GMST_0 + sd) * RADIANS_PER_SECOND;

    }

    /**
     * Get the Greenwich apparent sidereal time, in radians.
     * <p>
     * Greenwich apparent sidereal time is {@link #getGMST(AbsoluteDate) Greenwich mean sidereal time} plus
     * {@link TODProvider#getEquationOfEquinoxes(AbsoluteDate) equation of equinoxes}.
     * </p>
     * 
     * @param date
     *        current date
     * @return Greenwich apparent sidereal time, in radians
     * @exception PatriusException
     *            if UTS taime scale cannot be retrieved
     * @see #getGMST(AbsoluteDate)
     */
    public double getGAST(final AbsoluteDate date) throws PatriusException {
        return this.getGAST(date, FramesFactory.getConfiguration());
    }

    /**
     * Get the Greenwich apparent sidereal time, in radians.
     * <p>
     * Greenwich apparent sidereal time is {@link #getGMST(AbsoluteDate) Greenwich mean sidereal time} plus
     * {@link TODProvider#getEquationOfEquinoxes(AbsoluteDate) equation of equinoxes}.
     * </p>
     * 
     * @param date
     *        current date
     * @param config
     *        frames configuration to use
     * @return Greenwich apparent sidereal time, in radians
     * @exception PatriusException
     *            if UTS taime scale cannot be retrieved
     * @see #getGMST(AbsoluteDate)
     */
    public double getGAST(final AbsoluteDate date, final FramesConfiguration config) throws PatriusException {
        // offset from J2000.0 epoch
        final double eqe = TODProvider.getEquationOfEquinoxes(date);

        // compute Greenwich apparent sidereal time, in radians
        return getGMST(date, config) + eqe;

    }

    /**
     * Get the rotation rate of the Earth.
     * 
     * @param date
     *        given date
     * @return the rotation rate
     * @throws PatriusException
     *         if EOP data cannot be loaded
     */
    public static double getRotationRate(final AbsoluteDate date) throws PatriusException {
        return getRotationRate(date, FramesFactory.getConfiguration());
    }

    /**
     * Get the rotation rate of the Earth.
     * 
     * @param date
     *        given date
     * @param config
     *        frames configuration to use
     * @return the rotation rate
     * @throws PatriusException
     *         if EOP data cannot be loaded
     */
    public static double getRotationRate(final AbsoluteDate date,
                                         final FramesConfiguration config) throws PatriusException {
        // compute true angular rotation of Earth, in rad/s
        final double lod = config.getEOPHistory().getLOD(date);
        return AVE * (1 - lod / Constants.JULIAN_DAY);
    }

}
