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
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11.1:FA:FA-50:30/06/2023:[PATRIUS] Calcul d'intersections sur un FacetBodyShape
 * VERSION:4.11.1:DM:DM-88:30/06/2023:[PATRIUS] Complement FT 3319
 * VERSION:4.10.1:FA:FA-3265:02/12/2022:[PATRIUS] Calcul KO des points plus proches entre une Line et un FacetBodyShape
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:DM:DM-3211:03/11/2022:[PATRIUS] Ajout de fonctionnalites aux PyramidalField
 * VERSION:4.10:DM:DM-3183:03/11/2022:[PATRIUS] Acces aux points les plus proches entre un GeometricBodyShape...
 * VERSION:4.9:DM:DM-3135:10/05/2022:[PATRIUS] Calcul d'intersection sur BodyShape  
 * VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
 * VERSION:4.7:FA:FA-2762:18/05/2021:Probleme lors des controles qualite via la PIC 
 * VERSION:4.6:DM:DM-2544:27/01/2021:Ajouter la definition d'un corps celeste a partir d'un modele de forme 
 * VERSION:4.6:DM:DM-2528:27/01/2021:[PATRIUS] Integration du modele DTM 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies.mesh;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Binary Space Partition Tree for mesh storage. This class is package protected and is only for internal use.
 * It stores 2 branches which are either:
 * <ul>
 * <li>regular branches as {@link TrianglesSet}</li>
 * <li>leaves, which contains only one triangle. In this case, {@link TrianglesSet} is specialized in a
 * {@link TrianglesSetLeaf}.</li>
 * </ul>
 * <p>
 * During construction, center and encompassing spherical radius are computed for fast intersection computation.
 * </p>
 * 
 * @author Emmanuel Bignon
 * 
 * @since 4.6
 */
