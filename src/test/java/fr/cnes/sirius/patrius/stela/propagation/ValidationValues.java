/**
 * 
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
 * 
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:91:26/07/2013: new class
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.propagation;

import static fr.cnes.sirius.patrius.math.util.MathLib.toRadians;

/**
 * Validation values for Stela propagator Transition matrix tests
 * 
 * @author Rami Houdroge
 * @version $Id$
 * @since 2.1
 */
public class ValidationValues {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Stela GTO partial derivatives computation
         * 
         * @featureDescription test the Stela GTO partial derivatives computation
         * 
         * @coveredRequirements
         */
        STELA_GTO_PARTIAL_DERIVATIVES
    }

    // ***** LBN_TRANSITION_1 ***** (cases 1 to 4)
    /** input values */
    public static final int LBN_TRANSITION_1_IN_jjdeb = 17532;
    public static final int LBN_TRANSITION_1_IN_jjfin = 17897;
    public static final double LBN_TRANSITION_1_IN_a = 25000000.;
    public static final double LBN_TRANSITION_1_IN_eccentricity = 0.73;
    public static final double LBN_TRANSITION_1_IN_i = toRadians(2.);
    public static final double LBN_TRANSITION_1_IN_pom = toRadians(300.);
    public static final double LBN_TRANSITION_1_IN_gom = toRadians(30.);
    public static final double LBN_TRANSITION_1_IN_M = toRadians(10.);
    public static final double LBN_TRANSITION_1_IN_Cx = 2.2;
    public static final double LBN_TRANSITION_1_IN_refCoef = 2.;
    public static final double LBN_TRANSITION_1_IN_mass = 1000.0;
    public static final double LBN_TRANSITION_1_IN_dragArea = 1;
    public static final double LBN_TRANSITION_1_IN_refArea = 1;

    // ***** LBN_TRANSITION_5 ***** (cases 5 and 6)
    /** input values */
    public static final int LBN_TRANSITION_5_IN_jjdeb = 17532;
    public static final int LBN_TRANSITION_5_IN_jjfin = 17897;
    public static final double LBN_TRANSITION_5_IN_a = 8378136.3;
    public static final double LBN_TRANSITION_5_IN_eccentricity = 0.01;
    public static final double LBN_TRANSITION_5_IN_i = toRadians(104.8865478);
    public static final double LBN_TRANSITION_5_IN_pom = toRadians(90.);
    public static final double LBN_TRANSITION_5_IN_gom = toRadians(190.4708407);
    public static final double LBN_TRANSITION_5_IN_M = toRadians(10.);
    public static final double LBN_TRANSITION_5_IN_Cx = 2.2;
    public static final double LBN_TRANSITION_5_IN_refCoef = 2.;
    public static final double LBN_TRANSITION_5_IN_mass = 1000.0;
    public static final double LBN_TRANSITION_5_IN_dragArea = 1;
    public static final double LBN_TRANSITION_5_IN_refArea = 1;

}
