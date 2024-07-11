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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3164:10/05/2022:[PATRIUS] Amelioration de la gestion du GM (parametre gravitationnel)
 * VERSION:4.9:DM:DM-3127:10/05/2022:[PATRIUS] Ajout de deux methodes resize a l'interface GeometricBodyShape ...
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:FA:FA-2982:15/11/2021:[PATRIUS] Orienter correctement les facettes de la methode toObjFile de
 * GeodeticMeshLoader
 * VERSION:4.8:DM:DM-2975:15/11/2021:[PATRIUS] creation du repere synodique via un LOF 
 * VERSION:4.6:DM:DM-2544:27/01/2021:Ajouter la definition d'un corps celeste a partir d'un modele de forme 
 * VERSION:4.6:DM:DM-2528:27/01/2021:[PATRIUS] Integration du modele DTM 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies.mesh;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Loader for mesh defined by geodetic coordinates.
 * This reader assumes the mesh is defined by coronas with a north pole and a south pole.
 * <p>
 * The file format is column text file following format for each line: [latitude (deg) longitude (deg) altitude (m)].
 * </p>
 * <p>
 * Example:<br/>
 * -90.0000 0.0000 8.3726<br/>
 * -90.0000 2.0000 8.3726<br/>
 * -90.0000 4.0000 8.3726<br/>
 * ...<br/>
 * </p>
 * <p>
 * Note about the convention used <b>by loaded files</b>: they follow the cartographic convention meaning that, 
 * in the case of Phobos or Deimos for instance, longitudes are increasing toward West (from +X_body to -Y_body). This 
 * convention is used by NASA and is recommended by the IAU working group (Report of the IAU Working Group on 
 * Cartographic Coordinates and Rotational Elements: 2009):
 * "Thus, west longitudes (i.e., longitudes measured positively to the west) will be used when the rotation is direct, 
 * i.e. the sign of the second term in the expression for W is positive. East longitudes (i.e., longitudes measured 
 * positively to the east) will be used when the rotation is retrograde, i.e. the sign of the second term in the 
 * expression for W is negative. The origin is the center of mass. Also because of tradition, the Earth, Sun, and 
 * Moon do not conform with this definition. Their rotations are direct and longitudes run both east and west 180°, 
 * or east 360°"
 * <br/>
 * Once a body is loaded and instantiated as a {@link FacetBodyShape} its longitudes follow the more intuitive 
 * mathematical convention. In this case, longitudes are increasing counter-clockwise toward East 
 * (from +X_body to +Y_body).
 * </p>
 *
 * @author Emmanuel Bignon
 *
 * @since 4.6
 */
public class GeodeticMeshLoader implements MeshProvider {

     /** Serializable UID. */
    private static final long serialVersionUID = 7891219379062211805L;

    /** Epsilon for discarding small triangles. */
    private static final double EPS = 1E-6;

    /** m to km conversion. */
    private static final int M_TO_KM = 1000;

    /** Column separation. */
    private static final String SPLIT = "[ ]+";

    /** List of vertices. */
    private Map<Integer, ExtendedGeodeticPoint> vertices;

    /** Mesh of {@link Triangle} stored under a list of {@link Triangle}. */
    private Triangle[] triangles;

