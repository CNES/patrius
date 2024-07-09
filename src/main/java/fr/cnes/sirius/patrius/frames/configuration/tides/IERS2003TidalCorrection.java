/**
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
 * @history creation 11/10/2012
 *
 * HISTORY
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:144:06/11/2013:Changed UT1-UTC correction to UT1-TAI correction
 * VERSION::DM:660:24/09/2016:add getters to frames configuration
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.tides;

import fr.cnes.sirius.patrius.frames.configuration.FrameConvention;
import fr.cnes.sirius.patrius.frames.configuration.eop.PoleCorrection;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;

// CHECKSTYLE: stop MagicNumber check
// Reason: model - Orekit code

/**
 * Compute tidal correction to the pole motion.
 * 
 * <p>
 * This class computes the diurnal and semidiurnal variations in the Earth orientation. It is a java translation of the
 * fortran subroutine found at <a
 * href="ftp://tai.bipm.org/iers/conv2003/chapter8/ortho_eop.f">ftp://tai.bipm.org/iers/conv2003
 * /chapter8/ortho_eop.f</a>.
 * </p>
 * 
 * <p>
 * This class has been adapted from the TidalCorrection Orekit class.
 * </p>
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: IERS2003TidalCorrection.java 18073 2017-10-02 16:48:07Z bignon $
 * 
 * @since 1.3
 */
@SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
public class IERS2003TidalCorrection implements TidalCorrectionModel {

    /** Serial UID. */
    private static final long serialVersionUID = -7697143966464686404L;

    /** pi;/2. */
    private static final double HALF_PI = FastMath.PI / 2.0;

    /** Angular units conversion factor. */
    private static final double MICRO_ARC_SECONDS_TO_RADIANS = Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6;

    /** Time units conversion factor. */
    private static final double MICRO_SECONDS_TO_SECONDS = 1.0e-6;

    /** HS parameter. */
    private static final double[] HS = { -001.94, -001.25, -006.64, -001.51, -008.02, -009.47, -050.20, -001.80,
        -009.54, +001.52, -049.45, -262.21, +001.70, +003.43, +001.94, +001.37, +007.41, +020.62, +004.14, +003.94,
        -007.14, +001.37, -122.03, +001.02, +002.89, -007.30, +368.78, +050.01, -001.08, +002.93, +005.25, +003.95,
        +020.62, +004.09, +003.42, +001.69, +011.29, +007.23, +001.51, +002.16, +001.38, +001.80, +004.67, +016.01,
        +019.32, +001.30, -001.02, -004.51, +120.99, +001.13, +022.98, +001.06, -001.90, -002.18, -023.58, +631.92,
        +001.92, -004.66, -017.86, +004.47, +001.97, +017.20, +294.00, -002.46, -001.02, +079.96, +023.83, +002.59,
        +004.47, +001.95, +001.17 };

    /** PHASE parameter. */
    private static final double[] PHASE = { +09.0899831 - HALF_PI, +08.8234208 - HALF_PI, +12.1189598 - HALF_PI,
        +01.4425700 - HALF_PI, +04.7381090 - HALF_PI, +04.4715466 - HALF_PI, +07.7670857 - HALF_PI,
        -02.9093042 - HALF_PI, +00.3862349 - HALF_PI, -03.1758666 - HALF_PI, +00.1196725 - HALF_PI,
        +03.4152116 - HALF_PI, +12.8946194 - HALF_PI, +05.5137686 - HALF_PI, +06.4441883 - HALF_PI,
        -04.2322016 - HALF_PI, -00.9366625 - HALF_PI, +08.5427453 - HALF_PI, +11.8382843 - HALF_PI,
        +01.1618945 - HALF_PI, +05.9693878 - HALF_PI, -01.2032249 - HALF_PI, +02.0923141 - HALF_PI,
        -01.7847596 - HALF_PI, +08.0679449 - HALF_PI, +00.8953321 - HALF_PI, +04.1908712 - HALF_PI,
        +07.4864102 - HALF_PI, +10.7819493 - HALF_PI, +00.3137975 - HALF_PI, +06.2894282 - HALF_PI,
        +07.2198478 - HALF_PI, -00.1610030 - HALF_PI, +03.1345361 - HALF_PI, +02.8679737 - HALF_PI,
        -04.5128771 - HALF_PI, +04.9665307 - HALF_PI, +08.2620698 - HALF_PI, +11.5576089 - HALF_PI,
        +00.6146566 - HALF_PI, +03.9101957 - HALF_PI, +20.6617051, +13.2808543, +16.3098310, +08.9289802,
        +05.0519065, +15.8350306, +08.6624178, +11.9579569, +08.0808832, +04.5771061, +00.7000324, +14.9869335,
        +11.4831564, +04.3105437, +07.6060827, +03.7290090, +10.6350594, +03.2542086, +12.7336164, +16.0291555,
        +10.1602590, +06.2831853, +02.4061116, +05.0862033, +08.3817423, +11.6772814, +14.9728205, +04.0298682,
        +07.3254073, +09.1574019 };

