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
 * @history created 19/04/12
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2445:27/05/2020:optimisation de SolarActivityReader 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:93:08/08/2013:completed Javadoc concerning the partial derivatives parameters
 * VERSION::DM:90:15/10/2013:Using normalized gravitational attraction.
 * VERSION::DM:241:01/10/2014:created AbstractTides class
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::FA:273:20/10/2013:Minor code problems
 * VERSION::FA:345:30/10/2014:modified comments ratio
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity.tides;

import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class implements the perturbating force due to terrestrial tides (deformation due to third body attraction on an
 * aneslatic crust, ellipticity correction, frequency correction). It is possible to activate/deactivate one of these
 * corrections. At least the model take into account the deformation due to the moon and the sun attraction up to degree
 * 2.
 * <p>
 * The implementation of this class enables the computation of partial derivatives by finite differences with respect to
 * the <b>central attraction coefficient</b>.
 * </p>
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment not thread safe because of the method updateCoefficientsCandS().
 * 
 * @author Julie Anton, Gerald Mercadier
 * 
 * @version $Id: TerrestrialTides.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.1
 */
public class TerrestrialTides extends AbstractTides {
    /** Serializable UID. */
    private static final long serialVersionUID = 302414702448996411L;

    /** Constant 0.2. */
    private static final double ZERO_POINT_TWO = 0.2;
    /** Constant 0.5. */
    private static final double ZERO_POINT_FIVE = 0.5;
    /** Constant 1.5. */
    private static final double ONE_POINT_FIVE = 1.5;
    /** Constant 2.5. */
    private static final double TWO_POINT_FIVE = 2.5;
    /** Constant 7.5. */
    private static final double SEVEN_POINT_FIVE = 7.5;
    /** Constant 15. */
    private static final double FIFTEEN = 15.;
    /** Constant 360. */
    private static final double THREEHUNDREDSIXTY = 360.;
    /** Constant 12. */
    private static final double TWELVE = 12.;
    /** Constant sqrt(7). */
    private static final double SQRT7 = MathLib.sqrt(7);
    /** Constant sqrt(7/6). */
    private static final double SQRT7OVER6 = MathLib.sqrt(7. / 6.);
    /** Constant sqrt(7/60). */
    private static final double SQRT7OVER60 = MathLib.sqrt(7. / 60.);
    /** Constant sqrt(7/360). */
    private static final double SQRT7OVER360 = MathLib.sqrt(7. / THREEHUNDREDSIXTY);
    /** Constant sqrt(5). */
    private static final double SQRT5 = MathLib.sqrt(5);
    /** Constant sqrt(5/3). */
    private static final double SQRT5OVER3 = MathLib.sqrt(5. / 3.);
    /** Constant sqrt(5/12). */
    private static final double SQRT5OVER12 = MathLib.sqrt(5. / TWELVE);

    /** Perturbating bodies. */
    private final CelestialBody[] bodiesP;

    /** Deformation due to third body attraction (degree3). */
    private final boolean thirdBodyCorrectionUpToDegree3;

    /** Ellipticity correction. */
    private final boolean ellipticityCorrectionFlag;

    /** Frequency correction. */
    private final boolean frequencyCorrectionFlag;

    /** Terrestrial tide data provider. */
    private final ITerrestrialTidesDataProvider standard;

    /** True if acceleration partial derivatives with respect to position have to be computed. */
    private final boolean computePartialDerivativesWrtPosition;

    /**
     * Creates a new instance. It is possible to consider several perturbing bodies. The correction due to the tidal
     * potential is taken into account up to degree 2 and 3 if it is specified by the user. It is also possible to
     * activate the frequency correction and the ellipticity correction.
     * 
     * @param centralBodyFrame
     *        rotating body frame
     * @param equatorialRadius
     *        reference equatorial radius of the potential
     * @param mu
     *        central body attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @param bodies
     *        perturbing bodies
     * @param thirdBodyAttDegree3
     *        if true the perturbation of tidal potential are taken into account up to degree 3
     * @param frequencyCorr
     *        if true the frequency correction is applied
     * @param ellipticityCorr
     *        if true the ellipticity correction is applied
     * @param terrestrialData
     *        terrestrial tide data
     * @exception IllegalArgumentException
     *            if coefficients array do not match
     * @throws PatriusException
     *         if a perturbing celestial body cannot be built
     */
    public TerrestrialTides(final Frame centralBodyFrame, final double equatorialRadius, final double mu,
        final List<CelestialBody> bodies,
        final boolean thirdBodyAttDegree3, final boolean frequencyCorr, final boolean ellipticityCorr,
        final ITerrestrialTidesDataProvider terrestrialData) throws PatriusException {
        this(centralBodyFrame, equatorialRadius, mu, bodies, thirdBodyAttDegree3, frequencyCorr, ellipticityCorr,
            terrestrialData, true);
    }

