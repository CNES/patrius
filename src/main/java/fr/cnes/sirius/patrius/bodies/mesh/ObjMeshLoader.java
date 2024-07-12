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
 * VERSION:4.11:DM:DM-3288:22/05/2023:[PATRIUS] ID de facette pour un FacetBodyShape
 * VERSION:4.11:FA:FA-3314:22/05/2023:[PATRIUS] Anomalie evaluation ForceModel SpacecraftState en ITRF
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3164:10/05/2022:[PATRIUS] Amelioration de la gestion du GM (parametre gravitationnel)
 * VERSION:4.9:DM:DM-3127:10/05/2022:[PATRIUS] Ajout de deux methodes resize a l'interface GeometricBodyShape ...
 * VERSION:4.9:FA:FA-3167:10/05/2022:[PATRIUS] La methode ObJMeshLoader ne permet pas de lire des fichiers .obj 
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
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * .obj format mesh loader.
 * .obj format is defined here: https://fr.wikipedia.org/wiki/Objet_3D_(format_de_fichier).
 * <p>
 * Read data is considered to be in km.
 * </p>
 * <p>
 * This readers reads only "vertex" lines (starting with 'v' character) and "facet" lines (starting
 * with 'f' character)
 * </p>
 *
 * @author Emmanuel Bignon
 *
 * @since 4.6
 */
