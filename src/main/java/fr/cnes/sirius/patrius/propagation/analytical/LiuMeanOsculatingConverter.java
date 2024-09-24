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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2299:27/05/2020:Implementation du propagateur analytique de Liu 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.analytical;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.EquinoctialOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.KeplerianParameters;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.MeanOsculatingElementsProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 *
 **/
/**
 * Liu mean - osculating elements converter.
 * It provides a mean - osculating elements conversion following Liu theory.
 * Liu theory is detailed in the article published by J.J.F. Liu in 1980 entitled
 * "Semianalytic Theory for a Close-Earth Artificial Satellite".
 * 
 * @author Noe Charpigny
 *
 * @since 4.5
 */
public class LiuMeanOsculatingConverter implements MeanOsculatingElementsProvider {

    /** Default convergence threshold for osculating to mean/secular algorithm. */
    private static final double DEFAULT_THRESHOLD = 1E-14;

    /** Reference radius of the central body attraction model (Earth, for instance) (m). */
    private final double referenceRadius;

    /** Un-normalized 2nd zonal coefficient (about 1.08e-3 for Earth). */
    private final double j2;

    /** Central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>). */
    private final double mu;

    /** Inertial or quasi-inertial frame in which the model is supposed valid. */
    private final Frame frame;

    /**
     * Relative convergence threshold for osculating to mean/secular algorithm.
     * Default threshold is 1E-14.
     */
    private double threshold = DEFAULT_THRESHOLD;

    /**
     * Constructor.
     * @param referenceRadius
     *        reference radius of the central body attraction model (m)
     * @param mu
     *        central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @param j2
     *        Second harmonic of the oblateness of the Earth (.)
     * @param frame
     *        Inertial or quasi-inertial frame in which the model is supposed valid, the Z axis of the frame being the
     *        polar axis of the body
     * @throws PatriusException
     *         thrown if failed to build initial state or coefficients frame is not inertial
     */
    public LiuMeanOsculatingConverter(final double referenceRadius,
            final double mu,
            final double j2,
            final Frame frame) throws PatriusException {

        // Building Frame
        if (!frame.isPseudoInertial()) {
            // If frame is not pseudo-inertial, an exception is thrown
            throw new PatriusException(PatriusMessages.NOT_INERTIAL_FRAME);
        }
        this.frame = frame;

        // Building the other attributes
        this.referenceRadius = referenceRadius;
        this.mu = mu;
        this.j2 = j2;
    }

    /** {@inheritDoc} */
    @Override
    public Orbit osc2mean(final Orbit orbit) throws PatriusException {

        // Initialization
        final Orbit osculating = convertFrame(orbit, this.frame);

        // Relative tolerance: threshold for each equinoctial parameter
        final double[] tol = { this.threshold * (1. + MathLib.abs(osculating.getA())),
            this.threshold * (1. + MathLib.abs(osculating.getEquinoctialEx())),
            this.threshold * (1. + MathLib.abs(osculating.getEquinoctialEy())),
            this.threshold * (1. + MathLib.abs(osculating.getHx())),
            this.threshold * (1. + MathLib.abs(osculating.getHy())),
            this.threshold * FastMath.PI };

        // max number of iterations and current number
        int iter = 0;
        final double maxIter = 100;

        // Mean parameter initialization to osculating parameters
        EquinoctialOrbit mean = new EquinoctialOrbit(osculating);

        // Loop until convergence
        while (iter < maxIter) {

            // Compute f
            final Orbit newOsc = new EquinoctialOrbit(mean2osc(mean));

            // Parameters residuals
            final double[] delta = { newOsc.getA() - osculating.getA(),
                    newOsc.getEquinoctialEx() - osculating.getEquinoctialEx(),
                    newOsc.getEquinoctialEy() - osculating.getEquinoctialEy(), newOsc.getHx() - osculating.getHx(),
                    newOsc.getHy() - osculating.getHy(), newOsc.getLv() - osculating.getLv() };

            // Update mean parameters
            mean = new EquinoctialOrbit(mean.getA() - delta[0], mean.getEquinoctialEx() - delta[1],
                    mean.getEquinoctialEy() - delta[2], mean.getHx() - delta[3], mean.getHy() - delta[4], mean.getLv()
                            - delta[5], PositionAngle.TRUE, mean.getFrame(), mean.getDate(), this.mu);

            // Check convergence
            boolean interrupt = true;
            for (int i = 0; i < delta.length; i++) {
                interrupt &= MathLib.abs(delta[i]) < tol[i];
            }
            if (interrupt) {
                return orbit.getType().convertType(mean);
            }
            // Update loop counter
            iter++;
        }

        // Algorithm did not converge
        throw new PropagationException(PatriusMessages.UNABLE_TO_COMPUTE_LIU_MEAN_PARAMETERS, maxIter);
    }

