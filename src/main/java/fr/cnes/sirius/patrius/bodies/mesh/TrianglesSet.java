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

    /** Serial UID. */
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
        sphereRadius2 = 0;
        sphereRadius2 = MathLib.max(sphereRadius2, triangle.getVertices()[0].getPosition().distanceSq(center));
        sphereRadius2 = MathLib.max(sphereRadius2, triangle.getVertices()[1].getPosition().distanceSq(center));
        sphereRadius2 = MathLib.max(sphereRadius2, triangle.getVertices()[2].getPosition().distanceSq(center));
    }

    /**
     * Branch constructor. Assumes number of triangles >= 2.
     * @param triangles list of triangles
     * @param splitDirection next splitting direction
     */
    private TrianglesSet(final Triangle[] triangles,
            final SplitDirection splitDirection) {

        // Computer triangles center
        center = Vector3D.ZERO;
        for (final Triangle triangle : triangles) {
            center = center.add(triangle.getCenter());
        }
        center = center.scalarMultiply(1. / triangles.length);

        // Compute radius square of encompassing sphere
        sphereRadius2 = 0;
        for (final Triangle triangle : triangles) {
            sphereRadius2 = MathLib.max(sphereRadius2, triangle.getVertices()[0].getPosition().distanceSq(center));
            sphereRadius2 = MathLib.max(sphereRadius2, triangle.getVertices()[1].getPosition().distanceSq(center));
            sphereRadius2 = MathLib.max(sphereRadius2, triangle.getVertices()[2].getPosition().distanceSq(center));
        }

        // Branch (triangles count >= 2)

        // Split according to direction
        final List<Triangle> list1 = new ArrayList<Triangle>();
        final List<Triangle> list2 = new ArrayList<Triangle>();
        for (int i = 0; i < triangles.length; i++) {
            final Triangle triangle = triangles[i];
            if (splitDirection.isLeft(triangle.getCenter(), center)) {
                list1.add(triangle);
            } else {
                list2.add(triangle);
            }
        }

        // Create children branches
        // May be empty in case of triangles in same 2D cut plane
        if (list1.isEmpty()) {
            childBranch1 = null;
        } else if (list1.size() == 1) {
            // Leaf
            childBranch1 = new TrianglesSetLeaf(list1.get(0));
        } else {
            // Branch
            childBranch1 = new TrianglesSet(list1.toArray(new Triangle[list1.size()]), splitDirection.next());
        }
        if (list2.isEmpty()) {
            childBranch2 = null;
        } else if (list2.size() == 1) {
            // Leaf
            childBranch2 = new TrianglesSetLeaf(list2.get(0));
        } else {
            // Branch
            childBranch2 = new TrianglesSet(list2.toArray(new Triangle[list2.size()]), splitDirection.next());
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
    public Intersection[] getIntersections(final Line line) {
        Intersection[] res = null;
        // Check children only if line lies within own encompassing sphere radius
        if (isWithinSphereRadius(line)) {
            // Branch
            if (childBranch1 != null) {
                res = Intersection.append(res, childBranch1.getIntersections(line));
            }
            if (childBranch2 != null) {
                res = Intersection.append(res, childBranch2.getIntersections(line));
            }
        }
        return res;
    }

    /**
     * Returns the exact distance to the provided line.
     * The algorithm is iterative and is in O(log(n)).
     * @param line a line in the body frame
     * @return exact distance from tree to provided line
     */
    public double distanceTo(final Line line) {
        // Check intersection: if intersection, distance is 0
        double minDistance = 0;
        if (getIntersections(line) == null) {
            // No intersection: compute min distance through recursive algorithm in O(log(n)) starting with root tree
            TrianglesSet closestBranch = this;
            while (true) {
                // Compute min distance to both children branches
                // Closest branch is kept
                double minDistanceSq1 = Double.POSITIVE_INFINITY;
                if (closestBranch.childBranch1 != null) {
                    minDistanceSq1 = closestBranch.childBranch1.distanceSqTo(line);
                }
                double minDistanceSq2 = Double.POSITIVE_INFINITY;
                if (closestBranch.childBranch2 != null) {
                    minDistanceSq2 = closestBranch.childBranch2.distanceSqTo(line);
                }
                closestBranch = minDistanceSq1 < minDistanceSq2 ? closestBranch.childBranch1
                        : closestBranch.childBranch2;

                // Stop when leaf is reached
                if (closestBranch instanceof TrianglesSetLeaf) {
                    minDistance = MathLib.sqrt(closestBranch.distanceSqTo(line));
                    break;
                }
            }
        }

        return minDistance;
    }

    /**
     * Returns the squared distance from encompassing sphere center to provided line.
     * @param line a line in the body frame
     * @return squared distance from encompassing sphere center to provided line
     */
    protected double distanceSqTo(final Line line) {
        final double dx = center.getX() - line.getOrigin().getX();
        final double dy = center.getY() - line.getOrigin().getY();
        final double dz = center.getZ() - line.getOrigin().getZ();
        final double dot = dx * line.getDirection().getX() + dy * line.getDirection().getY() + dz
                * line.getDirection().getZ();
        final double nx = dx - dot * line.getDirection().getX();
        final double ny = dy - dot * line.getDirection().getY();
        final double nz = dz - dot * line.getDirection().getZ();
        return nx * nx + ny * ny + nz * nz;
    }

    /**
     * Returns true if line crosses encompassing sphere of triangles set.
     * 
     * @param line
     *        line of sight
     * @return returns true if line crosses encompassing sphere of triangles set
     */
    protected boolean isWithinSphereRadius(final Line line) {
        return distanceSqTo(line) < sphereRadius2;
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
        public abstract boolean isLeft(final Vector3D p,
                final Vector3D ref);
    }
}
