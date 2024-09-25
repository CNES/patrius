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
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1490:26/04/2018: major change to Coppola architecture
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.integration.sphere.lebedev;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Defines a Lebedev rule's grid.
 * <p>
 * Routines adapted from C++.</br> Original routines written by Vyacheslav Lebedev, Dmitri Laikov and John Burkardt
 * under the <a href="http://people.sc.fsu.edu/~jburkardt/txt/gnu_lgpl.txt">GNU LGPL license</a>.</br> (see <a href=
 * "http://people.sc.fsu.edu/~jburkardt/cpp_src/sphere_lebedev_rule/sphere_lebedev_rule.html"
 * >sphere_lebedev_rule.html</a>)</br>
 * </p>
 * <p>
 * <b>Reference:</b></br> Vyacheslav Lebedev, Dmitri Laikov,</br> <i>A quadrature formula for the sphere of the 131st
 * algebraic order of accuracy</i>,</br> Russian Academy of Sciences Doklady Mathematics,</br> Volume 59, Number 3,
 * 1999, pages 477-481.</br>
 * </p>
 *
 * @author GMV
 * 
 * @since 4.0
 *
 * @version $Id: SimpsonIntegrator.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class LebedevGrid {

    /** List of grid points (weights and coordinates) */
    private final List<LebedevGridPoint> points;

    /**
     * Build a new <code>LebedevGrid</code> instance from the specified list of grid points.
     *
     * @param pointsIn a list of grid points
     */
    public LebedevGrid(final List<LebedevGridPoint> pointsIn) {
        this.points = Collections.unmodifiableList(pointsIn);
    }

    /**
     * Build a new <code>LebedevGrid</code> instance existing instance.
     *
     * @param grid a Lebedev grid
     */
    public LebedevGrid(final LebedevGrid grid) {
        this.points = grid.points;
    }

    /**
     * Returns the grid points of the given grid that are also contained in the current grid.
     *
     * @param grid a Lebedev grid
     * @param absolutePrecision the absolute Cartesian distance above which two points are
     *        considered different
     *
     * @return the list of duplicates Lebedev grid points
     */
    public List<LebedevGridPoint> getDuplicates(final LebedevGrid grid,
                                                final double absolutePrecision) {
        return this.getDuplicates(grid.getPoints(), absolutePrecision);
    }

    /**
     * Returns the grid points of the given list that are also contained in the current grid.
     *
     * @param pointsIn a list of Lebedev grid points
     * @param absolutePrecision the absolute cartesian distance above which two points are
     *        considered to be different
     *
     * @return the list of duplicates Lebedev grid points
     */
    public List<LebedevGridPoint> getDuplicates(final List<LebedevGridPoint> pointsIn,
                                                final double absolutePrecision) {
        final List<LebedevGridPoint> duplicates = new ArrayList<>();

        for (final LebedevGridPoint p1 : this.points) {
            for (final LebedevGridPoint p2 : pointsIn) {
                if (p1.isSamePoint(p2, absolutePrecision)) {
                    duplicates.add(p1);
                }
            }
        }

        return duplicates;
    }

    /**
     * Returns the sum of the weights of all the grid points.
     * <p>
     * It should always be equal to one.
     * </p>
     *
     * @return the sum of grid points' weights
     */
    public double getTotalWeight() {
        double sum = 0.0;

        for (final LebedevGridPoint point : this.points) {
            sum += point.getWeight();
        }

        return sum;
    }

    /**
     * Gets the list of grid points.
     *
     * @return the list of grid points
     */
    public List<LebedevGridPoint> getPoints() {
        return this.points;
    }

    /**
     * Gets the number of grid points.
     *
     * @return the number of grid points
     */
    public int getSize() {
        return this.points.size();
    }
}
