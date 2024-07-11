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
 * @history 21/02/2013
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
 * VERSION:4.8:FA:FA-3009:15/11/2021:[PATRIUS] IllegalArgumentException SolarActivityToolbox
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:91:26/07/2013: corrections for validation
 * VERSION::FA:345:30/10/2014:modified comments ratio
 * VERSION::DM:316:26/02/2015:take into account Sun-Satellite direction in PRS computation
 * VERSION::DM:317:04/03/2015: STELA integration in CIRF with referential choice (ICRF, CIRF or MOD)
 * VERSION::DM:396:16/03/2015:new architecture for orbital parameters
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::FA:829:25/01/2017:Protection of trigonometric methods
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.forces.radiation;

import java.util.ArrayList;

import fr.cnes.sirius.patrius.forces.radiation.RadiationSensitive;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.frames.LocalOrbitalFrame;
import fr.cnes.sirius.patrius.math.complex.Complex;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.stela.JavaMathAdapter;
import fr.cnes.sirius.patrius.stela.forces.AbstractStelaGaussContribution;
import fr.cnes.sirius.patrius.stela.forces.Squaring;
import fr.cnes.sirius.patrius.stela.orbits.OrbitNatureConverter;
import fr.cnes.sirius.patrius.stela.orbits.StelaEquinoctialOrbit;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Class representing the srp gauss force model for STELA
 * 
 * @concurrency conditionally thread-safe
 * @concurrency.comment thread-safe if the given sun is thread-safe
 * 
 * @see AbstractStelaGaussContribution
 * 
 * @author Rami Houdroge
 * 
 * @version $Id$
 * 
 * @since 1.3
 * 
 */
public class SRPSquaring extends AbstractStelaGaussContribution {

    /** Default quadrature points */
    public static final int DEFAULT_QUADRATURE_POINTS = 11;

     /** Serializable UID. */
    private static final long serialVersionUID = 7808923418399620235L;

    /** 2π */
    private static final double TWO_PI = 2 * FastMath.PI;

    /** Epsilon for Polynomial order determination, from STELA internal parameters file */
    private static final double TOL_MAX_DEG = 1E-06;

    /** -0. */
    private static final double MINUS_ZERO = -0.;
    
    /** -3. */
    private static final double MINUS_THREE = -3.;

    /** Assembly */
    private final RadiationSensitive spacecraft;

    /** Number of points for Simpson's quadrature. */
    private final int quadPoints;

    /** Sun */
    private final PVCoordinatesProvider sun;

    /** Earth equatorial radius */
    private final double radius;

    /** Reference flux normalized for a 1m distance (N). */
    private final double kRef;

    /**
     * Create an instance with an assembly and a number of points. Reference normalized flux is that of STELA.
     * 
     * @param radiativeSpacecraft
     *        the radiative spacecraft
     * @param sunBody
     *        the sun
     * @param earthRadius
     *        earth's equatorial radius
     */
    public SRPSquaring(final RadiationSensitive radiativeSpacecraft, final PVCoordinatesProvider sunBody,
        final double earthRadius) {
        this(radiativeSpacecraft, DEFAULT_QUADRATURE_POINTS, sunBody, earthRadius, Constants.CNES_STELA_UA,
            Constants.CONST_SOL_STELA);
    }

    /**
     * Create an instance with an assembly and a number of points. Reference normalized flux is that of STELA.
     * 
     * @param radiativeSpacecraft
     *        the radiative spacecraft
     * @param quadraturePoints
     *        the number of points for quadrature
     * @param sunBody
     *        the sun
     * @param earthRadius
     *        earth's equatorial radius
     */
    public SRPSquaring(final RadiationSensitive radiativeSpacecraft, final int quadraturePoints,
        final PVCoordinatesProvider sunBody, final double earthRadius) {
        this(radiativeSpacecraft, quadraturePoints, sunBody, earthRadius, Constants.CNES_STELA_UA,
            Constants.CONST_SOL_STELA);
    }