    //CHECKSTYLE: stop MethodLength check
    /** {@inheritDoc} */
    @Override
    public Orbit mean2osc(final Orbit orbit) throws PatriusException {
      //CHECKSTYLE: resume MethodLength check

        // Converting the orbit into the frame used for short period variations computations
        final Orbit orbitComputationFrame = convertFrame(orbit, this.frame);

        // Transforming the input orbit into a Keplerian orbit
        final KeplerianOrbit kep = new KeplerianOrbit(orbitComputationFrame);

        // Computing the magnitude r of the position vector
        final PVCoordinates pv = kep.getParameters().getCartesianParameters().getPVCoordinates();
        final double r = pv.getPosition().getNorm();

        // Getting the mean Keplerian orbital parameters.
        final double a = kep.getA();
        final double e = kep.getE();
        final double i = kep.getI();
        final double pa = kep.getPerigeeArgument();
        final double raan = kep.getRightAscensionOfAscendingNode();
        final double m = kep.getMeanAnomaly();
        final double f = kep.getTrueAnomaly();

        // Computing the first-order short-periodic variations parameters. Equation taken from :
        // J.F.F.Liu (1980) 'Equations taken from the paper titled Semianalytic Theory for a CloseEarth Artificial
        // Satellite',
        // Journal of guidance and control, 3(), pp. 304.

        // Computing the Ellipse parameter (latus rectum)
        final double p = a * (1 - e * e);

        // Temporary variables for computation speed-up:
        final double sini = MathLib.sin(i);
        final double sini2 = sini * sini;
        final double e2 = e * e;
        final double e3 = MathLib.sqrt(1 - e2);
        final double ep = MathLib.pow(this.referenceRadius / p, 2);

        // Sin cos of different frequencies
        final double[] sincosf = MathLib.sinAndCos(f);
        final double sinf = sincosf[0];
        final double cosf = sincosf[1];
        final double[] sincos2f = MathLib.sinAndCos(2 * f);
        final double sin2f = sincos2f[0];
        final double cos2f = sincos2f[1];
        final double[] sincos3f = MathLib.sinAndCos(3 * f);
        final double sin3f = sincos3f[0];
        final double cos3f = sincos3f[1];
        final double[] sincos2pa = MathLib.sinAndCos(2 * pa);
        final double sin2pa = sincos2pa[0];
        final double cos2pa = sincos2pa[1];
        final double[] sincos2paf = MathLib.sinAndCos(2 * pa + f);
        final double sin2paf = sincos2paf[0];
        final double cos2paf = sincos2paf[1];
        final double[] sincos2pamf = MathLib.sinAndCos(2 * pa - f);
        final double sin2pamf = sincos2pamf[0];
        final double cos2pamf = sincos2pamf[1];
        final double[] sincos2pa2f = MathLib.sinAndCos(2 * pa + 2 * f);
        final double sin2pa2f = sincos2pa2f[0];
        final double cos2pa2f = sincos2pa2f[1];
        final double[] sincos2pa3f = MathLib.sinAndCos(2 * pa + 3 * f);
        final double sin2pa3f = sincos2pa3f[0];
        final double cos2pa3f = sincos2pa3f[1];
        final double[] sincos2pa4f = MathLib.sinAndCos(2 * pa + 4 * f);
        final double sin2pa4f = sincos2pa4f[0];
        final double cos2pa4f = sincos2pa4f[1];
        final double[] sincos2pa5f = MathLib.sinAndCos(2 * pa + 5 * f);
        final double sin2pa5f = sincos2pa5f[0];
        final double cos2pa5f = sincos2pa5f[1];

        final double sin2i = MathLib.sin(2 * i);
        final double cosi = MathLib.cos(i);

        // Computing the short period variation aSp of the semimajor axis a.
        final double aSp = +this.j2
                * MathLib.pow(this.referenceRadius, 2)
                / a
                * (+MathLib.pow(a / r, 3) * (1 - 1.5 * sini2 + 1.5 * sini2 * cos2pa2f) - (1 - 1.5 * sini2)
                        * MathLib.pow((1 - e2), -1.5));
        // Computing the short period variation eSp of the eccentricity e.
        final double eSp = 0.5 * this.j2 * ep * (1. - 1.5 * sini2)
                * (1 / e * (1. + 1.5 * e2 - MathLib.pow(1. - e2, 1.5)) + 3. * (1. + e2 / 4.) * cosf + 1.5 
                        * e * cos2f + e2 / 4. * cos3f)
                + 3. / 8. * this.j2 * ep * sini2
                * ((1. + 11. / 4. * e2) * cos2paf + e2 / 4. * cos2pamf + 5. * e * cos2pa2f + 1. / 3.
                        * (7. + 17. / 4. * e2) * cos2pa3f + 1.5 * e * cos2pa4f + 0.25 * e2 * cos2pa5f 
                        + 1.5 * e * cos2pa);

        // Computing the short period variation iSp of the inclination i.
        final double iSp = 3. / 8. * this.j2 * ep * sin2i * (+e * cos2paf + cos2pa2f + e / 3. * cos2pa3f);

        // Computing the short period variation paSp of the argument of periapsis pa ( also known as omega ).
        final double paSp = 0.75 * this.j2 * ep * (4 - 5 * sini2) * (f - m + e * sinf)
                + 1.5 * this.j2 * ep * (1 - 1.5 * sini2)
                * (+1 / e * (1 - 0.25 * e2) * sinf + 0.5 * sin2f + 1. / 12. * e * sin3f)
                - 1.5 * this.j2 * ep
                * (1 / e * (+0.25 * sini2 + e2 / 2 * (1 - 15. / 8. * sini2)) * sin2paf + e / 16 * sini2 * sin2pamf
                        + 0.5 * (1 - 2.5 * sini2) * sin2pa2f - 1 / e
                        * (+7. / 12. * sini2 - e2 / 6 * (1 - 19. / 8. * sini2)) * sin2pa3f - 3. / 8. * sini2 
                        * sin2pa4f - 1. / 16. * e * sini2 * sin2pa5f) - 9. / 16. * this.j2 * ep * sini2 * sin2pa;

        // Computing the short period variation mSp of the Right Ascension of the Ascending Node (RAAN)
        // ( also known as Omega ).
        final double raanSp = -1.5 * this.j2 * ep * cosi
                * (+f - m + e * sinf - e / 2 * sin2paf - 1. / 2. * sin2pa2f - e / 6 * sin2pa3f);

        // Computing the short period variation mSp of the mean anomaly m ( also known as M ).
        final double mSp = -1.5 * this.j2 * ep * e3 / e
                * ((1 - 1.5 * sini2) * ((1 - 0.25 * e2) * sinf + e / 2 * sin2f + e2 / 12 * sin3f) + 0.5
                        * sini2
                        * (-0.5 * (1 + 5. / 4. * e2) * sin2paf - e2 / 8 * sin2pamf + 7. / 6. * (1 - e2 / 28) * sin2pa3f
                                + 3. / 4. * e * sin2pa4f + e2 / 8 * sin2pa5f)) + 9. / 16. * this.j2 * ep * e3 * sini2
                * sin2pa;

        // Computing the osculating terms ( computed short-period terms + given mean terms ) and
        // Concatenating the computed short-period parameters variations.
        final KeplerianParameters paramSp = new KeplerianParameters(a + aSp, e + eSp, i + iSp, pa + paSp,
                raan + raanSp, m + mSp, PositionAngle.MEAN, kep.getMu());

        // Concatenating the computed osculating terms in a KeplerianOrbit object
        final KeplerianOrbit kepOut = new KeplerianOrbit(paramSp, kep.getFrame(), kep.getDate());

        // Converting the orbit into the user frame.
        final Orbit kepOutUserFrame = convertFrame(kepOut, orbit.getFrame());

        // Returning a KeplerianOrbit object containing the computed osculating terms,
        // the date and the frame of the inputed orbit.
        return orbit.getType().convertType(kepOutUserFrame);
    }

    /**
     * Convert provided orbit in output frame.
     * 
     * @param orbit
     *        orbit
     * @param outputFrame
     *        output frame
     * @return converted orbit in output frame
     * @throws PatriusException
     *         thrown if conversion failed
     */
    protected Orbit convertFrame(final Orbit orbit, final Frame outputFrame) throws PatriusException {
        Orbit res = orbit;
        if (!orbit.getFrame().equals(outputFrame)) {
            res = new KeplerianOrbit(orbit.getPVCoordinates(outputFrame), outputFrame, orbit.getDate(), orbit.getMu());
        }
        return res;
    }

    /**
     * Setter for relative convergence threshold for osculating to mean algorithm.
     * 
     * @param newThreshold
     *        new relative threshold
     */
    public void setThreshold(final double newThreshold) {
        this.threshold = newThreshold;
    }

    /** {@inheritDoc} */
    @Override
    public Orbit propagateMeanOrbit(final AbsoluteDate date) throws PatriusException {
        // Not available for Liu propagator
        throw new PatriusException(PatriusMessages.METHOD_NOT_AVAILABLE_LIU);
    }
}