    /**
     * Creates a new instance. It is possible to consider several perturbing bodies. The correction due to the tidal
     * potential is taken into account up to degree 2 and 3 if it is specified by the user. It is also possible to
     * activate the frequency correction and the ellipticity correction.
     * 
     * @param centralBodyFrame
     *        rotating body frame
     * @param equatorialRadius
     *        reference equatorial radius of the potential
     * @param mu
     *        central body attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @param bodies
     *        perturbing bodies
     * @param thirdBodyAttDegree3
     *        if true the perturbation of tidal potential are taken into account up to degree 3
     * @param frequencyCorr
     *        if true the frequency correction is applied
     * @param ellipticityCorr
     *        if true the ellipticity correction is applied
     * @param terrestrialData
     *        terrestrial tide data
     * @param computePD
     *        if partial derivatives wrt position have to be computed
     * @exception IllegalArgumentException
     *            if coefficients array do not match
     * @throws PatriusException
     *         if a perturbing celestial body cannot be built
     */
    public TerrestrialTides(final Frame centralBodyFrame, final double equatorialRadius, final double mu,
        final List<CelestialBody> bodies,
        final boolean thirdBodyAttDegree3, final boolean frequencyCorr, final boolean ellipticityCorr,
        final ITerrestrialTidesDataProvider terrestrialData, final boolean computePD) throws PatriusException {
        super(centralBodyFrame, equatorialRadius, mu, 5, 4, computePD ? 5 : 0, computePD ? 4 : 0);
        // all of the corrections are taken into account
        this.thirdBodyCorrectionUpToDegree3 = thirdBodyAttDegree3;
        this.ellipticityCorrectionFlag = ellipticityCorr;
        this.frequencyCorrectionFlag = frequencyCorr;
        this.standard = terrestrialData;
        // perturbing bodies
        this.bodiesP = new CelestialBody[bodies.size()];
        for (int i = 0; i < bodies.size(); i++) {
            this.bodiesP[i] = bodies.get(i);
        }
        this.computePartialDerivativesWrtPosition = computePD;
    }

    /**
     * Creates a new instance using {@link Parameter}. It is possible to consider several perturbing bodies. The
     * correction due to the tidal potential is taken into account up to degree 2 and 3 if it is specified by the user.
     * It is also possible to activate the frequency correction and the ellipticity correction.
     * 
     * @param centralBodyFrame
     *        rotating body frame
     * @param equatorialRadius
     *        reference equatorial radius of the potential parameter
     * @param mu
     *        central body attraction coefficient (m<sup>3</sup>/s<sup>2</sup>) parameter
     * @param bodies
     *        perturbing bodies
     * @param thirdBodyAttDegree3
     *        if true the perturbation of tidal potential are taken into account up to degree 3
     * @param frequencyCorr
     *        if true the frequency correction is applied
     * @param ellipticityCorr
     *        if true the ellipticity correction is applied
     * @param terrestrialData
     *        terrestrial tide data
     * @exception IllegalArgumentException
     *            if coefficients array do not match
     * @throws PatriusException
     *         if a perturbing celestial body cannot be built
     */
    public TerrestrialTides(final Frame centralBodyFrame, final Parameter equatorialRadius, final Parameter mu,
        final List<CelestialBody> bodies,
        final boolean thirdBodyAttDegree3, final boolean frequencyCorr, final boolean ellipticityCorr,
        final ITerrestrialTidesDataProvider terrestrialData) throws PatriusException {
        this(centralBodyFrame, equatorialRadius, mu, bodies, thirdBodyAttDegree3, frequencyCorr, ellipticityCorr,
            terrestrialData, true);
    }