    /**
     * Create an instance with an assembly and a number of points.
     * 
     * @param radiativeSpacecraft
     *        the radiative spacecraft
     * @param quadraturePoints
     *        the number of points for quadrature
     * @param sunBody
     *        the sun
     * @param earthRadius
     *        earth's equatorial radius
     * @param dRef
     *        reference distance for the solar radiation pressure (m)
     * @param pRef
     *        reference solar radiation pressure at dRef (N/m<sup>2</sup>)
     */
    public SRPSquaring(final RadiationSensitive radiativeSpacecraft, final int quadraturePoints,
        final PVCoordinatesProvider sunBody, final double earthRadius, final double dRef, final double pRef) {
        super();
        this.spacecraft = radiativeSpacecraft;
        this.quadPoints = quadraturePoints;
        this.sun = sunBody;
        this.radius = earthRadius;
        this.kRef = pRef * dRef * dRef;
    }

    /** {@inheritDoc} */
    @Override
    public double[] computePerturbation(final StelaEquinoctialOrbit orbit,
                                        final OrbitNatureConverter converter) throws PatriusException {

        // Get Sun PV coordinates
        final PVCoordinates sunPV = this.sun.getPVCoordinates(orbit.getDate(), orbit.getFrame());

        // Determination of shadowed part of the orbit (in and out true anomalies)
        final double[] nuInOut = this.computeInOutTrueAnom(orbit, sunPV);
        // Eccentricity
        final double ex = orbit.getEquinoctialEx();
        final double ey = orbit.getEquinoctialEy();
        final double e2 = ex * ex + ey * ey;
        final double e = MathLib.sqrt(e2);
        final double aopPlusRaan = MathLib.atan2(ey, ex);
        final double sqrt1minusE2 = MathLib.sqrt(MathLib.max(0.0, 1. - e2));
        // Cos of in-out true anomalies
        final double[] sincosNu1 = MathLib.sinAndCos(nuInOut[0]);
        final double sinNuIn = sincosNu1[0];
        final double cosNuIn = sincosNu1[1];
        final double[] sincosNu2 = MathLib.sinAndCos(nuInOut[1]);
        final double sinNuOut = sincosNu2[0];
        final double cosNuOut = sincosNu2[1];
        // Sin of in-out eccentric anomalies
        final double sinEeccIn = sqrt1minusE2 * sinNuIn / (1. + e * cosNuIn);
        final double sinEeccOut = sqrt1minusE2 * sinNuOut / (1. + e * cosNuOut);
        // Corresponding eccentric anomalies
        double anomEccIn = MathLib.atan2(sinEeccIn, (cosNuIn + e) / (1. + e * cosNuIn));
        double anomEccOut = MathLib.atan2(sinEeccOut, (cosNuOut + e) / (1. + e * cosNuOut));
        anomEccIn = JavaMathAdapter.mod(anomEccIn, TWO_PI);
        anomEccOut = JavaMathAdapter.mod(anomEccOut, TWO_PI);

        // Quadrature points repartition
        final double deltaEi;
        if (anomEccIn <= anomEccOut) {
            deltaEi = (TWO_PI - (anomEccOut - anomEccIn)) / (this.quadPoints - 1);
        } else {
            deltaEi = (anomEccIn - anomEccOut) / (this.quadPoints - 1);
        }
        // Quadrature points mean and eccentric anomalies
        final double[] meanAnomalies = new double[this.quadPoints];
        final double[] eccAnomalies = new double[this.quadPoints];
        for (int i = 0; i <= this.quadPoints - 1; i++) {
            eccAnomalies[i] = anomEccOut + i * deltaEi;
            meanAnomalies[i] = eccAnomalies[i] - e * MathLib.sin(eccAnomalies[i]);
        }

        // Mean motion
        final double n = MathLib.divide(MathLib.sqrt(MathLib.divide(orbit.getMu(), orbit.getA())), orbit.getA());

        final double[] result = new double[6];
        final double[] dADt = new double[this.quadPoints];
        final double[] dLamEqDt = new double[this.quadPoints];
        final double[] dExDt = new double[this.quadPoints];
        final double[] dEyDt = new double[this.quadPoints];
        final double[] dIxDt = new double[this.quadPoints];
        final double[] dIyDt = new double[this.quadPoints];

        // srp contribution for each quadrature point.
        for (int i = 0; i < this.quadPoints; i++) {
            // Get quadrature point date
            final double minit = JavaMathAdapter.mod(orbit.getLM() - aopPlusRaan, TWO_PI);
            final double mquad = JavaMathAdapter.mod(meanAnomalies[i], TWO_PI);
            final AbsoluteDate date = orbit.getDate().shiftedBy(MathLib.divide(mquad - minit, n));

            // LOF for quadrature point
            final StelaEquinoctialOrbit quadOrbit = new StelaEquinoctialOrbit(orbit.getA(), orbit.getEquinoctialEx(),
                orbit.getEquinoctialEy(), orbit.getIx(), orbit.getIy(), meanAnomalies[i] + aopPlusRaan,
                orbit.getFrame(), date, orbit.getMu());
            final LocalOrbitalFrame lof = new LocalOrbitalFrame(quadOrbit.getFrame(), LOFType.TNW, quadOrbit,
                "currentPosition");

            // Get satellite - Sun vector
            final PVCoordinates satSunVect2 = this.getSatSunVector(quadOrbit.getPVCoordinates(), quadOrbit.getFrame(),
                quadOrbit.getDate());
            // Solar Radiation Pressure in the inertial frame
            final Vector3D srp = this.computeAcceleration(quadOrbit, satSunVect2);

            // get srp in LOF
            final Vector3D srpTNW = orbit.getFrame().getTransformTo(lof, quadOrbit.getDate()).transformVector(srp);
            final double[] instantGaussEq = JavaMathAdapter.matrixVectorMultiply(computeGaussEquations(quadOrbit),
                srpTNW.toArray());

            // Perform change of variable
            final double adjustCoef = computeAdjustCoef(quadOrbit, eccAnomalies[i]);
            dADt[i] = adjustCoef * instantGaussEq[0];
            dLamEqDt[i] = adjustCoef * instantGaussEq[1];
            dExDt[i] = adjustCoef * instantGaussEq[2];
            dEyDt[i] = adjustCoef * instantGaussEq[3];
            dIxDt[i] = adjustCoef * instantGaussEq[4];
            dIyDt[i] = adjustCoef * instantGaussEq[5];
        }
        // Simpson's rule
        result[0] = Squaring.simpsonMean(dADt, deltaEi);
        result[1] = Squaring.simpsonMean(dLamEqDt, deltaEi);
        result[2] = Squaring.simpsonMean(dExDt, deltaEi);
        result[3] = Squaring.simpsonMean(dEyDt, deltaEi);
        result[4] = Squaring.simpsonMean(dIxDt, deltaEi);
        result[5] = Squaring.simpsonMean(dIyDt, deltaEi);
        this.dPert = result;
        return result;
    }

