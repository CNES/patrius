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
 * @history creation 11/10/2012
 *
 * HISTORY
 * VERSION:4.13.5:DM:DM-319:03/07/2024:[PATRIUS] Assurer la compatibilite ascendante de la v4.13
 * VERSION:4.13.2:DM:DM-222:08/03/2024:[PATRIUS] Assurer la compatibilité ascendante
 * VERSION:4.13:FA:FA-106:08/12/2023:[PATRIUS] calcul alambique des jours
 * juliens dans TidesToolbox.computeFundamentalArguments()
 * VERSION:4.13:DM:DM-103:08/12/2023:[PATRIUS] Optimisation du CIRFProvider
 * VERSION:4.13:DM:DM-120:08/12/2023:[PATRIUS] Merge de la branche patrius-for-lotus dans Patrius
 * VERSION:4.13:DM:DM-108:08/12/2023:[PATRIUS] Modele d'obliquite et de precession de la Terre
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:660:24/09/2016:add getters to frames configuration
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.precessionnutation;

import java.io.InputStream;

import fr.cnes.sirius.patrius.data.BodiesElements;
import fr.cnes.sirius.patrius.data.PoissonSeries;
import fr.cnes.sirius.patrius.frames.configuration.FrameConvention;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.PatriusConfiguration;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;

//CHECKSTYLE: stop MagicNumber check
//Reason: model - Orekit code kept as such

/**
 * This class implement the IERS 2003 and 2010 CIRF Precession Nutation models.
 *
 * <p>
 * The computations of this class are very heavy. It should not be used directly but only through
 * {@link PrecessionNutationInterpolation} that will perform a limited number of access to this class.
 * </p>
 *
 * @see PrecessionNutationModel
 * @author Rami Houdroge
 * @version $Id: IERS20032010PrecessionNutation.java 18073 2017-10-02 16:48:07Z bignon $
 * @since 1.3
 */
public class IERS20032010PrecessionNutation implements PrecessionNutationModel {

    /** Serializable UID. */
    private static final long serialVersionUID = 3503289367436972436L;