    /**
     * Creates a new instance using {@link Parameter}. It is possible to consider several perturbing bodies. The
     * correction due to the tidal potential is taken into account up to degree 2 and 3 if it is specified by the user.
     * It is also possible to activate the frequency correction and the ellipticity correction.
     * 
     * @param centralBodyFrame
     *        rotating body frame
     * @param equatorialRadius
     *        reference equatorial radius of the potential as a parameter
     * @param mu
     *        central body attraction coefficient (m<sup>3</sup>/s<sup>2</sup>) as a parameter
     * @param bodies
     *        perturbing bodies
     * @param thirdBodyAttDegree3
     *        if true the perturbation of tidal potential are taken into account up to degree 3
     * @param frequencyCorr
     *        if true the frequency correction is applied
     * @param ellipticityCorr
     *        if true the ellipticity correction is applied
     * @param terrestrialData
     *        terrestrial tide data
     * @param computePD
     *        true if partial derivatives wrt position have to be computed
     * @exception IllegalArgumentException
     *            if coefficients array do not match
     * @throws PatriusException
     *         if a perturbing celestial body cannot be built
     */
    public TerrestrialTides(final Frame centralBodyFrame, final Parameter equatorialRadius, final Parameter mu,
        final List<CelestialBody> bodies,
        final boolean thirdBodyAttDegree3, final boolean frequencyCorr, final boolean ellipticityCorr,
        final ITerrestrialTidesDataProvider terrestrialData, final boolean computePD) throws PatriusException {
        super(centralBodyFrame, equatorialRadius, mu, 5, 4, computePD ? 5 : 0, computePD ? 4 : 0);
        // all of the corrections are taken into account
        this.thirdBodyCorrectionUpToDegree3 = thirdBodyAttDegree3;
        this.ellipticityCorrectionFlag = ellipticityCorr;
        this.frequencyCorrectionFlag = frequencyCorr;
        this.standard = terrestrialData;
        // perturbing bodies
        this.bodiesP = new CelestialBody[bodies.size()];
        for (int i = 0; i < bodies.size(); i++) {
            this.bodiesP[i] = bodies.get(i);
        }
        this.computePartialDerivativesWrtPosition = computePD;
    }

    /**
     * Creates a new instance. The perturbating bodies are the moon and the sun. This constructor takes into account all
     * of the corrections.
     * 
     * @param centralBodyFrame
     *        rotating body frame
     * @param equatorialRadius
     *        reference equatorial radius of the potential
     * @param mu
     *        central body attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @exception IllegalArgumentException
     *            if coefficients array do not match
     * @throws PatriusException
     *         if a perturbing celestial body cannot be built
     */
    public TerrestrialTides(final Frame centralBodyFrame, final double equatorialRadius,
        final double mu) throws PatriusException {
        this(centralBodyFrame, equatorialRadius, mu, true);
    }

    /**
     * Creates a new instance. The perturbating bodies are the moon and the sun. This constructor takes into account all
     * of the corrections.
     * 
     * @param centralBodyFrame
     *        rotating body frame
     * @param equatorialRadius
     *        reference equatorial radius of the potential
     * @param mu
     *        central body attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @param computePD
     *        true if partial derivatives wrt position have to be computed
     * @exception IllegalArgumentException
     *            if coefficients array do not match
     * @throws PatriusException
     *         if a perturbing celestial body cannot be built
     */
    public TerrestrialTides(final Frame centralBodyFrame, final double equatorialRadius,
        final double mu, final boolean computePD) throws PatriusException {
        this(centralBodyFrame, equatorialRadius, mu, getLuniSolarPerturbation(), true, true, true,
            new TerrestrialTidesDataProvider(), computePD);
    }

    /**
     * Creates a new instance. The perturbating bodies are the moon and the sun. This constructor takes into account all
     * of the corrections.
     * 
     * @param centralBodyFrame
     *        rotating body frame
     * @param equatorialRadius
     *        reference equatorial radius of the potential parameter
     * @param mu
     *        central body attraction coefficient (m<sup>3</sup>/s<sup>2</sup>) parameter
     * @exception IllegalArgumentException
     *            if coefficients array do not match
     * @throws PatriusException
     *         if a perturbing celestial body cannot be built
     */
    public TerrestrialTides(final Frame centralBodyFrame, final Parameter equatorialRadius,
        final Parameter mu) throws PatriusException {
        this(centralBodyFrame, equatorialRadius, mu, true);
    }

