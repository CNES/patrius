/**
 * Copyright 2021-2021 CNES
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
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11.1:FA:FA-72:30/06/2023:[PATRIUS] Mauvaise prise en compte du MeteoConditionProvider dans les
 * AbstractTropoFactory
 * VERSION:4.11:DM:DM-3295:22/05/2023:[PATRIUS] Conditions meteorologiques variables dans modeles troposphere
 * VERSION:4.11:FA:FA-3314:22/05/2023:[PATRIUS] Anomalie evaluation ForceModel SpacecraftState en ITRF
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.8:DM:DM-2929:15/11/2021:[PATRIUS] Harmonisation des modeles de troposphere 
 * VERSION:4.8:DM:DM-2928:15/11/2021:[PATRIUS] Modele tropospherique Marini-Murray 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.signalpropagation.troposphere;

import java.util.ArrayList;

import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.signalpropagation.MeteorologicalConditions;
import fr.cnes.sirius.patrius.signalpropagation.MeteorologicalConditionsProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;

/**
 *
 * This class provides the correction of laser
 * range tracking data for the effect of atmospheric refraction. This derivation, provided by J. W.
 * Marini and C. W. Murray Jr, applies to satellites with elevation above 10° and whose heights
 * exceed 70km. The reference model used in this class is derived from the NASA technical report
 * TM-X-70555 (available at the following link: https://ntrs.nasa.gov/search.jsp?R=19740007037).
 *
 * Position accuracies of better than a few centimeters are achievable with this model. It is worth
 * stressing once more that the desired accuracy with this formulation can be obtained just for
 * elevation angles above 10°. It is important to add that this model does not provide any type of
 * correction in the elevation. In other words, the Marini-Murray model is based on the assumption
 * that there are no differences between geometric and real satellite elevation.
 *
 * @author Natale N. - GMV
 * 
 * @since 4.8
 */
public class MariniMurrayModel implements TroposphericCorrection {

    /** Serializable UID. */
    private static final long serialVersionUID = 8986209540502944938L;

    /** m to km factor. */
    private static final double M_TO_KM = 1000;

    /** nm to microm factor. */
    private static final double NM_TO_MICROM = 1000;

    /** Pascal to millibar factor. */
    private static final double PASCAL_TO_MILLIBAR = 0.01;

    /** Absolute zero temperature. */
    private static final double ABSOLUTE_ZERO_T = -273.15;

    /** It is a function of the radiation wavelength lambda. */
    private final double fLambda;

    /** It is a function of the latitude phi and of the observation site altitude. */
    private final double fPhiH;

    /** Temporary computation using the latitude used */
    private final double cos2Phi;

    /** Provider for the meteorological conditions */
    private final MeteorologicalConditionsProvider meteoConditionsProvider;

    /**
     * Constructor for the class MariniMurrayModel.
     * 
     * @param meteoConditionsProvider
     *        provider for the meteorological conditions
     * @param phi
     *        Latitude of the laser site [Radians]
     * @param lambda
     *        Wavelength of radiation [nm]
     * @param alt
     *        Altitude of the site [Meters]
     *
     */
    // CHECKSTYLE: stop MagicNumber check
    // Reason: numerous empirical model parameters
    public MariniMurrayModel(final MeteorologicalConditionsProvider meteoConditionsProvider,
                             final double phi, final double lambda, final double alt) {

        this.cos2Phi = MathLib.cos(2 * phi);

        this.meteoConditionsProvider = meteoConditionsProvider;

        // Computation of fLambda
        final double lambdaMicrons = lambda / NM_TO_MICROM;
        this.fLambda = 0.9650 + 0.0164 / MathLib.pow(lambdaMicrons, 2) + 0.000228
                / MathLib.pow(lambdaMicrons, 4);

        // Computation of fPhiH
        final double altKM = alt / M_TO_KM;
        this.fPhiH = 1 - 0.0026 * this.cos2Phi - 0.00031 * altKM;
    }

    /** {@inheritDoc} */
    @Override
    public boolean supportsParameter(final Parameter param) {
        return false; // No supported parameter yet
    }

    /** {@inheritDoc} */
    @Override
    public ArrayList<Parameter> getParameters() {
        return new ArrayList<>(); // No supported parameter yet
    }

    /** {@inheritDoc} */
    @Override
    public double derivativeValue(final Parameter p, final double elevation) {
        return 0.; // No supported parameter yet
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDifferentiableBy(final Parameter p) {
        return supportsParameter(p); // No supported parameter yet
    }

    /** {@inheritDoc} */
    @Override
    public double computeSignalDelay(final AbsoluteDate date, final double elevation) {

        // Meteo conditions at date
        final MeteorologicalConditions meteoConditions = this.meteoConditionsProvider
            .getMeteorologicalConditions(date);

        final double temperature = meteoConditions.getTemperature();
        final double pressure = meteoConditions.getPressure();
        final double humidity = meteoConditions.getHumidity();
        // Measurements unit conversions
        final double p0Millibar = pressure * PASCAL_TO_MILLIBAR; // In the model the pressure at the
                                                                 // laser site is

        // Computation of the water vapor pressure at the laser site
        final double e0ExpNum = 7.5 * (temperature + ABSOLUTE_ZERO_T); // Numerator in the exponent
                                                                       // of the water vapor
        // pressure computation
        final double e0ExpDen = 237.3 + (temperature + ABSOLUTE_ZERO_T); // Denominator in the
                                                                         // exponent of the water

        // Computation of k: its derivation follows the evaluation of the integrals supporting the
        // Marini-Murray model. It is defined as a function of the latitude phi and of pressure and
        // temperature at the laser site
        final double k = 1.163 - 0.00968 * this.cos2Phi - 0.00104 * temperature + 0.00001435
                * p0Millibar;

        // vapor pressure computation
        final double e0 = humidity / 100.0 * 6.11 * MathLib.pow(10, e0ExpNum / e0ExpDen);

        // It is a function of the pressure and of the water vapor pressure at the laser site.
        // Computation of "a"
        final double a = 0.002357 * p0Millibar + 0.000141 * e0;

        // It is a support variable in the definition of the range correction. It is a function of k
        // and of pressure and temperature at the laser site.
        final double bCoeff = 2.0 * MathLib.pow(p0Millibar, 2) / (temperature * (3.0 - 1.0 / k));
        final double b = 1.084e-8 * p0Millibar * temperature * k + 4.734e-8 * bCoeff;

        // Evaluation of the first term of the range correction
        final double deltaR1 = this.fLambda / this.fPhiH;

        // Evaluation of the second part of the range correction
        final double sinElev = MathLib.sin(elevation);
        final double deltaR2Num = a + b;
        final double deltaR2Den = sinElev + b / (a + b) / (sinElev + 0.01);
        final double deltaR2 = deltaR2Num / deltaR2Den;

        // Computation of the range correction so as expressed by the Marini-Murray model
        final double deltaR = deltaR1 * deltaR2;
        // Computation of the signal delay coming from the range correction
        return deltaR / Constants.SPEED_OF_LIGHT;
    }

    // CHECKSTYLE: resume MagicNumber check
}