    /**
     * Compute the acceleration due to the force.
     * 
     * @param orbit
     *        an orbit
     * @param satSunVector
     *        Satellite - Sun vector coordinates
     * @return acceleration in the {@link StelaEquinoctialOrbit#getFrame() orbit reference frame}
     * @exception PatriusException
     *            if some specific error occurs
     */
    public Vector3D computeAcceleration(final StelaEquinoctialOrbit orbit,
                                        final PVCoordinates satSunVector) throws PatriusException {

        final Vector3D flux = this.getFlux(orbit, satSunVector);
        // Attitude not taken into account
        final SpacecraftState state = new SpacecraftState(orbit);
        return this.spacecraft.radiationPressureAcceleration(state, flux);
    }

    /**
     * Get the solar flux vector.
     * 
     * @param orbit
     *        an orbit
     * @param satSunVect
     *        Satellite - Sun vector
     * @return the solar flux
     */
    protected Vector3D getFlux(final Orbit orbit, final PVCoordinates satSunVect) {
        final Vector3D satSunPos = satSunVect.getPosition();

        final double r2 = satSunPos.getNormSq();
        final double rawP = MathLib.divide(this.kRef, r2);
        final Vector3D flux = new Vector3D(MathLib.divide(rawP, MathLib.sqrt(r2)), satSunPos);
        return flux.negate();
    }

