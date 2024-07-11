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
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.6:DM:DM-2622:27/01/2021:Modelisation de la maree polaire dans Patrius 
 * VERSION:4.6:DM:DM-2571:27/01/2021:[PATRIUS] Integrateur Stormer-Cowell 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity.tides;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class implements the perturbating force due to pole tides. Pole tide is the deformation of the Earth due to
 * the movement of its rotation axis. Polar tides directly depends on Earth pole position (xp, yp) and have two
 * contributors:
 * <ul>
 * <li>Solid Earth pole tides</li>
 * <li>Earth ocean pole tides</li>
 * </ul>
 * It is possible to activate/deactivate each of these contributors through flags at construction.
 * 
 * @author Emmanuel Bignon
 * 
 * @since 4.6
 */
public class PoleTides extends AbstractTides {

    /** Serial UID. */
    private static final long serialVersionUID = 5448255772798307519L;

    /** Milli arcsec to arcsec coefficient. */
    private static final double MILLIARCSEC_TO_ARCSEC = 1000.;
    
    /** Solid Earth pole tides contribution. */
    private final boolean solidContributionFlag;

    /** Ocean pole tides contribution. */
    private final boolean oceanicContributionFlag;

    /** True if acceleration partial derivatives with respect to position have to be computed. */
    private final boolean computePartialDerivativesWrtPosition;

    /**
     * Constructor.
     * 
     * @param centralBodyFrame
     *        rotating body frame
     * @param equatorialRadius
     *        reference equatorial radius of the potential
     * @param mu
     *        central body attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @param solidTidesFlag
     *        true if solid Earth pole tides contribution should be taken into account
     * @param oceanicTidesFlag
     *        true if ocean pole tides contribution should be taken into account
     */
    public PoleTides(final Frame centralBodyFrame,
            final double equatorialRadius,
            final double mu,
            final boolean solidTidesFlag,
            final boolean oceanicTidesFlag) {
        this(centralBodyFrame, equatorialRadius, mu, solidTidesFlag, oceanicTidesFlag, false);
    }

    /**
     * Constructor.
     * 
     * @param centralBodyFrame
     *        rotating body frame
     * @param equatorialRadius
     *        reference equatorial radius of the potential
     * @param mu
     *        central body attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @param solidTidesFlag
     *        true if solid Earth pole tides contribution should be taken into account
     * @param oceanicTidesFlag
     *        true if ocean pole tides contribution should be taken into account
     * @param computePD
     *        true if partial derivatives wrt position have to be computed
     */
    public PoleTides(final Frame centralBodyFrame,
            final double equatorialRadius,
            final double mu,
            final boolean solidTidesFlag,
            final boolean oceanicTidesFlag,
            final boolean computePD) {
        super(centralBodyFrame, equatorialRadius, mu, 3, 3, computePD ? 3 : 0, computePD ? 3 : 0);
        this.solidContributionFlag = solidTidesFlag;
        this.oceanicContributionFlag = oceanicTidesFlag;
        this.computePartialDerivativesWrtPosition = computePD;
    }

    /**
     * Constructor.
     * 
     * @param centralBodyFrame
     *        rotating body frame
     * @param equatorialRadius
     *        reference equatorial radius of the potential parameter
     * @param mu
     *        central body attraction coefficient (m<sup>3</sup>/s<sup>2</sup>) parameter
     * @param solidTidesFlag
     *        true if solid Earth pole tides contribution should be taken into account
     * @param oceanicTidesFlag
     *        true if ocean pole tides contribution should be taken into account
     */
    public PoleTides(final Frame centralBodyFrame,
            final Parameter equatorialRadius,
            final Parameter mu,
            final boolean solidTidesFlag,
            final boolean oceanicTidesFlag) {
        this(centralBodyFrame, equatorialRadius, mu, solidTidesFlag, oceanicTidesFlag, false);
    }

    /**
     * Constructor.
     * 
     * @param centralBodyFrame
     *        rotating body frame
     * @param equatorialRadius
     *        reference equatorial radius of the potential parameter
     * @param mu
     *        central body attraction coefficient (m<sup>3</sup>/s<sup>2</sup>) parameter
     * @param solidTidesFlag
     *        true if solid Earth pole tides contribution should be taken into account
     * @param oceanicTidesFlag
     *        true if ocean pole tides contribution should be taken into account
     * @param computePD
     *        true if partial derivatives have to be computed
     */
    public PoleTides(final Frame centralBodyFrame,
            final Parameter equatorialRadius,
            final Parameter mu,
            final boolean solidTidesFlag,
            final boolean oceanicTidesFlag,
            final boolean computePD) {
        super(centralBodyFrame, equatorialRadius, mu, 3, 3, computePD ? 3 : 0, computePD ? 3 : 0);
        this.solidContributionFlag = solidTidesFlag;
        this.oceanicContributionFlag = oceanicTidesFlag;
        this.computePartialDerivativesWrtPosition = computePD;
    }

    /** {@inheritDoc} */
    @Override
    public void updateCoefficientsCandS(final AbsoluteDate date) throws PatriusException {

        if (solidContributionFlag || oceanicContributionFlag) {
            // Duration in years since J2000 epoch
            final double t = date.durationFrom(AbsoluteDate.J2000_EPOCH) / Constants.JULIAN_YEAR;

            // Secular position of pole in arcsec
            final double xs = (55. + 1.677 * t) / MILLIARCSEC_TO_ARCSEC;
            final double ys = (320.5 + 3.460 * t) / MILLIARCSEC_TO_ARCSEC;
            
            // Current position of pole
            final double[] pole = FramesFactory.getConfiguration().getPolarMotion(date);
            final double xp = pole[0] / Constants.ARC_SECONDS_TO_RADIANS;
            final double yp = pole[1] / Constants.ARC_SECONDS_TO_RADIANS;
            
            // Delta in arcsec
            final double m1 = xp - xs;
            final double m2 = -(yp - ys);

            this.coefficientsC[2][1] = 0.;
            this.coefficientsS[2][1] = 0.;
            
            // Solid tides
            if (solidContributionFlag) {
                // Delta on coefficients C21 and S21
                final double dc21 = -1.333 * 1E-9 * (m1 + 0.0115 * m2);
                final double ds21 = -1.333 * 1E-9 * (m2 - 0.0115 * m1);
                this.coefficientsC[2][1] = dc21;
                this.coefficientsS[2][1] = ds21;
            }

            // Oceanic tides
            if (oceanicContributionFlag) {
                // Delta on coefficients C21 and S21
                final double dc21 = -2.1778 * 1E-10 * (m1 + 0.01724 * m2);
                final double ds21 = -1.7232 * 1E-10 * (m2 - 0.03365 * m1);
                this.coefficientsC[2][1] = dc21;
                this.coefficientsS[2][1] = ds21;
            }
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

    /** {@inheritDoc} */
    @Override
    public boolean computeGradientPosition() {
        return this.computePartialDerivativesWrtPosition;
    }

    /** {@inheritDoc} */
    @Override
    public void checkData(final AbsoluteDate start,
            final AbsoluteDate end) throws PatriusException {
        // Nothing to do
    }
}
