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
package fr.cnes.sirius.patrius.math.exception.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for transforming the list of arguments passed to
 * constructors of exceptions.
 * 
 * @version $Id: ArgUtils.java 18108 2017-10-04 06:45:27Z bignon $
 */
public final class ArgUtils {
    /**
     * Class contains only static methods.
     */
    private ArgUtils() {
    }

    /**
     * Transform a multidimensional array into a one-dimensional list.
     * 
     * @param array
     *        Array (possibly multidimensional).
     * @return a list of all the {@code Object} instances contained in {@code array}.
     */
    public static Object[] flatten(final Object[] array) {
        final List<Object> list = new ArrayList<Object>();
        if (array != null) {
            for (final Object o : array) {
                if (o instanceof Object[]) {
                    for (final Object oR : flatten((Object[]) o)) {
                        list.add(oR);
                    }
                } else {
                    list.add(o);
                }
            }
        }
        return list.toArray();
    }
}