    /**
     * Creates a new instance. The perturbating bodies are the moon and the sun. This constructor takes into account all
     * of the corrections.
     * 
     * @param centralBodyFrame
     *        rotating body frame
     * @param equatorialRadius
     *        reference equatorial radius of the potential parameter
     * @param mu
     *        central body attraction coefficient (m<sup>3</sup>/s<sup>2</sup>) parameter
     * @param computePD
     *        true if partial derivatives have to be computed
     * @exception IllegalArgumentException
     *            if coefficients array do not match
     * @throws PatriusException
     *         if a perturbing celestial body cannot be built
     */
    public TerrestrialTides(final Frame centralBodyFrame, final Parameter equatorialRadius,
        final Parameter mu, final boolean computePD) throws PatriusException {
        this(centralBodyFrame, equatorialRadius, mu, getLuniSolarPerturbation(), true, true, true,
            new TerrestrialTidesDataProvider(), computePD);
    }

    /**
     * The perturbing bodies are the moon and the sun.
     * 
     * @return list of CelestialBody which contains the moon and the sun.
     * @throws PatriusException
     *         if the celestial body cannot be built
     */
    private static List<CelestialBody> getLuniSolarPerturbation() throws PatriusException {
        final List<CelestialBody> bodies = new ArrayList<>();
        bodies.add(CelestialBodyFactory.getSun());

        bodies.add(CelestialBodyFactory.getMoon());
        return bodies;
    }

