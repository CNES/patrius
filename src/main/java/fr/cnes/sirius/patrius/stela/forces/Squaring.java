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
 * @history created 18/02/2013
 *
 * HISTORY
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11:DM:DM-3287:22/05/2023:[PATRIUS] Courtes periodes traînee atmospherique et prs
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:317:02/03/2015: STELA integration in CIRF with referential choice (ICRF, CIRF or MOD)
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.forces;

import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.KeplerianParameters;
import fr.cnes.sirius.patrius.stela.JavaMathAdapter;
import fr.cnes.sirius.patrius.stela.orbits.StelaEquinoctialOrbit;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Class containing methods used to compute Simpson's quadrature.
 * 
 * @concurrency immutable
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.3
 * 
 */
public final class Squaring {

    /**
     * CNES Julian dates of points for squaring.
     */
    private static AbsoluteDate[] squaringJDCNES;
    /**
     * value of 2*PI.
     */
    private static final double TWO_PI = 2.0 * FastMath.PI;
    /**
     * value of PI.
     */
    private static final double PI = FastMath.PI;

    /**
     * Constructor.
     */
    private Squaring() {
    }

    /**
     * Simpson's rule.
     * 
     * @param y
     *        function values
     * @return simpson's mean of y.
     * @throws PatriusException
     *         exception raised if number of intervals
     *         for Simpson's Rule is not even.
     */
    public static double simpsonMean(final double[] y) throws PatriusException {

        // Number of intervals:
        final int iNum = y.length - 1;
        // The number of intervals is even:
        if ((iNum % 2) == 0) {
            double sum1 = 0.0;
            double sum2 = y[iNum - 1];
            for (int i = 1; i <= (iNum / 2) - 1; i++) {
                sum1 += y[2 * i];
                sum2 += y[2 * i - 1];
            }
            return ((y[0] + 2.0 * sum1 + 4.0 * sum2 + y[iNum]) / (iNum * 3.0));
        } else {
            // The Simpson rule computation failed:
            throw new PatriusException(PatriusMessages.PDB_SIMPSON_RULE_FAILED);
        }
    }

    /**
     * Simpson's rule when the integration is not done on the entire orbit, but only on one specific part.
     * 
     * @param y
     *        function values
     * @param deltaEi
     *        interval length in eccentric anomaly
     * @return simpson's mean of y.
     * @throws PatriusException
     *         exception raised if number of intervals for Simpson's Rule is not even.
     */
    public static double simpsonMean(final double[] y, final double deltaEi) throws PatriusException {
        // Number of intervals:
        final int iNum = y.length - 1;
        // The number of intervals is even.
        if ((iNum % 2) == 0) {
            double sum1 = 0.0;
            double sum2 = y[iNum - 1];
            for (int i = 1; i <= (iNum / 2) - 1; i++) {
                sum1 += y[2 * i];
                sum2 += y[2 * i - 1];
            }
            return ((y[0] + 2.0 * sum1 + 4.0 * sum2 + y[iNum]) * (deltaEi / (3.0 * TWO_PI)));
        } else {
            // The Simpson rule computation failed:
            throw new PatriusException(PatriusMessages.PDB_SIMPSON_RULE_FAILED);
        }
    }

