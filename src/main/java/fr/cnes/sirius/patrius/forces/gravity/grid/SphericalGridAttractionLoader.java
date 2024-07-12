/**
 * Copyright 2011-2020 CNES
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
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration gestion attractions gravitationnelles
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3113:10/05/2022:[PATRIUS] Probleme avec le modele de gravite sous forme de grille spherique 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2859:18/05/2021:Optimisation du code ; SpacecraftState 
 * VERSION:4.7:DM:DM-2861:18/05/2021:Optimisation du calcul des derivees partielles de EmpiricalForce 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity.grid;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.SphericalCoordinates;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;


/**
 * Grid attraction model loader.
 * This loader only reads attraction model files defined with a spherical grid (altitude, longitude, latitude).
 * <p>Read data is considered to be in km and degrees.</p>
 *
 * @author Emmanuel Bignon
 *
 * @since 4.7
 */
@SuppressWarnings("PMD.MethodReturnsInternalArray")
// Reason: performances
public class SphericalGridAttractionLoader implements GridAttractionProvider {

    /** m to km conversion. */
    private static final int M_TO_KM = 1000;

    /** Column separation. */
    private static final String SPLIT = "[ ]+";

    /** Attraction data stored after reading file. */
    private AttractionData data;

    /**
     * Constructor.
     *
     * @param modelFileName
     *        grid attraction model file name
     * @throws PatriusException thrown if loading failed
     */
    public SphericalGridAttractionLoader(final String modelFileName) throws PatriusException {
        // Load data from file
        loadData(modelFileName);
    }

    /**
     * Load model.
     * @param modelFileName model file name
     * @throws PatriusException thrown if load failed (data is inconsistent)
     */
    @SuppressWarnings({"PMD.AvoidCatchingGenericException", "PMD.AvoidCatchingNPE"})
    // Reason: code clarity
    private void loadData(final String modelFileName) throws PatriusException {

        try {
            // Set up reader for line-oriented file
            final InputStream fileReader = new FileInputStream(modelFileName);
            final InputStreamReader isr = new InputStreamReader(fileReader, StandardCharsets.UTF_8);
            final BufferedReader reader = new BufferedReader(isr);

            // First lines - Unused
            for (int i = 0; i < 4; i++) {
                reader.readLine();
            }
            
            // Center of mass
            reader.readLine();
            String line = reader.readLine();
            String[] components = line.split(SPLIT);
            final Vector3D centerOfMass = new Vector3D(Double.parseDouble(components[1]) * M_TO_KM,
                    Double.parseDouble(components[2]) * M_TO_KM, Double.parseDouble(components[3]) * M_TO_KM);
            
            // Gravitational constant
            reader.readLine();
            line = reader.readLine();
            final double gmkm = Double.parseDouble(line.trim());
            final double gm = gmkm * M_TO_KM * M_TO_KM * M_TO_KM;
            
            // Altitude min/max 
            reader.readLine();
            line = reader.readLine();
            components = line.split(SPLIT);
            final double[] altMinMax = { Double.parseDouble(components[1]) * M_TO_KM,
                    Double.parseDouble(components[2]) * M_TO_KM };

            // Longitude min/max
            reader.readLine();
            line = reader.readLine();
            components = line.split(SPLIT);
            final double[] lonMinMax = { MathLib.toRadians(Double.parseDouble(components[1])),
                    MathLib.toRadians(Double.parseDouble(components[2])) };

            // Latitude min/max
            reader.readLine();
            line = reader.readLine();
            components = line.split(SPLIT);
            final double[] latMinMax = { MathLib.toRadians(Double.parseDouble(components[1])),
                    MathLib.toRadians(Double.parseDouble(components[2])) };

            // Resolution
            reader.readLine();
            line = reader.readLine();
            components = line.split(SPLIT);
            final double[] resolution = { Double.parseDouble(components[1]) * M_TO_KM,
                    MathLib.toRadians(Double.parseDouble(components[2])),
                    MathLib.toRadians(Double.parseDouble(components[3])) };

            // Attraction data
            final List<AttractionDataPoint> pointsList = new ArrayList<>();
            line = reader.readLine();
            while (line != null) {
                // Skip comment
                if (!line.contains("#")) {
                    components = line.split(SPLIT);
                    // Construction of data
                    final double alt = Double.parseDouble(components[1]) * M_TO_KM;
                    final double lon = MathLib.toRadians(Double.parseDouble(components[2]));
                    final double lat = MathLib.toRadians(Double.parseDouble(components[3]));
                    final double ax = Double.parseDouble(components[4]) * gmkm * M_TO_KM;
                    final double ay = Double.parseDouble(components[5]) * gmkm * M_TO_KM;
                    final double az = Double.parseDouble(components[6]) * gmkm * M_TO_KM;
                    final double pot = Double.parseDouble(components[6 + 1]) * gm / M_TO_KM;
                    final AttractionDataPoint point = new AttractionDataPoint(new SphericalCoordinates(lat, lon, alt,
                            false), new Vector3D(ax, ay, az), pot);
                    pointsList.add(point);
                }

                // Update loop variables
                line = reader.readLine();
            }

            // Build final data container
            final AttractionDataPoint[] points = pointsList.toArray(new AttractionDataPoint[pointsList.size()]);
            final double[] gridMin = { altMinMax[0], lonMinMax[0], latMinMax[0] };
            final double[] gridMax = { altMinMax[1], lonMinMax[1], latMinMax[1] };
            final GridSystem grid = new SphericalGrid(gridMin, gridMax, resolution, points);
            this.data = new AttractionData(gm, centerOfMass, grid, points);

            // Close readers
            reader.close();
            isr.close();
            fileReader.close();

        } catch (final IOException | NullPointerException e) {
            // Failed to load data
            throw new PatriusException(e, PatriusMessages.FAILED_TO_LOAD_GRID_FILE, modelFileName);
        }
    }

