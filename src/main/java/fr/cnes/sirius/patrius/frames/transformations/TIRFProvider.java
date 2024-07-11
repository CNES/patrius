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
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.time.UT1Scale;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Terrestrial Intermediate Reference Frame 2000.
 * <p>
 * The pole motion is not considered : Pseudo Earth Fixed Frame. It handles the earth rotation angle, its parent frame
 * is the {@link CIRFProvider}
 * </p>
 *
 * <p>Spin derivative, when computed, is always 0.</p>
 * <p><i>Default</i> frames configuration UT1 - TAI is used.</p>
 * 
 * @serial serializable.
 */
@SuppressWarnings("PMD.NullAssignment")
public final class TIRFProvider implements TransformProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 7243684504752696164L;

    /** Reference date of Capitaine's Earth Rotation Angle model. */
    private static final AbsoluteDate ERA_REFERENCE = new AbsoluteDate(DateComponents.J2000_EPOCH, TimeComponents.H12,
        TimeScalesFactory.getTAI());

    /** Constant term of Capitaine's Earth Rotation Angle model. */
    private static final double ERA_0 = MathUtils.TWO_PI * 0.7790572732640;

    /**
     * Rate term of Capitaine's Earth Rotation Angle model. (radians per day, main part)
     */
    private static final double ERA_1A = MathUtils.TWO_PI;

    /**
     * Rate term of Capitaine's Earth Rotation Angle model. (radians per day, fractional part)
     */
    private static final double ERA_1B = ERA_1A * 0.00273781191135448;

    /**
     * Rotation rate.
     */
    private static final double ROT_RATE = (ERA_1A + ERA_1B) / Constants.JULIAN_DAY;

    /**
     * Get the transform from CIRF 2000 at specified date.
     * <p>
     * The update considers the earth rotation from IERS data.
     * </p>
     * <p>
     * <i>Default</i> frames configuration UT1 - TAI is used.
     * </p>
     * 
     * @param date
     *        new value of the date
     * @param config
     *        frames configuration to use
     * @return transform at the specified date
     * @exception PatriusException
     *            if the nutation model data embedded in the library cannot be read
     */
    @Override
    public Transform getTransform(final AbsoluteDate date, final FramesConfiguration config) throws PatriusException {
        return this.getTransform(date, config, false);
    }

    /**
     * Get the transform from CIRF 2000 at specified date.
     * <p>
     * The update considers the earth rotation from IERS data.
     * </p>
     * <p>
     * Spin derivative, when computed, is always 0.
     * </p>
     * <p>
     * <i>Default</i> frames configuration UT1 - TAI is used.
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
     *            if the nutation model data embedded in the library cannot be read
     */

    @Override
    public Transform getTransform(final AbsoluteDate date, final FramesConfiguration config,
                                  final boolean computeSpinDerivatives) throws PatriusException {

        // set up the transform from parent CIRF2000
        final Rotation rotation = new Rotation(Vector3D.PLUS_K, getERA(date, config));
        final Vector3D rotationRate = new Vector3D(ROT_RATE, Vector3D.PLUS_K);
        final Vector3D acc = computeSpinDerivatives ? Vector3D.ZERO : null;
        return new Transform(date, rotation, rotationRate, acc);
    }

    /**
     * Get the transform from CIRF 2000 at specified date.
     * <p>
     * The update considers the earth rotation from IERS data.
     * </p>
     * 
     * @param date
     *        new value of the date
     * @return transform at the specified date
     * @exception PatriusException
     *            if the nutation model data embedded in the library cannot be read
     */
    @Override
    public Transform getTransform(final AbsoluteDate date) throws PatriusException {
        return this.getTransform(date, FramesFactory.getConfiguration());
    }

    /**
     * Get the transform from CIRF 2000 at specified date.
     * <p>
     * The update considers the earth rotation from IERS data.
     * </p>
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
     *            if the nutation model data embedded in the library cannot be read
     */
    @Override
    public Transform getTransform(final AbsoluteDate date,
                                  final boolean computeSpinDerivatives) throws PatriusException {
        return this.getTransform(date, FramesFactory.getConfiguration(), computeSpinDerivatives);
    }

    /**
     * Get the Earth Rotation Angle at the current date.
     * 
     * @param date
     *        the date
     * @return Earth Rotation Angle at the current date in radians
     * @exception PatriusException
     *            if nutation model cannot be computed
     */
    public static double getEarthRotationAngle(final AbsoluteDate date) throws PatriusException {
        return getERA(date, FramesFactory.getConfiguration());
    }

    /**
     * Get the Earth Rotation rate.
     * 
     * @return Earth Rotation rate
     */
    public static double getEarthRotationRate() {
        return ROT_RATE;
    }

    /**
     * Get the Earth Rotation Angle at the current date.
     * 
     * @param date
     *        the date
     * @param config
     *        frames configuration to use
     * @return Earth Rotation Angle at the current date in radians
     * @exception PatriusException
     *            if nutation model cannot be computed
     */
    private static double getERA(final AbsoluteDate date, final FramesConfiguration config) throws PatriusException {

        final UT1Scale ut1 = TimeScalesFactory.getUT1();

        final double tu = (date.durationFrom(ERA_REFERENCE) + ut1.offsetFromTAI(date)) / Constants.JULIAN_DAY;
        double era = ERA_0 + ERA_1A * tu + ERA_1B * tu;
        era -= MathUtils.TWO_PI * MathLib.floor((era + FastMath.PI) / MathUtils.TWO_PI);

        return era;
    }

}