    /**
     * Compute sat-Sun vector in spacecraft state frame.
     * 
     * @param pv
     *        a position-velocity
     * @param frame
     *        frame in which the pv is expressed
     * @param date
     *        at which the PV is expressed, and Sun position is desired
     * @return sat-Sun vector in frame
     * @exception PatriusException
     *            if Sun position cannot be computed
     */
    private PVCoordinates getSatSunVector(final PVCoordinates pv, final Frame frame,
                                          final AbsoluteDate date) throws PatriusException {
        final PVCoordinates sunPV = this.sun.getPVCoordinates(date, frame);
        return new PVCoordinates(sunPV.getPosition().subtract(pv.getPosition()),
            sunPV.getVelocity().subtract(pv.getVelocity()));
    }

    /**
     * Compute a coefficient to represent the irregular distribution of eccentric anomalies.
     * 
     * @param orbit
     *        an orbit
     * @param eAnom
     *        eccentric anomaly corresponding to orbit.
     * @return the adjustment coefficient.
     */
    private static double computeAdjustCoef(final StelaEquinoctialOrbit orbit, final double eAnom) {
        final double ex = orbit.getEquinoctialEx();
        final double ey = orbit.getEquinoctialEy();
        return 1.0 - MathLib.sqrt(ex * ex + ey * ey) * MathLib.cos(eAnom);
    }

    /**
     * Computation of in and out true anomalies of the shadowed part of the orbit.
     * 
     * @param orbit
     *        an orbit
     * @param sunPV
     *        Sun coordinates
     * @return [&nu;<sub>in</sub>, &nu;<sub>out</sub>] : in and out true anomalies
     * @throws PatriusException
     *         if computation of declination and right ascension of the Sun fails
     */
    protected double[] computeInOutTrueAnom(final StelaEquinoctialOrbit orbit,
                                            final PVCoordinates sunPV) throws PatriusException {
        // Sun declination and right ascension in the orbital plane
        final double[] sunPhiBeta = computeSunBetaPhi(orbit, sunPV);
        final double beta = sunPhiBeta[0];
        final double phi = sunPhiBeta[1];
        // Initializations
        final double[] sincosbeta = MathLib.sinAndCos(beta);
        final double sinBeta = sincosbeta[0];
        final double cosBeta = sincosbeta[1];
        final double sinBetaSq = sinBeta * sinBeta;
        final double[] sincosphi = MathLib.sinAndCos(phi);
        final double sinPhi = sincosphi[0];
        final double cosPhi = sincosphi[1];
        final double a = orbit.getA();
        final double e = orbit.getE();
        final double raan = JavaMathAdapter.mod(MathLib.atan2(orbit.getIy(), orbit.getIx()), TWO_PI);
        final double aop = JavaMathAdapter.mod(
            MathLib.atan2(orbit.getEquinoctialEy(), orbit.getEquinoctialEx()) - raan, TWO_PI);
        final double gamma = aop - phi;
        final double nuIn;
        final double nuOut;

        // Test of the eclipse occurrence
        // No Eclipse
        if (MathLib.abs(sinBeta) > this.radius / (a * (1. - e))) {
            nuIn = 0;
            nuOut = 0;
        } else {
            // Eclipse : nuIn != nuOut
            // Initializations
            double k = this.radius / (a * (1. - e * e));
            k *= k;
            final double ex = e * MathLib.cos(aop);
            final double ey = e * MathLib.sin(aop);
            final double exGamma = ex * cosPhi + ey * sinPhi;
            final double eyGamma = ey * cosPhi - ex * sinPhi;
            // Definition of polynomial coefficients
            final double a0 = sinBetaSq - k * (1. - exGamma) * (1. - exGamma);
            final double a1 = -4. * k * eyGamma * (1. - exGamma);
            final double a2 = 2. * (1. + cosBeta * cosBeta - k * (1. - exGamma * exGamma + 2. * eyGamma * eyGamma));
            final double a3 = -4. * k * eyGamma * (1. + exGamma);
            final double a4 = sinBetaSq - k * (1. + exGamma) * (1. + exGamma);
            // Polynomial order
            final int polyOrder = getPolyOrder(a0, a1, a2, a3, a4);
            // Computation of polynomial roots
            final Complex[] roots;
            switch (polyOrder) {
                case 0:
                    throw new IllegalArgumentException();
                case 1:
                    throw new IllegalArgumentException();
                case 2:
                    // Second-order polynomial
                    roots = solvePolyDeg2(new Complex(a2, 0), new Complex(a1, 0), new Complex(a0, 0));
                    break;
                case 3:
                    // Third-order polynomial
                    roots = solvePolyDeg3(a3, a2, a1, a0);
                    break;
                case 4:
                    if (MathLib.abs(a3) < TOL_MAX_DEG && MathLib.abs(a1) < TOL_MAX_DEG) {
                        // Biquadratic polynomial
                        roots = solveBiquadratic(a4, a2, a0);
                    } else {
                        // Classical fourth-order polynomial
                        roots = solvePolyDeg4(a4, a3, a2, a1, a0);
                    }
                    break;
                default:
                    throw PatriusException.createInternalError(null);
            }
            // Roots filtering and ordering
            final double[] alphaInOut = rootsFiltering(roots);
            final double alphaIn = alphaInOut[0];
            final double alphaOut = alphaInOut[1];

            // True anomalies in [0;2pi]
            if (alphaIn == 0 & alphaOut == 0) {
                nuIn = 0;
                nuOut = 0;
            } else {
                nuIn = JavaMathAdapter.mod((alphaInOut[0] - gamma), TWO_PI);
                nuOut = JavaMathAdapter.mod((alphaInOut[1] - gamma), TWO_PI);
            }
        }
        return new double[] { nuIn, nuOut };
    }