public class ObjMeshLoader implements MeshProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = -37078204737389555L;

    /** m to km conversion. */
    private static final int M_TO_KM = 1000;

    /** Column separation. */
    private static final String SPLIT = "[ ]+";

    /** Slash separation. */
    private static final String SLASH = "/";

    /** Mesh model .obj file name. */
    private final String modelFileName;

    /** Object's name. */
    private String name;

    /** List of vertices. */
    private transient Map<Integer, Vertex> vertices;

    /** Mesh of {@link Triangle} stored under a list of {@link Triangle}. */
    private transient Triangle[] triangles;

    /**
     * Expected data type of .obj file
     */
    public enum DataType {
        /** Object name. */
        NAME("o"),
        /** Vertex. */
        VERTEX("v"),
        /** Texture. */
        TEXTURE("vt"),
        /** Normal vector. */
        NORMAL("vn"),
        /** Object face. */
        FACE("f"),
        /** Commentary line. */
        COMMENT("#"),
        /** Group of faces. */
        GROUP("g"),
        /** Other code type. */
        OTHER("");

        /** Code of the data stored in a line. */
        private final String code;

        /**
         * Simple constructor.
         * 
         * @param codeIn
         *        the data type code
         */
        private DataType(final String codeIn) {
            this.code = codeIn;
        }

        /**
         * Get the data type code.
         * @return the data type code
         */
        private String getCode() {
            return this.code;
        }

        /**
         * Get the type of data corresponding to the code.
         * @param codeIn
         *        code defining the type of data stored in the file line
         * 
         * @return the data type.
         * @throws PatriusException if the input code does not correspond to a data type
         */
        public static DataType getDataType(final String codeIn) throws PatriusException {
            // data type to return
            DataType dataType = DataType.OTHER;
            // loop over all the data types
            boolean notEqual = true;
            int index = 0;
            while (notEqual && index < DataType.values().length) {
                final DataType type = DataType.values()[index];
                if (type.getCode().equals(codeIn)) {
                    dataType = type;
                    notEqual = false;
                }
                index++;
            }

            return dataType;
        }
    }

    /**
     * Constructor.
     *
     * @param modelFileName
     *        mesh model .obj file name
     * @throws PatriusException thrown if loading failed
     */
    public ObjMeshLoader(final String modelFileName) throws PatriusException {
        this.modelFileName = modelFileName;
        // Load data from file
        loadData(modelFileName);
    }

    /**
     * Load model.
     * 
     * @param modelFileNameIn mesh model .obj file name
     * @throws PatriusException thrown if load failed (file is not .obj or data is inconsistent)
     */
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    // Reason: performances
        private
        void
            loadData(final String modelFileNameIn) throws PatriusException {

        try {
            // Set up reader for line-oriented file
            final InputStream fileReader = new FileInputStream(modelFileNameIn);
            final InputStreamReader isr = new InputStreamReader(fileReader, StandardCharsets.UTF_8);
            final BufferedReader reader = new BufferedReader(isr);

            // read first line
            String line = reader.readLine();

            // Load the vertex 3D vectors
            this.vertices = new HashMap<>();

            // Load the triangles
            final List<Triangle> trianglesList = new ArrayList<>();

            // Vertex index
            int vertexIndex = 1;
            // Triangle index
            int triangleIndex = 1;
            while (line != null) {
                // test if the input line is not empty
                final String[] components = line.split(SPLIT);
                // retrieve the data type allowed by object file
                final DataType dataType = DataType.getDataType(components[0]);
                // if input data is not a face or a vertex coordinate, the data is ignored
                switch (dataType) {

                    case NAME:
                        // retrieve the object name
                        this.name = components[1];
                        break;

                    case VERTEX:
                        // Construction of vertex and add to list
                        final double x = Double.parseDouble(components[1]) * M_TO_KM;
                        final double y = Double.parseDouble(components[2]) * M_TO_KM;
                        final double z = Double.parseDouble(components[3]) * M_TO_KM;
                        this.vertices.put(vertexIndex, new Vertex(vertexIndex, new Vector3D(x, y, z)));
                        vertexIndex++;
                        break;

                    case FACE:
                        // parse all indexes defining a face
                        final Integer[][] indexes = getFaceComponents(components);
                        // Construction of triangle and add to list
                        final int iv1 = indexes[0][0];
                        final int iv2 = indexes[1][0];
                        final int iv3 = indexes[2][0];
                        trianglesList.add(new Triangle(triangleIndex, this.vertices.get(iv1), this.vertices
                            .get(iv2), this.vertices.get(iv3)));
                        triangleIndex++;
                        break;

                    default:
                        // do nothing
                        break;
                }

                line = reader.readLine();
            }
            this.triangles = trianglesList.toArray(new Triangle[trianglesList.size()]);
            // Close readers
            reader.close();
            isr.close();
            fileReader.close();

        } catch (final IOException e) {
            // Failed to load mesh
            throw new PatriusException(e, PatriusMessages.FAILED_TO_LOAD_MESH, modelFileNameIn);
        }
    }

    /**
     * A face is defined by three sets of indexes, a set corresponds to a vertex of the triangle.
     * Each set stores at
     * most 3 indexes : the first one refers to the vertex 3D coordinates, the second one to the
     * vertex texture and the
     * last one to the vertex normal vector. The 3x3 array is built as following : rows correspond
     * to the
     * vertices and columns to the indexes refering to v/vt/vn (vt and vn indexes may be null if
     * they are not defined in
     * .obj file).
     * 
     * @param vertexIndexes
     *        the line storing the three sets of indexes
     * 
     * @return the array storing the indexes
     */
    private static Integer[][] getFaceComponents(final String[] vertexIndexes) {
        final Integer[][] indexesArray = new Integer[3][3];
        // split the vertex index, texture index and normal vector index
        for (int index = 1; index <= 3; index++) {
            final String[] indexes = vertexIndexes[index].split(SLASH);

            // extract index of vertex 3D coordinates
            final int vIndex = Integer.parseInt(indexes[0]);
            indexesArray[index - 1][0] = vIndex;

            // extract other indexes
            if (indexes.length == 3) {
                // texture index
                indexesArray[index - 1][1] = Integer.parseInt(indexes[1]);
                // normal vector index
                indexesArray[index - 1][2] = Integer.parseInt(indexes[2]);
            }
        }

        return indexesArray;
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    // Reason: performances
            public
            Triangle[] getTriangles() {
        return this.triangles;
    }

    /** {@inheritDoc} */
    @Override
    public Map<Integer, Vertex> getVertices() {
        return this.vertices;
    }

    /**
     * Return the name of the loaded object
     * 
     * @return the name of the object (may be null)
     */
    public String getName() {
        return this.name;
    }

    /**
     * Custom deserialization is needed.
     * 
     * @param stream
     *        Object stream
     * @throws IOException if an I/O error occurs
     * @throws ClassNotFoundException if the class of a serialized object cannot be found
     * @throws PatriusException
     *         if load failed (file is not .obj or data is inconsistent)
     */
    private void readObject(final ObjectInputStream stream) throws IOException,
            ClassNotFoundException, PatriusException {
        stream.defaultReadObject();
        // manually deserialize and load data from file
        loadData(this.modelFileName);
    }
}