    /** FREQUENCY parameter. */
    private static final double[] FREQUENCY = { 05.18688050, 05.38346657, 05.38439079, 05.41398343, 05.41490765,
        05.61149372, 05.61241794, 05.64201057, 05.64293479, 05.83859664, 05.83952086, 05.84044508, 05.84433381,
        05.87485066, 06.03795537, 06.06754801, 06.06847223, 06.07236095, 06.07328517, 06.10287781, 06.24878055,
        06.26505830, 06.26598252, 06.28318449, 06.28318613, 06.29946388, 06.30038810, 06.30131232, 06.30223654,
        06.31759007, 06.33479368, 06.49789839, 06.52841524, 06.52933946, 06.72592553, 06.75644239, 06.76033111,
        06.76125533, 06.76217955, 06.98835826, 06.98928248, 11.45675174, 11.48726860, 11.68477889, 11.71529575,
        11.73249771, 11.89560406, 11.91188181, 11.91280603, 11.93000800, 11.94332289, 11.96052486, 12.11031632,
        12.12363121, 12.13990896, 12.14083318, 12.15803515, 12.33834347, 12.36886033, 12.37274905, 12.37367327,
        12.54916865, 12.56637061, 12.58357258, 12.59985198, 12.60077620, 12.60170041, 12.60262463, 12.82880334,
        12.82972756, 13.06071921 };

    /** Orthotide weight factors. */
    private static final double[] SP = { 0.0298, 0.1408, 0.0805, 0.6002, 0.3025, 0.1517, 0.0200, 0.0905, 0.0638,
        0.3476, 0.1645, 0.0923 };

    /** Orthoweights for X polar motion. */
    private static final double[] ORTHOWX = { -06.77832 * MICRO_ARC_SECONDS_TO_RADIANS,
        -14.86323 * MICRO_ARC_SECONDS_TO_RADIANS, +00.47884 * MICRO_ARC_SECONDS_TO_RADIANS,
        -01.45303 * MICRO_ARC_SECONDS_TO_RADIANS, +00.16406 * MICRO_ARC_SECONDS_TO_RADIANS,
        +00.42030 * MICRO_ARC_SECONDS_TO_RADIANS, +00.09398 * MICRO_ARC_SECONDS_TO_RADIANS,
        +25.73054 * MICRO_ARC_SECONDS_TO_RADIANS, -04.77974 * MICRO_ARC_SECONDS_TO_RADIANS,
        +00.28080 * MICRO_ARC_SECONDS_TO_RADIANS, +01.94539 * MICRO_ARC_SECONDS_TO_RADIANS,
        -00.73089 * MICRO_ARC_SECONDS_TO_RADIANS };

    /** Orthoweights for Y polar motion. */
    private static final double[] ORTHOWY = { +14.86283 * MICRO_ARC_SECONDS_TO_RADIANS,
        -06.77846 * MICRO_ARC_SECONDS_TO_RADIANS, +01.45234 * MICRO_ARC_SECONDS_TO_RADIANS,
        +00.47888 * MICRO_ARC_SECONDS_TO_RADIANS, -00.42056 * MICRO_ARC_SECONDS_TO_RADIANS,
        +00.16469 * MICRO_ARC_SECONDS_TO_RADIANS, +15.30276 * MICRO_ARC_SECONDS_TO_RADIANS,
        -04.30615 * MICRO_ARC_SECONDS_TO_RADIANS, +00.07564 * MICRO_ARC_SECONDS_TO_RADIANS,
        +02.28321 * MICRO_ARC_SECONDS_TO_RADIANS, -00.45717 * MICRO_ARC_SECONDS_TO_RADIANS,
        -01.62010 * MICRO_ARC_SECONDS_TO_RADIANS };