    /**
     * Filtering of computed roots.
     * 
     * @param roots
     *        roots of a polynomial
     * @return [&alpha;<sub>in</sub> , &alpha;<sub>out</sub>], with &alpha;<sub>in</sub> < &alpha;<sub>out</sub>
     */
    protected double[] rootsFiltering(final Complex[] roots) {
        final ArrayList<Double> filteredRoots = new ArrayList<>();
        final double[] res = new double[2];
        final int rootsLength = roots.length;
        double realPart;
        double alphaTemp;
        // Filtering
        for (int i = 0; i < rootsLength; i++) {
            if (MathLib.abs(roots[i].getImaginary()) <= TOL_MAX_DEG) {
                realPart = roots[i].getReal();
                alphaTemp = FastMath.PI - 2. * MathLib.atan(realPart);
                if (alphaTemp >= FastMath.PI / 2. && alphaTemp <= 3. * FastMath.PI / 2.) {
                    filteredRoots.add(alphaTemp);
                }
            }
        }
        // Ordering
        // if filtered root number is lower than 2
        if (filteredRoots.size() < 2) {
            res[0] = 0;
            res[1] = 0;
        } else if (filteredRoots.size() == 2) {
            // if filtered root number is equal to 2
            final double alpha1 = filteredRoots.get(0);
            final double alpha2 = filteredRoots.get(1);
            res[0] = MathLib.min(alpha1, alpha2);
            res[1] = MathLib.max(alpha1, alpha2);
        } else {
            // if filtered root number is upper than 2
            throw new IllegalArgumentException();
        }
        return res;
    }

