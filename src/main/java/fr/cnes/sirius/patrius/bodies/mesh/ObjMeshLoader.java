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
* VERSION:4.6:DM:DM-2544:27/01/2021:Ajouter la definition d'un corps celeste a partir d'un modele de forme 
* VERSION:4.6:DM:DM-2528:27/01/2021:[PATRIUS] Integration du modele DTM 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies.mesh;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;


/**
 * .obj format mesh loader.
 * .obj format is defined here: https://fr.wikipedia.org/wiki/Objet_3D_(format_de_fichier).
 * <p>Read data is considered to be in km.</p>
 * <p>
 * This readers reads only "vertex" lines (starting with 'v' character) and "facet" lines (starting with 'f' character)
 * </p>
 *
 * @author Emmanuel Bignon
 *
 * @since 4.6
 */
public class ObjMeshLoader implements MeshProvider {

    /** m to km conversion. */
    private static final int M_TO_KM = 1000;

    /** Column separation. */
    private static final String SPLIT = "[ ]+";

    /** List of vertices. */
    private Map<Integer, Vertex> vertices;
    
    /** Mesh of {@link Triangle} stored under a list of {@link Triangle}. */
    private Triangle[] triangles;

    /**
     * Constructor.
     *
     * @param modelFileName
     *        mesh model .obj file name
     * @throws PatriusException thrown if loading failed
     */
    public ObjMeshLoader(final String modelFileName) throws PatriusException {
        // Load data from file
        loadData(modelFileName);
    }

    /**
     * Load model.
     * @param modelFileName mesh model .obj file name
     * @throws PatriusException thrown if load failed (file is not .obj or data is inconsistent)
     */
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    // Reason: performances
    private void loadData(final String modelFileName) throws PatriusException {

        try {
            // Set up reader for line-oriented file
            final InputStream fileReader = new FileInputStream(modelFileName);
            final InputStreamReader isr = new InputStreamReader(fileReader, StandardCharsets.UTF_8);
            final BufferedReader reader = new BufferedReader(isr);

            String line = reader.readLine();

            // Initial semi-major axis
            double a0max = 0;
            double a0min = Double.POSITIVE_INFINITY;

            // Load the vertex 3D vectors
            vertices = new HashMap<Integer, Vertex>();
            boolean isVertex = true;
            int i = 1;
            while (isVertex == true && line != null) {
                isVertex = line.charAt(0) == 'v';
                if (isVertex) {
                    final String[] components = line.split(SPLIT);
                    // Construction of vertex and add to list
                    final double x = Double.parseDouble(components[1]) * M_TO_KM;
                    final double y = Double.parseDouble(components[2]) * M_TO_KM;
                    final double z = Double.parseDouble(components[3]) * M_TO_KM;
                    vertices.put(i, new Vertex(i, new Vector3D(x, y, z)));

                    a0max = MathLib.max(a0max, MathLib.max(MathLib.abs(x), MathLib.abs(y)));
                    a0min = MathLib.min(a0min, MathLib.min(MathLib.abs(x), MathLib.abs(y)));

                    // Update loop variables
                    i++;
                    line = reader.readLine();
                }
            }

            // Load the triangles
            final List<Triangle> trianglesList = new ArrayList<Triangle>();
            boolean isTriangle = true;
            i = 1;
            while (isTriangle == true && line != null) {
                isTriangle = line.charAt(0) == 'f';
                if (isTriangle) {
                    final String[] components = line.split(SPLIT);
                    // Construction of triangle and add to list
                    final int iv1 = Integer.parseInt(components[1]);
                    final int iv2 = Integer.parseInt(components[2]);
                    final int iv3 = Integer.parseInt(components[3]);
                    trianglesList.add(new Triangle(i, vertices.get(iv1), vertices.get(iv2), vertices.get(iv3)));

                    // Update loop variables
                    i++;
                    line = reader.readLine();
                }
            }
            this.triangles = trianglesList.toArray(new Triangle[trianglesList.size()]);

            // Close readers
            reader.close();
            isr.close();
            fileReader.close();

        } catch (final IOException e) {
            // Failed to load mesh
            throw new PatriusException(e, PatriusMessages.FAILED_TO_LOAD_MESH, modelFileName);
        }
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    // Reason: performances
    public Triangle[] getTriangles() {
        return triangles;
    }

    /** {@inheritDoc} */
    @Override
    public Map<Integer, Vertex> getVertices() {
        return vertices;
    }
}