    /** {@inheritDoc} */
    @Override
    public AttractionData getData() {
        return this.data;
    }

    /**
     * Grid system defined by spherical coordinates.
     * <p>
     * This class is to be used in conjunction with {@link GridGravityModel} for attraction force defined by a grid.
     * </p>
     *
     * @author Emmanuel Bignon
     *
     * @since 4.7
     */
    private static class SphericalGrid implements GridSystem {

         /** Serializable UID. */
        private static final long serialVersionUID = 5884862979973205122L;

        /** Grid min on 3 axis. */
        private final double[] min;

        /** Grid max on 3 axis. */
        private final double[] max;

        /** Altitude data array (values along first abscissa). */
        private final double[] xArray;

        /** Longitude data array (values along second abscissa). */
        private final double[] yArray;

        /** Latitude data array (values along third abscissa). */
        private final double[] zArray;

        /** X acceleration data array (values along ordinates). */
        private final double[][][] accXArray;

        /** Y acceleration data array (values along ordinates). */
        private final double[][][] accYArray;

        /** Z acceleration data array (values along ordinates). */
        private final double[][][] accZArray;

        /** Potential data array (values along ordinates). */
        private final double[][][] potentialArray;

        /**
         * Constructor.
         * @param min grid min on 3 axis
         * @param max grid max on 3 axis
         * @param resolution grid resolution on 3 axis
         * @param points data points
         */
        public SphericalGrid(final double[] min,
                final double[] max,
                final double[] resolution,
                final AttractionDataPoint[] points) {
            this.min = min;
            this.max = max;

            // Initialize arrays
            final int nbX = (int)((max[0] - min[0]) / resolution[0] + 1);
            final int nbY = (int)((max[1] - min[1]) / resolution[1] + 1);
            final int nbZ = (int)((max[2] - min[2]) / resolution[2] + 1);
            this.xArray = new double[nbX];
            this.yArray = new double[nbY];
            this.zArray = new double[nbZ];
            this.accXArray = new double[nbX][nbY][nbZ];
            this.accYArray = new double[nbX][nbY][nbZ];
            this.accZArray = new double[nbX][nbY][nbZ];
            this.potentialArray = new double[nbX][nbY][nbZ];
            // Build arrays
            // Altitude, longitude, latitude
            for (int i = 0; i < nbX; i++) {
                this.xArray[i] = min[0] + i * resolution[0];
            }
            for (int i = 0; i < nbY; i++) {
                this.yArray[i] = min[1] + i * resolution[1];
            }
            for (int i = 0; i < nbZ; i++) {
                this.zArray[i] = min[2] + i * resolution[2];
            }
            // Ordinates
            for (int i = 0; i < points.length; i++) {
                // Spherical coordinates
                final SphericalCoordinates coords = points[i].getSphericalCoordinates();
                // Indices in array
                final int ix = (int) MathLib.round((coords.getNorm() - min[0]) / resolution[0]);
                final int iy = (int) MathLib.round((coords.getAlpha() - min[1]) / resolution[1]);
                final int iz = (int) MathLib.round((coords.getDelta() - min[2]) / resolution[2]);
                // Fill-in array
                this.accXArray[ix][iy][iz] = points[i].getAcceleration().getX();
                this.accYArray[ix][iy][iz] = points[i].getAcceleration().getY();
                this.accZArray[ix][iy][iz] = points[i].getAcceleration().getZ();
                this.potentialArray[ix][iy][iz] = points[i].getPotential();
            }
        }

        /** {@inheritDoc} */
        @Override
        public boolean isInsideGrid(final Vector3D position) {
            final SphericalCoordinates coords = new SphericalCoordinates(position);
            // Alpha is normalized in [min, min + 2Pi[
            final double normalizedAlpha = MathUtils.normalizeAngle(coords.getAlpha(), this.min[1] + MathLib.PI);
            final boolean isInsideAlt = coords.getNorm() >= this.min[0] && coords.getNorm() <= this.max[0];
            final boolean isInsideLon = normalizedAlpha >= this.min[1] && normalizedAlpha <= this.max[1];
            final boolean isInsideLat = coords.getDelta() >= this.min[2] && coords.getDelta() <= this.max[2];
            return isInsideAlt && isInsideLon && isInsideLat;
        }

        /** {@inheritDoc} */
        @Override
        public double[] getCoordinates(final Vector3D position) {
            final SphericalCoordinates coords = new SphericalCoordinates(position);
            // Alpha is normalized in [min, min + 2Pi[
            return new double[] { coords.getNorm(),
                MathUtils.normalizeAngle(coords.getAlpha(), this.min[1] + MathLib.PI),
                    coords.getDelta()
            };
        }

        /** {@inheritDoc} */
        @Override
        public double[] getXArray() {
            return this.xArray;
        }

        /** {@inheritDoc} */
        @Override
        public double[] getYArray() {
            return this.yArray;
        }

        /** {@inheritDoc} */
        @Override
        public double[] getZArray() {
            return this.zArray;
        }

        /** {@inheritDoc} */
        @Override
        public double[][][] getAccXArray() {
            return this.accXArray;
        }

        /** {@inheritDoc} */
        @Override
        public double[][][] getAccYArray() {
            return this.accYArray;
        }

        /** {@inheritDoc} */
        @Override
        public double[][][] getAccZArray() {
            return this.accZArray;
        }

        /** {@inheritDoc} */
        @Override
        public double[][][] getPotentialArray() {
            return this.potentialArray;
        }
    }
}