    /**
     * Solves the complex equation c<sub>2</sub>x<sup>2</sup> + c<sub>1</sub>x + c<sub>0</sub> = 0.
     * 
     * @param c2
     *        coefficient of order 2
     * @param c1
     *        coefficient of order 1
     * @param c0
     *        coefficient of order 0
     * @return [x<sub>0</sub>, x<sub>1</sub>] where x<sub>0</sub> and x<sub>1</sub> are the roots of the equation
     */
    protected Complex[] solvePolyDeg2(final Complex c2, final Complex c1, final Complex c0) {
        final Complex delta = c1.multiply(c1).subtract(c2.multiply(c0).multiply(4.));
        final double real = delta.getReal() == MINUS_ZERO ? +0. : delta.getReal();
        final double im = delta.getImaginary() == MINUS_ZERO ? +0. : delta.getImaginary();
        final Complex deltaSq = (new Complex(real, im)).sqrt();
        final Complex x0 = c1.negate().subtract(deltaSq).divide(c2.multiply(2.));
        final Complex x1 = c1.negate().add(deltaSq).divide(c2.multiply(2.));
        return new Complex[] { x0, x1 };
    }

    /**
     * Solves the equation c<sub>3</sub>x<sup>3</sup> + c<sub>2</sub>x<sup>2</sup> + c<sub>1</sub>x + c<sub>0</sub> = 0.
     * 
     * @param c3
     *        coefficient of order 3
     * @param c2
     *        coefficient of order 2
     * @param c1
     *        coefficient of order 1
     * @param c0
     *        coefficient of order 0
     * @return [x<sub>0</sub>, x<sub>1</sub>, x<sub>2</sub>] where x<sub>0</sub> , x<sub>1</sub> and x<sub>2</sub> are
     *         the roots of the equation
     */
    protected Complex[] solvePolyDeg3(final double c3, final double c2, final double c1, final double c0) {
        // Initializations
        final Complex j = new Complex(-1. / 2., MathLib.sqrt(3.) / 2);
        final Complex j2 = j.conjugate();
        final double temp1 = MathLib.divide(c2 * c2, c3 * c3);
        final double temp2 = MathLib.divide(c1, c3);
        final double p = -temp1 / 3. + temp2;
        final double q = MathLib.divide(c2, (3. * 9. * c3)) * (2 * temp1 - 9 * temp2) + MathLib.divide(c0, c3);
        // Roots after change of variables
        final Complex z0;
        final Complex z1;
        final Complex z2;

        // Case p = 0
        if (p == 0) {
            z0 = new Complex(MathLib.signum(-q) * MathLib.pow(MathLib.abs(q), 1. / 3.), 0);
            z1 = j.multiply(z0);
            z2 = j2.multiply(z0);
        } else {
            // Else, the discriminant must be computed
            final double p3 = p * p * p;
            final double delta = q * q + 4. * p3 / (3. * 9.);
            // Null discriminant : three real roots, among which one is double
            if (delta == 0) {
                z0 = new Complex(MathLib.divide(3 * q, p), 0);
                z1 = new Complex(MathLib.divide(MINUS_THREE * q, 2 * p), 0);
                z2 = new Complex(MathLib.divide(MINUS_THREE * q, 2 * p), 0);
            } else if (delta < 0) {
                // Negative discriminant : three distinct real roots
                final double temp3 = 2. * MathLib.sqrt(-p / 3.);
                final double value = -q * MathLib.sqrt(MathLib.divide(-(3. * 9.), p3)) / 2.;
                final double temp4 = MathLib.acos(MathLib.min(1.0, MathLib.max(-1.0, value))) / 3.;
                z0 = new Complex(temp3 * MathLib.cos(temp4), 0);
                z1 = new Complex(temp3 * MathLib.cos(temp4 + 2. * FastMath.PI / 3.), 0);
                z2 = new Complex(temp3 * MathLib.cos(temp4 + 4. * FastMath.PI / 3.), 0);
            } else {
                // Positive discriminant : one real root and two complex roots
                final double deltaSq = MathLib.sqrt(delta);
                final double t1 = (-q + deltaSq) / 2.;
                final double t2 = (-q - deltaSq) / 2.;
                final double u1 = MathLib.signum(t1) * MathLib.pow(MathLib.abs(t1), 1. / 3.);
                final double u2 = MathLib.signum(t2) * MathLib.pow(MathLib.abs(t2), 1. / 3.);
                z0 = new Complex(u1 + u2, 0);
                z1 = j.multiply(u1).add(j2.multiply(u2));
                z2 = j2.multiply(u1).add(j.multiply(u2));
            }
        }
        // Inverse change of variables to get the expected roots
        final Complex zToX = new Complex(c2 / (3. * c3), 0);
        return new Complex[] { z0.subtract(zToX), z1.subtract(zToX), z2.subtract(zToX) };
    }

