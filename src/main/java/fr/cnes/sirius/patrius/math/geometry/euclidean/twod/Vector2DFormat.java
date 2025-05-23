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
package fr.cnes.sirius.patrius.math.geometry.euclidean.twod;

import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Locale;

import fr.cnes.sirius.patrius.math.exception.MathParseException;
import fr.cnes.sirius.patrius.math.geometry.Vector;
import fr.cnes.sirius.patrius.math.geometry.VectorFormat;
import fr.cnes.sirius.patrius.math.util.CompositeFormat;

//CHECKSTYLE: stop IllegalType check
//Reason: Commons-Math code kept as such

/**
 * Formats a 2D vector in components list format "{x; y}".
 * <p>
 * The prefix and suffix "{" and "}" and the separator "; " can be replaced by any user-defined strings. The number
 * format for components can be configured.
 * </p>
 * <p>
 * White space is ignored at parse time, even if it is in the prefix, suffix or separator specifications. So even if the
 * default separator does include a space character that is used at format time, both input string "{1;1}" and
 * " { 1 ; 1 } " will be parsed without error and the same vector will be returned. In the second case, however, the
 * parse position after parsing will be just after the closing curly brace, i.e. just before the trailing space.
 * </p>
 * 
 * @version $Id: Vector2DFormat.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
public class Vector2DFormat extends VectorFormat<Euclidean2D> {

    /**
     * Create an instance with default settings.
     * <p>
     * The instance uses the default prefix, suffix and separator: "{", "}", and "; " and the default number format for
     * components.
     * </p>
     */
    public Vector2DFormat() {
        super(DEFAULT_PREFIX, DEFAULT_SUFFIX, DEFAULT_SEPARATOR,
            CompositeFormat.getDefaultNumberFormat());
    }

    /**
     * Create an instance with a custom number format for components.
     * 
     * @param format
     *        the custom format for components.
     */
    public Vector2DFormat(final NumberFormat format) {
        super(DEFAULT_PREFIX, DEFAULT_SUFFIX, DEFAULT_SEPARATOR, format);
    }

    /**
     * Create an instance with custom prefix, suffix and separator.
     * 
     * @param prefix
     *        prefix to use instead of the default "{"
     * @param suffix
     *        suffix to use instead of the default "}"
     * @param separator
     *        separator to use instead of the default "; "
     */
    public Vector2DFormat(final String prefix, final String suffix,
        final String separator) {
        super(prefix, suffix, separator, CompositeFormat.getDefaultNumberFormat());
    }

    /**
     * Create an instance with custom prefix, suffix, separator and format
     * for components.
     * 
     * @param prefix
     *        prefix to use instead of the default "{"
     * @param suffix
     *        suffix to use instead of the default "}"
     * @param separator
     *        separator to use instead of the default "; "
     * @param format
     *        the custom format for components.
     */
    public Vector2DFormat(final String prefix, final String suffix,
        final String separator, final NumberFormat format) {
        super(prefix, suffix, separator, format);
    }

    /**
     * Returns the default 2D vector format for the current locale.
     * 
     * @return the default 2D vector format.
     */
    public static Vector2DFormat getInstance() {
        return getInstance(Locale.getDefault());
    }

    /**
     * Returns the default 2D vector format for the given locale.
     * 
     * @param locale
     *        the specific locale used by the format.
     * @return the 2D vector format specific to the given locale.
     */
    public static Vector2DFormat getInstance(final Locale locale) {
        return new Vector2DFormat(CompositeFormat.getDefaultNumberFormat(locale));
    }

    /** {@inheritDoc} */
    @Override
    public StringBuffer format(final Vector<Euclidean2D> vector, final StringBuffer toAppendTo,
                               final FieldPosition pos) {
        final Vector2D p2 = (Vector2D) vector;
        return this.format(toAppendTo, pos, p2.getX(), p2.getY());
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.PrematureDeclaration")
    public Vector2D parse(final String source) {
        final ParsePosition parsePosition = new ParsePosition(0);
        final Vector2D result = this.parse(source, parsePosition);
        if (parsePosition.getIndex() == 0) {
            throw new MathParseException(source,
                parsePosition.getErrorIndex(),
                Vector2D.class);
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public Vector2D parse(final String source, final ParsePosition pos) {
        final double[] coordinates = this.parseCoordinates(2, source, pos);
        if (coordinates == null) {
            return null;
        }
        return new Vector2D(coordinates[0], coordinates[1]);
    }

    // CHECKSTYLE: resume IllegalType check
}
