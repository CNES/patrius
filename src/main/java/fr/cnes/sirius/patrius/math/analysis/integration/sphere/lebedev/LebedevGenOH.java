/**
 * Copyright 2011-2017 CNES
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
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1490:26/04/2018: major change to Coppola architecture
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.integration.sphere.lebedev;

import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Static class used to generates points under OH symmetry.</br> </br> Routines adapted from
 * C++.</br> Original routines written by Vyacheslav Lebedev and Dmitri Laikov under the <a
 * href="http://people.sc.fsu.edu/~jburkardt/txt/gnu_lgpl.txt">GNU LGPL license</a>.</br> (see <a
 * href="http://people.sc.fsu.edu/~jburkardt/cpp_src/sphere_lebedev_rule/sphere_lebedev_rule.html">
 * sphere_lebedev_rule.html</a>)</br> </br> <b>Reference:</b></br> Vyacheslav Lebedev, Dmitri
 * Laikov,</br> <i>A quadrature formula for the sphere of the 131st algebraic order of
 * accuracy</i>,</br> Russian Academy of Sciences Doklady Mathematics,</br> Volume 59, Number 3,
 * 1999, pages 477-481.</br> </br>
 *
 * @author seimanp
 *
 * @since 4.0
 *
 * @version $Id: SimpsonIntegrator.java 18108 2017-10-04 06:45:27Z bignon $
 */
final class LebedevGenOH {

    /**
     * Utility class.<br>
     * This private constructor avoid the creation of new instances.
     */
    private LebedevGenOH() {
        // Do nothing.
    }

    /**
     * Generates points under OH symmetry.</br> </br> <b>Reference:</b></br> Vyacheslav Lebedev,
     * Dmitri Laikov,</br> <i>A quadrature formula for the sphere of the 131st algebraic order of
     * accuracy</i>,</br> Russian Academy of Sciences Doklady Mathematics,</br> Volume 59, Number 3,
     * 1999, pages 477-481.</br> </br> <b>Discussion:</b></br> Given a point on a sphere, specified
     * by A and B, this routine generates all the equivalent points under OH symmetry, making grid
     * points with weight V.</br> </br> The variable NUM is increased by the number of different
     * points generated.</br> </br> Depending on CODE, there are from 6 to 48 different but
     * equivalent points that are generated:</br> </br> CODE=1: (0,0,1) etc ( 6 points)</br> CODE=2:
     * (0,A,A) etc, A=1/sqrt(2) (12 points)</br> CODE=3: (A,A,A) etc, A=1/sqrt(3) ( 8 points)</br>
     * CODE=4: (A,A,B) etc, B=sqrt(1-2 A^2) (24 points)</br> CODE=5: (A,B,0) etc, B=sqrt(1-A^2), A
     * input (24 points)</br> CODE=6: (A,B,C) etc, C=sqrt(1-A^2-B^2), A, B input (48 points)</br>
     * </br> <b>Original author:</b></br> Dmitri Laikov (11 September 2010)</br>
     *
     * @param code selects the symmetry group.
     * @param a information that may be needed to generate the coordinates of the points (for code =
     *        4, 5, 6 only).
     * @param b information that may be needed to generate the coordinates of the points (for code =
     *        6 only).
     * @param w the weight to be assigned the points.
     *
     * @return the number of points generated on this call.
     *
     * @author seimanp
     */
    public static List<LebedevGridPoint> genOH(final int code, final double a, final double b, final double w) {
        // Initialization
        List<LebedevGridPoint> res = null;

        // Build list of points depending on code
        switch (code) {
            case 1:
                res = genOHCODE01(w);
                break;
            case 2:
                res = genOHCODE02(w);
                break;
            case 3:
                res = genOHCODE03(w);
                break;
            case 4:
                res = genOHCODE04(a, w);
                break;
            case 5:
                res = genOHCODE05(a, w);
                break;
            case 6:
                res = genOHCODE06(a, b, w);
                break;
            default:
                // Invalid code
                throw new IllegalStateException("Illegal value of CODE");
        }

        // Return result
        return res;
    }

    /**
     * Functions generating the grid points for a given code.
     *
     * @param w w parameter
     * @return grid points
     */
    private static List<LebedevGridPoint> genOHCODE01(final double w) {
        // Initialization
        // 6 points
        final List<LebedevGridPoint> list = new ArrayList<LebedevGridPoint>(6);

        // Constant
        final double a = 1.0;

        // Add points
        list.add(new LebedevGridPoint(a, 0, 0, w));
        list.add(new LebedevGridPoint(-a, 0, 0, w));
        list.add(new LebedevGridPoint(0, a, 0, w));
        list.add(new LebedevGridPoint(0, -a, 0, w));
        list.add(new LebedevGridPoint(0, 0, a, w));
        list.add(new LebedevGridPoint(0, 0, -a, w));

        // Return list
        return list;
    }