    /** lunisolar nutation elements. */
    private static final double F10 = MathLib.toRadians(134.963_402_51);
    /** lunisolar nutation elements. */
    private static final double F11 = 715_923.217_8 * Constants.ARC_SECONDS_TO_RADIANS;
    /** lunisolar nutation elements. */
    private static final double F12 = 31.879_200 * Constants.ARC_SECONDS_TO_RADIANS;
    /** lunisolar nutation elements. */
    private static final double F13 = 0.051_635 * Constants.ARC_SECONDS_TO_RADIANS;
    /** lunisolar nutation elements. */
    private static final double F14 = -0.000_244_7 * Constants.ARC_SECONDS_TO_RADIANS;
    /** lunisolar nutation elements. */
    private static final double F20 = MathLib.toRadians(357.529_109_18);
    /** lunisolar nutation elements. */
    private static final double F21 = 1_292_581.048_1 * Constants.ARC_SECONDS_TO_RADIANS;
    /** lunisolar nutation elements. */
    private static final double F22 = -0.553_200 * Constants.ARC_SECONDS_TO_RADIANS;
    /** lunisolar nutation elements. */
    private static final double F23 = 0.000_136 * Constants.ARC_SECONDS_TO_RADIANS;
    /** lunisolar nutation elements. */
    private static final double F24 = -0.000_011_49 * Constants.ARC_SECONDS_TO_RADIANS;
    /** lunisolar nutation elements. */
    private static final double F30 = MathLib.toRadians(93.272_090_62);
    /** lunisolar nutation elements. */
    private static final double F31 = 295_262.847_8 * Constants.ARC_SECONDS_TO_RADIANS;
    /** lunisolar nutation elements. */
    private static final double F32 = -12.751_200 * Constants.ARC_SECONDS_TO_RADIANS;
    /** lunisolar nutation elements. */
    private static final double F33 = -0.001_037 * Constants.ARC_SECONDS_TO_RADIANS;
    /** lunisolar nutation elements. */
    private static final double F34 = 0.000_004_17 * Constants.ARC_SECONDS_TO_RADIANS;
    /** lunisolar nutation elements. */
    private static final double F40 = MathLib.toRadians(297.850_195_47);
    /** lunisolar nutation elements. */
    private static final double F41 = 1_105_601.209_0 * Constants.ARC_SECONDS_TO_RADIANS;
    /** lunisolar nutation elements. */
    private static final double F42 = -6.370_600 * Constants.ARC_SECONDS_TO_RADIANS;
    /** lunisolar nutation elements. */
    private static final double F43 = 0.006_593 * Constants.ARC_SECONDS_TO_RADIANS;
    /** lunisolar nutation elements. */
    private static final double F44 = -0.000_031_69 * Constants.ARC_SECONDS_TO_RADIANS;
    /** lunisolar nutation elements. */
    private static final double F50 = MathLib.toRadians(125.044_555_01);
    /** lunisolar nutation elements. */
    private static final double F51 = -482_890.543_1 * Constants.ARC_SECONDS_TO_RADIANS;
    /** lunisolar nutation elements. */
    private static final double F52 = 7.472_200 * Constants.ARC_SECONDS_TO_RADIANS;
    /** lunisolar nutation elements. */
    private static final double F53 = 0.007_702 * Constants.ARC_SECONDS_TO_RADIANS;
    /** lunisolar nutation elements. */
    private static final double F54 = -0.000_059_39 * Constants.ARC_SECONDS_TO_RADIANS;
    /** planetary nutation elements. */
    private static final double F60 = 4.402_608_842;
    /** planetary nutation elements. */
    private static final double F61 = 2_608.790_314_157_4;
    /** planetary nutation elements. */
    private static final double F70 = 3.176_146_697;
    /** planetary nutation elements. */
    private static final double F71 = 1_021.328_554_621_1;
    /** planetary nutation elements. */
    private static final double F80 = 1.753_470_314;
    /** planetary nutation elements. */
    private static final double F81 = 628.307_584_999_1;
    /** planetary nutation elements. */
    private static final double F90 = 6.203_480_913;
    /** planetary nutation elements. */
    private static final double F91 = 334.061_242_670_0;
    /** planetary nutation elements. */
    private static final double F100 = 0.599_546_497;
    /** planetary nutation elements. */
    private static final double F101 = 52.969_096_264_1;
    /** planetary nutation elements. */
    private static final double F110 = 0.874_016_757;
    /** planetary nutation elements. */
    private static final double F111 = 21.329_910_496_0;
    /** planetary nutation elements. */
    private static final double F120 = 5.481_293_872;
    /** planetary nutation elements. */
    private static final double F121 = 7.478_159_856_7;
    /** planetary nutation elements. */
    private static final double F130 = 5.311_886_287;
    /** planetary nutation elements. */
    private static final double F131 = 3.813_303_563_8;
    /** planetary nutation elements. */
    private static final double F141 = 0.024_381_750;
    /** planetary nutation elements. */
    private static final double F142 = 0.000_005_386_91;

    /** Exception message if the compatibility mode is unsupported. */
    private static final String UNSUPPORTED_MODE_EXCEPTION = "Unsupported compatibility mode : ";

    /** mas unit. */
    private static final double UNIT = 1E-6;

    /** Pole position (X). */
    private final PoissonSeries xDevelopment;

    /** Pole position (Y). */
    private final PoissonSeries yDevelopment;

    /** Pole position (S + XY/2). */
    private final PoissonSeries sxy2Development;

    /** IERS convention. */
    private final FrameConvention iersConvention;

    /** Current set. */
    private CIPCoordinates currentSet;

    /** Compute dvs. */
    private boolean rotation;

    /**
     * Constructor.
     *
     * @param convention
     *        IERS convention to use
     */
    public IERS20032010PrecessionNutation(final PrecessionNutationConvention convention) {
        final String[] dataLocation = convention.getDataLocation();
        this.xDevelopment = loadModel(dataLocation[0]);
        this.yDevelopment = loadModel(dataLocation[1]);
        this.sxy2Development = loadModel(dataLocation[2]);
        this.iersConvention = convention == PrecessionNutationConvention.IERS2003 ? FrameConvention.IERS2003
            : FrameConvention.IERS2010;
    }