    /**
     * Solves the equation c<sub>4</sub>x<sup>4</sup> + c<sub>2</sub>x<sup>2</sup> + c<sub>0</sub> = 0.
     * 
     * @param c4
     *        coefficient of order 4
     * @param c2
     *        coefficient of order 2
     * @param c0
     *        coefficient of order 0
     * @return [x<sub>0</sub>, x<sub>1</sub>, x<sub>2</sub>, x<sub>3</sub>] where x<sub>0</sub> , x<sub>1</sub> ,
     *         x<sub>2</sub> and x<sub>3</sub> are the roots of the equation
     */
    protected Complex[] solveBiquadratic(final double c4, final double c2, final double c0) {
        final Complex[] y = solvePolyDeg2(new Complex(c4, 0), new Complex(c2, 0), new Complex(c0, 0));
        final Complex x0 = y[0].sqrt();
        final Complex x2 = y[1].sqrt();
        return new Complex[] { x0, x0.negate(), x2, x2.negate() };
    }

    /**
     * Solves the equation c<sub>4</sub>x<sup>4</sup> + c<sub>3</sub>x<sup>3</sup> + c<sub>2</sub>x<sup>2</sup> +
     * c<sub>1</sub>x + c<sub>0</sub> = 0.
     * 
     * @param c4
     *        coefficient of order 4
     * @param c3
     *        coefficient of order 3
     * @param c2
     *        coefficient of order 2
     * @param c1
     *        coefficient of order 1
     * @param c0
     *        coefficient of order 0
     * @return [x<sub>0</sub>, x<sub>1</sub>, x<sub>2</sub>, x<sub>3</sub>] where x<sub>0</sub> , x<sub>1</sub> ,
     *         x<sub>2</sub> and x<sub>3</sub> are the roots of the equation
     */
    protected Complex[] solvePolyDeg4(final double c4, final double c3, final double c2,
                                      final double c1, final double c0) {
        // Initializations
        final double c1c4 = MathLib.divide(c1, c4);
        final double c2c4 = MathLib.divide(c2, c4);
        final double c3c4 = MathLib.divide(c3, c4);
        final double a = -3. * c3c4 * c3c4 / 8. + c2c4;
        final double b = MathLib.pow(c3c4 / 2., 3) - c3c4 * c2c4 / 2. + c1c4;
        final double c = -3 * MathLib.pow(c3c4 / 4., 4) + c2c4 * c3c4 * c3c4 / (4. * 4.) - c3c4 * c1c4 / 4.
            + MathLib.divide(c0, c4);
        // Roots after change of variables
        final Complex z0;
        final Complex z1;
        final Complex z2;
        final Complex z3;

        // Case b = 0 : biquadratic equation
        if (b == 0) {
            final Complex[] z = solveBiquadratic(1, a, c);
            z0 = z[0];
            z1 = z[1];
            z2 = z[2];
            z3 = z[3];
        } else {
            // b != 0
            // Else another equation solving is needed
            final Complex[] phiRoot = solvePolyDeg3(1., -a, -4 * c, 4 * a * c - b * b);
            final double phiR = phiRoot[0].getReal() / 2.;
            final Complex g = new Complex(a - 2. * phiR, 0).sqrt();
            final Complex ig = Complex.I.multiply(g);
            final Complex ib2g = Complex.I.multiply(b / 2.).divide(g);
            final Complex[] z01 = solvePolyDeg2(Complex.ONE, ig, ib2g.add(phiRoot[0].multiply(1. / 2.)));
            final Complex[] z23 =
                solvePolyDeg2(Complex.ONE, ig.negate(), phiRoot[0].multiply(1. / 2.).subtract(ib2g));
            // temporary result
            z0 = z01[0];
            z1 = z01[1];
            z2 = z23[0];
            z3 = z23[1];
        }
        // Inverse change of variables to get the expected roots
        final Complex zToX = new Complex(c3 / (4. * c4), 0);
        // return roots
        return new Complex[] { z0.subtract(zToX), z1.subtract(zToX), z2.subtract(zToX), z3.subtract(zToX) };
    }

