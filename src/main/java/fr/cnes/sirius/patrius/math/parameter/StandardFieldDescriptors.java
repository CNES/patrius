/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
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
 * Copyright 2010-2011 Centre National d'Études Spatiales
 */
package fr.cnes.sirius.patrius.math.parameter;

import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.OrbitalCoordinate;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;

/**
 * Standard field descriptors.
 *
 * @author Hugo Veuillez (CNES)
 * @author Thibaut Bonit (Thales Services)
* HISTORY
* VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
* VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
* END-HISTORY
 */
public final class StandardFieldDescriptors {

    /** Field descriptor to associate with the name of a parameter. */
    public static final FieldDescriptor<String> PARAMETER_NAME = new FieldDescriptor<>(
            "parameter_name", String.class);

    /** Field descriptor to associate with a force model. */
    @SuppressWarnings("unchecked")
    public static final FieldDescriptor<Class<? extends ForceModel>> FORCE_MODEL = new FieldDescriptor<>("force_model",
            (Class<Class<? extends ForceModel>>) ForceModel.class.getClass(), (cls) -> cls.getSimpleName());

    /** Field descriptor to associate with a date. */
    public static final FieldDescriptor<AbsoluteDate> DATE = new FieldDescriptor<>("date",
            AbsoluteDate.class);

    /** Field descriptor to associate with a date interval. */
    public static final FieldDescriptor<AbsoluteDateInterval> DATE_INTERVAL = new FieldDescriptor<>("date_interval",
            AbsoluteDateInterval.class, (dateInterval) -> {
        final AbsoluteDate initDate = dateInterval.getLowerData();
        final AbsoluteDate endDate = dateInterval.getUpperData();
        return initDate.toString() + "@" + endDate.toString();
    });

    /** Field descriptor to associate with an orbital coordinate. */
    public static final FieldDescriptor<OrbitalCoordinate> ORBITAL_COORDINATE = new FieldDescriptor<>(
            "orbital_coordinate", OrbitalCoordinate.class);

    /**
     * Private constructor.
     *
     * <p>
     * This class is a utility class, it should neither have a public nor a default constructor.
     * This private constructor prevents the compiler from generating one automatically.
     * </p>
     */
    private StandardFieldDescriptors() {
    }
}