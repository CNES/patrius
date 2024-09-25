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
 * VERSION:4.13:DM:DM-5:08/12/2023:[PATRIUS] Orientation d'un corps celeste sous forme de quaternions
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11:DM:DM-3311:22/05/2023:[PATRIUS] Evolutions mineures sur CelestialBody, shape et reperes
 * VERSION:4.11:FA:FA-3314:22/05/2023:[PATRIUS] Anomalie evaluation ForceModel SpacecraftState en ITRF
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

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.analysis.differentiation.DerivativeStructure;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
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
public class UserIAUPole implements CelestialBodyIAUOrientation {

    /** Serializable UID. */
    private static final long serialVersionUID = 430502071561530600L;

    /** IAU reference date: January 1st 2000 at 12h TDB. */
    private static final AbsoluteDate REF_DATE = new AbsoluteDate(2000, 1, 1, 12, 0, 0, TimeScalesFactory.getTDB());

    /** Tolerance for inertial frame rotation rate computation. */
    private static final double TOL = 1E-9;

    /** Coefficients. */
    private final IAUPoleCoefficients coefficients;

    /**
     * Constructor.
     * 
     * @param coefficients
     *        model coefficients
     */
    public UserIAUPole(final IAUPoleCoefficients coefficients) {
        this.coefficients = coefficients;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getPole(final AbsoluteDate date) {
        // Takes into account constant, secular and harmonics effects
        return getPole(date, IAUPoleModelType.TRUE);
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getPole(final AbsoluteDate date,
                            final IAUPoleModelType iauPoleType) {
        return new Vector3D(getValue(this.coefficients.getAlpha0Coeffs(), date, iauPoleType), getValue(
            this.coefficients.getDelta0Coeffs(), date, iauPoleType));
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getPoleDerivative(final AbsoluteDate date) {
        // Takes into account constant, secular and harmonics effects
        return getPoleDerivative(date, IAUPoleModelType.TRUE);
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getPoleDerivative(final AbsoluteDate date,
                                      final IAUPoleModelType iauPoleType) {
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
    public double getPrimeMeridianAngle(final AbsoluteDate date) {
        // Takes into account constant, secular and harmonics effects
        return getPrimeMeridianAngle(date, IAUPoleModelType.TRUE);
    }

    /** {@inheritDoc} */
    @Override
    public double getPrimeMeridianAngle(final AbsoluteDate date,
                                        final IAUPoleModelType iauPoleType) {
        return getValue(this.coefficients.getWCoeffs(), date, iauPoleType);
    }

    /** {@inheritDoc} */
    @Override
    public double getPrimeMeridianAngleDerivative(final AbsoluteDate date) {
        // Takes into account constant, secular and harmonics effects
        return getPrimeMeridianAngleDerivative(date, IAUPoleModelType.TRUE);
    }

    /** {@inheritDoc} */
    @Override
    public double getPrimeMeridianAngleDerivative(final AbsoluteDate date,
                                                  final IAUPoleModelType iauPoleType) {
        return getDerivative(this.coefficients.getWCoeffs(), date, iauPoleType);
    }

    /** {@inheritDoc} */
    @Override
    public AngularCoordinates getAngularCoordinates(final AbsoluteDate date, final OrientationType orientationType)
        throws PatriusException {
        return getAngularCoordinates(date, orientationType, IAUPoleModelType.TRUE);
    }

    /** {@inheritDoc} */
    @Override
    public AngularCoordinates getAngularCoordinates(final AbsoluteDate date, final OrientationType orientationType,
                                                    final IAUPoleModelType iauPoleType) throws PatriusException {

        final AngularCoordinates coord;
        switch (orientationType) {
            case ICRF_TO_ROTATING:
                // Compute the orientation from the ICRF frame to the rotating frame
                // Frames composition: ICRF_TO_ROTATING = ICRF_TO_INERTIAL + INERTIAL_TO_ROTATING
                final AngularCoordinates angCoord1 = getAngularCoordinates(date, OrientationType.ICRF_TO_INERTIAL,
                    iauPoleType);
                final AngularCoordinates angCoord2 = getAngularCoordinates(date, OrientationType.INERTIAL_TO_ROTATING,
                    iauPoleType);
                final Frame icrf = FramesFactory.getICRF();
                final Frame f1 = new Frame(icrf, new Transform(date, angCoord1), "f1");
                final Frame f2 = new Frame(f1, new Transform(date, angCoord2), "f2");
                final Transform t = icrf.getTransformTo(f2, date);
                coord = t.getAngular();
                break;

            case ICRF_TO_INERTIAL:
                // Compute the orientation from the ICRF frame to the inertial frame

                // Compute rotation from EME2000 frame to self, as per the
                // "Report of the IAU/IAG Working Group on Cartographic Coordinates and Rotational
                // Elements of the Planets and Satellites".
                // These definitions are common for all recent versions of this report published every three years, the
                // precise values of pole direction and W angle coefficients may vary from publication year as models
                // are adjusted. These coefficients are not in this class, they are in the specialized classes that do
                // implement the getPole and getPrimeMeridianAngle methods
                final Vector3D pole = this.getPole(date, iauPoleType);
                Vector3D qNode = Vector3D.crossProduct(Vector3D.PLUS_K, pole);
                if (qNode.getNormSq() < Precision.SAFE_MIN) {
                    qNode = Vector3D.PLUS_I;
                }

                if (pole.equals(Vector3D.PLUS_K)) {
                    // Specific case: pole is along +k, rotation is identity
                    coord = new AngularCoordinates(Rotation.IDENTITY, Vector3D.ZERO);
                } else {
                    AngularCoordinates tempCoor;
                    try {
                        final Vector3D poleDerivative = this.getPoleDerivative(date, iauPoleType);
                        tempCoor = new AngularCoordinates(new PVCoordinates(Vector3D.PLUS_K,
                            Vector3D.ZERO), new PVCoordinates(Vector3D.PLUS_I, Vector3D.ZERO), new PVCoordinates(pole,
                            poleDerivative), new PVCoordinates(qNode, Vector3D.crossProduct(Vector3D.PLUS_K,
                            poleDerivative)), TOL).revert();
                    } catch (final PatriusException e) {
                        // Spin cannot be computed (inconsistent pole derivative)
                        tempCoor = new AngularCoordinates(new Rotation(Vector3D.PLUS_K, Vector3D.PLUS_I, pole, qNode),
                            Vector3D.ZERO);
                    }
                    coord = tempCoor;
                }
                break;

            case INERTIAL_TO_ROTATING:
                // Compute the orientation from the inertial frame to the rotating frame
                final double w = this.getPrimeMeridianAngle(date, iauPoleType);
                final double wdot = this.getPrimeMeridianAngleDerivative(date, iauPoleType);
                coord = new AngularCoordinates(new Rotation(Vector3D.PLUS_K, w), new Vector3D(wdot, Vector3D.PLUS_K));
                break;

            default:
                // Shouldn't happened (internal error)
                throw new EnumConstantNotPresentException(OrientationType.class, orientationType.toString());
        }

        return coord;
    }

    /**
     * Compute the model value given a set of coefficients according to IAU note.
     * 
     * @param coeffs
     *        coefficients
     * @param date
     *        a date
     * @param iauPoleType
     *        IAUPole data to take into account for transformation
     * @return the model value given a set of coefficients
     */
    private static double getValue(final IAUPoleCoefficients1D coeffs, final AbsoluteDate date,
                                   final IAUPoleModelType iauPoleType) {
        // Durations
        final double t = centuries(date);
        final double d = days(date);

        // Initialization
        double value = 0;

        // Computation using only the matching functions
        if (coeffs.getFunctions() != null) {
            for (final IAUPoleFunction f : coeffs.getFunctions()) {
                if (iauPoleType.accept(f.getType())) {
                    switch (f.getTimeDependency()) {
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
     * 
     * @param coeffs
     *        coefficients
     * @param date
     *        a date
     * @param iauPoleType
     *        IAUPole data to take into account for transformation
     * @return the model value given a set of coefficients
     */
    private static double getDerivative(final IAUPoleCoefficients1D coeffs, final AbsoluteDate date,
                                        final IAUPoleModelType iauPoleType) {
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
                if (iauPoleType.accept(f.getType())) {
                    switch (f.getTimeDependency()) {
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