    /** Orthoweights for UT1. */
    private static double[] orthowt = { -1.76335 * MICRO_SECONDS_TO_SECONDS, +1.03364 * MICRO_SECONDS_TO_SECONDS,
        -0.27553 * MICRO_SECONDS_TO_SECONDS, +0.34569 * MICRO_SECONDS_TO_SECONDS,
        -0.12343 * MICRO_SECONDS_TO_SECONDS, -0.10146 * MICRO_SECONDS_TO_SECONDS,
        -0.47119 * MICRO_SECONDS_TO_SECONDS, +1.28997 * MICRO_SECONDS_TO_SECONDS,
        -0.19336 * MICRO_SECONDS_TO_SECONDS, +0.02724 * MICRO_SECONDS_TO_SECONDS,
        +0.08955 * MICRO_SECONDS_TO_SECONDS, +0.04726 * MICRO_SECONDS_TO_SECONDS };

    /** Offset from reference epoch (days). */
    private static final double OFFSET = 37076.5;

    /** Number of elements. */
    private static final int ELEMENTS_NUMBER = 41;

    /** 11. */
    private static final int ELEVEN = 11;

    /** token. */
    private final Object token = new Object();

    /** Current set. */
    private TidalCorrection currentSet;

    /**
     * Simple constructor.
     */
    public IERS2003TidalCorrection() {
        this.currentSet = this.computeCorrections(AbsoluteDate.J2000_EPOCH);
    }

