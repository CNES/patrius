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
* VERSION:4.13:FA:FA-106:08/12/2023:[PATRIUS] calcul alambique des jours 
 *          juliens dans TidesToolbox.computeFundamentalArguments() 
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.7:DM:DM-2590:18/05/2021:Configuration des TransformProvider 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:489:12/01/2016:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:524:25/05/2016:serialization java doc
 * VERSION::FA:1301:06/09/2017:Generalized EOP history
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.transformations;

import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfiguration;
import fr.cnes.sirius.patrius.frames.configuration.eop.EOP1980History;
import fr.cnes.sirius.patrius.frames.configuration.eop.NutationCorrection;
import fr.cnes.sirius.patrius.frames.configuration.eop.PoleCorrection;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.TimeStampedCacheException;

/**
 * True Equator, Mean Equinox of Date Frame.
 * <p>
 * This frame handles nutation effects according to the IAU-80 theory.
 * </p>
 * <p>
 * Its parent frame is the {@link MODProvider}.
 * </p>
 * <p>
 * It is sometimes called True of Date (ToD) frame.
 * <p>
 * 
 * <p>
 * Spin derivative is never computed and is either 0 or null. No analytical formula is currently available for spin
 * derivative. Spin is also 0.
 * </p>
 * <p>
 * Frames configuration nutation correction is used.
 * </p>
 * 
 * @serial serializable given a serializable attribut {@link EOP1980History}.
 *         used through FactoryManagedFrame. An instance of TODProvider
 *         does not need to be serialized if a FactoryManagedFrame TOD is serialized.
 * @author Pascal Parraud
 */
