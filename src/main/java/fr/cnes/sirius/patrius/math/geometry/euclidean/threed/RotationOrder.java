/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

import java.io.Serializable;

/**
 * This class is a utility representing a rotation order specification
 * for Cardan or Euler angles specification.
 * 
 * This class cannot be instanciated by the user. He can only use one
 * of the twelve predefined supported orders as an argument to either
 * the {@link Rotation#Rotation(RotationOrder,double,double,double)} constructor or the {@link Rotation#getAngles}
 * method.
 * 
 * @version $Id: RotationOrder.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 1.2
 */
public final class RotationOrder implements Serializable {

    /**
     * Set of Cardan angles.
     * this ordered set of rotations is around X, then around Y, then
     * around Z
     */
    public static final RotationOrder XYZ =
        new RotationOrder("XYZ", Vector3D.PLUS_I, Vector3D.PLUS_J, Vector3D.PLUS_K);

    /**
     * Set of Cardan angles.
     * this ordered set of rotations is around X, then around Z, then
     * around Y
     */
    public static final RotationOrder XZY =
        new RotationOrder("XZY", Vector3D.PLUS_I, Vector3D.PLUS_K, Vector3D.PLUS_J);

    /**
     * Set of Cardan angles.
     * this ordered set of rotations is around Y, then around X, then
     * around Z
     */
    public static final RotationOrder YXZ =
        new RotationOrder("YXZ", Vector3D.PLUS_J, Vector3D.PLUS_I, Vector3D.PLUS_K);

    /**
     * Set of Cardan angles.
     * this ordered set of rotations is around Y, then around Z, then
     * around X
     */
    public static final RotationOrder YZX =
        new RotationOrder("YZX", Vector3D.PLUS_J, Vector3D.PLUS_K, Vector3D.PLUS_I);

    /**
     * Set of Cardan angles.
     * this ordered set of rotations is around Z, then around X, then
     * around Y
     */
    public static final RotationOrder ZXY =
        new RotationOrder("ZXY", Vector3D.PLUS_K, Vector3D.PLUS_I, Vector3D.PLUS_J);

    /**
     * Set of Cardan angles.
     * this ordered set of rotations is around Z, then around Y, then
     * around X
     */
    public static final RotationOrder ZYX =
        new RotationOrder("ZYX", Vector3D.PLUS_K, Vector3D.PLUS_J, Vector3D.PLUS_I);

    /**
     * Set of Euler angles.
     * this ordered set of rotations is around X, then around Y, then
     * around X
     */
    public static final RotationOrder XYX =
        new RotationOrder("XYX", Vector3D.PLUS_I, Vector3D.PLUS_J, Vector3D.PLUS_I);

    /**
     * Set of Euler angles.
     * this ordered set of rotations is around X, then around Z, then
     * around X
     */
    public static final RotationOrder XZX =
        new RotationOrder("XZX", Vector3D.PLUS_I, Vector3D.PLUS_K, Vector3D.PLUS_I);

    /**
     * Set of Euler angles.
     * this ordered set of rotations is around Y, then around X, then
     * around Y
     */
    public static final RotationOrder YXY =
        new RotationOrder("YXY", Vector3D.PLUS_J, Vector3D.PLUS_I, Vector3D.PLUS_J);

    /**
     * Set of Euler angles.
     * this ordered set of rotations is around Y, then around Z, then
     * around Y
     */
    public static final RotationOrder YZY =
        new RotationOrder("YZY", Vector3D.PLUS_J, Vector3D.PLUS_K, Vector3D.PLUS_J);

    /**
     * Set of Euler angles.
     * this ordered set of rotations is around Z, then around X, then
     * around Z
     */
    public static final RotationOrder ZXZ =
        new RotationOrder("ZXZ", Vector3D.PLUS_K, Vector3D.PLUS_I, Vector3D.PLUS_K);

    /**
     * Set of Euler angles.
     * this ordered set of rotations is around Z, then around Y, then
     * around Z
     */
    public static final RotationOrder ZYZ =
        new RotationOrder("ZYZ", Vector3D.PLUS_K, Vector3D.PLUS_J, Vector3D.PLUS_K);
    
    /**
     * 
     */
    private static final long serialVersionUID = -4659695517744731021L;

    /** Name of the rotations order. */
    private final String name;

    /** Axis of the first rotation. */
    private final Vector3D a1;

    /** Axis of the second rotation. */
    private final Vector3D a2;

    /** Axis of the third rotation. */
    private final Vector3D a3;

    /**
     * Private constructor.
     * This is a utility class that cannot be instantiated by the user,
     * so its only constructor is private.
     * 
     * @param nameIn
     *        name of the rotation order
     * @param a1In
     *        axis of the first rotation
     * @param a2In
     *        axis of the second rotation
     * @param a3In
     *        axis of the third rotation
     */
    private RotationOrder(final String nameIn,
        final Vector3D a1In, final Vector3D a2In, final Vector3D a3In) {
        this.name = nameIn;
        this.a1 = a1In;
        this.a2 = a2In;
        this.a3 = a3In;
    }

    /**
     * Get a string representation of the instance.
     * 
     * @return a string representation of the instance (in fact, its name)
     */
    @Override
    public String toString() {
        return this.name;
    }

    /**
     * Get the axis of the first rotation.
     * 
     * @return axis of the first rotation
     */
    public Vector3D getA1() {
        return this.a1;
    }

    /**
     * Get the axis of the second rotation.
     * 
     * @return axis of the second rotation
     */
    public Vector3D getA2() {
        return this.a2;
    }

    /**
     * Get the axis of the second rotation.
     * 
     * @return axis of the second rotation
     */
    public Vector3D getA3() {
        return this.a3;
    }

}
