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
 * @history 24/04/2012
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.radiation;

import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;

/**
 * @description
 *              <p>
 *              Earth emissivity of the Knocke-Ries model (albedo and infrared)
 *              </p>
 * 
 * @concurrency immutable
 * 
 * @author ClaudeD
 * 
 * @version $Id: KnockeRiesModel.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.2
 * 
 */
public final class KnockeRiesModel implements IEmissivityModel {

    /** coefficient a0 of Knocke-Ries model. */
    public static final double A0 = 0.34;

    /** coefficient a2 of Knocke-Ries model. */
    public static final double A2 = 0.29;

    /** coefficient c1al of Knocke-Ries model. */
    public static final double C1AL = 0.10;

    /** coefficient a0 of Knocke-Ries model. */
    public static final double D0 = 0.68;

    /** coefficient a2 of Knocke-Ries model. */
    public static final double E2 = -0.18;

    /** coefficient c1ir of Knocke-Ries model. */
    public static final double C1IR = -0.07;

    /** reference day of Knocke-Ries model. */
    public static final AbsoluteDate REFDAY =
        new AbsoluteDate(AbsoluteDate.FIFTIES_EPOCH_TT, 11678. * Constants.JULIAN_DAY);

    /** duration of a year (in days). */
    public static final double DAYSYEAR = 365.25;

     /** Serializable UID. */
    private static final long serialVersionUID = -2245679638662817535L;

    /**
     * <p>
     * Computing of the emissivities of earth (albedo and infrared) based of the Knocke-Reis model (the longitude is not
     * used in this model)
     * </p>
     * See Obelix Reference manuel (NT-07-1)
     * 
     * @param cdate
     *        current date
     * @param latitude
     *        (rad)
     *        geocentric latitude.
     *        The angle between the radius (from centre to the point on the surface) and the equatorial plane
     * @param longitude
     *        (rad) geocentric longitude
     * @return albedo emissivity (emissivity[0]) and infrared emissivity (emissivity[1])
     * 
     * @since 1.2
     */
    @Override
    public double[] getEmissivity(final AbsoluteDate cdate, final double latitude, final double longitude) {

        final double[] emissivity = new double[2];

        // intermediate computations
        final double pSinLat = MathLib.sin(latitude);
        final int dd = (int) (cdate.durationFrom(REFDAY) / Constants.JULIAN_DAY);
        final double pCosSin = MathLib.cos(2. * FastMath.PI * dd / DAYSYEAR) * pSinLat;
        final double pSin2 = ((3. * pSinLat * pSinLat) - 1.) / 2.;

        // albedo emissivity
        emissivity[0] = A0 + C1AL * pCosSin + A2 * pSin2;

        // infrared emissivity
        emissivity[1] = D0 + C1IR * pCosSin + E2 * pSin2;

        return emissivity;
    }
}
