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
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies.mesh;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;

/**
 * Binary Space Partition Tree for mesh storage. This class is package protected and is only for internal use.
 * It stores a leaf, which contains only one triangle. This is a particular car of {@link TrianglesSet}.
 * <p>
 * During construction, center and encompassing spherical radius are computed for fast intersection computation.
 * </p>
 * 
 * @author Emmanuel Bignon
 * 
 * @since 4.6
 */
class TrianglesSetLeaf extends TrianglesSet implements Serializable {

    /** Serial UID. */
    private static final long serialVersionUID = 181527348212602026L;

    /** Leaf triangle. */
    private final Triangle leaf;

    /**
     * Branch constructor.
     * @param triangle triangle
     */
    public TrianglesSetLeaf(final Triangle triangle) {
        super(triangle);
        this.leaf = triangle;
    }

    /** {@inheritDoc} */
    @Override
    public Intersection[] getIntersections(final Line line) {
        Intersection[] res = null;
        final Vector3D intersection = leaf.getIntersection(line);
        if (intersection != null) {
            res = new Intersection[] { new Intersection(leaf, intersection) };
        }
        return res;
    }

    /**
     * Returns the exact squared distance from leaf triangle to provided line.
     * @param line a line in the body frame
     * @return exact squared distance from leaf triangle to provided line
     */
    @Override
    protected double distanceSqTo(final Line line) {
        final double d = leaf.distanceTo(line);
        return d * d;
    }
}