class TrianglesSet implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 181527348212602026L;

    /** First child branch (may be null). */
    private final TrianglesSet childBranch1;

    /** Second child branch (may be null). */
    private final TrianglesSet childBranch2;

    /** Barycenter of triangles set in body frame. */
    private Vector3D center;

    /** Encompassing sphere radius squared. */
    private double sphereRadius2;

    /**
     * Root constructor.
     * @param triangles list of triangles
     */
    public TrianglesSet(final Triangle[] triangles) {
        this(triangles, SplitDirection.X);
    }

    /**
     * Constructor for {@link TriangleSetLeaf}.
     * @param triangle triangle
     */
    @SuppressWarnings("PMD.NullAssignment")
    // Reason: performances
    protected TrianglesSet(final Triangle triangle) {
        this.childBranch1 = null;
        this.childBranch2 = null;
        this.center = triangle.getCenter();

        // Compute radius square of encompassing sphere
        this.sphereRadius2 = 0;
        this.sphereRadius2 = MathLib.max(this.sphereRadius2,
            triangle.getVertices()[0].getPosition().distanceSq(this.center));
        this.sphereRadius2 = MathLib.max(this.sphereRadius2,
            triangle.getVertices()[1].getPosition().distanceSq(this.center));
        this.sphereRadius2 = MathLib.max(this.sphereRadius2,
            triangle.getVertices()[2].getPosition().distanceSq(this.center));
    }

    /**
     * Branch constructor. Assumes number of triangles >= 2.
     * @param triangles list of triangles
     * @param splitDirection next splitting direction
     */
    @SuppressWarnings("PMD.NullAssignment")
    // Reason: math of BSP tree
    private TrianglesSet(final Triangle[] triangles,
            final SplitDirection splitDirection) {

        // Computer triangles center
        this.center = Vector3D.ZERO;
        for (final Triangle triangle : triangles) {
            this.center = this.center.add(triangle.getCenter());
        }
        this.center = this.center.scalarMultiply(1. / triangles.length);

        // Compute radius square of encompassing sphere
        this.sphereRadius2 = 0;
        for (final Triangle triangle : triangles) {
            this.sphereRadius2 = MathLib.max(this.sphereRadius2,
                triangle.getVertices()[0].getPosition().distanceSq(this.center));
            this.sphereRadius2 = MathLib.max(this.sphereRadius2,
                triangle.getVertices()[1].getPosition().distanceSq(this.center));
            this.sphereRadius2 = MathLib.max(this.sphereRadius2,
                triangle.getVertices()[2].getPosition().distanceSq(this.center));
        }

        // Branch (triangles count >= 2)

        // Split according to direction
        final List<Triangle> list1 = new ArrayList<>();
        final List<Triangle> list2 = new ArrayList<>();
        for (int i = 0; i < triangles.length; i++) {
            final Triangle triangle = triangles[i];
            if (splitDirection.isLeft(triangle.getCenter(), this.center)) {
                list1.add(triangle);
            } else {
                list2.add(triangle);
            }
        }

        // Create children branches
        // May be empty in case of triangles in same 2D cut plane
        if (list1.isEmpty()) {
            this.childBranch1 = null;
        } else if (list1.size() == 1) {
            // Leaf
            this.childBranch1 = new TrianglesSetLeaf(list1.get(0));
        } else {
            // Branch
            this.childBranch1 = new TrianglesSet(list1.toArray(new Triangle[list1.size()]), splitDirection.next());
        }
        if (list2.isEmpty()) {
            this.childBranch2 = null;
        } else if (list2.size() == 1) {
            // Leaf
            this.childBranch2 = new TrianglesSetLeaf(list2.get(0));
        } else {
            // Branch
            this.childBranch2 = new TrianglesSet(list2.toArray(new Triangle[list2.size()]), splitDirection.next());
        }
    }

    /**
     * Returns list of intersection points and corresponding triangle.
     * <p>
     * Warning: if intersection exactly lies on several triangles, then all triangles and corresponding points will be
     * listed.
     * </p>
     * <p>The algorithm is recursive and is in O(log(n)).</p>
     * @param line a line in the body frame
     * @return list of intersection points and corresponding triangle, null if no intersection
     */
    @SuppressWarnings({"PMD.ReturnEmptyArrayRatherThanNull", "PMD.OptimizableToArrayCall"})
    // Reason: fast computation requires minimal CPU workload including empty arrays
    public Intersection[] getIntersections(final Line line) {
        Intersection[] res = null;
        // Check children only if line lies within own encompassing sphere radius
        if (isWithinSphereRadius(line)) {
            // Branch
            if (this.childBranch1 != null) {
                res = Intersection.append(res, this.childBranch1.getIntersections(line));
            }
            if (this.childBranch2 != null) {
                res = Intersection.append(res, this.childBranch2.getIntersections(line));
            }
        }

        // Check if there are intersection points
        if (res == null) {
            return null;
        }

        // Otherwise define list of valid intersection points
        final List<Intersection> validResList = new ArrayList<>();

        // Loop on the intersection points
        for (int i = 0; i < res.length; i++) {
            // Check if the abscissa of the intersection point is bigger than the minimum one
            if (line.getAbscissa(res[i].getPoint()) > line.getMinAbscissa()) {
                // Add the current intersection point to the list of valid intersection points
                validResList.add(res[i]);
            }
        }
        if (validResList.isEmpty()) {
            return null;
        }
        return validResList.toArray(new Intersection[0]);
    }

    /**
     * Returns the squared distance from encompassing sphere center to provided line.
     * @param line a line in the body frame
     * @return squared distance from encompassing sphere center to provided line
     */
    protected double distanceSqTo(final Line line) {
        // final double dx = this.center.getX() - line.getOrigin().getX();
        // final double dy = this.center.getY() - line.getOrigin().getY();
        // final double dz = this.center.getZ() - line.getOrigin().getZ();
        // final double dot = dx * line.getDirection().getX() + dy * line.getDirection().getY() + dz
        // * line.getDirection().getZ();
        // final double nx = dx - dot * line.getDirection().getX();
        // final double ny = dy - dot * line.getDirection().getY();
        // final double nz = dz - dot * line.getDirection().getZ();
        // return nx * nx + ny * ny + nz * nz;

        // Compute distance with line, taking min abscissa into account
        final double dist = line.distance(this.center);
        return dist * dist;
    }

    /**
     * Returns true if line crosses encompassing sphere of triangles set.
     * 
     * @param line
     *        line of sight
     * @return returns true if line crosses encompassing sphere of triangles set
     */
    protected boolean isWithinSphereRadius(final Line line) {
        return distanceSqTo(line) < this.sphereRadius2;
    }

    /**
     * Splitting direction. This direction is used for BSP generation.
     * Each direction provides next splitting direction used for automatic BSP generation.
     */
    private enum SplitDirection {

        /** X direction. */
        X {
            /** {@inheritDoc} */
            @Override
            public SplitDirection next() {
                return SplitDirection.Y;
            }

            /** {@inheritDoc} */
            @Override
            public boolean isLeft(final Vector3D p,
                    final Vector3D ref) {
                return p.getX() < ref.getX();
            }
        },

        /** Y direction. */
        Y {
            /** {@inheritDoc} */
            @Override
            public SplitDirection next() {
                return SplitDirection.Z;
            }

            /** {@inheritDoc} */
            @Override
            public boolean isLeft(final Vector3D p,
                    final Vector3D ref) {
                return p.getY() < ref.getY();
            }
        },

        /** Z direction. */
        Z {
            /** {@inheritDoc} */
            @Override
            public SplitDirection next() {
                return SplitDirection.X;
            }

            /** {@inheritDoc} */
            @Override
            public boolean isLeft(final Vector3D p,
                    final Vector3D ref) {
                return p.getZ() < ref.getZ();
            }
        };

        /**
         * Returns next splitting direction. Next splitting direction follows the scheme: X => Y => Z => X.
         * @return next splitting direction
         */
        public abstract SplitDirection next();

        /**
         * Returns true if p < ref in the splitting direction.
         * @param p current point
         * @param ref reference point
         * @return true if p < ref in the splitting direction
         */
        public abstract boolean isLeft(final Vector3D p, final Vector3D ref);
    }
}