    /**
     * Compute the partials of the tidal variations to the orthoweights.
     * 
     * @param date
     *        date at which corrections are to be computed
     * @return a TidesCorrection object containing a {@link PoleCorrection} and a double for the time gap correction
     */
    // CHECKSTYLE: stop CommentRatio check
    //CHECKSTYLE: stop MethodLength check
    // Reason: model - Orekit code containing lots of arrays models
    protected TidalCorrection computeCorrections(final AbsoluteDate date) {
        //CHECKSTYLE: resume MethodLength check
        // CHECKSTYLE: resume CommentRatio check

        final double t = date.durationFrom(AbsoluteDate.MODIFIED_JULIAN_EPOCH) / Constants.JULIAN_DAY - OFFSET;

        // compute the time dependent potential matrix
        final double d60A = t + 2;
        final double d60B = t;
        final double d60C = t - 2;

        double anm00 = 0;
        double anm01 = 0;
        double anm02 = 0;
        double bnm00 = 0;
        double bnm01 = 0;
        double bnm02 = 0;
        for (int j = 0; j < ELEMENTS_NUMBER; j++) {

            final double hsj = HS[j];
            final double pj = PHASE[j];
            final double fj = FREQUENCY[j];

            final double alphaA = pj + fj * d60A;
            final double[] sincosA = MathLib.sinAndCos(alphaA);
            final double sinA = sincosA[0];
            final double cosA = sincosA[1];
            anm00 += hsj * cosA;
            bnm00 -= hsj * sinA;

            final double alphaB = pj + fj * d60B;
            final double[] sincosB = MathLib.sinAndCos(alphaB);
            final double sinB = sincosB[0];
            final double cosB = sincosB[1];
            anm01 += hsj * cosB;
            bnm01 -= hsj * sinB;

            final double alphaC = pj + fj * d60C;
            final double[] sincosC = MathLib.sinAndCos(alphaC);
            final double sinC = sincosC[0];
            final double cosC = sincosC[1];
            anm02 += hsj * cosC;
            bnm02 -= hsj * sinC;

        }

        double anm10 = 0;
        double anm11 = 0;
        double anm12 = 0;
        double bnm10 = 0;
        double bnm11 = 0;
        double bnm12 = 0;
        for (int j = ELEMENTS_NUMBER; j < HS.length; j++) {

            final double hsj = HS[j];
            final double pj = PHASE[j];
            final double fj = FREQUENCY[j];

            final double alphaA = pj + fj * d60A;
            anm10 += hsj * MathLib.cos(alphaA);
            bnm10 -= hsj * MathLib.sin(alphaA);

            final double alphaB = pj + fj * d60B;
            anm11 += hsj * MathLib.cos(alphaB);
            bnm11 -= hsj * MathLib.sin(alphaB);

            final double alphaC = pj + fj * d60C;
            anm12 += hsj * MathLib.cos(alphaC);
            bnm12 -= hsj * MathLib.sin(alphaC);

        }

        // orthogonalize the response terms ...
        final double ap0 = anm02 + anm00;
        final double am0 = anm02 - anm00;
        final double bp0 = bnm02 + bnm00;
        final double bm0 = bnm02 - bnm00;
        final double ap1 = anm12 + anm10;
        final double am1 = anm12 - anm10;
        final double bp1 = bnm12 + bnm10;
        final double bm1 = bnm12 - bnm10;

        // ... and fill partials vector
        final double partials0 = SP[0] * anm01;
        final double partials1 = SP[0] * bnm01;
        final double partials2 = SP[1] * anm01 - SP[2] * ap0;
        final double partials3 = SP[1] * bnm01 - SP[2] * bp0;
        final double partials4 = SP[3] * anm01 - SP[4] * ap0 + SP[5] * bm0;
        final double partials5 = SP[3] * bnm01 - SP[4] * bp0 - SP[5] * am0;
        final double partials6 = SP[6] * anm11;
        final double partials7 = SP[6] * bnm11;
        final double partials8 = SP[7] * anm11 - SP[8] * ap1;
        final double partials9 = SP[7] * bnm11 - SP[8] * bp1;
        final double partials10 = SP[9] * anm11 - SP[10] * ap1 + SP[ELEVEN] * bm1;
        final double partials11 = SP[9] * bnm11 - SP[10] * bp1 - SP[ELEVEN] * am1;

        // combine partials to set up corrections
        final double dxCurrent = partials0 * ORTHOWX[0] + partials1 * ORTHOWX[1] + partials2 * ORTHOWX[2] + partials3 *
            ORTHOWX[3] + partials4 * ORTHOWX[4] + partials5 * ORTHOWX[5] + partials6 * ORTHOWX[6] + partials7 *
            ORTHOWX[7] + partials8 * ORTHOWX[8] + partials9 * ORTHOWX[9] + partials10 * ORTHOWX[10] + partials11 *
            ORTHOWX[ELEVEN];
        final double dyCurrent = partials0 * ORTHOWY[0] + partials1 * ORTHOWY[1] + partials2 * ORTHOWY[2] + partials3 *
            ORTHOWY[3] + partials4 * ORTHOWY[4] + partials5 * ORTHOWY[5] + partials6 * ORTHOWY[6] + partials7 *
            ORTHOWY[7] + partials8 * ORTHOWY[8] + partials9 * ORTHOWY[9] + partials10 * ORTHOWY[10] + partials11 *
            ORTHOWY[ELEVEN];
        final double dtCurrent = partials0 * orthowt[0] + partials1 * orthowt[1] + partials2 * orthowt[2] + partials3 *
            orthowt[3] + partials4 * orthowt[4] + partials5 * orthowt[5] + partials6 * orthowt[6] + partials7 *
            orthowt[7] + partials8 * orthowt[8] + partials9 * orthowt[9] + partials10 * orthowt[10] + partials11 *
            orthowt[ELEVEN];

        return new TidalCorrection(date, new PoleCorrection(dxCurrent, dyCurrent), dtCurrent, 0);

    }

    /** {@inheritDoc} */
    @Override
    public PoleCorrection getPoleCorrection(final AbsoluteDate date) {
        synchronized (this.token) {
            if (MathLib.abs(this.currentSet.getDate().durationFrom(date)) < Precision.EPSILON) {
                return this.currentSet.getPoleCorrection();
            } else {
                this.currentSet = this.computeCorrections(date);
                return this.currentSet.getPoleCorrection();
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public double getUT1Correction(final AbsoluteDate date) {
        synchronized (this.token) {
            if (MathLib.abs(this.currentSet.getDate().durationFrom(date)) < Precision.EPSILON) {
                return this.currentSet.getUT1Correction();
            } else {
                this.currentSet = this.computeCorrections(date);
                return this.currentSet.getUT1Correction();
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public double getLODCorrection(final AbsoluteDate t) {
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public FrameConvention getOrigin() {
        return FrameConvention.IERS2003;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDirect() {
        return true;
    }

    // CHECKSTYLE: resume MagicNumber check

}