    /** {@inheritDoc} */
    @Override
    public void updateCoefficientsCandS(final AbsoluteDate date) throws PatriusException {

        final double[] coeff = this.thirdBodyAttractionDegree2(date);
        this.thirdBodyAttractionCorrectionDegree2(coeff);
        if (this.ellipticityCorrectionFlag) {
            this.ellipticityCorrection(coeff);
        }
        if (this.thirdBodyCorrectionUpToDegree3) {
            this.thirdBodyAttractionCorrectionDegree3(date);
        }
        if (this.frequencyCorrectionFlag) {
            this.frequencyCorrection(date);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void updateCoefficientsCandSPD(final AbsoluteDate date) throws PatriusException {

        // Update coefficients C and S first
        this.updateCoefficientsCandS(date);

        // Copy results
        this.coefficientsCPD = this.coefficientsC;
        this.coefficientsSPD = this.coefficientsS;
    }

    /**
     * Correction due to third body attraction up to degree 2.
     * 
     * @param coefficients
     *        : coefficients of the tidal potential (up to degree 2)
     * @throws PatriusException
     *         if position cannot be computed in given frame
     */
    private void thirdBodyAttractionCorrectionDegree2(final double[] coefficients) throws PatriusException {

        final double[] wk2 = this.standard.getAnelasticityCorrectionLoveNumber2();

        this.coefficientsC[2][0] = wk2[0] * coefficients[0];

        this.coefficientsC[2][1] = wk2[2] * coefficients[1] + wk2[3] * coefficients[2];
        this.coefficientsS[2][1] = wk2[2] * coefficients[2] - wk2[3] * coefficients[1];

        this.coefficientsC[2][2] = wk2[4] * coefficients[3] + wk2[5] * coefficients[4];
        this.coefficientsS[2][2] = wk2[4] * coefficients[4] - wk2[5] * coefficients[3];
    }

    /**
     * Correction due to third body attraction up to degree 3.
     * 
     * @param date
     *        : current date
     * @throws PatriusException
     *         if position cannot be computed in given frame
     */
    private void thirdBodyAttractionCorrectionDegree3(final AbsoluteDate date) throws PatriusException {

        // perturbing body position
        final double[] sinml = new double[5];
        final double[] cosml = new double[5];

        // coefficients
        double s32 = 0.;
        double c33 = 0.;
        double s33 = 0.;
        double c30 = 0.;
        double c31 = 0.;
        double s31 = 0.;
        double c32 = 0.;

        // loop over the perturbing bodies
        for (final CelestialBody bodyP : this.bodiesP) {

            // body position
            final Vector3D position = bodyP.getPVCoordinates(date, this.bodyFrame).getPosition();
            final double posx = position.getX();
            final double posy = position.getY();
            final double posz = position.getZ();
            // Temporary variables
            final double rpxy2 = posx * posx + posy * posy;
            final double rpxy = MathLib.sqrt(rpxy2);
            final double rp2 = rpxy2 + posz * posz;
            final double rp = MathLib.sqrt(rp2);
            final double sinPhi = MathLib.divide(posz, rp);
            final double cosPhi = MathLib.divide(rpxy, rp);
            final double sineLambda = MathLib.divide(posy, rpxy);
            final double cosineLambda = MathLib.divide(posx, rpxy);

            // constant multiplicative coefficient : mu_p(i) / mu_c * (req / r_p)^3
            final double k = MathLib.divide(bodyP.getGM(), this.paramMu.getValue())
                * MathLib.pow(MathLib.divide(this.paramAe.getValue(), rp), 4) / 7.;

            // calculation of cos(m * lambda) and sin(m * lambda) thanks to an iterative method
            sinml[0] = 0;
            cosml[0] = 1;
            for (int j = 1; j < 5; j++) {
                sinml[j] = sinml[j - 1] * cosineLambda + cosml[j - 1] * sineLambda;
                cosml[j] = cosml[j - 1] * cosineLambda - sinml[j - 1] * sineLambda;
            }

            // Legendre polynomial
            final double p30n = sinPhi * (TWO_POINT_FIVE * sinPhi * sinPhi - ONE_POINT_FIVE) * SQRT7;
            final double p31n = cosPhi * (SEVEN_POINT_FIVE * sinPhi * sinPhi - ONE_POINT_FIVE) * SQRT7OVER6;
            final double p32n = FIFTEEN * sinPhi * cosPhi * cosPhi * SQRT7OVER60;
            final double p33n = FIFTEEN * cosPhi * cosPhi * cosPhi * SQRT7OVER360;

            // unormalized updated coefficient
            c30 += k * p30n;

            c31 += k * p31n * cosml[1];
            s31 += k * p31n * sinml[1];

            c32 += k * p32n * cosml[2];
            s32 += k * p32n * sinml[2];

            c33 += k * p33n * cosml[3];
            s33 += k * p33n * sinml[3];
        }

        // Get anelasticity Love number
        final double[] wk3 = this.standard.getAnelasticityCorrectionLoveNumber3();

        // update C and S coefficients
        this.coefficientsC[3][0] = wk3[0] * c30;

        this.coefficientsC[3][1] = wk3[1] * c31;
        this.coefficientsS[3][1] = wk3[1] * s31;

        this.coefficientsC[3][2] = wk3[2] * c32;
        this.coefficientsS[3][2] = wk3[2] * s32;

        this.coefficientsC[3][3] = wk3[3] * c33;
        this.coefficientsS[3][3] = wk3[3] * s33;
        
        // No result to return
        // Coefficients are simply modified
    }

    /**
     * Frequency corrections.
     * 
     * @param date
     *        current date
     * @throws PatriusException
     *         if position cannot be computed in given frame
     */
    private void frequencyCorrection(final AbsoluteDate date) throws PatriusException {
        // variables initialization:
        int[] sextuplet;
        final double[][] fundamentalArguments;
        double teta;
        double steta;
        double cteta;
        // coefficients initialization:
        double c20 = 0;
        double c21 = 0;
        double s21 = 0;
        double c22 = 0;
        double s22 = 0;

        // get the frequency corrections table:
        final double[][] adk = this.standard.getFrequencyCorrection();
        // compute the Doodson fundamental arguments
        fundamentalArguments = TidesToolbox.computeFundamentalArguments(date, this.standard.getStandard());
        // get the Doodson numbers:
        final double[] doodson = this.standard.getDoodsonNumbers();

        for (int i = 0; i < adk.length; i++) {
            // Get doodson number
            sextuplet = TidesToolbox.nDoodson(doodson[i]);
            teta = 0.;
            for (int j = 0; j < sextuplet.length; j++) {
                teta += sextuplet[j] * fundamentalArguments[j][0];
            }

            final double[] sincos = MathLib.sinAndCos(teta);
            steta = sincos[0];
            cteta = sincos[1];

            // compute the coefficients:
            if (sextuplet[0] == 0) {
                c20 += adk[i][0] * cteta + adk[i][1] * steta;
            } else if (sextuplet[0] == 1) {
                c21 += adk[i][0] * steta + adk[i][1] * cteta;
                s21 += adk[i][0] * cteta - adk[i][1] * steta;
            } else if (sextuplet[0] == 2) {
                c22 += adk[i][0] * cteta;
                s22 += adk[i][0] * steta;
            }
        }
        // Update coefficients
        this.coefficientsC[2][0] += c20;
        this.coefficientsC[2][1] += c21;
        this.coefficientsS[2][1] += s21;
        this.coefficientsC[2][2] += c22;
        this.coefficientsS[2][2] -= s22;
        
        // No result to return
        // Coefficients are simply modified
    }

    /**
     * Ellipticity correction.
     * 
     * @param coefficients
     *        : coefficients of the tidal potential (up to degree 2)
     * @throws PatriusException
     *         if position cannot be computed in given frame
     */
    private void ellipticityCorrection(final double[] coefficients) throws PatriusException {

        final double[] wk2p = this.standard.getEllipticityCorrectionLoveNumber2();

        this.coefficientsC[4][0] = wk2p[0] * coefficients[0];

        this.coefficientsC[4][1] = wk2p[1] * coefficients[1];
        this.coefficientsS[4][1] = wk2p[1] * coefficients[2];

        this.coefficientsC[4][2] = wk2p[2] * coefficients[3];
        this.coefficientsS[4][2] = wk2p[2] * coefficients[4];
    }

    /**
     * Compute the coefficients of the tidal potential up to degree 2.
     * 
     * @param date
     *        : current date
     * @return the table of the coefficients c20, c21, s21, c22, s22
     * @throws PatriusException
     *         if position cannot be computed in given frame
     */
    private double[] thirdBodyAttractionDegree2(final AbsoluteDate date) throws PatriusException {

        // perturbing body position
        final double[] sinml = new double[5];
        final double[] cosml = new double[5];

        // coefficients
        double c20 = 0.;
        double c21 = 0.;
        double s21 = 0.;
        double c22 = 0.;
        double s22 = 0.;

        // loop over the perturbing bodies
        for (final CelestialBody bodyP : this.bodiesP) {

            // body position
            final Vector3D position = bodyP.getPVCoordinates(date, this.bodyFrame).getPosition();
            final double posx = position.getX();
            final double posy = position.getY();
            final double posz = position.getZ();
            final double rpxy2 = posx * posx + posy * posy;
            final double rpxy = MathLib.sqrt(rpxy2);
            final double rp2 = rpxy2 + posz * posz;
            final double rp = MathLib.sqrt(rp2);
            final double sinPhi = MathLib.divide(posz, rp);
            final double cosPhi = MathLib.divide(rpxy, rp);
            final double sinLambda = MathLib.divide(posy, rpxy);
            final double cosLambda = MathLib.divide(posx, rpxy);

            // multiplicative coefficient : mu_p(i) / mu_c * (req / r_p)^3
            final double k = MathLib.divide(ZERO_POINT_TWO * bodyP.getGM(), this.paramMu.getValue())
                * MathLib.pow(MathLib.divide(this.paramAe.getValue(), rp), 3);

            // calculation of cos(m * lambda) and sin(m * lambda) thanks to an iterative method
            sinml[0] = 0;
            cosml[0] = 1;
            for (int j = 1; j < 5; j++) {
                sinml[j] = sinml[j - 1] * cosLambda + cosml[j - 1] * sinLambda;
                cosml[j] = cosml[j - 1] * cosLambda - sinml[j - 1] * sinLambda;
            }

            // Legendre polynomial
            final double p20n = (ONE_POINT_FIVE * sinPhi * sinPhi - ZERO_POINT_FIVE) * SQRT5;
            final double p21n = 3 * sinPhi * cosPhi * SQRT5OVER3;
            final double p22n = 3 * cosPhi * cosPhi * SQRT5OVER12;

            // unormalized updated coefficient
            c20 += k * p20n;

            c21 += k * p21n * cosml[1];
            s21 += k * p21n * sinml[1];

            c22 += k * p22n * cosml[2];
            s22 += k * p22n * sinml[2];
        }

        return new double[] { c20, c21, s21, c22, s22 };
    }

    /** {@inheritDoc} */
    @Override
    public boolean computeGradientPosition() {
        return this.computePartialDerivativesWrtPosition;
    }
    
    /** {@inheritDoc} */
    @Override
    public void checkData(final AbsoluteDate start, final AbsoluteDate end) throws PatriusException {
        // Nothing to do
    }
}