    /**
     * Constructor.
     *
     * @param modelFileName
     *        mesh model .tab file name
     * @throws PatriusException thrown if loading failed
     */
    public GeodeticMeshLoader(final String modelFileName) throws PatriusException {
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

            // Load the vertex 3D vectors
            this.vertices = new HashMap<>();
            int i = 1;
            while (line != null) {
                final String[] components = line.trim().split(SPLIT);

                // Construction of geodetic point
                final double lat = MathLib.toRadians(Double.parseDouble(components[0]));
                final double lng = -MathLib.toRadians(Double.parseDouble(components[1]));
                final double alt = Double.parseDouble(components[2]) * M_TO_KM;

                // Get cartesian coordinates and add to list
                final double coslat = MathLib.cos(lat);
                final double sinlat = MathLib.sin(lat);
                final double coslon = MathLib.cos(lng);
                final double sinlon = MathLib.sin(lng);
                final Vector3D pos = new Vector3D(coslat * coslon, coslat * sinlon, sinlat).scalarMultiply(alt);
                this.vertices.put(i, new ExtendedGeodeticPoint(new GeodeticPoint(lat, lng, alt), new Vertex(i, pos)));

                // Update loop variables
                i++;
                line = reader.readLine();
            }

            // Close readers
            reader.close();
            isr.close();
            fileReader.close();

            // Retrieve the corona
            final Map<Double, List<ExtendedGeodeticPoint>> coronas = new TreeMap<>();
            for (final ExtendedGeodeticPoint p : this.vertices.values()) {
                List<ExtendedGeodeticPoint> corona = coronas.get(p.point.getLatitude());
                if (corona == null) {
                    corona = new ArrayList<>();
                }
                corona.add(p);
                coronas.put(p.point.getLatitude(), corona);
            }

            // Builds the triangles mesh
            final List<Triangle> trianglesList = new ArrayList<>();
            final Iterator<Entry<Double, List<ExtendedGeodeticPoint>>> iterator = coronas.entrySet().iterator();

            // North pole and first corona
            if (iterator.hasNext()) {
                final ExtendedGeodeticPoint np = iterator.next().getValue().get(0);
                final List<ExtendedGeodeticPoint> corona1 = iterator.next().getValue();
                trianglesList.addAll(tesselate(np, corona1));

                // Regular coronas up to south pole
                List<ExtendedGeodeticPoint> prevCorona = corona1;
                while (iterator.hasNext()) {
                    final List<ExtendedGeodeticPoint> nextCorona = iterator.next().getValue();
                    if (!iterator.hasNext()) {
                        // South pole
                        trianglesList.addAll(tesselate(nextCorona.get(0), prevCorona));
                    } else {
                        // Regular coronas
                        trianglesList.addAll(tesselate(prevCorona, nextCorona));
                    }
                    // Update loop variable
                    prevCorona = nextCorona;
                }
            }
            final List<Triangle> finalTrianglesList = new ArrayList<>();
            for (final Triangle t : trianglesList) {
                if (t.getSurface() > EPS) {
                    // Discard unnecessary triangles which have a null surface (not exactly 0 because of round-off
                    // errors)
                    finalTrianglesList.add(t);
                }
            }
            this.triangles = finalTrianglesList.toArray(new Triangle[finalTrianglesList.size()]);

        } catch (final IOException e) {
            // Failed to load mesh
            throw new PatriusException(e, PatriusMessages.FAILED_TO_LOAD_MESH, modelFileName);
        }
    }

    /**
     * Build a list of triangle from a pole to a corona.
     * @param pole pole
     * @param corona corona (list of geodetic points)
     * @return list of triangle from a pole to a corona
     */
    private static List<Triangle> tesselate(final ExtendedGeodeticPoint pole,
            final List<ExtendedGeodeticPoint> corona) {
        final List<Triangle> res = new ArrayList<>();
        for (int j = 0; j < corona.size(); j++) {
            res.add(new Triangle(pole.vertex, corona.get(j).vertex, corona.get((j + 1) % corona.size()).vertex));
        }
        return res;
    }

    /**
     * Build a list of triangle from two coronas.
     * @param corona1 first corona (list of geodetic points)
     * @param corona2 second corona (list of geodetic points)
     * @return list of triangle from two coronas
     * @throws PatriusException thrown if coronas do not have same size
     */
    private static List<Triangle> tesselate(final List<ExtendedGeodeticPoint> corona1,
            final List<ExtendedGeodeticPoint> corona2) throws PatriusException {
        if (corona1.size() != corona2.size()) {
            throw new PatriusException(PatriusMessages.FAILED_TO_LOAD_MESH, "");
        }

        final List<Triangle> res = new ArrayList<>();
        for (int j = 0; j < corona1.size(); j++) {
            res.add(new Triangle(corona1.get(j).vertex, corona1.get((j + 1) % corona1.size()).vertex,
                    corona2.get(j).vertex));
            res.add(new Triangle(corona2.get(j).vertex, corona2.get((j + 1) % corona1.size()).vertex, corona1
                    .get((j + 1) % corona1.size()).vertex));
        }
        return res;
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    // Reason: performances
    public Triangle[] getTriangles() {
        return this.triangles;
    }

    /**
     * Convert file to .obj file format.
     * @param fileName output file name
     * @throws PatriusException thrown if writing failed
     */
    public void toObjFile(final String fileName) throws PatriusException {
        try {
            // Set up writer for line-oriented file
            final OutputStream fileWriter = new FileOutputStream(fileName);
            final OutputStreamWriter osw = new OutputStreamWriter(fileWriter, StandardCharsets.UTF_8);
            final BufferedWriter writer = new BufferedWriter(osw);

            // Vertices
            for (final ExtendedGeodeticPoint point : this.vertices.values()) {
                final Vector3D pos = point.vertex.getPosition();
                writer.write(String.format(Locale.US, "v %.12f %.12f %.12f%n", pos.getX() / M_TO_KM, pos.getY()
                        / M_TO_KM, pos.getZ() / M_TO_KM));
            }

            // Triangles (facets always described counter-clockwise)
            for (final Triangle t : this.triangles) {
                // Culling test: counter-clockwise triangles will return a negative dot product with position vector
                final Vector3D v1 = t.getVertices()[0].getPosition().subtract(t.getVertices()[1].getPosition());
                final Vector3D v2 = t.getVertices()[0].getPosition().subtract(t.getVertices()[2].getPosition());
                final Vector3D v3 = t.getCenter();
                final double dotProduct = v3.dotProduct(v1.crossProduct(v2));
                final String format = "f %d %d %d%n";
                if (dotProduct >= 0) {
                    writer.write(String.format(format, t.getVertices()[0].getID(), t.getVertices()[1].getID(),
                            t.getVertices()[2].getID()));
                } else {
                    writer.write(String.format(format, t.getVertices()[0].getID(), t.getVertices()[2].getID(),
                            t.getVertices()[1].getID()));
                }
            }

            // Close writers
            writer.close();
            osw.close();
            fileWriter.close();

        } catch (final IOException e) {
            // Failed to write mesh
            throw new PatriusException(e, PatriusMessages.FAILED_TO_WRITE_MESH, fileName);
        }
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    // Reason: performances
    public Map<Integer, Vertex> getVertices() {
        final Map<Integer, Vertex> res = new HashMap<>();
        for (final ExtendedGeodeticPoint point : this.vertices.values()) {
            res.put(point.vertex.getID(), point.vertex);
        }
        return res;
    }

    /**
     * Geodetic point containing both geodetic and cartesian informations.
     *
     * @author Emmanuel Bignon
     */
    private static final class ExtendedGeodeticPoint implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = -2711257713112855434L;

        /** Geodetic point. */
        private final GeodeticPoint point;

        /** Vertex. */
        private final Vertex vertex;

        /**
         * Constructor.
         * @param point geodetic point
         * @param vertex vertex
         */
        public ExtendedGeodeticPoint(final GeodeticPoint point,
                final Vertex vertex) {
            this.point = point;
            this.vertex = vertex;
        }
    }
}