    /**
     * Computation of squaring points equally distributed according to true anomaly.
     * 
     * @param numPoints
     *        number of squaring points
     * @param orbit
     *        an orbit
     * @param startPoint
     *        starting true anomaly of the considered part of the orbit
     * @param endPoint
     *        ending true anomaly of the considered part of the orbit
     * @return squaring points
     * @throws PatriusException
     *         thrown if number of points is not odd
     */
    public static double[][] computeSquaringPoints(final int numPoints,
            final StelaEquinoctialOrbit orbit,
            final double startPoint,
            final double endPoint) throws PatriusException {

        double[][] squaringPoints = null;
        final int iNum = numPoints - 1;
        if ((iNum % 2) == 0) {
            squaringPoints = new double[numPoints][6];
            final double a = orbit.getA();
            final double ex = orbit.getEquinoctialEx();
            final double ey = orbit.getEquinoctialEy();
            final double lambdaEq = orbit.getLM();
            final double n = MathLib.sqrt(orbit.getMu() / (a * a * a));
            final double e2 = ex * ex + ey * ey;
            final double e = MathLib.sqrt(e2);
            final double eta = MathLib.sqrt(1.0 - e2);
            final double pomPlusRaan = JavaMathAdapter.mod(MathLib.atan2(ey, ex), TWO_PI);
            final double lamEq = JavaMathAdapter.mod(lambdaEq, TWO_PI);
            // Mean anomaly:
            final double[] m = new double[numPoints];
            // Eccentric anomaly:
            double u;
            // True anomaly:
            double nu;
            squaringJDCNES = new AbsoluteDate[numPoints];
            final AbsoluteDate cnesJD = orbit.getDate();
            // Average on a part of the orbit:
            final double dNuGTO = MathLib.divide(endPoint - startPoint, iNum);
            final double anomPass = radDiff(lamEq, pomPlusRaan);
            final double perigeeTime = MathLib.divide(-anomPass, n);

            // Loop on every point of the quadrature
            for (int k = 0; k <= iNum; k++) {
                // True anomaly:
                nu = startPoint + k * dNuGTO;
                final double[] sincosNu = MathLib.sinAndCos(nu);
                // Eccentric anomaly:
                u = MathLib.atan2((sincosNu[0] * eta), (e + sincosNu[1]));
                // Mean anomaly:
                m[k] = u - e * MathLib.sin(u);
                if (endPoint == TWO_PI) {
                    m[k] = JavaMathAdapter.mod(m[k], TWO_PI);
                    m[iNum] = TWO_PI;
                }
                squaringPoints[k] = orbit.mapOrbitToArray();
                squaringPoints[k][1] = JavaMathAdapter.mod(pomPlusRaan + m[k], TWO_PI);
                squaringJDCNES[k] = cnesJD.shiftedBy(perigeeTime + MathLib.divide(m[k], n));
            }
        } else {
            // The squaring computation failed:
            throw new PatriusException(PatriusMessages.PDB_SQUARING_FAILED);
        }
        return squaringPoints;
    }

    /**
     * Returns (a-b) in -pi:+pi range.
     * 
     * @param a
     *        first coeficient
     * @param b
     *        second coeficient
     * @return diff
     */
    private static double radDiff(final double a, final double b) {
        double diff;
        // normalisation in 0:2pi
        diff = JavaMathAdapter.mod(a - b, TWO_PI);
        // difference in -pi:+pi
        diff = (diff > PI ? diff - TWO_PI : diff);

        return diff;
    }

