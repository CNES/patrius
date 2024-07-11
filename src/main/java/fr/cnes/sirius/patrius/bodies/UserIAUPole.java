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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:FA:FA-2946:15/11/2021:[PATRIUS] Robustesse dans IAUPoleCoefficients1D 
 * VERSION:4.8:FA:FA-2945:15/11/2021:[PATRIUS] Utilisation des degres dans des fonctions mathematiques 
 * VERSION:4.7:DM:DM-2703:18/05/2021: Acces facilite aux donnees d'environnement (application interplanetaire) 
 * VERSION:4.7:DM:DM-2684:18/05/2021:Création d'une classe UserIAUPole, à l'image de la classe UserCelestialBody
 * VERSION:4.7:DM:DM-2888:18/05/2021:ajout des derivee des angles alpha,delta,w &amp;#224; la classe UserIAUPole
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import fr.cnes.sirius.patrius.math.analysis.differentiation.DerivativeStructure;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;

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

     /** Serializable UID. */
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
        // Takes into account constant, secular and harmonics effects
        return getPole(date, GlobalIAUPoleType.CONSTANT_SECULAR_HARMONICS);
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getPole(final AbsoluteDate date,
            final GlobalIAUPoleType iauPoleType) {
        return new Vector3D(getValue(this.coefficients.getAlpha0Coeffs(), date, iauPoleType), getValue(
            this.coefficients.getDelta0Coeffs(), date, iauPoleType));
    }

    /** {@inheritDoc} */
    @Override
    public double getPrimeMeridianAngle(final AbsoluteDate date) {
        // Takes into account constant, secular and harmonics effects
        return getPrimeMeridianAngle(date, GlobalIAUPoleType.CONSTANT_SECULAR_HARMONICS);
    }

    /** {@inheritDoc} */
    @Override
    public double getPrimeMeridianAngle(final AbsoluteDate date,
            final GlobalIAUPoleType iauPoleType) {
        return getValue(this.coefficients.getWCoeffs(), date, iauPoleType);
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getPoleDerivative(final AbsoluteDate date) {
        // Takes into account constant, secular and harmonics effects
        return getPoleDerivative(date, GlobalIAUPoleType.CONSTANT_SECULAR_HARMONICS);
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getPoleDerivative(final AbsoluteDate date,
            final GlobalIAUPoleType iauPoleType) {
        // Alpha, delta
        final double alpha = getValue(this.coefficients.getAlpha0Coeffs(), date, iauPoleType);
        final double delta = getValue(this.coefficients.getDelta0Coeffs(), date, iauPoleType);
        final double alphap = getDerivative(this.coefficients.getAlpha0Coeffs(), date, iauPoleType);
        final double deltap = getDerivative(this.coefficients.getDelta0Coeffs(), date, iauPoleType);
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
        // Takes into account constant, secular and harmonics effects
        return getPrimeMeridianAngleDerivative(date, GlobalIAUPoleType.CONSTANT_SECULAR_HARMONICS);
    }

    /** {@inheritDoc} */
    @Override
    public double getPrimeMeridianAngleDerivative(final AbsoluteDate date,
            final GlobalIAUPoleType iauPoleType) {
        return getDerivative(this.coefficients.getWCoeffs(), date, iauPoleType);
    }

    /**
     * Compute the model value given a set of coefficients according to IAU note.
     * @param coeffs coefficients
     * @param date a date
     * @param iauPoleType
     *        IAUPole data to take into account for transformation
     * @return the model value given a set of coefficients
     */
    private static double getValue(final IAUPoleCoefficients1D coeffs, final AbsoluteDate date,
                                   final GlobalIAUPoleType iauPoleType) {
        // Durations
        final double t = centuries(date);
        final double d = days(date);

        // Initialization
        double value = 0;

        // Computation using only the matching functions
        if (coeffs.getFunctions() != null) {
            for (final IAUPoleFunction f : coeffs.getFunctions()) {
                if (iauPoleType.accept(f.getIAUPoleType())) {
                    switch (f.getIAUTimeDependency()) {
                        case DAYS:
                            value += f.getFunction().value(d);
                            break;
                        case CENTURIES:
                            value += f.getFunction().value(t);
                            break;
                        default:
                            // Cannot happen
                            throw new PatriusRuntimeException(PatriusMessages.INTERNAL_ERROR, null);
                    }
                }
            }
        }

        // Return value
        return value;
    }

    /**
     * Compute the model value given a set of coefficients according to IAU note.
     * @param coeffs coefficients
     * @param date a date
     * @param iauPoleType
     *        IAUPole data to take into account for transformation
     * @return the model value given a set of coefficients
     */
    private static double getDerivative(final IAUPoleCoefficients1D coeffs, final AbsoluteDate date,
                                        final GlobalIAUPoleType iauPoleType) {
        // Durations
        final double t = centuries(date);
        final double d = days(date);

        // Initialization
        final DerivativeStructure dDS = new DerivativeStructure(1, 1, d, 1);
        final DerivativeStructure tDS = new DerivativeStructure(1, 1, t, 1);
        DerivativeStructure value = new DerivativeStructure(1, 1);

        // Computation using only the matching functions
        if (coeffs.getFunctions() != null) {
            for (final IAUPoleFunction f : coeffs.getFunctions()) {
                if (iauPoleType.accept(f.getIAUPoleType())) {
                    switch (f.getIAUTimeDependency()) {
                        case DAYS:
                            value = value.add(f.getFunction().value(dDS).multiply(1. / Constants.JULIAN_DAY));
                            break;
                        case CENTURIES:
                            value = value.add(f.getFunction().value(tDS).multiply(1. / Constants.JULIAN_CENTURY));
                            break;
                        default:
                            // Cannot happen
                            throw new PatriusRuntimeException(PatriusMessages.INTERNAL_ERROR, null);
                    }
                }
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
    private static double centuries(final AbsoluteDate date) {
        return date.durationFrom(REF_DATE, TimeScalesFactory.getTDB()) / Constants.JULIAN_CENTURY;
    }

    /**
     * Compute the interval in julian days from standard epoch TDB.
     * 
     * @param date
     *        date
     * @return interval between date and standard epoch in julian days
     */
    private static double days(final AbsoluteDate date) {
        return date.durationFrom(REF_DATE, TimeScalesFactory.getTDB()) / Constants.JULIAN_DAY;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "User-defined coefficients";
    }
}
