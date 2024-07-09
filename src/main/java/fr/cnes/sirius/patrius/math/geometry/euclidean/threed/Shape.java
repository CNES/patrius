/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

/**
 * Interface for all shapes.
 * <p>
 * It includes infinite and solid shapes. All of them must be able to compute their intersection and distance to a line.
 * </p>
 * 
 * @version $Id: Shape.java 18108 2017-10-04 06:45:27Z bignon $
 */

public interface Shape {

    /**
     * Tests the intersection with a line.
     * 
     * @param line
     *        the line
     * @return true if the line intersects the shape
     */
    boolean intersects(Line line);

    /**
     * Compute the intersection points with a line.
     * 
     * @param line
     *        the line
     * @return the intersection points if they exist. If no intersection is found,
     *         the dimension is zero
     */
    Vector3D[] getIntersectionPoints(Line line);

    /**
     * Computes the distance to a line.
     * 
     * @param line
     *        the line
     * @return the shortest distance between the the line and the shape
     */
    double distanceTo(Line line);

    /**
     * Computes the points of the shape and the line realizing
     * the shortest distance. If the line intersects the shape, the returned points
     * are identical : this point is the first common point found.
     * 
     * @param line
     *        the line
     * @return the two points : first the one from the line, and the one
     *         from the shape.
     */
    Vector3D[] closestPointTo(Line line);
}