    /**
     * Functions generating the grid points for a given code.
     *
     * @param w w parameter
     * @return grid points
     */
    private static List<LebedevGridPoint> genOHCODE02(final double w) {
        // Initialization
        // 12 points
        final List<LebedevGridPoint> list = new ArrayList<LebedevGridPoint>(12);

        // Sqrt(0.5)
        final double a = MathLib.sqrt(0.5);

        // Add points
        list.add(new LebedevGridPoint(0, a, a, w));
        list.add(new LebedevGridPoint(0, -a, a, w));
        list.add(new LebedevGridPoint(0, a, -a, w));
        list.add(new LebedevGridPoint(0, -a, -a, w));
        list.add(new LebedevGridPoint(a, 0, a, w));
        list.add(new LebedevGridPoint(-a, 0, a, w));
        list.add(new LebedevGridPoint(a, 0, -a, w));
        list.add(new LebedevGridPoint(-a, 0, -a, w));
        list.add(new LebedevGridPoint(a, a, 0, w));
        list.add(new LebedevGridPoint(-a, a, 0, w));
        list.add(new LebedevGridPoint(a, -a, 0, w));
        list.add(new LebedevGridPoint(-a, -a, 0, w));

        // Return list
        return list;
    }

    /**
     * Functions generating the grid points for a given code.
     *
     * @param w w parameter
     * @return grid points
     */
    private static List<LebedevGridPoint> genOHCODE03(final double w) {
        // Initialization
        // 8 points
        final List<LebedevGridPoint> list = new ArrayList<LebedevGridPoint>(8);

        // Sqrt(1 / 3)
        final double a = MathLib.sqrt(1.0 / 3.0);

        // Add points
        list.add(new LebedevGridPoint(a, a, a, w));
        list.add(new LebedevGridPoint(-a, a, a, w));
        list.add(new LebedevGridPoint(a, -a, a, w));
        list.add(new LebedevGridPoint(-a, -a, a, w));
        list.add(new LebedevGridPoint(a, a, -a, w));
        list.add(new LebedevGridPoint(-a, a, -a, w));
        list.add(new LebedevGridPoint(a, -a, -a, w));
        list.add(new LebedevGridPoint(-a, -a, -a, w));

        // Return list
        return list;
    }

    /**
     * Functions generating the grid points for a given code.
     *
     * @param a a parameter
     * @param w w parameter
     * @return grid points
     */
    private static List<LebedevGridPoint> genOHCODE04(final double a, final double w) {
        // Initialization
        // 24 points
        final List<LebedevGridPoint> list = new ArrayList<LebedevGridPoint>(24);

        // Constant
        final double b = MathLib.sqrt(1.0 - 2.0 * a * a);

        // Add points
        list.add(new LebedevGridPoint(a, a, b, w));
        list.add(new LebedevGridPoint(-a, a, b, w));
        list.add(new LebedevGridPoint(a, -a, b, w));
        list.add(new LebedevGridPoint(-a, -a, b, w));
        list.add(new LebedevGridPoint(a, a, -b, w));
        list.add(new LebedevGridPoint(-a, a, -b, w));
        list.add(new LebedevGridPoint(a, -a, -b, w));
        list.add(new LebedevGridPoint(-a, -a, -b, w));
        //
        list.add(new LebedevGridPoint(a, b, a, w));
        list.add(new LebedevGridPoint(-a, b, a, w));
        list.add(new LebedevGridPoint(a, -b, a, w));
        list.add(new LebedevGridPoint(-a, -b, a, w));
        list.add(new LebedevGridPoint(a, b, -a, w));
        list.add(new LebedevGridPoint(-a, b, -a, w));
        list.add(new LebedevGridPoint(a, -b, -a, w));
        list.add(new LebedevGridPoint(-a, -b, -a, w));
        //
        list.add(new LebedevGridPoint(b, a, a, w));
        list.add(new LebedevGridPoint(-b, a, a, w));
        list.add(new LebedevGridPoint(b, -a, a, w));
        list.add(new LebedevGridPoint(-b, -a, a, w));
        list.add(new LebedevGridPoint(b, a, -a, w));
        list.add(new LebedevGridPoint(-b, a, -a, w));
        list.add(new LebedevGridPoint(b, -a, -a, w));
        list.add(new LebedevGridPoint(-b, -a, -a, w));

        // Return list
        //
        return list;
    }