    /**
     * @return the squaringJDCNES
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public static AbsoluteDate[] getSquaringJDCNES() {
        return squaringJDCNES;
    }

    /**
     * Computation of squaring points equally distributed according to eccentric anomaly.
     * 
     * @param numPoints
     *        number of squaring points
     * @param orbit
     *        an orbit
     * @return squaring points
     * @throws PatriusException
     *         thrown if number of points is not odd
     */
    public static StelaEquinoctialOrbit[] computeSquaringPointsEccentric(final int numPoints,
            final StelaEquinoctialOrbit orbit) throws PatriusException {

        final int iNum = numPoints - 1;
        if ((iNum % 2) == 0) {

            // Initialization
            final StelaEquinoctialOrbit[] squaringPoints = new StelaEquinoctialOrbit[numPoints];

            final double a = orbit.getA();
            final double ex = orbit.getEquinoctialEx();
            final double ey = orbit.getEquinoctialEy();
            final double ix = orbit.getIx();
            final double iy = orbit.getIy();
            final double lambdaEq = orbit.getLM();
            final double n = MathLib.sqrt(orbit.getMu() / (a * a * a));
            final double e2 = ex * ex + ey * ey;
            final double e = MathLib.sqrt(e2);
            final double pomPlusRaan = JavaMathAdapter.mod(MathLib.atan2(ey, ex), 2. * FastMath.PI);
            final double lamEq = JavaMathAdapter.mod(lambdaEq, 2. * FastMath.PI);
            final double anomPass = radDiff(lamEq, pomPlusRaan);
            final AbsoluteDate perigeeTime = orbit.getDate().shiftedBy(-anomPass / (n * Constants.JULIAN_DAY));

            // Variation of eccentric anomaly between 2 points
            final double du = MathLib.divide(2. * FastMath.PI, iNum);

            // Loop on every point of the quadrature
            for (int k = 0; k <= iNum; k++) {

                // Eccentric anomaly
                final double u = k * du;

                // Mean anomaly
                double m = u - e * MathLib.sin(u);
                m = JavaMathAdapter.mod(m, 2. * FastMath.PI);

                if (k == iNum) {
                    m = 2. * FastMath.PI;
                }

                // Build bulletin
                final AbsoluteDate datek = perigeeTime.shiftedBy(m / (n * Constants.JULIAN_DAY));
                final double lmk = JavaMathAdapter.mod(pomPlusRaan + m, 2. * FastMath.PI);
                squaringPoints[k] = new StelaEquinoctialOrbit(a, ex, ey, ix, iy, lmk, orbit.getFrame(), datek,
                    orbit.getMu());
            }

            return squaringPoints;

        } else {
            // Number of squaring points must be odd
            throw new PatriusException(PatriusMessages.EVEN_SQUARING_POINTS);
        }
    }

    /**
     * Computation of squaring points equally distributed according to eccentric anomaly between start point and end
     * point.
     * 
     * @param numPoints
     *        number of squaring points
     * @param orbit
     *        an orbit
     * @param startPoint
     *        starting true anomaly of the considered part of the orbit
     * @param endPoint
     *        ending true anomaly of the considered part of the orbit
     * @return squaring points {date, eccentric anomaly, mean anomaly}
     * @throws PatriusException
     *         thrown if number of points is not odd
     */
    public static List<double[]> computeSquaringPointsEccentric2(final int numPoints,
            final StelaEquinoctialOrbit orbit,
            final double startPoint,
            final double endPoint) throws PatriusException {

        final int iNum = numPoints - 1;
        if ((iNum % 2) == 0) {
            // SMA, Eccentricity
            final KeplerianParameters kepParam = orbit.getParameters().getKeplerianParameters();
            final double a = orbit.getA();
            final double e = orbit.getE();

            // Mean motion
            final double n = MathLib.sqrt(orbit.getMu() / (a * a * a));

            // Quadrature points repartition
            final double deltaEi = (endPoint - startPoint) / (numPoints - 1);

            // Center of interval
            final double m1 = startPoint - e * MathLib.sin(startPoint);
            final double mn = endPoint - e * MathLib.sin(endPoint);
            final double mi = (m1 + mn) / 2.;

            // Mean anomaly
            final double m0 = JavaMathAdapter.roundAngleInRadians(kepParam.getMeanAnomaly());
            double m0bis = m0;
            if (m0 < mi - MathLib.PI) {
                m0bis += 2. * MathLib.PI;
            }
            if (m0 > mi + 2. * MathLib.PI) {
                m0bis -= 2. * MathLib.PI;
            }

            // Build list of [date, mean anomalies, eccentric anomalies]
            final List<double[]> list = new ArrayList<double[]>();
            for (int i = 0; i <= numPoints - 1; i++) {
                final double eccAnomaly = startPoint + i * deltaEi;
                final double meanAnomaly = eccAnomaly - e * MathLib.sin(eccAnomaly);
                final double dt = (meanAnomaly - m0bis) / n;
                list.add(new double[] { dt, eccAnomaly, meanAnomaly });
            }
            return list;

        } else {
            // Number of squaring points must be odd
            throw new PatriusException(PatriusMessages.EVEN_SQUARING_POINTS);
        }
    }

}
