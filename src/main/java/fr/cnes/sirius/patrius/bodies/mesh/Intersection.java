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

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;

/**
 * Intersection data. An intersection consists in a 3D point and an owning triangle {@link Triangle}.
 *
 * @author Emmanuel Bignon
 *
 * @since 4.6
 */
public class Intersection {

    /** Intersecting triangle.*/
    private final Triangle triangle;

    /** Intersection point in input frame.*/
    private final Vector3D point;
    
    /**
     * Constructor.
     * @param triangle intersecting triangle
     * @param point intersection point in input frame
     */
    public Intersection(final Triangle triangle, final Vector3D point) {
        this.triangle = triangle;
        this.point = point;
    }

    /**
     * Appends two arrays together.
     * @param array1 first array
     * @param array2 second array
     * @return [array1, array2]
     */
    public static Intersection[] append(final Intersection[] array1,
            final Intersection[] array2) {
        // Immediate returns if one of array is null
        if (array1 == null) {
            return array2;
        }
        if (array2 == null) {
            return array1;
        }
        // Standard concatenation
        final Intersection[] res = new Intersection[array1.length + array2.length];
        System.arraycopy(array1, 0, res, 0, array1.length);
        System.arraycopy(array2, 0, res, array1.length, array2.length);
        return res;
    }

    /**
     * Return the intersecting triangle.
     * @return the intersecting triangle
     */
    public Triangle getTriangle() {
        return triangle;
    }

    /**
     * Returns the intersection point in input frame.
     * @return the intersection point in input frame
     */
    public Vector3D getPoint() {
        return point;
    }
}