    /**
     * Constructor.
     * 
     * @param convention
     *        IERS convention to use
     * @param nonConstantRotation
     *        true if derivatives are to be computed, false otherwise
     */
    public IERS20032010PrecessionNutation(final PrecessionNutationConvention convention,
                                          final boolean nonConstantRotation) {
        this.xDevelopment = loadModel(convention.getDataLocation()[0]);
        this.yDevelopment = loadModel(convention.getDataLocation()[1]);
        this.sxy2Development = loadModel(convention.getDataLocation()[2]);
        this.currentSet = this.computePoleCoordinates(AbsoluteDate.J2000_EPOCH);
        this.rotation = nonConstantRotation;
        this.iersConvention =
            convention == PrecessionNutationConvention.IERS2003 ? FrameConvention.IERS2003 : FrameConvention.IERS2010;
    }

    /**
     * Compute pole coordinates from precession and nutation effects.
     *
     * @param date
     *        date
     * @return a {@link CIPCoordinates} set
     */
    private CIPCoordinates computePoleCoordinates(final AbsoluteDate date) {

        final double tc;
        switch (PatriusConfiguration.getPatriusCompatibilityMode()) {
            case OLD_MODELS:
            case MIXED_MODELS:
                tc = date.durationFrom(AbsoluteDate.J2000_EPOCH) / Constants.JULIAN_CENTURY;
                break;

            case NEW_MODELS:
                // Duration in centuries since J2000 epoch
                tc = date.durationFromJ2000EpochInCenturies();
                break;

            default:
                throw new IllegalArgumentException(
                    UNSUPPORTED_MODE_EXCEPTION + PatriusConfiguration.getPatriusCompatibilityMode());
        }

        final BodiesElements elements = new BodiesElements((((F14 * tc + F13) * tc + F12) * tc + F11) * tc + F10 +
        // mean
                mod(1325 * tc) * MathUtils.TWO_PI,
            // anomaly
            // of
            // the
            // Moon
            // mean anomaly
            (((F24 * tc + F23) * tc + F22) * tc + F21) * tc + F20 + mod(99 * tc) * MathUtils.TWO_PI,
            // of the Sun
            // L -
            (((F34 * tc + F33) * tc + F32) * tc + F31) * tc + F30 + mod(1342 * tc) * MathUtils.TWO_PI,
            // &Omega;
            // where L is
            // the mean
            // longitude
            // of the
            // Moon
            // mean
            (((F44 * tc + F43) * tc + F42) * tc + F41) * tc + F40 + mod(1236 * tc) * MathUtils.TWO_PI,
            // elongation
            // of the
            // Moon from
            // the Sun
            // mean
            (((F54 * tc + F53) * tc + F52) * tc + F51) * tc + F50 + mod(-5 * tc) * MathUtils.TWO_PI,
            // longitude of
            // the
            // ascending
            // node of the
            // Moon
            // mean Mercury longitude
            F61 * tc + F60,
            // mean Venus longitude
            F71 * tc + F70,
            // mean Earth longitude
            F81 * tc + F80,
            // mean Mars longitude
            F91 * tc + F90,
            // mean Jupiter longitude
            F101 * tc + F100,
            // mean Saturn longitude
            F111 * tc + F110,
            // mean Uranus longitude
            F121 * tc + F120,
            // mean Neptune longitude
            F131 * tc + F130,
            // general accumulated precession in longitude
            (F142 * tc + F141) * tc);

        final BodiesElements elementsP = new BodiesElements(
            // mean anomaly of the Moon
            (((4 * F14 * tc + 3 * F13) * tc + 2 * F12) * tc + F11 + 1325 * MathUtils.TWO_PI) /
                    Constants.JULIAN_CENTURY,
            // mean anomaly of the Sun
            (((4 * F24 * tc + 3 * F23) * tc + 2 * F22) * tc + F21 + 99 * MathUtils.TWO_PI) /
                    Constants.JULIAN_CENTURY,
            // L - &Omega; where L is the mean longitude of the Moon
            (((4 * F34 * tc + 3 * F33) * tc + 2 * F32) * tc + F31 + 1342 * MathUtils.TWO_PI) /
                    Constants.JULIAN_CENTURY,
            // mean elongation of the Moon from the Sun
            (((4 * F44 * tc + 3 * F43) * tc + 2 * F42) * tc + F41 + 1236 * MathUtils.TWO_PI) /
                    Constants.JULIAN_CENTURY,
            // mean longitude of the ascending node of the Moon
            (((4 * F54 * tc + 3 * F53) * tc + 2 * F52) * tc + F51 - 5 * MathUtils.TWO_PI) /
                    Constants.JULIAN_CENTURY,
            // mean Mercury longitude
            F61 / Constants.JULIAN_CENTURY,
            // mean Venus longitude
            F71 / Constants.JULIAN_CENTURY,
            // mean Earth longitude
            F81 / Constants.JULIAN_CENTURY,
            // mean Mars longitude
            F91 / Constants.JULIAN_CENTURY,
            // mean Jupiter longitude
            F101 / Constants.JULIAN_CENTURY,
            // mean Saturn longitude
            F111 / Constants.JULIAN_CENTURY,
            // mean Uranus longitude
            F121 / Constants.JULIAN_CENTURY,
            // mean Neptune longitude
            F131 / Constants.JULIAN_CENTURY,
            // general accumulated precession in longitude
            (2 * F142 * tc + F141) / Constants.JULIAN_CENTURY);

        // pole position
        final double[] resX;
        final double[] resY;
        final double[] resS;

        switch (PatriusConfiguration.getPatriusCompatibilityMode()) {
            case OLD_MODELS:
            case MIXED_MODELS:
                if (this.rotation) {
                    // non constant rotation
                    resX = this.xDevelopment.value(tc, elements, elementsP);
                    resY = this.yDevelopment.value(tc, elements, elementsP);
                    resS = this.sxy2Development.value(tc, elements, elementsP);
                } else {
                    // constant rotation ; derivation term is zero.
                    final double[] cresX = { this.xDevelopment.value(tc, elements), 0. };
                    final double[] cresY = { this.yDevelopment.value(tc, elements), 0. };
                    final double[] cresS = { this.sxy2Development.value(tc, elements), 0. };
                    resX = cresX;
                    resY = cresY;
                    resS = cresS;
                }
                break;

            case NEW_MODELS:
                resX = this.xDevelopment.value(tc, elements, elementsP);
                resY = this.yDevelopment.value(tc, elements, elementsP);
                resS = this.sxy2Development.value(tc, elements, elementsP);
                break;

            default:
                throw new IllegalArgumentException(
                    UNSUPPORTED_MODE_EXCEPTION + PatriusConfiguration.getPatriusCompatibilityMode());
        }

        return new CIPCoordinates(date, resX[0], resX[1], resY[0], resY[1],
            resS[0] - resX[0] * resY[0] / 2., resS[1] - resX[0] * resY[1] / 2. - resX[1] * resY[0] / 2.);
    }

    /**
     * Compute the fractional part of the number.
     *
     * @param d
     *        number
     * @return fractional part of d
     */
    private static double mod(final double d) {
        return d - (int) d;
    }

    /**
     * Load a series development model.
     *
     * @param name
     *        file name of the series development
     * @return series development model
     */
    private static PoissonSeries loadModel(final String name) {

        // get the table data
        final InputStream stream = IERS20032010PrecessionNutation.class.getResourceAsStream(name);

        // nutation models are in micro arcseconds in the data files
        // we store and use them in radians
        try {
            return new PoissonSeries(stream, Constants.ARC_SECONDS_TO_RADIANS * UNIT, name);
        } catch (final PatriusException e) {
            throw new PatriusExceptionWrapper(e);
        }

    }

    /** {@inheritDoc} */
    @Override
    public boolean isDirect() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public FrameConvention getOrigin() {
        return this.iersConvention;
    }

    /** {@inheritDoc} */
    @Override
    public CIPCoordinates getCIPCoordinates(final AbsoluteDate date) {
        return this.computePoleCoordinates(date);
    }

    // CHECKSTYLE: resume MagicNumber check
}
