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
 * @history Created 18/07/2012
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:---:11/04/2014:Quality assurance
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity.tides;

import java.util.TreeMap;

//QA exemption : Checkstyle disabled for the file
//CHECKSTYLE: stop MagicNumber

/**
 * Known ocean tides waves and heights
 * 
 * @concurrency immutable
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: OceanTidesWaves.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.2
 */
@SuppressWarnings("PMD.LooseCoupling")
final class OceanTidesWaves {

    /**
     * Wave heights data - main
     */
    private static final TreeMap<Double, Double> HEIGHTS_P = new TreeMap<>();
    /**
     * Wave heights data - secondary
     */
    private static final TreeMap<Double, Double> HEIGHTS_S = new TreeMap<>();
    /**
     * Wave names data - main
     */
    private static final TreeMap<String, Double> NAMES_P = new TreeMap<>();

    /**
     * Private constructor
     */
    private OceanTidesWaves() {

    }

    /**
     * Static initialization
     */
    static {
        initMainHeights();
        initSecondaryHeights();
        initMainNames();
    }

    /**
     * Get registered heights
     * 
     * @return treemap with doodson number ans heights
     */
    public static TreeMap<Double, Double> getSecondaryHeights() {
        return (TreeMap<Double, Double>) HEIGHTS_S.clone();
    }

    /**
     * Get registered heights
     * 
     * @return treemap with doodson number ans heights
     */
    public static TreeMap<String, Double> getMainNames() {
        return (TreeMap<String, Double>) NAMES_P.clone();
    }

    /**
     * Get registered heights
     * 
     * @return treemap with doodson number ans heights
     */
    public static TreeMap<Double, Double> getMainHeights() {
        return (TreeMap<Double, Double>) HEIGHTS_P.clone();
    }

    /**
     * Initialize TreeMap with know ocean waves names and numbers
     */
    private static void initMainNames() {
        // initialize main wave names
        NAMES_P.put("Omega1", 55.565);
        NAMES_P.put("Sa", 56.554);
        NAMES_P.put("Ssa", 57.555);
        NAMES_P.put("Mm", 65.455);
        // initialize main wave names
        NAMES_P.put("Mf", 75.555);
        NAMES_P.put("Mtm", 85.455);
        NAMES_P.put("Msqm", 93.555);
        NAMES_P.put("Q1", 135.655);
        // initialize main wave names
        NAMES_P.put("O1", 145.555);
        NAMES_P.put("K1", 165.555);
        NAMES_P.put("2N2", 235.755);
        // initialize main wave names
        NAMES_P.put("N2", 245.655);
        NAMES_P.put("M2", 255.555);
        NAMES_P.put("S2", 273.555);
        NAMES_P.put("K2", 275.555);
    }

    /**
     * Initialize TreeMap with know secondary ocean waves numbers and heights
     */
    // CHECKSTYLE: stop CommentRatio check
    private static void initSecondaryHeights() {
        // CHECKSTYLE: resume CommentRatio check
        final double[][] data = new double[][] {
            { 58.554, -0.00181 }, { 63.655, -0.00673 }, { 65.445, 0.00231 },
            { 65.465, 0.00229 }, { 65.555, -0.00375 }, { 65.655, 0.00188 }, { 73.555, -0.00583 },
            { 75.355, -0.00288 }, { 75.565, -0.02762 }, { 75.575, -0.00258 }, { 83.655, -0.00242 },
            { 83.665, -0.0010 }, { 85.455, -0.01276 }, { 85.465, -0.00529 }, { 93.555, -0.00204 },
            { 95.355, -0.00169 }, { 117.655, -0.00194 }, { 125.755, -0.00664 }, { 127.555, -0.00802 },
            { 135.645, -0.00947 }, { 137.445, -0.0018 }, { 137.455, -0.00954 }, { 145.545, -0.04946 },
            { 145.755, 0.0017 }, { 147.555, 0.00343 }, { 153.655, 0.00194 }, { 155.455, 0.00741 },
            { 155.555, -0.00399 }, { 155.655, 0.02062 }, { 155.665, 0.00414 }, { 157.455, 0.00394 },
            { 162.556, -0.00714 }, { 163.555, -0.12203 }, { 164.556, 0.00289 }, { 165.545, -0.0073 },
            { 165.565, 0.05001 }, { 166.554, 0.00293 }, { 167.555, 0.00525 }, { 173.655, 0.00395 },
            { 175.455, 0.02062 }, { 175.465, 0.00409 }, { 183.555, 0.00342 }, { 185.355, 0.00169 },
            { 185.555, 0.01129 }, { 185.565, 0.00723 }, { 195.455, 0.00216 }, { 225.855, 0.0018 },
            { 227.655, 0.00467 }, { 235.755, 0.01601 }, { 237.555, 0.01932 }, { 245.555, -0.00389 },
            { 245.645, -0.00451 }, { 247.455, 0.02298 }, { 253.755, -0.0019 }, { 254.556, -0.00218 },
            { 255.545, -0.02358 }, { 256.554, 0.00192 }, { 263.655, -0.00466 }, { 265.455, -0.01786 },
            { 265.555, 0.00359 }, { 265.655, 0.00447 }, { 265.665, 0.00197 }, { 272.556, 0.0172 },
            { 274.554, -0.00246 }, { 275.565, 0.02383 }, { 275.575, 0.00259 }, { 285.455, 0.00447 },
            { 285.465, 0.00195 } };

        for (final double[] col : data) {
            HEIGHTS_S.put(col[0], col[1]);
        }
    }

    /**
     * Initialize TreeMap with know primary ocean waves numbers and heights
     */
    // CHECKSTYLE: stop CommentRatio check
    private static void initMainHeights() {
        // CHECKSTYLE: resume CommentRatio check
        final double[][] data = new double[][] { { 55.565, 0.02793 }, { 56.554, -0.00492 }, { 57.555, -0.031 },
            { 65.455, -0.03518 }, { 75.555, -0.06663 }, { 85.455, -0.01276 }, { 93.555, -0.00204 },
            { 135.655, -0.0502 }, { 145.555, -0.26221 }, { 165.555, 0.36878 }, { 235.755, 0.01601 },
            { 245.655, 0.12099 }, { 255.555, 0.63192 }, { 273.555, 0.294 }, { 275.555, 0.07996 } };

        for (final double[] col : data) {
            HEIGHTS_P.put(col[0], col[1]);
        }
    }

}