    /**
     * Functions generating the grid points for a given code.
     *
     * @param a a parameter
     * @param w w parameter
     * @return grid points
     */
    private static List<LebedevGridPoint> genOHCODE05(final double a, final double w) {
        // Initialization
        // 24 points
        final List<LebedevGridPoint> list = new ArrayList<LebedevGridPoint>(24);

        // Constant
        final double b = MathLib.sqrt(1.0 - a * a);

        // Add points
        list.add(new LebedevGridPoint(a, b, 0, w));
        list.add(new LebedevGridPoint(-a, b, 0, w));
        list.add(new LebedevGridPoint(a, -b, 0, w));
        list.add(new LebedevGridPoint(-a, -b, 0, w));
        //
        list.add(new LebedevGridPoint(b, a, 0, w));
        list.add(new LebedevGridPoint(-b, a, 0, w));
        list.add(new LebedevGridPoint(b, -a, 0, w));
        list.add(new LebedevGridPoint(-b, -a, 0, w));
        //
        list.add(new LebedevGridPoint(a, 0, b, w));
        list.add(new LebedevGridPoint(-a, 0, b, w));
        list.add(new LebedevGridPoint(a, 0, -b, w));
        list.add(new LebedevGridPoint(-a, 0, -b, w));
        //
        list.add(new LebedevGridPoint(b, 0, a, w));
        list.add(new LebedevGridPoint(-b, 0, a, w));
        list.add(new LebedevGridPoint(b, 0, -a, w));
        list.add(new LebedevGridPoint(-b, 0, -a, w));
        //
        list.add(new LebedevGridPoint(0, a, b, w));
        list.add(new LebedevGridPoint(0, -a, b, w));
        list.add(new LebedevGridPoint(0, a, -b, w));
        list.add(new LebedevGridPoint(0, -a, -b, w));
        //
        list.add(new LebedevGridPoint(0, b, a, w));
        list.add(new LebedevGridPoint(0, -b, a, w));
        list.add(new LebedevGridPoint(0, b, -a, w));
        list.add(new LebedevGridPoint(0, -b, -a, w));

        // Return list
        return list;
    }

    /**
     * Functions generating the grid points for a given code.
     *
     * @param a a parameter
     * @param b b parameter
     * @param w w parameter
     * @return grid points
     */
    private static List<LebedevGridPoint> genOHCODE06(final double a, final double b, final double w) {
        // Initialization
        // 48 points
        final List<LebedevGridPoint> list = new ArrayList<LebedevGridPoint>(48);

        // Constant
        final double c = MathLib.sqrt(1.0 - a * a - b * b);

        // Add points
        list.add(new LebedevGridPoint(a, b, c, w));
        list.add(new LebedevGridPoint(-a, b, c, w));
        list.add(new LebedevGridPoint(a, -b, c, w));
        list.add(new LebedevGridPoint(-a, -b, c, w));
        list.add(new LebedevGridPoint(a, b, -c, w));
        list.add(new LebedevGridPoint(-a, b, -c, w));
        list.add(new LebedevGridPoint(a, -b, -c, w));
        list.add(new LebedevGridPoint(-a, -b, -c, w));
        //
        list.add(new LebedevGridPoint(a, c, b, w));
        list.add(new LebedevGridPoint(-a, c, b, w));
        list.add(new LebedevGridPoint(a, -c, b, w));
        list.add(new LebedevGridPoint(-a, -c, b, w));
        list.add(new LebedevGridPoint(a, c, -b, w));
        list.add(new LebedevGridPoint(-a, c, -b, w));
        list.add(new LebedevGridPoint(a, -c, -b, w));
        list.add(new LebedevGridPoint(-a, -c, -b, w));
        //
        list.add(new LebedevGridPoint(b, a, c, w));
        list.add(new LebedevGridPoint(-b, a, c, w));
        list.add(new LebedevGridPoint(b, -a, c, w));
        list.add(new LebedevGridPoint(-b, -a, c, w));
        list.add(new LebedevGridPoint(b, a, -c, w));
        list.add(new LebedevGridPoint(-b, a, -c, w));
        list.add(new LebedevGridPoint(b, -a, -c, w));
        list.add(new LebedevGridPoint(-b, -a, -c, w));
        //
        list.add(new LebedevGridPoint(b, c, a, w));
        list.add(new LebedevGridPoint(-b, c, a, w));
        list.add(new LebedevGridPoint(b, -c, a, w));
        list.add(new LebedevGridPoint(-b, -c, a, w));
        list.add(new LebedevGridPoint(b, c, -a, w));
        list.add(new LebedevGridPoint(-b, c, -a, w));
        list.add(new LebedevGridPoint(b, -c, -a, w));
        list.add(new LebedevGridPoint(-b, -c, -a, w));
        //
        list.add(new LebedevGridPoint(c, a, b, w));
        list.add(new LebedevGridPoint(-c, a, b, w));
        list.add(new LebedevGridPoint(c, -a, b, w));
        list.add(new LebedevGridPoint(-c, -a, b, w));
        list.add(new LebedevGridPoint(c, a, -b, w));
        list.add(new LebedevGridPoint(-c, a, -b, w));
        list.add(new LebedevGridPoint(c, -a, -b, w));
        list.add(new LebedevGridPoint(-c, -a, -b, w));
        //
        list.add(new LebedevGridPoint(c, b, a, w));
        list.add(new LebedevGridPoint(-c, b, a, w));
        list.add(new LebedevGridPoint(c, -b, a, w));
        list.add(new LebedevGridPoint(-c, -b, a, w));
        list.add(new LebedevGridPoint(c, b, -a, w));
        list.add(new LebedevGridPoint(-c, b, -a, w));
        list.add(new LebedevGridPoint(c, -b, -a, w));
        list.add(new LebedevGridPoint(-c, -b, -a, w));

        // Return list
        //
        return list;
    }
}
