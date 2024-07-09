/**
 * 
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
 * @history creation 17/04/2012
 * 
 * HISTORY
 * VERSION:4.5:DM:DM-2472:27/05/2020:Ajout d'un getter de sideAxis aux classes RectangleField et PyramidalField
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:---:11/04/2014:Quality assurance
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.fieldsofview;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
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
    
    /** Serial UID. */
    private static final long serialVersionUID = 7525847594766384443L;

    /** 7.0 */
    private static final double C_7 = 7.0;

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

        // directions number must be at least 3
        if (directions.length < 3) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.PDB_TOO_FEW_DIRECTIONS);
        }

        // initialisations
        this.inName = name;
        this.facesNumber = directions.length;
        this.sideAxis = directions;

        // faces
        this.faces = new FieldAngularFace[this.facesNumber];
        for (int i = 0; i < this.facesNumber - 1; i++) {
            this.faces[i] = new FieldAngularFace(directions[i], directions[i + 1]);
        }
        this.faces[this.facesNumber - 1] = new FieldAngularFace(directions[this.facesNumber - 1], directions[0]);

        // Boolean directDihedra[i]. (true if mixedProduct(V0, V1, V2) > 0
        // V0 and V1 defining faces[i - 1]
        // V1 and V2 defining faces[i]
        this.directDihedra = new Boolean[this.facesNumber];
        for (int i = 1; i < this.facesNumber - 1; i++) {
            this.directDihedra[i] = (this.mixedProduct(directions[i - 1], directions[i],
                directions[i + 1]) > 0);
        }
        // particular case of the last and first vectors
        this.directDihedra[0] = (this.mixedProduct(directions[this.facesNumber - 1],
            directions[0], directions[1]) > 0);
        this.directDihedra[this.facesNumber - 1] = (this.mixedProduct(
            directions[this.facesNumber - 2], directions[this.facesNumber - 1],
            directions[0]) > 0);

        // Boolean acuteDihedra[i]. (true if the dihedra between
        // faces[i - 1] and faces[i] is < Pi/2 or > 3Pi/2
        this.acuteDihedra = new Boolean[this.facesNumber];
        // loop on the faces
        for (int i = 1; i < this.facesNumber - 1; i++) {
            final Vector3D cp = Vector3D.crossProduct(directions[i - 1],
                directions[i]);
            this.acuteDihedra[i] = (this.mixedProduct(directions[i], cp,
                directions[i + 1]) > 0);
        }
        // particular case of the last and first vectors
        Vector3D cp = Vector3D.crossProduct(directions[this.facesNumber - 1],
            directions[0]);
        this.acuteDihedra[0] = (this.mixedProduct(directions[0], cp, directions[1]) > 0);
        cp = Vector3D.crossProduct(directions[this.facesNumber - 2],
            directions[this.facesNumber - 1]);
        this.acuteDihedra[this.facesNumber - 1] = (this.mixedProduct(
            directions[this.facesNumber - 1], cp, directions[0]) > 0);
    }

    /** {@inheritDoc} */
    @Override
    public double getAngularDistance(final Vector3D direction) {

        if (direction.getNorm() < UtilsPatrius.GEOMETRY_EPSILON) {
            return 0.0;
        } else {

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
        // comparison to 0.0 : relative comparison can't be used
        return (this.getAngularDistance(direction) > 0.0);
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
    private double mixedProduct(final Vector3D v1, final Vector3D v2, final Vector3D v3) {
        return Vector3D.dotProduct(Vector3D.crossProduct(v1, v2), v3);
    }

}
