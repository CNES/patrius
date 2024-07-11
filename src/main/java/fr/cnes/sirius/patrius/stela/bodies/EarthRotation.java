/**
 * 
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
 * @history created 04/03/2013
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:317:02/03/2015: STELA integration in CIRF with referential choice (ICRF, CIRF or MOD)
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.bodies;

import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.stela.forces.drag.StelaAeroModel;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;

/**
 * This class represents the Earth rotation. <br>
 * It is used to compute the Earth rotation rate when computing the atmospheric drag acceleration.
 * 
 * @concurrency immutable
 * 
 * @see StelaAeroModel
 * 
 * @author Vincent Ruch
 * @author Tiziana Sabatini
 * @author Emmanuel Bignon
 * 
 * @version $Id$
 * 
 * @since 1.3
 * 
 */
public final class EarthRotation {

    /** Coefficient used in the GMST computation. */
    private static final double GMST1 = 67310.54841;

    /** Coefficient used in the GMST computation. */
    private static final double GMST3 = 8640184.812866;

    /** Coefficient used in the GMST computation. */
    private static final double GMST4 = 0.093104;

    /** Coefficient used in the GMST computation. */
    private static final double GMST5 = 6.2E-06;

    /** Coefficient used in the Earth rotation rate computation. */
    private static final double EARTH_ROT_RATE1 = 1.002737909350795;

    /** Coefficient used in the Earth rotation rate computation. */
    private static final double EARTH_ROT_RATE2 = 5.9006E-11;

    /** Coefficient used in the Earth rotation rate computation. */
    private static final double EARTH_ROT_RATE3 = 5.9E-15;

    /** Coefficient used in the Earth Rotation Angle ERA computation. */
    private static final double ERA_TU_0 = 0.7790572732640;

    /** Coefficient used in the Earth Rotation Angle ERA computation. */
    private static final double ERA_TU_1 = 1.00273781191135448;

    /** Coefficient used in the Earth Rotation Angle ERA computation. */
    private static final double ERA_TU_2 = 0.00273781191135448;

    /** J2000 date. */
    private static final AbsoluteDate J2000 = new AbsoluteDate(new DateComponents(2000, 1, 1), new TimeComponents(12,
        0, 35.), TimeScalesFactory.getTAI());

    /**
     * Private constructor.
     */
    private EarthRotation() {

    }

    /**
     * Compute Greenwich Mean Sideral Time.
     * 
     * @param date
     *        a date
     * @return Greenwich Mean Sideral Time (rad)
     */
    public static double getGMST(final AbsoluteDate date) {
        final double secInDay = Constants.JULIAN_DAY;

        final double deltaT = (date.durationFrom(J2000)) / secInDay;
        final double t = deltaT / Constants.JULIAN_DAY_CENTURY;
        final double f = (deltaT - (int) deltaT) * secInDay;
        final double nsecs = GMST1 + GMST3 * t + GMST4 * t * t - GMST5 * t * t * t + f;
        final double nrad = nsecs / secInDay * (2 * FastMath.PI);
        return nrad % (2 * FastMath.PI);
    }

    /**
     * Compute Greenwich Mean Sideral Time derivative.
     * 
     * @param date
     *        a date
     * @return Greenwich Mean Sideral Time derivative (rad/s)
     */
    public static double getGMSTDerivative(final AbsoluteDate date) {
        final double secInDay = Constants.JULIAN_DAY;
        final double jdCNES = date.durationFrom(J2000) / secInDay;
        final double t = (jdCNES) / Constants.JULIAN_DAY_CENTURY;
        return 2.0 * FastMath.PI / secInDay * (EARTH_ROT_RATE1 + EARTH_ROT_RATE2 * t - EARTH_ROT_RATE3 * t * t);
    }

    /**
     * Compute the Earth Rotation Angle (ERA) using Capitaine model (2000).
     * 
     * @param date
     *        a date
     * @return Earth Rotation Angle (rad)
     */
    public static double getERA(final AbsoluteDate date) {
        final double tu = date.durationFrom(J2000) / Constants.JULIAN_DAY;
        final double f = tu - MathLib.floor(tu);
        final double era = 2 * FastMath.PI * (f + ERA_TU_0 + ERA_TU_2 * tu);
        return era % (2 * FastMath.PI);
    }

    /**
     * Compute the Earth Rotation Angle (ERA) derivative.
     * 
     * @param date
     *        a date
     * @return the Earth Rotation Angle derivative (rad/s)
     */
    public static double getERADerivative(final AbsoluteDate date) {
        return 2.0 * FastMath.PI * ERA_TU_1 / Constants.JULIAN_DAY;
    }
}
