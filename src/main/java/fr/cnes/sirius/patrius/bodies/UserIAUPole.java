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
* VERSION:4.8:FA:FA-2946:15/11/2021:[PATRIUS] Robustesse dans IAUPoleCoefficients1D 
* VERSION:4.8:FA:FA-2945:15/11/2021:[PATRIUS] Utilisation des degres dans des fonctions mathematiques 
 * VERSION:4.7:DM:DM-2703:18/05/2021: Acces facilite aux donnees d'environnement (application interplanetaire) 
 * VERSION:4.7:DM:DM-2684:18/05/2021:Création d'une classe UserIAUPole, à l'image de la classe UserCelestialBody
 * VERSION:4.7:DM:DM-2888:18/05/2021:ajout des derivee des angles alpha,delta,w &amp;#224; la classe UserIAUPole
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.differentiation.DerivativeStructure;
import fr.cnes.sirius.patrius.math.analysis.differentiation.UnivariateDifferentiableFunction;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;

/**
 * Generic IAU pole model for pole and prime meridian orientations.
 * <p>
 * This class represents the model compliant with the report of the IAU/IAG Working Group on Cartographic Coordinates
 * and Rotational Elements of the Planets and Satellites (WGCCRE). These definitions are common for all recent versions
 * of this report published every three years.
 * </p>
 * <p>
 * The precise values of pole direction and W angle coefficients may vary from publication year as models are adjusted.
 * The latest value of constants for implementing this interface can be found in the <a
 * href="http://astrogeology.usgs.gov/Projects/WGCCRE/">working group site</a>.
 * </p>
 * 
 * @author Emmanuel Bignon
 * 
 * @since 4.7
 */
public class UserIAUPole implements IAUPole {

    /** Serial UID. */
    private static final long serialVersionUID = 430502071561530600L;

    /** IAU reference date: January 1st 2000 at 12h TDB. */
    private static final AbsoluteDate REF_DATE = new AbsoluteDate(2000, 1, 1, 12, 0, 0, TimeScalesFactory.getTDB());

    /** Coefficients. */
    private final IAUPoleCoefficients coefficients;

    /**
     * Constructor.
     * @param coefficients model coefficients
     */
    public UserIAUPole(final IAUPoleCoefficients coefficients) {
        this.coefficients = coefficients;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getPole(final AbsoluteDate date) {
        return new Vector3D(getValue(coefficients.getAlpha0Coeffs(), date), getValue(coefficients.getDelta0Coeffs(),
                date));
    }

    /** {@inheritDoc} */
    @Override
    public double getPrimeMeridianAngle(final AbsoluteDate date) {
        return getValue(coefficients.getWCoeffs(), date);
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getPoleDerivative(final AbsoluteDate date) {
        // Alpha, delta
        final double alpha = getValue(coefficients.getAlpha0Coeffs(), date);
        final double delta = getValue(coefficients.getDelta0Coeffs(), date);
        final double alphap = getDerivative(coefficients.getAlpha0Coeffs(), date);
        final double deltap = getDerivative(coefficients.getDelta0Coeffs(), date);
        // Sin, cos of alpha, delta
        final double[] sincosAlpha = MathLib.sinAndCos(alpha);
        final double sinAlpha = sincosAlpha[0];
        final double cosAlpha = sincosAlpha[1];
        final double[] sincosDelta = MathLib.sinAndCos(delta);
        final double sinDelta = sincosDelta[0];
        final double cosDelta = sincosDelta[1];
        // Build derivative vector
        return new Vector3D(-alphap * sinAlpha * cosDelta - deltap * cosAlpha * sinDelta, alphap * cosAlpha * cosDelta
                - deltap * sinAlpha * sinDelta, deltap * cosDelta);
    }

    /** {@inheritDoc} */
    @Override
    public double getPrimeMeridianAngleDerivative(final AbsoluteDate date) {
        return getDerivative(coefficients.getWCoeffs(), date);
    }

    /**
     * Compute the model value given a set of coefficients according to IAU note.
     * @param coeffs coefficients
     * @param date a date
     * @return the model value given a set of coefficients
     */
    private double getValue(final IAUPoleCoefficients1D coeffs,
            final AbsoluteDate date) {
        // Durations
        final double t = centuries(date);
        final double d = days(date);

        double value = 0;

        // Call functions dependent on duration in days
        if (coeffs.getFunctionsInDays() != null) {
            for (final UnivariateFunction f : coeffs.getFunctionsInDays()) {
                value += f.value(d);
            }
        }

        // Call functions dependent on duration in centuries
        if (coeffs.getFunctionsInCenturies() != null) {
            for (final UnivariateFunction f : coeffs.getFunctionsInCenturies()) {
                value += f.value(t);
            }
        }

        // Return value
        return value;
    }

    /**
     * Compute the model value given a set of coefficients according to IAU note.
     * @param coeffs coefficients
     * @param date a date
     * @return the model value given a set of coefficients
     */
    private double getDerivative(final IAUPoleCoefficients1D coeffs,
            final AbsoluteDate date) {
        // Durations
        final double t = centuries(date);
        final double d = days(date);
        final DerivativeStructure tDS = new DerivativeStructure(1, 1, t, 1);
        final DerivativeStructure dDS = new DerivativeStructure(1, 1, d, 1);

        DerivativeStructure value = new DerivativeStructure(1, 1);

        // Call functions dependent on duration in days
        if (coeffs.getFunctionsInDays() != null) {
            for (final UnivariateDifferentiableFunction f : coeffs.getFunctionsInDays()) {
                value = value.add(f.value(dDS).multiply(1. / Constants.JULIAN_DAY));
            }
        }

        // Call functions dependent on duration in centuries
        if (coeffs.getFunctionsInCenturies() != null) {
            for (final UnivariateDifferentiableFunction f : coeffs.getFunctionsInCenturies()) {
                value = value.add(f.value(tDS).multiply(1. / Constants.JULIAN_CENTURY));
            }
        }

        // Return value
        return value.getPartialDerivative(1);
    }

    /**
     * Compute the interval in julian centuries from standard epoch TDB.
     * 
     * @param date
     *        date
     * @return interval between date and standard epoch in julian centuries
     */
    private double centuries(final AbsoluteDate date) {
        return date.durationFrom(REF_DATE, TimeScalesFactory.getTDB()) / Constants.JULIAN_CENTURY;
    }

    /**
     * Compute the interval in julian days from standard epoch TDB.
     * 
     * @param date
     *        date
     * @return interval between date and standard epoch in julian days
     */
    private double days(final AbsoluteDate date) {
        return date.durationFrom(REF_DATE, TimeScalesFactory.getTDB()) / Constants.JULIAN_DAY;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "User-defined coefficients";
    }
}
