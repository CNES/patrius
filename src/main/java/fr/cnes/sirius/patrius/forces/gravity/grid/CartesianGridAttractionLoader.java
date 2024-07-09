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

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;


/**
 * Grid attraction model loader.
 * This loader only reads attraction model files defined with a cubic grid (X, Y, Z).
 * <p>Read data is considered to be in km.</p>
 *
 * @author Emmanuel Bignon
 *
 * @since 4.7
 */
@SuppressWarnings("PMD.MethodReturnsInternalArray")
// Reason: performances
public class CartesianGridAttractionLoader implements GridAttractionProvider {

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
    public CartesianGridAttractionLoader(final String modelFileName) throws PatriusException {
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

            // Grid min
            reader.readLine();
            line = reader.readLine();
            components = line.split(SPLIT);
            final Vector3D gridMin = new Vector3D(Double.parseDouble(components[1]) * M_TO_KM,
                    Double.parseDouble(components[2]) * M_TO_KM, Double.parseDouble(components[3]) * M_TO_KM);

            // Grid max
            reader.readLine();
            line = reader.readLine();
            components = line.split(SPLIT);
            final Vector3D gridMax = new Vector3D(Double.parseDouble(components[1]) * M_TO_KM,
                    Double.parseDouble(components[2]) * M_TO_KM, Double.parseDouble(components[3]) * M_TO_KM);

            // Resolution
            reader.readLine();
            line = reader.readLine();
            components = line.split(SPLIT);
            final Vector3D resolution = new Vector3D(Double.parseDouble(components[1]) * M_TO_KM,
                    Double.parseDouble(components[2]) * M_TO_KM, Double.parseDouble(components[3]) * M_TO_KM);

            // Attraction data
            final List<AttractionDataPoint> pointsList = new ArrayList<AttractionDataPoint>();
            line = reader.readLine();
            while (line != null) {
                // Skip comments
                if (!line.contains("#")) {
                    components = line.split(SPLIT);
                    // Construction of data
                    final double x = Double.parseDouble(components[1]) * M_TO_KM;
                    final double y = Double.parseDouble(components[2]) * M_TO_KM;
                    final double z = Double.parseDouble(components[3]) * M_TO_KM;
                    final double ax = Double.parseDouble(components[4]) * gmkm * M_TO_KM;
                    final double ay = Double.parseDouble(components[5]) * gmkm * M_TO_KM;
                    final double az = Double.parseDouble(components[6]) * gmkm * M_TO_KM;
                    final double pot = Double.parseDouble(components[6 + 1]) * gm / M_TO_KM;
                    final AttractionDataPoint point = new AttractionDataPoint(new Vector3D(x, y, z), new Vector3D(ax,
                            ay, az), pot);
                    pointsList.add(point);
                }
                // Update loop variables
                line = reader.readLine();
            }

            // Build final data container
            final AttractionDataPoint[] points = pointsList.toArray(new AttractionDataPoint[pointsList.size()]);
            final GridSystem grid = new CartesianGrid(gridMin, gridMax, resolution, points);
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
        return data;
    }

    /**
     * Grid system defined by cartesian coordinates.
     * <p>
     * This class is to be used in conjunction with {@link GridAttractionModel} for attraction force defined by a grid.
     * </p>
     *
     * @author Emmanuel Bignon
     *
     * @since 4.7
     */
    private static class CartesianGrid implements GridSystem {

        /** Serial UID. */
        private static final long serialVersionUID = -2465692184591853096L;

        /** Grid min on 3 axis. */
        private final Vector3D min;

        /** Grid max on 3 axis. */
        private final Vector3D max;

        /** X data array (values along first abscissa). */
        private final double[] xArray;

        /** Y data array (values along second abscissa). */
        private final double[] yArray;

        /** Z data array (values along third abscissa). */
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
        public CartesianGrid(final Vector3D min,
                final Vector3D max,
                final Vector3D resolution,
                final AttractionDataPoint[] points) {
            // Attributes
            this.min = min;
            this.max = max;

            // Initialize arrays
            final int nbX = (int) MathLib.round((max.getX() - min.getX()) / resolution.getX() + 1);
            final int nbY = (int) MathLib.round((max.getY() - min.getY()) / resolution.getY() + 1);
            final int nbZ = (int) MathLib.round((max.getZ() - min.getZ()) / resolution.getZ() + 1);
            this.xArray = new double[nbX];
            this.yArray = new double[nbY];
            this.zArray = new double[nbZ];
            this.accXArray = new double[nbX][nbY][nbZ];
            this.accYArray = new double[nbX][nbY][nbZ];
            this.accZArray = new double[nbX][nbY][nbZ];
            this.potentialArray = new double[nbX][nbY][nbZ];
            // Build arrays
            // Abscissa
            for (int i = 0; i < nbX; i++) {
                this.xArray[i] = min.getX() + i * resolution.getX();
            }
            for (int i = 0; i < nbY; i++) {
                this.yArray[i] = min.getY() + i * resolution.getY();
            }
            for (int i = 0; i < nbZ; i++) {
                this.zArray[i] = min.getZ() + i * resolution.getZ();
            }
            // Ordinates
            for (int i = 0; i < points.length; i++) {
                // Indices in array
                final int ix = (int) MathLib.round((points[i].getPosition().getX() - min.getX()) / resolution.getX());
                final int iy = (int) MathLib.round((points[i].getPosition().getY() - min.getY()) / resolution.getY());
                final int iz = (int) MathLib.round((points[i].getPosition().getZ() - min.getZ()) / resolution.getZ());
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
            final boolean isInsideX = position.getX() >= min.getX() && position.getX() <= max.getX();
            final boolean isInsideY = position.getY() >= min.getY() && position.getY() <= max.getY();
            final boolean isInsideZ = position.getZ() >= min.getZ() && position.getZ() <= max.getZ();
            return isInsideX && isInsideY && isInsideZ;
        }

        /** {@inheritDoc} */
        @Override
        public double[] getCoordinates(final Vector3D position) {
            return new double[] { position.getX(), position.getY(), position.getZ() };
        }

        /** {@inheritDoc} */
        @Override
        public double[] getXArray() {
            return xArray;
        }

        /** {@inheritDoc} */
        @Override
        public double[] getYArray() {
            return yArray;
        }

        /** {@inheritDoc} */
        @Override
        public double[] getZArray() {
            return zArray;
        }

        /** {@inheritDoc} */
        @Override
        public double[][][] getAccXArray() {
            return accXArray;
        }

        /** {@inheritDoc} */
        @Override
        public double[][][] getAccYArray() {
            return accYArray;
        }

        /** {@inheritDoc} */
        @Override
        public double[][][] getAccZArray() {
            return accZArray;
        }

        /** {@inheritDoc} */
        @Override
        public double[][][] getPotentialArray() {
            return potentialArray;
        }
    }
}
