/**
 * Copyright 2011-2022 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.analytical.tle;

// CHECKSTYLE: stop MagicNumber check
// CHECKSTYLE: stop HideUtilityClassConstructor check
// Reason: constants

/**
 * Constants necessary to TLE propagation.
 * 
 * This constants are used in the WGS-72 model, compliant with NORAD implementations.
 * 
 * @author Fabien Maussion
 */
public class TLEConstants {

    /** Constant 1.0 / 3.0. */
    public static final double ONE_THIRD = 1.0 / 3.0;

    /** Constant 2.0 / 3.0. */
    public static final double TWO_THIRD = 2.0 / 3.0;

    /** Earth radius in km. */
    public static final double EARTH_RADIUS = 6378.135;

    /** Equatorial radius rescaled (1.0). */
    public static final double NORMALIZED_EQUATORIAL_RADIUS = 1.0;

    /** Time units per julian day. */
    public static final double MINUTES_PER_DAY = 1440.0;

    /** Potential perturbation coefficient. */
    public static final double XKE = 0.0743669161331734132;
    /** Potential perturbation coefficient. */
    public static final double XJ3 = -2.53881e-6;
    /** Potential perturbation coefficient. */
    public static final double XJ2 = 1.082616e-3;
    /** Potential perturbation coefficient. */
    public static final double XJ4 = -1.65597e-6;
    /** Potential perturbation coefficient. */
    public static final double CK2 = 0.5 * XJ2 * NORMALIZED_EQUATORIAL_RADIUS * NORMALIZED_EQUATORIAL_RADIUS;
    /** Potential perturbation coefficient. */
    public static final double CK4 = -0.375 * XJ4 * NORMALIZED_EQUATORIAL_RADIUS * NORMALIZED_EQUATORIAL_RADIUS *
            NORMALIZED_EQUATORIAL_RADIUS * NORMALIZED_EQUATORIAL_RADIUS;
    /** Potential perturbation coefficient. */
    public static final double S = NORMALIZED_EQUATORIAL_RADIUS * (1. + 78. / EARTH_RADIUS);
    /** Potential perturbation coefficient. */
    public static final double QOMS2T = 1.880279159015270643865e-9;
    /** Potential perturbation coefficient. */
    public static final double A3OVK2 = -XJ3 / CK2 * NORMALIZED_EQUATORIAL_RADIUS * NORMALIZED_EQUATORIAL_RADIUS *
            NORMALIZED_EQUATORIAL_RADIUS;

    // CHECKSTYLE: resume MagicNumber check
    // CHECKSTYLE: resume HideUtilityClassConstructor check
}
