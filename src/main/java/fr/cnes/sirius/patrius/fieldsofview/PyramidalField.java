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
 * @history creation 17/04/2012
 *
 * HISTORY
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11.1:DM:DM-88:30/06/2023:[PATRIUS] Complement FT 3319
 * VERSION:4.11.1:FA:FA-86:30/06/2023:[PATRIUS] Retours JE Alice
 * VERSION:4.11.1:FA:FA-98:30/06/2023:[PATRIUS] regression dans la classe AbstractAngularVelocitiesAttitudeProfile
 * VERSION:4.11:DM:DM-3232:22/05/2023:[PATRIUS] Detection d'extrema dans la classe ExtremaGenericDetector
 * VERSION:4.11:DM:DM-42:22/05/2023:[PATRIUS] Complement FT-3241
 * VERSION:4.10.2:FA:FA-3294:31/01/2023:[PATRIUS] Erreur dans la methode isInTheField(Vector3D) dans PyramidalField
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:DM:DM-3223:03/11/2022:[PATRIUS] Frame implements PVCoordinatesProvider
 * VERSION:4.10:DM:DM-3211:03/11/2022:[PATRIUS] Ajout de fonctionnalites aux PyramidalField
 * VERSION:4.10:DM:DM-3244:03/11/2022:[PATRIUS] Ajout propagation du signal dans ExtremaElevationDetector
 * VERSION:4.9:DM:DM-3158:10/05/2022:[PATRIUS] Ajout d'une methode computeSideDirections(Frame) in class PyramidalField 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2472:27/05/2020:Ajout d'un getter de sideAxis aux classes RectangleField et PyramidalField
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:---:11/04/2014:Quality assurance
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.fieldsofview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.cnes.sirius.patrius.attitudes.directions.ConstantVectorDirection;
import fr.cnes.sirius.patrius.attitudes.directions.IDirection;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.exception.MathRuntimeException;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Euclidean3D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.PolyhedronsSet;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Segment;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.geometry.partitioning.Region;
import fr.cnes.sirius.patrius.math.geometry.partitioning.RegionFactory;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.UtilsPatrius;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class describes a pyramidal field of view defined a list of vectors (its edges)
 * cone, to be used in "instruments" part properties. It implements the IFieldOfView interface
 * and so provides the associated services.
 * 
 * @concurrency immutable
 * 
 * @see IFieldOfView
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public final class PyramidalField implements IFieldOfView {
    
     /** Serializable UID. */
    private static final long serialVersionUID = 7525847594766384443L;

    /** 7.0 */
    private static final double C_7 = 7.0;
    
    /** Complementary field of view */
    private static final String COMPLEMENTARY = "-complementary-fov";
    
    /** String intersection */
    private static final String AND = "-intersection-";
    
    /** String union */
    private static final String OR = "-union-";
    
    /** Default spatial tolerance to compare vertices. */
    private static final double DEFAULT_TOLERANCE = 1E-6;
    
    /** Default origin point vector of the side axis. */
    private static final Vector3D ORIGIN = Vector3D.ZERO;

    /** the name of the field */
    private final String inName;

    /** the faces of the cone */
    private final FieldAngularFace[] faces;

    /** the number of faces */
    private final int facesNumber;

    /** Direct dihedra with previous face */
    private final Boolean[] directDihedra;

    /** Acute dihedra with previous face */
    private final Boolean[] acuteDihedra;
    
    /** Side axis. */
    private final Vector3D[] sideAxis;
    
    /** Indicates if the field of view is convex. */
    private boolean isFieldConvex;
    
    /** Indicates whether or not the field of view convexity has to be computed. */
    private boolean isConvexityComputed = false;
    
    /** Indicates whether or not the field is defined in the clockwise direction or not, regarding its side axes. */
    private boolean clockwise;
    
    /** Indicates whether or not the field of view direction has already been computed. */
    private boolean clockwiseAlreadyAssessed;
    
    /** Indicates whether or not intersecting faces have been checked. */
    private boolean checkIntersectingFaces;
    
    /** Polyhedron representing the field of view. */
    private transient PolyhedronsSet polyhedrons;

    /**
     * Constructor for a pyramidal field of view.
     * 
     * @param name
     *        the name of the field
     * @param directions
     *        the directions defining the border of the field.
     *        They must be given in the right order : from the vector i to the vector i + 1,
     *        the inside of the field is on the side of the positive cross vector v(i) * v(i+1)
     */
    public PyramidalField(final String name, final Vector3D[] directions) {
        this(name, directions, false);
    }
    
    /**
     * Constructor for a pyramidal field of view.
     * 
     * @param name
     *        the name of the field
     * @param directions
     *        the directions defining the border of the field.
     *        They must be given in the right order : from the vector i to the vector i + 1,
     *        the inside of the field is on the side of the positive cross vector v(i) * v(i+1)
     * @param polyhedronIn
     *        the polyhedron modelizing the field of view 
     */
    protected PyramidalField(final String name, final Vector3D[] directions, final PolyhedronsSet polyhedronIn) {
        this(name, directions, polyhedronIn, false);
    }
    
    /**
     * Constructor for a pyramidal field of view.
     * 
     * @param name
     *        the name of the field
     * @param directions
     *        the directions defining the border of the field.
     *        They must be given in the right order : from the vector i to the vector i + 1,
     *        the inside of the field is on the side of the positive cross vector v(i) * v(i+1)
     * @param checkIntersectingFaces
     *        flag that indicates if a verification on the field of view has to be performed: if yes, an exception is
     *        thrown if any field's face crosses any other face
     */
    public PyramidalField(final String name, final Vector3D[] directions, final boolean checkIntersectingFaces) {
        this(name, directions, null, checkIntersectingFaces);
    }
    
    /**
     * Protected constructor for a pyramidal field of view.
     * 
     * @param name
     *        the name of the field
     * @param directions
     *        the directions defining the border of the field.
     *        They must be given in the right order : from the vector i to the vector i + 1,
     *        the inside of the field is on the side of the positive cross vector v(i) * v(i+1)
     * @param polyhedronIn
     *        the polyhedron modelizing the field of view
     * @param checkIntersectingFaces
     *        flag that indicates if a verification on the field of view has to be performed: if yes, an exception is
     *        thrown if any field's face crosses any other face
     */
    protected PyramidalField(final String name, final Vector3D[] directions, final PolyhedronsSet polyhedronIn,
                             final boolean checkIntersectingFaces) {

        // Directions number must be at least 3
        if (directions.length < 3) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.PDB_TOO_FEW_DIRECTIONS);
        }

        // Initializations
        this.inName = name;
        this.facesNumber = directions.length;
        this.sideAxis = directions;
        this.clockwise = false;
        this.clockwiseAlreadyAssessed = false;
        this.checkIntersectingFaces = checkIntersectingFaces;

        // Check that the faces of this field do not intersect each other
        if (checkIntersectingFaces && this.isPyramidalFieldWithIntersectingFaces()) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.INVALID_FACES);
        }

        // Faces
        this.faces = new FieldAngularFace[this.facesNumber];

        for (int i = 0; i < this.facesNumber; i++) {
            final int next = i == this.facesNumber - 1 ? 0 : i + 1;
            this.faces[i] = new FieldAngularFace(directions[i], directions[next]);
        }

        // Build the polyhedron only if needed
        this.polyhedrons = polyhedronIn;

        // Boolean directDihedra[i]. (true if mixedProduct(V0, V1, V2) > 0
        // V0 and V1 defining faces[i - 1]
        // V1 and V2 defining faces[i]
        this.directDihedra = new Boolean[this.facesNumber];
        for (int i = 1; i < this.facesNumber - 1; i++) {
            this.directDihedra[i] = (mixedProduct(directions[i - 1], directions[i],
                directions[i + 1]) > 0);
        }

        // Particular case of the last and first vectors
        this.directDihedra[0] = (mixedProduct(directions[this.facesNumber - 1],
            directions[0], directions[1]) > 0);
        this.directDihedra[this.facesNumber - 1] = (mixedProduct(
            directions[this.facesNumber - 2], directions[this.facesNumber - 1],
            directions[0]) > 0);

        // Boolean acuteDihedra[i]. (true if the dihedra between
        // faces[i - 1] and faces[i] is < Pi/2 or > 3Pi/2
        this.acuteDihedra = new Boolean[this.facesNumber];

        // Loop on the faces
        for (int i = 1; i < this.facesNumber - 1; i++) {
            final Vector3D cp = Vector3D.crossProduct(directions[i - 1],
                directions[i]);
            this.acuteDihedra[i] = (mixedProduct(directions[i], cp,
                directions[i + 1]) >= 0);
        }
        // Particular case of the last and first vectors
        Vector3D cp = Vector3D.crossProduct(directions[this.facesNumber - 1],
            directions[0]);
        this.acuteDihedra[0] = (mixedProduct(directions[0], cp, directions[1]) >= 0);
        cp = Vector3D.crossProduct(directions[this.facesNumber - 2],
            directions[this.facesNumber - 1]);
        this.acuteDihedra[this.facesNumber - 1] = (mixedProduct(
            directions[this.facesNumber - 1], cp, directions[0]) >= 0);

    }

    /** {@inheritDoc} */
    @Override
    public double getAngularDistance(final Vector3D direction) {

        if (direction.getNorm() < UtilsPatrius.GEOMETRY_EPSILON) {
            return 0.0;
        }

        // initialisations (the first angle found will be smallest than 7.0
        // and replace it)
        double angle = C_7;
        double faceAngle;
        int targetClosestVertex = -1;

        // the result is the smallest (in absolute value!) angular distance
        // found on each face, returned with the right sign (in or out of
        // the field)
        for (int i = 0; i < this.facesNumber; i++) {
            faceAngle = this.faces[i].computeMinAngle(direction);
            if (MathLib.abs(faceAngle) < MathLib.abs(angle)) {
                angle = faceAngle;
                targetClosestVertex = this.getAngularDistancePart2(i);
            }
        }
        // Verify sign of distance when closest point is a vertex
        if (targetClosestVertex >= 0 && this.acuteDihedra[targetClosestVertex]
                && (this.directDihedra[targetClosestVertex] == (angle > 0))) {
            // For direct and acute vertex, angle must be negative (target outside)
            // For indirect and acute vertex, angle must be positive (target inside)
            angle = -angle;
        }

        return angle;
    }

    /**
     * Continued from previous for cyclomatic complexity
     * 
     * @param i
     *        face number
     * @return the number of the closest vertex
     */
    private int getAngularDistancePart2(final int i) {
        // the result is the smallest (in absolute value!) angular distance
        // found on each face, returned with the right sign (in or out of
        // the field)
        int targetClosestVertex = -1;
        if (this.faces[i].isCloseToVstart()) {
            targetClosestVertex = i;
        } else if (this.faces[i].isCloseToVend()) {
            targetClosestVertex = (i == (this.facesNumber - 1)) ? 0
                : (i + 1);
        } else {
            targetClosestVertex = -1;
        }
        return targetClosestVertex;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isInTheField(final Vector3D direction) {
        // comparison to 0.0: relative comparison can't be used
        return (this.getAngularDistance(direction) >= 0.0);
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return this.inName;
    }

    /**
     * Get the side axis of the field of view.
     * 
     * @return the side axis of the field of view
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public Vector3D[] getSideAxis() {
        return this.sideAxis;
    }

    /**
     * Indicates whether or not this field of view is convex. 
     * 
     * @return {@code true} if this field is convex 
     */
    public boolean isConvex(){
        if(!this.isConvexityComputed){
            // check convexity
            this.isFieldConvex = isConvex(this.sideAxis);
            this.isConvexityComputed = true;
        }
        
        return this.isFieldConvex;
    }

    /**
     * Indicates whether or not the faces of this field intersect each other.
     * 
     * @return {@code true} if the faces of this field intersect each other
     */
    public boolean isPyramidalFieldWithIntersectingFaces() {
        // Initialize field points
        final int numberOfFieldPoints = this.sideAxis.length;
        final Vector3D[] fieldPoints = new Vector3D[numberOfFieldPoints];

        // Compute field points
        for (int i = 0; i < numberOfFieldPoints; i++) {
            fieldPoints[i] = PyramidalField.ORIGIN.add(this.sideAxis[i].normalize());
        }

        // Initialize segments
        final int numberOfSegments = numberOfFieldPoints;
        final Segment[] segments = new Segment[numberOfSegments];
        int j;

        // Build segments with computed field points
        for (int i = 0; i < numberOfSegments; i++) {
            j = i == this.facesNumber - 1 ? 0 : i + 1;
            segments[i] = new Segment(fieldPoints[i], fieldPoints[j]);
        }

        return Segment.isIntersectingSegments(segments);
    }

    /**
     * Check the equality between this and a provided {@link PyramidalField}. The side axis are assumed to be sorted in
     * the same order. To be equal, both objects must have the same number of side axis and side axis at same index
     * position must be colinear within a given tolerance.
     * 
     * @param otherFov
     *        the other pyramidal field
     * @param angularTol
     *        the numerical tolerance to check the colinearity between side axis
     * 
     * @return {@code true} if both pyramidal field are equal within the tolerance
     */
    public boolean equals(final PyramidalField otherFov, final double angularTol){
        
        boolean isEqual = false;
        
        if (otherFov == this) {
            isEqual = true;
        } else if (otherFov != null) {

            isEqual = true;
            // check the side axis arrays length
            isEqual &= (otherFov.sideAxis.length == this.sideAxis.length);
            if(isEqual){
                for (int index = 0; index < this.sideAxis.length; index++) {
                    // retrieve the side vectors at same index position in the arrays
                    final Vector3D thisVector = this.sideAxis[index];
                    final Vector3D otherVector = otherFov.getSideAxis()[index];
                    
                    // compute angular distance
                    final double angularDist = Vector3D.angle(thisVector, otherVector);
                    isEqual &= MathLib.abs(angularDist) <= angularTol;
                }
            }        
        }
        
        return isEqual;
    }
    
    /**
     * Indicates whether or not the provided {@link PyramidalField} is included in this one. The condition
     * is respected if every side axis of the other field of view are contained in this one and if every side axis
     * of this one are outside the other pyramidal field.
     * <p>
     * Note: The second condition is verified only if this field of view is not convex.
     * </p>
     * <p>
     * Note: By convention the result is true if the provided {@link PyramidalField} is null.
     * </p>
     * 
     * @param otherFov
     *        the other field of view.
     * 
     * @return {@code true} if this field of view contains the other one
     */
    public boolean isInTheField(final PyramidalField otherFov){
        
        boolean isContained = true;
        
        if (otherFov == null) {
            // Convention: return true if the other field of view is null
            return isContained;
        }
        
        // check the first inclusion 
        for(final Vector3D otherSideAxis : otherFov.getSideAxis()){
            isContained &= this.getAngularDistance(otherSideAxis) >= 0;
        }
        
        // check the convexity
        if(!this.isConvex()){
            // every side axis must be outside the provided field of view
            for(final Vector3D thisSideAxis : this.sideAxis){
                isContained &= otherFov.getAngularDistance(thisSideAxis) <= 0;
            }
        }
        
        return isContained;
    }
    
    /**
     * Build the complementary field of view of this. The side axis are sorted in reverse order.
     * 
     * @return the complementary field of view
     */
    public PyramidalField getComplementaryFieldOfView(){
        
        // array of new directions to return
        final int nAxis = this.sideAxis.length; 
        final Vector3D[] newDirections = new Vector3D[nAxis];
        
        // first side axis
        newDirections[0] = this.sideAxis[0];
        for(int index = 1; index < nAxis; index++){
            newDirections[index] = this.sideAxis[nAxis - index];
        }
        
        // name of the complementary field of view 
        final String complementary = this.inName + COMPLEMENTARY;
        
        return new PyramidalField(complementary, newDirections, !this.turnClockwise());
    }
    
    /**
     * Build a new {@link PyramidalField} from the intersection between this field of view and the provided one.
     * 
     * @param otherFov
     *        the other field of view.
     * 
     * @return the intersection between the fields of view
     * 
     * @throws PatriusException
     *         thrown if an error occurs with the binary space partition. Such error can occur if one of the fields is
     *         concave and leads to several intersections.
     */
    public PyramidalField getIntersectionWith(final PyramidalField otherFov) throws PatriusException{

        PyramidalField merge = null;

        if (otherFov.isInTheField(this)) {
            merge = this;
        } else if (this.isInTheField(otherFov)) {
            // specific case : this field of view contains this one
            merge = otherFov;
        } else {
            // factory to manage the intersection computation
            final RegionFactory<Euclidean3D> factory = new RegionFactory<>();

            // build the polyhedrons
            final PolyhedronsSet polyhedron = this.getPolyhedronsSet();
            final PolyhedronsSet otherPolyhedron = otherFov.getPolyhedronsSet();

            // intersection computation
            final Region<Euclidean3D> intersection = factory.intersection(polyhedron.copySelf(),
                otherPolyhedron.copySelf());

            // the pyramidal field is built if the region is not empty
            if (!intersection.isEmpty()) {
                // build the polyhedron resulting the intersection
                final PolyhedronsSet polyhedronsSet = new PolyhedronsSet(intersection.getTree(false));

                // get the facets and vertices to build the intersected pyramidal field
                List<Vector3D> vertices = null;
                List<int[]> facets = null;
                try {
                    vertices = polyhedronsSet.getBRep().getVertices();
                    facets = polyhedronsSet.getBRep().getFacets();
                } catch (final MathRuntimeException mathException) {
                    throw new PatriusException(PatriusMessages.FOV_UNION_OR_INTERSECTION_TOO_COMPLEX, mathException);
                }

                // name the new field of view
                final String newName = this.getName() + AND + otherFov.getName();

                merge = new PyramidalField(newName, computeSideAxis(vertices, facets), polyhedronsSet);
            }
        }

        return merge;
    }
    
    /**
     * Build a new {@link PyramidalField} from the union between this field of view and the provided one.
     * 
     * @param otherFov
     *        the other field of view.
     * 
     * @return the union between the fields of view
     * 
     * @throws PatriusException
     *         thrown if an error occurs with the binary space partition. Such error can occur if the
     *         two fields are disjointed or if one of them is concave and leads to a hole in the middle of the
     *         theoretical resulting field.
     */
    public PyramidalField getUnionWith(final PyramidalField otherFov) throws PatriusException {

        PyramidalField merge = null;

        if (otherFov.isInTheField(this)) {
            merge = otherFov;
        } else if (this.isInTheField(otherFov)) {
            // specific case : this field of view contains this one
            merge = this;
        } else {
            // factory to manage the intersection computation
            final RegionFactory<Euclidean3D> factory = new RegionFactory<>();

            // build the polyhedrons
            final PolyhedronsSet polyhedron = this.getPolyhedronsSet();
            final PolyhedronsSet otherPolyhedron = otherFov.getPolyhedronsSet();

            // union computation
            final Region<Euclidean3D> union = factory.union(polyhedron.copySelf(),
                otherPolyhedron.copySelf());

            // the pyramidal field is built if the region is not empty
            if (!union.isEmpty()) {
                // build the polyhedron resulting the intersection
                final PolyhedronsSet polyhedronsSet = new PolyhedronsSet(union.getTree(true));

                // get the facets and vertices to build to intersected pyramidal field
                List<Vector3D> vertices = null;
                List<int[]> facets = null;
                try {
                    vertices = polyhedronsSet.getBRep().getVertices();
                    facets = polyhedronsSet.getBRep().getFacets();
                } catch (final MathRuntimeException mathException) {
                    throw new PatriusException(PatriusMessages.FOV_UNION_OR_INTERSECTION_TOO_COMPLEX, mathException);
                }
                

                // name of the new field of view 
                final String newName = this.getName() + OR + otherFov.getName();

                merge = new PyramidalField(newName, computeSideAxis(vertices, facets), polyhedronsSet);
            }
        }

        return merge;
    }
    
    /**
     * Compute the vectors delimiting a {@link PyramidalField} with the provided vertices and facets.
     * 
     * @param vertices
     *        the polyhedron vertices
     * @param facets
     *        the facets
     * 
     * @return the side axis
     */
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private static Vector3D[] computeSideAxis(final List<Vector3D> vertices, final List<int[]> facets) {

        // pyramidal field side axis array
        final List<Vector3D> directions = new ArrayList<>();

        // mapping edge vectors belonging to the same facet
        // as every side axis vector belongs to two consecutive facets, this resulting map is chained,
        // each vector is stored has a key and a value.
        final Map<Vector3D, Vector3D> mapOrientedVectors = new HashMap<>();

        // browse the facets of the polyhedron
        int nFacets = 0;
        for (int facetIndex = 0; facetIndex < facets.size(); facetIndex++) {
            // get current facet
            int[] facet = facets.get(facetIndex);
            
            // case with more than 3 vertices by facet: some vertices may be useless
            if (facet.length > 3) {
                facet = removeUnnecessaryVertices(facet, vertices);
            }
            
            if (facet.length == 3 && nFacets < facets.size()) {

                // ignore the polyhedron base facet
                // or the last facet if it is a tetrahedron
                final Vector3D p0 = vertices.get(facet[0]);
                final Vector3D p1 = vertices.get(facet[1]);
                final Vector3D p2 = vertices.get(facet[2]);

                // cross product key x value should be directed inside the polyhedron
                if (p0.equals(ORIGIN)) {
                    mapOrientedVectors.put(p2.subtract(p0), p1.subtract(p0));
                } else if (p1.equals(ORIGIN)) {
                    mapOrientedVectors.put(p0.subtract(p1), p2.subtract(p1));
                } else if (p2.equals(ORIGIN)) {
                    mapOrientedVectors.put(p1.subtract(p2), p0.subtract(p2));
                }
                nFacets++;
            }
        }

        // build the side axis array
        final Map.Entry<Vector3D, Vector3D> firstOrientedVectors = mapOrientedVectors.entrySet().iterator().next();

        // number of delimiting vectors
        final int nSideAxis = vertices.size() - 1;
        
        // the value of each entry is equal to a key
        final Vector3D startPoint = firstOrientedVectors.getValue();
        Vector3D previousDirection = startPoint;
        int counter = 0;
        while (counter < nSideAxis) {
            // add each value of the map of vectors in right order
            // sideAxis[i-1] x sideAxis[i] directed inside the polyhedron
            directions.add(mapOrientedVectors.get(previousDirection));

            // the new key is the previous value
            previousDirection = directions.get(counter);

            // update number of stored side axis
            counter++;
            
            // Break if loop is closed
            if (startPoint.equals(previousDirection)) {
                break;
            }
        }
        
        // Remove directions if the two surrounding edges are aligned: the corresponding vertex is useless
        removeUnnecessaryDirections(directions);

        // Return array version of directions list
        return directions.toArray(new Vector3D[directions.size()]);
    }
    
    /**
     * Remove unnecessary vertices from a facet. A Vertex is unnecessary if it does not mark a change in direction
     * between the two edges it links. A new facet is computed and returned.
     * 
     * @param facet the assessed facet
     * @param vertices the list of vertices used to create facets
     * 
     * @return a new facet formed by relevant vertices only
     */
    private static int[] removeUnnecessaryVertices(final int[] facet, final List<Vector3D> vertices) {
        // Ignore vertices that are in the middle of one facet edge since they are not relevant
        final List<Integer> relevantVertices = new ArrayList<>();
        for (int vertex = 0; vertex < facet.length; vertex++) {
            final Vector3D p0 = vertices.get(facet[vertex]);
            final Vector3D p1 = vertices.get(facet[(vertex + 1) % facet.length]);
            final Vector3D p2 = vertices.get(facet[(vertex + 2) % facet.length]);
            if (p0.subtract(p1).crossProduct(p2.subtract(p1)).getNorm() > DEFAULT_TOLERANCE) {
                // keep vertex if relevant
                relevantVertices.add(facet[(vertex + 1) % facet.length]);
            }
        }

        // update facet with relevant vertices only
        final int[] newFacet = new int[relevantVertices.size()];
        for (int index = 0; index < relevantVertices.size(); index++) {
            newFacet[index] = relevantVertices.get(index);
        }

        return newFacet;
    }
    
    /**
     * Assess if all computed directions are relevant, that is if they mark an corner of the field of view. If not,
     * useless directions are removed from the list. No new object is created.
     * 
     * @param directions field of view's directions
     */
    private static void removeUnnecessaryDirections(final List<Vector3D> directions) {
        // Remove directions that are contained in the plane formed by the two surrounding direction vectors
        final List<Integer> uselessDirections = new ArrayList<>();
        final int sizeDirections = directions.size();
        for (int vertex = 0; vertex < sizeDirections; vertex++) {
            final Vector3D p0 = directions.get(vertex);
            final Vector3D p1 = directions.get((vertex + 1) % sizeDirections);
            final Vector3D p2 = directions.get((vertex + 2) % sizeDirections);
            if (p0.subtract(p1).crossProduct(p2.subtract(p1)).getNorm() < DEFAULT_TOLERANCE) {
                // Store index of useless vectors so as to remove them from directions vector
                uselessDirections.add((vertex + 1) % sizeDirections);
            }
        }
        // Sort in reverse order so that indices are removed from the end of the list first
        Collections.sort(uselessDirections, Collections.reverseOrder());
        for (final int removeIndex : uselessDirections) {
            directions.remove(removeIndex);
        }
    }
    
    /**
     * Assess if the provided hull is convex.
     * 
     * @param directions
     *        the direction vectors delimiting the base of the polyhedron.
     * 
     * @return {@code true} if this field of view is convex, {@code false} if concave
     */
    private static boolean isConvex(final Vector3D[] directions) {
        
        // output initialization
        boolean isConvex = true;

        // fov number of side axis
        final int sideAxisNumber = directions.length;

        // loop on all sides of the field of view
        for (int iSide = 0; iSide < sideAxisNumber; iSide++) {
            // let's define the plane defined by both side axis indexed iSide and (iSide+1) = sidePlane

            // normal to sidePlane
            final Vector3D normal = directions[iSide].crossProduct(directions[(iSide + 1) % sideAxisNumber])
                .normalize();

            // assess on which side of sidePlane is the sideAxis indexed iSide+2
            final double scalarProduct = normal.dotProduct(directions[(iSide + 2) % sideAxisNumber].normalize());

            if (scalarProduct < 0) {
                // opposite side with respect to field of view: the field of view is curved inwards
                isConvex = false;
                break; 
            }
        }

        return isConvex;
    }
    
    /**
     * Computes the mixed product of three vectors.
     * 
     * @param v1
     *        first vector
     * @param v2
     *        second vector
     * @param v3
     *        third vector
     * @return the mixed product
     */
    private static double mixedProduct(final Vector3D v1, final Vector3D v2, final Vector3D v3) {
        return Vector3D.dotProduct(Vector3D.crossProduct(v1, v2), v3);
    }
    
    /**
     * Build the {@link PolyhedronsSet} modelizing this field of view.
     * 
     * @return the polyhedron of the field of view
     */
    private PolyhedronsSet getPolyhedronsSet() {
        if (this.polyhedrons == null) {
            this.polyhedrons = buildPolyhedronsSet(ORIGIN, this.sideAxis, DEFAULT_TOLERANCE);
        }

        return this.polyhedrons;
    }
    
    /**
     * Build the {@link PolyhedronsSet} with the provided polyhedron directions and the origin.
     * 
     * @param origin
     *        the origin of the polyhedron directions
     * @param directions
     *        the directions that delimits the polyhedron
     * @param tolerance
     *        the geometrical absolute tolerance
     *        
     * @return the polyhedron modelizing this field of view
     */
    private static PolyhedronsSet buildPolyhedronsSet(final Vector3D origin, final Vector3D[] directions,
                                                      final double tolerance) {

        // list of vertices
        final List<Vector3D> vertices = new ArrayList<>();
        vertices.add(origin);

        // list of facets
        final List<int[]> facets = new ArrayList<>();

        // facet of the polyhedron base
        final int[] base = new int[directions.length];

        // build the triangle faces
        int currentVertex = 1;
        for (final Vector3D direction : directions) {
            // add the current vertex
            vertices.add(origin.add(direction));

            final int nextVertex = currentVertex == directions.length ? 1 : currentVertex + 1;
            // build the facet with normal vector pointing outside of the polyhedron
            facets.add(new int[] { 0, nextVertex, currentVertex });

            // add the current vertex index to the polyhedron base facet
            base[currentVertex - 1] = currentVertex;

            // update vertex index position in list
            currentVertex++;
        }
        // add base facet at the end of facets list
        facets.add(base);


        
//        for (final Vector3D direction : directions) {
//            vertices.add(new Vector3D(direction.getX(), direction.getY(), 0.));
//        }
//        facets.add(new int[] { 0, 1, 2, 3 });
//        for (final Vector3D direction : directions) {
//            vertices.add(origin.add(direction));
//        }
//
//        facets.add(reverseArray(new int[] { 4, 5, 6, 7 }));
//
//        facets.add(reverseArray(new int[] { 0, 1, 5, 4 }));
//        facets.add(reverseArray(new int[] { 1, 2, 6, 5 }));
//        facets.add(reverseArray(new int[] { 2, 3, 7, 6 }));
//        facets.add(reverseArray(new int[] { 3, 0, 4, 7 }));

        return new PolyhedronsSet(vertices, facets, tolerance);
    }
    

//    private static int[] reverseArray(final int[] array) {
//        return new int[] { array[3], array[2], array[1], array[0] };
//    }

    /**
     * Retrieve the directions delimiting the pyramidal field of view.
     * 
     * @param frame the reference frame in which the vectors will be defined as
     *        constant
     * @return the directions delimiting the pyramidal field of view.
     */
    public IDirection[] computeSideDirections(final Frame frame) {
        // retrieve number of viewing directions
        final int nbDirections = getSideAxis().length;
        // initiate output
        final IDirection[] directions = new IDirection[nbDirections];
        // loop over all viewing directions
        for (int iDir = 0; iDir < nbDirections; iDir++) {
            // define current direction
            directions[iDir] = new ConstantVectorDirection(getSideAxis()[iDir], frame);
        }

        // return directions
        return directions;
    }
    
    /**
     * Assess if the side axes of the entered field of view turn clockwise or counter-clockwise, from a sensor's/user's
     * point of view.<br>
     * Calculation is perfomed only once for a field, meaning that it is performed if it has never been, and returns the
     * previously calculated result otherwise.<br>
     * Field's attribute clockwiseAlreadyAssessed is updated for this purpose.<br>
     * This algorithm relies on the hypothesis that the faces of this field of view do not cross each other, so this
     * field of view is expected to be well-formed.
     * 
     * @return true if the field of view turns clockwise, false if counter-clockwise
     */
    public final boolean turnClockwise() {
        
        // Check if the faces of this pyramidal field cross each other (because, if that is the case, this algorithm
        // cannot be used) and if the sense of rotation has already been computed
        if (isPyramidalFieldWithIntersectingFaces()) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.INVALID_FACES);
        } else if (!this.clockwiseAlreadyAssessed) {
            // Define the single rotation transforming the first side axis into +Z axis, so that the image of second
            // side axis has a azimuth equal to -PI in the XY plane
            // The XY plane is therefore including the first axis, and is orthogonal to the first face of the pyramid
            // The non-orthogonality of vectors is handled by the rotation
            // This rotation recalculates vectors in a basis suited for azimuth computation
            final Rotation rot = new Rotation(this.getSideAxis()[0], this.getSideAxis()[1], Vector3D.PLUS_K,
                Vector3D.MINUS_I);

            // Previous azimuth
            // Initialized to -PI so that [previousAzimuth, previousAzimuth+2*PI[ = [-PI, PI[
            // This interval is the image interval of method getAlpha used later on
            double previousAzimuth = -MathLib.PI;

            // Counter of azimuth crossings
            // The azimuth defines the side of next field axes relatively to the first pyramidal face (used as a
            // reference). It is calculated in the XY plane defined previously.
            // A crossing is an azimuth gap superior to half a turn: for a simple convex field this happens if the third
            // side axis is in the counter-clockwise direction (because its azimuth would be positive, whereas the
            // second axis azimuth is -PI by definition).
            // The assumption that the field of view does not have crossing faces is important here.
            int counter = 0;

            // Loop on side axes from third one to last one
            for (int iSide = 2; iSide < this.getSideAxis().length; iSide++) {
                // Compute the azimuth of current side axis in [-PI, PI[ = [previousAzimuth, previousAzimuth+2*PI[
                final double azimuth = rot.applyTo(this.getSideAxis()[iSide]).getAlpha();

                if (MathLib.abs(azimuth - previousAzimuth) > MathLib.PI) {
                    // Gap in azimuths: the limit is reached between previous and current side axis
                    // The assumption that the field of view does not have crossing faces intervenes here
                    counter++;
                }
                
                // Update azimuth for next side axis
                previousAzimuth = azimuth;
            }

            // Boolean is false by default as it has not been calculated previously
            if ((counter % 2) == 0) {
                // Even number of azimuth gaps (counter cannot be negative per construction)
                clockwise = true;
            }

            // Update flags
            this.clockwiseAlreadyAssessed = true;
        }

        return clockwise;
    }
}