    /**
     * Determination of the order of A<sub>4</sub>*s<sup>4</sup> + A<sub>3</sub>*s<sup>3</sup> +
     * A<sub>2</sub>*s<sup>2</sup> + A<sub>1</sub>*s<sup>1</sup> + A<sub>0</sub> .
     * 
     * @param a0
     *        coefficient of order 0
     * @param a1
     *        coefficient of order 1
     * @param a2
     *        coefficient of order 2
     * @param a3
     *        coefficient of order 3
     * @param a4
     *        coefficient of order 4
     * @return the polynomial order
     */
    protected int getPolyOrder(final double a0, final double a1, final double a2, final double a3,
                                      final double a4) {
        // Initialization
        final int res;
        // Determine polynomial order based on coefficients values
        if (MathLib.abs(a4) > TOL_MAX_DEG) {
            res = 4;
        } else if (MathLib.abs(a3) > TOL_MAX_DEG) {
            res = 3;
        } else if (MathLib.abs(a2) > TOL_MAX_DEG) {
            res = 2;
        } else if (MathLib.abs(a1) > TOL_MAX_DEG) {
            res = 1;
        } else {
            res = 0;
        }
        // Return polynomial order
        return res;
    }

    /**
     * Computation of Sun's right ascension (&phi;) and declination (&beta;) wrt the orbit plane.
     * 
     * @param orbit
     *        orbit
     * @param sunPV
     *        the sun PV coordinates
     * @return [&beta;,&phi;]
     * @throws PatriusException
     *         if sun position cannot be computed raised if beta computation fails
     */
    protected double[] computeSunBetaPhi(final StelaEquinoctialOrbit orbit,
                                         final PVCoordinates sunPV) throws PatriusException {
        // Initializations
        // Compute sun position normalized
        final Vector3D sunP = sunPV.getPosition();
        final double n = sunP.getNorm();
        final double xSun = MathLib.divide(sunP.getX(), n);
        final double ySun = MathLib.divide(sunP.getY(), n);
        final double zSun = MathLib.divide(sunP.getZ(), n);
        final double delta = MathLib.asin(MathLib.min(1.0, MathLib.max(-1.0, zSun)));
        final double cosDelta = MathLib.cos(delta);
        final double alpha = MathLib.atan2(ySun, xSun);
        final double i = orbit.getI();
        // Compute traditional keplerian orbital parameters
        final double raan = MathLib.atan2(orbit.getIy(), orbit.getIx());
        // Declaration of the outputs
        final double beta;
        final double phi;
        // Special case if the orbit is strictly equatorial
        if (i == 0) {
            beta = delta;
            phi = alpha;
        } else {
            // General case
            final double sinI = MathLib.sin(i);
            final double cosI = MathLib.cos(i);
            final double temp1 = cosDelta * MathLib.sin(raan - alpha);
            final double temp2 = MathLib.cos(alpha - raan);
            beta = MathLib.asin(MathLib.min(1.0, MathLib.max(-1.0, temp1 * sinI + zSun * cosI)));
            phi = MathLib.atan2(-temp1 * cosI + zSun * sinI, cosDelta * temp2);
        }
        return new double[] { beta, phi };
    }

    /** {@inheritDoc} */
    @Override
    public double[] computeShortPeriods(final StelaEquinoctialOrbit orbit) throws PatriusException {
        // Not implemented yet:
        return new double[6];
    }

    /** {@inheritDoc} */
    @Override
    public double[][] computePartialDerivatives(final StelaEquinoctialOrbit orbit) throws PatriusException {
        // Not implemented yet:
        return new double[6][6];
    }
}