@SuppressWarnings("PMD.NullAssignment")
public final class TODProvider implements TransformProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 7013467596084047566L;

    /** 0.1 mas unit. */
    private static final double UNIT = 1.e-4;

    // CHECKSTYLE: stop JavadocVariable check

    // Coefficients for the Equation of the Equinoxes.
    /** Model coefficient. */
    private static final double EQE_1 = 0.00264 * Constants.ARC_SECONDS_TO_RADIANS;
    /** Model coefficient. */
    private static final double EQE_2 = 0.000063 * Constants.ARC_SECONDS_TO_RADIANS;

    // Coefficients for the Mean Obliquity of the Ecliptic.
    /** Model coefficient. */
    private static final double MOE_0 = 84381.448 * Constants.ARC_SECONDS_TO_RADIANS;
    /** Model coefficient. */
    private static final double MOE_1 = -46.8150 * Constants.ARC_SECONDS_TO_RADIANS;
    /** Model coefficient. */
    private static final double MOE_2 = -0.00059 * Constants.ARC_SECONDS_TO_RADIANS;
    /** Model coefficient. */
    private static final double MOE_3 = 0.001813 * Constants.ARC_SECONDS_TO_RADIANS;

    // lunisolar nutation elements
    // Coefficients for l (Mean Anomaly of the Moon).
    /** Model coefficient. */
    private static final double F10 = MathLib.toRadians(134.96340251);
    /** Model coefficient. */
    private static final double F110 = 715923.2178 * Constants.ARC_SECONDS_TO_RADIANS;
    /** Model coefficient. */
    private static final double F111 = 1325.0;
    /** Model coefficient. */
    private static final double F12 = 31.87908 * Constants.ARC_SECONDS_TO_RADIANS;
    /** Model coefficient. */
    private static final double F13 = 0.0516348 * Constants.ARC_SECONDS_TO_RADIANS;

    // Coefficients for l' (Mean Anomaly of the Sun).
    /** Model coefficient. */
    private static final double F20 = MathLib.toRadians(357.52910918);
    /** Model coefficient. */
    private static final double F210 = 1292581.048 * Constants.ARC_SECONDS_TO_RADIANS;
    /** Model coefficient. */
    private static final double F211 = 99.0;
    /** Model coefficient. */
    private static final double F22 = -0.55332 * Constants.ARC_SECONDS_TO_RADIANS;
    /** Model coefficient. */
    private static final double F23 = 0.0001368 * Constants.ARC_SECONDS_TO_RADIANS;

    // Coefficients for F = L (Mean Longitude of the Moon) - Omega.
    /** Model coefficient. */
    private static final double F30 = MathLib.toRadians(93.27209062);
    /** Model coefficient. */
    private static final double F310 = 295262.8477 * Constants.ARC_SECONDS_TO_RADIANS;
    /** Model coefficient. */
    private static final double F311 = 1342.0;
    /** Model coefficient. */
    private static final double F32 = -12.7512 * Constants.ARC_SECONDS_TO_RADIANS;
    /** Model coefficient. */
    private static final double F33 = -0.0010368 * Constants.ARC_SECONDS_TO_RADIANS;

    // Coefficients for D (Mean Elongation of the Moon from the Sun).
    /** Model coefficient. */
    private static final double F40 = MathLib.toRadians(297.85019547);
    /** Model coefficient. */
    private static final double F410 = 1105601.209 * Constants.ARC_SECONDS_TO_RADIANS;
    /** Model coefficient. */
    private static final double F411 = 1236.0;
    /** Model coefficient. */
    private static final double F42 = -6.37056 * Constants.ARC_SECONDS_TO_RADIANS;
    /** Model coefficient. */
    private static final double F43 = 0.0065916 * Constants.ARC_SECONDS_TO_RADIANS;

    // Coefficients for Omega (Mean Longitude of the Ascending Node of the Moon).
    /** Model coefficient. */
    private static final double F50 = MathLib.toRadians(125.0445501);
    /** Model coefficient. */
    private static final double F510 = -482890.2665 * Constants.ARC_SECONDS_TO_RADIANS;
    /** Model coefficient. */
    private static final double F511 = -5.0;
    /** Model coefficient. */
    private static final double F52 = 7.4722 * Constants.ARC_SECONDS_TO_RADIANS;
    /** Model coefficient. */
    private static final double F53 = 0.007702 * Constants.ARC_SECONDS_TO_RADIANS;

    // CHECKSTYLE: resume JavadocVariable check

    /** coefficients of l, mean anomaly of the Moon. */
    private static final int[] CL = {
        +0, 0, -2, 2, -2, 1, 0, 2, 0, 0,
        +0, 0, 0, 2, 0, 0, 0, 0, 0, -2,
        +0, 2, 0, 1, 2, 0, 0, 0, -1, 0,
        +0, 1, 0, 1, 1, -1, 0, 1, -1, -1,
        +1, 0, 2, 1, 2, 0, -1, -1, 1, -1,
        +1, 0, 0, 1, 1, 2, 0, 0, 1, 0,
        +1, 2, 0, 1, 0, 1, 1, 1, -1, -2,
        +3, 0, 1, -1, 2, 1, 3, 0, -1, 1,
        -2, -1, 2, 1, 1, -2, -1, 1, 2, 2,
        +1, 0, 3, 1, 0, -1, 0, 0, 0, 1,
        +0, 1, 1, 2, 0, 0
    };

    /** coefficients of l', mean anomaly of the Sun. */
    private static final int[] CLP = {
        +0, 0, 0, 0, 0, -1, -2, 0, 0, 1,
        +1, -1, 0, 0, 0, 2, 1, 2, -1, 0,
        -1, 0, 1, 0, 1, 0, 1, 1, 0, 1,
        +0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        +0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        +1, 1, -1, 0, 0, 0, 0, 0, 0, 0,
        -1, 0, 1, 0, 0, 1, 0, -1, -1, 0,
        +0, -1, 1, 0, 0, 0, 0, 0, 0, 0,
        +0, 0, 0, 1, 0, 0, 0, -1, 0, 0,
        +0, 0, 0, 0, 1, -1, 0, 0, 1, 0,
        -1, 1, 0, 0, 0, 1
    };

    /** coefficients of F = L - &Omega, where L is the mean longitude of the Moon. */
    private static final int[] CF = {
        0, 0, 2, -2, 2, 0, 2, -2, 2, 0,
        2, 2, 2, 0, 2, 0, 0, 2, 0, 0,
        2, 0, 2, 0, 0, -2, -2, 0, 0, 2,
        2, 0, 2, 2, 0, 2, 0, 0, 0, 2,
        2, 2, 0, 2, 2, 2, 2, 0, 0, 2,
        0, 2, 2, 2, 0, 2, 0, 2, 2, 0,
        0, 2, 0, -2, 0, 0, 2, 2, 2, 0,
        2, 2, 2, 2, 0, 0, 0, 2, 0, 0,
        2, 2, 0, 2, 2, 2, 4, 0, 2, 2,
        0, 4, 2, 2, 2, 0, -2, 2, 0, -2,
        2, 0, -2, 0, 2, 0
    };

    /** coefficients of D, mean elongation of the Moon from the Sun. */
    private static final int[] CD = {
        +0, 0, 0, 0, 0, -1, -2, 0, -2, 0,
        -2, -2, -2, -2, -2, 0, 0, -2, 0, 2,
        -2, -2, -2, -1, -2, 2, 2, 0, 1, -2,
        +0, 0, 0, 0, -2, 0, 2, 0, 0, 2,
        +0, 2, 0, -2, 0, 0, 0, 2, -2, 2,
        -2, 0, 0, 2, 2, -2, 2, 2, -2, -2,
        +0, 0, -2, 0, 1, 0, 0, 0, 2, 0,
        +0, 2, 0, -2, 0, 0, 0, 1, 0, -4,
        +2, 4, -4, -2, 2, 4, 0, -2, -2, 2,
        +2, -2, -2, -2, 0, 2, 0, -1, 2, -2,
        +0, -2, 2, 2, 4, 1
    };

    /** coefficients of &Omega, mean longitude of the ascending node of the Moon. */
    private static final int[] COM = {
        1, 2, 1, 0, 2, 0, 1, 1, 2, 0,
        2, 2, 1, 0, 0, 0, 1, 2, 1, 1,
        1, 1, 1, 0, 0, 1, 0, 2, 1, 0,
        2, 0, 1, 2, 0, 2, 0, 1, 1, 2,
        1, 2, 0, 2, 2, 0, 1, 1, 1, 1,
        0, 2, 2, 2, 0, 2, 1, 1, 1, 1,
        0, 1, 0, 0, 0, 0, 0, 2, 2, 1,
        2, 2, 2, 1, 1, 2, 0, 2, 2, 0,
        2, 2, 0, 2, 1, 2, 2, 0, 1, 2,
        1, 2, 2, 0, 1, 1, 1, 2, 0, 0,
        1, 1, 0, 0, 2, 0
    };

    /** coefficients for nutation in longitude, const part, in 0.1milliarcsec. */
    private static final double[] SL = {
        -171996.0, 2062.0, 46.0, 11.0, -3.0, -3.0, -2.0, 1.0, -13187.0, 1426.0,
        -517.0, 217.0, 129.0, 48.0, -22.0, 17.0, -15.0, -16.0, -12.0, -6.0,
        -5.0, 4.0, 4.0, -4.0, 1.0, 1.0, -1.0, 1.0, 1.0, -1.0,
        -2274.0, 712.0, -386.0, -301.0, -158.0, 123.0, 63.0, 63.0, -58.0, -59.0,
        -51.0, -38.0, 29.0, 29.0, -31.0, 26.0, 21.0, 16.0, -13.0, -10.0,
        -7.0, 7.0, -7.0, -8.0, 6.0, 6.0, -6.0, -7.0, 6.0, -5.0,
        +5.0, -5.0, -4.0, 4.0, -4.0, -3.0, 3.0, -3.0, -3.0, -2.0,
        -3.0, -3.0, 2.0, -2.0, 2.0, -2.0, 2.0, 2.0, 1.0, -1.0,
        +1.0, -2.0, -1.0, 1.0, -1.0, -1.0, 1.0, 1.0, 1.0, -1.0,
        -1.0, 1.0, 1.0, -1.0, 1.0, 1.0, -1.0, -1.0, -1.0, -1.0,
        -1.0, -1.0, -1.0, 1.0, -1.0, 1.0
    };

    /** coefficients for nutation in longitude, t part, in 0.1milliarcsec. */
    private static final double[] SLT = {
        -174.2, 0.2, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.6, -3.4,
        +1.2, -0.5, 0.1, 0.0, 0.0, -0.1, 0.0, 0.1, 0.0, 0.0,
        +0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
        -0.2, 0.1, -0.4, 0.0, 0.0, 0.0, 0.0, 0.1, -0.1, 0.0,
        +0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
        +0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
        +0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
        +0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
        +0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
        +0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
        +0.0, 0.0, 0.0, 0.0, 0.0, 0.0
    };

    /** coefficients for nutation in obliquity, const part, in 0.1milliarcsec. */
    private static final double[] CO = {
        +92025.0, -895.0, -24.0, 0.0, 1.0, 0.0, 1.0, 0.0, 5736.0, 54.0,
        +224.0, -95.0, -70.0, 1.0, 0.0, 0.0, 9.0, 7.0, 6.0, 3.0,
        +3.0, -2.0, -2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
        +977.0, -7.0, 200.0, 129.0, -1.0, -53.0, -2.0, -33.0, 32.0, 26.0,
        +27.0, 16.0, -1.0, -12.0, 13.0, -1.0, -10.0, -8.0, 7.0, 5.0,
        +0.0, -3.0, 3.0, 3.0, 0.0, -3.0, 3.0, 3.0, -3.0, 3.0,
        +0.0, 3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0,
        +1.0, 1.0, -1.0, 1.0, -1.0, 1.0, 0.0, -1.0, -1.0, 0.0,
        -1.0, 1.0, 0.0, -1.0, 1.0, 1.0, 0.0, 0.0, -1.0, 0.0,
        +0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
        +0.0, 0.0, 0.0, 0.0, 0.0, 0.0
    };

    /** coefficients for nutation in obliquity, t part, in 0.1milliarcsec. */
    private static final double[] COT = {
        +8.9, 0.5, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -3.1, -0.1,
        -0.6, 0.3, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
        +0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
        -0.5, 0.0, 0.0, -0.1, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
        +0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
        +0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
        +0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
        +0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
        +0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
        +0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
        +0.0, 0.0, 0.0, 0.0, 0.0, 0.0
    };

    /**
     * Start date for applying Moon corrections to the equation of the equinoxes.
     * This date corresponds to 1997-02-27T00:00:00 UTC, hence the 30s offset from TAI.
     */
    private static final AbsoluteDate NEW_EQE_MODEL_START =
        new AbsoluteDate(1997, 2, 27, 0, 0, 30, TimeScalesFactory.getTAI());

    /** True if EOP correction should be applied. */
    private final boolean applyEOPCorrection;

    /**
     * Simple constructor.
     * 
     * @param applyEOPCorr
     *        if true, EOP correction is applied (here, nutation)
     * @exception PatriusException
     *            if EOP parameters are desired but cannot be read
     */
    public TODProvider(final boolean applyEOPCorr) throws PatriusException {
        this.applyEOPCorrection = applyEOPCorr;
    }

    /**
     * Get the LoD (Length of Day) value.
     * <p>
     * The data provided comes from the IERS files. It is smoothed data.
     * </p>
     * 
     * @param date
     *        date at which the value is desired
     * @return LoD in seconds (0 if date is outside covered range)
     */
    public double getLOD(final AbsoluteDate date) {
        return FramesFactory.getConfiguration().getEOPHistory().getLOD(date);
    }

    /**
     * Get the pole IERS Reference Pole correction.
     * <p>
     * The data provided comes from the IERS files. It is smoothed data.
     * </p>
     * 
     * @param date
     *        date at which the correction is desired
     * @return pole correction ({@link PoleCorrection#NULL_CORRECTION
     *         PoleCorrection.NULL_CORRECTION} if date is outside covered range)
     * @throws TimeStampedCacheException
     *         if no eop data available
     */
    public PoleCorrection getPoleCorrection(final AbsoluteDate date) throws TimeStampedCacheException {
        return FramesFactory.getConfiguration().getEOPHistory().getPoleCorrection(date);
    }

    /**
     * Get the transform from Mean Of Date at specified date.
     * <p>
     * The update considers the nutation effects from IERS data.
     * </p>
     * <p>
     * Frames configuration nutation correction is used.
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
     * Get the transform from Mean Of Date at specified date.
     * <p>
     * The update considers the nutation effects from IERS data.
     * </p>
     * <p>
     * Spin derivative is never computed and is either 0 or null. No analytical formula is currently available for spin
     * derivative. Spin is also 0.
     * </p>
     * <p>
     * Frames configuration nutation correction is used.
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
     *            if the nutation model data embedded in the
     *            library cannot be read
     */
    @Override
    public Transform getTransform(final AbsoluteDate date, final FramesConfiguration config,
                                  final boolean computeSpinDerivatives) throws PatriusException {

        // Duration in seconds since J2000 epoch
        final double t = date.durationFromJ2000EpochInSeconds();

        // evaluate the nutation elements
        final double[] nutation = computeNutationElements(t);

        // compute the mean obliquity of the ecliptic
        final double moe = getMeanObliquityOfEcliptic(t);

        // get the IAU1980 corrections for the nutation parameters
        final NutationCorrection nutCorr = this.applyEOPCorrection ?
            config.getEOPHistory().getNutationCorrection(date) :
            NutationCorrection.NULL_CORRECTION;

        final double deps = nutation[1] + nutCorr.getDdeps();
        final double dpsi = nutation[0] + nutCorr.getDdpsi();

        // compute the true obliquity of the ecliptic
        final double toe = moe + deps;

        // set up the elementary rotations for nutation
        final Rotation r1 = new Rotation(Vector3D.PLUS_I, toe);
        final Rotation r2 = new Rotation(Vector3D.PLUS_K, dpsi);
        final Rotation r3 = new Rotation(Vector3D.PLUS_I, -moe);

        // complete nutation
        final Rotation precession = r1.applyTo(r2.applyTo(r3));

        // set up the transform from parent MOD
        final Vector3D acc = computeSpinDerivatives ? Vector3D.ZERO : null;
        return new Transform(date, precession.revert(), Vector3D.ZERO, acc);
    }

    /**
     * Get the transform from Mean Of Date at specified date.
     * <p>
     * The update considers the nutation effects from IERS data.
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
     * Get the transform from Mean Of Date at specified date.
     * <p>
     * The update considers the nutation effects from IERS data.
     * </p>
     * <p>
     * Spin derivative is never computed and is either 0 or null. No analytical formula is currently available for spin
     * derivative. Spin is also 0.
     * </p>
     * 
     * @param date
     *        new value of the date
     * @param computeSpinDerivatives
     *        unused param
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
     * Get the Equation of the Equinoxes at the current date.
     * 
     * @param date
     *        the date
     * @return equation of the equinoxes
     * @exception PatriusException
     *            if nutation model cannot be computed
     */
    public static double getEquationOfEquinoxes(final AbsoluteDate date) throws PatriusException {

        // Duration in seconds since J2000 epoch
        final double t = date.durationFromJ2000EpochInSeconds();

        // nutation in longitude
        final double dPsi = computeNutationElements(t)[0];

        // mean obliquity of ecliptic
        final double moe = getMeanObliquityOfEcliptic(t);

        // original definition of equation of equinoxes
        double eqe = dPsi * MathLib.cos(moe);

        if (date.compareTo(NEW_EQE_MODEL_START) >= 0) {

            // IAU 1994 resolution C7 added two terms to the equation of equinoxes
            // taking effect since 1997-02-27 for continuity

            // Mean longitude of the ascending node of the Moon
            final double tc = t / Constants.JULIAN_CENTURY;
            final double om = ((F53 * tc + F52) * tc + F510) * tc + F50 + ((F511 * tc) % 1.0) * MathUtils.TWO_PI;

            // add the two correction terms
            eqe += EQE_1 * MathLib.sin(om) + EQE_2 * MathLib.sin(om + om);

        }

        return eqe;

    }

    /**
     * Compute the mean obliquity of the ecliptic.
     * 
     * @param t
     *        offset from J2000 epoch in seconds
     * @return mean obliquity of ecliptic
     */
    private static double getMeanObliquityOfEcliptic(final double t) {

        // offset from J2000 epoch in julian centuries
        final double tc = t / Constants.JULIAN_CENTURY;

        // compute the mean obliquity of the ecliptic
        return ((MOE_3 * tc + MOE_2) * tc + MOE_1) * tc + MOE_0;

    }

    /**
     * Compute nutation elements.
     * <p>
     * This method applies the IAU-1980 theory and hence is rather slow. It is indirectly called by the
     * {@link #getInterpolatedNutationElements(double)} on a small number of reference points only.
     * </p>
     * 
     * @param t
     *        offset from J2000.0 epoch in seconds
     * @return computed nutation elements in a two elements array,
     *         with dPsi at index 0 and dEpsilon at index 1
     */
    private static double[] computeNutationElements(final double t) {

        // offset in julian centuries
        final double tc = t / Constants.JULIAN_CENTURY;
        // mean anomaly of the Moon
        final double l = ((F13 * tc + F12) * tc + F110) * tc + F10 + ((F111 * tc) % 1.0) * MathUtils.TWO_PI;
        // mean anomaly of the Sun
        final double lp = ((F23 * tc + F22) * tc + F210) * tc + F20 + ((F211 * tc) % 1.0) * MathUtils.TWO_PI;
        // L - &Omega; where L is the mean longitude of the Moon
        final double f = ((F33 * tc + F32) * tc + F310) * tc + F30 + ((F311 * tc) % 1.0) * MathUtils.TWO_PI;
        // mean elongation of the Moon from the Sun
        final double d = ((F43 * tc + F42) * tc + F410) * tc + F40 + ((F411 * tc) % 1.0) * MathUtils.TWO_PI;
        // mean longitude of the ascending node of the Moon
        final double om = ((F53 * tc + F52) * tc + F510) * tc + F50 + ((F511 * tc) % 1.0) * MathUtils.TWO_PI;

        // loop size
        final int n = CL.length;
        // Initialize nutation elements.
        double dpsi = 0.0;
        double deps = 0.0;

        // Sum the nutation terms from smallest to biggest.
        for (int j = n - 1; j >= 0; j--) {
            // Set up current argument.
            final double arg = CL[j] * l + CLP[j] * lp + CF[j] * f + CD[j] * d + COM[j] * om;

            // Accumulate current nutation term.
            final double s = SL[j] + SLT[j] * tc;
            final double c = CO[j] + COT[j] * tc;
            if (s != 0.0) {
                dpsi += s * MathLib.sin(arg);
            }
            if (c != 0.0) {
                deps += c * MathLib.cos(arg);
            }
        }

        // Convert results from 0.1 mas units to radians. */
        return new double[] {
            dpsi * Constants.ARC_SECONDS_TO_RADIANS * UNIT,
            deps * Constants.ARC_SECONDS_TO_RADIANS * UNIT
        };
    }
}
